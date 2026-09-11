# Plugin Hub submission checklist

Reviewed: 2026-09-11. Baseline: `1f7183a593afe6606455e7b247f93d876edaefe4`.

**Status: submitted for review in [Plugin Hub PR #16428](https://github.com/runelite/plugin-hub/pull/16428).
Published source `e55d0c9` passed strict official packaging. CI/reviewer response
and approval remain pending.**

Work through the blockers first, then the release checks and submission steps.
Tick an item only when its completion criteria have been met. Record the fixing
commit or verification evidence in the progress log at the end.

The initial assessment inspected code and official Hub tooling, including
CI bundle v3 (`5654eb60d6e664d69dad0dda4fd0e794aea1aa88`). B4 passed the upstream
Gradle packaging tasks and runtime API scan; B5 passed the strict upstream
packager on local working-tree build inputs. R1 subsequently passed exact-commit
verification for candidate `977a21e5158c15a1481b7c23de31a9411208b342`. Recheck
rules and repeat packaging if the proposed release source changes.

## 1. Submission blockers

### B1. Remove reflection from Shortest Path

- [x] B1: Replace all reflective Shortest Path integration and verify routing.

The baseline `ShortestPathBridge.java` used reflection for route status, target
inspection, overrides and message-handler calls. Affected baseline lines include
274, 313, 325 and 339-350. Public-member reflection is still covered by RuneLite's
[forbidden language features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#forbidden-language-features).

Use the supported plugin-message handoff and a supported ownership/status
protocol. If upstream exposes no result notification, show a conservative
status instead of inspecting its internal pathfinder.

Done when: no reflective access remains; Route and Clear work; changing or
disabling the integration does not clear someone else's route; missing or
disabled Shortest Path produces a useful message.

Completed 2026-09-11. The bridge now uses PluginMessage handoffs, observed route
ownership, guarded queued cleanup, and a conservative "Route sent" status.
Build and source/bytecode checks passed. Live checks covered Route/Clear, rapid
panel clicks, disabled-provider messaging, owner-performed Set Target, Quest
Helper takeover, and preserving foreign routes across selection/lifecycle
changes. See [B1 verification and protocol limits](reconstruction/shortest-path-integration.md#b1-verification-2026-09-11).

### B2. Remove reflection from Weapon Charges

- [x] B2: Replace reflective charge metadata lookup and verify charge checks.

The baseline `WeaponChargesBridge.java` dynamically loaded enums and read their fields around
lines 152-164 and 189-195. Replace this with reviewed bundled mappings
or a supported integration API. See the same
[reflection restriction](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#forbidden-language-features).

Done when: charge estimates and fresh in-game Check observations still work;
the 1,000 activation ether stays separate from usable charges; the configured
charge limit is respected; unavailable provider data remains clearly unknown.

Working-tree implementation (2026-09-11): replaced reflection with the reviewed
Plugin Hub mappings; build, bytecode scan and metadata comparison passed. Kimi
reviewed the finished patch with no blocking findings; Claude Sonnet/medium was
unavailable because of its account usage limit. In the rebuilt client, the
Ursine chainmace showed a 500-charge provider estimate; disabling Weapon Charges
correctly changed it to Balance unknown and retained the Check requirement.
The owner subsequently confirmed that the carried/equipped Check, charge-limit
boundaries and separate activation-ether checks passed on 2026-09-11. This is
owner-reported manual verification; no new agent-operated game check is claimed. See [B2 verification](reconstruction/weapon-charges-integration.md#b2-verification-2026-09-11).

### B3. Replace restricted runtime API calls

- [x] B3: Replace the flagged widget identifiers and direct Gson construction.

The current [Hub API restrictions](https://github.com/runelite/plugin-hub-tooling/blob/5654eb60d6e664d69dad0dda4fd0e794aea1aa88/package/src/main/resources/net/runelite/pluginhub/packager/disallowed-apis.txt)
require supported component/interface identifiers and RuneLite's injected Gson.

| File | Baseline usage | Required change |
| --- | --- | --- |
| `integration/advisor/SetupExporter.java:98` | `new Gson()` | Pass or inject the client's Gson |
| `bank/advisor/PlayerStateCapture.java:163` | `WidgetInfo.BANK_ITEM_CONTAINER` | Use the supported equivalent identifier |
| `integration/advisor/BankSetupFilter.java:235` | `WidgetInfo.BANK_CONTENT_CONTAINER` | Use the supported equivalent identifier |
| `bank/advisor/DeathStateCapture.java:74` | `WidgetInfo.LOOTING_BAG_CONTAINER` | Use the supported equivalent identifier |
| `bank/advisor/DeathStateCapture.java:195` | `WidgetInfo.PVP_WILDERNESS_LEVEL` | Use the supported equivalent identifier |

Java paths above are relative to
`src/main/java/com/danieljglover/allinslayer/`.

Done when: the Hub API scan passes and bank detection/filtering, looting-bag
capture, Wilderness level detection and setup export retain their behavior.

Working-tree implementation (2026-09-11): all four widget references now use
their exact InterfaceID equivalents; SetupExporter receives RuneLite's injected
Gson. Build passed. After B4 moved authoring code into its own source set, the
official API recorder/checker passed for all 596 production runtime API
references with zero restricted calls. The owner confirmed an empty looting bag and
the panel retained that observation after closing its view. Live Copy setup
produced valid JSON with 14 equipment and 28 inventory slots. Open-bank capture,
equipment/inventory filtering and restoring the normal bank also passed. The
owner entered level 2 Wilderness; the live level display and exact replacement
widget/parser mapping were verified. AIO does not display its captured integer
directly. B4's unfiltered production scan now clears B3's remaining dependency.
See [B3 evidence and scan scope](reconstruction/runtime-api-compliance.md).

### B4. Separate data-generation code from the runtime

- [x] B4: Move authoring compilers into a separate build source set.

The baseline main source set included 33 authoring classes from 31 Java files. Six
compiler classes construct Gson or GsonBuilder. The Hub
[build integration](https://github.com/runelite/plugin-hub-tooling/blob/5654eb60d6e664d69dad0dda4fd0e794aea1aa88/bundle/src/main/resources/target_init.gradle)
records APIs during `compileJava`, so excluding compiler classes only from the
final JAR does not remove their recorded calls.

Keep source-data models/compiler packages organised, update generation task
classpaths, and continue generating runtime data from `src/main/data/slayer`.
Avoid a dependency cycle between resource generation and runtime compilation.
Do not recreate the removed legacy editable resource.

Done when: clean generation and compilation succeed; the runtime catalogue is
unchanged except for intentional updates; the Hub artifact contains the needed
generated resources and no authoring compiler classes.

Completed in the working tree (2026-09-11): the unchanged authoring package now
lives in `src/dataGenerator/java` and generation uses its own source set. Clean
generation/build passed without a cycle; all four generated resources are
byte-identical. The upstream Hub package contains those resources and 130 Java 11
runtime classes, with no authoring classes. The full production API scan passed
(596 references, zero restricted calls). Claude Sonnet/medium and Kimi approved
the final wiring and evidence. The packaged JAR also loaded active-task and
catalogue recommendations in the owner's established Jagex/Main-profile client.
See [B4 verification](reconstruction/data-generation-isolation.md).

### B5. Make the Hub build configuration explicit

- [x] B5: Set the custom build mode and satisfy build-script checks.

Add `build=gradle` to `runelite-plugin.properties`. The
[submission checker](https://github.com/runelite/plugin-hub-tooling/blob/5654eb60d6e664d69dad0dda4fd0e794aea1aa88/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java#L976)
requires an explicit build mode for new submissions. Standard mode replaces
the Gradle scripts, which would remove our catalogue-generation steps.

Wrap baseline `build.gradle:152`, currently 136 characters, to satisfy the
[120-character build-script limit](https://github.com/runelite/plugin-hub-tooling/blob/5654eb60d6e664d69dad0dda4fd0e794aea1aa88/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java#L515).

Done when: the explicit custom build generates all required resources and the
official tooling accepts its configuration and formatting.

Completed in the working tree (2026-09-11): added `build=gradle` and wrapped the
Windows newline filter. Clean generation/build passed. The unmodified upstream
`Plugin.build("1.12.38", true)` accepted the build configuration and formatting,
passed strict API/descriptor checks, and produced the final plugin JAR with all
four unchanged generated resources. Verification used Adoptium Java 11 after a
system-JDK ZIP rewrite failure. Claude Sonnet/medium found no issues; Kimi was
unavailable due to its usage quota. See [B5 verification and scope](reconstruction/hub-build-configuration.md).

## 2. Release checks

### R1. Verify the actual Hub artifact

- [x] R1: Run official Hub packaging/API checks on the proposed release commit.

Run the project's clean generation/build after compiler changes, then follow
the current official Hub tooling instructions. Record its version, target
RuneLite version, commit and output. A local build or Windows ZIP passing is
separate evidence from a Hub build passing.

Completed 2026-09-11 for candidate
`977a21e5158c15a1481b7c23de31a9411208b342`: the unmodified official tooling
cloned and checked out that commit, passed strict packaging/API checks against
RuneLite 1.12.38, and produced a 2,851,407-byte final JAR. All four generated
resources are unchanged; all 596 recorded API references passed. The final JAR
loaded catalogue recommendations in the existing Jagex/Main-profile client.
The clone used a process-local Git mirror; no push or submission occurred.
See [release verification, artifact hashes and scope](reconstruction/hub-release-verification.md).

At the assessment baseline, the normal plugin JAR was 3,277,428 bytes, contained
Java 11 classes and generated data, and excluded the development launcher and
dependency JARs. It was below the Hub's 10 MiB limit. Preserve those properties
and verify startup with resources loaded from the packaged JAR.

### R2. Finish presentation and attribution

- [x] R2: Add the Hub listing icon and refresh the public README/screenshots.
- [x] R3: Include code license and applicable Wiki attribution/license notices in the runtime JAR.

Completed in the working tree (2026-09-11): root `icon.png` is the current
24-by-24 sidebar helmet icon. The public README now leads with getting started,
capabilities, three real panel screenshots, optional integrations and limits.
Detailed settings and behavior are preserved in `docs/user-guide.md`. Captures
exclude account names, chat and the surrounding game view. The pre-release and
release verification status remain explicit.

The runtime JAR now includes the AIO BSD license, CC BY-NC-SA 3.0 legal text,
Wiki attribution/adaptation notice, and existing Weapon Charges BSD notice in
`META-INF/`. Code and adapted Wiki text licenses are distinguished. Licensing
was checked against Weird Gloop's Licensing page (page 24, revision 1299,
2026-09-07T14:20:49Z); original source URLs and evidence remain in the data.
Clean generation/build and both normal-JAR and final Hub-JAR resource checks
passed; all generated catalogue JSON files are unchanged. Claude Sonnet/medium
approved the presentation and attribution changes. Kimi was unavailable due to
its usage quota. These changes are included in candidate `977a21e`.

R4 and its M1-M12 manual checklist were removed at the owner's request on
2026-09-11. They are not marked passed or delegated. RuneLite's
[review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
explicitly excludes functionality, performance, compatibility and accuracy.
Existing verification evidence remains in the reconstruction documents.

### R5. Explain integrations clearly to reviewers

- [x] R5: Document passive behavior and integration boundaries in the submission description.

Explain the Bank Tags layout adjustments, existing withdrawal handlers,
explicit routing requests, static Wiki boss preparation, account-scoped bank
observations and bundled data. No automation, runtime HTTP collection or live
combat prayer/tile prompts were found in this assessment.

Reviewer interpretation of boss-guide scope remains uncertain: RuneLite lists
new high-end PvM boss plugins among features it is not currently considering.
That is a review risk, not a confirmed prohibition of this Slayer advisor.
Link the relevant [RuneLite policy](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
and [Jagex guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
when explaining the preparation-only behavior.

Completed 2026-09-11: the [draft submission description](plugin-hub-submission-description.md)
explains Bank Tags widget/layout adjustments and retained withdrawal handlers,
explicit routing and ownership limitations, charge observations, account-scoped
bank persistence, bundled data and static boss preparation. Both linked policy
pages were checked against their current published content. Boss-guide scope
remains explicitly subject to reviewer interpretation. No PR was opened; publication remains pending. B2 was subsequently confirmed
by the owner.

## 3. Optional improvement

- [ ] O1 (optional): Add Copy diagnostic summary for support reports.

Include plugin/data version, selected task/monster/location/method, filters and
blockers. Exclude account identifiers, credentials and complete bank contents.
The user should explicitly copy and share it. This would help investigate
reports such as an Active Task setup appearing for one player but not another.
It is not a submission blocker and can be deferred.

## 4. Submit for review

Follow the current [Plugin Hub submission process](https://github.com/runelite/plugin-hub#submitting-a-plugin).

- [x] S1: Resolve B1-B5 and record R1 packaging evidence for the final source commit.
- [x] S2: Review remaining release checks and explicitly record any deferred items.
- [x] S3: Push the release commit and replace the placeholder in the manifest template with its full 40-character hash.
- [x] S4: Create a branch in a Plugin Hub fork and add `plugins/all-in-slayer` containing the repository URL and release commit.
- [x] S5: Open one pull request with the feature summary, screenshots and integration/compliance explanation.
- [ ] S6: Address CI/reviewer findings in that PR, updating the pinned source commit as fixes are pushed.
- [ ] S7: After merge and availability, verify installation through the normal RuneLite Plugin Hub.

Pre-submission readiness review (2026-09-11): the owner confirmed the remaining B2 manual
checks passed, closing B1-B5. S1 is complete for the exact candidate verified by R1:
`977a21e5158c15a1481b7c23de31a9411208b342`; subsequent commits change only
documentation, including the README. If a later commit is selected for
the manifest, run official packaging against that exact commit before submission.

S2 is complete: R1, R2/R3 and R5 have recorded evidence. R4 is dropped at the
owner's request. O1 (Copy diagnostic summary) is deferred beyond the first
submission as a non-blocking support enhancement. B2 is complete through owner-reported manual verification.
The boss-preparation policy interpretation is disclosed for reviewers in R5;
no approval is assumed.

Submission update (2026-09-11): source commit
`e55d0c98fd0bb09116fd8161e0e997df8cfbbe8b` was pushed on
`release/plugin-hub-2026-09-11`. The official packager cloned it from GitHub
and passed strict checks. The manifest now pins that exact commit. The fork
branch `danieljglover:add-all-in-slayer` contains only the two-line descriptor;
[PR #16428](https://github.com/runelite/plugin-hub/pull/16428) is open. S6 remains
open for CI/reviewer findings; S7 requires merge and Hub availability. The current
upstream submission instructions still require a fork branch, a source-repository
URL and full commit hash in one plugin descriptor, and one PR updated for findings.

The existing [manifest template](plugin-hub-manifest.txt) is a starting point.
Submission points to source; the clan Windows ZIP is a separate distribution.
Approval and timing are controlled by RuneLite. Do not mark submission or
acceptance complete until the corresponding external action has occurred.

## Progress log

| Date | Item | Commit or evidence | Outcome / next action |
| --- | --- | --- | --- |
| 2026-09-11 | Assessment | Baseline `1f7183a`; official tooling v3 inspected | Blockers identified; Hub packaging and submission not performed |
| 2026-09-11 | B1 | Working-tree bridge/UI changes; [verification](reconstruction/shortest-path-integration.md#b1-verification-2026-09-11) | Build and live client checks passed; reflection removed from Shortest Path integration. Other blockers remain open. |
| 2026-09-11 | B2 | Working-tree bundled mappings; [verification](reconstruction/weapon-charges-integration.md#b2-verification-2026-09-11) | Build, mapping/bytecode review, provider estimate and disabled-provider unknown checks passed. Fresh owner Check and limit boundary remain pending. |
| 2026-09-11 | B3 | Working-tree InterfaceID/Gson migration; [verification](reconstruction/runtime-api-compliance.md) | Build, empty-bag capture, JSON export and bank capture/filter/clear passed. Owner level-2 display plus widget/parser equivalence verified. B4's full production API scan now passes; B3 closed. |
| 2026-09-11 | B4 | Working-tree dataGenerator source set; [verification](reconstruction/data-generation-isolation.md) | Clean generation/build and upstream Hub packaging passed; all four JSON files unchanged, no authoring classes in the artifact, zero restricted API findings. Packaged JAR loaded active-task/catalogue panels using the existing Jagex/Main profile. Final Claude Sonnet/medium and Kimi reviews approved. |
| 2026-09-11 | B5 | Working-tree explicit build mode and line wrap; [verification](reconstruction/hub-build-configuration.md) | Clean build and strict upstream packager passed using Adoptium Java 11; final JAR includes unchanged data and no authoring classes. Claude review passed; Kimi quota unavailable. Final release-commit verification remains R1. |
| 2026-09-11 | R1 | Candidate `977a21e`; [release evidence](reconstruction/hub-release-verification.md) | Official clone/checkout, strict packaging/API checks, source archive and final-JAR startup passed. Publication remains pending. |
| 2026-09-11 | R2/R3 | Candidate `977a21e`; README, screenshots, icon and META-INF notices | Presentation and attribution complete; final Hub artifact verified; Claude Sonnet/medium approved. |
| 2026-09-11 | R4 | Owner request | Removed manual checklist; not marked passed or delegated to RuneLite. |
| 2026-09-11 | R5 | [Draft submission description](plugin-hub-submission-description.md); implementation and current policy review | Passive behavior and integration boundaries documented; boss-guide review risk explicit. No external submission. |
| 2026-09-11 | S2 / readiness review | Current official submission instructions, local candidate evidence and remote refs checked | Release checks reviewed; O1 deferred and R4 dropped. S1 still awaits B2 verification; no push or PR performed. |
| 2026-09-11 | B2 / S1 | Owner confirmed the requested carried/equipped Check, charge-limit and activation-ether checks passed | B2 closed; S1 complete for verified candidate `977a21e`. Later source commits require their own packaging verification. |
| 2026-09-11 | S3-S5 | Published `e55d0c9`; [PR #16428](https://github.com/runelite/plugin-hub/pull/16428) | Official remote clone and strict packaging passed; manifest pinned; one Hub PR opened with screenshots and integration/policy notes. S6/S7 pending. |

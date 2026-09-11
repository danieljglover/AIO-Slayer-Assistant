# Hub release verification

Verified on 2026-09-11. The proposed release candidate is
`977a21e5158c15a1481b7c23de31a9411208b342`, on local branch
`release/plugin-hub-2026-09-11`. Later documentation commits are evidence about
this candidate; they are not the commit packaged in this run. Nothing has been
pushed or submitted to the Plugin Hub.

## Official packaging

The project passed `./gradlew clean generateSlayerData build`. The unmodified
official tooling at commit `9b441d86aa16ec9500ca4654b4c14696890c9cba` then
verified the committed candidate with RuneLite 1.12.38, Gradle 8.10 and
Adoptium Java 11.0.32+101. This uses the Java distribution that resolved the
local ZIP rewrite failure documented in [B5](hub-build-configuration.md).

The public `Plugin.download()` and `Plugin.build("1.12.38", true)` methods
were invoked through JShell. The descriptor used the repository URL and full
candidate hash. A process-only Git URL rewrite mapped that URL to the local
repository's Git mirror, allowing the official clone and checkout to run on
the unpublished commit. The resulting checkout HEAD matched the candidate.
No checker was patched or disabled, and no upload operation was invoked.

Strict checks accepted the custom Gradle configuration, build-script formatting,
descriptor, listing icon and all 596 recorded runtime API references.

| Final artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `all-in-slayer.jar` | 2,851,407 | `a2ac77537e100b12db9082e8873a1c7ba94a31b7d06759ac1b25b73e9e055650` |
| `all-in-slayer.zip` | 2,793,365 | `967aa8942bdd4dcf208b0562d1c32952fa45a9b6c89ed4b1e4256851eafab36b` |

The ZIP is the official source archive, not a runnable Windows distribution.
Comparison with the committed tree confirmed all source, generator, build and
presentation files were included. The upstream source archive size policy
omitted only four historical Windows preview ZIPs in `downloads/`.

The final JAR contains 130 Java 11 runtime classes, generated metadata and
resources, and all four license/notice files. It contains no authoring compiler
classes, development launcher classes, nested dependency JARs or session files.
All four generated data files are byte-identical to the B4 baseline:

| Resource | SHA-256 |
| --- | --- |
| `advisor-catalogue.json` | `cce5a503907996f0e7b96593ca14c308be9a7d19f8b3c754ae1b410870636ef8` |
| `advisor-coverage.json` | `37101dc7c21386c783cae81eea7572bbd0b7af1df1f4fd7ad88834e8447d6a9e` |
| `slayer-data.json` | `d50528f228e1ad338993235e858e019e56f74babc1bef2e823d6fd898bcaf3c6` |
| `slayer-meta.json` | `65dea545f924062b85842447454f8343cb0b0ac87378aa8eb1ce67b73763a248` |

## Actual packaged-client startup

The final Hub JAR was launched through the established Jagex/Bolt Main-profile
process, alongside Wilderness Sentinel. The running classpath was checked to
contain that exact JAR, without main class or resource directories that could
mask missing packaged resources.

The AIO sidebar opened and Catalogue loaded an Aberrant spectre recommendation
for Stronghold Slayer Cave, with the expected logged-out player-state warning.
Only plugin panels were operated; the client remained at Play Now. This proves
startup and packaged catalogue loading, not fresh in-game charge observations
or comprehensive gameplay behavior. B2's remaining checks stay open.

## Presentation and attribution

The accepted root icon is the current 24-by-24 sidebar helmet. The README now
provides quick-start instructions, capabilities, three actual panel screenshots,
optional integrations and accuracy limits. Detailed settings remain in
[the user guide](../user-guide.md). Public screenshots exclude account names,
chat and the game view.

The final JAR includes the AIO BSD license, Weapon Charges BSD notice, full
CC BY-NC-SA 3.0 legal text and a Wiki attribution/adaptation notice. Wiki text
licensing was checked against Weird Gloop's Licensing page, page 24, revision
1299 (2026-09-07T14:20:49Z). The notices distinguish code, adapted text and
third-party artwork. Source URLs and revision evidence remain bundled in data.

Claude Sonnet with medium effort approved the presentation and attribution
changes. Kimi was unavailable due to its usage quota; no Kimi approval is claimed.

## Evidence and remaining scope

Local diagnostic evidence is under `/tmp/aio-release-team/`: the invocation,
clone hash, packager build/result logs, artifact verification, final artifacts
and live startup verification. Temporary files are not a durable distribution;
the candidate hash and artifact hashes above identify what was verified.

R4 was removed at the owner's request, not passed or delegated. RuneLite's
[review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
excludes functional, performance, compatibility and factual-accuracy testing.
B2's owner-operated checks, R5's submission explanation, publishing and the
Plugin Hub pull request remain separate outstanding work. Changed release
source requires a new candidate and packaging verification.

Follow-up on 2026-09-11: R5 documentation was completed and the owner confirmed
the remaining B2 manual charge checks passed. The pending statements above
describe the original packaging session; publication and submission remain
pending. The verified artifact and candidate hash are unchanged.

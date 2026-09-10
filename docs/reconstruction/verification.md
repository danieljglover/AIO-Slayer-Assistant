# Reconstruction verification

## Baseline

- `./gradlew build`: passed before implementation.
- Existing unresolvable strategy descriptions demonstrated why the previous
  weapon-only runtime representation could not support complete wiki loadouts.
- The pre-existing `scratchpad/dev-run.log` change was preserved.

## Build and source checks, 2026-09-08

- JSON parsing across all 1,133 Slayer source files: passed.
- `./gradlew generateSlayerData`: passed source IDs, item references and links.
- `./gradlew build`: passed Java 11 compilation and bundled resource generation.
  Gradle's standard empty test task reported `NO-SOURCE`; no automated tests,
  test files or testing dependencies were added.
- `./gradlew run`: native RuneLite startup and catalogue checks passed as below.
- Final strategy, protective-equipment, assignment-area, and supply-ordering
  corrections received independent source review. Generated output confirmed
  resolved mandatory references, consistent item eligibility, 16 informational
  boss referrals, and removal of all legacy strategy Markdown files.
- The combined final `./gradlew build` passed after the live spellbook correction;
  `git diff --check` also passed.

## Native client checks, 2026-09-08

- RuneLite 1.12.38 launched through the dev launcher with the rebuilt plugin.
- Active Task correctly displayed no assignment at the login screen.
- Catalogue opened and listed 118 assignments. Searching `waterfiend` selected
  Waterfiends; scrolling exposed its location/method guidance and unavailable
  player-state explanation.
- Native inspection exposed narrow controls and an unreadable inherited font.
  After fixing Swing alignment and using RuneLite's UI font, a second launch
  showed full-width controls and readable wrapping at the standard sidebar size.
- Game login, movement, combat, bank interactions, and other game actions were
  not performed. The user will perform any needed in-game actions; agent
  interaction is limited to the plugin panel.
- At the user's invitation, `scripts/dev-run.sh` launched the account-aware
  client successfully to its Play Now screen. The agent did not press Play Now.
  Login and bank observation checks remain pending user interaction.
- After the final build, the account-aware development client was relaunched.
  Active Task and Catalogue rendered with all 118 assignments; startup logs
  contained no plugin error. The client was left at Play Now with the sidebar open.

## Manual account scenarios

Account scenarios require a logged-in character and are not inferred from a
successful build. Record the actual result beside each scenario when exercised.

| Scenario | Expected result | Result |
| --- | --- | --- |
| No active task | Catalogue usable; no invented assignment | Pending |
| Assignment changes/completes | Task count, filters, and overlay update together | Pending |
| Boss assignment | Only the assigned boss is eligible in Active Task | Pending |
| Konar assignment | Location lock respected; unresolved lock withholds advice | Pending |
| Browse while assigned | Separate planning context; no task-only bonuses/access | Pending |
| First bank use | Inventory/equipment-only notice before scan | Pending |
| Withdraw/deposit | No temporary double-counting; checklist updates | Pending |
| Account switching | No other account's bank or confirmations appear | Pending |
| Wiki alternatives | Best owned usable wiki alternative selected per slot | Pending |
| No wiki alternative owned | Compatible owned fallback is clearly labelled | Pending |
| Required protective gear absent | Method unavailable with specific reason | Pending |
| Two-handed weapon | Shield excluded; mandatory shield forces viable alternative | Pending |
| Ranged/magic method | Compatible ammo/runes/charges requirements visible | Pending |
| Change spellbook | Supported named-book requirements use live state; saved confirmation cannot override mismatch | Pending |
| Multi-style boss | Switches included within inventory capacity | Pending |
| Supplies | Owned quantities respected; missing amounts separate | Pending |
| Goals | Explained ranking changes, no invented hourly rates | Pending |
| Wilderness/group opt-in | Restricted methods excluded until enabled | Pending |
| Clipboard export | Inventory Setups imports 14 equipment and 28 inventory slots | Pending |
| Disable/re-enable plugin | No stale worker results or duplicate sidebar/overlay | Pending |

## Interface source references

- RuneLite SlayerPlugin task/area DB table lookup, checked 2026-09-08:
  https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPlugin.java
- Inventory Setups portable setup, equipment and quantity encoding, checked
  2026-09-08:
  https://github.com/dillydill123/inventory-setups/tree/master/src/main/java/inventorysetups/serialization

The live spellbook mapping uses RuneLite `VarbitID.SPELLBOOK` (4070), with values
0 Standard, 1 Ancient Magicks, 2 Lunar, and 3 Arceuus. The installed RuneLite API
and the maintained Inventory Setups mapping were checked on 2026-09-08:
https://github.com/dillydill123/inventory-setups/blob/master/src/main/java/inventorysetups/ui/InventorySetupsSpellbookPanel.java

## Variant and Wilderness correction, 2026-09-08

See [variant-link-review.md](variant-link-review.md) for the source audit and
specific restored alternatives. The combined build, generation, all 1,144 JSON
files and whitespace checks passed. Independent review confirmed the preference
invalidation and diary-confirmation fixes after identifying both issues.

The owner's logged-in Skeleton task and open bank were observed through a
passive screenshot before the fix. The panel showed 66 remaining and a captured
bank timestamp. This verifies basic task/bank observation in the prior build;
it does not verify withdrawals, deposits, or the corrected runtime behavior.
The owner was asked to restart the development client and reopen the bank to
exercise the new code. No game controls or client shutdown were performed.

## Slayer helmet and Salve correction, 2026-09-08

See [combat-bonus-review.md](combat-bonus-review.md) for the exact effect matrix,
source revisions, comparison limits and manual scenarios. The Java 11 build,
catalogue generation and changed JSON parsing passed. The generated catalogue
carries all 112 reviewed effect IDs and the target variants' undead flags.
No automated tests were added. The owner's bank and Skeleton task were observed
in the previous running build; this does not establish the new ranking behavior.

After reopening the development client with the new build, the owner logged in
and refreshed the bank. The Skeletons task remained at 66. Native plugin-panel
inspection of the Vet'ion preparation confirmed Slayer helmet (i) plus Amulet of
rancour, both labelled `Bonus comparison`, with +16.67% melee accuracy/damage and
an explanation of the joint, non-stacking comparison. The focused independent
code review found no concrete gaps. Remaining bonus-style and area scenarios in
the review document have not been manually exercised. Agent interaction was
limited to the plugin panel; login, banking and game input were the owner's.

## Automatic account requirements, 2026-09-08

Quest/diary/level evaluation now shares its status with the Account requirements
panel. The capture covers the current Quest enum and 48 diary tiers, supported
unlock flags, and a distinct claimed-reward check for Karuulm heat protection.
See [account-progress-sources.md](account-progress-sources.md) for evidence and
boundaries. The seven heat-protection waivers and authoring helper use the new
claimed-reward observation; changed JSON parsing and source generation pass.

Focused review identified no-requirement sentinels and mixed-expression grouping
edge cases. Corrections preserve punctuation clauses, named facts, and comma
conjunctions, including the Cerberus boosted alternative's minimum base level.
No automated tests or test dependencies were added. Account-panel confirmation
of the new build is recorded separately below when exercised.

The reopened development client detected the owner's Skeleton task and bank.
Passive panel inspection confirmed automatic quest completion (Death Plateau,
Dragon Slayer II), derived quest milestones, Defence 99 and Prayer 84 against
their required levels. No game controls were used. At the owner's request,
satisfied requirements are now hidden, whether automatic or manually confirmed;
unmet and unresolved checks remain visible, with an all-satisfied message when
nothing remains. The updated UI passed `./gradlew build`.

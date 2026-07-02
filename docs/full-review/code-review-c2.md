# Code Review - C2 segment (Krystilia Wilderness families, batches A-H)

Reviewer: ct-rv-c2 (Code Reviewer). Date: 2026-07-02.
Scope: the complete C2 delta on `main` - 23 net-new families (incl. 7 shared with C3 starter masters
and the wilderness-bosses meta-task), batches A (94889e4..8c7bb8f), E (7d9bec4/5297ff5/5a1354b),
F (5bd08fa/5726020), G (cc0ff14), H (db17eb8).
Against: ADR-0019 (+WD-7 amendment), CT-L location-ownership, c2-c3-worklist.md, ADR-0018 unlocks.

## Verdict: APPROVE-WITH-FINDINGS

**0 Blocker / 1 Major / 2 Minor.** The C2 segment is recipe-conformant, location-ownership-clean,
id-honest (the C3 fabricated-wikiPageId defect class did NOT recur), contract-conformant, and green
in isolation. Nothing gates integration; M1 should be closed before the C2/C3 wave is signed off
because it bakes a wrong-semantics value into 15 families' acceptance tests.

### Evidence (verification I ran myself)
- Isolated HEAD worktree (no untracked in-flight C3-G files): `./gradlew cleanTest test --tests
  'com.danieljglover.allinslayer.data.source.*' --tests '...TaskUnlockIntegrityTest'` =>
  **BUILD SUCCESSFUL, 613 tests, 0 fail / 0 error / 0 skip** (135 XMLs). All 23 C2 coverage tests +
  TaskUnlockIntegrityTest ran (Bandits/Revenants/WildernessBosses/Spiders/Zombies/... present).
- wikiPageId audit against the live MediaWiki API (`action=query&pageids=...`): the 4 non-null task
  ids all resolve to the CORRECT page - Bandit=20203, Black Knight=44926, Dark warrior=15856, Earth
  warrior=13043. Every other C2 task carries `wikiPageId:null` (honest UNKNOWN). C2 monster variant
  files carry no wikiPageId. **No fabricated 1523xxxx-shaped id anywhere in C2.**
- Revenants unlock name verified real (not garbage): `Revenenenenenants` resolves on the wiki (Slayer
  Rewards page); it is the genuine OSRS joke unlock name. Do NOT "fix" the spelling.

---

## Findings

### M1 (Major, data-honesty / convention) - `task.combatLevel` carries the monster NPC level, not the assignment threshold, in 15 C2 families

`SourceTask.combatLevel` means the combat-level REQUIREMENT to be assigned the task (legacy 43 tasks:
hellhounds 75, gargoyles 80; C3: catablepon 35, banshees 20, minotaurs 7; C2-A: bandits/black-knights/
dark-warriors/earth-warriors all `null`). The C3-F team-memory note states this explicitly: "task.
combatLevel = the master's Cb ASSIGNMENT threshold ... not the monster's combat level (that lives in
variantInfo)." Krystilia imposes **no** combat requirement, so a Krystilia-only family's `combatLevel`
must be `null`.

Batches C2-B/C/D/E instead set `task.combatLevel` to the **default variant's NPC combat level**:

| family | task.combatLevel | = NPC level of | correct value |
|---|---|---|---|
| ents | 101 | Ent (101) | null |
| lava-dragons | 252 | Lava dragon (252) | null (252 is an impossible requirement; max cb is 126) |
| ice-giants | 53 | Ice giant (53) | null |
| mammoths | 80 | Mammoth (80) | null |
| green-dragons | 79 | Green dragon (79) | null |
| moss-giants | 42 | Moss giant (42) | null |
| pirates | 26 | Pirate (26) | null |
| magic-axes | 42 | Magic axe (42) | null |
| rogues | 15 | Rogue (15) | null |
| chaos-druids | 13 | Chaos druid (13) | null |
| bears (shared) | 13 | Black bear (13) | null or starter threshold (worklist: no req) |
| hill-giants (shared) | 28 | Hill giant (28) | null (worklist: no req) |
| ice-warriors (shared) | 57 | Ice warrior (57) | null (worklist: no req) |
| scorpions (shared) | 14 | Scorpion (14) | 7 per worklist Cb-req, or null-with-note |
| skeletons (shared) | 15 | (default skeleton) | 15 per worklist Cb-req (value happens to match) |

Correct today: C2-A (bandits/black-knights/dark-warriors/earth-warriors) and C2-F (spiders/zombies)
are `null`. So the defect is internally inconsistent within C2.

**Runtime impact: none today.** `SourceTask.getCombatLevel()` is never read by
`ModularSlayerDataCompiler` (line 523's `setCombatLevel` is on `SourceMasterRequirements`, not the
task; `TaskData` has no `combatLevel` field). The value is only consumed by the source coverage tests
that read the raw JSON. That is exactly why this is Major, not Blocker - but also why it must be
fixed: the acceptance tests assert a falsehood (e.g. `EntsSourceCoverageTest` asserts BOTH
`task.getCombatLevel()==101` AND `variant.getCombatLevel()==101`), so the guard that is supposed to
prove data fidelity currently pins the wrong meaning. If `TaskData` ever gains a combat gate (WD/P2
skip-block advisor territory), lava-dragons would become permanently unassignable.

Fix (data owner, not reviewer): set `combatLevel:null` on every Krystilia-only C2 family and update
each family's coverage-test task-level assertion to `null`; for shared families use the starter
assignment threshold where a single value is honest (skeletons 15, scorpions 7) or `null` + a
taskNote where masters diverge (the C3-D cows precedent). Leave each variant's own `combatLevel`
(the NPC level) untouched - that field is correct.

### m1 (Minor, convention / cosmetic) - `bears` weakness element is `"FIRE"` uppercase

`tasks/bears.json` and all three `monsters/bears/*.json` carry `weakness.element:"FIRE"`; every other
C2 family uses lowercase (`"fire"`/`"earth"`/`"water"`), and C2-B established lowercase as the
convention. Impact is limited: bears are MELEE-weak so the element never reaches magic spell
selection, and `RuneTable` lowercases before its map lookup (`SPELLS.get(element.toLowerCase())`).
But it renders as "(FIRE)" in the panel weakness line (`SlayerPanel:1529`, user-visible) and is a
latent trap - `RuneTable.runeFor` switches on the raw string, so an uppercase element on a future
MAGIC-weak family would silently mis-handle. Fix: lowercase to `"fire"` (4 occurrences).

### m2 (Minor, note) - shared-family combatLevel folds into M1

scorpions (worklist Cb 7) and skeletons (worklist Cb 15) are the two shared families whose starter
masters do carry a real assignment threshold; scorpions currently shows 14 (the monster level).
Resolve as part of M1. No separate action.

---

## Dimension results (all PASS)

1. **Recipe conformance (ADR-0019):** all 23 families have task + monsters/ + strategy (>=2 methods,
   StrategyPlaceholder-clean) + locations + `<Family>SourceCoverageTest`. wilderness-bosses correctly
   authors no strategy file (its 3 new singles-plus variants route to the parent boss strategies by
   variantId) - conforms to the meta-task shape. Strategy method counts: revenants 3, all others 2.

2. **Data honesty / id audit:** PASS. 4 non-null task wikiPageIds all correct vs live MediaWiki API;
   all other task ids honest-null; no monster wikiPageIds; no 1523xxxx fabrication. SYNTHETIC
   slayerTargetIds 252-274 all flagged in taskNotes with the QA-live-verify caveat. Weights/amounts
   match the worklist (backend re-verified live 2026-07-02 per the batch memories). One casing nit
   (m1). One semantic issue (M1).

3. **CT-L location ownership:** PASS. Every C2 wilderness location `wilderness:true`; every non-wildy
   starter location (bears-area/hill-giants-area/ice-warriors-area/scorpions-area/skeletons-area/
   spiders-area/zombies-area) `wilderness:false`; `konarLockable:false` on all. Shared bases present
   with single owners (frozen-waste-plateau, graveyard-of-shadows, bandit-camp-wilderness) + correct
   tuned copies (graveyard-of-shadows-green-dragons/-skeletons, bandit-camp-wilderness-black-knights,
   wilderness-slayer-cave-ice-giants). No duplicate base files, no missing referenced files, no
   existing-file edits.

4. **Contract conformance:** PASS. Synthetic ids match the worklist allocation verbatim (252-274).
   Union masterIds on all 7 shared families match the worklist §2 starter columns (bears/scorpions/
   skeletons/zombies = kry+turael+spria+mazchna; hill-giants/ice-warriors = kry+mazchna; spiders =
   kry+turael+spria). §5.5 look-alike traps all avoided: generic "Spider" (not deadly red spiders),
   Wilderness "Bandit" (not Kharidian/Pollnivneach), wilderness-bosses is a distinct id-274 file that
   reuses boss variants by id (boss.json untouched), C3 rats not folded into spiders.

5. **C2-G unlock wiring:** PASS. `revenenenenenants` -> `SLAYER_LONGER_REVENANTS=14822` in both
   `VarbitSlayerUnlockStateProvider` and `SlayerVarbits`; 14822 is spike-verified
   (`spike-player-state.md:69` "revenants 14822"), not invented. `revenants.json` has exactly one
   EXTENSION unlock (pointsCost 100, extendedAmount krystilia 100-150). TaskUnlockIntegrityTest
   vacuity pin recount 28->29 correct; its exactly-one-EXTENSION-with-non-null-cost assertion holds.
   revenants `undead:true` correct (salve applies). revenant-caves `safeSpot:false` (singles-plus PvP,
   wiki-wins over CT-L's multi-way claim - correct).

6. **C2-H meta-task:** PASS. wilderness-bosses reuses 7 existing boss variants by id + 3 new
   singles-plus (Artio/Spindel/Calvar'ion); KBD correctly EXCLUDED (live wiki: KBD does not count when
   assigned by Krystilia); `unlocks:[]` (Like-a-boss is the global reward), so no new unlock pin.
   `weakness:null`/`monsterDefence:null` (no single profile) - correct for a meta-task.

## QA residuals (hand to QA, not review blockers)
- Real `SLAYER_TARGET` varps for synthetic ids 252-274 (live-capture).
- Revenant extension varbit 14822 value and the bossId varbit 4723 mappings for the 10 wilderness-boss
  variants (live-capture).

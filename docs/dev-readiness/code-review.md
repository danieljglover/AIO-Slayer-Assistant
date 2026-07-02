# DT-RV - Code Review: dev-readiness engagement (`d6afc4f..HEAD`, 18 commits)

Reviewer: Code Reviewer (merge gate, Stage 6). Reviewed cold - authored none of this.
Scope: the 18 committed commits `d6afc4f..HEAD` on `main` (ADR-0017 location-by-variant + loadout/location
coupling; DT-B1/D1 data + DT-B2..B11 backend + DT-FE1..FE4 frontend). Design contract: ADR-0017 +
`docs/dev-readiness/plan.md` (§DISPOSITION binds PD-1..7) + `readiness-gaps.md`.

---

## VERDICT: APPROVE-WITH-NITS (code) + 1 BLOCKER-severity MERGE-SEQUENCING condition for the DL

The **logic in all 18 commits is correct, faithful to ADR-0017 and the plan, and well-tested** - non-vacuous
tests that would fail on regression, item-ids hard-verified against the real client constants, all seven DL
dispositions implemented as specified, Gson back-compat and the ADR-0002 non-destructive re-render contract
intact. On the code itself I have **0 Blockers, 0 Shoulds, 3 Nits**.

There is **one Blocker-severity condition that is NOT a code defect but a merge-gate/release-sequencing fact**:
these 18 commits **do not compile or pass their tests in isolation** on a clean committed-HEAD checkout - they
depend on ~13 classes, the build wiring, and ~96% of the source-data tree that all live in the **uncommitted
modular-data track**. The integrated working tree (both tracks together) is green (687/0/0/0). The gate can
pass only if the modular-data track lands **atomically with (or before)** these commits. That sequencing is the
DL's to dispose; I flag it so nobody cherry-picks the 18 alone into a broken build.

Net: the engineering is merge-ready; the **commit atomicity** is the thing to get right at integration.

---

## FINDINGS

### BLOCKER (integration / merge-sequencing - process, not a code change to the diff)

**BLK-1 - The 18 commits are not independently buildable; they depend on the uncommitted modular-data track.**
- `ModularSlayerDataCompiler.java` was **created inside this scope** (commit `2416b85` DT-B1, 597 lines; grown
  to 713 by DT-B4/B5/B6) and is committed, together with its 6 test files. But **every class it references is
  uncommitted**: `ModularSlayerDataSet`, `SlayerDataValidationException`, `ModularSlayerDataCli`, and all
  `Source*` models (`SourceTask`, `SourceLocation`, `SourceMonsterVariant`, `SourceMonsterFamily`,
  `SourceStrategy`, `SourceWeapon`, `SourceMaster`, `SourceTaskVariantInfo`, `SourceTaskLocationComparison`,
  `SourceStrategyWeapon`). On a clean committed-HEAD checkout `compileJava` would fail with unresolved symbols.
- The in-scope compiler tests (`VariantLocationCompilerTest`, `MonsterVariantProfileBackfillTest`,
  `LocationSourceCompletenessTest`, `TaskLocationComparisonIntegrityTest`, `ModularSlayerDataCompilation/
  MigrationTest`) call `ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer"))`. That source tree
  is **26 of 637 files committed (~96% uncommitted)** - and no `tasks/`, `masters/`, `weapons/`, `strategies/`
  source is committed at all - so even if it compiled, `realTask("Abyssal demons")` etc. would throw
  "missing real task".
- The `build.gradle` `generateSlayerData` wiring (runs `ModularSlayerDataCli` to regenerate `slayer-data.json`)
  is **uncommitted**. At committed HEAD the runtime still reads the **old monolithic**
  `src/main/resources/data/slayer-data.json`, which carries `requiredItemId` (43) but **zero** `safeSpot` /
  `locationNames` - so even a hypothetically-compiling committed HEAD would run with **all ADR-0017 features
  dormant** (graceful additive fallback: `locationNames` null -> all-task, `safeSpot` false -> no hints).
- **Why this is not a code defect:** the DT track was correctly built on top of the in-flight modular-data
  track. The engineers' commits are clean conventional commits; the logic is right; the intended integrated
  state is green. **This is purely commit-atomicity.**
- **Suggested fix (DL / Release Manager):** land the modular-data track (the `Source*` models,
  `ModularSlayerDataCli`, `ModularSlayerDataSet`, `SlayerDataValidationException`, the `build.gradle` wiring, the
  full `src/main/data/slayer/**` source tree, and the `slayer-data.json` deletion) in the **same integration**
  as these 18 commits. Do not merge the 18 alone. Confirm the integrated `cleanTest test` is green post-merge.
- Evidence: `git ls-tree -r HEAD` vs `git status`; committed compiler at `2416b85`; committed
  `slayer-data.json` `safeSpot`/`locationNames` = 0; source-file count 26/637.

### SHOULD

None on the reviewed code. BLK-1 is the only blocking item and it is a sequencing/process condition.

### NIT

**N1 - Two different matching rules for the same variant->location subset.**
`LoadoutAdvisor.candidateLocations` matches locationNames to task locations with **exact**
`name.equals(l.getName())` (`LoadoutAdvisor.java:391`); the panel's `locationNames`/`matchesIgnoreCase` uses
**case-insensitive trimmed** match (`SlayerPanel.java:1408`). Harmless on real data (the compiler derives
`locationNames` directly from the task's own location-name strings, so they are byte-identical substrings and
both rules agree), but it is a latent inconsistency: if a hand-authored/refactored name ever drifts in case, the
dropdown and the advisor's suggested location could disagree (a suggested location absent from its own dropdown).
Suggested: share one helper or align both on the same (exact or case-insensitive) rule. Non-blocking.

**N2 - Safespot hint interpolates the raw enum constant into player-facing prose.**
`LoadoutAdvisor.java:415`: `"Safespot available - attack with " + style + " from a safe tile."` yields
"...attack with RANGED from a safe tile." The uppercase enum reads robotic. Consider title-casing
(`Ranged`/`Magic`). Matches the documented contract string and the DT-B10 test only pins the `"safespot"`
substring, so this is optional polish. Non-blocking.

**N3 - GAP-4 `range:15` (data; accepted per DL disposition #2, recorded for completeness, no action).**
The 5 backfilled variants use `monsterDefence.range = 15` to match the repo-wide sibling convention. Verified:
the runtime `MonsterDefence` model has a **single** `range` field (no dlight/dstandard/dheavy split), so the
modern wiki's split cannot be represented and the value **cannot mislead at the model level**; it is also inert
at each variant's recommended MELEE style (only a forced-RANGED estimate would see it, slightly overstated). This
is the disposed "match siblings, not fabrication" call, correctly followed. If the split ever matters it is a
repo-wide data-model item for the Architect/QA, not a defect in these commits.

---

## RISK-POINT VERIFICATIONS (as requested, with evidence)

**1. ae9f508 (DT-B5) derivation - subset invariant, determinism, empty->null FR-6 sentinel, non-vacuous.**
CONFIRMED correct. `deriveLocationNames` (`ModularSlayerDataCompiler.java:543`): boss OR empty-task-locations ->
`null` (the sentinel); `LinkedHashSet` gives insertion order + dedup; home location (`variant.locationId`, only
if in the task's `locationIds`) added first; then `variantInfo[]` rows matching this `variantId`
(`nullSafeEquals`) collected lowercased/trimmed and emitted in **task order** (iterate `taskLocationNames`,
keep those in `wanted`); `result.isEmpty() ? null`. **Subset invariant holds by construction** - every element
is sourced from `home.getName()` (guarded to be an in-task id) or from `taskLocationNames`. Determinism: no
hash-ordered iteration affects output (`wanted` is membership-only). **Test-pinned non-vacuously** by
`VariantLocationCompilerTest`: (c) unlinked and boss variants assert `null` (not empty list); the whole-dataset
invariant `everyRealDerivedLocationSubsetIsWithinItsTaskAndBossesAreEmpty` forbids empty lists (null sentinel
enforced), forbids dups, and asserts every derived name is one of its task's names; a real linked variant
(abyssal-demon -> [Abyssal Area, Slayer Tower, Wilderness Slayer Cave]) proves it is not vacuous; `(e)` pins
determinism. Boss guard verified (disposition #4): `unlinkedVariantAndBossVariantEmitNoLocationNames` asserts a
boss with a valid in-task locationId AND a matching variantInfo row still emits `null`.

**2. 3fe5058 (DT-B9) curated item-id tables - every id vs the real `net.runelite.api.ItemID`.**
CONFIRMED, zero drift. I parsed the real constant source (16,338 entries) and matched all 25 ids:
cannon parts 6/8/10/12 = `CANNON_BASE/STAND/BARRELS/FURNACE`; cannonballs 2 = `STEEL_CANNONBALL`,
21728 = `GRANITE_CANNONBALL`; antifire potions 21978/21981/21984/21987 = `SUPER_ANTIFIRE_POTION4..1`,
22209/22212/22215/22218 = `EXTENDED_SUPER_ANTIFIRE4..1`, 11951/11953/11955/11957 = `EXTENDED_ANTIFIRE4..1`,
2452/2454/2456/2458 = `ANTIFIRE_POTION4..1`; shields 22002 = `DRAGONFIRE_WARD`, 21633 =
`ANCIENT_WYVERN_SHIELD`, 11283 = `DRAGONFIRE_SHIELD`. Dose ordering (4->1) and priority (super > ext-super >
ext > regular, then shields) are correct. Raw-int literals match the established `RuneTable`/`WeaponEffect`
convention (no main code imports `ItemID`); `InventorySelectorTest` pins each id via the **named `ItemID`
constant** while the source uses raw literals, so any transcription drift fails the build. Owned-only rules
verified: `ownsCannon` requires all four parts; cannonball added only if owned (no fabricated ammo); antifire
only when `profile.isDragon()` and owned; `null` owned -> empty. `profile` is the **variant-resolved** profile
(`MonsterProfile.fromVariant`, `LoadoutAdvisor.java:86-88`), so the dragon trigger honours the per-variant flag
(FR-5).

**3. 7e6c952 (DT-B8) - selected vs suggested precedence; no combat-maths change beyond the wilderness input.**
CONFIRMED. `effectiveLocation = findLocation(selected).orElse(recommendedLocation)` (`LoadoutAdvisor.java:111`);
`wilderness = effectiveLocation != null && effectiveLocation.isWilderness()` is the **only** change fed to
`GearSelector.select` - `DpsEstimator.estimate` and every formula are untouched. `chooseLocation` was moved
above gear selection (it only needs task/variant/style/haveCannon, all available) so wilderness can feed gear;
`recommendedLocation` still flows to `setRecommendedLocation` and `locationReason`. PD-1 proven non-vacuously by
`suggestedWildernessLocationCreditsWildernessGearByDefault`: Viggora's (base 60/55) beats Dragon scimitar (70/65)
**only** with the +50% vs-Wilderness credit, so with nothing selected + a Wilderness suggested location Viggora's
wins by default; selecting the non-Wilderness location flips it back to the higher-base weapon. This is the one
intended behaviour change (disposition #1) - not flagged as a regression.

**4. requiredItemId fix (DT-B1) consumers end-to-end.**
CONFIRMED for a real task (Basilisks -> Mirror shield 4156). Compiler now emits it
(`ModularSlayerDataCompiler.java:384`). Consumers all read `getRequiredItemId()`: the Task-section required-item
row (`SlayerPanel.java:363-366`), the ownership signal `requiredItemOwned` (`AllInSlayerPlugin.java:288-290`),
the "owns X" branch of `unlockSummary` (`LoadoutAdvisor.java:483`), and the InventorySelector supply
(`InventorySelector.java:81-84`). (Note: the committed monolith already carries `requiredItemId` for 12 tasks,
so this fix restores parity for the modular-generated path specifically.)

**5. Gson back-compat of every new field + ADR-0002 non-destructive re-render.**
CONFIRMED. `SlayerLocation.safeSpot` (default false) + `accessNote` (default null) via an additive 8-arg ctor,
6-arg and 5-arg back-compat ctors preserved (`SlayerLocation.java`); `MonsterVariant.locationNames` default
null (fallback sentinel); `Recommendation.locationHint/accessNote/antifireNote` additive; `SlayerPanelState.
bankStale` additive with a delegating overload defaulting false (`SlayerPanelStateTest` pins the delegation).
An absent field reproduces today's behaviour throughout. ADR-0002: the FE1 reorder is pure vertical order
(`whereBody` moved after the location combo, still built once and reconciled in place). The loadout self-diff
key adds `state.isBankStale()` and already carries the whole `@Data` `Recommendation`, whose value-equality
covers `inventory`/`locationHint`/`accessNote`/`antifireNote` - so a value change forces a rebuild and a
value-equal render rebuilds nothing (NFR-4 zero-rebuild holds). Notes render only on non-null AND non-empty.

**Also verified against the dispositions (verify-implementation, not flag):**
- DT-B6 validator (`ModularSlayerDataCompiler.java:198`): `locationComparison[].locationId` + `variant.locationId`
  FK checks; `compile` throws `SlayerDataValidationException` on any error (aborts generation). Green on real
  data post DT-D1. GAP-5 (`d6911c2`) is author-not-repoint (9 new location files, no deletions) + a
  whole-dataset FK integrity test.
- GAP-9 (`a46e88a`) + `LocationSourceCompletenessTest` reads **raw JSON keys** (not the parsed model, which would
  silently default `false`) - the correct guard for primitive booleans.
- FE4 compact "Nd ago" (disposition #3): `AllInSlayerPlugin.bankAgeText` tiers just-now/Nm/Nh/Nd off one
  `Instant`; `bankStale` = `toDays() >= 7` (PD-5), null -> false (deferred to BANK_NOT_SCANNED). Boundaries
  pinned (6d false / 7d true) in `AllInSlayerPluginBankAgeTest`.

---

## SUITE-RUN EVIDENCE (verified independently, not on faith)

Command: `./gradlew cleanTest test --console=plain` (always `cleanTest` - bare `test` can pass the gate on a
stale cache; see reviewer MEMORY). BUILD SUCCESSFUL. XML-aggregated across **111** files in
`build/test-results/test/*.xml`:

```
tests=687  failures=0  errors=0  skipped=0
```

Matches the last handoff claim (687/0/0/0 across 111 XMLs). This is the **integrated working-tree** state
(committed 18 commits + the uncommitted modular-data track), which is the only coherent buildable state - see
BLK-1.

---

## OUT-OF-SCOPE (excluded from this review, per DL instruction)

The working tree carries **uncommitted prior-track (modular-data / ADR-0016) changes**: `build.gradle` (+26
lines of `generateSlayerData` wiring), deletion of `src/main/resources/data/slayer-data.json`, ~611 untracked
`src/main/data/slayer/**` source files, the `data/source/` `Source*` model classes + `ModularSlayerDataCli` +
`ModularSlayerDataSet` + `SlayerDataValidationException`, and modifications to 3 test files
(`DuradelDatasetValidationTest`, `MonsterVariantDatasetTest`, `SlayerDataServiceTest`). These were **not
reviewed or touched**. They are, however, the runtime/build/test substrate the 18 in-scope commits depend on -
hence BLK-1's sequencing condition. Memory `.md` files are also modified (session convention: left
flushed-to-disk-uncommitted).

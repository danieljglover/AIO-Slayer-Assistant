# Code Review - Loadout Engine Redesign (board LRV)

- Reviewer: Code Reviewer (merge gate, Stage 6)
- Date: 2026-06-29
- Scope: full Loadout Engine redesign, Waves A-E. Baseline `b510f29` (WIP: Waves A+B foundations +
  selection) through `4edf4cc` (Wave E). Reviewed the C/D/E diff AND re-read the Wave A/B
  algorithm files (`GearSelector`, `ConsumableSelector`, `RuneTable`,
  `DefaultEquipmentStatsProvider`, `DefaultConsumableEffectsProvider`) since they are load-bearing
  for plan §4/§5 and ship in this feature.
- Spec: `docs/loadout/plan.md` (§4 gear, §5 consumables, §6 bank-gate, §8 tests, §11 traceability),
  PRD `docs/loadout/prd.md`, ADRs 0001-0006.

## Verdict: APPROVE-WITH-NITS

The redesign is correct, simple, safe, and well-tested against every **functional** acceptance
criterion in plan §11. The engine algorithms match §4/§5 to the line; the bank-gate state machine
matches §6; the threading/contract invariants (FR-11, NFR-3, NFR-4) hold; the data migration is
complete; the upgrades concept is fully gone (FR-7). I read the entire feature diff, re-ran the
suite, and tried to break the selection, consumable, gate, and self-diff paths.

Two **Should** items (neither a code defect, both follow-ups) and a few Nits below. No Blockers.
The two known items the team-lead flagged (LD12 optional inject, status-in-self-diff-key) are both
assessed **acceptable** - see findings S2 and the NFR-4 note.

## Evidence

- `./gradlew cleanTest test --console=plain` -> **BUILD SUCCESSFUL**. Aggregated from
  `build/test-results/test/*.xml`: **226 tests, 0 failures, 0 errors, 0 skipped.** (First run was
  up-to-date/cached at 362ms; forced a real `cleanTest test` to confirm the count is genuine.)
- Data: `grep -c '"loadouts"' slayer-data.json` = **0**; 42 tasks; all **3** MAGIC-weakness tasks
  carry a non-null `element` (verified by parsing the JSON).
- NFR-3 fence: no `ItemManager` / `Client` / `itemstats` calls anywhere in `ui/` render paths.
  `SlayerPanel` imports `ItemManager` only to wrap it into the `ItemIconRenderer` seam in its
  `@Inject` ctor (`SlayerPanel.java:84-86`); no client method is called on the EDT.
- Leftover scan: no code references to `missingUpgrades` / `StyleLoadout` / `getLoadouts` /
  `.loadouts` in `src/main`; the only two hits are doc comments explaining the *absence*
  (`LoadoutAdvisor.java:21`, `SlayerPanel.java:987`) - intentional.

## Correctness against the plan

### §4 gear selection - `GearSelector` (FAITHFUL)
- Style from `task.weakness.style`; empty worn when style null or no weapon owned (viable iff WEAPON
  filled). `GearSelector.java:63-94`.
- Melee attack type = argmin monster defence, tie-break by best owned weapon attack for the tied
  type, final stable STAB<SLASH<CRUSH (iterate in enum order + strict `>`). `:127-152`. Correct.
- Scoring: MELEE `attack(type)+meleeStr`, RANGED `arange+rstr`, MAGIC `amagic+5*mdmg%`
  (`MAGIC_DMG_WEIGHT=5`). `:175-188`. Matches §4.3.
- Per-slot pick: DPS = score desc, tie secondary-stat desc, then id asc; COST = cheapest
  `effectivePrice` (non-positive sorted last via `Long.MAX_VALUE/4` sentinel), tie id asc.
  `:207-240`. Matches §4.5 + FR-9.
- 2h vs 1h+shield: 2h wins only when **strictly** better (`scoreA>scoreB` / `priceA<priceB`); ties
  and missing pieces degrade to 1h+shield. `:273-307`. Matches §4.6.
- AMMO (ranged) by highest rstr, tie id asc. `:104-106,222-233`. Matches §4.7.

### §5 consumables - `ConsumableSelector` (FAITHFUL)
- Food = max heal, tie lowest id; karambwan combo iff owned and != food (karambwan-only becomes the
  food). `:51-83`. Matches FR-4.
- Potion = style-matching by summed boost magnitude, dose-collapsed via `ItemVariationMapping.map`
  (try/catch -> id), winner max magnitude tie lowest id. `:87-133`. Matches FR-5.
- Magic: powered staff -> empty req + curated max hit; else highest **affordable** tier (runesShort
  empty), else strongest level-allowed tier WITH runesShort populated; staff `elementsSuppliedBy`
  zeroes that element's rune; level-too-low -> no-spell. `:168-238`. Matches FR-6 and the Wave-B
  precision note (drop-to-affordable leaves runesShort empty; populated only when no tier
  affordable). Auto-element (element==null): prefer affordable, then highest baseMaxHit, tie
  earliest element. `:189-204`. Correct.

### §6 bank-gate - `AllInSlayerPlugin.recompute` (FAITHFUL)
- Branch placed AFTER `owned`/`stats`/`requiredItemOwned` and BEFORE `loadoutAdvisor.recommend`:
  nulls the recommendation, refreshes the overlay (task + remaining, `method=null`), renders
  `bankNotScanned(...)`, returns. `AllInSlayerPlugin.java` (commit 34e5eec). Task + Where still
  render; no loadout produced pre-bank. Matches §6 + FR-1.
- Pure helper `isBankGate(taskPresent, bankLastSeen) = taskPresent && bankLastSeen==null`, unit
  tested with a 4-row table (`AllInSlayerPluginGateTest`). Correct.

### Estimator / advisor / provider
- `DefaultDpsEstimator.estimate(style, equipped, stats, task, spellBaseMaxHit)` uses the passed
  max hit for magic (`<=0` -> 0); no `task.getLoadouts()` read. Pre-existing melee/ranged maths
  retained (ADR-0006, display-only). `DefaultDpsEstimator.java`.
- `LoadoutAdvisor.recommend` orchestrates gear -> consumables -> dps, empty worn -> `Optional.empty`
  (TASK_WITHOUT_LOADOUT), sets `consumables`, never sets an upgrade list, empty inventory list.
  Location/reason/cost logic preserved. `LoadoutAdvisor.java`.
- `DefaultEquipmentStatsProvider.get` returns null for null-stats / non-equipable, caches non-null
  only (NFR-2), never caches null (NFR-5). `DefaultEquipmentStatsProvider.java`.

## Contract & threading invariants

- **FR-11 / NFR-3** (no client reads off the client thread / in UI): HELD. All `ItemManager` /
  `ItemStatChangesService` / `Client` reads are inside `recompute` (client thread); the panel
  receives only the immutable `SlayerPanelState` and draws sprites via the `ItemIconRenderer` seam.
  No client-service call in any `ui/` render path.
- **NFR-4** (value-equal Recommendation -> 0 loadout rebuilds): HELD even with the new
  `state.getStatus()` first element in the self-diff key (`SlayerPanel.java:528-530`). Status is
  stable across a value-equal re-render, so the key still compares equal and the section returns
  early. Verified by `valueEqualRecommendationWithConsumablesCausesNoLoadoutRebuild` (renders two
  distinct-but-equal states, asserts 0 added rebuilds). The status element correctly distinguishes
  BANK_NOT_SCANNED vs TASK_WITHOUT_LOADOUT (both rec==null) so they rebuild distinctly. Sound.
- **NFR-2 / NFR-5**: tested - `nonNullResultIsCachedSoTheSecondCallDoesNotRequery`
  (verify times(1)), `nullResultIsNotCachedAndRecoversOnceTheStatMapLoads` (verify times(2)).

## PRD §11 traceability spot-check

All FR-1..FR-11 and NG-1..NG-7 are met by code + real (non-vacuous) tests. Notable confirmations:
- FR-6 short-rune BLOCKED: `magicTaskRendersSpellAndRuneRowsWithShortRuneBlocked` asserts the short
  rune row is `STATE_BLOCKED` and the owned rune is `TEXT_PRIMARY`. BLOCKED is driven by
  `runesShort.containsKey(runeId)` (`SlayerPanel.java:675-676`), not quantity maths - as required.
- FR-7 no-upgrades: `noUpgradesSectionInAnyState` checks no "upgrade" text, no muted-upgrade row,
  AND (reflection) the `upgradesToShow` helper is deleted. `Recommendation.missingUpgrades` field is
  gone.
- NFR-1 (selection < 10ms for 2000 ids, warm cache): **no verifying test exists** - see S1.

## Findings

### Blockers
None.

### Should (follow-up; not blocking merge)
- **S1 - NFR-1 has no verifying test, though plan §11 mandates one.** Plan §11 maps NFR-1 to "a
  benchmark test (build a 2000-id owned set with a warm/cached provider; assert a wall-time
  budget)". No such test exists in `loadout/`. The architecture plainly supports the budget
  (selection is O(n) over `owned.ids()` with O(1) cached provider lookups), so this is low risk, but
  the criterion is currently unverified. Action: add a coarse guard benchmark, OR consciously waive
  NFR-1 in team memory / an ADR note so it is not silently dropped. (plan §11 NFR-1;
  `GearSelector.java`)
- **S2 - LD12 record contradiction on the live `ItemStatChangesService` binding.** The LD12 commit
  message (34e5eec) states the live binding is "confirmed via the manual checklist", but team memory
  (Wave A/LD00 and Wave D/LD12 entries) records it as STILL UNCONFIRMED. The **design is acceptable
  for merge** - `@Inject(optional=true)` on the field + curated fallback makes construction safe and
  degrades gracefully (`DefaultConsumableEffectsProvider.java:36-50,119-139`). The risk is purely in
  the *record*: if it is in fact unconfirmed and QA reads "confirmed", manual checks #4-5 may be
  skipped and a player with Bastion/Battlemage potions (deliberately not in the curated map) silently
  gets no potion recommendation. Action: reconcile the records and ensure QA manual #4-5 actually
  exercises the live service. (commit 34e5eec message vs the team memory record)

### Nits (author may decline with reason)
- **N1 - `(int) e.getMdmg()` truncates fractional magic-damage %** (e.g. 2.5% -> 2) in both the
  GearSelector ranking input and the estimator. Acceptable for ranking/display; worth a one-line
  comment that the truncation is intentional. (`DefaultEquipmentStatsProvider.java:52`)
- **N2 - COST-mode ranged AMMO ignores mode** (always highest rstr, never cheapest). This matches
  plan §4.7 (ammo is special-cased by rstr), so it is correct, but the asymmetry vs every other
  COST-mode slot is non-obvious; a one-line note at the AMMO branch would help the next maintainer.
  (`GearSelector.java:104-106`)
- **N3 - Curated heal values for Anglerfish / Saradomin brew are level-99 approximations** (already
  documented in-code at `DefaultConsumableEffectsProvider.java:194,199`). Fine for v1; the live
  service is authoritative when bound. No action.
- **N4 - `LoadoutItemRow.State.UPGRADE` enum constant remains** in the component API though the panel
  no longer uses it (noted intentional in team memory). Harmless; leave or prune in a later cleanup.

## Bottom line
Merge-ready. The two Should items are a missing performance-budget test (NFR-1) and a
documentation/verification reconciliation (LD12 binding) - neither is a code defect and neither
should hold the gate, but both should be closed before final QA sign-off so an acceptance criterion
and a manual check are not silently skipped. I would be happy to be paged about this code in a year.

---

## LRV2 - Gate-4 delta re-review (`4edf4cc..f2aa384`)

**Reviewer:** Code Reviewer | **Date:** 2026-06-29 | **Scope:** the Gate-4 delta landed after the LRV
approval - LFX follow-ups (`0d70fbf`, `9664e4a`), LFA consumables live-binding fix (`d579857`), LBD
design (`5601d4d`), LFB-1..4 conditional-bonus scoring (`22f4a2d`,`cdb1b79`,`d3f8deb`,`8adf535`), and
LFB-5 Est. DPS display (`f2aa384`).

### Verdict: APPROVE
**0 Blocker, 0 Should, 3 Nit.** Suite re-run by me: `./gradlew cleanTest test --console=plain` ->
BUILD SUCCESSFUL, aggregated from `build/test-results/test/*.xml` = **254 tests, 0 failures, 0 errors,
0 skipped** (forced `cleanTest`; bare `test` cache-hits). Merge-ready.

### What I verified (correct / simple / safe / tested)

**LFA - consumables `@Provides` adapter (`AllInSlayerPlugin.java:434-438`).** Root cause is sound and
the fix is minimal and correct. `ItemStatChangesService` is bound only in `ItemStatPlugin`'s child
injector, so the old `@Inject(optional=true)` field stayed null in the live client. The fix adapts the
public `ItemStatChanges` (`@Singleton`, no-arg ctor -> Guice JIT-constructs in our injector) to the
service's single method via `itemStatChanges::get`. **Threading is clean:** the `@Provides` body only
returns a method reference - it makes no client call, so no EDT leak is introduced. The live read
`Effect.calculate(client)` stays where it already was (provider methods, client thread; NFR-3 holds).
The provider ctor is now correctly non-optional (`DefaultConsumableEffectsProvider.java:40-46`); the
old field-inject + secondary test ctor are gone, simplifying construction.

**LFA - widened curated data (`DefaultConsumableEffectsProvider.java:213-228`).** Bastion (22461,
DEF+RANGED), Battlemage (22449, DEF+MAGIC) and the divine variants added, keyed by the 4-dose
canonical. The "doses DO collapse to one `ItemVariationMapping` canonical in 1.12.31.1" correction is
proven non-vacuously by `curatedBastionBoostsDefenceAndRangedForEveryDose` /
`curatedBattlemageBoostsDefenceAndMagicForEveryDose`, which exercise all four doses each through the
null-service path. Magnitudes are level-99 approximations (ranking only) - acceptable, consistent with
the existing curated entries.

**LFB - the conditional-bonus model.** The additive term is sound and matches design §3.2 exactly.
`GearSelector` (`GearSelector.java:71-110, 216-227`) does Pass A (flat base score per item +
`maxBaseBySlot`) then `L = sum(max(0, slotMax))`, then Pass B `eff = base + round((m-1)*L)`, keeping
`eff > 0` so a 0-flat Salve survives the filter. `stats.get(id)` is still called once per id (Scored
reused) and `registry.lookup` once per item - O(n), NFR-1 preserved. The selector/estimator asymmetry
is **intentional and correct, not a bug:** the selector credits each slot its own per-slot lift
(right for picking the best helm AND the best amulet independently, §3.4), while the estimator
(`DefaultDpsEstimator.java:102-123`) takes `Math.max` over equipped items - never the product - so the
displayed DPS uses only the higher of black-mask vs salve (no double-credit, §3.4/3.5). The no-stack
rule is proven by `helmAndSalveDoNotStackOnlyTheHigherMultiplierApplies` (dpsBoth == dpsSalveOnly,
dpsBoth > dpsHelmOnly via identical-stat no-bonus controls). The estimator applies `m` to both the
attack roll and the max hit, which is how these bonuses behave in OSRS.

**LFB - multipliers + raw-id registry.** Sanity-checked every registry entry against design §2:
Salve (e)/(ei) = x1.20, Salve base/(i) melee = x1.1667, imbued ranged/magic = x1.15, black mask /
slayer helm melee = x1.1667 (plain helm DOES give the melee bonus - it contains a black mask). All
verified by `ConditionalBonusRegistryTest`. The **raw-id (not canonical-collapse) keying is correct
and necessary:** `ItemVariationMapping.map()` collapses imbued+non-imbued to one canonical, erasing the
imbue state that carries the +15% ranged/magic and the +20% enchant - the registry test proves charge
states collapse to one entry while imbued recolours (Hydra helm (i)) keep the imbued bonus. This is a
deliberate, documented divergence from the consumables path (which only needs dose-collapse).

**Undead data.** `slayer-data.json` marks exactly Aberrant spectres (line 53) and Ankou (line 75) -
confirmed by name. `undeadTasksAreExactlyAnkouAndAberrantSpectres` asserts both true, three
representatives false (Skeletal wyverns, Vampyres, Abyssal demons), and a count of exactly 2 (guards
accidental adds). `undeadFlagRoundTripsAndDefaultsFalseWhenAbsent` confirms Gson back-compat.

**Invariants.** NFR-3: no new client call on the EDT (the `@Provides` is pure; estimator reads only
the equipped map + static registry + TaskData). NFR-4: the estimator stays pure and deterministic -
the Est. DPS is still one derived double, so a value-equal Recommendation still renders equal -> 0
rebuilds. FR-11 unaffected. NFR-1: the extra Pass A is O(n) and guarded by the (re-run green)
`selectionOverTwoThousandOwnedIdsStaysWithinTheWarmCacheBudget` benchmark (closes the old S1).

**ADR-0007.** Properly supersedes the NG-6 blanket non-goal (narrowed to the named families;
Void/crystal/coupling stay out), states the two accepted simplifications (per-slot double-credit in
selection; display understatement before LFB-5) as explicit consequences, and is recorded as amending
ADR-0001 given the data-model + dataset + scorer reversal cost. Sound.

**Prior LRV nits closed by this delta:** N1 (mdmg truncation) now has the intent comment
(`DefaultEquipmentStatsProvider.java:50-51`); N2 (AMMO ignores mode) now has the note
(`GearSelector.java:128-129`).

### Findings

#### Nits (author may decline with reason)
- **N1 (LRV2) - estimator scales the final max hit, not the effective level.** The on-task multiplier
  is applied as `floor(baseMaxHit) * m` and `atkRoll * m` (`DefaultDpsEstimator.java:64,68,80,81,91,93`).
  OSRS applies black-mask/salve to the effective strength/attack *level* before the max-hit floor, so
  the displayed number can differ by <=1 from the in-game tooltip. Display-only, and this is the
  pre-existing pattern (the old `helm` factor did the same) - no regression. No action.
- **N2 (LRV2) - `1.1667` literal vs exact `7.0/6.0`.** `ConditionalBonusRegistry.ON_TASK = 1.1667`
  (`ConditionalBonusRegistry.java:24`) is 7/6 rounded to 4dp; the prior estimator used `7.0/6.0`
  exactly. Negligible for ranking/display. No action.
- **N3 (LRV2) - the wiring test exercises the adapter directly, not through the plugin's Guice
  module.** `AllInSlayerPluginWiringTest` calls `new AllInSlayerPlugin().provideItemStatChangesService(...)`
  and JIT-constructs `ItemStatChanges` separately - this proves the adapter logic + live data
  recognition headlessly, but the final "RuneLite actually invokes `@Provides` for our plugin
  injector at runtime" step remains live-only (standard framework behaviour, same mechanism as
  `provideConfig`). Inherent to headless testing; covered by manual checklist plan §9 #4-5. The live
  boost-magnitude maths (`Effect.calculate(client)`) is likewise the only thing left to manual. No
  code action; carried as a QA action.

### Bottom line
Clean approve. The LFA fix is the right pattern (`@Provides`-adapt a service bound in another plugin's
injector), the conditional-bonus model is faithful to ADR-0007/§3.2 with the selector/estimator
asymmetry correctly intentional, the multipliers and undead set are right, and every claim is backed
by a non-vacuous test. The only residuals are inherent live-client checks already on the QA manual
checklist. I would be happy to be paged about this code in a year.

---

## WRV - Weapon-DPS delta (WDB-1..17 + WDBX): `f2aa384..HEAD` (HEAD `2aca34a`) -> APPROVE

**Reviewer:** Code Reviewer. **Date:** 2026-06-29. **Scope:** the merge gate for the speed-aware
weapon-DPS engine + special-weapon effects + monster-category bonuses + magic-weapon DPS. Code commits
`bceae38..de85846` (WDB-1..17) plus `e5f9a36` (WDBX dragon-set fix). Reviewed against
`weapon-dps-design.md`/ADR-0008 (v1) and `weapon-dps-design-v2.md`/ADR-0009 (v2), and the weapon
references.

**Verdict: APPROVE. 0 Blocker / 0 Should / 2 Nit (both no-action).**

**Suite re-run by me (forced cleanTest):** `./gradlew cleanTest test --console=plain` -> BUILD
SUCCESSFUL; aggregated `build/test-results/test/*.xml` across 44 files = **305 tests, 0 failures /
0 errors / 0 skipped** (was 254 at LRV2; +51). Confirmed, not taken on faith.

### What I verified (all faithful to the designs)

- **Speed-aware ranking.** `weaponDps(...)` is a clean extraction over `meleeCore`/`rangedCore` (the
  same shared `dps()` core as `estimate()`); the WEAPON slot (MELEE/RANGED) is routed through the
  `DpsEstimator.weaponDps` seam in `resolveWeaponAndShield`/`weaponRank`; non-weapon slots stay
  flat-scored (`bySlot` pass B, weapon slot explicitly skipped, `GearSelector.java:119`); AMMO still by
  `rangedStr` via `pickAmmo`. The Fang>AGS case is real and non-vacuous:
  `fasterHigherDpsWeaponBeatsSlowerHigherMaxHitWeapon` (6t/130s loses to 4t/82s) +
  `fangAccuracyRerollOutranksAHigherMaxHitWeaponOnAHighDefenceMonster` (high-def so neither saturates).
- **The clean three.** `WeaponEffectRegistry`: Fang `(accuracyRolls=2, damageMultiplier=1.0)` - NO
  damage multiplier (variance-only band confirmed, no one slipped a 1.x in); Scythe `(1, 1.75)`; DHL via
  `ConditionalBonusRegistry` symmetric `VS_DRAGON` 1.20. Uncharged Fang/Scythe ids deliberately excluded
  (fall through to NONE). `dps()` applies `1-(1-p)^rolls` then `*damageMultiplier` exactly per design 3.3.
- **Monster-category sets exact** (pinned by `DuradelDatasetValidationTest`, non-vacuous with negative
  controls): demon = exactly {Abyssal demons, Black demons, Greater demons, Nechryael} (count 4, with
  Hellhounds/Smoke devils/Dust devils asserted false); kalphite = exactly {Kalphite} (count 1); draconic
  = exactly the 8-task set incl. Wyrms+Drakes (WDBX correction, matches OSRS Dragonbane_weapons);
  Wilderness location = exactly {Ankou}. Silverlight excluded from the registry (DEC-7); Arclight active
  form only (inactive 30305 excluded).
- **Acc/dmg split + Keris.** `ConditionalBonus` carries per-style `{acc,dmg}`; `symmetric()` keeps all
  gear + DHL + demonbane + Wilderness byte-equivalent; Keris is the lone `asymmetric()` row (melee
  acc=1.0, dmg=1.382 = `1.33*53/51` EV of the 1/51 triple) - damage-only, no accuracy credit. The
  invariant is structurally guarded: `asymmetricBonusesLiveOnlyOnWeaponSlotItems` iterates the whole
  registry and fails any non-symmetric entry outside the Keris allowlist. `bonusTerm` (the additive
  non-weapon path) reads `dmgMultiplier` and only ever sees symmetric rows.
- **Magic DPS.** `MagicWeaponEvaluator` is cleanly extracted from `ConsumableSelector.buildMagic` (now a
  one-line delegate) and called by BOTH `ConsumableSelector` and `GearSelector` - one source of truth,
  no duplication, no order inversion, no cycle (the evaluator depends only on `RuneTable`/`OwnedItems`/
  `PlayerStats`). Ranking is gear-aware: non-weapon magic gear is picked first (weapon-independent flat
  score, no cycle) then summed into each candidate. Tumeken's Shadow: `matt = weapon.amagic + 3*gearMatt`,
  `mdmg = weapon.mdmg + min(100, 3*gearMdmg)` in `MagicWeaponEvaluator.effective*`; the display estimator
  mirrors it via the same statics. `tumekensShadowWinsViaGearMultiplier` proves the win is VIA the gear
  multiplier (wand wins without gear); `tumekensShadowTriplesGearAndCapsDamageAtOneHundred` proves the
  +100 cap (3*40=120->100) and the under-cap case (3*30=90).
- **No double-counting.** Weapon slot excluded from the additive `(m-1)*L` term (pass B skip); a weapon's
  own conditional is applied multiplicatively in `weaponRank` (split acc/dmg). The display estimator's
  higher-of `m` (LFB-5) stays symmetric (gear sources) and is orthogonal to the `WeaponEffect`.
- **Invariants.** NFR-3: estimator/selector/evaluator are pure over resolved `Bonuses`/`OwnedItems`/
  `PlayerStats` + static registries; no `ItemManager`/EDT. NFR-4: outputs are value types/EnumMaps;
  unchanged. NFR-1: the warm-cache 2000-id benchmark is retained and green.
- **Wilderness (WDB-17).** `SlayerLocation.wilderness` (back-compat 5-arg ctor kept) -> threaded by
  `LoadoutAdvisor` from the selected location into `GearSelector.select(..., wilderness)` ->
  `BonusContext.from(task, wilderness)`. Selection is correct (`wildernessWeaponRanksUpWhenWilderness
  LocationSelected` with in/out controls). The **display limitation is real and acceptable**:
  `estimate()` calls `BonusContext.from(task)` (wilderness=false, `DefaultDpsEstimator.java:66`) because
  it lacks the selected location, so Viggora's is correctly RECOMMENDED on Ankou-Wilderness but its shown
  Est. DPS understates the +50%. Documented, outside FR-14.11; not a selection bug.
- **Scorching bow latent** - confirmed modelled (`SCORCHING_BOW` ranged 1.30) + synthetically tested
  (`scorchingBowRanksUpOnASyntheticDemonRangedTask`). All real demon tasks are MELEE, so it never fires
  live - a data-reality limitation, not a code gap.

### Findings

- **N1 (Nit, no-action, pre-existing): magic display speed for standard casts.** The display `estimate()`
  MAGIC branch uses the worn weapon's `attackSpeedTicks` (`DefaultDpsEstimator.java:95`), whereas the
  selection path correctly uses `MagicWeaponEvaluator.castSpeedTicks` (5t for a standard cast, not the
  wand's melee speed). For a non-powered-staff magic weapon this is a minor selection/display speed
  asymmetry. **Pre-existing** (the magic branch used `weaponSpeed` before WDB too - verified at
  `f2aa384`), display-only, and moot for the common case (recommended magic weapons are powered staves,
  where `attackSpeedTicks` == the cast speed, so display and selection agree). Fix only if a standard-cast
  magic weapon ever becomes a recommended pick AND the shown number is questioned.
- **N2 (Nit, no-action): redundant per-slot pick during magic ranking.** `magicGearContext` calls
  `pick()` for each non-weapon/non-shield slot to build the gear context, and the fill loop calls `pick()`
  again for the same slots (`GearSelector.java:463` + `:159`). Deterministic and consistent (same winner
  both times, so the assumed gear == the worn gear), just a small redundant computation over the few magic
  slots. Not worth the added state.

### Assessed and accepted (not findings)

- The Wilderness display understatement (N/A above) - documented limitation, ranking correct.
- Scorching bow latency - data-reality, synthetically covered.
- Magic ranking gear-aware while melee/ranged stay weapon-only - intentional asymmetry (ADR-0009 B.3),
  justified because magic armour is weapon-independent and the Shadow forces it.

Clean approve. The DPS engine is a faithful, surgical realisation of ADR-0008/0009: the weapon slot now
ranks on real sustained DPS, the special-effect homes are correctly split by shape (formula-shape ->
`WeaponEffect`, per-task scalar -> `ConditionalBonus`), the acc/dmg split is minimal and invariant-guarded,
the magic evaluator is shared with no duplication, and every acceptance criterion (FR-13.x/FR-14.x) maps to
a non-vacuous test with proper controls. The only live residuals are the QA manual checks already listed in
the WDB team-memory entry. I would be happy to be paged about this code in a year.

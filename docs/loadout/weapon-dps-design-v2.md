# Weapon-DPS v2 - monster-category bonuses + magic-weapon DPS - design

> Status: design (user pulled BOTH v1 deferrals - magic DPS + monster-category weapons - into scope).
> Board task **WDB** (extends the WDB sub-tasks). Builds on **ADR-0008** (DPS weapon selection) and
> **ADR-0007** (task-conditional gear bonuses); recorded by **ADR-0009**. This EXTENDS the v1 design
> (`weapon-dps-design.md`); it does not rewrite it. Anything not restated here is unchanged from v1.
> Design only - no production code. Build is TDD, single-owner per hot file.

## 0. What v1 left out and v2 pulls in

ADR-0008 shipped melee+ranged DPS ranking + the "clean three" (Fang, Scythe, Dragon hunter lance) and
**deferred** two things as documented v1 boundaries:

- **DEC-2:** magic weapon DPS (the spell is chosen downstream of gear - the spell-ordering boundary)
  and Tumeken's Shadow (multiplies *other* gear).
- **§6 of WR1 / ADR-0008 limitations:** monster-CATEGORY conditional weapons (demon / kalphite /
  Wilderness) - the dataset carries only `undead` + `dragon` predicates.

The user has approved both. This doc designs:

- **Extension A** - monster-category conditional bonuses (demon, kalphite; Wilderness surfaced as a
  decision), reusing and generalising the LFB `ConditionalBonusRegistry`.
- **Extension B** - magic-weapon DPS ranking + the spell-ordering rework, including Tumeken's Shadow's
  gear-multiplier.

All numbers below are taken from the verified weapon-reference files (`weapon-reference-1h.md`,
`weapon-reference-2h.md`, `weapon-reference.md` §4); deltas vs my earlier notes are called out.

---

## Extension A - monster-category conditional bonuses

### A.1 The registry already fits - generalise the predicate set and (for one case) the multiplier shape

LFB's `ConditionalBonusRegistry` (ADR-0007) is `item id -> {BonusCondition predicate, per-style
multiplier}`, with predicates `ON_SLAYER_TASK` / `VS_UNDEAD` and (from ADR-0008/WDB-7) `VS_DRAGON`.
Two changes cover the category weapons:

1. **New predicates** `VS_DEMON`, `VS_KALPHITE` (and, if approved, `VS_WILDERNESS` - see A.5), each a
   `BonusContext` flag fed from `TaskData` exactly like `undead`/`dragon`.
2. **An accuracy/damage split of `ConditionalBonus`** (this is the deferred DEC-3 work, now needed for
   Keris). Today a `ConditionalBonus` is ONE multiplier per style applied to both accuracy and max hit
   (correct for every *symmetric* source: salve, black-mask, DHL, and every demonbane below). Keris is
   **damage-only** (no accuracy bonus), so it does not fit one scalar. We split each per-style
   multiplier into `{accuracy, damage}`. All existing rows are symmetric, so a `symmetric(...)`
   factory keeps them byte-for-byte equivalent (acc == dmg); only Keris (and, later, DHCB / dragon
   hunter wand) uses the asymmetric constructor.

```
ConditionalBonus {
  BonusCondition condition;
  // per style, two multipliers; symmetric sources set accuracy == damage
  double meleeAcc, meleeDmg;
  double rangedAcc, rangedDmg;
  double magicAcc,  magicDmg;
  double accMultiplier(CombatStyle);   // 1.0 if unset
  double dmgMultiplier(CombatStyle);   // 1.0 if unset
  static ConditionalBonus symmetric(condition, melee, ranged, magic); // acc==dmg per style
}
```

`weaponDps` (ADR-0008 §3.3) currently takes one `conditionalMultiplier` applied to both rolls; it now
takes **two** (`accMult`, `dmgMult`):

```
hitChance = 1 - (1 - p(accMult))^effect.accuracyRolls   // accMult scales the accuracy roll
avgHit    = hitChance * (maxHit(dmgMult) / 2) * effect.damageMultiplier  // dmgMult scales max hit
```

For symmetric sources `accMult == dmgMult` and the maths is identical to v1. **The display estimator's
`conditionalMultiplier` higher-of logic (LFB-5) stays symmetric** (gear sources only), so it is
unaffected; the split is consumed only by `weaponDps` and the estimator's *weapon* effect path.

> **Invariant (test-guarded):** an asymmetric `ConditionalBonus` may live ONLY on a weapon-slot item.
> The non-weapon additive-`(m-1)*L` term (ADR-0007 §3.2) uses a single symmetric multiplier; placing an
> asymmetric source on armour would be ill-defined. A registry test asserts every non-weapon row is
> symmetric.

### A.2 Demonbane (symmetric - no model change beyond the predicate)

All confirmed demonbane weapons give **equal accuracy and damage** % vs demons, so they drop straight
into the existing symmetric model. Exact values + speeds from the reference (speeds read live from
`Bonuses.attackSpeedTicks`, NOT the registry):

| Weapon | Style | Speed | `VS_DEMON` multiplier (acc / dmg) | Source |
|---|---|---|---|---|
| Arclight | melee | 4t | **1.70 / 1.70** | "+70% accuracy & damage vs demons" (WR-1h:53) |
| Emberlight | melee | 4t | **1.70 / 1.70** | "+70% ... vs demons" - same %s as Arclight (WR.md:208,363) |
| Darklight | melee | 5t | **1.60 / 1.60** | "+60% acc / +60% dmg vs demons" (WR.md:209) |
| Scorching bow | ranged | 5t (rapid) | ranged **1.30 / 1.30** | "+30% accuracy & damage vs demons" (WR-2h:245) |

Notes / decisions:

- **Reduced-vs-boss (Duke Sucellus 49%) is NOT modelled.** No dataset task is Duke; the per-boss
  reduction is a `Boss`-roll concern out of scope. If `Boss` ever resolves to Duke the model
  over-credits Arclight there - documented limitation, same shape as Scythe-vs-1-tile.
- **Emberlight's distinct on-hit defence drain is its SPEC**, not a sustained passive; its demonbane %
  is Arclight's. Modelled identically (1.70). (WR.md:363 - distinct passive not confirmed.)
- **Silverlight is EXCLUDED from v1 (DEC-7).** The reference gives only "Bonus damage vs demons" with
  **no numeric %** and damage-only wording (WR-1h:225). Do not guess; add when a % is verified.
- **Scorching bow is LATENT on the real dataset (accuracy note).** Every demon task in `slayer-data.json`
  is **MELEE** weakness (A.4), and `GearSelector` ranks only the weakness style's weapons. So the
  ranged demonbane row is correct and unit-tested with a *synthetic* `demon + RANGED` task, but **no
  current task selects it**. It is built for correctness/future tasks, not because a live task needs it.

### A.3 Kalphite (Keris - asymmetric + a proc)

Keris and the Keris partisan family give **+33% damage** vs Kalphites/scarabs with **no accuracy
bonus**, plus a **1/51 chance to deal triple damage** (WR-1h:160-167, WR.md:210). Asymmetric -> needs
the A.1 split.

- **Base multiplier:** `VS_KALPHITE` melee `accuracy = 1.0`, `damage = 1.33`.
- **The 1/51 triple proc - DEC-5, recommend model as an expected-value term.** On a hit, a 1/51 chance
  multiplies that hit by 3, so the steady-state damage expectation is
  `1.33 * ((50/51)*1 + (1/51)*3) = 1.33 * 53/51 ≈ 1.382`. This is a single deterministic constant,
  improves the displayed DPS, and is the natural fit for a sustained-DPS engine. **Recommended:**
  `damage = 1.382`. Alternative (also acceptable): model `1.33` and note the proc is ignored - the
  ranking outcome (Keris up on kalphite tasks) is identical either way, so this is low-stakes.
- **Variants:** the of-amascut / breaching / corruption / of-the-sun partisans all share **+33% vs
  kalphites**; their unique extras are ToA-only or specs (out of scope). One registry row value, listed
  per id. (WR.md:367 - per-variant %s not enumerated; +33% is the common, confirmed figure.)
- **Data-source caveat:** the reference's *base* Keris rows list only the proc, attributing +33% to the
  partisans; the OSRS effect is the same +33% on base Keris too. Model the whole family at +33%; **pin
  exact ids at build** and verify base-vs-partisan parity against the live client.

Keris fits the `WeaponEffect.damageMultiplier` field shape (acc unchanged, dmg scaled) but
`WeaponEffect` is UNCONDITIONAL (applies on every task) - wrong for a kalphite-only bonus. Hence the
asymmetric `ConditionalBonus` is the correct home, not `WeaponEffect`.

### A.4 Category data - the flags and the EXACT task sets (verified against slayer-data.json)

Add per-task `boolean demon`, `boolean kalphite` to `model/TaskData` and `resources/data/slayer-data.json`,
modelled exactly like the existing `undead` / `dragon` flags (Lombok `is...()`, Gson default `false`,
flag present only on true tasks). The sets below were verified against the 42-task dataset by
monster attribute (OSRS demon / kalphite attributes), not by name:

- **`demon = true` (exactly 4):** **Abyssal demons** (Abyssal demon, Greater abyssal demon),
  **Black demons** (Black demon), **Greater demons** (Greater demon + Greater Nechryael - both demons),
  **Nechryael** (Nechryael + Greater Nechryael - demons).
- **NOT demons (called out because they look it):** **Hellhounds** (no demon attribute - Arclight does
  NOT work), **Smoke devils**, **Dust devils** (the "devil" name is not the demon attribute),
  **Spiritual creatures**. `Boss` is a generic placeholder - left unflagged (could roll a demon boss,
  but the row is not a single monster).
- **`kalphite = true` (exactly 1):** **Kalphite** (Kalphite Worker / Soldier / Guardian). No scarab
  task exists in the dataset.

Validation-test expectations (`DuradelDatasetValidationTest`):
- `demon` true for exactly {Abyssal demons, Black demons, Greater demons, Nechryael}; count == 4;
  `Hellhounds`, `Smoke devils`, `Dust devils` are false.
- `kalphite` true for exactly {Kalphite}; count == 1.
- `TaskDataJsonTest`: a task with `"demon": true` / `"kalphite": true` round-trips; absent key -> false.

### A.5 Wilderness - DECISION REQUIRED (DEC-4), recommend DEFER

The Wilderness weapons (Viggora's chainmace, Craw's bow, Webweaver bow, Thammaron's sceptre) give +50%
acc & dmg **vs Wilderness NPCs** - **symmetric, would fit `ConditionalBonus` trivially.** The hard part
is the *predicate*: **Wilderness is a LOCATION, not a monster category.** The same monster (e.g. a Black
demon) gets the bonus in the Wilderness and not in Taverley Dungeon, so a per-task flag is **wrong** -
it would credit the weapon on every Black-demon task regardless of where the kill happens.

Three options, with the disqualifier for each:

| Option | Mechanism | Verdict |
|---|---|---|
| Per-task `wilderness` flag | flag tasks "done in the Wilderness" | **Wrong** - over-credits; most are doable outside the Wilderness. |
| Live location read | read the player's region at recompute | **Wrong timing** - the loadout is computed at bank-scan time (player at a bank, not in the Wilderness), so it would read "not Wilderness" and never recommend the weapon even when the user intends a Wilderness kill. |
| **Per-LOCATION flag + selected-location** | add `wilderness` to specific `locations[]` entries; bonus applies only when the user has selected a Wilderness location | **Correct, but cross-cutting** - needs a dataset location-schema add AND threading the selected location's wilderness state into `BonusContext` (today `GearSelector.select(owned, task, stats, mode)` does not receive the selected location; `LoadoutAdvisor` does). |

**Recommendation: DEFER Wilderness to a follow-up (DEC-4).** Reasons: (1) only the per-location option
is correct and it is the only one that touches the advisor signature + dataset location schema + UI
selection; (2) the payoff is tiny on THIS dataset - **only `Ankou` carries a Wilderness location entry**
(`"Wilderness (Ankou area)"`); the krystilia-assignable tasks (Abyssal/Black/Greater demons, Hellhounds,
Ankou) list non-Wilderness locations, so the bonus would fire on at most one task. If the user wants it,
the per-location design is specified above and is **+1 sub-task** (WDB-17) plus a `GearSelector.select`
signature change to accept the selected location's `wilderness` flag (threaded from `LoadoutAdvisor`,
which already holds `selectedLocation`). Demon and kalphite are clean and proceed regardless.

---

## Extension B - magic-weapon DPS + spell ordering

### B.1 The boundary, precisely

`LoadoutAdvisor` selects **gear first** (`GearSelector.select`) then the spell (`ConsumableSelector
.buildMagic` -> `MagicSetup`, given the chosen `weaponId`). The MAGIC weapon slot is still flat-scored
`amagic + 5*mdmg` (v1 DEC-2) because ranking magic weapons by DPS needs the spell's base max hit, which
is only known after the weapon is chosen. To rank by DPS we must know each candidate's effective base
max hit **during** weapon ranking, without duplicating or inverting the spell logic.

Good news from the code: `ConsumableSelector.buildMagic(owned, element, player, weaponId)` ALREADY
computes exactly "given this weapon, the best castable spell or powered-staff base max hit", returning a
`MagicSetup{spellBaseMaxHit, poweredStaff, ...}`. We reuse it.

### B.2 Extract a shared `MagicWeaponEvaluator` (one source of truth - no duplication)

Move the magic spell/staff resolution out of `ConsumableSelector` into a shared seam so both the
selector (final consumables) and `GearSelector` (ranking) call ONE implementation:

- New `MagicWeaponEvaluator` (or static methods on `RuneTable`, mirroring its all-static idiom) with
  `MagicProfile evaluate(int weaponId, OwnedItems owned, PlayerStats player, String element)` returning
  `{int baseMaxHit, int speedTicks, boolean poweredStaff}`:
  - powered staff -> `baseMaxHit = RuneTable.poweredStaffMaxHit(id)`, `speedTicks = Bonuses
    .attackSpeedTicks` (powered staves carry their cast speed there - 4t tridents/sang, 5t Shadow),
    `poweredStaff = true`.
  - standard caster / non-staff -> `baseMaxHit` = the best affordable tier's base max hit (the existing
    `buildMagicForElement` logic: `tiersDescending`, level gate, affordability over owned runes,
    element supplied by the staff), `speedTicks = 5` (standard spellbook / Ancients autocast at 5t -
    NOT the weapon's melee `attackSpeedTicks`, which is wrong for a cast), `poweredStaff = false`.
- `ConsumableSelector.buildMagic` becomes a thin caller of the evaluator (plus it still assembles the
  rune `requirement` / `runesShort` for the final `MagicSetup`). **Behaviour is unchanged** -
  characterization tests on `ConsumableSelectorTest` stay green. This is a pure refactor; it is the
  prerequisite that lets `GearSelector` reuse the logic instead of duplicating `RuneTable` handling.

### B.3 Magic weapon ranking is GEAR-AWARE (DEC-6) - the principled break from weapon-only

Melee/ranged DPS is weapon-only (ADR-0008 §3.2) for order-independence. **Magic cannot be weapon-only**
because Tumeken's Shadow's value comes entirely from multiplying *other* gear. Resolution:

1. **Select non-weapon magic slots first**, by the existing flat `amagic + 5*mdmg` score (UNCHANGED).
   This is **weapon-independent** (armour mdmg/matt do not depend on the weapon), so there is no cycle
   and the order is well-defined. Sum the chosen non-weapon pieces into `gearMatt` (Σ amagic) and
   `gearMdmg` (Σ magicDmgPercent).
2. **Rank candidate magic weapons** by a gear-aware magic DPS:
   ```
   profile  = MagicWeaponEvaluator.evaluate(weaponId, owned, player, element)
   matt     = weapon.amagic + gearMatt
   mdmg     = weapon.mdmg   + gearMdmg
   maxHit   = profile.baseMaxHit * (1 + mdmg/100.0)
   p        = magicHitChance( (player.magic+8) * (matt+64),  (def.defLevel+9) * (def.magic+64) )
   dps      = p * (maxHit/2.0) / (profile.speedTicks * 0.6)
   ```
   Pick the highest-DPS magic weapon. `GearSelector` reuses the estimator's magic core for this (same
   `hitChance` / `dps` helpers), so display and selection share one formula.
3. After the weapon is chosen, `LoadoutAdvisor` -> `ConsumableSelector` runs **as today** on the chosen
   weapon to produce the final `MagicSetup` (runes, shortfall). The evaluator is called per candidate
   during ranking and once more for the final setup - cheap, consistent, no duplicated logic.

This makes magic ranking gear-aware while melee/ranged stay weapon-only - asymmetric but justified:
magic armour is weapon-independent (so gear-first is sound) and the Shadow forces it. Documented in
ADR-0009.

### B.4 Tumeken's Shadow - the gear multiplier, specified precisely

The Shadow (powered, 5t, `baseMaxHit = floor(Magic/3)+1 -> 34@99`, own +35 magic attack) **multiplies
the magic-attack and magic-damage bonuses of OTHER worn gear by x3** (x4 inside ToA - not Slayer), with
the **magic-damage bonus capped at +100%** (WR.md:150,205; WR-2h:286,307). It is a multiplier on the
GEAR contribution, not a flat weapon bonus - exactly why weapon-only could not see it.

Model it as a special case in the magic ranking (B.3 step 2) and the display estimator:

```
isShadow = (weaponId is Tumeken's Shadow)
matt = weapon.amagic + (isShadow ? 3 * gearMatt : gearMatt)
mdmg = weapon.mdmg   + (isShadow ? Math.min(100, 3 * gearMdmg) : gearMdmg)
```

- The x3 applies to the **gear** matt/mdmg only; the Shadow's own +35 matt and 0% mdmg are added once.
- The cap is on the magic-**damage** bonus: `min(100, 3 * gearMdmg)` (percent points). Accuracy
  (`matt`) has no stated cap; it saturates in `hitChance` anyway.
- **Verify-before-hardcode:** confirm the cap is `+100%` on the *tripled* total (not on gear pre-triple)
  and that the x3 excludes the Shadow's own bonus, against the live infobox. Flagged, not blocking.

Because the Shadow's advantage is in the gear term, it only wins at meaningful magic gear - which is the
real-game behaviour and the point of the acceptance test (B.6 FR-14.8).

### B.5 Display Est. DPS reflects the magic weapon + Shadow

`DefaultDpsEstimator.estimate` MAGIC branch already sums worn matt/mdmg and uses `spellBaseMaxHit`.
Extend it: when the worn weapon is the Shadow, **triple the non-weapon gear's matt and mdmg** (cap mdmg)
before computing - mirroring B.4 - so the shown DPS matches the pick rationale. The `spellBaseMaxHit`
passed in already comes from the chosen weapon's `MagicSetup`, so powered-staff base max hits flow
through unchanged. NFR-3 (estimator pure, client reads stay in recompute) and NFR-4 (value-equal
`Recommendation` -> 0 rebuilds) are preserved - all inputs are already-resolved value types.

### B.6 Reconciliation / invariants

- **No duplication:** the spell/staff/affordability logic lives once in `MagicWeaponEvaluator`;
  `ConsumableSelector` and `GearSelector` both call it. The final rune selection stays in
  `ConsumableSelector`.
- **Client-thread-pure / no-EDT:** everything operates on resolved `Bonuses`, `OwnedItems`,
  `PlayerStats`, and static `RuneTable` - no `ItemManager` / EDT (NFR-3).
- **Powered vs standard speed:** powered staves use `attackSpeedTicks`; standard casters use 5t (the
  cast speed), not the wand's melee speed - fixed in the evaluator.

---

## Merged, dependency-ordered TDD build breakdown (task WDB)

WDB-1..8 are the v1 sub-tasks (`weapon-dps-design.md` §5), unchanged. WDB-9..16 are v2. Each starts
red. Hot files are single-owner/sequential: `GearSelector.java`, `DefaultDpsEstimator.java`/
`DpsEstimator.java`, `ConditionalBonus.java`/`ConditionalBonusRegistry.java`/`BonusCondition.java`/
`BonusContext.java`, `ConsumableSelector.java`, `RuneTable.java`/new `MagicWeaponEvaluator.java`,
`model/TaskData.java`, `resources/data/slayer-data.json`.

| Sub-task | Red-first assertion | Owns (files) | Depends on |
|---|---|---|---|
| WDB-1..6 | (v1) DPS core + `weaponDps` + WeaponEffect + DPS weapon ranking + 2h/shield + COST | (v1) | - |
| WDB-7 | (v1) Dragonbane via `VS_DRAGON` (DHL) + dragon flag | (v1) | WDB-1..4 |
| WDB-8 | (v1) Display reflects worn weapon effect | `DefaultDpsEstimator` | WDB-2/3 |
| **WDB-9** Category data | `demonTasksAreExactly{Abyssal demons,Black demons,Greater demons,Nechryael}` (count 4) + `kalphiteTasksAreExactly{Kalphite}` (count 1) + JSON round-trip | `model/TaskData`, `slayer-data.json`, `DuradelDatasetValidationTest`, `TaskDataJsonTest` | - |
| **WDB-10** Demon predicate + symmetric demonbane rows | `arclightOutranksScimitarOnADemonMeleeTask` + `demonbaneGivesNoBonusOffDemonTask` + `scorchingBowRanksUpOnASyntheticDemonRangedTask` | `BonusCondition`(+`VS_DEMON`), `BonusContext`(+`demon`), `ConditionalBonusRegistry`, `GearSelector` | WDB-7, WDB-9 |
| **WDB-11** acc/dmg split | `conditionalBonusAppliesSeparateAccAndDmgMultipliers` + symmetric rows regress-green (DHL/gear unchanged) + `nonWeaponRowsAreSymmetric` guard | `ConditionalBonus`, `ConditionalBonusRegistry`, `DpsEstimator.weaponDps`, `DefaultDpsEstimator` | WDB-7 |
| **WDB-12** Kalphite (Keris) | `kerisRanksUpOnAKalphiteTask` (acc 1.0 / dmg 1.382) + `kerisNoBonusOffKalphiteTask` | `BonusCondition`(+`VS_KALPHITE`), `BonusContext`(+`kalphite`), `ConditionalBonusRegistry`, `GearSelector` | WDB-9, WDB-11 |
| **WDB-13** Extract `MagicWeaponEvaluator` | `ConsumableSelectorTest` green (characterization) + `evaluatorReturnsPoweredStaffMaxHitAndSpeed` / `...bestAffordableTierForStandardCaster` | new `MagicWeaponEvaluator`(or `RuneTable`), `ConsumableSelector` | - |
| **WDB-14** Magic weapon DPS (gear-aware) | `higherDpsMagicWeaponSelectedOverFlatBetter` + `nonWeaponMagicSlotsUnchanged` | `GearSelector`, `DefaultDpsEstimator`(magic core reuse), `GearSelectorTest` | WDB-1, WDB-13 |
| **WDB-15** Tumeken's Shadow gear-multiplier | `tumekensShadowWinsViaGearMultiplier` + `shadowDamageBonusCappedAt100` | `GearSelector`, `DefaultDpsEstimator` | WDB-14 |
| **WDB-16** Display reflects magic weapon + Shadow | `estimateReflectsShadowGearMultiplier` | `DefaultDpsEstimator` | WDB-8, WDB-15 |
| WDB-17 (OPT) Wilderness per-location | `wildernessWeaponRanksUpWhenWildernessLocationSelected` | `slayer-data.json` (location schema), `model/TaskData`/location model, `LoadoutAdvisor`, `GearSelector`, `BonusContext` | WDB-11 (only if DEC-4 = build) |

**Count: 16 core sub-tasks (WDB-1..16); +1 (WDB-17) iff Wilderness is approved.** NFR-1 re-affirmed -
the category lookups are O(1) per candidate; magic ranking adds a small per-candidate evaluation over
the (few) owned magic weapons; the existing `selectionOverTwoThousandOwnedIdsStaysWithinTheWarmCacheBudget`
benchmark re-runs green.

### Single-owner file map

| File | Sub-tasks (sequential) |
|---|---|
| `GearSelector.java` | WDB-4,5,6,7,10,12,14,15 (+17) |
| `DefaultDpsEstimator.java` / `DpsEstimator.java` | WDB-1,3,8,11,14,15,16 |
| `ConditionalBonus.java` / `ConditionalBonusRegistry.java` / `BonusCondition.java` / `BonusContext.java` | WDB-7,10,11,12 (+17) |
| `ConsumableSelector.java` | WDB-13 |
| `RuneTable.java` / new `MagicWeaponEvaluator.java` | WDB-13 |
| `model/TaskData.java` + `resources/data/slayer-data.json` | WDB-7,9 (+17) |
| `WeaponEffect.java` / `WeaponEffectRegistry.java` | WDB-2 |

---

## Acceptance criteria (new FR ids, continue from v1's FR-13.x)

| ID | Acceptance criterion | Task | Verified by |
|---|---|---|---|
| FR-14.1 | On a demon (melee) task, **Arclight outranks a generic scimitar** via +70% acc&dmg. | WDB-10 | `arclightOutranksScimitarOnADemonMeleeTask` |
| FR-14.2 | Demonbane gives **no bonus off a demon task**. | WDB-10 | `demonbaneGivesNoBonusOffDemonTask` |
| FR-14.3 | Category flags **exact**: demon = {Abyssal demons, Black demons, Greater demons, Nechryael} (4); kalphite = {Kalphite} (1). | WDB-9 | `DuradelDatasetValidationTest` |
| FR-14.4 | **Scorching bow** is ranked up on a demon **ranged** task (synthetic - no real dataset demon task is ranged). | WDB-10 | `scorchingBowRanksUpOnASyntheticDemonRangedTask` |
| FR-14.5 | **Keris ranks up on a kalphite task** via the damage-only (asymmetric) multiplier. | WDB-12 | `kerisRanksUpOnAKalphiteTask` |
| FR-14.6 | `ConditionalBonus` applies **separate accuracy and damage** multipliers; symmetric sources unchanged (DHL/gear regress-green). | WDB-11 | `conditionalBonusAppliesSeparateAccAndDmgMultipliers`, regression suite |
| FR-14.7 | On a magic task a **higher-DPS magic weapon is selected over a flat-better one**. | WDB-14 | `higherDpsMagicWeaponSelectedOverFlatBetter` |
| FR-14.8 | **Tumeken's Shadow** is selected when its **x3 gear-multiplier** makes it highest DPS; damage bonus capped at +100%. | WDB-15 | `tumekensShadowWinsViaGearMultiplier`, `shadowDamageBonusCappedAt100` |
| FR-14.9 | Magic spell ordering is **reconciled** - `GearSelector` and `ConsumableSelector` share `MagicWeaponEvaluator` (no duplicated rune/tier logic). | WDB-13 | `ConsumableSelectorTest` (green) + evaluator tests |
| FR-14.10 | Displayed **Est. DPS reflects** the chosen magic weapon incl. the Shadow multiplier. | WDB-16 | `estimateReflectsShadowGearMultiplier` |
| FR-14.11 (DEFER) | Wilderness weapons rank up only when a Wilderness location is selected (per-location predicate). | WDB-17 | `wildernessWeaponRanksUpWhenWildernessLocationSelected` (only if DEC-4 = build) |
| NFR-1 (re-affirm) | Selection over 2000 warm ids stays within budget after category + magic ranking. | WDB-10/14 | existing warm-cache benchmark (re-run green) |

---

## Decisions to surface to the user (before WDB v2 build)

- **DEC-4 (Wilderness) - recommend DEFER.** Location not category; the loadout is computed at bank time
  before the player is in the Wilderness, so only a per-location flag threaded through the selected
  location is correct, and it is cross-cutting (advisor signature + dataset location schema + UI) for a
  one-task payoff on this dataset (only Ankou has a Wilderness location). Build it (WDB-17) only if the
  user wants Wilderness coverage now. Demon + kalphite proceed regardless.
- **DEC-5 (Keris 1/51 proc) - recommend EV term** `damage = 1.382` (`1.33 * 53/51`): deterministic,
  improves the displayed DPS, doesn't change ranking. Ignoring the proc (`1.33`) is also fine.
- **DEC-6 (magic ranking gear-aware) - recommend YES.** Magic weapon ranking selects non-weapon magic
  gear first, then ranks weapons with the gear context (so the Shadow's x3-gear can win). Principled
  because magic armour is weapon-independent; intentionally asymmetric vs melee/ranged weapon-only.
- **DEC-7 (Silverlight) - recommend EXCLUDE from v1.** No verified acc/dmg %; Arclight/Emberlight/
  Darklight cover demonbane. Add when a % is confirmed.
- **DEC-8 (do the acc/dmg split now) - recommend YES.** Needed for Keris; unlocks DHCB + dragon hunter
  wand later; symmetric sources unaffected (factory keeps existing rows identical).

## Sources

Verified weapon data: `weapon-reference-1h.md`, `weapon-reference-2h.md`, `weapon-reference.md` §3-§4
(speeds per individual weapon page). Code grounding: `ConsumableSelector.buildMagic`, `RuneTable`,
`DefaultDpsEstimator` (magic branch), `GearSelector`, `slayer-data.json` (42 tasks). OSRS Wiki pages
per `weapon-reference.md` §8.

# DPS-driven weapon selection + special-weapon effects - design

> Status: design (Gate-4 defect + user-approved scope expansion). Board task **WDP**. Builds on
> ADR-0001 (stat-driven selection) and ADR-0006 (estimator display-only), **amended by ADR-0008**.
> Reuses the LFB `ConditionalBonusRegistry` (ADR-0007) and the LFB-5 bonus-aware estimator. Scope:
> ranking only, owned-only, curated + extensible. Design only - no production code here. Build =
> task **WDB** (TDD, sub-tasks WDB-1..8).

## 1. Problem

`GearSelector.score()` ranks the WEAPON slot by flat offence - melee `attack(type)+meleeStr`, ranged
`arange+rangedStr`, magic `amagic+5*mdmg` (plan section 4.3). It **never reads
`Bonuses.attackSpeedTicks`**, which is carried but unused by the selector. So a 6-tick Armadyl
godsword (high strength) is recommended over a faster, higher-DPS weapon like Osmumten's fang (5
tick), and the whole 2h-vs-(1h+shield) comparison (section 4.6) inherits the same blind score. Live
testing surfaced this at Gate-4. The user approved the fullest fix: **rank weapons by real sustained
DPS** and **model passive special-weapon mechanics**.

## 2. Key insight: the DPS function already exists and is already attack-speed-aware

`DefaultDpsEstimator.estimate(...)` is a complete, correct, client-thread-pure sustained-DPS function.
It already:

- reads weapon speed from the WEAPON slot's `attackSpeedTicks` (`weaponSpeed`, line 43-46);
- computes hit chance against the **monster's defence** for the style (melee takes the **best** of
  stab/slash/crush, `bestHc = Math.max(...)`);
- computes average damage from the player's stats + summed strength;
- applies the LFB-5 conditional multiplier `m` to accuracy and max hit (higher-of, no stacking);
- returns `avgHit / (speedTicks * 0.6)` (tick = 0.6s).

**The defect is not in the DPS maths - it is that the selector does not call it.** The fix is to route
the WEAPON slot's ranking through this function instead of the flat score. Everything else (non-weapon
slots, ammo, the conditional additive term) stays as-is. This is a small, surgical change to one
ranking branch, not a new engine.

## 3. Design

### 3.1 What changes and what does not

| Concern | Before | After |
|---|---|---|
| WEAPON slot, MELEE / RANGED | flat `att+str` score | **`weaponDps(...)`** - hit-chance x avg-damage / attack-interval |
| WEAPON slot, MAGIC | flat `amagic+5*mdmg` | **unchanged** (flat) for v1 - see section 6 (the spell-ordering boundary) |
| 2h vs (1h+shield), section 4.6 | combined flat score | **combined DPS** (1h+shield offence summed); COST stays price-based |
| Non-weapon slots (head/body/legs/...) | flat score + LFB `(m-1)*L` term | **unchanged** (speed/defence do not apply to armour) |
| AMMO (ranged) | highest `rangedStr` | **unchanged** (plan 4.7) |
| COST mode, WEAPON | cheapest flat-positive | cheapest **positive-DPS** weapon |
| Est. DPS display | full-loadout estimate + LFB-5 `m` | **+ the worn weapon's passive effect** (Fang/Scythe) so the shown number matches the pick rationale |

### 3.2 `weaponDps` - extract a pure, selection-grade function from the estimator

Refactor `DefaultDpsEstimator` so the core DPS maths is a private method taking **aggregate** offensive
inputs (summed accuracy by type, strength, weapon speed, the multiplier, the weapon effect). Both
public entry points feed it:

- `estimate(style, equipped, stats, task, spellBaseMaxHit)` - **display path, behaviour unchanged.**
  Sums bonuses over the worn map -> core. (Plus: now also passes the worn weapon's `WeaponEffect`,
  section 3.4.)
- **NEW** `double weaponDps(Bonuses weapon, PlayerStats player, MonsterDefence def, CombatStyle style,
  WeaponEffect effect, double conditionalMultiplier)` - **selection path.** Uses the **weapon's own**
  offensive bonuses (the candidate in isolation) -> core. Added to the `DpsEstimator` interface so
  `GearSelector` depends on the seam and tests can fake it.

`weaponDps` handles **MELEE and RANGED only**; for MAGIC it is not called (section 6). Both methods are
pure (no `ItemManager`/EDT; stats arrive via the already-resolved `Bonuses`), satisfying NFR-3.

**Weapon-only ranking (deliberate simplification).** `weaponDps` scores a candidate weapon against the
monster on the player's **base** stats with **no other gear** summed in. This is a legitimate,
well-defined sustained-DPS metric ("what does this weapon deliver against this monster, on this
character") and keeps selection **order-independent** (the weapon choice does not depend on which body
armour you own). The only distortion is hit-chance saturation: with full gear both weapons sit closer
to the accuracy cap, so weapon-only scoring slightly amplifies accuracy differences. This is
second-order and conservative (it favours accuracy, rarely the wrong call). The **displayed** Est. DPS
still uses the full worn loadout, so the number the user sees is the real one. Documented limitation in
ADR-0008.

### 3.3 The DPS core (shared maths, unchanged formulas)

For a successful-hit probability `p` and per-hit average damage `d` over a swing of `speedTicks`:

```
hitChance = 1 - (1 - p)^effect.accuracyRolls          // Fang rerolls accuracy; default rolls = 1
avgHit    = hitChance * (maxHit / 2.0) * effect.damageMultiplier   // Scythe multiplies avg damage
dps       = avgHit / (speedTicks * 0.6)
```

`p` and `maxHit` come from the existing OSRS rolls in `estimate()` (effective level + 8, `(stat+64)`,
the `m` multiplier on both rolls). The two `effect` fields (`accuracyRolls`, `damageMultiplier`)
default to `(1, 1.0)` = today's maths exactly, so the display path is behaviour-preserving until
section 3.4 wires the worn weapon's effect.

### 3.4 Special-weapon-effects model - the in-scope list and the extensible table

Two distinct shapes, each housed where its shape fits:

**(a) Passive DPS-formula mechanics -> new `WeaponEffect` + `WeaponEffectRegistry`.** These change the
DPS *formula shape*, not a scalar, so they cannot live in `ConditionalBonusRegistry` (a per-style
scalar). New value type `WeaponEffect{int accuracyRolls; double damageMultiplier;}` and an all-static
`WeaponEffectRegistry.lookup(int itemId) -> WeaponEffect` (default `NONE = (1, 1.0)`), keyed by raw
item id (mirroring the LFB raw-id registry decision - imbue/variant collapse is irrelevant here).

| Weapon | Speed (data, from item stats) | `accuracyRolls` | `damageMultiplier` | Rationale (verified, OSRS Wiki) |
|---|---|---|---|---|
| Osmumten's fang | 5t | **2** | 1.0 | Rolls accuracy **twice** (stab) -> `1-(1-p)^2`. Damage range 15-85% of max keeps the **mean at 0.5*max** (DPS-neutral; only lowers variance) -> no damage multiplier. |
| Scythe of vitur | 5t | 1 | **1.75** | Hits a >=2-tile target 3 times at 100/50/25% of max, each rolled independently -> mean per swing = `p*(max/2)*(1+0.5+0.25)`. |
| (any other) | n/a | 1 | 1.0 | `NONE` - normal single-roll, single-hit. |

Speeds are **not** in the registry; they are read from `Bonuses.attackSpeedTicks` (already populated).
Adding a future passive weapon = **one registry data row + one test row**, no `GearSelector`/estimator
control-flow change.

**(b) Task-conditional damage multipliers -> extend the existing `ConditionalBonusRegistry`
(ADR-0007).** Dragonbane is a per-style scalar conditioned on a task predicate - identical shape to
salve-vs-undead. Add:

- `BonusCondition.VS_DRAGON` (`test = ctx.dragon`); `BonusContext` gains `boolean dragon`;
- `TaskData` gains `boolean dragon`, set per-task in `slayer-data.json` (same pattern as `undead`);
- registry row(s) for the dragonbane weapon(s).

| Weapon | id | condition | melee / ranged / magic | Notes |
|---|---|---|---|---|
| Dragon hunter lance | 22978 | VS_DRAGON | **1.20** / 1.0 / 1.0 | +20% accuracy **and** +20% damage vs draconic - **symmetric**, fits the single-multiplier `ConditionalBonus` exactly. |
| Dragon hunter crossbow | 21012 | VS_DRAGON | 1.0 / **~1.275** / 1.0 | +30% acc / +25% dmg - **asymmetric**; does NOT fit one scalar. **Deferred** (section 7, decision DEC-3). |

DHL's multiplier is applied multiplicatively inside `weaponDps` (section 3.5), exactly as the estimator
applies `m`. No code path beyond the data row + the `dragon` flag is needed for DHL.

### 3.5 Reuse, interactions, and no-double-count (brief item 3)

LFB-5 already made `DefaultDpsEstimator` conditional-bonus-aware (it derives the higher-of multiplier
over the worn map). Weapon ranking inherits this **for free and without double-applying**, under one
clean rule:

- **WEAPON slot (MELEE/RANGED):** scored by `weaponDps`, which applies (i) the weapon's `WeaponEffect`
  and (ii) the weapon's **own** conditional multiplier (DHL vs dragon), looked up from
  `ConditionalBonusRegistry` for the candidate id. It does **not** apply other slots' conditional
  bonuses (black-mask in HEAD, salve in AMULET) - those multiply *every* candidate weapon equally and
  so do not change weapon **ordering**. Omitting them from weapon-only DPS is therefore exact for
  ranking.
- **Non-weapon slots:** scored by flat base + the LFB additive `(m-1)*L` term (unchanged).
- **The weapon slot is EXCLUDED from the additive `(m-1)*L` term.** A conditional item that is a weapon
  (DHL) gets its bonus *multiplicatively* via `weaponDps`; it must not *also* receive the additive term.
  Concretely: pass B computes `eff = base + bonusTerm(...)` for every item today; the new code computes
  the weapon-slot candidates' scores via `weaponDps` instead, so they never receive `bonusTerm`. No item
  is credited twice. (Today no registry item is a weapon, so this is latent until DHL lands; the rule is
  stated now so the build cannot reintroduce the double-count.)
- **Display estimator (Est. DPS):** unchanged in its higher-of `m` logic; additionally applies the worn
  weapon's `WeaponEffect` (section 3.4a) so the shown DPS reflects Fang/Scythe and matches why the
  weapon was picked. The conditional `m` and the `WeaponEffect` are orthogonal (one scales rolls, the
  other reshapes the formula) - no interaction to reconcile.

**Melee attack-type derivation (section 4.2) under DPS (brief item 3).** Each weapon's `weaponDps`
already self-selects its best attack style vs the monster (the core takes the **max** hit chance over
stab/slash/crush). So the global `meleeAttackType()` argmin is **no longer load-bearing for weapon
ranking** - a stab weapon is judged on its stab roll, a slash weapon on its slash roll, automatically.
`meleeAttackType()` is **retained** because non-weapon melee gear is still flat-scored by
`attack(meleeType)+str` (the weakness-derived type is the right heuristic for armour/glove offence).
The weapon self-selecting while armour uses the weakness type is intentional and consistent with the
existing estimator's melee branch.

**COST mode (brief item 3).** Unchanged objective: cheapest **viable** weapon, where viable now =
`weaponDps > 0` (a usable weapon for the style) instead of flat `score > 0`. Tie-break id ascending,
non-positive price last - all as today. The 2h-vs-(1h+shield) COST comparison stays price-based.

### 3.6 Section 4.6 (2h vs 1h+shield) becomes DPS-based

The shield contributes a (usually small) offensive bonus on top of a 1h weapon. Under DPS:

- **Option A (2h):** `weaponDps(best2h, ...)`.
- **Option B (1h+shield):** DPS of the 1h weapon **with the shield's offensive bonuses summed in** -
  i.e. `weaponDps(best1h.bonuses + bestShield.bonuses, ..., effect=best1h's, m=best1h's)`, using the
  1h weapon's speed and effect. (Adding flat-score to DPS would be a unit error; summing the shield's
  *bonuses* into the DPS input is the correct model.)
- 2h wins iff strictly greater DPS; ties fall to 1h+shield (unchanged tie policy). COST mode unchanged
  (cheaper combined price). Missing-piece degradation unchanged.

## 4. Excluded scope (documented limitations, ADR-0008)

- **Special-ATTACK weapons (AGS / DWH / BGS / Voidwaker spec) are NOT modelled.** The plugin recommends
  a single continuous loadout, not spec-swaps. These weapons are ranked on their **sustained, no-spec**
  DPS only - which correctly ranks an AGS *below* a Fang as a main-hand. We do not credit spec damage.
  This is the desired outcome, recorded as an explicit limitation.
- **Magic weapon selection stays flat-scored in v1** (section 6) - the spell-ordering boundary.
- **Tumeken's Shadow deferred** - it is a magic weapon (out of v1 weapon-DPS scope) and its "x3 magic
  accuracy and damage" multiplies *other gear's* magic damage, which the weapon-only model does not
  capture. Deferred with magic DPS ranking.
- **Dragon hunter crossbow deferred** - asymmetric acc/dmg needs a `ConditionalBonus` model split
  (section 7, DEC-3).
- **Scythe vs small (1-tile) monsters** - we always credit the 3-hit (`x1.75`); against a single-tile
  target the scythe hits once, so its DPS is **over**-estimated there. Most scythe-worthy slayer
  targets are large; accepted and documented.
- **Weapon-only saturation** - section 3.2.

## 5. TDD build breakdown (task WDB)

Dependency-ordered; each starts with its failing assertion. `GearSelector.java` and
`DefaultDpsEstimator.java` are each touched by several sub-tasks -> **single-owner, sequential** within
each file (no concurrent edits). Tested with the existing fake `EquipmentStatsProvider`/`PriceService`
+ a fake/real `DpsEstimator` and synthetic stats so the speed/effect is the deciding factor.

| Sub-task | Red-first assertion | Owns (files) |
|---|---|---|
| **WDB-1** Extract DPS core + `weaponDps` seam | `DefaultDpsEstimatorTest.estimateUnchangedAfterCoreExtraction` (existing cases still green) + `weaponDpsScoresASingleWeaponAgainstMonsterDefence` | `DpsEstimator.java` (+method), `DefaultDpsEstimator.java`, `DefaultDpsEstimatorTest` |
| **WDB-2** `WeaponEffect` + registry | `WeaponEffectRegistryTest.fangRollsAccuracyTwice` / `scytheMultipliesDamageBy175` / `unknownReturnsNone` | new `WeaponEffect.java`, `WeaponEffectRegistry.java` (+test) |
| **WDB-3** `weaponDps` applies the effect | `weaponDpsAppliesFangAccuracyReroll` (`1-(1-p)^2 > p`) + `weaponDpsAppliesScytheDamageMultiplier` | `DefaultDpsEstimator.java`, test |
| **WDB-4** WEAPON slot ranks by `weaponDps` (MELEE/RANGED) | `fasterHigherDpsWeaponBeatsSlowerHigherMaxHitWeapon` (Fang-shaped 5t > AGS-shaped 6t on a stab-weak monster) + `nonWeaponSlotsUnchangedUnderDpsWeaponRanking` | `GearSelector.java`, `GearSelectorTest` |
| **WDB-5** Section 4.6 by DPS | `twoHandVsOneHandPlusShieldComparedByCombinedDps` | `GearSelector.java`, test |
| **WDB-6** COST = cheapest positive-DPS weapon | `costModePicksCheapestPositiveDpsWeapon` + `costModeSkipsZeroDpsWeapon` | `GearSelector.java`, test |
| **WDB-7** Dragonbane via registry extension (DHL) **[gated on DEC-1]** | `dragonHunterLanceOutranksFangOnADragonTask` + `dragonHunterLanceGetsNoBonusOffDragonTask` + `weaponConditionalBonusNotAlsoAddedAsAdditiveTerm` + dataset `dragonTasksAreExactly{...}` | `BonusCondition`, `BonusContext`, `ConditionalBonusRegistry`, `model/TaskData`, `slayer-data.json`, `GearSelector.java`, `DuradelDatasetValidationTest`, `TaskDataJsonTest` |
| **WDB-8** Display Est. DPS reflects the worn weapon's effect | `estimateReflectsFangRerollForWornWeapon` + `estimateReflectsScytheMultiHit` | `DefaultDpsEstimator.java`, test |

WDB-1..6 + 8 are unconditional. **WDB-7 is gated on the user's DEC-1 call** (which dragonbane weapons in
v1). NFR-1 re-affirmed: the WEAPON slot now does a few floating-point ops per weapon candidate instead of
an int score; the existing `selectionOverTwoThousandOwnedIdsStaysWithinTheWarmCacheBudget` benchmark
re-runs green (the weapon set is a small fraction of owned ids; no extra O(n) pass).

## 6. The magic boundary (why melee+ranged DPS, magic stays flat in v1)

Magic weapon DPS needs the chosen spell's base max hit. The advisor selects **gear before consumables**
(`ConsumableSelector` picks the spell/powered-staff *after* `GearSelector` returns the weapon), and the
spell choice itself depends on the weapon (powered staff vs spellbook). DPS-ranking magic weapons would
invert that ordering or duplicate the spell logic. The motivating defect is melee speed-blindness;
melee+ranged DPS fixes it fully. Magic's flat `amagic + 5*mdmg` already prefers higher magic accuracy
and damage (the dominant factors), and the **displayed** Est. DPS is still true DPS. Magic weapon DPS
ranking (and Tumeken's Shadow) is a documented v1 boundary and a clean follow-up. Surface as DEC-2.

## 7. Decisions to surface to the user (before WDB build)

- **DEC-1 (in-scope special-effects set).** Recommend v1 = **Osmumten's fang + Scythe of vitur** (passive
  melee, fit the model exactly) **+ Dragon hunter lance** (task-conditional, symmetric +20%, zero model
  change). This is the buildable, correct-by-construction set.
- **DEC-2 (magic stays flat in v1).** Recommend **yes** - melee+ranged go DPS, magic keeps the flat
  score (section 6). Defers Tumeken's Shadow.
- **DEC-3 (Dragon hunter crossbow).** Its +30% acc / +25% dmg is asymmetric and needs splitting
  `ConditionalBonus` into separate accuracy and damage multipliers (a small, clean model extension that
  also makes future asymmetric sources possible). Recommend **defer to a v2** unless the user wants it
  now; if now, it is a +1 model-split sub-task before WDB-7.

## 8. Traceability (new acceptance criteria)

Amends ADR-0001 (flat weapon score) and ADR-0006 (estimator display-only). FR ids continue from LFB's
FR-12.x.

| ID | Acceptance criterion | Task | Verified by |
|---|---|---|---|
| FR-13.1 | On a melee task, a faster weapon with **higher computed DPS** is selected over a slower, higher-max-hit weapon. | WDB-4 | `fasterHigherDpsWeaponBeatsSlowerHigherMaxHitWeapon` |
| FR-13.2 | **Osmumten's fang outranks an AGS** on a stab-weak monster via the accuracy reroll + faster speed. | WDB-3/4 | `weaponDpsAppliesFangAccuracyReroll`, `fasterHigherDpsWeaponBeatsSlowerHigherMaxHitWeapon` |
| FR-13.3 | **Scythe modelled as 3 scaled hits** (x1.75 average damage). | WDB-2/3 | `scytheMultipliesDamageBy175`, `weaponDpsAppliesScytheDamageMultiplier` |
| FR-13.4 | **Non-weapon slots unchanged** (flat base + LFB additive term; speed/defence not applied). | WDB-4 | `nonWeaponSlotsUnchangedUnderDpsWeaponRanking` |
| FR-13.5 | **COST mode** still picks the **cheapest positive-DPS** weapon. | WDB-6 | `costModePicksCheapestPositiveDpsWeapon` |
| FR-13.6 | **Special-attack weapons are not over-ranked** - ranked on sustained no-spec DPS only (limitation). | WDB-4 | `fasterHigherDpsWeaponBeatsSlowerHigherMaxHitWeapon` (AGS-shaped loses), ADR-0008 |
| FR-13.7 | **Ranged weapons are DPS-ranked** (a faster higher-DPS ranged weapon beats a slower one). | WDB-4 | `rangedWeaponRankedByDps` |
| FR-13.8 | **2h vs 1h+shield** decided by **combined DPS**, not flat score. | WDB-5 | `twoHandVsOneHandPlusShieldComparedByCombinedDps` |
| FR-13.9 | **Est. DPS display reflects** the worn weapon's passive effect (Fang/Scythe). | WDB-8 | `estimateReflectsFangRerollForWornWeapon` |
| FR-13.10 | **[DEC-1]** Dragon hunter lance outranks Fang on a **dragon** task, gets no bonus off-dragon, and is **not double-counted**. | WDB-7 | `dragonHunterLanceOutranksFangOnADragonTask`, `dragonHunterLanceGetsNoBonusOffDragonTask`, `weaponConditionalBonusNotAlsoAddedAsAdditiveTerm` |
| NFR-1 (re-affirm) | Selection over 2000 warm ids stays within budget after DPS weapon ranking. | WDB-4 | existing `selectionOverTwoThousandOwnedIdsStaysWithinTheWarmCacheBudget` (re-run green) |

## 9. Sources (verified OSRS Wiki)

- Osmumten's fang - accuracy rolled twice (stab); damage 15-85% of max (mean unchanged):
  https://oldschool.runescape.wiki/w/Osmumten%27s_fang
- Scythe of vitur - 5t; hits a >=2-tile target 3 times at 100/50/25%, rolled independently:
  https://oldschool.runescape.wiki/w/Scythe_of_vitur
- Dragon hunter lance - +20% accuracy and damage vs draconic:
  https://oldschool.runescape.wiki/w/Dragon_hunter_lance
- Dragon hunter crossbow - +30% ranged accuracy, +25% damage vs draconic:
  https://oldschool.runescape.wiki/w/Dragon_hunter_crossbow
- Tick = 0.6s; weapon speeds (whip 4t, fang 5t, AGS 6t, scythe 5t) read from item stats:
  https://oldschool.runescape.wiki/w/Game_tick/Action_lengths

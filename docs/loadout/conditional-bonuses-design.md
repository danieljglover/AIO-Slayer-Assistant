# Task-conditional gear bonuses - design

> Status: design (Gate-4 follow-up to the loadout engine). Board task **LBD**. Builds on ADR-0001
> (stat-driven selection), amended by **ADR-0007**. Scope: ranking only, owned-only, curated +
> extensible. This is design only - no production code here. Build = task **LFB** (TDD).

## 1. Problem and decision (summary)

The flat-stat `GearSelector` (plan §4, ADR-0001) ranked a **Serpentine helm above a Slayer helmet
(i)** on a melee Slayer task because it cannot see the helm's on-task +16.67% accuracy/strength
bonus. The user approved modelling **task-conditional offensive multipliers** for a curated set:
the **Slayer-helm / Black-mask** family (on Slayer task) and the **Salve-amulet** family (vs undead),
with an extensible registry for future sources. ADR-0007 records the decision and amends NG-6.

## 2. Canonical facts (verified, OSRS Wiki) - corrections to the brief

The build must use these, not the values in the kick-off brief. Each was checked against the OSRS
Wiki (Salve amulet (e)/(i)/(ei), Undead attribute, per-monster pages).

| Source | Predicate | Melee | Ranged | Magic | Notes |
|---|---|---|---|---|---|
| Black mask | on Slayer task | **+16.67%** (x7/6) | - | - | non-imbued = melee only |
| Black mask (i) | on Slayer task | +16.67% | **+15%** (x1.15) | +15% | imbued adds ranged/magic |
| Slayer helmet | on Slayer task | **+16.67%** | - | - | **contains a black mask -> melee bonus** |
| Slayer helmet (i) | on Slayer task | +16.67% | +15% | +15% | imbued adds ranged/magic |
| Salve amulet | vs undead | +16.67% | - | - | base, un-enchanted/un-imbued |
| Salve amulet (e) | vs undead | **+20%** (x1.20) | - | - | enchanted = melee only |
| Salve amulet (i) | vs undead | +16.67% | +15% | +15% | imbued = all styles |
| Salve amulet (ei) | vs undead | **+20%** | **+20%** | **+20%** | enchanted + imbued = all styles, 20% |

**Brief corrections (surface to the user):**
- Brief said "plain Slayer helmet = no combat bonus." **Wrong** - a plain Slayer helmet contains a
  black mask and gives the +16.67% melee on-task bonus. Modelled accordingly.
- Brief said Salve (e) and (ei) are x1.1667. **Wrong** - the *enchanted* forms are **+20% (x1.20)**.
  Only the *imbued-but-unenchanted* Salve (i) is +16.67% melee / +15% ranged/magic.
- Brief said "aberrant spectres are NOT undead." **Wrong** - Aberrant spectres **are** undead (Salve
  applies). Conversely **Skeletal wyverns** and **Vyrewatch/Vampyres** are **NOT** undead.

## 3. The bonus model and scoring contract

### 3.1 Why not a naive per-slot multiply

The obvious model - multiply the conditional item's own slot score by `m` - **does not fix the bug**:

- Serpentine helm has higher flat head stats than a Slayer helm; `slayerHelmScore * 1.1667` usually
  still loses to `serpentineScore`.
- A **Salve amulet has ~0 flat combat stats**, so `0 * 1.20 = 0` and it would be filtered by the
  existing `score > 0` rule and never selected.

The black-mask / salve effect multiplies your **whole** offensive output, so its value is
proportional to the **total loadout offence**, not the slot's own stats.

### 3.2 The model we use: loadout-level multiplier as an additive per-slot term

Let `L` = the baseline offensive score of the loadout for the weakness style:

```
L = sum over all slots of ( max base score among owned items in that slot )
```

computed once per `select()` from the **base** (flat) scores, for the style and (for melee) the
chosen attack type. `L` is a single constant for the whole loadout.

For an owned item with base score `base = score(b, style, meleeType)` (the existing §4.3 formula,
unchanged) and an applicable conditional bonus with per-style multiplier `m`:

```
effectiveScore = base + round( (m - 1.0) * L )      // bonus only added when the predicate holds
               = base                                // otherwise
```

Per-slot selection then ranks by `effectiveScore` (DPS mode). Because `L` is constant within a slot,
the term simply lifts conditional candidates by a fixed amount over non-conditional ones in that
slot - exactly the intent. With `(1/6) * L` typically tens of points (L spans weapon + body + legs +
... attack/strength), the Slayer helm (i) clears the Serpentine helm, and a 0-stat Salve clears a
Fury/Glory.

This is faithful to the registry's "per-style multiplier" semantics; we just lift "multiply total
offence by m" to "add `(m-1)*L` to this slot" so a per-slot selector captures a whole-loadout effect.

### 3.3 Exact integration points in `GearSelector` (plan §4.3)

1. Build a `BonusContext` once at the top of `select()`:
   `ctx = { slayerHelmApplies = task.isSlayerHelmApplies(), undead = task.isUndead() }`.
2. **Pass A (baseline offence):** scan `owned.ids()`, compute `base = score(b, style, meleeType)`,
   track `maxBaseBySlot[slot]`. `L = sum(maxBaseBySlot.values())`. (Reuse the existing scan; no new
   public method on `score()` - it stays pure.)
3. **Pass B (candidates):** for each owned id, `base = score(...)`; look up
   `bonus = registry.lookup(id)`; `m = (bonus != null && bonus.condition.test(ctx)) ?
   bonus.multiplier(style) : 1.0`; `eff = base + (int) Math.round((m - 1.0) * L)`. **Keep the item as
   a candidate iff `eff > 0`** (so a 0-flat Salve with a positive bonus term survives the filter).
   Group by slot with `eff` as the score.
4. Per-slot pick, weapon/shield interplay, AMMO: **unchanged** (operate on `eff`). No conditional
   source is currently a weapon/shield/ammo, so those branches are unaffected.
5. **COST mode:** unchanged objective - cheapest `price(id)` among `eff > 0` items. The bonus only
   changes which items *qualify* (Salve now qualifies) and DPS-mode ranking, not the cost tie-break.
   (Consistent with the existing "consumables ignore mode" stance.)

`L` can be 0 only when no owned item scores for the style (then `select()` already returns empty).
Determinism: integer rounding once per item; same inputs -> same output (NFR holds).

### 3.4 Per-slot independence (confirmed)

The Slayer-helm bonus is keyed to HEAD-slot items; the Salve bonus to AMULET-slot items; both use the
same `L`. On a task that is **both** on-Slayer and undead (Ankou, Aberrant spectres) both predicates
hold and each slot's pick gets its own bonus term independently. **Accepted simplification (ADR-0007):**
this credits both effects although in OSRS black-mask and Salve do not stack on one hit - fine for
per-slot *selection* (we still want the best helm and the best amulet); it would only matter for an
absolute DPS sum, which we are not changing.

### 3.5 Est. DPS display

**Recommendation: ranking only for v1; do not change the Est. DPS number yet.** Making the displayed
DPS reflect the bonus means teaching `DefaultDpsEstimator` the same multiplier (and, to be correct,
applying only the *higher* of salve/black-mask when both apply, per §3.4). That is a separate,
larger change touching display semantics. Flagged as a follow-up decision for the user (LFB-5,
deferred by default). Until then the recommended gear is correct but the shown DPS can understate
on-task output - note this in the card copy if cheap, else accept.

## 4. The conditional-bonus registry (extensible)

New files (all under `loadout/`, all new - no hot-file contention):

- `BonusCondition` (enum): `ON_SLAYER_TASK`, `VS_UNDEAD`, each with `boolean test(BonusContext)`.
  `ON_SLAYER_TASK.test = ctx.slayerHelmApplies`; `VS_UNDEAD.test = ctx.undead`. Extensible: add a
  constant for a new predicate.
- `BonusContext` (`@Value`): `boolean slayerHelmApplies; boolean undead;` (built from `TaskData`).
- `ConditionalBonus` (`@Value`): `BonusCondition condition; double melee; double ranged; double
  magic;` with `double multiplier(CombatStyle)` (defaults 1.0 for an unset style). Store the OSRS
  multiplier (e.g. 1.1667, 1.15, 1.20); 1.0 = no bonus for that style.
- `ConditionalBonusRegistry` (all-static, mirroring `RuneTable`): `static ConditionalBonus
  lookup(int itemId)` - resolves `id` to its **canonical** form via `ItemVariationMapping.map(id)`
  (collapses Black mask charge states and imbue variants; verified headless-runnable in Wave A), then
  returns the curated entry or `null`. A `Map<Integer, ConditionalBonus>` literal keyed by canonical
  id.

Curated entries (per §2). **Item ids: pin at build** from `net.runelite.api.ItemID` /
`ItemVariationMapping`; expected canonical ids as a starting point (the build verifies imbued vs
non-imbued are distinct canonicals, and lists explicit ids where the collapse is wrong):

| Item | Expected id(s) | condition | melee / ranged / magic |
|---|---|---|---|
| Black mask (all charges) | ~8921 (canon) | ON_SLAYER_TASK | 1.1667 / 1.0 / 1.0 |
| Black mask (i) | ~11774 (+imbue variants) | ON_SLAYER_TASK | 1.1667 / 1.15 / 1.15 |
| Slayer helmet | 11864 | ON_SLAYER_TASK | 1.1667 / 1.0 / 1.0 |
| Slayer helmet (i) | 11865 | ON_SLAYER_TASK | 1.1667 / 1.15 / 1.15 |
| Salve amulet | 4081 | VS_UNDEAD | 1.1667 / 1.0 / 1.0 |
| Salve amulet (e) | 10588 | VS_UNDEAD | 1.20 / 1.0 / 1.0 |
| Salve amulet (i) | 12017 | VS_UNDEAD | 1.1667 / 1.15 / 1.15 |
| Salve amulet (ei) | 12018 | VS_UNDEAD | 1.20 / 1.20 / 1.20 |

> Future source = one row here + one registry test row. No `GearSelector` change.

## 5. The undead data source

The selector needs to know whether the current task's monster is undead. **Decision: a per-task
`boolean undead` on `TaskData`, fed from `slayer-data.json`** (a per-task flag, not a per-monster
trait table - the dataset is task-keyed and the bonus is task-scoped; a trait table is unneeded
generality, YAGNI).

- `model/TaskData`: add `private boolean undead;` (Lombok `@Data` -> `isUndead()`; Gson default
  `false`).
- `resources/data/slayer-data.json`: add `"undead": true` to the undead tasks only; omit elsewhere
  (default false; keeps the diff to 2 lines and the curated formatting intact).

### 5.1 Which tasks are undead (verified against the dataset's 42 tasks)

**Undead (exactly two):** `Ankou`, `Aberrant spectres`.

Everything else is **not** undead. Specifically called out because they look undead but are not
(Salve does **not** apply): **Skeletal wyverns** (magically animated remains), **Vampyres /
Vyrewatch** (vampyre attribute, not undead), Nechryael/Greater Nechryael (demons), Spiritual
creatures (not undead). Note **Aberrant spectres** is a MAGIC-weakness task, so Salve (ei)/(i) boost
the *magic* amulet score there; Ankou is MELEE.

### 5.2 Validation-test expectation (`DuradelDatasetValidationTest`)

- `taskByName("Ankou").isUndead()` == true; `taskByName("Aberrant spectres").isUndead()` == true.
- `taskByName("Skeletal wyverns").isUndead()` == false; `taskByName("Vampyres").isUndead()` == false;
  `taskByName("Abyssal demons").isUndead()` == false (representative non-undead).
- Count: exactly **2** tasks in the dataset have `undead == true` (guards against accidental adds and
  documents the curated set).
- `TaskDataJsonTest`: a task JSON with `"undead": true` round-trips to `isUndead() == true`; a task
  with the key absent loads as `false` (Gson default; back-compat with the existing 40 entries).

## 6. TDD build breakdown (task LFB)

Dependency-ordered; each starts with the failing assertion. Single-owner files listed; **stay off
`loadout/DefaultConsumableEffectsProvider.java` + its wiring (task LFA owns it)**. LFB owns:
`GearSelector.java`, the new `loadout/{BonusCondition,BonusContext,ConditionalBonus,
ConditionalBonusRegistry}.java`, `model/TaskData.java`, `resources/data/slayer-data.json`, and the
tests below.

- **LFB-1 - undead data (TaskData + dataset + validation).** Files: `model/TaskData.java`,
  `resources/data/slayer-data.json`, `DuradelDatasetValidationTest`, `TaskDataJsonTest`.
  *Red first:* `DuradelDatasetValidationTest.undeadTasksAreExactlyAnkouAndAberrantSpectres()` -
  asserts the §5.2 expectations. Fails to compile/asserts false until the field + data land.
- **LFB-2 - predicate + value types.** Files (new): `BonusCondition`, `BonusContext`,
  `ConditionalBonus`.
  *Red first:* `ConditionalBonusTest` - `ON_SLAYER_TASK.test(new BonusContext(true,false))` true and
  `(…false,…)` false; `VS_UNDEAD.test(new BonusContext(false,true))` true; `ConditionalBonus`
  `multiplier(MELEE)` returns the configured value and `multiplier(MAGIC)` returns 1.0 when unset.
- **LFB-3 - curated registry.** File (new): `ConditionalBonusRegistry`.
  *Red first:* `ConditionalBonusRegistryTest` - `lookup(SLAYER_HELMET_I)` -> ON_SLAYER_TASK with
  (1.1667, 1.15, 1.15); `lookup(BLACK_MASK)` -> melee-only (ranged/magic == 1.0);
  `lookup(SALVE_AMULET_EI)` -> VS_UNDEAD with (1.20,1.20,1.20); `lookup(<unknown id>)` == null; a
  charged Black-mask variant id resolves to the same entry as the canonical (ItemVariationMapping
  collapse).
- **LFB-4 - scorer applies the bonus term (the acceptance criteria).** File: `GearSelector.java`,
  `GearSelectorTest`. Tested with the existing fake `EquipmentStatsProvider`/`PriceService` (synthetic
  stats - we control the numbers so the bonus is the deciding factor). Depends on LFB-1/2/3.
  *Red first* (each its own test):
  - `slayerHelmIOutranksSerpentineHelmOnAMeleeSlayerTask` - owned {serpentine helm (higher flat head
    score), slayer helm (i) (lower flat)} on a melee `slayerHelmApplies` task -> `worn.get(HEAD)` ==
    slayer helm (i). (Fails on flat stats; passes with the `(m-1)*L` term.)
  - `salveEiIsChosenOnAnUndeadTaskAndNotOnANonUndeadTask` - undead melee task: salve (ei) outranks a
    higher-flat Fury in AMULET; same owned set on a non-undead melee task: Fury wins (salve term not
    applied; salve filtered as 0-flat or ranked below).
  - `salveEiRaisesAmuletScoreOnAMagicUndeadTask` - Aberrant-spectres-shaped magic task: salve (ei)
    (magic 1.20) beats a flat magic amulet it would lose to without the bonus.
  - `helmAndSalveApplyIndependentlyPerSlotOnAnUndeadSlayerTask` - HEAD == slayer helm (i) AND AMULET
    == salve (ei) in one selection (per-slot independence, §3.4).
  - `noConditionalBonusWhenSlayerHelmDoesNotApply` - on a `slayerHelmApplies == false` task, a black
    mask gets no boost (predicate false) and a higher-flat helm wins.
- **LFB-5 - Est. DPS display (DEFERRED, decision flag).** Not built by default (see §3.5). If the user
  wants the shown DPS to reflect the bonus, it is a separate task on `DefaultDpsEstimator` applying the
  *higher* of the applicable multipliers; do not silently bundle it into LFB-4.

## 7. Traceability (new acceptance criteria)

Amends PRD NG-6 (no longer a blanket non-goal for these families). New criteria mapped to LFB tasks
and verification:

| ID | Acceptance criterion | Task | Verified by |
|---|---|---|---|
| FR-12.1 | On a melee Slayer task, when both are owned, **Slayer helmet (i) outranks a Serpentine helm** in the head slot. | LFB-4 | `slayerHelmIOutranksSerpentineHelmOnAMeleeSlayerTask` |
| FR-12.2 | **Salve amulet (ei) raises the amulet score on an undead task** (Ankou melee / Aberrant spectres magic) but **not on a non-undead task**. | LFB-1/4 | `salveEiIsChosenOnAnUndeadTaskAndNotOnANonUndeadTask`, `salveEiRaisesAmuletScoreOnAMagicUndeadTask` |
| FR-12.3 | On an undead Slayer task the head and amulet conditional bonuses apply **independently per slot**. | LFB-4 | `helmAndSalveApplyIndependentlyPerSlotOnAnUndeadSlayerTask` |
| FR-12.4 | The undead set is data-curated: **exactly {Ankou, Aberrant spectres}** in the current dataset. | LFB-1 | `DuradelDatasetValidationTest` undead assertions |
| FR-12.5 | Adding a future conditional source is a **registry data add** (one row + one test), no `GearSelector` change. | LFB-3 | `ConditionalBonusRegistryTest` |
| NFR-1 (re-affirm) | Selection over 2000 warm ids stays within budget after the extra pass. | LFB-4 | existing `selectionOverTwoThousandOwnedIdsStaysWithinTheWarmCacheBudget` (re-run green) |

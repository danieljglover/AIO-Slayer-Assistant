---
status: accepted
amends: 0001
---

# Task-conditional offensive bonuses (Slayer-helm / Salve families) are in scope for a curated, extensible set

## Context

ADR-0001 made gear selection stat-driven over owned items and accepted, as a known cost
(PRD NG-5/NG-6), that "conditionally-strong items (Void, Crystal, ammo/weapon coupling) are scored
on flat stats only". In live testing this produced a wrong recommendation: for a melee Slayer task
the scorer ranked a **Serpentine helm above a Slayer helmet (i)** in the head slot, because it cannot
see the helm's on-task +16.67% accuracy/strength bonus - the helm's flat head stats are lower than
the Serpentine helm's, and the conditional bonus is invisible to a flat-stat score.

The user approved extending the model to **task-conditional offensive multipliers, broadly**, for a
defined, curated, extensible set of sources.

## Decision

**Amend ADR-0001 / NG-6.** Conditional, context-dependent **offensive multipliers** ARE now in scope
for a curated set of equipment sources whose bonus depends on a task predicate the engine can
evaluate from `TaskData`:

1. **Slayer-helmet / Black-mask family** - on-Slayer-task multiplier (predicate: the task counts for
   the Slayer-helm bonus, read from the existing `TaskData.slayerHelmApplies`).
2. **Salve-amulet family** - vs-undead multiplier (predicate: the task monster is undead, read from a
   new `TaskData.undead` flag).

The multiplier set is held in a **data registry** (`item id -> {condition predicate, per-style
multiplier}`) so future sources (e.g. tome/arclight-style conditionals) are a data add, not a code
change. The multiplier is applied **at loadout level, translated to an additive per-slot term**
(see the design doc, not a naive per-slot self-multiply - which does not fix the bug). The change is
**ranking-only** for v1; the displayed Est. DPS is unchanged (flagged as a follow-up).

Still **owned-only** (we never recommend an item the player does not own) and still **no
set-effect / coupled-item logic** beyond the named conditional sources (Void, crystal-set,
weapon/ammo coupling remain out of scope).

## Why this, not the alternatives

- **Why not keep NG-6 as-is:** the flat-stat model is demonstrably wrong for the single most common
  Slayer head/amulet choice (Slayer helm (i), Salve). The fix is small, data-driven, and the bug is
  user-visible.
- **Why not a naive per-slot multiply (`score * m`):** it does not fix the reported bug. The
  Serpentine helm's flat head score is higher; multiplying the Slayer helm's own modest head score by
  1.1667 typically does not overcome it. Worse, a Salve amulet has ~0 flat combat stats, so
  `0 * 1.20 = 0` - it would never be selected. The bonus multiplies your *whole* offensive output, so
  its value is proportional to total loadout offence, not the slot's own stats. We therefore lift the
  multiplier to a loadout-level additive term `(m - 1) * L` (L = the style's baseline loadout offence);
  see the design doc.
- **Why a data registry, not hard-coded `if id == ...`:** the curated set will grow; keeping it as a
  static table (mirroring `RuneTable`) means new sources are one data row + one test row.

## Consequences

- New value type(s): a `ConditionalBonus {condition, per-style multiplier}` + a `BonusCondition`
  predicate (`ON_SLAYER_TASK`, `VS_UNDEAD`) + a curated `ConditionalBonusRegistry` keyed by canonical
  item id (collapse charge/imbue variants via `ItemVariationMapping`, mirroring the consumables path).
- `GearSelector.select` gains one baseline-offence pass (`L`) and an additive bonus term in scoring;
  conditional items survive the `score > 0` filter when they have an applicable bonus. NFR-1 (selection
  < 10 ms / 2000 warm ids) still holds - the extra pass is O(n).
- **Data model + dataset change:** `TaskData` gains `boolean undead`; `slayer-data.json` marks the
  undead tasks. In this dataset exactly **two** tasks are undead: **Ankou** and **Aberrant spectres**
  (verified against the OSRS Undead attribute; Skeletal wyverns, Vyrewatch/Vampyres are NOT undead).
- **Modeling simplifications, accepted for v1 (flagged):** (a) on an undead Slayer task the model
  credits *both* the Slayer-helm and the Salve bonus to their own slots, although in OSRS the
  black-mask and Salve effects do not stack on the same hit - acceptable because each slot's *pick* is
  still individually correct (best helm, best amulet) and selection is per-slot, not an absolute DPS
  sum. (b) Est. DPS display is not adjusted, so the shown number can understate on-task output.
- **Reversal cost** is real (data-model + dataset + scorer), so this is recorded as an ADR amending
  0001. NG-6 in the PRD is narrowed: the named conditional families are now modelled; everything else
  (Void, crystal, coupling) stays out.

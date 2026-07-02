---
status: accepted
---

# Stat-driven gear selection over owned items replaces static per-task loadout lists

The loadout engine no longer reads hand-curated BIS-to-budget item lists
(`TaskData.loadouts` / `StyleLoadout.slotOptions`). Instead, for the task's weakness, it scans
**every owned equipable item**, resolves each one's equipment stats via the existing
`EquipmentStatsProvider` seam (`ItemManager.getItemStats(id)`), groups them by our `EquipmentSlot`,
and picks the best item per slot by a weakness-relevant score. The **style is fixed by
`task.weakness.style`**; we no longer try all styles and keep the best DPS. The melee **attack type**
is the one the monster defends worst against (`min` of `monsterDefence.{stab,slash,crush}`); the magic
**element** is `task.weakness.element`.

Per-slot score (single number, deterministic, see `docs/loadout/plan.md` for the exact formula and
tie-breaks): melee = matching-attack-type bonus + strength; ranged = ranged attack + ranged strength;
magic = magic attack + weighted magic-damage%. Weapon/shield interplay compares the best 2h weapon
against the best 1h weapon + best shield by combined score (`ItemEquipmentStats.isTwoHanded()`).

Why this and not a curated whitelist/hint: keeping `slotOptions` as an optional whitelist would
reintroduce the exact curation burden and staleness this redesign exists to remove, and create two
competing sources of truth (the list vs the stats). The owned-item stats plus the per-task weakness
data are sufficient. The known cost is that conditionally-strong items (Void, Crystal, ammo/weapon
coupling) are scored on flat stats only - accepted for v1 (PRD NG-5/NG-6), flagged for follow-up.

Consequence: `StyleLoadout` and `TaskData.loadouts` are deleted; `slayer-data.json` loses every
`loadouts` block; `weakness` + `monsterDefence` become load-bearing (must be correct/complete). The
dataset validation test moves from asserting loadouts to asserting weakness + monster defence.
Reversal cost is real (data-model + dataset change), so this is recorded.

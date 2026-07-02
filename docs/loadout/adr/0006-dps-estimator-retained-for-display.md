---
status: accepted
---

# `DefaultDpsEstimator` is retained for display only; style is fixed by weakness and magic max-hit comes from the chosen spell

The DPS estimator is kept, but its role narrows: it no longer **chooses** the combat style (ADR-0001
fixes style to `task.weakness.style`), it only **scores the one chosen loadout** to show "Est. DPS" in
the panel (PRD FR-8). It is fed by the same `EquipmentStatsProvider` (`ItemManager`-backed) stats as
selection, so the displayed number is consistent with the picked gear.

One signature change: the magic base max hit was read from `task.getLoadouts().get(MAGIC).getSpellMaxHit()`,
which disappears with `StyleLoadout` (ADR-0001). The estimator now receives the base max hit of the
**spell the consumable selector chose** (from the curated element->tier table, ADR-0004) as an explicit
parameter; non-magic styles ignore it.

Why keep it rather than replace it: the existing OSRS hit-chance/max-hit formula is correct and tested;
the redesign changes *what feeds it* (owned-stat-derived worn map, fixed style, derived spell), not the
maths. Its melee branch already auto-selects the best-rolling attack type from the summed gear, which
will normally agree with the selector's weakness-derived attack type (ADR-0001); on the rare
disagreement the displayed DPS is still a valid figure for the equipped gear. Recorded because a reader
seeing the estimator no longer drive style selection would otherwise wonder why it still exists.

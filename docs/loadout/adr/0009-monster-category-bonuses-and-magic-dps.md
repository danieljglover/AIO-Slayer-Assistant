---
status: accepted
amends: 0007, 0008
---

# Monster-category conditional bonuses and magic-weapon DPS ranking

## Context

ADR-0008 shipped melee+ranged DPS weapon selection and the "clean three" (Fang, Scythe, Dragon hunter
lance), deferring two boundaries: (DEC-2) magic weapon DPS and Tumeken's Shadow, and the monster-CATEGORY
conditional weapons (demon / kalphite / Wilderness) that the dataset's `undead`+`dragon` predicates do
not cover. The user has pulled both deferrals into scope. This ADR records the design (full detail:
`docs/loadout/weapon-dps-design-v2.md`).

## Decision

**Extend - not rewrite - ADR-0007 (conditional bonuses) and ADR-0008 (weapon DPS).**

1. **Monster-category predicates.** Add `VS_DEMON` and `VS_KALPHITE` `BonusCondition`s fed from new
   `TaskData.demon` / `TaskData.kalphite` flags (modelled exactly like `undead`/`dragon`). Demonbane
   weapons (Arclight 1.70, Emberlight 1.70, Darklight 1.60, Scorching bow ranged 1.30 - all
   **symmetric** acc==dmg vs demons) are curated `ConditionalBonusRegistry` rows; no model change beyond
   the predicate. Demon tasks in the dataset are exactly **{Abyssal demons, Black demons, Greater demons,
   Nechryael}** (4; Hellhounds/Smoke devils/Dust devils are NOT demons); kalphite is exactly
   **{Kalphite}** (1).

2. **Accuracy/damage split of `ConditionalBonus`** (the deferred DEC-3). Generalise each per-style
   multiplier to `{accuracy, damage}`; a `symmetric(...)` factory keeps every existing row (salve,
   black-mask, DHL, all demonbane) byte-for-byte equivalent. `weaponDps` takes separate `accMult`/
   `dmgMult`. This is required for **Keris** (kalphite: +33% **damage only**, no accuracy bonus) and
   unlocks DHCB / dragon hunter wand later. Keris's 1/51 triple-damage proc is folded into an
   expected-value damage multiplier `1.33 * 53/51 ~= 1.382`. **Invariant:** asymmetric bonuses live only
   on weapon-slot items (the non-weapon additive-`(m-1)*L` term stays symmetric); test-guarded.

3. **Magic-weapon DPS ranking is gear-aware.** Extract the spell/staff/affordability resolution from
   `ConsumableSelector` into a shared `MagicWeaponEvaluator` that both the selector (final consumables)
   and `GearSelector` (ranking) call - one source of truth, no duplication. Magic ranking selects
   non-weapon magic gear first (weapon-independent flat `amagic+5*mdmg`), then ranks magic weapons by
   `hitChance * maxHit/2 / (speed*0.6)` using each candidate's evaluator base max hit + the gear matt/
   mdmg context. Powered staves use their item `attackSpeedTicks`; standard casters use 5t (cast speed).

4. **Tumeken's Shadow** is modelled as a multiplier on the GEAR contribution: `matt = weapon.amagic +
   3*gearMatt`, `mdmg = weapon.mdmg + min(100, 3*gearMdmg)` (magic-damage capped at +100%; the x3
   excludes the weapon's own bonus; x4-in-ToA not modelled - Slayer is outside ToA). The displayed Est.
   DPS mirrors this.

5. **Wilderness is deferred** (DEC-4): it is a location, not a monster category, and the loadout is
   computed at bank time before the player is in the Wilderness, so only a per-location flag threaded
   through the selected location is correct - cross-cutting for a one-task payoff on this dataset.

## Why this, not the alternatives

- **Generalise the registry, don't fork it.** Demonbane is symmetric and drops into the existing model;
  only Keris needs the acc/dmg split, and that split is the clean generalisation that also unlocks the
  other asymmetric sources. Forcing Keris into `WeaponEffect` (which has a damage-multiplier field) fails
  because `WeaponEffect` is unconditional - it would credit Keris on every task.
- **Gear-aware magic, weapon-only melee/ranged.** Melee/ranged stay weapon-only (order-independent).
  Magic must be gear-aware because Tumeken's Shadow's entire value is multiplying *other* gear; this is
  sound because magic armour selection is weapon-independent, so gear-first introduces no cycle.
- **Reuse `ConsumableSelector`'s spell logic via extraction**, rather than inverting the gear->spell
  order or duplicating `RuneTable` handling in `GearSelector` - the spell-ordering boundary that
  deferred magic in v1.
- **Defer Wilderness** because the only correct option is cross-cutting and the dataset payoff is one
  task (Ankou); a per-task flag or a live location read would be wrong (over-credit / wrong timing).

## Consequences

- ADR-0007 amended: `ConditionalBonus` gains separate accuracy/damage multipliers (symmetric factory
  preserves existing rows); new `VS_DEMON`/`VS_KALPHITE` predicates + `TaskData.demon`/`kalphite` flags +
  dataset rows. ADR-0008 amended: magic weapon selection is no longer flat (DEC-2 lifted) and is
  gear-aware; `weaponDps` takes split acc/dmg multipliers.
- New/changed: `MagicWeaponEvaluator` (extracted from `ConsumableSelector`); `GearSelector` magic-weapon
  ranking; `DefaultDpsEstimator` magic display reflects the chosen weapon + Shadow; category registry
  rows (ids pinned at build).
- **Limitations (explicit):** Silverlight excluded (no verified %); Emberlight modelled = Arclight
  demonbane (distinct passive unconfirmed; its defence-drain is a spec); Scorching bow is latent (no
  dataset demon task is ranged - built for correctness, unit-tested synthetically); Arclight's reduced
  49%-vs-Duke not modelled; Shadow x4-in-ToA not modelled. Wilderness deferred. Keris per-variant extras
  (ToA-only / specs) not modelled.
- Build = task **WDB** extended to 16 core sub-tasks (WDB-1..16; +WDB-17 iff Wilderness approved), TDD,
  single-owner per hot file. Decisions for the user before build: **DEC-4** Wilderness (defer),
  **DEC-5** Keris proc EV, **DEC-6** gear-aware magic, **DEC-7** exclude Silverlight, **DEC-8** do the
  acc/dmg split now. Full design + traceability: `docs/loadout/weapon-dps-design-v2.md`.

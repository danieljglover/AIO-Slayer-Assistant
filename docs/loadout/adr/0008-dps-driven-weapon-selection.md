---
status: accepted
amends: 0001, 0006
---

# DPS-driven weapon selection and a passive special-weapon-effects model

## Context

`GearSelector` ranks the WEAPON slot by flat offence (melee `attack(type)+meleeStr`, ranged
`arange+rangedStr`, magic `amagic+5*mdmg`) and never reads `Bonuses.attackSpeedTicks`, which it
carries but ignores. Live Gate-4 testing showed this is attack-speed-blind: a 6-tick Armadyl godsword
is recommended over a faster, higher-DPS Osmumten's fang (5-tick), and the 2h-vs-(1h+shield)
comparison (plan section 4.6) inherits the same blind score. The user approved the fullest fix: rank
weapons by real sustained DPS and model passive special-weapon mechanics.

`DefaultDpsEstimator.estimate(...)` is already a correct, attack-speed-aware, monster-defence-aware,
conditional-bonus-aware (LFB-5) sustained-DPS function - it is simply never consulted by the selector.

## Decision

**Rank the WEAPON slot by estimated sustained DPS for MELEE and RANGED, by reusing the estimator.**

1. Extract the estimator's DPS maths into a shared core and expose a pure, selection-grade
   `double weaponDps(Bonuses weapon, PlayerStats, MonsterDefence, CombatStyle, WeaponEffect, double
   conditionalMultiplier)` on the `DpsEstimator` seam. It scores a candidate weapon (in isolation, on
   the player's base stats) against the monster's defence for the style: `hitChance x avgDamage /
   (speedTicks * 0.6)`. `GearSelector` injects `DpsEstimator` and ranks weapon candidates by it.
2. **Non-weapon slots, AMMO, and the LFB conditional additive `(m-1)*L` term are unchanged** - speed
   and defence do not apply to armour. Magic weapon selection **stays flat-scored in v1** because magic
   weapon DPS depends on the spell chosen downstream of gear (the spell-ordering boundary).
3. The 2h-vs-(1h+shield) comparison (section 4.6) becomes **combined DPS** (the shield's offensive
   bonuses summed onto the 1h weapon); COST mode stays price-based. COST mode picks the cheapest
   **positive-DPS** weapon.
4. **Passive special-weapon mechanics** are modelled as data in a new `WeaponEffect` /
   `WeaponEffectRegistry` (formula-shape effects: Osmumten's fang accuracy reroll `1-(1-p)^2`; Scythe
   of vitur 3-hit `x1.75` average damage) and, for **task-conditional scalar** dragonbane damage, by
   extending the existing `ConditionalBonusRegistry` (ADR-0007) with a `VS_DRAGON` condition + a
   `TaskData.dragon` flag (Dragon hunter lance, symmetric +20%). A new weapon is added as **one data
   row + one test row**, no control-flow change.
5. **No double-count:** a weapon's own conditional multiplier (DHL) is applied *multiplicatively* inside
   `weaponDps`; the weapon slot is therefore **excluded** from the LFB additive `(m-1)*L` term. Other
   slots' conditional bonuses (black-mask/salve) are not applied to weapon-only DPS because they
   multiply every candidate weapon equally and do not change weapon ordering.
6. The **displayed Est. DPS** additionally applies the worn weapon's `WeaponEffect` so the shown number
   matches why the weapon was picked.

## Why this and not the alternatives

- **Reuse, not rebuild.** The DPS function exists and is tested; the defect is that selection does not
  call it. Routing the weapon slot through it is the smallest change that fully fixes the bug.
- **Weapon-only DPS** keeps selection order-independent and pure; the only cost is mild hit-chance
  saturation (favours accuracy, conservative). The displayed DPS still uses the full loadout.
- **Two data homes** because the effects have two shapes: formula-reshaping passives (`WeaponEffect`)
  vs per-style task-conditional scalars (`ConditionalBonusRegistry`). Forcing both into one type would
  distort one of them.

## Consequences

- ADR-0001 amended: weapon-slot ranking is DPS-based, not flat (non-weapon slots unchanged). ADR-0006
  amended: the estimator is no longer display-only - it now drives weapon selection via `weaponDps`
  (and its display path additionally reflects the worn weapon's passive effect).
- `DpsEstimator` gains `weaponDps`; `GearSelector` gains a `DpsEstimator` dependency; new
  `WeaponEffect`/`WeaponEffectRegistry`; `ConditionalBonusRegistry`/`BonusContext`/`BonusCondition`/
  `TaskData`/`slayer-data.json` extended for dragonbane (gated on DEC-1).
- **Limitations (explicit):** special-ATTACK weapons (AGS/DWH/BGS spec) are **excluded** - the plugin
  recommends one continuous loadout, so they are ranked on sustained no-spec DPS only (an AGS correctly
  ranks below a Fang as a main-hand; spec damage is not credited). Magic weapon DPS and Tumeken's Shadow
  are deferred (v1 boundary). Dragon hunter crossbow is deferred (asymmetric +30% acc / +25% dmg needs a
  `ConditionalBonus` accuracy/damage split). Scythe's `x1.75` assumes a >=2-tile monster (over-estimates
  vs single-tile targets, which we do not model). Weapon-only saturation as above.
- Build = task **WDB** (sub-tasks WDB-1..8, TDD). NFR-1 re-affirmed via the existing warm-cache
  benchmark. Full design + traceability: `docs/loadout/weapon-dps-design.md`.

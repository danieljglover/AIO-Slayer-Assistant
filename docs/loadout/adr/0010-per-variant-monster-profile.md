---
status: accepted
amends: 0001, 0007
---

# Per-variant monster profile and the MonsterProfile resolution seam

## Context

A single Duradel assignment is satisfiable by many monsters with different fights: leveled / location
variants, superiors, and bosses (Skotizo, K'ril, Tormented Demon, Demonic gorilla), each with its own
defence, weakness, and category. Today `TaskData` carries ONE profile (`weakness` + `monsterDefence` +
`demon`/`dragon`/`kalphite`/`undead`) and every engine entry point reads it directly:
`GearSelector.select`, `DefaultDpsEstimator.estimate`, and `BonusContext.from(TaskData)`. So the loadout
is always computed against the task default, never the monster the player intends to kill (PRD
`docs/variants/prd.md`, FR-4/FR-5). Re-keying the engine onto a per-variant profile is a cross-cutting
change (R-3); it must keep the 42-task baseline byte-identical (FR-6, R-5).

## Decision

**Keep the task-level profile as the DEFAULT variant; add an optional variant list; re-key the engine to
operate on a resolved `MonsterProfile`, not on `TaskData`.**

1. **Model.** `TaskData.weakness`/`monsterDefence`/`demon`/`dragon`/`kalphite`/`undead` are UNCHANGED and
   become *the default variant's profile* (zero churn to the 42 existing profiles - the strongest
   regression anchor). Add `TaskData.variants: List<MonsterVariant>` (optional; Gson defaults to
   null/empty -> a task with no variants behaves exactly as today, FR-6). `MonsterVariant` carries
   `{ name, npcIds, combatLevel, weakness, monsterDefence, demon, dragon, kalphite, undead, isBoss,
   isDefault, location, requirement }`. `slayerHelmApplies` stays task-level (always true; a variant does
   not change whether it is a Slayer task).

2. **Seam: a `MonsterProfile` value type** `{ weakness, defence, demon, dragon, kalphite, undead,
   slayerHelmApplies }` with `MonsterProfile.fromTask(TaskData)` (the default profile) and
   `MonsterProfile.fromVariant(TaskData, MonsterVariant)` (variant fields, falling back per-field to the
   task default when a variant field is null - this is the UNKNOWN-weakness fallback, see ADR-0012). The
   engine (`GearSelector`, `DefaultDpsEstimator`, `BonusContext.from`) is re-keyed to read the
   `MonsterProfile`, NOT `TaskData`. The combat maths is untouched; only the source of the
   weakness/defence/flags moves. This mirrors the existing `BonusContext.from(TaskData)` extraction
   pattern.

3. **Resolution lives in `LoadoutAdvisor`** (the single orchestrator), which gains a
   `selectedVariantName` parameter (threaded exactly as `selectedLocationName` already is). It resolves
   the selected variant -> `MonsterProfile`, then drives the unchanged `GearSelector` /
   `ConsumableSelector` / `DpsEstimator`. `ConsumableSelector` needs no signature change: it already
   takes `style`/`element` separately, which the advisor reads off the resolved profile's weakness.

4. **Default-selection rule (OQ-2):** the variant marked `isDefault` (validated exactly-one per
   multi-variant task), else the first listed, else the task default profile. The default variant's
   profile equals the task profile, so a fresh task reproduces today's loadout (FR-6).

## Considered options

- **Overlay a synthetic "effective TaskData"** (copy the task, overwrite profile fields). Rejected: a
  copy of a large mutable bean is a latent bug (a forgotten field) and dishonest - the engine pretends to
  read a task while reading a half-task. `MonsterProfile` is the honest unit the engine actually consumes.
- **Mutate the shared TaskData per selection.** Rejected: TaskData is loaded once and shared; mutation is
  a data race and breaks value-equality re-render (NFR-4).

## Consequences

- Engine signatures change from `TaskData` to `MonsterProfile`. `MonsterProfile.fromTask(task)` is the
  one-line adapter that migrates every existing test and keeps the baseline green (FR-6 is the migration's
  acceptance gate).
- Category bonuses (`VS_DEMON`/`VS_DRAGON`/`VS_KALPHITE`/undead) now follow the selected variant
  automatically, because `BonusContext` is built from the profile (FR-5).
</content>
</invoke>

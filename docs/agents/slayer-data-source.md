# Slayer Data Source Reference

This document expands the concise root `AGENTS.md` guidance for agents editing
or interpreting `src/main/data/slayer`.

## Source Folder

`src/main/data/slayer` is the editable Slayer knowledge base. Gradle compiles it
with `ModularSlayerDataCompiler` into
`build/generated/resources/slayer/data/slayer-data.json`. Runtime consumers still
load `/data/slayer-data.json` through `SlayerDataService`; do not edit generated
runtime JSON directly.

Current layout:

```text
src/main/data/slayer/
  masters/      Slayer master catalog; one JSON file per master.
  tasks/        Task assignment, requirements, variants, locations, and advice.
  monsters/     Monster family directories; one JSON file per combat variant.
  locations/    Location capability records used by task routing and Konar.
  weapons/      Stable weapon IDs mapped to RuneLite item IDs and effects.
  strategies/   Strategy records; prefer <strategy-id>/strategy.json.
```

As of 2026-07-01, the folder contains 621 source files: 9 masters, 43 tasks,
230 monster variant files, 166 locations, 79 weapons, 85 JSON strategy files,
and 9 legacy strategy Markdown files. New or migrated strategy work should use
`src/main/data/slayer/strategies/<strategy-id>/strategy.json`.

## IDs And Joins

All source IDs are stable, lowercase, hyphenated identifiers. File names should
match the primary ID where practical, but the JSON field is authoritative.
Interpret cross-file relationships by ID, not by display name:

- `tasks/*.json` uses `masterIds` to reference `masters/*.json`.
- `tasks/*.json` uses `variantIds` and `defaultVariantId` to reference monster
  variants under `monsters/<monster-family-id>/*.json`.
- `tasks/*.json` uses `locationIds` to reference `locations/*.json`.
- monster variant `strategyId` references `strategies/<strategy-id>/strategy.json`
  or a legacy `strategies/<strategy-id>.md`.
- strategy `variantIds` should point back to every monster variant the strategy
  describes.
- strategy `plugin.primaryWeapons` and `plugin.secondaryWeapons[].weaponId`
  reference `weapons/*.json`.

The compiler validates duplicate IDs, broken master/location/variant/strategy
references, missing defaults for multi-variant tasks, and unknown strategy
weapon IDs. When changing one side of a relationship, update the other side when
needed.

## Task Files

`src/main/data/slayer/tasks/<task-id>.json` is the task-level source of truth.
It answers who assigns the task, what counts for it, where it can be done, which
requirements gate it, which location trade-offs matter, and what broad
recommendation should be surfaced.

Important fields:

- identity and wiki evidence: `taskId`, `name`, `wikiPageId`, `slayerTargetId`
- requirements: `combatLevel`, `slayerLevel`, `questReqs`, `requiredItemId`,
  `requiredItemName`
- assignment data: `masterIds`, `amountByMaster`, `extendedAmount`, `unlocks`
- linked entities: `monsterIds`, `variantIds`, `defaultVariantId`,
  `locationIds`
- recommendation flags: `weakness`, `monsterDefence`, `slayerHelmApplies`,
  `undead`, `dragon`, `demon`, `kalphite`
- wiki-derived context: `variantInfo`, `locationComparison`, `taskNotes`,
  `recommendedMethod`

`variantInfo` and `locationComparison` preserve OSRS Wiki task-page tables in a
direct JSON form. Do not discard notes just because a boolean flag already
exists; notes carry routing, safespot, task-only, cannon, skip/block, and
account-progression context.

## Monster Variant Files

`src/main/data/slayer/monsters/<monster-family-id>/<variant-file>.json` stores
combat identity for one selectable or task-counting monster variant. The family
directory is a grouping convenience; globally unique `variantId` values are what
other files reference.

Important fields:

- `variantId`, `name`, `npcIds`, `combatLevel`
- `weakness`, `monsterDefence`
- category flags: `demon`, `dragon`, `kalphite`, `undead`, `boss`
- location and gates: `location`, `locationId`, `requirement`
- boss and strategy links: `bossId`, `strategyId`

Variant files answer which exact NPC IDs and combat profile the plugin should
recognize, what category modifiers apply, and which strategy record should be
attached. If a task lists multiple variants, exactly one listed variant should
be the task `defaultVariantId`.

## Strategy Files

`src/main/data/slayer/strategies/<strategy-id>/strategy.json` is the preferred
strategy format. Interpret it as two layers in one document:

- `plugin`: compact runtime recommendation fields consumed by the plugin.
- `requirements`, `mechanics`, `methods`, and `styleOptions`: richer structured
  strategy knowledge for maintainers and LLM-assisted reasoning.

Important fields:

- identity and provenance: `strategyId`, `variantIds`, `sourceUrl`
- plugin recommendation: `plugin.primaryStyle`, `plugin.primaryWeapons`,
  `plugin.secondaryWeapons`, `plugin.note`
- strategy context: `requirements`, `mechanics`
- method breakdowns: `methods[].methodId`, `label`, `combatStyle`, `role`,
  `summary`, `recommendedFor`, `requiredOrKeyItems`, `prayers`, `steps`,
  `fallbacks`, `risks`, `equipment`, `inventory`, `notes`
- style alternatives: `styleOptions[].styleId`, `label`, `combatStyle`, `role`,
  `summary`, `equipment`

Every meaningful wiki strategy should become a method or style option. Use
`methods` for procedures such as solo, duo, tank, attacker, safespot, cannon,
barrage, skip/block, travel, and location-specific tactics. Use `styleOptions`
for gear or combat-style alternatives such as melee, ranged, magic, demonbane,
crush, earth spells, or budget setups. Use `plugin` only for the concise
recommendation the RuneLite plugin should show at runtime.

The compiler still supports legacy strategy fields at the root
(`primaryStyle`, `primaryWeapons`, `secondaryWeapons`, `note`) and legacy
Markdown strategy files. New JSON should use the nested `plugin` object so the
runtime recommendation is clearly separated from the LLM-readable breakdown.

## Location Files

`src/main/data/slayer/locations/<location-id>.json` stores reusable location
capabilities:

- `locationId`, `name`
- `multi`, `cannon`, `burst`, `konarLockable`, `safeSpot`, `wilderness`
- `accessNote`

Location files answer what an area generally supports. Task files may override
or refine this with task-specific `locationComparison` rows. Prefer task-specific
rows for amount, exact monster composition, safespot applicability, and route
notes; use location files for shared flags and access constraints.

## Weapon And Master Files

`src/main/data/slayer/weapons/<weapon-id>.json` maps strategy-friendly weapon
IDs to item IDs and metadata. Important fields are `weaponId`, `name`, `itemIds`,
`slot`, `styles`, `attackSpeedTicks`, `effects`, and `aliases`. The compiler
currently emits the first `itemIds` value into runtime strategy weapons, so keep
aliases together only when the recommendation should treat them as equivalent.

`src/main/data/slayer/masters/<master-id>.json` is a small catalog record with
`masterId`, `name`, optional `aliases`, and optional `unlockNote`. Assignment
ranges live in each task's `amountByMaster` and `extendedAmount`, not in master
files.

## Editing Rules

- Keep JSON valid and deterministic; use `jq empty` on changed JSON files.
- Preserve OSRS Wiki facts as concise structured values and notes; do not paste
  raw MediaWiki markup into JSON.
- Prefer explicit arrays of notes, steps, risks, and requirements over long
  unstructured paragraphs when migrating strategy content.
- Add missing weapons before referencing them from a strategy.
- Add or update source coverage tests for migrated tasks, variants, locations,
  and strategies so wiki-derived facts stay pinned.

# Modular Slayer Data Contributor Guide

Date: 2026-06-30

The editable slayer knowledge base lives in `src/main/data/slayer`. Gradle compiles those modular files
into the runtime resource `build/generated/resources/slayer/data/slayer-data.json`. Do not edit generated
runtime JSON directly.

## Directory Map

```text
src/main/data/slayer/
  masters/      Slayer master catalog
  tasks/        Assignment, task profile, variant list, and location links
  monsters/     Monster family directories, with one JSON file per variant
  locations/    Location flags used by recommendations and Konar handling
  weapons/      Stable weapon IDs mapped to RuneLite item IDs
  strategies/   Strategy directories with plugin fields plus method/equipment context
```

IDs are stable, lowercase, hyphenated strings. Cross-file fields must reference IDs, not names:

- `masterIds` references `masters/*.json`.
- `variantIds` and `defaultVariantId` reference variants inside `monsters/<monster-family-id>/*.json`.
- `locationIds` references `locations/*.json`.
- `strategyId` references `strategies/<strategy-id>/strategy.json` for migrated strategies.
- `primaryWeapons` and `secondaryWeapons[].weaponId` reference `weapons/*.json`.

## Validate Changes

Run this before committing data changes:

```bash
./gradlew cleanTest test
```

For a faster data-only check:

```bash
./gradlew generateSlayerData test --tests 'com.danieljglover.allinslayer.data.source.*'
```

Validation fails on duplicate IDs, broken references, missing defaults for multi-variant tasks, unknown
strategy weapons, and non-deterministic generation.

## Add A Slayer Master

1. Add `src/main/data/slayer/masters/<master-id>.json`.
2. Use a stable `masterId`.
3. Add that ID to each task's `masterIds`.
4. Add an `amountByMaster` entry when the task has a known assignment range for that master.

Example:

```json
{
  "masterId": "duradel",
  "name": "Duradel"
}
```

To remove a master, delete the master file and remove its ID from every task. Validation will fail until
all references are gone.

## Add Or Change A Task Assignment

Edit one file in `src/main/data/slayer/tasks`.

Important fields:

- `taskId`: stable task ID.
- `masterIds`: masters that assign the task.
- `amountByMaster`: optional assignment ranges keyed by master ID.
- `variantIds`: monster variants that count for the task.
- `defaultVariantId`: required when more than one variant is listed.
- `locationIds`: known task locations.
- profile fields: `weakness`, `monsterDefence`, `slayerHelmApplies`, and category flags.

For a new task, create the task file after the referenced master, variants, and locations exist.

To remove a task, delete its task file. Do not delete shared monster variants, locations, weapons, or
strategies unless nothing else references them.

## Add A Monster Or Variant

Edit or create a variant file under `src/main/data/slayer/monsters/<monster-family-id>`.

Use one monster family directory per logical task family, with one JSON file per variant. Each variant needs
a stable `variantId`, display `name`, `npcIds`, combat profile, category flags, and optional `strategyId`.
Name files with a readable slug and combat level where useful, for example:

```text
src/main/data/slayer/monsters/greater-demons/
  greater-demon-lvl92.json
  greater-demon-lvl100.json
  tormented-demon-lvl450.json
```

Example variant:

```json
{
  "variantId": "tormented-demon",
  "name": "Tormented Demon",
  "npcIds": [13599, 13600, 13601, 13602],
  "combatLevel": 450,
  "demon": true,
  "boss": false,
  "location": "Ancient Guthixian Temple",
  "requirement": "While Guthix Sleeps",
  "strategyId": "tormented-demon"
}
```

After adding a variant, add its ID to the relevant task's `variantIds`. If the task has multiple
variants, ensure exactly one listed variant is the `defaultVariantId`.

## Add A Location

Add `src/main/data/slayer/locations/<location-id>.json`.

```json
{
  "locationId": "catacombs-of-kourend",
  "name": "Catacombs of Kourend",
  "multi": true,
  "cannon": false,
  "burst": true,
  "konarLockable": true,
  "wilderness": false
}
```

Then add the `locationId` to each relevant task's `locationIds`.

To remove a location, remove it from all task `locationIds` first.

## Add A Weapon Or Weapon Effect

Add `src/main/data/slayer/weapons/<weapon-id>.json`.

```json
{
  "weaponId": "arclight",
  "name": "Arclight",
  "itemIds": [19675]
}
```

The current compiler emits the first `itemIds` entry to the runtime strategy recommendation. Keep
charged, degraded, or alias forms together only when the recommendation should treat them as the same
source weapon.

When adding a weapon used by a strategy, reference `weaponId` in `primaryWeapons` or
`secondaryWeapons[].weaponId`.

## Add A Strategy Guide

Add `src/main/data/slayer/strategies/<strategy-id>/strategy.json`.

```json
{
  "strategyId": "tormented-demon",
  "variantIds": ["tormented-demon"],
  "sourceUrl": "https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies",
  "plugin": {
    "primaryStyle": "MELEE",
    "primaryWeapons": ["emberlight", "arclight"],
    "secondaryWeapons": [
      {"weaponId": "scorching-bow", "style": "RANGED"}
    ],
    "note": "Demonbane is core; switch styles to break the shield."
  },
  "requirements": [],
  "mechanics": [],
  "methods": [
    {
      "methodId": "primary",
      "label": "Primary method",
      "combatStyle": "MELEE",
      "summary": "Readable strategy context for maintainers and LLM-assisted edits.",
      "steps": []
    }
  ],
  "styleOptions": []
}
```

Then add `strategyId` to each monster variant that should use it. The compiler validates every listed
variant and every weapon reference in `plugin`.

To remove a strategy, remove `strategyId` from every variant first, then delete the strategy directory.

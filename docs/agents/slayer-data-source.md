# Slayer Data Source Reference

This document expands the concise root `AGENTS.md` guidance for agents editing
or interpreting `src/main/data/slayer`.

## Source Folder

`src/main/data/slayer` is the editable Slayer knowledge base. The rebuilt runtime
loads `/data/advisor-catalogue.json` through `AdvisorDataService`.
`AdvisorCatalogueCompiler` combines the modular graph with the reviewed wiki
equipment and context mappings under `advisor/`. The existing modular compiler
also runs to validate the original source contracts. Do not edit generated JSON.
See [Advisor catalogue authoring](advisor-catalogue.md) for the additional inputs
and eligibility constraints.

Current layout:

```text
src/main/data/slayer/
  masters/      Slayer master catalog; one JSON file per master.
  tasks/        Task assignment, requirements, variants, locations, and advice.
  monsters/     Monster family directories; one JSON file per combat variant.
  locations/    Location capability records used by task routing and Konar.
  weapons/      Stable weapon IDs mapped to RuneLite item IDs and effects.
  strategies/   Strategy records; prefer <strategy-id>/strategy.json.
  advisor/      Wiki evidence, equipment eligibility, and reviewed runtime mappings.
```

Current counts and unresolved contexts are recorded in
[the reconstruction coverage report](../reconstruction/data-coverage.md).
New or migrated strategy work should use
`src/main/data/slayer/strategies/<strategy-id>/strategy.json`.

## Compiler Source Set

Authoring Java lives under
`src/dataGenerator/java/com/danieljglover/allinslayer/data/source/`, retaining the
`com.danieljglover.allinslayer.data.source` package. Keep compiler code and source
DTOs there; runtime models remain under `src/main/java/.../model`.

The `dataGenerator` source set compiles against `main.output.classesDirs` and
the main compile dependency classpath, preserving the client's Gson dependency
version. It has its own Lombok annotation processor. Neither its output nor its
dependencies are added to the plugin runtime classpath or either plugin JAR.

Generation follows this dependency order:

```text
compileJava (runtime classes and shared models)
  -> compileDataGeneratorJava -> dataGeneratorClasses
  -> generateAdvisorCatalogue -> generateSlayerData
  -> processResources -> classes -> jar
```

Both generation tasks use `dataGenerator.runtimeClasspath` and still read only
`src/main/data/slayer`. They write `advisor-catalogue.json`,
`advisor-coverage.json`, `slayer-data.json`, and `slayer-meta.json` into
`build/generated/resources/slayer/data/`; `processResources` includes them in
the runtime artifact. `generateAdvisorCatalogue` also works on its own.

Do not put `main.output` or `main.runtimeClasspath` on the generator classpaths:
those include generated resources and would introduce a dependency cycle.
Do not make `compileJava` depend on resource generation. The Hub records runtime
API calls during that compilation, before authoring code is compiled separately.

After changing this wiring, run `./gradlew clean generateSlayerData build`.
Compare generated resources with the pre-change build and inspect the packaged
JAR for required data and absence of `data/source` classes. Do not recreate the
removed editable `src/main/resources/data/slayer-data.json`.

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

`src/main/data/slayer/masters/<master-id>.json` is the master-intrinsic record.
Important fields:

- identity and evidence: `masterId`, `name`, optional `aliases` (replacement
  NPCs: Aya, Kuradal, Steve, Achtryn), `wikiPageId`
- access: `location`, `requirements` (`combatLevel`, `slayerLevel`, `quests`)
- reward economy: `economy.basePoints`, `economy.streakMultipliers` (milestone
  task count -> multiple of base points), `economy.blockCost`,
  `economy.zeroPoints` (Turael/Spria award nothing), `economy.streakResets`
  (Turael/Aya only: replacing another master's task resets the streak),
  `economy.diaryBoostedPoints` and `economy.diaryBoostNote` (Konar 18 -> 20,
  Nieve 12 -> 15 with the relevant elite diary), `economy.separateStreak`
  (Krystilia's independent Wilderness task counter)
- `notes`: master-intrinsic wiki facts the structured fields cannot express
  (task-changing rules, brimstone/Larran's key drops, Konar's location
  assignment, Krystilia's Wilderness constraint, Slayer cape bypass)

Assignment ranges live in each task's `amountByMaster` and `extendedAmount`,
not in master files. Master facts were verified against raw OSRS Wiki source on
2026-07-03 (Slayer Master pageid 11822, Slayer reward point pageid 26619, plus
each master's own page pinned by `wikiPageId`); re-verify against the wiki API
before changing economy, requirement, or alias values.

The full per-master assignment tables (task list, amount, extended amount, and
weight for all 9 masters) were verified against raw wiki task-table source on
2026-07-03. Five masters keep their table inline on their own page (pinned by
`wikiPageId`); Turael, Mazchna, Nieve, and Duradel transclude a
`<Master>/Slayer assignments` subpage (pageids 588186, 588188, 111066, 588189).
Where the master tables and a `Slayer_task/<name>` page disagree on unlock
requirements, the task page infobox was preferred. Araxytes are intentionally
listed for Turael and Spria with amounts but no weight: the wiki task page
records they are assigned indirectly as part of a spiders task.

A second full re-verification on 2026-07-03 (Turael rev 15227331, Mazchna rev
15248953, Nieve rev 15237204, Duradel rev 15240612, Vannaka rev 15235190,
Chaeldar rev 15235191, Krystilia rev 15235187, Spria rev 15196758, Konar rev
15237205) matched every amount, weight, and extended amount in the data. Konar
assigns flat (not ranged) amounts for ankou (50) and nechryael (110), stored as
`[50, 50]` and `[110, 110]`.

`konarLockable` is reconciled against the location column of Konar's own
assignment table (pageid 199880, rev 15237205): a location record is `true`
exactly when Konar's table lists that area for a task the record is linked to.
Konar's boss-task row has an empty location cell - boss tasks are not
location-locked, so the Boss meta-task legitimately has no lockable location.
Sailing-era areas absent from her table (Deepfin Mine, Buccaneers' Laboratory,
Charred Dungeon, Dragon Nest, Kurask Lair, Stalker Den, Mynydd, Entrana
Dungeon, Heroes' Guild basement) are `false` even where the monster lives
there; re-check her table before flipping any of these.

Task-counting one-off NPCs were verified on 2026-07-03 and stored as variant
files: The Jormungand ids 9290/9291 (freed combat form only; pageid 235947 rev
15215873), Kolodion's demon final form id 1609 (only form 5 gives slayer
credit; pageid 23540 rev 15199471), and Reanimated dagannoth id 7033 (pageid
70522 rev 15211530). Abyssal demon id groups (415/416 standard, 7241
Catacombs, 11239 Wilderness Slayer Cave; rev 15199214) and black dragon level
groups (227 = 252-259/8084/8085, 247 = 7861-7863 Wilderness Slayer Cave only;
rev 15199311) were confirmed against the same API.

The Slayer reward shop catalogue was fully reconciled against raw wiki source
on 2026-07-03 (Slayer Rewards pageid 323186 rev 15241609, Slayer reward point
pageid 26619 rev 15237224, Slayer equipment pageid 26161 rev 15114519). Point
costs reflect the Summer Sweep Up repricing (27 August 2025): Slug Salter and
Reptile Freezer 10, Ring Bling 150, Bigger and Badder 50, Task Storage 500,
Pedal to the Metals 200 (the separate mithril/adamant/rune dragon unlocks were
removed). Coverage split: `rewards/*.json` holds only the global,
varbit-answerable purchases the WA-13 unlock hint may recommend (7 unlocks +
10 cosmetic helmet recolours at 1,000 each); every monster-scoped shop row
(task unlocks such as Seeing Red or Lured In, extensions, finishing-blow
perks, Duly Noted, Stop the Wyvern, Double Trouble, Chance of Heavy Frost)
lives on its task's `unlocks[]` with an `UnlockType` tag. Per-master block
costs (Turael/Aya/Spria 40, Mazchna 50, Vannaka 60, Chaeldar 70, Konar 80,
Nieve 90, Duradel and Krystilia 100), the 30-point cancel, and the block-slot
rules (one slot per 50 quest points up to six, plus one from the Elite
Lumbridge & Draynor Diary, max 7) come from the same revision; block lists are
per-master except Turael/Aya/Spria, who share one. Re-verify against the
Slayer Rewards page before changing any cost or unlock scope.

Task `requiredItemId` values were reconciled against wiki item infobox ids on
2026-07-03: Facemask 4164, Slayer gloves 6720, Witchwood icon 8923, Boots of
stone 23037, Fungicide spray 7421 (charged; 7422-7430 part-used), Bag of salt
4161, Fishing explosive 6664, Slayer bell 10952, Insulated boots 7159,
Reinforced goggles 24942, Spiny helmet 4551, Crystal chime 28577. The lit bug
lantern (7053) is deliberately NOT pinned on harpie-bug-swarms: players bank
the unlit 7051 form, so an owned check on the lit id would false-negative;
the same charge-variant caveat is noted on mutated-zygomites.

## Cannon And Location-Awareness Verification

Cannon usage and per-task location awareness were fully verified on 2026-07-03
against raw wiki source. The authoritative prohibited-areas list is the Dwarf
multicannon page (pageid 13551, rev 15233141): every location record's `cannon`
flag and every task `locationComparison[].cannonable` value was reconciled
against it plus each task's `Slayer_task/<name>` Location Comparison table
(pinned by the task's `wikiPageId`; several ids were corrected to point at the
task page where one exists). Rules applied, in order of precedence:

- A prohibited-list area is never cannonable, even where a task table says
  otherwise (the one known wiki self-conflict is Mort'ton: its Shades table
  says Yes but the multicannon page bans it; the data keeps `false` with the
  eastern-boundary exception in notes).
- Cannon permission is per-area, not per-dungeon: Karuulm bans only the wyrm
  and Alchemical Hydra areas (drakes/hellhounds/regular hydras are
  cannonable), the Smoke Dungeon ban is the dust-devil task-only extension,
  Jormungand's Prison bans only the basilisk areas, the Iorwerth kurask area
  is cannon-immune while its elf/dark-beast/nechryael areas allow cannons,
  and Entrana's ban is the island surface, not Entrana Dungeon.
- Where wiki task tables split on the same shared area (God Wars Dungeon:
  ogres/vampyres Yes, aviansie/bloodveld/hellhounds No because inhabitants
  destroy cannons), the shared location file stays `cannon: false` with a
  nuance note and the per-task row carries the table's verdict. The runtime
  prefers the task-scoped value: `SlayerLocation.isCannonEffective()` lets
  `LocationQuality.cannonable` override the location flag in loadout packing,
  location choice, cannon DPS notes, and the panel cannon tag.
- Aggregate/synthetic locations (bat-spawns, ghost-spawns, rat-spawns,
  spiders-area) with genuinely mixed or unstated per-spot truth keep a
  conservative `false` flag and record the mixed reality in `accessNote`.
- No `cannonable` value is null: rows without a wiki table were resolved from
  monster-page `immunecannon`, strategy prose, and the prohibited list, and
  rows that remain unverifiable are explicit `false` with a note saying so
  (pirates, rats, rogues, shadow-warriors, sourhog-cave, fever-spiders,
  flesh-crawlers, magic-axes, minotaurs, chaos-druids, crocodiles,
  dark-warriors, lava-dragons, ice-warriors elsewhere).

Structural simplifications flagged during the sweep (kept by design, not data
errors): dwarves models 1 of 16 wiki rows, ghosts 1 of ~20, skeletons 2 of
~15, and the wolves/zombies/dogs tables have many wiki spots without location
files. Expand only with matching location records.

## Editing Rules

- Keep JSON valid and deterministic; use `jq empty` on changed JSON files.
- After changing compiler validation code, run `./gradlew clean generateSlayerData`;
  incremental runs can validate with a stale compiled class and report
  failures that are not real.
- Preserve OSRS Wiki facts as concise structured values and notes; do not paste
  raw MediaWiki markup into JSON.
- Prefer explicit arrays of notes, steps, risks, and requirements over long
  unstructured paragraphs when migrating strategy content.
- Add missing weapons before referencing them from a strategy.
- Do not add automated tests; the project has none by design. Pin wiki-derived
  facts by recording page and revision IDs in the source JSON or docs, and
  verify changes with `jq empty`, `./gradlew generateSlayerData`, and
  `./gradlew build`.

---
status: proposed
---

# Modular slayer data sources compile into one runtime dataset

## Context

`SlayerDataService` currently loads `/data/slayer-data.json` into a `TaskData` graph. That runtime shape
is simple, but the editable source has become too broad: 43 tasks, 209 variants, 78 location entries,
77 strategy blocks, and 78 distinct strategy weapon item IDs live in one file. Adding or removing a
slayer master, task, monster variant, location, weapon, or strategy requires editing unrelated data.

The user wants the data to be readable by LLMs and developers while remaining consumable by the plugin.
Gate 0 and Gate 1 approved a compiler-first T2 approach: modular repository data, strict validation,
full migration, generated runtime data, and no runtime external packs in v1.

## Decision

Use modular repository sources as the source of truth and compile them into a single generated runtime
dataset.

1. **Source files live under `src/main/data/slayer`.**
   - `masters/*.json`
   - `tasks/*.json`
   - `monsters/<monster-family-id>/*.json`
   - `locations/*.json`
   - `weapons/*.json`
   - `strategies/<strategy-id>/strategy.json`

2. **Structured data is JSON. Strategy prose and method context live in structured strategy JSON.**
   Strategy JSON carries the fields the plugin needs under `plugin`, plus source-only requirements,
   mechanics, methods, and equipment style options for LLM-assisted maintenance and future plugin surfaces.
   Legacy Markdown strategy files are still compiler-supported during migration.

3. **Cross-file references use stable IDs.**
   Raw NPC IDs and raw item IDs remain fields, but source files reference `taskId`, `monsterId`,
   `variantId`, `locationId`, `weaponId`, and `masterId`. The compiler resolves stable IDs into the
   runtime `TaskData` shape.

4. **The plugin runtime keeps a simple data load path.**
   `SlayerDataService` continues to load `/data/slayer-data.json`. Gradle generates that resource under
   `build/generated/resources/slayer/data/slayer-data.json` before resources are processed and tests run.

5. **The old monolithic runtime JSON is generated output, not source.**
   In v1 the checked source of truth moves to modular files. The generated JSON must be deterministic and
   equivalence-tested against the pre-migration runtime behavior.

6. **Validation is strict and build-gated.**
   The compiler fails on duplicate IDs, broken references, missing defaults, invalid combat styles,
   missing profile fields, strategy references to unknown variants/weapons, and generation instability.

## Consequences

- Data changes become reviewable in focused files.
- LLM edits can target one data domain without loading the whole runtime dataset.
- Runtime Java consumers avoid direct knowledge of the authoring layout.
- The build gains a generation step and validation tests.
- The compiler becomes a load-bearing artifact and needs its own unit tests.
- Runtime external data packs stay out of v1, reducing support and validation risk.

## Rejected options

- **Runtime modular loader:** rejected for v1 because it pushes validation and failure modes into the
  RuneLite client.
- **Overlay system on top of the current JSON:** rejected because it creates two editable sources of
  truth.
- **Markdown-only data:** rejected because validation and deterministic generation are weaker than with
  structured JSON.

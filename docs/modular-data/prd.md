# Modular Slayer Data Architecture - PRD

Date: 2026-06-30
Status: Implemented

## Problem

The plugin's slayer knowledge is concentrated in one broad runtime file,
`src/main/resources/data/slayer-data.json`, loaded by `SlayerDataService` into `TaskData`. That file is
now the place for master assignments, tasks, monsters, variants, locations, combat profiles, category
flags, and strategy-guide weapon overrides. The runtime shape is convenient for Java, but the source of
truth is too coupled for safe future development. A contributor or LLM adding one master, task, monster,
weapon, or strategy has to edit the monolith and understand unrelated data.

## Objective

Create a modular source-of-truth data system for AIO Slayer. The data must be readable and editable by
developers and LLMs, strongly validated during build/test, and compiled into the simple runtime dataset
the plugin needs.

## Users

- Plugin maintainer adding or correcting slayer knowledge.
- LLM-assisted development agents editing data with bounded context.
- Plugin runtime consuming the generated dataset.
- Future reviewer validating data changes before they affect recommendations.

## Requirements

### FR-1: Modular source layout

The project must introduce modular source files for:

- Slayer masters.
- Slayer tasks and assignment rules.
- Monster families and variants.
- Locations.
- Weapons and weapon details.
- Strategy-guide context and structured strategy overrides.

The layout must let one domain be edited without opening the entire generated runtime dataset.

### FR-2: Generated runtime dataset

The plugin must continue to consume a simple bundled runtime dataset in v1. Modular sources must compile
into deterministic generated data compatible with the plugin's load path, or into a deliberately
designed replacement load path approved at Gate 2.

### FR-3: Full migration

All current slayer data must be represented in modular source files in v1. The old monolithic source of
truth must not remain as a parallel editable source.

### FR-4: Strict validation

The build/test workflow must fail on invalid modular data, including at least:

- Duplicate stable IDs.
- Broken master, task, monster, variant, location, weapon, or strategy references.
- Missing required profile fields.
- Invalid combat styles or style/element combinations.
- Missing or multiple defaults where exactly one default is required.
- Strategy references to unknown variants or weapons.
- Non-deterministic generation.

### FR-5: Stable references

Modular data must use stable, human-readable IDs for cross-file references. Runtime-only details such as
NPC IDs and raw item IDs can remain as fields, but they must not be the only way source files link to
each other.

### FR-6: Weapon catalog

Weapons must have their own catalog covering names, raw item IDs, aliases or charge forms where
applicable, slot/style information, attack speed, and modeled special effects or passive effects used by
the loadout engine.

### FR-7: Strategy documents

Strategies must be separate source documents linked to monster variants. They must preserve readable
context for development while compiling the structured fields the plugin needs, including style, priority
weapon references, secondary weapon references, notes, and provenance URL.

### FR-8: Behavior preservation

Migrating to modular sources must preserve current recommendation behavior except for intentional,
approved schema or data corrections. Existing tests should stay green or be updated to assert the new
modular invariants.

### FR-9: Developer workflow docs

Documentation must explain how to add, remove, and validate:

- A slayer master.
- A task assignment.
- A monster or variant.
- A location.
- A weapon or weapon effect.
- A strategy guide.

## Non-goals

- Runtime loading of user-editable external data packs.
- Plugin UI for editing data.
- Automatic wiki scraping during normal builds.
- Broad recommendation algorithm changes unrelated to the data modularisation.
- Solving every current game-data gap; this work changes ownership and validation, not the whole data
  research backlog.

## Acceptance Criteria

- AC-1: A clean build/test run validates modular source files before or during the normal test workflow.
- AC-2: Generated runtime data is deterministic across repeated generation from identical inputs.
- AC-3: Every current task loaded by `SlayerDataService` is sourced from modular files.
- AC-4: Every current monster variant and strategy override is sourced from modular files.
- AC-5: Current data validation tests are replaced or extended so they validate the modular sources and
  generated output.
- AC-6: At least one negative validation test proves broken references fail the build/test path.
- AC-7: At least one contributor-doc example covers each add/remove workflow listed in FR-9.
- AC-8: Runtime plugin consumers do not need to know whether the data was authored modularly or by the old
  monolith.

## Success Metrics

- A contributor can identify the file to edit for a master, task, monster variant, weapon, or strategy in
  under one minute from docs.
- A broken cross-reference is caught by tests before runtime.
- Generated runtime data can be regenerated without noisy ordering diffs.
- The main test suite remains green after the migration.

## Open Architecture Decisions For Gate 2

- Exact directory and schema names.
- Whether strategy structured metadata lives in Markdown frontmatter or paired JSON.
- Whether generation happens into `src/main/resources/data/slayer-data.json` before commit, or into
  `build/generated/resources` during Gradle.
- Whether the compiler is Java/JUnit-backed, a Gradle task, or a small script invoked by Gradle.
- Raw item ID versus stable weapon ID policy for charged and degraded item forms.

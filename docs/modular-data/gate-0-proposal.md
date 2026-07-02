# Modular Slayer Data Architecture - Gate 0 Proposal

Date: 2026-06-30

## Problem

The plugin currently loads one bundled `src/main/resources/data/slayer-data.json` file into a `TaskData`
graph through `SlayerDataService`. That file now carries 43 tasks, all monster variants, locations,
master assignment metadata, and 77 strategy blocks. It works at runtime, but it is too large and too
coupled for safe LLM-assisted development. Adding or removing a slayer master, task, monster variant,
weapon, or strategy requires editing one broad JSON document and understanding several Java consumers at
once.

## Goal

Create a modular data architecture where slayer masters, tasks, monsters and variants, locations,
weapons and weapon details, and strategy-guide context are easy to add, remove, review, and validate.
The source data must be readable by LLMs and developers, while the plugin keeps a simple runtime load
path.

## Recommended Approach

Use a compiler-first modular data system:

- Author structured source data as small JSON files in the repo.
- Keep long-form strategy/provenance context in Markdown files with structured frontmatter or companion
  JSON.
- Compile and validate those modular sources at build/test time into one deterministic runtime dataset
  that the plugin loads.
- Migrate all existing slayer data into the new modular source layout in v1, avoiding two sources of
  truth.

## Working Decisions From Grill

- Runtime data: keep one generated bundled dataset for the plugin in v1.
- Source format: JSON for structured data; Markdown only for long-form strategy/provenance notes.
- Weapons: global weapon catalog referenced by ID from strategies and loadout rules.
- Monsters: global monster/variant catalog referenced from task assignments.
- Strategies: separate per-monster or per-variant strategy documents, with a structured summary compiled
  into runtime data.
- Runtime extensibility: no user-editable external data packs in v1; design the compiler so packs remain
  possible later.
- Validation: required Gradle build/test step; fail on broken references, duplicate IDs, missing defaults,
  invalid schemas, and non-deterministic generation.
- Migration: migrate the full current dataset in v1, with generated output proven equivalent except for
  intentional schema changes.
- Delivery track: T2 large feature, because the work changes data ownership, build tooling, validation,
  tests, docs, and potentially loadout contracts.

## Proposed Modular Sources

- `src/main/data/slayer/masters/*.json`
  - Stable master IDs, display names, unlock notes, task assignment references.
- `src/main/data/slayer/tasks/*.json`
  - Stable task IDs, assignment amounts, requirements, default monster variant references, task notes.
- `src/main/data/slayer/monsters/<monster-family-id>/*.json`
  - Monster family IDs and variant records: NPC IDs, combat stats, weakness profile, category flags,
    boss markers, locations, requirements.
- `src/main/data/slayer/locations/*.json`
  - Location IDs, display names, wilderness/cannon/multicombat flags, access notes.
- `src/main/data/slayer/weapons/*.json`
  - Item IDs, names, slot, styles, attack speed, passive effects, special rules, aliases and charge forms.
- `src/main/data/slayer/strategies/*.md`
  - Human/LLM-readable strategy documents with structured metadata linking variant IDs and weapon IDs.

The exact folder names and schema belong to Gate 2 design. This Gate 0 proposal approves the direction,
not the final schema.

## Acceptance Direction

The finished feature should prove:

- Existing plugin behavior remains green after the runtime dataset is generated from modular sources.
- `SlayerDataService` still has a simple load contract, or a deliberately designed replacement.
- Every current task, variant, location, weapon effect, and strategy override is represented in modular
  source files.
- The generated runtime data is deterministic and checked into the expected resource location or generated
  before tests/resources are consumed.
- Validation catches at least: broken master/task/monster/variant/weapon/strategy references, duplicate
  stable IDs, missing default variant selection, invalid strategy weapon IDs, invalid combat styles, and
  missing required profile fields.
- Documentation shows how to add and remove a slayer master, task, monster variant, location, weapon, and
  strategy guide.

## Non-goals For V1

- Loading user-edited data packs from disk at plugin runtime.
- Building a data editing UI.
- Automatically scraping wiki pages during normal plugin builds.
- Changing recommendation behavior beyond what is required to preserve current behavior against the
  generated dataset.

## Risks

- Data migration can silently alter behavior if generated output is not equivalence-tested.
- Weapon and strategy IDs need a clear raw-ID versus canonical-ID policy to avoid charge/variant drift.
- Markdown frontmatter parsing adds tooling complexity; Gate 2 should decide whether strategy metadata is
  frontmatter or a paired JSON file.
- Existing tests pin some current dataset assumptions; they must be updated carefully to validate modular
  sources, not the old monolith.

## Proposed Track And Roster

Track: T2 large feature.

Activated roster:

- Project lead: scope control and acceptance evidence.
- Product Manager or Business Analyst: PRD and acceptance criteria for modular data workflows.
- Architect: schema, compiler architecture, validation model, migration plan, ADRs.
- Backend Engineer: compiler/validator, Java data model adjustments, Gradle integration, migration.
- QA/Test Engineer: equivalence, schema, validation, regression, and developer workflow tests.
- Code Reviewer: review gate for generated-data and tooling changes.
- Technical Writer: contributor docs and examples after implementation is verified.

Excluded unless pulled in later:

- UX/UI Designer and Frontend Engineer: no plugin UI change is planned.
- DevOps/Release/Security: no production deploy/auth/secrets work is planned; revisit if external runtime
  data packs enter scope.

## Gate 0 Decision

Approve this as a T2 compiler-first modular data architecture project.

If approved, next step is Stage 1 PRD: precise requirements, acceptance criteria, non-goals, and a signed
scope delta for the modular data capability.

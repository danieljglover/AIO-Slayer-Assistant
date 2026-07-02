# Modular Slayer Data Architecture - Gate 2 Plan

Date: 2026-06-30
Status: Implemented and verified

## Architecture

The source of truth moves from `src/main/resources/data/slayer-data.json` to modular files under
`src/main/data/slayer`. A Java compiler/validator reads those files, resolves stable IDs, validates the
graph, and emits the same runtime `TaskData` JSON shape the plugin already consumes. Gradle wires the
generated resource into `processResources` and `test`, so broken modular data fails before runtime.

Runtime remains intentionally boring: `SlayerDataService` keeps loading `/data/slayer-data.json`.

## Data layout

```text
src/main/data/slayer/
  masters/
    duradel.json
  tasks/
    greater-demons.json
  monsters/
    greater-demons/
      greater-demon-lvl92.json
      tormented-demon-lvl450.json
  locations/
    catacombs-of-kourend.json
  weapons/
    demonbane.json
  strategies/
    k-ril-tsutsaroth/
      strategy.json
```

The exact file split can be expanded during implementation, but each source record must carry a stable
ID and all cross-file links must use stable IDs.

## Compiler package

Create `src/main/java/com/danieljglover/allinslayer/data/source/`:

- `ModularSlayerDataCompiler` - orchestrates load, validate, compile.
- `ModularSlayerDataCli` - Gradle/CLI entry point.
- `ModularSlayerDataSet` - in-memory source graph.
- `CompiledSlayerData` - compiled runtime task list wrapper.
- `SlayerDataValidationException` - reports all validation errors in one failure.
- `StrategyMarkdownParser` - parses legacy strategy Markdown during migration.
- JSON strategy loading - parses `strategies/<strategy-id>/strategy.json` with plugin fields plus full
  method/equipment context.
- Source model classes for masters, tasks, monsters, variants, locations, weapons, strategies.

The compiler should not depend on RuneLite client runtime APIs beyond existing model enums/classes.

## Gradle integration

Add a `generateSlayerData` task that:

1. Depends on compiled Java classes.
2. Reads `src/main/data/slayer`.
3. Writes `build/generated/resources/slayer/data/slayer-data.json`.
4. Makes `processResources` and `test` depend on generation.
5. Adds `build/generated/resources/slayer` as a main resources source directory.

## Migration waves

1. Compiler skeleton and negative validation tests.
2. Gradle generation into a generated resource while preserving current runtime load behavior.
3. Modular source schema and seed files for a small representative slice.
4. Full mechanical migration of the current 43 tasks, 209 variants, 78 location entries, 77 strategy
   blocks, and weapon references.
5. Equivalence and determinism tests against current behavior.
6. Contributor documentation for add/remove workflows.
7. Code review and final verification.

## Verification strategy

- Unit tests for parser and validation failures.
- Determinism test that compiles twice and compares exact JSON.
- Dataset tests that assert every current task, variant, strategy, location, and weapon reference is
  represented in modular sources.
- Runtime load test using `SlayerDataService`.
- Full `./gradlew cleanTest test`.

## Open decisions resolved here

- Strategy metadata uses Markdown frontmatter in v1.
- Generated resource goes to `build/generated/resources/slayer`, not committed output.
- Compiler is Java and Gradle-invoked.
- Stable weapon IDs are the source references; raw item IDs remain weapon catalog fields.

## Gate 2 acceptance checklist

- ADR-0016 is accepted.
- Implementation plan is approved.
- Board tasks are dependency ordered.
- No source implementation starts before this gate is approved.

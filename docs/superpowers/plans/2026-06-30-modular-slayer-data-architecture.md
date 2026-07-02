# Modular Slayer Data Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the monolithic editable slayer data file with modular source files that compile into the runtime dataset consumed by the plugin.

**Architecture:** Modular JSON and Markdown sources live under `src/main/data/slayer`. A Java compiler/validator reads them, resolves stable IDs, validates the graph, and emits `data/slayer-data.json` as a generated resource. `SlayerDataService` keeps its simple runtime contract.

**Tech Stack:** Java 11, Gradle, Gson, JUnit 4, existing AIO Slayer model classes.

## Global Constraints

- No runtime external data packs in v1.
- Source-of-truth data must move to modular files.
- Generated runtime data must be deterministic.
- `SlayerDataService` must continue to load `/data/slayer-data.json` unless Gate 2 is explicitly amended.
- Validation must fail the build/test workflow on broken references, duplicate IDs, missing defaults, invalid combat styles, unknown strategy weapons, and missing required profile fields.
- Keep implementation test-first.
- Do not change recommendation behavior except where required to preserve current behavior with generated data.

---

## File Structure

- Create `src/main/data/slayer/masters/` for master records.
- Create `src/main/data/slayer/tasks/` for assignment/task records.
- Create `src/main/data/slayer/monsters/` for monster family and variant records.
- Create `src/main/data/slayer/locations/` for reusable location records.
- Create `src/main/data/slayer/weapons/` for weapon records.
- Create `src/main/data/slayer/strategies/` for strategy Markdown records.
- Create `src/main/java/com/danieljglover/allinslayer/data/source/` for compiler, parser, validation, and source model classes.
- Modify `build.gradle` to add generated resources and the `generateSlayerData` task.
- Modify or extend `src/test/java/com/danieljglover/allinslayer/data/` tests to validate modular sources and generated runtime data.
- Add contributor documentation at `docs/modular-data/contributor-guide.md`.

---

### Task 1: Source Model And Strategy Parser

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceMaster.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceTask.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceMonsterFamily.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceMonsterVariant.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceLocation.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceWeapon.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategy.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/StrategyMarkdownParser.java`
- Test: `src/test/java/com/danieljglover/allinslayer/data/source/StrategyMarkdownParserTest.java`

**Interfaces:**
- Produces: `StrategyMarkdownParser.parse(String path, String markdown): SourceStrategy`
- Produces: source model getters/setters usable by Gson and compiler tasks.

- [ ] **Step 1: Write parser tests**

Create `StrategyMarkdownParserTest` with:

```java
package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StrategyMarkdownParserTest
{
    @Test
    public void parsesFrontmatterAndBody()
    {
        String md = "---\n"
            + "strategyId: tormented-demon\n"
            + "variantIds: [tormented-demon]\n"
            + "primaryStyle: MELEE\n"
            + "primaryWeapons: [emberlight, arclight]\n"
            + "secondaryWeapons:\n"
            + "  - weaponId: scorching-bow\n"
            + "    style: RANGED\n"
            + "note: Switch to Scorching bow for the shield-down window\n"
            + "sourceUrl: https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies\n"
            + "---\n"
            + "# Tormented Demon\n"
            + "Use demonbane melee and a ranged swap.\n";

        SourceStrategy strategy = StrategyMarkdownParser.parse("strategies/tormented-demon.md", md);

        assertEquals("tormented-demon", strategy.getStrategyId());
        assertEquals("tormented-demon", strategy.getVariantIds().get(0));
        assertEquals(CombatStyle.MELEE, strategy.getPrimaryStyle());
        assertEquals("emberlight", strategy.getPrimaryWeapons().get(0));
        assertEquals("scorching-bow", strategy.getSecondaryWeapons().get(0).getWeaponId());
        assertEquals(CombatStyle.RANGED, strategy.getSecondaryWeapons().get(0).getStyle());
        assertTrue(strategy.getBody().contains("demonbane melee"));
    }
}
```

- [ ] **Step 2: Run parser test and confirm it fails**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.StrategyMarkdownParserTest`

Expected: compile failure because `SourceStrategy` and `StrategyMarkdownParser` do not exist.

- [ ] **Step 3: Implement source model classes**

Create Lombok `@Data @NoArgsConstructor` classes with fields matching the test:

```java
package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceStrategy
{
    private String strategyId;
    private List<String> variantIds;
    private CombatStyle primaryStyle;
    private List<String> primaryWeapons;
    private List<SourceStrategyWeapon> secondaryWeapons;
    private String note;
    private String sourceUrl;
    private String body;
}
```

Use the same package and Lombok pattern for the other source model classes. Keep fields simple and public via getters/setters:

```java
private String masterId;
private String taskId;
private String monsterId;
private String variantId;
private String locationId;
private String weaponId;
```

Add `SourceStrategyWeapon` as either a small top-level class or nested static class with `weaponId` and `CombatStyle style`.

- [ ] **Step 4: Implement `StrategyMarkdownParser`**

Implement a minimal parser that:

- Requires the document to start with `---`.
- Reads lines until the closing `---`.
- Supports scalar `key: value`.
- Supports inline lists like `[a, b]`.
- Supports the `secondaryWeapons` list shape shown in the test.
- Stores the rest as `body`.

- [ ] **Step 5: Run parser test**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.StrategyMarkdownParserTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/data/source src/test/java/com/danieljglover/allinslayer/data/source/StrategyMarkdownParserTest.java
git commit -m "feat: add modular strategy source parser"
```

---

### Task 2: Compiler Skeleton And Validation Errors

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompiler.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataSet.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SlayerDataValidationException.java`
- Test: `src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompilerTest.java`

**Interfaces:**
- Consumes: source models from Task 1.
- Produces: `ModularSlayerDataCompiler.compile(Path sourceRoot): List<TaskData>`
- Produces: `SlayerDataValidationException.getErrors(): List<String>`

- [ ] **Step 1: Write validation failure tests**

Create tests for duplicate IDs and broken references:

```java
@Test
public void duplicateMasterIdsFailValidation()
{
    Path root = tempDir();
    write(root.resolve("masters/a.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
    write(root.resolve("masters/b.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel copy\"}");

    try
    {
        ModularSlayerDataCompiler.compile(root);
        fail("expected validation failure");
    }
    catch (SlayerDataValidationException ex)
    {
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("duplicate masterId: duradel")));
    }
}
```

Add a second test where a task references `variantIds:["missing-variant"]` and assert the error contains `unknown variantId: missing-variant`.

- [ ] **Step 2: Run tests and confirm failure**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataCompilerTest`

Expected: compile failure because compiler classes do not exist.

- [ ] **Step 3: Implement exception and compile entry point**

Implement:

```java
public final class SlayerDataValidationException extends RuntimeException
{
    private final List<String> errors;

    public SlayerDataValidationException(List<String> errors)
    {
        super(String.join("\n", errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public List<String> getErrors()
    {
        return errors;
    }
}
```

`ModularSlayerDataCompiler.compile(Path)` should load JSON files with Gson, collect errors, and throw `SlayerDataValidationException` when errors are present. Return an empty list until Task 4 builds real compilation.

- [ ] **Step 4: Run validation tests**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataCompilerTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/data/source src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompilerTest.java
git commit -m "feat: validate modular slayer data references"
```

---

### Task 3: Gradle Generation Task

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCli.java`
- Modify: `build.gradle`
- Test: `src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCliTest.java`

**Interfaces:**
- Consumes: `ModularSlayerDataCompiler.compile(Path)`.
- Produces CLI: `ModularSlayerDataCli <sourceRoot> <outputJson>`.

- [ ] **Step 1: Write CLI output test**

Create a test that invokes `ModularSlayerDataCli.main(new String[]{sourceRoot, outputJson})` against a tiny valid source fixture and asserts the output file exists and starts with `[`.

- [ ] **Step 2: Run CLI test and confirm failure**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataCliTest`

Expected: compile failure because CLI does not exist.

- [ ] **Step 3: Implement CLI**

Implement `main(String[] args)`:

```java
Path sourceRoot = Paths.get(args[0]);
Path output = Paths.get(args[1]);
List<TaskData> tasks = ModularSlayerDataCompiler.compile(sourceRoot);
Files.createDirectories(output.getParent());
try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8))
{
    new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(tasks, writer);
}
```

- [ ] **Step 4: Wire Gradle**

Modify `build.gradle`:

```groovy
def generatedSlayerDataDir = layout.buildDirectory.dir('generated/resources/slayer')

sourceSets {
    main {
        resources {
            srcDir generatedSlayerDataDir
        }
    }
}

tasks.register('generateSlayerData', JavaExec) {
    dependsOn 'classes'
    mainClass = 'com.danieljglover.allinslayer.data.source.ModularSlayerDataCli'
    classpath = sourceSets.main.runtimeClasspath
    args file('src/main/data/slayer').absolutePath,
        generatedSlayerDataDir.get().file('data/slayer-data.json').asFile.absolutePath
}

tasks.named('processResources') {
    dependsOn 'generateSlayerData'
}

tasks.named('test') {
    dependsOn 'generateSlayerData'
}
```

- [ ] **Step 5: Run Gradle task**

Run: `./gradlew generateSlayerData`

Expected: task succeeds once a minimal valid source fixture exists, and writes `build/generated/resources/slayer/data/slayer-data.json`.

- [ ] **Step 6: Commit**

```bash
git add build.gradle src/main/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCli.java src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCliTest.java
git commit -m "feat: generate slayer runtime data from modular sources"
```

---

### Task 4: Compile A Representative Slice

**Files:**
- Create: `src/main/data/slayer/masters/duradel.json`
- Create: `src/main/data/slayer/tasks/greater-demons.json`
- Create: `src/main/data/slayer/monsters/greater-demons/greater-demon-lvl92.json`
- Create: `src/main/data/slayer/monsters/greater-demons/tormented-demon-lvl450.json`
- Create: `src/main/data/slayer/locations/catacombs-of-kourend.json`
- Create: `src/main/data/slayer/weapons/demonbane.json`
- Create: `src/main/data/slayer/strategies/tormented-demon.md`
- Modify: `ModularSlayerDataCompiler.java`
- Test: `src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompilationTest.java`

**Interfaces:**
- Consumes source files.
- Produces one `TaskData` for Greater demons with variants and strategy fields populated.

- [ ] **Step 1: Write slice compilation test**

Assert:

- `compile(root)` returns one task named `Greater demons`.
- The task is assigned by `duradel`.
- It has a Tormented Demon variant.
- Tormented Demon's strategy primary weapon item ID resolves from weapon ID `emberlight`.

- [ ] **Step 2: Run test and confirm failure**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataCompilationTest`

Expected: failure because compiler returns no tasks or does not populate variants.

- [ ] **Step 3: Add source fixture files**

Create the representative source files with stable IDs:

```json
{
  "masterId": "duradel",
  "name": "Duradel",
  "aliases": ["kuradal"]
}
```

Use `taskId: "greater-demons"`, `variantIds: ["greater-demon", "tormented-demon"]`, and weapon records for `emberlight`, `arclight`, and `scorching-bow`.

- [ ] **Step 4: Implement compilation mapping**

Implement enough mapping to create `TaskData`, `MonsterVariant`, `MonsterStrategy`, and `StrategyWeapon` from the source graph. Keep ordering deterministic by sorting IDs.

- [ ] **Step 5: Run slice tests**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataCompilationTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/data/slayer src/main/java/com/danieljglover/allinslayer/data/source src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompilationTest.java
git commit -m "feat: compile representative modular slayer data slice"
```

---

### Task 5: Full Data Migration

**Files:**
- Modify/create all files under `src/main/data/slayer/`
- Test: `src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataMigrationTest.java`

**Interfaces:**
- Consumes current `src/main/resources/data/slayer-data.json` as migration input only.
- Produces modular source files representing all current data.

- [ ] **Step 1: Write migration coverage test**

Test compiled modular data has:

```java
assertEquals(43, tasks.size());
assertEquals(209, variantCount(tasks));
assertEquals(77, strategyCount(tasks));
```

Also assert task names match the existing Duradel canonical set from `DuradelDatasetValidationTest`.

- [ ] **Step 2: Run test and confirm failure**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataMigrationTest`

Expected: failure because only the representative slice exists.

- [ ] **Step 3: Migrate current data**

Mechanically split every current task, variant, location, and strategy into modular files. Preserve names,
NPC IDs, combat profiles, category flags, boss flags, location labels, requirements, strategy notes, source URLs,
and raw item IDs.

- [ ] **Step 4: Run migration test**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataMigrationTest`

Expected: PASS with 43 tasks, 209 variants, 77 strategy blocks.

- [ ] **Step 5: Commit**

```bash
git add src/main/data/slayer src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataMigrationTest.java
git commit -m "feat: migrate slayer data into modular sources"
```

---

### Task 6: Determinism And Runtime Loading

**Files:**
- Test: `src/test/java/com/danieljglover/allinslayer/data/source/SlayerDataGenerationDeterminismTest.java`
- Modify: `src/test/java/com/danieljglover/allinslayer/data/SlayerDataServiceTest.java`

**Interfaces:**
- Consumes: compiler and generated resource.
- Produces: deterministic JSON and runtime load proof.

- [ ] **Step 1: Write determinism test**

Compile the same source root twice into strings and assert exact equality.

- [ ] **Step 2: Write runtime load assertion**

Extend `SlayerDataServiceTest.loadsBundledTasks` to assert:

```java
assertEquals(43, service.all().size());
assertTrue(service.byTaskName("Greater demons").isPresent());
assertTrue(service.byTaskName("Boss").isPresent());
```

- [ ] **Step 3: Run tests and confirm failure if generation is not deterministic**

Run: `./gradlew test --tests com.danieljglover.allinslayer.data.source.SlayerDataGenerationDeterminismTest --tests com.danieljglover.allinslayer.data.SlayerDataServiceTest`

Expected: PASS after deterministic ordering is implemented.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/danieljglover/allinslayer/data/source/SlayerDataGenerationDeterminismTest.java src/test/java/com/danieljglover/allinslayer/data/SlayerDataServiceTest.java
git commit -m "test: prove generated slayer data is deterministic and loadable"
```

---

### Task 7: Contributor Documentation

**Files:**
- Create: `docs/modular-data/contributor-guide.md`

**Interfaces:**
- Consumes: final source layout and compiler behavior.
- Produces: add/remove workflows for maintainers and LLM agents.

- [ ] **Step 1: Write guide**

The guide must include one concrete add/remove example for each:

- Slayer master.
- Task assignment.
- Monster variant.
- Location.
- Weapon or weapon effect.
- Strategy guide.

- [ ] **Step 2: Verify references**

Run: `rg -n "master|task|variant|location|weapon|strategy" docs/modular-data/contributor-guide.md`

Expected: each data type appears in an add/remove section.

- [ ] **Step 3: Commit**

```bash
git add docs/modular-data/contributor-guide.md
git commit -m "docs: document modular slayer data workflows"
```

---

### Task 8: Full Verification And Review Prep

**Files:**
- Create or update: `docs/modular-data/verification.md`

**Interfaces:**
- Consumes: all prior tasks.
- Produces: verification evidence for review and Gate 4.

- [ ] **Step 1: Run full suite**

Run: `./gradlew cleanTest test`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Record verification**

Create `docs/modular-data/verification.md` with:

- Command run.
- Test result summary.
- Generated resource path.
- Counts: tasks, variants, strategies.
- Known non-goals.

- [ ] **Step 3: Commit**

```bash
git add docs/modular-data/verification.md
git commit -m "chore: record modular data verification evidence"
```

---

## Coverage Self-Review

- FR-1 modular layout: Tasks 1, 4, 5.
- FR-2 generated runtime dataset: Tasks 3, 6.
- FR-3 full migration: Task 5.
- FR-4 strict validation: Tasks 2, 6.
- FR-5 stable references: Tasks 1, 2, 4.
- FR-6 weapon catalog: Tasks 1, 4, 5.
- FR-7 strategy documents: Tasks 1, 4, 5.
- FR-8 behavior preservation: Tasks 5, 6, 8.
- FR-9 docs: Task 7.

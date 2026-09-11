# Data-generation isolation

## B4 changes

The 31 authoring Java files (33 compiled classes including nested classes) moved
unchanged from `src/main/java/.../data/source/` to
`src/dataGenerator/java/com/danieljglover/allinslayer/data/source/`. Their package
names and source DTO/compiler organization are unchanged. The six authoring
Gson/GsonBuilder construction sites now compile in `compileDataGeneratorJava`,
outside the Hub-recorded runtime `compileJava` task.

Both generation tasks use `dataGenerator.runtimeClasspath` and depend on
`dataGeneratorClasses`. The authoring classpaths include only main class output
and the existing main compile dependencies; they have a separate Lombok
annotation processor. This preserves the dependency versions and shares runtime
model classes without depending on generated resources. No authoring output or
new dependencies were added to the runtime or development launcher classpaths.
See the [build graph and maintenance rules](../agents/slayer-data-source.md#compiler-source-set).

`src/main/data/slayer` remains the source of truth and was not edited. All four
generated JSON resources remain under `build/generated/resources/slayer/data`.
The removed editable `src/main/resources/data/slayer-data.json` was not recreated.

## Verification (2026-09-11)

- `./gradlew clean generateSlayerData build`: passed from an empty build
  directory, including separate runtime/authoring compilation and generation.
- All 31 moved Java files are byte-identical to their pre-move sources.
- All four generated resources are byte-identical to the pre-change build.
  The catalogue still contains 10 masters, 118 tasks, 388 variants and 1,403
  methods. Existing unresolved-label guidance is unchanged.
- Normal and preview JARs contain the same generated resources and no
  `com/danieljglover/allinslayer/data/source/` entries. `build previewJar`
  and the development launcher compilation also passed.
- The standalone `generateAdvisorCatalogue` task graph reaches `compileJava`,
  `compileDataGeneratorJava` and `dataGeneratorClasses` without
  `processResources` or a cycle. The same task ran successfully in both clean
  builds.

| Generated resource | SHA-256 before and after |
| --- | --- |
| `advisor-catalogue.json` | `cce5a503907996f0e7b96593ca14c308be9a7d19f8b3c754ae1b410870636ef8` |
| `advisor-coverage.json` | `37101dc7c21386c783cae81eea7572bbd0b7af1df1f4fd7ad88834e8447d6a9e` |
| `slayer-data.json` | `d50528f228e1ad338993235e858e019e56f74babc1bef2e823d6fd898bcaf3c6` |
| `slayer-meta.json` | `65dea545f924062b85842447454f8343cb0b0ac87378aa8eb1ce67b73763a248` |

## Hub recording and artifact

Used official tooling commit `9b441d86aa16ec9500ca4654b4c14696890c9cba` and
RuneLite `1.12.38`. Ran a second clean build with the unchanged upstream
[target_init.gradle](https://github.com/runelite/plugin-hub-tooling/blob/9b441d86aa16ec9500ca4654b4c14696890c9cba/bundle/src/main/resources/target_init.gradle),
invoking `runelitePluginHubPackage` and `runelitePluginHubManifest`. A separate
local init script only enabled javac process forking and module exports needed
by the recorder. No sources were filtered or excluded by verification scripts.

The official recorder captured **596 API references from the full production
`compileJava`**. The upstream API checker reported **zero restricted API
findings** against its disallowed-API list. This clears B3's remaining full-source
scan dependency. Gson/GsonBuilder constructors and WidgetInfo/WidgetID are absent
from the recorded calls.

The resulting Hub `plugin.jar`:

- Contains all four generated resources with the hashes above.
- Contains 130 runtime classes, all Java 11 bytecode.
- Contains no authoring classes, development launcher, nested JARs or bundled
  third-party classes.
- Is 3,155,135 bytes, below the Hub's 10 MiB artifact limit.
- Has SHA-256 `244ddda1bb8ee58690fe84ca74f90ee356a092785f89161e304415ab83483ea6`.

These are actual upstream recording and packaging tasks, followed by the
official restricted-API checker. This was not the full packager's
submission/configuration validation or a Hub submission. B5's custom-build
manifest/formatting work and the final release-commit checks remain separate.

Claude Sonnet with medium effort and Kimi independently reviewed the build
wiring and verification evidence. Both approved the final change with no
blocking findings. No automated tests or test dependencies were added.

## Live packaged-JAR verification

Launched through the established Jagex/Bolt launch process with the owner's
Main profile, installed plugins and Wilderness Sentinel. The launch classpath
used the verified Hub `plugin.jar` for AIO, with no main classes/resources
directories or authoring output as a fallback. Existing launcher credentials
were inherited in memory through the established helper.

After the owner logged in, the AIO panel displayed the active Skeleton task
(66 remaining), location, recommendation and equipment. Switching the plugin
panel to Catalogue displayed an Aberrant spectre recommendation with location
and requirement guidance. Returned the panel to Active task. This verifies that
the packaged catalogue loads and drives both panels. Only RuneLite/plugin-panel
controls were operated by the agent; the owner handled all game interaction.

## Local evidence

Evidence is under `/tmp/aio-b4-team/`:

- `baseline-resources.json`, `baseline-authoring.json`, `baseline/`:
  pre-change comparison material, used only for verification.
- `clean-build.log`, `preview-build.log`, `standalone-task-graph.log`:
  generation, compilation, packaging and task order.
- `equivalence.log`: byte-for-byte source/resource comparisons.
- `hub-build-command.txt`, `hub-build.log`, `hub/api`, `hub/plugin.jar`:
  upstream recording/package run and outputs.
- `check-apis.jsh`, `api-result.txt`, `api-scan-result.log`:
  official restricted-API checker invocation and result.
- `hub-artifact-check.log`, `hub-jar-entries.txt`: artifact verification.
- `claude-final.log`, `kimi-final.log`: independent final reviews.
- `profile-client.log`, `profile-artifact-verification.txt`: established-profile
  launch and verification of the packaged AIO classpath.
- `01-existing-profile-active-task.png`, `02-existing-profile-catalogue.png`:
  live active-task and catalogue panels from the packaged artifact.

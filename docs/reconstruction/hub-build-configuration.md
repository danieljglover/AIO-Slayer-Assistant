# Explicit Hub build configuration

## B5 changes

`runelite-plugin.properties` now declares `build=gradle`. This selects the custom
build that compiles authoring code separately and generates the bundled Slayer
resources. Standard mode would replace the Gradle scripts and remove those
generation tasks.

The Windows preview's `FixCrLfFilter` invocation is wrapped across two lines;
its arguments and CRLF conversion behavior are unchanged. The maximum weighted
line widths are now 118 in `build.gradle` and 34 in `settings.gradle`. The Hub
counts tabs as eight columns and non-ASCII characters as four.

## Official verification (2026-09-11)

Used the unchanged official plugin-hub-tooling checkout at
`9b441d86aa16ec9500ca4654b4c14696890c9cba`:

- [Build-mode parser](https://github.com/runelite/plugin-hub-tooling/blob/9b441d86aa16ec9500ca4654b4c14696890c9cba/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java#L422)
  consumes `build=gradle` before checking for unknown properties.
- [Formatting gate](https://github.com/runelite/plugin-hub-tooling/blob/9b441d86aa16ec9500ca4654b4c14696890c9cba/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java#L515)
  applies the weighted 120-column limit to Gradle scripts.
- [Explicit-build gate](https://github.com/runelite/plugin-hub-tooling/blob/9b441d86aa16ec9500ca4654b4c14696890c9cba/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java#L976)
  rejects missing build mode during strict verification.

First, `./gradlew clean generateSlayerData build` passed in the working tree.
Then built upstream `:package:shadowJar`, prepared its complete RuneLite
`1.12.38` API using the upstream preparer, and downloaded the official Hub
dependency-verification metadata. Invoked the public upstream
`Plugin.build("1.12.38", true)` through JShell, with strict checks enabled.
No verifier code or checks were modified, overridden or skipped.

The packager operated on an isolated copy of the working-tree build inputs:
`src`, `gradle`, `packaging`, build scripts, properties, README and LICENSE.
Its descriptor was stored in a temporary local Git repository so the normal
descriptor-history checks could run. `Plugin.download()` was not invoked;
these are uncommitted local inputs, not a remotely fetched release commit.
The descriptor's base-commit label is not evidence that the working-tree patch
exists at that commit. The final release still needs R1 verification against
the actual committed source and complete release archive.

The strict run **passed**, including build-mode parsing, formatting,
dependency verification, resource generation, API compatibility/restrictions,
plugin descriptor validation and final metadata/JAR packaging. The completed
artifact contains `runelite_plugin.json` and has valid ZIP integrity.

| Check | Result |
| --- | --- |
| Full production API record | 596 references; strict upstream checks passed |
| Generated JSON | All four resources byte-identical to the B4 baseline |
| Runtime classes | 130, all Java 11; no authoring classes |
| Official source archive | All 31 authoring Java files retained; exact build scripts and `build=gradle` present |
| Final JAR size | 2,841,849 bytes |
| Final JAR SHA-256 | `2c7310baa75142b6460c0a1fec6f7a0ad7a2187361efe672308049410318f653` |

See [B4 resource hashes](data-generation-isolation.md#verification-2026-09-11).
Both final build configuration files match the packager's inputs byte for byte.
No automated tests or test dependencies were introduced.

## Verification environment

The first run using the system Arch OpenJDK `11.0.32.1` passed the build and
strict validation but failed during the packager's final ZIP rewrite with
`invalid entry compressed size`. The same unmodified inputs and verifier passed
with checksum-verified Adoptium Java `11.0.32+101` in a temporary directory.
The precise compression-runtime cause was not diagnosed. The
[Hub workflow](https://github.com/runelite/plugin-hub/blob/master/.github/workflows/build.yml)
also selects an Adopt Java 11 distribution. Use that runtime family for the
final local packaging check; the system Java installation was not changed.

Claude Sonnet with medium effort completed an independent review and found no
issues. Kimi could not review B5 because its five-hour usage quota was exhausted.
The existing Main-profile RuneLite client was left running; B5 changes build
configuration and formatting, with no runtime Java or data changes.

## Evidence

Local evidence is under `/tmp/aio-b5-team/`:

- `clean-build.log`, `tooling-build.log`, `prepare-api.log`: successful builds
  and official API preparation.
- `invoke-packager.jsh`, `packager-invocation.log`, `packager-build.log`,
  `packager-result.txt`: unmodified upstream strict build invocation and pass.
- `artifacts/all-in-slayer.jar`, `artifacts/all-in-slayer.zip`: official final
  plugin JAR and source archive.
- `raw-artifacts/api`, `artifact-verification.log`: recorded API and artifact
  integrity/resource checks.
- `jdk-download.json`: temporary JDK version, upstream download URL and checksum.
- `system-jdk-packager-invocation.log`: initial system-runtime ZIP failure.
- `claude-corrected.log`, `kimi-review.log`: final review and quota limitation.

# AIO Slayer Assistant

RuneLite external plugin in Java 11. The plugin is a passive Old School
RuneScape Slayer advisor: it reads game state, shows task guidance, and never
performs game actions.

## Commands

- `./gradlew build`: compile verification; run before claiming code or data
  migrations are complete.
- `./gradlew generateSlayerData`: compile modular Slayer source JSON into the
  generated runtime resource; also validates IDs and cross-file references.
- `find src/main/data/slayer -type f -name '*.json' -print0 | xargs -0 -n1 jq empty`:
  validate all Slayer source JSON files.
- `./gradlew run`: launch RuneLite with the plugin loaded for manual testing.

## Architecture

- `src/main/java/com/danieljglover/allinslayer/`: plugin entry point, config,
  recompute loop, and top-level orchestration.
- `src/main/java/com/danieljglover/allinslayer/task/advisor/`: Slayer task,
  boss, and assignment-area detection from RuneLite game tables.
- `src/main/java/com/danieljglover/allinslayer/data/`: runtime data loading and
  indexing.
- `src/main/java/com/danieljglover/allinslayer/data/source/`: modular Slayer
  source compiler, source models, validation, and CLI generation.
- `src/main/java/com/danieljglover/allinslayer/loadout/advisor/`: wiki-first
  equipment, eligibility, inventory, and qualitative goal ranking.
- `src/main/java/com/danieljglover/allinslayer/ui/advisor/`: native task and
  catalogue panel plus passive overlay.
- `src/main/data/slayer/`: editable Slayer knowledge base; see
  `docs/agents/slayer-data-source.md`.
- `build/generated/resources/slayer/data/advisor-catalogue.json`: generated
  runtime resource consumed by `AdvisorDataService`.
- `docs/reconstruction/implementation.md`: current reconstruction interfaces;
  historical design documents describe superseded runtime code.

## Code Style

- Java targets release 11.
- Follow existing package boundaries. Put source-data models and compiler logic
  in `data/source`, runtime plugin models in `model`, and UI code in `ui`.
- Existing source DTOs use Lombok `@Data` and `@NoArgsConstructor`; match that
  pattern for new modular data source classes.
- Keep RuneLite client-state reads on the client thread and Swing updates on the
  EDT.
- Comments should explain non-obvious RuneLite, OSRS, or data-migration
  constraints; avoid comments that restate the code.

## Rules

- Edit Slayer knowledge in `src/main/data/slayer`, not in generated `build/`
  output.
- Treat `src/main/resources/data/slayer-data.json` as removed legacy data; do
  not recreate it as an editable source of truth.
- Preserve plugin compliance: no automation, no game actions, no input
  generation, no live prayer/tile prompts, no runtime HTTP data collection.
- Keep source IDs stable, lowercase, and hyphenated. Cross-file references use
  IDs, not display names.
- Prefer small, focused edits. Do not reformat or rewrite unrelated dirty files.
- Use ASCII in project docs and source data unless the existing file already
  needs non-ASCII content.

## Slayer Data

The modular Slayer data is a normalized source graph:

- `masters/*.json`: Slayer master catalog.
- `tasks/*.json`: assignments, requirements, linked variants, linked
  locations, task notes, and recommendation summary.
- `monsters/<family>/*.json`: one combat variant per JSON file.
- `locations/*.json`: reusable location flags and access notes.
- `weapons/*.json`: stable strategy weapon IDs mapped to item IDs.
- `strategies/<strategy-id>/strategy.json`: preferred strategy format with
  plugin recommendations plus LLM-readable methods and style options.
- `advisor/*.json`: reviewed wiki equipment, eligibility and context mappings;
  see `docs/agents/advisor-catalogue.md` before changing runtime data rules.

When migrating OSRS Wiki content, every meaningful strategy method must be
represented in JSON, including solo, tank, attacker, safespot, cannon, barrage,
travel, location-specific, skip/block, and combat-style alternatives. Do not
collapse full wiki strategy sections into a single short note.

Detailed field interpretation: `docs/agents/slayer-data-source.md`.

## OSRS Wiki Workflow

For Slayer data migrations, verify against current OSRS Wiki MediaWiki source
through the API, not just rendered HTML. Record source URLs, page IDs, revision
IDs, and timestamps when docs depend on that evidence.

Use this pattern:

```bash
curl -L --fail --silent \
  'https://oldschool.runescape.wiki/api.php?action=query&prop=revisions|info&rvprop=ids|timestamp|content&rvslots=main&titles=Slayer_task/Waterfiends|Waterfiend|Waterfiend/Strategies&format=json&formatversion=2' \
  -o /tmp/osrs-wiki-evidence.json

jq -r '.query.pages[] | [.title, .pageid, .lastrevid, .touched, (.revisions[0].revid // "missing"), (.revisions[0].timestamp // "missing")] | @tsv' \
  /tmp/osrs-wiki-evidence.json
```

Treat missing `/Strategies` pages as evidence. Many normal Slayer monsters keep
their full strategy on `Slayer_task/<monster>` instead.

Detailed MediaWiki syntax notes: `docs/agents/osrs-wiki-source.md`.

## Verification

This project has NO automated tests by design: the owner tests everything
manually. Do not create test files, test source sets, or JUnit/Mockito
dependencies.

- JSON source edits need `jq empty` across changed files, or all Slayer JSON
  files when the migration is broad, plus a `./gradlew generateSlayerData` run
  so the compiler validates IDs and cross-file references.
- Code changes need `./gradlew build` to compile cleanly.
- Behavior is verified manually with `./gradlew run` (the dev source set's
  `AllInSlayerPluginLauncher` boots RuneLite with the plugin loaded).

## Security And Boundaries

- Never commit secrets or session files. `scripts/.jagex-env` contains a live
  Jagex session and must remain local.
- Do not add runtime network fetching for Slayer data; the plugin ships bundled
  generated data.
- Do not read, modify, or rely on generated `build/` output as source input
  except for verification.
- Do not revert unrelated worktree changes. There are known dirty files outside
  this documentation task.

## Git And Docs

- Commit messages should be concise and conventional where practical, for
  example `docs: update agent instructions` or `data: pin waterfiends source`.
- Root `AGENTS.md` should stay concise. Add detailed, project-specific agent
  references under `docs/agents/` and link them from this file.
- Avoid duplicating README or manifest information unless agents regularly get
  it wrong.

# All-In Slayer

A **read-only Slayer advisor** for [RuneLite](https://runelite.net) (Old School RuneScape). It detects your current Slayer task and shows — in a side panel and a compact in-game overlay — the task's Slayer level, weakness, where to do it, and the most efficient method, plus a **bank-aware loadout** built from gear you actually own, toggleable between **DPS** and **cost**, with one-click export to the [Inventory Setups](https://github.com/dillydill123/inventory-setups) plugin.

It is a pure **advisor**: it never plays the game for you. You do every click.

> **Status — v1 vertical slice.** This version covers **Duradel's** task set as an end-to-end proof of the design; the remaining masters are planned for v2 (see [Roadmap](#roadmap)). Two things still gate a real release and are described in [Data & accuracy](#data--accuracy): the plugin has **not yet been run in the live RuneLite client**, and several dataset fields (notably the in-game task ids) are **best-effort placeholders pending in-game verification**.

---

## Features

- **Automatic task detection.** Reads your assigned task straight from the game's Slayer varplayers (no fragile chat-log scraping) and updates the moment your task changes.
- **Task intel panel.** For the current task: required Slayer level, combat-style weakness (e.g. *weak to magic*), any required item (nose peg, facemask, rock hammer, …), the recommended location, and the recommended method (cannon / burst / safespot).
- **Bank-aware loadout advisor.** Looks at what you own across **inventory, worn equipment, and your last-seen bank** and recommends the best setup you can actually assemble — with a **DPS ⇄ Cost** toggle.
- **"Upgrades you don't own"** hints — flags the best-in-slot item for each slot when you don't have it yet.
- **Cannon-aware recommendations.** Tell it whether you own a cannon and it steers location/method accordingly (and respects the areas where cannons are banned).
- **One-click Inventory Setups export.** Copies the recommended loadout to your clipboard as an Inventory Setups import string — paste it straight into that plugin's importer.
- **Compact overlay** showing the current task + method, toggleable and unobtrusive.

---

## How it works

### Task detection
`TaskDetector` reads the Slayer varplayers via the client — `SLAYER_TARGET` (varp 395) for the assigned monster and `SLAYER_COUNT` (varp 394) for the remaining count — and resolves them against the bundled dataset. Recomputation is triggered on Slayer varp changes (395 / 394 / `SLAYER_AREA` 2096), on inventory/equipment/bank container changes, and on game-state changes. Varp ids are defined locally (`SlayerVarbits`) so a RuneLite constant rename can't break the build.

### Loadout engine (DPS vs cost)
`LoadoutAdvisor` walks each combat style the task supports and fills every equipment slot from a curated, ordered (best-in-slot → budget) option list, keeping only items you own:

- **DPS mode** picks the highest-ranked owned option per slot (closest to BIS).
- **Cost mode** picks the cheapest owned option per slot (by Grand Exchange price).

A lightweight `DpsEstimator` (equipment bonuses + a simplified max-hit/accuracy model against the monster's defence) scores each assembled style, and the **highest-DPS style is chosen**. The DPS/Cost toggle changes *which owned variant* fills each slot — style selection is always by DPS. The recommendation records the worn set, suggested inventory, location, method, an estimated DPS, the total gear cost, and any missing best-in-slot upgrades.

> The DPS estimate is intentionally a **light ordering model** — good enough to rank gear you own, not a full combat simulator. It does not model prayers, special attacks, or set effects (that's on the roadmap).

### Inventory Setups export
The **Export to Inventory Setups** button builds the Inventory Setups import JSON (`eq` array indexed by equipment slot, `inv` array, `rp`, and the task name) and copies it to the system clipboard — paste it into Inventory Setups' import. Nothing is written to disk.

### Threading & data
All game-state reads (varps, item containers, skills, item prices/stats, item names) happen on the client thread; UI updates are marshalled to the Swing EDT. Bank contents are only readable while the bank is open, so the plugin persists a **last-seen bank snapshot** and shows how stale it is ("just now", "5m ago", …). All task data is **bundled in the jar** — no task data is fetched at runtime.

---

## Configuration

| Setting | Default | Description |
| --- | --- | --- |
| **I own a cannon** (`haveCannon`) | Off | Allow cannon-based location/method recommendations where cannons are permitted. |
| **Loadout mode** (`adviceMode`) | `DPS` | Whether the recommended loadout favours DPS or lowest cost. |
| **Show task overlay** (`showOverlay`) | On | Show the compact in-game overlay for the current task. |

---

## Rules compliance

This plugin is a **passive advisor**, designed from the start to stay within both Jagex's Third-Party Client Guidelines and RuneLite's Plugin Hub rules.

**It does:** render a side panel and overlay, read game state (skills, inventory/equipment/bank, varbits, NPCs), and display **static recommendations** (gear, weakness, location, method). Item prices and stats come only from RuneLite's own `ItemManager` — there are no custom network calls — and all task data is bundled and fully reviewable.

**It deliberately does NOT:**

- generate mouse/keyboard input or auto-perform any in-game action (no automation of any kind);
- show automatic "stand here / don't stand here" tile indicators;
- show live "switch prayer now" / next-attack prediction (weakness is static info, never a real-time prompt);
- add menu entries that send server actions, or conditionally hide combat menu entries;
- expose, POST, or crowdsource player data over HTTP;
- use reflection, JNI, native code, or subprocesses;
- download or vendor executable code at runtime.

See the [design spec](docs/superpowers/specs/2026-06-28-all-in-slayer-design.md) §3 for the full compliance analysis.

---

## Build & run

**Requirements:** a JDK (built and tested on JDK 17; the plugin targets Java 11 bytecode). No system Gradle install is needed — the repo ships a self-bootstrapping **Gradle 8.10** wrapper. The first build downloads the Gradle distribution and the RuneLite client.

```sh
gradlew build      # compile + run the unit tests
gradlew test       # run the unit tests only
gradlew run        # launch RuneLite with the plugin loaded (for manual testing)
```

(On macOS/Linux use `./gradlew`.)

### Launching with a Jagex account (Linux / Bolt)

OSRS now requires a **Jagex account**, so the dev client (`gradlew run`) needs a live
Jagex session to log in — the old email/password login screen will reject you. The
launcher passes the session to RuneLite via `JX_*` environment variables; the helper
scripts in [`scripts/`](scripts/) capture those from a running client and inject them
into the dev run.

This repo's flow uses [**Bolt**](https://codeberg.org/Adamcake/Bolt) (a third-party
Jagex launcher; install via `flatpak install com.adamcake.Bolt`). Any launcher that
sets the `JX_*` variables works the same way.

**One-time:** a JDK on your `PATH` (Java 11 — `sudo pacman -S jdk11-openjdk` on Arch/CachyOS).

**Each session:**

```sh
# 1. Open Bolt, click Play on RuneLite, and reach the login/loading screen.
# 2. While that client is running, capture the session:
scripts/capture-jagex-creds.sh        # writes scripts/.jagex-env (gitignored)

# 3. Close the RuneLite that Bolt opened (one account can't be logged in twice).
# 4. Launch the dev client with the plugin loaded + your session:
scripts/dev-run.sh                    # sources .jagex-env, then runs ./gradlew run
```

You'll be logged straight in — no login screen. Bolt hands over only the game session
(not refresh tokens), so after a few hours login will start failing; just repeat steps
1–4 to grab a fresh session.

> `scripts/.jagex-env` holds your live session and is **git-ignored** — never commit or
> share it. It logs in to your account directly. Revoke leaked sessions via *End sessions*
> in your account settings on runescape.com.

---

## Project structure

```
com.danieljglover.allinslayer
├─ AllInSlayerPlugin      // @PluginDescriptor, DI, event subscriptions, recompute loop
├─ AllInSlayerConfig      // settings (cannon owned, DPS/cost default, overlay)
├─ AdviceMode             // DPS | COST
├─ data/                  // SlayerDataService — loads + indexes the bundled JSON
├─ task/                  // TaskDetector + SlayerVarbits (varp-based detection)
├─ bank/                  // OwnedItems + InventoryService (live + last-seen-bank snapshot)
├─ loadout/               // LoadoutAdvisor, DpsEstimator, PriceService, EquipmentStatsProvider
├─ integration/           // InventorySetupsExporter (clipboard import string)
└─ ui/                    // SlayerPanel + SlayerOverlay

src/main/resources/data/slayer-data.json   // 42 Duradel tasks (the bundled knowledge base)
```

**Build/tooling:** RuneLite external-plugin Gradle setup (`net.runelite:client:latest.release`), Lombok 1.18.30; tests use JUnit 4.12 + Mockito 3.12.4 (with the inline mock-maker, since some RuneLite API types are `final`). The core logic operates on plain data structures so it is unit-testable without mocking the RuneLite client — there are 10 unit-test classes covering the data loader, task detection, bank merging, DPS ordering, loadout selection, panel rendering, the exporter, and a dataset-completeness gate (plus a dev-runner main class).

---

## Data & accuracy

The v1 dataset covers **42 Duradel tasks**. Be aware of what is and isn't verified:

- **Wiki-accurate:** task names, Slayer-level requirements, quest/unlock requirements, weaknesses, assignment amounts, locations, and cannon/burst flags.
- **Best-effort placeholders (must be verified before a real release):**
  - **`slayerTargetId`** — the in-game `SLAYER_TARGET` varp value per task. These are placeholders. **Until they are confirmed in-game (`::varp 395` while on each task), live task detection will not match for most tasks.** This is the single biggest gap to functional in-game behaviour.
  - **`npcIds`**, **`monsterDefence`**, and some **gear item ids** are illustrative and need verification against live data.
- The plugin's UI/overlay glue and the `ItemManager`-backed adapters have **only been compiled and unit-tested for pure logic** — they have not yet been exercised in the live client.
- The Inventory Setups export string shape is best-effort and may need a `layout`/`rp` fallback depending on your installed Inventory Setups version.

A `DuradelDatasetValidationTest` gates structural completeness (≥40 tasks, unique names + ids, all required fields present) and explicitly guards against a known hallucinated row.

---

## Roadmap

- Remaining 8 Slayer masters + full task coverage (data fill only — no re-architecture).
- Task/master **planner** + Slayer-point optimisation (skip/block/extend strategy).
- NPC highlighting via RuneLite's `NpcOverlayService`.
- Richer DPS model (prayers, special attacks, set effects).
- Konar location-lock handling surfaced in the UI (the data model already supports it).
- Optional GP/xp-efficiency cost mode.
- Boss-task disambiguation via `SLAYER_TARGET_BOSSID`.
- An automated data-generation pipeline (seed from machine-readable sources, replacing hand-curation) — see the design spec.

---

## Publishing to the Plugin Hub

Submission is a manual step (not done yet): push this repo to a public GitHub remote, then fork [`runelite/plugin-hub`](https://github.com/runelite/plugin-hub), add a `plugins/all-in-slayer` manifest (template at [`docs/plugin-hub-manifest.txt`](docs/plugin-hub-manifest.txt)) with the release commit hash, and open a PR.

---

## Documentation

- **Design spec:** [`docs/superpowers/specs/2026-06-28-all-in-slayer-design.md`](docs/superpowers/specs/2026-06-28-all-in-slayer-design.md)
- **Implementation plan:** [`docs/superpowers/plans/2026-06-28-all-in-slayer-v1.md`](docs/superpowers/plans/2026-06-28-all-in-slayer-v1.md)
- **Plugin Hub manifest template:** [`docs/plugin-hub-manifest.txt`](docs/plugin-hub-manifest.txt)

---

## License

[BSD 2-Clause "Simplified" License](LICENSE) — Copyright (c) 2026, danieljglover. (Required by the RuneLite Plugin Hub.)

---

*This is an unofficial, fan-made plugin. Old School RuneScape and RuneScape are trademarks of Jagex Ltd. Not affiliated with or endorsed by Jagex or the RuneLite developers.*

# All-In Slayer — RuneLite Plugin Design Spec

- **Status:** Approved design (brainstorming output) — pending implementation plan
- **Date:** 2026-06-28
- **Repo:** `F:\Git\AIO-Slayer-Assistant`
- **Public plugin name:** All-In Slayer
- **Target:** Old School RuneScape, RuneLite Plugin Hub (public release)

---

## 1. Overview & positioning

All-In Slayer is a **read-only Slayer advisor** for RuneLite. It unifies — in a single panel — the capabilities currently spread across ~6 separate Plugin Hub plugins (task intel, bank-aware loadouts, location/method guidance) and presents them for the player's **current assigned task**.

The plugin **advises**; the player performs every action. There is no automation of any kind. This is the central design constraint and is what keeps the plugin within both Jagex's third-party-client rules and RuneLite's Plugin Hub rules.

**Differentiation:** No existing Hub plugin unifies task detection + bank-aware loadout (owned + BIS, DPS/cost) + location/method into one tool. Individual pieces are mature (Slayer Loadout, Slayer Codex, Slayer Helper, Slayer Simplified, etc.), so we **integrate with core systems** (NPC Indicators, NPC Aggression Timer, Wiki) rather than rebuild them, and **bundle** a unified knowledge base.

## 2. Goals & non-goals

**Goals (the player's stated purpose):**
- Understand every Slayer master, the tasks each can assign, and unlock/level requirements.
- Understand the Slayer level required per task/monster.
- Understand task locations and per-area options (e.g. cannon — a configurable option).
- Understand each task's monster weakness and most efficient kill method.
- Read the player's bank/inventory/equipment and recommend the best setup per task.
- Offer **DPS-efficient vs cost-efficient** loadouts.

**Non-goals (v1):**
- No automation, input generation, auto-walk/equip/prayer, or "do it for me" of any kind.
- No live combat prediction (next-attack / "switch prayer now"), no auto "stand here" tiles.
- No task/master *planner* or point-optimisation engine (v2+).
- No coverage beyond the v1 slice master (Duradel) — see §4.
- No crowdsourcing/exposing of player data; no runtime download of code.

## 3. Rules & compliance constraints (hard lines)

Derived from Jagex's Third Party Client Guidelines and RuneLite's "Rejected or Rolled-Back Features". These are **binding** on the design.

**Allowed (precedent exists):** overlays, side panels, NPC/object/tile highlighting, hint arrows, reading game state (skills, inventory/equipment/bank, varbits, NPCs), info boxes, notifications, and **static recommendations** (gear, weakness, location, method). Reviewers do not even verify displayed info is accurate.

**Forbidden — we deliberately do NOT do these:**
- Generating mouse/keyboard input or auto-performing any in-game action.
- **Auto** "stand here / don't stand here" indicators (user-placed markers are fine; auto-computed ones are not).
- Live next-attack / prayer-switching prediction overlays.
- Adding menu entries that send new server actions; conditional hiding of combat menu entries.
- Exposing/POSTing player info over HTTP; crowdsourcing other players' data.
- Reflection, JNI, native code, subprocesses.
- Downloading/vendoring executable code or source at runtime.

**Data fetching:** task data is **bundled** in the jar (fully reviewable), not fetched at runtime. The only runtime "external" data is GE prices and item stats, both obtained through RuneLite's own `ItemManager` (no custom network calls).

**Implications for our advice features:** weakness is shown as **static info** ("weak to magic"), never as a real-time "switch now" prompt. Safespots, if ever shown, are static data, not auto-computed positions.

## 4. v1 scope — Duradel vertical slice

v1 is a **complete vertical slice for one master: Duradel** (top master; ~47 high-value endgame tasks). It proves the entire data→detection→advice→UI pipeline. For **any task Duradel can assign**, the panel shows:

1. **Task Intel card** — Slayer level requirement, weakness, locations, recommended method (cannon / burst / safespot / single), required items (nose peg, facemask, etc.), quest/unlock gates.
2. **Loadout card (the heart)** — bank-aware best setup *the player actually owns*, with a **DPS ⇄ Cost toggle** (Cost = cheapest effective owned setup), honouring the cannon config and required items, plus a **"Create Inventory Setup"** export button.
3. **Compact in-game overlay** — current task + recommended method summary (toggleable).

The slice is architected so scaling to the other 8 masters and full ~90-task coverage is **"add data + minor UI,"** not re-architecture.

## 5. Architecture (isolated modules)

```
net.danieljglover.slayer
├─ AioSlayerPlugin            // @PluginDescriptor, Guice lifecycle, @Provides config
├─ AioSlayerConfig            // toggles (see §11)
├─ data/
│   ├─ SlayerDataService      // loads + indexes bundled JSON at startUp()
│   └─ model/                 // TaskData, LocationData, GearTier, Weakness, MonsterStats…
├─ task/
│   └─ TaskDetector           // varbit/varplayer → current TaskData (no chat parsing)
├─ bank/
│   └─ InventoryService       // tracks INV/WORN/BANK containers; persists last-seen bank
├─ loadout/
│   ├─ LoadoutAdvisor         // task tiers ⨯ owned items ⨯ DPS/cost → Recommendation
│   ├─ DpsEstimator           // light estimate from equipment bonuses + monster defence
│   └─ PriceService           // wraps ItemManager prices (cost axis)
├─ integration/
│   └─ InventorySetupsExporter
└─ ui/
    ├─ SlayerPanel            // PluginPanel: the three cards
    ├─ SlayerOverlay          // OverlayPanel: current task + method
    └─ (NavigationButton wiring)
```

**Interface discipline:** each unit has one job and a narrow contract. E.g. `LoadoutAdvisor.recommend(TaskData, OwnedItems, Mode) → Recommendation` knows nothing about Swing or varbits. `TaskDetector` emits the current `TaskData` (or "no task"); the panel/overlay subscribe. This keeps files focused and the pure logic unit-testable.

## 6. Data model & pipeline

**Bundled, not fetched.** `src/main/resources/.../slayer-data.json` (~1–1.5k lines) ships in the jar. No runtime network for task data → maximally rules-safe and fully reviewable.

**Generated, not hand-typed.** A **dev-only** `/tools` pipeline (NOT shipped in the jar) seeds the JSON:
- `jamescer/osrs-tools` → task → {masters, weight, quantity ranges, reqs, monster variants, **locations**}.
- OSRS Wiki **Bucket API** (`action=bucket`, `infobox_monster`) → `slayer_level`, `slayer_experience`, `elemental_weakness`, immunities, combat/defence stats.
- `0xNeffarion/osrsreboxed-db` → NPC IDs (to resolve in-game NPCs).
- RuneLite `Task.java` → canonical task names + variant aliases (interop with the official Slayer plugin).

**Hand-curated judgment fields only:** `cannon` (y/n per location), `burst` (y/n), `gearTiers` (BIS→budget per style), `recommendedMethod` prose.

**Validation:** every scraped row is validated against the live wiki/Bucket API before locking (research flagged a hallucinated "Frost dragons / Sailing" Duradel row — guard against LLM-fetched table errors). A schema validation test runs in CI.

**Schema (per task):**
```jsonc
{
  "task": "Nechryael",
  "slayerLevel": 80,
  "questReqs": ["Priest in Peril"],            // [] if none
  "assignedBy": ["vannaka","chaeldar","konar","nieve","duradel","krystilia"],
  "amountByMaster": { "duradel": [130,200], "duradelExt": [200,250] },
  "monsters": ["Nechryael","Greater nechryael","Nechryarch"],
  "npcIds": [...],                              // joined from osrsreboxed-db
  "weakness": { "style": "magic", "element": "air" },
  "monsterDefence": { "stab": .., "slash": .., "crush": .., "magic": .., "range": .. },
  "slayerHelmApplies": true,
  "requiredItem": null,                         // e.g. "Facemask" for smoke devils
  "locations": [
    { "name": "Catacombs of Kourend", "multi": true,  "cannon": false, "burst": true,  "konarLockable": true },
    { "name": "Slayer Tower",         "multi": false, "cannon": false, "burst": false, "konarLockable": true }
  ],
  "gearTiers": {
    "melee":  [ { "tier": "bis", "items": {"weapon": .., "head": .., ...} }, { "tier": "budget", "items": {...} } ],
    "ranged": [ ... ],
    "magic":  [ ... ]
  },
  "recommendedMethod": "Ice Barrage in Catacombs (greater nechryael)"
}
```
v1 fully populates this only for Duradel's task set; other tasks may exist as stubs (name/level) and are filled in v2+.

## 7. Task detection

On `VarbitChanged`, read (gameval IDs):
- `VarPlayerID.SLAYER_TARGET` (395), `SLAYER_COUNT` (394), `SLAYER_AREA` (2096), `SLAYER_COUNT_ORIGINAL` (4258)
- `VarbitID.SLAYER_TARGET_BOSSID` (4723) for the "Bosses" pseudo-task

Resolve to our `TaskData` by task key (aligned with RuneLite `Task.java` names). **No chat-message scraping.** We read varbits directly rather than depending on the core Slayer plugin's internals, so a core refactor can't break us. "No current task" is a valid state the UI handles.

## 8. Bank / stat awareness

`InventoryService` listens to `ItemContainerChanged` for `INV(93)/WORN(94)/BANK(95)` and `WidgetLoaded` for the bank interface. **Bank contents are only readable while the bank is open**, so we persist a **last-seen bank snapshot** to the RS-profile config and mark loadout recommendations as "based on bank last seen <when>" when the live bank is unavailable. Player stats via `client.getRealSkillLevel(Skill.SLAYER)` etc.

`OwnedItems` = union of WORN + INV + last-seen BANK (de-duplicated, counts summed).

## 9. Loadout engine (hybrid)

1. Look up the task's **curated gear tiers** for each combat style (BIS → budget).
2. Filter tiers to **items the player owns** (`OwnedItems`).
3. **DPS mode:** rank owned options with `DpsEstimator` — a **light** estimate using equipment bonuses (`ItemManager.getItemStats`) + a simplified max-hit/accuracy formula against the task's stored monster defence. Accounts for on-task Slayer helm/black mask bonus. **Not** a live combat predictor — a static ranking only (rules-safe).
4. **Cost mode:** among owned gear meeting a per-slot effectiveness threshold, pick the **lowest total GE value** (`ItemManager.getItemPrice`). Definition of "cost-efficient" = cheapest effective owned setup.
5. Apply constraints: honour the **cannon** config (e.g. suppress cannon recommendation where banned — Slayer Tower, Catacombs, Fremennik dungeon, Kraken Cove, etc.), require special items (nose peg, facemask, mirror shield…), and prefer burst/cannon locations when the player is equipped for them.
6. Output a `Recommendation`: chosen style, worn items, inventory items, location, method, and any missing-but-recommended upgrades (shown as "you don't own — consider").

`DpsEstimator` deliberately omits prayers, special attacks, and proc effects in v1 — enough to **order** owned options, not to publish exact DPS numbers. Scope can grow later.

## 10. Inventory Setups export

A **"Create Inventory Setup"** button builds a setup from the recommended worn + inventory items. Implementation: verify the Inventory Setups plugin exposes a usable import path (config-group JSON import, or its public import-string format); if neither is cleanly available, fall back to copying its import string to clipboard with a one-line instruction. The player already runs Inventory Setups locally, so this is a natural bank-loading bridge. This is a convenience integration, not a hard dependency — the panel works fully without it.

## 11. UI & config

**UI surfaces:**
- **Side panel** (`PluginPanel` + `NavigationButton`) with the three cards (Task Intel, Loadout, Location/Method). Item icons via `ItemManager`.
- **Compact overlay** (`OverlayPanel`) for current task + method; toggleable, repositionable.
- NPC highlighting is **out of scope for v1** (defer to core NPC Indicators / Better NPC Highlight); may integrate via `NpcOverlayService` in v2.

**Config (`AioSlayerConfig`):**
- `haveCannon` (boolean) — influences location/method/loadout recommendations.
- `loadoutMode` (DPS | COST) — default for the toggle.
- `showOverlay` (boolean), overlay content options.
- `preferBurstWhenAble` (boolean).
- `panelAutoSwitch` (boolean) — auto-focus panel to the current task on assignment.
- (room to grow: per-style preference, budget ceiling.)

## 12. Testing

- **Pure-logic unit tests** (the bulk of value):
  - `SlayerDataService` loads and schema-validates the bundled JSON; rejects malformed rows.
  - `LoadoutAdvisor` selects correct setups from **synthetic** `OwnedItems` across DPS/cost modes, cannon on/off, and missing-special-item cases.
  - `DpsEstimator` produces expected **orderings** for known gear pairs.
  - `TaskDetector` maps representative varbit states → correct `TaskData` (including the boss pseudo-task path).
- Varbit/Swing glue is kept thin; manual in-client verification for the UI cards and overlay.
- A **data-validation test** cross-checks the bundled JSON against an expected Duradel task list.

## 13. Build, licensing, Hub submission

- Standard RuneLite **external-plugin Gradle** setup pinning `runeLiteVersion`.
- **BSD 2-Clause "Simplified" LICENSE** (Hub requirement).
- Main class extends `Plugin` with `@PluginDescriptor`; config interface extends `Config`; Guice `@Inject` / `@Provides`; register overlays/panel in `startUp`, unregister in `shutDown`.
- Target `net.runelite.api.gameval.*` IDs (the modern, non-deprecated constants).
- Submission: own GitHub repo + LICENSE → fork `runelite/plugin-hub`, add a `plugins/all-in-slayer` manifest (`repository`, `commit`), open PR; pass CI (reflection/native/dependency hash scans).

## 14. Roadmap (v2+)

- Remaining 8 masters + full ~90-task coverage (data fill).
- Task/master **planner** + Slayer-point optimisation (skip/block/extend strategy).
- NPC highlighting integration (`NpcOverlayService`).
- Richer `DpsEstimator` (prayers, specs, set effects) — optional.
- Konar location-lock handling surfaced in UI (data already supports it).
- Optional GP/xp-efficiency cost mode.

## 15. Open questions / risks to verify during implementation

- **Inventory Setups import path** — confirm the cleanest integration (API vs config JSON vs import string).
- **Superior-creature varbit** — `Varbits.SUPERIOR_ENABLED` (5362) has no exact `gameval` twin; verify the correct gameval id (or use legacy constant).
- **Bank snapshot staleness UX** — confirm how aggressively to warn when bank data is old.
- **`Task.java` key alignment** — confirm our task keys round-trip with the official plugin's enum names.
- **Data accuracy pass** — validate the full Duradel dataset against the live wiki before release.

## 16. References

- RuneLite Developer Guide — https://github.com/runelite/runelite/wiki/Developer-Guide
- Plugin Hub (submission, manifest, license, dependency hashing) — https://github.com/runelite/plugin-hub
- Plugin Hub Review — https://github.com/runelite/runelite/wiki/Plugin-Hub-Review
- Rejected or Rolled-Back Features — https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features
- Jagex Third Party Client Guidelines — https://oldschool.runescape.wiki/w/Update:Third_Party_Client_Guidelines
- RuneLite API Javadocs — https://static.runelite.net/runelite-api/apidocs/
- Official Slayer plugin source — https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer
- OSRS Wiki: Slayer master — https://oldschool.runescape.wiki/w/Slayer_master · Slayer task — https://oldschool.runescape.wiki/w/Slayer_task · Slayer Rewards — https://oldschool.runescape.wiki/w/Slayer_Rewards · Dwarf multicannon (restrictions) — https://oldschool.runescape.wiki/w/Dwarf_multicannon
- Data seeds — github.com/jamescer/osrs-tools · github.com/0xNeffarion/osrsreboxed-db · OSRS Wiki Bucket API (`action=bucket`)

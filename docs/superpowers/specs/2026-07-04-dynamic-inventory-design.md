# Dynamic, Task & Strategy Aware Inventory System — Design

Date: 2026-07-04
Status: Approved (user interview + plan approval on 2026-07-04)

## Problem

The plugin's inventory recommendations are static heuristics. `InventorySelector` adds hard-coded supplies (required item, cannon parts, antifire, slayer bracelet) and `TripInventoryPlanner` packs a fixed 28-slot convention (potion = 2 slots, combo food = 4, food fills the rest) regardless of task length, monster danger, or strategy.

Meanwhile the source data graph already contains rich per-method strategy data — `strategies/*/strategy.json` `methods[]` with `requiredOrKeyItems[]` (522/584 methods), `prayers[]` (85/151 strategies), `equipment{slots}` (92/151), and `inventory[]` (29/151) — but `ModularSlayerDataCompiler.resolveStrategy()` drops all of it; only `plugin{primaryStyle, weapons, note}` reaches runtime.

## Goal

Wire the authored strategy data through the compiler to runtime and build a layered, owned-only trip planner that:

- sizes the bag to the remaining task count (`SLAYER_COUNT` varp) and the method's sustain profile (prayer-potion-primary under protection prayers, food-primary otherwise),
- auto-picks the best feasible strategy method with a remembered manual override,
- (Phase 2) diffs the recommendation against the live inventory/equipment with a bank-open restock checklist,
- (Phase 3) includes structured travel items per location.

## Design decisions (user-approved, locked)

1. **Hybrid engine** — strategy `methods[]` data is the skeleton; the existing owned-item engine personalizes (best owned tier, quantities, 28-slot packing).
2. **Compile-time item catalog** — new `items/` source directory mapping stable names → item IDs (dose/variant lists, best-first), extending the `weapons/` pattern. The compiler resolves every strategy item name; malformed catalog entries fail the build, unresolved free-text names warn (the warning report is the authoring worklist).
3. **Sustain-profile-aware sizing** — kills-per-trip from `DpsEstimator` (time-to-kill) + `MonsterOffence` damage intake → food per kill → pack for remaining kills, capped at 28 slots. When the selected method's `prayers[]` rely on a protection prayer covering the monster's attack styles (e.g. K'ril, Tormented demons), prayer potions become the primary sustain resource sized to trip duration and prayer drain; food demotes to an emergency reserve. Fixed conventions remain the fallback whenever model inputs are missing.
4. **Missing items** — the loadout stays owned-only with best-owned substitution, plus a "missing key items" advisory note listing strategy items the player lacks.
5. **Method auto-pick + manual override** — filter feasible methods (cannon owned + location cannonable, barrage runes owned, requirements met), rank by `adviceMode` (DPS/Cost); a method selector allows override, remembered per task via config.
6. **Live diff + bank checklist** (Phase 2) — loadout rows show carried/missing/partial (e.g. 4/18 sharks); an overlay while the bank is open lists outstanding withdrawals. Strictly read-only.
7. **Layered fallback for sparse data** — `requiredOrKeyItems` + prayer-derived sustain + engine supplies always work; authored `inventory[]` refines where present. Backfilling the 122 strategies without `inventory[]` is a separate later data task.
8. **UI** — diff states in the existing step-4 grids + a bank-open overlay; no new persistent overlay.
9. **Travel items in scope** (Phase 3) — structured `travel` field added to the 288 location JSONs; teleport items join the catalog and the bag.
10. **Three phases, engine first** — each phase ships usable on its own.
11. **Code shape** — a new layered `TripPlanner` composes the bag (required/key → travel → method inventory → sustain sizing → runes → fill); `InventorySelector` knowledge becomes one layer, `TripInventoryPlanner` conventions become the fallback sizing; the old classes retire at parity.

## Architecture

### Data pipeline (compile time)

- `src/main/data/slayer/items/*.json` — one file per stable item: `{itemKey, name, aliases[], itemIds[] (best-first), stackable, dosed}`. Seeded with only what the engine consumes (~30–50 names from `requiredOrKeyItems`, sustain potions, supply bags, bracelets, antipoison/antivenom, stamina).
- `ItemNameResolver` (compiler) — normalized index over `items/` names+aliases and `weapons/` names. Per authored string: split `" or "` into alternatives, strip trailing qualifier clauses (`" for "`, `" if "`, `" when "`, comma), parse `xN` quantities, exact-then-longest-prefix match. Unresolved → a name-only ref with empty `itemIds` (honest advisory, never a fabricated ID).
- `resolveStrategy()` gains `resolveMethods(...)`: every `SourceStrategyMethod` maps to a runtime `StrategyMethod`, including `role=general` methods (their key items are strategy-wide; the engine filters pickability by non-null `combatStyle`).

### Runtime model

- `model/StrategyItemRef` — `{name, itemIds[], alternatives[], quantity}`; empty `itemIds` means advisory-only.
- `model/StrategyMethod` — `{methodId, label, combatStyle, role, keyItems[], prayers[] (verbatim), equipment: Map<EquipmentSlot, List<StrategyItemRef>>, inventory[], risks[], summary}`.
- `model/MonsterStrategy` — gains `List<StrategyMethod> methods`; Gson-null preserves today's behaviour for `.md`-only strategies, which is load-bearing (e.g. `tormented-demon.md` has no `methods[]`).

### Engine

- `loadout/MethodPicker` — feasibility filter (cannon: `ownsCannon` + location cannon-effective; burst/barrage: runes owned via `RuneTable` + location burst/multi; wilderness role deprioritized off-wilderness), then ranks by per-style DPS (cached per style) or gear cost. The user's explicit style toggle still wins; the picker re-picks within that style.
- `loadout/SustainModel` — pure static. TTK = monster HP / estimated DPS. Profile = `PRAYER_PRIMARY` when the method's prayers include a `Protect from X` covering `MonsterOffence.attackStyles` (any protect counts when attack styles are absent), else `FOOD_PRIMARY`.
  - Food-primary: damage/kill = `(maxHit/3.0) * (ttkSeconds / (attackSpeedTicks*0.6))`; food count = intake/heal, for `min(remaining, killsThatFit)`.
  - Prayer-primary: overhead drain 1 pt/3 s at 0 prayer bonus (conservative — gear prayer bonus is not available from `Bonuses` yet); restore/dose = `floor(prayerLevel/4)+7`; potions = `ceil(needed/restore/4)`, capped ~12 slots; food = fixed emergency reserve (4 slots) + fill. Trip duration capped (~2 h); `remaining == 0`/stale varp clamps to a ~50-kill default.
  - Any missing input → "no model" → today's fixed conventions.
  - Requires `PlayerStats` to gain `hitpoints` and `prayer` (Phase 1 addition).
- `loadout/TripPlanner` + `TripLayer` + `TripPlanContext` — `interface TripLayer { void contribute(TripPlanContext ctx, TripBag bag); }`; `TripBag` = ordered slots capped at 28 + accumulated `missingKeyItems` + notes; `TripSlot` reused unchanged. Layer order: RequiredKeyItemsLayer (absorbs `InventorySelector` knowledge) → TravelLayer (stub until Phase 3) → MethodInventoryLayer → SustainLayer → RuneLayer → FillLayer.
- `LoadoutAdvisor` stays an orchestrator: an 11-arg `recommend(...)` overload adds `selectedMethodId`; method `equipment` refs feed `GearSelector` as per-slot priority IDs (extending the `strategyWeaponIds` seam, ADR-0015 contract); `TripPlanner.plan(ctx)` replaces `TripInventoryPlanner.plan(...)`.
- `Recommendation` gains nullable `methodId, methodLabel, prayers[], sustainNote, missingKeyItemsNote`.

### UI

- `StrategyMethodSelector` (JComboBox of method labels, shown when ≥2 style-matching methods) beside the existing style segments; loadout card gains a Prayers row + sustain/missing-items note lines (existing `antifireNote` pattern).
- Per-task method memory via unregistered config keys `methodChoice.<task>` (the `bankSnapshot` pattern).
- Phase 2: `InventoryGrid`/`EquipmentGrid` accept an optional status map (border tint + "4/18" badges) from a pure `LoadoutDiff` over `InventoryService.liveCarried()` (INV+EQUIPMENT, no bank snapshot); `BankChecklistOverlay` renders outstanding withdrawals only while the bank is open; `showBankChecklist` config (default true).
- Phase 3: `SlayerLocation.travelItems` from a structured `travel` block (validation fails on unresolved `itemKey` — structured refs, unlike free text); TravelLayer packs the first owned ID per travel item.

## Error handling

- Unresolved free-text item names degrade to name-only advisories; never invent item IDs.
- Missing model inputs (DPS, max hit, attack speed, HP) fall back to the current fixed packing conventions — `.md`-strategy and sparse-data tasks behave exactly as today.
- Empty bank snapshot / no owned data → no crash, fallback conventions.

## Verification (manual only — owner policy: no automated tests)

Phase 1: clean `./gradlew build` with the unresolved-name warning report; regression parity on a `.md`-strategy task and a no-strategy task; Abyssal demons (method combo, auto-pick, Protect from Melee row, prayer-primary sizing, missing Kodai wand note); Dust devils (authored inventory refs, "or" alternatives resolve to owned); style toggle re-pick; method memory across task switches; empty-bank fallback.

Phase 2: bank checklist shows the exact shortfall, updates live on withdraw/deposit; grid badges update; no overlay when the bank is closed or no task; strictly read-only.

Phase 3: compiler fails on bad `itemKey`; owned teleport packs in required→travel order; location switch re-plans; untouched locations unchanged.

## Risks

- Free-text resolution quality is the top risk; expect iteration on qualifier heuristics, driven by the compiler warning report.
- Prayer bonus unavailable → modest over-pack of prayer potions; future fix is extending `DefaultEquipmentStatsProvider`.
- Huge tasks can exceed one bag: the 28-slot cap plus a "multiple trips" clause in `sustainNote`.
- Stale comments referencing nonexistent tests get updated when touched; never create test files.

## Delivery

- **Phase 1** — compiler wiring, item catalog, layered engine with sustain sizing and method auto-pick, surfaced in the existing step-4 UI.
- **Phase 2** — live diff + bank checklist overlay.
- **Phase 3** — travel data authoring (288 locations, incremental, prioritized by task frequency) + travel slot.

Full implementation detail (files, line anchors, step order) lives in the approved plan: `/home/danny/.claude/plans/the-runelite-plugin-is-parallel-alpaca.md`.

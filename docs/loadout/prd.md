# All-In Slayer - Loadout Engine Redesign: PRD (lite)

- Author: Architect / Principal Engineer (board LP1)
- Date: 2026-06-29
- Track: T2 feature redesign of the loadout subsystem
- Status: Gate 1 draft for review. Design + build plan in `docs/loadout/plan.md`; ADRs in `docs/loadout/adr/`.
- Sources of truth: `docs/loadout/research-bank-stats-and-domain.md` (RuneLite API + OSRS domain),
  `docs/loadout/current-subsystem-map.md` (what exists today).

## 1. Problem

The current loadout advisor recommends gear from **hand-curated, per-task BIS-to-budget item lists**
baked into `slayer-data.json` (`StyleLoadout.slotOptions`). This has three problems:

1. **It does not reflect what the player actually owns or how good it is.** It picks the first owned
   item from a fixed list; an excellent item the curator did not list is invisible, and the list goes
   stale every game update.
2. **It guesses without a full owned-item picture.** The plugin reads live inventory + worn + a
   last-seen bank snapshot, but it will still emit advice with no bank ever scanned, so the advice is
   built from a fraction of the player's gear and is often wrong.
3. **It splits attention between "your loadout" and "upgrades you do not own."** The user wants 100%
   focus on the best loadout they can field right now.

Players want: "Open my bank once, then tell me the best gear, food, potions, and runes I **own** for
this exact task's weakness." Today the plugin cannot do that.

## 2. Users

- **Primary:** an OSRS player doing Slayer in RuneLite who wants a correct, owned-only loadout for the
  current task without manually cross-referencing the wiki and their bank.
- The plugin is single-player, local, read-only against game state. No accounts, no server, no PII.

## 3. Goals / non-goals

**Goals**
- Derive the loadout from the **actual equipment stats of owned items**, targeted at the **task's
  weakness**, covering **gear + food + potions + runes**, and only **after the bank has been seen**.

**Non-goals (explicit, binding for v1)**
- NG-1 No automation: the plugin never equips, withdraws, drinks, or casts anything. It advises only.
- NG-2 No "recommended upgrades" / shopping advice. The upgrades concept is removed entirely (FR-7).
- NG-3 No in-combat or per-phase gear switching; one loadout per task.
- NG-4 Rune-pouch contents are not read in v1 (rune counts come from inventory + worn + last-seen
  bank). Deferred to a follow-up.
- NG-5 Weapon/ammunition compatibility (arrow vs bolt vs dart vs bow/crossbow) is not validated in v1;
  the highest ranged-strength owned ammo is suggested. Flagged limitation.
- NG-6 Set-effect items (Void, Crystal armour/bow, Dharok's, etc.) are scored on their flat equipment
  stats only; their conditional set bonuses are not modelled in v1.
- NG-7 No change to task detection, the overlay, the Inventory Setups export, or the side-panel
  redesign render contract (that work is tracked separately under `docs/plan.md`).

## 4. Functional requirements

Each FR has a verbatim, testable acceptance criterion (AC) used by the plan's traceability table.

| ID | Requirement | Acceptance criterion (testable) |
|---|---|---|
| FR-1 | **Bank-gate.** The engine emits no loadout until a bank snapshot exists. | When `InventoryService.bankLastSeen()` is `null`, no `Recommendation` is produced; if a task is detected the panel status is `BANK_NOT_SCANNED` and the loadout card shows the "open your bank" prompt; task and Where/How sections still render. Once `bankLastSeen() != null`, a loadout is produced. |
| FR-2 | **Stat-driven gear selection over owned items.** Per slot, pick the best **owned** equipable item by a weakness-relevant score derived from `ItemManager.getItemStats(id)`, not from any static list. | Given a fake stats provider and two owned items mapping to the same slot, the selector picks the one with the higher style score; an unowned item with a better score is never picked; an owned item with no positive relevant bonus is not used to fill the slot. |
| FR-3 | **Weakness-targeted style + attack type.** Style = `task.weakness.style`. For melee, the attack type is the one the monster has the **lowest** defence against (`min` of `monsterDefence.{stab,slash,crush}`). For magic, the element is `task.weakness.element`. | Unit tests: a MELEE task with `crush` as the lowest monster defence selects gear by crush attack bonus; a MAGIC task with `element="fire"` drives a fire spell (FR-6); a RANGED weakness selects by ranged attack + ranged strength. |
| FR-4 | **Food selection from owned items.** Recommend the highest-healing owned food; note Cooked karambwan as a combo-eat when owned. | With a fake consumable-effects provider returning heal amounts, the highest-heal owned food id is chosen; when karambwan is owned it is surfaced as the combo food; when no food is owned, none is recommended (not an error). |
| FR-5 | **Potion selection from owned items.** Recommend the best owned combat potion for the task's style. | With a fake provider, an owned potion whose positive boosts match the style (melee->attack/strength, ranged->ranged, magic->magic) is chosen; the higher-magnitude boost wins between two matching potions; dose variants of one potion are treated as the same potion. |
| FR-6 | **Rune / spell selection for magic tasks.** Recommend the highest spell tier (Surge>Wave>Blast>Bolt) for the task's element that the player can cast given Magic level and owned rune counts; show each required rune with an owned/short marker. A powered staff (curated set) means "no runes required". | Unit tests over the curated element->tier->rune table: with enough air+fire runes and Magic 95, a fire task recommends Fire Surge; insufficient runes drop to the highest affordable tier; an owned elemental/combination staff removes that element's rune from the requirement; an owned powered staff yields an empty rune requirement. |
| FR-7 | **Remove recommended upgrades.** The "upgrades you do not own" concept is removed from the model, engine, and UI. | `Recommendation` has no `missingUpgrades`; `LoadoutAdvisor` never builds an upgrade list; the panel renders no upgrades section; the guard test asserts no upgrades row is produced for any state. |
| FR-8 | **DPS estimate retained for display.** The chosen loadout shows an estimated DPS for the fixed weakness style. | The panel renders `Est. DPS` for a produced loadout; the estimator is fed by the same `ItemManager`-backed stats as selection. |
| FR-9 | **Cost mode retained (gear only).** In COST mode the per-slot pick is the cheapest owned item that still has a positive weakness-relevant bonus. | With a fake price service, COST mode selects the cheapest positive-score owned item in a slot; DPS mode selects the highest-score item. Consumables are unaffected by mode. |
| FR-10 | **Render gear + food + potions + runes.** The loadout card shows all four parts. | For a produced loadout, the panel shows worn gear rows, a food row, a potion row, and (for magic) the spell + rune rows; absent parts are omitted cleanly. |
| FR-11 | **No client reads on the EDT.** All `ItemManager` / `ItemStatChangesService` / `Client` reads happen on the client thread; the panel renders from the immutable `SlayerPanelState`. | The plugin resolves stats, effects, names, and prices in `recompute` on the client thread and marshals to the EDT; no UI component calls a client service (existing seam contract preserved). |

## 5. Non-functional requirements (numbers)

- **NFR-1 Selection latency.** A full gear+consumables selection completes in **< 10 ms** for an owned
  set of **2000 distinct item ids** once the per-id stat cache is warm (client thread).
- **NFR-2 Stat cache.** Each item id's equipment stats are fetched from `ItemManager` **at most once
  per session** (stats are immutable per id); repeat scans are map lookups.
- **NFR-3 Thread safety.** **0** `ItemManager`/`ItemStatChangesService`/`Client` calls on the EDT
  (FR-11). All such reads on the client thread.
- **NFR-4 Render stability.** An unchanged recommendation re-renders with **0** loadout-section
  rebuilds (preserves the existing self-diff NFR; `Recommendation` + the new consumables model are
  value types with `equals`).
- **NFR-5 Async-null safety.** When `getItemStats(id)` returns `null` (stats not loaded yet, or
  non-equipable) the item is skipped, never cached as a permanent "no stats"; a later recompute
  recovers it.

## 6. Curated-data needs

The stat API supplies item bonuses and consumable effects; the following must be **curated in-repo**
(small, versioned tables) because the live client does not expose them. The plan owns where each lives.

| Concern | Source |
|---|---|
| Item offensive/defensive bonuses, slot, 2h, attack speed | **API** - `ItemManager.getItemStats(id).getEquipment()` |
| Food heal amount (level-aware), potion stat boosts/drains | **API** - RuneLite `ItemStatChangesService` (behind our seam) |
| Potion dose-variant grouping | **API** - `ItemVariationMapping` |
| Per-task weakness **style + (melee) implied by monster defences + (magic) element** | **CURATED** - `slayer-data.json` (`weakness`, `monsterDefence`); magic tasks' `element` must be audited/completed |
| Element -> spell tier -> rune requirement + Magic level + base max hit (Bolt/Blast/Wave/Surge) | **CURATED** - in-repo table (research C.4) |
| Powered-staff ids (weapon supplies its own attack; no runes) | **CURATED** - small id set |
| Elemental / combination staff ids (supply one element's runes unlimited) | **CURATED** - small id->element map |
| Cooked karambwan id (combo-eat) | **CURATED** - constant |

## 7. Risks

- **R1 (high) - `ItemStatChangesService` may not be injectable** from our plugin (it is a core-plugin
  service). Mitigation: Phase 0 spike (LD00) verifies injection in a running client; fallback is a
  small curated `id->heal` / `id->boostedStats` map for common foods/potions. Decision gate before
  Phase 1 builds consumables.
- **R2 (med) - `getItemStats` returns null at startup** until the async stat map loads (research B.1).
  Mitigation: NFR-5 (skip + recompute); optional probe (worn items all-null => stats not ready).
- **R3 (med) - stale persisted bank** over-reports ownership (gear) and rune counts. Mitigation: gate
  on existence, surface bank age prominently; rune sufficiency is best-effort and labelled.
- **R4 (med) - data migration** (removing `loadouts`/`StyleLoadout` from `TaskData` +
  `slayer-data.json`) touches the dataset validation tests. Mitigation: sequenced single-task edit
  (LD14) with the validation test updated in the same task.
- **R5 (low) - set-effect / coupled items** (Void, Crystal, bolt-vs-bow) mis-scored. Accepted for v1
  (NG-5, NG-6); flagged for follow-up.

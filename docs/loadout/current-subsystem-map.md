# Current Loadout Subsystem Architecture

**Date:** 2026-06-29  
**Scope:** Complete trace of the loadout recommendation engine for redesign into a bank-gated, stat-driven, weakness-specific system.

---

## 1. LoadoutAdvisor: Recommendation Engine

**File:** `src/main/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisor.java`

**Core Method:**
- `recommend(TaskData task, OwnedItems owned, PlayerStats stats, AdviceMode mode, boolean haveCannon)` → `Optional<Recommendation>` (line 32–36)
- Full signature with location override: line 38–115
- **Contract:**
  1. Validates task has loadouts (line 41–44)
  2. Iterates all `CombatStyle` entries in `task.getLoadouts()` (line 52–82)
  3. For each style, calls `pickItem(options, owned, mode)` to fill each equipment slot (line 62)
  4. Records missing BIS items if owned doesn't have first option (line 68–70)
  5. Calls `dpsEstimator.estimate(style, worn, stats, task)` to score the loadout (line 73)
  6. Picks **best DPS style** (line 74–82)
  7. Builds final `Recommendation` (line 103–114)

**Item Selection Logic:**
- `pickItem(List<Integer> options, OwnedItems owned, AdviceMode mode)` (line 134–159)
  - Filters options to only owned items (line 136–143)
  - **DPS mode:** picks first owned (closest to BIS) (line 150)
  - **COST mode:** picks cheapest owned by GE price (line 152–158)

**Recommendation Output Fields** (src/main/java/com/danieljglover/allinslayer/loadout/Recommendation.java, line 10–23):
- `style: CombatStyle` — chosen combat style
- `worn: Map<EquipmentSlot, Integer>` — equipped items (can be partial; null slots omitted)
- `inventory: List<Integer>` — suggested inventory items (only owned; line 117–132)
- `location: SlayerLocation` — selected or recommended location
- `recommendedLocation: SlayerLocation` — advisor's top choice (separate from override)
- `locationReason: String` — human readable justification (line 208–243)
- `method: String` — task.recommendedMethod (line 110)
- `estimatedDps: double` — style's DPS score (line 111)
- `totalGearCost: long` — sum of GE prices for worn items (line 98–101, 112)
- `missingUpgrades: List<Integer>` — BIS items not owned + task's required item if not owned (line 69, 89–92, 113)

---

## 2. OwnedItems: The Owned-Set Model

**File:** `src/main/java/com/danieljglover/allinslayer/bank/OwnedItems.java`

**Structure:**
- Immutable view of `Map<Integer, Integer>` (itemId → count)
- Built from **inventory + worn equipment + last-seen bank snapshot** (line 9)

**Public API:**
- `has(int itemId): boolean` — checks count > 0 (line 19–22)
- `quantity(int itemId): int` — returns count or 0 (line 24–27)
- `ids(): Set<Integer>` — all owned item IDs (line 29–32)
- `fromCounts(Map<Integer, Integer> counts)` — factory (line 34–37)
- `fromContainers(Item[] inventory, Item[] worn, Item[] bank)` — static helper (line 39–46)

**How It Is Built:**
→ See **InventoryService** below.

---

## 3. InventoryService: Bank + Inventory Snapshot

**File:** `src/main/java/com/danieljglover/allinslayer/bank/InventoryService.java`

**Core Responsibility:**
- Persists bank snapshot to RS profile config when bank changes
- Merges live inventory + worn + saved bank into a single `OwnedItems` on-demand

**Bank Snapshot Mechanism:**
- **Storage keys:** `GROUP = "allinslayer"`, `BANK_SNAPSHOT_KEY = "bankSnapshot"` (line 22–23), `BANK_TS_KEY = "bankSnapshotTs"` (line 24)
- **Event hook:** `onBankChanged(ItemContainer bank)` (line 39–55)
  - Called by AllInSlayerPlugin's `onItemContainerChanged` (AllInSlayerPlugin.java:165)
  - Serializes bank items to JSON map: `{itemId: quantity}` (line 45–52)
  - Stores via `configManager.setRSProfileConfiguration(...)` (line 53–54)
  - Also timestamps the snapshot with `System.currentTimeMillis()` (line 54)

**Owned Items Resolution:**
- `currentOwned(): OwnedItems` (line 58–88)
  1. Starts with live `InventoryID.INV` and `InventoryID.WORN` containers (line 61–62)
  2. Loads persisted bank snapshot from config (line 64–86)
  3. Merges all three via `addContainer(counts, container)` (line 109–122)
  4. Returns `OwnedItems.fromCounts(counts)` (line 87)

**Bank-Age Tracking:**
- `bankLastSeen(): Long` (line 91–107)
  - Reads persisted timestamp from config (line 93)
  - Returns epoch millis or null if never seen (line 91, 100)
  - Used by AllInSlayerPlugin to display "bank seen X ago" (AllInSlayerPlugin.java:321–338)

---

## 4. DpsEstimator & PriceService

**Files:**
- Interface: `src/main/java/com/danieljglover/allinslayer/loadout/DpsEstimator.java` (line 12)
- Implementation: `src/main/java/com/danieljglover/allinslayer/loadout/DefaultDpsEstimator.java` (line 23–96)
- Interface: `src/main/java/com/danieljglover/allinslayer/loadout/PriceService.java` (line 8)
- Implementation: `src/main/java/com/danieljglover/allinslayer/loadout/DefaultPriceService.java` (line 18–22)

**DpsEstimator Contract:**
- `estimate(CombatStyle style, Map<EquipmentSlot, Integer> equipped, PlayerStats stats, TaskData task): double`
- **Inputs:**
  - `style`: which combat style to evaluate
  - `equipped`: worn items (equipment slot → item ID)
  - `stats`: player's live combat stats (line 310–318 in AllInSlayerPlugin builds these)
  - `task`: monster defence, slayer helm bonus, magic spell max hits
- **Outputs:** DPS double (score used to rank styles)
- **Algorithm** (DefaultDpsEstimator):
  1. Extracts attack bonuses from all worn items via `EquipmentStatsProvider.get(itemId)` (line 30–46)
  2. Applies slayer helm bonus (7/6) if applicable (line 49)
  3. Calculates style-specific hit chance and max hit (line 54–95)
  4. Converts to DPS via `dps(hitChance, maxHit, weaponSpeed)` (line 69, 77, 91, 103–106)
  5. **NOTE:** Does NOT use `ItemManager.getItemStats()` — uses a custom `EquipmentStatsProvider` (line 15)

**PriceService Contract:**
- `price(int itemId): int` → GE price
- **Implementation:** delegates to `ItemManager.getItemPrice(itemId)` (DefaultPriceService.java:21)
- **Usage:** LoadoutAdvisor uses prices to pick items in COST mode (line 154–156 in LoadoutAdvisor), and to sum gear cost (line 98–101)

---

## 5. PlayerStats & EquipmentStatsProvider

**File:** `src/main/java/com/danieljglover/allinslayer/loadout/PlayerStats.java`
- Simple data class: `attack, strength, defence, ranged, magic, slayer` (read-only)
- Built in AllInSlayerPlugin.buildStats() (line 310–318) from live Skill levels

**File:** `src/main/java/com/danieljglover/allinslayer/loadout/EquipmentStatsProvider.java` & `DefaultEquipmentStatsProvider`
- Interface: `get(int itemId): Bonuses` (line 8)
- Returns item combat bonuses (attack, defence, strength, ranged, magic, speed)
- Used by DefaultDpsEstimator to score gear (line 30)
- **⚠️ CRITICAL:** Currently the subsystem does NOT call `ItemManager.getItemStats()` — stats come from `DefaultEquipmentStatsProvider` (implementation not shown but injected as singleton)

---

## 6. Data Models

### TaskData
**File:** `src/main/java/com/danieljglover/allinslayer/model/TaskData.java` (line 10–28)
- `task: String` — task name
- `slayerTargetId: int` — SLAYER_TARGET varp value (0 if unmapped)
- `slayerLevel: int` — required level
- `questReqs: List<String>` — quest requirement names
- `monsters: List<String>`, `npcIds: List<Integer>` — NPC references
- **`weakness: Weakness`** (line 20) — see below
- `monsterDefence: MonsterDefence` — defence level and per-style defences (used by DpsEstimator)
- `slayerHelmApplies: boolean` — slayer helm applies 7/6 bonus
- `requiredItemId: Integer`, `requiredItemName: String` — must-have item (e.g. Facemask)
- `locations: List<SlayerLocation>` — multi/cannon/burst tags
- **`loadouts: Map<CombatStyle, StyleLoadout>`** (line 26) — **core recommendation data**
- `recommendedMethod: String` — human text describing suggested approach

### Weakness
**File:** `src/main/java/com/danieljglover/allinslayer/model/Weakness.java` (line 10–14)
- `style: CombatStyle` — MELEE / RANGED / MAGIC
- `element: String` — null or "air", "fire", etc. (informational; not used in DPS calc)
- **Source data:** slayer-data.json, e.g. `"weakness": { "style": "MELEE", "element": null }` (line 11 in data file)

### StyleLoadout
**File:** `src/main/java/com/danieljglover/allinslayer/model/StyleLoadout.java` (line 10–16)
- `style: CombatStyle` — redundant tag
- **`slotOptions: Map<EquipmentSlot, List<Integer>>`** (line 13) — **key data for selection**
  - Each slot has a BIS→budget ordered list
  - LoadoutAdvisor.pickItem() selects from this list (line 62, 134–159)
- `inventory: List<Integer>` — suggested inventory items
- `spellMaxHit: Integer` — base spell damage for magic (used by DpsEstimator line 82 in DefaultDpsEstimator)

### CombatStyle
**File:** `src/main/java/com/danieljglover/allinslayer/model/CombatStyle.java` (line 3–8)
- Enum: MELEE, RANGED, MAGIC

### EquipmentSlot
**File:** `src/main/java/com/danieljglover/allinslayer/model/EquipmentSlot.java` (line 3–6)
- Enum: HEAD, CAPE, AMULET, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING

### SlayerLocation
**File:** `src/main/java/com/danieljglover/allinslayer/model/SlayerLocation.java`
- `name: String`, `multi: boolean`, `cannon: boolean`, `burst: boolean`, `konarLockable: boolean`
- Used by LoadoutAdvisor.chooseLocation() (line 161–189) to pick best spot based on style + cannon availability

### MonsterDefence
**File:** `src/main/java/com/danieljglover/allinslayer/model/MonsterDefence.java`
- `defenceLevel: int`, and per-style: `stab, slash, crush, magic, range` (integers)
- Source: slayer-data.json (line 12 in data file example)
- Used by DefaultDpsEstimator (line 50–93)

### AdviceMode
**File:** `src/main/java/com/danieljglover/allinslayer/AdviceMode.java` (line 3–6)
- Enum: DPS, COST
- Influences item selection in LoadoutAdvisor.pickItem() (line 148–150)
- Toggled by UI action (AllInSlayerPlugin.java:89–92)

---

## 7. AllInSlayerPlugin: Integration & Event Wiring

**File:** `src/main/java/com/danieljglover/allinslayer/AllInSlayerPlugin.java`

**Initialization:**
- `startUp()` (line 83–113)
  - Loads data via `dataService.load()` (line 85)
  - Wires UI callbacks (line 88–99)

**Bank Capture (Event Hook):**
- `onItemContainerChanged(ItemContainerChanged ev)` (line 160–173)
  - **CRITICAL:** When container ID is `InventoryID.BANK` (line 163), calls `inventoryService.onBankChanged()` (line 165)
    - This **persists the bank snapshot** to config
  - Also triggers re-render on any BANK/INV/WORN change (line 167–171)

**Recommendation Pipeline (`recompute` method, line 197–286):**
1. Reads task from varps or detector (line 199–204)
2. **Calls `inventoryService.currentOwned()`** (line 242) ← **Merges live inv + worn + saved bank**
3. Builds player stats (line 243)
4. **Calls `loadoutAdvisor.recommend(task, owned, stats, mode, ...)`** (line 249–250) ← **Core recommendation**
5. Extracts item IDs via `LoadoutItems.ids(recommendation)` (line 266)
6. Collects item names and prices on the client thread (line 267–268)
7. Builds `SlayerPanelState` (line 270–284)
8. Renders panel on EDT (line 285)

**State Carried:**
- `lastRecommendation: Recommendation` (line 77) — persisted for export
- `selectedLocationName: String` (line 79) — user override
- `mode: AdviceMode` (line 76) — toggle-able by UI

---

## 8. UI: SlayerPanel & LoadoutSection

**File:** `src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java`

**LoadoutSection** (line 505–612):
- Inner class that renders the loadout card
- **Key method:** `update(SlayerPanelState state)` (line 518–530)
  - Self-diffs on recommendation + item names + prices + bankAge (line 520–522)
  - Rebuilds only if state changes

**Rendering Logic** (line 532–611):
- **No loadout case** (line 536–555): shows "No owned loadout found" + hint
- **Worn items** (line 559–571):
  - Grid view of equipped items (line 559)
  - Detail rows for each slot (line 565–571)
- **Inventory items** (line 574–579):
  - Listed if present and non-empty
- **Stats row** (line 582–584): Est. DPS (from recommendation)
- **Gear cost row** (line 584): sum of worn item prices
- **Upgrades section** (line 586–596):
  - Shows `missingUpgrades` items (from recommendation.getMissingUpgrades())
  - **Excludes task's required item** (via `upgradesToShow()` line 943–961)
  - Marked as `LoadoutItemRow.State.UPGRADE` (muted visual)
- **Bank age** (line 599–607):
  - Shows "Bank seen X ago" if bankAge is not null
  - Shows "no bank seen" note if null

**SlayerPanelState Model** (src/main/java/com/danieljglover/allinslayer/ui/SlayerPanelState.java, line 12–36):
- `task: TaskData` — current task
- `recommendation: Recommendation` — populated if a loadout exists
- `bankAge: String` — "just now", "5m ago", "1h ago", or null (line 216–338 in AllInSlayerPlugin computes this)
- `itemNames: Map<Integer, String>` — item ID → name (resolved via ItemManager)
- `itemPrices: Map<Integer, Integer>` — item ID → GE price
- `selectedLocationName: String` — user-selected location override
- `playerSlayerLevel: int` — used to color-code requirement rows
- `requiredItemOwned: Boolean` — authoritative signal: null = unknown, true/false = resolved (line 30–36, 246–248 in AllInSlayerPlugin)

---

## 9. Data Source: SlayerDataService

**File:** `src/main/java/com/danieljglover/allinslayer/data/SlayerDataService.java`

**Resource:** `/data/slayer-data.json` (line 26)

**Structure:**
- JSON array of TaskData objects
- Each task has `loadouts: Map<CombatStyle, StyleLoadout>` with static BIS→budget lists
- Weakness represented as `{ "style": "MELEE"|"RANGED"|"MAGIC", "element": "air"|null|... }`

**Example Entry** (src/main/resources/data/slayer-data.json, line 1–64):
```json
{
  "task": "Abyssal demons",
  "slayerTargetId": 12,
  "weakness": { "style": "MELEE", "element": null },
  "monsterDefence": { "defenceLevel": 135, "stab": 20, "slash": 20, ... },
  "loadouts": {
    "MELEE": {
      "slotOptions": {
        "WEAPON": [22325, 1305],     // BIS first, budget options later
        "HEAD": [21264, 4720],
        ...
      }
    }
  }
}
```

**Loading:** `load()` (line 38–70)
- Parses JSON into `List<TaskData>`
- Indexes by task name (lowercase) and slayerTargetId

**Lookups:**
- `byTaskName(String name): Optional<TaskData>` (line 72–79)
- `byTargetVarp(int slayerTargetId): Optional<TaskData>` (line 81–84)
- `all(): Collection<TaskData>` (line 86–89)

---

## 10. Tests: Existing Coverage

**Files:**
- `src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java` (line 22–148)
- `src/test/java/com/danieljglover/allinslayer/loadout/DefaultDpsEstimatorTest.java`
- `src/test/java/com/danieljglover/allinslayer/loadout/LoadoutItemsTest.java`

**Key Test Cases (LoadoutAdvisorTest):**
- `dpsModePicksBisWhenOwned()` — DPS mode picks first owned item from options (line 58–70)
- `costModePicksCheaperOwnedItem()` — COST mode picks cheapest (line 72–86)
- `missingBisRecordedWhenOnlyBudgetOwned()` — missing upgrades list includes BIS when not owned (line 88–101)
- `inventorySuggestionOnlyIncludesOwnedItems()` — inventory filtered to only owned (line 104–120)
- `locationOverrideKeepsRecommendedLocationSeparateFromSelectedLocation()` — location override works (line 123–147)

**Test Helpers:**
- Mock `DpsEstimator` (line 47–49): returns 10.0 if BIS present, else 1.0
- Mock `PriceService` (line 79): test-specific prices
- `OwnedItems.fromCounts(Map)` (line 63) — build owned set for testing

---

## 11. Bank-Gate Seam: Where to Hook

**Current State:**
- Bank snapshot is **passive**: captured on bank-changed event (InventoryService.java:165 in AllInSlayerPlugin)
- Stored as simple JSON map in config
- Merged with live inv/worn on every `currentOwned()` call
- No validation of bank-read age or completeness

**Where a Bank Gate Must Hook:**
1. **Before `loadoutAdvisor.recommend()` is called** (AllInSlayerPlugin.java:249–250)
   - Check `inventoryService.bankLastSeen()` age (line 323–324)
   - If too old or null, short-circuit or show warning
   - Current: age is only displayed in UI (line 601), not enforced

2. **In `InventoryService.currentOwned()`** (line 58–88)
   - Add optional validation: e.g., reject if snapshot is stale beyond threshold
   - Add optional flag: e.g., "bankScannedThisSession" to gate on explicit scan

3. **UI Feedback** (SlayerPanel.java:605–607)
   - Currently shows "no bank seen" note, not a blocker
   - Could elevate to a warning or error state in `SlayerPanelState.status`

---

## 12. Missing Upgrades: Exactly What to Remove

**Current Mechanism (LoadoutAdvisor, line 56–92):**
1. For each equipment slot, if first option (BIS) is not owned, add to missing list (line 68–70)
2. If task has required item and not owned, add to missing list (line 89–92)
3. **Inventory items are filtered to owned-only** — none appear in missing upgrades (line 117–132)

**UI Removal Logic (SlayerPanel, line 943–961):**
- Function `upgradesToShow(state)` returns `recommendation.getMissingUpgrades()`
- **Excludes** the task's `requiredItemId` (line 951–957)
  - Reason: required item has its own row in Task section; don't duplicate (line 941)

**What to Display:**
- Missing BIS equipment items (line 586–596 in LoadoutSection)
- Marked with `LoadoutItemRow.State.UPGRADE` (muted style)
- Listed in "Upgrades you do not own" section

**Critical Detail:**
- Only **first option per slot** (BIS) appears in missing upgrades
- Budget alternatives are silently dropped if not owned
- This is intentional: focus user on the most impactful upgrades

---

## 13. Item Stats Availability

**Current State: Custom Provider, Not ItemManager.getItemStats()**

**What is used:**
- `DefaultDpsEstimator` calls `EquipmentStatsProvider.get(itemId)` (line 30)
- This returns a `Bonuses` object with: astab, aslash, acrush, amagic, arange, meleeStr, rangedStr, magicDmgPercent, attackSpeedTicks
- **Implementation details not yet traced**, but clearly NOT using `ItemManager.getItemStats()` directly

**What ItemManager IS used for:**
- `ItemManager.getItemPrice(itemId)` — for GE prices (DefaultPriceService.java:21)
- `ItemManager.getItemComposition(itemId).getName()` — for item names (AllInSlayerPlugin.java:293)

**Seam for Stat-Driven Redesign:**
- The `EquipmentStatsProvider` interface (line 6) is the **injection point**
- Replace `DefaultEquipmentStatsProvider` with one that reads stats from a database / API / ItemManager
- DpsEstimator will automatically use the new provider without code changes

---

## 14. Architecture Seams for Redesign

### Seam 1: Weakness-Driven Selection
**Current:** Static loadout lists (BIS→budget per slot)  
**Hook:** `LoadoutAdvisor.recommend()` line 52–82
- Before iterating all styles, filter to only those matching task weakness
- Reduce branching: pick the weakness style first, evaluate only that one
- Pass weakness to DpsEstimator if needed for validation

### Seam 2: Stat-Driven Ranking
**Current:** DpsEstimator scores one style at a time  
**Hook:** `DefaultDpsEstimator.estimate()` interface
- Extend to return stat-aligned item alternatives (e.g., for higher defence or slayer level)
- Use ItemManager or stat database to rank items by bonus, not static lists

### Seam 3: Bank-Gated Availability
**Current:** Passive snapshot, no gate  
**Hook:** `InventoryService.currentOwned()` or `AllInSlayerPlugin.recompute()` line 242
- Add mandatory bank-age check before recommendation
- Return error state if bank not scanned recently enough
- Expose bankAge as a hard blocker in SlayerPanelState.status

### Seam 4: Item Selection Logic
**Current:** `LoadoutAdvisor.pickItem()` line 134–159  
**Hook:** Replace with pluggable selector
- Move from binary BIS→budget to multi-option ranking by stats
- Support levelled builds (e.g., "medium-tier melee at 70+ ATK")
- Use EquipmentStatsProvider to score options

### Seam 5: DPS Estimation
**Current:** `DefaultDpsEstimator` (line 23–96)  
**Hook:** Extend or replace estimator
- Add stat-scaling: lower DPS estimates for under-levelled gear
- Add element-matching bonus for magic weakness
- Use ItemManager.getItemStats() if available; fall back to DefaultEquipmentStatsProvider

### Seam 6: Required Item Validation
**Current:** Added to missing upgrades only (LoadoutAdvisor, line 89–92)  
**Hook:** Validate before recommendation
- Block recommendation if required item missing and no alternative
- Highlight required item ownership in SlayerPanelState.requiredItemOwned (already done, line 246–248)
- Surface in error state if not owned

### Seam 7: UI State Machine
**Current:** Simple TASK_WITH/WITHOUT_LOADOUT status  
**Hook:** `SlayerPanelState.status` and panel state builders (line 89–138)
- Add status for BANK_NOT_SCANNED, INSUFFICIENT_LEVEL, MISSING_REQUIRED_ITEM
- Update LoadoutSection.rebuild() to render different messages per status

---

## 15. Critical Observation: No Dynamic Item Stats Today

- The subsystem **does NOT currently call `ItemManager.getItemStats()`** to fetch item combat bonuses
- Instead, it uses a custom `EquipmentStatsProvider` (injected singleton)
- This is the **perfect seam** for a redesign: swap the provider, no code outside DpsEstimator changes
- ItemManager IS used for prices and names, but NOT for combat stats

---

## Summary: Entry Points for Redesign

| **Concern** | **Current Location** | **Redesign Hook** |
|---|---|---|
| **Weakness matching** | Static task → style mapping | `LoadoutAdvisor.recommend()` line 52–82; filter by weakness.style |
| **Bank gating** | Passive snapshot (InventoryService.java:165) | `InventoryService.currentOwned()` line 58 or `AllInSlayerPlugin.recompute()` line 242; enforce bankLastSeen age |
| **Item selection** | BIS→budget static lists (TaskData.loadouts) | `LoadoutAdvisor.pickItem()` line 134–159; replace with stat-driven ranking |
| **DPS scoring** | `DefaultDpsEstimator` monolithic formula | `EquipmentStatsProvider` interface (line 5) + `DpsEstimator` stat scaling |
| **Stat data** | Custom `EquipmentStatsProvider` | Plug in ItemManager.getItemStats() or database; inject alternative Bonuses source |
| **Missing upgrades** | Hardcoded BIS-only (LoadoutAdvisor line 68–70) | Extend to multi-tier recommendations with stat/level thresholds |
| **UI feedback** | Generic "Ready" or "No loadout" | `SlayerPanelState.status` enum; add BANK_STALE, LEVEL_TOO_LOW, etc. |

---

## Appendix: File Directory

```
src/main/java/com/danieljglover/allinslayer/
├── AllInSlayerPlugin.java                    (core event loop + recommendation wiring)
├── AllInSlayerConfig.java                    (config keys)
├── AdviceMode.java                           (DPS vs COST enum)
├── bank/
│   ├── InventoryService.java                (bank snapshot + owned-set builder)
│   └── OwnedItems.java                       (immutable {itemId → qty} view)
├── data/
│   └── SlayerDataService.java               (loads slayer-data.json)
├── loadout/
│   ├── LoadoutAdvisor.java                  (recommendation engine)
│   ├── Recommendation.java                  (output model)
│   ├── LoadoutItems.java                    (extract IDs from recommendation)
│   ├── PlayerStats.java                     (combat skill snapshot)
│   ├── DpsEstimator.java                    (interface)
│   ├── DefaultDpsEstimator.java             (OSRS DPS formula)
│   ├── EquipmentStatsProvider.java          (item bonuses interface)
│   ├── DefaultEquipmentStatsProvider.java   (fetch bonuses)
│   ├── PriceService.java                    (interface)
│   └── DefaultPriceService.java             (ItemManager.getItemPrice)
├── model/
│   ├── TaskData.java                        (task metadata + loadouts)
│   ├── StyleLoadout.java                    (per-style slot options)
│   ├── CombatStyle.java                     (MELEE/RANGED/MAGIC enum)
│   ├── EquipmentSlot.java                   (11 slots enum)
│   ├── Weakness.java                        (style + element)
│   ├── MonsterDefence.java                  (defence level + per-style bonuses)
│   └── SlayerLocation.java                  (multi/cannon/burst tags)
├── task/
│   ├── TaskDetector.java                    (NPC-based task resolution)
│   ├── SlayerVarbits.java                   (varp/varbit constants)
│   ├── MenuClickedItemResolver.java         (extract item from menu click)
│   ├── TaskRefreshTrigger.java              (detect task changes)
│   └── ...
├── ui/
│   ├── SlayerPanel.java                     (main side panel + LoadoutSection inner class)
│   ├── SlayerPanelState.java                (render state model)
│   ├── SlayerOverlay.java                   (in-game overlay)
│   ├── ItemIconRenderer.java                (interface for item sprites)
│   ├── ItemManagerIconRenderer.java         (impl)
│   ├── PanelStatus.java                     (NO_TASK / UNSUPPORTED_TASK / TASK_WITH/WITHOUT_LOADOUT)
│   ├── RefreshSource.java                   (STARTUP / VARBIT / CHAT / MENU_CHECK / ITEM_CONTAINER / ...)
│   ├── SlayerDebugSnapshot.java             (diagnostic telemetry)
│   └── components/
│       ├── LoadoutItemRow.java              (single item display: OWNED / UPGRADE / BLOCKED)
│       ├── EquipmentGrid.java               (grid of equipped items)
│       ├── InventoryGrid.java               (grid of inventory items)
│       ├── SectionCard.java                 (collapsible card container)
│       └── ...
└── resources/
    └── data/
        └── slayer-data.json                 (canonical task + loadout dataset)
```

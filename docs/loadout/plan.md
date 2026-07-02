# All-In Slayer - Loadout Engine Redesign: Build Plan

- Author: Architect / Principal Engineer (board LA1)
- Date: 2026-06-29
- Track: T2 feature redesign of the loadout subsystem
- Status: presented at Gate 2, awaiting approval. Do NOT start building before approval.
- Reads only this file at build time. The PRD is baked into Section 11 (traceability). Executor never
  opens `docs/loadout/prd.md`.

## 1. Context and scope

Rebuild the loadout engine so it is **bank-gated**, **stat-driven** over owned items, **weakness-
targeted**, covers **gear + food + potions + runes**, and drops the recommended-upgrades concept. The
PRD (`docs/loadout/prd.md`) and the inputs (`docs/loadout/research-bank-stats-and-domain.md`,
`docs/loadout/current-subsystem-map.md`) are the source of truth.

This sits **under** the in-flight side-panel redesign (`docs/plan.md`, ADRs 0001-0004). That render
contract is preserved: one `render(SlayerPanelState)`, self-diffing sections, plain-text labels
(no `<html>`), viewport-tracking, sprites via `ItemIconRenderer`, all client reads on the client
thread. This plan changes **what the engine computes and what the loadout card shows**, not the panel
framework.

API facts (from `docs/loadout/research-bank-stats-and-domain.md`, verified against RuneLite master and
`client-1.12.31.1`):
- `ItemManager.getItemStats(int)` -> `net.runelite.client.game.ItemStats` (non-deprecated). Returns
  `null` for noted/non-existent items and until the async stat map loads. `getEquipment()` is `null`
  for non-equipment. Client-thread only.
- `ItemEquipmentStats`: `getAstab/Aslash/Acrush/Amagic/Arange`, `getStr`, `getRstr`, `getMdmg` (float),
  `getAspeed`, `getSlot()` (RuneLite `EquipmentInventorySlot` index 0-13), `isTwoHanded()`.
- `ItemStatChangesService` (core-plugin singleton): `getItemStatChanges(id)` -> `Effect`;
  `Effect.calculate(client)` -> `StatsChanges` -> `StatChange[]` with `getStat()`/`getTheoretical()`.
  Injectability **unverified** -> spike LD00.
- `ItemVariationMapping.map(id)` collapses dose/variant ids.

## 2. Architecture decisions (load-bearing) - see `docs/loadout/adr/`

| ADR | Decision | One-line why |
|---|---|---|
| [0001](adr/0001-stat-driven-gear-selection-over-owned-items.md) | Stat-driven gear selection over owned items; style fixed by weakness; drop `StyleLoadout`/`slotOptions` | Static lists go stale and ignore what you own; owned stats + weakness data suffice |
| [0002](adr/0002-equipment-stats-via-itemmanager-seam.md) | Stats via `EquipmentStatsProvider` over `ItemManager.getItemStats(int)`; null=not-loaded->skip; per-id cache; client-thread | One stats source for selection + DPS; the async-null and threading traps are real |
| [0003](adr/0003-bank-gate-on-persisted-last-seen.md) | Bank-gate on persisted `bankLastSeen()`; `BANK_NOT_SCANNED` gates the loadout card only | Persisted snapshot = open the bank once ever, not every session; task intel still shows |
| [0004](adr/0004-consumables-via-itemstatchanges-seam-and-curated-rune-table.md) | Food/potions via a `ConsumableEffectsProvider` seam over `ItemStatChangesService`; runes via a curated element->tier table | Effects are in the API (behind a fakeable seam); spell rune costs are not, so curated |
| [0005](adr/0005-remove-recommended-upgrades.md) | Remove the recommended-upgrades concept entirely | Owned-only mandate leaves no static BIS to diff against |
| [0006](adr/0006-dps-estimator-retained-for-display.md) | Keep `DefaultDpsEstimator` for display only; style fixed by weakness; magic max-hit from the chosen spell | Maths is correct/tested; only its inputs change |

## 3. Target architecture

### 3.1 Data flow (client thread -> immutable state -> EDT)

```
[client thread]  AllInSlayerPlugin.recompute(source)            (unchanged trigger set)
  | read varps -> task (dataService / taskDetector)             (unchanged)
  | owned = inventoryService.currentOwned()                     (inv + worn + last-seen bank)
  | stats = buildStats()                                        (live skill levels)
  |
  +- bankLastSeen() == null  AND task present?
  |     -> SlayerPanelState.bankNotScanned(task, ...)   --------> EDT render (loadout card = gate prompt)
  |
  +- else task present:
  |     rec = loadoutAdvisor.recommend(task, owned, stats, mode, haveCannon, selectedLocation)
  |        |  GearSelector:        owned x EquipmentStatsProvider -> Map<EquipmentSlot,Integer> worn
  |        |  ConsumableSelector:  owned x ConsumableEffectsProvider x RuneTable -> Consumables
  |        |  DefaultDpsEstimator: worn x stats x spellBaseMaxHit -> estimatedDps  (display)
  |     ids   = LoadoutItems.ids(rec)        (worn + inventory + food + potion + runes)
  |     names = collectNames(ids)            (ItemManager.getItemComposition)
  |     prices= LoadoutItems.prices(ids,...) (ItemManager.getItemPrice)
  |     -> SlayerPanelState.forTask(task, rec, ...)    -----------> EDT render (full loadout card)
  |
  +- else -> noTask / unsupportedTask (unchanged)
[EDT] SlayerPanel.render(state): self-diffing sections; loadout card renders gear + food + potions + runes
```

All `ItemManager` / `ItemStatChangesService` / `Client` reads stay on the client thread inside
`recompute` and reach the panel only through the immutable `SlayerPanelState` (PRD FR-11, NFR-3).

### 3.2 New / changed types

New (all under `com.danieljglover.allinslayer`):

| Type | Package | Purpose |
|---|---|---|
| `loadout/EquipmentSlots` | `...loadout` | Static map RuneLite slot index (0-13) -> our `EquipmentSlot`; ARMS/HAIR/JAW -> none. |
| `loadout/MeleeAttackType` | `...loadout` | enum `STAB/SLASH/CRUSH`; accessors for the matching `MonsterDefence` field and `Bonuses` attack field. |
| `loadout/GearSelector` | `...loadout` | Stat-driven per-slot selection over owned items (the core algorithm, Section 4). |
| `loadout/ConsumableSelector` | `...loadout` | Food/potion/rune selection over owned items (Section 5). |
| `loadout/Consumables` | `...loadout` | `@Value` food + potion + magic setup model on `Recommendation`. |
| `loadout/MagicSetup` | `...loadout` | `@Value` element + spell + base max hit + rune requirement + shortfalls. |
| `loadout/ConsumableEffectsProvider` | `...loadout` | Seam: `Integer healAmount(id)`, `Set<BoostedStat> boostedStats(id)` (ADR-0004). |
| `loadout/DefaultConsumableEffectsProvider` | `...loadout` | Impl over `ItemStatChangesService` + `Client`. |
| `loadout/BoostedStat` | `...loadout` | enum `ATTACK/STRENGTH/DEFENCE/RANGED/MAGIC/PRAYER`. |
| `loadout/RuneTable` | `...loadout` | Curated element->tier->{runeId->count, magic level, base max hit}; powered-staff id set; elemental/combination staff id->element map; karambwan id. |

Changed:

| Type | Change |
|---|---|
| `loadout/Bonuses` | Add `EquipmentSlot slot` (null if non-equipable/unmapped) + `boolean twoHanded`. Keep existing offensive/str/speed fields. |
| `loadout/DefaultEquipmentStatsProvider` | Populate slot + 2h; **return `null`** (not `Bonuses.zero()`) when stats null or `getEquipment()==null` (ADR-0002); add a per-id cache of non-null results only. |
| `loadout/EquipmentStatsProvider` | Doc the new `null` contract (interface unchanged otherwise). |
| `loadout/Recommendation` | Add `Consumables consumables`; **remove `List<Integer> missingUpgrades`** (ADR-0005). |
| `loadout/LoadoutAdvisor` | Rewrite `recommend(...)` to use `GearSelector` + `ConsumableSelector` + estimator; drop all `StyleLoadout`/`missingUpgrades` logic. Same public method signatures (no plugin churn). |
| `loadout/DefaultDpsEstimator` | New `estimate(style, equipped, stats, task, int spellBaseMaxHit)`; drop the `task.getLoadouts()` read (ADR-0006). |
| `loadout/LoadoutItems` | `ids(rec)` = worn + inventory + consumable ids (food, combo, potion, runes); drop `missingUpgrades`. |
| `model/TaskData` | Remove `Map<CombatStyle,StyleLoadout> loadouts`. |
| `model/StyleLoadout` | Delete (unreferenced after LD08/LD09/LD14). |
| `ui/PanelStatus` | Add `BANK_NOT_SCANNED`. |
| `ui/SlayerPanelState` | Add `bankNotScanned(...)` factory (status `BANK_NOT_SCANNED`, null rec). |
| `ui/SlayerPanel` | Loadout card: render food/potion/rune rows + bank-gate prompt; remove upgrades section; treat `BANK_NOT_SCANNED` as "has task"; simplify `requiredState` to the flag only. |
| `AllInSlayerPlugin` | Bank-gate branch in `recompute`; inject + pass consumable provider wiring through the advisor. |
| `resources/data/slayer-data.json` | Remove every `loadouts` block; audit/complete `weakness.element` for magic tasks. |

## 4. Gear selection algorithm (precise)

Inputs: `OwnedItems owned`, `EquipmentStatsProvider stats`, `TaskData task`, `PlayerStats player`,
`AdviceMode mode`, `PriceService price`.

1. **Style** = `task.weakness.style` (ADR-0001). If `weakness` or its style is null -> no gear
   (advisor returns empty; status TASK_WITHOUT_LOADOUT).
2. **Melee attack type** (only if style == MELEE): `wType = argmin over {STAB:def.stab, SLASH:def.slash,
   CRUSH:def.crush}`. Tie-break: among the tied-lowest types, pick the one for which the best owned
   weapon's matching attack bonus is highest; final tie-break STAB < SLASH < CRUSH (stable order).
3. **Scan owned equipable items.** For each `id` in `owned.ids()`: `b = stats.get(id)`. Skip if
   `b == null` (not loaded / non-equipable, ADR-0002) or `b.slot == null`. Compute `score(b)`:
   - MELEE: `score = attack(b, wType) + b.meleeStr` where `attack(b,STAB)=b.astab` etc.
   - RANGED: `score = b.arange + b.rangedStr`.
   - MAGIC: `score = b.amagic + MAGIC_DMG_WEIGHT * b.magicDmgPercent` (MAGIC_DMG_WEIGHT = 5, a named
     constant; 10% magic damage ~= 50 magic-attack-equivalent for ranking only).
4. **Group by slot**, keep only items with `score > 0` (an item that contributes no weakness-relevant
   offence does not fill a slot).
5. **Per-slot pick:**
   - DPS mode: the highest `score`; tie-break by secondary stat (melee str / rstr / mdmg) desc, then
     item id asc (deterministic).
   - COST mode: among the slot's positive-score owned items, the **cheapest** by `price.price(id)`
     (non-positive price sorts last); tie-break item id asc. (PRD FR-9; consumables ignore mode.)
6. **Weapon/shield interplay** (ADR-0001): compute `best2h` (WEAPON slot, `b.twoHanded`), `best1h`
   (WEAPON slot, `!b.twoHanded`), `bestShield` (SHIELD slot). Compare option A = score(best2h) vs
   option B = score(best1h) + score(bestShield). Pick the higher; the winner sets the WEAPON slot and,
   for B, the SHIELD slot (A leaves SHIELD empty). DPS-mode comparison uses score; COST-mode uses the
   cheaper combined price among viable options. Missing pieces degrade gracefully (only 2h owned -> A;
   only 1h owned -> B without a shield).
7. **AMMO** (ranged): the highest-`rstr` owned ammo. Weapon/ammo type compatibility is not validated
   (PRD NG-5).
8. Output `Map<EquipmentSlot,Integer> worn` (slots with no positive-score owned item are omitted).

A loadout is "viable" iff the WEAPON slot is filled. No weapon for the weakness style ->
TASK_WITHOUT_LOADOUT with a message naming the missing style weapon (still shows consumables/bank age).

## 5. Consumables selection (precise)

Inputs: `OwnedItems owned`, `ConsumableEffectsProvider fx`, `RuneTable runes`, style, element,
`PlayerStats player`, chosen WEAPON id.

- **Food (FR-4):** for each owned id, `h = fx.healAmount(id)`; pick the id with the max non-null `h`.
  If `owned.has(RuneTable.KARAMBWAN_ID)` set `comboFoodId` (combo-eat). No food owned -> `foodId=null`
  (not an error).
- **Potion (FR-5):** style-relevant boosted stats: MELEE -> any of {ATTACK,STRENGTH,DEFENCE},
  RANGED -> {RANGED}, MAGIC -> {MAGIC}. For each owned id, `s = fx.boostedStats(id)`; candidate if
  `s` intersects the style set. Rank candidates by total boost magnitude over the style-relevant stats
  (from the effect; the seam can expose magnitude, see LD04 note) and pick the best; collapse dose
  variants via `ItemVariationMapping.map(id)` so all doses of one potion count once.
- **Magic runes/spell (FR-6):** only if style == MAGIC. `element` = `task.weakness.element`; if null,
  choose the element whose highest castable tier is greatest given owned runes + magic level.
  - If the chosen WEAPON id is in `RuneTable.POWERED_STAVES` -> `poweredStaff=true`, empty rune
    requirement, spell base max hit = the staff's curated value (e.g. Trident/Sang), `runesShort` empty.
  - Else pick the highest tier T in {SURGE,WAVE,BLAST,BOLT} with `player.magic >= runes.level(element,T)`
    AND every required rune affordable from owned counts, accounting for an owned elemental/combination
    staff that supplies `element` (its rune count requirement -> 0). If owned runes are insufficient
    for any tier, pick the highest tier the player's **level** allows and record the unaffordable runes
    in `runesShort` (so the UI can show "short").
  - Output `MagicSetup{element, spellName, spellBaseMaxHit, poweredStaff, runeRequirement, runesShort}`.
- Output `Consumables{foodId, comboFoodId, potionId, magic}`.

`spellBaseMaxHit` is passed to `DefaultDpsEstimator` for the magic DPS display (ADR-0006).

## 6. Bank-gate state machine

`recompute` selects exactly one `SlayerPanelState`:

| Condition | PanelStatus | Loadout card | Other sections |
|---|---|---|---|
| no Slayer target + detector finds nothing | `NO_TASK` | empty | empty-state copy |
| target present, not in our data | `UNSUPPORTED_TASK` | empty | empty-state copy |
| task present, `bankLastSeen() == null` | **`BANK_NOT_SCANNED`** (new) | "Open your bank once so All-In Slayer can read your gear." | Task + Where/How render |
| task present, bank seen, viable loadout | `TASK_WITH_LOADOUT` | gear + food + potions + runes | render |
| task present, bank seen, no viable loadout (e.g. no weapon owned for the weakness style) | `TASK_WITHOUT_LOADOUT` | "No {style} weapon owned for this task." + consumables/bank age | render |

`SlayerPanel`'s "has task" predicate (section visibility) treats `BANK_NOT_SCANNED`,
`TASK_WITH_LOADOUT`, `TASK_WITHOUT_LOADOUT` as having a task (ADR-0003).

## 7. Dependency-ordered, test-first task breakdown

LD-prefixed to distinguish from the side-panel T01-T26. Each task: write the failing test first
(red), make it pass (green), refactor. Effort S (<2h), M (~half day), L (~day).

### Phase 0 - de-risk (gates Phase 1 consumables)

- **LD00 (spike) - verify `ItemStatChangesService` injectability + populated `getItemStats`.**
  Creates: a throwaway `@Inject ItemStatChangesService` in a scratch binding (or a one-off log in a
  dev build) run in a live client; confirm `getItemStatChanges(<shark id>).calculate(client)` yields a
  HITPOINTS change and `getItemStats(<rapier id>)` is non-null after load. Modifies: none committed.
  Deps: none. Effort: M. **Test/verify:** manual run; record the result in team memory. **Decision
  gate:** if the service is not injectable, LD04 implements the curated `id->heal`/`id->boosts` fallback
  instead (PRD R1) - same seam, different impl.

### Phase 1 - models and seams (new files, parallel-safe)

- **LD01 - enrich `Bonuses` + `DefaultEquipmentStatsProvider` (slot, 2h, null-contract, cache).**
  Modifies: `Bonuses.java`, `DefaultEquipmentStatsProvider.java`, `EquipmentStatsProvider.java` (doc),
  `BonusesTest`/new `DefaultEquipmentStatsProviderTest`. Deps: none. Effort: M.
  **Failing test first:** `provider returns null for a null-stat id and for a non-equipable id`, and
  `provider maps RL slot index + 2h flag onto Bonuses` (Mockito `ItemManager`/`ItemStats`/
  `ItemEquipmentStats`), and `a non-null result is cached (second call does not re-query ItemManager)`.
- **LD02 - `EquipmentSlots` mapping + `MeleeAttackType`.** Creates: `EquipmentSlots.java`,
  `MeleeAttackType.java` + tests. Deps: none. Effort: S. **Failing test first:** `slot index 9 -> HANDS,
  10 -> FEET, 6/8/11 -> null`; `MeleeAttackType.CRUSH reads def.crush and Bonuses.acrush`.
- **LD03 - `Consumables` + `MagicSetup` model; add `Recommendation.consumables`.** Creates:
  `Consumables.java`, `MagicSetup.java`; modifies `Recommendation.java` (add field only). Deps: none.
  Effort: S. **Failing test first:** `Recommendation with equal consumables is value-equal` (protects
  NFR-4 self-diff).
- **LD04 - `ConsumableEffectsProvider` seam + `DefaultConsumableEffectsProvider` + `BoostedStat`.**
  Creates: the three files + `DefaultConsumableEffectsProviderTest`. Impl uses LD00's outcome (service
  or curated fallback). Expose per-stat boost magnitude (for FR-5 ranking) alongside the boosted-stat
  set. Deps: LD00. Effort: M. **Failing test first:** with a fake `ItemStatChangesService` (or canned
  map), `healAmount(sharkId)==20`, `boostedStats(superCombatId) contains ATTACK,STRENGTH,DEFENCE`,
  `healAmount(nonFood)==null`.
- **LD05 - `RuneTable` (curated).** Creates: `RuneTable.java` (+ powered/elemental staff sets,
  karambwan id) + `RuneTableTest`. Encodes research C.4 (Bolt/Blast/Wave/Surge x air/water/earth/fire:
  rune ids+counts, magic level, base max hit). Deps: none. Effort: M. **Failing test first:**
  `RuneTable.requirement("fire", SURGE)` = {air:7, fire:10, wrath:1}, level 95, base max hit 24;
  `tiersDescending()` = SURGE,WAVE,BLAST,BOLT.

### Phase 2 - selection logic (new classes, parallel-safe)

- **LD06 - `GearSelector`.** Creates: `GearSelector.java` + `GearSelectorTest`. Implements Section 4.
  Deps: LD01, LD02. Effort: L. **Failing test first (with a fake `EquipmentStatsProvider`):**
  `picks the higher-melee-score owned item in a slot`; `never picks an unowned id`; `skips
  null-stat ids`; `melee attack type = monster's lowest defence`; `2h beats 1h+shield when its score is
  higher, else 1h+shield`; `COST mode picks the cheapest positive-score owned item`; `no weapon owned
  -> empty worn`.
- **LD07 - `ConsumableSelector`.** Creates: `ConsumableSelector.java` + `ConsumableSelectorTest`.
  Implements Section 5. Deps: LD03, LD04, LD05. Effort: L. **Failing test first (fakes):** `picks
  highest-heal owned food + flags karambwan`; `picks style-matching potion by magnitude`; `magic:
  highest castable tier given level + owned runes`; `insufficient runes -> lower tier + runesShort`;
  `owned elemental staff zeroes that element's rune`; `powered staff -> empty rune requirement`.

### Phase 3 - advisor + estimator (hot, single-owner)

- **LD09 - `DefaultDpsEstimator` signature change.** Modifies: `DpsEstimator.java`,
  `DefaultDpsEstimator.java`, `DefaultDpsEstimatorTest`. New `estimate(style, equipped, stats, task,
  int spellBaseMaxHit)`; magic branch uses the param, no `task.getLoadouts()` read. Deps: LD03. Effort:
  M. **Failing test first:** `magic DPS uses the passed spellBaseMaxHit (not task data)`; melee/ranged
  branches unchanged (existing tests adapted to the new arg).
- **LD08 - rewrite `LoadoutAdvisor.recommend`.** Modifies: `LoadoutAdvisor.java`, `LoadoutAdvisorTest`
  (rewritten). Orchestrates `GearSelector` + `ConsumableSelector` + estimator; sets
  `rec.consumables`; keeps `location`/`recommendedLocation`/`locationReason`/`method`/`totalGearCost`
  logic; **stops setting `missingUpgrades`** and stops reading `StyleLoadout`. Same public signatures.
  Deps: LD06, LD07, LD09. Effort: L. **Failing test first:** `recommend produces worn from
  GearSelector + consumables from ConsumableSelector for a MELEE task`; `empty when style has no owned
  weapon`; `location override still separates recommended vs selected` (port existing test).

### Phase 4 - remove upgrades (hot-ish, single-owner)

- **LD10 - remove `missingUpgrades`; route consumable ids through `LoadoutItems`.** Modifies:
  `Recommendation.java` (delete field), `LoadoutItems.java` (ids = worn + inventory + food + combo +
  potion + runes; drop missingUpgrades), `LoadoutItemsTest`, and any remaining test referencing
  `getMissingUpgrades` (`LoadoutAdvisorTest` already rewritten in LD08). Deps: LD08, LD03. Effort: M.
  **Failing test first:** `LoadoutItems.ids includes food/potion/rune ids and de-dups`; `Recommendation
  has no missingUpgrades` (compile-time + a guard).

### Phase 5 - plugin gate + state (hot, single-owner)

- **LD11 - `PanelStatus.BANK_NOT_SCANNED` + `SlayerPanelState.bankNotScanned`.** Modifies:
  `PanelStatus.java`, `SlayerPanelState.java`, `SlayerPanelStateTest`. Deps: none. Effort: S.
  **Failing test first:** `bankNotScanned(task,...) has status BANK_NOT_SCANNED, null recommendation,
  carries task + developerMode`.
- **LD12 - `AllInSlayerPlugin.recompute` bank-gate + wiring.** Modifies: `AllInSlayerPlugin.java`.
  Add the `bankLastSeen()==null && task present -> bankNotScanned(...)` branch (Section 6); inject
  `ConsumableEffectsProvider` and pass it (and `RuneTable`) into the advisor (constructor/Guice). Deps:
  LD08, LD11, LD04, LD05. Effort: M. **Verify:** not headless-testable (field-injected, client-thread);
  covered by the manual checklist (Section 9) + the unit-tested `bankNotScanned` factory and gate
  helper. Keep the gate decision in a tiny pure helper if a unit test is cheap.

### Phase 6 - UI (hot, single-owner)

- **LD13 - `SlayerPanel` loadout card.** Modifies: `SlayerPanel.java`, `SlayerPanelTest`. Render
  food row + potion row + (magic) spell + rune rows (LoadoutItemRow OWNED, BLOCKED when `runesShort`);
  bank-gate prompt for `BANK_NOT_SCANNED`; **remove** the upgrades section + `upgradesToShow`; simplify
  `requiredState` to the `requiredItemOwned` flag (drop the missingUpgrades fallback); treat
  `BANK_NOT_SCANNED` as "has task". Keep self-diff/viewport/left-align/no-`<html>` invariants. Deps:
  LD03, LD05, LD10, LD11. Effort: L. **Failing test first:** `BANK_NOT_SCANNED renders the gate prompt
  and no gear rows, Task section still present`; `loadout card renders a food row + potion row`; `magic
  task renders spell + rune rows, short rune is BLOCKED`; `no upgrades section in any state`.

### Phase 7 - data migration (hot, single-owner)

- **LD14 - remove `loadouts` from `TaskData` + `slayer-data.json`; audit `weakness.element`.**
  Modifies: `TaskData.java` (drop field), `slayer-data.json` (remove every `loadouts` block; fill
  `weakness.element` for magic tasks), `DuradelDatasetValidationTest` (assert `weakness` +
  `monsterDefence` per task; drop the loadouts assertions), `TaskDataJsonTest` (drop spellMaxHit/
  slotOptions round-trip), `SlayerDataServiceTest` (unaffected lookups). Deps: LD08, LD09 (no longer
  read loadouts). Effort: M. **Failing test first:** `every task has a non-null weakness.style +
  monsterDefence`; `every MAGIC-weakness task has a non-null element` (new, makes FR-6 data correct).
- **LD15 - delete `StyleLoadout.java`.** Modifies: delete the file. Deps: LD14, LD08, LD09 (zero
  references). Effort: S. **Verify:** full `gradlew.bat test` compiles + green.

### 7.1 Conflict-safe single-owner map (hot/shared files)

| File | Sole editing task |
|---|---|
| `loadout/LoadoutAdvisor.java` | LD08 |
| `loadout/DefaultDpsEstimator.java` (+`DpsEstimator.java`) | LD09 |
| `loadout/Recommendation.java` | LD03 (add) then LD10 (remove) - sequential phases |
| `loadout/LoadoutItems.java` | LD10 |
| `loadout/Bonuses.java` / `DefaultEquipmentStatsProvider.java` | LD01 |
| `AllInSlayerPlugin.java` | LD12 |
| `ui/SlayerPanel.java` | LD13 |
| `ui/PanelStatus.java` / `ui/SlayerPanelState.java` | LD11 |
| `model/TaskData.java` / `resources/data/slayer-data.json` | LD14 |

No two tasks edit the same file concurrently. Phases 1-2 are internally parallel.

## 8. Test strategy

- **Stat-driven selection (headless).** `GearSelectorTest` injects a **fake `EquipmentStatsProvider`**
  returning canned `Bonuses` (with slot + 2h) per id and a fake `PriceService`; `OwnedItems.fromCounts`
  builds the owned set. Asserts per-slot picks, attack-type derivation, 2h/shield choice, DPS vs COST,
  null-skip, no-weapon empty. No live `ItemManager`.
- **Consumables (headless).** `ConsumableSelectorTest` injects a **fake `ConsumableEffectsProvider`**
  (canned heal/boosts) and a real `RuneTable`; owned set via `fromCounts`. Asserts food/potion/rune
  picks, tier drop on insufficient runes, staff handling. `RuneTableTest` locks the curated numbers.
- **Bank-gate (headless).** `SlayerPanelStateTest.bankNotScanned(...)` asserts the new status + null
  rec + carried task/devMode; if a pure gate helper exists, a small table test over
  `(taskPresent, bankSeen, weaponOwned)` -> PanelStatus. The plugin wiring is manual (Section 9).
- **Estimator.** `DefaultDpsEstimatorTest` adapted to the `spellBaseMaxHit` arg; magic DPS uses the
  param.
- **UI (headless).** `SlayerPanelTest` uses the existing recording `ItemIconRenderer` fake; asserts the
  gate prompt, food/potion/rune rows, short-rune BLOCKED, and **no upgrades section** in any state;
  re-asserts NFR-4 (value-equal recommendation -> 0 loadout rebuilds).
- **Data.** `DuradelDatasetValidationTest` becomes the guard that weakness + monsterDefence (+ magic
  element) are present and correct for every task - the data the engine now depends on.
- **Whole suite green** (`gradlew.bat test`) after every task; LD15 is the final compile/green gate.

## 9. Manual verification checklist (plugin wiring + sprites, not headless)

1. Fresh profile, never opened bank, on a Slayer task -> loadout card shows the "open your bank" prompt;
   Task + Where/How still render; no gear rows.
2. Open bank once -> loadout card fills with owned gear; bank age shows "just now".
3. Melee task -> gear matches the monster's weakest melee defence; Est. DPS shows.
4. Magic task with element -> spell + rune rows; a rune you lack shows "short"/BLOCKED; powered staff ->
   no rune rows.
5. Food + potion rows show your best owned; karambwan flagged when owned.
6. Toggle DPS/Cost -> gear picks change (cost = cheaper), consumables unchanged.
7. No "upgrades you do not own" section anywhere.
8. Sprites paint; no horizontal scrollbar; wheel scrolls; no flicker on an unchanged re-render.

## 10. Risks (carried from PRD)

R1 `ItemStatChangesService` injectability (LD00 gate; curated fallback). R2 async-null stats (skip +
recompute, NFR-5). R3 stale persisted bank (gate-on-existence + show age). R4 data migration vs
validation tests (single-task LD14). R5 set-effect/coupled items mis-scored (accepted v1, NG-5/NG-6).

## 11. Requirements traceability (PRD -> plan)

Every PRD requirement ID + verbatim acceptance criterion, mapped to the task(s) that build it and the
verification step. Zero unmapped IDs.

| PRD ID | Acceptance criterion (verbatim) | Task(s) | Verification |
|---|---|---|---|
| FR-1 | When `InventoryService.bankLastSeen()` is `null`, no `Recommendation` is produced; if a task is detected the panel status is `BANK_NOT_SCANNED` and the loadout card shows the "open your bank" prompt; task and Where/How sections still render. Once `bankLastSeen() != null`, a loadout is produced. | LD11, LD12, LD13 | `SlayerPanelStateTest` (factory), `SlayerPanelTest` (gate prompt + sections), manual #1-2 |
| FR-2 | Given a fake stats provider and two owned items mapping to the same slot, the selector picks the one with the higher style score; an unowned item with a better score is never picked; an owned item with no positive relevant bonus is not used to fill the slot. | LD01, LD06 | `GearSelectorTest` |
| FR-3 | A MELEE task with `crush` lowest selects gear by crush attack bonus; a MAGIC task with `element="fire"` drives a fire spell; a RANGED weakness selects by ranged attack + ranged strength. | LD02, LD06, LD07 | `GearSelectorTest`, `ConsumableSelectorTest` |
| FR-4 | The highest-heal owned food id is chosen; karambwan surfaced as combo food when owned; no food owned -> none recommended (not an error). | LD04, LD07 | `ConsumableSelectorTest`, manual #5 |
| FR-5 | An owned potion whose positive boosts match the style is chosen; the higher-magnitude boost wins between two matching potions; dose variants treated as the same potion. | LD04, LD07 | `ConsumableSelectorTest`, manual #5 |
| FR-6 | With enough air+fire runes and Magic 95 a fire task recommends Fire Surge; insufficient runes drop to the highest affordable tier; an owned elemental/combination staff removes that element's rune; an owned powered staff yields an empty rune requirement. | LD05, LD07, LD13 | `RuneTableTest`, `ConsumableSelectorTest`, `SlayerPanelTest`, manual #4 |
| FR-7 | `Recommendation` has no `missingUpgrades`; `LoadoutAdvisor` never builds an upgrade list; the panel renders no upgrades section; the guard test asserts no upgrades row for any state. | LD08, LD10, LD13 | `LoadoutItemsTest`, `SlayerPanelTest` (no-upgrades guard), manual #7 |
| FR-8 | The panel renders `Est. DPS` for a produced loadout; the estimator is fed by the same `ItemManager`-backed stats as selection. | LD09, LD08, LD13 | `DefaultDpsEstimatorTest`, `SlayerPanelTest`, manual #3 |
| FR-9 | COST mode selects the cheapest positive-score owned item in a slot; DPS mode selects the highest-score item; consumables unaffected by mode. | LD06 | `GearSelectorTest`, manual #6 |
| FR-10 | For a produced loadout the panel shows worn gear rows, a food row, a potion row, and (for magic) spell + rune rows; absent parts omitted cleanly. | LD10, LD13 | `SlayerPanelTest`, manual #3-5 |
| FR-11 | The plugin resolves stats, effects, names, and prices in `recompute` on the client thread and marshals to the EDT; no UI component calls a client service. | LD12, LD13 | code review (no client calls off-thread / in components), manual #8 |
| NFR-1 | Selection completes < 10 ms for 2000 distinct owned ids once the stat cache is warm. | LD01, LD06 | benchmark test (build a 2000-id owned set with a warm fake/cached provider; assert wall time budget) |
| NFR-2 | Each item id's stats fetched from `ItemManager` at most once per session. | LD01 | `DefaultEquipmentStatsProviderTest` (second call does not re-query) |
| NFR-3 | 0 `ItemManager`/`ItemStatChangesService`/`Client` calls on the EDT. | LD12, LD13 | code review + manual #8 |
| NFR-4 | An unchanged recommendation re-renders with 0 loadout-section rebuilds. | LD03, LD13 | `SlayerPanelTest` self-diff test (value-equal Recommendation incl. consumables) |
| NFR-5 | A `null` `getItemStats(id)` is skipped, never cached as permanent "no stats"; a later recompute recovers it. | LD01, LD06 | `DefaultEquipmentStatsProviderTest` (null not cached), `GearSelectorTest` (null skipped) |
| NG-1 | No automation: the plugin never equips, withdraws, drinks, or casts. | (design constraint) | code review: advisor/panel emit data only; no game writes |
| NG-2 | No recommended-upgrades / shopping advice. | LD05(ADR), LD08, LD10, LD13 | same as FR-7 |
| NG-3 | No in-combat / per-phase gear switching; one loadout per task. | (design constraint) | code review: single `Recommendation` per task |
| NG-4 | Rune-pouch contents not read in v1. | LD07 | `ConsumableSelectorTest` documents inv+worn+bank only; follow-up filed |
| NG-5 | Weapon/ammo compatibility not validated; highest-rstr ammo suggested. | LD06 | `GearSelectorTest` (ammo by rstr), limitation noted |
| NG-6 | Set-effect items scored on flat stats only. | LD06 | code review: no set-bonus logic; limitation noted |
| NG-7 | No change to task detection, overlay, Inventory Setups export, or the side-panel render contract. | (scope fence) | full suite green; those files untouched except the loadout card |

# AIO-Slayer-Assistant — Runtime Pipeline Audit (DT-CTX-CODE)

**Date:** 2026-07-01 · **Scope:** current runtime pipeline vs the target behaviour · **Method:** read-only, grounded in `file:line`.
Paths are relative to `src/main/java/com/danieljglover/allinslayer/` unless noted. Data paths are relative to `src/main/data/slayer/`.

> **Target behaviour (the /goal), verbatim intent:** for every Duradel task, suggest in order (1) the monster **VARIANT**, (2) the **LOCATION** filtered/suggested *by that variant*, (3) the **LOADOUT** (equipment + inventory) derived from the variant's variables + the location's variables + the wiki strategy. Loadout is selected from the player's **BANK**, scanned on login; until a bank scan exists the plugin must tell the user to open the bank before it functions fully.

> **Headline deltas (read the GAP LIST §8 for the ranked, code-sited version):**
> 1. **Data is now build-generated.** `/data/slayer-data.json` is deleted from `src/main/resources` and generated at build time from the modular sources in `src/main/data/slayer/` (`build.gradle:44-64`). The runtime is unchanged (`SlayerDataService` still reads the classpath resource), but the compiler (`ModularSlayerDataCompiler`) **flattens away** most of the rich source data.
> 2. **`requiredItemId` is dropped by the compiler** → the required-item row never renders and ownership is always "unknown" (see §7/§8 G1). Regression surfaced by the modular migration.
> 3. **Location is NOT filtered or suggested by variant.** Locations are a task-level flat list; the variant axis carries only a free-text `location` note (§4). Orthogonal, as ADR-0011 declared — the modular data did **not** change that.
> 4. **The boss varbit-4723 pre-select is dormant in data:** every boss variant has `bossId: null`, so `resolveBossVariantName` always falls through to the default (§7).
> 5. **No login bank scan exists.** The bank is captured only when the bank container changes (opened); a prior snapshot persists across sessions (§2).

---

## 1. Pipeline map: login → task → variant → method → location → bank → recommend → render

### 1.1 Entry points (events → `recompute`)
`AllInSlayerPlugin.startUp` (`AllInSlayerPlugin.java:92-135`) loads data (`dataService.load()`, :95), wires the six panel callbacks (:98-121), adds the nav button + overlay, and calls `recompute(STARTUP)` on the client thread (:134). Every subsequent update funnels through `recompute(RefreshSource)` (`:219-351`). Triggers:

| Event handler | Line | Fires recompute when |
|---|---|---|
| `onVarbitChanged` | :144-151 | `TaskRefreshTrigger.isTaskStateChange` — varp 395/394/2096/4258 **or** varbit 4723 (`TaskRefreshTrigger.java:16-25`) |
| `onChatMessage` | :153-160 | slayer assignment/progress phrases (`TaskRefreshTrigger.java:27-41`) |
| `onMenuOptionClicked` | :162-179 | "Check" on enchanted gem/slayer helm items 4155/11864/11866 (`TaskRefreshTrigger.java:43-48`) |
| `onItemContainerChanged` | :181-195 | BANK → **also** snapshots the bank (`inventoryService.onBankChanged`, :187); BANK/INV/WORN → recompute (:189-194) |
| `onGameStateChanged` | :197-205 | transition to `LOGGED_IN` only (`TaskRefreshTrigger.java:54-57`) — **rebuild only, no bank scan** |
| `onConfigChanged` | :207-216 | any `allinslayer` config key (picks up developer-mode live) |

### 1.2 `recompute` step-by-step (`AllInSlayerPlugin.java:219-351`)
1. **Read varps** (:221-224): `SLAYER_TARGET=395`, `SLAYER_COUNT=394`, `SLAYER_AREA=2096` (via `client.getVarpValue`); `bossTargetId = getVarbitValue(4723)` guarded in `readBossTargetVarbit` (:405-415, returns -1 on exception). Constants in `SlayerVarbits.java:11-15` (`SLAYER_COUNT_ORIGINAL=4258` is a refresh trigger only, never read here).
2. **Resolve TaskData** (:225-226): `targetId>0 ? dataService.byTargetVarp(targetId)` else empty; if absent, `taskDetector.resolveCurrentTask()`.
   - `byTargetVarp` is an exact map lookup on `TaskData.slayerTargetId` (`SlayerDataService.java:81-84`, map built at :59-62).
   - `TaskDetector.resolveCurrentTask` (`TaskDetector.java:32-46`) re-reads the varps, tries `byTargetVarp`, then `resolveByRuneLiteTaskDb` (:48-68): reads RuneLite DBTable **113** field **10** by target id and matches on `byTaskName` (case-insensitive, `SlayerDataService.java:72-79`).
3. **Debug snapshot** built (:229-236) — dev-only telemetry.
4. **No-task branch** (:240-254): clears state, renders `unsupportedTask` (targetId>0 but unmapped) or `noTask`. Returns.
5. **Per-task selection hygiene** (:256-275):
   - task changed (`lastSetupName != t.getTask()`) → clear `selectedLocationName/selectedVariantName/selectedMethod` (:257-263).
   - stale location dropped if not in `t.getLocations()` (:264-267, `hasLocation` :439-453).
   - stale variant dropped if not in `t.getVariants()` (:268-272, `hasVariant` :456-470).
   - **Boss auto-seed** (:275): `resolveBossVariantName(t, bossTargetId, selectedVariantName)` (:479-493) — user override wins; else for an all-boss task, match a variant whose `bossId == varbit`; else unchanged (→ default). **Dormant in data (§7).**
6. **Owned + stats** (:276-282): `owned = inventoryService.currentOwned()`; `stats = buildStats()` (real levels ATK/STR/DEF/RANGED/MAGIC/SLAYER, :375-384); `requiredItemOwned = requiredItemId==null ? null : owned.has(id)` (:280-282) — **always null in practice, §7 G1**.
7. **Bank gate** (:239, :287-310): `bankGate = isBankGate(task present, inventoryService.bankLastSeen())` (:434-437). If gated → render `bankNotScanned`, no loadout, return (§2).
8. **Recommend** (:312-313): `loadoutAdvisor.recommend(t, owned, stats, mode, config.haveCannon(), selectedLocationName, selectedVariantName, selectedMethod)`.
9. **Render** (:314-350): resolves item names (`collectNames`, :353-361) + GE prices off one id list (`LoadoutItems`), sets overlay, builds `SlayerPanelState.forTask(...)` and `panel.render(state)` on the EDT.

---

## 2. Bank gate — exactly

**What triggers a scan today:** ONLY `onItemContainerChanged` with `containerId == InventoryID.BANK` (`AllInSlayerPlugin.java:185-188`) → `InventoryService.onBankChanged(container)` (`InventoryService.java:39-55`). RuneLite fires this when the bank is open/changes, i.e. **when the player opens the bank**. There is **no login scan** and **no config-triggered scan**.

**State it sets:** serialises a `{itemId: qty}` map to the **RS-profile** config under group `allinslayer`, keys `bankSnapshot` + `bankSnapshotTs = System.currentTimeMillis()` (`InventoryService.java:23-24, 53-54`).

**Is anything scanned AT LOGIN?** No bank scan. `onGameStateChanged`→`recompute(GAME_STATE)` only re-renders (`AllInSlayerPlugin.java:197-205`). The bank is not readable unless open (class Javadoc, `InventoryService.java:17`). However `currentOwned()` (`:58-88`) merges live **INV + WORN** with the **persisted** bank snapshot from config — so a snapshot from a *previous* session is reused immediately at login.

**Does the scan persist across sessions?** **Yes** — it is stored in RS-profile config, so `bankLastSeen()` (`:91-107`) returns non-null across restarts once the bank has ever been opened on that profile.

**What the panel shows with no scan (exact text):**
- Gate state is `PanelStatus.BANK_NOT_SCANNED` (`SlayerPanelState.bankNotScanned`, `SlayerPanelState.java:172-181`).
- Loadout card body renders the wrapping prompt = `state.getStatusMessage()` = **"Open your bank once so All-In Slayer can read your gear."** (`SlayerPanelState.java:179`; rendered `SlayerPanel.java:662-670`, node name `loadout-bank-gate`).
- Task + Where/How sections still render (`SlayerPanel.java:229-236` treats `BANK_NOT_SCANNED` as "has task"). Only the loadout card is gated.
- Separately, whenever `bankAge == null` a non-blocking note is shown in the loadout ("We haven't seen your bank yet…", `noBankNote`, `SlayerPanel.java:977-985`).

**Delta vs target ("scanned on login; tell users to open the bank first"):**
- ✅ "Tell users to open the bank first" is implemented (exact copy above), and the gate correctly blocks only the loadout.
- ⚠️ "Scanned on login" is **only partially** met: there is no fresh login/active scan. A prior snapshot persists and is reused, so after the *first ever* bank open the gate never returns — but a *brand-new* profile stays gated until the user manually opens the bank, and a stale snapshot is never refreshed on login nor flagged as stale (only relative age text "Xm/h ago", `bankAgeText` `:386-403`).

---

## 3. Variant suggestion — is a variant pre-selected?

**Yes, a variant is pre-selected (not just listed).** The combo is populated from `task.getVariants()` and its selection is driven by the shared resolver:
- Panel: `reconcileVariant` (`SlayerPanel.java:528-554`) sets items to variant labels (boss gets a ` (Boss)` suffix, `variantLabel` :1232-1235) and pre-selects `selectedVariantLabel` = `MonsterVariant.resolve(task, state.getSelectedVariantName())` (:1250-1254). The combo is **hidden + inert when the task has ≤1 variant** (:551-553).
- The default rule (`MonsterVariant.resolve`, `MonsterVariant.java:59-88`): selected name match → else the `isDefault` variant → else the first listed → else null. Data sets exactly one default via `defaultVariantId` (compiler `resolveVariants` :470, validated for multi-variant tasks `ModularSlayerDataCompiler.java:236-248`).
- The **same** resolver drives the engine (`LoadoutAdvisor.recommend` :85) so the panel's pre-selected variant can never drift from the geared variant (FR-6 coupling, `MonsterVariant.java:50-54`).

**What drives the default:** the dataset's `defaultVariantId` on the task (e.g. `abyssal-demons` default = `abyssal-demon`, `tasks/abyssal-demons.json:99`). For the all-boss Boss task the default is `boss-abyssal-sire` (`tasks/boss.json` `defaultVariantId`). The varbit-4723 override that *would* change the boss default is dormant (§7).

---

## 4. Location handling — the key question

**Locations are TASK-LEVEL, not per-variant. There is no per-variant filtering.**
- The runtime task carries a flat `List<SlayerLocation> locations` (`TaskData.java:39`), built by the compiler from the task's `locationIds`, each resolved against the **shared** `locations/*.json` files (`ModularSlayerDataCompiler.resolveLocations`, `:425-442`).
- The variant axis carries only a **free-text** `location` string (`MonsterVariant.location`, `MonsterVariant.java:34`), sourced from the *monster variant* file's `location` field (compiler :471; e.g. `"Abyssal Area, Slayer Tower, Catacombs of Kourend, Wilderness Slayer Cave"` in `monsters/abyssal-demons/abyssal-demon-lvl124.json`). It is display-only and appears **only** in the boss note (`SlayerPanel.bossNote` :1023-1043). It never filters the location dropdown.
- The location dropdown lists **all** task locations regardless of the selected variant: `locationNames(state)` iterates `task.getLocations()` (`SlayerPanel.java:1277-1288`), and `reconcileCombo` (:579-604) is variant-agnostic.
- **Confirms ADR-0011:** variant × location is orthogonal, and the modular data migration did **not** change this. (The source *does* carry per-variant `variantInfo[].locations` and per-task `locationComparison[]` with `multicombat/cannonable/safespottable/notes` — see `tasks/abyssal-demons.json:126-243` — but the compiler **drops all of it**; only `locationIds` → shared location files survive.)

**Is any location SUGGESTED/defaulted?** Yes — a recommended location is computed (independent of variant): `LoadoutAdvisor.chooseLocation(task, style, haveCannon)` (`:134`, impl `:315-343`): if `haveCannon` and a `cannon` location exists → that; else if style is `MAGIC` and a `burst` location exists → that; else the **first** location. Surfaced as `Recommendation.recommendedLocation` and shown as the brand-accent headline in Where&How (`SlayerPanel.recommendedLocation` :1306-1321, rendered :482-497). The location combo pre-selects the recommended one when the user hasn't picked (`selectedLocationName` :1290-1304).

**Which location does the loadout computation use when none is selected?** For gear, the only location signal that reaches the engine is the **Wilderness** flag, and it is taken from the **user-selected** location only, defaulting to `false` when nothing is selected (`LoadoutAdvisor.java:107-109` → `findLocation(task, selectedLocationName)`, :345-360). The *recommended* location's flags do **not** feed gear. So with no selection the loadout is computed as non-Wilderness regardless of which location is recommended.

**Which location variables actually change the loadout — used vs carried-but-unused** (`SlayerLocation`, `model/SlayerLocation.java:10-19`):

| Field | Used by loadout? | Where |
|---|---|---|
| `wilderness` | **Yes** — gear only | `LoadoutAdvisor.java:107-109` → `GearSelector.select(..., wilderness, ...)` :89-119 → `BonusContext.from(profile, wilderness)` :104, credits Wilderness weapons' +50% |
| `cannon` | **No effect on gear/consumables** — only picks the recommended *location* | `chooseLocation` :325,334; `locationReason` :379-381. Nothing adds a cannon/cannonballs to the loadout. Gated by `config.haveCannon()`. |
| `burst` | **No effect on gear** — only picks the recommended *location* for MAGIC | `chooseLocation` :329,338 |
| `multi` | **No** — display tag + reason text only | `tagRow` `SlayerPanel.java:1073`; `locationReason` :387-388 |
| `konarLockable` | **No** — display tag only | `tagRow` `SlayerPanel.java:1085`; unused by the advisor entirely |
| `safespot` | **Field does not exist in runtime model** | `SourceLocation.safeSpot` exists (`source/SourceLocation.java:16`) but `resolveLocations` (`ModularSlayerDataCompiler.java:437-438`) omits it — dropped. |

Net: of the location traits, **only `wilderness` (and only when user-selected) changes the loadout.** `cannon`/`burst` steer only the recommended-location pick; `multi`/`konarLockable` are cosmetic; `safespot`/`cannonable`/`multicombat`/`amount`/`notes` from the source are not in the runtime model at all.

---

## 5. Variant → loadout variables — used vs unused

The variant resolves to a `MonsterProfile` (`MonsterProfile.fromVariant(task, variant)`, `LoadoutAdvisor.java:85-88`, impl `MonsterProfile.java:53-66`), which the engine reads instead of the task. Fields:

| Variant field | Drives loadout? | Mechanism |
|---|---|---|
| `weakness.style` | **Yes** — the **default** combat style (`recommendedStyle`) | `MonsterProfile.recommendedStyle` :69-72; used as `defaultMethod` when no strategy/override (`LoadoutAdvisor.java:96-99`). Per-field fallback to task weakness when null (`MonsterProfile.java:59-60`). |
| `weakness.element` | **Yes** — magic spell pick (inert unless style is magic) | `MonsterProfile.element` :75-78 → `ConsumableSelector`/`MagicWeaponEvaluator` (`LoadoutAdvisor.java:125-128`; `GearSelector.java:152,455`) |
| `monsterDefence` | **Yes** — melee attack-type pick + DPS accuracy | `GearSelector.meleeAttackType` :100-101,186-210; `weaponRank`/`DpsEstimator`. Fallback to task defence when null (`MonsterProfile.java:61-62`). |
| `demon` | **Yes** — demonbane bonus predicate | `BonusContext.from(profile,…)` (`GearSelector.java:104`) → `ConditionalBonusRegistry` demonbane (Arclight/Emberlight/Scorching bow) |
| `dragon` | **Yes** — dragonbane predicate (DHL/DHCB) | same path |
| `kalphite` | **Yes** — Keris predicate | same path |
| `undead` | **Yes** — Salve-amulet predicate | same path |
| `strategy` | **Yes** — overrides default method **and** the WEAPON slot | default method = `strategy.primaryStyle` when valid (`LoadoutAdvisor.java:95-99`); weapon override ids for the effective style (`strategyOverrideIds` :182-224, multi-style incl. secondary weapons) → `GearSelector.pickStrategyWeapon` (:350,506-531): highest-priority **owned+viable** id wins the WEAPON slot, else bank-aware fallback to the DPS pick. The "Wiki strategy" note is composed independently (`composeStrategyNote` :232-263), shown regardless of ownership/style. |
| `isBoss` | **Yes** — UI only (FR-7 "Boss - separate trip") + boss-task detection | `Recommendation.boss` (`LoadoutAdvisor.java:158`) → `SlayerPanel.bossNote` :696-700; `isBossTask` :496-510 |
| `isDefault` | Selection only (not combat) | `MonsterVariant.resolve` :80-86 |
| `bossId` | **Wired but dormant** — boss varbit pre-select | `resolveBossVariantName` :479-493 — all data is null (§7) |
| `name` | UI label + resolution key | combos + `Recommendation.variantName` :157 |
| `location` / `requirement` | **UI note only** (boss note); NOT a rich location, NOT a gear input | `Recommendation.variantLocation/Requirement` :159-160 → `bossNote` :1026-1039 |
| `combatLevel` | **Unused** — display only, and not surfaced in the panel today | set by compiler :462; no read in the loadout/panel path |
| `npcIds` | **Unused at runtime** | carried (`MonsterVariant.java:24`) but no runtime consumer for variant-level ids |

**Owned-weapon rule:** the strategy override is bank-aware — it only wins if the player owns a viable weapon for the effective style; otherwise the stat DPS pick stands (`GearSelector.java:345-362, 506-531`). This is the one place "the player's bank" directly shapes the *weapon* choice beyond generic stat scoring.

**Note on `Recommendation.method`:** set to `task.getRecommendedMethod()` — the long wiki prose string, **not** the effective `CombatStyle` (`LoadoutAdvisor.java:150`). The overlay and the Where&How "Method" note therefore show wiki prose (`SlayerPanel.methodText` :1193-1201), while the *effective* style lives in the separate "Attack style" selector.

---

## 6. Panel ordering — actual vs target

**Actual top-to-bottom** (`SlayerPanel` construction `:130-138`, render `:173-206`):
1. **HeaderBand** (anchored, never scrolls): `TaskHeader` (task name/count) + `ActionBar` (mode toggle · refresh · export) — `:109-116`.
2. **Task** section (`TaskSection` :325-373): Slayer-level row, Weakness row, and — *if `requiredItemId != null`* — a Required item row. **The required row never renders today (§7 G1).**
3. **Where & How** section (`WhereSection` :377-621), in this internal order:
   1. `whereBody`: recommended-location headline + trait tags + "Why" prose + "Method" prose (`:478-520`).
   2. **Variant** caption + combo (`:407-428`) — hidden if ≤1 variant.
   3. **Attack style** selector (`:431-438`) — **always visible**, all three Melee/Ranged/Magic segments (`MethodSelector.java:31,44-51`); never hidden even for single-style tasks.
   4. **Location** caption + combo (`:441-458`) — hidden if no locations.
4. **Loadout** section (`LoadoutSection` :625-756): gate prompt / boss note / strategy note / worn grid / worn rows / consumables / Est. DPS / Gear cost / bank-age note.
5. **Diagnostics (developer)** — only when `developerMode` (`:216-227`).

**Target order** is variant → location → loadout. **Deltas:**
- The **variant selector sits above the location selector** (✅ matches ordering) — but both are nested *inside* "Where & How", and the recommended-location headline + Why/Method prose print **above** the variant control, so the first thing the user reads in Where&How is a *location answer*, not the variant control.
- The **Attack style** control sits **between** variant and location (variant → style → location), i.e. an extra control the target's 3-step framing doesn't mention.
- Loadout is last (✅).

---

## 7. Dead wiring / dormant features / TODO that would confuse dev testing

1. **`requiredItemId` dropped by the compiler (active bug).** `ModularSlayerDataCompiler.compile` sets `requiredItemName` (`:360`) but **never** `requiredItemId`. Source tasks *do* carry it (e.g. `tasks/aberrant-spectres.json:103` = 4168 Nose peg; `tasks/gargoyles.json` = 4162; `tasks/vampyres.json` = 24699; ~10 tasks). Consequences at runtime: `TaskData.requiredItemId` is always null →
   - Task-section required-item **row never renders** (`SlayerPanel.java:363`).
   - `requiredItemOwned` always null → neutral (`AllInSlayerPlugin.java:280-282`).
   - `unlockSummary` always prints **"needs X"**, never "owns X", because the ownership branch requires a non-null id (`LoadoutAdvisor.java:408`).
2. **Boss varbit-4723 pre-select is dormant in DATA.** Code is live (`readBossTargetVarbit` :405-415; `resolveBossVariantName` :479-493) but **all 33 boss variants have `bossId: null`** (`monsters/boss/*.json`; verified 0 non-null across `monsters/`), so the match at `:487` never succeeds and it always returns the default. The Boss task itself (`tasks/boss.json`, `slayerTargetId:210`) IS reachable, so bosses list/pre-select the default (`boss-abyssal-sire`) but never the live-rolled boss.
3. **Frost Dragons synthetic target id 9001 → effectively unreachable.** `tasks/frost-dragons.json:6` uses `slayerTargetId: 9001` (a placeholder; real varp unverified — see its `taskNotes`). Live varp 395 never equals 9001, so `byTargetVarp` can't hit it (`SlayerDataService.java:81-84`); the only path is `resolveByRuneLiteTaskDb` matching the name "Frost Dragons" (`TaskDetector.java:48-68`), which is unverified. Treat this task as not testable via the normal varp path.
4. **Rich source data silently flattened.** The compiler discards per-task `variantInfo[]`, `locationComparison[]` (with `multicombat/cannonable/safespottable/amount/notes`), `taskNotes`, `unlocks`, `extendedAmount`, `wikiPageId`, `combatLevel` (`ModularSlayerDataCompiler.compile` :344-364 sets only the flat fields). `SourceLocation.safeSpot` is dropped in `resolveLocations` (:437-438). A dev editing these source blocks will see **no runtime change**.
5. **`Recommendation.method` = wiki prose, not the effective style** (`LoadoutAdvisor.java:150`) — overlay + "Method" note show prose while the effective `CombatStyle` is only in the "Attack style" selector. Easy to misread as a bug.
6. **Warn-once malformed-strategy path.** `LoadoutAdvisor.WARNED_MALFORMED_STRATEGY` (:37, :182-199) logs once per variant name; a malformed strategy silently falls back to the stat engine (no user-visible signal).
7. **`SLAYER_COUNT_ORIGINAL=4258`** is a refresh trigger only (`TaskRefreshTrigger.java:23`); never read as state.
8. **Attack-style selector always shown** even for single-style tasks (`SlayerPanel.java:431-438` unconditional; `MethodSelector` has no hide path) — can look like an offered choice where there is effectively one.

---

## 8. GAP LIST (ranked) — current vs target, with code sites

| # | Gap (target vs current) | Severity | Code site(s) to change |
|---|---|---|---|
| **G1** | **Required-item is invisible**: target loadout should reflect task-required gear; `requiredItemId` is dropped so the row never shows and ownership is always unknown. | **High** (active regression) | `data/source/ModularSlayerDataCompiler.java:344-364` (add `task.setRequiredItemId(source.getRequiredItemId())`); consumers already exist (`SlayerPanel.java:363`, `AllInSlayerPlugin.java:280-282`, `LoadoutAdvisor.java:408`). |
| **G2** | **Location is not filtered/suggested BY the variant** (target step 2). Locations are task-level; the recommended location ignores the variant; the variant's `location` is free-text only. | **High** (core target behaviour) | Data: `MonsterVariant`/`variantInfo` carry no rich per-variant locations (`MonsterVariant.java:34`; compiler drops `variantInfo`/`locationComparison` at `ModularSlayerDataCompiler.java:361,444-481`). Logic: `LoadoutAdvisor.chooseLocation` (:315-343) + panel `locationNames`/`reconcileCombo` (`SlayerPanel.java:1277-1288, 579-604`) are variant-agnostic. Requires a data model change (per-variant location links) + advisor/panel filtering. |
| **G3** | **Loadout uses only the *selected* location's Wilderness flag**; a *suggested* location's traits (wilderness/cannon) don't shape the loadout, and cannon never adds cannon/cannonballs to the inventory. Target wants loadout derived from the location's variables. | **Medium** | `LoadoutAdvisor.java:107-109` (wilderness from selected only), `:134-135`; cannon/burst only steer `chooseLocation` (:315-343); no consumable/inventory contribution (`ConsumableSelector` call :128). Inventory list is always empty (`:145`). |
| **G4** | **No login bank scan / no staleness refresh**: target says "scanned on login". Only an active bank-open captures it; a prior snapshot persists but is never refreshed or flagged stale. | **Medium** | `AllInSlayerPlugin.onGameStateChanged` :197-205 (no scan); `InventoryService.onBankChanged` :39-55 is the only writer; `bankLastSeen` :91-107. (Note: bank is only readable while open — a true "login scan" may be infeasible; the realistic fix is staleness UX.) |
| **G5** | **Boss live-roll pre-select dormant**: target implies suggesting the actual assigned boss; code reads varbit 4723 but no variant carries `bossId`. | **Medium** | Data: populate `bossId` on `monsters/boss/*.json` (all null today). Logic already correct: `AllInSlayerPlugin.resolveBossVariantName` :479-493. |
| **G6** | **Inventory (consumables/supplies) is thin**: target wants "equipment + inventory". Inventory list is always empty; only food/combo-food/potion/runes are surfaced as consumables, driven by owned items — no task/location-specific supplies (antifire, cannonballs, teleports, required item). | **Medium** | `LoadoutAdvisor.java:145` (empty inventory), `146-153`; `ConsumableSelector.select` (call :128); `SlayerPanel.addConsumables` :765-814. |
| **G7** | **Panel step order includes an extra "Attack style" control and prints the recommended-location prose above the variant control**, diverging from the clean variant→location→loadout framing. | **Low** (UX) | `SlayerPanel` WhereSection layout :398-459 (reorder / conditionally hide method selector for single-style). |
| **G8** | **`Recommendation.method` shows wiki prose, not the effective style**; potential confusion / not aligned with the "method derived" intent. | **Low** | `LoadoutAdvisor.java:150`; `SlayerPanel.methodText` :1193-1201. |
| **G9** | **Frost Dragons unreachable via varp** (synthetic 9001) — cannot be dev-tested through the normal target-varp path. | **Low** (data/QA) | `tasks/frost-dragons.json:6`; resolution `SlayerDataService.java:81-84` / `TaskDetector.java:48-68`. |
| **G10** | **Rich source data flattened** — editing `variantInfo`/`locationComparison`/`safeSpot`/`taskNotes` has no runtime effect, blocking several of the above targets until the model+compiler carry them. | **Medium** (enabler for G2/G3/G6) | Model: `TaskData`/`SlayerLocation`/`MonsterVariant`; compiler `ModularSlayerDataCompiler.java:344-364, 425-442, 444-481`. |

---

### Verified anchors (for the next dev)
- Data is generated at build: `build.gradle:44-64` (`generateSlayerData` → `ModularSlayerDataCli` → `build/generated/resources/slayer/data/slayer-data.json`); `src/main/resources/data/slayer-data.json` is deleted (git status `D`). Runtime still reads classpath `/data/slayer-data.json` (`SlayerDataService.java:26,42`).
- Modular source tree: `src/main/data/slayer/{masters,tasks,monsters,locations,strategies,weapons}` (43 task files; `masters/duradel.json`).
- Duradel-assigned tasks are those whose `masterIds` contain `"duradel"` (e.g. `tasks/abyssal-demons.json:11-18`, `tasks/boss.json`, `tasks/frost-dragons.json:9-12`).

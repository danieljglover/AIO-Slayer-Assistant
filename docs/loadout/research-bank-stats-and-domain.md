# Research — Bank-gated, stat-driven, weakness-specific loadout engine

Feeds architecture + build of a recommender that: reads owned items (esp. bank), reads each
item's equipment stats, and assembles the best loadout (gear + food + potions + runes) for the
current Slayer task's weakness. No suggestion until the bank has been opened at least once.

Verification legend:
- `[SRC]` = read directly from source at the cited `file:line` (high confidence)
- `[WIKI]` = OSRS Wiki (gameplay fact)
- `[INFERRED]` = my reasoning / recommendation, not a quoted fact

Source snapshots pulled 2026-06-29 from `master`/`main` of each repo. Line numbers are from the
files as fetched (saved under the session scratchpad `…/scratchpad/ref` and `…/scratchpad/rl`).

---

## Part A — `adamgiles1/bank-equipment-stat-filter` teardown

Repo default branch: `main`. Java sources (all under `src/main/java/bankequipmentstatfilter/`):
`BankEquipmentStatFilterPlugin.java` (153 lines), `BankEquipmentStatFilterPanel.java` (237),
`EquipmentStat.java` (28), `ItemWithStat.java` (12), `BankEquipmentStatFilterConfig.java` (18),
plus `…/test/…/BankEquipmentStatFilterPluginTest.java`.

### A.1 How it reads the BANK `[SRC]`
`BankEquipmentStatFilterPlugin.java:45-64`:
```java
@Subscribe
public void onItemContainerChanged(ItemContainerChanged event) {
    if (event.getItemContainer() == client.getItemContainer(InventoryID.BANK)) {
        Item[] bankItems = event.getItemContainer().getItems();
        items = Arrays.stream(bankItems)
            .map(item -> {
                ItemStats stats = itemManager.getItemStats(item.getId(), false);
                ItemComposition composition = itemManager.getItemComposition(item.getId());
                if (stats == null || !stats.isEquipable()) { return null; }
                return new ItemWithStat(item.getId(), stats, composition.getName());
            })
            .filter(Objects::nonNull)
            .toArray(ItemWithStat[]::new);
    }
}
```
- Container identity test: `event.getItemContainer() == client.getItemContainer(InventoryID.BANK)`
  using the legacy enum `net.runelite.api.InventoryID.BANK`.
- Items via `event.getItemContainer().getItems()` → `Item[]` (`Item.getId()`, `Item.getQuantity()`).
- **Thread handling: none explicit** — and none needed. `ItemContainerChanged` is dispatched on the
  client thread, so the inline `getItemComposition`/`getItemStats` (which read game state) are safe
  here `[INFERRED, standard RuneLite event model]`.
- Result cached into the plugin field `ItemWithStat[] items` (`:43`). This is the "owned items"
  snapshot; it only refreshes when the bank container changes (i.e. when the bank is open/seen).

### A.2 The bank-gate `[SRC]`
`BankEquipmentStatFilterPlugin.java:89-94` — the gate is simply "is the cached array null?":
```java
public void bankFilter(EquipmentInventorySlot slot, EquipmentStat statType, boolean allSlots) {
    if (items == null) {
        panel.displayMessage("You need to open your bank once so the plugin can sync with it");
        return;
    }
    ...
}
```
README confirms: bank data persists only while RuneLite stays open and requires opening the bank at
least once per account session. This is exactly the bank-gate behaviour we want.

### A.3 How it gets each item's equipment stats `[SRC]`
- **Note:** this plugin uses the **deprecated** 2-arg overload and the **http-api** `ItemStats`
  type: import `net.runelite.http.api.item.ItemStats` (`:15`), call
  `itemManager.getItemStats(item.getId(), false)` (`:54`). `false` = disallow noted. We should use
  the **newer** 1-arg `getItemStats(int)` returning `net.runelite.client.game.ItemStats` instead
  (see Part B).
- Null + equipable guard: `if (stats == null || !stats.isEquipable())` (`:56`, repeated `:110`).
- Stat extraction `getItemStat(ItemStats, EquipmentStat)` (`:108-146`) — the full real getter list
  on `getEquipment()`:
  `getAstab() getAslash() getAcrush() getAmagic() getArange()` (attack),
  `getDstab() getDslash() getDcrush() getDmagic() getDrange()` (defence),
  `getStr()` (melee str), `getRstr()` (ranged str), `getMdmg()` (magic dmg), `getPrayer()`.
  (No `getRstr`-style accessor used for aspeed/slot/is2h here.)
  - GOTCHA: on the **http-api** `ItemEquipmentStats`, `getMdmg()` returns `int`; on the **client.game**
    one it returns `float` (see Part B). Method returns `int` here only because it's the http-api type.

### A.4 Slot mapping & filtering `[SRC]`
- Slot is an `int` from `getEquipment().getSlot()`, compared to `EquipmentInventorySlot.getSlotIdx()`:
  `…getSlot() == slot.getSlotIdx() || allSlots` (`:98`), then grouped by slot
  (`Collectors.groupingBy(item -> item.getStats().getEquipment().getSlot())`, `:99`) and each group
  sorted descending by the chosen stat (`:101-104`).
- Equipable filter = `ItemStats.isEquipable()` (`:56`). Stat-positive filter = `getItemStat(...) > 0`
  (`:98`).
- Reverse map slotIdx→enum for display: `BankEquipmentStatFilterPanel.java:145-152` iterates
  `EquipmentInventorySlot.values()` matching `getSlotIdx()`.

### A.5 Gotchas observed in the reference `[SRC]/[INFERRED]`
- Uses deprecated http-api `ItemStats`/2-arg `getItemStats` — avoid in our build.
- No handling for the stats map not yet loaded (async remote load — Part B); early calls just yield
  fewer/empty results, not a crash.
- Does not filter members/quest/locked items; relies purely on `isEquipable()`.
- Placeholder/noted bank items: `getItemStats`/`canonicalize` already normalise these (Part B), so
  a placeholder still resolves to its real stats — fine, but quantity for placeholders is 0.
- It ranks by a *single* stat only (README explicitly disclaims "best item" — ignores set bonuses,
  attack speed, 2h-vs-shield). Our engine must add that judgement layer.

---

## Part B — RuneLite core API contract (confirmed against `runelite/runelite` `master`)

### B.1 `ItemManager` `[SRC]` (`runelite-client/.../game/ItemManager.java`)
```java
@Nullable public ItemStats getItemStats(int itemId)                       // :395-406  PREFERRED
@Deprecated @Nullable
public net.runelite.http.api.item.ItemStats getItemStats(int itemId, boolean allowNote) // :415-428
@Nonnull public ItemComposition getItemComposition(int itemId)            // :458-462
public int canonicalize(int itemID)                                       // :467-482
```
- `getItemStats(int)` body (`:398-405`): gets `ItemComposition`; returns `null` if
  `name == null || getNote() != -1` (rejects noted items); else
  `return itemStats.get(canonicalize(itemId));`. Return type is `net.runelite.client.game.ItemStats`.
- `getItemComposition(int)` = `client.getItemDefinition(itemId)` (`:461`) → **requires the client
  thread** `[INFERRED, standard]`. Therefore `getItemStats`/`canonicalize` (which call it) are also
  client-thread-only.
- Stats data load is **async + remote**: field `private Map<Integer, ItemStats> itemStats =
  Collections.emptyMap();` (`:106`); `loadStats()` (`:292-308`) does
  `itemClient.getStats()` → `ImmutableMap.copyOf(...)`; submitted on construction via
  `scheduledExecutorService.submit(this::loadStats)` (`:219`).
  - **GOTCHA:** immediately after client start the map can be empty, so `getItemStats` returns
    `null` for everything until the HTTP fetch completes. Treat null as "unknown, retry later",
    don't cache a permanent "no stats". `[SRC]`
- `canonicalize` (`:467-482`) maps noted → `getLinkedNoteId()`, placeholder → `getPlaceholderId()`,
  and worn variants via `WORN_ITEMS` — so callers don't need to de-note/de-placeholder themselves.

### B.2 `net.runelite.client.game.ItemStats` `[SRC]`
`@Value`. Getters: `isEquipable()` (boolean), `getWeight()` (double), `getGeLimit()` (int),
`getEquipment()` → `ItemEquipmentStats` (null for non-equipment).

### B.3 `net.runelite.client.game.ItemEquipmentStats` `[SRC]` (`:31-56`)
`@Value @Builder`. Fields & generated getters:
| field | getter | notes |
|---|---|---|
| `int slot` | `getSlot()` | maps to `EquipmentInventorySlot.getSlotIdx()` |
| `boolean isTwoHanded` (`@SerializedName("is2h")`) | `isTwoHanded()` | 2h vs shield logic |
| `int astab aslash acrush amagic arange` | `getAstab()`… | **offensive** attack bonuses |
| `int dstab dslash dcrush dmagic drange` | `getDstab()`… | defensive bonuses |
| `int str` | `getStr()` | melee strength |
| `int rstr` | `getRstr()` | ranged strength |
| `float mdmg` | `getMdmg()` | **magic damage % — float here** (int on http-api type) |
| `int prayer` | `getPrayer()` | prayer bonus |
| `int aspeed` | `getAspeed()` | attack speed (ticks) |

### B.4 `EquipmentInventorySlot` `[SRC]` (`runelite-api/.../EquipmentInventorySlot.java:39-52`)
`HEAD(0) CAPE(1) AMULET(2) WEAPON(3) BODY(4) SHIELD(5) ARMS(6) LEGS(7) HAIR(8) GLOVES(9) BOOTS(10)
JAW(11) RING(12) AMMO(13)`; `getSlotIdx()` returns the int. `ItemEquipmentStats.getSlot()` returns
this same index. (For loadout we care about: WEAPON, SHIELD, HEAD, BODY, LEGS, CAPE, AMULET, GLOVES,
BOOTS, RING, AMMO.)

### B.5 `InventoryID` — which container to read `[SRC]`
- Legacy enum `net.runelite.api.InventoryID` (`:48-56`): `INVENTORY(93) EQUIPMENT(94) BANK(95)`.
  Used by `client.getItemContainer(InventoryID.BANK)` (what the reference uses).
- Newer **gameval** `net.runelite.api.gameval.InventoryID` int constants (`:100-102`):
  `INV = 93`, `WORN = 94`, `BANK = 95`. Use with `client.getItemContainer(int)`.
- `[INFERRED]` The legacy enum is being phased out in favour of the gameval int constants — prefer
  `client.getItemContainer(InventoryID.BANK)` from `net.runelite.api.gameval`. For "owned items" we
  read **BANK (95)** + **INV (93)** + **WORN/EQUIPMENT (94)**.

### B.6 Thread rules `[SRC]/[INFERRED]`
- `ItemContainerChanged` (and game events) are dispatched on the **client thread** → safe to call
  `getItemStats`/`getItemComposition` inside `@Subscribe` handlers.
- Off the client thread (e.g. a Swing button handler in the panel), wrap game reads in
  `clientThread.invoke(() -> …)` then push results to the EDT via `SwingUtilities.invokeLater`.
- `ItemManager.getImage(...)` returns an `AsyncBufferedImage` (loaded on the client thread) — use
  `.addTo(label)` for panel icons (reference does this at `Panel:182-183`).

### B.7 BONUS — RuneLite already encodes food/potion effects: `ItemStatChangesService` `[SRC]`
This is significant for Part C: we likely do **not** need a hand-curated food-heal map.
- `net.runelite.client.plugins.itemstats.ItemStatChangesService` is a **public interface** (the
  impl `ItemStatChangesServiceImpl` is package-private but `@Singleton @Inject` — so the interface
  is **injectable** into our plugin):
  ```java
  public interface ItemStatChangesService { Effect getItemStatChanges(int id); }
  ```
- `Effect.calculate(Client client)` → `StatsChanges`; `StatsChanges.getStatChanges()` → `StatChange[]`.
- `StatChange` (`StatChange.java`): `getStat()` (a `Stat`, e.g. `Stats.HITPOINTS`), `getRelative()`
  (change applied now, **capped** to missing HP / boost cap), `getTheoretical()` (**uncapped** full
  value), `getAbsolute()`, `getPositivity()` (`Positivity` enum — BETTER/WORSE/etc.).
- `Stats` (`stats/Stats.java`) exposes `ATTACK DEFENCE STRENGTH HITPOINTS RANGED PRAYER MAGIC …
  RUN_ENERGY` as `Stat` constants.
- Food example `food/Anglerfish.java`: `heals(client) = maxHP/10 + C` with C scaling by HP level and
  `setBoost(true)` (overheal) — i.e. **player-level-aware**, computed live.
- Potion example `potions/SaradominBrew.java`: emits `StatChange`s for `HITPOINTS` (+), `DEFENCE`
  (+) and drains `ATTACK/STRENGTH/RANGED/MAGIC` (−).
- **How we use it:**
  - Food heal amount = the `StatChange` whose `getStat() == Stats.HITPOINTS`, read `getTheoretical()`.
  - Potion → style = inspect which `Stats` are **positively** boosted (ATTACK/STR/DEF ⇒ melee;
    RANGED ⇒ ranged; MAGIC ⇒ magic; PRAYER ⇒ restore).
  - **GOTCHA:** `calculate(client)` reads live levels → call on the **client thread**;
    `getItemStatChanges(id)` returns `null` for non-consumables.

---

## Part C — OSRS domain reference

### C.1 Weakness → relevant offensive stat
**MELEE** `[WIKI/INFERRED]`: three attack types — **stab / slash / crush**. For a monster weak to a
given type, maximise the matching gear **attack** bonus (`astab` / `aslash` / `acrush`) and, in all
melee cases, **Strength** (`str`, drives max hit). A monster's melee weakness = its **lowest melee
defensive bonus** among `dstab/dslash/dcrush` (the wiki "attack styles to use" / "Aggressive/Stab"
recommendation is derived from that). Example: Gargoyles low crush defence ⇒ use crush.

**RANGED** `[WIKI]`: maximise **ranged attack** (`arange`) and **ranged strength** (`rstr`). Ranged
sub-defences exist (standard/heavy/light, e.g. bolts vs arrows vs thrown) — see C.5 data shape.

**MAGIC** `[WIKI]`: maximise **magic attack** (`amagic`) and **magic damage %** (`mdmg`), and match
the **element** to the monster's elemental weakness. Elemental-weakness mechanic (added **29 May
2024**, Project Rebalance): each 1 percentage-point of an NPC's weakness gives **+1% magic accuracy
and +1% magic damage** when you hit it with the matching element. Four elements: **air / water /
earth / fire**. Examples `[WIKI]`: fire giants/ice creatures weak to one element at up to 100%;
dragons/demons/TzHaar commonly **water** (≈40–50%); flying/ghostly things **air**; "tough exterior"
(drakes, gargoyles) **earth**; icy/nature things **fire**. Visible in-game via **Monster
Examine/Inspect** "Other attributes".

**How our engine should determine the attack-type weakness — recommendation `[INFERRED]`:**
- The live client does **not** reliably expose an NPC's defensive bonuses or elemental weakness
  (`NPCComposition` has no per-style defence getters; community plugins ship their own data — see
  C.5). So: **use a curated per-monster data table keyed by NPC id/name** giving
  `{dstab, dslash, dcrush, dmagicDef, drangeDef(+light/heavy/standard), elementalWeakness,
  elementalPercent}`.
- Selection logic: for each style compute a cheap "exploit score" = (your best owned offensive bonus
  for that style) weighted against (the monster's defence for that style); pick the style with the
  best score. A good first cut: **pick the style whose monster-defence is lowest** (melee: pick
  stab/slash/crush by min `d*`; then choose melee vs ranged vs magic by which the player is geared
  for / lowest overall defence). For magic, prefer the element with the highest `elementalPercent`.
- Simpler MVP: store, per task, a **recommended style + (for melee) attack type + (for magic)
  element** directly in our task data (we already maintain a canonical Duradel task list). This
  sidesteps a full defence table; upgrade to score-based later. **(Recommended starting point.)**

### C.2 Food — heals & identification
Heal amounts `[WIKI]` (Hitpoints restored):
| Food | Heals | Note |
|---|---|---|
| Cooked karambwan | 18 | **combo-eats** (same tick as another food) |
| Shark | 20 | |
| Sea turtle | 21 | |
| Manta ray | 22 | |
| Dark crab | 22 | |
| Tuna potato | 22 | |
| Anglerfish | ~22 at 99 HP + **overheal above max** | scales: `maxHP/10 + C` (C 2→13 by HP) |
| Monkfish | 16 | |
| Saradomin brew | `floor(HP*15/100)+2` per dose + **+Defence boost**, **drains** other combat stats | 4 doses |
| Jug of wine | 11 | also **−2 Attack** |
| Pizza (plain) | 14 (7×2 bites) | |
| Cake | 12 (4×3 bites) | |

**Identification `[INFERRED, strongly recommended]`:** use `ItemStatChangesService` (B.7) — call
`getItemStatChanges(id).calculate(client)` and read the `Stats.HITPOINTS` `StatChange`
(`getTheoretical()` = full heal, player-level aware incl. anglerfish/brew). It also tells you Sara
brew's defence boost and stat drains. **Fallback** only if we want to work without the service: a
small curated `id→heal` map for the ~10 common foods above (note: brew/anglerfish are level-scaled
so a static number is approximate). There is no `ItemComposition` "heal" field, so heuristic-by-name
is not viable — use the service or a curated map.

### C.3 Potions — selection by style & identification
Best-per-style `[WIKI]`:
- **Melee:** Super combat potion (Attack+Strength+Defence, +15%+5 each) — top pick; else Super
  strength + Super attack (+ Super defence). Divine super combat = no re-sip needed. Combat potion
  (Att+Str) is the budget option.
- **Ranged:** Ranging potion / Divine ranging potion (+10%+4 Ranged); Bastion potion adds Defence
  (+15%+5) too.
- **Magic:** Magic potion / Divine magic potion (+4 Magic); Battlemage adds Defence; **Imbued
  heart** (+ a % of Magic level) and **Saturated heart** are the strong non-potion boosts.
- **Prayer/utility:** Prayer potion (restores 25%+7 Prayer), Super restore (restores all non-HP
  stats 25%+8) / Sanfew serum; Antipoison/Antidote++ (poison tasks e.g. some kalphites/spiders);
  Stamina potion (run energy).

**Identification `[INFERRED]`:** classify via `ItemStatChangesService` — a potion's `StatChange[]`
with **positive** boosts on `ATTACK/STRENGTH/DEFENCE` ⇒ melee; `RANGED` ⇒ ranged; `MAGIC` ⇒ magic;
`PRAYER` ⇒ restore. This auto-handles new potions without a curated list. **Dose handling:** the 4
dose variants (e.g. `(1)`/`(2)`/`(3)`/`(4)`) are distinct item ids — collapse them with
`net.runelite.client.game.ItemVariationMapping.getVariations(...)` / `.map(id)` so "owns a super
combat potion" matches any dose. A curated "best potion per style" **priority ordering** is still a
judgement call we should hardcode (the *effects* are in the API, the *ranking* is ours).

### C.4 Runes — combat spells by element `[WIKI]`
Standard-spellbook elemental tiers. Each cast needs the per-tier "combat" rune + air runes + the
element's runes:

| Tier (combat rune) | Wind/Air | Water | Earth | Fire | Magic lvl (wind→fire) |
|---|---|---|---|---|---|
| **Bolt** (1× Chaos) | 2 air | 2 air, 2 water | 2 air, 3 earth | 3 air, 4 fire | 17 / 23 / 29 / 35 |
| **Blast** (1× Death) | 3 air | 3 air, 3 water | 3 air, 4 earth | 4 air, 5 fire | 41 / 47 / 53 / 59 |
| **Wave** (1× Blood) | 5 air | 5 air, 7 water | 5 air, 7 earth | 5 air, 7 fire | 62 / 65 / 70 / 75 |
| **Surge** (1× Wrath) | 7 air | 7 air, 10 water | 7 air, 10 earth | 7 air, 10 fire | 81 / 85 / 90 / 95 |

(Wind variants use only air + the combat rune; Fire Surge = 7 air + 10 fire + 1 wrath, base max 24,
lvl 95. Strike tier omitted — too weak for Slayer.)

**Ownership check & recommendation `[INFERRED]`:** map `element → {tier → rune requirement}`. To
pick the spell: highest tier the player can cast given (a) Magic level and (b) runes owned across
bank + inventory + **rune pouch** (read its varbits) — **and** account for an equipped/owned
**elemental staff or combination staff / Tome** which supplies that element's runes unlimited (so
you only need the other runes). Powered staves (Trident, Sang, etc.) bypass runes entirely and may
be the better magic recommendation when owned — treat those as a separate "weapon supplies its own
ammo" case in gear selection.

### C.5 Proof that NPC weakness/defence is curated, not from the API `[SRC]`
`Koitere/monster-stats` ships `src/main/resources/monsterdata.csv` (a bundled wiki-derived table),
loaded by `NPCDataLoader` into `NPCStats` (`NPCStats.java`): fields include `elementalWeakness`,
`elementalPercent`, `crushDefence`, `stabDefence`, `slashDefence`, `standardDefence`, `magicDefence`,
`heavyDefence`, `lightDefence`, `maxHits`, `attackStyles`, `npcID`, `flatArmour`. Sample row:
`Aberrant spectre,Air,50,0,20,20,20,-15,15,15,"2,3,4,5,6,7",…,Magic,0`. This is the proven pattern:
**defensive bonuses + elemental weakness must come from a curated resource**, keyed by NPC id/name,
with multi-id support for variant forms.

---

## Recommendations for our engine

**(a) Read owned-item stats**
- Subscribe to `ItemContainerChanged`; capture **BANK (95)** and also **INV (93)** + **WORN (94)**.
  Build `Map<Integer,Integer>` (canonicalised id → total quantity) + a per-id stats cache.
- Resolve stats with `ItemManager.getItemStats(int)` (the **non-deprecated** overload →
  `net.runelite.client.game.ItemStats`); keep `getEquipment()` per equipable id.
- Do all `ItemManager`/`Effect.calculate` reads on the **client thread**; marshal results to the EDT
  for the panel. Treat `null` stats as "not loaded yet" (async remote map), not "no stats".

**(b) Pick best gear per slot for a weakness** `[INFERRED]`
- Determine the task's style + (melee) attack-type + (magic) element (C.1; start from per-task data).
- Define the primary score per style: melee ⇒ matching `a*` attack bonus, tie-break `str`; ranged ⇒
  `arange` then `rstr`; magic ⇒ `amagic` then `mdmg`. Filter owned equipable items by
  `isEquipable()` & stat `> 0`, group by `getSlot()`, take the max-scoring item per slot.
- Handle WEAPON/SHIELD interplay via `isTwoHanded()` (a 2h weapon vacates the shield slot — compare
  best 2h vs best 1h+shield combined offence). Respect ammo/weapon coupling for ranged/magic.

**(c) Select food / potions / runes from owned items** `[INFERRED]`
- **Food:** `ItemStatChangesService.getItemStatChanges(id).calculate(client)` → `Stats.HITPOINTS`
  `getTheoretical()`; recommend the highest-heal food owned (offer karambwan as combo-eat). Fallback:
  curated `id→heal` map (C.2).
- **Potions:** classify owned potions by positive boosted `Stats` (C.3); pick the best for the task's
  style by our hardcoded preference order; collapse dose variants with `ItemVariationMapping`.
- **Runes (magic tasks):** element→tier→rune map (C.4); choose the highest castable tier given Magic
  level + runes owned (bank+inv+rune pouch) + any element-supplying staff; prefer a powered staff if
  owned.

**(d) The bank-gate** `[INFERRED]`
- Maintain a `boolean bankSeenThisSession`, set true the first time the BANK container is observed.
  Until then, the panel shows "Open your bank once so I can read your gear" and emits **no**
  recommendation (mirrors the reference plugin's gate).

### What must be CURATED vs what comes from the stats API
| Concern | Source |
|---|---|
| Equipment offensive/defensive bonuses, slot, 2h, attack speed | **API** — `ItemManager.getItemStats(int).getEquipment()` |
| Food heal amount (level-aware), potion stat boosts/drains | **API** — `ItemStatChangesService` (curated `id→heal` only as fallback) |
| Potion dose-variant grouping | **API** — `ItemVariationMapping` |
| **NPC defensive bonuses (stab/slash/crush, ranged sub-types, magic) + elemental weakness %** | **CURATED** — per-monster table (proven by `monster-stats/monsterdata.csv`); not in live API |
| Per-task recommended style/attack-type/element (MVP shortcut) | **CURATED** — our task data |
| "Best potion per style" preference ranking | **CURATED** — small hardcoded priority list |
| Element → spell tier → rune requirement map | **CURATED** — table in C.4 |

---

## Sources
- Reference repo: https://github.com/adamgiles1/bank-equipment-stat-filter (files via
  `raw.githubusercontent.com/adamgiles1/bank-equipment-stat-filter/main/src/main/java/bankequipmentstatfilter/…`)
- RuneLite core: `raw.githubusercontent.com/runelite/runelite/master/…` — `ItemManager.java`,
  `game/ItemStats.java`, `game/ItemEquipmentStats.java`, `api/EquipmentInventorySlot.java`,
  `api/InventoryID.java`, `api/gameval/InventoryID.java`, and the `plugins/itemstats/` package
  (`ItemStatChangesService`, `Effect`, `StatChange`, `StatsChanges`, `stats/Stats`, `food/Anglerfish`,
  `potions/SaradominBrew`).
- Curated-data precedent: https://github.com/Koitere/monster-stats (`monsterdata.csv`, `NPCStats.java`).
- OSRS Wiki: Elemental weakness (https://oldschool.runescape.wiki/w/Elemental_weakness), Food
  (https://oldschool.runescape.wiki/w/Food), Potion (https://oldschool.runescape.wiki/w/Potion),
  Bolt/Blast/Wave/Surge spells (https://oldschool.runescape.wiki/w/Surge_spells etc.), Project
  Rebalance — NPC Defence Changes (https://oldschool.runescape.wiki/w/Update:Project_Rebalance_-_NPC_Defence_Changes).

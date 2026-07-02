# Dragons + Bosses task variants (13-task slice)

Source: OSRS Wiki (oldschool.runescape.wiki), `?action=raw` infoboxes. Fetched 2026-06-29.
Covers: Black/Blue/Red/Metal/Frost dragons, Skeletal Wyverns, Dagannoth, Hellhounds, TzHaar, Trolls,
Dark Beasts, Suqahs, Waterfiends. Authoritative variant lists per `duradel-tasks.md` "Alternative(s)" column.

## Conventions (read first)
- **Weakness**: OSRS now ships an explicit in-combat *elemental* weakness (`elementalweaknesstype` +
  `elementalweaknesspercent`). Where the wiki states one it is the authoritative weakness and is reported as
  `MAGIC (<element> <pct>%)`. A second line `lowest-def:` gives the lowest stab/slash/crush/magic/range
  defensive bonus — that is what drives **melee/ranged weapon** selection. Where no elemental type is stated,
  weakness is derived from `lowest-def` only (and the gap is listed in `gaps-a.md`).
- **Defence bonuses**: stab/slash/crush/magic, then range as `light/standard/heavy` (OSRS splits ranged
  defence by ammo weight). `def` = the monster's Defence level.
- **draconic** = takes dragonbane (Dragon hunter lance/crossbow). Per team memory the draconic set = anything
  with the `dragon` attribute (dragons + wyverns + Wyrms + Drakes + Hydras). Flagged `draconic: true` below.
- **demon attribute ≠ demonbane**. Hellhounds and Waterfiends carry the `demon` attribute but the plugin's
  demonbane category is EXACTLY {Abyssal/Black/Greater demons, Nechryael} — so `demonbane: NO` is noted for them.
- **isBoss**: true only for actual bosses. Boss-summoned minions (Vet'ion/Calvar'ion skeleton hounds) are
  marked `isBoss: No` with a "boss-summoned" note.

---

## 1. Black Dragons
Alternatives that count: Baby black dragons, brutal black dragons (Slayer 77), King Black Dragon (boss).
Task: Duradel 10-20 (ext 40-60), Combat 80, DS1 partial. Anti-dragon/dragonfire protection recommended.

### Black dragon
- npc_ids: [252,253,254,255,256,257,258,259,8084,8085] (lvl 227); [7861,7862,7863] (lvl 247)
- combat_level: 227 / 247
- defence_level: 200
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50/50/10 (light/standard/heavy)
- weakness: MAGIC (Water 50%); lowest-def: stab 0 (MELEE-stab) / heavy-ranged 10
- categories: draconic: true (attributes dragon,fiery); not undead/demon/kalphite
- location(s): Taverley Dungeon, Evil Chicken's Lair, Catacombs of Kourend, Myths' Guild basement
- requirement: none (Slayer 1); dragonfire protection advised
- isBoss: No
- notes: HP 190/250. "Immune to weak burns" (fiery).

### Baby black dragon
- npc_ids: [1871,1872] (normal); [7955] (Myths' Guild)
- combat_level: 83
- defence_level: 70
- def_bonuses: stab 0 / slash 50 / crush 50 / magic 40 / range 30/30/5
- weakness: lowest-def: stab 0 -> MELEE (stab). No elemental type stated on infobox (see gaps-a).
- categories: draconic: true (attributes dragon)
- location(s): Taverley Dungeon, Myths' Guild basement
- requirement: none
- isBoss: No
- notes: HP 80. slayxp 80. Does not breathe dragonfire (no shield needed).

### Brutal black dragon
- npc_ids: [7275,8092,8093]
- combat_level: 318
- defence_level: 258
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50/50/10
- weakness: MAGIC (Water 50%); lowest-def: stab 0 (MELEE-stab)
- categories: draconic: true (attributes dragon,fiery)
- location(s): Catacombs of Kourend, Lithkren Vault (DS2)
- requirement: Slayer 77
- isBoss: No
- notes: HP 315. slayxp 346.4.

### King Black Dragon
- npc_ids: [239,2642]
- combat_level: 276
- defence_level: 240
- def_bonuses: stab 40 / slash 90 / crush 90 / magic 80 / range 70/70/40
- weakness: MAGIC (Water 50%); lowest-def: stab 40 / heavy-ranged 40
- categories: draconic: true (attributes dragon,fiery); cat Bosses
- location(s): KBD Lair (Wilderness, lvl 19), via the lever/dungeon
- requirement: boss — counts toward Black dragons task; separate trip (Wilderness)
- isBoss: Yes
- notes: HP 240. slayxp 258.

---

## 2. Blue Dragons
Alternatives: Baby blue dragons, brutal blue dragons, Vorkath (boss). Task: Duradel 110-170, Combat 65, DS1 partial.

### Blue dragon
- npc_ids: [265],[266],[267],[268],[269] (standard); [5878-5882] (task-only); [14103,14104] (Ruins of Tapoyauik)
- combat_level: 111
- defence_level: 95
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50/50/10-15
- weakness: MAGIC (Water 50%; Fire 50% for the Tapoyauik variant); lowest-def: stab 0 (MELEE-stab)
- categories: draconic: true (attributes dragon,fiery)
- location(s): Taverley Dungeon, Ogre Enclave, Catacombs of Kourend, Isle of Souls, Myths' Guild, Ruins of Tapoyauik
- requirement: none
- isBoss: No
- notes: HP 105. slayxp 105.

### Baby blue dragon
- npc_ids: [241,242,243] (normal); [14105,14106] (Ruins of Tapoyauik)
- combat_level: 48
- defence_level: 40
- def_bonuses: stab 0 / slash 50 / crush 50 / magic 40 / range 30/30/5
- weakness: MAGIC (Water 50%; Fire 50% Tapoyauik); lowest-def: stab 0 (MELEE-stab)
- categories: draconic: true (attributes dragon)
- location(s): Taverley Dungeon, Ogre Enclave, Isle of Souls, Ruins of Tapoyauik
- requirement: none
- isBoss: No
- notes: HP 50. slayxp 50. No dragonfire.

### Brutal blue dragon
- npc_ids: [7273] (Catacombs); [13795] (Ruins of Tapoyauik)
- combat_level: 271
- defence_level: 198
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50/50/10
- weakness: MAGIC (50%, element type not explicit on infobox — blue family = Water, see gaps-a); lowest-def: stab 0 (MELEE-stab)
- categories: draconic: true (attributes dragon,fiery)
- location(s): Catacombs of Kourend, Ruins of Tapoyauik
- requirement: none stated (high level)
- isBoss: No
- notes: HP 245. slayxp 257.

### Vorkath
- npc_ids: [8059,8061] (post-quest, lvl 732); [8058,8060] (DS2 fight, lvl 392)
- combat_level: 732 (post-quest) / 392 (Dragon Slayer II encounter)
- defence_level: 214 / 164
- def_bonuses: not split on infobox (uses scaled mechanics) — UNKNOWN per-style (see gaps-a)
- weakness: MAGIC (Fire 40%); the head/standard phase is most reliably damaged by ranged in practice
- categories: draconic: true AND **undead** (attributes dragon,undead,fiery) -> Salve amulet applies; cat Blue Dragons, Zombies, Bosses
- location(s): Ungael (island north of Fremennik, post-Dragon Slayer II)
- requirement: boss — Dragon Slayer II; counts toward Blue dragons task; separate trip
- isBoss: Yes
- notes: HP 750/460. Both DHL (dragon) AND Salve (undead) relevant. Most masters incl. Duradel/turael assign.

---

## 3. Red Dragons
Alternatives: Baby red dragons, brutal red dragons. Task: Duradel 30-65, Combat 68, DS1 partial + "Seeing red" unlock.

### Red dragon
- npc_ids: [247],[248,8075,8078,8079],[249],[250],[251]
- combat_level: 152
- defence_level: 130
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50/50/10
- weakness: MAGIC (Water 50%); lowest-def: stab 0 (MELEE-stab)
- categories: draconic: true (attributes dragon,fiery)
- location(s): Brimhaven Dungeon, Catacombs of Kourend, Forthos Dungeon, Myths' Guild
- requirement: none
- isBoss: No
- notes: HP 140. slayxp 143.4.

### Baby red dragon
- npc_ids: [244,245,246]
- combat_level: 48
- defence_level: 40
- def_bonuses: stab 0 / slash 50 / crush 50 / magic 40 / range 30/30/10
- weakness: MAGIC (Water 50%); lowest-def: stab 0 (MELEE-stab)
- categories: draconic: true (attributes dragon)
- location(s): Brimhaven Dungeon, Myths' Guild
- requirement: none
- isBoss: No
- notes: HP 50. slayxp 50.

### Brutal red dragon
- npc_ids: [7274,8087]
- combat_level: 289
- defence_level: 198
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50/50/10
- weakness: MAGIC (Water 50%); lowest-def: stab 0 (MELEE-stab)
- categories: draconic: true (attributes dragon,fiery)
- location(s): Catacombs of Kourend
- requirement: none stated (high level)
- isBoss: No
- notes: HP 285. slayxp 306.2.

---

## 4. Metal Dragons
Alternatives: Bronze, Iron, Steel, Mithril, Adamant (DS2), Rune (DS2). Task: Duradel 35-45 (ext 150-200), DS1 partial.
All metal dragons: elemental weakness **Earth 50% (magic)**; melee-wise stab is the lowest melee bonus.

### Bronze dragon
- npc_ids: [270,271] (standard); [7253] (Catacombs)
- combat_level: 131 / 143
- defence_level: 112
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90/90/10
- weakness: MAGIC (Earth 50%); lowest-def: stab 0 (MELEE-stab), then magic 30
- categories: draconic: true (attributes dragon,fiery)
- location(s): Brimhaven Dungeon, Catacombs of Kourend
- requirement: none
- isBoss: No
- notes: HP 122. slayxp 125.

### Iron dragon
- npc_ids: [272,273,8080] (standard); [7254] (Catacombs)
- combat_level: 189 / 215
- defence_level: 165 / 185
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90/90/10
- weakness: MAGIC (Earth 50%); lowest-def: stab 0 (MELEE-stab), then magic 30
- categories: draconic: true (attributes dragon,fiery)
- location(s): Brimhaven Dungeon, Catacombs of Kourend
- requirement: none
- isBoss: No
- notes: HP 165/195.

### Steel dragon
- npc_ids: [274],[275] (task-only),[7255] (Catacombs)
- combat_level: 246 / 274
- defence_level: 215 / 235
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90/90/10
- weakness: MAGIC (Earth 50%); lowest-def: stab 0 (MELEE-stab), then magic 30
- categories: draconic: true (attributes dragon,fiery)
- location(s): Brimhaven Dungeon, Catacombs of Kourend
- requirement: none
- isBoss: No
- notes: HP 210/250.

### Mithril dragon
- npc_ids: [2919,8088,8089]
- combat_level: 304
- defence_level: 268
- def_bonuses: stab 0 / slash 100 / crush 70 / magic 30 / range 90/90/20
- weakness: MAGIC (Earth 50%); lowest-def: stab 0 (MELEE-stab), then magic 30
- categories: draconic: true (attributes dragon,fiery)
- location(s): Ancient Cavern
- requirement: none (Barbarian Training to reach Ancient Cavern)
- isBoss: No
- notes: HP 254. slayxp 266.5.

### Adamant dragon
- npc_ids: [8030,8090]
- combat_level: 338
- defence_level: 272
- def_bonuses: stab 20 / slash 110 / crush 85 / magic 30 / range 95/95/65
- weakness: MAGIC (Earth 50%); lowest-def: stab 20 / magic 30
- categories: draconic: true (attributes dragon,fiery)
- location(s): Lithkren Vault (Dragon Slayer II)
- requirement: Dragon Slayer II
- isBoss: No
- notes: HP 295. slayxp 324.5.

### Rune dragon
- npc_ids: [8031,8091]
- combat_level: 380
- defence_level: 276
- def_bonuses: stab 20 / slash 115 / crush 90 / magic 30 / range 95/95/50
- weakness: MAGIC (Earth 50%); lowest-def: stab 20 / magic 30
- categories: draconic: true (attributes dragon,fiery)
- location(s): Lithkren Vault (Dragon Slayer II)
- requirement: Dragon Slayer II
- isBoss: No
- notes: HP 330. slayxp 363.

---

## 5. Frost Dragons (Sailing-locked)
Task: Duradel 70-120 (ext 180-240), Combat 85, **Sailing 87** (DS1 NOT required). No "Alternative(s)" — Frost dragon only.
Wiki page exists with full stats (not thin).

### Frost dragon
- npc_ids: [14922]
- combat_level: 202
- defence_level: 150
- def_bonuses: stab 25 / slash 90 / crush 15 / magic 50 / range 70/50/30
- weakness: MAGIC (Fire 100%); lowest-def: crush 15 (MELEE-crush)
- categories: draconic: true (attributes Dragon,Fiery)
- location(s): UNKNOWN — Sailing content; exact island/dungeon not confirmed from infobox (see gaps-a)
- requirement: Slayer task req Sailing 87 + Combat 85 per duradel-tasks; dragonfire protection advised
- isBoss: No
- notes: HP 230. slayxp 235.5. Strong Fire weakness (100%) — fire spells / fire-aligned setups favoured.

---

## 6. Skeletal Wyverns
Task: Duradel 20-40 (ext 50-70), Slayer 72, Combat 70, Elemental Workshop I. No alternatives.
Elemental/mind/dragonfire/ancient wyvern shield REQUIRED (icy breath otherwise heavy damage).

### Skeletal Wyvern
- npc_ids: [468],[465],[466],[467] (4 poses)
- combat_level: 140
- defence_level: 120
- def_bonuses: stab 140 / slash 90 / crush 90 / magic 80 / range 140/140/140
- weakness: MAGIC (Fire 25%); lowest-def: magic 80 (MAGIC) — also melee slash/crush 90
- categories: draconic: true (attributes dragon) -> takes dragonbane (DHL/DHCB). NOT undead (no undead attribute).
- location(s): Asgarnian Ice Dungeon (deep, south end)
- requirement: Slayer 72, Elemental Workshop I; anti-wyvern/elemental/mind/dragonfire shield
- isBoss: No
- notes: HP 200. slayxp 210. Confirms team memory: skeletal wyverns ARE draconic, NOT undead.

---

## 7. Dagannoth
Alternatives: Dagannoth spawn, Dagannoth fledgeling, Dagannoth Kings (Rex/Prime/Supreme), Reanimated dagannoth.
Task: Duradel 130-200, Combat 75, Horror from the Deep. All share elemental Earth 35% (magic).

### Dagannoth (standard)
- npc_ids: [970,971,972] (lvl74 v1); [973,974,975] (lvl92 v1); [7259] (lvl74 v2); [7260] (lvl92 v2)
- combat_level: 74 / 92
- defence_level: 50 / 71
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0(lvl74) or 50(lvl92) / range 0(lvl74) or 50(lvl92)
- weakness: MAGIC (Earth 35%); lowest-def: melee 0 (MELEE — any style on lvl74; lvl92 still melee 0)
- categories: none (no draconic/demon/undead/kalphite)
- location(s): Lighthouse basement, Catacombs of Kourend, Waterbirth Island
- requirement: Horror from the Deep (for Lighthouse)
- isBoss: No
- notes: HP 70/120.

### Dagannoth spawn
- npc_ids: [3184]
- combat_level: 42
- defence_level: 25
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Earth 35%); lowest-def: all 0 (any style)
- categories: none
- location(s): Waterbirth Island (Dagannoth Kings antechamber)
- requirement: counts toward Dagannoth task
- isBoss: No
- notes: HP 35. slayxp 35.

### Dagannoth fledgeling
- npc_ids: [2264]
- combat_level: 70
- defence_level: 50
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Earth 35%); lowest-def: all 0 (any style)
- categories: none
- location(s): Lighthouse (Horror from the Deep area)
- requirement: counts toward Dagannoth task
- isBoss: No
- notes: HP 100. slayxp 0 (gives no Slayer xp but counts as task kill).

### Dagannoth Rex
- npc_ids: [2267]
- combat_level: 303
- defence_level: 255
- def_bonuses: stab 255 / slash 255 / crush 255 / magic 10 / range 255/255/255
- weakness: MAGIC (magic def 10 — kill with magic); Earth 35% elemental
- categories: cat Bosses, Dagannoth
- location(s): Waterbirth Island, Dagannoth Kings lair
- requirement: boss — counts toward Dagannoth task; separate trip
- isBoss: Yes
- notes: HP 255. Melee attacker; very high melee/ranged def, only magic lands.

### Dagannoth Prime
- npc_ids: [2266]
- combat_level: 303
- defence_level: 255
- def_bonuses: stab 255 / slash 255 / crush 255 / magic 255 / range 10/10/10
- weakness: RANGED (range def 10 — kill with ranged); Earth 35% elemental
- categories: cat Bosses, Dagannoth
- location(s): Waterbirth Island, Dagannoth Kings lair
- requirement: boss — counts toward Dagannoth task; separate trip
- isBoss: Yes
- notes: HP 255. Magic attacker; only ranged lands.

### Dagannoth Supreme
- npc_ids: [2265]
- combat_level: 303
- defence_level: 128
- def_bonuses: stab 10 / slash 10 / crush 10 / magic 255 / range 550/550/550
- weakness: MELEE (melee def 10 — kill with melee); Earth 35% elemental
- categories: cat Bosses, Dagannoth
- location(s): Waterbirth Island, Dagannoth Kings lair
- requirement: boss — counts toward Dagannoth task; separate trip
- isBoss: Yes
- notes: HP 255. Ranged attacker; only melee lands.

### Reanimated dagannoth
- npc_ids: [7033]
- combat_level: N/A (reanimated thrall)
- defence_level: 81
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: lowest-def: all 0 (any style); no elemental type stated
- categories: none
- location(s): Player-created via Arceuus "Reanimate Dagannoth" spell (Dagannoth bones)
- requirement: Arceuus spellbook, Magic for Reanimate Dagannoth; counts toward task
- isBoss: No
- notes: HP 35. slayxp 35.

---

## 8. Hellhounds
Alternatives: Cerberus (Slayer 91, boss), Skeleton Hellhound, Greater Skeleton Hellhound, Reanimated hellhound.
Task: Duradel 130-200, Combat 75. **demon attribute but demonbane: NO** (not in plugin demon set).

### Hellhound
- npc_ids: [104,105,7256] (lvl122); [3133] (lvl127, GWD); [7877] (lvl136)
- combat_level: 122 / 127 / 136
- defence_level: 102 / 106 / 102
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: UNKNOWN — all def bonuses 0, no elemental type stated (any style works; see gaps-a)
- categories: attributes demon -> demonbane: NO (Hellhounds excluded from plugin demon set). Not draconic/undead/kalphite.
- location(s): Stronghold Slayer Cave, Taverley Dungeon, Witchaven Dungeon, Catacombs of Kourend, God Wars Dungeon, Karuulm Slayer Dungeon, Wilderness (lvl 136)
- requirement: none
- isBoss: No
- notes: HP 116/116/150.

### Cerberus
- npc_ids: [5862,5863,5866]
- combat_level: 318
- defence_level: 100
- def_bonuses: stab 50 / slash 100 / crush 25 / magic 65 / range 100/100/100
- weakness: MAGIC (Water 40%); lowest-def: crush 25 (MELEE-crush)
- categories: attributes demon -> demonbane: NO (not in plugin demon set); cat Hellhounds, Bosses
- location(s): Cerberus's Lair (Taverley Dungeon, requires spiked boots area)
- requirement: boss — Slayer 91; counts toward Hellhounds task; separate trip
- isBoss: Yes
- notes: HP 600. slayxp 690.

### Skeleton Hellhound (Tarn's Lair)
- npc_ids: [5054]
- combat_level: 97
- defence_level: 100
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Earth 35%); lowest-def: all 0
- categories: attributes Demon -> demonbane: NO; cat Hellhounds
- location(s): Tarn's Lair (Lair of Tarn Razorlor)
- requirement: counts toward Hellhounds task
- isBoss: No
- notes: HP 55. slayxp 55.

### Greater Skeleton Hellhound (Vet'ion)
- npc_ids: [6614]
- combat_level: 231
- defence_level: 180
- def_bonuses: stab 150 / slash 163 / crush 20 / magic 210 / range 275/275/275
- weakness: MAGIC (Earth 40%); lowest-def: crush 20 (MELEE-crush)
- categories: attributes **undead** (NOT demon, NOT draconic); cat Hellhounds, Skeletons
- location(s): Vet'ion fight (Wilderness, lvl 49)
- requirement: boss-summoned minion (Vet'ion summons them); counts toward Hellhounds task; Wilderness, separate trip
- isBoss: No
- notes: HP 30. slayxp 33. Undead -> Salve applies. Crush-weak.

### Greater Skeleton Hellhound (Calvar'ion)
- npc_ids: [12108]
- combat_level: 139
- defence_level: 110
- def_bonuses: stab 95 / slash 101 / crush 15 / magic 156 / range 184/184/184
- weakness: MAGIC (Earth 40%); lowest-def: crush 15 (MELEE-crush)
- categories: attributes undead; cat Hellhounds, Skeletons
- location(s): Calvar'ion fight (Wilderness — weaker Vet'ion variant)
- requirement: boss-summoned minion; counts toward Hellhounds task; Wilderness, separate trip
- isBoss: No
- notes: HP 30. slayxp 30.

### Reanimated hellhound
- npc_ids: [11463]
- combat_level: N/A (reanimated thrall)
- defence_level: 102
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: lowest-def: all 0 (any style); no elemental type stated
- categories: attributes demon -> demonbane: NO
- location(s): Player-created via Arceuus "Reanimate Hellhound" spell
- requirement: Arceuus spellbook; counts toward task
- isBoss: No
- notes: HP 35. slayxp 35.

---

## 9. TzHaar
Alternatives: TzTok-Jad (boss), TzKal-Zuk (boss if killed before). Task: Duradel 130-199, "Hot stuff" unlock.
City TzHaar (Ket/Xil/Mej/Hur) are the core task monsters. All share elemental Water 40% (magic).

### TzHaar-Ket
- npc_ids: [2173,2174,2175,2176,2177,2178,2179,2187] (lvl149); [7679] (lvl221)
- combat_level: 149 / 221
- defence_level: 120 / 190
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Water 40%); lowest-def: all 0
- categories: none (TzHaar attribute only)
- location(s): Mor Ul Rek (TzHaar City), Karamja volcano
- requirement: none
- isBoss: No
- notes: HP 140/200. Melee attacker, high HP.

### TzHaar-Xil
- npc_ids: [2168,2171] (sword/melee); [2167,2170] (knife/ranged); [2169,2172] (ring)
- combat_level: 133
- defence_level: 100
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Water 40%); lowest-def: all 0
- categories: none
- location(s): Mor Ul Rek
- requirement: none
- isBoss: No
- notes: HP 120. slayxp 120. Ranged + melee variants.

### TzHaar-Mej
- npc_ids: [2154,2155,2156,2157,2158,2159,2160]
- combat_level: 103
- defence_level: 80
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Water 40%); lowest-def: all 0
- categories: none
- location(s): Mor Ul Rek
- requirement: none
- isBoss: No
- notes: HP 100. slayxp 100. Magic attacker.

### TzHaar-Hur
- npc_ids: [2161,2162,2163,2164,2165,2166,7682,7683,7684,7685,7686,7687]
- combat_level: 74
- defence_level: 60
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Water 40%); lowest-def: all 0
- categories: none
- location(s): Mor Ul Rek
- requirement: none
- isBoss: No
- notes: HP 80. slayxp 80. Lowest-level TzHaar, common task fodder.

### TzTok-Jad
- npc_ids: [3127]
- combat_level: 702
- defence_level: 480
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0/0/0
- weakness: MAGIC (Water 40%); lowest-def: all 0 (very high Defence LEVEL 480)
- categories: cat TzHaar
- location(s): TzHaar Fight Cave (final wave)
- requirement: boss — counts toward TzHaar task; separate trip (Fight Cave)
- isBoss: Yes
- notes: HP 250. slayxp 25250.

### TzKal-Zuk
- npc_ids: [7706]
- combat_level: 1400
- defence_level: 260
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 350 / range 100/100/100
- weakness: lowest-def: melee 0 (MELEE) — magic def 350 high; Water 40% elemental stated
- categories: cat TzHaar
- location(s): The Inferno (final wave)
- requirement: boss — counts toward TzHaar task ONLY if killed before getting the task; separate trip (Inferno)
- isBoss: Yes
- notes: HP 1200. slayxp 101890.

---

## 10. Trolls
Alternatives: Mountain troll, ice troll, Troll general, Dad, Ice Troll King, Arrg, Reanimated troll.
Task: Duradel 130-200, Combat 60. Trolls generally have huge magic/ranged def -> MELEE.

### Mountain troll
- npc_ids: [936,937,938,939,940,941,942] (lvl69); [4143] (lvl71)
- combat_level: 69 / 71
- defence_level: 40 / 25
- def_bonuses: stab 0 / slash 0 / crush 10 / magic 200 / range 200/40/200 (light/standard/heavy)
- weakness: MAGIC (Fire 50% / Earth 50% — two types stated); lowest-def: stab/slash 0 (MELEE-stab/slash)
- categories: none (Troll attribute only)
- location(s): Death Plateau, Troll Stronghold approach, Trollheim, Keldagrim entrance
- requirement: none
- isBoss: No
- notes: HP 90. slayxp 90.

### Ice troll
- npc_ids: [650,652,653,701,703,704] (lvl120); [651,654,702,705] (lvl121); [649,700] (lvl123); [648,699] (lvl124)
- combat_level: 120 / 121 / 123 / 124
- defence_level: 120 / 110 / 100 / 80
- def_bonuses: stab 30 / slash 60 / crush 30 / magic 0 / range 0/0/0
- weakness: MAGIC (Fire 100%); lowest-def: magic 0 / range 0 — note Fire 100% means fire spells excel
- categories: none
- location(s): Trollweiss/Ice Mountain area, Jatizso/Neitiznot (Fremennik Isles), God Wars Dungeon approach
- requirement: none
- isBoss: No
- notes: HP 100/90/80/80. Despite magic/range def 0, Fire 100% weakness makes fire magic strong.

### Troll general
- npc_ids: [4120] (sword); [4121] (grey hammer); [4122] (brown hammer)
- combat_level: 113
- defence_level: 40
- def_bonuses: stab 35 / slash 60 / crush 35 / magic 200 / range 200/120/200
- weakness: MAGIC (Earth 20%); lowest-def: stab/crush 35 (MELEE-stab/crush)
- categories: none
- location(s): Trollheim, Troll Stronghold
- requirement: none (some are quest-related)
- isBoss: No
- notes: HP 140. slayxp 150.5.

### Dad
- npc_ids: [4130]
- combat_level: 101
- defence_level: 50
- def_bonuses: stab 25 / slash 25 / crush 40 / magic 200 / range 200/200/200
- weakness: MAGIC (Earth 40%); lowest-def: stab/slash 25 (MELEE-stab/slash)
- categories: none
- location(s): Troll Stronghold (Death Plateau), arena
- requirement: counts toward Trolls task (a named troll mini-boss; respawns)
- isBoss: No
- notes: HP 120. slayxp 126.

### Ice Troll King
- npc_ids: [5822]
- combat_level: 122
- defence_level: 80
- def_bonuses: stab 45 / slash 45 / crush 45 / magic 2000 / range 2000/2000/2000
- weakness: MELEE (magic/ranged def 2000 — only melee viable); Fire 35% elemental stated
- categories: none; named mini-boss
- location(s): Jatizso (post The Fremennik Isles), respawns
- requirement: The Fremennik Isles to access; counts toward Trolls task
- isBoss: Yes
- notes: HP 150. slayxp 161. Magic/ranged effectively immune (def 2000) -> must melee.

### Arrg
- npc_ids: [643,642]
- combat_level: 113
- defence_level: 40
- def_bonuses: stab 35 / slash 60 / crush 35 / magic 200 / range 200/200/200
- weakness: MAGIC (Earth 50%); lowest-def: stab/crush 35 (MELEE-stab/crush)
- categories: none; named mini-boss
- location(s): Troll Stronghold (Death Plateau) — Burthorpe troll arena
- requirement: counts toward Trolls task; Death Plateau/Troll Stronghold quest context
- isBoss: Yes
- notes: HP 140. slayxp 150.5.

### Reanimated troll
- npc_ids: [7030]
- combat_level: N/A (reanimated thrall)
- defence_level: 40
- def_bonuses: stab 0 / slash 0 / crush 10 / magic 200 / range 200/200/200
- weakness: lowest-def: stab/slash 0 (MELEE-stab/slash); no elemental type stated
- categories: none
- location(s): Player-created via Arceuus "Reanimate Troll" spell
- requirement: Arceuus spellbook; counts toward task
- isBoss: No
- notes: HP 35. slayxp 35.

---

## 11. Dark Beasts
Alternatives: none in duradel-tasks column, but Night beast is the SUPERIOR. Task: Duradel 10-20 (ext 110-135),
Slayer 90, Combat 90, Mourning's End Part II (started).

### Dark beast
- npc_ids: [4005] (Mourner Tunnels); [7250] (Catacombs of Kourend)
- combat_level: 182
- defence_level: 120
- def_bonuses: stab 30 / slash 40 / crush 100 / magic 90 / range 100/100/100
- weakness: MAGIC (Earth 60%); lowest-def: stab 30 (MELEE-stab)
- categories: none (no draconic/demon/undead/kalphite)
- location(s): Mourner Tunnels (Temple of Light), Catacombs of Kourend, Iorwerth Dungeon (Prifddinas)
- requirement: Mourning's End Part II started (for Mourner Tunnels)
- isBoss: No
- notes: HP 220. slayxp 225.4. Uses a Dark bow-style ranged attack.

### Night beast (superior)
- npc_ids: [7409]
- combat_level: 374
- defence_level: 220
- def_bonuses: stab 75 / slash 80 / crush 200 / magic 190 / range 200/200/200
- weakness: lowest-def: stab 75 (MELEE-stab); no elemental type stated on infobox
- categories: none; cat Dark Beasts (superior)
- location(s): wherever Dark beasts spawn (superior slayer creature)
- requirement: superior — "Bigger and Badder" unlock; counts toward Dark Beasts task
- isBoss: No
- notes: HP 550. slayxp 6462.

---

## 12. Suqahs
Task: Duradel 60-90 (ext 186-250), Combat 85, Lunar Diplomacy partial. No alternatives.

### Suqah
- npc_ids: [787],[788],[789],[790],[791],[792],[793] (7 poses)
- combat_level: 111
- defence_level: 95
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 90 / range 50/30/50 (light/standard/heavy)
- weakness: RANGED (standard ammo def 30 lowest); Earth 20% elemental stated; lowest melee = stab 50
- categories: none
- location(s): Lunar Isle (all over the island surface)
- requirement: Lunar Diplomacy (partial) to access Lunar Isle
- isBoss: No
- notes: HP 105. slayxp 107.6. Commonly meleed in practice (stab), but data low-point is standard-ranged def 30.

---

## 13. Waterfiends
Task: Duradel 130-200, Combat 75, Barbarian Training (pyre ships). No alternatives. **demon attribute but demonbane: NO**.

### Waterfiend
- npc_ids: [2916] (normal); [2917] (Slayer task)
- combat_level: 115
- defence_level: 128
- def_bonuses: stab 100 / slash 100 / crush 10 / magic 100 / range 100/100/20
- weakness: MAGIC (Earth 100%); lowest-def: crush 10 (MELEE-crush), then heavy-ranged 20
- categories: attributes demon -> demonbane: NO (Waterfiends excluded from plugin demon set). Not draconic/undead.
- location(s): Ancient Cavern (Barbarian Training), Chaos Tunnels-style spawns, Catacombs not applicable
- requirement: Barbarian Training (to reach Ancient Cavern); Konar/Duradel assign
- isBoss: No
- notes: HP 128. slayxp 128. Crush-weak (crush 10) — bludgeon/maul favoured in melee; Earth 100% magic strong.

---

## Per-task variant counts
1. Black Dragons — 4 (Black, Baby black, Brutal black, King Black Dragon*)
2. Blue Dragons — 4 (Blue, Baby blue, Brutal blue, Vorkath*)
3. Red Dragons — 3 (Red, Baby red, Brutal red)
4. Metal Dragons — 6 (Bronze, Iron, Steel, Mithril, Adamant, Rune)
5. Frost Dragons — 1 (Frost dragon)
6. Skeletal Wyverns — 1 (Skeletal Wyvern)
7. Dagannoth — 7 (Dagannoth, spawn, fledgeling, Rex*, Prime*, Supreme*, Reanimated)
8. Hellhounds — 6 (Hellhound, Cerberus*, Skeleton Hellhound, Greater Skeleton Hellhound x2 [Vet'ion/Calvar'ion], Reanimated)
9. TzHaar — 6 (Ket, Xil, Mej, Hur, TzTok-Jad*, TzKal-Zuk*)
10. Trolls — 7 (Mountain, Ice, Troll general, Dad, Ice Troll King*, Arrg*, Reanimated)
11. Dark Beasts — 2 (Dark beast, Night beast [superior])
12. Suqahs — 1 (Suqah)
13. Waterfiends — 1 (Waterfiend)
(* = boss / boss-summoned, separate trip)
Total enumerated variants: 49.

# Dragon-task variants

Source: OSRS Wiki (https://oldschool.runescape.wiki), Infobox Monster raw data fetched 2026-06-29.
Covers the 5 Duradel DRAGON slayer tasks. Per team rule the DRACONIC set (dragons + wyverns + Wyrms + Drakes + Hydras) all take dragonbane (Dragon hunter lance/crossbow, dragonbane bolts); every monster here has `attributes = dragon` so `dragon: true` and dragonbane applies (DHL/dragonbane only excludes revenants and Elvarg).

Notes on defensive bonuses: OSRS replaced the single ranged-defence stat with three (light / standard / heavy). `range` below = standard ranged defence; light/heavy noted when they differ materially. `dstab/dslash/dcrush` are the three melee defences; `dmagic` is magic defence. Elemental weakness is a separate multiplier applied to spells of that element regardless of magic defence.

---

## Black dragons
Assigned set (dragonbane-eligible): Black dragon, Baby black dragon, Brutal black dragon (Slayer 77), King Black Dragon (boss — separate trip).

### Black dragon (Level 227)
- npc_ids: [252,253,254,255,256,257,258,259,8084,8085]
- combat_level: 227
- defence_level: 200
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50 (light 50, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50% (Water spells)
- categories: dragon, fiery
- location(s): Taverley Dungeon, Evil Chicken's Lair, Catacombs of Kourend, Myths' Guild basement
- requirement: none
- notes: HP 190. Max hit 21 (Melee), 50 (Dragonfire). Anti-dragon protection strongly advised.

### Black dragon (Level 247)
- npc_ids: [7861,7862,7863]
- combat_level: 247
- defence_level: 200
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50 (light 50, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon, fiery
- location(s): Catacombs of Kourend (stronger task-area variant)
- requirement: none
- notes: HP 250. Max hit 22 (Melee), 50+ (Dragonfire). Same defensive profile as Level 227, higher HP/combat.

### Baby black dragon
- npc_ids: [1871,1872] (Normal); [7955] (Myths' Guild)
- combat_level: 83
- defence_level: 70
- def_bonuses: stab 0 / slash 50 / crush 50 / magic 40 / range 30 (light 30, heavy 5)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon
- location(s): Taverley Dungeon (Normal), Myths' Guild basement
- requirement: none
- notes: HP 80. Max hit 11 (Melee). No dragonfire breath. Two versions share stats; ids differ by location.

### Brutal black dragon
- npc_ids: [7275,8092,8093]
- combat_level: 318
- defence_level: 258
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50 (light 50, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon, fiery
- location(s): Catacombs of Kourend
- requirement: Slayer level 77
- notes: HP 315. Max hit 29 (Melee/Magic), 50 (Dragonfire). Counts toward Black dragon task.

### King Black Dragon
- npc_ids: [239,2642]
- combat_level: 276
- defence_level: 240
- def_bonuses: stab 40 / slash 90 / crush 90 / magic 80 / range 70 (light 70, heavy 40)
- weakness: MELEE, best stab (def 40, tied with heavy ranged 40); element Water 50%
- categories: dragon, fiery, boss
- location(s): King Black Dragon Lair (Wilderness, via lever; also Nightmare Zone)
- requirement: boss — separate trip
- notes: HP 240. Max hit 25 (Melee), 65 (Dragonfire — highest standard dragonfire). aka KBD.

---

## Blue dragons
Assigned set: Blue dragon, Baby blue dragon, Brutal blue dragon, Vorkath (boss — separate trip).

### Blue dragon
- npc_ids: [265,266,267,268,269] (appearance versions 1-5, identical stats)
- combat_level: 111
- defence_level: 95
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50 (light 50, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon, fiery
- location(s): Taverley Dungeon, Ogre Enclave, Catacombs of Kourend, Isle of Souls Dungeon, Myths' Guild basement, Ruins of Tapoyauik
- requirement: none
- notes: HP 105. Max hit 10 (Slash), 50 (Dragonfire). 5 graphical versions all share stats.

### Baby blue dragon
- npc_ids: [241,242,243] (versions 1-3, identical stats)
- combat_level: 48
- defence_level: 40
- def_bonuses: stab 0 / slash 50 / crush 50 / magic 40 / range 30 (light 30, heavy 5)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon
- location(s): Taverley Dungeon, Ruins of Tapoyauik
- requirement: none
- notes: HP 50. Max hit 5 (Melee). No dragonfire.

### Brutal blue dragon
- npc_ids: [7273] (Catacombs of Kourend); [13795] (Ruins of Tapoyauik)
- combat_level: 271
- defence_level: 198
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50 (light 50, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50% (page also lists Fire as secondary type)
- categories: dragon, fiery
- location(s): Catacombs of Kourend, Ruins of Tapoyauik
- requirement: none (no Slayer level requirement)
- notes: HP 245. Max hit 21 (Melee/Magic), 50 (Dragonfire).

### Vorkath (Post-quest)
- npc_ids: [8059,8061]
- combat_level: 732
- defence_level: 214
- def_bonuses: stab 26 / slash 108 / crush 108 / magic 240 / range 26 (light 26, heavy 26)
- weakness: MELEE/RANGED, best stab or ranged (both ~26); element Fire 40%. Magic def 240 — never use magic.
- categories: dragon, undead, fiery, boss
- location(s): Ungael (north of Fossil Island)
- requirement: boss — separate trip; requires Dragon Slayer II completed
- notes: HP 750. Max hits: 30 Magic / 32 Ranged / 32 Melee / 80 Dragonfire / 121 Dragonfire-bomb special / 41 rapid-fire special. Undead → salve amulet works.

### Vorkath (Dragon Slayer II encounter)
- npc_ids: [8058,8060]
- combat_level: 392
- defence_level: 164
- def_bonuses: stab 66 / slash 126 / crush 126 / magic 204 / range 80 (light 80, heavy 80)
- weakness: MELEE, best stab (def 66, lowest melee); element Fire 40%. Magic def 204 — avoid magic.
- categories: dragon, undead, fiery, boss
- location(s): Ungael (one-time quest fight during Dragon Slayer II)
- requirement: boss — separate trip; quest encounter (not a repeatable slayer kill)
- notes: HP 460. Weaker quest version; the post-quest version is the one that counts for Blue dragon tasks.

---

## Red dragons
Assigned set: Red dragon, Baby red dragon, Brutal red dragon.

### Red dragon
- npc_ids: [247,248,8075,8078,8079,249,250,251] (appearance versions 1-5)
- combat_level: 152
- defence_level: 130
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50 (light 50, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon, fiery
- location(s): Brimhaven Dungeon, Catacombs of Kourend, Forthos Dungeon, Myths' Guild basement, Ruins of Tapoyauik
- requirement: none
- notes: HP 140. Max hit 14 (Melee), 50 (Dragonfire). 5 graphical versions share stats.

### Baby red dragon
- npc_ids: [244,245,246] (versions 1-3)
- combat_level: 48
- defence_level: 40
- def_bonuses: stab 0 / slash 50 / crush 50 / magic 40 / range 30 (light 30, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon
- location(s): Brimhaven Dungeon
- requirement: none
- notes: HP 50. Max hit 5 (Melee). No dragonfire.

### Brutal red dragon
- npc_ids: [7274,8087]
- combat_level: 289
- defence_level: 198
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 60 / range 50 (light 50, heavy 10)
- weakness: MELEE, best stab (def 0); element Water 50%
- categories: dragon, fiery
- location(s): Catacombs of Kourend
- requirement: none (no Slayer level requirement)
- notes: HP 285. Max hit 22 (Melee/Magic), 50 (Dragonfire).

---

## Metal dragons
Assigned set (Metal dragons task): Bronze, Iron, Steel, Mithril (needs Barbarian Training started), Adamant (DS2), Rune (DS2).

### Bronze dragon (Standard)
- npc_ids: [270,271]
- combat_level: 131
- defence_level: 112
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90 (light 90, heavy 10)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Brimhaven Dungeon (incl. cannonable slayer-only area)
- requirement: none
- notes: HP 122. Max hit 12 (Melee), 50 (Dragonfire). Magic def low (30) but stab 0 is lowest.

### Bronze dragon (Catacombs of Kourend)
- npc_ids: [7253]
- combat_level: 143
- defence_level: 112
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90 (light 90, heavy 10)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Catacombs of Kourend
- requirement: none
- notes: HP 122. Max hit 14 (Melee). Stronger combat-level variant of standard bronze.

### Iron dragon (Standard)
- npc_ids: [272,273,8080]
- combat_level: 189
- defence_level: 165
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90 (light 90, heavy 10)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Brimhaven Dungeon
- requirement: none
- notes: HP 165. Max hit 17 (Melee), 50 (Dragonfire). Drops draconic visage.

### Iron dragon (Catacombs of Kourend)
- npc_ids: [7254]
- combat_level: 215
- defence_level: 185
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90 (light 90, heavy 10)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Catacombs of Kourend
- requirement: none
- notes: HP 195. Max hit 19 (Melee). Stronger Catacombs variant.

### Steel dragon (Level 246)
- npc_ids: [274]
- combat_level: 246
- defence_level: 215
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90 (light 90, heavy 10)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Brimhaven Dungeon
- requirement: none
- notes: HP 210. Max hit 22 (Melee), 50 (Dragonfire).

### Steel dragon (Level 246, Task only)
- npc_ids: [275]
- combat_level: 246
- defence_level: 215
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90 (light 90, heavy 10)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Brimhaven Dungeon (task-only spawn)
- requirement: none
- notes: HP 210. Separate NPC id used for slayer-task spawns; identical stats to Level 246.

### Steel dragon (Level 274, Catacombs of Kourend)
- npc_ids: [7255]
- combat_level: 274
- defence_level: 235
- def_bonuses: stab 0 / slash 70 / crush 70 / magic 30 / range 90 (light 90, heavy 10)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Catacombs of Kourend
- requirement: none
- notes: HP 250. Max hit 24 (Melee). Stronger Catacombs variant.

### Mithril dragon
- npc_ids: [2919,8088,8089]
- combat_level: 304
- defence_level: 268
- def_bonuses: stab 0 / slash 100 / crush 70 / magic 30 / range 90 (light 90, heavy 20)
- weakness: MELEE, best stab (def 0); element Earth 50%
- categories: dragon, fiery
- location(s): Ancient Cavern
- requirement: Barbarian Training (Firemaking/access) — Ancient Cavern requires starting Barbarian Training (Otto Godblessed); counts toward Metal dragons task
- notes: HP 254. Max hit 28 (Stab Melee), 18 (Magic), 18 (Magical ranged), 50 (Dragonfire). Note slash def 100.

### Adamant dragon
- npc_ids: [8030,8090]
- combat_level: 338
- defence_level: 272
- def_bonuses: stab 20 / slash 110 / crush 85 / magic 30 / range 95 (light 95, heavy 65)
- weakness: MELEE, best stab (def 20, lowest); element Earth 50%
- categories: dragon, fiery
- location(s): Lithkren Vault
- requirement: Dragon Slayer II completed
- notes: HP 295. Max hit 29 (Melee), 20 (Ranged), 20 (Magic), 50 (Dragonfire), 25 (poison special). Ranged special uses ruby-bolt Blood Forfeit (keep HP low).

### Rune dragon
- npc_ids: [8031,8091]
- combat_level: 380
- defence_level: 276
- def_bonuses: stab 20 / slash 115 / crush 90 / magic 30 / range 95 (light 95, heavy 50)
- weakness: MELEE, best stab (def 20, lowest); element Earth 50%
- categories: dragon, fiery
- location(s): Lithkren Vault
- requirement: Dragon Slayer II completed
- notes: HP 330. Max hit 29 (Melee), 31 (Ranged), 26 (Magic), 40 (ranged special), 50 (Dragonfire). Most profitable metal dragon.

---

## Frost dragons
NOT previously in the dataset — Sailing task (released 19 Nov 2025). Assigned by Nieve and Duradel.

### Frost dragon
- npc_ids: [14922]
- combat_level: 202
- defence_level: 150
- def_bonuses: stab 25 / slash 90 / crush 15 / magic 50 / range 50 (light 70, standard 50, heavy 30)
- weakness: MELEE, best crush (def 15, lowest); element Fire 100% (full damage bonus to Fire spells)
- categories: dragon, fiery
- location(s): Frost dragon area (Sailing content, ice region)
- requirement: none (Slayer level field blank on wiki); Sailing access required to reach
- notes: HP 230. Max hit 16 (Stab), 50 (Dragonfire). Attack speed 4, size 4, aggressive. Slayer XP 235.5. Elemental weakness Fire 100% is unusually high — Fire spells / fire-based methods are strongly favoured despite magic def 50. Best melee is crush (15), unlike most dragons (stab).

---

## Gaps

No stat fields were left UNKNOWN — every monster's Infobox Monster on the OSRS wiki provided combat level(s), NPC id(s), defence level, full defensive bonuses (incl. light/standard/heavy ranged), attributes, and elemental weakness.

Minor caveats (not stat gaps):
- Brutal blue dragon: wiki lists Water as primary elemental weakness type and Fire as a secondary type with a single 50% value; treated as Water 50%.
- Slayer-level requirement: only Brutal black dragon has an explicit `slaylvl = 77`. Brutal blue/red, Mithril, Adamant, Rune, Steel and Frost dragons have a blank/absent `slaylvl` field (no Slayer level needed to attack; access gating is via quests/areas as noted).
- Locations are summarised from each page's body text plus version labels; not every minor spawn is enumerated.

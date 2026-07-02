# Demon-task variants

Source: oldschool.runescape.wiki Infobox Monster wikitext (`?action=raw`) for each NPC, plus `Slayer_task/*` pages for the assigned-monster / alternatives lists. Fetched 2026-06-29. Defence bonuses: stab/slash/crush = melee, magic = dmagic, range = dlight/dstandard/dheavy (equal unless noted). Weakness = lowest defensive bonus + any stated elemental weakness. All four task base monsters carry the `demon` attribute.

## Abyssal demons

### Abyssal demon (Standard)
- npc_ids: [415, 416]
- combat_level: 124
- defence_level: 135
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 0 / range 20
- weakness: MAGIC (also demonbane weapons)
- categories: demon
- location(s): Abyssal Area, Slayer Tower (3rd floor + task-only basement)
- requirement: 85 Slayer; Priest in Peril (or Fairytale II) for Abyss access
- notes: Base task monster. Max hit 8 (melee stab). Magic def 0 -> barraged.

### Abyssal demon (Catacombs of Kourend)
- npc_ids: [7241]
- combat_level: 124
- defence_level: 135
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 0 / range 20
- weakness: MAGIC (also demonbane)
- categories: demon
- location(s): Catacombs of Kourend
- requirement: 85 Slayer
- notes: Same stats as Standard; different NPC id. Counts toward task.

### Abyssal demon (Wilderness Slayer Cave)
- npc_ids: [11239]
- combat_level: 124
- defence_level: 135
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 0 / range 20
- weakness: MAGIC (also demonbane)
- categories: demon
- location(s): Wilderness Slayer Cave
- requirement: 85 Slayer
- notes: Wilderness variant (has Wilderness Slayer tertiary drops). Counts toward task.

### Greater abyssal demon
- npc_ids: [7410]
- combat_level: 342
- defence_level: 240
- def_bonuses: stab 50 / slash 50 / crush 50 / magic 0 / range 50
- weakness: MAGIC (also demonbane)
- categories: demon
- location(s): Anywhere abyssal demons spawn (replaces a kill)
- requirement: 85 Slayer + Bigger and Badder unlocked
- notes: SUPERIOR slayer monster. Max hit 27, far more accurate. Counts toward task.

### Abyssal Sire
- npc_ids: [5886, 5887, 5888 (Phase 1); 5889, 5890 (Phase 2); 5891 (Phase 3 s1); 5908 (Phase 3 s2)]
- combat_level: 350
- defence_level: 250
- def_bonuses (Phase 1-3): stab 40 / slash 60 / crush 50 / magic 20 / range 60
- def_bonuses (Phase 3 stage 2): stab 20 / slash 30 / crush 25 / magic -40 / range 30
- weakness: MAGIC (lowest def both states; also demonbane)
- categories: demon
- location(s): Abyssal Nexus
- requirement: 85 Slayer; boss — separate trip
- notes: BOSS. Multi-phase; magic def drops to -40 once stunned in stage 2. Max hit 66 melee / 96 with explosion.

### Reanimated abyssal
- npc_ids: [7038]
- combat_level: N/A (per infobox)
- defence_level: 135
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 0 / range 20
- weakness: MAGIC
- categories: demon
- location(s): Wherever cast (Arceuus Reanimate Abyssal spell)
- requirement: 85 Magic, Arceuus spellbook; an Abyssal demon's ensouled head
- notes: Listed as an alternative, but reanimated monsters do NOT award Slayer XP / do NOT count toward the task. Same stats as Abyssal demon. Combat level listed as N/A on wiki.

## Black demons

### Black demon (Level 172)
- npc_ids: [240, 2048, 2049, 2050, 2051, 2052, 5874, 5875, 5876, 5877]
- combat_level: 172
- defence_level: 152
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Standard — Taverley Dungeon, Brimhaven Dungeon, Chasm of Fire, Edgeville/Wilderness dungeons, etc.
- requirement: none (slayer task)
- notes: Base task monster. Max hit 16.

### Black demon (Level 178)
- npc_ids: [7243]
- combat_level: 178
- defence_level: 175
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -5 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Catacombs of Kourend
- requirement: none
- notes: Catacombs variant. Max hit 16.

### Black demon (Level 184)
- npc_ids: [7242]
- combat_level: 184
- defence_level: 162
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Catacombs of Kourend
- requirement: none
- notes: Catacombs variant. Max hit 17.

### Black demon (Level 188)
- npc_ids: [7874, 7875, 7876]
- combat_level: 188
- defence_level: 152
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Wilderness Slayer Cave
- requirement: none
- notes: Wilderness variant (Wilderness Slayer tertiary drops). Max hit 17.

### Demonic gorilla
- npc_ids: [7144, 7145, 7146, 7147, 7148, 7149, 7152]
- combat_level: 275
- defence_level: 200
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 20 / range 0
- weakness: MELEE / RANGED (def 0, tied), element Water (35%)
- categories: demon
- location(s): Crash Site Cavern
- requirement: Monkey Madness II; needs two combat styles (prayer-switches). Only when task NOT assigned by Krystilia/Konar.
- notes: Switches Protect prayers and retaliates style-for-style; players alternate ranged/magic/melee. Max hit 31 (40 special). Counts toward black demons.

### Balfrug Kreeyath
- npc_ids: [3132]
- combat_level: 151
- defence_level: 153
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 10 / range 0
- weakness: MELEE / RANGED (def 0, tied), element Water (25%)
- categories: demon
- location(s): God Wars Dungeon — K'ril Tsutsaroth's chamber (Zamorak)
- requirement: GWD access (60 KC etc.); boss bodyguard
- notes: K'ril bodyguard, a black demon — counts toward black demons task. Max hit 16.

### Porazdir
- npc_ids: [7515, 7860]
- combat_level: 235
- defence_level: 100
- def_bonuses: stab 200 / slash 200 / crush 200 / magic -60 / range 200
- weakness: MAGIC (only harmable by Flames of Zamorak), element none stated
- categories: demon
- location(s): Wilderness (roams)
- requirement: Zamorak staff / Staff of the Dead / toxic SotD / Accursed sceptre (to cast Flames of Zamorak)
- notes: Tsutsaroth demon; can ONLY be damaged by Flames of Zamorak. Counts toward black demons. Max hit 43 magic / 16 melee.

### Skotizo
- npc_ids: [7286]
- combat_level: 321
- defence_level: 200
- def_bonuses: stab 80 / slash 80 / crush 80 / magic 80 / range 130
- weakness: MELEE / MAGIC (def 80, tied; range higher), element Water (40%); demonbane (Arclight)
- categories: demon
- location(s): Skotizo's Lair, beneath Catacombs of Kourend
- requirement: Dark totem (1 per attempt); boss — separate trip
- notes: BOSS. Counts toward BOTH black demons and greater demons. Max hit 38. Demonbane greatly recommended.

## Greater demons

### Greater demon (Level 92)
- npc_ids: [2025, 2026, 2027, 2028, 2029, 2030, 2031, 2032]
- combat_level: 92
- defence_level: 81
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Standard — Brimhaven Dungeon, Catacombs, Chasm of Fire, Wilderness, etc.
- requirement: none (some level-92 spawns are F2P per infobox members=No)
- notes: Base task monster. Max hit 9.

### Greater demon (Level 100)
- npc_ids: [7245]
- combat_level: 100
- defence_level: 80
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Catacombs of Kourend
- requirement: none
- notes: Catacombs variant. Max hit 8.

### Greater demon (Level 101)
- npc_ids: [7244]
- combat_level: 101
- defence_level: 50
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Catacombs of Kourend
- requirement: none
- notes: Catacombs variant. Max hit 10.

### Greater demon (Level 104, Wilderness Slayer Cave)
- npc_ids: [7871, 7872, 7873]
- combat_level: 104
- defence_level: 81
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Wilderness Slayer Cave
- requirement: none
- notes: Wilderness variant (Wilderness Slayer tertiary drops). Max hit 10.

### Greater demon (Level 113)
- npc_ids: [7246]
- combat_level: 113
- defence_level: 50
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -10 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): Catacombs of Kourend
- requirement: none
- notes: Catacombs variant. Max hit 10.

### K'ril Tsutsaroth
- npc_ids: [3129]
- combat_level: 650
- defence_level: 270
- def_bonuses: stab 70 / slash 80 / crush 80 / magic 80 / range 80
- weakness: MELEE stab (def 70, lowest), element Water (30%); also demonbane
- categories: demon
- location(s): God Wars Dungeon — Zamorak's Fortress
- requirement: 70 Hitpoints + partial Troll Stronghold (GWD access); boss — separate trip
- notes: BOSS. Counts toward greater demons. Max hit 30 magic / 46 melee / 49 special.

### Tstanon Karlak
- npc_ids: [3130]
- combat_level: 145
- defence_level: 125
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -5 / range 0
- weakness: MAGIC, element Water (40%); also demonbane
- categories: demon
- location(s): God Wars Dungeon — K'ril Tsutsaroth's chamber (Zamorak)
- requirement: GWD access; boss bodyguard
- notes: K'ril bodyguard, a greater demon — counts toward greater demons task. Max hit 15. (Balfrug Kreeyath counts as black demon; Zakl'n Gritch counts as lesser demon — neither toward greater.)

### Skotizo
- npc_ids: [7286]
- combat_level: 321
- defence_level: 200
- def_bonuses: stab 80 / slash 80 / crush 80 / magic 80 / range 130
- weakness: MELEE / MAGIC (def 80, tied), element Water (40%); demonbane (Arclight)
- categories: demon
- location(s): Skotizo's Lair, beneath Catacombs of Kourend
- requirement: Dark totem; boss — separate trip
- notes: BOSS. Counts toward greater demons (and black demons). Max hit 38.

### Tormented Demon
- npc_ids: [13599, 13600, 13601, 13602]
- combat_level: 450
- defence_level: 150
- def_bonuses: stab 75 / slash 175 / crush 68 / magic 5 / range (light 140 / standard 150 / heavy 90)
- weakness: MAGIC (magic def 5 lowest); demonbane — but shield must be broken first
- categories: demon
- location(s): Ancient Guthixian Temple
- requirement: While Guthix Sleeps quest
- notes: Counts toward greater demons. 4 NPC ids = shield/attack-style states; prays against and uses all combat styles, shield negates damage until broken. Max hit 31 auto / 45 special.

## Nechryael

### Nechryael (Normal)
- npc_ids: [8]
- combat_level: 115
- defence_level: 105
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 0 / range 20
- weakness: MAGIC
- categories: demon
- location(s): Catacombs of Kourend
- requirement: 80 Slayer, 85 combat
- notes: Base task monster (Nechryael ARE demons per attributes). Max hit 11. Summons Death spawn.

### Nechryael (Slayer task)
- npc_ids: [11]
- combat_level: 115
- defence_level: 105
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 0 / range 20
- weakness: MAGIC
- categories: demon
- location(s): Slayer Tower (task-only area)
- requirement: 80 Slayer, 85 combat; on a Nechryael task
- notes: Task-only version (id 11). Same stats. Counts toward task.

### Greater Nechryael (Regular)
- npc_ids: [7278]
- combat_level: 200
- defence_level: 85
- def_bonuses: stab 50 / slash 50 / crush 50 / magic 0 / range 50
- weakness: MAGIC
- categories: demon
- location(s): Catacombs of Kourend
- requirement: 80 Slayer, 85 combat
- notes: Stronger regular variant (not a superior). Max hit 21. Counts toward task.

### Greater Nechryael (Wilderness Slayer Cave)
- npc_ids: [11240]
- combat_level: 200
- defence_level: 85
- def_bonuses: stab 50 / slash 50 / crush 50 / magic 0 / range 50
- weakness: MAGIC
- categories: demon
- location(s): Wilderness Slayer Cave
- requirement: 80 Slayer, 85 combat
- notes: Wilderness variant. Counts toward task.

### Nechryarch
- npc_ids: [7411]
- combat_level: 300
- defence_level: 140
- def_bonuses: stab 30 / slash 30 / crush 30 / magic 0 / range 30
- weakness: MAGIC
- categories: demon
- location(s): Wherever Nechryael spawn (replaces a kill)
- requirement: 80 Slayer + Bigger and Badder unlocked
- notes: SUPERIOR slayer monster. Max hit 27. Spawns extra Death spawns. Counts toward task.

## Gaps

- Reanimated abyssal — combat_level: wiki lists N/A (no combat level); also does not actually count toward task (informational gap, not a stat).
- Skotizo — weakness style ambiguous: melee and magic defensive bonuses tied at 80 (range 130 higher); recorded as MELEE/MAGIC. Elemental Water 40% + demonbane resolve it in practice (Arclight melee).
- Demonic gorilla & Balfrug Kreeyath — weakness style tied between MELEE and RANGED (both def 0); recorded as MELEE/RANGED.
- No genuinely missing/UNKNOWN numeric stat fields: every variant has npc_ids, combat_level, defence_level, and full defensive bonuses from the wiki infoboxes.

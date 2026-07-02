# Giants / Beasts / Hounds-task variants

Source: OSRS Wiki (https://oldschool.runescape.wiki) `Infobox Monster` raw data, fetched 2026-06-29.
Weakness derived from lowest defensive bonus (melee = lowest of stab/slash/crush; range = lowest of light/standard/heavy) plus `elementalweakness`. Categories taken ONLY from wiki `attributes`.

## Fire giants

### Fire giant (Level 86)
- npc_ids: [2075,2076,2077,2078,2079,2080,2081,2082,2083,2084]
- combat_level: 86
- defence_level: 65
- def_bonuses: stab 0 / slash 3 / crush 2 / magic 0 / range 0
- weakness: MAGIC (water 100%), all melee/range def ~0 (best melee stab)
- categories: (none — attribute "fiery")
- location(s): Waterfall Dungeon, Brimhaven Dungeon, Stronghold Slayer Cave, Karuulm, etc.
- requirement: none
- notes: Attribute fiery → weak to water spells; immune to weak burns. slayxp 111.

### Fire giant (Level 104)
- npc_ids: [7252]
- combat_level: 104
- defence_level: 120
- def_bonuses: stab 20 / slash 10 / crush 10 / magic 50 / range -10
- weakness: RANGED (range def -10); water 100% element for magic; best melee crush/slash (10)
- categories: (none — attribute "fiery")
- location(s): Catacombs of Kourend
- requirement: none
- notes: slayxp 133.5. Higher defence and magic def than L86.

### Fire giant (Level 109)
- npc_ids: [7251]
- combat_level: 109
- defence_level: 65
- def_bonuses: stab 0 / slash 3 / crush 2 / magic 0 / range 0
- weakness: MAGIC (water 100%), all melee/range def ~0 (best melee stab)
- categories: (none — attribute "fiery")
- location(s): Catacombs of Kourend
- requirement: none
- notes: slayxp 153.5. Same defensive profile as L86.

### Branda the Fire Queen
- npc_ids: [12596]
- combat_level: 350
- defence_level: 100
- def_bonuses: stab 12 / slash 12 / crush 0 / magic 700 / range 700
- weakness: MELEE (crush, def 0); water 50% element
- categories: (none — attribute "fiery")
- location(s): Royal Titans arena (Asgarnia)
- requirement: boss — separate trip (Royal Titans, duo encounter)
- notes: cat "Fire Giants". Huge magic/range def (700) → crush only. Cannon-immune, freeze-immune. slayxp 735.

## Hellhounds

### Hellhound (Level 122)
- npc_ids: [104,105,7256]
- combat_level: 122
- defence_level: 102
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC (water 50%); all def bonuses 0
- categories: demon
- location(s): Regular (Stronghold Slayer Cave, Taverley Dungeon, Witchaven, etc.) and Catacombs of Kourend
- requirement: none
- notes: slayxp 116.

### Hellhound (Level 127, God Wars Dungeon)
- npc_ids: [3133]
- combat_level: 127
- defence_level: 106
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): God Wars Dungeon
- requirement: none (aggressive unless wearing Zamorak item)
- notes: slayxp 116. Not assignable by konar.

### Hellhound (Level 136, Wilderness Slayer Cave)
- npc_ids: [7877]
- combat_level: 136
- defence_level: 102
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC (water 50%); all def bonuses 0
- categories: demon
- location(s): Wilderness Slayer Cave
- requirement: none
- notes: slayxp 150.

### Cerberus
- npc_ids: [5862,5863,5866]
- combat_level: 318
- defence_level: 100
- def_bonuses: stab 50 / slash 100 / crush 25 / magic 65 / range 100
- weakness: MELEE (crush, def 25); water 40% element
- categories: demon (cat "Hellhounds, Bosses")
- location(s): Cerberus's Lair (Taverley Dungeon)
- requirement: Slayer 91; boss — separate trip
- notes: slayxp 690. Three combat styles.

### Skeleton Hellhound (Tarn's Lair, Level 97)
- npc_ids: [5054]
- combat_level: 97
- defence_level: 100
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC (earth 35%); all def bonuses 0
- categories: demon
- location(s): Tarn's Lair / In Search of the Myreque content area
- requirement: none
- notes: slayxp 55. Distinct from Vet'ion/Calvar'ion variants below.

### Skeleton Hellhound (Vet'ion, Level 194)
- npc_ids: [6613]
- combat_level: 194
- defence_level: 150
- def_bonuses: stab 101 / slash 103 / crush 10 / magic 180 / range 266
- weakness: MELEE (crush, def 10); earth 30% element
- categories: undead (cat "Hellhounds, Skeletons")
- location(s): Vet'ion fight, Wilderness
- requirement: summoned by Vet'ion — separate trip (boss fight)
- notes: slayxp 33. HP only 30.

### Skeleton Hellhound (Calvar'ion, Level 115)
- npc_ids: [12107]
- combat_level: 115
- defence_level: 95
- def_bonuses: stab 76 / slash 73 / crush 10 / magic 126 / range 155
- weakness: MELEE (crush, def 10); earth 30% element
- categories: undead (cat "Hellhounds, Skeletons")
- location(s): Calvar'ion fight, Wilderness (singles)
- requirement: summoned by Calvar'ion — separate trip (boss fight)
- notes: slayxp 30. HP 30.

### Greater Skeleton Hellhound (Vet'ion, Level 231)
- npc_ids: [6614]
- combat_level: 231
- defence_level: 180
- def_bonuses: stab 150 / slash 163 / crush 20 / magic 210 / range 275
- weakness: MELEE (crush, def 20); earth 40% element
- categories: undead (cat "Hellhounds, Skeletons")
- location(s): Vet'ion fight (second phase), Wilderness
- requirement: summoned by Vet'ion — separate trip (boss fight)
- notes: slayxp 33. HP 30.

### Greater Skeleton Hellhound (Calvar'ion, Level 139)
- npc_ids: [12108]
- combat_level: 139
- defence_level: 110
- def_bonuses: stab 95 / slash 101 / crush 15 / magic 156 / range 184
- weakness: MELEE (crush, def 15); earth 40% element
- categories: undead (cat "Hellhounds, Skeletons")
- location(s): Calvar'ion fight (second phase), Wilderness (singles)
- requirement: summoned by Calvar'ion — separate trip (boss fight)
- notes: slayxp 30. HP 30.

### Reanimated hellhound
- npc_ids: [11463]
- combat_level: N/A
- defence_level: 102
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): Anywhere (cast Reanimate Hellhound, Arceuus spellbook)
- requirement: 72 Magic + Arceuus; ensouled hellhound head
- notes: slayxp 35. HP 35. Counts toward Hellhound task.

## Dark beasts

### Dark beast
- npc_ids: [4005 (Mourning's End area), 7250 (Catacombs of Kourend)]
- combat_level: 182
- defence_level: 120
- def_bonuses: stab 30 / slash 40 / crush 100 / magic 90 / range 100
- weakness: MELEE (stab, def 30); earth 60% element for magic
- categories: (none — no attribute; NOT a demon)
- location(s): Temple of Light / Mourning's End Part II tunnels (id 4005); Catacombs of Kourend & Iorwerth Dungeon (id 7250)
- requirement: Slayer 90; id 4005 area requires Mourning's End Part II; Catacombs/Iorwerth no quest
- notes: slayxp 225.4. Attacks crush + magic. High crush/magic/range def.

### Night beast (superior)
- npc_ids: [7409]
- combat_level: 374
- defence_level: 220
- def_bonuses: stab 75 / slash 80 / crush 200 / magic 190 / range 200
- weakness: MELEE (stab, def 75); no elemental weakness listed
- categories: (none — no attribute)
- location(s): wherever Dark beasts spawn (Tirannwn region)
- requirement: Slayer 90; Bigger and Badder unlock (superior); on Dark beast task
- notes: slayxp 6462. Superior variant of Dark beast.

## Dagannoth

### Dagannoth (Level 74, ranged, Waterbirth)
- npc_ids: [970,971,972]
- combat_level: 74
- defence_level: 50
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC (earth 35%); all def bonuses 0
- categories: (none — cat "Dagannoth")
- location(s): Waterbirth Island Dungeon (regular)
- requirement: none
- notes: slayxp 70. Ranged attacker variant.

### Dagannoth (Level 92, melee, Waterbirth)
- npc_ids: [973,974,975]
- combat_level: 92
- defence_level: 71
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 50 / range 50
- weakness: MELEE (melee def 0; magic/range def 50); earth 35% element
- categories: (none — cat "Dagannoth")
- location(s): Waterbirth Island Dungeon (regular)
- requirement: none
- notes: slayxp 120. Stab attacker variant.

### Dagannoth (Level 74, Catacombs of Kourend)
- npc_ids: [7259]
- combat_level: 74
- defence_level: 50
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC (earth 35%); all def bonuses 0
- categories: (none — cat "Dagannoth")
- location(s): Catacombs of Kourend
- requirement: none
- notes: slayxp 70.

### Dagannoth (Level 92, Catacombs of Kourend)
- npc_ids: [7260]
- combat_level: 92
- defence_level: 71
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 50 / range 50
- weakness: MELEE (melee def 0); earth 35% element
- categories: (none — cat "Dagannoth")
- location(s): Catacombs of Kourend
- requirement: none
- notes: slayxp 120.

### Dagannoth spawn
- npc_ids: [3184]
- combat_level: 42
- defence_level: 25
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC (earth 35%); all def bonuses 0
- categories: (none — cat "Dagannoth")
- location(s): Lighthouse basement / Waterbirth (Fremennik)
- requirement: none
- notes: slayxp 35.

### Dagannoth fledgeling
- npc_ids: [2264]
- combat_level: 70
- defence_level: 50
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC (earth 35%); all def bonuses 0
- categories: (none — cat "Dagannoth")
- location(s): Waterbirth Island Dungeon (Dagannoth Kings area)
- requirement: none
- notes: slayxp 0 (xpbonus -100). Gives no slayer XP but counts as Dagannoth.

### Reanimated dagannoth
- npc_ids: [7033]
- combat_level: N/A
- defence_level: 81
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: (none — cat "Dagannoth")
- location(s): Anywhere (Reanimate Dagannoth, Arceuus spellbook)
- requirement: 64 Magic + Arceuus; ensouled dagannoth head
- notes: slayxp 35. Ranged attacker.

### Dagannoth Rex
- npc_ids: [2267]
- combat_level: 303
- defence_level: 255
- def_bonuses: stab 255 / slash 255 / crush 255 / magic 10 / range 255
- weakness: MAGIC (magic def 10)
- categories: (none — cat "Bosses, Dagannoth")
- location(s): Dagannoth Kings, Waterbirth Island Dungeon
- requirement: boss — separate trip
- notes: slayxp 331.4. Melee attacker; kill with magic.

### Dagannoth Prime
- npc_ids: [2266]
- combat_level: 303
- defence_level: 255
- def_bonuses: stab 255 / slash 255 / crush 255 / magic 255 / range 10
- weakness: RANGED (range def 10)
- categories: (none — cat "Bosses, Dagannoth")
- location(s): Dagannoth Kings, Waterbirth Island Dungeon
- requirement: boss — separate trip
- notes: slayxp 331.4. Magic attacker; kill with ranged.

### Dagannoth Supreme
- npc_ids: [2265]
- combat_level: 303
- defence_level: 128
- def_bonuses: stab 10 / slash 10 / crush 10 / magic 255 / range 550
- weakness: MELEE (melee def 10)
- categories: (none — cat "Bosses, Dagannoth")
- location(s): Dagannoth Kings, Waterbirth Island Dungeon
- requirement: boss — separate trip
- notes: slayxp 255. Ranged attacker; kill with melee.

## Bloodveld

### Bloodveld (Level 76)
- npc_ids: [484,485,486,487]
- combat_level: 76
- defence_level: 30
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): Slayer Tower, Catacombs of Kourend, Stronghold Slayer Cave, Meiyerditch, etc.
- requirement: Slayer 50; Morytania locations need Priest in Peril
- notes: slayxp 120. Magical melee attacker.

### Bloodveld (Level 81, God Wars Dungeon)
- npc_ids: [3138]
- combat_level: 81
- defence_level: 30
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): God Wars Dungeon
- requirement: Slayer 50
- notes: slayxp 134.

### Mutated Bloodveld
- npc_ids: [7276]
- combat_level: 123
- defence_level: 30
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): Catacombs of Kourend, Iorwerth Dungeon, Meiyerditch Laboratories
- requirement: Slayer 50
- notes: slayxp 170. Higher att/str than regular.

### Reanimated bloodveld
- npc_ids: [7034]
- combat_level: N/A
- defence_level: 30
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): Anywhere (Reanimate Bloodveld, Arceuus spellbook)
- requirement: Slayer 50; 65 Magic + Arceuus; ensouled bloodveld head
- notes: slayxp 35. HP 35.

### Insatiable Bloodveld (superior)
- npc_ids: [7397]
- combat_level: 202
- defence_level: 85
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): wherever regular Bloodvelds spawn
- requirement: Slayer 50; Bigger and Badder unlock (superior); on Bloodveld task
- notes: slayxp 2900. Superior of regular Bloodveld.

### Insatiable mutated Bloodveld (superior)
- npc_ids: [7398]
- combat_level: 278
- defence_level: 130
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: UNKNOWN (all def bonuses 0; no elemental weakness listed)
- categories: demon
- location(s): wherever Mutated Bloodvelds spawn (Catacombs/Iorwerth)
- requirement: Slayer 50; Bigger and Badder unlock (superior); on Bloodveld task
- notes: slayxp 4100. Superior of Mutated Bloodveld.

## Gaps
- Fire giants have NO superior slayer monster on the wiki (confirmed — only Branda the Fire Queen shares cat "Fire Giants" as a boss). Dagannoth tasks also have NO superior.
- Bloodveld family (all variants) and Hellhound GWD/Reanimated have all-zero defensive bonuses and no listed elemental weakness → weakness marked UNKNOWN; in practice these are killed with any style (commonly melee/magic). No data to disambiguate from the infobox.
- Hellhound v1/v3 and Skeleton Hellhound (Tarn's Lair) list elemental weakness (water/earth) despite zero defensive bonuses → magic with that element is the listed soft weakness.
- Dagannoth fledgeling counts as Dagannoth but yields 0 slayer XP (xpbonus -100) and is non-aggressive.
- Skeleton Hellhound (Tarn's Lair, id 5054) is a separate demon-attribute monster from the undead Vet'ion/Calvar'ion skeleton hellhounds; all three families counted under the Hellhound task.

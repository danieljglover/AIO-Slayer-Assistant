# Humanoid-task variants

Source: OSRS Wiki (oldschool.runescape.wiki) `Slayer_task/*` pages + each monster's `{{Infobox Monster}}` (raw), fetched 2026-06-29.
Weakness = lowest defensive bonus (stab/slash/crush/magic + light/standard/heavy ranged) combined with `elementalweakness`. Categories set ONLY where the wiki `attributes` field is present — none of the monsters below declare `attributes`, so all `categories` are empty.
Range defence shown is `dstandard` (standard ammo); where it differs, lowest of light/standard/heavy is used for the weakness call.

## Trolls
Task: combat 60+. Mountain & ice trolls. Cannon strongly recommended. Bosses also fightable in Nightmare Zone.

### Mountain troll
- npc_ids: lvl69 [936,937,938,939,940,941,942]; lvl71 [4143]
- combat_level: 69 and 71 (versioned)
- defence_level: 40 (lvl69), 25 (lvl71)
- def_bonuses: stab 0 / slash 0 / crush 10 / magic 200 / range 40 (light 200, std 40, heavy 200)
- weakness: MELEE, best stab/slash (crush slightly resisted); element fire 50% (magic w/ fire viable)
- categories: (none)
- location(s): Death Plateau, Trollheim, S of Keldagrim, S of Mount Quidamortem, Troll Stronghold (surface/underground)
- requirement: none (no quest)
- notes: Easiest troll variant; fully negated with Protect from Melee. Keldagrim tunnel is lvl69 only.

### Ice troll (Ice Path / Trollweiss)
- npc_ids: lvl120 [650,652,653,701,703,704]; lvl121 [651,654,702,705]; lvl123 [649,700]; lvl124 [648,699]
- combat_level: 120, 121, 123, 124 (versioned)
- defence_level: 120, 110, 100, 80 (respectively)
- def_bonuses: stab 30 / slash 60 / crush 30 / magic 0 / range 0
- weakness: MAGIC (fire 100%, magic def 0); RANGED def also 0; best melee stab/crush (slash resisted)
- categories: (none)
- location(s): Ice Path, Trollweiss Dungeon
- requirement: none for Ice Path; stronger with poor drops — wiki advises against vs isle trolls
- notes: Drains stats. Trollweiss dungeon mageable without Neitiznot shield.

### Ice troll runt
- npc_ids: variant1 [5828] (fire 100%); variant2 [1874] (fire 50%)
- combat_level: 74
- defence_level: 70
- def_bonuses: stab 30 / slash 60 / crush 30 / magic 0 / range 0
- weakness: MAGIC (fire 100% variant1 / 50% variant2, magic def 0); RANGED def 0; best melee stab/crush
- categories: (none)
- location(s): Fremennik Isles, Ice Troll Caves, Jatizso mine
- requirement: The Fremennik Trials (Jatizso access)
- notes: Weakest isle troll; guards help kills. Drops vary by spawn.

### Ice troll male
- npc_ids: variant1 [5829] (fire 100%); variant2 [1875] (fire 50%)
- combat_level: 82
- defence_level: 40
- def_bonuses: stab 30 / slash 60 / crush 30 / magic 0 / range 0
- weakness: MAGIC (fire 100%/50%, magic def 0); RANGED def 0; best melee stab/crush
- categories: (none)
- location(s): Fremennik Isles, Ice Troll Caves, Jatizso mine
- requirement: The Fremennik Trials (Jatizso access)
- notes: Drops vary by spawn.

### Ice troll female
- npc_ids: variant1 [5830] (fire 100%); variant2 [1876] (fire 50%)
- combat_level: 82
- defence_level: 40
- def_bonuses: stab 30 / slash 60 / crush 30 / magic 0 / range 0
- weakness: MAGIC (fire 100%/50%, magic def 0); RANGED def 0; best melee stab/crush
- categories: (none)
- location(s): Fremennik Isles, Ice Troll Caves, Jatizso mine
- requirement: The Fremennik Trials (Jatizso access)
- notes: Uses ranged attacks; Neitiznot shield reduces their accuracy/damage.

### Ice troll grunt
- npc_ids: [1877,5831]
- combat_level: 100 (wiki table also lists 102 in places; infobox = 100)
- defence_level: 60
- def_bonuses: stab 30 / slash 60 / crush 30 / magic 0 / range 0
- weakness: MAGIC/RANGED (both def 0); fire 35%; best melee stab/crush
- categories: (none)
- location(s): Fremennik Isles, Ice Troll Caves
- requirement: The Fremennik Trials (Jatizso access)
- notes: Neitiznot shield reduces rock-throw max hit.

### Troll general
- npc_ids: [4120,4121,4122] (sword / hammer grey / hammer brown)
- combat_level: 113
- defence_level: 40
- def_bonuses: stab 35 / slash 60 / crush 35 / magic 200 / range 120 (light 200, std 120, heavy 200)
- weakness: MELEE, best stab/crush; element earth 20%
- categories: (none)
- location(s): Troll Stronghold (level 2)
- requirement: Troll Stronghold quest access — scarce, separate trip; near-nonexistent drops
- notes: Rarely killed for tasks.

### Reanimated troll
- npc_ids: [7030]
- combat_level: N/A (reanimated)
- defence_level: 40
- def_bonuses: stab 0 / slash 0 / crush 10 / magic 200 / range 200
- weakness: MELEE, best stab/slash
- categories: (none — no `attributes` on wiki)
- location(s): Dark Altar (Arceuus) or wherever cast
- requirement: Adept Reanimation (Arceuus spellbook) on ensouled troll head; reduced slayer xp, gives Prayer xp
- notes: Alternative count method.

### Dad (boss)
- npc_ids: [4130]
- combat_level: 101 (hard mode 201)
- defence_level: 50
- def_bonuses: stab 25 / slash 25 / crush 40 / magic 200 / range 200
- weakness: MELEE, best stab/slash; element earth 40%
- categories: (none)
- location(s): Troll Arena; Nightmare Zone
- requirement: boss — separate trip (Troll Romance / Troll Arena), or NMZ
- notes: Counts toward task.

### Ice Troll King (boss)
- npc_ids: [5822]
- combat_level: 122 (hard mode 213)
- defence_level: 80
- def_bonuses: stab 45 / slash 45 / crush 45 / magic 2000 / range 2000
- weakness: MELEE (all melee 45); element fire 35%
- categories: (none)
- location(s): Northern Neitiznot lair; Nightmare Zone
- requirement: boss — separate trip (The Fremennik Isles), or NMZ
- notes: Uses all three attack styles; immune to magic/ranged via 2000 bonuses.

### Arrg (boss)
- npc_ids: [643,642]
- combat_level: 113 (hard mode 210)
- defence_level: 40
- def_bonuses: stab 35 / slash 60 / crush 35 / magic 200 / range 200
- weakness: MELEE, best stab/crush; element earth 50%
- categories: (none)
- location(s): Troll Stronghold (middle level); Nightmare Zone
- requirement: boss — separate trip (Troll Stronghold), or NMZ
- notes: Counts toward task.

## Elves
Task: combat 70+, requires Regicide. Elves wield crystal bow (ranged) or crystal halberd (melee, 2-tile range).

### Iorwerth Warrior
- npc_ids: Iorwerth Camp [3429,8759]; Iorwerth Dungeon [9502,9503]
- combat_level: 108
- defence_level: 80
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 60 / range 50
- weakness: MELEE stab / RANGED (both 50); magic 60; best melee stab
- categories: (none)
- location(s): Iorwerth Camp; Iorwerth Dungeon
- requirement: Regicide (camp); Song of the Elves (dungeon, cannonable, 13 warriors)
- notes: Dungeon ones drop crystal shards + enhanced crystal teleport seeds; camp ones do not.

### Elf Warrior
- npc_ids: [5293,5294]
- combat_level: 108
- defence_level: 80
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 60 / range 50
- weakness: MELEE stab / RANGED (both 50); best melee stab
- categories: (none)
- location(s): Lletya
- requirement: Mourning's End Part I (progress) for Lletya access
- notes: Halberd elf; minimal drops (crystal teleport seeds).

### Mourner (level 108)
- npc_ids: [9017] (level-108 only; level-11 mourner [9013] does NOT count)
- combat_level: 108
- defence_level: 80
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 60 / range 50
- weakness: MELEE stab / RANGED (both 50); best melee stab
- categories: (none)
- location(s): Mourner Headquarters (downstairs)
- requirement: Mourning's End Part I; full Mourner gear to enter; NOT available after Song of the Elves
- notes: Only the 4 downstairs lvl-108 mourners count; upstairs mourners do not.

### Guard (Prifddinas)
- npc_ids: [9182,9183,9184,9185,9186,9189,9187,9188]
- combat_level: 108
- defence_level: 80
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 60 / range 50
- weakness: MELEE stab / RANGED (both 50); best melee stab
- categories: (none)
- location(s): Prifddinas (all gates, Tower of Voices, market)
- requirement: Song of the Elves
- notes: Drop crystal shards + enhanced crystal teleport seeds.

### Reanimated elf
- npc_ids: [7029]
- combat_level: N/A (reanimated)
- defence_level: 80
- def_bonuses: stab 5 / slash 20 / crush 40 / magic 60 / range 50
- weakness: MELEE, best stab
- categories: (none)
- location(s): Dark Altar (Arceuus) or wherever cast
- requirement: Adept Reanimation on ensouled elf head; reduced slayer xp, +754 Prayer xp/kill
- notes: Ensouled elf heads buyable on GE.

### Iorwerth Archer
- npc_ids: female [8760]; male [3428]
- combat_level: 90
- defence_level: 80
- def_bonuses: stab 50 / slash 50 / crush 50 / magic 60 / range 70
- weakness: MELEE (all melee 50)
- categories: (none)
- location(s): Iorwerth Camp
- requirement: Regicide
- notes: Bow-wielding (ranged attacker).

### Elf Archer
- npc_ids: [5295,5296]
- combat_level: 90
- defence_level: 80
- def_bonuses: stab 50 / slash 50 / crush 50 / magic 60 / range 70
- weakness: MELEE (all melee 50)
- categories: (none)
- location(s): Lletya
- requirement: Mourning's End Part I (progress)
- notes: Bow-wielding (ranged attacker).

## Lizardmen
Task: requires "Reptile got ripped" unlock (75 Slayer reward pts). Killing lizardmen on a normal Lizard task does NOT count.

### Lizardman
- npc_ids: lvl53 [6914,6915]; lvl62 [6916,6917,8563]
- combat_level: 53 and 62 (versioned)
- defence_level: 43 (lvl53), 52 (lvl62)
- def_bonuses: stab -20 / slash 25(lvl53)/20(lvl62) / crush 0(lvl53)/5(lvl62) / magic 0 / range 0 (light -10, std 0)
- weakness: MELEE stab (def -20)
- categories: (none)
- location(s): Shayziens' Wall, Lizardman Canyon, Lizardman Settlement, Kebos Swamp, Battlefront, Molch
- requirement: Reptile got ripped unlock
- notes: Weak poison (starts 3). Canyon is multicombat + cannonable.

### Lizardman brute
- npc_ids: standard [6918,6919]; Battlefront [8564,10947]
- combat_level: 75
- defence_level: 65
- def_bonuses: stab -20 / slash 30 / crush 10 / magic 0 / range 0 (light -10, std 0)
- weakness: MELEE stab (def -20)
- categories: (none)
- location(s): Shayziens' Wall, Lizardman Canyon, Settlement, Kebos Swamp, Battlefront, Molch, S of Xeric's Shrine
- requirement: Reptile got ripped unlock
- notes: —

### Lizardman shaman
- npc_ids: standard [6766,6767,7744,7745]; Lizardman Temple [8565]
- combat_level: 150
- defence_level: 140
- def_bonuses: stab -20 / slash 40 / crush 30 / magic 50 / range 0 (light -10, std 0)
- weakness: MELEE stab (def -20); ranged also low; commonly ranged + Protect from Missiles
- categories: (none)
- location(s): Lizardman Canyon west; Lizardman Caves (slayer only); Lizardman Temple (beneath Molch)
- requirement: Reptile got ripped unlock; Shayzien armour (tier 5) to block their 30-dmg poison spit
- notes: Drops dragon warhammer. Spawn AoE reduced 5x5→3x3 in Temple. Hard Kourend & Kebos diary lets Slayer helm act as Shayzien helm.

## Suqahs
Task: partial completion of Lunar Diplomacy. Lunar Isle only. Two behavioural types (melee-only vs melee+weak Ice Barrage/Water Wave) share one infobox.

### Suqah
- npc_ids: [787,788,789,790,791,792,793]
- combat_level: 111
- defence_level: 95
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 90 / range: light 50, std 30, heavy 30
- weakness: RANGED (standard/heavy ammo, def 30 lowest); then MELEE stab (50); element earth 20%
- categories: (none)
- location(s): North Lunar Isle (melee + magic), South-east & South-west Lunar Isle (melee only)
- requirement: partial Lunar Diplomacy
- notes: Northern suqah cast weak Ice Barrage (guaranteed 10 if no Protect from Magic); southern/SW are melee-only — better if avoiding Protect from Magic. Seal of passage for bank unless Fremennik Elite diary / Dream Mentor done.

## Cave horrors
Task: Slayer 58, combat 85, requires Cabin Fever. Mos Le'Harmless Cave only. Witchwood icon needed to melee (else special always hits 10% base HP); light source required.

### Cave horror
- npc_ids: [1047,1048,1049,1050,1051] (Worker / Young worker / Guard / Small guard / Alpha)
- combat_level: 80
- defence_level: 62
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: no style-specific defence (all bonuses 0 — equally weak to all); element fire 30%
- categories: (none)
- location(s): Mos Le'Harmless Cave
- requirement: Cabin Fever; witchwood icon to melee at distance; light source
- notes: Attacks with Magic-based melee (Protect from Melee negates). Fast ranged weapon recommended. Horrorific unlock raises count to 200-250.

### Cave abomination (superior)
- npc_ids: [7401]
- combat_level: 206
- defence_level: 142
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: no style-specific defence (all bonuses 0); no elemental weakness listed
- categories: (none)
- location(s): Mos Le'Harmless Cave
- requirement: superior — requires Bigger and Badder unlock
- notes: Superior variant of Cave horror.

## Gaps
- Ice troll grunt: wiki Monster Variants table lists combat 102 in one spot but the infobox states 100 — recorded 100 (infobox authoritative); minor discrepancy noted.
- Thrower Troll / Thrower troll (Trollheim) / Twig / Berry / Stick / Rock / Pee Hat / Kraka / Troll spectator / River troll count toward Trolls tasks but were out of scope (prompt named Mountain/Ice/General/Reanimated/Dad/Ice Troll King/Arrg) — not enumerated here.
- No monster in this file declares an `attributes` field on the wiki, so all `categories` are empty (incl. reanimated variants — wiki does not tag them undead).
- Suqah ranged `dheavy` assumed 30 (same as standard); only dlight 50 / dstandard 30 explicitly grepped.

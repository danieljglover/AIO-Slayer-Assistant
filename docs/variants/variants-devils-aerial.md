# Devils / Kraken / Aerial-task variants

Source: OSRS Wiki (oldschool.runescape.wiki), `?action=raw` Infobox Monster + Slayer task pages. Fetched 2026-06-29.
Weakness method: lowest defensive bonus + `elementalweakness`. Ranged def = dlight/dstandard/dheavy.
Categories set ONLY per wiki `attributes` (none of these monsters carry demon/dragon/undead/kalphite attributes; aviansie/Kree'arra/bodyguards/reanimated have `flying`).

## Smoke devils
Task: Slayer 93, Combat 85. Konar/Nieve/Duradel. Facemask/slayer helm mandatory (else stat-draining damage). Only in Smoke Devil Dungeon, on-task only. Attack style: magical ranged.

### Smoke devil
- npc_ids: [498]
- combat_level: 160
- defence_level: 275
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 600 / range 44
- weakness: MAGIC, element air 30% (note: stab/slash/crush def are 0 but magic def is 600; killed via Ancient barrage because magic LEVEL is only 1, not because of magic def)
- categories: (none)
- location(s): Smoke Devil Dungeon (south of Castle Wars)
- requirement: on-task only; facemask/slayer helm required
- notes: HP 185, max hit 20, slayxp 185. Cannon strongly recommended to lure/stack.

### Nuclear smoke devil
- npc_ids: [7406]
- combat_level: 280
- defence_level: 390
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 850 / range 80
- weakness: MAGIC, element air 20% (same nuance as above; barrage)
- categories: (none)
- location(s): Smoke Devil Dungeon
- requirement: superior — spawns on smoke devil death after 'Bigger and Badder' (50 pts)
- notes: HP 240, max hit 29, slayxp 2400. Very high superior drop-table roll chance.

### Thermonuclear smoke devil
- npc_ids: [499]
- combat_level: 301
- defence_level: 360
- def_bonuses: stab 11 / slash 4 / crush 9 / magic 800 / range UNKNOWN (no dlight/standard/heavy versioned grep; treat ~melee-tier)
- weakness: MAGIC, element air 20% (melee bonuses very low but boss is maged in its lair)
- categories: (Bosses)
- location(s): Smoke Devil Dungeon — boss lair
- requirement: boss — separate trip
- notes: cat = Bosses, Smoke Devils. See Thermonuclear smoke devil/Strategies.

## Dust devils
Task: Slayer 65, Combat 70, Desert Treasure I started. Krystilia/Chaeldar/Konar/Nieve/Duradel. Facemask/slayer helm mandatory. Attack style: ranged-melee (melee that rolls vs ranged defence).

### Dust devil (multiple combat variants)
- npc_ids: [423, 11238, 7249]
- combat_level: 93 (id 423), 93 (id 11238), 110 (id 7249)
- defence_level: 40 (shared)
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0 (all variants share one defensive profile)
- weakness: MAGIC, element air 35% (all defensive bonuses 0; meta is Ancient barrage/chinning in multi)
- categories: (none)
- location(s): id 423 Catacombs of Kourend; id 11238 Wilderness Slayer Cave (lvl 25-28 Wild); id 7249 Smoke Dungeon. HP 105/105/130 respectively.
- requirement: Wilderness Slayer Cave / Smoke Dungeon west room are on-task only
- notes: 3 versioned ids differ only by combat level (93/93/110) and location/drop table; stats identical.

### Choke devil
- npc_ids: [7404]
- combat_level: 264
- defence_level: 120
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: MAGIC, element air 25% (all bonuses 0)
- categories: (none)
- location(s): Catacombs of Kourend, Wilderness Slayer Cave, Smoke Dungeon
- requirement: superior — spawns on dust devil death after 'Bigger and Badder' (50 pts)
- notes: HP 300.

## Cave kraken
Task: Slayer 87, Magic 50. Chaeldar/Konar/Nieve/Duradel. Kraken Cove only. Magic-only (cannot be meleed; reduced ranged damage).

### Cave kraken
- npc_ids: [492] (Cave kraken); [493] (Whirlpool — spawn form, combat N/A)
- combat_level: 127
- defence_level: 150
- def_bonuses: stab 0 / slash 0 / crush 0 / magic -63 / range 100
- weakness: MAGIC, element earth 50% (magic def is negative -63 = strongest weakness; ranged resisted +100, melee unreachable)
- categories: (none)
- location(s): Kraken Cove
- requirement: must attack the whirlpool to surface the kraken; magic only
- notes: HP 125. Uncharged trident drop 1/200.

### Kraken (boss)
- npc_ids: [494] (Kraken); [496] (Whirlpool, combat N/A)
- combat_level: 291
- defence_level: 1
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 130 / range 300
- weakness: MAGIC, element earth 50% (def level only 1; magic only reachable style; typeless attacks unprayable)
- categories: (Bosses)
- location(s): Kraken Cove — boss whirlpool
- requirement: boss — separate trip; magic only
- notes: HP 255. Fishing explosives to initiate. See Kraken/Strategies.

## Aviansie
Task: Agility 60 or Strength 60; Death Plateau; 'Watch the birdie' unlock (80 pts). Krystilia/Chaeldar/Konar/Nieve/Duradel. Flying — only ranged/magic hit them (plus salamanders/halberds as direct target). Ranged-weak. Armadylean god items needed for tolerance.

### Aviansie (15 combat-level variants)
- npc_ids: [3169, 3177, 3178, 3170, 3179, 3172, 3171, 3180, 3173, 3181, 3174, 3182, 3183, 3175, 3176]
- combat_level: 69, 71, 73, 79, 79, 83, 84, 89, 92, 94, 97, 97, 131, 137, 148 (in id order above)
- defence_level: 70, 55, 55, 70, 55, 100, 70, 115, 100, 115, 100, 115, 175, 160, 160 (matching id order)
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range -20 (light), 0 (standard/heavy) — shared across all 15
- weakness: RANGED, element air 45% (range light def -20 = weakest; flying blocks melee)
- categories: flying
- location(s): God Wars Dungeon (combat 69-148) and Wilderness God Wars Dungeon (combat 69-137). Same id set; Wild GWD is multicombat/PvP.
- requirement: GWD access (Death Plateau); Armadyl items for tolerance
- notes: 15 versioned rows differ by combat/HP/def/range level only; defensive bonuses identical. HP 70/63/67/83/77/86/86/69/95/75/98/79/115/124/139.

### Kree'arra (boss)
- npc_ids: [3162]
- combat_level: 580
- defence_level: 260
- def_bonuses: stab 180 / slash 180 / crush 180 / magic 200 / range 200
- weakness: RANGED/MAGIC, element air 30% (melee bonus 180 lowest but flying blocks melee; meta is maged with Eldritch staff). Best reachable: tie magic/range 200.
- categories: flying, Bosses
- location(s): God Wars Dungeon — Armadyl boss room
- requirement: boss — separate trip; 70 Ranged + crossbow + mith grapple to access. Counts as aviansie task.
- notes: HP 255.

### Flight Kilisa (Kree'arra bodyguard)
- npc_ids: [3165]
- combat_level: 159
- defence_level: 175
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: RANGED/MAGIC, element air 30% (all bonuses 0; flying)
- categories: flying
- location(s): GWD Armadyl boss room
- requirement: in boss room (separate trip). Counts toward aviansie task.
- notes: melee-style bodyguard.

### Flockleader Geerin (Kree'arra bodyguard)
- npc_ids: [3164]
- combat_level: 149
- defence_level: 175
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: RANGED/MAGIC, element air 30%
- categories: flying
- location(s): GWD Armadyl boss room
- requirement: boss room (separate trip). Counts toward aviansie task.
- notes: ranged bodyguard.

### Wingman Skree (Kree'arra bodyguard)
- npc_ids: [3163]
- combat_level: 143
- defence_level: 160
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: RANGED/MAGIC, element air 30%
- categories: flying
- location(s): GWD Armadyl boss room
- requirement: boss room (separate trip). Counts toward aviansie task.
- notes: mage bodyguard.

### Reanimated aviansie
- npc_ids: [7037]
- combat_level: N/A
- defence_level: 70
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0
- weakness: RANGED/MAGIC, element UNKNOWN (no elementalweakness field); flying
- categories: flying
- location(s): wherever ensouled aviansie head reanimated (Arceuus spellbook)
- requirement: 'Watch the birdie' alt; needs ensouled aviansie head
- notes: counts toward aviansie task.

## Gryphons
Task: Slayer 51; Troubled Tortugans quest. Vannaka & Chaeldar assign; Nieve/Duradel after 'Wings Spread' unlock. NEW Sailing task. Exclusively on The Great Conch. Melee-weak (best stab). Tortugan shield required vs superior/boss only. Special attack knockback if worn weight < 30 (normal) / 40 (boss).

### Gryphon
- npc_ids: [14857, 14858]
- combat_level: 95
- defence_level: 50
- def_bonuses: stab 10 / slash 20 / crush 40 / magic 100 / range 60
- weakness: MELEE, best stab (10 lowest), element air 50%
- categories: (none — attributes empty)
- location(s): The Great Conch — western cavern + eastern cavern (task-only). Both have southern multi/cannon rooms.
- requirement: Troubled Tortugans quest; Slayer 51
- notes: HP 110. Two ids = same stats (likely model/orientation variants).

### Shellbane gryphon (boss)
- npc_ids: [14860]
- combat_level: 235
- defence_level: 120
- def_bonuses: stab 10 / slash 20 / crush 40 / magic 100 / range 60
- weakness: MELEE, best stab (10 lowest), element air 50%
- categories: (Bosses)
- location(s): Shellbane Gryphon Cave (central lair on The Great Conch)
- requirement: boss — separate trip; tortugan shield required to nullify special
- notes: HP 400. See Shellbane Gryphon/Strategies.

### Dire gryphon (superior)
- npc_ids: UNKNOWN (not fetched; superior listed on task page as alternative)
- combat_level: UNKNOWN
- defence_level: UNKNOWN
- def_bonuses: stab UNKNOWN / slash UNKNOWN / crush UNKNOWN / magic UNKNOWN / range UNKNOWN
- weakness: MELEE (per task page, mechanics like boss); element air 50% (gryphon family)
- categories: UNKNOWN (likely none + superior)
- location(s): The Great Conch
- requirement: superior — spawns on gryphon death after 'Bigger and Badder' (50 pts); tortugan shield required (boss-like mechanics)
- notes: Not in original brief's superior list; task page confirms Dire gryphon as the gryphon superior. Stats page not fetched.

## Gaps
- Thermonuclear smoke devil ranged def bonus (dlight/standard/heavy) not captured — melee def bonuses given (stab 11/slash 4/crush 9), magic 800.
- Smoke devils & dust devils: brief calls them "magic-weak"; data shows magic def is very HIGH for smoke devils (600) and 0 for dust — they are barraged due to low magic LEVEL, not low magic def. Flagged inline.
- Dire gryphon (gryphon superior) stats UNKNOWN — page not fetched; ids/combat/def all UNKNOWN. This is the actual superior (brief listed "Shellbane gryphon" as the alt, which is the BOSS, not the superior).
- Reanimated aviansie has no elementalweakness field (UNKNOWN element).
- Kree'arra & bodyguards: weakness shown RANGED/MAGIC (flying blocks melee); meta is magic. Not a single unambiguous style.

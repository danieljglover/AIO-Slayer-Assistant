# Wyrm / Drake / Wyvern-task variants

Source: OSRS Wiki (oldschool.runescape.wiki), `?action=raw` infoboxes, fetched 2026-06-29.
Stats verified per `{{Infobox Monster}}`. `range` def value = `dstandard` (standard ranged); light/heavy noted where they diverge meaningfully.
Team rule: the DRACONIC set (dragons + wyverns + Wyrms + Drakes + Hydras) all take dragonbane, so EVERY monster here gets `dragon: true`.
Undead check: every monster has `attributes = dragon` only — NONE are undead. Skeletal wyverns are explicitly magically-animated remains, NOT undead (Salve amulet does not work) — confirmed.
Superiors verified via Superior_slayer_monster page: Drake→Guardian Drake, Wyrm→Shadow Wyrm, Lava Strykewyrm→Magma strykewyrm. Fossil Island wyverns and Skeletal wyverns have NO superior.

## Drakes
Base Slayer 84. Karuulm Slayer Dungeon (middle level). Requires boots of stone / boots of brimstone / granite boots (or elite Kourend & Kebos Diary) to avoid floor burn. Assigned by konar, nieve, duradel.

### Drake
- npc_ids: [8612, 8613]
- combat_level: 192
- defence_level: 120
- def_bonuses: stab 5 / slash 60 / crush 60 / magic 20 / range 100 (heavy 0)
- weakness: MELEE, best type stab (stab def 5 lowest), element Water 50; magic def 20 and heavy-ranged def 0 also low
- categories: dragon
- location(s): Karuulm Slayer Dungeon (middle level)
- requirement: heat-protection boots or elite K&K diary
- notes: Dragonbane weapons apply. No dragonfire protection needed but a periodic volcanic-breath special hits ~6-8x4 (mitigated by antifire/dragonfire shield or sidestepping). Normal ranged attack blocked by Protect from Missiles.

### Guardian Drake
- npc_ids: [10400]
- combat_level: 376
- defence_level: 200
- def_bonuses: stab 20 / slash 100 / crush 100 / magic 20 / range 150 (heavy 0)
- weakness: heavy-ranged def 0 lowest (RANGED, e.g. DHCB); stab 20 and magic 20 tie next; element Water 50
- categories: dragon
- location(s): Karuulm Slayer Dungeon (middle level)
- requirement: superior — Bigger and Badder unlock (50 Slayer pts); same heat-protection
- notes: Superior of Drake. Slayxp 7087.

## Wyrms
Base Slayer 62. Karuulm Slayer Dungeon (lower level). Requires heat-protection boots (or elite K&K diary). Assigned by chaeldar, konar, nieve, duradel. Cannon CANNOT be used vs standard Wyrms.

### Wyrm
- npc_ids: [8610, 8611]
- combat_level: 97
- defence_level: 80
- def_bonuses: stab 10 / slash 50 / crush 50 / magic 50 / range 20 (heavy 10)
- weakness: RANGED (range def 0 / heavy 10 lowest), best melee type stab (10); element Earth 50 (earth spells)
- categories: dragon
- location(s): Karuulm Slayer Dungeon (lower level + task-only sub-area)
- requirement: heat-protection boots or elite K&K diary
- notes: Uses Magic + Slash; at range uses only Magic, so Protect from Magic negates all. Immune to cannon, poison, venom. Dragonbane applies.

### Shadow Wyrm
- npc_ids: [10398, 10399]
- combat_level: 259
- defence_level: 125
- def_bonuses: stab 20 / slash 100 / crush 100 / magic 50 / range 0 (heavy 20)
- weakness: RANGED (range def 0), best melee type stab (20); element Earth 50
- categories: dragon
- location(s): Karuulm Slayer Dungeon (lower level)
- requirement: superior — Bigger and Badder unlock (50 Slayer pts)
- notes: Superior of Wyrm. Slayxp 3520. Cannot be safespotted; Protect from Magic negates at range. Cannot spawn from Wyrmlings.

### Wyrmling
- npc_ids: [13031, 13032]
- combat_level: 55
- defence_level: 40
- def_bonuses: stab 20 / slash 50 / crush 50 / magic 50 / range 0 (heavy 20)
- weakness: RANGED (range def 0), best melee type stab (20); element Earth 50
- categories: dragon
- location(s): Neypotzli — Earthbound Cavern
- requirement: alternative to Wyrm on task; requires partial completion of Perilous Moons (talisman enchantments) for spawn
- notes: Slayer 62. Half HP/Defence of normal Wyrm. Cannonable. Melee only (Slash). CANNOT spawn the Shadow Wyrm superior. Only drops wyrmling bones.

### Lava Strykewyrm
- npc_ids: [15500]
- combat_level: 116
- defence_level: 50
- def_bonuses: stab 30 / slash 60 / crush 70 / magic 40 / range 120
- weakness: MELEE, best type stab (30 lowest); element Water 50 (water spells; FIRE spells HEAL it)
- categories: dragon
- location(s): Charred Dungeon, beneath Charred Island
- requirement: alternative to Wyrm on task; requires 60 Sailing
- notes: Slayer 62. Uses Melee (Crush) within 1 tile, Ranged at distance — so Protect from Missiles at range. Immune to cannon and normal burns. Dragonbane applies. Dragon metal sheet drop (much more common on Wyrm task). Safespot needs 9-10 tile attack range.

### Magma strykewyrm
- npc_ids: [15504]
- combat_level: 249
- defence_level: 110
- def_bonuses: stab 60 / slash 30 / crush 70 / magic 40 / range 120
- weakness: MELEE, best type slash (30 lowest); element Water 50
- categories: dragon
- location(s): Charred Dungeon
- requirement: superior — Bigger and Badder unlock (50 Slayer pts); spawns from Lava Strykewyrm
- notes: Superior of Lava Strykewyrm. Slayxp 3655. Immune to cannon and normal burns. Can burrow and target player's tile for 20+ damage.

## Fossil Island Wyverns
All require Slayer level shown, reside in Wyvern Cave (Fossil Island), and need an elemental/mind/dragonfire/ancient-wyvern shield to reduce icy breath. Task requires Bone Voyage quest. Assigned by chaeldar, konar, nieve, duradel. Magic defence is very high on all — avoid Magic; use melee/ranged. Drop wyvern visage (not draconic visage).

### Spitting Wyvern
- npc_ids: [7794]
- combat_level: 139
- defence_level: 90
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 140 / range 70 (light/heavy 120)
- weakness: MELEE, best type stab (50 lowest); element Air 25; AVOID magic (def 140)
- categories: dragon
- location(s): Wyvern Cave, Fossil Island
- requirement: Slayer 66; Bone Voyage; anti-icy-breath shield
- notes: Uses Slash, Ranged, Icy breath.

### Taloned Wyvern
- npc_ids: [7793]
- combat_level: 147
- defence_level: 90
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 140 / range 70 (light/heavy 120)
- weakness: MELEE, best type stab (50 lowest); element Air 35; AVOID magic (def 140)
- categories: dragon
- location(s): Wyvern Cave, Fossil Island
- requirement: Slayer 66; Bone Voyage; anti-icy-breath shield
- notes: Uses Slash, Typeless Magic (ignores prayer, up to 10), Icy breath.

### Long-tailed Wyvern
- npc_ids: [7792]
- combat_level: 152
- defence_level: 90
- def_bonuses: stab 70 / slash 70 / crush 70 / magic 140 / range 120 (heavy 70)
- weakness: MELEE (stab/slash/crush all 70), best type stab; element Air 25; AVOID magic (def 140)
- categories: dragon
- location(s): Wyvern Cave, Fossil Island
- requirement: Slayer 66; Bone Voyage; anti-icy-breath shield
- notes: Uses Slash, Typeless Magic, Icy breath.

### Ancient Wyvern
- npc_ids: [7795]
- combat_level: 210
- defence_level: 150
- def_bonuses: stab 50 / slash 70 / crush 70 / magic 170 / range 120 (heavy 90)
- weakness: MELEE, best type stab (50 lowest); element Air 35; AVOID magic (def 170)
- categories: dragon
- location(s): Wyvern Cave, Fossil Island (Slayer-task-only sub-area + open area)
- requirement: Slayer 82; Bone Voyage; anti-icy-breath shield
- notes: Strongest wyvern species. Uses Slash, Typeless Magic, Icy breath (up to 44, or 10 with elemental shield). Drops wyvern visage (1/10000).

## Skeletal Wyverns
Base Slayer 72. Asgarnian Ice Dungeon (end). Requires elemental/mind/dragonfire/ancient-wyvern shield for icy breath, and Elemental Workshop I quest is required before they are assigned. Assigned by chaeldar, konar, nieve, duradel.

### Skeletal Wyvern
- npc_ids: [468, 465, 466, 467]
- combat_level: 140
- defence_level: 120
- def_bonuses: stab 140 / slash 90 / crush 90 / magic 80 / range 140
- weakness: MAGIC (magic def 80 lowest); best melee type slash or crush (90) — AVOID stab (def 140); element Fire 25
- categories: dragon
- location(s): Asgarnian Ice Dungeon (main + task-only upper area)
- requirement: Slayer 72; Elemental Workshop I; anti-icy-breath shield
- notes: NOT undead (magically animated remains) — Salve amulet does NOT work. Dragonbane + dragonfire both effective (rare combo); DHL recommended on pound/swipe (crush) due to high stab def. Drops draconic visage (not wyvern visage) and granite legs. Regenerates 1 HP / 10 ticks.

## Gaps
- None. All 11 variants enumerated with full stats from their own infoboxes.
- Versioned infoboxes (Wyrm, Shadow Wyrm, Wyrmling, Skeletal Wyvern) share identical stats across versions (idle/attacking/animation states), so one row each; all NPC IDs listed.
- Range defence on newer monsters is split into light/standard/heavy; `range` field uses standard, with notable light/heavy divergences noted inline (e.g. Drake/Guardian Drake heavy-ranged def 0).

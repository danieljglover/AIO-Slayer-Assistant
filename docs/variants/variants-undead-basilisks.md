# Undead / Basilisk-task variants

Source: OSRS Wiki (oldschool.runescape.wiki), `?action=raw` infoboxes + `Slayer_task/*` pages. Fetched 2026-06-29.

Weakness derivation: lowest defensive bonus among stab/slash/crush/magic/range, plus any `elementalweakness`. Categories set `undead: true` ONLY where the monster `attributes` field literally contains `undead`.

Key category findings:
- Aberrant/Deviant spectres + superiors: attributes = `undead, spectral` -> undead TRUE (confirms team memory).
- Ankou + Dark Ankou: attributes = `undead, spectral` -> undead TRUE.
- Vampyres (Feral/Juvenile/Juvinate/Vyrewatch/Sentinel): attributes = `vampyre1/2/3` -> NOT undead (confirms team memory: vampyres are not flagged undead).
- Basilisks (all): NO attributes field -> NOT undead, no special category.

---

## Aberrant spectres
Source: Slayer_task/Aberrant_spectres. Nose peg OR Slayer helmet REQUIRED (special attack ignores prayer otherwise). Salve amulet works (undead).

### Aberrant spectre
- npc_ids: [2,3,4,5,6,7]
- combat_level: 96
- defence_level: 90
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 0 / range UNKNOWN (not set)
- weakness: MAGIC (lowest def is magic 0), element Air 50%
- categories: [undead, spectral]
- location(s): Slayer Tower (floor 1), Stronghold Slayer Cave, Deepfin Mine
- requirement: Priest in Peril (quest); nose peg or Slayer helmet (item)
- notes: max hit 8 magic; Protect from Magic negates. HP 90.

### Abhorrent spectre
- npc_ids: [7402]
- combat_level: 253
- defence_level: 180
- def_bonuses: stab 40 / slash 40 / crush 40 / magic 0 / range UNKNOWN
- weakness: MAGIC (magic 0), element Air 50%
- categories: [undead, spectral]
- location(s): Slayer Tower, Stronghold Slayer Cave, Deepfin Mine
- requirement: superior — Bigger and Badder unlock (50 pts); same nose peg/helm requirement
- notes: Superior of Aberrant spectre. Max hit 33. HP 250.

### Deviant spectre
- npc_ids: [7279]
- combat_level: 169
- defence_level: 90
- def_bonuses: stab 80 / slash 80 / crush 80 / magic 0 / range UNKNOWN
- weakness: MAGIC (magic 0), element Air 30%
- categories: [undead, spectral]
- location(s): Catacombs of Kourend
- requirement: Priest in Peril; nose peg or Slayer helmet
- notes: Higher-defence Catacombs alternative. Aggressive. HP 190.

### Repugnant spectre
- npc_ids: [7403]
- combat_level: 335
- defence_level: 220
- def_bonuses: stab 120 / slash 120 / crush 120 / magic 0 / range UNKNOWN
- weakness: MAGIC (magic 0), element Air 40%
- categories: [undead, spectral]
- location(s): Catacombs of Kourend
- requirement: superior — Bigger and Badder; nose peg/helm
- notes: Superior of Deviant spectre. Always drops a Dark totem piece. HP 380.

---

## Ankou
Source: Slayer_task/Ankous. Undead (Salve amulet works). Melee attacker; Protect from Melee negates. No nose peg/shield requirement.

### Ankou (Level 75)
- npc_ids: [2514,2517]
- combat_level: 75
- defence_level: 60
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: MAGIC (all melee/magic def equal 0; element Air 40% breaks tie), element Air 40%
- categories: [undead, spectral]
- location(s): Sepulchre of Death (Stronghold of Security), Stronghold Slayer Cave, Catacombs of Kourend, Deepfin Mine
- requirement: none
- notes: HP 60. Def bonuses shared across all versions (single dstab/etc block).

### Ankou (Level 82)
- npc_ids: [2515,2518]
- combat_level: 82
- defence_level: 70
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: MAGIC, element Air 40%
- categories: [undead, spectral]
- location(s): as above
- requirement: none
- notes: HP 65.

### Ankou (Level 86)
- npc_ids: [2516,2519,6608]
- combat_level: 86
- defence_level: 80
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: MAGIC, element Air 40%
- categories: [undead, spectral]
- location(s): as above (incl. Wilderness Slayer Cave / Forgotten Cemetery tier)
- requirement: none
- notes: HP 70.

### Ankou (Level 95)
- npc_ids: [7257]
- combat_level: 95
- defence_level: 100
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: MAGIC, element Air 40%
- categories: [undead, spectral]
- location(s): Catacombs of Kourend
- requirement: none
- notes: HP 60.

### Ankou (Level 98)
- npc_ids: [7864]
- combat_level: 98
- defence_level: 80
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: MAGIC, element Air 40%
- categories: [undead, spectral]
- location(s): Wilderness Slayer Cave
- requirement: none (Wilderness — PKer risk)
- notes: HP 100.

### Dark Ankou
- npc_ids: [7296]
- combat_level: 95
- defence_level: 100
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: UNKNOWN (all def bonuses 0, no elementalweakness set — no clear lean)
- categories: [undead, spectral]
- location(s): Catacombs of Kourend (Skotizo fight only)
- requirement: boss — summoned during Skotizo; separate trip / pro-forma task kill
- notes: HP 60. Not the assigned "superior"; only spawns in Skotizo fight. No dedicated superior exists for Ankou task.

---

## Vampyres
Source: Slayer_task/Vampyres. Priest in Peril + (usually) "Actual Vampyre Slayer" unlock for high masters. NOT undead (vampyre attribute). Blisterwood flail (or Ivandis flail) needed for Vyrewatch/Sentinel; Juvinate needs silver/blisterwood/efaritay's aid; Feral can use any weapon. All listed variants have def_bonuses 0 across the board and no elementalweakness -> weakness UNKNOWN (no defensive lean); damage is gated by special anti-vampyre weapons rather than style.

### Feral Vampyre
- npc_ids: lvl61 [3237], lvl64 [3707,3708,4431], lvl70 [5640], lvl72 [3234], lvl77 [3137], lvl100 [5641], lvl130 [5642]
- combat_level: 61 / 64 / 70 / 72 / 77 / 100 / 130 (versioned)
- defence_level: 55 / 60 / 30 / 65 / 81 / 30 / 30 (per version order above)
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: UNKNOWN (all def 0, no element)
- categories: [vampyre] (NOT undead)
- location(s): God Wars Dungeon, Haunted Woods, West of Burgh de Rott, Wilderness God Wars Dungeon, Temple Trekking/Burgh de Rott Ramble
- requirement: Priest in Peril (most locations); any weapon works
- notes: HP 40/80/75/50/60/135/185. Guaranteed vampyre dust drop. Lvl64 is Juvenile/Juvinate-trek variant.

### Vampyre Juvenile
- npc_ids: Burgh/Crombwick [4436,4437,4438,4439], Meiyerditch [3692,3693,3696,3697], Darkmeyer [9731,9732,9733,9734]
- combat_level: 45
- defence_level: 30
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: UNKNOWN (all def 0)
- categories: [vampyre] (NOT undead)
- location(s): Darkmeyer, East of Burgh de Rott, Meiyerditch, Crombwick Manor
- requirement: any weapon to damage, but retreat at low HP unless held (Rod of ivandis / Ivandis flail special)
- notes: HP 60. Generally does NOT give Slayer xp / task credit unless instakilled by Guthix balance or its feral form is slain — not recommended.

### Vampyre Juvinate
- npc_ids: lvl50 [4443,4486], lvl54-Burgh [4427,4428,4429,4430,4432,4487], lvl54-Meiyerditch [3694,3695,3698,3699], lvl54-Darkmeyer [9727,9728,9729,9730], lvl59 [5634,5637], lvl75 [4442], lvl90 [5635,5638], lvl119-trek [5636,5639], lvl119-SotF [9614,9615,9616,9617]
- combat_level: 50 / 54 / 54 / 54 / 59 / 75 / 90 / 119 / 119 (versioned)
- defence_level: 30 / 30 / 30 / 30 / 45 / 35 / 55 / 65 / 65 (per version order)
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: UNKNOWN (all def 0)
- categories: [vampyre] (NOT undead)
- location(s): Darkmeyer, East of Burgh de Rott, Meiyerditch Mine, Ver Sinhaza (mostly lvl54)
- requirement: silver weaponry OR efaritay's aid to damage
- notes: HP 60/85/85/85/50/110/100/150/150. Lvl54 (65 xp) is the common task variant.

### Vyrewatch
- npc_ids: lvl87 [8252-8259], lvl105 [3709..9735 set], lvl110 [3710,3714,3718,3722,3726,3730,3749,3753,3757,3761], lvl120 [3711,3715,3719,3723,3727,3731,3750,3754,3758,3762], lvl125 [3712,3716,3720,3724,3728,3732,3751,3755,3759,3763]
- combat_level: 87 / 105 / 110 / 120 / 125 (versioned)
- defence_level: 75 / 85 / 85 / 85 / 85 (per version order)
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: UNKNOWN (all def 0)
- categories: [vampyre] (NOT undead)
- location(s): Darkmeyer, Meiyerditch, Slepe
- requirement: A Taste of Hope (quest, lvl38 Slayer); Ivandis flail OR Blisterwood flail to damage
- notes: HP 75/90/90/105/110. Aggressive without Vyre noble disguise.

### Vyrewatch Sentinel
- npc_ids: [9756,9757,9760,9762,9763,9759,9758,9761]
- combat_level: 151
- defence_level: 180
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range UNKNOWN
- weakness: UNKNOWN (all def 0)
- categories: [vampyre] (NOT undead)
- location(s): Darkmeyer
- requirement: Sins of the Father (quest, lvl50 Slayer); Ivandis or Blisterwood flail to damage
- notes: HP 150. Best variant — blood shard drop. Aggressive without full Vyre noble.

---

## Basilisks
Source: Slayer_task/Basilisks. Slayer 40, Defence 20. Mirror shield OR V's shield REQUIRED (gaze drains stats / heavy damage otherwise). NOT undead (no attributes). Crush is consistently the lowest defensive bonus -> MELEE (crush). No cannons anywhere.

### Basilisk
- npc_ids: [417]
- combat_level: 61
- defence_level: 75
- def_bonuses: stab 20 / slash 20 / crush 0 / magic 20 / range UNKNOWN
- weakness: MELEE (crush, lowest at 0); element Earth 40%
- categories: [] (not undead)
- location(s): Fremennik Slayer Dungeon, Jormungand's Prison
- requirement: mirror shield or V's shield (item)
- notes: HP 75. Max hit 5, very inaccurate melee.

### Monstrous basilisk
- npc_ids: [7395]
- combat_level: 135
- defence_level: 130
- def_bonuses: stab 35 / slash 35 / crush 0 / magic 35 / range UNKNOWN
- weakness: MELEE (crush 0); no elementalweakness set
- categories: [] (not undead)
- location(s): Fremennik Slayer Dungeon, Jormungand's Prison
- requirement: superior — Bigger and Badder unlock; mirror/V's shield
- notes: Superior of Basilisk. HP 170.

### Basilisk Knight
- npc_ids: [9293]
- combat_level: 204
- defence_level: 186
- def_bonuses: stab 30 / slash 30 / crush -15 / magic 30 / range UNKNOWN
- weakness: MELEE (crush -15, lowest); element Earth 40%
- categories: [] (not undead)
- location(s): Jormungand's Prison
- requirement: The Fremennik Exiles (quest); mirror/V's shield
- notes: HP 300. Melee + magic attacks, both max 20; matches player attack speed (min 4 ticks). Drops Basilisk jaw (1/1000 on task).

### Basilisk Sentinel
- npc_ids: [9258]
- combat_level: 358
- defence_level: 274
- def_bonuses: stab 50 / slash 50 / crush 10 / magic 50 / range UNKNOWN
- weakness: MELEE (crush 10, lowest); element Earth 40%
- categories: [] (not undead)
- location(s): Jormungand's Prison
- requirement: superior — Bigger and Badder; mirror/V's shield
- notes: Superior of Basilisk Knight. HP 520. Stone-encasing special attack (sidestep one tile).

---

## Gaps
- range defensive bonus (`drange`) is not set on any of these infoboxes — all UNKNOWN.
- Vampyre task `The Jormungand` is NOT a vampyre (basilisk page); listed below for basilisks: The Jormungand (cmb 363, Jormungand's Prison) is a one-time Fremennik Exiles quest boss, not a repeatable task variant — intentionally excluded from variant list.
- Vampyre/Juvenile/Juvinate/Vyrewatch/Sentinel have zero defensive bonuses and no elementalweakness, so combat-style weakness is genuinely UNKNOWN (damage gated by silver/Ivandis/Blisterwood weapons, not style).
- Dark Ankou weakness UNKNOWN (all def 0, no element); it is a Skotizo-summoned add, not a true superior. Ankou task has no superior monster.
- Quest/NMZ exception vampyres (Dessous, Ranis Drakan, Damien Leucurte, Kroy, Count Draynor) grant task credit but are not enumerated as standing task variants.

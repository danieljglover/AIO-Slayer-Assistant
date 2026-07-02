# Boss-task variants (meta-task)

Source: https://oldschool.runescape.wiki/w/Boss (raw, "Boss slayer" section) + each boss's own
`?action=raw` {{Infobox Monster}}, fetched 2026-06-29.

The Duradel **Boss** task is a meta-task: with the "Like a boss" unlock (200 pts) the master rolls ONE boss
from the pool below for which the player meets the requirements. Each is a separate trip with bespoke,
boss-specific gear. **The user has decided Boss IS in scope for variant selection** (board task MV-R2): when
the assigned task is Boss, the selected "variant" IS the specific rolled boss, and the loadout drives off that
boss's per-variant profile. Per-boss profiles are therefore now enumerated below (this supersedes the earlier
"intentionally not enumerated" scope note).

Duradel boss-task weight = 12. Amount 3-35 (up to 65 via Combat Achievement tiers; Barrows 3-36 → 72).

Defence-weakness derivation (same as the other variant files): the style facing the LOWEST defensive bonus
among stab/slash/crush/magic/range, plus any `elementalweakness`. Where the infobox splits ranged defence into
`dlight`/`dstandard`/`dheavy` (identical on these NPCs) it is collapsed to a single `range` value. Where a
per-style bonus is unset on the infobox it is recorded UNKNOWN — nothing invented.

## Assignable boss pool (Duradel / any non-Konar-exclusive)
Each row: boss | location | Slayer req | other req / substitute. `category flag` corrected against each boss's
own infobox `attributes` (see profiles). Flags follow the team rule: a category is set ONLY where the infobox
`attributes` literally contains it.

| Boss | Location | Slayer | Other req / notes | category flag (verified) |
|------|----------|--------|-------------------|--------------------------|
| Abyssal Sire | Abyssal Nexus | 85 | Enter the Abyss OR Fairytale II partial | demon |
| Alchemical Hydra | Mount Karuulm Dungeon | 95 | **Konar only** (not Duradel) | dragon (draconic/dragonbane) |
| Araxxor | Morytania Spider Cave | 92 | Priest in Peril | - |
| Barrows brothers | Morytania | 1 | Priest in Peril | **NONE** (attributes=spectral, NOT undead — see gaps) |
| Callisto | Callisto's Den (Wildy) | 1 | Artio killable instead | - |
| Cerberus | Cerberus' Lair | 91 | - | demon |
| Chaos Elemental | Wilderness | 1 | - | - |
| Chaos Fanatic | Wilderness | 1 | - | - |
| Commander Zilyana | God Wars Dungeon | 1 | Agility 70, Death Plateau; 40 Saradomin KC | - |
| Crazy archaeologist | Wilderness | 1 | Deranged archaeologist killable instead | - |
| Dagannoth Kings | Waterbirth Island | 1 | - | - (no attributes line; per-king weakness differs) |
| Duke Sucellus | Ghorrock Prison | 1 | Desert Treasure II | demon (attributes=Demon) |
| General Graardor | God Wars Dungeon | 1 | Strength 70, Death Plateau; 40 Bandos KC | - |
| Giant Mole | Mole Lair | 1 | - | - |
| Grotesque Guardians | Slayer Tower roof | 75 | Brittle key, gargoyle task, Priest in Peril | - (attributes=Golem/Flying) |
| K'ril Tsutsaroth | God Wars Dungeon | 1 | Hitpoints 70, Death Plateau; 40 Zamorak KC | demon |
| Kalphite Queen | Kalphite Lair | 1 | - | kalphite |
| King Black Dragon | KBD Lair | 1 | Dragon Slayer I | dragon (draconic/dragonbane) |
| Kraken | Kraken Cove | 87 | - | - |
| Kree'arra | God Wars Dungeon | 1 | Ranged 70, Death Plateau; 40 Armadyl KC | - (attributes=flying) |
| The Leviathan | The Scar | 1 | Desert Treasure II | - |
| Phantom Muspah | Ghorrock Dungeon | 1 | Secrets of the North | - (attributes=spectral) |
| Sarachnis | Forthos Dungeon | 1 | Reached Great Kourend by boat | - |
| Scorpia | Wilderness (Scorpion Pit) | 1 | - | - |
| Shellbane gryphon | Shellbane Gryphon Cave | 51 | Troubled Tortugans | - |
| Thermonuclear smoke devil | Smoke Devil Dungeon | 93 | - | - |
| Vardorvis | The Stranglewood | 1 | Desert Treasure II | - |
| Venenatis | Silk Chasm (Wildy) | 1 | Spindel killable instead | - |
| Vet'ion | Vet'ion's Rest (Wildy) | 1 | Calvar'ion killable instead | **undead** (confirmed) |
| Vorkath | Ungael | 18 | Dragon Slayer II | **dragon + undead** (attributes=dragon,undead,fiery) |
| The Whisperer | Lassar Undercity | 1 | Desert Treasure II | - |
| Zulrah | Poison Waste | 1 | Regicide | - (snake, not draconic — see gaps) |

Total pool: 32 bosses (1 of which, Alchemical Hydra, is Konar-exclusive — Duradel can roll the other 31).

## Bosses explicitly NOT assignable as a Boss task (per wiki)
Corporeal Beast, Hespori, Skotizo (killable on Black/Greater demon task), Obor (hill giant task), Bryophyta
(moss giant task), The Mimic, The Nightmare/Phosani's, Nex, Scurrius (rats task), Perilous Moons, The
Hueycoatl, Amoxliatl (lesser nagua task), Royal Titans / Branda the Fire Queen + Eldric the Ice King
(fire/ice giant task), Yama, Doom of Mokhaiotl, all raid bosses (except CoX lizardman shamans / skeletal
mystics on the matching task).

---

## Per-boss profiles

One `### <Boss>` block per assignable boss, same field format as the other variant files. `isBoss: true` for
all. Multi-form/multi-NPC bosses list a `def_bonuses` line per form or a sub-block per NPC.

### Abyssal Sire
- npc_ids: phases 1-3 [5886, 5887, 5888, 5889, 5890, 5891]; final stage [5908]
- combat_level: 350
- defence_level: 250
- def_bonuses (phases 1-3): stab 40 / slash 60 / crush 50 / magic 20 / range 60
- def_bonuses (final stage / 5908): stab 20 / slash 30 / crush 25 / magic -40 / range 30
- weakness: MAGIC (magic lowest in every form — 20 then -40); no elementalweakness set
- categories: [demon]
- location(s): The Abyssal Nexus
- requirement: Slayer 85; Enter the Abyss miniquest OR partial Fairytale II for Abyss access
- isBoss: true
- notes: HP 425/form. Max hit 66 melee / 96 with explosion. Final stage has negative magic defence.

### Alchemical Hydra
- npc_ids: serpentine [8615, 8616]; electric [8619, 8617]; lava [8620, 8618]; final [8621, 8622]
- combat_level: 426
- defence_level: 100
- def_bonuses (all 4 phases identical): stab 75 / slash 150 / crush 150 / magic 150 / range 45
- weakness: RANGED (range 45 lowest); element Earth 50%
- categories: [dragon] (draconic — dragonbane applies)
- location(s): Karuulm Slayer Dungeon (lower level), Mount Karuulm
- requirement: Slayer 95; on a Hydra task. **Konar-exclusive as a Boss roll (Duradel cannot roll it).**
- isBoss: true
- notes: HP 1100. 4 phases, defence constant across them. Max hit varies by phase (17×2 → 55).

### Araxxor
- npc_ids: [13668]
- combat_level: 890
- defence_level: UNKNOWN (def level not on infobox; bonuses present)
- def_bonuses: stab 160 / slash 75 / crush 15 / magic 237 / range 218
- weakness: MELEE (crush 15 lowest); element Fire 50%
- categories: []
- location(s): Morytania Spider Cave (Quetzacalli area entrance)
- requirement: Slayer 92; Priest in Peril (Morytania access)
- isBoss: true
- notes: HP 1020. Enrage at 25% HP (+35 Defence, 6→4 tick speed, melee 1×3 cleave). Venomous; cannon-immune.

### Barrows brothers
**CATEGORY CORRECTION:** every brother's infobox `attributes = spectral`, NOT `undead` (verified directly on
two pages). Per the team rule (flag undead only where `attributes` literally contains undead) they are NOT
flagged undead here — this contradicts the earlier table and common Salve assumption. See gaps for the
Architect call. All six: HP 100, defence_level 100, combat-achievement/quest req = members + Priest in Peril,
element Air 50%, ranged defence given via dlight/dstandard/dheavy (collapsed below).

#### Ahrim the Blighted
- npc_ids: [1672]
- combat_level: 98
- defence_level: 100
- def_bonuses: stab 103 / slash 85 / crush 117 / magic 73 / range 0
- weakness: RANGED (range 0 lowest); element Air 50%
- categories: [] (attributes=spectral)
- isBoss: true
- notes: Magic attacker (speed 6). Max hit 20.

#### Dharok the Wretched
- npc_ids: [1673]
- combat_level: 115
- defence_level: 100
- def_bonuses: stab 252 / slash 250 / crush 244 / magic -11 / range 249
- weakness: MAGIC (magic -11 lowest); element Air 50%
- categories: [] (attributes=spectral)
- isBoss: true
- notes: Melee attacker; hits harder the lower his HP. Max hit 29 (57 at 1 HP).

#### Guthan the Infested
- npc_ids: [1674]
- combat_level: 115
- defence_level: 100
- def_bonuses: stab 259 / slash 257 / crush 241 / magic -11 / range 250
- weakness: MAGIC (magic -11 lowest); element Air 50%
- categories: [] (attributes=spectral)
- isBoss: true
- notes: Melee attacker; his hits heal him. Max hit 24.

#### Karil the Tainted
- npc_ids: [1675]
- combat_level: 98
- defence_level: 100
- def_bonuses: stab 79 / slash 71 / crush 90 / magic 106 / range 100
- weakness: MELEE (slash 71 lowest); element Air 50%
- categories: [] (attributes=spectral)
- isBoss: true
- notes: Ranged attacker. Max hit 20.

#### Torag the Corrupted
- npc_ids: [1676]   (wiki page is "Torag the Corrupted", not "...Corrupt")
- combat_level: 115
- defence_level: 100
- def_bonuses: stab 221 / slash 235 / crush 222 / magic 0 / range 221
- weakness: MAGIC (magic 0 lowest); element Air 50%
- categories: [] (attributes=spectral)
- isBoss: true
- notes: Melee attacker; drains run energy. Max hit 23.

#### Verac the Defiled
- npc_ids: [1677]
- combat_level: 115
- defence_level: 100
- def_bonuses: stab 227 / slash 230 / crush 221 / magic 0 / range 225
- weakness: MAGIC (magic 0 lowest); element Air 50%
- categories: [] (attributes=spectral)
- isBoss: true
- notes: Melee attacker; can hit through Protect from Melee. Max hit 23 (15 through prayer).

### Callisto
- npc_ids: [6609]
- combat_level: 470
- defence_level: 225
- def_bonuses: stab 150 / slash 130 / crush 125 / magic 0 / range 50
- weakness: MAGIC (magic 0 lowest); element Fire 30%
- categories: []
- location(s): Callisto's Den, ~Level 40+ Wilderness
- requirement: none. Singles-plus alternative: Artio.
- isBoss: true
- notes: HP 1000. Max hit 55 crush / 31 ranged / 50 special. Cannon-immune, aggressive.

### Cerberus
- npc_ids: [5862, 5863, 5866]
- combat_level: 318
- defence_level: 100
- def_bonuses: stab 50 / slash 100 / crush 25 / magic 65 / range 100
- weakness: MELEE (crush 25 lowest); element Water 40%
- categories: [demon]
- location(s): Cerberus's Lair, beneath Taverley Dungeon
- requirement: Slayer 91; on a hellhound/Cerberus task
- isBoss: true
- notes: HP 600. Max hit 23. Summons Summoned Souls; ghost/lava/spectral mechanics.

### Chaos Elemental
- npc_ids: [2054]
- combat_level: 305
- defence_level: 270
- def_bonuses: stab 70 / slash 70 / crush 70 / magic 70 / range 70 (all equal)
- weakness: MAGIC via element Air 50% (all defensive bonuses tied at 70 — no defensive lean; element breaks tie)
- categories: []
- location(s): West of the Rogues' Castle, deep Wilderness
- requirement: none
- isBoss: true
- notes: HP 250. Max hit 28. Disarms/teleports players.

### Chaos Fanatic
- npc_ids: [6619]
- combat_level: 202
- defence_level: 220
- def_bonuses: stab 260 / slash 260 / crush 250 / magic 280 / range 50
- weakness: RANGED (range 50 lowest); no elementalweakness set
- categories: []
- location(s): West of the Lava Maze, ~Level 38+ Wilderness
- requirement: none
- isBoss: true
- notes: HP 225. Max hit 31. Magic-based attacker.

### Commander Zilyana
- npc_ids: [2205]
- combat_level: 596
- defence_level: 300
- def_bonuses: stab 100 / slash 100 / crush 100 / magic 100 / range 100 (all equal)
- weakness: UNKNOWN — all five defensive bonuses tied at 100, no elementalweakness set (style driven by accuracy/setup)
- categories: []
- location(s): God Wars Dungeon — Saradomin's Encampment
- requirement: Agility 70 + Death Plateau (GWD access); 40 Saradomin KC
- isBoss: true
- notes: HP 255. Max hit 27 melee / 20 magic. Bodyguards Starlight/Growler/Bree.

### Crazy archaeologist
- npc_ids: [6618]
- combat_level: 204
- defence_level: 240
- def_bonuses: stab 5 / slash 5 / crush 30 / magic 250 / range 250
- weakness: MELEE (stab/slash tied 5 lowest); no elementalweakness set
- categories: []
- location(s): Ruins south of the Forgotten Cemetery, ~Level 23 Wilderness
- requirement: none. Counterpart: Deranged archaeologist (Fossil Island, non-Wildy).
- isBoss: true
- notes: HP 225. Max hit 14 (24 special "Rain of Knowledge").

### Dagannoth Kings
Three separate NPCs, each weak to a different style (the classic tri-bracket). No `attributes` line on any.
All combat 303, HP 255, Waterbirth Island Dungeon, element Earth 35%, members, no minimum Slayer level.

#### Dagannoth Rex
- npc_ids: [2267]
- combat_level: 303
- defence_level: 255
- def_bonuses: stab 255 / slash 255 / crush 255 / magic 10 / range 255
- weakness: MAGIC (magic 10 lowest); element Earth 35%
- categories: []
- isBoss: true
- notes: Melee attacker (speed 4). Max hit 26.

#### Dagannoth Prime
- npc_ids: [2266]
- combat_level: 303
- defence_level: 255
- def_bonuses: stab 255 / slash 255 / crush 255 / magic 255 / range 10
- weakness: RANGED (range 10 lowest); element Earth 35%
- categories: []
- isBoss: true
- notes: Magic attacker (speed 4). Max hit 50.

#### Dagannoth Supreme
- npc_ids: [2265]
- combat_level: 303
- defence_level: 128
- def_bonuses: stab 10 / slash 10 / crush 10 / magic 255 / range 550
- weakness: MELEE (stab/slash/crush tied 10 lowest); element Earth 35%
- categories: []
- isBoss: true
- notes: Ranged attacker (speed 4). Max hit 30.

### Duke Sucellus
- npc_ids: [12191] (post-quest), [12195] (quest)
- combat_level: 758 (post-quest) / 538 (quest) / 1099 (Awakened)
- defence_level: 275 (post-quest) / 215 (quest) / 316 (Awakened)
- def_bonuses (post-quest): stab 255 / slash 45 / crush 190 / magic 440 / range UNKNOWN (drange not on infobox)
- weakness: MELEE (slash 45 lowest); no elementalweakness set
- categories: [demon] (attributes=Demon)
- location(s): Ghorrock Prison Asylum (Ghorrock Dungeon, via Weiss salt mine)
- requirement: Desert Treasure II - The Fallen Empire; no Slayer level
- isBoss: true
- notes: HP 485/330/1697. Max hit 56 melee / 48 magic / 101 gaze (post-quest). drange UNKNOWN — see gaps.

### General Graardor
- npc_ids: [2215]
- combat_level: 624
- defence_level: 250
- def_bonuses: stab 90 / slash 90 / crush 90 / magic 298 / range 100
- weakness: MELEE (stab/slash/crush tied 90 lowest); element Earth 40%
- categories: []
- location(s): God Wars Dungeon — Bandos's Stronghold
- requirement: Strength 70 + Death Plateau (GWD access); 40 Bandos KC
- isBoss: true
- notes: HP 255. Max hit 60 melee / 35 ranged. Very high magic defence (298). Bodyguards Strongstack/Steelwill/Grimspike.

### Giant Mole
- npc_ids: [5779]
- combat_level: 230
- defence_level: 200
- def_bonuses: stab 60 / slash 80 / crush 100 / magic 80 / range 60
- weakness: MELEE-stab / RANGED tied (stab 60, range 60 lowest); element Earth 50%
- categories: []
- location(s): Mole Hole (under Falador Park)
- requirement: none
- isBoss: true
- notes: HP 200. Max hit 21. Burrows/relocates during fight.

### Grotesque Guardians
Two NPCs fought together (Dusk + Dawn). Both HP 450, Slayer Tower rooftop, Slayer 75 + active gargoyle task +
brittle key (+ Priest in Peril for Morytania). Attributes are Golem/Flying — no tracked category flag.

#### Dusk
- npc_ids: form1 [7851, 7854, 7855, 7882, 7883, 7886]; form2 [7887, 7888, 7889]
- combat_level: 248 (form1) / 328 (form2)
- defence_level: 100 (form1) / 150 (form2)
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 0 / range 0 (all equal)
- weakness: UNKNOWN by defence (all 0); element Earth 40%
- categories: [] (attributes=Golem)
- isBoss: true
- notes: HP 450. Max hit 15 melee / 33 special (form1); 26 melee / 15×2 ranged / 65 special (form2).

#### Dawn
- npc_ids: [7852, 7853, 7884, 7885]
- combat_level: 228
- defence_level: 100
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 80 / range 0
- weakness: MELEE / RANGED (stab/slash/crush/range tied 0, below magic 80); element Earth 70%
- categories: [] (attributes=Flying, Golem)
- isBoss: true
- notes: HP 450. Max hit 15. Flying (halberd-type melee needed).

### K'ril Tsutsaroth
- npc_ids: [3129]
- combat_level: 650
- defence_level: 270
- def_bonuses: stab 70 / slash 80 / crush 80 / magic 80 / range 80
- weakness: MELEE (stab 70 lowest); element Water 30%
- categories: [demon]
- location(s): God Wars Dungeon — Zamorak's Fortress
- requirement: Hitpoints 70 + Death Plateau (GWD access); 40 Zamorak KC
- isBoss: true
- notes: HP 255. Max hit 30 magic / 46 melee / 49 special. Bodyguards incl. Balfrug Kreeyath.

### Kalphite Queen
- npc_ids: first form (crawling/melee) [963, 4303]; second form (airborne) [965, 4304]
- combat_level: 333 (both forms)
- defence_level: 300 (both forms)
- def_bonuses: stab 50 / slash 50 / crush 10 / magic 100 / range 0 (infobox uses paired dstab1/dstab2 split per form; range 0)
- weakness: RANGED (range 0 lowest); element Fire 40%
- categories: [kalphite]
- location(s): Kalphite Lair (Kharidian Desert)
- requirement: none mandatory (rope access); often a Kalphite task
- isBoss: true
- notes: HP 255. Max hit 31. 2 forms — first melee-focused (crawling), second ranged/magic (airborne). Per-style
  defence listed as paired split values on the infobox; verify exact per-form split live if needed.

### King Black Dragon
- npc_ids: [239, 2642]
- combat_level: 276
- defence_level: 240
- def_bonuses: stab 40 / slash 90 / crush 90 / magic 80 / range 40
- weakness: MELEE-stab / RANGED tied (stab 40, range 40 lowest); element Water 50%
- categories: [dragon] (also "fiery"; draconic — dragonbane applies)
- location(s): King Black Dragon Lair (Wilderness instance via lever)
- requirement: Dragon Slayer I (per pool table); no Slayer level
- isBoss: true
- notes: HP 240. Max hit 25 melee / 65 dragonfire. Single form.

### Kraken
- npc_ids: [494], whirlpool [496]
- combat_level: 291
- defence_level: UNKNOWN (def level not on infobox; bonuses present)
- def_bonuses: stab 0 / slash 0 / crush 0 / magic 130 / range 300
- weakness: MELEE (stab/slash/crush tied 0 lowest); element Earth 50%
- categories: []
- location(s): Kraken Cove
- requirement: Slayer 87
- isBoss: true
- notes: HP 255. Max hit 28 (typeless magic — Protect from Magic gives no reduction). Disturb whirlpools to spawn.

### Kree'arra
- npc_ids: [3162]
- combat_level: 580
- defence_level: 260
- def_bonuses: stab 180 / slash 180 / crush 180 / magic 200 / range 200
- weakness: MELEE by defence (stab/slash/crush tied 180 lowest); element Air 30%. PRACTICAL: flying boss — ranged/magic in practice (melee needs a special weapon).
- categories: [] (attributes=flying)
- location(s): God Wars Dungeon — Armadyl's Eyrie
- requirement: Ranged 70 + Death Plateau (GWD access); 40 Armadyl KC
- isBoss: true
- notes: HP 255. Max hit 69 ranged / 25 melee / 21 magic. Bodyguards Skree/Geerin/Kilisa.

### The Leviathan
- npc_ids: [12214] (post-quest), [12215, 12219] (quest)
- combat_level: 798 (post-quest) / 593 (quest) / 1157 (Awakened)
- defence_level: 250 (post-quest) / 200 (quest) / 287 (Awakened)
- def_bonuses (post-quest): stab 260 / slash 190 / crush 230 / magic 280 / range UNKNOWN (drange not on infobox)
- weakness: MELEE (slash 190 lowest of the listed); no elementalweakness set
- categories: []
- location(s): The Scar (Abyssal Space)
- requirement: Desert Treasure II - The Fallen Empire; no Slayer level
- isBoss: true
- notes: HP 900/720/2700. Uses melee/ranged/magic orbs. drange UNKNOWN — see gaps.

### Phantom Muspah
- npc_ids: [12077, 12078, 12082, 12079, 12080] (across forms)
- combat_level: 741
- defence_level: 200
- def_bonuses (ranged form): stab 185 / slash 134 / crush 120 / magic 437 / range 56
- def_bonuses (melee/stab form): magic defence drops to 34 (other bonuses as above)
- weakness: RANGED (range 56 lowest in ranged form); element Air 65%
- categories: [] (attributes=spectral)
- location(s): Ghorrock Dungeon (via Weiss salt mine)
- requirement: Secrets of the North (NOT DT2); no Slayer level
- isBoss: true
- notes: HP 850. Multiple forms (ranged / melee / teleport-lightning / shielded). Freeze resist 33%, poison/venom immune.

### Sarachnis
- npc_ids: [8713]
- combat_level: 318
- defence_level: 150
- def_bonuses: stab 60 / slash 40 / crush 10 / magic 150 / range 300
- weakness: MELEE (crush 10 lowest); element Fire 40%
- categories: []
- location(s): Forthos Dungeon — Burial tomb
- requirement: none (reached Great Kourend by boat for dungeon access)
- isBoss: true
- notes: HP 400. Max hit 31.

### Scorpia
- npc_ids: [6615]
- combat_level: 225
- defence_level: 180
- def_bonuses: stab 246 / slash 284 / crush 284 / magic 44 / range 284
- weakness: MAGIC (magic 44 lowest); element Fire 35%
- categories: []
- location(s): Scorpion Pit cave, north-east Wilderness (~Level 53-55)
- requirement: none
- isBoss: true
- notes: HP 200. Max hit 16. Aggressive, poisonous.

### Shellbane gryphon
- npc_ids: [14860]
- combat_level: 235
- defence_level: 120
- def_bonuses: stab 10 / slash 20 / crush 40 / magic 100 / range 60
- weakness: MELEE (stab 10 lowest); element Air 50%
- categories: []
- location(s): Cave in the centre of the Great Conch, north of fairy ring CJQ
- requirement: Slayer 51; Troubled Tortugans
- isBoss: true
- notes: HP 400. Max hit 22 (64 whirlwinds / 30 knockback). Cannon-immune, aggressive.

### Thermonuclear smoke devil
- npc_ids: [499]
- combat_level: 301
- defence_level: 360
- def_bonuses: stab 11 / slash 4 / crush 9 / magic 800 / range 900
- weakness: MELEE (slash 4 lowest); element Air 20%
- categories: []
- location(s): Smoke Devil Dungeon (south of Castle Wars)
- requirement: Slayer 93
- isBoss: true
- notes: HP 240. Max hit 8 (typeless magic-ranged). Cannon-immune.

### Vardorvis
- npc_ids: [12223, 12426] (post-quest), [12224, 12228, 12425] (quest)
- combat_level: 784 (post-quest) / 572 (quest) / 1136 (Awakened)
- defence_level: 215→145 scaling (post-quest); 180→130 (quest); 268→181 (Awakened) — defence DROPS over the fight
- def_bonuses (post-quest): stab 215 / slash 65 / crush 85 / magic 580 / range UNKNOWN (drange not on infobox)
- weakness: MELEE (slash 65 lowest); element Fire 35%
- categories: []
- location(s): The Stranglewood — Ritual Site
- requirement: Desert Treasure II - The Fallen Empire; no Slayer level
- isBoss: true
- notes: HP 700/500/1400. Defence & strength scale during the fight. drange UNKNOWN — see gaps.

### Venenatis
- npc_ids: [6610]
- combat_level: 464
- defence_level: 321
- def_bonuses: stab 100 / slash 100 / crush 10 / magic 300 / range 150
- weakness: MELEE (crush 10 lowest); element Fire 40%
- categories: [] (attributes "Spiders, Bosses" — no tracked flag)
- location(s): Silk Chasm (Venenatis' web), ~Level 35+ Wilderness
- requirement: none. Singles-plus alternative: Spindel.
- isBoss: true
- notes: HP 850. Max hit 21 melee / 35 ranged / 30 magic.

### Vet'ion
- npc_ids: [6611, 6612] (two combat phases)
- combat_level: 454
- defence_level: 395
- def_bonuses: stab 201 / slash 200 / crush -10 / magic 250 / range 1
- weakness: MELEE (crush -10 lowest; note range also very low at 1); no elementalweakness set
- categories: [undead] (CONFIRMED — attributes literally contains undead)
- location(s): Vet'ion's Rest, ~Level 30+ Wilderness
- requirement: none. Singles-plus alternative: Calvar'ion.
- isBoss: true
- notes: HP 255/phase. Max hit 44. Two phases.

### Vorkath
- npc_ids: [8059, 8061] (post-quest/awakened), [8058, 8060] (quest)
- combat_level: 732 (awakened/post-quest) / 392 (quest)
- defence_level: 214 (awakened) / 164 (quest)
- def_bonuses (awakened): stab 26 / slash 108 / crush 108 / magic 240 / range 78
- def_bonuses (quest): stab 66 / slash 126 / crush 126 / magic 204 / range 96
- weakness: MELEE-stab (stab lowest in both forms); element Fire 40%
- categories: [dragon, undead] (attributes=dragon,undead,fiery — draconic AND undead)
- location(s): Ungael
- requirement: Dragon Slayer II (access); Slayer 18 noted in pool table; task optional
- isBoss: true
- notes: HP 750/460. Max hits up to 121 (dragonfire bomb special). Acid/spawn phases. **Both dragonbane AND Salve apply.**

### The Whisperer
- npc_ids: [12204, 12205] (post-quest), [12206, 12207] (quest)
- combat_level: 791 (post-quest) / 587 (quest) / 1146 (Awakened)
- defence_level: 250 (post-quest) / 200 (quest) / 300 (Awakened)
- def_bonuses (post-quest): stab 180 / slash 300 / crush 220 / magic 10 / range UNKNOWN (drange not on infobox)
- weakness: MAGIC (magic 10 lowest); element Earth 60%
- categories: []
- location(s): Lassar Undercity — Sunken Cathedral
- requirement: Desert Treasure II - The Fallen Empire; no Slayer level
- isBoss: true
- notes: HP 900/660/2700. Uses melee/ranged/magic. drange UNKNOWN — see gaps.

### Zulrah
- npc_ids: serpentine/green [2042]; magma/red [2043]; tanzanite/blue [2044]
- combat_level: 725
- defence_level: 300
- def_bonuses (serpentine/green, ranged form): stab 0 / slash 0 / crush 0 / magic -45 / range 50
- def_bonuses (magma/red, melee-range form): stab 0 / slash 0 / crush 0 / magic 0 / range 300
- def_bonuses (tanzanite/blue, magic form): stab 0 / slash 0 / crush 0 / magic 300 / range 0
- weakness: per form — green→MAGIC (magic -45); blue→RANGED (range 0); red→high def all around (tail melee, no clear lean); element Fire 50%
- categories: [] (snake — NOT draconic; attributes flag not set on infobox)
- location(s): Zulrah's Shrine, east of Zul-Andra
- requirement: Regicide (Zul-Andra access); no Slayer level
- isBoss: true
- notes: HP 500. Rotates 3 forms. Ranged defence given via dlight/dstandard/dheavy per form. Max hit ~30-41.

---

## Gaps
See `gaps-research.md` `## Boss pool (variants-boss.md)` for the consolidated unfindable/ambiguous values
(Duke/Leviathan/Vardorvis/Whisperer ranged defence UNKNOWN, Araxxor/Kraken defence-level UNKNOWN, the Barrows
spectral-vs-undead correction, Commander Zilyana/Dusk all-tied defences, etc.).

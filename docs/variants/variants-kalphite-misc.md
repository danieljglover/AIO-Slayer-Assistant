# Kalphite / Araxyte / Zygomite / Aquanite-task variants

Source: OSRS Wiki (oldschool.runescape.wiki), `?action=raw` infoboxes + `Slayer_task/*` pages. Fetched 2026-06-29.
Defensive bonuses: dstab/dslash/dcrush/dmagic/(drange via dstandard). Weakness = lowest defensive bonus (+ elemental weakness where present).

## Kalphites

### Kalphite Worker
- npc_ids: [955, 956, 961]
- combat_level: 28
- defence_level: 20
- def_bonuses: stab 5 / slash 5 / crush 1 / magic 10 / range 10
- weakness: MELEE, best type crush, element fire 40%
- categories: kalphite
- location(s): Kalphite Lair, Kalphite Cave (task-only)
- requirement: none
- notes: Weakest variant, crush attacker. Keris bonus applies (kalphite). slayxp 40.

### Kalphite Soldier
- npc_ids: [957 (Lair), 958 (Cave task-only)]
- combat_level: 85
- defence_level: 70
- def_bonuses: stab 25 / slash 25 / crush 5 / magic 50 / range 30
- weakness: MELEE, best type crush, element fire 40%
- categories: kalphite
- location(s): Kalphite Lair, Kalphite Cave (task-only)
- requirement: none
- notes: Induces poison from 4. slayxp 90. Two ids for Lair vs Cave.

### Kalphite Guardian
- npc_ids: [959, 962 (Lair upper/lower), 960 (Cave)]
- combat_level: 141
- defence_level: 110
- def_bonuses: stab 25 / slash 25 / crush 5 / magic 50 / range 30
- weakness: MELEE, best type crush, element fire 40%
- categories: kalphite
- location(s): Kalphite Lair, Kalphite Cave (task-only)
- requirement: none
- notes: Induces poison from 6. slayxp 170. Two infoboxes (Lair ids 959/962, Cave id 960); identical stats.

### Kalphite Queen
- npc_ids: [963, 4303 (crawling form), 965, 4304 (airborne form)]
- combat_level: 333
- defence_level: 300 (mage 150)
- def_bonuses: crawling — stab 50 / slash 50 / crush 10 / magic 100 / range 100; airborne — stab 100 / slash 100 / crush 100 / magic 10 / range 10
- weakness: form-dependent — Crawling: MELEE (crush, 10); Airborne: MAGIC (10). Element fire 40%.
- categories: kalphite (also Bosses)
- location(s): Kalphite Lair
- requirement: boss — separate trip
- notes: Two forms with swapped resistances; attacks with stab/ranged/magic. hp 255, max hit 31, slayxp 535.4. Crush phase 1, Magic phase 2.

### Reanimated kalphite
- npc_ids: [7032]
- combat_level: N/A
- defence_level: 110
- def_bonuses: stab 25 / slash 25 / crush 5 / magic 50 / range 50
- weakness: MELEE, best type crush; no elemental weakness listed
- categories: kalphite
- location(s): summoned via Arceuus Reanimation spell (reanimated kalphite head)
- requirement: none (Arceuus magic; 7032)
- notes: Crush attacker, poison from 6. hp 35, slayxp 35. Assignable monster counts as kalphite. Keris bonus applies.

## Araxytes

Task: Slayer 92 required to harm; Priest in Peril to be assigned (or indirectly via Spiders task). Found in Morytania Spider Cave. Inflict venom from 6.

### Araxyte (level 96)
- npc_ids: [11175, 13666]
- combat_level: 96
- defence_level: 60 (mage 60)
- def_bonuses: stab 60 / slash 30 / crush 20 / magic 20 / range 20
- weakness: crush 20 / magic 20 tie → MELEE (crush) or MAGIC; element fire 50%
- categories: spider, araxyte
- location(s): Morytania Spider Cave
- requirement: Slayer 92; Priest in Peril
- notes: Smaller 1x1 variant. CANNOT spawn superior (Dreadborn). Worse drop table — avoid. slayxp 60, max hit 13.

### Araxyte (level 146)
- npc_ids: [11176, 13667]
- combat_level: 146
- defence_level: 70 (mage 80)
- def_bonuses: stab 60 / slash 30 / crush 20 / magic 20 / range 20
- weakness: crush 20 / magic 20 tie → MELEE (crush) or MAGIC; element fire 50%
- categories: spider, araxyte
- location(s): Morytania Spider Cave
- requirement: Slayer 92; Priest in Peril
- notes: Larger 2x2 variant. Only this variant spawns the Dreadborn superior. slayxp 100, max hit 17.

### Dreadborn Araxyte
- npc_ids: [13680]
- combat_level: 281
- defence_level: 100 (mage 100)
- def_bonuses: stab 60 / slash 30 / crush 0 / magic 10 / range 100
- weakness: MELEE, best type crush (0); element fire 50%
- categories: spider, araxyte (superior)
- location(s): Morytania Spider Cave
- requirement: Slayer 92; Priest in Peril; superior only spawns from level 146 araxytes
- notes: Superior variant. Venom pools up to 50. hp 350, slayxp 4462.5, special 50.

### Araxxor
- npc_ids: [13668]
- combat_level: 890
- defence_level: 135 (in combat) / 170 (enraged); mage 190/218, range 210/241
- def_bonuses: stab 160 / slash 75 / crush 15 / magic 237 / range 218
- weakness: MELEE, best type crush (15); element fire 50%
- categories: araxyte, spider (also Bosses)
- location(s): Morytania Spider Cave
- requirement: boss — separate trip (Slayer 92, or boost from 87)
- notes: Crush strongly recommended. hp 1020. Cannon-immune. Varied mechanics per kill.

## Mutated zygomites

Task: Slayer 57, Combat 60, Lost City. Fungicide spray required to deal finishing blow. Use magic-based melee + ranged. Wear dragonhide (magic def).

### Zygomite (level 74)
- npc_ids: [537]
- combat_level: 74
- defence_level: 65 (mage 65)
- def_bonuses: stab 10 / slash 10 / crush 10 / magic 20 / range 20
- weakness: MELEE (stab/slash/crush all 10); element fire 40%
- categories: (mutated zygomite — no demon/dragon/undead attributes)
- location(s): Zanaris (east of furnace)
- requirement: Slayer 57; fungicide spray to finish
- notes: Magical melee + magical ranged attacker. slayxp 65, max hit 7 melee / 6 ranged.

### Zygomite (level 86)
- npc_ids: [1024]
- combat_level: 86
- defence_level: 75 (mage 75)
- def_bonuses: stab 10 / slash 10 / crush 10 / magic 20 / range 20
- weakness: MELEE (stab/slash/crush all 10); element fire 40%
- categories: (mutated zygomite)
- location(s): Zanaris (near Cosmic Altar; also east of furnace)
- requirement: Slayer 57; fungicide spray to finish
- notes: slayxp 75, max hit 8 melee / 10 ranged.

### Ancient Zygomite
- npc_ids: [7797]
- combat_level: 109
- defence_level: 80 (mage 80)
- def_bonuses: stab 20 / slash 20 / crush 20 / magic 30 / range 30
- weakness: MELEE (stab/slash/crush all 20); element fire 40%
- categories: (mutated zygomite)
- location(s): Mushroom Forest (Fossil Island), Stalker Den (Custodia Pass)
- requirement: Slayer 57; fungicide spray to finish
- notes: Guaranteed Mort myre fungus drop (use Reagent pouch). slayxp 154, max hit 9 melee / 10 ranged.

## Aquanites

Task: Slayer 78; Sailing 73 + adamant keel (or better) to reach Ynysdail. Must unlock "Lured In" (80 pts) to be assigned. Single-way combat; cannon CANNOT be placed. Magic attacker — Protect from Magic. Slash severs the lure, dropping stab defence from 60 to 10.

### Aquanite
- npc_ids: [15497 (lure), 15498 (no lure)]
- combat_level: 145
- defence_level: 70 (mage 170)
- def_bonuses: stab 60 (lure) / 10 (no lure), slash 80 / crush 80 / magic 140 / range 100
- weakness: MELEE, best type STAB — especially after severing lure (stab 60→10)
- categories: (aquanite — no demon/dragon/undead attributes)
- location(s): Ynysdail Cavern (beneath Ynysdail)
- requirement: Slayer 78; Sailing 73 + adamant keel; "Lured In" unlock to be assigned
- notes: Magic attacker, max hit 18, slayxp 180, hp 180. Sever lure with a fast slash weapon (whip/scimitar/SGS spec) then stab. Cannon-immune. Drops Aquanite tendon (→ Aquanite hopper).

### Elder aquanite (superior)
- npc_ids: [15502 (lure), 15503 (no lure)]
- combat_level: 305
- defence_level: 180 (mage 330)
- def_bonuses: stab 60 (lure) / 10 (no lure), slash 80 / crush 80 / magic 140 / range 100
- weakness: MELEE, best type STAB — especially after severing lure (stab 60→10); no elemental weakness listed
- categories: (aquanite — superior)
- location(s): Ynysdail Cavern
- requirement: Slayer 78; Sailing 73 + adamant keel; "Bigger and Badder" unlock (50 pts) for superior to spawn (1/200 on task)
- notes: Magic special deals 35 if Protect from Magic held continuously — flick prayer. hp 400, slayxp 4200, max hit 34 / 50 special. Poison-immune, cannon-immune. RS name "Aquanite (elite)".

## Gaps

- Araxyte (lv 96 & 146) weakness is a genuine crush/magic tie (both 20) — listed as MELEE-crush or MAGIC; fire 50% applies to both.
- Kalphite Queen weakness is form-dependent (crush form 1, magic form 2) — documented per form rather than a single value.
- Reanimated kalphite combat_level is N/A on the wiki (combat = N/A); defence/bonuses taken from infobox.
- No standard superior listed for Kalphites or Zygomites (none on wiki). Araxyte superior = Dreadborn Araxyte; Aquanite superior = Elder aquanite.

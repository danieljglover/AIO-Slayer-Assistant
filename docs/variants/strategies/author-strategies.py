#!/usr/bin/env python3
"""MV-S3: author wiki /Strategies gear into slayer-data.json via byte-faithful line injection.

Unchanged lines stay identical (FR-6); only matching variant lines get a `, "strategy": {...}`
appended before the object's closing brace. Item ids pinned from net.runelite.api.ItemID
(verified against /tmp/itemids.txt). Never fabricate an id: every weapon name MUST resolve in W
or the script aborts."""
import json, sys

JSON = "src/main/resources/data/slayer-data.json"

# --- weapon name -> raw item id (net.runelite.api.ItemID, 1.12.31.1) -------------------------
W = {
    # melee
    "Emberlight": 29589, "Arclight": 19675, "Darklight": 6746, "Osmumten's fang": 26219,
    "Abyssal bludgeon": 13263, "Abyssal dagger": 13265, "Belle's folly": 31248,
    "Abyssal tentacle": 12006, "Abyssal whip": 4151, "Zamorakian hasta": 11889,
    "Sarachnis cudgel": 23528, "Inquisitor's mace": 24417, "Ghrazi rapier": 22324,
    "Blade of saeldor": 23995, "Scythe of vitur": 22325, "Dragon hunter lance": 22978,
    "Dual macuahuitl": 28997, "Zombie axe": 28810, "Noxious halberd": 29796,
    "Soulreaper axe": 25484, "Dragon scimitar": 4587, "Arkan blade": 30955,
    "Colossal blade": 27021, "Ursine chainmace": 27660, "Viggora's chainmace": 22545,
    "Dragon mace": 1434, "Leaf-bladed battleaxe": 20727, "Verac's flail": 4755,
    "Keris partisan": 25979, "Keris partisan of breaching": 25981, "Dharok's greataxe": 4718,
    "Granite hammer": 21742, "Voidwaker": 27690, "Blisterwood flail": 24699,
    "Saradomin's blessed sword": 12809, "Saradomin sword": 11838, "Crystal halberd": 13092,
    "Dragon dagger": 1215, "Saradomin godsword": 11806, "Dragon claws": 13652,
    "Burning claws": 29577,
    # ranged
    "Twisted bow": 20997, "Toxic blowpipe": 12926, "Bow of faerdhinen": 25865,
    "Zaryte crossbow": 26374, "Armadyl crossbow": 11785, "Dragon crossbow": 21902,
    "Rune crossbow": 9185, "Karil's crossbow": 4734, "Eclipse atlatl": 29000,
    "Webweaver bow": 27655, "Craw's bow": 22550, "Heavy ballista": 19481,
    "Venator bow": 27610, "Crystal bow": 4214, "Rosewood blowpipe": 31583,
    "Yew shortbow": 857, "Hunters' sunlight crossbow": 28869, "Magic shortbow (i)": 12788,
    "Scorching bow": 29591, "Dragon hunter crossbow": 21012,
    # magic
    "Tumeken's shadow": 27275, "Eye of ayak": 31113, "Sanguinesti staff": 22323,
    "Trident of the swamp": 12899, "Trident of the seas": 11907,
    "Harmonised nightmare staff": 24423, "Staff of the dead": 11791,
    "Toxic staff of the dead": 12904, "Twinflame staff": 30634, "Smoke battlestaff": 11998,
    "Iban's staff (u)": 12658, "Kodai wand": 21006, "Volatile nightmare staff": 24424,
    "Nightmare staff": 24422, "Ancient sceptre": 27624, "Accursed sceptre": 27665,
    "Thammaron's sceptre": 22555, "Warped sceptre": 28585, "Blue moon spear": 28988,
    "Dragon hunter wand": 30070,
}

WIKI = "https://oldschool.runescape.wiki/w/"

# A barrows magic primary list shared by Dharok/Guthan/Karil/Torag/Verac.
BARROWS_MAGIC = ["Eye of ayak", "Tumeken's shadow", "Harmonised nightmare staff",
                 "Sanguinesti staff", "Trident of the swamp", "Staff of the dead", "Twinflame staff"]

# variant name -> (primaryStyle, [primary names], [(secondary name, style)], note, sourceUrl-leaf)
V = {
 # ---- DEMON BOSSES + TORMENTED DEMON (priority) ----
 "Tormented Demon": ("MELEE",
   ["Emberlight","Arclight","Abyssal bludgeon","Abyssal dagger","Osmumten's fang","Belle's folly","Abyssal tentacle","Abyssal whip"],
   [("Scorching bow","RANGED"),("Twisted bow","RANGED"),("Toxic blowpipe","RANGED")],
   "Demon - demonbane (Arclight/Emberlight) is core; set to crush, or use a Scorching bow / heavy ranged / a spell, to break the fire shield, then punish during the shieldless window.",
   "Tormented_Demon/Strategies"),
 "K'ril Tsutsaroth": ("MELEE",
   ["Emberlight","Osmumten's fang","Arclight"],
   [("Scorching bow","RANGED"),("Tumeken's shadow","MAGIC"),("Toxic blowpipe","RANGED")],
   "Demon boss - demonbane melee (Emberlight/Arclight) is central. The Scorching bow (ranged) is the top solo method and Tumeken's shadow the magic 0-switch.",
   "K'ril_Tsutsaroth/Strategies"),
 "Skotizo": ("MELEE",
   ["Emberlight","Arclight"],
   [("Twisted bow","RANGED"),("Scorching bow","RANGED"),("Bow of faerdhinen","RANGED")],
   "Demon - demonbane (Arclight/Emberlight) one-shots awakened altars and takes less damage; carry it even on a ranged setup. Ranged (Twisted/Scorching bow) is the best method for most players.",
   "Skotizo/Strategies"),
 "Demonic gorilla": ("MELEE",
   ["Emberlight","Arclight","Osmumten's fang","Noxious halberd"],
   [("Twisted bow","RANGED"),("Scorching bow","RANGED"),("Toxic blowpipe","RANGED"),("Bow of faerdhinen","RANGED"),("Eclipse atlatl","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Hybrid prayer-switcher - bring melee + ranged. Demonbane melee (Emberlight/Arclight, they count as black demons); switch to ranged (Twisted/Scorching bow, blowpipe) as they overhead-pray.",
   "Demonic_gorilla/Strategies"),
 "Abyssal Sire": ("MELEE",
   ["Emberlight","Osmumten's fang","Scythe of vitur","Ghrazi rapier","Arclight"],
   [("Scorching bow","RANGED"),("Tumeken's shadow","MAGIC"),("Sanguinesti staff","MAGIC"),("Trident of the swamp","MAGIC")],
   "Demonbane-weak; the optimal kill is melee (Emberlight). Magic/ranged only wake/stun the Sire and pop the respiratory vents (a Scorching bow one-shots them). Guide centres melee despite the data weakness.",
   "Abyssal_Sire/Strategies"),

 # ---- BARROWS ----
 "Ahrim the Blighted": ("RANGED",
   ["Toxic blowpipe","Rosewood blowpipe","Hunters' sunlight crossbow","Magic shortbow (i)","Karil's crossbow","Yew shortbow"],
   [("Tumeken's shadow","MAGIC"),("Eye of ayak","MAGIC"),("Dragon dagger","MELEE")],
   "Ahrim is the only ranged-weak brother, so the guide carves out a dedicated ranged kit. Air spells or a Shadow let you mage him like the others.",
   "Barrows/Strategies"),
 "Dharok the Wretched": ("MAGIC", BARROWS_MAGIC, [],
   "Dharok melees, so the shared BiS magic setup (air spells, 50% wind weakness) is the recommended kill - no per-brother switch.",
   "Barrows/Strategies"),
 "Guthan the Infested": ("MAGIC", BARROWS_MAGIC, [],
   "Guthan melees, so the shared magic setup (50% wind weakness) is the recommended kill.",
   "Barrows/Strategies"),
 "Karil the Tainted": ("MAGIC", BARROWS_MAGIC, [("Dragon dagger","MELEE")],
   "Wiki centres Karil on Magic (50% air weakness) with the shared setup; a melee/Piety kill is a supported alternative.",
   "Barrows/Strategies"),
 "Torag the Corrupted": ("MAGIC", BARROWS_MAGIC, [],
   "Torag melees, so the shared magic setup (50% wind weakness) is the recommended kill.",
   "Barrows/Strategies"),
 "Verac the Defiled": ("MAGIC", BARROWS_MAGIC, [],
   "Wiki centres Verac on Magic (50% air weakness); a stab-tank melee kill is viable since his Defiler effect ignores armour and prayer.",
   "Barrows/Strategies"),

 # ---- GOD WARS DUNGEON ----
 "General Graardor": ("MELEE",
   ["Osmumten's fang","Inquisitor's mace","Ghrazi rapier","Blade of saeldor","Abyssal tentacle","Zamorakian hasta"],
   [("Bow of faerdhinen","RANGED"),("Tumeken's shadow","MAGIC")],
   "Melee solo is generally not recommended vs magic/ranged kiting; if meleeing, Osmumten's fang is strongly recommended.",
   "General_Graardor/Strategies"),
 "Kree'arra": ("MAGIC",
   ["Tumeken's shadow"],
   [("Bow of faerdhinen","RANGED"),("Twisted bow","RANGED"),("Toxic blowpipe","RANGED")],
   "Airborne - regular melee does not work. Tumeken's shadow is the recommended direct method (chinchompas are the AoE alternative).",
   "Kree'arra/Strategies"),
 "Grotesque Guardians - Dawn": ("RANGED",
   ["Venator bow","Bow of faerdhinen","Eclipse atlatl","Hunters' sunlight crossbow","Magic shortbow (i)","Toxic blowpipe"],
   [("Noxious halberd","MELEE"),("Crystal halberd","MELEE")],
   "Dawn is the airborne twin - immune to non-halberd melee; hit her with Ranged (Magic resisted more). Carry a rock/granite hammer to finish.",
   "Grotesque_Guardians/Strategies"),
 "Grotesque Guardians - Dusk": ("MELEE",
   ["Scythe of vitur","Granite hammer","Soulreaper axe","Ghrazi rapier","Blade of saeldor","Inquisitor's mace","Noxious halberd","Osmumten's fang"],
   [("Dragon claws","MELEE"),("Crystal halberd","MELEE"),("Burning claws","MELEE")],
   "Dusk is immune to magic and ranged - melee only. Carry a rock/granite hammer to finish.",
   "Grotesque_Guardians/Strategies"),

 # ---- WILDERNESS BOSSES ----
 "Callisto": ("RANGED",
   ["Webweaver bow","Craw's bow","Bow of faerdhinen","Zaryte crossbow","Armadyl crossbow","Dragon crossbow","Rune crossbow","Magic shortbow (i)"],
   [("Accursed sceptre","MAGIC"),("Thammaron's sceptre","MAGIC"),("Eye of ayak","MAGIC")],
   "Low ranged defence - guide is RANGED (not the data MAGIC). Webweaver/Craw's bow carry the Wilderness bonus.",
   "Callisto/Strategies"),
 "Venenatis": ("MELEE",
   ["Ursine chainmace","Viggora's chainmace","Inquisitor's mace","Soulreaper axe","Abyssal bludgeon","Zombie axe","Zamorakian hasta","Sarachnis cudgel"],
   [("Webweaver bow","RANGED"),("Craw's bow","RANGED"),("Twisted bow","RANGED"),("Toxic blowpipe","RANGED")],
   "Weakest to crush. Ursine chainmace is strongest (Wilderness mace); Viggora's is the budget alt. Ranged stays competitive near the exit.",
   "Venenatis/Strategies"),
 "Vet'ion": ("MELEE",
   ["Ursine chainmace","Viggora's chainmace","Soulreaper axe","Inquisitor's mace","Abyssal bludgeon","Zamorakian hasta","Zombie axe","Sarachnis cudgel","Dual macuahuitl","Leaf-bladed battleaxe","Dragon mace"],
   [],
   "Pure melee fight - crush defence -10. Ursine chainmace is by far the most effective (Wilderness mace); Viggora's is the budget alt.",
   "Vet%27ion/Strategies"),
 "Scorpia": ("MAGIC",
   ["Accursed sceptre","Thammaron's sceptre","Eye of ayak","Sanguinesti staff","Trident of the swamp","Trident of the seas","Warped sceptre"],
   [],
   "Very low magic defence - prioritise magic damage + prayer bonus. Accursed/Thammaron's sceptre are Wilderness weapons; Trident of the swamp also poisons.",
   "Scorpia/Strategies"),
 "Chaos Elemental": ("RANGED",
   ["Webweaver bow","Craw's bow","Twisted bow","Bow of faerdhinen","Toxic blowpipe","Armadyl crossbow"],
   [("Ursine chainmace","MELEE"),("Viggora's chainmace","MELEE"),("Osmumten's fang","MELEE"),("Abyssal tentacle","MELEE")],
   "Guide is RANGED or a melee safespot (not the data MAGIC - magic is not recommended). Webweaver/Craw's bow + Ursine/Viggora's chainmace are Wilderness weapons.",
   "Chaos_Elemental/Strategies"),
 "Chaos Fanatic": ("RANGED",
   ["Webweaver bow","Craw's bow","Twisted bow","Bow of faerdhinen","Scorching bow","Magic shortbow (i)","Crystal bow"],
   [],
   "High magic level/defence - range him. Webweaver/Craw's bow are top picks and Wilderness weapons.",
   "Chaos_Fanatic/Strategies"),
 "Crazy archaeologist": ("MAGIC",
   ["Accursed sceptre","Thammaron's sceptre","Eye of ayak","Sanguinesti staff","Trident of the swamp","Trident of the seas","Warped sceptre","Iban's staff (u)"],
   [],
   "Weak to magic (guide MAGIC, not the data MELEE) - negligible magic level so prioritise magic damage + prayer bonus. Accursed/Thammaron's sceptre are Wilderness weapons.",
   "Crazy_archaeologist/Strategies"),

 # ---- SLAYER-POOL BOSSES ----
 "Alchemical Hydra": ("RANGED",
   ["Twisted bow","Toxic blowpipe","Dragon hunter crossbow","Bow of faerdhinen","Eclipse atlatl"],
   [("Zaryte crossbow","RANGED"),("Dragon hunter lance","MELEE"),("Scythe of vitur","MELEE")],
   "Weakest to ranged (magic not recommended). Draconic - dragonbane (DH crossbow / lance) is effective.",
   "Alchemical_Hydra/Strategies"),
 "Cerberus": ("MELEE",
   ["Scythe of vitur","Arclight","Emberlight","Osmumten's fang","Ghrazi rapier","Inquisitor's mace"],
   [("Twisted bow","RANGED"),("Scorching bow","RANGED")],
   "Weak to melee, primarily crush; demonic so Arclight/Emberlight apply (set to stab). Twisted bow is extremely effective ranged.",
   "Cerberus/Strategies"),
 "Kraken": ("MAGIC",
   ["Eye of ayak","Tumeken's shadow","Harmonised nightmare staff","Sanguinesti staff","Trident of the swamp","Staff of the dead","Trident of the seas","Twinflame staff","Warped sceptre"],
   [],
   "Magic only - ranged deals 1/7 damage and melee cannot reach it. Prioritise occult + magic-damage gear.",
   "Kraken"),
 "Thermonuclear smoke devil": ("MAGIC",
   ["Tumeken's shadow","Eye of ayak","Harmonised nightmare staff","Sanguinesti staff","Trident of the swamp","Kodai wand","Nightmare staff","Ancient sceptre"],
   [("Dragon claws","MELEE")],
   "Air spells hit a 20% elemental weakness; the fastest method is magic. A facemask/Slayer helmet is required.",
   "Thermonuclear_smoke_devil/Strategies"),
 "Sarachnis": ("MELEE",
   ["Dual macuahuitl","Zamorakian hasta","Sarachnis cudgel","Zombie axe","Abyssal bludgeon","Abyssal tentacle","Saradomin sword","Colossal blade"],
   [("Burning claws","MELEE"),("Dragon dagger","MELEE")],
   "Weak to crush; high magic AND ranged defence. High strength + a fast crush weapon; wear magic-defence armour for the magic spawn.",
   "Sarachnis/Strategies"),
 "Giant Mole": ("MELEE",
   ["Osmumten's fang","Noxious halberd","Ghrazi rapier","Zamorakian hasta","Blade of saeldor","Belle's folly","Zombie axe","Abyssal dagger","Dharok's greataxe"],
   [("Twisted bow","RANGED"),("Tumeken's shadow","MAGIC")],
   "Melee uses stab (lowest stab defence). Dharok's-at-1HP is the standard affordable fast-kill; Twisted bow the high-end alt. Poison does not trigger her burrow.",
   "Giant_Mole/Strategies"),

 # ---- DRAGONS / ZULRAH / KQ / DAGANNOTH KINGS ----
 "King Black Dragon": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier"],
   [("Twisted bow","RANGED"),("Dragon hunter crossbow","RANGED"),("Voidwaker","MELEE"),("Dragon hunter wand","MAGIC")],
   "Draconic - the dragonbane Dragon hunter lance is the recommended main weapon. Anti-dragon / Dragonfire shield advised.",
   "King_Black_Dragon/Strategies"),
 "Vorkath": ("RANGED",
   ["Dragon hunter crossbow","Toxic blowpipe"],
   [("Dragon hunter lance","MELEE")],
   "Weak to stab melee and ranged; the dragonbane Dragon hunter crossbow is the most effective ranged weapon, the Dragon hunter lance the melee-method weapon.",
   "Vorkath/Strategies"),
 "Zulrah - serpentine (green) form": ("MAGIC",
   ["Tumeken's shadow","Eye of ayak","Harmonised nightmare staff","Sanguinesti staff","Twinflame staff","Trident of the swamp"],
   [("Twisted bow","RANGED"),("Bow of faerdhinen","RANGED")],
   "Green form is weak to magic (50% fire-spell weakness). The best setup hybridises magic + ranged across the rotation.",
   "Zulrah/Strategies"),
 "Zulrah - tanzanite (blue) form": ("RANGED",
   ["Twisted bow","Toxic blowpipe","Bow of faerdhinen","Hunters' sunlight crossbow"],
   [("Tumeken's shadow","MAGIC")],
   "Tanzanite (blue) form is weak to ranged; magic is near-useless on it. Use ranged here even in an otherwise magic-led trip.",
   "Zulrah/Strategies"),
 "Kalphite Queen": ("RANGED",
   ["Toxic blowpipe","Twisted bow","Bow of faerdhinen","Dragon crossbow","Rune crossbow"],
   [("Keris partisan of breaching","MELEE"),("Keris partisan","MELEE"),("Inquisitor's mace","MELEE"),("Tumeken's shadow","MAGIC")],
   "Two phases - ground form best meleed (Keris is the kalphite weapon, use crush), airborne wasp form best ranged. Slayer helm (i) on task is the biggest DPS boost.",
   "Kalphite_Queen/Strategies"),
 "Kalphite Queen (airborne form)": ("MAGIC",
   ["Tumeken's shadow","Eye of ayak"],
   [("Toxic blowpipe","RANGED"),("Twisted bow","RANGED"),("Bow of faerdhinen","RANGED")],
   "Phase 2 (airborne wasp) has Protect from Melee active (defence only). Shadow 0-switches both phases; otherwise ranged is preferred.",
   "Kalphite_Queen/Strategies"),
 "Kalphite Queen (crawling form)": ("MELEE",
   ["Keris partisan of breaching","Keris partisan","Inquisitor's mace","Scythe of vitur","Soulreaper axe","Zombie axe","Verac's flail","Colossal blade"],
   [("Dragon claws","MELEE")],
   "Phase 1 ground form best meleed. Keris partisan of breaching is the standout kalphite weapon (crush). Slayer helm (i) on task = biggest single DPS gain.",
   "Kalphite_Queen/Strategies"),
 "Dagannoth Prime": ("RANGED",
   ["Twisted bow","Bow of faerdhinen","Toxic blowpipe","Crystal bow","Hunters' sunlight crossbow","Rune crossbow"],
   [],
   "Prime is a magic attacker, weak to RANGED - kill with ranged, pray Protect from Magic.",
   "Dagannoth_Kings/Strategies"),
 "Dagannoth Rex": ("MAGIC",
   ["Eye of ayak","Sanguinesti staff","Trident of the swamp","Trident of the seas","Twinflame staff","Toxic staff of the dead","Warped sceptre","Iban's staff (u)"],
   [],
   "Rex is a melee attacker, weak to MAGIC - kill with magic (very low magic defence), pray Protect from Melee.",
   "Dagannoth_Kings/Strategies"),
 "Dagannoth Supreme": ("MELEE",
   ["Osmumten's fang","Abyssal tentacle","Scythe of vitur","Inquisitor's mace","Ghrazi rapier","Blade of saeldor","Noxious halberd","Abyssal whip","Zombie axe"],
   [("Saradomin godsword","MELEE")],
   "Supreme is a ranged attacker, killed with MELEE - pray Protect from Missiles.",
   "Dagannoth_Kings/Strategies"),

 # ---- INFERNO / FIGHT CAVE / ARAXXOR / ROYAL TITANS / GRYPHON ----
 "TzTok-Jad": ("RANGED",
   ["Toxic blowpipe","Twisted bow","Bow of faerdhinen","Zaryte crossbow","Armadyl crossbow","Hunters' sunlight crossbow","Karil's crossbow","Rune crossbow"],
   [("Tumeken's shadow","MAGIC"),("Twinflame staff","MAGIC")],
   "Ranged avoids Jad's no-warning melee and lets you safespot. Twisted bow is best on Jad specifically (very magic-resistant).",
   "TzHaar_Fight_Cave/Strategies"),
 "TzKal-Zuk": ("RANGED",
   ["Toxic blowpipe","Twisted bow","Bow of faerdhinen","Armadyl crossbow","Dragon crossbow"],
   [("Kodai wand","MAGIC"),("Tumeken's shadow","MAGIC"),("Eye of ayak","MAGIC")],
   "The Inferno is a ranged encounter. Twisted bow on the set mager/Jad/Zuk; blowpipe on rangers/healers.",
   "Inferno/Strategies"),
 "Araxxor": ("MELEE",
   ["Scythe of vitur","Inquisitor's mace","Soulreaper axe","Abyssal bludgeon","Zamorakian hasta","Sarachnis cudgel","Dual macuahuitl","Zombie axe"],
   [("Noxious halberd","MELEE"),("Hunters' sunlight crossbow","RANGED"),("Karil's crossbow","RANGED"),("Heavy ballista","RANGED")],
   "True melee boss - weak to crush (lowest crush defence). Bring a Noxious halberd alongside the main crush weapon for egg spawns.",
   "Araxxor/Strategies"),
 "Branda the Fire Queen": ("MELEE",
   ["Scythe of vitur","Inquisitor's mace","Soulreaper axe","Blade of saeldor","Ghrazi rapier","Noxious halberd","Dual macuahuitl","Osmumten's fang","Abyssal tentacle","Dragon scimitar"],
   [("Toxic blowpipe","RANGED"),("Eclipse atlatl","RANGED"),("Hunters' sunlight crossbow","RANGED"),("Karil's crossbow","RANGED"),("Magic shortbow (i)","RANGED"),("Twinflame staff","MAGIC"),("Kodai wand","MAGIC"),("Ancient sceptre","MAGIC")],
   "3-style boss designed to teach gear switching: prioritise melee strength, but you MUST bring ranged for her retreat phases and ideally magic for the elementals. Weak to water and slightly to crush.",
   "Royal_Titans/Strategies"),
 "Shellbane gryphon": ("MELEE",
   ["Scythe of vitur","Soulreaper axe","Ghrazi rapier","Noxious halberd","Osmumten's fang","Blade of saeldor","Abyssal tentacle","Abyssal whip","Abyssal dagger","Zombie axe","Belle's folly","Arkan blade","Colossal blade","Dragon scimitar"],
   [],
   "Primarily weak to melee (set slash-style weapons to slash; stab def lowest). A tortugan shield is REQUIRED to survive; magic is impractical due to the corrosive spit.",
   "Shellbane_gryphon/Strategies"),

 # ---- NON-BOSS MONSTERS ----
 "Smoke devil": ("MAGIC",
   ["Kodai wand","Volatile nightmare staff","Nightmare staff","Dragon hunter wand","Ancient sceptre","Blue moon spear","Accursed sceptre"],
   [],
   "BiS magic-damage gear with an Ancient-Magicks autocast staff (Ice Barrage). A facemask/Slayer helmet is required to enter.",
   "Smoke_devil/Strategies"),
 "Nuclear smoke devil": ("MAGIC",
   ["Kodai wand","Volatile nightmare staff","Nightmare staff","Dragon hunter wand","Ancient sceptre","Blue moon spear","Accursed sceptre"],
   [],
   "Superior smoke devil - same magic guide as the base monster (Ice Barrage). Facemask/Slayer helmet required.",
   "Smoke_devil/Strategies"),
 "Aquanite": ("MELEE",
   ["Ghrazi rapier","Osmumten's fang","Blade of saeldor","Abyssal dagger","Voidwaker"],
   [("Abyssal whip","MELEE"),("Saradomin godsword","MELEE")],
   "Attacks with Magic (pray Protect from Magic). Slash severs the lure (drops stab defence 60->10) - carry a fast slash weapon for the sever, then stab.",
   "Aquanite/Strategies"),
 "Aviansie": ("RANGED",
   ["Toxic blowpipe","Bow of faerdhinen","Eclipse atlatl","Hunters' sunlight crossbow","Zaryte crossbow","Armadyl crossbow","Dragon crossbow"],
   [],
   "BiS ranged. Wear an Armadyl + a Zamorak item to avoid GWD aggression. Protect from Missiles.",
   "Aviansie/Strategies"),
 "Flight Kilisa": ("RANGED",
   ["Toxic blowpipe","Bow of faerdhinen","Eclipse atlatl","Hunters' sunlight crossbow","Zaryte crossbow","Armadyl crossbow","Dragon crossbow"],
   [],
   "Armadyl GWD minion - shares the Aviansie ranged guide.",
   "Aviansie/Strategies"),
 "Flockleader Geerin": ("RANGED",
   ["Toxic blowpipe","Bow of faerdhinen","Eclipse atlatl","Hunters' sunlight crossbow","Zaryte crossbow","Armadyl crossbow","Dragon crossbow"],
   [],
   "Armadyl GWD minion - shares the Aviansie ranged guide.",
   "Aviansie/Strategies"),
 "Wingman Skree": ("RANGED",
   ["Toxic blowpipe","Bow of faerdhinen","Eclipse atlatl","Hunters' sunlight crossbow","Zaryte crossbow","Armadyl crossbow","Dragon crossbow"],
   [],
   "Armadyl GWD minion - shares the Aviansie ranged guide.",
   "Aviansie/Strategies"),
 "Basilisk Knight": ("MELEE",
   ["Inquisitor's mace","Ursine chainmace","Osmumten's fang","Zamorakian hasta","Zombie axe"],
   [("Hunters' sunlight crossbow","RANGED"),("Zaryte crossbow","RANGED"),("Armadyl crossbow","RANGED"),("Dragon hunter crossbow","RANGED"),("Dragon crossbow","RANGED")],
   "Requires V's shield to survive their petrify. Melee is fastest; heavy ranged (crossbows) is preferred for safespotting.",
   "Basilisk_Knight/Strategies"),
 "Lizardman shaman": ("RANGED",
   ["Toxic blowpipe","Rosewood blowpipe","Bow of faerdhinen","Twisted bow","Karil's crossbow","Hunters' sunlight crossbow","Scorching bow","Rune crossbow","Magic shortbow (i)","Crystal bow"],
   [("Ghrazi rapier","MELEE"),("Noxious halberd","MELEE"),("Osmumten's fang","MELEE"),("Scythe of vitur","MELEE"),("Voidwaker","MELEE")],
   "Weak to stab (negative stab defence) and ranged; avoid slash/crush/magic. Get the Slayer-helm Shayzien acid effect (Hard Kourend diary) or wear Shayzien helm (5).",
   "Lizardman_shaman/Strategies"),
 "Vyrewatch Sentinel": ("MELEE",
   ["Blisterwood flail"],
   [],
   "Vyres require a vampyre-specific weapon - the Blisterwood flail. Protect from Melee.",
   "Vyrewatch_Sentinel/Strategies"),
 "Skeletal Wyvern": ("MELEE",
   ["Dragon hunter lance","Inquisitor's mace","Blade of saeldor","Osmumten's fang","Abyssal whip","Zombie axe"],
   [("Dragon hunter crossbow","RANGED"),("Bow of faerdhinen","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Draconic - dragonbane (DHL / DHCB). Must carry an anti-icy-breath shield (elemental/mind/dragonfire/wyvern). Ranged is equally promoted for safespotting.",
   "Skeletal_Wyvern/Strategies"),
 "Spitting Wyvern": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta","Abyssal dagger","Abyssal whip"],
   [("Eye of ayak","MAGIC"),("Dragon hunter wand","MAGIC"),("Sanguinesti staff","MAGIC"),("Dragon hunter crossbow","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Draconic - dragonbane. Tank by maximising ranged Defence + Protect from Melee; carry an anti-icy-breath shield.",
   "Ancient_Wyvern/Strategies"),
 "Taloned Wyvern": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta","Abyssal dagger","Abyssal whip"],
   [("Eye of ayak","MAGIC"),("Dragon hunter wand","MAGIC"),("Sanguinesti staff","MAGIC"),("Dragon hunter crossbow","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Draconic - dragonbane. Tank ranged Defence + Protect from Melee; carry an anti-icy-breath shield.",
   "Ancient_Wyvern/Strategies"),
 "Long-tailed Wyvern": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta","Abyssal dagger","Abyssal whip"],
   [("Eye of ayak","MAGIC"),("Dragon hunter wand","MAGIC"),("Sanguinesti staff","MAGIC"),("Dragon hunter crossbow","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Draconic - dragonbane. Tank ranged Defence + Protect from Melee; carry an anti-icy-breath shield.",
   "Ancient_Wyvern/Strategies"),
 "Ancient Wyvern": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta","Abyssal dagger","Abyssal whip"],
   [("Eye of ayak","MAGIC"),("Dragon hunter wand","MAGIC"),("Sanguinesti staff","MAGIC"),("Dragon hunter crossbow","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Draconic - dragonbane. Tank ranged Defence + Protect from Melee; carry an anti-icy-breath shield.",
   "Ancient_Wyvern/Strategies"),
 "Frost dragon": ("MELEE",
   ["Dragon hunter lance"],
   [("Dragon hunter crossbow","RANGED"),("Zaryte crossbow","RANGED"),("Armadyl crossbow","RANGED"),("Dragon crossbow","RANGED"),("Dragon hunter wand","MAGIC"),("Harmonised nightmare staff","MAGIC")],
   "Weakest to crush then stab, and 100% weak to fire spells; as a dragon, weak to dragonbane. Standard dragonfire protection negates their icy breath.",
   "Frost_dragon/Strategies"),
 "Bronze dragon": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta","Abyssal dagger"],
   [("Dragon hunter crossbow","RANGED"),("Zaryte crossbow","RANGED"),("Dragon crossbow","RANGED"),("Rune crossbow","RANGED"),("Dragon hunter wand","MAGIC"),("Harmonised nightmare staff","MAGIC")],
   "Weak to stab + heavy ranged + Earth spells; very high defence. DHL is best stab vs draconic, then fang. Do NOT use slash.",
   "Metal_dragons/Strategies"),
 "Iron dragon": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta","Abyssal dagger"],
   [("Dragon hunter crossbow","RANGED"),("Zaryte crossbow","RANGED"),("Dragon crossbow","RANGED"),("Rune crossbow","RANGED"),("Dragon hunter wand","MAGIC"),("Harmonised nightmare staff","MAGIC")],
   "Weak to stab + heavy ranged + Earth spells; very high defence. DHL best stab vs draconic, then fang. Do NOT use slash.",
   "Metal_dragons/Strategies"),
 "Steel dragon": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta","Abyssal dagger"],
   [("Dragon hunter crossbow","RANGED"),("Zaryte crossbow","RANGED"),("Dragon crossbow","RANGED"),("Rune crossbow","RANGED"),("Dragon hunter wand","MAGIC"),("Harmonised nightmare staff","MAGIC")],
   "Weak to stab + heavy ranged + Earth spells; very high defence. DHL best stab vs draconic, then fang. Do NOT use slash.",
   "Metal_dragons/Strategies"),
 "Mithril dragon": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang","Ghrazi rapier","Zamorakian hasta"],
   [("Tumeken's shadow","MAGIC"),("Dragon hunter wand","MAGIC"),("Harmonised nightmare staff","MAGIC"),("Eye of ayak","MAGIC"),("Dragon hunter crossbow","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Draconic - dragonbane; 50% Earth-spell weakness makes magic strong. Bring a defender + extended super antifire.",
   "Mithril_dragon/Strategies"),
 "Adamant dragon": ("MELEE",
   ["Dragon hunter lance","Ghrazi rapier","Osmumten's fang"],
   [("Dragon hunter crossbow","RANGED"),("Twisted bow","RANGED"),("Tumeken's shadow","MAGIC"),("Harmonised nightmare staff","MAGIC"),("Dragon hunter wand","MAGIC"),("Eye of ayak","MAGIC")],
   "Draconic - dragonbane reliable at base-90 melee; Tumeken's shadow is fastest off-task (low magic defence + Earth weakness).",
   "Adamant_dragon/Strategies"),
 "Rune dragon": ("MELEE",
   ["Dragon hunter lance","Osmumten's fang"],
   [("Tumeken's shadow","MAGIC"),("Harmonised nightmare staff","MAGIC"),("Dragon hunter wand","MAGIC"),("Eye of ayak","MAGIC"),("Dragon hunter crossbow","RANGED"),("Hunters' sunlight crossbow","RANGED")],
   "Draconic - dragonbane reliable at base-90; Tumeken's shadow is the single best weapon overall (low magic defence).",
   "Rune_dragon/Strategies"),
}


def wid(name):
    if name not in W:
        sys.exit("UNRESOLVED WEAPON ID: " + name)
    return W[name]


def build_strategy(spec):
    style, primaries, secondaries, note, leaf = spec
    obj = {
        "primaryStyle": style,
        "primaryWeapons": [{"name": n, "itemId": wid(n)} for n in primaries],
        "secondaryWeapons": [{"name": n, "itemId": wid(n), "style": s} for (n, s) in secondaries],
        "note": note,
        "sourceUrl": WIKI + leaf,
    }
    return json.dumps(obj, separators=(", ", ": "), ensure_ascii=False)


def main():
    with open(JSON, encoding="utf-8") as f:
        lines = f.readlines()

    seen = {}
    out = []
    for line in lines:
        stripped = line.lstrip()
        injected = False
        if stripped.startswith('{ "name": "'):
            name = stripped.split('"', 4)[3]  # { "name": "<NAME>", ...
            if name in V and '"konarLockable"' not in line:
                nl = line.rstrip("\n")
                # insert before the object's closing brace (rightmost '}')
                idx = nl.rfind("}")
                strat = build_strategy(V[name])
                nl = nl[:idx] + ', "strategy": ' + strat + " " + nl[idx:]
                out.append(nl + "\n")
                seen[name] = seen.get(name, 0) + 1
                injected = True
        if not injected:
            out.append(line)

    with open(JSON, "w", encoding="utf-8") as f:
        f.writelines(out)

    print("Injected variants (name -> occurrences):", len(seen))
    missing = [k for k in V if k not in seen]
    for k in sorted(seen):
        print(f"  {seen[k]}x {k}")
    if missing:
        print("WARNING: defined but NOT FOUND in JSON:", missing)
    print("Total strategy blocks written:", sum(seen.values()))


if __name__ == "__main__":
    main()

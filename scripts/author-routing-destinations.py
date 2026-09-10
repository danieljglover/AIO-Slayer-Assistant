#!/usr/bin/env python3
"""Author bundled exact routing bindings from raw Wiki revisions; never run by the plugin.

The explicit aliases below are reviewed source-label mappings, not runtime fuzzy
geocoding. Raw pages remain in /tmp. Review the generated gap report after updates.
"""
import argparse
import collections
import importlib.util
import json
import pathlib
import re
import sys
from datetime import datetime, timezone

sys.dont_write_bytecode = True
ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'src/main/data/slayer'
spec = importlib.util.spec_from_file_location('wiki_audit', ROOT / 'scripts/audit-advisor-wiki.py')
wiki = importlib.util.module_from_spec(spec)
spec.loader.exec_module(wiki)

# These aliases retain meaningful area/level distinctions. A row is accepted only
# for a monster whose authored source graph already links this exact location ID.
ALIASES = {'abyssal-area': ['Abyssal Area (alr)'],
 'ammonite-crabs-fossil-island': ['Fossil Island Volcano', 'Mushroom Forest', 'Wyvern Cave - northern cave'],
 'ancient-cavern-metal-dragons': ['Ancient Cavern (upper level)'],
 'ancient-cavern-waterfiends': ['Ancient Cavern'],
 'aquanite-cavern-sailing': ['Ynysdail Cavern'],
 'araxxor-lair': ['Morytania Spider Cave', 'Morytania Spider Cave (task-only area)'],
 'araxyte-lair-morytania': ['Morytania Spider Cave', 'Morytania Spider Cave (task-only area)'],
 'asgarnian-ice-dungeon': ['Asgarnian Ice Dungeon (AIQ)'],
 'axe-hut': ['Magic axe hut (23 Thieving and lockpick required)'],
 'brimhaven-dungeon': ['Brimhaven Dungeon upper level', 'Brimhaven Dungeon ()'],
 'brimhaven-dungeon-baby-red-dragons': ['Brimhaven Dungeon'],
 'brimhaven-dungeon-metal-dragons-task-only': ['Brimhaven Dungeon (Task only area)'],
 'brimhaven-dungeon-red-dragons': ['Brimhaven Dungeon'],
 'brine-rat-cavern': ['Brine Rat Cavern (dks)'],
 'canifis-ghoul-area': ['North of Mort Myre Swamp', 'South of the Slayer Tower'],
 'catacombs-of-kourend': ['Catacombs of Kourend (north-west)'],
 'catacombs-of-kourend-brutal-red-dragons': ['Catacombs of Kourend'],
 'chaos-druid-tower-dungeon': ['Chaos Druid Tower Dungeon (requires 46 Thieving)'],
 'chaos-elemental-rogues-castle': ["West of the Rogues' Castle"],
 'chaos-fanatic-lava-maze': ['West of the Lava Maze'],
 'charred-dungeon-lava-strykewyrms': ['Charred Dungeon'],
 'charred-dungeon-red-dragons': ['Charred Dungeon'],
 'chasm-of-fire-middle': ['Chasm of Fire'],
 'combat-training-camp': ['Combat Training Camp (requires Biohazard)'],
 'corsair-cove-dungeon': ["Corsair Cove Dungeon/Myths' Guild (basement)"],
 'corsair-cove-dungeon-baby-red-dragons': ["Corsair Cove Dungeon/Myths' Guild (basement)"],
 'corsair-cove-dungeon-red-dragons': ["Corsair Cove Dungeon/Myths' Guild (basement)"],
 'cow-field': ['Lumbridge East farm', 'South Falador Farm', 'Crafting Guild'],
 'demonic-ruins': ['Wilderness near Demonic Ruins'],
 'dorgesh-kaan-south-dungeon': ['Dorgesh-Kaan South Dungeon (ajq)'],
 'earth-warriors-wilderness': ['Edgeville Dungeon (Wilderness)'],
 'eastern-gryphon-dungeon': ['Gryphons (task-only dungeon)'],
 'edgeville-dungeon-wilderness-skeletons': ['Edgeville Dungeon (Wilderness area)'],
 'ent-wilderness': ['East of Chaos Temple (Wilderness)', 'North of Chaos Temple (Wilderness)'],
 'forthos-dungeon-baby-red-dragons': ['Forthos Dungeon'],
 'forthos-dungeon-red-dragons': ['Forthos Dungeon'],
 'forthos-sarachnis': ['Forthos Dungeon — Burial tomb'],
 'fossil-island-mushroom-forest-zygomites': ['Fossil Island'],
 'fremennik-isles-ice-trolls': ['Fremennik Isles'],
 'fremennik-slayer-dungeon': ['Fremennik Slayer Dungeon (ajr)'],
 'fremennik-slayer-dungeon-cave-crawlers': ['Fremennik Slayer Dungeon (ajr)'],
 'fremennik-slayer-dungeon-cockatrice': ['Fremennik Slayer Cave (AJR)'],
 'fremennik-slayer-dungeon-kurasks': ['Fremennik Slayer Dungeon AJR',
                                      'Fremennik Slayer Dungeon (Slayer task only)'],
 'fremennik-slayer-dungeon-pyrefiends': ['Fremennik Slayer Cave (ajr)'],
 'fremennik-slayer-dungeon-rockslugs': ['Fremennik Slayer Dungeon (ajr)'],
 'fremennik-slayer-dungeon-turoths': ['Fremennik Slayer Dungeon (ajr)'],
 'frozen-waste-plateau': ['Frozen Waste Plateau (Wilderness)',
                          'Northern Frozen Waste Plateau',
                          'Southern Frozen Waste Plateau'],
 'graveyard-of-shadows-green-dragons': ['North of the Graveyard of Shadows'],
 'gu-tanoth': ["Gu'Tanoth (Requires partial completion of Watchtower)"],
 'haunted-woods': ['Haunted Woods ALQ'],
 'hobgoblin-area': ['Edgeville Dungeon', 'Hobgoblin Peninsula west of the Crafting Guild'],
 'iorwerth-dungeon-waterfiends': ['Iorwerth Dungeon'],
 'kalphite-cave': ['Kalphite Cave (on Slayer task)'],
 'karuulm-slayer-dungeon-greater-demons': ['Karuulm Slayer Dungeon'],
 'karuulm-slayer-dungeon-wyrms': ['Karuulm Slayer Dungeon - lower level',
                                  'Karuulm Slayer Dungeon - lower level (task-only)'],
 'keldagrim-entrance-trolls': ['Tunnel entrance to Keldagrim'],
 'kharidian-desert-lizards': ['South-east of Shantay Pass', 'West of Ruins of Ullek'],
 'king-black-dragon-lair-lesser-demons': ['Wilderness - King Black Dragon Lair entrance'],
 'kingstown': ["Kingstown - Councillor Hughes' house"],
 'kourend-castle': ['Kourend Castle - Cage on the top floor'],
 'kraken-cove-waterfiends': ['Kraken Cove (Slayer Task Only)'],
 'lava-maze': ['Wilderness - by the muddy chest in the Lava Maze'],
 'lighthouse-basement': ['Lighthouse'],
 'lizardman-canyon': ['Lizardman Canyon, west side', 'Lizardman Canyon, east side'],
 'lizardman-caves': ['Lizardman Caves, underneath Lizardman Settlement (Slayer task only)'],
 'lizardman-temple': ['Lizardman Temple, beneath Molch'],
 'mammoth-wilderness': ['South-east of Ferox Enclave'],
 'meiyerditch': ['Southern Meiyerditch'],
 'mole-hole': ['Mole Hole (Under Falador Park)'],
 'mor-ul-rek-tzhaar-city': ['Mor Ul Rek', 'Mor Ul Rek (Melee)', 'Mor Ul Rek (Ranged)'],
 'moss-giants-wilderness': ['Wilderness Pond'],
 'mudskipper-point': ['Mudskipper Point (aiq)'],
 'neypotzli-sulphur-nagua': ['Neypotzli - Ancient Prison'],
 'neypotzli-wyrmlings': ['Neypotzli - Earthbound Cavern'],
 'ogre-enclave-watchtower': ['Ogre Enclave'],
 'rat-spawns': ['Lumbridge',
                'Lumbridge Swamp',
                'Behind Lumbridge Castle',
                'Draynor Village',
                'Draynor Sewers',
                'Varrock',
                'Varrock Sewers',
                'Edgeville Dungeon',
                'Stronghold of Security: Catacomb of Famine',
                'Stronghold of Security: Vault of War'],
 'revenant-caves': ['Revenant Cave'],
 'rock-crabs-rellekka': ['North of Rellekka'],
 'rogues-castle': ["Rogues' Castle ()"],
 'ruins-of-tapoyauik': ['Ruins of Tapoyauik (Bottom floor)',
                        'Ruins of Tapoyauik bottom floor',
                        'Ruins of Tapoyauik middle floor'],
 'sand-crabs-crabclaw-isle': ['Crabclaw Isle', 'Hosidius - southern coast'],
 'scorpion-pit-cave': ['Cave in the Scorpion Pit (north-east Wilderness)'],
 'skeletons-area': ['Edgeville Dungeon', 'Varrock Sewers', 'Draynor Sewers'],
 'slayer-tower': ['Slayer Tower ()'],
 'slayer-tower-basement': ['Slayer Tower (basement)', 'Slayer Tower basement (Slayer task only)'],
 'slayer-tower-basement-nechryael': ['Slayer Tower (basement)'],
 'slayer-tower-top-floor': ['Slayer Tower ()'],
 'slayer-tower-top-floor-nechryael': ['Slayer Tower Top Floor'],
 'sophanem-dungeon-cavern': ['Sophanem Dungeon (cavern level)'],
 'sophanem-dungeon-maze': ['Sophanem Dungeon (maze level)'],
 'sourhog-cave': ['Sourhog Cave'],
 'stalker-den-custodian-stalkers': ['Stalker Den (multicombat)', 'Stalker Den (single-combat)'],
 'stronghold-of-security': ['Sepulchre of Death in Stronghold of Security'],
 'stronghold-of-security-catablepon': ['Stronghold of Security: Pit of Pestilence'],
 'stronghold-of-security-flesh-crawlers': ['Stronghold of Security: Catacomb of Famine'],
 'stronghold-of-security-minotaurs': ['Stronghold of Security: Vault of War'],
 'stronghold-slayer-cave-fire-giants': ['Stronghold Slayer Dungeon'],
 'tai-bwo-wannai': ['North-east of Tai Bwo Wannai (dkp)'],
 'taverley-dungeon': ['Taverley Dungeon (upper level)'],
 'uzer-mastaba': ['Uzer Mastaba - lower level'],
 'waterbirth-island-dungeon': ['Waterbirth Island Dungeon (Slayer lair)',
                               'Waterbirth Island Dungeon sublevel 1',
                               'Waterbirth Island Dungeon sublevel 2',
                               'Waterbirth Island Dungeon sublevel 3',
                               'Waterbirth Island Dungeon sublevel 5'],
 'wilderness-slayer-cave-ice-giants': ['Wilderness Slayer Cave'],
 'wyvern-cave-fossil-island': ['Wyvern Cave'],
 'wyvern-cave-fossil-island-task-only': ['Wyvern Cave (Slayer task only)'],
 'zanaris-cosmic-altar-zygomites': ['Zanaris'],
 'zanaris-furnace-zygomites': ['Zanaris'],
 'zanaris-otherworldly-beings': ['Zanaris']}

# Broad task areas deliberately include only explicitly classified non-Wilderness
# rows. Wilderness consent must not be bypassed by a generic "various" location.
GENERIC = {'bat-spawns', 'bears-area', 'bird-spawns', 'dog-spawns',
           'ghost-spawns', 'goblin-spawns', 'ice-warriors-area',
           'monkey-spawns', 'scorpions-area',
           'spiders-area', 'zombies-area'}
WILD_GENERIC = {'bears-wilderness', 'green-dragons-wilderness', 'scorpions-wilderness', 'spiders-wilderness'}

# The Wiki uses surface pins for these lairs. They are entrance arrivals, never
# claims that an instanced boss tile can be routed directly.
ENTRANCE_LOCATIONS = {'callistos-den', 'hunters-end', 'silk-chasm', 'vetions-rest'}

TITLE_OVERRIDES = {'monkey': ['Monkey (monster)'], 'guard-prifddinas': ['Guard (Prifddinas)']}

# Exact upstream teleport landing tiles are used only as endpoint coordinates;
# this does not offer, enable, or require the seasonal teleport itself.
UPSTREAM_COMMIT = '6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a'
ENTRANCES = {
    'vetions-rest': (213, "Vet'ion", "Vet'ion's Rest entrance", ["Vet'ion", "Vet'ion's Rest"]),
    'skeletal-tomb': (204, "Calvar'ion", 'Skeletal Tomb entrance', ["Calvar'ion", 'Skeletal Tomb']),
    'web-chasm': (211, 'Spindel', 'Web Chasm entrance', ['Spindel', 'Web Chasm']),
    'royal-titans-arena': (168, 'Royal Titans', 'Royal Titans entrance in Asgarnian Ice Dungeon', ['Royal Titans']),
    'slayer-tower-roof': (189, 'Grotesque Guardians', 'Slayer Tower entrance - continue to the rooftop', ['Grotesque Guardians']),
    'zulrahs-shrine': (194, 'Zulrah', "Zul-Andra boat - continue to Zulrah's Shrine", ['Zulrah']),
    'amoxliatl-chamber': (195, 'Amoxliatl', 'Ruins of Tapoyauik entrance - continue to Amoxliatl', ['Amoxliatl', 'Ruins of Tapoyauik']),
    'ungael-vorkath': (172, 'Vorkath', 'Ungael arrival - continue through the Vorkath entrance', ['Vorkath']),
    'alchemical-hydra-lair': (178, 'Alchemical Hydra', 'Mount Karuulm arrival - continue into the dungeon', ['Alchemical Hydra']),
    'kraken-boss-cove': (173, 'Kraken', 'Kraken Cove surface entrance', ['Kraken']),
    'ghorrock-dungeon': (171, 'Phantom Muspah', 'Ghorrock Dungeon arrival - continue to the lair crevice', ['Phantom Muspah']),
    'barrows': (187, 'Barrows surface', 'Barrows surface - descend into the selected crypt', ['Barrows']),
    'skotizos-lair': (183, 'Skotizo', 'Catacombs dark altar - enter Skotizo encounter manually', ['Skotizo']),
    'god-wars-dungeon-armadyl': (165, "Kree'arra", 'God Wars Dungeon arrival - continue to Armadyl boss room', ["Kree'arra", 'God Wars Dungeon']),
    'bandos-stronghold': (164, 'General Graardor', 'God Wars Dungeon arrival - continue to Bandos boss room', ['General Graardor', 'God Wars Dungeon']),
    'zamorak-fortress': (166, "K'ril Tsutsaroth", 'God Wars Dungeon arrival - continue to Zamorak boss room', ["K'ril Tsutsaroth", 'God Wars Dungeon']),
}

# Reviewed inverse rectangles from the pinned Quest Helper mapping table. Bounds
# are half-open; distinct matching outputs are rejected. Attribution and complete
# BSD notice are in docs/reconstruction/routing-sources.md.
QUEST_HELPER_COMMIT = '633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a'
MAP_TRANSFORMS = [[1, 1792, 5376, 1856, 5440, -64, -64, 1, 'ANCIENT_CAVERN_6995'],
 [1, 1792, 5312, 1856, 5376, -64, -64, 1, 'ANCIENT_CAVERN_6994'],
 [3, 3072, 9536, 3136, 9600, -64, 0, 1, 'ASGARNIA_ICE_CAVE_12181'],
 [3, 3136, 9536, 3152, 9600, -64, 0, 1, 'ASGARNIA_ICE_CAVE_12437'],
 [3, 3008, 9616, 3072, 9664, -64, 0, 0, 'ASGARNIA_ICE_CAVE_RAT_PITS_11926'],
 [44, 1344, 9536, 1408, 9600, 0, 0, 1, 'CAM_TORUM_5525'], [44, 1408, 9472, 1472, 9536, 0, 0, 1, 'CAM_TORUM_5780'],
 [44, 1408, 9536, 1472, 9600, 0, 0, 1, 'CAM_TORUM_5781'], [44, 1408, 9600, 1472, 9608, 0, 0, 1, 'CAM_TORUM_5782'],
 [44, 1472, 9536, 1536, 9600, 0, 0, 1, 'CAM_TORUM_6037'],
 [5, 2752, 5312, 2816, 5376, -64, -64, 1, 'DORGESH_KAAN_10834'],
 [5, 2752, 5376, 2816, 5440, -64, -64, 1, 'DORGESH_KAAN_10835'],
 [5, 2816, 5376, 2880, 5440, -128, -128, 2, 'DORGESH_KAAN_10834_PLANE2'],
 [5, 2816, 5440, 2880, 5504, -128, -128, 2, 'DORGESH_KAAN_10835_PLANE2'],
 [6, 3072, 9664, 3136, 9728, 640, -4032, 0, 'DWARVEN_MINES_14936'],
 [6, 3168, 9664, 3232, 9728, 544, -4032, 1, 'DWARVEN_MINES_14936_PLANE1'],
 [31, 2008, 9064, 2040, 9096, 488, 304, 0, 'FELDIP_HILLS_UNDERGROUND_10130'],
 [31, 1856, 8960, 1920, 9024, 0, 0, 1, 'FELDIP_HILLS_UNDERGROUND_7564'],
 [31, 1920, 8960, 1984, 9024, 0, 0, 1, 'FELDIP_HILLS_UNDERGROUND_7820'],
 [31, 1920, 9024, 1984, 9088, 0, 0, 1, 'FELDIP_HILLS_UNDERGROUND_7821'],
 [31, 1984, 8960, 2048, 9024, 0, 0, 1, 'FELDIP_HILLS_UNDERGROUND_8076'],
 [31, 2048, 8960, 2112, 9024, 0, 0, 1, 'FELDIP_HILLS_UNDERGROUND_8332'],
 [31, 1792, 9088, 1856, 9152, 320, 192, 0, 'FELDIP_HILLS_UNDERGROUND_SOUL_WARS_8593'],
 [31, 1664, 9152, 1728, 9216, 256, 256, 0, 'FELDIP_HILLS_UNDERGROUND_CRUMBING_TOWER_7827'],
 [30, 3872, 10144, 3936, 10208, -96, -32, 3, 'FOSSIL_ISLAND_UNDERGROUND_VOLCANIC_MINE_15262'],
 [30, 3872, 10208, 3936, 10272, -96, -32, 3, 'FOSSIL_ISLAND_UNDERGROUND_VOLCANIC_MINE_15263'],
 [17, 2688, 9952, 2728, 9960, 0, 32, 0, 'FREMENNIK_SLAYER_CAVE_ENTRANCE'],
 [17, 2688, 9888, 2752, 9952, 0, 32, 0, 'FREMENNIK_SLAYER_CAVE_10907'],
 [8, 2880, 6336, 2944, 6400, 0, 0, 2, 'GHORROCK_PRISON_11619'],
 [8, 2880, 6400, 2944, 6464, 0, 0, 2, 'GHORROCK_PRISON_11620'],
 [8, 2944, 6336, 3008, 6400, 0, 0, 2, 'GHORROCK_PRISON_11875'],
 [8, 2944, 6400, 3008, 6464, 0, 0, 2, 'GHORROCK_PRISON_11876'], [7, 2816, 5248, 2880, 5312, 0, 0, 2, 'GOD_WARS_11346'],
 [7, 2816, 5312, 2880, 5376, 0, 0, 2, 'GOD_WARS_11347'], [7, 2880, 5248, 2944, 5312, 0, 0, 2, 'GOD_WARS_11602'],
 [7, 2880, 5312, 2944, 5376, 0, 0, 2, 'GOD_WARS_11603'],
 [7, 2936, 5264, 2976, 5304, -32, 8, 1, 'GOD_WARS_SARA_ENCAMPMENT'],
 [7, 2888, 5216, 2944, 5256, -8, 32, 0, 'GOD_WARS_ZILYANA_ROOM'],
 [7, 2816, 5152, 2880, 5200, 0, 32, 0, 'GOD_WARS_DUNGEON_11345'],
 [7, 2880, 5152, 2944, 5200, 0, 32, 0, 'GOD_WARS_DUNGEON_11601'],
 [33, 1064, 10136, 1128, 10200, 152, 40, 1, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F1_W'],
 [33, 1160, 10120, 1192, 10136, 152, 40, 1, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F1_S'],
 [33, 1128, 10136, 1192, 10200, 152, 40, 1, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F1_C'],
 [33, 1128, 10200, 1192, 10232, 152, 40, 1, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F1_N'],
 [33, 1192, 10136, 1216, 10200, 152, 40, 1, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F1_E'],
 [33, 1192, 10200, 1216, 10232, 152, 40, 1, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F1_NE'],
 [33, 1120, 10240, 1136, 10296, 144, -64, 2, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F2_W'],
 [33, 1136, 10240, 1200, 10296, 144, -64, 2, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F2_C'],
 [33, 1200, 10240, 1216, 10296, 144, -64, 2, 'KEBOS_UNDERGROUND_KARUULM_DUNGEON_F2_E'],
 [33, 1152, 10032, 1216, 10096, 64, 16, 0, 'KEBOS_UNDERGROUND_HESPORI'],
 [33, 1280, 10000, 1344, 10064, 0, 48, 0, 'KEBOS_UNDERGROUND_LIZARDMAN_TEMPLE'],
 [33, 1280, 9904, 1344, 9968, 0, 16, 0, 'KEBOS_UNDERGROUND_LIZARDMAN_CAVES'],
 [10, 2816, 10072, 2864, 10104, -56, 48, 0, 'KELDAGRIM_11166'],
 [10, 2816, 10104, 2840, 10120, -56, 48, 0, 'KELDAGRIM_11166_2'],
 [42, 3328, 9472, 3392, 9536, -64, 0, 0, 'KHARIDIAN_DESERT_UNDERGROUND_KALPHITE_13204'],
 [42, 3328, 9536, 3392, 9552, -64, 0, 0, 'KHARIDIAN_DESERT_UNDERGROUND_KALPHITE_13205'],
 [42, 3392, 9472, 3424, 9528, -64, 0, 0, 'KHARIDIAN_DESERT_UNDERGROUND_KALPHITE_13460'],
 [42, 3392, 9528, 3408, 9536, -64, 0, 0, 'KHARIDIAN_DESERT_UNDERGROUND_KALPHITE_13460_2'],
 [42, 3168, 9472, 3232, 9536, 288, 0, 2, 'KHARIDIAN_DESERT_UNDERGROUND_KALPHITE_LAIR_13972'],
 [42, 3168, 9400, 3232, 9464, 288, 72, 0, 'KHARIDIAN_DESERT_UNDERGROUND_KALPHITE_LAIR_13972_PLANE0'],
 [32, 1792, 10112, 1856, 10176, -64, 0, 0, 'KOUREND_UNDERGROUND_7070'],
 [32, 1856, 10112, 1920, 10176, -64, 0, 0, 'KOUREND_UNDERGROUND_7326'],
 [32, 1440, 9856, 1504, 9920, 32, 64, 1, 'KOUREND_UNDERGROUND_SHAYZIEN_CRYPTS_6043'],
 [32, 1440, 9952, 1504, 10016, 32, -32, 2, 'KOUREND_UNDERGROUND_SHAYZIEN_CRYPTS_6043_PLANE2'],
 [32, 1440, 10048, 1504, 10112, 32, -128, 3, 'KOUREND_UNDERGROUND_SHAYZIEN_CRYPTS_6043_PLANE3'],
 [32, 1344, 10048, 1408, 10112, 64, 0, 3, 'KOUREND_UNDERGROUND_CHASM_OF_FIRE_5789'],
 [32, 1344, 9952, 1408, 10016, 64, 96, 2, 'KOUREND_UNDERGROUND_CHASM_OF_FIRE_5789_PLANE2'],
 [32, 1344, 9856, 1408, 9920, 64, 192, 1, 'KOUREND_UNDERGROUND_CHASM_OF_FIRE_5789_PLANE1'],
 [32, 1408, 9792, 1472, 9856, 0, 64, 0, 'KOUREND_UNDERGROUND_GIANTS_DEN_5786'],
 [24, 3232, 4544, 3296, 4608, -96, 0, 1, 'LAIR_OF_TARN_RAZORLOR_12615_PLANE1'],
 [24, 3328, 4544, 3392, 4608, -192, 0, 2, 'LAIR_OF_TARN_RAZORLOR_12615_PLANE2'],
 [23, 2464, 4992, 2528, 5056, -224, 320, 0, 'MOR_UL_REK_9043'],
 [14, 3576, 9808, 3584, 9824, -64, 0, 0, 'MORYTANIA_UNDERGROUND_MEIYERDITCH_LABS_13977'],
 [14, 3520, 9792, 3560, 9816, -40, 64, 0, 'MORYTANIA_UNDERGROUND_MEIYERDITCH_LABS_13978'],
 [14, 3584, 9792, 3648, 9856, -64, 0, 0, 'MORYTANIA_UNDERGROUND_MEIYERDITCH_LABS_14233'],
 [14, 3584, 9728, 3648, 9792, -64, 0, 0, 'MORYTANIA_UNDERGROUND_MEIYERDITCH_LABS_14232'],
 [14, 3648, 9728, 3712, 9792, -64, 0, 0, 'MORYTANIA_UNDERGROUND_MEIYERDITCH_LABS_14488'],
 [14, 3648, 9664, 3712, 9728, -64, 0, 0, 'MORYTANIA_UNDERGROUND_MEIYERDITCH_LABS_14487'],
 [14, 3712, 9696, 3728, 9712, 16, -24, 0, 'MORYTANIA_UNDERGROUND_SLEPE_BASEMENT'],
 [14, 3456, 9640, 3520, 9704, 0, 24, 0, 'MORYTANIA_UNDERGROUND_SHADE_CATACOMBS_13975'],
 [14, 3456, 9560, 3520, 9624, 0, 40, 0, 'MORYTANIA_UNDERGROUND_MYREQUE_HIDEOUT_13974'],
 [14, 3640, 9896, 3688, 9944, 16, -32, 0, 'MORYTANIA_UNDERGROUND_ECTOFUNTUS_BASEMENT'],
 [14, 3672, 9880, 3688, 9896, 0, 72, 0, 'MORYTANIA_UNDERGROUND_PORT_PHASMATYS_BREWERY'],
 [14, 3376, 9848, 3440, 9912, 16, 8, 0, 'MORYTANIA_UNDERGROUND_PATERDOMUS_UNDERGROUND'],
 [45, 1408, 9544, 1472, 9600, 0, 64, 1, 'NEYPOTZLI_5782'],
 [45, 1408, 9664, 1472, 9728, 0, 0, 0, 'NEYPOTZLI_BLUE_MOON_5783'],
 [45, 1408, 6016, 1472, 6080, 0, 0, 0, 'NEYPOTZLI_BLOOD_MOON_5726'],
 [45, 1472, 9600, 1536, 9664, 0, 0, 0, 'NEYPOTZLI_ECLIPSE_MOON_6038'],
 [35, 2752, 6080, 2816, 6144, -192, 0, 1, 'PRIFDDINAS_GRAND_LIBRARY_F1_10335'],
 [35, 2752, 6144, 2816, 6208, -192, 0, 1, 'PRIFDDINAS_GRAND_LIBRARY_F1_10336'],
 [35, 2816, 6080, 2880, 6144, -192, 0, 1, 'PRIFDDINAS_GRAND_LIBRARY_F1_10591'],
 [35, 2816, 6144, 2880, 6208, -192, 0, 1, 'PRIFDDINAS_GRAND_LIBRARY_F1_10592'],
 [35, 2944, 6080, 3008, 6144, -384, 0, 2, 'PRIFDDINAS_GRAND_LIBRARY_F2_10335'],
 [35, 2944, 6144, 3008, 6208, -384, 0, 2, 'PRIFDDINAS_GRAND_LIBRARY_F2_10336'],
 [35, 3008, 6080, 3072, 6144, -384, 0, 2, 'PRIFDDINAS_GRAND_LIBRARY_F2_10591'],
 [35, 3008, 6144, 3072, 6208, -384, 0, 2, 'PRIFDDINAS_GRAND_LIBRARY_F2_10592'],
 [34, 3264, 12352, 3328, 12416, 0, 64, 0, 'PRIFFDDINAS_UNDERGROUND_TRAHAERN_MINE'],
 [34, 3264, 12416, 3328, 12480, -256, -6400, 0, 'PRIFFDDINAS_UNDERGROUND_ZALCANO'],
 [34, 3216, 12488, 3240, 12512, -192, -6368, 1, 'PRIFFDDINAS_UNDERGROUND_GAUNTLET'],
 [18, 1888, 5088, 1952, 5152, -32, 96, 1, 'STRONGHOLD_OF_SECURITY_7505_PLANE1'],
 [18, 1920, 4992, 1984, 5056, -64, 192, 2, 'STRONGHOLD_OF_SECURITY_7505_PLANE2'],
 [18, 1952, 4896, 2016, 4960, -96, 288, 3, 'STRONGHOLD_OF_SECURITY_7505_PLANE3'],
 [20, 2752, 9600, 2816, 9664, 0, 192, 1, 'TAVERLEY_DUNGEON_11161'],
 [20, 2816, 9600, 2880, 9664, 0, 192, 1, 'TAVERLEY_DUNGEON_11417'],
 [20, 2880, 9600, 2944, 9664, 0, 192, 1, 'TAVERLEY_DUNGEON_11673'],
 [20, 2944, 9608, 2976, 9656, 0, 192, 1, 'TAVERLEY_DUNGEON_11929'],
 [20, 2840, 9984, 2880, 10008, 64, -32, 0, 'WARRIORS_GUILD_BASEMENT_11675'],
 [20, 2656, 9856, 2720, 9912, -1376, -8632, 0, 'CERBERUS_LAIR_5139'],
 [22, 2880, 10080, 2944, 10144, -64, -32, 1, 'TROLL_STRONGHOLD_11421_PLANE1'],
 [22, 2944, 10112, 3008, 10176, -128, -64, 2, 'TROLL_STRONGHOLD_11421_PLANE2'],
 [25, 2544, 9728, 2608, 9792, -752, -5376, 0, 'WATERBIRTH_DUNGEON_7236_F0'],
 [25, 2608, 9728, 2672, 9792, -752, -5376, 0, 'WATERBIRTH_DUNGEON_7492_F0'],
 [25, 2672, 9728, 2736, 9792, -752, -5376, 0, 'WATERBIRTH_DUNGEON_7748_F0'],
 [25, 2544, 9824, 2608, 9888, -752, -5472, 1, 'WATERBIRTH_DUNGEON_7236_F1'],
 [25, 2608, 9824, 2672, 9888, -752, -5472, 1, 'WATERBIRTH_DUNGEON_7492_F1'],
 [25, 2672, 9824, 2736, 9888, -752, -5472, 1, 'WATERBIRTH_DUNGEON_7748_F1'],
 [25, 2544, 9920, 2608, 9984, -752, -5568, 2, 'WATERBIRTH_DUNGEON_7236_F2'],
 [25, 2608, 9920, 2672, 9984, -752, -5568, 2, 'WATERBIRTH_DUNGEON_7492_F2'],
 [25, 2672, 9920, 2736, 9984, -752, -5568, 2, 'WATERBIRTH_DUNGEON_7748_F2'],
 [25, 2544, 10016, 2608, 10080, -752, -5664, 3, 'WATERBIRTH_DUNGEON_7236_F3'],
 [25, 2608, 10016, 2672, 10080, -752, -5664, 3, 'WATERBIRTH_DUNGEON_7492_F3'],
 [25, 2672, 10016, 2736, 10080, -752, -5664, 3, 'WATERBIRTH_DUNGEON_7748_F3'],
 [25, 2592, 9632, 2656, 9696, 288, -5216, 0, 'WATERBIRTH_DUNGEON_DAGG_KINGS_11589'],
 [25, 2656, 9632, 2720, 9696, 224, -5280, 0, 'WATERBIRTH_DUNGEON_IRONMAN_DAGG_KINGS_11588'],
 [26, 3280, 10064, 3320, 10096, -320, -5696, 2, 'WILDERNESS_DUNGEONS_CORPOREAL_BEAST'],
 [26, 3088, 10240, 3136, 10288, -840, -5568, 0, 'WILDERNESS_DUNGEONS_KBD_LAIR'],
 [26, 3072, 10320, 3128, 10376, -576, -5640, 0, 'WILDERNESS_DUNGEONS_MAGE_ARENA_BANK'],
 [26, 2928, 10072, 2992, 10128, 80, 40, 0, 'WILDERNESS_DUNGEONS_GWD'],
 [26, 3008, 10112, 3024, 10128, 48, 24, 3, 'WILDERNESS_DUNGEONS_GWD_ENTRANCE'],
 [26, 2992, 10128, 3024, 10152, 48, 24, 3, 'WILDERNESS_DUNGEONS_GWD_ENTRANCE_2'],
 [27, 2624, 9472, 2640, 9488, -16, 88, 0, 'YANILLE_UNDERGROUND_SMALL_ROOM'],
 [27, 2520, 9424, 2568, 9472, 40, 120, 0, 'YANILLE_UNDERGROUND_POISON_SPIDERS']]

def key(value):
    return re.sub(r'\s+', ' ', wiki.clean(value).replace('\u2019', "'")).strip().lower()

def source_graph():
    locations = {d['locationId']: d for _, d in wiki.read_sources('locations')}
    overrides = json.loads((SOURCE / 'advisor/overrides.json').read_text())
    for lid, patch in overrides.get('locations', {}).items():
        locations[lid].update(patch)
    monsters = {}
    for path, data in wiki.read_sources('monsters'):
        data['_path'] = str(path.relative_to(SOURCE))
        data['_locations'] = set([data['locationId']] if data.get('locationId') else [])
        monsters[data['variantId']] = data
    for _, task in wiki.read_sources('tasks'):
        for mid in task.get('variantIds', []):
            monster = monsters[mid]
            for variant in task.get('variantInfo', []):
                if variant.get('variantId') != mid:
                    continue
                for name in variant.get('locations', []):
                    monster['_locations'].update(lid for lid in task.get('locationIds', [])
                        if key(locations[lid]['name']) == key(name))
            if not monster['_locations'] and not monster.get('boss'):
                monster['_locations'].update(task.get('locationIds', []))
    return monsters, locations

def requested_titles(monsters):
    refs = json.loads((SOURCE / 'advisor/wiki-audit.json').read_text())['records']
    result = {}
    for mid, monster in monsters.items():
        record = refs.get(monster['_path'], {})
        evidence = record.get('evidence', []) + monster.get('evidence', [])
        result[mid] = list(dict.fromkeys(e['title'] for e in evidence
            if not e.get('missing') and e.get('title') and '/Strategies' not in e['title']
            and not e['title'].startswith(('Slayer task/', 'Update:'))))
        if mid in TITLE_OVERRIDES:
            result[mid] = TITLE_OVERRIDES[mid]
    return result

def loclines(page):
    rows = []
    for _, _, body in wiki.templates(wiki.body(page)):
        name, data, positional = wiki.params(body)
        if name != 'locline':
            continue
        points = []
        for value in positional:
            fields = dict(re.findall(r'\b(x|y|plane)\s*:\s*(\d+)', value))
            if re.fullmatch(r'\s*\d+\s*,\s*\d+\s*', value):
                fields = dict(zip(('x', 'y'), re.findall(r'\d+', value)))
            if 'x' in fields and 'y' in fields:
                # Module:Map's documented default plane is zero.
                plane = fields.get('plane', data.get('plane', '0'))
                if str(plane).isdigit():
                    points.append((int(fields['x']), int(fields['y']), int(plane)))
        if data.get('x', '').isdigit() and data.get('y', '').isdigit():
            points.append((int(data['x']), int(data['y']), int(data.get('plane', '0'))))
        rows.append({'location': wiki.clean(data.get('location', '')), 'data': data,
            'points': points, 'evidence': wiki.evidence(page)})
    return rows

def region(row):
    area = key(row['data'].get('leagueregion', ''))
    if 'wilderness' in area or 'wilderness' in key(row['location']):
        return 'WILDERNESS'
    if area and area not in ('n/a', '?', 'unknown'):
        return 'NON_WILDERNESS'
    return 'UNKNOWN'

def matches(row, monster, lid, location):
    if lid == 'araxxor-lair' and monster['variantId'] != 'araxxor':
        return False
    if lid == 'zanaris-cosmic-altar-zygomites' and monster['variantId'] != 'zygomite-level-86':
        return False
    if lid == 'zanaris-furnace-zygomites' and monster['variantId'] != 'zygomite-level-74':
        return False
    levels = [int(v) for v in re.findall(r'\b\d+\b', wiki.clean(row['data'].get('levels', '')))]
    if levels and monster.get('combatLevel', 0) and monster['combatLevel'] not in levels:
        return False
    if lid.startswith('spiritual-'):
        style = next((s for s in ('mage', 'ranger', 'warrior') if lid.startswith('spiritual-' + s + 's-')), None)
        if style is None or not monster['variantId'].startswith('spiritual-' + style + '-'):
            return False
        label = ('God Wars Dungeon - Ancient Prison' if 'ancient-prison' in lid else
            'Wilderness God Wars Dungeon' if 'wilderness-' in lid else 'God Wars Dungeon')
        return key(row['location']) == key(label)
    if lid.startswith('poison-waste-dungeon-'):
        species = 'terrorbird' if '-terrorbird-' in lid else 'tortoise'
        if species not in key(monster['name']):
            return False
        level = 'lower' if lid.endswith('-lower') else 'main'
        return key(row['location']) == 'poison waste dungeon - ' + level + ' level'
    if 'slayer-tower-top-floor' in lid and any(p[2] != 2 for p in row['points']):
        return False
    if lid in GENERIC:
        return region(row) == 'NON_WILDERNESS' and not any(s in key(row['location'])
            for s in ('during ', 'instanced', 'unreachable', 'in prison', 'nightmare zone'))
    if lid in WILD_GENERIC:
        return region(row) == 'WILDERNESS'
    if not location.get('wilderness') and region(row) == 'WILDERNESS':
        return False
    labels = [location['name']] + ALIASES.get(lid, [])
    return key(row['location']) in {key(label) for label in labels}

def blob_evidence(repository, commit, filename, line):
    timestamp = {UPSTREAM_COMMIT: '2026-09-03T01:19:09Z',
        QUEST_HELPER_COMMIT: '2026-09-01T09:10:12Z'}[commit]
    return {'url': f'https://github.com/{repository}/blob/{commit}/{filename}#L{line}',
        'title': filename + ':' + str(line), 'pageId': 0, 'revisionId': 0, 'timestamp': timestamp, 'missing': False}

def transport_points(transport_root, filename, lines, column, expected):
    relative = 'src/main/resources/transports/' + filename
    source = (transport_root / relative).read_text().splitlines()
    points = []
    for line in lines:
        row = source[line - 1]
        if expected not in row:
            raise ValueError('Pinned transport row changed: ' + relative + ':' + str(line))
        points.append(tuple(int(n) for n in row.split('\t')[column].split()))
    return points, [blob_evidence('Skretzo/shortest-path', UPSTREAM_COMMIT, relative,
        line) for line in lines]

def world_points(row):
    map_id = row['data'].get('mapid', '0')
    if map_id in ('0', '-1'):
        return row['points']
    result = []
    for x, y, _ in row['points']:
        candidates = {(x + dx, y + dy, plane)
            for mid, x1, y1, x2, y2, dx, dy, plane, _ in MAP_TRANSFORMS
            if str(mid) == map_id and x1 <= x < x2 and y1 <= y < y2}
        if len(candidates) == 1:
            result.extend(candidates)
    return result

# Each index is an explicitly reviewed pin in the location page's raw Map
# template. These are entrance fallbacks, never guessed dungeon centres.
LOCATION_ENTRANCES = {
    'wilderness-slayer-cave': ('Wilderness Slayer Cave', 0),
    'wilderness-slayer-cave-dust-devils': ('Wilderness Slayer Cave', 0),
    'wilderness-slayer-cave-hellhounds': ('Wilderness Slayer Cave', 0),
    'wilderness-slayer-cave-ice-giants': ('Wilderness Slayer Cave', 0),
    'wilderness-slayer-cave-jellies': ('Wilderness Slayer Cave', 0),
    'wilderness-slayer-cave-lesser-demons': ('Wilderness Slayer Cave', 0),
    'wilderness-slayer-cave-nechryael': ('Wilderness Slayer Cave', 0),
    'stronghold-slayer-cave': ('Stronghold Slayer Cave', 0),
    'stronghold-slayer-cave-fire-giants': ('Stronghold Slayer Cave', 0),
    'fremennik-slayer-dungeon': ('Fremennik Slayer Dungeon', 0),
    'fremennik-slayer-dungeon-cave-crawlers': ('Fremennik Slayer Dungeon', 0),
    'fremennik-slayer-dungeon-cockatrice': ('Fremennik Slayer Dungeon', 0),
    'fremennik-slayer-dungeon-pyrefiends': ('Fremennik Slayer Dungeon', 0),
    'fremennik-slayer-dungeon-rockslugs': ('Fremennik Slayer Dungeon', 0),
    'fremennik-slayer-dungeon-turoths': ('Fremennik Slayer Dungeon', 0),
    'fremennik-slayer-dungeon-kurasks': ('Fremennik Slayer Dungeon', 0),
    'taverley-dungeon': ('Taverley Dungeon', 0),
    'brimhaven-dungeon': ('Brimhaven Dungeon', 0),
    'brimhaven-dungeon-baby-red-dragons': ('Brimhaven Dungeon', 0),
    'brimhaven-dungeon-red-dragons': ('Brimhaven Dungeon', 0),
    'brimhaven-dungeon-fire-giants': ('Brimhaven Dungeon', 0),
    'brimhaven-dungeon-metal-dragons': ('Brimhaven Dungeon', 0),
    'brimhaven-dungeon-metal-dragons-task-only': ('Brimhaven Dungeon', 0),
    'mos-le-harmless-cave': ("Mos Le'Harmless Cave", 0),
    'ancient-cavern-metal-dragons': ('Ancient Cavern', 1),
    'ancient-cavern-waterfiends': ('Ancient Cavern', 1),
    'edgeville-dungeon-wilderness-skeletons': ('Edgeville Dungeon', 1),
    'edgeville-dungeon-wilderness': ('Edgeville Dungeon', 1),
    'earth-warriors-wilderness': ('Edgeville Dungeon', 1),
    'chaos-druid-wilderness': ('Edgeville Dungeon', 1),
    'hill-giants-area': ('Edgeville Dungeon', 1),
}

def location_entrance(lid, pages, transport_root):
    if lid.startswith('catacombs-of-kourend'):
        return {'label': 'Catacombs of Kourend dark altar - continue to selected monsters', 'arrival': 'ENTRANCE',
            'points': [(1666, 10050, 0)],
            'evidence': [wiki.evidence(pages['Catacombs of Kourend']), blob_evidence('Skretzo/shortest-path', UPSTREAM_COMMIT,
                'src/main/resources/transports/seasonal_transports.tsv', 183)],
            'note': 'Verified Catacombs central arrival; continue to the selected monster section manually.'}
    if lid not in LOCATION_ENTRANCES:
        return None
    title, index = LOCATION_ENTRANCES[lid]
    maps = [wiki.params(b) for _, _, b in wiki.templates(wiki.body(pages[title])) if wiki.params(b)[0] == 'map']
    _, data, positional = maps[index]
    if data.get('mtype') != 'pin' or data.get('mapid', '0') != '0':
        raise ValueError('Reviewed entrance Map template changed: ' + title)
    points = []
    for value in positional:
        match = re.match(r'\s*(?:x:)?(\d+)\s*[:,]\s*(?:y:)?(\d+)(?:,|\s*$)', value)
        if match:
            points.append((int(match[1]), int(match[2]), 0))
    if not points:
        raise ValueError('Reviewed entrance pin missing: ' + title)
    evidence = [wiki.evidence(pages[title])]
    # Wiki scenery pins can sit on blocked object footprints. Where a normal
    # transport declares an approach, use its exact origin instead.
    normal = {
        'Edgeville Dungeon': ('transports.tsv', [816], 'Climb-down Trapdoor 1581'),
        'Fremennik Slayer Dungeon': ('transports.tsv', [4409, 4411, 4413], 'Enter Cave Entrance 2123'),
        'Taverley Dungeon': ('transports.tsv', [1144, 1146, 1148, 1150], 'Climb-down Ladder 16680'),
        'Brimhaven Dungeon': ('transports.tsv', [4757, 4759, 4761], 'Enter Dungeon entrance 20876'),
        "Mos Le'Harmless Cave": ('transports.tsv', [3636], 'Enter Cave entrance 3650'),
        'Ancient Cavern': ('transports.tsv', [5043, 5044], 'Jump-into Whirlpool 25274'),
        'Wilderness Slayer Cave': ('transports.tsv', [3208, 3212, 3213], 'Walk-down Stairs'),
        'Stronghold Slayer Cave': ('teleportation_items.tsv', [90], 'Slayer ring: Stronghold'),
    }
    if title in normal:
        filename, lines, expected = normal[title]
        points, transport_evidence = transport_points(transport_root, filename, lines, 0, expected)
        evidence.extend(transport_evidence)
        if title == 'Brimhaven Dungeon':
            alternate, alternate_evidence = transport_points(transport_root, 'transports.tsv', [4766, 4767, 4768, 4769], 0, 'Climb Rope 30200')
            points.extend(alternate)
            evidence.extend(alternate_evidence)
    note = 'Verified dungeon entrance; continue inside to the selected monsters manually. '
    if lid in ('edgeville-dungeon-wilderness-skeletons', 'edgeville-dungeon-wilderness', 'earth-warriors-wilderness', 'chaos-druid-wilderness'):
        note += 'The entrance is outside the Wilderness; the selected monsters are in the Wilderness section. '
    return {'label': title + ' entrance - continue to selected monsters', 'arrival': 'ENTRANCE',
        'points': points, 'evidence': evidence, 'note': note.strip()}

def entrance(mid, monster, lid, pages, transport_root):
    # Conditional encounters and superior spawns do not become independent routes
    # merely because an inherited catalogue join mentions the boss location.
    minions = {'skeleton-hellhound-calvar-ion', 'greater-skeleton-hellhound-calvar-ion',
        'skeleton-hellhound-vet-ion', 'greater-skeleton-hellhound-vet-ion',
        'flight-kilisa', 'flockleader-geerin', 'wingman-skree', 'balfrug-kreeyath',
        'tstanon-karlak', 'zakln-gritch'}
    if not monster.get('boss') and mid not in minions:
        return None
    normal_bosses = {
        'scurrius-lair': ('transports.tsv', [958], 0, 'Climb-down Manhole 882', 'Scurrius',
            'Varrock Sewers entrance - continue east to Scurrius'),
        'abyssal-nexus': ('fairy_rings.tsv', [103], 1, 'D I P', 'Abyssal Nexus',
            'Abyssal Nexus fairy ring - continue to the Sire chambers'),
        'cerberus-lair': ('transports.tsv', [1165, 1167, 1169], 0, 'Crawl Cave', "Cerberus' Lair",
            "Taverley Dungeon entrance to Cerberus' Lair"),
        'mole-hole': ('teleportation_items.tsv', [86], 0, 'Ring of wealth: Falador Park', 'Mole hill',
            'Falador Park arrival - continue to the mole hills'),
        'forthos-sarachnis': ('transports.tsv', [4851, 4861, 4862, 4863, 4864], 0, 'Climb-down', 'Forthos Dungeon',
            'Forthos Dungeon entrances - continue to Sarachnis'),
        'scorpion-pit-cave': ('transports.tsv', [2496, 2497], 0, 'Enter Cavern 26762', 'Scorpia',
            'Scorpion Pit cavern entrance'),
        'thermonuclear-smoke-devil-lair': ('fairy_rings.tsv', [82], 1, 'B K P', 'Smoke Devil Dungeon',
            'BKP fairy ring approach - continue to Smoke Devil Dungeon'),
    }
    if lid in normal_bosses:
        filename, lines, column, expected, title, label = normal_bosses[lid]
        points, evidence = transport_points(transport_root, filename, lines, column, expected)
        return {'label': label, 'arrival': 'ENTRANCE', 'points': points,
            'evidence': [wiki.evidence(pages[title])] + evidence,
            'note': 'Route stops at the labelled arrival point. Continue to the selected encounter and enter manually.'}
    if (lid == 'araxxor-lair' and mid == 'araxxor') or lid == 'scurrius-lair':
        title = 'Cave (Morytania Spider Cave)' if lid == 'araxxor-lair' else 'Broken bars'
        maps = [wiki.params(b) for _, _, b in wiki.templates(wiki.body(pages[title])) if wiki.params(b)[0] == 'map']
        _, data, positional = maps[0]
        if data.get('mapid') != '-1' or data.get('mtype') != 'pin':
            raise ValueError('Reviewed scenery map changed: ' + title)
        point = (int(data['x']), int(data['y']), 0) if 'x' in data else tuple(int(n) for n in positional[0].split(',')) + (0,)
        return {'label': 'Morytania Spider Cave entrance - continue to Araxxor' if lid == 'araxxor-lair' else "Broken bars outside Scurrius's lair",
            'arrival': 'ENTRANCE', 'points': [point], 'evidence': [wiki.evidence(pages[title])],
            'note': 'Verified entrance scenery; enter the encounter manually.'}
    if lid == 'shellbane-gryphon-cave' and mid == 'shellbane-gryphon':
        body = wiki.body(pages['Shellbane Gryphon'])
        maps = [wiki.params(b)[1] for _, _, b in wiki.templates(body) if wiki.params(b)[0] == 'map']
        data = next(d for d in maps if d.get('caption') == 'The entrance to the cave.')
        return {'label': 'Shellbane Gryphon cave entrance', 'arrival': 'ENTRANCE',
            'points': [(int(data['x']), int(data['y']), 0)], 'evidence': [wiki.evidence(pages['Shellbane Gryphon'])],
            'note': 'Enter the instanced encounter manually.'}
    if lid == 'king-black-dragon-lair':
        filename = 'src/main/resources/transports/teleportation_levers.tsv'
        row = (transport_root / filename).read_text().splitlines()[3]
        if 'Pull Lever 1816' not in row:
            raise ValueError('Pinned King Black Dragon lever source changed')
        return {'label': 'King Black Dragon lever entrance in the Wilderness dungeon', 'arrival': 'ENTRANCE',
            'points': [tuple(int(n) for n in row.split('\t')[0].split())],
            'evidence': [wiki.evidence(pages['King Black Dragon']), blob_evidence('Skretzo/shortest-path', UPSTREAM_COMMIT, filename, 4)],
            'note': 'Route stops at the Wilderness-side lever; enter the King Black Dragon lair manually. The journey is through the Wilderness.'}
    if lid not in ENTRANCES:
        return None
    line, expected, label, wiki_titles = ENTRANCES[lid]
    filename = 'src/main/resources/transports/seasonal_transports.tsv'
    row = (transport_root / filename).read_text().splitlines()[line - 1]
    if expected not in row:
        raise ValueError('Pinned entrance line changed: ' + expected)
    points = [tuple(int(n) for n in row.split('\t')[0].split())]
    evidence = [wiki.evidence(pages[t]) for t in wiki_titles]
    evidence.append(blob_evidence('Skretzo/shortest-path', UPSTREAM_COMMIT, filename, line))
    return {'label': label, 'arrival': 'ENTRANCE', 'points': points, 'evidence': evidence,
        'note': 'Route ends at the labelled approach, outside the encounter. Continue and enter manually.'}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh', action='store_true')
    parser.add_argument('--pages', default='/tmp/aio-routing-pages.json')
    parser.add_argument('--shortest-path-source', default='/tmp/aio-shortest-path-source')
    args = parser.parse_args()
    monsters, locations = source_graph()
    titles = requested_titles(monsters)
    path = pathlib.Path(args.pages)
    pages = json.loads(path.read_text()) if path.exists() and not args.refresh else {}
    client = wiki.Wiki('/tmp/aio-routing-wiki-cache', refresh=args.refresh)
    needed = {t for names in titles.values() for t in names}
    needed.update(t for entry in ENTRANCES.values() for t in entry[3])
    needed.update(('Module:Map', 'RuneScape:Map/mapIDs', 'Shellbane Gryphon', 'King Black Dragon', 'Catacombs of Kourend',
        'Cave (Morytania Spider Cave)', 'Broken bars', 'Abyssal Nexus', "Cerberus' Lair", 'Mole hill', 'Forthos Dungeon',
        'Scorpia', 'Smoke Devil Dungeon'))
    needed.update(t for t, _ in LOCATION_ENTRANCES.values())
    client.fetch(needed - pages.keys())
    pages.update(client.pages)
    wiki.save(path, pages)
    destinations, unsupported = [], []
    transport_root = pathlib.Path(args.shortest_path_source)
    for mid, monster in sorted(monsters.items()):
        rows = []
        for title in titles[mid]:
            page = pages.get(title, {})
            if not page.get('missing'):
                rows.extend(loclines(page))
        for lid in sorted(monster['_locations']):
            explicit = entrance(mid, monster, lid, pages, transport_root)
            matches_here = [r for r in rows if matches(r, monster, lid, locations[lid])]
            points = sorted({p for row in matches_here for p in world_points(row)})
            if not points and matches_here and not explicit:
                explicit = location_entrance(lid, pages, transport_root)
            if explicit:
                points = explicit['points']
            if not points or len(points) > 512:
                if not rows:
                    reason = 'No independent spawn row: conditional encounter/superior or Wiki coordinate gap'
                elif not matches_here:
                    reason = 'No compatible exact location/level row; inherited join or unreviewed source label'
                elif any(r['points'] for r in matches_here):
                    reason = 'Legacy map coordinates lack an unambiguous world conversion or verified entrance'
                else:
                    reason = 'No coordinate points in matched Wiki rows'
                if len(points) > 512: reason = 'More than 512 spawn targets; requires a reviewed subset'
                unsupported.append({'monsterId': mid, 'locationId': lid, 'reason': reason,
                    'candidateLocations': sorted({r['location'] for r in rows})})
                continue
            evidence = {r['evidence']['title']: r['evidence'] for r in matches_here}
            if any(r['data'].get('mapid', '0') not in ('0', '-1') and world_points(r) for r in matches_here):
                evidence['Quest Helper world map conversions'] = blob_evidence('Zoinkwiz/quest-helper', QUEST_HELPER_COMMIT,
                    'src/main/java/com/questhelper/util/worldmap/WorldMapPointMapping.java', 39)
            if any(not r['data'].get('plane') for r in matches_here) and 'Module:Map' in pages:
                evidence['Module:Map'] = wiki.evidence(pages['Module:Map'])
            arrival = 'ENTRANCE' if lid in ENTRANCE_LOCATIONS else 'MONSTER'
            result = {'id': mid + '-at-' + lid, 'monsterIds': [mid], 'locationId': lid,
                'label': locations[lid]['name'] + (' entrance' if arrival == 'ENTRANCE' else ' - ' + monster['name']),
                'arrival': arrival, 'points': [dict(zip(('x', 'y', 'plane'), p)) for p in points],
                'evidence': list(evidence.values()),
                'note': 'Spawn locations for the selected monster in this area.'
                    if arrival == 'MONSTER' else 'Route ends at the lair entrance, outside the encounter.'}
            if explicit:
                result.update(explicit)
                result['points'] = [dict(zip(('x', 'y', 'plane'), p)) for p in points]
            elif any(len(re.findall(r'\b\d+\b', r['data'].get('levels', ''))) > 1 for r in matches_here):
                result['note'] += ' This area has multiple listed combat levels; the nearest spawn may differ from the selected level.'
            if not explicit and sum(len(world_points(r)) for r in matches_here) < sum(len(r['points']) for r in matches_here):
                result['note'] += ' Only verified spawns in this area are included.'
            destinations.append(result)
    wiki.save(SOURCE / 'advisor/routing-destinations.json', {'schemaVersion': 1,
        'reviewedAt': datetime.now(timezone.utc).isoformat(), 'destinations': destinations})
    wiki.save(pathlib.Path('/tmp/aio-routing-gaps.json'), unsupported)
    counts = collections.Counter(r['reason'] for r in unsupported)
    report = ['# Routing data coverage', '', f'- Exact bindings with routes: {len(destinations)}',
        f'- Unsupported bindings: {len(unsupported)}', f'- Route points: {sum(len(d["points"]) for d in destinations)}',
        '', '## Gaps', '']
    report.extend(f'- {n}: {reason}' for reason, n in counts.items())
    report.extend(['', '| Monster | Location | Reason |', '| --- | --- | --- |'])
    report.extend(f'| {r["monsterId"]} | {r["locationId"]} | {r["reason"]} |' for r in unsupported)
    pathlib.Path('/tmp/aio-routing-data-report.md').write_text('\n'.join(report) + '\n')
    print('Routing bindings', len(destinations), 'unsupported', len(unsupported), 'points', sum(len(d['points']) for d in destinations))

if __name__ == '__main__':
    main()

# Modular Slayer Data Verification

Date: 2026-07-01
Status: Passed

## What Changed

The editable source of truth moved from `src/main/resources/data/slayer-data.json` to modular source files
under `src/main/data/slayer`. Gradle now generates the runtime resource at
`build/generated/resources/slayer/data/slayer-data.json` before resources/tests run.

Runtime consumers still load `/data/slayer-data.json` through `SlayerDataService`.

## Source Counts

`src/main/data/slayer` contains 621 source files, including 612 JSON source files and 9 legacy strategy
Markdown files:

- 9 masters
- 43 tasks
- 43 monster family directories
- 230 monster variant files
- 166 locations
- 79 weapons
- 9 legacy strategy Markdown files
- 85 migrated strategy JSON files at `strategies/<strategy-id>/strategy.json`, including
  every variant in `monsters/boss`, `strategies/k-ril-tsutsaroth/strategy.json`,
  `strategies/abyssal-demons/strategy.json`, `strategies/ankou/strategy.json`, and
  `strategies/aquanite/strategy.json`, `strategies/araxytes/strategy.json`, and
  `strategies/aviansie/strategy.json`, `strategies/basilisks/strategy.json`, and
  `strategies/black-demons/strategy.json`, `strategies/black-dragons/strategy.json`, and
  `strategies/bloodveld/strategy.json`, `strategies/blue-dragons/strategy.json`,
  `strategies/cave-horrors/strategy.json`, `strategies/cave-kraken/strategy.json`, and
  `strategies/kraken/strategy.json`, `strategies/dagannoth/strategy.json`, and expanded
  `strategies/dagannoth-{rex,prime,supreme}/strategy.json`, and
  `strategies/dark-beasts/strategy.json`, `strategies/drakes/strategy.json`,
  `strategies/dust-devils/strategy.json`, `strategies/elves/strategy.json`,
  `strategies/fire-giants/strategy.json`, `strategies/fossil-island-wyverns/strategy.json`,
  `strategies/frost-dragons/strategy.json`, `strategies/gargoyles/strategy.json`, and
  `strategies/grotesque-guardians-dusk/strategy.json`, `strategies/gryphons/strategy.json`, and
  `strategies/hellhounds/strategy.json`, `strategies/kalphite/strategy.json`, and
  `strategies/kalphite-queen-{crawling-form,airborne-form}/strategy.json`, and
  `strategies/kurask/strategy.json`, plus migrated metal dragon strategies for bronze, iron, steel,
  mithril, adamant, and rune dragons, `strategies/mutated-zygomites/strategy.json`,
  `strategies/nechryael/strategy.json`, `strategies/red-dragons/strategy.json`, and
  `strategies/skeletal-wyvern/strategy.json`, `strategies/smoke-devil/strategy.json`, and
  `strategies/nuclear-smoke-devil/strategy.json`, plus `strategies/spiritual-creatures/strategy.json`,
  `strategies/suqahs/strategy.json`, `strategies/trolls/strategy.json`, and
  `strategies/tzhaar/strategy.json`, plus migrated TzHaar boss strategies at
  `strategies/tztok-jad/strategy.json` and `strategies/tzkal-zuk/strategy.json`,
  `strategies/vampyres/strategy.json`, migrated
  `strategies/vyrewatch-sentinel/strategy.json`, `strategies/warped-creatures/strategy.json`,
  `strategies/waterfiends/strategy.json`, and `strategies/wyrms/strategy.json`

There are no JSON files directly under `src/main/data/slayer/monsters`; every monster source variant now
lives under `monsters/<family-id>/<variant-id-or-name>-lvl###.json`.

Generated runtime output preserves the migrated dataset totals:

- 43 tasks
- 228 task-visible variants
- 210 strategy-bearing variants
- 187 task-visible location entries

The Boss meta-task now has JSON-backed strategy source coverage for all 33 boss variants. Phantom Muspah was
the only missing boss strategy; it now links to `strategies/phantom-muspah/strategy.json` with ranged,
magic-switch, melee-form, shielded-phase, and shield-skip method coverage.

Cave horrors now has task-page JSON source coverage from `Slayer_task/Cave_horrors`, including assignment
ranges, the Horrorific extension, variant/location comparison rows, Mos Le'Harmless safespot and cannon
notes, and `strategies/cave-horrors/strategy.json` with ranged safespot, prayer melee, witchwood melee,
cannoning, and Cave abomination method coverage.

Cave kraken now has task-page JSON source coverage from `Slayer_task/Cave_krakens`, including assignment
ranges, the Krack on extension, regular Cave kraken and Kraken variant rows, Kraken Cove notes, regular
Cave kraken strategy JSON, and full `Kraken/Strategies` boss JSON with release, typeless attack, private
instance, inventory, and sustain coverage.

Dagannoth now has task-page JSON source coverage from `Slayer_task/Dagannoth`, including all assignment
ranges, Horror from the Deep and 75 combat requirements, regular/Waterbirth/spawn/fledgeling/Kings/reanimated
variant rows, location comparison rows, a dedicated Waterbirth Dagannoth runtime variant, Dagannoth-specific
Jormungand's Prison location flags, regular task strategy JSON, and expanded Dagannoth Kings JSON methods
for entering, multicombat handling, tribrid rotations, Rex-only, Prime-only, inventory, and trip optimisation.

Dark beasts now has task-page JSON source coverage from `Slayer_task/Dark_beast`, including assignment ranges,
Need More Darkness extension, Dark beast and Night beast variant rows, Iorwerth Dungeon and Mourner Tunnels
location comparison rows, task-specific location flags, and `strategies/dark-beasts/strategy.json` with melee,
prayer-bonus, location, Night beast, and inventory method coverage.

Drakes now has task-page JSON source coverage from `Slayer_task/Drake`, including assignment ranges, Karuulm
and heat-protection requirements, Drake and Guardian Drake variant rows, a Drake-specific Karuulm location
record, dragonfire and volcanic breath mechanics, dragonbane melee/ranged options, water-spell magic, Guardian
Drake special handling, and style-specific inventory coverage in `strategies/drakes/strategy.json`.

Dust devils now has task-page JSON source coverage from `Slayer_task/Dust_devils`, including all assignment
ranges and extended amounts, Desert Treasure I and facemask requirements, Dust devil and Choke devil variant
rows, Catacombs, Smoke Dungeon, and Wilderness Slayer Cave location comparison rows, task-specific Smoke
Dungeon and Wilderness Slayer Cave location flags, ranged-melee and stat-drain mechanics, and
`strategies/dust-devils/strategy.json` with Catacombs bursting/barraging, Smoke Dungeon, Wilderness,
luring, Choke devil, and inventory/loot method coverage.

Elves now has task-page JSON source coverage from `Slayer_task/Elves`, including assignment ranges, Regicide
and quest-stage location requirements, Iorwerth Archer, Elf Archer, Iorwerth Warrior, Elf Warrior, Mourner,
Prifddinas Guard, and Reanimated elf variant rows, all task-page location comparison rows from Iorwerth Camp
through Prifddinas and reanimation support, task-specific location flags, crystal halberd reach, safespot,
Mourner gear, crystal shard/enhanced seed, and Adept Reanimation mechanics, and
`strategies/elves/strategy.json` with Iorwerth Dungeon, Prifddinas guard, Lletya prayer, safespot,
Iorwerth Camp, Mourner Headquarters, reanimated elf, and inventory/loot method coverage.

Fire giants now has task-page JSON source coverage from `Slayer_task/Fire_giants`, including all assignment
ranges, the 65 combat requirement, level 86, 104, and 109 Fire giant variant rows, Branda the Fire Queen as
the Royal Titans alternative, all task-page location comparison rows, task-specific location flags for
Catacombs, Giants' Den, Konar cannon locations, Deep Wilderness Dungeon, Charred Dungeon, Smoke Dungeon,
Waterfall, and Isle of Souls, water elemental weakness and safespot mechanics, and
`strategies/fire-giants/strategy.json` with Catacombs AFK, Giants' Den cannon, Konar cannon, Wilderness,
Ranged/Magic safespot, Branda alternative, and inventory/loot method coverage.

Fossil Island wyverns now has task-page JSON source coverage from `Slayer_task/Fossil_Island_wyverns`,
including assignment ranges, Stop the Wyvern and Wyver-nother two unlocks, shield-specific icy breath
mechanics, Spitting/Taloned/Long-tailed/Ancient Wyvern variant rows, regular and task-only cave location
comparison rows, task-specific task-only cave location flags, dragonbane and air-weakness handling, and
`strategies/fossil-island-wyverns/strategy.json` with travel, shield protection, Spitting quick-clear,
melee tank, prayer melee, Magic, Ranged, Ancient Wyvern, and inventory/loot method coverage.

Frost Dragons now has monster-page and strategy-page JSON source coverage from `Frost_dragon` and
`Frost_dragon/Strategies`, including 87 Sailing and 85 combat requirements, Nieve/Duradel assignment ranges,
Chance of Heavy Frost and I see Dragons unlock notes, icy dragonfire and normal dragonfire-protection
mechanics, Grimstone Dungeon and task-only cavern location records, 100% fire elemental weakness,
dragonbane/crush/stab/ranged handling, Dragon metal sheet on-task drop-rate notes, and
`strategies/frost-dragons/strategy.json` with attacks/protection, transportation, melee, ranged, magic, and
inventory/loot method coverage.

Gargoyles now has task-page JSON source coverage from `Slayer_task/Gargoyles`, including all assignment
ranges and extended amounts, Gargoyle Smasher, Bigger and Badder, Double Trouble, and Get smashed unlock
notes, Gargoyle/Marble gargoyle/Dawn/Dusk variant rows, Slayer Tower top-floor, basement, and roof location
comparison rows, rock hammer and granite hammer finishing mechanics, basement semi-AFK routing, Marble
gargoyle special-attack handling, Grotesque Guardians trade-off guidance, and `strategies/gargoyles/strategy.json`
with hammer, basement, melee, ranged, superior, boss-choice, and inventory/loot method coverage. The legacy
Dusk Markdown strategy is now migrated to `strategies/grotesque-guardians-dusk/strategy.json`.

Gryphons now has task-page JSON source coverage from `Slayer_task/Gryphons`, plus real Gryphon and Dire
gryphon monster-page stats, including all Vannaka/Chaeldar/Nieve/Duradel assignment ranges and extensions,
Bigger and Badder, Wings Spread, and Gryphon and on unlock notes, Gryphon/Dire gryphon/Shellbane Gryphon
variant rows, western cave, eastern task-only cave, and Shellbane cave location comparison rows, 30 kg and
40 kg weight-threshold mechanics, tortugan shield requirements, 50% air weakness handling, Venator bow plus
cannon task strategy, Dire superior handling, Shellbane boss-choice guidance, and
`strategies/gryphons/strategy.json` with requirements, weight, cannon, melee, superior, boss-choice, and
inventory/loot method coverage.

Hellhounds now has task-page JSON source coverage from `Slayer_task/Hellhounds`, plus real Hellhound
monster-page stats, including all Krystilia/Vannaka/Chaeldar/Konar/Nieve/Duradel assignment ranges, regular
level 122, God Wars Dungeon level 127, Wilderness Slayer Cave level 136, Vet'ion/Calvar'ion skeleton
summons, and Cerberus variant rows, all task-page location comparison rows, task-specific flags for
Buccaneers' Laboratory, Charred Dungeon, Karuulm Slayer Dungeon, Wilderness Slayer Cave, Wilderness Resource
Area, Vet'ion spawns, Witchaven Dungeon, and Cerberus' Lair, demonbane and water-weakness handling, hard clue
scroll and smouldering stone notes, quest-counting exceptions, and `strategies/hellhounds/strategy.json`
with general protection, Stronghold cannon, Catacombs Venator bow, Wilderness Slayer Cave, safespots,
Vet'ion/Calvar'ion spawns, Cerberus choice, and inventory/loot method coverage.

Kalphite now has task-page JSON source coverage from `Slayer_task/Kalphites`, plus real Kalphite Worker,
Soldier, Guardian, and Kalphite Queen monster-page stats, including all Turael/Spria/Mazchna/Vannaka/
Chaeldar/Konar/Nieve/Duradel assignment ranges, worker/soldier/guardian/Kalphite Queen variant rows,
Kalphite Lair and Kalphite Cave location comparison rows, poison-through-prayer mechanics, Protect from
Melee plus poison-immunity handling, Kalphite Cave quick-task and experience-task routing, Keris/Keris
partisan/Keris partisan of breaching guidance, Kalphite Queen diary and 86 Agility caveats, and
`strategies/kalphite/strategy.json` with optimal tasking, worker cannon, soldier cannon, guardian avoidance,
Konar Lair, Keris weapons, Kalphite Queen choice, and inventory/loot method coverage. The legacy Kalphite
Queen crawling and airborne Markdown strategies are now migrated to JSON under
`strategies/kalphite-queen-crawling-form/strategy.json` and
`strategies/kalphite-queen-airborne-form/strategy.json`.

Kurask now has task-page JSON source coverage from `Slayer_task/Kurasks`, plus real Kurask and King kurask
monster-page stats, including all Vannaka/Chaeldar/Konar/Nieve/Duradel assignment ranges, Bigger and Badder
superior unlock coverage, Kurask and King kurask variant rows, Fremennik Slayer Dungeon, Iorwerth Dungeon,
and Kurask Lair location comparison rows, task-specific cannon-immune location flags, 70 Slayer and 65
combat requirements, leafy damage restrictions, leaf-bladed/broad ammunition/Magic Dart handling,
Protect from Melee, 75 Defence Bones to Peaches sustain, King kurask superior handling, crystal shard and
Herb sack loot notes, and `strategies/kurask/strategy.json` with damage restriction, leaf-bladed melee,
broad ammunition ranged, Bones to Peaches, superior, location-choice, and inventory/loot method coverage.

Metal dragons now has task-page JSON source coverage from `Slayer_task/Metal_dragons`, plus Bronze/Iron/
Steel/Mithril/Adamant/Rune dragon monster-page stats, including Konar/Nieve/Duradel assignment and Pedal to
the Metals extended ranges, split Catacombs profiles for bronze/iron/steel, all task-page location comparison
rows, metallic dragonfire protection, draconic/earth weakness handling, dragonbane/stab/heavy ranged/earth
style options, bronze fast-task, iron visage, rune profit guidance, adamant/rune special attacks, and
migrated JSON strategies for all six legacy metal dragon Markdown files.

Zygomites now has task-page JSON source coverage from `Slayer_task/Zygomites`, plus real Zygomite and Ancient
Zygomite monster-page stats, including Chaeldar/Konar/Nieve/Duradel assignment ranges, 60 combat and Lost City
requirements, 'Shroom sprayer unlock coverage, level 74, level 86, and Ancient Zygomite variant rows, all
task-page location comparison rows for Zanaris, Mushroom Forest, and Stalker Den, fungicide spray finishing
rules, magical melee/ranged attack handling, dragonhide/Karil's armour guidance, Protect from Melee and
Ancient Zygomite safespot handling, Konar location-lock caveats, Reagent pouch support, 40% fire weakness
handling, and `strategies/mutated-zygomites/strategy.json` with melee, ranged safespot, Ancient Zygomite, and
fire magic method coverage.

Nechryael now has task-page JSON source coverage from `Slayer_task/Nechryael`, plus real Nechryael, Greater
Nechryael, and Nechryarch monster-page stats, including Krystilia/Vannaka/Chaeldar/Konar/Nieve/Duradel
assignment and extended ranges, 85 combat requirement, demon attribute handling, regular/greater/superior
variant rows, all six task-page location comparison rows, death spawn and chaotic death spawn mechanics,
Catacombs goading-potion burst/barrage strategy, demonbane melee, Iorwerth crystal shard routing, Wilderness
Slayer Cave risk handling, Charred Dungeon caveats, Nechryarch safespot/farcast handling, and
`strategies/nechryael/strategy.json` with magic, luring, melee, wilderness, superior, and inventory/loot
method coverage.

Red dragons now has task-page JSON source coverage from `Slayer_task/Red_dragons`, plus real Red dragon,
Baby red dragon, and Brutal red dragon monster-page stats, including Seeing red unlock coverage, Konar/Nieve/
Duradel assignment ranges, partial Dragon Slayer I anti-dragon shield requirement, red/baby/brutal variant
rows, all adult/baby/brutal task-page location comparison rows, dragonfire and Protect from Magic handling,
Elite Karamja Diary noted-hide handling, Dragon Slayer II Corsair Cove bank routing, Forthos Sacred Bone
Burner and altar support, Charred Dungeon 60 Sailing caveats, Brutal red dragon long-range dragonfire and
draconic visage caveat, 50% water weakness handling, and `strategies/red-dragons/strategy.json` with unlock,
protection, adult, baby, Forthos, Corsair Cove, brutal, dragonbane, and water-magic method coverage.

Skeletal Wyverns now has monster-page and strategy-page JSON source coverage from `Skeletal_Wyvern` and
`Skeletal_Wyvern/Strategies`, including Chaeldar/Konar/Nieve/Duradel assignment ranges, Elemental Workshop I
and 70 combat assignment requirements, Wyver-nother One extension notes, current Fire elemental weakness and
defence stats, draconic-but-not-undead handling, icy-breath shield and freeze mechanics, lower and task-only
Asgarnian Ice Dungeon location records, lower-cave cannon and upper-area no-cannon flags, safespot aggression
timing, and `strategies/skeletal-wyvern/strategy.json` with requirements, attacks/protection, dragonbane
melee, ranged safespot, Ranged Void safespot, Magic Fire safespot, cannon/prayer, and inventory/loot method
coverage.

Smoke devils now has task-page, monster-page, and strategy-page JSON source coverage from
`Slayer_task/Smoke_devils`, `Smoke_devil`, `Nuclear_smoke_devil`, `Thermonuclear_smoke_devil`, and
`Smoke_devil/Strategies`, including Konar/Nieve/Duradel assignment ranges, 93 Slayer and 85 combat
requirements, Bigger and Badder superior unlock coverage, Smoke devil/Nuclear smoke devil/Thermonuclear
smoke devil variant rows, current Air elemental weaknesses and defence stats, Smoke Devil Dungeon task-only
and mixed cannon rules, facemask/Slayer helmet smoke protection, Protect from Missiles, cannon-barrage
grouping around the western skeletons, Combat Achievement cannonball-capacity notes, normal-versus-boss
task-routing guidance, and migrated `strategies/smoke-devil/strategy.json` plus
`strategies/nuclear-smoke-devil/strategy.json` with requirements, smoke protection, cannon-barrage grouping,
Magic equipment, superior handling, and inventory/loot method coverage.

Spiritual creatures now has task-page JSON source coverage from `Slayer_task/Spiritual_creatures`, plus
current Spiritual ranger, Spiritual warrior, and Spiritual mage monster-page stats, including
Krystilia/Vannaka/Chaeldar/Nieve/Duradel assignment ranges, Spiritual fervour extension, Death Plateau,
80 combat, and 60 Strength/Agility requirements, 63/68/83 Slayer split by ranger/warrior/mage, all
Armadyl/Bandos/Saradomin/Zamorak/Zaros variant rows, God Wars Dungeon, Ancient Prison, and Wilderness God Wars
Dungeon location rows, task-specific Ancient Prison and Wilderness location records, god-item tolerance
routing, Armadyl/Bandos/Zamorak/Zaros access requirements, Zaros ranger/warrior/mage special attacks,
dragon boots guidance, skip/block guidance, and `strategies/spiritual-creatures/strategy.json` with
requirements, god-item protection, target selection, Zamorak warriors, Spiritual mages, Zaros Ancient Prison,
Wilderness, and inventory/loot method coverage.

Suqahs now has task-page and monster-page JSON source coverage from `Slayer_task/Suqah` and `Suqah`,
including Nieve/Duradel assignment and extended ranges, 85 combat and partial Lunar Diplomacy requirements,
current level 111 Suqah stats, 20% Earth elemental weakness while preserving the existing MELEE recommended
style, north/south magic-versus-melee mechanics, three Lunar Isle task-specific location records, cannon and
safespot flags, seal of passage banking rules, and `strategies/suqahs/strategy.json` with north Ranged
cannon-safespot, south melee cannon, south-west dead-tree cannon placement, and banking/supply method coverage.

Trolls now has task-page and monster-page JSON source coverage from `Slayer_task/Trolls` plus the named Troll
monster pages, including Vannaka/Chaeldar/Konar/Nieve/Duradel assignment ranges, 60 combat requirement,
all task-page variant rows plus source-only Reanimated troll notes, 20 selectable runtime Troll variants,
current mountain/ice/named/thrower/quest-boss monster stats, seven task-specific location records from the
location comparison table, Konar-lockable location flags, quest-boss and Nightmare Zone notes, Neitiznot
shield handling, cannon recommendations, Fire-magic handling for ice trolls, reanimation/Sinister
Offering/bonecrusher support, and
`strategies/trolls/strategy.json` with low-level melee, mountain troll melee-cannon, island ice troll
melee-cannon, fire magic, ice troll runt quick-task, quest boss/reanimation, and inventory/loot method
coverage.

TzHaar now has task-page and monster-page JSON source coverage from `Slayer_task/TzHaar` plus current
TzHaar-Hur, TzHaar-Mej, TzHaar-Xil, TzHaar-Ket, TzTok-Jad, and TzKal-Zuk monster pages, including
Chaeldar/Nieve/Duradel assignment ranges, Hot Stuff unlock coverage, regular city variant rows, inner
level 221 Ket fire-cape access, Jad and Zuk task alternatives, Ring of wealth drop notes, Hur/Mej helper
aggro mechanics, city vent safespots, inner TzHaar-Ket-Rak burst/barrage routing, four task-specific
location records, and `strategies/tzhaar/strategy.json` with unlock, Ket/Xil safespot, inner Ket magic,
aggro-management, and Jad/Zuk alternative method coverage. The legacy TzTok-Jad and TzKal-Zuk Markdown
strategies are now migrated to `strategies/tztok-jad/strategy.json` and
`strategies/tzkal-zuk/strategy.json` with Fight Cave prayer-switching and Inferno shield-movement
breakdowns.

Vampyres now has task-page and monster-page JSON source coverage from `Slayer_task/Vampyres`, Feral
Vampyre, Vampyre Juvenile, Vampyre Juvinate, Vyrewatch, Vyrewatch Sentinel, and
`Vyrewatch_Sentinel/Strategies`, including Priest in Peril and 35 combat requirements, Mazchna/Vannaka
baseline assignments, Actual Vampyre Slayer and More at stake unlocks, all task-page assignment and
extended ranges, Feral/Juvenile/Juvinate/Vyrewatch/Sentinel variant rows, seven task-specific location rows,
Wilderness God Wars Dungeon wilderness flagging, Guthix balance and mist mechanics, flail-only Vyrewatch
damage rules, Slepe prayer routing, Darkmeyer bank/altar Sentinel routing, Efaritay's aid handling, blood
shard notes, and quest-boss exception notes. The legacy Vyrewatch Sentinel Markdown strategy is now migrated
to `strategies/vyrewatch-sentinel/strategy.json`, and regular Vampyre task routing is covered by
`strategies/vampyres/strategy.json`.

Warped creatures now has task-page and monster-page JSON source coverage from
`Slayer_task/Warped_creatures`, Warped Terrorbird, Warped Tortoise, Mutated Tortoise, and Mutated
Terrorbird, including The Path of Glouphrie and 56 Slayer requirements, Warped Reality and Bigger and
Badder unlocks, crystal chime invulnerability handling, Chaeldar/Konar/Nieve/Duradel assignment ranges,
five regular/superior variant rows, four Poison Waste Dungeon location rows, tortoise Protect from Melee
and earth weakness handling, Ranged/Magic tortoise safespots, terrorbird earmuff/Slayer helmet damage
reduction, prayer-switch/pathing safespotting, cannon placement at 1490,4263, and
`strategies/warped-creatures/strategy.json` with prayer melee, Ranged, Magic, terrorbird safespot,
cannoning, and superior methods.

Waterfiends now has task-page and monster-page JSON source coverage from `Slayer_task/Waterfiends` and
`Waterfiend`, including 75 combat and Ancient Cavern access requirements, Konar/Duradel assignment ranges,
current demon attribute handling, current crush/heavy-ranged/earth weakness defence profile, Magic and
magical Ranged attack mechanics, Magic Defence armour guidance, Protect from Missiles caveat, Eclipse Moon
armour handling, High Level Alchemy and skip/block guidance, one Waterfiend variant row, three task-page
location rows for Ancient Cavern, Kraken Cove, and Iorwerth Dungeon, Ancient Cavern river/aggression timer
safespotting, Kraken Cove task-only routing, Iorwerth cannon and crystal shard routing, and
`strategies/waterfiends/strategy.json` with requirements, crush melee, earth magic, Ranged defence, all
location methods, and skip guidance.

Wyrms now has task-page and monster-page JSON source coverage from `Slayer_task/Wyrms`, `Wyrm`,
`Shadow Wyrm`, `Wyrmling`, `Lava Strykewyrm`, and `Magma strykewyrm`, including 62 Slayer and
Karuulm heat-protection requirements, Chaeldar/Konar/Nieve/Duradel assignment and Can of Wyrms extension
ranges, Bigger and Badder superior unlocks, current draconic and elemental weakness handling, current
defence profiles for all five variants, standard Wyrm Protect from Magic distance mechanics, flat armour,
Wyrmling cannon/free-supply caveats and no-superior caveat, Lava Strykewyrm 60 Sailing, Protect from
Missiles, water weakness, dragon metal sheet, and Charred Island routing, Magma strykewyrm burrow handling,
three task-page location rows, and `strategies/wyrms/strategy.json` with requirements, Karuulm safespot,
melee dragonbane, elemental magic, Wyrmling, Lava Strykewyrm, and superior methods.

## Behaviour Preservation Notes

Some monster display names appear in more than one task with task-specific profiles in the old runtime
dataset. The migration keeps source IDs globally unique by using task-scoped variant IDs only for these
conflicting duplicates, for example `boss-abyssal-sire` and
`smoke-devils-thermonuclear-smoke-devil`. Strategy documents can reference all matching source variant IDs.

This preserves the existing Boss meta-task behaviour where several boss records intentionally differ from
their regular-task counterparts.

## Verification Commands

Focused Wyrms slice:

```bash
./gradlew test --tests com.danieljglover.allinslayer.data.source.WyrmsSourceCoverageTest
```

Result: `BUILD SUCCESSFUL`.

Source data suite:

```bash
./gradlew test --tests 'com.danieljglover.allinslayer.data.source.*'
```

Result: `BUILD SUCCESSFUL`.

JSON source audit:

```bash
find src/main/data/slayer -type f -name '*.json' -print0 | xargs -0 -n1 jq empty
```

Result: all JSON source files parsed successfully.

Full suite:

```bash
./gradlew cleanTest test
```

Result: `BUILD SUCCESSFUL`; XML reports total 611 tests, 0 failures, 0 errors, 0 skipped.

## Regression Covered

During full-suite verification, the first modular migration failed existing tests because duplicate variant
names had been deduplicated too aggressively. Boss/Abyssal Sire lost its task-specific MAGIC weakness, and
Boss/Thermonuclear smoke devil lost its task-specific defence profile.

The fix adds task-scoped IDs for conflicting duplicate variants and pins it with
`ModularSlayerDataMigrationTest.bossMetaTaskKeepsTaskSpecificDuplicateVariantProfiles`.

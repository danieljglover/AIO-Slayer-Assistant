# Advisor data coverage

This report was updated on 2026-09-08 after the combined catalogue generation
and source JSON validation passed. It covers the reconstructed runtime and all
seven final legacy strategy migrations. It is a structural audit and a set of
explicit semantic reviews; it does not certify that every current Wiki strategy
section has been fully migrated.

## Runtime catalogue

| Measure | Count |
| --- | ---: |
| Slayer masters | 10 |
| Task categories | 118 |
| Combat variants, including superior and incidental forms | 388 |
| Linked locations | 321 |
| Methods and reference sections | 1,329 |
| Selectable preparation methods | 764 |
| Selectable methods with at least 10 nonempty equipment slots | 734 |
| Selectable methods with all 11 nonempty equipment slots | 579 |
| Runtime item definitions, including supplies | 1,273 |
| Named equipment/item evidence records | 807 |
| Unique usable IDs in the equipment evidence source | 1,268 |
| Equipment records with reviewed equip eligibility | 727 of 729 |

A two-handed weapon or a setup without ammunition does not necessarily need all
11 slots. These counts describe resolved priorities, not optimal DPS, measured
XP per hour, measured profit, or a full trip simulation. The goal ranks are
qualitative preferences whose reasons are shown in the panel.

All emitted equipment options have usable item IDs. No task required item and
no selectable method's mandatory item/switch has an empty ID list. Optional
unresolved prose remains visible guidance; it does not become an item suggestion.
The runtime compiler rejects dangling task/master/monster/location/method links.

## Changes with explicit semantic review

Mortimer and his 25 assignment joins are included. Venators are a separate direct
Mortimer assignment and are also linked as variants of the existing Vampyres
assignment. The current Venators prose explicitly distinguishes these cases;
copying every master from that task infobox would incorrectly create six direct
Venator assignments.

Tormented Demons now have four complete starting presets with mandatory second
combat styles, including melee/magic and melee/ranged choices. Demonic gorillas
have two explicit melee/ranged presets, Monkey Madness II and assignment-context
requirements, and mandatory second-style switches. Dagannoth Kings have two
explicit solo presets with melee/ranged/magic weapon preparation and travel
items. Original incomplete tribrid references remain informational. Rex-only,
Prime-only, and Supreme-only references are constrained to their corresponding
boss variants; Rex uses the dedicated magic equipment table.

The Scabarite Guthan method requires all four set pieces and is restricted to
cavern locust riders. Void and Guthan dependencies use AND across required slots
and OR within valid alternatives. Crystal armour requires a compatible crystal
weapon. Named Venator bow, scorching bow, and Bow of Faerdhinen methods retain
those method-defining weapons instead of inheriting an unrelated generic weapon.

Ten previously unlinked ordinary families now preserve their existing detailed
task and location guidance as structured methods: crabs, crocodiles, harpie bug
swarms, infernal mages, molanisks, ogres, otherworldly beings, sea snakes, terror
dogs, and werewolves. Their general equipment comes from current Slayer training
tables. This repairs playable preparation coverage but does not establish that
all current prose for those families has been reviewed line by line.

Task protection is explicit for 24 task categories: spectres, banshees, basilisks,
cave horrors, cave slime, cockatrice, dust devils, fever spiders, fossil-island
wyverns, gargoyles, harpie bug swarms, killerwatts, lizards, metal dragons, mogres,
molanisks, zygomites, rockslugs, sea snakes, skeletal wyverns, smoke devils,
sourhogs, wall beasts, and warped creatures. Seven Karuulm locations require
appropriate boots, with an explicit elite-diary waiver. Protection alternatives
include 60 usable regular, imbued, and cosmetic Slayer helmet IDs. Lit bug lantern
means item 7053, not an unlit lantern. Hammer finishing is not waived by Gargoyle
smasher. Mixed antifire alternatives require either an equipped shield or a
carried consumable accepted by the engine.

Burst/barrage methods have a named spell baseline, Magic level, Ancient spellbook
and quest gates, and explicit rune quantities for 500 casts. Cannon methods
require all four cannon parts and cannonballs. Other Magic methods can still
require the manual confirmation that a compatible spellbook and sufficient runes
or a charged powered staff are prepared. The advisor does not calculate every
non-barrage spell's rune inventory. Supply quantities are planning defaults,
not a prediction that an entire assignment can be completed without banking.

Six-god blessed bodies, chaps, and bracers now expand into concrete item options.
Ardougne cloak and Explorer's ring expand from tier 4 down to tier 1. Expansion
preserves each concrete item's equip gates; it creates no aggregate definition
that overwrites per-ID requirements. Equipment eligibility details and the two
unknown items, Sunspear and Blisterwood stake, are documented in
[item-review.md](item-review.md).

## Current evidence and unresolved source comparisons

The authoring audit visited 1,117 modular/legacy source records and fetched 1,819
Wiki titles including item follow-ups. It captured 214 current equipment tables.
The per-record evidence contains 1,134 unique requested/redirected titles: 693
existing pages and 441 missing page titles. Missing task and strategy pages are
retained as evidence, and explicit current main-page links are recorded in
`advisor/page-links.json` where available. Every record remains labelled
`evidence-fetched-not-reviewed`; retrieval alone is not semantic verification.

At this checkpoint 125 records have only missing direct page evidence: 124
location labels and the aggregate Boss task. Many location labels are local
subareas described on a parent task, monster or dungeon page, and Boss is an
aggregate assignment. Their retained task/location prose and linked methods do
not establish that the direct page names exist or that all their access flags
have been independently reviewed. The exact list is below; no missing-page record
is silently relabelled verified.

Four task-infobox comparisons require explanation. Spria shares Turael's Dogs
assignments according to her own current page, although Dogs omits her field.
Duradel's assignment table says Elves 110-170, while the task infobox says 100-170.
Vannaka's table says Molanisks 40-50, while its task infobox says 39-50. The master
table ranges are retained. Venators' infobox includes masters that assign the
broader Vampyres task, while its prose and Mortimer's page say only Mortimer
assigns Venators directly. The existing direct joins are retained. These are
source conflicts/context differences, not automated evidence of changed joins.

Four legacy task `wikiPageId` values differ from the current task-page IDs:
Cows, Earth warriors, Lizards, and Rats. The generated evidence records the
actual fetched page IDs. Twelve monster records have NPC IDs absent from the
primary fetched page: Black Knight; Ammonite crab; level 90 Waterbirth dagannoth;
Prifddinas guard; the four Calvar'ion/Vet'ion skeleton hellhound forms; monkey;
sea mogre; rock troll; and Trollheim thrower troll. Those differences remain
candidate drift because related pages, versions, quest states and aliases can
change the comparison. They are not certified as correct NPC mappings.

| Source checkpoint | Page ID | Revision ID | Revision timestamp |
| --- | ---: | ---: | --- |
| [Mortimer](https://oldschool.runescape.wiki/w/Mortimer) | 663129 | 15327685 | 2026-09-01T18:26:44Z |
| [Slayer task/Venators](https://oldschool.runescape.wiki/w/Slayer_task/Venators) | 674589 | 15327318 | 2026-09-01T14:15:27Z |
| [Venator/Strategies](https://oldschool.runescape.wiki/w/Venator/Strategies) | 678481 | 15319166 | 2026-08-25T04:05:09Z |
| [Tormented Demon/Strategies](https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies) | 516379 | 15332199 | 2026-09-06T03:07:11Z |
| [Demonic gorilla/Strategies](https://oldschool.runescape.wiki/w/Demonic_gorilla/Strategies) | 73909 | 15329349 | 2026-09-03T08:08:08Z |
| [Dagannoth Kings/Strategies](https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies) | 25401 | 15331444 | 2026-09-05T13:05:10Z |
| [Araxxor/Strategies](https://oldschool.runescape.wiki/w/Araxxor/Strategies) | 529623 | 15322538 | 2026-08-27T23:23:46Z |
| [Lizardman shaman/Strategies](https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies) | 80876 | 15307096 | 2026-08-19T15:41:32Z |
| [Skotizo/Strategies](https://oldschool.runescape.wiki/w/Skotizo/Strategies) | 119426 | 15316231 | 2026-08-22T05:14:13Z |
| [Royal Titans/Strategies](https://oldschool.runescape.wiki/w/Royal_Titans/Strategies) | 566475 | 15329439 | 2026-09-03T13:43:40Z |
| [Kree'arra/Strategies](https://oldschool.runescape.wiki/w/Kree'arra/Strategies) | 41793 | 15299717 | 2026-08-14T03:48:26Z |
| [Eldric the Ice King](https://oldschool.runescape.wiki/w/Eldric_the_Ice_King) | 565718 | 15271709 | 2026-07-22T02:24:22Z |
| [Amoxliatl/Strategies](https://oldschool.runescape.wiki/w/Amoxliatl/Strategies) | 538484 | 15329075 | 2026-09-02T23:14:13Z |

## Final method migrations and remaining gaps

All legacy strategy Markdown files have been replaced by structured JSON. The
final Araxxor, Lizardman shaman, Skotizo, Royal Titans, Wingman Skree, Flight
Kilisa, and Flockleader Geerin migrations contain 53 combat presets, 42 supporting
method sections, and 28 equipment references. Their explicit equipment grids,
method requirements, mandatory switches, travel, solo/group, cannon, safespot,
and combat-style alternatives retain pinned raw Wiki source evidence in JSON.

Royal Titans covers both Branda and Eldric; the Ice giants task links Eldric.
Amoxliatl has three dedicated combat presets and four supporting sections, its
own chamber, and the Heart of Darkness access gate. Lizardman caves and temple
have independent task/cannon restrictions. Explicit assignment-area parents
allow these chambers to match a canonical assignment area without widening the
lock to unrelated locations.

Sixteen cross-family boss referral methods are now informational, directing the
user to the dedicated boss variant and preparation instead of borrowing ordinary
monster equipment. Reviewed authored equipment is preserved by the compiler.
Shaman helmet alternatives require the specific diary and Captain Cleive unlock;
the remaining four Shayzien pieces are mandatory. Armadyl transport crossbows and
grapples occupy inventory slots, while actual combat crossbow switches also
require compatible bolts.

The variant-link correction is recorded in [variant-link-review.md](variant-link-review.md).
It adds existing cross-family alternatives plus Scurrius solo/group preparation
and a source-verified Wilderness skeleton variant. Incidental encounters remain
visible but are excluded from repeatable recommendations.

The following 19 variants have no selectable preparation: eight superior
encounters, four reanimated forms, five boss summons, and two incidental or
quest forms.

- `dreadborn-araxyte`
- `monstrous-basilisk`
- `kolodion-final-form`
- `reanimated-bloodveld`
- `reanimated-dagannoth`
- `reanimated-elf`
- `marble-gargoyle-superior`
- `dire-gryphon-superior`
- `greater-skeleton-hellhound-calvar-ion`
- `greater-skeleton-hellhound-vet-ion`
- `skeleton-hellhound-calvar-ion`
- `skeleton-hellhound-tarn-s-lair`
- `skeleton-hellhound-vet-ion`
- `colossal-hydra`
- `king-kurask-superior`
- `scurrius-giant-rat`
- `reanimated-troll`
- `spiked-turoth-superior`
- `blood-starved-venator`

The compiler reports 876 distinct unresolved authored labels. That number mixes
prose, formatting/case variants, generic inventory categories, and actual
equipment families; it is not that many missing concrete item pages. There are
47 unresolved equipment-row labels. Counts below are resolution occurrences
across references and inherited tables, not unique strategies. These rows are
omitted from item priorities and retained in coverage/guidance.

| Unresolved equipment row | Resolution occurrences |
| --- | ---: |
| Barrows equipment | 49 |
| Barrows helm | 24 |
| Barrows legs | 22 |
| Barrows body | 20 |
| two-handed weapons | 10 |
| God book | 7 |
| Barrows chestplate | 6 |
| (empty source row) | 4 |
| Proselyte armour | 3 |
| Darts | 3 |
| Any decent melee weapon | 3 |
| Dragonhide shield | 3 |
| Cheap prayer equipment | 2 |
| Arrows | 2 |
| Best available melee weapon | 2 |
| Bandos armour | 2 |
| Barrows armour | 2 |
| Ranged armour | 2 |
| Heavy melee armour | 2 |
| Inquisitor's hauberk with crush weapon | 2 |

All 24 skillcape families now have concrete trimmed and untrimmed options with
their individual skill requirements. Blessed boots, coifs, and shields cover all
six gods; Rada's blessing has all four tiers; Broodoo shields include all colours
and usable charge forms. These supplement the already resolved blessed bodies,
chaps, bracers, Ardougne cloaks, and Explorer's rings. Family definitions and
supporting evidence live in `advisor/equipment-families.json`.

Barrows categories do not identify one combat style or an ordered interchangeable
set. A generic God book also has no universal priority across melee, ranged, and
magic. These and other broad categories remain explicit normalization gaps.

## Reproduction and verification

The editable source is `src/main/data/slayer`. The authoring-only API script
`scripts/audit-advisor-wiki.py --evidence-only` refreshes provenance and tables
without replacing reviewed item metadata. Add `--refresh` to request current
revisions instead of reusing the external `/tmp/aio-slayer-wiki-cache` cache.
`scripts/normalize-advisor-overrides.py` rebuilds explicit family, protection and
dependency overrides from reviewed source item records. No runtime HTTP is used.

`find src/main/data/slayer -type f -name '*.json' -print0 | xargs -0 -n1 jq empty`
and `./gradlew generateSlayerData` passed for all 1,144 source JSON files.
Generation compiled the updated compiler and validated graph references.
The final build and native-client results are recorded in
[verification.md](verification.md).
No test files or test dependencies were added; behavior is checked manually in
RuneLite. Generated runtime and coverage JSON are verification output, never
editable source input.

## Source records with missing direct pages

- `tasks/boss.json`
- `locations/ammonite-crabs-fossil-island.json`
- `locations/ancient-cavern-waterfiends.json`
- `locations/aquanite-cavern-sailing.json`
- `locations/araxxor-lair.json`
- `locations/araxyte-lair-morytania.json`
- `locations/asgarnian-ice-dungeon-task-only.json`
- `locations/axe-hut.json`
- `locations/bandit-camp-wilderness-black-knights.json`
- `locations/bat-spawns.json`
- `locations/bears-area.json`
- `locations/bird-spawns.json`
- `locations/brimhaven-dungeon-baby-red-dragons.json`
- `locations/brimhaven-dungeon-metal-dragons-task-only.json`
- `locations/brimhaven-dungeon-red-dragons.json`
- `locations/canifis-ghoul-area.json`
- `locations/catacombs-of-kourend-brutal-red-dragons.json`
- `locations/chaos-druid-wilderness.json`
- `locations/charred-dungeon-lava-strykewyrms.json`
- `locations/charred-dungeon-red-dragons.json`
- `locations/chasm-of-fire-bottom.json`
- `locations/chasm-of-fire-middle.json`
- `locations/colossal-wyrm-remains.json`
- `locations/corsair-cove-dungeon-baby-red-dragons.json`
- `locations/corsair-cove-dungeon-red-dragons.json`
- `locations/cow-field.json`
- `locations/dog-spawns.json`
- `locations/earth-warriors-wilderness.json`
- `locations/eastern-gryphon-dungeon.json`
- `locations/edgeville-dungeon-wilderness.json`
- `locations/ent-wilderness.json`
- `locations/forthos-dungeon-baby-red-dragons.json`
- `locations/forthos-dungeon-red-dragons.json`
- `locations/fremennik-isles-ice-trolls.json`
- `locations/fremennik-slayer-dungeon-cave-crawlers.json`
- `locations/fremennik-slayer-dungeon-cockatrice.json`
- `locations/fremennik-slayer-dungeon-pyrefiends.json`
- `locations/fremennik-slayer-dungeon-rockslugs.json`
- `locations/ghost-spawns.json`
- `locations/goblin-spawns.json`
- `locations/god-wars-dungeon-ancient-prison.json`
- `locations/graveyard-of-shadows-green-dragons.json`
- `locations/graveyard-of-shadows-skeletons.json`
- `locations/green-dragons-wilderness.json`
- `locations/grimstone-dungeon-task-only.json`
- `locations/heroes-guild-basement.json`
- `locations/hobgoblin-area.json`
- `locations/ice-warriors-area.json`
- `locations/iorwerth-dungeon-waterfiends.json`
- `locations/karuulm-slayer-dungeon-greater-demons.json`
- `locations/karuulm-slayer-dungeon-wyrms.json`
- `locations/keldagrim-entrance-trolls.json`
- `locations/kharidian-desert-lizards.json`
- `locations/kraken-cove-waterfiends.json`
- `locations/legends-guild-dungeon.json`
- `locations/lighthouse-basement.json`
- `locations/lletya-upstairs.json`
- `locations/lunar-isle-north-suqah.json`
- `locations/lunar-isle-south-east-suqah.json`
- `locations/lunar-isle-south-west-suqah.json`
- `locations/mammoth-wilderness.json`
- `locations/monkey-spawns.json`
- `locations/mor-ul-rek-inner-tzhaar-ket.json`
- `locations/mor-ul-rek-tzhaar-city.json`
- `locations/mount-quidamortem-north-east-trolls.json`
- `locations/mount-quidamortem-south-west-trolls.json`
- `locations/mourner-tunnels-iorwerth-dungeon.json`
- `locations/neypotzli-wyrmlings.json`
- `locations/poison-waste-dungeon-terrorbird-lower.json`
- `locations/poison-waste-dungeon-terrorbird-main.json`
- `locations/poison-waste-dungeon-tortoise-lower.json`
- `locations/poison-waste-dungeon-tortoise-main.json`
- `locations/prifddinas-east-gate.json`
- `locations/prifddinas-market-place.json`
- `locations/prifddinas-north-gate.json`
- `locations/prifddinas-south-gate.json`
- `locations/prifddinas-west-gate.json`
- `locations/rat-spawns.json`
- `locations/river-elid.json`
- `locations/rock-crabs-rellekka.json`
- `locations/rogues-castle.json`
- `locations/sand-crabs-crabclaw-isle.json`
- `locations/scorpions-area.json`
- `locations/skeletons-area.json`
- `locations/slayer-tower-basement-nechryael.json`
- `locations/slayer-tower-basement.json`
- `locations/slayer-tower-top-floor-nechryael.json`
- `locations/slayer-tower-top-floor.json`
- `locations/sophanem-dungeon-cavern.json`
- `locations/sophanem-dungeon-maze.json`
- `locations/sourhog-cave.json`
- `locations/spiders-area.json`
- `locations/spiders-wilderness.json`
- `locations/spiritual-mages-ancient-prison.json`
- `locations/spiritual-mages-god-wars-dungeon.json`
- `locations/spiritual-mages-wilderness-god-wars-dungeon.json`
- `locations/spiritual-rangers-ancient-prison.json`
- `locations/spiritual-rangers-god-wars-dungeon.json`
- `locations/spiritual-rangers-wilderness-god-wars-dungeon.json`
- `locations/spiritual-warriors-ancient-prison.json`
- `locations/spiritual-warriors-god-wars-dungeon.json`
- `locations/spiritual-warriors-wilderness-god-wars-dungeon.json`
- `locations/stronghold-of-security-catablepon.json`
- `locations/stronghold-of-security-flesh-crawlers.json`
- `locations/stronghold-of-security-minotaurs.json`
- `locations/tai-bwo-wannai.json`
- `locations/taverley-dungeon-upper.json`
- `locations/tower-of-voices-ground-floor.json`
- `locations/tower-of-voices-upper-floor.json`
- `locations/troll-stronghold-outside-trolls.json`
- `locations/trollheim-death-plateau.json`
- `locations/various-boss-locations.json`
- `locations/various-wilderness-boss-locations.json`
- `locations/vetion-hellhound-spawns.json`
- `locations/west-of-yanille.json`
- `locations/western-gryphon-dungeon.json`
- `locations/wilderness-ankou-area.json`
- `locations/wilderness-resource-area-hellhounds.json`
- `locations/wilderness-slayer-cave-ice-giants.json`
- `locations/wyvern-cave-fossil-island-task-only.json`
- `locations/wyvern-cave-fossil-island.json`
- `locations/zanaris-cosmic-altar-zygomites.json`
- `locations/zanaris-furnace-zygomites.json`
- `locations/zanaris-otherworldly-beings.json`
- `locations/zombies-area.json`

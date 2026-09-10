# Trip travel and Wilderness supplies research

For the subsequent house/bank return and escape-ranking additions, see
[return teleport sources](return-teleport-sources.md).

Current raw MediaWiki source was retrieved through the API during authoring. The reviewed rules are authored in `src/main/data/slayer/advisor/trip-preparation.json`; the compiler and trip planner consume these rules; behavior is verified manually. Source retrieval is evidence acquisition, not a claim that every related game state is observable.

## Implemented coverage

- 42 destination-specific item/contact options across all 10 catalogue masters.
- 107 explicit item variants, including charged/uncharged metadata; only usable charged IDs occur in travel options.
- Seven same-dose/same-food ordinary-to-blighted replacements: manta ray, anglerfish, cooked karambwan and each of four super-restore doses.
- Closed/open looting bags: 11941 and 22586. A looting bag note is not counted as a ready bag.
- Separate spell and casting-resource source files contain 79 spell recipes; Astral Contact consumes one Astral, one Cosmic and two Air runes at 67 Magic on Lunar after Lunar Diplomacy.

## Load-bearing travel rules

Most portable teleports work through level 20 Wilderness inclusive. The reviewed level-30 exceptions include charged/eternal glory, combat bracelet, skills necklace, charged/imbued ring of wealth, regular/eternal Slayer ring, royal/grand seed pod and charged pharaoh's sceptre. Greater Wilderness levels require leaving the restricted depth before those items work. The planner must describe this as a conditional return/escape route, not immediate escape readiness.

Tele Block prevents ordinary portable teleports and Wilderness levers/obelisks. Its usual duration is five minutes, reduced to 2.5 minutes if Protect from Magic was active when it landed. Live Tele Block status and PvP timing are not established by this static bundle.

Artio, Spindel and Calvar'ion have a three-tick teleport delay without Hard Wilderness Diary. A corresponding boss assignment can grant access without granting that diary benefit. Revenant Caves has a separate two-tick delay during NPC combat, restartable by an incoming NPC hit, removed by the hard diary. The Royal seed pod page loosely groups both as three ticks; the location-specific Revenant Caves description is more precise and was used.

All item charges are per usable item ID. Zero means unlimited, never uncharged. Charged pharaoh's sceptre IDs prove at least one use but not the total loaded charges; source conservatively reserves one use. Rada's blessing 3 has three daily Mount Karuulm teleports; one use is accepted only after explicit remaining-charge confirmation. Same-ID items may serve return and escape roles only if the runtime conserves the charges and consumed quantities.

Heroes' Quest and Legends' Quest jewellery recharge conditions are not gates for using already-owned charged jewellery. Grand seed pod Squash is the indoor-capable fast action; Launch requires outdoors and is not the emergency action. Kharyrll tablets still require Desert Treasure I, but no casting level or Ancient spellbook to break them.

Conditional ring-of-life/Defence-cape/Escape-crystal effects are not generic manual escape substitutes. Chronicle and minigame teleports are not Wilderness escapes. Custom house portals, boat facilities and seasonal/PvP-world exceptions were not invented from ownership.

## Master return options

| Catalogue master | Physical route options | Remote assignment caveat |
| --- | --- | --- |
| Turael | Games necklace - Burthorpe; Combat bracelet - Warriors Guild; Falador Teleport | Astral Contact casting requirements |
| Spria | Amulet of glory - Draynor Village; Necklace of passage - Wizards Tower; Lumbridge Teleport | A Porcine of Interest |
| Mazchna | Kharyrll teleport (tablet); Slayer ring - Slayer Tower; Ectophial; Kharyrll Teleport | Astral Contact casting requirements |
| Vannaka | Amulet of glory - Edgeville; Varrock Teleport | Astral Contact casting requirements |
| Chaeldar | Lumbridge tablet - walk to Zanaris; Lumbridge Teleport - Zanaris route | Lost City |
| Nieve | Slayer ring - Stronghold Slayer Cave; Royal seed pod; Grand seed pod; Necklace of passage - Outpost | Astral Contact casting requirements |
| Duradel | Karamja gloves 4; Karamja gloves 3 | Astral Contact casting requirements |
| Krystilia | Amulet of glory - Edgeville; Varrock Teleport | Krystilia has assigned the first Wilderness Slayer task |
| Konar quo Maten | Rada's blessing 4; Rada's blessing 3; Skills necklace - Farming Guild; Battlefront Teleport | Konar Astral Contact unlocked by using Talk-to in person |
| Mortimer | Slayer ring - Wyrmscraig Cavern | Fallen From Grace progressed to unlock Mortimer assignments |

Turael/Aya, Mazchna/Achtryn, Nieve/Steve and Duradel/Kuradal share their stable catalogue slots. Astral Contact (formerly NPC Contact, renamed 29 July 2026) supports all ten slots. Konar requires her Talk-to dialogue in person; right-click Assignment alone does not unlock her. Krystilia requires the first Wilderness task assignment. Dream Mentor is required for random contacts, not these master contacts.

Mortimer's ring teleport requires completed Fallen From Grace, although his task access begins during the quest. The Slayer ring Master option cannot assign tasks; the physical route uses its teleport. Chaeldar's Lumbridge-tablet route packs one dramen/lunar staff alternative and requires Lost City; the staff must be wieldable for entry. Lunar staff requires 65 Magic and 40 Defence. No unverified diary-only shed-entry shortcut is assumed.

## Blighted supplies and looting bag

Blighted food and super restores are usable in Wilderness and specifically listed safe enclaves/minigame areas; the trip planner for an ordinary world should substitute them only for a Wilderness combat destination. Crossing Wilderness to the non-Wilderness King Black Dragon lair does not make those supplies usable in the lair. Manta rays heal 22, karambwans heal 18. Anglerfish healing scales with Hitpoints; recent combat damage in PvP areas restricts overhealing, so the trip plan should not promise excess-HP combat healing.

Blighted ancient ice sacks replace ice-spell runes but retain Magic and Ancient spellbook requirements. Blighted teleport spell sacks supply Tele Block/Teleport to Target, not an escape teleport. The supposed page Blighted teleport surge sack is missing; no item or capability is authored from it. Spell/sack allocation is in the separate casting rules.

The looting bag consumes one real inventory slot and stores up to 28 loot stacks. Tradeable items may be deposited in Wilderness, including Ferox on ordinary worlds. Its contents cannot be used as trip supplies and are normally recovered at a bank. It is not 28 additional usable inventory slots.

On Wilderness PvP death the bag is always lost; the killer receives its contents except food/potions. On Wilderness PvM death the bag is destroyed and all contents go to the gravestone. Destroying outside Wilderness on an ordinary world, including Ferox, deletes all contents; destroying inside Wilderness publicly drops contents and destroys unnoted cooked food, potions and vials of water. Runtime risk must account for observed bag contents separately from physical carried inventory.

## Compiler and metadata boundary

TripPreparationCompiler merges trip-preparation.json, spells.json and casting-resources.json after route compilation. It rejects duplicate JSON keys, noninteger/invalid quantities and IDs, unknown master/teleport/spell/rune references, incomplete charge maps, malformed helper alternatives, invalid spellbooks and unpinned Wiki evidence. Existing item equipment definitions are never overwritten. New verified inventory resources receive known empty-slot definitions; new travel jewellery with unreviewed equip facts remains equip-requirement unknown. Lunar staff has a separate sourced equipment requirement definition.

## Pinned source revisions

| Source | Page ID | Revision | Revision timestamp |
| --- | ---: | ---: | --- |
| [Amulet of eternal glory](https://oldschool.runescape.wiki/w/Amulet_of_eternal_glory?oldid=15244819) | 74899 | 15244819 | 2026-06-30T18:22:52Z |
| [Amulet of glory](https://oldschool.runescape.wiki/w/Amulet_of_glory?oldid=15320178) | 10272 | 15320178 | 2026-08-25T22:41:01Z |
| [Amulet of glory (t)](https://oldschool.runescape.wiki/w/Amulet_of_glory_(t)?oldid=15324765) | 10273 | 15324765 | 2026-08-29T20:46:13Z |
| [Anglerfish](https://oldschool.runescape.wiki/w/Anglerfish?oldid=15317631) | 68424 | 15317631 | 2026-08-24T00:05:46Z |
| [Ardougne Teleport](https://oldschool.runescape.wiki/w/Ardougne_Teleport?oldid=14918399) | 18348 | 14918399 | 2025-06-12T04:58:22Z |
| [Artio](https://oldschool.runescape.wiki/w/Artio?oldid=15309934) | 374838 | 15309934 | 2026-08-19T21:24:33Z |
| [Astral Contact](https://oldschool.runescape.wiki/w/Astral_Contact?oldid=15320080) | 17845 | 15320080 | 2026-08-25T21:06:25Z |
| [Aya](https://oldschool.runescape.wiki/w/Aya?oldid=15279905) | 516011 | 15279905 | 2026-07-29T12:40:31Z |
| [Battlefront Teleport](https://oldschool.runescape.wiki/w/Battlefront_Teleport?oldid=15252748) | 204502 | 15252748 | 2026-07-04T18:33:55Z |
| [Blighted anglerfish](https://oldschool.runescape.wiki/w/Blighted_anglerfish?oldid=15317632) | 261719 | 15317632 | 2026-08-24T00:07:42Z |
| [Blighted karambwan](https://oldschool.runescape.wiki/w/Blighted_karambwan?oldid=15331033) | 261720 | 15331033 | 2026-09-04T21:26:21Z |
| [Blighted manta ray](https://oldschool.runescape.wiki/w/Blighted_manta_ray?oldid=15189707) | 261721 | 15189707 | 2026-04-22T04:32:32Z |
| [Blighted super restore](https://oldschool.runescape.wiki/w/Blighted_super_restore?oldid=15233476) | 261717 | 15233476 | 2026-06-14T15:03:44Z |
| [Blighted teleport spell sack](https://oldschool.runescape.wiki/w/Blighted_teleport_spell_sack?oldid=15189712) | 261727 | 15189712 | 2026-04-22T04:32:34Z |
| [Calvar'ion](https://oldschool.runescape.wiki/w/Calvar'ion?oldid=15309944) | 374839 | 15309944 | 2026-08-19T21:25:26Z |
| [Chaeldar](https://oldschool.runescape.wiki/w/Chaeldar?oldid=15319052) | 11510 | 15319052 | 2026-08-25T03:23:21Z |
| [Combat bracelet](https://oldschool.runescape.wiki/w/Combat_bracelet?oldid=15320281) | 10524 | 15320281 | 2026-08-25T23:13:02Z |
| [Cooked karambwan](https://oldschool.runescape.wiki/w/Cooked_karambwan?oldid=15331032) | 24277 | 15331032 | 2026-09-04T21:26:05Z |
| [Dramen staff](https://oldschool.runescape.wiki/w/Dramen_staff?oldid=15182983) | 10581 | 15182983 | 2026-04-22T02:50:04Z |
| [Duradel](https://oldschool.runescape.wiki/w/Duradel?oldid=15326700) | 11511 | 15326700 | 2026-08-31T23:16:40Z |
| [Ectophial](https://oldschool.runescape.wiki/w/Ectophial?oldid=15195604) | 12680 | 15195604 | 2026-04-24T01:51:36Z |
| [Falador Teleport](https://oldschool.runescape.wiki/w/Falador_Teleport?oldid=14918402) | 16740 | 14918402 | 2025-06-12T04:59:03Z |
| [Games necklace](https://oldschool.runescape.wiki/w/Games_necklace?oldid=15183028) | 10636 | 15183028 | 2026-04-22T02:50:26Z |
| [Grand seed pod](https://oldschool.runescape.wiki/w/Grand_seed_pod?oldid=15195675) | 39678 | 15195675 | 2026-04-24T06:33:33Z |
| [Karamja gloves 3](https://oldschool.runescape.wiki/w/Karamja_gloves_3?oldid=15266075) | 15672 | 15266075 | 2026-07-17T14:26:22Z |
| [Karamja gloves 4](https://oldschool.runescape.wiki/w/Karamja_gloves_4?oldid=15266078) | 16445 | 15266078 | 2026-07-17T14:29:11Z |
| [Kharyrll Teleport](https://oldschool.runescape.wiki/w/Kharyrll_Teleport?oldid=15331348) | 24986 | 15331348 | 2026-09-05T08:05:02Z |
| [Kharyrll teleport (tablet)](https://oldschool.runescape.wiki/w/Kharyrll_teleport_(tablet)?oldid=15186392) | 41567 | 15186392 | 2026-04-22T04:07:35Z |
| [Konar quo Maten](https://oldschool.runescape.wiki/w/Konar_quo_Maten?oldid=15291798) | 199880 | 15291798 | 2026-08-10T07:26:15Z |
| [Krystilia](https://oldschool.runescape.wiki/w/Krystilia?oldid=15318319) | 25302 | 15318319 | 2026-08-24T23:31:56Z |
| [Looting bag](https://oldschool.runescape.wiki/w/Looting_bag?oldid=15318005) | 31284 | 15318005 | 2026-08-24T11:28:34Z |
| [Looting bag note](https://oldschool.runescape.wiki/w/Looting_bag_note?oldid=15189714) | 261761 | 15189714 | 2026-04-22T04:32:35Z |
| [Lumbridge Teleport](https://oldschool.runescape.wiki/w/Lumbridge_Teleport?oldid=14918404) | 16739 | 14918404 | 2025-06-12T04:59:25Z |
| [Lumbridge teleport (tablet)](https://oldschool.runescape.wiki/w/Lumbridge_teleport_(tablet)?oldid=15185081) | 28072 | 15185081 | 2026-04-22T03:09:27Z |
| [Lunar staff](https://oldschool.runescape.wiki/w/Lunar_staff?oldid=15183957) | 17895 | 15183957 | 2026-04-22T02:58:48Z |
| [Manta ray](https://oldschool.runescape.wiki/w/Manta_ray?oldid=15184402) | 23257 | 15184402 | 2026-04-22T03:02:54Z |
| [Mazchna](https://oldschool.runescape.wiki/w/Mazchna?oldid=15279911) | 10150 | 15279911 | 2026-07-29T12:42:12Z |
| [Mortimer](https://oldschool.runescape.wiki/w/Mortimer?oldid=15327685) | 663129 | 15327685 | 2026-09-01T18:26:44Z |
| [Necklace of passage](https://oldschool.runescape.wiki/w/Necklace_of_passage?oldid=15309251) | 92766 | 15309251 | 2026-08-19T19:56:18Z |
| [Nieve](https://oldschool.runescape.wiki/w/Nieve?oldid=15297222) | 26611 | 15297222 | 2026-08-13T14:33:58Z |
| [Pharaoh's sceptre](https://oldschool.runescape.wiki/w/Pharaoh's_sceptre?oldid=15322386) | 12985 | 15322386 | 2026-08-27T21:33:34Z |
| [Rada's blessing 3](https://oldschool.runescape.wiki/w/Rada's_blessing_3?oldid=15240310) | 199912 | 15240310 | 2026-06-26T20:36:59Z |
| [Rada's blessing 4](https://oldschool.runescape.wiki/w/Rada's_blessing_4?oldid=15242366) | 199914 | 15242366 | 2026-06-29T21:19:12Z |
| [Revenant Caves](https://oldschool.runescape.wiki/w/Revenant_Caves?oldid=15274504) | 106797 | 15274504 | 2026-07-25T13:02:38Z |
| [Ring of dueling](https://oldschool.runescape.wiki/w/Ring_of_dueling?oldid=15322346) | 11003 | 15322346 | 2026-08-27T21:18:50Z |
| [Ring of wealth](https://oldschool.runescape.wiki/w/Ring_of_wealth?oldid=15322347) | 11005 | 15322347 | 2026-08-27T21:19:22Z |
| [Ring of wealth (i)](https://oldschool.runescape.wiki/w/Ring_of_wealth_(i)?oldid=15322517) | 41594 | 15322517 | 2026-08-27T22:20:00Z |
| [Royal seed pod](https://oldschool.runescape.wiki/w/Royal_seed_pod?oldid=15323014) | 72458 | 15323014 | 2026-08-28T13:14:02Z |
| [Skills necklace](https://oldschool.runescape.wiki/w/Skills_necklace?oldid=15235017) | 11078 | 15235017 | 2026-06-18T13:22:33Z |
| [Slayer ring](https://oldschool.runescape.wiki/w/Slayer_ring?oldid=15286008) | 27936 | 15286008 | 2026-08-03T03:49:08Z |
| [Slayer ring (eternal)](https://oldschool.runescape.wiki/w/Slayer_ring_(eternal)?oldid=15322601) | 98439 | 15322601 | 2026-08-28T04:44:06Z |
| [Spindel](https://oldschool.runescape.wiki/w/Spindel?oldid=15309918) | 374849 | 15309918 | 2026-08-19T21:20:00Z |
| [Spria](https://oldschool.runescape.wiki/w/Spria?oldid=15327896) | 273906 | 15327896 | 2026-09-02T00:40:34Z |
| [Super restore](https://oldschool.runescape.wiki/w/Super_restore?oldid=15183989) | 18131 | 15183989 | 2026-04-22T02:59:05Z |
| [Tele Block](https://oldschool.runescape.wiki/w/Tele_Block?oldid=15301572) | 15712 | 15301572 | 2026-08-14T21:21:37Z |
| [Teleport crystal](https://oldschool.runescape.wiki/w/Teleport_crystal?oldid=15261004) | 26720 | 15261004 | 2026-07-11T12:18:11Z |
| [Turael](https://oldschool.runescape.wiki/w/Turael?oldid=15327867) | 11452 | 15327867 | 2026-09-01T23:22:30Z |
| [Vannaka](https://oldschool.runescape.wiki/w/Vannaka?oldid=15332812) | 11509 | 15332812 | 2026-09-06T21:45:59Z |
| [Varrock Teleport](https://oldschool.runescape.wiki/w/Varrock_Teleport?oldid=14918406) | 17435 | 14918406 | 2025-06-12T04:59:52Z |
| [Wilderness](https://oldschool.runescape.wiki/w/Wilderness?oldid=15317990) | 12459 | 15317990 | 2026-08-24T10:38:34Z |
| [Wyrmscraig Cavern](https://oldschool.runescape.wiki/w/Wyrmscraig_Cavern?oldid=15288292) | 674686 | 15288292 | 2026-08-05T22:26:53Z |
| [Zanaris](https://oldschool.runescape.wiki/w/Zanaris?oldid=15332213) | 9435 | 15332213 | 2026-09-06T04:25:54Z |

Moonclan Teleport also provides a level-20 Lunar escape option. Its spell level, runes, Lunar Diplomacy gate and pinned revision are recorded in spells.json; the general Wilderness teleport limit is pinned alongside that route.

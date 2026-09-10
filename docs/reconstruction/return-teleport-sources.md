# Escape and return teleport sources

Reviewed on 2026-09-09 against current OSRS Wiki MediaWiki API source with
`action=query&prop=revisions|info&rvprop=ids|timestamp|content&rvslots=main`,
`format=json`, and `formatversion=2`. The revision identifiers below pin the
review even when a page's latest edit predates this review.

## Authored selection rules

`trip-preparation.json` has 58 teleport/contact entries, 28 ordered `bankRoutes`,
and two `houseRoutes`: `house-tablet`, then `house-spell`. `spells.json` adds
Paddewwa Teleport with 54 Magic, the Ancient spellbook, Desert Treasure I, two
law runes, one air rune and one fire rune. Existing route and item IDs remain
stable. Every teleport has an explicit `escapePriority`; contacts use 100 but
are excluded from escape selection.

Escape priority is an authored preference after Wilderness coverage and
eligibility, not a measured latency or combat guarantee:

- Royal seed pod: 0, unlimited one-click Commune, usable through level 30
  Wilderness after Monkey Madness II.
- Filled ectophial, charged teleport crystals and tablets: 10. The crystal's
  left-click destination is configurable; the route describes Lletya and does
  not assume the plugin has observed that setting.
- Ordinary teleport spells: 20, with their spellbook, Magic and rune gates.
- Glory destinations: 30, retaining every charged normal and trimmed form and
  eternal glory. Inventory Rub and equipped destination selection need a menu.
- Other jewellery and direct right-click item options: 50. Grand seed pod uses
  Squash, which supports indoor use and drains five Farming levels; default
  Launch is slower and requires outdoors. It is not classified as a default
  one-click escape.
- Slayer rings and Pharaoh's sceptre: 100 because their inventory destination
  interfaces are slower to navigate. A configured previous-destination action
  or a user's menu swaps are not observed.

This order does not remove Tele Block, special-attack restrictions, or the
existing Wilderness cave/boss teleport delay rules. Royal seed pod arrival is
not a safe area on PvP worlds. Bank preference order considers convenience;
it does not estimate live path length, animations, prices or movement time.

## Bank, house and master semantics

`bankNearby` means the reviewed destination offers a straightforward usable
bank with its stated requirements. It may involve a short walk or stairs;
it does not mean arrival directly beside a bank, a deposit box, or a PvP-world
safe zone. Grand Tree seed-pod banking needs one ladder; the Stronghold Slayer
Cave bank needs one set of stairs. Lumbridge routes use the castle top-floor
bank, without assuming Recipe for Disaster cellar access. Generic Farming
Guild access is not marked as a bank because the existing master route works
below the guild's bank access level.

Castle Wars and Ferox dueling routes lead the bank list, followed by Edgeville
and Grand Exchange jewellery, charged Lletya crystals and seed pods. City
teleports and house routes remain fallbacks. Standard Varrock destinations do
not imply the Medium Varrock Diary; separate `varrock-grand-exchange-tablet`
and `varrock-grand-exchange-spell` entries require that diary.

Generic `moonclan-spell` remains an escape without asserting usable banking.
`moonclan-bank-spell` requires `Dream Mentor OR Elite Fremennik Diary`; after
Dream Mentor the player must use Birds-Eye Jack. The alternative
`moonclan-bank-seal-spell` packs seal of passage 9083, which works while carried
and need not replace equipped neckwear. Spellbook and Lunar Diplomacy gates
still come from Moonclan Teleport's spell metadata.

Generic house routes require `Own a player-owned house` and have
`bankNearby: false`. The tablet is exact item 8013; an owned tablet does not
require 40 Magic or the Standard spellbook. Those are casting requirements for
the spell, not tablet-use requirements. A house alone establishes neither a
bank nor a Slayer master route.

- `house-edgeville-tablet` and `house-edgeville-spell` additionally require
  `POH has an Edgeville teleport (mounted glory or ornate jewellery box)`.
  Teleport inside, use the confirmed furniture, then walk to Edgeville bank,
  Krystilia, or the dungeon entrance for Vannaka.
- `house-bank-tablet` and `house-bank-spell` additionally require
  `POH has a jewellery box`. Every tier supports Castle Wars; the route does
  not assume portable jewellery, an ornate tier, or the level to build it.

House furniture conditions require explicit account confirmation. Completing
Daddy's Home can establish house ownership in runtime account capture; a
player who bought a house independently can confirm ownership manually. House
ownership must not be inferred from Construction level or portable tablets.

Krystilia and Vannaka now have glory, confirmed POH Edgeville, Paddewwa
spell/tablet, combat bracelet Monastery, Grand Exchange jewellery, and Varrock
spell/tablet approaches. Paddewwa arrives inside Edgeville Dungeon: exit by the
ladder for Krystilia or continue through the dungeon for Vannaka. The combat
bracelet route stays outside the Monastery, so no Prayer/guild gate is added.
Turael also accepts the Falador tablet approach, and Spria accepts the Lumbridge
tablet approach. Existing Astral Contact routes remain remote contact, with their original
first-contact requirements; they do not provide travel or an escape.

New item records are house tablet 8013, Paddewwa tablet 12781, Varrock tablet
8007, Falador tablet 8009, and seal of passage 9083. Tablet variants each consume
one item; the seal is a carried helper. Portable charge families retain exact
existing stock IDs, including no uncharged or legacy-convertible variants.
Paddewwa tablets require Desert Treasure I but no casting level or spellbook.

## Evidence

Redirects were resolved to `Amulet of Glory (mounted)`, `Paddewwa Teleport` and
`Paddewwa teleport (tablet)`; redirect revision metadata was not substituted
for substantive page evidence.

| Page | Page ID | Revision ID | Revision timestamp (UTC) |
| --- | ---: | ---: | --- |
| [Amulet of Glory (mounted)](https://oldschool.runescape.wiki/w/Amulet_of_Glory_(mounted)) | 53165 | 14913382 | 2025-06-01T19:23:02Z |
| [Amulet of glory](https://oldschool.runescape.wiki/w/Amulet_of_glory) | 10272 | 15320178 | 2026-08-25T22:41:01Z |
| [Basic jewellery box](https://oldschool.runescape.wiki/w/Basic_jewellery_box) | 79147 | 15262556 | 2026-07-13T18:55:42Z |
| [Canifis](https://oldschool.runescape.wiki/w/Canifis) | 6916 | 15276281 | 2026-07-27T00:20:46Z |
| [Combat bracelet](https://oldschool.runescape.wiki/w/Combat_bracelet) | 10524 | 15320281 | 2026-08-25T23:13:02Z |
| [East Ardougne](https://oldschool.runescape.wiki/w/East_Ardougne) | 18831 | 15332699 | 2026-09-06T19:32:00Z |
| [Edgeville](https://oldschool.runescape.wiki/w/Edgeville) | 12681 | 15276296 | 2026-07-27T00:31:38Z |
| [Edgeville Dungeon](https://oldschool.runescape.wiki/w/Edgeville_Dungeon) | 15477 | 15285142 | 2026-08-01T15:03:19Z |
| [Falador](https://oldschool.runescape.wiki/w/Falador) | 11657 | 15332935 | 2026-09-07T00:26:18Z |
| [Falador teleport (tablet)](https://oldschool.runescape.wiki/w/Falador_teleport_(tablet)) | 28066 | 15185079 | 2026-04-22T03:09:26Z |
| [Fastest bank teleports](https://oldschool.runescape.wiki/w/Fastest_bank_teleports) | 394064 | 15309284 | 2026-08-19T20:00:40Z |
| [Ferox Enclave](https://oldschool.runescape.wiki/w/Ferox_Enclave) | 269560 | 15327854 | 2026-09-01T23:12:25Z |
| [Grand Tree](https://oldschool.runescape.wiki/w/Grand_Tree) | 17297 | 15114413 | 2026-01-28T04:51:50Z |
| [Grand seed pod](https://oldschool.runescape.wiki/w/Grand_seed_pod) | 39678 | 15195675 | 2026-04-24T06:33:33Z |
| [Krystilia](https://oldschool.runescape.wiki/w/Krystilia) | 25302 | 15318319 | 2026-08-24T23:31:56Z |
| [Lletya](https://oldschool.runescape.wiki/w/Lletya) | 9393 | 15322229 | 2026-08-27T18:21:11Z |
| [Lumbridge](https://oldschool.runescape.wiki/w/Lumbridge) | 9837 | 15275326 | 2026-07-25T20:45:54Z |
| [Lunar Isle bank](https://oldschool.runescape.wiki/w/Lunar_Isle_bank) | 396119 | 15285646 | 2026-08-02T10:48:21Z |
| [Moonclan Teleport](https://oldschool.runescape.wiki/w/Moonclan_Teleport) | 20045 | 14863798 | 2025-03-17T22:14:58Z |
| [Ornate jewellery box](https://oldschool.runescape.wiki/w/Ornate_jewellery_box) | 79149 | 15262552 | 2026-07-13T18:53:29Z |
| [Paddewwa Teleport](https://oldschool.runescape.wiki/w/Paddewwa_Teleport) | 14144 | 15331334 | 2026-09-05T07:55:52Z |
| [Paddewwa teleport (tablet)](https://oldschool.runescape.wiki/w/Paddewwa_teleport_(tablet)) | 41572 | 15186395 | 2026-04-22T04:07:37Z |
| [Player-owned house](https://oldschool.runescape.wiki/w/Player-owned_house) | 6913 | 15272470 | 2026-07-22T18:12:24Z |
| [Ring of dueling](https://oldschool.runescape.wiki/w/Ring_of_dueling) | 11003 | 15322346 | 2026-08-27T21:18:50Z |
| [Royal seed pod](https://oldschool.runescape.wiki/w/Royal_seed_pod) | 72458 | 15323014 | 2026-08-28T13:14:02Z |
| [Seal of passage](https://oldschool.runescape.wiki/w/Seal_of_passage) | 22624 | 15325966 | 2026-08-31T01:55:49Z |
| [Slayer ring](https://oldschool.runescape.wiki/w/Slayer_ring) | 27936 | 15286008 | 2026-08-03T03:49:08Z |
| [Teleport crystal](https://oldschool.runescape.wiki/w/Teleport_crystal) | 26720 | 15261004 | 2026-07-11T12:18:11Z |
| [Teleport to House](https://oldschool.runescape.wiki/w/Teleport_to_House) | 17634 | 14918405 | 2025-06-12T04:59:41Z |
| [Teleport to house (tablet)](https://oldschool.runescape.wiki/w/Teleport_to_house_(tablet)) | 28090 | 15185086 | 2026-04-22T03:09:29Z |
| [Vannaka](https://oldschool.runescape.wiki/w/Vannaka) | 11509 | 15332812 | 2026-09-06T21:45:59Z |
| [Varrock](https://oldschool.runescape.wiki/w/Varrock) | 11655 | 15314401 | 2026-08-20T20:54:34Z |
| [Varrock Teleport](https://oldschool.runescape.wiki/w/Varrock_Teleport) | 17435 | 14918406 | 2025-06-12T04:59:52Z |
| [Varrock teleport (tablet)](https://oldschool.runescape.wiki/w/Varrock_teleport_(tablet)) | 20972 | 15184222 | 2026-04-22T03:01:13Z |
| [Wilderness](https://oldschool.runescape.wiki/w/Wilderness) | 12459 | 15317990 | 2026-08-24T10:38:34Z |
| [Daddy's Home](https://oldschool.runescape.wiki/w/Daddy's_Home) | 272522 | 15292258 | 2026-08-10T23:05:13Z |
| [Items Kept on Death](https://oldschool.runescape.wiki/w/Items_Kept_on_Death) | 36390 | 15322198 | 2026-08-27T18:11:51Z |

The royal seed pod page gives Commune/Destroy inventory options and a two-coin
replacement purchase, but no explicit item-specific PvP death rule. The
`Items Kept on Death` section "Items that are never kept" states that items
without a ground state (cannot be dropped) disappear on unsafe death regardless
of protection and never appear to other players. Applying that general rule
to royal seed pod is an inference from its Destroy-only removal option;
the pod is not named in that section or its always-kept exceptions. The runtime death rule uses this documented inference: the pod does not consume
a protected slot and its replacement loss is two coins, not killer loot.

## Verification

Validate edited JSON with `jq empty`, then run `./gradlew generateSlayerData`
and `./gradlew build` for source references and compilation. The owner verifies
behavior manually; no automated test files or test dependencies are used.

## House observations and Shortest Path settings

House ownership is positively confirmed from completed Daddy's Home or a
recognised `POH_HOUSE_LOCATION` (2187) value 1-9. Unknown values retain manual
confirmation. The reviewed [Shortest Path teleport table](https://github.com/Skretzo/shortest-path/blob/6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a/src/main/resources/transports/teleportation_spells.tsv)
provides these portal encodings. They do not describe furniture.

`ShortestPathHouseSettings` reads saved `shortestpath` configuration only when
`usePoh` is enabled: `pohJewelleryBoxTier` unlocks the matching bank/Edgeville
routes, and `usePohMountedItems` permits its configured mounted-glory route.
Missing values do not inherit interface defaults. RuneLite may persist plugin
defaults, so a saved value is a configuration assumption, not proof of a
manual edit or an observed object. The planner and optional house checks label
the source. Configuration changes refresh planning, and no Shortest Path
settings or manual account confirmations are written by this import. The
settings belong to the current RuneLite profile; verify them when switching
accounts that share that profile.

Portal nexus contents and scene furniture scanning are not imported by this
change. The [Shortest Path configuration](https://github.com/Skretzo/shortest-path/blob/6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a/src/main/java/shortestpath/ShortestPathConfig.java)
and `PathfinderConfig.checkJewelleryBoxTier` are the compatibility reference.

## Packing shared exits

When the Wilderness escape also serves the chosen bank or master destination,
one portable item serves both roles. If the selected onward master route is a
different teleport spell and the escape reaches a usable bank, collect that
spell's runes after banking instead of reserving extra inventory stacks.
Offensive spell resources, remote assignment contact, and an explicit House
return preference remain separate requirements.

Final verification: all Slayer source JSON passed `jq empty`; the clean source
compiler/build and subsequent `./gradlew build compileDevJava` passed. Manual
login/bank inspection showed the royal seed pod in a complete 28-slot Wilderness
plan with a complete risk estimate. The subsequent duplicate-return-rune fix
and imported-house configuration need the owner's next manual session.

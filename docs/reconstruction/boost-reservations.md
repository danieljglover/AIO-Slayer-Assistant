# Boost reservations

The native plugin configuration has collapsed Melee boosts, Ranged boosts and
Magic boosts sections. Each has a Boost slots count and an ordered Preferred
boosts list. All counts default to 0, which preserves automatic supply packing.
A positive count opts the primary combat style into an explicit reservation.

A count measures inventory slots occupied by bottles, not doses. The planner
chooses the first preferred family with an owned usable form and fills the
reservation with the fullest available bottles of that family first. For
example, three reserved slots can hold two four-dose bottles and one two-dose
bottle. A shortage in the chosen family does not mix in a later preference.
Missing quantities leave slots free and produce preparation blockers, so food
cannot silently consume the requested space. A selected reusable heart is
packed once even when the configured count is larger.

Omit a dose suffix to accept all owned positive-dose forms. An explicit suffix,
such as `Super combat potion(4)`, restricts that preference to four-dose bottles.
The selected boosts are ordinary planned inventory items for bank filtering,
setup export, departure checks and Wilderness loss estimates. Reservations do
not assume boosted skill levels, unlock spells, or confirm a heart's cooldown.

Preferences accept comma, semicolon or newline separators. Parsing trims names,
removes empty entries and case-insensitive duplicates, and retains at most 28
entries in their original order. Counts are clamped to 0-28. Preference names
identify supported families; usable concrete item forms come from observed
owned item state. Unrecognised names are ignored with a planning note; a list
with no recognised combat boosts blocks the setup and retains the empty space.
Any supported combat boost can be explicitly preferred in another style's
section for a hybrid strategy. Required source supplies retain priority. The
override replaces optional boost supplies belonging to the primary style or an
explicitly preferred family. It never removes required source items.

## Bundled metadata

`src/main/data/slayer/advisor/trip-preparation.json` owns `boostFamilies`, a map
with exactly `MELEE`, `RANGED` and `MAGIC` keys. Values are nonempty lists of
case-insensitive-unique display family names without dose suffixes or
parentheses. `reusableBoosts` contains Imbued heart and Saturated heart; every
reusable name must appear in at least one style. `TripPreparationCompiler`
validates these rules before bundling `TripPreparationData`.

Style membership defines which optional source boosts a reservation can
replace. It does not model potion effects, confer equipment eligibility, or
assert that every member improves offensive damage. The MELEE group includes
Defence-only potions from the melee combat supply family. Bastion variants
belong to RANGED, and battlemage variants belong to MAGIC, while both also boost
Defence. Ancient and forgotten brews retain their Magic-family identity despite
their other effects. New item definitions or equip requirements are unnecessary
for this name mapping.

The planner reads `TripPreparationData.getBoostFamilies()` and
`getReusableBoosts()` from the generated bundled catalogue. Authoring evidence
is fetched outside RuneLite; this feature adds no runtime network requests or
game actions.

## Wiki evidence

All 26 family pages below were fetched and reviewed from the current raw OSRS
Wiki MediaWiki API on 2026-09-11. The API response timestamp was
`2026-09-11T00:58:11Z`. The request used `action=query`, `prop=revisions|info`,
`rvprop=ids|timestamp|content`, `rvslots=main` and `formatversion=2`; none of the
requested pages were missing or redirects. Each linked revision supplies the
item-family names and the associated skill boost. The potion infoboxes list
separate dose forms, supporting bottle-based inventory counting.

The Imbued heart source explicitly describes unlimited activations with a
per-player cooldown regardless of the number owned. The Saturated heart source
identifies the upgraded reusable heart, its cooldown, and the restriction on
using both heart types together. These facts support packing one chosen heart.
The top-level preparation `evidence` array preserves each source URL, page ID,
revision ID and revision timestamp.

| Style | Family and source revision | Page ID | Revision ID | Revision timestamp |
| --- | --- | ---: | ---: | --- |
| MELEE | [Combat potion](https://oldschool.runescape.wiki/w/Combat_potion?oldid=15183999) | 18171 | 15183999 | 2026-04-22T02:59:11Z |
| MELEE | [Super combat potion](https://oldschool.runescape.wiki/w/Super_combat_potion?oldid=15281186) | 40966 | 15281186 | 2026-07-29T18:56:38Z |
| MELEE | [Divine super combat potion](https://oldschool.runescape.wiki/w/Divine_super_combat_potion?oldid=15331555) | 226118 | 15331555 | 2026-09-05T16:34:50Z |
| MELEE | [Attack potion](https://oldschool.runescape.wiki/w/Attack_potion?oldid=15247702) | 10213 | 15247702 | 2026-07-02T01:53:48Z |
| MELEE | [Strength potion](https://oldschool.runescape.wiki/w/Strength_potion?oldid=15183998) | 18169 | 15183998 | 2026-04-22T02:59:11Z |
| MELEE | [Defence potion](https://oldschool.runescape.wiki/w/Defence_potion?oldid=15183997) | 18164 | 15183997 | 2026-04-22T02:59:10Z |
| MELEE | [Super attack](https://oldschool.runescape.wiki/w/Super_attack?oldid=15184104) | 19385 | 15184104 | 2026-04-22T03:00:09Z |
| MELEE | [Super strength](https://oldschool.runescape.wiki/w/Super_strength?oldid=15184084) | 19233 | 15184084 | 2026-04-22T02:59:59Z |
| MELEE | [Super defence](https://oldschool.runescape.wiki/w/Super_defence?oldid=15183946) | 17840 | 15183946 | 2026-04-22T02:58:43Z |
| MELEE | [Divine super attack potion](https://oldschool.runescape.wiki/w/Divine_super_attack_potion?oldid=15331554) | 226127 | 15331554 | 2026-09-05T16:34:35Z |
| MELEE | [Divine super strength potion](https://oldschool.runescape.wiki/w/Divine_super_strength_potion?oldid=15331557) | 226128 | 15331557 | 2026-09-05T16:35:14Z |
| MELEE | [Divine super defence potion](https://oldschool.runescape.wiki/w/Divine_super_defence_potion?oldid=15331556) | 226129 | 15331556 | 2026-09-05T16:35:02Z |
| MELEE | [Zamorak brew](https://oldschool.runescape.wiki/w/Zamorak_brew?oldid=15184356) | 22750 | 15184356 | 2026-04-22T03:02:26Z |
| RANGED | [Ranging potion](https://oldschool.runescape.wiki/w/Ranging_potion?oldid=15293943) | 20252 | 15293943 | 2026-08-12T18:10:48Z |
| RANGED | [Divine ranging potion](https://oldschool.runescape.wiki/w/Divine_ranging_potion?oldid=15331553) | 226130 | 15331553 | 2026-09-05T16:34:24Z |
| RANGED | [Bastion potion](https://oldschool.runescape.wiki/w/Bastion_potion?oldid=15293966) | 117490 | 15293966 | 2026-08-12T18:17:18Z |
| RANGED | [Divine bastion potion](https://oldschool.runescape.wiki/w/Divine_bastion_potion?oldid=15331551) | 262469 | 15331551 | 2026-09-05T16:33:49Z |
| MAGIC | [Magic potion](https://oldschool.runescape.wiki/w/Magic_potion?oldid=15247721) | 16399 | 15247721 | 2026-07-02T02:02:44Z |
| MAGIC | [Divine magic potion](https://oldschool.runescape.wiki/w/Divine_magic_potion?oldid=15331552) | 226131 | 15331552 | 2026-09-05T16:33:58Z |
| MAGIC | [Magic essence](https://oldschool.runescape.wiki/w/Magic_essence?oldid=15184293) | 22121 | 15184293 | 2026-04-22T03:01:50Z |
| MAGIC | [Battlemage potion](https://oldschool.runescape.wiki/w/Battlemage_potion?oldid=15188871) | 117491 | 15188871 | 2026-04-22T04:24:56Z |
| MAGIC | [Divine battlemage potion](https://oldschool.runescape.wiki/w/Divine_battlemage_potion?oldid=15331550) | 262470 | 15331550 | 2026-09-05T16:33:38Z |
| MAGIC | [Ancient brew](https://oldschool.runescape.wiki/w/Ancient_brew?oldid=15190634) | 340212 | 15190634 | 2026-04-22T04:38:57Z |
| MAGIC | [Forgotten brew](https://oldschool.runescape.wiki/w/Forgotten_brew?oldid=15191164) | 373070 | 15191164 | 2026-04-22T04:42:30Z |
| MAGIC | [Imbued heart](https://oldschool.runescape.wiki/w/Imbued_heart?oldid=15293066) | 80101 | 15293066 | 2026-08-11T20:11:59Z |
| MAGIC | [Saturated heart](https://oldschool.runescape.wiki/w/Saturated_heart?oldid=15304376) | 373066 | 15304376 | 2026-08-17T16:21:19Z |

Source JSON needs `jq empty`, `./gradlew generateSlayerData` and
`./gradlew build` before the migration is considered verified. Runtime behavior
is checked manually with `./gradlew run`; the project has no automated tests.

## Manual checks

- With all three counts at 0, compare a setup with its previous automatic packing.
- Set melee to 2 with `Super combat potion, Combat potion`; use an observed bank
  with both full and partial bottles. Confirm two bottles, preferring full doses,
  and no additional optional melee boost from the source strategy.
- Change the chosen strategy to ranged or magic and confirm only its section is
  applied. Changing a setting should refresh the recommendation immediately.
- For magic, select an owned heart with a count above 1. Confirm exactly one
  heart and that remaining space can hold other supplies.
- Choose an unowned family, an explicit unavailable dose, or an empty list with
  a positive count. Confirm marked empty slots and a missing-boost check.
- Request more bottles than observed stock or remaining capacity. Confirm the
  shortage is shown, required supplies remain, and the grid never exceeds 28.
- Confirm actual selected doses appear in the bank layout and exported setup;
  departure checks require them in the backpack. Review their Wilderness loss
  alongside the other planned items.

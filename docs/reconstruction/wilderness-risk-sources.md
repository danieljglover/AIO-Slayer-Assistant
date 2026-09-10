# Wilderness death-risk source review

Reviewed 2026-09-08 from current OSRS Wiki MediaWiki revision source. Cached
search results and older item prose can still describe the pre-June 2026 Trouver
system; they are not used to override the rework or current death-rule tables.

## Bundled inputs

- `src/main/data/slayer/advisor/death-rules.json`: 216 reviewed rule groups,
  covering 485 exact item IDs. Rules have individual page/revision evidence.
- `src/main/data/slayer/advisor/wilderness-risk.json`: 57 source-location
  profiles, including 18 explicitly unknown depth ranges and KBD travel risk.
- `equipment-items.json`: 18 additional usable ornamented/locked ornamented
  forms across the eight Void pieces and Fighter torso. The current individual
  Wiki pages establish that these are cosmetic variants with unchanged equip
  requirements. Their exact IDs retain different death behavior.
- `overrides.json`: the eight Void pieces' equipped-dependency groups include
  those variants. The existing `normalize-advisor-overrides.py` derives these
  groups from the same item metadata, so regenerating it preserves the change.

The compiler rejects missing rule kinds/evidence, duplicate/non-positive item
IDs, invalid quantities and valuations, unknown location references and missing
Wilderness profiles. Missing item death behavior is not automatically safe.
Ordinary tradeable items use the captured tradeable-item rule; unsupported
untradeable forms remain unknown. Broken/mangled items are not usable equipment
alternatives and are not inferred from their intact counterpart.

## Reviewed rules

Unskulled protection has three normal slots; active Protect Item adds a fourth.
Skulled protection has zero slots, or one with the prayer. High-risk-world and
Ultimate Ironman restrictions require separate context. Protection valuation is
not always an item's replacement cost: notably, Avernic defenders protect at
600,000 gp, including their discontinued locked form.

The special untradeable threshold is above level 20 (21+), not above level 30.
After the 17 June 2026 rework, current lockable items retain their lock after a
PvP death. Unprotected locked combat equipment breaks at 20 or below and mangles
above 20; mangled repair costs 500,000 gp. Without a lock, those high-tier items
are permanently lost above 20 if they fall outside protected slots. An item
occupying a protection slot keeps its original form, apart from independently
specified losses such as ether or ring-of-wealth imbue.

Fire capes, all defenders, Ava/Masori assemblers, imbued god capes, halos,
Barbarian Assault hats and penance skirt, decorative combat armour, and the
Barronite/Void knight maces now break at all Wilderness depths. Their
pre-rework locked forms are discontinued and follow the same repair-only
behavior. The current `(broken)` table and June update take precedence over
outdated death paragraphs on some decorative-item pages.

Void and defender ornament kits are additional lost tradeable components.
Fighter torso's Bounty Hunter ornament kit instead goes to the victim's
gravestone. Ancient sceptre without elemental upgrade is a separate exception:
it converts to an ancient staff and destroys its icon at any depth when
unprotected. Its locked version has the new broken/mangled behavior. The four
upgraded sceptres' low-Wilderness repairs cost 200,000 gp plus the current
Ancient staff guide price. The base locked sceptre's low-Wilderness fee is not
established by the reviewed table, so `repairCostKnown` is false for that form;
its above-20 mangled fee remains known.

Slayer helmets convert to a black mask; all six recipe components are included
in replacement value. Imbue points/scrolls are refunded. Salve variants remain
kept. Rune and divine rune pouches cannot use ordinary protection slots:
unlocked pouches are lost in PvP at any Wilderness level; locked pouches are
kept. Current pouch item records have no broken/mangled form, so no invented
500,000 gp repair is charged. Stored runes are lost separately. Divine pouch
loss also destroys its untradeable thread.

Looting bags and their contents are never protected. Bolt-pouch contents and
unsupported storage contents remain unknown quantities. The July 8 update
keeps storage containers such as seed boxes and fish barrels, while PvP
contents have separate loss rules. Protection of a container must not be used
to assume its contents are protected.

Ether weapons lose their 1,000 activation ether and all remaining ether even
when the weapon is protected. The known activation cost is included; remaining
charges are unknown. Ring of wealth (i) likewise loses its imbue even protected:
re-imbuing costs the current scroll price plus 50,000 gp, as the current item
recipe explicitly requires both. Other charged equipment records price its
uncharged replacement and retain unknown charge value, rather than treating
unobserved charges as zero. Barrows/Moon equipment is lost as broken loot;
replacement estimates use an intact item to avoid valuing usable owned armour
as the cheaper broken item the killer receives.

## Locations and uncertainty

Calvar'ion's Skeletal Tomb is level 21; Vet'ion's Rest is level 35. Hunter's End
has a level-20 entrance and level-21 chamber. The shared boss entry fee is at
most 50,000 gp, reduced by qualifying kills and subject to whether the player
has left Wilderness since the visit. The source stores the conservative
maximum, not an assertion of the account's current unpaid fee. Revenant Caves
have a separate 100,000 gp fee and special PvM-as-PvP death rules.

KBD's chamber is outside Wilderness but the reviewed access crosses level
42-43 Wilderness. Its profile marks Wilderness travel explicitly. Generic or
family-specific spawn areas with no confidently pinned range use -1/-1 and
must be assessed above level 20. Wilderness Slayer Cave uses the widest
reviewed table range, 17-33: the article lead says 32 but an individual chamber
reaches 33. A whole-area profile must cover that chamber.

Current quiver pages state that stored ammunition drops and that a locked
quiver can survive while losing ammunition and unblessed sunfire charges.
They do not establish whether protecting the quiver itself protects its extra
ammunition or whether that ammunition competes for normal slots. Its observable
ammunition is therefore exposed as an explicit protection uncertainty. Blessed
quivers retain their permanent sunfire blessing when retained. This review does
not invent a precise unknown ammunition-protection rule.

The maps cover reviewed item forms, not every OSRS item or every possible
charged/ornamented form. Unknown prices, unobserved charges, unidentified item
death rules and unsupported account restrictions cannot prove a setup within a
budget. Alternative travel routes can enter deeper Wilderness than the pinned
encounter. No game actions, input, runtime Wiki HTTP requests or automated tests
are introduced.

## Evidence

The following are the principal rule/location revisions. Every mapped item
also carries its item-page URL, page ID, revision ID and timestamp directly in
`death-rules.json`; each location carries its evidence in `wilderness-risk.json`.
The source URLs point to current pages; revision IDs identify the exact reviewed
source even after later edits.

| Page | Page ID | Revision ID | Revision timestamp (UTC) |
| --- | ---: | ---: | --- |
| [Items Kept on Death](https://oldschool.runescape.wiki/w/Items_Kept_on_Death) | 36390 | 15322198 | 2026-08-27T18:11:51Z |
| [(l)](https://oldschool.runescape.wiki/w/(l)) | 233950 | 15314626 | 2026-08-20T23:33:44Z |
| [(broken)](https://oldschool.runescape.wiki/w/(broken)) | 223706 | 15314620 | 2026-08-20T23:31:42Z |
| [(mangled)](https://oldschool.runescape.wiki/w/(mangled)) | 663713 | 15314627 | 2026-08-20T23:34:04Z |
| [Trouver parchment](https://oldschool.runescape.wiki/w/Trouver_parchment) | 232192 | 15257360 | 2026-07-08T15:28:17Z |
| [Update:Bank Tags, Trouver System Rework & More!](https://oldschool.runescape.wiki/w/Update%3ABank_Tags%2C_Trouver_System_Rework_%26_More%21) | 663679 | 15237567 | 2026-06-22T18:11:04Z |
| [Update:The Blood Moon Rises Tweaks & Fixes](https://oldschool.runescape.wiki/w/Update%3AThe_Blood_Moon_Rises_Tweaks_%26_Fixes) | 669050 | 15256817 | 2026-07-08T10:15:28Z |
| [Avernic defender](https://oldschool.runescape.wiki/w/Avernic_defender) | 112765 | 15298485 | 2026-08-14T00:36:42Z |
| [Ancient sceptre](https://oldschool.runescape.wiki/w/Ancient_sceptre) | 372991 | 15275937 | 2026-07-26T17:31:52Z |
| [Blood ancient sceptre](https://oldschool.runescape.wiki/w/Blood_ancient_sceptre) | 381921 | 15292069 | 2026-08-10T16:41:52Z |
| [Dizana's quiver](https://oldschool.runescape.wiki/w/Dizana's_quiver) | 416942 | 15329079 | 2026-09-02T23:16:50Z |
| [Blessed Dizana's quiver](https://oldschool.runescape.wiki/w/Blessed_Dizana's_quiver) | 459145 | 15314615 | 2026-08-20T23:29:07Z |
| [Rune pouch](https://oldschool.runescape.wiki/w/Rune_pouch) | 36526 | 15316472 | 2026-08-22T08:27:20Z |
| [Divine rune pouch](https://oldschool.runescape.wiki/w/Divine_rune_pouch) | 363149 | 15315583 | 2026-08-21T08:43:52Z |
| [Looting bag](https://oldschool.runescape.wiki/w/Looting_bag) | 31284 | 15318005 | 2026-08-24T11:28:34Z |
| [Ring of wealth (i)](https://oldschool.runescape.wiki/w/Ring_of_wealth_(i)) | 41594 | 15322517 | 2026-08-27T22:20:00Z |
| [Slayer helmet](https://oldschool.runescape.wiki/w/Slayer_helmet) | 27486 | 15322450 | 2026-08-27T21:55:37Z |
| [Salve amulet(ei)](https://oldschool.runescape.wiki/w/Salve_amulet(ei)) | 34094 | 15330245 | 2026-09-03T19:21:00Z |
| [Emberlight](https://oldschool.runescape.wiki/w/Emberlight) | 446743 | 15320249 | 2026-08-25T22:58:18Z |
| [Amulet of blood fury](https://oldschool.runescape.wiki/w/Amulet_of_blood_fury) | 256451 | 15318464 | 2026-08-25T01:27:05Z |
| [Barrows equipment](https://oldschool.runescape.wiki/w/Barrows_equipment) | 11682 | 15326889 | 2026-09-01T02:08:51Z |
| [Skeletal Tomb](https://oldschool.runescape.wiki/w/Skeletal_Tomb) | 374983 | 15292449 | 2026-08-11T01:48:47Z |
| [Vet'ion's Rest](https://oldschool.runescape.wiki/w/Vet'ion's_Rest) | 374960 | 15165442 | 2026-04-04T19:18:04Z |
| [Hunter's End](https://oldschool.runescape.wiki/w/Hunter's_End) | 374965 | 15292444 | 2026-08-11T01:46:39Z |
| [Callisto's Den](https://oldschool.runescape.wiki/w/Callisto's_Den) | 374963 | 15165439 | 2026-04-04T19:15:47Z |
| [Silk Chasm](https://oldschool.runescape.wiki/w/Silk_Chasm) | 374962 | 15165438 | 2026-04-04T19:15:17Z |
| [Web Chasm](https://oldschool.runescape.wiki/w/Web_Chasm) | 374984 | 15292447 | 2026-08-11T01:48:29Z |
| [Revenant Caves](https://oldschool.runescape.wiki/w/Revenant_Caves) | 106797 | 15274504 | 2026-07-25T13:02:38Z |
| [King Black Dragon Lair](https://oldschool.runescape.wiki/w/King_Black_Dragon_Lair) | 19532 | 15167741 | 2026-04-07T04:44:35Z |
| [Graveyard of Shadows](https://oldschool.runescape.wiki/w/Graveyard_of_Shadows) | 28980 | 15233557 | 2026-06-14T17:43:48Z |
| [Wilderness Slayer Cave](https://oldschool.runescape.wiki/w/Wilderness_Slayer_Cave) | 277451 | 15252501 | 2026-07-04T15:36:34Z |

Authoring reads used the API parameters `action=query`,
`prop=revisions|info`, `rvprop=ids|timestamp|content`, `rvslots=main`,
`redirects=1`, `format=json`, `formatversion=2`, in batches of explicit titles.
The raw responses were saved locally under `/tmp/aio-*-evidence.json`; these
are research artifacts and are not runtime dependencies. Nonexistent tentative
ornament-kit titles were resolved to actual item pages before mapping any IDs.

Validation: `jq empty` passed for all four changed JSON files.
`./gradlew generateSlayerData` compiled and validated the new rule and area maps.
The final integration build and owner-led gameplay checks are recorded with the
runtime implementation, separately from this source review.

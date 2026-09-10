# Equipment eligibility review

Reviewed on 2026-09-08 against current OSRS Wiki MediaWiki API revision bodies.
The authoring cache is outside the repository at `/tmp/aio-slayer-wiki-cache`;
there is no runtime network dependency. Each equipment record retains its source
URL, page ID, revision ID, and revision timestamp in `evidence`.

## Result

| Measure | Count |
| --- | ---: |
| Named item records, including case and strategy aliases | 807 |
| Records with an equipment slot | 729 |
| Equipment records with reviewed eligibility | 727 |
| Equipment records with incomplete eligibility evidence | 2 |
| Inventory-only records; equipment eligibility not applicable | 78 |
| Unique usable item IDs across all records | 1,268 |
| Usable Slayer helmet protection variants | 60 |

`requirementsKnown` records a semantic authoring decision. It is not set merely
because the API returned a page or the extractor found a number. Item entries
with matching IDs were checked for consistent levels, requirements, and known
status; no conflicts remain.

## Interpretation

The review retained direct wear, wield, and equip requirements and corrected
numbers taken from crafting, enchanting, smithing, charging, monster drops,
comparison gear, and combat bonuses. Examples include amulets of fury and torture,
tormented bracelets, spirit shields, ordinary Masori armour, the keris partisan,
the warped sceptre, monk robes, and the witchwood icon.

The item-family pages supply requirements absent from individual item leads:
god capes require 60 Magic, the lunar ring requires 65 Magic and 40 Defence, and
hasta wielding requires the relevant Barbarian Training progress. Skillcapes
retain the named skill's 99 requirement; their trimmed appearance adds no
independent equip gate. The max cape includes all 24 current skills.

For ordinary jewellery, cosmetic clothing, and untradeable rewards, a named
reviewed cohort has no additional equip gate stated in the item-page lead or
combat section. These records explicitly label that authoring conclusion in
`requirementEvidence`; it is an inference from the reviewed source, not a direct
wiki claim that all requirements are absent. Acquisition-only restrictions are
not added as independent equip gates. For example, owning a fire cape, a diary
reward, or a completed god book already establishes the corresponding reward's
acquisition. Enchanting an amulet is not a requirement for wearing a purchased
one. The only two equipment records whose full equip eligibility could not be
established remain unknown, as listed below.

Ammunition needs separate care: the `Ammunition` page explicitly says its level
tables describe compatible weapons. Those indirect firing levels are not copied
into ammunition equip requirements. The actual bow, crossbow, or salamander has
its own equipment eligibility. Direct item-page equip/wield requirements are
retained for broad arrows and bolts, amethyst broad bolts, mithril bolts, runite
bolts, dragon bolts, dragonstone bolts (e), and ruby bolts (e). Brutal arrows retain
no crafting-level restriction; rune and adamant brutal arrows require a comp
ogre bow, whereas mithril brutal arrows also support the ordinary ogre bow.

Actual completed-quest gates use `Quest: <quest name>` for RuneLite quest-state
matching. Partial quest progress, diary completion, and advanced training have
explicit strings so the runtime can recognise captured progress or request the
specific missing confirmation. Rune gloves preserve the actual Daero-training
OR Culinaromancer unlock instead of treating their derived minimum Defence level
as the complete gate.

The regular and imbued Slayer helmet entries now include their respective
cosmetic variants. `Slayer helmet protection variants` combines both groups for
environmental protection. The wiki explicitly describes these recolours as
cosmetic and retaining the original stats and bonuses. Uncharged black masks
remain usable for their passive task bonus; uncharged combat jewellery retains
its combat stats. Broken equipment and empty powered weapons remain excluded.

## Source checkpoints

These supporting revisions supplement each item's own recorded evidence.

| Page | Page ID | Revision ID | Revision timestamp |
| --- | ---: | ---: | --- |
| [Ammunition](https://oldschool.runescape.wiki/w/Ammunition) | 17661 | 15294956 | 2026-08-12T22:34:58Z |
| [God capes](https://oldschool.runescape.wiki/w/God_capes) | 10662 | 15296608 | 2026-08-13T07:13:12Z |
| [Lunar equipment](https://oldschool.runescape.wiki/w/Lunar_equipment) | 16586 | 14805285 | 2024-11-22T00:38:39Z |
| [Hasta](https://oldschool.runescape.wiki/w/Hasta) | 9461 | 15264928 | 2026-07-16T04:31:28Z |
| [Slayer helmet](https://oldschool.runescape.wiki/w/Slayer_helmet) | 27486 | 15322450 | 2026-08-27T21:55:37Z |
| [Slayer helmet (i)](https://oldschool.runescape.wiki/w/Slayer_helmet_(i)) | 27974 | 15322452 | 2026-08-27T21:55:53Z |
| [Comp ogre bow](https://oldschool.runescape.wiki/w/Comp_ogre_bow) | 10525 | 15182944 | 2026-04-22T02:49:42Z |

## Exact unresolved equipment

| Item | Item ID | Page ID | Revision ID | Revision timestamp | Remaining gap |
| --- | ---: | ---: | ---: | --- | --- |
| [Blisterwood stake](https://oldschool.runescape.wiki/w/Blisterwood_stake) | 33716 | 666511 | 15320434 | 2026-08-26T04:37:48Z | The current item page describes acquisition and combat behaviour but does not establish the complete equip-level requirements. |
| [Sunspear](https://oldschool.runescape.wiki/w/Sunspear) | 33722 | 641594 | 15298173 | 2026-08-13T23:53:52Z | The current item page describes acquisition and combat behaviour but does not establish the complete equip-level requirements. |

The current `Attack`, `Ranged`, and `The Blood Moon Rises` raw sources were also
checked without resolving these two gaps. They remain `requirementsKnown: false`;
a directly equipped item or an explicit account confirmation can supply runtime
eligibility evidence.

The separate `unresolvedNames` field still preserves the extractor's 135 raw
strategy descriptions and generic aliases, such as "Food", "Barrows armour",
and spell or style instructions. It is not a list of 135 missing concrete item
pages. Contextual alias normalization belongs to the method compiler, and this
review does not silently turn generic descriptions into one arbitrary item.

## Verification

`jq empty src/main/data/slayer/advisor/equipment-items.json` passed after the
edits. All shared usable IDs agree on eligibility metadata. The integration task
runs `generateSlayerData` and `build` for the complete combined migration. No
automated test files or test dependencies were added. RuneLite behaviour remains
subject to the owner's manual testing.

## Concrete family additions

A follow-up review on 2026-09-08 checked all six gods' blessed dragonhide bodies,
chaps, and bracers, plus all four tiers of Ardougne cloak and Explorer's ring.
Ten missing concrete records were added. Each blessed body requires 40 Defence
and 70 Ranged; each chaps or bracers piece requires 70 Ranged. None requires
Dragon Slayer I, which the body pages distinguish from the green dragonhide
body. Guthix equipment does not grant God Wars Dungeon faction protection.

The four Ardougne cloaks and Explorer's rings retain separate item IDs and
individual source evidence. Their diary requirements govern acquisition, and
are not copied into additional equip restrictions. Runtime family aliases are
maintained separately in the override source; no aggregate item definition was
created for these families.

| Added concrete item | Item ID | Page ID | Revision ID | Revision timestamp |
| --- | ---: | ---: | ---: | --- |
| [Ancient bracers](https://oldschool.runescape.wiki/w/Ancient_bracers) | 12490 | 37877 | 15324778 | 2026-08-29T20:48:01Z |
| [Ancient chaps](https://oldschool.runescape.wiki/w/Ancient_chaps) | 12494 | 37880 | 15324768 | 2026-08-29T20:47:51Z |
| [Ancient d'hide body](https://oldschool.runescape.wiki/w/Ancient_d'hide_body) | 12492 | 37881 | 15324767 | 2026-08-29T20:47:44Z |
| [Bandos chaps](https://oldschool.runescape.wiki/w/Bandos_chaps) | 12502 | 37888 | 15324776 | 2026-08-29T20:50:23Z |
| [Explorer's ring 2](https://oldschool.runescape.wiki/w/Explorer's_ring_2) | 13126 | 45852 | 15186729 | 2026-04-22T04:09:49Z |
| [Explorer's ring 3](https://oldschool.runescape.wiki/w/Explorer's_ring_3) | 13127 | 45895 | 15284006 | 2026-07-31T07:16:07Z |
| [Guthix bracers](https://oldschool.runescape.wiki/w/Guthix_bracers) | 10376 | 27003 | 15324781 | 2026-08-29T20:51:06Z |
| [Guthix chaps](https://oldschool.runescape.wiki/w/Guthix_chaps) | 10380 | 27001 | 15324780 | 2026-08-29T20:50:59Z |
| [Guthix d'hide body](https://oldschool.runescape.wiki/w/Guthix_d'hide_body) | 10378 | 26999 | 15324779 | 2026-08-29T20:50:51Z |
| [Saradomin bracers](https://oldschool.runescape.wiki/w/Saradomin_bracers) | 10384 | 26990 | 15324786 | 2026-08-29T20:51:54Z |

JSON parsing and shared-ID eligibility consistency checks passed after these
additions. The two unresolved vampyre weapons listed above are unchanged.

## Final family review

The final additions cover all 24 skillcapes (trimmed and untrimmed separately),
six-god blessed boots/coifs/shields, all Rada's blessing tiers, and all three
Broodoo shield colours and their usable charge forms. Trimmed capes retain their
prayer bonus priority, while each cape keeps its own skill requirement. The
per-item revisions remain in `equipment-items.json`; shared family page evidence
and ordering decisions are in `equipment-families.json`. Ambiguous Barrows and
God book categories remain unresolved rather than receiving an arbitrary style.

The final item source has 807 named records and 1,268 unique usable IDs. All
shared IDs agree on level, additional requirement, and reviewed-status metadata.
The two unresolved vampyre weapons above are still explicitly unknown.

The Scurrius migration adds 19 reviewed records, including bone staff, lower-level
ranged equipment, complete ordinary team-cape IDs, remaining vestment cloaks,
and source-listed supplies. Their individual API evidence is retained in the
item source. Shared item IDs still agree on equip eligibility.

## Slayer/Salve effect forms, 2026-09-08

The [bonus review](combat-bonus-review.md) adds effect identities for the existing
Slayer helmet and Salve families, plus 20 previously missing charged Black mask
(i) forms from Soul Wars and Emir's Arena. The current source has 1,268 unique
usable IDs and the generated runtime has 1,273 definitions including supplies.

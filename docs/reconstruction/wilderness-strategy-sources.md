# Wilderness strategy source review

Reviewed 2026-09-08 against current OSRS Wiki MediaWiki revision source.
This change adds 60 selectable methods: 20 exact boss/style methods and 40
Krystilia methods covering 29 normal combat variants. Existing full method
entries remain in source as reference material rather than being deleted.

## Before and after

| Context | Previous source behavior | Reviewed behavior |
| --- | --- | --- |
| Ordinary Wilderness bears | Dragon scimitar/general melee guidance, without an explicit cannon preparation | Separate cannon + melee, cannon + ranged, melee-only and ranged-only methods using Krystilia's Wilderness grids |
| Cave lesser demons, hellhounds and ice giants | General Slayer melee equipment could represent the Wilderness method | Venator with compatible arrows, with/without cannon, plus a lower-priority melee fallback |
| Cave dust devils, jellies, Greater Nechryael and abyssal demons | Generic Slayer methods could inherit the wrong weapon context | Blood Barrage with/without cannon using autocasting `(a)` sceptres, plus melee fallbacks; task-only gates apply |
| Cave ankou | Generic equipment and area-method overlap | Blood Barrage methods restricted to the level-98 cave variant |
| Black/greater demons, green dragons, ice warriors, mammoths | General Slayer weapon lists omitted contextual Wilderness priority | Ursine then Viggora's chainmace in the named low-defence/large-target contexts, with owned alternatives retained |
| Ordinary black dragons | General ranged equipment could compete with Wilderness advice | Webweaver then Craw's bow at Wilderness locations; KBD excluded |
| Ordinary scorpions, skeletons, spiders | Generic melee/ranged methods | Source's low-Hitpoints blowpipe context, with bows as alternatives and bosses kept separate |
| Zombie pirates | Could inherit general zombie/pirate gear | Chaos Temple-specific Venator and Blood Barrage, with/without cannon, and Salve necklace options |
| Vet'ion / Calvar'ion | Shared strategy methods and inherited grids | Exact variant/location grids; Ursine, Viggora, then source-ordered crush alternatives |
| Callisto / Artio | Artio inherited Callisto strategy context | Individual ranged and magic grids; Webweaver/Craw and Accursed/Thammaron priorities respectively |
| Venenatis / Spindel | Shared strategy context | Individual melee and ranged grids, including the different fallback rows |
| Other Wilderness bosses | Earlier summaries/grids could override current weapon rows | Current Chaos Elemental ranged/melee and full-Verac alternative, Chaos Fanatic ranged and crystal-armour alternative, Crazy archaeologist magic and Scorpia magic grids |
| Lava dragons | Ranged primary with Craw's bow and a melee alternative | Current Magic primary with Accursed sceptre; ranged and stab-chainmace alternatives |
| Venator variants | Only base charged form represented by ordinary equipment metadata | Echo Venator charged form included; both forms retain charges when protected, with Echo ornament kit included in unprotected replacement loss |

## Runtime contracts

New methods supply complete reviewed equipment grids with
`advisor.equipmentReviewed=true`. Upgraded/base weapons are distinct ordered
options: they are not merged into one interchangeable item-ID option.
Venator methods require the actual bow family; barrage methods require an
Ancient-autocasting weapon family. Powered sceptres and `(a)` autocasting
sceptres remain separate usable IDs with their already-reviewed equip gates.

The 20 replaced normal-task strategies use `advisor.excludeLocationIds` to
subtract only the reviewed Wilderness locations. This does not broaden a
Catacombs, Chasm or other named-area method into unrelated places. The compiler
applies the exclusion after existing area narrowing and disables empty resolved
location lists. Previous boss methods remain nonselectable references; the
new methods identify their exact boss and actual chamber. The source preserves
all meaningful travel, solo/team, safespot, equipment, support and task-management
material under `reviewedReferenceSections`, including full pinned source text
for later authoring reviews. These sections are source material, not live
combat prompts or automation.

Krystilia's guide is not interpreted as a universal weapon ranking. Its ranged
advice distinguishes high-defence/large single targets, 2x2 multicombat targets,
and low-Hitpoints targets. Its magic advice distinguishes 1x1 area spells from
powered-staff single-target combat. These distinctions determine the explicit
method variants and locations rather than a general stat multiplier.

Every cannon context has a preparation without a cannon. A carried cannon's
four parts remain part of transport risk; none of these methods treats a
future deployed cannon as already safe. Ether methods use the configured loaded
amount, not a duplicate loose-ether inventory entry. Existing `ETHER_WEAPON`
rules retain the independently lost 1,000 activation ether and captured/assumed
loaded-amount distinction.

Normal and Echo Venator charge-retention flags are supported by the current
base weapon's explicit protected-death rule; Echo is a cosmetic version with no
additional bonuses. Echo's tradeable kit is a separate loss component.
Unprotected charge quantities remain uncertain. No temporary-world-only weapon
forms were added.

Some preparations remain explicit manual checks: freezing supplies for
Callisto/Artio/Scorpia and dragonfire protection or a verified safespot where
needed. The new source does not infer these preparations from levels alone.
The risk calculator's reviewed June/July 2026 death rules take precedence over
older fee examples still present in some strategy prose.

## Change manifest

The following source files belong to this strategy change. Earlier unrelated
worktree changes remain in place; a whole-worktree diff includes reconstruction
work predating this review.

- `src/main/data/slayer/strategies/krystilia-wilderness/strategy.json`
- `src/main/data/slayer/strategies/callisto/strategy.json`
- `src/main/data/slayer/strategies/chaos-elemental/strategy.json`
- `src/main/data/slayer/strategies/chaos-fanatic/strategy.json`
- `src/main/data/slayer/strategies/crazy-archaeologist/strategy.json`
- `src/main/data/slayer/strategies/lava-dragons/strategy.json`
- `src/main/data/slayer/strategies/scorpia/strategy.json`
- `src/main/data/slayer/strategies/venenatis/strategy.json`
- `src/main/data/slayer/strategies/vet-ion/strategy.json`
- `src/main/data/slayer/strategies/abyssal-demons/strategy.json`
- `src/main/data/slayer/strategies/ankou/strategy.json`
- `src/main/data/slayer/strategies/bears/strategy.json`
- `src/main/data/slayer/strategies/black-demons/strategy.json`
- `src/main/data/slayer/strategies/black-dragons/strategy.json`
- `src/main/data/slayer/strategies/dust-devils/strategy.json`
- `src/main/data/slayer/strategies/greater-demons/strategy.json`
- `src/main/data/slayer/strategies/green-dragons/strategy.json`
- `src/main/data/slayer/strategies/hellhounds/strategy.json`
- `src/main/data/slayer/strategies/ice-giants/strategy.json`
- `src/main/data/slayer/strategies/ice-warriors/strategy.json`
- `src/main/data/slayer/strategies/jellies/strategy.json`
- `src/main/data/slayer/strategies/lesser-demons/strategy.json`
- `src/main/data/slayer/strategies/mammoths/strategy.json`
- `src/main/data/slayer/strategies/nechryael/strategy.json`
- `src/main/data/slayer/strategies/pirates/strategy.json`
- `src/main/data/slayer/strategies/scorpions/strategy.json`
- `src/main/data/slayer/strategies/skeletons/strategy.json`
- `src/main/data/slayer/strategies/spiders/strategy.json`
- `src/main/data/slayer/strategies/zombies/strategy.json`
- `src/main/data/slayer/monsters/lesser-demons/lesser-demon-wilderness-lvl94.json`
- `src/main/data/slayer/monsters/ice-giants/ice-giant-wilderness-cave-lvl67.json`
- `src/main/data/slayer/monsters/lava-dragons/lava-dragon-lvl252.json`
- `src/main/data/slayer/advisor/wiki-audit.json`
- `src/main/data/slayer/advisor/equipment-items.json`
- `src/main/data/slayer/advisor/death-rules.json`

No weapon-ID files, equipment-link files, Java files or normalizer scripts were
changed by this data subtask. Existing metadata already distinguishes every
base/upgraded Wilderness weapon and both sceptre modes. The compiler/runtime
integration is owned by the main implementation task.

## Evidence manifest

API parameters: `action=query`, `prop=revisions|info`,
`rvprop=ids|timestamp|content`, `rvslots=main`, `redirects=1`, `format=json`,
`formatversion=2`. Actual resolved titles are recorded below. Tentative plural
`Slayer task/Lesser demons` was missing; the valid source is singular
`Slayer task/Lesser demon`. `Slayer task/Lava dragons` was missing; the current
`Lava dragon/Strategies` page exists and supplies the three real style grids.

| Page | Page ID | Revision ID | Revision timestamp (UTC) |
| --- | ---: | ---: | --- |
| [Accursed sceptre](https://oldschool.runescape.wiki/w/Accursed_sceptre) | 365515 | 15316849 | 2026-08-22T21:53:54Z |
| [Accursed sceptre (a)](https://oldschool.runescape.wiki/w/Accursed_sceptre_(a)) | 366192 | 15316852 | 2026-08-22T21:54:13Z |
| [Artio/Strategies](https://oldschool.runescape.wiki/w/Artio/Strategies) | 392950 | 15331538 | 2026-09-05T15:58:33Z |
| [Blood Barrage](https://oldschool.runescape.wiki/w/Blood_Barrage) | 19221 | 15020225 | 2025-11-08T10:59:27Z |
| [Blood Burst](https://oldschool.runescape.wiki/w/Blood_Burst) | 25023 | 15102375 | 2026-01-09T12:19:56Z |
| [Callisto/Strategies](https://oldschool.runescape.wiki/w/Callisto/Strategies) | 61964 | 15318347 | 2026-08-25T00:24:18Z |
| [Calvar'ion/Strategies](https://oldschool.runescape.wiki/w/Calvar'ion/Strategies) | 384116 | 15324725 | 2026-08-29T20:09:52Z |
| [Chaos Elemental/Strategies](https://oldschool.runescape.wiki/w/Chaos_Elemental/Strategies) | 81996 | 15325471 | 2026-08-30T15:38:49Z |
| [Chaos Fanatic/Strategies](https://oldschool.runescape.wiki/w/Chaos_Fanatic/Strategies) | 81998 | 15325298 | 2026-08-30T03:53:24Z |
| [Craw's bow](https://oldschool.runescape.wiki/w/Craw's_bow) | 119520 | 15264470 | 2026-07-15T18:03:04Z |
| [Crazy archaeologist/Strategies](https://oldschool.runescape.wiki/w/Crazy_archaeologist/Strategies) | 52454 | 15318138 | 2026-08-24T16:48:49Z |
| [Echo venator bow](https://oldschool.runescape.wiki/w/Echo_venator_bow) | 551653 | 15192919 | 2026-04-22T04:53:59Z |
| [Echo venator bow ornament kit](https://oldschool.runescape.wiki/w/Echo_venator_bow_ornament_kit) | 551652 | 15214894 | 2026-05-22T04:02:21Z |
| [Krystilia/Strategies](https://oldschool.runescape.wiki/w/Krystilia/Strategies) | 97339 | 15316255 | 2026-08-22T05:27:05Z |
| [Lava dragon](https://oldschool.runescape.wiki/w/Lava_dragon) | 31202 | 15290540 | 2026-08-08T18:18:04Z |
| [Lava dragon/Strategies](https://oldschool.runescape.wiki/w/Lava_dragon/Strategies) | 82925 | 15316268 | 2026-08-22T05:30:31Z |
| [Scorpia/Strategies](https://oldschool.runescape.wiki/w/Scorpia/Strategies) | 52453 | 15317574 | 2026-08-23T21:22:52Z |
| [Slayer task/Bears](https://oldschool.runescape.wiki/w/Slayer_task/Bears) | 297754 | 15250961 | 2026-07-04T03:17:10Z |
| [Slayer task/Hellhounds](https://oldschool.runescape.wiki/w/Slayer_task/Hellhounds) | 256645 | 15329467 | 2026-09-03T14:40:56Z |
| [Slayer task/Ice giants](https://oldschool.runescape.wiki/w/Slayer_task/Ice_giants) | 566858 | 15251009 | 2026-07-04T03:22:30Z |
| [Slayer task/Lesser demon](https://oldschool.runescape.wiki/w/Slayer_task/Lesser_demon) | 548773 | 15252584 | 2026-07-04T16:27:36Z |
| [Slayer task/Scorpions](https://oldschool.runescape.wiki/w/Slayer_task/Scorpions) | 297991 | 15316864 | 2026-08-22T21:56:11Z |
| [Slayer task/Spiders](https://oldschool.runescape.wiki/w/Slayer_task/Spiders) | 298267 | 15250979 | 2026-07-04T03:17:18Z |
| [Slayer task/Zombies](https://oldschool.runescape.wiki/w/Slayer_task/Zombies) | 298268 | 15327782 | 2026-09-01T22:05:26Z |
| [Spindel/Strategies](https://oldschool.runescape.wiki/w/Spindel/Strategies) | 382059 | 15330947 | 2026-09-04T19:07:47Z |
| [Thammaron's sceptre](https://oldschool.runescape.wiki/w/Thammaron's_sceptre) | 119519 | 15316847 | 2026-08-22T21:53:25Z |
| [Thammaron's sceptre (a)](https://oldschool.runescape.wiki/w/Thammaron's_sceptre_(a)) | 376447 | 15316854 | 2026-08-22T21:54:28Z |
| [Ursine chainmace](https://oldschool.runescape.wiki/w/Ursine_chainmace) | 365539 | 15309757 | 2026-08-19T21:05:05Z |
| [Venator bow](https://oldschool.runescape.wiki/w/Venator_bow) | 373065 | 15328965 | 2026-09-02T20:56:11Z |
| [Venenatis/Strategies](https://oldschool.runescape.wiki/w/Venenatis/Strategies) | 61746 | 15330927 | 2026-09-04T18:51:19Z |
| [Vet'ion/Strategies](https://oldschool.runescape.wiki/w/Vet'ion/Strategies) | 61723 | 15313276 | 2026-08-20T07:45:02Z |
| [Viggora's chainmace](https://oldschool.runescape.wiki/w/Viggora's_chainmace) | 119521 | 15264477 | 2026-07-15T18:10:44Z |
| [Webweaver bow](https://oldschool.runescape.wiki/w/Webweaver_bow) | 365542 | 15309891 | 2026-08-19T21:12:21Z |
| [Wilderness Slayer Cave](https://oldschool.runescape.wiki/w/Wilderness_Slayer_Cave) | 277451 | 15252501 | 2026-07-04T15:36:34Z |

## Validation

- All Slayer source JSON files passed `jq empty`.
- `./gradlew generateSlayerData` passed, including ID/reference validation.
- Generated output was inspected only for verification: all 60 new selectable
  methods have nonempty resolved location lists; Greater Nechryael uses its cave
  location; boss variants use their own chambers and expected first weapons.
- No automated tests or game interaction were performed by this data task.

Source generation logs: `/tmp/aio-wilderness-strategy-generate.log` and
`/tmp/aio-wilderness-strategy-final-generate.log`. The final plugin build and
manual panel verification belong to the integration review.

## Live preparation gate correction

The live lesser-demon preparation exposed an assignment-description string being
treated as a kill-access requirement. Seven reviewed variants were corrected:
lesser-demon-wilderness-lvl94, hellhound-wilderness-slayer-cave, spider,
ankou-level-98, greater-demon-level-104, greater-nechryael and dust-devil.
Risk, location and assignment-only combat prose moved to the existing task
variant notes. Greater Nechryael retains `SLAYER >= 80`; dust devils retain
`SLAYER >= 65`. Dust devils' existing task quest prerequisite and mandatory face
protection item group remain enforced. The actual 85 Slayer abyssal-demon gate
is unchanged. No generic clearing of quest, level or equipment gates was used.

Additional source files in this correction:

- `src/main/data/slayer/tasks/lesser-demons.json`
- `src/main/data/slayer/monsters/hellhounds/hellhound-wilderness-slayer-cave-lvl136.json`
- `src/main/data/slayer/tasks/hellhounds.json`
- `src/main/data/slayer/monsters/spiders/spider.json`
- `src/main/data/slayer/tasks/spiders.json`
- `src/main/data/slayer/monsters/ankou/ankou-lvl98.json`
- `src/main/data/slayer/tasks/ankou.json`
- `src/main/data/slayer/monsters/dust-devils/dust-devil-lvl93.json`
- `src/main/data/slayer/tasks/dust-devils.json`
- `src/main/data/slayer/monsters/nechryael/greater-nechryael-lvl200.json`
- `src/main/data/slayer/tasks/nechryael.json`
- `src/main/data/slayer/monsters/greater-demons/greater-demon-lvl104.json`
- `src/main/data/slayer/tasks/greater-demons.json`

## Conditional equipment review

Chaos Elemental revision 15325471 marks each Verac piece as requiring the full
set. Its full-Verac method now requires the helm, brassard, plateskirt and flail
as equipped pieces. The general melee method retains chainmaces and other
ordinary weapon/armour options, with the conditional Verac rows removed.

Chaos Fanatic revision 15325298 conditions its Bow of Faerdhinen row on crystal
armour. A separate method requires the bow, crystal body and crystal legs, while
keeping the table's Slayer helmet and ordinary ranged head choices. Those three
conditional rows were removed from the general ranged method. The methods keep
the reviewed combat-style ranks; they do not imply a measured DPS comparison
between separately authored methods.

Callisto and Artio explicitly allow Bow of Faerdhinen with or without crystal
armour, so no global bow-to-armour dependency was added. Existing crystal-armour
to-compatible-bow dependencies and Void set dependencies remain enforced.
Other reviewed crystal footnotes at Chaos Elemental and Spindel describe using
crystal pieces with Bow of Faerdhinen and protecting them; they do not explicitly
require an entire armour set. Both original footnotes remain in the pinned
source sections. Contemporary death rules govern whether those pieces are kept.

Final source generation after the preparation-gate and conditional-equipment
corrections is recorded in `/tmp/aio-wilderness-source-review-generate.log`.

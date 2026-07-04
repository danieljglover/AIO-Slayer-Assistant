# OSRS Wiki Source Reference

Use this reference when migrating OSRS Wiki Slayer task, monster, strategy, or
location content into `src/main/data/slayer`.

## Fetch Raw MediaWiki Source

Verify against current OSRS Wiki MediaWiki source, not only rendered HTML. Fetch
pages through the API so exact revisions can be recorded:

```bash
curl -L --fail --silent \
  'https://oldschool.runescape.wiki/api.php?action=query&prop=revisions|info&rvprop=ids|timestamp|content&rvslots=main&titles=Slayer_task/Waterfiends|Waterfiend|Waterfiend/Strategies&format=json&formatversion=2' \
  -o /tmp/osrs-wiki-evidence.json

jq -r '.query.pages[] | [.title, .pageid, .lastrevid, .touched, (.revisions[0].revid // "missing"), (.revisions[0].timestamp // "missing")] | @tsv' \
  /tmp/osrs-wiki-evidence.json
```

Treat `missing` pages as evidence. Many normal Slayer monsters do not have a
`/<monster>/Strategies` page; their strategy content may live inside the
`Slayer_task/<monster>` page instead.

The syntax notes below were verified against these raw page revisions on
2026-07-01:

| Page | Page ID | Revision ID | Revision timestamp |
| --- | ---: | ---: | --- |
| `Slayer_task/Waterfiends` | 362720 | 15245225 | 2026-06-30T20:17:19Z |
| `Waterfiend` | 16265 | 15237720 | 2026-06-23T01:09:23Z |
| `K'ril Tsutsaroth/Strategies` | 24889 | 15226847 | 2026-06-06T03:08:41Z |
| `Catacombs of Kourend` | 74240 | 15241751 | 2026-06-28T18:36:47Z |
| `Slayer_task/Abyssal demons` | 298016 | 15238692 | 2026-06-24T10:34:38Z |
| `Abyssal demon` | 12840 | 15199214 | 2026-04-28T07:15:30Z |
| `Abyssal demon/Strategies` | missing | missing | missing |
| `K'ril Tsutsaroth` | 22758 | 15235043 | 2026-06-18T15:13:23Z |
| `Karuulm Slayer Dungeon` | 199886 | 15175044 | 2026-04-13T20:41:49Z |
| `Slayer_task/Araxytes` | 529981 | 15244118 | 2026-06-30T15:34:52Z |
| `Araxyte` | 325423 | 15243485 | 2026-06-30T14:05:53Z |
| `Araxxor` | 443990 | 15243584 | 2026-06-30T14:20:30Z |
| `Araxxor/Strategies` | 529623 | 15243619 | 2026-06-30T14:24:56Z |

## `Slayer_task/<monster>`

Verified examples: `Slayer_task/Waterfiends`, `Slayer_task/Abyssal demons`, and
`Slayer_task/Araxytes`.

Task pages usually start with `{{Infobox Slayer}}`. Important fields include:

- `name`, `icon`
- `skillreq`, `combatreq`, `otherreq`
- one field per Slayer master, for example `duradel = 130-200`, often with an
  extended range and weighting such as `130-200 (200-250) (Weighting 12)`
- `id`, which is the Slayer target identifier when present

Important body structures:

- prose before the first heading often contains unlocks, requirements, attack
  styles, weakness notes, task value, skip/block guidance, and boss alternatives
- `==Strategy==` may contain the full strategy when no `/Strategies` page exists
- `==Equipment==` may use `<tabber>`, `{{Recommended equipment}}`,
  `{{Equipment}}`, `{{Inventory}}`, and `{{Rune pouch}}`
- `==Monster variants==` or `==Monster Variants==` is usually a raw wikitable
  listing variant names, combat levels, Slayer XP, locations, and notes
- `==Location Comparison==` or `===Locations===` is usually a raw wikitable with
  location, maplink, amount, multicombat, cannonable, safespottable, and notes
- Slayer shop unlocks may appear as raw wikitables under headings such as
  `===Related slayer shop options===` or `==Slayer Unlocks==`

For JSON migration, preserve every strategy method, style option, requirement,
location trade-off, unlock, and boss alternative described on the task page.

## `<npc>` And `<monster>`

Verified examples: `Waterfiend`, `Abyssal demon`, `K'ril Tsutsaroth`, `Araxyte`,
and `Araxxor`.

Combat monster pages usually use `{{Infobox Monster}}`, sometimes inside
`{{Multi Infobox}}`. Boss or stateful pages may also include `{{Infobox NPC}}`
for non-combat states, such as Araxxor's dead state.

Important `{{Infobox Monster}}` fields:

- identity and versioning: `name`, `version1`, `version2`, `bucketname`,
  `image`, `release`, `update`, `members`, `id`, `id1`, `id2`
- combat profile: `combat`, `size`, `hitpoints`, `att`, `str`, `def`, `mage`,
  `range`, `max hit`, `attack style`, `attack speed`, `aggressive`,
  `poisonous`
- Slayer metadata: `slaylvl`, `slayxp`, `cat`, `assignedby`
- defensive bonuses: `dstab`, `dslash`, `dcrush`, `dmagic`, `dlight`,
  `dstandard`, `dheavy`
- elemental weakness: `elementalweaknesstype`, `elementalweaknesspercent`
- immunity and category fields: `attributes`, `immunecannon`, `immunethrall`,
  `poisonresistance`, `venomresistance`, `leagueRegion`

Important body structures:

- `{{HasTask|...}}` links the monster to Slayer task categories.
- `{{HasStrategy}}` means a strategy page probably exists, but still verify it.
- `{{Otheruses|...}}`, disambiguation pages, and redirects must be resolved to
  the concrete combat variant pages before migration.
- locations usually use `{{LocTableHead}}`, one or more `{{LocLine}}` blocks,
  and `{{LocTableBottom}}`.
- drops use `{{DropsTableHead}}`, `{{DropsLine}}`, `{{DropsLineClue}}`,
  `{{DropsTableBottom}}`, and sometimes `dropversion=...`.
- citations and notes often appear inside field values; keep factual caveats in
  migration notes when they affect task, variant, or strategy behavior.

Prefer monster-page stats for variant files and task-page tables for
task-specific amounts, safespots, cannon flags, and location notes.

## `<monster>/Strategies`

Verified examples: `K'ril Tsutsaroth/Strategies` and `Araxxor/Strategies`.
`Abyssal demon/Strategies` was verified as missing, so do not assume this page
exists for every task.

Strategy pages are prose-first MediaWiki documents. They commonly contain:

- major headings such as `==Requirements==`, `==Recommendations==`,
  `==Mechanics==`, `==Strategy==`, `==Fight overview==`, `==Transportation==`,
  `==Suggested skills==`, `==Killcount==`, `==Equipment==`, and `==Inventory==`
- sub-strategies in `===...===` headings, such as solo, tank, attacker, enrage,
  safespot, barrage, or location-specific methods
- `<tabber>` equipment tabs in the form `Tab label = ...` separated by `|-|`
- `{{Recommended equipment}}` and `{{Equipment}}` for style-specific loadouts
- `{{Inventory}}`, `{{Rune pouch}}`, `{{rune pouch}}`, `{{plink}}`,
  `{{plinkp}}`, `{{SCP}}`, `{{efn}}`, and `{{notelist}}`
- raw wikitables for mechanics, special attacks, locations, and unlocks
- media embeds such as files, galleries, and videos; convert these to concise
  mechanical notes when they describe required movement or timing

Every strategy tab and prose method is required. Preserve full breakdowns like
K'ril's scorching bow solo method, tank/attacker roles, and Araxxor's egg,
special attack, and enrage handling. Interpret wiki syntax into structured
`requirements`, `mechanics`, `methods`, `styleOptions`, `equipment`,
`inventory`, and plugin recommendation fields that an LLM can use without
needing to parse MediaWiki templates.

## `<location>`

Verified examples: `Catacombs of Kourend` and `Karuulm Slayer Dungeon`.

Location pages usually start with `{{Infobox Location}}`. Important fields
include `name`, `release`, `members`, `floors`, `location`, `map`, `type`, and
`leagueRegion`.

Important body structures:

- `==Transportation==` describes teleports, shortcuts, diaries, and quest gates
- `==Features==` often contains multicombat, cannon restrictions, heat/damage
  rules, task-only areas, bone or ash effects, and local hazards
- monster lists may use `{{LocationMonsterTableHead}}`,
  `{{LocationMonsterTableLine}}`, and `{{LocationMonsterTableBottom}}`
- some locations use raw wikitables instead of `LocationMonsterTable...`
- maps use `{{Map}}` with coordinate pins; task pages often repeat smaller
  `{{Map|type=maplink}}` blocks inside location comparison tables

Location pages are supporting evidence. Task-page location comparison rows
remain the preferred source for per-task `amount`, `multi`, `cannon`, `burst`,
`safespot`, Konar/task-only notes, and route notes.

## Other Important Formats

- Redirects and disambiguation pages: check `missing`, `#REDIRECT`, and
  disambiguation/`{{Otheruses}}` before deciding a page is the combat source.
- Versioned infoboxes: `version1`, `version2`, `id1`, `id2`, and `bucketname`
  often map to multiple NPC IDs or forms in one page.
- Multi-infobox pages: `{{Multi Infobox}}` can wrap both combat and non-combat
  templates; migrate only the records relevant to Slayer combat unless a state
  affects mechanics.
- Transclusion hints: `{{main|...}}`, `{{HasStrategy}}`, `{{HasTask|...}}`, and
  category navboxes point to additional pages that may contain required data.
- Drop variants: `dropversion=...`, Wilderness-specific sections, brimstone key
  notes, superior variants, and tertiary drops may affect plugin notes even if
  drop tables are not fully modelled.
- Boolean templates: `{{Yes}}`, `{{No}}`, `{{NA}}`, and `{{SCP|Skill|level}}`
  need semantic conversion, not literal string copying.
- Inline references: `{{Cite...}}`, `<ref>`, `{{NamedRef}}`, and `{{Refn}}`
  should not be copied verbatim into JSON; retain only gameplay facts and source
  page metadata unless the citation changes interpretation.
- Wiki links and item templates: `[[Item]]`, `[[Page|label]]`, `{{plink|Item}}`,
  and `{{plinkp|Item}}` should become plain item names or stable weapon IDs.

## Verification Expectations

For every migrated page:

- record OSRS Wiki source URLs used
- record page IDs and revision IDs when docs depend on source evidence
- run `jq empty` across changed JSON files before claiming JSON source validity
- run `./gradlew generateSlayerData` so the compiler validates IDs and
  cross-file references
- run `./gradlew build` before claiming a migration is complete; there are no
  automated tests by design - the owner verifies behavior manually with
  `./gradlew run`

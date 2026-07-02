# Strategy JSON Source Design

Date: 2026-06-30
Status: Approved for K'ril first slice

## Objective

Move strategy source documents from Markdown frontmatter into JSON files that are easy for LLMs to read and
rich enough for future RuneLite integration. The first migrated strategy is K'ril Tsutsaroth from
`https://oldschool.runescape.wiki/w/K'ril_Tsutsaroth/Strategies`.

## Layout

Strategies use a directory per strategy:

```text
src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json
```

The compiler continues to support legacy `.md` strategy files while strategies are migrated one at a time.

## Source Shape

Each JSON strategy has:

- `strategyId`, `variantIds`, and `sourceUrl`.
- `plugin`: the compact fields the current plugin already consumes.
- `requirements`: access requirements and useful prerequisites.
- `mechanics`: attack styles, special mechanics, and relevant adds/bodyguards.
- `methods`: all wiki strategy methods for the monster, with full structured steps, prayers, key items,
  risks, fallbacks, and equipment/inventory details.
- `styleOptions`: all wiki equipment tabs, even when they are variants of a broader method.

## K'ril Coverage

The K'ril JSON must include all strategy methods represented on the wiki page:

- General strategy.
- Solo Scorching bow bind/kite method.
- Solo Tumeken's shadow 5:0 method.
- Tank using Protect from Melee.
- Defensive tank using Protect from Magic.
- Team attacker.

It must also include all equipment style options:

- Ranged.
- Solo Melee.
- Melee Tank.
- Melee.
- Magic.

## Runtime Compilation

Current runtime behaviour remains unchanged. The compiler extracts only `plugin.primaryStyle`,
`plugin.primaryWeapons`, `plugin.secondaryWeapons`, `plugin.note`, and `sourceUrl` into `MonsterStrategy`.
Richer method/style data remains source-only until the plugin grows a dedicated UI or advisor surface for it.

## Validation

Existing validation still applies to plugin fields:

- `strategyId` is unique.
- `variantIds` reference known monster variants.
- `plugin.primaryWeapons` and `plugin.secondaryWeapons[].weaponId` reference known weapon IDs.

The K'ril source layout test additionally proves that the first migrated strategy is JSON, not Markdown, and
that all required methods and style options are represented.

# Slayer helmet and Salve bonus review

## Corrected behavior

The old advisor selected the first compatible Wiki loadout and ranked owned
fallbacks by flat stats. It did not apply Slayer helmet or Salve multipliers.
HEAD was selected before AMULET, so it could not compare the two slots together.

The runtime now compiles 112 reviewed item IDs into six effect families, including
regular and imbued Slayer helmet recolours, Black mask charge states and all
three imbue sources. Twenty charged Black mask (i) IDs were missing from the
prior equipment metadata and are now included. Oathplate helm has no Slayer
effect; Oathplate *slayer* helmets retain the correct regular/imbued effects.

| Family | Melee accuracy/strength | Ranged accuracy/strength | Magic accuracy/damage |
| --- | --- | --- | --- |
| Slayer helmet / Black mask | 7/6 | None | None |
| Imbued Slayer helmet / Black mask | 7/6 | 1.15 | 1.15 |
| Salve | 7/6 | None | None |
| Salve (e) | 1.20 | None | None |
| Salve (i) | 7/6 | 7/6 | 1.15 |
| Salve (ei) | 1.20 | 1.20 | 1.20 |

Active-task effects require a logged-in, unfinished matching assignment and an
eligible target in its assigned area. At the owner's request, Catalogue searches
assume the selected task for planning and apply eligible Slayer effects to its
targets. The panel and copied setup label this as an on-task preview. Actual
assignment detection and saved account progress remain separate; Krystilia
previews still require Wilderness kills, and Salve still suppresses Slayer boosts.
The compiled task flag can disable the effect. Salve uses the actual monster
variant's undead flag, including Vet'ion and Calvar'ion; it is not inferred from
the task family or a monster's name.

An applicable Salve effect takes priority over the Slayer helmet/Black mask
boost. The equipment can still be worn together for protection or flat stats,
but their offensive multipliers are never added or multiplied together. A Salve
form with no effect for the chosen style does not contribute a boost.

## Comparison boundaries

The planner starts from a compatible Wiki loadout and compares owned usable
head/amulet pairs while keeping all other equipment fixed. It preserves required
protection, shield/ammunition constraints and companion groups. An original
head/amulet option with its own set/effect dependencies is retained so the
comparison cannot silently dismantle that method's set. Item requirements and
ownership still apply, including when a bonus item is absent from the Wiki row.

The relative score uses total style-specific attack and strength/magic-damage
stats, the existing strength weight of four, and the goal's prayer/defence
weights. Attack and melee/ranged damage have a 64 baseline; magic damage has a
100 baseline. Exactly one eligible multiplier scales the offensive terms.
These offsets value the effect on the whole attack even when the bonus item has
no offensive equipment stats. Ties retain the original choice or Wiki order.
This is not an exact DPS, max-hit, attack-type, prayer, potion, target-defence,
weapon-effect or death-risk calculation. Other set effects are not optimized.

For barrage and chinchompa area methods, Salve's primary-target boost is not
credited across secondary targets. The comparison conservatively credits no
area boost from a pair whose primary-target Slayer bonus is suppressed by
Salve. Explanations distinguish the actual primary-target bonus from this
ranking limitation. Cannon accuracy receives neither target-specific effect.

Changed choices are labelled `Bonus comparison`; explanations identify the
active percentage and any suppressed helmet effect. Oathplate plus Salve can
still outrank a Slayer helmet plus a different amulet under this estimate.
Vet'ion's Wiki strategy also prioritizes Salve in its Wilderness risk context.

This supersedes the old ADR-0007 simplification that independently credited both
slots. That historical document describes the removed engine.

## Source evidence

Current raw MediaWiki revisions were retrieved through `api.php` on 2026-09-08,
with redirects enabled and `rvprop=ids|timestamp|content`. The editable effect
manifest is `src/main/data/slayer/advisor/combat-bonuses.json`. Item requirements
and cosmetic-form evidence remain in `equipment-items.json`.

| Page | Page ID | Revision ID | Revision timestamp |
| --- | ---: | ---: | --- |
| [Salve amulet (e)](https://oldschool.runescape.wiki/w/Salve_amulet_(e)) | 11031 | 15183197 | 2026-04-22T02:51:55Z |
| [Black mask](https://oldschool.runescape.wiki/w/Black_mask) | 12925 | 15290821 | 2026-08-09T14:13:43Z |
| [Salve amulet](https://oldschool.runescape.wiki/w/Salve_amulet) | 13206 | 15241628 | 2026-06-28T12:50:11Z |
| [Black mask (i)](https://oldschool.runescape.wiki/w/Black_mask_(i)) | 23398 | 15213278 | 2026-05-20T03:54:56Z |
| [Slayer helmet](https://oldschool.runescape.wiki/w/Slayer_helmet) | 27486 | 15322450 | 2026-08-27T21:55:37Z |
| [Slayer helmet (i)](https://oldschool.runescape.wiki/w/Slayer_helmet_(i)) | 27974 | 15322452 | 2026-08-27T21:55:53Z |
| [Vet'ion](https://oldschool.runescape.wiki/w/Vet%27ion) | 31269 | 15332958 | 2026-09-07T00:55:18Z |
| [Salve amulet(i)](https://oldschool.runescape.wiki/w/Salve_amulet(i)) | 34093 | 15242254 | 2026-06-29T20:10:56Z |
| [Salve amulet(ei)](https://oldschool.runescape.wiki/w/Salve_amulet(ei)) | 34094 | 15330245 | 2026-09-03T19:21:00Z |
| [Vet'ion/Strategies](https://oldschool.runescape.wiki/w/Vet%27ion%2FStrategies) | 61723 | 15313276 | 2026-08-20T07:45:02Z |

## Verification

- `./gradlew build` passed Java 11 compilation and both catalogue generators.
- Both changed/new advisor JSON files passed `jq empty`.
- Generated output contains all 112 effect IDs and distinct regular/imbued effects;
  Vet'ion and Calvar'ion retain their undead flags.
- No automated tests, test files or testing dependencies were added.
- Independent focused code review found no concrete correctness/regression gaps.
- The development client was reopened through the local launcher. The owner
  logged in and refreshed the bank; the panel detected Skeletons, 66 remaining.
- In the Vet'ion preparation, the live panel selected Slayer helmet (i) and
  Amulet of rancour, labelled both `Bonus comparison`, and displayed +16.67%
  melee accuracy/damage for the matching task/area. The agent only scrolled the
  plugin panel and expanded its explanation; no game input was performed.
- Other account scenarios below remain for the owner's manual verification.

Manual checks: Skeletons/Vet'ion with and without Salve available; an ordinary
non-undead melee task; imbued versus regular helmets for ranged and magic;
Salve (i) versus (ei); no-task browsing; wrong Konar area; required headgear/full
Void; and barrage primary versus secondary-target explanations. Confirm that
paired equipment never claims both offensive effects on the same hit.

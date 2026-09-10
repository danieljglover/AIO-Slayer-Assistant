# Task variant link review

Reviewed current raw OSRS Wiki MediaWiki API source on 2026-09-08. This review repairs missing task-to-monster joins in the existing source graph. It does not claim a complete migration of every current wiki monster or strategy.

The compiler copies `tasks/*.json` `variantIds` into the catalogue. A monster existing under Boss, Hellhounds or another family did not automatically make it available under Skeletons, Bears or Spiders. The joins now use the existing combat variant and strategy IDs.

This pass adds 45 joins across 14 task records and compares the variant tables from 72 available task pages against existing monster identities. The structured [evidence manifest](../../src/main/data/slayer/advisor/task-variant-evidence.json) includes source URLs, page IDs, revision IDs, timestamps, reviewed table rows and unresolved source gaps.

| Task | Added variants |
| --- | --- |
| Ogres | skogre-zombie |
| Skeletons | vet-ion, calvar-ion, skogre, skeleton-hellhound-vet-ion, greater-skeleton-hellhound-vet-ion, skeleton-hellhound-calvar-ion, greater-skeleton-hellhound-calvar-ion |
| Bears | callisto, artio |
| Spiders | venenatis, spindel, sarachnis, araxxor, araxyte-level-96, araxyte-level-146, dreadborn-araxyte, fever-spider |
| Rats | brine-rat |
| Monkeys | demonic-gorilla |
| Zombies | blue-dragons-vorkath, zombie-pirate, zogre, skogre-zombie |
| Scorpions | scorpia |
| Ghosts | revenant-imp, revenant-goblin, revenant-pyrefiend, revenant-hobgoblin, revenant-cyclops, revenant-hellhound, revenant-demon, revenant-ork, revenant-dark-beast, revenant-knight, revenant-dragon |
| Elves | reanimated-elf |
| Dagannoth | reanimated-dagannoth |
| Bloodveld | reanimated-bloodveld |
| Trolls | reanimated-troll |
| Boss | calvar-ion, artio, spindel, araxxor, kraken |

Existing source IDs stay stable. The only new combat identity in this link pass is `skogre-zombie` (NPC 878). The current Skogre infobox gives that form the Ogres/Zombies categories; NPCs 872 and 879 remain `skogre` and count as Ogres/Skeletons. The Ogre strategy explicitly binds the new identity to its brutal-arrow method.

The four reanimated variants, Dreadborn Araxyte and the four skeleton hellhound boss summons carry `advisorRepeatable: false`. They remain task-counting context, while the runtime excludes them from automatic repeatable combat recommendations. Reanimated forms use the Dark Altar anchor with the local-drop alternative preserved in notes. Boss summons use the actual Skeletal Tomb or Vet'ion's Rest chamber.

Calvar'ion, Artio and Spindel are also linked under Boss as substitutes for Vet'ion, Callisto and Venenatis respectively. Araxxor is included for the specific Konar boss assignment, and the existing Kraken variant is also linked to Boss. Each primary Wilderness boss requires its Medium Wilderness Diary OR its matching boss assignment; each weaker substitute requires Hard Wilderness Diary OR that same matching boss assignment. A Skeletons/Bears/Spiders assignment does not waive the diary.

| Source | Page ID | Revision ID | Revision timestamp |
| --- | ---: | ---: | --- |
| [Slayer task/Skeletons](https://oldschool.runescape.wiki/w/Slayer_task/Skeletons) | 298266 | 15325000 | 2026-08-29T22:05:41Z |
| [Slayer task/Bears](https://oldschool.runescape.wiki/w/Slayer_task/Bears) | 297754 | 15250961 | 2026-07-04T03:17:10Z |
| [Slayer task/Spiders](https://oldschool.runescape.wiki/w/Slayer_task/Spiders) | 298267 | 15250979 | 2026-07-04T03:17:18Z |
| [Slayer task/Monkeys](https://oldschool.runescape.wiki/w/Slayer_task/Monkeys) | 298251 | 15250976 | 2026-07-04T03:17:16Z |
| [Slayer task/Rats](https://oldschool.runescape.wiki/w/Slayer_task/Rats) | 300146 | 15251205 | 2026-07-04T06:14:04Z |
| [Slayer task/Ghosts](https://oldschool.runescape.wiki/w/Slayer_task/Ghosts) | 298241 | 15250974 | 2026-07-04T03:17:16Z |
| [Slayer task/Zombies](https://oldschool.runescape.wiki/w/Slayer_task/Zombies) | 298268 | 15327782 | 2026-09-01T22:05:26Z |
| [Slayer task/Scorpions](https://oldschool.runescape.wiki/w/Slayer_task/Scorpions) | 297991 | 15316864 | 2026-08-22T21:56:11Z |
| [Skogre](https://oldschool.runescape.wiki/w/Skogre) | 22158 | 15199447 | 2026-04-28T07:20:03Z |
| [Vet'ion](https://oldschool.runescape.wiki/w/Vet'ion) | 31269 | 15332958 | 2026-09-07T00:55:18Z |
| [Calvar'ion](https://oldschool.runescape.wiki/w/Calvar'ion) | 374839 | 15309944 | 2026-08-19T21:25:26Z |
| [Callisto](https://oldschool.runescape.wiki/w/Callisto) | 31268 | 15309935 | 2026-08-19T21:24:40Z |
| [Artio](https://oldschool.runescape.wiki/w/Artio) | 374838 | 15309934 | 2026-08-19T21:24:33Z |
| [Venenatis](https://oldschool.runescape.wiki/w/Venenatis) | 31270 | 15324347 | 2026-08-29T08:17:05Z |
| [Spindel](https://oldschool.runescape.wiki/w/Spindel) | 374849 | 15309918 | 2026-08-19T21:20:00Z |
| [Araxxor](https://oldschool.runescape.wiki/w/Araxxor) | 443990 | 15326066 | 2026-08-31T06:17:34Z |

Review decisions that prevent false alternatives:

- Task-specific boss identities and Kalphite Queen phase variants already represent their boss. The review does not add duplicate Boss catalogue IDs to those tasks.
- The Jormungand remains a documented one-off quest form, without a repeatable Basilisks recommendation. Tarn's Lair Skeleton Hellhound is only evidenced for Hellhounds; the four Wilderness boss summons explicitly count as Skeletons too.
- Fever spiders are listed directly in the Spider task table, despite their narrower monster-infobox category. Deadly red spiders, Maniacal monkeys and Brine rats are valid umbrella-task alternatives; previous exclusion notes were corrected.
- Venenatis/Spindel spiderlings and Araxxor egg-spawned araxytes do not decrement the remaining task count. Sarachnis minions do count, but their missing combat source records need a separate migration.
- The Dreadborn row uses the monster page's 4462.5 Slayer XP, rather than the inconsistent 4658 in the Spider task table. Calvar'ion minion pages report variable spawn counts, so guidance does not promise five task decrements per fight.

Remaining gaps include unmodelled ordinary Skeleton forms (including Ape Atoll), Bear Cubs and Escape Caves bears, Monkey Archers/Guards/Zombies and Tortured/Maniacal monkeys, Deadly red and other Spider variants, additional Zombie/Ghost/Scorpion forms, and other Rats table variants. The evidence manifest lists unresolved rows from the affected starter-task tables. These require proper combat IDs, locations and complete strategies, rather than guessed joins. The Boss slayer table additionally exposes missing source records for Commander Zilyana, Duke Sucellus, The Leviathan, Maggot King, Vardorvis, The Whisperer and the non-Konar Deranged archaeologist substitute. Scurrius is handled by a separate source migration and cannot itself be assigned as a Boss task.

Validation for this pass: JSON syntax checks and source-reference checks. The parent change runs `./gradlew generateSlayerData` and `./gradlew build`; the owner verifies the catalogue manually with `./gradlew run`. No automated tests were added.

## Integrated correction

The complete change adds another four joins beyond the 45 listed above: three
Scurrius variants for Rats and the level 45 northern Edgeville Dungeon skeleton.
Scurrius has 20 combat preparations covering six source equipment tabs, solo/group
encounters, and explicit magic alternatives, plus six support methods. Its
summoned giant rat is task-counting context only. The Scurrius strategy source is
[Scurrius/Strategies](https://oldschool.runescape.wiki/w/Scurrius/Strategies), page
413365, revision 15331669, timestamp 2026-09-05T18:19:34Z.

The ordinary Skeleton source incorrectly pointed to Graveyard of Shadows. The
existing level 21 and 22 IDs now remain in their non-Wilderness context, while
`skeleton-edgeville-wilderness` uses the armed level 45 NPC 82 and the seven
northern Edgeville Dungeon spawns. The old Graveyard location ID is retained but
unlinked from Skeletons. Evidence: [Skeleton](https://oldschool.runescape.wiki/w/Skeleton),
page 13405, revision 15323222, timestamp 2026-08-28T15:58:12Z, and the
Skeletons task revision listed above. The Wilderness method is restricted to
that actual Wilderness variant and location.

Automatic recommendations exclude Wilderness locations when the checkbox is
off, even when no setup is ready. An active Wilderness assignment displays an
explanation instead of overriding the preference. Explicitly selected excluded
locations remain blocked previews. Preference changes clear stale results in
both panel tabs. Duplicate monster names now show combat levels and, where
needed, locations.

The six Wilderness lairs enforce their diary requirement or the actual matching
boss assignment. Only the diary can be confirmed manually; the boss waiver is
read from live assignment state. The current boss reader recognizes the three
weaker boss substitutes.

`./gradlew generateSlayerData`, `./gradlew build`, all 1,144 source JSON syntax
checks, and `git diff --check` passed. Generated output was inspected for task
joins, unique references, incidental-target exclusion, the corrected Skeleton
location, and mandatory item IDs. No automated tests were added. Live verification
of the updated code awaits an owner-controlled client restart; the running game
was not interrupted.

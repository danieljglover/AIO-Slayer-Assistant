# Infernal Mage setup and routing review

Reviewed on 2026-09-11 after a clan tester reported no setup, while the owner's
catalogue preview worked. The existing melee method already inherited the
general Slayer training equipment grid. A replacement grid was unnecessary.

Two source defects were confirmed:

- The variant requirement `45 Slayer.` did not match the evaluator's exact
  skill syntax. It appeared as an unknown manual confirmation even on an
  eligible account. Use `SLAYER >= 45` so current levels decide the result.
- The task linked only Vannaka. Mortimer also assigns Infernal Mages; selecting
  him caused the engine's master/task check to return no setups. His base
  assignment is 35-50, weight 10, before modifiers. Vannaka remains 40-90,
  weight 8. Neither missing ownership nor a failed account check normally
  removes the setup: the panel displays a blocked candidate in those cases.

The tester subsequently confirmed the Active task tab displayed
`The task is not listed for the selected Slayer master`, identifying the
master/task early return as the cause. Their exact master is not yet known.

The engine now uses a logged-in active assignment with kills remaining even
when its master link is absent from bundled data. It retains a coverage notice
and continues normal level, ownership, access, variant, area and Wilderness
checks. Catalogue previews still reject unlisted master/task pairs. The plugin
also keeps an unknown active master unknown instead of substituting a saved
selection. These changes address missing or historical assignment links beyond
Infernal Mages without making catalogue previews assume invalid assignments.
The Checks panel also follows the engine's preview-only master eligibility
checks: having an active task does not require re-proving the requirements to
obtain that task. Combat, equipment and travel requirements still apply.

## Wiki evidence

Fetched current raw MediaWiki revisions through `api.php` with
`prop=revisions|info`, `rvprop=ids|timestamp|content` and `rvslots=main`.

| Page | Page ID | Revision ID | Revision timestamp (UTC) |
| --- | ---: | ---: | --- |
| [Infernal Mage](https://oldschool.runescape.wiki/w/Infernal_Mage) | 12370 | 15339658 | 2026-09-10T15:17:55Z |
| [Vannaka](https://oldschool.runescape.wiki/w/Vannaka) | 11509 | 15332812 | 2026-09-06T21:45:59Z |
| [Mortimer](https://oldschool.runescape.wiki/w/Mortimer) | 663129 | 15327685 | 2026-09-01T18:26:44Z |
| [Slayer Tower](https://oldschool.runescape.wiki/w/Slayer_Tower) | 6923 | 15290578 | 2026-08-08T19:39:00Z |
| [Slayer training](https://oldschool.runescape.wiki/w/Slayer_training) | 16746 | 15336054 | 2026-09-09T01:09:50Z |
| [Protect from Magic](https://oldschool.runescape.wiki/w/Protect_from_Magic) | 12409 | 15303753 | 2026-08-17T06:33:21Z |
| [Nose peg](https://oldschool.runescape.wiki/w/Nose_peg) | 15342 | 15183698 | 2026-04-22T02:56:29Z |

`Infernal Mage/Strategies` and `Slayer task/Infernal Mages` are missing pages.
The monster page supplies combat and location facts; Slayer training supplies
general melee equipment guidance. This review retains the existing shared
equipment table and its own extraction provenance. Melee is the authored
default, supported by zero melee defence bonuses and general training advice;
the monster page does not contain an Infernal Mage equipment ranking.

All 15 existing route destinations match the current monster page, on plane 1
in the north-east room. The ordinary south-east stairs and optional 18 Agility
window avoid requiring the spectre-room chain. The latter requires 61 Agility;
60 Slayer is the Nose peg equipment gate, not an Infernal Mage access gate.
The tower prohibits cannons. No safespot is documented on the monster page,
so the old safe-spot recommendation and task-specific flag were removed.

The legacy synthetic `slayerTargetId` remains unchanged. Active task detection
uses the client's task tables; it must not begin relying on that placeholder.

## Verification

- Changed JSON passed `jq empty`; `git diff --check` passed.
- `./gradlew generateSlayerData build` passed. Existing catalogue-wide
  unresolved item-label warnings remain; the Infernal Mage method compiled
  with all 11 equipment slots and resolved weapon IDs.
- Generated output contains both master links, the canonical level gate, one
  selectable melee method, a nonselectable travel method, and 15 plane-1 route
  points bound only to Infernal Mage at the Slayer Tower.
- The live plugin initially displayed `Confirm requirement: 45 Slayer.`.
  After a panel-only AIO reload, the same catalogue preview displayed
  `Available with known items` without that confirmation.
- The Route button reached `Route ready` for the existing spawn points, then
  Clear removed the route. Coordinates are unchanged by this review.

Only plugin-panel controls were used. No game actions or automated tests were
performed. The updated engine still needs the clan tester to retry the reported
active-task scenario; the owner's live task was different during this review.

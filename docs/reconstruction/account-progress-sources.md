# Account progress observations

`AccountProgressCapture` reads the logged-in account on the RuneLite client
thread. The captured values are observations, not persisted confirmations. No
runtime network access or game actions are involved.

The implementation was checked against RuneLite API 1.12.38 and the sources
below on 2026-09-08. The accompanying Wiki evidence verifies the equipment
waiver's meaning; runtime observations still come only from the client.

## Quest state

[RuneLite Quest.java](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Quest.java)
defines the current quest and miniquest enum, including Recipe for Disaster
subquests. Its `getState` method invokes the read-only quest-status script.
The capture first checks for the quest's display-name table field, because a
missing quest definition must not become an observed failure.

Each quest has separate finished and started observations. Exact names are
preserved, including names containing `and` or `&`. Explicit source phrases that
describe only one quest gate have reviewed aliases; there is no general prose
parser that treats every mention of a quest as proof of access.

Completed quests can prove selected intermediate milestones already recognized
by the plugin: receiving the ancient mace, speaking to Grish, defeating the
Culinaromancer, and the Desert Treasure II bosses. An unfinished quest does not
prove those milestones absent. Those derived keys are omitted until proven, so
manual fallback remains available.

## Achievement diaries

[RuneLite gameval VarbitID](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/VarbitID.java)
provides the explicit identifiers for all 12 regions and four tiers.
The decompiled game
[area_task_complete script](https://github.com/RuneStar/cs2-scripts/blob/master/scripts/%5Bproc%2Carea_task_complete%5D.cs2)
shows that Karamja's original easy, medium, and hard completion varbits
(`3578`, `3599`, `3611`) must equal `2`. Karamja elite (`4566`) and the other
regions use boolean completion flags equal to `1`. A partially completed
Karamja tier must not be treated as complete merely because its value is nonzero.

Task completion and reward collection are distinct observations. The game's
[diary_completion_info script](https://github.com/RuneStar/cs2-scripts/blob/master/scripts/%5Bproc%2Cdiary_completion_info%5D.cs2)
returns separate task counts and reward flags. Current Quest Helper
[QuestVarbits](https://github.com/Zoinkwiz/quest-helper/blob/master/src/main/java/com/questhelper/questinfo/QuestVarbits.java)
uses the reward flags for its finished-diary state, with completion value `1` in
[QuestHelperQuest](https://github.com/Zoinkwiz/quest-helper/blob/master/src/main/java/com/questhelper/questinfo/QuestHelperQuest.java).

`Elite Kourend & Kebos Diary` records task completion from
`KOUREND_DIARY_ELITE_COMPLETE` (`7928`). Equipment heat-protection waivers instead
use `Elite Kourend & Kebos Diary reward claimed`, read from
`KOUREND_ELITE_REWARD` (`7932`). This keeps a task-completion observation from
waiving equipment before the account has collected the diary benefits.

Current OSRS Wiki MediaWiki source was fetched through the revisions API on
2026-09-08. The
[Kourend & Kebos Diary](https://oldschool.runescape.wiki/w/Kourend_%26_Kebos_Diary?oldid=15300044)
source (page ID `199354`, revision `15300044`, timestamp
`2026-08-14T04:28:49Z`) places permanent Karuulm protection in the elite rewards
and identifies Elise as the reward collector. The
[Boots of stone](https://oldschool.runescape.wiki/w/Boots_of_stone?oldid=15195794)
source (page ID `199885`, revision `15195794`, timestamp
`2026-04-24T15:03:07Z`) confirms the boots' Karuulm protection and the alternative
protective footwear. Reward collection is therefore the conservative fact used
for the equipment waiver.

## Other unlocks and limits

The capture uses named RuneLite gameval flags for unlocked prayers, Bones to
Peaches, Captain Cleive's Slayer helmet protection, the unlocked Slayer Tower
roof, and the reviewed Slayer reward and extension requirements. Prayer flags
describe permanent unlocks, not currently activated prayers. Bigger and Badder
checks both `SLAYER_UNLOCK_SUPERIORMOBS` and the separate
`SLAYER_TOGGLEOFF_SUPERIORMOBS` flag, because a purchased unlock can be disabled.

Missing quest or varbit definitions, failed reads, and unexpected encoded values
produce `UNKNOWN`. An observed `UNMET` remains distinct from an unavailable read.
Specific partial quest milestones, training stages, travel preparations, and
mixed quest/gear requirements without reviewed state mappings remain manual.
Skill checks, compound requirement evaluation, worn equipment, and spellbooks
are handled outside this capture class.

## Requirement evaluation and panel

The engine and Account requirements panel share `RequirementEvaluator.assess`.
Exact observed facts take priority over saved manual confirmations. The panel hides satisfied requirements, including automatic and manual
confirmations. It shows unmet requirements, unavailable account data, and manual
confirmation only where no supported observation resolves the condition. If no
checks remain, it shows that all listed requirements are satisfied.
Skill entries show current base levels and required levels; temporary boosts do
not raise an equipment requirement's captured base level.

Expressions preserve whole quest/unlock/diary names. Semicolon-separated
requirements stay required even when one clause offers alternatives. Commas
before `and`/`or` belong to that conjunction. A boosted alternative such as
`86 Slayer with a wild pie boost` checks the base level automatically and leaves
boost preparation for manual confirmation. Literal `None` means no requirement.

Known unmet clauses cannot be bypassed by confirming the whole sentence. A
supported but unavailable observation also withholds automatic confirmation;
unsupported quest milestones and preparations retain the manual fallback.
The existing varbit/stat and periodic capture events refresh the panel without
writing completion flags, clicking game controls, or persisting automatic state.

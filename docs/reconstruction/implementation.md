# AIO Slayer reconstruction

Approved scope: fresh native Java 11 RuneLite implementation; active-task and full catalogue views; wiki-first complete equipment with owned fallback; ranked XP/profit/low-effort goals; solo/non-Wilderness defaults; complete source audit. No automated tests, runtime data network access, game actions, or external application. Keep unrelated scratchpad changes and session secrets untouched.

## Work ownership

- Data task: source audit, new catalogue/compiler and source models, build integration, coverage report.
- Engine task: recommendation runtime request/result/player models and pure engine under loadout/advisor.
- Panel task: native Swing panel and passive overlay under ui/advisor.
- Integration: RuneLite state capture, plugin entry/configuration, final cleanup and verification.

## Shared contracts

All new runtime models live in model/advisor. Java 11. No existing loadout engine dependencies.

`SlayerCatalogue` (data task) uses maps keyed by stable IDs: masters, tasks, monsters, locations, methods. Nested public static Lombok @Data @NoArgsConstructor types; non-null collections by default. Getter names follow Lombok.

- Master: id, name, location (String); combatLevel, slayerLevel (int); requirements, notes (List<String>); evidence (List<Evidence>).
- Task: id, name; targetId, slayerLevel (int); masterIds, monsterIds, locationIds, notes, requirements (List<String>); amounts (Map<String,String>); requiredItems (List<Supply>); evidence (List<Evidence>).
- Monster: id, name; npcIds (List<Integer>); locationIds, methodIds, requirements (List<String>); slayerLevel (int); boss (boolean); evidence (List<Evidence>).
- Location: id, name; wilderness, multi, cannon, barrage, safespot, taskOnly (boolean); requirements, notes (List<String>); travel (List<Supply>); evidence (List<Evidence>).
- Method: id, name, style (MELEE/RANGED/MAGIC String), role, summary; monsterIds, locationIds, requirements, guidance, risks (List<String>); equipment (Map<String,List<ItemOption>>); requiredItems, inventory, switches (List<Supply>); xpRank, profitRank, effortRank (int; lower is better; 0 means unrated); rankingReason (String); cannon, barrage, group, taskOnly, selectable (boolean, selectable defaults true); evidence (List<Evidence>).
- ItemOption: name; itemIds (List<Integer>, equivalent usable alternatives only); requires (List<List<Integer>>, AND across groups/OR within group; equipped companion dependencies); requirements (List<String>).
- Supply: name; itemIds (List<Integer>, ordered alternatives); quantity (int default 1); required, stackable (boolean).
- Evidence: url, title, timestamp (String); pageId, revisionId (long); missing (boolean).

Equipment slot keys: HEAD,CAPE,AMULET,WEAPON,BODY,SHIELD,LEGS,HANDS,FEET,RING,AMMO.
`data.AdvisorDataService`: `void load()` and `SlayerCatalogue getCatalogue()` loads /data/advisor-catalogue.json. Data task owns it.

Engine task defines these models with defensive collection copies:
- RecommendationGoal enum SLAYER_XP, PROFIT, LOW_EFFORT with toString display.
- PlayerSnapshot: owned, carried (Map<Integer,Integer>); items (Map<Integer,ItemStats>); levels (Map<String,Integer> upper-case Skill names); confirmedRequirements (Set<String>); loggedIn (boolean); bankSeenAt (long epoch ms, 0 absent). Constructor in exactly this order. Nested ItemStats constructor fields: id (int), name, slot, style (String; style may ANY), usable, twoHanded, stackable (boolean), attack, strength, defence, prayer (double). ItemStats are captured from RuneLite by root. Unusable item forms excluded before snapshot. No client access in engine.
- RecommendationRequest: taskId, masterId, monsterId, locationId, methodId (nullable String); activeTask (boolean); remaining (int); lockedLocationId (nullable String); wildernessAssignment (boolean); goal (RecommendationGoal); allowWilderness, allowGroups (boolean); player (PlayerSnapshot). Constructor in exactly this order. Missing selected IDs means automatic.
- RecommendationResult: taskId (String); setups (List<Setup>); notices (List<String>). `best()` returns first feasible setup or null. Nested Setup getters: monsterId, locationId, methodId, title, summary (String); feasible (boolean); equipment (Map<String,Choice>); inventory (List<Choice>); explanations, blockers, guidance (List<String>); evidence (List<SlayerCatalogue.Evidence>). Choice fields: itemId, quantity, carried, withdraw, missing (int); slot, name, origin (String; origin = Wiki/Wiki alternative/Owned fallback/Required/Bonus comparison); required (boolean).
- `loadout.advisor.RecommendationEngine`: no-arg constructor; `RecommendationResult recommend(SlayerCatalogue catalogue, RecommendationRequest request)`.

Panel task defines `ui.advisor.AdvisorPanel extends PluginPanel` with no-arg constructor and:
- `setCatalogue(SlayerCatalogue catalogue)` on EDT.
- `setOnSelection(Consumer<Selection>)` on EDT. Nested immutable Selection public getters: browsing (boolean); taskId, masterId, monsterId, locationId, methodId (String nullable), goal (RecommendationGoal); allowWilderness, allowGroups (boolean). Constructor in this field order.
- `setOnRefresh(Runnable)`, `setOnExport(Runnable)`, `setOnConfirmRequirement(Consumer<String>)`.
- `render(RecommendationRequest request, RecommendationResult result)` on EDT; keeps active vs catalogue selection independent. Active task is provided through render request.taskId when activeTask true. Add `setActiveTask(String taskId, int remaining, String status)` for independent active header updates during browsing.
- `showStatus(String text)`; `setPreferences(RecommendationGoal goal, boolean wilderness, boolean groups)` initial setup.
- `ui.advisor.AdvisorOverlay extends OverlayPanel`: no-arg constructor; `update(String title, String detail)`, `setEnabled(boolean)`; no client reads.

## Delivery ledger

- Baseline: branch feat/aio-slayer-reconstruction; only pre-existing dirty path scratchpad/dev-run.log. Baseline build passed.
- Ruling: work in the user's current workspace on a feature branch; changes stay directly reviewable and no unrelated edits are rewritten.

## Integration refinements

- `RecommendationRequest` additionally accepts an immutable `allowedMonsterIds`
  set (final constructor argument; original constructor delegates to an empty set).
  Active grouped boss assignments constrain eligibility without preventing users
  from choosing among the assigned group. This lock is independent of browsing.
- `Location.requiredItems` represents environmental protection. A
  `Supply.waiverRequirement` may waive it only after the exact account exemption
  is confirmed, such as the relevant elite diary for Karuulm boots.
- `Location.assignmentAreaId` can explicitly link a chamber to its canonical
  Konar assignment area, without inheriting that area's combat flags or access.
- `Method.equipmentReviewed` preserves a complete authored equipment grid against
  inherited table replacement. Supporting referrals remain informational and
  direct users to the named monster's full strategy.
- `SlayerCatalogue.items` stores item definitions with explicit level requirements
  and a `requirementsKnown` marker. Unknown equipment requires a `Can equip: NAME`
  confirmation; currently worn equipment provides that evidence automatically.
- Panel additions: `setItemManager`, `setActiveAssignmentIdentity`,
  `setActiveAllowedMonsters`, and `setOnClearConfirmations`. Account switching and
  assignment changes invalidate independent active selections and stale exports.
- Runtime item/quest reads stay on the client thread; identical snapshots are
  suppressed, new requests supersede queued work, and only the current revision
  can be published on the EDT.

- `PlayerSnapshot` also accepts a nullable `Spellbook` enum as the final constructor
  argument. Client capture reads `VarbitID.SPELLBOOK`; no value is assumed when
  logged out or unavailable. Supported named-book requirements reject mismatches
  before consulting account confirmations. Compound preparation requirements
  still require their remaining conditions to be confirmed.

## Conditional equipment bonuses, 2026-09-08

The runtime now carries monster `undead`, task `slayerHelmApplies`, and item
`combatBonus`. `PlayerSnapshot.ItemStats` retains melee, ranged and magic
offensive stats independently. RuneLite reads remain on the client thread.

After the compatible Wiki loadout is found, `EquipmentPlanner` compares owned
head/amulet pairs using `ConditionalBonuses`. Only one eligible Slayer/Salve
boost is credited. All other slots stay fixed; mandatory protection and
companion groups must remain satisfied. Original choices with their own set
or effect dependencies are retained. See [the bonus review](combat-bonus-review.md)
for exact rules, source revisions and the limits of the relative stat score.

## Automatic account requirements, 2026-09-08

`AccountProgressCapture` captures quest, diary and supported unlock states on the
client thread. `PlayerSnapshot.accountProgress` is an immutable map with MET,
UNMET and UNKNOWN states and participates in snapshot equality. Manual account
confirmations remain separate; observations are refreshed after varbit/stat
changes and are cleared from snapshots when logged out.

`RequirementEvaluator.assess` is the common status API for loadout eligibility,
supply/equipment exemptions and the Account requirements panel. Known unmet or
unavailable facts cannot be bypassed with a saved manual confirmation. Whole
quest/diary names resolve before AND/OR expressions. Unsupported remainders can
still be manually confirmed after all known conditions are met.

The panel lists unmet or unresolved Slayer/master/item requirements with current
base levels. Satisfied requirements are hidden, whether verified automatically
or confirmed manually; an empty list displays an all-satisfied message.

## Wilderness death risk

The planner now includes exact-item Wilderness loss rules and a configurable
trip budget. See [runtime and verification](wilderness-risk-implementation.md)
and [pinned source evidence](wilderness-risk-sources.md) for calculations,
client capture, selection, limits and manual verification status.

## Compact advisor panel, 2026-09-08

Active task and Catalogue each retain a Setup, Checks, or Guide view. Setup
shows compact equipment/inventory sprites, a packing list, and concise readiness
and Wilderness risk. Checks holds unresolved selected-setup requirements,
preparation issues, account confirmations, and complete planned/carried death
scenarios. Unused equipment alternatives do not contribute to its count; their
requirements remain in the method guide. Guide opens with the selected method
and retains task/access notes, other setups, equipment reasoning, and sources.

Change setup contains location, method, master, ranking, and Wilderness/group
controls. Search and monster selection remain visible. Refresh and Copy setup
stay outside the scrolling content; copying is disabled for blocked setups.
Details preserve scroll position when expanded, and item tooltips retain exact
names, origins, quantities, and carried/withdraw/missing counts.

Verified by Gradle build and manual plugin-panel checks with a captured bank:
active Skeletons, Catalogue Spiders/Spindel, Checks, Guide, packing list, and Copy
setup. Final spacing and disclosure adjustments were also checked in Catalogue
while logged out. No game actions or automated tests were used.

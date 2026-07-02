# Greater Demons And K'ril Loadout Design

Date: 2026-07-02

## Problem

The Greater demons task is broad: regular Greater demons, Wilderness variants,
Catacombs variants, Skotizo, Tormented demons, Tstanon Karlak, and K'ril
Tsutsaroth all count for the task. The source data already represents most of
that surface, but the K'ril loadout behavior is weaker than the strategy data.

The current runtime strategy contract is style-level:

- `plugin.primaryStyle` sets the default method for a strategy variant.
- `plugin.primaryWeapons` can override the weapon slot for that style.
- `plugin.secondaryWeapons` can override the weapon slot when the user manually
  selects that secondary style.

K'ril's rich strategy JSON includes solo Scorching bow, solo Shadow, melee tank,
Protect from Magic tank, and team attacker methods. Runtime loadout only sees
the compact plugin fields, so K'ril defaults to melee because
`plugin.primaryStyle` is `MELEE`. Scorching bow is respected only after the user
selects `RANGED`; the plugin does not treat the Scorching bow solo method as a
first-class K'ril loadout path.

OSRS Wiki evidence checked during brainstorming:

- `Slayer task/Greater demons`: page id `298108`, revision `15242589`,
  revision timestamp `2026-06-30T06:39:10Z`.
- `K'ril Tsutsaroth/Strategies`: page id `24889`, revision `15226847`,
  revision timestamp `2026-06-06T03:08:41Z`.
- `Greater demon/Strategies`: missing page, which is expected; regular Greater
  demon strategy context lives on the monster and Slayer task pages.

## Goals

- Shore up Greater demons source data: variants, location rows, task notes,
  strategy links, and source coverage should reflect the current OSRS Wiki
  pages.
- Make K'ril strategy loadouts method-aware enough that the Scorching bow solo
  strategy is respected when the player owns Scorching bow.
- Preserve bank-aware behavior: never recommend an unowned strategy weapon.
- Preserve passive plugin compliance: read game state and advise only; no game
  actions, input generation, live prayer prompts, tile prompts, or runtime HTTP.
- Keep existing no-strategy and simple-strategy variants behaviorally stable.

## Non-Goals

- Do not build full boss rotation assistance, prayer switching prompts, tile
  guidance, or live in-room tactical calls.
- Do not implement exact K'ril spec-energy, bind-duration, or tick-cycle
  simulation.
- Do not move editable Slayer knowledge into generated `build/` resources.
- Do not replace the stat-driven gear selector with hand-authored complete gear
  sets.

## Recommended Approach

Add optional runtime strategy loadout profiles. These profiles sit below
`MonsterStrategy` and represent the strategy's loadout paths directly:

```text
MonsterStrategy
  primaryStyle
  primaryWeapons
  secondaryWeapons
  note
  sourceUrl
  profiles[]

StrategyProfile
  profileId
  label
  combatStyle
  role
  weapons[]
  notes[]
```

Profiles are optional. Existing strategies without profiles continue through
the current primary/secondary weapon override path. Rich boss strategies can add
profiles without forcing every task family to author them immediately.

K'ril should define at least these runtime profiles:

- `solo-scorching-bow`: `RANGED`, role `solo`, weapons `scorching-bow`, then
  wiki-backed lower-priority ranged options such as `twisted-bow` and
  `bow-of-faerdhinen` when those weapon IDs are present in the source weapon
  catalog.
- `solo-melee`: `MELEE`, role `solo`, weapons `emberlight`,
  `osmumten-s-fang`, `arclight`.
- `melee-tank`: `MELEE`, role `tank`, weapons `emberlight`, `arclight`,
  `zamorakian-hasta`, and other wiki-backed tank options.
- `team-attacker`: `MELEE`, role `attacker`, weapons `emberlight`, `arclight`,
  `osmumten-s-fang`, and other wiki-backed attacker options.
- `solo-shadow-5-0`: `MAGIC`, role `solo`, weapons `tumeken-s-shadow`.

The advisor resolves an active profile before gear selection. For K'ril, when
the user has not explicitly selected a combat method, the default profile order
is `solo-scorching-bow`, `solo-melee`, `melee-tank`, `team-attacker`, then
`solo-shadow-5-0`. The first viable owned profile wins. This makes Scorching
bow the default K'ril path when owned, while avoiding a high-effort Shadow 5:0
default unless earlier profiles are not viable. If no profile is viable, the
advisor falls back to the existing strategy weapon path and then the stat
engine.

Manual method selection remains respected. If the user selects `MELEE`, the
advisor considers only melee profiles. If the user selects `RANGED`, it
considers only ranged profiles. If the selected method has no viable profile,
the advisor continues through the existing strategy weapon path and then the
stat engine for that selected style; it must not silently switch to another
style.

## Alternatives Considered

### Change K'ril `primaryStyle` to `RANGED`

This is small, but too blunt. It would make Scorching bow more visible while
turning a multi-method boss strategy into one global default. It also leaves
melee tank, team attacker, and Shadow method data source-only.

### Keep Current Behavior And Document The Ranged Toggle

This is lowest risk, but it does not address the reported problem. The data
contains a K'ril Scorching bow strategy, yet the loadout does not surface it as
the natural K'ril path.

### Author Complete Fixed Gear Sets

This would better mirror wiki equipment tables, but it conflicts with the
project's stat-driven, owned-item loadout model. It is also too high-maintenance
for a broad Slayer plugin where bank-aware partial ownership matters.

## Data Flow

1. Source JSON stores strategy profiles under
   `src/main/data/slayer/strategies/<strategy-id>/strategy.json`.
2. `ModularSlayerDataCompiler` validates profile IDs, combat styles, and weapon
   references against `weapons/*.json`.
3. The compiler emits profile weapon item IDs into the runtime `MonsterStrategy`
   nested under each `MonsterVariant`.
4. `LoadoutAdvisor` resolves the selected variant, effective method, effective
   location, and strategy profile.
5. `GearSelector` receives the selected profile's ordered weapon IDs as the
   strategy override list.
6. `Recommendation` carries the active strategy profile label/notes so the UI
   can explain why the loadout differs from a generic stat pick.

## UI Behavior

The panel should show the active profile when a profile drove the loadout, for
example `K'ril strategy: Solo Scorching bow bind`. The existing wiki strategy
note remains useful, but it is not specific enough to explain the active
loadout by itself.

The attack-method selector must reflect the recommendation's effective style.
For K'ril, if no method is manually selected and the Scorching bow profile wins,
the method should render as `RANGED`. If the player picks melee, the UI should
render and keep the melee choice.

No visual companion is needed for this design; the decision is data/runtime
behavior, not layout.

## Greater Demon Data Audit Scope

The implementation should audit and adjust source data for:

- Greater demons task assignment, extension, and per-master metadata.
- Regular Greater demon variants and their locations.
- Catacombs variants and shard/totem notes.
- Wilderness Slayer Cave, Demonic Ruins, Lava Maze Dungeon, and Wilderness risk
  notes.
- Chasm of Fire task-only, cannon, and contract notes.
- K'ril Tsutsaroth and Tstanon Karlak as Greater demon task-counting variants.
- Skotizo and Tormented Demon task-counting alternatives.
- Strategy links for every assigned Greater demon variant.
- K'ril method/profile coverage for solo ranged, solo melee, magic, tank, and
  team attacker options.

## Error Handling And Fallbacks

- Missing profiles: preserve current primary/secondary strategy behavior.
- Malformed profiles: treat that profile as unavailable, warn or validate at
  compile time where possible, and do not fabricate loadout advice.
- Unowned profile weapons: do not recommend them; proceed to the next viable
  profile or the existing fallback.
- Unknown weapon IDs in source: fail modular data validation.
- Unsupported selected method: keep the user's selected style, try the existing
  strategy/stat fallback path for that style, and use current empty-loadout
  behavior if no owned gear can support it.

## Tests And Verification

Implementation should use TDD for behavior changes:

- Add a failing `LoadoutAdvisorTest` proving K'ril defaults to `RANGED` and
  equips Scorching bow when the player owns it.
- Add fallback coverage proving K'ril defaults or falls back to melee when
  Scorching bow is not owned but melee strategy weapons are.
- Add selected-method coverage proving an explicit melee choice does not get
  overridden by the Scorching bow profile.
- Add compiler/source tests for strategy profile parsing and weapon reference
  validation.
- Extend Greater demons source coverage for the wiki-backed task rows,
  variants, locations, and K'ril profile data.
- Validate changed JSON with `jq empty`.
- Run
  `./gradlew test --tests 'com.danieljglover.allinslayer.data.source.*'`.
- Run broader loadout/compiler tests touched by implementation. If shared
  model/compiler behavior changes are broad, run `./gradlew cleanTest test`
  before claiming completion.

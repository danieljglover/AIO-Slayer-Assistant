# All-In Slayer Side Panel Design And Functional Spec

- Status: Approved design
- Date: 2026-06-28
- Scope: Redesign the RuneLite side panel for All-In Slayer
- Implementation status: Not implemented in this document

## Goal

Replace the current flat side panel with a RuneLite-native task dashboard that is useful while playing and also makes refresh failures diagnosable. The panel must show the current Slayer task, remaining count, task intel, loadout advice, and refresh/debug state in a compact, predictable layout.

This design is based on official RuneLite side panel patterns from `PluginPanel`, `PluginErrorPanel`, `MaterialTabGroup`, `LootTrackerPanel`, `TimeTrackingPanel`, `GrandExchangePanel`, `HiscorePanel`, `XpPanel`, and `WorldSwitcherPanel`, plus third-party Plugin Hub panels from Inventory Setups, Quest Helper, and Loot Logger.

## Success Criteria

The side panel redesign is complete when all of these are true:

- The panel uses RuneLite side-panel conventions for layout, colors, empty states, tabs, and rebuilds.
- A player can answer "what is my task?", "how many are left?", "where should I kill them?", and "what loadout should I use?" without reading dense HTML text blocks.
- A player can check an enchanted gem or Slayer helm and see whether the plugin observed the check.
- The panel can show a no-task state without losing the last refresh source and debug snapshot.
- The panel can show a task that exists but has no owned loadout without hiding task intel.
- The panel can show a target id that is not in the bundled data set.
- All UI updates are rendered from a single `SlayerPanelState`.
- Client reads remain on the RuneLite client thread and Swing writes remain on the EDT.
- The implementation has unit tests for state construction, trigger/debug capture, and panel rendering decisions.

## Functional Requirements

### FR-1: Render From One State

`SlayerPanel` must expose one primary render entry point:

```java
void render(SlayerPanelState state)
```

The previous split between `showNoTask()` and `update(...)` should be removed or made private compatibility helpers during migration. Public callers should not mutate individual labels. This prevents no-task rendering from dropping useful event/debug context.

### FR-2: Preserve Refresh Source

Every path that causes recompute must pass a `RefreshSource` into state construction. The panel must display that source in the header and in the Debug tab.

Required mappings:

- plugin startup -> `STARTUP`
- manual refresh button -> `MANUAL`
- Slayer varp or varbit change -> `VARBIT`
- Slayer task chat/dialog message -> `CHAT`
- enchanted gem or Slayer helm check menu click -> `MENU_CHECK`
- bank, inventory, or worn item container update -> `ITEM_CONTAINER`
- game state change -> `GAME_STATE`
- DPS/Cost toggle -> `MODE_TOGGLE`

### FR-3: Capture Menu Check Diagnostics

When `onMenuOptionClicked` runs, the plugin must capture these fields before scheduling recompute:

- menu option
- menu action
- raw resolved item id
- mapped item id from `ItemVariationMapping.map`
- whether the click matched a task-check item

The Debug tab must show those fields even when the click does not match. That makes false negatives visible.

### FR-4: Show Raw Slayer State

Each recompute must capture raw Slayer-related values:

- target varp
- count varp
- area varp
- boss target varbit when available

The Debug tab must show these values. If a value cannot be read safely, use `-1`.

### FR-5: Distinguish Detector Results

The state must distinguish:

- no assigned task because target varp is `0` or less
- assigned target id exists but no `TaskData` matches
- supported task resolved

Do not collapse unsupported task and no task into the same UI state.

### FR-6: Keep Task Intel Visible Without Loadout

If a supported task resolves but `LoadoutAdvisor` returns empty, the `Task` tab must still render task intel. The `Loadout` tab should show a no-loadout empty state.

### FR-7: Manual Refresh

The action row must include a manual refresh control. Activating it must schedule recompute with `RefreshSource.MANUAL`.

### FR-8: Mode Toggle

The DPS/Cost mode control must:

- show the current mode
- toggle the mode
- schedule recompute with `RefreshSource.MODE_TOGGLE`
- keep the selected tab unchanged after render

### FR-9: Export Control

The export control must be enabled only when `SlayerPanelState.recommendation` is non-null. Activating it must export the current recommendation and setup name. It must not throw or visually appear enabled in no-task, unsupported-task, or no-loadout states.

### FR-10: Stable Layout

The header, action row, and tab strip must keep stable dimensions across state changes. Long task names and item names must wrap or truncate cleanly inside the RuneLite panel width instead of resizing the panel.

### FR-11: No Sensitive Or Network Data

The panel must not display account credentials, session data, player profile file paths, or any network-derived data beyond data already available through RuneLite APIs and bundled plugin data.

## Current Problem

`SlayerPanel` currently renders three labels and two text buttons:

- `taskTitle`
- `taskInfo`
- `loadoutInfo`
- `Mode: DPS`
- `Export to Inventory Setups`

That layout hides important state. If a Slayer helm or enchanted gem check fires but the detector returns stale data, the panel gives no evidence of what happened. The user only sees that the side menu did not update.

The plugin already has useful refresh sources:

- startup recompute
- Slayer varp/varbit changes
- Slayer chat/dialog messages
- menu click checks for enchanted gem and Slayer helms
- inventory, worn, and bank container updates
- game state changes

The redesigned panel must surface these sources instead of treating recompute as an invisible side effect.

## RuneLite Panel Patterns To Follow

Official RuneLite panels use a consistent structure:

- `PluginPanel` provides the fixed side-panel width and scroll wrapping.
- `ColorScheme.DARK_GRAY_COLOR` and `ColorScheme.DARKER_GRAY_COLOR` provide the standard panel palette.
- `PluginErrorPanel` is used for empty or unavailable states rather than blank labels.
- Rich panels use fixed action/header rows plus scrollable content sections.
- `MaterialTabGroup` and `MaterialTab` are used where the panel has distinct workflows.
- Rebuilds are explicit: containers are cleared, rebuilt, then `revalidate()` and `repaint()` are called.
- Icon-only controls and compact action rows are preferred over large text buttons where the action is familiar.
- Expensive RuneLite client reads happen on the client thread; Swing mutation happens on the EDT.

The side panel should copy these conventions rather than inventing a new Swing style.

## Plugin Hub Panel Patterns Reviewed

Third-party Plugin Hub panels follow the same base conventions but show how larger external plugins organize richer workflows.

### Inventory Setups

Plugin Hub manifest reviewed:

- `plugins/inventory-setups`
- repository `https://github.com/dillydill123/inventory-setups.git`
- manifest commit `afd6e45adb24a90d3411c9760b84d2b43bfe7489`

Relevant classes:

- `InventorySetupsPluginPanel`
- `InventorySetupsInventoryPanel`
- `InventorySetupsEquipmentPanel`
- `InventorySetupsNameActions`

Layout lessons:

- Use `super(false)` when the panel needs to own its own scroll behavior.
- Keep a `northAnchoredPanel` for title, action icons, and search so important controls do not scroll away.
- Put the main content in a `JScrollPane` with horizontal scrolling disabled.
- Use separate top bars for overview vs detail mode, then toggle visibility instead of rebuilding the entire root panel.
- Use icon controls with hover icons and tooltips for common actions.
- Use `PluginErrorPanel` for empty states.
- Preserve scroll position when moving between overview and detail views.
- Use structured inventory/equipment slot grids for item-heavy data instead of long text.

All-In Slayer implication:

- Keep the header and actions anchored above the tab content.
- Do not put task/loadout/debug data into one scrolling HTML label.
- Preserve selected tab and scroll position across recomputes.
- Item-heavy loadout content should use rows or grids with stable dimensions.

### Quest Helper

Plugin Hub manifest reviewed:

- `plugins/quest-helper`
- repository `https://github.com/Zoinkwiz/quest-helper.git`
- manifest commit `0a43adc5f77de886e4e3cfefa6d26adfa6ff5cd3`

Relevant classes:

- `QuestHelperPanel`
- `QuestOverviewPanel`
- `QuestStepPanel`
- `QuestSectionSection`
- `SkillFilterPanel`
- `RegionFilterPanel`

Layout lessons:

- Use `CardLayout` when switching between major panel modes such as list, detail, and settings.
- Use `IconTextField` for filter/search workflows.
- Keep a top intro/search/filter area separate from the scrollable content viewport.
- Use collapsible sections for long step lists.
- Highlight the active/current section with RuneLite brand color while leaving inactive sections subdued.
- Use small icon buttons with `SwingUtil.removeButtonDecorations`, hover colors, and tooltips.
- After dynamic changes, call `revalidate()` and `repaint()`.

All-In Slayer implication:

- The approved three-tab design is still appropriate because All-In Slayer has three stable workflows, not a large searchable list.
- If later versions add task/master browsing, use Quest Helper's list/detail/search pattern instead of overloading the task dashboard.
- Use color only for real state, such as current task status or warnings; avoid decorative color.
- Collapsible sections are useful for future long location lists or detailed gear alternatives, but are not required for v1.

### Loot Logger

Plugin Hub manifest reviewed:

- `plugins/loot-logger`
- repository `https://github.com/TheStonedTurtle/Loot-Logger.git`
- manifest commit `89343b984426f6beddafb38c766469b6972e1ebd`

Relevant classes:

- `LootLoggerPanel`
- `SelectionPanel`
- `LootPanel`
- `LootGrid`
- `NamedLootGrid`
- `UniqueItemPanel`

Layout lessons:

- Use `PluginPanel` with `ColorScheme` and wrapped scroll containers.
- Use `PluginErrorPanel` for "nothing selected/no data" states.
- Use `MaterialTabGroup` for category selection.
- Use `IconTextField` for searchable selection lists.
- Use fixed item grids for loot/item-heavy content.
- Keep category selection and content display distinct.

All-In Slayer implication:

- `MaterialTabGroup` remains a good fit for `Task`, `Loadout`, and `Debug`.
- The loadout tab should be item-grid/row oriented, not prose-heavy.
- The Debug tab should remain a distinct content category so it does not crowd the player-facing task advice.

### Bank Tag Layouts

Plugin Hub manifest reviewed:

- `plugins/bank-tag-layouts`
- repository `https://github.com/geheur/bank-tag-custom-layouts.git`
- manifest commit `a904d95d242cc62f9f2e028399287bea72c01f05`

This plugin does not primarily use a RuneLite side-panel popout. It is more relevant as an example of in-bank widget/UI manipulation than side-panel layout, so it should not influence the All-In Slayer side panel design.

### Reviewed But Not Used

Shortest Path was also checked at Plugin Hub manifest commit `f3a579496dda48468212489ec0b37db91bda5f44`. It did not expose a relevant `PluginPanel` side-panel layout for this design pass.

## Recommended Design

Build a tabbed task dashboard with a fixed header, compact action row, and three content tabs:

1. `Task`
2. `Loadout`
3. `Debug`

The `Debug` tab is part of the product, not a temporary hack. All-In Slayer depends on game-state events and external client data; when something does not update, the panel should show whether the plugin saw the event and what data it resolved.

## User Workflows

### Workflow A: Starting With No Task

1. Player opens RuneLite and opens the All-In Slayer side panel.
2. Plugin computes state from startup or game state.
3. Header shows "No task detected", last refresh source, and update time.
4. `Task` and `Loadout` tabs show empty states.
5. `Debug` tab shows raw target/count values and detector result.

### Workflow B: Checking Slayer Helm Or Enchanted Gem

1. Player chooses `Check` on an enchanted gem or Slayer helm variant.
2. Plugin captures menu diagnostics.
3. If the click matches, recompute runs with `MENU_CHECK`.
4. Header updates last source/time.
5. Debug tab shows raw item id, mapped item id, match result, and current Slayer varps.
6. If task resolution still fails, the panel shows whether the event was seen and whether the varp/data lookup failed.

### Workflow C: Reviewing Task Advice

1. Player opens the `Task` tab.
2. Panel shows task name, remaining count, Slayer level, weakness, required item, recommended location, method, and known locations.
3. If no recommendation exists, task intel remains visible.

### Workflow D: Choosing Gear

1. Player opens the `Loadout` tab.
2. Panel shows current mode, selected style, worn gear rows, inventory items, DPS, gear cost, missing upgrades, and bank age.
3. Player toggles DPS/Cost.
4. Panel recomputes and keeps the player on the `Loadout` tab.

### Workflow E: Exporting A Setup

1. Player has a supported task with a recommendation.
2. Export control is enabled.
3. Player activates export.
4. Plugin copies the Inventory Setups import string for the current recommendation.
5. Panel state is not cleared or re-rendered solely because export happened.

## Layout

### Header

The top of the panel is always visible above the tabs.

Content:

- task name, or "No task detected"
- remaining count when known
- last updated time
- last refresh source
- small status text for unsupported/no-loadout states

Example states:

- `Abyssal demons`
- `Remaining: 142`
- `Updated: 21:48:12 from helm check`
- `Bank: 12m ago`

When there is no task, the header should still show the last refresh source and time. This matters because a helm check can be observed even when task resolution fails.

### Action Row

Use compact controls:

- refresh button: manually triggers recompute
- DPS/Cost toggle: switches recommendation mode
- export button: copies the Inventory Setups import string when a recommendation exists

If icon resources are not available yet, text labels are acceptable for the first implementation, but the layout should reserve stable dimensions so controls do not resize the panel.

### Tabs

Use `MaterialTabGroup` with text tabs for first implementation:

- `Task`
- `Loadout`
- `Debug`

Icon tabs can be added later if suitable resources exist. Text tabs are clearer during the current debugging phase.

## Task Tab

Purpose: answer "what is my task and how should I do it?"

Sections:

- task summary: Slayer level, remaining count, amount range if available
- weakness: combat style and element
- required item: facemask, rock hammer, nose peg, mirror shield, or none
- location recommendation: selected location plus cannon/burst/multi flags
- method: short recommendation from the selected `Recommendation` or `TaskData`
- task locations: list all known locations when space allows

Empty states:

- no task detected: `PluginErrorPanel` with "No Slayer task detected"
- unsupported task: show the target id and explain that the task is not in the bundled dataset yet

## Loadout Tab

Purpose: answer "what gear should I use from what I own?"

Sections:

- selected mode: DPS or Cost
- selected style: Melee, Ranged, or Magic
- worn gear rows grouped by equipment slot
- inventory items from the recommendation
- estimated DPS
- total gear cost
- missing upgrades
- bank snapshot age

Rows should be structured Swing components, not a single large HTML label. Each row should have:

- left label: slot or category
- right value: item name
- optional item icon when available from `ItemManager`

Empty states:

- no recommendation: `PluginErrorPanel` with "No owned loadout found"
- no bank snapshot: show a non-blocking warning that advice uses inventory and worn gear only

## Debug Tab

Purpose: make side-panel update bugs visible in-game.

Show a compact diagnostic snapshot:

- last refresh source
- last refresh timestamp
- last menu option
- last menu action
- raw clicked item id
- mapped clicked item id
- whether the click matched a Slayer check item
- current Slayer target varp
- current Slayer count varp
- current boss target varbit, using `-1` when unavailable
- detector result: task name, unsupported target id, or no task
- last recommendation result: style or no loadout

The Debug tab should not log sensitive data. It should only display local game/client state needed to diagnose the plugin.

## State Model

Introduce a single immutable panel state object named:

```java
SlayerPanelState
```

Fields:

- `TaskData task`, nullable only for no-task and unsupported-task states
- `Recommendation recommendation`, nullable when no owned loadout exists
- `int remaining`, `0` when no task or unknown
- `AdviceMode mode`, never null
- `String bankAge`, nullable when no bank snapshot has been seen
- `Map<Integer, String> itemNames`, never null, may be empty
- `RefreshSource refreshSource`, never null
- `Instant updatedAt`, never null
- `PanelStatus status`, never null
- `String statusMessage`, never null, user-facing short text
- `SlayerDebugSnapshot debug`, never null

Supporting objects:

```java
enum RefreshSource
{
    STARTUP,
    MANUAL,
    VARBIT,
    CHAT,
    MENU_CHECK,
    ITEM_CONTAINER,
    GAME_STATE,
    MODE_TOGGLE
}
```

```java
enum PanelStatus
{
    NO_TASK,
    UNSUPPORTED_TASK,
    TASK_WITH_LOADOUT,
    TASK_WITHOUT_LOADOUT
}
```

```java
SlayerDebugSnapshot
```

Fields:

- `String menuOption`
- `MenuAction menuAction`
- `int rawItemId`, using `-1` when unavailable
- `int mappedItemId`, using `-1` when unavailable
- `boolean matchedTaskCheck`
- `int slayerTargetVarp`
- `int slayerCountVarp`
- `int slayerAreaVarp`
- `int bossTargetVarbit`, using `-1` when unavailable
- `String detectorResult`
- `String unsupportedTargetId`, empty unless status is `UNSUPPORTED_TASK`

The panel renders from `SlayerPanelState` only. The plugin computes the state on the client thread, resolves item names there, then sends the finished state to Swing on the EDT.

## State Construction Rules

State construction should be isolated in a small builder or factory method, not spread through Swing code. The builder runs on the client thread and must follow these rules:

- If target varp is `0` or less, set `status = NO_TASK`, `task = null`, and `recommendation = null`.
- If target varp is positive but `SlayerDataService.byTargetVarp` returns empty, set `status = UNSUPPORTED_TASK`, `task = null`, and store the target id in the debug snapshot.
- If a supported task exists and a recommendation exists, set `status = TASK_WITH_LOADOUT`.
- If a supported task exists but no recommendation exists, set `status = TASK_WITHOUT_LOADOUT`.
- `updatedAt` is the time the state object is built.
- `remaining` is copied from `TaskDetector` or the raw count varp for all statuses.
- `itemNames` includes every item id the UI will render from `Recommendation`; missing names fall back to `Item <id>`.
- Debug fields reflect the latest relevant event and raw Slayer state at recompute time.

## Data Flow

Each event handler records its refresh source and any relevant debug fields, then schedules recompute:

- `onVarbitChanged` -> `RefreshSource.VARBIT`
- `onChatMessage` -> `RefreshSource.CHAT`
- `onMenuOptionClicked` -> `RefreshSource.MENU_CHECK`
- `onItemContainerChanged` -> `RefreshSource.ITEM_CONTAINER`
- `onGameStateChanged` -> `RefreshSource.GAME_STATE`
- manual refresh button -> `RefreshSource.MANUAL`
- mode toggle -> `RefreshSource.MODE_TOGGLE`
- startup -> `RefreshSource.STARTUP`

Recompute flow:

1. Read Slayer varps and current task on the client thread.
2. Read owned items and player stats.
3. Compute recommendation.
4. Resolve item names and optional icons on the client thread.
5. Build `SlayerPanelState`.
6. Call `panel.render(state)` on the EDT.
7. Update overlay from the same state.

This avoids the current split where `showNoTask()` discards context that would explain why a refresh did not change the visible task.

## Panel State Matrix

| Status | Header | Task Tab | Loadout Tab | Debug Tab | Export |
| --- | --- | --- | --- | --- | --- |
| `NO_TASK` | "No task detected", source/time | `PluginErrorPanel` no-task message | `PluginErrorPanel` no-loadout message | raw varps and detector result | disabled |
| `UNSUPPORTED_TASK` | "Unsupported task", target id, source/time | unsupported target message | `PluginErrorPanel` no-loadout message | target id, raw varps, detector result | disabled |
| `TASK_WITHOUT_LOADOUT` | task name, remaining, source/time | full task intel | `PluginErrorPanel` no owned loadout message | task name and no-loadout result | disabled |
| `TASK_WITH_LOADOUT` | task name, remaining, source/time | full task intel | full recommendation | task name and recommendation style | enabled |

The selected tab should not reset during re-render unless the selected tab no longer exists. In this design, all three tabs always exist.

## Swing Structure

`SlayerPanel` should own small reusable component sections:

- `HeaderPanel`
- `ActionPanel`
- `TaskTabPanel`
- `LoadoutTabPanel`
- `DebugTabPanel`
- `KeyValueRow`

These can be package-private classes or private inner classes depending on size. If `SlayerPanel` grows past a readable size, split them into separate files under `ui`.

Panel construction:

- use `BorderLayout` at the root
- use `ColorScheme.DARK_GRAY_COLOR` for the panel background
- use `ColorScheme.DARKER_GRAY_COLOR` for header/action/content bands
- use `EmptyBorder(6, 6, 6, 6)` or matching RuneLite spacing
- use stable row heights where possible
- avoid nested card styling; use compact bands and rows

Panel rendering:

- `render(SlayerPanelState state)` is the only public update method
- render methods assert or document that they run on the EDT
- each tab panel updates from the same state
- empty states use `PluginErrorPanel`
- after structural changes, call `revalidate()` and `repaint()`

## Component Responsibilities

### `SlayerPanel`

Owns the root `PluginPanel`, tab group, high-level render method, and callbacks.

Public surface:

```java
void render(SlayerPanelState state)
void setOnRefresh(Runnable callback)
void setOnToggleMode(Runnable callback)
void setOnExport(Runnable callback)
```

Responsibilities:

- construct header, actions, tabs, and tab display container
- keep current selected tab stable across render calls
- delegate state rendering to child sections
- enable or disable export based on state status
- avoid direct RuneLite client reads

### `HeaderPanel`

Renders current task identity and update metadata.

Responsibilities:

- display task name or status title
- display remaining count where meaningful
- display refresh source and update time
- display bank age or short warning text
- avoid tall multi-line growth beyond the intended header footprint

### `ActionPanel`

Renders refresh, mode, and export controls.

Responsibilities:

- bind callbacks
- display current mode
- keep button dimensions stable
- expose disabled export state clearly

### `TaskTabPanel`

Renders task intel.

Responsibilities:

- show `PluginErrorPanel` for no-task and unsupported-task states
- render supported task details independently from loadout availability
- render selected recommendation location/method when present
- fall back to `TaskData.recommendedMethod` when no recommendation exists

### `LoadoutTabPanel`

Renders recommendation details.

Responsibilities:

- show `PluginErrorPanel` when no recommendation exists
- render gear rows from `Recommendation.worn`
- render inventory items from `Recommendation.inventory`
- render DPS, cost, missing upgrades, and bank age
- use `itemNames` lookup with `Item <id>` fallback

### `DebugTabPanel`

Renders diagnostic data.

Responsibilities:

- always render, regardless of panel status
- show latest refresh source/time
- show menu click diagnostics
- show raw Slayer varps
- show detector result and unsupported target id
- avoid sensitive/account/session data

### `KeyValueRow`

Reusable row component for dense panel data.

Responsibilities:

- left aligned key label
- right aligned or wrapping value label
- optional icon slot
- stable height where possible
- no direct state mutation outside render/update methods

## Plugin Callback Contract

`AllInSlayerPlugin` owns game state and panel callbacks.

Callbacks:

- `onRefresh` schedules `recompute(RefreshSource.MANUAL)`.
- `onToggleMode` flips the in-memory `AdviceMode` for the current session and schedules `recompute(RefreshSource.MODE_TOGGLE)`. Persisting the changed mode to RuneLite config is allowed but not required for this redesign.
- `onExport` exports only the latest non-null recommendation.

Event handlers should not call `panel.render(...)` directly. They should record refresh/debug context and call recompute. Recompute builds one complete state and then renders it.

The overlay should be updated from the same `SlayerPanelState` so panel and overlay cannot disagree on task name, remaining count, or method.

## Error Handling

The panel must distinguish these states:

- no current Slayer task
- task target id exists but is not in the data service
- task exists but no owned loadout was found
- recommendation exists but bank snapshot is missing or stale
- item names are unavailable and fall back to `Item <id>`

These states should be visible in the UI and, where relevant, the Debug tab.

## Interaction Details

### Refresh Button

- Enabled in every panel status.
- Calls plugin callback `onRefresh`.
- Plugin schedules recompute with `RefreshSource.MANUAL`.
- Does not change the selected tab.

### DPS/Cost Toggle

- Enabled in every status because it changes the next recommendation.
- Shows the active mode in the control text or selected state.
- Calls plugin callback `onToggleMode`.
- Plugin updates mode and schedules recompute with `RefreshSource.MODE_TOGGLE`.
- Does not change the selected tab.

### Export Button

- Enabled only for `TASK_WITH_LOADOUT`.
- Calls plugin callback `onExport`.
- Does not trigger recompute.
- If the recommendation disappears before the callback runs, plugin should no-op.

### Tab Selection

- `Task` is the default tab on first panel construction.
- User selection persists across renders.
- `Debug` is always visible; do not hide it behind a developer config while the helm/gem update issue is being diagnosed.

### Empty State Navigation

- Empty states should occupy the tab content area only.
- Header and action controls stay visible in empty states.
- Debug tab stays available in every status.

## Testing

Unit tests should cover state and rendering decisions without requiring a live client:

- `SlayerPanelStateTest`: creates states for no task, unsupported task, valid task, no loadout, and valid loadout.
- `SlayerPanelTest`: verifies render updates header text, tab contents, button enabled state, and debug fields.
- `TaskRefreshTriggerTest`: keep existing menu/varbit/chat trigger tests.
- `MenuClickedItemResolverTest`: keep existing widget item resolution tests.
- `AllInSlayerPlugin` state builder tests where feasible: verify each refresh source is preserved through recompute.

Minimum automated acceptance coverage:

- no-task state renders source/time and leaves export disabled
- unsupported target state displays target id and leaves export disabled
- supported task without recommendation keeps task intel visible
- supported task with recommendation enables export and renders gear rows
- menu check diagnostics render raw id, mapped id, action, option, and match result
- mode toggle changes source to `MODE_TOGGLE`
- manual refresh changes source to `MANUAL`
- selected tab survives a render
- long task and item names do not change preferred panel width

Manual verification in RuneLite:

1. Launch the dev client.
2. Open the All-In Slayer panel.
3. Confirm no-task state shows refresh source/time.
4. Check enchanted gem or Slayer helm.
5. Confirm Debug tab shows the menu option/action, raw item id, mapped item id, match result, target varp, and count varp.
6. Confirm Task and Loadout tabs update from the same state.
7. Toggle DPS/Cost and confirm the header/debug source changes to mode toggle.
8. Export only enables when a recommendation exists.

Manual acceptance criteria:

- Checking the Slayer helm or enchanted gem visibly changes last refresh source/time to menu check when the click is recognized.
- If the task panel still does not update, Debug tab shows whether raw target/count varps changed.
- Opening and closing the bank updates bank age and can change loadout advice without changing selected tab.
- Switching DPS/Cost changes the recommendation or explicitly keeps the same recommendation when both modes choose the same gear.
- No task, unsupported task, no-loadout, and valid-loadout states are visually distinct.

## Non-Goals

This redesign does not add automation, menu actions, input generation, live prayer advice, live movement advice, or network calls. It is a passive RuneLite UI and diagnostic redesign.

This redesign does not require solving the exact Slayer helm event issue before implementation. It makes that issue observable, which is the right next step because the current UI hides the failure mode.

## Implementation Notes

The first implementation can keep the existing event handlers and recommendation code. The main change is to route every update through a panel state object and replace the flat label panel with structured RuneLite-style components.

Recommended implementation order:

1. Add `RefreshSource`, `SlayerDebugSnapshot`, and `SlayerPanelState`.
2. Add tests for state construction and debug snapshots.
3. Refactor `AllInSlayerPlugin.recompute` to accept a refresh source.
4. Capture menu click debug fields in `onMenuOptionClicked`.
5. Replace `SlayerPanel.update(...)` and `showNoTask()` with `render(SlayerPanelState)`.
6. Build the header, actions, and tabs.
7. Wire manual refresh and mode toggle through refresh sources.
8. Run unit tests and manually verify in RuneLite.

## References

- RuneLite `PluginPanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/PluginPanel.java
- RuneLite `PluginErrorPanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/components/PluginErrorPanel.java
- RuneLite `MaterialTabGroup`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/components/materialtabs/MaterialTabGroup.java
- RuneLite `MaterialTab`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/components/materialtabs/MaterialTab.java
- RuneLite `LootTrackerPanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/loottracker/LootTrackerPanel.java
- RuneLite `TimeTrackingPanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/TimeTrackingPanel.java
- RuneLite `GrandExchangePanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/grandexchange/GrandExchangePanel.java
- RuneLite `HiscorePanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/hiscore/HiscorePanel.java
- RuneLite `XpPanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/xptracker/XpPanel.java
- RuneLite `WorldSwitcherPanel`: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/worldhopper/WorldSwitcherPanel.java
- Plugin Hub `inventory-setups` manifest: https://github.com/runelite/plugin-hub/blob/master/plugins/inventory-setups
- Inventory Setups `InventorySetupsPluginPanel`: https://github.com/dillydill123/inventory-setups/blob/afd6e45adb24a90d3411c9760b84d2b43bfe7489/src/main/java/inventorysetups/ui/InventorySetupsPluginPanel.java
- Plugin Hub `quest-helper` manifest: https://github.com/runelite/plugin-hub/blob/master/plugins/quest-helper
- Quest Helper `QuestHelperPanel`: https://github.com/Zoinkwiz/quest-helper/blob/0a43adc5f77de886e4e3cfefa6d26adfa6ff5cd3/src/main/java/com/questhelper/panel/QuestHelperPanel.java
- Quest Helper `QuestOverviewPanel`: https://github.com/Zoinkwiz/quest-helper/blob/0a43adc5f77de886e4e3cfefa6d26adfa6ff5cd3/src/main/java/com/questhelper/panel/QuestOverviewPanel.java
- Quest Helper `QuestStepPanel`: https://github.com/Zoinkwiz/quest-helper/blob/0a43adc5f77de886e4e3cfefa6d26adfa6ff5cd3/src/main/java/com/questhelper/panel/queststepsection/QuestStepPanel.java
- Plugin Hub `loot-logger` manifest: https://github.com/runelite/plugin-hub/blob/master/plugins/loot-logger
- Loot Logger `LootLoggerPanel`: https://github.com/TheStonedTurtle/Loot-Logger/blob/89343b984426f6beddafb38c766469b6972e1ebd/src/main/java/thestonedturtle/lootlogger/ui/LootLoggerPanel.java
- Loot Logger `SelectionPanel`: https://github.com/TheStonedTurtle/Loot-Logger/blob/89343b984426f6beddafb38c766469b6972e1ebd/src/main/java/thestonedturtle/lootlogger/ui/SelectionPanel.java
- Plugin Hub `bank-tag-layouts` manifest: https://github.com/runelite/plugin-hub/blob/master/plugins/bank-tag-layouts
- Plugin Hub `shortest-path` manifest: https://github.com/runelite/plugin-hub/blob/master/plugins/shortest-path

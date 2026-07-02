# Side Panel Dashboard Implementation Plan

## Goal

Implement `docs/superpowers/specs/2026-06-28-all-in-slayer-side-panel-design.md` so the RuneLite side panel is driven by one render state, updates when Slayer task state is checked or changed, and exposes task, loadout, and debug views.

## Scope

- Add immutable-ish UI state objects for panel render data, refresh source, panel status, and debug diagnostics.
- Replace the old split `showNoTask()` / `update(...)` panel API with `SlayerPanel.render(SlayerPanelState)`.
- Wire plugin refresh events, manual refresh, mode toggles, and menu-check diagnostics into that state.
- Use RuneLite UI primitives already available in the dependency: `PluginPanel`, `PluginErrorPanel`, `MaterialTabGroup`, `MaterialTab`, and `ColorScheme`.
- Add focused unit tests for state classification, HTML escaping, action-button state, and debug visibility.

## Steps

1. Add failing tests for `SlayerPanelState` factories and `SlayerPanel.render(...)`.
2. Implement `RefreshSource`, `PanelStatus`, `SlayerDebugSnapshot`, and `SlayerPanelState`.
3. Refactor `SlayerPanel` into a tabbed dashboard with header actions, empty states, loadout rows, and debug rows.
4. Refactor `AllInSlayerPlugin` to build `SlayerPanelState` for startup, manual refresh, varbit/chat/menu/item/game-state events, and mode toggles.
5. Run focused tests first, then the full test suite.

## Verification

- `gradle test --tests com.danieljglover.allinslayer.ui.SlayerPanelStateTest`
- `gradle test --tests com.danieljglover.allinslayer.ui.SlayerPanelTest`
- `gradle test`

# All-In Slayer — Side Panel: Round-2 Review (live render)

- Date: 2026-06-29
- Trigger: the redesigned panel now runs in a live client (`round2-current-ui.png`). Seeing it rendered
  surfaced layout bugs the headless unit tests couldn't catch. New review round: research
  10–15 top plugins + the RuneLite core repo for how they handle grids/long-text in the narrow panel,
  and root-cause our defects from the code.
- Inputs: [`render-rootcause.md`](render-rootcause.md) (code root-cause) + [`research-r2-grids-and-layout.md`](research-r2-grids-and-layout.md) (12 plugins + core).

## What the live render showed (3 defects)
1. **Equipment/inventory grid overflows the panel and cells are oversized** — bulky, mostly-empty wells; content clipped off the right edge (stray sprite top-right, truncated "Inve[ntory]" / "Gear…/Upg…").
2. **`Why` / `Method` values clip** instead of wrapping/truncating.
3. **Worn gear shown twice** — the big empty grid *and* the rows below.

## Root cause (verified from bytecode + our code — both reports agree)
The three defects are **interlinked**, tracing to two layout truths the redesign missed:

- **`DynamicGridLayout` scales every cell by `actualWidth/preferredWidth`** (RuneLite core `DynamicGridLayout.java:102-131`), ignoring the 38×34 max. Our grids have no max cap and aren't left-anchored, and `BoxLayout.Y` stretches them to full width → ~65–70px cells (`EquipmentGrid.java:44`, `EquipmentSlotCell.java:43-46`).
- **The scroll view doesn't track the viewport width.** `bodyWrap` is a plain `JPanel` as the `JScrollPane` view (`SlayerPanel.java:121-125`); a non-`Scrollable` view is sized to its **widest child**, not to 225px. With `HORIZONTAL_SCROLLBAR_NEVER` (`:129`), anything past 225 clips silently. The `PluginPanel` 225 clamp is on the panel, not the scroll content.
- **The long `KeyValueRow` value is what inflates the body past 225** (`KeyValueRow.java:38-41,90-94` — width-uncapped single-line `JLabel`), which then makes the grids scale up and the right edge clip. So defect 2 *feeds* defect 1.
- Defect 3 is the Wave-5 "Decision 3: grid + rows"; the grid only *looked* like a void because the stretch ballooned the empty cells.
- **Test gap:** `EquipmentGridTest` asserts cell *preferred* size, never *laid-out* width — which is why the scaling slipped through.

## How the top plugins / core solve it (the fixes we'll adopt)
- **Fixed-cell grids in a non-stretching container** — Inventory Setups uses plain `GridLayout` of fixed-size slots inside a `FlowLayout` wrapper (`InventorySetupsContainerPanel.java:55-76`); Loot Tracker `GridLayout(rows,5,1,1)` with a centered sprite so the cell never scales the icon. → cap `getMaximumSize()==getPreferredSize()` + left-anchor (house idiom), or a `FlowLayout(LEFT)` wrapper.
- **Wrap long prose** — Quest Helper renders every description as a line-wrapping `JTextArea` styled as a label (`JGenerator.makeJTextArea`). → render `Why`/`Method` via our existing `wrappingNote`.
- **Truncate short single-line values** — Loot Tracker uses `minimumSize=(1,h)` + `Box.createHorizontalGlue()` + tooltip (`LootTrackerBox.java:109-124`). → harden `KeyValueRow` generally (protects diagnostics too).
- **Empty slots recede, not void** — Inventory Setups mutes placeholder wells (`DARK_GRAY` vs `DARKER_GRAY`) and keeps the recognisable cross. → keep the grid (Decision 3's "secondary at-a-glance") but mute empty wells.
- **Width-discipline invariant** — because `PluginPanel` never h-scrolls, *no descendant may report width > viewport*; enforce via a viewport-tracking content root (the proper ADR-0004 replacement for the deleted `FixedWidthPanel`).

## Prioritised fix list (round-2 → fix wave W8)
| # | Fix | Source pattern | Effort |
|---|---|---|---|
| F1 | **Cap + left-anchor the grids** (`getMaximumSize()==getPreferredSize()`, `LEFT_ALIGNMENT`) so `DynamicGridLayout` scale=1.0 → 38×34 cells | Inventory Setups; house idiom (`LoadoutItemRow`) | S |
| F2 | **`Why`/`Method` → `wrappingNote`** (wrapping JTextArea) instead of `KeyValueRow` | Quest Helper | S |
| F3 | **Harden `KeyValueRow`** — value `minimumSize=(1,h)` + cap row width to `PANEL_WIDTH` + full-text tooltip | Loot Tracker | S |
| F4 | **Mute empty equipment wells** (darker, border-less) so the cross recedes; keep grid + rows per Decision 3 | Inventory Setups | S |
| F5 | **Structural backstop: viewport-tracking scroll view** — `bodyWrap` implements `Scrollable` (`tracksViewportWidth=true`, height=false) so nothing can silently clip again | Quest Helper `FixedWidthPanel` / core viewport | M |
| F6 | **Close the test gap** — assert laid-out cell width after `setSize/doLayout`, and a panel-level invariant: no `dashboardBody` descendant width > `PANEL_WIDTH` with long Why/Method | render-rootcause §tests | S |

F1+F2+F3+F4 are the quick wins that fix the visible render; F5 is the durable backstop (do alongside); F6 locks it so it can't regress. These honour the confirmed decisions (keep the grid as secondary; brand/colour only for state) and ADR-0004 (no `<html>`, width from layout).

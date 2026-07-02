# Render root-cause - round 2 live client defects

Diagnosis only (no production code changed). Three observed defects in the live render
(`docs/ui-review/round2-current-ui.png`, account with a few items equipped), each root-caused from the
code with the load-bearing Swing facts verified against the actual client jar bytecode
(`net.runelite:client:1.12.31.1`).

## TL;DR

All three render faults trace to **two** layout truths the redesign did not account for:

1. **The scroll view never tracks the viewport width.** `bodyWrap` is a plain `JPanel` used as the
   `JScrollPane` view. A non-`Scrollable` view is sized by the viewport to **its own preferred width**,
   not to the 225px viewport. So the body's width is driven by its **widest child** with no clamp to
   `PANEL_WIDTH`, and anything past 225 is clipped on the right (horizontal scrollbar = NEVER). The
   `PluginPanel` 225 clamp applies to the panel, **not** to the scroll content. (defect 1 overflow,
   defect 2 clip)
2. **`DynamicGridLayout` scales cells to fill the allocated width** (confirmed bytecode), ignoring each
   cell's 38x34 max. The grids have no max-size cap and aren't left-anchored, so they stretch to the
   (over-wide) body and cells balloon. (defect 1 oversized cells)

The long `KeyValueRow` value (defect 2) is also the **main thing inflating the body past 225**, which is
what makes the grids scale up and the right edge clip. The defects are interlinked, not independent.

Recommended fix shape: **1 structural** (make the scroll view track viewport width) + **3 targeted**
(cap+left-anchor the grids; wrap the prose `KeyValueRow`s; drop the duplicate equipment grid). The
targeted three are quick wins; the structural one is the durable fix that stops any future child
silently clipping and is the proper ADR-0004 replacement for the deleted `FixedWidthPanel`.

---

## Defect 1 - equipment grid overflows the panel and cells are oversized

### Root cause (two compounding causes)

**1a. The scroll view is sized to its widest child, with no 225 clamp.**
`SlayerPanel.java:121-125` - `bodyWrap` is a plain `JPanel(BorderLayout)` holding `dashboardBody`
(BoxLayout-Y) in `NORTH`, and `bodyWrap` is the `JScrollPane` view (`scrollPane = new JScrollPane(bodyWrap)`).

- A `JViewport` sizes a view that does **not** implement `Scrollable` with
  `getScrollableTracksViewportWidth()==true` to the **view's preferred width**, then lets it scroll. It
  does **not** shrink the view to the viewport. So `bodyWrap` width = `dashboardBody.getPreferredSize().width`
  = the **max preferred width** of the BoxLayout-Y children (sections + struts). Nothing clamps this to
  225.
- The only 225 clamp in the stack is `PluginPanel.getPreferredSize()` (verified bytecode: returns
  `Dimension(225, ...)` when `wrap==false`, which is our case via `super(false)`). That sizes the **panel**
  (and therefore the viewport) to 225 - but the **view inside** is free to be wider and is then clipped on
  the right because `HORIZONTAL_SCROLLBAR_NEVER` (`SlayerPanel.java:129`).
- The Wave-5 note "BorderLayout.NORTH keeps the body at full panel width" (team memory) is wrong for the
  over-width case: `BorderLayout.NORTH` gives the child the *container's* width, but the container
  (`bodyWrap`) is itself sized to its preferred width by the viewport - a circular sizing that resolves to
  "as wide as the widest child," not "225."
- What pushes the body over 225 today: a `KeyValueRow` with a long value (defect 2). Its preferred width =
  `KEY_COL_WIDTH(64) + SPACE_4(8) + full unwrapped value-text width` (e.g. the "Why"/"Method" sentences),
  easily 300-470px. That sets `dashboardBody` preferred width ~340-490 -> view ~340-490 wide -> the right
  ~115-265px is clipped with no scrollbar. That is the "Inve[ntory]" header, the stray top-right sprite,
  the "Gear.../Upg..." truncation.

**1b. `DynamicGridLayout` stretches cells to fill the allocated width.**
`EquipmentGrid.java:44` / `InventoryGrid.java:23` build on `DynamicGridLayout(5,3,..)` / `(rows,4,..)`.
Disassembling `net.runelite.client.ui.DynamicGridLayout.layoutContainer` from the 1.12.31.1 jar confirms
it does **not** behave like it leaves cells at preferred size:

```
sw = (parent.getWidth()  - insetsX) / (preferredLayoutSize.width  - insetsX)
sh = (parent.getHeight() - insetsY) / (preferredLayoutSize.height - insetsY)
for each child: d = child.getPreferredSize(); d.width = (int)(sw*d.width); d.height = (int)(sh*d.height)
```

It **scales every child's preferred size by `actualWidth / preferredWidth`** and lays cells out at the
scaled size. `EquipmentSlotCell`'s fixed 38x34 preferred/min/**max** (`EquipmentSlotCell.java:43-46`) is
honored only when computing the grid's *preferred* size; at layout time the cell width becomes
`38 * (gridAllocatedWidth / gridPreferredWidth)`.

- The grid JPanels set no `getMaximumSize` and no `alignmentX`, so a `JPanel`'s default max is
  `Short.MAX_VALUE`. In the BoxLayout-Y card body the cross-axis stretches the grid to the **full body
  width**. Equipment preferred width = `3*38 + 2*SPACE_1 = 118`; allocated the (inflated) body width it
  scales `sw = bodyWidth/118` -> the observed ~65-70px (and larger) cells. Height stays ~34 because the
  body gives the grid its preferred height (`sh ~= 1.0`), so cells stretch **horizontally only** - exactly
  the "stretched, bulky, mostly empty" look.

### Why it happens
1a: a plain `JPanel` scroll view + `HORIZONTAL_SCROLLBAR_NEVER` = silent right clip whenever any child's
preferred width > viewport. 1b: `DynamicGridLayout` is a fill-to-width layout; the grids were added
unconstrained into a stretch-to-width BoxLayout, so the 38x34 intent is never enforced at layout time.

### Fix options

**1b - cap + left-anchor the grids (quick win, component-local, low risk).** Make the grid non-stretching
so `DynamicGridLayout`'s `sw` stays 1.0:

- In `EquipmentGrid` and `InventoryGrid`: `setAlignmentX(LEFT_ALIGNMENT)` and override
  `getMaximumSize()` to return `getPreferredSize()` (same idiom already used by `LoadoutItemRow.java:146`,
  `KeyValueRow.java:90`, `wrappingNote`). With `max == pref`, the BoxLayout-Y cross-axis gives the grid its
  preferred width (118 / 158, both < 225), `sw == 1.0`, cells stay 38x34, grid sits left-anchored.
- Equivalent alternative: wrap each grid in a `FlowLayout(LEFT)` panel (FlowLayout never stretches its
  child). The `getMaximumSize` override is simpler and matches the house idiom - recommend that.

**1a - make the scroll view track the viewport width (structural, the durable fix).** Replace the plain
`bodyWrap` `JPanel` with a `JPanel` that implements `Scrollable` with
`getScrollableTracksViewportWidth()==true` (and `...TracksViewportHeight()==false` so vertical scroll still
works). The viewport then forces the view to exactly the viewport width (225), the BoxLayout-Y lays every
child within 225, wrapping components wrap and capped components cap - nothing can silently exceed 225
again. This is the ADR-0004-consistent replacement for the deleted `FixedWidthPanel` (honor the viewport
width instead of hardcoding it). NOTE: this only helps for children whose **minimum** width <= 225; the
grids after 1b (min 118/158) qualify, but a long-value `KeyValueRow` has minimum width = full text (JLabel
min defaults to pref) and will still overflow until defect 2 is fixed. So 1a needs 1b **and** 2.

### Recommendation
Do **1b + 2 + 3** as the immediate render fix, and **1a** as the structural backstop (ideally same wave).
1b alone fixes cell size but not the clip; 1a alone fixes the clip mechanism but the grids would then
scale to a full 225 (still ~75px cells) and long values still overflow - they must go together.

---

## Defect 2 - KeyValueRow values clip instead of wrapping/truncating

### Root cause
`SlayerPanel.java:371` and `:376` render long prose ("Why" = `locationReason`, "Method") in a
`KeyValueRow`. `KeyValueRow.java:38-41` makes the value a plain `JLabel` with **no max width, no minimum
cap, no wrap, no ellipsis tooltip**. `KeyValueRow.getMaximumSize()` (`:90-94`) caps **height only**
(width = `Integer.MAX_VALUE`).

- A `JLabel`'s preferred **and minimum** width both equal the full unwrapped text width (JLabel min
  defaults to pref). So the value row (a) inflates the row/body preferred width - this is the primary
  feeder of defect 1a's overflow - and (b) cannot be shrunk below its huge minimum by BoxLayout, so even a
  viewport-width clamp would not save it. The viewport simply clips the raw label at 225, with no ellipsis
  and no tooltip - exactly the "Why Gear: MELEE | ... | U..." and "Method Cannon + melee in Brimhaven..."
  cut-offs.
- W7's S1 fix only converted the inline **NOTE** labels to wrapping `JTextArea`s
  (`SlayerPanel.wrappingNote`); it never touched `KeyValueRow`, so the value rows still clip.

### Fix options

**A - wrap the prose rows (recommended; consistent with S1 / FR-10).** For "Why" and "Method"
specifically, stop using `KeyValueRow`; render a `caption("Why")` + `wrappingNote(reason, TEXT_PRIMARY)`
(reuse the existing helper at `SlayerPanel.java:673`). A lineWrap `JTextArea` has a **small minimum
width**, so it neither inflates the body nor clips - it wraps to the panel width (already proven for the
no-bank note: `SlayerPanelTest.java:789-794`). Most humane (full text visible) and reuses a tested idiom.
Trade-off: loses the single-line key|value alignment for those two rows - acceptable, they are prose, not
scannable key/values.

**B - harden `KeyValueRow` generally (defense in depth).** Keep the key|value layout for short rows but
make the component overflow-proof: set the value label's minimum width small (so it can shrink), cap the
row's preferred/maximum **width** to `PANEL_WIDTH`, and set `valueLabel.setToolTipText(value)` in the
ctor/`setValue` so the full text is reachable on hover (a `JLabel` auto-ellipsizes "..." once allocated <
preferred). This also protects "Bank seen", "Est. DPS" and the dev diag rows (the N3 clip).

### Recommendation
**A** for the two prose offenders (wrap = best UX, matches the studio's S1/FR-10 wrap decision), plus
**B** as a cheap general hardening so no `KeyValueRow` (incl. diagnostics) can blow out the panel again.

---

## Defect 3 - worn gear shown twice (empty grid + rows)

### Root cause
`SlayerPanel.LoadoutSection.rebuild` adds **both** the `EquipmentGrid` (`SlayerPanel.java:483-484`) **and**
a `LoadoutItemRow` per worn slot (`:487-496`). This is the Wave-5 "Decision 3: grid = secondary overview,
rows = primary." With only a few items, the 15-cell OSRS cross is mostly empty wells - it duplicates the
rows and wastes vertical space. UX1 (worn primary as `EquipmentGrid` vs `LoadoutItemRow` list) was formally
deferred to UX (team memory), and the build shipped **both**.

### Fix options
- **Drop the `EquipmentGrid` from the loadout body (recommended).** Keep the worn `LoadoutItemRow`s - they
  are strictly richer in a 225px column (name + slot + price + state, and consistent with the required-item
  row in the Task card). Removing `SlayerPanel.java:483-484` realizes the "rows primary" half of Decision 3
  and the empty-cross waste disappears. The `EquipmentGrid` component stays in the library for future use.
- **Or keep a glanceable grid, compact/filled-only.** Render only occupied slots instead of the 15-cell
  cross. But this loses positional reading and still duplicates the rows - not recommended.
- **Or grid-only, drop the rows.** Loses names/prices/state in the narrow column - not recommended.

### Recommendation
Drop the grid from the loadout section (keep rows). Flag for UX sign-off since UX1 was their call; from an
engineering view, rows are the better primitive for this width and the grid is the redundant, mostly-empty
element.

---

## Horizontal scrollbar / silent clip - confirmation
`SlayerPanel.java:129` sets `HORIZONTAL_SCROLLBAR_NEVER` (an NFR: "0 horizontal scrollbars"). Combined with
a non-viewport-tracking view (1a), any over-width content is clipped with no scrollbar and no signal. The
NFR is correct; the fix is to make content fit the viewport (1a/1b/2), not to enable the scrollbar.

## Quick wins vs structural
- **Quick wins (component-local, low risk):** 1b (grid max-size cap + left-align), 2A (Why/Method ->
  `wrappingNote`), 3 (remove `EquipmentGrid` from loadout). These three resolve the visible render defects.
- **Structural (do alongside):** 1a (`Scrollable` view tracking viewport width) - prevents any future
  child from silently clipping; proper ADR-0004 replacement for `FixedWidthPanel`. Slightly higher risk:
  touches the scroll contract and the scroll-capture/restore NFR tests, so re-run the self-diff/scroll NFR
  tests after.

## Testable assertions (match the existing headless idiom)
The current `EquipmentGridTest.java:134-135` asserts a cell's **preferred** size (38x34) but never the
**laid-out** width - which is exactly why the scaling defect slipped through. Add layout-time and
panel-width assertions, following the `setSize(PANEL_WIDTH, ...)`-then-assert idiom already used at
`SlayerPanelTest.java:789-794`:

- **1b:** `assertEquals(grid.getPreferredSize(), grid.getMaximumSize())`; and build a grid, `setSize(400,
  h)`, `doLayout()`, then `assertEquals(GRID_CELL_WIDTH, componentByName(grid,"equip-cell-WEAPON").getWidth())`
  (fails today at ~130, passes after the cap).
- **1a (panel-level):** render a task with a long "Why"/"Method", `panel.setSize(PANEL_WIDTH, big)`,
  `panel.doLayout()` / validate, then assert **no** descendant of `dashboardBody` has
  `getWidth() > PANEL_WIDTH` (or `dashboardBody.getPreferredSize().width <= PANEL_WIDTH` once the scroll
  view tracks viewport width). Lock the `Scrollable` contract:
  `assertTrue(((Scrollable) view).getScrollableTracksViewportWidth())`.
- **2:** assert the "Why"/"Method" content is a wrapping `JTextArea` (`getLineWrap()`), and after
  `setSize(PANEL_WIDTH, ...)` its `getPreferredSize().width <= PANEL_WIDTH` (mirrors the no-bank note test).
  For option B: assert `valueLabel.getToolTipText()` equals the full value and row preferred width <=
  PANEL_WIDTH.
- **3:** assert the loadout body has **no** `equipment-grid` descendant while still rendering one
  `loadout-item-row` per worn slot.

# Plugin-Hub UI Research R2 — Item Grids, Empty Slots & Long-Text Layout

**Purpose:** Round-2 deep dive into how the top RuneLite plugins size item/equipment grids, fill empty
slots, and handle long text inside the fixed 225px `PluginPanel`, to fix three concrete layout bugs now
visible in our live panel:
- **(a)** the equipment/inventory grid overflows the panel / clips off the right edge, with cells
  stretched far larger than the 36×32 sprite;
- **(b)** long `Why` / `Method` key-value text clips instead of wrapping or truncating;
- **(c)** a mostly-empty equipment grid wastes vertical space.

**Author:** research analyst (RuneLite UI/UX review, round 2)
**Date:** 2026-06-29 · builds on `docs/ui-review/research-top-plugins.md`

## Sourcing & confidence
- **Code citations are VERIFIED.** I read source directly from shallow clones (the 5 round-1 plugin-hub
  repos already on disk, plus `collection-log` and `shortest-path` cloned today) and from RuneLite
  **core** files downloaded from `raw.githubusercontent.com/runelite/runelite/master` today. `file:line`
  refers to those exact HEADs (today); the plugin-hub pins slightly older commits so a line may drift a
  few rows — file paths and the code shapes are stable.
- **The root-cause analysis of `DynamicGridLayout` is VERIFIED from its source** (the scaling math is the
  crux of bug (a) — see §0).
- **Install ranks** for plugin-hub entries are from round-1's live API pull (2026-06-29). RuneLite
  **core** plugins (Loot Tracker, Hiscore, XP Tracker, Grand Exchange) are *bundled* with every client,
  so they have no hub rank but effectively ship to ~100% of users; they are the most authoritative Swing
  references that exist for this client.
- One round-1 classification is **CORRECTED** below: `shortest-path` has **no side panel** (config +
  scene overlay only) — flagged in the table.

---

## §0 — The mechanism behind bug (a): how `DynamicGridLayout` actually sizes cells (VERIFIED)

RuneLite core `ui/DynamicGridLayout.java` (a `GridLayout` subclass "with support for cells with unequal
size"). Its `layoutContainer` does **not** keep cells at their preferred size — it **scales every cell to
fill the parent**:

```java
// DynamicGridLayout.java:102-131
final Dimension pd = preferredLayoutSize(parent);                 // natural grid size
final double sw = (parent.getWidth()  - wborder) / (pd.width  - wborder);   // width  scale
final double sh = (parent.getHeight() - hborder) / (pd.height - hborder);   // height scale
...
d.width  = (int)(sw * d.width);   // each cell's preferred size is multiplied by the scale
d.height = (int)(sh * d.height);
```

Consequences for us (our `EquipmentGrid`/`InventoryGrid` use `DynamicGridLayout` with fixed 38×34 cells):
- A plain `JPanel` whose layout is a `GridLayout`/`DynamicGridLayout` (a `LayoutManager`, **not** a
  `LayoutManager2`) reports `getMaximumSize() == (Integer.MAX_VALUE, MAX_VALUE)`. So when added straight
  into our `SectionCard` body (`BoxLayout.Y_AXIS`, `SlayerPanel.java:483,502`), **BoxLayout stretches the
  grid to the full card width** (~190px) and gives it extra height.
- `DynamicGridLayout` then multiplies the 38×34 cells by `sw`/`sh`. For the 3-wide equipment cross,
  `sw ≈ 190 / (3·38 + gaps ≈ 118) ≈ 1.6` → ~60px-wide wells with a 36px sprite floating inside a bright
  `LineBorder` (the "stretched far larger than the sprite" look). Extra vertical space scales them taller
  too — the "wasted void" of bug (c). **The empty cells are not the problem; the stretch is.**
- `PluginPanel` sets `scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)`
  (`PluginPanel.java:70`). **There is never a horizontal scrollbar** — any child forced wider than the
  ~213px viewport is **clipped on the right**, not scrolled. That is bug (a)'s "clips off the right edge"
  and bug (b)'s clipping. Keeping every child ≤ panel width is therefore non-negotiable.

**The exact `PluginPanel` width contract (VERIFIED, `PluginPanel.java:38-92`):**
```java
public static final int PANEL_WIDTH     = 225;  // :38  usable content width
public static final int SCROLLBAR_WIDTH = 17;   // :39  reserved for the vertical scrollbar
public static final int BORDER_OFFSET   = 6;    // :40  EmptyBorder(6,6,6,6) inside the content
private static final Dimension OUTER_PREFERRED_SIZE = new Dimension(PANEL_WIDTH + SCROLLBAR_WIDTH, 0); // :42 = 242
// wrap=true (default): content = EmptyBorder(6) + DynamicGridLayout(0,1,0,3), placed NORTH in a
// vertically-scrolling, horizontally-FIXED viewport (:60-78).
@Override public Dimension getPreferredSize() {                                       // :88-92
    int width = this == wrappedPanel ? PANEL_WIDTH + SCROLLBAR_WIDTH : PANEL_WIDTH;   // 242 outer / 225 inner
    return new Dimension(width, super.getPreferredSize().height);
}
```
A subclass's usable content width is therefore **`225 − 2·6 = 213px`** (less while the vertical scrollbar
shows). Subclasses are expected to (i) lay out top-down within that fixed width, (ii) never report a child
wider than it, and (iii) let height grow freely (viewport scrolls vertically only). Our theme's
`PANEL_WIDTH = 225` matches; the missing discipline is bounding inner content to ~213 and never letting a
child force it wider.

The fix the top plugins use is to *never let the grid be stretched*: size cells fixed and put the grid in
a container that respects its preferred size (FlowLayout wrapper, or cap `maximumSize == preferredSize`).
Details and copy-paste in §3.

---

## §1 — Plugins reviewed (12 + core base classes)

> The RuneLite **core** repo (`github.com/runelite/runelite`) is treated as a first-class source here, not
> just a dependency: it provides the side-panel base (`PluginPanel`, §0), the grid layout whose scaling
> causes our bug (`DynamicGridLayout`, §0), and the reusable components/list panels in §2.12. Four bundled
> core plugins below (Loot Tracker, Hiscore, XP Tracker, Grand Exchange) plus WorldSwitcher are the
> authoritative grid/long-text examples.

| Plugin | Hub rank / source | Repo | UI surface relevant here |
|---|---|---|---|
| Quest Helper | #1 (565k) | Zoinkwiz/quest-helper | `FixedWidthPanel` + `JTextArea` wrapped text; requirement rows |
| Inventory Setups | ~#24 (216k) | dillydill123/inventory-setups | **Equipment cross + 4-wide inventory** (closest analog to ours) |
| Party Panel | #13 (240k) | TheStonedTurtle/party-panel | Real 4-wide inventory grid + equipment slots |
| Banked Experience | #18 (233k) | TheStonedTurtle/banked-experience | Recomputed 5-wide item selection grid |
| Equipment Inspector | ~#27 (204k) | botanicvelious/Equipment-Inspector | sprite + name/slot/price row |
| Loot Tracker | **core (bundled)** | runelite/runelite | 5-wide item grid w/ empty padding + title truncation |
| Hiscore | **core (bundled)** | runelite/runelite | 8×3 dense icon+value grid in 225px |
| XP Tracker | **core (bundled)** | runelite/runelite | per-skill 2×2 stat boxes |
| Grand Exchange | **core (bundled)** | runelite/runelite | fixed-size offer slot + CardLayout |
| Collection Log | ~#47 (128k) | evansloan/collection-log | control/status side panel; line-wrapped status text |
| Shortest Path | ~#28 (197k) | Skretzo/shortest-path | **CORRECTION: no side panel** — config + scene overlay only |
| World Hopper | **core (bundled)** | runelite/runelite | `WorldSwitcherPanel`/`WorldTableRow` — fixed-column list + tooltip (§2.12) |
| *(core base)* | core | runelite/runelite | `PluginPanel`, `DynamicGridLayout` (§0); `PluginErrorPanel`, `IconTextField`, `materialtabs`, `TabContentPanel` (§2.12) |

---

## §2 — Per-plugin techniques (points 1–5: grid sizing · empty slots · long text · width · density)

### 2.1 Inventory Setups — the direct analog (equipment cross + inventory grid) ✅ copy this
Repo `dillydill123/inventory-setups`, package `inventorysetups/ui/`.

1. **Grid sizing — FIXED cells in a PLAIN `GridLayout`, never stretched.**
   - The slot is a `JPanel` with a **fixed** `setPreferredSize(46×42)` (`InventorySetupsSlot.java:78-79,
     131`): `SLOT_WIDTH=46, SLOT_HEIGHT=42`, `GridBagLayout` with the sprite `imageLabel` centered and
     fuzzy/stack indicators pinned NE/SE.
   - Equipment cross = **`new GridLayout(5, 3, 1, 1)`** filling **all 15** cells
     (`InventorySetupsEquipmentPanel.java:99-133`). Inventory = `new GridLayout(7, 4, 1, 1)` with all 28
     (`InventorySetupsInventoryPanel.java:72-86`). Plain `GridLayout` (not `DynamicGridLayout`) because
     every cell is the same size.
   - **The anti-stretch trick:** the grid is *not* added to a BoxLayout directly. The base class
     `InventorySetupsContainerPanel` keeps its **default `FlowLayout`** and does
     `add(containerPanel)` where `containerPanel` (BorderLayout) holds the grid in CENTER
     (`InventorySetupsContainerPanel.java:55-76`). FlowLayout lays its child out at **preferred size and
     centers it**, so the grid stays `cols × cell` wide and the cells never scale up. This is the single
     pattern that fixes our bug (a).
2. **Empty slots — full grid of placeholder wells.** The 4 cross corners are real fixed-size slots tinted
   `ColorScheme.DARK_GRAY_COLOR` (vs `DARKER_GRAY_COLOR` for live slots) so they recede but hold the
   shape (`InventorySetupsEquipmentPanel.java:115,128`). Empty inventory slots are the same panels with
   no icon. No void because the grid isn't stretched.
3. **Long text — shrink the font.** For the one overflowing label (attack option "Longrange") they swap
   to a smaller font when `length() >= 9` (`InventorySetupsEquipmentPanel.java:144-154`). Lightweight,
   single-field technique.
4. **Width discipline.** Fixed cell sizes + FlowLayout wrapper means the panel's content width is bounded
   by `cols × cell` regardless of available width — it can only ever be *narrower* than 225, never wider.
5. **Density.** `FontManager.getRunescapeSmallFont()` for indicators; the cross is a compact ~3·46 ≈ 140px.

### 2.2 Party Panel — tune the cell so columns fill the width exactly
Repo `TheStonedTurtle/party-panel`, `ui/`.

1. **Grid sizing — `DynamicGridLayout` but cell tuned so `cols × cell ≈ panel width`.** Inventory is
   `new DynamicGridLayout(7, 4, 2, 2)` (`PlayerInventoryPanel.java:55`) with `INVI_SLOT_SIZE = 50×42`
   set as both **min and preferred** on every slot label (`:69-70,95-96`), and the whole panel pinned to
   `PANEL_SIZE = (PANEL_WIDTH - 14, 296)` (`:44,57`). Because `4·50 + gaps ≈ 206 ≈ PANEL_WIDTH-14`, the
   `DynamicGridLayout` scale factor is ≈ 1.0, so cells don't visibly stretch. (This works *because* the
   column count and cell width were chosen to match the width — it does not generalise to a 3-wide cross,
   which is why §2.1's FlowLayout wrapper is the better fix for our equipment grid.)
2. **Empty slots — always render the full 28.** After laying real items it pads with blank `JLabel`s up
   to 28 so the bag shape is always complete (`PlayerInventoryPanel.java:92-100`).
3. **Long text — push detail into HTML tooltips, not the cell.** Rune pouch contents render as a
   multi-line `<html>…<br>…</html>` tooltip (`PlayerInventoryPanel.java:106-123`); the cell itself stays
   sprite-only.
4. **Width discipline.** `setPreferredSize(PANEL_WIDTH - 14, …)` on the grid panel (`:44`).
5. **Equipment slot look.** `EquipmentPanelSlot` composites the item over the native slot background and
   resizes to a fixed `IMAGE_SIZE = 48` icon, with a placeholder image when empty
   (`EquipmentPanelSlot.java:38,49,56-67`) — the icon's fixed size *is* the cell size.

### 2.3 Loot Tracker (core) — the canonical 5-wide grid + empty padding + title truncation ✅
`runelite/runelite` `…/plugins/loottracker/LootTrackerBox.java`.

1. **Grid sizing — plain `GridLayout(rows, 5, 1, 1)`, rows computed from item count.**
   `ITEMS_PER_ROW = 5` (`:64`); `rowSize = ceil(items / 5)` (`:272`); `itemContainer.setLayout(new
   GridLayout(rowSize, ITEMS_PER_ROW, 1, 1))` (`:275`). Each cell is a `JPanel` holding a centered image
   `JLabel`; the **sprite is a centered icon, so even though `GridLayout` stretches the cell to fill the
   column the 36px sprite is never scaled** — it just gets padding. (Our bug is worse only because we
   draw a bright `LineBorder` on the over-wide well; a centered icon on a plain background hides the
   stretch.)
2. **Empty slots — pad to a full last row.** The loop runs `i < rowSize * ITEMS_PER_ROW` and adds an
   empty `slotContainer` (background-only `JPanel`) when items run out (`:279-325`) so the grid is always
   rectangular.
3. **Long text — truncate via `setMinimumSize(1, h)` in a `BoxLayout.X_AXIS` row + glue.** The title
   label sets `setMinimumSize(new Dimension(1, prefH))` "to make BoxLayout truncate the name"
   (`:109-111`), with `Box.createHorizontalGlue()` pushing the price right (`:124`). Full value lives in
   a tooltip (`:183,189`). This is the clean single-line clip-instead-of-overflow recipe.
4. **Width discipline.** Nothing reports a width > viewport: the grid fills width, the title row squeezes.
5. **Density.** `FontManager.getRunescapeSmallFont()`, collapsible box (`collapse()/expand()` toggle
   `itemContainer.setVisible` `:195-216`), `<html>` tooltips for per-item GE/HA detail (`:330-364`).

### 2.4 Banked Experience — recompute the grid rows on every data change
`TheStonedTurtle/banked-experience` `components/`.

1. **Grid sizing — `new GridLayout(rowSize, 5, 1, 1)` rebuilt each refresh.** `ITEMS_PER_ROW = 5`
   (`SelectionGrid.java:48`); `rowSize = ceil(items/5)` (`:119`); layout is re-set in
   `refreshGridDisplay()` after filtering to items with qty>0 (`:112-137`). Each `GridItem extends
   JLabel` carries the sprite via `icon.addTo(this)`; selection/ignore states are background colours.
2. **Empty slots — omit them.** It filters out zero-qty items rather than drawing blanks (a *data* grid,
   not a fixed *equipment* grid) — relevant contrast: for a variable item list, recompute rows and don't
   pad; for a fixed-shape equipment cross, pad (§2.1/§2.3).
3. **Long text — rich `<html>` tooltip** carries name + xp/action + total; the cell is sprite-only.
4. **Width discipline.** 5 columns of icon-sized cells ⇒ width bounded.
5. **Density.** Distinct background colours encode state; `ExpandableSection` for collapse.

### 2.5 Equipment Inspector — the sprite + name/slot/price row template
`botanicvelious/Equipment-Inspector` `ItemPanel.java`.

1. **Row, not grid** — `GroupLayout`: sprite LEFT, stacked `name / slot / price` RIGHT (`:73-91`), sprite
   via `icon.addTo(imageLabel)` (`:65-66`). This is the model for our `LoadoutItemRow`.
2. **Empty:** n/a (one row per worn item).
3. **Long text:** *weakly handled* — names are plain `JLabel`s and rely on item names being short;
   `GroupLayout` will grow the row to the longest label. **Do not copy this for our Why/Method** (it can
   exceed 225). Use §2.6 instead.
4. **Width discipline.** OK only because item names are short.
5. **Price colour-coding** (green >10M, white >100k, yellow else, light-grey 0) + full number tooltip via
   `QuantityFormatter` (`:97-112`); right-click → wiki `Special:Lookup` (`:44-58`).

### 2.6 Quest Helper — the gold standard for WRAPPING long text in 225px ✅ copy this
`Zoinkwiz/quest-helper` `panel/`.

1. **Grid:** n/a (text-first panel).
2. **Empty:** uses `PluginErrorPanel`-style empty messages.
3. **Long text — `JTextArea` line-wrap, dressed as a label.** `JGenerator.makeJTextArea(text)`:
   `setLineWrap(true)`, `setWrapStyleWord(true)`, `setEditable(false)`, `setFocusable(false)`,
   `setOpaque(false)`, `setBackground(UIManager.getColor("Label.background"))`, zero border
   (`JGenerator.java:67-93`). **Every** description / requirement / overview string in the plugin is one
   of these (`QuestRequirementsPanel.java:100,140,250`, `QuestOverviewPanel`, `QuestRewardsPanel.java:
   56-57`). Plain single-line labels go through `makeJLabel`, which **disables HTML**
   (`putClientProperty("html.disable", TRUE)`, `JGenerator.java:35`) so a stray `<` can't trigger
   HTML width blow-ups.
4. **Width discipline — `FixedWidthPanel`.** A `JPanel` overriding `getPreferredSize` to
   `(PluginPanel.PANEL_WIDTH, super.height)` (`FixedWidthPanel.java:34-38`). Wrapping `JTextArea`s live
   inside it, so they always know the width to wrap at and the panel can never report a width > 225
   (`QuestHelperPanel.java:76,91-92`; search bar `PANEL_WIDTH - 20`, `:290`). This is the umbrella fix
   for bug (b)/(a)-clipping.
5. **Density.** Collapsible step/section panels; tiny 10×10 info buttons; live `IconTextField` search.

### 2.7 Hiscore (core) — dense icon+value grid in 225px
`…/plugins/hiscore/HiscorePanel.java`.

1. **Grid sizing — `new GridLayout(8, 3)`** for 24 skills (`:254`); minigames/bosses `GridLayout(0, 3)`
   (`:279,297`). Cells are sized by the GridLayout (panel/3 ≈ 70px); content is one `JLabel` per cell.
2. **Empty:** fixed skill set, every cell filled.
3. **Long text:** values are short, padded numbers (`pad("--", type)`).
4. **Width discipline.** 3 equal columns; nothing exceeds width.
5. **Density — normalise sprites to a consistent small canvas.** Each icon is fit into a 25×25 canvas
   then scaled to 20×20 for alignment, `iconTextGap` 4–10, `RunescapeSmallFont`, `EmptyBorder(2,0,2,0)`
   (`makeHiscorePanel`, `:326-353`, esp. the canvas-resize comment `:338-340`). Good template if we ever
   want a compact stat strip.

### 2.8 XP Tracker (core) — per-skill mini-dashboard
`…/plugins/xptracker/XpInfoBox.java`, `XpPanel.java`.

1. **Grid sizing — `new DynamicGridLayout(2, 2)`** for the four configurable stat readouts inside each
   skill box (`XpInfoBox.java:201`); the skill icon uses a fixed `setPreferredSize(width, height)`
   (`:297`).
2. **Empty:** per-skill boxes are created on demand (no empties).
3. **Long text — HTML stat labels** built by a `htmlLabel(...)` helper (`:388-391`); compact key/number.
4. **Width discipline.** Box uses `BorderLayout`/fixed icon; stacks vertically in `XpPanel`.
5. **Density.** 2×2 stat grid packs 4 numbers under one header in minimal height.

### 2.9 Grand Exchange (core) — fixed-size slot with CardLayout for two densities
`…/plugins/grandexchange/GrandExchangeOfferSlot.java`.

1. **Sizing — fixed dimensions everywhere.** Item icon `setPreferredSize(45, PANEL_HEIGHT)` (`:140`),
   toggle icons 30px (`:154,185`), details panel `setPreferredSize(0, PANEL_HEIGHT)` so height is pinned
   and width flexes (`:190`); `offerFaceDetails` is `GridLayout(2,1,0,2)` (`:158`).
2. **Empty:** placeholder/empty offer state via CardLayout card.
3. **Long text:** item name truncates within the fixed icon+text row.
4. **Width discipline — `setPreferredSize(0, H)`** (width 0 ⇒ "take what BoxLayout gives, fix height").
5. **Density — `CardLayout`** flips a compact "face" view ↔ detailed view in the same footprint (`:130`).

### 2.10 Collection Log — control/status side panel (item grid is in-game)
`evansloan/collection-log` `CollectionLogPanel.java`.

1. **Grid:** the actual item grid is the **in-game** collection-log widget, not the side panel. The side
   panel is buttons + status text. Icon button row = `new GridLayout(1, 3, 10, 0)` (`:134`); button
   stacks `GridLayout(4, 1)` (`:214,317`); a single fixed sprite uses `setPreferredSize(36, 36)`
   (`:409-410`).
2. **Empty:** n/a.
3. **Long text — `new JTextArea(0, 22)` with `setLineWrap(true)`** (`createTextArea`, `:422-427`): the
   `22` is a column width hint so the area wraps at ~22 chars. Also an HTML colour label helper
   `<html><body style='color:#A5A5A5'>…` (`:493`) — note it sets colour but **not** a width, so it
   relies on the parent for wrapping.
4. **Width discipline — `setMinimumSize(new Dimension(PANEL_WIDTH, 0))`** on rows (`:281,297`) to make
   them fill, and buttons `setPreferredSize(PANEL_WIDTH - 5, 30)` (`:466`).
5. **Density.** Standard stacked `BoxLayout.Y_AXIS` sections.

### 2.11 Shortest Path — CORRECTION: no side panel
`Skretzo/shortest-path`. The repo has **no `PluginPanel`** — only `DebugOverlayPanel` (a scene
`OverlayPanel`), config, and pathfinder logic. Round-1's "control panel (inferred)" was wrong. Nothing to
borrow for our grid/text bugs; listed for the record.

### 2.12 RuneLite core `ui/components` + list panels — the authoritative base classes
`runelite/runelite` `runelite-client/src/main/java/net/runelite/client/`. These are the components the
client itself reuses across every built-in panel; they are the canonical answers to "empty state",
"long text in columns", and "search".

- **`PluginErrorPanel` (the standard empty/error state)** — `ui/components/PluginErrorPanel.java`. A
  `BorderLayout` panel with a centered title (`JShadowedLabel`) NORTH and a description CENTER
  (`RunescapeSmallFont`, grey), `EmptyBorder(50,10,0,10)`, hidden until `setContent`. **Long-text mechanism:
  `setContent` wraps the description in HTML so it can wrap** — the source comment is explicit: *"The
  description has to be wrapped in html so that its text can be wrapped"*
  (`PluginErrorPanel.java:67-72`): `setText("<html><body style='text-align:center'>" + description +
  "</body></html>")`. This is the `<html>`-label wrapping approach (works because the label is
  width-bounded by the 213px content). We already use `PluginErrorPanel` for "No task"; extend it.
- **`WorldSwitcherPanel` / `WorldTableRow` (dense list with FIXED COLUMNS + tooltip overflow)** —
  `plugins/worldhopper/`. The panel stacks rows with `DynamicGridLayout(0, 1)` / `GridLayout(0, 1)`
  (`WorldSwitcherPanel.java:87,91`) — a **single column is the safe use of `DynamicGridLayout`**: with 1
  column there is no horizontal stretch problem, only vertical stacking. Each row gives its short fields a
  **fixed column width** — `WORLD_COLUMN_WIDTH=60`, `PLAYERS_COLUMN_WIDTH=40`, `PING_COLUMN_WIDTH=35`
  (`WorldTableRow.java:63-65`) via `field.setPreferredSize(new Dimension(COLUMN_WIDTH, 0))` placed
  `BorderLayout.WEST/EAST` (`:182-205`) — and puts the **one variable-length field (activity) in the
  flexible CENTER column with the full text in a tooltip** (`activityField.setToolTipText(activity)`,
  `:352`). This is the canonical fixed-column long-text recipe: fixed widths for short data, one flexible
  CENTER column that truncates, tooltip for the rest. Directly applicable to our `KeyValueRow`.
- **`LootTrackerPanel` (the container around the §2.3 boxes)** — `plugins/loottracker/LootTrackerPanel.java`.
  Plain top-down `BoxLayout.Y_AXIS` stacks (`:178,185`) inside `EmptyBorder(6,6,6,6)` (`:172`), a
  `GridLayout(1,3,10,0)` icon-control row (`:209`) and a `GridLayout(2,1)` overall summary (`:298`). It
  sets **no width of its own** — it trusts `PluginPanel`'s fixed-width viewport and only ever stacks
  vertically. Confirms the intended contract: subclasses stack, the base fixes the width.
- **`IconTextField` (search box)** — `ui/components/IconTextField.java`. The reusable search field
  (leading icon, hover colour, clear button, `DocumentListener` for live filtering) every searchable panel
  uses; pair with `PANEL_WIDTH - N` preferred width as Quest Helper does. Lower priority for us.
- **`materialtabs/*` (`MaterialTab`/`MaterialTabGroup`)** — `ui/components/materialtabs/`. The tab bar we
  already use; no grid/long-text lessons beyond "it exists and is the standard".
- **`TabContentPanel` (reactive base, time-tracking)** — `plugins/timetracking/TabContentPanel.java`.
  Abstract base with `getUpdateInterval()` (200ms ticks) + `update()` (`:42-49`); the farming tab pushes
  long produce names into icon **tooltips** (`FarmingTabPanel.java:176-193,243`), never into the cell.
  Reinforces round-1's "react to ticks, no manual Refresh; detail lives in tooltips".

---

## §3 — Recommendations for our 3 bugs (copy-pasteable, each citing who does it this way)

Our relevant code today: `ui/components/EquipmentGrid.java` & `InventoryGrid.java` (both
`DynamicGridLayout`, added straight into the `SectionCard` `BoxLayout.Y` body at
`SlayerPanel.java:483,502`); `EquipmentSlotCell.java` (fixed 38×34 well with a 1px `LineBorder`);
`KeyValueRow.java` (single-line value `JLabel`, height-capped only); `LoadoutItemRow.java` (plain name
`JLabel` in `BorderLayout.CENTER`); theme `GRID_CELL_WIDTH/HEIGHT = 38/34`, `PANEL_WIDTH = 225`.

### Fix (a) — grid cell sizing & overflow
**Root cause (verified §0):** `DynamicGridLayout` scales fixed cells to fill whatever width/height the
parent grants, and our grids are added to a `BoxLayout.Y` that stretches them to full width (and the
`PluginPanel` then clips, never scrolls).

**Do two things — both proven by Inventory Setups (§2.1):**

1. **Use a plain `GridLayout` of fixed cells** (cells are all equal, so `DynamicGridLayout` buys nothing):
   ```java
   // EquipmentGrid: plain GridLayout, fixed-size cells, fill all 15 (real slots + blank wells)
   JPanel grid = new JPanel(new GridLayout(5, 3, SlayerTheme.SPACE_1, SlayerTheme.SPACE_1));
   grid.setOpaque(false);
   // ...add 15 EquipmentSlotCell (each already setPreferredSize 38×34)...
   ```
   (Inventory Setups equipment cross: `GridLayout(5,3,1,1)`, `InventorySetupsEquipmentPanel.java:99`;
   inventory: `GridLayout(7,4,1,1)`, `InventorySetupsInventoryPanel.java:73`. Loot Tracker:
   `GridLayout(rows,5,1,1)`, `LootTrackerBox.java:275`.)

2. **Stop the parent stretching it** — pick ONE:
   - **Preferred: FlowLayout wrapper** (the verified Inventory Setups mechanism, `InventorySetupsContainerPanel.java:55-76`). Add the *wrapper* to the body, not the grid:
     ```java
     JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); // or CENTER to centre the cross
     wrap.setOpaque(false);
     wrap.add(grid);
     body.add(wrap);   // FlowLayout gives `grid` its preferred size, un-stretched
     ```
     (Our `SlayerPanel.java:707` already builds exactly this kind of left FlowLayout panel elsewhere —
     reuse the idiom for the grids.)
   - **Minimal: cap the grid's max size to its preferred size** so BoxLayout can't grow it:
     ```java
     @Override public Dimension getMaximumSize() { return getPreferredSize(); } // on EquipmentGrid/InventoryGrid
     ```
     With `max == pref`, `DynamicGridLayout`'s `sw/sh` become 1.0 and cells stay 38×34 even if you keep
     that layout. (This is the same family as `LoadoutItemRow.getMaximumSize`, which already caps height.)

3. (Cosmetic) With cells now pinned at 38×34 the `LineBorder` hugs the sprite again; keep it, or follow
   Loot Tracker and use a background-only well (no border) so any residual padding is invisible.

For the **4-wide inventory**, if you want it to fill the width instead of left-clumping, tune the cell so
`4 × cell ≈ inner width` (Party Panel's `50×42` for `PANEL_WIDTH-14`, `PlayerInventoryPanel.java:44,55`);
otherwise the FlowLayout-left wrapper is fine and simplest.

### Fix (c) — empty equipment grid wasting space
This is **mostly a side effect of the stretch** — once Fix (a) stops vertical scaling, the cross is a
compact `3·38 × 5·34` block and the empty cells are small 38×34 wells, not a void. Then:
- **Keep the full cross with placeholder wells** (Inventory Setups parity, `…EquipmentPanel.java:115,128`;
  Party Panel pads to full grid, `PlayerInventoryPanel.java:92-100`; Loot Tracker pads the last row,
  `LootTrackerBox.java:279-325`). The recognisable cross shape is the point.
- **Mute the empty wells** so they recede: give corner/empty cells a darker, border-less background
  (Inventory Setups tints placeholders `DARK_GRAY` vs live slots `DARKER_GRAY`). Reserve the visible
  border/`SURFACE_CARD` for *filled* slots only.
- The cross is already "secondary overview" and lives in a collapsible `SectionCard` — that, plus the
  per-slot detail rows (`SlayerPanel.java:486-496`), is exactly the Equipment-Inspector/Party-Panel
  division of "compact grid for shape + text rows for detail". No big empty grid needed.
- **For a wholly-empty loadout (no gear at all), use `PluginErrorPanel`** rather than an empty cross —
  the RuneLite-core standard empty state (`ui/components/PluginErrorPanel.java`, §2.12), as the client
  itself does for "no results / no offers". We already use it for "No task"; extend it to the loadout.

### Fix (b) — long `Why` / `Method` (and long item names) clip
**Root cause:** `KeyValueRow` value is a single-line `JLabel` whose preferred width = full text width,
and the row only caps height (`KeyValueRow.java:90-94`); a long value pushes the row's preferred width
past 225 → `PluginPanel` clips it (no h-scroll, §0). Same for long names in `LoadoutItemRow`
(`BorderLayout.CENTER` `JLabel`).

**Choose by field:**

1. **Sentence-like values (`Why`, `Method`) → WRAP with a `JTextArea` (Quest Helper, §2.6).** Replace the
   value `JLabel` with:
   ```java
   JTextArea v = new JTextArea(value);
   v.setLineWrap(true); v.setWrapStyleWord(true);
   v.setEditable(false); v.setFocusable(false); v.setOpaque(false);
   v.setBorder(new EmptyBorder(0,0,0,0));
   v.setFont(SlayerTheme.TYPE_BODY); v.setForeground(SlayerTheme.TEXT_PRIMARY);
   ```
   (verbatim shape of `JGenerator.makeJTextArea`, `quest-helper JGenerator.java:67-93`). For this to wrap
   you must **remove the fixed `ROW_HEIGHT` cap** on a wrapping row (let height follow the text) and keep
   the value inside a width-bounded container — adopt a `FixedWidthPanel`-style content root that pins
   width to `PANEL_WIDTH` (Quest Helper `FixedWidthPanel.java:34-38`) or give the area a column hint like
   Collection Log's `new JTextArea(0, 22)` (`CollectionLogPanel.java:424`). Keep the key column fixed
   (`KEY_COL_WIDTH=64`) and let the wrapped value flow beside/below it.

2. **Short single-line values & long item names → TRUNCATE + tooltip (Loot Tracker, §2.3).** Keep a
   `JLabel` but stop it forcing width: put it in a `BoxLayout.X_AXIS` row, set
   `label.setMinimumSize(new Dimension(1, label.getPreferredSize().height))` and add
   `Box.createHorizontalGlue()` after it, with `label.setToolTipText(fullText)` for the cut-off content
   (`LootTrackerBox.java:109-111,124,183`). The label then clips/squeezes instead of widening the panel.

3. **Key/value as FIXED COLUMNS + tooltip (RuneLite core `WorldTableRow`, §2.12)** — the cleanest fit for
   `KeyValueRow` if you keep it single-line. Pin the key to a fixed column and let the value take the
   flexible CENTER, with the full value in a tooltip:
   ```java
   key.setPreferredSize(new Dimension(SlayerTheme.KEY_COL_WIDTH, 0)); // fixed column (WorldTableRow :182-190)
   row.add(key, BorderLayout.WEST);
   row.add(value, BorderLayout.CENTER);   // flexible column; clips to remaining width
   value.setToolTipText(fullText);        // full text on hover (WorldTableRow :352)
   ```
   The CENTER column can never exceed `213 − KEY_COL_WIDTH`, so the row never forces the panel wider.

4. **Belt-and-braces:** disable HTML on plain labels (`putClientProperty("html.disable", TRUE)`,
   `JGenerator.java:35`) so no value can accidentally trigger HTML auto-width. For a *centered message*
   block (not a row), the core idiom is the opposite — wrap in HTML so it wraps:
   `setText("<html><body style='text-align:center'>…</body></html>")` (RuneLite core `PluginErrorPanel.java:67-72`).

### Panel-width discipline (applies to all three — the umbrella rule)
Because `PluginPanel` **never** shows a horizontal scrollbar (`PluginPanel.java:70`), the invariant is
"no descendant may report a preferred/min width > the viewport." Enforce it the way the tops do:
- a content root that pins width to `PANEL_WIDTH` (`FixedWidthPanel`, Quest Helper);
- grids at **fixed cell size** in a **non-stretching** container (FlowLayout wrapper / `max==pref`),
  never a layout that sums child widths;
- single-line labels that can be long get **`minimumSize=(1,h)` + glue + tooltip**;
- multi-line text gets a **wrapping `JTextArea`**;
- push overflow detail into **`<html>` tooltips** (Party Panel, Banked Experience, Loot Tracker).

### Density notes
Keep `FontManager.getRunescapeSmallFont()` for captions/values; keep grids compact (36–50px cells, our 38
is fine); lean on the existing collapsible `SectionCard`; normalise any odd-sized sprite to a fixed canvas
before display (Hiscore 25×25→20×20, `HiscorePanel.java:338-340`).

---

## Appendix — files read (for reproducibility)
- Core (raw `runelite/runelite@master`, today): `ui/DynamicGridLayout.java`, `ui/PluginPanel.java`,
  `ui/components/PluginErrorPanel.java`, `ui/components/IconTextField.java`,
  `plugins/loottracker/LootTrackerBox.java` + `LootTrackerPanel.java`, `plugins/hiscore/HiscorePanel.java`,
  `plugins/xptracker/XpInfoBox.java` + `XpPanel.java`,
  `plugins/grandexchange/GrandExchangeOfferSlot.java` + `GrandExchangePanel.java`,
  `plugins/worldhopper/WorldSwitcherPanel.java` + `WorldTableRow.java`,
  `plugins/timetracking/TabContentPanel.java` + `farming/FarmingTabPanel.java`.
- Plugin-hub clones: `Zoinkwiz/quest-helper` (`panel/JGenerator.java`, `FixedWidthPanel.java`,
  `QuestRequirementsPanel.java`, `QuestHelperPanel.java`), `dillydill123/inventory-setups`
  (`ui/InventorySetupsSlot.java`, `InventorySetupsEquipmentPanel.java`, `InventorySetupsInventoryPanel.java`,
  `InventorySetupsContainerPanel.java`), `TheStonedTurtle/party-panel`
  (`ui/PlayerInventoryPanel.java`, `ui/equipment/EquipmentPanelSlot.java`),
  `TheStonedTurtle/banked-experience` (`components/SelectionGrid.java`, `GridItem.java`),
  `botanicvelious/Equipment-Inspector` (`ItemPanel.java`), `evansloan/collection-log`
  (`CollectionLogPanel.java`), `Skretzo/shortest-path` (no panel — verified).
- Our code: `ui/components/{EquipmentGrid,InventoryGrid,EquipmentSlotCell,KeyValueRow,LoadoutItemRow,
  SectionCard}.java`, `ui/theme/SlayerTheme.java`, `ui/SlayerPanel.java`.

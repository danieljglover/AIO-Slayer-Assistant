# Research R3 — Flipping Copilot side-panel layout (scroll without a visible bar + clean left alignment)

Status: COMPLETE. Feeds the immediate refinement wave for `SlayerPanel`.

Scope: how Flipping Copilot (FC) gets (1) wheel scrolling with no visible scrollbar and (2) consistent
left-aligned layout — plus the RuneLite-native scroll truth — translated into concrete, copy-pasteable
fixes for our two issues.

Sources (all primary, read at source unless noted):
- FC repo: https://github.com/cbrewitt/flipping-copilot (package `com.flippingcopilot.ui`). Files read
  from `master` (cross-checked the key file against `raw.githubusercontent.com/.../master/...`).
- RuneLite core: `runelite-client/.../net/runelite/client/ui/PluginPanel.java` (master) and
  `.../ui/components/CustomScrollBarUI.java` (present through tag `runelite-parent-1.9.3`; **removed from
  master** — see §2.2).
- Our code: `src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java` and `ui/components/*`.

Legend: **[V]** = verified by reading source / established JDK behaviour. **[I]** = inferred.

---

## TL;DR (the two fixes)

1. **No visible scrollbar, wheel still works** — keep our existing architecture exactly
   (`super(false)` + our own `JScrollPane` + the `ViewportTrackingPanel` view). Change ONE thing: keep
   `VERTICAL_SCROLLBAR_AS_NEEDED` (do **not** switch to `NEVER`) and give the vertical scrollbar **zero
   preferred width** (optionally a no-paint UI). This is exactly FC's trick — FC uses a 2px sliver
   (`StatsPanelV2.java:109`); 0px + a no-paint UI makes it fully invisible. **Do NOT switch to
   `super(true)`.** Rationale + risk in §3a. **[V]**

2. **Clean left alignment** — stop depending on `alignmentX` for the *narrow* children (the two grids).
   Wrap each grid in a **full-width wrapper that pins it left** (`BorderLayout` + `WEST`, or
   `FlowLayout(LEFT,0,0)`), with `setMaximumSize(MAX_VALUE, prefHeight)`. A full-width child cannot be
   horizontally mis-positioned by `BoxLayout`, so the ragged "grid indented, rows flush-left" look is
   structurally impossible. This is FC's idiom everywhere (`FlipPanel.java:40-42`,
   `StatsPanelV2.java:251-263`, `ControlPanel.java:38-42`). Details in §3b. **[V]**

---

## 1. Flipping Copilot teardown

### 1.1 Panel construction — `super(false)`, no FC-owned whole-panel scroll

`MainPanel` is the `PluginPanel` subclass and it opts **out** of RuneLite's scroll wrap: **[V]**

```
MainPanel.java:20   public class MainPanel extends PluginPanel {
MainPanel.java:35       super(false);                                  // NO RuneLite scroll wrap
MainPanel.java:36       setLayout(new BorderLayout());
MainPanel.java:37       setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
```

It then swaps whole views in `BorderLayout` regions (a fixed top bar pinned NORTH, content CENTER):

```
MainPanel.java:67   add(constructTopBar(false), BorderLayout.NORTH);   // anchored top bar
MainPanel.java:69   add(loginPanel,  BorderLayout.CENTER);
MainPanel.java:76   add(constructTopBar(true),  BorderLayout.NORTH);
MainPanel.java:77   add(copilotPanel, BorderLayout.CENTER);
```

Key point: **FC does not scroll the whole side panel as one unit.** The main panel is a fixed
`BorderLayout`; only the sub-panels that contain *lists* create their own `JScrollPane`. So FC's
top bar is effectively anchored — the same shape as ours (header anchored, body scrolls). **[V]**

`CopilotPanel` (the CENTER content) is a plain `JPanel` with `BoxLayout.Y_AXIS` that stacks the
sub-panels, sizing each by capping its `MaximumSize` height to its preferred height: **[V]**

```
CopilotPanel.java:23   setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
CopilotPanel.java:24   suggestionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, suggestionPanel.getPreferredSize().height));
CopilotPanel.java:26   add(suggestionPanel);  ...  add(controlPanel);  ...  add(statsPanel);
```

### 1.2 Scroll WITHOUT a visible bar — the thin-scrollbar trick

The flips list scrolls inside its own `JScrollPane`, and the vertical bar is shrunk to a **2px-wide
sliver** so it is effectively invisible while the wheel still works: **[V]** (this is THE mechanism)

```
StatsPanelV2.java:97    setLayout(new BorderLayout());
StatsPanelV2.java:107   JScrollPane scrollPane = new JScrollPane(flipsPanel);
StatsPanelV2.java:108   scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
StatsPanelV2.java:109   scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(2, 0));   // <-- thin bar
StatsPanelV2.java:110   scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
```

Note what FC does **not** do here: it does **not** set `VERTICAL_SCROLLBAR_NEVER`. The vertical policy
is left at the default `AS_NEEDED`, so when the flips overflow, the scrollbar component is made
*visible* — which is what keeps the mouse wheel alive (see §2.3). Shrinking its preferred *width* to 2px
just stops it taking visible space. The wheel scrolls; the bar is a hairline. **[V]**

Other FC scroll panes confirm the standard recipe (AS_NEEDED + horizontal NEVER + `setUnitIncrement(16)`):

```
graph/ConfigPanel.java:78    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
graph/ConfigPanel.java:79    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
PremiumInstancePanel.java:178 scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
PremiumInstancePanel.java:179 scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
PremiumInstancePanel.java:181 scrollPane.getVerticalScrollBar().setUnitIncrement(16);
```

### 1.3 Alignment & spacing — wrappers, not `alignmentX`

FC almost never touches `alignmentX` (a repo-wide search finds only two uses, both
`CENTER_ALIGNMENT` on a title — `PreferencesPanel.java:40`, `PremiumInstancePanel.java:118`). Instead it
keeps everything aligned by **construction**: **[V]**

- **Rows are `BorderLayout` with `WEST`/`EAST` (or `LINE_START`/`LINE_END`), capped to full width and
  natural height.** A full-width row can't be mis-positioned, and WEST/EAST give the clean
  "label left / value right" rhythm with zero alignment math:

  ```
  StatsPanelV2.java:252   JPanel item = new JPanel(new BorderLayout());
  StatsPanelV2.java:257   item.add(keyLabel, BorderLayout.WEST);
  StatsPanelV2.java:260   item.add(value,    BorderLayout.EAST);
  StatsPanelV2.java:261   item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));   // fill width, cap height

  FlipPanel.java:21       setLayout(new BorderLayout());
  FlipPanel.java:40       add(leftPanel,   BorderLayout.LINE_START);
  FlipPanel.java:41       add(profitLabel, BorderLayout.LINE_END);
  FlipPanel.java:42       setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
  ```

- **Left-aligned labels live inside a `FlowLayout(FlowLayout.LEFT, 0, 0)` wrapper** rather than fighting
  `alignmentX` in a `BoxLayout`:

  ```
  ControlPanel.java:40    JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
  ControlPanel.java:42    labelPanel.add(timeframeLabel);
  ```

- **Equal-width button rows use `GridLayout`** (`ControlPanel.java:44` → `new GridLayout(1, 4, 0, 0)`).
- A shared helper builds the vertical columns (`UIUtilities.java:123 newVerticalBoxLayoutJPanel()`),
  always with full-width children — so `BoxLayout` off-axis alignment is never ambiguous.

The takeaway: **FC dodges the entire `alignmentX` problem class by never putting a narrow, raw component
straight into a `BoxLayout.Y_AXIS`.** Narrow content is always inside a full-width `BorderLayout` /
`FlowLayout(LEFT)` wrapper. **[V]**

### 1.4 Section / divider / header / font / colour system

The polished look is mostly **MatteBorder hairline dividers + EmptyBorder padding rhythm + RuneLite
`ColorScheme`/`FontManager` tokens**: **[V]**

- **Dividers** are 1px `MatteBorder`s composed with padding via `CompoundBorder` — not separate
  components:

  ```
  StatsPanelV2.java:274   BorderFactory.createMatteBorder(0,0,1,0, ColorScheme.DARK_GRAY_COLOR)   // bottom hairline
  StatsPanelV2.java:286-288 createMatteBorder(1,0,1,0, ...) + new EmptyBorder(5,0,5,0)             // top+bottom rule
  ```

- **Collapsible header** = a `BorderLayout` header panel (title CENTER, chevron EAST) with hover
  recolour and a click toggle (`StatsPanelV2.java:284-358`) — same pattern as our `SectionCard`.
- **Hero number**: the profit value is the base Runescape bold font scaled up
  (`StatsPanelV2.java:294` → `FontManager.getRunescapeBoldFont().deriveFont(24f)`), centred.
- **Fonts**: `FontManager.getRunescapeSmallFont()` for keys/meta, bold for titles. **Colours**: all
  `net.runelite.client.ui.ColorScheme.*` (`DARK_GRAY_COLOR`, `DARKER_GRAY_COLOR`,
  `GRAND_EXCHANGE_PRICE`, `BRAND_ORANGE`, …) — exactly the tokens our `SlayerTheme` already maps to.
- **Density**: tight `EmptyBorder`s (e.g. items `new EmptyBorder(4,2,4,2)`), 5px rigid-area gaps
  (`Box.createRigidArea(new Dimension(0,5))`), heights pinned via `setMaximumSize(..., 20|70)`.

Net: FC's visual system and ours are already the *same* system. We are not missing a toolkit; we have
two mechanical defects (visible bar, ragged grid) that FC happens to have clean answers for.

---

## 2. The RuneLite-native scroll truth (core repo)

### 2.1 `PluginPanel(boolean wrap)` — what `super(true)` actually does **[V]**

```
PluginPanel.java:38   public static final int PANEL_WIDTH = 225;
PluginPanel.java:39   public static final int SCROLLBAR_WIDTH = 17;
PluginPanel.java:42   private static final Dimension OUTER_PREFERRED_SIZE = new Dimension(PANEL_WIDTH + SCROLLBAR_WIDTH, 0);
PluginPanel.java:50   protected PluginPanel()        { this(true); }     // default ctor wraps
PluginPanel.java:55   protected PluginPanel(boolean wrap) {
PluginPanel.java:58       if (wrap) {
PluginPanel.java:64           final JPanel northPanel = new JPanel();
PluginPanel.java:65           northPanel.setLayout(new BorderLayout());
PluginPanel.java:66           northPanel.add(this, BorderLayout.NORTH);              // content pinned to top
PluginPanel.java:69           scrollPane = new JScrollPane(northPanel);             // RuneLite-OWNED scroll pane
PluginPanel.java:70           scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
PluginPanel.java:72           wrappedPanel = new JPanel();
PluginPanel.java:76           wrappedPanel.setPreferredSize(OUTER_PREFERRED_SIZE);  // reserves 225+17 width
PluginPanel.java:78           wrappedPanel.add(scrollPane, BorderLayout.CENTER);
PluginPanel.java:80       } else { scrollPane = null; wrappedPanel = this; }       // super(false): no scroll
```

So with `super(true)`:
- RuneLite **creates the `JScrollPane` for you**, wrapping your content in a `BorderLayout.NORTH`
  holder (this is the "view tracks viewport width, pin to top" trick — our `ViewportTrackingPanel`
  re-implements the same idea more explicitly).
- **Horizontal policy = `NEVER`** (line 70). **Vertical policy is never set → JDK default
  `VERTICAL_SCROLLBAR_AS_NEEDED`.** So a tall panel shows a bar. **[V]**
- The outer width is hard-reserved at `PANEL_WIDTH + SCROLLBAR_WIDTH = 242` (`OUTER_PREFERRED_SIZE`,
  line 42/76), so even a hidden bar leaves a reserved gutter unless you fight it.
- The scroll pane is reachable from a subclass: `@Getter(AccessLevel.PROTECTED) ... scrollPane`
  (`PluginPanel.java:44-45`) → `getScrollPane()`.

`super(false)` (our path, and FC's): `wrappedPanel = this`, **no scroll pane at all** — you own the
layout and any scrolling.

### 2.2 Scrollbar styling — `CustomScrollBarUI` (slim "obsidian" bar)

RuneLite's slim dark scrollbar is a `BasicScrollBarUI` subclass. The class
`net.runelite.client.ui.components.CustomScrollBarUI` existed through `runelite-parent-1.9.3` and is the
canonical pattern (it is **removed from `master`'s `components/` package** — now applied globally via the
client LAF/`UIManager` "ScrollBarUI", **[I]** for the exact current location). Read at tag 1.9.3: **[V]**

```
CustomScrollBarUI.java:59   protected void paintTrack(...) { graphics.setColor(trackColor); fillRect(...); }   // flat track
CustomScrollBarUI.java:70   protected void paintThumb(...) { graphics.setColor(thumbColor); fillRect(...); }   // flat thumb
CustomScrollBarUI.java:79   protected JButton createEmptyButton() { ... setPreferredSize(new Dimension(0,0)); ... } // no arrows
CustomScrollBarUI.java:89   public static ComponentUI createUI(JComponent c) {
CustomScrollBarUI.java:92       bar.setUnitIncrement(16);
CustomScrollBarUI.java:93       bar.setPreferredSize(new Dimension(7, 7));                                       // 7px slim bar
CustomScrollBarUI.java:101  protected JButton createDecreaseButton(int o) { return createEmptyButton(); }
CustomScrollBarUI.java:110  protected JButton createIncreaseButton(int o) { return createEmptyButton(); }
```

This is the exact recipe to copy for a **fully invisible** bar: subclass `BasicScrollBarUI`, paint
nothing, return zero-size arrow buttons (§3a, option B).

### 2.3 Definitive: how a RuneLite side panel wheel-scrolls, and why `NEVER` kills the wheel **[V]**

- A `JScrollPane` handles the mouse wheel itself (`setWheelScrollingEnabled(true)` is the default). You
  do **not** add a `MouseWheelListener`.
- **But** `BasicScrollPaneUI`'s wheel handler scrolls the *vertical* bar only `if (toScroll != null &&
  toScroll.isVisible())`; otherwise it falls back to the horizontal bar, and if that's invisible too it
  returns without scrolling. With `VERTICAL_SCROLLBAR_NEVER` the vertical scrollbar component is set
  **invisible**, so the wheel does nothing. (This is precisely what our own `SlayerPanel.java:134-136`
  comment already states — it is correct.)
- Therefore the idiomatic "wheel scroll, minimal/no visible bar" recipe is **`AS_NEEDED` + a
  zero/near-zero-width, non-painting scrollbar** — *not* `NEVER`. Keeping the policy `AS_NEEDED` keeps
  the bar `isVisible()` (so the wheel works); shrinking its preferred width + suppressing its paint
  removes it visually. FC's 2px sliver and RuneLite's `CustomScrollBarUI` are two points on that same
  spectrum.

**Idiomatic answer:** a RuneLite side panel scrolls with the wheel because its content sits in a
`JScrollPane` whose vertical bar is *present and visible-flagged* (`AS_NEEDED`); the bar is kept
unobtrusive by LAF styling (≈7px) — and can be made fully invisible by zeroing its width and painting
nothing, while never setting the policy to `NEVER`.

---

## 3. Recommendations for our 2 issues + polish

Our `SlayerPanel` is already on the right architecture and is, in fact, the FC architecture:
`super(false)` (`SlayerPanel.java:86`), `BorderLayout` with the header band anchored NORTH
(`:99`) and our own `JScrollPane` CENTER (`:131-141`), fed by a `ViewportTrackingPanel`
(`Scrollable`, `getScrollableTracksViewportWidth()==true`, `:235-271`) that re-creates RuneLite's
"NORTH-pinned, viewport-width-tracking view". Both fixes are small, localized changes — no re-architecture.

### 3a. Wheel-scroll with NO visible bar — **keep our custom `JScrollPane`; hide the bar**

**Recommendation: keep the custom `JScrollPane` (do NOT move to `super(true)`).** Make the vertical bar
take no space and paint nothing, while leaving the policy at `AS_NEEDED` so the wheel keeps working.

In `SlayerPanel`'s constructor, where the scroll pane is configured (`SlayerPanel.java:137-140`), keep
`VERTICAL_SCROLLBAR_AS_NEEDED` and add:

```java
// Invisible vertical bar: AS_NEEDED keeps the bar "visible" so the wheel still scrolls (a JScrollPane
// ignores the wheel only when the bar is NOT visible, i.e. policy NEVER). Zero width = no gutter.
JScrollBar vbar = scrollPane.getVerticalScrollBar();
vbar.setPreferredSize(new Dimension(0, 0));   // FC uses (2,0); 0 = fully hidden
vbar.setUnitIncrement(16);                     // already present at :140
```

Option B (bulletproof across LAFs — mirrors RuneLite `CustomScrollBarUI`): also install a no-paint UI so
no thumb/track/arrows can ever render even if a LAF forces a minimum width. Add a small reusable class
under `ui/components` (or `ui/theme`):

```java
final class InvisibleScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
    @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { /* nothing */ }
    @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) { /* nothing */ }
    @Override protected JButton createDecreaseButton(int o) { return zero(); }
    @Override protected JButton createIncreaseButton(int o) { return zero(); }
    private JButton zero() {
        JButton b = new JButton();
        Dimension d = new Dimension(0, 0);
        b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d);
        return b;
    }
}
// then: vbar.setUI(new InvisibleScrollBarUI());
```

Why keep the custom `JScrollPane` and reject `super(true)`:
- **Anchored header.** `super(true)` would put the *entire* panel (our `TaskHeader` + `ActionBar`)
  inside RuneLite's scroll pane, so the header would scroll away. Our design (and FC's) anchors the
  header by keeping it in `BorderLayout.NORTH` outside the scroll pane — only possible with
  `super(false)`. **[V]**
- **No reserved gutter / full control.** `super(true)` bakes a 17px reserved width into
  `OUTER_PREFERRED_SIZE` (`PluginPanel.java:42/76`) and you'd still have to reach in via
  `getScrollPane()` to thin the bar — more friction for a worse result.
- **It's the FC pattern.** `MainPanel` is `super(false)` (`MainPanel.java:35`); list scrolling is a
  per-sub-panel `JScrollPane` with a thinned bar (`StatsPanelV2.java:107-110`). We already match it.

Risk (low): with a zero-width bar there is no draggable thumb — scrolling is wheel / keyboard / drag-
select only. **That is exactly the requested behaviour.** One caveat verified above: this *only* works
because the policy stays `AS_NEEDED`; if anyone "tidies" it to `NEVER`, the wheel dies — keep the
comment at `:134-136`. Touches one file (plus an optional ~10-line UI class). **[V]**

### 3b. Fix the alignment so everything left-aligns — **wrap the grids full-width**

Root cause **[V]**: in a `BoxLayout.Y_AXIS` column, children only sit flush-left when **every** child
shares the same `alignmentX`. Our full-width rows (`KeyValueRow`, `wrappingNote`, captions, combo) fill
the column width, so they always sit at the left edge. The **two grids are the only width-*capped*
children** — `EquipmentGrid`/`InventoryGrid` override `getMaximumSize()` to return `getPreferredSize()`
(`EquipmentGrid.java:69-72`, `InventoryGrid.java:43-47`), so they are ~120px wide in a ~207px column.
A capped child is the only thing `BoxLayout` can position off-axis, so it's the only thing that can look
"indented" if the column's resolved alignment isn't a clean 0.0. (We already call
`setAlignmentX(LEFT_ALIGNMENT)` on the grids — `EquipmentGrid.java:50`, `InventoryGrid.java:29` — which
helps but is fragile: it relies on *every* sibling, including any default-`0.5` component, also being
0.0. Relying on alignmentX is the brittle path FC deliberately avoids.)

**Recommended technique (FC's idiom — immune to alignmentX): wrap each narrow grid in a full-width,
left-pinning wrapper.** A full-width wrapper occupies the whole column, so `BoxLayout` has no slack to
mis-position; the grid sits at the wrapper's left edge regardless of any sibling's alignment.

Replace the two raw grid adds in `SlayerPanel.LoadoutSection.rebuild` (`SlayerPanel.java:547` and
`:566`) with a wrapped add. Add this helper next to `column()`:

```java
/** Pin a fixed-size component (a grid) flush-left in a full-width row so BoxLayout can't offset it. */
private static JComponent leftRow(JComponent fixed)
{
    JPanel row = new JPanel(new BorderLayout());            // or: new FlowLayout(FlowLayout.LEFT, 0, 0)
    row.setOpaque(false);
    row.setAlignmentX(LEFT_ALIGNMENT);
    row.add(fixed, BorderLayout.WEST);                      // grid hugs the left; East stays empty
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, fixed.getPreferredSize().height));
    return row;
}
```

```java
// :547  body.add(new EquipmentGrid(renderer, rec.getWorn(), state.getItemNames()));
        body.add(leftRow(new EquipmentGrid(renderer, rec.getWorn(), state.getItemNames())));
// :566  body.add(new InventoryGrid(renderer, rec.getInventory(), state.getItemNames()));
        body.add(leftRow(new InventoryGrid(renderer, rec.getInventory(), state.getItemNames())));
```

This mirrors `FlipPanel.java:40-42` and `StatsPanelV2.java:252/261` exactly (full-width `BorderLayout`
row, content pinned to a side, `setMaximumSize(MAX_VALUE, prefHeight)`).

Supporting rule (apply project-wide so the bug can't recur): **every direct child of a `BoxLayout.Y_AXIS`
should either fill the full width or be wrapped to do so; do not mix `alignmentX` values.** Our
`addContent()` already forces `LEFT_ALIGNMENT` on what it adds (`SectionCard.java:88-95`); the gap was
the two grids added straight to `body` while being width-capped. Wrapping them closes it without relying
on every future sibling staying 0.0.

Note on tests: anything asserting `getName()=="equipment-grid"`/`"inventory-grid"` as a *direct* child
of the loadout body will now find it one level down (inside the wrapper). Search the wrapper, or set the
name on the wrapper. (Checked: `EquipmentGridTest`/`InventoryGrid` references live under
`src/test/.../ui/components/` — verify the loadout-section traversal tests if any walk the body.)

### 3c. High-value polish worth adopting from FC

- **Hairline dividers via `MatteBorder`, not spacer components.** FC separates sections with a 1px
  `MatteBorder` folded into a `CompoundBorder` (`StatsPanelV2.java:274`, `:286-288`). We use a full
  `LineBorder` box around each `SectionCard` (`SectionCard.java:44-46`) plus `SPACE_5` gaps — clean, but
  if cards ever start to look heavy, a single bottom-hairline rhythm reads lighter and is more "FC".
  Low priority; our card border is already tasteful.
- **A hero number for the headline metric.** FC scales the base font for its profit value
  (`getRunescapeBoldFont().deriveFont(24f)`, `StatsPanelV2.java:294`). If we ever want "Est. DPS" or
  task progress to pop, the idiom is `SlayerTheme.TYPE_TITLE.deriveFont(<size>f)` — keeps it tokenised.
- **Confirm `setUnitIncrement(16)` stays** (we have it at `SlayerPanel.java:140`; FC and
  `CustomScrollBarUI` both use 16) so wheel steps feel native.
- **No new colour/spacing work needed.** Our `SlayerTheme` already maps to the same `ColorScheme` /
  `FontManager` tokens FC uses; the visual system is on-parity. The wins here are the two mechanical
  fixes above, not a restyle.

---

## Verified vs inferred summary

- **[V]** FC `super(false)` + `BorderLayout`, the 2px thin-bar trick, AS_NEEDED elsewhere, and the
  wrapper-based alignment idiom — all read from FC source (file:line cited; key file cross-checked
  against `master` raw).
- **[V]** RuneLite `PluginPanel(boolean wrap)` semantics, horizontal `NEVER` / vertical default
  `AS_NEEDED`, reserved 242 width, protected `getScrollPane()` — read from `master`.
- **[V]** `CustomScrollBarUI` recipe (flat paint, zero-size arrow buttons, 7px width, unit increment 16)
  — read at tag `runelite-parent-1.9.3`.
- **[V]** Wheel handler skips an invisible vertical bar (so `NEVER` disables the wheel; `AS_NEEDED`
  keeps it) — established `BasicScrollPaneUI` behaviour; matches our own code comment.
- **[I]** That current `master` RuneLite applies the slim scrollbar styling globally through its LAF
  (`UIManager` "ScrollBarUI") rather than the now-removed `components/CustomScrollBarUI` — corroborated
  by the RuneLite docs/wiki and the file's absence from `master`, but the exact current registration
  site was not pinned to a line.

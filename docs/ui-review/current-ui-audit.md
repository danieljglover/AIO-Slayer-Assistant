# All-In Slayer - Current Side Panel UI Audit (Board A1)

- Author: Frontend Engineer
- Date: 2026-06-29
- Scope: Code-level audit of the *shipped* RuneLite Swing UI and Swing/RuneLite feasibility of fixes.
- Inputs read: `SlayerPanel.java`, `SlayerOverlay.java`, `SlayerPanelState.java`, `PanelStatus.java`, `RefreshSource.java`, `SlayerDebugSnapshot.java`, `Recommendation.java`, the approved design spec (`docs/superpowers/specs/2026-06-28-all-in-slayer-side-panel-design.md`), `AllInSlayerPlugin.java` wiring, `build.gradle`.

## TL;DR

The redesign delivered the *information architecture* the spec asked for (one `SlayerPanelState`, three tabs, preserved refresh source, empty states). It still feels clunky because of **presentation and interaction choices the spec left as "acceptable for v1" but never revisited**:

1. The whole panel is text. Loadout is a column of `Item: Abyssal whip` strings with **zero item sprites**, even though every item ID is in hand and `ItemManager` is already wired and already resolving names. This is the single biggest "spreadsheet, not a game tool" offender.
2. The action row is **three full-width stacked text buttons** (~100px of vertical bloat) where RuneLite convention is a compact icon bar.
3. The UI leaks **developer language** at the player: `Source: STARTUP | Updated: ...`, raw enum names, and a permanent **Debug tab**.
4. Two **different ad-hoc row systems** (`row()` HTML labels vs `keyValueRow()` GridBag) plus manual fixed-width math and `<html><div style='width:Npx'>` hacks make spacing inconsistent and fragile.
5. Interaction smells: a prominent **manual Refresh** the panel almost never needs, a **"Mode: DPS" relabel button** that hides its toggle nature, and a **full teardown + revalidate of all three tabs on every event** (including every `GameStateChanged`).

RuneLite version note: `build.gradle:13,16` pins `net.runelite:client:latest.release`, so the full modern client UI API is available (`ItemManager.getImage`, `AsyncBufferedImage`, `SwingUtil`, `IconTextField`, `DimmableMaterialTab`, `ImageUtil`, etc.). Nothing below is blocked by an old client.

---

## Findings

### Finding 1 - Loadout is plain text with no item sprites (highest impact)

1. **What** - The Loadout tab lists gear as text rows: `Style: MELEE`, `Weapon: Abyssal whip`, `Item: Super combat potion(4)`, `Upgrade: Ghrazi rapier`. No icons, no visual gear layout. It reads like a config dump, not a loadout.
2. **Evidence** - `SlayerPanel.renderLoadoutTab` `SlayerPanel.java:265-310`. Worn rows: `row(prettySlot(entry.getKey()), itemName(state, entry.getValue()))` `:284`. Inventory rows: `row("Item", itemName(state, itemId))` `:292`. Upgrades: `row("Upgrade", itemName(state, itemId))` `:303`. `itemName(...)` only maps id -> String name via `state.getItemNames()` `:621-625`. The data carries IDs: `Recommendation.worn` (`Map<EquipmentSlot,Integer>`), `inventory` (`List<Integer>`), `missingUpgrades` (`List<Integer>`) `Recommendation.java:15,16,22`. `ItemManager` is injected in the plugin `AllInSlayerPlugin.java:67` and already called for names `:279` (`itemManager.getItemComposition(id).getName()`), but **icons are never resolved**, and the panel is handed only a `Map<Integer,String>` (`SlayerPanelState.itemNames` `SlayerPanelState.java:20`) so it *cannot* draw an icon even if it wanted to.
3. **Why it feels clunky** - OSRS players recognise gear by sprite instantly and by name slowly. A wall of names forces reading; it looks like a debugging view, not the "what do I wear" answer the Loadout tab promises. It also wastes the panel's strongest native affordance (item icons) that every other item-heavy RuneLite panel uses.
4. **Swing/RuneLite feasibility - MEDIUM (easy data path, some layout work).** `ItemManager.getImage(int itemId)` and `getImage(int itemId, int quantity, boolean stackable)` return `AsyncBufferedImage`. Set it on a `JLabel` with `image.addTo(label)` (or `label.setIcon(image)` then `image.onLoaded(label::repaint)`) so the sprite paints when the async load completes - this is exactly how `LootGrid`/`InventorySetups*Panel` do it. Two implementation options:
   - Minimal: add an icon slot to the existing key/value row (the spec's `KeyValueRow` already lists an "optional icon slot", spec `:667`), so each worn/inventory/upgrade row gets its 32x32 sprite on the left.
   - Better: an equipment-slot grid + inventory grid (model on `InventorySetupsEquipmentPanel`/`LootGrid`) for an at-a-glance loadout.
   - Plumbing: either pass the injected `ItemManager` into `SlayerPanel`, or pre-resolve `Map<Integer, AsyncBufferedImage>` on the client thread next to the existing `collectNames(...)` call (`AllInSlayerPlugin.java:242-252`) and add it to `SlayerPanelState`. Icons must be requested where `ItemManager` is safe (client thread / off-EDT); `AsyncBufferedImage` then repaints itself on the EDT.

### Finding 2 - Action row: three full-width stacked text buttons (vertical bloat, non-native)

1. **What** - Refresh, "Mode: DPS", and Export are three full-width buttons stacked vertically above the tabs, eating roughly a third of the visible panel before any task data appears.
2. **Evidence** - Buttons declared `SlayerPanel.java:55-57`; laid out with `new JPanel(new GridLayout(0, 1, 0, 6))` and added in order `:155-160`; each forced to 30px tall in `styleActionButton` `:398`. Three rows x 30px + 2 x 6px gaps + 2 x 6px outer struts (`:85,:87`) is ~100px of chrome. Buttons are re-skinned `JButton`s via `BasicButtonUI` with a manual hover `MouseAdapter` `:395-424`.
3. **Why it feels clunky** - RuneLite side panels reserve vertical space for content; full-width stacked text buttons are a desktop-form idiom, not a RuneLite idiom. The hand-rolled `BasicButtonUI` + hover listener also doesn't match native control feel.
4. **Swing/RuneLite feasibility - MEDIUM.** Replace the `GridLayout(0,1)` column with a single compact horizontal icon bar (`BorderLayout`/`FlowLayout` row, ~24-28px tall). Use small icon `JButton`s built with `SwingUtil.removeButtonDecorations(button)`, `ImageUtil.loadImageResource(getClass(), "/refresh.png")` (+ a hover/`ImageUtil.luminanceScale` variant), and `setToolTipText(...)`. This is the established Inventory Setups / Quest Helper action-icon pattern (spec already cites it `:216`). Refresh/Export become single icons; mode becomes a segmented control (Finding 6). Net: ~100px -> ~28px.

### Finding 3 - Debug tab shipped as a permanent player-facing product tab

1. **What** - A third tab, "Debug", is always present and shows raw varps, menu actions, mapped item IDs, detector strings.
2. **Evidence** - Tab added unconditionally `SlayerPanel.java:92-95`. Content in `renderDebugTab` `:312-331`: `Status`, `Source`, `Menu option`, `Menu action`, `Raw item id`, `Mapped item id`, `Matched task check`, `Target varp`, `Count varp`, `Area varp`, `Boss target varbit`, `Detector result`, `Unsupported target`.
3. **Why it feels clunky** - This is internal telemetry surfaced as a primary, player-facing tab. It dilutes the panel's purpose and signals "unfinished tool". **Caveat:** this was a deliberate spec decision - the spec explicitly says "The `Debug` tab is part of the product, not a temporary hack" and "do not hide it behind a developer config while the helm/gem update issue is being diagnosed" (spec `:280`, `:725`). So it is *intended*, not a bug. But it is a live contributor to the "clunky/unintuitive" feeling and should be revisited now that the diagnostic phase is presumably ending. This is a product call, flagged for the PM/UX.
4. **Swing/RuneLite feasibility - EASY.** Gate the tab behind a config toggle: only `tabs.addTab(debugTab)` when `config.developerMode()` is true (add a hidden/advanced `@ConfigItem`). Alternatively keep it but move it last and visually subordinate (it already is third). No new APIs needed; the tab content/state already exists, so toggling is low-risk and reversible.

### Finding 4 - HTML labels + manual fixed-width hacks + two competing row systems

1. **What** - Layout is built from `<html><div style='width:Npx'>` labels and hand-computed pixel widths, and the panel uses **two different row renderers** that look subtly different between tabs.
2. **Evidence** -
   - HTML width hacks: `row(...)` wraps every line in `"<html><div style='width:" + CONTENT_WIDTH + "px'>..."` `:435-436`; `wrappedValueLabel(...)` does the same `:517-518`; `section(...)` and `titleText(...)` also emit HTML `:445,:595`.
   - Manual width math: `KEY_WIDTH = 74`, `VALUE_WIDTH = INNER_WIDTH - KEY_WIDTH - 8` `:45-46`; `fixedWidth(...)` forces component widths `:530-536`; bespoke `FixedWidthPanel` overrides `getPreferred/Maximum/MinimumSize` `:738-769`.
   - **Two row systems:** the Task tab uses `keyValueRow(...)` (a `GridBagLayout` two-column row, key as plain label, value as HTML) `:474-505`; the Loadout and Debug tabs use `row(...)` (a single full-width HTML label) `:433-441`. They have different padding, alignment, and key styling.
   - Width bug risk: `row()` sets the inner `<div>` to `CONTENT_WIDTH` (= `PluginPanel.PANEL_WIDTH`, the *full* panel width) `:41,:435` while the label already sits inside a full-width container, so the content can exceed the usable width once the scrollpane border/insets are accounted for.
3. **Why it feels clunky** - Swing's HTML renderer gives inconsistent baselines, font metrics, and spacing versus native labels; mixing two row systems makes the Task tab and Loadout tab look like two different plugins. The manual pixel math is brittle: any font-size or `PANEL_WIDTH` change breaks wrapping/alignment, and the full-width `div` invites horizontal overflow.
4. **Swing/RuneLite feasibility - MEDIUM.** Standardise on one `KeyValueRow` component for all three tabs (the spec already names `KeyValueRow` `:659-669`). Prefer real layout over HTML: `JLabel` with plain text for keys/values inside a `GridBagLayout` (or RuneLite's `DynamicGridLayout`) and let `PluginPanel`'s fixed width plus the existing `JScrollPane` (horizontal scrollbar already disabled `:347`) handle bounds. Where genuine multi-line wrap is needed (long task/method text), use a single consistent wrap mechanism (e.g. a non-editable, line-wrapping `JTextArea` styled to match, or one shared HTML helper) rather than per-call inline `<div style>`. Remove `FixedWidthPanel`/`fixedWidth` once components derive width from the layout.

### Finding 5 - Header shows developer language ("Source: STARTUP | Updated: ...")

1. **What** - The header's second line reads e.g. `Source: MENU_CHECK | Updated: 21:48:12`, exposing internal enum constants.
2. **Evidence** - `taskMeta` initialised to `"Source: STARTUP"` `:53`; rebuilt every render as `"Source: " + state.getRefreshSource() + " | Updated: " + formatTime(state)` `:113`. `getRefreshSource()` returns a `RefreshSource` enum whose `toString()` is the raw constant (`STARTUP`, `VARBIT`, `ITEM_CONTAINER`, `MENU_CHECK`, `GAME_STATE`, `MODE_TOGGLE`, `LOCATION_SELECT`) `RefreshSource.java:4-14`.
3. **Why it feels clunky** - "ITEM_CONTAINER" / "VARBIT" / "MODE_TOGGLE" are meaningless to a player and read as a leaked internal state machine. The word "Source:" is itself developer framing. The spec's own mock-up wanted humane phrasing - `Updated: 21:48:12 from helm check` (spec `:336-343`) - which was never implemented.
4. **Swing/RuneLite feasibility - EASY.** Add a `displayName` to `RefreshSource` (or a small `switch`/map in the panel) -> "manual", "helm check", "task changed", "bank update", "startup", "mode change", "location change". Render `Updated 21:48 - helm check`. Pure string mapping, no API needed. Consider hiding the source entirely in the player-facing header once the Debug tab carries the raw enum (it already does, `:318`).

### Finding 6 - "Mode: DPS" full-width button hides that it is a toggle

1. **What** - Mode switching is a single button labelled `Mode: DPS` that relabels itself to `Mode: Cost` when pressed.
2. **Evidence** - `modeToggle = new JButton("Mode: DPS")` `:56`; text updated each render `modeToggle.setText("Mode: " + state.getMode())` `:115`; same full-width `GridLayout` slot as the other buttons `:158`.
3. **Why it feels clunky** - A button that shows only the *current* value gives no signal that it toggles or what the alternative is. The player must click and observe to learn there is a second mode. It also competes visually with the real actions (Refresh/Export).
4. **Swing/RuneLite feasibility - MEDIUM.** Replace with a two-segment selector showing both options with the active one highlighted in `ColorScheme.BRAND_ORANGE`: either two `JToggleButton`s in a `ButtonGroup` (`DPS | Cost`), or a 2-tab `MaterialTabGroup` used as a segmented control. Both make the choice and the current state visible at a glance and free the action row for icons. Wiring is unchanged - it still calls `onToggleMode` (`AllInSlayerPlugin.java:87-91`).

### Finding 7 - Prominent manual "Refresh" the panel almost never needs

1. **What** - A primary, full-width Refresh button sits at the top of the action stack.
2. **Evidence** - `refreshButton = new JButton("Refresh")` `:55`, first in the action column `:157`; callback runs `recompute(RefreshSource.MANUAL)` `AllInSlayerPlugin.java:86`. The panel already auto-recomputes on essentially every relevant event: startup `:110`, varbit `:125`, chat `:134`, menu check `:153`, item container (bank/inv/worn) `:169`, and game state `:176`.
3. **Why it feels clunky** - Given the event coverage above, Refresh is almost always redundant; presenting it as a top-level primary action implies the player must press it to get current data, which undermines confidence that the panel is live. It reads as a leftover crutch from when updates were unreliable (the very problem the Debug tab was added to diagnose).
4. **Swing/RuneLite feasibility - EASY.** Keep it as a small icon (a circular-arrow refresh glyph) in the compact icon bar from Finding 2, not a full-width primary control - a fallback, not the headline. No behaviour change, just demotion. (Recommend keeping it: it is still a useful manual nudge and the spec requires a manual refresh control, spec `:93-94`.)

### Finding 8 - Text-only tab strip

1. **What** - The Task/Loadout/Debug tabs are text-only `MaterialTab`s.
2. **Evidence** - `new MaterialTab("Task", tabs, ...)`, `"Loadout"`, `"Debug"` `:90-92`.
3. **Why it feels clunky** - Minor. Text tabs are fine and legible; they are simply less polished than the icon tabs used by larger plugins. This was a deliberate spec choice for the debugging phase (spec `:363` "Text tabs are clearer during the current debugging phase").
4. **Swing/RuneLite feasibility - MEDIUM (gated on assets).** `MaterialTab` has an `ImageIcon` constructor; `DimmableMaterialTab` dims inactive icon tabs natively. Needs icon resources via `ImageUtil.loadImageResource(...)`. Low priority versus Findings 1-3; revisit after the loadout/action-bar work and only if Debug is also being de-emphasised.

### Finding 9 - Full teardown + revalidate of all three tabs on every event (perceived responsiveness)

1. **What** - Every render rebuilds all three tab contents from scratch and revalidates/repaints the whole panel, and recompute fires on far more events than necessary.
2. **Evidence** -
   - `render(...)` rebuilds all tabs every call and then `revalidate(); repaint();` on the root `:118-123`.
   - Each tab renderer does `removeAll()` then recreates every child: `renderTaskTab` `:165`, `renderLoadoutTab` `:267`, `renderDebugTab` `:314`.
   - The location `JComboBox` is reconstructed on every render (`buildLocationPanel` -> `locationSelector(...)` `new JComboBox<>()` `:254,:353-378`), and its action listener triggers `selectLocation` -> `onSelectLocation` -> `recompute(LOCATION_SELECT)` -> another full render.
   - `onGameStateChanged` calls `recompute(RefreshSource.GAME_STATE)` on **every** `GameStateChanged` with no filter `AllInSlayerPlugin.java:174-177`, so logging in/out, loading, and connection blips all churn a full rebuild and flip the header `Source` to `GAME_STATE`.
3. **Why it feels clunky** - Tearing down and rebuilding the entire component tree on every event causes flicker, can drop the combo box's open/popup and focus state mid-interaction, and risks scroll jumps. The unfiltered `GameStateChanged` recompute makes the header's "Source/Updated" line flicker and wastes work. Together they make the panel feel twitchy rather than live-and-stable.
4. **Swing/RuneLite feasibility - MEDIUM.** Threading itself is correct - reads are on the client thread and renders are marshalled to the EDT via `SwingUtilities.invokeLater(() -> panel.render(state))` `AllInSlayerPlugin.java:212,:266`, matching spec FR. Improvements: (a) filter `onGameStateChanged` to states that matter (e.g. `GameState.LOGGED_IN`); (b) only rebuild the tab whose data changed, or update existing components in place instead of `removeAll()`+recreate (the partial re-render already done in `selectLocation` `:380-393` is the right pattern to generalise); (c) build/reuse the `JComboBox` once and only update its model/selection rather than recreating it each render. All standard Swing; no new RuneLite API.

---

## Direct answers to the specific items called out in the brief

- **Action row = 3 full-width stacked text buttons via `GridLayout(0,1)` (Refresh / "Mode: DPS" / Export):** CONFIRMED. `SlayerPanel.java:55-57` (buttons), `:155-160` (`GridLayout(0,1,0,6)`), `:398` (30px each). ~100px of vertical chrome, non-native. See Finding 2.
- **Debug tab shipped as a permanent player-facing product tab:** CONFIRMED. `:92-95`, content `:312-331`. Note: this is per the approved spec (`:280`, `:725`), so intended, not accidental. See Finding 3.
- **Loadout as plain TEXT rows with NO item sprites despite IDs being available; is ItemManager available?:** CONFIRMED, and YES `ItemManager` is available. Text rows `:284,:292,:303`; IDs present in `Recommendation` (`worn`/`inventory`/`missingUpgrades`); `ItemManager` injected `AllInSlayerPlugin.java:67` and already used for names `:279` but never for icons; panel only receives `Map<Integer,String>` so it has no image to draw. See Finding 1.
- **Heavy `<html><div style='width:...'>` labels and manual fixed-width / `FixedWidthPanel` hacks:** CONFIRMED. `:435-436,:517-518,:445,:595` (HTML), `:45-46,:530-536` (width math), `:738-769` (`FixedWidthPanel`). Plus two competing row systems (`row()` vs `keyValueRow()`). See Finding 4.
- **Header showing developer language ("Source: STARTUP | Updated: ..."):** CONFIRMED. `:53,:113`; raw enum names from `RefreshSource`. See Finding 5.
- **"Mode: DPS" as a full-width toggle button vs a clearer segmented toggle:** CONFIRMED. `:56,:115,:158`. See Finding 6.
- **Manual "Refresh" as a primary action - does the panel already react to events?:** CONFIRMED redundant-as-primary. Auto-recompute on startup/varbit/chat/menu/item-container/game-state (`AllInSlayerPlugin.java:110,125,134,153,169,176`). Refresh should be demoted to an icon. See Finding 7.
- **Tab strip text-only; are icon tabs feasible?:** CONFIRMED text-only `:90-92`. Icon tabs feasible via `MaterialTab(ImageIcon,...)`/`DimmableMaterialTab` but need assets; low priority. See Finding 8.
- **EDT/threading or revalidate/repaint smells affecting perceived responsiveness:** Threading is CORRECT (client-thread reads, EDT renders via `invokeLater` `:212,:266`). Smells are full teardown+revalidate of all tabs every render (`:118-123,:165,:267,:314`), combo box recreated each render (`:254,:353`), and unfiltered `onGameStateChanged` recompute (`:174-177`). See Finding 9.

## Suggested priority for the redesign plan (impact x effort)

1. Finding 1 - item sprites in Loadout (medium effort, highest perceived-quality gain).
2. Finding 2 + 6 + 7 - collapse the three stacked buttons into one compact icon bar with a segmented DPS/Cost control and a demoted Refresh icon.
3. Finding 5 - humanise the header source text (easy win).
4. Finding 4 - unify on one `KeyValueRow`, drop HTML/fixed-width hacks (consistency + durability).
5. Finding 3 - gate the Debug tab behind a dev config (product decision; easy once approved).
6. Finding 9 - partial re-render + filter `GameStateChanged` (responsiveness polish).
7. Finding 8 - icon tabs, only after assets exist.

Note on the overlay (`SlayerOverlay.java`): it is small and uses native `OverlayPanel`/`TitleComponent`/`LineComponent` correctly `:36-50`; not a source of clunk. No action recommended beyond keeping it fed from the same state (already done `AllInSlayerPlugin.java:237-240`).

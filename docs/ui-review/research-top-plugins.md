# Top RuneLite Plugin Hub Plugins — UI/UX Research

**Purpose:** Identify the most-installed RuneLite Plugin Hub plugins and study the UIs of those with
notable side panels / overlays, to compare against the All-In Slayer assistant side panel
(`src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java`).

**Author:** research analyst (RuneLite UI/UX review)
**Date:** 2026-06-29

## Sourcing & confidence

- **Install counts are VERIFIED LIVE.** Pulled today (2026-06-29) from the same JSON endpoint the
  Plugin Hub site uses: `https://api.runelite.net/pluginhub` (a `{internalName: activeInstalls}` map,
  2,130 plugins, 28,412,697 total active installs). I downloaded and sorted the raw JSON myself, so the
  ranking and numbers below are exact as of today. They drift daily.
- The third-party tracker `https://runelite.phyce.dev/top/absolute` corroborates the same ordering
  (it is JS-rendered so it could not be scraped directly, but its numbers matched the API to within
  daily churn).
- **Repo mappings are VERIFIED** from the plugin-hub manifests at
  `https://raw.githubusercontent.com/runelite/plugin-hub/master/plugins/<internalName>`.
- **Code citations are VERIFIED** — I shallow-cloned 5 repos and read the source. Line numbers are from
  current `master` HEAD (cloned 2026-06-29); the plugin-hub pins older commits, so a line may drift by a
  few. File paths are stable.
- **Panel/Overlay classification:** VERIFIED for the 5 plugins I read in depth (Quest Helper, Inventory
  Setups, Party Panel, Banked Experience, Equipment Inspector). For the rest it is INFERENCE from the
  manifest one-liner + community knowledge and is flagged "(inferred)". A few newer content plugins
  (Sailing, Port Tasks, Mastering Mixology) I could not confirm the exact UI surface for — flagged
  "(unverified)".

---

## Section 1 — Top plugins by install count

Top 20 by active installs (live, 2026-06-29), plus 6 lower-ranked plugins pulled up because they have
panels directly comparable to ours (marked ★). "Panel" = `PluginPanel` side panel; "Overlay" =
in-scene/infobox `Overlay`.

| # | Plugin (internalName) | Installs | Purpose (1-line) | Side panel? | Overlay? |
|---:|---|---:|---|---|---|
| 1 | quest-helper | 565,158 | Step-by-step quest guidance | **Yes — rich** (verified) | Yes (world arrows/highlights) |
| 2 | 117hd | 394,721 | GPU renderer + graphics enhancements | No | No (config only) |
| 3 | sailing | 351,729 | Sailing skill utilities | No (unverified) | Likely (unverified) |
| 4 | tile-packs | 351,637 | Importable tile-marker collections | No | Via built-in Ground Markers |
| 5 | guardians-of-the-rift-helper | 349,706 | GOTR minigame info | No (inferred) | **Yes** (inferred) |
| 6 | tombs-of-amascut | 314,226 | ToA raid utilities/info | No (inferred) | **Yes** (inferred) |
| 7 | wikisync | 310,126 | Syncs player data to the wiki | No | No (headless) |
| 8 | better-npc-highlight | 296,927 | Customizable NPC highlighting | No (config) | **Yes** (inferred) |
| 9 | zulrah-helper | 276,773 | Zulrah rotation guidance | **Yes** (inferred) | Yes (inferred) |
| 10 | the-gauntlet | 273,190 | Gauntlet all-in-one helper | No (inferred) | **Yes** (inferred) |
| 11 | mahogany-homes | 254,099 | Mahogany Homes contract helper | No (inferred) | **Yes** + infobox (inferred) |
| 12 | tempoross | 249,635 | Tempoross helper | No (inferred) | **Yes** (inferred) |
| 13 | party-panel | 240,207 | Party members' live stats/gear | **Yes — rich** (verified) | Small reminder overlay |
| 14 | rogues-den | 237,979 | Rogues' Den maze solver | No (inferred) | **Yes** (inferred) |
| 15 | easy-giantsfoundry | 236,487 | Giants' Foundry helper | No (inferred) | **Yes** (inferred) |
| 16 | port-tasks | 233,905 | Task tracker (unverified content) | Unverified | Unverified |
| 17 | mastering-mixology | 233,664 | Mastering Mixology helper | No (inferred) | **Yes** (unverified) |
| 18 | banked-experience | 233,289 | Banked-XP calculator | **Yes — rich** (verified) | No |
| 19 | bank-tag-layouts | 231,152 | Custom drag-drop bank tab layouts | No | In-game bank widget |
| 20 | fight-cave-waves | 227,986 | Fight Caves wave/spawn info | No (inferred) | **Yes** + infobox (inferred) |
| ★24 | inventory-setups | 215,840 | Save/restore gear+inv loadouts | **Yes — rich** (verified) | Bank-tab highlight |
| ★27 | equipment-inspector | 203,550 | Inspect a player's worn gear | **Yes** (verified) | No |
| ★28 | shortest-path | 197,033 | Pathfinder to any tile | **Yes** (control panel, inferred) | Yes (path on map/scene) |
| ★39 | loot-lookup | 142,957 | NPC drop tables in-client | **Yes** (inferred) | No |
| ★47 | collection-log | 128,286 | Collection-log mirror w/ item grid | **Yes** (inferred; **disabled=unmaintained**) | No |
| ★51 | c-engineer-completed | 118,980 | Skill/levelup completion images | No | **Yes** (decorative) |

Takeaways for classification:
- **Most top plugins are overlay/infobox-first** (boss & minigame helpers). True rich *side panels* are a
  minority but are exactly the cohort our plugin belongs to.
- The directly-comparable "assistant dashboard" side panels are: **Quest Helper, Inventory Setups, Party
  Panel, Banked Experience, Equipment Inspector** (all verified below), plus Zulrah Helper / Shortest
  Path / Collection Log (panels, not read in depth).

---

## Section 2 — Deep UI/UX patterns (verified from source)

GitHub blob links point at `master`; line numbers are from the HEAD I cloned 2026-06-29.

### 2.1 Quest Helper — the gold-standard assistant dashboard
Repo: https://github.com/Zoinkwiz/quest-helper · `src/main/java/com/questhelper/panel/`

- **Compact icon action bar in the title row.** The header is a `BorderLayout`: title text WEST, a
  `GridLayout(1, N)` of borderless **icon buttons** EAST (Settings / Discord / GitHub / Patreon). Each
  button is built with `SwingUtil.removeButtonDecorations(btn)` + `setUI(new BasicButtonUI())` +
  `setIcon(16x16)` + `setToolTipText(...)` + a `MouseAdapter` that swaps background to
  `ColorScheme.DARK_GRAY_HOVER_COLOR` on hover. `QuestHelperPanel.java:164-279`.
  → This is the canonical alternative to stacked full-width text buttons.
- **Search + filter row.** `IconTextField searchBar` with `IconTextField.Icon.SEARCH`, hover colour, and a
  `DocumentListener` that filters live as you type — no submit button. `QuestHelperPanel.java:289-312`.
  Filtering is done with `JComboBox` dropdowns built by helper factories (`makeNewDropdown` /
  `makeDropdownPanel`). `QuestHelperPanel.java:326-336`.
- **List ↔ detail via `CardLayout`.** One `viewportContent` panel with a `CardLayout` flips between
  `quest_list`, `quest_overview`, and `settings` views; `getPreferredSize()` is overridden to return the
  *visible* card's size so the scrollpane sizes correctly. `QuestHelperPanel.java:94-128`. (We currently
  use `MaterialTabGroup` — fine, but Card/list-detail is the pattern for "pick a thing → see its detail".)
- **Requirement rows recolor by live game state (NOT a refresh button).** `QuestRequirementsPanel`
  keeps a `List<InlineRequirement>` of (requirement, label, infoButton). An `update(client, plugin)`
  method recomputes each label's colour via `req.getColor(client, config)` (met = green, unmet = red) and
  hides rows that don't apply. `QuestRequirementsPanel.java:258-321`. It is driven from
  `onGameTick`/state changes upstream (see `QuestHelperPlugin.java:270+`, and the `update(...)` fan-out in
  `QuestOverviewPanel.java:638-641` and `QuestStepPanel.java:640-645`). **The panel reacts to events; the
  player never presses "refresh".**
- **Per-row right-click context menu + clickable links.** Each requirement row gets a `JPopupMenu` with
  "Go to wiki.." (`LinkBrowser.browse`), "Open quest helper..", or "Go to NER.." depending on type; the
  label underlines on hover when it's a clickable cross-link. `QuestRequirementsPanel.java:142-241`.
- **Tiny inline info button** for tooltips: a `JButton(INFO_ICON)` sized 10×10 with all decorations off
  (`setBorderPainted(false)`, `setContentAreaFilled(false)`), shown only when a tooltip exists.
  `QuestRequirementsPanel.java:323-341`.
- **Collapsible sections.** Step/section panels implement `MouseListener` on a bold header
  (`FontManager.getRunescapeBoldFont()`); clicking toggles `bodyPanel.setVisible(...)` and swaps a
  collapse/expand chevron icon. `QuestStepPanel.java:96-132,604-617`. The *active* section header is
  painted with `ColorScheme.BRAND_ORANGE` to draw the eye (`QuestSectionSection.java:114-116`).

### 2.2 Inventory Setups — the item-grid + icon-toolbar gold standard
Repo: https://github.com/dillydill123/inventory-setups · `src/main/java/inventorysetups/ui/`

- **Item slot = `JPanel` with an image `JLabel` + corner indicators.** `InventorySetupsSlot` is a
  `GridBagLayout` panel: the item sprite fills it, a "fuzzy" `*` indicator pins NORTH-EAST and a stack
  indicator pins SOUTH-EAST, both in `FontManager.getRunescapeSmallFont()`.
  `InventorySetupsSlot.java:81-146`.
- **Sprites via `ItemManager.getImage(id, qty, stackable)` → `AsyncBufferedImage.addTo(label)`.** The
  async image repaints the label itself when the sprite finishes loading — no manual icon plumbing.
  `InventorySetupsSlot.java:148-169,338-359`.
- **Slot highlighting communicates state by background colour** (item present-but-wrong vs match), with a
  reset to `ColorScheme.DARKER_GRAY_COLOR`. `InventorySetupsSlot.java:362-403`.
- **Icon "marker" action bar** (a JLabel variant of the icon toolbar): import / add / update / back /
  section-toggle / sort are `JLabel(ICON)` with `setToolTipText`, a hover icon swap
  (`IMPORT_ICON ↔ IMPORT_HOVER_ICON`) and an attached `JPopupMenu`. `InventorySetupsPluginPanel.java:
  350-409` (and the toolTip set list at `:246,351,382,424,450,476,901,913`).
- **Live search** via `IconTextField` (`InventorySetupsPluginPanel.java:541-542`) and **empty state** via
  `PluginErrorPanel` (`:600`).
- **Right-click power-menus per slot**, incl. shift-right-click for "apply to ALL setups" with a
  `JOptionPane` confirm. `InventorySetupsSlot.java:99-128,184-318`.

### 2.3 Party Panel — native-looking inventory/equipment mirrors
Repo: https://github.com/TheStonedTurtle/party-panel · `src/main/java/thestonedturtle/partypanel/`

- **Inventory rendered as the real 4-wide grid.** `DynamicGridLayout(7, 4, 2, 2)`, 28 `JLabel` slots,
  each `itemManager.getImage(id, qty, stackable).addTo(label)`; empty slots are blank labels so the grid
  shape is preserved. `ui/PlayerInventoryPanel.java:55-104`.
- **Rich HTML tooltips** — e.g. a rune pouch shows its contents as multi-line `<html>...<br>...</html>`.
  `ui/PlayerInventoryPanel.java:106-123`.
- **Equipment slots composite the item over the native slot background.** `EquipmentPanelSlot` draws the
  sprite centered on the equipment-slot background image (and a placeholder when empty) via a small
  `ImgUtil.overlapImages(...)` helper, resized to 48px. `ui/equipment/EquipmentPanelSlot.java:36-74`,
  `ImgUtil.java:55-77`. This is what makes it feel like the in-game tab rather than a web list.

### 2.4 Banked Experience — selectable item grid with state colours
Repo: https://github.com/TheStonedTurtle/banked-experience · `src/main/java/.../components/`

- **`GridItem extends JLabel`** = one selectable item icon. `icon.addTo(this)` for the sprite; a rich
  `<html>` tooltip lists item name + xp/action + total xp; selection / ignore / RNG states are encoded as
  distinct background colours with matching hover colours (`SELECTED_BACKGROUND = new Color(0,70,0)`,
  `IGNORED_BACKGROUND = new Color(90,0,0)`, etc.). `components/GridItem.java:55-99,165-202`.
- **`SelectionGrid extends JPanel`** lays the items out with `GridLayout(rows, 5, 1, 1)` computed from
  item count, recreated from `ItemManager` on data change. `components/SelectionGrid.java:48-137`.
- **Right-click `JPopupMenu`** per item for Ignore / Include-all / Ignore-all. `GridItem.java:134-162`.
- Also has an `ExpandableSection` component using `removeButtonDecorations` (collapsible) and
  `FontManager` fonts — consistent with the others.

### 2.5 Equipment Inspector — the cleanest "icon + text + price" row (closest to our Loadout tab)
Repo: https://github.com/botanicvelious/Equipment-Inspector · `src/main/java/equipmentinspector/ItemPanel.java`

- **One row = `GroupLayout`: sprite LEFT, stacked `name / slot / price` text RIGHT.** Sprite via
  `icon.addTo(imageLabel)`; text in `FontManager.getRunescapeFont()` / `getRunescapeSmallFont()`.
  `ItemPanel.java:29-95`.
- **Colour-coded GE price label** (green > 10M, white > 100k, yellow otherwise, light-grey if 0) with a
  full-number tooltip via `QuantityFormatter`. `ItemPanel.java:97-112`.
- **Right-click → "Wiki"** opens `oldschool.runescape.wiki/w/Special:Lookup?...&utm_source=runelite` via
  `LinkBrowser`. `ItemPanel.java:44-58`.
- A **`TotalPanel`** row sums values at the bottom. `ItemPanel.java:114-137`.
  → This single 137-line file is essentially a drop-in template for turning our text-only Loadout rows
  into sprite rows with price + wiki lookup.

### Cross-plugin idiom adoption (verified by grep across the 5 repos)
- `ItemManager.getImage(...)` + `AsyncBufferedImage.addTo(label)` for every item icon: **all 5**.
- `FontManager` RuneScape fonts for native look: **all 5** (15+ files).
- `SwingUtil.removeButtonDecorations` for icon buttons/expanders: Quest Helper, Banked Experience
  (Inventory Setups uses the `JLabel`-marker variant instead).
- `IconTextField` live search: Quest Helper, Inventory Setups.
- `PluginErrorPanel` empty/error state: Inventory Setups (and it's the RuneLite-standard for empties).
- `LinkBrowser` to wiki/Discord/GitHub: Quest Helper, Equipment Inspector.
- **None of the five uses a manual "Refresh" button** — all rebuild/recolor from game events.

---

## Section 3 — Patterns to adopt in the All-In Slayer panel (prioritised)

Context: today `SlayerPanel.java` renders everything as plain-text HTML `JLabel` rows
(`row()` :433, `keyValueRow()` :474), stacks three **full-width text buttons** in a
`GridLayout(0,1)` (Refresh / Mode / Export, :155-160), exposes a player-facing **Debug tab**
(:312-331), and relies on a manual **Refresh** button. The top plugins suggest, in order of impact:

1. **Render items as sprites, not text.** Replace `itemName(...)` text rows in the Loadout tab (and the
   "Required" item on the Task tab) with icon rows. Use
   `itemManager.getImage(id, qty, stackable).addTo(label)` — the universal idiom. Model the row on
   Equipment Inspector's `ItemPanel` (sprite + name + GE price + wiki right-click) or, for the gear
   set, on Party Panel's equipment-slot grid. **This is the single biggest visual upgrade** and the
   thing every comparable top panel does that we don't.

2. **Replace the stacked full-width text buttons with a compact icon action bar in the header.** Put
   Refresh / Mode / Export as borderless 16px icon buttons in a title row (Quest Helper:
   `removeButtonDecorations` + `BasicButtonUI` + tooltip + hover-colour; or Inventory Setups' JLabel-icon
   markers). Frees vertical space and reads as native.

3. **React to game events instead of a manual Refresh.** Quest Helper, Inventory Setups, Party Panel and
   Banked Experience all rebuild/recolor from ticks/varbits. Keep Refresh only as an optional manual
   override; drive normal updates from the existing task-detection events so the panel is always current.

4. **Encode state with colour on the row, sparingly.** Reserve the brand colour for *real* state. Mirror
   Quest Helper's met/unmet colouring (green = owned/requirement satisfied, red/muted = missing) on
   loadout/required rows, and Banked Experience's distinct background colours for selected vs ignored —
   rather than using `BRAND_ORANGE` decoratively on every section heading (current `section()` :443-449).

5. **Hide the Debug tab from players.** None of the top plugins ship a player-facing debug surface. Gate
   it behind a config flag (developer mode) so the default player sees only Task + Loadout.

6. **Use `PluginErrorPanel` for every empty/error state** (we already do for "No task" :573 — extend it
   consistently to the Loadout empty state and any "unsupported" state) and **`FontManager`** fonts for a
   native feel.

7. **Add per-row right-click + wiki lookup.** Item rows should offer "Wiki" via `LinkBrowser` to
   `Special:Lookup` (Equipment Inspector pattern) — cheap, high-utility, and expected by players.

8. **Adopt collapsible sections** for long content (Quest Helper / Banked Experience): a bold header that
   toggles `body.setVisible(...)` with a chevron icon, so Task / Location / Loadout can each collapse.

9. **Live search/filter** is lower priority for a single-task panel, but if the location list or a future
   task browser grows, use `IconTextField` (Quest Helper / Inventory Setups) rather than a bare combo.

---

## Appendix — methodology / reproducibility

- Live counts: `curl https://api.runelite.net/pluginhub` → JSON map of `{internalName: installs}`, sorted
  descending (2,130 entries; total 28,412,697; top entry quest-helper 565,158), fetched 2026-06-29.
- Repo mappings: `https://raw.githubusercontent.com/runelite/plugin-hub/master/plugins/<internalName>`.
- Code read from shallow clones of: Zoinkwiz/quest-helper, dillydill123/inventory-setups,
  TheStonedTurtle/party-panel, TheStonedTurtle/banked-experience, botanicvelious/Equipment-Inspector.
- Sources: https://runelite.net/plugin-hub/ · https://runelite.phyce.dev/top/absolute ·
  https://github.com/runelite/plugin-hub/tree/master/plugins
</content>
</invoke>

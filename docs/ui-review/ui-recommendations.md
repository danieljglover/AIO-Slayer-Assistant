# All-In Slayer - UI / Visual Recommendations & Component Specs (Board UI1)

- Author: UI / Visual Designer
- Date: 2026-06-29
- Scope: The **visual language** and **reusable component contracts** an engineer can build from
  without guessing. Sizes in px, colours as `ColorScheme` constants (+ verified RGB/hex), fonts as
  `FontManager` methods. References the audit by Finding number and research by plugin/file.
- Inputs: `docs/ui-review/current-ui-audit.md` (9 findings), `docs/ui-review/research-top-plugins.md`
  (component patterns), `src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java`,
  `loadout/Recommendation.java`, `model/EquipmentSlot.java`, `ui/RefreshSource.java`, `ui/PanelStatus.java`.

## Ownership boundary (read first)

I own **how it looks** and the **reusable component contracts** (tokens, sprite rows/grids, the icon
bar, the row system, colour rules, header chrome). The **UX Designer owns IA/flow/interaction**: which
tabs exist, whether worn gear is a grid or a list as the *primary* view, collapsible-section behaviour,
and empty-state journeys (`docs/ui-review/ux-recommendations.md`, board UX1 - **not yet on disk at time
of writing**). Where a layout choice depends on IA I mark it **[pending UX]** and give the visual spec
for whichever way UX lands. I align to their structure; I do not redefine it.

---

## 0. Verified API & version facts (load-bearing - correction inside)

`build.gradle:13,16` pins `net.runelite:client:latest.release`, which **resolves on this machine to
`client-1.12.31.1`** (gradle cache). Every API and value below was read from that jar's bytecode, not
assumed.

| Fact | Verified value / signature | Note |
|---|---|---|
| Item sprite | `ItemManager.getImage(int id)`, `getImage(int id, int qty, boolean stackable)` -> `AsyncBufferedImage` | async, repaints itself |
| Sprite onto label | `AsyncBufferedImage.addTo(JLabel)` **and** `addTo(JButton)` | both exist; no manual `onLoaded` needed |
| Icon button strip | `SwingUtil.removeButtonDecorations(AbstractButton)` | confirmed |
| Icon load | `ImageUtil.loadImageResource(Class, String)` | from `/resources` |
| Icon hover/disabled variants | `ImageUtil.luminanceScale(Image,float)`, `luminanceOffset(Image,int)`, `grayscaleImage(BufferedImage)`, `alphaOffset(Image,int)`, `recolorImage(Image,Color)` | generate variants in code; **no extra art needed** |
| Price text | `QuantityFormatter.quantityToRSDecimalStack(int)` (compact), `formatNumber(long)` (full, for tooltip) | confirmed |
| Grid layout | `DynamicGridLayout()`, `(rows,cols)`, `(rows,cols,hgap,vgap)` | use for gear/inventory grids |
| Icon tabs | `MaterialTab(ImageIcon, MaterialTabGroup, JComponent)` exists | feasible |
| **CORRECTION** | **`DimmableMaterialTab` does NOT exist in client 1.12.31.1** | audit Finding 8 and the brief both named it; **it is not in the jar**. Only `MaterialTab`/`MaterialTabGroup` ship. `DimmableJPanel` exists but is unrelated. Inactive icon-tab dimming must be hand-rolled (a pre-dimmed `ImageIcon` via `grayscaleImage`/`alphaOffset`, swapped on select/unselect). This changes the cost calculus for icon tabs - see Section 6. |
| Panel width | `PluginPanel.PANEL_WIDTH = 225`, `BORDER_OFFSET = 6`, `SCROLLBAR_WIDTH = 17` | usable inner width derives from layout, not manual math |
| Fonts | `FontManager.getRunescapeFont()`, `getRunescapeSmallFont()`, `getRunescapeBoldFont()` | fixed-size bitmap RS fonts; set the role, never an arbitrary point size |

---

## 1. Design tokens (system first - every value below resolves to one of these)

### 1.1 Colour tokens (semantic role -> `ColorScheme` constant -> exact RGB / hex)

All RGB values read from `ColorScheme` static initialiser in `client-1.12.31.1`.

| Token (semantic) | `ColorScheme` constant | RGB | Hex | Use |
|---|---|---|---|---|
| `surface/page` | `DARK_GRAY_COLOR` | 40,40,40 | `#282828` | panel root, action bar background, gaps between cards |
| `surface/card` | `DARKER_GRAY_COLOR` | 30,30,30 | `#1E1E1E` | section cards, rows, grid cells (recessed wells) |
| `surface/card-hover` | `DARKER_GRAY_HOVER_COLOR` | 60,60,60 | `#3C3C3C` | hover on an **interactive** content row (clearly visible) |
| `surface/icon-hover` | `DARK_GRAY_HOVER_COLOR` | 35,35,35 | `#232323` | hover on an icon button sitting on `surface/page` (subtle, native) |
| `border/divider` | `BORDER_COLOR` | 23,23,23 | `#171717` | card borders, hairline dividers |
| `text/primary` | `TEXT_COLOR` | 198,198,198 | `#C6C6C6` | item names, values, body |
| `text/secondary` | `LIGHT_GRAY_COLOR` | 165,165,165 | `#A5A5A5` | keys, captions, slot labels, meta |
| `text/muted` | `MEDIUM_GRAY_COLOR` | 77,77,77 | `#4D4D4D` | disabled, tertiary, "you don't own this yet" |
| `accent/brand` | `BRAND_ORANGE` | 220,138,0 | `#DC8A00` | **reserved for real state only** (Section 4) |
| `state/met` | `PROGRESS_COMPLETE_COLOR` | 55,240,70 | `#37F046` | requirement met (e.g. Slayer level satisfied) - sparing |
| `state/blocked` | `PROGRESS_ERROR_COLOR` | 230,30,30 | `#E61E1E` | required item **missing** (blocks the task) |
| `price/high` | `GRAND_EXCHANGE_PRICE` | 110,225,110 | `#6EE16E` | GE value > 10M |
| `price/mid` | `GRAND_EXCHANGE_ALCH` | 240,207,123 | `#F0CF7B` | GE value 100k - 10M |

### 1.2 Type tokens (role -> `FontManager` method)

| Token | Method | Use |
|---|---|---|
| `type/title` | `getRunescapeBoldFont()` | task name in header, section headers |
| `type/body` | `getRunescapeFont()` | item names, primary values |
| `type/caption` | `getRunescapeSmallFont()` | keys, slot labels, price, meta, chips/tags |

Rule: never `setFont(new Font(...))` with an arbitrary size; these three methods are the whole scale.
Replaces the current `<html><b>` weight hacks (audit Finding 4) - weight comes from the bold *token*.

### 1.3 Spacing scale (px) - replaces ad-hoc 2/3/4/6/8 math

`space-1 = 2` · `space-2 = 4` · `space-3 = 6` · `space-4 = 8` · `space-5 = 12`

| Where | Token |
|---|---|
| intra-row / sprite-to-text micro gap | `space-1` (2) |
| chip/tag internal gap, grid cell gap | `space-2` (4) |
| row vertical padding, inter-section gap | `space-3` (6) |
| card inner padding, key->value column gap | `space-4` (8) |
| between major stacked sections | `space-5` (12) |

### 1.4 Sizing tokens

| Token | Value | Notes |
|---|---|---|
| `size/sprite` | **36 x 32** | native `ItemManager` image size; use as-is, **do not rescale** (rescaling blurs) |
| `size/grid-cell` | 38 x 34 | `size/sprite` + 1px breathing room each side |
| `size/icon` | 16 x 16 | action-bar glyphs |
| `size/icon-button` | 24 x 24 | 16px icon + 4px inset each side |
| `size/action-bar` | height 24 | one compact row |
| `size/row` | height 22 | one `KeyValueRow` |
| `size/key-col` | width 64 | key column in `KeyValueRow` (was 74; tighter, value gets the room) |
| `size/segment` | height 22 | DPS/Cost toggle |
| `size/tag` | height 16 | chip/pill |
| `width/panel` | 225 | `PluginPanel.PANEL_WIDTH`; **derive inner widths from layout managers, never hardcode** (kills `FixedWidthPanel`, `fixedWidth()`, `KEY_WIDTH/VALUE_WIDTH` math, and every `<html><div style='width:Npx'>` - audit Finding 4) |

---

## 2. Item-sprite row / grid (audit Finding 1 - highest impact)

Today the Loadout tab is a wall of `Item: Abyssal whip` strings with zero sprites
(`SlayerPanel.java:284,292,303`), even though every item id is in `Recommendation`
(`worn: Map<EquipmentSlot,Integer>`, `inventory: List<Integer>`, `missingUpgrades: List<Integer>`).
This is the single biggest "spreadsheet not a game tool" offender. Two reusable components solve it.

### 2.1 `LoadoutItemRow` (the primary item component - model: Equipment Inspector `ItemPanel.java:29-112`)

One row = sprite LEFT + a stacked text block RIGHT. This is the closest match to our data (name +
slot + price + state) and the cleanest template in the research.

**Anatomy**
```
+------------------------------------------------------+
| [ 36x32 ] <name, type/body, text/primary>            |
| [ sprite] <slot label · price>  type/caption         |
+------------------------------------------------------+
```

**Spec**
- Container: `JPanel`, `GroupLayout` (Equipment Inspector idiom) **or** `GridBagLayout`. Background
  `surface/card` (`DARKER_GRAY_COLOR`). Inset `space-1` top/bottom, `space-2` left/right.
- Sprite: `JLabel`, fixed `size/sprite` (36x32). Populate with
  `itemManager.getImage(itemId, qty, stackable).addTo(spriteLabel)` (Inventory Setups
  `InventorySetupsSlot.java:148-169`; Party Panel `PlayerInventoryPanel.java:55-104`). `qty=1`,
  `stackable=false` for gear; for stackable inventory items (runes, cannonballs) pass real qty +
  `stackable=true` so the stack number renders.
- Text block (vertical, gap `space-1`):
  - Line 1 name: `type/body`, `text/primary`.
  - Line 2 sub: `type/caption`; slot label (`text/secondary`) + optional GE price (price colour from
    2.3). Format `"Weapon  ·  1.4m"`.
- Gap sprite->text: `space-3` (6).
- Hover (only if the row is interactive, i.e. has the wiki action): background -> `surface/card-hover`
  (`DARKER_GRAY_HOVER_COLOR`) via `MouseAdapter`.
- Right-click: `JPopupMenu` -> **"Wiki"** opening
  `https://oldschool.runescape.wiki/w/Special:Lookup?type=item&id={id}&utm_source=runelite` via
  `LinkBrowser.browse(...)` (Equipment Inspector `ItemPanel.java:44-58`; research §3.7).
- **State variants** (drives colour - Section 4):
  - `OWNED` (worn/inventory): name `text/primary`, sprite full opacity.
  - `UPGRADE` (`missingUpgrades`, aspirational, not an error): name `text/muted`
    (`MEDIUM_GRAY_COLOR`), sprite dimmed `ImageUtil.alphaOffset(img, -80)`, trailing `Tag("upgrade")`
    in `text/secondary`. **Not red** - you are not failing, you just don't own it yet.
  - `BLOCKED` (a *required* item you lack): name `state/blocked` (`PROGRESS_ERROR_COLOR`), trailing
    `Tag("missing")` in red.

### 2.2 `EquipmentGrid` (at-a-glance worn gear - model: Party Panel `EquipmentPanelSlot` / `PlayerInventoryPanel.java:55-104`) [pending UX: grid vs list as primary]

Mirrors the in-game worn-equipment tab so players read gear by position, instantly. Our model has 11
slots (`EquipmentSlot`: HEAD, CAPE, AMULET, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING).

- Container: `JPanel`, `DynamicGridLayout(5, 3, 2, 2)` (5 rows x 3 cols, `space-1` gaps). 15 cells; 11
  filled, 4 blank-but-present so the cross shape holds (Party Panel keeps empty `JLabel`s for shape).
- Cell placement (canonical OSRS layout):
  ```
  row0:   .       HEAD     .
  row1:  CAPE    AMULET   AMMO
  row2: WEAPON   BODY     SHIELD
  row3:   .       LEGS     .
  row4:  HANDS    FEET     RING
  ```
- `EquipmentSlotCell`: `JLabel`, `size/grid-cell` (38x34), background `surface/card`
  (`DARKER_GRAY_COLOR`), 1px `border/divider`. Filled = `getImage(id,1,false).addTo(label)`. Empty =
  blank cell (optionally a faint slot glyph, but not required for v1).
- Tooltip per cell: item name (`setToolTipText`).
- Right-click wiki as in 2.1.

### 2.3 `InventoryGrid` (consumables - 4-wide like the real bag)

- Container: `DynamicGridLayout(ceil(n/4), 4, 2, 2)`. Cells as `EquipmentSlotCell` but stackable:
  `getImage(id, qty, true)` so potion doses / rune counts show their stack number (Party Panel
  `PlayerInventoryPanel`). Use for `Recommendation.inventory`.

### 2.4 GE price colour coding (only when cost data is shown; mode = Cost or always on the sub-line)

From `Recommendation.totalGearCost` (per-item price via `ItemManager`/price service - data path is
engineering's). Colour the price label by tier (Equipment Inspector `ItemPanel.java:97-112`, adapted to
RuneLite's own GE constants):

| GE value | Colour token | Constant |
|---|---|---|
| 0 / unknown | `text/muted` | `MEDIUM_GRAY_COLOR` |
| < 100k | `text/secondary` | `LIGHT_GRAY_COLOR` |
| 100k - 10M | `price/mid` | `GRAND_EXCHANGE_ALCH` |
| > 10M | `price/high` | `GRAND_EXCHANGE_PRICE` |

Compact label `QuantityFormatter.quantityToRSDecimalStack(price)`; full number on tooltip via
`QuantityFormatter.formatNumber(price)`.

**Plumbing note for engineering (audit Finding 1 step 4):** the panel currently receives only
`Map<Integer,String>` item *names* (`SlayerPanelState.itemNames`) so it physically cannot draw a
sprite. Either pass the injected `ItemManager` into `SlayerPanel`, or pre-resolve sprites on the client
thread next to `collectNames(...)` (`AllInSlayerPlugin.java:242-252`) and add them to the state. This is
an engineering call; I only require that the row/grid components receive an item id + qty + stackable so
they can call `getImage(...)` themselves.

---

## 3. Compact icon action bar + segmented mode control (audit Findings 2, 6, 7)

Replaces the three full-width stacked text buttons (`GridLayout(0,1)`, ~100px of chrome,
`SlayerPanel.java:55-57,155-160,398`) with one **24px** row. Net: ~100px -> ~24px.

### 3.1 `ActionBar` container

- `JPanel`, `BorderLayout`, height `size/action-bar` (24), background `surface/page`
  (`DARK_GRAY_COLOR`), no border, horizontal inset `space-2`.
- **WEST**: `ModeSelector` (segmented DPS|Cost). **EAST**: `FlowLayout(RIGHT, space-1, 0)` of
  `IconButton`s - Refresh, Export.

### 3.2 `IconButton` (model: Quest Helper `QuestHelperPanel.java:164-279`)

- `JButton`; `SwingUtil.removeButtonDecorations(btn)` + `setUI(new BasicButtonUI())`.
- `setIcon(ImageIcon)` 16x16 from `ImageUtil.loadImageResource(getClass(), "/icons/<name>.png")`.
- Hover: either `setRolloverIcon(new ImageIcon(ImageUtil.luminanceScale(base, 1.2f)))` **or** a
  `MouseAdapter` swapping background to `surface/icon-hover` (`DARK_GRAY_HOVER_COLOR`). Pick the
  background-swap to match the rest of the app's hover language; keep it consistent across both icons.
- Disabled (Export when `Recommendation == null`, per `SlayerPanel.java:116`):
  `setIcon(new ImageIcon(ImageUtil.grayscaleImage(base)))` + `setEnabled(false)`.
- `setToolTipText(...)`: "Refresh", "Export loadout".
- Size `size/icon-button` (24x24).

**Which actions get icons:**
- **Refresh** (`refresh.png`, circular-arrow) - demoted from primary to fallback icon. The panel
  already auto-recomputes on startup/varbit/chat/menu/item-container/game-state
  (`AllInSlayerPlugin.java:110,125,134,153,169,176`), so Refresh is a manual nudge, not the headline
  (audit Finding 7; none of the 5 researched plugins ship a primary Refresh, research §2 cross-idiom).
- **Export** (`export.png`, copy/clipboard) - secondary action.
- Mode is **not** an icon; it becomes the segmented control below (Finding 6).

### 3.3 `ModeSelector` (segmented DPS | Cost - audit Finding 6)

Two `JToggleButton`s in a `ButtonGroup` (a 2-tab `MaterialTabGroup` is an acceptable alt but heavier).
Shows *both* options at once so the choice and current state are visible without clicking - the current
`Mode: DPS` relabel button (`SlayerPanel.java:56,115`) hides that it toggles.

- Each segment: `JToggleButton`, `type/caption`, height `size/segment` (22), `removeButtonDecorations`.
- **Active** segment: text `accent/brand` (`BRAND_ORANGE`), background `surface/card`
  (`DARKER_GRAY_COLOR`), 2px bottom border `accent/brand`.
- **Inactive** segment: text `text/secondary` (`LIGHT_GRAY_COLOR`), transparent background, no border.
- Wiring unchanged: selecting a segment calls the existing `onToggleMode` path
  (`AllInSlayerPlugin.java:87-91`).

### 3.4 Icon assets required

Only **2 base PNGs** for the core redesign (hover/disabled variants generated in code, §0):
`src/main/resources/.../icons/refresh.png`, `export.png` - 16x16, monochrome light grey
(`#A5A5A5`-ish) so `luminanceScale` reads cleanly. Idiom: `ImageUtil.loadImageResource`. (Icon-tab
assets are separate and optional - Section 6.)

---

## 4. Colour usage rules (research §3.4 - the discipline)

The current panel paints **every** section heading `BRAND_ORANGE` (`section()` `SlayerPanel.java:446`,
`sectionHeading()` `:468`). That spends the one accent colour on decoration, so it no longer signals
anything. Fix:

**Brand orange is reserved for *real* state, never decoration:**
- Active mode segment (3.3).
- Active tab underline (`MaterialTab` default).
- The **recommended / currently-selected** location marker (the one the advisor picked).
- Nothing else. Section headers become `type/title` in `text/primary` (or `text/secondary` small-caps),
  **not** orange.

**Met / unmet / missing palette:**
| Situation | Colour | Constant |
|---|---|---|
| Owned & in the loadout | `text/primary` (sprite carries it) | `TEXT_COLOR` |
| Requirement met (e.g. Slayer level >= required) | `state/met` (sparing - a number or check) | `PROGRESS_COMPLETE_COLOR` |
| Required item missing (blocks task) | `state/blocked` | `PROGRESS_ERROR_COLOR` |
| Upgrade you don't own yet (optional) | `text/muted` + dimmed sprite | `MEDIUM_GRAY_COLOR` + `alphaOffset` |

Rationale: red is for *blockers*, not for aspirational upgrades; an upgrade you can't afford is muted,
not alarming (mirrors Quest Helper met=green/unmet=muted, `QuestRequirementsPanel.java:258-321`; Banked
Experience distinct state backgrounds, `GridItem.java:55-99`).

**Background bands (the two-gray system):**
- `surface/page` `DARK_GRAY_COLOR` (40,40,40) = panel root + action bar + gaps (the "page").
- `surface/card` `DARKER_GRAY_COLOR` (30,30,30) = every recessed card/row/grid cell (the "wells").
- `border/divider` `BORDER_COLOR` (23,23,23) = card borders, dividers.
- Interactive row hover -> `surface/card-hover` (60,60,60, visible); icon hover -> `surface/icon-hover`
  (35,35,35, subtle). These are the only two hover states.

---

## 5. Header spec (`TaskHeader` - audit Finding 5)

Replaces `Source: MENU_CHECK | Updated: 21:48:12` (raw enums, dev framing, `SlayerPanel.java:53,113`)
and the `<html><b>` title hack (`titleText()` `:595`).

**Anatomy (3 lines, left-aligned, on `surface/page`):**
```
Abyssal demons                 <- type/title, text/primary  (task name, no <html>, no enum)
147 remaining                  <- type/caption, text/secondary  (omit if remaining <= 0)
updated 21:48 · helm check     <- type/caption, text/muted
```

- Line 1 task name: `type/title` (`getRunescapeBoldFont`), `text/primary`. Plain text.
- Line 2 remaining: `type/caption`, `text/secondary`. `"{n} remaining"`; hide when unknown/0.
- Line 3 meta: `type/caption`, `text/muted`. `"updated {HH:mm} · {humane source}"` - **minutes only**,
  seconds are dev-grade precision; the raw enum + seconds stay in the (gated) Debug tab
  (`SlayerPanel.java:318`).

**`RefreshSource` -> humane label map** (add a `displayName` to the enum or a `switch` in the panel;
pure string mapping, no API):

| Enum (`RefreshSource.java`) | Humane label |
|---|---|
| `STARTUP` | startup |
| `MANUAL` | manual refresh |
| `VARBIT` | task changed |
| `CHAT` | new assignment |
| `MENU_CHECK` | helm check |
| `ITEM_CONTAINER` | gear update |
| `GAME_STATE` | login |
| `MODE_TOGGLE` | mode change |
| `LOCATION_SELECT` | location change |

Padding `space-2` around the block; gap between lines `space-1`.

---

## 6. Tabs / chrome (audit Finding 8 - lower priority, **with the DimmableMaterialTab correction**)

**Recommendation: keep text tabs for now.** Rationale:
1. The library helper the audit/brief assumed for icon tabs - **`DimmableMaterialTab` - does not exist
   in client 1.12.31.1** (Section 0). Icon tabs are still possible via `MaterialTab(ImageIcon,...)`, but
   inactive-tab dimming must be hand-built (a `grayscaleImage`/`alphaOffset` `ImageIcon` swapped on
   select/unselect). That is real bespoke work for 2-3 tabs.
2. Only 2 player-facing tabs (Task, Loadout; Debug gated per audit Finding 3) - text is perfectly
   legible and is the spec's deliberate choice during the debugging phase.
3. ROI is far below Findings 1-3.

**Text-tab styling (apply now):** `MaterialTabGroup` + `MaterialTab(String,...)` (as today,
`SlayerPanel.java:89-96`); ensure tab labels use `type/body`; active tab keeps the `MaterialTab` default
`BRAND_ORANGE` underline (this is a *legitimate* brand-for-state use, Section 4).

**If icon tabs are adopted later (deferred):** assets `task.png`, `loadout.png` (and `debug.png` if
kept) at 18-20px in `src/main/resources/.../icons/`, plus a code-generated dimmed variant per tab; wire
the dim swap into `MaterialTab.setOnSelectEvent`/`select()/unselect()`. Treat as a separate, low-priority
ticket.

---

## 7. ASCII mockup - redesigned panel, `TASK_WITH_LOADOUT` state (Loadout tab)

Reflects Sections 1-6. Width ~225px. `(O)`=orange/active, dimmed rows = upgrades.

```
+---------------------------------------------------------+   surface/page (#282828)
|  Abyssal demons                                         |   type/title, text/primary
|  147 remaining                                          |   type/caption, text/secondary
|  updated 21:48 · helm check                             |   type/caption, text/muted
|---------------------------------------------------------|
|  [ DPS ](O)  [ Cost ]                    (refresh)(export)|  ActionBar 24px: ModeSelector W, icons E
|---------------------------------------------------------|
|   Task    | [Loadout] |                                 |  MaterialTab, active underline = BRAND_ORANGE
|=========================================================|
|  +---------------------- card (#1E1E1E) --------------+ |
|  |  Worn                                              | |   section header: type/title, text/primary
|  |             +------+                               | |
|  |             |[HEAD]|                               | |
|  |     +------+ +------+ +------+                      | |   EquipmentGrid: DynamicGridLayout(5,3,2,2)
|  |     |[CAPE]| |[AMUL]| |[AMMO]|                      | |   cells = sprite on #1E1E1E
|  |     +------+ +------+ +------+                      | |
|  |     |[WEAP]| |[BODY]| |[SHLD]|                      | |
|  |             +------+                               | |
|  |             |[LEGS]|                               | |
|  |     +------+ +------+ +------+                      | |
|  |     |[HAND]| |[FEET]| |[RING]|                      | |
|  +----------------------------------------------------+ |
|                                                         |   space-5 gap
|  +---------------------- card -----------------------+  |
|  |  Inventory                                        |  |
|  |  +----+ +----+ +----+ +----+                      |  |   InventoryGrid: 4-wide, stackable qty shown
|  |  |sup4| |sara| |rune| |    |                      |  |
|  |  +----+ +----+ +----+ +----+                      |  |
|  +---------------------------------------------------+  |
|                                                         |
|  +---------------------- card -----------------------+  |
|  |  Est. DPS  12.4              Gear cost  4.2m       |  |   KeyValueRow x2; price colour = price/mid
|  +---------------------------------------------------+  |
|                                                         |
|  +---------------------- card -----------------------+  |
|  |  Upgrades you don't own                           |  |   header: text/primary (NOT orange)
|  |  [36x32] Ghrazi rapier                            |  |   LoadoutItemRow state=UPGRADE:
|  |   dim    Stab  ·  55m                  (upgrade)   |  |   name text/muted, sprite alphaOffset -80,
|  |  [36x32] Torva platebody                          |  |   price price/high (#6EE16E), Tag(upgrade)
|  |   dim    Body  ·  120m                 (upgrade)   |  |
|  +---------------------------------------------------+  |
+---------------------------------------------------------+
```

Task tab (for completeness) keeps `KeyValueRow`s (Remaining/Slayer/Weakness/Required/Method) in one
card + a Location card where the **recommended** location name is the only `BRAND_ORANGE` element, with
`Tag`s (multi/cannon/burst/konar) replacing the current `chips()`.

---

## 8. Component spec table (the build contract)

| Component | Purpose | Key props / sizes / colours / fonts | RuneLite API | Source pattern (plugin · file) |
|---|---|---|---|---|
| `KeyValueRow` | the **one** row system for all tabs (kills the `row()` vs `keyValueRow()` split, Finding 4) | `GridBagLayout`; key col `size/key-col` 64px `type/caption` `text/secondary`, value flex `type/body` `text/primary`; row `size/row` 22; pad `space-3` V; bg `surface/card`; **no HTML, width from layout** | plain Swing | Equipment Inspector `ItemPanel` text block |
| `SectionCard` | recessed container grouping rows (replaces `sectionPanel`/`FixedWidthPanel`) | `BoxLayout` Y; bg `surface/card`; 1px `border/divider`; inner pad `space-4`; width from layout (drop `FixedWidthPanel`) | plain Swing | Inventory Setups section panels |
| `SectionHeader` | card title | `type/title`, `text/primary` (**not** `BRAND_ORANGE`); pad bottom `space-3` | plain Swing | Quest Helper section headers `QuestStepPanel` |
| `LoadoutItemRow` | sprite + name + slot + price item line, w/ states | sprite `size/sprite` 36x32; `type/body` name, `type/caption` sub; states OWNED/UPGRADE/BLOCKED (Section 4); hover `surface/card-hover`; right-click Wiki | `ItemManager.getImage(id,qty,stk)`, `AsyncBufferedImage.addTo`, `ImageUtil.alphaOffset`, `QuantityFormatter`, `LinkBrowser` | Equipment Inspector `ItemPanel.java:29-112` |
| `EquipmentGrid` | at-a-glance worn gear (OSRS cross) | `DynamicGridLayout(5,3,2,2)`; `EquipmentSlotCell` `size/grid-cell` 38x34 bg `surface/card` | `DynamicGridLayout`, `getImage`, `addTo` | Party Panel `PlayerInventoryPanel.java:55-104`, `EquipmentPanelSlot` |
| `InventoryGrid` | consumables, 4-wide, stacked qty | `DynamicGridLayout(ceil(n/4),4,2,2)`; cells stackable `getImage(id,qty,true)` | as above | Party Panel `PlayerInventoryPanel` |
| `ActionBar` | compact toolbar (replaces 3 stacked buttons) | `BorderLayout`, height `size/action-bar` 24, bg `surface/page` | plain Swing | Quest Helper `QuestHelperPanel.java:164-279` |
| `IconButton` | borderless 16px action icon | `removeButtonDecorations`+`BasicButtonUI`; icon 16; button 24x24; hover `surface/icon-hover`; disabled `grayscaleImage`; tooltip | `SwingUtil.removeButtonDecorations`, `ImageUtil.loadImageResource/luminanceScale/grayscaleImage` | Quest Helper `QuestHelperPanel.java:164-279` |
| `ModeSelector` | DPS\|Cost segmented toggle (replaces relabel button) | 2x `JToggleButton` in `ButtonGroup`; height `size/segment` 22; active text `accent/brand` + 2px `accent/brand` underline; inactive `text/secondary` | plain Swing | research §3.4 (segmented control) |
| `TaskHeader` | humane task identity | 3 lines: `type/title` name / `type/caption` remaining / `type/caption` meta; humane source map (Section 5) | plain Swing | - (Finding 5) |
| `Tag` | small pill (location attrs, item state) | `type/caption`, height `size/tag` 16, 1px `border/divider`, pad `space-1`x`space-2`; variants default/`upgrade`(`text/secondary`)/`missing`(`state/blocked`) | plain Swing | replaces `chips()`/`addChip()` |
| `PriceLabel` | colour-tiered GE value | `type/caption`; tier colours per 2.4; compact `quantityToRSDecimalStack`, tooltip `formatNumber` | `QuantityFormatter` | Equipment Inspector `ItemPanel.java:97-112` |

---

## 9. Handoff to Frontend Engineer

- **Build order (impact x effort), matching audit's priority:** (1) `LoadoutItemRow` + `EquipmentGrid`
  + the `ItemManager`/sprite plumbing - Finding 1; (2) `ActionBar`/`IconButton`/`ModeSelector` -
  Findings 2,6,7; (3) `TaskHeader` humanisation - Finding 5; (4) `KeyValueRow`/`SectionCard` unification
  and deletion of `FixedWidthPanel`/`fixedWidth`/HTML-width hacks - Finding 4; (5) colour-rule pass -
  Section 4. Icon tabs (Section 6) are deferred.
- **Accessibility floor (desktop Swing):** all text/background pairings here clear WCAG AA - quick
  ratios on `surface/card` (#1E1E1E): `text/primary` #C6C6C6 ~ 11.6:1, `text/secondary` #A5A5A5 ~ 7.4:1,
  `accent/brand` #DC8A00 ~ 6.2:1, `state/blocked` #E61E1E ~ 4.0:1 (fine for large/UI, keep red for
  short tags/numbers not body). `text/muted` #4D4D4D on card is **decorative-only** (low contrast by
  design for dimmed upgrades); never put load-bearing text in `text/muted` alone - always pair with the
  dimmed sprite + a `Tag`. Tooltips back every icon and truncated label.
- **Two open dependencies on UX (board UX1, not yet on disk):** (a) is worn gear primary as
  `EquipmentGrid` or `LoadoutItemRow` list - I spec both; (b) collapsible `SectionCard`s
  (Quest Helper `QuestStepPanel.java:96-132`) - visual is ready, the interaction model is UX's call.

## 10. Corrections logged for the team
1. **`DimmableMaterialTab` is not in client 1.12.31.1** - audit Finding 8 and the UI1 brief both cite it
   as available; verified absent. Icon-tab dimming is hand-rolled if pursued. (Section 0, 6.)
2. Verified the full `ColorScheme` palette RGB values from bytecode (Section 1.1) so no token is guessed.
</content>
</invoke>

# All-In Slayer - Side Panel Redesign: Build Plan

- Author: Architect / Principal Engineer
- Date: 2026-06-29
- Track: T1/T2 implementation of the approved side-panel redesign (P0 + P1 + P2)
- Status: presented at Gate 2, awaiting approval. Do NOT start building before approval.

## 1. Context and scope

The design is done and signed off. This plan turns the approved component contract into a
dependency-ordered, test-first, conflict-safe build the engineers execute. It does not redesign.

Sources of truth (the "spec" for this track):
- `docs/ui-review/README.md` - consolidated roadmap P0-1..P2-14 and the 3 CONFIRMED decisions.
- `docs/ui-review/ui-recommendations.md` - design tokens + buildable component contracts (verified
  against `client-1.12.31.1`).
- `docs/ui-review/ux-recommendations.md` - IA, interaction model, state walkthroughs, copy.
- `docs/ui-review/current-ui-audit.md` - file:line of what changes.
- `docs/superpowers/specs/2026-06-28-all-in-slayer-side-panel-design.md` - the prior approved spec
  whose still-binding functional requirements (FR-1..FR-11) the redesign must preserve.

The 3 binding confirmed decisions (README): (1) Debug gated behind a developer-mode config;
(2) full merge to one scrollable dashboard, order header -> TASK -> WHERE & HOW -> LOADOUT;
(3) worn gear as sprite+name rows (`LoadoutItemRow`) primary, `EquipmentGrid` as a secondary
at-a-glance view.

API facts verified from the jar this session (`client-1.12.31.1`,
`...gradle/caches/.../client-1.12.31.1.jar`):
- `ItemManager.getImage(int)`, `getImage(int,int,boolean)` -> `AsyncBufferedImage`; `addTo(JLabel)`
  and `addTo(JButton)` both exist.
- `AsyncBufferedImage`'s only ctor is `AsyncBufferedImage(ClientThread,int,int,int)` - cannot be
  built in a headless test (drives ADR-0001).
- `ItemManager.getItemPrice(int)` and `getItemPriceWithSource(int,boolean)` exist - per-item GE price
  IS available (drives ADR-0004 price plumbing).
- `DynamicGridLayout(int,int)` and `(int,int,int,int)`; `SwingUtil.removeButtonDecorations`;
  `ImageUtil.loadImageResource/luminanceScale/grayscaleImage/alphaOffset`;
  `QuantityFormatter.quantityToRSDecimalStack/formatNumber`; `LinkBrowser.browse` - all present.
- `DimmableMaterialTab` does NOT exist (team memory). Not needed: the merged dashboard removes tabs.

## 2. Architecture decisions (load-bearing) - see `docs/adr/`

| ADR | Decision | One-line why |
|---|---|---|
| [0001](adr/0001-item-sprite-rendering-seam.md) | Sprites render through an injected `ItemIconRenderer` seam, not `ItemManager` directly | `AsyncBufferedImage` needs a live `ClientThread`, so components must be testable behind a fakeable seam |
| [0002](adr/0002-single-scrollable-dashboard-render-contract.md) | One scrollable dashboard; self-diffing sections behind a single `render(SlayerPanelState)`; combo built once; `GameStateChanged` filtered to `LOGGED_IN` | Merge is signed off; non-destructive re-render kills the flicker/scroll-jump/combo-drop |
| [0003](adr/0003-developer-mode-gates-debug-surface.md) | `developerMode()` config gates the Diagnostics surface; flag carried in `SlayerPanelState` | No top plugin ships a player debug surface; keeps the single-state render contract |
| [0004](adr/0004-plain-text-labels-tokens-and-price-plumbing.md) | Plain-text labels (no `<html>`), one `SlayerTheme` token holder, per-item price plumbed via state on the client thread | Removes brittle width/HTML hacks; per-item price needs a client-thread read |

### 2.1 New component tree (target structure)

```
SlayerPanel  (PluginPanel, BorderLayout; @Inject ctor(ItemManager) -> ItemManagerIconRenderer;
              package-private ctor(ItemIconRenderer) for tests)
|-- NORTH  headerBand (BoxLayout Y, surface/page; never scrolls)
|     |-- TaskHeader        3 lines: name / "{n} remaining" / "updated HH:mm . {humane source}" (in-place)
|     |-- ActionBar         BorderLayout: WEST ModeSelector(DPS|Cost), EAST [IconButton refresh, IconButton export]
|-- CENTER JScrollPane (built once, HORIZONTAL_SCROLLBAR_NEVER, VERTICAL_SCROLLBAR_NEVER, wheel on)
      |-- dashboardBody (BoxLayout Y, surface/page)
            |-- taskSection      SectionCard "Task": KeyValueRows (Slayer lvl, Weakness) + required item as LoadoutItemRow
            |-- whereSection     SectionCard "Where & How": recommended location (brand) + Tags + Why + Method + location JComboBox(once)
            |-- loadoutSection   SectionCard "Loadout": EquipmentGrid (at-a-glance) + worn LoadoutItemRows (primary) +
            |                      InventoryGrid + KeyValueRow Est.DPS + KeyValueRow/PriceLabel Gear cost +
            |                      missing-upgrade LoadoutItemRows (UPGRADE) + bank age
            |-- emptyState       PluginErrorPanel for NO_TASK / UNSUPPORTED_TASK / no-loadout (replaces the relevant sections)
            |-- debugSection     SectionCard "Diagnostics (developer)" - present only when state.developerMode
```

Each `*Section` is self-diffing (ADR-0002): `render(state)` calls `section.update(state)`; a section
rebuilds its body only when its slice changed.

### 2.2 Humane `RefreshSource` -> label map (reconciles UI vs UX wording; copy is trivially tunable)

`STARTUP`->"startup" - `MANUAL`->"manual refresh" - `VARBIT`->"task changed" - `CHAT`->"new assignment"
- `MENU_CHECK`->"gem / helm check" - `ITEM_CONTAINER`->"gear update" - `GAME_STATE`->"login" -
`MODE_TOGGLE`->"mode change" - `LOCATION_SELECT`->"location change". Lives in `TaskHeader` (or a
`RefreshSource.displayName()`); pure string mapping, no API. Raw enum stays in the Diagnostics surface.

## 3. New file list

All new production files; package roots under `com.danieljglover.allinslayer`.

| File | Package | Purpose |
|---|---|---|
| `ui/theme/SlayerTheme.java` | `...ui.theme` | The token holder: colour/font/spacing/size constants resolving to `ColorScheme`/`FontManager` (ui-rec §1). Single source for all styling. |
| `ui/ItemIconRenderer.java` | `...ui` | Seam interface `render(JLabel,id,qty,stackable)` (ADR-0001). |
| `ui/ItemManagerIconRenderer.java` | `...ui` | Production impl wrapping `ItemManager.getImage(...).addTo(label)`. |
| `ui/components/KeyValueRow.java` | `...ui.components` | The one key/value row (GridBagLayout, caption key + body value, no HTML). |
| `ui/components/SectionCard.java` | `...ui.components` | Recessed card container (BoxLayout Y, surface/card, divider border). |
| `ui/components/SectionHeader.java` | `...ui.components` | Card title (type/title, TEXT_COLOR, not brand). |
| `ui/components/Tag.java` | `...ui.components` | Small pill; variants default/upgrade/missing (replaces `chips()`). |
| `ui/components/PriceLabel.java` | `...ui.components` | Colour-tiered GE value; compact text + full-number tooltip. |
| `ui/components/IconButton.java` | `...ui.components` | Borderless 16px icon button (takes an `Icon`); hover + disabled (grayscale) + tooltip. |
| `ui/components/ActionBar.java` | `...ui.components` | Toolbar: WEST `ModeSelector`, EAST refresh/export `IconButton`s; loads `/icons/*.png`. |
| `ui/components/ModeSelector.java` | `...ui.components` | Segmented DPS\|Cost (two `JToggleButton`s, `ButtonGroup`); active=brand. |
| `ui/components/TaskHeader.java` | `...ui.components` | 3-line humane header; in-place `update(state)`; humane source map. |
| `ui/components/LoadoutItemRow.java` | `...ui.components` | Sprite (via `ItemIconRenderer`) + name + slot + optional `PriceLabel`; OWNED/UPGRADE/BLOCKED; right-click Wiki. |
| `ui/components/EquipmentGrid.java` | `...ui.components` | OSRS-cross worn grid (`DynamicGridLayout(5,3,2,2)`); contains `EquipmentSlotCell`. |
| `ui/components/EquipmentSlotCell.java` | `...ui.components` | One grid cell (sprite or blank; tooltip = item name). May be a static nested class of `EquipmentGrid`. |
| `ui/components/InventoryGrid.java` | `...ui.components` | 4-wide consumables grid; stackable `getImage(id,qty,true)`. |
| `src/main/resources/icons/refresh.png` | resources | 16x16 monochrome refresh glyph (see Risk R1). |
| `src/main/resources/icons/export.png` | resources | 16x16 monochrome export/clipboard glyph. |

Modified files: `ui/SlayerPanel.java` (hot), `ui/SlayerPanelState.java` (shared),
`AllInSlayerPlugin.java` (hot), `AllInSlayerConfig.java`, optionally `ui/RefreshSource.java`. Deleted:
`FixedWidthPanel` and the HTML/width helpers inside `SlayerPanel.java` (ADR-0004).

## 4. State and plugin changes (the seam between the panel and the client)

`SlayerPanelState` gains two fields (ADR-0003/0004):
- `Map<Integer,Integer> itemPrices` - never null, may be empty; defensive copy like `itemNames`.
- `boolean developerMode` - default `false`.

Keep the existing short factory overloads (`noTask`/`unsupportedTask`/`forTask`) defaulting
`itemPrices=emptyMap`, `developerMode=false`, so existing non-UI call sites keep compiling; add one
full `forTask(...)` overload the plugin uses. This bounds the blast radius (Risk R2).

`AllInSlayerPlugin.recompute(...)` (client thread) additions:
- After `collectNames(...)`, a `collectPrices(...)` builds `Map<Integer,Integer>` via
  `itemManager.getItemPrice(id)` for the same ids; pass into the full `forTask(...)`.
- Pass `config.developerMode()` into every state it builds.
- `onGameStateChanged`: rebuild only on `GameState.LOGGED_IN` (ADR-0002, Finding 9).
- New `@Subscribe onConfigChanged(ConfigChanged)`: if group is `allinslayer`, schedule a recompute so
  the developer-mode toggle is live.

`AllInSlayerConfig`: add `developerMode()` `@ConfigItem` (default `false`).

No plugin change is needed to inject `ItemManager` into the panel: the panel is field-injected
(`@Inject private SlayerPanel panel`), so Guice builds it via its new `@Inject` constructor.

## 5. Requirements traceability (roadmap / decisions / FRs -> task -> verification)

Zero unmapped requirement IDs. "Acceptance criterion" quotes the roadmap/spec verbatim where short.

| ID | Acceptance criterion (verbatim / condensed) | Task(s) | Verification |
|---|---|---|---|
| P0-1 | "Render gear as item sprites - worn rows/grid, inventory grid, required item, missing upgrades - via `ItemManager.getImage(...).addTo()`." | T02, T10, T11, T12, T19, T21 | LoadoutItemRow/EquipmentGrid/InventoryGrid tests assert renderer called with (id,qty,stackable); manual: sprites appear |
| P0-2 | "Collapse the 3 stacked full-width buttons into one ~24px icon action bar (Refresh + Export as borderless 16px icons with tooltips)." | T07, T13, T14, T18 | ActionBarTest: 2 IconButtons, tooltips set, 24px; no GridLayout button stack remains |
| P0-3 | "Segmented `[DPS][Cost]` control (both shown, active = `BRAND_ORANGE`) replacing the relabel button." | T08, T13, T18 | ModeSelectorTest: both segments present, active fg = BRAND_ORANGE, toggle fires `onToggleMode` |
| P0-4 | "Humanise the header - `updated 21:48 . helm check`; drop `Source:`, raw enums, and seconds." | T09, T18, T19 | TaskHeaderTest: HH:mm not seconds, humane source per enum, no "Source:"/raw enum text |
| P0-5 | "Gate the Debug tab behind a developer config - default players see only player-facing content; diagnostics preserved when enabled." | T16, T15, T23 | SlayerPanelTest: developerMode=false -> no Diagnostics; =true -> all debug fields (ADR-0003) |
| P1-6 | "Merge Task + Loadout into one scrollable dashboard ordered by the 3 player questions; remove the Task/Loadout tab split." | T18 | SlayerPanelTest: no MaterialTab/MaterialTabGroup; one scroll body; section order task/where/loadout (ADR-0002) |
| P1-7 | "Reactive by default; demote Refresh to the icon-bar fallback (keep it - FR-7)." | T13, T17 | Refresh is an IconButton in ActionBar that fires `onRefresh`; plugin already reactive |
| P1-8 | "Non-destructive re-render - preserve scroll, selected location, mode, combo popup/focus; rebuild only changed section; filter `GameStateChanged` to `LOGGED_IN`; build the combo once." | T20, T22, T17 | T22 test: combo instance ==, scroll value preserved, no rebuild on unchanged slice; plugin LOGGED_IN filter |
| P1-9 | "Player-language empty/error states for every state, each naming the actual lever (no 'then refresh', no `UNSUPPORTED_TASK`)." | T24 | T24 test: each state's copy present; no raw enum / "Source:" / "then refresh" strings |
| P1-10 | "Encode owned/missing with colour on required-item and missing-upgrade rows; reserve `BRAND_ORANGE` for real state." | T10, T19, T21, T25 | Tests: BLOCKED row fg = PROGRESS_ERROR; UPGRADE muted+dimmed; section headers not brand |
| P2-11 | "Unify on one `KeyValueRow`/`SectionCard`; delete `FixedWidthPanel`, `fixedWidth()`, `KEY_WIDTH/VALUE_WIDTH`, all `<html><div style='width:Npx'>`." | T03, T04, T25 | T25: no `<html>` in any label text; `FixedWidthPanel` deleted; widths from layout (ADR-0004) |
| P2-12 | "Per-row right-click -> Wiki lookup on item rows (`LinkBrowser` -> `Special:Lookup`)." | T10 | LoadoutItemRowTest: JPopupMenu has a "Wiki" item targeting the item id |
| P2-13 | "Collapsible sections (chevron + bold header)." | T26 (deferred) | Optional; not required for v1 (UX P2, spec line 224) |
| P2-14 | "Icon tabs - only if assets exist; low ROI; text tabs are fine." | N/A | No tabs after the merge; explicitly out of scope |
| Decision 1 | Debug gated behind developer config (supersedes spec 280/725) | T16, T23 | ADR-0003; T23 test |
| Decision 2 | Full merge to one scrollable dashboard | T18 | ADR-0002; T18 test |
| Decision 3 | Worn gear = `LoadoutItemRow` primary, `EquipmentGrid` secondary at-a-glance | T21 | T21 test: worn rows are the detail list; grid present as overview |
| FR-1 | "`SlayerPanel` must expose one primary render entry point `void render(SlayerPanelState)`." | T18 | render is the only public update method; preserved through integration |
| FR-2 | "Every recompute passes a `RefreshSource`... panel displays that source in the header and Debug." | T09, T23 | Header shows humane source; Diagnostics shows raw enum |
| FR-3 | "Capture menu-check diagnostics... Debug tab must show those fields even when the click does not match." | T23 | Existing capture unchanged; T23 surfaces fields in dev mode |
| FR-4 | "Each recompute captures raw Slayer values (target/count/area/boss varbit); `-1` when unreadable." | T23 | Existing capture unchanged; T23 surfaces in dev mode |
| FR-5 | "Distinguish no-task vs unsupported vs supported; do not collapse unsupported and no-task." | T24 | T24: NO_TASK and UNSUPPORTED_TASK render distinct copy |
| FR-6 | "If a task resolves but loadout is empty, task intel must still render; loadout shows an empty state." | T19, T20, T21, T24 | TASK_WITHOUT_LOADOUT: task+where render; loadout empty state; export disabled |
| FR-7 | "The action row must include a manual refresh control scheduling `RefreshSource.MANUAL`." | T13 | Refresh IconButton present, fires `onRefresh` |
| FR-8 | "Mode control: show current mode, toggle, recompute `MODE_TOGGLE`, keep position after render." | T08, T22 | ModeSelector reflects mode; T22 preserves scroll/state |
| FR-9 | "Export enabled only when `recommendation` non-null; must not appear enabled otherwise." | T13, T18 | T18: export disabled (and looks disabled) in NO_TASK/UNSUPPORTED/no-loadout |
| FR-10 | "Header/action row keep stable dimensions; long names wrap/truncate inside panel width." | T18, T25 | HORIZONTAL_SCROLLBAR_NEVER; widths from layout; long-name test keeps PANEL_WIDTH |
| FR-11 | "No sensitive or network data displayed." | T23 | Diagnostics shows only local game/client state (unchanged) |

## 6. Dependency-ordered task breakdown (test-first)

Effort: S ~ hours, M ~ a day, L ~ multi-day. Each task writes its failing test(s) first
(red-green-refactor). "Creates" = new files; "Modifies" = edits to existing/hot files.

### Phase 0 - foundations (new files, no hot-file contention)

- **T01 - SlayerTheme tokens.** Creates `ui/theme/SlayerTheme.java`. Deps: none. Effort S.
  First test `SlayerThemeTest`: asserts the semantic tokens resolve to the expected `ColorScheme`
  constants and the 3 `FontManager` roles (guards against drift / accidental brand-on-heading).
- **T02 - ItemIconRenderer seam.** Creates `ui/ItemIconRenderer.java`, `ui/ItemManagerIconRenderer.java`.
  Deps: none. Effort S. First test `ItemManagerIconRendererTest` (Mockito): a mocked `ItemManager`
  returning a mocked `AsyncBufferedImage` is asked `getImage(id,qty,stackable)` and `addTo(label)`.

### Phase 1 - leaf component library (new files; FE-A; parallelisable after T01/T02)

- **T03 - KeyValueRow.** Creates `ui/components/KeyValueRow.java`. Deps: T01. Effort S.
  `KeyValueRowTest`: key caption text+colour, value body text+colour, height, no `<html>` in text.
  (T03 is the headless-Swing/FontManager canary - see test strategy.)
- **T04 - SectionCard + SectionHeader.** Creates both. Deps: T01. Effort S. `SectionCardTest`:
  bg = surface/card, border = divider, header colour = TEXT_COLOR (NOT BRAND_ORANGE).
- **T05 - Tag.** Creates `ui/components/Tag.java`. Deps: T01. Effort S. `TagTest`: text, default/
  upgrade/missing variant colours, height = size/tag.
- **T06 - PriceLabel.** Creates `ui/components/PriceLabel.java`. Deps: T01. Effort S. `PriceLabelTest`:
  tier colour by value (0/unknown muted, <100k secondary, 100k-10M alch, >10M ge-price), compact
  text via `quantityToRSDecimalStack`, tooltip via `formatNumber`.
- **T07 - IconButton.** Creates `ui/components/IconButton.java` (ctor takes an `Icon`/`BufferedImage`
  + tooltip). Deps: T01. Effort S. `IconButtonTest` (in-memory `BufferedImage`, no resource):
  tooltip set, 24x24, decorations removed, disabled -> grayscale icon + `!isEnabled()`.
- **T08 - ModeSelector.** Creates `ui/components/ModeSelector.java`. Deps: T01. Effort S/M.
  `ModeSelectorTest`: two toggles in a `ButtonGroup`; active fg = BRAND_ORANGE; selection reflects
  `AdviceMode`; clicking the inactive segment fires the supplied callback.
- **T09 - TaskHeader.** Creates `ui/components/TaskHeader.java`. Deps: T01. Effort M.
  `TaskHeaderTest`: 3 labels; name rendered literally and not interpreted; remaining line hidden when
  `remaining<=0`; meta = "updated HH:mm . {humane source}" (minutes, not seconds) for each
  `RefreshSource`; `update(state)` mutates in place (same label instances).
- **T10 - LoadoutItemRow.** Creates `ui/components/LoadoutItemRow.java`. Deps: T01, T02, T05, T06.
  Effort M. `LoadoutItemRowTest` (fake renderer): name+slot text; OWNED name = TEXT_COLOR;
  UPGRADE name = muted + renderer asked with a dim flag + trailing `Tag("upgrade")`; BLOCKED name =
  PROGRESS_ERROR + `Tag("missing")`; renderer called with (id,qty,stackable); right-click `JPopupMenu`
  has a "Wiki" item.
- **T11 - EquipmentGrid (+EquipmentSlotCell).** Creates both. Deps: T01, T02. Effort M.
  `EquipmentGridTest` (fake renderer): `DynamicGridLayout(5,3)`, 15 cells, OSRS slot->cell mapping,
  filled cells ask the renderer for the right id, empty cells blank, tooltip = item name.
- **T12 - InventoryGrid.** Creates `ui/components/InventoryGrid.java`. Deps: T01, T02. Effort S/M.
  `InventoryGridTest` (fake renderer): `ceil(n/4)` rows x 4 cols; renderer asked per id with
  `stackable=true`.
- **T13 - ActionBar.** Creates `ui/components/ActionBar.java`. Deps: T07, T08, T14. Effort M.
  `ActionBarTest`: contains `ModeSelector` (WEST) + 2 `IconButton`s (EAST); export disabled when the
  no-recommendation flag is set; height = size/action-bar.
- **T14 - Icon assets.** Creates `resources/icons/refresh.png`, `export.png` (16x16 monochrome
  ~`#A5A5A5`). Deps: none. Effort S. Verification: `ImageUtil.loadImageResource(getClass(),
  "/icons/refresh.png")` returns non-null. (Risk R1; IconButton tests do not depend on this.)

### Phase 2 - state + plugin plumbing (shared state + hot plugin; FE-B; parallel to Phase 1)

- **T15 - SlayerPanelState fields.** Modifies `ui/SlayerPanelState.java`, `SlayerPanelStateTest.java`.
  Adds `itemPrices` + `developerMode` with short-overload defaults + one full `forTask(...)`.
  Deps: none (coordinate field names with FE-A). Effort S/M. Test: extend `SlayerPanelStateTest` -
  `itemPrices` defensive copy + empty default; `developerMode` default false; full overload carries both.
- **T16 - developerMode config.** Modifies `AllInSlayerConfig.java`. Adds `developerMode()` default
  false. Deps: none. Effort S. (Default verified by existing config-default conventions.)
- **T17 - Plugin plumbing.** Modifies `AllInSlayerPlugin.java`. `collectPrices(...)`; pass
  `itemPrices` + `config.developerMode()` into state; `onGameStateChanged` -> only `LOGGED_IN`; add
  `onConfigChanged` recompute. Deps: T15, T16. Effort M. Test: where feasible, a plugin state-builder
  test asserting `LOGGED_IN`-only rebuild and that prices/devMode reach the state; otherwise cover the
  `LOGGED_IN` predicate by extracting a tiny testable method, and rely on manual checklist for the rest.

### Phase 3 - SlayerPanel integration (hot file; SERIAL; FE-A after Phases 1-2)

All Phase 3 tasks modify `ui/SlayerPanel.java` and `ui/SlayerPanelTest.java`. They must run in order.

- **T18 - Shell restructure.** Replace the 3-tab scaffold with the anchored header band
  (`TaskHeader`+`ActionBar`) + single `JScrollPane(dashboardBody)`; keep `render(...)` as sole entry;
  add `@Inject` ctor(`ItemManager`) -> `ItemManagerIconRenderer` and package-private ctor
  (`ItemIconRenderer`) for tests. Rewrite the test scaffolding (drop `selectTab`/MaterialTab helpers;
  construct with a fake renderer). Deps: T02, T09, T13. Effort M. Test: constructs headless with a
  fake renderer; `render(noTask)` shows header + `PluginErrorPanel`; no `MaterialTab`; export disabled.
- **T19 - Task section.** `taskSection` SectionCard: Slayer-lvl + Weakness `KeyValueRow`s; required
  item as a `LoadoutItemRow` (OWNED vs BLOCKED by ownership). Deps: T18, T10, T03. Effort M.
  Test: required item renders as a sprite row with state; remaining count shown in the header.
- **T20 - Where & How section.** Recommended location (only BRAND_ORANGE element) + `Tag`s
  (multi/cannon/burst/konar) + Why + Method + the location `JComboBox` built once and updated in place.
  Deps: T18, T05. Effort M. Test: combo instance stable across two renders; selected location
  persists; tags present; recommended is the sole brand element.
- **T21 - Loadout section.** `EquipmentGrid` (at-a-glance) + worn `LoadoutItemRow`s (primary) +
  `InventoryGrid` + `KeyValueRow` Est.DPS + Gear-cost via `PriceLabel` + missing-upgrade
  `LoadoutItemRow`s (UPGRADE) + bank age. Deps: T18, T10, T11, T12, T06. Effort M/L. Test: worn rows
  carry sprite+name+slot; inventory grid present; DPS/cost rows; upgrades dimmed.
- **T22 - Non-destructive re-render.** Self-diffing `section.update(slice)`; capture/restore scroll
  value; in-place header/mode/export-enabled. Deps: T19, T20, T21. Effort M. Test: re-render with an
  unchanged loadout slice -> 0 section rebuilds (rebuild-count probe) and scrollbar value unchanged;
  a remaining-count tick updates only the header.
- **T23 - Developer-mode Diagnostics.** Render "Diagnostics (developer)" SectionCard only when
  `state.developerMode`; preserve all current debug fields. Deps: T18, T15. Effort M. Test:
  developerMode=false -> no Diagnostics; =true -> all debug fields present.
- **T24 - Player-language empties.** NO_TASK / UNSUPPORTED_TASK / no-loadout / no-bank copy per UX §4.
  Deps: T19, T21. Effort S/M. Test: each state's copy present; assert absence of "Source:",
  "UNSUPPORTED_TASK", raw enums, "then refresh".
- **T25 - Colour-rule pass + delete legacy.** Delete `FixedWidthPanel`, `fixedWidth()`,
  `KEY_WIDTH/VALUE_WIDTH`, `row()`/`section()`/`wrappedValueLabel()`/`chips()`/`addChip()` and every
  `<html>` usage; enforce brand-only-for-state. Deps: T19, T20, T21, T22. Effort M. Test: no `<html>`
  in any label text; section headers not BRAND_ORANGE; brand appears only on active mode / active
  state / recommended location.
- **T26 - Collapsible sections (DEFERRED, optional P2-13).** Not required for v1; ticket only.

## 7. Conflict-safe sequencing and parallelism

Hot files (serialise): `ui/SlayerPanel.java` (every T18-T25 edits it) and `AllInSlayerPlugin.java`
(T17). Shared file: `ui/SlayerPanelState.java` (T15) - one commit, lands early.

- **Independent NEW files, safe to parallelise:** T01, T02, T03-T14, T16. Only ordering constraint is
  T01 before T03-T13 (token dependency) and T02 before T10/T11/T12 (seam dependency); T14 before T13's
  runtime (T07/T13 tests use in-memory icons, so tests are not blocked).
- **Must serialise:** T15 (shared state) lands before T17 and T18-T25 read the new fields. T18-T25 are
  strictly ordered (all touch `SlayerPanel.java`); do them as one engineer's sequence of small commits.

Recommended team: **2 frontend engineers.**
- **FE-A** owns the leaf library (T01, T02, T03-T14) then the `SlayerPanel` integration (T18-T25).
- **FE-B** owns state + plugin plumbing (T15, T16, T17) in parallel with FE-A's leaf library.
- They converge at **T18** (needs T15's state shape + T09/T13 components). A 3rd engineer would collide
  on `SlayerPanel.java`/`SlayerPanelState.java` and is not recommended; if a 3rd is available, scope
  them to QA building the manual checklist and to the icon assets (T14).

Integration order at the merge point: T15 -> (T17 in parallel) -> T18 -> T19 -> T20 -> T21 -> T22 ->
T23 -> T24 -> T25.

## 8. Test strategy

TDD throughout (red-green-refactor), matching the existing headless Swing pattern in
`SlayerPanelTest` / `SlayerPanelStateTest`.

How to test Swing components headlessly (proven by the existing suite):
- Construct real Swing components directly in the test; drive `render`/`update` via
  `SwingUtilities.invokeAndWait` (the existing `runOnEdt` helper). No `GraphicsEnvironment.isHeadless`
  guard is needed - the current tests run under gradle without one and pass.
- Walk the tree with the existing helpers (`allComponents`, `labelTexts`, `componentByName`). Keep
  naming components via `setName(...)` (the current code already names rows like
  `task-row-remaining-value`) so tests find them by stable name.
- **Never call `ItemManager.getImage(...)` in a test** - `AsyncBufferedImage` needs a live
  `ClientThread` (verified). Inject a fake `ItemIconRenderer` (no-op or call-recording). This is the
  whole point of ADR-0001.
- Mockito (3.12.4) is available for `ItemManager`/plugin-level tests (T02, T17).

What to assert (unit-testable):
- Structure: component present/absent by name and type; child counts; layout type
  (`DynamicGridLayout` rows/cols); section presence per `PanelStatus` and per `developerMode`.
- State: `enabled` (export), `selected` (mode), combo identity (`==`) and selection across renders,
  preserved scrollbar value, rebuild-count probe for self-diffing.
- Colour: foreground/background equals the expected `ColorScheme` token per state (OWNED/UPGRADE/
  BLOCKED, active mode brand, section header not brand).
- Behaviour: callbacks fire (`onToggleMode`, `onSelectLocation`, `onRefresh`); humane-source mapping;
  Wiki popup item present; no `<html>` and no dev-language strings in rendered text.
- The icon-renderer seam: the fake records (id, qty, stackable, dim) the component asked for.

What cannot be unit-tested (covered by the manual checklist below):
- Actual async sprite pixels / image load (needs a live `ItemManager`/client cache).
- Real font glyph rendering and visual polish (hover colours, spacing feel).
- Clipboard export side effect end to end (the callback is asserted; the OS clipboard is not).

Pitfalls observed / anticipated:
- `FontManager.getRunescape*Font()` loads bundled fonts from the client jar on the test classpath -
  expected to work headless; T03 is the canary. If static init fails headless, do not assert exact
  `Font` in unit tests (font is visual) - assert the role via component identity instead.
- Adding fields to the `@Value SlayerPanelState` ripples to factory call sites - mitigated by keeping
  short overloads (Section 4 / Risk R2). Grep call sites at build time
  (`forTask(`/`noTask(`/`unsupportedTask(`) before changing signatures.
- The existing "escapes HTML" test inverts meaning under ADR-0004: it now asserts the literal string
  is present and NOT interpreted as markup. Update it, do not delete the intent.
- `IconButton`/`ActionBar`: if the icon PNGs are missing, `ImageUtil.loadImageResource` throws.
  IconButton takes an `Icon` so its tests use an in-memory image; only `ActionBar` runtime needs the
  real asset (T14).

### Manual verification checklist (run in a dev RuneLite client)

1. Open the panel with no task: header shows "No Slayer task" + humane "updated HH:mm . startup"; one
   `PluginErrorPanel`; export greyed.
2. Get/check a supported task: TASK + WHERE & HOW + LOADOUT render; worn gear shows sprites + names +
   slots; inventory grid shows sprites; required item shows owned/missing colour.
3. Sprites actually paint (not blanks) and repaint when loaded; hover on an item row highlights;
   right-click an item row -> "Wiki" opens the correct OSRS wiki lookup.
4. Toggle DPS/Cost: both segments visible, active is orange; gear/DPS/cost update; scroll position and
   selected location do not jump.
5. Kill a monster: remaining count ticks down with no flicker and no scroll jump (in-place header).
6. Override location in the combo: only WHERE & HOW updates; the combo keeps focus/popup; the choice
   persists across the next reactive re-render.
7. Export enables only with a recommendation; clicking it copies the Inventory Setups string.
8. Toggle `developerMode` in settings: "Diagnostics (developer)" appears/disappears; when on, it shows
   the raw varps, menu action, mapped item id, detector result.
9. Long task/item names wrap or truncate inside ~225px; no horizontal scrollbar ever appears.

## 9. NFRs (checkable numbers)

- **Re-render cost:** an event whose visible sections' slices are unchanged triggers **0** section
  rebuilds (rebuild-count probe). Target `render(state)` on the EDT < 16ms for in-place updates
  (not unit-asserted; manual/observed in step 5).
- **Scroll stability:** vertical scrollbar value is **identical** before/after a re-render whose
  visible sections are unchanged (assert exact int).
- **Combo stability:** the location `JComboBox` is the **same instance** (`==`) across >=2 renders.
- **Thread safety:** **0** calls to `ItemManager.getItemComposition`/`getItemPrice` on the EDT (names
  and prices resolve on the client thread into the state; `getImage(...).addTo(...)` on the EDT is the
  sanctioned async exception per ADR-0001).
- **Width discipline:** **0** horizontal scrollbars; **0** `<html>` labels; **0** hardcoded
  pixel-width math (`FixedWidthPanel`/`KEY_WIDTH`/`VALUE_WIDTH` deleted); content width derives from
  layout managers within `PluginPanel.PANEL_WIDTH` (225).
- **Accessibility:** all text/background pairings >= WCAG AA per `ui-recommendations.md` §9
  (text/primary ~11.6:1, text/secondary ~7.4:1, brand ~6.2:1, blocked-red ~4.0:1 reserved for short
  tags/numbers); `text/muted` is decorative-only, never the sole carrier of load-bearing text.

## 10. Risks and mitigations

- **R1 - Icon assets (refresh.png/export.png).** Without them `ImageUtil.loadImageResource` throws at
  runtime. Mitigation: IconButton takes an `Icon` (tests use in-memory images); create the two 16x16
  monochrome PNGs early (T14) under `resources/icons/`, referenced as `/icons/refresh.png` (matches
  the existing `/panel_icon.png` convention). UI designer or a simple generated glyph suffices; hover/
  disabled variants are generated in code via `ImageUtil` (no extra art).
- **R2 - `@Value` state field additions ripple to call sites.** Mitigation: keep the short factory
  overloads defaulting `itemPrices=emptyMap`/`developerMode=false`; only the plugin uses the full
  overload. Grep `forTask(`/`noTask(`/`unsupportedTask(` before editing.
- **R3 - Per-item price source.** Confirmed: `ItemManager.getItemPrice(int)` exists, so per-item GE
  price is available (not only `Recommendation.totalGearCost`). Resolve on the client thread into
  `itemPrices`; treat 0/absent as muted so P0 sprites never block on price.
- **R4 - Inventory stack counts.** `Recommendation.inventory` is `List<Integer>` with no quantities,
  so inventory sprites render at qty=1 (no stack number for runes/cannonballs). Known limitation;
  adding quantities to the model is a deliberate out-of-scope follow-up.
- **R5 - Self-diff correctness.** A wrong slice comparison either leaves stale UI or over-rebuilds.
  Mitigation: compare exactly the fields each section reads using the existing `equals()` on
  `TaskData`/`Recommendation`; cover with T22's rebuild-count probe.
- **R6 - Headless FontManager.** Low risk (font ships in the test-classpath client jar); T03 is the
  canary; fall back to not asserting exact `Font` if static init misbehaves headless.

## 11. Gate 2 - decision requested

Approve: (a) the 4 ADRs in `docs/adr/`, (b) this phased, dependency-ordered task breakdown (T01-T25,
T26 deferred), and (c) the 2-engineer parallelisation with the serial `SlayerPanel` integration.
Recommendation: approve as written and start with Phase 0 (T01, T02) plus the icon assets (T14) so the
seam, tokens, and assets are ready before the leaf library and integration begin.

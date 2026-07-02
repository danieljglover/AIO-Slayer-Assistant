# All-In Slayer — Side Panel Redesign: Build Summary

- Date: 2026-06-29
- Track: T2 implementation. Design = `ui-recommendations.md` + `ux-recommendations.md`; plan = `docs/plan.md` + `docs/adr/0001–0004`.
- Status: **COMPLETE & merge-gate approved.** `gradlew.bat clean test` → **143 tests, 0 fail / 0 error / 0 skip** (29 suites; independently re-run). Not yet committed (commit is the user's call).

## What shipped (the whole P0+P1+P2 roadmap)

| Roadmap | Delivered |
|---|---|
| P0-1 | Gear rendered as **item sprites** (worn rows + equipment grid, inventory grid, required item, missing upgrades) via an injected `ItemIconRenderer` seam (`ItemManager.getImage(...).addTo()`). |
| P0-2 | **Compact icon action bar** (~24px) — borderless Refresh + Export icons with tooltips — replaces the 3 stacked full-width text buttons (~100px). |
| P0-3 | **Segmented `[DPS][Cost]`** control (active = brand) replaces the relabel button. |
| P0-4 | **Humanised header** — `updated HH:mm · helm check`; raw enums/`Source:`/seconds gone. |
| P0-5 | **Debug gated** behind `developerMode` config; default players never see it (and it's cleared, not just hidden — no telemetry lingers). |
| P1-6 | **One scrollable dashboard** (Task → Where & How → Loadout); 3-tab split removed. |
| P1-7 | Reactive by default; Refresh demoted to a header icon. |
| P1-8 | **Non-destructive re-render** — self-diffing sections (0 rebuilds on unchanged slice), preserved scroll/mode/selected-location, combo built once; `GameStateChanged` filtered to `LOGGED_IN`. |
| P1-9 | **Player-language** empty/error states (+ "no bank seen yet" note); no dev language. |
| P1-10 | Owned/missing **state colour**: required item (authoritative ownership signal) + Slayer-level met/unmet + dimmed unowned upgrades; brand reserved for real state. |
| P2-11 | One unified `KeyValueRow`/`SectionCard`; `FixedWidthPanel` + all `<html>`/width hacks deleted. |
| P2-12 | Per-item **right-click → Wiki** (`LinkBrowser` → Special:Lookup). |
| P2-13 | **Collapsible** sections (chevron + bold header; collapse persists across re-render). |
| P2-14 | N/A — tabs removed by the merge; icon tabs explicitly out of scope. |

## How it was built
7 TDD waves (foundations → leaf library → item components → plugin plumbing → panel integration → finish → review-fixes), each test-first and green before the next. Merge gate: review found 1 blocker (required-item ownership computed from a weak proxy) + 2 should-fixes; all fixed in W7; fresh re-review = **APPROVE**.

## New/changed code (high level)
- New: `ui/theme/SlayerTheme`, `ui/ItemIconRenderer` (+`ItemManagerIconRenderer`), `ui/components/{KeyValueRow, SectionCard, SectionHeader, Tag, PriceLabel, IconButton, ModeSelector, TaskHeader, LoadoutItemRow, EquipmentGrid, EquipmentSlotCell, InventoryGrid, ActionBar, ItemWiki}`, `loadout/LoadoutItems`, `resources/icons/{refresh,export}.png`, plus matching tests.
- Rewritten: `ui/SlayerPanel` (3-tab → single self-diffing dashboard).
- Modified: `ui/SlayerPanelState` (price + developerMode + requiredItemOwned, all additive), `AllInSlayerPlugin` (prices, devMode, LOGGED_IN filter, onConfigChanged), `AllInSlayerConfig` (developerMode), `task/TaskRefreshTrigger`.

## Known limitations / accepted (from review)
- **R4**: `Recommendation.inventory` carries no quantities → inventory sprites show qty=1 (model change is a separate follow-up).
- **N3/N5/N6** accepted as documented: dev-only diagnostic-key clipping; `onConfigChanged` shows "manual refresh" meta on a dev-mode toggle; ~12px trailing strut when Diagnostics is hidden.

## Still to do (needs a live client — can't run headless)
The manual in-client checklist in **`docs/plan.md` §8**: confirm sprites actually paint/repaint, hover + right-click Wiki, wheel-scroll with hidden scrollbar, combo focus/popup retained, remaining-count ticks with no flicker/scroll-jump, dev-mode toggle shows/hides Diagnostics, long names never produce a horizontal scrollbar.

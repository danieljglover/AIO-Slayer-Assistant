# All-In Slayer — Side Panel: Round-3 Refinement Plan

- Date: 2026-06-29
- Trigger: user feedback on the round-2 build (`round3-current-ui.png`) — "it's better", but (1) the
  visible scrollbar isn't wanted (wheel scrolling feels natural), and (2) the loadout grid is off-centre
  along with other parts. User cited **Flipping Copilot** as exemplary side-panel layout.
- Input: [`research-r3-flipping-copilot-layout.md`](research-r3-flipping-copilot-layout.md) (FC teardown + RuneLite core scroll truth).

## Findings (verified)
- **Scroll:** `VERTICAL_SCROLLBAR_NEVER` genuinely disables wheel scrolling — `BasicScrollPaneUI`'s wheel
  handler skips an invisible bar (confirms RV3 N-B; our W9 `AS_NEEDED` change was the right call for the
  wheel, but it shows a bar). Flipping Copilot keeps `AS_NEEDED` and **shrinks the vertical bar to a
  sliver / no-paint UI** (`StatsPanelV2.java:109`) so the wheel works with no visible bar. It does NOT use
  `super(true)` (that would un-anchor our header band).
- **Alignment:** FC never drops a narrow component straight into a `BoxLayout.Y_AXIS`; it wraps it in a
  full-width `BorderLayout(WEST)` / `FlowLayout(LEFT)` row with `setMaximumSize(MAX, prefHeight)`
  (`FlipPanel:40-42`). In our panel the **two grids are the only width-capped children**, so they're the
  only elements that float off-centre; every other row is already full-width left. Wrapping the grids in a
  left-pinning full-width row fixes it and makes alignment immune to `alignmentX`.
- **Polish:** our visual system is already on-parity (same `ColorScheme`/`FontManager` tokens). Optional
  tasteful adds: `MatteBorder` hairline dividers between sections; a slightly larger "hero" remaining count.

## Round-3 fix list → W10
| # | Fix | Source | Effort |
|---|---|---|---|
| G1 | **Hide the scrollbar, keep the wheel** — keep custom `JScrollPane` + `AS_NEEDED`; set the vertical bar to a zero/sliver size + a no-paint `ScrollBarUI` (RuneLite `CustomScrollBarUI` style). Wheel scroll stays on. Do NOT switch to `super(true)`. | Flipping Copilot §3a | S |
| G2 | **Fix off-centre alignment** — wrap `EquipmentGrid` + `InventoryGrid` in a full-width left-pinning row (`leftRow()` helper: full-width, `setMaximumSize(MAX, prefH)`, grid pinned WEST/left). Audit the other section children and assert none float. | Flipping Copilot §3b | S |
| G3 | **(Optional, tasteful) polish** — hairline `MatteBorder` section dividers + a slightly larger remaining-count number, only if clean. | FC §3c | S |
| G4 | **Lock it with tests** — scrollbar not visible (pref width 0 / no-paint UI) but wheel enabled + policy AS_NEEDED; grids sit at the section's left edge (x==0 after layout) like the worn rows; no `dashboard-body` child floats. Update grid `componentByName` lookups (now one wrapper deeper). | — | S |

G1+G2 resolve the two reported issues; G3 is light polish per the user's "refine visually"; G4 prevents regress.

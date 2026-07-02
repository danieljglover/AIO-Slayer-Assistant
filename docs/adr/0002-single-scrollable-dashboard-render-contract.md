---
status: accepted
---

# One scrollable dashboard with self-diffing sections behind a single `render(SlayerPanelState)`

Confirmed product decision (README "Decisions - CONFIRMED 2026-06-29", item 2): drop the
Task/Loadout `MaterialTab` split and present one scrollable dashboard ordered by the three player
questions (header -> TASK -> WHERE & HOW -> LOADOUT). We keep `void render(SlayerPanelState)` as the
panel's sole public update entry (spec FR-1) and make re-render non-destructive, because the panel
reacts to nearly every game event and a full teardown each time causes the flicker, scroll jumps,
and combo-box drops the audit flagged (Finding 9).

Structure: `PluginPanel` (BorderLayout) -> NORTH a non-scrolling header band (`TaskHeader` +
`ActionBar`) -> CENTER one `JScrollPane` (built once) wrapping a `dashboardBody` (BoxLayout Y) that
holds stable `SectionCard` containers: Task, Where & How, Loadout, and (dev-mode only) Diagnostics.

Re-render contract (the load-bearing part):
- Header text, mode selection, and export-enabled update **in place** every render (no rebuild).
- The location `JComboBox` is **built once** and only its model/selection updated in place, never
  recreated (preserves open popup/focus mid-interaction).
- Each section is a **self-diffing** component: `render()` calls `section.update(state)`, and the
  section rebuilds its body only if its backing slice changed (compared via the `@Data`/`@Value`
  `equals()` already on `TaskData`/`Recommendation`/`SlayerPanelState`). Unchanged slice -> no-op.
- The vertical scrollbar value is captured before and restored after render as belt-and-suspenders.
- `GameStateChanged` is filtered to `LOGGED_IN` in the plugin so login/loading blips do not churn.

Why not the 2-tab fallback (Option C in the UX doc): it still splits one decision across a mode
change; the merge was explicitly chosen and signed off. Why self-diffing sections rather than a
global `removeAll()` rebuild: the global rebuild is what makes the panel feel twitchy and resets
scroll; localising the diff keeps `render()` simple while making the common case (a kill ticking the
remaining count) a pure in-place header update that touches the scroll body not at all.

This supersedes the spec's "Tab Selection persists across render" requirement: with no Task/Loadout
tabs, tab persistence generalises to scroll-position + mode + selected-location persistence.

# Triage — All-In Slayer UI/UX Review

- Date: 2026-06-29
- Requested by: Daniel Glover
- Track: **T2-style design review** (analysis + design deliverable; no code shipped in this engagement)

## Problem

The All-In Slayer side panel (RuneLite Swing `PluginPanel`) "feels clunky and unintuitive."
A side-panel redesign was already specced (`docs/superpowers/specs/2026-06-28-all-in-slayer-side-panel-design.md`)
and implemented (`SlayerPanel`), yet the result still feels off. We need an evidence-based review
that compares the current UI against the UIs of the top-performing RuneLite Plugin Hub plugins and
produces concrete, prioritised, actionable recommendations.

## Users

OSRS players running RuneLite who use the plugin while actively doing Slayer. They expect the panel to
follow RuneLite-native conventions (item sprites, icon action bars, reactive updates) used by the
plugins they already have installed.

## Scope

- Identify the top-performing plugins on the Plugin Hub (by install count) and review the UIs of those
  with notable side panels / overlays (read their git repos).
- Audit the current All-In Slayer UI (`SlayerPanel`, `SlayerOverlay`) for clunky / unintuitive aspects,
  with file:line evidence.
- Produce a consolidated recommendations report: UX (information architecture, workflows, interaction),
  UI (visual language, icons, item sprites, spacing, colour, action layout, tabs), and RuneLite/Swing
  feasibility, each mapped to current code and prioritised (P0/P1/P2) with effort estimates.

## Non-goals

- No code changes / redesign implementation in THIS engagement (review + recommendations only).
- No new automation, input generation, or network features (the plugin is and stays passive).
- Not re-litigating the already-approved 3-tab information model unless the comparison shows it harms UX.

## Success criteria

- A player-facing recommendations report that names, for each finding: the problem, the evidence (top
  plugin doing it better + current code location), the concrete fix, and a priority + rough effort.
- Recommendations are RuneLite-native and implementable within `PluginPanel` / Swing constraints.

## Constraints

- RuneLite Swing (`PluginPanel`, `ColorScheme`, `MaterialTabGroup`, `ItemManager`, overlay components).
- Fixed side-panel width (`PluginPanel.PANEL_WIDTH`).
- Must keep client reads on the client thread, Swing writes on the EDT.

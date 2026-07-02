---
status: accepted
---

# Debug diagnostics are gated behind a developer-mode config, carried in `SlayerPanelState`

Confirmed product decision (README item 1): default players see only player-facing content; the
diagnostics surface (raw varps, menu action, mapped item ids, detector result) renders only when a
new `AllInSlayerConfig.developerMode()` `@ConfigItem` (default `false`) is on. No top Plugin Hub
panel ships a player-facing debug surface, and the helm/gem diagnosis phase is treated as complete.

We carry the flag in the render state - add `boolean developerMode` to `SlayerPanelState`, populated
from `config.developerMode()` in `recompute(...)` - rather than giving the panel a side-channel
setter or injecting `AllInSlayerConfig` into the panel. Reason: the panel already renders purely from
one `SlayerPanelState` (spec FR-1 / ADR-0002); debug visibility is part of "what to render", so it
belongs in the state, keeping the single-source-of-truth contract intact and the decision unit-
testable (render with `developerMode=true/false` and assert the section's presence). When on, the
Diagnostics surface is a subordinate `SectionCard` titled "Diagnostics (developer)" at the bottom of
the same scroll (not a re-introduced tab - there is no tab system after ADR-0002). To make the toggle
live without a game event, the plugin subscribes to `ConfigChanged` for the `allinslayer` group and
schedules a recompute.

This is a deliberate deviation from the approved spec, which says the Debug tab "is part of the
product" and "do not hide it behind a developer config" (spec lines 280, 363, 725). That guidance was
correct for the diagnostic phase; this ADR records that the phase is over and the diagnostic
capability is fully preserved (identical fields, identical state), just no longer the first thing a
default player sees. Recorded here so the next reader does not "restore" the always-on Debug tab.

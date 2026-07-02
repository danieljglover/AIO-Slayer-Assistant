---
status: accepted
---

# Bank-gate on the persisted `bankLastSeen()`, and gate the loadout card only (not the whole panel)

The engine produces no loadout until a bank snapshot exists, gated on
`InventoryService.bankLastSeen() != null` (the **persisted** RS-profile snapshot), **not** an
in-memory "scanned this session" flag. New `PanelStatus.BANK_NOT_SCANNED` is set when a task is
detected but no snapshot exists; in that state the Task and Where/How sections still render and only
the **loadout card** shows an "open your bank once" prompt.

Why persisted, not session: our `InventoryService` already persists the bank snapshot to RS-profile
config and exposes its age, unlike the reference plugin (`adamgiles1/bank-equipment-stat-filter`)
which holds the bank in memory and so re-gates every session. Persisted honours the requirement ("no
advice until the bank has been opened") while being far less annoying - the player opens the bank once,
ever, and advice persists across restarts. The trade-off is staleness: a persisted snapshot can lag
reality (gear/runes moved since). We accept this and **surface the bank age prominently** rather than
enforce freshness; flipping to a session flag later is a one-line change in the gate.

Why gate the card, not the panel: task intel and location advice do not need the bank, so hiding them
when the bank is unseen would be worse UX. `BANK_NOT_SCANNED` therefore counts as "has task" for the
panel's section-visibility logic; only the recommendation is withheld.

Consequence: the gate decision lives in `AllInSlayerPlugin.recompute` (it already reads
`bankLastSeen()`), expressed through a new `SlayerPanelState.bankNotScanned(...)` factory so the state
selection is unit-testable headlessly. The advisor itself stays bank-agnostic (it just consumes
`OwnedItems`).

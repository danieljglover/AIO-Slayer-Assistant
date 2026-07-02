---
status: accepted
---

# Equipment stats come from `ItemManager.getItemStats(int)` through the existing provider seam, with a null=not-loaded contract and a per-id cache

Item combat stats are read via the non-deprecated `ItemManager.getItemStats(int)` (returns
`net.runelite.client.game.ItemStats`; **not** the deprecated 2-arg `getItemStats(int, boolean)` and
**not** the `net.runelite.http.api` type the reference plugin used). We keep the project's existing
`EquipmentStatsProvider` seam (`DefaultEquipmentStatsProvider` already calls the correct overload) so
selection and DPS estimation share one stats source and stay headless-testable behind a fakeable
interface.

The seam's value type (`Bonuses`) is enriched with the **slot index** and the **two-handed** flag
(from `ItemEquipmentStats.getSlot()` / `isTwoHanded()`) so the selector can group by slot and resolve
weapon/shield interplay. Slot mapping: `ItemEquipmentStats.getSlot()` (RuneLite
`EquipmentInventorySlot` index 0-13) maps to our `EquipmentSlot`; ARMS(6)/HAIR(8)/JAW(11) have no
combat slot in our model and are dropped.

Two traps, recorded because they are easy to get wrong:
1. **Null means "not loaded yet" OR "non-equipable".** The stat map loads async-remotely after client
   start, so `getItemStats` returns `null` for everything until the fetch completes. We **skip** a
   null-stat item this pass and never cache a permanent "no stats"; the frequent recompute (and the
   Refresh button) recovers it (PRD NFR-5). Optional probe: if every currently-worn item resolves
   null, the map is not ready.
2. **Client-thread only.** `getItemStats` -> `getItemComposition` -> `client.getItemDefinition`, all
   client-thread. All stat reads happen in `recompute` on the client thread and reach the panel only
   through the immutable `SlayerPanelState` (PRD FR-11 / NFR-3).

A per-id `Bonuses` cache is added (stats are immutable per id) so scanning a large bank is cheap after
warmup (PRD NFR-1/NFR-2).

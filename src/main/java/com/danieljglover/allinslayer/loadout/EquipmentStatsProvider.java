package com.danieljglover.allinslayer.loadout;

import com.google.inject.ImplementedBy;

/**
 * Seam over {@code ItemManager.getItemStats(int)} that yields one item's equipment {@link Bonuses}.
 *
 * <p><b>Null contract (ADR-0002):</b> {@link #get(int)} returns {@code null} - not
 * {@link Bonuses#zero()} - when the item has no equipment stats: either the item is non-equipable,
 * or the async remote stat map has not loaded yet. Callers must skip a {@code null} (never treat it
 * as "zero stats") and may retry on a later recompute; the implementation must never cache a
 * {@code null} as a permanent "no stats" (PRD NFR-5).
 */
@ImplementedBy(DefaultEquipmentStatsProvider.class)
public interface EquipmentStatsProvider
{
    /**
     * @return the item's equipment {@link Bonuses}, or {@code null} if it is non-equipable or the
     *     stat map is not yet loaded.
     */
    Bonuses get(int itemId);
}

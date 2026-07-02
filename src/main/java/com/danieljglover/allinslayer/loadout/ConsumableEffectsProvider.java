package com.danieljglover.allinslayer.loadout;

import com.google.inject.ImplementedBy;
import java.util.Map;
import java.util.Set;

/**
 * Seam over RuneLite's {@code ItemStatChangesService} (ADR-0004) that classifies a consumable: how
 * much an item heals, and which combat stats it positively boosts and by how much. Mirrors the
 * {@link EquipmentStatsProvider} seam idiom so the consumable selector is headless-testable behind a
 * fake and never touches the live service or {@code Client} in a test.
 *
 * <p>The implementation reads live effects from the service when it is available and populated, and
 * otherwise falls back to a curated map (LD00: the service's injectability could not be confirmed
 * headless, so the provider works regardless).
 */
@ImplementedBy(DefaultConsumableEffectsProvider.class)
public interface ConsumableEffectsProvider
{
    /** @return the item's heal in Hitpoints, or {@code null} if it is not food. */
    Integer healAmount(int itemId);

    /** @return the combat stats this item positively boosts (empty if none / not a potion). */
    Set<BoostedStat> boostedStats(int itemId);

    /**
     * @return {@code stat -> positive boost magnitude} for this item (empty if none). The key set
     *     equals {@link #boostedStats(int)}; the magnitudes drive the FR-5 best-potion ranking.
     */
    Map<BoostedStat, Integer> boostMagnitudes(int itemId);
}

package com.danieljglover.allinslayer.loadout;

import lombok.Value;

/**
 * One occupied slot of the suggested 28-slot trip inventory composed by {@link TripInventoryPlanner}:
 * the item to show, how many of it, and whether it stacks (so the cell renders a stack number).
 *
 * <p>Immutable value type so equal recommendations re-render with no rebuild (NFR-4 self-diff),
 * mirroring {@link Consumables}.
 */
@Value
public class TripSlot
{
    int itemId;
    int quantity;
    boolean stackable;
}

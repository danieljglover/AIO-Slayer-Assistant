package com.danieljglover.allinslayer.loadout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The mutable working set the {@link TripPlanner} layers fill, then materialise into the 28-slot trip
 * inventory. Supplies are added in priority order (highest first); {@link #toSlots(Integer)} expands
 * them, caps at {@link #CAPACITY}, and fills any remainder with food. A stackable entry occupies one
 * slot regardless of quantity; a non-stackable entry occupies one slot per unit (the OSRS reality for
 * potions/food). Also accumulates the "strategy wants but you lack" names for the missing-key-items
 * advisory (owned-only substitution stays the loadout's job).
 */
final class TripBag
{
    /** The OSRS inventory size - the trip inventory never exceeds this many slots. */
    static final int CAPACITY = 28;

    private static final class Entry
    {
        final int itemId;
        int count;            // non-stackable: number of slots/units; stackable: the stack quantity
        final boolean stackable;

        Entry(int itemId, int count, boolean stackable)
        {
            this.itemId = itemId;
            this.count = count;
            this.stackable = stackable;
        }
    }

    private final Map<Integer, Entry> entries = new LinkedHashMap<>();
    private final List<String> missingKeyItems = new ArrayList<>();
    private boolean truncated;

    /** Add {@code units} of an item, merging with any existing entry for the same id (idempotent by id). */
    void add(int itemId, int units, boolean stackable)
    {
        if (units <= 0)
        {
            return;
        }
        Entry existing = entries.get(itemId);
        if (existing == null)
        {
            entries.put(itemId, new Entry(itemId, units, stackable));
        }
        else
        {
            existing.count += units;
        }
    }

    /** Add a single unit of a supply when it is not already present (the common "one slot each" case). */
    void ensure(int itemId, boolean stackable)
    {
        if (!entries.containsKey(itemId))
        {
            entries.put(itemId, new Entry(itemId, 1, stackable));
        }
    }

    boolean contains(int itemId)
    {
        return entries.containsKey(itemId);
    }

    /** Record a strategy item the player owns none of, for the missing-key-items note (deduped by name). */
    void noteMissing(String name)
    {
        if (name != null && !name.trim().isEmpty() && !missingKeyItems.contains(name))
        {
            missingKeyItems.add(name);
        }
    }

    List<String> getMissingKeyItems()
    {
        return missingKeyItems;
    }

    boolean wasTruncated()
    {
        return truncated;
    }

    /**
     * Expand the entries in insertion (priority) order into at most {@link #CAPACITY} slots, then fill
     * any remaining slots with {@code fillFoodId} (the best owned food) when non-null. Sets the
     * truncated flag when the priority supplies alone overflow the bag.
     */
    List<TripSlot> toSlots(Integer fillFoodId)
    {
        List<TripSlot> slots = new ArrayList<>();
        for (Entry entry : entries.values())
        {
            if (entry.stackable)
            {
                if (slots.size() >= CAPACITY)
                {
                    truncated = true;
                    break;
                }
                slots.add(new TripSlot(entry.itemId, Math.max(1, entry.count), true));
            }
            else
            {
                for (int i = 0; i < entry.count; i++)
                {
                    if (slots.size() >= CAPACITY)
                    {
                        truncated = true;
                        break;
                    }
                    slots.add(new TripSlot(entry.itemId, 1, false));
                }
            }
        }
        if (fillFoodId != null)
        {
            while (slots.size() < CAPACITY)
            {
                slots.add(new TripSlot(fillFoodId, 1, false));
            }
        }
        return slots;
    }
}

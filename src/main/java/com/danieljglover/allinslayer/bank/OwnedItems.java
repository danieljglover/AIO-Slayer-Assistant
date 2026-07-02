package com.danieljglover.allinslayer.bank;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Item;

/** Immutable view of every item the player has across inventory, worn equipment, and last-seen bank. */
public final class OwnedItems
{
    private final Map<Integer, Integer> counts;

    private OwnedItems(Map<Integer, Integer> counts)
    {
        this.counts = counts;
    }

    public boolean has(int itemId)
    {
        return counts.getOrDefault(itemId, 0) > 0;
    }

    public int quantity(int itemId)
    {
        return counts.getOrDefault(itemId, 0);
    }

    public Set<Integer> ids()
    {
        return Collections.unmodifiableSet(counts.keySet());
    }

    public static OwnedItems fromCounts(Map<Integer, Integer> counts)
    {
        return new OwnedItems(new HashMap<>(counts));
    }

    public static OwnedItems fromContainers(Item[] inventory, Item[] worn, Item[] bank)
    {
        Map<Integer, Integer> m = new HashMap<>();
        addAll(m, inventory);
        addAll(m, worn);
        addAll(m, bank);
        return new OwnedItems(m);
    }

    private static void addAll(Map<Integer, Integer> m, Item[] items)
    {
        if (items == null)
        {
            return;
        }
        for (Item it : items)
        {
            if (it == null)
            {
                continue;
            }
            int id = it.getId();
            int q = it.getQuantity();
            if (id > 0 && q > 0)
            {
                m.merge(id, q, Integer::sum);
            }
        }
    }
}

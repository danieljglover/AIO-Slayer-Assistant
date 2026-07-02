package com.danieljglover.allinslayer.bank;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Item;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OwnedItemsTest
{
    @Test
    public void mergesCountsAcrossSources()
    {
        Map<Integer, Integer> inv = new HashMap<>();
        inv.put(560, 100);
        Map<Integer, Integer> bank = new HashMap<>();
        bank.put(560, 5000);
        bank.put(11865, 1);

        OwnedItems owned = OwnedItems.fromCounts(merge(inv, bank));

        assertTrue(owned.has(560));
        assertTrue(owned.has(11865));
        assertEquals(5100, owned.quantity(560));
        assertFalse(owned.has(999999));
    }

    @Test
    public void fromContainersFiltersEmptyAndNullSlots()
    {
        Item a = mock(Item.class);
        when(a.getId()).thenReturn(11865);
        when(a.getQuantity()).thenReturn(1);
        Item empty = mock(Item.class);
        when(empty.getId()).thenReturn(-1);
        when(empty.getQuantity()).thenReturn(0);

        OwnedItems owned = OwnedItems.fromContainers(
            new Item[]{a, empty, null}, null, null);

        assertTrue(owned.has(11865));
        assertEquals(1, owned.ids().size());
    }

    private static Map<Integer, Integer> merge(Map<Integer, Integer> a, Map<Integer, Integer> b)
    {
        Map<Integer, Integer> m = new HashMap<>(a);
        b.forEach((k, v) -> m.merge(k, v, Integer::sum));
        return m;
    }
}

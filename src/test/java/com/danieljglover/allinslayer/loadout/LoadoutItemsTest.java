package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.junit.Test;

public class LoadoutItemsTest
{
    @Test
    public void idsCollectsWornInventoryAndConsumablesDistinctInOrder()
    {
        Recommendation rec = new Recommendation();
        Map<EquipmentSlot, Integer> worn = new LinkedHashMap<>();
        worn.put(EquipmentSlot.WEAPON, 4151);
        worn.put(EquipmentSlot.HEAD, 10);
        rec.setWorn(worn);
        rec.setInventory(Arrays.asList(10, 20)); // 10 duplicates a worn item

        Map<Integer, Integer> runeReq = new LinkedHashMap<>();
        runeReq.put(554, 10); // fire rune
        runeReq.put(556, 7);  // air rune
        runeReq.put(20, 1);   // duplicates an inventory item
        MagicSetup magic = new MagicSetup("fire", "Fire Surge", 24, false, runeReq, Collections.emptyMap());
        rec.setConsumables(new Consumables(385, 3144, 12695, magic)); // shark, karambwan combo, super combat

        // worn -> inventory -> food -> combo -> potion -> rune ids, de-duplicated, order preserved.
        assertEquals(Arrays.asList(4151, 10, 20, 385, 3144, 12695, 554, 556),
            LoadoutItems.ids(rec));
    }

    @Test
    public void idsHandlesPartialConsumables()
    {
        Recommendation rec = new Recommendation();
        Map<EquipmentSlot, Integer> worn = new LinkedHashMap<>();
        worn.put(EquipmentSlot.WEAPON, 1333);
        rec.setWorn(worn);
        // food only, no combo / potion / magic.
        rec.setConsumables(new Consumables(385, null, null, null));

        assertEquals(Arrays.asList(1333, 385), LoadoutItems.ids(rec));
    }

    @Test
    public void idsIsNullSafe()
    {
        assertTrue(LoadoutItems.ids(null).isEmpty());

        Recommendation empty = new Recommendation(); // null worn / inventory / consumables
        assertTrue(LoadoutItems.ids(empty).isEmpty());
    }

    @Test
    public void pricesMapsPositiveValuesAndOmitsNonPositive()
    {
        IntUnaryOperator lookup = id ->
        {
            switch (id)
            {
                case 1:
                    return 100;
                case 2:
                    return 0;  // unknown price -> omitted (muted-safe, ADR-0004 / Risk R3)
                case 3:
                    return -5; // never-block sentinel -> omitted
                default:
                    return 0;
            }
        };

        Map<Integer, Integer> prices = LoadoutItems.prices(Arrays.asList(1, 2, 3), lookup);

        assertEquals(1, prices.size());
        assertEquals(Integer.valueOf(100), prices.get(1));
    }

    @Test
    public void pricesIsNullSafe()
    {
        assertTrue(LoadoutItems.prices(null, id -> 100).isEmpty());
        assertTrue(LoadoutItems.prices(Collections.singletonList(1), null).isEmpty());
    }
}

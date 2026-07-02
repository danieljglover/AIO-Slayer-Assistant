package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/**
 * LD03: {@link Consumables} / {@link MagicSetup} are value types and a {@link Recommendation}
 * carrying equal consumables is value-equal - the contract the panel relies on to skip rebuilds on
 * an unchanged recommendation (NFR-4 self-diff).
 */
public class ConsumablesTest
{
    @Test
    public void consumablesAreValueEqual()
    {
        Consumables a = new Consumables(385, 3144, 12695, null);
        Consumables b = new Consumables(385, 3144, 12695, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Consumables(385, null, 12695, null));
    }

    @Test
    public void magicSetupIsValueEqualIncludingItsRuneMaps()
    {
        Map<Integer, Integer> req1 = req();
        Map<Integer, Integer> req2 = req();

        MagicSetup a = new MagicSetup("fire", "Fire Surge", 24, false, req1, new LinkedHashMap<>());
        MagicSetup b = new MagicSetup("fire", "Fire Surge", 24, false, req2, new LinkedHashMap<>());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new MagicSetup("water", "Water Surge", 22, false, req(), new LinkedHashMap<>()));
    }

    @Test
    public void recommendationWithEqualConsumablesIsValueEqual()
    {
        Consumables c1 = new Consumables(385, 3144, 12695,
            new MagicSetup("fire", "Fire Surge", 24, false, req(), new LinkedHashMap<>()));
        Consumables c2 = new Consumables(385, 3144, 12695,
            new MagicSetup("fire", "Fire Surge", 24, false, req(), new LinkedHashMap<>()));

        Recommendation r1 = new Recommendation();
        r1.setConsumables(c1);
        Recommendation r2 = new Recommendation();
        r2.setConsumables(c2);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    private static Map<Integer, Integer> req()
    {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        m.put(556, 7);   // air
        m.put(554, 10);  // fire
        m.put(21880, 1); // wrath
        return m;
    }
}

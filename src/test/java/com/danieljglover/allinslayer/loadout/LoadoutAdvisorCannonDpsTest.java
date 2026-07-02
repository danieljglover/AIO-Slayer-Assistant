package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.SlayerLocation;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * WD-6 (ADR-0020 #3): the gating of the honest single-target cannon DPS line. It appears ONLY when the
 * effective location supports a cannon AND the player owns one; a multi-combat spot keeps the number and
 * appends the ceiling caveat. Absent otherwise (FR-6).
 */
public class LoadoutAdvisorCannonDpsTest
{
    // Dwarf multicannon part ids (mirrors InventorySelector.CANNON_PARTS).
    private static final int[] CANNON_PARTS = {6, 8, 10, 12};

    @Test
    public void cannonLocationWithOwnedCannonShowsTheSingleTargetLine()
    {
        String note = LoadoutAdvisor.cannonDpsNote(cannonLocation(), ownsCannon(), false);
        assertTrue(note.contains("Cannon adds") && note.contains("single-target"));
        assertTrue("single-target spot carries no ceiling caveat",
            !note.contains("real rate is higher"));
    }

    @Test
    public void multicombatCannonSpotAppendsTheCeilingCaveat()
    {
        String note = LoadoutAdvisor.cannonDpsNote(cannonLocation(), ownsCannon(), true);
        assertTrue("keeps the single-target number", note.contains("single-target"));
        assertTrue("adds the honest multi ceiling caveat", note.contains("real rate is higher"));
    }

    @Test
    public void noLineWithoutAnOwnedCannon()
    {
        assertNull(LoadoutAdvisor.cannonDpsNote(cannonLocation(), OwnedItems.fromCounts(new HashMap<>()),
            false));
    }

    @Test
    public void noLineOnANonCannonLocation()
    {
        SlayerLocation nonCannon = new SlayerLocation("swamp", false, false, false, false);
        assertNull(LoadoutAdvisor.cannonDpsNote(nonCannon, ownsCannon(), false));
        assertNull("null location -> no line", LoadoutAdvisor.cannonDpsNote(null, ownsCannon(), false));
    }

    private static SlayerLocation cannonLocation()
    {
        return new SlayerLocation("cannon-spot", true, true, false, false);
    }

    private static OwnedItems ownsCannon()
    {
        Map<Integer, Integer> m = new HashMap<>();
        for (int part : CANNON_PARTS)
        {
            m.put(part, 1);
        }
        return OwnedItems.fromCounts(m);
    }
}

package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DT-B9 (ADR-0017 #4): owned-driven inventory supplies. Every curated cannon/cannonball/antifire id is
 * pinned by a behaviour row against the real {@code net.runelite.api.ItemID} constant (R3), so a
 * transcribed literal that drifts from the client constant fails the build.
 */
public class InventorySelectorTest
{
    private static final MonsterProfile DRAGON =
        new MonsterProfile(null, null, false, true, false, false, false, null);
    private static final MonsterProfile NON_DRAGON =
        new MonsterProfile(null, null, false, false, false, false, false, null);
    private static final SlayerLocation CANNONABLE =
        new SlayerLocation("Brimhaven Dungeon", true, true, false, false);
    private static final SlayerLocation NO_CANNON =
        new SlayerLocation("Slayer Tower", false, false, false, false);

    private static OwnedItems owned(int... ids)
    {
        Map<Integer, Integer> m = new HashMap<>();
        for (int id : ids)
        {
            m.put(id, 1);
        }
        return OwnedItems.fromCounts(m);
    }

    private static TaskData requiredItemTask(int id)
    {
        TaskData t = new TaskData();
        t.setRequiredItemId(id);
        return t;
    }

    // --- required item (PD-4) ------------------------------------------------------------------

    @Test
    public void addsRequiredItemWhenOwned()
    {
        List<Integer> s = InventorySelector.select(
            owned(ItemID.ROCK_HAMMER), NON_DRAGON, NO_CANNON, requiredItemTask(ItemID.ROCK_HAMMER));
        assertTrue("required item added when owned", s.contains(ItemID.ROCK_HAMMER));
    }

    @Test
    public void omitsRequiredItemWhenNotOwned()
    {
        List<Integer> s = InventorySelector.select(
            owned(), NON_DRAGON, NO_CANNON, requiredItemTask(ItemID.ROCK_HAMMER));
        assertFalse("required item omitted when unowned", s.contains(ItemID.ROCK_HAMMER));
        assertTrue("nothing else applies", s.isEmpty());
    }

    // --- cannon (PD-2, owned-driven) -----------------------------------------------------------

    private static OwnedItems fullCannon(int... extra)
    {
        int[] parts = {ItemID.CANNON_BASE, ItemID.CANNON_STAND, ItemID.CANNON_BARRELS, ItemID.CANNON_FURNACE};
        int[] all = new int[parts.length + extra.length];
        System.arraycopy(parts, 0, all, 0, parts.length);
        System.arraycopy(extra, 0, all, parts.length, extra.length);
        return owned(all);
    }

    @Test
    public void addsCannonPartsAndCannonballWhenCannonableAndCannonOwned()
    {
        List<Integer> s = InventorySelector.select(
            fullCannon(ItemID.STEEL_CANNONBALL), NON_DRAGON, CANNONABLE, new TaskData());
        for (int part : new int[]{ItemID.CANNON_BASE, ItemID.CANNON_STAND, ItemID.CANNON_BARRELS,
            ItemID.CANNON_FURNACE})
        {
            assertTrue("cannon part " + part, s.contains(part));
        }
        assertTrue("owned cannonball added", s.contains(ItemID.STEEL_CANNONBALL));
    }

    @Test
    public void omitsCannonWhenLocationNotCannonable()
    {
        List<Integer> s = InventorySelector.select(
            fullCannon(ItemID.STEEL_CANNONBALL), NON_DRAGON, NO_CANNON, new TaskData());
        assertFalse("no cannon at a non-cannonable location", s.contains(ItemID.CANNON_BASE));
    }

    @Test
    public void omitsCannonWhenNotAllFourPartsOwned()
    {
        // owns base+stand+barrels + a cannonball but not the furnace -> no assembled cannon
        List<Integer> s = InventorySelector.select(
            owned(ItemID.CANNON_BASE, ItemID.CANNON_STAND, ItemID.CANNON_BARRELS, ItemID.STEEL_CANNONBALL),
            NON_DRAGON, CANNONABLE, new TaskData());
        assertFalse("no cannon without all four parts", s.contains(ItemID.CANNON_BASE));
    }

    @Test
    public void cannonAddedWithoutAmmoWhenNoCannonballOwned()
    {
        // owns the full cannon but no cannonballs -> parts only (owned-only; no fabricated ammo)
        List<Integer> s = InventorySelector.select(fullCannon(), NON_DRAGON, CANNONABLE, new TaskData());
        assertTrue("cannon parts still packed", s.contains(ItemID.CANNON_BASE));
        assertFalse("no steel ball fabricated", s.contains(ItemID.STEEL_CANNONBALL));
        assertFalse("no granite ball fabricated", s.contains(ItemID.GRANITE_CANNONBALL));
    }

    @Test
    public void prefersTheOwnedCannonballType()
    {
        List<Integer> s = InventorySelector.select(
            fullCannon(ItemID.GRANITE_CANNONBALL), NON_DRAGON, CANNONABLE, new TaskData());
        assertTrue("granite ball added when it is the owned type", s.contains(ItemID.GRANITE_CANNONBALL));
        assertFalse("no steel ball fabricated over the owned granite", s.contains(ItemID.STEEL_CANNONBALL));
    }

    // --- antifire (PD-3, off the dragon flag) --------------------------------------------------

    @Test
    public void addsAntifireWhenDragonAndOwned()
    {
        List<Integer> s = InventorySelector.select(
            owned(ItemID.SUPER_ANTIFIRE_POTION4), DRAGON, NO_CANNON, new TaskData());
        assertTrue("owned antifire added on a draconic task", s.contains(ItemID.SUPER_ANTIFIRE_POTION4));
    }

    @Test
    public void omitsAntifireWhenNotDragon()
    {
        List<Integer> s = InventorySelector.select(
            owned(ItemID.SUPER_ANTIFIRE_POTION4), NON_DRAGON, NO_CANNON, new TaskData());
        assertFalse("no antifire on a non-draconic task", s.contains(ItemID.SUPER_ANTIFIRE_POTION4));
    }

    @Test
    public void omitsAntifireWhenDragonButNoneOwned()
    {
        List<Integer> s = InventorySelector.select(owned(), DRAGON, NO_CANNON, new TaskData());
        assertTrue("nothing added when no antifire owned", s.isEmpty());
    }

    // --- antifire "note when unowned" (PD-3 / DT-B11) ------------------------------------------

    @Test
    public void antifireNoteWhenDragonAndNoneOwned()
    {
        String note = InventorySelector.antifireNote(owned(), DRAGON);
        assertNotNull("draconic + no antifire owned -> a nudge note", note);
        assertTrue("the note recommends antifire", note.contains("Antifire"));
    }

    @Test
    public void noAntifireNoteWhenDragonButOwned()
    {
        assertNull("owns antifire -> the supply covers it, no nudge",
            InventorySelector.antifireNote(owned(ItemID.SUPER_ANTIFIRE_POTION4), DRAGON));
    }

    @Test
    public void noAntifireNoteWhenNotDragon()
    {
        assertNull("non-draconic task -> no antifire nudge",
            InventorySelector.antifireNote(owned(), NON_DRAGON));
    }

    // --- R3: one behaviour row per curated id, pinned against the real ItemID constant ---------

    @Test
    public void everyCuratedAntifireIdIsRecognised()
    {
        int[] antifire = {
            ItemID.SUPER_ANTIFIRE_POTION4, ItemID.SUPER_ANTIFIRE_POTION3, ItemID.SUPER_ANTIFIRE_POTION2,
            ItemID.SUPER_ANTIFIRE_POTION1,
            ItemID.EXTENDED_SUPER_ANTIFIRE4, ItemID.EXTENDED_SUPER_ANTIFIRE3, ItemID.EXTENDED_SUPER_ANTIFIRE2,
            ItemID.EXTENDED_SUPER_ANTIFIRE1,
            ItemID.EXTENDED_ANTIFIRE4, ItemID.EXTENDED_ANTIFIRE3, ItemID.EXTENDED_ANTIFIRE2,
            ItemID.EXTENDED_ANTIFIRE1,
            ItemID.ANTIFIRE_POTION4, ItemID.ANTIFIRE_POTION3, ItemID.ANTIFIRE_POTION2, ItemID.ANTIFIRE_POTION1,
            ItemID.DRAGONFIRE_WARD, ItemID.ANCIENT_WYVERN_SHIELD, ItemID.DRAGONFIRE_SHIELD,
        };
        for (int id : antifire)
        {
            List<Integer> s = InventorySelector.select(owned(id), DRAGON, NO_CANNON, new TaskData());
            assertTrue("antifire id recognised: " + id, s.contains(id));
        }
    }

    @Test
    public void everyCuratedCannonballIdIsRecognised()
    {
        for (int ball : new int[]{ItemID.STEEL_CANNONBALL, ItemID.GRANITE_CANNONBALL})
        {
            List<Integer> s = InventorySelector.select(fullCannon(ball), NON_DRAGON, CANNONABLE, new TaskData());
            assertTrue("cannonball recognised: " + ball, s.contains(ball));
        }
    }

    // --- combination + null-safety -------------------------------------------------------------

    @Test
    public void combinesRequiredItemCannonAndAntifire()
    {
        TaskData task = requiredItemTask(ItemID.ROCK_HAMMER);
        List<Integer> s = InventorySelector.select(
            fullCannon(ItemID.STEEL_CANNONBALL, ItemID.SUPER_ANTIFIRE_POTION4, ItemID.ROCK_HAMMER),
            DRAGON, CANNONABLE, task);
        assertTrue(s.contains(ItemID.ROCK_HAMMER));
        assertTrue(s.contains(ItemID.CANNON_BASE));
        assertTrue(s.contains(ItemID.STEEL_CANNONBALL));
        assertTrue(s.contains(ItemID.SUPER_ANTIFIRE_POTION4));
    }

    @Test
    public void nullOwnedYieldsEmpty()
    {
        assertTrue(InventorySelector.select(null, DRAGON, CANNONABLE, requiredItemTask(ItemID.ROCK_HAMMER))
            .isEmpty());
    }
}

package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import org.junit.Test;

public class BonusesTest
{
    @Test
    public void backwardCompatibleNineArgConstructorHasNoSlotAndIsNotTwoHanded()
    {
        Bonuses b = new Bonuses(1, 2, 3, 4, 5, 6, 7, 8, 4);

        assertNull(b.getSlot());
        assertFalse(b.isTwoHanded());
        assertEquals(1, b.getAstab());
        assertEquals(4, b.getAttackSpeedTicks());
    }

    @Test
    public void slotAndTwoHandedAreCarriedAndParticipateInValueEquality()
    {
        Bonuses a = new Bonuses(1, 2, 3, 4, 5, 6, 7, 8, 4, EquipmentSlot.WEAPON, true);
        Bonuses b = new Bonuses(1, 2, 3, 4, 5, 6, 7, 8, 4, EquipmentSlot.WEAPON, true);
        Bonuses differentSlot = new Bonuses(1, 2, 3, 4, 5, 6, 7, 8, 4, EquipmentSlot.SHIELD, true);

        assertEquals(EquipmentSlot.WEAPON, a.getSlot());
        assertTrue(a.isTwoHanded());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, differentSlot);
    }
}

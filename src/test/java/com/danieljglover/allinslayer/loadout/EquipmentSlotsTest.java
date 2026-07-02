package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import org.junit.Test;

public class EquipmentSlotsTest
{
    @Test
    public void mapsTheLoadoutBearingRuneLiteSlotIndexes()
    {
        assertEquals(EquipmentSlot.HEAD, EquipmentSlots.fromIndex(0));
        assertEquals(EquipmentSlot.CAPE, EquipmentSlots.fromIndex(1));
        assertEquals(EquipmentSlot.AMULET, EquipmentSlots.fromIndex(2));
        assertEquals(EquipmentSlot.WEAPON, EquipmentSlots.fromIndex(3));
        assertEquals(EquipmentSlot.BODY, EquipmentSlots.fromIndex(4));
        assertEquals(EquipmentSlot.SHIELD, EquipmentSlots.fromIndex(5));
        assertEquals(EquipmentSlot.LEGS, EquipmentSlots.fromIndex(7));
        assertEquals(EquipmentSlot.HANDS, EquipmentSlots.fromIndex(9));
        assertEquals(EquipmentSlot.FEET, EquipmentSlots.fromIndex(10));
        assertEquals(EquipmentSlot.RING, EquipmentSlots.fromIndex(12));
        assertEquals(EquipmentSlot.AMMO, EquipmentSlots.fromIndex(13));
    }

    @Test
    public void slotsWithNoCombatMeaningMapToNull()
    {
        assertNull("ARMS(6)", EquipmentSlots.fromIndex(6));
        assertNull("HAIR(8)", EquipmentSlots.fromIndex(8));
        assertNull("JAW(11)", EquipmentSlots.fromIndex(11));
    }

    @Test
    public void outOfRangeIndexesMapToNull()
    {
        assertNull(EquipmentSlots.fromIndex(-1));
        assertNull(EquipmentSlots.fromIndex(14));
        assertNull(EquipmentSlots.fromIndex(Integer.MIN_VALUE));
    }
}

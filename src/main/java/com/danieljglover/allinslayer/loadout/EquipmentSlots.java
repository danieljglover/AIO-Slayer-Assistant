package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.EquipmentSlot;

/**
 * Maps a RuneLite {@code EquipmentInventorySlot} index - the value returned by
 * {@code ItemEquipmentStats.getSlot()} (0-13) - to our {@link EquipmentSlot}.
 *
 * <p>The three slots with no combat-loadout meaning in our model - ARMS(6), HAIR(8), JAW(11) - map
 * to {@code null} so the gear selector ignores items in them (ADR-0002).
 */
public final class EquipmentSlots
{
    private static final EquipmentSlot[] BY_INDEX = new EquipmentSlot[14];

    static
    {
        BY_INDEX[0] = EquipmentSlot.HEAD;
        BY_INDEX[1] = EquipmentSlot.CAPE;
        BY_INDEX[2] = EquipmentSlot.AMULET;
        BY_INDEX[3] = EquipmentSlot.WEAPON;
        BY_INDEX[4] = EquipmentSlot.BODY;
        BY_INDEX[5] = EquipmentSlot.SHIELD;
        BY_INDEX[6] = null;            // ARMS - no slot in our model
        BY_INDEX[7] = EquipmentSlot.LEGS;
        BY_INDEX[8] = null;            // HAIR - no slot in our model
        BY_INDEX[9] = EquipmentSlot.HANDS;
        BY_INDEX[10] = EquipmentSlot.FEET;
        BY_INDEX[11] = null;           // JAW - no slot in our model
        BY_INDEX[12] = EquipmentSlot.RING;
        BY_INDEX[13] = EquipmentSlot.AMMO;
    }

    private EquipmentSlots()
    {
    }

    /**
     * @return our {@link EquipmentSlot} for the given RuneLite slot index, or {@code null} for an
     *     out-of-range index or a slot with no combat-loadout meaning (ARMS/HAIR/JAW).
     */
    public static EquipmentSlot fromIndex(int rlSlotIndex)
    {
        if (rlSlotIndex < 0 || rlSlotIndex >= BY_INDEX.length)
        {
            return null;
        }
        return BY_INDEX[rlSlotIndex];
    }
}

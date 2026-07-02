package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Immutable equipment-stat snapshot for one item (offensive + strength + speed bonuses), enriched
 * with the item's loadout {@link #slot} and the {@link #twoHanded} flag so the gear selector can
 * group by slot and resolve the weapon/shield interplay (ADR-0002). {@code slot} is {@code null}
 * for an item that maps to no loadout slot (ARMS/HAIR/JAW or unmapped).
 */
@Value
@AllArgsConstructor
public class Bonuses
{
    int astab;
    int aslash;
    int acrush;
    int amagic;
    int arange;
    int meleeStr;
    int rangedStr;
    int magicDmgPercent;
    int attackSpeedTicks;
    EquipmentSlot slot;
    boolean twoHanded;

    /**
     * Backward-compatible constructor for stat-only callers and tests that do not care about the
     * loadout slot: no slot, not two-handed.
     */
    public Bonuses(int astab, int aslash, int acrush, int amagic, int arange,
        int meleeStr, int rangedStr, int magicDmgPercent, int attackSpeedTicks)
    {
        this(astab, aslash, acrush, amagic, arange, meleeStr, rangedStr, magicDmgPercent,
            attackSpeedTicks, null, false);
    }

    public static Bonuses zero()
    {
        return new Bonuses(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}

package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * WD-10 (ADR-0020 #4): a resolved armour-set bonus - the loadout-level accuracy/damage multipliers a
 * complete owned set applies, plus the exact piece ids to wear (helm/top/robe/gloves for Void). The
 * multipliers are applied ONCE to the whole loadout's DPS (never per slot), so this is a single
 * loadout-level effect, not the per-slot additive term (no double-count, ADR-0008 section 3.5).
 */
public final class SetBonus
{
    private final Map<EquipmentSlot, Integer> pieces;
    private final double accMultiplier;
    private final double dmgMultiplier;

    SetBonus(Map<EquipmentSlot, Integer> pieces, double accMultiplier, double dmgMultiplier)
    {
        this.pieces = Collections.unmodifiableMap(new EnumMap<>(pieces));
        this.accMultiplier = accMultiplier;
        this.dmgMultiplier = dmgMultiplier;
    }

    /** The slot -> item id map of the set pieces to wear (a defensive, unmodifiable copy). */
    public Map<EquipmentSlot, Integer> getPieces()
    {
        return pieces;
    }

    /** The loadout-level accuracy multiplier (e.g. 1.10 for regular ranged/melee void). */
    public double getAccMultiplier()
    {
        return accMultiplier;
    }

    /** The loadout-level damage multiplier (e.g. 1.125 for elite ranged void). */
    public double getDmgMultiplier()
    {
        return dmgMultiplier;
    }
}

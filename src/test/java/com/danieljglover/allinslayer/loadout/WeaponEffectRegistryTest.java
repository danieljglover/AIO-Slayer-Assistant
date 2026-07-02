package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import net.runelite.api.ItemID;
import org.junit.Test;

/**
 * WDB-2 (ADR-0008 section 3.4a): passive DPS-formula special-weapon mechanics live in the all-static
 * {@link WeaponEffectRegistry}, keyed by raw item id (imbue/variant collapse is irrelevant here). Each
 * {@link WeaponEffect} reshapes the DPS formula: {@code accuracyRolls} (Fang rerolls accuracy twice)
 * and {@code damageMultiplier} (Scythe multiplies average damage 1.75x). Unknown ids = NONE = (1, 1.0)
 * which is today's maths exactly. A future passive weapon is one data row + one test row.
 */
public class WeaponEffectRegistryTest
{
    private static final double EPS = 1e-9;

    @Test
    public void fangRollsAccuracyTwice()
    {
        WeaponEffect fang = WeaponEffectRegistry.lookup(ItemID.OSMUMTENS_FANG);
        assertEquals("fang rolls accuracy twice", 2, fang.getAccuracyRolls());
        assertEquals("fang's 15-85% band keeps the mean at 0.5*max -> no damage multiplier",
            1.0, fang.getDamageMultiplier(), EPS);
    }

    @Test
    public void scytheMultipliesDamageBy175()
    {
        WeaponEffect scythe = WeaponEffectRegistry.lookup(ItemID.SCYTHE_OF_VITUR);
        assertEquals("scythe rolls accuracy once", 1, scythe.getAccuracyRolls());
        assertEquals("scythe hits 3x at 100/50/25% -> 1.75x average damage",
            1.75, scythe.getDamageMultiplier(), EPS);
    }

    @Test
    public void fangAndScytheCosmeticRecoloursShareTheEffect()
    {
        // Cosmetic / ornament variants are combat-identical and must carry the same effect.
        assertEquals(2, WeaponEffectRegistry.lookup(ItemID.OSMUMTENS_FANG_OR).getAccuracyRolls());
        assertEquals(1.75, WeaponEffectRegistry.lookup(ItemID.HOLY_SCYTHE_OF_VITUR)
            .getDamageMultiplier(), EPS);
        assertEquals(1.75, WeaponEffectRegistry.lookup(ItemID.SANGUINE_SCYTHE_OF_VITUR)
            .getDamageMultiplier(), EPS);
    }

    @Test
    public void unknownReturnsNone()
    {
        WeaponEffect none = WeaponEffectRegistry.lookup(4151); // abyssal whip - a normal weapon
        assertEquals(1, none.getAccuracyRolls());
        assertEquals(1.0, none.getDamageMultiplier(), EPS);
        assertSame("unknown ids share the singleton NONE", WeaponEffect.NONE, none);
        assertSame(WeaponEffect.NONE, WeaponEffectRegistry.lookup(0));
        assertSame(WeaponEffect.NONE, WeaponEffectRegistry.lookup(-1));
    }
}

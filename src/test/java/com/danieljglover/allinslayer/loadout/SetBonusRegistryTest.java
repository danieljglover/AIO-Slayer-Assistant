package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import net.runelite.api.ItemID;
import org.junit.Test;

/**
 * WD-10 (ADR-0020 #4): the Void / Elite void set-bonus seam. Ids pinned from {@code
 * net.runelite.api.ItemID} (the RuneTable-pinning discipline). Each style's complete set applies a
 * loadout-level accuracy/damage multiplier; the elite top+robe supply a distinct (higher) ranged and
 * magic damage bonus over the regular set. An incomplete set applies no bonus.
 */
public class SetBonusRegistryTest
{
    private static IntPredicate owns(int... ids)
    {
        Set<Integer> s = new HashSet<>();
        for (int id : ids)
        {
            s.add(id);
        }
        return s::contains;
    }

    private static final int[] MELEE_SET =
        {ItemID.VOID_MELEE_HELM, ItemID.VOID_KNIGHT_TOP, ItemID.VOID_KNIGHT_ROBE, ItemID.VOID_KNIGHT_GLOVES};
    private static final int[] RANGE_SET =
        {ItemID.VOID_RANGER_HELM, ItemID.VOID_KNIGHT_TOP, ItemID.VOID_KNIGHT_ROBE, ItemID.VOID_KNIGHT_GLOVES};
    private static final int[] MAGE_SET =
        {ItemID.VOID_MAGE_HELM, ItemID.VOID_KNIGHT_TOP, ItemID.VOID_KNIGHT_ROBE, ItemID.VOID_KNIGHT_GLOVES};

    @Test
    public void completeMeleeVoidSetGivesTenPercentAccuracyAndDamage()
    {
        SetBonus b = SetBonusRegistry.voidBonus(CombatStyle.MELEE, owns(MELEE_SET));
        assertEquals(1.10, b.getAccMultiplier(), 1e-9);
        assertEquals(1.10, b.getDmgMultiplier(), 1e-9);
        assertEquals("void melee helm goes on the head", Integer.valueOf(ItemID.VOID_MELEE_HELM),
            b.getPieces().get(EquipmentSlot.HEAD));
        assertEquals(Integer.valueOf(ItemID.VOID_KNIGHT_TOP), b.getPieces().get(EquipmentSlot.BODY));
        assertEquals(Integer.valueOf(ItemID.VOID_KNIGHT_ROBE), b.getPieces().get(EquipmentSlot.LEGS));
        assertEquals(Integer.valueOf(ItemID.VOID_KNIGHT_GLOVES), b.getPieces().get(EquipmentSlot.HANDS));
    }

    @Test
    public void completeRangedVoidSetGivesTenPercentAccuracyAndDamage()
    {
        SetBonus b = SetBonusRegistry.voidBonus(CombatStyle.RANGED, owns(RANGE_SET));
        assertEquals(1.10, b.getAccMultiplier(), 1e-9);
        assertEquals(1.10, b.getDmgMultiplier(), 1e-9);
        assertEquals(Integer.valueOf(ItemID.VOID_RANGER_HELM), b.getPieces().get(EquipmentSlot.HEAD));
    }

    @Test
    public void completeMagicVoidSetGivesFortyFivePercentAccuracyNoDamage()
    {
        SetBonus b = SetBonusRegistry.voidBonus(CombatStyle.MAGIC, owns(MAGE_SET));
        assertEquals(1.45, b.getAccMultiplier(), 1e-9);
        assertEquals(1.00, b.getDmgMultiplier(), 1e-9);
        assertEquals(Integer.valueOf(ItemID.VOID_MAGE_HELM), b.getPieces().get(EquipmentSlot.HEAD));
    }

    @Test
    public void eliteVoidRangedDamageIsDistinctlyHigherThanRegular()
    {
        SetBonus elite = SetBonusRegistry.voidBonus(CombatStyle.RANGED,
            owns(ItemID.VOID_RANGER_HELM, ItemID.ELITE_VOID_TOP, ItemID.ELITE_VOID_ROBE,
                ItemID.VOID_KNIGHT_GLOVES));
        assertEquals("elite ranged damage is +12.5%", 1.125, elite.getDmgMultiplier(), 1e-9);
        assertEquals("accuracy is unchanged by elite", 1.10, elite.getAccMultiplier(), 1e-9);
        assertTrue("elite ranged damage exceeds the regular set",
            elite.getDmgMultiplier() > SetBonusRegistry
                .voidBonus(CombatStyle.RANGED, owns(RANGE_SET)).getDmgMultiplier());
        // The worn top/robe are the elite pieces.
        assertEquals(Integer.valueOf(ItemID.ELITE_VOID_TOP), elite.getPieces().get(EquipmentSlot.BODY));
        assertEquals(Integer.valueOf(ItemID.ELITE_VOID_ROBE), elite.getPieces().get(EquipmentSlot.LEGS));
    }

    @Test
    public void eliteVoidMagicDamageIsDistinctlyHigherThanRegular()
    {
        SetBonus elite = SetBonusRegistry.voidBonus(CombatStyle.MAGIC,
            owns(ItemID.VOID_MAGE_HELM, ItemID.ELITE_VOID_TOP, ItemID.ELITE_VOID_ROBE,
                ItemID.VOID_KNIGHT_GLOVES));
        assertEquals("elite magic damage is +2.5%", 1.025, elite.getDmgMultiplier(), 1e-9);
        assertEquals(1.45, elite.getAccMultiplier(), 1e-9);
        assertTrue("elite magic damage exceeds the regular set",
            elite.getDmgMultiplier() > SetBonusRegistry
                .voidBonus(CombatStyle.MAGIC, owns(MAGE_SET)).getDmgMultiplier());
    }

    @Test
    public void eliteTopDoesNotBoostMeleeBeyondRegular()
    {
        // Elite void carries no melee bonus - a melee set with elite pieces stays at +10%/+10%.
        SetBonus b = SetBonusRegistry.voidBonus(CombatStyle.MELEE,
            owns(ItemID.VOID_MELEE_HELM, ItemID.ELITE_VOID_TOP, ItemID.ELITE_VOID_ROBE,
                ItemID.VOID_KNIGHT_GLOVES));
        assertEquals(1.10, b.getDmgMultiplier(), 1e-9);
        assertEquals(1.10, b.getAccMultiplier(), 1e-9);
    }

    @Test
    public void incompleteSetGivesNoBonus()
    {
        // Missing the gloves -> no set -> null (3/4 owned).
        assertNull(SetBonusRegistry.voidBonus(CombatStyle.RANGED,
            owns(ItemID.VOID_RANGER_HELM, ItemID.VOID_KNIGHT_TOP, ItemID.VOID_KNIGHT_ROBE)));
        // Wrong-style helm (melee helm on a ranged query) -> no ranged set.
        assertNull(SetBonusRegistry.voidBonus(CombatStyle.RANGED,
            owns(ItemID.VOID_MELEE_HELM, ItemID.VOID_KNIGHT_TOP, ItemID.VOID_KNIGHT_ROBE,
                ItemID.VOID_KNIGHT_GLOVES)));
    }

    @Test
    public void mixedEliteTopWithRegularRobeGivesOnlyTheRegularBonus()
    {
        // Elite bonus requires BOTH elite top and elite robe; one elite piece -> regular set bonus.
        SetBonus b = SetBonusRegistry.voidBonus(CombatStyle.RANGED,
            owns(ItemID.VOID_RANGER_HELM, ItemID.ELITE_VOID_TOP, ItemID.VOID_KNIGHT_ROBE,
                ItemID.VOID_KNIGHT_GLOVES));
        assertEquals("one elite piece -> regular +10% damage", 1.10, b.getDmgMultiplier(), 1e-9);
    }
}

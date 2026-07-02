package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.loadout.RuneTable.Tier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * LD05: locks the curated element -> tier -> {runes, level, base max hit} numbers from research C.4
 * plus the powered/elemental-staff sets and the karambwan id, so a later data change is deliberate.
 */
public class RuneTableTest
{
    @Test
    public void tiersAreOrderedStrongestToWeakest()
    {
        assertEquals(Arrays.asList(Tier.SURGE, Tier.WAVE, Tier.BLAST, Tier.BOLT),
            RuneTable.tiersDescending());
    }

    @Test
    public void fireSurgeMatchesTheCuratedRequirementLevelAndMaxHit()
    {
        Map<Integer, Integer> expected = new HashMap<>();
        expected.put(RuneTable.AIR_RUNE, 7);
        expected.put(RuneTable.FIRE_RUNE, 10);
        expected.put(RuneTable.WRATH_RUNE, 1);

        assertEquals(expected, RuneTable.requirement("fire", Tier.SURGE));
        assertEquals(95, RuneTable.level("fire", Tier.SURGE));
        assertEquals(24, RuneTable.baseMaxHit("fire", Tier.SURGE));
        assertEquals("Fire Surge", RuneTable.spellName("fire", Tier.SURGE));
    }

    @Test
    public void airSpellsUseOnlyAirAndTheCombatRune()
    {
        Map<Integer, Integer> windBolt = new HashMap<>();
        windBolt.put(RuneTable.AIR_RUNE, 2);
        windBolt.put(RuneTable.CHAOS_RUNE, 1);

        assertEquals(windBolt, RuneTable.requirement("air", Tier.BOLT));
        assertEquals(17, RuneTable.level("air", Tier.BOLT));
        assertEquals(9, RuneTable.baseMaxHit("air", Tier.BOLT));
        assertEquals("Wind Bolt", RuneTable.spellName("air", Tier.BOLT));
    }

    @Test
    public void waterBlastCarriesAirWaterAndDeathRunes()
    {
        Map<Integer, Integer> waterBlast = new HashMap<>();
        waterBlast.put(RuneTable.AIR_RUNE, 3);
        waterBlast.put(RuneTable.WATER_RUNE, 3);
        waterBlast.put(RuneTable.DEATH_RUNE, 1);

        assertEquals(waterBlast, RuneTable.requirement("water", Tier.BLAST));
        assertEquals(47, RuneTable.level("water", Tier.BLAST));
        assertEquals("Water Blast", RuneTable.spellName("water", Tier.BLAST));
    }

    @Test
    public void eachTierUsesItsOwnCombatRune()
    {
        assertTrue(RuneTable.requirement("earth", Tier.BOLT).containsKey(RuneTable.CHAOS_RUNE));
        assertTrue(RuneTable.requirement("earth", Tier.BLAST).containsKey(RuneTable.DEATH_RUNE));
        assertTrue(RuneTable.requirement("earth", Tier.WAVE).containsKey(RuneTable.BLOOD_RUNE));
        assertTrue(RuneTable.requirement("earth", Tier.SURGE).containsKey(RuneTable.WRATH_RUNE));
    }

    @Test
    public void unknownElementOrTierIsEmpty()
    {
        assertTrue(RuneTable.requirement("smoke", Tier.SURGE).isEmpty());
        assertTrue(RuneTable.requirement(null, Tier.SURGE).isEmpty());
        assertEquals(0, RuneTable.level("smoke", Tier.SURGE));
        assertNull(RuneTable.spellName("smoke", Tier.SURGE));
    }

    @Test
    public void poweredStavesAreFlaggedWithABaseMaxHitAndNeedNoRunes()
    {
        assertTrue(RuneTable.isPoweredStaff(11905)); // Trident of the seas
        assertEquals(Integer.valueOf(26), RuneTable.poweredStaffMaxHit(22323)); // Sanguinesti staff
        assertFalse(RuneTable.isPoweredStaff(1387)); // Staff of fire is an elemental staff, not powered
        assertNull(RuneTable.poweredStaffMaxHit(1387));
    }

    @Test
    public void elementalAndCombinationStavesReportTheElementsTheySupply()
    {
        assertTrue(RuneTable.elementsSuppliedBy(1387).contains("fire")); // Staff of fire
        assertTrue(RuneTable.elementsSuppliedBy(1393).contains("fire")); // Fire battlestaff
        // Lava battlestaff supplies earth AND fire.
        assertTrue(RuneTable.elementsSuppliedBy(3053).contains("earth"));
        assertTrue(RuneTable.elementsSuppliedBy(3053).contains("fire"));
        assertEquals(2, RuneTable.elementsSuppliedBy(3053).size());
        assertTrue(RuneTable.elementsSuppliedBy(4151).isEmpty()); // Abyssal whip supplies nothing
    }

    @Test
    public void karambwanIdIsCurated()
    {
        assertEquals(3144, RuneTable.KARAMBWAN_ID);
    }
}

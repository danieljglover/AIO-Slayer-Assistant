package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.bank.OwnedItems;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * WDB-13 (ADR-0009 B.2): the shared {@link MagicWeaponEvaluator} resolves "given this weapon, the best
 * castable spell or powered-staff base max hit". The full rune/tier/affordability behaviour is
 * characterised by {@link ConsumableSelectorTest} (which now delegates here); these tests pin the
 * evaluator's own contract incl. the cast-speed helper.
 */
public class MagicWeaponEvaluatorTest
{
    private static final int TRIDENT_SEAS = 11905;
    private static final PlayerStats MAGIC_99 = new PlayerStats(99, 99, 99, 99, 99, 99);

    @Test
    public void evaluatorReturnsPoweredStaffMaxHitAndSpeed()
    {
        MagicSetup m = MagicWeaponEvaluator.evaluate(owned(), "fire", MAGIC_99, TRIDENT_SEAS);
        assertTrue("a powered staff supplies its own attack", m.isPoweredStaff());
        assertEquals("Trident of the seas base max hit", 23, m.getSpellBaseMaxHit());
        assertTrue("no runes needed", m.getRuneRequirement().isEmpty());

        // A powered staff casts at its own attack speed (e.g. 4t trident); a standard caster at 5t.
        assertEquals(4, MagicWeaponEvaluator.castSpeedTicks(true, 4));
        assertEquals("powered staff with unknown speed falls back to 5t",
            5, MagicWeaponEvaluator.castSpeedTicks(true, 0));
        assertEquals("standard caster always casts at 5t, not the wand's melee speed",
            5, MagicWeaponEvaluator.castSpeedTicks(false, 4));
    }

    @Test
    public void evaluatorPicksBestAffordableTierForStandardCaster()
    {
        OwnedItems owned = own(
            RuneTable.AIR_RUNE, 100, RuneTable.FIRE_RUNE, 100, RuneTable.WRATH_RUNE, 100);
        MagicSetup m = MagicWeaponEvaluator.evaluate(owned, "fire", MAGIC_99, null);
        assertFalse("a standard caster is not a powered staff", m.isPoweredStaff());
        assertEquals("Fire Surge", m.getSpellName());
        assertEquals(24, m.getSpellBaseMaxHit());
        assertTrue(m.getRunesShort().isEmpty());
    }

    @Test
    public void tumekensShadowTriplesGearAndCapsDamageAtOneHundred()
    {
        // WDB-15 (ADR-0009 B.4): the Shadow triples the GEAR magic attack/damage; the tripled gear
        // damage is capped at +100%. A normal weapon adds gear once (uncapped here).
        int shadow = 27275;
        int wand = 7001;

        assertEquals("gear matt tripled for the Shadow", 35 + 3 * 50,
            MagicWeaponEvaluator.effectiveMagicAttack(shadow, 35, 50));
        assertEquals("normal weapon adds gear matt once", 60 + 50,
            MagicWeaponEvaluator.effectiveMagicAttack(wand, 60, 50));

        assertEquals("tripled gear damage 3*40=120 capped at +100", 0 + 100,
            MagicWeaponEvaluator.effectiveMagicDamage(shadow, 0, 40));
        assertEquals("tripled gear damage 3*30=90 under the cap", 0 + 90,
            MagicWeaponEvaluator.effectiveMagicDamage(shadow, 0, 30));
        assertEquals("normal weapon: no cap, gear added once", 60 + 50,
            MagicWeaponEvaluator.effectiveMagicDamage(wand, 60, 50));

        assertTrue(MagicWeaponEvaluator.isTumekensShadow(27275));
        assertFalse(MagicWeaponEvaluator.isTumekensShadow(7001));
    }

    // --- WD-9: Ancient Barrage/Burst on multi-combat tasks (ADR-0020 #4) ----------------------

    @Test
    public void multicombatPicksStrongestAffordableAncientBarrage()
    {
        // Level 99, holding the Ice Barrage runes -> the strongest affordable multi spell is picked
        // over the standard-book spell. Not a powered staff; no runes short.
        OwnedItems owned = own(
            RuneTable.WATER_RUNE, 1000, RuneTable.BLOOD_RUNE, 1000, RuneTable.DEATH_RUNE, 1000);
        MagicSetup m = MagicWeaponEvaluator.evaluate(owned, "fire", MAGIC_99, null, true);
        assertEquals("Ice Barrage", m.getSpellName());
        assertEquals(30, m.getSpellBaseMaxHit());
        assertFalse(m.isPoweredStaff());
        assertTrue(m.getRunesShort().isEmpty());
    }

    @Test
    public void multicombatFallsToBurstBelowBarrageLevel()
    {
        // Magic 70: barrages (level 86+) are gated out; Ice Burst (level 70) is the best affordable multi.
        PlayerStats magic70 = new PlayerStats(99, 99, 99, 99, 70, 99);
        OwnedItems owned = own(
            RuneTable.WATER_RUNE, 1000, RuneTable.CHAOS_RUNE, 1000, RuneTable.DEATH_RUNE, 1000);
        MagicSetup m = MagicWeaponEvaluator.evaluate(owned, "fire", magic70, null, true);
        assertEquals("Ice Burst", m.getSpellName());
        assertEquals(22, m.getSpellBaseMaxHit());
    }

    @Test
    public void multicombatFallsBackToStandardWhenNoAncientAffordable()
    {
        // Multi-combat but only standard-book runes owned -> no ancient is affordable -> standard book.
        OwnedItems owned = own(
            RuneTable.AIR_RUNE, 1000, RuneTable.FIRE_RUNE, 1000, RuneTable.WRATH_RUNE, 1000);
        MagicSetup m = MagicWeaponEvaluator.evaluate(owned, "fire", MAGIC_99, null, true);
        assertEquals("Fire Surge", m.getSpellName());
        assertEquals(24, m.getSpellBaseMaxHit());
    }

    @Test
    public void singleTargetIgnoresAncientAndDefaultOverloadIsSingleTarget()
    {
        // Even holding Ancient runes, a single-target task casts the standard book (FR-6 unchanged), and
        // the 4-arg overload is byte-identically the single-target path.
        OwnedItems owned = own(
            RuneTable.WATER_RUNE, 1000, RuneTable.BLOOD_RUNE, 1000, RuneTable.DEATH_RUNE, 1000,
            RuneTable.AIR_RUNE, 1000, RuneTable.FIRE_RUNE, 1000, RuneTable.WRATH_RUNE, 1000);
        MagicSetup single = MagicWeaponEvaluator.evaluate(owned, "fire", MAGIC_99, null, false);
        assertEquals("Fire Surge", single.getSpellName());
        assertEquals(MagicWeaponEvaluator.evaluate(owned, "fire", MAGIC_99, null).getSpellName(),
            single.getSpellName());
    }

    private static OwnedItems owned()
    {
        return OwnedItems.fromCounts(new HashMap<>());
    }

    private static OwnedItems own(int... idQtyPairs)
    {
        Map<Integer, Integer> m = new HashMap<>();
        for (int i = 0; i < idQtyPairs.length; i += 2)
        {
            m.put(idQtyPairs[i], idQtyPairs[i + 1]);
        }
        return OwnedItems.fromCounts(m);
    }
}

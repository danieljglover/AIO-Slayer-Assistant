package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.model.CombatStyle;
import org.junit.Test;

/**
 * LFB-2: the conditional-bonus predicate ({@link BonusCondition}) reads a {@link BonusContext} built
 * from the task, and {@link ConditionalBonus} exposes a per-style multiplier (ADR-0007). These are
 * pure value types - no selector or registry involved.
 */
public class ConditionalBonusTest
{
    private static final double EPS = 1e-9;

    @Test
    public void onSlayerTaskPredicateReadsTheSlayerHelmAppliesFlag()
    {
        assertTrue(BonusCondition.ON_SLAYER_TASK.test(
            BonusContext.builder().slayerHelmApplies(true).build()));
        assertFalse(BonusCondition.ON_SLAYER_TASK.test(BonusContext.builder().build()));
        // It is keyed to the slayer-helm flag, not undead.
        assertFalse(BonusCondition.ON_SLAYER_TASK.test(BonusContext.builder().undead(true).build()));
    }

    @Test
    public void vsUndeadPredicateReadsTheUndeadFlag()
    {
        assertTrue(BonusCondition.VS_UNDEAD.test(BonusContext.builder().undead(true).build()));
        assertFalse(BonusCondition.VS_UNDEAD.test(BonusContext.builder().build()));
        // It is keyed to undead, not the slayer-helm flag.
        assertFalse(BonusCondition.VS_UNDEAD.test(
            BonusContext.builder().slayerHelmApplies(true).build()));
    }

    @Test
    public void vsDragonPredicateReadsTheDragonFlag()
    {
        assertTrue(BonusCondition.VS_DRAGON.test(BonusContext.builder().dragon(true).build()));
        assertFalse(BonusCondition.VS_DRAGON.test(BonusContext.builder().build()));
        assertFalse(BonusCondition.VS_DRAGON.test(BonusContext.builder().undead(true).build()));
    }

    @Test
    public void vsDemonPredicateReadsTheDemonFlag()
    {
        assertTrue(BonusCondition.VS_DEMON.test(BonusContext.builder().demon(true).build()));
        assertFalse(BonusCondition.VS_DEMON.test(BonusContext.builder().build()));
        assertFalse(BonusCondition.VS_DEMON.test(BonusContext.builder().dragon(true).build()));
    }

    @Test
    public void vsKalphitePredicateReadsTheKalphiteFlag()
    {
        assertTrue(BonusCondition.VS_KALPHITE.test(BonusContext.builder().kalphite(true).build()));
        assertFalse(BonusCondition.VS_KALPHITE.test(BonusContext.builder().build()));
        assertFalse(BonusCondition.VS_KALPHITE.test(BonusContext.builder().demon(true).build()));
    }

    @Test
    public void vsWildernessPredicateReadsTheWildernessFlag()
    {
        assertTrue(BonusCondition.VS_WILDERNESS.test(BonusContext.builder().wilderness(true).build()));
        assertFalse(BonusCondition.VS_WILDERNESS.test(BonusContext.builder().build()));
        // Wilderness is a location predicate, independent of the task-category flags.
        assertFalse(BonusCondition.VS_WILDERNESS.test(BonusContext.builder().demon(true).build()));
    }

    @Test
    public void symmetricMultiplierReturnsTheConfiguredValuePerStyle()
    {
        ConditionalBonus imbued = ConditionalBonus.symmetric(BonusCondition.ON_SLAYER_TASK, 1.1667, 1.15, 1.15);
        assertEquals(1.1667, imbued.dmgMultiplier(CombatStyle.MELEE), EPS);
        assertEquals(1.1667, imbued.accMultiplier(CombatStyle.MELEE), EPS);
        assertEquals(1.15, imbued.dmgMultiplier(CombatStyle.RANGED), EPS);
        assertEquals(1.15, imbued.accMultiplier(CombatStyle.MAGIC), EPS);
        assertTrue("symmetric source has acc == dmg", imbued.isSymmetric());
    }

    @Test
    public void multiplierIsOnePointZeroForAnUnsetStyle()
    {
        // A melee-only bonus (black mask / salve base): ranged and magic must read as no-op 1.0.
        ConditionalBonus meleeOnly = ConditionalBonus.symmetric(BonusCondition.VS_UNDEAD, 1.1667, 1.0, 1.0);
        assertEquals(1.1667, meleeOnly.dmgMultiplier(CombatStyle.MELEE), EPS);
        assertEquals(1.0, meleeOnly.dmgMultiplier(CombatStyle.RANGED), EPS);
        assertEquals(1.0, meleeOnly.accMultiplier(CombatStyle.MAGIC), EPS);
    }

    @Test
    public void asymmetricBonusAppliesSeparateAccuracyAndDamageMultipliers()
    {
        // WDB-11 / FR-14.6: Keris-shaped - damage-only melee (acc 1.0, dmg 1.382), no ranged/magic.
        ConditionalBonus keris = ConditionalBonus.asymmetric(BonusCondition.VS_UNDEAD,
            1.0, 1.382, 1.0, 1.0, 1.0, 1.0);
        assertEquals("no accuracy bonus", 1.0, keris.accMultiplier(CombatStyle.MELEE), EPS);
        assertEquals("damage-only +38.2%", 1.382, keris.dmgMultiplier(CombatStyle.MELEE), EPS);
        assertFalse("acc != dmg -> asymmetric", keris.isSymmetric());
    }
}

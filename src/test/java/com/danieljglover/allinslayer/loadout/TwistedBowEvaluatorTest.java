package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * WD-11 (ADR-0020 §5, PD-D3): the Twisted bow per-target Magic-level scaling evaluator. The bow's
 * accuracy/damage multipliers rise with the monster's Magic level, capped at the OSRS ceilings, and
 * are undefined (null -> base-stats ranking, FR-6) when the weapon is not the Twisted bow or the
 * monster's Magic level is unknown.
 */
public class TwistedBowEvaluatorTest
{
    private static final int TBOW = net.runelite.api.ItemID.TWISTED_BOW;
    private static final int OTHER_BOW = 12926; // a non-Twisted-bow ranged weapon id

    @Test
    public void nonTwistedBowWeaponHasNoScaling()
    {
        assertNull("only the Twisted bow scales", TwistedBowEvaluator.scaling(OTHER_BOW, 200));
    }

    @Test
    public void unknownMagicLevelYieldsNoScaling()
    {
        // Honest UNKNOWN (ADR-0019): no magic-level datum -> rank the Twisted bow on its base stats,
        // never a fabricated multiplier (FR-6 byte-identical).
        assertNull("null magic level -> no scaling", TwistedBowEvaluator.scaling(TBOW, null));
    }

    @Test
    public void scalingRisesWithMagicLevel()
    {
        double[] low = TwistedBowEvaluator.scaling(TBOW, 100);
        double[] high = TwistedBowEvaluator.scaling(TBOW, 250);
        assertTrue("accuracy multiplier rises with magic level", high[0] > low[0]);
        assertTrue("damage multiplier rises with magic level", high[1] > low[1]);
    }

    @Test
    public void multipliersAreCappedAtTheCeilings()
    {
        // Magic is capped at 250 first (a 500-magic monster scales as a 250 one), then the accuracy and
        // damage percentages are capped at +140% / +250% (the OSRS Twisted bow ceilings).
        double[] capped = TwistedBowEvaluator.scaling(TBOW, 500);
        double[] atCap = TwistedBowEvaluator.scaling(TBOW, 250);
        assertEquals("magic beyond 250 scales identically to 250", atCap[0], capped[0], 1e-9);
        assertEquals("magic beyond 250 scales identically to 250", atCap[1], capped[1], 1e-9);
        assertTrue("accuracy multiplier never exceeds +140%", capped[0] <= 1.40 + 1e-9);
        assertTrue("damage multiplier never exceeds +250%", capped[1] <= 2.50 + 1e-9);
    }

    @Test
    public void lowMagicMonsterMakesTheTwistedBowWeak()
    {
        // The Twisted bow is deliberately poor against low-magic targets: at magic 1 both multipliers
        // fall well below 1.0, so it will rank below an ordinary bow (the real-game trade-off).
        double[] weak = TwistedBowEvaluator.scaling(TBOW, 1);
        assertTrue("accuracy multiplier below 1.0 vs a low-magic target", weak[0] < 1.0);
        assertTrue("damage multiplier below 1.0 vs a low-magic target", weak[1] < 1.0);
    }
}

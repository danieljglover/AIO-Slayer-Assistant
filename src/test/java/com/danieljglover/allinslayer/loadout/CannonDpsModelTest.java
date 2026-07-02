package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * WD-6 (ADR-0020 #3): the honest single-target Dwarf multicannon DPS term. A deterministic constant
 * independent of the player's gear - a static max hit of 30 (steel cannonballs, wiki-verified),
 * mean damage 15 over the uniform 0-30 distribution, one shot every 4 game ticks (2.4s).
 */
public class CannonDpsModelTest
{
    @Test
    public void singleTargetDpsIsMeanDamageOverShotInterval()
    {
        // 15 mean damage / 2.4s = 6.25 DPS. Independent of monster HP and player gear (ADR-0020 #3).
        assertEquals(15.0 / 2.4, CannonDpsModel.singleTargetDps(), 1e-9);
        assertEquals(6.25, CannonDpsModel.singleTargetDps(), 1e-9);
    }
}

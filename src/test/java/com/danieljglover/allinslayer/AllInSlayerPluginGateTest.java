package com.danieljglover.allinslayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit-tests the pure bank-gate decision (plan §6 / §8). The surrounding {@code recompute} wiring is
 * field-injected and client-thread bound, so it is exercised via the manual checklist (plan §9); this
 * locks the one piece of gate logic that is cheaply testable in isolation.
 */
public class AllInSlayerPluginGateTest
{
    @Test
    public void bankGateOnlyWhenTaskPresentAndBankNeverSeen()
    {
        assertTrue("task present + no bank snapshot -> gate",
            AllInSlayerPlugin.isBankGate(true, null));
        assertFalse("task present + bank already seen -> no gate",
            AllInSlayerPlugin.isBankGate(true, 123L));
        assertFalse("no task -> no gate even without a bank snapshot",
            AllInSlayerPlugin.isBankGate(false, null));
        assertFalse("no task + bank seen -> no gate",
            AllInSlayerPlugin.isBankGate(false, 123L));
    }
}

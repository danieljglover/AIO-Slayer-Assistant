package com.danieljglover.allinslayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

/**
 * Unit-tests the pure bank-snapshot age tiering + staleness decision (DT-FE4 / G4, PD-5). The
 * surrounding {@code recompute} wiring is field-injected and client-thread bound, so it is exercised
 * via the manual checklist (plan §9); these lock the two pieces of bank-age logic that are cheaply
 * testable in isolation, mirroring {@link AllInSlayerPluginGateTest}.
 */
public class AllInSlayerPluginBankAgeTest
{
    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    private static Long ago(Duration d)
    {
        return NOW.minus(d).toEpochMilli();
    }

    @Test
    public void bankAgeTextTiersFromJustNowThroughDays()
    {
        assertNull("never seen -> no age text (the BANK_NOT_SCANNED gate handles that)",
            AllInSlayerPlugin.bankAgeText(null, NOW));
        assertEquals("just now", AllInSlayerPlugin.bankAgeText(ago(Duration.ofSeconds(30)), NOW));
        assertEquals("5m ago", AllInSlayerPlugin.bankAgeText(ago(Duration.ofMinutes(5)), NOW));
        assertEquals("under an hour still reads in minutes", "59m ago",
            AllInSlayerPlugin.bankAgeText(ago(Duration.ofMinutes(59)), NOW));
        assertEquals("1h ago", AllInSlayerPlugin.bankAgeText(ago(Duration.ofMinutes(90)), NOW));
        assertEquals("under a day still reads in hours", "23h ago",
            AllInSlayerPlugin.bankAgeText(ago(Duration.ofHours(23)), NOW));
        assertEquals("past 24h flips to a compact days tier", "1d ago",
            AllInSlayerPlugin.bankAgeText(ago(Duration.ofHours(25)), NOW));
        assertEquals("8d ago", AllInSlayerPlugin.bankAgeText(ago(Duration.ofDays(8)), NOW));
    }

    @Test
    public void bankStaleOnlyPastTheSevenDayThreshold()
    {
        assertFalse("never seen is not stale (that is the gate's job, not this flag)",
            AllInSlayerPlugin.bankStale(null, NOW));
        assertFalse("fresh snapshot is not stale",
            AllInSlayerPlugin.bankStale(ago(Duration.ofHours(2)), NOW));
        assertFalse("six days is under the threshold",
            AllInSlayerPlugin.bankStale(ago(Duration.ofDays(6)), NOW));
        assertTrue("seven days hits the threshold (PD-5)",
            AllInSlayerPlugin.bankStale(ago(Duration.ofDays(7)), NOW));
        assertTrue("a month-old snapshot is stale",
            AllInSlayerPlugin.bankStale(ago(Duration.ofDays(30)), NOW));
    }
}

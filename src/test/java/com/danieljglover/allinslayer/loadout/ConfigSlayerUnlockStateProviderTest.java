package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AllInSlayerConfig;
import com.danieljglover.allinslayer.PrimaryMaster;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * WA-9 (ADR-0018 #7): the config-backed {@link SlayerUnlockStateProvider} - the seam's fallback
 * implementation (and the {@code @ImplementedBy} default until WA-10 binds the varbit reader).
 * The player self-declares: a point balance, per-unlock owned toggles for the 7 global rewards,
 * and a primary master. Safe defaults: 0 points, nothing owned, Duradel (the dataset is
 * Duradel-shaped).
 */
public class ConfigSlayerUnlockStateProviderTest
{
    /** An all-defaults config: every value is the interface default. */
    private static class DefaultsConfig implements AllInSlayerConfig
    {
    }

    private final SlayerUnlockStateProvider defaults =
        new ConfigSlayerUnlockStateProvider(new DefaultsConfig());

    @Test
    public void defaultsAreSafe()
    {
        assertEquals("0 points by default", 0, defaults.rewardPoints().getAsInt());
        for (String id : new String[] {"malevolent-masquerade", "broader-fletching", "ring-bling",
            "like-a-boss", "task-storage", "i-wildy-more-slayer", "bigger-and-badder"})
        {
            assertFalse(id + " unowned by default", defaults.ownsUnlock(id));
        }
        assertEquals("primary master defaults to duradel (the dataset shape)",
            "duradel", defaults.currentMaster().get());
    }

    @Test
    public void rewardPointsReturnsTheConfiguredBalanceClampedAtZero()
    {
        SlayerUnlockStateProvider p = new ConfigSlayerUnlockStateProvider(new DefaultsConfig()
        {
            @Override
            public int slayerRewardPoints()
            {
                return 734;
            }
        });
        assertEquals(734, p.rewardPoints().getAsInt());

        SlayerUnlockStateProvider negative = new ConfigSlayerUnlockStateProvider(new DefaultsConfig()
        {
            @Override
            public int slayerRewardPoints()
            {
                return -5;
            }
        });
        assertEquals("a mistyped negative balance clamps to 0", 0,
            negative.rewardPoints().getAsInt());
    }

    @Test
    public void eachOwnedToggleMapsToItsRewardId()
    {
        // One toggle on at a time -> exactly that rewardId reads owned (catches a crossed wire).
        assertToggleMaps("malevolent-masquerade", new DefaultsConfig()
        {
            @Override
            public boolean ownsMalevolentMasquerade()
            {
                return true;
            }
        });
        assertToggleMaps("broader-fletching", new DefaultsConfig()
        {
            @Override
            public boolean ownsBroaderFletching()
            {
                return true;
            }
        });
        assertToggleMaps("ring-bling", new DefaultsConfig()
        {
            @Override
            public boolean ownsRingBling()
            {
                return true;
            }
        });
        assertToggleMaps("like-a-boss", new DefaultsConfig()
        {
            @Override
            public boolean ownsLikeABoss()
            {
                return true;
            }
        });
        assertToggleMaps("task-storage", new DefaultsConfig()
        {
            @Override
            public boolean ownsTaskStorage()
            {
                return true;
            }
        });
        assertToggleMaps("i-wildy-more-slayer", new DefaultsConfig()
        {
            @Override
            public boolean ownsWildyMoreSlayer()
            {
                return true;
            }
        });
        assertToggleMaps("bigger-and-badder", new DefaultsConfig()
        {
            @Override
            public boolean ownsBiggerAndBadder()
            {
                return true;
            }
        });
    }

    private static void assertToggleMaps(String expectedOwned, AllInSlayerConfig config)
    {
        SlayerUnlockStateProvider p = new ConfigSlayerUnlockStateProvider(config);
        for (String id : new String[] {"malevolent-masquerade", "broader-fletching", "ring-bling",
            "like-a-boss", "task-storage", "i-wildy-more-slayer", "bigger-and-badder"})
        {
            assertEquals(id, expectedOwned.equals(id), p.ownsUnlock(id));
        }
    }

    @Test
    public void unknownAndCosmeticIdsReadUnowned()
    {
        // The config UI carries no toggle for the 10 helm recolours (cosmetic, no gear guard) or
        // per-task unlock ids - the config impl honestly reports them unowned; the varbit impl
        // (WA-10) is the path that can answer those.
        assertFalse(defaults.ownsUnlock("king-black-bonnet"));
        assertFalse(defaults.ownsUnlock("augment-my-abbies"));
        assertFalse(defaults.ownsUnlock("no-such-id"));
        assertFalse(defaults.ownsUnlock(null));
    }

    @Test
    public void currentMasterFollowsThePrimaryMasterConfig()
    {
        SlayerUnlockStateProvider p = new ConfigSlayerUnlockStateProvider(new DefaultsConfig()
        {
            @Override
            public PrimaryMaster primaryMaster()
            {
                return PrimaryMaster.KRYSTILIA;
            }
        });
        assertEquals("krystilia", p.currentMaster().get());
    }

    @Test
    public void primaryMasterEnumCoversAllNineMastersWithTheirDataIds()
    {
        assertEquals("exactly the 9 masters", 9, PrimaryMaster.values().length);
        assertEquals("turael", PrimaryMaster.TURAEL.getMasterId());
        assertEquals("spria", PrimaryMaster.SPRIA.getMasterId());
        assertEquals("mazchna", PrimaryMaster.MAZCHNA.getMasterId());
        assertEquals("vannaka", PrimaryMaster.VANNAKA.getMasterId());
        assertEquals("chaeldar", PrimaryMaster.CHAELDAR.getMasterId());
        assertEquals("konar", PrimaryMaster.KONAR.getMasterId());
        assertEquals("krystilia", PrimaryMaster.KRYSTILIA.getMasterId());
        assertEquals("nieve", PrimaryMaster.NIEVE.getMasterId());
        assertEquals("duradel", PrimaryMaster.DURADEL.getMasterId());
        // The config dropdown renders toString(): a human name, not the enum constant.
        assertEquals("Konar quo Maten", PrimaryMaster.KONAR.toString());
        assertTrue(defaults.currentMaster().isPresent());
    }
}

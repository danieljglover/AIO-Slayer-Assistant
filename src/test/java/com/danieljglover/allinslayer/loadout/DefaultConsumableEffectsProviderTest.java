package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.StatChange;
import net.runelite.client.plugins.itemstats.StatsChanges;
import net.runelite.client.plugins.itemstats.stats.Stat;
import net.runelite.client.plugins.itemstats.stats.Stats;
import org.junit.Test;

/**
 * LD04: the provider classifies consumables from the live {@code ItemStatChangesService} when it is
 * available, and from a curated map when it is not (LD00 belt-and-braces). The final value types are
 * built directly from canned {@link StatsChanges}; the {@link Client} is a stub the fake effects
 * ignore.
 */
public class DefaultConsumableEffectsProviderTest
{
    private static final int SHARK = 385;
    private static final int SUPER_COMBAT = 12695;
    private static final int COINS = 995; // not a consumable
    private static final int SARA_BREW = 6685;

    private final Client client = mock(Client.class);

    // --- service path --------------------------------------------------------------------------

    @Test
    public void healAmountReadsTheHitpointsChangeFromTheService()
    {
        ItemStatChangesService service = service()
            .with(SHARK, change(Stats.HITPOINTS, 20))
            .build();
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider(service, client);

        assertEquals(Integer.valueOf(20), p.healAmount(SHARK));
    }

    @Test
    public void boostedStatsReadsPositiveCombatBoostsFromTheService()
    {
        ItemStatChangesService service = service()
            .with(SUPER_COMBAT,
                change(Stats.ATTACK, 19), change(Stats.STRENGTH, 19), change(Stats.DEFENCE, 19))
            .build();
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider(service, client);

        assertTrue(p.boostedStats(SUPER_COMBAT).contains(BoostedStat.ATTACK));
        assertTrue(p.boostedStats(SUPER_COMBAT).contains(BoostedStat.STRENGTH));
        assertTrue(p.boostedStats(SUPER_COMBAT).contains(BoostedStat.DEFENCE));
        assertEquals(p.boostMagnitudes(SUPER_COMBAT).keySet(), p.boostedStats(SUPER_COMBAT));
        assertEquals(Integer.valueOf(19), p.boostMagnitudes(SUPER_COMBAT).get(BoostedStat.STRENGTH));
    }

    @Test
    public void onlyPositiveBoostsCount_drainsAreIgnored()
    {
        // Saradomin brew: heals + boosts Defence, but drains Attack/Strength.
        ItemStatChangesService service = service()
            .with(SARA_BREW,
                change(Stats.HITPOINTS, 16), change(Stats.DEFENCE, 21),
                change(Stats.ATTACK, -8), change(Stats.STRENGTH, -8))
            .build();
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider(service, client);

        assertEquals(Integer.valueOf(16), p.healAmount(SARA_BREW));
        assertTrue(p.boostedStats(SARA_BREW).contains(BoostedStat.DEFENCE));
        assertFalse("a drained stat is not a boost", p.boostedStats(SARA_BREW).contains(BoostedStat.ATTACK));
    }

    @Test
    public void nonFoodHasNoHealAndNoBoosts()
    {
        ItemStatChangesService service = service().build(); // returns null effect for everything
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider(service, client);

        assertNull(p.healAmount(COINS));
        assertTrue(p.boostedStats(COINS).isEmpty());
    }

    // --- curated fallback (service unavailable, LD00) ------------------------------------------

    @Test
    public void curatedFallbackWorksWhenTheServiceIsUnavailable()
    {
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider((ItemStatChangesService) null, client);

        assertEquals(Integer.valueOf(20), p.healAmount(SHARK));
        assertTrue(p.boostedStats(SUPER_COMBAT).contains(BoostedStat.STRENGTH));
        assertTrue(p.boostedStats(SUPER_COMBAT).contains(BoostedStat.ATTACK));
        assertTrue(p.boostedStats(SUPER_COMBAT).contains(BoostedStat.DEFENCE));
        assertNull(p.healAmount(COINS));
    }

    // --- curated fallback: Bastion / Battlemage / divine (LFA hardening) ------------------------

    /** Bastion potion: boosts DEFENCE + RANGED. Every dose resolves via the canonical id. */
    @Test
    public void curatedBastionBoostsDefenceAndRangedForEveryDose()
    {
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider((ItemStatChangesService) null, client);
        for (int dose : new int[]{22461, 22464, 22467, 22470}) // Bastion(4/3/2/1)
        {
            Set<BoostedStat> stats = p.boostedStats(dose);
            assertTrue("Bastion dose " + dose + " boosts DEFENCE", stats.contains(BoostedStat.DEFENCE));
            assertTrue("Bastion dose " + dose + " boosts RANGED", stats.contains(BoostedStat.RANGED));
            assertFalse("Bastion is not a magic potion", stats.contains(BoostedStat.MAGIC));
            assertTrue(p.boostMagnitudes(dose).get(BoostedStat.RANGED) > 0);
        }
    }

    /** Battlemage potion: boosts DEFENCE + MAGIC. Every dose resolves via the canonical id. */
    @Test
    public void curatedBattlemageBoostsDefenceAndMagicForEveryDose()
    {
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider((ItemStatChangesService) null, client);
        for (int dose : new int[]{22449, 22452, 22455, 22458}) // Battlemage(4/3/2/1)
        {
            Set<BoostedStat> stats = p.boostedStats(dose);
            assertTrue("Battlemage dose " + dose + " boosts DEFENCE", stats.contains(BoostedStat.DEFENCE));
            assertTrue("Battlemage dose " + dose + " boosts MAGIC", stats.contains(BoostedStat.MAGIC));
            assertFalse("Battlemage is not a ranged potion", stats.contains(BoostedStat.RANGED));
            assertTrue(p.boostMagnitudes(dose).get(BoostedStat.MAGIC) > 0);
        }
    }

    @Test
    public void curatedDivineVariantsAreRecognised()
    {
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider((ItemStatChangesService) null, client);
        assertTrue(p.boostedStats(23733).contains(BoostedStat.RANGED));   // Divine ranging(4)
        assertTrue(p.boostedStats(23745).contains(BoostedStat.MAGIC));    // Divine magic(4)
        assertTrue(p.boostedStats(24635).contains(BoostedStat.RANGED));   // Divine bastion(4)
        assertTrue(p.boostedStats(24635).contains(BoostedStat.DEFENCE));
        assertTrue(p.boostedStats(24623).contains(BoostedStat.MAGIC));    // Divine battlemage(4)
        assertTrue(p.boostedStats(24623).contains(BoostedStat.DEFENCE));
        assertTrue(p.boostedStats(23685).contains(BoostedStat.STRENGTH)); // Divine super combat(4)
    }

    /** The live service is authoritative when present, even for Bastion/Battlemage. */
    @Test
    public void serviceBastionBoostsDefenceAndRanged()
    {
        ItemStatChangesService service = service()
            .with(22461, change(Stats.DEFENCE, 19), change(Stats.RANGED, 13))
            .build();
        ConsumableEffectsProvider p = new DefaultConsumableEffectsProvider(service, client);

        assertTrue(p.boostedStats(22461).contains(BoostedStat.DEFENCE));
        assertTrue(p.boostedStats(22461).contains(BoostedStat.RANGED));
    }

    // --- canned-effect builders ----------------------------------------------------------------

    private static StatChange change(Stat stat, int theoretical)
    {
        StatChange c = new StatChange();
        c.setStat(stat);
        c.setTheoretical(theoretical);
        return c;
    }

    private static FakeServiceBuilder service()
    {
        return new FakeServiceBuilder();
    }

    private static final class FakeServiceBuilder
    {
        private final Map<Integer, Effect> effects = new HashMap<>();

        FakeServiceBuilder with(int itemId, StatChange... changes)
        {
            List<StatChange> list = new ArrayList<>();
            for (StatChange c : changes)
            {
                list.add(c);
            }
            Effect effect = c ->
            {
                StatsChanges sc = new StatsChanges(list.size());
                sc.setStatChanges(list.toArray(new StatChange[0]));
                return sc;
            };
            effects.put(itemId, effect);
            return this;
        }

        ItemStatChangesService build()
        {
            return effects::get; // null for any id not registered
        }
    }
}

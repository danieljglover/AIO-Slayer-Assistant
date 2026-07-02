package com.danieljglover.allinslayer.loadout;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.StatChange;
import net.runelite.client.plugins.itemstats.StatsChanges;
import net.runelite.client.plugins.itemstats.stats.Stat;
import net.runelite.client.plugins.itemstats.stats.Stats;

/**
 * Reads consumable effects from RuneLite's {@code ItemStatChangesService} (ADR-0004) and falls back
 * to a curated map when the service has no data for an item.
 *
 * <p><b>LFA root cause + fix.</b> RuneLite binds {@code ItemStatChangesService} only inside
 * {@code ItemStatPlugin.configure(Binder)} - i.e. in that plugin's own child injector - and the
 * interface carries no {@code @ImplementedBy}, so an external plugin's injector never sees the
 * binding. That is why the previous {@code @Inject(optional = true)} field came back null in the live
 * client and every consumable silently fell through to the curated map. The service is now injected
 * <b>non-optionally</b>; {@link com.danieljglover.allinslayer.AllInSlayerPlugin} provides the binding
 * by adapting the public {@code ItemStatChanges} (a {@code @Singleton} Guice can just-in-time
 * construct in our injector) to the service interface. The live path therefore actually feeds the
 * selector now.
 *
 * <p>Effect reads call {@code Effect.calculate(client)} which reads live levels - client-thread only
 * (PRD FR-11). The curated fallback collapses potion dose variants via
 * {@code ItemVariationMapping.map(id)} so any dose resolves.
 */
@Singleton
public class DefaultConsumableEffectsProvider implements ConsumableEffectsProvider
{
    private final Client client;
    private final ItemStatChangesService statChangesService;

    /**
     * @param statChangesService the live service (see class doc for how it is bound); a test may pass
     *     a fake, or {@code null} to exercise the curated fallback in isolation.
     */
    @javax.inject.Inject
    public DefaultConsumableEffectsProvider(ItemStatChangesService statChangesService, Client client)
    {
        this.statChangesService = statChangesService;
        this.client = client;
    }

    @Override
    public Integer healAmount(int itemId)
    {
        Integer fromService = serviceHeal(itemId);
        if (fromService != null)
        {
            return fromService;
        }
        return CURATED_HEAL.get(canonical(itemId));
    }

    @Override
    public Set<BoostedStat> boostedStats(int itemId)
    {
        return boostMagnitudes(itemId).keySet();
    }

    @Override
    public Map<BoostedStat, Integer> boostMagnitudes(int itemId)
    {
        Map<BoostedStat, Integer> fromService = serviceBoosts(itemId);
        if (fromService != null && !fromService.isEmpty())
        {
            return fromService;
        }
        Map<BoostedStat, Integer> curated = CURATED_BOOSTS.get(canonical(itemId));
        return curated == null ? Collections.emptyMap() : curated;
    }

    // --- service path --------------------------------------------------------------------------

    private Integer serviceHeal(int itemId)
    {
        StatChange[] changes = changes(itemId);
        if (changes == null)
        {
            return null;
        }
        for (StatChange c : changes)
        {
            if (c.getStat() == Stats.HITPOINTS && c.getTheoretical() > 0)
            {
                return c.getTheoretical();
            }
        }
        return null;
    }

    private Map<BoostedStat, Integer> serviceBoosts(int itemId)
    {
        StatChange[] changes = changes(itemId);
        if (changes == null)
        {
            return null;
        }
        Map<BoostedStat, Integer> boosts = new EnumMap<>(BoostedStat.class);
        for (StatChange c : changes)
        {
            BoostedStat stat = toBoostedStat(c.getStat());
            if (stat != null && c.getTheoretical() > 0)
            {
                boosts.put(stat, c.getTheoretical());
            }
        }
        return boosts;
    }

    private StatChange[] changes(int itemId)
    {
        if (statChangesService == null || client == null)
        {
            return null;
        }
        try
        {
            Effect effect = statChangesService.getItemStatChanges(itemId);
            if (effect == null)
            {
                return null;
            }
            StatsChanges changes = effect.calculate(client);
            return changes == null ? null : changes.getStatChanges();
        }
        catch (RuntimeException e)
        {
            return null; // fall back to curated rather than break a recompute
        }
    }

    private static BoostedStat toBoostedStat(Stat s)
    {
        if (s == Stats.ATTACK)
        {
            return BoostedStat.ATTACK;
        }
        if (s == Stats.STRENGTH)
        {
            return BoostedStat.STRENGTH;
        }
        if (s == Stats.DEFENCE)
        {
            return BoostedStat.DEFENCE;
        }
        if (s == Stats.RANGED)
        {
            return BoostedStat.RANGED;
        }
        if (s == Stats.MAGIC)
        {
            return BoostedStat.MAGIC;
        }
        if (s == Stats.PRAYER)
        {
            return BoostedStat.PRAYER;
        }
        return null;
    }

    private static int canonical(int itemId)
    {
        try
        {
            return ItemVariationMapping.map(itemId);
        }
        catch (RuntimeException e)
        {
            return itemId;
        }
    }

    // --- curated fallback (research C.2/C.3; the service is authoritative when present) ---------

    private static final Map<Integer, Integer> CURATED_HEAL = new HashMap<>();
    private static final Map<Integer, Map<BoostedStat, Integer>> CURATED_BOOSTS = new HashMap<>();

    static
    {
        CURATED_HEAL.put(385, 20);    // Shark
        CURATED_HEAL.put(397, 21);    // Sea turtle
        CURATED_HEAL.put(391, 22);    // Manta ray
        CURATED_HEAL.put(11936, 22);  // Dark crab
        CURATED_HEAL.put(7060, 22);   // Tuna potato
        CURATED_HEAL.put(13441, 22);  // Anglerfish (level-scaled; ~22 at 99, approximate)
        CURATED_HEAL.put(7946, 16);   // Monkfish
        CURATED_HEAL.put(379, 12);    // Lobster
        CURATED_HEAL.put(373, 14);    // Swordfish
        CURATED_HEAL.put(RuneTable.KARAMBWAN_ID, 18); // Cooked karambwan
        CURATED_HEAL.put(6685, 16);   // Saradomin brew(4) (per-dose, approximate)

        // Keyed by the ItemVariationMapping.map() canonical id so every dose resolves. Magnitudes
        // are level-99 approximations; the service is the authority when injectable.
        CURATED_BOOSTS.put(12695, boosts(19, 19, 19, 0, 0, 0)); // Super combat -> ATT/STR/DEF
        CURATED_BOOSTS.put(145, boosts(19, 0, 0, 0, 0, 0));     // Super attack
        CURATED_BOOSTS.put(157, boosts(0, 19, 0, 0, 0, 0));     // Super strength
        CURATED_BOOSTS.put(163, boosts(0, 0, 19, 0, 0, 0));     // Super defence
        CURATED_BOOSTS.put(121, boosts(12, 0, 0, 0, 0, 0));     // Attack potion
        CURATED_BOOSTS.put(113, boosts(0, 12, 0, 0, 0, 0));     // Strength potion
        CURATED_BOOSTS.put(9739, boosts(12, 12, 0, 0, 0, 0));   // Combat potion
        CURATED_BOOSTS.put(169, boosts(0, 0, 0, 13, 0, 0));     // Ranging potion
        CURATED_BOOSTS.put(3040, boosts(0, 0, 0, 0, 4, 0));     // Magic potion

        // LFA: Bastion / Battlemage and the divine variants. Verified against RuneLite 1.12.31.1 that
        // ItemVariationMapping.map() DOES collapse every dose to the 4-dose canonical (Bastion(1/2/3/4)
        // -> 22461, Battlemage -> 22449, each divine -> its own 4-dose id), so one canonical key per
        // family covers all doses - same idiom as Super combat above. (This corrects the earlier note
        // that claimed the doses did not collapse; they do, in this client version.)
        CURATED_BOOSTS.put(22461, boosts(0, 0, 19, 13, 0, 0));  // Bastion -> DEF + RANGED
        CURATED_BOOSTS.put(22449, boosts(0, 0, 19, 0, 4, 0));   // Battlemage -> DEF + MAGIC
        CURATED_BOOSTS.put(23685, boosts(19, 19, 19, 0, 0, 0)); // Divine super combat -> ATT/STR/DEF
        CURATED_BOOSTS.put(23697, boosts(19, 0, 0, 0, 0, 0));   // Divine super attack
        CURATED_BOOSTS.put(23709, boosts(0, 19, 0, 0, 0, 0));   // Divine super strength
        CURATED_BOOSTS.put(23721, boosts(0, 0, 19, 0, 0, 0));   // Divine super defence
        CURATED_BOOSTS.put(23733, boosts(0, 0, 0, 13, 0, 0));   // Divine ranging -> RANGED
        CURATED_BOOSTS.put(23745, boosts(0, 0, 0, 0, 4, 0));    // Divine magic -> MAGIC
        CURATED_BOOSTS.put(24635, boosts(0, 0, 19, 13, 0, 0));  // Divine bastion -> DEF + RANGED
        CURATED_BOOSTS.put(24623, boosts(0, 0, 19, 0, 4, 0));   // Divine battlemage -> DEF + MAGIC
    }

    private static Map<BoostedStat, Integer> boosts(int att, int str, int def, int ranged,
        int magic, int prayer)
    {
        Map<BoostedStat, Integer> m = new EnumMap<>(BoostedStat.class);
        if (att > 0)
        {
            m.put(BoostedStat.ATTACK, att);
        }
        if (str > 0)
        {
            m.put(BoostedStat.STRENGTH, str);
        }
        if (def > 0)
        {
            m.put(BoostedStat.DEFENCE, def);
        }
        if (ranged > 0)
        {
            m.put(BoostedStat.RANGED, ranged);
        }
        if (magic > 0)
        {
            m.put(BoostedStat.MAGIC, magic);
        }
        if (prayer > 0)
        {
            m.put(BoostedStat.PRAYER, prayer);
        }
        return Collections.unmodifiableMap(m);
    }
}

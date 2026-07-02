package com.danieljglover.allinslayer.loadout;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/**
 * Reads item equipment stats from {@code ItemManager.getItemStats(int)} (the non-deprecated 1-arg
 * overload) and maps them onto {@link Bonuses}, including the loadout slot and two-handed flag
 * (ADR-0002).
 *
 * <p>Returns {@code null} when the item is non-equipable or its stats have not loaded yet, and
 * caches only non-null results per id (the stat map is async-remote; a {@code null} is "not loaded",
 * recoverable on a later call - PRD NFR-2/NFR-5). All reads happen on the client thread inside
 * {@code recompute} (PRD FR-11/NFR-3).
 */
@Singleton
public class DefaultEquipmentStatsProvider implements EquipmentStatsProvider
{
    private final ItemManager itemManager;
    private final Map<Integer, Bonuses> cache = new HashMap<>();

    @Inject
    public DefaultEquipmentStatsProvider(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    @Override
    public Bonuses get(int itemId)
    {
        Bonuses cached = cache.get(itemId);
        if (cached != null)
        {
            return cached;
        }

        ItemStats stats = itemManager.getItemStats(itemId);
        if (stats == null || stats.getEquipment() == null)
        {
            // null = non-equipable OR async stat map not loaded yet; skip and never cache (NFR-5).
            return null;
        }

        ItemEquipmentStats e = stats.getEquipment();
        // (int) e.getMdmg() truncates the fractional magic-damage % (e.g. 2.5% -> 2) intentionally:
        // magic damage feeds ranking/display only (GearSelector score + DPS estimate), not exact maths.
        Bonuses bonuses = new Bonuses(
            e.getAstab(), e.getAslash(), e.getAcrush(), e.getAmagic(), e.getArange(),
            e.getStr(), e.getRstr(), (int) e.getMdmg(), e.getAspeed(),
            EquipmentSlots.fromIndex(e.getSlot()), e.isTwoHanded());
        cache.put(itemId, bonuses);
        return bonuses;
    }
}

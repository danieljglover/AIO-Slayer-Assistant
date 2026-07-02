package com.danieljglover.allinslayer.loadout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Resolves the distinct item ids a {@link Recommendation} references and their GE prices.
 *
 * <p>Names and prices are both derived from one id list (via {@link #ids(Recommendation)}) so they
 * always cover the same items. Both the name and price reads happen on the client thread - see
 * {@code AllInSlayerPlugin#recompute} - because {@code ItemManager} must not be touched from the EDT.
 */
public final class LoadoutItems
{
    private LoadoutItems() {}

    /**
     * Distinct item ids in a recommendation, in order: worn, then inventory, then consumable ids
     * (food, combo food, potion, then the magic spell's rune requirement). Null-safe; never returns
     * {@code null}. The recommended-upgrades concept is gone (ADR-0005), so no upgrade ids are added.
     */
    public static List<Integer> ids(Recommendation rec)
    {
        Set<Integer> ids = new LinkedHashSet<>();
        if (rec != null)
        {
            if (rec.getWorn() != null)
            {
                addAll(ids, rec.getWorn().values());
            }
            addAll(ids, rec.getInventory());
            addConsumables(ids, rec.getConsumables());
        }
        return new ArrayList<>(ids);
    }

    private static void addConsumables(Set<Integer> ids, Consumables consumables)
    {
        if (consumables == null)
        {
            return;
        }
        add(ids, consumables.getFoodId());
        add(ids, consumables.getComboFoodId());
        add(ids, consumables.getPotionId());
        MagicSetup magic = consumables.getMagic();
        if (magic != null && magic.getRuneRequirement() != null)
        {
            addAll(ids, magic.getRuneRequirement().keySet());
        }
    }

    private static void add(Set<Integer> ids, Integer id)
    {
        if (id != null)
        {
            ids.add(id);
        }
    }

    /**
     * GE price per id via {@code priceLookup}. Non-positive prices are omitted so an unknown/zero
     * price renders muted and the panel never blocks on price (ADR-0004 / Risk R3). Null-safe; never
     * returns {@code null}.
     */
    public static Map<Integer, Integer> prices(Collection<Integer> ids, IntUnaryOperator priceLookup)
    {
        Map<Integer, Integer> prices = new HashMap<>();
        if (ids == null || priceLookup == null)
        {
            return prices;
        }
        for (Integer id : ids)
        {
            if (id == null || prices.containsKey(id))
            {
                continue;
            }
            int price = priceLookup.applyAsInt(id);
            if (price > 0)
            {
                prices.put(id, price);
            }
        }
        return prices;
    }

    private static void addAll(Set<Integer> ids, Collection<Integer> source)
    {
        if (source == null)
        {
            return;
        }
        for (Integer id : source)
        {
            if (id != null)
            {
                ids.add(id);
            }
        }
    }
}

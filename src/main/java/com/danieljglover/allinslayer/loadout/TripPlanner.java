package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.MonsterStrategy;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.StrategyItemRef;
import com.danieljglover.allinslayer.model.StrategyMethod;
import com.danieljglover.allinslayer.model.TravelItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Composes the suggested 28-slot trip inventory in explicit, ordered layers from what the player owns,
 * the selected strategy method, and the sustain estimate - replacing the fixed-convention
 * {@link TripInventoryPlanner}. Each layer contributes to a shared {@link TripBag}; food fills the
 * remainder last.
 *
 * <p>Layer order (highest priority first): required + key items (the owned base supplies from
 * {@link InventorySelector} plus the method's/strategy's key supplies) &rarr; travel (a Phase-1 stub)
 * &rarr; the method's authored inventory &rarr; sustain (style potion, prayer/restore potions sized by
 * {@link SustainModel}, per-cast runes, combo food) &rarr; food fill. With no strategy method and no
 * sustain signal the layers reduce to exactly today's supplies + potion&times;2 + runes + combo&times;4
 * + food-fill (the FR-6 regression anchor).
 *
 * <p>Owned-only throughout: a key/inventory item is packed only when owned; the best owned of its
 * listed alternatives is chosen; and a resolved item the player owns none of is recorded for the
 * missing-key-items advisory rather than shown as an unusable slot.
 */
public final class TripPlanner
{
    /** Prayer-restore priority (pinned ids, the {@link InventorySelector} discipline): prayer potion
     *  (4..1), super restore (4..1), sanfew serum (4..1). The sustain layer is the sole packer of these. */
    private static final int[] PRAYER_RESTORE = {
        2434, 139, 141, 143,
        3024, 3026, 3028, 3030,
        10925, 10927, 10929, 10931,
    };

    /** How many restore slots to pack for a method that lists prayer potions but has no sustain signal. */
    private static final int LISTED_RESTORE_FLOOR = 2;
    private static final int STYLE_POTION_SLOTS = 2;
    private static final int COMBO_FOOD_SLOTS = 4;

    @Value
    public static class TripPlan
    {
        List<TripSlot> slots;
        /** The "strategy recommends but you own none" advisory, or null when nothing is missing. */
        String missingKeyItemsNote;
    }

    private TripPlanner()
    {
    }

    public static TripPlan plan(TripPlanContext ctx)
    {
        TripBag bag = new TripBag();
        requiredAndKeyItems(ctx, bag);
        travel(ctx, bag);
        methodInventory(ctx, bag);
        sustainAndConsumables(ctx, bag);
        Integer foodId = ctx.getConsumables() == null ? null : ctx.getConsumables().getFoodId();
        List<TripSlot> slots = bag.toSlots(foodId);
        return new TripPlan(slots, missingNote(bag));
    }

    /** Base owned supplies (required item, cannon, antifire, bracelet) + method/strategy key supplies. */
    private static void requiredAndKeyItems(TripPlanContext ctx, TripBag bag)
    {
        OwnedItems owned = ctx.getOwned();
        if (owned == null)
        {
            return;
        }
        for (int id : InventorySelector.select(owned, ctx.getProfile(), ctx.getEffectiveLocation(),
            ctx.getTask()))
        {
            bag.ensure(id, true);
        }
        for (StrategyItemRef ref : strategyWideKeyItems(ctx.getStrategy()))
        {
            handleKeyItem(ref, bag, owned);
        }
        if (ctx.getMethod() != null)
        {
            for (StrategyItemRef ref : nullSafe(ctx.getMethod().getKeyItems()))
            {
                handleKeyItem(ref, bag, owned);
            }
        }
    }

    /**
     * The effective location's teleport/access items (Phase 3): pack the first owned id per travel
     * item; an unowned travel item becomes a "bring X" advisory. No authored travel = no contribution.
     */
    private static void travel(TripPlanContext ctx, TripBag bag)
    {
        OwnedItems owned = ctx.getOwned();
        SlayerLocation location = ctx.getEffectiveLocation();
        if (owned == null || location == null || location.getTravelItems() == null)
        {
            return;
        }
        for (TravelItem item : location.getTravelItems())
        {
            if (item.getItemIds() == null || item.getItemIds().isEmpty())
            {
                continue;
            }
            Integer ownedId = firstOwned(owned, item.getItemIds());
            if (ownedId != null)
            {
                if (!bag.contains(ownedId))
                {
                    bag.ensure(ownedId, true);
                }
            }
            else
            {
                bag.noteMissing(item.getName());
            }
        }
    }

    /** The selected method's authored inventory[] supplies (owned-only), refining the base pack. */
    private static void methodInventory(TripPlanContext ctx, TripBag bag)
    {
        if (ctx.getMethod() == null || ctx.getOwned() == null)
        {
            return;
        }
        for (StrategyItemRef ref : nullSafe(ctx.getMethod().getInventory()))
        {
            handleKeyItem(ref, bag, ctx.getOwned());
        }
    }

    /**
     * Pack the owned supply for a key/inventory ref (skipping prayer-restore, which the sustain layer
     * owns, and anything already in the bag). When the player owns none of the ref's options AND the
     * ref resolved to a real item (supply or gear), record it as missing; a fully unresolved ref
     * (generic terms like "Food", prayers) is dropped silently.
     */
    private static void handleKeyItem(StrategyItemRef ref, TripBag bag, OwnedItems owned)
    {
        if (ref == null)
        {
            return;
        }
        List<StrategyItemRef> options = options(ref);
        boolean anyResolved = false;
        for (StrategyItemRef option : options)
        {
            List<Integer> ids = option.getItemIds();
            if (ids == null || ids.isEmpty())
            {
                continue;
            }
            anyResolved = true;
            Integer ownedId = firstOwned(owned, ids);
            if (ownedId != null)
            {
                if (option.isSupply() && !isPrayerRestore(ownedId) && !bag.contains(ownedId))
                {
                    bag.ensure(ownedId, false);
                }
                return; // owned something across the options -> satisfied, no missing note
            }
        }
        if (anyResolved)
        {
            bag.noteMissing(ref.getName());
        }
    }

    /** Style potion, prayer/restore potions (sized), per-cast runes, and combo food. */
    private static void sustainAndConsumables(TripPlanContext ctx, TripBag bag)
    {
        Consumables consumables = ctx.getConsumables();
        if (consumables != null && consumables.getPotionId() != null)
        {
            bag.add(consumables.getPotionId(), STYLE_POTION_SLOTS, false);
        }

        int restoreSlots = ctx.getSustain() == null ? 0 : ctx.getSustain().getRestoreSlots();
        if (restoreSlots == 0 && methodListsPrayerRestore(ctx))
        {
            restoreSlots = LISTED_RESTORE_FLOOR;
        }
        if (restoreSlots > 0 && ctx.getOwned() != null)
        {
            Integer restoreId = firstOwned(ctx.getOwned(), PRAYER_RESTORE);
            if (restoreId != null)
            {
                bag.add(restoreId, restoreSlots, false);
            }
        }

        if (consumables != null && consumables.getMagic() != null)
        {
            MagicSetup magic = consumables.getMagic();
            if (!magic.isPoweredStaff() && magic.getRuneRequirement() != null)
            {
                for (Map.Entry<Integer, Integer> rune : magic.getRuneRequirement().entrySet())
                {
                    bag.add(rune.getKey(), rune.getValue(), true);
                }
            }
        }

        if (consumables != null && consumables.getComboFoodId() != null)
        {
            bag.add(consumables.getComboFoodId(), COMBO_FOOD_SLOTS, false);
        }
    }

    /** True when any of the method's/strategy's key or inventory refs resolves to a prayer-restore item. */
    private static boolean methodListsPrayerRestore(TripPlanContext ctx)
    {
        if (referencesPrayerRestore(strategyWideKeyItems(ctx.getStrategy())))
        {
            return true;
        }
        StrategyMethod method = ctx.getMethod();
        if (method == null)
        {
            return false;
        }
        return referencesPrayerRestore(method.getKeyItems())
            || referencesPrayerRestore(method.getInventory());
    }

    private static boolean referencesPrayerRestore(List<StrategyItemRef> refs)
    {
        if (refs == null)
        {
            return false;
        }
        for (StrategyItemRef ref : refs)
        {
            for (StrategyItemRef option : options(ref))
            {
                if (option.getItemIds() == null)
                {
                    continue;
                }
                for (int id : option.getItemIds())
                {
                    if (isPrayerRestore(id))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The strategy's role=general (null-style) methods' key items - strategy-wide supplies. */
    private static List<StrategyItemRef> strategyWideKeyItems(MonsterStrategy strategy)
    {
        if (strategy == null || strategy.getMethods() == null)
        {
            return Collections.emptyList();
        }
        List<StrategyItemRef> refs = new ArrayList<>();
        for (StrategyMethod method : strategy.getMethods())
        {
            if (method != null && method.getCombatStyle() == null && method.getKeyItems() != null)
            {
                refs.addAll(method.getKeyItems());
            }
        }
        return refs;
    }

    /** A ref flattened to its options: itself (without its alternatives) followed by each alternative. */
    private static List<StrategyItemRef> options(StrategyItemRef ref)
    {
        List<StrategyItemRef> options = new ArrayList<>();
        if (ref == null)
        {
            return options;
        }
        options.add(ref);
        if (ref.getAlternatives() != null)
        {
            options.addAll(ref.getAlternatives());
        }
        return options;
    }

    private static Integer firstOwned(OwnedItems owned, List<Integer> ids)
    {
        for (int id : ids)
        {
            if (owned.has(id))
            {
                return id;
            }
        }
        return null;
    }

    private static Integer firstOwned(OwnedItems owned, int[] ids)
    {
        for (int id : ids)
        {
            if (owned.has(id))
            {
                return id;
            }
        }
        return null;
    }

    private static boolean isPrayerRestore(int itemId)
    {
        for (int id : PRAYER_RESTORE)
        {
            if (id == itemId)
            {
                return true;
            }
        }
        return false;
    }

    private static String missingNote(TripBag bag)
    {
        List<String> missing = bag.getMissingKeyItems();
        if (missing.isEmpty())
        {
            return null;
        }
        return "Recommended but you own none: " + String.join(", ", missing) + ".";
    }

    private static List<StrategyItemRef> nullSafe(List<StrategyItemRef> refs)
    {
        return refs == null ? Collections.emptyList() : refs;
    }
}

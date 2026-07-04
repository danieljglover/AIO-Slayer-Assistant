package com.danieljglover.allinslayer.loadout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composes the suggested 28-slot trip inventory (the OSRS bag size) for a {@link Recommendation} from
 * the owned {@link InventorySelector} supplies plus the {@link Consumables}. Same shape as
 * {@code InventorySelector} (final, private constructor, one public static factory).
 *
 * <p>Packing order, each category stopping the moment {@link #CAPACITY} is reached:
 *
 * <ol>
 *   <li>the owned supplies (required item, cannon parts + cannonball, antifire, bracelet) - one slot
 *       each, stackable so the cell can carry a stack number;</li>
 *   <li>the potion (when owned) - 2 slots, a trip-packing convention, not combat maths (NG-4);</li>
 *   <li>the per-cast runes (magic tasks on a non-powered staff) - one slot per rune id, quantity =
 *       the PER-CAST requirement from the map. The model has no trip-length estimate, so the honest
 *       per-cast count is shown rather than a fabricated trip total;</li>
 *   <li>the combo food (when owned) - 4 slots, a trip-packing convention;</li>
 *   <li>the food (when owned) - fills ALL remaining slots.</li>
 * </ol>
 *
 * <p>Never returns more than 28 entries; never null. A null {@code supplies} is treated as empty.
 * Duplicated ids across categories are fine - each slot is a slot. Ids are drawn only from the
 * supplies and consumables, so no new item ids (and no LoadoutItems name/price coverage) are needed.
 */
public final class TripInventoryPlanner
{
    /** The OSRS inventory size - the trip inventory never exceeds this many slots. */
    private static final int CAPACITY = 28;

    private TripInventoryPlanner()
    {
    }

    /**
     * The composed 28-slot trip inventory. Never null; never longer than {@link #CAPACITY}.
     *
     * @param supplies the owned pack-list from {@link InventorySelector#select}; null -> empty
     * @param consumables the selected consumables (food, combo food, potion, magic); may be null
     */
    public static List<TripSlot> plan(List<Integer> supplies, Consumables consumables)
    {
        List<TripSlot> slots = new ArrayList<>();

        // a. Owned supplies - one stackable slot each (matches the old InventoryGrid rendering).
        if (supplies != null)
        {
            for (int id : supplies)
            {
                if (slots.size() >= CAPACITY)
                {
                    return slots;
                }
                slots.add(new TripSlot(id, 1, true));
            }
        }

        if (consumables == null)
        {
            return slots;
        }

        // b. Potion - 2 slots (trip-packing convention, NG-4).
        if (consumables.getPotionId() != null)
        {
            for (int i = 0; i < 2; i++)
            {
                if (slots.size() >= CAPACITY)
                {
                    return slots;
                }
                slots.add(new TripSlot(consumables.getPotionId(), 1, false));
            }
        }

        // c. Runes - one slot per rune id, quantity = per-cast requirement. Only for a magic task on a
        // non-powered staff (a powered staff supplies its own attack, so needs no runes).
        MagicSetup magic = consumables.getMagic();
        if (magic != null && !magic.isPoweredStaff()
            && magic.getRuneRequirement() != null && !magic.getRuneRequirement().isEmpty())
        {
            for (Map.Entry<Integer, Integer> rune : magic.getRuneRequirement().entrySet())
            {
                if (slots.size() >= CAPACITY)
                {
                    return slots;
                }
                slots.add(new TripSlot(rune.getKey(), rune.getValue(), true));
            }
        }

        // d. Combo food - 4 slots (trip-packing convention).
        if (consumables.getComboFoodId() != null)
        {
            for (int i = 0; i < 4; i++)
            {
                if (slots.size() >= CAPACITY)
                {
                    return slots;
                }
                slots.add(new TripSlot(consumables.getComboFoodId(), 1, false));
            }
        }

        // e. Food - fills ALL remaining slots.
        if (consumables.getFoodId() != null)
        {
            while (slots.size() < CAPACITY)
            {
                slots.add(new TripSlot(consumables.getFoodId(), 1, false));
            }
        }

        return slots;
    }
}

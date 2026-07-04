package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import net.runelite.client.game.ItemVariationMapping;

/**
 * Compares a {@link Recommendation}'s worn gear + trip inventory against what the player is actually
 * carrying (inventory + equipment, from {@code InventoryService.liveCarried()}), so the loadout card
 * can tint each cell and the bank overlay can list what is still to withdraw (Phase 2). Pure and
 * static - no client access; the caller supplies the carried view.
 *
 * <p>Comparison is dose/variant tolerant: ids are canonicalised through {@link ItemVariationMapping}
 * so a carried Prayer potion(3) counts toward a recommended Prayer potion(4), and a charged item
 * toward its uncharged base. Quantities matter for stacks and multi-slot supplies (18 sharks needs 18
 * carried); worn gear needs one of its canonical id.
 */
public final class LoadoutDiff
{
    /** Per-item carry state relative to the recommendation. */
    public enum Status
    {
        CARRIED,   // have at least as many as recommended
        PARTIAL,   // have some but fewer than recommended
        MISSING    // have none
    }

    /** One outstanding withdrawal for the bank checklist: a representative id and how many are short. */
    @Value
    public static class Need
    {
        int itemId;   // a real (non-canonical) id to resolve a name/icon from
        int have;
        int need;
    }

    private LoadoutDiff()
    {
    }

    /** Status per trip-inventory item id (keyed by the actual slot id so the grid cell can look it up). */
    public static Map<Integer, Status> inventoryStatus(List<TripSlot> trip, OwnedItems carried)
    {
        Map<Integer, Status> out = new HashMap<>();
        if (trip == null || trip.isEmpty())
        {
            return out;
        }
        Map<Integer, Integer> need = neededByCanon(trip);
        Map<Integer, Integer> have = carriedByCanon(carried);
        for (TripSlot slot : trip)
        {
            int canon = ItemVariationMapping.map(slot.getItemId());
            out.put(slot.getItemId(),
                statusOf(have.getOrDefault(canon, 0), need.getOrDefault(canon, 0)));
        }
        return out;
    }

    /** Status per worn slot: CARRIED when the canonical worn id is on the character, else MISSING. */
    public static Map<EquipmentSlot, Status> wornStatus(Map<EquipmentSlot, Integer> worn, OwnedItems carried)
    {
        Map<EquipmentSlot, Status> out = new EnumMap<>(EquipmentSlot.class);
        if (worn == null || worn.isEmpty())
        {
            return out;
        }
        Map<Integer, Integer> have = carriedByCanon(carried);
        for (Map.Entry<EquipmentSlot, Integer> entry : worn.entrySet())
        {
            int canon = ItemVariationMapping.map(entry.getValue());
            out.put(entry.getKey(), have.getOrDefault(canon, 0) >= 1 ? Status.CARRIED : Status.MISSING);
        }
        return out;
    }

    /**
     * The outstanding withdrawals (worn gear + trip supplies the player is short of), in loadout order,
     * for the bank checklist overlay. The caller resolves each {@code itemId} to a name.
     */
    public static List<Need> outstanding(Recommendation rec, OwnedItems carried)
    {
        List<Need> out = new ArrayList<>();
        if (rec == null)
        {
            return out;
        }
        Map<Integer, Integer> need = new LinkedHashMap<>();
        Map<Integer, Integer> rep = new LinkedHashMap<>();   // canon -> a representative real id
        if (rec.getWorn() != null)
        {
            for (int id : rec.getWorn().values())
            {
                addNeed(need, rep, id, 1);
            }
        }
        if (rec.getTripInventory() != null)
        {
            for (TripSlot slot : rec.getTripInventory())
            {
                addNeed(need, rep, slot.getItemId(), slot.isStackable() ? slot.getQuantity() : 1);
            }
        }
        Map<Integer, Integer> have = carriedByCanon(carried);
        for (Map.Entry<Integer, Integer> entry : need.entrySet())
        {
            int canon = entry.getKey();
            int required = entry.getValue();
            int carriedCount = have.getOrDefault(canon, 0);
            if (carriedCount < required)
            {
                out.add(new Need(rep.get(canon), carriedCount, required));
            }
        }
        return out;
    }

    private static void addNeed(Map<Integer, Integer> need, Map<Integer, Integer> rep, int itemId, int qty)
    {
        int canon = ItemVariationMapping.map(itemId);
        need.merge(canon, qty, Integer::sum);
        rep.putIfAbsent(canon, itemId);
    }

    private static Map<Integer, Integer> neededByCanon(List<TripSlot> trip)
    {
        Map<Integer, Integer> need = new HashMap<>();
        for (TripSlot slot : trip)
        {
            int canon = ItemVariationMapping.map(slot.getItemId());
            need.merge(canon, slot.isStackable() ? slot.getQuantity() : 1, Integer::sum);
        }
        return need;
    }

    private static Map<Integer, Integer> carriedByCanon(OwnedItems carried)
    {
        Map<Integer, Integer> have = new HashMap<>();
        if (carried == null)
        {
            return have;
        }
        for (int id : carried.ids())
        {
            have.merge(ItemVariationMapping.map(id), carried.quantity(id), Integer::sum);
        }
        return have;
    }

    private static Status statusOf(int have, int need)
    {
        if (have <= 0)
        {
            return Status.MISSING;
        }
        return have >= need ? Status.CARRIED : Status.PARTIAL;
    }
}

package com.danieljglover.allinslayer.model.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Physical item placement, captured on the client thread. Bank ownership is not preparation. */
@Getter
@EqualsAndHashCode
public final class PreparationSnapshot
{
    private final boolean known;
    private final Map<Integer, Integer> inventory;
    private final Map<String, Stack> equipment;
    private final Map<Integer, Integer> canonicalIds;
    private final Map<Integer, ChargeObservation> charges;

    public PreparationSnapshot(boolean known, Map<Integer, Integer> inventory,
        Map<String, Stack> equipment, Map<Integer, Integer> canonicalIds, Map<Integer, ChargeObservation> charges)
    {
        this.known = known;
        this.inventory = copy(inventory);
        this.equipment = copy(equipment);
        this.canonicalIds = copy(canonicalIds);
        this.charges = copy(charges);
    }

    public static PreparationSnapshot unknown()
    {
        return new PreparationSnapshot(false, null, null, null, null);
    }

    public int canonicalId(int id)
    {
        return canonicalIds.getOrDefault(id, id);
    }

    private static <K, V> Map<K, V> copy(Map<K, V> input)
    {
        return Collections.unmodifiableMap(new LinkedHashMap<>(input == null ? Collections.emptyMap() : input));
    }

    @Getter
    @EqualsAndHashCode
    public static final class Stack
    {
        private final int itemId;
        private final int quantity;

        public Stack(int itemId, int quantity)
        {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }
}

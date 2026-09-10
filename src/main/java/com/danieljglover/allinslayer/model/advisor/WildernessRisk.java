package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** Preparation estimates, separate from the live game's authoritative death interface. */
@Getter
public final class WildernessRisk
{
    private final String area;
    private final long budget;
    private final List<Scenario> planned;
    private final List<Scenario> carried;
    private final List<String> notes;
    private final int plannedEtherCharges;

    public WildernessRisk(String area, long budget, List<Scenario> planned,
        List<Scenario> carried, List<String> notes)
    {
        this(area, budget, planned, carried, notes, 0);
    }

    public WildernessRisk(String area, long budget, List<Scenario> planned,
        List<Scenario> carried, List<String> notes, int plannedEtherCharges)
    {
        this.area = area;
        this.budget = Math.max(0, budget);
        this.planned = copy(planned);
        this.carried = copy(carried);
        this.notes = copy(notes);
        this.plannedEtherCharges = Math.max(0, Math.min(16000, plannedEtherCharges));
    }

    public Scenario baseline() { return planned.get(0); }

    public boolean isWithinBudget()
    {
        return baseline().isComplete() && baseline().getPermanentLosses() == 0
            && baseline().getTotalRisk() <= budget;
    }

    private static <T> List<T> copy(List<T> values)
    {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Getter
    public static final class Scenario
    {
        private final String name;
        private final int protectedSlots;
        private final long lostValue;
        private final long repairCost;
        private final long entryFee;
        private final int permanentLosses;
        private final boolean complete;
        private final List<ItemLoss> items;
        private final List<String> warnings;

        public Scenario(String name, int protectedSlots, long lostValue, long repairCost,
            long entryFee, int permanentLosses, boolean complete, List<ItemLoss> items,
            List<String> warnings)
        {
            this.name = name;
            this.protectedSlots = protectedSlots;
            this.lostValue = lostValue;
            this.repairCost = repairCost;
            this.entryFee = entryFee;
            this.permanentLosses = permanentLosses;
            this.complete = complete;
            this.items = copy(items);
            this.warnings = copy(warnings);
        }

        public long getTotalRisk() { return saturatedAdd(saturatedAdd(lostValue, repairCost), entryFee); }
    }

    @Getter
    public static final class ItemLoss
    {
        private final int itemId;
        private final String name;
        private final int quantity;
        private final int protectedQuantity;
        private final long protectionValue;
        private final String outcome;
        private final long lostValue;
        private final long repairCost;
        private final boolean permanent;
        private final boolean known;
        private final String detail;

        public ItemLoss(int itemId, String name, int quantity, int protectedQuantity,
            long protectionValue, String outcome, long lostValue, long repairCost,
            boolean permanent, boolean known, String detail)
        {
            this.itemId = itemId;
            this.name = name;
            this.quantity = quantity;
            this.protectedQuantity = protectedQuantity;
            this.protectionValue = protectionValue;
            this.outcome = outcome;
            this.lostValue = lostValue;
            this.repairCost = repairCost;
            this.permanent = permanent;
            this.known = known;
            this.detail = detail;
        }
    }

    public static long saturatedAdd(long a, long b)
    {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    public static long saturatedMultiply(long value, long quantity)
    {
        return value == 0 || quantity == 0 ? 0
            : value > Long.MAX_VALUE / quantity ? Long.MAX_VALUE : value * quantity;
    }
}

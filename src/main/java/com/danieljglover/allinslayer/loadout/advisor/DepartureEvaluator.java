package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.AccountProgress.State;
import com.danieljglover.allinslayer.model.advisor.ChargeObservation;
import com.danieljglover.allinslayer.model.advisor.DeathContext;
import com.danieljglover.allinslayer.model.advisor.DeathRule;
import com.danieljglover.allinslayer.model.advisor.DepartureCheck;
import com.danieljglover.allinslayer.model.advisor.DepartureCheck.Entry;
import com.danieljglover.allinslayer.model.advisor.ItemValue;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.PreparationSnapshot;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Setup;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/** Compares the selected plan with physical placement, never with estimated bank ownership. */
public final class DepartureEvaluator
{
    private DepartureEvaluator() { }

    public static DepartureCheck assess(SlayerCatalogue catalogue, RecommendationRequest request,
        Setup setup, Map<String, RequirementEvaluator.Assessment> requirements)
    {
        List<Entry> entries = new ArrayList<>();
        PlayerSnapshot player = request.getPlayer();
        PreparationSnapshot actual = player.getPreparation();
        if (!player.isLoggedIn() || !actual.isKnown())
        {
            entries.add(new Entry("Waiting for current items", "Log in to check your worn equipment and backpack.", true));
            return new DepartureCheck(entries);
        }

        Map<Integer, Integer> inventory = canonicalQuantities(actual.getInventory(), actual);
        for (Map.Entry<String, Choice> slot : setup.getEquipment().entrySet())
        {
            Choice choice = slot.getValue();
            if (!physical(choice)) { continue; }
            int id = actual.canonicalId(choice.getItemId());
            PreparationSnapshot.Stack worn = actual.getEquipment().get(slot.getKey());
            int equipped = worn != null && actual.canonicalId(worn.getItemId()) == id ? worn.getQuantity() : 0;
            int remaining = Math.max(0, choice.getQuantity() - equipped);
            if (remaining > 0)
            {
                entries.add(new Entry("Equip " + amount(choice.getName(), remaining),
                    "Required in " + slot.getKey().toLowerCase(Locale.ROOT) + ". "
                        + availability(choice, remaining, inventory.getOrDefault(id, 0)), false));
                // A copy waiting to be equipped cannot also satisfy an inventory switch or supply.
                take(inventory, id, remaining);
            }
        }
        for (Choice choice : setup.getInventory())
        {
            if (!physical(choice)) { continue; }
            int remaining = choice.getQuantity() - take(inventory, actual.canonicalId(choice.getItemId()), choice.getQuantity());
            if (remaining > 0)
            {
                entries.add(new Entry("Pack " + amount(choice.getName(), remaining),
                    "Backpack is short of this quantity. " + availability(choice, remaining, 0), false));
            }
        }
        pouch(catalogue, setup, player, entries);
        charges(catalogue, setup, request, entries);
        wilderness(catalogue, setup, player, entries);

        requirements.forEach((name, assessment) -> {
            if (assessment.getState() != State.MET || assessment.isAssumed())
            {
                entries.add(new Entry(assessment.isAssumed() ? "Check assignment access" : name,
                    assessment.getDetail(), true));
            }
        });
        for (String blocker : setup.getBlockers())
        {
            if (requirements.keySet().stream().anyMatch(blocker::contains)) { continue; }
            // Missing item rows above already give the actual quantity and destination.
            if (blocker.startsWith("Missing") && choices(setup).stream().anyMatch(c -> blocker.contains(c.getName()))) { continue; }
            entries.add(new Entry("Resolve setup blocker", blocker, true));
        }
        if (!setup.isFeasible() && entries.isEmpty())
        {
            entries.add(new Entry("Review setup", "The selected recommendation is not currently feasible.", true));
        }
        return new DepartureCheck(entries);
    }

    private static void pouch(SlayerCatalogue catalogue, Setup setup, PlayerSnapshot player, List<Entry> entries)
    {
        Map<Integer, Integer> planned = new LinkedHashMap<>();
        setup.getInventory().stream().filter(c -> "RUNE POUCH".equals(c.getSlot()) && c.getQuantity() > 0)
            .forEach(c -> planned.merge(c.getItemId(), c.getQuantity(), DepartureEvaluator::add));
        boolean selected = setup.getInventory().stream().filter(DepartureEvaluator::physical)
            .anyMatch(c -> catalogue.getPreparation().getPouches().stream().anyMatch(p -> p.getItemId() == c.getItemId()));
        if (!selected && planned.isEmpty()) { return; }
        boolean packed = setup.getInventory().stream().filter(DepartureEvaluator::physical)
            .anyMatch(c -> player.getPreparation().getInventory().getOrDefault(c.getItemId(), 0) > 0
                && catalogue.getPreparation().getPouches().stream().anyMatch(p -> p.getItemId() == c.getItemId()));
        DeathContext context = player.getDeathContext();
        if (!packed || !context.isPouchContentsKnown())
        {
            entries.add(new Entry("Check rune pouch contents", packed ? "Current contents are unavailable."
                : "Pack the selected pouch before its contents can be confirmed.", true));
            return;
        }
        Set<Integer> ids = new LinkedHashSet<>(planned.keySet());
        ids.addAll(context.getPouchContents().keySet());
        for (int id : ids)
        {
            int target = planned.getOrDefault(id, 0);
            int current = context.getPouchContents().getOrDefault(id, 0);
            if (current == target) { continue; }
            entries.add(new Entry((current < target ? "Load " : "Unload ") + amount(name(player, id), Math.abs(target - current)),
                "Rune pouch: " + number(current) + " / " + number(target) + " planned. Loose runes do not count as loaded.", false));
        }
    }

    private static void charges(SlayerCatalogue catalogue, Setup setup, RecommendationRequest request, List<Entry> entries)
    {
        PlayerSnapshot player = request.getPlayer();
        Set<Integer> checked = new LinkedHashSet<>();
        for (Choice choice : choices(setup))
        {
            if (!physical(choice) || !checked.add(choice.getItemId())) { continue; }
            ItemValue value = player.getItemValues().get(choice.getItemId());
            DeathRule rule = catalogue.getDeathRules().get(choice.getItemId());
            boolean ether = rule != null && "ETHER_WEAPON".equals(rule.getContentsType());
            boolean powered = catalogue.getPreparation().getPoweredWeapons().containsKey(choice.getItemId());
            boolean spellCharges = choice.getName().startsWith("Iban's staff") || choice.getItemId() == 21255;
            ChargeObservation observed = player.getPreparation().getCharges().get(choice.getItemId());
            if (!ether && !powered && !spellCharges && observed == null
                && (value == null || !value.isChargesUnknown())) { continue; }
            int minimum = ether ? 1
                : powered || spellCharges ? catalogue.getPreparation().getCombatCasts() : 1;
            if (observed != null && observed.isObserved() && observed.getCharges() >= 0)
            {
                if (ether && observed.getCharges() > request.getWildernessChargeLimit())
                {
                    entries.add(new Entry("Remove excess ether from " + choice.getName(),
                        chargeDetails(observed, player) + " Alert limit: " + number(request.getWildernessChargeLimit())
                        + " usable charges, excluding the 1,000 activation ether. Change Wildy charge limit in AIO settings.", false));
                }
                else if (observed.getCharges() < minimum)
                {
                    entries.add(new Entry("Charge " + choice.getName(), chargeDetails(observed, player)
                        + " Needs at least " + number(minimum)
                        + (observed.getResources().isEmpty() ? " usable charges." : " of each loaded resource."), false));
                }
                continue;
            }
            String detail = ether
                ? "Confirm a usable charge balance. The excess-charge alert applies only above "
                    + number(request.getWildernessChargeLimit()) + " usable charges; the 1,000 activation ether is separate."
                : powered || spellCharges ? "Check at least " + number(catalogue.getPreparation().getCombatCasts())
                    + " loaded charges for this trip. Loose runes are not loaded charges."
                : "Check the remaining charges and any loaded ammunition before departure.";
            entries.add(new Entry("Check " + choice.getName() + " charges", (observed == null ? "Balance unknown. "
                : chargeDetails(observed, player) + " This tracker balance is an estimate shared by item family. ") + detail
                + " Right-click the carried item in game and choose Check. AIO records the result automatically; use or recharge can require another check.", true));
        }
    }

    private static void wilderness(SlayerCatalogue catalogue, Setup setup, PlayerSnapshot player, List<Entry> entries)
    {
        WildernessRisk risk = setup.getWildernessRisk();
        if (risk == null) { return; }
        PreparationSnapshot actual = player.getPreparation();
        Map<Integer, Integer> planned = new LinkedHashMap<>();
        choices(setup).stream().filter(DepartureEvaluator::physical)
            .forEach(c -> planned.merge(actual.canonicalId(c.getItemId()), c.getQuantity(), DepartureEvaluator::add));
        // Include noted items here: they fail packing checks and still add Wilderness risk.
        canonicalQuantities(player.getRiskCarried(), actual).forEach((id, quantity) -> {
            int surplus = quantity - planned.getOrDefault(id, 0);
            if (surplus > 0)
            {
                entries.add(new Entry("Bank extra " + amount(name(player, id), surplus),
                    "Outside the selected Wilderness setup; it adds risk or changes which items are protected.", false));
            }
        });
        boolean bag = choices(setup).stream().filter(DepartureEvaluator::physical)
            .anyMatch(c -> catalogue.getPreparation().getLootingBagIds().contains(c.getItemId()));
        if (bag)
        {
            DeathContext context = player.getDeathContext();
            if (!context.isLootingBagContentsKnown())
            {
                entries.add(new Entry("Check looting bag is empty", context.getLootingBagDetail()
                    + " Use the bag's View/Check option in game once to refresh it. Closing the view keeps that observation.", true));
            }
            else if (!context.getLootingBagContents().isEmpty())
            {
                entries.add(new Entry("Empty looting bag", "The selected setup assumes an empty bag at departure.", false));
            }
        }
        if (!risk.getCarried().isEmpty())
        {
            WildernessRisk.Scenario carried = risk.getCarried().get(0);
            if (carried.getPermanentLosses() > 0)
            {
                entries.add(new Entry("Review items lost permanently", "Current carried items include permanent losses in the baseline death scenario. Open Wilderness risk below.", true));
            }
            if (carried.getTotalRisk() > risk.getBudget())
            {
                entries.add(new Entry("Carried risk exceeds budget", number(carried.getTotalRisk()) + " gp vs "
                    + number(risk.getBudget()) + " gp budget, before any unverified contents. Open Wilderness risk below.", true));
            }
            if (!carried.isComplete())
            {
                entries.add(new Entry("Carried risk is unverified", "Some charges, contents or values are unknown. The planned estimate is not confirmation of what you are carrying.", true));
            }
        }
    }

    public static String chargeDetails(ChargeObservation charge, PlayerSnapshot player)
    {
        String balance;
        if (charge.getResources().isEmpty())
        {
            balance = charge.getCharges() < 0 ? "balance unknown" : number(charge.getCharges()) + " charges";
        }
        else
        {
            List<String> parts = new ArrayList<>();
            charge.getResources().forEach((id, count) -> parts.add(name(player, id) + " x" + number(count)));
            balance = String.join(", ", parts);
            if (charge.getCharges() < 0) { balance += "; other loaded resources unknown"; }
            else if (charge.getCharges() == 0) { balance += "; not ready to use"; }
        }
        return charge.getSource() + ": " + balance + ".";
    }

    private static List<Choice> choices(Setup setup)
    {
        List<Choice> result = new ArrayList<>(setup.getEquipment().values());
        result.addAll(setup.getInventory());
        return result;
    }

    private static boolean physical(Choice choice)
    {
        return choice.getItemId() > 0 && choice.getQuantity() > 0 && !"RUNE POUCH".equals(choice.getSlot());
    }

    private static Map<Integer, Integer> canonicalQuantities(Map<Integer, Integer> input, PreparationSnapshot actual)
    {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        input.forEach((id, quantity) -> result.merge(actual.canonicalId(id), quantity, DepartureEvaluator::add));
        return result;
    }

    private static int take(Map<Integer, Integer> quantities, int id, int needed)
    {
        int available = quantities.getOrDefault(id, 0);
        int taken = Math.min(needed, available);
        quantities.put(id, available - taken);
        return taken;
    }

    private static String availability(Choice choice, int remaining, int inBackpack)
    {
        return inBackpack >= remaining ? "In your backpack."
            : choice.getMissing() > 0 ? "Some items are missing from known stock; check your bank."
            : "Use the bank filter to find the selected item. Exact doses and charged forms must match.";
    }

    private static String name(PlayerSnapshot player, int id)
    {
        ItemValue value = player.getItemValues().get(id);
        PlayerSnapshot.ItemStats stats = player.getItems().get(id);
        return value != null ? value.getName() : stats != null ? stats.getName() : "Item " + id;
    }

    private static String amount(String name, int quantity) { return name + (quantity == 1 ? "" : " x" + number(quantity)); }
    private static String number(long value) { return String.format(Locale.ENGLISH, "%,d", value); }
    private static int add(int a, int b) { return (int) Math.min(Integer.MAX_VALUE, (long) a + b); }
}

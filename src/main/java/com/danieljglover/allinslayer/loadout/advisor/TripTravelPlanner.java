package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.ReturnDestination;
import com.danieljglover.allinslayer.model.advisor.ItemValue;
import com.danieljglover.allinslayer.model.advisor.AccountProgress;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.ItemStats;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.ItemDefinition;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Supply;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData.Teleport;
import com.danieljglover.allinslayer.model.advisor.WildernessArea;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Value;

/** Plans portable return/abort resources. It never invokes a teleport or changes a route. */
final class TripTravelPlanner
{
    private final SlayerCatalogue catalogue;
    private final TripPreparationData data;
    private final RecommendationRequest request;
    private final RequirementEvaluator requirements;
    private final SpellPlanner spells;

    TripTravelPlanner(SlayerCatalogue catalogue, RecommendationRequest request)
    {
        this.catalogue = catalogue;
        this.data = catalogue.getPreparation();
        this.request = request;
        this.requirements = new RequirementEvaluator(request);
        this.spells = new SpellPlanner(data, request);
    }

    @Value
    static class TravelPlan
    {
        List<Supply> supplies;
        List<Supply> runes;
        List<String> notes;
        List<String> blockers;
    }

    TravelPlan plan(Location location, Map<String, Choice> equipment)
    {
        return plan(location, equipment, Collections.emptyList());
    }

    TravelPlan plan(Location location, Map<String, Choice> equipment, List<Supply> reservedCasting)
    {
        List<Supply> supplies = new ArrayList<>();
        List<Supply> runes = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        Map<Integer, Integer> remaining = new LinkedHashMap<>(request.getPlayer().getOwned());
        reserve(remaining, reservedCasting);
        ReturnDestination target = request.getReturnDestination();
        List<String> routes = target == ReturnDestination.HOUSE ? data.getHouseRoutes()
            : target == ReturnDestination.BANK ? data.getBankRoutes()
            : data.getMasterRoutes().getOrDefault(request.getMasterId(), Collections.emptyList());
        Option back = firstOption(routes, location, equipment, remaining);
        boolean bankFallback = back == null && target == ReturnDestination.SLAYER_MASTER;
        if (bankFallback) { back = firstOption(data.getBankRoutes(), location, equipment, remaining); }
        if (back != null) { reserve(remaining, back.runes); }
        boolean deferredReturn = false;
        WildernessArea area = catalogue.getWildernessAreas().get(location.getId());
        boolean wilderness = location.isWilderness() || area != null && area.isWildernessTravel();
        if (wilderness)
        {
            int maxLevel = area == null ? -1 : area.getMaxLevel();
            final Option returnOption = back;
            List<Option> escapes = new ArrayList<>();
            for (Teleport teleport : data.getTeleports().values())
            {
                if (!teleport.isEmergency()) { continue; }
                // Reuse the already-reserved return cast for the alternative abort outcome.
                Option option = back != null && back.teleport.getId().equals(teleport.getId())
                    ? back : option(teleport, location, equipment, remaining);
                if (option != null) { escapes.add(option); }
            }
            // Above level 20 (or with unknown depth), retain the broader level-30 exit.
            escapes.sort(Comparator.comparingInt((Option o) ->
                (maxLevel < 0 || maxLevel > 20) && o.teleport.getWildernessLimit() < 30 ? 1 : 0)
                .thenComparingInt(o -> o.teleport.getEscapePriority())
                .thenComparingInt(o -> returnOption != null && o.itemId > 0 && o.itemId == returnOption.itemId ? 0 : 1));
            if (escapes.isEmpty())
            {
                String missing = "Escape: no verified owned and usable emergency teleport remains after reserving combat and return resources. Arrange one before entering the Wilderness.";
                notes.add(missing);
                blockers.add(missing);
            }
            else
            {
                Option escape = escapes.get(0);
                if (escape.teleport.isBankNearby())
                {
                    if (target == ReturnDestination.BANK || bankFallback
                        || target == ReturnDestination.SLAYER_MASTER && routes.contains(escape.teleport.getId())
                            && (back == null || !back.teleport.isRemoteContact()))
                    {
                        // One portable exit can serve both roles when its destination already fits.
                        back = escape;
                    }
                    else if (target == ReturnDestination.SLAYER_MASTER && back != null
                        && !back.teleport.isRemoteContact() && !back.runes.isEmpty()
                        && !sharesReturnUse(back, escape))
                    {
                        // Collect onward spell resources at the escape destination's bank instead
                        // of carrying multiple extra rune stacks through Wilderness combat.
                        notes.add("Return: use " + escape.teleport.getName() + " and bank near "
                            + escape.teleport.getDestination() + ". Collect the runes"
                            + (back.accessItem > 0 ? " and " + catalogue.getItems().get(back.accessItem).getName() : "")
                            + " for "
                            + back.teleport.getName() + " there before continuing to your Slayer master."
                            + " Onward teleport resources are not packed for this trip. "
                            + safe(back.teleport.getNote()));
                        appendConfigurationNotes(back, "Return: ", notes);
                        back = null;
                        deferredReturn = true;
                    }
                }
                // Finishing and aborting are alternative outcomes; one remaining use can serve both.
                if (!sharesReturnUse(back, escape))
                {
                    pack(escape, supplies, runes);
                }
                notes.add("Escape: " + escape.teleport.getName() + " works at Wilderness level "
                    + escape.teleport.getWildernessLimit() + " or below. "
                    + (maxLevel < 0 || maxLevel > escape.teleport.getWildernessLimit()
                        ? "Leave the boss arena if necessary and retreat within that limit before teleporting. " : "")
                    + safe(escape.teleport.getNote()));
                Set<String> mechanisms = new LinkedHashSet<>();
                mechanisms.add(mechanism(escape));
                List<String> alternatives = new ArrayList<>();
                for (Option alternative : escapes)
                {
                    if (mechanisms.add(mechanism(alternative)))
                    {
                        alternatives.add(alternative.teleport.getName() + " (level "
                            + alternative.teleport.getWildernessLimit() + ")");
                    }
                    if (alternatives.size() == 3) { break; }
                }
                if (!alternatives.isEmpty())
                {
                    notes.add("Escape: owned alternatives, not additionally packed: "
                        + String.join("; ", alternatives) + ".");
                }
            }
            for (String note : data.getWildernessEscapeNotes()) { notes.add("Escape: " + note); }
            if (back != null && !back.teleport.isRemoteContact())
            {
                notes.add("Return: this teleport also requires Wilderness level " + back.teleport.getWildernessLimit()
                    + " or below; leave restricted areas first.");
            }
        }
        if (back != null)
        {
            pack(back, supplies, runes);
            String master = catalogue.getMasters().containsKey(request.getMasterId())
                ? catalogue.getMasters().get(request.getMasterId()).getName() : "your Slayer master";
            String purpose = target == ReturnDestination.HOUSE ? "your house"
                : target == ReturnDestination.BANK || bankFallback ? "banking" : master;
            notes.add("Return: " + back.teleport.getName() + " for " + purpose + ". "
                + (back.teleport.isRemoteContact() ? "Request the next assignment remotely. " : "")
                + (back.teleport.isBankNearby() ? "A bank is nearby. " : "")
                + safe(back.teleport.getNote()));
            appendConfigurationNotes(back, "Return: ", notes);
            if (bankFallback)
            {
                notes.add("Return: no usable direct route to " + master
                    + " was found; bank first, then arrange the next-assignment journey.");
            }
        }
        else if (!deferredReturn)
        {
            notes.add("Return: no owned and unlocked " + target.toString().toLowerCase(java.util.Locale.ROOT)
                + " return option is available. Check teleport stock and unlocks before leaving.");
            if (target == ReturnDestination.HOUSE && !requirements.isMet("Own a player-owned house"))
            {
                blockers.add("Confirm requirement: Own a player-owned house");
            }
        }
        return new TravelPlan(supplies, runes, notes, blockers);
    }

    private Option firstOption(List<String> ids, Location location, Map<String, Choice> equipment,
        Map<Integer, Integer> remaining)
    {
        for (String id : ids)
        {
            Option option = option(data.getTeleports().get(id), location, equipment, remaining);
            if (option != null) { return option; }
        }
        return null;
    }

    private void appendConfigurationNotes(Option option, String prefix, List<String> notes)
    {
        Set<String> sources = new LinkedHashSet<>();
        for (String requirement : option.teleport.getRequirements())
        {
            String source = request.getPlayer().getAccountProgress().getConfiguredRequirements()
                .get(AccountProgress.key(requirement));
            if (source != null) { sources.add(source); }
        }
        for (String source : sources) { notes.add(prefix + source + " Imported configuration, not observed furniture."); }
    }

    private static String mechanism(Option option)
    {
        return option.itemId > 0 ? "item:" + option.itemId : "spell:" + option.teleport.getSpellName();
    }

    private Option option(Teleport teleport, Location location, Map<String, Choice> equipment,
        Map<Integer, Integer> remaining)
    {
        if (teleport == null || teleport.getRequirements().stream().anyMatch(r -> !requirements.isMet(r))) { return null; }
        int accessItem = teleport.getRequiredItemIds().stream()
            .filter(id -> request.getPlayer().getOwned().getOrDefault(id, 0) > 0)
            .filter(this::helperUsable).findFirst().orElse(0);
        if (!teleport.getRequiredItemIds().isEmpty() && accessItem == 0) { return null; }
        if (teleport.getSpellName() != null)
        {
            TripPreparationData.Spell spell = data.getSpells().get(teleport.getSpellName());
            if (spell == null || !spells.problems(spell, equipment).isEmpty()) { return null; }
            List<Supply> runes = spells.suppliesFor(spell, 1, equipment, false, remaining);
            if (runes.stream().anyMatch(s -> s.getItemIds().stream().mapToLong(id ->
                remaining.getOrDefault(id, 0)).sum() < s.getQuantity())) { return null; }
            return new Option(teleport, 0, 1, accessItem, runes);
        }
        List<Integer> variants = new ArrayList<>(teleport.getItemIds());
        variants.sort(Comparator.comparingInt((Integer id) -> equipment.values().stream()
            .anyMatch(choice -> choice.getItemId() == id && choice.getMissing() == 0 && choice.getQuantity() > 0) ? 0 : 1)
            // An eternal or ornamented piece is not worth risking merely for a spare teleport.
            .thenComparingLong(this::itemPrice)
            .thenComparingInt(id -> teleport.getCharges().getOrDefault(id, -1) == 0
                ? Integer.MIN_VALUE : -teleport.getCharges().getOrDefault(id, -1)));
        for (int id : variants)
        {
            if (request.getPlayer().getOwned().getOrDefault(id, 0) <= 0) { continue; }
            // Do not spend the sole charge/item on the outward journey and promise it again for return.
            int outward = location.getTravel().stream().filter(s -> s.getItemIds().contains(id))
                .mapToInt(s -> Math.max(1, s.getQuantity())).max().orElse(0);
            int charges = teleport.getCharges().getOrDefault(id, -1);
            int count = teleport.isConsumable() ? outward + 1 : 1;
            if (charges < 0 || charges > 0 && !teleport.isConsumable() && charges <= outward
                || request.getPlayer().getOwned().getOrDefault(id, 0) < count) { continue; }
            return new Option(teleport, id, count, accessItem, Collections.emptyList());
        }
        return null;
    }

    private long itemPrice(int id)
    {
        ItemValue value = request.getPlayer().getItemValues().get(id);
        return value == null || value.getMarketPrice() < 0 ? Long.MAX_VALUE : value.getMarketPrice();
    }

    private boolean helperUsable(int id)
    {
        ItemStats stats = request.getPlayer().getItems().get(id);
        ItemDefinition definition = catalogue.getItems().get(id);
        if (stats == null || !stats.isUsable() || definition == null || !definition.isUsable()) { return false; }
        if (!definition.isRequirementsKnown() && !requirements.isMet("Can equip: " + definition.getName())) { return false; }
        return definition.getLevels().entrySet().stream()
            .allMatch(level -> requirements.isMet(level.getKey() + " >= " + level.getValue()))
            && definition.getRequirements().stream().allMatch(requirements::isMet);
    }

    private static boolean sharesReturnUse(Option back, Option escape)
    {
        if (back == null) { return false; }
        if (back.itemId > 0) { return back.itemId == escape.itemId; }
        return back.teleport.getSpellName() != null
            && back.teleport.getSpellName().equals(escape.teleport.getSpellName());
    }

    private static void reserve(Map<Integer, Integer> remaining, List<Supply> supplies)
    {
        for (Supply supply : supplies)
        {
            int missing = Math.max(0, supply.getQuantity());
            for (int id : supply.getItemIds())
            {
                int owned = remaining.getOrDefault(id, 0);
                int used = Math.min(missing, owned);
                remaining.put(id, owned - used);
                missing -= used;
                if (missing == 0) { break; }
            }
        }
    }

    private void pack(Option option, List<Supply> supplies, List<Supply> runes)
    {
        if (option.itemId > 0) { supplies.add(item(option.teleport.getName(), option.itemId, option.quantity)); }
        if (option.accessItem > 0) { supplies.add(item("Return route access item", option.accessItem, 1)); }
        runes.addAll(option.runes);
    }

    private static Supply item(String name, int id, int quantity)
    {
        Supply supply = new Supply();
        supply.setName(name);
        supply.setItemIds(Collections.singletonList(id));
        supply.setQuantity(quantity);
        supply.setRequired(true);
        return supply;
    }

    private static String safe(String text) { return text == null ? "" : text; }

    @Value
    private static class Option
    {
        Teleport teleport;
        int itemId;
        int quantity;
        int accessItem;
        List<Supply> runes;
    }
}

package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.DeathContext;
import com.danieljglover.allinslayer.model.advisor.DeathRule;
import com.danieljglover.allinslayer.model.advisor.ItemValue;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location;
import com.danieljglover.allinslayer.model.advisor.WildernessArea;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk.ItemLoss;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk.Scenario;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.danieljglover.allinslayer.model.advisor.WildernessRisk.saturatedAdd;
import static com.danieljglover.allinslayer.model.advisor.WildernessRisk.saturatedMultiply;

/** Pure PvP death estimates for an entire trip; no client access or item-family normalization. */
public final class WildernessRiskCalculator
{
    private static final int REVENANT_ETHER = 21820;
    private final SlayerCatalogue catalogue;
    private final PlayerSnapshot player;
    private final Location location;
    private final WildernessArea area;
    private final int plannedEtherCharges;

    public WildernessRiskCalculator(SlayerCatalogue catalogue, PlayerSnapshot player, Location location)
    {
        this(catalogue, player, location, 0);
    }

    public WildernessRiskCalculator(SlayerCatalogue catalogue, PlayerSnapshot player, Location location,
        int plannedEtherCharges)
    {
        this.catalogue = catalogue;
        this.player = player;
        this.location = location;
        this.area = catalogue.getWildernessAreas().get(location.getId());
        this.plannedEtherCharges = Math.max(0, Math.min(16000, plannedEtherCharges));
    }

    public boolean applies()
    {
        return location.isWilderness() || area != null && area.isWildernessTravel();
    }

    public WildernessRisk assess(Map<String, Choice> equipment, List<Choice> inventory, long budget)
    {
        Map<Integer, Integer> planned = quantities(equipment, inventory);
        Map<Integer, Integer> pouch = plannedPouchContents(inventory);
        boolean emptyBag = plansEmptyBag(inventory);
        boolean missing = inventory.stream().anyMatch(c -> c.getMissing() > 0)
            || equipment.values().stream().anyMatch(c -> c.getMissing() > 0);
        List<Scenario> plans = scenarios(planned, false, missing, pouch, emptyBag);
        List<Scenario> carried = scenarios(player.getRiskCarried(), true, false, null, false);
        int etherTarget = usesPlannedEther(planned) ? plannedEtherCharges : 0;
        List<String> notes = new ArrayList<>();
        notes.add("PvP death at this destination or on its Wilderness route. Equipment, ammunition, inventory and switches share the protected-item allowance; bank items are not at risk.");
        if (player.getDeathContext().isKnown())
        {
            DeathContext observed = player.getDeathContext();
            notes.add("Observed account: " + (observed.isSkulled() ? "skulled" : "unskulled")
                + "; Protect Item " + (observed.isProtectItemActive() ? "active" : "inactive")
                + (observed.isHighRisk() ? "; high-risk world." : "."));
        }
        notes.add("The baseline does not rely on Protect Item. The additional-item scenario requires the prayer to remain active at death; loot or equipment changes can change which item is protected.");
        if (!pouch.isEmpty())
        {
            notes.add("Planned rune pouch losses use the displayed configuration, not its observed contents. Set that configuration before departure; extra runes increase the loss. Pouch runes are always exposed and do not consume protected-item slots. Actual carried losses retain the observed contents separately.");
        }
        if (emptyBag)
        {
            notes.add("Preparation required: empty the looting bag at a bank before departure. Planned risk uses this empty target, not observed contents. Future loot adds loss; actual carried bag contents remain separately observed or unverified.");
        }
        if (etherTarget > 0)
        {
            notes.add("Planned loss assumes each Wilderness weapon holds " + etherTarget
                + " usable charges (" + (1000 + etherTarget) + " total ether including activation). Planned losses use this target, not observed loaded charges; carrying more ether raises the loss.");
            notes.add("The charge target needs owned loose ether for any shortfall after freshly observed loaded charges, plus any ether packed separately. Activation ether is already present in the charged weapon form. Unverified loaded charges do not count as preparation resources.");
        }
        if (area == null || area.getMaxLevel() < 0)
        {
            notes.add("Wilderness depth is not verified for this location. Above-level-20 death rules are used conservatively.");
        }
        else
        {
            notes.add("Assessed Wilderness levels " + area.getMinLevel() + "-" + area.getMaxLevel()
                + "; the deepest level governs untradeable losses, including travel where documented.");
        }
        if (area != null && area.isPvmUsesPvpRules())
        {
            notes.add("This location also applies PvP-style item losses to monster deaths.");
        }
        else
        {
            notes.add("A monster death without recent player damage normally uses a gravestone. These totals model a PK death, not gravestone reclaim fees.");
        }
        if (area != null && area.getEntryFee() > 0)
        {
            notes.add("Includes the full " + area.getEntryFee() + " gp entry deposit at risk; the current paid balance and kill-based reductions are not observed.");
        }
        notes.add("Loss values use RuneLite's cached prices; protection uses official game values and reviewed overrides. Unpriced items and hidden charges keep an estimate incomplete. The game's Items Kept on Death screen remains authoritative.");
        return new WildernessRisk(location.getName(), budget, plans, carried, notes, etherTarget);
    }

    public Scenario baseline(Map<Integer, Integer> items)
    {
        return calculate(items, false, false, player.getDeathContext().isSkulled(), false, "Baseline");
    }

    public Scenario baseline(Map<String, Choice> equipment, List<Choice> inventory)
    {
        boolean missing = inventory.stream().anyMatch(choice -> choice.getMissing() > 0)
            || equipment.values().stream().anyMatch(choice -> choice.getMissing() > 0);
        return calculate(quantities(equipment, inventory), false, missing,
            player.getDeathContext().isSkulled(), false, "Baseline", plannedPouchContents(inventory), plansEmptyBag(inventory));
    }

    private List<Scenario> scenarios(Map<Integer, Integer> items, boolean actual, boolean missing,
        Map<Integer, Integer> plannedPouch, boolean plannedEmptyBag)
    {
        DeathContext context = player.getDeathContext();
        if (context.isHighRisk() || context.isUltimateIronman())
        {
            return Collections.singletonList(calculate(items, actual, missing, true, false,
                context.isHighRisk() ? "High-risk world: no ordinary items protected"
                    : "Ultimate Ironman: no ordinary items protected", plannedPouch, plannedEmptyBag));
        }
        List<Scenario> result = new ArrayList<>();
        boolean skull = context.isSkulled();
        result.add(calculate(items, actual, missing, skull, false,
            skull ? "Baseline: skulled, no Protect Item" : "Baseline: unskulled, no Protect Item", plannedPouch, plannedEmptyBag));
        result.add(calculate(items, actual, missing, skull, true,
            skull ? "Conditional: skulled + Protect Item" : "Conditional: unskulled + Protect Item", plannedPouch, plannedEmptyBag));
        result.add(calculate(items, actual, missing, !skull, false,
            skull ? "Comparison: unskulled" : "Comparison: skulled", plannedPouch, plannedEmptyBag));
        result.add(calculate(items, actual, missing, !skull, true,
            skull ? "Comparison: unskulled + Protect Item" : "Comparison: skulled + Protect Item", plannedPouch, plannedEmptyBag));
        return result;
    }

    public Scenario calculate(Map<Integer, Integer> quantities, boolean actual, boolean missing,
        boolean skulled, boolean protectItem, String name)
    {
        return calculate(quantities, actual, missing, skulled, protectItem, name, null, false);
    }

    private Scenario calculate(Map<Integer, Integer> quantities, boolean actual, boolean missing,
        boolean skulled, boolean protectItem, String name, Map<Integer, Integer> plannedPouch, boolean plannedEmptyBag)
    {
        DeathContext context = player.getDeathContext();
        int slots = context.isHighRisk() || context.isUltimateIronman() ? 0
            : (skulled ? 0 : 3) + (protectItem ? 1 : 0);
        Set<String> warnings = new LinkedHashSet<>();
        if (!context.isKnown()) { warnings.add("Account skull/world restrictions are unavailable; protection is provisional."); }
        if (context.isUltimateIronman())
        {
            warnings.add("Ultimate Ironman item-loss exceptions are not fully modelled; this is not a verified loss total.");
        }
        if (protectItem && player.getLevels().getOrDefault("PRAYER", 0) < 25)
        {
            warnings.add("Protect Item requires Prayer level 25; this conditional scenario is unavailable at the observed level.");
        }
        if (missing) { warnings.add("The plan has missing or unresolved items; their full risk cannot be verified."); }
        if (!actual && usesPlannedEther(quantities))
        {
            checkEtherResources(quantities, warnings);
        }
        List<Integer> order = new ArrayList<>();
        quantities.forEach((id, count) -> {
            if (id > 0 && count != null && count > 0) { order.add(id); }
        });
        order.sort(Comparator.comparingLong((Integer id) -> protectionValue(id)).reversed().thenComparingInt(id -> id));
        Map<Integer, Integer> protectedCounts = new LinkedHashMap<>();
        int remaining = slots;
        long boundary = -1;
        for (int id : order)
        {
            DeathRule rule = rule(id);
            if (!rule.isProtectable()) { continue; }
            long value = protectionValue(id);
            if (value < 0) { warnings.add("Protection value unavailable for " + itemName(id) + "."); }
            int count = quantities.get(id);
            int kept = Math.min(count, remaining);
            protectedCounts.put(id, kept);
            remaining -= kept;
            if (kept > 0) { boundary = value; }
            if (kept < count && boundary >= 0 && value == boundary && slots > 0)
            {
                // Equal values across distinct IDs can have different break/convert outcomes.
                boolean otherId = protectedCounts.entrySet().stream().anyMatch(e -> e.getKey() != id
                    && e.getValue() > 0 && protectionValue(e.getKey()) == value);
                if (otherId) { warnings.add("Equal protection values cross the kept-item boundary; the exact tied item is not verified."); }
            }
        }
        List<ItemLoss> losses = new ArrayList<>();
        for (int id : order)
        {
            losses.add(outcome(id, quantities.get(id), protectedCounts.getOrDefault(id, 0), actual, plannedEmptyBag, warnings));
        }
        addContents(quantities, actual, plannedPouch, plannedEmptyBag, losses, warnings);
        long lostValue = 0;
        long repairs = 0;
        int permanent = 0;
        for (ItemLoss loss : losses)
        {
            lostValue = saturatedAdd(lostValue, loss.getLostValue());
            repairs = saturatedAdd(repairs, loss.getRepairCost());
            if (loss.isPermanent()) { permanent += loss.getQuantity() - loss.getProtectedQuantity(); }
        }
        return new Scenario(name, slots, lostValue, repairs, area == null ? 0 : area.getEntryFee(),
            permanent, warnings.isEmpty() && losses.stream().allMatch(ItemLoss::isKnown), losses, new ArrayList<>(warnings));
    }

    private ItemLoss outcome(int id, int quantity, int kept, boolean actual, boolean plannedEmptyBag, Set<String> warnings)
    {
        ItemValue value = player.getItemValues().get(id);
        DeathRule rule = rule(id);
        int exposed = quantity - kept;
        long lost = saturatedMultiply(rule.getAlwaysLostValue(), quantity);
        long repair = 0;
        boolean known = rule.getKind() != DeathRule.Kind.UNKNOWN;
        long alwaysComponents = componentValue(rule.getAlwaysLostItems());
        known &= alwaysComponents >= 0;
        lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, alwaysComponents), quantity));
        boolean permanent = false;
        String outcome = exposed == 0 ? "Protected" : "Lost";
        String detail = rule.getNote() == null ? "" : rule.getNote();
        if (!actual && plannedEmptyBag && "LOOTING_BAG".equals(rule.getContentsType()))
        {
            detail = "Preparation target: empty the bag at a bank before departure. Future loot adds loss; current contents are assessed separately. " + detail;
        }
        boolean etherWeapon = "ETHER_WEAPON".equals(rule.getContentsType());
        boolean etherTarget = etherWeapon && !actual && plannedEtherCharges > 0 && applies();
        if (etherTarget)
        {
            long etherPrice = price(REVENANT_ETHER);
            known &= etherPrice >= 0;
            lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, etherPrice),
                saturatedMultiply(plannedEtherCharges, quantity)));
            detail = "Planned loss assumes " + plannedEtherCharges + " usable charges plus 1,000 activation ether per weapon, all lost even when protected. Actual carried loss uses an observed balance when available.";
        }
        else if (etherWeapon)
        {
            com.danieljglover.allinslayer.model.advisor.ChargeObservation observed = actual && quantity == 1
                ? player.getPreparation().getCharges().get(id) : null;
            if (observed != null && observed.isObserved() && observed.getCharges() >= 0)
            {
                long etherPrice = price(REVENANT_ETHER);
                known &= etherPrice >= 0;
                lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, etherPrice), observed.getCharges()));
                detail = "Latest carried Check: " + observed.getCharges()
                    + " usable charges plus 1,000 activation ether, all lost even when protected.";
            }
            else
            {
                known = false;
                warnings.add(itemName(id) + ": loaded ether is unverified; the 1,000 activation ether is only the minimum loss.");
            }
        }
        else if (value != null && value.isChargesUnknown())
        {
            if (exposed == 0 && rule.isChargesKeptWhenProtected())
            {
                detail = "Stored charges are retained while this item remains protected. " + detail;
            }
            else
            {
                known = false;
                warnings.add(itemName(id) + ": stored charges or contents are not fully valued.");
            }
        }
        if (exposed > 0)
        {
            switch (rule.getKind())
            {
                case KEEP:
                    outcome = "Kept";
                    break;
                case REPAIR:
                    outcome = "Kept broken";
                    known &= rule.isRepairCostKnown();
                    repair = saturatedMultiply(rule.getRepairCost(), exposed);
                    break;
                case LOCKABLE:
                    if (aboveTwenty() && !rule.isLocked())
                    {
                        outcome = "Permanently lost";
                        permanent = true;
                        detail = "Unlocked untradeable above level 20. " + detail;
                    }
                    else
                    {
                        outcome = aboveTwenty() ? "Kept mangled" : "Kept broken";
                        known &= aboveTwenty() || rule.isRepairCostKnown();
                        repair = saturatedMultiply(aboveTwenty() ? 500000 : rule.getRepairCost(), exposed);
                        if (rule.isLocked()) { detail = "Trouver lock retained. " + detail; }
                    }
                    break;
                case POUCH:
                    outcome = rule.isLocked() ? "Pouch kept; contents lost" : "Pouch lost";
                    if (!rule.isLocked())
                    {
                        if (rule.getReplacementItems().isEmpty())
                        {
                            long price = price(id);
                            known &= price >= 0;
                            lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, price), exposed));
                        }
                        permanent = rule.isPermanentLoss();
                    }
                    break;
                case CONVERT:
                    outcome = "Converted / components lost";
                    permanent = rule.isPermanentLoss();
                    break;
                case ALWAYS_LOST:
                    outcome = "Always lost";
                    permanent = rule.isPermanentLoss();
                    // Reviewed replacement components already price the item itself. A looting
                    // bag has no gp reclaim fee; other containers still need their own valuation.
                    if (rule.getReplacementItems().isEmpty() && !"LOOTING_BAG".equals(rule.getContentsType()))
                    {
                        long price = price(id);
                        known &= price >= 0;
                        lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, price), exposed));
                    }
                    break;
                case DROP:
                    long price = price(id);
                    known &= price >= 0;
                    lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, price), exposed));
                    break;
                default:
                    outcome = "Unknown death rule";
                    known = false;
                    long estimate = price(id);
                    lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, estimate), exposed));
            }
            if (!rule.getReplacementItems().isEmpty())
            {
                long components = componentValue(rule.getReplacementItems());
                known &= components >= 0;
                lost = saturatedAdd(lost, saturatedMultiply(Math.max(0, components), exposed));
            }
            if (repair > 0 && !(rule.getKind() == DeathRule.Kind.LOCKABLE && aboveTwenty()))
            {
                long components = componentValue(rule.getRepairCostItems());
                known &= components >= 0;
                repair = saturatedAdd(repair, saturatedMultiply(Math.max(0, components), exposed));
            }
        }
        if (exposed == 0 && (lost > 0 || rule.isContentsLost()))
        {
            outcome = "Protected item; additional losses";
        }
        if (kept > 0 && exposed > 0) { detail = kept + " protected; " + exposed + " exposed. " + detail; }
        if (!known) { warnings.add(itemName(id) + ": loss estimate is incomplete."); }
        return new ItemLoss(id, itemName(id), quantity, kept, protectionValue(id), outcome,
            lost, repair, permanent, known, detail.trim());
    }

    private void addContents(Map<Integer, Integer> items, boolean actual, Map<Integer, Integer> plannedPouch,
        boolean plannedEmptyBag, List<ItemLoss> losses, Set<String> warnings)
    {
        boolean pouch = items.keySet().stream().anyMatch(id -> rule(id).getKind() == DeathRule.Kind.POUCH);
        boolean bag = items.keySet().stream().anyMatch(id -> "LOOTING_BAG".equals(rule(id).getContentsType()));
        boolean quiver = items.keySet().stream().anyMatch(id -> "QUIVER".equals(rule(id).getContentsType()));
        for (int id : items.keySet())
        {
            DeathRule rule = rule(id);
            if (rule.isContentsLost() && (rule.getContentsType() == null || rule.getContentsType().isEmpty()))
            {
                warnings.add(itemName(id) + ": contents or charges can be lost independently of item protection and are not fully observed.");
            }
        }
        DeathContext context = player.getDeathContext();
        if (quiver)
        {
            if (context.isQuiverContentsKnown())
            {
                contents(context.getQuiverContents(), "Quiver", losses, warnings);
            }
            warnings.add("Quiver ammunition protection is not verified; any observed ammunition is shown as exposed. Hidden ammunition or sunfire charges may add to this estimate.");
        }
        if (!actual && plannedPouch != null && !plannedPouch.isEmpty())
        {
            if (!pouch)
            {
                warnings.add("The rune pouch configuration has no physical pouch in the plan.");
            }
            plannedPouch.forEach((id, quantity) ->
            {
                long required = (long) quantity + items.getOrDefault(id, 0);
                if (required > player.getOwned().getOrDefault(id, 0))
                {
                    warnings.add("Rune pouch preparation needs " + required + " " + itemName(id)
                        + " including any loose runes; observed usable stock is insufficient.");
                }
            });
            contents(plannedPouch, "Planned rune pouch", losses, warnings);
        }
        else if (pouch)
        {
            boolean carriedPouch = player.getRiskCarried().keySet().stream()
                .anyMatch(id -> rule(id).getKind() == DeathRule.Kind.POUCH && items.containsKey(id));
            if (context.isPouchContentsKnown() && (actual || carriedPouch))
            {
                contents(context.getPouchContents(), "Rune pouch", losses, warnings);
            }
            else { warnings.add("Rune pouch contents are unverified; runes are not protected by its lock."); }
        }
        if (bag && (actual || !plannedEmptyBag))
        {
            // Only an explicit planned-empty target can replace observed contents. The actual
            // carried assessment and legacy map-only comparisons retain their known/unknown state.
            if (context.isLootingBagContentsKnown())
            {
                contents(context.getLootingBagContents(), "Looting bag", losses, warnings);
            }
            else { warnings.add("Looting bag contents are unverified and are outside normal item protection."); }
        }
    }

    private boolean usesPlannedEther(Map<Integer, Integer> items)
    {
        return plannedEtherCharges > 0 && applies() && items.entrySet().stream()
            .anyMatch(entry -> entry.getValue() != null && entry.getValue() > 0
                && "ETHER_WEAPON".equals(rule(entry.getKey()).getContentsType()));
    }

    private void checkEtherResources(Map<Integer, Integer> items, Set<String> warnings)
    {
        long target = 0;
        for (Map.Entry<Integer, Integer> entry : items.entrySet())
        {
            if (entry.getValue() != null && entry.getValue() > 0
                && "ETHER_WEAPON".equals(rule(entry.getKey()).getContentsType()))
            {
                com.danieljglover.allinslayer.model.advisor.ChargeObservation observed = player.getPreparation()
                    .getCharges().get(entry.getKey());
                long required = saturatedMultiply(entry.getValue(), plannedEtherCharges);
                // One observation belongs to one uniquely carried item, never every banked copy.
                if (observed != null && observed.isObserved() && observed.getCharges() >= 0)
                {
                    required -= Math.min(plannedEtherCharges, observed.getCharges());
                }
                target = saturatedAdd(target, required);
            }
        }
        long packed = Math.max(0, items.getOrDefault(REVENANT_ETHER, 0));
        long required = saturatedAdd(target, packed);
        long owned = Math.max(0, player.getOwned().getOrDefault(REVENANT_ETHER, 0));
        if (owned < required)
        {
            warnings.add("Ether preparation needs " + target + " loose ether for weapon charges"
                + (packed > 0 ? " plus " + packed + " packed separately" : "")
                + "; only " + owned + " loose ether is observed. Unverified loaded charges are not counted as preparation resources.");
        }
    }

    private void contents(Map<Integer, Integer> items, String container, List<ItemLoss> losses, Set<String> warnings)
    {
        items.forEach((id, quantity) -> {
            long price = price(id);
            boolean known = price >= 0;
            if (!known) { warnings.add(container + ": value unavailable for " + itemName(id) + "."); }
            losses.add(new ItemLoss(id, itemName(id), quantity, 0, 0, "Contents lost",
                saturatedMultiply(Math.max(0, price), quantity), 0, false, known,
                container + ": contents do not use protected slots."));
        });
    }

    public long protectionValue(int id)
    {
        DeathRule rule = catalogue.getDeathRules().get(id);
        if (rule != null && rule.getProtectionValue() >= 0) { return rule.getProtectionValue(); }
        ItemValue value = player.getItemValues().get(id);
        return value == null ? -1 : value.getProtectionValue();
    }

    public long price(int id)
    {
        ItemValue value = player.getItemValues().get(id);
        return value == null ? -1 : value.getMarketPrice();
    }

    private long componentValue(Map<Integer, Integer> components)
    {
        long total = 0;
        for (Map.Entry<Integer, Integer> component : components.entrySet())
        {
            long value = price(component.getKey());
            if (value < 0) { return -1; }
            total = saturatedAdd(total, saturatedMultiply(value, component.getValue()));
        }
        return total;
    }

    private DeathRule rule(int id)
    {
        DeathRule rule = catalogue.getDeathRules().get(id);
        if (rule != null) { return rule; }
        ItemValue value = player.getItemValues().get(id);
        DeathRule fallback = new DeathRule();
        fallback.setKind(value != null && value.isTradeable() ? DeathRule.Kind.DROP : DeathRule.Kind.UNKNOWN);
        return fallback;
    }

    private boolean aboveTwenty() { return area == null || area.getMaxLevel() < 0 || area.getMaxLevel() > 20; }

    private String itemName(int id)
    {
        ItemValue value = player.getItemValues().get(id);
        return value == null ? "Item " + id : value.getName();
    }

    public static Map<Integer, Integer> quantities(Map<String, Choice> equipment, List<Choice> inventory)
    {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        List<Choice> all = new ArrayList<>(equipment.values());
        all.addAll(inventory);
        for (Choice choice : all)
        {
            if (!"RUNE POUCH".equals(choice.getSlot()) && choice.getItemId() > 0 && choice.getQuantity() > 0)
            {
                result.merge(choice.getItemId(), choice.getQuantity(), (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
            }
        }
        return result;
    }

    private static Map<Integer, Integer> plannedPouchContents(List<Choice> inventory)
    {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (Choice choice : inventory)
        {
            if ("RUNE POUCH".equals(choice.getSlot()) && choice.getItemId() > 0 && choice.getQuantity() > 0)
            {
                result.merge(choice.getItemId(), choice.getQuantity(),
                    (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
            }
        }
        return result;
    }

    private boolean plansEmptyBag(List<Choice> inventory)
    {
        return inventory.stream().anyMatch(choice -> "LOOTING BAG".equals(choice.getSlot())
            && choice.getQuantity() > 0 && "LOOTING_BAG".equals(rule(choice.getItemId()).getContentsType()));
    }
}

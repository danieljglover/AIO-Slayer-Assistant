package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.ItemStats;
import com.danieljglover.allinslayer.model.advisor.RecommendationGoal;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.ItemDefinition;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.ItemOption;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Method;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Monster;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Task;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Supply;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Starts from Wiki priorities, then compares compatible head/amulet conditional bonuses. */
final class EquipmentPlanner
{
	static final List<String> SLOTS = Collections.unmodifiableList(Arrays.asList(
		"HEAD", "CAPE", "AMULET", "WEAPON", "BODY", "SHIELD", "LEGS", "HANDS", "FEET", "RING", "AMMO"));
	private static final List<String> SEARCH_ORDER = Arrays.asList(
		"WEAPON", "HEAD", "BODY", "LEGS", "SHIELD", "AMMO", "CAPE", "AMULET", "HANDS", "FEET", "RING");
	private static final List<String> RISK_SEARCH_ORDER = Arrays.asList(
		"HEAD", "BODY", "LEGS", "SHIELD", "AMMO", "CAPE", "AMULET", "HANDS", "FEET", "RING", "WEAPON");
	private static final int SEARCH_LIMIT = 25000;
	private final SlayerCatalogue catalogue;
	private final RecommendationRequest request;
	private final PlayerSnapshot player;
	private final RequirementEvaluator requirements;
	private final Method method;
	private final Location location;
	private final WildernessRiskCalculator riskCalculator;
	private final PreparationWork work;
	private final ConditionalBonuses bonuses;
	private final Map<String, List<Candidate>> domains = new LinkedHashMap<>();
	private final List<List<Integer>> mandatory = new ArrayList<>();
	private final List<Integer> primaryAmmo = new ArrayList<>();
	private final List<Supply> gearRequirements = new ArrayList<>();
	private final Map<Integer, List<String>> usability = new HashMap<>();
	private int visited;

	EquipmentPlanner(SlayerCatalogue catalogue, RecommendationRequest request, Task task,
		Monster monster, Location location, Method method)
	{
		this(catalogue, request, task, monster, location, method, new PreparationWork(() -> false));
	}

	EquipmentPlanner(SlayerCatalogue catalogue, RecommendationRequest request, Task task,
		Monster monster, Location location, Method method, PreparationWork work)
	{
		this.work = work;
		this.catalogue = catalogue;
		this.request = request;
		this.player = request.getPlayer();
		this.requirements = new RequirementEvaluator(request);
		this.method = method;
		this.location = location;
		this.riskCalculator = new WildernessRiskCalculator(catalogue, player, location, request.getPlannedEtherCharges());
		this.bonuses = new ConditionalBonuses(catalogue, request, task, monster, location, method);
	}

	Map<String, Choice> plan(List<Supply> required, List<String> blockers, List<String> explanations)
	{
		createDomains();
		for (Supply supply : required)
		{
			boolean ammunition = isAmmunition(supply);
			boolean protectiveGear = hasGearAlternative(supply);
			if (!supply.isRequired() || supply.getItemIds().isEmpty()
				|| !protectiveGear && !(ammunition && "RANGED".equalsIgnoreCase(method.getStyle())))
			{
				continue;
			}
			if (protectiveGear && hasOwnedInventoryAlternative(supply))
			{
				explanations.add(supply.getName()
					+ ": sufficient usable owned supplies can replace equipped protection; the inventory plan must include them when that protection is not equipped.");
				continue;
			}
			if (ammunition)
			{
				primaryAmmo.addAll(supply.getItemIds());
			}
			else
			{
				gearRequirements.add(supply);
			}
			List<Integer> usableIds = new ArrayList<>();
			for (Integer id : supply.getItemIds())
			{
				if (!isGearItem(id) && !(ammunition && "AMMO".equals(itemSlot(id))))
				{
					continue;
				}
				ItemStats stats = player.getItems().get(id);
				if (stats != null && usable(id) && owned(id))
				{
					boolean rejectedWikiOption = method.getEquipment().values().stream().flatMap(List::stream)
						.anyMatch(option -> option.getItemIds().contains(id))
						&& domains.values().stream().flatMap(List::stream)
							.noneMatch(candidate -> candidate.stats.getId() == id);
					if (rejectedWikiOption)
					{
						continue;
					}
					usableIds.add(id);
					addCandidate(stats.getSlot(), new Candidate(stats, "Required", null), true);
				}
			}
			if (usableIds.isEmpty())
			{
				blockers.add("Required equipment unavailable: " + supply.getName() + ".");
				addRelevantRequirements(supply.getItemIds().stream()
					.filter(id -> SLOTS.contains(itemSlot(id))).collect(Collectors.toList()), blockers);
			}
			else if (!ammunition)
			{
				mandatory.add(usableIds);
			}
		}
		// Empty slots are permitted, but never satisfy mandatory protective gear or a weapon.
		domains.values().forEach(options -> options.add(Candidate.EMPTY));
		Map<String, Candidate> selected = new LinkedHashMap<>();
		if (!search(0, selected))
		{
			blockers.add(visited >= SEARCH_LIMIT
				? "Could not verify a complete compatible loadout within the search limit; narrow the method or gear options."
				: "Owned equipment cannot satisfy all mandatory gear, companion, shield and ammunition constraints together.");
			selected.clear();
			for (String slot : SEARCH_ORDER)
			{
				Candidate candidate = domains.get(slot).get(0);
				if ("SHIELD".equals(slot) && stats(selected, "WEAPON") != null
					&& stats(selected, "WEAPON").isTwoHanded())
				{
					candidate = Candidate.EMPTY;
				}
				if ("AMMO".equals(slot))
				{
					candidate = domains.get(slot).stream().filter(c ->
						EquipmentCompatibility.compatible(stats(selected, "WEAPON"), c.stats))
						.findFirst().orElse(Candidate.EMPTY);
				}
				selected.put(slot, candidate);
			}
		}
		else
		{
			compareConditionalPairs(selected, explanations);
			if (riskCalculator.applies())
			{
				optimiseWilderness(selected, required, explanations);
			}
		}
		bonuses.explain(stats(selected, "HEAD"), stats(selected, "AMULET"), stats(selected, "WEAPON"), explanations);
		Map<String, Choice> result = new LinkedHashMap<>();
		for (String slot : SLOTS)
		{
			Candidate candidate = selected.getOrDefault(slot, Candidate.EMPTY);
			if (candidate.stats == null)
			{
				String reason = "Wilderness budget".equals(candidate.origin) ? "Empty - Wilderness budget"
					: "SHIELD".equals(slot) && stats(selected, "WEAPON") != null
					&& stats(selected, "WEAPON").isTwoHanded() ? "Empty - two-handed weapon" : "Empty";
				result.put(slot, new Choice(0, 0, 0, 0, 0, slot, reason, "Owned fallback", false));
				if ("WEAPON".equals(slot))
				{
					blockers.add("No owned usable weapon fits this method.");
				}
				else if ("Wilderness budget".equals(candidate.origin))
				{
					explanations.add(slot + ": left empty to meet the Wilderness loss budget; no mandatory protection is removed.");
				}
				else if (!"AMMO".equals(slot) && !"SHIELD".equals(slot))
				{
					explanations.add(slot + ": no owned usable option; left empty.");
				}
				addSlotRequirements(slot, explanations);
				continue;
			}
			ItemStats item = candidate.stats;
			int target = targetQuantity(slot, item, stats(selected, "WEAPON"), required);
			int carried = Math.min(target, player.getCarried().getOrDefault(item.getId(), 0));
			boolean requiredGear = gearRequirements.stream().anyMatch(s -> s.getItemIds().contains(item.getId()));
			result.put(slot, new Choice(item.getId(), target, carried, target - carried, 0,
				slot, item.getName(), candidate.origin, requiredGear || "WEAPON".equals(slot),
				"HEAD".equals(slot) ? headChoiceExplanation(selected) : ""));
			if (!"Wiki".equals(candidate.origin))
			{
				explanations.add(slot + ": " + item.getName() + " - " + candidate.origin
					+ ("Owned fallback".equals(candidate.origin)
						? "; highest compatible owned stat preference after Wiki options."
						: "Bonus comparison".equals(candidate.origin) ? "; selected with the amulet/head slot after applying eligible, non-stacking boosts."
						: "Wilderness budget".equals(candidate.origin) ? "; selected after assessing the full trip death risk."
						: "; earlier options were unavailable or incompatible."));
			}
			if (candidate.option != null && !candidate.option.getRequires().isEmpty())
			{
				explanations.add(item.getName() + ": required companion equipment is included in this setup.");
			}
		}
		ItemStats weapon = stats(selected, "WEAPON");
		if (!EquipmentCompatibility.compatible(weapon, stats(selected, "AMMO")))
		{
			blockers.add("No owned compatible ammunition is available for " + weapon.getName() + ".");
		}
		for (Supply supply : gearRequirements)
		{
			if (result.values().stream().noneMatch(c -> supply.getItemIds().contains(c.getItemId())))
			{
				add(blockers, "Must equip required protection or method item: " + supply.getName() + ".");
			}
		}
		if (EquipmentCompatibility.chargeBearing(weapon))
		{
			explanations.add("Check " + weapon.getName() + " charges and loaded ammunition before leaving; item ownership does not reveal internal charge quantities.");
		}
		if (!request.isActiveTask())
		{
			explanations.add("Catalogue on-task preview: eligible Slayer bonuses assume the selected task and target. Your detected assignment is unchanged.");
		}
		return result;
	}

	private String headChoiceExplanation(Map<String, Candidate> selected)
	{
		ItemStats head = stats(selected, "HEAD");
		ItemStats amulet = stats(selected, "AMULET");
		if (request.isTaskPreview() && bonuses.effect(head).isSlayer() && bonuses.applicable(head) > 1
			&& bonuses.applicable(amulet) <= 1)
		{
			return "On-task preview: this Slayer headgear's task boost is included for the selected monster. Your detected assignment is unchanged.";
		}
		boolean slayerAlternative = domains.get("HEAD").stream()
			.anyMatch(candidate -> bonuses.effect(candidate.stats).isSlayer() && bonuses.applicable(candidate.stats) > 1);
		if (slayerAlternative && !bonuses.effect(head).isSlayer() && bonuses.applicable(amulet) > 1)
		{
			return "Slayer headgear is still available. " + amulet.getName()
				+ " supplies this setup's undead boost; Slayer helmet and Black mask boosts do not stack with it. "
				+ "The head slot is compared using equipment stats and, in the Wilderness, loss constraints.";
		}
		return "";
	}


	private int targetQuantity(String slot, ItemStats item, ItemStats weapon, List<Supply> required)
	{
		int target = "AMMO".equals(slot) && EquipmentCompatibility.needsAmmo(weapon)
			|| "WEAPON".equals(slot) && item.isStackable()
			? Math.min(player.getOwned().getOrDefault(item.getId(), 0), 1000) : 1;
		if (item.isStackable())
		{
			int minimum = required.stream().filter(s -> s.isRequired() && s.getItemIds().contains(item.getId()))
				.mapToInt(Supply::getQuantity).max().orElse(1);
			target = Math.min(player.getOwned().getOrDefault(item.getId(), 0), Math.max(target, minimum));
		}
		return target;
	}

	private Map<String, Choice> previewChoices(Map<String, Candidate> selected, List<Supply> required)
	{
		Map<String, Choice> choices = new LinkedHashMap<>();
		selected.forEach((slot, candidate) -> {
			if (candidate.stats != null)
			{
				int id = candidate.stats.getId();
				int quantity = targetQuantity(slot, candidate.stats, stats(selected, "WEAPON"), required);
				int carried = Math.min(quantity, player.getCarried().getOrDefault(id, 0));
				choices.put(slot, new Choice(id, quantity, carried, quantity - carried, 0,
					slot, candidate.stats.getName(), candidate.origin, "WEAPON".equals(slot)));
			}
		});
		return choices;
	}

	private RiskPreference riskPreference(Map<String, Candidate> selected, List<Supply> required)
	{
		work.checkCancelled();
		Map<String, Choice> equipment = previewChoices(selected, required);
		List<String> inventoryProblems = new ArrayList<>();
		List<Choice> inventory = new SupplyPlanner(request, this, equipment).plan(method, location,
			required, equipment, inventoryProblems, new ArrayList<>());
		com.danieljglover.allinslayer.model.advisor.WildernessRisk.Scenario risk = riskCalculator.baseline(equipment, inventory);
		int unknown = (int) risk.getItems().stream().filter(item -> !item.isKnown()).count();
		if (!risk.isComplete()) { unknown++; }
		double preference = bonusScore(selected);
		for (Map.Entry<String, Candidate> entry : selected.entrySet())
		{
			// Wiki order remains the method's weapon/set preference; stats rank compatible downgrades.
			preference -= 8 * Math.max(0, domains.get(entry.getKey()).indexOf(entry.getValue()));
		}
		return new RiskPreference(inventoryProblems.size(), risk.getPermanentLosses(), unknown,
			Math.max(0, risk.getTotalRisk() - request.getWildernessRiskBudget()),
			weaponPreference(stats(selected, "WEAPON")), preference);
	}

	private int weaponPreference(ItemStats weapon)
	{
		List<ItemOption> options = method.getEquipment().getOrDefault("WEAPON", Collections.emptyList());
		for (int index = 0; index < options.size(); index++)
		{
			if (weapon != null && options.get(index).getItemIds().contains(weapon.getId())) { return index; }
		}
		return options.size();
	}

	private void optimiseWilderness(Map<String, Candidate> selected, List<Supply> required, List<String> explanations)
	{
		if (!work.spend())
		{
			explanations.add("The shared Wilderness comparison limit was reached. This setup's Wiki equipment is still assessed; select its monster/location to focus the alternative search.");
			return;
		}
		Map<String, Candidate> original = new LinkedHashMap<>(selected);
		RiskPreference best = riskPreference(selected, required);
		int evaluated = 0;
		// Bounded coordinate search includes supplies for each candidate. Only complete compatible
		// setups are compared, so a price improvement cannot remove mandatory protection or a set.
		for (int pass = 0; pass < 12 && evaluated < 600 && !work.exhausted(); pass++)
		{
			Map<String, Candidate> next = null;
			boolean changed = false;
			RiskPreference nextPreference = best;
			// Reduce supporting gear risk before replacing the method's preferred weapon.
			// Otherwise expensive armour can displace it from the protected item slots.
			for (String slot : RISK_SEARCH_ORDER)
			{
				Candidate previous = selected.get(slot);
				if (previous.option != null && !previous.option.getRequires().isEmpty()) { continue; }
				for (Candidate candidate : domains.get(slot))
				{
					if (candidate == previous || candidate.stats == null && "WEAPON".equals(slot)) { continue; }
					Map<String, Candidate> before = new LinkedHashMap<>(selected);
					selected.put(slot, candidate);
					if ("WEAPON".equals(slot))
					{
						if (candidate.stats.isTwoHanded()) { selected.put("SHIELD", Candidate.EMPTY); }
						if (!EquipmentCompatibility.compatible(candidate.stats, stats(selected, "AMMO")))
						{
							selected.put("AMMO", domains.get("AMMO").stream()
								.filter(ammo -> EquipmentCompatibility.compatible(candidate.stats, ammo.stats))
								.findFirst().orElse(Candidate.EMPTY));
						}
					}
					if (possible(selected) && work.spend())
					{
						RiskPreference preference = riskPreference(selected, required);
						evaluated++;
						if (preference.betterThan(nextPreference))
						{
							next = new LinkedHashMap<>(selected);
							nextPreference = preference;
						}
					}
					selected.clear();
					selected.putAll(before);
					if (evaluated >= 600 || work.exhausted()) { break; }
				}
				selected.put(slot, previous);
				if (next != null)
				{
					selected.clear();
					selected.putAll(next);
					best = nextPreference;
					changed = true;
					next = null;
				}
				if (evaluated >= 600 || work.exhausted()) { break; }
			}
			// Conditional bonus pairs may require changing both slots to reach an affordable setup.
			if (evaluated < 600 && !work.exhausted())
			{
				Candidate head = selected.get("HEAD");
				Candidate amulet = selected.get("AMULET");
				for (Candidate h : riskPairOptions("HEAD", head))
				{
					selected.put("HEAD", h);
					for (Candidate a : riskPairOptions("AMULET", amulet))
					{
						selected.put("AMULET", a);
						if (possible(selected) && work.spend())
						{
							RiskPreference preference = riskPreference(selected, required);
							evaluated++;
							if (preference.betterThan(nextPreference))
							{
								next = new LinkedHashMap<>(selected);
								nextPreference = preference;
							}
						}
						if (evaluated >= 600 || work.exhausted()) { break; }
					}
					if (evaluated >= 600 || work.exhausted()) { break; }
				}
				selected.put("HEAD", head);
				selected.put("AMULET", amulet);
			}
			if (next != null)
			{
				selected.clear();
				selected.putAll(next);
				best = nextPreference;
				changed = true;
			}
			if (!changed) { break; }
		}
		for (String slot : SLOTS)
		{
			Candidate chosen = selected.get(slot);
			if (chosen != original.get(slot))
			{
				selected.put(slot, new Candidate(chosen.stats, "Wilderness budget", chosen.option));
			}
		}
		explanations.add("Wilderness gear compared against a " + request.getWildernessRiskBudget()
			+ " gp full-trip loss budget without relying on Protect Item. Permanent untradeable losses and unknown costs are excluded from automatic readiness; mandatory gear and non-stacking combat bonuses are preserved.");
		explanations.add("Within those loss constraints, this method's Wiki weapon order takes priority over flat equipment stats. The source order accounts for encounter-specific weapons and effects such as Wilderness damage bonuses or Venator bounces; this is not a measured DPS calculation.");
		if (evaluated >= 600 || work.exhausted())
		{
			explanations.add("Wilderness gear comparison reached its work limit; this is a verified candidate where marked ready, not a proof that no better owned combination exists.");
		}
	}

	private static final class RiskPreference
	{
		private final int blockers;
		private final int permanent;
		private final int unknown;
		private final long excess;
		private final int weaponPreference;
		private final double quality;

		private RiskPreference(int blockers, int permanent, int unknown, long excess, int weaponPreference, double quality)
		{
			this.blockers = blockers;
			this.permanent = permanent;
			this.unknown = unknown;
			this.excess = excess;
			this.weaponPreference = weaponPreference;
			this.quality = quality;
		}

		private boolean betterThan(RiskPreference other)
		{
			if (blockers != other.blockers) { return blockers < other.blockers; }
			if (permanent != other.permanent) { return permanent < other.permanent; }
			if (unknown != other.unknown) { return unknown < other.unknown; }
			if (excess != other.excess) { return excess < other.excess; }
			if (weaponPreference != other.weaponPreference) { return weaponPreference < other.weaponPreference; }
			return quality > other.quality + 0.000001;
		}
	}

	WildernessRiskCalculator riskCalculator() { return riskCalculator; }
	SlayerCatalogue catalogue() { return catalogue; }

	boolean spendComparison() { return work.spend(); }

	void checkCancelled() { work.checkCancelled(); }

	boolean isGear(Supply supply)
	{
		return !supply.getItemIds().isEmpty() && supply.getItemIds().stream().allMatch(this::isGearItem);
	}

	boolean hasGearAlternative(Supply supply)
	{
		return supply.getItemIds().stream().anyMatch(this::isGearItem);
	}

	boolean isGearItem(int id)
	{
		String slot = itemSlot(id);
		return SLOTS.contains(slot) && !"AMMO".equals(slot);
	}

	boolean isInventoryItem(int id)
	{
		return "".equals(itemSlot(id));
	}

	private boolean hasOwnedInventoryAlternative(Supply supply)
	{
		long quantity = supply.getItemIds().stream().distinct()
			.filter(this::isInventoryItem).filter(id -> supplyProblems(id).isEmpty())
			.mapToLong(id -> player.getOwned().getOrDefault(id, 0)).sum();
		return quantity >= Math.max(1, supply.getQuantity());
	}

	private String itemSlot(int id)
	{
		ItemStats item = player.getItems().get(id);
		ItemDefinition definition = catalogue.getItems().get(id);
		if (item != null && SLOTS.contains(item.getSlot()))
		{
			return item.getSlot();
		}
		if (definition != null && SLOTS.contains(definition.getSlot()))
		{
			return definition.getSlot();
		}
		if (item != null && "".equals(item.getSlot())
			|| definition != null && "".equals(definition.getSlot()))
		{
			return "";
		}
		return null;
	}

	private boolean isAmmunition(Supply supply)
	{
		return !supply.getItemIds().isEmpty() && supply.getItemIds().stream()
			.allMatch(id -> "AMMO".equals(itemSlot(id)));
	}

	List<String> itemProblems(int id)
	{
		return usability.computeIfAbsent(id, key ->
		{
			List<String> problems = new ArrayList<>();
			ItemStats stats = player.getItems().get(key);
			ItemDefinition definition = catalogue.getItems().get(key);
			String name = stats != null ? stats.getName() : definition == null ? "Item " + key : definition.getName();
			if (stats == null || !stats.isUsable())
			{
				problems.add("Item usability is not verified: " + name + ".");
			}
			if ((definition == null || !definition.isRequirementsKnown())
				&& !requirements.isMet("Can equip: " + name))
			{
				problems.add("Confirm requirement: Can equip: " + name);
			}
			if (definition != null)
			{
				if (!definition.isUsable())
				{
					problems.add("Unusable item form: " + name + ".");
				}
				definition.getLevels().forEach((skill, level) -> requirements.level(skill, level, problems));
				requirements.checkAll(definition.getRequirements(), problems);
			}
			return problems;
		});
	}

	List<String> supplyProblems(int id)
	{
		ItemStats stats = player.getItems().get(id);
		if (SLOTS.contains(itemSlot(id)))
		{
			return itemProblems(id);
		}
		List<String> problems = new ArrayList<>();
		ItemDefinition definition = catalogue.getItems().get(id);
		if (!isInventoryItem(id))
		{
			problems.add("Supply type is not verified: Item " + id + ".");
		}
		if (stats != null && !stats.isUsable())
		{
			problems.add("Unusable supply form: " + stats.getName() + ".");
		}
		if (definition != null)
		{
			if (!definition.isUsable())
			{
				problems.add("Unusable supply form: " + definition.getName() + ".");
			}
			definition.getLevels().forEach((skill, level) -> requirements.level(skill, level, problems));
			requirements.checkAll(definition.getRequirements(), problems);
		}
		return problems;
	}

	private void createDomains()
	{
		SLOTS.forEach(slot -> domains.put(slot, new ArrayList<>()));
		for (String slot : SLOTS)
		{
			List<ItemOption> options = method.getEquipment().getOrDefault(slot, Collections.emptyList());
			for (int index = 0; index < options.size(); index++)
			{
				ItemOption option = options.get(index);
				List<String> optionProblems = new ArrayList<>();
				requirements.checkAll(option.getRequirements(), optionProblems);
				if (!optionProblems.isEmpty())
				{
					continue;
				}
				List<Integer> ids = new ArrayList<>(option.getItemIds());
				ids.sort(Comparator.comparingInt((Integer id) -> player.getCarried().getOrDefault(id, 0) > 0 ? 0 : 1));
				for (Integer id : ids)
				{
					ItemStats item = player.getItems().get(id);
					if (item != null && owned(id) && usable(id) && slot.equals(item.getSlot())
						&& (!"WEAPON".equals(slot) || styleCompatible(item)))
					{
						addCandidate(slot, new Candidate(item, index == 0 ? "Wiki" : "Wiki alternative", option), false);
					}
				}
			}
		}
		// A companion can be necessary even when its own Wiki row was not compiled.
		Set<Integer> companionIds = method.getEquipment().values().stream().flatMap(List::stream)
			.flatMap(option -> option.getRequires().stream()).flatMap(List::stream).collect(Collectors.toSet());
		Set<Integer> wikiIds = method.getEquipment().values().stream().flatMap(List::stream)
			.flatMap(option -> option.getItemIds().stream()).collect(Collectors.toSet());
		for (Integer id : companionIds)
		{
			ItemStats item = player.getItems().get(id);
			if (item != null && usable(id) && owned(id) && !wikiIds.contains(id))
			{
				addCandidate(item.getSlot(), new Candidate(item, "Required", null), false);
			}
		}
		List<ItemStats> fallback = player.getItems().values().stream()
			.filter(item -> item != null && SLOTS.contains(item.getSlot()) && owned(item.getId())
				&& usable(item.getId()) && styleCompatible(item))
			.sorted(Comparator.comparingDouble(this::fallbackScore).reversed()
				.thenComparing(ItemStats::getName).thenComparingInt(ItemStats::getId))
			.collect(Collectors.toList());
		for (ItemStats item : fallback)
		{
			// Do not bypass a method-specific requirement/dependency via the same item's fallback.
			if (!wikiIds.contains(item.getId()))
			{
				addCandidate(item.getSlot(), new Candidate(item, "Owned fallback", null), false);
			}
		}
	}

	private boolean search(int index, Map<String, Candidate> selected)
	{
		if (++visited > SEARCH_LIMIT)
		{
			return false;
		}
		if (index == SEARCH_ORDER.size())
		{
			return stats(selected, "WEAPON") != null;
		}
		String slot = SEARCH_ORDER.get(index);
		for (Candidate candidate : domains.get(slot))
		{
			selected.put(slot, candidate);
			if (possible(selected) && search(index + 1, selected))
			{
				return true;
			}
		}
		selected.remove(slot);
		return false;
	}

	private void compareConditionalPairs(Map<String, Candidate> selected, List<String> explanations)
	{
		boolean available = domains.get("HEAD").stream().anyMatch(c -> bonuses.applicable(c.stats) > 1)
			|| domains.get("AMULET").stream().anyMatch(c -> bonuses.applicable(c.stats) > 1);
		if (!available)
		{
			return;
		}
		Candidate originalHead = selected.get("HEAD");
		Candidate originalAmulet = selected.get("AMULET");
		Candidate bestHead = originalHead;
		Candidate bestAmulet = originalAmulet;
		double best = bonusScore(selected);
		for (Candidate head : pairOptions("HEAD", originalHead))
		{
			selected.put("HEAD", head);
			for (Candidate amulet : pairOptions("AMULET", originalAmulet))
			{
				selected.put("AMULET", amulet);
				if (!possible(selected))
				{
					continue;
				}
				double score = bonusScore(selected);
				// Keep the original Wiki/carried priority when scores tie.
				if (score > best + 0.000001)
				{
					best = score;
					bestHead = head;
					bestAmulet = amulet;
				}
			}
		}
		selected.put("HEAD", compared(bestHead, originalHead));
		selected.put("AMULET", compared(bestAmulet, originalAmulet));
		explanations.add("Head and amulet compared together using whole-loadout offensive stats and one applicable Slayer/Salve boost. This is a relative stat estimate, not a DPS calculation; mandatory equipment and set dependencies are retained.");
	}

	private List<Candidate> riskPairOptions(String slot, Candidate original)
	{
		return original.option != null && !original.option.getRequires().isEmpty()
			? Collections.singletonList(original) : domains.get(slot);
	}

	private List<Candidate> pairOptions(String slot, Candidate original)
	{
		// Keep method-defining set/effect choices, including Void headgear and companion necklaces.
		// Their effects are outside this comparison; removing the option would erase its dependencies.
		if (original.option != null && !original.option.getRequires().isEmpty())
		{
			return Collections.singletonList(original);
		}
		return domains.get(slot).stream().filter(c -> c.stats != null || original.stats == null)
			.collect(Collectors.toList());
	}

	private Candidate compared(Candidate selected, Candidate original)
	{
		return selected == original || selected.stats == null ? selected
			: new Candidate(selected.stats, "Bonus comparison", selected.option);
	}

	private double bonusScore(Map<String, Candidate> selected)
	{
		double attack = 0;
		double strength = 0;
		double utility = 0;
		for (Candidate candidate : selected.values())
		{
			if (candidate.stats != null)
			{
				attack += candidate.stats.attackFor(method.getStyle());
				strength += candidate.stats.strengthFor(method.getStyle());
				utility += utilityScore(candidate.stats);
			}
		}
		double multiplier = bonuses.rankingMultiplier(stats(selected, "HEAD"), stats(selected, "AMULET"), stats(selected, "WEAPON"));
		// The bonus affects the whole attack, not just the helmet/amulet's own flat stats.
		// Offsets retain the underlying attack/damage when gear has zero offensive bonuses.
		double damageBase = "MAGIC".equalsIgnoreCase(method.getStyle()) ? 100 : 64;
		return (Math.max(0, strength + damageBase) * 4 + Math.max(0, attack + 64)) * multiplier + utility;
	}

	private boolean possible(Map<String, Candidate> selected)
	{
		ItemStats weapon = stats(selected, "WEAPON");
		if (selected.containsKey("WEAPON") && weapon == null)
		{
			return false;
		}
		if (weapon != null && weapon.isTwoHanded() && stats(selected, "SHIELD") != null)
		{
			return false;
		}
		if (selected.containsKey("AMMO") && !EquipmentCompatibility.compatible(weapon, stats(selected, "AMMO")))
		{
			return false;
		}
		if (!selected.containsKey("AMMO") && weapon != null && domains.get("AMMO").stream()
			.noneMatch(candidate -> EquipmentCompatibility.compatible(weapon, candidate.stats)))
		{
			return false;
		}
		List<List<Integer>> groups = new ArrayList<>(mandatory);
		if (!primaryAmmo.isEmpty() && EquipmentCompatibility.needsAmmo(weapon))
		{
			groups.add(primaryAmmo);
		}
		selected.values().stream().filter(c -> c.option != null)
			.forEach(c -> groups.addAll(c.option.getRequires()));
		for (List<Integer> group : groups)
		{
			boolean satisfiable = selected.values().stream()
				.anyMatch(c -> c.stats != null && group.contains(c.stats.getId()));
			if (!satisfiable)
			{
				satisfiable = domains.entrySet().stream().filter(e -> !selected.containsKey(e.getKey()))
					.filter(e -> weapon == null || !weapon.isTwoHanded() || !"SHIELD".equals(e.getKey()))
					.flatMap(e -> e.getValue().stream())
					.anyMatch(c -> c.stats != null && group.contains(c.stats.getId()));
			}
			if (!satisfiable)
			{
				return false;
			}
		}
		return true;
	}

	private boolean styleCompatible(ItemStats item)
	{
		String style = method.getStyle() == null ? "MELEE" : method.getStyle().toUpperCase(Locale.ROOT);
		if ("AMMO".equals(item.getSlot()))
		{
			return true;
		}
		if ("WEAPON".equals(item.getSlot()))
		{
			return style.equals(item.getStyle()) || "ANY".equals(item.getStyle()) && "MELEE".equals(style);
		}
		if (bonuses.applicable(item) > 1)
		{
			return true;
		}
		return style.equals(item.getStyle()) || "ANY".equals(item.getStyle());
	}

	private double fallbackScore(ItemStats item)
	{
		return item.strengthFor(method.getStyle()) * 4 + item.attackFor(method.getStyle()) + utilityScore(item);
	}

	private double utilityScore(ItemStats item)
	{
		double prayerWeight = request.getGoal() == RecommendationGoal.LOW_EFFORT ? 5 : 1;
		double defenceWeight = request.getGoal() == RecommendationGoal.LOW_EFFORT ? 0.4 : 0.05;
		return item.getPrayer() * prayerWeight
			+ item.getDefence() * defenceWeight;
	}

	private void addCandidate(String slot, Candidate candidate, boolean first)
	{
		List<Candidate> options = domains.get(slot);
		if (options == null || options.stream().anyMatch(c -> c.stats != null && c.stats.getId() == candidate.stats.getId()))
		{
			return;
		}
		options.add(first ? 0 : options.size(), candidate);
	}

	private boolean usable(int id)
	{
		return itemProblems(id).isEmpty();
	}

	private boolean owned(int id)
	{
		return player.getOwned().getOrDefault(id, 0) > 0;
	}

	private void addRelevantRequirements(List<Integer> ids, List<String> target)
	{
		for (Integer id : ids)
		{
			if (owned(id))
			{
				itemProblems(id).forEach(problem -> add(target, problem));
			}
		}
	}

	private void addSlotRequirements(String slot, List<String> target)
	{
		for (ItemOption option : method.getEquipment().getOrDefault(slot, Collections.emptyList()))
		{
			if (option.getItemIds().stream().anyMatch(this::owned))
			{
				requirements.checkAll(option.getRequirements(), target);
				addRelevantRequirements(option.getItemIds(), target);
			}
		}
		player.getItems().values().stream().filter(item -> item != null && slot.equals(item.getSlot())
			&& owned(item.getId()) && styleCompatible(item))
			.sorted(Comparator.comparingDouble(this::fallbackScore).reversed().thenComparing(ItemStats::getName))
			.limit(5).forEach(item -> itemProblems(item.getId()).forEach(problem -> add(target, problem)));
	}

	private static ItemStats stats(Map<String, Candidate> selected, String slot)
	{
		Candidate candidate = selected.get(slot);
		return candidate == null ? null : candidate.stats;
	}

	private static void add(List<String> target, String value)
	{
		if (!target.contains(value))
		{
			target.add(value);
		}
	}

	private static final class Candidate
	{
		private static final Candidate EMPTY = new Candidate(null, "Owned fallback", null);
		private final ItemStats stats;
		private final String origin;
		private final ItemOption option;

		private Candidate(ItemStats stats, String origin, ItemOption option)
		{
			this.stats = stats;
			this.origin = origin;
			this.option = option;
		}
	}
}

package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.ItemStats;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Method;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Supply;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk.Scenario;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Packs required items first, then switches, travel, and supplies, with a strict 28-slot limit. */
final class SupplyPlanner
{
	private static final int CAPACITY = 28;
	private static final int MAX_SUPPLY_REPLANS = 32;
	private final RecommendationRequest request;
	private final PlayerSnapshot player;
	private final long riskBudget;
	private final EquipmentPlanner equipmentPlanner;
	private final RequirementEvaluator requirements;
	private final Map<Integer, Integer> available = new HashMap<>();
	private final Map<Integer, Integer> carried = new HashMap<>();
	private final Map<Integer, Allocation> allocations = new LinkedHashMap<>();
	private final List<Choice> unresolved = new ArrayList<>();
	private final Map<String, String> forcedFamilies;
	private final boolean collectAlternatives;
	private final boolean compareContainers;
	private final boolean omitOptionalPouch;
	private final boolean omitOptionalBag;
	private final Map<String, List<String>> alternatives = new LinkedHashMap<>();
	private final Map<String, Fulfillment> fulfilled = new LinkedHashMap<>();
	private final Map<String, Coverage> coverage = new LinkedHashMap<>();
	private int slots;
	private int capacity = CAPACITY;
	private boolean allowBlighted;
	private final TripPreparationData preparation;

	SupplyPlanner(RecommendationRequest request, EquipmentPlanner equipmentPlanner,
		Map<String, Choice> equipment)
	{
		this(request, equipmentPlanner, equipment, java.util.Collections.emptyMap(), false);
	}

	private SupplyPlanner(RecommendationRequest request, EquipmentPlanner equipmentPlanner,
		Map<String, Choice> equipment, Map<String, String> forcedFamilies, boolean collectAlternatives)
	{
		this(request, equipmentPlanner, equipment, forcedFamilies, collectAlternatives, true, false, false);
	}

	private SupplyPlanner(RecommendationRequest request, EquipmentPlanner equipmentPlanner,
		Map<String, Choice> equipment, Map<String, String> forcedFamilies, boolean collectAlternatives,
		boolean compareContainers, boolean omitOptionalPouch, boolean omitOptionalBag)
	{
		this.request = request;
		this.player = request.getPlayer();
		this.riskBudget = request.getWildernessRiskBudget();
		this.equipmentPlanner = equipmentPlanner;
		this.requirements = new RequirementEvaluator(request);
		this.preparation = equipmentPlanner.catalogue().getPreparation();
		this.forcedFamilies = new LinkedHashMap<>(forcedFamilies);
		this.collectAlternatives = collectAlternatives;
		this.compareContainers = compareContainers;
		this.omitOptionalPouch = omitOptionalPouch;
		this.omitOptionalBag = omitOptionalBag;
		available.putAll(player.getOwned());
		carried.putAll(player.getCarried());
		if (player.getDeathContext().isPouchContentsKnown() && preparation.getPouches().stream()
			.anyMatch(pouch -> player.getCarried().getOrDefault(pouch.getItemId(), 0) > 0))
		{
			player.getDeathContext().getPouchContents().forEach((id, quantity) ->
				carried.merge(id, quantity, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b)));
		}
		for (Choice choice : equipment.values())
		{
			if (choice.getItemId() > 0)
			{
				available.computeIfPresent(choice.getItemId(), (id, count) -> Math.max(0, count - choice.getQuantity()));
				carried.computeIfPresent(choice.getItemId(), (id, count) -> Math.max(0, count - choice.getCarried()));
			}
		}
	}

	List<Choice> planOptimized(Method method, Location location, List<Supply> required,
		Map<String, Choice> equipment, List<String> blockers, List<String> explanations)
	{
		if (!equipmentPlanner.riskCalculator().applies())
		{
			return plan(method, location, required, equipment, blockers, explanations);
		}
		Attempt initial = attempt(method, location, required, equipment, java.util.Collections.emptyMap());
		Attempt best = initial;
		int replans = 0;
		boolean workAvailable = true;
		java.util.Set<Map<String, String>> seen = new java.util.HashSet<>();
		seen.add(java.util.Collections.emptyMap());
		while (workAvailable && replans < MAX_SUPPLY_REPLANS && !Thread.currentThread().isInterrupted() && !withinBudget(best.risk))
		{
			Attempt improved = best;
			// A complete trip is rebuilt for each choice: later runes, food and ammunition can
			// push an otherwise affordable early supply over budget or change protected items.
			comparison: for (Map.Entry<String, List<String>> group : best.planner.alternatives.entrySet())
			{
				for (String family : group.getValue())
				{
					if (replans >= MAX_SUPPLY_REPLANS || Thread.currentThread().isInterrupted()) { break; }
					String current = best.planner.forcedFamilies.getOrDefault(group.getKey(), group.getValue().get(0));
					if (family.equals(current)) { continue; }
					Map<String, String> forced = new LinkedHashMap<>(best.planner.forcedFamilies);
					forced.put(group.getKey(), family);
					if (!seen.add(forced)) { continue; }
					if (!equipmentPlanner.spendComparison())
					{
						workAvailable = false;
						break comparison;
					}
					replans++;
					Attempt candidate = attempt(method, location, required, equipment, forced);
					if (preserves(initial, candidate) && compare(candidate.risk, improved.risk) < 0)
					{
						improved = candidate;
						if (withinBudget(improved.risk)) { break; }
					}
				}
				if (withinBudget(improved.risk)) { break; }
			}
			if (improved == best) { break; }
			best = improved;
		}
		blockers.addAll(best.blockers);
		explanations.addAll(best.explanations);
		if (best != initial)
		{
			explanations.add("Owned supply alternatives were compared against the completed trip's Wilderness loss, preserving planned quantities and visible potion doses.");
		}
		if (!withinBudget(best.risk))
		{
			explanations.add("The bounded supply comparison did not find a complete inventory within the Wilderness loss budget.");
		}
		return best.inventory;
	}

	private Attempt attempt(Method method, Location location, List<Supply> required,
		Map<String, Choice> equipment, Map<String, String> forced)
	{
		equipmentPlanner.checkCancelled();
		SupplyPlanner planner = new SupplyPlanner(request, equipmentPlanner, equipment, forced, true);
		List<String> blockers = new ArrayList<>();
		List<String> explanations = new ArrayList<>();
		List<Choice> inventory = planner.plan(method, location, required, equipment, blockers, explanations);
		Scenario risk = equipmentPlanner.riskCalculator().baseline(equipment, inventory);
		return new Attempt(planner, inventory, blockers, explanations, risk);
	}

	private boolean withinBudget(Scenario scenario)
	{
		return scenario.isComplete() && scenario.getPermanentLosses() == 0 && scenario.getTotalRisk() <= riskBudget;
	}

	private int compare(Scenario first, Scenario second)
	{
		int order = Integer.compare(first.getPermanentLosses(), second.getPermanentLosses());
		if (order == 0) { order = Boolean.compare(second.isComplete(), first.isComplete()); }
		if (order == 0) { order = Long.compare(Math.max(0, first.getTotalRisk() - riskBudget), Math.max(0, second.getTotalRisk() - riskBudget)); }
		return order;
	}

	private static boolean preserves(Attempt original, Attempt candidate)
	{
		if (!original.blockers.containsAll(candidate.blockers)) { return false; }
		for (Map.Entry<String, Fulfillment> entry : original.planner.fulfilled.entrySet())
		{
			Fulfillment next = candidate.planner.fulfilled.get(entry.getKey());
			if (next == null || next.quantity < entry.getValue().quantity || next.doses < entry.getValue().doses)
			{
				return false;
			}
		}
		return true;
	}

	List<Choice> plan(Method method, Location location, List<Supply> required,
		Map<String, Choice> equipment, List<String> blockers, List<String> explanations)
	{
		List<String> initialBlockers = new ArrayList<>();
		List<String> initialExplanations = new ArrayList<>();
		List<Choice> initialInventory = pack(method, location, required, equipment, initialBlockers, initialExplanations);
		Attempt best = new Attempt(this, initialInventory, initialBlockers, initialExplanations, null);
		if (compareContainers && equipmentPlanner.riskCalculator().applies()
			&& (hasOptionalContainer(false) || hasOptionalContainer(true)))
		{
			best = new Attempt(this, initialInventory, initialBlockers, initialExplanations,
				equipmentPlanner.riskCalculator().baseline(equipment, initialInventory));
			// Storage earns its place only against the completed trip. Rebuild once per
			// optional container so freed slots are refilled and rejected notes cannot leak.
			for (boolean pouch : new boolean[] {false, true})
			{
				if (withinBudget(best.risk) || !best.planner.hasOptionalContainer(pouch)) { continue; }
				if (!equipmentPlanner.spendComparison()) { break; }
				equipmentPlanner.checkCancelled();
				SupplyPlanner candidate = new SupplyPlanner(request, equipmentPlanner, equipment,
					forcedFamilies, collectAlternatives, false,
					pouch || best.planner.omitOptionalPouch, !pouch || best.planner.omitOptionalBag);
				List<String> candidateBlockers = new ArrayList<>();
				List<String> candidateExplanations = new ArrayList<>();
				List<Choice> inventory = candidate.pack(method, location, required, equipment,
					candidateBlockers, candidateExplanations);
				Scenario risk = equipmentPlanner.riskCalculator().baseline(equipment, inventory);
				if (best.blockers.containsAll(candidateBlockers) && compare(risk, best.risk) < 0)
				{
					best = new Attempt(candidate, inventory, candidateBlockers, candidateExplanations, risk);
				}
			}
		}
		if (best.planner != this)
		{
			alternatives.clear(); alternatives.putAll(best.planner.alternatives);
			coverage.clear(); coverage.putAll(best.planner.coverage);
			fulfilled.clear(); fulfilled.putAll(best.planner.fulfilled);
		}
		blockers.addAll(best.blockers);
		explanations.addAll(best.explanations);
		return best.inventory;
	}

	private boolean hasOptionalContainer(boolean pouch)
	{
		return allocations.values().stream().anyMatch(allocation -> !allocation.required && allocation.quantity > 0
			&& (pouch ? pouchItem(allocation.id) : preparation.getLootingBagIds().contains(allocation.id)));
	}

	private List<Choice> pack(Method method, Location location, List<Supply> required,
		Map<String, Choice> equipment, List<String> blockers, List<String> explanations)
	{
		allowBlighted = location.isWilderness();
		SpellPlanner.CastingPlan casting = new SpellPlanner(preparation, request).plan(method, equipment, allowBlighted);
		blockers.addAll(casting.getBlockers());
		casting.getNotes().forEach(note -> explanations.add("Casting: " + note));
		TripTravelPlanner.TravelPlan travel = new TripTravelPlanner(equipmentPlanner.catalogue(), request)
			.plan(location, equipment, casting.getSupplies());
		blockers.addAll(travel.getBlockers());
		explanations.addAll(travel.getNotes());
		List<Need> needs = new ArrayList<>();
		for (Supply supply : required)
		{
			if (casting.isManaged() && castingResource(supply)) { continue; }
			if (supply.isRequired() && equipmentPlanner.isGear(supply)
				&& supply.getQuantity() <= 1)
			{
				// Merely carrying mandatory protection must not satisfy the equipment gate.
				continue;
			}
			needs.add(new Need(supply, "Required", "SUPPLY", supply.isRequired()));
		}
		for (Supply supply : method.getSwitches())
		{
			if (!waived(supply))
			{
				needs.add(new Need(supply, "Wiki", "SWITCH", supply.isRequired()));
			}
		}
		Choice shield = equipment.get("SHIELD");
		if (shield != null && shield.getItemId() > 0 && method.getSwitches().stream()
			.flatMap(supply -> supply.getItemIds().stream()).map(player.getItems()::get)
			.anyMatch(item -> item != null && item.isTwoHanded() && available.getOrDefault(item.getId(), 0) > 0))
		{
			capacity--;
			explanations.add("One inventory slot is reserved for the shield displaced by a two-handed weapon switch.");
		}
		for (Supply supply : location.getTravel())
		{
			if (!waived(supply))
			{
				needs.add(new Need(supply, "Wiki", "TRAVEL", supply.isRequired()));
			}
		}
		for (Supply supply : method.getInventory())
		{
			if (casting.isManaged() && castingResource(supply)) { continue; }
			if (!waived(supply))
			{
				needs.add(new Need(supply, "Wiki", "SUPPLY", supply.isRequired()));
			}
		}
		List<Supply> castingResources = new ArrayList<>(casting.getSupplies());
		castingResources.addAll(travel.getRunes());
		for (Supply resource : aggregateCasting(castingResources))
		{
			needs.add(0, new Need(resource, "Casting", "CASTING", true));
		}
		for (Supply resource : travel.getSupplies())
		{
			needs.add(0, new Need(resource, "Trip preparation", "TRAVEL", true));
		}
		for (Need need : needs.stream().filter(value -> value.required).collect(Collectors.toList()))
		{
			allocate(need, equipment, blockers, explanations);
		}
		if (method.isCannon())
		{
			ensureCannon(equipment, blockers, explanations);
		}
		validateSwitchAmmunition(equipment, blockers, explanations);
		packRunePouch(equipment, explanations);
		packLootingBag(equipment, blockers, explanations);
		for (Need need : needs.stream().filter(value -> !value.required).collect(Collectors.toList()))
		{
			allocate(need, equipment, blockers, explanations);
		}
		validateSwitchAmmunition(equipment, blockers, explanations);
		addOwnedBasics(equipment, blockers, explanations);
		if (collectAlternatives) { captureFulfillment(); }
		List<Choice> result = choices();
		result.addAll(unresolved);
		explanations.add("Inventory: " + slots + "/28 planned slots. Bank quantities come from the latest observed bank; doses and internal charges are not inferred.");
		return result;
	}

	private void allocate(Need need, Map<String, Choice> equipment, List<String> blockers,
		List<String> explanations)
	{
		if (!need.required && !need.supply.getItemIds().isEmpty())
		{
			boolean pouch = need.supply.getItemIds().stream().allMatch(this::pouchItem);
			boolean bag = need.supply.getItemIds().stream().allMatch(preparation.getLootingBagIds()::contains);
			// Optional source pouch rows are handled by the compression pass. A second
			// source form must not add another physical pouch after that pass.
			if (pouch || bag && (omitOptionalBag || !allowBlighted
				|| allocations.keySet().stream().anyMatch(preparation.getLootingBagIds()::contains)))
			{
				return;
			}
		}
		Supply supply = blightedAlternatives(need.supply);
		int target = Math.max(1, supply.getQuantity());
		boolean mixedProtection = need.required && !"SWITCH".equals(need.slot)
			&& equipmentPlanner.hasGearAlternative(supply) && !equipmentPlanner.isGear(supply);
		Choice equipped = equipment.values().stream()
			.filter(choice -> choice.getItemId() > 0 && choice.getQuantity() > 0
				&& supply.getItemIds().contains(choice.getItemId()))
			.findFirst().orElse(null);
		if (equipped != null)
		{
			if (mixedProtection && equipmentPlanner.isGearItem(equipped.getItemId())
				|| target <= equipped.getQuantity() || "SWITCH".equals(need.slot))
			{
				return;
			}
			target -= equipped.getQuantity();
		}
		if (supply.getItemIds().isEmpty())
		{
			String message = "No verified item IDs for " + supply.getName() + "; ownership cannot be checked.";
			add(need.required ? blockers : explanations, message);
			if (need.required)
			{
				unresolved.add(new Choice(0, 0, 0, 0, target, need.slot,
					supply.getName(), need.origin, true));
			}
			return;
		}
		List<Integer> usable = supply.getItemIds().stream()
			// A shield in the bag provides no protection while a two-handed weapon is equipped.
			.filter(id -> !mixedProtection || equipmentPlanner.isInventoryItem(id))
			.filter(id -> allowBlighted || !blightedItem(id))
			.filter(id -> "Trip preparation".equals(need.origin) || equipmentPlanner.supplyProblems(id).isEmpty()).collect(Collectors.toList());
		preferFullContainers(usable);
		if ("Owned fallback".equals(need.origin))
		{
			// The existing fallback policy prefers full potions across restoration families,
			// including when only one full potion of the first family is owned.
			usable.sort(Comparator.comparingInt((Integer id) ->
			{
				ItemStats item = player.getItems().get(id);
				return item == null ? 0 : -doses(item.getName());
			}));
		}
		// Fallbacks take part in the final full-trip comparison without multiplying work in
		// the equipment search, which calls the inexpensive plan path for many candidates.
		if (!"Owned fallback".equals(need.origin)) { preferAffordableAlternatives(usable, target, equipment); }
		if (collectAlternatives) { preferForcedFamily(need.key(), usable); }
		int previous = usable.stream().distinct().map(allocations::get)
			.filter(value -> value != null).mapToInt(value -> value.quantity).sum();
		int missing = Math.max(0, target - previous);
		boolean capacityLimited = false;
		boolean pouchLimited = false;
		Allocation last = null;
		for (Integer id : new LinkedHashSet<>(usable))
		{
			ItemStats stats = player.getItems().get(id);
			boolean stackable = stats == null ? supply.isStackable() : stats.isStackable();
			Allocation existing = allocations.get(id);
			int owned = available.getOrDefault(id, 0);
			int room = stackable ? existing != null || slots < capacity ? missing : 0 : capacity - slots;
			int quantity = Math.min(missing, Math.min(owned, room));
			if (existing != null && "RUNE POUCH".equals(existing.slot))
			{
				int pouchRoom = Math.max(0, plannedPouchLimit() - existing.quantity);
				pouchLimited |= quantity > pouchRoom;
				quantity = Math.min(quantity, pouchRoom);
			}
			capacityLimited |= room < missing && owned > room;
			if (quantity > 0)
			{
				if (existing == null)
				{
					existing = new Allocation(id, stats == null ? supply.getName() : stats.getName(),
						need.slot, need.origin, stackable);
					allocations.put(id, existing);
				}
				slots += stackable ? existing.quantity == 0 ? 1 : 0 : quantity;
				existing.quantity += quantity;
				missing -= quantity;
				available.put(id, owned - quantity);
			}
			if (existing != null)
			{
				last = existing;
				existing.required |= need.required;
				if (need.required)
				{
					existing.origin = "Required";
				}
			}
		}
		if (collectAlternatives) { recordFulfillment(need.key(), usable, target); }
		if (missing > 0)
		{
			String reason = pouchLimited ? "the rune pouch's per-rune capacity" : capacityLimited
				? "the 28-slot inventory limit" : "observed ownership or item usability";
			String message = supply.getName() + ": " + (target - missing) + "/" + target
				+ " available in the plan; limited by " + reason + ".";
			add(need.required ? blockers : explanations, message);
			for (Integer id : supply.getItemIds())
			{
				if (!usable.contains(id) && player.getOwned().getOrDefault(id, 0) > 0)
				{
					equipmentPlanner.supplyProblems(id).forEach(problem -> add(need.required ? blockers : explanations, problem));
				}
			}
			if (need.required && last != null)
			{
				last.missing = Math.max(last.missing, missing);
			}
			else if (need.required)
			{
				unresolved.add(new Choice(supply.getItemIds().get(0), 0, 0, 0, missing,
					need.slot, supply.getName(), "Required", true));
			}
		}
	}

	private void ensureCannon(Map<String, Choice> equipment, List<String> blockers, List<String> explanations)
	{
		String[] names = {"Cannon base", "Cannon stand", "Cannon barrels", "Cannon furnace"};
		int[] ids = {6, 8, 10, 12};
		for (int index = 0; index < ids.length; index++)
		{
			Supply supply = supply(names[index], java.util.Collections.singletonList(ids[index]), 1, true, false);
			allocate(new Need(supply, "Required", "SUPPLY", true), equipment, blockers, explanations);
		}
		if (allocations.values().stream().noneMatch(a -> a.name.toLowerCase(Locale.ROOT).contains("cannonball")))
		{
			int ownedBalls = (int) Math.min(1000L, (long) available.getOrDefault(2, 0)
				+ available.getOrDefault(21726, 0));
			Supply balls = supply("Cannonballs", java.util.Arrays.asList(2, 21726), Math.max(1, ownedBalls), true, true);
			allocate(new Need(balls, "Required", "SUPPLY", true), equipment, blockers, explanations);
			explanations.add("Cannonballs use up to 1,000 observed owned shots when no source quantity is specified; adjust for trip length.");
		}
	}

	private void preferAffordableAlternatives(List<Integer> ids, int quantity, Map<String, Choice> equipment)
	{
		WildernessRiskCalculator calculator = equipmentPlanner.riskCalculator();
		if (!calculator.applies() || ids.size() < 2) { return; }
		Map<String, com.danieljglover.allinslayer.model.advisor.WildernessRisk.Scenario> scores = new LinkedHashMap<>();
		List<Choice> base = choices();
		// Compare each source alternative at the fullest owned dose; never prefer a nearly empty
		// potion merely because its unit price is lower. Stable sorting retains Wiki preference ties.
		for (Integer id : ids)
		{
			if (available.getOrDefault(id, 0) < quantity || scores.containsKey(containerFamily(id))) { continue; }
			List<Choice> proposed = new ArrayList<>(base);
			proposed.add(new Choice(id, quantity, 0, quantity, 0, "SUPPLY", "Supply alternative", "Comparison", false));
			scores.put(containerFamily(id), calculator.baseline(equipment, proposed));
		}
		ids.sort(Comparator.comparingInt((Integer id) -> scores.containsKey(containerFamily(id))
			? scores.get(containerFamily(id)).getPermanentLosses() : Integer.MAX_VALUE)
			.thenComparingInt(id -> scores.containsKey(containerFamily(id))
				&& scores.get(containerFamily(id)).isComplete() ? 0 : 1)
			.thenComparingLong(id -> scores.containsKey(containerFamily(id))
				? Math.max(0, scores.get(containerFamily(id)).getTotalRisk() - riskBudget) : Long.MAX_VALUE));
	}

	private void preferFullContainers(List<Integer> ids)
	{
		// Wiki item IDs are not necessarily ordered by dose. Preserve the source's order
		// between different supplies, but prefer a fuller visible form of the same item.
		Map<String, Integer> order = new LinkedHashMap<>();
		for (int id : ids)
		{
			order.putIfAbsent(containerFamily(id), order.size());
		}
		ids.sort(Comparator.comparingInt((Integer id) -> order.get(containerFamily(id)))
			.thenComparingInt(id ->
			{
				ItemStats item = player.getItems().get(id);
				return item == null ? 0 : -doses(item.getName());
			}));
	}

	private void preferForcedFamily(String key, List<Integer> ids)
	{
		List<String> families = ids.stream().filter(id -> available.getOrDefault(id, 0) > 0)
			.map(this::containerFamily).distinct().collect(Collectors.toList());
		if (families.size() > 1) { alternatives.put(key, families); }
		String forced = forcedFamilies.get(key);
		if (forced != null)
		{
			// Move the family as a unit so four-dose forms still precede one-dose forms.
			ids.sort(Comparator.comparingInt(id -> forced.equals(containerFamily(id)) ? 0 : 1));
		}
	}

	private void recordFulfillment(String key, List<Integer> ids, int target)
	{
		Coverage existing = coverage.get(key);
		if (existing == null || existing.target < target)
		{
			coverage.put(key, new Coverage(new ArrayList<>(new LinkedHashSet<>(ids)), target));
		}
	}

	private void captureFulfillment()
	{
		for (Map.Entry<String, Coverage> entry : coverage.entrySet())
		{
			int quantity = 0;
			long doseCount = 0;
			List<Integer> ids = new ArrayList<>(entry.getValue().ids);
			preferFullContainers(ids);
			for (int id : ids)
			{
				Allocation allocation = allocations.get(id);
				if (allocation == null) { continue; }
				int count = Math.min(allocation.quantity, entry.getValue().target - quantity);
				quantity += count;
				doseCount += (long) count * doses(allocation.name);
			}
			fulfilled.put(entry.getKey(), new Fulfillment(quantity, doseCount));
		}
	}

	private String containerFamily(int id)
	{
		ItemStats item = player.getItems().get(id);
		return item == null || doses(item.getName()) == 0 ? "item:" + id
			: item.getName().toLowerCase(Locale.ROOT).replaceFirst("\\s*\\([1-9]\\)$", "");
	}

	private void validateSwitchAmmunition(Map<String, Choice> equipment, List<String> blockers,
		List<String> explanations)
	{
		for (Allocation allocation : new ArrayList<>(allocations.values()))
		{
			if (!"SWITCH".equals(allocation.slot))
			{
				continue;
			}
			ItemStats weapon = player.getItems().get(allocation.id);
			if (weapon == null || !"WEAPON".equals(weapon.getSlot()))
			{
				continue;
			}
			if (EquipmentCompatibility.chargeBearing(weapon))
			{
				explanations.add("Check " + weapon.getName() + " switch charges and loaded ammunition before the trip.");
			}
			if (!EquipmentCompatibility.needsAmmo(weapon))
			{
				continue;
			}
			Choice ammo = equipment.get("AMMO");
			boolean equippedAmmo = ammo != null && ammo.getItemId() > 0
				&& EquipmentCompatibility.compatible(weapon, player.getItems().get(ammo.getItemId()));
			boolean packedAmmo = allocations.values().stream().map(value -> player.getItems().get(value.id))
				.anyMatch(item -> item != null && "AMMO".equals(item.getSlot())
					&& EquipmentCompatibility.compatible(weapon, item));
			if (equippedAmmo || packedAmmo)
			{
				continue;
			}
			List<ItemStats> alternatives = player.getItems().values().stream()
				.filter(item -> item != null && "AMMO".equals(item.getSlot())
					&& equipmentPlanner.itemProblems(item.getId()).isEmpty()
					&& EquipmentCompatibility.compatible(weapon, item))
				.sorted(Comparator.comparingDouble(ItemStats::getStrength).reversed().thenComparingInt(ItemStats::getId))
				.collect(Collectors.toList());
			ItemStats alternative = alternatives.stream().filter(item -> available.getOrDefault(item.getId(), 0) > 0)
				.findFirst().orElse(null);
			if (alternative != null && slots < capacity)
			{
				int quantity = Math.min(1000, available.get(alternative.getId()));
				Supply supply = supply("Ammunition for " + weapon.getName(), alternatives.stream().map(ItemStats::getId).collect(Collectors.toList()),
					quantity, allocation.required, alternative.isStackable());
				allocate(new Need(supply, "Owned fallback", "SWITCH AMMO", allocation.required),
					equipment, blockers, explanations);
				explanations.add(weapon.getName() + ": compatible ammunition is packed with the weapon switch.");
			}
			else if (allocation.required)
			{
				blockers.add("Required weapon switch " + weapon.getName() + " has no usable compatible ammunition in the plan.");
			}
			else
			{
				allocations.remove(allocation.id);
				slots -= allocation.stackable ? 1 : allocation.quantity;
				available.merge(allocation.id, allocation.quantity, Integer::sum);
				explanations.add("Omitted optional " + weapon.getName() + " switch because compatible ammunition cannot be packed.");
			}
		}
	}

	private void addOwnedBasics(Map<String, Choice> equipment, List<String> blockers, List<String> explanations)
	{
		boolean prayerPlanned = allocations.values().stream().anyMatch(a -> restoration(a.name));
		if (!prayerPlanned && player.getLevels().getOrDefault("PRAYER", 1) > 1)
		{
			addFallback("Owned prayer/restoration potions", player.getItems().values().stream().filter(item -> restoration(item.getName()))
				.sorted(Comparator.comparingInt((ItemStats item) -> doses(item.getName())).reversed()
					.thenComparing(ItemStats::getName)).collect(Collectors.toList()), 2, equipment, blockers, explanations);
		}
		if (slots < capacity)
		{
			int plannedFood = allocations.values().stream().filter(a -> foodPriority(a.name) > 0).mapToInt(a -> a.quantity).sum();
			addFallback("Owned food", player.getItems().values().stream().filter(item -> foodPriority(item.getName()) > 0)
				.sorted(Comparator.comparingInt((ItemStats item) -> foodPriority(item.getName())).reversed()
				.thenComparing(ItemStats::getName)).collect(Collectors.toList()), plannedFood + capacity - slots, equipment, blockers, explanations);
		}
		if (allocations.values().stream().noneMatch(a -> foodPriority(a.name) > 0))
		{
			explanations.add("No recognised owned food is included; review the method's sustain and healing guidance.");
		}
		if (allocations.values().stream().anyMatch(a -> "Owned fallback".equals(a.origin)))
		{
			explanations.add("Owned restoration supplements the method supplies; useful owned food fills the remaining inventory slots after travel, casting and storage items.");
		}
	}

	private boolean castingResource(Supply supply)
	{
		return !supply.getItemIds().isEmpty() && supply.getItemIds().stream().allMatch(id ->
			preparation.getRuneIds().containsValue(id) || preparation.getCombinationRunes().containsKey(id)
				|| preparation.getSpells().values().stream().anyMatch(spell -> spell.getSackItemId() == id));
	}

	private List<Supply> aggregateCasting(List<Supply> supplies)
	{
		Map<List<Integer>, Supply> result = new LinkedHashMap<>();
		for (Supply resource : supplies)
		{
			List<Integer> key = new ArrayList<>(resource.getItemIds());
			Supply previous = result.get(key);
			if (previous == null)
			{
				result.put(key, supply(resource.getName(), key, resource.getQuantity(), true, true));
			}
			else
			{
				previous.setQuantity((int) Math.min(Integer.MAX_VALUE, (long) previous.getQuantity() + resource.getQuantity()));
			}
		}
		return new ArrayList<>(result.values());
	}

	private boolean blightedItem(int id)
	{
		ItemStats item = player.getItems().get(id);
		return preparation.getBlightedReplacements().containsValue(id)
			|| item != null && item.getName().toLowerCase(Locale.ROOT).startsWith("blighted ");
	}

	private Supply blightedAlternatives(Supply original)
	{
		if (!allowBlighted) { return original; }
		List<Integer> ids = new ArrayList<>();
		for (int id : original.getItemIds())
		{
			Integer replacement = preparation.getBlightedReplacements().get(id);
			if (replacement != null) { ids.add(replacement); }
			ids.add(id);
		}
		return supply(original.getName(), new ArrayList<>(new LinkedHashSet<>(ids)),
			original.getQuantity(), original.isRequired(), original.isStackable());
	}

	private List<Choice> choices()
	{
		List<Choice> result = new ArrayList<>();
		boolean ownsPouch = preparation.getPouches().stream()
			.anyMatch(pouch -> player.getOwned().getOrDefault(pouch.getItemId(), 0) > 0);
		for (Allocation allocation : allocations.values())
		{
			Choice choice = allocation.choice(carried);
			int inPouch = ownsPouch && player.getDeathContext().isPouchContentsKnown()
				? Math.min(allocation.quantity, player.getDeathContext().getPouchContents().getOrDefault(allocation.id, 0)) : 0;
			String explanation = "RUNE POUCH".equals(allocation.slot)
				? inPouch + " already in pouch; load " + Math.max(0, allocation.quantity - inPouch)
					+ " more to match this configuration. Unload other runes."
				: inPouch > 0 ? "Includes " + inPouch + " runes available to unload from your observed pouch." : "";
			if (inPouch > 0 && preparation.getPouches().stream()
				.noneMatch(pouch -> player.getCarried().getOrDefault(pouch.getItemId(), 0) > 0))
			{
				explanation += " The pouch is banked; withdraw the loaded pouch or unload those runes at the bank.";
			}
			if ("LOOTING BAG".equals(allocation.slot))
			{
				explanation = "Preparation target: empty the bag at a bank before departure. Future loot adds to the planned loss; current contents are assessed separately.";
			}
			result.add(new Choice(choice.getItemId(), choice.getQuantity(), choice.getCarried(), choice.getWithdraw(),
				choice.getMissing(), choice.getSlot(), choice.getName(), choice.getOrigin(), choice.isRequired(), explanation));
		}
		return result;
	}

	private void packRunePouch(Map<String, Choice> equipment, List<String> explanations)
	{
		if (omitOptionalPouch)
		{
			explanations.add("Rune pouch: leave the optional pouch banked; the completed trip has lower Wilderness risk with loose runes.");
			return;
		}
		List<Allocation> runes = allocations.values().stream().filter(a -> a.quantity > 0
			&& (preparation.getRuneIds().containsValue(a.id) || preparation.getCombinationRunes().containsKey(a.id)))
			.collect(Collectors.toList());
		if (runes.size() < 2) { return; }
		boolean packedPouch = allocations.keySet().stream().anyMatch(this::pouchItem);
		List<TripPreparationData.Pouch> pouches = preparation.getPouches().stream()
			.filter(p -> packedPouch ? allocations.containsKey(p.getItemId()) : available.getOrDefault(p.getItemId(), 0) > 0)
			.sorted(Comparator.comparingInt(TripPreparationData.Pouch::getCapacity).reversed())
			.collect(Collectors.toList());
		Scenario looseRisk = equipmentPlanner.riskCalculator().applies()
			? equipmentPlanner.riskCalculator().baseline(equipment, choices()) : null;
		for (TripPreparationData.Pouch pouch : pouches)
		{
			List<Allocation> contained = runes.stream().filter(a -> a.quantity <= pouch.getMaxPerRune())
				.limit(pouch.getCapacity()).collect(Collectors.toList());
			if (contained.size() < 2) { continue; }
			ItemStats stats = player.getItems().get(pouch.getItemId());
			Allocation container = allocations.get(pouch.getItemId());
			boolean added = container == null;
			if (added)
			{
				container = new Allocation(pouch.getItemId(), stats == null ? "Rune pouch" : stats.getName(),
					"SUPPLY", "Trip preparation", false);
				container.quantity = 1;
			}
			allocations.put(container.id, container);
			Map<Allocation, String> priorSlots = new LinkedHashMap<>();
			contained.forEach(a -> { priorSlots.put(a, a.slot); a.slot = "RUNE POUCH"; });
			Scenario pouchRisk = looseRisk == null ? null : equipmentPlanner.riskCalculator().baseline(equipment, choices());
			if (pouchRisk != null && (!pouchRisk.isComplete() || pouchRisk.getPermanentLosses() > looseRisk.getPermanentLosses()
				|| pouchRisk.getTotalRisk() > riskBudget && pouchRisk.getTotalRisk() > looseRisk.getTotalRisk()))
			{
				if (added) { allocations.remove(container.id); }
				priorSlots.forEach((a, slot) -> a.slot = slot);
				continue;
			}
			if (added) { available.computeIfPresent(container.id, (id, count) -> count - 1); }
			int saved = contained.size() - (added ? 1 : 0);
			slots -= saved;
			explanations.add("Rune pouch: " + container.name + " saves " + saved
				+ " inventory slots. Use the exact displayed configuration and unload any extra runes. Capacity " + pouch.getCapacity()
				+ " types, " + pouch.getMaxPerRune() + " per type.");
			return;
		}
		explanations.add("Rune pouch: loose runes retained; no owned pouch can save slots within the verified capacity and Wilderness risk limits.");
	}

	private void packLootingBag(Map<String, Choice> equipment, List<String> blockers, List<String> explanations)
	{
		if (!allowBlighted) { return; }
		if (omitOptionalBag)
		{
			explanations.add("Looting bag: leave the optional bag banked; the completed trip has lower Wilderness risk without it.");
			return;
		}
		List<Integer> owned = preparation.getLootingBagIds().stream()
			.filter(id -> allocations.containsKey(id) || available.getOrDefault(id, 0) > 0).collect(Collectors.toList());
		if (owned.isEmpty())
		{
			explanations.add("Looting bag: none observed. Loot must use inventory space freed as supplies are consumed.");
			return;
		}
		if (owned.stream().noneMatch(allocations::containsKey))
		{
			allocate(new Need(supply("Looting bag", owned, 1, false, false), "Trip preparation", "LOOTING BAG", false),
				equipment, blockers, explanations);
		}
		if (owned.stream().noneMatch(allocations::containsKey))
		{
			explanations.add("Looting bag: no spare slot after mandatory items; loot uses freed inventory slots.");
			return;
		}
		owned.stream().map(allocations::get).filter(allocation -> allocation != null)
			.forEach(allocation -> allocation.slot = "LOOTING BAG");
		explanations.add("Looting bag: one inventory slot reserved. Its 28 loot slots cannot supply food, potions or casting runes during the trip.");
		explanations.add("Looting bag: empty the bag at a bank before departure. Planned risk assumes this empty target; future loot adds loss and actual carried contents remain separately assessed.");
		preparation.getLootingBagNotes().forEach(note -> explanations.add("Looting bag: " + note));
	}

	private boolean pouchItem(int id)
	{
		return preparation.getPouches().stream().anyMatch(pouch -> pouch.getItemId() == id);
	}

	private int plannedPouchLimit()
	{
		return preparation.getPouches().stream().filter(pouch -> allocations.containsKey(pouch.getItemId()))
			.mapToInt(TripPreparationData.Pouch::getMaxPerRune).min().orElse(0);
	}

	private void addFallback(String name, List<ItemStats> options, int target,
		Map<String, Choice> equipment, List<String> blockers, List<String> explanations)
	{
		if (target <= 0 || options.isEmpty()) { return; }
		Supply supply = supply(name, options.stream().map(ItemStats::getId).collect(Collectors.toList()), target, false, false);
		allocate(new Need(supply, "Owned fallback", "SUPPLY", false), equipment, blockers, explanations);
	}

	private static int foodPriority(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT).replaceFirst("^blighted ", "");
		if (lower.equals("karambwan")) { lower = "cooked karambwan"; }
		if (lower.startsWith("raw ") || lower.contains("burnt") || lower.contains("noted"))
		{
			return 0;
		}
		String[] foods = {"trout", "salmon", "tuna", "lobster", "bass", "swordfish", "monkfish",
			"cooked karambwan", "shark", "sea turtle", "manta ray", "dark crab", "anglerfish"};
		for (int index = foods.length - 1; index >= 0; index--)
		{
			if (lower.equals(foods[index]))
			{
				return index + 1;
			}
		}
		return 0;
	}

	private boolean waived(Supply supply)
	{
		return supply.getWaiverRequirement() != null
			&& requirements.isMet(supply.getWaiverRequirement());
	}

	private static boolean restoration(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT);
		return (lower.startsWith("prayer potion(") || lower.startsWith("super restore(") || lower.startsWith("blighted super restore("))
			&& !lower.contains("(0)");
	}

	private static int doses(String name)
	{
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\((\\d)\\)").matcher(name);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
	}

	private static Supply supply(String name, List<Integer> ids, int quantity, boolean required, boolean stackable)
	{
		Supply supply = new Supply();
		supply.setName(name);
		supply.setItemIds(ids);
		supply.setQuantity(quantity);
		supply.setRequired(required);
		supply.setStackable(stackable);
		return supply;
	}

	private static void add(List<String> values, String value)
	{
		if (!values.contains(value))
		{
			values.add(value);
		}
	}

	private static final class Attempt
	{
		private final SupplyPlanner planner;
		private final List<Choice> inventory;
		private final List<String> blockers;
		private final List<String> explanations;
		private final Scenario risk;

		private Attempt(SupplyPlanner planner, List<Choice> inventory, List<String> blockers,
			List<String> explanations, Scenario risk)
		{
			this.planner = planner;
			this.inventory = inventory;
			this.blockers = blockers;
			this.explanations = explanations;
			this.risk = risk;
		}
	}

	private static final class Coverage
	{
		private final List<Integer> ids;
		private final int target;

		private Coverage(List<Integer> ids, int target)
		{
			this.ids = ids;
			this.target = target;
		}
	}

	private static final class Fulfillment
	{
		private final int quantity;
		private final long doses;

		private Fulfillment(int quantity, long doses)
		{
			this.quantity = quantity;
			this.doses = doses;
		}
	}

	private static final class Need
	{
		private final Supply supply;
		private final String origin;
		private final String slot;
		private final boolean required;

		private Need(Supply supply, String origin, String slot, boolean required)
		{
			this.supply = supply;
			this.origin = origin;
			this.slot = slot;
			this.required = required;
		}

		private String key()
		{
			return slot + "|" + origin + "|" + supply.getName() + "|" + supply.getItemIds();
		}
	}

	private static final class Allocation
	{
		private final int id;
		private final String name;
		private String slot;
		private String origin;
		private final boolean stackable;
		private int quantity;
		private int missing;
		private boolean required;

		private Allocation(int id, String name, String slot, String origin, boolean stackable)
		{
			this.id = id;
			this.name = name;
			this.slot = slot;
			this.origin = origin;
			this.stackable = stackable;
		}

		private Choice choice(Map<Integer, Integer> carried)
		{
			int carriedCount = Math.min(quantity, carried.getOrDefault(id, 0));
			return new Choice(id, quantity, carriedCount, quantity - carriedCount,
				missing, slot, name, origin, required);
		}
	}
}

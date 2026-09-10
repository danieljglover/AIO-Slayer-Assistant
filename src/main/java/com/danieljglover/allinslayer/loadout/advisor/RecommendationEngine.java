package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.RecommendationGoal;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Setup;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Evidence;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Master;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Method;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Monster;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Supply;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Pure, deterministic recommendations from bundled knowledge and an immutable player snapshot. */
public final class RecommendationEngine
{
	public RecommendationResult recommend(SlayerCatalogue catalogue, RecommendationRequest request)
	{
		return recommend(catalogue, request, () -> false);
	}

	public RecommendationResult recommend(SlayerCatalogue catalogue, RecommendationRequest request,
		java.util.function.BooleanSupplier cancelled)
	{
		PreparationWork work = new PreparationWork(cancelled);
		List<String> notices = new ArrayList<>();
		if (request == null)
		{
			return new RecommendationResult(null, Collections.emptyList(),
				Collections.singletonList("Select a Slayer task to build a recommendation."));
		}
		if (catalogue == null)
		{
			return new RecommendationResult(request.getTaskId(), Collections.emptyList(),
				Collections.singletonList("The bundled Slayer catalogue is unavailable."));
		}
		Task task = catalogue.getTasks().get(request.getTaskId());
		if (task == null)
		{
			return new RecommendationResult(request.getTaskId(), Collections.emptyList(),
				Collections.singletonList(request.getTaskId() == null ? "Select a task from the Slayer catalogue."
					: "This Slayer assignment has no matching bundled task record: " + request.getTaskId() + "."));
		}
		if (!request.getPlayer().isLoggedIn())
		{
			notices.add("Log in to verify levels and equipment. The catalogue remains available for planning.");
		}
		if (request.getPlayer().getBankSeenAt() == 0)
		{
			notices.add("Open your bank to include banked items. Ownership currently covers only observed carried equipment and inventory.");
		}
		else
		{
			notices.add("Bank ownership uses the last observed bank. Reopen it after bank changes or account changes.");
		}
		if (!request.isActiveTask())
		{
			notices.add("On-task preview: the selected assignment is assumed for Slayer bonuses and task-only preparations. Your detected task is unchanged; levels, quests, unlocks and Wilderness loss are still checked.");
		}
		if (request.isWildernessTaskForPlanning() && !request.isAllowWilderness())
		{
			notices.add("This assignment requires Wilderness kills. Enable Allow Wilderness to see preparations for it; no location is recommended while it is disabled.");
			return new RecommendationResult(task.getId(), Collections.emptyList(), notices);
		}
		if (request.getMasterId() != null && !task.getMasterIds().contains(request.getMasterId()))
		{
			notices.add("This task is not listed for the selected Slayer master.");
			return new RecommendationResult(task.getId(), Collections.emptyList(), notices);
		}
		List<RankedSetup> ranked = new ArrayList<>();
		boolean excludedWilderness = false;
		for (String monsterId : task.getMonsterIds())
		{
			if (!request.getAllowedMonsterIds().isEmpty() && !request.getAllowedMonsterIds().contains(monsterId))
			{
				continue;
			}
			if (request.getMonsterId() != null && !request.getMonsterId().equals(monsterId))
			{
				continue;
			}
			Monster monster = catalogue.getMonsters().get(monsterId);
			if (monster == null)
			{
				notices.add("Missing monster record: " + monsterId + ".");
				continue;
			}
			if (!monster.isRepeatable())
			{
				if (request.getMonsterId() != null)
				{
					notices.add(monster.getName() + " is an incidental encounter. Its task information remains available, but it has no repeatable trip recommendation.");
				}
				continue;
			}
			Set<String> methodIds = new LinkedHashSet<>(monster.getMethodIds());
			catalogue.getMethods().values().stream().filter(method -> method.getMonsterIds().contains(monsterId))
				.forEach(method -> methodIds.add(method.getId()));
			List<String> locationIds = monster.getLocationIds().isEmpty() ? task.getLocationIds() : monster.getLocationIds();
			if (methodIds.isEmpty())
			{
				notices.add(monster.getName() + ": no combat method has been compiled; read the task guidance.");
			}
			for (String methodId : methodIds)
			{
				if (request.getMethodId() != null && !request.getMethodId().equals(methodId))
				{
					continue;
				}
				Method method = catalogue.getMethods().get(methodId);
				if (method == null || !method.isSelectable()
					|| !method.getMonsterIds().isEmpty() && !method.getMonsterIds().contains(monsterId))
				{
					continue;
				}
				for (String locationId : new LinkedHashSet<>(locationIds))
				{
					if (request.getLocationId() != null && !request.getLocationId().equals(locationId)
						|| !method.getLocationIds().isEmpty() && !method.getLocationIds().contains(locationId))
					{
						continue;
					}
					Location base = catalogue.getLocations().get(locationId);
					Location location = task.getLocationOverrides().getOrDefault(locationId, base);
					if (location == null)
					{
						notices.add("Missing location record: " + locationId + ".");
						continue;
					}
					// Disabled locations are not automatic candidates, even if every owned setup
					// has missing gear. An explicitly selected location can still explain its blocker.
					if (wildernessRisk(catalogue, location) && !request.isAllowWilderness() && request.getLocationId() == null)
					{
						excludedWilderness = true;
						continue;
					}
					if (request.isWildernessTaskForPlanning() && !location.isWilderness())
					{
						continue;
					}
					work.checkCancelled();
					Setup setup = setup(catalogue, request, task, monster, location, method, work);
					ranked.add(new RankedSetup(setup, rank(method, request.getGoal()),
						wildernessRisk(catalogue, location) ? 1 : 0, method.isGroup() ? 1 : 0));
				}
			}
		}
		ranked.sort(Comparator.comparing((RankedSetup value) -> !value.setup.isFeasible())
			.thenComparingInt(value -> value.priority)
			.thenComparingInt(value -> value.wilderness)
			.thenComparingLong(value -> value.setup.getWildernessRisk() == null ? 0
				: value.setup.getWildernessRisk().baseline().getTotalRisk())
			.thenComparingInt(value -> value.group)
			.thenComparingInt(value -> value.setup.getBlockers().size())
			.thenComparingInt(value -> (int) value.setup.getEquipment().values().stream()
				.filter(choice -> "Owned fallback".equals(choice.getOrigin())).count())
			.thenComparing(value -> value.setup.getMonsterId())
			.thenComparing(value -> value.setup.getLocationId())
			.thenComparing(value -> value.setup.getMethodId()));
		if (work.exhausted())
		{
			notices.add("The task-wide Wilderness comparison limit was reached. Select a monster or location to focus the owned-alternative search. Displayed costs are still assessed; an optimal loadout is not guaranteed.");
		}
		if (excludedWilderness)
		{
			notices.add("Wilderness locations are excluded from automatic recommendations. Enable Allow Wilderness to include them.");
		}
		if (ranked.isEmpty())
		{
			notices.add("No compiled monster, location and combat-method combination matches these selections. Clear a filter or inspect the source coverage notes.");
		}
		else if (ranked.stream().noneMatch(value -> value.setup.isFeasible()))
		{
			notices.add("No setup is currently verified as feasible. Review the listed requirements, missing gear and access confirmations.");
		}
		notices.add("Goals use relative source priorities; hourly XP, kill speed and profit are not measured. Wilderness losses use cached item prices.");
		return new RecommendationResult(task.getId(), ranked.stream().map(value -> value.setup).collect(Collectors.toList()),
			new ArrayList<>(new LinkedHashSet<>(notices)));
	}

	private Setup setup(SlayerCatalogue catalogue, RecommendationRequest request, Task task,
		Monster monster, Location location, Method method, PreparationWork work)
	{
		List<String> blockers = new ArrayList<>();
		List<String> explanations = new ArrayList<>();
		List<String> guidance = new ArrayList<>(task.getNotes());
		guidance.addAll(location.getNotes());
		guidance.addAll(method.getGuidance());
		method.getRisks().forEach(risk -> guidance.add("Risk: " + risk));
		RequirementEvaluator evaluator = new RequirementEvaluator(request, monster.getId());
		if (!request.getPlayer().isLoggedIn())
		{
			blockers.add("Player state is unavailable while logged out.");
		}
		evaluator.level("SLAYER", Math.max(task.getSlayerLevel(), monster.getSlayerLevel()), blockers);
		evaluator.checkAll(task.getRequirements(), blockers);
		evaluator.checkAll(monster.getRequirements(), blockers);
		evaluator.checkAll(location.getRequirements(), blockers);
		SpellPlanner spellPlanner = new SpellPlanner(catalogue.getPreparation(), request);
		evaluator.checkAll(method.getRequirements().stream().filter(requirement -> !spellPlanner.managesRequirement(requirement))
			.collect(Collectors.toList()), blockers);
		Master master = catalogue.getMasters().get(request.getMasterId());
		if (master != null && !request.isActiveTask())
		{
			evaluator.level("COMBAT", master.getCombatLevel(), blockers);
			evaluator.level("SLAYER", master.getSlayerLevel(), blockers);
			evaluator.checkAll(master.getRequirements(), blockers);
		}
		if ((location.isTaskOnly() || method.isTaskOnly()) && !request.hasTaskForPlanning())
		{
			blockers.add("Requires an active matching Slayer task.");
		}
		if (request.isActiveTask() && request.getLockedLocationId() != null
			&& !request.getLockedLocationId().equals(location.getId())
			&& !request.getLockedLocationId().equals(location.getAssignmentAreaId()))
		{
			blockers.add("Your active task is locked to " + locationName(catalogue, request.getLockedLocationId()) + ".");
		}
		if (request.isWildernessTaskForPlanning() && !location.isWilderness())
		{
			blockers.add("This Wilderness assignment only counts eligible Wilderness kills.");
		}
		if (wildernessRisk(catalogue, location) && !request.isAllowWilderness())
		{
			blockers.add("Wilderness setups are disabled. Enable Wilderness options to include this setup.");
		}
		if (method.isGroup() && !request.isAllowGroups())
		{
			blockers.add("Group methods are disabled. Enable group options to include this setup.");
		}
		if (method.isCannon() && !location.isCannon())
		{
			blockers.add("This location does not support the method's cannon.");
		}
		if (method.isCannon())
		{
			evaluator.checkAll(Collections.singletonList("Dwarf Cannon"), blockers);
		}
		if (method.isBarrage() && (!location.isBarrage() || !location.isMulti()))
		{
			blockers.add("This location does not support the method's multi-target barrage setup.");
		}
		if (method.getEquipment().isEmpty())
		{
			explanations.add("No Wiki equipment priorities were compiled for this method; all gear uses labelled owned fallbacks.");
		}
		if (method.getCoverageStatus() != null)
		{
			explanations.add("Source coverage: " + method.getCoverageStatus());
		}
		int sourceRank = rank(method, request.getGoal());
		explanations.add(sourceRank == Integer.MAX_VALUE
			? "Goal: " + request.getGoal() + ". This method is unrated; placed after methods with a source priority."
			: "Goal: " + request.getGoal() + "; relative source priority " + sourceRank + ".");
		if (method.getRankingReason() != null && !method.getRankingReason().isEmpty())
		{
			explanations.add(method.getRankingReason());
		}
		List<Supply> required = new ArrayList<>(task.getRequiredItems());
		required.addAll(location.getRequiredItems());
		required.addAll(method.getRequiredItems());
		required.removeIf(supply -> waived(supply, request));
		if (method.isBarrage() && !spellPlanner.hasDocumentedBarrageSpell(method))
		{
			blockers.add("This barrage method lacks a reviewed explicit spell baseline; its source plan needs review.");
		}
		EquipmentPlanner equipmentPlanner = new EquipmentPlanner(catalogue, request, task, monster, location, method, work);
		Map<String, Choice> equipment = equipmentPlanner.plan(required, blockers, explanations);
		SupplyPlanner supplies = new SupplyPlanner(request, equipmentPlanner, equipment);
		List<Choice> inventory = supplies.planOptimized(method, location, required, equipment, blockers, explanations);
		WildernessRisk risk = null;
		WildernessRiskCalculator riskCalculator = equipmentPlanner.riskCalculator();
		if (riskCalculator.applies())
		{
			risk = riskCalculator.assess(equipment, inventory, request.getWildernessRiskBudget());
			if (risk.baseline().getPermanentLosses() > 0)
			{
				blockers.add("This setup risks permanent loss of an untradeable item or component. Use a protected or locked alternative.");
			}
			if (!risk.baseline().isComplete())
			{
				blockers.add("Wilderness loss cannot be fully valued for this setup. Review its unknown death rules, prices or stored charges in Wilderness death risk.");
			}
			if (risk.baseline().getTotalRisk() > request.getWildernessRiskBudget())
			{
				blockers.add("Estimated Wilderness loss exceeds the " + request.getWildernessRiskBudget()
					+ " gp budget. Adjust the loss budget in plugin configuration or choose another setup.");
			}
		}

		List<Evidence> evidence = new ArrayList<>(task.getEvidence());
		evidence.addAll(monster.getEvidence());
		evidence.addAll(location.getEvidence());
		evidence.addAll(method.getEvidence());
		if (risk != null)
		{
			if (catalogue.getWildernessAreas().containsKey(location.getId()))
			{
				evidence.addAll(catalogue.getWildernessAreas().get(location.getId()).getEvidence());
			}
			java.util.Set<Integer> riskIds = new LinkedHashSet<>(WildernessRiskCalculator.quantities(equipment, inventory).keySet());
			riskIds.addAll(request.getPlayer().getRiskCarried().keySet());
			riskIds.stream().map(catalogue.getDeathRules()::get).filter(java.util.Objects::nonNull)
				.forEach(rule -> evidence.addAll(rule.getEvidence()));
		}
		Map<String, Evidence> uniqueEvidence = new LinkedHashMap<>();
		for (Evidence source : evidence)
		{
			uniqueEvidence.put(source.getUrl() + "#" + source.getRevisionId(), source);
		}
		return new Setup(monster.getId(), location.getId(), method.getId(),
			monster.getName() + " - " + location.getName() + " - " + method.getName(),
			method.getSummary(), blockers.isEmpty(), equipment, inventory,
			new ArrayList<>(new LinkedHashSet<>(explanations)), new ArrayList<>(new LinkedHashSet<>(blockers)),
			new ArrayList<>(new LinkedHashSet<>(guidance)), new ArrayList<>(uniqueEvidence.values()), risk);
	}

	private static boolean wildernessRisk(SlayerCatalogue catalogue, Location location)
	{
		return location.isWilderness() || catalogue.getWildernessAreas().containsKey(location.getId())
			&& catalogue.getWildernessAreas().get(location.getId()).isWildernessTravel();
	}

	private static int rank(Method method, RecommendationGoal goal)
	{
		int value = goal == RecommendationGoal.PROFIT ? method.getProfitRank()
			: goal == RecommendationGoal.LOW_EFFORT ? method.getEffortRank() : method.getXpRank();
		return value > 0 ? value : Integer.MAX_VALUE;
	}

	private static boolean waived(Supply supply, RecommendationRequest request)
	{
		return supply.getWaiverRequirement() != null
			&& new RequirementEvaluator(request).isMet(supply.getWaiverRequirement());
	}

	private static String locationName(SlayerCatalogue catalogue, String id)
	{
		Location location = catalogue.getLocations().get(id);
		return location == null ? id : location.getName();
	}

	private static final class RankedSetup
	{
		private final Setup setup;
		private final int priority;
		private final int wilderness;
		private final int group;

		private RankedSetup(Setup setup, int priority, int wilderness, int group)
		{
			this.setup = setup;
			this.priority = priority;
			this.wilderness = wilderness;
			this.group = group;
		}
	}
}

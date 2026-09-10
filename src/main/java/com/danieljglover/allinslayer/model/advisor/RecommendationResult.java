package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public final class RecommendationResult
{
	private final String taskId;
	private final List<Setup> setups;
	private final List<String> notices;

	public RecommendationResult(String taskId, List<Setup> setups, List<String> notices)
	{
		this.taskId = taskId;
		this.setups = copy(setups);
		this.notices = copy(notices);
	}

	public Setup best()
	{
		return setups.stream().filter(Setup::isFeasible).findFirst().orElse(null);
	}

	private static <T> List<T> copy(List<T> input)
	{
		return Collections.unmodifiableList(new ArrayList<>(
			input == null ? Collections.emptyList() : input));
	}

	@Getter
	public static final class Setup
	{
		private final WildernessRisk wildernessRisk;
		private final String monsterId;
		private final String locationId;
		private final String methodId;
		private final String title;
		private final String summary;
		private final boolean feasible;
		private final Map<String, Choice> equipment;
		private final List<Choice> inventory;
		private final List<String> explanations;
		private final List<String> blockers;
		private final List<String> guidance;
		private final List<SlayerCatalogue.Evidence> evidence;

		public Setup(String monsterId, String locationId, String methodId, String title,
			String summary, boolean feasible, Map<String, Choice> equipment,
			List<Choice> inventory, List<String> explanations, List<String> blockers,
			List<String> guidance, List<SlayerCatalogue.Evidence> evidence)
		{
			this(monsterId, locationId, methodId, title, summary, feasible, equipment, inventory,
				explanations, blockers, guidance, evidence, null);
		}

		public Setup(String monsterId, String locationId, String methodId, String title,
			String summary, boolean feasible, Map<String, Choice> equipment,
			List<Choice> inventory, List<String> explanations, List<String> blockers,
			List<String> guidance, List<SlayerCatalogue.Evidence> evidence, WildernessRisk wildernessRisk)
		{
			this.wildernessRisk = wildernessRisk;
			this.monsterId = monsterId;
			this.locationId = locationId;
			this.methodId = methodId;
			this.title = title;
			this.summary = summary;
			this.feasible = feasible;
			this.equipment = Collections.unmodifiableMap(new LinkedHashMap<>(
				equipment == null ? Collections.emptyMap() : equipment));
			this.inventory = copy(inventory);
			this.explanations = copy(explanations);
			this.blockers = copy(blockers);
			this.guidance = copy(guidance);
			this.evidence = copy(evidence);
		}
	}

	@Getter
	public static final class Choice
	{
		private final int itemId;
		private final int quantity;
		private final int carried;
		private final int withdraw;
		private final int missing;
		private final String slot;
		private final String name;
		private final String origin;
		private final boolean required;
		private final String explanation;

		public Choice(int itemId, int quantity, int carried, int withdraw, int missing,
			String slot, String name, String origin, boolean required)
		{
			this(itemId, quantity, carried, withdraw, missing, slot, name, origin, required, "");
		}

		public Choice(int itemId, int quantity, int carried, int withdraw, int missing,
			String slot, String name, String origin, boolean required, String explanation)
		{
			this.itemId = itemId;
			this.quantity = Math.max(0, quantity);
			this.carried = Math.max(0, carried);
			this.withdraw = Math.max(0, withdraw);
			this.missing = Math.max(0, missing);
			this.slot = slot;
			this.name = name;
			this.origin = origin;
			this.required = required;
			this.explanation = explanation == null ? "" : explanation;
		}
	}
}

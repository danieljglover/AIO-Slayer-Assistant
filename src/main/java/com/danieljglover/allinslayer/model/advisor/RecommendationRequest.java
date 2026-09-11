package com.danieljglover.allinslayer.model.advisor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.EqualsAndHashCode;

@Getter
@EqualsAndHashCode
public final class RecommendationRequest
{
	private final String taskId;
	private final String masterId;
	private final String monsterId;
	private final String locationId;
	private final String methodId;
	private final boolean activeTask;
	private final int remaining;
	private final String lockedLocationId;
	private final boolean wildernessAssignment;
	private final RecommendationGoal goal;
	private final ReturnDestination returnDestination;
	private final boolean allowWilderness;
	private final boolean allowGroups;
	private final long wildernessRiskBudget;
	private final int plannedEtherCharges;
	private final int wildernessChargeLimit;
	private final BoostReservations boostReservations;
	private final FoodOverride foodOverride;
	private final PlayerSnapshot player;
	private final Set<String> allowedMonsterIds;

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player)
	{
		this(taskId, masterId, monsterId, locationId, methodId, activeTask, remaining,
			lockedLocationId, wildernessAssignment, goal, allowWilderness, allowGroups,
			player, Collections.emptySet());
	}

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player, Set<String> allowedMonsterIds)
	{
		this(taskId, masterId, monsterId, locationId, methodId, activeTask, remaining,
			lockedLocationId, wildernessAssignment, goal, allowWilderness, allowGroups, player, allowedMonsterIds, 500000);
	}

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player, Set<String> allowedMonsterIds,
		long wildernessRiskBudget)
	{
		this(taskId, masterId, monsterId, locationId, methodId, activeTask, remaining,
			lockedLocationId, wildernessAssignment, goal, allowWilderness, allowGroups,
			player, allowedMonsterIds, wildernessRiskBudget, 500);
	}

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player, Set<String> allowedMonsterIds,
		long wildernessRiskBudget, int plannedEtherCharges)
	{
		this(taskId, masterId, monsterId, locationId, methodId, activeTask, remaining,
			lockedLocationId, wildernessAssignment, goal, allowWilderness, allowGroups,
			player, allowedMonsterIds, wildernessRiskBudget, plannedEtherCharges, ReturnDestination.SLAYER_MASTER);
	}

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player, Set<String> allowedMonsterIds,
		long wildernessRiskBudget, int plannedEtherCharges, ReturnDestination returnDestination)
	{
		this(taskId, masterId, monsterId, locationId, methodId, activeTask, remaining,
			lockedLocationId, wildernessAssignment, goal, allowWilderness, allowGroups,
			player, allowedMonsterIds, wildernessRiskBudget, plannedEtherCharges, returnDestination, 500);
	}

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player, Set<String> allowedMonsterIds,
		long wildernessRiskBudget, int plannedEtherCharges, ReturnDestination returnDestination,
		int wildernessChargeLimit)
	{
		this(taskId, masterId, monsterId, locationId, methodId, activeTask, remaining,
			lockedLocationId, wildernessAssignment, goal, allowWilderness, allowGroups,
			player, allowedMonsterIds, wildernessRiskBudget, plannedEtherCharges, returnDestination,
			wildernessChargeLimit, BoostReservations.disabled());
	}

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player, Set<String> allowedMonsterIds,
		long wildernessRiskBudget, int plannedEtherCharges, ReturnDestination returnDestination,
		int wildernessChargeLimit, BoostReservations boostReservations)
	{
		this(taskId, masterId, monsterId, locationId, methodId, activeTask, remaining,
			lockedLocationId, wildernessAssignment, goal, allowWilderness, allowGroups,
			player, allowedMonsterIds, wildernessRiskBudget, plannedEtherCharges, returnDestination,
			wildernessChargeLimit, boostReservations, FoodOverride.disabled());
	}

	public RecommendationRequest(String taskId, String masterId, String monsterId,
		String locationId, String methodId, boolean activeTask, int remaining,
		String lockedLocationId, boolean wildernessAssignment, RecommendationGoal goal,
		boolean allowWilderness, boolean allowGroups, PlayerSnapshot player, Set<String> allowedMonsterIds,
		long wildernessRiskBudget, int plannedEtherCharges, ReturnDestination returnDestination,
		int wildernessChargeLimit, BoostReservations boostReservations, FoodOverride foodOverride)
	{
		this.returnDestination = returnDestination == null ? ReturnDestination.SLAYER_MASTER : returnDestination;
		this.wildernessRiskBudget = Math.max(0, wildernessRiskBudget);
		this.plannedEtherCharges = Math.max(1, Math.min(16000, plannedEtherCharges));
		this.wildernessChargeLimit = Math.max(1, Math.min(16000, wildernessChargeLimit));
		this.boostReservations = boostReservations == null ? BoostReservations.disabled() : boostReservations;
		this.foodOverride = foodOverride == null ? FoodOverride.disabled() : foodOverride;
		this.taskId = optional(taskId);
		this.masterId = optional(masterId);
		this.monsterId = optional(monsterId);
		this.locationId = optional(locationId);
		this.methodId = optional(methodId);
		this.activeTask = activeTask;
		this.remaining = Math.max(0, remaining);
		this.lockedLocationId = optional(lockedLocationId);
		this.wildernessAssignment = wildernessAssignment;
		this.goal = goal == null ? RecommendationGoal.SLAYER_XP : goal;
		this.allowWilderness = allowWilderness;
		this.allowGroups = allowGroups;
		this.player = player == null ? new PlayerSnapshot(Collections.emptyMap(),
			Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
			Collections.emptySet(), false, 0) : player;
		this.allowedMonsterIds = Collections.unmodifiableSet(new LinkedHashSet<>(
			allowedMonsterIds == null ? Collections.emptySet() : allowedMonsterIds));
	}

	private static String optional(String value)
	{
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

	public boolean isTaskPreview()
	{
		return !activeTask && taskId != null;
	}

	public boolean hasTaskForPlanning()
	{
		return isTaskPreview() || activeTask && player.isLoggedIn() && remaining > 0;
	}

	public boolean isWildernessTaskForPlanning()
	{
		return wildernessAssignment || isTaskPreview() && "krystilia".equals(masterId);
	}
}

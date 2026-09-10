package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.CombatBonus;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.ItemStats;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.ItemDefinition;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Method;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Monster;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Task;
import java.util.List;
import java.util.Locale;

/** Resolves mutually exclusive task/undead bonuses for this target and preparation. */
final class ConditionalBonuses
{
	private final SlayerCatalogue catalogue;
	private final Method method;
	private final boolean onTask;
	private final boolean undead;
	private final boolean preview;

	ConditionalBonuses(SlayerCatalogue catalogue, RecommendationRequest request, Task task,
		Monster monster, Location location, Method method)
	{
		this.catalogue = catalogue;
		this.method = method;
		this.undead = monster.isUndead();
		this.preview = request.isTaskPreview();
		String assignedArea = preview ? null : request.getLockedLocationId();
		this.onTask = request.hasTaskForPlanning()
			&& task.isSlayerHelmApplies() && task.getId().equals(request.getTaskId())
			&& task.getMonsterIds().contains(monster.getId())
			&& (request.getAllowedMonsterIds().isEmpty() || request.getAllowedMonsterIds().contains(monster.getId()))
			&& (!request.isWildernessTaskForPlanning() || location.isWilderness())
			&& (assignedArea == null || assignedArea.equals(location.getId())
				|| assignedArea.equals(location.getAssignmentAreaId()));
	}

	CombatBonus effect(ItemStats item)
	{
		ItemDefinition definition = item == null ? null : catalogue.getItems().get(item.getId());
		return definition == null || definition.getCombatBonus() == null ? CombatBonus.NONE : definition.getCombatBonus();
	}

	double applicable(ItemStats item)
	{
		CombatBonus effect = effect(item);
		return effect.isSlayer() && onTask || effect.isSalve() && undead ? effect.multiplier(method.getStyle()) : 1;
	}

	double multiplier(ItemStats head, ItemStats amulet)
	{
		double salve = applicable(amulet);
		// Wearing both is legal, including when the helmet supplies mandatory protection.
		// An applicable Salve effect takes priority; never add or multiply the two effects.
		return salve > 1 ? salve : applicable(head);
	}

	double rankingMultiplier(ItemStats head, ItemStats amulet, ItemStats weapon)
	{
		// Do not value Salve as an all-target boost for barrage/chinchompa preparations.
		return areaAttack(weapon) && applicable(amulet) > 1 ? 1 : multiplier(head, amulet);
	}

	void explain(ItemStats head, ItemStats amulet, ItemStats weapon, List<String> explanations)
	{
		double salve = applicable(amulet);
		double slayer = applicable(head);
		if (salve > 1)
		{
			explanations.add(amulet.getName() + ": +" + percent(salve) + "% " + method.getStyle().toLowerCase(Locale.ROOT)
				+ " accuracy and damage against this undead variant. Salve and Slayer helmet/Black mask boosts do not stack."
				+ (slayer > 1 ? " The helmet's task boost is suppressed; its equipment stats and protection remain." : ""));
			if (areaAttack(weapon))
			{
				explanations.add("Salve boosts only the primary ranged/magic target. Its boost is not credited to secondary targets when comparing this area method's head and amulet.");
			}
		}
		else if (slayer > 1)
		{
			explanations.add(head.getName() + ": +" + percent(slayer) + "% " + method.getStyle().toLowerCase(Locale.ROOT)
				+ (preview ? " accuracy and damage assuming the selected Catalogue task and location. This is a planning assumption."
					: " accuracy and damage for this matching active Slayer task and assigned area."));
		}
		if (effect(head).isSlayer() && slayer == 1)
		{
			explanations.add(head.getName() + (onTask
				? ": the unimbued form has no ranged or magic task boost."
				: ": no task boost applies to this preview, target or location."));
		}
		if (effect(amulet).isSalve() && salve == 1)
		{
			explanations.add(amulet.getName() + (undead
				? ": this unimbued form has no ranged or magic undead boost."
				: ": this target variant is not flagged undead; no Salve boost is applied."));
		}
		if (method.isCannon() && (slayer > 1 || salve > 1))
		{
			explanations.add("These target-specific boosts apply to your own attacks; no Slayer/Salve accuracy boost is credited to the cannon.");
		}
	}

	private boolean areaAttack(ItemStats weapon)
	{
		return method.isBarrage() || "RANGED".equalsIgnoreCase(method.getStyle()) && weapon != null
			&& weapon.getName().toLowerCase(Locale.ROOT).contains("chinchompa");
	}

	private static String percent(double multiplier)
	{
		return multiplier == 7.0 / 6.0 ? "16.67" : Integer.toString((int) Math.round((multiplier - 1) * 100));
	}
}

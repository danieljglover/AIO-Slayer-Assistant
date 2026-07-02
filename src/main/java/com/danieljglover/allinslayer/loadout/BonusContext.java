package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.TaskData;
import lombok.Builder;
import lombok.Value;

/**
 * The task-derived facts a {@link BonusCondition} predicate evaluates (ADR-0007/0008/0009). Built once
 * per {@code GearSelector.select()} / DPS estimate from the current {@code TaskData}: whether the task
 * counts for the Slayer-helm / Black-mask bonus, whether the monster is undead (Salve), draconic
 * (Dragon hunter), a demon (demonbane), or a kalphite (Keris). Unset flags default to {@code false}.
 *
 * <p>Built via {@link #from(TaskData)} so adding a new task-category predicate is a one-field change
 * here plus a {@code TaskData} flag - no call-site churn. The {@code wilderness} flag is per-LOCATION,
 * not per-task, so it is threaded in separately (ADR-0009 A.5).
 */
@Value
@Builder
public class BonusContext
{
    boolean slayerHelmApplies;
    boolean undead;
    boolean dragon;
    boolean demon;
    boolean kalphite;
    boolean wilderness;

    /** Task-category context for a task (no Wilderness location predicate). */
    public static BonusContext from(TaskData task)
    {
        return from(task, false);
    }

    /**
     * Task-category context plus the per-LOCATION Wilderness predicate (ADR-0009 A.5): {@code
     * wilderness} comes from the user's selected location, NOT the task, so it is threaded in here.
     */
    public static BonusContext from(TaskData task, boolean wilderness)
    {
        if (task == null)
        {
            return BonusContext.builder().wilderness(wilderness).build();
        }
        return BonusContext.builder()
            .slayerHelmApplies(task.isSlayerHelmApplies())
            .undead(task.isUndead())
            .dragon(task.isDragon())
            .demon(task.isDemon())
            .kalphite(task.isKalphite())
            .wilderness(wilderness)
            .build();
    }

    /** Category context for the user-selected monster profile (no Wilderness predicate). */
    public static BonusContext from(MonsterProfile profile)
    {
        return from(profile, false);
    }

    /**
     * Category context built from the selected {@link MonsterProfile} (MV-B2): the category flags
     * follow the variant (FR-5), {@code slayerHelmApplies} comes from the task (carried on the
     * profile), and {@code wilderness} is the per-LOCATION predicate threaded in separately.
     */
    public static BonusContext from(MonsterProfile profile, boolean wilderness)
    {
        if (profile == null)
        {
            return BonusContext.builder().wilderness(wilderness).build();
        }
        return BonusContext.builder()
            .slayerHelmApplies(profile.isSlayerHelmApplies())
            .undead(profile.isUndead())
            .dragon(profile.isDragon())
            .demon(profile.isDemon())
            .kalphite(profile.isKalphite())
            .wilderness(wilderness)
            .build();
    }
}

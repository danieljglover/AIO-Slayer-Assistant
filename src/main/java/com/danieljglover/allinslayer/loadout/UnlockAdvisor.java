package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.RewardData;
import com.danieljglover.allinslayer.model.RewardEffect;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.TaskUnlock;
import com.danieljglover.allinslayer.model.UnlockType;
import java.util.List;
import java.util.OptionalInt;

/**
 * The "what to unlock next" hint (WA-13, ADR-0018 #9d): the highest-value affordable unowned
 * unlock, computed from the player's point balance + ownership (the {@link
 * SlayerUnlockStateProvider} seam), the global rewards catalogue ({@code slayer-meta.json},
 * WA-6/WA-8), and the current task's EXTENSION unlock.
 *
 * <p>Value = points cost among affordable candidates - the big unlocks (helm 400, broads 300) are
 * the valuable ones, so "the most expensive thing you can afford" is the honest simple ranking.
 * COSMETIC recolours are excluded (a 1000-pt recolour is never "what to unlock next" advice). The
 * task's own extension wins an equal-cost tie - it is the task-relevant pick. Null whenever there
 * is nothing honest to say: unknown balance (no guessing), nothing affordable, or everything
 * owned. An EXTENSION whose ownership is unanswerable (no verified varbit - see {@link
 * VarbitSlayerUnlockStateProvider#answersUnlock}) is never a candidate (FR-RV S2). Advice only -
 * nothing here changes gear or maths (NG-4).
 */
final class UnlockAdvisor
{
    private UnlockAdvisor()
    {
    }

    /**
     * @param catalogue the global rewards ({@code SlayerDataService.rewards()}); may be null/empty
     *     (degraded meta resource) - the task extension is then the only candidate source
     * @param task the current task, whose EXTENSION unlock competes; may be null
     * @return the hint line, e.g. {@code "Unlock next: Bigger and Badder (50 pts; you have 60)."},
     *     or null when there is no affordable unowned pick or the balance is unknown
     */
    static String nextUnlockHint(SlayerUnlockStateProvider unlockState, List<RewardData> catalogue,
        TaskData task)
    {
        OptionalInt balance = unlockState.rewardPoints();
        if (!balance.isPresent())
        {
            return null;
        }
        int points = balance.getAsInt();

        String bestName = null;
        int bestCost = -1;

        // The task's EXTENSION unlock is considered FIRST so it wins an equal-cost tie against a
        // global reward (strictly-greater replacement below keeps the first-seen candidate).
        if (task != null && task.getUnlocks() != null)
        {
            for (TaskUnlock unlock : task.getUnlocks())
            {
                // An EXTENSION whose ownership no provider can answer (no verified varbit, no
                // config toggle) reads unowned forever - recommending it would be persistent
                // wrong advice to a player who owns it, so it is never a candidate (FR-RV S2).
                if (unlock == null || unlock.getType() != UnlockType.EXTENSION
                    || unlock.getPointsCost() == null || unlock.getUnlockId() == null
                    || !VarbitSlayerUnlockStateProvider.answersUnlock(unlock.getUnlockId()))
                {
                    continue;
                }
                int cost = unlock.getPointsCost();
                if (cost <= points && !unlockState.ownsUnlock(unlock.getUnlockId()) && cost > bestCost)
                {
                    bestCost = cost;
                    bestName = unlock.getName() != null ? unlock.getName() : unlock.getUnlockId();
                }
            }
        }

        if (catalogue != null)
        {
            for (RewardData reward : catalogue)
            {
                if (reward == null || reward.getRewardId() == null
                    || reward.getEffect() == RewardEffect.COSMETIC)
                {
                    continue;
                }
                int cost = reward.getPointsCost();
                if (cost <= points && !unlockState.ownsUnlock(reward.getRewardId()) && cost > bestCost)
                {
                    bestCost = cost;
                    bestName = reward.getName() != null ? reward.getName() : reward.getRewardId();
                }
            }
        }

        if (bestName == null)
        {
            return null;
        }
        return "Unlock next: " + bestName + " (" + bestCost + " pts; you have " + points + ").";
    }
}

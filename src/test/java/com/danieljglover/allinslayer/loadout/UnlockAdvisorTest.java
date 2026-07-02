package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.RewardData;
import com.danieljglover.allinslayer.model.RewardEffect;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.TaskUnlock;
import com.danieljglover.allinslayer.model.UnlockType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WA-13 (ADR-0018 #9d): the "what to unlock next" hint - the highest-value affordable unowned
 * unlock from the player's points (seam), the global rewards catalogue (WA-6/WA-8), and the
 * current task's EXTENSION unlock. Value = points cost among affordable candidates (the big
 * unlocks are the valuable ones); cosmetic recolours are never advice; the task's own extension
 * wins an equal-cost tie (it is the task-relevant pick). Null whenever there is nothing honest to
 * say: unknown balance, nothing affordable, or everything owned.
 */
public class UnlockAdvisorTest
{
    private static SlayerUnlockStateProvider state(Integer points, String... ownedIds)
    {
        Set<String> owned = new HashSet<>(Arrays.asList(ownedIds));
        return new SlayerUnlockStateProvider()
        {
            @Override
            public boolean ownsUnlock(String rewardOrUnlockId)
            {
                return owned.contains(rewardOrUnlockId);
            }

            @Override
            public OptionalInt rewardPoints()
            {
                return points == null ? OptionalInt.empty() : OptionalInt.of(points);
            }

            @Override
            public Optional<String> currentMaster()
            {
                return Optional.empty();
            }
        };
    }

    private static RewardData reward(String rewardId, String name, int cost, RewardEffect effect)
    {
        RewardData reward = new RewardData();
        reward.setRewardId(rewardId);
        reward.setName(name);
        reward.setPointsCost(cost);
        reward.setEffect(effect);
        return reward;
    }

    private static List<RewardData> catalogue()
    {
        return Arrays.asList(
            reward("malevolent-masquerade", "Malevolent masquerade", 400, RewardEffect.GEAR_UNLOCK),
            reward("broader-fletching", "Broader Fletching", 300, RewardEffect.GEAR_UNLOCK),
            reward("bigger-and-badder", "Bigger and Badder", 50, RewardEffect.SUPERIOR),
            reward("king-black-bonnet", "King black bonnet", 1000, RewardEffect.COSMETIC));
    }

    private static TaskData extendingTask(String unlockId, String name, Integer cost)
    {
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        TaskUnlock extension = new TaskUnlock();
        extension.setUnlockId(unlockId);
        extension.setName(name);
        extension.setPointsCost(cost);
        extension.setType(UnlockType.EXTENSION);
        task.setUnlocks(Collections.singletonList(extension));
        return task;
    }

    @Test
    public void highestValueAffordableUnlockWins()
    {
        String hint = UnlockAdvisor.nextUnlockHint(state(350), catalogue(), null);
        assertEquals("350 points affords Broader Fletching (300) but not the helm (400)",
            "Unlock next: Broader Fletching (300 pts; you have 350).", hint);
    }

    @Test
    public void ownedUnlocksAreSkipped()
    {
        String hint = UnlockAdvisor.nextUnlockHint(
            state(350, "broader-fletching"), catalogue(), null);
        assertTrue("owned Broader Fletching falls through to the next affordable pick",
            hint.contains("Bigger and Badder"));
    }

    @Test
    public void cosmeticRecoloursAreNeverTheAdvice()
    {
        String hint = UnlockAdvisor.nextUnlockHint(state(5000), catalogue(), null);
        assertTrue("the 1000-pt recolour is skipped; the helm unlock is the real advice",
            hint.contains("Malevolent masquerade"));
    }

    @Test
    public void currentTaskExtensionCompetesOnCost()
    {
        TaskData task = extendingTask("augment-my-abbies", "Augment my abbies", 100);
        String hint = UnlockAdvisor.nextUnlockHint(state(100), catalogue(), task);
        assertEquals("the 100-pt extension out-ranks the 50-pt global at 100 points",
            "Unlock next: Augment my abbies (100 pts; you have 100).", hint);
    }

    @Test
    public void taskExtensionWinsAnEqualCostTie()
    {
        TaskData task = extendingTask("augment-my-abbies", "Augment my abbies", 50);
        String hint = UnlockAdvisor.nextUnlockHint(state(60), catalogue(), task);
        assertTrue("equal cost -> the task-relevant extension wins",
            hint.contains("Augment my abbies"));
    }

    @Test
    public void unanswerableExtensionIsNeverTheHint()
    {
        // FR-RV S2: an EXTENSION whose ownership no provider can answer (no verified varbit, no
        // config toggle) reads unowned forever - recommending it would be persistent wrong advice
        // to a player who owns it. Unanswerable candidates are excluded; the answerable global
        // catalogue still advises.
        TaskData task = extendingTask("some-unverified-extension", "Some Unverified Extension", 100);
        String hint = UnlockAdvisor.nextUnlockHint(state(100), catalogue(), task);
        assertTrue("the unanswerable 100-pt extension must lose to the answerable 50-pt global",
            hint.contains("Bigger and Badder"));
        assertNull("no answerable candidate at all -> no hint, never a guess",
            UnlockAdvisor.nextUnlockHint(state(100), null, task));
    }

    @Test
    public void nothingAffordableYieldsNull()
    {
        assertNull(UnlockAdvisor.nextUnlockHint(state(20), catalogue(), null));
    }

    @Test
    public void everythingOwnedYieldsNull()
    {
        assertNull(UnlockAdvisor.nextUnlockHint(
            state(5000, "malevolent-masquerade", "broader-fletching", "bigger-and-badder",
                "king-black-bonnet"),
            catalogue(), null));
    }

    @Test
    public void unknownPointBalanceYieldsNull()
    {
        assertNull("no balance -> no advice, never a guess",
            UnlockAdvisor.nextUnlockHint(state(null), catalogue(), null));
    }

    @Test
    public void emptyCatalogueAndNoTaskYieldNull()
    {
        assertNull(UnlockAdvisor.nextUnlockHint(state(500), Collections.emptyList(), null));
        assertNull(UnlockAdvisor.nextUnlockHint(state(500), null, null));
    }
}

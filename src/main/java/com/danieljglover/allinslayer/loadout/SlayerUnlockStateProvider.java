package com.danieljglover.allinslayer.loadout;

import com.google.inject.ImplementedBy;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Seam over the player's Slayer reward-point/unlock state (WA-9, ADR-0018 #7), in the
 * {@link EquipmentStatsProvider}/{@link ConsumableEffectsProvider} idiom. Consumers (the WA-12
 * unlock-gated gear guards, the WA-13 "what to unlock next" hint, the WA-11 master-context line)
 * depend ONLY on this interface, so the feature ships regardless of where the answers come from.
 *
 * <p>Two implementations: {@link VarbitSlayerUnlockStateProvider} - the DEFAULT binding (WA-10,
 * per PD-A: the WA-0 spike verified ids for all three signals) - and
 * {@link ConfigSlayerUnlockStateProvider} (self-declared via the config panel), which the varbit
 * reader itself delegates to when logged out or for an id with no verified varbit.
 */
@ImplementedBy(VarbitSlayerUnlockStateProvider.class)
public interface SlayerUnlockStateProvider
{
    /**
     * @param rewardOrUnlockId a global {@code rewardId} ({@code rewards/*.json}) or a per-task
     *     {@code unlockId} ({@code unlocks[]})
     * @return true if the player has purchased it; false when unowned, unknown or unanswerable
     *     (an implementation must never guess ownership)
     */
    boolean ownsUnlock(String rewardOrUnlockId);

    /** @return the player's current reward-point balance, or empty when unavailable. */
    OptionalInt rewardPoints();

    /**
     * @return the {@code masterId} of the player's current/assigning master, or empty when no
     *     signal exists (no task, logged out, or the implementation cannot know).
     */
    Optional<String> currentMaster();
}

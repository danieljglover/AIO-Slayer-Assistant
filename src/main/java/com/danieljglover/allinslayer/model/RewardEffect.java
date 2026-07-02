package com.danieljglover.allinslayer.model;

/**
 * The coarse effect tag on a global reward (WA-2, ADR-0018 #6), so consumers (the WA-12 gear
 * guards, the WA-13 unlock-next hint) can find the reward that gates a recommended item without
 * hard-coding names. Exactly these five values - the WA-3 contract pins the source string ->
 * enum mapping 1:1 (see MasterRewardJsonTest).
 */
public enum RewardEffect
{
    GEAR_UNLOCK,
    TASK_UNLOCK,
    SUPERIOR,
    CONVENIENCE,
    COSMETIC
}

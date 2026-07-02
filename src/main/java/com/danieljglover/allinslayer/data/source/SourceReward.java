package com.danieljglover.allinslayer.data.source;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An account-wide Slayer reward-point unlock (WA-3, ADR-0018 #6): the new modular source domain
 * {@code rewards/*.json}, home of the global purchases no per-task {@code unlocks[]} row can hold
 * (Malevolent masquerade, Broader Fletching, Ring bling, ...). Compiled into
 * {@code slayer-meta.json} as {@code RewardData} (WA-8).
 *
 * <p>{@code effect} is authored as the string form of the runtime {@code RewardEffect} tag
 * (GEAR_UNLOCK | TASK_UNLOCK | SUPERIOR | CONVENIENCE | COSMETIC). It stays a String at the
 * source layer so the compiler (WA-8) can FAIL LOUDLY on an unknown value - Gson would silently
 * deserialize a typo'd enum to null.
 */
@Data
@NoArgsConstructor
public class SourceReward
{
    private String rewardId;
    private String name;
    private Integer pointsCost;
    private String effect;
    private String notes;
}

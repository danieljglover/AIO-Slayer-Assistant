package com.danieljglover.allinslayer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An account-wide Slayer reward-point unlock at runtime (WA-2, ADR-0018 #6): the global purchases
 * no per-task {@code unlocks[]} row can hold (Malevolent masquerade, Broader Fletching, Ring
 * bling, ...). Compiled from {@code rewards/*.json} into {@code slayer-meta.json} (WA-8), where
 * the compiler maps the source's STRING effect to {@link RewardEffect} failing loudly - so at
 * runtime an absent key is the only null path for {@code effect}.
 */
@Data
@NoArgsConstructor
public class RewardData
{
    private String rewardId;
    private String name;
    private int pointsCost;
    private RewardEffect effect;
}

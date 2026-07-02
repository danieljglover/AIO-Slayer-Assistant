package com.danieljglover.allinslayer.loadout;

/**
 * The combat stats a potion can positively boost, used to classify a potion by style and rank
 * candidates by boost magnitude (FR-5). HITPOINTS is intentionally excluded - food heal is exposed
 * separately by {@link ConsumableEffectsProvider#healAmount(int)}.
 */
public enum BoostedStat
{
    ATTACK, STRENGTH, DEFENCE, RANGED, MAGIC, PRAYER
}

package com.danieljglover.allinslayer.loadout;

import lombok.Value;

/**
 * The consumable half of a {@link Recommendation} (plan section 5): the best owned food, an optional
 * combo food (cooked karambwan) when owned, the best owned style-matching potion, and - for magic
 * tasks - the {@link MagicSetup}. Each id field is {@code null} when nothing suitable is owned (not
 * an error). Immutable value type so equal recommendations re-render with no rebuild (NFR-4).
 */
@Value
public class Consumables
{
    Integer foodId;
    Integer comboFoodId;
    Integer potionId;
    MagicSetup magic;
}

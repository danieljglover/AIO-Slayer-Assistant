package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A teleport/access item to bring for a {@link SlayerLocation} (Phase 3 travel data), resolved at
 * compile time from the location's authored {@code travel.items} against the {@code items/} catalogue.
 * The trip planner packs the first of {@code itemIds} the player owns; an unowned travel item becomes
 * a "bring X" advisory. Unlike the free-text strategy item names, a travel {@code itemKey} is a
 * structured reference and fails the build if it does not resolve.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelItem
{
    private String name;           // catalogue display name (e.g. "Slayer ring")
    private List<Integer> itemIds; // resolved ids best-first (charge/dose variants)
    private int quantity;          // how many to bring (default 1)
}

package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One authored item reference from a strategy method's {@code requiredOrKeyItems}/{@code inventory}
 * list, resolved at compile time by the {@code ItemNameResolver}. The authored source is free text
 * ("Prayer potions", "Dinh's bulwark or goading potion", "twisted-bow"); this is the structured,
 * runtime-usable form.
 *
 * <p>Resolution never fabricates ids (the {@link StrategyWeapon} discipline): when a name cannot be
 * matched to the item catalogue or the weapon catalogue, {@code itemIds} is empty and the ref carries
 * only its display {@code name} - an honest advisory the engine can surface without pretending to
 * know an id. {@code supply} distinguishes a consumable/supply (matched via the {@code items/}
 * catalogue - the trip planner may pack it) from gear (matched via {@code weapons/} - handled by the
 * gear selector, never packed as an inventory slot).
 *
 * <p>{@code alternatives} holds the parsed "X or Y" options after the first ("Prayer potions or super
 * restores" -> name = "Prayer potions", alternatives = [super restores]); each alternative is itself a
 * resolved ref with no further nesting. {@code quantity} is the parsed leading/trailing count when the
 * author stated one ("2 Divine super combat potions" -> 2), else null (unspecified).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyItemRef
{
    private String name;                     // display name of the primary option (cleaned of qualifier clauses)
    private List<Integer> itemIds;           // resolved ids best-first; empty = unresolved (advisory-only)
    private boolean supply;                  // true = catalogue supply (packable); false = gear or unresolved
    private List<StrategyItemRef> alternatives; // parsed "or" options after the first; null when none
    private Integer quantity;                // parsed count; null = unspecified
}

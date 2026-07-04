package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An {@code items/*.json} row: the build-time supply-item catalogue that resolves the free-text
 * item names in strategy {@code requiredOrKeyItems}/{@code inventory} lists to real item ids for the
 * dynamic trip planner. The supply counterpart to {@link SourceWeapon} (weapons stay in
 * {@code weapons/}; this holds consumables/supplies - potions, cannon parts, ammo, teleports, bags,
 * bracelets).
 *
 * <p>{@code itemIds} is best-first (e.g. a 4-dose potion before its 1-dose, a charged variant before
 * uncharged) - the {@code SourceWeapon} and {@code InventorySelector} preference-order idiom.
 * {@code aliases} are alternate spellings the resolver also matches (case/punctuation-insensitive).
 * {@code dosed} marks potions so the planner can reason in doses; {@code stackable} marks items that
 * occupy one inventory slot regardless of quantity (cannonballs, runes, teleport tablets).
 */
@Data
@NoArgsConstructor
public class SourceItem
{
    private String itemKey;
    private String name;
    private List<String> aliases;
    private List<Integer> itemIds;
    private boolean stackable;
    private boolean dosed;
}

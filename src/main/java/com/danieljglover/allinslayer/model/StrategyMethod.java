package com.danieljglover.allinslayer.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One playable method of a monster's {@code /Strategies} guide (e.g. "Catacombs magic", "Cannon +
 * Salve"), carried through the compiler from {@code strategies/&lt;id&gt;/strategy.json} {@code methods[]}
 * to runtime so the trip planner and {@code MethodPicker} can size and pick a bag from what the guide
 * actually prescribes. Previously the compiler kept only the thin weapon list; this is the additive
 * per-method detail.
 *
 * <p>A {@code combatStyle == null} method (typically {@code role == "general"}) documents
 * strategy-wide key items rather than a pickable style; the picker filters it out of the style choice
 * but the planner still reads its {@code keyItems}. Every list is nullable (Gson default) - an absent
 * field means the author did not specify it, and the layered planner falls back accordingly (the
 * sparse-data contract: {@code requiredOrKeyItems} + prayers-derived sustain always work, authored
 * {@code inventory} refines where present).
 */
@Data
@NoArgsConstructor
public class StrategyMethod
{
    private String methodId;
    private String label;
    private CombatStyle combatStyle;                          // null = not a pickable style (e.g. role=general)
    private String role;                                     // general/default/alternative/wilderness/single-target/...
    private String summary;
    private List<StrategyItemRef> keyItems;                  // resolved from requiredOrKeyItems
    private List<String> prayers;                            // verbatim ("Protect from Melee", "Piety", ...)
    private Map<EquipmentSlot, List<StrategyItemRef>> equipment; // resolved from equipment.slots (parsable slots only)
    private List<StrategyItemRef> inventory;                 // resolved from the authored inventory[]
    private List<String> risks;
}

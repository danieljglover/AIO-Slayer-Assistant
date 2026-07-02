package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.ui.ItemIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Dimension;
import java.util.Collections;
import java.util.Map;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The at-a-glance worn-gear grid - the in-game equipment cross so players read gear by position
 * (ui-recommendations.md §2.2). A {@code DynamicGridLayout(5, 3)} of 15 {@link EquipmentSlotCell}s:
 * the 11 {@link EquipmentSlot}s placed in their canonical OSRS positions plus 4 blank wells that hold
 * the cross shape.
 *
 * <p>Sprites are drawn through the injected {@link ItemIconRenderer} seam (ADR-0001); worn gear is
 * never stackable and never dimmed.</p>
 */
public class EquipmentGrid extends JPanel
{
    /**
     * Cell-by-cell slot placement, row-major across the 5x3 cross. {@code null} entries are the
     * blank corner wells.
     * <pre>
     * row0:   .       HEAD     .
     * row1:  CAPE    AMULET   AMMO
     * row2: WEAPON   BODY     SHIELD
     * row3:   .       LEGS     .
     * row4:  HANDS    FEET     RING
     * </pre>
     */
    static final EquipmentSlot[] CELL_SLOTS = {
        null, EquipmentSlot.HEAD, null,
        EquipmentSlot.CAPE, EquipmentSlot.AMULET, EquipmentSlot.AMMO,
        EquipmentSlot.WEAPON, EquipmentSlot.BODY, EquipmentSlot.SHIELD,
        null, EquipmentSlot.LEGS, null,
        EquipmentSlot.HANDS, EquipmentSlot.FEET, EquipmentSlot.RING
    };

    public EquipmentGrid(ItemIconRenderer renderer, Map<EquipmentSlot, Integer> worn,
        Map<Integer, String> itemNames)
    {
        super(new DynamicGridLayout(5, 3, SlayerTheme.SPACE_1, SlayerTheme.SPACE_1));
        setName("equipment-grid");
        setOpaque(false);
        // Cap + left-anchor so a BoxLayout-Y card body cannot stretch the grid. With max == pref,
        // DynamicGridLayout's width scale stays 1.0 and the cells keep their 38x34 size (W8 F1).
        setAlignmentX(LEFT_ALIGNMENT);

        Map<EquipmentSlot, Integer> wornGear = worn == null ? Collections.emptyMap() : worn;
        Map<Integer, String> names = itemNames == null ? Collections.emptyMap() : itemNames;

        for (EquipmentSlot slot : CELL_SLOTS)
        {
            Integer itemId = slot == null ? null : wornGear.get(slot);
            EquipmentSlotCell cell = itemId != null
                ? new EquipmentSlotCell(renderer, itemId, 1, false, names.get(itemId))
                : new EquipmentSlotCell();
            // Name by slot position (stable across fills) so the cross layout is testable; only the
            // four true corner wells are "empty".
            cell.setName(slot == null ? "equip-cell-empty" : "equip-cell-" + slot.name());
            add(cell);
        }
    }

    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }
}

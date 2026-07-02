package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.ItemIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The consumables grid - 4-wide like the real inventory bag (ui-recommendations.md §2.3). One
 * stackable {@link EquipmentSlotCell} per item so rune / dose stack numbers can render.
 *
 * <p>Per R4 the {@code Recommendation.inventory} model carries no per-item quantities, so cells
 * render at {@code qty=1} (a deliberate, documented limitation - the stack number stays available
 * for when quantities are added to the model).</p>
 */
public class InventoryGrid extends JPanel
{
    public InventoryGrid(ItemIconRenderer renderer, List<Integer> inventory, Map<Integer, String> itemNames)
    {
        super(new DynamicGridLayout(rows(inventory), 4, SlayerTheme.SPACE_1, SlayerTheme.SPACE_1));
        setName("inventory-grid");
        setOpaque(false);
        // Cap + left-anchor so a BoxLayout-Y card body cannot stretch the grid. With max == pref,
        // DynamicGridLayout's width scale stays 1.0 and the cells keep their 38x34 size (W8 F1).
        setAlignmentX(LEFT_ALIGNMENT);

        List<Integer> items = inventory == null ? Collections.emptyList() : inventory;
        Map<Integer, String> names = itemNames == null ? Collections.emptyMap() : itemNames;

        for (int i = 0; i < items.size(); i++)
        {
            int itemId = items.get(i);
            EquipmentSlotCell cell = new EquipmentSlotCell(renderer, itemId, 1, true, names.get(itemId));
            cell.setName("inv-cell-" + i);
            add(cell);
        }
    }

    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    private static int rows(List<Integer> inventory)
    {
        int count = inventory == null ? 0 : inventory.size();
        return (count + 3) / 4; // ceil(count / 4); 0 when empty (a valid 0-row, 4-col grid)
    }
}

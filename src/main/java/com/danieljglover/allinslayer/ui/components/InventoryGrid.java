package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.loadout.LoadoutDiff;
import com.danieljglover.allinslayer.loadout.TripSlot;
import com.danieljglover.allinslayer.ui.ItemIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The full 28-slot inventory bag - a 4-wide x 7-tall grid like the in-game inventory
 * (ui-recommendations.md §2.3). The first {@code min(slots.size(), 28)} cells render the suggested
 * trip items (owned supplies, potions, runes, combo food, food) composed by {@code
 * TripInventoryPlanner}; the remaining cells are muted empty wells that pad the unfilled slots.
 *
 * <p>Quantities and stackability come from each {@link TripSlot}, so rune / dose stack numbers render
 * (unlike the old quantity-less pack list). Sprites draw through the injected {@link ItemIconRenderer}
 * seam (ADR-0001); owned items are never dimmed.</p>
 */
public class InventoryGrid extends JPanel
{
    /** The OSRS inventory size - always this many cells (filled then empty wells). */
    private static final int CAPACITY = 28;

    public InventoryGrid(ItemIconRenderer renderer, List<TripSlot> slots, Map<Integer, String> itemNames)
    {
        this(renderer, slots, itemNames, null);
    }

    /**
     * @param diff optional per-item carry status (Phase 2); when non-null each filled cell is tinted
     *     carried/partial/missing vs what the player is holding. Null renders the neutral grid.
     */
    public InventoryGrid(ItemIconRenderer renderer, List<TripSlot> slots, Map<Integer, String> itemNames,
        Map<Integer, LoadoutDiff.Status> diff)
    {
        super(new DynamicGridLayout(7, 4, SlayerTheme.SPACE_1, SlayerTheme.SPACE_1));
        setName("inventory-grid");
        setOpaque(false);
        // Cap + left-anchor so a BoxLayout-Y card body cannot stretch the grid. With max == pref,
        // DynamicGridLayout's width scale stays 1.0 and the cells keep their 38x34 size (W8 F1).
        setAlignmentX(LEFT_ALIGNMENT);

        List<TripSlot> items = slots == null ? Collections.emptyList() : slots;
        Map<Integer, String> names = itemNames == null ? Collections.emptyMap() : itemNames;

        int filled = Math.min(items.size(), CAPACITY);
        for (int i = 0; i < CAPACITY; i++)
        {
            EquipmentSlotCell cell;
            if (i < filled)
            {
                TripSlot slot = items.get(i);
                cell = new EquipmentSlotCell(renderer, slot.getItemId(), slot.getQuantity(),
                    slot.isStackable(), names.get(slot.getItemId()),
                    diff == null ? null : diff.get(slot.getItemId()));
                cell.setName("inv-cell-" + i);
            }
            else
            {
                cell = new EquipmentSlotCell();
                cell.setName("inv-cell-empty");
            }
            add(cell);
        }
    }

    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }
}

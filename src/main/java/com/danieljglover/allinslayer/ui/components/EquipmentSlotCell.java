package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.ItemIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

/**
 * One sprite cell in {@link EquipmentGrid} / {@link InventoryGrid} (ui-recommendations.md §2.2, §2.3).
 *
 * <p>A fixed {@code size/grid-cell} (38x34) well. A filled cell is a bordered {@code surface/card}
 * recess that draws its item through the injected {@link ItemIconRenderer} seam (owned, so never
 * dimmed), shows the item name on its tooltip, and offers the shared right-click Wiki lookup. An
 * empty cell is a muted, border-less {@code border/divider} well (darker than the card it sits on)
 * that recedes so the equipment cross keeps its shape without a bright empty void (W8 F4 / RV3 N-A).</p>
 */
class EquipmentSlotCell extends JLabel
{
    /** A blank, muted well (keeps the grid shape but recedes; no sprite, tooltip or popup). */
    EquipmentSlotCell()
    {
        configureWell();
        // Empty wells recede: a recessed tone DARKER than the surrounding card, no bright outline,
        // so the cross keeps its shape without a void or a lighter patch that advances (W8 F4 / RV3 N-A).
        setBackground(SlayerTheme.BORDER_DIVIDER);
        setBorder(null);
    }

    /** A filled cell rendering {@code itemId} through the seam. */
    EquipmentSlotCell(ItemIconRenderer renderer, int itemId, int quantity, boolean stackable, String name)
    {
        configureWell();
        // Filled wells keep the bordered card recess so they read as occupied (W8 F4).
        setBackground(SlayerTheme.SURFACE_CARD);
        setBorder(new LineBorder(SlayerTheme.BORDER_DIVIDER, 1));
        setToolTipText(name);
        setComponentPopupMenu(ItemWiki.popup(itemId));
        renderer.render(this, itemId, quantity, stackable, false);
    }

    private void configureWell()
    {
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        Dimension size = new Dimension(SlayerTheme.GRID_CELL_WIDTH, SlayerTheme.GRID_CELL_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }
}

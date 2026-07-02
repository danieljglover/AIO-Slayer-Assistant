package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.componentByName;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.ui.components.ComponentTestSupport.RecordingIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import org.junit.Test;

/**
 * T12 - InventoryGrid. The 4-wide consumables grid; each id is drawn stackable so rune/dose stack
 * numbers can render. Per R4 the model carries no quantities, so cells render at qty=1 (a known gap).
 */
public class InventoryGridTest
{
    private static final List<Integer> INVENTORY = Arrays.asList(2444, 6685, 12695, 560, 565);

    private static Map<Integer, String> names()
    {
        Map<Integer, String> names = new HashMap<>();
        names.put(2444, "Ranging potion(4)");
        names.put(6685, "Saradomin brew(4)");
        names.put(12695, "Super combat potion(4)");
        names.put(560, "Death rune");
        names.put(565, "Blood rune");
        return names;
    }

    @Test
    public void usesFourWideDynamicGridWithCeilingRows()
    {
        InventoryGrid grid = buildOnEdt(() -> new InventoryGrid(new RecordingIconRenderer(), INVENTORY, names()));

        assertTrue(grid.getLayout() instanceof DynamicGridLayout);
        DynamicGridLayout layout = (DynamicGridLayout) grid.getLayout();
        assertEquals("ceil(5 / 4) = 2 rows", 2, layout.getRows());
        assertEquals(4, layout.getColumns());
        assertEquals(SlayerTheme.SPACE_1, layout.getHgap());
        assertEquals(SlayerTheme.SPACE_1, layout.getVgap());

        assertEquals("one cell per inventory item", INVENTORY.size(), grid.getComponentCount());
        for (Component cell : grid.getComponents())
        {
            assertTrue(cell instanceof EquipmentSlotCell);
        }
    }

    @Test
    public void rendersEachIdStackableInOrderWithTooltip()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        InventoryGrid grid = buildOnEdt(() -> new InventoryGrid(renderer, INVENTORY, names()));

        assertEquals(INVENTORY.size(), renderer.calls.size());
        for (int i = 0; i < INVENTORY.size(); i++)
        {
            EquipmentSlotCell cell = componentByName(grid, "inv-cell-" + i, EquipmentSlotCell.class);
            RecordingIconRenderer.Call call = renderer.callFor(cell);
            assertEquals((int) INVENTORY.get(i), call.itemId);
            assertEquals("R4: no model quantities, so qty=1", 1, call.quantity);
            assertTrue("inventory items are stackable so stack numbers can show", call.stackable);
            assertEquals("consumables are owned, never dimmed", false, call.dim);
            assertEquals(names().get(INVENTORY.get(i)), cell.getToolTipText());
        }
    }

    @Test
    public void emptyInventoryProducesNoCells()
    {
        InventoryGrid grid = buildOnEdt(() -> new InventoryGrid(new RecordingIconRenderer(), null, null));
        assertEquals(0, grid.getComponentCount());
    }

    // ---- W8 F1/F6: capped + left-anchored so DynamicGridLayout never scales the cells --------------

    @Test
    public void maximumSizeEqualsPreferredSizeAndIsLeftAnchored()
    {
        InventoryGrid grid = buildOnEdt(() -> new InventoryGrid(new RecordingIconRenderer(), INVENTORY, names()));

        assertEquals("max == pref so a BoxLayout-Y card body cannot stretch the grid (F1)",
            grid.getPreferredSize(), grid.getMaximumSize());
        assertEquals("the grid is left-anchored in the card body (F1)",
            Component.LEFT_ALIGNMENT, grid.getAlignmentX(), 0.0f);
    }

    @Test
    public void cellsStayAtTheGridCellWidthWhenOfferedExtraWidth()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        InventoryGrid grid = buildOnEdt(() -> new InventoryGrid(renderer, INVENTORY, names()));

        JPanel host = buildOnEdt(() ->
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(grid);
            return panel;
        });
        runOnEdt(() ->
        {
            host.setSize(400, 400);
            layoutTree(host);
        });

        EquipmentSlotCell first = componentByName(grid, "inv-cell-0", EquipmentSlotCell.class);
        assertEquals("the cap keeps DynamicGridLayout's scale at 1.0 so cells never balloon (F1/F6)",
            SlayerTheme.GRID_CELL_WIDTH, first.getWidth());
    }

    /** Recursive top-down {@code doLayout} so headless layout propagates parent sizes to children. */
    private static void layoutTree(Component component)
    {
        component.doLayout();
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                layoutTree(child);
            }
        }
    }
}

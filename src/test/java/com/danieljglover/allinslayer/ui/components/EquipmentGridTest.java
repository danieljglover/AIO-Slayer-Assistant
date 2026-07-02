package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.componentByName;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.ui.components.ComponentTestSupport.RecordingIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import org.junit.Test;

/**
 * T11 - EquipmentGrid (+ EquipmentSlotCell). A 5x3 OSRS cross of 15 cells (11 slots + 4 blanks),
 * filled cells drawing the worn sprite through the {@link RecordingIconRenderer} seam.
 */
public class EquipmentGridTest
{
    private static final int HEAD_ID = 10828;
    private static final int WEAPON_ID = 4151;
    private static final int BODY_ID = 4720;
    private static final int RING_ID = 6737;

    private static Map<EquipmentSlot, Integer> worn()
    {
        Map<EquipmentSlot, Integer> worn = new HashMap<>();
        worn.put(EquipmentSlot.HEAD, HEAD_ID);
        worn.put(EquipmentSlot.WEAPON, WEAPON_ID);
        worn.put(EquipmentSlot.BODY, BODY_ID);
        worn.put(EquipmentSlot.RING, RING_ID);
        return worn;
    }

    private static Map<Integer, String> names()
    {
        Map<Integer, String> names = new HashMap<>();
        names.put(HEAD_ID, "Neitiznot faceguard");
        names.put(WEAPON_ID, "Abyssal whip");
        names.put(BODY_ID, "Bandos chestplate");
        names.put(RING_ID, "Berserker ring");
        return names;
    }

    @Test
    public void usesFiveByThreeDynamicGridWithFifteenCells()
    {
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(new RecordingIconRenderer(), worn(), names()));

        assertTrue(grid.getLayout() instanceof DynamicGridLayout);
        DynamicGridLayout layout = (DynamicGridLayout) grid.getLayout();
        assertEquals(5, layout.getRows());
        assertEquals(3, layout.getColumns());
        assertEquals(SlayerTheme.SPACE_1, layout.getHgap());
        assertEquals(SlayerTheme.SPACE_1, layout.getVgap());

        assertEquals(15, grid.getComponentCount());
        for (Component cell : grid.getComponents())
        {
            assertTrue("every cell is an EquipmentSlotCell", cell instanceof EquipmentSlotCell);
        }
    }

    @Test
    public void cellsFollowTheCanonicalOsrsCrossOrder()
    {
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(new RecordingIconRenderer(), worn(), names()));

        // row0: . HEAD . | row1: CAPE AMULET AMMO | row2: WEAPON BODY SHIELD
        // row3: . LEGS . | row4: HANDS FEET RING
        String[] expected = {
            "equip-cell-empty", "equip-cell-HEAD", "equip-cell-empty",
            "equip-cell-CAPE", "equip-cell-AMULET", "equip-cell-AMMO",
            "equip-cell-WEAPON", "equip-cell-BODY", "equip-cell-SHIELD",
            "equip-cell-empty", "equip-cell-LEGS", "equip-cell-empty",
            "equip-cell-HANDS", "equip-cell-FEET", "equip-cell-RING"
        };
        for (int i = 0; i < expected.length; i++)
        {
            assertEquals("cell " + i + " is in the wrong slot position",
                expected[i], grid.getComponent(i).getName());
        }
    }

    @Test
    public void filledCellsAskTheRendererForTheRightIdUnstackedWithTooltip()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(renderer, worn(), names()));

        assertEquals("one render per worn item, none for blanks", 4, renderer.calls.size());

        EquipmentSlotCell weapon = componentByName(grid, "equip-cell-WEAPON", EquipmentSlotCell.class);
        RecordingIconRenderer.Call call = renderer.callFor(weapon);
        assertNotNull(call);
        assertEquals(WEAPON_ID, call.itemId);
        assertEquals(1, call.quantity);
        assertFalse("worn gear is not stackable", call.stackable);
        assertFalse("worn gear is owned, never dimmed", call.dim);
        assertEquals("Abyssal whip", weapon.getToolTipText());

        EquipmentSlotCell head = componentByName(grid, "equip-cell-HEAD", EquipmentSlotCell.class);
        assertEquals(HEAD_ID, renderer.callFor(head).itemId);
        assertEquals("Neitiznot faceguard", head.getToolTipText());
    }

    @Test
    public void emptyCellsAreBlankAndCarryNoSpriteRequest()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(renderer, worn(), names()));

        Component topLeft = grid.getComponent(0);
        assertTrue(topLeft instanceof EquipmentSlotCell);
        EquipmentSlotCell empty = (EquipmentSlotCell) topLeft;
        assertNull("blank cells request no sprite", renderer.callFor(empty));
        assertNull("blank cells have no icon", empty.getIcon());
        assertNull("blank cells have no tooltip", empty.getToolTipText());
    }

    @Test
    public void cellsAreRecessedWellsAtTheGridCellSize()
    {
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(new RecordingIconRenderer(), worn(), names()));

        EquipmentSlotCell weapon = componentByName(grid, "equip-cell-WEAPON", EquipmentSlotCell.class);
        assertEquals(SlayerTheme.SURFACE_CARD, weapon.getBackground());
        assertEquals(SlayerTheme.GRID_CELL_WIDTH, weapon.getPreferredSize().width);
        assertEquals(SlayerTheme.GRID_CELL_HEIGHT, weapon.getPreferredSize().height);
        assertTrue(weapon.getBorder() instanceof javax.swing.border.LineBorder);
        javax.swing.border.LineBorder border = (javax.swing.border.LineBorder) weapon.getBorder();
        assertEquals(SlayerTheme.BORDER_DIVIDER, border.getLineColor());
    }

    @Test
    public void filledCellsOfferAWikiLookup()
    {
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(new RecordingIconRenderer(), worn(), names()));

        EquipmentSlotCell weapon = componentByName(grid, "equip-cell-WEAPON", EquipmentSlotCell.class);
        assertNotNull("filled cells carry the wiki popup", weapon.getComponentPopupMenu());
    }

    @Test
    public void toleratesNullWornAndNames()
    {
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(new RecordingIconRenderer(), null, null));
        assertEquals(15, grid.getComponentCount());
    }

    // ---- W8 F1/F6: the grid is capped + left-anchored so DynamicGridLayout never scales the cells ----

    @Test
    public void maximumSizeEqualsPreferredSizeAndIsLeftAnchored()
    {
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(new RecordingIconRenderer(), worn(), names()));

        assertEquals("max == pref so a BoxLayout-Y card body cannot stretch the grid (F1)",
            grid.getPreferredSize(), grid.getMaximumSize());
        assertEquals("the grid is left-anchored in the card body (F1)",
            Component.LEFT_ALIGNMENT, grid.getAlignmentX(), 0.0f);
    }

    @Test
    public void filledCellsStayAtTheGridCellWidthWhenOfferedExtraWidth()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(renderer, worn(), names()));

        // Mirror the real usage: the grid sits in a BoxLayout-Y card body far wider than the cross.
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

        EquipmentSlotCell weapon = componentByName(grid, "equip-cell-WEAPON", EquipmentSlotCell.class);
        assertEquals("the cap keeps DynamicGridLayout's scale at 1.0 so cells never balloon (F1/F6)",
            SlayerTheme.GRID_CELL_WIDTH, weapon.getWidth());
        assertEquals(SlayerTheme.GRID_CELL_HEIGHT, weapon.getHeight());
    }

    // ---- W8 F4: empty wells recede (muted, border-less) so the cross has no bright void --------------

    @Test
    public void emptyWellsRecedeMutedAndBorderlessWhileFilledWellsKeepTheBorderedCard()
    {
        EquipmentGrid grid = buildOnEdt(() -> new EquipmentGrid(new RecordingIconRenderer(), worn(), names()));

        EquipmentSlotCell empty = (EquipmentSlotCell) grid.getComponent(0); // the top-left corner well
        assertEquals("equip-cell-empty", empty.getName());
        assertEquals("empty wells recede into a recessed tone darker than the card (F4 / RV3 N-A)",
            SlayerTheme.BORDER_DIVIDER, empty.getBackground());
        assertNull("empty wells drop the bright outline so the cross has no void (F4)", empty.getBorder());

        EquipmentSlotCell weapon = componentByName(grid, "equip-cell-WEAPON", EquipmentSlotCell.class);
        assertEquals("filled wells keep the bordered card look (F4)",
            SlayerTheme.SURFACE_CARD, weapon.getBackground());
        assertTrue("filled wells keep the divider outline (F4)",
            weapon.getBorder() instanceof javax.swing.border.LineBorder);
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

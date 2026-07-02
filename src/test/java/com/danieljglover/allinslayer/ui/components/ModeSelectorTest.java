package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.DefaultButtonModel;
import org.junit.Test;

/**
 * T08 - ModeSelector. Two toggles in a ButtonGroup; active segment = brand; clicking the inactive
 * segment fires the callback, re-clicking the active one does not.
 */
public class ModeSelectorTest
{
    @Test
    public void bothSegmentsShareAButtonGroupWithDpsActiveInBrand()
    {
        ModeSelector selector = buildOnEdt(() -> new ModeSelector(AdviceMode.DPS, mode -> { }));

        assertEquals("DPS", selector.getDpsButton().getText());
        assertEquals("Cost", selector.getCostButton().getText());
        assertEquals(SlayerTheme.TYPE_CAPTION, selector.getDpsButton().getFont());

        DefaultButtonModel dpsModel = (DefaultButtonModel) selector.getDpsButton().getModel();
        DefaultButtonModel costModel = (DefaultButtonModel) selector.getCostButton().getModel();
        assertNotNull("Segments must belong to a ButtonGroup", dpsModel.getGroup());
        assertSame("Both segments must share one ButtonGroup", dpsModel.getGroup(), costModel.getGroup());

        assertTrue(selector.getDpsButton().isSelected());
        assertFalse(selector.getCostButton().isSelected());
        assertEquals(SlayerTheme.ACCENT_BRAND, selector.getDpsButton().getForeground());
        assertEquals(SlayerTheme.TEXT_SECONDARY, selector.getCostButton().getForeground());
    }

    @Test
    public void initialModeCostSelectsCostSegment()
    {
        ModeSelector selector = buildOnEdt(() -> new ModeSelector(AdviceMode.COST, mode -> { }));

        assertEquals(AdviceMode.COST, selector.getMode());
        assertTrue(selector.getCostButton().isSelected());
        assertEquals(SlayerTheme.ACCENT_BRAND, selector.getCostButton().getForeground());
        assertEquals(SlayerTheme.TEXT_SECONDARY, selector.getDpsButton().getForeground());
    }

    @Test
    public void clickingInactiveSegmentFiresCallbackAndMovesBrand()
    {
        AtomicReference<AdviceMode> fired = new AtomicReference<>();
        ModeSelector selector = buildOnEdt(() -> new ModeSelector(AdviceMode.DPS, fired::set));

        runOnEdt(() -> selector.getCostButton().doClick());

        assertEquals(AdviceMode.COST, fired.get());
        assertEquals(AdviceMode.COST, selector.getMode());
        assertTrue(selector.getCostButton().isSelected());
        assertEquals(SlayerTheme.ACCENT_BRAND, selector.getCostButton().getForeground());
        assertEquals(SlayerTheme.TEXT_SECONDARY, selector.getDpsButton().getForeground());
    }

    @Test
    public void clickingTheActiveSegmentDoesNotFireCallback()
    {
        AtomicReference<AdviceMode> fired = new AtomicReference<>();
        ModeSelector selector = buildOnEdt(() -> new ModeSelector(AdviceMode.DPS, fired::set));

        runOnEdt(() -> selector.getDpsButton().doClick());

        assertNull("Re-clicking the active segment must not trigger a recompute", fired.get());
        assertEquals(AdviceMode.DPS, selector.getMode());
    }

    @Test
    public void setModeReflectsSelectionWithoutFiringCallback()
    {
        AtomicReference<AdviceMode> fired = new AtomicReference<>();
        ModeSelector selector = buildOnEdt(() -> new ModeSelector(AdviceMode.DPS, fired::set));

        runOnEdt(() -> selector.setMode(AdviceMode.COST));

        assertNull("Programmatic setMode is non-destructive, no callback", fired.get());
        assertEquals(AdviceMode.COST, selector.getMode());
        assertTrue(selector.getCostButton().isSelected());
    }
}

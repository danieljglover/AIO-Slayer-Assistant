package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/**
 * T13 - ActionBar. A 24px toolbar: WEST a {@link ModeSelector}, EAST a right-aligned strip of two
 * borderless {@link IconButton}s (refresh, export). Export is disabled with no recommendation (FR-9).
 */
public class ActionBarTest
{
    private static ActionBar bar(boolean hasRecommendation, AtomicReference<AdviceMode> mode,
        AtomicInteger refreshes, AtomicInteger exports)
    {
        return buildOnEdt(() -> new ActionBar(
            AdviceMode.DPS,
            mode::set,
            refreshes::incrementAndGet,
            exports::incrementAndGet,
            hasRecommendation));
    }

    private static Component west(ActionBar bar)
    {
        return ((BorderLayout) bar.getLayout()).getLayoutComponent(BorderLayout.WEST);
    }

    private static Container east(ActionBar bar)
    {
        return (Container) ((BorderLayout) bar.getLayout()).getLayoutComponent(BorderLayout.EAST);
    }

    @Test
    public void laysModeSelectorWestAndTwoIconButtonsEast()
    {
        ActionBar bar = bar(true, new AtomicReference<>(), new AtomicInteger(), new AtomicInteger());

        assertTrue(bar.getLayout() instanceof BorderLayout);
        assertEquals(SlayerTheme.SURFACE_PAGE, bar.getBackground());

        assertTrue("DPS|Cost lives on the west", west(bar) instanceof ModeSelector);
        assertSame(west(bar), bar.getModeSelector());

        Container east = east(bar);
        assertTrue("actions are right-aligned", east.getLayout() instanceof FlowLayout);
        int iconButtons = 0;
        for (Component child : east.getComponents())
        {
            if (child instanceof IconButton)
            {
                iconButtons++;
            }
        }
        assertEquals("refresh + export", 2, iconButtons);

        assertEquals("Refresh", bar.getRefreshButton().getToolTipText());
        assertEquals("Export loadout", bar.getExportButton().getToolTipText());
        assertNotNull("icons load from /icons/*.png", bar.getRefreshButton().getIcon());
        assertNotNull(bar.getExportButton().getIcon());
    }

    @Test
    public void barIsOneCompactRowTall()
    {
        ActionBar bar = bar(true, new AtomicReference<>(), new AtomicInteger(), new AtomicInteger());

        assertEquals(SlayerTheme.ACTION_BAR_HEIGHT, bar.getPreferredSize().height);
        assertEquals("the bar must not stretch vertically in the header band",
            SlayerTheme.ACTION_BAR_HEIGHT, bar.getMaximumSize().height);
    }

    @Test
    public void exportIsDisabledWithoutARecommendation()
    {
        ActionBar bar = bar(false, new AtomicReference<>(), new AtomicInteger(), new AtomicInteger());

        assertFalse("FR-9: export only with a recommendation", bar.getExportButton().isEnabled());
        assertTrue("refresh is always available", bar.getRefreshButton().isEnabled());
    }

    @Test
    public void exportIsEnabledWithARecommendation()
    {
        ActionBar bar = bar(true, new AtomicReference<>(), new AtomicInteger(), new AtomicInteger());
        assertTrue(bar.getExportButton().isEnabled());
    }

    @Test
    public void buttonsAndModeFireTheirCallbacks()
    {
        AtomicReference<AdviceMode> mode = new AtomicReference<>();
        AtomicInteger refreshes = new AtomicInteger();
        AtomicInteger exports = new AtomicInteger();
        ActionBar bar = bar(true, mode, refreshes, exports);

        runOnEdt(() -> bar.getRefreshButton().doClick());
        runOnEdt(() -> bar.getExportButton().doClick());
        runOnEdt(() -> bar.getModeSelector().getCostButton().doClick());

        assertEquals(1, refreshes.get());
        assertEquals(1, exports.get());
        assertEquals(AdviceMode.COST, mode.get());
    }

    @Test
    public void setExportEnabledAndSetModeUpdateInPlace()
    {
        AtomicReference<AdviceMode> mode = new AtomicReference<>();
        ActionBar bar = bar(true, mode, new AtomicInteger(), new AtomicInteger());

        runOnEdt(() -> bar.setExportEnabled(false));
        assertFalse(bar.getExportButton().isEnabled());

        runOnEdt(() -> bar.setMode(AdviceMode.COST));
        assertEquals(AdviceMode.COST, bar.getModeSelector().getMode());
        assertEquals("setMode is non-destructive: no spurious recompute", null, mode.get());
    }
}

package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.labelTexts;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import org.junit.Test;

/**
 * T03 - the headless-Swing / FontManager canary for the leaf library. If the bundled RuneScape
 * fonts fail to resolve headless, this is where it surfaces first (plan §8).
 */
public class KeyValueRowTest
{
    @Test
    public void keyUsesCaptionSecondaryAndValueUsesBodyPrimary()
    {
        KeyValueRow row = buildOnEdt(() -> new KeyValueRow("Slayer", "85 / 85"));

        JLabel key = row.getKeyLabel();
        assertEquals("Slayer", key.getText());
        assertEquals(SlayerTheme.TYPE_CAPTION, key.getFont());
        assertEquals(SlayerTheme.TEXT_SECONDARY, key.getForeground());

        JLabel value = row.getValueLabel();
        assertEquals("85 / 85", value.getText());
        assertEquals(SlayerTheme.TYPE_BODY, value.getFont());
        assertEquals(SlayerTheme.TEXT_PRIMARY, value.getForeground());
    }

    @Test
    public void usesGridBagLayoutCardBackgroundAndRowHeight()
    {
        KeyValueRow row = buildOnEdt(() -> new KeyValueRow("Weakness", "Fire"));

        assertTrue(row.getLayout() instanceof GridBagLayout);
        assertEquals(SlayerTheme.SURFACE_CARD, row.getBackground());
        assertEquals(SlayerTheme.ROW_HEIGHT, row.getPreferredSize().height);
        assertEquals(SlayerTheme.KEY_COL_WIDTH, row.getKeyLabel().getPreferredSize().width);
    }

    @Test
    public void rendersPlainTextWithoutHtml()
    {
        KeyValueRow row = buildOnEdt(() -> new KeyValueRow("Required", "Fire cape"));

        for (String text : labelTexts(row))
        {
            assertFalse("KeyValueRow must not use <html>: " + text,
                text.toLowerCase().contains("<html"));
        }
    }

    @Test
    public void setValueMutatesTheSameLabelInPlace()
    {
        KeyValueRow row = buildOnEdt(() -> new KeyValueRow("Remaining", "147"));
        JLabel value = row.getValueLabel();

        runOnEdt(() -> row.setValue("146"));

        assertSame("setValue must reuse the value label instance", value, row.getValueLabel());
        assertEquals("146", value.getText());
        assertEquals("setValue refreshes the full-text tooltip too (F3)", "146", value.getToolTipText());
    }

    // ---- W8 F3: harden the row so a long value can never blow the panel width --------------------

    @Test
    public void valueCanShrinkAndKeepsTheFullTextOnHover()
    {
        String longValue = "Cannon + melee in the multi area gives the best XP and the safest spot here";
        KeyValueRow row = buildOnEdt(() -> new KeyValueRow("Why", longValue));

        JLabel value = row.getValueLabel();
        assertEquals("the value can shrink so the layout truncates instead of overflowing (F3)",
            1, value.getMinimumSize().width);
        assertEquals("the full value stays reachable on hover when truncated (F3)",
            longValue, value.getToolTipText());
    }

    @Test
    public void rowWidthIsCappedToThePanelWidth()
    {
        KeyValueRow row = buildOnEdt(() -> new KeyValueRow("Why",
            "An extremely long value that would otherwise blow the row far past the narrow side panel"));

        assertTrue("preferred width never exceeds the panel (F3)",
            row.getPreferredSize().width <= SlayerTheme.PANEL_WIDTH);
        assertEquals("maximum width is pinned to the panel (F3)",
            SlayerTheme.PANEL_WIDTH, row.getMaximumSize().width);
    }
}

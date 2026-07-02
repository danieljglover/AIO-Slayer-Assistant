package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import org.junit.Test;

/**
 * T04 - SectionHeader. Title type, primary colour, and explicitly NOT brand orange (Section 4: brand
 * is reserved for real state, not decoration).
 */
public class SectionHeaderTest
{
    @Test
    public void usesTitleFontAndPrimaryText()
    {
        SectionHeader header = buildOnEdt(() -> new SectionHeader("Task"));

        assertEquals("Task", header.getText());
        assertEquals(SlayerTheme.TYPE_TITLE, header.getFont());
        assertEquals(SlayerTheme.TEXT_PRIMARY, header.getForeground());
        assertNotEquals(SlayerTheme.ACCENT_BRAND, header.getForeground());
    }
}

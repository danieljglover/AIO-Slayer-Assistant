package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.allComponents;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import org.junit.Test;

/**
 * T04 - SectionCard. The card is a recessed well ({@code surface/card}) with a 1px
 * {@code border/divider} outline; its header is {@code text/primary}, never {@code accent/brand}.
 */
public class SectionCardTest
{
    @Test
    public void cardUsesCardSurfaceBoxLayoutAndDividerBorder()
    {
        SectionCard card = buildOnEdt(() -> new SectionCard("Loadout"));

        assertEquals(SlayerTheme.SURFACE_CARD, card.getBackground());
        assertTrue(card.getLayout() instanceof BoxLayout);
        assertTrue("Expected a compound border (line + padding)",
            card.getBorder() instanceof CompoundBorder);

        CompoundBorder border = (CompoundBorder) card.getBorder();
        assertTrue("Outer border should be a hairline divider",
            border.getOutsideBorder() instanceof LineBorder);
        LineBorder line = (LineBorder) border.getOutsideBorder();
        assertEquals(SlayerTheme.BORDER_DIVIDER, line.getLineColor());
        assertEquals(1, line.getThickness());
    }

    @Test
    public void headerIsPrimaryTextNotBrand()
    {
        SectionCard card = buildOnEdt(() -> new SectionCard("Where & How"));

        SectionHeader header = card.getHeader();
        assertEquals("Where & How", header.getText());
        assertEquals(SlayerTheme.TYPE_TITLE, header.getFont());
        assertEquals(SlayerTheme.TEXT_PRIMARY, header.getForeground());
        assertNotEquals("Section headers must not spend the brand accent",
            SlayerTheme.ACCENT_BRAND, header.getForeground());

        boolean headerIsChild = false;
        for (Component component : allComponents(card))
        {
            if (component == header)
            {
                headerIsChild = true;
                break;
            }
        }
        assertTrue("Header should be added to the card", headerIsChild);
    }
}

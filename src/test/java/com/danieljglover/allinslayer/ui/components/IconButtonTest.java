package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import org.junit.Test;

/**
 * T07 - IconButton. Uses an in-memory image so the test has no resource dependency (the real PNGs
 * arrive in T14). Asserts the borderless chrome, fixed 24px size, tooltip, and a grayscale disabled
 * icon generated in code.
 */
public class IconButtonTest
{
    private static BufferedImage redIcon()
    {
        BufferedImage image = new BufferedImage(SlayerTheme.ICON_SIZE, SlayerTheme.ICON_SIZE,
            BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                image.setRGB(x, y, Color.RED.getRGB());
            }
        }
        return image;
    }

    @Test
    public void isBorderlessFixedSizeWithTooltip()
    {
        IconButton button = buildOnEdt(() -> new IconButton(redIcon(), "Refresh"));

        assertEquals("Refresh", button.getToolTipText());
        assertEquals(SlayerTheme.ICON_BUTTON_SIZE, button.getPreferredSize().width);
        assertEquals(SlayerTheme.ICON_BUTTON_SIZE, button.getPreferredSize().height);
        assertFalse("Icon buttons must be borderless", button.isBorderPainted());
        assertFalse("Icon buttons must not fill their content area", button.isContentAreaFilled());
        assertFalse("Side-panel actions do not paint focus", button.isFocusPainted());
        assertNotNull(button.getIcon());
    }

    @Test
    public void disabledStateUsesAGrayscaleIconAndIsNotEnabled()
    {
        IconButton button = buildOnEdt(() -> new IconButton(redIcon(), "Export loadout"));

        runOnEdt(() -> button.setEnabled(false));

        assertFalse(button.isEnabled());
        assertNotNull("A grayscale disabled icon must be supplied", button.getDisabledIcon());

        BufferedImage disabled = (BufferedImage) ((ImageIcon) button.getDisabledIcon()).getImage();
        int rgb = disabled.getRGB(disabled.getWidth() / 2, disabled.getHeight() / 2);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        assertEquals("Grayscale red -> equal channels", r, g);
        assertEquals("Grayscale red -> equal channels", g, b);
    }
}

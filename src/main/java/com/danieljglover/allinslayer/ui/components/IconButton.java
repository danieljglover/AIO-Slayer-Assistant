package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

/**
 * A borderless 16px action icon (ui-recommendations.md §3.2, §8).
 *
 * <p>Takes a {@link BufferedImage} (so tests can pass an in-memory image and the action bar can pass
 * a loaded resource) plus a tooltip. The button is decoration-free and a fixed
 * {@code size/icon-button} (24px). A hover rollover (brighter) and a disabled variant (grayscale)
 * are generated in code via {@link ImageUtil} - no extra art.</p>
 *
 * <p>Note: in client 1.12.31.1 {@link SwingUtil#removeButtonDecorations} only tags a FlatLaf style
 * class; it no longer clears border/content/focus painting. We therefore strip those explicitly so
 * the button is genuinely borderless under any look and feel.</p>
 */
public class IconButton extends JButton
{
    public IconButton(BufferedImage icon, String tooltip)
    {
        SwingUtil.removeButtonDecorations(this);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder());
        setToolTipText(tooltip);

        setIcon(new ImageIcon(icon));
        setRolloverIcon(new ImageIcon(ImageUtil.luminanceScale(icon, 1.2f)));
        setDisabledIcon(new ImageIcon(ImageUtil.grayscaleImage(icon)));

        Dimension size = new Dimension(SlayerTheme.ICON_BUTTON_SIZE, SlayerTheme.ICON_BUTTON_SIZE);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }
}

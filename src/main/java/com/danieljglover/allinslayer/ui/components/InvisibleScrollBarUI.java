package com.danieljglover.allinslayer.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A scrollbar UI that takes no space and paints nothing - the track and thumb render as no-ops and
 * the increase/decrease arrow buttons are zero-size (W10 G1).
 *
 * <p>Installed on the dashboard scroll pane's vertical bar so the body scrolls by mouse wheel and
 * keyboard with no visible bar and no reserved gutter. The scroll policy stays
 * {@code VERTICAL_SCROLLBAR_AS_NEEDED} (never {@code NEVER}): a {@link javax.swing.JScrollPane}'s
 * wheel handler scrolls only while the vertical bar is {@code isVisible()}, which {@code NEVER} would
 * defeat. This UI just removes the bar visually while keeping it present - mirroring RuneLite's
 * {@code CustomScrollBarUI} and Flipping Copilot's thin-bar trick (research-r3 §2.2, §3a).</p>
 *
 * <p>A {@code BasicScrollBarUI} delegate is stateful per component, so a fresh instance must be used
 * for each scrollbar (we create one per panel).</p>
 */
public final class InvisibleScrollBarUI extends BasicScrollBarUI
{
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
    {
        // The track never paints.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
    {
        // The thumb never paints (there is no draggable thumb - wheel/keyboard scroll only).
    }

    @Override
    protected JButton createDecreaseButton(int orientation)
    {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation)
    {
        return zeroButton();
    }

    private static JButton zeroButton()
    {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }
}

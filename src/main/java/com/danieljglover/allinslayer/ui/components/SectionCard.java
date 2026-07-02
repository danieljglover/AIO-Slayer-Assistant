package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * A recessed, collapsible card grouping related rows (ui-recommendations.md §8, P2-13).
 *
 * <p>Vertical {@link BoxLayout} on a {@code surface/card} background, a 1px {@code border/divider}
 * outline and {@code space-4} inner padding. Replaces the old {@code sectionPanel} /
 * {@code FixedWidthPanel} pair; inner widths come from the layout manager (ADR-0004).</p>
 *
 * <p>The header row (a chevron + the {@link SectionHeader}) toggles the card body on click - the
 * Quest Helper {@code QuestStepPanel} collapse pattern. The expanded/collapsed flag lives on the card
 * instance, so - because the dashboard never recreates its sections, only rebuilds their bodies - it
 * persists across reactive re-renders for free, like scroll / combo / mode selection.</p>
 */
public class SectionCard extends JPanel
{
    private final SectionHeader header;
    private final JLabel chevron;
    private final JPanel content;
    private boolean expanded = true;

    public SectionCard(String title)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(SlayerTheme.SURFACE_CARD);
        setBorder(new CompoundBorder(
            new LineBorder(SlayerTheme.BORDER_DIVIDER, 1),
            new EmptyBorder(SlayerTheme.SPACE_4, SlayerTheme.SPACE_4, SlayerTheme.SPACE_4, SlayerTheme.SPACE_4)));
        setAlignmentX(LEFT_ALIGNMENT);

        header = new SectionHeader(title);

        chevron = new JLabel(new ChevronIcon(true));
        chevron.setName("section-chevron");

        JPanel headerBar = new JPanel();
        headerBar.setName("section-header-bar");
        headerBar.setLayout(new BoxLayout(headerBar, BoxLayout.X_AXIS));
        headerBar.setBackground(SlayerTheme.SURFACE_CARD);
        headerBar.setAlignmentX(LEFT_ALIGNMENT);
        headerBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerBar.add(chevron);
        headerBar.add(Box.createHorizontalStrut(SlayerTheme.SPACE_2));
        headerBar.add(header);
        headerBar.add(Box.createHorizontalGlue());

        // The whole header row is the hit target (chevron, title, and the bar between them).
        MouseAdapter toggle = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                setExpanded(!expanded);
            }
        };
        headerBar.addMouseListener(toggle);
        chevron.addMouseListener(toggle);
        header.addMouseListener(toggle);
        add(headerBar);

        content = new JPanel();
        content.setName("section-content");
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(SlayerTheme.SURFACE_CARD);
        content.setAlignmentX(LEFT_ALIGNMENT);
        add(content);
    }

    /** Add a body component below the header, left-aligned within the (collapsible) card body. */
    public void addContent(Component component)
    {
        if (component instanceof JComponent)
        {
            ((JComponent) component).setAlignmentX(LEFT_ALIGNMENT);
        }
        content.add(component);
    }

    /** Vertical breathing room before the next row, using the spacing scale. */
    public void addGap(int height)
    {
        content.add(Box.createVerticalStrut(height));
    }

    /** Whether the card body is currently shown. */
    public boolean isExpanded()
    {
        return expanded;
    }

    /** Show or collapse the card body (the chevron follows); the body is never recreated. */
    public void setExpanded(boolean expanded)
    {
        if (this.expanded == expanded)
        {
            return;
        }
        this.expanded = expanded;
        content.setVisible(expanded);
        chevron.setIcon(new ChevronIcon(expanded));
        revalidate();
        repaint();
    }

    public SectionHeader getHeader()
    {
        return header;
    }

    /**
     * A tiny triangular chevron painted in {@code text/secondary}: pointing down when expanded,
     * right when collapsed. Drawn rather than shipped as an asset so it needs no PNG and follows the
     * theme colour.
     */
    private static final class ChevronIcon implements Icon
    {
        private static final int SIZE = 8;
        private final boolean expanded;

        ChevronIcon(boolean expanded)
        {
            this.expanded = expanded;
        }

        @Override
        public int getIconWidth()
        {
            return SIZE;
        }

        @Override
        public int getIconHeight()
        {
            return SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SlayerTheme.TEXT_SECONDARY);
            if (expanded)
            {
                g2.fillPolygon(new int[] {x, x + SIZE, x + SIZE / 2},
                    new int[] {y + 1, y + 1, y + SIZE - 1}, 3);
            }
            else
            {
                g2.fillPolygon(new int[] {x + 1, x + 1, x + SIZE - 1},
                    new int[] {y, y + SIZE, y + SIZE / 2}, 3);
            }
            g2.dispose();
        }
    }
}

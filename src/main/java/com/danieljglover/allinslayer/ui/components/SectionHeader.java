package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * A {@link SectionCard} title (ui-recommendations.md §8, §4).
 *
 * <p>Rendered in {@code type/title} and {@code text/primary} - deliberately <b>not</b>
 * {@code accent/brand}: brand orange is reserved for real state (active mode, recommended
 * location), so spending it on every heading would stop it signalling anything.</p>
 */
public class SectionHeader extends JLabel
{
    public SectionHeader(String title)
    {
        super(title);
        setName("section-header");
        setFont(SlayerTheme.TYPE_TITLE);
        setForeground(SlayerTheme.TEXT_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(0, 0, SlayerTheme.SPACE_3, 0));
    }
}

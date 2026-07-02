package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * A small pill used for location attributes (multi / cannon / burst / konar) and item state
 * (ui-recommendations.md §8). Replaces the old {@code chips()} / {@code addChip()} helpers.
 *
 * <p>Rendered in {@code type/caption} at {@code size/tag} (16px) height with a 1px
 * {@code border/divider} outline. The {@link Variant} drives the foreground colour: an
 * {@code UPGRADE} you do not own yet is {@code text/secondary} (informational, not alarming) while a
 * {@code MISSING} required item is {@code state/blocked} red.</p>
 */
public class Tag extends JLabel
{
    public enum Variant
    {
        DEFAULT,
        UPGRADE,
        MISSING
    }

    public Tag(String text)
    {
        this(text, Variant.DEFAULT);
    }

    public Tag(String text, Variant variant)
    {
        super(text);
        setName("tag");
        setFont(SlayerTheme.TYPE_CAPTION);
        setForeground(foregroundFor(variant));
        setBorder(new CompoundBorder(
            new LineBorder(SlayerTheme.BORDER_DIVIDER, 1),
            new EmptyBorder(SlayerTheme.SPACE_1, SlayerTheme.SPACE_2, SlayerTheme.SPACE_1, SlayerTheme.SPACE_2)));
    }

    private static Color foregroundFor(Variant variant)
    {
        switch (variant)
        {
            case MISSING:
                return SlayerTheme.STATE_BLOCKED;
            case UPGRADE:
            case DEFAULT:
            default:
                return SlayerTheme.TEXT_SECONDARY;
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(preferred.width, SlayerTheme.TAG_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize()
    {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(preferred.width, SlayerTheme.TAG_HEIGHT);
    }
}

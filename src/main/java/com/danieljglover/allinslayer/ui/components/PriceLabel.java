package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Color;
import javax.swing.JLabel;
import net.runelite.client.util.QuantityFormatter;

/**
 * A colour-tiered Grand Exchange value (ui-recommendations.md §2.4, §8).
 *
 * <p>Compact text via {@link QuantityFormatter#quantityToRSDecimalStack(int)} with the full number
 * on the tooltip via {@link QuantityFormatter#formatNumber(long)}. The colour tier signals value at
 * a glance: unknown is muted (never blocks a sprite on price), and the value climbs through
 * {@code text/secondary} -> {@code price/mid} -> {@code price/high}.</p>
 */
public class PriceLabel extends JLabel
{
    private static final int TIER_LOW = 100_000;
    private static final int TIER_MID = 10_000_000;

    public PriceLabel(int price)
    {
        setName("price");
        setFont(SlayerTheme.TYPE_CAPTION);
        setPrice(price);
    }

    /**
     * {@code long} overload for totals that can exceed {@link Integer#MAX_VALUE} (~2.147b gp), e.g. a
     * full gear cost. Values within {@code int} range render identically to the {@code int} path; only
     * values above it use the long stack formatter (and are always the high tier).
     */
    public PriceLabel(long price)
    {
        setName("price");
        setFont(SlayerTheme.TYPE_CAPTION);
        setPrice(price);
    }

    /** Update the displayed value in place (same label instance). */
    public void setPrice(int price)
    {
        if (price <= 0)
        {
            setText("-");
            setToolTipText(null);
            setForeground(SlayerTheme.TEXT_MUTED);
            return;
        }
        setText(QuantityFormatter.quantityToRSDecimalStack(price));
        setToolTipText(QuantityFormatter.formatNumber(price));
        setForeground(colourFor(price));
    }

    /**
     * Update the displayed value in place from a {@code long}. Within {@code int} range this delegates
     * to {@link #setPrice(int)} for identical formatting/colour; above {@link Integer#MAX_VALUE} the
     * compact decimal stack cannot represent the value, so it uses {@link
     * QuantityFormatter#quantityToStackSize(long)} and the high tier (anything &gt; 2.147b is high).
     */
    public void setPrice(long price)
    {
        if (price <= Integer.MAX_VALUE)
        {
            setPrice((int) price);
            return;
        }
        setText(QuantityFormatter.quantityToStackSize(price));
        setToolTipText(QuantityFormatter.formatNumber(price));
        setForeground(SlayerTheme.PRICE_HIGH);
    }

    private static Color colourFor(int price)
    {
        if (price <= 0)
        {
            return SlayerTheme.TEXT_MUTED;
        }
        if (price < TIER_LOW)
        {
            return SlayerTheme.TEXT_SECONDARY;
        }
        if (price <= TIER_MID)
        {
            return SlayerTheme.PRICE_MID;
        }
        return SlayerTheme.PRICE_HIGH;
    }
}

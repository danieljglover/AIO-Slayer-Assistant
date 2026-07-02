package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import net.runelite.client.util.QuantityFormatter;
import org.junit.Test;

/**
 * T06 - PriceLabel. Colour tier by GE value, compact text via
 * {@link QuantityFormatter#quantityToRSDecimalStack(int)}, full number on the tooltip.
 */
public class PriceLabelTest
{
    @Test
    public void unknownValueIsMutedWithDash()
    {
        PriceLabel label = buildOnEdt(() -> new PriceLabel(0));

        assertEquals(SlayerTheme.TEXT_MUTED, label.getForeground());
        assertEquals("-", label.getText());
    }

    @Test
    public void subHundredKisSecondaryWithCompactTextAndFullTooltip()
    {
        PriceLabel label = buildOnEdt(() -> new PriceLabel(50_000));

        assertEquals(SlayerTheme.TEXT_SECONDARY, label.getForeground());
        assertEquals(QuantityFormatter.quantityToRSDecimalStack(50_000), label.getText());
        assertEquals(QuantityFormatter.formatNumber(50_000), label.getToolTipText());
    }

    @Test
    public void tierBoundariesPickTheRightColour()
    {
        assertEquals(SlayerTheme.TEXT_SECONDARY, buildOnEdt(() -> new PriceLabel(99_999)).getForeground());
        assertEquals(SlayerTheme.PRICE_MID, buildOnEdt(() -> new PriceLabel(100_000)).getForeground());
        assertEquals(SlayerTheme.PRICE_MID, buildOnEdt(() -> new PriceLabel(1_400_000)).getForeground());
        assertEquals(SlayerTheme.PRICE_MID, buildOnEdt(() -> new PriceLabel(10_000_000)).getForeground());
        assertEquals(SlayerTheme.PRICE_HIGH, buildOnEdt(() -> new PriceLabel(10_000_001)).getForeground());
        assertEquals(SlayerTheme.PRICE_HIGH, buildOnEdt(() -> new PriceLabel(55_000_000)).getForeground());
    }

    @Test
    public void usesCaptionFontAndPlainText()
    {
        PriceLabel label = buildOnEdt(() -> new PriceLabel(1_400_000));

        assertEquals(SlayerTheme.TYPE_CAPTION, label.getFont());
        assertFalse(label.getText().toLowerCase().contains("<html"));
    }

    @Test
    public void setPriceRetiersInPlace()
    {
        PriceLabel label = buildOnEdt(() -> new PriceLabel(50_000));

        runOnEdt(() -> label.setPrice(55_000_000));

        assertEquals(SlayerTheme.PRICE_HIGH, label.getForeground());
        assertEquals(QuantityFormatter.quantityToRSDecimalStack(55_000_000), label.getText());
    }

    @Test
    public void longOverloadShowsValuesAboveIntegerMaxWithoutClamping()
    {
        long huge = 3_000_000_000L; // > Integer.MAX_VALUE (~2.147b)
        PriceLabel label = buildOnEdt(() -> new PriceLabel(huge));

        assertEquals("a >2.147b value is the high tier", SlayerTheme.PRICE_HIGH, label.getForeground());
        assertEquals("the full magnitude is on the tooltip, not clamped to int",
            QuantityFormatter.formatNumber(huge), label.getToolTipText());
        assertEquals("the compact text uses the long stack formatter, not an int-clamped value",
            QuantityFormatter.quantityToStackSize(huge), label.getText());
    }

    @Test
    public void longOverloadWithinIntRangeMatchesTheIntPath()
    {
        long value = 1_400_000L;
        PriceLabel longLabel = buildOnEdt(() -> new PriceLabel(value));
        PriceLabel intLabel = buildOnEdt(() -> new PriceLabel(1_400_000));

        assertEquals(intLabel.getForeground(), longLabel.getForeground());
        assertEquals(intLabel.getText(), longLabel.getText());
        assertEquals(intLabel.getToolTipText(), longLabel.getToolTipText());
    }
}

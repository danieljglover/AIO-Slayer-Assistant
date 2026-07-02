package com.danieljglover.allinslayer.ui.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.junit.Test;

/**
 * Guards the design tokens against drift: every token must resolve to the agreed
 * {@link ColorScheme} constant / {@link FontManager} role and the spacing/size scale from
 * docs/ui-review/ui-recommendations.md §1. A failure here means a token was retargeted by hand
 * (e.g. a section heading accidentally painted brand orange) and the visual contract has moved.
 */
public class SlayerThemeTest
{
    @Test
    public void colourTokensResolveToColorSchemeConstants()
    {
        assertSame(ColorScheme.DARK_GRAY_COLOR, SlayerTheme.SURFACE_PAGE);
        assertSame(ColorScheme.DARKER_GRAY_COLOR, SlayerTheme.SURFACE_CARD);
        assertSame(ColorScheme.DARKER_GRAY_HOVER_COLOR, SlayerTheme.SURFACE_CARD_HOVER);
        assertSame(ColorScheme.DARK_GRAY_HOVER_COLOR, SlayerTheme.SURFACE_ICON_HOVER);
        assertSame(ColorScheme.BORDER_COLOR, SlayerTheme.BORDER_DIVIDER);
        assertSame(ColorScheme.TEXT_COLOR, SlayerTheme.TEXT_PRIMARY);
        assertSame(ColorScheme.LIGHT_GRAY_COLOR, SlayerTheme.TEXT_SECONDARY);
        assertSame(ColorScheme.MEDIUM_GRAY_COLOR, SlayerTheme.TEXT_MUTED);
        assertSame(ColorScheme.BRAND_ORANGE, SlayerTheme.ACCENT_BRAND);
        assertSame(ColorScheme.PROGRESS_COMPLETE_COLOR, SlayerTheme.STATE_MET);
        assertSame(ColorScheme.PROGRESS_ERROR_COLOR, SlayerTheme.STATE_BLOCKED);
        assertSame(ColorScheme.GRAND_EXCHANGE_PRICE, SlayerTheme.PRICE_HIGH);
        assertSame(ColorScheme.GRAND_EXCHANGE_ALCH, SlayerTheme.PRICE_MID);
    }

    @Test
    public void fontTokensResolveToFontManagerRoles()
    {
        // Identity by role, not an exact Font literal (headless FontManager is fragile - plan §8).
        assertEquals(FontManager.getRunescapeBoldFont(), SlayerTheme.TYPE_TITLE);
        assertEquals(FontManager.getRunescapeFont(), SlayerTheme.TYPE_BODY);
        assertEquals(FontManager.getRunescapeSmallFont(), SlayerTheme.TYPE_CAPTION);
    }

    @Test
    public void spacingScaleMatchesTokens()
    {
        assertEquals(2, SlayerTheme.SPACE_1);
        assertEquals(4, SlayerTheme.SPACE_2);
        assertEquals(6, SlayerTheme.SPACE_3);
        assertEquals(8, SlayerTheme.SPACE_4);
        assertEquals(12, SlayerTheme.SPACE_5);
    }

    @Test
    public void sizingTokensMatchSpec()
    {
        assertEquals(36, SlayerTheme.SPRITE_WIDTH);
        assertEquals(32, SlayerTheme.SPRITE_HEIGHT);
        assertEquals(38, SlayerTheme.GRID_CELL_WIDTH);
        assertEquals(34, SlayerTheme.GRID_CELL_HEIGHT);
        assertEquals(16, SlayerTheme.ICON_SIZE);
        assertEquals(24, SlayerTheme.ICON_BUTTON_SIZE);
        assertEquals(24, SlayerTheme.ACTION_BAR_HEIGHT);
        assertEquals(22, SlayerTheme.ROW_HEIGHT);
        assertEquals(64, SlayerTheme.KEY_COL_WIDTH);
        assertEquals(22, SlayerTheme.SEGMENT_HEIGHT);
        assertEquals(16, SlayerTheme.TAG_HEIGHT);
        assertEquals(225, SlayerTheme.PANEL_WIDTH);
    }
}

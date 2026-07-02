package com.danieljglover.allinslayer.ui.theme;

import java.awt.Color;
import java.awt.Font;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The single source of truth for the side panel's visual tokens.
 *
 * <p>Every colour, font, spacing and size used by the panel components resolves to a token here,
 * which in turn resolves to a {@link ColorScheme} constant or {@link FontManager} role
 * (docs/ui-review/ui-recommendations.md §1, ADR-0004). Components never call
 * {@code new Font(...)} or use ad-hoc RGB, and they never hardcode pixel widths - widths come from
 * layout managers within {@link #PANEL_WIDTH}.</p>
 *
 * <p>Brand orange ({@link #ACCENT_BRAND}) is reserved for real state only (active mode, active
 * state, recommended location) - never for section headings or decoration.</p>
 */
public final class SlayerTheme
{
    private SlayerTheme()
    {
    }

    // --- Colour tokens (ui-recommendations.md §1.1) ---

    /** Panel root, action bar background, gaps between cards. */
    public static final Color SURFACE_PAGE = ColorScheme.DARK_GRAY_COLOR;
    /** Section cards, rows, grid cells (recessed wells). */
    public static final Color SURFACE_CARD = ColorScheme.DARKER_GRAY_COLOR;
    /** Hover on an interactive content row (clearly visible). */
    public static final Color SURFACE_CARD_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
    /** Hover on an icon button sitting on the page (subtle, native). */
    public static final Color SURFACE_ICON_HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;
    /** Card borders, hairline dividers. */
    public static final Color BORDER_DIVIDER = ColorScheme.BORDER_COLOR;

    /** Item names, values, body text. */
    public static final Color TEXT_PRIMARY = ColorScheme.TEXT_COLOR;
    /** Keys, captions, slot labels, meta. */
    public static final Color TEXT_SECONDARY = ColorScheme.LIGHT_GRAY_COLOR;
    /** Disabled, tertiary, "you don't own this yet" (decorative-only, never sole carrier). */
    public static final Color TEXT_MUTED = ColorScheme.MEDIUM_GRAY_COLOR;

    /** Reserved for real state only (active mode, active state, recommended location). */
    public static final Color ACCENT_BRAND = ColorScheme.BRAND_ORANGE;
    /** Requirement met (e.g. Slayer level satisfied) - used sparingly. */
    public static final Color STATE_MET = ColorScheme.PROGRESS_COMPLETE_COLOR;
    /** Required item missing (blocks the task). */
    public static final Color STATE_BLOCKED = ColorScheme.PROGRESS_ERROR_COLOR;

    /** GE value &gt; 10M. */
    public static final Color PRICE_HIGH = ColorScheme.GRAND_EXCHANGE_PRICE;
    /** GE value 100k - 10M. */
    public static final Color PRICE_MID = ColorScheme.GRAND_EXCHANGE_ALCH;

    // --- Type tokens (ui-recommendations.md §1.2) ---

    /** Task name in header, section headers. */
    public static final Font TYPE_TITLE = FontManager.getRunescapeBoldFont();
    /** Item names, primary values. */
    public static final Font TYPE_BODY = FontManager.getRunescapeFont();
    /** Keys, slot labels, price, meta, chips/tags. */
    public static final Font TYPE_CAPTION = FontManager.getRunescapeSmallFont();

    // --- Spacing scale, px (ui-recommendations.md §1.3) ---

    /** Intra-row / sprite-to-text micro gap. */
    public static final int SPACE_1 = 2;
    /** Chip/tag internal gap, grid cell gap. */
    public static final int SPACE_2 = 4;
    /** Row vertical padding, inter-section gap. */
    public static final int SPACE_3 = 6;
    /** Card inner padding, key-&gt;value column gap. */
    public static final int SPACE_4 = 8;
    /** Between major stacked sections. */
    public static final int SPACE_5 = 12;

    // --- Sizing tokens (ui-recommendations.md §1.4) ---

    /** Native ItemManager sprite width; use as-is, do not rescale. */
    public static final int SPRITE_WIDTH = 36;
    /** Native ItemManager sprite height; use as-is, do not rescale. */
    public static final int SPRITE_HEIGHT = 32;
    /** Equipment/inventory grid cell width (sprite + 1px breathing room each side). */
    public static final int GRID_CELL_WIDTH = 38;
    /** Equipment/inventory grid cell height. */
    public static final int GRID_CELL_HEIGHT = 34;
    /** Action-bar glyph size. */
    public static final int ICON_SIZE = 16;
    /** Icon button size (16px icon + 4px inset each side). */
    public static final int ICON_BUTTON_SIZE = 24;
    /** Action bar height (one compact row). */
    public static final int ACTION_BAR_HEIGHT = 24;
    /** One KeyValueRow height. */
    public static final int ROW_HEIGHT = 22;
    /** Key column width in KeyValueRow. */
    public static final int KEY_COL_WIDTH = 64;
    /** DPS/Cost toggle segment height. */
    public static final int SEGMENT_HEIGHT = 22;
    /** Chip/pill height. */
    public static final int TAG_HEIGHT = 16;
    /** PluginPanel.PANEL_WIDTH; derive inner widths from layout managers, never hardcode. */
    public static final int PANEL_WIDTH = 225;
}

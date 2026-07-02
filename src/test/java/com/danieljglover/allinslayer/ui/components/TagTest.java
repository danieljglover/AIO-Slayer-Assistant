package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import org.junit.Test;

/**
 * T05 - Tag. A {@code size/tag} pill in {@code type/caption}; the variant drives the foreground:
 * default/upgrade are informational ({@code text/secondary}), missing is {@code state/blocked} red.
 */
public class TagTest
{
    @Test
    public void defaultVariantIsSecondaryCaptionAtTagHeight()
    {
        Tag tag = buildOnEdt(() -> new Tag("multi"));

        assertEquals("multi", tag.getText());
        assertEquals(SlayerTheme.TYPE_CAPTION, tag.getFont());
        assertEquals(SlayerTheme.TEXT_SECONDARY, tag.getForeground());
        assertEquals(SlayerTheme.TAG_HEIGHT, tag.getPreferredSize().height);
    }

    @Test
    public void upgradeVariantIsSecondaryNotRed()
    {
        Tag tag = buildOnEdt(() -> new Tag("upgrade", Tag.Variant.UPGRADE));

        assertEquals(SlayerTheme.TEXT_SECONDARY, tag.getForeground());
    }

    @Test
    public void missingVariantIsBlockedRed()
    {
        Tag tag = buildOnEdt(() -> new Tag("missing", Tag.Variant.MISSING));

        assertEquals(SlayerTheme.STATE_BLOCKED, tag.getForeground());
    }

    @Test
    public void rendersPlainTextWithoutHtml()
    {
        Tag tag = buildOnEdt(() -> new Tag("cannon"));

        assertFalse(tag.getText().toLowerCase().contains("<html"));
    }
}

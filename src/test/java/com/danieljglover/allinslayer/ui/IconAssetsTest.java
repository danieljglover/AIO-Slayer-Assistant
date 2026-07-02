package com.danieljglover.allinslayer.ui;

import static org.junit.Assert.assertNotNull;

import net.runelite.client.util.ImageUtil;
import org.junit.Test;

/**
 * The action bar (T13) loads these glyphs at runtime via {@link ImageUtil#loadImageResource}, which
 * throws if the asset is missing. These tests fail loudly if the icons are absent from the jar.
 */
public class IconAssetsTest
{
    @Test
    public void refreshIconLoadsFromResources()
    {
        assertNotNull(ImageUtil.loadImageResource(IconAssetsTest.class, "/icons/refresh.png"));
    }

    @Test
    public void exportIconLoadsFromResources()
    {
        assertNotNull(ImageUtil.loadImageResource(IconAssetsTest.class, "/icons/export.png"));
    }
}

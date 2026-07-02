package com.danieljglover.allinslayer.ui;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Production binding of {@link ItemIconRenderer} (ADR-0001): wraps the injected {@link ItemManager}.
 *
 * <p>{@code getImage(...).addTo(label)} is the EDT-safe idiom every researched RuneLite panel uses -
 * the {@link AsyncBufferedImage} self-loads on the client thread and repaints the label when ready.
 * For a dimmed (UPGRADE) sprite we cannot dim the bytes until they exist, so we hook
 * {@link AsyncBufferedImage#onLoaded(Runnable)} and alpha-offset the loaded image instead of using
 * {@code addTo} (which would paint it at full opacity).</p>
 */
public class ItemManagerIconRenderer implements ItemIconRenderer
{
    /** Alpha offset applied to an upgrade sprite the player does not own (ui-recommendations §2.1). */
    private static final int DIM_ALPHA = -80;

    private final ItemManager itemManager;

    public ItemManagerIconRenderer(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    @Override
    public void render(JLabel label, int itemId, int quantity, boolean stackable, boolean dim)
    {
        AsyncBufferedImage image = itemManager.getImage(itemId, quantity, stackable);
        if (dim)
        {
            image.onLoaded(() -> label.setIcon(new ImageIcon(ImageUtil.alphaOffset(image, DIM_ALPHA))));
        }
        else
        {
            image.addTo(label);
        }
    }
}

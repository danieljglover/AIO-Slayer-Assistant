package com.danieljglover.allinslayer.ui;

import javax.swing.JLabel;

/**
 * The seam between the panel's leaf components and {@code ItemManager} (ADR-0001).
 *
 * <p>Components draw item sprites through this one-method interface rather than calling
 * {@code ItemManager} directly, because the returned {@code AsyncBufferedImage} needs a live
 * {@code ClientThread} and cannot be constructed in a headless unit test. Production binds
 * {@link ItemManagerIconRenderer}; tests inject a no-op / recording fake and assert "asked to render
 * id X (qty, stackable, dim)" rather than asserting pixels.</p>
 */
public interface ItemIconRenderer
{
    /**
     * Render the sprite for {@code itemId} onto {@code label}.
     *
     * @param label     the label to receive the sprite icon
     * @param itemId    the item id to draw
     * @param quantity  the stack quantity (use 1 for non-stackable gear)
     * @param stackable whether the stack number should be drawn (runes, cannonballs, doses)
     * @param dim       whether to render a dimmed sprite (an UPGRADE the player does not own yet)
     */
    void render(JLabel label, int itemId, int quantity, boolean stackable, boolean dim);
}

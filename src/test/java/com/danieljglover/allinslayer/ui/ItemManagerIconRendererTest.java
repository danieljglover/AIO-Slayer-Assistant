package com.danieljglover.allinslayer.ui;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.swing.JLabel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * The production binding of the {@link ItemIconRenderer} seam (ADR-0001). We never call the real
 * {@code getImage(...)} - {@link AsyncBufferedImage} needs a live ClientThread - so we mock the
 * {@link ItemManager} and assert the renderer asks it for the right (id, qty, stackable) image and
 * attaches it to the label.
 */
public class ItemManagerIconRendererTest
{
    @Test
    public void rendersGearSpriteByAddingAsyncImageToLabel()
    {
        ItemManager itemManager = Mockito.mock(ItemManager.class);
        AsyncBufferedImage image = Mockito.mock(AsyncBufferedImage.class);
        JLabel label = new JLabel();
        when(itemManager.getImage(4151, 1, false)).thenReturn(image);

        new ItemManagerIconRenderer(itemManager).render(label, 4151, 1, false, false);

        verify(itemManager).getImage(4151, 1, false);
        verify(image).addTo(label);
    }

    @Test
    public void stackableQuantityIsPassedThroughToItemManager()
    {
        ItemManager itemManager = Mockito.mock(ItemManager.class);
        AsyncBufferedImage image = Mockito.mock(AsyncBufferedImage.class);
        JLabel label = new JLabel();
        when(itemManager.getImage(560, 5000, true)).thenReturn(image);

        new ItemManagerIconRenderer(itemManager).render(label, 560, 5000, true, false);

        verify(itemManager).getImage(560, 5000, true);
        verify(image).addTo(label);
    }

    @Test
    public void dimmedRowDefersIconToOnLoadedAndDoesNotAttachDirectly()
    {
        ItemManager itemManager = Mockito.mock(ItemManager.class);
        AsyncBufferedImage image = Mockito.mock(AsyncBufferedImage.class);
        JLabel label = new JLabel();
        when(itemManager.getImage(4587, 1, false)).thenReturn(image);

        new ItemManagerIconRenderer(itemManager).render(label, 4587, 1, false, true);

        verify(itemManager).getImage(4587, 1, false);
        // A dimmed (upgrade) sprite must be alpha-offset once the async image loads, so we hook
        // onLoaded rather than addTo (which would paint the sprite at full opacity).
        verify(image).onLoaded(Mockito.any(Runnable.class));
        verify(image, never()).addTo(label);
    }
}

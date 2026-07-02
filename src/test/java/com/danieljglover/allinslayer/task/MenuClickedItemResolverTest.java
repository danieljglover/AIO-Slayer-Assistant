package com.danieljglover.allinslayer.task;

import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MenuClickedItemResolverTest
{
    @Test
    public void resolvesWidgetChildItemWhenDirectMenuItemIdIsUnset()
    {
        Widget root = mock(Widget.class);
        Widget child = mock(Widget.class);
        MenuEntry entry = mock(MenuEntry.class);

        when(entry.getItemId()).thenReturn(0);
        when(entry.getWidget()).thenReturn(root);
        when(entry.getParam0()).thenReturn(2);
        when(root.getChild(2)).thenReturn(child);
        when(child.getItemId()).thenReturn(11864);

        assertEquals(11864, MenuClickedItemResolver.resolve(new MenuOptionClicked(entry)));
    }

    @Test
    public void resolvesDirectItemIdWhenPresent()
    {
        MenuEntry entry = mock(MenuEntry.class);
        when(entry.getItemId()).thenReturn(4155);

        assertEquals(4155, MenuClickedItemResolver.resolve(new MenuOptionClicked(entry)));
    }
}

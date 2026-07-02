package com.danieljglover.allinslayer.task;

import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;

public final class MenuClickedItemResolver
{
    private MenuClickedItemResolver() {}

    public static int resolve(MenuOptionClicked event)
    {
        Widget widget = event.getWidget();
        if (widget != null)
        {
            int widgetItemId = resolveWidgetItemId(widget, event.getParam0());
            if (widgetItemId > 0)
            {
                return widgetItemId;
            }
        }

        int itemId = event.getItemId();
        return itemId > 0 ? itemId : -1;
    }

    private static int resolveWidgetItemId(Widget widget, int childIndex)
    {
        Widget itemWidget = widget;
        if (childIndex != -1)
        {
            itemWidget = widget.getChild(childIndex);
            if (itemWidget == null)
            {
                return -1;
            }
        }

        int itemId = itemWidget.getItemId();
        if (itemId > 0)
        {
            return itemId;
        }

        Widget[] dynamicChildren = itemWidget.getDynamicChildren();
        if (dynamicChildren == null)
        {
            return -1;
        }

        for (Widget dynamicChild : dynamicChildren)
        {
            if (dynamicChild.getItemId() > 0)
            {
                return dynamicChild.getItemId();
            }
        }
        return -1;
    }
}

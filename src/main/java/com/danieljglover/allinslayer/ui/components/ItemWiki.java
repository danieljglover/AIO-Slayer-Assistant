package com.danieljglover.allinslayer.ui.components;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import net.runelite.client.util.LinkBrowser;

/**
 * The shared right-click "Wiki" lookup for item components (ui-recommendations.md §2.1, P2-12).
 *
 * <p>Both {@link LoadoutItemRow} and the grid cells offer the same per-item action: open the item's
 * OSRS wiki page via {@code Special:Lookup}. The lookup URL is carried on the menu item's
 * action command so a test can assert the action targets the right item id without launching a
 * browser; the listener simply hands that command to {@link LinkBrowser#browse(String)}.</p>
 */
final class ItemWiki
{
    private ItemWiki()
    {
    }

    /** The OSRS wiki {@code Special:Lookup} URL for {@code itemId}. */
    static String url(int itemId)
    {
        return "https://oldschool.runescape.wiki/w/Special:Lookup?type=item&id="
            + itemId + "&utm_source=runelite";
    }

    /** A fresh single-item "Wiki" popup targeting {@code itemId}. */
    static JPopupMenu popup(int itemId)
    {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem wiki = new JMenuItem("Wiki");
        wiki.setName("wiki-menu-item");
        wiki.setActionCommand(url(itemId));
        wiki.addActionListener(e -> LinkBrowser.browse(e.getActionCommand()));
        menu.add(wiki);
        return menu;
    }
}

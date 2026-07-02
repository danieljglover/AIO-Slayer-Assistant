package com.danieljglover.allinslayer.loadout;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;

@Singleton
public class DefaultPriceService implements PriceService
{
    private final ItemManager itemManager;

    @Inject
    public DefaultPriceService(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    @Override
    public int price(int itemId)
    {
        return itemManager.getItemPrice(itemId);
    }
}

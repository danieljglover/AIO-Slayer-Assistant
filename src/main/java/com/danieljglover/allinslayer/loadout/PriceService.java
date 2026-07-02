package com.danieljglover.allinslayer.loadout;

import com.google.inject.ImplementedBy;

@ImplementedBy(DefaultPriceService.class)
public interface PriceService
{
    int price(int itemId);
}

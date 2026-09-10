package com.danieljglover.allinslayer.model.advisor;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Cached per-unit values for an exact item form. A missing value is -1, never zero. */
@Getter
@EqualsAndHashCode
public final class ItemValue
{
    private final int id;
    private final String name;
    private final long marketPrice;
    private final long protectionValue;
    private final boolean tradeable;
    private final boolean stackable;
    private final boolean chargesUnknown;

    public ItemValue(int id, String name, long marketPrice, long protectionValue,
        boolean tradeable, boolean stackable, boolean chargesUnknown)
    {
        this.id = id;
        this.name = name == null ? "Item " + id : name;
        this.marketPrice = Math.max(-1, marketPrice);
        this.protectionValue = Math.max(-1, protectionValue);
        this.tradeable = tradeable;
        this.stackable = stackable;
        this.chargesUnknown = chargesUnknown;
    }
}

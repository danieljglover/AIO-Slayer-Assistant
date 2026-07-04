package com.danieljglover.allinslayer.data.source;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One authored travel item in a {@code locations/*.json} {@code travel.items[]} list: a stable
 * {@code itemKey} into the {@code items/} catalogue plus how many to bring. Unlike the free-text
 * strategy item names, {@code itemKey} is a structured reference and fails the build if it does not
 * resolve to a catalogue item.
 */
@Data
@NoArgsConstructor
public class SourceLocationTravelItem
{
    private String itemKey;
    private int quantity;
}

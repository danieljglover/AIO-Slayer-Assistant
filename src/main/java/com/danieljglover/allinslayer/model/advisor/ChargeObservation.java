package com.danieljglover.allinslayer.model.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.EqualsAndHashCode;

/** A current-session Check or a labelled external tracker estimate. Negative charges means unknown. */
@Getter
@EqualsAndHashCode
public final class ChargeObservation
{
    private final int itemId;
    private final int charges;
    private final int observedTick;
    private final boolean observed;
    private final String source;
    private final Map<Integer, Integer> resources;

    public ChargeObservation(int itemId, int charges, int observedTick)
    {
        this(itemId, charges, observedTick, true, "In-game Check", Collections.emptyMap());
    }

    public ChargeObservation(int itemId, int charges, int observedTick, boolean observed,
        String source, Map<Integer, Integer> resources)
    {
        this.itemId = itemId;
        this.charges = Math.max(-1, charges);
        this.observedTick = observedTick;
        this.observed = observed;
        this.source = source;
        this.resources = Collections.unmodifiableMap(new LinkedHashMap<>(resources));
    }
}

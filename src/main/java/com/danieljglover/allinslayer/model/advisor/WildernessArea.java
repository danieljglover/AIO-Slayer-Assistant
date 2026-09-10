package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Encounter and travel exposure; unknown depths must use the above-20 scenario. */
@Data
@NoArgsConstructor
public class WildernessArea
{
    private int minLevel = -1;
    private int maxLevel = -1;
    private boolean wildernessTravel;
    private boolean pvmUsesPvpRules;
    private long entryFee;
    private List<SlayerCatalogue.Evidence> evidence = new ArrayList<>();
}

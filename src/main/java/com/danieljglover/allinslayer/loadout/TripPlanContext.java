package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.MonsterStrategy;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.StrategyMethod;
import com.danieljglover.allinslayer.model.TaskData;
import lombok.Value;

/**
 * The immutable input bundle the {@link TripPlanner} layers read. Assembled by {@link LoadoutAdvisor}
 * once the profile, effective location, selected method, consumables, and sustain estimate are known.
 * A null {@code method}/{@code strategy}/{@code sustain} simply means the corresponding layer
 * contributes nothing beyond the owned base supplies - the sparse-data fallback.
 */
@Value
public class TripPlanContext
{
    OwnedItems owned;
    MonsterProfile profile;
    TaskData task;
    SlayerLocation effectiveLocation;
    MonsterStrategy strategy;          // for strategy-wide (role=general) key items; nullable
    StrategyMethod method;             // the selected/auto-picked method; nullable
    Consumables consumables;           // best owned food/combo/potion/magic; nullable
    SustainModel.Result sustain;       // prayer-vs-food sizing; nullable -> convention
}

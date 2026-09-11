package com.danieljglover.allinslayer.data.source;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ModularSlayerDataSet
{
    private List<SourceMaster> masters = new ArrayList<>();
    private List<SourceTask> tasks = new ArrayList<>();
    private List<SourceMonsterFamily> monsters = new ArrayList<>();
    private List<SourceLocation> locations = new ArrayList<>();
    private List<SourceWeapon> weapons = new ArrayList<>();
    // The supply-item catalogue (items/*.json) that resolves free-text strategy item names to ids for
    // the dynamic trip planner; the supply counterpart to weapons.
    private List<SourceItem> items = new ArrayList<>();
    private List<SourceStrategy> strategies = new ArrayList<>();
    // WA-8 (ADR-0018 #6): the global rewards/*.json domain, compiled into slayer-meta.json.
    private List<SourceReward> rewards = new ArrayList<>();
}

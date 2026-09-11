package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceStrategy
{
    private String strategyId;
    private List<String> variantIds;
    private CombatStyle primaryStyle;
    private List<String> primaryWeapons;
    private List<SourceStrategyWeapon> secondaryWeapons;
    private String note;
    private String sourceUrl;
    private String body;
    private SourceStrategyPlugin plugin;
    private List<String> requirements;
    private List<String> mechanics;
    private List<SourceStrategyMethod> methods;
    private List<SourceStrategyStyleOption> styleOptions;
}

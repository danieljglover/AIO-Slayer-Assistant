package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceStrategyStyleOption
{
    private String styleId;
    private String label;
    private CombatStyle combatStyle;
    private String role;
    private String summary;
    private SourceStrategyEquipment equipment;
}

package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceStrategyPlugin
{
    private CombatStyle primaryStyle;
    private List<String> primaryWeapons;
    private List<SourceStrategyWeapon> secondaryWeapons;
    private String note;
}

package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceStrategyMethod
{
    private String methodId;
    private String label;
    private CombatStyle combatStyle;
    private String role;
    private String summary;
    private List<String> recommendedFor;
    private List<String> requiredOrKeyItems;
    private List<String> prayers;
    private List<String> steps;
    private List<String> fallbacks;
    private List<String> risks;
    private SourceStrategyEquipment equipment;
    private List<String> inventory;
    private List<String> notes;
}

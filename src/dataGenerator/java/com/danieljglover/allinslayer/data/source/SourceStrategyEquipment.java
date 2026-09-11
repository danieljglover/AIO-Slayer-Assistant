package com.danieljglover.allinslayer.data.source;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceStrategyEquipment
{
    private Map<String, List<String>> slots;
}

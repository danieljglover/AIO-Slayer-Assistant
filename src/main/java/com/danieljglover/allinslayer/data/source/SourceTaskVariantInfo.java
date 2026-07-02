package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceTaskVariantInfo
{
    private String variantId;
    private String name;
    private Integer combatLevel;
    private Number slayerXp;
    private List<String> locations;
    private List<String> notes;
}

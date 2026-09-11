package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceMonsterFamily
{
    private String monsterId;
    private String name;
    private List<SourceMonsterVariant> variants;
}

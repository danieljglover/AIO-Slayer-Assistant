package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceTaskLocationComparison
{
    private String locationId;
    private String name;
    private Integer amount;
    private Boolean multicombat;
    private Boolean cannonable;
    private Boolean safespottable;
    private List<String> notes;
}

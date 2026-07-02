package com.danieljglover.allinslayer.data.source;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceLocation
{
    private String locationId;
    private String name;
    private boolean multi;
    private boolean cannon;
    private boolean burst;
    private boolean konarLockable;
    private boolean safeSpot;
    private boolean wilderness;
    private String accessNote;
}

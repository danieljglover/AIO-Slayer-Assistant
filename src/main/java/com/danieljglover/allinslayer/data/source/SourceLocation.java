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
    // Phase 3 travel data: teleport/access items to reach this location. Absent = no travel (backfill).
    private SourceLocationTravel travel;
}

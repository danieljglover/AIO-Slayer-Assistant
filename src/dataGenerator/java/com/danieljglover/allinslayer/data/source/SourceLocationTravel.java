package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The optional {@code travel} block of a {@code locations/*.json}: the teleport/access items to bring
 * to reach this location (Phase 3), plus a free-text {@code note}. Absent = no travel contribution
 * (the incremental-backfill contract).
 */
@Data
@NoArgsConstructor
public class SourceLocationTravel
{
    private List<SourceLocationTravelItem> items;
    private String note;
}

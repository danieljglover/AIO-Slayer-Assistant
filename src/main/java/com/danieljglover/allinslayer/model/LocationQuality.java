package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WD-5a (ADR-0020 #2): the per-task, per-location quality overlay compiled from the source
 * {@code locationComparison[]} rows (previously validate-only, ADR-0017 #2). It carries the wiki's
 * task-scoped truth for a location - richer than the location file's intrinsic {@code multi}/
 * {@code cannon}/{@code safeSpot} flags because those are location-intrinsic, not task-scoped.
 *
 * <ul>
 *   <li>{@code amount} - the wiki kills/density figure; the new ranking key (WD-5b), used only as an
 *       ordinal tie-break, never displayed as a precise rate (no fabrication).</li>
 *   <li>{@code multicombat}/{@code cannonable}/{@code safespottable} - task-scoped flags feeding the
 *       cannon-DPS term (WD-6) and Ancients multi signal (WD-9).</li>
 *   <li>{@code notes} - free-text authored notes.</li>
 * </ul>
 *
 * <p>All fields optional (boxed / list); a location with no comparison row carries a null overlay ->
 * today's authored-order behaviour (FR-6 byte-identical-by-default).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationQuality
{
    private Integer amount;
    private Boolean multicombat;
    private Boolean cannonable;
    private Boolean safespottable;
    private List<String> notes;
}

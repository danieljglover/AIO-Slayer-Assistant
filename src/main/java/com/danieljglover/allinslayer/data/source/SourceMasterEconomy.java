package com.danieljglover.allinslayer.data.source;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Slayer master's reward-point economy (WA-3, ADR-0018 #3 / FR-R2 section 1a-1b): what
 * completing a task with this master is worth. {@code streakMultipliers} is keyed by the
 * task-count milestone as a string ("10", "50", "100", "250", "1000") -> the multiple of
 * {@code basePoints} awarded on that completion. {@code zeroPoints} (Turael/Spria: assignments
 * award nothing) and {@code streakResets} (taking a task here resets the completion streak) are
 * primitive booleans, so an absent key means false - the master-coverage test (WA-4) asserts the
 * explicit true rows.
 */
@Data
@NoArgsConstructor
public class SourceMasterEconomy
{
    private Integer basePoints;
    private Map<String, Integer> streakMultipliers;
    private Integer blockCost;
    private boolean zeroPoints;
    private boolean streakResets;
}

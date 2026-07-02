package com.danieljglover.allinslayer.model;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A master's reward-point economy at runtime (WA-2, mirrors {@code SourceMasterEconomy}).
 * {@code streakMultipliers} is keyed by the task-count milestone as a string ("10", "50", "100",
 * "250", "1000") -> the multiple of {@code basePoints} awarded on that completion.
 * {@code zeroPoints} (Turael/Spria award nothing) and {@code streakResets} (taking a task here
 * resets the streak) are primitive booleans - absent means false, authored explicitly true where
 * the wiki says so (WA-4).
 */
@Data
@NoArgsConstructor
public class MasterEconomy
{
    private Integer basePoints;
    private Map<String, Integer> streakMultipliers;
    private Integer blockCost;
    private boolean zeroPoints;
    private boolean streakResets;
}

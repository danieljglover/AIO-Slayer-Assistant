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
    // Elite-diary boosted base points (Konar 18 -> 20 with Kourend & Kebos elite, Nieve 12 -> 15
    // with Western Provinces elite); null = no diary boost exists. The streak multipliers apply
    // to whichever base is in effect, so only the base needs modelling.
    private Integer diaryBoostedPoints;
    private String diaryBoostNote;
    // Krystilia only: her assignments run a separate task-completion counter from the standard
    // masters (its own streak, and its own first-four-tasks grace before points accrue).
    private boolean separateStreak;
}

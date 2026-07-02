package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WD-1 (ADR-0020 #1): the NEW additive monster-offence domain - what the monster does TO the player,
 * distinct from {@link Weakness}/{@link MonsterDefence} (what the player does to it). Lives on the
 * monster variant (a superior hits harder than its base), with an optional task-level fallback, and
 * drives a note-only prayer/survival advisory (WD-3).
 *
 * <p>Every field is optional - where the wiki does not state a value, omit it (honest UNKNOWN,
 * ADR-0019); the advisory simply does not assert that dimension. Absent everywhere -> the whole
 * {@code offence} is null -> no advisory (FR-6 byte-identical-by-default).
 *
 * <ul>
 *   <li>{@code hitpoints} - monster HP (also the single-target cannon-DPS input, WD-6).</li>
 *   <li>{@code maxHit} - the monster's max hit, for the survival clause.</li>
 *   <li>{@code attackStyles} - the styles to pray against (WD-3 prayer clause).</li>
 *   <li>{@code attackSpeedTicks} - attack interval in game ticks (display/future use).</li>
 *   <li>{@code magicLevel} - the monster Magic level (PD-D3), the Twisted bow scaling input (WD-11).</li>
 *   <li>{@code poisonous}/{@code venomous} - drive the antipoison/antivenom nudge (owned-driven).</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonsterOffence
{
    private Integer hitpoints;
    private Integer maxHit;
    private List<AttackStyle> attackStyles;
    private Integer attackSpeedTicks;
    private Integer magicLevel;
    private boolean poisonous;
    private boolean venomous;
}

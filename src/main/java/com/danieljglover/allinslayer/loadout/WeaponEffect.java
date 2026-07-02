package com.danieljglover.allinslayer.loadout;

import lombok.Value;

/**
 * A passive special-weapon mechanic that reshapes the sustained-DPS formula (ADR-0008 section 3.4a).
 * Unlike a {@link ConditionalBonus} (a per-style scalar gated on a task predicate), these change the
 * shape of the hit-chance / average-damage maths itself, so they cannot live in the conditional-bonus
 * registry. Two orthogonal fields:
 *
 * <ul>
 *   <li>{@link #accuracyRolls} - how many independent accuracy rolls the weapon takes (Osmumten's fang
 *       rolls twice, so its hit chance is {@code 1 - (1 - p)^2}); default {@code 1}.</li>
 *   <li>{@link #damageMultiplier} - a multiplier on average per-swing damage (Scythe of vitur hits a
 *       large target three times at 100/50/25% of max, so its mean per swing is {@code 1.75x});
 *       default {@code 1.0}.</li>
 * </ul>
 *
 * {@link #NONE} = {@code (1, 1.0)} is a normal single-roll, single-hit weapon - today's maths exactly.
 */
@Value
public class WeaponEffect
{
    /** A normal weapon: one accuracy roll, no damage multiplier. */
    public static final WeaponEffect NONE = new WeaponEffect(1, 1.0);

    int accuracyRolls;
    double damageMultiplier;
}

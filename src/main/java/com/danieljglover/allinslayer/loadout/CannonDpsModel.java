package com.danieljglover.allinslayer.loadout;

/**
 * The honest single-target Dwarf multicannon DPS term (WD-6, ADR-0020 #3).
 *
 * <p>The cannon's damage is deterministic and independent of the player's gear or Ranged level: a
 * static max hit of {@value #MAX_HIT} with steel cannonballs (wiki-verified), a uniform 0-{@value
 * #MAX_HIT} distribution (mean {@code 15}), one shot every {@value #SHOT_TICKS} game ticks
 * ({@value #SHOT_INTERVAL_SECONDS}s). So the single-target DPS is a computable constant - deliberately
 * NOT folded into weapon ranking (that would break the ADR-0008 order-independence invariant); it is a
 * separate, additive display line beside the worn-gear "Est. DPS".
 *
 * <p>Multi-target stacking is intentionally unmodelled (per-spot monster density is unsourced -
 * fabrication); the multi-combat case is a note-only ceiling statement, surfaced by the caller.
 */
public final class CannonDpsModel
{
    /** Static max hit with steel cannonballs, regardless of Ranged level or bonuses (wiki). */
    private static final int MAX_HIT = 30;

    /** Mean of the uniform 0-{@link #MAX_HIT} damage distribution. */
    private static final double MEAN_DAMAGE = MAX_HIT / 2.0;

    /** One shot every 4 game ticks. */
    private static final int SHOT_TICKS = 4;

    /** Seconds per shot (4 ticks * 0.6s). */
    private static final double SHOT_INTERVAL_SECONDS = SHOT_TICKS * 0.6;

    private CannonDpsModel()
    {
    }

    /**
     * @return the single-target cannon DPS: mean damage over the shot interval ({@code 15 / 2.4 = 6.25}).
     *     A gear-independent constant (ADR-0020 #3); the display rounds it for the "~X DPS" line.
     */
    public static double singleTargetDps()
    {
        return MEAN_DAMAGE / SHOT_INTERVAL_SECONDS;
    }
}

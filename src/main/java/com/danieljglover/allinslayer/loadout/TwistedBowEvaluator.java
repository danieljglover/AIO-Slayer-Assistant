package com.danieljglover.allinslayer.loadout;

import net.runelite.api.ItemID;

/**
 * WD-11 (ADR-0020 §5, PD-D3): the Twisted bow's per-target Magic-level scaling. The Twisted bow's real
 * accuracy and damage rise with the Magic level of the monster it is fired at (its signature raid-scaling
 * mechanic), so a flat per-slot selector under-values it against high-Magic targets and over-values it
 * against low-Magic ones. This evaluator returns the OSRS accuracy/damage multipliers for a given monster
 * Magic level, applied multiplicatively in {@link GearSelector#weaponRank} exactly like a weapon's own
 * conditional bonus - so the bow is never double-counted (ADR-0008 §3.5 no-double-count invariant).
 *
 * <p>Returns {@code null} (base-stats ranking, FR-6 byte-identical) when the weapon is not the Twisted
 * bow, or when the monster's Magic level is unknown (honest UNKNOWN, ADR-0019 - never a fabricated
 * multiplier). The formulas are the published OSRS ones: Magic is capped at {@value #MAGIC_CAP} first,
 * then accuracy is capped at +{@value #ACC_CAP_PERCENT}% and damage at +{@value #DMG_CAP_PERCENT}%.
 */
public final class TwistedBowEvaluator
{
    /** The Twisted bow item id (pinned from {@link ItemID}, the RuneTable-pinning discipline). */
    public static final int TWISTED_BOW = ItemID.TWISTED_BOW;

    /** Monster Magic level is capped at this value before scaling (non-raid ceiling). */
    static final int MAGIC_CAP = 250;

    /** The accuracy bonus percentage ceiling (+140%). */
    static final int ACC_CAP_PERCENT = 140;

    /** The damage bonus percentage ceiling (+250%). */
    static final int DMG_CAP_PERCENT = 250;

    private TwistedBowEvaluator()
    {
    }

    /**
     * The Twisted bow's accuracy/damage multipliers for a target of the given Magic level, or
     * {@code null} when the weapon is not the Twisted bow or the Magic level is unknown.
     *
     * @return {@code {accuracyMultiplier, damageMultiplier}} (e.g. {@code {1.40, 2.15}}), or null.
     */
    public static double[] scaling(int weaponId, Integer magicLevel)
    {
        if (weaponId != TWISTED_BOW || magicLevel == null)
        {
            return null;
        }
        int m = Math.min(magicLevel, MAGIC_CAP);
        int accPct = clamp(140 + Math.floorDiv(3 * m - 10, 100)
            - Math.floorDiv(sq(3 * m / 10 - 100), 100), ACC_CAP_PERCENT);
        int dmgPct = clamp(250 + Math.floorDiv(3 * m - 14, 100)
            - Math.floorDiv(sq(3 * m / 10 - 140), 100), DMG_CAP_PERCENT);
        return new double[] {accPct / 100.0, dmgPct / 100.0};
    }

    private static int clamp(int pct, int cap)
    {
        return Math.max(0, Math.min(pct, cap));
    }

    private static int sq(int x)
    {
        return x * x;
    }
}

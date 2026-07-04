package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.StrategyMethod;
import java.util.List;
import java.util.Locale;
import lombok.Value;

/**
 * Sizes a trip's sustain resources - how many restore potions vs how much food - from the selected
 * method's prayers, the monster's offence, and the remaining kill count. Pure and static (no client
 * access), like {@link DefaultDpsEstimator}, so it stays trivially reasoned about and headless.
 *
 * <p>The key refinement (owner's direction): when the method leans on a <b>protection prayer</b> that
 * covers what the monster throws (K'ril, Tormented demons - Protect from Melee), prayer/restore
 * potions become the PRIMARY sustain resource, sized to the trip's prayer drain, and food drops to a
 * small emergency reserve. When the method only uses offensive prayers (Piety/Rigour/Augury) food
 * still leads but a few restores are packed for the prayer upkeep. With no prayers at all the model
 * asks for nothing extra and the planner reproduces today's food-led convention.
 *
 * <p><b>Modelling gaps (deliberate, conservative):</b> gear prayer bonus is unavailable from the stat
 * seam, so overhead drain is a flat 1 point / 3 s (this slightly over-packs restores - the safe
 * direction); prayer flicking and monster accuracy are not modelled. Any missing input (no TTK) falls
 * back to a fixed heavy-restore reserve rather than a computed one.
 */
public final class SustainModel
{
    /** How the trip is sustained; drives the note and the planner's food-vs-restore split. */
    public enum Profile
    {
        PRAYER_PRIMARY,
        FOOD_PRIMARY
    }

    /** Overhead protection drains one prayer point roughly every this-many seconds at 0 prayer bonus. */
    private static final double DRAIN_SECONDS_PER_POINT = 3.0;
    /** Cap the trip length the drain is sized against, so a huge task does not ask for an absurd stack. */
    private static final double MAX_TRIP_SECONDS = 2 * 60 * 60;
    /** A trip with no kill count clamps to this so the model still produces a sane bag. */
    private static final int DEFAULT_TRIP_KILLS = 50;
    private static final int MAX_RESTORE_SLOTS = 9;
    private static final int OFFENSIVE_PRAYER_RESTORE_SLOTS = 3;
    private static final int FALLBACK_RESTORE_SLOTS = 6;

    private SustainModel()
    {
    }

    @Value
    public static class Result
    {
        Profile profile;
        /** Restore/prayer-potion slots to pack (0 = none; the planner then follows today's convention). */
        int restoreSlots;
        /** A human-readable sustain line for the loadout card, or null when there is nothing to say. */
        String note;
    }

    /**
     * @param method the selected strategy method (its {@code prayers} drive the profile); may be null
     * @param offence the monster's offence (its {@code hitpoints} give TTK); may be null
     * @param dps the recommendation's estimated DPS (for TTK); &le; 0 means unknown
     * @param stats the player's stats ({@code prayer} sizes the restore count); never null
     * @param remaining the remaining task kill count (SLAYER_COUNT); &le; 0 clamps to a default trip
     */
    public static Result estimate(StrategyMethod method, MonsterOffence offence, double dps,
        PlayerStats stats, int remaining)
    {
        List<String> prayers = method == null ? null : method.getPrayers();
        boolean hasProtection = coversAttackStyle(prayers, offence);
        boolean hasAnyPrayer = prayers != null && !prayers.isEmpty();

        if (hasProtection)
        {
            int effectiveKills = remaining <= 0 ? DEFAULT_TRIP_KILLS : remaining;
            Double ttk = timeToKillSeconds(offence, dps);
            if (ttk == null)
            {
                return new Result(Profile.PRAYER_PRIMARY, FALLBACK_RESTORE_SLOTS,
                    "Prayer-primary: pack prayer/restore potions - this method relies on a protection prayer.");
            }
            double tripSeconds = Math.min(effectiveKills * ttk, MAX_TRIP_SECONDS);
            double pointsNeeded = tripSeconds / DRAIN_SECONDS_PER_POINT;
            double startingPool = Math.max(1, stats.getPrayer());
            double netPoints = Math.max(0, pointsNeeded - startingPool);
            double restorePerDose = Math.floor(startingPool / 4.0) + 7;
            int dosesNeeded = (int) Math.ceil(netPoints / Math.max(1, restorePerDose));
            int slots = Math.max(1, (int) Math.ceil(dosesNeeded / 4.0));
            boolean capped = slots > MAX_RESTORE_SLOTS;
            slots = Math.min(slots, MAX_RESTORE_SLOTS);
            String note = "Prayer-primary: ~" + slots + " restore potion(s) for " + effectiveKills
                + " kills under a protection prayer"
                + (capped ? "; a long task - expect multiple trips." : ".");
            return new Result(Profile.PRAYER_PRIMARY, slots, note);
        }

        if (hasAnyPrayer)
        {
            return new Result(Profile.FOOD_PRIMARY, OFFENSIVE_PRAYER_RESTORE_SLOTS,
                "Food-primary: a few restores for the offensive prayer upkeep.");
        }

        return new Result(Profile.FOOD_PRIMARY, 0, null);
    }

    /** TTK in seconds from monster HP and estimated DPS, or null when either is missing/non-positive. */
    private static Double timeToKillSeconds(MonsterOffence offence, double dps)
    {
        if (offence == null || offence.getHitpoints() == null || offence.getHitpoints() <= 0 || dps <= 0)
        {
            return null;
        }
        return offence.getHitpoints() / dps;
    }

    /**
     * True when the method's prayers include a "Protect from X" that matches one of the monster's
     * attack styles. When the monster's attack styles are unknown, any "Protect from" prayer is trusted
     * (the author named it for a reason) - the plan's absent-attack-styles rule.
     */
    private static boolean coversAttackStyle(List<String> prayers, MonsterOffence offence)
    {
        if (prayers == null || prayers.isEmpty())
        {
            return false;
        }
        boolean anyProtect = false;
        for (String prayer : prayers)
        {
            if (prayer == null)
            {
                continue;
            }
            String lower = prayer.toLowerCase(Locale.ROOT);
            if (!lower.contains("protect from"))
            {
                continue;
            }
            anyProtect = true;
            List<AttackStyle> styles = offence == null ? null : offence.getAttackStyles();
            if (styles == null || styles.isEmpty())
            {
                return true; // unknown styles: trust the authored protection prayer
            }
            for (AttackStyle style : styles)
            {
                if (style == AttackStyle.MELEE && lower.contains("melee"))
                {
                    return true;
                }
                if (style == AttackStyle.RANGED && (lower.contains("missile") || lower.contains("range")))
                {
                    return true;
                }
                if (style == AttackStyle.MAGIC && (lower.contains("magic") || lower.contains("mage")))
                {
                    return true;
                }
            }
        }
        // A protection prayer was named but did not textually match a known style: still treat as
        // protection-led (the author asserted it) rather than silently dropping to food.
        return anyProtect;
    }
}

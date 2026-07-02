package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import lombok.Value;

/**
 * A curated task-conditional offensive multiplier (ADR-0007/0008/0009): the per-style factors applied
 * to offence when {@link #condition} holds for the current task. Each style carries a SEPARATE
 * accuracy and damage multiplier (ADR-0009 A.1) so asymmetric sources fit - e.g. Keris is +33% damage
 * only ({@code dmg=1.33}, {@code acc=1.0}) vs kalphites. The stored values are OSRS multipliers
 * ({@code 1.20} = +20%); {@code 1.0} means no bonus for that roll/style.
 *
 * <p>Every gear source (salve, black-mask, slayer-helm) and most weapon sources (DHL, demonbane) are
 * <b>symmetric</b> (accuracy == damage) and are built with {@link #symmetric}, which keeps them
 * byte-for-byte equivalent to the pre-split model. {@link #asymmetric} is for the rare acc != dmg
 * weapon source (Keris, and later DHCB / dragon hunter wand).
 *
 * <p><b>Invariant (test-guarded):</b> an asymmetric bonus may live only on a weapon-slot item - the
 * non-weapon additive {@code (m-1)*L} term uses a single symmetric multiplier (ADR-0009 A.1).
 */
@Value
public class ConditionalBonus
{
    BonusCondition condition;
    double meleeAcc;
    double meleeDmg;
    double rangedAcc;
    double rangedDmg;
    double magicAcc;
    double magicDmg;

    /** A symmetric source: accuracy == damage for every style. */
    public static ConditionalBonus symmetric(BonusCondition condition, double melee, double ranged,
        double magic)
    {
        return new ConditionalBonus(condition, melee, melee, ranged, ranged, magic, magic);
    }

    /** An asymmetric source: separate accuracy and damage multipliers per style (weapon-only). */
    public static ConditionalBonus asymmetric(BonusCondition condition, double meleeAcc,
        double meleeDmg, double rangedAcc, double rangedDmg, double magicAcc, double magicDmg)
    {
        return new ConditionalBonus(condition, meleeAcc, meleeDmg, rangedAcc, rangedDmg, magicAcc,
            magicDmg);
    }

    /** @return the accuracy multiplier for the style (1.0 = no bonus / unset / unknown style). */
    public double accMultiplier(CombatStyle style)
    {
        if (style == null)
        {
            return 1.0;
        }
        switch (style)
        {
            case MELEE:
                return meleeAcc;
            case RANGED:
                return rangedAcc;
            case MAGIC:
                return magicAcc;
            default:
                return 1.0;
        }
    }

    /** @return the damage multiplier for the style (1.0 = no bonus / unset / unknown style). */
    public double dmgMultiplier(CombatStyle style)
    {
        if (style == null)
        {
            return 1.0;
        }
        switch (style)
        {
            case MELEE:
                return meleeDmg;
            case RANGED:
                return rangedDmg;
            case MAGIC:
                return magicDmg;
            default:
                return 1.0;
        }
    }

    /** @return whether accuracy == damage for every style (a symmetric source). */
    public boolean isSymmetric()
    {
        return meleeAcc == meleeDmg && rangedAcc == rangedDmg && magicAcc == magicDmg;
    }
}

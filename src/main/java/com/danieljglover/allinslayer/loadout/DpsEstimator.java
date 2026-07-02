package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.google.inject.ImplementedBy;
import java.util.Map;

@ImplementedBy(DefaultDpsEstimator.class)
public interface DpsEstimator
{
    /**
     * Full-loadout sustained-DPS estimate for display (ADR-0006): sums the worn map's offensive
     * bonuses, applies the higher-of conditional multiplier and the worn weapon's passive
     * {@link WeaponEffect}, and returns DPS against the selected {@link MonsterProfile}'s defence.
     * The {@code style} is the effective method (selected or recommended, ADR-0013).
     */
    double estimate(CombatStyle style, Map<EquipmentSlot, Integer> equipped, PlayerStats stats,
        MonsterProfile profile, int spellBaseMaxHit);

    /**
     * Selection-grade sustained DPS for ONE candidate weapon in isolation (ADR-0008 section 3.2),
     * scored on the player's base stats against the monster's defence. Handles MELEE and RANGED only
     * (magic weapon ranking is gear-aware - see ADR-0009). Pure: stats arrive via the resolved
     * {@link Bonuses}; no {@code ItemManager} / EDT.
     *
     * @param weapon the candidate weapon's resolved bonuses (incl. {@code attackSpeedTicks})
     * @param player the player's live skill levels
     * @param def    the monster's defence (null -> treated as zero defence)
     * @param style  MELEE or RANGED
     * @param effect the weapon's passive special effect ({@link WeaponEffect#NONE} for a normal weapon)
     * @param accMult the weapon's OWN task-conditional ACCURACY multiplier (e.g. Dragon hunter lance
     *     vs a dragon task), or {@code 1.0} when none applies
     * @param dmgMult the weapon's OWN task-conditional DAMAGE multiplier (Keris is damage-only:
     *     {@code acc=1.0}, {@code dmg=1.382}), or {@code 1.0} when none applies
     * @return sustained DPS, or {@code 0} for a non-weapon / unusable candidate
     */
    double weaponDps(Bonuses weapon, PlayerStats player, MonsterDefence def, CombatStyle style,
        WeaponEffect effect, double accMult, double dmgMult);

    /**
     * Gear-aware magic sustained DPS for a candidate magic weapon (ADR-0009 B.3). Magic ranking is
     * gear-aware (not weapon-only) because Tumeken's Shadow's value comes from multiplying OTHER gear;
     * the caller sums the chosen non-weapon magic gear into {@code matt}/{@code mdmg} (applying any
     * weapon-specific gear multiplier) and passes the spell's {@code baseMaxHit} + cast {@code speed}
     * from {@link MagicWeaponEvaluator}. Shares the same hit-chance / dps core as {@link #estimate}.
     *
     * @param baseMaxHit the chosen spell / powered-staff base max hit (before magic-damage %)
     * @param speedTicks the cast speed in ticks (powered staff: its attack speed; standard caster: 5)
     * @param matt       total magic attack (weapon + gear, gear x3 for the Shadow)
     * @param mdmg       total magic damage % (weapon + gear, gear x3 capped +100 for the Shadow)
     * @param accMult    the weapon's own task-conditional accuracy multiplier (e.g. Thammaron's
     *     sceptre in the Wilderness), or {@code 1.0}
     * @param dmgMult    the weapon's own task-conditional damage multiplier, or {@code 1.0}
     * @return sustained magic DPS, or {@code 0} when no spell is castable ({@code baseMaxHit <= 0})
     */
    double magicWeaponDps(PlayerStats player, MonsterDefence def, int baseMaxHit, int speedTicks,
        int matt, int mdmg, double accMult, double dmgMult);
}

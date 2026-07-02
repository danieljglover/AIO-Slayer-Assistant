package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultDpsEstimator implements DpsEstimator
{
    private final EquipmentStatsProvider provider;

    @Inject
    public DefaultDpsEstimator(EquipmentStatsProvider provider)
    {
        this.provider = provider;
    }

    @Override
    public double estimate(CombatStyle style, Map<EquipmentSlot, Integer> equipped, PlayerStats stats,
        MonsterProfile profile, int spellBaseMaxHit)
    {
        int astab = 0, aslash = 0, acrush = 0, amagic = 0, arange = 0;
        int meleeStr = 0, rangedStr = 0, magicDmg = 0, weaponSpeed = 4;
        int weaponAmagic = 0, weaponMdmg = 0, weaponId = 0;
        WeaponEffect weaponEffect = WeaponEffect.NONE;
        for (Map.Entry<EquipmentSlot, Integer> entry : equipped.entrySet())
        {
            Bonuses b = provider.get(entry.getValue());
            if (b == null)
            {
                continue;
            }
            astab += b.getAstab();
            aslash += b.getAslash();
            acrush += b.getAcrush();
            amagic += b.getAmagic();
            arange += b.getArange();
            meleeStr += b.getMeleeStr();
            rangedStr += b.getRangedStr();
            magicDmg += b.getMagicDmgPercent();
            if (entry.getKey() == EquipmentSlot.WEAPON)
            {
                if (b.getAttackSpeedTicks() > 0)
                {
                    weaponSpeed = b.getAttackSpeedTicks();
                }
                // The shown Est. DPS reflects the worn weapon's passive effect (Fang reroll / Scythe
                // multi-hit) so the number matches why the weapon was picked (ADR-0008 WDB-8).
                weaponEffect = WeaponEffectRegistry.lookup(entry.getValue());
                weaponId = entry.getValue();
                weaponAmagic = b.getAmagic();
                weaponMdmg = b.getMagicDmgPercent();
            }
        }

        // Task-conditional offensive multiplier (LFB-5 / design section 3.5): the applicable on-task
        // bonus (Slayer-helm/Black-mask on task, Salve vs undead) for the actually-equipped items,
        // from the SAME registry the selector ranks with. Applied to accuracy and max hit alike, as
        // these bonuses boost both rolls in OSRS. Only the HIGHER applicable multiplier counts - salve
        // and black-mask do not stack on one hit (section 3.4) - so this is display-correct, not the
        // selector's independent per-slot lift.
        BonusContext ctx = BonusContext.from(profile);
        double accM = conditionalMultiplier(equipped, style, ctx, false);
        double dmgM = conditionalMultiplier(equipped, style, ctx, true);
        MonsterDefence def = profile == null ? null : profile.getDefence();

        switch (style)
        {
            case MELEE:
                return meleeCore(astab, aslash, acrush, meleeStr, weaponSpeed, stats, def, accM, dmgM,
                    weaponEffect);
            case RANGED:
                return rangedCore(arange, rangedStr, weaponSpeed, stats, def, accM, dmgM, weaponEffect);
            case MAGIC:
            {
                if (spellBaseMaxHit <= 0)
                {
                    return 0; // no castable spell -> no magic dps to display (ADR-0006)
                }
                // Tumeken's Shadow triples the NON-weapon gear's magic attack/damage (cap +100), so
                // the shown DPS matches the gear-aware pick (ADR-0009 B.5). For any other weapon this
                // is the plain worn total (weapon + gear).
                int gearMatt = amagic - weaponAmagic;
                int gearMdmg = magicDmg - weaponMdmg;
                int effMatt = MagicWeaponEvaluator.effectiveMagicAttack(weaponId, weaponAmagic, gearMatt);
                int effMdmg = MagicWeaponEvaluator.effectiveMagicDamage(weaponId, weaponMdmg, gearMdmg);
                double maxHit = spellBaseMaxHit * (1 + effMdmg / 100.0) * dmgM;
                int effMag = stats.getMagic() + 8;
                double atkRoll = effMag * (effMatt + 64) * accM;
                double defRoll = (defenceLevel(def) + 9) * (magicDefence(def) + 64);
                return dps(hitChance(atkRoll, defRoll), maxHit, weaponSpeed, WeaponEffect.NONE);
            }
            default:
                return 0;
        }
    }

    @Override
    public double weaponDps(Bonuses weapon, PlayerStats player, MonsterDefence def, CombatStyle style,
        WeaponEffect effect, double accMult, double dmgMult)
    {
        if (weapon == null)
        {
            return 0;
        }
        WeaponEffect fx = effect == null ? WeaponEffect.NONE : effect;
        int speed = weapon.getAttackSpeedTicks() > 0 ? weapon.getAttackSpeedTicks() : 4;
        switch (style)
        {
            case MELEE:
                return meleeCore(weapon.getAstab(), weapon.getAslash(), weapon.getAcrush(),
                    weapon.getMeleeStr(), speed, player, def, accMult, dmgMult, fx);
            case RANGED:
                return rangedCore(weapon.getArange(), weapon.getRangedStr(), speed, player, def,
                    accMult, dmgMult, fx);
            default:
                return 0; // magic weapon DPS is gear-aware, not weapon-only (ADR-0009)
        }
    }

    @Override
    public double magicWeaponDps(PlayerStats player, MonsterDefence def, int baseMaxHit, int speedTicks,
        int matt, int mdmg, double accMult, double dmgMult)
    {
        if (baseMaxHit <= 0)
        {
            return 0; // no castable spell -> no magic DPS
        }
        int speed = speedTicks > 0 ? speedTicks : MagicWeaponEvaluator.STANDARD_CAST_SPEED;
        double maxHit = baseMaxHit * (1 + mdmg / 100.0) * dmgMult;
        int effMag = player.getMagic() + 8;
        double atkRoll = effMag * (matt + 64) * accMult;
        double defRoll = (defenceLevel(def) + 9) * (magicDefence(def) + 64);
        return dps(hitChance(atkRoll, defRoll), maxHit, speed, WeaponEffect.NONE);
    }

    // --- shared DPS core (design section 3.3) --------------------------------------------------

    private double meleeCore(int astab, int aslash, int acrush, int meleeStr, int weaponSpeed,
        PlayerStats stats, MonsterDefence def, double accMult, double dmgMult, WeaponEffect effect)
    {
        int effStr = stats.getStrength() + 8;
        int effAtk = stats.getAttack() + 8;
        double maxHit = Math.floor(0.5 + effStr * (meleeStr + 64) / 640.0) * dmgMult;
        int[][] pairs = {
            {astab, stab(def)}, {aslash, slash(def)}, {acrush, crush(def)}
        };
        double bestP = 0;
        for (int[] p : pairs)
        {
            double atkRoll = effAtk * (p[0] + 64) * accMult;
            double defRoll = (defenceLevel(def) + 9) * (p[1] + 64);
            bestP = Math.max(bestP, hitChance(atkRoll, defRoll));
        }
        return dps(bestP, maxHit, weaponSpeed, effect);
    }

    private double rangedCore(int arange, int rangedStr, int weaponSpeed, PlayerStats stats,
        MonsterDefence def, double accMult, double dmgMult, WeaponEffect effect)
    {
        int effRng = stats.getRanged() + 8;
        double maxHit = Math.floor(0.5 + effRng * (rangedStr + 64) / 640.0) * dmgMult;
        double atkRoll = effRng * (arange + 64) * accMult;
        double defRoll = (defenceLevel(def) + 9) * (rangeDefence(def) + 64);
        return dps(hitChance(atkRoll, defRoll), maxHit, weaponSpeed, effect);
    }

    /**
     * The highest applicable task-conditional offensive multiplier for the equipped items and style,
     * for the accuracy ({@code damage=false}) or damage ({@code damage=true}) roll - or {@code 1.0}
     * when none applies. Takes the max - never the product - so the salve and black-mask families do
     * not stack (design 3.4). Splitting accuracy from damage lets an asymmetric worn weapon (Keris is
     * damage-only) be reflected correctly without wrongly boosting accuracy (ADR-0009 A.1); symmetric
     * sources give acc == dmg, so this is behaviour-preserving for all gear.
     */
    private double conditionalMultiplier(Map<EquipmentSlot, Integer> equipped, CombatStyle style,
        BonusContext ctx, boolean damage)
    {
        double m = 1.0;
        for (Integer id : equipped.values())
        {
            if (id == null)
            {
                continue;
            }
            ConditionalBonus bonus = ConditionalBonusRegistry.lookup(id);
            if (bonus != null && bonus.getCondition().test(ctx))
            {
                m = Math.max(m, damage ? bonus.dmgMultiplier(style) : bonus.accMultiplier(style));
            }
        }
        return m;
    }

    private double hitChance(double atk, double def)
    {
        return atk > def ? 1 - (def + 2) / (2 * (atk + 1)) : atk / (2 * (def + 1));
    }

    /** Average per-swing damage / attack interval, with the weapon's passive effect applied. */
    private double dps(double singleRollHitChance, double maxHit, int speedTicks, WeaponEffect effect)
    {
        double hc = 1 - Math.pow(1 - singleRollHitChance, effect.getAccuracyRolls());
        double avgHit = hc * (maxHit / 2.0) * effect.getDamageMultiplier();
        return avgHit / (speedTicks * 0.6);
    }

    private static int defenceLevel(MonsterDefence d)
    {
        return d == null ? 0 : d.getDefenceLevel();
    }

    private static int stab(MonsterDefence d)
    {
        return d == null ? 0 : d.getStab();
    }

    private static int slash(MonsterDefence d)
    {
        return d == null ? 0 : d.getSlash();
    }

    private static int crush(MonsterDefence d)
    {
        return d == null ? 0 : d.getCrush();
    }

    private static int magicDefence(MonsterDefence d)
    {
        return d == null ? 0 : d.getMagic();
    }

    private static int rangeDefence(MonsterDefence d)
    {
        return d == null ? 0 : d.getRange();
    }
}

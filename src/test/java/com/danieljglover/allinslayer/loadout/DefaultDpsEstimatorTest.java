package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultDpsEstimatorTest
{
    private static final double EPS = 1e-9;

    // MV-B4: the estimator now consumes a MonsterProfile. These helpers wrap the task-level profile via
    // MonsterProfile.fromTask, so the migration is FR-6 byte-identical (default method/profile).
    private MonsterProfile meleeTask()
    {
        TaskData t = new TaskData();
        t.setSlayerHelmApplies(false);
        t.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        return MonsterProfile.fromTask(t);
    }

    private MonsterProfile task(boolean slayerHelmApplies, boolean undead)
    {
        TaskData t = new TaskData();
        t.setSlayerHelmApplies(slayerHelmApplies);
        t.setUndead(undead);
        t.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        return MonsterProfile.fromTask(t);
    }

    @Test
    public void higherStrengthGearGivesHigherMeleeDps()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(1, new Bonuses(70, 70, 70, 0, 0, 80, 0, 0, 4));
        table.put(2, new Bonuses(20, 20, 20, 0, 0, 5, 0, 0, 4));
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99);
        Map<EquipmentSlot, Integer> strong = new EnumMap<>(EquipmentSlot.class);
        strong.put(EquipmentSlot.WEAPON, 1);
        Map<EquipmentSlot, Integer> weak = new EnumMap<>(EquipmentSlot.class);
        weak.put(EquipmentSlot.WEAPON, 2);

        double dpsStrong = est.estimate(CombatStyle.MELEE, strong, stats, meleeTask(), 0);
        double dpsWeak = est.estimate(CombatStyle.MELEE, weak, stats, meleeTask(), 0);

        assertTrue("strong gear should out-dps weak gear", dpsStrong > dpsWeak);
    }

    @Test
    public void magicUsesPassedSpellBaseMaxHitNotTaskData()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(10, new Bonuses(0, 0, 0, 60, 0, 0, 0, 0, 5));
        table.put(11, new Bonuses(0, 0, 0, 60, 0, 0, 0, 20, 5));
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        // No loadouts on the task at all: the estimate must come purely from the passed base max hit.
        TaskData t = new TaskData();
        t.setMonsterDefence(new MonsterDefence(80, 0, 0, 0, 0, 0));

        Map<EquipmentSlot, Integer> base = new EnumMap<>(EquipmentSlot.class);
        base.put(EquipmentSlot.WEAPON, 10);
        Map<EquipmentSlot, Integer> boosted = new EnumMap<>(EquipmentSlot.class);
        boosted.put(EquipmentSlot.WEAPON, 11);

        double dpsBase = est.estimate(CombatStyle.MAGIC, base, stats(), MonsterProfile.fromTask(t), 30);
        double dpsBoosted = est.estimate(CombatStyle.MAGIC, boosted, stats(), MonsterProfile.fromTask(t), 30);

        assertTrue("passed spell base max hit drives a positive magic dps", dpsBase > 0);
        assertTrue("magic dmg % should raise dps", dpsBoosted > dpsBase);
    }

    @Test
    public void magicWithZeroSpellBaseMaxHitYieldsNoDps()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(10, new Bonuses(0, 0, 0, 60, 0, 0, 0, 0, 5));
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        TaskData t = new TaskData();
        t.setMonsterDefence(new MonsterDefence(80, 0, 0, 0, 0, 0));
        Map<EquipmentSlot, Integer> base = new EnumMap<>(EquipmentSlot.class);
        base.put(EquipmentSlot.WEAPON, 10);

        assertEquals(0.0, est.estimate(CombatStyle.MAGIC, base, stats(), MonsterProfile.fromTask(t), 0), 1e-9);
    }

    @Test
    public void fasterWeaponWinsEvenWithArmorEquipped()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(1, new Bonuses(130, 130, 130, 0, 0, 130, 0, 0, 7)); // slow, strong
        table.put(2, new Bonuses(82, 82, 82, 0, 0, 82, 0, 0, 4));      // fast, weaker
        table.put(99, new Bonuses(0, 0, 0, 0, 0, 10, 0, 0, 0));        // body armor: no attack speed
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99);
        Map<EquipmentSlot, Integer> slow = new EnumMap<>(EquipmentSlot.class);
        slow.put(EquipmentSlot.WEAPON, 1);
        slow.put(EquipmentSlot.BODY, 99);
        Map<EquipmentSlot, Integer> fast = new EnumMap<>(EquipmentSlot.class);
        fast.put(EquipmentSlot.WEAPON, 2);
        fast.put(EquipmentSlot.BODY, 99);

        double dpsSlow = est.estimate(CombatStyle.MELEE, slow, stats, meleeTask(), 0);
        double dpsFast = est.estimate(CombatStyle.MELEE, fast, stats, meleeTask(), 0);

        assertTrue("faster weapon should win despite armor in the loadout", dpsFast > dpsSlow);
    }

    @Test
    public void higherRangedStrengthGearGivesHigherRangedDps()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(20, new Bonuses(0, 0, 0, 0, 60, 0, 80, 0, 4));
        table.put(21, new Bonuses(0, 0, 0, 0, 30, 0, 10, 0, 4));
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99);
        Map<EquipmentSlot, Integer> strong = new EnumMap<>(EquipmentSlot.class);
        strong.put(EquipmentSlot.WEAPON, 20);
        Map<EquipmentSlot, Integer> weak = new EnumMap<>(EquipmentSlot.class);
        weak.put(EquipmentSlot.WEAPON, 21);

        double dpsStrong = est.estimate(CombatStyle.RANGED, strong, stats, meleeTask(), 0);
        double dpsWeak = est.estimate(CombatStyle.RANGED, weak, stats, meleeTask(), 0);

        assertTrue(dpsStrong > dpsWeak);
    }

    // LFB-5 (design section 3.5): the displayed Est. DPS must reflect the applicable on-task
    // conditional damage bonus, drawn from the SAME ConditionalBonusRegistry the selector uses.

    @Test
    public void slayerHelmOnTaskRaisesMeleeDpsAndOffTaskDoesNot()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(1, new Bonuses(70, 70, 70, 0, 0, 80, 0, 0, 4));            // weapon
        table.put(ItemID.SLAYER_HELMET_I, new Bonuses(5, 5, 5, 0, 0, 5, 0, 0, 0)); // head
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 1);
        worn.put(EquipmentSlot.HEAD, ItemID.SLAYER_HELMET_I);

        double onTask = est.estimate(CombatStyle.MELEE, worn, stats(), task(true, false), 0);
        double offTask = est.estimate(CombatStyle.MELEE, worn, stats(), task(false, false), 0);

        assertTrue("slayer helm on-task must raise the shown DPS", onTask > offTask);
    }

    @Test
    public void salveRaisesDpsVsUndeadAndNotOtherwise()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(1, new Bonuses(70, 70, 70, 0, 0, 80, 0, 0, 4));               // weapon
        table.put(ItemID.SALVE_AMULETEI, new Bonuses(0, 0, 0, 0, 0, 0, 0, 0, 0)); // salve: ~0 flat
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 1);
        worn.put(EquipmentSlot.AMULET, ItemID.SALVE_AMULETEI);

        double undead = est.estimate(CombatStyle.MELEE, worn, stats(), task(false, true), 0);
        double notUndead = est.estimate(CombatStyle.MELEE, worn, stats(), task(false, false), 0);

        assertTrue("salve (ei) vs undead must raise the shown DPS", undead > notUndead);
    }

    @Test
    public void salveRaisesMagicDpsVsUndead()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(10, new Bonuses(0, 0, 0, 60, 0, 0, 0, 0, 5));                  // magic weapon
        table.put(ItemID.SALVE_AMULETEI, new Bonuses(0, 0, 0, 0, 0, 0, 0, 0, 0)); // salve
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 10);
        worn.put(EquipmentSlot.AMULET, ItemID.SALVE_AMULETEI);

        double undead = est.estimate(CombatStyle.MAGIC, worn, stats(), task(false, true), 30);
        double notUndead = est.estimate(CombatStyle.MAGIC, worn, stats(), task(false, false), 30);

        assertTrue("salve (ei) magic multiplier must raise magic DPS vs undead", undead > notUndead);
    }

    @Test
    public void helmAndSalveDoNotStackOnlyTheHigherMultiplierApplies()
    {
        // Plain slayer helm = melee x1.1667; salve (ei) = melee x1.20. On a task that is BOTH on-task
        // and undead, only the HIGHER (salve, 1.20) may apply - never the product (design section 3.4/3.5).
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(1, new Bonuses(70, 70, 70, 0, 0, 80, 0, 0, 4));                  // weapon
        table.put(ItemID.SLAYER_HELMET, new Bonuses(5, 5, 5, 0, 0, 5, 0, 0, 0));   // head, registry
        table.put(ItemID.SALVE_AMULETEI, new Bonuses(0, 0, 0, 0, 0, 0, 0, 0, 0));  // amulet, registry
        table.put(9001, new Bonuses(5, 5, 5, 0, 0, 5, 0, 0, 0));                   // plain head, no bonus
        table.put(9002, new Bonuses(0, 0, 0, 0, 0, 0, 0, 0, 0));                   // plain amulet, no bonus
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        MonsterProfile both = task(true, true);

        Map<EquipmentSlot, Integer> bothWorn = new EnumMap<>(EquipmentSlot.class);
        bothWorn.put(EquipmentSlot.WEAPON, 1);
        bothWorn.put(EquipmentSlot.HEAD, ItemID.SLAYER_HELMET);
        bothWorn.put(EquipmentSlot.AMULET, ItemID.SALVE_AMULETEI);

        Map<EquipmentSlot, Integer> salveOnly = new EnumMap<>(EquipmentSlot.class);
        salveOnly.put(EquipmentSlot.WEAPON, 1);
        salveOnly.put(EquipmentSlot.HEAD, 9001); // identical head stats, no registry bonus
        salveOnly.put(EquipmentSlot.AMULET, ItemID.SALVE_AMULETEI);

        Map<EquipmentSlot, Integer> helmOnly = new EnumMap<>(EquipmentSlot.class);
        helmOnly.put(EquipmentSlot.WEAPON, 1);
        helmOnly.put(EquipmentSlot.HEAD, ItemID.SLAYER_HELMET);
        helmOnly.put(EquipmentSlot.AMULET, 9002); // identical amulet stats, no registry bonus

        double dpsBoth = est.estimate(CombatStyle.MELEE, bothWorn, stats(), both, 0);
        double dpsSalveOnly = est.estimate(CombatStyle.MELEE, salveOnly, stats(), both, 0);
        double dpsHelmOnly = est.estimate(CombatStyle.MELEE, helmOnly, stats(), both, 0);

        assertEquals("helm must add nothing on top of the higher salve multiplier (no stacking)",
            dpsSalveOnly, dpsBoth, EPS);
        assertTrue("the higher (salve x1.20) multiplier must be used, not the lower helm x1.1667",
            dpsBoth > dpsHelmOnly);
    }

    // WDB-8 (ADR-0008 section 3.4/FR-13.9): the displayed Est. DPS reflects the worn weapon's passive
    // WeaponEffect, so the shown number matches the pick rationale.

    @Test
    public void estimateReflectsFangRerollForWornWeapon()
    {
        // Same weapon stats: under the Fang's id (accuracy reroll) the shown DPS is higher than under
        // a plain id (NONE), on a high-defence monster where the reroll matters.
        Map<Integer, Bonuses> table = new HashMap<>();
        Bonuses w = new Bonuses(60, 60, 60, 0, 0, 70, 0, 0, 5);
        table.put(ItemID.OSMUMTENS_FANG, w);
        table.put(9100, w); // identical stats, no registered effect
        DefaultDpsEstimator est = new DefaultDpsEstimator(table::get);

        TaskData t = new TaskData();
        t.setMonsterDefence(new MonsterDefence(250, 250, 250, 250, 250, 250));
        Map<EquipmentSlot, Integer> fang = new EnumMap<>(EquipmentSlot.class);
        fang.put(EquipmentSlot.WEAPON, ItemID.OSMUMTENS_FANG);
        Map<EquipmentSlot, Integer> plain = new EnumMap<>(EquipmentSlot.class);
        plain.put(EquipmentSlot.WEAPON, 9100);

        double dpsFang = est.estimate(CombatStyle.MELEE, fang, stats(), MonsterProfile.fromTask(t), 0);
        double dpsPlain = est.estimate(CombatStyle.MELEE, plain, stats(), MonsterProfile.fromTask(t), 0);
        assertTrue("the shown DPS reflects the fang's accuracy reroll", dpsFang > dpsPlain);
    }

    @Test
    public void estimateReflectsScytheMultiHit()
    {
        // The shown DPS for a worn Scythe is exactly 1.75x the same weapon without the effect.
        Map<Integer, Bonuses> table = new HashMap<>();
        Bonuses w = new Bonuses(60, 60, 60, 0, 0, 70, 0, 0, 5);
        table.put(ItemID.SCYTHE_OF_VITUR, w);
        table.put(9101, w);
        DefaultDpsEstimator est = new DefaultDpsEstimator(table::get);

        TaskData t = new TaskData();
        t.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        Map<EquipmentSlot, Integer> scythe = new EnumMap<>(EquipmentSlot.class);
        scythe.put(EquipmentSlot.WEAPON, ItemID.SCYTHE_OF_VITUR);
        Map<EquipmentSlot, Integer> plain = new EnumMap<>(EquipmentSlot.class);
        plain.put(EquipmentSlot.WEAPON, 9101);

        double dpsScythe = est.estimate(CombatStyle.MELEE, scythe, stats(), MonsterProfile.fromTask(t), 0);
        double dpsPlain = est.estimate(CombatStyle.MELEE, plain, stats(), MonsterProfile.fromTask(t), 0);
        assertEquals("scythe shown DPS is 1.75x the single-hit DPS", dpsPlain * 1.75, dpsScythe, 1e-6);
    }

    @Test
    public void estimateReflectsShadowGearMultiplier()
    {
        // WDB-16 / FR-14.10: the shown magic Est. DPS triples the non-weapon gear's magic damage for
        // Tumeken's Shadow, so it exceeds the same loadout with a plain magic weapon.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(ItemID.TUMEKENS_SHADOW, new Bonuses(0, 0, 0, 35, 0, 0, 0, 0, 5)); // shadow
        table.put(9200, new Bonuses(0, 0, 0, 35, 0, 0, 0, 0, 5));                    // plain, same stats
        table.put(960, new Bonuses(0, 0, 0, 0, 0, 0, 0, 20, 0));                     // +20% magic dmg gear
        DefaultDpsEstimator est = new DefaultDpsEstimator(table::get);

        TaskData t = new TaskData();
        t.setMonsterDefence(new MonsterDefence(80, 0, 0, 0, 0, 0));
        Map<EquipmentSlot, Integer> shadow = new EnumMap<>(EquipmentSlot.class);
        shadow.put(EquipmentSlot.WEAPON, ItemID.TUMEKENS_SHADOW);
        shadow.put(EquipmentSlot.HEAD, 960);
        Map<EquipmentSlot, Integer> plain = new EnumMap<>(EquipmentSlot.class);
        plain.put(EquipmentSlot.WEAPON, 9200);
        plain.put(EquipmentSlot.HEAD, 960);

        double shadowDps = est.estimate(CombatStyle.MAGIC, shadow, stats(), MonsterProfile.fromTask(t), 34);
        double plainDps = est.estimate(CombatStyle.MAGIC, plain, stats(), MonsterProfile.fromTask(t), 34);
        assertTrue("the Shadow's x3 gear multiplier raises the shown DPS", shadowDps > plainDps);
    }

    // WDB-1 (ADR-0008 section 3.2): the weaponDps selection seam scores a single candidate weapon in
    // isolation against the monster's defence, sharing the same DPS core as estimate().

    @Test
    public void weaponDpsScoresASingleWeaponAgainstMonsterDefence()
    {
        DefaultDpsEstimator est = new DefaultDpsEstimator(id -> null); // weaponDps takes Bonuses directly
        MonsterDefence def = new MonsterDefence(100, 10, 10, 10, 10, 10);

        Bonuses strong = new Bonuses(70, 70, 70, 0, 0, 80, 0, 0, 4);
        Bonuses weak = new Bonuses(20, 20, 20, 0, 0, 5, 0, 0, 4);

        double dpsStrong = est.weaponDps(strong, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.0);
        double dpsWeak = est.weaponDps(weak, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.0);

        assertTrue("a single weapon scores positive sustained DPS", dpsStrong > 0);
        assertTrue("the stronger/more-accurate weapon out-dps the weaker", dpsStrong > dpsWeak);
    }

    @Test
    public void weaponDpsIsZeroForMagicAndNullWeapon()
    {
        DefaultDpsEstimator est = new DefaultDpsEstimator(id -> null);
        MonsterDefence def = new MonsterDefence(100, 10, 10, 10, 10, 10);
        Bonuses w = new Bonuses(0, 0, 0, 60, 0, 0, 0, 0, 5);
        assertEquals("magic weapon DPS is gear-aware, not weapon-only", 0.0,
            est.weaponDps(w, stats(), def, CombatStyle.MAGIC, WeaponEffect.NONE, 1.0, 1.0), EPS);
        assertEquals(0.0, est.weaponDps(null, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.0),
            EPS);
    }

    @Test
    public void weaponDpsMatchesEstimateForALoneWeapon()
    {
        // Core-extraction guard: estimate() over a lone weapon and weaponDps() of the same weapon must
        // agree (both feed the shared core; NONE effect + m=1.0 is the legacy maths).
        Bonuses w = new Bonuses(70, 60, 50, 0, 0, 80, 0, 0, 4);
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(1, w);
        DefaultDpsEstimator est = new DefaultDpsEstimator(table::get);
        MonsterDefence def = new MonsterDefence(100, 10, 20, 30, 10, 10);

        TaskData t = new TaskData();
        t.setMonsterDefence(def);
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 1);

        double viaEstimate = est.estimate(CombatStyle.MELEE, worn, stats(), MonsterProfile.fromTask(t), 0);
        double viaWeaponDps = est.weaponDps(w, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.0);

        assertEquals("the two entry points share the DPS core", viaEstimate, viaWeaponDps, EPS);
    }

    // WDB-3 (ADR-0008 section 3.3/3.4): weaponDps applies the weapon's passive WeaponEffect.

    @Test
    public void weaponDpsAppliesFangAccuracyReroll()
    {
        // Against a high-defence monster (p < 1) the fang's second accuracy roll lifts the effective
        // hit chance: 1-(1-p)^2 > p, so the same weapon scores strictly higher with the fang effect.
        DefaultDpsEstimator est = new DefaultDpsEstimator(id -> null);
        MonsterDefence def = new MonsterDefence(200, 200, 200, 200, 200, 200); // forces p well below 1
        Bonuses w = new Bonuses(60, 60, 60, 0, 0, 70, 0, 0, 5);

        double plain = est.weaponDps(w, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.0);
        double fang = est.weaponDps(w, stats(), def, CombatStyle.MELEE, new WeaponEffect(2, 1.0), 1.0, 1.0);

        assertTrue("the fang accuracy reroll raises effective DPS", fang > plain);
    }

    @Test
    public void conditionalBonusAppliesSeparateAccAndDmgMultipliers()
    {
        // WDB-11 / FR-14.6: weaponDps takes separate accuracy and damage multipliers. A damage-only
        // multiplier (Keris shape: acc 1.0, dmg 1.382) scales DPS by exactly 1.382 (accuracy
        // unchanged); an accuracy-only multiplier raises DPS without scaling the max hit.
        DefaultDpsEstimator est = new DefaultDpsEstimator(id -> null);
        MonsterDefence def = new MonsterDefence(150, 150, 150, 150, 150, 150);
        Bonuses w = new Bonuses(60, 60, 60, 0, 0, 70, 0, 0, 5);

        double plain = est.weaponDps(w, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.0);
        double dmgOnly = est.weaponDps(w, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.382);
        double accOnly = est.weaponDps(w, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.20, 1.0);

        assertEquals("damage-only multiplier scales DPS by 1.382", plain * 1.382, dmgOnly, 1e-6);
        assertTrue("accuracy-only multiplier raises DPS", accOnly > plain);
        assertTrue("accuracy-only raises DPS by less than the 1.382 damage multiplier",
            accOnly < dmgOnly);
    }

    @Test
    public void weaponDpsAppliesScytheDamageMultiplier()
    {
        // The scythe multiplies average damage by exactly 1.75 (accuracy unchanged).
        DefaultDpsEstimator est = new DefaultDpsEstimator(id -> null);
        MonsterDefence def = new MonsterDefence(100, 10, 10, 10, 10, 10);
        Bonuses w = new Bonuses(60, 60, 60, 0, 0, 70, 0, 0, 5);

        double plain = est.weaponDps(w, stats(), def, CombatStyle.MELEE, WeaponEffect.NONE, 1.0, 1.0);
        double scythe = est.weaponDps(w, stats(), def, CombatStyle.MELEE, new WeaponEffect(1, 1.75), 1.0, 1.0);

        assertEquals("scythe scales average damage by 1.75x", plain * 1.75, scythe, 1e-6);
    }

    private PlayerStats stats()
    {
        return new PlayerStats(99, 99, 99, 99, 99, 99);
    }
}

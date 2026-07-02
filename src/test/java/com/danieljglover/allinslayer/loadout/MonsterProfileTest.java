package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.Weakness;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MV-B2: {@link MonsterProfile} is the unit the engine consumes (ADR-0010). {@code fromTask} equals
 * today's task-level profile (FR-6 anchor); {@code fromVariant} overlays the variant's fields with a
 * per-field fallback to the task default (ADR-0012.3), and the category flags follow the variant (FR-5).
 */
public class MonsterProfileTest
{
    private static TaskData task()
    {
        TaskData t = new TaskData();
        t.setWeakness(new Weakness(CombatStyle.MELEE, null));
        t.setMonsterDefence(new MonsterDefence(70, 1, 2, 3, 0, 0));
        t.setSlayerHelmApplies(true);
        t.setDemon(true);
        return t;
    }

    @Test
    public void fromTaskEqualsTheTaskLevelProfile()
    {
        MonsterProfile p = MonsterProfile.fromTask(task());
        assertEquals(CombatStyle.MELEE, p.getWeakness().getStyle());
        assertEquals(CombatStyle.MELEE, p.recommendedStyle());
        assertEquals(70, p.getDefence().getDefenceLevel());
        assertTrue("slayerHelmApplies comes from the task", p.isSlayerHelmApplies());
        assertTrue("demon flag preserved", p.isDemon());
        assertFalse(p.isDragon());
        assertFalse(p.isKalphite());
        assertFalse(p.isUndead());
    }

    @Test
    public void fromVariantOverlaysVariantFields()
    {
        MonsterVariant v = new MonsterVariant();
        v.setName("K'ril Tsutsaroth");
        v.setWeakness(new Weakness(CombatStyle.MAGIC, "fire"));
        v.setMonsterDefence(new MonsterDefence(240, 5, 5, 5, 0, 0));
        v.setDemon(true);

        MonsterProfile p = MonsterProfile.fromVariant(task(), v);
        assertEquals("variant weakness overlays the task", CombatStyle.MAGIC, p.getWeakness().getStyle());
        assertEquals("fire", p.getWeakness().getElement());
        assertEquals("variant defence overlays the task", 240, p.getDefence().getDefenceLevel());
        assertTrue("variant demon flag", p.isDemon());
        // slayerHelmApplies always comes from the task, never the variant.
        assertTrue(p.isSlayerHelmApplies());
    }

    @Test
    public void fromVariantFallsBackToTaskWhenVariantFieldsNull()
    {
        // ADR-0012.3: an UNKNOWN-weakness variant (null weakness/defence) inherits the task default.
        MonsterVariant v = new MonsterVariant();
        v.setName("Vampyre");
        // no weakness, no defence, no flags

        MonsterProfile p = MonsterProfile.fromVariant(task(), v);
        assertEquals("inherits task style", CombatStyle.MELEE, p.getWeakness().getStyle());
        assertEquals("inherits task defence", 70, p.getDefence().getDefenceLevel());
    }

    @Test
    public void categoryFlagsFollowTheVariantNotTheTask()
    {
        // FR-5: a non-demon sibling under a demon task does NOT credit demonbane.
        MonsterVariant nonDemon = new MonsterVariant();
        nonDemon.setName("Tortured gorilla");
        nonDemon.setWeakness(new Weakness(CombatStyle.MELEE, null));
        nonDemon.setMonsterDefence(new MonsterDefence(80, 1, 1, 1, 0, 0));
        // demon flag left false even though the task is a demon task

        MonsterProfile p = MonsterProfile.fromVariant(task(), nonDemon);
        assertFalse("non-demon variant under a demon task is not a demon", p.isDemon());
    }

    @Test
    public void bonusContextIsBuiltFromTheProfile()
    {
        // BonusContext.from(profile) carries the profile's flags (FR-5). A demon variant -> demon=true.
        MonsterVariant demonV = new MonsterVariant();
        demonV.setWeakness(new Weakness(CombatStyle.MELEE, null));
        demonV.setMonsterDefence(new MonsterDefence(80, 1, 1, 1, 0, 0));
        demonV.setDemon(true);
        BonusContext demonCtx = BonusContext.from(MonsterProfile.fromVariant(task(), demonV), false);
        assertTrue(demonCtx.isDemon());
        assertTrue("slayerHelmApplies from the task", demonCtx.isSlayerHelmApplies());

        MonsterVariant plainV = new MonsterVariant();
        plainV.setWeakness(new Weakness(CombatStyle.MELEE, null));
        plainV.setMonsterDefence(new MonsterDefence(80, 1, 1, 1, 0, 0));
        BonusContext plainCtx = BonusContext.from(MonsterProfile.fromVariant(task(), plainV), true);
        assertFalse("non-demon sibling -> demon=false", plainCtx.isDemon());
        assertTrue("wilderness threaded in separately", plainCtx.isWilderness());
    }

    @Test
    public void offenceOverlaysFromTheVariant()
    {
        // WD-2 (ADR-0020 #1): the variant's own offence drives the profile.
        MonsterVariant v = new MonsterVariant();
        v.setWeakness(new Weakness(CombatStyle.MELEE, null));
        v.setMonsterDefence(new MonsterDefence(80, 1, 1, 1, 0, 0));
        v.setOffence(new MonsterOffence(255, 31,
            Collections.singletonList(AttackStyle.MAGIC), 4, 200, false, true));

        MonsterProfile p = MonsterProfile.fromVariant(task(), v);
        assertEquals(Integer.valueOf(255), p.getOffence().getHitpoints());
        assertEquals(AttackStyle.MAGIC, p.getOffence().getAttackStyles().get(0));
        assertTrue(p.getOffence().isVenomous());
    }

    @Test
    public void offenceFallsBackToTheTaskWhenVariantHasNone()
    {
        // A variant with no offence inherits the task-level offence default (object-level fallback,
        // the weakness/defence idiom).
        TaskData t = task();
        t.setOffence(new MonsterOffence(120, 12,
            Arrays.asList(AttackStyle.MELEE, AttackStyle.RANGED), 5, null, true, false));
        MonsterVariant v = new MonsterVariant();
        v.setWeakness(new Weakness(CombatStyle.MELEE, null));

        MonsterProfile p = MonsterProfile.fromVariant(t, v);
        assertEquals("inherits task offence", Integer.valueOf(120), p.getOffence().getHitpoints());
        assertEquals(2, p.getOffence().getAttackStyles().size());
        assertTrue(p.getOffence().isPoisonous());
    }

    @Test
    public void offenceIsNullWhenAbsentEverywhere()
    {
        // FR-6: no offence on task or variant -> null -> no advisory.
        assertNull(MonsterProfile.fromTask(task()).getOffence());
        MonsterVariant v = new MonsterVariant();
        v.setWeakness(new Weakness(CombatStyle.MELEE, null));
        assertNull(MonsterProfile.fromVariant(task(), v).getOffence());
    }

    @Test
    public void fromTaskNullSafe()
    {
        MonsterProfile p = MonsterProfile.fromTask(null);
        assertNull(p.getWeakness());
        assertNull(p.getDefence());
        assertFalse(p.isDemon());
    }
}

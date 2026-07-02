package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MV-B7/B8: the pure variant/method selection helpers. The surrounding {@code recompute} wiring is
 * field-injected and client-thread bound (manual checklist, plan §9); these lock the cheaply-testable
 * pieces - stale-variant validation, the boss live-pre-selection resolver, and method parsing.
 */
public class AllInSlayerPluginVariantTest
{
    private static MonsterVariant variant(String name, boolean boss, Integer bossId)
    {
        MonsterVariant v = new MonsterVariant();
        v.setName(name);
        v.setBoss(boss);
        v.setBossId(bossId);
        return v;
    }

    private static TaskData taskWith(List<MonsterVariant> variants)
    {
        TaskData t = new TaskData();
        t.setTask("T");
        t.setVariants(variants);
        return t;
    }

    @Test
    public void hasVariantValidatesAgainstTheTaskVariants()
    {
        TaskData t = taskWith(Arrays.asList(variant("A", false, null), variant("B", false, null)));
        assertTrue("null selection is always valid (-> default)", AllInSlayerPlugin.hasVariant(t, null));
        assertTrue(AllInSlayerPlugin.hasVariant(t, "A"));
        assertFalse("a stale name not in this task is invalid", AllInSlayerPlugin.hasVariant(t, "Zzz"));
        assertTrue("no-variants task: only null is valid",
            AllInSlayerPlugin.hasVariant(taskWith(null), null));
        assertFalse(AllInSlayerPlugin.hasVariant(taskWith(null), "A"));
    }

    @Test
    public void resolveBossVariantNameSeedsFromVarbitWhenNoOverride()
    {
        TaskData boss = taskWith(new ArrayList<>(Arrays.asList(
            variant("Default boss", true, null),
            variant("K'ril Tsutsaroth", true, 7),
            variant("Vorkath", true, 12))));

        assertEquals("varbit 7 seeds K'ril", "K'ril Tsutsaroth",
            AllInSlayerPlugin.resolveBossVariantName(boss, 7, null));
        assertEquals("varbit 12 seeds Vorkath", "Vorkath",
            AllInSlayerPlugin.resolveBossVariantName(boss, 12, null));
    }

    @Test
    public void userOverrideAlwaysWinsOverTheVarbit()
    {
        TaskData boss = taskWith(Arrays.asList(variant("A", true, 7), variant("B", true, 9)));
        assertEquals("explicit selection is kept regardless of the varbit", "B",
            AllInSlayerPlugin.resolveBossVariantName(boss, 7, "B"));
    }

    @Test
    public void unmappedVarbitFallsThroughToDeterministicDefault()
    {
        TaskData boss = taskWith(Arrays.asList(variant("A", true, 7), variant("B", true, 9)));
        assertNull("an unmapped varbit value seeds nothing (-> default)",
            AllInSlayerPlugin.resolveBossVariantName(boss, 999, null));
        assertNull("no varbit seeds nothing", AllInSlayerPlugin.resolveBossVariantName(boss, -1, null));
    }

    @Test
    public void nonBossTaskIsNeverSeededFromTheBossVarbit()
    {
        TaskData normal = taskWith(Arrays.asList(variant("A", false, null), variant("Boss form", true, 7)));
        assertNull("a task that is not all-boss is never auto-seeded",
            AllInSlayerPlugin.resolveBossVariantName(normal, 7, null));
        assertFalse(AllInSlayerPlugin.isBossTask(normal));
        assertTrue(AllInSlayerPlugin.isBossTask(
            taskWith(Arrays.asList(variant("A", true, null), variant("B", true, null)))));
    }

    @Test
    public void parseMethodMapsLabelsToCombatStyle()
    {
        assertEquals(CombatStyle.MELEE, AllInSlayerPlugin.parseMethod("Melee"));
        assertEquals(CombatStyle.RANGED, AllInSlayerPlugin.parseMethod("RANGED"));
        assertEquals(CombatStyle.MAGIC, AllInSlayerPlugin.parseMethod("magic"));
        assertNull(AllInSlayerPlugin.parseMethod(null));
        assertNull(AllInSlayerPlugin.parseMethod(""));
        assertNull("an unknown label is null (no method override)", AllInSlayerPlugin.parseMethod("Bogus"));
    }
}

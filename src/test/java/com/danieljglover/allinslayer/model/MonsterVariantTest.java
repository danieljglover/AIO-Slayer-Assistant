package com.danieljglover.allinslayer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * Pins the ONE shared variant-resolution rule (MV-FX, code-review S1). Both {@code LoadoutAdvisor}
 * (which drives the loadout) and {@code SlayerPanel} (which pre-selects the variant) delegate to
 * {@link MonsterVariant#resolve}, so this contract is the single source of truth for the FR-6 /
 * default-variant coupling. A drift in either caller is caught by the coupling tests in
 * LoadoutAdvisorTest + SlayerPanelTest, which assert each path resolves to exactly this oracle.
 */
public class MonsterVariantTest
{
    private static MonsterVariant variant(String name, boolean isDefault, boolean boss)
    {
        MonsterVariant v = new MonsterVariant();
        v.setName(name);
        v.setDefault(isDefault);
        v.setBoss(boss);
        return v;
    }

    @Test
    public void nameMatchWinsOverTheDefaultRule()
    {
        MonsterVariant def = variant("Aberrant spectre", true, false);
        MonsterVariant other = variant("Deviant spectre", false, false);
        TaskData task = new TaskData();
        task.setVariants(Arrays.asList(def, other));

        assertSame("an explicit name match resolves to that variant, not the default",
            other, MonsterVariant.resolve(task, "Deviant spectre"));
    }

    @Test
    public void noSelectionResolvesToTheDefaultVariant()
    {
        MonsterVariant first = variant("Deviant spectre", false, false);
        MonsterVariant def = variant("Aberrant spectre", true, false);
        TaskData task = new TaskData();
        task.setVariants(Arrays.asList(first, def));

        assertSame("no selection resolves to the isDefault variant (FR-6)",
            def, MonsterVariant.resolve(task, null));
    }

    @Test
    public void unknownSelectedNameFallsBackToTheDefaultRule()
    {
        MonsterVariant def = variant("Aberrant spectre", true, false);
        TaskData task = new TaskData();
        task.setVariants(Arrays.asList(variant("Deviant spectre", false, false), def));

        assertSame("a name with no match falls back to the default rule",
            def, MonsterVariant.resolve(task, "Does not exist"));
    }

    @Test
    public void allBossTaskWithNoDefaultResolvesToTheFirstVariant()
    {
        // The Boss meta-task (ADR-0014): every variant is a boss; none is isDefault -> first listed.
        MonsterVariant abyssalSire = variant("Abyssal Sire", false, true);
        MonsterVariant kril = variant("K'ril Tsutsaroth", false, true);
        TaskData task = new TaskData();
        task.setVariants(Arrays.asList(abyssalSire, kril));

        assertSame("an all-boss task with no isDefault resolves to the first variant",
            abyssalSire, MonsterVariant.resolve(task, null));
    }

    @Test
    public void noVariantsResolvesToNull()
    {
        TaskData empty = new TaskData();
        empty.setVariants(Collections.emptyList());
        assertNull("an empty variant list resolves to null (task default profile)",
            MonsterVariant.resolve(empty, null));

        TaskData none = new TaskData();
        assertNull("a null variant list resolves to null", MonsterVariant.resolve(none, "anything"));
    }

    @Test
    public void nullTaskResolvesToNull()
    {
        assertNull("a null task resolves to null (panel pre-render guard)",
            MonsterVariant.resolve(null, null));
    }

    @Test
    public void firstVariantIsTheFinalFallbackWhenNoNameAndNoDefault()
    {
        MonsterVariant a = variant("A", false, false);
        MonsterVariant b = variant("B", false, false);
        TaskData task = new TaskData();
        task.setVariants(Arrays.asList(a, b));
        assertEquals("no name + no default -> first listed", "A",
            MonsterVariant.resolve(task, null).getName());
    }

    // ---- WB-2 (D6): the ONE shared location-name match rule ------------------------------------

    @Test
    public void locationNameMatchIsTrimmedAndCaseInsensitive()
    {
        // The single match rule both the panel dropdown filter and the advisor candidate set
        // delegate to (WB-2 / D6): trimmed + case-insensitive, defensive vs display-string drift.
        org.junit.Assert.assertTrue(
            MonsterVariant.locationNameMatches(" slayer tower ", "Slayer Tower"));
        org.junit.Assert.assertTrue(
            MonsterVariant.locationNameMatches("Catacombs of Kourend", "Catacombs of Kourend"));
        org.junit.Assert.assertFalse(
            MonsterVariant.locationNameMatches("Slayer Tower", "Catacombs of Kourend"));
        org.junit.Assert.assertFalse("null never matches (no fabricated links)",
            MonsterVariant.locationNameMatches(null, "Slayer Tower"));
        org.junit.Assert.assertFalse(
            MonsterVariant.locationNameMatches("Slayer Tower", null));
    }
}

package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.Weakness;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * GAP-4 (data-audit.md): five assignable variants shipped with {@code weakness == null} and
 * {@code monsterDefence == null}, forcing the loadout engine to fall back to the task-level profile.
 * This pins the wiki-sourced backfill so the nulls cannot silently return.
 *
 * <p>Authoring convention (recorded in backend-engineer/MEMORY.md): weakness + the melee/magic/range
 * defensive-bonus block match the non-null sibling in the same family (ankou -> MELEE/air, range +15;
 * bloodveld -> MELEE/no-element, range +15); the {@code defenceLevel} is the per-variant value from
 * each monster's OSRS wiki Infobox Monster.</p>
 */
public class MonsterVariantProfileBackfillTest
{
    private static final List<TaskData> TASKS =
        ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer"));

    @Test
    public void darkAnkouCarriesTheAnkouFamilyProfile()
    {
        MonsterVariant darkAnkou = variantNamed("Ankou", "Dark Ankou");
        assertWeakness(darkAnkou.getWeakness(), CombatStyle.MELEE, "air");
        assertDefence(darkAnkou.getMonsterDefence(), 100);
    }

    @Test
    public void gwdBloodveldCarriesTheBloodveldFamilyProfile()
    {
        MonsterVariant v = variantNamed("Bloodveld", "Bloodveld (God Wars Dungeon)");
        assertWeakness(v.getWeakness(), CombatStyle.MELEE, null);
        assertDefence(v.getMonsterDefence(), 30);
    }

    @Test
    public void mutatedBloodveldCarriesTheBloodveldFamilyProfile()
    {
        MonsterVariant v = variantNamed("Bloodveld", "Mutated Bloodveld");
        assertWeakness(v.getWeakness(), CombatStyle.MELEE, null);
        assertDefence(v.getMonsterDefence(), 30);
    }

    @Test
    public void insatiableBloodveldCarriesTheBloodveldFamilyProfile()
    {
        MonsterVariant v = variantNamed("Bloodveld", "Insatiable Bloodveld");
        assertWeakness(v.getWeakness(), CombatStyle.MELEE, null);
        assertDefence(v.getMonsterDefence(), 85);
    }

    @Test
    public void insatiableMutatedBloodveldCarriesTheBloodveldFamilyProfile()
    {
        MonsterVariant v = variantNamed("Bloodveld", "Insatiable mutated Bloodveld");
        assertWeakness(v.getWeakness(), CombatStyle.MELEE, null);
        assertDefence(v.getMonsterDefence(), 130);
    }

    private static void assertWeakness(Weakness weakness, CombatStyle style, String element)
    {
        assertNotNull("weakness must be authored", weakness);
        assertEquals(style, weakness.getStyle());
        if (element == null)
        {
            assertNull("element must match sibling convention", weakness.getElement());
        }
        else
        {
            assertEquals(element, weakness.getElement());
        }
    }

    private static void assertDefence(MonsterDefence defence, int defenceLevel)
    {
        assertNotNull("monsterDefence must be authored", defence);
        assertEquals(defenceLevel, defence.getDefenceLevel());
        assertEquals(0, defence.getStab());
        assertEquals(0, defence.getSlash());
        assertEquals(0, defence.getCrush());
        assertEquals(0, defence.getMagic());
        assertEquals("ranged defence follows sibling convention", 15, defence.getRange());
    }

    private static MonsterVariant variantNamed(String taskName, String variantName)
    {
        return TASKS.stream()
            .filter(t -> taskName.equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing task: " + taskName))
            .getVariants().stream()
            .filter(v -> variantName.equals(v.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing variant: " + taskName + " / " + variantName));
    }
}

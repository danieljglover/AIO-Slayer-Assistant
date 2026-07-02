package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C3-D (ADR-0019): pins the wiki-sourced Cows starter family (Turael/Spria).
 * Numbers verified live 2026-07-02 against the Cow wiki page (pageId 12398) and the master
 * assignment tables (weight 8). slayerTargetId 283 is SYNTHETIC per the c2-c3-worklist
 * allocation contract. Turael has no requirement; Spria requires combat level 5.
 */
public class CowsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cows.json"), SourceTask.class);

        assertEquals(Integer.valueOf(12398), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(283, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("spria"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "cow-field");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("283")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Spria requires combat level 5")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo cow = task.getVariantInfo().stream()
            .filter(v -> "cow-lvl2".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(2), cow.getCombatLevel());
        assertEquals(8.0, cow.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison field = task.getLocationComparison().stream()
            .filter(l -> "cow-field".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertNull(field.getAmount());
        assertEquals(Boolean.FALSE, field.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant cow = read(
            Paths.get("src/main/data/slayer/monsters/cows/cow-lvl2.json"), SourceMonsterVariant.class);
        assertTrue(cow.getNpcIds().contains(Integer.valueOf(2790)));
        assertNotNull(cow.getOffence());
        assertEquals(Integer.valueOf(8), cow.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), cow.getOffence().getMaxHit());
        assertTrue(cow.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(cow.getOffence().getMagicLevel());
        assertEquals(-21, cow.getMonsterDefence().getCrush());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/cows/strategy.json");
        assertTrue("Cows strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("cows", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("cowhide")));
    }

    @Test
    public void generatedRuntimeDataCarriesCowsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Cows".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cows"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertEquals(1, task.getVariants().size());

        MonsterVariant deflt = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Cow", deflt.getName());
        assertNotNull(deflt.getStrategy());
        assertEquals(CombatStyle.MELEE, deflt.getStrategy().getPrimaryStyle());
        assertNotNull(deflt.getOffence());
        assertEquals(Integer.valueOf(8), deflt.getOffence().getHitpoints());
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("expected " + values + " to contain " + expected, values.contains(expected));
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

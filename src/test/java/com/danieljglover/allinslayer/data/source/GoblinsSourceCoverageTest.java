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
 * C3-D (ADR-0019): pins the wiki-sourced Goblins starter family (Turael/Spria).
 * Numbers verified live 2026-07-02 against the Goblin wiki page (pageId 11672) and the master
 * assignment tables (weight 7). slayerTargetId 291 is SYNTHETIC per the c2-c3-worklist
 * allocation contract. No combat, Slayer, or quest requirement to be assigned.
 */
public class GoblinsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/goblins.json"), SourceTask.class);

        assertEquals(Integer.valueOf(11672), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(291, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "goblin-spawns");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("291")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Sergeants")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo goblin = task.getVariantInfo().stream()
            .filter(v -> "goblin-lvl2".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(2), goblin.getCombatLevel());
        assertEquals(5.0, goblin.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison spawns = task.getLocationComparison().stream()
            .filter(l -> "goblin-spawns".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertNull(spawns.getAmount());
        assertEquals(Boolean.FALSE, spawns.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant goblin = read(
            Paths.get("src/main/data/slayer/monsters/goblins/goblin-lvl2.json"), SourceMonsterVariant.class);
        assertTrue(goblin.getNpcIds().contains(Integer.valueOf(3028)));
        assertNotNull(goblin.getOffence());
        assertEquals(Integer.valueOf(5), goblin.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), goblin.getOffence().getMaxHit());
        assertTrue(goblin.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(goblin.getOffence().getMagicLevel());
        assertEquals(-15, goblin.getMonsterDefence().getCrush());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/goblins/strategy.json");
        assertTrue("Goblins strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("goblins", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("no elemental weakness")));
    }

    @Test
    public void generatedRuntimeDataCarriesGoblinsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Goblins".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Goblins"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertEquals(1, task.getVariants().size());

        MonsterVariant deflt = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Goblin", deflt.getName());
        assertNotNull(deflt.getStrategy());
        assertEquals(CombatStyle.MELEE, deflt.getStrategy().getPrimaryStyle());
        assertNotNull(deflt.getOffence());
        assertEquals(Integer.valueOf(5), deflt.getOffence().getHitpoints());
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

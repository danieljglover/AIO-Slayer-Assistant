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
 * C3-D (ADR-0019): pins the wiki-sourced Birds starter family (Turael/Spria).
 * Numbers verified live 2026-07-02 against the Chicken wiki page and the Birds task page
 * (weight 6). slayerTargetId 277 is SYNTHETIC per the c2-c3-worklist allocation contract.
 * No combat, Slayer, or quest requirement to be assigned.
 */
public class BirdsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/birds.json"), SourceTask.class);

        assertEquals(Integer.valueOf(37232), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(277, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("spria"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "bird-spawns");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("277")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Sixteen bird types")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo chicken = task.getVariantInfo().stream()
            .filter(v -> "chicken-lvl1".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(1), chicken.getCombatLevel());
        assertEquals(3.0, chicken.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison spawns = task.getLocationComparison().stream()
            .filter(l -> "bird-spawns".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertNull(spawns.getAmount());
        assertEquals(Boolean.FALSE, spawns.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant chicken = read(
            Paths.get("src/main/data/slayer/monsters/birds/chicken-lvl1.json"), SourceMonsterVariant.class);
        assertTrue(chicken.getNpcIds().contains(Integer.valueOf(1173)));
        assertNotNull(chicken.getOffence());
        assertEquals(Integer.valueOf(3), chicken.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(0), chicken.getOffence().getMaxHit());
        assertTrue(chicken.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(chicken.getOffence().getMagicLevel());
        assertEquals(-42, chicken.getMonsterDefence().getStab());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/birds/strategy.json");
        assertTrue("Birds strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("birds", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("no elemental weakness")));
    }

    @Test
    public void generatedRuntimeDataCarriesBirdsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Birds".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Birds"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertEquals(1, task.getVariants().size());

        MonsterVariant deflt = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Chicken", deflt.getName());
        assertNotNull(deflt.getStrategy());
        assertEquals(CombatStyle.MELEE, deflt.getStrategy().getPrimaryStyle());
        assertNotNull(deflt.getOffence());
        assertEquals(Integer.valueOf(3), deflt.getOffence().getHitpoints());
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

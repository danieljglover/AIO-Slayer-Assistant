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
 * C3-D (ADR-0019): pins the wiki-sourced Bats starter family (Turael/Spria/Mazchna).
 * Numbers verified live 2026-07-02 against the Bat (pageId 12331) and Giant bat wiki pages
 * and the master assignment tables (weight 7). slayerTargetId 276 is SYNTHETIC per the
 * c2-c3-worklist allocation contract. Combat level 5 to assign; no Slayer/quest requirement.
 */
public class BatsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/bats.json"), SourceTask.class);

        assertEquals(Integer.valueOf(12331), task.getWikiPageId());
        assertEquals(Integer.valueOf(5), task.getCombatLevel());
        assertEquals(276, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(3, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());

        assertContains(locationIds(task), "bat-spawns");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("276")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("combat level 5")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("air spells")));

        assertEquals(2, task.getVariantInfo().size());
        SourceTaskVariantInfo giant = task.getVariantInfo().stream()
            .filter(v -> "giant-bat-lvl27".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(27), giant.getCombatLevel());
        assertEquals(32.0, giant.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison spawns = task.getLocationComparison().stream()
            .filter(l -> "bat-spawns".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertNull(spawns.getAmount());
        assertEquals(Boolean.FALSE, spawns.getCannonable());
    }

    @Test
    public void monsterVariantsCarryHonestOffence() throws IOException
    {
        SourceMonsterVariant giant = read(
            Paths.get("src/main/data/slayer/monsters/bats/giant-bat-lvl27.json"), SourceMonsterVariant.class);
        assertTrue(giant.getNpcIds().contains(Integer.valueOf(2834)));
        assertNotNull(giant.getOffence());
        assertEquals(Integer.valueOf(32), giant.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(3), giant.getOffence().getMaxHit());
        assertTrue(giant.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(giant.getOffence().getMagicLevel());
        assertEquals(22, giant.getMonsterDefence().getDefenceLevel());

        SourceMonsterVariant bat = read(
            Paths.get("src/main/data/slayer/monsters/bats/bat-lvl6.json"), SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(8), bat.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), bat.getOffence().getMaxHit());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/bats/strategy.json");
        assertTrue("Bats strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("bats", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("air spells")));
    }

    @Test
    public void generatedRuntimeDataCarriesBatsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Bats".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Bats"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(2, task.getVariants().size());

        MonsterVariant deflt = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Giant bat", deflt.getName());
        assertNotNull(deflt.getStrategy());
        assertEquals(CombatStyle.MELEE, deflt.getStrategy().getPrimaryStyle());
        assertNotNull(deflt.getOffence());
        assertEquals(Integer.valueOf(32), deflt.getOffence().getHitpoints());
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

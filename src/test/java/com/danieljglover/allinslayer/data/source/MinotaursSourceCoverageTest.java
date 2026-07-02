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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C3-F (ADR-0019 recipe): pins the wiki-authored Minotaur family. Turael/Spria starter-tier task, no
 * Slayer level required, free-to-play, on the first level (Vault of War) of the Stronghold of
 * Security. Stats from the live Minotaur wiki page (id 12310), fetched 2026-07-02. slayerTargetId is
 * SYNTHETIC (296). Location is a family-tuned copy of the stronghold-of-security base.
 */
public class MinotaursSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/minotaurs.json"), SourceTask.class);

        assertEquals(Integer.valueOf(12310), task.getWikiPageId());
        assertEquals(296, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("turael", "spria"), masterIds(task));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));

        assertNull("Minotaurs have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Minotaurs have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(-21, task.getMonsterDefence().getStab());
        assertEquals(Set.of("stronghold-of-security-minotaurs"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant minotaur = read(
            Paths.get("src/main/data/slayer/monsters/minotaur/minotaur.json"), SourceMonsterVariant.class);
        assertEquals("minotaur", minotaur.getVariantId());
        assertTrue(minotaur.getNpcIds().contains(2481));
        assertTrue(minotaur.getNpcIds().contains(2483));
        assertEquals(CombatStyle.MELEE, minotaur.getWeakness().getStyle());
        assertNull("no elemental weakness (honest null)", minotaur.getWeakness().getElement());
        assertEquals(10, minotaur.getMonsterDefence().getDefenceLevel());
        assertEquals(-21, minotaur.getMonsterDefence().getCrush());

        assertNotNull("minotaur offence authored", minotaur.getOffence());
        assertEquals(Integer.valueOf(10), minotaur.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(2), minotaur.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE), minotaur.getOffence().getAttackStyles());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/minotaurs/strategy.json");
        assertTrue("Minotaur strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("minotaurs", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void generatedRuntimeDataResolvesMinotaur() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Minotaur".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Minotaur"));

        assertEquals(296, task.getSlayerTargetId());
        assertEquals(Set.of("turael", "spria"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant minotaur = task.getVariants().stream()
            .filter(variant -> "Minotaur".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Minotaur variant"));
        assertNotNull(minotaur.getStrategy());
        assertNotNull("offence compiled onto runtime variant", minotaur.getOffence());
        assertEquals(Integer.valueOf(10), minotaur.getOffence().getHitpoints());
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

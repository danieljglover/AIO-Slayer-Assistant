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
 * C3-F (ADR-0019 recipe): pins the wiki-authored Hobgoblin family. Mazchna starter-tier task, no
 * Slayer level required, Edgeville Dungeon / Crafting Guild peninsula. Stats from the live Hobgoblin
 * wiki page (id 13076), fetched 2026-07-02. slayerTargetId is SYNTHETIC (292). Authored Mazchna-only
 * per the worklist; the wiki's additional Vannaka assignment is documented in taskNotes.
 */
public class HobgoblinsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/hobgoblins.json"), SourceTask.class);

        assertEquals(Integer.valueOf(13076), task.getWikiPageId());
        assertEquals(292, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Hobgoblins have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Hobgoblins have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("hobgoblin-area"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue("Vannaka scope note documented",
            task.getTaskNotes().stream().anyMatch(note -> note.contains("Vannaka")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant hobgoblin = read(
            Paths.get("src/main/data/slayer/monsters/hobgoblin/hobgoblin.json"), SourceMonsterVariant.class);
        assertEquals("hobgoblin", hobgoblin.getVariantId());
        assertTrue(hobgoblin.getNpcIds().contains(3049));
        assertEquals(CombatStyle.MELEE, hobgoblin.getWeakness().getStyle());
        assertNull("no elemental weakness (honest null)", hobgoblin.getWeakness().getElement());
        assertEquals(24, hobgoblin.getMonsterDefence().getDefenceLevel());

        assertNotNull("hobgoblin offence authored", hobgoblin.getOffence());
        assertEquals(Integer.valueOf(29), hobgoblin.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(3), hobgoblin.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE), hobgoblin.getOffence().getAttackStyles());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/hobgoblins/strategy.json");
        assertTrue("Hobgoblin strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("hobgoblins", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void generatedRuntimeDataResolvesHobgoblin() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Hobgoblin".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Hobgoblin"));

        assertEquals(292, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant hobgoblin = task.getVariants().stream()
            .filter(variant -> "Hobgoblin".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Hobgoblin variant"));
        assertNotNull(hobgoblin.getStrategy());
        assertNotNull("offence compiled onto runtime variant", hobgoblin.getOffence());
        assertEquals(Integer.valueOf(29), hobgoblin.getOffence().getHitpoints());
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

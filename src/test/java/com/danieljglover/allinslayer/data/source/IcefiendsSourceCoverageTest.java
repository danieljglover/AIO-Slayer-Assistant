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
 * C3-F (ADR-0019 recipe): pins the wiki-authored Icefiend family. Turael/Spria starter-tier task, no
 * Slayer level required, on Ice Mountain. Stats from the live Icefiend wiki page (id 12639), fetched
 * 2026-07-02. slayerTargetId is SYNTHETIC (293). 100% fire weakness -> MAGIC/fire; new
 * fire-battlestaff plugin weapon.
 */
public class IcefiendsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/icefiends.json"), SourceTask.class);

        assertEquals(Integer.valueOf(12639), task.getWikiPageId());
        assertEquals(293, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("turael", "spria"), masterIds(task));
        assertArrayEquals(new int[] {15, 20}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 20}, task.getAmountByMaster().get("spria"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("spria"));

        assertNull("Icefiends have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Icefiends have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());
        assertEquals(Set.of("ice-mountain"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant icefiend = read(
            Paths.get("src/main/data/slayer/monsters/icefiend/icefiend.json"), SourceMonsterVariant.class);
        assertEquals("icefiend", icefiend.getVariantId());
        assertTrue(icefiend.getNpcIds().contains(4813));
        assertEquals(CombatStyle.MAGIC, icefiend.getWeakness().getStyle());
        assertEquals("fire", icefiend.getWeakness().getElement());
        assertEquals(12, icefiend.getMonsterDefence().getDefenceLevel());

        assertNotNull("icefiend offence authored", icefiend.getOffence());
        assertEquals(Integer.valueOf(15), icefiend.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(2), icefiend.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MAGIC), icefiend.getOffence().getAttackStyles());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/icefiends/strategy.json");
        assertTrue("Icefiend strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("icefiends", strategy.getStrategyId());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("fire-battlestaff"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void generatedRuntimeDataResolvesIcefiend() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Icefiend".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Icefiend"));

        assertEquals(293, task.getSlayerTargetId());
        assertEquals(Set.of("turael", "spria"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant icefiend = task.getVariants().stream()
            .filter(variant -> "Icefiend".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Icefiend variant"));
        assertNotNull(icefiend.getStrategy());
        assertNotNull("offence compiled onto runtime variant", icefiend.getOffence());
        assertEquals(Integer.valueOf(15), icefiend.getOffence().getHitpoints());
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

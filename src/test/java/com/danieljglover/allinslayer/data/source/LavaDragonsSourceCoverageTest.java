package com.danieljglover.allinslayer.data.source;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C2-C (ADR-0019): Lava dragons (Krystilia, deep Wilderness) family coverage. Numbers verified
 * against the live wiki on 2026-07-02 (Lava_dragon monster page + the Krystilia task table).
 */
public class LavaDragonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lava-dragons.json"), SourceTask.class);

        assertNull(task.getWikiPageId());
        assertEquals(Integer.valueOf(252), task.getCombatLevel());
        assertEquals(263, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs() == null || task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {35, 60}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getUnlocks() == null || task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "lava-dragon-isle");
        assertEquals("lava-dragon", task.getDefaultVariantId());
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.isDragon());
        assertFalse(task.isKalphite());
        assertNull(task.getRequiredItemId());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Lava Dragon Isle")));
    }

    @Test
    public void monsterVariantMatchesWikiStats() throws IOException
    {
        SourceMonsterVariant dragon = read(
            Paths.get("src/main/data/slayer/monsters/lava-dragons/lava-dragon-lvl252.json"),
            SourceMonsterVariant.class);

        assertEquals("lava-dragon", dragon.getVariantId());
        assertEquals(Integer.valueOf(252), dragon.getCombatLevel());
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(6593)),
            new java.util.HashSet<>(dragon.getNpcIds()));
        assertEquals(220, dragon.getMonsterDefence().getDefenceLevel());
        assertEquals(90, dragon.getMonsterDefence().getSlash());
        assertEquals("water", dragon.getWeakness().getElement());
        assertTrue(dragon.isDragon());
        assertFalse(dragon.isBoss());
        assertEquals("lava-dragons", dragon.getStrategyId());
    }

    @Test
    public void locationIsDeepWildernessAndHonest() throws IOException
    {
        SourceLocation isle = read(
            Paths.get("src/main/data/slayer/locations/lava-dragon-isle.json"),
            SourceLocation.class);
        assertEquals("lava-dragon-isle", isle.getLocationId());
        assertTrue(isle.isWilderness());
        assertFalse(isle.isKonarLockable());
        assertFalse(isle.isCannon());
        assertTrue(isle.getAccessNote().contains("Wilderness"));
    }

    @Test
    public void strategySourceCarriesPluginWeaponsAndMethods() throws IOException
    {
        SourceStrategy strategy = read(
            Paths.get("src/main/data/slayer/strategies/lava-dragons/strategy.json"),
            SourceStrategy.class);

        assertEquals("lava-dragons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Lava_dragon", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("craw-s-bow"));
        assertContains(methodIds(strategy), "general");
    }

    @Test
    public void generatedRuntimeDataCarriesLavaDragons() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Lava dragons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Lava dragons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getLocations().stream().allMatch(location -> location.isWilderness()));

        MonsterVariant dragon = task.getVariants().stream()
            .filter(variant -> "Lava dragon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Lava dragon variant"));
        assertNotNull(dragon.getStrategy());
        assertEquals(CombatStyle.RANGED, dragon.getStrategy().getPrimaryStyle());
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("missing " + expected + " in " + values, values.contains(expected));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream().map(SourceStrategyMethod::getMethodId)
            .collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

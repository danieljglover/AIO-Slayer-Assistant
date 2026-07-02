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
import java.util.Map;
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
 * C2-C (ADR-0019): Green dragons (Krystilia, Wilderness) family coverage. Every number was
 * verified against the live wiki on 2026-07-02 (Green_dragon monster page + the Krystilia task
 * table, which was re-verified unchanged in c2-c3-worklist.md).
 */
public class GreenDragonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/green-dragons.json"), SourceTask.class);

        assertNull(task.getWikiPageId());
        assertEquals(Integer.valueOf(79), task.getCombatLevel());
        // Synthetic id 259 (C2/C3 block 252-305, worklist section 1.3): the real SLAYER_TARGET
        // varp for Green dragons is not wiki-published; see taskNotes.
        assertEquals(259, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs() == null || task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {65, 100}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getUnlocks() == null || task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "graveyard-of-shadows-green-dragons");
        assertContains(locationIds(task), "green-dragons-wilderness");
        assertEquals("green-dragon", task.getDefaultVariantId());
        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isUndead());
        assertFalse(task.isDemon());
        assertTrue(task.isDragon());
        assertFalse(task.isKalphite());
        assertNull(task.getRequiredItemId());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness-only")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonfire")));
    }

    @Test
    public void monsterVariantMatchesWikiStats() throws IOException
    {
        SourceMonsterVariant dragon = read(
            Paths.get("src/main/data/slayer/monsters/green-dragons/green-dragon-lvl79.json"),
            SourceMonsterVariant.class);

        assertEquals("green-dragon", dragon.getVariantId());
        assertEquals(Integer.valueOf(79), dragon.getCombatLevel());
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(260, 261, 262, 263, 264, 8073, 8076, 8082)),
            new java.util.HashSet<>(dragon.getNpcIds()));
        assertEquals(68, dragon.getMonsterDefence().getDefenceLevel());
        assertEquals(0, dragon.getMonsterDefence().getStab());
        assertEquals("water", dragon.getWeakness().getElement());
        assertTrue(dragon.isDragon());
        assertFalse(dragon.isBoss());
        assertEquals("green-dragons", dragon.getStrategyId());
    }

    @Test
    public void locationSourcesAreWildernessAndHonest() throws IOException
    {
        SourceLocation graveyard = read(
            Paths.get("src/main/data/slayer/locations/graveyard-of-shadows-green-dragons.json"),
            SourceLocation.class);
        assertEquals("graveyard-of-shadows-green-dragons", graveyard.getLocationId());
        assertTrue(graveyard.isWilderness());
        assertFalse(graveyard.isKonarLockable());
        assertFalse(graveyard.isCannon());

        SourceLocation wildy = read(
            Paths.get("src/main/data/slayer/locations/green-dragons-wilderness.json"),
            SourceLocation.class);
        assertTrue(wildy.isWilderness());
        assertFalse(wildy.isKonarLockable());
    }

    @Test
    public void strategySourceCarriesPluginWeaponsAndMethods() throws IOException
    {
        SourceStrategy strategy = read(
            Paths.get("src/main/data/slayer/strategies/green-dragons/strategy.json"),
            SourceStrategy.class);

        assertEquals("green-dragons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Green_dragon", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("viggora-s-chainmace"));
        assertContains(methodIds(strategy), "general");
    }

    @Test
    public void generatedRuntimeDataCarriesGreenDragons() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Green dragons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Green dragons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getLocations().stream().allMatch(location -> location.isWilderness()));

        MonsterVariant dragon = task.getVariants().stream()
            .filter(variant -> "Green dragon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Green dragon variant"));
        assertNotNull(dragon.getStrategy());
        assertEquals(CombatStyle.MELEE, dragon.getStrategy().getPrimaryStyle());
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

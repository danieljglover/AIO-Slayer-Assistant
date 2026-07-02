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
 * C2-C (ADR-0019): Mammoths (Krystilia, Wilderness) family coverage. Numbers verified against the
 * live wiki on 2026-07-02 (Mammoth monster page + the Krystilia task table). Mammoths have no
 * elemental weakness (honest null element).
 */
public class MammothsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/mammoths.json"), SourceTask.class);

        assertNull(task.getWikiPageId());
        assertEquals(Integer.valueOf(80), task.getCombatLevel());
        assertEquals(265, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getUnlocks() == null || task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "mammoth-wilderness");
        assertEquals("mammoth", task.getDefaultVariantId());
        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isDragon());
        // No elemental weakness on the wiki -> honest null element.
        assertNull(task.getWeakness().getElement());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ferox")));
    }

    @Test
    public void monsterVariantMatchesWikiStats() throws IOException
    {
        SourceMonsterVariant mammoth = read(
            Paths.get("src/main/data/slayer/monsters/mammoths/mammoth-lvl80.json"),
            SourceMonsterVariant.class);

        assertEquals("mammoth", mammoth.getVariantId());
        assertEquals(Integer.valueOf(80), mammoth.getCombatLevel());
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(6604)),
            new java.util.HashSet<>(mammoth.getNpcIds()));
        assertEquals(50, mammoth.getMonsterDefence().getDefenceLevel());
        assertEquals(0, mammoth.getMonsterDefence().getStab());
        assertNull(mammoth.getWeakness().getElement());
        assertFalse(mammoth.isBoss());
        assertEquals("mammoths", mammoth.getStrategyId());
    }

    @Test
    public void locationIsWildernessAndHonest() throws IOException
    {
        SourceLocation loc = read(
            Paths.get("src/main/data/slayer/locations/mammoth-wilderness.json"),
            SourceLocation.class);
        assertEquals("mammoth-wilderness", loc.getLocationId());
        assertTrue(loc.isWilderness());
        assertFalse(loc.isKonarLockable());
        assertFalse(loc.isCannon());
        assertTrue(loc.getAccessNote().contains("Ferox"));
    }

    @Test
    public void strategySourceCarriesPluginWeaponsAndMethods() throws IOException
    {
        SourceStrategy strategy = read(
            Paths.get("src/main/data/slayer/strategies/mammoths/strategy.json"),
            SourceStrategy.class);
        assertEquals("mammoths", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("viggora-s-chainmace"));
        assertContains(methodIds(strategy), "general");
    }

    @Test
    public void generatedRuntimeDataCarriesMammoths() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Mammoths".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Mammoths"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getLocations().stream().allMatch(location -> location.isWilderness()));

        MonsterVariant mammoth = task.getVariants().stream()
            .filter(variant -> "Mammoth".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Mammoth variant"));
        assertNotNull(mammoth.getStrategy());
        assertEquals(CombatStyle.MELEE, mammoth.getStrategy().getPrimaryStyle());
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

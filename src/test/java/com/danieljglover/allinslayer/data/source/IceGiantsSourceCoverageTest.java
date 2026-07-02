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
 * C2-C (ADR-0019): Ice giants (Krystilia, Wilderness) family coverage. Numbers verified against
 * the live wiki on 2026-07-02 (Ice_giant monster page + the Krystilia task table). This family
 * owns the shared location file frozen-waste-plateau (CT-L ownership doc; owner id 261).
 */
public class IceGiantsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ice-giants.json"), SourceTask.class);

        assertNull(task.getWikiPageId());
        assertEquals(Integer.valueOf(53), task.getCombatLevel());
        assertEquals(261, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {100, 150}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getUnlocks() == null || task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "frozen-waste-plateau");
        assertContains(locationIds(task), "wilderness-slayer-cave-ice-giants");
        assertEquals("ice-giant", task.getDefaultVariantId());
        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isDragon());
        assertEquals("fire", task.getWeakness().getElement());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fire")));
    }

    @Test
    public void bothWildernessVariantsMatchWikiStats() throws IOException
    {
        SourceMonsterVariant standard = read(
            Paths.get("src/main/data/slayer/monsters/ice-giants/ice-giant-lvl53.json"),
            SourceMonsterVariant.class);
        assertEquals("ice-giant", standard.getVariantId());
        assertEquals(Integer.valueOf(53), standard.getCombatLevel());
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(2085, 2086, 2087, 2088, 2089, 13796, 13797)),
            new java.util.HashSet<>(standard.getNpcIds()));
        assertEquals(40, standard.getMonsterDefence().getDefenceLevel());
        assertEquals("fire", standard.getWeakness().getElement());
        assertEquals("ice-giants", standard.getStrategyId());

        SourceMonsterVariant cave = read(
            Paths.get("src/main/data/slayer/monsters/ice-giants/ice-giant-wilderness-cave-lvl67.json"),
            SourceMonsterVariant.class);
        assertEquals("ice-giant-wilderness-cave", cave.getVariantId());
        assertEquals(Integer.valueOf(67), cave.getCombatLevel());
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(7878, 7879, 7880)),
            new java.util.HashSet<>(cave.getNpcIds()));
    }

    @Test
    public void locationsAreWildernessAndFrozenWastePlateauIsOwned() throws IOException
    {
        SourceLocation plateau = read(
            Paths.get("src/main/data/slayer/locations/frozen-waste-plateau.json"),
            SourceLocation.class);
        assertEquals("frozen-waste-plateau", plateau.getLocationId());
        assertEquals("Frozen Waste Plateau", plateau.getName());
        assertTrue(plateau.isWilderness());
        assertFalse(plateau.isKonarLockable());

        SourceLocation cave = read(
            Paths.get("src/main/data/slayer/locations/wilderness-slayer-cave-ice-giants.json"),
            SourceLocation.class);
        assertTrue(cave.isWilderness());
        assertTrue(cave.isMulti());
        assertTrue(cave.isCannon());
    }

    @Test
    public void strategySourceCarriesPluginWeaponsAndMethods() throws IOException
    {
        SourceStrategy strategy = read(
            Paths.get("src/main/data/slayer/strategies/ice-giants/strategy.json"),
            SourceStrategy.class);
        assertEquals("ice-giants", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("viggora-s-chainmace"));
        assertContains(methodIds(strategy), "general");
    }

    @Test
    public void generatedRuntimeDataCarriesIceGiants() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Ice giants".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ice giants"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().allMatch(location -> location.isWilderness()));
        assertEquals(2, task.getVariants().size());

        MonsterVariant giant = task.getVariants().stream()
            .filter(variant -> variant.getCombatLevel() == 53)
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing level 53 ice giant"));
        assertNotNull(giant.getStrategy());
        assertEquals(CombatStyle.MELEE, giant.getStrategy().getPrimaryStyle());
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

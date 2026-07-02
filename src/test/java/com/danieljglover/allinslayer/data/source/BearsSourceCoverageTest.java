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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C2-D (ADR-0019 recipe): pins the wiki-authored Bears family sources. Bears is a SHARED family -
 * authored once with the union of Krystilia (Wilderness) plus the Turael/Spria/Mazchna starter
 * masters, and with both Wilderness and non-Wilderness variant/location sets (worklist §5.4).
 * Masters, quantities, and weights are from the live Bears Slayer-task / master pages, fetched
 * 2026-07-02. slayerTargetId is SYNTHETIC (253) per the C2/C3 allocation contract (worklist §1).
 */
public class BearsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/bears.json"), SourceTask.class);

        assertEquals(253, task.getSlayerTargetId());
        assertEquals(Integer.valueOf(13), task.getCombatLevel());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("krystilia", "turael", "spria", "mazchna"), masterIds(task));
        assertArrayEquals(new int[] {65, 100}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));

        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("mazchna"));

        assertNull("Bears have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Bears have no superior or extension unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("bears-wilderness", "bears-area"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("do NOT count") || note.contains("Angry bears")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void taskSourceCarriesBothWildernessAndStarterVariants() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/bears.json"), SourceTask.class);

        Set<String> variantIds = task.getVariantInfo().stream()
            .map(SourceTaskVariantInfo::getVariantId)
            .collect(Collectors.toSet());
        assertEquals(Set.of("grizzly-bear", "grizzly-bear-cub", "black-bear"), variantIds);

        Set<String> comparisonLocs = task.getLocationComparison().stream()
            .map(SourceTaskLocationComparison::getLocationId)
            .collect(Collectors.toSet());
        assertEquals(Set.of("bears-wilderness", "bears-area"), comparisonLocs);
    }

    @Test
    public void monsterSourcesCarryWikiStats() throws IOException
    {
        SourceMonsterVariant grizzly = read(
            Paths.get("src/main/data/slayer/monsters/bears/grizzly-bear.json"), SourceMonsterVariant.class);
        assertEquals("grizzly-bear", grizzly.getVariantId());
        assertTrue(grizzly.getNpcIds().contains(2838));
        assertEquals(CombatStyle.MELEE, grizzly.getWeakness().getStyle());
        assertEquals(15, grizzly.getMonsterDefence().getDefenceLevel());

        SourceMonsterVariant blackBear = read(
            Paths.get("src/main/data/slayer/monsters/bears/black-bear.json"), SourceMonsterVariant.class);
        assertEquals(Set.of(2839), blackBear.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(13, blackBear.getMonsterDefence().getDefenceLevel());
    }

    @Test
    public void strategySourceResolvesMeleeWeapon() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/bears/strategy.json");
        assertTrue("Bears strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("bears", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));

        SourceWeapon scimitar = read(
            Paths.get("src/main/data/slayer/weapons/dragon-scimitar.json"), SourceWeapon.class);
        assertEquals("dragon-scimitar", scimitar.getWeaponId());
        assertEquals(Set.of(4587), scimitar.getItemIds().stream().collect(Collectors.toSet()));
    }

    @Test
    public void ownedLocationSourcesMatchWiki() throws IOException
    {
        SourceLocation wildy = read(
            Paths.get("src/main/data/slayer/locations/bears-wilderness.json"), SourceLocation.class);
        assertEquals("bears-wilderness", wildy.getLocationId());
        assertEquals(true, wildy.isWilderness());
        assertEquals(false, wildy.isKonarLockable());

        SourceLocation area = read(
            Paths.get("src/main/data/slayer/locations/bears-area.json"), SourceLocation.class);
        assertEquals("bears-area", area.getLocationId());
        assertEquals(false, area.isWilderness());
        assertEquals(false, area.isKonarLockable());
    }

    @Test
    public void generatedRuntimeDataCarriesBearsSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Bears".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Bears"));

        assertEquals(253, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia", "turael", "spria", "mazchna"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Wilderness bears".equals(location.getName()) && location.isWilderness()));

        MonsterVariant grizzly = task.getVariants().stream()
            .filter(variant -> "Grizzly bear".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Grizzly bear variant"));
        assertNotNull(grizzly.getStrategy());
        assertEquals(CombatStyle.MELEE, grizzly.getStrategy().getPrimaryStyle());
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

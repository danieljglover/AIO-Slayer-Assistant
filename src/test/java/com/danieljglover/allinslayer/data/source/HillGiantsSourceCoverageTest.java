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
 * C2-D (ADR-0019 recipe): pins the wiki-authored Hill giants family sources. Hill giants are a
 * SHARED family: assigned by Krystilia (Wilderness only, 75-125, weight 3) and Mazchna (30-50,
 * weight 7). Authored once with the union of master sets and both Wilderness (Deep Wilderness
 * Dungeon) and non-Wilderness (Edgeville Dungeon) locations. Numbers verified against the live
 * Hill giant / Krystilia / Mazchna wiki pages on 2026-07-02. slayerTargetId 260 is SYNTHETIC per
 * the C2/C3 allocation contract (worklist §1).
 */
public class HillGiantsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/hill-giants.json"), SourceTask.class);

        assertEquals(Integer.valueOf(28), task.getCombatLevel());
        assertEquals(260, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(Set.of("krystilia", "mazchna"), masterIds(task));
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));

        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Hill giants have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Hill giants have no superior or extension unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("hill-giants-area", "hill-giants-wilderness"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());
        assertEquals(26, task.getMonsterDefence().getDefenceLevel());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Mazchna")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void taskSourceContainsBothLocationComparisons() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/hill-giants.json"), SourceTask.class);

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo hillGiant = task.getVariantInfo().get(0);
        assertEquals("hill-giant", hillGiant.getVariantId());
        assertEquals(35.0, hillGiant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(2, task.getLocationComparison().size());
        Set<String> compared = task.getLocationComparison().stream()
            .map(SourceTaskLocationComparison::getLocationId)
            .collect(Collectors.toSet());
        assertEquals(Set.of("hill-giants-area", "hill-giants-wilderness"), compared);
    }

    @Test
    public void monsterSourceCarriesWikiStats() throws IOException
    {
        SourceMonsterVariant hillGiant = read(
            Paths.get("src/main/data/slayer/monsters/hill-giants/hill-giant-lvl28.json"), SourceMonsterVariant.class);
        assertEquals("hill-giant", hillGiant.getVariantId());
        assertEquals(Set.of(2098, 13502), hillGiant.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(28), hillGiant.getCombatLevel());
        assertEquals(CombatStyle.MELEE, hillGiant.getWeakness().getStyle());
        assertEquals("earth", hillGiant.getWeakness().getElement());
        assertEquals(26, hillGiant.getMonsterDefence().getDefenceLevel());
        assertEquals("hill-giants", hillGiant.getStrategyId());
    }

    @Test
    public void strategySourceCarriesMeleeGear() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/hill-giants/strategy.json");
        assertTrue("Hill giants strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("hill-giants", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Hill_giant", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));

        SourceWeapon scimitar = read(
            Paths.get("src/main/data/slayer/weapons/dragon-scimitar.json"), SourceWeapon.class);
        assertEquals("dragon-scimitar", scimitar.getWeaponId());
    }

    @Test
    public void generatedRuntimeDataCarriesHillGiantsSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Hill giants".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Hill giants"));

        assertEquals(260, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia", "mazchna"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Deep Wilderness Dungeon".equals(location.getName()) && location.isWilderness()));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Edgeville Dungeon".equals(location.getName()) && !location.isWilderness()));

        MonsterVariant hillGiant = task.getVariants().stream()
            .filter(variant -> "Hill giant".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Hill giant variant"));
        assertNotNull(hillGiant.getStrategy());
        assertEquals(CombatStyle.MELEE, hillGiant.getStrategy().getPrimaryStyle());
    }

    @Test
    public void ownedLocationSourcesMatchWiki() throws IOException
    {
        SourceLocation wildy = read(
            Paths.get("src/main/data/slayer/locations/hill-giants-wilderness.json"), SourceLocation.class);
        assertEquals("hill-giants-wilderness", wildy.getLocationId());
        assertEquals(true, wildy.isWilderness());
        assertEquals(false, wildy.isKonarLockable());

        SourceLocation area = read(
            Paths.get("src/main/data/slayer/locations/hill-giants-area.json"), SourceLocation.class);
        assertEquals("hill-giants-area", area.getLocationId());
        assertEquals(false, area.isWilderness());
        assertEquals(false, area.isKonarLockable());
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

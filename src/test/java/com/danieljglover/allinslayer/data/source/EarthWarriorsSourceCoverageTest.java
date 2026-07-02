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
 * C2-A (ADR-0019): pins the wiki-sourced Earth warriors family (north-eastern Edgeville Dungeon,
 * Wilderness). Stats re-verified live 2026-07-02 against the Earth warrior wiki page
 * (wikiPageId 13043). Krystilia-only; quantity 75-125, weight 6. slayerTargetId 257 SYNTHETIC.
 * Single-combat area with a wiki-documented safespot; cannon is usable (not immune).
 */
public class EarthWarriorsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/earth-warriors.json"), SourceTask.class);

        assertEquals(Integer.valueOf(13043), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(257, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "earth-warriors-wilderness");

        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());
        assertEquals(42, task.getMonsterDefence().getDefenceLevel());
        assertEquals(20, task.getMonsterDefence().getCrush());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("257")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Agility")));

        assertEquals(1, task.getVariantInfo().size());
        assertEquals("earth-warrior-lvl51", task.getVariantInfo().get(0).getVariantId());
    }

    @Test
    public void strategySourceCarriesCrushPluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/earth-warriors/strategy.json");
        assertTrue("Earth warriors strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("earth-warriors", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Earth_warrior", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getNote().contains("crush"));
    }

    @Test
    public void generatedRuntimeDataCarriesEarthWarriorsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Earth warriors".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Earth warriors"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertEquals(1, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(
            location -> location.isWilderness() && location.isSafeSpot() && location.isCannon()));

        MonsterVariant def = task.getVariants().get(0);
        assertTrue(def.isDefault());
        assertNotNull(def.getStrategy());
        assertEquals(CombatStyle.MELEE, def.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/earth-warriors-wilderness.json"),
            SourceLocation.class);

        assertEquals("earth-warriors-wilderness", location.getLocationId());
        assertTrue(location.isWilderness());
        assertFalse("single-combat area", location.isMulti());
        assertTrue("cannon is usable (not immune)", location.isCannon());
        assertTrue("wiki-documented safespot in the first chamber", location.isSafeSpot());
        assertFalse(location.isKonarLockable());
        assertTrue(location.getAccessNote().contains("Agility"));
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

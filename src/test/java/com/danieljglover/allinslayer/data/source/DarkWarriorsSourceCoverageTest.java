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
 * C2-A (ADR-0019): pins the wiki-sourced Dark warriors family (Dark Warriors' Fortress,
 * Wilderness). Stats re-verified live 2026-07-02 against the Dark warrior wiki page
 * (wikiPageId 15856). Krystilia-only; quantity 75-125, weight 4. slayerTargetId 256 SYNTHETIC.
 * The level 8 fortress variant has huge melee defence (stab +96) but zero magic/ranged defence.
 */
public class DarkWarriorsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dark-warriors.json"), SourceTask.class);

        assertEquals(Integer.valueOf(15856), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(256, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "dark-warriors-fortress");

        assertFalse(task.isUndead());
        assertEquals(96, task.getMonsterDefence().getStab());
        assertEquals(0, task.getMonsterDefence().getMagic());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("256")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Fortress")));

        assertEquals(1, task.getVariantInfo().size());
        assertEquals("dark-warrior-lvl8", task.getVariantInfo().get(0).getVariantId());
    }

    @Test
    public void strategySourceCarriesPluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/dark-warriors/strategy.json");
        assertTrue("Dark warriors strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("dark-warriors", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Dark_warrior", strategy.getSourceUrl());
        assertNotNull(strategy.getPlugin().getPrimaryStyle());
        assertFalse(strategy.getPlugin().getPrimaryWeapons().isEmpty());
    }

    @Test
    public void generatedRuntimeDataCarriesDarkWarriorsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Dark warriors".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Dark warriors"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));
        assertEquals(1, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> location.isWilderness()));

        MonsterVariant def = task.getVariants().get(0);
        assertTrue(def.isDefault());
        assertNotNull(def.getStrategy());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/dark-warriors-fortress.json"),
            SourceLocation.class);

        assertEquals("dark-warriors-fortress", location.getLocationId());
        assertEquals("Dark Warriors' Fortress", location.getName());
        assertTrue(location.isWilderness());
        assertFalse(location.isKonarLockable());
        assertNotNull(location.getAccessNote());
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

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
 * C2-B (ADR-0019): pins the wiki-sourced Rogues family (Krystilia Wilderness). Numbers sourced
 * 2026-07-02 from https://oldschool.runescape.wiki/w/Rogue and the Krystilia table. slayerTargetId
 * 269 is SYNTHETIC. Rogues' Castle in the deep Wilderness; the level 15 Rogue (npc 526) is authored,
 * the level 135 variant stats are honest UNKNOWN.
 */
public class RoguesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/rogues.json"), SourceTask.class);

        assertNull("wikiPageId honest UNKNOWN", task.getWikiPageId());
        assertEquals(Integer.valueOf(15), task.getCombatLevel());
        assertEquals(269, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "rogues-castle");

        assertFalse(task.isUndead());
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());
        assertEquals(13, task.getMonsterDefence().getDefenceLevel());
        assertEquals(0, task.getMonsterDefence().getRange());
        assertEquals(0, task.getMonsterDefence().getMagic());
        assertNull(task.getRequiredItemId());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("269")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Chaos Elemental")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("UNKNOWN")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo variant = task.getVariantInfo().get(0);
        assertEquals("rogue", variant.getVariantId());
        assertEquals(Integer.valueOf(15), variant.getCombatLevel());
        assertEquals(17.0, variant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        assertEquals("rogues-castle", task.getLocationComparison().get(0).getLocationId());
    }

    @Test
    public void strategySourceCarriesRangedPluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/rogues/strategy.json");
        assertTrue("Rogues strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("rogues", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Rogue", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("toxic-blowpipe"));
    }

    @Test
    public void generatedRuntimeDataCarriesRoguesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Rogues".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Rogues"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(location -> location.isWilderness()));

        assertEquals(1, task.getVariants().size());
        MonsterVariant rogue = task.getVariants().get(0);
        assertEquals("Rogue", rogue.getName());
        assertTrue(rogue.isDefault());
        assertNotNull(rogue.getStrategy());
        assertEquals(CombatStyle.RANGED, rogue.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/rogues-castle.json"),
            SourceLocation.class);

        assertEquals("rogues-castle", location.getLocationId());
        assertTrue(location.isWilderness());
        assertFalse(location.isKonarLockable());
        assertFalse(location.isSafeSpot());
        assertTrue(location.getAccessNote().contains("Wilderness"));
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

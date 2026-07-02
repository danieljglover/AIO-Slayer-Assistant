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
 * C2-B (ADR-0019): pins the wiki-sourced Chaos druids family (Krystilia Wilderness).
 * Numbers sourced 2026-07-02 from https://oldschool.runescape.wiki/w/Chaos_druid and the
 * Krystilia table (worklist c2-c3-worklist.md). slayerTargetId 255 is SYNTHETIC per the
 * C2/C3 allocation contract. Only the Edgeville Dungeon (Wilderness) spawns count for Krystilia.
 */
public class ChaosDruidsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/chaos-druids.json"), SourceTask.class);

        assertNull("wikiPageId honest UNKNOWN", task.getWikiPageId());
        assertEquals(Integer.valueOf(13), task.getCombatLevel());
        assertEquals(255, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {50, 90}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "chaos-druid-wilderness");

        assertFalse(task.isUndead());
        assertFalse(task.isDemon());
        assertFalse(task.isDragon());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull("no wiki elemental weakness", task.getWeakness().getElement());
        assertEquals(12, task.getMonsterDefence().getDefenceLevel());
        assertEquals(0, task.getMonsterDefence().getStab());
        assertNull(task.getRequiredItemId());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("255")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Elder chaos druid")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo variant = task.getVariantInfo().get(0);
        assertEquals("chaos-druid", variant.getVariantId());
        assertEquals(Integer.valueOf(13), variant.getCombatLevel());
        assertEquals(20.0, variant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        SourceTaskLocationComparison location = task.getLocationComparison().get(0);
        assertEquals("chaos-druid-wilderness", location.getLocationId());
        assertEquals(Integer.valueOf(11), location.getAmount());
    }

    @Test
    public void strategySourceCarriesMeleePluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/chaos-druids/strategy.json");
        assertTrue("Chaos druids strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("chaos-druids", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Chaos_druid", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
    }

    @Test
    public void generatedRuntimeDataCarriesChaosDruidsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Chaos druids".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Chaos druids"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(
            location -> location.isWilderness()));

        assertEquals(1, task.getVariants().size());
        MonsterVariant druid = task.getVariants().get(0);
        assertEquals("Chaos druid", druid.getName());
        assertTrue(druid.isDefault());
        assertNotNull(druid.getStrategy());
        assertEquals(CombatStyle.MELEE, druid.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/chaos-druid-wilderness.json"),
            SourceLocation.class);

        assertEquals("chaos-druid-wilderness", location.getLocationId());
        assertTrue("Krystilia counts only Wilderness kills", location.isWilderness());
        assertFalse("Konar does not assign Chaos druids", location.isKonarLockable());
        assertFalse(location.isMulti());
        assertFalse(location.isCannon());
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

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
 * C2-B (ADR-0019): pins the wiki-sourced Ents family (Krystilia Wilderness). Numbers sourced
 * 2026-07-02 from https://oldschool.runescape.wiki/w/Ent and the Krystilia table. slayerTargetId
 * 258 is SYNTHETIC. Only the level 101 Wilderness ent (npc 6594) counts for Krystilia; it has a
 * 40% fire weakness.
 */
public class EntsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ents.json"), SourceTask.class);

        assertNull("wikiPageId honest UNKNOWN", task.getWikiPageId());
        assertEquals(Integer.valueOf(101), task.getCombatLevel());
        assertEquals(258, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {35, 60}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "ent-wilderness");

        assertFalse(task.isUndead());
        assertFalse(task.isDragon());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());
        assertEquals(75, task.getMonsterDefence().getDefenceLevel());
        assertEquals(30, task.getMonsterDefence().getRange());
        assertNull(task.getRequiredItemId());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("258")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("trunk")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo variant = task.getVariantInfo().get(0);
        assertEquals("ent", variant.getVariantId());
        assertEquals(Integer.valueOf(101), variant.getCombatLevel());
        assertEquals(107.5, variant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        assertEquals("ent-wilderness", task.getLocationComparison().get(0).getLocationId());
    }

    @Test
    public void strategySourceCarriesMagicPluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/ents/strategy.json");
        assertTrue("Ents strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("ents", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Ent", strategy.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getNote().contains("fire"));
    }

    @Test
    public void generatedRuntimeDataCarriesEntsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Ents".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ents"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(location -> location.isWilderness()));

        assertEquals(1, task.getVariants().size());
        MonsterVariant ent = task.getVariants().get(0);
        assertEquals("Ent", ent.getName());
        assertTrue(ent.isDefault());
        assertNotNull(ent.getStrategy());
        assertEquals(CombatStyle.MAGIC, ent.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/ent-wilderness.json"),
            SourceLocation.class);

        assertEquals("ent-wilderness", location.getLocationId());
        assertTrue(location.isWilderness());
        assertFalse(location.isKonarLockable());
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

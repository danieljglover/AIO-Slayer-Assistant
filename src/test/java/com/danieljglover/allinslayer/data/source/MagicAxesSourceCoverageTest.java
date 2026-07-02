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
 * C2-B (ADR-0019): pins the wiki-sourced Magic axes family (Krystilia Wilderness). Numbers
 * sourced 2026-07-02 from https://oldschool.runescape.wiki/w/Magic_axe and the Krystilia table.
 * slayerTargetId 264 is SYNTHETIC. Only the Magic Axe Hut spawns (npc 2844) count for Krystilia.
 */
public class MagicAxesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/magic-axes.json"), SourceTask.class);

        assertNull("wikiPageId honest UNKNOWN", task.getWikiPageId());
        assertEquals(Integer.valueOf(42), task.getCombatLevel());
        assertEquals(264, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "axe-hut");

        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());
        assertEquals(29, task.getMonsterDefence().getDefenceLevel());
        assertEquals(5, task.getMonsterDefence().getSlash());
        assertNull(task.getRequiredItemId());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("264")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Thieving")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo variant = task.getVariantInfo().get(0);
        assertEquals("magic-axe", variant.getVariantId());
        assertEquals(Integer.valueOf(42), variant.getCombatLevel());
        assertEquals(44.0, variant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        SourceTaskLocationComparison location = task.getLocationComparison().get(0);
        assertEquals("axe-hut", location.getLocationId());
        assertEquals(Integer.valueOf(9), location.getAmount());
    }

    @Test
    public void strategySourceCarriesMeleePluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/magic-axes/strategy.json");
        assertTrue("Magic axes strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("magic-axes", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Magic_axe", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
    }

    @Test
    public void generatedRuntimeDataCarriesMagicAxesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Magic axes".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Magic axes"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(location -> location.isWilderness()));

        assertEquals(1, task.getVariants().size());
        MonsterVariant axe = task.getVariants().get(0);
        assertEquals("Magic axe", axe.getName());
        assertTrue(axe.isDefault());
        assertNotNull(axe.getStrategy());
        assertEquals(CombatStyle.MELEE, axe.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/axe-hut.json"),
            SourceLocation.class);

        assertEquals("axe-hut", location.getLocationId());
        assertTrue(location.isWilderness());
        assertFalse(location.isKonarLockable());
        assertTrue(location.getAccessNote().contains("Thieving"));
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

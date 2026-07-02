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
 * C2-A (ADR-0019): pins the wiki-sourced Wilderness Black Knights family (south of the Bandit
 * Camp / Lava Maze). Stats re-verified live 2026-07-02 against the Black Knight wiki page
 * (wikiPageId 44926). Krystilia-only; quantity 75-125, weight 3. slayerTargetId 254 SYNTHETIC.
 * Weak to MAGIC (magic defence -11) despite very high melee/ranged defence. Uses a family-tuned
 * copy of the shared bandit-camp-wilderness location (CT-L rule 2).
 */
public class BlackKnightsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/black-knights.json"), SourceTask.class);

        assertEquals(Integer.valueOf(44926), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(254, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "bandit-camp-wilderness-black-knights");

        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());
        assertEquals(25, task.getMonsterDefence().getDefenceLevel());
        assertEquals("magic defence is negative (weakest)", -11, task.getMonsterDefence().getMagic());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("254")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("magic")));

        assertEquals(1, task.getVariantInfo().size());
        assertEquals("black-knight-lvl33", task.getVariantInfo().get(0).getVariantId());
    }

    @Test
    public void strategySourceCarriesMagicPluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/black-knights/strategy.json");
        assertTrue("Black knights strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("black-knights", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Black_Knight", strategy.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertFalse(strategy.getPlugin().getPrimaryWeapons().isEmpty());
        assertTrue(strategy.getPlugin().getNote().contains("magic"));
    }

    @Test
    public void generatedRuntimeDataCarriesBlackKnightsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Black knights".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Black knights"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertEquals(1, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> location.isWilderness()));

        MonsterVariant def = task.getVariants().get(0);
        assertTrue(def.isDefault());
        assertNotNull(def.getStrategy());
        assertEquals(CombatStyle.MAGIC, def.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(
            Paths.get("src/main/data/slayer/locations/bandit-camp-wilderness-black-knights.json"),
            SourceLocation.class);

        assertEquals("bandit-camp-wilderness-black-knights", location.getLocationId());
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

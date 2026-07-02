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
 * C2-A (ADR-0019): pins the wiki-sourced Wilderness Bandits family (Bandit Camp, Wilderness).
 * Monster stats re-verified live 2026-07-02 against the Bandit wiki page (wikiPageId 20203).
 * Krystilia-only task; quantity 75-125, weight 4 (masters-coverage.md 3.7 / live Krystilia page).
 * slayerTargetId 252 is SYNTHETIC per the WC-1..11 / C2-C3 allocation contract. Owner of the
 * shared new location file bandit-camp-wilderness (CT-L). Not the Kharidian/Pollnivneach bandits.
 */
public class BanditsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/bandits.json"), SourceTask.class);

        assertEquals(Integer.valueOf(20203), task.getWikiPageId());
        assertNull("no combat-level requirement for the Krystilia bandits task", task.getCombatLevel());
        assertEquals(252, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        // Weight re-verified live on the Krystilia page 2026-07-02.
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "bandit-camp-wilderness");

        assertFalse(task.isUndead());
        assertFalse(task.isDemon());
        assertFalse(task.isDragon());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull("no wiki-documented elemental weakness", task.getWeakness().getElement());
        assertNull(task.getRequiredItemId());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("252")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("aggressive")));

        assertEquals(2, task.getVariantInfo().size());
    }

    @Test
    public void strategySourceCarriesMeleePluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/bandits/strategy.json");
        assertTrue("Bandits strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("bandits", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Bandit", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertFalse(strategy.getPlugin().getPrimaryWeapons().isEmpty());
    }

    @Test
    public void generatedRuntimeDataCarriesBanditsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Bandits".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Bandits"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));
        assertEquals(2, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(
            location -> "Bandit Camp (Wilderness)".equals(location.getName()) && location.isWilderness()));

        MonsterVariant def = task.getVariants().stream().filter(MonsterVariant::isDefault).findFirst()
            .orElseThrow(() -> new AssertionError("no default variant"));
        assertNotNull(def.getStrategy());
        assertEquals(CombatStyle.MELEE, def.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/bandit-camp-wilderness.json"),
            SourceLocation.class);

        assertEquals("bandit-camp-wilderness", location.getLocationId());
        assertEquals("Bandit Camp (Wilderness)", location.getName());
        assertTrue("Krystilia kills only count in the Wilderness", location.isWilderness());
        assertFalse("Konar does not assign the Wilderness bandits", location.isKonarLockable());
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

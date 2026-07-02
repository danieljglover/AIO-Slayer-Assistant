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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AraxytesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/araxytes.json"), SourceTask.class);

        assertEquals(Integer.valueOf(529981), task.getWikiPageId());
        assertNull("wiki task infobox combat requirement is None", task.getCombatLevel());
        assertEquals(203, task.getSlayerTargetId());
        assertEquals(92, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Priest in Peril"));

        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {40, 60}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {60, 80}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "more-eyes-than-sense");
        assertContains(locationIds(task), "araxyte-lair-morytania");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Turael") && note.contains("spiders")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("92 Slayer")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("venom")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("extended anti-venom+")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("spider cave teleport")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("level 96 araxytes")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("level 146")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dreadborn Araxyte")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("seed box")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("herb sack")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Araxyte venom sacks")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Venator bow")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Araxxor")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/araxytes.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertEquals(60.0, variants.get("araxyte-level-96").getSlayerXp().doubleValue(), 0.0);
        assertEquals(100.0, variants.get("araxyte-level-146").getSlayerXp().doubleValue(), 0.0);
        assertEquals(4658.0, variants.get("dreadborn-araxyte").getSlayerXp().doubleValue(), 0.0);
        assertEquals(1708.0, variants.get("araxxor").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("dreadborn-araxyte").getNotes().stream()
            .anyMatch(note -> note.contains("venom pools")));
        assertTrue(variants.get("araxxor").getNotes().stream()
            .anyMatch(note -> note.contains("Boss variant")));

        assertEquals(1, locations.size());
        assertEquals(null, locations.get("araxyte-lair-morytania").getAmount());
        assertEquals(Boolean.TRUE, locations.get("araxyte-lair-morytania").getMulticombat());
        assertEquals(Boolean.TRUE, locations.get("araxyte-lair-morytania").getCannonable());
        assertEquals(Boolean.TRUE, locations.get("araxyte-lair-morytania").getSafespottable());
        assertTrue(locations.get("araxyte-lair-morytania").getNotes().stream()
            .anyMatch(note -> note.contains("A lot")));
        assertTrue(locations.get("araxyte-lair-morytania").getNotes().stream()
            .anyMatch(note -> note.contains("Araxxor")));
    }

    @Test
    public void strategySourceContainsNormalAraxyteTaskBreakdown() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/araxytes/strategy.json");

        assertTrue("Araxytes strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("araxytes", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Araxytes", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("venator-bow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "noxious-halberd".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "normal-146-araxytes");
        assertContains(methodIds(strategy), "cannon-venator");
        assertContains(methodIds(strategy), "avoid-level-96");
        assertContains(methodIds(strategy), "araxxor-alternative");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "cannon-venator".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing cannon Venator method"))
            .getSteps().stream().anyMatch(step -> step.contains("dwarf multicannon")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "araxxor-alternative".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Araxxor method"))
            .getNotes().stream().anyMatch(note -> note.contains("strong crush weapon")));

        assertContains(styleIds(strategy), "ranged-cannon");
        assertContains(styleIds(strategy), "melee");
    }

    @Test
    public void generatedRuntimeDataCarriesAraxyteStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Araxytes".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Araxytes"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Araxyte Lair (Morytania)".equals(location.getName())
            && location.isMulti() && location.isCannon()));

        MonsterVariant araxyte = task.getVariants().stream()
            .filter(variant -> "Araxyte (level 146)".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing level 146 Araxyte"));

        assertNotNull(araxyte.getStrategy());
        assertEquals(CombatStyle.RANGED, araxyte.getStrategy().getPrimaryStyle());
        assertEquals("Venator bow", araxyte.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(araxyte.getStrategy().getNote().contains("level 146"));
    }

    @Test
    public void araxyteLairLocationSourceMatchesTaskPage() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/araxyte-lair-morytania.json"),
            SourceLocation.class);

        assertEquals("araxyte-lair-morytania", location.getLocationId());
        assertEquals("Araxyte Lair (Morytania)", location.getName());
        assertTrue(location.isMulti());
        assertTrue(location.isCannon());
        assertTrue(location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertTrue(location.getAccessNote().contains("Morytania Spider Cave"));
        assertTrue(location.getAccessNote().contains("Araxxor"));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> unlockIds(SourceTask task)
    {
        return task.getUnlocks().stream().map(SourceTaskUnlock::getUnlockId).collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream().map(SourceStrategyMethod::getMethodId).collect(Collectors.toSet());
    }

    private static Set<String> styleIds(SourceStrategy strategy)
    {
        return strategy.getStyleOptions().stream().map(SourceStrategyStyleOption::getStyleId)
            .collect(Collectors.toSet());
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

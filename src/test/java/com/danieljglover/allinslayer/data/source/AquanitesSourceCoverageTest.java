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

public class AquanitesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndAccessData() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/aquanites.json"), SourceTask.class);

        assertEquals(Integer.valueOf(630114), task.getWikiPageId());
        assertNull("wiki task infobox combat requirement is None", task.getCombatLevel());
        assertEquals(202, task.getSlayerTargetId());
        assertEquals(78, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {40, 60}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "lured-in");
        assertContains(unlockIds(task), "lets-stay-all-aquanite");
        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "aquanite-cavern-sailing");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Sailing") && note.contains("73")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("adamant keel")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Lured In")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Aquanite tendon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("single-way combat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dwarf multicannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("slash attack")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("stab Defence")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("rowboat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Song of the Elves")));
    }

    @Test
    public void taskSourceContainsVariantAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/aquanites.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertEquals(180.0, variants.get("aquanite").getSlayerXp().doubleValue(), 0.0);
        assertEquals(4200.0, variants.get("elder-aquanite").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("aquanite").getNotes().stream().anyMatch(note -> note.contains("lure")));
        assertTrue(variants.get("elder-aquanite").getNotes().stream().anyMatch(note -> note.contains("1/200")));
        assertTrue(variants.get("elder-aquanite").getNotes().stream().anyMatch(note -> note.contains("35 damage")));

        assertEquals(1, locations.size());
        assertEquals(Integer.valueOf(0), locations.get("aquanite-cavern-sailing").getAmount());
        assertEquals(Boolean.FALSE, locations.get("aquanite-cavern-sailing").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("aquanite-cavern-sailing").getCannonable());
        assertEquals(Boolean.FALSE, locations.get("aquanite-cavern-sailing").getSafespottable());
        assertTrue(locations.get("aquanite-cavern-sailing").getNotes().stream()
            .anyMatch(note -> note.contains("Ynysdail Cavern")));
    }

    @Test
    public void strategySourceContainsWikiEquipmentAndMechanics() throws IOException
    {
        Path legacyMarkdown = Paths.get("src/main/data/slayer/strategies/aquanite.md");
        Path json = Paths.get("src/main/data/slayer/strategies/aquanite/strategy.json");

        assertFalse("Aquanite strategy should be JSON, not Markdown", Files.exists(legacyMarkdown));
        assertTrue("Aquanite strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("aquanite", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Aquanites", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("soulreaper-axe"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("ghrazi-rapier"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "abyssal-whip".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "saradomin-godsword".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "melee-slash-stab");
        assertContains(methodIds(strategy), "elder-aquanite");
        assertContains(methodIds(strategy), "access");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "melee-slash-stab".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing melee slash/stab method"))
            .getSteps().stream().anyMatch(step -> step.contains("sever")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "elder-aquanite".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing elder method"))
            .getSteps().stream().anyMatch(step -> step.contains("flick")));
        assertContains(styleIds(strategy), "melee-switch");
    }

    @Test
    public void generatedRuntimeDataCarriesAquaniteStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Aquanites".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Aquanites"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Aquanite cavern (Sailing)".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));

        MonsterVariant aquanite = task.getVariants().stream()
            .filter(variant -> "Aquanite".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Aquanite variant"));

        assertNotNull(aquanite.getStrategy());
        assertEquals(CombatStyle.MELEE, aquanite.getStrategy().getPrimaryStyle());
        assertEquals("Soulreaper axe", aquanite.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(aquanite.getStrategy().getNote().contains("Protect from Magic"));
    }

    @Test
    public void aquaniteCavernLocationSourceMatchesTaskPage() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/aquanite-cavern-sailing.json"),
            SourceLocation.class);

        assertEquals("aquanite-cavern-sailing", location.getLocationId());
        assertEquals("Aquanite cavern (Sailing)", location.getName());
        assertFalse(location.isMulti());
        assertFalse(location.isCannon());
        assertFalse(location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertTrue(location.getAccessNote().contains("Sailing 73"));
        assertTrue(location.getAccessNote().contains("adamant keel"));
        assertTrue(location.getAccessNote().contains("Ynysdail"));
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

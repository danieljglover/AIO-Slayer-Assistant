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

public class FrostDragonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentRequirementsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/frost-dragons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(39287), task.getWikiPageId());
        assertEquals(Integer.valueOf(85), task.getCombatLevel());
        assertEquals(9001, task.getSlayerTargetId());
        assertEquals("Frost dragon has no Slayer skill requirement on the wiki", 1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());
        assertTrue(task.isDragon());

        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {60, 100}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {70, 120}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {180, 240}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {180, 240}, task.getExtendedAmount().get("duradel"));
        assertContains(unlockIds(task), "chance-of-heavy-frost");
        assertContains(unlockIds(task), "i-see-dragons");
        assertContains(locationIds(task), "grimstone-dungeon");
        assertContains(locationIds(task), "grimstone-dungeon-task-only");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("87 Sailing")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fairy ring") && note.contains("DLP")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("melee") && note.contains("short-ranged dragonfire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("icy") && note.contains("freeze")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("regular antifire potion with an anti-dragon shield")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("super antifire potion alone")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Safespotted")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonbane weapons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("do not count as blue dragons or metal dragons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dragon metal sheet") && note.contains("1/40")));
    }

    @Test
    public void taskSourceContainsMonsterPageVariantAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/frost-dragons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(1, variants.size());
        SourceTaskVariantInfo frostDragon = variants.get("frost-dragon");
        assertNotNull(frostDragon);
        assertEquals(Integer.valueOf(202), frostDragon.getCombatLevel());
        assertEquals(235.5, frostDragon.getSlayerXp().doubleValue(), 0.0);
        assertTrue(frostDragon.getNotes().stream().anyMatch(note -> note.contains("max hit 16")));
        assertTrue(frostDragon.getNotes().stream().anyMatch(note -> note.contains("50") && note.contains("dragonfire")));
        assertTrue(frostDragon.getNotes().stream().anyMatch(note -> note.contains("100%") && note.contains("fire spells")));

        assertEquals(2, locations.size());
        assertLocation(locations, "grimstone-dungeon", 20, false, false, true, "Grimstone Dungeon");
        assertLocation(locations, "grimstone-dungeon-task-only", null, false, false, true, "without permission");
    }

    @Test
    public void strategySourceContainsAllStrategyPageStylesAndMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/frost-dragons/strategy.json");

        assertTrue("Frost dragons strategy JSON missing", Files.exists(json));
        assertFalse("Legacy Frost dragon Markdown strategy should be migrated to JSON",
            Files.exists(Paths.get("src/main/data/slayer/strategies/frost-dragon.md")));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("frost-dragons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Frost_dragon/Strategies", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-lance"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("inquisitor-s-mace"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("Protect from Melee"));
        assertTrue(strategy.getPlugin().getNote().contains("full dragonfire protection"));

        assertContains(methodIds(strategy), "attacks-and-protection");
        assertContains(methodIds(strategy), "transportation");
        assertContains(methodIds(strategy), "melee-crush-dragonbane");
        assertContains(methodIds(strategy), "ranged-heavy-dragonbane");
        assertContains(methodIds(strategy), "magic-fire-spells");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "attacks-and-protection".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing attacks method"))
            .getSteps().stream().anyMatch(step -> step.contains("Protect from Melee")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "transportation".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing transportation method"))
            .getSteps().stream().anyMatch(step -> step.contains("DLP")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "magic-fire-spells".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing magic method"))
            .getSteps().stream().anyMatch(step -> step.contains("100%") && step.contains("fire")));

        assertContains(styleIds(strategy), "melee-crush-dragonbane");
        assertContains(styleIds(strategy), "ranged-heavy-dragonbane");
        assertContains(styleIds(strategy), "magic-fire-spells");
    }

    @Test
    public void generatedRuntimeDataCarriesFrostDragonSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Frost Dragons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Frost Dragons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Grimstone Dungeon".equals(location.getName())
            && !location.isCannon() && !location.isMulti()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Grimstone Dungeon task-only cavern".equals(location.getName())
            && !location.isCannon() && !location.isMulti()));
        assertTrue(task.isDragon());

        MonsterVariant frostDragon = task.getVariants().stream()
            .filter(variant -> "Frost dragon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Frost dragon"));

        assertEquals(CombatStyle.RANGED, frostDragon.getWeakness().getStyle());
        assertEquals("fire", frostDragon.getWeakness().getElement());
        assertNotNull(frostDragon.getStrategy());
        assertEquals(CombatStyle.MELEE, frostDragon.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Frost_dragon/Strategies",
            frostDragon.getStrategy().getSourceUrl());
        assertTrue(frostDragon.getStrategy().getNote().contains("100% fire elemental weakness"));
    }

    @Test
    public void locationSourcesMatchFrostDragonPage() throws IOException
    {
        assertLocationSource("grimstone-dungeon", "Grimstone Dungeon", false, false, true, false, "87 Sailing");
        assertLocationSource("grimstone-dungeon-task-only", "Grimstone Dungeon task-only cavern", false, false,
            true, false, "Frost dragon Slayer task");
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

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        Integer amount, boolean multi, boolean cannon, boolean safespot, String note)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertEquals(amount, location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(value -> value.contains(note)));
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safeSpot, boolean wilderness, String accessNote) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertEquals(safeSpot, location.isSafeSpot());
        assertEquals(wilderness, location.isWilderness());
        assertTrue(location.getAccessNote().contains(accessNote));
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

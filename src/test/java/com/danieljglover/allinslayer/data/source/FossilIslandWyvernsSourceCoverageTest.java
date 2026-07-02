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
import static org.junit.Assert.assertTrue;

public class FossilIslandWyvernsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlocksAndShieldMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/fossil-island-wyverns.json"), SourceTask.class);

        assertEquals(Integer.valueOf(297887), task.getWikiPageId());
        assertEquals(Integer.valueOf(60), task.getCombatLevel());
        assertEquals(219, task.getSlayerTargetId());
        assertEquals(66, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Bone Voyage"));
        assertTrue(task.getQuestReqs().contains("Elemental Workshop I"));
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());
        assertTrue(task.isDragon());

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertFalse(task.getMasterIds().contains("vannaka"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {5, 25}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {20, 50}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {55, 75}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {55, 75}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {55, 75}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {55, 75}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "stop-the-wyvern");
        assertContains(unlockIds(task), "wyver-nother-two");
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("icy breath") && note.contains("elemental shield")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("ancient wyvern shield") && note.contains("freezing")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Anti-dragon shield") && note.contains("do not work")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Magic") && note.contains("half")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Spitting Wyverns") && note.contains("quick clear")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Taloned Wyverns") && note.contains("least favourable")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fossils") && note.contains("Fossil Storage")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationComparisonRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/fossil-island-wyverns.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertVariant(variants, "spitting-wyvern", 139, 205.0, "Protect from Missiles");
        assertVariant(variants, "taloned-wyvern", 147, 205.0, "least favourable");
        assertVariant(variants, "long-tailed-wyvern", 152, 205.0, "melee distance");
        assertVariant(variants, "ancient-wyvern", 210, 315.0, "82 Slayer");

        assertEquals(2, locations.size());
        assertLocation(locations, "wyvern-cave-fossil-island", 14, false, false, false, "east of Museum Camp");
        assertLocation(locations, "wyvern-cave-fossil-island-task-only", 15, false, false, false, "Requires a Fossil Island Wyverns slayer task");
    }

    @Test
    public void strategySourceContainsAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/fossil-island-wyverns/strategy.json");

        assertTrue("Fossil Island wyverns strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("fossil-island-wyverns", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Fossil_Island_wyverns", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-lance"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("osmumten-s-fang"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("zamorakian-hasta"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("anti-icy-breath shield"));
        assertTrue(strategy.getPlugin().getNote().contains("Spitting Wyverns"));

        assertContains(methodIds(strategy), "travel-and-locations");
        assertContains(methodIds(strategy), "shield-protection");
        assertContains(methodIds(strategy), "spitting-quick-clear");
        assertContains(methodIds(strategy), "melee-tank");
        assertContains(methodIds(strategy), "prayer-melee");
        assertContains(methodIds(strategy), "magic-powered-staves");
        assertContains(methodIds(strategy), "ranged-distance");
        assertContains(methodIds(strategy), "ancient-wyverns");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "shield-protection".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing shield method"))
            .getSteps().stream().anyMatch(step -> step.contains("Anti-dragon shield")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "spitting-quick-clear".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing spitting method"))
            .getSteps().stream().anyMatch(step -> step.contains("Protect from Missiles")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "inventory-and-loot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing inventory method"))
            .getInventory().stream().anyMatch(item -> item.contains("Seed box")));

        assertContains(styleIds(strategy), "melee-tank");
        assertContains(styleIds(strategy), "prayer-melee");
        assertContains(styleIds(strategy), "magic-powered-staves");
        assertContains(styleIds(strategy), "ranged-distance");
    }

    @Test
    public void generatedRuntimeDataCarriesFossilIslandWyvernStrategyAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Fossil Island wyverns".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Fossil Island wyverns"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(4, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wyvern Cave (Fossil Island)".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Task-only Wyvern Cave (Fossil Island)".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));

        MonsterVariant spitting = variantNamed(task, "Spitting Wyvern");
        MonsterVariant ancient = variantNamed(task, "Ancient Wyvern");

        assertNotNull(spitting.getStrategy());
        assertEquals(CombatStyle.MELEE, spitting.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Fossil_Island_wyverns",
            spitting.getStrategy().getSourceUrl());
        assertNotNull(ancient.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Fossil_Island_wyverns",
            ancient.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchFossilIslandWyvernsTaskPage() throws IOException
    {
        assertLocationSource("wyvern-cave-fossil-island", "Wyvern Cave (Fossil Island)", false, false, false, false,
            "east of Museum Camp");
        assertLocationSource("wyvern-cave-fossil-island-task-only", "Task-only Wyvern Cave (Fossil Island)", false,
            false, false, false, "Mushroom Forest");
    }

    private static MonsterVariant variantNamed(TaskData task, String name)
    {
        return task.getVariants().stream()
            .filter(variant -> name.equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing variant: " + name));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> unlockIds(SourceTask task)
    {
        return task.getUnlocks().stream().map(SourceTaskUnlock::getUnlockId).collect(Collectors.toSet());
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

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        Integer combatLevel, double slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(slayerXp, variant.getSlayerXp().doubleValue(), 0.0);
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        Integer amount, boolean multi, boolean cannon, boolean safespot, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertEquals(amount, location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safespot, boolean wilderness, String accessNote) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertEquals(safespot, location.isSafeSpot());
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

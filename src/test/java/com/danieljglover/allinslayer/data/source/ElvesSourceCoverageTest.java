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

public class ElvesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentRequirementsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/elves.json"), SourceTask.class);

        assertEquals(Integer.valueOf(256687), task.getWikiPageId());
        assertEquals(Integer.valueOf(70), task.getCombatLevel());
        assertEquals(217, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Regicide"));
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {30, 70}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {60, 90}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {100, 170}, task.getAmountByMaster().get("duradel"));
        assertTrue(task.getUnlocks().isEmpty());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Regicide")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("crystal bow") && note.contains("crystal halberd")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Iorwerth Camp") && note.contains("tedious")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Mourner Headquarters") && note.contains("Mourner gear")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Song of the Elves") && note.contains("Iorwerth Dungeon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("crystal shard") && note.contains("enhanced crystal teleport seed")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("skip elf tasks")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Adept Reanimation") && note.contains("754 Prayer")));
    }

    @Test
    public void taskSourceContainsAllVariantRowsAndLocationComparisonRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/elves.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(7, variants.size());
        assertVariant(variants, "iorwerth-archer", 90, 105.0, "aggressive");
        assertVariant(variants, "elf-archer", 90, 105.0, "Mourning's End Part I");
        assertVariant(variants, "iorwerth-warrior", 108, 107.5, "crystal shard");
        assertVariant(variants, "elf-warrior", 108, 107.5, "Mourning's End Part I");
        assertVariant(variants, "mourner", 108, 107.5, "not after Song of the Elves");
        assertVariant(variants, "guard-prifddinas", 108, 107.5, "enhanced crystal teleport seed");
        assertNull(variants.get("reanimated-elf").getCombatLevel());
        assertEquals(35.0, variants.get("reanimated-elf").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("reanimated-elf").getNotes().stream()
            .anyMatch(note -> note.contains("754 Prayer") && note.contains("Adept Reanimation")));

        assertEquals(12, locations.size());
        assertLocation(locations, "iorwerth-camp", null, false, true, true, "two tiles");
        assertLocation(locations, "iorwerth-dungeon-prifddinas", 13, false, true, true, "fabric panels");
        assertLocation(locations, "lletya", 3, false, true, true, "bench");
        assertLocation(locations, "lletya-upstairs", 1, false, true, false, "upstairs");
        assertLocation(locations, "mourner-headquarters-elves", 4, false, false, true, "Mourner Gear");
        assertLocation(locations, "prifddinas-south-gate", 2, false, true, true, "South Gate");
        assertLocation(locations, "prifddinas-east-gate", 2, false, true, true, "Close to a bank");
        assertLocation(locations, "prifddinas-north-gate", 2, false, true, true, "Spirit tree");
        assertLocation(locations, "prifddinas-west-gate", 2, false, true, true, "house portal");
        assertLocation(locations, "tower-of-voices-ground-floor", 4, false, true, true, "each entrance");
        assertLocation(locations, "tower-of-voices-upper-floor", 8, false, true, false, "walkways");
        assertLocation(locations, "prifddinas-market-place", 3, false, true, true, "Glenda");
    }

    @Test
    public void strategySourceContainsAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/elves/strategy.json");

        assertTrue("Elves strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("elves", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Elves", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("osmumten-s-fang"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("blade-of-saeldor"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-swamp".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("Iorwerth Dungeon"));
        assertTrue(strategy.getPlugin().getNote().contains("skip"));

        assertContains(methodIds(strategy), "iorwerth-dungeon");
        assertContains(methodIds(strategy), "prifddinas-guards");
        assertContains(methodIds(strategy), "lletya-prayer");
        assertContains(methodIds(strategy), "safespot-ranged-magic");
        assertContains(methodIds(strategy), "iorwerth-camp");
        assertContains(methodIds(strategy), "mourner-headquarters");
        assertContains(methodIds(strategy), "reanimated-elf");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "iorwerth-dungeon".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing dungeon method"))
            .getSteps().stream().anyMatch(step -> step.contains("fabric panels")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "mourner-headquarters".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing mourner method"))
            .getRequiredOrKeyItems().stream().anyMatch(item -> item.contains("Mourner gear")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "reanimated-elf".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing reanimation method"))
            .getSteps().stream().anyMatch(step -> step.contains("Adept Reanimation")));

        assertContains(styleIds(strategy), "melee-prayer");
        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "magic-safespot");
        assertContains(styleIds(strategy), "reanimation");
    }

    @Test
    public void generatedRuntimeDataCarriesElfStrategiesVariantsAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Elves".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Elves"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertFalse(task.getAssignedBy().contains("konar"));
        assertEquals(6, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Iorwerth Camp".equals(location.getName())
            && !location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Iorwerth Dungeon".equals(location.getName())
            && !location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Lletya".equals(location.getName())
            && !location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Mourner Headquarters".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));
        assertTrue(task.getVariants().stream()
            .noneMatch(variant -> "Reanimated elf".equals(variant.getName())));

        MonsterVariant warrior = variantNamed(task, "Iorwerth Warrior");

        assertNotNull(warrior.getStrategy());
        assertEquals(CombatStyle.MELEE, warrior.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Elves", warrior.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchElfTaskPage() throws IOException
    {
        assertLocationSource("iorwerth-camp", "Iorwerth Camp", false, true, true, false, "stools");
        assertLocationSource("iorwerth-dungeon-prifddinas", "Iorwerth Dungeon", false, true, true, false,
            "fabric panels");
        assertLocationSource("lletya", "Lletya", false, true, true, false, "bench");
        assertLocationSource("lletya-upstairs", "Lletya (upstairs)", false, true, false, false, "upstairs");
        assertLocationSource("mourner-headquarters-elves", "Mourner Headquarters", false, false, true, false,
            "Mourner gear");
        assertLocationSource("dark-altar-elves", "Dark Altar", false, false, false, false, "Adept Reanimation");
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

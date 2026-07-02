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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BloodveldSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/bloodveld.json"), SourceTask.class);

        assertEquals(Integer.valueOf(297737), task.getWikiPageId());
        assertEquals(Integer.valueOf(50), task.getCombatLevel());
        assertEquals(208, task.getSlayerTargetId());
        assertEquals(50, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Priest in Peril"));

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {70, 110}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("krystilia"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("vannaka"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "bleed-me-dry");
        assertContains(locationIds(task), "buccaneers-laboratory");
        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "iorwerth-dungeon");
        assertContains(locationIds(task), "god-wars-dungeon");
        assertContains(locationIds(task), "meiyerditch-laboratories");
        assertContains(locationIds(task), "slayer-tower");
        assertContains(locationIds(task), "slayer-tower-basement");
        assertContains(locationIds(task), "stronghold-slayer-cave");
        assertContains(locationIds(task), "wilderness-god-wars-dungeon");

        assertTrue("Bloodveld are demons", task.isDemon());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("demonbane weapons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("ash sanctifier")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("magic-based melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonhide") && note.contains("blood moon armour")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Stronghold Slayer Cave") && note.contains("dwarf multicannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Meiyerditch Laboratories") && note.contains("seven mutated bloodvelds")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Buccaneers' Laboratory") && note.contains("76 Sailing")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Konar") && note.contains("God Wars Dungeon") && note.contains("skipped")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia") && note.contains("Wilderness God Wars Dungeon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Expert Reanimation") && note.contains("ensouled bloodveld head")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/bloodveld.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(6, variants.size());
        assertEquals(120.0, variants.get("bloodveld").getSlayerXp().doubleValue(), 0.0);
        assertEquals(134.0, variants.get("bloodveld-god-wars-dungeon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(2900.0, variants.get("insatiable-bloodveld").getSlayerXp().doubleValue(), 0.0);
        assertEquals(170.0, variants.get("mutated-bloodveld").getSlayerXp().doubleValue(), 0.0);
        assertEquals(4100.0, variants.get("insatiable-mutated-bloodveld").getSlayerXp().doubleValue(), 0.0);
        assertEquals(35.0, variants.get("reanimated-bloodveld").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("bloodveld").getNotes().stream()
            .anyMatch(note -> note.contains("Morytania Diary")));
        assertTrue(variants.get("bloodveld-god-wars-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("Zamorak item")));
        assertTrue(variants.get("insatiable-bloodveld").getNotes().stream()
            .anyMatch(note -> note.contains("Cannot spawn") && note.contains("God Wars")));
        assertTrue(variants.get("mutated-bloodveld").getNotes().stream()
            .anyMatch(note -> note.contains("ancient shard") && note.contains("dark totem")));
        assertTrue(variants.get("reanimated-bloodveld").getNotes().stream()
            .anyMatch(note -> note.contains("1,040 Prayer experience")));

        assertEquals(9, locations.size());
        assertLocation(locations, "buccaneers-laboratory", 8, true, true, true, "76 Sailing");
        assertLocation(locations, "catacombs-of-kourend", 18, true, false, true, "dark totem");
        assertLocation(locations, "iorwerth-dungeon", 12, false, true, true, "two sections");
        assertLocation(locations, "god-wars-dungeon", 16, true, false, true, "superior version");
        assertLocation(locations, "meiyerditch-laboratories", 11, true, true, true, "4 Bloodvelds");
        assertLocation(locations, "slayer-tower", 15, false, false, true, "Morytania Diary");
        assertLocation(locations, "slayer-tower-basement", 12, false, false, true, "Slayer ring");
        assertLocation(locations, "stronghold-slayer-cave", 12, false, true, true, "Slayer ring");
        assertLocation(locations, "wilderness-god-wars-dungeon", 5, true, false, true, "64 Agility");
    }

    @Test
    public void strategySourceContainsAllTaskPageStrategyOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/bloodveld/strategy.json");

        assertTrue("Bloodveld strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("bloodveld", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Bloodveld", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("emberlight"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "venator-bow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-seas".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "stronghold-cannon");
        assertContains(methodIds(strategy), "meiyerditch-mutated-cannon");
        assertContains(methodIds(strategy), "buccaneers-laboratory");
        assertContains(methodIds(strategy), "slayer-tower");
        assertContains(methodIds(strategy), "catacombs-of-kourend");
        assertContains(methodIds(strategy), "iorwerth-dungeon");
        assertContains(methodIds(strategy), "konar-god-wars-dungeon");
        assertContains(methodIds(strategy), "wilderness-god-wars-dungeon");
        assertContains(methodIds(strategy), "reanimated-bloodveld");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "general".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing general method"))
            .getPrayers().stream().anyMatch(prayer -> prayer.contains("Protect from Melee")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "meiyerditch-mutated-cannon".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Meiyerditch method"))
            .getSteps().stream().anyMatch(step -> step.contains("cluster of seven mutated bloodvelds")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness-god-wars-dungeon".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Wilderness method"))
            .getRisks().stream().anyMatch(risk -> risk.contains("player killing")));

        assertContains(styleIds(strategy), "melee-demonbane");
        assertContains(styleIds(strategy), "ranged-cannon-or-safespot");
        assertContains(styleIds(strategy), "magic-safespot");
    }

    @Test
    public void generatedRuntimeDataCarriesBloodveldSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Bloodveld".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Bloodveld"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Meiyerditch Laboratories".equals(location.getName())
            && location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness God Wars Dungeon".equals(location.getName())
            && location.isMulti() && location.isWilderness()));
        assertTrue(task.getVariants().stream()
            .noneMatch(variant -> "Reanimated bloodveld".equals(variant.getName())));

        MonsterVariant bloodveld = task.getVariants().stream()
            .filter(variant -> "Bloodveld".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Bloodveld variant"));

        assertNotNull(bloodveld.getStrategy());
        assertEquals(CombatStyle.MELEE, bloodveld.getStrategy().getPrimaryStyle());
        assertEquals("Emberlight", bloodveld.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(bloodveld.getStrategy().getNote().contains("magic-based melee"));
        assertTrue(task.isDemon());
    }

    @Test
    public void locationSourcesMatchTaskPage()
        throws IOException
    {
        assertLocationSource("buccaneers-laboratory", "Buccaneers' Laboratory", true, true, true, false,
            "76 Sailing");
        assertLocationSource("iorwerth-dungeon", "Iorwerth Dungeon", false, true, true, false,
            "Song of the Elves");
        assertLocationSource("meiyerditch-laboratories", "Meiyerditch Laboratories", true, true, true, false,
            "Sins of the Father");
        assertLocationSource("god-wars-dungeon", "God Wars Dungeon", true, false, true, false, "Zamorak item");
        assertLocationSource("wilderness-god-wars-dungeon", "Wilderness God Wars Dungeon", true, false, false, true,
            "64 Agility");
        assertLocationSource("dark-altar", "Dark Altar", false, false, false, false,
            "ensouled bloodveld head");
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        int amount, boolean multi, boolean cannon, boolean safespot, String note)
    {
        SourceTaskLocationComparison location = locations.get(locationId);
        assertEquals(Integer.valueOf(amount), location.getAmount());
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

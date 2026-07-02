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

public class BlueDragonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/blue-dragons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(297956), task.getWikiPageId());
        assertEquals(Integer.valueOf(65), task.getCombatLevel());
        assertEquals(209, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Dragon Slayer I"));

        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {110, 170}, task.getAmountByMaster().get("duradel"));
        assertTrue(task.getExtendedAmount() == null || task.getExtendedAmount().isEmpty());

        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "corsair-cove-dungeon");
        assertContains(locationIds(task), "dragon-nest");
        assertContains(locationIds(task), "heroes-guild-basement");
        assertContains(locationIds(task), "isle-of-souls-dungeon");
        assertContains(locationIds(task), "ogre-enclave-watchtower");
        assertContains(locationIds(task), "taverley-dungeon");
        assertContains(locationIds(task), "taverley-dungeon-upper");
        assertContains(locationIds(task), "ruins-of-tapoyauik");
        assertContains(locationIds(task), "ungael");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("65 combat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("starting Dragon Slayer I")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("melee") && note.contains("dragonfire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("brutal blue dragons") && note.contains("magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonfire protection")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("safespotted") && note.contains("halberd")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonbane weapons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dwarf multicannon") && note.contains("baby blue dragons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dragon Slayer II") && note.contains("Vorkath")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Konar") && note.contains("Ogre Enclave")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Isle of Souls Dungeon") && note.contains("cannon")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/blue-dragons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertEquals(50.0, variants.get("baby-blue-dragon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(107.6, variants.get("blue-dragon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(257.0, variants.get("brutal-blue-dragon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(460.0, variants.get("blue-dragons-vorkath").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("baby-blue-dragon").getNotes().stream()
            .anyMatch(note -> note.contains("does not use dragonfire")));
        assertTrue(variants.get("blue-dragon").getNotes().stream()
            .anyMatch(note -> note.contains("Heroes' Quest") && note.contains("Watchtower")));
        assertTrue(variants.get("brutal-blue-dragon").getNotes().stream()
            .anyMatch(note -> note.contains("additional magic attack") && note.contains("long ranged dragonfire")));
        assertTrue(variants.get("blue-dragons-vorkath").getNotes().stream()
            .anyMatch(note -> note.contains("undead") && note.contains("750")));

        assertEquals(10, locations.size());
        assertLocation(locations, "catacombs-of-kourend", 2, false, false, true, "Dark Totem");
        assertLocation(locations, "corsair-cove-dungeon", 8, false, true, true, "Dragon Slayer II");
        assertLocation(locations, "dragon-nest", 7, false, true, true, "Tal Teklan");
        assertLocation(locations, "heroes-guild-basement", 1, false, true, true, "Heroes' Quest");
        assertLocation(locations, "isle-of-souls-dungeon", 7, false, true, true, "4 Adults");
        assertLocation(locations, "ogre-enclave-watchtower", 6, false, true, true, "Watchtower");
        assertLocation(locations, "taverley-dungeon", 21, false, true, true, "dusty key");
        assertLocation(locations, "taverley-dungeon-upper", 12, false, true, true, "Slayer task");
        assertLocation(locations, "ruins-of-tapoyauik", 19, false, false, true, "Pendant of ates");
        assertLocation(locations, "ungael", 1, false, false, false, "Vorkath/Strategies");
    }

    @Test
    public void strategySourceContainsAllTaskPageStrategyOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/blue-dragons/strategy.json");

        assertTrue("Blue dragons strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("blue-dragons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Blue_dragons", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-crossbow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-lance".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "taverley-baby-cannon");
        assertContains(methodIds(strategy), "adult-blue-dragons");
        assertContains(methodIds(strategy), "brutal-blue-dragons");
        assertContains(methodIds(strategy), "konar-location-handling");
        assertContains(methodIds(strategy), "vorkath-profit");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "taverley-baby-cannon".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Taverley baby cannon method"))
            .getSteps().stream().anyMatch(step -> step.contains("main dungeon") && step.contains("baby blue dragons")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "konar-location-handling".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Konar method"))
            .getSteps().stream().anyMatch(step -> step.contains("Ogre Enclave") && step.contains("Catacombs")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "vorkath-profit".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Vorkath method"))
            .getNotes().stream().anyMatch(note -> note.contains("Vorkath/Strategies")));

        assertContains(styleIds(strategy), "ranged-dragonbane");
        assertContains(styleIds(strategy), "melee-dragonbane");
        assertContains(styleIds(strategy), "magic-dragonbane");
    }

    @Test
    public void generatedRuntimeDataCarriesBlueDragonSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Blue dragons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Blue dragons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Taverley Dungeon".equals(location.getName())
            && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Ungael".equals(location.getName())
            && !location.isCannon()));

        MonsterVariant blueDragon = task.getVariants().stream()
            .filter(variant -> "Blue dragon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Blue dragon"));
        MonsterVariant vorkath = task.getVariants().stream()
            .filter(variant -> "Vorkath".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Vorkath"));

        assertNotNull(blueDragon.getStrategy());
        assertEquals(CombatStyle.RANGED, blueDragon.getStrategy().getPrimaryStyle());
        assertEquals("Dragon hunter crossbow", blueDragon.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(blueDragon.getStrategy().getNote().contains("dragonfire protection"));
        assertNotNull(vorkath.getStrategy());
        assertEquals("Vorkath should retain dedicated boss strategy", "https://oldschool.runescape.wiki/w/Vorkath/Strategies",
            vorkath.getStrategy().getSourceUrl());
        assertTrue(task.isDragon());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("dragon-nest", "Dragon Nest", false, true, true, false, "Tal Teklan");
        assertLocationSource("heroes-guild-basement", "Heroes' Guild basement", false, true, true, false,
            "Heroes' Quest");
        assertLocationSource("isle-of-souls-dungeon", "Isle of Souls Dungeon", false, true, true, false,
            "Blue dragons");
        assertLocationSource("ruins-of-tapoyauik", "Ruins of Tapoyauik", false, false, true, false,
            "The Heart of Darkness");
        assertLocationSource("ungael", "Ungael", false, false, false, false, "Vorkath");
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

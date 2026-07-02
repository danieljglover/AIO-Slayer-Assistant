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

public class BlackDragonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/black-dragons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(271133), task.getWikiPageId());
        assertEquals(Integer.valueOf(80), task.getCombatLevel());
        assertEquals(207, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Dragon Slayer I"));

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {8, 16}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {10, 15}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {40, 60}, task.getExtendedAmount().get("krystilia"));
        assertArrayEquals(new int[] {40, 60}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {40, 60}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {40, 60}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "fire-and-darkness");
        assertContains(locationIds(task), "taverley-dungeon");
        assertContains(locationIds(task), "taverley-dungeon-upper");
        assertContains(locationIds(task), "mynydd");
        assertContains(locationIds(task), "charred-dungeon");
        assertContains(locationIds(task), "lava-maze-dungeon");
        assertContains(locationIds(task), "evil-chickens-lair");
        assertContains(locationIds(task), "wilderness-slayer-cave");
        assertContains(locationIds(task), "corsair-cove-dungeon");
        assertContains(locationIds(task), "king-black-dragon-lair");
        assertContains(locationIds(task), "catacombs-of-kourend");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("partial completion of Dragon Slayer I")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("melee") && note.contains("dragonfire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("anti-dragon shield") && note.contains("extended antifire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("baby black dragons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("brutal black dragons") && note.contains("77 Slayer")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("flinching")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Mythical cape")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("raw chicken") && note.contains("Chicken Shrine")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("82 Agility") && note.contains("knife")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("royal seed pod")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("King Black Dragon") && note.contains("unwilling to lose")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/black-dragons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, variants.size());
        assertEquals(80.0, variants.get("baby-black-dragon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(199.5, variants.get("black-dragon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(262.0, variants.get("black-dragon-level-247").getSlayerXp().doubleValue(), 0.0);
        assertEquals(258.0, variants.get("black-dragons-king-black-dragon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(346.5, variants.get("brutal-black-dragon").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("baby-black-dragon").getNotes().stream()
            .anyMatch(note -> note.contains("does not use dragonfire")));
        assertTrue(variants.get("brutal-black-dragon").getNotes().stream()
            .anyMatch(note -> note.contains("Requires level 77 Slayer")));
        assertTrue(variants.get("black-dragons-king-black-dragon").getNotes().stream()
            .anyMatch(note -> note.contains("arena itself is NOT considered in the Wilderness")));

        assertEquals(10, locations.size());
        assertLocation(locations, "taverley-dungeon", 2, false, true, true, "2 Adult dragons");
        assertTrue(locations.get("taverley-dungeon-upper").getNotes().stream()
            .anyMatch(note -> note.contains("12 Adult dragons") && note.contains("5 Baby dragons")));
        assertLocation(locations, "mynydd", 1, false, true, true, "Song of the Elves");
        assertTrue(locations.get("charred-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("5 Adult dragons") && note.contains("7 Baby dragons")));
        assertLocation(locations, "lava-maze-dungeon", 2, false, true, true, "poison spider");
        assertTrue(locations.get("evil-chickens-lair").getNotes().stream()
            .anyMatch(note -> note.contains("4 Adult dragons") && note.contains("1 Baby dragon")));
        assertLocation(locations, "wilderness-slayer-cave", 3, true, true, true, "level 247");
        assertTrue(locations.get("corsair-cove-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("2 Adult dragons") && note.contains("1 Baby dragon")));
        assertEquals(Integer.valueOf(1), locations.get("king-black-dragon-lair").getAmount());
        assertEquals(Boolean.TRUE, locations.get("king-black-dragon-lair").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("king-black-dragon-lair").getCannonable());
        assertEquals(Boolean.FALSE, locations.get("king-black-dragon-lair").getSafespottable());
        assertTrue(locations.get("king-black-dragon-lair").getNotes().stream()
            .anyMatch(note -> note.contains("arena itself is NOT considered part of the Wilderness")));
        assertLocation(locations, "catacombs-of-kourend", 4, false, false, false, "77 Slayer");
    }

    @Test
    public void strategySourceContainsAllTaskPageStrategyOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/black-dragons/strategy.json");

        assertTrue("Black dragons strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("black-dragons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Black_dragons", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-crossbow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-lance".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "baby-black-dragons");
        assertContains(methodIds(strategy), "normal-black-dragons");
        assertContains(methodIds(strategy), "brutal-black-dragons");
        assertContains(methodIds(strategy), "wilderness");
        assertContains(methodIds(strategy), "king-black-dragon");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "brutal-black-dragons".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing brutal method"))
            .getSteps().stream().anyMatch(step -> step.contains("Protect from Magic")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing wilderness method"))
            .getNotes().stream().anyMatch(note -> note.contains("royal seed pod")));

        assertContains(styleIds(strategy), "ranged-dragonbane");
        assertContains(styleIds(strategy), "melee-dragonbane");
        assertContains(styleIds(strategy), "magic-dragonbane");
    }

    @Test
    public void generatedRuntimeDataCarriesBlackDragonSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Black dragons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Black dragons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness Slayer Cave".equals(location.getName())
            && location.isMulti() && location.isCannon() && location.isWilderness()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "King Black Dragon Lair".equals(location.getName())
            && location.isMulti() && !location.isCannon() && !location.isWilderness()));

        MonsterVariant blackDragon = task.getVariants().stream()
            .filter(variant -> "Black dragon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Black dragon"));

        assertNotNull(blackDragon.getStrategy());
        assertEquals(CombatStyle.RANGED, blackDragon.getStrategy().getPrimaryStyle());
        assertEquals("Dragon hunter crossbow", blackDragon.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(blackDragon.getStrategy().getNote().contains("anti-dragon"));
        assertTrue(task.isDragon());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("taverley-dungeon-upper", "Taverley Dungeon (upper level)", false, true, true, false,
            "task-only");
        assertLocationSource("mynydd", "Mynydd", false, true, true, false, "Song of the Elves");
        assertLocationSource("lava-maze-dungeon", "Lava Maze Dungeon", false, true, true, true, "82 Agility");
        assertLocationSource("evil-chickens-lair", "Evil Chicken's Lair", false, true, true, false, "raw chicken");
        assertLocationSource("corsair-cove-dungeon", "Corsair Cove Dungeon", false, true, true, false,
            "Dragon Slayer II");
        assertLocationSource("king-black-dragon-lair", "King Black Dragon Lair", true, false, false, false,
            "NOT considered part of the Wilderness");
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

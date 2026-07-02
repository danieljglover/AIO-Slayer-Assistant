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

public class BlackDemonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/black-demons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298036), task.getWikiPageId());
        assertEquals(Integer.valueOf(80), task.getCombatLevel());
        assertEquals(206, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {100, 150}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("krystilia"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "it-s-dark-in-here");
        assertContains(locationIds(task), "brimhaven-dungeon");
        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "charred-dungeon");
        assertContains(locationIds(task), "chasm-of-fire-bottom");
        assertContains(locationIds(task), "crash-site-cavern");
        assertContains(locationIds(task), "edgeville-dungeon-wilderness");
        assertContains(locationIds(task), "taverley-dungeon");
        assertContains(locationIds(task), "wilderness-slayer-cave");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("80 combat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("safespot") && note.contains("Ranged")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("demonbane weapons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("water spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Taverley Dungeon") && note.contains("Chasm of Fire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("A Kingdom Divided") && note.contains("Yama")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Catacombs") && note.contains("higher Defence")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Edgeville Dungeon") && note.contains("Wilderness Slayer Cave")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Demonic gorillas") && note.contains("Monkey Madness II")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia") && note.contains("Konar")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Nightmare Zone")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Kolodion") && note.contains("once")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/black-demons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(6, variants.size());
        assertTrue(variants.get("black-demon").getNotes().stream()
            .anyMatch(note -> note.contains("Combat level range: 172-188")));
        assertEquals(157.0, variants.get("black-demon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(408.5, variants.get("demonic-gorilla").getSlayerXp().doubleValue(), 0.0);
        assertEquals(161.0, variants.get("balfrug-kreeyath").getSlayerXp().doubleValue(), 0.0);
        assertEquals(618.5, variants.get("skotizo").getSlayerXp().doubleValue(), 0.0);
        assertEquals(376.0, variants.get("porazdir").getSlayerXp().doubleValue(), 0.0);
        assertEquals(107.0, variants.get("kolodion-final-form").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("demonic-gorilla").getNotes().stream()
            .anyMatch(note -> note.contains("Zenyte shard")));
        assertTrue(variants.get("skotizo").getNotes().stream()
            .anyMatch(note -> note.contains("dark totem")));
        assertTrue(variants.get("kolodion-final-form").getNotes().stream()
            .anyMatch(note -> note.contains("fourth form")));

        assertEquals(8, locations.size());
        assertLocation(locations, "brimhaven-dungeon", 4, false, true, true, "agility pipe");
        assertLocation(locations, "catacombs-of-kourend", 4, true, false, true, "Ancient shards");
        assertLocation(locations, "charred-dungeon", 3, true, true, true, "60 Sailing");
        assertLocation(locations, "chasm-of-fire-bottom", 17, false, true, true, "Voice of Yama");
        assertLocation(locations, "crash-site-cavern", 27, true, false, false, "Monkey Madness II");
        assertLocation(locations, "edgeville-dungeon-wilderness", 3, false, true, true, "level 5 Wilderness");
        assertLocation(locations, "taverley-dungeon", 24, false, true, true, "Dusty Key");
        assertLocation(locations, "wilderness-slayer-cave", 4, true, true, true, "player killing");
    }

    @Test
    public void strategySourceContainsAllTaskPageStrategyOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/black-demons/strategy.json");

        assertTrue("Black demons strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("black-demons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Black_demons", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("emberlight"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "scorching-bow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-seas".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "taverley-cannon");
        assertContains(methodIds(strategy), "chasm-of-fire");
        assertContains(methodIds(strategy), "catacombs-of-kourend");
        assertContains(methodIds(strategy), "wilderness");
        assertContains(methodIds(strategy), "demonic-gorillas");
        assertContains(methodIds(strategy), "boss-and-miniquest-variants");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "demonic-gorillas".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing demonic gorilla method"))
            .getSteps().stream().anyMatch(step -> step.contains("two combat styles")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing wilderness method"))
            .getNotes().stream().anyMatch(note -> note.contains("under 30 Wilderness")));

        assertContains(styleIds(strategy), "melee-demonbane");
        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "magic-water");
    }

    @Test
    public void generatedRuntimeDataCarriesBlackDemonSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Black demons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Black demons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Chasm of Fire (Bottom level)".equals(location.getName())
            && !location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness Slayer Cave".equals(location.getName())
            && location.isMulti() && location.isCannon() && location.isWilderness()));

        MonsterVariant blackDemon = task.getVariants().stream()
            .filter(variant -> "Black demon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Black demon"));

        assertNotNull(blackDemon.getStrategy());
        assertEquals(CombatStyle.MELEE, blackDemon.getStrategy().getPrimaryStyle());
        assertEquals("Emberlight", blackDemon.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(blackDemon.getStrategy().getNote().contains("Taverley Dungeon"));
        assertTrue(task.isDemon());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("brimhaven-dungeon", "Brimhaven Dungeon", false, true, true, false, "agility pipe");
        assertLocationSource("charred-dungeon", "Charred Dungeon", true, true, true, false, "60 Sailing");
        assertLocationSource("chasm-of-fire-bottom", "Chasm of Fire (Bottom level)", false, true, true, false, "Voice of Yama");
        assertLocationSource("crash-site-cavern", "Crash Site Cavern", true, false, false, false, "Monkey Madness II");
        assertLocationSource("edgeville-dungeon-wilderness", "Edgeville Dungeon (Wilderness)", false, true, true, true,
            "level 5 Wilderness");
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

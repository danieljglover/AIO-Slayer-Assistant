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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DustDevilsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentRequirementsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dust-devils.json"), SourceTask.class);

        assertEquals(Integer.valueOf(267355), task.getWikiPageId());
        assertEquals(Integer.valueOf(70), task.getCombatLevel());
        assertEquals(216, task.getSlayerTargetId());
        assertEquals(65, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().stream().anyMatch(req -> req.contains("Desert Treasure I")));
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
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

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("started Desert Treasure I")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("ranged defence")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("facemask") && note.contains("stat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("burst") && note.contains("barrage")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Choke devil") && note.contains("Bigger and Badder")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("35%") && note.contains("air")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Hard Desert Diary")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dust-devils.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertEquals(105.0, variants.get("dust-devil").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("dust-devil").getNotes().stream()
            .anyMatch(note -> note.contains("Catacombs") && note.contains("130")));
        assertTrue(variants.get("dust-devil").getNotes().stream()
            .anyMatch(note -> note.contains("35%") && note.contains("air")));
        assertEquals(3000.0, variants.get("choke-devil").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("choke-devil").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));
        assertTrue(variants.get("choke-devil").getNotes().stream()
            .anyMatch(note -> note.contains("25%") && note.contains("air")));

        assertEquals(3, locations.size());
        assertLocation(locations, "catacombs-of-kourend", 13, true, false, true, "Catacombs drop table");
        assertLocation(locations, "wilderness-slayer-cave-dust-devils", 9, true, true, true, "level 25-28 Wilderness");
        assertMaybeCannonLocation(locations, "smoke-dungeon-dust-devils", 44, true, true, "task-only");
    }

    @Test
    public void strategySourceContainsAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/dust-devils/strategy.json");

        assertTrue("Dust devils strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("dust-devils", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dust_devils", strategy.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("kodai-wand"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("nightmare-staff"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("ancient-sceptre"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dinh-s-bulwark".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "venator-bow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getNote().contains("burst"));
        assertTrue(strategy.getPlugin().getNote().contains("facemask"));

        assertContains(methodIds(strategy), "catacombs-burst-barrage");
        assertContains(methodIds(strategy), "smoke-dungeon");
        assertContains(methodIds(strategy), "wilderness-slayer-cave");
        assertContains(methodIds(strategy), "luring");
        assertContains(methodIds(strategy), "choke-devil");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "luring".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing luring method"))
            .getRequiredOrKeyItems().stream().anyMatch(item -> item.contains("Dinh")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness-slayer-cave".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing wilderness method"))
            .getRisks().stream().anyMatch(risk -> risk.contains("player killers")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "inventory-and-loot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing inventory method"))
            .getInventory().stream().anyMatch(item -> item.contains("bracelet of slaughter")));

        assertContains(styleIds(strategy), "bursting-barraging");
        assertContains(styleIds(strategy), "luring-weapons");
        assertContains(styleIds(strategy), "wilderness-bursting");
    }

    @Test
    public void generatedRuntimeDataCarriesDustDevilStrategiesAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Dust devils".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Dust devils"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Catacombs of Kourend".equals(location.getName())
            && location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Smoke Dungeon".equals(location.getName())
            && location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness Slayer Cave".equals(location.getName())
            && location.isMulti() && location.isCannon()));

        MonsterVariant dustDevil = variantNamed(task, "Dust devil");
        MonsterVariant chokeDevil = variantNamed(task, "Choke devil");

        assertNotNull(dustDevil.getStrategy());
        assertEquals(CombatStyle.MAGIC, dustDevil.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dust_devils",
            dustDevil.getStrategy().getSourceUrl());
        assertNotNull(chokeDevil.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dust_devils",
            chokeDevil.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchDustDevilTaskPage() throws IOException
    {
        assertLocationSource("smoke-dungeon-dust-devils", "Smoke Dungeon", true, true, true, false,
            "Dusty Aliv");
        assertLocationSource("wilderness-slayer-cave-dust-devils", "Wilderness Slayer Cave", true, true, true, true,
            "level 25-28 Wilderness");
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

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        int amount, boolean multi, boolean cannon, boolean safespot, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertEquals(Integer.valueOf(amount), location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertMaybeCannonLocation(Map<String, SourceTaskLocationComparison> locations,
        String locationId, int amount, boolean multi, boolean safespot, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertEquals(Integer.valueOf(amount), location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertNull(location.getCannonable());
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

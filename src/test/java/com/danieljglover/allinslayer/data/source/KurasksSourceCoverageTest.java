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

public class KurasksSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlocksAndLeafyMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/kurask.json"), SourceTask.class);

        assertEquals(Integer.valueOf(634049), task.getWikiPageId());
        assertEquals(Integer.valueOf(65), task.getCombatLevel());
        assertEquals(226, task.getSlayerTargetId());
        assertEquals(70, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertTrue(task.getRequiredItemName().contains("leaf-bladed"));
        assertTrue(task.getRequiredItemName().contains("broad"));
        assertTrue(task.getRequiredItemName().contains("Magic Dart"));

        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertFalse(task.getMasterIds().contains("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertTrue(task.getExtendedAmount() == null || task.getExtendedAmount().isEmpty());

        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(locationIds(task), "fremennik-slayer-dungeon-kurasks");
        assertContains(locationIds(task), "iorwerth-dungeon-kurasks");
        assertContains(locationIds(task), "kurask-lair");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("70 Slayer") && note.contains("65 combat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Song of the Elves")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Laguna Aurorae") && note.contains("58 Sailing")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("task-only area")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("two 3D models") && note.contains("cosmetic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("crush attacks") && note.contains("11")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee") && note.contains("negate")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("75 Defence") && note.contains("Bones to Peaches")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("leafy") && note.contains("leaf-bladed")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("broad") && note.contains("Magic Dart")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Cannons") && note.contains("Thralls")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Saradomin godsword") && note.contains("heal")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Guthan") && note.contains("not activate")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Herb sack") && note.contains("three herbs")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Iorwerth Dungeon") && note.contains("Crystal shards")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/kurask.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertVariant(variants, "kurask", 106, 97.0, "Standard kurasks");
        assertVariant(variants, "king-kurask-superior", 295, 2764.4, "Bigger and Badder");

        assertEquals(3, locations.size());
        assertLocation(locations, "fremennik-slayer-dungeon-kurasks", 14, false, false, true, "task-only section");
        assertLocation(locations, "iorwerth-dungeon-kurasks", 14, false, false, true, "crystal shard");
        assertLocation(locations, "kurask-lair", 8, false, false, true, "58 Sailing");
    }

    @Test
    public void variantSourcesUseMonsterPageStatsAndStrategy() throws IOException
    {
        SourceMonsterVariant kurask = read(Paths.get("src/main/data/slayer/monsters/kurask/kurask-lvl106.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant king = read(Paths.get("src/main/data/slayer/monsters/kurask/king-kurask-superior-lvl295.json"),
            SourceMonsterVariant.class);

        assertEquals("kurask", kurask.getVariantId());
        assertEquals(Integer.valueOf(106), kurask.getCombatLevel());
        assertEquals(CombatStyle.MELEE, kurask.getWeakness().getStyle());
        assertEquals(105, kurask.getMonsterDefence().getDefenceLevel());
        assertEquals(0, kurask.getMonsterDefence().getStab());
        assertEquals(20, kurask.getMonsterDefence().getSlash());
        assertEquals(20, kurask.getMonsterDefence().getCrush());
        assertEquals(0, kurask.getMonsterDefence().getMagic());
        assertEquals(0, kurask.getMonsterDefence().getRange());
        assertTrue(kurask.getRequirement().contains("leaf-bladed"));
        assertTrue(kurask.getRequirement().contains("Magic Dart"));
        assertEquals("kurask", kurask.getStrategyId());

        assertEquals("king-kurask-superior", king.getVariantId());
        assertEquals(Integer.valueOf(295), king.getCombatLevel());
        assertEquals(250, king.getMonsterDefence().getDefenceLevel());
        assertEquals(0, king.getMonsterDefence().getStab());
        assertEquals(50, king.getMonsterDefence().getSlash());
        assertEquals(50, king.getMonsterDefence().getCrush());
        assertEquals(0, king.getMonsterDefence().getMagic());
        assertEquals(0, king.getMonsterDefence().getRange());
        assertTrue(king.getRequirement().contains("Bigger and Badder"));
        assertEquals("kurask", king.getStrategyId());
    }

    @Test
    public void strategySourceContainsTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/kurask/strategy.json");

        assertTrue("Kurask strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("kurask", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Kurasks", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("leaf-bladed-battleaxe"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "rune-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "magic-shortbow-i".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getNote().contains("leaf-bladed"));
        assertTrue(strategy.getPlugin().getNote().contains("Magic Dart"));

        assertContains(methodIds(strategy), "damage-restrictions");
        assertContains(methodIds(strategy), "melee-leaf-bladed");
        assertContains(methodIds(strategy), "ranged-broad-ammunition");
        assertContains(methodIds(strategy), "bones-to-peaches");
        assertContains(methodIds(strategy), "king-kurask-superior");
        assertContains(methodIds(strategy), "location-choice");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "damage-restrictions".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing restriction method"))
            .getSteps().stream().anyMatch(step -> step.contains("Cannons") && step.contains("Thralls")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "bones-to-peaches".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing bones method"))
            .getSteps().stream().anyMatch(step -> step.contains("75 Defence")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "king-kurask-superior".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing superior method"))
            .getRequiredOrKeyItems().stream().anyMatch(item -> item.contains("Prayer potion")));

        assertContains(styleIds(strategy), "melee-leaf-bladed");
        assertContains(styleIds(strategy), "ranged-broad");
        assertContains(styleIds(strategy), "magic-dart");
    }

    @Test
    public void generatedRuntimeDataCarriesKuraskSourcesAndStrategies() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Kurask".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Kurask"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(2, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Fremennik Slayer Dungeon".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Iorwerth Dungeon".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Kurask Lair".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));

        MonsterVariant kurask = variantNamed(task, "Kurask");
        MonsterVariant king = variantNamed(task, "King kurask (superior)");

        assertNotNull(kurask.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Kurasks", kurask.getStrategy().getSourceUrl());
        assertNotNull(king.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Kurasks", king.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("fremennik-slayer-dungeon-kurasks", "Fremennik Slayer Dungeon", false, false, true,
            false, "task-only section");
        assertLocationSource("iorwerth-dungeon-kurasks", "Iorwerth Dungeon", false, false, true, false,
            "crystal shard");
        assertLocationSource("kurask-lair", "Kurask Lair", false, false, true, false, "58 Sailing");
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

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
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

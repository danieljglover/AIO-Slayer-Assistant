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

public class RedDragonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndDragonfireMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/red-dragons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(297868), task.getWikiPageId());
        assertNull("wiki task infobox combat requirement is None", task.getCombatLevel());
        assertEquals(231, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Dragon Slayer I (partial completion)"));
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("water", task.getWeakness().getElement());
        assertTrue(task.isDragon());

        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {30, 80}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {30, 65}, task.getAmountByMaster().get("duradel"));
        assertContains(unlockIds(task), "seeing-red");

        assertContains(locationIds(task), "brimhaven-dungeon-red-dragons");
        assertContains(locationIds(task), "charred-dungeon-red-dragons");
        assertContains(locationIds(task), "corsair-cove-dungeon-red-dragons");
        assertContains(locationIds(task), "forthos-dungeon-red-dragons");
        assertContains(locationIds(task), "brimhaven-dungeon-baby-red-dragons");
        assertContains(locationIds(task), "corsair-cove-dungeon-baby-red-dragons");
        assertContains(locationIds(task), "forthos-dungeon-baby-red-dragons");
        assertContains(locationIds(task), "catacombs-of-kourend-brutal-red-dragons");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Seeing red")
            && note.contains("50")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("partial completion")
            && note.contains("Dragon Slayer I")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Adult")
            && note.contains("brutal") && note.contains("dragonfire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonfire protection")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Elite Karamja Diary")
            && note.contains("noted red dragonhide")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dragon Slayer II")
            && note.contains("close to a bank")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Brutal red dragons")
            && note.contains("draconic visage")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("slow compared to normal or baby red dragons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("50%")
            && note.contains("water")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/red-dragons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(3, variants.size());
        assertVariant(variants, "red-dragon", 152, 143.4, "Brimhaven Dungeon requires at least 30 Agility");
        assertVariant(variants, "baby-red-dragon", 48, 50.0, "does not attack with dragonfire");
        assertVariant(variants, "brutal-red-dragon", 289, 306.2, "long-range dragonfire");

        assertEquals(8, locations.size());
        assertLocation(locations, "brimhaven-dungeon-red-dragons", 16, false, true, true,
            "Karamja Elite Diary");
        assertLocation(locations, "charred-dungeon-red-dragons", 4, true, true, true, "60 Sailing");
        assertLocation(locations, "corsair-cove-dungeon-red-dragons", 3, false, true, true,
            "Dragon Slayer II");
        assertLocation(locations, "forthos-dungeon-red-dragons", 6, false, false, true,
            "Sacred Bone Burner");
        assertLocation(locations, "brimhaven-dungeon-baby-red-dragons", 12, false, true, false,
            "Baby red dragons");
        assertLocation(locations, "corsair-cove-dungeon-baby-red-dragons", 2, false, true, true,
            "Dragon Slayer II");
        assertLocation(locations, "forthos-dungeon-baby-red-dragons", 5, false, false, false,
            "Forthos Dungeon");
        assertLocation(locations, "catacombs-of-kourend-brutal-red-dragons", 3, false, false, false,
            "Brutal red dragons");
    }

    @Test
    public void variantSourcesUseMonsterPageStatsAndStrategyId() throws IOException
    {
        SourceMonsterVariant red = read(Paths.get("src/main/data/slayer/monsters/red-dragons/red-dragon-lvl152.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant baby = read(Paths.get("src/main/data/slayer/monsters/red-dragons/baby-red-dragon-lvl48.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant brutal = read(Paths.get("src/main/data/slayer/monsters/red-dragons/brutal-red-dragon-lvl289.json"),
            SourceMonsterVariant.class);

        assertDragonVariant(red, "red-dragon", 152, 130, 0, 70, 70, 60, 50, "brimhaven-dungeon-red-dragons");
        assertDragonVariant(baby, "baby-red-dragon", 48, 40, 0, 50, 50, 40, 30,
            "brimhaven-dungeon-baby-red-dragons");
        assertDragonVariant(brutal, "brutal-red-dragon", 289, 198, 0, 70, 70, 60, 50,
            "catacombs-of-kourend-brutal-red-dragons");
    }

    @Test
    public void strategyJsonCoversTaskPageMethodsAndStyleOptions() throws IOException
    {
        assertFalse(Files.exists(Paths.get("src/main/data/slayer/strategies/red-dragons.md")));
        SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies/red-dragons/strategy.json"),
            SourceStrategy.class);

        assertEquals("red-dragons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Red_dragons", strategy.getSourceUrl());
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "red-dragon");
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "baby-red-dragon");
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "brutal-red-dragon");
        assertNotNull(strategy.getPlugin());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-crossbow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-lance".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("Seeing red"));
        assertTrue(strategy.getPlugin().getNote().contains("dragonfire protection"));

        assertContains(methodIds(strategy), "unlock-and-protection");
        assertContains(methodIds(strategy), "brimhaven-adult-cannon");
        assertContains(methodIds(strategy), "baby-red-dragons");
        assertContains(methodIds(strategy), "forthos-adult-safespot");
        assertContains(methodIds(strategy), "corsair-cove-bank");
        assertContains(methodIds(strategy), "brutal-red-dragons");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "unlock-and-protection".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing protection method"))
            .getSteps().stream().anyMatch(step -> step.contains("Seeing red")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "brutal-red-dragons".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing brutal method"))
            .getSteps().stream().anyMatch(step -> step.contains("Protect from Magic") && step.contains("antifire")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "forthos-adult-safespot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Forthos method"))
            .getSteps().stream().anyMatch(step -> step.contains("Sacred Bone Burner")));

        assertContains(styleIds(strategy), "ranged-dragonbane");
        assertContains(styleIds(strategy), "melee-dragonbane");
        assertContains(styleIds(strategy), "magic-water");
    }

    @Test
    public void generatedRuntimeDataCarriesRedDragonSourcesAndStrategies()
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Red dragons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Red dragons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.isDragon());
        assertEquals(3, task.getVariants().size());
        assertEquals(8, task.getLocations().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Brimhaven Dungeon (adult red dragons)".equals(location.getName())
            && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Catacombs of Kourend (brutal red dragons)".equals(location.getName())
            && !location.isCannon()));

        MonsterVariant red = variantNamed(task, "Red dragon");
        MonsterVariant baby = variantNamed(task, "Baby red dragon");
        MonsterVariant brutal = variantNamed(task, "Brutal red dragon");

        assertNotNull(red.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Red_dragons", red.getStrategy().getSourceUrl());
        assertEquals(CombatStyle.RANGED, red.getStrategy().getPrimaryStyle());
        assertNotNull(baby.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Red_dragons", baby.getStrategy().getSourceUrl());
        assertNotNull(brutal.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Red_dragons", brutal.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("brimhaven-dungeon-red-dragons", "Brimhaven Dungeon (adult red dragons)",
            false, true, true, false, "16 Red dragons");
        assertLocationSource("charred-dungeon-red-dragons", "Charred Dungeon (red dragons)", true, true,
            true, false, "60 Sailing");
        assertLocationSource("corsair-cove-dungeon-red-dragons", "Corsair Cove Dungeon (adult red dragons)",
            false, true, true, false, "Dragon Slayer II");
        assertLocationSource("forthos-dungeon-red-dragons", "Forthos Dungeon (adult red dragons)", false,
            false, true, false, "Sacred Bone Burner");
        assertLocationSource("brimhaven-dungeon-baby-red-dragons", "Brimhaven Dungeon (baby red dragons)",
            false, true, false, false, "12 Baby red dragons");
        assertLocationSource("corsair-cove-dungeon-baby-red-dragons", "Corsair Cove Dungeon (baby red dragons)",
            false, true, true, false, "Dragon Slayer II");
        assertLocationSource("forthos-dungeon-baby-red-dragons", "Forthos Dungeon (baby red dragons)", false,
            false, false, false, "5 Baby red dragons");
        assertLocationSource("catacombs-of-kourend-brutal-red-dragons",
            "Catacombs of Kourend (brutal red dragons)", false, false, false, false, "3 Brutal red dragons");
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

    private static void assertDragonVariant(SourceMonsterVariant variant, String variantId, Integer combatLevel,
        int defenceLevel, int stab, int slash, int crush, int magic, int range, String locationId)
    {
        assertEquals(variantId, variant.getVariantId());
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(CombatStyle.RANGED, variant.getWeakness().getStyle());
        assertEquals("water", variant.getWeakness().getElement());
        assertEquals(defenceLevel, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(stab, variant.getMonsterDefence().getStab());
        assertEquals(slash, variant.getMonsterDefence().getSlash());
        assertEquals(crush, variant.getMonsterDefence().getCrush());
        assertEquals(magic, variant.getMonsterDefence().getMagic());
        assertEquals(range, variant.getMonsterDefence().getRange());
        assertEquals(locationId, variant.getLocationId());
        assertEquals("red-dragons", variant.getStrategyId());
        assertTrue(variant.isDragon());
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safespot, boolean wilderness, String accessNote)
        throws IOException
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
        assertTrue("missing " + expected + " from " + values, values.contains(expected));
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

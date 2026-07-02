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

public class WaterfiendsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/waterfiends.json"), SourceTask.class);

        assertEquals(Integer.valueOf(362720), task.getWikiPageId());
        assertEquals(239, task.getSlayerTargetId());
        assertEquals(Integer.valueOf(75), task.getCombatLevel());
        assertEquals(1, task.getSlayerLevel());
        assertContains(questReqs(task), "Ancient Cavern access");
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());
        assertTrue(task.isDemon());

        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "duradel");
        assertFalse(masterIds(task).contains("nieve"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));

        assertContains(locationIds(task), "ancient-cavern-waterfiends");
        assertContains(locationIds(task), "kraken-cove-waterfiends");
        assertContains(locationIds(task), "iorwerth-dungeon-waterfiends");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Magic")
            && note.contains("magical Ranged")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Magic Defence")
            && note.contains("Dragonhide")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Missiles")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Eclipse Moon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("100% elemental weakness")
            && note.contains("earth")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("skip")
            && note.contains("weight value of 2")));
    }

    @Test
    public void taskSourceContainsVariantRowAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/waterfiends.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(1, variants.size());
        assertVariant(variants, "waterfiend", 115, 128, "Ancient Cavern");

        assertEquals(3, locations.size());
        assertLocation(locations, "ancient-cavern-waterfiends", 16, false, false, true, "aggression timer");
        assertLocation(locations, "kraken-cove-waterfiends", 15, false, false, false, "Task Only");
        assertLocation(locations, "iorwerth-dungeon-waterfiends", 20, false, true, false, "crystal shard");
    }

    @Test
    public void variantSourceUsesCurrentMonsterPageStatsAndStrategyId() throws IOException
    {
        SourceMonsterVariant waterfiend = read(Paths.get(
            "src/main/data/slayer/monsters/waterfiends/waterfiend-lvl115.json"), SourceMonsterVariant.class);

        assertEquals("waterfiend", waterfiend.getVariantId());
        assertEquals(Integer.valueOf(115), waterfiend.getCombatLevel());
        assertEquals("earth", waterfiend.getWeakness().getElement());
        assertTrue(waterfiend.isDemon());
        assertDefence(waterfiend, 128, 100, 100, 10, 100, 20);
        assertEquals("waterfiends", waterfiend.getStrategyId());
    }

    @Test
    public void strategyJsonCoversAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path strategyJson = Paths.get("src/main/data/slayer/strategies/waterfiends/strategy.json");

        assertTrue("Waterfiends strategy JSON missing", Files.exists(strategyJson));
        SourceStrategy strategy = read(strategyJson, SourceStrategy.class);

        assertEquals("waterfiends", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Waterfiends", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-bludgeon"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("sarachnis-cudgel"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "eye-of-ayak".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "hunters-sunlight-crossbow".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));

        assertContains(methodIds(strategy), "requirements-and-access");
        assertContains(methodIds(strategy), "crush-melee-magic-defence");
        assertContains(methodIds(strategy), "earth-magic");
        assertContains(methodIds(strategy), "ranged-defence-option");
        assertContains(methodIds(strategy), "ancient-cavern-safespot");
        assertContains(methodIds(strategy), "kraken-cove-task-only");
        assertContains(methodIds(strategy), "iorwerth-dungeon-cannon");
        assertContains(methodIds(strategy), "skip-guidance");
        assertContains(styleIds(strategy), "crush-melee");
        assertContains(styleIds(strategy), "earth-magic");
        assertContains(styleIds(strategy), "ranged-defence");

        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("100% earth")));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("10 min aggression")));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("crystal shards")));
    }

    @Test
    public void generatedRuntimeDataCarriesWaterfiendStrategyAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Waterfiends".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Waterfiends"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertFalse(task.getAssignedBy().contains("nieve"));
        assertEquals(1, task.getVariants().size());
        assertEquals(3, task.getLocations().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Ancient Cavern - Waterfiends"
            .equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Kraken Cove - Waterfiends"
            .equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Iorwerth Dungeon - Waterfiends"
            .equals(location.getName())));

        MonsterVariant waterfiend = variantNamed(task, "Waterfiend");

        assertNotNull(waterfiend.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Waterfiends",
            waterfiend.getStrategy().getSourceUrl());
        assertEquals(CombatStyle.MELEE, waterfiend.getWeakness().getStyle());
    }

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        Integer combatLevel, Integer slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(slayerXp.doubleValue(), variant.getSlayerXp().doubleValue(), 0.0);
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        Integer amount, boolean multicombat, boolean cannonable, boolean safespottable, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location " + locationId, location);
        assertEquals(amount, location.getAmount());
        assertEquals(Boolean.valueOf(multicombat), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannonable), location.getCannonable());
        assertEquals(Boolean.valueOf(safespottable), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertDefence(SourceMonsterVariant variant, int defenceLevel, int stab, int slash,
        int crush, int magic, int range)
    {
        assertEquals(defenceLevel, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(stab, variant.getMonsterDefence().getStab());
        assertEquals(slash, variant.getMonsterDefence().getSlash());
        assertEquals(crush, variant.getMonsterDefence().getCrush());
        assertEquals(magic, variant.getMonsterDefence().getMagic());
        assertEquals(range, variant.getMonsterDefence().getRange());
    }

    private static MonsterVariant variantNamed(TaskData task, String name)
    {
        return task.getVariants().stream()
            .filter(variant -> name.equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing variant: " + name));
    }

    private static Set<String> questReqs(SourceTask task)
    {
        return task.getQuestReqs().stream().collect(Collectors.toSet());
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
        return strategy.getStyleOptions().stream().map(SourceStrategyStyleOption::getStyleId).collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("missing " + expected + " in " + values, values.contains(expected));
    }
}

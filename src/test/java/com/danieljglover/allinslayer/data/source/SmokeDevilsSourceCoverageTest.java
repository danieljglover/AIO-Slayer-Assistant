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
import java.util.Arrays;
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

public class SmokeDevilsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/smoke-devils.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298012), task.getWikiPageId());
        assertEquals(Integer.valueOf(85), task.getCombatLevel());
        assertEquals(84, task.getSlayerTargetId());
        assertEquals(93, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());
        assertFalse(task.isDemon());

        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(locationIds(task), "smoke-devil-dungeon");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("active Slayer task")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("facemask")
            && note.contains("slayer helm") && note.contains("drains stats")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("magical ranged")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("barrage")
            && note.contains("cannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Combat Achievements")
            && note.contains("cannonballs")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Thermonuclear smoke devil")
            && note.contains("profit")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Nuclear smoke devil")
            && note.contains("imbued heart")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/smoke-devils.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(3, variants.size());
        assertVariant(variants, "smoke-devil", 160, 185.0, "Standard smoke devil");
        assertVariant(variants, "nuclear-smoke-devil", 280, 2400.0, "Bigger and Badder");
        assertVariant(variants, "smoke-devils-thermonuclear-smoke-devil", 301, 240.0,
            "boss monster found in its lair");

        assertEquals(1, locations.size());
        SourceTaskLocationComparison location = locations.get("smoke-devil-dungeon");
        assertNotNull(location);
        assertNull(location.getAmount());
        assertEquals(Boolean.TRUE, location.getMulticombat());
        assertNull(location.getCannonable());
        assertEquals(Boolean.FALSE, location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains("30 smoke devils")));
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains("6 in room with boss")));
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains("boss room")
            && note.contains("destroyed")));
    }

    @Test
    public void variantSourcesUseMonsterPageStatsAndStrategyIds() throws IOException
    {
        SourceMonsterVariant smokeDevil = read(Paths.get(
            "src/main/data/slayer/monsters/smoke-devils/smoke-devil-lvl160.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant nuclear = read(Paths.get(
            "src/main/data/slayer/monsters/smoke-devils/nuclear-smoke-devil-lvl280.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant thermy = read(Paths.get(
            "src/main/data/slayer/monsters/smoke-devils/smoke-devils-thermonuclear-smoke-devil-lvl301.json"),
            SourceMonsterVariant.class);

        assertEquals(Arrays.asList(498), smokeDevil.getNpcIds());
        assertEquals(Integer.valueOf(160), smokeDevil.getCombatLevel());
        assertEquals("air", smokeDevil.getWeakness().getElement());
        assertDefence(smokeDevil, 275, 0, 0, 0, 600, 44);
        assertEquals("smoke-devil", smokeDevil.getStrategyId());

        assertEquals(Arrays.asList(7406), nuclear.getNpcIds());
        assertEquals(Integer.valueOf(280), nuclear.getCombatLevel());
        assertEquals("air", nuclear.getWeakness().getElement());
        assertDefence(nuclear, 390, 0, 0, 0, 850, 80);
        assertEquals("nuclear-smoke-devil", nuclear.getStrategyId());

        assertEquals(Arrays.asList(499), thermy.getNpcIds());
        assertEquals(Integer.valueOf(301), thermy.getCombatLevel());
        assertEquals("air", thermy.getWeakness().getElement());
        assertDefence(thermy, 360, 11, 4, 9, 800, 900);
        assertTrue(thermy.isBoss());
        assertEquals("thermonuclear-smoke-devil", thermy.getStrategyId());
    }

    @Test
    public void strategyJsonCoversSmokeDevilStrategyPageMethodsAndStyleOptions() throws IOException
    {
        Path smokeJson = Paths.get("src/main/data/slayer/strategies/smoke-devil/strategy.json");
        Path nuclearJson = Paths.get("src/main/data/slayer/strategies/nuclear-smoke-devil/strategy.json");

        assertTrue("Smoke devil strategy JSON missing", Files.exists(smokeJson));
        assertTrue("Nuclear smoke devil strategy JSON missing", Files.exists(nuclearJson));
        assertFalse("Legacy Smoke devil Markdown strategy should be migrated to JSON",
            Files.exists(Paths.get("src/main/data/slayer/strategies/smoke-devil.md")));
        assertFalse("Legacy Nuclear smoke devil Markdown strategy should be migrated to JSON",
            Files.exists(Paths.get("src/main/data/slayer/strategies/nuclear-smoke-devil.md")));

        SourceStrategy strategy = read(smokeJson, SourceStrategy.class);
        SourceStrategy nuclear = read(nuclearJson, SourceStrategy.class);

        assertSmokeStrategy(strategy, "smoke-devil");
        assertSmokeStrategy(nuclear, "nuclear-smoke-devil");
        assertTrue(nuclear.getMethods().stream()
            .filter(method -> "superior-handling".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing superior method"))
            .getSteps().stream().anyMatch(step -> step.contains("Nuclear smoke devil")));
    }

    @Test
    public void generatedRuntimeDataCarriesSmokeDevilStrategiesAndLocation() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Smoke devils".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Smoke devils"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Smoke Devil Dungeon".equals(location.getName())
            && location.isMulti() && location.isCannon() && location.isBurst()));

        MonsterVariant smokeDevil = variantNamed(task, "Smoke devil");
        MonsterVariant nuclear = variantNamed(task, "Nuclear smoke devil");
        MonsterVariant thermy = variantNamed(task, "Thermonuclear smoke devil");

        assertNotNull(smokeDevil.getStrategy());
        assertEquals(CombatStyle.MAGIC, smokeDevil.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Smoke_devil/Strategies",
            smokeDevil.getStrategy().getSourceUrl());
        assertNotNull(nuclear.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Smoke_devil/Strategies",
            nuclear.getStrategy().getSourceUrl());
        assertNotNull(thermy.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil/Strategies",
            thermy.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourceMatchesSmokeDevilsTaskPage() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/smoke-devil-dungeon.json"),
            SourceLocation.class);

        assertEquals("smoke-devil-dungeon", location.getLocationId());
        assertEquals("Smoke Devil Dungeon", location.getName());
        assertTrue(location.isMulti());
        assertTrue(location.isCannon());
        assertTrue(location.isBurst());
        assertFalse(location.isKonarLockable());
        assertFalse(location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertTrue(location.getAccessNote().contains("south of Castle Wars"));
        assertTrue(location.getAccessNote().contains("task-only"));
        assertTrue(location.getAccessNote().contains("boss room"));
    }

    private static void assertSmokeStrategy(SourceStrategy strategy, String strategyId)
    {
        assertEquals(strategyId, strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Smoke_devil/Strategies", strategy.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("kodai-wand"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("volatile-nightmare-staff"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("nightmare-staff"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("ancient-sceptre"));
        assertTrue(strategy.getPlugin().getNote().contains("Ice Barrage"));
        assertTrue(strategy.getPlugin().getNote().contains("facemask"));

        assertContains(methodIds(strategy), "requirements-and-transportation");
        assertContains(methodIds(strategy), "smoke-protection");
        assertContains(methodIds(strategy), "cannon-barrage-grouping");
        assertContains(methodIds(strategy), "magic-equipment");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "cannon-barrage-grouping".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing grouping method"))
            .getSteps().stream().anyMatch(step -> step.contains("skeleton") && step.contains("west")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "magic-equipment".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing magic method"))
            .getSteps().stream().anyMatch(step -> step.contains("Kodai wand") && step.contains("Ancient Magicks")));

        assertContains(styleIds(strategy), "ice-barrage-cannon");
        assertContains(styleIds(strategy), "ice-burst-budget");
        assertContains(styleIds(strategy), "ranged-lure-ironman");
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

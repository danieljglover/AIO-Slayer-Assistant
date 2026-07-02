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

public class WarpedCreaturesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsUnlocksAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/warped-creatures.json"), SourceTask.class);

        assertEquals(Integer.valueOf(601420), task.getWikiPageId());
        assertEquals(238, task.getSlayerTargetId());
        assertEquals(56, task.getSlayerLevel());
        assertContains(questReqs(task), "The Path of Glouphrie");
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());
        assertEquals("Crystal chime", task.getRequiredItemName());

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {110, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertContains(unlockIds(task), "warped-reality");
        assertContains(unlockIds(task), "bigger-and-badder");

        assertContains(locationIds(task), "poison-waste-dungeon-tortoise-main");
        assertContains(locationIds(task), "poison-waste-dungeon-tortoise-lower");
        assertContains(locationIds(task), "poison-waste-dungeon-terrorbird-main");
        assertContains(locationIds(task), "poison-waste-dungeon-terrorbird-lower");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Warped Reality")
            && note.contains("60")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("crystal chime")
            && note.contains("invulnerable")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Warped Tortoise")
            && note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("earth spells")
            && note.contains("20%")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("earmuffs")
            && note.contains("halve")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Missiles")
            && note.contains("melee distance")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("cannon")
            && note.contains("1490,4263")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/warped-creatures.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, variants.size());
        assertVariant(variants, "warped-tortoise", 121, 200, "melee");
        assertVariant(variants, "warped-terrorbird-level-96", 96, 150, "ranged attacks");
        assertVariant(variants, "warped-terrorbird-level-138", 138, 200, "stronger");
        assertVariant(variants, "mutated-tortoise", 247, 4510, "Bigger and Badder");
        assertVariant(variants, "mutated-terrorbird", 178, 3200, "Bigger and Badder");

        assertEquals(4, locations.size());
        assertLocation(locations, "poison-waste-dungeon-tortoise-main", 14, true, true, true, "main level");
        assertLocation(locations, "poison-waste-dungeon-tortoise-lower", 3, true, true, true, "lower level");
        assertLocation(locations, "poison-waste-dungeon-terrorbird-main", 33, true, true, true, "Maybe");
        assertLocation(locations, "poison-waste-dungeon-terrorbird-lower", 3, true, true, true, "Maybe");
    }

    @Test
    public void variantSourcesUseCurrentMonsterPageStatsAndStrategyIds() throws IOException
    {
        SourceMonsterVariant tortoise = readVariant("warped-tortoise-lvl121.json");
        SourceMonsterVariant terrorbird96 = readVariant("warped-terrorbird-lvl96.json");
        SourceMonsterVariant terrorbird138 = readVariant("warped-terrorbird-lvl138.json");
        SourceMonsterVariant mutatedTortoise = readVariant("mutated-tortoise-lvl247.json");
        SourceMonsterVariant mutatedTerrorbird = readVariant("mutated-terrorbird-lvl178.json");

        assertEquals("earth", tortoise.getWeakness().getElement());
        assertDefence(tortoise, 78, 50, 50, 0, 0, 40);
        assertEquals("warped-creatures", tortoise.getStrategyId());
        assertDefence(terrorbird96, 20, 50, 50, 0, 0, 40);
        assertEquals("warped-creatures", terrorbird96.getStrategyId());
        assertDefence(terrorbird138, 60, 50, 50, 0, 0, 70);
        assertEquals("warped-creatures", terrorbird138.getStrategyId());
        assertEquals("earth", mutatedTortoise.getWeakness().getElement());
        assertDefence(mutatedTortoise, 120, 50, 50, 0, 0, 0);
        assertEquals("warped-creatures", mutatedTortoise.getStrategyId());
        assertDefence(mutatedTerrorbird, 40, 50, 50, 0, 0, 75);
        assertEquals("warped-creatures", mutatedTerrorbird.getStrategyId());
    }

    @Test
    public void strategyJsonCoversAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path strategyJson = Paths.get("src/main/data/slayer/strategies/warped-creatures/strategy.json");

        assertTrue("Warped creatures strategy JSON missing", Files.exists(strategyJson));
        SourceStrategy strategy = read(strategyJson, SourceStrategy.class);

        assertEquals("warped-creatures", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Warped_creatures", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("sarachnis-cudgel"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "hunters-sunlight-crossbow".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "twinflame-staff".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "requirements-and-unlock");
        assertContains(methodIds(strategy), "prayer-melee-tortoise");
        assertContains(methodIds(strategy), "ranged-tortoise-safespot");
        assertContains(methodIds(strategy), "magic-tortoise-safespot");
        assertContains(methodIds(strategy), "terrorbird-prayer-safespot");
        assertContains(methodIds(strategy), "terrorbird-cannoning");
        assertContains(methodIds(strategy), "superior-variants");
        assertContains(styleIds(strategy), "prayer-melee-tortoise");
        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "magic-safespot");

        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("crystal chime")));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("1490,4263")));
    }

    @Test
    public void generatedRuntimeDataCarriesWarpedCreatureStrategiesAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Warped creatures".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Warped creatures"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(5, task.getVariants().size());
        assertEquals(4, task.getLocations().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Poison Waste Dungeon - tortoise main level"
            .equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Poison Waste Dungeon - terrorbird main level"
            .equals(location.getName())));

        MonsterVariant tortoise = variantNamed(task, "Warped Tortoise");
        MonsterVariant terrorbird = variantNamed(task, "Warped Terrorbird (Level 96)");
        MonsterVariant superior = variantNamed(task, "Mutated Terrorbird");

        assertNotNull(tortoise.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Warped_creatures",
            tortoise.getStrategy().getSourceUrl());
        assertNotNull(terrorbird.getStrategy());
        assertNotNull(superior.getStrategy());
    }

    private static SourceMonsterVariant readVariant(String fileName) throws IOException
    {
        return read(Paths.get("src/main/data/slayer/monsters/warped-creatures/" + fileName),
            SourceMonsterVariant.class);
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
        return strategy.getStyleOptions().stream().map(SourceStrategyStyleOption::getStyleId).collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }

    private static <T> void assertContains(Set<T> set, T value)
    {
        assertTrue("expected " + set + " to contain " + value, set.contains(value));
    }
}

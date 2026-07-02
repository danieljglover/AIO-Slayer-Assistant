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

public class ZygomitesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentsUnlocksAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/mutated-zygomites.json"), SourceTask.class);

        assertEquals(Integer.valueOf(271838), task.getWikiPageId());
        assertEquals(Integer.valueOf(60), task.getCombatLevel());
        assertEquals(229, task.getSlayerTargetId());
        assertEquals(57, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Lost City"));
        assertEquals(Integer.valueOf(7806), task.getRequiredItemId());
        assertEquals("Fungicide spray", task.getRequiredItemName());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {8, 15}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {10, 25}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {10, 25}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {20, 30}, task.getAmountByMaster().get("duradel"));
        assertContains(unlockIds(task), "shroom-sprayer");

        assertContains(locationIds(task), "zanaris-furnace-zygomites");
        assertContains(locationIds(task), "zanaris-cosmic-altar-zygomites");
        assertContains(locationIds(task), "fossil-island-mushroom-forest-zygomites");
        assertContains(locationIds(task), "stalker-den-zygomites");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fungicide spray")
            && note.contains("final blow")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("magical melee")
            && note.contains("magical ranged")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dragonhide")
            && note.contains("Karil")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ancient Zygomites")
            && note.contains("Reagent pouch")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Konar")
            && note.contains("Zanaris") && note.contains("Fossil Island")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/mutated-zygomites.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(3, task.getVariantIds().size());
        assertVariant(variants, "zygomite-level-74", 74, 65.0, "East of the furnace");
        assertVariant(variants, "zygomite-level-86", 86, 75.0, "Cosmic Altar");
        assertVariant(variants, "ancient-zygomite", 109, 154.0, "Stalker Den");

        assertEquals(4, locations.size());
        assertLocation(locations, "zanaris-furnace-zygomites", 5, false, true, false, "East of the furnace");
        assertLocation(locations, "zanaris-cosmic-altar-zygomites", 5, false, true, true, "pillar");
        assertLocation(locations, "fossil-island-mushroom-forest-zygomites", 27, false, true, true,
            "Mushroom Forest");
        assertLocation(locations, "stalker-den-zygomites", 9, true, true, true, "Custodia Pass");
    }

    @Test
    public void variantSourcesUseMonsterPageStatsAndSharedStrategyId() throws IOException
    {
        SourceMonsterVariant level74 = read(Paths.get("src/main/data/slayer/monsters/mutated-zygomites/zygomite-lvl74.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant level86 = read(Paths.get("src/main/data/slayer/monsters/mutated-zygomites/zygomite-lvl86.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant ancient = read(Paths.get("src/main/data/slayer/monsters/mutated-zygomites/ancient-zygomite-lvl109.json"),
            SourceMonsterVariant.class);

        assertZygomiteVariant(level74, "zygomite-level-74", 74, 65, 10, 10, 10, 20, 20,
            "zanaris-furnace-zygomites");
        assertZygomiteVariant(level86, "zygomite-level-86", 86, 75, 10, 10, 10, 20, 20,
            "zanaris-cosmic-altar-zygomites");
        assertZygomiteVariant(ancient, "ancient-zygomite", 109, 80, 20, 20, 20, 30, 30,
            "fossil-island-mushroom-forest-zygomites");
    }

    @Test
    public void strategyJsonCoversTaskPageMethodsAndStyleOptions() throws IOException
    {
        assertFalse(Files.exists(Paths.get("src/main/data/slayer/strategies/mutated-zygomites.md")));
        SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies/mutated-zygomites/strategy.json"),
            SourceStrategy.class);

        assertEquals("mutated-zygomites", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Zygomites", strategy.getSourceUrl());
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "zygomite-level-74");
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "zygomite-level-86");
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "ancient-zygomite");
        assertNotNull(strategy.getPlugin());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getNote().contains("fungicide spray"));
        assertTrue(strategy.getPlugin().getNote().contains("magic defence"));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> "fungicide-finisher".equals(method.getMethodId())
            && method.getSteps().stream().anyMatch(step -> step.contains("under 8 Hitpoints"))));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> "zanaris-safespot".equals(method.getMethodId())
            && method.getSteps().stream().anyMatch(step -> step.contains("mysterious ruins"))));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> "ancient-zygomites".equals(method.getMethodId())
            && method.getSteps().stream().anyMatch(step -> step.contains("Reagent pouch"))));
        assertContains(styleIds(strategy), "melee-dragonhide");
        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "magic-fire");
    }

    @Test
    public void generatedRuntimeDataCarriesZygomiteSourcesAndStrategies()
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Mutated zygomites".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Mutated zygomites"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertEquals(3, task.getVariants().size());
        assertEquals(4, task.getLocations().size());
        assertEquals("Fungicide spray", task.getRequiredItemName());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Zanaris near the Cosmic Altar".equals(location.getName())
            && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Stalker Den".equals(location.getName())
            && location.isMulti() && location.isCannon()));

        MonsterVariant zygomite = variantNamed(task, "Zygomite (level 74)");
        MonsterVariant ancient = variantNamed(task, "Ancient Zygomite");

        assertNotNull(zygomite.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Zygomites", zygomite.getStrategy().getSourceUrl());
        assertNotNull(ancient.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Zygomites", ancient.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("zanaris-furnace-zygomites", "Zanaris east of the furnace", false, true, false,
            false, "5 level 74 and 86 Zygomites");
        assertLocationSource("zanaris-cosmic-altar-zygomites", "Zanaris near the Cosmic Altar", false, true,
            true, false, "pillar");
        assertLocationSource("fossil-island-mushroom-forest-zygomites", "Mushroom Forest", false, true, true,
            false, "27 Ancient Zygomites");
        assertLocationSource("stalker-den-zygomites", "Stalker Den", true, true, true, false,
            "Custodia Pass");
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

    private static void assertZygomiteVariant(SourceMonsterVariant variant, String variantId, Integer combatLevel,
        int defenceLevel, int stab, int slash, int crush, int magic, int range, String locationId)
    {
        assertEquals(variantId, variant.getVariantId());
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(CombatStyle.MELEE, variant.getWeakness().getStyle());
        assertEquals("fire", variant.getWeakness().getElement());
        assertEquals(defenceLevel, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(stab, variant.getMonsterDefence().getStab());
        assertEquals(slash, variant.getMonsterDefence().getSlash());
        assertEquals(crush, variant.getMonsterDefence().getCrush());
        assertEquals(magic, variant.getMonsterDefence().getMagic());
        assertEquals(range, variant.getMonsterDefence().getRange());
        assertEquals(locationId, variant.getLocationId());
        assertEquals("mutated-zygomites", variant.getStrategyId());
        assertTrue(variant.getRequirement().contains("Fungicide spray"));
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

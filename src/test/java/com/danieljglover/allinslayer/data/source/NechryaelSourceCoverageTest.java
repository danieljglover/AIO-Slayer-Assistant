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

public class NechryaelSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentsAndDeathSpawnMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/nechryael.json"), SourceTask.class);

        assertEquals(Integer.valueOf(523205), task.getWikiPageId());
        assertEquals(Integer.valueOf(85), task.getCombatLevel());
        assertEquals(230, task.getSlayerTargetId());
        assertEquals(80, task.getSlayerLevel());
        assertTrue("task page lists no other requirement", task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());
        assertTrue(task.isDemon());

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {110, 110}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {110, 170}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        for (String masterId : masterIds(task))
        {
            assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get(masterId));
        }

        assertContains(locationIds(task), "slayer-tower-top-floor-nechryael");
        assertContains(locationIds(task), "slayer-tower-basement-nechryael");
        assertContains(locationIds(task), "catacombs-of-kourend-nechryael");
        assertContains(locationIds(task), "wilderness-slayer-cave-nechryael");
        assertContains(locationIds(task), "iorwerth-dungeon-nechryael");
        assertContains(locationIds(task), "charred-dungeon-nechryael");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("type of demon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("death spawn")
            && note.contains("max hit of 2")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("typeless melee")
            && note.contains("Protect from Melee has no effect")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("single-combat")
            && note.contains("already in combat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("food")
            && note.contains("blood spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Greater Nechryael")
            && note.contains("more lucrative drop table")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Nechryarch")
            && note.contains("chaotic death spawn")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("safespots")
            && note.contains("farcasting")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("goading potion")
            && note.contains("Catacombs")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Iorwerth Dungeon")
            && note.contains("crystal shards")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness Slayer Cave")
            && note.contains("player killing")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("60 Sailing")
            && note.contains("Charred Dungeon")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/nechryael.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(3, variants.size());
        assertVariant(variants, "nechryael", 115, 128.0, "Slayer Tower");
        assertVariant(variants, "greater-nechryael", 200, 205.0, "slightly different drop table");
        assertVariant(variants, "nechryarch", 300, 3280.0, "Chaotic death spawn");

        assertEquals(6, locations.size());
        assertLocation(locations, "slayer-tower-top-floor-nechryael", 11, false, false, true, "top floor");
        assertLocation(locations, "slayer-tower-basement-nechryael", 12, false, false, true, "slayer task");
        assertLocation(locations, "catacombs-of-kourend-nechryael", 14, true, false, false, "Great Kourend");
        assertLocation(locations, "wilderness-slayer-cave-nechryael", 8, true, true, false, "player-killers");
        assertLocation(locations, "iorwerth-dungeon-nechryael", 16, false, true, false, "Song of the Elves");
        assertLocation(locations, "charred-dungeon-nechryael", 11, true, true, true, "60 Sailing");
    }

    @Test
    public void variantSourcesUseMonsterPageStatsAndStrategyId() throws IOException
    {
        SourceMonsterVariant regular = read(Paths.get("src/main/data/slayer/monsters/nechryael/nechryael-lvl115.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant greater = read(Paths.get("src/main/data/slayer/monsters/nechryael/greater-nechryael-lvl200.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant nechryarch = read(Paths.get("src/main/data/slayer/monsters/nechryael/nechryarch-lvl300.json"),
            SourceMonsterVariant.class);

        assertNechryaelVariant(regular, "nechryael", 115, 105, 20, 20, 20, 0, 20,
            "slayer-tower-top-floor-nechryael");
        assertNechryaelVariant(greater, "greater-nechryael", 200, 85, 50, 50, 50, 0, 50,
            "catacombs-of-kourend-nechryael");
        assertNechryaelVariant(nechryarch, "nechryarch", 300, 140, 30, 30, 30, 0, 30,
            "catacombs-of-kourend-nechryael");
    }

    @Test
    public void strategyJsonCoversTaskPageMagicMeleeWildernessAndSuperiorMethods() throws IOException
    {
        assertFalse(Files.exists(Paths.get("src/main/data/slayer/strategies/nechryael.md")));
        SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies/nechryael/strategy.json"),
            SourceStrategy.class);

        assertEquals("nechryael", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Nechryael", strategy.getSourceUrl());
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "nechryael");
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "greater-nechryael");
        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()), "nechryarch");
        assertNotNull(strategy.getPlugin());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("kodai-wand"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("ancient-sceptre"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "emberlight".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dinh-s-bulwark".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getNote().contains("Greater Nechryael"));
        assertTrue(strategy.getPlugin().getNote().contains("death spawn"));

        assertContains(methodIds(strategy), "greater-magic-burst-barrage");
        assertContains(methodIds(strategy), "luring-goading");
        assertContains(methodIds(strategy), "regular-demonbane-melee");
        assertContains(methodIds(strategy), "iorwerth-melee");
        assertContains(methodIds(strategy), "wilderness-krystilia");
        assertContains(methodIds(strategy), "nechryarch-superior");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "greater-magic-burst-barrage".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing magic method"))
            .getSteps().stream().anyMatch(step -> step.contains("goading potion")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "regular-demonbane-melee".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing melee method"))
            .getSteps().stream().anyMatch(step -> step.contains("Emberlight")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "nechryarch-superior".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing superior method"))
            .getSteps().stream().anyMatch(step -> step.contains("chaotic death spawn")));

        assertContains(styleIds(strategy), "greater-magic");
        assertContains(styleIds(strategy), "demonbane-melee");
        assertContains(styleIds(strategy), "wilderness-magic");
    }

    @Test
    public void generatedRuntimeDataCarriesNechryaelSourcesAndStrategies()
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Nechryael".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Nechryael"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertTrue(task.isDemon());
        assertEquals(3, task.getVariants().size());
        assertEquals(6, task.getLocations().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Catacombs of Kourend".equals(location.getName())
            && location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness Slayer Cave".equals(location.getName())
            && location.isMulti() && location.isCannon() && location.isWilderness()));

        MonsterVariant regular = variantNamed(task, "Nechryael");
        MonsterVariant greater = variantNamed(task, "Greater Nechryael");
        MonsterVariant superior = variantNamed(task, "Nechryarch");

        assertNotNull(regular.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Nechryael", regular.getStrategy().getSourceUrl());
        assertNotNull(greater.getStrategy());
        assertEquals(CombatStyle.MAGIC, greater.getStrategy().getPrimaryStyle());
        assertNotNull(superior.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Nechryael", superior.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("slayer-tower-top-floor-nechryael", "Slayer Tower (top floor)", false, false,
            true, false, "11 Nechryael");
        assertLocationSource("slayer-tower-basement-nechryael", "Slayer Tower (basement)", false, false,
            true, false, "12 Nechryael");
        assertLocationSource("catacombs-of-kourend-nechryael", "Catacombs of Kourend", true, false, false,
            false, "14 Greater Nechryael");
        assertLocationSource("wilderness-slayer-cave-nechryael", "Wilderness Slayer Cave", true, true, false,
            true, "player-killers");
        assertLocationSource("iorwerth-dungeon-nechryael", "Iorwerth Dungeon", false, true, false, false,
            "Song of the Elves");
        assertLocationSource("charred-dungeon-nechryael", "Charred Dungeon", true, true, true, false,
            "60 Sailing");
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

    private static void assertNechryaelVariant(SourceMonsterVariant variant, String variantId, Integer combatLevel,
        int defenceLevel, int stab, int slash, int crush, int magic, int range, String locationId)
    {
        assertEquals(variantId, variant.getVariantId());
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(CombatStyle.MELEE, variant.getWeakness().getStyle());
        assertNull(variant.getWeakness().getElement());
        assertEquals(defenceLevel, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(stab, variant.getMonsterDefence().getStab());
        assertEquals(slash, variant.getMonsterDefence().getSlash());
        assertEquals(crush, variant.getMonsterDefence().getCrush());
        assertEquals(magic, variant.getMonsterDefence().getMagic());
        assertEquals(range, variant.getMonsterDefence().getRange());
        assertEquals(locationId, variant.getLocationId());
        assertEquals("nechryael", variant.getStrategyId());
        assertTrue(variant.isDemon());
        assertTrue(variant.getRequirement().contains("80 Slayer"));
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

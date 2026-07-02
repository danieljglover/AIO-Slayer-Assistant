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

public class MetalDragonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlocksAndDragonfireMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/metal-dragons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(602634), task.getWikiPageId());
        assertNull("wiki task infobox combat requirement is blank", task.getCombatLevel());
        assertEquals(228, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Dragon Slayer I (partial completion)"));
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());
        assertTrue(task.isDragon());

        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {30, 40}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {30, 40}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {35, 45}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("duradel"));
        assertContains(unlockIds(task), "pedal-to-the-metals");

        assertContains(locationIds(task), "brimhaven-dungeon-metal-dragons-task-only");
        assertContains(locationIds(task), "brimhaven-dungeon-metal-dragons");
        assertContains(locationIds(task), "catacombs-of-kourend-metal-dragons");
        assertContains(locationIds(task), "isle-of-souls-dungeon-metal-dragons");
        assertContains(locationIds(task), "ancient-cavern-metal-dragons");
        assertContains(locationIds(task), "lithkren-vault-metal-dragons");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Partial completion") && note.contains("Dragon Slayer I")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("only mithril dragons cannot be cannoned")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("metal dragons aren't affected") && note.contains("Protect from Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("anti-dragon shield") && note.contains("super antifire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Mithril, adamant and rune dragons") && note.contains("magic and ranged")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("ruby") && note.contains("onyx")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragon hunter lance") && note.contains("dragon hunter wand")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("50%") && note.contains("earth spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("bronze dragons") && note.contains("slayer-only area")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("iron dragons") && note.contains("draconic visage")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("rune dragons") && note.contains("profit")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("block") && note.contains("high weight")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/metal-dragons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(9, task.getVariantIds().size());
        assertEquals(9, variants.size());
        assertVariant(variants, "bronze-dragon", 131, 125.0, "does not drop the draconic visage");
        assertVariant(variants, "bronze-dragon-catacombs", 143, 125.0, "Catacombs of Kourend");
        assertVariant(variants, "iron-dragon", 189, 173.2, "draconic visage");
        assertVariant(variants, "iron-dragon-catacombs", 215, 204.5, "Catacombs of Kourend");
        assertVariant(variants, "steel-dragon", 246, 220.5, "Third weakest");
        assertVariant(variants, "steel-dragon-catacombs", 274, 262.5, "Catacombs of Kourend");
        assertVariant(variants, "mithril-dragon", 304, 273.0, "dragon full helm");
        assertVariant(variants, "adamant-dragon", 338, 324.5, "Ruby bolts");
        assertVariant(variants, "rune-dragon", 380, 363.0, "insulated boots");

        assertEquals(6, locations.size());
        assertLocation(locations, "brimhaven-dungeon-metal-dragons-task-only", null, false, true, false, "5 bronze");
        assertLocation(locations, "brimhaven-dungeon-metal-dragons", null, false, true, false, "3 bronze");
        assertLocation(locations, "catacombs-of-kourend-metal-dragons", null, true, false, false, "Ancient shards");
        assertLocation(locations, "isle-of-souls-dungeon-metal-dragons", 3, false, true, false, "3 iron");
        assertLocation(locations, "ancient-cavern-metal-dragons", 7, false, false, false, "Barbarian Firemaking");
        assertLocation(locations, "lithkren-vault-metal-dragons", null, false, true, false, "Dragon Slayer II");
    }

    @Test
    public void variantSourcesUseMonsterPageStatsAndStrategyIds() throws IOException
    {
        SourceMonsterVariant bronze = read(Paths.get("src/main/data/slayer/monsters/metal-dragons/bronze-dragon-lvl131.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant bronzeCatacombs = read(Paths.get("src/main/data/slayer/monsters/metal-dragons/bronze-dragon-catacombs-lvl143.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant ironCatacombs = read(Paths.get("src/main/data/slayer/monsters/metal-dragons/iron-dragon-catacombs-lvl215.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant steelCatacombs = read(Paths.get("src/main/data/slayer/monsters/metal-dragons/steel-dragon-catacombs-lvl274.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant mithril = read(Paths.get("src/main/data/slayer/monsters/metal-dragons/mithril-dragon-lvl304.json"),
            SourceMonsterVariant.class);

        assertDragonVariant(bronze, "bronze-dragon", 131, 112, 0, 70, 70, 30, 90, "bronze-dragon");
        assertDragonVariant(bronzeCatacombs, "bronze-dragon-catacombs", 143, 112, 0, 70, 70, 30, 90, "bronze-dragon");
        assertDragonVariant(ironCatacombs, "iron-dragon-catacombs", 215, 185, 0, 70, 70, 30, 90, "iron-dragon");
        assertDragonVariant(steelCatacombs, "steel-dragon-catacombs", 274, 235, 0, 70, 70, 30, 90, "steel-dragon");
        assertDragonVariant(mithril, "mithril-dragon", 304, 268, 0, 100, 70, 30, 90, "mithril-dragon");
        assertTrue(mithril.getRequirement().contains("Barbarian Firemaking"));
    }

    @Test
    public void legacyStrategyMarkdownIsMigratedToJsonForEveryStyleOption() throws IOException
    {
        for (String strategyId : new String[] {
            "bronze-dragon", "iron-dragon", "steel-dragon", "mithril-dragon", "adamant-dragon", "rune-dragon"
        })
        {
            assertFalse("legacy Markdown should be migrated: " + strategyId,
                Files.exists(Paths.get("src/main/data/slayer/strategies/" + strategyId + ".md")));
            SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies/" + strategyId + "/strategy.json"),
                SourceStrategy.class);
            assertEquals(strategyId, strategy.getStrategyId());
            assertNotNull(strategy.getPlugin());
            assertTrue(strategy.getPlugin().getNote().contains("dragon"));
            assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getMethodId().contains("dragonfire")));
            assertContains(styleIds(strategy), "melee-dragonbane");
            assertTrue(styleIds(strategy).contains("ranged-dragonbane") || styleIds(strategy).contains("magic-earth"));
        }

        SourceStrategy bronze = read(Paths.get("src/main/data/slayer/strategies/bronze-dragon/strategy.json"),
            SourceStrategy.class);
        assertEquals("https://oldschool.runescape.wiki/w/Metal_dragons/Strategies", bronze.getSourceUrl());
        assertTrue(bronze.getPlugin().getPrimaryWeapons().contains("dragon-hunter-lance"));
        assertTrue(bronze.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        SourceStrategy rune = read(Paths.get("src/main/data/slayer/strategies/rune-dragon/strategy.json"),
            SourceStrategy.class);
        assertEquals("https://oldschool.runescape.wiki/w/Rune_dragon/Strategies", rune.getSourceUrl());
        assertTrue(rune.getPlugin().getPrimaryWeapons().contains("dragon-hunter-lance"));
        assertTrue(rune.getMethods().stream()
            .filter(method -> "special-attacks".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing rune special method"))
            .getSteps().stream().anyMatch(step -> step.contains("insulated boots")));
    }

    @Test
    public void generatedRuntimeDataCarriesMetalDragonSourcesAndStrategies() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Metal dragons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Metal dragons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.isDragon());
        assertEquals(9, task.getVariants().size());
        assertEquals(6, task.getLocations().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Brimhaven Dungeon (task-only metal dragons)".equals(location.getName())
            && !location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Ancient Cavern".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));

        MonsterVariant bronze = variantNamed(task, "Bronze dragon");
        MonsterVariant bronzeCatacombs = variantNamed(task, "Bronze dragon (Catacombs)");
        MonsterVariant rune = variantNamed(task, "Rune dragon");

        assertNotNull(bronze.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Metal_dragons/Strategies", bronze.getStrategy().getSourceUrl());
        assertNotNull(bronzeCatacombs.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Metal_dragons/Strategies", bronzeCatacombs.getStrategy().getSourceUrl());
        assertNotNull(rune.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Rune_dragon/Strategies", rune.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("brimhaven-dungeon-metal-dragons-task-only", "Brimhaven Dungeon (task-only metal dragons)",
            false, true, false, false, "5 bronze");
        assertLocationSource("brimhaven-dungeon-metal-dragons", "Brimhaven Dungeon", false, true, false, false,
            "3 bronze");
        assertLocationSource("catacombs-of-kourend-metal-dragons", "Catacombs of Kourend", true, false, false,
            false, "Ancient shards");
        assertLocationSource("isle-of-souls-dungeon-metal-dragons", "Isle of Souls Dungeon", false, true, false,
            false, "3 iron");
        assertLocationSource("ancient-cavern-metal-dragons", "Ancient Cavern", false, false, false, false,
            "Barbarian Firemaking");
        assertLocationSource("lithkren-vault-metal-dragons", "Lithkren Vault", false, true, false, false,
            "Dragon Slayer II");
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

    private static void assertDragonVariant(SourceMonsterVariant variant, String variantId, Integer combatLevel,
        int defenceLevel, int stab, int slash, int crush, int magic, int range, String strategyId)
    {
        assertEquals(variantId, variant.getVariantId());
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals("earth", variant.getWeakness().getElement());
        assertEquals(defenceLevel, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(stab, variant.getMonsterDefence().getStab());
        assertEquals(slash, variant.getMonsterDefence().getSlash());
        assertEquals(crush, variant.getMonsterDefence().getCrush());
        assertEquals(magic, variant.getMonsterDefence().getMagic());
        assertEquals(range, variant.getMonsterDefence().getRange());
        assertTrue(variant.isDragon());
        assertEquals(strategyId, variant.getStrategyId());
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

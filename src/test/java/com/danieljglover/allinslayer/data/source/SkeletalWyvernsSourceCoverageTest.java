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
import static org.junit.Assert.assertTrue;

public class SkeletalWyvernsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsMonsterPageAssignmentsRequirementsAndShieldMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/skeletal-wyverns.json"), SourceTask.class);

        assertEquals(Integer.valueOf(14925), task.getWikiPageId());
        assertEquals(Integer.valueOf(70), task.getCombatLevel());
        assertEquals(232, task.getSlayerTargetId());
        assertEquals(72, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Elemental Workshop I"));
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());
        assertTrue(task.isDragon());
        assertFalse(task.isUndead());

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {5, 12}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {5, 15}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {20, 40}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {50, 70}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {50, 70}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {50, 70}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {50, 70}, task.getExtendedAmount().get("duradel"));
        assertContains(unlockIds(task), "wyver-nother-one");
        assertContains(locationIds(task), "asgarnian-ice-dungeon");
        assertContains(locationIds(task), "asgarnian-ice-dungeon-task-only");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("elemental shield")
            && note.contains("icy breath")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("ancient wyvern shield")
            && note.contains("freezing")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Salve amulet")
            && note.contains("do not work")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonbane weapons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("25% elemental weakness")
            && note.contains("fire spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Safespotted")
            && note.contains("10 minutes")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dwarf multicannon")
            && note.contains("lower")));
    }

    @Test
    public void taskSourceContainsMonsterPageVariantAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/skeletal-wyverns.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(1, variants.size());
        SourceTaskVariantInfo skeletalWyvern = variants.get("skeletal-wyvern");
        assertNotNull(skeletalWyvern);
        assertEquals(Integer.valueOf(140), skeletalWyvern.getCombatLevel());
        assertEquals(210.0, skeletalWyvern.getSlayerXp().doubleValue(), 0.0);
        assertTrue(skeletalWyvern.getNotes().stream().anyMatch(note -> note.contains("max hit 13")
            && note.contains("50+")));
        assertTrue(skeletalWyvern.getNotes().stream().anyMatch(note -> note.contains("Hitpoints 200")));
        assertTrue(skeletalWyvern.getNotes().stream().anyMatch(note -> note.contains("Fire")
            && note.contains("25%")));

        assertEquals(2, locations.size());
        assertLocation(locations, "asgarnian-ice-dungeon", null, false, true, true, "lower cave");
        assertLocation(locations, "asgarnian-ice-dungeon-task-only", null, false, false, true, "task-only");
    }

    @Test
    public void variantSourceUsesMonsterPageStatsAndStrategyId() throws IOException
    {
        SourceMonsterVariant variant = read(Paths.get(
            "src/main/data/slayer/monsters/skeletal-wyverns/skeletal-wyvern-lvl140.json"),
            SourceMonsterVariant.class);

        assertEquals("skeletal-wyvern", variant.getVariantId());
        assertEquals("Skeletal Wyvern", variant.getName());
        assertEquals(Arrays.asList(468, 465, 466, 467), variant.getNpcIds());
        assertEquals(Integer.valueOf(140), variant.getCombatLevel());
        assertEquals(CombatStyle.RANGED, variant.getWeakness().getStyle());
        assertEquals("fire", variant.getWeakness().getElement());
        assertEquals(120, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(140, variant.getMonsterDefence().getStab());
        assertEquals(90, variant.getMonsterDefence().getSlash());
        assertEquals(90, variant.getMonsterDefence().getCrush());
        assertEquals(80, variant.getMonsterDefence().getMagic());
        assertEquals(140, variant.getMonsterDefence().getRange());
        assertTrue(variant.isDragon());
        assertFalse(variant.isUndead());
        assertEquals("skeletal-wyvern", variant.getStrategyId());
    }

    @Test
    public void strategyJsonCoversStrategyPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/skeletal-wyvern/strategy.json");

        assertTrue("Skeletal Wyvern strategy JSON missing", Files.exists(json));
        assertFalse("Legacy Skeletal Wyvern Markdown strategy should be migrated to JSON",
            Files.exists(Paths.get("src/main/data/slayer/strategies/skeletal-wyvern.md")));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("skeletal-wyvern", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Skeletal_Wyvern/Strategies", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-lance"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("inquisitor-s-mace"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-crossbow".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("anti-icy-breath shield"));
        assertTrue(strategy.getPlugin().getNote().contains("10-minute aggression timer"));

        assertContains(methodIds(strategy), "requirements-and-transportation");
        assertContains(methodIds(strategy), "attacks-and-protection");
        assertContains(methodIds(strategy), "dragonbane-melee");
        assertContains(methodIds(strategy), "ranged-safespot");
        assertContains(methodIds(strategy), "ranged-void-safespot");
        assertContains(methodIds(strategy), "magic-fire-safespot");
        assertContains(methodIds(strategy), "cannon-and-prayers");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "attacks-and-protection".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing attacks method"))
            .getSteps().stream().anyMatch(step -> step.contains("50+")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "ranged-safespot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing ranged safespot method"))
            .getSteps().stream().anyMatch(step -> step.contains("tolerant")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "magic-fire-safespot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing magic method"))
            .getSteps().stream().anyMatch(step -> step.contains("Fire") && step.contains("Dragon hunter wand")));

        assertContains(styleIds(strategy), "melee-dragonbane");
        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "ranged-void-safespot");
        assertContains(styleIds(strategy), "magic-fire-safespot");
    }

    @Test
    public void generatedRuntimeDataCarriesSkeletalWyvernStrategyAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Skeletal wyverns".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Skeletal wyverns"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Asgarnian Ice Dungeon".equals(location.getName())
            && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Asgarnian Ice Dungeon task-only area"
            .equals(location.getName()) && !location.isCannon()));
        assertTrue(task.isDragon());
        assertFalse(task.isUndead());

        MonsterVariant skeletalWyvern = task.getVariants().stream()
            .filter(variant -> "Skeletal Wyvern".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Skeletal Wyvern"));

        assertEquals(CombatStyle.RANGED, skeletalWyvern.getWeakness().getStyle());
        assertEquals("fire", skeletalWyvern.getWeakness().getElement());
        assertNotNull(skeletalWyvern.getStrategy());
        assertEquals(CombatStyle.MELEE, skeletalWyvern.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Skeletal_Wyvern/Strategies",
            skeletalWyvern.getStrategy().getSourceUrl());
        assertTrue(skeletalWyvern.getStrategy().getNote().contains("dragonbane"));
    }

    @Test
    public void locationSourcesMatchSkeletalWyvernPage() throws IOException
    {
        assertLocationSource("asgarnian-ice-dungeon", "Asgarnian Ice Dungeon", false, true, true, false,
            "south of Port Sarim");
        assertLocationSource("asgarnian-ice-dungeon-task-only", "Asgarnian Ice Dungeon task-only area", false,
            false, true, false, "Steve/Pieve");
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

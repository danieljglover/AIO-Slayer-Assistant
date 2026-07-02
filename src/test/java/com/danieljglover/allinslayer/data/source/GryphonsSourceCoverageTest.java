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

public class GryphonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlocksAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/gryphons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(613360), task.getWikiPageId());
        assertNull("wiki task infobox combat requirement is blank", task.getCombatLevel());
        assertEquals(223, task.getSlayerTargetId());
        assertEquals(51, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Troubled Tortugans"));
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());

        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertFalse(task.getMasterIds().contains("konar"));
        assertArrayEquals(new int[] {30, 80}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {60, 100}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {110, 170}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {100, 210}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {110, 160}, task.getExtendedAmount().get("vannaka"));
        assertArrayEquals(new int[] {140, 180}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {190, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {180, 290}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "wings-spread");
        assertContains(unlockIds(task), "gryphon-and-on");
        assertContains(locationIds(task), "western-gryphon-dungeon");
        assertContains(locationIds(task), "eastern-gryphon-dungeon");
        assertContains(locationIds(task), "shellbane-gryphon-cave");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Troubled Tortugans")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wings Spread") && note.contains("Nieve")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Vannaka") && note.contains("highest weighted task")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("30 kg") && note.contains("normal gryphons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("40 kg") && note.contains("boss")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("tortugan shield") && note.contains("Dire gryphons")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("50%") && note.contains("air spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("venator bow") && note.contains("dwarf multicannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("poor drops") && note.contains("avoid toggling")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/gryphons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(3, variants.size());
        assertVariant(variants, "gryphon", 95, 110.0, "Standard variant");
        assertVariant(variants, "dire-gryphon-superior", 209, 2800.0, "tortugan shield");
        assertVariant(variants, "shellbane-gryphon", 235, 400.0, "Shellbane Gryphon/Strategies");

        assertEquals(3, locations.size());
        assertLocation(locations, "western-gryphon-dungeon", 18, true, true, true, "western cave");
        assertLocation(locations, "eastern-gryphon-dungeon", 18, true, true, true, "Task-only");
        assertLocation(locations, "shellbane-gryphon-cave", 1, false, false, false, "Task-only boss");
    }

    @Test
    public void variantSourcesUseRealGryphonAndDireGryphonStats() throws IOException
    {
        SourceMonsterVariant gryphon = read(Paths.get("src/main/data/slayer/monsters/gryphons/gryphon-lvl95.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant dire = read(Paths.get("src/main/data/slayer/monsters/gryphons/dire-gryphon-lvl209.json"),
            SourceMonsterVariant.class);

        assertEquals("gryphon", gryphon.getVariantId());
        assertEquals(Integer.valueOf(95), gryphon.getCombatLevel());
        assertEquals("air", gryphon.getWeakness().getElement());
        assertEquals(50, gryphon.getMonsterDefence().getDefenceLevel());
        assertEquals(10, gryphon.getMonsterDefence().getStab());
        assertEquals(20, gryphon.getMonsterDefence().getSlash());
        assertEquals(40, gryphon.getMonsterDefence().getCrush());
        assertEquals(100, gryphon.getMonsterDefence().getMagic());
        assertEquals("gryphons", gryphon.getStrategyId());

        assertEquals("dire-gryphon-superior", dire.getVariantId());
        assertEquals(Integer.valueOf(209), dire.getCombatLevel());
        assertEquals("air", dire.getWeakness().getElement());
        assertEquals(100, dire.getMonsterDefence().getDefenceLevel());
        assertEquals(10, dire.getMonsterDefence().getStab());
        assertEquals(20, dire.getMonsterDefence().getSlash());
        assertEquals(40, dire.getMonsterDefence().getCrush());
        assertEquals(150, dire.getMonsterDefence().getMagic());
        assertTrue(dire.getRequirement().contains("Bigger and Badder"));
        assertEquals("gryphons", dire.getStrategyId());
    }

    @Test
    public void strategySourceContainsTaskPageEquipmentAndMechanics() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/gryphons/strategy.json");

        assertTrue("Gryphons strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("gryphons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Gryphons", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("venator-bow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "scythe-of-vitur".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getNote().contains("30 kg"));
        assertTrue(strategy.getPlugin().getNote().contains("tortugan shield"));

        assertContains(methodIds(strategy), "requirements-and-unlocks");
        assertContains(methodIds(strategy), "weight-and-knockback");
        assertContains(methodIds(strategy), "eastern-cannon-venator");
        assertContains(methodIds(strategy), "melee-heavy-gear");
        assertContains(methodIds(strategy), "dire-gryphon-superior");
        assertContains(methodIds(strategy), "shellbane-choice");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "eastern-cannon-venator".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing cannon method"))
            .getSteps().stream().anyMatch(step -> step.contains("south-western multicombat room")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "weight-and-knockback".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing weight method"))
            .getSteps().stream().anyMatch(step -> step.contains("within 2 ticks")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "inventory-and-loot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing inventory method"))
            .getInventory().stream().anyMatch(item -> item.contains("Tortugan shield")));

        assertContains(styleIds(strategy), "ranged-cannon");
        assertContains(styleIds(strategy), "melee-heavy");
    }

    @Test
    public void generatedRuntimeDataCarriesGryphonStrategyVariantsAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Gryphons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Gryphons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(3, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Western gryphon dungeon".equals(location.getName())
            && location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Eastern gryphon dungeon".equals(location.getName())
            && location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Shellbane Gryphon Cave".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));

        MonsterVariant gryphon = variantNamed(task, "Gryphon");
        MonsterVariant dire = variantNamed(task, "Dire gryphon");
        MonsterVariant shellbane = variantNamed(task, "Shellbane gryphon");

        assertNotNull(gryphon.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Gryphons", gryphon.getStrategy().getSourceUrl());
        assertEquals(CombatStyle.RANGED, gryphon.getStrategy().getPrimaryStyle());
        assertNotNull(dire.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Gryphons", dire.getStrategy().getSourceUrl());
        assertNotNull(shellbane.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Shellbane_gryphon/Strategies",
            shellbane.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertFalse("Old generic Gryphon roost location should be replaced by task-page locations",
            Files.exists(Paths.get("src/main/data/slayer/locations/gryphon-roost-sailing.json")));
        assertLocationSource("western-gryphon-dungeon", "Western gryphon dungeon", true, true, true, false,
            "18 gryphons");
        assertLocationSource("eastern-gryphon-dungeon", "Eastern gryphon dungeon", true, true, true, false,
            "Task-only");
        assertLocationSource("shellbane-gryphon-cave", "Shellbane Gryphon Cave", false, false, false, false,
            "Task-only boss");
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

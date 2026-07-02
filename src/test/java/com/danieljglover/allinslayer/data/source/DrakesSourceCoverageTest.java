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

public class DrakesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentRequirementsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/drakes.json"), SourceTask.class);

        assertEquals(Integer.valueOf(267357), task.getWikiPageId());
        assertEquals(Integer.valueOf(90), task.getCombatLevel());
        assertEquals(215, task.getSlayerTargetId());
        assertEquals(84, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("water", task.getWeakness().getElement());

        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 140}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {30, 95}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {50, 110}, task.getAmountByMaster().get("duradel"));
        assertTrue(task.getUnlocks().isEmpty());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Karuulm Slayer Dungeon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("boots of stone")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Elite Kourend")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragonbane")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("50%") && note.contains("water")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("volcanic breath") && note.contains("seven")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Missiles")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("five times more common")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("block drakes")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndKaruulmLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/drakes.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertEquals(230.6, variants.get("drake").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("drake").getNotes().stream()
            .anyMatch(note -> note.contains("volcanic breath") && note.contains("four hits")));
        assertTrue(variants.get("drake").getNotes().stream()
            .anyMatch(note -> note.contains("Drake's claw") && note.contains("Drake's tooth")));
        assertEquals(7087.0, variants.get("guardian-drake").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("guardian-drake").getNotes().stream()
            .anyMatch(note -> note.contains("stun") && note.contains("melee distance")));
        assertTrue(variants.get("guardian-drake").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));

        assertEquals(1, locations.size());
        SourceTaskLocationComparison karuulm = locations.get("karuulm-slayer-dungeon-drakes");
        assertNotNull(karuulm);
        assertEquals(Integer.valueOf(13), karuulm.getAmount());
        assertEquals(Boolean.FALSE, karuulm.getMulticombat());
        assertEquals(Boolean.TRUE, karuulm.getCannonable());
        assertEquals(Boolean.FALSE, karuulm.getSafespottable());
        assertTrue(karuulm.getNotes().stream().anyMatch(note -> note.contains("task-only area")));
        assertTrue(karuulm.getNotes().stream().anyMatch(note -> note.contains("Mount Karuulm")));
    }

    @Test
    public void strategySourceContainsAllTaskPageMethodsAndStyles() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/drakes/strategy.json");

        assertTrue("Drakes strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("drakes", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Drake", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-lance"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("osmumten-s-fang"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("volcanic breath"));
        assertTrue(strategy.getPlugin().getNote().contains("Guardian Drake"));

        assertContains(methodIds(strategy), "requirements-and-transport");
        assertContains(methodIds(strategy), "mechanics-and-dragonfire");
        assertContains(methodIds(strategy), "melee");
        assertContains(methodIds(strategy), "ranged");
        assertContains(methodIds(strategy), "magic-water-spells");
        assertContains(methodIds(strategy), "guardian-drake");
        assertContains(methodIds(strategy), "inventory-melee");
        assertContains(methodIds(strategy), "inventory-ranged");
        assertContains(methodIds(strategy), "inventory-magic");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "mechanics-and-dragonfire".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing mechanics method"))
            .getSteps().stream().anyMatch(step -> step.contains("seven auto-attacks")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "guardian-drake".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing guardian method"))
            .getSteps().stream().anyMatch(step -> step.contains("yellow gaps")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "inventory-ranged".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing ranged inventory"))
            .getInventory().stream().anyMatch(item -> item.contains("Extended antifire")));

        assertContains(styleIds(strategy), "melee-dragonbane");
        assertContains(styleIds(strategy), "ranged-dragonbane");
        assertContains(styleIds(strategy), "magic-water-spells");
    }

    @Test
    public void generatedRuntimeDataCarriesDrakeStrategiesAndLocation() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Drakes".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Drakes"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Karuulm Slayer Dungeon".equals(location.getName())
            && !location.isMulti() && location.isCannon()));

        MonsterVariant drake = variantNamed(task, "Drake");
        MonsterVariant guardian = variantNamed(task, "Guardian Drake");

        assertNotNull(drake.getStrategy());
        assertEquals(CombatStyle.MELEE, drake.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Drake", drake.getStrategy().getSourceUrl());
        assertNotNull(guardian.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Drake", guardian.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourceMatchesDrakeTaskPage() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/karuulm-slayer-dungeon-drakes.json"),
            SourceLocation.class);

        assertEquals("karuulm-slayer-dungeon-drakes", location.getLocationId());
        assertEquals("Karuulm Slayer Dungeon", location.getName());
        assertEquals(false, location.isMulti());
        assertEquals(true, location.isCannon());
        assertEquals(false, location.isSafeSpot());
        assertEquals(false, location.isWilderness());
        assertTrue(location.getAccessNote().contains("boots of stone"));
        assertTrue(location.getAccessNote().contains("granite boots"));
        assertTrue(location.getAccessNote().contains("task-only area"));
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

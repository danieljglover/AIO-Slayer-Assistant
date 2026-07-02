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

public class CaveHorrorsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cave-horrors.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298271), task.getWikiPageId());
        assertEquals(Integer.valueOf(85), task.getCombatLevel());
        assertEquals(211, task.getSlayerTargetId());
        assertEquals(58, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Cabin Fever"));

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 180}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "horrorific");
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("witchwood icon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("10%") && note.contains("Hitpoints")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("light source")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("magic-based melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fast-firing Ranged")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dwarf multicannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Bones to Peaches")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("black mask")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("30% elemental weakness to fire")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cave-horrors.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertEquals(55.0, variants.get("cave-horror").getSlayerXp().doubleValue(), 0.0);
        assertEquals(1300.0, variants.get("cave-abomination").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("cave-horror").getNotes().stream()
            .anyMatch(note -> note.contains("standard variant")));
        assertTrue(variants.get("cave-abomination").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));

        assertEquals(1, locations.size());
        SourceTaskLocationComparison cave = locations.get("mos-le-harmless-cave");
        assertEquals(Integer.valueOf(70), cave.getAmount());
        assertEquals(Boolean.FALSE, cave.getMulticombat());
        assertEquals(Boolean.TRUE, cave.getCannonable());
        assertEquals(Boolean.TRUE, cave.getSafespottable());
        assertTrue(cave.getNotes().stream().anyMatch(note -> note.contains("70 Cave horrors")));
        assertTrue(cave.getNotes().stream().anyMatch(note -> note.contains("mushrooms") && note.contains("boulders")));
    }

    @Test
    public void strategySourceContainsAllTaskPageStrategyOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/cave-horrors/strategy.json");

        assertTrue("Cave horrors strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("cave-horrors", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Cave_horrors", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("toxic-blowpipe"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("hunters-sunlight-crossbow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "abyssal-whip".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "harmonised-nightmare-staff".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "ranged-safespot");
        assertContains(methodIds(strategy), "prayer-melee");
        assertContains(methodIds(strategy), "witchwood-melee");
        assertContains(methodIds(strategy), "cannoning");
        assertContains(methodIds(strategy), "cave-abomination");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "ranged-safespot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing ranged safespot method"))
            .getSteps().stream().anyMatch(step -> step.contains("Telekinetic Grab")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "prayer-melee".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing prayer melee method"))
            .getSteps().stream().anyMatch(step -> step.contains("Protect from Melee")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "cannoning".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing cannoning method"))
            .getNotes().stream().anyMatch(note -> note.contains("3785,9460")));

        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "prayer-melee");
        assertContains(styleIds(strategy), "witchwood-melee");
        assertContains(styleIds(strategy), "fire-magic");
    }

    @Test
    public void generatedRuntimeDataCarriesCaveHorrorSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Cave horrors".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave horrors"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Mos Le'Harmless Cave".equals(location.getName())
            && !location.isMulti() && location.isCannon()));

        MonsterVariant caveHorror = task.getVariants().stream()
            .filter(variant -> "Cave horror".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave horror"));

        assertNotNull(caveHorror.getStrategy());
        assertEquals(CombatStyle.RANGED, caveHorror.getStrategy().getPrimaryStyle());
        assertEquals("Toxic blowpipe", caveHorror.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(caveHorror.getStrategy().getNote().contains("witchwood icon"));
        assertEquals("Witchwood icon", task.getRequiredItemName());
    }

    @Test
    public void locationSourceMatchesTaskPage() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/mos-le-harmless-cave.json"),
            SourceLocation.class);

        assertEquals("mos-le-harmless-cave", location.getLocationId());
        assertEquals("Mos Le'Harmless Cave", location.getName());
        assertFalse(location.isMulti());
        assertTrue(location.isCannon());
        assertTrue(location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertTrue(location.getAccessNote().contains("Cabin Fever"));
        assertTrue(location.getAccessNote().contains("70 Cave horrors"));
        assertTrue(location.getAccessNote().contains("Fire of eternal light"));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> unlockIds(SourceTask task)
    {
        return task.getUnlocks().stream().map(SourceTaskUnlock::getUnlockId).collect(Collectors.toSet());
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

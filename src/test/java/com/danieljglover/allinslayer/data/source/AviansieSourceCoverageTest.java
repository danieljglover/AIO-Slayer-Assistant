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

public class AviansieSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/aviansie.json"), SourceTask.class);

        assertEquals(Integer.valueOf(342833), task.getWikiPageId());
        assertNull("wiki task infobox combat requirement is None", task.getCombatLevel());
        assertEquals(204, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Death Plateau"));

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {120, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("krystilia"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "watch-the-birdie");
        assertContains(unlockIds(task), "birds-of-a-feather");
        assertContains(locationIds(task), "god-wars-dungeon-armadyl");
        assertContains(locationIds(task), "wilderness-god-wars-dungeon");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Watch the birdie")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("60 Agility") && note.contains("60 Strength")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Armadyl") && note.contains("tolerant")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("ranged and magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("salamanders") && note.contains("halberds")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("adamantite bars")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Hard Fremennik Diary")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Kree'arra")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("spiritual warrior")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Reanimated aviansie")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/aviansie.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(6, variants.size());
        assertTrue(variants.get("aviansie-god-wars-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("Combat level range: 69-148")));
        assertTrue(variants.get("aviansie-god-wars-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("Slayer XP range: 70-139")));
        assertTrue(variants.get("aviansie-wilderness-god-wars-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("Combat level range: 69-137")));
        assertEquals(357.0, variants.get("aviansie-kree-arra").getSlayerXp().doubleValue(), 0.0);
        assertEquals(132.5, variants.get("flight-kilisa").getSlayerXp().doubleValue(), 0.0);
        assertEquals(124.0, variants.get("wingman-skree").getSlayerXp().doubleValue(), 0.0);
        assertEquals(132.5, variants.get("flockleader-geerin").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("aviansie-kree-arra").getNotes().stream()
            .anyMatch(note -> note.contains("70 Ranged") && note.contains("mith grapple")));

        assertEquals(2, locations.size());
        assertEquals(Integer.valueOf(36), locations.get("god-wars-dungeon-armadyl").getAmount());
        assertEquals(Boolean.TRUE, locations.get("god-wars-dungeon-armadyl").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("god-wars-dungeon-armadyl").getCannonable());
        assertEquals(Boolean.FALSE, locations.get("god-wars-dungeon-armadyl").getSafespottable());
        assertTrue(locations.get("god-wars-dungeon-armadyl").getNotes().stream()
            .anyMatch(note -> note.contains("Armadylean followers")));

        assertEquals(Integer.valueOf(8), locations.get("wilderness-god-wars-dungeon").getAmount());
        assertEquals(Boolean.TRUE, locations.get("wilderness-god-wars-dungeon").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("wilderness-god-wars-dungeon").getCannonable());
        assertEquals(Boolean.FALSE, locations.get("wilderness-god-wars-dungeon").getSafespottable());
        assertTrue(locations.get("wilderness-god-wars-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("player killing")));
    }

    @Test
    public void strategySourceContainsAllTaskPageStrategyOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/aviansie/strategy.json");

        assertTrue("Aviansie strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("aviansie", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Aviansie", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("toxic-blowpipe"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "tumeken-s-shadow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "normal-god-wars-dungeon");
        assertContains(methodIds(strategy), "wilderness-god-wars-dungeon");
        assertContains(methodIds(strategy), "kree-arra-alternative");
        assertContains(methodIds(strategy), "reanimated-aviansie-alternative");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "kree-arra-alternative".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Kree'arra method"))
            .getSteps().stream().anyMatch(step -> step.contains("ecumenical key")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "kree-arra-alternative".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Kree'arra method"))
            .getNotes().stream().anyMatch(note -> note.contains("Eldritch nightmare staff")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "reanimated-aviansie-alternative".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing reanimated method"))
            .getNotes().stream().anyMatch(note -> note.contains("ensouled heads")));

        assertContains(styleIds(strategy), "ranged");
        assertContains(styleIds(strategy), "magic");
        assertContains(styleIds(strategy), "direct-exception");
    }

    @Test
    public void generatedRuntimeDataCarriesAviansieSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Aviansie".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Aviansie"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "God Wars Dungeon (Armadyl)".equals(location.getName())
            && location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness God Wars Dungeon".equals(location.getName())
            && location.isMulti() && !location.isCannon() && location.isWilderness()));

        MonsterVariant aviansie = task.getVariants().stream()
            .filter(variant -> "Aviansie".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Aviansie variant"));

        assertNotNull(aviansie.getStrategy());
        assertEquals(CombatStyle.RANGED, aviansie.getStrategy().getPrimaryStyle());
        assertEquals("Toxic blowpipe", aviansie.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(aviansie.getStrategy().getNote().contains("Watch the birdie"));
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        SourceLocation armadyl = read(Paths.get("src/main/data/slayer/locations/god-wars-dungeon-armadyl.json"),
            SourceLocation.class);
        SourceLocation wilderness = read(Paths.get("src/main/data/slayer/locations/wilderness-god-wars-dungeon.json"),
            SourceLocation.class);

        assertEquals("god-wars-dungeon-armadyl", armadyl.getLocationId());
        assertEquals("God Wars Dungeon (Armadyl)", armadyl.getName());
        assertTrue(armadyl.isMulti());
        assertFalse(armadyl.isCannon());
        assertFalse(armadyl.isSafeSpot());
        assertFalse(armadyl.isWilderness());
        assertTrue(armadyl.getAccessNote().contains("Death Plateau"));
        assertTrue(armadyl.getAccessNote().contains("Armadyl"));

        assertEquals("wilderness-god-wars-dungeon", wilderness.getLocationId());
        assertEquals("Wilderness God Wars Dungeon", wilderness.getName());
        assertTrue(wilderness.isMulti());
        assertFalse(wilderness.isCannon());
        assertFalse(wilderness.isSafeSpot());
        assertTrue(wilderness.isWilderness());
        assertTrue(wilderness.getAccessNote().contains("player killing"));
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

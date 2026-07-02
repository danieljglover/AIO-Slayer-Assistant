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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WC-2 (ADR-0019 recipe): pins the wiki-authored Jellies family sources. Masters, quantities, and
 * weights re-verified against the live Krystilia/Vannaka/Chaeldar/Konar pages on 2026-07-02 (PD-D);
 * slayerTargetId is SYNTHETIC (240 + WC index = 242) per the WC-1..11 allocation contract in team
 * memory. Location reuse/tuned copies follow docs/full-review/wc0-location-ownership.md verbatim.
 */
public class JelliesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/jellies.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298051), task.getWikiPageId());
        assertEquals(Integer.valueOf(57), task.getCombatLevel());
        assertEquals(242, task.getSlayerTargetId());
        assertEquals(52, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(Set.of("krystilia", "vannaka", "chaeldar", "konar"), masterIds(task));
        assertArrayEquals(new int[] {100, 150}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));

        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(10), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("konar"));

        assertNull("Jellies have no task-extension unlock", task.getExtendedAmount());
        assertEquals(Set.of("bigger-and-badder"), unlockIds(task));
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("fremennik-slayer-dungeon", "catacombs-of-kourend",
            "wilderness-slayer-cave-jellies", "ruins-of-tapoyauik-jellies"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("magic-based melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ice Burst") || note.contains("Ice Barrage")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia") && note.contains("Wilderness")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/jellies.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(6, variants.size());
        assertEquals(75.0, variants.get("jelly").getSlayerXp().doubleValue(), 0.0);
        assertEquals(140.0, variants.get("warped-jelly").getSlayerXp().doubleValue(), 0.0);
        assertEquals(140.0, variants.get("chilled-jelly").getSlayerXp().doubleValue(), 0.0);
        assertEquals(1900.0, variants.get("vitreous-jelly").getSlayerXp().doubleValue(), 0.0);
        assertEquals(2200.0, variants.get("vitreous-warped-jelly").getSlayerXp().doubleValue(), 0.0);
        assertEquals(2200.0, variants.get("vitreous-chilled-jelly").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("chilled-jelly").getNotes().stream()
            .anyMatch(note -> note.contains("fire")));

        assertEquals(4, locations.size());
        assertLocation(locations, "fremennik-slayer-dungeon", 10, false, false, true);
        assertLocation(locations, "catacombs-of-kourend", 18, true, false, true);
        assertLocation(locations, "wilderness-slayer-cave-jellies", 9, true, true, true);
        assertLocation(locations, "ruins-of-tapoyauik-jellies", 10, false, false, true);
    }

    @Test
    public void monsterSourcesCarryWikiStats() throws IOException
    {
        SourceMonsterVariant jelly = read(
            Paths.get("src/main/data/slayer/monsters/jellies/jelly-lvl78.json"), SourceMonsterVariant.class);
        assertEquals("jelly", jelly.getVariantId());
        assertTrue(jelly.getNpcIds().containsAll(Set.of(437, 438, 439, 440, 441, 442)));
        assertTrue("wilderness slayer cave jelly npc ids", jelly.getNpcIds().containsAll(
            Set.of(11241, 11242, 11243, 11244, 11245)));
        assertEquals(Integer.valueOf(78), jelly.getCombatLevel());
        assertEquals("earth", jelly.getWeakness().getElement());
        assertEquals(120, jelly.getMonsterDefence().getDefenceLevel());

        SourceMonsterVariant warped = read(
            Paths.get("src/main/data/slayer/monsters/jellies/warped-jelly-lvl112.json"), SourceMonsterVariant.class);
        assertEquals(Set.of(7277), warped.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(70, warped.getMonsterDefence().getDefenceLevel());

        SourceMonsterVariant chilled = read(
            Paths.get("src/main/data/slayer/monsters/jellies/chilled-jelly-lvl112.json"), SourceMonsterVariant.class);
        assertEquals(Set.of(13799), chilled.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals("fire", chilled.getWeakness().getElement());

        SourceMonsterVariant vitreous = read(
            Paths.get("src/main/data/slayer/monsters/jellies/vitreous-jelly-superior-lvl206.json"),
            SourceMonsterVariant.class);
        assertEquals(Set.of(7399), vitreous.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(220, vitreous.getMonsterDefence().getDefenceLevel());
    }

    @Test
    public void strategySourceCarriesMeleePrimaryWithMagicAndRangedAlternatives() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/jellies/strategy.json");
        assertTrue("Jellies strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("jellies", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Jellies", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("ghrazi-rapier"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "kodai-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "venator-bow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getNote().contains("Protect from Melee"));
    }

    @Test
    public void generatedRuntimeDataCarriesJelliesSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Jellies".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Jellies"));

        assertEquals(242, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia", "vannaka", "chaeldar", "konar"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Wilderness Slayer Cave".equals(location.getName()) && location.isWilderness() && location.isSafeSpot()));

        MonsterVariant jelly = task.getVariants().stream()
            .filter(variant -> "Jelly".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Jelly variant"));
        assertNotNull(jelly.getStrategy());
        assertEquals(CombatStyle.MELEE, jelly.getStrategy().getPrimaryStyle());

        assertEquals(6, task.getVariants().size());
    }

    @Test
    public void ownedLocationSourcesMatchTaskPage() throws IOException
    {
        SourceLocation wsc = read(
            Paths.get("src/main/data/slayer/locations/wilderness-slayer-cave-jellies.json"), SourceLocation.class);
        assertEquals("Wilderness Slayer Cave", wsc.getName());
        assertTrue(wsc.isMulti());
        assertTrue(wsc.isCannon());
        assertTrue(wsc.isSafeSpot());
        assertTrue(wsc.isWilderness());
        assertEquals(false, wsc.isKonarLockable());
        assertTrue(wsc.getAccessNote().contains("9 Jellies"));

        SourceLocation ruins = read(
            Paths.get("src/main/data/slayer/locations/ruins-of-tapoyauik-jellies.json"), SourceLocation.class);
        assertEquals("Ruins of Tapoyauik", ruins.getName());
        assertEquals(false, ruins.isMulti());
        assertEquals(false, ruins.isCannon());
        assertTrue(ruins.isSafeSpot());
        assertTrue(ruins.isKonarLockable());
        assertEquals(false, ruins.isWilderness());
        assertTrue(ruins.getAccessNote().contains("Chilled Jellies"));
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        int amount, boolean multi, boolean cannon, boolean safespot)
    {
        SourceTaskLocationComparison location = locations.get(locationId);
        assertNotNull(locationId, location);
        assertEquals(Integer.valueOf(amount), location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
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

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

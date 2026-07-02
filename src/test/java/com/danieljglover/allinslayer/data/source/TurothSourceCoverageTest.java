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
 * WC-1 (ADR-0019 recipe): pins the wiki-authored Turoth family sources. Masters, quantities, and
 * weights were re-verified against the live Vannaka/Chaeldar/Konar/Nieve pages on 2026-07-02
 * (PD-D); the slayerTargetId is SYNTHETIC (240 + WC index = 241) per the WC-1..11 allocation
 * contract in team memory - the real SLAYER_TARGET varp value is a QA live-verify item.
 */
public class TurothSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/turoth.json"), SourceTask.class);

        assertEquals(Integer.valueOf(343297), task.getWikiPageId());
        assertEquals(Integer.valueOf(60), task.getCombatLevel());
        assertEquals(241, task.getSlayerTargetId());
        assertEquals(55, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(Set.of("vannaka", "chaeldar", "konar", "nieve"), masterIds(task));
        assertArrayEquals(new int[] {30, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));

        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(10), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("konar"));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("nieve"));

        assertNull("Turoth has no task-extension unlock", task.getExtendedAmount());
        assertEquals(Set.of("bigger-and-badder"), unlockIds(task));

        assertNull(task.getRequiredItemId());
        assertTrue(task.getRequiredItemName().contains("leaf-bladed"));

        assertEquals(Set.of("fremennik-slayer-dungeon-turoths"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("leaf-bladed weapons")
            && note.contains("broad") && note.contains("Magic Dart")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("herb")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Spiked Turoth")
            && note.contains("Bigger and Badder")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/turoth.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertEquals(81.0, variants.get("turoth").getSlayerXp().doubleValue(), 0.0);
        assertEquals(1998.0, variants.get("spiked-turoth-superior").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("turoth").getNotes().stream()
            .anyMatch(note -> note.contains("83") && note.contains("89")));
        assertTrue(variants.get("spiked-turoth-superior").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));

        assertEquals(1, locations.size());
        SourceTaskLocationComparison fremennik = locations.get("fremennik-slayer-dungeon-turoths");
        assertEquals(Integer.valueOf(22), fremennik.getAmount());
        assertEquals(Boolean.FALSE, fremennik.getMulticombat());
        assertEquals(Boolean.FALSE, fremennik.getCannonable());
        assertEquals(Boolean.TRUE, fremennik.getSafespottable());
    }

    @Test
    public void monsterSourcesCarryWikiStatsAndLeafyRestriction() throws IOException
    {
        SourceMonsterVariant turoth = read(
            Paths.get("src/main/data/slayer/monsters/turoth/turoth-lvl89.json"), SourceMonsterVariant.class);
        assertEquals("turoth", turoth.getVariantId());
        assertEquals(Set.of(427, 428, 429, 430), turoth.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(89), turoth.getCombatLevel());
        assertEquals(CombatStyle.MELEE, turoth.getWeakness().getStyle());
        assertEquals(83, turoth.getMonsterDefence().getDefenceLevel());
        assertTrue(turoth.getRequirement().contains("leaf-bladed"));

        SourceMonsterVariant superior = read(
            Paths.get("src/main/data/slayer/monsters/turoth/spiked-turoth-superior-lvl244.json"),
            SourceMonsterVariant.class);
        assertEquals("spiked-turoth-superior", superior.getVariantId());
        assertEquals(Set.of(10397), superior.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(244), superior.getCombatLevel());
        assertEquals(154, superior.getMonsterDefence().getDefenceLevel());
    }

    @Test
    public void strategySourceCarriesLeafBladedPluginOverride() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/turoth/strategy.json");
        assertTrue("Turoth strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("turoth", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Turoth", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("leaf-bladed-battleaxe"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "rune-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getNote().contains("leafy"));
    }

    @Test
    public void generatedRuntimeDataCarriesTurothSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Turoth".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Turoth"));

        assertEquals(241, task.getSlayerTargetId());
        assertEquals(Set.of("vannaka", "chaeldar", "konar", "nieve"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(10), task.getWeightByMaster().get("chaeldar"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Fremennik Slayer Dungeon".equals(location.getName()) && location.isSafeSpot() && !location.isCannon()));

        MonsterVariant defaultVariant = task.getVariants().stream()
            .filter(variant -> "Turoth".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Turoth variant"));
        assertNotNull(defaultVariant.getStrategy());
        assertEquals(CombatStyle.MELEE, defaultVariant.getStrategy().getPrimaryStyle());
        assertEquals("Leaf-bladed battleaxe", defaultVariant.getStrategy().getPrimaryWeapons().get(0).getName());

        assertTrue(task.getVariants().stream()
            .anyMatch(variant -> variant.getName().contains("Spiked Turoth")));
    }

    @Test
    public void locationSourceMatchesTaskPage() throws IOException
    {
        SourceLocation location = read(
            Paths.get("src/main/data/slayer/locations/fremennik-slayer-dungeon-turoths.json"),
            SourceLocation.class);

        assertEquals("fremennik-slayer-dungeon-turoths", location.getLocationId());
        assertEquals("Fremennik Slayer Dungeon", location.getName());
        assertEquals(false, location.isMulti());
        assertEquals(false, location.isCannon());
        assertEquals(false, location.isBurst());
        assertEquals(true, location.isKonarLockable());
        assertEquals(true, location.isSafeSpot());
        assertEquals(false, location.isWilderness());
        assertTrue(location.getAccessNote().contains("22 Turoths"));
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

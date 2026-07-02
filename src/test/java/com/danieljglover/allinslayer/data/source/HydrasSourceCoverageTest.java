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
 * WC-5 (plan.md Wave C1, ADR-0019): pins the Hydras family to its wiki source, fetched live
 * 2026-07-02 from https://oldschool.runescape.wiki/w/Slayer_task/Hydras and the Hydra /
 * Colossal Hydra / Alchemical Hydra monster pages. Konar-only assignment (125-190, weight 10),
 * no extension unlock exists for this family.
 */
public class HydrasSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/hydras.json"), SourceTask.class);

        assertEquals(Integer.valueOf(297876), task.getWikiPageId());
        assertNull("wiki lists no combat level requirement for Hydras", task.getCombatLevel());
        assertEquals(245, task.getSlayerTargetId());
        assertEquals(95, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(Set.of("konar"), masterIds(task));
        assertArrayEquals(new int[] {125, 190}, task.getAmountByMaster().get("konar"));
        assertEquals(Integer.valueOf(10), task.getWeightByMaster().get("konar"));
        assertNull("no extension unlock exists for Hydras on the wiki", task.getExtendedAmount());

        assertEquals(Set.of("bigger-and-badder"), unlockIds(task));
        assertTrue(task.getUnlocks().stream().noneMatch(unlock -> unlock.getType() != null));

        assertEquals(Integer.valueOf(21647), task.getRequiredItemId());
        assertEquals("Boots of stone", task.getRequiredItemName());

        assertEquals(Set.of("karuulm-slayer-dungeon-hydras"), locationIds(task));

        assertTrue("Hydras are draconic (dragonbane applies)", task.isDragon());
        assertTrue(task.isSlayerHelmApplies());
        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());
        assertEquals(100, task.getMonsterDefence().getDefenceLevel());
        assertEquals(0, task.getMonsterDefence().getRange());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Konar")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Boots of stone")
            || note.contains("boots of stone")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("poison")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Alchemical Hydra")
            && note.contains("on a hydra Slayer task")));
        assertTrue("synthetic target id must be flagged honestly",
            task.getTaskNotes().stream().anyMatch(note -> note.contains("245") && note.contains("synthetic")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/hydras.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(3, variants.size());
        assertEquals(322.5, variants.get("hydra").getSlayerXp().doubleValue(), 0.0);
        assertEquals(8625.0, variants.get("colossal-hydra").getSlayerXp().doubleValue(), 0.0);
        assertEquals(1320.0, variants.get("hydras-alchemical-hydra").getSlayerXp().doubleValue(), 0.0);
        assertEquals(Integer.valueOf(194), variants.get("hydra").getCombatLevel());
        assertEquals(Integer.valueOf(309), variants.get("colossal-hydra").getCombatLevel());
        assertEquals(Integer.valueOf(426), variants.get("hydras-alchemical-hydra").getCombatLevel());
        assertTrue(variants.get("colossal-hydra").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));
        assertTrue(variants.get("hydras-alchemical-hydra").getNotes().stream()
            .anyMatch(note -> note.contains("on a hydra Slayer task")));

        assertEquals(1, locations.size());
        SourceTaskLocationComparison karuulm = locations.get("karuulm-slayer-dungeon-hydras");
        assertEquals(Integer.valueOf(17), karuulm.getAmount());
        assertEquals(Boolean.FALSE, karuulm.getMulticombat());
        assertEquals(Boolean.TRUE, karuulm.getCannonable());
        assertEquals(Boolean.FALSE, karuulm.getSafespottable());
    }

    @Test
    public void monsterSourcesCarryWikiStats() throws IOException
    {
        SourceMonsterVariant hydra = read(
            Paths.get("src/main/data/slayer/monsters/hydras/hydra-lvl194.json"), SourceMonsterVariant.class);
        assertEquals("hydra", hydra.getVariantId());
        assertTrue(hydra.getNpcIds().contains(8609));
        assertEquals(Integer.valueOf(194), hydra.getCombatLevel());
        assertEquals(CombatStyle.RANGED, hydra.getWeakness().getStyle());
        assertEquals("earth", hydra.getWeakness().getElement());
        assertEquals(100, hydra.getMonsterDefence().getDefenceLevel());
        assertEquals(160, hydra.getMonsterDefence().getStab());
        assertEquals(160, hydra.getMonsterDefence().getMagic());
        assertEquals(0, hydra.getMonsterDefence().getRange());
        assertTrue(hydra.isDragon());
        assertTrue(!hydra.isBoss());
        assertEquals("hydras", hydra.getStrategyId());

        SourceMonsterVariant colossal = read(
            Paths.get("src/main/data/slayer/monsters/hydras/colossal-hydra-lvl309.json"), SourceMonsterVariant.class);
        assertEquals("colossal-hydra", colossal.getVariantId());
        assertTrue(colossal.getNpcIds().contains(10402));
        assertEquals(100, colossal.getMonsterDefence().getDefenceLevel());
        assertEquals(100, colossal.getMonsterDefence().getStab());
        assertEquals(200, colossal.getMonsterDefence().getSlash());
        assertEquals(20, colossal.getMonsterDefence().getRange());
        assertNull("wiki: Colossal Hydra has no elemental weakness", colossal.getWeakness().getElement());
        assertTrue(colossal.isDragon());

        SourceMonsterVariant alch = read(
            Paths.get("src/main/data/slayer/monsters/hydras/hydras-alchemical-hydra-lvl426.json"),
            SourceMonsterVariant.class);
        assertEquals("hydras-alchemical-hydra", alch.getVariantId());
        assertTrue(alch.getNpcIds().contains(8615));
        assertTrue(alch.isBoss());
        assertTrue(alch.isDragon());
        assertEquals("alchemical-hydra", alch.getStrategyId());
    }

    @Test
    public void strategySourceCoversTaskPageMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/hydras/strategy.json");
        assertTrue("Hydras strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("hydras", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Hydras", strategy.getSourceUrl());
        assertTrue(strategy.getVariantIds().contains("hydra"));
        assertTrue(strategy.getVariantIds().contains("colossal-hydra"));
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-crossbow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-lance".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "prayer-switching");
        assertContains(methodIds(strategy), "poison-handling");
        assertContains(methodIds(strategy), "alchemical-hydra-alternative");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "prayer-switching".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing prayer-switching method"))
            .getSteps().stream().anyMatch(step -> step.contains("three")));
    }

    @Test
    public void generatedRuntimeDataCarriesHydrasSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Hydras".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Hydras"));

        assertEquals(Set.of("konar"), task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(10), task.getWeightByMaster().get("konar"));
        assertNull(task.getExtendedAmount());
        assertEquals(Integer.valueOf(21647), task.getRequiredItemId());
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Karuulm Slayer Dungeon".equals(location.getName()) && location.isCannon()
                && !location.isMulti() && location.isKonarLockable()));

        assertEquals(3, task.getVariants().size());
        MonsterVariant hydra = task.getVariants().stream()
            .filter(variant -> "Hydra".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Hydra variant"));
        assertTrue(hydra.isDefault());
        assertNotNull(hydra.getStrategy());
        assertEquals(CombatStyle.RANGED, hydra.getStrategy().getPrimaryStyle());
        assertEquals("Dragon hunter crossbow", hydra.getStrategy().getPrimaryWeapons().get(0).getName());

        MonsterVariant alch = task.getVariants().stream()
            .filter(variant -> "Alchemical Hydra".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Alchemical Hydra variant"));
        assertTrue(alch.isBoss());
        assertNotNull(alch.getStrategy());
    }

    @Test
    public void locationSourceMatchesTaskPage() throws IOException
    {
        SourceLocation location = read(
            Paths.get("src/main/data/slayer/locations/karuulm-slayer-dungeon-hydras.json"), SourceLocation.class);

        assertEquals("karuulm-slayer-dungeon-hydras", location.getLocationId());
        assertEquals("Karuulm Slayer Dungeon", location.getName());
        assertEquals(false, location.isMulti());
        assertEquals(true, location.isCannon());
        assertEquals(false, location.isBurst());
        assertEquals(true, location.isKonarLockable());
        assertEquals(false, location.isSafeSpot());
        assertEquals(false, location.isWilderness());
        assertTrue(location.getAccessNote().contains("Boots of stone")
            || location.getAccessNote().contains("boots of stone"));
        assertTrue(location.getAccessNote().contains("Elite Kourend & Kebos"));
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

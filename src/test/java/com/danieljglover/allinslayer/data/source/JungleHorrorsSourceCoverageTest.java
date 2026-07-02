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

/**
 * WC-10 (ADR-0019): Jungle horrors family coverage. Every number below was re-verified against
 * the live wiki on 2026-07-02 (Jungle_horror monster page - the family has no Slayer_task/
 * subpage - plus the Vannaka and Chaeldar task tables, PD-D).
 */
public class JungleHorrorsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/jungle-horrors.json"), SourceTask.class);

        assertEquals(Integer.valueOf(15660), task.getWikiPageId());
        assertEquals(Integer.valueOf(65), task.getCombatLevel());
        // Synthetic id: 240 + WC index 10 (team contract 2026-07-02, Frost-Dragons precedent) -
        // the real SLAYER_TARGET varp for Jungle horrors is not wiki-published; see taskNotes.
        assertEquals(250, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Cabin Fever"));

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(10), task.getWeightByMaster().get("chaeldar"));
        // Not extendable (wiki: N/A for both assigners), so no extendedAmount and no EXTENSION unlock.
        assertNull(task.getExtendedAmount());
        assertTrue(task.getUnlocks() == null || task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "mos-le-harmless");
        assertEquals("jungle-horror", task.getDefaultVariantId());
        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isUndead());
        assertFalse(task.isDemon());
        assertFalse(task.isDragon());
        assertFalse(task.isKalphite());
        assertNull(task.getRequiredItemId());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Cabin Fever")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fire spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("pineapple")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/jungle-horrors.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));

        assertEquals(1, variants.size());
        SourceTaskVariantInfo horror = variants.get("jungle-horror");
        assertEquals(Integer.valueOf(70), horror.getCombatLevel());
        assertEquals(45.0, horror.getSlayerXp().doubleValue(), 0.0);
        assertTrue(horror.getLocations().contains("Mos Le'Harmless"));

        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));
        assertEquals(1, locations.size());
        SourceTaskLocationComparison island = locations.get("mos-le-harmless");
        assertEquals(Boolean.TRUE, island.getSafespottable());
        assertTrue(island.getNotes().stream().anyMatch(note -> note.contains("50")));
    }

    @Test
    public void monsterVariantMatchesWikiStats() throws IOException
    {
        SourceMonsterVariant horror = read(
            Paths.get("src/main/data/slayer/monsters/jungle-horrors/jungle-horror-lvl70.json"),
            SourceMonsterVariant.class);

        assertEquals("jungle-horror", horror.getVariantId());
        assertEquals(Integer.valueOf(70), horror.getCombatLevel());
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(1042, 1043, 1044, 1045, 1046)),
            new java.util.HashSet<>(horror.getNpcIds()));
        assertEquals(55, horror.getMonsterDefence().getDefenceLevel());
        assertEquals(0, horror.getMonsterDefence().getSlash());
        assertEquals("fire", horror.getWeakness().getElement());
        assertFalse(horror.isBoss());
        assertEquals("jungle-horrors", horror.getStrategyId());
    }

    @Test
    public void locationSourceMatchesOwnershipContract() throws IOException
    {
        // WC-0 ownership doc: mos-le-harmless is unique to WC-10. safeSpot wiki-verified true;
        // multi/cannon not wiki-verifiable for the island surface -> rule 7 conservative defaults
        // with the honesty note in accessNote.
        SourceLocation island = read(Paths.get("src/main/data/slayer/locations/mos-le-harmless.json"),
            SourceLocation.class);

        assertEquals("mos-le-harmless", island.getLocationId());
        assertEquals("Mos Le'Harmless", island.getName());
        assertFalse(island.isMulti());
        assertFalse(island.isCannon());
        assertFalse(island.isBurst());
        assertFalse(island.isKonarLockable());
        assertTrue(island.isSafeSpot());
        assertFalse(island.isWilderness());
        assertTrue(island.getAccessNote().contains("Cabin Fever"));
        assertTrue(island.getAccessNote().contains("unverified"));
    }

    @Test
    public void strategySourceCarriesPluginWeaponsAndMethods() throws IOException
    {
        SourceStrategy strategy = read(
            Paths.get("src/main/data/slayer/strategies/jungle-horrors/strategy.json"),
            SourceStrategy.class);

        assertEquals("jungle-horrors", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Jungle_horror", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "harmonised-nightmare-staff".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "safespot");
    }

    @Test
    public void generatedRuntimeDataCarriesJungleHorrors() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Jungle horrors".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Jungle horrors"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(10), task.getWeightByMaster().get("chaeldar"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Mos Le'Harmless".equals(location.getName())
            && location.isSafeSpot() && !location.isWilderness()));

        MonsterVariant horror = task.getVariants().stream()
            .filter(variant -> "Jungle horror".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Jungle horror variant"));
        assertNotNull(horror.getStrategy());
        assertEquals(CombatStyle.MELEE, horror.getStrategy().getPrimaryStyle());
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("missing " + expected + " in " + values, values.contains(expected));
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
        return strategy.getMethods().stream().map(SourceStrategyMethod::getMethodId)
            .collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

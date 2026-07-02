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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C2-D (ADR-0019 recipe): pins the wiki-authored Moss giants family (Krystilia Wilderness-only).
 * Quantity/weight from the live Krystilia page (100-150, weight 4); monster stats from the live
 * Moss giant page (Cb 42, 60 HP, Defence 30, 50% Fire weakness). slayerTargetId 266 is SYNTHETIC
 * per the C2/C3 allocation contract (worklist §1). Location flags are honest-UNKNOWN conservative
 * false (the wiki documents no multicombat/cannon/safespot detail for the Wilderness spawn).
 */
public class MossGiantsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/moss-giants.json"), SourceTask.class);

        assertEquals(Integer.valueOf(42), task.getCombatLevel());
        assertEquals(266, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(Set.of("krystilia"), masterIds(task));
        assertArrayEquals(new int[] {100, 150}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));

        assertNull("Moss giants have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Moss giants have no superior or extension unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("moss-giants-wilderness"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Fire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/moss-giants.json"), SourceTask.class);

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo mossGiant = task.getVariantInfo().get(0);
        assertEquals("moss-giant", mossGiant.getVariantId());
        assertEquals(60.0, mossGiant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        SourceTaskLocationComparison wildy = task.getLocationComparison().get(0);
        assertEquals("moss-giants-wilderness", wildy.getLocationId());
        assertEquals(Boolean.FALSE, wildy.getMulticombat());
        assertEquals(Boolean.FALSE, wildy.getCannonable());
    }

    @Test
    public void monsterSourceCarriesWikiStats() throws IOException
    {
        SourceMonsterVariant mossGiant = read(
            Paths.get("src/main/data/slayer/monsters/moss-giants/moss-giant-lvl42.json"), SourceMonsterVariant.class);
        assertEquals("moss-giant", mossGiant.getVariantId());
        assertTrue(mossGiant.getNpcIds().contains(2090));
        assertEquals(Integer.valueOf(42), mossGiant.getCombatLevel());
        assertEquals(CombatStyle.MAGIC, mossGiant.getWeakness().getStyle());
        assertEquals("fire", mossGiant.getWeakness().getElement());
        assertEquals(30, mossGiant.getMonsterDefence().getDefenceLevel());
    }

    @Test
    public void strategySourceResolves() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/moss-giants/strategy.json");
        assertTrue("Moss giants strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("moss-giants", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Moss_giant", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));

        SourceWeapon scim = read(Paths.get("src/main/data/slayer/weapons/dragon-scimitar.json"), SourceWeapon.class);
        assertEquals("dragon-scimitar", scim.getWeaponId());
    }

    @Test
    public void generatedRuntimeDataCarriesMossGiantsSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Moss giants".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Moss giants"));

        assertEquals(266, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia"), task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Deep Wilderness".equals(location.getName()) && location.isWilderness()));

        MonsterVariant mossGiant = task.getVariants().stream()
            .filter(variant -> "Moss giant".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Moss giant variant"));
        assertNotNull(mossGiant.getStrategy());
        assertEquals(CombatStyle.MELEE, mossGiant.getStrategy().getPrimaryStyle());
    }

    @Test
    public void ownedLocationSourceMatchesWiki() throws IOException
    {
        SourceLocation wildy = read(
            Paths.get("src/main/data/slayer/locations/moss-giants-wilderness.json"), SourceLocation.class);

        assertEquals("moss-giants-wilderness", wildy.getLocationId());
        assertEquals("Deep Wilderness", wildy.getName());
        assertEquals(false, wildy.isMulti());
        assertEquals(false, wildy.isCannon());
        assertEquals(false, wildy.isBurst());
        assertEquals(false, wildy.isKonarLockable());
        assertEquals(false, wildy.isSafeSpot());
        assertEquals(true, wildy.isWilderness());
        assertTrue(wildy.getAccessNote().contains("Wilderness"));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
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

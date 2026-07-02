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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C2-D (ADR-0019 recipe): pins the wiki-authored Pirates family sources. Krystilia-only Wilderness
 * task, quantity 62-75 weight 3, re-verified against the live Krystilia page 2026-07-02 (PD-D). The
 * main assignable pirates are in the Pirates' Hideout (Wilderness level 54, lockpick + 39 Thieving);
 * undead Zombie pirates at the Chaos Temple also count. slayerTargetId is SYNTHETIC (267) per the
 * C2/C3 allocation contract in the worklist.
 */
public class PiratesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/pirates.json"), SourceTask.class);

        assertEquals(267, task.getSlayerTargetId());
        assertEquals(Integer.valueOf(26), task.getCombatLevel());

        assertEquals(Set.of("krystilia"), masterIds(task));
        assertArrayEquals(new int[] {62, 75}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));

        assertNull("Pirates have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Pirates have no superior or extension unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("pirates-hideout", "chaos-temple-wilderness"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isUndead());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("lockpick")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void taskSourceContainsWikiVariantRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/pirates.json"), SourceTask.class);

        assertEquals(2, task.getVariantInfo().size());
        assertTrue(task.getVariantInfo().stream().anyMatch(v -> "pirate".equals(v.getVariantId())));
        assertTrue(task.getVariantInfo().stream().anyMatch(v -> "zombie-pirate".equals(v.getVariantId())));

        SourceTaskLocationComparison hideout = task.getLocationComparison().stream()
            .filter(c -> "pirates-hideout".equals(c.getLocationId()))
            .findFirst().orElseThrow(() -> new AssertionError("missing pirates-hideout comparison"));
        assertEquals(Boolean.FALSE, hideout.getMulticombat());
        assertEquals(Boolean.FALSE, hideout.getCannonable());
    }

    @Test
    public void monsterSourcesCarryWikiStats() throws IOException
    {
        SourceMonsterVariant pirate = read(
            Paths.get("src/main/data/slayer/monsters/pirates/pirate-lvl26.json"), SourceMonsterVariant.class);
        assertEquals("pirate", pirate.getVariantId());
        assertEquals(Set.of(523, 6995), pirate.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(26), pirate.getCombatLevel());
        assertEquals(23, pirate.getMonsterDefence().getDefenceLevel());
        assertFalse(pirate.isUndead());

        SourceMonsterVariant zombiePirate = read(
            Paths.get("src/main/data/slayer/monsters/pirates/zombie-pirate-lvl34.json"), SourceMonsterVariant.class);
        assertEquals("zombie-pirate", zombiePirate.getVariantId());
        assertTrue("Zombie pirates are undead", zombiePirate.isUndead());
        assertEquals(CombatStyle.MAGIC, zombiePirate.getWeakness().getStyle());
    }

    @Test
    public void strategySourceCarriesMeleeWeapon() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/pirates/strategy.json");
        assertTrue("Pirates strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("pirates", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));

        SourceWeapon scimitar = read(
            Paths.get("src/main/data/slayer/weapons/dragon-scimitar.json"), SourceWeapon.class);
        assertEquals("dragon-scimitar", scimitar.getWeaponId());
    }

    @Test
    public void ownedLocationSourcesAreWildernessAndComplete() throws IOException
    {
        SourceLocation hideout = read(
            Paths.get("src/main/data/slayer/locations/pirates-hideout.json"), SourceLocation.class);
        assertEquals("pirates-hideout", hideout.getLocationId());
        assertEquals(true, hideout.isWilderness());
        assertEquals(false, hideout.isKonarLockable());
        assertTrue(hideout.getAccessNote().contains("lockpick"));

        SourceLocation chaosTemple = read(
            Paths.get("src/main/data/slayer/locations/chaos-temple-wilderness.json"), SourceLocation.class);
        assertEquals("chaos-temple-wilderness", chaosTemple.getLocationId());
        assertEquals(true, chaosTemple.isWilderness());
        assertEquals(false, chaosTemple.isKonarLockable());
    }

    @Test
    public void generatedRuntimeDataCarriesPiratesSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Pirates".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Pirates"));

        assertEquals(267, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia"), task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Pirates' Hideout".equals(location.getName()) && location.isWilderness()));

        MonsterVariant pirate = task.getVariants().stream()
            .filter(variant -> "Pirate".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Pirate variant"));
        assertNotNull(pirate.getStrategy());
        assertEquals(CombatStyle.MELEE, pirate.getStrategy().getPrimaryStyle());
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

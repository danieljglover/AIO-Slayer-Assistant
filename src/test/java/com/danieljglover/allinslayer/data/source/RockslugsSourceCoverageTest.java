package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.AttackStyle;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C3-C (ADR-0019 recipe): pins the wiki-authored Rockslugs family. Mazchna starter-tier task,
 * Slayer level 20, requires a bag of salt (or brine sabre) to kill. Stats from the live Rockslug
 * wiki page, fetched 2026-07-02. slayerTargetId is SYNTHETIC (301) per the C2/C3 allocation
 * contract (worklist §1). Locations: a family-tuned copy of the fremennik-slayer-dungeon base
 * (basilisk flags do not apply) plus the shared cave-bugs-owned lumbridge-swamp-caves.
 */
public class RockslugsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/rockslugs.json"), SourceTask.class);

        assertEquals(301, task.getSlayerTargetId());
        assertEquals(20, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));

        assertNull("Rockslugs have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Rockslugs have no unlocks", task.getUnlocks().isEmpty());
        assertNull("bag of salt item id is honest-UNKNOWN", task.getRequiredItemId());
        assertEquals("Bag of salt", task.getRequiredItemName());

        assertEquals(Set.of("fremennik-slayer-dungeon-rockslugs", "lumbridge-swamp-caves"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("bag of salt")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant rockslug = read(
            Paths.get("src/main/data/slayer/monsters/rockslugs/rockslug.json"), SourceMonsterVariant.class);
        assertEquals("rockslug", rockslug.getVariantId());
        assertTrue(rockslug.getNpcIds().contains(421));
        assertEquals(CombatStyle.MELEE, rockslug.getWeakness().getStyle());
        assertEquals("earth", rockslug.getWeakness().getElement());
        assertEquals(27, rockslug.getMonsterDefence().getDefenceLevel());

        assertNotNull("rockslug offence authored", rockslug.getOffence());
        assertEquals(Integer.valueOf(27), rockslug.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(4), rockslug.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE), rockslug.getOffence().getAttackStyles());
        assertNull("magicLevel is honest-UNKNOWN", rockslug.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/rockslugs/strategy.json");
        assertTrue("Rockslugs strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("rockslugs", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void ownedLocationSourceMatchesWiki() throws IOException
    {
        SourceLocation dungeon = read(
            Paths.get("src/main/data/slayer/locations/fremennik-slayer-dungeon-rockslugs.json"), SourceLocation.class);
        assertEquals("fremennik-slayer-dungeon-rockslugs", dungeon.getLocationId());
        assertEquals(false, dungeon.isWilderness());
        assertEquals(false, dungeon.isKonarLockable());
    }

    @Test
    public void generatedRuntimeDataResolvesRockslugs() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Rockslugs".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Rockslugs"));

        assertEquals(301, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant rockslug = task.getVariants().stream()
            .filter(variant -> "Rockslug".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Rockslug variant"));
        assertNotNull(rockslug.getStrategy());
        assertNotNull("offence compiled onto runtime variant", rockslug.getOffence());
        assertEquals(Integer.valueOf(27), rockslug.getOffence().getHitpoints());
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

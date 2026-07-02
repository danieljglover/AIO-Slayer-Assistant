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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C3-B (ADR-0019 recipe): pins the wiki-authored Pyrefiends family. Mazchna-assigned starter-tier
 * task requiring 30 Slayer. Pyrefiends are demons (demonbane applies) with a 100% water weakness.
 * Primary location is a tuned copy of the Fremennik Slayer Dungeon
 * ({@code fremennik-slayer-dungeon-pyrefiends}, konarLockable:false); the existing {@code smoke-dungeon}
 * base is referenced by id as the cannon alternative (CT-L reuse rule). Vannaka also assigns pyrefiends
 * on the live wiki but is outside the C3 starter scope (sibling banshees precedent). Stats from the
 * live Pyrefiend wiki page, fetched 2026-07-02. slayerTargetId is SYNTHETIC (299) per the C2/C3
 * allocation contract.
 */
public class PyrefiendsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/pyrefiends.json"), SourceTask.class);

        assertEquals(299, task.getSlayerTargetId());
        assertEquals(Integer.valueOf(25), task.getCombatLevel());
        assertEquals(30, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));

        assertNull("Pyrefiends have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Pyrefiends have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(
            Set.of("fremennik-slayer-dungeon-pyrefiends", "smoke-dungeon"), locationIds(task));

        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("water", task.getWeakness().getElement());

        assertTrue("pyrefiends are demons", task.isDemon());
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("water")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant pyrefiend = read(
            Paths.get("src/main/data/slayer/monsters/pyrefiends/pyrefiend.json"),
            SourceMonsterVariant.class);
        assertEquals("pyrefiend", pyrefiend.getVariantId());
        assertTrue(pyrefiend.getNpcIds().contains(433));
        assertEquals(CombatStyle.MAGIC, pyrefiend.getWeakness().getStyle());
        assertEquals("water", pyrefiend.getWeakness().getElement());
        assertEquals(22, pyrefiend.getMonsterDefence().getDefenceLevel());
        assertEquals(0, pyrefiend.getMonsterDefence().getMagic());

        assertNotNull("pyrefiend offence authored", pyrefiend.getOffence());
        assertEquals(Integer.valueOf(45), pyrefiend.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(4), pyrefiend.getOffence().getMaxHit());
        assertEquals(Integer.valueOf(4), pyrefiend.getOffence().getAttackSpeedTicks());
        assertEquals(List.of(AttackStyle.MAGIC), pyrefiend.getOffence().getAttackStyles());
        assertEquals("magic level for Twisted-bow scaling", Integer.valueOf(1),
            pyrefiend.getOffence().getMagicLevel());
        assertFalse("pyrefiends are not poisonous", pyrefiend.getOffence().isPoisonous());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/pyrefiends/strategy.json");
        assertTrue("Pyrefiends strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("pyrefiends", strategy.getStrategyId());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void tunedLocationSourceMatchesWiki() throws IOException
    {
        SourceLocation dungeon = read(
            Paths.get("src/main/data/slayer/locations/fremennik-slayer-dungeon-pyrefiends.json"),
            SourceLocation.class);
        assertEquals("fremennik-slayer-dungeon-pyrefiends", dungeon.getLocationId());
        assertEquals(false, dungeon.isWilderness());
        assertEquals("C2/C3 files are never konar-lockable", false, dungeon.isKonarLockable());
        assertEquals(true, dungeon.isSafeSpot());
    }

    @Test
    public void generatedRuntimeDataResolvesPyrefiends() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Pyrefiends".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Pyrefiends"));

        assertEquals(299, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant pyrefiend = task.getVariants().stream()
            .filter(variant -> "Pyrefiend".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Pyrefiend variant"));
        assertNotNull(pyrefiend.getStrategy());
        assertEquals(CombatStyle.MAGIC, pyrefiend.getStrategy().getPrimaryStyle());
        assertNotNull("offence compiled onto runtime variant", pyrefiend.getOffence());
        assertEquals(Integer.valueOf(45), pyrefiend.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), pyrefiend.getOffence().getMagicLevel());
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

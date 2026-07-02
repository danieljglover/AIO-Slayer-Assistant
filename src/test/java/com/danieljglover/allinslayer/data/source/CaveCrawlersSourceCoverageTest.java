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
 * C3-B (ADR-0019 recipe): pins the wiki-authored Cave crawlers family. Starter-tier
 * Turael/Spria/Mazchna task requiring 10 Slayer. Cave crawlers use a tuned copy of the shared
 * Fremennik Slayer Dungeon location ({@code fremennik-slayer-dungeon-cave-crawlers},
 * konarLockable:false vs the konar-lockable Basilisk base) and reference the cave-bugs-owned
 * {@code lumbridge-swamp-caves} base (CT-L doc) as the alternative light-source location. Stats from
 * the live Cave crawler wiki page, fetched 2026-07-02. slayerTargetId is SYNTHETIC (280) per the
 * C2/C3 allocation contract.
 */
public class CaveCrawlersSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cave-crawlers.json"), SourceTask.class);

        assertEquals(280, task.getSlayerTargetId());
        assertEquals(Integer.valueOf(10), task.getCombatLevel());
        assertEquals(10, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("turael", "spria", "mazchna"), masterIds(task));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));

        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));

        assertNull("Cave crawlers have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Cave crawlers have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(
            Set.of("fremennik-slayer-dungeon-cave-crawlers", "lumbridge-swamp-caves"), locationIds(task));

        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull("standard cave crawlers have no elemental weakness", task.getWeakness().getElement());

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("poison")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant crawler = read(
            Paths.get("src/main/data/slayer/monsters/cave-crawlers/cave-crawler.json"),
            SourceMonsterVariant.class);
        assertEquals("cave-crawler", crawler.getVariantId());
        assertTrue(crawler.getNpcIds().contains(406));
        assertEquals(CombatStyle.MELEE, crawler.getWeakness().getStyle());
        assertNull("no elemental weakness on the standard variant", crawler.getWeakness().getElement());
        assertEquals(18, crawler.getMonsterDefence().getDefenceLevel());

        assertNotNull("cave crawler offence authored", crawler.getOffence());
        assertEquals(Integer.valueOf(22), crawler.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(3), crawler.getOffence().getMaxHit());
        assertEquals(Integer.valueOf(4), crawler.getOffence().getAttackSpeedTicks());
        assertEquals(List.of(AttackStyle.MELEE), crawler.getOffence().getAttackStyles());
        assertTrue("cave crawlers are poisonous", crawler.getOffence().isPoisonous());
        assertNull("magicLevel is honest-UNKNOWN", crawler.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/cave-crawlers/strategy.json");
        assertTrue("Cave crawlers strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("cave-crawlers", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void tunedLocationSourceMatchesWiki() throws IOException
    {
        SourceLocation dungeon = read(
            Paths.get("src/main/data/slayer/locations/fremennik-slayer-dungeon-cave-crawlers.json"),
            SourceLocation.class);
        assertEquals("fremennik-slayer-dungeon-cave-crawlers", dungeon.getLocationId());
        assertEquals(false, dungeon.isWilderness());
        assertEquals("C2/C3 files are never konar-lockable", false, dungeon.isKonarLockable());
        assertEquals("no light source needed here, unlike the swamp caves", true, dungeon.isSafeSpot());
    }

    @Test
    public void generatedRuntimeDataResolvesCaveCrawlers() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Cave crawlers".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave crawlers"));

        assertEquals(280, task.getSlayerTargetId());
        assertEquals(Set.of("turael", "spria", "mazchna"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant crawler = task.getVariants().stream()
            .filter(variant -> "Cave crawler".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave crawler variant"));
        assertNotNull(crawler.getStrategy());
        assertEquals(CombatStyle.MELEE, crawler.getStrategy().getPrimaryStyle());
        assertNotNull("offence compiled onto runtime variant", crawler.getOffence());
        assertEquals(Integer.valueOf(22), crawler.getOffence().getHitpoints());
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

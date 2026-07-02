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
 * C3-C (ADR-0019 recipe): pins the wiki-authored Wall beasts family. Mazchna starter-tier task,
 * Slayer level 35, requires a spiny helmet (or Slayer helmet) to fight safely. Stats from the live
 * Wall beast wiki page, fetched 2026-07-02. slayerTargetId is SYNTHETIC (304) per the C2/C3
 * allocation contract. Location: the shared cave-bugs-owned lumbridge-swamp-caves, referenced by id.
 */
public class WallBeastsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/wall-beasts.json"), SourceTask.class);

        assertEquals(304, task.getSlayerTargetId());
        assertEquals(35, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Wall beasts have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Wall beasts have no unlocks", task.getUnlocks().isEmpty());
        assertNull("spiny helmet item id is honest-UNKNOWN", task.getRequiredItemId());
        assertEquals("Spiny helmet", task.getRequiredItemName());

        assertEquals(Set.of("lumbridge-swamp-caves"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("spiny helmet")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant beast = read(
            Paths.get("src/main/data/slayer/monsters/wall-beasts/wall-beast.json"), SourceMonsterVariant.class);
        assertEquals("wall-beast", beast.getVariantId());
        assertTrue(beast.getNpcIds().contains(476));
        assertEquals(CombatStyle.MELEE, beast.getWeakness().getStyle());
        assertNull("no elemental weakness (honest null)", beast.getWeakness().getElement());
        assertEquals(16, beast.getMonsterDefence().getDefenceLevel());

        assertNotNull("wall beast offence authored", beast.getOffence());
        assertEquals(Integer.valueOf(105), beast.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(18), beast.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE), beast.getOffence().getAttackStyles());
        assertNull("magicLevel is honest-UNKNOWN", beast.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/wall-beasts/strategy.json");
        assertTrue("Wall beasts strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("wall-beasts", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void generatedRuntimeDataResolvesWallBeasts() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Wall beasts".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Wall beasts"));

        assertEquals(304, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant beast = task.getVariants().stream()
            .filter(variant -> "Wall beast".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Wall beast variant"));
        assertNotNull(beast.getStrategy());
        assertNotNull("offence compiled onto runtime variant", beast.getOffence());
        assertEquals(Integer.valueOf(18), beast.getOffence().getMaxHit());
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

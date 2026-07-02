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
 * C2-E (ADR-0019 recipe): pins the wiki-authored Scorpions family. SHARED family (worklist §5.4) -
 * authored once with Krystilia (Wilderness) + Turael/Spria/Mazchna starter masters and both
 * Wilderness and non-Wilderness variant/location sets. Stats from the live Scorpion / King scorpion
 * wiki pages, fetched 2026-07-02. slayerTargetId is SYNTHETIC (270) per the C2/C3 allocation
 * contract (worklist §1).
 */
public class ScorpionsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/scorpions.json"), SourceTask.class);

        assertEquals(270, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("krystilia", "turael", "spria", "mazchna"), masterIds(task));
        assertArrayEquals(new int[] {65, 100}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));

        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Scorpions have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Scorpions have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("scorpions-wilderness", "scorpions-area"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia")));
    }

    @Test
    public void monsterSourcesCarryWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant wildy = read(
            Paths.get("src/main/data/slayer/monsters/scorpions/scorpion-wilderness.json"), SourceMonsterVariant.class);
        assertEquals("scorpion-wilderness", wildy.getVariantId());
        assertTrue(wildy.getNpcIds().contains(3024));
        assertEquals(CombatStyle.MELEE, wildy.getWeakness().getStyle());
        assertEquals("fire", wildy.getWeakness().getElement());
        assertNotNull("scorpion offence authored", wildy.getOffence());
        assertEquals(List.of(AttackStyle.MELEE), wildy.getOffence().getAttackStyles());
        assertNull("HP is honest-UNKNOWN for the level 14 scorpion", wildy.getOffence().getHitpoints());

        SourceMonsterVariant king = read(
            Paths.get("src/main/data/slayer/monsters/scorpions/king-scorpion.json"), SourceMonsterVariant.class);
        assertEquals(Set.of(3027), king.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(30), king.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(4), king.getOffence().getMaxHit());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/scorpions/strategy.json");
        assertTrue("Scorpions strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("scorpions", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void ownedLocationSourcesMatchWiki() throws IOException
    {
        SourceLocation wildy = read(
            Paths.get("src/main/data/slayer/locations/scorpions-wilderness.json"), SourceLocation.class);
        assertEquals("scorpions-wilderness", wildy.getLocationId());
        assertEquals(true, wildy.isWilderness());
        assertEquals(false, wildy.isKonarLockable());

        SourceLocation area = read(
            Paths.get("src/main/data/slayer/locations/scorpions-area.json"), SourceLocation.class);
        assertEquals("scorpions-area", area.getLocationId());
        assertEquals(false, area.isWilderness());
    }

    @Test
    public void generatedRuntimeDataResolvesScorpions() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Scorpions".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Scorpions"));

        assertEquals(270, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia", "turael", "spria", "mazchna"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant king = task.getVariants().stream()
            .filter(variant -> "King scorpion".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing King scorpion variant"));
        assertNotNull(king.getStrategy());
        assertNotNull("offence compiled onto runtime variant", king.getOffence());
        assertEquals(Integer.valueOf(30), king.getOffence().getHitpoints());
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

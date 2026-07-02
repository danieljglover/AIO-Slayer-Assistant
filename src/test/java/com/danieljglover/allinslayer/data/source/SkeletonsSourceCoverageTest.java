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
 * C2-E (ADR-0019 recipe): pins the wiki-authored Skeletons family. SHARED family (worklist §5.4) -
 * authored once with Krystilia (Wilderness) + Turael/Spria/Mazchna starter masters and both
 * Wilderness and non-Wilderness variant/location sets. undead:true. Stats from the live Skeleton
 * wiki page, fetched 2026-07-02. The Wilderness location is a family-tuned copy of the zombies-owned
 * graveyard-of-shadows base file (CT-L shared #3), which is not yet on disk. slayerTargetId is
 * SYNTHETIC (271) per the C2/C3 allocation contract (worklist §1).
 */
public class SkeletonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/skeletons.json"), SourceTask.class);

        assertEquals(271, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("krystilia", "turael", "spria", "mazchna"), masterIds(task));
        assertArrayEquals(new int[] {65, 100}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));

        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Skeletons have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Skeletons have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertTrue("undead applies to skeletons", task.isUndead());
        assertEquals(Set.of("graveyard-of-shadows-skeletons", "skeletons-area"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("undead")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant skeleton = read(
            Paths.get("src/main/data/slayer/monsters/skeletons/skeleton.json"), SourceMonsterVariant.class);
        assertEquals("skeleton", skeleton.getVariantId());
        assertTrue(skeleton.getNpcIds().contains(74));
        assertTrue(skeleton.isUndead());
        assertEquals(CombatStyle.MELEE, skeleton.getWeakness().getStyle());
        assertEquals("earth", skeleton.getWeakness().getElement());
        assertEquals(17, skeleton.getMonsterDefence().getDefenceLevel());

        assertNotNull("skeleton offence authored", skeleton.getOffence());
        assertEquals(Integer.valueOf(24), skeleton.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(3), skeleton.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE), skeleton.getOffence().getAttackStyles());
        assertNull("magicLevel is honest-UNKNOWN", skeleton.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/skeletons/strategy.json");
        assertTrue("Skeletons strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("skeletons", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void ownedLocationSourcesMatchWiki() throws IOException
    {
        SourceLocation wildy = read(
            Paths.get("src/main/data/slayer/locations/graveyard-of-shadows-skeletons.json"), SourceLocation.class);
        assertEquals("graveyard-of-shadows-skeletons", wildy.getLocationId());
        assertEquals(true, wildy.isWilderness());
        assertEquals(false, wildy.isKonarLockable());

        SourceLocation area = read(
            Paths.get("src/main/data/slayer/locations/skeletons-area.json"), SourceLocation.class);
        assertEquals("skeletons-area", area.getLocationId());
        assertEquals(false, area.isWilderness());
    }

    @Test
    public void generatedRuntimeDataResolvesSkeletons() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Skeletons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Skeletons"));

        assertEquals(271, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia", "turael", "spria", "mazchna"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant skeleton = task.getVariants().stream()
            .filter(variant -> "Skeleton".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Skeleton variant"));
        assertNotNull(skeleton.getStrategy());
        assertNotNull("offence compiled onto runtime variant", skeleton.getOffence());
        assertEquals(Integer.valueOf(24), skeleton.getOffence().getHitpoints());
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

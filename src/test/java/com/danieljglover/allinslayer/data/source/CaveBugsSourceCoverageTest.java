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
 * C3-B (ADR-0019 recipe): pins the wiki-authored Cave bugs family. Starter-tier Turael/Spria/Mazchna
 * task; cave-bugs OWNS the shared {@code lumbridge-swamp-caves} base location (CT-L doc), referenced
 * by cave-slime, cave-crawlers, rockslugs and wall-beasts. Stats from the live Cave bug wiki page,
 * fetched 2026-07-02. slayerTargetId is SYNTHETIC (279) per the C2/C3 allocation contract.
 */
public class CaveBugsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cave-bugs.json"), SourceTask.class);

        assertEquals(279, task.getSlayerTargetId());
        assertNull("no combat requirement", task.getCombatLevel());
        assertEquals(7, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("turael", "spria", "mazchna"), masterIds(task));
        assertArrayEquals(new int[] {10, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {10, 30}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("mazchna"));

        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));

        assertNull("Cave bugs have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Cave bugs have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("lumbridge-swamp-caves"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("light source")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dorgesh-Kaan")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant caveBug = read(
            Paths.get("src/main/data/slayer/monsters/cave-bugs/cave-bug.json"), SourceMonsterVariant.class);
        assertEquals("cave-bug", caveBug.getVariantId());
        assertTrue(caveBug.getNpcIds().contains(481));
        assertEquals(CombatStyle.MELEE, caveBug.getWeakness().getStyle());
        assertEquals("fire", caveBug.getWeakness().getElement());
        assertEquals(6, caveBug.getMonsterDefence().getDefenceLevel());

        assertNotNull("cave bug offence authored", caveBug.getOffence());
        assertEquals(Integer.valueOf(5), caveBug.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), caveBug.getOffence().getMaxHit());
        assertEquals(Integer.valueOf(4), caveBug.getOffence().getAttackSpeedTicks());
        assertEquals(List.of(AttackStyle.MELEE), caveBug.getOffence().getAttackStyles());
        assertNull("magicLevel is honest-UNKNOWN", caveBug.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/cave-bugs/strategy.json");
        assertTrue("Cave bugs strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("cave-bugs", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void ownedLocationSourceMatchesWiki() throws IOException
    {
        SourceLocation caves = read(
            Paths.get("src/main/data/slayer/locations/lumbridge-swamp-caves.json"), SourceLocation.class);
        assertEquals("lumbridge-swamp-caves", caves.getLocationId());
        assertEquals("Lumbridge Swamp Caves", caves.getName());
        assertEquals(false, caves.isWilderness());
        assertEquals(false, caves.isKonarLockable());
        assertEquals(true, caves.isCannon());
        assertTrue(caves.getAccessNote().contains("light source"));
    }

    @Test
    public void generatedRuntimeDataResolvesCaveBugs() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Cave bugs".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave bugs"));

        assertEquals(279, task.getSlayerTargetId());
        assertEquals(Set.of("turael", "spria", "mazchna"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant caveBug = task.getVariants().stream()
            .filter(variant -> "Cave bug".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave bug variant"));
        assertNotNull(caveBug.getStrategy());
        assertEquals(CombatStyle.MELEE, caveBug.getStrategy().getPrimaryStyle());
        assertNotNull("offence compiled onto runtime variant", caveBug.getOffence());
        assertEquals(Integer.valueOf(5), caveBug.getOffence().getHitpoints());
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

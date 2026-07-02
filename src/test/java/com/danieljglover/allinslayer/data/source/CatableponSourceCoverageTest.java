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
 * C3-C (ADR-0019 recipe): pins the wiki-authored Catablepon family. Mazchna starter-tier task, no
 * Slayer level required, in the Stronghold of Security. Stats from the live Catablepon wiki page,
 * fetched 2026-07-02. slayerTargetId is SYNTHETIC (278) per the C2/C3 allocation contract. Location
 * is a family-tuned copy of the stronghold-of-security base (Ankou flags do not apply).
 */
public class CatableponSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/catablepon.json"), SourceTask.class);

        assertEquals(278, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {20, 30}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));

        assertNull("Catablepon have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Catablepon have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("stronghold-of-security-catablepon"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant catablepon = read(
            Paths.get("src/main/data/slayer/monsters/catablepon/catablepon.json"), SourceMonsterVariant.class);
        assertEquals("catablepon", catablepon.getVariantId());
        assertTrue(catablepon.getNpcIds().contains(2474));
        assertTrue(catablepon.getNpcIds().contains(2476));
        assertEquals(CombatStyle.MELEE, catablepon.getWeakness().getStyle());
        assertNull("no elemental weakness (honest null)", catablepon.getWeakness().getElement());
        assertEquals(40, catablepon.getMonsterDefence().getDefenceLevel());

        assertNotNull("catablepon offence authored", catablepon.getOffence());
        assertEquals(Integer.valueOf(40), catablepon.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(6), catablepon.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE, AttackStyle.MAGIC), catablepon.getOffence().getAttackStyles());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/catablepon/strategy.json");
        assertTrue("Catablepon strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("catablepon", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void generatedRuntimeDataResolvesCatablepon() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Catablepon".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Catablepon"));

        assertEquals(278, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant catablepon = task.getVariants().stream()
            .filter(variant -> "Catablepon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Catablepon variant"));
        assertNotNull(catablepon.getStrategy());
        assertNotNull("offence compiled onto runtime variant", catablepon.getOffence());
        assertEquals(Integer.valueOf(40), catablepon.getOffence().getHitpoints());
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

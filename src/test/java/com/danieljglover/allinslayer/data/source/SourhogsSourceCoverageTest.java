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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C3-H (ADR-0019): pins the wiki-sourced Sourhogs starter family (Spria-only).
 * Numbers verified live 2026-07-02 against the Sourhog wiki page (pageId 273904) and the Spria
 * assignment table (15-25, weight 6). slayerTargetId 303 is SYNTHETIC per the c2-c3-worklist
 * allocation contract. Requires the quest A Porcine of Interest; no combat/Slayer requirement.
 */
public class SourhogsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/sourhogs.json"), SourceTask.class);

        assertEquals(Integer.valueOf(273904), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(303, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().stream().anyMatch(q -> q.contains("A Porcine of Interest")));

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "spria");
        assertArrayEquals(new int[] {15, 25}, task.getAmountByMaster().get("spria"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("spria"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals("Reinforced goggles", task.getRequiredItemName());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "sourhog-cave");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("303")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Spria") && n.contains("May 2024")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo sourhog = task.getVariantInfo().stream()
            .filter(v -> "sourhog-lvl37".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(37), sourhog.getCombatLevel());
        assertEquals(40.0, sourhog.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison cave = task.getLocationComparison().stream()
            .filter(l -> "sourhog-cave".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertNull(cave.getAmount());
        assertEquals(Boolean.FALSE, cave.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant sourhog = read(
            Paths.get("src/main/data/slayer/monsters/sourhogs/sourhog-lvl37.json"), SourceMonsterVariant.class);
        assertTrue(sourhog.getNpcIds().contains(Integer.valueOf(10435)));
        assertNotNull(sourhog.getOffence());
        assertEquals(Integer.valueOf(40), sourhog.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(30), sourhog.getOffence().getMaxHit());
        assertTrue(sourhog.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertTrue(sourhog.getOffence().getAttackStyles().contains(AttackStyle.RANGED));
        assertNull(sourhog.getOffence().getMagicLevel());
        assertEquals(0, sourhog.getMonsterDefence().getStab());
        assertEquals(30, sourhog.getMonsterDefence().getSlash());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/sourhogs/strategy.json");
        assertTrue("Sourhogs strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("sourhogs", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("caustic")));
    }

    @Test
    public void generatedRuntimeDataCarriesSourhogsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Sourhogs".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Sourhogs"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertEquals(1, task.getVariants().size());

        MonsterVariant deflt = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Sourhog", deflt.getName());
        assertNotNull(deflt.getStrategy());
        assertEquals(CombatStyle.MELEE, deflt.getStrategy().getPrimaryStyle());
        assertNotNull(deflt.getOffence());
        assertEquals(Integer.valueOf(40), deflt.getOffence().getHitpoints());
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("expected " + values + " to contain " + expected, values.contains(expected));
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

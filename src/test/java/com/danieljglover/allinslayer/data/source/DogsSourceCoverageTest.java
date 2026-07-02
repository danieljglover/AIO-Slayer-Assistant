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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C3-E (ADR-0019): pins the wiki-sourced Dogs starter family (Turael/Spria/Mazchna).
 * Numbers verified live 2026-07-02 against the Guard dog / Jackal / Wild dog wiki pages and the
 * Turael/Mazchna assignment tables (15-30 / 15-30 / 30-50, weight 7). slayerTargetId 286 is SYNTHETIC
 * per the c2-c3-worklist allocation contract. No single Dogs monster page exists, so wikiPageId is null.
 */
public class DogsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dogs.json"), SourceTask.class);

        assertNull(task.getWikiPageId());
        assertEquals(Integer.valueOf(15), task.getCombatLevel());
        assertEquals(286, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(3, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertNull(task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "dog-spawns");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("286")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("combat level 15")));

        assertEquals(3, task.getVariantInfo().size());
        SourceTaskVariantInfo guardDog = task.getVariantInfo().stream()
            .filter(v -> "guard-dog".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(44), guardDog.getCombatLevel());
        assertEquals(49.0, guardDog.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison spawns = task.getLocationComparison().stream()
            .filter(l -> "dog-spawns".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(10), spawns.getAmount());
    }

    @Test
    public void jackalVariantCarriesFireWeaknessAndOffence() throws IOException
    {
        SourceMonsterVariant jackal = read(
            Paths.get("src/main/data/slayer/monsters/dogs/jackal.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {4185, 11583}, toIntArray(jackal.getNpcIds()));
        assertEquals(CombatStyle.MAGIC, jackal.getWeakness().getStyle());
        assertEquals("fire", jackal.getWeakness().getElement());
        assertNotNull(jackal.getOffence());
        assertEquals(Integer.valueOf(27), jackal.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(3), jackal.getOffence().getMaxHit());
        assertTrue(jackal.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(jackal.getOffence().getMagicLevel());

        SourceMonsterVariant guardDog = read(
            Paths.get("src/main/data/slayer/monsters/dogs/guard-dog.json"), SourceMonsterVariant.class);
        assertNull(guardDog.getWeakness().getElement());
        assertEquals(Integer.valueOf(49), guardDog.getOffence().getHitpoints());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/dogs/strategy.json");
        assertTrue("Dogs strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("dogs", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("Guard dog")));
    }

    @Test
    public void generatedRuntimeDataCarriesDogsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Dogs".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Dogs"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(3, task.getVariants().size());

        MonsterVariant guardDog = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertTrue(guardDog.isDefault());
        assertNotNull(guardDog.getStrategy());
        assertEquals(CombatStyle.MELEE, guardDog.getStrategy().getPrimaryStyle());
        assertNotNull(guardDog.getOffence());
        assertEquals(Integer.valueOf(49), guardDog.getOffence().getHitpoints());
    }

    private static int[] toIntArray(java.util.List<Integer> list)
    {
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            out[i] = list.get(i);
        }
        return out;
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

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
 * C3-E (ADR-0019): pins the wiki-sourced Monkeys starter family (Turael/Spria only).
 * Numbers verified live 2026-07-02 against the Monkey (monster) wiki page (pageId 18457) and the
 * Turael/Spria assignment tables (15-30, weight 6). slayerTargetId 298 is SYNTHETIC per the
 * c2-c3-worklist allocation contract. No combat or Slayer level requirement.
 */
public class MonkeysSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/monkeys.json"), SourceTask.class);

        assertEquals(Integer.valueOf(18457), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(298, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertFalse(masterIds(task).contains("mazchna"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("spria"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertNull(task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "monkey-spawns");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("298")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Maniacal")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo monkey = task.getVariantInfo().stream()
            .filter(v -> "monkey".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(3), monkey.getCombatLevel());
        assertEquals(6.0, monkey.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison spawns = task.getLocationComparison().stream()
            .filter(l -> "monkey-spawns".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Boolean.FALSE, spawns.getCannonable());
    }

    @Test
    public void monkeyVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant monkey = read(
            Paths.get("src/main/data/slayer/monsters/monkeys/monkey.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {1038, 2848}, toIntArray(monkey.getNpcIds()));
        assertNull(monkey.getWeakness().getElement());
        assertNotNull(monkey.getOffence());
        assertEquals(Integer.valueOf(6), monkey.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), monkey.getOffence().getMaxHit());
        assertTrue(monkey.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(monkey.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/monkeys/strategy.json");
        assertTrue("Monkeys strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("monkeys", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("Maniacal")));
    }

    @Test
    public void generatedRuntimeDataCarriesMonkeysAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Monkeys".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Monkeys"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertEquals(1, task.getVariants().size());

        MonsterVariant monkey = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertTrue(monkey.isDefault());
        assertNotNull(monkey.getStrategy());
        assertEquals(CombatStyle.MELEE, monkey.getStrategy().getPrimaryStyle());
        assertNotNull(monkey.getOffence());
        assertEquals(Integer.valueOf(6), monkey.getOffence().getHitpoints());
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

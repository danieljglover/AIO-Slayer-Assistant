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
 * C3-G (ADR-0019): pins the wiki-sourced Killerwatts starter family (Mazchna only). Numbers verified
 * live 2026-07-02 against the Killerwatt wiki page (pageId 16183) and the Mazchna assignment table
 * (30-50, weight 6). slayerTargetId 294 is SYNTHETIC per the c2-c3-worklist allocation contract.
 */
public class KillerwattsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/killerwatts.json"), SourceTask.class);

        assertEquals(Integer.valueOf(16183), task.getWikiPageId());
        assertEquals(Integer.valueOf(50), task.getCombatLevel());
        assertEquals(294, task.getSlayerTargetId());
        assertEquals(37, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Ernest the Chicken"));

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals("Insulated boots", task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());

        assertContains(locationIds(task), "killerwatt-plane");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("294")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Insulated boots")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("air spells")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo kw = task.getVariantInfo().stream()
            .filter(v -> "killerwatt".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(55), kw.getCombatLevel());
        assertEquals(51.0, kw.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison plane = task.getLocationComparison().stream()
            .filter(l -> "killerwatt-plane".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Boolean.FALSE, plane.getCannonable());
        assertEquals(Boolean.FALSE, plane.getSafespottable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant kw = read(
            Paths.get("src/main/data/slayer/monsters/killerwatts/killerwatt.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {470, 469}, toIntArray(kw.getNpcIds()));
        assertFalse(kw.isUndead());
        assertNotNull(kw.getOffence());
        assertEquals(Integer.valueOf(51), kw.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(15), kw.getOffence().getMaxHit());
        assertTrue(kw.getOffence().getAttackStyles().contains(AttackStyle.RANGED));
        assertTrue(kw.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(kw.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/killerwatts/strategy.json");
        assertTrue("Killerwatts strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("killerwatts", strategy.getStrategyId());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("air spells")));
    }

    @Test
    public void generatedRuntimeDataCarriesKillerwattsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Killerwatts".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Killerwatts"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(1, task.getVariants().size());

        MonsterVariant kw = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertNotNull(kw.getStrategy());
        assertEquals(CombatStyle.MAGIC, kw.getStrategy().getPrimaryStyle());
        assertNotNull(kw.getOffence());
        assertEquals(Integer.valueOf(51), kw.getOffence().getHitpoints());
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

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
 * C3-A (ADR-0019): pins the wiki-sourced Cave slime starter family (Turael/Spria/Mazchna).
 * Numbers verified live 2026-07-02 against the Cave slime wiki page (pageId 13094) and the
 * Turael/Mazchna assignment tables (10-20, weight 8). slayerTargetId 281 is SYNTHETIC per the
 * c2-c3-worklist allocation contract. A light source is required (item id honest-UNKNOWN).
 */
public class CaveSlimeSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cave-slime.json"), SourceTask.class);

        assertEquals(Integer.valueOf(13094), task.getWikiPageId());
        assertEquals(Integer.valueOf(15), task.getCombatLevel());
        assertEquals(281, task.getSlayerTargetId());
        assertEquals(17, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(3, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        // Light source required, but no single canonical item id (honest UNKNOWN id, named).
        assertNull(task.getRequiredItemId());
        assertEquals("Light source", task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());

        assertContains(locationIds(task), "lumbridge-swamp-caves");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("281")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("light source") || n.contains("Light source")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("poison")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo slime = task.getVariantInfo().stream()
            .filter(v -> "cave-slime".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(23), slime.getCombatLevel());
        assertEquals(25.0, slime.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison caves = task.getLocationComparison().stream()
            .filter(l -> "lumbridge-swamp-caves".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(17), caves.getAmount());
        assertEquals(Boolean.TRUE, caves.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant slime = read(
            Paths.get("src/main/data/slayer/monsters/cave-slime/cave-slime.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {480}, toIntArray(slime.getNpcIds()));
        assertFalse(slime.isUndead());
        assertNotNull(slime.getOffence());
        assertEquals(Integer.valueOf(25), slime.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(2), slime.getOffence().getMaxHit());
        assertTrue(slime.getOffence().isPoisonous());
        assertFalse(slime.getOffence().isVenomous());
        assertTrue(slime.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(slime.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/cave-slime/strategy.json");
        assertTrue("Cave slime strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("cave-slime", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("earth spells")));
    }

    @Test
    public void generatedRuntimeDataCarriesCaveSlimeAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Cave slime".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave slime"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(1, task.getVariants().size());

        MonsterVariant slime = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertTrue(slime.isDefault());
        assertNotNull(slime.getStrategy());
        assertEquals(CombatStyle.MELEE, slime.getStrategy().getPrimaryStyle());
        assertNotNull(slime.getOffence());
        assertEquals(Integer.valueOf(25), slime.getOffence().getHitpoints());
        assertTrue(slime.getOffence().isPoisonous());
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

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
 * C3-A (ADR-0019): pins the wiki-sourced Crawling Hands starter family (Turael/Spria/Mazchna).
 * Numbers verified live 2026-07-02 against the Crawling Hand wiki page (pageId 11874) and the
 * Turael/Mazchna assignment tables. slayerTargetId 285 is SYNTHETIC per the c2-c3-worklist
 * allocation contract. No required item, no extension, no combat requirement.
 */
public class CrawlingHandsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/crawling-hands.json"), SourceTask.class);

        assertEquals(Integer.valueOf(11874), task.getWikiPageId());
        // No combat level requirement for this starter task (honest null).
        assertNull(task.getCombatLevel());
        assertEquals(285, task.getSlayerTargetId());
        assertEquals(5, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Priest in Peril"));

        assertEquals(3, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertTrue(task.isUndead());
        assertFalse(task.isDemon());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "slayer-tower");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("285")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("undead")));

        assertEquals(2, task.getVariantInfo().size());
        SourceTaskVariantInfo lvl8 = task.getVariantInfo().stream()
            .filter(v -> "crawling-hand-lvl8".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(8), lvl8.getCombatLevel());
        assertEquals(12.0, lvl8.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison tower = task.getLocationComparison().stream()
            .filter(l -> "slayer-tower".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(24), tower.getAmount());
        assertEquals(Boolean.TRUE, tower.getSafespottable());
        assertEquals(Boolean.FALSE, tower.getCannonable());
    }

    @Test
    public void monsterVariantsCarryHonestOffence() throws IOException
    {
        SourceMonsterVariant lvl8 = read(
            Paths.get("src/main/data/slayer/monsters/crawling-hands/crawling-hand-lvl8.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {448, 449}, toIntArray(lvl8.getNpcIds()));
        assertTrue(lvl8.isUndead());
        assertNotNull(lvl8.getOffence());
        assertEquals(Integer.valueOf(15), lvl8.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), lvl8.getOffence().getMaxHit());
        assertTrue(lvl8.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        // Not Tbow-relevant: magicLevel is honest-UNKNOWN (absent).
        assertNull(lvl8.getOffence().getMagicLevel());

        SourceMonsterVariant lvl12 = read(
            Paths.get("src/main/data/slayer/monsters/crawling-hands/crawling-hand-lvl12.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {453, 454}, toIntArray(lvl12.getNpcIds()));
        assertEquals(Integer.valueOf(15), lvl12.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), lvl12.getOffence().getMaxHit());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/crawling-hands/strategy.json");
        assertTrue("Crawling Hands strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("crawling-hands", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("undead")));
    }

    @Test
    public void generatedRuntimeDataCarriesCrawlingHandsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Crawling Hands".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Crawling Hands"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(2, task.getVariants().size());

        MonsterVariant lvl8 = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertTrue(lvl8.isDefault());
        assertNotNull(lvl8.getStrategy());
        assertEquals(CombatStyle.MELEE, lvl8.getStrategy().getPrimaryStyle());
        assertNotNull(lvl8.getOffence());
        assertEquals(Integer.valueOf(15), lvl8.getOffence().getHitpoints());
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

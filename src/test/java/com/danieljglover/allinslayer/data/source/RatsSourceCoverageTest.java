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
 * C3-E (ADR-0019): pins the wiki-sourced Rats starter family (Turael/Spria only).
 * Numbers verified live 2026-07-02 against the Rat (pageId 13609) and Giant rat wiki pages and the
 * Turael/Spria assignment tables (15-30, weight 7). slayerTargetId 300 is SYNTHETIC per the
 * c2-c3-worklist allocation contract. Brine rats are a SEPARATE on-disk family and are not folded in.
 */
public class RatsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/rats.json"), SourceTask.class);

        assertEquals(Integer.valueOf(13609), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(300, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertFalse(masterIds(task).contains("mazchna"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertNull(task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());
        assertEquals("giant-rat", task.getDefaultVariantId());

        assertContains(locationIds(task), "rat-spawns");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("300")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("brine rats") || n.contains("Brine")));

        assertEquals(2, task.getVariantInfo().size());
        SourceTaskVariantInfo giantRat = task.getVariantInfo().stream()
            .filter(v -> "giant-rat".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(3), giantRat.getCombatLevel());
        assertEquals(5.0, giantRat.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison spawns = task.getLocationComparison().stream()
            .filter(l -> "rat-spawns".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(27), spawns.getAmount());
    }

    @Test
    public void ratVariantsCarryHonestOffence() throws IOException
    {
        SourceMonsterVariant rat = read(
            Paths.get("src/main/data/slayer/monsters/rats/rat.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {2854, 2855}, toIntArray(rat.getNpcIds()));
        assertEquals(-42, rat.getMonsterDefence().getStab());
        assertNotNull(rat.getOffence());
        assertEquals(Integer.valueOf(2), rat.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(0), rat.getOffence().getMaxHit());

        SourceMonsterVariant giantRat = read(
            Paths.get("src/main/data/slayer/monsters/rats/giant-rat.json"), SourceMonsterVariant.class);
        assertNull(giantRat.getWeakness().getElement());
        assertEquals(Integer.valueOf(5), giantRat.getOffence().getHitpoints());
        assertTrue(giantRat.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(giantRat.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/rats/strategy.json");
        assertTrue("Rats strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("rats", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("Giant rat")));
    }

    @Test
    public void generatedRuntimeDataCarriesRatsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Rats".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Rats"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertEquals(2, task.getVariants().size());

        MonsterVariant giantRat = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertTrue(giantRat.isDefault());
        assertEquals("Giant rat", giantRat.getName());
        assertNotNull(giantRat.getStrategy());
        assertEquals(CombatStyle.MELEE, giantRat.getStrategy().getPrimaryStyle());
        assertNotNull(giantRat.getOffence());
        assertEquals(Integer.valueOf(5), giantRat.getOffence().getHitpoints());
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

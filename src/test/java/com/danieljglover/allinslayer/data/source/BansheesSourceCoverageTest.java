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
 * C3-A (ADR-0019): pins the wiki-sourced Banshees starter family (Turael/Spria/Mazchna).
 * Numbers verified live 2026-07-02 against the Banshee / Twisted Banshee wiki pages.
 * slayerTargetId 275 is SYNTHETIC per the c2-c3-worklist allocation contract. Earmuffs
 * (ItemID 4166) are the task's required protective item.
 */
public class BansheesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/banshees.json"), SourceTask.class);

        assertEquals(Integer.valueOf(15230449), task.getWikiPageId());
        assertEquals(Integer.valueOf(20), task.getCombatLevel());
        assertEquals(275, task.getSlayerTargetId());
        assertEquals(15, task.getSlayerLevel());
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

        assertEquals(Integer.valueOf(4166), task.getRequiredItemId());
        assertEquals("Earmuffs", task.getRequiredItemName());
        assertTrue(task.isUndead());
        assertFalse(task.isDemon());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());

        assertContains(locationIds(task), "slayer-tower");
        assertContains(locationIds(task), "catacombs-of-kourend");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("275")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Earmuffs") || n.contains("earmuffs")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("undead")));

        assertEquals(2, task.getVariantInfo().size());
        SourceTaskVariantInfo banshee = task.getVariantInfo().stream()
            .filter(v -> "banshee".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(23), banshee.getCombatLevel());
        assertEquals(22.0, banshee.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison catacombs = task.getLocationComparison().stream()
            .filter(l -> "catacombs-of-kourend".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(6), catacombs.getAmount());
        assertEquals(Boolean.TRUE, catacombs.getSafespottable());
    }

    @Test
    public void monsterVariantsCarryHonestOffence() throws IOException
    {
        SourceMonsterVariant banshee = read(
            Paths.get("src/main/data/slayer/monsters/banshees/banshee-lvl23.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {414}, toIntArray(banshee.getNpcIds()));
        assertTrue(banshee.isUndead());
        assertNotNull(banshee.getOffence());
        assertEquals(Integer.valueOf(22), banshee.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(2), banshee.getOffence().getMaxHit());
        assertTrue(banshee.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        // Banshees are not Tbow-relevant: magicLevel is honest-UNKNOWN (absent).
        assertNull(banshee.getOffence().getMagicLevel());

        SourceMonsterVariant twisted = read(
            Paths.get("src/main/data/slayer/monsters/banshees/twisted-banshee-lvl89.json"), SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(100), twisted.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(9), twisted.getOffence().getMaxHit());
    }

    @Test
    public void strategySourceCarriesEarmuffsAndAirWeakness() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/banshees/strategy.json");
        assertTrue("Banshees strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("banshees", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Banshee", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getNote().contains("earmuffs"));
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("air spells")));
    }

    @Test
    public void generatedRuntimeDataCarriesBansheesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Banshees".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Banshees"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(Integer.valueOf(4166), task.getRequiredItemId());
        assertEquals(2, task.getVariants().size());

        MonsterVariant banshee = task.getVariants().stream()
            .filter(v -> "Banshee".equals(v.getName())).findFirst().orElseThrow(AssertionError::new);
        assertTrue(banshee.isDefault());
        assertNotNull(banshee.getStrategy());
        assertEquals(CombatStyle.MELEE, banshee.getStrategy().getPrimaryStyle());
        assertNotNull(banshee.getOffence());
        assertEquals(Integer.valueOf(22), banshee.getOffence().getHitpoints());
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

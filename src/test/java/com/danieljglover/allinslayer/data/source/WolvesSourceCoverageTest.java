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
 * C3-H (ADR-0019): pins the wiki-sourced Wolves starter family (Turael/Spria/Mazchna).
 * Numbers verified live 2026-07-02 against the Wolf/White wolf/Big wolf wiki pages (pageId 16539)
 * and the master assignment tables (weight 7, combat level 20 requirement). slayerTargetId 305 is
 * SYNTHETIC per the c2-c3-worklist allocation contract.
 */
public class WolvesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/wolves.json"), SourceTask.class);

        assertEquals(Integer.valueOf(16539), task.getWikiPageId());
        assertEquals(Integer.valueOf(20), task.getCombatLevel());
        assertEquals(305, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(3, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());

        assertContains(locationIds(task), "white-wolf-mountain");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("305")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("combat level of 20")));

        assertEquals(3, task.getVariantInfo().size());
        SourceTaskVariantInfo big = task.getVariantInfo().stream()
            .filter(v -> "big-wolf".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(73), big.getCombatLevel());
        assertEquals(74.0, big.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison mountain = task.getLocationComparison().stream()
            .filter(l -> "white-wolf-mountain".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertNull(mountain.getAmount());
        assertEquals(Boolean.FALSE, mountain.getCannonable());
    }

    @Test
    public void monsterVariantsCarryHonestOffence() throws IOException
    {
        SourceMonsterVariant wolf = read(
            Paths.get("src/main/data/slayer/monsters/wolves/wolf-lvl25.json"), SourceMonsterVariant.class);
        assertTrue(wolf.getNpcIds().contains(Integer.valueOf(110)));
        assertNotNull(wolf.getOffence());
        assertEquals(Integer.valueOf(20), wolf.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(3), wolf.getOffence().getMaxHit());
        assertTrue(wolf.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(wolf.getOffence().getMagicLevel());
        assertEquals(CombatStyle.MAGIC, wolf.getWeakness().getStyle());
        assertEquals("fire", wolf.getWeakness().getElement());

        SourceMonsterVariant bigWolf = read(
            Paths.get("src/main/data/slayer/monsters/wolves/big-wolf.json"), SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(74), bigWolf.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(7), bigWolf.getOffence().getMaxHit());
        assertEquals(62, bigWolf.getMonsterDefence().getDefenceLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/wolves/strategy.json");
        assertTrue("Wolves strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("wolves", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("fire weakness")));
    }

    @Test
    public void generatedRuntimeDataCarriesWolvesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Wolves".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Wolves"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(3, task.getVariants().size());

        MonsterVariant deflt = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Wolf", deflt.getName());
        assertNotNull(deflt.getStrategy());
        assertEquals(CombatStyle.MELEE, deflt.getStrategy().getPrimaryStyle());
        assertNotNull(deflt.getOffence());
        assertEquals(Integer.valueOf(20), deflt.getOffence().getHitpoints());
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

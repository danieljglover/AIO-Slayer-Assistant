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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * C3-A (ADR-0019): pins the wiki-sourced Ghosts starter family (Turael/Spria/Mazchna).
 * Numbers verified live 2026-07-02 against the Ghost wiki page (pageId 16777) and the
 * Turael/Mazchna assignment tables (weight 7). slayerTargetId 289 is SYNTHETIC per the
 * c2-c3-worklist allocation contract. Combat level 13 to assign; no Slayer/quest requirement.
 */
public class GhostsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ghosts.json"), SourceTask.class);

        assertEquals(Integer.valueOf(16777), task.getWikiPageId());
        assertEquals(Integer.valueOf(13), task.getCombatLevel());
        assertEquals(289, task.getSlayerTargetId());
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
        assertTrue(task.isUndead());
        assertEquals(CombatStyle.MAGIC, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());

        assertContains(locationIds(task), "ghost-spawns");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("289")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("undead")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("air spells")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo lvl19 = task.getVariantInfo().stream()
            .filter(v -> "ghost-lvl19".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(19), lvl19.getCombatLevel());
        assertEquals(25.0, lvl19.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison spawns = task.getLocationComparison().stream()
            .filter(l -> "ghost-spawns".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        // Density not wiki-stated: amount honest-UNKNOWN (absent).
        assertNull(spawns.getAmount());
        assertEquals(Boolean.FALSE, spawns.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant lvl19 = read(
            Paths.get("src/main/data/slayer/monsters/ghosts/ghost-lvl19.json"), SourceMonsterVariant.class);
        assertTrue(lvl19.isUndead());
        assertTrue(lvl19.getNpcIds().contains(Integer.valueOf(85)));
        assertNotNull(lvl19.getOffence());
        assertEquals(Integer.valueOf(25), lvl19.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(2), lvl19.getOffence().getMaxHit());
        assertTrue(lvl19.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(lvl19.getOffence().getMagicLevel());
        assertEquals(-5, lvl19.getMonsterDefence().getMagic());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/ghosts/strategy.json");
        assertTrue("Ghosts strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("ghosts", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("air spells")));
    }

    @Test
    public void generatedRuntimeDataCarriesGhostsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Ghosts".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ghosts"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(1, task.getVariants().size());

        MonsterVariant lvl19 = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertTrue(lvl19.isDefault());
        assertNotNull(lvl19.getStrategy());
        assertEquals(CombatStyle.MELEE, lvl19.getStrategy().getPrimaryStyle());
        assertNotNull(lvl19.getOffence());
        assertEquals(Integer.valueOf(25), lvl19.getOffence().getHitpoints());
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

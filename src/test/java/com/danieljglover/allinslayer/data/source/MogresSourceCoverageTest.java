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
 * C3-G (ADR-0019): pins the wiki-sourced Mogres starter family (Mazchna only). Numbers verified live
 * 2026-07-02 against the Mogre wiki page (pageId 17608) and the Mazchna assignment table (30-50, weight
 * 8). slayerTargetId 297 is SYNTHETIC per the c2-c3-worklist allocation contract.
 */
public class MogresSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/mogres.json"), SourceTask.class);

        assertEquals(Integer.valueOf(17608), task.getWikiPageId());
        assertEquals(Integer.valueOf(30), task.getCombatLevel());
        assertEquals(297, task.getSlayerTargetId());
        assertEquals(32, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Skippy and the Mogres"));

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals("Fishing explosive", task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "mudskipper-point");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("297")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("fishing explosive")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("earth")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo mogre = task.getVariantInfo().stream()
            .filter(v -> "mogre".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(60), mogre.getCombatLevel());
        assertEquals(48.0, mogre.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison point = task.getLocationComparison().stream()
            .filter(l -> "mudskipper-point".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Boolean.FALSE, point.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant mogre = read(
            Paths.get("src/main/data/slayer/monsters/mogres/mogre.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {2592}, toIntArray(mogre.getNpcIds()));
        assertFalse(mogre.isUndead());
        assertNotNull(mogre.getOffence());
        assertEquals(Integer.valueOf(48), mogre.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(8), mogre.getOffence().getMaxHit());
        assertTrue(mogre.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(mogre.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/mogres/strategy.json");
        assertTrue("Mogres strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("mogres", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("fishing explosive")));
    }

    @Test
    public void generatedRuntimeDataCarriesMogresAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Mogres".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Mogres"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(1, task.getVariants().size());

        MonsterVariant mogre = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertNotNull(mogre.getStrategy());
        assertEquals(CombatStyle.MELEE, mogre.getStrategy().getPrimaryStyle());
        assertNotNull(mogre.getOffence());
        assertEquals(Integer.valueOf(48), mogre.getOffence().getHitpoints());
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

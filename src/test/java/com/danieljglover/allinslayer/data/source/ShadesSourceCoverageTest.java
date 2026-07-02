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
 * C3-G (ADR-0019): pins the wiki-sourced Shades starter family (Mazchna). Numbers verified live
 * 2026-07-02 against the Shades slayer-task page (pageId 36404) and the Loar/Shade monster pages, and
 * the Mazchna assignment table (30-70, weight 8). slayerTargetId 302 is SYNTHETIC per the
 * c2-c3-worklist allocation contract. undead:true (salve amulet applies). Vannaka is documented in
 * taskNotes but authored Mazchna-only per the C3 starter-tier scope.
 */
public class ShadesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/shades.json"), SourceTask.class);

        assertEquals(Integer.valueOf(36404), task.getWikiPageId());
        assertEquals(Integer.valueOf(30), task.getCombatLevel());
        assertEquals(302, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {30, 70}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertTrue("Shades are undead", task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "mortton");
        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "stronghold-of-security");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("302")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Vannaka")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("salve") || n.contains("undead")));

        assertEquals(3, task.getVariantInfo().size());
        SourceTaskVariantInfo loar = task.getVariantInfo().stream()
            .filter(v -> "loar-shade".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(40), loar.getCombatLevel());
        assertEquals(38.0, loar.getSlayerXp().doubleValue(), 0.0);
    }

    @Test
    public void monsterVariantsCarryHonestOffence() throws IOException
    {
        SourceMonsterVariant loar = read(
            Paths.get("src/main/data/slayer/monsters/shades/loar-shade.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {1277, 1276}, toIntArray(loar.getNpcIds()));
        assertTrue(loar.isUndead());
        assertNotNull(loar.getOffence());
        assertEquals(Integer.valueOf(38), loar.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(4), loar.getOffence().getMaxHit());
        assertTrue(loar.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(loar.getOffence().getMagicLevel());

        SourceMonsterVariant cata = read(
            Paths.get("src/main/data/slayer/monsters/shades/shade-catacombs.json"), SourceMonsterVariant.class);
        assertTrue(cata.isUndead());
        assertEquals(Integer.valueOf(115), cata.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(15), cata.getOffence().getMaxHit());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/shades/strategy.json");
        assertTrue("Shades strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("shades", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("undead")));
    }

    @Test
    public void generatedRuntimeDataCarriesShadesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Shades".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Shades"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(3, task.getVariants().size());

        MonsterVariant loar = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Loar Shade", loar.getName());
        assertNotNull(loar.getStrategy());
        assertEquals(CombatStyle.MELEE, loar.getStrategy().getPrimaryStyle());
        assertNotNull(loar.getOffence());
        assertEquals(Integer.valueOf(38), loar.getOffence().getHitpoints());
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

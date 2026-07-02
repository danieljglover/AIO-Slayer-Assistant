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
 * C3-E (ADR-0019): pins the wiki-sourced Dwarves starter family (Turael/Spria only).
 * Numbers verified live 2026-07-02 against the Dwarf (pageId 16965) and Black Guard wiki pages and
 * the Turael/Spria assignment tables (10-25, weight 7). slayerTargetId 287 is SYNTHETIC per the
 * c2-c3-worklist allocation contract. Mazchna does not assign Dwarves.
 */
public class DwarvesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dwarves.json"), SourceTask.class);

        assertEquals(Integer.valueOf(16965), task.getWikiPageId());
        assertEquals(Integer.valueOf(6), task.getCombatLevel());
        assertEquals(287, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertFalse(masterIds(task).contains("mazchna"));
        assertArrayEquals(new int[] {10, 25}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {10, 25}, task.getAmountByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertNull(task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "dwarven-mine");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("287")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Black Guard")));

        assertEquals(2, task.getVariantInfo().size());
        SourceTaskVariantInfo dwarf = task.getVariantInfo().stream()
            .filter(v -> "dwarf".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(10), dwarf.getCombatLevel());
        assertEquals(16.0, dwarf.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison mine = task.getLocationComparison().stream()
            .filter(l -> "dwarven-mine".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(36), mine.getAmount());
    }

    @Test
    public void dwarfVariantCarriesMagicDefenceAndOffence() throws IOException
    {
        SourceMonsterVariant dwarf = read(
            Paths.get("src/main/data/slayer/monsters/dwarves/dwarf.json"), SourceMonsterVariant.class);
        assertEquals(6, dwarf.getMonsterDefence().getDefenceLevel());
        assertEquals(5, dwarf.getMonsterDefence().getMagic());
        assertNull(dwarf.getWeakness().getElement());
        assertNotNull(dwarf.getOffence());
        assertEquals(Integer.valueOf(16), dwarf.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(2), dwarf.getOffence().getMaxHit());
        assertEquals(Integer.valueOf(5), dwarf.getOffence().getAttackSpeedTicks());
        assertTrue(dwarf.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(dwarf.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/dwarves/strategy.json");
        assertTrue("Dwarves strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("dwarves", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("Dwarf")));
    }

    @Test
    public void generatedRuntimeDataCarriesDwarvesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Dwarves".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Dwarves"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertEquals(2, task.getVariants().size());

        MonsterVariant dwarf = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertTrue(dwarf.isDefault());
        assertNotNull(dwarf.getStrategy());
        assertEquals(CombatStyle.MELEE, dwarf.getStrategy().getPrimaryStyle());
        assertNotNull(dwarf.getOffence());
        assertEquals(Integer.valueOf(16), dwarf.getOffence().getHitpoints());
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

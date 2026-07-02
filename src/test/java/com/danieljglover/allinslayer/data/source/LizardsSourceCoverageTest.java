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
 * C3-G (ADR-0019): pins the wiki-sourced Lizards starter family (Turael/Spria/Mazchna). Numbers verified
 * live 2026-07-02 against the Desert Lizard wiki page (pageId 13072) and the Spria/Mazchna assignment
 * tables (15-30 / 30-50, weight 8). slayerTargetId 295 is SYNTHETIC per the c2-c3-worklist allocation
 * contract. requiredItemId 6696 = Ice cooler (ItemID.ICE_COOLER, javap-verified).
 */
public class LizardsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lizards.json"), SourceTask.class);

        assertEquals(Integer.valueOf(13072), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(295, task.getSlayerTargetId());
        assertEquals(22, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(3, task.getMasterIds().size());
        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "mazchna");
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        // Ice cooler is a hard finishing mechanic with a known item id.
        assertEquals(Integer.valueOf(6696), task.getRequiredItemId());
        assertEquals("Ice cooler", task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());

        assertContains(locationIds(task), "kharidian-desert-lizards");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("295")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("6696") && n.contains("Ice cooler")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("ice cooler") && n.contains("5 Hitpoints")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo lizard = task.getVariantInfo().stream()
            .filter(v -> "desert-lizard".equals(v.getVariantId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Integer.valueOf(24), lizard.getCombatLevel());
        assertEquals(25.0, lizard.getSlayerXp().doubleValue(), 0.0);

        SourceTaskLocationComparison desert = task.getLocationComparison().stream()
            .filter(l -> "kharidian-desert-lizards".equals(l.getLocationId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals(Boolean.FALSE, desert.getCannonable());
    }

    @Test
    public void monsterVariantCarriesHonestOffence() throws IOException
    {
        SourceMonsterVariant lizard = read(
            Paths.get("src/main/data/slayer/monsters/lizards/desert-lizard.json"), SourceMonsterVariant.class);
        assertArrayEquals(new int[] {459, 12003, 460, 461}, toIntArray(lizard.getNpcIds()));
        assertFalse(lizard.isUndead());
        assertNotNull(lizard.getOffence());
        assertEquals(Integer.valueOf(25), lizard.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(3), lizard.getOffence().getMaxHit());
        assertTrue(lizard.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertNull(lizard.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/lizards/strategy.json");
        assertTrue("Lizards strategy JSON missing", Files.exists(json));
        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("lizards", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getMethods().size() >= 2);
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("ice cooler")));
    }

    @Test
    public void generatedRuntimeDataCarriesLizardsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Lizards".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Lizards"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(1, task.getVariants().size());

        MonsterVariant lizard = task.getVariants().stream()
            .filter(MonsterVariant::isDefault).findFirst().orElseThrow(AssertionError::new);
        assertNotNull(lizard.getStrategy());
        assertEquals(CombatStyle.MELEE, lizard.getStrategy().getPrimaryStyle());
        assertNotNull(lizard.getOffence());
        assertEquals(Integer.valueOf(25), lizard.getOffence().getHitpoints());
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

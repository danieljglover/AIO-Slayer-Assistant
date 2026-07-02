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
import java.util.List;
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
 * C3-B (ADR-0019 recipe): pins the wiki-authored Cockatrice family. Mazchna-assigned starter-tier
 * task requiring 25 Slayer and an equipped mirror shield / V's shield (item 4156, 20 Defence to
 * wield). Uses a tuned copy of the shared Fremennik Slayer Dungeon location
 * ({@code fremennik-slayer-dungeon-cockatrice}, konarLockable:false vs the konar-lockable Basilisk
 * base). Vannaka also assigns cockatrice on the live wiki but is outside the C3 starter scope
 * (sibling banshees precedent). Stats from the live Cockatrice wiki page, fetched 2026-07-02.
 * slayerTargetId is SYNTHETIC (282) per the C2/C3 allocation contract.
 */
public class CockatriceSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cockatrice.json"), SourceTask.class);

        assertEquals(282, task.getSlayerTargetId());
        assertEquals(Integer.valueOf(25), task.getCombatLevel());
        assertEquals(25, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("mazchna"));

        assertNull("Cockatrice have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Cockatrice have no unlocks", task.getUnlocks().isEmpty());
        assertEquals("mirror shield / V's shield", Integer.valueOf(4156), task.getRequiredItemId());

        assertEquals(Set.of("fremennik-slayer-dungeon-cockatrice"), locationIds(task));

        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull("cockatrice have no elemental weakness", task.getWeakness().getElement());

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("mirror shield")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant cockatrice = read(
            Paths.get("src/main/data/slayer/monsters/cockatrice/cockatrice.json"),
            SourceMonsterVariant.class);
        assertEquals("cockatrice", cockatrice.getVariantId());
        assertTrue(cockatrice.getNpcIds().contains(419));
        assertEquals(CombatStyle.MELEE, cockatrice.getWeakness().getStyle());
        assertNull(cockatrice.getWeakness().getElement());
        assertEquals(37, cockatrice.getMonsterDefence().getDefenceLevel());
        assertEquals(0, cockatrice.getMonsterDefence().getCrush());

        assertNotNull("cockatrice offence authored", cockatrice.getOffence());
        assertEquals(Integer.valueOf(37), cockatrice.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(5), cockatrice.getOffence().getMaxHit());
        assertEquals(Integer.valueOf(4), cockatrice.getOffence().getAttackSpeedTicks());
        assertEquals(List.of(AttackStyle.MELEE), cockatrice.getOffence().getAttackStyles());
        assertFalse("cockatrice are not poisonous", cockatrice.getOffence().isPoisonous());
        assertNull("magicLevel is honest-UNKNOWN", cockatrice.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/cockatrice/strategy.json");
        assertTrue("Cockatrice strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("cockatrice", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
        assertTrue("mirror shield called out in the strategy note",
            strategy.getPlugin().getNote().contains("mirror shield"));
    }

    @Test
    public void tunedLocationSourceMatchesWiki() throws IOException
    {
        SourceLocation dungeon = read(
            Paths.get("src/main/data/slayer/locations/fremennik-slayer-dungeon-cockatrice.json"),
            SourceLocation.class);
        assertEquals("fremennik-slayer-dungeon-cockatrice", dungeon.getLocationId());
        assertEquals(false, dungeon.isWilderness());
        assertEquals("C2/C3 files are never konar-lockable", false, dungeon.isKonarLockable());
        assertTrue(dungeon.getAccessNote().contains("mirror shield"));
    }

    @Test
    public void generatedRuntimeDataResolvesCockatrice() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Cockatrice".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cockatrice"));

        assertEquals(282, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant cockatrice = task.getVariants().stream()
            .filter(variant -> "Cockatrice".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cockatrice variant"));
        assertNotNull(cockatrice.getStrategy());
        assertEquals(CombatStyle.MELEE, cockatrice.getStrategy().getPrimaryStyle());
        assertNotNull("offence compiled onto runtime variant", cockatrice.getOffence());
        assertEquals(Integer.valueOf(37), cockatrice.getOffence().getHitpoints());
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

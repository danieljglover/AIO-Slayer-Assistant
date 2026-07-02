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
 * C3-F (ADR-0019 recipe): pins the wiki-authored Ghoul family. Mazchna starter-tier task, no Slayer
 * level required, west of Canifis (Priest in Peril for Morytania access). Stats from the live Ghoul
 * wiki page (id 13610), fetched 2026-07-02. slayerTargetId is SYNTHETIC (290) per the C2/C3
 * allocation contract. Ghouls are NOT undead - the Salve amulet does not apply.
 */
public class GhoulsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ghouls.json"), SourceTask.class);

        assertEquals(Integer.valueOf(13610), task.getWikiPageId());
        assertEquals(290, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("Priest in Peril required", task.getQuestReqs().contains("Priest in Peril"));

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Ghouls have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Ghouls have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertFalse("Ghouls are NOT undead (Salve does not apply)", task.isUndead());
        assertEquals(Set.of("canifis-ghoul-area"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant ghoul = read(
            Paths.get("src/main/data/slayer/monsters/ghoul/ghoul.json"), SourceMonsterVariant.class);
        assertEquals("ghoul", ghoul.getVariantId());
        assertTrue(ghoul.getNpcIds().contains(289));
        assertEquals(CombatStyle.MELEE, ghoul.getWeakness().getStyle());
        assertNull("no elemental weakness (honest null)", ghoul.getWeakness().getElement());
        assertEquals(30, ghoul.getMonsterDefence().getDefenceLevel());
        assertFalse("Ghouls are not undead", ghoul.isUndead());

        assertNotNull("ghoul offence authored", ghoul.getOffence());
        assertEquals(Integer.valueOf(50), ghoul.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(5), ghoul.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE), ghoul.getOffence().getAttackStyles());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/ghouls/strategy.json");
        assertTrue("Ghoul strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("ghouls", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void generatedRuntimeDataResolvesGhoul() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Ghoul".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ghoul"));

        assertEquals(290, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant ghoul = task.getVariants().stream()
            .filter(variant -> "Ghoul".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ghoul variant"));
        assertNotNull(ghoul.getStrategy());
        assertNotNull("offence compiled onto runtime variant", ghoul.getOffence());
        assertEquals(Integer.valueOf(50), ghoul.getOffence().getHitpoints());
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

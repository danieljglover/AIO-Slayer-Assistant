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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * C2-E (ADR-0019 recipe): pins the wiki-authored Ice warriors family. SHARED family (worklist §5.4) -
 * authored once with Krystilia (Wilderness) + Mazchna (starter) masters and both the Wilderness
 * (Frozen Waste Plateau, referenced from ice-giants) and non-Wilderness (Asgarnian Ice Dungeon)
 * variant/location sets. Stats from the live Ice warrior wiki page, fetched 2026-07-02.
 * slayerTargetId is SYNTHETIC (262) per the C2/C3 allocation contract (worklist §1).
 */
public class IceWarriorsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ice-warriors.json"), SourceTask.class);

        assertEquals(262, task.getSlayerTargetId());
        assertEquals(Integer.valueOf(57), task.getCombatLevel());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("krystilia", "mazchna"), masterIds(task));
        assertArrayEquals(new int[] {100, 150}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 50}, task.getAmountByMaster().get("mazchna"));

        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Ice warriors have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Ice warriors have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("frozen-waste-plateau", "ice-warriors-area"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Turael and Spria do NOT")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant iceWarrior = read(
            Paths.get("src/main/data/slayer/monsters/ice-warriors/ice-warrior.json"), SourceMonsterVariant.class);
        assertEquals("ice-warrior", iceWarrior.getVariantId());
        assertTrue(iceWarrior.getNpcIds().contains(2841));
        assertEquals(CombatStyle.MELEE, iceWarrior.getWeakness().getStyle());
        assertEquals("fire", iceWarrior.getWeakness().getElement());
        assertEquals(47, iceWarrior.getMonsterDefence().getDefenceLevel());

        assertNotNull("ice warrior offence authored", iceWarrior.getOffence());
        assertEquals(Integer.valueOf(59), iceWarrior.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(6), iceWarrior.getOffence().getMaxHit());
        assertEquals(Integer.valueOf(4), iceWarrior.getOffence().getAttackSpeedTicks());
        assertEquals(List.of(AttackStyle.MELEE), iceWarrior.getOffence().getAttackStyles());
        assertNull("magicLevel is honest-UNKNOWN", iceWarrior.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/ice-warriors/strategy.json");
        assertTrue("Ice warriors strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("ice-warriors", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void ownedLocationSourcesMatchWiki() throws IOException
    {
        SourceLocation wildy = read(
            Paths.get("src/main/data/slayer/locations/frozen-waste-plateau.json"), SourceLocation.class);
        assertEquals(true, wildy.isWilderness());

        SourceLocation area = read(
            Paths.get("src/main/data/slayer/locations/ice-warriors-area.json"), SourceLocation.class);
        assertEquals("ice-warriors-area", area.getLocationId());
        assertEquals(false, area.isWilderness());
        assertEquals(false, area.isKonarLockable());
    }

    @Test
    public void generatedRuntimeDataResolvesIceWarriors() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Ice warriors".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ice warriors"));

        assertEquals(262, task.getSlayerTargetId());
        assertEquals(Set.of("krystilia", "mazchna"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant iceWarrior = task.getVariants().stream()
            .filter(variant -> "Ice warrior".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ice warrior variant"));
        assertNotNull(iceWarrior.getStrategy());
        assertEquals(CombatStyle.MELEE, iceWarrior.getStrategy().getPrimaryStyle());
        assertNotNull("offence compiled onto runtime variant", iceWarrior.getOffence());
        assertEquals(Integer.valueOf(59), iceWarrior.getOffence().getHitpoints());
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

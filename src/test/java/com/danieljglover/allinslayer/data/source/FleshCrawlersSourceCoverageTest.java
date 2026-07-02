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
 * C3-C (ADR-0019 recipe): pins the wiki-authored Flesh Crawlers family. Mazchna starter-tier task,
 * no Slayer level required, on the 2nd level (Catacomb of Famine) of the Stronghold of Security.
 * Stats from the live Flesh Crawler wiki page, fetched 2026-07-02. slayerTargetId is SYNTHETIC (288)
 * per the C2/C3 allocation contract. Location is a family-tuned copy of the stronghold-of-security
 * base (Ankou flags do not apply).
 */
public class FleshCrawlersSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/flesh-crawlers.json"), SourceTask.class);

        assertEquals(288, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue("no quest requirement", task.getQuestReqs().isEmpty());

        assertEquals(Set.of("mazchna"), masterIds(task));
        assertArrayEquals(new int[] {15, 25}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertNull("Flesh Crawlers have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Flesh Crawlers have no unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("stronghold-of-security-flesh-crawlers"), locationIds(task));
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fire")));
    }

    @Test
    public void monsterSourceCarriesWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant crawler = read(
            Paths.get("src/main/data/slayer/monsters/flesh-crawlers/flesh-crawler.json"), SourceMonsterVariant.class);
        assertEquals("flesh-crawler", crawler.getVariantId());
        assertTrue(crawler.getNpcIds().contains(2498));
        assertTrue(crawler.getNpcIds().contains(2500));
        assertEquals(CombatStyle.MELEE, crawler.getWeakness().getStyle());
        assertEquals("fire", crawler.getWeakness().getElement());
        assertEquals(10, crawler.getMonsterDefence().getDefenceLevel());

        assertNotNull("flesh crawler offence authored", crawler.getOffence());
        assertEquals(Integer.valueOf(25), crawler.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(1), crawler.getOffence().getMaxHit());
        assertEquals(List.of(AttackStyle.MELEE), crawler.getOffence().getAttackStyles());
        assertNull("magicLevel is honest-UNKNOWN", crawler.getOffence().getMagicLevel());
    }

    @Test
    public void strategySourceHasTwoWikiMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/flesh-crawlers/strategy.json");
        assertTrue("Flesh Crawlers strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("flesh-crawlers", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-scimitar"));
        assertTrue("gate: >=2 methods", strategy.getMethods().size() >= 2);
    }

    @Test
    public void generatedRuntimeDataResolvesFleshCrawlers() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Flesh Crawlers".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Flesh Crawlers"));

        assertEquals(288, task.getSlayerTargetId());
        assertEquals(Set.of("mazchna"), task.getAssignedBy().stream().collect(Collectors.toSet()));

        MonsterVariant crawler = task.getVariants().stream()
            .filter(variant -> "Flesh Crawler".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Flesh Crawler variant"));
        assertNotNull(crawler.getStrategy());
        assertNotNull("offence compiled onto runtime variant", crawler.getOffence());
        assertEquals(Integer.valueOf(1), crawler.getOffence().getMaxHit());
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

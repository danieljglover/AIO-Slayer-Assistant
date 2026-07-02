package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.LocationQuality;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.SlayerLocation;
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
 * C2-F (ADR-0019): pins the wiki-sourced shared Spiders family. Spiders is assigned by Krystilia
 * (Wilderness, 65-100, weight 6) and by the starter masters Turael and Spria (15-30, weight 6 each);
 * Mazchna does NOT assign Spiders. Not undead. Deadly red spiders are a distinct task - NOT folded
 * in. Monster stats verified live 2026-07-02 against the Spider wiki page. slayerTargetId 272 is
 * SYNTHETIC per the C2-C3 allocation contract. Owns spiders-wilderness + spiders-area.
 */
public class SpidersSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsSharedMastersAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/spiders.json"), SourceTask.class);

        assertNull("wikiPageId is honest-UNKNOWN", task.getWikiPageId());
        assertNull("no combat-level requirement", task.getCombatLevel());
        assertEquals(272, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        Set<String> masters = masterIds(task);
        assertEquals(3, masters.size());
        assertContains(masters, "krystilia");
        assertContains(masters, "turael");
        assertContains(masters, "spria");
        assertArrayEquals(new int[] {65, 100}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("spria"));
        assertNull("Mazchna does not assign Spiders", task.getWeightByMaster().get("mazchna"));

        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertFalse("spiders are not undead", task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());

        Set<String> locations = locationIds(task);
        assertContains(locations, "spiders-wilderness");
        assertContains(locations, "spiders-area");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("272")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Deadly red spiders")));
        assertEquals(1, task.getVariantInfo().size());
    }

    @Test
    public void variantOffenceIsWikiHonest() throws IOException
    {
        SourceMonsterVariant spider = read(
            Paths.get("src/main/data/slayer/monsters/spiders/spider.json"), SourceMonsterVariant.class);
        MonsterOffence offence = spider.getOffence();
        assertNotNull("authored offence block", offence);
        assertEquals(Integer.valueOf(2), offence.getHitpoints());
        assertEquals(Integer.valueOf(0), offence.getMaxHit());
        assertEquals(1, offence.getAttackStyles().size());
        assertEquals(AttackStyle.MELEE, offence.getAttackStyles().get(0));
        assertNull("attack speed not authored (honest UNKNOWN)", offence.getAttackSpeedTicks());
        assertNull("magic level not relevant (honest UNKNOWN)", offence.getMagicLevel());
        assertFalse(offence.isPoisonous());
        assertFalse(offence.isVenomous());
    }

    @Test
    public void locationComparisonAmountsHonestlyAbsent() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/spiders.json"), SourceTask.class);
        assertEquals(2, task.getLocationComparison().size());
        task.getLocationComparison().forEach(row ->
        {
            assertNull("density amount is honest-UNKNOWN", row.getAmount());
            assertEquals(Boolean.FALSE, row.getMulticombat());
            assertEquals(Boolean.FALSE, row.getCannonable());
            assertEquals(Boolean.FALSE, row.getSafespottable());
        });
    }

    @Test
    public void generatedRuntimeDataResolvesSpiders() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Spiders".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Spiders"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertEquals(1, task.getVariants().size());

        MonsterVariant def = task.getVariants().stream().filter(MonsterVariant::isDefault).findFirst()
            .orElseThrow(() -> new AssertionError("no default variant"));
        assertNotNull(def.getStrategy());
        assertEquals(CombatStyle.MELEE, def.getStrategy().getPrimaryStyle());
        assertNotNull("compiled variant offence", def.getOffence());
        assertEquals(Integer.valueOf(2), def.getOffence().getHitpoints());

        SlayerLocation wildy = task.getLocations().stream()
            .filter(l -> "Wilderness spider spawns".equals(l.getName()))
            .findFirst().orElseThrow(() -> new AssertionError("no wilderness location"));
        assertTrue(wildy.isWilderness());
        LocationQuality quality = wildy.getQuality();
        assertNotNull("locationComparison compiled to a quality overlay", quality);
        assertNull(quality.getAmount());
        assertEquals(Boolean.FALSE, quality.getMulticombat());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation wildy = read(Paths.get("src/main/data/slayer/locations/spiders-wilderness.json"),
            SourceLocation.class);
        assertTrue("Krystilia kills only count in the Wilderness", wildy.isWilderness());
        assertFalse(wildy.isKonarLockable());
        assertNotNull(wildy.getAccessNote());

        SourceLocation area = read(Paths.get("src/main/data/slayer/locations/spiders-area.json"),
            SourceLocation.class);
        assertFalse("starter spiders count outside the Wilderness", area.isWilderness());
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

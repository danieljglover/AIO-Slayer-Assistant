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
import java.util.Map;
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
 * C2-H (ADR-0019): Wilderness bosses meta-task coverage. Krystilia assigns a single "Wilderness
 * bosses" task (3-35, weight 8) gated by the "Like a boss" unlock (200 pts), rolling one boss from
 * her set. Verified against the live wiki 2026-07-02 (the Krystilia task table; the Artio, Spindel
 * and Calvar'ion monster pages; the King Black Dragon page confirming it is NOT in Krystilia's set).
 * SYNTHETIC slayerTargetId 274 (252-305 C2/C3 block).
 *
 * <p>Like {@code boss.json}, this is a meta-task: {@code weakness} and {@code monsterDefence} are
 * null (no single profile), the seven pre-existing boss variants are reused by variantId from the
 * {@code boss} monster family, and only the three singles-plus variants (Artio, Spindel, Calvar'ion)
 * are authored fresh under {@code monsters/wilderness-bosses/}. Each reused variant already routes to
 * its own strategy, so this family authors no strategy or location file (CT-L: zero new locations).
 */
public class WildernessBossesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMetaShape() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/wilderness-bosses.json"),
            SourceTask.class);

        assertNull(task.getCombatLevel());
        assertEquals(274, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertTrue(task.getMasterIds().contains("krystilia"));
        assertArrayEquals(new int[] {3, 35}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("krystilia"));

        // Meta-task: no extension, no per-task unlock row (mirrors boss.json; "Like a boss" is the
        // global reward), no single weakness/defence profile.
        assertNull(task.getExtendedAmount());
        assertTrue(task.getUnlocks() == null || task.getUnlocks().isEmpty());
        assertNull(task.getWeakness());
        assertNull(task.getMonsterDefence());

        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isDragon());
        assertFalse(task.isDemon());
        assertNull(task.getRequiredItemId());

        assertContains(locationIds(task), "various-boss-locations");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("Like a boss")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("King Black Dragon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("singles-plus")));
    }

    @Test
    public void taskReferencesTheSevenReusedBossesAndThreeSinglesPlusVariants() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/wilderness-bosses.json"),
            SourceTask.class);

        Set<String> variantIds = task.getVariantIds().stream().collect(Collectors.toSet());
        assertEquals(10, variantIds.size());
        // Reused from the boss monster family.
        assertContains(variantIds, "callisto");
        assertContains(variantIds, "venenatis");
        assertContains(variantIds, "vet-ion");
        assertContains(variantIds, "chaos-elemental");
        assertContains(variantIds, "chaos-fanatic");
        assertContains(variantIds, "crazy-archaeologist");
        assertContains(variantIds, "scorpia");
        // Authored fresh for this family.
        assertContains(variantIds, "artio");
        assertContains(variantIds, "spindel");
        assertContains(variantIds, "calvar-ion");
        // KBD is excluded (wiki: not in Krystilia's set); Revenants are a separate task.
        assertFalse(variantIds.contains("boss-king-black-dragon"));
        assertFalse(variantIds.contains("revenant-knight"));

        assertEquals("callisto", task.getDefaultVariantId());

        Map<String, SourceTaskVariantInfo> info = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        assertEquals(10, info.size());
        assertEquals(Integer.valueOf(320), info.get("artio").getCombatLevel());
        assertEquals(Integer.valueOf(302), info.get("spindel").getCombatLevel());
        assertEquals(Integer.valueOf(264), info.get("calvar-ion").getCombatLevel());
    }

    @Test
    public void newSinglesPlusVariantsMatchWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant artio = read(
            Paths.get("src/main/data/slayer/monsters/wilderness-bosses/artio-lvl320.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(320), artio.getCombatLevel());
        assertTrue(artio.getNpcIds().contains(11992));
        assertEquals(150, artio.getMonsterDefence().getDefenceLevel());
        assertEquals(CombatStyle.MAGIC, artio.getWeakness().getStyle());
        assertNull(artio.getWeakness().getElement());
        assertEquals("callisto", artio.getStrategyId());
        assertNull(artio.getLocationId());
        assertNotNull(artio.getOffence());
        assertEquals(Integer.valueOf(450), artio.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(50), artio.getOffence().getMaxHit());
        assertEquals(Integer.valueOf(90), artio.getOffence().getMagicLevel());
        assertTrue(artio.getOffence().getAttackStyles().contains(AttackStyle.MAGIC));
        assertTrue(artio.getOffence().getAttackStyles().contains(AttackStyle.RANGED));

        SourceMonsterVariant spindel = read(
            Paths.get("src/main/data/slayer/monsters/wilderness-bosses/spindel-lvl302.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(302), spindel.getCombatLevel());
        assertTrue(spindel.getNpcIds().contains(11998));
        assertEquals(CombatStyle.MELEE, spindel.getWeakness().getStyle());
        assertEquals("fire", spindel.getWeakness().getElement());
        assertEquals("venenatis", spindel.getStrategyId());
        assertEquals(Integer.valueOf(515), spindel.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(235), spindel.getOffence().getMagicLevel());

        SourceMonsterVariant calvarion = read(
            Paths.get("src/main/data/slayer/monsters/wilderness-bosses/calvar-ion-lvl264.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(264), calvarion.getCombatLevel());
        assertTrue(calvarion.getNpcIds().contains(11993));
        assertTrue(calvarion.isUndead());
        assertEquals(CombatStyle.MELEE, calvarion.getWeakness().getStyle());
        assertNull(calvarion.getWeakness().getElement());
        assertEquals("vet-ion", calvarion.getStrategyId());
        assertEquals(Integer.valueOf(150), calvarion.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(26), calvarion.getOffence().getMaxHit());
        assertEquals(2, calvarion.getOffence().getAttackStyles().size());
    }

    @Test
    public void locationComparisonUsesTheReusedPlaceholder() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/wilderness-bosses.json"),
            SourceTask.class);
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));
        assertEquals(1, locations.size());
        SourceTaskLocationComparison various = locations.get("various-boss-locations");
        assertNotNull(various);
        assertEquals(Boolean.TRUE, various.getMulticombat());
        assertEquals(Boolean.FALSE, various.getCannonable());
        assertNull(various.getAmount());
        assertTrue(various.getNotes().stream().anyMatch(n -> n.contains("singles-plus")));
    }

    @Test
    public void generatedRuntimeDataResolvesTheBossSetAndStrategies() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Wilderness bosses".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Wilderness bosses"));

        assertTrue(task.getAssignedBy().contains("krystilia"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("krystilia"));
        assertNull(task.getExtendedAmount());
        assertEquals(10, task.getVariants().size());

        MonsterVariant artio = task.getVariants().stream()
            .filter(v -> "Artio".equals(v.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Artio variant"));
        // Artio reuses Callisto's strategy (same fight), whose plugin primary style is RANGED.
        assertNotNull(artio.getStrategy());
        assertEquals(CombatStyle.RANGED, artio.getStrategy().getPrimaryStyle());
        assertNotNull(artio.getOffence());
        assertEquals(Integer.valueOf(450), artio.getOffence().getHitpoints());

        MonsterVariant callisto = task.getVariants().stream()
            .filter(v -> "Callisto".equals(v.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Callisto variant"));
        assertNotNull(callisto.getStrategy());
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("missing " + expected + " in " + values, values.contains(expected));
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

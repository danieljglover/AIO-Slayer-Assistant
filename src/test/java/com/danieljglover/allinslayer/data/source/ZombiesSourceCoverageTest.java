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
 * C2-F (ADR-0019): pins the wiki-sourced shared Zombies family. Zombies is undead and is assigned by
 * Krystilia (Wilderness, 75-125, weight 3) and by the starter masters Turael/Spria (15-30, w7) and
 * Mazchna (30-50, w7). Owner of the shared new location graveyard-of-shadows (CT-L). Monster stats
 * verified live 2026-07-02 against the Zombie wiki page (npcs 26/42/49). slayerTargetId 273 is
 * SYNTHETIC per the C2-C3 allocation contract.
 */
public class ZombiesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsSharedMastersAndUndeadFlag() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/zombies.json"), SourceTask.class);

        assertNull("wikiPageId is honest-UNKNOWN", task.getWikiPageId());
        assertNull("no combat-level requirement", task.getCombatLevel());
        assertEquals(273, task.getSlayerTargetId());
        assertTrue(task.getQuestReqs().isEmpty());

        Set<String> masters = masterIds(task);
        assertEquals(4, masters.size());
        assertContains(masters, "krystilia");
        assertContains(masters, "turael");
        assertContains(masters, "spria");
        assertContains(masters, "mazchna");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("turael"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("spria"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("mazchna"));

        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertTrue("zombies are undead", task.isUndead());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());

        Set<String> locations = locationIds(task);
        assertContains(locations, "graveyard-of-shadows");
        assertContains(locations, "zombies-area");

        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("SYNTHETIC") && n.contains("273")));
        assertTrue(task.getTaskNotes().stream().anyMatch(n -> n.contains("undead")));
        assertEquals(3, task.getVariantInfo().size());
    }

    @Test
    public void variantOffenceIsWikiHonest() throws IOException
    {
        MonsterOffence lvl24 = read(
            Paths.get("src/main/data/slayer/monsters/zombies/zombie-lvl24.json"),
            SourceMonsterVariant.class).getOffence();
        assertNotNull(lvl24);
        assertEquals(Integer.valueOf(30), lvl24.getHitpoints());
        assertEquals(Integer.valueOf(3), lvl24.getMaxHit());
        assertEquals(1, lvl24.getAttackStyles().size());
        assertEquals(AttackStyle.MELEE, lvl24.getAttackStyles().get(0));
        assertNull("magic level not relevant (honest UNKNOWN)", lvl24.getMagicLevel());
        assertFalse(lvl24.isPoisonous());

        MonsterOffence lvl13 = read(
            Paths.get("src/main/data/slayer/monsters/zombies/zombie-lvl13.json"),
            SourceMonsterVariant.class).getOffence();
        assertEquals(Integer.valueOf(22), lvl13.getHitpoints());
    }

    @Test
    public void undeadFlagOnEveryVariant() throws IOException
    {
        for (String v : new String[] {"zombie-lvl13", "zombie-lvl18", "zombie-lvl24"})
        {
            SourceMonsterVariant variant = read(
                Paths.get("src/main/data/slayer/monsters/zombies/" + v + ".json"), SourceMonsterVariant.class);
            assertTrue(v + " must be undead", variant.isUndead());
        }
    }

    @Test
    public void locationComparisonAmountsHonestlyAbsent() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/zombies.json"), SourceTask.class);
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
    public void generatedRuntimeDataResolvesZombies() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Zombies".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Zombies"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertEquals(3, task.getVariants().size());

        MonsterVariant def = task.getVariants().stream().filter(MonsterVariant::isDefault).findFirst()
            .orElseThrow(() -> new AssertionError("no default variant"));
        assertNotNull(def.getStrategy());
        assertEquals(CombatStyle.MELEE, def.getStrategy().getPrimaryStyle());
        assertNotNull("compiled variant offence", def.getOffence());
        assertEquals(Integer.valueOf(30), def.getOffence().getHitpoints());

        SlayerLocation graveyard = task.getLocations().stream()
            .filter(l -> "Graveyard of Shadows".equals(l.getName()))
            .findFirst().orElseThrow(() -> new AssertionError("no graveyard location"));
        assertTrue(graveyard.isWilderness());
        LocationQuality quality = graveyard.getQuality();
        assertNotNull("locationComparison compiled to a quality overlay", quality);
        assertNull(quality.getAmount());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation graveyard = read(
            Paths.get("src/main/data/slayer/locations/graveyard-of-shadows.json"), SourceLocation.class);
        assertEquals("graveyard-of-shadows", graveyard.getLocationId());
        assertTrue("Krystilia kills only count in the Wilderness", graveyard.isWilderness());
        assertFalse(graveyard.isKonarLockable());
        assertNotNull(graveyard.getAccessNote());

        SourceLocation area = read(
            Paths.get("src/main/data/slayer/locations/zombies-area.json"), SourceLocation.class);
        assertFalse("starter zombies count outside the Wilderness", area.isWilderness());
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

package com.danieljglover.allinslayer.data.source;

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
import static org.junit.Assert.assertTrue;

/**
 * WC-11 (ADR-0019): pins the wiki-sourced Fever spiders family. Assignment numbers were
 * re-verified live on 2026-07-02 against the Vannaka/Chaeldar wiki pages (PD-D, wiki wins).
 * slayerTargetId 251 is SYNTHETIC per the WC-1..11 team contract. The monster page has no
 * Slayer task subpage; the sole location is the Braindeath Island brewery basement (Rum Deal).
 * Slayer gloves (ItemID.SLAYER_GLOVES = 6708) are the task's required item.
 */
public class FeverSpidersSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/fever-spiders.json"), SourceTask.class);

        assertEquals(Integer.valueOf(15976), task.getWikiPageId());
        assertEquals(Integer.valueOf(40), task.getCombatLevel());
        assertEquals(251, task.getSlayerTargetId());
        assertEquals(42, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Rum Deal (partial)"));

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertArrayEquals(new int[] {30, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        // Weights re-verified live on the master pages 2026-07-02 (Vannaka total 325, Chaeldar 360).
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("chaeldar"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "braindeath-island");

        assertEquals(Integer.valueOf(6708), task.getRequiredItemId());
        assertEquals("Slayer gloves", task.getRequiredItemName());
        assertFalse(task.isUndead());
        assertFalse(task.isDemon());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());
        assertEquals(40, task.getMonsterDefence().getDefenceLevel());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("251")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Slayer gloves")
            && note.contains("disease")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Rum Deal")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Relicym's balm")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo variant = task.getVariantInfo().get(0);
        assertEquals("fever-spider", variant.getVariantId());
        assertEquals(Integer.valueOf(49), variant.getCombatLevel());
        assertEquals(40.0, variant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        SourceTaskLocationComparison location = task.getLocationComparison().get(0);
        assertEquals("braindeath-island", location.getLocationId());
        assertEquals(Integer.valueOf(11), location.getAmount());
        assertEquals(Boolean.TRUE, location.getSafespottable());
    }

    @Test
    public void strategySourceCarriesGlovesAndFireWeaknessPluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/fever-spiders/strategy.json");

        assertTrue("Fever spiders strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("fever-spiders", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Fever_spider", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-seas".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("Slayer gloves"));
        assertTrue(strategy.getRequirements().stream().anyMatch(req -> req.contains("42 Slayer")));
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("fire spells")));
    }

    @Test
    public void generatedRuntimeDataCarriesFeverSpidersAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Fever spiders".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Fever spiders"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(6708), task.getRequiredItemId());
        assertTrue(task.getLocations().stream().anyMatch(
            location -> "Braindeath Island".equals(location.getName()) && location.isSafeSpot()));

        assertEquals(1, task.getVariants().size());

        MonsterVariant spider = task.getVariants().get(0);
        assertEquals("Fever spider", spider.getName());
        assertTrue(spider.isDefault());
        assertNotNull(spider.getStrategy());
        assertEquals(CombatStyle.MELEE, spider.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/braindeath-island.json"),
            SourceLocation.class);

        assertEquals("braindeath-island", location.getLocationId());
        assertEquals("Braindeath Island", location.getName());
        // The brewery basement is wiki-noted multicombat; cannon placement is not wiki-stated (rule 7).
        assertTrue(location.isMulti());
        assertFalse(location.isCannon());
        assertTrue("crate safespots are wiki-documented", location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertFalse("Konar does not assign Fever spiders", location.isKonarLockable());
        assertTrue(location.getAccessNote().contains("Rum Deal"));
        assertTrue(location.getAccessNote().contains("Slayer gloves"));
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

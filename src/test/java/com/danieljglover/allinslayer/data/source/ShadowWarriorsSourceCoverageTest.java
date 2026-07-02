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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WC-9 (ADR-0019): pins the wiki-sourced Shadow warriors family. Assignment numbers were
 * re-verified live on 2026-07-02 against the Vannaka/Chaeldar wiki pages (PD-D, wiki wins).
 * slayerTargetId 249 is SYNTHETIC per the WC-1..11 team contract. The monster page has no
 * Slayer task subpage; the sole location is the Legends' Guild Dungeon (WC-0). Shadow warriors
 * are NOT undead (Salve amulet does not apply) despite the ghostly appearance.
 */
public class ShadowWarriorsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/shadow-warriors.json"), SourceTask.class);

        assertEquals(Integer.valueOf(15476), task.getWikiPageId());
        assertEquals(Integer.valueOf(60), task.getCombatLevel());
        assertEquals(249, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Legends' Quest"));

        assertEquals(2, task.getMasterIds().size());
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertArrayEquals(new int[] {30, 80}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        // Weights re-verified live on the master pages 2026-07-02 (Vannaka total 325, Chaeldar 360).
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("chaeldar"));
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertEquals(1, task.getLocationIds().size());
        assertContains(locationIds(task), "legends-guild-dungeon");

        assertFalse("Shadow warriors are NOT undead (Salve does not apply)", task.isUndead());
        assertFalse(task.isDemon());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull("no wiki-documented elemental weakness", task.getWeakness().getElement());
        assertEquals(36, task.getMonsterDefence().getDefenceLevel());
        assertEquals(19, task.getMonsterDefence().getCrush());
        assertNull(task.getRequiredItemId());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("249")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Legends' Quest")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("not undead")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fence")));

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo variant = task.getVariantInfo().get(0);
        assertEquals("shadow-warrior", variant.getVariantId());
        assertEquals(Integer.valueOf(48), variant.getCombatLevel());
        assertEquals(67.0, variant.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        SourceTaskLocationComparison location = task.getLocationComparison().get(0);
        assertEquals("legends-guild-dungeon", location.getLocationId());
        assertEquals(Integer.valueOf(19), location.getAmount());
        assertEquals(Boolean.TRUE, location.getSafespottable());
    }

    @Test
    public void strategySourceCarriesCrushPluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/shadow-warriors/strategy.json");

        assertTrue("Shadow warriors strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("shadow-warriors", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Shadow_warrior", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-bludgeon"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-seas".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("crush"));
    }

    @Test
    public void generatedRuntimeDataCarriesShadowWarriorsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Shadow warriors".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Shadow warriors"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("chaeldar"));
        assertFalse(task.isUndead());
        assertTrue(task.getLocations().stream().anyMatch(
            location -> "Legends' Guild Dungeon".equals(location.getName()) && location.isSafeSpot()));

        assertEquals(1, task.getVariants().size());

        MonsterVariant warrior = task.getVariants().get(0);
        assertEquals("Shadow warrior", warrior.getName());
        assertTrue(warrior.isDefault());
        assertNotNull(warrior.getStrategy());
        assertEquals(CombatStyle.MELEE, warrior.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourceMatchesWiki() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/legends-guild-dungeon.json"),
            SourceLocation.class);

        assertEquals("legends-guild-dungeon", location.getLocationId());
        assertEquals("Legends' Guild Dungeon", location.getName());
        // multi/cannon are not wiki-stated for the shadow warrior area: rule 7 conservative defaults.
        assertFalse(location.isMulti());
        assertFalse(location.isCannon());
        assertTrue("fence/fungus safespots are wiki-documented", location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertFalse("Konar does not assign Shadow warriors", location.isKonarLockable());
        assertTrue(location.getAccessNote().contains("Legends' Quest"));
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

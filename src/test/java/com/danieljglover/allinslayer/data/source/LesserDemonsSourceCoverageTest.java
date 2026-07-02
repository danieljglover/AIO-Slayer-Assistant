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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WC-8 (ADR-0019): pins the wiki-sourced Lesser demons family. Assignment numbers were re-verified
 * live on 2026-07-02 against the Krystilia/Vannaka/Chaeldar wiki pages (PD-D, wiki wins). The
 * slayerTargetId 248 is SYNTHETIC per the WC-1..11 team contract (240 + WC index); the real varp
 * 395 value is a QA live-verify item. Lesser demons have NO extension unlock and NO superior.
 */
public class LesserDemonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lesser-demons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(548773), task.getWikiPageId());
        assertEquals(Integer.valueOf(60), task.getCombatLevel());
        assertEquals(248, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(3, task.getMasterIds().size());
        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertArrayEquals(new int[] {80, 120}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        // Weights re-verified live on the master pages 2026-07-02 (Krystilia total 196, Vannaka 325,
        // Chaeldar 360 corroborated).
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(9), task.getWeightByMaster().get("chaeldar"));
        // No extension unlock exists for Lesser demons (wiki task page carries no extended amounts).
        assertTrue(task.getExtendedAmount().isEmpty());
        assertTrue(task.getUnlocks().isEmpty());

        assertContains(locationIds(task), "demonic-ruins");
        assertContains(locationIds(task), "taverley-dungeon");
        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "isle-of-souls-dungeon");
        assertContains(locationIds(task), "wilderness-slayer-cave-lesser-demons");
        assertContains(locationIds(task), "charred-dungeon-lesser-demons");
        assertContains(locationIds(task), "king-black-dragon-lair-lesser-demons");
        assertContains(locationIds(task), "lava-maze");
        assertContains(locationIds(task), "chasm-of-fire-middle");
        assertContains(locationIds(task), "sisterhood-sanctuary");
        assertContains(locationIds(task), "crandor-and-karamja-dungeon");
        assertContains(locationIds(task), "crandor");
        assertContains(locationIds(task), "wizards-tower");
        assertContains(locationIds(task), "kourend-castle");
        assertContains(locationIds(task), "kingstown");
        assertContains(locationIds(task), "temple-of-ikov");
        assertContains(locationIds(task), "viyeldi-caves");

        assertTrue("Lesser demons are demons", task.isDemon());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("water", task.getWeakness().getElement());
        assertNull(task.getRequiredItemId());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")
            && note.contains("248")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("demonbane")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("water spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Zakl'n Gritch")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia")
            && note.contains("Wilderness")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lesser-demons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, variants.size());
        assertEquals(79.0, variants.get("lesser-demon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(85.0, variants.get("lesser-demon-catacombs-lvl87").getSlayerXp().doubleValue(), 0.0);
        assertEquals(98.0, variants.get("lesser-demon-catacombs-lvl94").getSlayerXp().doubleValue(), 0.0);
        assertEquals(110.0, variants.get("lesser-demon-wilderness-lvl94").getSlayerXp().doubleValue(), 0.0);
        assertEquals(150.0, variants.get("zakln-gritch").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("zakln-gritch").getNotes().stream()
            .anyMatch(note -> note.contains("God Wars Dungeon")));

        assertEquals(17, locations.size());
        assertLocation(locations, "chasm-of-fire-middle", 9, false, true, true, "Yama");
        assertLocation(locations, "catacombs-of-kourend", 8, true, false, true, "ancient shard");
        assertLocation(locations, "crandor-and-karamja-dungeon", 11, false, true, true, "Crandor");
        assertLocation(locations, "wilderness-slayer-cave-lesser-demons", 7, true, true, true, "Wilderness");
        assertLocation(locations, "sisterhood-sanctuary", 9, false, true, true, "Slepe");
        assertLocation(locations, "charred-dungeon-lesser-demons", 6, true, true, false, "Sailing");
        assertLocation(locations, "taverley-dungeon", 5, false, true, true, "Taverley");
        assertLocation(locations, "king-black-dragon-lair-lesser-demons", 4, true, true, true, "lever");
        assertLocation(locations, "isle-of-souls-dungeon", 4, false, true, true, "Soul Wars");
        assertLocation(locations, "temple-of-ikov", 3, true, true, true, "shiny key");
        assertLocation(locations, "viyeldi-caves", 3, true, true, true, "Legends'");
        assertLocation(locations, "lava-maze", 2, false, true, true, "Wilderness");
        assertLocation(locations, "demonic-ruins", 2, true, true, true, "Wilderness");
        assertLocation(locations, "crandor", 2, false, true, true, "Crandor");
        assertLocation(locations, "wizards-tower", 1, false, true, true, "caged");
        assertLocation(locations, "kourend-castle", 1, false, false, true, "caged");
        assertLocation(locations, "kingstown", 1, false, true, true, "Kingstown");
    }

    @Test
    public void strategySourceCarriesDemonbanePluginBlock() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/lesser-demons/strategy.json");

        assertTrue("Lesser demons strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("lesser-demons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Lesser_demon", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("emberlight"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("arclight"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-seas".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("demonbane"));
    }

    @Test
    public void generatedRuntimeDataCarriesLesserDemonsAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Lesser demons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Lesser demons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertEquals(Integer.valueOf(6), task.getWeightByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(9), task.getWeightByMaster().get("chaeldar"));
        assertTrue(task.isDemon());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Lava Maze".equals(location.getName())
            && location.isWilderness() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(
            location -> "Chasm of Fire (middle floor)".equals(location.getName())
                && !location.isMulti() && location.isCannon() && location.isSafeSpot()));

        assertEquals(5, task.getVariants().size());

        MonsterVariant lesserDemon = task.getVariants().stream()
            .filter(variant -> "Lesser demon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Lesser demon default variant"));

        assertTrue(lesserDemon.isDefault());
        assertNotNull(lesserDemon.getStrategy());
        assertEquals(CombatStyle.MELEE, lesserDemon.getStrategy().getPrimaryStyle());
        assertEquals("Emberlight", lesserDemon.getStrategy().getPrimaryWeapons().get(0).getName());

        MonsterVariant zakln = task.getVariants().stream()
            .filter(variant -> "Zakl'n Gritch".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Zakl'n Gritch variant"));
        assertTrue(zakln.isDemon());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        // Unique-to-create files per docs/full-review/wc0-location-ownership.md WC-8.
        assertLocationSource("lava-maze", "Lava Maze", false, true, true, true, "Wilderness");
        assertLocationSource("chasm-of-fire-middle", "Chasm of Fire (middle floor)", false, true, true, false,
            "Yama");
        assertLocationSource("sisterhood-sanctuary", "Sisterhood Sanctuary", false, true, true, false, "Slepe");
        assertLocationSource("crandor-and-karamja-dungeon", "Crandor and Karamja Dungeon", false, true, true,
            false, "Crandor");
        assertLocationSource("crandor", "Crandor", false, true, true, false, "Dragon Slayer I");
        assertLocationSource("wizards-tower", "Wizards' Tower", false, true, true, false, "caged");
        assertLocationSource("kourend-castle", "Kourend Castle", false, false, true, false, "caged");
        assertLocationSource("kingstown", "Kingstown", false, true, true, false, "Kingstown");
        assertLocationSource("temple-of-ikov", "Temple of Ikov", true, true, true, false, "shiny key");
        assertLocationSource("viyeldi-caves", "Viyeldi caves", true, true, true, false, "Legends' Quest");
        // Family-tuned copies (base flags diverge from the wiki lesser demon rows; never edit base files).
        assertLocationSource("wilderness-slayer-cave-lesser-demons", "Wilderness Slayer Cave", true, true,
            true, true, "safespot");
        assertLocationSource("charred-dungeon-lesser-demons", "Charred Dungeon", true, true, false, false,
            "not safespottable");
        assertLocationSource("king-black-dragon-lair-lesser-demons", "King Black Dragon Lair", true, true,
            true, false, "lever");
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        int amount, boolean multi, boolean cannon, boolean safespot, String note)
    {
        SourceTaskLocationComparison location = locations.get(locationId);
        assertNotNull("missing locationComparison row: " + locationId, location);
        assertEquals(Integer.valueOf(amount), location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(value -> value.contains(note)));
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safeSpot, boolean wilderness, String accessNote) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertEquals(safeSpot, location.isSafeSpot());
        assertEquals(wilderness, location.isWilderness());
        // Konar does not assign Lesser demons (WC-0 rule 6).
        assertEquals(false, location.isKonarLockable());
        assertTrue(location.getAccessNote().contains(accessNote));
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

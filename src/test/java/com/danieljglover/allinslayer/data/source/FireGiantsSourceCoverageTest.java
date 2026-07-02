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
import static org.junit.Assert.assertTrue;

public class FireGiantsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentRequirementsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/fire-giants.json"), SourceTask.class);

        assertEquals(Integer.valueOf(297889), task.getWikiPageId());
        assertEquals(Integer.valueOf(65), task.getCombatLevel());
        assertEquals(218, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("water", task.getWeakness().getElement());

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("rune scimitar")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("100%") && note.contains("water")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Catacombs of Kourend") && note.contains("recommended")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Giants' Den") && note.contains("second alternative")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("level 109") && note.contains("maximum xp")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Konar") && note.contains("Brimhaven Dungeon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Krystilia") && note.contains("Deep Wilderness Dungeon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Branda the Fire Queen") && note.contains("Royal Titans")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationComparisonRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/fire-giants.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertVariant(variants, "fire-giant", 86, 111.0, "rune scimitar");
        assertVariant(variants, "fire-giant-catacombs", 104, 133.5, "Catacombs");
        assertVariant(variants, "fire-giant-catacombs-lvl109", 109, 153.5, "prioritized");
        assertVariant(variants, "branda-the-fire-queen", 350, 735.0, "Royal Titans");

        assertEquals(10, locations.size());
        assertLocation(locations, "brimhaven-dungeon-fire-giants", 13, false, true, true, "Saniboch");
        assertLocation(locations, "charred-dungeon-fire-giants", 6, true, true, true, "60 Sailing");
        assertLocation(locations, "stronghold-slayer-cave-fire-giants", 12, false, true, true, "Groups of 3");
        assertLocation(locations, "deep-wilderness-dungeon-fire-giants", 5, false, true, true, "Monk of Zamorak");
        assertLocation(locations, "catacombs-of-kourend-fire-giants", 12, true, false, true, "Ancient shards");
        assertLocation(locations, "giants-den-fire-giants", 8, false, true, true, "Dark totem");
        assertLocation(locations, "karuulm-slayer-dungeon-fire-giants", 13, false, true, true, "boots of stone");
        assertLocation(locations, "smoke-dungeon-fire-giants", 6, false, true, true, "Facemask");
        assertLocation(locations, "waterfall-dungeon-fire-giants", 11, false, true, true, "Waterfall Quest");
        assertLocation(locations, "isle-of-souls-dungeon-fire-giants", 8, false, true, true, "hall entrance");
    }

    @Test
    public void strategySourceContainsAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/fire-giants/strategy.json");

        assertTrue("Fire giants strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("fire-giants", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Fire_giants", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("osmumten-s-fang"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("saradomin-sword"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-swamp".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("Catacombs"));
        assertTrue(strategy.getPlugin().getNote().contains("level 109"));

        assertContains(methodIds(strategy), "catacombs-afk");
        assertContains(methodIds(strategy), "giants-den-cannon");
        assertContains(methodIds(strategy), "konar-cannon-locations");
        assertContains(methodIds(strategy), "wilderness-krystilia");
        assertContains(methodIds(strategy), "safespot-ranged-magic");
        assertContains(methodIds(strategy), "branda-alternative");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "catacombs-afk".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing catacombs method"))
            .getSteps().stream().anyMatch(step -> step.contains("level 109")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness-krystilia".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing wilderness method"))
            .getRisks().stream().anyMatch(risk -> risk.contains("player killers")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "branda-alternative".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Branda method"))
            .getNotes().stream().anyMatch(note -> note.contains("helmet boost")));

        assertContains(styleIds(strategy), "melee-cannon");
        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "magic-water-spells");
        assertContains(styleIds(strategy), "wilderness-cannon");
    }

    @Test
    public void generatedRuntimeDataCarriesFireGiantStrategiesVariantsAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Fire giants".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Fire giants"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(4, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Catacombs of Kourend".equals(location.getName())
            && location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Deep Wilderness Dungeon".equals(location.getName())
            && !location.isMulti() && location.isCannon() && location.isWilderness()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Karuulm Slayer Dungeon".equals(location.getName())
            && !location.isMulti() && location.isCannon()));

        MonsterVariant fireGiant = variantNamed(task, "Fire giant");
        MonsterVariant level109 = variantNamed(task, "Fire giant (Catacombs level 109)");
        MonsterVariant branda = variantNamed(task, "Branda the Fire Queen");

        assertNotNull(fireGiant.getStrategy());
        assertEquals(CombatStyle.MELEE, fireGiant.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Fire_giants",
            fireGiant.getStrategy().getSourceUrl());
        assertNotNull(level109.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Fire_giants",
            level109.getStrategy().getSourceUrl());
        assertNotNull(branda.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Royal_Titans/Strategies", branda.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchFireGiantsTaskPage() throws IOException
    {
        assertLocationSource("brimhaven-dungeon-fire-giants", "Brimhaven Dungeon", false, true, true, false,
            "Saniboch");
        assertLocationSource("charred-dungeon-fire-giants", "Charred Dungeon", true, true, true, false,
            "60 Sailing");
        assertLocationSource("deep-wilderness-dungeon-fire-giants", "Deep Wilderness Dungeon", false, true, true,
            true, "Wilderness Medium Diary");
        assertLocationSource("karuulm-slayer-dungeon-fire-giants", "Karuulm Slayer Dungeon", false, true, true,
            false, "boots of stone");
        assertLocationSource("waterfall-dungeon-fire-giants", "Waterfall Dungeon", false, true, true, false,
            "Rope");
    }

    private static MonsterVariant variantNamed(TaskData task, String name)
    {
        return task.getVariants().stream()
            .filter(variant -> name.equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing variant: " + name));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream().map(SourceStrategyMethod::getMethodId).collect(Collectors.toSet());
    }

    private static Set<String> styleIds(SourceStrategy strategy)
    {
        return strategy.getStyleOptions().stream().map(SourceStrategyStyleOption::getStyleId)
            .collect(Collectors.toSet());
    }

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        Integer combatLevel, double slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(slayerXp, variant.getSlayerXp().doubleValue(), 0.0);
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        Integer amount, boolean multi, boolean cannon, boolean safespot, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertEquals(amount, location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safespot, boolean wilderness, String accessNote) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertEquals(safespot, location.isSafeSpot());
        assertEquals(wilderness, location.isWilderness());
        assertTrue(location.getAccessNote().contains(accessNote));
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

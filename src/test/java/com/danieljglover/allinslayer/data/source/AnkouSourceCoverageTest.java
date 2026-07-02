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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AnkouSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndUnlockData() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ankou.json"), SourceTask.class);

        assertEquals(Integer.valueOf(357194), task.getWikiPageId());
        assertEquals(Integer.valueOf(40), task.getCombatLevel());
        assertEquals(201, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {25, 35}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {50, 50}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {50, 90}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {50, 80}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {91, 150}, task.getExtendedAmount().get("krystilia"));
        assertArrayEquals(new int[] {91, 150}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "ankou-very-much");
        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "stronghold-of-security");
        assertContains(locationIds(task), "stronghold-slayer-cave");
        assertContains(locationIds(task), "wilderness-slayer-cave");
        assertContains(locationIds(task), "forgotten-cemetery");
        assertContains(locationIds(task), "deepfin-mine");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Salve amulet")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Slayer helmet")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Burst")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Cannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ectoplasmator")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Bonecrusher")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("one-click teleport")));
    }

    @Test
    public void taskSourceContainsWikiVariantRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ankou.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));

        assertEquals(6, variants.size());
        assertEquals(60.0, variants.get("ankou-level-75").getSlayerXp().doubleValue(), 0.0);
        assertEquals(65.0, variants.get("ankou-level-82").getSlayerXp().doubleValue(), 0.0);
        assertEquals(70.0, variants.get("ankou-level-86").getSlayerXp().doubleValue(), 0.0);
        assertEquals(60.0, variants.get("ankou-level-95").getSlayerXp().doubleValue(), 0.0);
        assertEquals(100.0, variants.get("ankou-level-98").getSlayerXp().doubleValue(), 0.0);
        assertEquals(60.0, variants.get("dark-ankou").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("ankou-level-75").getLocations().contains("Deepfin Mine"));
        assertTrue(variants.get("dark-ankou").getNotes().stream().anyMatch(note -> note.contains("Skotizo")));
    }

    @Test
    public void taskSourceContainsWikiLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/ankou.json"), SourceTask.class);
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, locations.size());
        assertEquals(Integer.valueOf(10), locations.get("catacombs-of-kourend").getAmount());
        assertEquals(Boolean.TRUE, locations.get("catacombs-of-kourend").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("catacombs-of-kourend").getCannonable());
        assertTrue(locations.get("catacombs-of-kourend").getNotes().get(0).contains("Bonecrusher"));

        assertEquals(Integer.valueOf(32), locations.get("stronghold-of-security").getAmount());
        assertEquals(Boolean.FALSE, locations.get("stronghold-of-security").getMulticombat());
        assertEquals(Boolean.TRUE, locations.get("stronghold-of-security").getCannonable());
        assertEquals(Boolean.TRUE, locations.get("stronghold-of-security").getSafespottable());

        assertEquals(Integer.valueOf(8), locations.get("stronghold-slayer-cave").getAmount());
        assertEquals(Boolean.FALSE, locations.get("stronghold-slayer-cave").getMulticombat());
        assertEquals(Boolean.TRUE, locations.get("stronghold-slayer-cave").getCannonable());

        assertEquals(Integer.valueOf(10), locations.get("wilderness-slayer-cave").getAmount());
        assertEquals(Boolean.TRUE, locations.get("wilderness-slayer-cave").getMulticombat());
        assertEquals(Boolean.TRUE, locations.get("wilderness-slayer-cave").getCannonable());
        assertTrue(locations.get("wilderness-slayer-cave").getNotes().get(0).contains("Royal seed pod"));

        assertEquals(Integer.valueOf(21), locations.get("forgotten-cemetery").getAmount());
        assertEquals(Boolean.FALSE, locations.get("forgotten-cemetery").getMulticombat());
        assertEquals(Boolean.TRUE, locations.get("forgotten-cemetery").getCannonable());
        assertEquals(Boolean.TRUE, locations.get("forgotten-cemetery").getSafespottable());
    }

    @Test
    public void strategySourceContainsWikiStrategyBreakdown() throws IOException
    {
        Path legacyMarkdown = Paths.get("src/main/data/slayer/strategies/ankou.md");
        Path json = Paths.get("src/main/data/slayer/strategies/ankou/strategy.json");

        assertFalse("Ankou strategy should be JSON, not Markdown", Files.exists(legacyMarkdown));
        assertTrue("Ankou strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("ankou", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Ankous", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "venator-bow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "kodai-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "single-combat-melee");
        assertContains(methodIds(strategy), "multicombat-magic");
        assertContains(methodIds(strategy), "multicombat-ranged");
        assertContains(methodIds(strategy), "cannon");
        assertContains(methodIds(strategy), "wilderness");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "general".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing general method"))
            .getNotes().stream().anyMatch(note -> note.contains("Salve amulet")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing wilderness method"))
            .getRisks().stream().anyMatch(risk -> risk.contains("Player killing")));

        assertContains(styleIds(strategy), "melee");
        assertContains(styleIds(strategy), "magic");
        assertContains(styleIds(strategy), "ranged");
    }

    @Test
    public void generatedRuntimeDataCarriesAnkouStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Ankou".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ankou"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Deepfin Mine".equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "The Forgotten Cemetery".equals(location.getName())));
        assertTrue(task.isUndead());

        MonsterVariant ankou = task.getVariants().stream()
            .filter(variant -> "Ankou (Level 75)".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing standard Ankou variant"));

        assertNotNull(ankou.getStrategy());
        assertEquals(CombatStyle.MELEE, ankou.getStrategy().getPrimaryStyle());
        assertEquals("Abyssal whip", ankou.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(ankou.getStrategy().getNote().contains("Salve"));
    }

    @Test
    public void forgottenCemeteryLocationSourceExists() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/forgotten-cemetery.json"),
            SourceLocation.class);

        assertEquals("forgotten-cemetery", location.getLocationId());
        assertEquals("The Forgotten Cemetery", location.getName());
        assertFalse(location.isMulti());
        assertTrue(location.isCannon());
        assertTrue(location.isSafeSpot());
        assertTrue(location.isWilderness());
        assertTrue(location.getAccessNote().contains("south-west coffin"));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> unlockIds(SourceTask task)
    {
        return task.getUnlocks().stream().map(SourceTaskUnlock::getUnlockId).collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
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

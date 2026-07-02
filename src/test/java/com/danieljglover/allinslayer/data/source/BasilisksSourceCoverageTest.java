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
import static org.junit.Assert.assertTrue;

public class BasilisksSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/basilisks.json"), SourceTask.class);

        assertEquals(Integer.valueOf(256653), task.getWikiPageId());
        assertEquals(Integer.valueOf(40), task.getCombatLevel());
        assertEquals(205, task.getSlayerTargetId());
        assertEquals(40, task.getSlayerLevel());
        assertTrue("task itself has no quest requirement", task.getQuestReqs().isEmpty());

        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {110, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("vannaka"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "basilocked");
        assertContains(unlockIds(task), "basilonger");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("20 Defence")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Basilocked")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("mirror shield") && note.contains("V's shield")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("gaze")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("extremely inaccurate melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("skip") && note.contains("blocking")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("The Fremennik Exiles")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Neitiznot faceguard")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Basilisk jaw") && note.contains("1/1000")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("100 per hour")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("attack speed")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Basilisk Sentinel")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Cannons cannot be placed")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/basilisks.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, variants.size());
        assertEquals(75.0, variants.get("basilisk").getSlayerXp().doubleValue(), 0.0);
        assertEquals(1700.0, variants.get("monstrous-basilisk").getSlayerXp().doubleValue(), 0.0);
        assertEquals(300.0, variants.get("basilisk-knight").getSlayerXp().doubleValue(), 0.0);
        assertEquals(5590.0, variants.get("basilisk-sentinel").getSlayerXp().doubleValue(), 0.0);
        assertEquals(600.0, variants.get("the-jormungand").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("basilisk-knight").getNotes().stream()
            .anyMatch(note -> note.contains("The Fremennik Exiles")));
        assertTrue(variants.get("basilisk-sentinel").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));
        assertTrue(variants.get("the-jormungand").getNotes().stream()
            .anyMatch(note -> note.contains("can only be fought once")));

        assertEquals(2, locations.size());
        assertEquals(Integer.valueOf(8), locations.get("fremennik-slayer-dungeon").getAmount());
        assertEquals(Boolean.FALSE, locations.get("fremennik-slayer-dungeon").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("fremennik-slayer-dungeon").getCannonable());
        assertEquals(Boolean.TRUE, locations.get("fremennik-slayer-dungeon").getSafespottable());
        assertTrue(locations.get("fremennik-slayer-dungeon").getNotes().stream()
            .anyMatch(note -> note.contains("8 Basilisks")));

        assertEquals(null, locations.get("jormungand-s-prison").getAmount());
        assertEquals(Boolean.FALSE, locations.get("jormungand-s-prison").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("jormungand-s-prison").getCannonable());
        assertEquals(Boolean.TRUE, locations.get("jormungand-s-prison").getSafespottable());
        assertTrue(locations.get("jormungand-s-prison").getNotes().stream()
            .anyMatch(note -> note.contains("12 Basilisks") && note.contains("20 Knights")));
        assertTrue(locations.get("jormungand-s-prison").getNotes().stream()
            .anyMatch(note -> note.contains("Basilisk knights will still retaliate with magic")));
    }

    @Test
    public void strategySourceContainsAllTaskPageStrategyOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/basilisks/strategy.json");

        assertTrue("Basilisks strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("basilisks", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Basilisks", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("inquisitor-s-mace"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "hunters-sunlight-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "ordinary-basilisks");
        assertContains(methodIds(strategy), "basilisk-knights");
        assertContains(methodIds(strategy), "ranged-safespot");
        assertContains(methodIds(strategy), "basilisk-sentinel");
        assertContains(methodIds(strategy), "skip-or-toggle");
        assertContains(methodIds(strategy), "jormungand-quest");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "basilisk-knights".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing knight method"))
            .getSteps().stream().anyMatch(step -> step.contains("Protect from Magic")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "basilisk-sentinel".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing sentinel method"))
            .getNotes().stream().anyMatch(note -> note.contains("click rapidly")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "skip-or-toggle".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing skip method"))
            .getNotes().stream().anyMatch(note -> note.contains("toggle off")));

        assertContains(styleIds(strategy), "melee");
        assertContains(styleIds(strategy), "ranged-safespot");
    }

    @Test
    public void generatedRuntimeDataCarriesBasiliskSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Basilisks".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Basilisks"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Fremennik Slayer Dungeon".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Jormungand's Prison".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));

        MonsterVariant knight = task.getVariants().stream()
            .filter(variant -> "Basilisk Knight".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Basilisk Knight"));

        assertNotNull(knight.getStrategy());
        assertEquals(CombatStyle.MELEE, knight.getStrategy().getPrimaryStyle());
        assertEquals("Inquisitor's mace", knight.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(knight.getStrategy().getNote().contains("mirror shield"));
        assertTrue(task.getRequiredItemName().contains("Mirror shield"));
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        SourceLocation fremennik = read(Paths.get("src/main/data/slayer/locations/fremennik-slayer-dungeon.json"),
            SourceLocation.class);
        SourceLocation prison = read(Paths.get("src/main/data/slayer/locations/jormungand-s-prison.json"),
            SourceLocation.class);

        assertEquals("fremennik-slayer-dungeon", fremennik.getLocationId());
        assertFalse(fremennik.isMulti());
        assertFalse(fremennik.isCannon());
        assertTrue(fremennik.isSafeSpot());
        assertFalse(fremennik.isWilderness());
        assertTrue(fremennik.getAccessNote().contains("Basilisk"));

        assertEquals("jormungand-s-prison", prison.getLocationId());
        assertFalse(prison.isMulti());
        assertFalse(prison.isCannon());
        assertTrue(prison.isSafeSpot());
        assertFalse(prison.isWilderness());
        assertTrue(prison.getAccessNote().contains("The Fremennik Exiles"));
        assertTrue(prison.getAccessNote().contains("Basilisk Knight"));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> unlockIds(SourceTask task)
    {
        return task.getUnlocks().stream().map(SourceTaskUnlock::getUnlockId).collect(Collectors.toSet());
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

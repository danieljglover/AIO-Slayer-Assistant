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

public class DarkBeastsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dark-beasts.json"), SourceTask.class);

        assertEquals(Integer.valueOf(72120), task.getWikiPageId());
        assertEquals(Integer.valueOf(90), task.getCombatLevel());
        assertEquals(214, task.getSlayerTargetId());
        assertEquals(90, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Mourning's End Part II"));
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());

        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {10, 15}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {110, 135}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {110, 135}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {110, 135}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "need-more-darkness");
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("permanently aggressive")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Magic") && note.contains("first attack")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("herb sack") && note.contains("gem bag")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dark bow") && note.contains("death talisman")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("crystal shard")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dark-beasts.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertEquals(225.4, variants.get("dark-beast").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("dark-beast").getNotes().stream()
            .anyMatch(note -> note.contains("60%") && note.contains("earth")));
        assertEquals(6462.0, variants.get("night-beast").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("night-beast").getNotes().stream()
            .anyMatch(note -> note.contains("3x3") && note.contains("current Hitpoints")));
        assertTrue(variants.get("night-beast").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));

        assertEquals(2, locations.size());
        assertLocation(locations, "iorwerth-dungeon-dark-beasts", 10, false, true, false, "crystal shard");
        assertLocation(locations, "mourner-tunnels", 19, false, true, false, "Slayer ring");
    }

    @Test
    public void strategySourceContainsAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/dark-beasts/strategy.json");

        assertTrue("Dark beasts strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("dark-beasts", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dark_beast", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("scythe-of-vitur"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("soulreaper-axe"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("zamorakian-hasta"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "saradomin-godsword".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getNote().contains("Protect from Melee"));
        assertTrue(strategy.getPlugin().getNote().contains("Night beast"));

        assertContains(methodIds(strategy), "melee-general");
        assertContains(methodIds(strategy), "melee-prayer-bonus");
        assertContains(methodIds(strategy), "iorwerth-dungeon");
        assertContains(methodIds(strategy), "mourner-tunnels");
        assertContains(methodIds(strategy), "night-beast");
        assertContains(methodIds(strategy), "inventory");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "melee-prayer-bonus".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing prayer bonus method"))
            .getRequiredOrKeyItems().stream().anyMatch(item -> item.contains("proselyte")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "night-beast".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing night beast method"))
            .getSteps().stream().anyMatch(step -> step.contains("melee range")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "inventory".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing inventory method"))
            .getInventory().stream().anyMatch(item -> item.contains("12-18 prayer")));

        assertContains(styleIds(strategy), "melee-dps");
        assertContains(styleIds(strategy), "melee-prayer-bonus");
    }

    @Test
    public void generatedRuntimeDataCarriesDarkBeastStrategiesAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Dark beasts".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Dark beasts"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Iorwerth Dungeon".equals(location.getName())
            && !location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Mourner Tunnels".equals(location.getName())
            && !location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().noneMatch(location -> "Catacombs of Kourend".equals(location.getName())));

        MonsterVariant darkBeast = variantNamed(task, "Dark beast");
        MonsterVariant nightBeast = variantNamed(task, "Night beast");

        assertNotNull(darkBeast.getStrategy());
        assertEquals(CombatStyle.MELEE, darkBeast.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dark_beast",
            darkBeast.getStrategy().getSourceUrl());
        assertNotNull(nightBeast.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dark_beast",
            nightBeast.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchDarkBeastTaskPage() throws IOException
    {
        assertLocationSource("iorwerth-dungeon-dark-beasts", "Iorwerth Dungeon", false, true, false, false,
            "crystal shards");
        assertLocationSource("mourner-tunnels", "Mourner Tunnels", false, true, false, false,
            "Slayer ring");
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

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        int amount, boolean multi, boolean cannon, boolean safespot, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertEquals(Integer.valueOf(amount), location.getAmount());
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

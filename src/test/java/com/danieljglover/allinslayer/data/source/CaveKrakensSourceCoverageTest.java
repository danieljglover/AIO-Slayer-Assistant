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

public class CaveKrakensSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cave-kraken.json"), SourceTask.class);

        assertEquals(Integer.valueOf(357207), task.getWikiPageId());
        assertEquals(Integer.valueOf(80), task.getCombatLevel());
        assertEquals(212, task.getSlayerTargetId());
        assertEquals(87, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {80, 100}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {100, 120}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {100, 120}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {150, 200}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "krack-on");
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("50 Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("exclusively") && note.contains("Kraken Cove")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ranged") && note.contains("1/7")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Melee") && note.contains("cannot")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Earth spells")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("uncharged trident") && note.contains("1/200")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Kraken boss") && note.contains("generous drop table")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Fishing explosives")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("typeless") && note.contains("cannot be prayed")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/cave-kraken.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(2, variants.size());
        assertEquals(125.0, variants.get("cave-kraken").getSlayerXp().doubleValue(), 0.0);
        assertEquals(255.0, variants.get("kraken").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("cave-kraken").getNotes().stream()
            .anyMatch(note -> note.contains("Regular") && note.contains("whirlpool")));
        assertTrue(variants.get("kraken").getNotes().stream()
            .anyMatch(note -> note.contains("Boss variant")));

        assertEquals(1, locations.size());
        SourceTaskLocationComparison cove = locations.get("kraken-cove");
        assertEquals(null, cove.getAmount());
        assertEquals(Boolean.FALSE, cove.getMulticombat());
        assertEquals(Boolean.FALSE, cove.getCannonable());
        assertEquals(Boolean.FALSE, cove.getSafespottable());
        assertTrue(cove.getNotes().stream().anyMatch(note -> note.contains("regular cave kraken whirlpools")));
        assertTrue(cove.getNotes().stream().anyMatch(note -> note.contains("Kraken boss whirlpool")));
    }

    @Test
    public void taskStrategySourceContainsRegularAndBossTaskOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/cave-kraken/strategy.json");

        assertTrue("Cave krakens strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("cave-kraken", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Cave_krakens", strategy.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("eye-of-ayak"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("trident-of-the-seas"));
        assertTrue(strategy.getPlugin().getNote().contains("earth spells"));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "regular-cave-kraken");
        assertContains(methodIds(strategy), "kraken-boss-alternative");
        assertContains(methodIds(strategy), "konar-and-krack-on");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "regular-cave-kraken".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing regular method"))
            .getSteps().stream().anyMatch(step -> step.contains("whirlpool")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "kraken-boss-alternative".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing boss alternative method"))
            .getNotes().stream().anyMatch(note -> note.contains("Kraken/Strategies")));

        assertContains(styleIds(strategy), "magic-earth-spells");
        assertContains(styleIds(strategy), "powered-staff");
    }

    @Test
    public void krakenBossStrategySourceContainsAllStrategyPageOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/kraken/strategy.json");

        assertTrue("Kraken strategy JSON missing", Files.exists(json));
        assertFalse("legacy Kraken markdown should be migrated",
            Files.exists(Paths.get("src/main/data/slayer/strategies/kraken.md")));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("kraken", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Kraken/Strategies", strategy.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("eye-of-ayak"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("trident-of-the-swamp"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "release-the-kraken");
        assertContains(methodIds(strategy), "attack-styles");
        assertContains(methodIds(strategy), "private-instance-long-trips");
        assertContains(methodIds(strategy), "inventory-and-sustain");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "release-the-kraken".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing release method"))
            .getSteps().stream().anyMatch(step -> step.contains("fishing explosive")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "attack-styles".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing attack method"))
            .getRisks().stream().anyMatch(risk -> risk.contains("Protection prayers")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "private-instance-long-trips".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing instance method"))
            .getNotes().stream().anyMatch(note -> note.contains("3 hours")));

        assertContains(styleIds(strategy), "magic-boss");
        assertContains(styleIds(strategy), "earth-spells");
        assertContains(styleIds(strategy), "sustain-specials");
    }

    @Test
    public void generatedRuntimeDataCarriesCaveKrakenAndKrakenStrategies() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Cave kraken".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Cave kraken"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Kraken Cove".equals(location.getName())
            && !location.isCannon() && !location.isMulti()));

        MonsterVariant caveKraken = variantNamed(task, "Cave kraken");
        MonsterVariant kraken = variantNamed(task, "Kraken");

        assertNotNull(caveKraken.getStrategy());
        assertEquals(CombatStyle.MAGIC, caveKraken.getStrategy().getPrimaryStyle());
        assertEquals("Eye of ayak", caveKraken.getStrategy().getPrimaryWeapons().get(0).getName());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Cave_krakens",
            caveKraken.getStrategy().getSourceUrl());

        assertNotNull(kraken.getStrategy());
        assertEquals(CombatStyle.MAGIC, kraken.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Kraken/Strategies", kraken.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourceMatchesTaskAndVariantPages() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/kraken-cove.json"),
            SourceLocation.class);

        assertEquals("kraken-cove", location.getLocationId());
        assertEquals("Kraken Cove", location.getName());
        assertFalse(location.isMulti());
        assertFalse(location.isCannon());
        assertFalse(location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertTrue(location.getAccessNote().contains("87 Slayer"));
        assertTrue(location.getAccessNote().contains("50 Magic"));
        assertTrue(location.getAccessNote().contains("Fairy ring AKQ"));
        assertTrue(location.getAccessNote().contains("Fishing explosives"));
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

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

public class VampyresSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsUnlocksAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/vampyres.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298140), task.getWikiPageId());
        assertEquals(Integer.valueOf(35), task.getCombatLevel());
        assertEquals(237, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertContains(questReqs(task), "Priest in Peril");
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());

        assertContains(masterIds(task), "mazchna");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("mazchna"));
        assertArrayEquals(new int[] {10, 20}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {80, 100}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {100, 160}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {110, 170}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {100, 210}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));
        assertContains(unlockIds(task), "actual-vampyre-slayer");
        assertContains(unlockIds(task), "more-at-stake");

        assertContains(locationIds(task), "burgh-de-rott-vampyres");
        assertContains(locationIds(task), "darkmeyer");
        assertContains(locationIds(task), "god-wars-dungeon-vampyres");
        assertContains(locationIds(task), "haunted-woods");
        assertContains(locationIds(task), "meiyerditch");
        assertContains(locationIds(task), "slepe");
        assertContains(locationIds(task), "wilderness-god-wars-dungeon-vampyres");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Actual Vampyre Slayer")
            && note.contains("80")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("More at stake")
            && note.contains("200-250")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Feral Vampyre")
            && note.contains("any weapon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Guthix balance")
            && note.contains("25%")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Vyrewatch")
            && note.contains("Ivandis flail")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Vyrewatch Sentinels")
            && note.contains("blood shard")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Count Draynor")
            && note.contains("Slayer experience")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/vampyres.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, variants.size());
        assertVariant(variants, "feral-vampyre", null, null, "any weaponry");
        assertVariant(variants, "vampyre-juvenile", 45, 60, "does not give Slayer experience");
        assertVariant(variants, "vampyre-juvinate", null, null, "level 54 variants");
        assertVariant(variants, "vyrewatch", null, null, "A Taste of Hope");
        assertVariant(variants, "vyrewatch-sentinel", 151, 150, "blood shard");

        assertEquals(7, locations.size());
        assertLocation(locations, "burgh-de-rott-vampyres", null, false, true, true, "In Aid of the Myreque");
        assertLocation(locations, "darkmeyer", null, false, false, false, "Hallowed Sepulchre");
        assertLocation(locations, "god-wars-dungeon-vampyres", 13, true, true, true, "Zamorak item");
        assertLocation(locations, "haunted-woods", 19, false, true, true, "fairy ring");
        assertLocation(locations, "meiyerditch", null, false, false, false, "Darkness of Hallowvale");
        assertLocation(locations, "slepe", 10, false, false, true, "Crombwick Manor");
        assertLocation(locations, "wilderness-god-wars-dungeon-vampyres", 2, true, true, true,
            "emergency teleport");
    }

    @Test
    public void variantSourcesUseCurrentMonsterPageStatsAndStrategyIds() throws IOException
    {
        SourceMonsterVariant feral = readVariant("feral-vampyre.json");
        SourceMonsterVariant juvenile = readVariant("vampyre-juvenile-lvl45.json");
        SourceMonsterVariant juvinate = readVariant("vampyre-juvinate.json");
        SourceMonsterVariant vyrewatch = readVariant("vyrewatch.json");
        SourceMonsterVariant sentinel = readVariant("vyrewatch-sentinel-lvl151.json");

        assertDefence(feral, 55, 0, 0, 0, 0, 0);
        assertEquals("vampyres", feral.getStrategyId());
        assertDefence(juvenile, 30, 0, 0, 0, 0, 0);
        assertEquals("vampyres", juvenile.getStrategyId());
        assertDefence(juvinate, 30, 0, 0, 0, 0, 0);
        assertEquals("vampyres", juvinate.getStrategyId());
        assertDefence(vyrewatch, 85, 0, 0, 0, 0, 0);
        assertEquals("vampyres", vyrewatch.getStrategyId());
        assertDefence(sentinel, 180, 0, 0, 0, 0, 0);
        assertEquals("vyrewatch-sentinel", sentinel.getStrategyId());
    }

    @Test
    public void strategyJsonCoversTaskAndMigratedSentinelStrategy() throws IOException
    {
        Path taskJson = Paths.get("src/main/data/slayer/strategies/vampyres/strategy.json");
        Path sentinelJson = Paths.get("src/main/data/slayer/strategies/vyrewatch-sentinel/strategy.json");

        assertTrue("Vampyres strategy JSON missing", Files.exists(taskJson));
        assertTrue("Vyrewatch Sentinel strategy JSON missing", Files.exists(sentinelJson));
        assertFalse("Legacy Vyrewatch Sentinel Markdown should be migrated",
            Files.exists(Paths.get("src/main/data/slayer/strategies/vyrewatch-sentinel.md")));

        SourceStrategy task = read(taskJson, SourceStrategy.class);
        SourceStrategy sentinel = read(sentinelJson, SourceStrategy.class);

        assertEquals("vampyres", task.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Vampyres", task.getSourceUrl());
        assertEquals(CombatStyle.MELEE, task.getPlugin().getPrimaryStyle());
        assertTrue(task.getPlugin().getPrimaryWeapons().contains("blisterwood-flail"));
        assertContains(methodIds(task), "requirements-and-unlocks");
        assertContains(methodIds(task), "feral-vampyre-quick-task");
        assertContains(methodIds(task), "juvenile-juvinate-guthix-balance");
        assertContains(methodIds(task), "slepe-vyrewatch-prayer");
        assertContains(methodIds(task), "sentinel-upgrade-path");
        assertContains(styleIds(task), "feral-any-weapon");
        assertContains(styleIds(task), "flail-vyrewatch-prayer");

        assertEquals("vyrewatch-sentinel", sentinel.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Vyrewatch_Sentinel/Strategies",
            sentinel.getSourceUrl());
        assertEquals(CombatStyle.MELEE, sentinel.getPlugin().getPrimaryStyle());
        assertTrue(sentinel.getPlugin().getPrimaryWeapons().contains("blisterwood-flail"));
        assertContains(methodIds(sentinel), "darkmeyer-transport");
        assertContains(methodIds(sentinel), "prayer-afk-bank-altar");
        assertTrue(sentinel.getMethods().stream().anyMatch(method -> method.getSummary().contains("Efaritay")));
    }

    @Test
    public void generatedRuntimeDataCarriesVampyreStrategiesAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Vampyres".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Vampyres"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(5, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Haunted Woods".equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Darkmeyer".equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Slepe".equals(location.getName())));

        MonsterVariant vyrewatch = variantNamed(task, "Vyrewatch");
        MonsterVariant sentinel = variantNamed(task, "Vyrewatch Sentinel");

        assertNotNull(vyrewatch.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Vampyres",
            vyrewatch.getStrategy().getSourceUrl());
        assertNotNull(sentinel.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Vyrewatch_Sentinel/Strategies",
            sentinel.getStrategy().getSourceUrl());
    }

    private static SourceMonsterVariant readVariant(String fileName) throws IOException
    {
        return read(Paths.get("src/main/data/slayer/monsters/vampyres/" + fileName), SourceMonsterVariant.class);
    }

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        Integer combatLevel, Integer slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        if (slayerXp == null)
        {
            assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains("Combat levels")
                || note.contains("Slayer XP values")));
        }
        else
        {
            assertEquals(slayerXp.doubleValue(), variant.getSlayerXp().doubleValue(), 0.0);
        }
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertDefence(SourceMonsterVariant variant, int defenceLevel, int stab, int slash,
        int crush, int magic, int range)
    {
        assertEquals(defenceLevel, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(stab, variant.getMonsterDefence().getStab());
        assertEquals(slash, variant.getMonsterDefence().getSlash());
        assertEquals(crush, variant.getMonsterDefence().getCrush());
        assertEquals(magic, variant.getMonsterDefence().getMagic());
        assertEquals(range, variant.getMonsterDefence().getRange());
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        Integer amount, boolean multicombat, boolean cannonable, boolean safespottable, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location " + locationId, location);
        assertEquals(amount, location.getAmount());
        assertEquals(Boolean.valueOf(multicombat), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannonable), location.getCannonable());
        assertEquals(Boolean.valueOf(safespottable), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static MonsterVariant variantNamed(TaskData task, String name)
    {
        return task.getVariants().stream()
            .filter(variant -> name.equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing variant: " + name));
    }

    private static Set<String> questReqs(SourceTask task)
    {
        return task.getQuestReqs().stream().collect(Collectors.toSet());
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
        return strategy.getStyleOptions().stream().map(SourceStrategyStyleOption::getStyleId).collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }

    private static <T> void assertContains(Set<T> set, T value)
    {
        assertTrue("expected " + set + " to contain " + value, set.contains(value));
    }
}

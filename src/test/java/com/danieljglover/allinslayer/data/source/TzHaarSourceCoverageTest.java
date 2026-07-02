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

public class TzHaarSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/tzhaar.json"), SourceTask.class);

        assertEquals(Integer.valueOf(514851), task.getWikiPageId());
        assertEquals(236, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("water", task.getWeakness().getElement());

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertFalse(task.getMasterIds().contains("konar"));
        assertArrayEquals(new int[] {90, 150}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {110, 180}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 199}, task.getAmountByMaster().get("duradel"));
        assertContains(unlockIds(task), "hot-stuff");

        assertContains(locationIds(task), "mor-ul-rek-tzhaar-city");
        assertContains(locationIds(task), "mor-ul-rek-inner-tzhaar-ket");
        assertContains(locationIds(task), "tzhaar-fight-cave");
        assertContains(locationIds(task), "inferno");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Hot Stuff")
            && note.contains("100 slayer points")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ring of wealth")
            && note.contains("gems")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Hur")
            && note.contains("nearby TzHaar")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ket")
            && note.contains("vents")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("fire cape")
            && note.contains("inner area")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("TzTok-Jad")
            && note.contains("TzKal-Zuk")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("TzHaar-Ket-Rak")
            && note.contains("burst")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/tzhaar.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(6, variants.size());
        assertVariant(variants, "tzhaar-hur", 74, 80, "nearby Mej, Xil, and Ket");
        assertVariant(variants, "tzhaar-mej", 103, 100, "nearby Xil and Ket");
        assertVariant(variants, "tzhaar-xil", 133, 120, "melee/range variant");
        assertVariant(variants, "tzhaar-ket", null, null, "fire cape");
        assertVariant(variants, "tztok-jad", 702, 25250, "37,010 total");
        assertVariant(variants, "tzkal-zuk", 1400, 101890, "125,000");

        assertEquals(4, locations.size());
        assertLocation(locations, "mor-ul-rek-tzhaar-city", null, false, false, true, "vents");
        assertLocation(locations, "mor-ul-rek-inner-tzhaar-ket", null, false, false, true,
            "TzHaar-Ket-Rak");
        assertLocation(locations, "tzhaar-fight-cave", null, false, false, false, "TzTok-Jad");
        assertLocation(locations, "inferno", null, false, false, false, "TzKal-Zuk");
    }

    @Test
    public void variantSourcesUseCurrentMonsterPageStatsAndStrategyIds() throws IOException
    {
        SourceMonsterVariant hur = readVariant("tzhaar-hur-lvl74.json");
        SourceMonsterVariant mej = readVariant("tzhaar-mej-lvl103.json");
        SourceMonsterVariant xil = readVariant("tzhaar-xil-lvl133.json");
        SourceMonsterVariant ket221 = readVariant("tzhaar-ket-lvl221.json");
        SourceMonsterVariant jad = readVariant("tztok-jad-lvl702.json");
        SourceMonsterVariant zuk = readVariant("tzkal-zuk-lvl1400.json");

        assertEquals("water", hur.getWeakness().getElement());
        assertDefence(hur, 60, 0, 0, 0, 0, 0);
        assertEquals("tzhaar", hur.getStrategyId());
        assertDefence(mej, 80, 0, 0, 0, 0, 0);
        assertEquals("tzhaar", mej.getStrategyId());
        assertDefence(xil, 100, 0, 0, 0, 0, 0);
        assertEquals("tzhaar", xil.getStrategyId());
        assertDefence(ket221, 190, 0, 0, 0, 0, 0);
        assertEquals("tzhaar", ket221.getStrategyId());
        assertEquals("tztok-jad", jad.getStrategyId());
        assertEquals("tzkal-zuk", zuk.getStrategyId());
        assertDefence(zuk, 260, 0, 0, 0, 350, 100);
    }

    @Test
    public void strategyJsonCoversTaskMethodsAndMigratedBossStrategyJson() throws IOException
    {
        Path cityJson = Paths.get("src/main/data/slayer/strategies/tzhaar/strategy.json");
        Path jadJson = Paths.get("src/main/data/slayer/strategies/tztok-jad/strategy.json");
        Path zukJson = Paths.get("src/main/data/slayer/strategies/tzkal-zuk/strategy.json");

        assertTrue("TzHaar strategy JSON missing", Files.exists(cityJson));
        assertTrue("TzTok-Jad strategy JSON missing", Files.exists(jadJson));
        assertTrue("TzKal-Zuk strategy JSON missing", Files.exists(zukJson));
        assertFalse("Legacy TzTok-Jad Markdown should be migrated",
            Files.exists(Paths.get("src/main/data/slayer/strategies/tztok-jad.md")));
        assertFalse("Legacy TzKal-Zuk Markdown should be migrated",
            Files.exists(Paths.get("src/main/data/slayer/strategies/tzkal-zuk.md")));

        SourceStrategy city = read(cityJson, SourceStrategy.class);
        SourceStrategy jad = read(jadJson, SourceStrategy.class);
        SourceStrategy zuk = read(zukJson, SourceStrategy.class);

        assertEquals("tzhaar", city.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/TzHaar", city.getSourceUrl());
        assertEquals(CombatStyle.MELEE, city.getPlugin().getPrimaryStyle());
        assertTrue(city.getPlugin().getPrimaryWeapons().contains("scythe-of-vitur"));
        assertTrue(city.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "kodai-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertContains(methodIds(city), "requirements-and-unlock");
        assertContains(methodIds(city), "ket-and-melee-xil-safespots");
        assertContains(methodIds(city), "inner-ket-burst-barrage");
        assertContains(methodIds(city), "hur-mej-aggro-management");
        assertContains(methodIds(city), "jad-and-zuk-task-alternatives");
        assertContains(styleIds(city), "melee-city-safespot");
        assertContains(styleIds(city), "magic-ket-burst-barrage");

        assertEquals("tztok-jad", jad.getStrategyId());
        assertEquals(CombatStyle.RANGED, jad.getPlugin().getPrimaryStyle());
        assertTrue(jad.getMethods().stream().anyMatch(method -> method.getMethodId().contains("prayer")));

        assertEquals("tzkal-zuk", zuk.getStrategyId());
        assertEquals(CombatStyle.RANGED, zuk.getPlugin().getPrimaryStyle());
        assertTrue(zuk.getMethods().stream().anyMatch(method -> method.getMethodId().contains("shield")));
    }

    @Test
    public void generatedRuntimeDataCarriesTzHaarStrategiesAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "TzHaar".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing TzHaar"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertFalse(task.getAssignedBy().contains("konar"));
        assertEquals(7, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Mor Ul Rek (TzHaar City)"
            .equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "TzHaar Fight Cave"
            .equals(location.getName())));

        MonsterVariant ket = variantNamed(task, "TzHaar-Ket (Level 221)");
        MonsterVariant jad = variantNamed(task, "TzTok-Jad");
        MonsterVariant zuk = variantNamed(task, "TzKal-Zuk");

        assertNotNull(ket.getStrategy());
        assertEquals(CombatStyle.MELEE, ket.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/TzHaar", ket.getStrategy().getSourceUrl());
        assertNotNull(jad.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/TzHaar_Fight_Cave/Strategies",
            jad.getStrategy().getSourceUrl());
        assertNotNull(zuk.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Inferno/Strategies", zuk.getStrategy().getSourceUrl());
    }

    private static SourceMonsterVariant readVariant(String fileName) throws IOException
    {
        return read(Paths.get("src/main/data/slayer/monsters/tzhaar/" + fileName), SourceMonsterVariant.class);
    }

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        Integer combatLevel, Integer slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        if (slayerXp == null)
        {
            assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains("149")
                || note.contains("221")));
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

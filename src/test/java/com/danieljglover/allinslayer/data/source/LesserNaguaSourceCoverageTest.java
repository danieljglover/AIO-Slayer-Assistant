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
 * WC-6 (plan.md Wave C1, ADR-0019): pins the Lesser Nagua family (Perilous Moons content) to its
 * wiki source, fetched live 2026-07-02 from https://oldschool.runescape.wiki/w/Slayer_task/Lesser_Nagua,
 * the Sulphur/Frost/Earthen Nagua and Amoxliatl monster pages, and the Chaeldar/Konar task tables
 * (weights re-verified per PD-D: Chaeldar 50-100 w4, Konar 55-120 w2, no extension).
 */
public class LesserNaguaSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lesser-nagua.json"), SourceTask.class);

        assertEquals(Integer.valueOf(540924), task.getWikiPageId());
        assertEquals(Integer.valueOf(70), task.getCombatLevel());
        assertEquals(246, task.getSlayerTargetId());
        assertEquals(48, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Perilous Moons"));

        assertEquals(Set.of("chaeldar", "konar"), masterIds(task));
        assertArrayEquals(new int[] {50, 100}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {55, 120}, task.getAmountByMaster().get("konar"));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(2), task.getWeightByMaster().get("konar"));
        assertNull("wiki: no extension unlock exists for Lesser Nagua", task.getExtendedAmount());
        assertTrue(task.getUnlocks().isEmpty());

        assertNull(task.getRequiredItemId());
        assertEquals(Set.of("neypotzli-sulphur-nagua", "ruins-of-tapoyauik-frost-nagua", "crypt-of-tonali"),
            locationIds(task));

        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());
        assertEquals(40, task.getMonsterDefence().getDefenceLevel());
        assertEquals(200, task.getMonsterDefence().getRange());
        assertTrue(task.isSlayerHelmApplies());
        assertTrue(!task.isDragon() && !task.isDemon() && !task.isUndead() && !task.isKalphite());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dual Macuahuitl")
            || note.contains("multiple hitsplats")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.toLowerCase().contains("moonlight moth")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Amoxliatl")));
        assertTrue("synthetic target id must be flagged honestly",
            task.getTaskNotes().stream().anyMatch(note -> note.contains("246") && note.contains("synthetic")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lesser-nagua.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertEquals(105.0, variants.get("sulphur-nagua").getSlayerXp().doubleValue(), 0.0);
        assertEquals(120.0, variants.get("frost-nagua").getSlayerXp().doubleValue(), 0.0);
        assertEquals(165.0, variants.get("earthen-nagua").getSlayerXp().doubleValue(), 0.0);
        assertEquals(546.0, variants.get("lesser-nagua-amoxliatl").getSlayerXp().doubleValue(), 0.0);
        assertEquals(Integer.valueOf(98), variants.get("sulphur-nagua").getCombatLevel());
        assertEquals(Integer.valueOf(104), variants.get("frost-nagua").getCombatLevel());
        assertEquals(Integer.valueOf(128), variants.get("earthen-nagua").getCombatLevel());
        assertEquals(Integer.valueOf(263), variants.get("lesser-nagua-amoxliatl").getCombatLevel());
        assertTrue(variants.get("sulphur-nagua").getNotes().stream()
            .anyMatch(note -> note.contains("prayer")));
        assertTrue(variants.get("earthen-nagua").getNotes().stream()
            .anyMatch(note -> note.contains("The Final Dawn")));
        assertTrue(variants.get("earthen-nagua").getNotes().stream()
            .anyMatch(note -> note.contains("crush")));

        assertEquals(3, locations.size());
        SourceTaskLocationComparison neypotzli = locations.get("neypotzli-sulphur-nagua");
        assertEquals(Integer.valueOf(14), neypotzli.getAmount());
        assertEquals(Boolean.FALSE, neypotzli.getMulticombat());
        assertEquals(Boolean.FALSE, neypotzli.getCannonable());
        assertEquals(Boolean.TRUE, neypotzli.getSafespottable());
        SourceTaskLocationComparison tapoyauik = locations.get("ruins-of-tapoyauik-frost-nagua");
        assertEquals(Integer.valueOf(11), tapoyauik.getAmount());
        assertEquals(Boolean.FALSE, tapoyauik.getSafespottable());
        assertNotNull(locations.get("crypt-of-tonali"));
    }

    @Test
    public void monsterSourcesCarryWikiStats() throws IOException
    {
        SourceMonsterVariant sulphur = read(
            Paths.get("src/main/data/slayer/monsters/lesser-nagua/sulphur-nagua-lvl98.json"),
            SourceMonsterVariant.class);
        assertTrue(sulphur.getNpcIds().contains(13033));
        assertEquals(CombatStyle.MELEE, sulphur.getWeakness().getStyle());
        assertEquals("air", sulphur.getWeakness().getElement());
        assertEquals(40, sulphur.getMonsterDefence().getDefenceLevel());
        assertEquals(10, sulphur.getMonsterDefence().getCrush());
        assertEquals(200, sulphur.getMonsterDefence().getRange());
        assertTrue(!sulphur.isBoss());
        assertEquals("lesser-nagua", sulphur.getStrategyId());

        SourceMonsterVariant frost = read(
            Paths.get("src/main/data/slayer/monsters/lesser-nagua/frost-nagua-lvl104.json"),
            SourceMonsterVariant.class);
        assertTrue(frost.getNpcIds().contains(13728));
        assertEquals("fire", frost.getWeakness().getElement());
        assertEquals(0, frost.getMonsterDefence().getStab());
        assertEquals(60, frost.getMonsterDefence().getMagic());

        SourceMonsterVariant earthen = read(
            Paths.get("src/main/data/slayer/monsters/lesser-nagua/earthen-nagua-lvl128.json"),
            SourceMonsterVariant.class);
        assertTrue(earthen.getNpcIds().contains(14420));
        assertEquals("air", earthen.getWeakness().getElement());
        assertTrue(earthen.getRequirement().contains("The Final Dawn"));

        SourceMonsterVariant amoxliatl = read(
            Paths.get("src/main/data/slayer/monsters/lesser-nagua/lesser-nagua-amoxliatl-lvl263.json"),
            SourceMonsterVariant.class);
        assertTrue(amoxliatl.getNpcIds().contains(13685));
        assertTrue(amoxliatl.isBoss());
        assertEquals("fire", amoxliatl.getWeakness().getElement());
        assertEquals(80, amoxliatl.getMonsterDefence().getDefenceLevel());
        assertEquals("lesser-nagua", amoxliatl.getStrategyId());
    }

    @Test
    public void strategySourceCoversTaskPageMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/lesser-nagua/strategy.json");
        assertTrue("Lesser Nagua strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("lesser-nagua", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Lesser_Nagua", strategy.getSourceUrl());
        assertTrue(strategy.getVariantIds().contains("sulphur-nagua"));
        assertTrue(strategy.getVariantIds().contains("frost-nagua"));
        assertTrue(strategy.getVariantIds().contains("earthen-nagua"));
        assertTrue(strategy.getVariantIds().contains("lesser-nagua-amoxliatl"));
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dual-macuahuitl"));
        assertTrue(strategy.getPlugin().getNote().contains("negative flat armour")
            || strategy.getPlugin().getNote().contains("multiple hitsplats"));

        assertContains(methodIds(strategy), "neypotzli-sulphur");
        assertContains(methodIds(strategy), "tapoyauik-frost");
        assertContains(methodIds(strategy), "crypt-earthen");
        assertContains(methodIds(strategy), "amoxliatl-alternative");
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("prayer")));
    }

    @Test
    public void generatedRuntimeDataCarriesLesserNaguaSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Lesser Nagua".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Lesser Nagua"));

        assertEquals(Set.of("chaeldar", "konar"), task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(2), task.getWeightByMaster().get("konar"));
        assertNull(task.getExtendedAmount());
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Neypotzli".equals(location.getName()) && location.isSafeSpot() && location.isKonarLockable()));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Crypt of Tonali".equals(location.getName()) && location.isMulti() && !location.isCannon()));

        assertEquals(4, task.getVariants().size());
        MonsterVariant sulphur = task.getVariants().stream()
            .filter(variant -> "Sulphur Nagua".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Sulphur Nagua variant"));
        assertTrue(sulphur.isDefault());
        assertNotNull(sulphur.getStrategy());
        assertEquals(CombatStyle.MELEE, sulphur.getStrategy().getPrimaryStyle());
        assertEquals("Dual macuahuitl", sulphur.getStrategy().getPrimaryWeapons().get(0).getName());

        MonsterVariant amoxliatl = task.getVariants().stream()
            .filter(variant -> "Amoxliatl".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Amoxliatl variant"));
        assertTrue(amoxliatl.isBoss());
        assertNotNull(amoxliatl.getStrategy());
        assertEquals(CombatStyle.MELEE, amoxliatl.getStrategy().getPrimaryStyle());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        SourceLocation neypotzli = read(
            Paths.get("src/main/data/slayer/locations/neypotzli-sulphur-nagua.json"), SourceLocation.class);
        assertEquals("Neypotzli", neypotzli.getName());
        assertEquals(false, neypotzli.isMulti());
        assertEquals(false, neypotzli.isCannon());
        assertEquals(true, neypotzli.isKonarLockable());
        assertEquals(true, neypotzli.isSafeSpot());
        assertEquals(false, neypotzli.isWilderness());
        assertTrue(neypotzli.getAccessNote().contains("Supplies"));

        SourceLocation tapoyauik = read(
            Paths.get("src/main/data/slayer/locations/ruins-of-tapoyauik-frost-nagua.json"), SourceLocation.class);
        assertEquals("Ruins of Tapoyauik", tapoyauik.getName());
        assertEquals(false, tapoyauik.isSafeSpot());
        assertEquals(true, tapoyauik.isKonarLockable());
        assertTrue(tapoyauik.getAccessNote().contains("moonlight moth"));

        SourceLocation crypt = read(
            Paths.get("src/main/data/slayer/locations/crypt-of-tonali.json"), SourceLocation.class);
        assertEquals("Crypt of Tonali", crypt.getName());
        assertEquals(true, crypt.isMulti());
        assertEquals(false, crypt.isCannon());
        assertEquals(false, crypt.isSafeSpot());
        assertEquals(true, crypt.isKonarLockable());
        assertTrue(crypt.getAccessNote().contains("The Final Dawn"));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream().map(SourceStrategyMethod::getMethodId).collect(Collectors.toSet());
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

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
 * WC-3 (ADR-0019 recipe): pins the wiki-authored Brine rats family sources. Masters, quantities,
 * and weights re-verified against the live Vannaka/Chaeldar/Konar/Nieve pages on 2026-07-02 (PD-D).
 * The WC-0 flag "wiki adds Turael/Spria" was re-verified and REFUTED: on the live Turael/Spria
 * pages Brine rats appear only as an ALTERNATIVE kill under the generic Rats task, not as a Brine
 * rats family row, so the family masters stay the four G3 masters. slayerTargetId is SYNTHETIC
 * (240 + WC index = 243) per the WC-1..11 allocation contract in team memory.
 */
public class BrineRatsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/brine-rats.json"), SourceTask.class);

        assertEquals(Integer.valueOf(11604), task.getWikiPageId());
        assertEquals(Integer.valueOf(45), task.getCombatLevel());
        assertEquals(243, task.getSlayerTargetId());
        assertEquals(47, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().stream().anyMatch(req -> req.contains("Olaf's Quest")));

        assertEquals(Set.of("vannaka", "chaeldar", "konar", "nieve"), masterIds(task));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));

        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("vannaka"));
        assertEquals(Integer.valueOf(7), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(2), task.getWeightByMaster().get("konar"));
        assertEquals(Integer.valueOf(3), task.getWeightByMaster().get("nieve"));

        assertNull("Brine rats have no task-extension unlock", task.getExtendedAmount());
        assertTrue("Brine rats have no superior or extension unlocks", task.getUnlocks().isEmpty());
        assertNull(task.getRequiredItemId());

        assertEquals(Set.of("brine-rat-cavern"), locationIds(task));

        assertTrue(task.isSlayerHelmApplies());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("spade")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Brine sabre")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Turael") && note.contains("Rats")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/brine-rats.json"), SourceTask.class);

        assertEquals(1, task.getVariantInfo().size());
        SourceTaskVariantInfo brineRat = task.getVariantInfo().get(0);
        assertEquals("brine-rat", brineRat.getVariantId());
        assertEquals(50.0, brineRat.getSlayerXp().doubleValue(), 0.0);

        assertEquals(1, task.getLocationComparison().size());
        SourceTaskLocationComparison cavern = task.getLocationComparison().get(0);
        assertEquals("brine-rat-cavern", cavern.getLocationId());
        assertEquals(Integer.valueOf(7), cavern.getAmount());
        assertEquals(Boolean.FALSE, cavern.getMulticombat());
        assertEquals(Boolean.TRUE, cavern.getCannonable());
        assertEquals(Boolean.TRUE, cavern.getSafespottable());
    }

    @Test
    public void monsterSourceCarriesWikiStats() throws IOException
    {
        SourceMonsterVariant brineRat = read(
            Paths.get("src/main/data/slayer/monsters/brine-rats/brine-rat-lvl70.json"), SourceMonsterVariant.class);
        assertEquals("brine-rat", brineRat.getVariantId());
        assertEquals(Set.of(4501), brineRat.getNpcIds().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(70), brineRat.getCombatLevel());
        assertEquals(CombatStyle.MELEE, brineRat.getWeakness().getStyle());
        assertNull("no elemental weakness", brineRat.getWeakness().getElement());
        assertEquals(40, brineRat.getMonsterDefence().getDefenceLevel());
    }

    @Test
    public void strategySourceCarriesRatBoneWeaponAndNewWeaponFileResolves() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/brine-rats/strategy.json");
        assertTrue("Brine rats strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);
        assertEquals("brine-rats", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Brine_rat", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("bone-mace"));
        assertTrue(strategy.getPlugin().getNote().contains("rat"));

        SourceWeapon boneMace = read(Paths.get("src/main/data/slayer/weapons/bone-mace.json"), SourceWeapon.class);
        assertEquals("bone-mace", boneMace.getWeaponId());
        assertEquals("Bone mace", boneMace.getName());
        assertEquals(Set.of(28792), boneMace.getItemIds().stream().collect(Collectors.toSet()));
    }

    @Test
    public void generatedRuntimeDataCarriesBrineRatsSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Brine rats".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Brine rats"));

        assertEquals(243, task.getSlayerTargetId());
        assertEquals(Set.of("vannaka", "chaeldar", "konar", "nieve"),
            task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(2), task.getWeightByMaster().get("konar"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Brine Rat Cavern".equals(location.getName()) && location.isCannon() && location.isSafeSpot()
                && !location.isMulti()));

        MonsterVariant brineRat = task.getVariants().stream()
            .filter(variant -> "Brine rat".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Brine rat variant"));
        assertNotNull(brineRat.getStrategy());
        assertEquals(CombatStyle.MELEE, brineRat.getStrategy().getPrimaryStyle());
        assertEquals("Bone mace", brineRat.getStrategy().getPrimaryWeapons().get(0).getName());
    }

    @Test
    public void ownedLocationSourceMatchesWiki() throws IOException
    {
        SourceLocation cavern = read(
            Paths.get("src/main/data/slayer/locations/brine-rat-cavern.json"), SourceLocation.class);

        assertEquals("brine-rat-cavern", cavern.getLocationId());
        assertEquals("Brine Rat Cavern", cavern.getName());
        assertEquals(false, cavern.isMulti());
        assertEquals(true, cavern.isCannon());
        assertEquals(false, cavern.isBurst());
        assertEquals(true, cavern.isKonarLockable());
        assertEquals(true, cavern.isSafeSpot());
        assertEquals(false, cavern.isWilderness());
        assertTrue(cavern.getAccessNote().contains("spade"));
    }

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

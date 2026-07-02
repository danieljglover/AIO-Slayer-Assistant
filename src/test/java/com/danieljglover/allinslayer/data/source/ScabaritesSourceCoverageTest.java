package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.loadout.VarbitSlayerUnlockStateProvider;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.UnlockType;
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

/**
 * WC-7 (ADR-0019): Scabarites family coverage. Every number below was re-verified against the
 * live wiki on 2026-07-02 (Slayer_task/Scabarites, the Nieve task table, and the Locust rider /
 * Scarab mage / Scarab swarm / Small scarab monster pages, PD-D).
 */
public class ScabaritesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/scabarites.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298250), task.getWikiPageId());
        assertEquals(Integer.valueOf(85), task.getCombatLevel());
        // Synthetic id: 240 + WC index 7 (team contract 2026-07-02, Frost-Dragons precedent) -
        // the real SLAYER_TARGET varp for Scabarites is not wiki-published; see taskNotes.
        assertEquals(247, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Contact! (partial completion)"));

        assertEquals(1, task.getMasterIds().size());
        assertTrue(task.getMasterIds().contains("nieve"));
        assertArrayEquals(new int[] {30, 60}, task.getAmountByMaster().get("nieve"));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 170}, task.getExtendedAmount().get("nieve"));

        SourceTaskUnlock extension = task.getUnlocks().stream()
            .filter(u -> u.getType() == UnlockType.EXTENSION)
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing EXTENSION unlock"));
        assertEquals("get-scabaright-on-it", extension.getUnlockId());
        assertEquals(Integer.valueOf(50), extension.getPointsCost());

        assertEquals("locust-rider", task.getDefaultVariantId());
        assertTrue("Scabarites count as kalphites for keris purposes", task.isKalphite());
        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isUndead());
        assertFalse(task.isDemon());
        assertNull(task.getRequiredItemId());

        assertContains(locationIds(task), "sophanem-dungeon-cavern");
        assertContains(locationIds(task), "sophanem-dungeon-maze");
        assertContains(locationIds(task), "uzer-mastaba");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Contact!")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("keris")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("floor trap")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Nightmare Zone")));
    }

    @Test
    public void scabaritesExtensionUnlockIsVarbitAnswerable()
    {
        // FR-RV S2 invariant: an EXTENSION unlock must be answerable or the WA-13 hint would
        // recommend it forever. SLAYER_LONGER_SCABARITES=5359 is spike-verified
        // (docs/full-review/spike-player-state.md Signal 2) and wired with this family.
        assertTrue(VarbitSlayerUnlockStateProvider.answersUnlock("get-scabaright-on-it"));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/scabarites.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));

        assertEquals(4, variants.size());
        assertEquals(92.5, variants.get("locust-rider").getSlayerXp().doubleValue(), 0.0);
        assertEquals(51.2, variants.get("scarab-mage").getSlayerXp().doubleValue(), 0.0);
        assertEquals(1.0, variants.get("scarab-swarm").getSlayerXp().doubleValue(), 0.0);
        assertEquals(40.0, variants.get("small-scarab").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("locust-rider").getNotes().stream()
            .anyMatch(note -> note.contains("keris")));
        assertTrue(variants.get("small-scarab").getNotes().stream()
            .anyMatch(note -> note.contains("The Curse of Arrav")));

        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));
        assertEquals(3, locations.size());
        SourceTaskLocationComparison cavern = locations.get("sophanem-dungeon-cavern");
        assertEquals(Boolean.TRUE, cavern.getMulticombat());
        assertEquals(Boolean.TRUE, cavern.getCannonable());
        assertTrue(cavern.getNotes().stream().anyMatch(note -> note.contains("27 Locust riders")));
        SourceTaskLocationComparison maze = locations.get("sophanem-dungeon-maze");
        assertEquals(Boolean.FALSE, maze.getCannonable());
        assertTrue(locations.get("uzer-mastaba").getNotes().stream()
            .anyMatch(note -> note.contains("19 Small scarabs")));
    }

    @Test
    public void monsterVariantsMatchWikiStats() throws IOException
    {
        SourceMonsterVariant rider = read(
            Paths.get("src/main/data/slayer/monsters/scabarites/locust-rider-lvl98.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(98), rider.getCombatLevel());
        assertEquals(90, rider.getMonsterDefence().getDefenceLevel());
        assertEquals(90, rider.getMonsterDefence().getSlash());
        assertEquals(50, rider.getMonsterDefence().getStab());
        assertTrue(rider.isKalphite());
        assertTrue(rider.getNpcIds().containsAll(java.util.Arrays.asList(795, 796)));
        assertEquals("scabarites", rider.getStrategyId());

        SourceMonsterVariant mage = read(
            Paths.get("src/main/data/slayer/monsters/scabarites/scarab-mage-lvl93.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(93), mage.getCombatLevel());
        assertEquals("fire", mage.getWeakness().getElement());
        assertTrue(mage.isKalphite());

        SourceMonsterVariant swarm = read(
            Paths.get("src/main/data/slayer/monsters/scabarites/scarab-swarm-lvl98.json"),
            SourceMonsterVariant.class);
        assertEquals(30, swarm.getMonsterDefence().getDefenceLevel());

        SourceMonsterVariant small = read(
            Paths.get("src/main/data/slayer/monsters/scabarites/small-scarab-lvl41.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(41), small.getCombatLevel());
        assertTrue(small.getNpcIds().contains(14126));
    }

    @Test
    public void locationSourcesMatchOwnershipContract() throws IOException
    {
        // WC-0 ownership doc: all three are unique to WC-7; wiki flags per the Slayer task page.
        assertLocationSource("sophanem-dungeon-cavern", "Sophanem Dungeon (cavern)", true, true, false, "Contact!");
        assertLocationSource("sophanem-dungeon-maze", "Sophanem Dungeon (maze)", true, false, false, "Contact!");
        assertLocationSource("uzer-mastaba", "Uzer Mastaba", false, false, false, "The Curse of Arrav");
    }

    @Test
    public void strategySourceCarriesPluginWeaponsAndMethods() throws IOException
    {
        SourceStrategy strategy = read(
            Paths.get("src/main/data/slayer/strategies/scabarites/strategy.json"),
            SourceStrategy.class);

        assertEquals("scabarites", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Scabarites", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("keris-partisan-of-breaching"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "cannon");
        assertContains(methodIds(strategy), "guthans-sustain");
        assertContains(methodIds(strategy), "maze-prayer-flick");
    }

    @Test
    public void generatedRuntimeDataCarriesScabarites() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Scabarites".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Scabarites"));

        assertTrue(task.getAssignedBy().contains("nieve"));
        assertEquals(Integer.valueOf(4), task.getWeightByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 170}, task.getExtendedAmount().get("nieve"));
        assertTrue(task.getUnlocks().stream()
            .anyMatch(u -> "get-scabaright-on-it".equals(u.getUnlockId())
                && u.getType() == UnlockType.EXTENSION));
        assertTrue(task.isKalphite());
        assertTrue(task.getLocations().stream()
            .anyMatch(location -> "Sophanem Dungeon (cavern)".equals(location.getName())
                && location.isMulti() && location.isCannon()));

        MonsterVariant rider = task.getVariants().stream()
            .filter(variant -> "Locust rider".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Locust rider variant"));
        assertNotNull(rider.getStrategy());
        assertEquals(CombatStyle.MELEE, rider.getStrategy().getPrimaryStyle());
        assertEquals(4, task.getVariants().size());
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safeSpot, String accessNote) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);
        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertEquals(safeSpot, location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertFalse(location.isKonarLockable());
        assertTrue(location.getAccessNote().contains(accessNote));
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("missing " + expected + " in " + values, values.contains(expected));
    }

    private static Set<String> locationIds(SourceTask task)
    {
        return task.getLocationIds().stream().collect(Collectors.toSet());
    }

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream().map(SourceStrategyMethod::getMethodId)
            .collect(Collectors.toSet());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

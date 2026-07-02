package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.loadout.VarbitSlayerUnlockStateProvider;
import com.danieljglover.allinslayer.model.AttackStyle;
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
 * C2-G (ADR-0019): Revenants family coverage. Every number below was verified against the live wiki
 * on 2026-07-02 (the Krystilia task table, the Revenants / Revenant Caves monster pages, and the
 * Slayer Rewards "Extend" table, PD-D). SYNTHETIC slayerTargetId 268 (252-305 C2/C3 block).
 */
public class RevenantsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/revenants.json"), SourceTask.class);

        assertNull(task.getCombatLevel());
        assertEquals(268, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());

        assertEquals(1, task.getMasterIds().size());
        assertTrue(task.getMasterIds().contains("krystilia"));
        assertArrayEquals(new int[] {40, 100}, task.getAmountByMaster().get("krystilia"));
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertArrayEquals(new int[] {100, 150}, task.getExtendedAmount().get("krystilia"));

        SourceTaskUnlock extension = task.getUnlocks().stream()
            .filter(u -> u.getType() == UnlockType.EXTENSION)
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing EXTENSION unlock"));
        assertEquals("revenenenenenants", extension.getUnlockId());
        assertEquals(Integer.valueOf(100), extension.getPointsCost());

        assertEquals("revenant-knight", task.getDefaultVariantId());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertNull(task.getWeakness().getElement());
        assertTrue(task.isUndead());
        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isDemon());
        assertFalse(task.isKalphite());
        assertNull(task.getRequiredItemId());

        assertContains(locationIds(task), "revenant-caves");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("SYNTHETIC")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("salve")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("singles-plus")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("heal")));
    }

    @Test
    public void revenantsExtensionUnlockIsVarbitAnswerable()
    {
        // FR-RV S2 invariant: an EXTENSION unlock must be answerable or the WA-13 hint would
        // recommend it forever. SLAYER_LONGER_REVENANTS=14822 is spike-verified
        // (docs/full-review/spike-player-state.md Signal 2) and wired with this family.
        assertTrue(VarbitSlayerUnlockStateProvider.answersUnlock("revenenenenenants"));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/revenants.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));

        assertEquals(11, variants.size());
        assertEquals(10.0, variants.get("revenant-imp").getSlayerXp().doubleValue(), 0.0);
        assertEquals(168.0, variants.get("revenant-knight").getSlayerXp().doubleValue(), 0.0);
        assertEquals(186.0, variants.get("revenant-dragon").getSlayerXp().doubleValue(), 0.0);

        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));
        assertEquals(1, locations.size());
        SourceTaskLocationComparison caves = locations.get("revenant-caves");
        assertEquals(Boolean.FALSE, caves.getMulticombat());
        assertEquals(Boolean.FALSE, caves.getCannonable());
        assertEquals(Boolean.FALSE, caves.getSafespottable());
        assertTrue(caves.getNotes().stream().anyMatch(note -> note.contains("singles-plus")));
    }

    @Test
    public void monsterVariantsMatchWikiStatsAndOffence() throws IOException
    {
        SourceMonsterVariant knight = read(
            Paths.get("src/main/data/slayer/monsters/revenants/revenant-knight-lvl126.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(126), knight.getCombatLevel());
        assertEquals(80, knight.getMonsterDefence().getDefenceLevel());
        assertTrue(knight.isUndead());
        assertEquals("revenants", knight.getStrategyId());
        assertEquals("revenant-caves", knight.getLocationId());
        assertEquals(CombatStyle.MELEE, knight.getWeakness().getStyle());
        assertNotNull(knight.getOffence());
        assertEquals(Integer.valueOf(143), knight.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(27), knight.getOffence().getMaxHit());
        assertTrue(knight.getOffence().getAttackStyles().contains(AttackStyle.MELEE));
        assertTrue(knight.getOffence().getAttackStyles().contains(AttackStyle.RANGED));
        assertTrue(knight.getOffence().getAttackStyles().contains(AttackStyle.MAGIC));

        SourceMonsterVariant imp = read(
            Paths.get("src/main/data/slayer/monsters/revenants/revenant-imp-lvl7.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(7), imp.getCombatLevel());
        assertEquals(Integer.valueOf(10), imp.getOffence().getHitpoints());

        SourceMonsterVariant dragon = read(
            Paths.get("src/main/data/slayer/monsters/revenants/revenant-dragon-lvl135.json"),
            SourceMonsterVariant.class);
        assertEquals(Integer.valueOf(135), dragon.getCombatLevel());
        assertEquals(Integer.valueOf(155), dragon.getOffence().getHitpoints());
        assertEquals(Integer.valueOf(30), dragon.getOffence().getMaxHit());
    }

    @Test
    public void locationSourceMatchesOwnershipContract() throws IOException
    {
        // C2-G ownership: Revenant Caves is unique to this family; wiki-wins over the CT-L doc's
        // "multi-way" claim - the live wiki says singles-plus, so multi=false and cannon=false.
        SourceLocation location = read(
            Paths.get("src/main/data/slayer/locations/revenant-caves.json"), SourceLocation.class);
        assertEquals("revenant-caves", location.getLocationId());
        assertEquals("Revenant Caves", location.getName());
        assertFalse(location.isMulti());
        assertFalse(location.isCannon());
        assertFalse(location.isSafeSpot());
        assertTrue(location.isWilderness());
        assertFalse(location.isKonarLockable());
        assertTrue(location.getAccessNote().contains("singles-plus"));
    }

    @Test
    public void strategySourceCarriesPluginWeaponsAndMethods() throws IOException
    {
        SourceStrategy strategy = read(
            Paths.get("src/main/data/slayer/strategies/revenants/strategy.json"),
            SourceStrategy.class);

        assertEquals("revenants", strategy.getStrategyId());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("viggora-s-chainmace"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "craw-s-bow".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "thammaron-s-sceptre".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        // GLOBAL GATE: >=2 wiki-honest methods.
        assertTrue("needs >=2 methods", strategy.getMethods().size() >= 2);
        assertContains(methodIds(strategy), "melee");
        assertContains(methodIds(strategy), "ranged");
        assertContains(methodIds(strategy), "magic");
    }

    @Test
    public void generatedRuntimeDataCarriesRevenants() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Revenants".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Revenants"));

        assertTrue(task.getAssignedBy().contains("krystilia"));
        assertEquals(Integer.valueOf(5), task.getWeightByMaster().get("krystilia"));
        assertArrayEquals(new int[] {100, 150}, task.getExtendedAmount().get("krystilia"));
        assertTrue(task.getUnlocks().stream()
            .anyMatch(u -> "revenenenenenants".equals(u.getUnlockId())
                && u.getType() == UnlockType.EXTENSION));
        assertTrue(task.isUndead());

        MonsterVariant knight = task.getVariants().stream()
            .filter(variant -> "Revenant knight".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Revenant knight variant"));
        assertNotNull(knight.getStrategy());
        assertEquals(CombatStyle.MELEE, knight.getStrategy().getPrimaryStyle());
        assertEquals(11, task.getVariants().size());
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

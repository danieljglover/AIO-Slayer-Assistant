package com.danieljglover.allinslayer.data.source;

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
 * WC-4 (plan.md Wave C1, ADR-0019): pins the Custodian stalker family to current
 * OSRS Wiki MediaWiki source fetched 2026-07-02.
 *
 * Evidence:
 * Slayer task/Custodian stalker page 601070 rev 15237764 (2026-06-23T06:03:38Z);
 * Custodian stalker page 561473 rev 15159870 (2026-03-28T08:21:20Z);
 * Juvenile/Mature/Elder pages 594262/594263/593963 revs 15200692/15200693/15212889;
 * Ancient Custodian page 594224 rev 15200690; Stalker Den page 593875 rev 15207813.
 * Custodian stalker/Strategies was fetched and verified missing.
 */
public class CustodianStalkerSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentExtensionAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/custodian-stalker.json"),
            SourceTask.class);

        assertEquals(Integer.valueOf(601070), task.getWikiPageId());
        assertNull(task.getCombatLevel());
        assertEquals(244, task.getSlayerTargetId());
        assertEquals(54, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Shadows of Custodia"));

        assertEquals(Set.of("chaeldar", "nieve"), masterIds(task));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {110, 170}, task.getAmountByMaster().get("nieve"));
        assertEquals(Integer.valueOf(11), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));

        assertEquals(Set.of("un-restraining-order", "bigger-and-badder"), unlockIds(task));
        SourceTaskUnlock extension = task.getUnlocks().stream()
            .filter(unlock -> "un-restraining-order".equals(unlock.getUnlockId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing extension unlock"));
        assertEquals(Integer.valueOf(100), extension.getPointsCost());
        assertEquals(UnlockType.EXTENSION, extension.getType());

        assertEquals(Set.of("stalker-den-custodian-stalkers"), locationIds(task));
        assertFalse("do not reuse the Ancient Zygomite tuned copy",
            task.getLocationIds().contains("stalker-den-zygomites"));

        assertEquals(CombatStyle.RANGED, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());
        assertEquals(45, task.getMonsterDefence().getDefenceLevel());
        assertEquals(-10, task.getMonsterDefence().getSlash());
        assertTrue(task.isSlayerHelmApplies());
        assertFalse(task.isDragon());
        assertFalse(task.isDemon());
        assertFalse(task.isUndead());
        assertFalse(task.isKalphite());

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Shadows of Custodia")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("south-west")
            && note.contains("cannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Elder")
            && note.contains("Ancient Custodian")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("244")
            && note.contains("synthetic")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/custodian-stalker.json"),
            SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertEquals(135.0, variants.get("juvenile-custodian-stalker").getSlayerXp().doubleValue(), 0.0);
        assertEquals(190.0, variants.get("mature-custodian-stalker").getSlayerXp().doubleValue(), 0.0);
        assertEquals(250.0, variants.get("elder-custodian-stalker").getSlayerXp().doubleValue(), 0.0);
        assertEquals(3382.0, variants.get("ancient-custodian").getSlayerXp().doubleValue(), 0.0);
        assertEquals(Integer.valueOf(93), variants.get("juvenile-custodian-stalker").getCombatLevel());
        assertEquals(Integer.valueOf(117), variants.get("mature-custodian-stalker").getCombatLevel());
        assertEquals(Integer.valueOf(142), variants.get("elder-custodian-stalker").getCombatLevel());
        assertEquals(Integer.valueOf(239), variants.get("ancient-custodian").getCombatLevel());
        assertTrue(variants.get("elder-custodian-stalker").getNotes().stream()
            .anyMatch(note -> note.contains("20%") && note.contains("bleed")));
        assertTrue(variants.get("ancient-custodian").getNotes().stream()
            .anyMatch(note -> note.contains("Bigger and Badder")));

        assertEquals(1, locations.size());
        SourceTaskLocationComparison den = locations.get("stalker-den-custodian-stalkers");
        assertNull("no task-page combined spawn count is published", den.getAmount());
        assertEquals(Boolean.TRUE, den.getMulticombat());
        assertEquals(Boolean.TRUE, den.getCannonable());
        assertEquals(Boolean.TRUE, den.getSafespottable());
        assertTrue(den.getNotes().stream().anyMatch(note -> note.contains("single-combat caverns")));
    }

    @Test
    public void monsterSourcesCarryWikiStats() throws IOException
    {
        SourceMonsterVariant juvenile = read(
            Paths.get("src/main/data/slayer/monsters/custodian-stalker/juvenile-custodian-stalker-lvl93.json"),
            SourceMonsterVariant.class);
        assertEquals("juvenile-custodian-stalker", juvenile.getVariantId());
        assertTrue(juvenile.getNpcIds().contains(14702));
        assertEquals(Integer.valueOf(93), juvenile.getCombatLevel());
        assertEquals(CombatStyle.RANGED, juvenile.getWeakness().getStyle());
        assertEquals("fire", juvenile.getWeakness().getElement());
        assertEquals(45, juvenile.getMonsterDefence().getDefenceLevel());
        assertEquals(-10, juvenile.getMonsterDefence().getSlash());
        assertEquals(35, juvenile.getMonsterDefence().getMagic());
        assertEquals(5, juvenile.getMonsterDefence().getRange());
        assertEquals("custodian-stalker", juvenile.getStrategyId());

        SourceMonsterVariant elder = read(
            Paths.get("src/main/data/slayer/monsters/custodian-stalker/elder-custodian-stalker-lvl142.json"),
            SourceMonsterVariant.class);
        assertTrue(elder.getNpcIds().contains(14704));
        assertEquals(Integer.valueOf(142), elder.getCombatLevel());
        assertEquals("76 Slayer; only elder stalkers can spawn Ancient Custodian superiors",
            elder.getRequirement());
        assertEquals("custodian-stalker", elder.getStrategyId());

        SourceMonsterVariant ancient = read(
            Paths.get("src/main/data/slayer/monsters/custodian-stalker/ancient-custodian-lvl239.json"),
            SourceMonsterVariant.class);
        assertTrue(ancient.getNpcIds().contains(14520));
        assertEquals(Integer.valueOf(239), ancient.getCombatLevel());
        assertEquals(80, ancient.getMonsterDefence().getDefenceLevel());
        assertEquals(-30, ancient.getMonsterDefence().getSlash());
        assertFalse(ancient.isBoss());
        assertEquals("custodian-stalker", ancient.getStrategyId());
    }

    @Test
    public void strategySourceCoversTaskPageEquipmentTabsAndMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/custodian-stalker/strategy.json");
        assertTrue("Custodian stalker strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("custodian-stalker", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Custodian_stalker",
            strategy.getSourceUrl());
        assertEquals(Set.of("juvenile-custodian-stalker", "mature-custodian-stalker",
            "elder-custodian-stalker", "ancient-custodian"),
            strategy.getVariantIds().stream().collect(Collectors.toSet()));
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("venator-bow"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "kodai-wand".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "blade-of-saeldor".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MELEE));

        assertContains(methodIds(strategy), "ranged-multicombat");
        assertContains(methodIds(strategy), "magic-multicombat");
        assertContains(methodIds(strategy), "melee-single-combat");
        assertContains(methodIds(strategy), "elder-superior-focus");
        assertTrue(strategy.getMechanics().stream().anyMatch(m -> m.contains("20%")
            && m.contains("bleed")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "ranged-multicombat".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing ranged-multicombat method"))
            .getRequiredOrKeyItems().stream().anyMatch(item -> item.contains("Dwarf multicannon")));
    }

    @Test
    public void generatedRuntimeDataCarriesCustodianSourcesAndStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Custodian stalker".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Custodian stalker"));

        assertEquals(Set.of("chaeldar", "nieve"), task.getAssignedBy().stream().collect(Collectors.toSet()));
        assertEquals(Integer.valueOf(11), task.getWeightByMaster().get("chaeldar"));
        assertEquals(Integer.valueOf(8), task.getWeightByMaster().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertTrue(task.getLocations().stream().anyMatch(location ->
            "Stalker Den".equals(location.getName()) && location.isMulti() && location.isCannon()
                && location.isSafeSpot() && !location.isKonarLockable()));

        assertEquals(4, task.getVariants().size());
        MonsterVariant juvenile = task.getVariants().stream()
            .filter(variant -> "Juvenile custodian stalker".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing juvenile variant"));
        assertTrue(juvenile.isDefault());
        assertNotNull(juvenile.getStrategy());
        assertEquals(CombatStyle.RANGED, juvenile.getStrategy().getPrimaryStyle());
        assertEquals("Venator bow", juvenile.getStrategy().getPrimaryWeapons().get(0).getName());

        MonsterVariant ancient = task.getVariants().stream()
            .filter(variant -> "Ancient Custodian".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Ancient Custodian variant"));
        assertNotNull(ancient.getStrategy());
    }

    @Test
    public void locationSourceMatchesStalkerDenEvidence() throws IOException
    {
        SourceLocation location = read(
            Paths.get("src/main/data/slayer/locations/stalker-den-custodian-stalkers.json"),
            SourceLocation.class);

        assertEquals("stalker-den-custodian-stalkers", location.getLocationId());
        assertEquals("Stalker Den", location.getName());
        assertTrue(location.isMulti());
        assertTrue(location.isCannon());
        assertFalse(location.isBurst());
        assertFalse(location.isKonarLockable());
        assertTrue(location.isSafeSpot());
        assertFalse(location.isWilderness());
        assertTrue(location.getAccessNote().contains("south-west"));
        assertTrue(location.getAccessNote().contains("single-combat"));
        assertTrue(location.getAccessNote().contains("Shadows of Custodia"));
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

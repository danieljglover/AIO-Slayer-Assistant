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

public class WyrmsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsUnlocksAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/wyrms.json"), SourceTask.class);

        assertEquals(Integer.valueOf(356964), task.getWikiPageId());
        assertEquals(240, task.getSlayerTargetId());
        assertEquals(62, task.getSlayerLevel());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());
        assertTrue(task.isDragon());
        assertEquals("Boots of stone", task.getRequiredItemName());

        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {60, 100}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {125, 190}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {80, 145}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {100, 160}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));
        assertContains(unlockIds(task), "can-of-wyrms");
        assertContains(unlockIds(task), "bigger-and-badder");

        assertContains(locationIds(task), "karuulm-slayer-dungeon-wyrms");
        assertContains(locationIds(task), "neypotzli-wyrmlings");
        assertContains(locationIds(task), "charred-dungeon-lava-strykewyrms");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Boots of stone")
            && note.contains("Elite Kourend")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Magic")
            && note.contains("direct melee range")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("flat armour")
            && note.contains("1")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wyrmlings")
            && note.contains("cannot spawn the Shadow Wyrm")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Lava Strykewyrms")
            && note.contains("Protect from Missiles")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragon metal sheet")
            && note.contains("4x more common")));
    }

    @Test
    public void taskSourceContainsVariantRowsAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/wyrms.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, variants.size());
        assertVariant(variants, "wyrm", 97, 123, "melee and magic");
        assertVariant(variants, "shadow-wyrm", 267, 3520, "Bigger and Badder");
        assertVariant(variants, "wyrmling", 55, 65, "Earthbound Cavern");
        assertVariant(variants, "lava-strykewyrm", 116, 154, "60 Sailing");
        assertVariant(variants, "magma-strykewyrm", 249, 3655, "burrow");

        assertEquals(3, locations.size());
        assertLocation(locations, "karuulm-slayer-dungeon-wyrms", 16, false, false, true, "task-only area");
        assertLocation(locations, "neypotzli-wyrmlings", 6, false, true, true, "Only drops");
        assertLocation(locations, "charred-dungeon-lava-strykewyrms", 12, false, false, true, "Charred Dungeon");
    }

    @Test
    public void variantSourcesUseCurrentMonsterPageStatsAndStrategyIds() throws IOException
    {
        SourceMonsterVariant wyrm = readVariant("wyrm-lvl97.json");
        SourceMonsterVariant shadowWyrm = readVariant("shadow-wyrm-lvl259.json");
        SourceMonsterVariant wyrmling = readVariant("wyrmling-lvl55.json");
        SourceMonsterVariant lavaStrykewyrm = readVariant("lava-strykewyrm-lvl116.json");
        SourceMonsterVariant magmaStrykewyrm = readVariant("magma-strykewyrm-lvl249.json");

        assertEquals("earth", wyrm.getWeakness().getElement());
        assertDefence(wyrm, 80, 10, 50, 50, 50, 20);
        assertEquals("wyrms", wyrm.getStrategyId());
        assertEquals("earth", shadowWyrm.getWeakness().getElement());
        assertDefence(shadowWyrm, 125, 20, 100, 100, 50, 0);
        assertEquals("wyrms", shadowWyrm.getStrategyId());
        assertEquals("earth", wyrmling.getWeakness().getElement());
        assertDefence(wyrmling, 40, 20, 50, 50, 50, 20);
        assertEquals("wyrms", wyrmling.getStrategyId());
        assertEquals("water", lavaStrykewyrm.getWeakness().getElement());
        assertDefence(lavaStrykewyrm, 50, 30, 60, 70, 40, 120);
        assertEquals("wyrms", lavaStrykewyrm.getStrategyId());
        assertEquals("water", magmaStrykewyrm.getWeakness().getElement());
        assertDefence(magmaStrykewyrm, 110, 60, 30, 70, 40, 120);
        assertEquals("wyrms", magmaStrykewyrm.getStrategyId());
    }

    @Test
    public void strategyJsonCoversAllTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path strategyJson = Paths.get("src/main/data/slayer/strategies/wyrms/strategy.json");

        assertTrue("Wyrms strategy JSON missing", Files.exists(strategyJson));
        SourceStrategy strategy = read(strategyJson, SourceStrategy.class);

        assertEquals("wyrms", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Wyrms", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dragon-hunter-lance"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("noxious-halberd"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dragon-hunter-wand".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));

        assertContains(methodIds(strategy), "requirements-and-unlocks");
        assertContains(methodIds(strategy), "karuulm-ranged-safespot");
        assertContains(methodIds(strategy), "karuulm-melee-dragonbane");
        assertContains(methodIds(strategy), "elemental-magic");
        assertContains(methodIds(strategy), "wyrmling-alternative");
        assertContains(methodIds(strategy), "lava-strykewyrm-alternative");
        assertContains(methodIds(strategy), "superior-variants");
        assertContains(styleIds(strategy), "ranged-safespot");
        assertContains(styleIds(strategy), "melee-dragonbane");
        assertContains(styleIds(strategy), "elemental-magic");

        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("Protect from Magic")));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("60 Sailing")));
        assertTrue(strategy.getMethods().stream().anyMatch(method -> method.getSummary().contains("Shadow Wyrm")));
    }

    @Test
    public void generatedRuntimeDataCarriesWyrmStrategiesAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Wyrms".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Wyrms"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(5, task.getVariants().size());
        assertEquals(3, task.getLocations().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Karuulm Slayer Dungeon - Wyrms"
            .equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Neypotzli - Wyrmlings"
            .equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Charred Dungeon - Lava Strykewyrms"
            .equals(location.getName())));

        MonsterVariant wyrm = variantNamed(task, "Wyrm");
        MonsterVariant shadowWyrm = variantNamed(task, "Shadow Wyrm");
        MonsterVariant lavaStrykewyrm = variantNamed(task, "Lava Strykewyrm");

        assertNotNull(wyrm.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Wyrms", wyrm.getStrategy().getSourceUrl());
        assertNotNull(shadowWyrm.getStrategy());
        assertNotNull(lavaStrykewyrm.getStrategy());
        assertEquals(CombatStyle.MELEE, wyrm.getWeakness().getStyle());
    }

    private static SourceMonsterVariant readVariant(String fileName) throws IOException
    {
        return read(Paths.get("src/main/data/slayer/monsters/wyrms/" + fileName), SourceMonsterVariant.class);
    }

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        Integer combatLevel, Integer slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(slayerXp.doubleValue(), variant.getSlayerXp().doubleValue(), 0.0);
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
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

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("missing " + expected + " in " + values, values.contains(expected));
    }
}

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

public class SpiritualCreaturesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsUnlockAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/spiritual-creatures.json"), SourceTask.class);

        assertEquals(Integer.valueOf(526458), task.getWikiPageId());
        assertEquals(Integer.valueOf(80), task.getCombatLevel());
        assertEquals(233, task.getSlayerTargetId());
        assertEquals(63, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Death Plateau"));
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("air", task.getWeakness().getElement());
        assertFalse(task.isDemon());

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertFalse(task.getMasterIds().contains("konar"));
        assertArrayEquals(new int[] {100, 150}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {181, 250}, task.getExtendedAmount().get("krystilia"));
        assertArrayEquals(new int[] {181, 250}, task.getExtendedAmount().get("vannaka"));
        assertArrayEquals(new int[] {181, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {181, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {181, 250}, task.getExtendedAmount().get("duradel"));
        assertContains(unlockIds(task), "spiritual-fervour");

        assertContains(locationIds(task), "god-wars-dungeon");
        assertContains(locationIds(task), "god-wars-dungeon-ancient-prison");
        assertContains(locationIds(task), "wilderness-god-wars-dungeon-spiritual-creatures");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("60 Strength")
            && note.contains("60 Agility")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Spiritual rangers")
            && note.contains("63 Slayer")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Spiritual warriors")
            && note.contains("68 Slayer")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Spiritual mages")
            && note.contains("83 Slayer")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dragon boots")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("god-affiliated item")
            && note.contains("tolerant")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("skip or block")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Zamorak's Fortress")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ancient Prison")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("neither cannonable nor AFK")));
    }

    @Test
    public void taskSourceContainsAllVariantRowsAndLocationComparisonRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/spiritual-creatures.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(3, variants.size());
        assertVariant(variants, "spiritual-ranger", "118 (Zamorak)", "Zarosian variant can occasionally launch");
        assertVariant(variants, "spiritual-warrior", "115 (Zamorak)", "X formation");
        assertVariant(variants, "spiritual-mage", "121 (Zamorak)", "Flames of Zamorak");
        assertTrue(variants.get("spiritual-ranger").getNotes().stream()
            .anyMatch(note -> note.contains("Zaros") && note.contains("disable overhead")));
        assertTrue(variants.get("spiritual-mage").getNotes().stream()
            .anyMatch(note -> note.contains("Ancient Magicks") && note.contains("blood heals")));

        assertEquals(9, locations.size());
        assertLocation(locations, "spiritual-rangers-god-wars-dungeon", 30, true, false, true, "Main");
        assertLocation(locations, "spiritual-rangers-ancient-prison", 9, true, false, true, "Ancient Prison");
        assertLocation(locations, "spiritual-rangers-wilderness-god-wars-dungeon", 12, true, false, true,
            "level 28-32 Wilderness");
        assertLocation(locations, "spiritual-warriors-god-wars-dungeon", 44, true, false, true, "Main");
        assertLocation(locations, "spiritual-warriors-ancient-prison", 9, true, false, true, "Ancient Prison");
        assertLocation(locations, "spiritual-warriors-wilderness-god-wars-dungeon", 11, true, false, true,
            "level 28-32 Wilderness");
        assertLocation(locations, "spiritual-mages-god-wars-dungeon", 40, true, false, true, "Main");
        assertLocation(locations, "spiritual-mages-ancient-prison", 5, true, false, true, "Ancient Prison");
        assertLocation(locations, "spiritual-mages-wilderness-god-wars-dungeon", 12, true, false, true,
            "level 28-32 Wilderness");
    }

    @Test
    public void variantSourcesUseCurrentMonsterPageStatsAndSharedStrategyId() throws IOException
    {
        SourceMonsterVariant zamorakRanger = readVariant("spiritual-ranger-zamorak-lvl118.json");
        SourceMonsterVariant saradominRanger = readVariant("spiritual-ranger-saradomin-lvl122.json");
        SourceMonsterVariant zarosRanger = readVariant("spiritual-ranger-zaros-lvl158.json");
        SourceMonsterVariant armadylWarrior = readVariant("spiritual-warrior-armadyl-lvl123.json");
        SourceMonsterVariant zarosWarrior = readVariant("spiritual-warrior-zaros-lvl158.json");
        SourceMonsterVariant saradominMage = readVariant("spiritual-mage-saradomin-lvl120.json");
        SourceMonsterVariant zarosMage = readVariant("spiritual-mage-zaros-lvl182.json");

        assertEquals("air", zamorakRanger.getWeakness().getElement());
        assertDefence(zamorakRanger, 80, 0, 0, 0, 0, 0);
        assertEquals("spiritual-creatures", zamorakRanger.getStrategyId());
        assertDefence(saradominRanger, 100, 3, 5, 13, 16, 23);
        assertEquals("fire", zarosRanger.getWeakness().getElement());
        assertDefence(zarosRanger, 100, 20, 0, 20, 300, 50);
        assertDefence(armadylWarrior, 120, 23, 25, 13, 35, 35);
        assertDefence(zarosWarrior, 100, 100, 100, 80, 0, 250);
        assertDefence(saradominMage, 86, 8, 7, 3, 16, 2);
        assertEquals("fire", zarosMage.getWeakness().getElement());
        assertDefence(zarosMage, 100, 420, 400, 420, 200, 0);
        assertEquals("spiritual-creatures", zarosMage.getStrategyId());
    }

    @Test
    public void strategyJsonCoversTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/spiritual-creatures/strategy.json");

        assertTrue("Spiritual creatures strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("spiritual-creatures", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Spiritual_creatures",
            strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("zamorakian-hasta"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "rune-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-swamp".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("skip or block rangers"));
        assertTrue(strategy.getPlugin().getNote().contains("Zamorak's Fortress"));

        assertContains(methodIds(strategy), "requirements-and-access");
        assertContains(methodIds(strategy), "god-item-protection");
        assertContains(methodIds(strategy), "target-selection");
        assertContains(methodIds(strategy), "zamorak-fortress-warriors");
        assertContains(methodIds(strategy), "spiritual-mages");
        assertContains(methodIds(strategy), "zaros-ancient-prison");
        assertContains(methodIds(strategy), "wilderness-god-wars");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "target-selection".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing target selection"))
            .getSteps().stream().anyMatch(step -> step.contains("Spiritual rangers") && step.contains("skip")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "zaros-ancient-prison".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing zaros method"))
            .getRisks().stream().anyMatch(risk -> risk.contains("40 damage")));

        assertContains(styleIds(strategy), "zamorak-warriors-melee");
        assertContains(styleIds(strategy), "spiritual-mages-dragon-boots");
        assertContains(styleIds(strategy), "zaros-ancient-prison");
    }

    @Test
    public void generatedRuntimeDataCarriesSpiritualCreatureStrategyAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Spiritual creatures".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Spiritual creatures"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertFalse(task.getAssignedBy().contains("konar"));
        assertTrue(task.getLocations().stream().anyMatch(location -> "God Wars Dungeon".equals(location.getName())
            && location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "God Wars Dungeon - Ancient Prison"
            .equals(location.getName()) && location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness God Wars Dungeon"
            .equals(location.getName()) && location.isWilderness()));

        MonsterVariant zamorakWarrior = variantNamed(task, "Spiritual warrior (Zamorak)");
        MonsterVariant zarosMage = variantNamed(task, "Spiritual mage (Zaros)");

        assertNotNull(zamorakWarrior.getStrategy());
        assertEquals(CombatStyle.MELEE, zamorakWarrior.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Spiritual_creatures",
            zamorakWarrior.getStrategy().getSourceUrl());
        assertNotNull(zarosMage.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Spiritual_creatures",
            zarosMage.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchSpiritualCreaturesTaskPage() throws IOException
    {
        assertLocationSource("god-wars-dungeon", "God Wars Dungeon", true, false, true, false,
            "Death Plateau");
        assertLocationSource("god-wars-dungeon-ancient-prison", "God Wars Dungeon - Ancient Prison", true,
            false, true, false, "The Frozen Door");
        assertLocationSource("wilderness-god-wars-dungeon-spiritual-creatures", "Wilderness God Wars Dungeon", true, false, true, true,
            "level 28-32 Wilderness");
    }

    private static SourceMonsterVariant readVariant(String fileName) throws IOException
    {
        return read(Paths.get("src/main/data/slayer/monsters/spiritual-creatures/" + fileName),
            SourceMonsterVariant.class);
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
        return strategy.getStyleOptions().stream().map(SourceStrategyStyleOption::getStyleId)
            .collect(Collectors.toSet());
    }

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        String combatLevelText, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(combatLevelText)));
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
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

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KalphitesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentMechanicsAndKerisGuidance() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/kalphite.json"), SourceTask.class);

        assertEquals(Integer.valueOf(259777), task.getWikiPageId());
        assertEquals(Integer.valueOf(15), task.getCombatLevel());
        assertEquals(225, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("fire", task.getWeakness().getElement());
        assertTrue(task.isKalphite());

        assertContains(masterIds(task), "turael");
        assertContains(masterIds(task), "spria");
        assertContains(masterIds(task), "mazchna");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertFalse("Krystilia does not assign Kalphites", task.getMasterIds().contains("krystilia"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("turael"));
        assertArrayEquals(new int[] {15, 30}, task.getAmountByMaster().get("spria"));
        assertArrayEquals(new int[] {30, 50}, task.getAmountByMaster().get("mazchna"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertTrue(task.getExtendedAmount() == null || task.getExtendedAmount().isEmpty());

        assertContains(locationIds(task), "kalphite-lair");
        assertContains(locationIds(task), "kalphite-cave");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("15 combat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("exception of Wilderness Slayer master Krystilia")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Kalphite Cave") && note.contains("task-exclusive")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("melee attacks") && note.contains("Queen")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("poison") && note.contains("bypasses protection prayers")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee") && note.contains("poison immunity")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("five chambers")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("workers") && note.contains("quick task")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("soldiers") && note.contains("experience")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Guardians") && note.contains("cost/experience")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Kalphite Queen") && note.contains("86 Agility")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("keris") && note.contains("1/51")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Keris partisan") && note.contains("Beneath Cursed Sands")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Keris partisan of breaching") && note.contains("33% accuracy")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/kalphite.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertVariant(variants, "kalphite-worker", 28, 40.0, "weakest variant");
        assertVariant(variants, "kalphite-soldier", 85, 90.0, "poison");
        assertVariant(variants, "kalphite-guardian", 141, 170.0, "poison");
        assertVariant(variants, "kalphite-queen", 333, 535.5, "Kalphite Queen/Strategies");

        assertEquals(2, locations.size());
        assertLocation(locations, "kalphite-lair", "13 Workers; 7 Soldiers; 4 Guardians; 1 Queen",
            true, true, true, "rope");
        assertLocation(locations, "kalphite-cave", "18 Workers; 20 Soldiers; 5 Guardians",
            true, true, true, "Slayer task-exclusive");
    }

    @Test
    public void variantSourcesUseMonsterPageStatsAndTaskStrategies() throws IOException
    {
        SourceMonsterVariant worker = read(Paths.get("src/main/data/slayer/monsters/kalphite/kalphite-worker-lvl28.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant soldier = read(Paths.get("src/main/data/slayer/monsters/kalphite/kalphite-soldier-lvl85.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant guardian = read(Paths.get("src/main/data/slayer/monsters/kalphite/kalphite-guardian-lvl141.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant crawling = read(Paths.get("src/main/data/slayer/monsters/kalphite/kalphite-queen-crawling-form-lvl333.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant airborne = read(Paths.get("src/main/data/slayer/monsters/kalphite/kalphite-queen-airborne-form-lvl333.json"),
            SourceMonsterVariant.class);

        assertVariantSource(worker, "kalphite-worker", 28, 20, 5, 5, 1, 10, 10, "kalphite");
        assertVariantSource(soldier, "kalphite-soldier", 85, 70, 25, 25, 5, 50, 30, "kalphite");
        assertVariantSource(guardian, "kalphite-guardian", 141, 110, 25, 25, 5, 50, 30, "kalphite");
        assertEquals("fire", worker.getWeakness().getElement());
        assertEquals("fire", soldier.getWeakness().getElement());
        assertEquals("fire", guardian.getWeakness().getElement());
        assertTrue(soldier.getRequirement().contains("poison starts at 4"));
        assertTrue(guardian.getRequirement().contains("poison starts at 6"));

        assertEquals("kalphite-queen-crawling-form", crawling.getVariantId());
        assertEquals("kalphite-queen-crawling-form", crawling.getStrategyId());
        assertEquals(300, crawling.getMonsterDefence().getDefenceLevel());
        assertEquals(10, crawling.getMonsterDefence().getCrush());
        assertEquals(100, crawling.getMonsterDefence().getRange());

        assertEquals("kalphite-queen-airborne-form", airborne.getVariantId());
        assertEquals("kalphite-queen-airborne-form", airborne.getStrategyId());
        assertEquals(300, airborne.getMonsterDefence().getDefenceLevel());
        assertEquals(10, airborne.getMonsterDefence().getMagic());
        assertEquals(10, airborne.getMonsterDefence().getRange());
    }

    @Test
    public void strategySourceContainsTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/kalphite/strategy.json");

        assertTrue("Kalphite strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("kalphite", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Kalphites", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("keris-partisan-of-breaching"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("keris-partisan"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-swamp".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("Kalphite Cave"));
        assertTrue(strategy.getPlugin().getNote().contains("poison immunity"));

        assertContains(methodIds(strategy), "optimal-tasking");
        assertContains(methodIds(strategy), "worker-quick-cannon");
        assertContains(methodIds(strategy), "soldier-experience-cannon");
        assertContains(methodIds(strategy), "guardian-avoid");
        assertContains(methodIds(strategy), "konar-lair");
        assertContains(methodIds(strategy), "keris-weapons");
        assertContains(methodIds(strategy), "kalphite-queen-choice");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "keris-weapons".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing keris method"))
            .getSteps().stream().anyMatch(step -> step.contains("Breach of the scarab")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "soldier-experience-cannon".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing soldier method"))
            .getSteps().stream().anyMatch(step -> step.contains("poison immunity")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "kalphite-queen-choice".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing queen method"))
            .getRequiredOrKeyItems().stream().anyMatch(item -> item.contains("Desert diary")));

        assertContains(styleIds(strategy), "melee-keris");
        assertContains(styleIds(strategy), "ranged-cannon");
        assertContains(styleIds(strategy), "magic-fire");
    }

    @Test
    public void kalphiteQueenFormStrategyMarkdownIsMigratedToJson() throws IOException
    {
        assertFalse("Legacy crawling form Markdown strategy should be migrated to JSON",
            Files.exists(Paths.get("src/main/data/slayer/strategies/kalphite-queen-crawling-form.md")));
        assertFalse("Legacy airborne form Markdown strategy should be migrated to JSON",
            Files.exists(Paths.get("src/main/data/slayer/strategies/kalphite-queen-airborne-form.md")));

        SourceStrategy crawling = read(Paths.get("src/main/data/slayer/strategies/kalphite-queen-crawling-form/strategy.json"),
            SourceStrategy.class);
        SourceStrategy airborne = read(Paths.get("src/main/data/slayer/strategies/kalphite-queen-airborne-form/strategy.json"),
            SourceStrategy.class);

        assertEquals("kalphite-queen-crawling-form", crawling.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Kalphite_Queen/Strategies", crawling.getSourceUrl());
        assertEquals(CombatStyle.MELEE, crawling.getPlugin().getPrimaryStyle());
        assertTrue(crawling.getPlugin().getPrimaryWeapons().contains("keris-partisan-of-breaching"));
        assertTrue(crawling.getPlugin().getNote().contains("Phase 1"));
        assertContains(methodIds(crawling), "phase-one-melee");

        assertEquals("kalphite-queen-airborne-form", airborne.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Kalphite_Queen/Strategies", airborne.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, airborne.getPlugin().getPrimaryStyle());
        assertTrue(airborne.getPlugin().getPrimaryWeapons().contains("tumeken-s-shadow"));
        assertTrue(airborne.getPlugin().getNote().contains("Phase 2"));
        assertContains(methodIds(airborne), "phase-two-magic-ranged");
    }

    @Test
    public void generatedRuntimeDataCarriesKalphiteSourcesAndStrategies() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Kalphite".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Kalphite"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "turael");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "spria");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "mazchna");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.isKalphite());
        assertEquals(5, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Kalphite Cave".equals(location.getName())
            && location.isMulti() && location.isCannon()));

        MonsterVariant worker = variantNamed(task, "Kalphite Worker");
        MonsterVariant soldier = variantNamed(task, "Kalphite Soldier");
        MonsterVariant queen = variantNamed(task, "Kalphite Queen (crawling form)");

        assertNotNull(worker.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Kalphites",
            worker.getStrategy().getSourceUrl());
        assertNotNull(soldier.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Kalphites",
            soldier.getStrategy().getSourceUrl());
        assertNotNull(queen.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Kalphite_Queen/Strategies",
            queen.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("kalphite-lair", "Kalphite Lair", true, true, true, false, "13 Workers");
        assertLocationSource("kalphite-cave", "Kalphite Cave", true, true, true, false,
            "Slayer task-exclusive");
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
        Integer combatLevel, double slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(slayerXp, variant.getSlayerXp().doubleValue(), 0.0);
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        String amount, boolean multi, boolean cannon, boolean safespot, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertNull("Kalphite mixed-count rows should preserve amount in notes", location.getAmount());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(amount)));
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertVariantSource(SourceMonsterVariant variant, String variantId, Integer combatLevel,
        int defenceLevel, int stab, int slash, int crush, int magic, int range, String strategyId)
    {
        assertEquals(variantId, variant.getVariantId());
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(CombatStyle.MELEE, variant.getWeakness().getStyle());
        assertEquals("fire", variant.getWeakness().getElement());
        assertEquals(defenceLevel, variant.getMonsterDefence().getDefenceLevel());
        assertEquals(stab, variant.getMonsterDefence().getStab());
        assertEquals(slash, variant.getMonsterDefence().getSlash());
        assertEquals(crush, variant.getMonsterDefence().getCrush());
        assertEquals(magic, variant.getMonsterDefence().getMagic());
        assertEquals(range, variant.getMonsterDefence().getRange());
        assertTrue(variant.isKalphite());
        assertEquals(strategyId, variant.getStrategyId());
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safeSpot, boolean wilderness, String accessNote) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertEquals(safeSpot, location.isSafeSpot());
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

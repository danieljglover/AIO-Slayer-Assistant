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

public class SuqahsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/suqahs.json"), SourceTask.class);

        assertEquals(Integer.valueOf(378001), task.getWikiPageId());
        assertEquals(Integer.valueOf(85), task.getCombatLevel());
        assertEquals(234, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Lunar Diplomacy partial completion"));
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());
        assertFalse(task.isDemon());

        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertFalse(task.getMasterIds().contains("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {60, 90}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {186, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {162, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(locationIds(task), "lunar-isle-north-suqah");
        assertContains(locationIds(task), "lunar-isle-south-east-suqah");
        assertContains(locationIds(task), "lunar-isle-south-west-suqah");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dwarf multicannon")
            && note.contains("high amount of experience")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("loss of coins")
            && note.contains("lack of valuable drops")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("northern area")
            && note.contains("Protect from Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("southern part")
            && note.contains("only melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Seal of passage")
            && note.contains("Dream Mentor")));
    }

    @Test
    public void taskSourceContainsVariantRowAndLocationComparisonRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/suqahs.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(1, variants.size());
        SourceTaskVariantInfo suqah = variants.get("suqah");
        assertNotNull(suqah);
        assertEquals(Integer.valueOf(111), suqah.getCombatLevel());
        assertEquals(107.6, suqah.getSlayerXp().doubleValue(), 0.0);
        assertTrue(suqah.getNotes().stream().anyMatch(note -> note.contains("Stab")
            && note.contains("Magic")));
        assertTrue(suqah.getNotes().stream().anyMatch(note -> note.contains("Ice Barrage")
            && note.contains("Water Wave")));

        assertEquals(3, locations.size());
        assertLocation(locations, "lunar-isle-north-suqah", 16, true, true, true, "Protect from Magic");
        assertLocation(locations, "lunar-isle-south-east-suqah", 4, true, true, true, "melee");
        assertLocation(locations, "lunar-isle-south-west-suqah", 6, false, true, true, "single-way");
    }

    @Test
    public void variantSourceUsesCurrentMonsterPageStatsAndStrategyId() throws IOException
    {
        SourceMonsterVariant suqah = read(Paths.get(
            "src/main/data/slayer/monsters/suqahs/suqah-lvl111.json"),
            SourceMonsterVariant.class);

        assertEquals(Integer.valueOf(111), suqah.getCombatLevel());
        assertEquals("earth", suqah.getWeakness().getElement());
        assertDefence(suqah, 95, 50, 70, 70, 90, 30);
        assertEquals("suqahs", suqah.getStrategyId());
        assertTrue(suqah.getLocation().contains("North Lunar Isle"));
        assertTrue(suqah.getLocation().contains("South Lunar Isle"));
        assertTrue(suqah.getRequirement().contains("Lunar Diplomacy"));
    }

    @Test
    public void strategyJsonCoversTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/suqahs/strategy.json");

        assertTrue("Suqahs strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("suqahs", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Suqah", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("venator-bow"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("toxic-blowpipe"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "ghrazi-rapier".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getNote().contains("north"));
        assertTrue(strategy.getPlugin().getNote().contains("south"));

        assertContains(methodIds(strategy), "requirements-and-access");
        assertContains(methodIds(strategy), "north-ranged-cannon-safespot");
        assertContains(methodIds(strategy), "south-melee-cannon");
        assertContains(methodIds(strategy), "south-west-melee-only-cannon");
        assertContains(methodIds(strategy), "banking-and-supplies");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "north-ranged-cannon-safespot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing north ranged method"))
            .getSteps().stream().anyMatch(step -> step.contains("hill") && step.contains("Ranged")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "south-west-melee-only-cannon".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing south-west method"))
            .getSteps().stream().anyMatch(step -> step.contains("dead tree") && step.contains("north-west")));

        assertContains(styleIds(strategy), "ranged-north");
        assertContains(styleIds(strategy), "melee-south");
    }

    @Test
    public void generatedRuntimeDataCarriesSuqahStrategyAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Suqahs".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Suqahs"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertFalse(task.getAssignedBy().contains("konar"));
        assertTrue(task.getLocations().stream().anyMatch(location -> "North Lunar Isle".equals(location.getName())
            && location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "South-west Lunar Isle".equals(location.getName())
            && !location.isMulti() && location.isCannon()));

        MonsterVariant suqah = variantNamed(task, "Suqah");

        assertNotNull(suqah.getStrategy());
        assertEquals(CombatStyle.RANGED, suqah.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Suqah", suqah.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchSuqahTaskPage() throws IOException
    {
        assertLocationSource("lunar-isle-north-suqah", "North Lunar Isle", true, true, true, false,
            "16 Suqah");
        assertLocationSource("lunar-isle-south-east-suqah", "South-east Lunar Isle", true, true, true, false,
            "4 Suqah");
        assertLocationSource("lunar-isle-south-west-suqah", "South-west Lunar Isle", false, true, true, false,
            "6 Suqah");
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
        int amount, boolean multicombat, boolean cannonable, boolean safespottable, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location " + locationId, location);
        assertEquals(Integer.valueOf(amount), location.getAmount());
        assertEquals(Boolean.valueOf(multicombat), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannonable), location.getCannonable());
        assertEquals(Boolean.valueOf(safespottable), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safeSpot, boolean wilderness, String accessText) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertFalse(location.isBurst());
        assertFalse(location.isKonarLockable());
        assertEquals(safeSpot, location.isSafeSpot());
        assertEquals(wilderness, location.isWilderness());
        assertTrue(location.getAccessNote().contains(accessText));
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

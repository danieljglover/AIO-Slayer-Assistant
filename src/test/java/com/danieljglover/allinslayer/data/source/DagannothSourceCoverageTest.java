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

public class DagannothSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentRequirementsAndNotes() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dagannoth.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298104), task.getWikiPageId());
        assertEquals(Integer.valueOf(75), task.getCombatLevel());
        assertEquals(213, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Horror from the Deep"));
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("earth", task.getWeakness().getElement());

        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("high slayer experience")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Lighthouse") && note.contains("AFK")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Jormungand") && note.contains("cannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Catacombs") && note.contains("Dinh")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Waterbirth") && note.contains("spawn")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dagannoth Kings")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Expert Reanimation")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("seed box")));
    }

    @Test
    public void taskSourceContainsAllWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/dagannoth.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(8, variants.size());
        assertEquals(70.0, variants.get("dagannoth").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("dagannoth").getNotes().stream()
            .anyMatch(note -> note.contains("level 74") && note.contains("range")));
        assertEquals(99.6, variants.get("dagannoth-waterbirth").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("dagannoth-waterbirth").getNotes().stream()
            .anyMatch(note -> note.contains("level 88") && note.contains("ranged")));
        assertEquals(35.0, variants.get("dagannoth-spawn").getSlayerXp().doubleValue(), 0.0);
        assertEquals(0.0, variants.get("dagannoth-fledgeling").getSlayerXp().doubleValue(), 0.0);
        assertEquals(331.5, variants.get("dagannoth-prime").getSlayerXp().doubleValue(), 0.0);
        assertEquals(331.5, variants.get("dagannoth-rex").getSlayerXp().doubleValue(), 0.0);
        assertEquals(255.0, variants.get("dagannoth-supreme").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("reanimated-dagannoth").getNotes().stream()
            .anyMatch(note -> note.contains("936 Prayer")));

        assertEquals(4, locations.size());
        assertLocation(locations, "catacombs-of-kourend", 14, true, false, true, "southern section");
        assertLocation(locations, "jormungand-s-prison-dagannoth", 62, true, true, true,
            "The Fremennik Exiles");
        assertLocation(locations, "lighthouse-basement", 22, true, true, false, "fastest slayer experience");
        assertLocation(locations, "waterbirth-island", 93, true, true, true, "Dagannoth Kings");
    }

    @Test
    public void regularDagannothStrategySourceContainsAllTaskMethods() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/dagannoth/strategy.json");

        assertTrue("Dagannoth strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("dagannoth", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dagannoth", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("osmumten-s-fang"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "dinh-s-bulwark".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));
        assertTrue(strategy.getPlugin().getNote().contains("cannon"));
        assertTrue(strategy.getPlugin().getNote().contains("Dagannoth Kings"));

        assertContains(methodIds(strategy), "lighthouse-cannon-afk");
        assertContains(methodIds(strategy), "jormungands-prison-cannon");
        assertContains(methodIds(strategy), "catacombs-prayer-afk");
        assertContains(methodIds(strategy), "catacombs-safespot");
        assertContains(methodIds(strategy), "waterbirth-island-task");
        assertContains(methodIds(strategy), "dagannoth-kings-alternative");
        assertContains(methodIds(strategy), "expert-reanimation");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "catacombs-prayer-afk".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Catacombs prayer method"))
            .getRequiredOrKeyItems().contains("dinh-s-bulwark"));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "waterbirth-island-task".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Waterbirth method"))
            .getSteps().stream().anyMatch(step -> step.contains("dagannoth spawn")));

        assertContains(styleIds(strategy), "melee-cannon");
        assertContains(styleIds(strategy), "melee-prayer-afk");
        assertContains(styleIds(strategy), "ranged-or-magic-safespot");
        assertContains(styleIds(strategy), "dagannoth-kings");
    }

    @Test
    public void dagannothKingStrategiesContainFullStrategyPageOptions() throws IOException
    {
        for (String strategyId : new String[] {"dagannoth-rex", "dagannoth-prime", "dagannoth-supreme"})
        {
            SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies/" + strategyId
                + "/strategy.json"), SourceStrategy.class);

            assertEquals("https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies",
                strategy.getSourceUrl());
            assertContains(methodIds(strategy), "entering-the-lair");
            assertContains(methodIds(strategy), "handling-multicombat");
            assertContains(methodIds(strategy), "tribrid-safe-rotation");
            assertContains(methodIds(strategy), "tribrid-fast-rotation");
            assertContains(methodIds(strategy), "rex-only-safespot");
            assertContains(methodIds(strategy), "prime-only");
            assertContains(methodIds(strategy), "inventory-max-tribrid");
            assertContains(methodIds(strategy), "inventory-mid-level-tribrid");
            assertContains(methodIds(strategy), "inventory-rex-safespot");
            assertContains(methodIds(strategy), "inventory-early-ironman-rex");
            assertContains(methodIds(strategy), "tips-and-tricks");
            assertContains(styleIds(strategy), "tribrid-solo");
            assertContains(styleIds(strategy), "magic-rex-only");
            assertContains(styleIds(strategy), "ranged-prime-only");
            assertTrue(strategy.getMethods().stream()
                .filter(method -> "entering-the-lair".equals(method.getMethodId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing entering method"))
                .getPrayers().contains("Protect from Magic"));
            assertTrue(strategy.getMethods().stream()
                .filter(method -> "rex-only-safespot".equals(method.getMethodId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing Rex method"))
                .getSteps().stream().anyMatch(step -> step.contains("eastern wall")));
            assertTrue(strategy.getMethods().stream()
                .filter(method -> "tips-and-tricks".equals(method.getMethodId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing tips method"))
                .getNotes().stream().anyMatch(note -> note.contains("1,000-2,000 cannonballs")));
        }
    }

    @Test
    public void generatedRuntimeDataCarriesDagannothStrategiesAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Dagannoth".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Dagannoth"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Jormungand's Prison".equals(location.getName())
            && location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Lighthouse Basement".equals(location.getName())
            && location.isMulti() && location.isCannon()));

        MonsterVariant dagannoth = variantNamed(task, "Dagannoth");
        MonsterVariant waterbirth = variantNamed(task, "Dagannoth (Waterbirth Island)");
        MonsterVariant rex = variantNamed(task, "Dagannoth Rex");
        MonsterVariant prime = variantNamed(task, "Dagannoth Prime");
        MonsterVariant supreme = variantNamed(task, "Dagannoth Supreme");

        assertNotNull(dagannoth.getStrategy());
        assertEquals(CombatStyle.MELEE, dagannoth.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dagannoth",
            dagannoth.getStrategy().getSourceUrl());
        assertNotNull(waterbirth.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Dagannoth",
            waterbirth.getStrategy().getSourceUrl());
        assertEquals("https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies",
            rex.getStrategy().getSourceUrl());
        assertEquals("https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies",
            prime.getStrategy().getSourceUrl());
        assertEquals("https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies",
            supreme.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchDagannothTaskPage() throws IOException
    {
        assertLocationSource("lighthouse-basement", "Lighthouse Basement", true, true, false, false,
            "AFK");
        assertLocationSource("jormungand-s-prison-dagannoth", "Jormungand's Prison", true, true, true,
            false, "The Fremennik Exiles");
        assertLocationSource("waterbirth-island", "Waterbirth Island", true, true, true, false,
            "pet rock");
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

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream().map(SourceStrategyMethod::getMethodId).collect(Collectors.toSet());
    }

    private static Set<String> styleIds(SourceStrategy strategy)
    {
        return strategy.getStyleOptions().stream().map(SourceStrategyStyleOption::getStyleId)
            .collect(Collectors.toSet());
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

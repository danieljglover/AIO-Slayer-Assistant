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

public class AbyssalDemonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndUnlockData() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/abyssal-demons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(85), task.getCombatLevel());
        assertEquals(Integer.valueOf(298016), task.getWikiPageId());
        assertEquals(12, task.getSlayerTargetId());
        assertEquals(85, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().stream().anyMatch(req -> req.contains("Priest in Peril")));
        assertTrue(task.getQuestReqs().stream().anyMatch(req -> req.contains("Fairytale II - Cure a Queen")));

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("krystilia"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "augment-my-abbies");
        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "unholy-helmet");
        assertContains(locationIds(task), "abyssal-area");
        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "slayer-tower");
        assertContains(locationIds(task), "slayer-tower-basement");
        assertContains(locationIds(task), "wilderness-slayer-cave");
        assertFalse("boss-only Abyssal Nexus should not be a normal task location",
            task.getLocationIds().contains("abyssal-nexus"));

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Teleport anchoring scroll")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Demonbane")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Abyssal whip")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Soul bearer")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Abyssal Sire")));
    }

    @Test
    public void taskSourceContainsWikiVariantRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/abyssal-demons.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));

        assertEquals(4, variants.size());
        assertEquals(150.0, variants.get("abyssal-demon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(150.0, variants.get("abyssal-demon-catacombs").getSlayerXp().doubleValue(), 0.0);
        assertEquals(4200.0, variants.get("greater-abyssal-demon").getSlayerXp().doubleValue(), 0.0);
        assertEquals(478.0, variants.get("abyssal-demons-abyssal-sire").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("greater-abyssal-demon").getNotes().get(0).contains("Superior"));
        assertTrue(variants.get("abyssal-demons-abyssal-sire").getNotes().stream()
            .anyMatch(note -> note.contains("678 with respiratory systems")));
    }

    @Test
    public void taskSourceContainsWikiLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/abyssal-demons.json"), SourceTask.class);
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(5, locations.size());
        assertEquals(Integer.valueOf(13), locations.get("abyssal-area").getAmount());
        assertEquals(Boolean.FALSE, locations.get("abyssal-area").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("abyssal-area").getCannonable());
        assertTrue(locations.get("abyssal-area").getNotes().get(0).contains("aggressive abyssal creatures"));

        assertEquals(Integer.valueOf(13), locations.get("catacombs-of-kourend").getAmount());
        assertEquals(Boolean.TRUE, locations.get("catacombs-of-kourend").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("catacombs-of-kourend").getCannonable());
        assertTrue(locations.get("catacombs-of-kourend").getNotes().get(0).contains("barraging"));

        assertEquals(Integer.valueOf(14), locations.get("slayer-tower").getAmount());
        assertEquals(Boolean.FALSE, locations.get("slayer-tower").getSafespottable());
        assertTrue(locations.get("slayer-tower").getNotes().get(0).contains("71 Agility"));

        assertEquals(Integer.valueOf(14), locations.get("slayer-tower-basement").getAmount());
        assertEquals(Boolean.TRUE, locations.get("slayer-tower-basement").getSafespottable());
        assertTrue(locations.get("slayer-tower-basement").getNotes().get(0).contains("Task only"));

        assertEquals(Integer.valueOf(8), locations.get("wilderness-slayer-cave").getAmount());
        assertEquals(Boolean.TRUE, locations.get("wilderness-slayer-cave").getMulticombat());
        assertEquals(Boolean.TRUE, locations.get("wilderness-slayer-cave").getCannonable());
        assertTrue(locations.get("wilderness-slayer-cave").getNotes().get(0).contains("Level 29-32 Wilderness"));
    }

    @Test
    public void strategySourceContainsAllWikiStrategyTabs() throws IOException
    {
        Path legacyMarkdown = Paths.get("src/main/data/slayer/strategies/abyssal-demons.md");
        Path json = Paths.get("src/main/data/slayer/strategies/abyssal-demons/strategy.json");

        assertFalse("Abyssal demons strategy should be JSON, not Markdown", Files.exists(legacyMarkdown));
        assertTrue("Abyssal demons strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("abyssal-demons", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Abyssal_demons", strategy.getSourceUrl());
        assertEquals(CombatStyle.MAGIC, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("kodai-wand"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("accursed-sceptre"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "venator-bow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "emberlight".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MELEE));

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "catacombs-magic");
        assertContains(methodIds(strategy), "catacombs-ranged");
        assertContains(methodIds(strategy), "wilderness-slayer-cave");
        assertContains(methodIds(strategy), "melee");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "catacombs-magic".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing catacombs magic"))
            .getSteps().stream().anyMatch(step -> step.contains("Smoke Barrage")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness-slayer-cave".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing wilderness strategy"))
            .getRisks().stream().anyMatch(risk -> risk.contains("player killers")));

        assertContains(styleIds(strategy), "catacombs-magic");
        assertContains(styleIds(strategy), "catacombs-ranged");
        assertContains(styleIds(strategy), "wilderness-magic");
        assertContains(styleIds(strategy), "melee");
    }

    @Test
    public void generatedRuntimeDataCarriesAbyssalDemonStrategy() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Abyssal demons".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Abyssal demons"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Abyssal Area".equals(location.getName())));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness Slayer Cave".equals(location.getName())));

        MonsterVariant abyssalDemon = task.getVariants().stream()
            .filter(variant -> "Abyssal demon".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing standard Abyssal demon variant"));

        assertNotNull(abyssalDemon.getStrategy());
        assertEquals(CombatStyle.MAGIC, abyssalDemon.getStrategy().getPrimaryStyle());
        assertEquals("Kodai wand", abyssalDemon.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(abyssalDemon.getStrategy().getNote().contains("Catacombs magic"));
    }

    @Test
    public void abyssalLocationSourcesExist() throws IOException
    {
        assertLocation("abyssal-area", "Abyssal Area", false, false, false, false, false);
        assertLocation("slayer-tower-basement", "Slayer Tower Basement", false, false, false, true, false);
        assertLocation("wilderness-slayer-cave", "Wilderness Slayer Cave", true, true, true, false, true);
        assertLocation("abyssal-nexus", "Abyssal Nexus", false, false, false, false, false);
    }

    private static void assertLocation(String locationId, String name, boolean multi, boolean cannon, boolean burst,
        boolean safeSpot, boolean wilderness) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);
        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertEquals(burst, location.isBurst());
        assertEquals(safeSpot, location.isSafeSpot());
        assertEquals(wilderness, location.isWilderness());
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

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

public class GargoylesSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentUnlocksAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/gargoyles.json"), SourceTask.class);

        assertEquals(Integer.valueOf(256647), task.getWikiPageId());
        assertEquals(Integer.valueOf(80), task.getCombatLevel());
        assertEquals(221, task.getSlayerTargetId());
        assertEquals(75, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().contains("Priest in Peril"));

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
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("vannaka"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "gargoyle-smasher");
        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "double-trouble");
        assertContains(unlockIds(task), "get-smashed");
        assertContains(locationIds(task), "slayer-tower-top-floor");
        assertContains(locationIds(task), "slayer-tower-basement");
        assertContains(locationIds(task), "slayer-tower-roof");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("rock hammer") && note.contains("9 or lower")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Gargoyle Smasher") && note.contains("120")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("granite hammer") && note.contains("automatically smash")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("basement") && note.contains("on a Slayer task")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Marble gargoyle") && note.contains("ranged attacks")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("white-red orb") && note.contains("1x1")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("weak to both magic and crush")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Dual macuahuitl") && note.contains("-2 flat armour")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("brittle key") && note.contains("Grotesque Guardians")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Double Trouble") && note.contains("avoid")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/gargoyles.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, variants.size());
        assertVariant(variants, "gargoyle", 111, 105.0, "basement are immune to poison and venom");
        assertVariant(variants, "marble-gargoyle-superior", 349, 2768.0, "Bigger and Badder");
        assertVariant(variants, "gargoyles-grotesque-guardians-dawn", 228, 0.0, "Grotesque Guardians");
        assertVariant(variants, "grotesque-guardians-dusk", 248, 1350.0, "328");

        assertEquals(3, locations.size());
        assertLocation(locations, "slayer-tower-top-floor", 8, false, false, true, "banshees");
        assertLocation(locations, "slayer-tower-basement", 12, false, false, true, "only be killed here while on a Slayer task");
        assertLocation(locations, "slayer-tower-roof", 2, true, false, false, "brittle key");
    }

    @Test
    public void strategySourceContainsCommonSetupsInventoryAndUnlockGuidance() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/gargoyles/strategy.json");

        assertTrue("Gargoyles strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("gargoyles", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Gargoyles", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("scythe-of-vitur"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("granite-hammer"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("dual-macuahuitl"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "toxic-blowpipe".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "hunters-sunlight-crossbow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getNote().contains("rock hammer"));
        assertTrue(strategy.getPlugin().getNote().contains("basement"));

        assertContains(methodIds(strategy), "hammer-finisher");
        assertContains(methodIds(strategy), "basement-semi-afk");
        assertContains(methodIds(strategy), "melee-crush");
        assertContains(methodIds(strategy), "ranged-safespot");
        assertContains(methodIds(strategy), "marble-gargoyle");
        assertContains(methodIds(strategy), "grotesque-guardians-choice");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "hammer-finisher".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing hammer method"))
            .getSteps().stream().anyMatch(step -> step.contains("Gargoyle Smasher")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "basement-semi-afk".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing basement method"))
            .getSteps().stream().anyMatch(step -> step.contains("north") && step.contains("south-west")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "inventory-and-loot".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing inventory method"))
            .getInventory().stream().anyMatch(item -> item.contains("Divine rune pouch")));

        assertContains(styleIds(strategy), "melee-crush");
        assertContains(styleIds(strategy), "ranged");
    }

    @Test
    public void duskStrategyMarkdownIsMigratedToJson() throws IOException
    {
        assertFalse("Legacy Dusk Markdown strategy should be migrated to JSON",
            Files.exists(Paths.get("src/main/data/slayer/strategies/grotesque-guardians-dusk.md")));

        SourceStrategy dusk = read(Paths.get("src/main/data/slayer/strategies/grotesque-guardians-dusk/strategy.json"),
            SourceStrategy.class);

        assertEquals("grotesque-guardians-dusk", dusk.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies", dusk.getSourceUrl());
        assertEquals(CombatStyle.MELEE, dusk.getPlugin().getPrimaryStyle());
        assertTrue(dusk.getPlugin().getPrimaryWeapons().contains("scythe-of-vitur"));
        assertTrue(dusk.getPlugin().getNote().contains("immune to magic and ranged"));
        assertContains(methodIds(dusk), "melee-only");
        assertContains(methodIds(dusk), "phase-two-blinding-attack");
        assertContains(methodIds(dusk), "phase-four-prison-and-finish");
        assertFalse(dusk.getMethods().stream()
            .filter(method -> method.getNotes() != null)
            .flatMap(method -> method.getNotes().stream())
            .anyMatch(note -> note.contains("Source strategy:")));
    }

    @Test
    public void dawnStrategyJsonCoversGrotesqueGuardianPhases() throws IOException
    {
        SourceStrategy dawn = read(Paths.get("src/main/data/slayer/strategies/grotesque-guardians-dawn/strategy.json"),
            SourceStrategy.class);

        assertEquals("grotesque-guardians-dawn", dawn.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies", dawn.getSourceUrl());
        assertContains(methodIds(dawn), "phase-one-dawn-ranged");
        assertContains(methodIds(dawn), "phase-three-lightning-and-orbs");
        assertContains(methodIds(dawn), "hammer-finish");
        assertFalse(dawn.getMethods().stream()
            .filter(method -> method.getNotes() != null)
            .flatMap(method -> method.getNotes().stream())
            .anyMatch(note -> note.contains("Source strategy:")));
    }

    @Test
    public void generatedRuntimeDataCarriesGargoyleStrategyAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Gargoyles".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Gargoyles"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.getLocations().stream().anyMatch(location -> "Slayer Tower (top floor)".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Slayer Tower Basement".equals(location.getName())
            && !location.isMulti() && !location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Slayer Tower roof".equals(location.getName())
            && location.isMulti() && !location.isCannon()));

        MonsterVariant gargoyle = variantNamed(task, "Gargoyle");
        MonsterVariant marble = variantNamed(task, "Marble gargoyle (superior)");
        MonsterVariant dusk = variantNamed(task, "Grotesque Guardians - Dusk");

        assertNotNull(gargoyle.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Gargoyles", gargoyle.getStrategy().getSourceUrl());
        assertTrue(gargoyle.getStrategy().getNote().contains("rock hammer"));
        assertNotNull(marble.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Gargoyles", marble.getStrategy().getSourceUrl());
        assertNotNull(dusk.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies",
            dusk.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("slayer-tower-top-floor", "Slayer Tower (top floor)", false, false, true, false,
            "Gargoyles");
        assertLocationSource("slayer-tower-basement", "Slayer Tower Basement", false, false, true, false,
            "Gargoyles can only be killed here while on a Slayer task");
        assertLocationSource("slayer-tower-roof", "Slayer Tower roof", true, false, false, false,
            "brittle key");
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
        Integer combatLevel, double slayerXp, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertEquals(combatLevel, variant.getCombatLevel());
        assertEquals(slayerXp, variant.getSlayerXp().doubleValue(), 0.0);
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
    }

    private static void assertLocation(Map<String, SourceTaskLocationComparison> locations, String locationId,
        Integer amount, boolean multi, boolean cannon, boolean safespot, String noteText)
    {
        SourceTaskLocationComparison location = locations.get(locationId);

        assertNotNull("missing location comparison " + locationId, location);
        assertEquals(amount, location.getAmount());
        assertEquals(Boolean.valueOf(multi), location.getMulticombat());
        assertEquals(Boolean.valueOf(cannon), location.getCannonable());
        assertEquals(Boolean.valueOf(safespot), location.getSafespottable());
        assertTrue(location.getNotes().stream().anyMatch(note -> note.contains(noteText)));
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

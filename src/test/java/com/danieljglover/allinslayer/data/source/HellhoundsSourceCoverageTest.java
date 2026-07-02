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

public class HellhoundsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentMechanicsAndExceptions() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/hellhounds.json"), SourceTask.class);

        assertEquals(Integer.valueOf(256645), task.getWikiPageId());
        assertEquals(Integer.valueOf(75), task.getCombatLevel());
        assertEquals(224, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());
        assertEquals("water", task.getWeakness().getElement());
        assertTrue("Hellhound monster page marks hellhounds as demonic", task.isDemon());

        assertContains(masterIds(task), "krystilia");
        assertContains(masterIds(task), "vannaka");
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {75, 125}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {30, 60}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertTrue(task.getExtendedAmount() == null || task.getExtendedAmount().isEmpty());

        assertContains(locationIds(task), "buccaneers-laboratory-hellhounds");
        assertContains(locationIds(task), "catacombs-of-kourend");
        assertContains(locationIds(task), "charred-dungeon-hellhounds");
        assertContains(locationIds(task), "god-wars-dungeon");
        assertContains(locationIds(task), "karuulm-slayer-dungeon-hellhounds");
        assertContains(locationIds(task), "stronghold-slayer-cave");
        assertContains(locationIds(task), "taverley-dungeon");
        assertContains(locationIds(task), "witchaven-dungeon");
        assertContains(locationIds(task), "wilderness-resource-area-hellhounds");
        assertContains(locationIds(task), "wilderness-slayer-cave-hellhounds");
        assertContains(locationIds(task), "vetion-hellhound-spawns");
        assertContains(locationIds(task), "cerberus-lair");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("75 combat")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("melee bites") && note.contains("Protect from Melee")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("hard clue scrolls") && note.contains("1/64")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("1/32") && note.contains("imbued ring of wealth")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Stronghold Slayer Cave") && note.contains("dwarf multicannon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Catacombs") && note.contains("Venator Bow")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness Slayer Cave") && note.contains("player killing")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Revenant hellhounds") && note.contains("do not count")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Skeleton Hellhound") && note.contains("In Search of the Myreque")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Bouncer") && note.contains("Fight Arena")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ward of Arceuus") && note.contains("Cerberus")));
    }

    @Test
    public void taskSourceContainsWikiVariantRowsAndLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/hellhounds.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(8, variants.size());
        assertVariant(variants, "hellhound", 122, 116.0, "Standard Hellhounds");
        assertVariant(variants, "hellhound-god-wars-dungeon", 127, 116.0, "Zamorak");
        assertVariant(variants, "hellhound-wilderness-slayer-cave", 136, 150.0, "Wilderness Slayer Cave");
        assertVariant(variants, "skeleton-hellhound-vet-ion", 214, 33.0, "Vet'ion");
        assertVariant(variants, "greater-skeleton-hellhound-vet-ion", 281, 33.0, "second phase");
        assertVariant(variants, "skeleton-hellhound-calvar-ion", 115, 30.0, "Calvar'ion");
        assertVariant(variants, "greater-skeleton-hellhound-calvar-ion", 139, 30.0, "Calvar'ion's second phase");
        assertVariant(variants, "hellhounds-cerberus", 318, 690.0, "91 Slayer");

        assertEquals(11, locations.size());
        assertLocation(locations, "buccaneers-laboratory-hellhounds", 5, true, true, false, "76 Sailing");
        assertLocation(locations, "catacombs-of-kourend", 9, true, false, true, "Ancient shards");
        assertLocation(locations, "charred-dungeon-hellhounds", 5, true, true, false, "60 Sailing");
        assertLocation(locations, "god-wars-dungeon", 5, true, false, true, "Zamorak");
        assertLocation(locations, "karuulm-slayer-dungeon-hellhounds", 12, false, true, true, "boots of stone");
        assertLocation(locations, "stronghold-slayer-cave", 13, false, true, true, "Stronghold Slayer Dungeon");
        assertLocation(locations, "taverley-dungeon", 13, false, true, true, "80 Agility");
        assertLocation(locations, "witchaven-dungeon", 8, false, true, true, "short puzzle");
        assertLocation(locations, "wilderness-resource-area-hellhounds", 6, false, true, true, "high Wilderness");
        assertLocation(locations, "wilderness-slayer-cave-hellhounds", 9, true, true, true, "Wilderness weapons");
        assertLocation(locations, "vetion-hellhound-spawns", 4, true, true, false, "looting bag");
    }

    @Test
    public void variantSourcesUseTaskPageAndMonsterPageProfiles() throws IOException
    {
        SourceMonsterVariant regular = read(Paths.get("src/main/data/slayer/monsters/hellhounds/hellhound-lvl122.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant wilderness = read(Paths.get("src/main/data/slayer/monsters/hellhounds/hellhound-wilderness-slayer-cave-lvl136.json"),
            SourceMonsterVariant.class);
        SourceMonsterVariant godWars = read(Paths.get("src/main/data/slayer/monsters/hellhounds/hellhound-god-wars-dungeon-lvl127.json"),
            SourceMonsterVariant.class);

        assertEquals("hellhound", regular.getVariantId());
        assertEquals("water", regular.getWeakness().getElement());
        assertEquals(102, regular.getMonsterDefence().getDefenceLevel());
        assertEquals(0, regular.getMonsterDefence().getStab());
        assertEquals(0, regular.getMonsterDefence().getMagic());
        assertTrue(regular.isDemon());
        assertEquals("hellhounds", regular.getStrategyId());

        assertEquals("hellhound-wilderness-slayer-cave", wilderness.getVariantId());
        assertEquals(Integer.valueOf(136), wilderness.getCombatLevel());
        assertEquals("water", wilderness.getWeakness().getElement());
        assertEquals(102, wilderness.getMonsterDefence().getDefenceLevel());
        assertTrue(wilderness.isDemon());
        assertEquals("hellhounds", wilderness.getStrategyId());

        assertEquals("hellhound-god-wars-dungeon", godWars.getVariantId());
        assertEquals(Integer.valueOf(127), godWars.getCombatLevel());
        assertNull(godWars.getWeakness().getElement());
        assertEquals(106, godWars.getMonsterDefence().getDefenceLevel());
        assertTrue(godWars.isDemon());
        assertEquals("hellhounds", godWars.getStrategyId());
    }

    @Test
    public void strategySourceContainsTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/hellhounds/strategy.json");

        assertTrue("Hellhounds strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("hellhounds", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Hellhounds", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("emberlight"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("arclight"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "venator-bow".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.RANGED));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "trident-of-the-swamp".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("Protect from Melee"));
        assertTrue(strategy.getPlugin().getNote().contains("hard clue scroll"));

        assertContains(methodIds(strategy), "general-protection");
        assertContains(methodIds(strategy), "stronghold-cannon");
        assertContains(methodIds(strategy), "catacombs-afk-venator");
        assertContains(methodIds(strategy), "wilderness-slayer-cave");
        assertContains(methodIds(strategy), "safespots");
        assertContains(methodIds(strategy), "vetion-calvarion-spawns");
        assertContains(methodIds(strategy), "cerberus-choice");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "catacombs-afk-venator".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing catacombs method"))
            .getSteps().stream().anyMatch(step -> step.contains("Giants' Den")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "wilderness-slayer-cave".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing wilderness method"))
            .getRisks().stream().anyMatch(risk -> risk.contains("unwilling to lose")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "cerberus-choice".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing cerberus method"))
            .getRequiredOrKeyItems().stream().anyMatch(item -> item.contains("Ward of Arceuus")));

        assertContains(styleIds(strategy), "melee-demonbane");
        assertContains(styleIds(strategy), "ranged-venator");
        assertContains(styleIds(strategy), "magic-water");
    }

    @Test
    public void generatedRuntimeDataCarriesHellhoundSourcesAndStrategies() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Hellhounds".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Hellhounds"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "krystilia");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertTrue(task.isDemon());
        assertEquals(8, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Wilderness Slayer Cave".equals(location.getName())
            && location.isMulti() && location.isCannon() && location.isWilderness()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Cerberus' Lair".equals(location.getName())
            && !location.isCannon() && !location.isWilderness()));

        MonsterVariant hellhound = variantNamed(task, "Hellhound");
        MonsterVariant wilderness = variantNamed(task, "Hellhound (Wilderness Slayer Cave)");
        MonsterVariant cerberus = variantNamed(task, "Cerberus");
        MonsterVariant skeleton = variantNamed(task, "Skeleton Hellhound (Vet'ion)");

        assertNotNull(hellhound.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Hellhounds",
            hellhound.getStrategy().getSourceUrl());
        assertNotNull(wilderness.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Hellhounds",
            wilderness.getStrategy().getSourceUrl());
        assertNotNull(cerberus.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Cerberus/Strategies", cerberus.getStrategy().getSourceUrl());
        assertNotNull(skeleton.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Vet%27ion/Strategies", skeleton.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTaskPage() throws IOException
    {
        assertLocationSource("witchaven-dungeon", "Witchaven Dungeon", false, true, true, false, "short puzzle");
        assertLocationSource("wilderness-resource-area-hellhounds", "Wilderness near Resource Area", false, true,
            true, true, "high Wilderness");
        assertLocationSource("vetion-hellhound-spawns", "Wilderness near Vet'ion", true, true, false, true,
            "looting bag");
        assertLocationSource("cerberus-lair", "Cerberus' Lair", false, false, false, false, "91 Slayer");
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

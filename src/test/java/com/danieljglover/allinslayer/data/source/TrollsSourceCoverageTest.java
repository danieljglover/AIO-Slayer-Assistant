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

public class TrollsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiRequirementsAssignmentsAndMechanics() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/trolls.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298280), task.getWikiPageId());
        assertEquals(Integer.valueOf(60), task.getCombatLevel());
        assertEquals(235, task.getSlayerTargetId());
        assertEquals(1, task.getSlayerLevel());
        assertTrue(task.getQuestReqs().isEmpty());
        assertEquals(CombatStyle.MELEE, task.getWeakness().getStyle());

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

        assertContains(locationIds(task), "enchanted-valley-river-troll");
        assertContains(locationIds(task), "fremennik-isles-ice-trolls");
        assertContains(locationIds(task), "keldagrim-entrance-trolls");
        assertContains(locationIds(task), "mount-quidamortem-north-east-trolls");
        assertContains(locationIds(task), "mount-quidamortem-south-west-trolls");
        assertContains(locationIds(task), "death-plateau-trolls");
        assertContains(locationIds(task), "troll-stronghold-outside-trolls");

        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Fremennik Province")
            && note.contains("Burthorpe")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("quests")
            && note.contains("sea trolls")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Nightmare Zone")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("dwarf multicannon")
            && note.contains("low aggression range")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Neitiznot shield")
            && note.contains("ice troll females")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Sinister Offering")));
    }

    @Test
    public void taskSourceContainsAllVariantRowsAndLocationComparisonRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/trolls.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(21, variants.size());
        assertVariant(variants, "river-troll", "14-159", "severely reduced experience");
        assertVariant(variants, "thrower-troll", "67", "always successful");
        assertVariant(variants, "thrower-troll-trollheim", "68", "only drop bones");
        assertVariant(variants, "mountain-troll", "69, 71", "Fire and Earth");
        assertVariant(variants, "troll-spectator", "71", "Troll arena");
        assertVariant(variants, "twig", "71", "cell key 1");
        assertVariant(variants, "berry", "71", "cell key 2");
        assertVariant(variants, "ice-troll-runt", "74", "guards help");
        assertVariant(variants, "ice-troll-male", "82", "melee");
        assertVariant(variants, "ice-troll-female", "82", "Ranged");
        assertVariant(variants, "pee-hat", "91", "rune warhammer");
        assertVariant(variants, "kraka", "91", "granite shield");
        assertVariant(variants, "dad", "101 (201)", "Nightmare Zone");
        assertVariant(variants, "ice-troll-grunt", "100", "Neitiznot shield");
        assertVariant(variants, "stick", "104", "safespotted");
        assertVariant(variants, "rock-troll", "111", "biggest and baddest");
        assertVariant(variants, "troll-general", "113", "near-nonexistent drops");
        assertVariant(variants, "arrg", "113 (210)", "Troll Romance");
        assertVariant(variants, "ice-troll", "120, 121, 123, 124", "stat drain");
        assertVariant(variants, "ice-troll-king", "122 (213)", "all three attack styles");
        assertVariant(variants, "reanimated-troll", "N/A", "Adept Reanimation");

        assertEquals(7, locations.size());
        assertLocation(locations, "enchanted-valley-river-troll", 1, false, false, false, "River troll");
        assertLocation(locations, "fremennik-isles-ice-trolls", null, true, true, true, "Better drops");
        assertLocation(locations, "keldagrim-entrance-trolls", 7, false, true, true, "Narrow passage");
        assertLocation(locations, "mount-quidamortem-north-east-trolls", 7, true, true, true, "More open");
        assertLocation(locations, "mount-quidamortem-south-west-trolls", 7, true, true, true, "obstacles");
        assertLocation(locations, "death-plateau-trolls", 15, false, true, true, "Protect from Missiles");
        assertLocation(locations, "troll-stronghold-outside-trolls", 8, false, true, true,
            "Bonecrusher necklace");
    }

    @Test
    public void variantSourcesUseCurrentMonsterPageStatsAndSharedStrategyId() throws IOException
    {
        SourceMonsterVariant mountain = readVariant("mountain-troll-lvl69.json");
        SourceMonsterVariant ice = readVariant("ice-troll-lvl120.json");
        SourceMonsterVariant thrower = readVariant("thrower-troll-lvl67.json");
        SourceMonsterVariant female = readVariant("ice-troll-female-lvl82.json");
        SourceMonsterVariant king = readVariant("ice-troll-king-lvl122.json");
        SourceMonsterVariant reanimated = readVariant("reanimated-troll.json");

        assertEquals("fire", mountain.getWeakness().getElement());
        assertDefence(mountain, 40, 0, 0, 10, 200, 40);
        assertEquals("trolls", mountain.getStrategyId());

        assertEquals("fire", ice.getWeakness().getElement());
        assertDefence(ice, 120, 30, 60, 30, 0, 0);
        assertEquals("trolls", ice.getStrategyId());

        assertEquals(Integer.valueOf(67), thrower.getCombatLevel());
        assertDefence(thrower, 30, 0, 0, 0, 200, 120);
        assertEquals("trolls", thrower.getStrategyId());

        assertEquals(CombatStyle.MAGIC, female.getWeakness().getStyle());
        assertEquals("fire", female.getWeakness().getElement());
        assertTrue(female.getLocation().contains("Jatizso"));

        assertTrue(king.isBoss());
        assertDefence(king, 80, 45, 45, 45, 2000, 2000);
        assertEquals("trolls", king.getStrategyId());

        assertEquals(Integer.valueOf(7030), reanimated.getNpcIds().get(0));
        assertEquals("Adept Reanimation", reanimated.getRequirement());
    }

    @Test
    public void strategyJsonCoversTaskPageMethodsAndStyleOptions() throws IOException
    {
        Path json = Paths.get("src/main/data/slayer/strategies/trolls/strategy.json");

        assertTrue("Trolls strategy JSON missing", Files.exists(json));

        SourceStrategy strategy = read(json, SourceStrategy.class);

        assertEquals("trolls", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Trolls", strategy.getSourceUrl());
        assertEquals(CombatStyle.MELEE, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("soulreaper-axe"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("abyssal-whip"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "kodai-wand".equals(weapon.getWeaponId()) && weapon.getStyle() == CombatStyle.MAGIC));
        assertTrue(strategy.getPlugin().getNote().contains("dwarf multicannon"));
        assertTrue(strategy.getPlugin().getNote().contains("Neitiznot shield"));

        assertContains(methodIds(strategy), "requirements-and-access");
        assertContains(methodIds(strategy), "mountain-trolls-melee-cannon");
        assertContains(methodIds(strategy), "ice-trolls-melee-cannon");
        assertContains(methodIds(strategy), "fire-magic");
        assertContains(methodIds(strategy), "ice-troll-runt-quick-task");
        assertContains(methodIds(strategy), "quest-bosses-and-reanimation");
        assertContains(methodIds(strategy), "inventory-and-loot");
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "ice-trolls-melee-cannon".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing ice troll melee method"))
            .getSteps().stream().anyMatch(step -> step.contains("Neitiznot shield")));
        assertTrue(strategy.getMethods().stream()
            .filter(method -> "fire-magic".equals(method.getMethodId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing fire magic method"))
            .getSteps().stream().anyMatch(step -> step.contains("100%") && step.contains("fire")));

        assertContains(styleIds(strategy), "melee-low-level");
        assertContains(styleIds(strategy), "melee-mountain-trolls");
        assertContains(styleIds(strategy), "melee-ice-trolls");
        assertContains(styleIds(strategy), "magic-fire-spells");
    }

    @Test
    public void generatedRuntimeDataCarriesTrollStrategyAndLocations() throws IOException
    {
        TaskData task = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
            .filter(t -> "Trolls".equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Trolls"));

        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "vannaka");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "chaeldar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "konar");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "nieve");
        assertContains(task.getAssignedBy().stream().collect(Collectors.toSet()), "duradel");
        assertEquals(20, task.getVariants().size());
        assertTrue(task.getLocations().stream().anyMatch(location -> "Northern Jatizso and Neitiznot"
            .equals(location.getName()) && location.isMulti() && location.isCannon()));
        assertTrue(task.getLocations().stream().anyMatch(location -> "Death Plateau".equals(location.getName())
            && !location.isMulti() && location.isCannon()));

        MonsterVariant mountain = variantNamed(task, "Mountain troll");
        MonsterVariant king = variantNamed(task, "Ice Troll King");

        assertNotNull(mountain.getStrategy());
        assertEquals(CombatStyle.MELEE, mountain.getStrategy().getPrimaryStyle());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Trolls",
            mountain.getStrategy().getSourceUrl());
        assertNotNull(king.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Trolls",
            king.getStrategy().getSourceUrl());
    }

    @Test
    public void locationSourcesMatchTrollsTaskPage() throws IOException
    {
        assertLocationSource("enchanted-valley-river-troll", "Enchanted Valley", false, false, false, false, false,
            "Fairy ring BKQ");
        assertLocationSource("fremennik-isles-ice-trolls", "Northern Jatizso and Neitiznot", true, true, true, true,
            false, "Neitiznot shield");
        assertLocationSource("keldagrim-entrance-trolls", "Keldagrim entrance tunnel", false, true, true, true, false,
            "level 69");
        assertLocationSource("mount-quidamortem-north-east-trolls", "South of Mount Quidamortem north-east",
            true, true, true, true, false, "More open");
        assertLocationSource("mount-quidamortem-south-west-trolls", "South of Mount Quidamortem south-west",
            true, true, true, true, false, "obstacles");
        assertLocationSource("death-plateau-trolls", "Death Plateau", false, true, true, true, false,
            "Protect from Missiles");
        assertLocationSource("troll-stronghold-outside-trolls", "Troll Stronghold outside", false, true, true,
            true, false, "Bonecrusher necklace");
    }

    private static SourceMonsterVariant readVariant(String fileName) throws IOException
    {
        return read(Paths.get("src/main/data/slayer/monsters/trolls/" + fileName), SourceMonsterVariant.class);
    }

    private static void assertVariant(Map<String, SourceTaskVariantInfo> variants, String variantId,
        String combatLevel, String noteText)
    {
        SourceTaskVariantInfo variant = variants.get(variantId);

        assertNotNull("missing variant " + variantId, variant);
        assertTrue(String.valueOf(variant.getCombatLevel()).contains(combatLevel)
            || variant.getNotes().stream().anyMatch(note -> note.contains(combatLevel)));
        assertTrue(variant.getNotes().stream().anyMatch(note -> note.contains(noteText)));
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

    private static void assertLocationSource(String locationId, String name, boolean multi, boolean cannon,
        boolean safeSpot, boolean konarLockable, boolean wilderness, String accessText) throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/" + locationId + ".json"),
            SourceLocation.class);

        assertEquals(locationId, location.getLocationId());
        assertEquals(name, location.getName());
        assertEquals(multi, location.isMulti());
        assertEquals(cannon, location.isCannon());
        assertFalse(location.isBurst());
        assertEquals(konarLockable, location.isKonarLockable());
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

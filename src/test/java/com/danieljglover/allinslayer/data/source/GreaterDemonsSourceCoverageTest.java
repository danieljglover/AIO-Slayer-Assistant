package com.danieljglover.allinslayer.data.source;

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

public class GreaterDemonsSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsCurrentWikiAssignmentsAndNotes() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/greater-demons.json"), SourceTask.class);

        assertEquals(Integer.valueOf(298108), task.getWikiPageId());
        assertEquals(Integer.valueOf(70), task.getCombatLevel());
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertContains(masterIds(task), "krystilia");
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {100, 150}, task.getAmountByMaster().get("krystilia"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("chaeldar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("konar"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("nieve"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("krystilia"));

        assertNotNull("taskNotes missing", task.getTaskNotes());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Chasm of Fire")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Wilderness Slayer Cave")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Tormented Demon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("K'ril Tsutsaroth")));
    }

    @Test
    public void taskSourceContainsCurrentWikiVariantAndLocationRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/greater-demons.json"), SourceTask.class);
        assertNotNull("variantInfo missing", task.getVariantInfo());
        assertNotNull("locationComparison missing", task.getLocationComparison());

        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(9, variants.size());
        assertVariant(variants, "greater-demon", 92, 87, "regular level 92");
        assertVariant(variants, "greater-demon-level-100", 100, 115, "Catacombs");
        assertVariant(variants, "greater-demon-level-101", 101, 120, "Catacombs");
        assertVariant(variants, "greater-demon-level-104", 104, 120, "Wilderness Slayer Cave");
        assertVariant(variants, "greater-demon-level-113", 113, 130, "Catacombs");
        assertVariant(variants, "skotizo", 321, 618.5, "boss");
        assertVariant(variants, "tormented-demon", 450, 1065, "While Guthix Sleeps");
        assertVariant(variants, "tstanon-karlak", 145, 142, "bodyguard");
        assertVariant(variants, "greater-demons-k-ril-tsutsaroth", 650, 350.5, "God Wars Dungeon");

        assertEquals(10, locations.size());
        assertLocation(locations, "brimhaven-dungeon", 7, false, true, true, "Axe");
        assertLocation(locations, "chasm-of-fire-bottom", 16, false, true, true, "Fairy Ring DJR");
        assertLocation(locations, "catacombs-of-kourend", 10, true, false, true, "ancient shard");
        assertLocation(locations, "entrana-dungeon", 2, false, true, true, "not advisable");
        assertLocation(locations, "lava-maze-dungeon", 3, false, true, true, "82 Agility");
        assertLocation(locations, "demonic-ruins", 4, true, true, true, "deep in the Wilderness");
        assertLocation(locations, "ogre-enclave-watchtower", 5, false, true, true, "behind a cage");
        assertLocation(locations, "isle-of-souls-dungeon", 7, false, true, true, "lesser demon");
        assertLocation(locations, "karuulm-slayer-dungeon-greater-demons", 9, false, true, true, "Stone Boots");
        assertLocation(locations, "wilderness-slayer-cave", 10, true, true, true, "Viggora");
    }

    @Test
    public void assignedVariantsDeclareStrategies() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/greater-demons.json"), SourceTask.class);

        for (String variantId : task.getVariantIds())
        {
            SourceMonsterVariant variant = readVariant(variantId);

            assertNotNull("variant should declare strategyId: " + variantId, variant.getStrategyId());
        }
    }

    private static SourceMonsterVariant readVariant(String variantId) throws IOException
    {
        Path root = Paths.get("src/main/data/slayer/monsters");

        try (java.util.stream.Stream<Path> files = Files.walk(root))
        {
            Path match = files
                .filter(path -> path.toString().endsWith(".json"))
                .filter(path -> {
                    try
                    {
                        return variantId.equals(read(path, SourceMonsterVariant.class).getVariantId());
                    }
                    catch (IOException e)
                    {
                        throw new IllegalStateException(e);
                    }
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing variant: " + variantId));

            return read(match, SourceMonsterVariant.class);
        }
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

    private static Set<String> masterIds(SourceTask task)
    {
        return task.getMasterIds().stream().collect(Collectors.toSet());
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

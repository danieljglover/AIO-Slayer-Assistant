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

public class LizardmenSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsCurrentWikiAssignmentsAndVariantRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lizardmen.json"), SourceTask.class);

        assertEquals(Integer.valueOf(297852), task.getWikiPageId());
        assertContains(masterIds(task), "chaeldar");
        assertContains(masterIds(task), "konar");
        assertContains(masterIds(task), "nieve");
        assertContains(masterIds(task), "duradel");
        assertArrayEquals(new int[] {50, 90}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {90, 110}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {90, 120}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 210}, task.getAmountByMaster().get("duradel"));

        assertNotNull("variantInfo missing", task.getVariantInfo());
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));

        assertEquals(3, variants.size());
        assertVariant(variants, "lizardman", 53, 60, "level 53 and 62");
        assertVariant(variants, "lizardman-brute", 75, 60, "brute");
        assertVariant(variants, "lizardman-shaman", 150, 157.5, "shaman");

        assertNotNull("taskNotes missing", task.getTaskNotes());
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Reptile got ripped")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Chaeldar")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Lizardman Canyon")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("shaman")));
    }

    @Test
    public void assignedVariantsDeclareStrategies() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/lizardmen.json"), SourceTask.class);

        for (String variantId : task.getVariantIds())
        {
            SourceMonsterVariant variant = readVariant(variantId);

            assertNotNull("variant should declare strategyId: " + variantId, variant.getStrategyId());
        }
    }

    @Test
    public void strategyJsonSeparatesNormalBruteAndShamanMethods() throws IOException
    {
        SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies/lizardmen/strategy.json"),
            SourceStrategy.class);
        Set<String> methodIds = strategy.getMethods().stream()
            .map(SourceStrategyMethod::getMethodId)
            .collect(Collectors.toSet());

        assertContains(methodIds, "normal-canyon-task");
        assertContains(methodIds, "brute-task");
        assertContains(methodIds, "shaman-dragon-warhammer-route");
    }

    private static SourceMonsterVariant readVariant(String variantId) throws IOException
    {
        Path root = Paths.get("src/main/data/slayer/monsters/lizardmen");

        try (java.util.stream.Stream<Path> files = Files.list(root))
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

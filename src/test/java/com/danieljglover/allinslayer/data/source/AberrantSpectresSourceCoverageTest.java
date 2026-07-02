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
import static org.junit.Assert.assertTrue;

public class AberrantSpectresSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskSourceContainsWikiAssignmentAndUnlockData() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/aberrant-spectres.json"), SourceTask.class);

        assertEquals(Integer.valueOf(65), task.getCombatLevel());
        assertEquals(Integer.valueOf(298029), task.getWikiPageId());
        assertEquals(200, task.getSlayerTargetId());
        assertTrue(task.getQuestReqs().contains("Priest in Peril"));
        assertTrue(task.getMasterIds().contains("vannaka"));
        assertTrue(task.getMasterIds().contains("chaeldar"));
        assertTrue(task.getMasterIds().contains("konar"));
        assertTrue(task.getMasterIds().contains("nieve"));
        assertTrue(task.getMasterIds().contains("duradel"));
        assertArrayEquals(new int[] {40, 90}, task.getAmountByMaster().get("vannaka"));
        assertArrayEquals(new int[] {70, 130}, task.getAmountByMaster().get("chaeldar"));
        assertArrayEquals(new int[] {120, 170}, task.getAmountByMaster().get("konar"));
        assertArrayEquals(new int[] {120, 185}, task.getAmountByMaster().get("nieve"));
        assertArrayEquals(new int[] {130, 200}, task.getAmountByMaster().get("duradel"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("vannaka"));
        assertArrayEquals(new int[] {200, 250}, task.getExtendedAmount().get("duradel"));

        assertContains(unlockIds(task), "bigger-and-badder");
        assertContains(unlockIds(task), "smell-ya-later");
        assertTrue(task.getLocationIds().contains("deepfin-mine"));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("nose peg")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Protect from Magic")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("herb sack")));
        assertTrue(task.getTaskNotes().stream().anyMatch(note -> note.contains("Ectoplasmator")));
    }

    @Test
    public void taskSourceContainsWikiVariantRows() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/aberrant-spectres.json"), SourceTask.class);
        Map<String, SourceTaskVariantInfo> variants = task.getVariantInfo().stream()
            .collect(Collectors.toMap(SourceTaskVariantInfo::getVariantId, v -> v));

        assertEquals(4, variants.size());
        assertEquals(90.0, variants.get("aberrant-spectre").getSlayerXp().doubleValue(), 0.0);
        assertEquals(2500.0, variants.get("abhorrent-spectre").getSlayerXp().doubleValue(), 0.0);
        assertEquals(194.5, variants.get("deviant-spectre").getSlayerXp().doubleValue(), 0.0);
        assertEquals(4085.0, variants.get("repugnant-spectre").getSlayerXp().doubleValue(), 0.0);
        assertTrue(variants.get("abhorrent-spectre").getNotes().get(0).contains("Superior"));
        assertTrue(variants.get("repugnant-spectre").getLocations().contains("Catacombs of Kourend"));
    }

    @Test
    public void taskSourceContainsWikiLocationComparison() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/aberrant-spectres.json"), SourceTask.class);
        Map<String, SourceTaskLocationComparison> locations = task.getLocationComparison().stream()
            .collect(Collectors.toMap(SourceTaskLocationComparison::getLocationId, l -> l));

        assertEquals(4, locations.size());
        assertEquals(Integer.valueOf(14), locations.get("slayer-tower").getAmount());
        assertEquals(Boolean.FALSE, locations.get("slayer-tower").getCannonable());
        assertEquals(Boolean.TRUE, locations.get("slayer-tower").getSafespottable());
        assertTrue(locations.get("slayer-tower").getNotes().stream()
            .anyMatch(note -> note.contains("Morytania Diary")));

        assertEquals(Integer.valueOf(13), locations.get("stronghold-slayer-cave").getAmount());
        assertEquals(Boolean.TRUE, locations.get("stronghold-slayer-cave").getCannonable());
        assertEquals(Boolean.FALSE, locations.get("stronghold-slayer-cave").getMulticombat());

        assertEquals(Integer.valueOf(12), locations.get("catacombs-of-kourend").getAmount());
        assertEquals(Boolean.TRUE, locations.get("catacombs-of-kourend").getMulticombat());
        assertEquals(Boolean.FALSE, locations.get("catacombs-of-kourend").getCannonable());

        assertEquals(Integer.valueOf(7), locations.get("deepfin-mine").getAmount());
        assertEquals(Boolean.TRUE, locations.get("deepfin-mine").getCannonable());
        assertTrue(locations.get("deepfin-mine").getNotes().get(0).contains("standing torch"));
    }

    @Test
    public void deepfinMineLocationSourceExists() throws IOException
    {
        SourceLocation location = read(Paths.get("src/main/data/slayer/locations/deepfin-mine.json"),
            SourceLocation.class);

        assertEquals("deepfin-mine", location.getLocationId());
        assertEquals("Deepfin Mine", location.getName());
        assertEquals(false, location.isMulti());
        assertEquals(true, location.isCannon());
        assertEquals(true, location.isSafeSpot());
        assertTrue(location.getAccessNote().contains("bank chest"));
    }

    private static Set<String> unlockIds(SourceTask task)
    {
        return task.getUnlocks().stream().map(SourceTaskUnlock::getUnlockId).collect(Collectors.toSet());
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

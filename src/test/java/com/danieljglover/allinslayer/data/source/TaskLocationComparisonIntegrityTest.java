package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * GAP-5 (data-audit.md): {@code locationComparison[].locationId} is treated as a foreign key into
 * {@code locations/*.json}, but the compiler's validator never checks it. spiritual-creatures shipped 9
 * rows pointing at non-existent location IDs. This pins the invariant so a broken FK cannot silently
 * return (it is a prerequisite for any future work that reads locationComparison, per GAP-1/GAP-2).
 */
public class TaskLocationComparisonIntegrityTest
{
    private static final Path ROOT = Paths.get("src/main/data/slayer");
    private static final Gson GSON = new Gson();

    @Test
    public void everyLocationComparisonLocationIdResolvesToALocationFile() throws IOException
    {
        Set<String> locationIds = loadLocationIds();
        List<String> broken = new ArrayList<>();
        for (Path taskFile : jsonFiles(ROOT.resolve("tasks")))
        {
            SourceTask task = read(taskFile, SourceTask.class);
            if (task == null || task.getLocationComparison() == null)
            {
                continue;
            }
            for (SourceTaskLocationComparison row : task.getLocationComparison())
            {
                String id = row == null ? null : row.getLocationId();
                if (id != null && !locationIds.contains(id))
                {
                    broken.add(task.getTaskId() + " -> " + id);
                }
            }
        }
        assertTrue("locationComparison.locationId values must resolve to a location file, broken: "
            + broken, broken.isEmpty());
    }

    private static Set<String> loadLocationIds() throws IOException
    {
        Set<String> ids = new HashSet<>();
        for (Path file : jsonFiles(ROOT.resolve("locations")))
        {
            SourceLocation location = read(file, SourceLocation.class);
            if (location != null && location.getLocationId() != null)
            {
                ids.add(location.getLocationId());
            }
        }
        return ids;
    }

    private static List<Path> jsonFiles(Path dir) throws IOException
    {
        try (Stream<Path> paths = Files.list(dir))
        {
            return paths
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    private static <T> T read(Path file, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

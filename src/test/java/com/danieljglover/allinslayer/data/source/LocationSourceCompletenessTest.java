package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * GAP-9 (data-audit.md): 12 location files shipped without {@code safeSpot}/{@code wilderness}/
 * {@code accessNote}. {@code safeSpot} and {@code wilderness} are primitive booleans on
 * {@link SourceLocation}, so a missing key silently compiles to {@code false} rather than surfacing;
 * this test reads the raw JSON key set so an unauthored field is caught explicitly.
 */
public class LocationSourceCompletenessTest
{
    private static final Path LOCATIONS = Paths.get("src/main/data/slayer/locations");
    private static final Gson GSON = new Gson();

    @Test
    public void everyLocationFileAuthorsSafeSpotWildernessAndAccessNote() throws IOException
    {
        List<String> missing = new ArrayList<>();
        for (Path file : jsonFiles(LOCATIONS))
        {
            JsonObject obj;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
            {
                obj = GSON.fromJson(reader, JsonObject.class);
            }
            String name = file.getFileName().toString();
            for (String key : new String[] {"safeSpot", "wilderness", "accessNote"})
            {
                if (obj == null || !obj.has(key) || obj.get(key).isJsonNull())
                {
                    missing.add(name + " -> " + key);
                }
            }
        }
        assertTrue("location files must author safeSpot/wilderness/accessNote, missing: " + missing,
            missing.isEmpty());
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
}

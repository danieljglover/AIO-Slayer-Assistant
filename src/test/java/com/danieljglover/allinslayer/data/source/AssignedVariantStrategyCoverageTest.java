package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class AssignedVariantStrategyCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void assignedMonsterVariantsHaveStrategyLinks() throws IOException
    {
        Set<String> assignedVariantIds = new LinkedHashSet<>();
        try (java.util.stream.Stream<Path> paths = Files.list(Paths.get("src/main/data/slayer/tasks")))
        {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".json"))::iterator)
            {
                SourceTask task = read(path, SourceTask.class);
                assignedVariantIds.addAll(task.getVariantIds());
            }
        }

        Map<String, String> missing = new LinkedHashMap<>();
        try (java.util.stream.Stream<Path> paths = Files.walk(Paths.get("src/main/data/slayer/monsters")))
        {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".json"))::iterator)
            {
                SourceMonsterVariant variant = read(path, SourceMonsterVariant.class);
                if (assignedVariantIds.contains(variant.getVariantId())
                    && (variant.getStrategyId() == null || variant.getStrategyId().trim().isEmpty()))
                {
                    missing.put(variant.getVariantId(), path.toString());
                }
            }
        }

        assertTrue("Assigned variants missing strategyId: " + missing, missing.isEmpty());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

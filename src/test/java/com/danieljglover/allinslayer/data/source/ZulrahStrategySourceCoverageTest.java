package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZulrahStrategySourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void serpentineGreenFormModelsMagicAndRotationPrayerHandling() throws IOException
    {
        SourceStrategy strategy = read("zulrah-serpentine-green-form");
        Set<String> methodIds = methodIds(strategy);

        assertTrue(methodIds.contains("green-fire-magic"));
        assertTrue(methodIds.contains("jad-rotation-prayers"));
        assertTrue(methodIds.contains("venom-and-snakelings"));
        assertNoSourcePlaceholderNotes(strategy);
    }

    @Test
    public void tanzaniteBlueFormModelsRangedAndDefensiveHandling() throws IOException
    {
        SourceStrategy strategy = read("zulrah-tanzanite-blue-form");
        Set<String> methodIds = methodIds(strategy);

        assertTrue(methodIds.contains("blue-ranged"));
        assertTrue(methodIds.contains("hide-or-tank-blue-phase"));
        assertTrue(methodIds.contains("venom-and-snakelings"));
        assertNoSourcePlaceholderNotes(strategy);
    }

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream()
            .map(SourceStrategyMethod::getMethodId)
            .collect(Collectors.toSet());
    }

    private static void assertNoSourcePlaceholderNotes(SourceStrategy strategy)
    {
        assertFalse(strategy.getStrategyId(), strategy.getMethods().stream()
            .filter(method -> method.getNotes() != null)
            .flatMap(method -> method.getNotes().stream())
            .anyMatch(note -> note.contains("Source strategy:")));
    }

    private static SourceStrategy read(String strategyId) throws IOException
    {
        Path path = Paths.get("src/main/data/slayer/strategies", strategyId, "strategy.json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, SourceStrategy.class);
        }
    }
}

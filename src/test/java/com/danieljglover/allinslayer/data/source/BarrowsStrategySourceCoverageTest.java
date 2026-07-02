package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BarrowsStrategySourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void barrowsBrotherStrategiesModelSharedBarrowsMethods() throws IOException
    {
        for (String strategyId : Arrays.asList("ahrim-the-blighted", "dharok-the-wretched",
            "guthan-the-infested", "karil-the-tainted", "torag-the-corrupted", "verac-the-defiled"))
        {
            SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies", strategyId, "strategy.json"));
            Set<String> methodIds = strategy.getMethods().stream()
                .map(SourceStrategyMethod::getMethodId)
                .collect(Collectors.toSet());

            assertTrue(strategyId, methodIds.contains("barrows-route"));
            assertTrue(strategyId, methodIds.contains("protection-prayer-kill"));
            assertTrue(strategyId, methodIds.contains("tunnel-safespot"));
            assertFalse(strategyId, strategy.getMethods().stream()
                .filter(method -> method.getNotes() != null)
                .flatMap(method -> method.getNotes().stream())
                .anyMatch(note -> note.contains("Source strategy:")));
        }
    }

    @Test
    public void barrowsBrotherStrategiesKeepPerBrotherCombatStyleOptions() throws IOException
    {
        assertHasStyle("ahrim-the-blighted", "ranged");
        assertHasStyle("ahrim-the-blighted", "magic-secondary");
        assertHasStyle("karil-the-tainted", "melee-secondary");
        assertHasStyle("verac-the-defiled", "stab-melee");
        assertHasStyle("dharok-the-wretched", "magic");
        assertHasStyle("guthan-the-infested", "magic");
        assertHasStyle("torag-the-corrupted", "magic");
    }

    private static void assertHasStyle(String strategyId, String styleId) throws IOException
    {
        SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies", strategyId, "strategy.json"));
        Set<String> styleIds = strategy.getStyleOptions().stream()
            .map(SourceStrategyStyleOption::getStyleId)
            .collect(Collectors.toSet());

        assertTrue(strategyId + " styles " + styleIds, styleIds.contains(styleId));
    }

    private static SourceStrategy read(Path path) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, SourceStrategy.class);
        }
    }
}

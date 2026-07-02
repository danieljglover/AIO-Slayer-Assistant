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

public class KalphiteQueenStrategySourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void kalphiteQueenStrategyModelsTravelPhasesAndFlinching() throws IOException
    {
        SourceStrategy strategy = read("kalphite-queen");
        Set<String> methodIds = methodIds(strategy);

        assertTrue(methodIds.contains("travel-and-access"));
        assertTrue(methodIds.contains("two-phase-main-kill"));
        assertTrue(methodIds.contains("guardian-flinch-setup"));
        assertNoSourcePlaceholderNotes(strategy);
    }

    @Test
    public void phaseSpecificStrategiesKeepPhaseMethodsAndNoSourcePlaceholders() throws IOException
    {
        SourceStrategy crawling = read("kalphite-queen-crawling-form");
        SourceStrategy airborne = read("kalphite-queen-airborne-form");

        assertTrue(methodIds(crawling).contains("phase-one-melee"));
        assertTrue(methodIds(crawling).contains("phase-one-defence-spec"));
        assertTrue(methodIds(airborne).contains("phase-two-magic-ranged"));
        assertTrue(methodIds(airborne).contains("phase-two-walk-under"));
        assertNoSourcePlaceholderNotes(crawling);
        assertNoSourcePlaceholderNotes(airborne);
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

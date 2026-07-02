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

public class StrategyJsonSourceCoverageTest
{
    @Test
    public void krilStrategyIsJsonAndContainsAllWikiStrategyMethods() throws IOException
    {
        Path legacyMarkdown = Paths.get("src/main/data/slayer/strategies/k-ril-tsutsaroth.md");
        Path json = Paths.get("src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json");

        assertFalse("K'ril strategy should be migrated away from Markdown", Files.exists(legacyMarkdown));
        assertTrue("K'ril strategy JSON missing", Files.exists(json));

        SourceStrategy strategy;
        try (Reader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8))
        {
            strategy = new Gson().fromJson(reader, SourceStrategy.class);
        }

        assertContains(methodIds(strategy), "general");
        assertContains(methodIds(strategy), "solo-scorching-bow");
        assertContains(methodIds(strategy), "solo-shadow-5-0");
        assertContains(methodIds(strategy), "tank-protect-from-melee");
        assertContains(methodIds(strategy), "tank-protect-from-magic");
        assertContains(methodIds(strategy), "team-attacker");

        assertContains(styleIds(strategy), "ranged");
        assertContains(styleIds(strategy), "solo-melee");
        assertContains(styleIds(strategy), "melee-tank");
        assertContains(styleIds(strategy), "melee");
        assertContains(styleIds(strategy), "magic");
    }

    private static Set<String> methodIds(SourceStrategy strategy)
    {
        return strategy.getMethods().stream()
            .map(SourceStrategyMethod::getMethodId)
            .collect(Collectors.toSet());
    }

    private static Set<String> styleIds(SourceStrategy strategy)
    {
        return strategy.getStyleOptions().stream()
            .map(SourceStrategyStyleOption::getStyleId)
            .collect(Collectors.toSet());
    }

    private static void assertContains(Set<String> values, String expected)
    {
        assertTrue("expected " + values + " to contain " + expected, values.contains(expected));
    }
}

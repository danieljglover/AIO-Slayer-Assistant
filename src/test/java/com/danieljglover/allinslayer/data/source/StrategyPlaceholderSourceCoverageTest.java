package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class StrategyPlaceholderSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void strategyJsonFilesDoNotUseGeneratedWikiPlaceholderText() throws IOException
    {
        List<String> failures = new ArrayList<>();

        try (java.util.stream.Stream<Path> paths = Files.walk(Paths.get("src/main/data/slayer/strategies")))
        {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.getFileName().toString().equals("strategy.json"))::iterator)
            {
                String raw = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                SourceStrategy strategy = read(path);
                int methodCount = strategy.getMethods() == null ? 0 : strategy.getMethods().size();

                if (methodCount < 2)
                {
                    failures.add(path + " has only " + methodCount + " method(s)");
                }
                if (raw.contains("Source strategy:")
                    || raw.contains("This JSON preserves")
                    || raw.contains("Review the linked")
                    || raw.contains("boss-specific positioning, phase, and inventory adjustments"))
                {
                    failures.add(path + " contains generated placeholder wording");
                }
            }
        }

        assertTrue("Strategy placeholder data remains: " + failures, failures.isEmpty());
    }

    private static SourceStrategy read(Path path) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, SourceStrategy.class);
        }
    }
}

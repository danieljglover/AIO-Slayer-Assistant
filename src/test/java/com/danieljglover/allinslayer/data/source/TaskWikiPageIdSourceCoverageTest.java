package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TaskWikiPageIdSourceCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void taskWikiPageIdsMatchCurrentOsrsWikiSourcePages() throws IOException
    {
        Map<String, Integer> expected = expectedWikiPageIds();

        for (Map.Entry<String, Integer> entry : expected.entrySet())
        {
            SourceTask task = read(Paths.get("src/main/data/slayer/tasks/" + entry.getKey() + ".json"));

            assertEquals(entry.getKey(), entry.getValue(), task.getWikiPageId());
        }
    }

    private static Map<String, Integer> expectedWikiPageIds()
    {
        Map<String, Integer> ids = new LinkedHashMap<>();

        ids.put("aberrant-spectres", 298029);
        ids.put("abyssal-demons", 298016);
        ids.put("ankou", 357194);
        ids.put("aquanites", 630114);
        ids.put("araxytes", 529981);
        ids.put("aviansie", 342833);
        ids.put("basilisks", 256653);
        ids.put("black-demons", 298036);
        ids.put("black-dragons", 271133);
        ids.put("bloodveld", 297737);
        ids.put("blue-dragons", 297956);
        ids.put("boss", null);
        ids.put("cave-horrors", 298271);
        ids.put("cave-kraken", 357207);
        ids.put("dagannoth", 298104);
        ids.put("dark-beasts", 72120);
        ids.put("drakes", 267357);
        ids.put("dust-devils", 267355);
        ids.put("elves", 256687);
        ids.put("fire-giants", 297889);
        ids.put("fossil-island-wyverns", 297887);
        ids.put("frost-dragons", 39287);
        ids.put("gargoyles", 256647);
        ids.put("greater-demons", 298108);
        ids.put("gryphons", 613360);
        ids.put("hellhounds", 256645);
        ids.put("kalphite", 259777);
        ids.put("kurask", 634049);
        ids.put("lizardmen", 297852);
        ids.put("metal-dragons", 602634);
        ids.put("mutated-zygomites", 271838);
        ids.put("nechryael", 523205);
        ids.put("red-dragons", 297868);
        ids.put("skeletal-wyverns", 14925);
        ids.put("smoke-devils", 298012);
        ids.put("spiritual-creatures", 526458);
        ids.put("suqahs", 378001);
        ids.put("trolls", 298280);
        ids.put("tzhaar", 514851);
        ids.put("vampyres", 298140);
        ids.put("warped-creatures", 601420);
        ids.put("waterfiends", 362720);
        ids.put("wyrms", 356964);

        return Collections.unmodifiableMap(ids);
    }

    private static SourceTask read(Path path) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, SourceTask.class);
        }
    }
}

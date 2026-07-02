package com.danieljglover.allinslayer.data.source;

import com.google.gson.GsonBuilder;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SlayerDataGenerationDeterminismTest
{
    @Test
    public void compilingTheSameSourcesTwiceProducesIdenticalJson()
    {
        String first = toJson();
        String second = toJson();

        assertEquals(first, second);
    }

    private static String toJson()
    {
        return new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
            .toJson(ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")));
    }
}

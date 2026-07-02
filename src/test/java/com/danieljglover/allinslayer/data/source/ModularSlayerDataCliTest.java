package com.danieljglover.allinslayer.data.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class ModularSlayerDataCliTest
{
    @Test
    public void writesCompiledJsonAndMetaToOutputPaths() throws IOException
    {
        // WA-8 (ADR-0018 #1): one CLI run emits BOTH generated resources. The tasks artifact
        // keeps its bare-array root (the byte-shape contract its determinism/equivalence tests
        // rely on); masters + rewards live in the separate slayer-meta.json object.
        Path root = Files.createTempDirectory("modular-slayer-cli");
        write(root.resolve("masters/duradel.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("rewards/bigger-and-badder.json"),
            "{\"rewardId\":\"bigger-and-badder\",\"name\":\"Bigger and Badder\","
                + "\"pointsCost\":50,\"effect\":\"SUPERIOR\"}");
        Path output = root.resolve("out/data/slayer-data.json");
        Path meta = root.resolve("out/data/slayer-meta.json");

        ModularSlayerDataCli.main(new String[] {root.toString(), output.toString(), meta.toString()});

        assertTrue("tasks json exists", Files.exists(output));
        assertTrue("tasks artifact keeps its bare-array root",
            new String(Files.readAllBytes(output), StandardCharsets.UTF_8).trim().startsWith("["));
        assertTrue("meta json exists", Files.exists(meta));
        String metaJson = new String(Files.readAllBytes(meta), StandardCharsets.UTF_8).trim();
        assertTrue("meta artifact is an object", metaJson.startsWith("{"));
        assertTrue(metaJson.contains("\"masters\""));
        assertTrue(metaJson.contains("\"rewards\""));
        assertTrue(metaJson.contains("\"duradel\""));
        assertTrue(metaJson.contains("\"bigger-and-badder\""));
    }

    private static void write(Path path, String json) throws IOException
    {
        Files.createDirectories(path.getParent());
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }
}

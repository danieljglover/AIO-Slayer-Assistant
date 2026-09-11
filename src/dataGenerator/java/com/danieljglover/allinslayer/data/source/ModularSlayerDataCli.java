package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.SlayerMeta;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class ModularSlayerDataCli
{
    private ModularSlayerDataCli()
    {
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length != 3)
        {
            throw new IllegalArgumentException(
                "usage: ModularSlayerDataCli <sourceRoot> <tasksJson> <metaJson>");
        }

        Path sourceRoot = Paths.get(args[0]);
        Path tasksOutput = Paths.get(args[1]);
        Path metaOutput = Paths.get(args[2]);
        // Two artifacts, one run (WA-8, ADR-0018 #1): the task array keeps its bare-array root;
        // masters + rewards travel in the separate slayer-meta.json object.
        List<TaskData> tasks = ModularSlayerDataCompiler.compile(sourceRoot);
        SlayerMeta meta = ModularSlayerDataCompiler.compileMeta(sourceRoot);
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        write(tasksOutput, gson, tasks);
        write(metaOutput, gson, meta);
    }

    private static void write(Path output, Gson gson, Object value) throws IOException
    {
        Files.createDirectories(output.getParent());
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8))
        {
            gson.toJson(value, writer);
        }
    }
}

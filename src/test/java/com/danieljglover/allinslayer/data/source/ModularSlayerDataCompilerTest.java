package com.danieljglover.allinslayer.data.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModularSlayerDataCompilerTest
{
    @Test
    public void duplicateMasterIdsFailValidation() throws IOException
    {
        Path root = tempDir();
        write(root.resolve("masters/a.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("masters/b.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel copy\"}");

        try
        {
            ModularSlayerDataCompiler.compile(root);
            fail("expected validation failure");
        }
        catch (SlayerDataValidationException ex)
        {
            assertContains(ex.getErrors(), "duplicate masterId: duradel");
        }
    }

    @Test
    public void taskReferencingUnknownVariantFailsValidation() throws IOException
    {
        Path root = tempDir();
        write(root.resolve("masters/duradel.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\","
                + "\"masterIds\":[\"duradel\"],\"variantIds\":[\"missing-variant\"]}");

        try
        {
            ModularSlayerDataCompiler.compile(root);
            fail("expected validation failure");
        }
        catch (SlayerDataValidationException ex)
        {
            assertContains(ex.getErrors(), "unknown variantId: missing-variant");
        }
    }

    @Test
    public void multiVariantTaskWithoutDefaultFailsValidation() throws IOException
    {
        Path root = tempDir();
        write(root.resolve("masters/duradel.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("monsters/demons.json"),
            "{\"monsterId\":\"demons\",\"name\":\"Demons\",\"variants\":["
                + "{\"variantId\":\"greater-demon\",\"name\":\"Greater demon\"},"
                + "{\"variantId\":\"tormented-demon\",\"name\":\"Tormented Demon\"}]}");
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\","
                + "\"masterIds\":[\"duradel\"],\"variantIds\":[\"greater-demon\",\"tormented-demon\"]}");

        try
        {
            ModularSlayerDataCompiler.compile(root);
            fail("expected validation failure");
        }
        catch (SlayerDataValidationException ex)
        {
            assertContains(ex.getErrors(), "missing defaultVariantId for multi-variant task: greater-demons");
        }
    }

    @Test
    public void strategyReferencingUnknownWeaponFailsValidation() throws IOException
    {
        Path root = tempDir();
        write(root.resolve("monsters/demons.json"),
            "{\"monsterId\":\"demons\",\"name\":\"Demons\",\"variants\":["
                + "{\"variantId\":\"tormented-demon\",\"name\":\"Tormented Demon\",\"strategyId\":\"tormented-demon\"}]}");
        write(root.resolve("strategies/tormented-demon.md"),
            "---\n"
                + "strategyId: tormented-demon\n"
                + "variantIds: [tormented-demon]\n"
                + "primaryStyle: MELEE\n"
                + "primaryWeapons: [missing-weapon]\n"
                + "---\n"
                + "# Tormented Demon\n");

        try
        {
            ModularSlayerDataCompiler.compile(root);
            fail("expected validation failure");
        }
        catch (SlayerDataValidationException ex)
        {
            assertContains(ex.getErrors(), "unknown weaponId: missing-weapon");
        }
    }

    private static Path tempDir() throws IOException
    {
        return Files.createTempDirectory("modular-slayer-data");
    }

    private static void write(Path path, String json) throws IOException
    {
        Files.createDirectories(path.getParent());
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertContains(List<String> errors, String expected)
    {
        assertTrue("expected errors to contain '" + expected + "' but got " + errors,
            errors.stream().anyMatch(e -> e.contains(expected)));
    }
}

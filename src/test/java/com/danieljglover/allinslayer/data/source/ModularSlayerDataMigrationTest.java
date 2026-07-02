package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterVariant;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModularSlayerDataMigrationTest
{
    @Test
    public void modularSourcesRepresentTheFullCurrentDataset()
    {
        List<TaskData> tasks = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer"));

        // WC-1..11 (C1 fan-out): the compiled task count must equal the source task-file count,
        // and the pre-C1 baseline of 43/228 is a floor. Fixed literals were a contested line for
        // every parallel family agent; the source-derived invariant is race-free.
        assertEquals(taskSourceFileCount(), tasks.size());
        assertTrue("pre-C1 baseline regressed: " + tasks.size(), tasks.size() >= 43);
        assertTrue("pre-C1 variant baseline regressed: " + variantCount(tasks), variantCount(tasks) >= 228);
        assertEquals(variantCount(tasks), strategyCount(tasks));

        Set<String> names = new HashSet<>();
        for (TaskData task : tasks)
        {
            names.add(task.getTask());
        }
        assertTrue(names.contains("Greater demons"));
        assertTrue(names.contains("Boss"));
        assertTrue(names.contains("Frost Dragons"));
    }

    @Test
    public void compilesRequiredItemIdFromRealTaskSources()
    {
        List<TaskData> tasks = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer"));

        TaskData aberrant = taskNamed(tasks, "Aberrant spectres");
        assertEquals(Integer.valueOf(4168), aberrant.getRequiredItemId());
        assertEquals("Nose peg", aberrant.getRequiredItemName());

        TaskData gargoyles = taskNamed(tasks, "Gargoyles");
        assertEquals(Integer.valueOf(4162), gargoyles.getRequiredItemId());
        assertEquals("Rock hammer", gargoyles.getRequiredItemName());
    }

    @Test
    public void bossMetaTaskKeepsTaskSpecificDuplicateVariantProfiles()
    {
        List<TaskData> tasks = ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer"));
        TaskData boss = taskNamed(tasks, "Boss");

        MonsterVariant abyssalSire = variantNamed(boss, "Abyssal Sire");
        assertEquals(CombatStyle.MAGIC, abyssalSire.getWeakness().getStyle());

        MonsterVariant thermonuclear = variantNamed(boss, "Thermonuclear smoke devil");
        assertEquals(360, thermonuclear.getMonsterDefence().getDefenceLevel());
        assertEquals(900, thermonuclear.getMonsterDefence().getRange());
    }

    @Test
    public void everyMonsterFamilyUsesOneFilePerVariant() throws IOException
    {
        Path monstersDir = Paths.get("src/main/data/slayer/monsters");
        try (Stream<Path> paths = Files.list(monstersDir))
        {
            List<Path> topLevelJsonFiles = paths
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());
            assertTrue("monster families must be directories, found top-level files: " + topLevelJsonFiles,
                topLevelJsonFiles.isEmpty());
        }

        try (Stream<Path> paths = Files.list(monstersDir))
        {
            List<Path> emptyFamilyDirs = paths
                .filter(Files::isDirectory)
                .filter(path -> variantJsonCount(path) == 0)
                .collect(Collectors.toList());
            assertTrue("monster family directories must contain variant JSON files: " + emptyFamilyDirs,
                emptyFamilyDirs.isEmpty());
        }
    }

    private static int taskSourceFileCount()
    {
        try (Stream<Path> paths = Files.list(Paths.get("src/main/data/slayer/tasks")))
        {
            return (int) paths.filter(p -> p.getFileName().toString().endsWith(".json")).count();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static int variantCount(List<TaskData> tasks)
    {
        int count = 0;
        for (TaskData task : tasks)
        {
            if (task.getVariants() != null)
            {
                count += task.getVariants().size();
            }
        }
        return count;
    }

    private static int strategyCount(List<TaskData> tasks)
    {
        int count = 0;
        for (TaskData task : tasks)
        {
            if (task.getVariants() == null)
            {
                continue;
            }
            count += task.getVariants().stream().filter(v -> v.getStrategy() != null).count();
        }
        return count;
    }

    private static long variantJsonCount(Path familyDir)
    {
        try (Stream<Path> paths = Files.list(familyDir))
        {
            return paths
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .count();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("failed to inspect " + familyDir, e);
        }
    }

    private static TaskData taskNamed(List<TaskData> tasks, String name)
    {
        return tasks.stream()
            .filter(task -> name.equals(task.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing task: " + name));
    }

    private static MonsterVariant variantNamed(TaskData task, String name)
    {
        return task.getVariants().stream()
            .filter(variant -> name.equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing variant: " + task.getTask() + " / " + name));
    }
}

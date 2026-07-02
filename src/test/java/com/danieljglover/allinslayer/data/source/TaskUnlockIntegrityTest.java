package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.loadout.VarbitSlayerUnlockStateProvider;
import com.danieljglover.allinslayer.model.UnlockType;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * WA-5 (ADR-0018 #5 / FR-R2 sections 1d + 5): the whole-dataset unlock invariants. Every task that
 * carries an {@code extendedAmount} must name the exact EXTENSION-typed unlock that enables it
 * (with a real cost), and no authored unlock may have a null {@code pointsCost} - the audit found
 * 4 (bloodveld's two under a wrong "points" key, frost-dragons' two explicitly null). These pin
 * the fix and stop a silent regression; the compiler's WA-7 cross-validation enforces the same
 * pairing at build time.
 */
public class TaskUnlockIntegrityTest
{
    private static final Path TASKS = Paths.get("src/main/data/slayer/tasks");
    private static final Gson GSON = new Gson();

    @Test
    public void everyExtendingTaskHasExactlyOneExtensionUnlockWithANonNullCost() throws IOException
    {
        List<String> broken = new ArrayList<>();
        int extendingTasks = 0;
        for (SourceTask task : tasks())
        {
            if (task.getExtendedAmount() == null || task.getExtendedAmount().isEmpty())
            {
                continue;
            }
            extendingTasks++;
            List<SourceTaskUnlock> extensions = (task.getUnlocks() == null
                ? Stream.<SourceTaskUnlock>empty() : task.getUnlocks().stream())
                .filter(u -> u != null && u.getType() == UnlockType.EXTENSION)
                .collect(Collectors.toList());
            if (extensions.size() != 1)
            {
                broken.add(task.getTaskId() + " has " + extensions.size() + " EXTENSION unlocks");
            }
            else if (extensions.get(0).getPointsCost() == null)
            {
                broken.add(task.getTaskId() + " EXTENSION unlock "
                    + extensions.get(0).getUnlockId() + " has a null pointsCost");
            }
        }
        assertTrue("every extendedAmount task needs exactly one costed EXTENSION unlock, broken: "
            + broken, broken.isEmpty());
        assertEquals("the dataset's extending-task count (guards test vacuity)", 29, extendingTasks);
    }

    @Test
    public void everyAuthoredUnlockCarriesANonNullPointsCost() throws IOException
    {
        // A null cost is unrenderable ("... with Bleed me dry (? pts)") and historically meant a
        // wrong key ("points") or an unresearched value - author the real cost or not the row.
        List<String> broken = new ArrayList<>();
        for (SourceTask task : tasks())
        {
            if (task.getUnlocks() == null)
            {
                continue;
            }
            for (SourceTaskUnlock unlock : task.getUnlocks())
            {
                if (unlock != null && unlock.getPointsCost() == null)
                {
                    broken.add(task.getTaskId() + " -> " + unlock.getUnlockId());
                }
            }
        }
        assertTrue("unlock rows with a null pointsCost: " + broken, broken.isEmpty());
    }

    @Test
    public void theFourAuditedNullCostsCarryTheirWikiValues() throws IOException
    {
        // FR-R2 section 1d: bloodveld 50/75 (were under a wrong "points" key), frost-dragons
        // 100/100 (were explicitly null).
        Map<String, Integer> costs = unlockCosts();
        assertEquals(Integer.valueOf(50), costs.get("bloodveld/bigger-and-badder"));
        assertEquals(Integer.valueOf(75), costs.get("bloodveld/bleed-me-dry"));
        assertEquals(Integer.valueOf(100), costs.get("frost-dragons/chance-of-heavy-frost"));
        assertEquals(Integer.valueOf(100), costs.get("frost-dragons/i-see-dragons"));
    }

    @Test
    public void theThreeUnlocklessExtendingTasksGainedTheirWikiExtensionUnlocks() throws IOException
    {
        // dust-devils / nechryael / suqahs carried an extendedAmount but NO unlock rows at all;
        // WA-5 authored the enabling unlock from the wiki Slayer Rewards "Extend" table.
        Map<String, Integer> costs = unlockCosts();
        assertEquals(Integer.valueOf(100), costs.get("dust-devils/to-dust-you-shall-return"));
        assertEquals(Integer.valueOf(100), costs.get("nechryael/nechs-please"));
        assertEquals(Integer.valueOf(100), costs.get("suqahs/suq-a-nother-one"));
    }

    @Test
    public void everyAuthoredExtensionUnlockIsVarbitAnswerable() throws IOException
    {
        // FR-RV S2: the WA-13 hint only considers an EXTENSION candidate whose ownership the
        // varbit provider can answer (UnlockAdvisor excludes the rest - an unanswerable candidate
        // must never be recommended, or an owned extension reads unowned forever). Every
        // extension in the dataset today is spike-verified and mapped. A NEW extending task must
        // either get its SLAYER_LONGER_* varbit verified per the spike method
        // (docs/full-review/spike-player-state.md) and mapped in VarbitSlayerUnlockStateProvider,
        // or be added to KNOWN_UNANSWERABLE here as a conscious, documented hint exclusion -
        // never a silent guess.
        Set<String> knownUnanswerable = Collections.emptySet();
        List<String> unanswerable = new ArrayList<>();
        int extensions = 0;
        for (SourceTask task : tasks())
        {
            if (task.getUnlocks() == null)
            {
                continue;
            }
            for (SourceTaskUnlock unlock : task.getUnlocks())
            {
                if (unlock == null || unlock.getType() != UnlockType.EXTENSION)
                {
                    continue;
                }
                extensions++;
                if (!VarbitSlayerUnlockStateProvider.answersUnlock(unlock.getUnlockId())
                    && !knownUnanswerable.contains(unlock.getUnlockId()))
                {
                    unanswerable.add(task.getTaskId() + "/" + unlock.getUnlockId());
                }
            }
        }
        assertTrue("EXTENSION unlocks with no verified varbit and no conscious exclusion: "
            + unanswerable, unanswerable.isEmpty());
        assertEquals("the dataset's extension-unlock count (guards test vacuity)", 29, extensions);
    }

    private static Map<String, Integer> unlockCosts() throws IOException
    {
        Map<String, Integer> costs = new LinkedHashMap<>();
        for (SourceTask task : tasks())
        {
            if (task.getUnlocks() == null)
            {
                continue;
            }
            for (SourceTaskUnlock unlock : task.getUnlocks())
            {
                if (unlock != null && unlock.getUnlockId() != null)
                {
                    costs.put(task.getTaskId() + "/" + unlock.getUnlockId(), unlock.getPointsCost());
                }
            }
        }
        return costs;
    }

    private static List<SourceTask> tasks() throws IOException
    {
        List<SourceTask> tasks = new ArrayList<>();
        try (Stream<Path> paths = Files.list(TASKS))
        {
            for (Path file : paths.filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted().collect(Collectors.toList()))
            {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
                {
                    SourceTask task = GSON.fromJson(reader, SourceTask.class);
                    if (task != null)
                    {
                        tasks.add(task);
                    }
                }
            }
        }
        return tasks;
    }
}

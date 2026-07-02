package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.model.TaskData;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * WA-11 (ADR-0018 #8): the pure master-selection helper. The surrounding {@code recompute} wiring
 * (seeding from {@link com.danieljglover.allinslayer.loadout.SlayerUnlockStateProvider#currentMaster()},
 * per-task reset) is field-injected and client-thread bound (manual checklist); this locks the
 * cheaply-testable piece - stale-master validation against the task's assigners.
 */
public class AllInSlayerPluginMasterTest
{
    private static TaskData taskAssignedBy(String... masterIds)
    {
        TaskData t = new TaskData();
        t.setTask("T");
        t.setAssignedBy(Arrays.asList(masterIds));
        return t;
    }

    @Test
    public void isAssignedByAcceptsOnlyTheTasksAssigners()
    {
        TaskData task = taskAssignedBy("duradel", "nieve");
        assertTrue(AllInSlayerPlugin.isAssignedBy(task, "duradel"));
        assertTrue(AllInSlayerPlugin.isAssignedBy(task, "nieve"));
        assertFalse("a master that does not assign this task is not a valid selection",
            AllInSlayerPlugin.isAssignedBy(task, "krystilia"));
    }

    @Test
    public void isAssignedByRejectsNullsAndMasterlessTasks()
    {
        TaskData task = taskAssignedBy("duradel");
        assertFalse("null = no selection, never a valid pick", AllInSlayerPlugin.isAssignedBy(task, null));
        assertFalse(AllInSlayerPlugin.isAssignedBy(null, "duradel"));

        TaskData bare = new TaskData();
        bare.setTask("T");
        assertFalse("a task with no assignedBy list validates nothing",
            AllInSlayerPlugin.isAssignedBy(bare, "duradel"));
    }
}

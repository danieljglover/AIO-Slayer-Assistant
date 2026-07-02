package com.danieljglover.allinslayer.task;

import com.danieljglover.allinslayer.data.SlayerDataService;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;

@Singleton
public class TaskDetector
{
    private static final int SLAYER_TASK_TABLE = 113;
    private static final int SLAYER_TASK_NAME_FIELD = 10;

    private final Client client;
    private final SlayerDataService data;

    @Getter
    private int remaining;

    @Inject
    public TaskDetector(Client client, SlayerDataService data)
    {
        this.client = client;
        this.data = data;
    }

    /** Reads the live slayer varps and resolves the current task (empty if none assigned). */
    public Optional<TaskData> resolveCurrentTask()
    {
        int targetId = client.getVarpValue(SlayerVarbits.SLAYER_TARGET);
        this.remaining = client.getVarpValue(SlayerVarbits.SLAYER_COUNT);
        if (targetId <= 0)
        {
            return Optional.empty();
        }
        Optional<TaskData> byTarget = data.byTargetVarp(targetId);
        if (byTarget.isPresent())
        {
            return byTarget;
        }
        return resolveByRuneLiteTaskDb(targetId);
    }

    private Optional<TaskData> resolveByRuneLiteTaskDb(int targetId)
    {
        try
        {
            List<Integer> rows = client.getDBRowsByValue(SLAYER_TASK_TABLE, 0, 0, targetId);
            if (rows.isEmpty())
            {
                return Optional.empty();
            }
            Object[] fields = client.getDBTableField(rows.get(0), SLAYER_TASK_NAME_FIELD, 0);
            if (fields.length == 0 || !(fields[0] instanceof String))
            {
                return Optional.empty();
            }
            return data.byTaskName((String) fields[0]);
        }
        catch (RuntimeException e)
        {
            return Optional.empty();
        }
    }
}

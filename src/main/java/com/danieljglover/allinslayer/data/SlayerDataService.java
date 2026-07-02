package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.model.MasterData;
import com.danieljglover.allinslayer.model.RewardData;
import com.danieljglover.allinslayer.model.SlayerMeta;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SlayerDataService
{
    private static final String RESOURCE = "/data/slayer-data.json";
    // WA-8 (ADR-0018 #1): the second generated resource carrying {masters, rewards}.
    private static final String META_RESOURCE = "/data/slayer-meta.json";

    private final Gson gson;
    private final Map<String, TaskData> byName = new LinkedHashMap<>();
    private final Map<Integer, TaskData> byTarget = new HashMap<>();
    private final Map<String, MasterData> masters = new LinkedHashMap<>();
    private final List<RewardData> rewards = new ArrayList<>();

    @Inject
    public SlayerDataService(Gson gson)
    {
        this.gson = gson;
    }

    public void load()
    {
        byName.clear();
        byTarget.clear();
        masters.clear();
        rewards.clear();
        try (InputStream in = SlayerDataService.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                log.error("Slayer data resource not found: {}", RESOURCE);
                return;
            }
            loadTasks(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            log.error("Failed to load slayer data", e);
        }
        // The meta resource is ADDITIVE (WA-8): whatever happens here, task data stands.
        try (InputStream in = SlayerDataService.class.getResourceAsStream(META_RESOURCE))
        {
            if (in == null)
            {
                log.warn("Slayer meta resource not found: {}", META_RESOURCE);
                return;
            }
            loadMeta(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            log.error("Failed to load slayer meta", e);
        }
    }

    /**
     * WB-5 (FR-C1 D9): parse + index the task array, degrading instead of throwing through the
     * plugin's {@code startUp()}. A non-parseable resource (Gson's JsonParseException family and
     * any other runtime failure) logs and leaves the service empty - the panel's existing no-data
     * path; a null entry or a task with no name is skipped so one bad row cannot take down the
     * other 42. Package-private seam so tests can feed synthetic content.
     */
    void loadTasks(Reader reader)
    {
        List<TaskData> tasks;
        try
        {
            tasks = gson.fromJson(reader, new TypeToken<List<TaskData>>() {}.getType());
        }
        catch (RuntimeException e)
        {
            log.error("Slayer data resource is not parseable - task data disabled", e);
            return;
        }
        if (tasks == null)
        {
            return;
        }
        for (TaskData t : tasks)
        {
            if (t == null || t.getTask() == null)
            {
                log.warn("Skipping slayer task entry with no name: {}", t);
                continue;
            }
            byName.put(t.getTask().toLowerCase(Locale.ROOT), t);
            if (t.getSlayerTargetId() > 0)
            {
                byTarget.put(t.getSlayerTargetId(), t);
            }
        }
        log.debug("Loaded {} slayer tasks", byName.size());
    }

    /**
     * WA-8: parse + index the {masters, rewards} meta resource, with the same degrade-not-crash
     * discipline as {@link #loadTasks(Reader)} - a corrupt meta must never take down task data
     * (masters/rewards render is additive; the loadout core is not). Package-private test seam.
     */
    void loadMeta(Reader reader)
    {
        SlayerMeta meta;
        try
        {
            meta = gson.fromJson(reader, SlayerMeta.class);
        }
        catch (RuntimeException e)
        {
            log.error("Slayer meta resource is not parseable - master/reward data disabled", e);
            return;
        }
        if (meta == null)
        {
            return;
        }
        if (meta.getMasters() != null)
        {
            for (MasterData master : meta.getMasters())
            {
                if (master == null || master.getMasterId() == null)
                {
                    log.warn("Skipping slayer master entry with no id: {}", master);
                    continue;
                }
                masters.put(master.getMasterId(), master);
            }
        }
        if (meta.getRewards() != null)
        {
            for (RewardData reward : meta.getRewards())
            {
                if (reward == null)
                {
                    continue;
                }
                rewards.add(reward);
            }
        }
        log.debug("Loaded {} slayer masters, {} rewards", masters.size(), rewards.size());
    }

    public Optional<MasterData> masterById(String masterId)
    {
        if (masterId == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(masters.get(masterId));
    }

    public List<RewardData> rewards()
    {
        return Collections.unmodifiableList(rewards);
    }

    public Optional<TaskData> byTaskName(String name)
    {
        if (name == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<TaskData> byTargetVarp(int slayerTargetId)
    {
        return Optional.ofNullable(byTarget.get(slayerTargetId));
    }

    public Collection<TaskData> all()
    {
        return Collections.unmodifiableCollection(byName.values());
    }
}

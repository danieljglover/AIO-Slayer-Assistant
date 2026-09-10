package com.danieljglover.allinslayer.task.advisor;

import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.AllArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/** Resolves live task and location tables; no chat parsing or invented target IDs. */
@Singleton
public final class ActiveTaskReader
{
    private static final String[] MASTERS = {null, "turael", "mazchna", "vannaka", "chaeldar",
        "duradel", "nieve", "krystilia", "konar", "spria", "mortimer"};
    private final Client client;

    @Inject
    public ActiveTaskReader(Client client)
    {
        this.client = client;
    }

    public ActiveTask read(SlayerCatalogue catalogue)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return new ActiveTask(null, null, null, null, 0, false, "Log in to detect your Slayer assignment.");
        }
        int remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        int target = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
        if (remaining <= 0 || target <= 0)
        {
            return new ActiveTask(null, null, null, null, 0, false, "No active task. Browse the catalogue to plan a trip.");
        }
        int masterValue = client.getVarbitValue(VarbitID.SLAYER_MASTER);
        String master = masterValue > 0 && masterValue < MASTERS.length ? MASTERS[masterValue] : null;
        String name = null;
        boolean boss = target == 98;
        try
        {
            Integer taskRow;
            if (boss)
            {
                Integer row = row(DBTableID.SlayerTaskSublist.ID, DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
                    client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
                Object value = row == null ? null : field(row, DBTableID.SlayerTaskSublist.COL_TASK);
                taskRow = value instanceof Integer ? (Integer) value : null;
            }
            else
            {
                taskRow = row(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, target);
            }
            if (taskRow != null)
            {
                Object value = field(taskRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE);
                name = value instanceof String ? (String) value : null;
            }
        }
        catch (RuntimeException ignored)
        {
            // During login or a cache update the table may not be ready. Try again next tick.
        }
        SlayerCatalogue.Task task = null;
        String monsterId = null;
        Set<String> allowedMonsters = new LinkedHashSet<>();
        for (SlayerCatalogue.Task candidate : catalogue.getTasks().values())
        {
            if ((!boss && equivalent(candidate.getName(), name))
                || (boss && ("boss".equals(candidate.getId()) || "bosses".equals(candidate.getId()))))
            {
                task = candidate;
                break;
            }
        }
        if (boss && task != null)
        {
            for (String id : task.getMonsterIds())
            {
                SlayerCatalogue.Monster monster = catalogue.getMonsters().get(id);
                if (monster != null && matchesBoss(monster.getName(), name))
                {
                    allowedMonsters.add(id);
                }
            }
            if (allowedMonsters.isEmpty())
            {
                return new ActiveTask(null, master, null, null, remaining, "krystilia".equals(master),
                    "Boss assignment not mapped: " + (name == null ? "waiting for task details" : name));
            }
            if (allowedMonsters.size() == 1)
            {
                monsterId = allowedMonsters.iterator().next();
            }
        }
        if (task == null)
        {
            return new ActiveTask(null, master, null, null, remaining, "krystilia".equals(master),
                "Assignment not mapped: " + (name == null ? "task " + target : name) + ". The catalogue is still available.");
        }
        String lockedLocation = null;
        int area = client.getVarpValue(VarPlayerID.SLAYER_AREA);
        if (area > 0)
        {
            String areaName = null;
            try
            {
                Integer row = row(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, area);
                Object value = row == null ? null : field(row, DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER);
                areaName = value instanceof String ? (String) value : null;
            }
            catch (RuntimeException ignored)
            {
                // An unresolved active lock must never become an unrestricted recommendation.
            }
            // Several source records share a physical area's name but carry task-specific
            // capabilities. Resolve the lock in this assignment's locations, never globally.
            Set<String> taskLocations = new LinkedHashSet<>(task.getLocationIds());
            for (String id : task.getMonsterIds())
            {
                SlayerCatalogue.Monster monster = catalogue.getMonsters().get(id);
                if (monster != null && (allowedMonsters.isEmpty() || allowedMonsters.contains(id)))
                {
                    taskLocations.addAll(monster.getLocationIds());
                }
            }
            for (String id : taskLocations)
            {
                SlayerCatalogue.Location location = catalogue.getLocations().get(id);
                if (location != null && equivalent(location.getName(), areaName))
                {
                    lockedLocation = location.getId();
                    break;
                }
                SlayerCatalogue.Location parent = location == null ? null
                    : catalogue.getLocations().get(location.getAssignmentAreaId());
                if (parent != null && equivalent(parent.getName(), areaName))
                {
                    // A boss chamber can share a Konar assignment area without sharing the
                    // parent location's cannon, safespot or access capabilities.
                    lockedLocation = parent.getId();
                    break;
                }
            }
            if (lockedLocation == null)
            {
                lockedLocation = "unresolved-area-" + area;
                return new ActiveTask(task.getId(), master, monsterId, lockedLocation, remaining,
                    "krystilia".equals(master), "Location restriction not mapped: "
                    + (areaName == null ? "area " + area : areaName) + ". Recommendations are withheld until it is resolved.",
                    Collections.unmodifiableSet(allowedMonsters));
            }
        }
        return new ActiveTask(task.getId(), master, monsterId, lockedLocation, remaining,
            "krystilia".equals(master), (boss ? name : task.getName()) + " - " + remaining + " remaining",
            Collections.unmodifiableSet(allowedMonsters));
    }

    private Integer row(int table, int column, int value)
    {
        List<Integer> rows = client.getDBRowsByValue(table, column, 0, value);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Object field(int row, int column)
    {
        Object[] fields = client.getDBTableField(row, column, 0);
        return fields.length == 0 ? null : fields[0];
    }

    private static boolean equivalent(String first, String second)
    {
        return first != null && second != null && normalise(first).equals(normalise(second));
    }

    private static String normalise(String value)
    {
        String normalised = value.toLowerCase(Locale.ROOT).replaceFirst("^the ", "")
            .replaceAll("[^a-z0-9]", "");
        // The bundled task families use some singular names; the game's assignment labels
        // use plurals (for example Kalphite/Kalphites and Aviansie/Aviansies).
        return normalised.endsWith("s") && !normalised.endsWith("ss")
            ? normalised.substring(0, normalised.length() - 1) : normalised;
    }

    private static boolean matchesBoss(String monster, String assignment)
    {
        if (monster == null || assignment == null)
        {
            return false;
        }
        String target = normalise(assignment);
        String name = normalise(monster);
        if (target.equals(name))
        {
            return true;
        }
        if (target.equals("barrowsbrother"))
        {
            return name.matches("^(ahrim|dharok|guthan|karil|torag|verac).*$");
        }
        if (target.equals("dagannothking"))
        {
            return name.equals("dagannothrex") || name.equals("dagannothprime") || name.equals("dagannothsupreme");
        }
        return target.equals("grotesqueguardian") && name.startsWith("grotesqueguardian")
            || target.equals("zulrah") && name.startsWith("zulrah")
            || target.equals("vetion") && name.equals("calvarion")
            || target.equals("callisto") && name.equals("artio")
            || target.equals(normalise("Venenatis")) && name.equals("spindel")
            || target.equals("cavekrakenboss") && name.equals("kraken");
    }

    @Value
    @AllArgsConstructor
    public static class ActiveTask
    {
        String taskId;
        String masterId;
        String monsterId;
        String lockedLocationId;
        int remaining;
        boolean wilderness;
        String status;
        Set<String> allowedMonsterIds;

        public ActiveTask(String taskId, String masterId, String monsterId, String lockedLocationId,
            int remaining, boolean wilderness, String status)
        {
            this(taskId, masterId, monsterId, lockedLocationId, remaining, wilderness, status,
                Collections.emptySet());
        }
    }
}

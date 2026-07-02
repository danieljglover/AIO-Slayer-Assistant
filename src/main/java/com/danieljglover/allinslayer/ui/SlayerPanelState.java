package com.danieljglover.allinslayer.ui;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.TaskData;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Value;

@Value
public class SlayerPanelState
{
    TaskData task;
    Recommendation recommendation;
    int remaining;
    AdviceMode mode;
    String bankAge;
    Map<Integer, String> itemNames;
    Map<Integer, Integer> itemPrices;
    RefreshSource refreshSource;
    Instant updatedAt;
    PanelStatus status;
    String statusMessage;
    SlayerDebugSnapshot debug;
    String selectedLocationName;
    int playerSlayerLevel;
    boolean developerMode;
    /**
     * Whether the player owns the task's required item, when the plugin could resolve it (the
     * authoritative ownership signal). {@code null} means "unknown" - the panel then stays neutral
     * (OWNED: primary text, no tag) rather than asserting the item is missing.
     */
    Boolean requiredItemOwned;
    /**
     * The user-selected monster variant name and combat method (MV-B7, ADR-0010/0013). Both {@code
     * null} = the default variant / recommended method (today's behaviour). The Frontend reads these to
     * pre-select the variant/method combos (MV-FE1/FE3); the plugin reconciles them per render.
     */
    String selectedVariantName;
    CombatStyle selectedMethod;
    /**
     * Whether the persisted bank snapshot has aged past the staleness threshold (DT-FE4 / PD-5). When
     * true the loadout card shows a non-blocking "reopen your bank to refresh" nudge. Defaults false;
     * a never-seen bank is handled by {@link PanelStatus#BANK_NOT_SCANNED}, not this flag.
     */
    boolean bankStale;
    /**
     * The user-selected (or {@code currentMaster()}-seeded) assigning master's {@code masterId}
     * (WA-11, ADR-0018 #8 / PD-E). {@code null} = no selection - the panel falls back to the task's
     * first assigning master. Reset on task change; validated plugin-side against
     * {@code task.assignedBy} so a stale pick never leaks across tasks.
     */
    String selectedMaster;
    /**
     * Display names for the task's assigning masters, keyed by {@code masterId} (resolved by the
     * plugin from {@code SlayerDataService.masterById}). Never {@code null}; an absent key makes the
     * panel prettify the slug instead, so a degraded meta resource still renders honestly.
     */
    Map<String, String> masterNames;

    public static SlayerPanelState noTask(int remaining, AdviceMode mode, String bankAge,
        Map<Integer, String> itemNames, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug)
    {
        return noTask(remaining, mode, bankAge, itemNames, refreshSource, updatedAt, debug, false);
    }

    /**
     * No-task state carrying the developer-mode flag (ADR-0003) so Diagnostics is reachable in the
     * very state a developer most needs it. The 7-arg overload above delegates here with
     * {@code developerMode=false} to keep existing call sites compiling unchanged.
     */
    public static SlayerPanelState noTask(int remaining, AdviceMode mode, String bankAge,
        Map<Integer, String> itemNames, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, boolean developerMode)
    {
        return new SlayerPanelState(null, null, remaining, mode, bankAge, copyNames(itemNames),
            Collections.emptyMap(), refreshSource, updatedAt, PanelStatus.NO_TASK,
            "No Slayer task detected", safeDebug(debug), null, -1, developerMode, null, null, null,
            false, null, Collections.emptyMap());
    }

    public static SlayerPanelState unsupportedTask(int targetId, int remaining, AdviceMode mode,
        String bankAge, RefreshSource refreshSource, Instant updatedAt, SlayerDebugSnapshot debug)
    {
        return unsupportedTask(targetId, remaining, mode, bankAge, refreshSource, updatedAt, debug, false);
    }

    /**
     * Unsupported-task state carrying the developer-mode flag (ADR-0003). The 7-arg overload above
     * delegates here with {@code developerMode=false} to keep existing call sites compiling unchanged.
     */
    public static SlayerPanelState unsupportedTask(int targetId, int remaining, AdviceMode mode,
        String bankAge, RefreshSource refreshSource, Instant updatedAt, SlayerDebugSnapshot debug,
        boolean developerMode)
    {
        SlayerDebugSnapshot resolvedDebug = safeDebug(debug);
        if (resolvedDebug.getUnsupportedTargetId() != targetId)
        {
            resolvedDebug = resolvedDebug.withSlayerState(
                resolvedDebug.getSlayerTargetVarp(),
                resolvedDebug.getSlayerCountVarp(),
                resolvedDebug.getSlayerAreaVarp(),
                resolvedDebug.getBossTargetVarbit(),
                resolvedDebug.getDetectorResult(),
                targetId);
        }
        return new SlayerPanelState(null, null, remaining, mode, bankAge, Collections.emptyMap(),
            Collections.emptyMap(), refreshSource, updatedAt, PanelStatus.UNSUPPORTED_TASK,
            "Unsupported Slayer target: " + targetId, resolvedDebug, null, -1, developerMode, null,
            null, null, false, null, Collections.emptyMap());
    }

    public static SlayerPanelState forTask(TaskData task, Recommendation recommendation, int remaining,
        AdviceMode mode, String bankAge, Map<Integer, String> itemNames, RefreshSource refreshSource,
        Instant updatedAt, SlayerDebugSnapshot debug)
    {
        return forTask(task, recommendation, remaining, mode, bankAge, itemNames, refreshSource, updatedAt,
            debug, null, -1);
    }

    public static SlayerPanelState forTask(TaskData task, Recommendation recommendation, int remaining,
        AdviceMode mode, String bankAge, Map<Integer, String> itemNames, RefreshSource refreshSource,
        Instant updatedAt, SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel)
    {
        return forTask(task, recommendation, remaining, mode, bankAge, itemNames, Collections.emptyMap(),
            refreshSource, updatedAt, debug, selectedLocationName, playerSlayerLevel, false);
    }

    /**
     * Full factory carrying every render input, including per-item GE prices (ADR-0004) and the
     * developer-mode flag (ADR-0003). Delegates to the {@code requiredItemOwned}-carrying overload with
     * a {@code null} (unknown) ownership signal so existing call sites keep compiling.
     */
    public static SlayerPanelState forTask(TaskData task, Recommendation recommendation, int remaining,
        AdviceMode mode, String bankAge, Map<Integer, String> itemNames,
        Map<Integer, Integer> itemPrices, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel,
        boolean developerMode)
    {
        return forTask(task, recommendation, remaining, mode, bankAge, itemNames, itemPrices,
            refreshSource, updatedAt, debug, selectedLocationName, playerSlayerLevel, developerMode, null);
    }

    /**
     * Full factory additionally carrying the authoritative required-item ownership signal (B1). The
     * plugin uses this overload, passing {@code owned.has(requiredItemId)}; the overload above defaults
     * {@code requiredItemOwned} to {@code null} (unknown).
     */
    public static SlayerPanelState forTask(TaskData task, Recommendation recommendation, int remaining,
        AdviceMode mode, String bankAge, Map<Integer, String> itemNames,
        Map<Integer, Integer> itemPrices, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel,
        boolean developerMode, Boolean requiredItemOwned)
    {
        return forTask(task, recommendation, remaining, mode, bankAge, itemNames, itemPrices,
            refreshSource, updatedAt, debug, selectedLocationName, playerSlayerLevel, developerMode,
            requiredItemOwned, null, null);
    }

    /**
     * Full factory additionally carrying the selected variant + method (MV-B7). The plugin passes the
     * reconciled selection so the panel can pre-select the variant/method combos (MV-FE1/FE3). The
     * overload above delegates here with {@code null}/{@code null} (default variant / recommended method).
     */
    public static SlayerPanelState forTask(TaskData task, Recommendation recommendation, int remaining,
        AdviceMode mode, String bankAge, Map<Integer, String> itemNames,
        Map<Integer, Integer> itemPrices, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel,
        boolean developerMode, Boolean requiredItemOwned, String selectedVariantName,
        CombatStyle selectedMethod)
    {
        return forTask(task, recommendation, remaining, mode, bankAge, itemNames, itemPrices,
            refreshSource, updatedAt, debug, selectedLocationName, playerSlayerLevel, developerMode,
            requiredItemOwned, selectedVariantName, selectedMethod, false);
    }

    /**
     * Full factory additionally carrying the bank-staleness flag (DT-FE4 / PD-5). The plugin passes
     * {@code AllInSlayerPlugin.bankStale(...)}; the overload above delegates here with {@code false}
     * so every existing call site keeps compiling with today's (non-stale) behaviour.
     */
    public static SlayerPanelState forTask(TaskData task, Recommendation recommendation, int remaining,
        AdviceMode mode, String bankAge, Map<Integer, String> itemNames,
        Map<Integer, Integer> itemPrices, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel,
        boolean developerMode, Boolean requiredItemOwned, String selectedVariantName,
        CombatStyle selectedMethod, boolean bankStale)
    {
        return forTask(task, recommendation, remaining, mode, bankAge, itemNames, itemPrices,
            refreshSource, updatedAt, debug, selectedLocationName, playerSlayerLevel, developerMode,
            requiredItemOwned, selectedVariantName, selectedMethod, bankStale, null, null);
    }

    /**
     * Fullest factory additionally carrying the selected assigning master + the masterId->display-name
     * map (WA-11, ADR-0018 #8 / PD-E). The plugin passes the reconciled/seeded selection and the names
     * it resolved from {@code SlayerDataService.masterById}; the overload above delegates here with
     * {@code null}/{@code null} (no selection, no names) so every existing call site keeps compiling
     * with today's master-less Task card.
     */
    public static SlayerPanelState forTask(TaskData task, Recommendation recommendation, int remaining,
        AdviceMode mode, String bankAge, Map<Integer, String> itemNames,
        Map<Integer, Integer> itemPrices, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel,
        boolean developerMode, Boolean requiredItemOwned, String selectedVariantName,
        CombatStyle selectedMethod, boolean bankStale, String selectedMaster,
        Map<String, String> masterNames)
    {
        PanelStatus status = recommendation == null
            ? PanelStatus.TASK_WITHOUT_LOADOUT
            : PanelStatus.TASK_WITH_LOADOUT;
        String statusMessage = recommendation == null ? "No owned loadout found" : "Ready";
        return new SlayerPanelState(task, recommendation, remaining, mode, bankAge, copyNames(itemNames),
            copyPrices(itemPrices), refreshSource, updatedAt, status, statusMessage, safeDebug(debug),
            selectedLocationName, playerSlayerLevel, developerMode, requiredItemOwned,
            selectedVariantName, selectedMethod, bankStale, selectedMaster,
            copyMasterNames(masterNames));
    }

    /**
     * Bank-gate state (ADR-0003): a task is detected but {@code InventoryService.bankLastSeen()} is
     * {@code null}, so no loadout can be produced. Status is {@link PanelStatus#BANK_NOT_SCANNED}, the
     * recommendation is {@code null}, and Task + Where/How still render. Carries {@code task} and
     * {@code developerMode} so the side panel can show task intel and keep Diagnostics reachable.
     */
    public static SlayerPanelState bankNotScanned(TaskData task, int remaining, AdviceMode mode,
        String bankAge, Map<Integer, String> itemNames, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel,
        boolean developerMode, Boolean requiredItemOwned)
    {
        return bankNotScanned(task, remaining, mode, bankAge, itemNames, refreshSource, updatedAt,
            debug, selectedLocationName, playerSlayerLevel, developerMode, requiredItemOwned, null,
            null);
    }

    /**
     * Bank-gate state additionally carrying the selected master + display names (WA-11): the gate
     * still renders the Task card, so the master selector/context must survive it. The overload above
     * delegates here with {@code null}/{@code null} (master-less) for existing call sites.
     */
    public static SlayerPanelState bankNotScanned(TaskData task, int remaining, AdviceMode mode,
        String bankAge, Map<Integer, String> itemNames, RefreshSource refreshSource, Instant updatedAt,
        SlayerDebugSnapshot debug, String selectedLocationName, int playerSlayerLevel,
        boolean developerMode, Boolean requiredItemOwned, String selectedMaster,
        Map<String, String> masterNames)
    {
        return new SlayerPanelState(task, null, remaining, mode, bankAge, copyNames(itemNames),
            Collections.emptyMap(), refreshSource, updatedAt, PanelStatus.BANK_NOT_SCANNED,
            "Open your bank once so All-In Slayer can read your gear.", safeDebug(debug),
            selectedLocationName, playerSlayerLevel, developerMode, requiredItemOwned, null, null,
            false, selectedMaster, copyMasterNames(masterNames));
    }

    private static Map<Integer, String> copyNames(Map<Integer, String> itemNames)
    {
        if (itemNames == null || itemNames.isEmpty())
        {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(itemNames));
    }

    private static Map<Integer, Integer> copyPrices(Map<Integer, Integer> itemPrices)
    {
        if (itemPrices == null || itemPrices.isEmpty())
        {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(itemPrices));
    }

    private static Map<String, String> copyMasterNames(Map<String, String> masterNames)
    {
        if (masterNames == null || masterNames.isEmpty())
        {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(masterNames));
    }

    private static SlayerDebugSnapshot safeDebug(SlayerDebugSnapshot debug)
    {
        return debug == null ? SlayerDebugSnapshot.empty() : debug;
    }
}

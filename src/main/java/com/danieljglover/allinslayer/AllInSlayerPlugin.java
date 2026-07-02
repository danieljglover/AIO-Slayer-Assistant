package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.bank.InventoryService;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.data.SlayerDataService;
import com.danieljglover.allinslayer.integration.InventorySetupsExporter;
import com.danieljglover.allinslayer.loadout.LoadoutAdvisor;
import com.danieljglover.allinslayer.loadout.LoadoutItems;
import com.danieljglover.allinslayer.loadout.PlayerStats;
import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.loadout.SlayerUnlockStateProvider;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MasterData;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.task.MenuClickedItemResolver;
import com.danieljglover.allinslayer.task.SlayerVarbits;
import com.danieljglover.allinslayer.task.TaskDetector;
import com.danieljglover.allinslayer.task.TaskRefreshTrigger;
import com.danieljglover.allinslayer.ui.RefreshSource;
import com.danieljglover.allinslayer.ui.SlayerDebugSnapshot;
import com.danieljglover.allinslayer.ui.SlayerOverlay;
import com.danieljglover.allinslayer.ui.SlayerPanel;
import com.danieljglover.allinslayer.ui.SlayerPanelState;
import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.itemstats.ItemStatChanges;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
    name = "All-In Slayer",
    description = "Bank-aware Slayer advisor: task intel and recommended loadouts",
    tags = {"slayer", "combat", "pve", "loadout", "gear", "task"}
)
public class AllInSlayerPlugin extends Plugin
{
    /** Bank-snapshot staleness threshold (DT-FE4 / PD-5): reopen-nudge past 7 days. */
    private static final long BANK_STALE_THRESHOLD_DAYS = 7;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private AllInSlayerConfig config;
    @Inject private SlayerDataService dataService;
    @Inject private TaskDetector taskDetector;
    @Inject private InventoryService inventoryService;
    @Inject private LoadoutAdvisor loadoutAdvisor;
    @Inject private SlayerUnlockStateProvider unlockStateProvider;
    @Inject private InventorySetupsExporter exporter;
    @Inject private ItemManager itemManager;
    @Inject private OverlayManager overlayManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private SlayerPanel panel;
    @Inject private SlayerOverlay overlay;

    private NavigationButton navButton;
    private AdviceMode mode;
    private volatile Recommendation lastRecommendation;
    private volatile String lastSetupName;
    private volatile String selectedLocationName;
    // MV-B7/B8 (ADR-0010/0013/0014): the user-selected monster variant + combat method. Null = the
    // default variant / recommended method. Reset on task change; selectedVariantName is also auto-seeded
    // for the Boss meta-task from varbit 4723 (MV-B8). Set by the panel variant/method combos (MV-FE1/FE3).
    private volatile String selectedVariantName;
    private volatile CombatStyle selectedMethod;
    // WA-11 (ADR-0018 #8 / PD-E): the user-selected assigning master. Null = no pick yet - recompute
    // seeds it from the seam's currentMaster() (varbit-backed default, WA-10) when that master
    // actually assigns the task. Reset on task change; validated against task.assignedBy every pass.
    private volatile String selectedMaster;
    private volatile SlayerDebugSnapshot lastDebugSnapshot = SlayerDebugSnapshot.empty();

    @Override
    protected void startUp()
    {
        dataService.load();
        mode = config.adviceMode();

        panel.setOnRefresh(() -> clientThread.invoke(() -> recompute(RefreshSource.MANUAL)));
        panel.setOnToggleMode(() ->
        {
            mode = (mode == AdviceMode.DPS) ? AdviceMode.COST : AdviceMode.DPS;
            clientThread.invoke(() -> recompute(RefreshSource.MODE_TOGGLE));
        });
        panel.setOnSelectLocation(locationName ->
        {
            selectedLocationName = locationName;
            clientThread.invoke(() -> recompute(RefreshSource.LOCATION_SELECT));
        });
        panel.setOnSelectVariant(variantName ->
        {
            selectedVariantName = variantName;
            // A manual variant pick resets the method to the new variant's recommended style (ADR-0013.5).
            selectedMethod = null;
            clientThread.invoke(() -> recompute(RefreshSource.VARIANT_SELECT));
        });
        panel.setOnSelectMethod(methodName ->
        {
            selectedMethod = parseMethod(methodName);
            clientThread.invoke(() -> recompute(RefreshSource.METHOD_SELECT));
        });
        panel.setOnSelectMaster(masterId ->
        {
            selectedMaster = masterId;
            clientThread.invoke(() -> recompute(RefreshSource.MASTER_SELECT));
        });
        panel.setOnExport(this::exportCurrent);

        navButton = NavigationButton.builder()
            .tooltip("All-In Slayer")
            .icon(ImageUtil.loadImageResource(getClass(), "/panel_icon.png"))
            .priority(6)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);

        overlay.setEnabled(config.showOverlay());
        overlayManager.add(overlay);

        clientThread.invoke(() -> recompute(RefreshSource.STARTUP));
    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged ev)
    {
        if (TaskRefreshTrigger.isTaskStateChange(ev))
        {
            clientThread.invokeLater(() -> recompute(RefreshSource.VARBIT));
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage ev)
    {
        if (TaskRefreshTrigger.isSlayerTaskMessage(ev.getType(), ev.getMessage()))
        {
            clientThread.invokeLater(() -> recompute(RefreshSource.CHAT));
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked ev)
    {
        int itemId = MenuClickedItemResolver.resolve(ev);
        int mappedItemId = itemId == -1 ? -1 : ItemVariationMapping.map(itemId);
        boolean matchedTaskCheck = TaskRefreshTrigger.isSlayerTaskCheckMenuAction(
            ev.getMenuAction(), ev.getMenuOption(), mappedItemId);
        lastDebugSnapshot = lastDebugSnapshot.withMenu(
            ev.getMenuOption(),
            ev.getMenuAction(),
            itemId,
            mappedItemId,
            matchedTaskCheck);
        if (matchedTaskCheck || "Check".equals(ev.getMenuOption()))
        {
            clientThread.invokeLater(() -> recompute(RefreshSource.MENU_CHECK));
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged ev)
    {
        int id = ev.getContainerId();
        if (id == InventoryID.BANK)
        {
            inventoryService.onBankChanged(ev.getItemContainer());
        }
        if (id == InventoryID.BANK
            || id == InventoryID.INV
            || id == InventoryID.WORN)
        {
            recompute(RefreshSource.ITEM_CONTAINER);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        // Only a login should rebuild; login/loading blips otherwise churn the render (ADR-0002).
        if (TaskRefreshTrigger.isLoggedInGameState(ev))
        {
            recompute(RefreshSource.GAME_STATE);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged ev)
    {
        // Pick up a developer-mode toggle live; ConfigChanged arrives on the EDT, so read game state
        // back on the client thread.
        if (TaskRefreshTrigger.isConfigGroupChange(ev, AllInSlayerConfig.GROUP))
        {
            clientThread.invokeLater(() -> recompute(RefreshSource.MANUAL));
        }
    }

    /** Runs on the client thread; reads game state, computes a recommendation, updates UI on the EDT. */
    private void recompute(RefreshSource refreshSource)
    {
        int targetId = client.getVarpValue(SlayerVarbits.SLAYER_TARGET);
        int remaining = client.getVarpValue(SlayerVarbits.SLAYER_COUNT);
        int areaId = client.getVarpValue(SlayerVarbits.SLAYER_AREA);
        int bossTargetId = readBossTargetVarbit();
        Optional<TaskData> directTask = targetId > 0 ? dataService.byTargetVarp(targetId) : Optional.empty();
        Optional<TaskData> task = directTask.isPresent() ? directTask : taskDetector.resolveCurrentTask();
        String detectorResult = detectorResult(targetId, directTask.isPresent(), task);
        int unsupportedTargetId = targetId > 0 && !task.isPresent() ? targetId : -1;
        SlayerDebugSnapshot debug = lastDebugSnapshot.withSlayerState(
            targetId,
            remaining,
            areaId,
            bossTargetId,
            detectorResult,
            unsupportedTargetId);
        lastDebugSnapshot = debug;

        // One bank-snapshot read drives the age text, the gate decision, and the staleness flag off
        // the same instant (DT-FE4 / G4), so they can never disagree within a render.
        final Long bankLastSeen = inventoryService.bankLastSeen();
        final Instant bankNow = Instant.now();
        String bankAge = bankAgeText(bankLastSeen, bankNow);
        final boolean bankGate = isBankGate(task.isPresent(), bankLastSeen);
        final boolean bankStale = bankStale(bankLastSeen, bankNow);
        if (!task.isPresent())
        {
            lastRecommendation = null;
            lastSetupName = null;
            selectedLocationName = null;
            overlay.setTaskName(null);
            overlay.setRemaining(0);
            overlay.setMethod(null);

            SlayerPanelState state = targetId > 0
                ? SlayerPanelState.unsupportedTask(targetId, remaining, mode, bankAge, refreshSource, Instant.now(), debug, config.developerMode())
                : SlayerPanelState.noTask(remaining, mode, bankAge, null, refreshSource, Instant.now(), debug, config.developerMode());
            SwingUtilities.invokeLater(() -> panel.render(state));
            return;
        }

        TaskData t = task.get();
        if (lastSetupName != null && !lastSetupName.equals(t.getTask()))
        {
            // Task changed: clear all per-task selections so a stale pick never leaks across tasks.
            selectedLocationName = null;
            selectedVariantName = null;
            selectedMethod = null;
            selectedMaster = null;
        }
        if (!hasLocation(t, selectedLocationName))
        {
            selectedLocationName = null;
        }
        // MV-B7: drop a stale variant selection that is not one of this task's variants.
        if (!hasVariant(t, selectedVariantName))
        {
            selectedVariantName = null;
        }
        // MV-B8 (ADR-0014): for the Boss meta-task, auto-seed the live-rolled boss from varbit 4723 when
        // the user has not overridden. Unmapped/unknown falls through to the deterministic default.
        selectedVariantName = resolveBossVariantName(t, bossTargetId, selectedVariantName);
        // WA-11 (ADR-0018 #8 / PD-E): drop a stale master pick that does not assign this task, then
        // seed an empty selection from the seam's current-master signal (varbit 4067 when on task,
        // else the config primary master - WA-10). A signal for a master that does not assign this
        // task is ignored - the panel then defaults to the first assigning master, never fabricates.
        if (!isAssignedBy(t, selectedMaster))
        {
            selectedMaster = null;
        }
        if (selectedMaster == null)
        {
            selectedMaster = unlockStateProvider.currentMaster()
                .filter(masterId -> isAssignedBy(t, masterId))
                .orElse(null);
        }
        final String stateSelectedMaster = selectedMaster;
        final Map<String, String> masterNames = masterNames(t);
        OwnedItems owned = inventoryService.currentOwned();
        PlayerStats stats = buildStats();
        // Authoritative required-item ownership (B1): resolved here where the owned set is in scope, so
        // the panel never has to infer it from worn/inventory. Null only when the task has no required item.
        final Boolean requiredItemOwned = t.getRequiredItemId() == null
            ? null
            : owned.has(t.getRequiredItemId());

        // Bank gate (ADR-0003 / plan §6): without a persisted bank snapshot we cannot read the player's
        // gear, so produce no loadout - Task and Where/How still render. Opening the bank once unlocks it.
        if (bankGate)
        {
            lastRecommendation = null;
            lastSetupName = t.getTask();
            overlay.setEnabled(config.showOverlay());
            overlay.setTaskName(t.getTask());
            overlay.setRemaining(remaining);
            overlay.setMethod(null);

            final SlayerPanelState gateState = SlayerPanelState.bankNotScanned(
                t,
                remaining,
                mode,
                bankAge,
                null,
                refreshSource,
                Instant.now(),
                debug,
                selectedLocationName,
                stats.getSlayer(),
                config.developerMode(),
                requiredItemOwned,
                stateSelectedMaster,
                masterNames);
            SwingUtilities.invokeLater(() -> panel.render(gateState));
            return;
        }

        // WD-12 (ADR-0020 #5): the config-declared disliked/blocked task set drives the skip/block note.
        java.util.Set<String> dislikedTasks = parseDislikedTasks(config.dislikedTasks());
        Optional<Recommendation> rec = loadoutAdvisor.recommend(t, owned, stats, mode, config.haveCannon(),
            selectedLocationName, selectedVariantName, selectedMethod, stateSelectedMaster, dislikedTasks);
        String stateSelectedLocation = rec
            .map(Recommendation::getLocation)
            .map(SlayerLocation::getName)
            .orElse(selectedLocationName);

        lastRecommendation = rec.orElse(null);
        lastSetupName = t.getTask();

        overlay.setEnabled(config.showOverlay());
        overlay.setTaskName(t.getTask());
        overlay.setRemaining(remaining);
        overlay.setMethod(rec.map(Recommendation::getMethod).orElse(null));

        // Resolve item names and GE prices here, off one id list so they cover the same items;
        // ItemManager must not be called from the EDT.
        final List<Integer> itemIds = LoadoutItems.ids(lastRecommendation);
        final Map<Integer, String> names = collectNames(itemIds);
        final Map<Integer, Integer> prices = LoadoutItems.prices(itemIds, itemManager::getItemPrice);

        final SlayerPanelState state = SlayerPanelState.forTask(
            t,
            lastRecommendation,
            remaining,
            mode,
            bankAge,
            names,
            prices,
            refreshSource,
            Instant.now(),
            debug,
            stateSelectedLocation,
            stats.getSlayer(),
            config.developerMode(),
            requiredItemOwned,
            selectedVariantName,
            selectedMethod,
            bankStale,
            stateSelectedMaster,
            masterNames);
        SwingUtilities.invokeLater(() -> panel.render(state));
    }

    /**
     * Display names for the task's assigning masters, keyed by masterId, resolved from the runtime
     * masters catalogue (WA-8 {@code slayer-meta.json}). An unresolved master (degraded meta / stub
     * file) is simply absent - the panel prettifies the slug instead, never blank, never fabricated.
     */
    private Map<String, String> masterNames(TaskData task)
    {
        Map<String, String> names = new HashMap<>();
        if (task.getAssignedBy() != null)
        {
            for (String masterId : task.getAssignedBy())
            {
                if (masterId != null)
                {
                    dataService.masterById(masterId)
                        .map(MasterData::getName)
                        .ifPresent(name -> names.put(masterId, name));
                }
            }
        }
        return names;
    }

    /**
     * The config-declared disliked/blocked task names (WD-12 / ADR-0020 #5) parsed from the
     * comma-separated config string into a set of trimmed, non-blank names. Empty when unset - the
     * skip/block advisory is then inert. The advisor matches case-insensitively.
     */
    private static java.util.Set<String> parseDislikedTasks(String raw)
    {
        java.util.Set<String> names = new java.util.HashSet<>();
        if (raw == null || raw.trim().isEmpty())
        {
            return names;
        }
        for (String part : raw.split(","))
        {
            String name = part.trim();
            if (!name.isEmpty())
            {
                names.add(name);
            }
        }
        return names;
    }

    private Map<Integer, String> collectNames(List<Integer> ids)
    {
        Map<Integer, String> names = new HashMap<>();
        for (Integer id : ids)
        {
            names.put(id, itemManager.getItemComposition(id).getName());
        }
        return names;
    }

    private void exportCurrent()
    {
        Recommendation rec = lastRecommendation;
        if (rec == null)
        {
            return;
        }
        String json = exporter.buildImportString(rec, lastSetupName);
        exporter.copyToClipboard(json);
        log.debug("Copied Inventory Setups import string for {}", lastSetupName);
    }

    private PlayerStats buildStats()
    {
        return new PlayerStats(
            client.getRealSkillLevel(Skill.ATTACK),
            client.getRealSkillLevel(Skill.STRENGTH),
            client.getRealSkillLevel(Skill.DEFENCE),
            client.getRealSkillLevel(Skill.RANGED),
            client.getRealSkillLevel(Skill.MAGIC),
            client.getRealSkillLevel(Skill.SLAYER));
    }

    /**
     * Humane bank-snapshot age (DT-FE4 / G4): compact tiers that feed the "Bank seen" row -
     * "just now" (&lt;1m), "Nm ago" (&lt;1h), "Nh ago" (&lt;24h), "Nd ago" (past a day). A null
     * timestamp (never seen) returns null - that case is the {@code BANK_NOT_SCANNED} gate's job, not
     * this text. Pure + static so the tiering is unit-testable in isolation (like {@link #isBankGate}).
     */
    static String bankAgeText(Long lastSeenEpochMillis, Instant now)
    {
        if (lastSeenEpochMillis == null)
        {
            return null;
        }
        long mins = Duration.between(Instant.ofEpochMilli(lastSeenEpochMillis), now).toMinutes();
        if (mins < 1)
        {
            return "just now";
        }
        if (mins < 60)
        {
            return mins + "m ago";
        }
        if (mins < 1440)
        {
            return (mins / 60) + "h ago";
        }
        return (mins / 1440) + "d ago";
    }

    /**
     * Whether the persisted bank snapshot is stale enough to nudge a reopen (DT-FE4 / PD-5 = 7 days).
     * A null timestamp (never seen) is NOT stale - that is the {@code BANK_NOT_SCANNED} gate's job, so
     * this flag only fires when we HAVE a snapshot and it has aged past the threshold.
     */
    static boolean bankStale(Long lastSeenEpochMillis, Instant now)
    {
        if (lastSeenEpochMillis == null)
        {
            return false;
        }
        return Duration.between(Instant.ofEpochMilli(lastSeenEpochMillis), now).toDays()
            >= BANK_STALE_THRESHOLD_DAYS;
    }

    private int readBossTargetVarbit()
    {
        try
        {
            return client.getVarbitValue(SlayerVarbits.SLAYER_TARGET_BOSSID);
        }
        catch (RuntimeException e)
        {
            return -1;
        }
    }

    private static String detectorResult(int targetId, boolean matchedDirectTarget, Optional<TaskData> task)
    {
        if (targetId <= 0)
        {
            return "No Slayer target varp";
        }
        return task.map(taskData -> matchedDirectTarget
                ? "Matched " + taskData.getTask()
                : "Matched " + taskData.getTask() + " via RuneLite task DB")
            .orElse("Unsupported Slayer target");
    }

    /**
     * Pure bank-gate decision (ADR-0003 / plan §6): a loadout can only be produced once the player's
     * gear has been read, which requires a persisted bank snapshot. Returns true when a task is present
     * but {@code InventoryService.bankLastSeen()} has never been recorded.
     */
    static boolean isBankGate(boolean taskPresent, Long bankLastSeen)
    {
        return taskPresent && bankLastSeen == null;
    }

    private static boolean hasLocation(TaskData task, String locationName)
    {
        if (locationName == null || task == null || task.getLocations() == null)
        {
            return false;
        }
        for (SlayerLocation location : task.getLocations())
        {
            if (locationName.equals(location.getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * WA-11 (ADR-0018 #8): whether {@code masterId} (a possibly-stale selection or a seam
     * {@code currentMaster()} signal) is one of the task's assigning masters. Unlike
     * {@link #hasVariant}, {@code null} is NOT a valid pick here - it means "no selection", which the
     * seeding path then tries to fill; a task with no {@code assignedBy} list validates nothing.
     */
    static boolean isAssignedBy(TaskData task, String masterId)
    {
        if (task == null || masterId == null || task.getAssignedBy() == null)
        {
            return false;
        }
        return task.getAssignedBy().contains(masterId);
    }

    /** MV-B7: whether {@code variantName} (a possibly-stale selection) is one of the task's variants. */
    static boolean hasVariant(TaskData task, String variantName)
    {
        if (variantName == null || task == null || task.getVariants() == null)
        {
            return variantName == null; // null = "no selection", which is always valid (-> default)
        }
        for (MonsterVariant v : task.getVariants())
        {
            if (variantName.equals(v.getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * MV-B8 (ADR-0014): resolve the selected boss-variant name for the Boss meta-task from the live
     * {@code SLAYER_TARGET_BOSSID} varbit. A user override (non-null {@code current}) always wins. With no
     * override, match a boss variant whose {@code bossId} equals the varbit; an unmapped/absent varbit
     * (or a non-boss task) returns {@code current} unchanged so resolution falls through to the
     * deterministic default - never fabricated.
     */
    static String resolveBossVariantName(TaskData task, int bossVarbit, String current)
    {
        if (current != null || task == null || !isBossTask(task) || bossVarbit <= 0)
        {
            return current;
        }
        for (MonsterVariant v : task.getVariants())
        {
            if (v.getBossId() != null && v.getBossId() == bossVarbit)
            {
                return v.getName();
            }
        }
        return current; // unmapped -> deterministic default
    }

    /** A Boss meta-task is one whose variants are all bosses (ADR-0014). */
    static boolean isBossTask(TaskData task)
    {
        if (task == null || task.getVariants() == null || task.getVariants().isEmpty())
        {
            return false;
        }
        for (MonsterVariant v : task.getVariants())
        {
            if (!v.isBoss())
            {
                return false;
            }
        }
        return true;
    }

    /** Parse a method-combo label ("Melee"/"Ranged"/"Magic") to a {@link CombatStyle}; null if blank. */
    static CombatStyle parseMethod(String methodName)
    {
        if (methodName == null || methodName.trim().isEmpty())
        {
            return null;
        }
        try
        {
            return CombatStyle.valueOf(methodName.trim().toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    @Provides
    AllInSlayerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AllInSlayerConfig.class);
    }

    /**
     * Binds {@code ItemStatChangesService} into this plugin's injector (LFA). RuneLite only binds it
     * inside {@code ItemStatPlugin}'s own child injector, so we cannot inject it directly. Instead we
     * adapt the public {@code ItemStatChanges} - a {@code @Singleton} with a no-arg constructor that
     * Guice just-in-time constructs here - whose {@code get(int)} matches the service's single method.
     */
    @Provides
    @Singleton
    ItemStatChangesService provideItemStatChangesService(ItemStatChanges itemStatChanges)
    {
        return itemStatChanges::get;
    }
}

package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.bank.advisor.PlayerStateCapture;
import com.danieljglover.allinslayer.bank.advisor.ChargeStateCapture;
import com.danieljglover.allinslayer.bank.advisor.LootingBagCapture;
import com.danieljglover.allinslayer.integration.advisor.WeaponChargesBridge;
import com.danieljglover.allinslayer.data.AdvisorDataService;
import com.danieljglover.allinslayer.integration.advisor.BankSetupFilter;
import com.danieljglover.allinslayer.integration.advisor.SetupExporter;
import com.danieljglover.allinslayer.integration.advisor.ShortestPathBridge;
import com.danieljglover.allinslayer.loadout.advisor.RecommendationEngine;
import com.danieljglover.allinslayer.model.advisor.BoostReservations;
import com.danieljglover.allinslayer.model.advisor.FoodOverride;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.RouteDestination;
import com.danieljglover.allinslayer.task.advisor.ActiveTaskReader;
import com.danieljglover.allinslayer.task.advisor.ActiveTaskReader.ActiveTask;
import com.danieljglover.allinslayer.ui.advisor.AdvisorOverlay;
import com.danieljglover.allinslayer.ui.advisor.AdvisorPanel;
import com.google.inject.Provides;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDependency(BankTagsPlugin.class)
@PluginDescriptor(name = "All-In Slayer", description = "Wiki-first Slayer planning with the gear you own",
    tags = {"slayer", "tasks", "gear", "bank", "locations"})
public class AllInSlayerPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private AllInSlayerConfig config;
    @Inject private ConfigManager configManager;
    @Inject private AdvisorDataService data;
    @Inject private PlayerStateCapture playerState;
    @Inject private ChargeStateCapture chargeState;
    @Inject private LootingBagCapture lootingBag;
    @Inject private WeaponChargesBridge weaponCharges;
    @Inject private ActiveTaskReader taskReader;
    @Inject private ItemManager itemManager;
    @Inject private ClientToolbar toolbar;
    @Inject private OverlayManager overlays;
    @Inject private ScheduledExecutorService executor;
    @Inject private ShortestPathBridge shortestPath;
    @Inject private BankSetupFilter bankFilter;
    @Inject private SetupExporter exporter;

    private final RecommendationEngine engine = new RecommendationEngine();
    private final AtomicReference<Work> pending = new AtomicReference<>();
    private final AtomicBoolean computing = new AtomicBoolean();
    private final AtomicLong revision = new AtomicLong();
    private volatile boolean started;
    private volatile AdvisorPanel.Selection selection;
    private AdvisorPanel panel;
    private AdvisorOverlay overlay;
    private NavigationButton navigation;
    private boolean dirty;
    private boolean chargesAvailable;
    private int retryTicks;
    private String activeIdentity;
    private int lastDeathStateFingerprint;
    private RecommendationRequest displayedRequest;
    private RecommendationResult displayedResult;
    private RecommendationRequest lastCapturedRequest;
    private ActiveTask lastCapturedTask;

    @Provides
    AllInSlayerConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(AllInSlayerConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        data.load();
        started = true;
        selection = new AdvisorPanel.Selection(false, null, null, null, null, null,
            config.recommendationGoal(), config.allowWilderness(), config.allowGroups(), config.returnDestination());
        onEdtAndWait(() ->
        {
            panel = new AdvisorPanel();
            overlay = new AdvisorOverlay();
            panel.setItemManager(itemManager);
            panel.setCatalogue(data.getCatalogue());
            panel.setPreferences(config.recommendationGoal(), config.allowWilderness(), config.allowGroups(), config.returnDestination());
            panel.setOnSelection(this::select);
            panel.setOnRefresh(() -> clientThread.invokeLater(() ->
            {
                lastCapturedRequest = null;
                capture();
            }));
            panel.setOnConfirmRequirement(requirement -> clientThread.invokeLater(() ->
            {
                playerState.confirm(requirement);
                capture();
            }));
            panel.setOnClearConfirmations(() -> clientThread.invokeLater(() ->
            {
                playerState.clearConfirmations();
                capture();
            }));
            panel.setOnExport(this::export);
            panel.setOnBankFilter(this::filterBank);
            panel.setOnRoute(this::route);
            panel.setOnClearRoute(() -> clientThread.invokeLater(shortestPath::clear));
            navigation = NavigationButton.builder().tooltip("All-In Slayer")
                .icon(ImageUtil.loadImageResource(getClass(), "/icons/aio-slayer-assistant.png"))
                .priority(6).panel(panel).build();
            toolbar.addNavigation(navigation);
            overlay.setEnabled(config.showOverlay());
            overlays.add(overlay);
        });
        clientThread.invokeLater(() ->
        {
            if (!started) { return; }
            weaponCharges.start();
            lootingBag.reset();
            bankFilter.start(state -> SwingUtilities.invokeLater(() -> {
                if (started && panel != null) { panel.setBankFilterState(state); }
            }));
            shortestPath.setListener(state -> SwingUtilities.invokeLater(() -> {
                if (started && panel != null) { panel.setRouteState(state); }
            }));
            SwingUtilities.invokeLater(() -> {
                if (started && panel != null) { panel.setRouteState(shortestPath.getState()); }
            });
            dirty = true;
            retryTicks = 0;
            capture();
        });
    }

    @Override
    protected void shutDown() throws Exception
    {
        started = false;
        revision.incrementAndGet();
        pending.set(null);
        selection = null;
        activeIdentity = null;
        lastCapturedRequest = null;
        lastCapturedTask = null;
        clientThread.invokeLater(() -> {
            bankFilter.stop();
            shortestPath.stop();
            weaponCharges.stop();
            lootingBag.reset();
            playerState.clear();
        });
        onEdtAndWait(() ->
        {
            displayedRequest = null;
            displayedResult = null;
            if (navigation != null)
            {
                toolbar.removeNavigation(navigation);
            }
            if (overlay != null)
            {
                overlays.remove(overlay);
            }
            panel = null;
            overlay = null;
        });
    }

    private void select(AdvisorPanel.Selection next)
    {
        selection = next;
        displayedResult = null;
        revision.incrementAndGet();
        // Only preference changes write configuration; browsing never mutates the active assignment.
        if (next.getGoal() != config.recommendationGoal())
        {
            configManager.setConfiguration(AllInSlayerConfig.GROUP, "recommendationGoal", next.getGoal());
        }
        if (next.getReturnDestination() != config.returnDestination())
        {
            configManager.setConfiguration(AllInSlayerConfig.GROUP, "returnDestination", next.getReturnDestination());
        }
        if (next.isAllowWilderness() != config.allowWilderness())
        {
            configManager.setConfiguration(AllInSlayerConfig.GROUP, "allowWilderness", next.isAllowWilderness());
        }
        if (next.isAllowGroups() != config.allowGroups())
        {
            configManager.setConfiguration(AllInSlayerConfig.GROUP, "allowGroups", next.isAllowGroups());
        }
        clientThread.invokeLater(() ->
        {
            bankFilter.reset();
            shortestPath.clear();
            lastCapturedRequest = null;
            capture();
        });
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        int id = event.getContainerId();
        if (lootingBag.observe(event)) { dirty = true; }
        if (id == InventoryID.BANK)
        {
            playerState.bankChanged();
            shortestPath.bankObserved();
        }
        if (id == InventoryID.BANK || id == InventoryID.INV || id == InventoryID.WORN || id == InventoryID.LOOTING_BAG)
        {
            dirty = true;
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        chargeState.observe(event);
        lootingBag.observe(event);
        dirty = true;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        chargeState.observe(event);
        lootingBag.observe(event);
        dirty = true;
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (lootingBag.observe(event)) { dirty = true; }
        if (event.getActor() == client.getLocalPlayer() && event.getActor().getAnimation() != -1)
        {
            chargeState.equipmentUsed();
            dirty = true;
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event)
    {
        if (lootingBag.observe(event)) { dirty = true; }
    }

    @Subscribe
    public void onItemQuantityChanged(ItemQuantityChanged event)
    {
        if (lootingBag.observe(event)) { dirty = true; }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (event.getActor() == client.getLocalPlayer()) { chargeState.equipmentUsed(); dirty = true; }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (event.getActor() == client.getLocalPlayer())
        {
            lootingBag.invalidate();
            chargeState.invalidate();
            dirty = true;
        }
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        if (WeaponChargesBridge.PLUGIN_CLASS.equals(event.getPlugin().getClass().getName()))
        {
            weaponCharges.pluginChanged();
            clientThread.invokeLater(() -> { lastCapturedRequest = null; dirty = true; });
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        // Quest/access gates can change on varbits beyond the Slayer assignment itself.
        dirty = true;
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        dirty = true;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (playerState.settlePreparation()) { dirty = true; }
        if (chargesAvailable != weaponCharges.isAvailable())
        {
            chargesAvailable = weaponCharges.isAvailable();
            dirty = true;
        }
        if (lootingBag.gameTick()) { dirty = true; }
        bankFilter.refresh();
        shortestPath.refresh();
        // Capture inventory and bank together after the tick's container updates have settled.
        // Periodic refresh also recovers item stats that loaded asynchronously after the first scan.
        int deathFingerprint = playerState.deathStateFingerprint();
        if (deathFingerprint != lastDeathStateFingerprint)
        {
            lastDeathStateFingerprint = deathFingerprint;
            dirty = true;
        }
        boolean periodic = ++retryTicks >= 50;
        if (dirty || periodic)
        {
            if (periodic)
            {
                lastCapturedRequest = null;
            }
            retryTicks = 0;
            capture();
        }
    }

    @Subscribe(priority = -2)
    public void onScriptPreFired(ScriptPreFired event)
    {
        if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING) { bankFilter.finishLayout(); }
    }

    @Subscribe(priority = -2)
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == ScriptID.BANKMAIN_BUILD) { bankFilter.afterBankBuild(); }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.BANKMAIN)
        {
            clientThread.invokeLater(() -> { if (started) { bankFilter.refresh(); } });
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == InterfaceID.BANKMAIN && event.isUnload())
        {
            // Widget unload can be emitted inside a game script; bank relayouts are not reentrant.
            clientThread.invokeLater(bankFilter::clear);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        lootingBag.observe(event);
        dirty = true;
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            revision.incrementAndGet();
            pending.set(null);
            lastCapturedRequest = null;
            playerState.invalidatePreparation();
            chargeState.invalidate();
            invalidatePreparation();
        }
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.LOGGING_IN)
        {
            clientThread.invokeLater(bankFilter::reset);
            shortestPath.accountChanged();
            playerState.clear();
            lootingBag.reset();
            activeIdentity = null;
            clientThread.invokeLater(this::capture);
        }
        else if (event.getGameState() == GameState.LOGGED_IN)
        {
            clientThread.invokeLater(this::capture);
        }
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        revision.incrementAndGet();
        pending.set(null);
        invalidatePreparation();
        clientThread.invokeLater(() ->
        {
            bankFilter.reset();
            shortestPath.accountChanged();
            playerState.clear();
            lootingBag.reset();
            lastCapturedRequest = null;
            activeIdentity = null;
            capture();
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (WeaponChargesBridge.CONFIG_GROUP.equals(event.getGroup()))
        {
            // Most provider updates originate in the same game-message callback as our Check.
            // Preserve that ordering so an older queued balance cannot erase a newer check.
            if (client.isClientThread()) { chargeState.providerChanged(event); dirty = true; }
            else { clientThread.invokeLater(() -> { chargeState.providerChanged(event); dirty = true; }); }
            return;
        }
        if ("shortestpath".equals(event.getGroup()) && ("usePoh".equals(event.getKey())
            || "pohJewelleryBoxTier".equals(event.getKey()) || "usePohMountedItems".equals(event.getKey())))
        {
            clientThread.invokeLater(() -> {
                lastCapturedRequest = null;
                capture();
            });
            return;
        }
        if (!AllInSlayerConfig.GROUP.equals(event.getGroup()) || event.getKey().startsWith("advisor"))
        {
            return;
        }
        revision.incrementAndGet();
        SwingUtilities.invokeLater(() ->
        {
            if (!started || panel == null)
            {
                return;
            }
            displayedResult = null;
            panel.setPreferences(config.recommendationGoal(), config.allowWilderness(), config.allowGroups(), config.returnDestination());
            overlay.setEnabled(config.showOverlay());
            AdvisorPanel.Selection previous = selection;
            if (previous != null)
            {
                selection = new AdvisorPanel.Selection(previous.isBrowsing(), previous.getTaskId(),
                    previous.getMasterId(), previous.getMonsterId(), previous.getLocationId(), previous.getMethodId(),
                    config.recommendationGoal(), config.allowWilderness(), config.allowGroups(), config.returnDestination());
            }
            clientThread.invokeLater(() ->
            {
                bankFilter.reset();
                if ("allowWilderness".equals(event.getKey())) { shortestPath.clear(); }
                lastCapturedRequest = null;
                capture();
            });
        });
    }

    private void capture()
    {
        if (!started || selection == null)
        {
            return;
        }
        dirty = false;
        try
        {
            ActiveTask active = taskReader.read(data.getCatalogue());
            String identity = assignmentIdentity(active);
            AdvisorPanel.Selection chosen = selection;
            if (!Objects.equals(activeIdentity, identity))
            {
                activeIdentity = identity;
                if (!chosen.isBrowsing())
                {
                    chosen = new AdvisorPanel.Selection(false, active.getTaskId(), active.getMasterId(),
                        active.getMonsterId(), null, null, chosen.getGoal(), chosen.isAllowWilderness(), chosen.isAllowGroups(), chosen.getReturnDestination());
                    selection = chosen;
                }
            }
            boolean live = !chosen.isBrowsing();
            PlayerSnapshot player = playerState.capture();
            RecommendationRequest request = new RecommendationRequest(
                live ? active.getTaskId() : chosen.getTaskId(),
                live ? active.getMasterId() : chosen.getMasterId(),
                live && active.getMonsterId() != null ? active.getMonsterId() : chosen.getMonsterId(),
                chosen.getLocationId(), chosen.getMethodId(), live, live ? active.getRemaining() : 0,
                live ? active.getLockedLocationId() : null, live && active.isWilderness(),
                chosen.getGoal(), chosen.isAllowWilderness(), chosen.isAllowGroups(), player,
                live ? active.getAllowedMonsterIds() : Collections.emptySet(), config.wildernessRiskBudget(),
                config.plannedEtherCharges(), chosen.getReturnDestination(), config.wildernessChargeLimit(),
                new BoostReservations(config.meleeBoostSlots(), config.meleeBoostItems(),
                    config.rangedBoostSlots(), config.rangedBoostItems(), config.magicBoostSlots(), config.magicBoostItems()),
                new FoodOverride(config.overrideFood(), config.preferredFoods()));
            if (request.equals(lastCapturedRequest) && active.equals(lastCapturedTask))
            {
                return;
            }
            lastCapturedRequest = request;
            lastCapturedTask = active;
            long sequence = revision.incrementAndGet();
            invalidatePreparation();
            pending.set(new Work(sequence, request, active));
            submit();
        }
        catch (RuntimeException ex)
        {
            revision.incrementAndGet();
            pending.set(null);
            lastCapturedRequest = null;
            invalidatePreparation();
            log.warn("Unable to capture Slayer preparation state", ex);
            SwingUtilities.invokeLater(() ->
            {
                if (started && panel != null)
                {
                    panel.showStatus("Game data is not ready. Open your bank or refresh to try again.");
                }
            });
        }
    }

    private void invalidatePreparation()
    {
        SwingUtilities.invokeLater(() -> {
            if (started && panel != null) { panel.invalidatePreparation(); }
        });
    }

    private void submit()
    {
        if (computing.compareAndSet(false, true))
        {
            executor.execute(() ->
            {
                try
                {
                    Work work;
                    while (started && (work = pending.getAndSet(null)) != null)
                    {
                        Work current = work;
                        RecommendationResult result;
                        try
                        {
                            result = engine.recommend(data.getCatalogue(), current.getRequest(),
                                () -> !started || current.getRevision() != revision.get());
                        }
                        catch (java.util.concurrent.CancellationException ignored)
                        {
                            continue;
                        }
                        catch (RuntimeException ex)
                        {
                            log.warn("Unable to calculate Slayer preparation", ex);
                            SwingUtilities.invokeLater(() ->
                            {
                                if (started && panel != null && current.getRevision() == revision.get())
                                {
                                    displayedResult = null;
                                    clientThread.invokeLater(() -> {
                                        if (revision.get() == current.getRevision()) { bankFilter.reset(); }
                                    });
                                    panel.showStatus("This recommendation could not be calculated. Refresh to try again.");
                                }
                            });
                            continue;
                        }
                        SwingUtilities.invokeLater(() -> publish(current, result));
                    }
                }
                finally
                {
                    computing.set(false);
                    if (started && pending.get() != null)
                    {
                        submit();
                    }
                }
            });
        }
    }

    private void publish(Work work, RecommendationResult result)
    {
        if (!started || panel == null || work.getRevision() != revision.get())
        {
            return;
        }
        displayedRequest = work.getRequest();
        displayedResult = result;
        ActiveTask active = work.getActive();
        panel.setActiveAssignmentIdentity(assignmentIdentity(active));
        panel.setActiveAllowedMonsters(active.getAllowedMonsterIds());
        panel.setActiveTask(active.getTaskId(), active.getRemaining(), active.getStatus());
        panel.render(work.getRequest(), result);
        RecommendationResult.Setup best = result.best();
        RecommendationResult.Setup selected = best == null && !result.getSetups().isEmpty() ? result.getSetups().get(0) : best;
        clientThread.invokeLater(() -> {
            if (started && revision.get() == work.getRevision())
            {
                bankFilter.setSetup(work.getRequest().getPlayer().isLoggedIn() ? selected : null, work.getRequest().getPlayer());
            }
        });
        String routedId = shortestPath.getState().getRouteId();
        RouteDestination routed = data.getCatalogue().getRouteDestinations().get(routedId);
        if (routed != null && (selected == null || !routed.getLocationId().equals(selected.getLocationId())
            || !routed.getMonsterIds().contains(selected.getMonsterId())))
        {
            // A bank/task update can change the recommendation without a selector click.
            clientThread.invokeLater(() -> {
                if (revision.get() == work.getRevision() && routedId.equals(shortestPath.getState().getRouteId()))
                {
                    shortestPath.clear();
                }
            });
        }
        overlay.update(active.getTaskId() == null ? "" : active.getStatus(),
            !work.getRequest().isActiveTask() || best == null ? "" : best.getTitle());
    }

    private void export()
    {
        if (displayedResult == null || displayedRequest == null || displayedResult.best() == null)
        {
            panel.showStatus("Choose a feasible setup before exporting.");
            return;
        }
        try
        {
            String encoded = exporter.export(displayedResult.best(), displayedRequest.getPlayer(), displayedRequest.isTaskPreview());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(encoded), null);
            panel.showStatus("Setup copied. Import it in Inventory Setups inside RuneLite.");
        }
        catch (HeadlessException | IllegalStateException | SecurityException ex)
        {
            panel.showStatus("Clipboard is unavailable. The equipment and checklist remain in this panel.");
        }
    }

    private void filterBank(boolean enabled)
    {
        if (!enabled)
        {
            clientThread.invokeLater(bankFilter::clear);
            return;
        }
        if (displayedResult == null || displayedRequest == null || !displayedRequest.getPlayer().isLoggedIn()) { return; }
        RecommendationResult.Setup best = displayedResult.best();
        RecommendationResult.Setup selected = best == null && !displayedResult.getSetups().isEmpty()
            ? displayedResult.getSetups().get(0) : best;
        PlayerSnapshot player = displayedRequest.getPlayer();
        long requestedRevision = revision.get();
        clientThread.invokeLater(() -> {
            if (started && requestedRevision == revision.get())
            {
                bankFilter.setSetup(selected, player);
                bankFilter.open();
            }
        });
    }

    private void route(RouteDestination destination)
    {
        if (displayedResult == null || displayedRequest == null) { return; }
        RecommendationResult.Setup setup = displayedResult.best();
        if (setup == null && !displayedResult.getSetups().isEmpty()) { setup = displayedResult.getSetups().get(0); }
        if (setup == null || !destination.getLocationId().equals(setup.getLocationId())
            || !destination.getMonsterIds().contains(setup.getMonsterId())) { return; }
        boolean wilderness = displayedRequest.isAllowWilderness();
        com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location location = data.getCatalogue().getLocations().get(setup.getLocationId());
        if (!wilderness && (location.isWilderness() || setup.getWildernessRisk() != null))
        {
            panel.showStatus("Enable Allow Wilderness before routing to this location.");
            return;
        }
        long requestedRevision = revision.get();
        clientThread.invokeLater(() -> {
            if (started && revision.get() == requestedRevision) { shortestPath.route(destination, wilderness); }
        });
    }

    private static void onEdtAndWait(Runnable action) throws Exception
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            action.run();
        }
        else
        {
            SwingUtilities.invokeAndWait(action);
        }
    }

    private static String assignmentIdentity(ActiveTask task)
    {
        return task.getTaskId() + ":" + task.getMonsterId() + ":" + task.getLockedLocationId()
            + ":" + String.join(",", task.getAllowedMonsterIds());
    }

    @Value
    private static class Work
    {
        long revision;
        RecommendationRequest request;
        ActiveTask active;
    }
}

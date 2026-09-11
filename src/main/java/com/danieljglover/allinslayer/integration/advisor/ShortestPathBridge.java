package com.danieljglover.allinslayer.integration.advisor;

import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.RouteDestination;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.Text;

/** Client-thread travel handoff. The external plugin owns pathfinding and never receives game actions. */
@Singleton
public final class ShortestPathBridge
{
    private static final String NAMESPACE = "shortestpath";
    private static final String PLUGIN_CLASS = "shortestpath.ShortestPathPlugin";
    private static final String UNAVAILABLE = "Install and enable Shortest Path in RuneLite's Plugin Hub.";
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private EventBus eventBus;
    @Inject private PluginManager pluginManager;
    @Inject private ConfigManager configManager;
    @Inject private KeyManager keyManager;
    private Consumer<State> listener;
    private volatile State state = new State(false, false, false, "", UNAVAILABLE, Phase.NONE);
    private final Set<PluginMessage> ownMessages = Collections.newSetFromMap(new IdentityHashMap<>());
    // Plugin lifecycle and hotkey observations can arrive before their client-thread callbacks.
    private final AtomicLong generation = new AtomicLong();
    private long leaseGeneration;
    private Plugin owner;
    private Plugin pendingClearOwner;
    private Plugin suspendedOwner;
    private long suspendedGeneration;
    private String routeId = "";
    private boolean overridesOwned;
    private boolean routePending;
    private boolean bankSeen;
    private final AtomicLong availabilityGeneration = new AtomicLong();
    private long lifecycleGeneration;
    private boolean registered;
    private volatile Plugin availablePlugin;
    private final KeyListener clearKeyListener = new KeyListener()
    {
        @Override public void keyTyped(KeyEvent event) { }
        @Override public void keyReleased(KeyEvent event) { }
        @Override public void keyPressed(KeyEvent event)
        {
            Keybind keybind = configManager.getConfiguration(NAMESPACE, "clearPathHotkey", Keybind.class);
            if (availablePlugin != null && keybind != null && keybind.matches(event))
            {
                // Observe Shortest Path's existing shortcut; never consume or generate input.
                observeTakeover(true, true);
            }
        }
    };

    public void setListener(Consumer<State> listener)
    {
        lifecycleGeneration++;
        if (!registered)
        {
            eventBus.register(this);
            keyManager.registerKeyListener(clearKeyListener);
            registered = true;
        }
        this.listener = listener;
        pluginChanged();
        refresh();
    }

    public State getState() { return state; }

    public void bankObserved()
    {
        if (availablePlugin != null) { bankSeen = true; }
        refresh();
    }

    public void accountChanged()
    {
        clear();
        bankSeen = false;
        refresh();
    }

    public void pluginChanged()
    {
        long request = availabilityGeneration.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            Plugin available = pluginManager.getPlugins().stream()
                .filter(candidate -> PLUGIN_CLASS.equals(candidate.getClass().getName()))
                .filter(pluginManager::isPluginActive).findFirst().orElse(null);
            clientThread.invokeLater(() -> {
                if (!registered || request != availabilityGeneration.get()) { return; }
                boolean clearSuspended = available != null && available == suspendedOwner
                    && generation.get() == suspendedGeneration;
                if (availablePlugin != available)
                {
                    generation.incrementAndGet();
                    forgetRoute();
                    availablePlugin = available;
                    bankSeen = false;
                    publish("");
                }
                if (available != null)
                {
                    suspendedOwner = null;
                    if (clearSuspended) { post(new PluginMessage(NAMESPACE, "clear")); }
                }
                refresh();
            });
        });
    }

    public void route(RouteDestination destination, boolean allowWilderness)
    {
        Plugin shortest = availablePlugin;
        if (shortest == null) { publish(UNAVAILABLE); return; }
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            publish("Log in to start a route.");
            return;
        }
        Set<WorldPoint> next = new LinkedHashSet<>();
        destination.getPoints().forEach(point -> next.add(new WorldPoint(point.getX(), point.getY(), point.getPlane())));
        if (next.isEmpty()) { publish("No verified destination for this setup."); return; }
        clear();
        long request = generation.incrementAndGet();
        pendingClearOwner = null;
        WorldPoint start = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
        Map<String, Object> config = new LinkedHashMap<>();
        // Display strings are the upstream protocol values, not Java enum names.
        config.put("useTeleportationItems", bankSeen ? "Inventory and Bank" : "Inventory");
        config.put("includeBankPath", bankSeen);
        config.put("avoidWilderness", !allowWilderness);
        for (String transport : new String[]{"useAgilityShortcuts", "useGrappleShortcuts", "useBoats",
            "useCanoes", "useCharterShips", "useShips", "useFairyRings", "useGnomeGliders",
            "useHotAirBalloons", "useMagicCarpets", "useMagicMushtrees", "useMinecarts", "useQuetzals",
            "useSpiritTrees", "useTeleportationLevers", "useTeleportationPortals", "useTeleportationSpells",
            "useTeleportationSpellsHome", "useTeleportationMinigames", "useWildernessObelisks"})
        {
            config.put(transport, true);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("start", start);
        payload.put("target", next.size() == 1 ? next.iterator().next() : next);
        payload.put("config", config);
        owner = shortest;
        leaseGeneration = request;
        routeId = destination.getId();
        overridesOwned = true;
        routePending = true;
        post(new PluginMessage(NAMESPACE, "path", payload));
        if (ownsRoute()) { publish("Route sent - see Shortest Path"); }
        // Upstream queues path creation with invokeLater. Do not infer completion from a tick count.
        clientThread.invokeLater(() -> {
            if (generation.get() == request) { routePending = false; }
        });
    }

    public void clear()
    {
        boolean owned = ownsRoute();
        if (!owned) { publish(""); return; }
        Plugin previousOwner = owner;
        boolean pending = routePending;
        long request = generation.incrementAndGet();
        forgetRoute();
        post(new PluginMessage(NAMESPACE, "clear"));
        if (pending) { cancelPending(request, previousOwner); }
        publish("");
    }

    private void cancelPending(long request, Plugin previousOwner)
    {
        if (generation.get() != request) { return; }
        pendingClearOwner = previousOwner;
        clientThread.invokeLater(() -> {
            if (generation.get() == request && availablePlugin == previousOwner)
            {
                pendingClearOwner = null;
                post(new PluginMessage(NAMESPACE, "clear"));
            }
        });
    }

    public void stop()
    {
        long stoppedLifecycle = ++lifecycleGeneration;
        clear();
        listener = null;
        suspendedOwner = null;
        bankSeen = false;
        availabilityGeneration.incrementAndGet();
        // Observe takeovers until the queued cancellation has run, including during AIO shutdown.
        clientThread.invokeLater(() -> {
            if (stoppedLifecycle == lifecycleGeneration && registered)
            {
                eventBus.unregister(this);
                keyManager.unregisterKeyListener(clearKeyListener);
                registered = false;
                availablePlugin = null;
            }
        });
    }

    @Subscribe(priority = 1)
    public void onPluginMessage(PluginMessage event)
    {
        if (!NAMESPACE.equals(event.getNamespace())
            || !("path".equals(event.getName()) || "clear".equals(event.getName()))) { return; }
        if (client.isClientThread() && ownMessages.contains(event)) { return; }
        boolean clear = "clear".equals(event.getName());
        Object config = event.getData() == null ? null : event.getData().get("config");
        boolean replacesConfig = config instanceof Map<?, ?> && !((Map<?, ?>) config).isEmpty();
        // A foreign config replaces ours synchronously. For config-less requests, our queued
        // release precedes upstream's queued path creation, including Quest Helper's EDT requests.
        observeTakeover(!clear && !replacesConfig, clear);
    }

    @Subscribe(priority = 1)
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        String option = event.getMenuOption();
        String plain = Text.removeTags(event.getMenuTarget() == null ? "" : event.getMenuTarget());
        if ("Clear".equals(option) && "Path".equals(plain)) { observeTakeover(true, true); }
        else if (("Set".equals(option) && (plain.startsWith("Target") || "Start".equals(plain)))
            || "Find closest".equals(option))
        {
            observeTakeover(true, false);
        }
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        if (PLUGIN_CLASS.equals(event.getPlugin().getClass().getName()))
        {
            long observation = generation.incrementAndGet();
            availabilityGeneration.incrementAndGet();
            availablePlugin = null;
            clientThread.invokeLater(() -> {
                if (!event.isLoaded())
                {
                    // Upstream retains its path and overrides while disabled. Remember only our
                    // last uninterrupted lease; any intervening handoff invalidates this cleanup.
                    suspendedOwner = owner == event.getPlugin() && overridesOwned
                        && leaseGeneration == observation - 1 ? owner : null;
                    suspendedGeneration = observation;
                }
                else if (suspendedOwner == event.getPlugin() && suspendedGeneration == observation - 1)
                {
                    suspendedGeneration = observation;
                }
                else { suspendedOwner = null; }
                pendingClearOwner = null;
                bankSeen = false;
                // A stopped plugin has already left the EventBus; never call its handler directly.
                forgetRoute();
                publish("");
                pluginChanged();
            });
        }
    }

    private void observeTakeover(boolean releaseOverrides, boolean cancelPending)
    {
        long observation = generation.incrementAndGet();
        Runnable relinquish = () -> {
            if (generation.get() != observation) { return; }
            suspendedOwner = null;
            Plugin previousOwner = owner;
            boolean release = releaseOverrides && overridesOwned && previousOwner == availablePlugin;
            Plugin pendingOwner = routePending ? previousOwner : pendingClearOwner;
            pendingClearOwner = null;
            forgetRoute();
            if (release)
            {
                // Nonempty config replaces global overrides; no start/target preserves the route.
                // The marker has no upstream meaning: all real options fall back to saved defaults.
                post(new PluginMessage(NAMESPACE, "path",
                    Collections.singletonMap("config", Collections.singletonMap("allInSlayerRelease", true))));
            }
            if (cancelPending && pendingOwner != null) { cancelPending(observation, pendingOwner); }
            publish("");
        };
        if (client.isClientThread()) { relinquish.run(); }
        else { clientThread.invokeLater(relinquish); }
    }

    private boolean ownsRoute()
    {
        return owner != null && owner == availablePlugin && leaseGeneration == generation.get();
    }

    private void forgetRoute()
    {
        owner = null;
        routeId = "";
        overridesOwned = false;
        routePending = false;
    }

    public void refresh()
    {
        // A cross-thread takeover has its own queued cleanup; keep its metadata until that runs.
        publish(owner != null && !ownsRoute() ? "" : state.getMessage());
    }

    private void post(PluginMessage event)
    {
        ownMessages.add(event);
        try { eventBus.post(event); }
        finally { ownMessages.remove(event); }
    }

    private void publish(String message)
    {
        boolean active = ownsRoute();
        State next = new State(availablePlugin != null, active, bankSeen, active ? routeId : "",
            availablePlugin == null ? UNAVAILABLE : message == null ? "" : message, active ? Phase.SENT : Phase.NONE);
        if (!Objects.equals(state, next))
        {
            state = next;
            if (listener != null) { listener.accept(next); }
        }
    }

    @Value
    public static class State
    {
        boolean available;
        boolean active;
        boolean bankSeen;
        String routeId;
        String message;
        Phase phase;
    }

    public enum Phase { NONE, SENT }
}

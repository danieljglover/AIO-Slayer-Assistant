package com.danieljglover.allinslayer.integration.advisor;

import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.RouteDestination;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.Text;

/** Client-thread travel handoff. The external plugin owns pathfinding and never receives game actions. */
@Slf4j
@Singleton
public final class ShortestPathBridge
{
    private static final String NAMESPACE = "shortestpath";
    private static final String OWNER_MARKER = "allInSlayerRoute";
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private EventBus eventBus;
    @Inject private PluginManager pluginManager;
    private Consumer<State> listener;
    private volatile State state = new State(false, false, false, "", "Install and enable Shortest Path.", Phase.NONE);
    private Plugin owner;
    private Set<WorldPoint> targets = Collections.emptySet();
    private String routeId = "";
    private boolean overridesOwned;
    private boolean dispatching;
    private boolean bankSeen;
    private int settlingTicks;
    private long generation;
    private long availabilityGeneration;
    private long lifecycleGeneration;
    private boolean registered;
    private Plugin availablePlugin;
    private Phase phase = Phase.NONE;
    private Object observedPathfinder;

    public void setListener(Consumer<State> listener)
    {
        lifecycleGeneration++;
        if (!registered) { eventBus.register(this); registered = true; }
        this.listener = listener;
        pluginChanged();
        refresh();
    }

    public State getState() { return state; }

    public void bankObserved()
    {
        if (plugin() != null) { bankSeen = true; }
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
        long request = ++availabilityGeneration;
        SwingUtilities.invokeLater(() -> {
            Plugin available = pluginManager.getPlugins().stream()
                .filter(candidate -> "shortestpath.ShortestPathPlugin".equals(candidate.getClass().getName()))
                .filter(pluginManager::isPluginActive).findFirst().orElse(null);
            clientThread.invokeLater(() -> {
                if (!registered || request != availabilityGeneration) { return; }
                if (availablePlugin != available)
                {
                    availablePlugin = available;
                    bankSeen = false;
                    if (owner != null && owner != available) { clear(); }
                }
                refresh();
            });
        });
    }

    public void route(RouteDestination destination, boolean allowWilderness)
    {
        Plugin shortest = plugin();
        if (shortest == null)
        {
            publish("Install and enable Shortest Path in RuneLite's Plugin Hub.");
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            publish("Log in to start a route.");
            return;
        }
        clear();
        Set<WorldPoint> next = new LinkedHashSet<>();
        destination.getPoints().forEach(point -> next.add(new WorldPoint(point.getX(), point.getY(), point.getPlane())));
        if (next.isEmpty()) { publish("No verified destination for this setup."); return; }
        generation++;
        WorldPoint start = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(OWNER_MARKER, true);
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
        targets = next;
        routeId = destination.getId();
        overridesOwned = true;
        // Upstream installs the new pathfinder on a later client-thread callback.
        settlingTicks = 2;
        phase = Phase.CALCULATING;
        post(new PluginMessage(NAMESPACE, "path", payload));
        publish("Calculating route...");
    }

    public void clear()
    {
        long clearGeneration = ++generation;
        Plugin previousOwner = owner;
        Set<WorldPoint> previousTargets = targets;
        boolean pending = settlingTicks > 0;
        if (owner != null && ownsOverrides() && ownsCurrentTarget())
        {
            deliver(new PluginMessage(NAMESPACE, "clear"), owner);
            overridesOwned = false;
        }
        releaseOverrides();
        owner = null;
        targets = Collections.emptySet();
        routeId = "";
        settlingTicks = 0;
        phase = Phase.NONE;
        observedPathfinder = null;
        publish("");
        if (pending && previousOwner != null)
        {
            // Upstream queues path creation. Clear after that callback too, unless another
            // route request or manual/foreign handoff has since taken ownership.
            clientThread.invokeLater(() -> {
                if (generation == clearGeneration && ownsTarget(previousOwner, previousTargets))
                {
                    deliver(new PluginMessage(NAMESPACE, "clear"), previousOwner);
                }
            });
        }
    }

    public void stop()
    {
        long stoppedLifecycle = ++lifecycleGeneration;
        clear();
        listener = null;
        bankSeen = false;
        availablePlugin = null;
        availabilityGeneration++;
        // Keep takeover observation alive until pending route cancellation has settled.
        // The owning AIO plugin is already unregistered when its shutdown callback runs.
        clientThread.invokeLater(() -> {
            if (stoppedLifecycle == lifecycleGeneration && registered)
            {
                eventBus.unregister(this);
                registered = false;
            }
        });
    }

    @Subscribe(priority = 1)
    public void onPluginMessage(PluginMessage event)
    {
        if (!NAMESPACE.equals(event.getNamespace())
            || !("path".equals(event.getName()) || "clear".equals(event.getName()))) { return; }
        if (client.isClientThread()) { message(event); }
        else { clientThread.invokeLater(() -> message(event)); }
    }

    @Subscribe(priority = 1)
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        menuAction(event.getMenuOption(), event.getMenuTarget());
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        if ("shortestpath.ShortestPathPlugin".equals(event.getPlugin().getClass().getName()))
        {
            clientThread.invokeLater(this::pluginChanged);
        }
    }

    /** Called before Shortest Path's subscriber, so Quest Helper's config-less requests use saved defaults. */
    public void message(PluginMessage event)
    {
        if (dispatching || !NAMESPACE.equals(event.getNamespace())) { return; }
        if ("clear".equals(event.getName())) { clear(); }
        else if ("path".equals(event.getName()))
        {
            relinquish();
        }
    }

    public void menuAction(String option, String target)
    {
        String plain = Text.removeTags(target == null ? "" : target);
        if ("Clear".equals(option) && "Path".equals(plain)) { clear(); return; }
        if (("Set".equals(option) && (plain.startsWith("Target") || "Start".equals(plain)))
            || "Find closest".equals(option))
        {
            relinquish();
        }
    }

    private void relinquish()
    {
        generation++;
        releaseOverrides();
        owner = null;
        targets = Collections.emptySet();
        routeId = "";
        settlingTicks = 0;
        phase = Phase.NONE;
        observedPathfinder = null;
        publish("");
    }

    public void refresh()
    {
        if (owner != null)
        {
            if (settlingTicks > 0) { settlingTicks--; }
            else if (!ownsOverrides() || !ownsCurrentTarget()) { relinquish(); }
            else { updateProgress(); }
        }
        publish(state.getMessage());
    }

    private void updateProgress()
    {
        try
        {
            Object pathfinder = owner.getClass().getMethod("getPathfinder").invoke(owner);
            if (pathfinder == observedPathfinder && phase != Phase.CALCULATING) { return; }
            observedPathfinder = pathfinder;
            Object result = pathfinder.getClass().getMethod("getResult").invoke(pathfinder);
            if (result == null) { phase = Phase.CALCULATING; publish("Calculating route..."); }
            else if (Boolean.TRUE.equals(result.getClass().getMethod("isReached").invoke(result)))
            {
                phase = Phase.READY;
                publish("Route ready");
            }
            else
            {
                phase = Phase.INCOMPLETE;
                publish("Route incomplete - see Shortest Path");
            }
        }
        catch (ReflectiveOperationException | RuntimeException ex)
        {
            phase = Phase.UNKNOWN;
            publish("Route sent - see Shortest Path");
        }
    }

    private void releaseOverrides()
    {
        if (!overridesOwned || !ownsOverrides()) { overridesOwned = false; return; }
        // A nonempty config replaces upstream's global override map. With no start/target,
        // it only restores saved defaults and leaves another plugin's destination untouched.
        PluginMessage release = new PluginMessage(NAMESPACE, "path",
            Collections.singletonMap("config", Collections.singletonMap("allInSlayerRelease", true)));
        deliver(release, owner);
        overridesOwned = false;
    }

    private boolean ownsOverrides()
    {
        if (owner == null) { return false; }
        try
        {
            return Boolean.TRUE.equals(owner.getClass().getMethod("override", String.class, boolean.class)
                .invoke(null, OWNER_MARKER, false));
        }
        catch (ReflectiveOperationException | RuntimeException ex) { return false; }
    }

    private void deliver(PluginMessage message, Plugin recipient)
    {
        if (recipient == null) { return; }
        // Cleanup uses the known owner's public handler, even across EventBus unregistration.
        // The asynchronous availability snapshot cannot prove that a bus delivery reached it.
        // Only clear/config-only messages come here; route requests use the shared EventBus.
        try { recipient.getClass().getMethod("onPluginMessage", PluginMessage.class).invoke(recipient, message); }
        catch (ReflectiveOperationException | RuntimeException ex) { log.debug("Unable to release Shortest Path route/settings", ex); }
    }

    private boolean ownsCurrentTarget()
    {
        if (owner == null) { return false; }
        return ownsTarget(owner, targets);
    }

    private boolean ownsTarget(Plugin candidate, Set<WorldPoint> submitted)
    {
        try
        {
            Object pathfinder = candidate.getClass().getMethod("getPathfinder").invoke(candidate);
            if (pathfinder == null) { return false; }
            Object rawTargets = pathfinder.getClass().getMethod("getTargets").invoke(pathfinder);
            if (!(rawTargets instanceof Set<?>)) { return false; }
            Set<?> actual = (Set<?>) rawTargets;
            if (actual.isEmpty()) { return false; }
            Class<?> pointUtil = Class.forName("shortestpath.WorldPointUtil", false, candidate.getClass().getClassLoader());
            java.lang.reflect.Method pack = pointUtil.getMethod("packWorldPoint", WorldPoint.class);
            Set<Object> expected = new LinkedHashSet<>();
            for (WorldPoint point : submitted)
            {
                expected.add(pack.invoke(null, point));
            }
            // The pathfinder may remove inaccessible candidates from the submitted set.
            return expected.containsAll(actual);
        }
        catch (ReflectiveOperationException | RuntimeException ex)
        {
            log.debug("Cannot verify Shortest Path route ownership", ex);
            return false;
        }
    }

    private Plugin plugin()
    {
        return availablePlugin;
    }

    private void post(PluginMessage event)
    {
        boolean previous = dispatching;
        dispatching = true;
        try { eventBus.post(event); }
        finally { dispatching = previous; }
    }

    private void publish(String message)
    {
        State next = new State(plugin() != null, owner != null, bankSeen, routeId, message == null ? "" : message, phase);
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

    public enum Phase { NONE, CALCULATING, READY, INCOMPLETE, UNKNOWN }
}

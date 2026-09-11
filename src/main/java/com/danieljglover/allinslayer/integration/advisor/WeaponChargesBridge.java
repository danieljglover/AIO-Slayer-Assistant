package com.danieljglover.allinslayer.integration.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/** Reads the active Weapon Charges plugin's account estimates without invoking its handlers. */
@Singleton
public final class WeaponChargesBridge
{
    public static final String CONFIG_GROUP = "weaponCharges";
    public static final String PLUGIN_CLASS = "com.weaponcharges.WeaponChargesPlugin";
    private static final String SOURCE = "Weapon Charges";
    private static final String BLOWPIPE = "blowpipe";
    private static final int ZULRAH_SCALES = 12934;
    private static final State ABSENT = new State(false, Collections.emptyMap(), Collections.emptyMap());

    @Inject private Client client;
    @Inject private ConfigManager configManager;
    @Inject private PluginManager pluginManager;
    private final AtomicLong generation = new AtomicLong();
    private volatile boolean running;
    private volatile State state = ABSENT;

    public void start()
    {
        running = true;
        pluginChanged();
    }

    public void stop()
    {
        running = false;
        generation.incrementAndGet();
        state = ABSENT;
    }

    /** The owning AIO plugin forwards plugin lifecycle changes here. */
    public void pluginChanged()
    {
        long request = generation.incrementAndGet();
        state = ABSENT;
        SwingUtilities.invokeLater(() -> {
            if (!running || request != generation.get()) { return; }
            Plugin candidate = pluginManager.getPlugins().stream()
                .filter(plugin -> PLUGIN_CLASS.equals(plugin.getClass().getName()))
                .findFirst().orElse(null);
            State next = ABSENT;
            if (candidate != null)
            {
                next = new State(true, Collections.emptyMap(), Collections.emptyMap());
                if (pluginManager.isPluginActive(candidate))
                {
                    next = new State(true, WeaponChargeMappings.ITEMS, WeaponChargeMappings.DARTS);
                }
            }
            if (running && request == generation.get()) { state = next; }
        });
    }

    public boolean hasPlugin() { return state.installed; }

    public boolean isAvailable() { return !state.items.isEmpty(); }

    public String familyKey(int itemId)
    {
        return state.items.get(itemId);
    }

    /**
     * Client-thread-only snapshot. Upstream persists one balance per family, with no timestamp
     * or per-item identity; callers must retain estimate provenance and handle duplicate items.
     */
    public Map<Integer, Reading> snapshot()
    {
        State captured = state;
        if (!running || captured.items.isEmpty() || !client.isClientThread()
            || client.getGameState() != GameState.LOGGED_IN) { return Collections.emptyMap(); }
        String profile = configManager.getRSProfileKey();
        if (profile == null) { return Collections.emptyMap(); }
        long accountHash = client.getAccountHash();
        Map<String, Reading> families = new LinkedHashMap<>();
        Map<Integer, Reading> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> item : captured.items.entrySet())
        {
            String key = item.getValue();
            if (!families.containsKey(key))
            {
                families.put(key, BLOWPIPE.equals(key) ? blowpipe(captured) : charges(key));
            }
            Reading reading = families.get(key);
            if (reading != null) { result.put(item.getKey(), reading); }
        }
        // Profile changes and plugin shutdown can arrive while an external config is read.
        if (captured != state || !running || client.getGameState() != GameState.LOGGED_IN
            || !Objects.equals(profile, configManager.getRSProfileKey())
            || accountHash != client.getAccountHash()) { return Collections.emptyMap(); }
        return Collections.unmodifiableMap(result);
    }

    private Reading charges(String key)
    {
        Integer count = count(key);
        return count == null ? null : new Reading(count, Collections.emptyMap(), key);
    }

    private Reading blowpipe(State captured)
    {
        Integer scales = count("blowpipeScales");
        Integer darts = count("blowpipeDarts");
        if (scales == null && darts == null) { return null; }
        Map<Integer, Integer> resources = new LinkedHashMap<>();
        if (scales != null) { resources.put(ZULRAH_SCALES, scales); }
        String dartType = configManager.getRSProfileConfiguration(CONFIG_GROUP, "blowpipeDartType");
        Integer dartId = captured.darts.get(dartType);
        if (darts != null && dartId != null) { resources.put(dartId, darts); }
        return new Reading(null, resources, BLOWPIPE);
    }

    private Integer count(String key)
    {
        String raw = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
        if (raw == null) { return null; }
        try
        {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value) || value < 0 || value > Integer.MAX_VALUE) { return null; }
            return (int) Math.floor(value);
        }
        catch (NumberFormatException ex) { return null; }
    }

    @Getter
    public static final class Reading
    {
        private final Integer charges;
        private final Map<Integer, Integer> resources;
        private final String familyKey;
        private final String source = SOURCE;

        private Reading(Integer charges, Map<Integer, Integer> resources, String familyKey)
        {
            this.charges = charges;
            this.resources = Collections.unmodifiableMap(new LinkedHashMap<>(resources));
            this.familyKey = familyKey;
        }
    }

    private static final class State
    {
        private final boolean installed;
        private final Map<Integer, String> items;
        private final Map<String, Integer> darts;

        private State(boolean installed, Map<Integer, String> items, Map<String, Integer> darts)
        {
            this.installed = installed;
            this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
            this.darts = Collections.unmodifiableMap(new LinkedHashMap<>(darts));
        }
    }
}

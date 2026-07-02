package com.danieljglover.allinslayer.bank;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;

/** Tracks live inventory/equipment and persists a last-seen bank snapshot (bank is only readable while open). */
@Slf4j
@Singleton
public class InventoryService
{
    public static final String GROUP = "allinslayer";
    public static final String BANK_SNAPSHOT_KEY = "bankSnapshot";
    public static final String BANK_TS_KEY = "bankSnapshotTs";

    private final Client client;
    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public InventoryService(Client client, ConfigManager configManager, Gson gson)
    {
        this.client = client;
        this.configManager = configManager;
        this.gson = gson;
    }

    /** Call when the bank ItemContainer changes; serialises a {itemId: qty} snapshot to the RS profile. */
    public void onBankChanged(ItemContainer bank)
    {
        if (bank == null)
        {
            return;
        }
        Map<Integer, Integer> snapshot = new HashMap<>();
        for (Item it : bank.getItems())
        {
            if (it != null && it.getId() > 0 && it.getQuantity() > 0)
            {
                snapshot.merge(it.getId(), it.getQuantity(), Integer::sum);
            }
        }
        configManager.setRSProfileConfiguration(GROUP, BANK_SNAPSHOT_KEY, gson.toJson(snapshot));
        configManager.setRSProfileConfiguration(GROUP, BANK_TS_KEY, String.valueOf(System.currentTimeMillis()));
    }

    /** Live inventory + worn + last-seen bank, merged. */
    public OwnedItems currentOwned()
    {
        Map<Integer, Integer> counts = new HashMap<>();
        addContainer(counts, client.getItemContainer(InventoryID.INV));
        addContainer(counts, client.getItemContainer(InventoryID.WORN));

        String snapshot = configManager.getRSProfileConfiguration(GROUP, BANK_SNAPSHOT_KEY);
        if (snapshot != null && !snapshot.isEmpty())
        {
            try
            {
                Map<Integer, Integer> bank = gson.fromJson(
                    snapshot, new TypeToken<Map<Integer, Integer>>() {}.getType());
                if (bank != null)
                {
                    bank.forEach((k, v) ->
                    {
                        if (k != null && v != null && k > 0 && v > 0)
                        {
                            counts.merge(k, v, Integer::sum);
                        }
                    });
                }
            }
            catch (JsonSyntaxException ex)
            {
                log.warn("Ignoring malformed saved bank snapshot", ex);
            }
        }
        return OwnedItems.fromCounts(counts);
    }

    /** Epoch millis the bank was last seen, or null. */
    public Long bankLastSeen()
    {
        String ts = configManager.getRSProfileConfiguration(GROUP, BANK_TS_KEY);
        if (ts == null || ts.isEmpty())
        {
            return null;
        }
        try
        {
            return Long.parseLong(ts);
        }
        catch (NumberFormatException ex)
        {
            log.warn("Ignoring malformed bank snapshot timestamp: {}", ts);
            return null;
        }
    }

    private void addContainer(Map<Integer, Integer> counts, ItemContainer c)
    {
        if (c == null)
        {
            return;
        }
        for (Item it : c.getItems())
        {
            if (it != null && it.getId() > 0 && it.getQuantity() > 0)
            {
                counts.merge(it.getId(), it.getQuantity(), Integer::sum);
            }
        }
    }
}

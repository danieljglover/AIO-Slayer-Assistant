package com.danieljglover.allinslayer.bank;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InventoryServiceTest
{
    @Mock private Client client;
    @Mock private ConfigManager configManager;
    @Mock private ItemContainer bankContainer;
    @Mock private ItemContainer invContainer;
    @Mock private ItemContainer wornContainer;

    private InventoryService service;

    @Before
    public void setUp()
    {
        service = new InventoryService(client, configManager, new Gson());
    }

    @Test
    public void persistedBankMergesWithLiveInventory()
    {
        Item helm = item(11865, 1);
        when(bankContainer.getItems()).thenReturn(new Item[]{helm});
        service.onBankChanged(bankContainer);

        String stored = "{\"11865\":1}";
        lenient().when(configManager.getRSProfileConfiguration(
            eq(InventoryService.GROUP), eq(InventoryService.BANK_SNAPSHOT_KEY)))
            .thenReturn(stored);

        Item runes = item(560, 100);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(invContainer);
        when(invContainer.getItems()).thenReturn(new Item[]{runes});
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(wornContainer);
        when(wornContainer.getItems()).thenReturn(new Item[0]);

        OwnedItems owned = service.currentOwned();

        assertTrue(owned.has(11865));
        assertTrue(owned.has(560));
    }

    @Test
    public void invalidPersistedBankSnapshotIsIgnored()
    {
        when(configManager.getRSProfileConfiguration(
            eq(InventoryService.GROUP), eq(InventoryService.BANK_SNAPSHOT_KEY)))
            .thenReturn("{not-json");

        Item runes = item(560, 100);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(invContainer);
        when(invContainer.getItems()).thenReturn(new Item[]{runes});
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(wornContainer);
        when(wornContainer.getItems()).thenReturn(new Item[0]);

        OwnedItems owned = service.currentOwned();

        assertTrue(owned.has(560));
        assertFalse(owned.has(11865));
    }

    @Test
    public void invalidBankTimestampIsIgnored()
    {
        when(configManager.getRSProfileConfiguration(
            eq(InventoryService.GROUP), eq(InventoryService.BANK_TS_KEY)))
            .thenReturn("not-a-timestamp");

        assertNull(service.bankLastSeen());
    }

    private static Item item(int id, int qty)
    {
        Item i = org.mockito.Mockito.mock(Item.class);
        when(i.getId()).thenReturn(id);
        when(i.getQuantity()).thenReturn(qty);
        return i;
    }
}

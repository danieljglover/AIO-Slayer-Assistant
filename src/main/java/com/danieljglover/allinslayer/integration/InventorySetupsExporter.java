package com.danieljglover.allinslayer.integration;

import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Builds an Inventory Setups import string from a recommendation and copies it to the clipboard. */
@Slf4j
@Singleton
public class InventorySetupsExporter
{
    private static final Map<EquipmentSlot, Integer> SLOT_INDEX = new EnumMap<>(EquipmentSlot.class);
    static
    {
        SLOT_INDEX.put(EquipmentSlot.HEAD, 0);
        SLOT_INDEX.put(EquipmentSlot.CAPE, 1);
        SLOT_INDEX.put(EquipmentSlot.AMULET, 2);
        SLOT_INDEX.put(EquipmentSlot.WEAPON, 3);
        SLOT_INDEX.put(EquipmentSlot.BODY, 4);
        SLOT_INDEX.put(EquipmentSlot.SHIELD, 5);
        SLOT_INDEX.put(EquipmentSlot.LEGS, 7);
        SLOT_INDEX.put(EquipmentSlot.HANDS, 9);
        SLOT_INDEX.put(EquipmentSlot.FEET, 10);
        SLOT_INDEX.put(EquipmentSlot.RING, 12);
        SLOT_INDEX.put(EquipmentSlot.AMMO, 13);
    }
    private static final int EQ_SIZE = 14;

    private final Gson gson = new Gson();

    public String buildImportString(Recommendation rec, String setupName)
    {
        JsonObject setup = new JsonObject();

        JsonArray eq = new JsonArray();
        JsonObject[] slots = new JsonObject[EQ_SIZE];
        if (rec.getWorn() != null)
        {
            for (Map.Entry<EquipmentSlot, Integer> e : rec.getWorn().entrySet())
            {
                Integer idx = SLOT_INDEX.get(e.getKey());
                if (idx != null)
                {
                    JsonObject item = new JsonObject();
                    item.addProperty("id", e.getValue());
                    slots[idx] = item;
                }
            }
        }
        for (int i = 0; i < EQ_SIZE; i++)
        {
            eq.add(slots[i] == null ? JsonNull.INSTANCE : slots[i]);
        }
        setup.add("eq", eq);

        JsonArray inv = new JsonArray();
        List<Integer> invItems = rec.getInventory();
        if (invItems != null)
        {
            for (Integer id : invItems)
            {
                JsonObject item = new JsonObject();
                item.addProperty("id", id);
                inv.add(item);
            }
        }
        setup.add("inv", inv);

        setup.add("rp", new JsonArray());
        setup.addProperty("name", setupName);

        JsonObject root = new JsonObject();
        root.add("setup", setup);
        root.add("layout", new JsonArray());
        return gson.toJson(root);
    }

    public void copyToClipboard(String s)
    {
        try
        {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(s), null);
        }
        catch (HeadlessException | IllegalStateException ex)
        {
            log.warn("Could not copy setup to clipboard", ex);
        }
    }
}

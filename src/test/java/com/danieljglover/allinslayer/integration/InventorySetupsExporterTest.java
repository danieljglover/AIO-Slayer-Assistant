package com.danieljglover.allinslayer.integration;

import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InventorySetupsExporterTest
{
    @Test
    public void buildsImportStringWithWeaponInSlot3AndInventory()
    {
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        worn.put(EquipmentSlot.HEAD, 11865);

        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(Arrays.asList(12695, 385));

        String json = new InventorySetupsExporter().buildImportString(rec, "Abyssal demons");

        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonObject setup = root.getAsJsonObject("setup");
        assertEquals("Abyssal demons", setup.get("name").getAsString());

        JsonArray eq = setup.getAsJsonArray("eq");
        assertEquals(14, eq.size());
        assertEquals(4151, eq.get(3).getAsJsonObject().get("id").getAsInt());
        assertEquals(11865, eq.get(0).getAsJsonObject().get("id").getAsInt());
        assertTrue(eq.get(1).isJsonNull());

        JsonArray inv = setup.getAsJsonArray("inv");
        assertEquals(2, inv.size());
        assertEquals(12695, inv.get(0).getAsJsonObject().get("id").getAsInt());
    }
}

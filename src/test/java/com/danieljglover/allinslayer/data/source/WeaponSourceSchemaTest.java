package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * WB-8 (PD-C disposed DELETE, FR-C2 section 3): {@code SourceWeapon} is a build-time
 * name -> item-id lookup and nothing more. The audit found 5 schema fields
 * ({@code slot}/{@code styles}/{@code attackSpeedTicks}/{@code effects}/{@code aliases}) parsed
 * but never populated by any of the 79 weapon files nor read by the compiler - dead schema that
 * invites fabricated data. This pins the schema to EXACTLY the 3 live fields; wiring a data-driven
 * effects layer is a deliberate design decision (rejected for now), not a field re-add.
 */
public class WeaponSourceSchemaTest
{
    private static final Path WEAPONS = Paths.get("src/main/data/slayer/weapons");
    private static final Set<String> LIVE_FIELDS = new HashSet<>(Arrays.asList(
        "weaponId", "name", "itemIds"));

    @Test
    public void sourceWeaponCarriesExactlyTheThreeLiveFields()
    {
        Set<String> declared = Arrays.stream(SourceWeapon.class.getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .map(Field::getName)
            .collect(Collectors.toCollection(TreeSet::new));
        assertEquals("SourceWeapon = the 3 live fields, nothing dead (PD-C)",
            new TreeSet<>(LIVE_FIELDS), declared);
    }

    @Test
    public void sourceWeaponDeserializesTheLiveShape()
    {
        SourceWeapon w = new Gson().fromJson(
            "{\"weaponId\":\"osmumtens-fang\",\"name\":\"Osmumten's fang\",\"itemIds\":[26219]}",
            SourceWeapon.class);
        assertEquals("osmumtens-fang", w.getWeaponId());
        assertEquals("Osmumten's fang", w.getName());
        assertEquals(Integer.valueOf(26219), w.getItemIds().get(0));
    }

    @Test
    public void everyWeaponFileCarriesOnlyLiveKeys() throws IOException
    {
        // The data side of the same pin: a weapon file that starts authoring dead keys (e.g. a
        // hand-written effects block the compiler would silently drop) fails here, loudly.
        Gson gson = new Gson();
        try (Stream<Path> paths = Files.list(WEAPONS))
        {
            for (Path file : (Iterable<Path>) paths
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()::iterator)
            {
                JsonObject obj;
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
                {
                    obj = gson.fromJson(reader, JsonObject.class);
                }
                for (String key : obj.keySet())
                {
                    assertTrue(file.getFileName() + " carries a non-live key: " + key,
                        LIVE_FIELDS.contains(key));
                }
                assertTrue(file.getFileName() + " has weaponId", obj.has("weaponId"));
                assertTrue(file.getFileName() + " has itemIds", obj.has("itemIds"));
            }
        }
    }
}

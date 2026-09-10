package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Evidence;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.ItemDefinition;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData.Pouch;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData.Spell;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData.Teleport;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Combines authoring-time travel and casting rules into one validated runtime bundle. */
final class TripPreparationCompiler
{
    private static final Gson GSON = new Gson();
    private static final Set<String> BOOKS = new HashSet<>(Arrays.asList("STANDARD", "ANCIENT", "LUNAR", "ARCEUUS"));

    private TripPreparationCompiler() { }

    static void load(Path root, SlayerCatalogue catalogue) throws IOException
    {
        JsonObject travel = read(root.resolve("advisor/trip-preparation.json"));
        JsonObject spells = read(root.resolve("advisor/spells.json"));
        JsonObject resources = read(root.resolve("advisor/casting-resources.json"));
        TripPreparationData result = GSON.fromJson(travel, TripPreparationData.class);
        require(result != null, "Missing trip preparation rules");
        integer(travel.get("combatCasts"), 1, 100000, "combatCasts");
        evidence(travel.get("evidence"), "trip preparation");
        evidence(resources.get("evidence"), "casting resources");
        addItems(travel, catalogue, false);
        addItems(resources, catalogue, true);

        JsonObject runeIds = object(resources, "runeIds");
        Set<Integer> uniqueRuneIds = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : runeIds.entrySet())
        {
            nonempty(entry.getKey(), "rune name");
            int id = itemId(entry.getValue(), entry.getKey());
            require(uniqueRuneIds.add(id), "Duplicate rune item ID: " + id);
            knownItem(catalogue, id, entry.getKey());
            result.getRuneIds().put(entry.getKey(), id);
        }
        require(!result.getRuneIds().isEmpty(), "Missing rune definitions");
        loadRuneProviders(resources, "infiniteRunes", result.getInfiniteRunes(), result);
        loadRuneProviders(resources, "combinationRunes", result.getCombinationRunes(), result);
        for (Integer id : result.getCombinationRunes().keySet())
        {
            require(uniqueRuneIds.contains(id), "Combination rune missing from runeIds: " + id);
        }

        for (Map.Entry<String, JsonElement> entry : object(resources, "poweredWeapons").entrySet())
        {
            int id = numericKey(entry.getKey(), "powered weapon");
            require(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString(), "Powered weapon needs a charge-check requirement");
            String requirement = entry.getValue().getAsString();
            nonempty(requirement, "powered weapon " + id);
            result.getPoweredWeapons().put(id, requirement);
        }

        JsonObject spellRows = object(spells, "spells");
        for (Map.Entry<String, JsonElement> entry : spellRows.entrySet())
        {
            String name = entry.getKey();
            require(entry.getValue().isJsonObject(), name + ": spell must be an object");
            JsonObject row = entry.getValue().getAsJsonObject();
            Spell spell = GSON.fromJson(row, Spell.class);
            require(name.equals(spell.getName()), name + ": spell name differs from map key");
            nonempty(spell.getFamily(), name + " family");
            require(BOOKS.contains(spell.getSpellbook()), name + ": invalid spellbook");
            integer(row.get("magicLevel"), 1, 99, name + " Magic level");
            JsonObject costs = object(row, "runes");
            require(costs.size() > 0, name + ": missing rune quantities");
            for (Map.Entry<String, JsonElement> cost : costs.entrySet())
            {
                require(result.getRuneIds().containsKey(cost.getKey()), name + ": unknown rune " + cost.getKey());
                integer(cost.getValue(), 1, 1000, name + " " + cost.getKey());
            }
            itemIds(row.get("weaponIds"), name + " weapons", true);
            integer(row.get("sackItemId"), 0, Integer.MAX_VALUE, name + " sack");
            if (spell.getSackItemId() > 0) { knownItem(catalogue, spell.getSackItemId(), name + " sack"); }
            strings(row.get("requirements"), name + " requirements", true);
            evidence(row.get("evidence"), name);
            result.getSpells().put(name, spell);
        }
        require(!result.getSpells().isEmpty(), "Missing preparation spells");

        JsonArray pouches = array(resources.get("pouches"), "pouches");
        Set<Integer> pouchIds = new HashSet<>();
        for (JsonElement raw : pouches)
        {
            require(raw.isJsonObject(), "Pouch must be an object");
            JsonObject row = raw.getAsJsonObject();
            int id = itemId(row.get("itemId"), "pouch");
            require(pouchIds.add(id), "Duplicate pouch: " + id);
            knownItem(catalogue, id, "pouch");
            integer(row.get("capacity"), 1, 4, "pouch capacity");
            integer(row.get("maxPerRune"), 1, 16000, "pouch rune capacity");
            evidence(row.get("evidence"), "pouch " + id);
            result.getPouches().add(GSON.fromJson(row, Pouch.class));
        }

        JsonObject teleportRows = object(travel, "teleports");
        for (Map.Entry<String, JsonElement> entry : teleportRows.entrySet())
        {
            String id = entry.getKey();
            require(id.matches("[a-z0-9]+(?:-[a-z0-9]+)*"), "Invalid teleport ID: " + id);
            require(entry.getValue().isJsonObject(), id + ": teleport must be an object");
            JsonObject row = entry.getValue().getAsJsonObject();
            Teleport teleport = result.getTeleports().get(id);
            require(id.equals(teleport.getId()), id + ": teleport ID differs from map key");
            nonempty(teleport.getName(), id + " name");
            nonempty(teleport.getDestination(), id + " destination");
            Set<Integer> ids = itemIds(row.get("itemIds"), id, true);
            for (int helper : itemIds(row.get("requiredItemIds"), id + " required helper alternatives", true))
            {
                knownItem(catalogue, helper, id + " required helper");
            }
            boolean spell = teleport.getSpellName() != null && !teleport.getSpellName().trim().isEmpty();
            require(spell != !ids.isEmpty(), id + ": needs exactly one item or spell mechanism");
            int limit = integer(row.get("wildernessLimit"), 0, 30, id + " Wilderness limit");
            require(limit == 0 || limit == 20 || limit == 30, id + ": unsupported Wilderness limit");
            require(!teleport.isEmergency() || limit > 0, id + ": emergency teleport has no Wilderness coverage");
            require(!teleport.isRemoteContact() || spell && !teleport.isEmergency() && limit == 0, id + ": remote contact cannot be an escape");
            integer(row.get("escapePriority"), 0, 1000, id + " escape priority");
            require(!teleport.isRemoteContact() || !teleport.isBankNearby(), id + ": remote contact does not reach a bank");
            JsonObject charges = object(row, "charges");
            require(charges.size() == ids.size(), id + ": each usable item needs a charge count");
            for (Map.Entry<String, JsonElement> charge : charges.entrySet())
            {
                int item = numericKey(charge.getKey(), id + " charge item");
                require(ids.contains(item), id + ": charge belongs to an unlisted item");
                // Zero means unlimited uses. Uncharged item forms must never be included.
                int count = integer(charge.getValue(), 0, 16000, id + " charge count");
                require(!teleport.isConsumable() || count > 0, id + ": consumable cannot have unlimited charges");
                knownItem(catalogue, item, id);
            }
            if (spell)
            {
                Spell definition = result.getSpells().get(teleport.getSpellName());
                require(definition != null && !definition.isOffensive(), id + ": missing non-offensive spell reference");
            }
            strings(row.get("requirements"), id + " requirements", true);
            evidence(row.get("evidence"), id);
        }
        require(!teleportRows.entrySet().isEmpty(), "Missing travel options");
        JsonObject masters = object(travel, "masterRoutes");
        for (Map.Entry<String, JsonElement> entry : masters.entrySet())
        {
            require(catalogue.getMasters().containsKey(entry.getKey()), "Unknown return master: " + entry.getKey());
            for (String route : strings(entry.getValue(), entry.getKey() + " return routes", false))
            {
                require(result.getTeleports().containsKey(route), entry.getKey() + ": unknown return route " + route);
            }
        }
        require(masters.keySet().equals(catalogue.getMasters().keySet()), "Every master needs explicit return routes");
        for (String group : Arrays.asList("bankRoutes", "houseRoutes"))
        {
            for (String route : strings(travel.get(group), group, false))
            {
                Teleport teleport = result.getTeleports().get(route);
                require(teleport != null && !teleport.isRemoteContact(), group + ": invalid return route " + route);
                require(!"bankRoutes".equals(group) || teleport.isBankNearby(), group + ": no reviewed nearby bank for " + route);
            }
        }
        for (int id : itemIds(travel.get("lootingBagIds"), "looting bags", false)) { knownItem(catalogue, id, "looting bag"); }
        for (Map.Entry<String, JsonElement> entry : object(travel, "blightedReplacements").entrySet())
        {
            int original = numericKey(entry.getKey(), "ordinary supply");
            int replacement = itemId(entry.getValue(), "blighted supply");
            require(original != replacement, "Blighted replacement cannot refer to itself");
            knownItem(catalogue, original, "ordinary supply");
            knownItem(catalogue, replacement, "blighted supply");
        }
        strings(travel.get("wildernessEscapeNotes"), "Wilderness escape notes", false);
        strings(travel.get("lootingBagNotes"), "looting bag notes", false);
        for (JsonElement raw : array(resources.get("evidence"), "casting evidence"))
        {
            result.getEvidence().add(GSON.fromJson(raw, Evidence.class));
        }
        catalogue.setPreparation(result);
    }

    private static void addItems(JsonObject source, SlayerCatalogue catalogue, boolean inventoryDefault)
    {
        Set<Integer> seen = new HashSet<>();
        for (JsonElement raw : array(source.get("items"), "preparation items"))
        {
            require(raw.isJsonObject(), "Preparation item must be an object");
            JsonObject row = raw.getAsJsonObject();
            int id = itemId(row.get("id"), "preparation item");
            require(seen.add(id), "Duplicate preparation item: " + id);
            require(row.has("name") && row.get("name").isJsonPrimitive(), "Preparation item missing name: " + id);
            String name = row.get("name").getAsString();
            nonempty(name, "preparation item name");
            if (row.has("evidence")) { evidence(row.get("evidence"), name); }
            if (!catalogue.getItems().containsKey(id))
            {
                ItemDefinition item = new ItemDefinition();
                item.setId(id);
                item.setName(name);
                item.setSlot("");
                item.setRequirementsKnown(row.has("inventoryResource") ? row.get("inventoryResource").getAsBoolean() : inventoryDefault);
                if (row.has("slot"))
                {
                    String slot = row.get("slot").getAsString();
                    require(new HashSet<>(Arrays.asList("HEAD", "CAPE", "NECK", "WEAPON", "BODY", "SHIELD", "LEGS", "HANDS", "FEET", "RING", "AMMO")).contains(slot), name + ": invalid equipment slot");
                    require(row.has("requirementsKnown") && row.get("requirementsKnown").getAsBoolean(), name + ": new equipment needs reviewed requirements");
                    evidence(row.get("evidence"), name + " equipment");
                    item.setSlot(slot);
                    item.setRequirementsKnown(true);
                    for (Map.Entry<String, JsonElement> level : object(row, "levels").entrySet())
                    {
                        item.getLevels().put(level.getKey(), integer(level.getValue(), 1, 99, name + " " + level.getKey()));
                    }
                    item.setRequirements(strings(row.get("requirements"), name + " equipment requirements", true));
                }
                // Travel use and equipment eligibility differ; never overwrite existing gear facts.
                catalogue.getItems().put(id, item);
            }
        }
    }

    private static void loadRuneProviders(JsonObject source, String key, Map<Integer, java.util.List<String>> target, TripPreparationData data)
    {
        for (Map.Entry<String, JsonElement> entry : object(source, key).entrySet())
        {
            int id = numericKey(entry.getKey(), key);
            java.util.List<String> runes = strings(entry.getValue(), key + " " + id, false);
            for (String rune : runes) { require(data.getRuneIds().containsKey(rune), key + ": unknown rune " + rune); }
            target.put(id, runes);
        }
    }

    private static void knownItem(SlayerCatalogue catalogue, int id, String owner)
    {
        require(catalogue.getItems().containsKey(id), owner + ": missing item metadata " + id);
    }

    private static Set<Integer> itemIds(JsonElement raw, String owner, boolean empty)
    {
        JsonArray array = array(raw, owner);
        require(empty || array.size() > 0, owner + ": needs item IDs");
        Set<Integer> result = new HashSet<>();
        for (JsonElement value : array) { require(result.add(itemId(value, owner)), owner + ": duplicate item ID"); }
        return result;
    }

    private static java.util.List<String> strings(JsonElement raw, String owner, boolean empty)
    {
        JsonArray array = array(raw, owner);
        require(empty || array.size() > 0, owner + ": needs values");
        java.util.List<String> result = new java.util.ArrayList<>();
        for (JsonElement value : array)
        {
            require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(), owner + ": expected string");
            String text = value.getAsString();
            nonempty(text, owner);
            require(!result.contains(text), owner + ": duplicate value " + text);
            result.add(text);
        }
        return result;
    }

    private static void evidence(JsonElement raw, String owner)
    {
        JsonArray array = array(raw, owner + " evidence");
        require(array.size() > 0, owner + ": missing evidence");
        for (JsonElement value : array)
        {
            require(value.isJsonObject(), owner + ": evidence must be an object");
            JsonObject row = value.getAsJsonObject();
            Evidence evidence = GSON.fromJson(row, Evidence.class);
            require(!evidence.isMissing() && evidence.getUrl() != null && evidence.getUrl().startsWith("https://oldschool.runescape.wiki/w/"), owner + ": needs Wiki source evidence");
            nonempty(evidence.getTitle(), owner + " evidence title");
            integer(row.get("pageId"), 1, Integer.MAX_VALUE, owner + " page ID");
            integer(row.get("revisionId"), 1, Integer.MAX_VALUE, owner + " revision ID");
            nonempty(evidence.getTimestamp(), owner + " revision timestamp");
            Instant.parse(evidence.getTimestamp());
        }
    }

    private static int numericKey(String key, String owner)
    {
        require(key.matches("[1-9][0-9]*"), owner + ": invalid item ID key");
        return integer(new JsonPrimitive(new BigDecimal(key)), 1, Integer.MAX_VALUE, owner);
    }

    private static int itemId(JsonElement raw, String owner) { return integer(raw, 1, Integer.MAX_VALUE, owner + " item ID"); }

    private static int integer(JsonElement raw, int min, int max, String owner)
    {
        require(raw != null && raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isNumber()
            && raw.getAsString().matches("[0-9]+"), owner + ": expected explicit integer");
        BigDecimal value = raw.getAsBigDecimal();
        require(value.compareTo(BigDecimal.valueOf(min)) >= 0 && value.compareTo(BigDecimal.valueOf(max)) <= 0, owner + ": value outside allowed range");
        return value.intValueExact();
    }

    private static JsonObject object(JsonObject source, String key)
    {
        require(source.has(key) && source.get(key).isJsonObject(), key + ": expected object");
        return source.getAsJsonObject(key);
    }

    private static JsonArray array(JsonElement source, String owner)
    {
        require(source != null && source.isJsonArray(), owner + ": expected array");
        return source.getAsJsonArray();
    }

    private static void nonempty(String value, String owner) { require(value != null && !value.trim().isEmpty(), owner + ": missing text"); }
    private static void require(boolean condition, String message) { if (!condition) { throw new IllegalArgumentException(message); } }

    private static JsonObject read(Path path) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8); JsonReader json = new JsonReader(reader))
        {
            JsonElement value = readValue(json);
            require(value.isJsonObject() && json.peek() == JsonToken.END_DOCUMENT, path + ": expected one root object");
            return value.getAsJsonObject();
        }
    }

    // Gson's ordinary object deserializer silently overwrites duplicate rule keys.
    private static JsonElement readValue(JsonReader reader) throws IOException
    {
        switch (reader.peek())
        {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext())
                {
                    String name = reader.nextName();
                    require(!object.has(name), "Duplicate preparation source key: " + name);
                    object.add(name, readValue(reader));
                }
                reader.endObject();
                return object;
            case BEGIN_ARRAY:
                JsonArray array = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) { array.add(readValue(reader)); }
                reader.endArray();
                return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER: return new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw new IllegalArgumentException("Invalid preparation JSON token: " + reader.peek());
        }
    }
}

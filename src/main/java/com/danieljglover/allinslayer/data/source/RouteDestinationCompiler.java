package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Evidence;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Monster;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.RouteDestination;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** Validates authored destinations; no runtime geocoding or inferred map centres. */
final class RouteDestinationCompiler
{
    private static final Gson GSON = new Gson();

    private RouteDestinationCompiler() { }

    static void load(Path root, SlayerCatalogue catalogue) throws IOException
    {
        JsonObject source;
        try (Reader reader = Files.newBufferedReader(root.resolve("advisor/routing-destinations.json"), StandardCharsets.UTF_8))
        {
            source = GSON.fromJson(reader, JsonObject.class);
        }
        require(source != null && source.has("destinations") && source.get("destinations").isJsonArray(),
            "routing-destinations.json needs a destinations array");
        Set<String> bindings = new HashSet<>();
        for (JsonElement raw : source.getAsJsonArray("destinations"))
        {
            require(raw.isJsonObject(), "Route destination must be an object");
            JsonObject row = raw.getAsJsonObject();
            RouteDestination destination = GSON.fromJson(row, RouteDestination.class);
            String id = destination.getId();
            require(id != null && id.matches("[a-z0-9]+(?:-[a-z0-9]+)*"), "Invalid route destination ID: " + id);
            require(destination.getLabel() != null && !destination.getLabel().trim().isEmpty(), id + ": missing label");
            require("MONSTER".equals(destination.getArrival()) || "ENTRANCE".equals(destination.getArrival()), id + ": invalid arrival type");
            require(catalogue.getLocations().containsKey(destination.getLocationId()), id + ": unknown location");
            require(destination.getMonsterIds() != null && !destination.getMonsterIds().isEmpty(), id + ": missing monster bindings");
            for (String monsterId : destination.getMonsterIds())
            {
                Monster monster = catalogue.getMonsters().get(monsterId);
                require(monster != null, id + ": unknown monster " + monsterId);
                require(monster.getLocationIds().contains(destination.getLocationId()), id + ": monster is not linked to this location: " + monsterId);
                require(bindings.add(monsterId + "/" + destination.getLocationId()), id + ": duplicate monster/location destination");
            }
            require(row.has("points") && row.get("points").isJsonArray()
                && row.getAsJsonArray("points").size() > 0 && row.getAsJsonArray("points").size() <= 512, id + ": needs 1..512 points");
            Set<String> points = new HashSet<>();
            for (JsonElement rawPoint : row.getAsJsonArray("points"))
            {
                require(rawPoint.isJsonObject(), id + ": point must be an object");
                JsonObject point = rawPoint.getAsJsonObject();
                int x = coordinate(point, "x", 16383, id);
                int y = coordinate(point, "y", 16383, id);
                int plane = coordinate(point, "plane", 3, id);
                require(points.add(x + "/" + y + "/" + plane), id + ": duplicate coordinate");
            }
            require(destination.getEvidence() != null && !destination.getEvidence().isEmpty(), id + ": missing source evidence");
            boolean wikiEvidence = false;
            for (Evidence evidence : destination.getEvidence())
            {
                require(evidence != null && !evidence.isMissing() && evidence.getUrl() != null
                    && evidence.getTitle() != null && !evidence.getTitle().isEmpty(), id + ": invalid evidence");
                if (evidence.getUrl().startsWith("https://oldschool.runescape.wiki/w/"))
                {
                    require(evidence.getPageId() > 0 && evidence.getRevisionId() > 0, id + ": unpinned Wiki evidence");
                    wikiEvidence = true;
                }
                else
                {
                    require(evidence.getUrl().matches("https://github\\.com/[^/]+/[^/]+/blob/[0-9a-f]{40}/.+"),
                        id + ": external coordinate evidence must use an immutable source blob");
                }
                require(evidence.getTimestamp() != null, id + ": missing evidence timestamp");
                Instant.parse(evidence.getTimestamp());
            }
            require(wikiEvidence, id + ": missing Wiki encounter/location evidence");
            require(catalogue.getRouteDestinations().putIfAbsent(id, destination) == null, id + ": duplicate route ID");
        }
    }

    private static int coordinate(JsonObject point, String name, int maximum, String owner)
    {
        require(point.has(name) && point.get(name).isJsonPrimitive()
            && point.getAsJsonPrimitive(name).isNumber() && point.get(name).getAsString().matches("[0-9]+"),
            owner + ": " + name + " must be an explicit integer");
        long value = point.get(name).getAsLong();
        require(value <= maximum, owner + ": " + name + " outside map range");
        return (int) value;
    }

    private static void require(boolean condition, String message)
    {
        if (!condition) { throw new IllegalArgumentException(message); }
    }
}

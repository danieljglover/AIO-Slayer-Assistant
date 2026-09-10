package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.advisor.DeathRule;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.WildernessArea;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

/** Validates reviewed death rules separately from combat equipment eligibility. */
final class DeathRuleCompiler
{
    private static final Gson GSON = new Gson();

    private DeathRuleCompiler()
    {
    }

    static void load(Path root, SlayerCatalogue catalogue) throws IOException
    {
        JsonObject rules = read(root.resolve("advisor/death-rules.json"));
        if (!rules.has("rules") || !rules.get("rules").isJsonArray())
            throw new IllegalArgumentException("death-rules.json must contain a rules array");
        for (JsonElement entry : rules.getAsJsonArray("rules"))
        {
            JsonObject source = entry.getAsJsonObject();
            DeathRule rule = GSON.fromJson(source, DeathRule.class);
            String owner = source.has("name") ? source.get("name").getAsString() : "Death rule";
            if (!source.has("kind") || rule.getKind() == null)
                throw new IllegalArgumentException(owner + ": missing or unknown death rule kind");
            if (rule.getProtectionValue() < -1 || rule.getRepairCost() < 0 || rule.getAlwaysLostValue() < 0)
                throw new IllegalArgumentException(owner + ": invalid death valuation or repair cost");
            if (rule.isLocked() && rule.getKind() != DeathRule.Kind.LOCKABLE && rule.getKind() != DeathRule.Kind.POUCH)
                throw new IllegalArgumentException(owner + ": lock requires LOCKABLE or POUCH behavior");
            if ((rule.getKind() == DeathRule.Kind.ALWAYS_LOST
                || rule.getKind() == DeathRule.Kind.POUCH) && rule.isProtectable())
                throw new IllegalArgumentException(owner + ": automatic loss/pouch cannot consume a protection slot");
            quantities(owner, rule.getReplacementItems());
            quantities(owner, rule.getRepairCostItems());
            quantities(owner, rule.getAlwaysLostItems());
            if (rule.isChargesKeptWhenProtected() && (!rule.isProtectable() || rule.isContentsLost()
                || rule.getAlwaysLostValue() > 0 || !rule.getAlwaysLostItems().isEmpty()
                || rule.getKind() == DeathRule.Kind.UNKNOWN))
                throw new IllegalArgumentException(owner + ": protected charge retention conflicts with unknown or unconditional loss behavior");
            if (!Arrays.asList("", "RUNE_POUCH", "LOOTING_BAG", "QUIVER", "ETHER_WEAPON").contains(rule.getContentsType()))
                throw new IllegalArgumentException(owner + ": unsupported stored contents type");
            if ("ETHER_WEAPON".equals(rule.getContentsType()) && (!rule.isContentsLost()
                || rule.getKind() != DeathRule.Kind.CONVERT
                || rule.getAlwaysLostItems().getOrDefault(21820, 0) != 1000))
                throw new IllegalArgumentException(owner + ": ether weapon must convert and always lose its 1,000 activation ether");
            if (rule.getKind() == DeathRule.Kind.CONVERT && rule.getReplacementItems().isEmpty())
                throw new IllegalArgumentException(owner + ": conversion must identify loss components");
            evidence(owner, rule.getEvidence());
            if (!source.has("itemIds") || !source.get("itemIds").isJsonArray()
                || source.getAsJsonArray("itemIds").size() == 0)
                throw new IllegalArgumentException(owner + ": death rule needs exact item IDs");
            for (JsonElement rawId : source.getAsJsonArray("itemIds"))
            {
                int id = positiveId(owner, rawId.getAsString());
                if (catalogue.getDeathRules().putIfAbsent(id, rule) != null)
                    throw new IllegalArgumentException(owner + ": duplicate death rule item ID " + id);
            }
        }

        JsonObject areas = read(root.resolve("advisor/wilderness-risk.json"));
        if (!areas.has("areas") || !areas.get("areas").isJsonObject())
            throw new IllegalArgumentException("wilderness-risk.json must contain an areas object");
        for (Map.Entry<String, JsonElement> entry : areas.getAsJsonObject("areas").entrySet())
        {
            String id = entry.getKey();
            if (!catalogue.getLocations().containsKey(id))
                throw new IllegalArgumentException("Wilderness risk references missing location " + id);
            WildernessArea area = GSON.fromJson(entry.getValue(), WildernessArea.class);
            if (area.getMinLevel() < -1 || area.getMaxLevel() < -1
                || area.getMinLevel() > 56 || area.getMaxLevel() > 56
                || (area.getMinLevel() == -1) != (area.getMaxLevel() == -1)
                || area.getMinLevel() > area.getMaxLevel() || area.getEntryFee() < 0)
                throw new IllegalArgumentException(id + ": invalid Wilderness range or entry fee");
            if (!catalogue.getLocations().get(id).isWilderness() && !area.isWildernessTravel())
                throw new IllegalArgumentException(id + ": non-Wilderness location needs Wilderness travel exposure");
            evidence(id, area.getEvidence());
            catalogue.getWildernessAreas().put(id, area);
        }
        for (SlayerCatalogue.Location location : catalogue.getLocations().values())
        {
            if (location.isWilderness() && !catalogue.getWildernessAreas().containsKey(location.getId()))
                throw new IllegalArgumentException("Missing Wilderness risk profile: " + location.getId());
        }
    }

    private static int positiveId(String owner, String raw)
    {
        if (!raw.matches("[1-9][0-9]*")) throw new IllegalArgumentException(owner + ": invalid item ID " + raw);
        return Integer.parseInt(raw);
    }

    private static void quantities(String owner, Map<Integer, Integer> quantities)
    {
        if (quantities == null) throw new IllegalArgumentException(owner + ": null item quantities");
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet())
        {
            if (entry.getKey() <= 0 || entry.getValue() == null || entry.getValue() <= 0)
                throw new IllegalArgumentException(owner + ": invalid component item quantity");
        }
    }

    private static void evidence(String owner, List<SlayerCatalogue.Evidence> evidence)
    {
        if (evidence == null || evidence.isEmpty())
            throw new IllegalArgumentException(owner + ": missing death rule evidence");
        for (SlayerCatalogue.Evidence source : evidence)
        {
            if (source == null || source.isMissing() || source.getPageId() <= 0 || source.getRevisionId() <= 0
                || source.getTitle() == null || source.getTitle().isEmpty() || source.getUrl() == null
                || !source.getUrl().startsWith("https://oldschool.runescape.wiki/w/"))
                throw new IllegalArgumentException(owner + ": incomplete death rule evidence");
            Instant.parse(source.getTimestamp());
        }
    }

    private static JsonObject read(Path path) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            JsonObject source = GSON.fromJson(reader, JsonObject.class);
            if (source == null) throw new IllegalArgumentException("Empty death rule source: " + path);
            return source;
        }
    }
}

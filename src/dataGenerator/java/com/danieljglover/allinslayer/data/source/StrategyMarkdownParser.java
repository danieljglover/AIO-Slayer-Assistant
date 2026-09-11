package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class StrategyMarkdownParser
{
    private StrategyMarkdownParser()
    {
    }

    public static SourceStrategy parse(String path, String markdown)
    {
        if (markdown == null || !markdown.startsWith("---\n"))
        {
            throw new IllegalArgumentException("strategy markdown must start with frontmatter: " + path);
        }

        String[] lines = markdown.split("\\r?\\n", -1);
        SourceStrategy strategy = new SourceStrategy();
        List<SourceStrategyWeapon> secondaryWeapons = new ArrayList<>();
        SourceStrategyWeapon currentSecondary = null;
        int bodyStart = -1;

        for (int i = 1; i < lines.length; i++)
        {
            String raw = lines[i];
            String trimmed = raw.trim();
            if ("---".equals(trimmed))
            {
                bodyStart = i + 1;
                break;
            }
            if (trimmed.isEmpty())
            {
                continue;
            }
            if (trimmed.startsWith("- "))
            {
                currentSecondary = new SourceStrategyWeapon();
                secondaryWeapons.add(currentSecondary);
                applySecondary(currentSecondary, trimmed.substring(2));
                continue;
            }
            if (raw.startsWith(" ") && currentSecondary != null)
            {
                applySecondary(currentSecondary, trimmed);
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon < 0)
            {
                throw new IllegalArgumentException("invalid strategy frontmatter line in " + path + ": " + raw);
            }
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            apply(strategy, key, value);
        }

        if (bodyStart < 0)
        {
            throw new IllegalArgumentException("strategy markdown missing closing frontmatter: " + path);
        }
        strategy.setSecondaryWeapons(secondaryWeapons);
        strategy.setBody(String.join("\n", Arrays.copyOfRange(lines, bodyStart, lines.length)));
        return strategy;
    }

    private static void apply(SourceStrategy strategy, String key, String value)
    {
        switch (key)
        {
            case "strategyId":
                strategy.setStrategyId(value);
                break;
            case "variantIds":
                strategy.setVariantIds(inlineList(value));
                break;
            case "primaryStyle":
                strategy.setPrimaryStyle(CombatStyle.valueOf(value));
                break;
            case "primaryWeapons":
                strategy.setPrimaryWeapons(inlineList(value));
                break;
            case "note":
                strategy.setNote(value);
                break;
            case "sourceUrl":
                strategy.setSourceUrl(value);
                break;
            case "secondaryWeapons":
                break;
            default:
                throw new IllegalArgumentException("unknown strategy frontmatter key: " + key);
        }
    }

    private static void applySecondary(SourceStrategyWeapon weapon, String line)
    {
        int colon = line.indexOf(':');
        if (colon < 0)
        {
            throw new IllegalArgumentException("invalid secondary weapon line: " + line);
        }
        String key = line.substring(0, colon).trim();
        String value = line.substring(colon + 1).trim();
        switch (key)
        {
            case "weaponId":
                weapon.setWeaponId(value);
                break;
            case "style":
                weapon.setStyle(CombatStyle.valueOf(value));
                break;
            default:
                throw new IllegalArgumentException("unknown secondary weapon key: " + key);
        }
    }

    private static List<String> inlineList(String value)
    {
        if (value == null || value.length() < 2 || value.charAt(0) != '[' || value.charAt(value.length() - 1) != ']')
        {
            return Collections.emptyList();
        }
        String inner = value.substring(1, value.length() - 1).trim();
        if (inner.isEmpty())
        {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (String part : inner.split(","))
        {
            String trimmed = part.trim();
            if (!trimmed.isEmpty())
            {
                values.add(trimmed);
            }
        }
        return values;
    }
}

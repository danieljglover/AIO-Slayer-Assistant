package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.StrategyItemRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compile-time resolver from the free-text item names in strategy {@code requiredOrKeyItems}/
 * {@code inventory} lists to real item ids, over two catalogues: the {@code items/} supply catalogue
 * ({@link SourceItem}) and the {@code weapons/} gear catalogue ({@link SourceWeapon}). A supply match
 * is packable by the trip planner; a gear match is handled by the gear selector (never packed as an
 * inventory slot).
 *
 * <p>The authored strings are messy ("Prayer potions or super restores", "Dinh's bulwark or goading
 * potion", "twisted-bow", "2 Divine super combat potions", "Extended antifire for the quickest
 * route"). Resolution is deliberately tolerant but never fabricates ids (the {@code StrategyWeapon}
 * discipline): it splits {@code " or "} into alternatives, strips trailing qualifier clauses and a
 * leading count, then matches on a compact key (letters+digits only) so kebab-case ids, apostrophes,
 * and spacing all normalise to one form. An unmatched name yields a ref with empty {@code itemIds} -
 * an honest advisory - and is recorded for the build's unresolved-name report (the catalogue
 * authoring worklist).
 */
final class ItemNameResolver
{
    private static final Pattern LEADING_COUNT = Pattern.compile("^(\\d+)\\s+(.+)$");
    private static final Pattern QUALIFIER = Pattern.compile(
        "(?i)\\s+(?:for|if|when|unless|while|to)\\s+.*$");
    private static final Pattern OR_SPLIT = Pattern.compile("(?i)\\s+or\\s+");

    private static final class Target
    {
        final List<Integer> itemIds;
        final boolean supply;

        Target(List<Integer> itemIds, boolean supply)
        {
            this.itemIds = itemIds;
            this.supply = supply;
        }
    }

    private final Map<String, Target> index = new LinkedHashMap<>();
    /** Distinct unresolved authored names -> occurrence count, for the build worklist report. */
    private final Map<String, Integer> unresolved = new LinkedHashMap<>();

    ItemNameResolver(List<SourceItem> items, List<SourceWeapon> weapons)
    {
        // Supplies first so a name shared with a weapon resolves to the packable supply.
        if (items != null)
        {
            for (SourceItem item : items)
            {
                if (item == null || item.getItemIds() == null || item.getItemIds().isEmpty())
                {
                    continue;
                }
                Target target = new Target(new ArrayList<>(item.getItemIds()), true);
                put(item.getItemKey(), target);
                put(item.getName(), target);
                if (item.getAliases() != null)
                {
                    for (String alias : item.getAliases())
                    {
                        put(alias, target);
                    }
                }
            }
        }
        if (weapons != null)
        {
            for (SourceWeapon weapon : weapons)
            {
                if (weapon == null || weapon.getItemIds() == null || weapon.getItemIds().isEmpty())
                {
                    continue;
                }
                Target target = new Target(new ArrayList<>(weapon.getItemIds()), false);
                put(weapon.getWeaponId(), target);
                put(weapon.getName(), target);
            }
        }
    }

    private void put(String key, Target target)
    {
        String compact = compact(key);
        if (!compact.isEmpty())
        {
            index.putIfAbsent(compact, target);
        }
    }

    /** Resolve a whole authored list into refs, skipping blank entries. Never null. */
    List<StrategyItemRef> resolveList(List<String> authored)
    {
        List<StrategyItemRef> refs = new ArrayList<>();
        if (authored == null)
        {
            return refs;
        }
        for (String raw : authored)
        {
            StrategyItemRef ref = resolve(raw);
            if (ref != null)
            {
                refs.add(ref);
            }
        }
        return refs;
    }

    /** Resolve one authored string (with its "or"-alternatives) into a single ref, or null when blank. */
    StrategyItemRef resolve(String raw)
    {
        if (raw == null || raw.trim().isEmpty())
        {
            return null;
        }
        String[] options = OR_SPLIT.split(raw.trim());
        StrategyItemRef primary = resolveOption(options[0]);
        if (primary == null)
        {
            return null;
        }
        for (int i = 1; i < options.length; i++)
        {
            StrategyItemRef alt = resolveOption(options[i]);
            if (alt == null)
            {
                continue;
            }
            if (primary.getAlternatives() == null)
            {
                primary.setAlternatives(new ArrayList<>());
            }
            primary.getAlternatives().add(alt);
        }
        return primary;
    }

    private StrategyItemRef resolveOption(String option)
    {
        if (option == null)
        {
            return null;
        }
        String cleaned = QUALIFIER.matcher(option.trim()).replaceAll("").trim();
        if (cleaned.isEmpty())
        {
            return null;
        }
        Integer quantity = null;
        Matcher count = LEADING_COUNT.matcher(cleaned);
        if (count.matches())
        {
            quantity = Integer.parseInt(count.group(1));
            cleaned = count.group(2).trim();
        }
        Target target = index.get(compact(cleaned));
        if (target == null)
        {
            unresolved.merge(cleaned, 1, Integer::sum);
            return new StrategyItemRef(cleaned, Collections.emptyList(), false, null, quantity);
        }
        return new StrategyItemRef(cleaned, new ArrayList<>(target.itemIds), target.supply, null, quantity);
    }

    /** Compact key: lowercase, letters+digits only, so spacing/apostrophes/kebab-case all coincide. */
    private static String compact(String value)
    {
        if (value == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++)
        {
            char c = Character.toLowerCase(value.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** The distinct unresolved names with counts, highest first, for the build worklist report. */
    List<String> unresolvedReport()
    {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(unresolved.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries)
        {
            lines.add(entry.getValue() + "x  " + entry.getKey());
        }
        return lines;
    }
}

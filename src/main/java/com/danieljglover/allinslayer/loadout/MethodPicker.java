package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterStrategy;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.StrategyItemRef;
import com.danieljglover.allinslayer.model.StrategyMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chooses which strategy method drives the loadout - the auto-pick behind the manual override. Only
 * style-bearing methods are pickable (a {@code role == "general"} / null-style method documents
 * strategy-wide key items, not a playable method). The pick is: an explicit {@code selectedMethodId}
 * wins outright; otherwise, among the methods matching the active style constraint (the user's style
 * toggle, or all styles when none), the highest-scoring feasible method wins.
 *
 * <p>Feasibility gates a method against the effective location and ownership: a cannon method needs an
 * owned cannon at a cannon-effective spot; a burst/barrage method needs a burst/multi spot; a
 * Wilderness-role method is deprioritised off-Wilderness. Infeasible methods are penalised, not
 * excluded, so a strategy always yields a method. Full per-method DPS/cost ranking is deferred (the
 * advisor computes DPS for the chosen style only); the default here prefers the guide's primary
 * style and its {@code default}-role method, which is the intended recommendation.
 */
public final class MethodPicker
{
    private MethodPicker()
    {
    }

    /**
     * @param strategy the valid strategy (its {@code methods}); null/empty -> null
     * @param owned the player's items (cannon feasibility); may be null
     * @param location the effective location (cannon/burst/wilderness feasibility); may be null
     * @param styleConstraint the user's style toggle - restrict to this style; null = any style
     * @param selectedMethodId the user's explicit method choice - wins when it matches; may be null
     */
    public static StrategyMethod pick(MonsterStrategy strategy, OwnedItems owned, SlayerLocation location,
        CombatStyle styleConstraint, String selectedMethodId)
    {
        List<StrategyMethod> pickable = pickable(strategy);
        if (pickable.isEmpty())
        {
            return null;
        }
        if (selectedMethodId != null)
        {
            for (StrategyMethod method : pickable)
            {
                if (selectedMethodId.equals(method.getMethodId()))
                {
                    return method;
                }
            }
        }
        List<StrategyMethod> candidates = new ArrayList<>();
        for (StrategyMethod method : pickable)
        {
            if (styleConstraint == null || method.getCombatStyle() == styleConstraint)
            {
                candidates.add(method);
            }
        }
        if (candidates.isEmpty())
        {
            candidates = pickable;
        }
        StrategyMethod best = null;
        int bestScore = Integer.MIN_VALUE;
        for (StrategyMethod method : candidates)
        {
            int score = score(method, owned, location, strategy);
            if (score > bestScore)
            {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    /** The style-bearing (pickable) methods of a strategy, in authored order. */
    public static List<StrategyMethod> pickable(MonsterStrategy strategy)
    {
        List<StrategyMethod> pickable = new ArrayList<>();
        if (strategy == null || strategy.getMethods() == null)
        {
            return pickable;
        }
        for (StrategyMethod method : strategy.getMethods())
        {
            if (method != null && method.getCombatStyle() != null)
            {
                pickable.add(method);
            }
        }
        return pickable;
    }

    private static int score(StrategyMethod method, OwnedItems owned, SlayerLocation location,
        MonsterStrategy strategy)
    {
        int score = 0;
        if (isCannonMethod(method))
        {
            boolean usable = location != null && location.isCannonEffective()
                && owned != null && InventorySelector.ownsCannon(owned);
            score += usable ? 15 : -100;
        }
        if (isBurstMethod(method))
        {
            boolean usable = location != null && (location.isBurst() || location.isMulti());
            score += usable ? 10 : -50;
        }
        if (isWildernessRole(method) && !(location != null && location.isWilderness()))
        {
            score -= 40;
        }
        if ("default".equalsIgnoreCase(method.getRole()))
        {
            score += 20;
        }
        if (strategy.getPrimaryStyle() != null && method.getCombatStyle() == strategy.getPrimaryStyle())
        {
            score += 10;
        }
        return score;
    }

    private static boolean isCannonMethod(StrategyMethod method)
    {
        return mentions(method, "cannon");
    }

    private static boolean isBurstMethod(StrategyMethod method)
    {
        return mentions(method, "burst") || mentions(method, "barrage");
    }

    private static boolean isWildernessRole(StrategyMethod method)
    {
        String role = method.getRole() == null ? "" : method.getRole().toLowerCase(Locale.ROOT);
        return role.contains("wilderness") || role.contains("wildy");
    }

    /** True when the method's id/label/role or any key item name contains {@code needle}. */
    private static boolean mentions(StrategyMethod method, String needle)
    {
        if (contains(method.getMethodId(), needle) || contains(method.getLabel(), needle)
            || contains(method.getRole(), needle))
        {
            return true;
        }
        if (method.getKeyItems() != null)
        {
            for (StrategyItemRef ref : method.getKeyItems())
            {
                if (ref != null && contains(ref.getName(), needle))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean contains(String haystack, String needle)
    {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }
}

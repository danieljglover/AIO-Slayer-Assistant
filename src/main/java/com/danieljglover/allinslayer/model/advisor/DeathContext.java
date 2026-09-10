package com.danieljglover.allinslayer.model.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Immutable observed state. This describes the present, not prayer or skull status at a future death. */
@Getter
@EqualsAndHashCode
public final class DeathContext
{
    private final boolean known;
    private final boolean skulled;
    private final boolean protectItemActive;
    private final boolean highRisk;
    private final boolean ultimateIronman;
    private final int wildernessLevel;
    private final Map<Integer, Integer> pouchContents;
    private final Map<Integer, Integer> lootingBagContents;
    private final boolean pouchContentsKnown;
    private final boolean lootingBagContentsKnown;
    private final String lootingBagDetail;
    private final Map<Integer, Integer> quiverContents;
    private final boolean quiverContentsKnown;

    public DeathContext(boolean known, boolean skulled, boolean protectItemActive,
        boolean highRisk, boolean ultimateIronman, int wildernessLevel,
        Map<Integer, Integer> pouchContents, Map<Integer, Integer> lootingBagContents,
        boolean pouchContentsKnown, boolean lootingBagContentsKnown)
    {
        this(known, skulled, protectItemActive, highRisk, ultimateIronman, wildernessLevel,
            pouchContents, lootingBagContents, pouchContentsKnown, lootingBagContentsKnown, null, false);
    }

    public DeathContext(boolean known, boolean skulled, boolean protectItemActive,
        boolean highRisk, boolean ultimateIronman, int wildernessLevel,
        Map<Integer, Integer> pouchContents, Map<Integer, Integer> lootingBagContents,
        boolean pouchContentsKnown, boolean lootingBagContentsKnown,
        Map<Integer, Integer> quiverContents, boolean quiverContentsKnown)
    {
        this(known, skulled, protectItemActive, highRisk, ultimateIronman, wildernessLevel,
            pouchContents, lootingBagContents, pouchContentsKnown, lootingBagContentsKnown,
            quiverContents, quiverContentsKnown, "Bag contents have not been observed this session.");
    }

    public DeathContext(boolean known, boolean skulled, boolean protectItemActive,
        boolean highRisk, boolean ultimateIronman, int wildernessLevel,
        Map<Integer, Integer> pouchContents, Map<Integer, Integer> lootingBagContents,
        boolean pouchContentsKnown, boolean lootingBagContentsKnown,
        Map<Integer, Integer> quiverContents, boolean quiverContentsKnown, String lootingBagDetail)
    {
        this.known = known;
        this.skulled = skulled;
        this.protectItemActive = protectItemActive;
        this.highRisk = highRisk;
        this.ultimateIronman = ultimateIronman;
        this.wildernessLevel = Math.max(-1, wildernessLevel);
        this.pouchContents = quantities(pouchContents);
        this.lootingBagContents = quantities(lootingBagContents);
        this.pouchContentsKnown = pouchContentsKnown;
        this.lootingBagContentsKnown = lootingBagContentsKnown;
        this.lootingBagDetail = lootingBagDetail;
        this.quiverContents = quantities(quiverContents);
        this.quiverContentsKnown = quiverContentsKnown;
    }

    public static DeathContext unknown()
    {
        return new DeathContext(false, false, false, false, false, -1, null, null, false, false);
    }

    private static Map<Integer, Integer> quantities(Map<Integer, Integer> input)
    {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        if (input != null)
        {
            input.forEach((id, quantity) ->
            {
                if (id != null && id > 0 && quantity != null && quantity > 0)
                {
                    result.put(id, quantity);
                }
            });
        }
        return Collections.unmodifiableMap(result);
    }
}

package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reviewed PvP death behavior for an exact item form, including its lock state. */
@Data
@NoArgsConstructor
public class DeathRule
{
    public enum Kind
    {
        DROP, KEEP, REPAIR, LOCKABLE, POUCH, ALWAYS_LOST, CONVERT, UNKNOWN
    }

    private Kind kind = Kind.UNKNOWN;
    private boolean locked;
    private boolean protectable = true;
    private long protectionValue = -1;
    private long repairCost;
    private boolean repairCostKnown = true;
    private Map<Integer, Integer> repairCostItems = new LinkedHashMap<>();
    private Map<Integer, Integer> replacementItems = new LinkedHashMap<>();
    private boolean contentsLost;
    private boolean chargesKeptWhenProtected;
    private String contentsType = "";
    private boolean permanentLoss;
    private long alwaysLostValue;
    private Map<Integer, Integer> alwaysLostItems = new LinkedHashMap<>();
    private String note = "";
    private List<SlayerCatalogue.Evidence> evidence = new ArrayList<>();
}

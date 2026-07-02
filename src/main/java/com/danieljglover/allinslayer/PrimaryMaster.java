package com.danieljglover.allinslayer;

/**
 * The config "primary master" choice (WA-9, PD-E): the master the panel's selector defaults to
 * when no live signal picks one. {@code masterId} matches the {@code masters/*.json} data ids;
 * {@code toString()} is the human name the RuneLite config dropdown renders.
 */
public enum PrimaryMaster
{
    TURAEL("turael", "Turael"),
    SPRIA("spria", "Spria"),
    MAZCHNA("mazchna", "Mazchna"),
    VANNAKA("vannaka", "Vannaka"),
    CHAELDAR("chaeldar", "Chaeldar"),
    KONAR("konar", "Konar quo Maten"),
    KRYSTILIA("krystilia", "Krystilia"),
    NIEVE("nieve", "Nieve"),
    DURADEL("duradel", "Duradel");

    private final String masterId;
    private final String displayName;

    PrimaryMaster(String masterId, String displayName)
    {
        this.masterId = masterId;
        this.displayName = displayName;
    }

    public String getMasterId()
    {
        return masterId;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}

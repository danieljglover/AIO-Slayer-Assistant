package com.danieljglover.allinslayer.model.advisor;

/** The destination to prepare for when a trip finishes. */
public enum ReturnDestination
{
    SLAYER_MASTER("Slayer master"), BANK("Bank"), HOUSE("House");

    private final String label;

    ReturnDestination(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}

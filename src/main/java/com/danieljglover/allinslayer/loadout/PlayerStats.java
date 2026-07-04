package com.danieljglover.allinslayer.loadout;

import lombok.Value;

@Value
public class PlayerStats
{
    int attack;
    int strength;
    int defence;
    int ranged;
    int magic;
    int slayer;
    // Sustain inputs for the dynamic trip planner (SustainModel): Hitpoints sizes food per kill;
    // Prayer sizes the prayer-potion count and the starting prayer pool for a prayer-primary method.
    int hitpoints;
    int prayer;
}

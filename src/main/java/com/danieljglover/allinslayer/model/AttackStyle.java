package com.danieljglover.allinslayer.model;

/**
 * WD-1 (ADR-0020 #1): the monster attack styles you can pray against, driving the note-only
 * prayer advisory (WD-3). {@code MELEE|RANGED|MAGIC} map to the overhead protection prayers;
 * {@code DRAGONFIRE} defers to the existing antifire note (no duplication); {@code TYPELESS}
 * is unprayable (documented, no prayer clause). Gson maps an absent/unknown token to null.
 */
public enum AttackStyle
{
    MELEE,
    RANGED,
    MAGIC,
    DRAGONFIRE,
    TYPELESS
}

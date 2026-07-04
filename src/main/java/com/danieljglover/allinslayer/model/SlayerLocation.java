package com.danieljglover.allinslayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlayerLocation
{
    private String name;
    private boolean multi;
    private boolean cannon;
    private boolean burst;
    private boolean konarLockable;
    // True for a Wilderness location: when the user selects it, Wilderness weapons (Viggora's/Craw's/
    // Webweaver/Thammaron's) get their +50% vs-Wilderness bonus (ADR-0009 A.5). Gson defaults false.
    private boolean wilderness;
    // DT-B2 (ADR-0017): safespot support at this location. Surfaces a ranged/magic method hint
    // (DT-B10) - a note only, NO combat-maths change (NG-4). Gson defaults false.
    private boolean safeSpot;
    // DT-B2 (ADR-0017): free-text access/travel note for this location (quest/teleport/agility gate).
    // Rendered as a note. Gson defaults null.
    private String accessNote;
    // WD-5a (ADR-0020 #2): the optional task-scoped location-quality overlay compiled from
    // locationComparison[]. Gson default null = no comparison authored = today's behaviour (FR-6).
    private LocationQuality quality;

    /**
     * Cannon usability for THIS task at this location. The task-scoped {@link
     * LocationQuality#getCannonable() cannonable} overlay wins when authored: it carries per-area truth
     * the shared location flag cannot (partial in-dungeon cannon bans like Karuulm's wyrm/Alchemical
     * Hydra areas, and cannon-immune targets like kurask in the Iorwerth Dungeon). An absent overlay or
     * null flag falls back to the location's {@code cannon} flag (FR-6).
     */
    public boolean isCannonEffective()
    {
        if (quality != null && quality.getCannonable() != null)
        {
            return quality.getCannonable();
        }
        return cannon;
    }

    /**
     * Backward-compatible constructor for locations without the WD-5a quality overlay: {@code quality
     * = null}. Keeps the compiler emission (DT-B4) and every 8-arg call site compiling and byte-
     * identical for tasks lacking a locationComparison.
     */
    public SlayerLocation(String name, boolean multi, boolean cannon, boolean burst,
        boolean konarLockable, boolean wilderness, boolean safeSpot, String accessNote)
    {
        this(name, multi, cannon, burst, konarLockable, wilderness, safeSpot, accessNote, null);
    }

    /**
     * Backward-compatible constructor for locations without safespot/access enrichment (DT-B2):
     * {@code safeSpot = false}, {@code accessNote = null}. Keeps the ~110 existing call sites and the
     * pre-DT-B4 compiler emission compiling and byte-identical.
     */
    public SlayerLocation(String name, boolean multi, boolean cannon, boolean burst,
        boolean konarLockable, boolean wilderness)
    {
        this(name, multi, cannon, burst, konarLockable, wilderness, false, null);
    }

    /** Backward-compatible constructor for non-Wilderness locations (wilderness = false). */
    public SlayerLocation(String name, boolean multi, boolean cannon, boolean burst,
        boolean konarLockable)
    {
        this(name, multi, cannon, burst, konarLockable, false);
    }
}

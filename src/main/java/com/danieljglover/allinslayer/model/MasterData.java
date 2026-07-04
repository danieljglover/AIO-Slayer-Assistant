package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Slayer master at runtime (WA-2, ADR-0018 #1/#2): carried by the second generated resource
 * {@code slayer-meta.json}, compiled from {@code masters/*.json} (WA-8). Master-INTRINSIC data
 * only - assignment stays task-side ({@code TaskData.assignedBy} / {@code weightByMaster}), so
 * there is no assignable-task list here and un-authored families need no FK. All enrichment
 * fields Gson-default-null: a stub master loads cleanly as identity-only.
 */
@Data
@NoArgsConstructor
public class MasterData
{
    private String masterId;
    private String name;
    private List<String> aliases;
    private String location;
    private MasterRequirements requirements;
    private MasterEconomy economy;
    // Master-intrinsic wiki facts (task-changing/streak rules, key drops, location assignment,
    // Wilderness constraint) that the structured fields cannot express; null = unauthored.
    private List<String> notes;
}

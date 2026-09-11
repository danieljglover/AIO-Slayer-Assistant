package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceMaster
{
    private String masterId;
    private String name;
    private List<String> aliases;
    private String unlockNote;
    // WA-3 (ADR-0018 #2): master-INTRINSIC data only - no assignable task list (assignment stays
    // task-side via task.masterIds). All Gson-default-null so the existing stubs parse unchanged.
    private Integer wikiPageId;
    private String location;
    private SourceMasterRequirements requirements;
    private SourceMasterEconomy economy;
    // Master-intrinsic wiki facts the structured fields cannot express: task-changing and
    // streak-reset rules, key drops (brimstone/Larran's), Konar's location assignment,
    // Krystilia's Wilderness constraint, replacement-NPC lore, and cape-bypass rules.
    private List<String> notes;
}

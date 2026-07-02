package com.danieljglover.allinslayer.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskData
{
    private String task;
    private int slayerTargetId; // SLAYER_TARGET varp value identifying this task (0 if unmapped)
    private int slayerLevel;
    private List<String> questReqs;
    private List<String> assignedBy;
    private Map<String, int[]> amountByMaster;
    // Extended assignment range per master once the EXTENSION unlock is owned (WA-1, ADR-0018 #4).
    // Gson defaults null -> the task has no extension (today's behaviour, FR-6).
    private Map<String, int[]> extendedAmount;
    // Per-(master, task) assignment weight (WA-1, ADR-0018 #3), parallel to amountByMaster.
    // Gson defaults null -> no weight data authored yet (WC-W retrofits the existing 43).
    private Map<String, Integer> weightByMaster;
    // The reward-point unlocks that affect this assignment (WA-1, ADR-0018 #4/#5). Gson defaults
    // null -> no unlock data (today's behaviour, FR-6). The EXTENSION-typed entry enables
    // extendedAmount (cross-validated at build time, WA-7).
    private List<TaskUnlock> unlocks;
    private List<String> monsters;
    private List<Integer> npcIds;
    private Weakness weakness;
    private MonsterDefence monsterDefence;
    private boolean slayerHelmApplies;
    // True when the task monster is undead (the Salve-amulet vs-undead bonus predicate, ADR-0007).
    // Gson defaults to false for tasks that omit the key. In this dataset exactly Ankou and Aberrant
    // spectres are undead.
    private boolean undead;
    // True when the task monster is draconic (the Dragon-hunter vs-dragon bonus predicate, ADR-0008).
    // Gson defaults to false. Set on all draconic tasks (dragons + wyverns) excluding Elvarg/revenants.
    private boolean dragon;
    // True when the task monster is a demon (the demonbane predicate, ADR-0009): Arclight/Emberlight/
    // Darklight/Scorching bow. Gson defaults to false. Exactly {Abyssal demons, Black demons, Greater
    // demons, Nechryael} - the "devil"-named tasks are NOT demons.
    private boolean demon;
    // True when the task monster is a kalphite (the Keris predicate, ADR-0009). Gson defaults to
    // false. Exactly {Kalphite} in this dataset.
    private boolean kalphite;
    private Integer requiredItemId;
    private String requiredItemName;
    private List<SlayerLocation> locations;
    private String recommendedMethod;
    // Optional per-monster variants (MV-A1, ADR-0010). Gson defaults null -> the task uses its
    // task-level profile exactly as today (FR-6 back-compat). When present, the user-selected variant
    // resolves to the MonsterProfile that drives the engine; the task-level fields above remain the
    // default variant's profile and the regression anchor.
    private List<MonsterVariant> variants;
    // WD-2 (ADR-0020 #1): optional task-level monster offence, the fallback when a variant omits its
    // own offence (MonsterProfile.fromVariant). Gson default null (FR-6).
    private MonsterOffence offence;
}

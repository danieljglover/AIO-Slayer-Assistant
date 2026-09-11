package com.danieljglover.allinslayer.data.source;

import java.util.List;
import java.util.Map;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.Weakness;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceTask
{
    private String taskId;
    private String name;
    private Integer wikiPageId;
    private Integer combatLevel;
    private int slayerTargetId;
    private int slayerLevel;
    private List<String> questReqs;
    private List<String> masterIds;
    private Map<String, int[]> amountByMaster;
    private Map<String, int[]> extendedAmount;
    // WA-3 (ADR-0018 #3): per-(master, task) assignment weight, beside the per-master amount -
    // the single task-side source of assignment truth. Gson-default-null = not yet authored.
    private Map<String, Integer> weightByMaster;
    private List<SourceTaskUnlock> unlocks;
    private List<String> monsterIds;
    private List<String> variantIds;
    private String defaultVariantId;
    private Weakness weakness;
    private MonsterDefence monsterDefence;
    private boolean slayerHelmApplies;
    private boolean undead;
    private boolean dragon;
    private boolean demon;
    private boolean kalphite;
    private Integer requiredItemId;
    private String requiredItemName;
    private List<String> locationIds;
    private List<SourceTaskVariantInfo> variantInfo;
    private List<SourceTaskLocationComparison> locationComparison;
    private List<String> taskNotes;
    private String recommendedMethod;
    // WD-1 (ADR-0020 #1): optional task-level offence default, the per-field fallback when a family's
    // variants share offence and a variant omits it (WD-2). Gson-default-null (FR-6).
    private MonsterOffence offence;
}

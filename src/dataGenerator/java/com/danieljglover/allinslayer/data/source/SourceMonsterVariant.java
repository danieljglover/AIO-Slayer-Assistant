package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.Weakness;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceMonsterVariant
{
    private String variantId;
    private String name;
    private List<Integer> npcIds;
    private Integer combatLevel;
    private Weakness weakness;
    private MonsterDefence monsterDefence;
    private boolean demon;
    private boolean dragon;
    private boolean kalphite;
    private boolean undead;
    private boolean boss;
    private String location;
    private String locationId;
    private String requirement;
    private Integer bossId;
    private String strategyId;
    // WD-1 (ADR-0020 #1): the NEW additive monster-offence domain (HP/max-hit/attack-styles/magic
    // level/poison flags). Gson-default-null = not authored = no advisory (FR-6). Per-variant so a
    // superior's harder hits are honest; the task-level offence is the fallback (WD-2).
    private MonsterOffence offence;
}

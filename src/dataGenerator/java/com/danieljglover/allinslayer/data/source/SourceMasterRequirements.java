package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Slayer master's use-requirements (WA-3, ADR-0018 #2): the levels/quests a player needs before
 * this master will assign tasks. All fields Gson-default-null = unauthored/none.
 */
@Data
@NoArgsConstructor
public class SourceMasterRequirements
{
    private Integer combatLevel;
    private Integer slayerLevel;
    private List<String> quests;
}

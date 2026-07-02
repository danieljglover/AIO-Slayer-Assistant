package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A master's use-requirements at runtime (WA-2, mirrors {@code SourceMasterRequirements} so the
 * WA-8 compile is a straight copy). All Gson-default-null = unauthored/none.
 */
@Data
@NoArgsConstructor
public class MasterRequirements
{
    private Integer combatLevel;
    private Integer slayerLevel;
    private List<String> quests;
}

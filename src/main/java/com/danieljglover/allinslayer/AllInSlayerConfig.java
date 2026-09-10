package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.model.advisor.RecommendationGoal;
import com.danieljglover.allinslayer.model.advisor.ReturnDestination;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(AllInSlayerConfig.GROUP)
public interface AllInSlayerConfig extends Config
{
    String GROUP = "allinslayer";

    @ConfigItem(keyName = "returnDestination", name = "After task",
        description = "Pack an owned teleport towards your Slayer master, a bank, or your house. Wilderness escape is planned separately.", position = 6)
    default ReturnDestination returnDestination()
    {
        return ReturnDestination.SLAYER_MASTER;
    }

    @ConfigItem(keyName = "recommendationGoal", name = "Recommendation goal",
        description = "Rank feasible wiki strategies by Slayer XP, profit, or low effort", position = 0)
    default RecommendationGoal recommendationGoal()
    {
        return RecommendationGoal.SLAYER_XP;
    }

    @ConfigItem(keyName = "allowWilderness", name = "Include Wilderness",
        description = "Allow Wilderness recommendations, including for active Wilderness assignments", position = 1)
    default boolean allowWilderness()
    {
        return false;
    }

    @ConfigItem(keyName = "allowGroups", name = "Include group methods",
        description = "Include methods requiring a partner or team; choose a documented role in the panel", position = 2)
    default boolean allowGroups()
    {
        return false;
    }

    @net.runelite.client.config.Range(min = 0)
    @ConfigItem(keyName = "wildernessRiskBudget", name = "Wilderness loss budget (gp)",
        description = "Maximum estimated trip loss without relying on Protect Item. Unknown costs and permanent untradeable loss block automatic Wilderness picks.", position = 3)
    default int wildernessRiskBudget()
    {
        return 500000;
    }

    @Range(min = 1, max = 16000)
    @ConfigItem(keyName = "plannedEtherCharges", name = "Planned ether charges",
        description = "Usable charges to prepare in each Wilderness weapon, plus its 1,000 ether activation deposit. Planned loss uses this target; carried loss uses fresh Check observations when available. The charge alert limit is separate.", position = 4)
    default int plannedEtherCharges()
    {
        return 500;
    }

    @Range(min = 1, max = 16000)
    @ConfigItem(keyName = "wildernessChargeLimit", name = "Wildy charge limit",
        description = "Warn when a Wilderness weapon has more than this many usable charges, excluding its 1,000 activation ether. Empty and unchecked weapons still need attention. Separate from the planned ether preparation amount.", position = 7)
    default int wildernessChargeLimit()
    {
        return 500;
    }

    @ConfigItem(keyName = "showOverlay", name = "Show task overlay",
        description = "Show the active task and selected preparation method", position = 5)
    default boolean showOverlay()
    {
        return true;
    }
}

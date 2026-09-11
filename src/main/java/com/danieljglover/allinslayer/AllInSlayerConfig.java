package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.model.advisor.RecommendationGoal;
import com.danieljglover.allinslayer.model.advisor.ReturnDestination;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
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

    @ConfigSection(name = "Melee boosts", description = "Reserve inventory space for melee boosts",
        position = 8, closedByDefault = true)
    String MELEE_BOOST_SECTION = "meleeBoosts";

    @Range(min = 0, max = 28)
    @ConfigItem(keyName = "meleeBoostSlots", name = "Boost slots",
        description = "Reserve this many inventory slots for melee boosts. Counts bottles, not doses; fullest owned doses first. 0 keeps automatic packing.",
        position = 0, section = MELEE_BOOST_SECTION)
    default int meleeBoostSlots()
    {
        return 0;
    }

    @ConfigItem(keyName = "meleeBoostItems", name = "Preferred boosts",
        description = "Comma-separated priority list. Choose the first owned usable option; pack its fullest owned doses first.",
        position = 1, section = MELEE_BOOST_SECTION)
    default String meleeBoostItems()
    {
        return "Super combat potion, Divine super combat potion, Combat potion";
    }

    @ConfigSection(name = "Ranged boosts", description = "Reserve inventory space for ranged boosts",
        position = 9, closedByDefault = true)
    String RANGED_BOOST_SECTION = "rangedBoosts";

    @Range(min = 0, max = 28)
    @ConfigItem(keyName = "rangedBoostSlots", name = "Boost slots",
        description = "Reserve this many inventory slots for ranged boosts. Counts bottles, not doses; fullest owned doses first. 0 keeps automatic packing.",
        position = 0, section = RANGED_BOOST_SECTION)
    default int rangedBoostSlots()
    {
        return 0;
    }

    @ConfigItem(keyName = "rangedBoostItems", name = "Preferred boosts",
        description = "Comma-separated priority list. Choose the first owned usable option; pack its fullest owned doses first.",
        position = 1, section = RANGED_BOOST_SECTION)
    default String rangedBoostItems()
    {
        return "Ranging potion, Divine ranging potion, Bastion potion, Divine bastion potion";
    }

    @ConfigSection(name = "Magic boosts", description = "Reserve inventory space for magic boosts",
        position = 10, closedByDefault = true)
    String MAGIC_BOOST_SECTION = "magicBoosts";

    @Range(min = 0, max = 28)
    @ConfigItem(keyName = "magicBoostSlots", name = "Boost slots",
        description = "Reserve this many inventory slots for magic boosts. Counts bottles, not doses; fullest owned doses first. A reusable heart uses one slot only. 0 keeps automatic packing.",
        position = 0, section = MAGIC_BOOST_SECTION)
    default int magicBoostSlots()
    {
        return 0;
    }

    @ConfigItem(keyName = "magicBoostItems", name = "Preferred boosts",
        description = "Comma-separated priority list. Choose the first owned usable option; pack its fullest owned doses first. Pack a reusable heart once.",
        position = 1, section = MAGIC_BOOST_SECTION)
    default String magicBoostItems()
    {
        return "Saturated heart, Imbued heart, Forgotten brew, Ancient brew, Divine magic potion, Magic potion";
    }

    @ConfigSection(name = "Food", description = "Choose the food used for all combat styles",
        position = 11, closedByDefault = true)
    String FOOD_SECTION = "food";

    @ConfigItem(keyName = "overrideFood", name = "Override food",
        description = "Fill remaining inventory slots with your preferred foods. Required strategy items and reserved boosts retain priority.",
        position = 0, section = FOOD_SECTION)
    default boolean overrideFood()
    {
        return false;
    }

    @ConfigItem(keyName = "preferredFoods", name = "Preferred foods",
        description = "Full food names, separated by commas, in priority order. Use owned food from each entry before the next. Non-stackable food only; blighted food needs a Wilderness combat destination.",
        position = 1, section = FOOD_SECTION)
    default String preferredFoods()
    {
        return "Shark";
    }
}

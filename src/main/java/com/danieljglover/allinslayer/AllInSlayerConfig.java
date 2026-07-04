package com.danieljglover.allinslayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(AllInSlayerConfig.GROUP)
public interface AllInSlayerConfig extends Config
{
    String GROUP = "allinslayer";

    @ConfigItem(
        keyName = "haveCannon",
        name = "I own a cannon",
        description = "Allow cannon-based location/method recommendations where cannons are permitted",
        position = 1
    )
    default boolean haveCannon()
    {
        return false;
    }

    @ConfigItem(
        keyName = "adviceMode",
        name = "Loadout mode",
        description = "Whether the recommended loadout favours DPS or lowest cost",
        position = 2
    )
    default AdviceMode adviceMode()
    {
        return AdviceMode.DPS;
    }

    @ConfigItem(
        keyName = "showOverlay",
        name = "Show task overlay",
        description = "Show the compact in-game overlay for the current task",
        position = 3
    )
    default boolean showOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showBankChecklist",
        name = "Show bank checklist",
        description = "While the bank is open, list the recommended items you still need to withdraw",
        position = 4
    )
    default boolean showBankChecklist()
    {
        return true;
    }

    @ConfigItem(
        keyName = "developerMode",
        name = "Developer mode",
        description = "Show the developer diagnostics section at the bottom of the side panel",
        position = 4
    )
    default boolean developerMode()
    {
        return false;
    }

    @ConfigItem(
        keyName = "dislikedTasks",
        name = "Disliked tasks",
        description = "Comma-separated task names you would rather skip or block; the panel then suggests "
            + "blocking them with your selected master (when affordable) or a free Turael skip",
        position = 5
    )
    default String dislikedTasks()
    {
        return "";
    }

    // WA-9 (ADR-0018 #7): the self-declared fallback for the SlayerUnlockStateProvider seam.
    // The varbit-backed provider (WA-10) reads these signals live; this section lets the feature
    // work fully when logged out or if a varbit read is ever unavailable.
    @ConfigSection(
        name = "Slayer points & unlocks",
        description = "Self-declared reward-point balance, owned unlocks and primary master "
            + "(used when the live client signals are unavailable)",
        position = 5,
        closedByDefault = true
    )
    String UNLOCKS_SECTION = "unlocks";

    @ConfigItem(
        keyName = "slayerRewardPoints",
        name = "Reward points",
        description = "Your current Slayer reward point balance",
        position = 1,
        section = UNLOCKS_SECTION
    )
    default int slayerRewardPoints()
    {
        return 0;
    }

    @ConfigItem(
        keyName = "primaryMaster",
        name = "Primary master",
        description = "The Slayer master the panel defaults to when the client exposes no signal",
        position = 2,
        section = UNLOCKS_SECTION
    )
    default PrimaryMaster primaryMaster()
    {
        return PrimaryMaster.DURADEL;
    }

    @ConfigItem(
        keyName = "ownsMalevolentMasquerade",
        name = "Malevolent masquerade",
        description = "I have unlocked crafting the Slayer helmet (400 points)",
        position = 3,
        section = UNLOCKS_SECTION
    )
    default boolean ownsMalevolentMasquerade()
    {
        return false;
    }

    @ConfigItem(
        keyName = "ownsBroaderFletching",
        name = "Broader Fletching",
        description = "I have unlocked fletching broad ammunition (300 points)",
        position = 4,
        section = UNLOCKS_SECTION
    )
    default boolean ownsBroaderFletching()
    {
        return false;
    }

    @ConfigItem(
        keyName = "ownsRingBling",
        name = "Ring bling",
        description = "I have unlocked crafting the Slayer ring (150 points)",
        position = 5,
        section = UNLOCKS_SECTION
    )
    default boolean ownsRingBling()
    {
        return false;
    }

    @ConfigItem(
        keyName = "ownsLikeABoss",
        name = "Like a Boss",
        description = "I have unlocked boss slayer tasks (200 points)",
        position = 6,
        section = UNLOCKS_SECTION
    )
    default boolean ownsLikeABoss()
    {
        return false;
    }

    @ConfigItem(
        keyName = "ownsTaskStorage",
        name = "Task Storage",
        description = "I have unlocked storing/swapping a slayer task (500 points)",
        position = 7,
        section = UNLOCKS_SECTION
    )
    default boolean ownsTaskStorage()
    {
        return false;
    }

    @ConfigItem(
        keyName = "ownsWildyMoreSlayer",
        name = "I Wildy More Slayer",
        description = "I have enabled Krystilia's Wilderness task list (free)",
        position = 8,
        section = UNLOCKS_SECTION
    )
    default boolean ownsWildyMoreSlayer()
    {
        return false;
    }

    @ConfigItem(
        keyName = "ownsBiggerAndBadder",
        name = "Bigger and Badder",
        description = "I have unlocked superior slayer monsters (50 points)",
        position = 9,
        section = UNLOCKS_SECTION
    )
    default boolean ownsBiggerAndBadder()
    {
        return false;
    }
}

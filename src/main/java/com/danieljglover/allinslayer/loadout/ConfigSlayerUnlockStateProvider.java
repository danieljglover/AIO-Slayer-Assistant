package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AllInSlayerConfig;
import java.util.Optional;
import java.util.OptionalInt;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The config-backed {@link SlayerUnlockStateProvider} (WA-9, ADR-0018 #7): the player
 * self-declares their point balance, the 7 global unlocks they own, and a primary master. Honest
 * limits: the config UI carries no toggle for cosmetic helm recolours or per-task unlock ids, so
 * those read unowned here - the varbit-backed implementation (WA-10) is the path that can answer
 * them from the client.
 */
@Singleton
public class ConfigSlayerUnlockStateProvider implements SlayerUnlockStateProvider
{
    private final AllInSlayerConfig config;

    @Inject
    public ConfigSlayerUnlockStateProvider(AllInSlayerConfig config)
    {
        this.config = config;
    }

    @Override
    public boolean ownsUnlock(String rewardOrUnlockId)
    {
        if (rewardOrUnlockId == null)
        {
            return false;
        }
        switch (rewardOrUnlockId)
        {
            case "malevolent-masquerade":
                return config.ownsMalevolentMasquerade();
            case "broader-fletching":
                return config.ownsBroaderFletching();
            case "ring-bling":
                return config.ownsRingBling();
            case "like-a-boss":
                return config.ownsLikeABoss();
            case "task-storage":
                return config.ownsTaskStorage();
            case "i-wildy-more-slayer":
                return config.ownsWildyMoreSlayer();
            case "bigger-and-badder":
                return config.ownsBiggerAndBadder();
            default:
                return false;
        }
    }

    @Override
    public OptionalInt rewardPoints()
    {
        return OptionalInt.of(Math.max(0, config.slayerRewardPoints()));
    }

    @Override
    public Optional<String> currentMaster()
    {
        return Optional.of(config.primaryMaster().getMasterId());
    }
}

package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.task.SlayerVarbits;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;

/**
 * The varbit-backed {@link SlayerUnlockStateProvider} (WA-10) - the DEFAULT binding per PD-A: the
 * WA-0 spike found clean, verified ids for all three signals
 * ({@code docs/full-review/spike-player-state.md}). Reads the reward-point balance (varbit 4068),
 * the current/assigning master (varbit 4067, value map 1..9) and the global reward-unlock varbits.
 *
 * <p><b>Fallback discipline (never guess):</b> delegates to the config-backed provider when the
 * client is not in game (logged out the varp cache would read a false 0), when the master varbit
 * is 0 (no task) or an unknown future value, and for any id it has no verified varbit for - the
 * three recolours whose name->colour pairing the spike could not verify (Eye see you / Absolutely
 * Slayin' / Oath Breaker). Per-task EXTENSION ids ARE mapped (FR-RV S2): the spike verified the
 * whole {@code SLAYER_LONGER_*} family, so an owned extension reads owned in game.
 *
 * <p><b>Client-thread only</b> (like every varbit read in the plugin): consumers call this from
 * the recompute path, never from a background thread.
 */
@Singleton
public class VarbitSlayerUnlockStateProvider implements SlayerUnlockStateProvider
{
    /** rewardId -> unlock varbit; only spike-verified rows (see class doc). */
    private static final Map<String, Integer> UNLOCK_VARBITS = new HashMap<>();

    /** Varbit 4067 value -> masterId (spike value map; index 0 unused = no task). */
    private static final String[] MASTER_BY_VALUE = {
        null, "turael", "mazchna", "vannaka", "chaeldar", "duradel", "nieve", "krystilia",
        "konar", "spria",
    };

    static
    {
        UNLOCK_VARBITS.put("malevolent-masquerade", SlayerVarbits.SLAYER_HELM_UNLOCKED);
        UNLOCK_VARBITS.put("ring-bling", SlayerVarbits.SLAYER_RING_UNLOCKED);
        UNLOCK_VARBITS.put("broader-fletching", SlayerVarbits.SLAYER_AMMO_UNLOCKED);
        UNLOCK_VARBITS.put("like-a-boss", SlayerVarbits.SLAYER_UNLOCK_BOSSES);
        UNLOCK_VARBITS.put("bigger-and-badder", SlayerVarbits.SLAYER_UNLOCK_SUPERIORMOBS);
        UNLOCK_VARBITS.put("task-storage", SlayerVarbits.SLAYER_UNLOCK_STORAGE);
        UNLOCK_VARBITS.put("i-wildy-more-slayer", SlayerVarbits.SLAYER_UNLOCK_WILDY_EXTRATASKS);
        // Wiki-verified name -> colour pairings only.
        UNLOCK_VARBITS.put("king-black-bonnet", SlayerVarbits.SLAYER_UNLOCK_HELM_BLACK);
        UNLOCK_VARBITS.put("kalphite-khat", SlayerVarbits.SLAYER_UNLOCK_HELM_GREEN);
        UNLOCK_VARBITS.put("unholy-helmet", SlayerVarbits.SLAYER_UNLOCK_HELM_RED);
        UNLOCK_VARBITS.put("dark-mantle", SlayerVarbits.SLAYER_UNLOCK_HELM_PURPLE);
        UNLOCK_VARBITS.put("undead-head", SlayerVarbits.SLAYER_UNLOCK_HELM_TURQUOISE);
        UNLOCK_VARBITS.put("use-more-head", SlayerVarbits.SLAYER_UNLOCK_HELM_HYDRA);
        UNLOCK_VARBITS.put("twisted-vision", SlayerVarbits.SLAYER_UNLOCK_HELM_TWISTED);
        // Task-extension unlocks (FR-RV S2): every EXTENSION unlockId in the dataset, keyed to
        // its spike-verified SLAYER_LONGER_* varbit - an owned extension must not read unowned.
        UNLOCK_VARBITS.put("smell-ya-later", SlayerVarbits.SLAYER_LONGER_ABERRANTSPECTRES);
        UNLOCK_VARBITS.put("augment-my-abbies", SlayerVarbits.SLAYER_LONGER_ABYSSALDEMONS);
        UNLOCK_VARBITS.put("ankou-very-much", SlayerVarbits.SLAYER_LONGER_ANKOU);
        UNLOCK_VARBITS.put("lets-stay-all-aquanite", SlayerVarbits.SLAYER_LONGER_AQUANITES);
        UNLOCK_VARBITS.put("more-eyes-than-sense", SlayerVarbits.SLAYER_LONGER_ARAXYTES);
        UNLOCK_VARBITS.put("birds-of-a-feather", SlayerVarbits.SLAYER_LONGER_AVIANSIES);
        UNLOCK_VARBITS.put("basilonger", SlayerVarbits.SLAYER_LONGER_BASILISK);
        UNLOCK_VARBITS.put("it-s-dark-in-here", SlayerVarbits.SLAYER_LONGER_BLACKDEMONS);
        UNLOCK_VARBITS.put("fire-and-darkness", SlayerVarbits.SLAYER_LONGER_BLACKDRAGONS);
        UNLOCK_VARBITS.put("bleed-me-dry", SlayerVarbits.SLAYER_LONGER_BLOODVELD);
        UNLOCK_VARBITS.put("horrorific", SlayerVarbits.SLAYER_LONGER_CAVEHORRORS);
        UNLOCK_VARBITS.put("krack-on", SlayerVarbits.SLAYER_LONGER_CAVEKRAKEN);
        UNLOCK_VARBITS.put("need-more-darkness", SlayerVarbits.SLAYER_LONGER_DARKBEASTS);
        UNLOCK_VARBITS.put("to-dust-you-shall-return", SlayerVarbits.SLAYER_LONGER_DUSTDEVILS);
        UNLOCK_VARBITS.put("wyver-nother-two", SlayerVarbits.SLAYER_LONGER_FOSSILWYVERNS);
        UNLOCK_VARBITS.put("i-see-dragons", SlayerVarbits.SLAYER_UNLOCK_LONGER_FROST_DRAGONS);
        UNLOCK_VARBITS.put("get-smashed", SlayerVarbits.SLAYER_LONGER_GARGOYLES);
        UNLOCK_VARBITS.put("greater-challenge", SlayerVarbits.SLAYER_LONGER_GREATERDEMONS);
        UNLOCK_VARBITS.put("gryphon-and-on", SlayerVarbits.SLAYER_UNLOCK_LONGER_GRYPHON);
        UNLOCK_VARBITS.put("pedal-to-the-metals", SlayerVarbits.SLAYER_LONGER_METALDRAGONS);
        UNLOCK_VARBITS.put("nechs-please", SlayerVarbits.SLAYER_LONGER_NECHRYAEL);
        UNLOCK_VARBITS.put("revenenenenenants", SlayerVarbits.SLAYER_LONGER_REVENANTS);
        UNLOCK_VARBITS.put("get-scabaright-on-it", SlayerVarbits.SLAYER_LONGER_SCABARITES);
        UNLOCK_VARBITS.put("un-restraining-order", SlayerVarbits.SLAYER_LONGER_CUSTODIANS);
        UNLOCK_VARBITS.put("wyver-nother-one", SlayerVarbits.SLAYER_LONGER_SKELETALWYVERNS);
        UNLOCK_VARBITS.put("spiritual-fervour", SlayerVarbits.SLAYER_LONGER_SPIRITUALGWD);
        UNLOCK_VARBITS.put("suq-a-nother-one", SlayerVarbits.SLAYER_LONGER_SUQAH);
        UNLOCK_VARBITS.put("more-at-stake", SlayerVarbits.SLAYER_LONGER_VAMPYRES);
        UNLOCK_VARBITS.put("can-of-wyrms", SlayerVarbits.SLAYER_LONGER_WYRMS);
    }

    private final Client client;
    private final ConfigSlayerUnlockStateProvider fallback;

    @Inject
    public VarbitSlayerUnlockStateProvider(Client client, ConfigSlayerUnlockStateProvider fallback)
    {
        this.client = client;
        this.fallback = fallback;
    }

    /**
     * Whether {@link #ownsUnlock} answers this id from a spike-verified varbit (rather than
     * delegating to the self-declared config state). Static curated knowledge - game state does
     * not change it. The WA-13 hint uses this to exclude EXTENSION candidates whose ownership
     * nothing can answer (FR-RV S2): an unanswerable candidate must never be recommended.
     */
    public static boolean answersUnlock(String rewardOrUnlockId)
    {
        return rewardOrUnlockId != null && UNLOCK_VARBITS.containsKey(rewardOrUnlockId);
    }

    @Override
    public boolean ownsUnlock(String rewardOrUnlockId)
    {
        Integer varbit = rewardOrUnlockId == null ? null : UNLOCK_VARBITS.get(rewardOrUnlockId);
        if (varbit == null || !inGame())
        {
            return fallback.ownsUnlock(rewardOrUnlockId);
        }
        return client.getVarbitValue(varbit) > 0;
    }

    @Override
    public OptionalInt rewardPoints()
    {
        if (!inGame())
        {
            return fallback.rewardPoints();
        }
        return OptionalInt.of(client.getVarbitValue(SlayerVarbits.SLAYER_POINTS));
    }

    @Override
    public Optional<String> currentMaster()
    {
        if (!inGame())
        {
            return fallback.currentMaster();
        }
        int value = client.getVarbitValue(SlayerVarbits.SLAYER_MASTER);
        if (value > 0 && value < MASTER_BY_VALUE.length)
        {
            return Optional.of(MASTER_BY_VALUE[value]);
        }
        return fallback.currentMaster();
    }

    private boolean inGame()
    {
        GameState state = client.getGameState();
        return state == GameState.LOGGED_IN || state == GameState.LOADING;
    }
}

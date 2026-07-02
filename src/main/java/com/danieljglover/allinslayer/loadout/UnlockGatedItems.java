package com.danieljglover.allinslayer.loadout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The curated item -&gt; Slayer-reward-unlock guard map (WA-12, ADR-0018 #9b). A recommended item
 * that the reward shop gates behind an unlock the player has not bought composes an NG-4 NOTE -
 * never a gear or combat-maths change (owning the item is still the recommendation signal; this
 * only tells the player which shop unlock the gear family needs).
 *
 * <p>Coverage is deliberately curated to the three gear families the audit named (FR-R2 section 2),
 * all of whose rewardIds are varbit-answered by the default provider (WA-10): every slayer helmet
 * (base, imbued, and recolours - the same pinned family {@link ConditionalBonusRegistry} credits) -&gt;
 * {@code malevolent-masquerade}; broad ammo -&gt; {@code broader-fletching}; slayer rings -&gt;
 * {@code ring-bling}. Ids are pinned from the 1.12.31.1 {@code net.runelite.api.gameval.ItemID}
 * constants (risk R4); the rewardIds are the {@code rewards/*.json} catalogue ids (WA-6).
 */
final class UnlockGatedItems
{
    /** rewardId -> the note fragment naming the unlock and the gear family it gates. */
    private static final Map<String, String> REWARD_LABELS = new HashMap<>();

    /** itemId -> the rewardId that gates its gear family. */
    private static final Map<Integer, String> ITEM_REWARDS = new HashMap<>();

    /**
     * The slayer-helmet family is SHARED with the DPS credit, not copied (FR-RV S1): composed
     * from {@link ConditionalBonusRegistry#slayerHelmetFamily()} so a future recolour added to
     * the registry is guarded here automatically - credit and guard cannot drift.
     */
    private static final int[] SLAYER_HELMETS = ConditionalBonusRegistry.slayerHelmetFamily();

    /** Broad arrows 4160, broad bolts 11875, amethyst broad bolts 21316. */
    private static final int[] BROAD_AMMO = {4160, 11875, 21316};

    /** Slayer ring (8)..(1) 11866..11873 + slayer ring (eternal) 21268. */
    private static final int[] SLAYER_RINGS = {11866, 11867, 11868, 11869, 11870, 11871, 11872, 11873, 21268};

    static
    {
        REWARD_LABELS.put("malevolent-masquerade", "Malevolent masquerade (slayer helmet)");
        REWARD_LABELS.put("broader-fletching", "Broader Fletching (broad ammo)");
        REWARD_LABELS.put("ring-bling", "Ring bling (slayer ring)");
        register(SLAYER_HELMETS, "malevolent-masquerade");
        register(BROAD_AMMO, "broader-fletching");
        register(SLAYER_RINGS, "ring-bling");
    }

    private UnlockGatedItems()
    {
    }

    private static void register(int[] itemIds, String rewardId)
    {
        for (int itemId : itemIds)
        {
            ITEM_REWARDS.put(itemId, rewardId);
        }
    }

    /**
     * The guard note for a loadout's item ids, or null when nothing is gated or every gating unlock
     * is owned. Distinct rewards join once each, in encounter order.
     */
    static String guardNote(Collection<Integer> itemIds, SlayerUnlockStateProvider unlockState)
    {
        if (itemIds == null || itemIds.isEmpty())
        {
            return null;
        }
        Set<String> unowned = new LinkedHashSet<>();
        for (Integer itemId : itemIds)
        {
            if (itemId == null)
            {
                continue;
            }
            String rewardId = ITEM_REWARDS.get(itemId);
            if (rewardId != null && !unlockState.ownsUnlock(rewardId))
            {
                unowned.add(rewardId);
            }
        }
        if (unowned.isEmpty())
        {
            return null;
        }
        List<String> labels = new ArrayList<>(unowned.size());
        for (String rewardId : unowned)
        {
            labels.add(REWARD_LABELS.get(rewardId));
        }
        String lead = unowned.size() == 1
            ? "Needs a Slayer unlock you haven't bought: "
            : "Needs Slayer unlocks you haven't bought: ";
        return lead + String.join(", ", labels) + ".";
    }
}

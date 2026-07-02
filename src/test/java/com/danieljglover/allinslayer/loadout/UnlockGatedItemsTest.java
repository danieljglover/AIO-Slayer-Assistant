package com.danieljglover.allinslayer.loadout;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WA-12 (ADR-0018 #9b): the curated unlock-gated gear guard. A recommended item whose reward-shop
 * unlock the player has not bought composes an NG-4 note - never a gear or combat-maths change.
 * Ids are pinned from the 1.12.31.1 {@code net.runelite.api.gameval.ItemID} constants (risk R4).
 */
public class UnlockGatedItemsTest
{
    private static final int SLAYER_HELMET = 11864;
    private static final int SLAYER_HELMET_I = 11865;
    private static final int SLAYER_HELMET_BLACK = 19639;
    private static final int BROAD_BOLTS = 11875;
    private static final int AMETHYST_BROAD_BOLTS = 21316;
    private static final int BROAD_ARROWS = 4160;
    private static final int SLAYER_RING_8 = 11866;
    private static final int SLAYER_RING_ETERNAL = 21268;
    private static final int ABYSSAL_WHIP = 4151;

    /** A seam fake owning exactly the given rewardIds. */
    private static SlayerUnlockStateProvider owning(String... rewardIds)
    {
        Set<String> owned = new HashSet<>(Arrays.asList(rewardIds));
        return new SlayerUnlockStateProvider()
        {
            @Override
            public boolean ownsUnlock(String rewardOrUnlockId)
            {
                return owned.contains(rewardOrUnlockId);
            }

            @Override
            public OptionalInt rewardPoints()
            {
                return OptionalInt.empty();
            }

            @Override
            public Optional<String> currentMaster()
            {
                return Optional.empty();
            }
        };
    }

    @Test
    public void slayerHelmWithoutMalevolentMasqueradeComposesTheGuardNote()
    {
        String note = UnlockGatedItems.guardNote(
            Collections.singletonList(SLAYER_HELMET), owning());
        assertEquals("Needs a Slayer unlock you haven't bought: "
            + "Malevolent masquerade (slayer helmet).", note);
    }

    @Test
    public void ownedUnlockMeansNoNote()
    {
        assertNull("owning the unlock silences the guard (NG-4 note-only, never nags the unlocked)",
            UnlockGatedItems.guardNote(
                Collections.singletonList(SLAYER_HELMET), owning("malevolent-masquerade")));
    }

    @Test
    public void imbuedAndRecolouredHelmsAreGatedByTheSameReward()
    {
        assertTrue(UnlockGatedItems.guardNote(
            Collections.singletonList(SLAYER_HELMET_I), owning()).contains("Malevolent masquerade"));
        assertTrue(UnlockGatedItems.guardNote(
            Collections.singletonList(SLAYER_HELMET_BLACK), owning())
            .contains("Malevolent masquerade"));
    }

    @Test
    public void broadAmmoIsGatedByBroaderFletching()
    {
        for (int itemId : new int[]{BROAD_BOLTS, AMETHYST_BROAD_BOLTS, BROAD_ARROWS})
        {
            String note = UnlockGatedItems.guardNote(
                Collections.singletonList(itemId), owning());
            assertTrue("broad ammo id " + itemId + " maps to Broader Fletching",
                note.contains("Broader Fletching"));
        }
        assertNull(UnlockGatedItems.guardNote(
            Collections.singletonList(BROAD_BOLTS), owning("broader-fletching")));
    }

    @Test
    public void slayerRingsAreGatedByRingBling()
    {
        for (int itemId : new int[]{SLAYER_RING_8, SLAYER_RING_ETERNAL})
        {
            String note = UnlockGatedItems.guardNote(
                Collections.singletonList(itemId), owning());
            assertTrue("slayer ring id " + itemId + " maps to Ring bling",
                note.contains("Ring bling"));
        }
        assertNull(UnlockGatedItems.guardNote(
            Collections.singletonList(SLAYER_RING_8), owning("ring-bling")));
    }

    @Test
    public void guardMapCoversExactlyTheRegistrySlayerHelmFamily()
    {
        // FR-RV S1: the guard map must stay in lockstep with the family the DPS credit uses -
        // a recolour added to ConditionalBonusRegistry but not guarded here would be credited
        // in DPS with no WA-12 note. The map is COMPOSED from the registry's family, and this
        // pins the coupling end-to-end through the public guardNote seam.
        int[] family = ConditionalBonusRegistry.slayerHelmetFamily();
        assertEquals("base (15) + imbued (45) helm family size (guards vacuity)",
            60, family.length);
        for (int itemId : family)
        {
            String note = UnlockGatedItems.guardNote(
                Collections.singletonList(itemId), owning());
            assertTrue("registry helm-family id " + itemId + " must compose the "
                + "malevolent-masquerade guard note",
                note != null && note.contains("Malevolent masquerade"));
        }
    }

    @Test
    public void multipleGatedRewardsComposeOneDedupedNote()
    {
        String note = UnlockGatedItems.guardNote(
            Arrays.asList(SLAYER_HELMET, SLAYER_HELMET_I, BROAD_BOLTS), owning());
        assertEquals("distinct rewards join once each, plural copy",
            "Needs Slayer unlocks you haven't bought: Malevolent masquerade (slayer helmet), "
                + "Broader Fletching (broad ammo).", note);
    }

    @Test
    public void ungatedItemsAndEmptyInputsYieldNull()
    {
        assertNull(UnlockGatedItems.guardNote(Collections.singletonList(ABYSSAL_WHIP), owning()));
        assertNull(UnlockGatedItems.guardNote(Collections.emptyList(), owning()));
        assertNull(UnlockGatedItems.guardNote(null, owning()));
    }
}

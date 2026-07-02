package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AllInSlayerConfig;
import com.danieljglover.allinslayer.PrimaryMaster;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;

/**
 * WA-10 (ADR-0018 #7, spike-gated by PD-A - the WA-0 spike found verified ids, so this varbit
 * reader becomes the DEFAULT binding). Reads: points varbit 4068, current-master varbit 4067
 * (value map 1=turael .. 9=spria; 7=Krystilia verified from the stock plugin's own bytecode),
 * per-unlock varbits from the Jagex gameval names (spike doc table). Delegates to the config
 * provider when logged out, when the master varbit is 0 (no task), or for an id it has no
 * verified varbit for - so the self-declared state remains the honest fallback.
 */
public class VarbitSlayerUnlockStateProviderTest
{
    private Client client;
    private VarbitSlayerUnlockStateProvider provider;

    private static class DefaultsConfig implements AllInSlayerConfig
    {
    }

    @Before
    public void setUp()
    {
        client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        provider = new VarbitSlayerUnlockStateProvider(client,
            new ConfigSlayerUnlockStateProvider(new DefaultsConfig()));
    }

    @Test
    public void rewardPointsReadTheSlayerPointsVarbit()
    {
        when(client.getVarbitValue(4068)).thenReturn(734);
        assertEquals(734, provider.rewardPoints().getAsInt());
    }

    @Test
    public void ownsUnlockReadsTheMappedGlobalRewardVarbits()
    {
        // The spike's global-rewards table: rewardId -> varbit (Jagex gameval names).
        when(client.getVarbitValue(3202)).thenReturn(1);  // SLAYER_HELM_UNLOCKED
        when(client.getVarbitValue(3208)).thenReturn(0);  // SLAYER_AMMO_UNLOCKED
        when(client.getVarbitValue(3207)).thenReturn(1);  // SLAYER_RING_UNLOCKED
        when(client.getVarbitValue(4724)).thenReturn(0);  // SLAYER_UNLOCK_BOSSES
        when(client.getVarbitValue(5358)).thenReturn(1);  // SLAYER_UNLOCK_SUPERIORMOBS
        when(client.getVarbitValue(12442)).thenReturn(0); // SLAYER_UNLOCK_STORAGE
        when(client.getVarbitValue(13636)).thenReturn(1); // SLAYER_UNLOCK_WILDY_EXTRATASKS

        assertTrue(provider.ownsUnlock("malevolent-masquerade"));
        assertFalse(provider.ownsUnlock("broader-fletching"));
        assertTrue(provider.ownsUnlock("ring-bling"));
        assertFalse(provider.ownsUnlock("like-a-boss"));
        assertTrue(provider.ownsUnlock("bigger-and-badder"));
        assertFalse(provider.ownsUnlock("task-storage"));
        assertTrue(provider.ownsUnlock("i-wildy-more-slayer"));
    }

    @Test
    public void ownsUnlockReadsTheVerifiedRecolourVarbits()
    {
        when(client.getVarbitValue(5080)).thenReturn(1);  // SLAYER_UNLOCK_HELM_BLACK
        when(client.getVarbitValue(10104)).thenReturn(0); // SLAYER_UNLOCK_HELM_TWISTED
        assertTrue(provider.ownsUnlock("king-black-bonnet"));
        assertFalse(provider.ownsUnlock("twisted-vision"));
    }

    @Test
    public void currentMasterMapsTheMasterVarbitValues()
    {
        // Spike value map: 1..9 in release order (7=Krystilia pinned by the stock plugin's own
        // "wilderness streak" branch; 1/3/4 corroborated by gameval NpcID names).
        String[] expected = {null, "turael", "mazchna", "vannaka", "chaeldar", "duradel", "nieve",
            "krystilia", "konar", "spria"};
        for (int value = 1; value <= 9; value++)
        {
            when(client.getVarbitValue(4067)).thenReturn(value);
            assertEquals("varbit value " + value, expected[value],
                provider.currentMaster().get());
        }
    }

    @Test
    public void masterVarbitZeroOrUnknownFallsBackToTheConfigPrimary()
    {
        VarbitSlayerUnlockStateProvider p = new VarbitSlayerUnlockStateProvider(client,
            new ConfigSlayerUnlockStateProvider(new DefaultsConfig()
            {
                @Override
                public PrimaryMaster primaryMaster()
                {
                    return PrimaryMaster.KONAR;
                }
            }));
        when(client.getVarbitValue(4067)).thenReturn(0); // no task assigned
        assertEquals("konar", p.currentMaster().get());

        when(client.getVarbitValue(4067)).thenReturn(42); // future/unknown master value
        assertEquals("an unknown value degrades to the config primary, never a guess",
            "konar", p.currentMaster().get());
    }

    @Test
    public void loggedOutDelegatesEverythingToTheConfigProvider()
    {
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        VarbitSlayerUnlockStateProvider p = new VarbitSlayerUnlockStateProvider(client,
            new ConfigSlayerUnlockStateProvider(new DefaultsConfig()
            {
                @Override
                public int slayerRewardPoints()
                {
                    return 250;
                }

                @Override
                public boolean ownsRingBling()
                {
                    return true;
                }

                @Override
                public PrimaryMaster primaryMaster()
                {
                    return PrimaryMaster.NIEVE;
                }
            }));

        assertEquals("logged out -> the self-declared balance", 250, p.rewardPoints().getAsInt());
        assertTrue("logged out -> the self-declared toggle", p.ownsUnlock("ring-bling"));
        assertEquals("logged out -> the config primary", "nieve", p.currentMaster().get());
        verify(client, never()).getVarbitValue(Mockito.anyInt());
    }

    @Test
    public void ownsUnlockReadsTheSpikeVerifiedExtensionVarbits()
    {
        // FR-RV S2: the WA-0 spike verified the SLAYER_LONGER_* family (spike doc, Signal 2), so
        // every EXTENSION unlockId in the dataset reads its own varbit - an owned extension no
        // longer reads unowned forever. One row per mapping; raw varbit ids repeated here on
        // purpose (a transcription guard against the SlayerVarbits constants), gameval names in
        // the comments. Each row proves its OWN pairing: only that varbit is non-zero when read.
        Object[][] rows = {
            {"smell-ya-later", 4747},           // SLAYER_LONGER_ABERRANTSPECTRES
            {"augment-my-abbies", 4090},        // SLAYER_LONGER_ABYSSALDEMONS
            {"ankou-very-much", 4085},          // SLAYER_LONGER_ANKOU
            {"lets-stay-all-aquanite", 19603},  // SLAYER_LONGER_AQUANITES
            {"more-eyes-than-sense", 11022},    // SLAYER_LONGER_ARAXYTES
            {"birds-of-a-feather", 4748},       // SLAYER_LONGER_AVIANSIES
            {"basilonger", 9455},               // SLAYER_LONGER_BASILISK
            {"it-s-dark-in-here", 4091},        // SLAYER_LONGER_BLACKDEMONS
            {"fire-and-darkness", 4087},        // SLAYER_LONGER_BLACKDRAGONS
            {"bleed-me-dry", 4746},             // SLAYER_LONGER_BLOODVELD
            {"horrorific", 4750},               // SLAYER_LONGER_CAVEHORRORS
            {"krack-on", 4755},                 // SLAYER_LONGER_CAVEKRAKEN
            {"need-more-darkness", 4031},       // SLAYER_LONGER_DARKBEASTS
            {"to-dust-you-shall-return", 4751}, // SLAYER_LONGER_DUSTDEVILS
            {"wyver-nother-two", 5733},         // SLAYER_LONGER_FOSSILWYVERNS
            {"i-see-dragons", 15399},           // SLAYER_UNLOCK_LONGER_FROST_DRAGONS
            {"get-smashed", 4753},              // SLAYER_LONGER_GARGOYLES
            {"greater-challenge", 4092},        // SLAYER_LONGER_GREATERDEMONS
            {"gryphon-and-on", 15398},          // SLAYER_UNLOCK_LONGER_GRYPHON
            {"pedal-to-the-metals", 4088},      // SLAYER_LONGER_METALDRAGONS
            {"nechs-please", 4754},             // SLAYER_LONGER_NECHRYAEL
            {"un-restraining-order", 17219},    // SLAYER_LONGER_CUSTODIANS
            {"wyver-nother-one", 4752},         // SLAYER_LONGER_SKELETALWYVERNS
            {"spiritual-fervour", 4757},        // SLAYER_LONGER_SPIRITUALGWD
            {"suq-a-nother-one", 4086},         // SLAYER_LONGER_SUQAH
            {"more-at-stake", 10389},           // SLAYER_LONGER_VAMPYRES
            {"can-of-wyrms", 19602},            // SLAYER_LONGER_WYRMS
        };
        assertEquals("all 27 dataset extension unlocks are varbit-answered", 27, rows.length);
        for (Object[] row : rows)
        {
            String unlockId = (String) row[0];
            int varbit = (Integer) row[1];
            when(client.getVarbitValue(varbit)).thenReturn(1);
            assertTrue(unlockId + " must read varbit " + varbit,
                provider.ownsUnlock(unlockId));
            when(client.getVarbitValue(varbit)).thenReturn(0);
            assertFalse(unlockId + " unowned when varbit " + varbit + " is 0",
                provider.ownsUnlock(unlockId));
        }
    }

    @Test
    public void unmappedUnlockIdsDelegateToConfigInsteadOfGuessing()
    {
        // The 3 recolours whose name->colour pairing the spike could not verify, and any id with
        // no verified varbit, carry no row here - they fall through to config (which reads
        // unowned). Never a guess.
        assertFalse(provider.ownsUnlock("eye-see-you"));
        assertFalse(provider.ownsUnlock("absolutely-slayin"));
        assertFalse(provider.ownsUnlock("oath-breaker"));
        assertFalse(provider.ownsUnlock("some-unverified-extension"));
        assertFalse(provider.ownsUnlock(null));
        verify(client, never()).getVarbitValue(Mockito.anyInt());
    }

    @Test
    public void answersUnlockIsTheVerifiedVarbitInventory()
    {
        // The static answerability query the WA-13 hint filters EXTENSION candidates on.
        assertTrue(VarbitSlayerUnlockStateProvider.answersUnlock("malevolent-masquerade"));
        assertTrue(VarbitSlayerUnlockStateProvider.answersUnlock("augment-my-abbies"));
        assertFalse(VarbitSlayerUnlockStateProvider.answersUnlock("eye-see-you"));
        assertFalse(VarbitSlayerUnlockStateProvider.answersUnlock("some-unverified-extension"));
        assertFalse(VarbitSlayerUnlockStateProvider.answersUnlock(null));
    }
}

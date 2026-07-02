package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.runelite.api.ItemID;
import org.junit.Test;

/**
 * LFB-3 / FR-12.5: the curated, data-driven conditional-bonus registry (ADR-0007). Keyed by raw item
 * id because {@code ItemVariationMapping.map()} collapses imbued and non-imbued variants to the SAME
 * canonical (verified: every black mask -> 8901, every slayer helmet -> 11864, every salve -> 4081),
 * which would erase the imbued ranged/magic uplift and the salve enchant/imbue distinctions that
 * carry different multipliers. Adding a source is one data row + one test row, no selector change.
 */
public class ConditionalBonusRegistryTest
{
    private static final double EPS = 1e-9;

    @Test
    public void slayerHelmetImbuedIsOnSlayerTaskAllStyles()
    {
        ConditionalBonus b = ConditionalBonusRegistry.lookup(ItemID.SLAYER_HELMET_I);
        assertNotNull(b);
        assertEquals(BonusCondition.ON_SLAYER_TASK, b.getCondition());
        assertEquals(1.1667, b.getMeleeDmg(), EPS);
        assertEquals(1.15, b.getRangedDmg(), EPS);
        assertEquals(1.15, b.getMagicDmg(), EPS);
    }

    @Test
    public void blackMaskBaseIsOnSlayerTaskMeleeOnly()
    {
        ConditionalBonus b = ConditionalBonusRegistry.lookup(ItemID.BLACK_MASK);
        assertNotNull(b);
        assertEquals(BonusCondition.ON_SLAYER_TASK, b.getCondition());
        assertEquals(1.1667, b.getMeleeDmg(), EPS);
        assertEquals("non-imbued mask gives no ranged bonus", 1.0, b.getRangedDmg(), EPS);
        assertEquals("non-imbued mask gives no magic bonus", 1.0, b.getMagicDmg(), EPS);
    }

    @Test
    public void slayerHelmetBaseIsMeleeOnly()
    {
        // Brief correction: a plain Slayer helmet contains a black mask and DOES give the +16.67%
        // melee bonus (only the imbued form adds ranged/magic).
        ConditionalBonus b = ConditionalBonusRegistry.lookup(ItemID.SLAYER_HELMET);
        assertNotNull(b);
        assertEquals(1.1667, b.getMeleeDmg(), EPS);
        assertEquals(1.0, b.getRangedDmg(), EPS);
        assertEquals(1.0, b.getMagicDmg(), EPS);
    }

    @Test
    public void salveVariantsCarryTheirOwnMultipliers()
    {
        ConditionalBonus base = ConditionalBonusRegistry.lookup(ItemID.SALVE_AMULET);
        assertEquals(BonusCondition.VS_UNDEAD, base.getCondition());
        assertEquals(1.1667, base.getMeleeDmg(), EPS);
        assertEquals(1.0, base.getRangedDmg(), EPS);

        // Enchanted = +20% melee (brief said 1.1667 - wrong).
        ConditionalBonus enchanted = ConditionalBonusRegistry.lookup(ItemID.SALVE_AMULET_E);
        assertEquals(1.20, enchanted.getMeleeDmg(), EPS);
        assertEquals(1.0, enchanted.getRangedDmg(), EPS);

        // Imbued-but-unenchanted = +16.67% melee, +15% ranged/magic.
        ConditionalBonus imbued = ConditionalBonusRegistry.lookup(ItemID.SALVE_AMULETI);
        assertEquals(1.1667, imbued.getMeleeDmg(), EPS);
        assertEquals(1.15, imbued.getRangedDmg(), EPS);
        assertEquals(1.15, imbued.getMagicDmg(), EPS);

        // Enchanted + imbued = +20% all styles.
        ConditionalBonus both = ConditionalBonusRegistry.lookup(ItemID.SALVE_AMULETEI);
        assertEquals(1.20, both.getMeleeDmg(), EPS);
        assertEquals(1.20, both.getRangedDmg(), EPS);
        assertEquals(1.20, both.getMagicDmg(), EPS);
    }

    @Test
    public void dragonHunterLanceIsSymmetricMeleeVsDragon()
    {
        // WDB-7 (ADR-0008): DHL +20% accuracy AND damage vs draconic - melee only, symmetric.
        ConditionalBonus dhl = ConditionalBonusRegistry.lookup(22978); // DRAGON_HUNTER_LANCE
        assertNotNull(dhl);
        assertEquals(BonusCondition.VS_DRAGON, dhl.getCondition());
        assertEquals(1.20, dhl.getMeleeDmg(), EPS);
        assertEquals("dragonbane lance gives no ranged bonus", 1.0, dhl.getRangedDmg(), EPS);
        assertEquals("dragonbane lance gives no magic bonus", 1.0, dhl.getMagicDmg(), EPS);
    }

    @Test
    public void demonbaneWeaponsAreSymmetricVsDemon()
    {
        // WDB-10 (ADR-0009 A.2): demonbane weapons - symmetric acc/dmg vs demons.
        ConditionalBonus arclight = ConditionalBonusRegistry.lookup(ItemID.ARCLIGHT);
        assertEquals(BonusCondition.VS_DEMON, arclight.getCondition());
        assertEquals(1.70, arclight.getMeleeDmg(), EPS);
        assertEquals(1.70, arclight.getMeleeAcc(), EPS);

        assertEquals(1.70, ConditionalBonusRegistry.lookup(ItemID.EMBERLIGHT).getMeleeDmg(), EPS);
        assertEquals(1.60, ConditionalBonusRegistry.lookup(ItemID.DARKLIGHT).getMeleeDmg(), EPS);

        // Scorching bow: ranged-only +30% (melee/magic 1.0).
        ConditionalBonus bow = ConditionalBonusRegistry.lookup(ItemID.SCORCHING_BOW);
        assertEquals(1.30, bow.getRangedDmg(), EPS);
        assertEquals("ranged demonbane, no melee bonus", 1.0, bow.getMeleeDmg(), EPS);

        // Silverlight is excluded (no verified %, DEC-7); the inactive Arclight has no bonus.
        assertNull("Silverlight excluded", ConditionalBonusRegistry.lookup(ItemID.SILVERLIGHT));
        assertNull("inactive Arclight has no demonbane", ConditionalBonusRegistry.lookup(30305));
    }

    @Test
    public void dragonHunterCrossbowIsAsymmetricRangedVsDragon()
    {
        // WB-1 (FR-C2 5.2): DHCB +30% ranged ACCURACY / +25% ranged DAMAGE vs draconic - wiki-
        // verified 2026-07-01 ("30% increase in ranged accuracy and 25% increase in damage").
        // Asymmetric, so weapon-slot only (Keris precedent). One row per id incl. recolours.
        for (int id : new int[] {ItemID.DRAGON_HUNTER_CROSSBOW, ItemID.DRAGON_HUNTER_CROSSBOW_T,
            ItemID.DRAGON_HUNTER_CROSSBOW_B})
        {
            ConditionalBonus b = ConditionalBonusRegistry.lookup(id);
            assertNotNull("id " + id, b);
            assertEquals(BonusCondition.VS_DRAGON, b.getCondition());
            assertEquals("ranged accuracy +30%", 1.30, b.getRangedAcc(), EPS);
            assertEquals("ranged damage +25%", 1.25, b.getRangedDmg(), EPS);
            assertEquals("no melee bonus", 1.0, b.getMeleeAcc(), EPS);
            assertEquals(1.0, b.getMeleeDmg(), EPS);
            assertEquals("no magic bonus", 1.0, b.getMagicAcc(), EPS);
            assertEquals(1.0, b.getMagicDmg(), EPS);
        }
    }

    @Test
    public void dragonHunterWandIsAsymmetricMagicVsDragon()
    {
        // WB-1: DH wand +75% magic ACCURACY / +40% magic DAMAGE vs draconic - wiki-verified
        // 2026-07-01 ("accuracy is increased by 75% and damage increased by 40%"; buffed from
        // 50%/20% in June 2025, so the audit's numbers are the CURRENT ones).
        ConditionalBonus b = ConditionalBonusRegistry.lookup(ItemID.DRAGON_HUNTER_WAND);
        assertNotNull(b);
        assertEquals(BonusCondition.VS_DRAGON, b.getCondition());
        assertEquals("magic accuracy +75%", 1.75, b.getMagicAcc(), EPS);
        assertEquals("magic damage +40%", 1.40, b.getMagicDmg(), EPS);
        assertEquals("no melee bonus", 1.0, b.getMeleeAcc(), EPS);
        assertEquals(1.0, b.getMeleeDmg(), EPS);
        assertEquals("no ranged bonus", 1.0, b.getRangedAcc(), EPS);
        assertEquals(1.0, b.getRangedDmg(), EPS);
    }

    @Test
    public void kerisIsAsymmetricDamageOnlyVsKalphite()
    {
        // WDB-12 (ADR-0009 A.3): Keris = damage-only +38.2% (EV incl. 1/51 triple proc), no accuracy.
        ConditionalBonus keris = ConditionalBonusRegistry.lookup(ItemID.KERIS);
        assertNotNull(keris);
        assertEquals(BonusCondition.VS_KALPHITE, keris.getCondition());
        assertEquals("no accuracy bonus", 1.0, keris.getMeleeAcc(), EPS);
        assertEquals("damage +38.2% (1.33 * 53/51)", 1.382, keris.getMeleeDmg(), EPS);
        // The whole family shares +33% (partisans too).
        assertEquals(1.382, ConditionalBonusRegistry.lookup(ItemID.KERIS_PARTISAN).getMeleeDmg(), EPS);
        assertEquals(1.382, ConditionalBonusRegistry.lookup(ItemID.KERIS_PARTISAN_OF_THE_SUN)
            .getMeleeDmg(), EPS);
    }

    @Test
    public void wildernessWeaponsAreSymmetricVsWilderness()
    {
        // WDB-17 (ADR-0009 A.5): Wilderness weapons - symmetric +50%, one per style.
        ConditionalBonus viggora = ConditionalBonusRegistry.lookup(ItemID.VIGGORAS_CHAINMACE);
        assertEquals(BonusCondition.VS_WILDERNESS, viggora.getCondition());
        assertEquals(1.50, viggora.getMeleeDmg(), EPS);
        assertEquals(1.50, ConditionalBonusRegistry.lookup(ItemID.CRAWS_BOW).getRangedDmg(), EPS);
        assertEquals(1.50, ConditionalBonusRegistry.lookup(ItemID.WEBWEAVER_BOW).getRangedDmg(), EPS);
        assertEquals(1.50, ConditionalBonusRegistry.lookup(ItemID.THAMMARONS_SCEPTRE).getMagicDmg(), EPS);
        assertEquals("Viggora's gives no ranged bonus", 1.0, viggora.getRangedDmg(), EPS);
    }

    @Test
    public void asymmetricBonusesLiveOnlyOnWeaponSlotItems()
    {
        // WDB-11 invariant (ADR-0009 A.1): the non-weapon additive (m-1)*L term uses a single
        // symmetric multiplier, so an asymmetric bonus may live ONLY on a weapon-slot item. Every
        // asymmetric registry entry must therefore be a known weapon (the Keris family); all other
        // entries must be symmetric (acc == dmg).
        java.util.Set<Integer> weaponAsymmetricAllowlist = new java.util.HashSet<>(java.util.Arrays.asList(
            10581, 10582, 10583, 10584,        // Keris + Keris (p) variants
            25979, 25981, 27287, 27291, 30891, // Keris partisans
            21012, 25916, 25918,               // Dragon hunter crossbow + (t)/(b) recolours (WB-1)
            30070                              // Dragon hunter wand (WB-1)
        ));
        for (java.util.Map.Entry<Integer, ConditionalBonus> e : ConditionalBonusRegistry.all().entrySet())
        {
            if (!e.getValue().isSymmetric())
            {
                assertTrue("asymmetric bonus on non-weapon item id " + e.getKey(),
                    weaponAsymmetricAllowlist.contains(e.getKey()));
            }
        }
    }

    @Test
    public void unknownItemHasNoBonus()
    {
        assertNull("Abyssal whip is not a conditional source", ConditionalBonusRegistry.lookup(4151));
        assertNull(ConditionalBonusRegistry.lookup(-1));
        assertNull(ConditionalBonusRegistry.lookup(0));
    }

    @Test
    public void everyChargeStateOfABlackMaskResolvesToTheSameEntry()
    {
        // The selector sees whatever charge state the player owns; every charge must score identically.
        ConditionalBonus canonical = ConditionalBonusRegistry.lookup(ItemID.BLACK_MASK);
        assertEquals(canonical, ConditionalBonusRegistry.lookup(ItemID.BLACK_MASK_1));
        assertEquals(canonical, ConditionalBonusRegistry.lookup(ItemID.BLACK_MASK_10));
    }

    @Test
    public void recolouredAndImbuedHelmetVariantsAreCovered()
    {
        // A common case: an imbued recoloured helm (e.g. Hydra slayer helmet (i)) must get the full
        // imbued bonus, and its non-imbued recolour the melee-only bonus - the bug would otherwise
        // recur for every recolour. Keyed explicitly because the canonical collapse loses imbue state.
        ConditionalBonus hydraImbued = ConditionalBonusRegistry.lookup(ItemID.HYDRA_SLAYER_HELMET_I);
        assertNotNull(hydraImbued);
        assertEquals(1.15, hydraImbued.getRangedDmg(), EPS);

        ConditionalBonus hydraBase = ConditionalBonusRegistry.lookup(ItemID.HYDRA_SLAYER_HELMET);
        assertNotNull(hydraBase);
        assertEquals(1.0, hydraBase.getRangedDmg(), EPS);
    }
}

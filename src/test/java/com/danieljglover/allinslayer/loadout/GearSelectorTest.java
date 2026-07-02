package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.Weakness;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * LD06: the gear selector implements plan section 4 over owned items using a fake
 * {@link EquipmentStatsProvider} (canned {@link Bonuses}) and a fake {@link PriceService}. No live
 * {@code ItemManager} is touched. Each test fixes the weakness style + monster defence and asserts
 * the {@code worn} map the selector produces.
 */
public class GearSelectorTest
{
    private static final PlayerStats MAXED = new PlayerStats(99, 99, 99, 99, 99, 99);
    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    // --- per-slot scoring ----------------------------------------------------------------------

    @Test
    public void picksTheHigherScoreOwnedItemInASlot()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10)) // weapon, score 60
            .put(200, melee(EquipmentSlot.HEAD, false, 10, 0, 0, 5))    // head A, score 15
            .put(201, melee(EquipmentSlot.HEAD, false, 20, 0, 0, 2));   // head B, score 22

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(100, 200, 201), AdviceMode.DPS);

        assertEquals(Integer.valueOf(100), worn.get(EquipmentSlot.WEAPON));
        assertEquals("higher-score head wins", Integer.valueOf(201), worn.get(EquipmentSlot.HEAD));
    }

    @Test
    public void neverPicksAnUnownedItem()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10))
            .put(200, melee(EquipmentSlot.HEAD, false, 10, 0, 0, 5))   // owned
            .put(202, melee(EquipmentSlot.HEAD, false, 99, 0, 0, 99)); // far better but NOT owned

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(100, 200), AdviceMode.DPS);

        assertEquals(Integer.valueOf(200), worn.get(EquipmentSlot.HEAD));
    }

    @Test
    public void skipsItemsWithNullStatsWithoutFailing()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10));
        // id 999 is owned but the provider returns null for it (async-not-loaded / non-equipable).

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(100, 999), AdviceMode.DPS);

        assertEquals(Integer.valueOf(100), worn.get(EquipmentSlot.WEAPON));
        assertFalse(worn.containsKey(EquipmentSlot.HEAD));
    }

    @Test
    public void itemWithNoPositiveScoreDoesNotFillItsSlot()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10))
            .put(200, melee(EquipmentSlot.HEAD, false, 0, 0, 0, 0)); // score 0 -> not used

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(100, 200), AdviceMode.DPS);

        assertFalse(worn.containsKey(EquipmentSlot.HEAD));
    }

    // --- melee attack type (section 4.2) -------------------------------------------------------

    @Test
    public void scoresMeleeByTheMonstersLowestDefenceType()
    {
        // crush is lowest -> the selector must score by crush attack, not stab.
        FakeStats stats = new FakeStats()
            .put(300, melee(EquipmentSlot.WEAPON, false, 0, 0, 40, 0))    // acrush 40, score(CRUSH)=40
            .put(301, melee(EquipmentSlot.WEAPON, false, 100, 0, 10, 0)); // big astab, tiny acrush

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(50, 50, 0), owned(300, 301), AdviceMode.DPS);

        assertEquals("crush weapon wins because crush is the weakness", Integer.valueOf(300),
            worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void tieBreaksLowestDefenceByBestOwnedWeaponAttackBonus()
    {
        // stab and slash both lowest (0); the type with the higher best-owned-weapon attack wins.
        FakeStats stats = new FakeStats()
            .put(401, melee(EquipmentSlot.WEAPON, false, 30, 0, 0, 0))  // stab 30
            .put(402, melee(EquipmentSlot.WEAPON, false, 0, 80, 0, 0)); // slash 80 -> slash wins

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 0, 99), owned(401, 402), AdviceMode.DPS);

        assertEquals("slash chosen via tie-break, so the slash weapon wins", Integer.valueOf(402),
            worn.get(EquipmentSlot.WEAPON));
    }

    // --- weapon / shield interplay (section 4.6) -----------------------------------------------

    @Test
    public void twoHandedWinsWhenItsScoreIsHigherThanOneHandPlusShield()
    {
        FakeStats stats = new FakeStats()
            .put(500, melee(EquipmentSlot.WEAPON, true, 100, 0, 0, 0))  // 2h, score 100
            .put(501, melee(EquipmentSlot.WEAPON, false, 40, 0, 0, 0))  // 1h, score 40
            .put(502, melee(EquipmentSlot.SHIELD, false, 30, 0, 0, 0)); // shield, score 30

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(500, 501, 502), AdviceMode.DPS);

        assertEquals(Integer.valueOf(500), worn.get(EquipmentSlot.WEAPON));
        assertFalse("2h leaves the shield slot empty", worn.containsKey(EquipmentSlot.SHIELD));
    }

    @Test
    public void oneHandPlusShieldWinsWhenCombinedScoreIsHigher()
    {
        FakeStats stats = new FakeStats()
            .put(510, melee(EquipmentSlot.WEAPON, true, 60, 0, 0, 0))   // 2h, score 60
            .put(511, melee(EquipmentSlot.WEAPON, false, 40, 0, 0, 0))  // 1h, score 40
            .put(512, melee(EquipmentSlot.SHIELD, false, 30, 0, 0, 0)); // shield, score 30 -> 70 combined

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(510, 511, 512), AdviceMode.DPS);

        assertEquals(Integer.valueOf(511), worn.get(EquipmentSlot.WEAPON));
        assertEquals(Integer.valueOf(512), worn.get(EquipmentSlot.SHIELD));
    }

    @Test
    public void fallsBackToTheOnlyWeaponTypeOwned()
    {
        // only a 2h owned -> use it, no shield.
        FakeStats only2h = new FakeStats()
            .put(520, melee(EquipmentSlot.WEAPON, true, 50, 0, 0, 0))
            .put(521, melee(EquipmentSlot.SHIELD, false, 30, 0, 0, 0));
        Map<EquipmentSlot, Integer> worn2h = select(only2h, new FakePrice(),
            meleeTask(0, 10, 10), owned(520, 521), AdviceMode.DPS);
        assertEquals(Integer.valueOf(520), worn2h.get(EquipmentSlot.WEAPON));
        assertFalse(worn2h.containsKey(EquipmentSlot.SHIELD));

        // only a 1h (+ shield) owned -> 1h + shield.
        FakeStats only1h = new FakeStats()
            .put(531, melee(EquipmentSlot.WEAPON, false, 40, 0, 0, 0))
            .put(532, melee(EquipmentSlot.SHIELD, false, 30, 0, 0, 0));
        Map<EquipmentSlot, Integer> worn1h = select(only1h, new FakePrice(),
            meleeTask(0, 10, 10), owned(531, 532), AdviceMode.DPS);
        assertEquals(Integer.valueOf(531), worn1h.get(EquipmentSlot.WEAPON));
        assertEquals(Integer.valueOf(532), worn1h.get(EquipmentSlot.SHIELD));
    }

    @Test
    public void noWeaponForTheStyleYieldsEmptyWorn()
    {
        // owns armour but no weapon at all -> not viable -> empty.
        FakeStats stats = new FakeStats()
            .put(800, melee(EquipmentSlot.HEAD, false, 50, 0, 0, 10))
            .put(801, melee(EquipmentSlot.BODY, false, 40, 0, 0, 10));

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(800, 801), AdviceMode.DPS);

        assertTrue("no weapon -> empty worn (TASK_WITHOUT_LOADOUT)", worn.isEmpty());
    }

    @Test
    public void noWeaknessStyleYieldsEmptyWorn()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10));
        TaskData task = new TaskData(); // no weakness

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            task, owned(100), AdviceMode.DPS);

        assertTrue(worn.isEmpty());
    }

    // --- DPS tie-break (section 4.5) -----------------------------------------------------------

    @Test
    public void dpsTieBreaksBySecondaryStatThenId()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10))
            .put(900, melee(EquipmentSlot.HEAD, false, 10, 0, 0, 10))  // score 20, str 10
            .put(901, melee(EquipmentSlot.HEAD, false, 15, 0, 0, 5));  // score 20, str 5

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(100, 900, 901), AdviceMode.DPS);

        assertEquals("equal score -> higher melee strength wins", Integer.valueOf(900),
            worn.get(EquipmentSlot.HEAD));
    }

    // --- COST mode (section 4.5 / FR-9) --------------------------------------------------------

    @Test
    public void costModePicksTheCheapestPositiveScoreOwnedItem()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10))
            .put(600, melee(EquipmentSlot.HEAD, false, 80, 0, 0, 0))  // higher score, dearer
            .put(601, melee(EquipmentSlot.HEAD, false, 50, 0, 0, 0)); // lower score, cheaper
        FakePrice price = new FakePrice().put(100, 1).put(600, 1000).put(601, 500);

        Map<EquipmentSlot, Integer> worn = select(stats, price,
            meleeTask(0, 10, 10), owned(100, 600, 601), AdviceMode.COST);

        assertEquals("cost mode -> cheapest, not highest score", Integer.valueOf(601),
            worn.get(EquipmentSlot.HEAD));
    }

    @Test
    public void costModeTreatsNonPositivePriceAsLast()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10))
            .put(620, melee(EquipmentSlot.HEAD, false, 90, 0, 0, 0))  // high score, but no GE price
            .put(621, melee(EquipmentSlot.HEAD, false, 10, 0, 0, 0)); // low score, has a price
        FakePrice price = new FakePrice().put(100, 1).put(620, 0).put(621, 999);

        Map<EquipmentSlot, Integer> worn = select(stats, price,
            meleeTask(0, 10, 10), owned(100, 620, 621), AdviceMode.COST);

        assertEquals("zero-price item sorts last even with the higher score",
            Integer.valueOf(621), worn.get(EquipmentSlot.HEAD));
    }

    @Test
    public void costModeTieBreaksByItemIdAscending()
    {
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 50, 0, 0, 10))
            .put(611, melee(EquipmentSlot.HEAD, false, 50, 0, 0, 0))
            .put(610, melee(EquipmentSlot.HEAD, false, 50, 0, 0, 0));
        FakePrice price = new FakePrice().put(100, 1).put(610, 500).put(611, 500);

        Map<EquipmentSlot, Integer> worn = select(stats, price,
            meleeTask(0, 10, 10), owned(100, 610, 611), AdviceMode.COST);

        assertEquals("equal price -> lowest id", Integer.valueOf(610), worn.get(EquipmentSlot.HEAD));
    }

    @Test
    public void costModeWeaponInterplayUsesCombinedPrice()
    {
        // 2h is dearer than 1h+shield combined -> cost mode picks 1h+shield.
        FakeStats stats = new FakeStats()
            .put(700, melee(EquipmentSlot.WEAPON, true, 100, 0, 0, 0))  // 2h
            .put(701, melee(EquipmentSlot.WEAPON, false, 40, 0, 0, 0))  // 1h
            .put(702, melee(EquipmentSlot.SHIELD, false, 30, 0, 0, 0)); // shield
        FakePrice price = new FakePrice().put(700, 5000).put(701, 1000).put(702, 1000);

        Map<EquipmentSlot, Integer> worn = select(stats, price,
            meleeTask(0, 10, 10), owned(700, 701, 702), AdviceMode.COST);

        assertEquals(Integer.valueOf(701), worn.get(EquipmentSlot.WEAPON));
        assertEquals(Integer.valueOf(702), worn.get(EquipmentSlot.SHIELD));
    }

    // --- ranged ammo (section 4.7) -------------------------------------------------------------

    @Test
    public void rangedAmmoPicksHighestRangedStrengthNotHighestScore()
    {
        FakeStats stats = new FakeStats()
            .put(700, ranged(EquipmentSlot.WEAPON, true, 50, 10)) // ranged 2h weapon
            .put(701, ranged(EquipmentSlot.AMMO, false, 0, 10))   // rstr 10
            .put(702, ranged(EquipmentSlot.AMMO, false, 0, 40))   // rstr 40 -> should win
            .put(703, ranged(EquipmentSlot.AMMO, false, 100, 5)); // huge arange (score) but rstr 5

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            rangedTask(), owned(700, 701, 702, 703), AdviceMode.DPS);

        assertEquals(Integer.valueOf(700), worn.get(EquipmentSlot.WEAPON));
        assertEquals("ammo chosen by ranged strength, not by score", Integer.valueOf(702),
            worn.get(EquipmentSlot.AMMO));
    }

    // --- ammo + blowpipe darts fold into ranged weapon ranking (WD-8, FR-C2 5.5/5.6) -----------

    @Test
    public void strongerOwnedAmmoLiftsAFasterBowAboveASlowerStrongerBowInRanking()
    {
        // WD-8: the picked AMMO's ranged strength folds into the weapon's DPS in RANKING (matching the
        // display estimator, which already sums the worn ammo). A fast, low-strength bow (2t) and a
        // slow, high-strength bow (4t) both wear the same best owned ammo. With WEAK ammo the slow bow's
        // strength advantage wins; with STRONG ammo the strength ratio narrows and the fast bow's
        // speed/accuracy edge wins. The pick flips ONLY because ammo strength enters ranking.
        int fastBow = 8100;   // fast, no base strength
        int slowBow = 8101;   // slow, high base strength
        int weakAmmo = 8110;  // rstr 2
        int strongAmmo = 8111; // rstr 200
        FakeStats stats = new FakeStats()
            .put(fastBow, rangedWeapon(true, 0, 0, 2))
            .put(slowBow, rangedWeapon(true, 0, 100, 4))
            .put(weakAmmo, ranged(EquipmentSlot.AMMO, false, 0, 2))
            .put(strongAmmo, ranged(EquipmentSlot.AMMO, false, 0, 200));

        Map<EquipmentSlot, Integer> weak = select(stats, new FakePrice(),
            rangedTask(), owned(fastBow, slowBow, weakAmmo), AdviceMode.DPS);
        assertEquals("with weak ammo the slow, stronger bow wins", Integer.valueOf(slowBow),
            weak.get(EquipmentSlot.WEAPON));

        Map<EquipmentSlot, Integer> strong = select(stats, new FakePrice(),
            rangedTask(), owned(fastBow, slowBow, strongAmmo), AdviceMode.DPS);
        assertEquals("with strong ammo folded into ranking the fast bow wins", Integer.valueOf(fastBow),
            strong.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void blowpipeCreditedItsBestOwnedDartRangedStrength()
    {
        // WD-8: the blowpipe is credited its BEST owned dart's ranged strength before ranking. A fast
        // blowpipe (2t) vs a slower, stronger rival; two darts owned (weak + strong). The best dart is
        // folded, lifting the blowpipe above the rival, and that dart is the worn AMMO.
        int blowpipe = 8100; // fast, no base strength
        int rival = 8101;    // slow, high base strength
        int weakDart = 8110;   // rstr 2
        int strongDart = 8111; // rstr 200 -> the best owned dart
        FakeStats stats = new FakeStats()
            .put(blowpipe, rangedWeapon(true, 0, 0, 2))
            .put(rival, rangedWeapon(true, 0, 100, 4))
            .put(weakDart, ranged(EquipmentSlot.AMMO, false, 0, 2))
            .put(strongDart, ranged(EquipmentSlot.AMMO, false, 0, 200));

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            rangedTask(), owned(blowpipe, rival, weakDart, strongDart), AdviceMode.DPS);
        assertEquals("blowpipe wins once its best owned dart's strength is credited",
            Integer.valueOf(blowpipe), worn.get(EquipmentSlot.WEAPON));
        assertEquals("the best owned dart is the worn ammo", Integer.valueOf(strongDart),
            worn.get(EquipmentSlot.AMMO));
    }

    // --- Void set bonus post-pick pass (WD-10, ADR-0020 #4) ------------------------------------

    private static final int VOID_RANGER_HELM = net.runelite.api.ItemID.VOID_RANGER_HELM;
    private static final int VOID_TOP = net.runelite.api.ItemID.VOID_KNIGHT_TOP;
    private static final int VOID_ROBE = net.runelite.api.ItemID.VOID_KNIGHT_ROBE;
    private static final int VOID_GLOVES = net.runelite.api.ItemID.VOID_KNIGHT_GLOVES;
    private static final int ELITE_VOID_TOP = net.runelite.api.ItemID.ELITE_VOID_TOP;
    private static final int ELITE_VOID_ROBE = net.runelite.api.ItemID.ELITE_VOID_ROBE;

    // Flat-better ranged armour alternatives (higher accuracy score than the void pieces per slot).
    private static final int ALT_HEAD = 8210;
    private static final int ALT_BODY = 8211;
    private static final int ALT_LEGS = 8212;
    private static final int ALT_HANDS = 8213;
    private static final int RANGED_2H = 8200;

    private static FakeStats voidRangedFixture()
    {
        return new FakeStats()
            .put(RANGED_2H, rangedWeapon(true, 100, 0, 4))
            .put(VOID_RANGER_HELM, ranged(EquipmentSlot.HEAD, false, 0, 30))
            .put(VOID_TOP, ranged(EquipmentSlot.BODY, false, 0, 30))
            .put(VOID_ROBE, ranged(EquipmentSlot.LEGS, false, 0, 30))
            .put(VOID_GLOVES, ranged(EquipmentSlot.HANDS, false, 0, 30))
            .put(ELITE_VOID_TOP, ranged(EquipmentSlot.BODY, false, 0, 30))
            .put(ELITE_VOID_ROBE, ranged(EquipmentSlot.LEGS, false, 0, 30))
            // Alternatives: same strength but extra ranged attack -> higher per-slot score, so they
            // win every armour slot UNLESS the complete void set's loadout multiplier flips it.
            .put(ALT_HEAD, ranged(EquipmentSlot.HEAD, false, 5, 30))
            .put(ALT_BODY, ranged(EquipmentSlot.BODY, false, 5, 30))
            .put(ALT_LEGS, ranged(EquipmentSlot.LEGS, false, 5, 30))
            .put(ALT_HANDS, ranged(EquipmentSlot.HANDS, false, 5, 30));
    }

    @Test
    public void completeVoidSetWinsTheArmourSlotsViaTheSetMultiplier()
    {
        // WD-10: a complete owned Void set applies its +10%/+10% loadout multiplier, lifting the whole
        // void loadout above the per-slot flat-better alternatives - so the four void pieces are worn.
        FakeStats stats = voidRangedFixture();
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(), rangedTask(),
            owned(RANGED_2H, VOID_RANGER_HELM, VOID_TOP, VOID_ROBE, VOID_GLOVES,
                ALT_HEAD, ALT_BODY, ALT_LEGS, ALT_HANDS), AdviceMode.DPS);
        assertEquals(Integer.valueOf(VOID_RANGER_HELM), worn.get(EquipmentSlot.HEAD));
        assertEquals(Integer.valueOf(VOID_TOP), worn.get(EquipmentSlot.BODY));
        assertEquals(Integer.valueOf(VOID_ROBE), worn.get(EquipmentSlot.LEGS));
        assertEquals(Integer.valueOf(VOID_GLOVES), worn.get(EquipmentSlot.HANDS));
    }

    @Test
    public void incompleteVoidSetGetsNoBonusSoTheFlatAlternativesWin()
    {
        // WD-10: 3/4 void pieces (no gloves) -> no set bonus -> the per-slot flat-better alternatives
        // win their slots (including the hands, where only the alternative is owned).
        FakeStats stats = voidRangedFixture();
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(), rangedTask(),
            owned(RANGED_2H, VOID_RANGER_HELM, VOID_TOP, VOID_ROBE,
                ALT_HEAD, ALT_BODY, ALT_LEGS, ALT_HANDS), AdviceMode.DPS);
        assertEquals(Integer.valueOf(ALT_HEAD), worn.get(EquipmentSlot.HEAD));
        assertEquals(Integer.valueOf(ALT_BODY), worn.get(EquipmentSlot.BODY));
        assertEquals(Integer.valueOf(ALT_LEGS), worn.get(EquipmentSlot.LEGS));
        assertEquals(Integer.valueOf(ALT_HANDS), worn.get(EquipmentSlot.HANDS));
    }

    @Test
    public void completeEliteVoidSetWearsTheElitePieces()
    {
        // WD-10: with elite top+robe owned, the elite pieces (higher ranged damage bonus) are the ones
        // worn when the set wins.
        FakeStats stats = voidRangedFixture();
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(), rangedTask(),
            owned(RANGED_2H, VOID_RANGER_HELM, ELITE_VOID_TOP, ELITE_VOID_ROBE, VOID_GLOVES,
                ALT_HEAD, ALT_BODY, ALT_LEGS, ALT_HANDS), AdviceMode.DPS);
        assertEquals(Integer.valueOf(ELITE_VOID_TOP), worn.get(EquipmentSlot.BODY));
        assertEquals(Integer.valueOf(ELITE_VOID_ROBE), worn.get(EquipmentSlot.LEGS));
    }

    // --- Twisted bow per-target Magic-level scaling (WD-11, ADR-0020 §5, PD-D3) -----------------

    private static final int TWISTED_BOW = net.runelite.api.ItemID.TWISTED_BOW;
    private static final int RIVAL_BOW = 8300;

    private static FakeStats twistedBowFixture()
    {
        return new FakeStats()
            // Tbow: modest base ranged strength, slower (5t). On base stats the rival out-ranks it.
            .put(TWISTED_BOW, rangedWeapon(true, 0, 70, 5))
            // Rival: stronger base ranged strength + attack, faster (4t) -> wins with no magic scaling.
            .put(RIVAL_BOW, rangedWeapon(true, 50, 110, 4));
    }

    private static MonsterProfile rangedProfileWithMagicLevel(Integer magicLevel)
    {
        TaskData t = rangedTask();
        if (magicLevel != null)
        {
            t.setOffence(new com.danieljglover.allinslayer.model.MonsterOffence(
                null, null, null, null, magicLevel, false, false));
        }
        return MonsterProfile.fromTask(t);
    }

    @Test
    public void twistedBowRankedOnBaseStatsWhenMagicLevelUnknown()
    {
        // FR-6: no monster Magic level -> the Twisted bow is ranked on its base stats only (no fabricated
        // scaling), so the stronger rival bow wins - byte-identical to the pre-WD-11 path.
        FakeStats stats = twistedBowFixture();
        Map<EquipmentSlot, Integer> worn = selectProfile(stats,
            rangedProfileWithMagicLevel(null), CombatStyle.RANGED, owned(TWISTED_BOW, RIVAL_BOW));
        assertEquals("no magic level -> rival bow wins on base stats",
            Integer.valueOf(RIVAL_BOW), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void highMagicLevelScalesTheTwistedBowAboveTheRivalBow()
    {
        // WD-11: against a high-Magic target the Twisted bow's accuracy/damage scaling lifts its DPS
        // above the otherwise-stronger rival, flipping the pick. The flip is driven ONLY by magicLevel.
        FakeStats stats = twistedBowFixture();
        Map<EquipmentSlot, Integer> worn = selectProfile(stats,
            rangedProfileWithMagicLevel(250), CombatStyle.RANGED, owned(TWISTED_BOW, RIVAL_BOW));
        assertEquals("high magic level -> the Twisted bow's scaling wins the weapon slot",
            Integer.valueOf(TWISTED_BOW), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void magicScoresByMagicAttackPlusWeightedMagicDamage()
    {
        // MAGIC_DMG_WEIGHT = 5: amagic 10 + 5*8% = 50 beats amagic 40 + 5*0 = 40.
        FakeStats stats = new FakeStats()
            .put(100, magic(EquipmentSlot.WEAPON, false, 30, 0)) // weapon, score 30
            .put(950, magic(EquipmentSlot.HEAD, false, 10, 8))   // 10 + 40 = 50
            .put(951, magic(EquipmentSlot.HEAD, false, 40, 0));  // 40

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            magicTask("fire"), owned(100, 950, 951), AdviceMode.DPS);

        assertEquals(Integer.valueOf(950), worn.get(EquipmentSlot.HEAD));
        assertEquals("MAGIC_DMG_WEIGHT is the documented 5", 5, GearSelector.MAGIC_DMG_WEIGHT);
    }

    // --- task-conditional gear bonuses (ADR-0007, FR-12) ---------------------------------------

    private static final int SLAYER_HELMET_I = 11865;
    private static final int BLACK_MASK = 8921;
    private static final int SALVE_AMULET_EI = 12018;

    @Test
    public void slayerHelmIOutranksSerpentineHelmOnAMeleeSlayerTask()
    {
        // FR-12.1: the reported bug. Serpentine helm has the higher FLAT head score, but on a melee
        // Slayer task the Slayer helm (i)'s +16.67% on-task bonus must lift it above the Serpentine.
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 100, 0, 0, 0)) // weapon, base 100
            .put(700, melee(EquipmentSlot.HEAD, false, 30, 0, 0, 0))    // serpentine helm, base 30
            .put(SLAYER_HELMET_I, melee(EquipmentSlot.HEAD, false, 20, 0, 0, 0)); // slayer (i), base 20

        TaskData task = meleeTask(0, 10, 10);
        task.setSlayerHelmApplies(true);

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            task, owned(100, 700, SLAYER_HELMET_I), AdviceMode.DPS);

        assertEquals("slayer helm (i) wins via the on-task bonus, not flat stats",
            Integer.valueOf(SLAYER_HELMET_I), worn.get(EquipmentSlot.HEAD));
    }

    @Test
    public void salveEiIsChosenOnAnUndeadTaskAndNotOnANonUndeadTask()
    {
        // FR-12.2: a 0-flat Salve (ei) must beat a higher-flat Fury vs undead (the +20% term lifts it
        // past the score>0 filter), and must NOT be chosen vs a non-undead monster (predicate false).
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 100, 0, 0, 0)) // weapon, base 100
            .put(800, melee(EquipmentSlot.AMULET, false, 10, 0, 0, 0))  // fury, base 10
            .put(SALVE_AMULET_EI, melee(EquipmentSlot.AMULET, false, 0, 0, 0, 0)); // salve (ei), base 0

        TaskData undead = meleeTask(0, 10, 10);
        undead.setUndead(true);
        Map<EquipmentSlot, Integer> wornUndead = select(stats, new FakePrice(),
            undead, owned(100, 800, SALVE_AMULET_EI), AdviceMode.DPS);
        assertEquals("salve (ei) wins on an undead task", Integer.valueOf(SALVE_AMULET_EI),
            wornUndead.get(EquipmentSlot.AMULET));

        TaskData notUndead = meleeTask(0, 10, 10); // undead defaults false
        Map<EquipmentSlot, Integer> wornNot = select(stats, new FakePrice(),
            notUndead, owned(100, 800, SALVE_AMULET_EI), AdviceMode.DPS);
        assertEquals("fury wins when not vs undead (salve gets no bonus, 0-flat -> filtered)",
            Integer.valueOf(800), wornNot.get(EquipmentSlot.AMULET));
    }

    @Test
    public void salveEiRaisesAmuletScoreOnAMagicUndeadTask()
    {
        // FR-12.2 (Aberrant-spectres shape): a MAGIC undead task; Salve (ei)'s +20% magic term lifts
        // the 0-flat salve above a flat magic amulet (e.g. occult) it would otherwise lose to.
        FakeStats stats = new FakeStats()
            .put(100, magic(EquipmentSlot.WEAPON, false, 100, 0)) // magic weapon, base 100
            .put(801, magic(EquipmentSlot.AMULET, false, 20, 0))  // occult, base 20
            .put(SALVE_AMULET_EI, magic(EquipmentSlot.AMULET, false, 0, 0)); // salve (ei), base 0

        TaskData task = magicTask("air");
        task.setUndead(true);

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            task, owned(100, 801, SALVE_AMULET_EI), AdviceMode.DPS);

        assertEquals("salve (ei) magic bonus beats the flat magic amulet",
            Integer.valueOf(SALVE_AMULET_EI), worn.get(EquipmentSlot.AMULET));
    }

    @Test
    public void helmAndSalveApplyIndependentlyPerSlotOnAnUndeadSlayerTask()
    {
        // FR-12.3: on a task that is BOTH on-Slayer and undead (Ankou-shape), the head and amulet
        // bonuses apply independently - the best helm AND the best amulet each get their own term.
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 100, 0, 0, 0))
            .put(700, melee(EquipmentSlot.HEAD, false, 30, 0, 0, 0))    // serpentine helm, base 30
            .put(SLAYER_HELMET_I, melee(EquipmentSlot.HEAD, false, 20, 0, 0, 0))
            .put(800, melee(EquipmentSlot.AMULET, false, 10, 0, 0, 0))  // fury, base 10
            .put(SALVE_AMULET_EI, melee(EquipmentSlot.AMULET, false, 0, 0, 0, 0));

        TaskData task = meleeTask(0, 10, 10);
        task.setSlayerHelmApplies(true);
        task.setUndead(true);

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            task, owned(100, 700, SLAYER_HELMET_I, 800, SALVE_AMULET_EI), AdviceMode.DPS);

        assertEquals("slayer helm (i) wins the head slot", Integer.valueOf(SLAYER_HELMET_I),
            worn.get(EquipmentSlot.HEAD));
        assertEquals("salve (ei) wins the amulet slot", Integer.valueOf(SALVE_AMULET_EI),
            worn.get(EquipmentSlot.AMULET));
    }

    @Test
    public void noConditionalBonusWhenSlayerHelmDoesNotApply()
    {
        // The predicate gates the bonus: on a task that does NOT count for the Slayer-helm bonus, a
        // black mask gets no boost and the higher-flat plain helm wins.
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 100, 0, 0, 0))
            .put(700, melee(EquipmentSlot.HEAD, false, 30, 0, 0, 0))  // plain helm, base 30
            .put(BLACK_MASK, melee(EquipmentSlot.HEAD, false, 5, 0, 0, 0)); // black mask, base 5

        TaskData task = meleeTask(0, 10, 10); // slayerHelmApplies defaults false

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            task, owned(100, 700, BLACK_MASK), AdviceMode.DPS);

        assertEquals("no bonus off-task -> the higher-flat helm wins", Integer.valueOf(700),
            worn.get(EquipmentSlot.HEAD));
    }

    // --- DPS-driven weapon ranking (ADR-0008, WDB-4/5) -----------------------------------------

    @Test
    public void fasterHigherDpsWeaponBeatsSlowerHigherMaxHitWeapon()
    {
        // FR-13.1/13.2/13.6: a slow, high-max-hit weapon (AGS-shaped, 6t) loses to a faster weapon
        // (5t) with lower raw stats, because the WEAPON slot is now ranked by real sustained DPS.
        FakeStats stats = new FakeStats()
            .put(900, meleeWeapon(false, 130, 130, 130, 130, 7)) // slow, very strong (AGS-shaped)
            .put(901, meleeWeapon(false, 82, 82, 82, 82, 4));    // faster, weaker
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(10, 10, 10), owned(900, 901), AdviceMode.DPS);
        assertEquals("the faster, higher-DPS weapon wins despite the lower max hit",
            Integer.valueOf(901), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void fangAccuracyRerollOutranksAHigherMaxHitWeaponOnAHighDefenceMonster()
    {
        // FR-13.2: Osmumten's fang (5t, rolls accuracy twice) beats a same-speed weapon with a higher
        // max hit on a high-defence monster, because the second accuracy roll lifts its real DPS.
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.OSMUMTENS_FANG, meleeWeapon(false, 90, 0, 0, 60, 5))
            .put(950, meleeWeapon(false, 95, 0, 0, 95, 5)); // higher attack AND strength, no reroll
        // High stab defence so neither weapon saturates to ~100% hit chance.
        TaskData task = task(CombatStyle.MELEE, null, new MonsterDefence(250, 250, 250, 250, 0, 0));
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            task, owned(net.runelite.api.ItemID.OSMUMTENS_FANG, 950), AdviceMode.DPS);
        assertEquals("fang's accuracy reroll wins on a high-defence monster",
            Integer.valueOf(net.runelite.api.ItemID.OSMUMTENS_FANG), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void nonWeaponSlotsUnchangedUnderDpsWeaponRanking()
    {
        // FR-13.4: non-weapon slots are still flat-scored (base + LFB term), unaffected by DPS weapon
        // ranking - the higher-flat-score head still wins.
        FakeStats stats = new FakeStats()
            .put(900, meleeWeapon(false, 80, 0, 0, 80, 5))
            .put(200, melee(EquipmentSlot.HEAD, false, 10, 0, 0, 5))  // score 15
            .put(201, melee(EquipmentSlot.HEAD, false, 20, 0, 0, 2)); // score 22 -> wins
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(0, 10, 10), owned(900, 200, 201), AdviceMode.DPS);
        assertEquals("non-weapon slot still picks the higher flat score", Integer.valueOf(201),
            worn.get(EquipmentSlot.HEAD));
    }

    @Test
    public void rangedWeaponRankedByDps()
    {
        // FR-13.7: a faster ranged weapon with a lower max hit out-DPS a slower one.
        FakeStats stats = new FakeStats()
            .put(910, rangedWeapon(true, 80, 80, 6))  // slow, strong
            .put(911, rangedWeapon(true, 55, 55, 4)); // faster, weaker
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            rangedTask(), owned(910, 911), AdviceMode.DPS);
        assertEquals("the faster ranged weapon wins on DPS", Integer.valueOf(911),
            worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void twoHandVsOneHandPlusShieldComparedByCombinedDps()
    {
        // FR-13.8 (WDB-5): the 2h-vs-(1h+shield) choice is decided by combined DPS - the shield's
        // offensive bonuses are summed into the 1h's DPS. Here a strong shield lifts a slightly weaker
        // 1h above the 2h.
        FakeStats stats = new FakeStats()
            .put(920, meleeWeapon(true, 90, 0, 0, 75, 5))   // 2h
            .put(921, meleeWeapon(false, 80, 0, 0, 70, 5))  // 1h, slightly weaker
            .put(922, melee(EquipmentSlot.SHIELD, false, 20, 0, 0, 15)); // shield adds attack+str
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            meleeTask(10, 10, 10), owned(920, 921, 922), AdviceMode.DPS);
        assertEquals("1h+shield wins on combined DPS", Integer.valueOf(921),
            worn.get(EquipmentSlot.WEAPON));
        assertEquals(Integer.valueOf(922), worn.get(EquipmentSlot.SHIELD));
    }

    @Test
    public void costModePicksCheapestPositiveDpsWeapon()
    {
        // FR-13.5: in COST mode the WEAPON slot is the cheapest weapon with positive DPS (not the
        // cheapest flat score). Both weapons are usable; the cheaper one wins.
        FakeStats stats = new FakeStats()
            .put(930, meleeWeapon(false, 120, 0, 0, 120, 5)) // strong but dear
            .put(931, meleeWeapon(false, 60, 0, 0, 60, 5));  // weaker but cheap
        FakePrice price = new FakePrice().put(930, 5000).put(931, 200);
        Map<EquipmentSlot, Integer> worn = select(stats, price,
            meleeTask(10, 10, 10), owned(930, 931), AdviceMode.COST);
        assertEquals("cost mode picks the cheaper positive-DPS weapon", Integer.valueOf(931),
            worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void costModeSkipsZeroDpsWeapon()
    {
        // A weapon that cannot deal damage for the style (zero max hit / zero hit chance) has zero DPS
        // and is not viable, even though it is the cheapest - the usable weapon is chosen instead.
        FakeStats stats = new FakeStats()
            .put(940, meleeWeapon(false, -64, -64, -64, -64, 5)) // 0 DPS: cannot hit, 0 max
            .put(941, meleeWeapon(false, 60, 0, 0, 60, 5));      // usable
        FakePrice price = new FakePrice().put(940, 1).put(941, 9999);
        Map<EquipmentSlot, Integer> worn = select(stats, price,
            meleeTask(10, 10, 10), owned(940, 941), AdviceMode.COST);
        assertEquals("the zero-DPS weapon is skipped despite being cheapest", Integer.valueOf(941),
            worn.get(EquipmentSlot.WEAPON));
    }

    // --- dragonbane via the conditional registry (WDB-7, FR-13.10) -----------------------------

    private static final int DRAGON_HUNTER_LANCE = 22978;

    private static TaskData dragonTask(boolean dragon)
    {
        // A typical low/moderate-defence dragon: both weapons sit near the accuracy cap, so the DHL's
        // +20% damage drives the ranking (the Fang's reroll only dominates vs very high defence).
        TaskData t = task(CombatStyle.MELEE, null, new MonsterDefence(1, 1, 1, 1, 0, 0));
        t.setDragon(dragon);
        return t;
    }

    @Test
    public void dragonHunterLanceOutranksFangOnADragonTask()
    {
        // FR-13.10: on a dragon task the DHL's own +20% acc/dmg (applied multiplicatively in weaponDps)
        // lifts it above the Fang, which would otherwise win on its higher strength + accuracy reroll.
        FakeStats stats = new FakeStats()
            .put(DRAGON_HUNTER_LANCE, meleeWeapon(false, 85, 0, 0, 70, 5))
            .put(net.runelite.api.ItemID.OSMUMTENS_FANG, meleeWeapon(false, 85, 0, 0, 75, 5));
        int[] ids = {DRAGON_HUNTER_LANCE, net.runelite.api.ItemID.OSMUMTENS_FANG};

        Map<EquipmentSlot, Integer> onDragon = select(stats, new FakePrice(),
            dragonTask(true), owned(ids), AdviceMode.DPS);
        assertEquals("DHL wins on a dragon task via its +20% bonus",
            Integer.valueOf(DRAGON_HUNTER_LANCE), onDragon.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void dragonHunterLanceGetsNoBonusOffDragonTask()
    {
        // Off a dragon task the predicate is false: DHL gets no bonus and the Fang (higher str +
        // accuracy reroll) wins.
        FakeStats stats = new FakeStats()
            .put(DRAGON_HUNTER_LANCE, meleeWeapon(false, 85, 0, 0, 70, 5))
            .put(net.runelite.api.ItemID.OSMUMTENS_FANG, meleeWeapon(false, 85, 0, 0, 75, 5));
        int[] ids = {DRAGON_HUNTER_LANCE, net.runelite.api.ItemID.OSMUMTENS_FANG};

        Map<EquipmentSlot, Integer> offDragon = select(stats, new FakePrice(),
            dragonTask(false), owned(ids), AdviceMode.DPS);
        assertEquals("off a dragon task the Fang wins (DHL gets no bonus)",
            Integer.valueOf(net.runelite.api.ItemID.OSMUMTENS_FANG), offDragon.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void weaponConditionalBonusNotAlsoAddedAsAdditiveTerm()
    {
        // No-double-count (design section 3.5): a weapon's conditional bonus is applied ONLY
        // multiplicatively in weaponDps - never also as the additive (m-1)*L term. Here the DHL's
        // true x1.20 DPS is strictly below the Fang's, and a large loadout offence L is present (a
        // big-stat head). If the weapon also wrongly received +round(0.20*L), the DHL would leap past
        // the Fang. It must not: the Fang still wins.
        FakeStats stats = new FakeStats()
            .put(DRAGON_HUNTER_LANCE, meleeWeapon(false, 40, 0, 0, 30, 5)) // weak DHL
            .put(net.runelite.api.ItemID.OSMUMTENS_FANG, meleeWeapon(false, 90, 0, 0, 90, 5)) // strong Fang
            .put(500, melee(EquipmentSlot.HEAD, false, 400, 0, 0, 400)); // huge L
        int[] ids = {DRAGON_HUNTER_LANCE, net.runelite.api.ItemID.OSMUMTENS_FANG, 500};

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            dragonTask(true), owned(ids), AdviceMode.DPS);
        assertEquals("the weapon's conditional bonus is multiplicative only, not additively double-counted",
            Integer.valueOf(net.runelite.api.ItemID.OSMUMTENS_FANG), worn.get(EquipmentSlot.WEAPON));
    }

    // --- demonbane via monster-category predicate (WDB-10, FR-14.1/14.2/14.4) ------------------

    private static TaskData demonTask(boolean demon, CombatStyle style)
    {
        TaskData t = task(style, style == CombatStyle.MAGIC ? "fire" : null,
            new MonsterDefence(1, 1, 1, 1, 1, 1));
        t.setDemon(demon);
        return t;
    }

    @Test
    public void arclightOutranksScimitarOnADemonMeleeTask()
    {
        // FR-14.1: on a demon task Arclight's +70% acc/dmg lifts it above a higher-base scimitar.
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.ARCLIGHT, meleeWeapon(false, 0, 70, 0, 65, 4))
            .put(net.runelite.api.ItemID.DRAGON_SCIMITAR, meleeWeapon(false, 0, 75, 0, 70, 4));
        int[] ids = {net.runelite.api.ItemID.ARCLIGHT, net.runelite.api.ItemID.DRAGON_SCIMITAR};

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            demonTask(true, CombatStyle.MELEE), owned(ids), AdviceMode.DPS);
        assertEquals("Arclight wins on a demon task via +70%",
            Integer.valueOf(net.runelite.api.ItemID.ARCLIGHT), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void demonbaneGivesNoBonusOffDemonTask()
    {
        // FR-14.2: off a demon task the predicate is false; the higher-base scimitar wins.
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.ARCLIGHT, meleeWeapon(false, 0, 70, 0, 65, 4))
            .put(net.runelite.api.ItemID.DRAGON_SCIMITAR, meleeWeapon(false, 0, 75, 0, 70, 4));
        int[] ids = {net.runelite.api.ItemID.ARCLIGHT, net.runelite.api.ItemID.DRAGON_SCIMITAR};

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            demonTask(false, CombatStyle.MELEE), owned(ids), AdviceMode.DPS);
        assertEquals("off a demon task the scimitar wins (no demonbane bonus)",
            Integer.valueOf(net.runelite.api.ItemID.DRAGON_SCIMITAR), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void scorchingBowRanksUpOnASyntheticDemonRangedTask()
    {
        // FR-14.4: the ranged Scorching bow ranks up on a (synthetic) demon RANGED task via +30%.
        // No real dataset demon task is ranged - this proves the latent ranged demonbane row.
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.SCORCHING_BOW, rangedWeapon(true, 60, 60, 5))
            .put(8000, rangedWeapon(true, 70, 70, 5)); // higher-base plain bow
        int[] ids = {net.runelite.api.ItemID.SCORCHING_BOW, 8000};

        Map<EquipmentSlot, Integer> onDemon = select(stats, new FakePrice(),
            demonTask(true, CombatStyle.RANGED), owned(ids), AdviceMode.DPS);
        assertEquals("Scorching bow ranks up on a synthetic demon ranged task",
            Integer.valueOf(net.runelite.api.ItemID.SCORCHING_BOW), onDemon.get(EquipmentSlot.WEAPON));

        Map<EquipmentSlot, Integer> offDemon = select(stats, new FakePrice(),
            demonTask(false, CombatStyle.RANGED), owned(ids), AdviceMode.DPS);
        assertEquals("off a demon task the higher-base bow wins", Integer.valueOf(8000),
            offDemon.get(EquipmentSlot.WEAPON));
    }

    // --- Keris vs kalphites (asymmetric, WDB-12, FR-14.5) --------------------------------------

    private static TaskData kalphiteTask(boolean kalphite)
    {
        TaskData t = task(CombatStyle.MELEE, null, new MonsterDefence(1, 1, 1, 1, 0, 0));
        t.setKalphite(kalphite);
        return t;
    }

    @Test
    public void kerisRanksUpOnAKalphiteTask()
    {
        // FR-14.5: on a kalphite task the Keris's damage-only +38.2% lifts it above a higher-base
        // weapon, despite having NO accuracy bonus (asymmetric).
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.KERIS, meleeWeapon(false, 60, 0, 0, 55, 4))
            .put(net.runelite.api.ItemID.DRAGON_SCIMITAR, meleeWeapon(false, 0, 70, 0, 65, 4));
        int[] ids = {net.runelite.api.ItemID.KERIS, net.runelite.api.ItemID.DRAGON_SCIMITAR};

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            kalphiteTask(true), owned(ids), AdviceMode.DPS);
        assertEquals("Keris wins on a kalphite task via the damage-only multiplier",
            Integer.valueOf(net.runelite.api.ItemID.KERIS), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void kerisNoBonusOffKalphiteTask()
    {
        // Off a kalphite task the Keris gets no bonus and the higher-base weapon wins.
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.KERIS, meleeWeapon(false, 60, 0, 0, 55, 4))
            .put(net.runelite.api.ItemID.DRAGON_SCIMITAR, meleeWeapon(false, 0, 70, 0, 65, 4));
        int[] ids = {net.runelite.api.ItemID.KERIS, net.runelite.api.ItemID.DRAGON_SCIMITAR};

        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            kalphiteTask(false), owned(ids), AdviceMode.DPS);
        assertEquals("off a kalphite task the higher-base weapon wins",
            Integer.valueOf(net.runelite.api.ItemID.DRAGON_SCIMITAR), worn.get(EquipmentSlot.WEAPON));
    }

    // --- dragonbane ranged/magic via the conditional registry (WB-1, FR-C2 5.2) ----------------

    @Test
    public void dragonHunterCrossbowOutranksGenericCrossbowOnADragonTask()
    {
        // WB-1: on a RANGED dragon task the DHCB's +30% acc/+25% dmg (asymmetric, applied inside
        // weaponDps) lifts it above a flat-better generic crossbow; off-dragon the generic wins.
        int dhcb = net.runelite.api.ItemID.DRAGON_HUNTER_CROSSBOW;
        int rune = net.runelite.api.ItemID.RUNE_CROSSBOW;
        FakeStats stats = new FakeStats()
            .put(dhcb, rangedWeapon(false, 85, 0, 5))
            .put(rune, rangedWeapon(false, 95, 0, 5)); // flat-better
        TaskData onDragon = task(CombatStyle.RANGED, null, new MonsterDefence(1, 0, 0, 0, 0, 0));
        onDragon.setDragon(true);
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            onDragon, owned(dhcb, rune), AdviceMode.DPS);
        assertEquals("DHCB wins on a dragon task via its +30%/+25% bonus",
            Integer.valueOf(dhcb), worn.get(EquipmentSlot.WEAPON));

        Map<EquipmentSlot, Integer> offDragon = select(stats, new FakePrice(),
            rangedTask(), owned(dhcb, rune), AdviceMode.DPS);
        assertEquals("off a dragon task the flat-better crossbow wins",
            Integer.valueOf(rune), offDragon.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void dragonHunterWandCreditedOnAMagicDragonTask()
    {
        // WB-1: on a MAGIC dragon task the DH wand's +75% acc/+40% dmg (asymmetric, applied in
        // magicWeaponDps) lifts it above a flat-better standard caster; off-dragon that one wins.
        int wand = net.runelite.api.ItemID.DRAGON_HUNTER_WAND;
        int generic = 7002; // synthetic flat-better standard caster (5t like the wand)
        FakeStats stats = new FakeStats()
            .put(wand, magicWeapon(false, 40, 0, 5))
            .put(generic, magicWeapon(false, 60, 0, 5));
        TaskData onDragon = task(CombatStyle.MAGIC, "fire", new MonsterDefence(1, 0, 0, 0, 0, 0));
        onDragon.setDragon(true);
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            onDragon, owned(wand, generic), AdviceMode.DPS);
        assertEquals("DH wand wins on a magic dragon task via its +75%/+40% bonus",
            Integer.valueOf(wand), worn.get(EquipmentSlot.WEAPON));

        Map<EquipmentSlot, Integer> offDragon = select(stats, new FakePrice(),
            magicTask("fire"), owned(wand, generic), AdviceMode.DPS);
        assertEquals("off a dragon task the flat-better caster wins",
            Integer.valueOf(generic), offDragon.get(EquipmentSlot.WEAPON));
    }

    // --- gear-aware magic weapon DPS (WDB-14, FR-14.7) -----------------------------------------

    private static final int TRIDENT_SEAS = 11905; // powered staff, base max hit 23

    private static Bonuses magicWeapon(boolean twoH, int amagic, int mdmg, int speed)
    {
        return new Bonuses(0, 0, 0, amagic, 0, 0, 0, mdmg, speed, EquipmentSlot.WEAPON, twoH);
    }

    @Test
    public void higherDpsMagicWeaponSelectedOverFlatBetter()
    {
        // FR-14.7: a faster powered staff (Trident, 4t, own base max hit) out-DPS a flat-better wand
        // (higher magic attack but a slower 5t standard cast), so it is selected though its flat magic
        // score is lower.
        FakeStats stats = new FakeStats()
            .put(7000, magicWeapon(true, 50, 0, 5))           // flat score 50, standard 5t cast
            .put(TRIDENT_SEAS, magicWeapon(true, 20, 0, 4));  // flat score 20, fast powered staff
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            magicTask("fire"), owned(7000, TRIDENT_SEAS), AdviceMode.DPS);
        assertEquals("the higher-DPS powered staff wins over the flat-better wand",
            Integer.valueOf(TRIDENT_SEAS), worn.get(EquipmentSlot.WEAPON));
    }

    @Test
    public void nonWeaponMagicSlotsUnchanged()
    {
        // The non-weapon magic slots are still flat-scored (amagic + 5*mdmg), unaffected by the
        // gear-aware weapon DPS - the higher flat-score head wins.
        FakeStats stats = new FakeStats()
            .put(7000, magicWeapon(true, 40, 0, 5))
            .put(950, magic(EquipmentSlot.HEAD, false, 10, 8))  // 10 + 40 = 50 -> wins
            .put(951, magic(EquipmentSlot.HEAD, false, 40, 0));  // 40
        Map<EquipmentSlot, Integer> worn = select(stats, new FakePrice(),
            magicTask("fire"), owned(7000, 950, 951), AdviceMode.DPS);
        assertEquals("non-weapon magic slot still picks the higher flat score", Integer.valueOf(950),
            worn.get(EquipmentSlot.HEAD));
    }

    // --- Tumeken's Shadow gear multiplier (WDB-15, FR-14.8) ------------------------------------

    private static final int TUMEKENS_SHADOW = 27275;

    @Test
    public void tumekensShadowWinsViaGearMultiplier()
    {
        // FR-14.8: the Shadow's value is its x3 multiplier on OTHER gear. With strong magic-damage
        // gear it out-DPS a flat-better wand; WITHOUT that gear the wand wins - proving it wins VIA
        // the gear multiplier, not just its base max hit.
        FakeStats withGear = new FakeStats()
            .put(TUMEKENS_SHADOW, magicWeapon(true, 35, 0, 5))      // base max hit 34
            .put(7001, magicWeapon(true, 60, 60, 5))               // flat-better wand, +60% mdmg
            .put(960, magic(EquipmentSlot.HEAD, false, 0, 20));    // +20% magic damage gear
        Map<EquipmentSlot, Integer> worn = select(withGear, new FakePrice(),
            magicTask("fire"), owned(TUMEKENS_SHADOW, 7001, 960), AdviceMode.DPS);
        assertEquals("Shadow wins when its x3 gear multiplier applies",
            Integer.valueOf(TUMEKENS_SHADOW), worn.get(EquipmentSlot.WEAPON));

        // Same weapons but no magic-damage gear -> the flat-better wand wins.
        FakeStats noGear = new FakeStats()
            .put(TUMEKENS_SHADOW, magicWeapon(true, 35, 0, 5))
            .put(7001, magicWeapon(true, 60, 60, 5));
        Map<EquipmentSlot, Integer> wornNoGear = select(noGear, new FakePrice(),
            magicTask("fire"), owned(TUMEKENS_SHADOW, 7001), AdviceMode.DPS);
        assertEquals("without gear to multiply, the flat-better wand wins", Integer.valueOf(7001),
            wornNoGear.get(EquipmentSlot.WEAPON));
    }

    // --- Wilderness weapons via per-location predicate (WDB-17, FR-14.11) ----------------------

    @Test
    public void wildernessWeaponRanksUpWhenWildernessLocationSelected()
    {
        // FR-14.11: Viggora's chainmace's +50% applies ONLY when a Wilderness location is selected
        // (threaded via the select(..., wilderness) overload), lifting it above a higher-base weapon.
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.VIGGORAS_CHAINMACE, meleeWeapon(false, 0, 0, 60, 55, 4))
            .put(net.runelite.api.ItemID.DRAGON_SCIMITAR, meleeWeapon(false, 0, 0, 70, 65, 4));
        int[] ids = {net.runelite.api.ItemID.VIGGORAS_CHAINMACE, net.runelite.api.ItemID.DRAGON_SCIMITAR};
        TaskData task = meleeTask(1, 1, 1);

        Map<EquipmentSlot, Integer> inWildy = new GearSelector(stats, new FakePrice(),
            new DefaultDpsEstimator(stats))
            .select(owned(ids), MonsterProfile.fromTask(task), styleOf(task), MAXED, AdviceMode.DPS, true);
        assertEquals("Viggora's wins when a Wilderness location is selected",
            Integer.valueOf(net.runelite.api.ItemID.VIGGORAS_CHAINMACE), inWildy.get(EquipmentSlot.WEAPON));

        Map<EquipmentSlot, Integer> notWildy = new GearSelector(stats, new FakePrice(),
            new DefaultDpsEstimator(stats))
            .select(owned(ids), MonsterProfile.fromTask(task), styleOf(task), MAXED, AdviceMode.DPS, false);
        assertEquals("outside the Wilderness the higher-base weapon wins",
            Integer.valueOf(net.runelite.api.ItemID.DRAGON_SCIMITAR), notWildy.get(EquipmentSlot.WEAPON));
    }

    // --- variant profile re-key (MV-B3, FR-4) + method axis (MV-B9, ADR-0013) ------------------

    private static Map<EquipmentSlot, Integer> selectProfile(EquipmentStatsProvider stats,
        MonsterProfile profile, CombatStyle style, OwnedItems owned)
    {
        return new GearSelector(stats, new FakePrice(), new DefaultDpsEstimator(stats))
            .select(owned, profile, style, MAXED, AdviceMode.DPS);
    }

    @Test
    public void twoVariantProfilesOfOneTaskFlipTheMeleeWeaponByDefenceProfile()
    {
        // FR-4: two sibling variant profiles of one task with different defence flip the chosen melee
        // attack type, so a different weapon is selected - the engine reads the variant's profile.
        FakeStats stats = new FakeStats()
            .put(401, melee(EquipmentSlot.WEAPON, false, 80, 0, 0, 10))  // stab weapon
            .put(402, melee(EquipmentSlot.WEAPON, false, 0, 0, 80, 10)); // crush weapon

        MonsterProfile stabWeak = MonsterProfile.fromTask(
            task(CombatStyle.MELEE, null, new MonsterDefence(1, 0, 99, 99, 0, 0))); // stab lowest
        MonsterProfile crushWeak = MonsterProfile.fromTask(
            task(CombatStyle.MELEE, null, new MonsterDefence(1, 99, 99, 0, 0, 0))); // crush lowest

        assertEquals("stab-weak profile picks the stab weapon", Integer.valueOf(401),
            selectProfile(stats, stabWeak, CombatStyle.MELEE, owned(401, 402)).get(EquipmentSlot.WEAPON));
        assertEquals("crush-weak profile picks the crush weapon", Integer.valueOf(402),
            selectProfile(stats, crushWeak, CombatStyle.MELEE, owned(401, 402)).get(EquipmentSlot.WEAPON));
    }

    @Test
    public void forcingTheMethodReDrivesBestInStyleGear()
    {
        // MV-B9 / ADR-0013: the user-selected method OVERRIDES the recommended style. One profile,
        // owned a melee weapon AND a ranged weapon+ammo; forcing MELEE picks the melee weapon, forcing
        // RANGED picks the ranged weapon - the same profile, different method.
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 80, 0, 0, 10))   // melee weapon
            .put(110, ranged(EquipmentSlot.WEAPON, true, 80, 10))         // ranged 2h weapon
            .put(111, ranged(EquipmentSlot.AMMO, false, 0, 30));          // ammo
        MonsterProfile profile = MonsterProfile.fromTask(
            task(CombatStyle.MELEE, null, new MonsterDefence(1, 1, 1, 1, 0, 0)));
        OwnedItems owned = owned(100, 110, 111);

        assertEquals("forcing MELEE selects the melee weapon", Integer.valueOf(100),
            selectProfile(stats, profile, CombatStyle.MELEE, owned).get(EquipmentSlot.WEAPON));
        assertEquals("forcing RANGED selects the ranged weapon", Integer.valueOf(110),
            selectProfile(stats, profile, CombatStyle.RANGED, owned).get(EquipmentSlot.WEAPON));
    }

    @Test
    public void magicMethodIsEmptyWhenNoSpellIsCastable()
    {
        // MV-B9: a method that yields no viable loadout returns empty worn - the signal the UI uses to
        // disable that method (ADR-0013 decision 1). The realistic per-method case is MAGIC with the
        // magic level too low to cast even the lowest spell and no powered staff: MagicWeaponEvaluator
        // gives baseMaxHit 0 -> zero magic DPS -> no viable weapon -> empty.
        FakeStats stats = new FakeStats()
            .put(100, melee(EquipmentSlot.WEAPON, false, 80, 0, 0, 10)); // a plain (non-staff) weapon
        MonsterProfile profile = MonsterProfile.fromTask(
            task(CombatStyle.MELEE, null, new MonsterDefence(1, 1, 1, 1, 0, 0)));
        PlayerStats lowMagic = new PlayerStats(99, 99, 99, 99, 1, 99); // magic 1 -> no castable spell

        Map<EquipmentSlot, Integer> worn = new GearSelector(stats, new FakePrice(),
            new DefaultDpsEstimator(stats))
            .select(owned(100), profile, CombatStyle.MAGIC, lowMagic, AdviceMode.DPS);
        assertTrue("MAGIC with no castable spell -> empty worn (UI disables the method)", worn.isEmpty());
    }

    @Test
    public void categoryBonusFollowsTheVariantProfile()
    {
        // FR-5: a demon variant profile credits demonbane while a non-demon sibling profile under the
        // same task does not - the bonus follows the profile's flag, not the task.
        FakeStats stats = new FakeStats()
            .put(net.runelite.api.ItemID.ARCLIGHT, meleeWeapon(false, 0, 70, 0, 65, 4))
            .put(net.runelite.api.ItemID.DRAGON_SCIMITAR, meleeWeapon(false, 0, 75, 0, 70, 4));
        int[] ids = {net.runelite.api.ItemID.ARCLIGHT, net.runelite.api.ItemID.DRAGON_SCIMITAR};
        MonsterDefence def = new MonsterDefence(1, 1, 1, 1, 1, 1);

        MonsterVariant demonV = new MonsterVariant();
        demonV.setWeakness(new Weakness(CombatStyle.MELEE, null));
        demonV.setMonsterDefence(def);
        demonV.setDemon(true);
        MonsterVariant plainV = new MonsterVariant();
        plainV.setWeakness(new Weakness(CombatStyle.MELEE, null));
        plainV.setMonsterDefence(def);

        TaskData parent = task(CombatStyle.MELEE, null, def);
        assertEquals("demon variant credits demonbane", Integer.valueOf(net.runelite.api.ItemID.ARCLIGHT),
            selectProfile(stats, MonsterProfile.fromVariant(parent, demonV), CombatStyle.MELEE, owned(ids))
                .get(EquipmentSlot.WEAPON));
        assertEquals("non-demon sibling does not", Integer.valueOf(net.runelite.api.ItemID.DRAGON_SCIMITAR),
            selectProfile(stats, MonsterProfile.fromVariant(parent, plainV), CombatStyle.MELEE, owned(ids))
                .get(EquipmentSlot.WEAPON));
    }

    // --- strategy-guide weapon override (MV-S2, ADR-0015) --------------------------------------

    // Synthetic, NON-registry ids so these isolate the override MECHANISM (no demonbane / WeaponEffect
    // noise): a strategy weapon with modest stats vs a higher-DPS stat-best weapon.
    private static final int STRAT_WEAPON = 90001;
    private static final int BETTER_WEAPON = 90002;

    @Test
    public void strategyWeaponOverridesTheHigherDpsPickWhenOwned()
    {
        // FR-S1: the strategy weapon wins the WEAPON slot, bypassing the DPS ranking, even though the
        // player owns a higher-raw-DPS weapon. Proven relative to the no-override baseline.
        FakeStats stats = new FakeStats()
            .put(STRAT_WEAPON, meleeWeapon(false, 50, 0, 0, 50, 5))    // modest
            .put(BETTER_WEAPON, meleeWeapon(false, 95, 0, 0, 95, 4));  // higher stats + faster -> higher DPS
        MonsterProfile profile = MonsterProfile.fromTask(meleeTask(10, 10, 10));
        OwnedItems owned = owned(STRAT_WEAPON, BETTER_WEAPON);
        GearSelector selector = new GearSelector(stats, new FakePrice(), new DefaultDpsEstimator(stats));

        assertEquals("baseline: the higher-DPS weapon wins with no override", Integer.valueOf(BETTER_WEAPON),
            selector.select(owned, profile, CombatStyle.MELEE, MAXED, AdviceMode.DPS, false,
                Collections.emptyList()).get(EquipmentSlot.WEAPON));

        assertEquals("the strategy weapon overrides the DPS pick", Integer.valueOf(STRAT_WEAPON),
            selector.select(owned, profile, CombatStyle.MELEE, MAXED, AdviceMode.DPS, false,
                Collections.singletonList(STRAT_WEAPON)).get(EquipmentSlot.WEAPON));
    }

    @Test
    public void strategyOverrideFallsBackToStatPickWhenWeaponNotOwned()
    {
        // FR-S2 (bank-aware): the strategy weapon is NOT owned -> stat-driven best owned weapon, never
        // an unowned recommendation.
        FakeStats stats = new FakeStats().put(BETTER_WEAPON, meleeWeapon(false, 95, 0, 0, 95, 4));
        MonsterProfile profile = MonsterProfile.fromTask(meleeTask(10, 10, 10));
        GearSelector selector = new GearSelector(stats, new FakePrice(), new DefaultDpsEstimator(stats));

        assertEquals("unowned strategy weapon -> stat pick", Integer.valueOf(BETTER_WEAPON),
            selector.select(owned(BETTER_WEAPON), profile, CombatStyle.MELEE, MAXED, AdviceMode.DPS, false,
                Collections.singletonList(STRAT_WEAPON)).get(EquipmentSlot.WEAPON));
    }

    @Test
    public void strategyOverrideWalksPriorityOrderToFirstOwnedWeapon()
    {
        // Priority order: the highest-priority OWNED-and-viable id wins; an unowned higher-priority id
        // is skipped (bank-aware priority walk).
        FakeStats stats = new FakeStats().put(BETTER_WEAPON, meleeWeapon(false, 60, 0, 0, 60, 5));
        MonsterProfile profile = MonsterProfile.fromTask(meleeTask(10, 10, 10));
        GearSelector selector = new GearSelector(stats, new FakePrice(), new DefaultDpsEstimator(stats));

        assertEquals("falls to the next owned priority weapon", Integer.valueOf(BETTER_WEAPON),
            selector.select(owned(BETTER_WEAPON), profile, CombatStyle.MELEE, MAXED, AdviceMode.DPS, false,
                Arrays.asList(STRAT_WEAPON, BETTER_WEAPON)).get(EquipmentSlot.WEAPON));
    }

    @Test
    public void strategyOverrideRespectsTwoHandAndShieldInterplay()
    {
        int twoHandStrat = 90003;
        int oneHandStrat = 90004;
        int shield = 90005;
        FakeStats stats = new FakeStats()
            .put(twoHandStrat, meleeWeapon(true, 70, 0, 0, 70, 5))
            .put(oneHandStrat, meleeWeapon(false, 60, 0, 0, 60, 5))
            .put(shield, melee(EquipmentSlot.SHIELD, false, 0, 0, 0, 20));
        MonsterProfile profile = MonsterProfile.fromTask(meleeTask(10, 10, 10));
        GearSelector selector = new GearSelector(stats, new FakePrice(), new DefaultDpsEstimator(stats));
        OwnedItems owned = owned(twoHandStrat, oneHandStrat, shield);

        Map<EquipmentSlot, Integer> twoH = selector.select(owned, profile, CombatStyle.MELEE, MAXED,
            AdviceMode.DPS, false, Collections.singletonList(twoHandStrat));
        assertEquals(Integer.valueOf(twoHandStrat), twoH.get(EquipmentSlot.WEAPON));
        assertFalse("a 2h strategy weapon drops the shield", twoH.containsKey(EquipmentSlot.SHIELD));

        Map<EquipmentSlot, Integer> oneH = selector.select(owned, profile, CombatStyle.MELEE, MAXED,
            AdviceMode.DPS, false, Collections.singletonList(oneHandStrat));
        assertEquals(Integer.valueOf(oneHandStrat), oneH.get(EquipmentSlot.WEAPON));
        assertEquals("a 1h strategy weapon keeps the stat-picked best shield", Integer.valueOf(shield),
            oneH.get(EquipmentSlot.SHIELD));
    }

    @Test
    public void emptyStrategyListIsByteIdenticalToTheNoOverloadPath()
    {
        // FR-S4: an empty override list is exactly today's path (the regression anchor for every
        // non-strategy variant).
        FakeStats stats = new FakeStats()
            .put(STRAT_WEAPON, meleeWeapon(false, 50, 0, 0, 50, 5))
            .put(BETTER_WEAPON, meleeWeapon(false, 95, 0, 0, 95, 4));
        MonsterProfile profile = MonsterProfile.fromTask(meleeTask(10, 10, 10));
        OwnedItems owned = owned(STRAT_WEAPON, BETTER_WEAPON);
        GearSelector selector = new GearSelector(stats, new FakePrice(), new DefaultDpsEstimator(stats));

        assertEquals("empty override == the 6-arg overload's pick",
            selector.select(owned, profile, CombatStyle.MELEE, MAXED, AdviceMode.DPS, false),
            selector.select(owned, profile, CombatStyle.MELEE, MAXED, AdviceMode.DPS, false,
                Collections.emptyList()));
    }

    // --- NFR-1 performance guard (plan section 11) ---------------------------------------------

    /**
     * NFR-1: gear selection must complete well within budget over a large owned set once the stat
     * cache is warm. The fake {@link EquipmentStatsProvider} models a warm cache - O(1) HashMap
     * lookups, no I/O - exactly the state {@code DefaultEquipmentStatsProvider} reaches once its
     * per-id cache is populated, so this isolates the selector's own work.
     *
     * <p>We time the BEST (minimum) wall time across several timed selections after a warm-up. The
     * minimum is the most stable statistic on a loaded CI box: GC pauses and scheduler jitter only
     * ever ADD time, so the minimum tracks the true cost and never flakes upward. The asserted
     * ceiling (250ms) is ~25x the NFR-1 target of &lt;10ms warm - a coarse regression guard that
     * trips only on an order-of-magnitude regression (an accidental O(n^2) scan, per-item I/O, or a
     * lost cache), not on transient noise.
     */
    @Test
    public void selectionOverTwoThousandOwnedIdsStaysWithinTheWarmCacheBudget()
    {
        final int idCount = 2000;
        FakeStats stats = new FakeStats();
        int[] ids = new int[idCount];
        for (int i = 0; i < idCount; i++)
        {
            int id = 1000 + i;
            ids[i] = id;
            EquipmentSlot slot = SLOTS[i % SLOTS.length];
            boolean twoHanded = slot == EquipmentSlot.WEAPON && (i % 2 == 0);
            // Vary the bonuses (co-prime moduli) so scores differ and every slot has many real
            // candidates - the selector does genuine grouping/sorting work, not a degenerate pass.
            stats.put(id, melee(slot, twoHanded, i % 97, i % 89, i % 83, i % 79 + 1));
        }
        OwnedItems owned = owned(ids);
        TaskData task = meleeTask(40, 30, 20); // crush lowest -> a real attack-type decision
        MonsterProfile profile = MonsterProfile.fromTask(task);
        CombatStyle style = styleOf(task);
        GearSelector selector = new GearSelector(stats, new FakePrice(), new DefaultDpsEstimator(stats));

        // Warm up the JIT and confirm the selection actually produces a loadout (a real workload).
        Map<EquipmentSlot, Integer> warm = selector.select(owned, profile, style, MAXED, AdviceMode.DPS);
        assertTrue("benchmark must exercise a real selection (weapon filled)",
            warm.containsKey(EquipmentSlot.WEAPON));
        for (int i = 0; i < 20; i++)
        {
            selector.select(owned, profile, style, MAXED, AdviceMode.DPS);
        }

        long bestNanos = Long.MAX_VALUE;
        for (int run = 0; run < 20; run++)
        {
            long start = System.nanoTime();
            selector.select(owned, profile, style, MAXED, AdviceMode.DPS);
            bestNanos = Math.min(bestNanos, System.nanoTime() - start);
        }

        long bestMillis = bestNanos / 1_000_000L;
        assertTrue("warm 2000-id selection should be well under budget but took " + bestMillis
            + "ms (best of 20)", bestMillis < 250);
    }

    // --- helpers -------------------------------------------------------------------------------

    private static Map<EquipmentSlot, Integer> select(EquipmentStatsProvider stats, PriceService price,
        TaskData task, OwnedItems owned, AdviceMode mode)
    {
        // MV-B3: the selector now consumes a MonsterProfile + the effective method style. The default
        // method == the recommended (weakness) style, so this migration is FR-6 byte-identical.
        return new GearSelector(stats, price, new DefaultDpsEstimator(stats))
            .select(owned, MonsterProfile.fromTask(task), styleOf(task), MAXED, mode);
    }

    private static CombatStyle styleOf(TaskData t)
    {
        return t.getWeakness() == null ? null : t.getWeakness().getStyle();
    }

    private static Bonuses melee(EquipmentSlot slot, boolean twoH, int astab, int aslash, int acrush,
        int meleeStr)
    {
        return new Bonuses(astab, aslash, acrush, 0, 0, meleeStr, 0, 0, 4, slot, twoH);
    }

    private static Bonuses ranged(EquipmentSlot slot, boolean twoH, int arange, int rstr)
    {
        return new Bonuses(0, 0, 0, 0, arange, 0, rstr, 0, 4, slot, twoH);
    }

    /** A WEAPON-slot melee item with an explicit attack speed (ticks) for DPS ranking. */
    private static Bonuses meleeWeapon(boolean twoH, int astab, int aslash, int acrush, int meleeStr,
        int speed)
    {
        return new Bonuses(astab, aslash, acrush, 0, 0, meleeStr, 0, 0, speed, EquipmentSlot.WEAPON, twoH);
    }

    /** A WEAPON-slot ranged item with an explicit attack speed (ticks) for DPS ranking. */
    private static Bonuses rangedWeapon(boolean twoH, int arange, int rstr, int speed)
    {
        return new Bonuses(0, 0, 0, 0, arange, 0, rstr, 0, speed, EquipmentSlot.WEAPON, twoH);
    }

    private static Bonuses magic(EquipmentSlot slot, boolean twoH, int amagic, int mdmg)
    {
        return new Bonuses(0, 0, 0, amagic, 0, 0, 0, mdmg, 4, slot, twoH);
    }

    private static TaskData meleeTask(int stab, int slash, int crush)
    {
        return task(CombatStyle.MELEE, null, new MonsterDefence(1, stab, slash, crush, 0, 0));
    }

    private static TaskData rangedTask()
    {
        return task(CombatStyle.RANGED, null, new MonsterDefence(1, 0, 0, 0, 0, 0));
    }

    private static TaskData magicTask(String element)
    {
        return task(CombatStyle.MAGIC, element, new MonsterDefence(1, 0, 0, 0, 0, 0));
    }

    private static TaskData task(CombatStyle style, String element, MonsterDefence def)
    {
        TaskData t = new TaskData();
        t.setWeakness(new Weakness(style, element));
        t.setMonsterDefence(def);
        return t;
    }

    private static OwnedItems owned(int... ids)
    {
        Map<Integer, Integer> m = new HashMap<>();
        for (int id : ids)
        {
            m.put(id, 1);
        }
        return OwnedItems.fromCounts(m);
    }

    private static final class FakeStats implements EquipmentStatsProvider
    {
        private final Map<Integer, Bonuses> map = new HashMap<>();

        FakeStats put(int id, Bonuses b)
        {
            map.put(id, b);
            return this;
        }

        @Override
        public Bonuses get(int itemId)
        {
            return map.get(itemId); // null for any unregistered id (async-not-loaded / non-equipable)
        }
    }

    private static final class FakePrice implements PriceService
    {
        private final Map<Integer, Integer> map = new HashMap<>();

        FakePrice put(int id, int p)
        {
            map.put(id, p);
            return this;
        }

        @Override
        public int price(int itemId)
        {
            return map.getOrDefault(itemId, 0);
        }
    }
}

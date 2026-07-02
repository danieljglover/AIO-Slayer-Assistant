package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.LocationQuality;
import com.danieljglover.allinslayer.model.MasterData;
import com.danieljglover.allinslayer.model.MasterEconomy;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.Weakness;
import com.danieljglover.allinslayer.data.SlayerDataService;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LoadoutAdvisorTest
{
    private static final int MELEE_WEAPON = 1000;
    private static final int SHARK = 385;

    /** WEAPON-slot melee weapon (positive melee score). */
    private static Bonuses meleeWeapon()
    {
        return new Bonuses(70, 70, 70, 0, 0, 80, 0, 0, 4, EquipmentSlot.WEAPON, false);
    }

    private TaskData meleeTask()
    {
        TaskData t = new TaskData();
        t.setWeakness(new Weakness(CombatStyle.MELEE, null));
        t.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        t.setLocations(Collections.singletonList(
            new SlayerLocation("Catacombs", true, true, false, true)));
        return t;
    }

    /** A WA-12/13 seam fake: the given point balance (null = unknown) + owned rewardIds. */
    private static SlayerUnlockStateProvider unlockState(Integer points, String... rewardIds)
    {
        Set<String> owned = new java.util.HashSet<>(Arrays.asList(rewardIds));
        return new SlayerUnlockStateProvider()
        {
            @Override
            public boolean ownsUnlock(String rewardOrUnlockId)
            {
                return owned.contains(rewardOrUnlockId);
            }

            @Override
            public java.util.OptionalInt rewardPoints()
            {
                return points == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(points);
            }

            @Override
            public Optional<String> currentMaster()
            {
                return Optional.empty();
            }
        };
    }

    /** Seam fake owning exactly the given rewardIds, unknown balance (the honest default). */
    private static SlayerUnlockStateProvider owningUnlocks(String... rewardIds)
    {
        return unlockState(null, rewardIds);
    }

    /** Real GearSelector + ConsumableSelector over fakes; a fake estimator keeps DPS math out of scope. */
    private LoadoutAdvisor advisor(EquipmentStatsProvider stats, PriceService prices,
        ConsumableEffectsProvider fx)
    {
        return advisor(stats, prices, fx, owningUnlocks());
    }

    private LoadoutAdvisor advisor(EquipmentStatsProvider stats, PriceService prices,
        ConsumableEffectsProvider fx, SlayerUnlockStateProvider unlockState)
    {
        return advisor(stats, prices, fx, unlockState, new SlayerDataService(new Gson()));
    }

    private LoadoutAdvisor advisor(EquipmentStatsProvider stats, PriceService prices,
        ConsumableEffectsProvider fx, SlayerUnlockStateProvider unlockState,
        SlayerDataService dataService)
    {
        DpsEstimator est = new DpsEstimator()
        {
            @Override
            public double estimate(com.danieljglover.allinslayer.model.CombatStyle style,
                java.util.Map<EquipmentSlot, Integer> equipped, PlayerStats s,
                MonsterProfile profile, int spellBaseMaxHit)
            {
                return equipped.containsKey(EquipmentSlot.WEAPON) ? 10.0 : 0.0;
            }

            @Override
            public double weaponDps(Bonuses weapon, PlayerStats player,
                com.danieljglover.allinslayer.model.MonsterDefence def,
                com.danieljglover.allinslayer.model.CombatStyle style, WeaponEffect effect,
                double accMult, double dmgMult)
            {
                return weapon == null ? 0.0 : 1.0;
            }

            @Override
            public double magicWeaponDps(PlayerStats player,
                com.danieljglover.allinslayer.model.MonsterDefence def, int baseMaxHit, int speedTicks,
                int matt, int mdmg, double accMult, double dmgMult)
            {
                return baseMaxHit <= 0 ? 0.0 : 1.0;
            }
        };
        return new LoadoutAdvisor(new GearSelector(stats, prices, est), new ConsumableSelector(fx),
            est, prices, unlockState, dataService);
    }

    private PlayerStats stats()
    {
        return new PlayerStats(99, 99, 99, 99, 99, 99);
    }

    @Test
    public void recommendProducesWornFromGearSelectorAndConsumablesForMeleeTask()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider statsProvider = table::get; // SHARK -> null (not equipable)

        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);
        counts.put(SHARK, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);

        Optional<Recommendation> rec = advisor(statsProvider, id -> 1000, food(SHARK, 20)).recommend(
            meleeTask(), owned, stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertEquals(CombatStyle.MELEE, rec.get().getStyle());
        assertEquals(Integer.valueOf(MELEE_WEAPON), rec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertNotNull("consumables wired from ConsumableSelector", rec.get().getConsumables());
        assertEquals(Integer.valueOf(SHARK), rec.get().getConsumables().getFoodId());
        assertTrue("estimator fed the produced worn map", rec.get().getEstimatedDps() > 0);
    }

    @Test
    public void emptyWhenStyleHasNoOwnedWeapon()
    {
        EquipmentStatsProvider statsProvider = id -> null; // nothing equipable owned

        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(SHARK, 1); // only food owned
        OwnedItems owned = OwnedItems.fromCounts(counts);

        Optional<Recommendation> rec = advisor(statsProvider, id -> 1000, food(SHARK, 20)).recommend(
            meleeTask(), owned, stats(), AdviceMode.DPS, false);

        assertTrue("no weapon for the weakness style -> no viable loadout", !rec.isPresent());
    }

    @Test
    public void nullTaskYieldsEmpty()
    {
        EquipmentStatsProvider statsProvider = id -> null;
        Optional<Recommendation> rec = advisor(statsProvider, id -> 1000, food(SHARK, 20)).recommend(
            null, OwnedItems.fromCounts(new HashMap<>()), stats(), AdviceMode.DPS, false);
        assertTrue(!rec.isPresent());
    }

    @Test
    public void locationOverrideKeepsRecommendedLocationSeparateFromSelectedLocation()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider statsProvider = table::get;

        TaskData task = meleeTask();
        task.setSlayerLevel(85);
        task.setQuestReqs(Collections.singletonList("Priest in Peril"));
        task.setLocations(Arrays.asList(
            new SlayerLocation("Catacombs", true, false, true, true),
            new SlayerLocation("Stronghold Slayer Cave", true, true, false, true),
            new SlayerLocation("Slayer Tower", false, false, false, true)));

        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);

        Optional<Recommendation> rec = advisor(statsProvider, id -> 1000, food(SHARK, 20)).recommend(
            task, owned, new PlayerStats(99, 99, 99, 99, 99, 92), AdviceMode.DPS, true, "Slayer Tower");

        assertTrue(rec.isPresent());
        assertEquals("Slayer Tower", rec.get().getLocation().getName());
        assertNotNull(rec.get().getRecommendedLocation());
        assertEquals("Stronghold Slayer Cave", rec.get().getRecommendedLocation().getName());
        assertTrue(rec.get().getLocationReason().contains("Gear: MELEE"));
        assertTrue(rec.get().getLocationReason().contains("Slayer: 92/85"));
        assertTrue(rec.get().getLocationReason().contains("Unlocks: Priest in Peril"));
    }

    // --- variant resolution (MV-B5, FR-4/FR-5/FR-6) --------------------------------------------

    private static final int RANGED_WEAPON = 1001;

    private static Bonuses rangedWeapon()
    {
        return new Bonuses(0, 0, 0, 0, 70, 0, 80, 0, 4, EquipmentSlot.WEAPON, true);
    }

    /** Advisor over a REAL DefaultDpsEstimator so the effective style actually differentiates weapons. */
    private LoadoutAdvisor advisorReal(EquipmentStatsProvider stats, PriceService prices,
        ConsumableEffectsProvider fx)
    {
        DpsEstimator est = new DefaultDpsEstimator(stats);
        return new LoadoutAdvisor(new GearSelector(stats, prices, est), new ConsumableSelector(fx),
            est, prices, owningUnlocks(), new SlayerDataService(new Gson()));
    }

    // ---- WA-12: unlock-gated gear guard (ADR-0018 #9b, NG-4 note-only) -------------------------

    private static final int SLAYER_HELMET = 11864;

    /** HEAD-slot armour with a positive melee score so GearSelector picks it. */
    private static Bonuses slayerHelmBonuses()
    {
        return new Bonuses(5, 5, 5, 0, 0, 3, 0, 0, 0, EquipmentSlot.HEAD, false);
    }

    @Test
    public void recommendingASlayerHelmWithoutItsUnlockSetsTheGuardNote()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        table.put(SLAYER_HELMET, slayerHelmBonuses());
        EquipmentStatsProvider statsProvider = table::get;

        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);
        counts.put(SLAYER_HELMET, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);

        Optional<Recommendation> rec = advisor(statsProvider, id -> 1000, food(SHARK, 20),
            owningUnlocks()).recommend(meleeTask(), owned, stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertEquals("the helm made the loadout (guard is a note, NEVER a gear change - NG-4)",
            Integer.valueOf(SLAYER_HELMET), rec.get().getWorn().get(EquipmentSlot.HEAD));
        assertNotNull(rec.get().getUnlockGuardNote());
        assertTrue(rec.get().getUnlockGuardNote().contains("Malevolent masquerade"));
    }

    @Test
    public void owningTheUnlockLeavesNoGuardNote()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        table.put(SLAYER_HELMET, slayerHelmBonuses());
        EquipmentStatsProvider statsProvider = table::get;

        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);
        counts.put(SLAYER_HELMET, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);

        Optional<Recommendation> rec = advisor(statsProvider, id -> 1000, food(SHARK, 20),
            owningUnlocks("malevolent-masquerade")).recommend(
            meleeTask(), owned, stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertEquals(Integer.valueOf(SLAYER_HELMET), rec.get().getWorn().get(EquipmentSlot.HEAD));
        assertNull("owned unlock -> no note", rec.get().getUnlockGuardNote());
    }

    // ---- WB-2 (D6): the advisor candidate set shares the panel's lenient location match ---------

    @Test
    public void advisorCandidateSetMatchesDriftedLocationNamesLikeThePanelDoes()
    {
        // D6: the panel dropdown matched locationNames trimmed + case-insensitively while the
        // advisor required exact equals, so a drifted row filtered the dropdown but silently fell
        // back to ALL locations in the advisor. One shared rule (MonsterVariant.locationNameMatches)
        // must resolve this variant's " slayer tower " row to the task's "Slayer Tower" here too.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);

        TaskData task = meleeTask();
        task.setLocations(Arrays.asList(
            new SlayerLocation("Catacombs", true, true, false, true),
            new SlayerLocation("Slayer Tower", false, false, false, true)));
        MonsterVariant base = variant("Base", CombatStyle.MELEE, true, false);
        base.setLocationNames(Arrays.asList(" slayer tower "));
        task.setVariants(Collections.singletonList(base));

        Optional<Recommendation> rec = advisor(table::get, id -> 1000, food(SHARK, 20)).recommend(
            task, OwnedItems.fromCounts(counts), stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertEquals("the drifted locationNames row resolves to the variant's linked location, "
                + "not the fall-back-to-all first location",
            "Slayer Tower", rec.get().getRecommendedLocation().getName());
    }

    // ---- WA-13: "what to unlock next" hint (ADR-0018 #9d) --------------------------------------

    @Test
    public void unlockHintIsComposedFromTheLiveCatalogueAndTheSeam()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);

        SlayerDataService service = new SlayerDataService(new Gson());
        service.load(); // the real generated catalogue (17 rewards, WA-8)

        Optional<Recommendation> rec = advisor(table::get, id -> 1000, food(SHARK, 20),
            unlockState(60), service).recommend(
            meleeTask(), OwnedItems.fromCounts(counts), stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertNotNull(rec.get().getUnlockHint());
        assertTrue("60 points affords Bigger and Badder (50) from the live catalogue",
            rec.get().getUnlockHint().contains("Bigger and Badder"));
    }

    @Test
    public void noUnlockHintWhenThePointBalanceIsUnknown()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);

        SlayerDataService service = new SlayerDataService(new Gson());
        service.load();

        Optional<Recommendation> rec = advisor(table::get, id -> 1000, food(SHARK, 20),
            unlockState(null), service).recommend(
            meleeTask(), OwnedItems.fromCounts(counts), stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertNull("unknown balance -> no hint, never a guess", rec.get().getUnlockHint());
    }

    @Test
    public void siblingVariantsOfOneTaskReDriveDifferentRecommendations()
    {
        // FR-4: two variants of one task with different recommended styles re-drive a different weapon.
        // The base variant is MELEE, a sibling is RANGED; the player owns both weapons.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        table.put(RANGED_WEAPON, rangedWeapon());
        EquipmentStatsProvider statsProvider = table::get;

        TaskData task = meleeTask();
        MonsterVariant base = variant("Base", CombatStyle.MELEE, true, false);
        MonsterVariant ranged = variant("Aerial form", CombatStyle.RANGED, false, false);
        task.setVariants(Arrays.asList(base, ranged));

        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);
        counts.put(RANGED_WEAPON, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);
        LoadoutAdvisor advisor = advisorReal(statsProvider, id -> 1000, food(SHARK, 20));

        Optional<Recommendation> baseRec = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, false, null, "Base", null);
        Optional<Recommendation> rangedRec = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, false, null, "Aerial form", null);

        assertEquals(CombatStyle.MELEE, baseRec.get().getStyle());
        assertEquals(Integer.valueOf(MELEE_WEAPON), baseRec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertEquals(CombatStyle.RANGED, rangedRec.get().getStyle());
        assertEquals(Integer.valueOf(RANGED_WEAPON), rangedRec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertEquals("resolved variant name carried for the card", "Aerial form",
            rangedRec.get().getVariantName());
    }

    @Test
    public void defaultVariantSelectedWhenNoNameGivenAndBossNoteCarried()
    {
        // FR-6: a fresh task (no selection) resolves to the isDefault variant. FR-7: a boss variant
        // carries the boss flag + its location/requirement note onto the recommendation.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider statsProvider = table::get;

        TaskData task = meleeTask();
        MonsterVariant base = variant("Greater demon", CombatStyle.MELEE, true, false);
        MonsterVariant boss = variant("K'ril Tsutsaroth", CombatStyle.MELEE, false, true);
        boss.setLocation("God Wars Dungeon");
        boss.setRequirement("boss - separate trip");
        task.setVariants(Arrays.asList(base, boss));

        OwnedItems owned = OwnedItems.fromCounts(Collections.singletonMap(MELEE_WEAPON, 1));
        LoadoutAdvisor advisor = advisor(statsProvider, id -> 1000, food(SHARK, 20));

        Optional<Recommendation> def = advisor.recommend(task, owned, stats(), AdviceMode.DPS, false);
        assertEquals("default resolves to the isDefault variant", "Greater demon",
            def.get().getVariantName());
        assertTrue("base variant is not a boss", !def.get().isBoss());

        Optional<Recommendation> bossRec = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, false, null, "K'ril Tsutsaroth", null);
        assertTrue("boss variant flagged for the FR-7 note", bossRec.get().isBoss());
        assertEquals("God Wars Dungeon", bossRec.get().getVariantLocation());
        assertEquals("boss - separate trip", bossRec.get().getVariantRequirement());
    }

    @Test
    public void selectedMethodOverridesRecommendedStyle()
    {
        // MV-B9 / ADR-0013: a no-variant task whose recommended style is MELEE re-drives to a ranged
        // weapon when the user picks the RANGED method. FR-6 default (method null) keeps MELEE.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        table.put(RANGED_WEAPON, rangedWeapon());
        EquipmentStatsProvider statsProvider = table::get;

        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);
        counts.put(RANGED_WEAPON, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);
        LoadoutAdvisor advisor = advisorReal(statsProvider, id -> 1000, food(SHARK, 20));

        Optional<Recommendation> def = advisor.recommend(meleeTask(), owned, stats(), AdviceMode.DPS, false);
        assertEquals("default method == recommended style (MELEE)", CombatStyle.MELEE, def.get().getStyle());

        Optional<Recommendation> ranged = advisor.recommend(
            meleeTask(), owned, stats(), AdviceMode.DPS, false, null, null, CombatStyle.RANGED);
        assertEquals("RANGED method overrides the recommended style", CombatStyle.RANGED,
            ranged.get().getStyle());
        assertEquals(Integer.valueOf(RANGED_WEAPON), ranged.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void advisorGearsForExactlyTheSharedResolverVariant()
    {
        // MV-FX / code-review S1: the advisor must drive the loadout from exactly the variant the
        // shared MonsterVariant.resolve picks, so it can never diverge from the panel's pre-selection.
        // Covers the default-rule and the all-boss-no-default (first listed) cases.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider statsProvider = table::get;
        OwnedItems owned = OwnedItems.fromCounts(Collections.singletonMap(MELEE_WEAPON, 1));
        LoadoutAdvisor advisor = advisor(statsProvider, id -> 1000, food(SHARK, 20));

        TaskData withDefault = meleeTask();
        withDefault.setVariants(Arrays.asList(
            variant("Greater demon", CombatStyle.MELEE, true, false),
            variant("K'ril Tsutsaroth", CombatStyle.MELEE, false, true)));
        Optional<Recommendation> def = advisor.recommend(withDefault, owned, stats(), AdviceMode.DPS, false);
        assertEquals("advisor gears for the shared resolver's default-rule variant",
            MonsterVariant.resolve(withDefault, null).getName(), def.get().getVariantName());

        TaskData allBoss = meleeTask();
        allBoss.setVariants(Arrays.asList(
            variant("Abyssal Sire", CombatStyle.MELEE, false, true),
            variant("K'ril Tsutsaroth", CombatStyle.MELEE, false, true)));
        Optional<Recommendation> boss = advisor.recommend(allBoss, owned, stats(), AdviceMode.DPS, false);
        assertEquals("advisor gears for the shared resolver's first-listed variant (all-boss task)",
            MonsterVariant.resolve(allBoss, null).getName(), boss.get().getVariantName());
    }

    // --- DT-B7 (ADR-0017 #3): variant-aware location suggestion --------------------------------

    @Test
    public void suggestedLocationIsDrawnFromTheVariantsLocationSubset()
    {
        // A linked variant's locationNames scope the candidate set and the suggestion re-ranks WITHIN
        // it; an unlinked variant (null locationNames) falls back to ALL task locations = byte-identical
        // to today. Same task, same haveCannon: the linked variant's subset excludes the cannon location
        // so its suggestion is the plain location; the unlinked variant still picks the cannon location.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(Collections.singletonMap(MELEE_WEAPON, 1));
        LoadoutAdvisor advisor = advisor(sp, id -> 1000, food(SHARK, 20));

        TaskData task = meleeTask();
        task.setLocations(Arrays.asList(
            new SlayerLocation("Catacombs", true, true, false, true),          // cannonable (today's pick)
            new SlayerLocation("Slayer Tower", false, false, false, true)));   // plain
        MonsterVariant linked = variant("Linked", CombatStyle.MELEE, true, false);
        linked.setLocationNames(Collections.singletonList("Slayer Tower"));    // subset excludes the cannon loc
        MonsterVariant unlinked = variant("Unlinked", CombatStyle.MELEE, false, false); // null -> fallback
        task.setVariants(Arrays.asList(linked, unlinked));

        // haveCannon=true: unlinked -> all task locations -> cannon location (today's byte-identical pick).
        Optional<Recommendation> unlinkedRec = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, true, null, "Unlinked", null);
        assertEquals("unlinked variant: today's pick over ALL task locations (cannonable)",
            "Catacombs", unlinkedRec.get().getRecommendedLocation().getName());

        // haveCannon=true: linked -> candidate set is only [Slayer Tower] (no cannon in the subset) -> first.
        Optional<Recommendation> linkedRec = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, true, null, "Linked", null);
        assertEquals("linked variant: suggestion drawn from the variant's locationNames subset",
            "Slayer Tower", linkedRec.get().getRecommendedLocation().getName());
    }

    // --- WD-5b (ADR-0020 #2): rank chooseLocation on the LocationQuality amount overlay ----------

    /** A plain (non-cannon, non-burst, non-wilderness) location carrying only a quality {@code amount}. */
    private static SlayerLocation ranked(String name, Integer amount)
    {
        LocationQuality q = amount == null ? null
            : new LocationQuality(amount, null, null, null, null);
        return new SlayerLocation(name, false, false, false, true, false, false, null, q);
    }

    @Test
    public void higherAmountQualityWinsTheFormerFirstTie()
    {
        // WD-5b: with the cannon/burst biases inapplicable (no cannon owned, melee style), the pick used
        // to be the authored-first candidate. The LocationQuality amount now breaks that tie: the
        // higher-amount candidate wins even though it is authored second.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(Collections.singletonMap(MELEE_WEAPON, 1));
        LoadoutAdvisor advisor = advisor(sp, id -> 1000, food(SHARK, 20));

        TaskData task = meleeTask();
        task.setLocations(Arrays.asList(
            ranked("Lower density", 30),   // authored first, today's "first" pick
            ranked("Higher density", 90))); // authored second, higher amount

        Optional<Recommendation> rec = advisor.recommend(task, owned, stats(), AdviceMode.DPS, false);
        assertEquals("higher-amount candidate wins the former authored-first tie",
            "Higher density", rec.get().getRecommendedLocation().getName());
    }

    @Test
    public void allUnknownQualityKeepsAuthoredOrder()
    {
        // FR-6: no candidate carries a quality overlay -> ranking degrades to authored order = today's
        // exact behaviour (the authored-first candidate).
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(Collections.singletonMap(MELEE_WEAPON, 1));
        LoadoutAdvisor advisor = advisor(sp, id -> 1000, food(SHARK, 20));

        TaskData task = meleeTask();
        task.setLocations(Arrays.asList(
            ranked("First authored", null),
            ranked("Second authored", null)));

        Optional<Recommendation> rec = advisor.recommend(task, owned, stats(), AdviceMode.DPS, false);
        assertEquals("no quality overlay -> authored order preserved (FR-6)",
            "First authored", rec.get().getRecommendedLocation().getName());
    }

    // --- WD-3 (ADR-0020 #1): note-only prayer/survival advisory from MonsterProfile.offence --------

    /** A single-weapon melee advisor + owner, so the loadout is viable and offence drives the note. */
    private Recommendation recWithOffence(MonsterOffence offence, int... ownedIds)
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(MELEE_WEAPON, 1);
        for (int id : ownedIds)
        {
            counts.put(id, 1);
        }
        OwnedItems owned = OwnedItems.fromCounts(counts);
        TaskData task = meleeTask();
        task.setOffence(offence);
        return advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false).get();
    }

    @Test
    public void meleeMonsterSurfacesProtectFromMeleeAndSurvivalLine()
    {
        MonsterOffence off = new MonsterOffence(
            255, 29, Collections.singletonList(AttackStyle.MELEE), 4, null, false, false);
        Recommendation rec = recWithOffence(off);
        assertNotNull("offence present -> survival note", rec.getSurvivalNote());
        assertTrue("prayer clause from attackStyles", rec.getSurvivalNote().contains("Protect from Melee"));
        assertTrue("survival clause from maxHit", rec.getSurvivalNote().contains("29"));
    }

    @Test
    public void multipleAttackStylesAreListedAndDragonfireDefersToAntifire()
    {
        MonsterOffence off = new MonsterOffence(255, null,
            Arrays.asList(AttackStyle.MAGIC, AttackStyle.DRAGONFIRE, AttackStyle.TYPELESS), 4, null,
            false, false);
        Recommendation rec = recWithOffence(off);
        assertNotNull(rec.getSurvivalNote());
        assertTrue("MAGIC surfaces a prayer", rec.getSurvivalNote().contains("Protect from Magic"));
        assertTrue("DRAGONFIRE/TYPELESS are not prayable overheads",
            !rec.getSurvivalNote().contains("Dragonfire") && !rec.getSurvivalNote().contains("Typeless"));
    }

    @Test
    public void venomousWithoutAntivenomNudges()
    {
        MonsterOffence off = new MonsterOffence(255, null, null, 4, null, false, true);
        Recommendation rec = recWithOffence(off); // owns no cure
        assertNotNull(rec.getSurvivalNote());
        assertTrue("venom nudge when no antivenom owned",
            rec.getSurvivalNote().toLowerCase().contains("antivenom"));
    }

    @Test
    public void venomousWithAntivenomOwnedSuppressesNudge()
    {
        MonsterOffence off = new MonsterOffence(255, null, null, 4, null, false, true);
        Recommendation rec = recWithOffence(off, net.runelite.api.ItemID.ANTIVENOM4);
        assertTrue("owns antivenom -> no venom nudge",
            rec.getSurvivalNote() == null || !rec.getSurvivalNote().toLowerCase().contains("antivenom"));
    }

    @Test
    public void noOffenceDataMeansNoSurvivalNote()
    {
        Recommendation rec = recWithOffence(null); // meleeTask default: offence null
        assertNull("offence == null -> no survival note (FR-6)", rec.getSurvivalNote());
    }

    // --- WD-12 (ADR-0020 #5): skip/block + Turael-skip advisor (config-declared disliked set) ------

    /** A SlayerDataService whose {@code masterById} returns one master with the given economy. */
    private static SlayerDataService dsWithMaster(String masterId, Integer blockCost, boolean zeroPoints)
    {
        MasterEconomy economy = new MasterEconomy();
        economy.setBlockCost(blockCost);
        economy.setZeroPoints(zeroPoints);
        MasterData master = new MasterData();
        master.setMasterId(masterId);
        master.setName(masterId);
        master.setEconomy(economy);
        return new SlayerDataService(new Gson())
        {
            @Override
            public Optional<MasterData> masterById(String query)
            {
                return masterId.equals(query) ? Optional.of(master) : Optional.empty();
            }
        };
    }

    /** Full 10-arg recommend for a disliked-task scenario; returns the skip/block note (may be null). */
    private String skipBlockNote(String taskName, Set<String> disliked, String selectedMaster,
        SlayerDataService ds, SlayerUnlockStateProvider us)
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(Collections.singletonMap(MELEE_WEAPON, 1));
        LoadoutAdvisor advisor = advisor(sp, id -> 1000, food(SHARK, 20), us, ds);
        TaskData task = meleeTask();
        task.setTask(taskName);
        return advisor.recommend(task, owned, stats(), AdviceMode.DPS, false, null, null, null,
            selectedMaster, disliked).get().getSkipBlockNote();
    }

    @Test
    public void dislikedTaskWithAffordableBlockSuggestsBlocking()
    {
        Set<String> disliked = new java.util.HashSet<>(Arrays.asList("bloodveld"));
        SlayerDataService ds = dsWithMaster("duradel", 100, false);
        String note = skipBlockNote("Bloodveld", disliked, "duradel", ds, unlockState(200));
        assertNotNull("disliked + affordable block -> note", note);
        assertTrue("suggests blocking with the cost", note.toLowerCase().contains("block"));
        assertTrue("states the block cost", note.contains("100"));
        assertTrue("states the player's balance", note.contains("200"));
    }

    @Test
    public void dislikedTaskWithUnknownPointsMakesNoCostClaimButOffersFreeSkip()
    {
        Set<String> disliked = new java.util.HashSet<>(Arrays.asList("bloodveld"));
        SlayerDataService ds = dsWithMaster("duradel", 100, false);
        String note = skipBlockNote("Bloodveld", disliked, "duradel", ds, unlockState(null));
        assertNotNull(note);
        assertTrue("offers the Turael free skip", note.toLowerCase().contains("turael"));
        assertTrue("no cost claim when the balance is unknown", !note.toLowerCase().contains("costs"));
    }

    @Test
    public void notDislikedTaskGetsNoSkipBlockNote()
    {
        SlayerDataService ds = dsWithMaster("duradel", 100, false);
        String note = skipBlockNote("Bloodveld", Collections.emptySet(), "duradel", ds, unlockState(200));
        assertNull("task not in the disliked set -> no note", note);
    }

    // --- DT-B8 (ADR-0017 #4 / PD-1): the SUGGESTED location's Wilderness flag credits gear ------

    private static final int VIGGORAS = net.runelite.api.ItemID.VIGGORAS_CHAINMACE;
    private static final int DRAGON_SCIMITAR = net.runelite.api.ItemID.DRAGON_SCIMITAR;

    @Test
    public void suggestedWildernessLocationCreditsWildernessGearByDefault()
    {
        // PD-1: the loadout now couples to the SUGGESTED-or-selected location, not only a user-selected
        // one. Viggora's (lower base) beats the Dragon scimitar (higher base) ONLY with the +50%
        // vs-Wilderness credit (same stat gap as GearSelectorTest). With nothing selected and the
        // suggested location a Wilderness one, Viggora's wins by DEFAULT - the one intended behaviour
        // change. Selecting the non-Wilderness location removes the credit and the higher-base wins.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(VIGGORAS, meleeW(60, 55, 4));
        table.put(DRAGON_SCIMITAR, meleeW(70, 65, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(VIGGORAS, DRAGON_SCIMITAR));
        LoadoutAdvisor advisor = advisorReal(sp, id -> 1000, food(SHARK, 20));

        TaskData task = meleeTask();
        task.setLocations(Arrays.asList(
            new SlayerLocation("Wilderness Slayer Cave", true, false, false, true, true), // wilderness
            new SlayerLocation("Catacombs", true, false, false, true)));                  // non-wilderness

        // Nothing selected -> suggested location is the (first) Wilderness one -> Viggora's credited.
        Optional<Recommendation> def = advisor.recommend(task, owned, stats(), AdviceMode.DPS, false);
        assertTrue("suggested location is the Wilderness one", def.get().getRecommendedLocation().isWilderness());
        assertEquals("suggested Wilderness location credits the Wilderness weapon by default (PD-1)",
            Integer.valueOf(VIGGORAS), def.get().getWorn().get(EquipmentSlot.WEAPON));

        // Selecting the non-Wilderness location removes the credit -> the higher-base weapon wins.
        Optional<Recommendation> picked = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, false, "Catacombs");
        assertEquals("selecting a non-Wilderness location removes the vs-Wilderness credit",
            Integer.valueOf(DRAGON_SCIMITAR), picked.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    // --- DT-B10 (ADR-0017 #4 / GAP-2): safespot method hint + accessNote from the effective location

    @Test
    public void safespotLocationSetsARangedMagicHintAndSurfacesAccessNote()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(RANGED_WEAPON, rangedWeapon());
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;

        SlayerLocation safespot = new SlayerLocation("Smoke Dungeon", false, false, false, false, false,
            true, "Requires Desert Treasure; bring a light source.");

        // safespot + effective style RANGED -> a method hint is set; the accessNote is surfaced.
        TaskData rangedTask = new TaskData();
        rangedTask.setWeakness(new Weakness(CombatStyle.RANGED, null));
        rangedTask.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        rangedTask.setLocations(Collections.singletonList(safespot));
        Optional<Recommendation> ranged = advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(rangedTask, OwnedItems.fromCounts(counts(RANGED_WEAPON)), stats(), AdviceMode.DPS, false);
        assertNotNull("safespot + ranged -> a method hint", ranged.get().getLocationHint());
        assertTrue("hint names the safespot", ranged.get().getLocationHint().toLowerCase().contains("safespot"));
        assertEquals("access note surfaced", "Requires Desert Treasure; bring a light source.",
            ranged.get().getAccessNote());

        // Same safespot location but a MELEE task -> no hint (safespotting is a ranged/magic tactic).
        TaskData meleeSafespot = new TaskData();
        meleeSafespot.setWeakness(new Weakness(CombatStyle.MELEE, null));
        meleeSafespot.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        meleeSafespot.setLocations(Collections.singletonList(safespot));
        Optional<Recommendation> melee = advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(meleeSafespot, OwnedItems.fromCounts(counts(MELEE_WEAPON)), stats(), AdviceMode.DPS, false);
        assertNull("safespot + melee -> no hint", melee.get().getLocationHint());
        assertEquals("access note surfaced regardless of style",
            "Requires Desert Treasure; bring a light source.", melee.get().getAccessNote());

        // A non-safespot location with no access note -> both null.
        Optional<Recommendation> plain = advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(meleeTask(), OwnedItems.fromCounts(counts(MELEE_WEAPON)), stats(), AdviceMode.DPS, false);
        assertNull("no safespot -> no hint", plain.get().getLocationHint());
        assertNull("no access note -> null", plain.get().getAccessNote());
    }

    // --- DT-B9 wiring: recommend() fills the inventory from InventorySelector -------------------

    @Test
    public void recommendPopulatesOwnedInventorySupplies()
    {
        // The formerly always-empty inventory is now filled owned-driven (DT-B9). A draconic task plus
        // an owned antifire -> the antifire is packed (the detailed rules are in InventorySelectorTest).
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(
            counts(MELEE_WEAPON, net.runelite.api.ItemID.SUPER_ANTIFIRE_POTION4));

        TaskData task = meleeTask();
        task.setDragon(true);
        Optional<Recommendation> rec = advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false);
        assertTrue("owned antifire packed for a draconic task",
            rec.get().getInventory().contains(net.runelite.api.ItemID.SUPER_ANTIFIRE_POTION4));
    }

    @Test
    public void recommendSetsAntifireNoteWhenDraconicAndNoneOwned()
    {
        // DT-B11 (PD-3): a draconic task with no antifire owned -> the advisor sets the unowned nudge on
        // the Recommendation; owning antifire clears it (the supply covers that case); a non-draconic
        // task never gets it. A note only (NG-4).
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(MELEE_WEAPON, meleeWeapon());
        EquipmentStatsProvider sp = table::get;

        TaskData dragon = meleeTask();
        dragon.setDragon(true);

        Optional<Recommendation> unowned = advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(dragon, OwnedItems.fromCounts(counts(MELEE_WEAPON)), stats(), AdviceMode.DPS, false);
        assertNotNull("draconic + no antifire owned -> a nudge note", unowned.get().getAntifireNote());

        Optional<Recommendation> owned = advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(dragon, OwnedItems.fromCounts(
                counts(MELEE_WEAPON, net.runelite.api.ItemID.SUPER_ANTIFIRE_POTION4)),
                stats(), AdviceMode.DPS, false);
        assertNull("owns antifire -> no nudge", owned.get().getAntifireNote());

        Optional<Recommendation> nonDragon = advisor(sp, id -> 1000, food(SHARK, 20))
            .recommend(meleeTask(), OwnedItems.fromCounts(counts(MELEE_WEAPON)), stats(), AdviceMode.DPS, false);
        assertNull("non-draconic -> no nudge", nonDragon.get().getAntifireNote());
    }

    // --- strategy-guide gear override (MV-S2, ADR-0015 + amended toggle rule) -------------------

    private static final int STRAT_MELEE = 90001;   // Emberlight stand-in (modest stats)
    private static final int BETTER_MELEE = 90002;  // Fang stand-in (higher raw DPS)
    private static final int STRAT_RANGED = 90003;  // Scorching bow stand-in
    private static final int BETTER_RANGED = 90004; // higher-DPS ranged weapon

    private static Bonuses meleeW(int atk, int str, int speed)
    {
        return new Bonuses(atk, atk, atk, 0, 0, str, 0, 0, speed, EquipmentSlot.WEAPON, false);
    }

    private static Bonuses rangedW(int arange, int rstr, int speed)
    {
        return new Bonuses(0, 0, 0, 0, arange, 0, rstr, 0, speed, EquipmentSlot.WEAPON, true);
    }

    private static Map<Integer, Integer> counts(int... ids)
    {
        Map<Integer, Integer> m = new HashMap<>();
        for (int id : ids)
        {
            m.put(id, 1);
        }
        return m;
    }

    /** Tormented-Demon-shaped strategy: MELEE primary (Emberlight) + RANGED secondary (Scorching bow). */
    private static com.danieljglover.allinslayer.model.MonsterStrategy demonStrategy()
    {
        com.danieljglover.allinslayer.model.MonsterStrategy s =
            new com.danieljglover.allinslayer.model.MonsterStrategy();
        s.setPrimaryStyle(CombatStyle.MELEE);
        s.setPrimaryWeapons(Collections.singletonList(
            new com.danieljglover.allinslayer.model.StrategyWeapon("Emberlight", STRAT_MELEE, null)));
        s.setSecondaryWeapons(Collections.singletonList(
            new com.danieljglover.allinslayer.model.StrategyWeapon(
                "Scorching bow", STRAT_RANGED, CombatStyle.RANGED)));
        return s;
    }

    private static MonsterVariant strategyVariant(com.danieljglover.allinslayer.model.MonsterStrategy s)
    {
        MonsterVariant v = variant("Tormented Demon", CombatStyle.MELEE, true, true);
        v.setDemon(true);
        v.setStrategy(s);
        return v;
    }

    @Test
    public void strategyOverridesEveryDocumentedStyle()
    {
        // FR-S1 + amended toggle rule: the strategy documents MELEE (Emberlight) AND RANGED (Scorching
        // bow). Method=Melee -> Emberlight (over the higher-DPS Fang); Method=Ranged -> Scorching bow
        // (over the higher-DPS ranged weapon). The override drives EVERY documented style.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(50, 50, 5));
        table.put(BETTER_MELEE, meleeW(95, 95, 4));
        table.put(STRAT_RANGED, rangedW(50, 50, 5));
        table.put(BETTER_RANGED, rangedW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(
            counts(STRAT_MELEE, BETTER_MELEE, STRAT_RANGED, BETTER_RANGED));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(strategyVariant(demonStrategy())));
        LoadoutAdvisor advisor = advisorReal(sp, id -> 1000, food(SHARK, 20));

        Optional<Recommendation> melee = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);
        assertEquals("melee method -> the strategy melee weapon", Integer.valueOf(STRAT_MELEE),
            melee.get().getWorn().get(EquipmentSlot.WEAPON));

        Optional<Recommendation> ranged = advisor.recommend(
            task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", CombatStyle.RANGED);
        assertEquals("ranged method -> the strategy ranged (secondary) weapon",
            Integer.valueOf(STRAT_RANGED), ranged.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void strategyOverrideFallsBackToStatPickWhenStrategyWeaponNotOwned()
    {
        // FR-S2: own the Fang (BETTER) but not the strategy weapon -> stat pick, no unowned recommend.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(BETTER_MELEE, meleeW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(BETTER_MELEE));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(strategyVariant(demonStrategy())));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);

        assertEquals("unowned strategy weapon -> stat-driven best owned weapon",
            Integer.valueOf(BETTER_MELEE), rec.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void methodToggledToAnUndocumentedStyleDisablesTheOverride()
    {
        // FR-S3 (amended): a MELEE-only strategy. Switching to RANGED (undocumented) disables the
        // override -> the stat engine drives ranged gear normally.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(50, 50, 5));
        table.put(BETTER_RANGED, rangedW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(STRAT_MELEE, BETTER_RANGED));

        com.danieljglover.allinslayer.model.MonsterStrategy meleeOnly =
            new com.danieljglover.allinslayer.model.MonsterStrategy();
        meleeOnly.setPrimaryStyle(CombatStyle.MELEE);
        meleeOnly.setPrimaryWeapons(Collections.singletonList(
            new com.danieljglover.allinslayer.model.StrategyWeapon("Emberlight", STRAT_MELEE, null)));
        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(strategyVariant(meleeOnly)));

        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20)).recommend(
            task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", CombatStyle.RANGED);
        assertEquals("undocumented style -> stat engine, not the melee strategy weapon",
            Integer.valueOf(BETTER_RANGED), rec.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void variantWithoutStrategyIsUnchanged()
    {
        // FR-S4: a variant with no strategy -> pure stat engine (the override path is never entered).
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(50, 50, 5));
        table.put(BETTER_MELEE, meleeW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(STRAT_MELEE, BETTER_MELEE));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(variant("Greater demon", CombatStyle.MELEE, true, false)));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Greater demon", null);

        assertEquals("no strategy -> higher-DPS stat pick", Integer.valueOf(BETTER_MELEE),
            rec.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void malformedStrategyIsTreatedAsNoStrategy()
    {
        // Honesty: a malformed strategy (null primaryStyle / empty primaryWeapons) is ignored (stat
        // engine) and never throws.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(BETTER_MELEE, meleeW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(BETTER_MELEE));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(
            strategyVariant(new com.danieljglover.allinslayer.model.MonsterStrategy())));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);

        assertEquals("malformed strategy -> stat engine", Integer.valueOf(BETTER_MELEE),
            rec.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void malformedStrategySurfacesAVisibleFallbackNote()
    {
        // WB-4 (D8): the stat-engine fallback for a present-but-malformed strategy used to be a
        // log-only warn - invisible to the player. It now sets strategyFallbackNote so the panel
        // can say "strategy data unavailable" instead of silently rendering a stat pick.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(BETTER_MELEE, meleeW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(BETTER_MELEE));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(
            strategyVariant(new com.danieljglover.allinslayer.model.MonsterStrategy())));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);

        assertNull("no wiki-strategy note composes from malformed data", rec.get().getStrategyNote());
        assertNotNull(rec.get().getStrategyFallbackNote());
        assertTrue("the note names the problem", rec.get().getStrategyFallbackNote()
            .contains("strategy data unavailable"));
    }

    @Test
    public void validAndAbsentStrategiesLeaveNoFallbackNote()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(50, 50, 5));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(STRAT_MELEE));

        // Valid strategy -> the real strategy note, no fallback note.
        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(strategyVariant(demonStrategy())));
        Optional<Recommendation> valid = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);
        assertNotNull(valid.get().getStrategyNote());
        assertNull(valid.get().getStrategyFallbackNote());

        // Absent strategy -> silent (a no-strategy variant is normal, not a data defect).
        TaskData plain = meleeTask();
        plain.setVariants(Collections.singletonList(
            variant("Greater demon", CombatStyle.MELEE, true, false)));
        Optional<Recommendation> none = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(plain, owned, stats(), AdviceMode.DPS, false, null, "Greater demon", null);
        assertNull(none.get().getStrategyFallbackNote());
    }

    @Test
    public void strategyPrimaryStyleBecomesTheDefaultMethodOverridingWeaknessStyle()
    {
        // MV-S2 clarification: when a variant has a strategy, the DEFAULT selected method =
        // strategy.primaryStyle (advisor-derived), overriding the variant's authored weakness.style.
        // E.g. Abyssal Sire: authored weakness MAGIC, guide MELEE (Emberlight) -> default method MELEE
        // and the strategy weapon wins. Derived at advise-time; the variant is not mutated.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(50, 50, 5));
        table.put(BETTER_MELEE, meleeW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(STRAT_MELEE, BETTER_MELEE));

        com.danieljglover.allinslayer.model.MonsterStrategy s =
            new com.danieljglover.allinslayer.model.MonsterStrategy();
        s.setPrimaryStyle(CombatStyle.MELEE);
        s.setPrimaryWeapons(Collections.singletonList(
            new com.danieljglover.allinslayer.model.StrategyWeapon("Emberlight", STRAT_MELEE, null)));
        MonsterVariant v = variant("Abyssal Sire", CombatStyle.MAGIC, true, true); // weakness MAGIC
        v.setStrategy(s);
        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(v));

        // No selectedMethod -> default. Despite weakness MAGIC, the default flips to the strategy's MELEE.
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Abyssal Sire", null);
        assertEquals("default method = strategy.primaryStyle (not the authored weakness MAGIC)",
            CombatStyle.MELEE, rec.get().getStyle());
        assertEquals("recommends the strategy weapon by default (override engaged)",
            Integer.valueOf(STRAT_MELEE), rec.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void strategyNoteComposedWheneverAStrategyExistsRegardlessOfOwnership()
    {
        // FR-S5 / MV-S4: the "Wiki strategy" note lists the primary + secondary weapons + the free-text
        // note, shown even when the player owns NONE of the strategy weapons (informational).
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(BETTER_MELEE, meleeW(95, 95, 4)); // owns only a non-strategy weapon
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(BETTER_MELEE));

        com.danieljglover.allinslayer.model.MonsterStrategy s = demonStrategy();
        s.setNote("Switch to ranged for the shield-down window");
        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(strategyVariant(s)));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);

        String note = rec.get().getStrategyNote();
        assertNotNull("strategy note composed even when no strategy weapon is owned", note);
        assertTrue("lists the primary weapon", note.contains("Emberlight"));
        assertTrue("names the primary style", note.contains("MELEE"));
        assertTrue("lists the secondary weapon", note.contains("Scorching bow"));
        assertTrue("names the secondary style", note.contains("RANGED"));
        assertTrue("carries the free-text note", note.contains("Switch to ranged"));
    }

    @Test
    public void noStrategyNoteWhenVariantHasNoStrategy()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(BETTER_MELEE, meleeW(95, 95, 4));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(BETTER_MELEE));
        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(variant("Greater demon", CombatStyle.MELEE, true, false)));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "Greater demon", null);
        assertNull("no strategy -> no note", rec.get().getStrategyNote());
    }

    // --- MV-S3: the strategy override + style-mismatch default on the REAL authored dataset ----

    private static final int EMBERLIGHT = 29589;
    private static final int FANG = 26219;
    private static final int SCORCHING_BOW = 29591;
    private static final int DRAGON_HUNTER_CROSSBOW = 21012;

    /** The real TaskData carrying a variant of the given name (optionally pinned to a weakness style). */
    private static TaskData realTaskWith(String variantName, CombatStyle weaknessStyle)
    {
        SlayerDataService service = new SlayerDataService(new Gson());
        service.load();
        Collection<TaskData> all = service.all();
        for (TaskData t : all)
        {
            if (t.getVariants() == null)
            {
                continue;
            }
            for (MonsterVariant v : t.getVariants())
            {
                if (variantName.equals(v.getName())
                    && (weaknessStyle == null
                        || (v.getWeakness() != null && weaknessStyle == v.getWeakness().getStyle())))
                {
                    return t;
                }
            }
        }
        throw new IllegalStateException("no real task with variant " + variantName + " / " + weaknessStyle);
    }

    @Test
    public void tormentedDemonOnRealDataRecommendsEmberlightNotFang()
    {
        // FR-S1 on REAL authored data: the Tormented Demon variant carries the demonbane-melee strategy.
        // Fang is given much higher raw DPS than Emberlight, so Emberlight winning proves the OVERRIDE
        // fired (not a DPS coincidence). Toggle to Ranged -> the Scorching bow secondary. Own only the
        // Fang -> it is the next owned documented melee primary (the priority-walk fallback).
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(EMBERLIGHT, meleeW(50, 50, 5));        // modest
        table.put(FANG, meleeW(150, 150, 4));            // far higher raw DPS than Emberlight
        table.put(SCORCHING_BOW, rangedW(95, 95, 5));
        EquipmentStatsProvider sp = table::get;
        LoadoutAdvisor advisor = advisorReal(sp, id -> 1000, food(SHARK, 20));
        TaskData task = realTaskWith("Tormented Demon", null);

        // Own Emberlight + Fang, default method (= strategy MELEE) -> Emberlight wins via the override.
        OwnedItems both = OwnedItems.fromCounts(counts(EMBERLIGHT, FANG));
        Optional<Recommendation> melee = advisor.recommend(
            task, both, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);
        assertEquals("default method = strategy MELEE", CombatStyle.MELEE, melee.get().getStyle());
        assertEquals("Emberlight is recommended over the higher-DPS Fang (override)",
            Integer.valueOf(EMBERLIGHT), melee.get().getWorn().get(EquipmentSlot.WEAPON));

        // Toggle to Ranged, own the Scorching bow -> the ranged secondary wins.
        OwnedItems ranged = OwnedItems.fromCounts(counts(EMBERLIGHT, FANG, SCORCHING_BOW));
        Optional<Recommendation> rangedRec = advisor.recommend(
            task, ranged, stats(), AdviceMode.DPS, false, null, "Tormented Demon", CombatStyle.RANGED);
        assertEquals("ranged toggle -> Scorching bow (strategy secondary)",
            Integer.valueOf(SCORCHING_BOW), rangedRec.get().getWorn().get(EquipmentSlot.WEAPON));

        // Own the Fang but NOT Emberlight -> Fang (the next owned documented melee primary).
        OwnedItems fangOnly = OwnedItems.fromCounts(counts(FANG));
        Optional<Recommendation> fallback = advisor.recommend(
            task, fangOnly, stats(), AdviceMode.DPS, false, null, "Tormented Demon", null);
        assertEquals("own Fang-not-Emberlight -> Fang",
            Integer.valueOf(FANG), fallback.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void styleMismatchVariantsDefaultToTheStrategyStyleOnRealData()
    {
        // The default-method handling on REAL data: Abyssal Sire (Boss pool, authored weakness MAGIC)
        // defaults to MELEE per its guide, and Vorkath (Boss pool, authored weakness MELEE) defaults to
        // RANGED. The authored weakness.style is not mutated; the advisor derives the default from the
        // strategy's primaryStyle.
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(EMBERLIGHT, meleeW(80, 80, 5));
        table.put(DRAGON_HUNTER_CROSSBOW, rangedW(95, 95, 5));
        EquipmentStatsProvider sp = table::get;
        LoadoutAdvisor advisor = advisorReal(sp, id -> 1000, food(SHARK, 20));

        TaskData sireTask = realTaskWith("Abyssal Sire", CombatStyle.MAGIC); // guide MELEE
        Optional<Recommendation> sire = advisor.recommend(
            sireTask, OwnedItems.fromCounts(counts(EMBERLIGHT)), stats(), AdviceMode.DPS, false,
            null, "Abyssal Sire", null);
        assertEquals("Abyssal Sire defaults to the guide MELEE (not the authored MAGIC weakness)",
            CombatStyle.MELEE, sire.get().getStyle());
        assertEquals("and the melee strategy weapon wins",
            Integer.valueOf(EMBERLIGHT), sire.get().getWorn().get(EquipmentSlot.WEAPON));

        TaskData vorkathTask = realTaskWith("Vorkath", CombatStyle.MELEE); // guide RANGED
        Optional<Recommendation> vorkath = advisor.recommend(
            vorkathTask, OwnedItems.fromCounts(counts(DRAGON_HUNTER_CROSSBOW)), stats(), AdviceMode.DPS,
            false, null, "Vorkath", null);
        assertEquals("Vorkath defaults to the guide RANGED (not the authored MELEE weakness)",
            CombatStyle.RANGED, vorkath.get().getStyle());
        assertEquals("and the ranged strategy weapon wins", Integer.valueOf(DRAGON_HUNTER_CROSSBOW),
            vorkath.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    private static MonsterVariant variant(String name, CombatStyle style, boolean isDefault, boolean boss)
    {
        MonsterVariant v = new MonsterVariant();
        v.setName(name);
        v.setWeakness(new Weakness(style, null));
        v.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        v.setDefault(isDefault);
        v.setBoss(boss);
        return v;
    }

    /** Fake effects provider: one food id heals, nothing boosts. */
    private static ConsumableEffectsProvider food(int foodId, int heal)
    {
        return new ConsumableEffectsProvider()
        {
            @Override
            public Integer healAmount(int itemId)
            {
                return itemId == foodId ? heal : null;
            }

            @Override
            public Set<BoostedStat> boostedStats(int itemId)
            {
                return Collections.emptySet();
            }

            @Override
            public Map<BoostedStat, Integer> boostMagnitudes(int itemId)
            {
                return Collections.emptyMap();
            }
        };
    }
}

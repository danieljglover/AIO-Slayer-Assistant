package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Stat-driven gear selection over the player's owned items (plan section 4, ADR-0001, ADR-0008). For a
 * task the weakness fixes the combat style; for melee the monster's lowest defence fixes the attack
 * type. Non-weapon slots are flat-scored (base offence + the LFB additive conditional term) and the
 * best (DPS) or cheapest (COST) per slot is worn. The WEAPON slot (MELEE/RANGED) is instead ranked by
 * real sustained DPS through the {@link DpsEstimator#weaponDps} seam, so attack speed and passive
 * special effects (Fang/Scythe) drive the pick; magic weapons stay flat-scored in v1 (ADR-0008 §6).
 * The 2h-vs-(1h+shield) interplay is resolved on combined DPS. The output is empty when the weakness
 * has no style or no weapon for the style is owned.
 *
 * <p>Reads item stats through the {@link EquipmentStatsProvider} seam (null = not loaded /
 * non-equipable -> skipped, ADR-0002); never touches a live {@code ItemManager}.
 */
@Singleton
public class GearSelector
{
    /**
     * Magic-damage % is converted to a magic-attack equivalent for ranking only: 10% magic damage
     * counts like 50 magic attack (plan section 4.3).
     */
    public static final int MAGIC_DMG_WEIGHT = 5;

    private final EquipmentStatsProvider stats;
    private final PriceService price;
    private final DpsEstimator dpsEstimator;

    @Inject
    public GearSelector(EquipmentStatsProvider stats, PriceService price, DpsEstimator dpsEstimator)
    {
        this.stats = stats;
        this.price = price;
        this.dpsEstimator = dpsEstimator;
    }

    /**
     * @param owned   every item the player has (inventory + worn + last-seen bank)
     * @param profile the user-selected monster profile - its {@code monsterDefence} fixes the melee
     *                attack type and its category flags gate the bane bonuses (MV-B3, ADR-0010)
     * @param style   the effective combat style to gear for: the user-selected method, or the
     *                profile's recommended style by default (MV-B9, ADR-0013). Replaces the old
     *                weakness-derived style so the user can override it via the method selector.
     * @param player  the player's live skill levels (load-bearing: the WEAPON slot is ranked by DPS,
     *                which reads the player's offensive levels)
     * @param mode    DPS = highest DPS/score per slot, COST = cheapest viable item per slot
     * @return the worn map ({@code slot -> item id}); empty if the style is null or no weapon is owned
     *     for the style (the latter drives the UI method-disable, ADR-0013)
     */
    public Map<EquipmentSlot, Integer> select(OwnedItems owned, MonsterProfile profile,
        CombatStyle style, PlayerStats player, AdviceMode mode)
    {
        return select(owned, profile, style, player, mode, false);
    }

    /**
     * @param wilderness whether the user's selected location is in the Wilderness (ADR-0009 A.5) - a
     *     per-LOCATION predicate threaded from {@code LoadoutAdvisor}, crediting Wilderness weapons.
     */
    public Map<EquipmentSlot, Integer> select(OwnedItems owned, MonsterProfile profile,
        CombatStyle style, PlayerStats player, AdviceMode mode, boolean wilderness)
    {
        return select(owned, profile, style, player, mode, wilderness, Collections.emptyList());
    }

    /**
     * @param strategyWeaponIds the priority-ordered wiki {@code /Strategies} weapon ids that OVERRIDE
     *     the DPS weapon pick for the WEAPON slot (ADR-0015 / MV-S2). Empty = no override = today's
     *     pure-DPS path (the FR-S4 regression anchor). When non-empty, the highest-priority id that is
     *     OWNED and a viable weapon for the style wins the WEAPON slot, bypassing the DPS ranking; if
     *     none is owned-and-viable the stat-driven pick stands (bank-aware fallback). {@code
     *     LoadoutAdvisor} owns the policy (which ids apply for the effective style); this owns the
     *     mechanism. Every other slot stays stat-driven.
     */
    public Map<EquipmentSlot, Integer> select(OwnedItems owned, MonsterProfile profile,
        CombatStyle style, PlayerStats player, AdviceMode mode, boolean wilderness,
        List<Integer> strategyWeaponIds)
    {
        EnumMap<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);

        if (style == null)
        {
            return worn; // no method/style -> no gear (section 4.1)
        }

        MonsterDefence def = profile == null ? null : profile.getDefence();
        MeleeAttackType meleeType = style == CombatStyle.MELEE ? meleeAttackType(def, owned) : null;

        // The variant + location predicates a curated conditional source evaluates (ADR-0007/0008/0009).
        BonusContext ctx = BonusContext.from(profile, wilderness);

        // Pass A: flat (base) score every owned equipable item and track the per-slot baseline maximum.
        // L = the loadout's baseline offence for the style (sum of the best base per slot). A
        // conditional bonus multiplies the WHOLE loadout's offence, so its slot value is proportional
        // to L, not to the item's own stats - hence the loadout-level term below (design section 3.2).
        List<Scored> scored = new ArrayList<>();
        EnumMap<EquipmentSlot, Integer> maxBaseBySlot = new EnumMap<>(EquipmentSlot.class);
        for (int id : owned.ids())
        {
            Bonuses b = stats.get(id);
            if (b == null || b.getSlot() == null)
            {
                continue; // not loaded / non-equipable / unmapped slot (ADR-0002)
            }
            int base = score(b, style, meleeType);
            scored.add(new Scored(id, b, base));
            maxBaseBySlot.merge(b.getSlot(), base, Math::max);
        }
        int loadoutOffence = 0;
        for (int slotMax : maxBaseBySlot.values())
        {
            loadoutOffence += Math.max(0, slotMax); // a slot with no positive item adds no offence
        }

        // Pass B: NON-weapon slots get effective score = base + round((m - 1) * L) (the LFB additive
        // conditional term). The WEAPON slot is EXCLUDED here - it is ranked by DPS in the interplay
        // below, and a weapon's own conditional multiplier (DHL) is applied multiplicatively there, so
        // it must NOT also receive the additive term (no double-count, design section 3.5).
        Map<EquipmentSlot, List<Candidate>> bySlot = new EnumMap<>(EquipmentSlot.class);
        for (Scored s : scored)
        {
            if (s.b.getSlot() == EquipmentSlot.WEAPON)
            {
                continue;
            }
            int eff = s.base + bonusTerm(s.id, style, ctx, loadoutOffence);
            if (eff <= 0)
            {
                continue; // contributes no weakness-relevant offence -> does not fill a slot
            }
            bySlot.computeIfAbsent(s.b.getSlot(), k -> new ArrayList<>())
                .add(new Candidate(s.id, s.b, eff));
        }

        // Step 6: the weapon/shield interplay decides WEAPON (and possibly SHIELD). Magic ranking is
        // gear-aware (ADR-0009 B.3): the chosen non-weapon magic gear is summed into the candidate's
        // magic DPS, so compute it from the (weapon-independent) non-weapon slots first.
        String element = profile != null ? profile.element() : null;
        int[] magicGear = style == CombatStyle.MAGIC && mode == AdviceMode.DPS
            ? magicGearContext(bySlot, style, mode)
            : new int[] {0, 0};
        // WD-8: the best owned ranged ammo (arrows/bolts/darts) is folded into every ranged weapon's
        // DPS in ranking, matching the display estimator which already sums the worn ammo. Null for
        // non-ranged styles or when no ammo is owned (byte-identical to today).
        Bonuses bestAmmo = bestOwnedAmmo(scored, style);
        // WD-11: the target's Magic level (nullable) scales the Twisted bow in weapon ranking; null (no
        // offence datum) -> the bow is ranked on base stats only (FR-6). No other weapon reads it.
        Integer magicLevel = profile != null && profile.getOffence() != null
            ? profile.getOffence().getMagicLevel() : null;
        resolveWeaponAndShield(scored, bySlot, style, meleeType, def, player, mode, ctx, owned,
            element, magicGear[0], magicGear[1], bestAmmo, magicLevel, strategyWeaponIds, worn);
        if (!worn.containsKey(EquipmentSlot.WEAPON))
        {
            return new EnumMap<>(EquipmentSlot.class); // not viable -> empty worn
        }

        // Step 5/7: fill every other slot (WEAPON + SHIELD are owned by the interplay above).
        for (Map.Entry<EquipmentSlot, List<Candidate>> e : bySlot.entrySet())
        {
            EquipmentSlot slot = e.getKey();
            if (slot == EquipmentSlot.WEAPON || slot == EquipmentSlot.SHIELD)
            {
                continue;
            }
            // Ranged ammo is always picked by highest ranged strength regardless of DPS/COST mode
            // (plan section 4.7) - the one slot whose pick ignores mode, unlike every other slot.
            Candidate chosen = slot == EquipmentSlot.AMMO && style == CombatStyle.RANGED
                ? pickAmmo(e.getValue())
                : pick(e.getValue(), style, mode);
            if (chosen != null)
            {
                worn.put(slot, chosen.id);
            }
        }

        // WD-10 (ADR-0020 #4): the only structural post-pick pass. If the player owns a complete Void
        // set for the style, compare the whole loadout with the set (its loadout-level acc/dmg
        // multiplier applied) against the per-slot pick; wear the set when it wins on real DPS.
        applyVoidSetBonus(owned, worn, style, def, player, ctx, mode);
        return worn;
    }

    /**
     * WD-10: the Void / Elite void set-bonus post-pick pass (MELEE/RANGED, ADR-0008 §6 scopes magic
     * weapon ranking specially - the magic void bonus is carried by {@link SetBonusRegistry} but not
     * yet folded into magic selection). When a complete set is owned, the void loadout's DPS is scored
     * WITH the set's loadout multiplier (applied once, on top of the loadout's task-conditional
     * multiplier) and swapped in only if it strictly beats the per-slot pick. Owned-gated and capped by
     * the registry, so it never over-credits (R2 mitigation).
     */
    private void applyVoidSetBonus(OwnedItems owned, EnumMap<EquipmentSlot, Integer> worn,
        CombatStyle style, MonsterDefence def, PlayerStats player,
        BonusContext ctx, AdviceMode mode)
    {
        if (mode != AdviceMode.DPS || style == CombatStyle.MAGIC)
        {
            return; // COST mode ignores set bonuses; magic selection is a documented boundary
        }
        if (!worn.containsKey(EquipmentSlot.WEAPON))
        {
            return;
        }
        SetBonus set = SetBonusRegistry.voidBonus(style, owned::has);
        if (set == null)
        {
            return;
        }
        double baseDps = loadoutStyleDps(worn, style, def, player, ctx, 1.0, 1.0);
        EnumMap<EquipmentSlot, Integer> voidWorn = new EnumMap<>(worn);
        voidWorn.putAll(set.getPieces());
        double voidDps = loadoutStyleDps(voidWorn, style, def, player, ctx,
            set.getAccMultiplier(), set.getDmgMultiplier());
        if (voidDps > baseDps)
        {
            worn.putAll(set.getPieces());
        }
    }

    /**
     * WD-10 helper: the whole worn loadout's sustained MELEE/RANGED DPS, summing every worn slot's
     * offensive bonuses (incl. the WD-8 ammo) into a synthetic weapon and scoring via {@link
     * DpsEstimator#weaponDps}. The loadout's highest applicable task-conditional multiplier is combined
     * with the passed set multiplier (both boost accuracy and damage rolls) - the SAME "max, not
     * product" conditional rule the display estimator uses, so this is display-consistent.
     */
    private double loadoutStyleDps(EnumMap<EquipmentSlot, Integer> worn, CombatStyle style,
        MonsterDefence def, PlayerStats player, BonusContext ctx, double setAcc, double setDmg)
    {
        int astab = 0;
        int aslash = 0;
        int acrush = 0;
        int arange = 0;
        int meleeStr = 0;
        int rangedStr = 0;
        int speed = 4;
        WeaponEffect fx = WeaponEffect.NONE;
        double condAcc = 1.0;
        double condDmg = 1.0;
        for (Map.Entry<EquipmentSlot, Integer> e : worn.entrySet())
        {
            Bonuses b = stats.get(e.getValue());
            if (b == null)
            {
                continue;
            }
            astab += b.getAstab();
            aslash += b.getAslash();
            acrush += b.getAcrush();
            arange += b.getArange();
            meleeStr += b.getMeleeStr();
            rangedStr += b.getRangedStr();
            ConditionalBonus bonus = ConditionalBonusRegistry.lookup(e.getValue());
            if (bonus != null && bonus.getCondition().test(ctx))
            {
                condAcc = Math.max(condAcc, bonus.accMultiplier(style));
                condDmg = Math.max(condDmg, bonus.dmgMultiplier(style));
            }
            if (e.getKey() == EquipmentSlot.WEAPON)
            {
                if (b.getAttackSpeedTicks() > 0)
                {
                    speed = b.getAttackSpeedTicks();
                }
                fx = WeaponEffectRegistry.lookup(e.getValue());
            }
        }
        Bonuses summed = new Bonuses(astab, aslash, acrush, 0, arange, meleeStr, rangedStr, 0, speed,
            EquipmentSlot.WEAPON, true);
        return dpsEstimator.weaponDps(summed, player, def, style, fx, condAcc * setAcc,
            condDmg * setDmg);
    }

    // --- melee attack type (section 4.2) -------------------------------------------------------

    private MeleeAttackType meleeAttackType(MonsterDefence d, OwnedItems owned)
    {
        int min = Integer.MAX_VALUE;
        for (MeleeAttackType t : MeleeAttackType.values())
        {
            min = Math.min(min, defence(d, t));
        }

        MeleeAttackType best = null;
        int bestWeaponAttack = Integer.MIN_VALUE;
        for (MeleeAttackType t : MeleeAttackType.values()) // STAB, SLASH, CRUSH -> stable tie-break
        {
            if (defence(d, t) != min)
            {
                continue;
            }
            int weaponAttack = bestOwnedWeaponAttack(t, owned);
            if (weaponAttack > bestWeaponAttack)
            {
                bestWeaponAttack = weaponAttack;
                best = t;
            }
        }
        return best;
    }

    private static int defence(MonsterDefence d, MeleeAttackType t)
    {
        return d == null ? 0 : t.monsterDefence(d);
    }

    private int bestOwnedWeaponAttack(MeleeAttackType t, OwnedItems owned)
    {
        int best = 0;
        for (int id : owned.ids())
        {
            Bonuses b = stats.get(id);
            if (b != null && b.getSlot() == EquipmentSlot.WEAPON)
            {
                best = Math.max(best, t.attackBonus(b));
            }
        }
        return best;
    }

    // --- scoring (section 4.3) -----------------------------------------------------------------

    private static int score(Bonuses b, CombatStyle style, MeleeAttackType meleeType)
    {
        switch (style)
        {
            case MELEE:
                return meleeType.attackBonus(b) + b.getMeleeStr();
            case RANGED:
                return b.getArange() + b.getRangedStr();
            case MAGIC:
                return b.getAmagic() + MAGIC_DMG_WEIGHT * b.getMagicDmgPercent();
            default:
                return 0;
        }
    }

    /**
     * The additive per-slot bonus term {@code round((m - 1) * L)} for an owned item (design section
     * 3.2), where {@code m} is the item's curated conditional multiplier for the style when its
     * predicate holds, else {@code 1.0} (no term). Integer-rounded once for determinism (NFR-1).
     */
    private static int bonusTerm(int itemId, CombatStyle style, BonusContext ctx, int loadoutOffence)
    {
        ConditionalBonus bonus = ConditionalBonusRegistry.lookup(itemId);
        if (bonus == null || !bonus.getCondition().test(ctx))
        {
            return 0;
        }
        // Non-weapon sources are symmetric (invariant, ADR-0009 A.1) so acc == dmg; the additive term
        // takes the (single) damage multiplier.
        return (int) Math.round((bonus.dmgMultiplier(style) - 1.0) * loadoutOffence);
    }

    private static int secondaryStat(Bonuses b, CombatStyle style)
    {
        switch (style)
        {
            case MELEE:
                return b.getMeleeStr();
            case RANGED:
                return b.getRangedStr();
            case MAGIC:
                return b.getMagicDmgPercent();
            default:
                return 0;
        }
    }

    // --- per-slot pick (section 4.5) -----------------------------------------------------------

    private Candidate pick(List<Candidate> candidates, CombatStyle style, AdviceMode mode)
    {
        if (candidates == null || candidates.isEmpty())
        {
            return null;
        }
        Comparator<Candidate> order = mode == AdviceMode.COST
            ? Comparator.<Candidate>comparingLong(c -> effectivePrice(c.id))
                .thenComparingInt(c -> c.id)
            : Comparator.<Candidate>comparingInt((Candidate c) -> c.score).reversed()
                .thenComparing(Comparator.comparingInt((Candidate c) -> secondaryStat(c.b, style)).reversed())
                .thenComparingInt(c -> c.id);
        return candidates.stream().min(order).orElse(null);
    }

    /** Section 4.7: ranged ammo is chosen by highest ranged strength (tie-break id ascending). */
    private static Candidate pickAmmo(List<Candidate> candidates)
    {
        if (candidates == null || candidates.isEmpty())
        {
            return null;
        }
        return candidates.stream()
            .min(Comparator.comparingInt((Candidate c) -> c.b.getRangedStr()).reversed()
                .thenComparingInt(c -> c.id))
            .orElse(null);
    }

    /**
     * WD-8: the best owned ranged AMMO's bonuses for ranking - highest ranged strength, tie-break id
     * ascending (the same ordering as {@link #pickAmmo}, so the ammo folded into weapon ranking is the
     * ammo that will be worn). Null for non-ranged styles or when no AMMO is owned - the byte-identical
     * pre-WD-8 path (FR-6).
     */
    private static Bonuses bestOwnedAmmo(List<Scored> scored, CombatStyle style)
    {
        if (style != CombatStyle.RANGED)
        {
            return null;
        }
        Bonuses best = null;
        int bestId = 0;
        for (Scored s : scored)
        {
            if (s.b.getSlot() != EquipmentSlot.AMMO)
            {
                continue;
            }
            if (best == null || s.b.getRangedStr() > best.getRangedStr()
                || (s.b.getRangedStr() == best.getRangedStr() && s.id < bestId))
            {
                best = s.b;
                bestId = s.id;
            }
        }
        return best;
    }

    /** A ranged weapon with its ammo's ranged attack + strength summed in (WD-8), other fields kept. */
    private static Bonuses withAmmo(Bonuses w, Bonuses ammo)
    {
        return new Bonuses(
            w.getAstab(), w.getAslash(), w.getAcrush(), w.getAmagic(),
            w.getArange() + ammo.getArange(), w.getMeleeStr(), w.getRangedStr() + ammo.getRangedStr(),
            w.getMagicDmgPercent(), w.getAttackSpeedTicks(), w.getSlot(), w.isTwoHanded());
    }

    /** GE price with non-positive (unknown) prices sorted last, matching the COST per-slot rule. */
    private long effectivePrice(int id)
    {
        int p = price.price(id);
        return p <= 0 ? Long.MAX_VALUE / 4 : p;
    }

    // --- weapon / shield interplay (section 4.6, ADR-0008 section 3.6) --------------------------

    private void resolveWeaponAndShield(List<Scored> scored,
        Map<EquipmentSlot, List<Candidate>> bySlot, CombatStyle style, MeleeAttackType meleeType,
        MonsterDefence def, PlayerStats player, AdviceMode mode, BonusContext ctx, OwnedItems owned,
        String element, int gearMatt, int gearMdmg, Bonuses ammo, Integer magicLevel,
        List<Integer> strategyWeaponIds, EnumMap<EquipmentSlot, Integer> worn)
    {
        List<WeaponPick> twoHand = new ArrayList<>();
        List<WeaponPick> oneHand = new ArrayList<>();
        for (Scored s : scored)
        {
            if (s.b.getSlot() != EquipmentSlot.WEAPON)
            {
                continue;
            }
            double dps = weaponRank(s.id, s.b, style, meleeType, def, player, ctx, mode, owned,
                element, gearMatt, gearMdmg, ammo, magicLevel);
            if (dps <= 0)
            {
                continue; // not a usable weapon for the style (viable filter)
            }
            (s.b.isTwoHanded() ? twoHand : oneHand).add(new WeaponPick(s.id, s.b, dps));
        }

        List<Candidate> shields = bySlot.get(EquipmentSlot.SHIELD);
        Candidate bestShield = pick(shields, style, mode);

        // Strategy override (ADR-0015 / MV-S2): a wiki /Strategies weapon wins the WEAPON slot,
        // bypassing the DPS ranking, when one of its priority-ordered ids is OWNED and a viable weapon
        // candidate for the effective style. A 2h strategy weapon drops the shield; a 1h keeps the
        // stat-picked best shield (the same interplay as the normal path). If no strategy weapon is
        // owned-and-viable we fall through to the stat-driven pick below (bank-aware fallback).
        WeaponPick override = pickStrategyWeapon(strategyWeaponIds, twoHand, oneHand);
        if (override != null)
        {
            if (override.b.isTwoHanded())
            {
                worn.put(EquipmentSlot.WEAPON, override.id);
            }
            else
            {
                putOneHandAndShield(worn, override, bestShield);
            }
            return;
        }

        WeaponPick best2h = pickWeapon(twoHand, mode);
        WeaponPick best1h = pickWeapon(oneHand, mode);

        if (best2h == null && best1h == null)
        {
            return; // no weapon -> WEAPON left empty (caller treats this as non-viable)
        }
        if (best1h == null)
        {
            worn.put(EquipmentSlot.WEAPON, best2h.id); // only a 2h owned
            return;
        }
        if (best2h == null)
        {
            putOneHandAndShield(worn, best1h, bestShield); // only a 1h owned
            return;
        }

        // Both exist: 2h wins only when strictly better; ties fall to 1h+shield (section 4.6).
        boolean chooseTwoHand;
        if (mode == AdviceMode.COST)
        {
            long priceA = effectivePrice(best2h.id);
            long priceB = effectivePrice(best1h.id)
                + (bestShield == null ? 0 : effectivePrice(bestShield.id));
            chooseTwoHand = priceA < priceB;
        }
        else
        {
            // Combined 1h+shield DPS: the shield's offensive bonuses are summed into the 1h's DPS
            // input (using the 1h's speed/effect/conditional), NOT flat-score added (a unit error).
            double dpsA = best2h.dps;
            double dpsB = bestShield == null
                ? best1h.dps
                : weaponRank(best1h.id, combine(best1h.b, bestShield.b), style, meleeType, def, player,
                    ctx, mode, owned, element, gearMatt, gearMdmg, ammo, magicLevel);
            chooseTwoHand = dpsA > dpsB;
        }

        if (chooseTwoHand)
        {
            worn.put(EquipmentSlot.WEAPON, best2h.id);
        }
        else
        {
            putOneHandAndShield(worn, best1h, bestShield);
        }
    }

    /**
     * The ranking value for a weapon candidate: real sustained DPS for MELEE/RANGED (via the
     * {@link DpsEstimator#weaponDps} seam, applying the weapon's passive {@link WeaponEffect} and its
     * own task-conditional multiplier), or the flat magic score for MAGIC (v1 - magic weapon DPS is a
     * documented boundary, ADR-0008 §6).
     */
    private double weaponRank(int id, Bonuses b, CombatStyle style, MeleeAttackType meleeType,
        MonsterDefence def, PlayerStats player, BonusContext ctx, AdviceMode mode, OwnedItems owned,
        String element, int gearMatt, int gearMdmg, Bonuses ammo, Integer magicLevel)
    {
        if (style == CombatStyle.MAGIC)
        {
            if (mode == AdviceMode.COST)
            {
                return score(b, style, meleeType); // COST mode: cheapest viable, flat magic score
            }
            return magicWeaponRank(id, b, owned, player, def, element, gearMatt, gearMdmg, ctx);
        }
        // WD-8: fold the best owned ammo's ranged strength (and attack) into the ranged weapon before
        // its DPS is computed, so ranking matches the display estimator (which sums the worn ammo). The
        // blowpipe's best owned dart rides the same seam - darts are AMMO-slot items.
        if (style == CombatStyle.RANGED && ammo != null)
        {
            b = withAmmo(b, ammo);
        }
        ConditionalBonus bonus = ConditionalBonusRegistry.lookup(id);
        double accMult = 1.0;
        double dmgMult = 1.0;
        if (bonus != null && bonus.getCondition().test(ctx))
        {
            // A weapon's OWN conditional bonus (DHL vs dragon, Keris vs kalphite) is applied
            // multiplicatively here - separate accuracy and damage so an asymmetric source (Keris is
            // damage-only) is correct. The weapon slot is excluded from the additive (m-1)*L term so
            // it is never double-counted (ADR-0008 section 3.5, ADR-0009 A.1).
            accMult = bonus.accMultiplier(style);
            dmgMult = bonus.dmgMultiplier(style);
        }
        // WD-11: the Twisted bow's per-target Magic-level scaling folds in the same multiplicative way
        // (no double-count). Null (not the Tbow, or unknown magic level) leaves the multipliers at their
        // base-stats values (FR-6). The Tbow carries no conditional bonus, so this composes cleanly.
        double[] tbow = TwistedBowEvaluator.scaling(id, magicLevel);
        if (tbow != null)
        {
            accMult *= tbow[0];
            dmgMult *= tbow[1];
        }
        return dpsEstimator.weaponDps(b, player, def, style, WeaponEffectRegistry.lookup(id),
            accMult, dmgMult);
    }

    /**
     * Gear-aware magic DPS for a candidate magic weapon (ADR-0009 B.3): resolve the best castable
     * spell / powered-staff via the shared {@link MagicWeaponEvaluator}, add the chosen non-weapon
     * magic gear ({@code gearMatt}/{@code gearMdmg}), and score via the estimator's magic core.
     */
    private double magicWeaponRank(int id, Bonuses b, OwnedItems owned, PlayerStats player,
        MonsterDefence def, String element, int gearMatt, int gearMdmg, BonusContext ctx)
    {
        MagicSetup profile = MagicWeaponEvaluator.evaluate(owned, element, player, id);
        int speed = MagicWeaponEvaluator.castSpeedTicks(profile.isPoweredStaff(), b.getAttackSpeedTicks());
        // Tumeken's Shadow triples the GEAR contribution (matt/mdmg); a normal weapon adds it once.
        int matt = MagicWeaponEvaluator.effectiveMagicAttack(id, b.getAmagic(), gearMatt);
        int mdmg = MagicWeaponEvaluator.effectiveMagicDamage(id, b.getMagicDmgPercent(), gearMdmg);
        // The magic weapon's OWN conditional bonus (e.g. Thammaron's sceptre in the Wilderness).
        double accMult = 1.0;
        double dmgMult = 1.0;
        ConditionalBonus bonus = ConditionalBonusRegistry.lookup(id);
        if (bonus != null && bonus.getCondition().test(ctx))
        {
            accMult = bonus.accMultiplier(CombatStyle.MAGIC);
            dmgMult = bonus.dmgMultiplier(CombatStyle.MAGIC);
        }
        return dpsEstimator.magicWeaponDps(player, def, profile.getSpellBaseMaxHit(), speed, matt, mdmg,
            accMult, dmgMult);
    }

    /**
     * The chosen non-weapon, non-shield magic gear's summed magic attack and magic damage % (the gear
     * that will be worn in DPS mode). The shield is excluded - it is folded into the 1h candidate's
     * DPS via {@link #combine} in the weapon/shield interplay.
     */
    private int[] magicGearContext(Map<EquipmentSlot, List<Candidate>> bySlot, CombatStyle style,
        AdviceMode mode)
    {
        int matt = 0;
        int mdmg = 0;
        for (Map.Entry<EquipmentSlot, List<Candidate>> e : bySlot.entrySet())
        {
            if (e.getKey() == EquipmentSlot.SHIELD)
            {
                continue; // handled by the weapon/shield interplay
            }
            Candidate chosen = pick(e.getValue(), style, mode);
            if (chosen != null)
            {
                matt += chosen.b.getAmagic();
                mdmg += chosen.b.getMagicDmgPercent();
            }
        }
        return new int[] {matt, mdmg};
    }

    /**
     * The highest-priority strategy weapon that is OWNED and a viable weapon candidate for the style
     * (ADR-0015 / MV-S2). Walks {@code ids} in priority order and returns the first matching {@link
     * WeaponPick} from the already-built (viability-filtered) 2h/1h candidate lists, or null when none
     * is owned-and-viable (the bank-aware fallback signal). The viability filter (dps &gt; 0) is reused
     * for free: an id absent from both lists is unowned, non-equipable, or wrong-style.
     */
    private static WeaponPick pickStrategyWeapon(List<Integer> ids, List<WeaponPick> twoHand,
        List<WeaponPick> oneHand)
    {
        if (ids == null || ids.isEmpty())
        {
            return null;
        }
        for (int id : ids)
        {
            for (WeaponPick w : twoHand)
            {
                if (w.id == id)
                {
                    return w;
                }
            }
            for (WeaponPick w : oneHand)
            {
                if (w.id == id)
                {
                    return w;
                }
            }
        }
        return null;
    }

    private WeaponPick pickWeapon(List<WeaponPick> candidates, AdviceMode mode)
    {
        if (candidates == null || candidates.isEmpty())
        {
            return null;
        }
        Comparator<WeaponPick> order = mode == AdviceMode.COST
            ? Comparator.<WeaponPick>comparingLong(c -> effectivePrice(c.id)).thenComparingInt(c -> c.id)
            : Comparator.<WeaponPick>comparingDouble((WeaponPick c) -> c.dps).reversed()
                .thenComparingInt(c -> c.id);
        return candidates.stream().min(order).orElse(null);
    }

    /** Sums the shield's offensive bonuses into the weapon's, keeping the weapon's speed/slot. */
    private static Bonuses combine(Bonuses weapon, Bonuses shield)
    {
        return new Bonuses(
            weapon.getAstab() + shield.getAstab(),
            weapon.getAslash() + shield.getAslash(),
            weapon.getAcrush() + shield.getAcrush(),
            weapon.getAmagic() + shield.getAmagic(),
            weapon.getArange() + shield.getArange(),
            weapon.getMeleeStr() + shield.getMeleeStr(),
            weapon.getRangedStr() + shield.getRangedStr(),
            weapon.getMagicDmgPercent() + shield.getMagicDmgPercent(),
            weapon.getAttackSpeedTicks(),
            weapon.getSlot(),
            weapon.isTwoHanded());
    }

    private static void putOneHandAndShield(EnumMap<EquipmentSlot, Integer> worn, WeaponPick oneHand,
        Candidate shield)
    {
        worn.put(EquipmentSlot.WEAPON, oneHand.id);
        if (shield != null)
        {
            worn.put(EquipmentSlot.SHIELD, shield.id);
        }
    }

    /** An owned item with its flat (base) score, before any conditional bonus term (pass A). */
    private static final class Scored
    {
        private final int id;
        private final Bonuses b;
        private final int base;

        private Scored(int id, Bonuses b, int base)
        {
            this.id = id;
            this.b = b;
            this.base = base;
        }
    }

    private static final class Candidate
    {
        private final int id;
        private final Bonuses b;
        private final int score;

        private Candidate(int id, Bonuses b, int score)
        {
            this.id = id;
            this.b = b;
            this.score = score;
        }
    }

    /** A weapon candidate with its computed sustained DPS (or flat magic score) for ranking. */
    private static final class WeaponPick
    {
        private final int id;
        private final Bonuses b;
        private final double dps;

        private WeaponPick(int id, Bonuses b, double dps)
        {
            this.id = id;
            this.b = b;
            this.dps = dps;
        }
    }
}

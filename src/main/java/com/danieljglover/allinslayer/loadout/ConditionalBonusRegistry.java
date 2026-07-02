package com.danieljglover.allinslayer.loadout;

import java.util.HashMap;
import java.util.Map;

/**
 * Curated registry of task-conditional offensive bonuses (ADR-0007), mirroring {@link RuneTable}'s
 * all-static, in-repo data idiom. Maps an owned item id to its {@link ConditionalBonus} (predicate +
 * per-style multiplier), or {@code null} when the item carries no curated bonus.
 *
 * <p><b>Keyed by raw item id, NOT by {@code ItemVariationMapping.map()}.</b> The design sketched a
 * canonical-collapse key (as the consumables path uses), but a build-time probe of the 1.12.31.1
 * client showed the collapse is <i>wrong</i> here: every black mask charge AND its imbued variants
 * collapse to one canonical (8901), every slayer helmet - plain, imbued, and every recolour - to
 * 11864, and all four salve forms (base / (e) / (i) / (ei)) to 4081. The imbue/enchant distinction is
 * exactly what carries the differing multipliers (imbued adds +15% ranged/magic; enchanted is +20%
 * melee), so collapsing would erase it. We therefore enumerate the raw ids explicitly per variant.
 * The id lists are generated from {@code net.runelite.api.ItemID} and split by imbue status (the
 * {@code _I} marker); a future game recolour is a one-line data add, consistent with FR-12.5.
 */
public final class ConditionalBonusRegistry
{
    // OSRS multipliers (the per-style factor on offence; 1.0 = no bonus). Verified against the wiki.
    private static final double ON_TASK = 1.1667;   // +16.67% (x7/6): black mask / slayer helm melee.
    private static final double IMBUE = 1.15;       // +15%: imbued ranged/magic uplift.
    private static final double ENCHANT = 1.20;     // +20%: enchanted salve (e)/(ei).

    // Slayer-helm / Black-mask family ids, generated from ItemID and split by imbue status (_I).
    private static final int[] BLACK_MASK_BASE =
    {
        8901, 8903, 8905, 8907, 8909, 8911, 8913, 8915, 8917, 8919, 8921
    };
    private static final int[] BLACK_MASK_IMBUED =
    {
        11774, 11775, 11776, 11777, 11778, 11779, 11780, 11781, 11782, 11783, 11784,
        25266, 25267, 25268, 25269, 25270, 25271, 25272, 25273, 25274, 25275, 25276,
        26771, 26772, 26773, 26774, 26775, 26776, 26777, 26778, 26779, 26780, 26781
    };
    private static final int[] SLAYER_HELMET_BASE =
    {
        11864, 19639, 19643, 19647, 21264, 21888, 23073, 24370, 25898, 25904, 25910, 29816,
        33066, 33338, 33340
    };
    private static final int[] SLAYER_HELMET_IMBUED =
    {
        11865, 19641, 19645, 19649, 21266, 21890, 23075, 24444, 25177, 25179, 25181, 25183,
        25185, 25187, 25189, 25191, 25900, 25902, 25906, 25908, 25912, 25914, 26674, 26675,
        26676, 26677, 26678, 26679, 26680, 26681, 26682, 26683, 26684, 29818, 29820, 29822,
        33068, 33070, 33072, 33439, 33441, 33443, 33445, 33447, 33449
    };

    /**
     * The full slayer-helmet family (base + recolours + imbued) this registry credits on-task -
     * the single source of truth for any consumer that must stay in lockstep with the DPS credit.
     * {@link UnlockGatedItems} composes its malevolent-masquerade guard map from this (FR-RV S1),
     * so a future recolour added above is automatically guarded; never fork a second copy.
     */
    static int[] slayerHelmetFamily()
    {
        int[] family = new int[SLAYER_HELMET_BASE.length + SLAYER_HELMET_IMBUED.length];
        System.arraycopy(SLAYER_HELMET_BASE, 0, family, 0, SLAYER_HELMET_BASE.length);
        System.arraycopy(SLAYER_HELMET_IMBUED, 0, family,
            SLAYER_HELMET_BASE.length, SLAYER_HELMET_IMBUED.length);
        return family;
    }

    // Salve-amulet family (few ids; the i/ei forms have alternate ids across game updates).
    private static final int SALVE_AMULET = 4081;
    private static final int SALVE_AMULET_E = 10588;
    private static final int[] SALVE_AMULET_I = {12017, 25250, 26763};
    private static final int[] SALVE_AMULET_EI = {12018, 25278, 26782};

    // Dragonbane (Dragon hunter lance): +20% accuracy AND damage vs draconic - symmetric melee, fits
    // the single-multiplier model exactly. A WEAPON-slot source: its multiplier is applied
    // multiplicatively inside weaponDps, and the weapon slot is excluded from the additive (m-1)*L
    // term, so it is never double-counted (ADR-0008 section 3.5).
    private static final int DRAGON_HUNTER_LANCE = 22978;
    private static final double DRAGONBANE = 1.20;

    // Dragonbane ranged/magic (WB-1, FR-C2 5.2) - both ASYMMETRIC (acc != dmg), weapon-slot only
    // (ADR-0009 A.1, Keris precedent). Wiki-verified 2026-07-01:
    //   DHCB "30% increase in ranged accuracy and 25% increase in damage" vs draconic;
    //   DH wand "accuracy is increased by 75% and damage increased by 40%" (buffed from 50%/20%
    //   in June 2025 - the current values, matching the FR-C2 audit).
    private static final int[] DRAGON_HUNTER_CROSSBOW = {21012, 25916, 25918}; // base + (t)/(b)
    private static final double DHCB_ACC = 1.30;
    private static final double DHCB_DMG = 1.25;
    private static final int DRAGON_HUNTER_WAND = 30070;
    private static final double DH_WAND_ACC = 1.75;
    private static final double DH_WAND_DMG = 1.40;

    // Demonbane (vs demons, symmetric acc/dmg). Speeds are read live from Bonuses, not the registry.
    private static final int ARCLIGHT = 19675;        // +70% (active form only; inactive 30305 excluded)
    private static final int EMBERLIGHT = 29589;      // +70% (same %s as Arclight)
    private static final int[] DARKLIGHT = {6746, 8281}; // +60% (base + Shadow-of-the-Storm variant)
    private static final int SCORCHING_BOW = 29591;   // +30% RANGED
    private static final double DEMONBANE_70 = 1.70;
    private static final double DEMONBANE_60 = 1.60;
    private static final double DEMONBANE_RANGED_30 = 1.30;

    // Keris family (vs kalphites): +33% DAMAGE only, no accuracy -> asymmetric. The 1/51 triple-damage
    // proc is folded in as an expected-value term: 1.33 * ((50/51)*1 + (1/51)*3) = 1.33 * 53/51 = 1.382
    // (DEC-5, deterministic; does not change ranking). A WEAPON-slot source, so the asymmetry is
    // allowed (ADR-0009 A.1/A.3).
    private static final int[] KERIS =
    {
        10581, 10582, 10583, 10584,        // Keris + Keris (p) variants
        25979, 25981, 27287, 27291, 30891  // Keris partisan + breaching/corruption/sun/amascut
    };
    private static final double KERIS_DMG = 1.382;

    // Wilderness weapons (vs Wilderness NPCs, symmetric +50%). Charged combat ids only (uncharged
    // excluded). A per-LOCATION predicate (ADR-0009 A.5) - only fires when a Wilderness location is
    // selected. On this dataset only Ankou's Wilderness location triggers it, so only Viggora's
    // (melee) is actually selected; the ranged/magic ones are latent (no ranged/magic Wildy task).
    private static final int VIGGORAS_CHAINMACE = 22545;      // melee
    private static final int CRAWS_BOW = 22550;               // ranged
    private static final int WEBWEAVER_BOW = 27655;           // ranged
    private static final int[] THAMMARONS_SCEPTRE = {22555, 27788}; // magic (+ autocast variant)
    private static final double WILDERNESS = 1.50;

    private static final Map<Integer, ConditionalBonus> BONUSES = new HashMap<>();

    static
    {
        // Slayer-helm / Black-mask: on-task. Base = melee only; imbued adds +15% ranged/magic.
        ConditionalBonus onTaskMelee = ConditionalBonus.symmetric(BonusCondition.ON_SLAYER_TASK, ON_TASK, 1.0, 1.0);
        ConditionalBonus onTaskImbued = ConditionalBonus.symmetric(BonusCondition.ON_SLAYER_TASK, ON_TASK, IMBUE, IMBUE);
        put(onTaskMelee, BLACK_MASK_BASE);
        put(onTaskMelee, SLAYER_HELMET_BASE);
        put(onTaskImbued, BLACK_MASK_IMBUED);
        put(onTaskImbued, SLAYER_HELMET_IMBUED);

        // Salve: vs undead. base = +16.67% melee; (e) = +20% melee; (i) = +16.67%/+15%; (ei) = +20% all.
        put(ConditionalBonus.symmetric(BonusCondition.VS_UNDEAD, ON_TASK, 1.0, 1.0), SALVE_AMULET);
        put(ConditionalBonus.symmetric(BonusCondition.VS_UNDEAD, ENCHANT, 1.0, 1.0), SALVE_AMULET_E);
        put(ConditionalBonus.symmetric(BonusCondition.VS_UNDEAD, ON_TASK, IMBUE, IMBUE), SALVE_AMULET_I);
        put(ConditionalBonus.symmetric(BonusCondition.VS_UNDEAD, ENCHANT, ENCHANT, ENCHANT), SALVE_AMULET_EI);

        // Dragon hunter lance: melee-only +20% acc/dmg vs draconic (symmetric).
        put(ConditionalBonus.symmetric(BonusCondition.VS_DRAGON, DRAGONBANE, 1.0, 1.0), DRAGON_HUNTER_LANCE);

        // Dragon hunter crossbow / wand: ranged +30%acc/+25%dmg, magic +75%acc/+40%dmg vs draconic.
        // Asymmetric -> allowed only because both are weapon-slot items (applied inside weaponDps/
        // magicWeaponDps, excluded from the additive (m-1)*L term).
        put(ConditionalBonus.asymmetric(BonusCondition.VS_DRAGON, 1.0, 1.0, DHCB_ACC, DHCB_DMG, 1.0, 1.0),
            DRAGON_HUNTER_CROSSBOW);
        put(ConditionalBonus.asymmetric(BonusCondition.VS_DRAGON, 1.0, 1.0, 1.0, 1.0, DH_WAND_ACC, DH_WAND_DMG),
            DRAGON_HUNTER_WAND);

        // Demonbane: melee weapons (Arclight/Emberlight +70%, Darklight +60%) + the ranged Scorching
        // bow (+30%). All symmetric (acc == dmg). Silverlight excluded (no verified %, DEC-7).
        put(ConditionalBonus.symmetric(BonusCondition.VS_DEMON, DEMONBANE_70, 1.0, 1.0), ARCLIGHT);
        put(ConditionalBonus.symmetric(BonusCondition.VS_DEMON, DEMONBANE_70, 1.0, 1.0), EMBERLIGHT);
        put(ConditionalBonus.symmetric(BonusCondition.VS_DEMON, DEMONBANE_60, 1.0, 1.0), DARKLIGHT);
        put(ConditionalBonus.symmetric(BonusCondition.VS_DEMON, 1.0, DEMONBANE_RANGED_30, 1.0), SCORCHING_BOW);

        // Keris: melee damage-only +38.2% (EV incl. proc) vs kalphites - asymmetric (acc 1.0).
        put(ConditionalBonus.asymmetric(BonusCondition.VS_KALPHITE, 1.0, KERIS_DMG, 1.0, 1.0, 1.0, 1.0),
            KERIS);

        // Wilderness weapons: +50% acc/dmg vs Wilderness NPCs (symmetric), one per style.
        put(ConditionalBonus.symmetric(BonusCondition.VS_WILDERNESS, WILDERNESS, 1.0, 1.0), VIGGORAS_CHAINMACE);
        put(ConditionalBonus.symmetric(BonusCondition.VS_WILDERNESS, 1.0, WILDERNESS, 1.0), CRAWS_BOW);
        put(ConditionalBonus.symmetric(BonusCondition.VS_WILDERNESS, 1.0, WILDERNESS, 1.0), WEBWEAVER_BOW);
        put(ConditionalBonus.symmetric(BonusCondition.VS_WILDERNESS, 1.0, 1.0, WILDERNESS), THAMMARONS_SCEPTRE);
    }

    private ConditionalBonusRegistry()
    {
    }

    /**
     * @return the curated conditional bonus for the owned item id, or {@code null} if the item is not
     *     a curated conditional source.
     */
    public static ConditionalBonus lookup(int itemId)
    {
        return BONUSES.get(itemId);
    }

    /**
     * @return an unmodifiable view of every curated entry ({@code itemId -> bonus}). Exposed for the
     *     invariant guard test (asymmetric bonuses live only on weapon-slot items, ADR-0009 A.1).
     */
    public static Map<Integer, ConditionalBonus> all()
    {
        return java.util.Collections.unmodifiableMap(BONUSES);
    }

    private static void put(ConditionalBonus bonus, int... ids)
    {
        for (int id : ids)
        {
            BONUSES.put(id, bonus);
        }
    }
}

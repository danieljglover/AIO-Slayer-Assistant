package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntPredicate;
import net.runelite.api.ItemID;

/**
 * WD-10 (ADR-0020 #4): the curated Void / Elite void set-bonus data - the only structural selector
 * change in Wave D. A complete Void set for a style (the style's helm + top + robe + gloves) applies a
 * loadout-level accuracy/damage multiplier; the elite top+robe supply a distinct, higher ranged and
 * magic damage bonus. Item ids are pinned from {@code net.runelite.api.ItemID} (the RuneTable /
 * ConditionalBonusRegistry discipline - Jagex's own generated names, authoritative in a way a
 * community list is not).
 *
 * <p>Void bonuses (OSRS, verified): melee +10% acc / +10% dmg; ranged +10% / +10% (elite +12.5% dmg);
 * magic +45% acc / +0% dmg (elite +2.5% dmg). Elite void carries NO melee bonus - the melee set stays
 * at +10%/+10% even with elite pieces. The elite bonus requires BOTH the elite top AND elite robe;
 * a single elite piece falls back to the regular set bonus.
 *
 * <p><b>Crystal armour + crystal weapon synergy</b> (Crystal_equipment, pageid 6902, revid 15196146,
 * 2026-04-25). Each active crystal armour piece boosts the crystal bow / bow of Faerdhinen ONLY - it is
 * weapon-conditional, unlike void. Verified per-piece (acc / dmg): helm +5% / +2.5%, body +15% / +7.5%,
 * legs +10% / +5%; full set +30% / +15%. The bonus is ADDITIVE per piece (a partial set still helps) and
 * stacks MULTIPLICATIVELY with the slayer helm's +15% ranged (the wiki's "body+legs with Slayer helm (i)
 * = ~29.375%" = 1.15 * 1.125), so the selector/estimator apply it as a product on top of the task
 * conditional, never folded into the "max, not product" conditional family.
 *
 * <p><b>Inquisitor's armour (crush synergy)</b> (Inquisitor's armour, pageid 240480, revid 15213319,
 * 2026-05-20). Each piece +0.5% crush acc/dmg; the full three-piece set adds an extra +1.0% for +2.5%
 * total. With the Inquisitor's mace equipped the effect TRIPLES to +2.5% per piece (7.5% full) with NO
 * extra full-set term. Applies only on the CRUSH attack style, so the selector runs this pass only when
 * the effective melee type is CRUSH. Symmetric (acc == dmg).
 */
public final class SetBonusRegistry
{
    // --- Void piece ids (net.runelite.api.ItemID) ----------------------------------------------
    private static final int VOID_MELEE_HELM = ItemID.VOID_MELEE_HELM;   // 11665
    private static final int VOID_RANGER_HELM = ItemID.VOID_RANGER_HELM; // 11664
    private static final int VOID_MAGE_HELM = ItemID.VOID_MAGE_HELM;     // 11663
    private static final int VOID_TOP = ItemID.VOID_KNIGHT_TOP;          // 8839
    private static final int VOID_ROBE = ItemID.VOID_KNIGHT_ROBE;        // 8840
    private static final int VOID_GLOVES = ItemID.VOID_KNIGHT_GLOVES;    // 8842
    private static final int ELITE_TOP = ItemID.ELITE_VOID_TOP;          // 13072
    private static final int ELITE_ROBE = ItemID.ELITE_VOID_ROBE;        // 13073

    // --- Crystal armour: ACTIVE pieces only (inactive/degraded carry no bonus), default Meilyr + all
    // eight clan recolours + deadman, enumerated as raw ids per the ConditionalBonusRegistry idiom (a
    // future recolour is a one-line add). Generated from net.runelite.api.ItemID by piece + active state.
    private static final int[] CRYSTAL_HELM =
    {
        23971, 27705, 27717, 27729, 27741, 27753, 27765, 27777, 33031, 33170
    };
    private static final int[] CRYSTAL_BODY =
    {
        23975, 27697, 27709, 27721, 27733, 27745, 27757, 27769, 33023, 33166
    };
    private static final int[] CRYSTAL_LEGS =
    {
        23979, 27701, 27713, 27725, 27737, 27749, 27761, 27773, 33027, 33168
    };
    // Crystal weapons the armour boosts: the reworked crystal bow (23983) and every bow of Faerdhinen -
    // the degrading base (25865) and the corrupted (c) + recolour variants. Inactive/legacy ids excluded.
    private static final int[] CRYSTAL_WEAPONS =
    {
        23983,                                                    // Crystal bow (reworked)
        25865,                                                    // Bow of faerdhinen (base, degrades)
        25867, 25869, 25884, 25886, 25888, 25890, 25892, 25894,  // Bow of faerdhinen (c) + recolours
        25896, 27187, 33021
    };
    // Per-piece crystal acc/dmg increments (fractions), verified above. Additive per piece.
    private static final double CRYSTAL_HELM_ACC = 0.05;
    private static final double CRYSTAL_HELM_DMG = 0.025;
    private static final double CRYSTAL_BODY_ACC = 0.15;
    private static final double CRYSTAL_BODY_DMG = 0.075;
    private static final double CRYSTAL_LEGS_ACC = 0.10;
    private static final double CRYSTAL_LEGS_DMG = 0.05;

    // --- Inquisitor's armour + mace (net.runelite.api.ItemID): base + recolour ids per piece.
    private static final int[] INQ_HELM = {24419, 27195};       // Inquisitor's great helm
    private static final int[] INQ_BODY = {24420, 27196};       // Inquisitor's hauberk
    private static final int[] INQ_LEGS = {24421, 27197};       // Inquisitor's plateskirt
    private static final int[] INQ_MACE = {24417, 27198};       // Inquisitor's mace
    private static final double INQ_PER_PIECE = 0.005;          // +0.5% crush per piece
    private static final double INQ_PER_PIECE_MACE = 0.025;     // +2.5% per piece with the mace
    private static final double INQ_FULL_SET_EXTRA = 0.01;      // +1.0% extra for the full 3-piece set

    private SetBonusRegistry()
    {
    }

    /**
     * The best Void set bonus the owned items complete for {@code style}, or {@code null} when the set
     * is incomplete. The helm must match the style (a melee helm does not complete a ranged set). Elite
     * pieces satisfy the base set and, when both are owned, upgrade the ranged/magic damage bonus.
     *
     * @param owns membership test over owned item ids ({@code OwnedItems::has})
     */
    public static SetBonus voidBonus(CombatStyle style, IntPredicate owns)
    {
        if (style == null)
        {
            return null;
        }
        int helm = helmFor(style);
        if (helm == 0 || !owns.test(helm) || !owns.test(VOID_GLOVES))
        {
            return null;
        }
        boolean topRegular = owns.test(VOID_TOP);
        boolean topElite = owns.test(ELITE_TOP);
        boolean robeRegular = owns.test(VOID_ROBE);
        boolean robeElite = owns.test(ELITE_ROBE);
        if ((!topRegular && !topElite) || (!robeRegular && !robeElite))
        {
            return null; // no top or no robe -> incomplete set
        }

        boolean elite = topElite && robeElite;
        int topId = topRegular ? VOID_TOP : ELITE_TOP;   // prefer the plain piece for a regular set
        int robeId = robeRegular ? VOID_ROBE : ELITE_ROBE;
        if (elite)
        {
            topId = ELITE_TOP;
            robeId = ELITE_ROBE;
        }

        Map<EquipmentSlot, Integer> pieces = new EnumMap<>(EquipmentSlot.class);
        pieces.put(EquipmentSlot.HEAD, helm);
        pieces.put(EquipmentSlot.BODY, topId);
        pieces.put(EquipmentSlot.LEGS, robeId);
        pieces.put(EquipmentSlot.HANDS, VOID_GLOVES);

        double acc = accMultiplier(style);
        double dmg = dmgMultiplier(style, elite);
        return new SetBonus(pieces, acc, dmg);
    }

    private static int helmFor(CombatStyle style)
    {
        switch (style)
        {
            case MELEE:
                return VOID_MELEE_HELM;
            case RANGED:
                return VOID_RANGER_HELM;
            case MAGIC:
                return VOID_MAGE_HELM;
            default:
                return 0;
        }
    }

    private static double accMultiplier(CombatStyle style)
    {
        return style == CombatStyle.MAGIC ? 1.45 : 1.10;
    }

    private static double dmgMultiplier(CombatStyle style, boolean elite)
    {
        switch (style)
        {
            case MELEE:
                return 1.10; // elite void carries no melee bonus
            case RANGED:
                return elite ? 1.125 : 1.10;
            case MAGIC:
                return elite ? 1.025 : 1.00;
            default:
                return 1.0;
        }
    }

    // --- Crystal armour + crystal weapon synergy ------------------------------------------------

    /** @return true when {@code id} is a crystal bow / bow of Faerdhinen the armour set boosts. */
    public static boolean isCrystalWeapon(int id)
    {
        return contains(CRYSTAL_WEAPONS, id);
    }

    /**
     * The combined crystal-armour accuracy/damage multiplier ({@code {acc, dmg}}, 1.0 = none) for the
     * pieces the predicate accepts - additive per piece (helm/body/legs at their verified rates). Shared
     * by the selector (over owned ids) and the display estimator (over worn ids) so both credit the same
     * synergy. Caller gates on a crystal weapon being worn (RANGED); this is armour-only.
     */
    public static double[] crystalMultipliers(IntPredicate has)
    {
        double acc = 1.0;
        double dmg = 1.0;
        if (anyOwned(has, CRYSTAL_HELM))
        {
            acc += CRYSTAL_HELM_ACC;
            dmg += CRYSTAL_HELM_DMG;
        }
        if (anyOwned(has, CRYSTAL_BODY))
        {
            acc += CRYSTAL_BODY_ACC;
            dmg += CRYSTAL_BODY_DMG;
        }
        if (anyOwned(has, CRYSTAL_LEGS))
        {
            acc += CRYSTAL_LEGS_ACC;
            dmg += CRYSTAL_LEGS_DMG;
        }
        return new double[] {acc, dmg};
    }

    /**
     * The crystal set bonus the owned items supply for a RANGED crystal-weapon loadout, or {@code null}
     * when no active crystal armour piece is owned. The pieces are exactly those owned (partial sets are
     * valid, being additive); the multipliers come from {@link #crystalMultipliers}. Caller must have
     * already confirmed the worn weapon is a crystal weapon ({@link #isCrystalWeapon}).
     */
    public static SetBonus crystalBonus(IntPredicate owns)
    {
        Map<EquipmentSlot, Integer> pieces = new EnumMap<>(EquipmentSlot.class);
        putFirstOwned(pieces, EquipmentSlot.HEAD, CRYSTAL_HELM, owns);
        putFirstOwned(pieces, EquipmentSlot.BODY, CRYSTAL_BODY, owns);
        putFirstOwned(pieces, EquipmentSlot.LEGS, CRYSTAL_LEGS, owns);
        if (pieces.isEmpty())
        {
            return null;
        }
        double[] m = crystalMultipliers(owns);
        return new SetBonus(pieces, m[0], m[1]);
    }

    // --- Inquisitor's armour (crush synergy) ---------------------------------------------------

    /** @return true when {@code id} is an Inquisitor's mace (base or recolour) - triples the set effect. */
    public static boolean isInquisitorMace(int id)
    {
        return contains(INQ_MACE, id);
    }

    /**
     * The combined Inquisitor accuracy/damage multiplier ({@code {acc, dmg}}, symmetric) for the pieces
     * the predicate accepts. Each piece is +0.5% (or +2.5% when the mace is equipped); the full
     * three-piece set (mace absent) adds a further +1.0%. Applies only on the CRUSH attack style - the
     * caller gates on that.
     */
    public static double[] inquisitorMultipliers(IntPredicate has, boolean maceEquipped)
    {
        int count = 0;
        if (anyOwned(has, INQ_HELM))
        {
            count++;
        }
        if (anyOwned(has, INQ_BODY))
        {
            count++;
        }
        if (anyOwned(has, INQ_LEGS))
        {
            count++;
        }
        double perPiece = maceEquipped ? INQ_PER_PIECE_MACE : INQ_PER_PIECE;
        double bonus = perPiece * count;
        if (!maceEquipped && count == 3)
        {
            bonus += INQ_FULL_SET_EXTRA; // the full-set term does not apply once the mace triples it
        }
        double m = 1.0 + bonus;
        return new double[] {m, m};
    }

    /**
     * The Inquisitor set bonus the owned items supply, or {@code null} when no piece is owned. The
     * multiplier depends on whether the worn weapon is the mace ({@code maceEquipped}); the caller (which
     * owns the weapon pick) supplies that. Pieces are exactly those owned (partial sets valid).
     */
    public static SetBonus inquisitorBonus(IntPredicate owns, boolean maceEquipped)
    {
        Map<EquipmentSlot, Integer> pieces = new EnumMap<>(EquipmentSlot.class);
        putFirstOwned(pieces, EquipmentSlot.HEAD, INQ_HELM, owns);
        putFirstOwned(pieces, EquipmentSlot.BODY, INQ_BODY, owns);
        putFirstOwned(pieces, EquipmentSlot.LEGS, INQ_LEGS, owns);
        if (pieces.isEmpty())
        {
            return null;
        }
        double[] m = inquisitorMultipliers(owns, maceEquipped);
        return new SetBonus(pieces, m[0], m[1]);
    }

    // --- shared helpers ------------------------------------------------------------------------

    private static boolean anyOwned(IntPredicate has, int[] ids)
    {
        for (int id : ids)
        {
            if (has.test(id))
            {
                return true;
            }
        }
        return false;
    }

    private static void putFirstOwned(Map<EquipmentSlot, Integer> pieces, EquipmentSlot slot, int[] ids,
        IntPredicate owns)
    {
        for (int id : ids)
        {
            if (owns.test(id))
            {
                pieces.put(slot, id);
                return;
            }
        }
    }

    private static boolean contains(int[] ids, int id)
    {
        for (int x : ids)
        {
            if (x == id)
            {
                return true;
            }
        }
        return false;
    }
}

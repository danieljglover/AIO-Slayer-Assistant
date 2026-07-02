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
}

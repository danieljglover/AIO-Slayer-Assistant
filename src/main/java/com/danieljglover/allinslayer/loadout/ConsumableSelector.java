package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemVariationMapping;

/**
 * Selects the consumable half of a loadout over owned items (plan section 5): the best owned food
 * (with a karambwan combo flag), the best owned style-matching potion (dose variants collapsed), and
 * - for magic tasks - the spell tier and rune requirement from the curated {@link RuneTable}.
 *
 * <p>Item effects are read through the {@link ConsumableEffectsProvider} seam (ADR-0004); rune costs
 * come from {@link RuneTable}. Nothing here touches a live {@code ItemStatChangesService}, so it is
 * headless-testable behind a fake. Each field is {@code null} when nothing suitable is owned (not an
 * error).
 */
@Singleton
public class ConsumableSelector
{
    private final ConsumableEffectsProvider fx;

    @Inject
    public ConsumableSelector(ConsumableEffectsProvider fx)
    {
        this.fx = fx;
    }

    /**
     * @param owned    every item the player has
     * @param style    the weakness combat style (potion matching; magic setup only for MAGIC)
     * @param element  the magic element ({@code air/water/earth/fire}); {@code null} -> auto-pick the
     *                 element whose castable tier hits hardest
     * @param player   the player's live skill levels (magic level gates the spell tier)
     * @param weaponId the chosen WEAPON id (for powered/elemental staff handling); may be {@code null}
     * @return the consumable picks; any field {@code null} when nothing suitable is owned
     */
    public Consumables select(OwnedItems owned, CombatStyle style, String element, PlayerStats player,
        Integer weaponId)
    {
        return select(owned, style, element, player, weaponId, false);
    }

    /**
     * As {@link #select(OwnedItems, CombatStyle, String, PlayerStats, Integer)}, but on a
     * {@code multicombat} task an affordable Ancient Barrage/Burst is preferred over the standard-book
     * spell (WD-9, ADR-0020 #4). Single-target tasks are byte-identical to today (FR-6).
     */
    public Consumables select(OwnedItems owned, CombatStyle style, String element, PlayerStats player,
        Integer weaponId, boolean multicombat)
    {
        Integer foodId = bestFood(owned);
        Integer comboFoodId = owned.has(RuneTable.KARAMBWAN_ID)
            && !Integer.valueOf(RuneTable.KARAMBWAN_ID).equals(foodId)
            ? RuneTable.KARAMBWAN_ID
            : null;
        Integer potionId = bestPotion(owned, style);
        MagicSetup magic = style == CombatStyle.MAGIC
            ? buildMagic(owned, element, player, weaponId, multicombat)
            : null;
        return new Consumables(foodId, comboFoodId, potionId, magic);
    }

    // --- food (FR-4) ---------------------------------------------------------------------------

    private Integer bestFood(OwnedItems owned)
    {
        Integer best = null;
        int bestHeal = Integer.MIN_VALUE;
        for (int id : owned.ids())
        {
            Integer heal = fx.healAmount(id);
            if (heal == null)
            {
                continue; // not food
            }
            if (heal > bestHeal || (heal == bestHeal && (best == null || id < best)))
            {
                bestHeal = heal;
                best = id;
            }
        }
        return best;
    }

    // --- potion (FR-5) -------------------------------------------------------------------------

    private Integer bestPotion(OwnedItems owned, CombatStyle style)
    {
        Set<BoostedStat> styleSet = styleStats(style);
        if (styleSet.isEmpty())
        {
            return null;
        }

        // Collapse dose variants: keep one representative (highest magnitude, then lowest id) per
        // ItemVariationMapping canonical so all doses of a potion count once (FR-5).
        Map<Integer, PotionCandidate> byCanonical = new HashMap<>();
        for (int id : owned.ids())
        {
            Set<BoostedStat> boosts = fx.boostedStats(id);
            if (boosts.isEmpty() || Collections.disjoint(boosts, styleSet))
            {
                continue; // not a potion, or none of its boosts match the style
            }
            int magnitude = 0;
            Map<BoostedStat, Integer> magnitudes = fx.boostMagnitudes(id);
            for (BoostedStat stat : styleSet)
            {
                if (boosts.contains(stat))
                {
                    magnitude += magnitudes.getOrDefault(stat, 0);
                }
            }
            int canonical = canonical(id);
            PotionCandidate current = byCanonical.get(canonical);
            if (current == null || magnitude > current.magnitude
                || (magnitude == current.magnitude && id < current.id))
            {
                byCanonical.put(canonical, new PotionCandidate(id, magnitude));
            }
        }

        PotionCandidate winner = null;
        for (PotionCandidate c : byCanonical.values())
        {
            if (winner == null || c.magnitude > winner.magnitude
                || (c.magnitude == winner.magnitude && c.id < winner.id))
            {
                winner = c;
            }
        }
        return winner == null ? null : winner.id;
    }

    private static Set<BoostedStat> styleStats(CombatStyle style)
    {
        if (style == null)
        {
            return EnumSet.noneOf(BoostedStat.class);
        }
        switch (style)
        {
            case MELEE:
                return EnumSet.of(BoostedStat.ATTACK, BoostedStat.STRENGTH, BoostedStat.DEFENCE);
            case RANGED:
                return EnumSet.of(BoostedStat.RANGED);
            case MAGIC:
                return EnumSet.of(BoostedStat.MAGIC);
            default:
                return EnumSet.noneOf(BoostedStat.class);
        }
    }

    private static int canonical(int itemId)
    {
        try
        {
            return ItemVariationMapping.map(itemId);
        }
        catch (RuntimeException e)
        {
            return itemId;
        }
    }

    // --- magic runes / spell (FR-6) ------------------------------------------------------------

    private MagicSetup buildMagic(OwnedItems owned, String element, PlayerStats player, Integer weaponId,
        boolean multicombat)
    {
        // The spell/staff/affordability resolution lives once in MagicWeaponEvaluator (ADR-0009 B.2),
        // shared with GearSelector's magic ranking - no duplicated RuneTable handling here.
        return MagicWeaponEvaluator.evaluate(owned, element, player, weaponId, multicombat);
    }

    private static final class PotionCandidate
    {
        private final int id;
        private final int magnitude;

        private PotionCandidate(int id, int magnitude)
        {
            this.id = id;
            this.magnitude = magnitude;
        }
    }
}

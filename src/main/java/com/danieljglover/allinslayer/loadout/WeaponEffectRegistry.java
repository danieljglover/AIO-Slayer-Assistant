package com.danieljglover.allinslayer.loadout;

import java.util.HashMap;
import java.util.Map;

/**
 * Curated registry of passive special-weapon mechanics (ADR-0008 section 3.4a), mirroring the
 * all-static, in-repo idiom of {@link RuneTable} / {@link ConditionalBonusRegistry}. Maps an owned
 * item id to its {@link WeaponEffect}, defaulting to {@link WeaponEffect#NONE} for any weapon without
 * a curated passive (i.e. today's normal single-roll, single-hit maths).
 *
 * <p><b>Keyed by raw item id.</b> Unlike the gear conditional registry, variant/imbue collapse is
 * irrelevant here - these ids are weapons with cosmetic ornament/recolour variants that are
 * combat-identical, so each combat-capable id is listed explicitly. Adding a future passive weapon is
 * one data row + one test row; no estimator/selector control-flow change.
 *
 * <p><b>Uncharged forms are deliberately excluded.</b> An uncharged Scythe of Vitur / Osmumten's fang
 * is not the combat form (the player charges it before use); crediting the multi-hit / reroll to an
 * uncharged weapon would over-rank it. Uncharged ids fall through to {@code NONE}.
 */
public final class WeaponEffectRegistry
{
    // Osmumten's fang - rolls accuracy twice (stab). Damage band 15-85% keeps the mean at 0.5*max,
    // so it is DPS-neutral on damage (variance only) -> no damage multiplier.
    private static final WeaponEffect FANG = new WeaponEffect(2, 1.0);

    // Scythe of vitur - hits a >=2-tile target 3 times at 100/50/25% of max, each rolled
    // independently -> mean per swing = p*(max/2)*(1 + 0.5 + 0.25) = 1.75x average damage.
    private static final WeaponEffect SCYTHE = new WeaponEffect(1, 1.75);

    // Charged / cosmetic-recolour combat ids (from net.runelite.api.ItemID). Uncharged ids omitted.
    private static final int[] FANG_IDS =
    {
        26219, // OSMUMTENS_FANG
        27246, // OSMUMTENS_FANG_OR (cosmetic)
        33174  // OSMUMTENS_FANG_33174 (variant)
    };
    private static final int[] SCYTHE_IDS =
    {
        22325, // SCYTHE_OF_VITUR
        22664, // SCYTHE_OF_VITUR_22664 (variant)
        25736, // HOLY_SCYTHE_OF_VITUR (cosmetic)
        25739, // SANGUINE_SCYTHE_OF_VITUR (cosmetic)
        28543  // CORRUPTED_SCYTHE_OF_VITUR (cosmetic)
    };

    private static final Map<Integer, WeaponEffect> EFFECTS = new HashMap<>();

    static
    {
        put(FANG, FANG_IDS);
        put(SCYTHE, SCYTHE_IDS);
    }

    private WeaponEffectRegistry()
    {
    }

    /**
     * @return the curated passive effect for the owned item id, or {@link WeaponEffect#NONE} when the
     *     weapon carries no curated passive (normal single-roll, single-hit maths).
     */
    public static WeaponEffect lookup(int itemId)
    {
        return EFFECTS.getOrDefault(itemId, WeaponEffect.NONE);
    }

    private static void put(WeaponEffect effect, int... ids)
    {
        for (int id : ids)
        {
            EFFECTS.put(id, effect);
        }
    }
}

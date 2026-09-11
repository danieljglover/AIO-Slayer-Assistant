/*
 * Weapon Charges compatibility data derived from geheur/weapon-charges.
 * Copyright (c) 2021, geheur. BSD-2-Clause.
 * Full notice: src/main/resources/META-INF/LICENSE-weapon-charges.txt.
 */
package com.danieljglover.allinslayer.integration.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * Reviewed against Plugin Hub weapon-charges-2 commit
 * 8da860f3628cbd4fc72ba09cfdff4b67ad88bac9 (2026-09-11).
 * See docs/reconstruction/weapon-charges-integration.md for the update procedure.
 */
final class WeaponChargeMappings
{
    static final Map<Integer, String> ITEMS = items();
    static final Map<String, Integer> DARTS = darts();

    private WeaponChargeMappings() { }

    private static Map<Integer, String> items()
    {
        Map<Integer, String> result = new LinkedHashMap<>();
        // Only upstream chargedItemIds qualify. An uncharged form must not inherit a
        // saved balance from a different item. These are balance keys, not settingsConfigKey.
        add(result, "ibans_staff", 1409, 12658);
        add(result, "trident_of_the_seas", 11907, 33322);
        add(result, "trident_of_the_swamp", 12899, 33314);
        add(result, "trident_of_the_seas_e", 22288, 33326);
        add(result, "trident_of_the_swamp_e", 22292, 33318);
        add(result, "warped_sceptre", 28585);
        add(result, "abyssal_tentacle", 12006, 26484);
        add(result, "crystal_halberd", 23987);
        add(result, "tome_of_fire", 20714);
        add(result, "tome_of_water", 25574);
        add(result, "scythe_of_vitur", 22325, 25736, 25739);
        add(result, "amulet_of_blood_fury", 24780);
        add(result, "sanguinesti_staff", 22323, 25731);
        add(result, "arclight", 19675);
        // Upstream stores usable ether charges; the activation deposit is not in these balances.
        add(result, "craws_bow", 22550);
        add(result, "webweaver_bow", 27655);
        add(result, "viggoras_chainmace", 22545);
        add(result, "ursine_chainmace", 27660);
        add(result, "thammarons_sceptre", 22555, 27788);
        add(result, "accursed_sceptre", 27665, 27679);
        add(result, "crystal_bow", 23983);
        add(result, "bow_of_faerdhinen", 25865);
        add(result, "blade_of_saeldor", 23995);
        add(result, "crystal_helm", 23971, 27705, 27717, 27729, 27741, 27753, 27765, 27777);
        add(result, "crystal_body", 23975, 27697, 27709, 27721, 27733, 27745, 27757, 27769);
        add(result, "crystal_legs", 23979, 27701, 27713, 27725, 27737, 27749, 27761, 27773);
        add(result, "serpentine_helm", 12931, 13197, 13199);
        add(result, "tumekens_shadow", 27275);
        // Blowpipe has no single upstream balance key: the bridge reads scales, darts and type.
        add(result, "blowpipe", 12926, 28688);
        add(result, "venator_bow", 27610, ItemID.VENATOR_BOW_ORNAMENT);
        add(result, "eye_of_ayak", 31113);
        return Collections.unmodifiableMap(result);
    }

    private static void add(Map<Integer, String> items, String key, int... ids)
    {
        for (int id : ids)
        {
            if (items.putIfAbsent(id, key) != null)
            {
                throw new IllegalArgumentException("Duplicate Weapon Charges item mapping: " + id);
            }
        }
    }

    private static Map<String, Integer> darts()
    {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("BRONZE", ItemID.BRONZE_DART);
        result.put("IRON", ItemID.IRON_DART);
        result.put("STEEL", ItemID.STEEL_DART);
        result.put("MITHRIL", ItemID.MITHRIL_DART);
        result.put("ADAMANT", ItemID.ADAMANT_DART);
        result.put("RUNE", ItemID.RUNE_DART);
        result.put("AMETHYST", ItemID.AMETHYST_DART);
        result.put("DRAGON", ItemID.DRAGON_DART);
        // UNKNOWN (-1), missing and future enum names cannot identify loaded ammunition.
        return Collections.unmodifiableMap(result);
    }
}

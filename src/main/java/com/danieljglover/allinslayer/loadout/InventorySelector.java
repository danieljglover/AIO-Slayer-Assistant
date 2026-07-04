package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.ArrayList;
import java.util.List;

/**
 * Owned-driven inventory supplies for a task (ADR-0017 #4, DT-B9). Fills the previously always-empty
 * {@link Recommendation#getInventory()} with the pack items the player actually owns for THIS
 * variant/location:
 *
 * <ul>
 *   <li>the task's {@code requiredItemId} (compiled by DT-B1) when owned (PD-4);</li>
 *   <li>the Dwarf multicannon parts + cannonballs when the effective location is cannonable and the
 *       player owns a cannon (PD-2, owned-driven - {@code config.haveCannon()} stays only as the
 *       {@code chooseLocation} bias);</li>
 *   <li>antifire protection (potion or shield) when the monster is draconic and the player owns some
 *       (PD-3, off the {@code dragon} flag).</li>
 * </ul>
 *
 * <p>The list stays {@code List<Integer>} (no quantities, ADR-0017) and renders via the existing
 * {@code InventoryGrid}. Every item added is one the player owns (owned-only, ADR-0001); nothing is
 * added for an unowned trigger. Item ids are pinned from {@code net.runelite.api.ItemID} (the same
 * no-fabrication discipline as {@code RuneTable}/{@code WeaponEffectRegistry}); {@code
 * InventorySelectorTest} pins one behaviour row per id against the real constant.
 */
public final class InventorySelector
{
    // --- Dwarf multicannon: the four parts + ammo (net.runelite.api.ItemID) --------------------
    private static final int CANNON_BASE = 6;
    private static final int CANNON_STAND = 8;
    private static final int CANNON_BARRELS = 10;
    private static final int CANNON_FURNACE = 12;
    private static final int[] CANNON_PARTS = {CANNON_BASE, CANNON_STAND, CANNON_BARRELS, CANNON_FURNACE};

    private static final int STEEL_CANNONBALL = 2;
    private static final int GRANITE_CANNONBALL = 21728;
    /** Cannonball preference order; the first the player owns is added (owned-only). */
    private static final int[] CANNONBALLS = {GRANITE_CANNONBALL, STEEL_CANNONBALL};

    // --- Antifire protection: potions (best/most doses first) then shields (net.runelite.api.ItemID)
    private static final int[] ANTIFIRE = {
        21978, 21981, 21984, 21987, // Super antifire potion (4..1)
        22209, 22212, 22215, 22218, // Extended super antifire (4..1)
        11951, 11953, 11955, 11957, // Extended antifire (4..1)
        2452, 2454, 2456, 2458,     // Antifire potion (4..1)
        22002,                      // Dragonfire ward
        21633,                      // Ancient wyvern shield
        11283,                      // Dragonfire shield
    };

    // --- Slayer bracelets (net.runelite.api.ItemID). These change task-kill accounting, NOT DPS, so
    // they are ONLY ever inventory suggestions - never fed to any combat maths (NG-4). Verified item
    // ids (OSRS Wiki): Expeditious bracelet 21177 (id=21177, revid 15188169) counts some kills twice to
    // finish tasks faster; Bracelet of slaughter 21183 (id=21183, revid 15188180) extends tasks by
    // sometimes not decrementing. Preference order below suggests the faster (expeditious) when both are
    // owned - the default "get the task done" bias; ambiguity resolves to whichever single one is owned.
    private static final int EXPEDITIOUS_BRACELET = 21177;
    private static final int BRACELET_OF_SLAUGHTER = 21183;
    private static final int[] SLAYER_BRACELETS = {EXPEDITIOUS_BRACELET, BRACELET_OF_SLAUGHTER};

    /** The PD-3 "note when unowned" nudge (DT-B11): shown when draconic but no antifire is owned. */
    private static final String ANTIFIRE_UNOWNED_NOTE =
        "Antifire recommended - none found in your bank.";

    private InventorySelector()
    {
    }

    /**
     * The owned pack-list for the recommendation. Order: required item, cannon (parts + one owned
     * cannonball), antifire. Never null; empty when nothing owned applies.
     *
     * @param owned the player's items across inventory/worn/bank; null -> empty list
     * @param profile the resolved monster profile (its {@code dragon} flag gates antifire)
     * @param effectiveLocation the suggested-or-selected location (its {@code cannon} flag gates cannon)
     * @param task the assignment (its {@code requiredItemId} is the required-item supply)
     */
    public static List<Integer> select(OwnedItems owned, MonsterProfile profile,
        SlayerLocation effectiveLocation, TaskData task)
    {
        List<Integer> supplies = new ArrayList<>();
        if (owned == null)
        {
            return supplies;
        }

        // Required item (DT-B1's requiredItemId), when owned (PD-4 pack-list).
        if (task != null && task.getRequiredItemId() != null && owned.has(task.getRequiredItemId()))
        {
            supplies.add(task.getRequiredItemId());
        }

        // Cannon + cannonballs, when the location is cannonable and the player owns a full cannon
        // (PD-2, owned-driven). The four parts are packed; a cannonball is added only when owned
        // (owned-only - no fabricated ammo).
        if (effectiveLocation != null && effectiveLocation.isCannonEffective() && ownsCannon(owned))
        {
            for (int part : CANNON_PARTS)
            {
                supplies.add(part);
            }
            Integer cannonball = firstOwned(owned, CANNONBALLS);
            if (cannonball != null)
            {
                supplies.add(cannonball);
            }
        }

        // Antifire protection, when the monster is draconic and the player owns some (PD-3, off the
        // dragon flag). The single best owned antifire (potion preferred, most doses first) is added.
        if (profile != null && profile.isDragon())
        {
            Integer antifire = firstOwned(owned, ANTIFIRE);
            if (antifire != null)
            {
                supplies.add(antifire);
            }
        }

        // Slayer bracelet, when owned: a task-pace supply (expeditious to finish faster, slaughter to
        // extend), never a combat-maths input (NG-4). Every task the plugin advises IS a Slayer task, so
        // this is unconditionally relevant when a bracelet is owned. Only one is suggested (the preference
        // order prefers expeditious); the player swaps to slaughter deliberately if they want longer tasks.
        Integer bracelet = firstOwned(owned, SLAYER_BRACELETS);
        if (bracelet != null)
        {
            supplies.add(bracelet);
        }
        return supplies;
    }

    /**
     * The PD-3 "note when unowned" antifire nudge (DT-B11): a non-null recommendation string when the
     * monster is draconic ({@code profile.dragon}) AND the player owns no antifire protection - the
     * unowned counterpart to {@link #select}'s owned-antifire supply. Null when the monster is not
     * draconic, or when antifire is already owned (the supply covers that case). A NOTE only - never a
     * combat-maths change (NG-4).
     *
     * @param owned the player's items across inventory/worn/bank; null -> treated as none owned
     * @param profile the resolved monster profile (its {@code dragon} flag gates the note)
     */
    public static String antifireNote(OwnedItems owned, MonsterProfile profile)
    {
        if (profile == null || !profile.isDragon())
        {
            return null;
        }
        if (owned != null && firstOwned(owned, ANTIFIRE) != null)
        {
            return null; // owns antifire -> the select() supply handles it, no nudge needed
        }
        return ANTIFIRE_UNOWNED_NOTE;
    }

    /** True only when the player owns all four Dwarf multicannon parts (an assembled cannon). */
    static boolean ownsCannon(OwnedItems owned)
    {
        for (int part : CANNON_PARTS)
        {
            if (!owned.has(part))
            {
                return false;
            }
        }
        return true;
    }

    /** The first id in {@code ids} (priority order) the player owns, or null when none is owned. */
    private static Integer firstOwned(OwnedItems owned, int[] ids)
    {
        for (int id : ids)
        {
            if (owned.has(id))
            {
                return id;
            }
        }
        return null;
    }
}

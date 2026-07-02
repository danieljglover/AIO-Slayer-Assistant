package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.loadout.RuneTable.Tier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The single source of truth for "given this weapon, the best castable spell or powered-staff base
 * max hit" (ADR-0009 B.2). Extracted from {@code ConsumableSelector.buildMagic} so BOTH the consumable
 * selector (final rune requirement) and {@code GearSelector} (gear-aware magic weapon ranking) call
 * one implementation - no duplicated {@link RuneTable} handling, no spell-ordering inversion.
 *
 * <p>All-static, pure over {@link RuneTable} + owned items + player levels (no {@code ItemManager} /
 * EDT, NFR-3). The cast SPEED is determined by the caller from the weapon's resolved
 * {@link Bonuses#getAttackSpeedTicks()} via {@link #castSpeedTicks} (powered staves carry their cast
 * speed there; standard casters cast at 5 ticks regardless of the wand's melee speed).
 */
public final class MagicWeaponEvaluator
{
    private static final String[] ELEMENTS = {"air", "water", "earth", "fire"};

    /** Standard spellbook / Ancients autocast speed (ticks) - NOT the wand's melee speed. */
    public static final int STANDARD_CAST_SPEED = 5;

    // Tumeken's Shadow (charged + corrupted-cosmetic) - multiplies OTHER gear's magic bonuses x3.
    private static final int TUMEKENS_SHADOW = 27275;
    private static final int CORRUPTED_TUMEKENS_SHADOW = 28547;
    private static final int SHADOW_GEAR_MULTIPLIER = 3;
    private static final int SHADOW_MAGIC_DAMAGE_CAP = 100; // +100% cap on the tripled gear damage

    private MagicWeaponEvaluator()
    {
    }

    /** @return whether the weapon id is Tumeken's Shadow (which triples worn-gear magic bonuses). */
    public static boolean isTumekensShadow(int weaponId)
    {
        return weaponId == TUMEKENS_SHADOW || weaponId == CORRUPTED_TUMEKENS_SHADOW;
    }

    /**
     * The effective total magic attack for a magic weapon: weapon's own magic attack plus the worn
     * non-weapon gear's magic attack - tripled for Tumeken's Shadow (ADR-0009 B.4). The Shadow's own
     * +35 attack is in {@code weaponAmagic} and is added once (not tripled).
     */
    public static int effectiveMagicAttack(int weaponId, int weaponAmagic, int gearMatt)
    {
        int gear = isTumekensShadow(weaponId) ? SHADOW_GEAR_MULTIPLIER * gearMatt : gearMatt;
        return weaponAmagic + gear;
    }

    /**
     * The effective total magic damage % for a magic weapon: weapon's own magic damage plus the worn
     * gear's magic damage - tripled for Tumeken's Shadow, with the tripled GEAR contribution capped at
     * +100% (ADR-0009 B.4; x4-in-ToA is not modelled).
     */
    public static int effectiveMagicDamage(int weaponId, int weaponMdmg, int gearMdmg)
    {
        int gear = isTumekensShadow(weaponId)
            ? Math.min(SHADOW_MAGIC_DAMAGE_CAP, SHADOW_GEAR_MULTIPLIER * gearMdmg)
            : gearMdmg;
        return weaponMdmg + gear;
    }

    /**
     * @param owned    every item the player has
     * @param element  the magic element ({@code air/water/earth/fire}); {@code null} -> auto-pick the
     *                 element whose castable tier hits hardest (prefer affordable)
     * @param player   the player's live skill levels (magic level gates the spell tier)
     * @param weaponId the chosen WEAPON id (powered/elemental staff handling); may be {@code null}
     * @return the magic setup: spell/staff, base max hit, powered-staff flag, rune requirement + short
     */
    public static MagicSetup evaluate(OwnedItems owned, String element, PlayerStats player,
        Integer weaponId)
    {
        return evaluate(owned, element, player, weaponId, false);
    }

    /**
     * As {@link #evaluate(OwnedItems, String, PlayerStats, Integer)}, but on a {@code multicombat} task
     * an affordable Ancient Barrage/Burst is preferred over the standard-book spell (WD-9, ADR-0020 #4):
     * the strongest multi-target spell the Magic level allows and the owned runes afford is chosen. If
     * no Ancient is affordable, or the task is single-target, the standard-book behaviour is unchanged
     * (FR-6). A powered staff supplies its own attack and cannot autocast Ancients, so it is unaffected.
     */
    public static MagicSetup evaluate(OwnedItems owned, String element, PlayerStats player,
        Integer weaponId, boolean multicombat)
    {
        // A powered staff supplies its own attack: no runes, the staff's curated base max hit.
        if (weaponId != null && RuneTable.isPoweredStaff(weaponId))
        {
            Integer maxHit = RuneTable.poweredStaffMaxHit(weaponId);
            return new MagicSetup(element, null, maxHit == null ? 0 : maxHit, true,
                Collections.emptyMap(), Collections.emptyMap());
        }

        Set<String> supplied = weaponId == null
            ? Collections.emptySet()
            : RuneTable.elementsSuppliedBy(weaponId);
        int magicLevel = player == null ? 0 : player.getMagic();

        if (multicombat)
        {
            MagicSetup ancient = bestAffordableAncient(element, magicLevel, supplied, owned);
            if (ancient != null)
            {
                return ancient;
            }
        }

        if (element != null)
        {
            return buildMagicForElement(element, magicLevel, supplied, owned);
        }

        // Element unknown: pick the element whose castable tier hits hardest (prefer affordable).
        MagicSetup best = null;
        boolean bestAffordable = false;
        for (String e : ELEMENTS)
        {
            MagicSetup setup = buildMagicForElement(e, magicLevel, supplied, owned);
            boolean affordable = setup.getSpellName() != null && setup.getRunesShort().isEmpty();
            if (best == null
                || (affordable && !bestAffordable)
                || (affordable == bestAffordable
                    && setup.getSpellBaseMaxHit() > best.getSpellBaseMaxHit()))
            {
                best = setup;
                bestAffordable = affordable;
            }
        }
        return best;
    }

    /**
     * The cast speed (ticks) for a magic weapon: a powered staff casts at its own attack speed (from
     * {@link Bonuses#getAttackSpeedTicks()}, falling back to 5 if unknown); a standard caster always
     * casts at 5 ticks, NOT the wand's melee speed (ADR-0009 B.6).
     */
    public static int castSpeedTicks(boolean poweredStaff, int weaponAttackSpeedTicks)
    {
        if (poweredStaff && weaponAttackSpeedTicks > 0)
        {
            return weaponAttackSpeedTicks;
        }
        return STANDARD_CAST_SPEED;
    }

    private static MagicSetup buildMagicForElement(String element, int magicLevel, Set<String> supplied,
        OwnedItems owned)
    {
        MagicSetup highestLevelAllowed = null;
        for (Tier tier : RuneTable.tiersDescending()) // SURGE -> BOLT
        {
            if (magicLevel < RuneTable.level(element, tier))
            {
                continue; // level gate
            }
            Map<Integer, Integer> requirement = adjustedRequirement(element, tier, supplied);
            Map<Integer, Integer> shortfall = shortfall(requirement, owned);
            MagicSetup setup = new MagicSetup(element, RuneTable.spellName(element, tier),
                RuneTable.baseMaxHit(element, tier), false,
                Collections.unmodifiableMap(requirement), Collections.unmodifiableMap(shortfall));
            if (highestLevelAllowed == null)
            {
                highestLevelAllowed = setup; // first (strongest) tier the level allows
            }
            if (shortfall.isEmpty())
            {
                return setup; // highest affordable tier
            }
        }
        if (highestLevelAllowed != null)
        {
            return highestLevelAllowed; // no tier affordable -> strongest level-allowed + shortfall
        }
        // Magic level too low for even the weakest tier: no castable spell.
        return new MagicSetup(element, null, 0, false,
            Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * The strongest Ancient multi-target spell (Barrages before Bursts) the Magic level allows AND the
     * owned runes fully afford (an equipped elemental staff's supplied runes are free). {@code null} if
     * none is affordable -> the caller falls back to the standard book (WD-9).
     */
    private static MagicSetup bestAffordableAncient(String element, int magicLevel, Set<String> supplied,
        OwnedItems owned)
    {
        for (RuneTable.AncientSpell spell : RuneTable.ancientsDescending())
        {
            if (magicLevel < spell.getLevel())
            {
                continue; // level gate
            }
            Map<Integer, Integer> requirement = adjustedRequirement(spell.getRunes(), supplied);
            Map<Integer, Integer> shortfall = shortfall(requirement, owned);
            if (shortfall.isEmpty())
            {
                return new MagicSetup(element, spell.getName(), spell.getMaxHit(), false,
                    Collections.unmodifiableMap(requirement), Collections.emptyMap());
            }
        }
        return null;
    }

    /** The spell's rune requirement, with any rune the equipped staff supplies removed. */
    private static Map<Integer, Integer> adjustedRequirement(String element, Tier tier,
        Set<String> supplied)
    {
        return adjustedRequirement(RuneTable.requirement(element, tier), supplied);
    }

    /** A rune requirement with any rune the equipped staff supplies removed (shared by spell/ancient). */
    private static Map<Integer, Integer> adjustedRequirement(Map<Integer, Integer> runes,
        Set<String> supplied)
    {
        Map<Integer, Integer> requirement = new LinkedHashMap<>(runes);
        for (String suppliedElement : supplied)
        {
            int rune = runeFor(suppliedElement);
            if (rune != -1)
            {
                requirement.remove(rune);
            }
        }
        return requirement;
    }

    private static Map<Integer, Integer> shortfall(Map<Integer, Integer> requirement, OwnedItems owned)
    {
        Map<Integer, Integer> shortfall = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : requirement.entrySet())
        {
            int missing = e.getValue() - owned.quantity(e.getKey());
            if (missing > 0)
            {
                shortfall.put(e.getKey(), missing);
            }
        }
        return shortfall;
    }

    private static int runeFor(String element)
    {
        switch (element)
        {
            case "water":
                return RuneTable.WATER_RUNE;
            case "earth":
                return RuneTable.EARTH_RUNE;
            case "fire":
                return RuneTable.FIRE_RUNE;
            case "air":
                return RuneTable.AIR_RUNE;
            default:
                return -1;
        }
    }
}

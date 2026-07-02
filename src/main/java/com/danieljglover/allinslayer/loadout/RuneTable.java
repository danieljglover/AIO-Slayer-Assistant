package com.danieljglover.allinslayer.loadout;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Curated standard-spellbook elemental combat data (ADR-0004, research C.4). The live API carries no
 * spell rune costs, so this table is owned in-repo: for each element (air/water/earth/fire) and tier
 * (Bolt/Blast/Wave/Surge) it gives the per-cast rune requirement ({@code runeId -> count}, including
 * the per-tier combat rune), the Magic level required, the spell's base max hit, and the spell name.
 *
 * <p>It also curates the powered-staff id set (with each staff's base max hit, since they supply
 * their own attack and need no runes), the elemental/combination staff and tome id -> supplied
 * element(s) map (an owned supplier zeroes that element's rune in the requirement), and the cooked
 * karambwan id (combo food).
 */
public final class RuneTable
{
    /** Spell tiers, weakest to strongest. */
    public enum Tier
    {
        BOLT, BLAST, WAVE, SURGE
    }

    // --- Rune item ids -------------------------------------------------------------------------
    public static final int AIR_RUNE = 556;
    public static final int WATER_RUNE = 555;
    public static final int EARTH_RUNE = 557;
    public static final int FIRE_RUNE = 554;
    public static final int CHAOS_RUNE = 562; // Bolt
    public static final int DEATH_RUNE = 560; // Blast
    public static final int BLOOD_RUNE = 565; // Wave
    public static final int WRATH_RUNE = 21880; // Surge
    public static final int SOUL_RUNE = 566; // Ancient Shadow/Blood spells

    /** Cooked karambwan - eaten on the same tick as another food (combo-eat, FR-4). */
    public static final int KARAMBWAN_ID = 3144;

    private static final List<Tier> TIERS_DESCENDING =
        Collections.unmodifiableList(java.util.Arrays.asList(Tier.SURGE, Tier.WAVE, Tier.BLAST, Tier.BOLT));

    /** element -> tier -> spell. */
    private static final Map<String, Map<Tier, Spell>> SPELLS = new HashMap<>();

    /** powered staff item id -> base max hit (the staff supplies its own attack; no runes needed). */
    private static final Map<Integer, Integer> POWERED_STAFF_MAX_HIT = new HashMap<>();

    /** elemental/combination staff or tome item id -> the element(s) it supplies unlimited. */
    private static final Map<Integer, Set<String>> STAFF_ELEMENTS = new HashMap<>();

    /**
     * Ancient Magicks multi-target combat spells (Burst 3x3 / Barrage 3x3), strongest base max hit
     * first (ADR-0020 #4). Curated in-repo like the standard spellbook; base max hits and rune costs are
     * the wiki-verified values (Ice Barrage 30 down to Smoke Burst 19). Used only for the WD-9 multi-
     * combat branch - a single-target task is unaffected (FR-6).
     */
    private static final List<AncientSpell> ANCIENTS_DESCENDING = new java.util.ArrayList<>();

    static
    {
        // element, tier, combat rune, [air count, element-rune count], level, base max hit
        // Air spells use only air + the combat rune; the others add their element rune.
        putSpell("air", Tier.BOLT, CHAOS_RUNE, 2, 0, 17, 9);
        putSpell("water", Tier.BOLT, CHAOS_RUNE, 2, 2, 23, 10);
        putSpell("earth", Tier.BOLT, CHAOS_RUNE, 2, 3, 29, 11);
        putSpell("fire", Tier.BOLT, CHAOS_RUNE, 3, 4, 35, 12);

        putSpell("air", Tier.BLAST, DEATH_RUNE, 3, 0, 41, 13);
        putSpell("water", Tier.BLAST, DEATH_RUNE, 3, 3, 47, 14);
        putSpell("earth", Tier.BLAST, DEATH_RUNE, 3, 4, 53, 15);
        putSpell("fire", Tier.BLAST, DEATH_RUNE, 4, 5, 59, 16);

        putSpell("air", Tier.WAVE, BLOOD_RUNE, 5, 0, 62, 17);
        putSpell("water", Tier.WAVE, BLOOD_RUNE, 5, 7, 65, 18);
        putSpell("earth", Tier.WAVE, BLOOD_RUNE, 5, 7, 70, 19);
        putSpell("fire", Tier.WAVE, BLOOD_RUNE, 5, 7, 75, 20);

        putSpell("air", Tier.SURGE, WRATH_RUNE, 7, 0, 81, 21);
        putSpell("water", Tier.SURGE, WRATH_RUNE, 7, 10, 85, 22);
        putSpell("earth", Tier.SURGE, WRATH_RUNE, 7, 10, 90, 23);
        putSpell("fire", Tier.SURGE, WRATH_RUNE, 7, 10, 95, 24);

        // Powered staves (base max hit at high Magic; display estimate only, ADR-0006).
        POWERED_STAFF_MAX_HIT.put(11905, 23); // Trident of the seas
        POWERED_STAFF_MAX_HIT.put(22288, 23); // Trident of the seas (e)
        POWERED_STAFF_MAX_HIT.put(12899, 25); // Trident of the swamp
        POWERED_STAFF_MAX_HIT.put(22292, 25); // Trident of the swamp (e)
        POWERED_STAFF_MAX_HIT.put(22323, 26); // Sanguinesti staff
        // Tumeken's Shadow (powered, 5t): base max hit floor(Magic/3)+1 -> 34 @99. Its +35 magic
        // attack and the x3 gear multiplier are applied in MagicWeaponEvaluator (ADR-0009 B.4).
        // Uncharged forms cannot cast, so they are excluded (like uncharged tridents/scythe).
        POWERED_STAFF_MAX_HIT.put(27275, 34); // Tumeken's Shadow
        POWERED_STAFF_MAX_HIT.put(28547, 34); // Corrupted Tumeken's Shadow (cosmetic)

        // Single-element staves and battle/mystic variants.
        putStaff("fire", 1387, 1393, 1401); // Staff of fire, Fire battlestaff, Mystic fire staff
        putStaff("water", 1383, 1395, 1403); // Staff of water, Water battlestaff, Mystic water staff
        putStaff("air", 1381, 1397, 1405); // Staff of air, Air battlestaff, Mystic air staff
        putStaff("earth", 1385, 1399, 1407); // Staff of earth, Earth battlestaff, Mystic earth staff
        // Tomes (shield slot) that supply an element.
        putStaff("fire", 20714); // Tome of fire
        putStaff("water", 25574); // Tome of water

        // Combination staves supply two elements each.
        putCombo(6562, 6563, "water", "earth"); // Mud battlestaff, Mystic mud staff
        putCombo(3053, 3054, "earth", "fire"); // Lava battlestaff, Mystic lava staff
        putCombo(11787, 11789, "water", "fire"); // Steam battlestaff, Mystic steam staff
        putCombo(11998, 12000, "air", "fire"); // Smoke battlestaff, Mystic smoke staff
        putCombo(20730, 20733, "air", "water"); // Mist battlestaff, Mystic mist staff
        putCombo(20736, 20739, "air", "earth"); // Dust battlestaff, Mystic dust staff

        // Ancient Magicks multi-target spells, strongest base max hit first. Barrages (level 86-94)
        // then bursts (level 62-70). Rune costs and levels are wiki-verified; base max hits are the
        // pre-magic-damage values (Ice Barrage 30 ... Smoke Burst 19).
        putAncient("Ice Barrage", 94, 30, WATER_RUNE, 6, BLOOD_RUNE, 2, DEATH_RUNE, 4);
        putAncient("Blood Barrage", 92, 29, BLOOD_RUNE, 4, DEATH_RUNE, 4, SOUL_RUNE, 1);
        putAncient("Shadow Barrage", 88, 28, AIR_RUNE, 4, BLOOD_RUNE, 2, DEATH_RUNE, 4, SOUL_RUNE, 3);
        putAncient("Smoke Barrage", 86, 27, AIR_RUNE, 4, FIRE_RUNE, 4, BLOOD_RUNE, 2, DEATH_RUNE, 4);
        putAncient("Ice Burst", 70, 22, WATER_RUNE, 4, CHAOS_RUNE, 4, DEATH_RUNE, 2);
        putAncient("Blood Burst", 68, 21, BLOOD_RUNE, 2, CHAOS_RUNE, 4, DEATH_RUNE, 2);
        putAncient("Shadow Burst", 64, 20, AIR_RUNE, 1, CHAOS_RUNE, 4, DEATH_RUNE, 2, SOUL_RUNE, 2);
        putAncient("Smoke Burst", 62, 19, AIR_RUNE, 2, FIRE_RUNE, 2, CHAOS_RUNE, 4, DEATH_RUNE, 2);
    }

    private RuneTable()
    {
    }

    /** Tiers strongest to weakest: {@code [SURGE, WAVE, BLAST, BOLT]}. */
    public static List<Tier> tiersDescending()
    {
        return TIERS_DESCENDING;
    }

    /**
     * @return the Ancient Magicks multi-target combat spells, strongest base max hit first (Barrages
     *     then Bursts). Used by the WD-9 multi-combat branch of {@link MagicWeaponEvaluator}.
     */
    public static List<AncientSpell> ancientsDescending()
    {
        return Collections.unmodifiableList(ANCIENTS_DESCENDING);
    }

    /**
     * @return the per-cast rune requirement ({@code runeId -> count}) for the spell, or an empty map
     *     if the element/tier is unknown. Includes air, the element rune (if any) and the combat rune.
     */
    public static Map<Integer, Integer> requirement(String element, Tier tier)
    {
        Spell s = spell(element, tier);
        return s == null ? Collections.emptyMap() : s.runes;
    }

    /** @return the Magic level required to cast the spell, or {@code 0} if unknown. */
    public static int level(String element, Tier tier)
    {
        Spell s = spell(element, tier);
        return s == null ? 0 : s.level;
    }

    /** @return the spell's base max hit (before magic-damage %), or {@code 0} if unknown. */
    public static int baseMaxHit(String element, Tier tier)
    {
        Spell s = spell(element, tier);
        return s == null ? 0 : s.maxHit;
    }

    /** @return the spell name (e.g. {@code "Fire Surge"}), or {@code null} if unknown. */
    public static String spellName(String element, Tier tier)
    {
        Spell s = spell(element, tier);
        return s == null ? null : s.name;
    }

    /** @return whether the item id is a curated powered staff (supplies its own attack, no runes). */
    public static boolean isPoweredStaff(int itemId)
    {
        return POWERED_STAFF_MAX_HIT.containsKey(itemId);
    }

    /** @return the powered staff's base max hit, or {@code null} if the id is not a powered staff. */
    public static Integer poweredStaffMaxHit(int itemId)
    {
        return POWERED_STAFF_MAX_HIT.get(itemId);
    }

    /**
     * @return the elements an owned staff/tome supplies unlimited (empty if it is not an element
     *     supplier). Combination staves return two elements.
     */
    public static Set<String> elementsSuppliedBy(int itemId)
    {
        return STAFF_ELEMENTS.getOrDefault(itemId, Collections.emptySet());
    }

    private static Spell spell(String element, Tier tier)
    {
        if (element == null || tier == null)
        {
            return null;
        }
        Map<Tier, Spell> byTier = SPELLS.get(element.toLowerCase());
        return byTier == null ? null : byTier.get(tier);
    }

    private static void putSpell(String element, Tier tier, int combatRune, int airCount,
        int elementCount, int level, int maxHit)
    {
        Map<Integer, Integer> runes = new LinkedHashMap<>();
        runes.put(AIR_RUNE, airCount);
        if (elementCount > 0)
        {
            runes.put(elementRune(element), elementCount);
        }
        runes.put(combatRune, 1);

        Spell spell = new Spell(Collections.unmodifiableMap(runes), level, maxHit,
            elementDisplay(element) + " " + tierDisplay(tier));
        SPELLS.computeIfAbsent(element, k -> new EnumMap<>(Tier.class)).put(tier, spell);
    }

    private static void putStaff(String element, int... ids)
    {
        for (int id : ids)
        {
            STAFF_ELEMENTS.put(id, Collections.singleton(element));
        }
    }

    private static void putCombo(int battlestaffId, int mysticId, String e1, String e2)
    {
        Set<String> both = Collections.unmodifiableSet(
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(e1, e2)));
        STAFF_ELEMENTS.put(battlestaffId, both);
        STAFF_ELEMENTS.put(mysticId, both);
    }

    private static void putAncient(String name, int level, int maxHit, int... runeQtyPairs)
    {
        Map<Integer, Integer> runes = new LinkedHashMap<>();
        for (int i = 0; i < runeQtyPairs.length; i += 2)
        {
            runes.put(runeQtyPairs[i], runeQtyPairs[i + 1]);
        }
        ANCIENTS_DESCENDING.add(
            new AncientSpell(name, level, maxHit, Collections.unmodifiableMap(runes)));
    }

    private static int elementRune(String element)
    {
        switch (element)
        {
            case "water":
                return WATER_RUNE;
            case "earth":
                return EARTH_RUNE;
            case "fire":
                return FIRE_RUNE;
            default:
                return AIR_RUNE;
        }
    }

    private static String elementDisplay(String element)
    {
        // Air spells are named "Wind" on the standard spellbook (Wind Surge etc.).
        if ("air".equals(element))
        {
            return "Wind";
        }
        return Character.toUpperCase(element.charAt(0)) + element.substring(1);
    }

    private static String tierDisplay(Tier tier)
    {
        String s = tier.name().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * A curated Ancient Magicks multi-target spell (Burst/Barrage): its name, Magic level, base max hit
     * (pre magic-damage %), and per-cast rune requirement ({@code runeId -> count}).
     */
    public static final class AncientSpell
    {
        private final String name;
        private final int level;
        private final int maxHit;
        private final Map<Integer, Integer> runes;

        private AncientSpell(String name, int level, int maxHit, Map<Integer, Integer> runes)
        {
            this.name = name;
            this.level = level;
            this.maxHit = maxHit;
            this.runes = runes;
        }

        public String getName()
        {
            return name;
        }

        public int getLevel()
        {
            return level;
        }

        public int getMaxHit()
        {
            return maxHit;
        }

        /** @return the per-cast rune requirement ({@code runeId -> count}), unmodifiable. */
        public Map<Integer, Integer> getRunes()
        {
            return runes;
        }
    }

    private static final class Spell
    {
        private final Map<Integer, Integer> runes;
        private final int level;
        private final int maxHit;
        private final String name;

        private Spell(Map<Integer, Integer> runes, int level, int maxHit, String name)
        {
            this.runes = runes;
            this.level = level;
            this.maxHit = maxHit;
            this.name = name;
        }
    }
}

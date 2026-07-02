package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * LD07: the consumable selector implements plan section 5 over owned items using a fake
 * {@link ConsumableEffectsProvider} (canned heal/boosts/magnitudes) and the real {@link RuneTable}.
 * No live {@code ItemStatChangesService} is touched.
 */
public class ConsumableSelectorTest
{
    // canonical potion ids (== ItemVariationMapping.map of every dose).
    private static final int SUPER_COMBAT = 12695; // ATT/STR/DEF
    private static final int SUPER_COMBAT_2_DOSE = 12699;
    private static final int SUPER_STRENGTH = 157; // STR only
    private static final int RANGING_POTION = 169; // RANGED
    private static final int MAGIC_POTION = 3040;  // MAGIC

    // food
    private static final int SHARK = 385;
    private static final int MONKFISH = 7946;
    private static final int COINS = 995; // not food, not a potion

    // staves
    private static final int FIRE_BATTLESTAFF = 1393;
    private static final int TRIDENT_SEAS = 11905;

    private static final PlayerStats MAGIC_99 = stats(99);

    // --- food (FR-4) ---------------------------------------------------------------------------

    @Test
    public void picksTheHighestHealOwnedFoodAndFlagsKarambwanAsCombo()
    {
        FakeFx fx = new FakeFx()
            .food(SHARK, 20).food(MONKFISH, 16).food(RuneTable.KARAMBWAN_ID, 18);
        Consumables c = select(fx, own()
            .add(SHARK, 5).add(MONKFISH, 5).add(RuneTable.KARAMBWAN_ID, 5),
            CombatStyle.MELEE, null, null);

        assertEquals(Integer.valueOf(SHARK), c.getFoodId());
        assertEquals(Integer.valueOf(RuneTable.KARAMBWAN_ID), c.getComboFoodId());
    }

    @Test
    public void noFoodOwnedYieldsNullFood()
    {
        FakeFx fx = new FakeFx(); // nothing is food
        Consumables c = select(fx, own().add(COINS, 1), CombatStyle.MELEE, null, null);

        assertNull(c.getFoodId());
        assertNull(c.getComboFoodId());
    }

    @Test
    public void karambwanAloneBecomesTheFoodWithNoSeparateCombo()
    {
        FakeFx fx = new FakeFx().food(RuneTable.KARAMBWAN_ID, 18);
        Consumables c = select(fx, own().add(RuneTable.KARAMBWAN_ID, 3),
            CombatStyle.MELEE, null, null);

        assertEquals(Integer.valueOf(RuneTable.KARAMBWAN_ID), c.getFoodId());
        assertNull("no separate combo when karambwan is the only food", c.getComboFoodId());
    }

    // --- potion (FR-5) -------------------------------------------------------------------------

    @Test
    public void picksTheStyleMatchingPotionWithTheHigherMagnitude()
    {
        FakeFx fx = new FakeFx()
            .potion(SUPER_COMBAT, boosts(19, 19, 19, 0, 0, 0))  // melee total 57
            .potion(SUPER_STRENGTH, boosts(0, 19, 0, 0, 0, 0))  // melee total 19
            .potion(RANGING_POTION, boosts(0, 0, 0, 13, 0, 0)); // not a melee boost -> ignored
        Consumables c = select(fx, own()
            .add(SUPER_COMBAT, 1).add(SUPER_STRENGTH, 1).add(RANGING_POTION, 1),
            CombatStyle.MELEE, null, null);

        assertEquals(Integer.valueOf(SUPER_COMBAT), c.getPotionId());
    }

    @Test
    public void treatsDoseVariantsAsTheSamePotion()
    {
        // owning two doses of super combat must resolve to a single deterministic representative.
        FakeFx fx = new FakeFx()
            .potion(SUPER_COMBAT, boosts(19, 19, 19, 0, 0, 0))
            .potion(SUPER_COMBAT_2_DOSE, boosts(19, 19, 19, 0, 0, 0));
        Consumables c = select(fx, own()
            .add(SUPER_COMBAT, 1).add(SUPER_COMBAT_2_DOSE, 1),
            CombatStyle.MELEE, null, null);

        assertEquals(Integer.valueOf(SUPER_COMBAT), c.getPotionId());
    }

    @Test
    public void noStyleMatchingPotionYieldsNullPotion()
    {
        FakeFx fx = new FakeFx().potion(RANGING_POTION, boosts(0, 0, 0, 13, 0, 0));
        Consumables c = select(fx, own().add(RANGING_POTION, 1), CombatStyle.MELEE, null, null);

        assertNull(c.getPotionId());
    }

    @Test
    public void rangedAndMagicPotionsMatchTheirStyles()
    {
        FakeFx fx = new FakeFx()
            .potion(RANGING_POTION, boosts(0, 0, 0, 13, 0, 0))
            .potion(MAGIC_POTION, boosts(0, 0, 0, 0, 4, 0))
            .potion(SUPER_COMBAT, boosts(19, 19, 19, 0, 0, 0));

        Consumables ranged = select(fx, own()
            .add(RANGING_POTION, 1).add(SUPER_COMBAT, 1), CombatStyle.RANGED, null, null);
        assertEquals(Integer.valueOf(RANGING_POTION), ranged.getPotionId());

        Consumables magic = select(fx, own()
            .add(MAGIC_POTION, 1).add(SUPER_COMBAT, 1), CombatStyle.MAGIC, "fire", null);
        assertEquals(Integer.valueOf(MAGIC_POTION), magic.getPotionId());
    }

    // --- magic runes / spell (FR-6) ------------------------------------------------------------

    @Test
    public void nonMagicStyleHasNoMagicSetup()
    {
        Consumables c = select(new FakeFx(), own().add(SHARK, 1), CombatStyle.MELEE, null, null);
        assertNull(c.getMagic());
    }

    @Test
    public void picksTheHighestCastableTierGivenLevelAndRunes()
    {
        OwnedItems owned = own()
            .add(RuneTable.AIR_RUNE, 100).add(RuneTable.FIRE_RUNE, 100).add(RuneTable.WRATH_RUNE, 100)
            .build();
        MagicSetup m = new ConsumableSelector(new FakeFx())
            .select(owned, CombatStyle.MAGIC, "fire", MAGIC_99, null).getMagic();

        assertEquals("fire", m.getElement());
        assertEquals("Fire Surge", m.getSpellName());
        assertEquals(24, m.getSpellBaseMaxHit());
        assertFalse(m.isPoweredStaff());
        assertTrue(m.getRunesShort().isEmpty());
        Map<Integer, Integer> expected = new HashMap<>();
        expected.put(RuneTable.AIR_RUNE, 7);
        expected.put(RuneTable.FIRE_RUNE, 10);
        expected.put(RuneTable.WRATH_RUNE, 1);
        assertEquals(expected, m.getRuneRequirement());
    }

    @Test
    public void dropsToTheHighestAffordableTierWhenTopTierRunesAreShort()
    {
        // enough for Fire Wave but not Fire Surge (no wrath runes) -> Wave, runesShort empty.
        OwnedItems owned = own()
            .add(RuneTable.AIR_RUNE, 100).add(RuneTable.FIRE_RUNE, 100).add(RuneTable.BLOOD_RUNE, 100)
            .build();
        MagicSetup m = new ConsumableSelector(new FakeFx())
            .select(owned, CombatStyle.MAGIC, "fire", MAGIC_99, null).getMagic();

        assertEquals("Fire Wave", m.getSpellName());
        assertEquals(20, m.getSpellBaseMaxHit());
        assertTrue("an affordable tier has no shortfall", m.getRunesShort().isEmpty());
    }

    @Test
    public void whenNoTierIsAffordableUsesTheHighestLevelAllowedTierAndRecordsRunesShort()
    {
        // Magic 40 only allows Fire Bolt (level 35); owning zero runes -> Bolt + full shortfall.
        OwnedItems owned = own().add(SHARK, 1).build(); // no runes at all
        MagicSetup m = new ConsumableSelector(new FakeFx())
            .select(owned, CombatStyle.MAGIC, "fire", stats(40), null).getMagic();

        assertEquals("Fire Bolt", m.getSpellName());
        assertFalse("runes are short -> populated", m.getRunesShort().isEmpty());
        assertEquals(Integer.valueOf(3), m.getRunesShort().get(RuneTable.AIR_RUNE));
        assertEquals(Integer.valueOf(4), m.getRunesShort().get(RuneTable.FIRE_RUNE));
        assertEquals(Integer.valueOf(1), m.getRunesShort().get(RuneTable.CHAOS_RUNE));
    }

    @Test
    public void multicombatSelectPrefersAffordableAncientBarrage()
    {
        // WD-9 (ADR-0020 #4): on a multi-combat task, holding Ice Barrage runes, the magic setup is the
        // Ancient Barrage, not the standard Fire Surge. The single-target overload is unchanged (FR-6).
        OwnedItems owned = own()
            .add(RuneTable.WATER_RUNE, 1000).add(RuneTable.BLOOD_RUNE, 1000).add(RuneTable.DEATH_RUNE, 1000)
            .add(RuneTable.AIR_RUNE, 1000).add(RuneTable.WRATH_RUNE, 1000)
            .build();

        MagicSetup multi = new ConsumableSelector(new FakeFx())
            .select(owned, CombatStyle.MAGIC, "fire", MAGIC_99, null, true).getMagic();
        assertEquals("Ice Barrage", multi.getSpellName());
        assertEquals(30, multi.getSpellBaseMaxHit());

        MagicSetup single = new ConsumableSelector(new FakeFx())
            .select(owned, CombatStyle.MAGIC, "fire", MAGIC_99, null).getMagic();
        assertEquals("Fire Surge", single.getSpellName());
    }

    @Test
    public void anOwnedElementalStaffZeroesThatElementsRune()
    {
        // Fire battlestaff supplies fire -> Fire Surge needs only air + wrath; castable with 0 fire runes.
        OwnedItems owned = own()
            .add(RuneTable.AIR_RUNE, 100).add(RuneTable.WRATH_RUNE, 100)
            .build();
        MagicSetup m = new ConsumableSelector(new FakeFx())
            .select(owned, CombatStyle.MAGIC, "fire", MAGIC_99, FIRE_BATTLESTAFF).getMagic();

        assertEquals("Fire Surge", m.getSpellName());
        assertEquals("fire rune supplied by the staff", 0,
            (int) m.getRuneRequirement().getOrDefault(RuneTable.FIRE_RUNE, 0));
        assertEquals(Integer.valueOf(7), m.getRuneRequirement().get(RuneTable.AIR_RUNE));
        assertTrue("castable despite owning no fire runes", m.getRunesShort().isEmpty());
    }

    @Test
    public void anOwnedPoweredStaffNeedsNoRunesAndUsesItsBaseMaxHit()
    {
        MagicSetup m = new ConsumableSelector(new FakeFx())
            .select(own().build(), CombatStyle.MAGIC, "fire", MAGIC_99, TRIDENT_SEAS).getMagic();

        assertTrue(m.isPoweredStaff());
        assertEquals(23, m.getSpellBaseMaxHit()); // Trident of the seas
        assertTrue(m.getRuneRequirement().isEmpty());
        assertTrue(m.getRunesShort().isEmpty());
    }

    @Test
    public void picksTheElementWhoseCastableTierHitsHardestWhenElementIsUnknown()
    {
        // owns air+fire+wrath only -> both Wind Surge (21) and Fire Surge (24) castable; fire wins.
        OwnedItems owned = own()
            .add(RuneTable.AIR_RUNE, 100).add(RuneTable.FIRE_RUNE, 100).add(RuneTable.WRATH_RUNE, 100)
            .build();
        MagicSetup m = new ConsumableSelector(new FakeFx())
            .select(owned, CombatStyle.MAGIC, null, MAGIC_99, null).getMagic();

        assertEquals("fire", m.getElement());
        assertEquals("Fire Surge", m.getSpellName());
    }

    // --- helpers -------------------------------------------------------------------------------

    private static Consumables select(ConsumableEffectsProvider fx, Owned owned, CombatStyle style,
        String element, Integer weaponId)
    {
        return new ConsumableSelector(fx).select(owned.build(), style, element, MAGIC_99, weaponId);
    }

    private static PlayerStats stats(int magic)
    {
        return new PlayerStats(99, 99, 99, 99, magic, 99);
    }

    private static Map<BoostedStat, Integer> boosts(int att, int str, int def, int ranged, int magic,
        int prayer)
    {
        Map<BoostedStat, Integer> m = new EnumMap<>(BoostedStat.class);
        if (att > 0)
        {
            m.put(BoostedStat.ATTACK, att);
        }
        if (str > 0)
        {
            m.put(BoostedStat.STRENGTH, str);
        }
        if (def > 0)
        {
            m.put(BoostedStat.DEFENCE, def);
        }
        if (ranged > 0)
        {
            m.put(BoostedStat.RANGED, ranged);
        }
        if (magic > 0)
        {
            m.put(BoostedStat.MAGIC, magic);
        }
        if (prayer > 0)
        {
            m.put(BoostedStat.PRAYER, prayer);
        }
        return m;
    }

    private static Owned own()
    {
        return new Owned();
    }

    private static final class Owned
    {
        private final Map<Integer, Integer> counts = new HashMap<>();

        Owned add(int id, int qty)
        {
            counts.put(id, qty);
            return this;
        }

        OwnedItems build()
        {
            return OwnedItems.fromCounts(counts);
        }
    }

    private static final class FakeFx implements ConsumableEffectsProvider
    {
        private final Map<Integer, Integer> heal = new HashMap<>();
        private final Map<Integer, Map<BoostedStat, Integer>> boost = new HashMap<>();

        FakeFx food(int id, int healAmount)
        {
            heal.put(id, healAmount);
            return this;
        }

        FakeFx potion(int id, Map<BoostedStat, Integer> magnitudes)
        {
            boost.put(id, magnitudes);
            return this;
        }

        @Override
        public Integer healAmount(int itemId)
        {
            return heal.get(itemId);
        }

        @Override
        public Set<BoostedStat> boostedStats(int itemId)
        {
            Map<BoostedStat, Integer> m = boost.get(itemId);
            return m == null ? Collections.emptySet() : m.keySet();
        }

        @Override
        public Map<BoostedStat, Integer> boostMagnitudes(int itemId)
        {
            Map<BoostedStat, Integer> m = boost.get(itemId);
            return m == null ? Collections.emptyMap() : m;
        }
    }
}

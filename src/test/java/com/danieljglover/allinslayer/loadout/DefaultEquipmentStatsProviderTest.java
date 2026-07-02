package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danieljglover.allinslayer.model.EquipmentSlot;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * LD01: the provider returns {@code null} (not {@link Bonuses#zero()}) for null/non-equipable stats
 * (ADR-0002), maps the RuneLite slot index + two-handed flag onto {@link Bonuses}, and caches only
 * non-null results per id (NFR-2/NFR-5). The final {@code ItemStats}/{@code ItemEquipmentStats}
 * value types are mocked via the inline mock-maker.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultEquipmentStatsProviderTest
{
    @Mock private ItemManager itemManager;

    private DefaultEquipmentStatsProvider provider;

    @Before
    public void setUp()
    {
        provider = new DefaultEquipmentStatsProvider(itemManager);
    }

    @Test
    public void returnsNullWhenStatsAreNotLoaded()
    {
        when(itemManager.getItemStats(4151)).thenReturn(null);

        assertNull("null stats must not become Bonuses.zero()", provider.get(4151));
    }

    @Test
    public void returnsNullForNonEquipableItem()
    {
        ItemStats stats = mock(ItemStats.class);
        when(stats.getEquipment()).thenReturn(null);
        when(itemManager.getItemStats(995)).thenReturn(stats);

        assertNull("non-equipable item has no Bonuses", provider.get(995));
    }

    @Test
    public void mapsSlotAndTwoHandedAndAttackBonusesOntoBonuses()
    {
        // Slot index 9 (GLOVES) -> HANDS, two-handed false; a scimitar-ish slash weapon would be
        // WEAPON(3)/2h, but gloves prove the index mapping is applied.
        ItemEquipmentStats e = mock(ItemEquipmentStats.class);
        when(e.getSlot()).thenReturn(9);
        when(e.isTwoHanded()).thenReturn(false);
        when(e.getAstab()).thenReturn(1);
        when(e.getAslash()).thenReturn(2);
        when(e.getAcrush()).thenReturn(3);
        when(e.getAmagic()).thenReturn(4);
        when(e.getArange()).thenReturn(5);
        when(e.getStr()).thenReturn(6);
        when(e.getRstr()).thenReturn(7);
        when(e.getMdmg()).thenReturn(8.0f);
        when(e.getAspeed()).thenReturn(4);
        ItemStats stats = mock(ItemStats.class);
        when(stats.getEquipment()).thenReturn(e);
        when(itemManager.getItemStats(7457)).thenReturn(stats);

        Bonuses b = provider.get(7457);

        assertNotNull(b);
        assertEquals(EquipmentSlot.HANDS, b.getSlot());
        assertEquals(1, b.getAstab());
        assertEquals(2, b.getAslash());
        assertEquals(3, b.getAcrush());
        assertEquals(4, b.getAmagic());
        assertEquals(5, b.getArange());
        assertEquals(6, b.getMeleeStr());
        assertEquals(7, b.getRangedStr());
        assertEquals(8, b.getMagicDmgPercent());
        assertEquals(4, b.getAttackSpeedTicks());
        assertTrue("slot index 9 -> HANDS, not two-handed", !b.isTwoHanded());
    }

    @Test
    public void mapsTwoHandedWeapon()
    {
        ItemEquipmentStats e = mock(ItemEquipmentStats.class);
        when(e.getSlot()).thenReturn(3); // WEAPON
        when(e.isTwoHanded()).thenReturn(true);
        when(e.getMdmg()).thenReturn(0.0f);
        ItemStats stats = mock(ItemStats.class);
        when(stats.getEquipment()).thenReturn(e);
        when(itemManager.getItemStats(11802)).thenReturn(stats);

        Bonuses b = provider.get(11802);

        assertEquals(EquipmentSlot.WEAPON, b.getSlot());
        assertTrue("AGS is two-handed", b.isTwoHanded());
    }

    @Test
    public void nonNullResultIsCachedSoTheSecondCallDoesNotRequery()
    {
        ItemEquipmentStats e = mock(ItemEquipmentStats.class);
        when(e.getSlot()).thenReturn(3);
        when(e.getMdmg()).thenReturn(0.0f);
        ItemStats stats = mock(ItemStats.class);
        when(stats.getEquipment()).thenReturn(e);
        when(itemManager.getItemStats(4151)).thenReturn(stats);

        Bonuses first = provider.get(4151);
        Bonuses second = provider.get(4151);

        assertEquals(first, second);
        verify(itemManager, times(1)).getItemStats(4151);
    }

    @Test
    public void nullResultIsNotCachedAndRecoversOnceTheStatMapLoads()
    {
        ItemEquipmentStats e = mock(ItemEquipmentStats.class);
        when(e.getSlot()).thenReturn(3);
        when(e.getMdmg()).thenReturn(0.0f);
        ItemStats stats = mock(ItemStats.class);
        when(stats.getEquipment()).thenReturn(e);
        // First call: map not loaded -> null. Second call: loaded -> stats.
        when(itemManager.getItemStats(4151)).thenReturn(null, stats);

        assertNull(provider.get(4151));
        assertNotNull("a later recompute recovers once stats load", provider.get(4151));
        verify(itemManager, times(2)).getItemStats(4151);
    }
}

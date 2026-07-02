package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.allComponents;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.componentByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.ui.components.ComponentTestSupport.RecordingIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.junit.Test;

/**
 * T10 - LoadoutItemRow. A sprite (drawn through the injected {@link RecordingIconRenderer} seam) plus
 * a name + slot text block, with OWNED / UPGRADE / BLOCKED state colouring and a right-click Wiki
 * lookup. Never touches a live {@code ItemManager} - the fake records what the renderer was asked for.
 */
public class LoadoutItemRowTest
{
    private static final int WHIP_ID = 4151;

    private static int countOfType(Component root, Class<?> type)
    {
        int count = 0;
        for (Component component : allComponents((java.awt.Container) root))
        {
            if (type.isInstance(component))
            {
                count++;
            }
        }
        return count;
    }

    @Test
    public void ownedRowShowsNameAndSlotInPrimaryAndAsksRendererUndimmed()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        LoadoutItemRow row = buildOnEdt(() -> new LoadoutItemRow(
            renderer, WHIP_ID, "Abyssal whip", "Weapon", 1, false, 1_400_000, LoadoutItemRow.State.OWNED));

        JLabel name = componentByName(row, "loadout-item-name", JLabel.class);
        JLabel slot = componentByName(row, "loadout-item-slot", JLabel.class);
        assertEquals("Abyssal whip", name.getText());
        assertEquals(SlayerTheme.TEXT_PRIMARY, name.getForeground());
        assertEquals(SlayerTheme.TYPE_BODY, name.getFont());
        assertEquals("Weapon", slot.getText());
        assertEquals(SlayerTheme.TEXT_SECONDARY, slot.getForeground());
        assertEquals(SlayerTheme.TYPE_CAPTION, slot.getFont());

        assertEquals("renderer asked exactly once", 1, renderer.calls.size());
        RecordingIconRenderer.Call call = renderer.callFor(row.getSpriteLabel());
        assertNotNull("sprite label must be the render target", call);
        assertEquals(WHIP_ID, call.itemId);
        assertEquals(1, call.quantity);
        assertFalse("gear is not stackable", call.stackable);
        assertFalse("OWNED is not dimmed", call.dim);

        assertEquals("OWNED carries no state tag", 0, countOfType(row, Tag.class));
    }

    @Test
    public void rendererReceivesQuantityAndStackableForStackableItems()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        LoadoutItemRow row = buildOnEdt(() -> new LoadoutItemRow(
            renderer, 2, "Cannonball", "Inventory", 30, true, null, LoadoutItemRow.State.OWNED));

        RecordingIconRenderer.Call call = renderer.callFor(row.getSpriteLabel());
        assertNotNull(call);
        assertEquals(2, call.itemId);
        assertEquals(30, call.quantity);
        assertTrue("stackable items pass stackable=true", call.stackable);
    }

    @Test
    public void upgradeRowIsMutedDimmedWithUpgradeTag()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        LoadoutItemRow row = buildOnEdt(() -> new LoadoutItemRow(
            renderer, 26219, "Ghrazi rapier", "Weapon", 1, false, 55_000_000, LoadoutItemRow.State.UPGRADE));

        JLabel name = componentByName(row, "loadout-item-name", JLabel.class);
        assertEquals("upgrade name is muted, not alarming", SlayerTheme.TEXT_MUTED, name.getForeground());

        RecordingIconRenderer.Call call = renderer.callFor(row.getSpriteLabel());
        assertNotNull(call);
        assertTrue("UPGRADE sprite is dimmed via the seam", call.dim);

        Tag tag = componentByName(row, "tag", Tag.class);
        assertEquals("upgrade", tag.getText());
        assertEquals("upgrade tag is informational (secondary), not red",
            SlayerTheme.TEXT_SECONDARY, tag.getForeground());
    }

    @Test
    public void blockedRowIsErrorWithMissingTagUndimmed()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        LoadoutItemRow row = buildOnEdt(() -> new LoadoutItemRow(
            renderer, 4151, "Abyssal whip", "Required", 1, false, null, LoadoutItemRow.State.BLOCKED));

        JLabel name = componentByName(row, "loadout-item-name", JLabel.class);
        assertEquals("a missing required item blocks the task -> red",
            SlayerTheme.STATE_BLOCKED, name.getForeground());

        RecordingIconRenderer.Call call = renderer.callFor(row.getSpriteLabel());
        assertNotNull(call);
        assertFalse("BLOCKED is not dimmed (it is loud, not faded)", call.dim);

        Tag tag = componentByName(row, "tag", Tag.class);
        assertEquals("missing", tag.getText());
        assertEquals(SlayerTheme.STATE_BLOCKED, tag.getForeground());
    }

    @Test
    public void priceLabelAppearsOnlyWhenPriceProvided()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();

        LoadoutItemRow withPrice = buildOnEdt(() -> new LoadoutItemRow(
            renderer, WHIP_ID, "Abyssal whip", "Weapon", 1, false, 1_400_000, LoadoutItemRow.State.OWNED));
        assertEquals("a price was supplied -> one PriceLabel", 1, countOfType(withPrice, PriceLabel.class));

        LoadoutItemRow noPrice = buildOnEdt(() -> new LoadoutItemRow(
            renderer, WHIP_ID, "Abyssal whip", "Weapon", 1, false, null, LoadoutItemRow.State.OWNED));
        assertEquals("no price supplied -> no PriceLabel", 0, countOfType(noPrice, PriceLabel.class));
    }

    @Test
    public void rowIsACardSurfaceAndPlainText()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        LoadoutItemRow row = buildOnEdt(() -> new LoadoutItemRow(
            renderer, WHIP_ID, "Abyssal whip", "Weapon", 1, false, 1_400_000, LoadoutItemRow.State.OWNED));

        assertEquals(SlayerTheme.SURFACE_CARD, row.getBackground());
        JLabel name = componentByName(row, "loadout-item-name", JLabel.class);
        assertFalse("names are plain text, no HTML", name.getText().toLowerCase().contains("<html"));
    }

    @Test
    public void rightClickPopupHasWikiItemTargetingTheItemId()
    {
        RecordingIconRenderer renderer = new RecordingIconRenderer();
        LoadoutItemRow row = buildOnEdt(() -> new LoadoutItemRow(
            renderer, WHIP_ID, "Abyssal whip", "Weapon", 1, false, 1_400_000, LoadoutItemRow.State.OWNED));

        JPopupMenu popup = row.getComponentPopupMenu();
        assertNotNull("item rows carry a right-click popup", popup);

        JMenuItem wiki = null;
        for (Component component : popup.getComponents())
        {
            if (component instanceof JMenuItem && "Wiki".equals(((JMenuItem) component).getText()))
            {
                wiki = (JMenuItem) component;
            }
        }
        assertNotNull("popup must offer a Wiki lookup", wiki);
        assertEquals("Wiki action targets the item id via Special:Lookup",
            ItemWiki.url(WHIP_ID), wiki.getActionCommand());
        assertTrue(wiki.getActionCommand().contains("id=" + WHIP_ID));
        assertTrue("sprite inherits the row popup so a right-click anywhere on the row works",
            row.getSpriteLabel().getInheritsPopupMenu());
    }
}

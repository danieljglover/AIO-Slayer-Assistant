package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.ItemIconRenderer;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The primary item line: sprite LEFT + a stacked name / sub-line text block RIGHT
 * (ui-recommendations.md §2.1). The sprite is drawn through the injected {@link ItemIconRenderer}
 * seam (ADR-0001), never via {@code ItemManager} directly.
 *
 * <p>{@link State} drives the colour discipline (§4): an {@code OWNED} item is plain
 * {@code text/primary}; an {@code UPGRADE} you do not own yet is muted with a dimmed sprite and an
 * informational "upgrade" {@link Tag} (not alarming); a {@code BLOCKED} required item you lack is
 * {@code state/blocked} red with a "missing" tag. Right-clicking the row opens the item's wiki page
 * (P2-12, via {@link ItemWiki}).</p>
 */
public class LoadoutItemRow extends JPanel
{
    /** The ownership state of the item, which drives colour and the trailing tag. */
    public enum State
    {
        OWNED,
        UPGRADE,
        BLOCKED
    }

    private final JLabel sprite;
    private final JLabel nameLabel;
    private final JLabel slotLabel;

    /**
     * @param renderer  the sprite seam (ADR-0001)
     * @param itemId    the item id to draw and to target with the wiki lookup
     * @param name      the item name (plain text, no HTML)
     * @param slotText  the slot/sub label (e.g. "Weapon", "Required")
     * @param quantity  the stack quantity (1 for non-stackable gear)
     * @param stackable whether the stack number should render
     * @param price     the GE value to show, or {@code null} to omit the price entirely
     * @param state     OWNED / UPGRADE / BLOCKED
     */
    public LoadoutItemRow(ItemIconRenderer renderer, int itemId, String name, String slotText,
        int quantity, boolean stackable, Integer price, State state)
    {
        super(new BorderLayout(SlayerTheme.SPACE_3, 0));
        setName("loadout-item-row");
        setBackground(SlayerTheme.SURFACE_CARD);
        setBorder(new EmptyBorder(
            SlayerTheme.SPACE_1, SlayerTheme.SPACE_2, SlayerTheme.SPACE_1, SlayerTheme.SPACE_2));

        sprite = new JLabel();
        sprite.setName("loadout-item-sprite");
        Dimension spriteSize = new Dimension(SlayerTheme.SPRITE_WIDTH, SlayerTheme.SPRITE_HEIGHT);
        sprite.setPreferredSize(spriteSize);
        sprite.setMinimumSize(spriteSize);
        renderer.render(sprite, itemId, quantity, stackable, state == State.UPGRADE);
        add(sprite, BorderLayout.WEST);

        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);

        nameLabel = new JLabel(name);
        nameLabel.setName("loadout-item-name");
        nameLabel.setFont(SlayerTheme.TYPE_BODY);
        nameLabel.setForeground(nameColour(state));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textBlock.add(nameLabel);

        JPanel subRow = new JPanel();
        subRow.setLayout(new BoxLayout(subRow, BoxLayout.X_AXIS));
        subRow.setOpaque(false);
        subRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        slotLabel = new JLabel(slotText);
        slotLabel.setName("loadout-item-slot");
        slotLabel.setFont(SlayerTheme.TYPE_CAPTION);
        slotLabel.setForeground(SlayerTheme.TEXT_SECONDARY);
        subRow.add(slotLabel);

        if (price != null)
        {
            subRow.add(Box.createHorizontalStrut(SlayerTheme.SPACE_2));
            JLabel dot = new JLabel("·");
            dot.setName("loadout-item-dot");
            dot.setFont(SlayerTheme.TYPE_CAPTION);
            dot.setForeground(SlayerTheme.TEXT_SECONDARY);
            subRow.add(dot);
            subRow.add(Box.createHorizontalStrut(SlayerTheme.SPACE_2));
            subRow.add(new PriceLabel(price));
        }

        Tag tag = tagFor(state);
        if (tag != null)
        {
            subRow.add(Box.createHorizontalGlue());
            subRow.add(tag);
        }

        textBlock.add(subRow);
        add(textBlock, BorderLayout.CENTER);

        setComponentPopupMenu(ItemWiki.popup(itemId));
        sprite.setInheritsPopupMenu(true);
        nameLabel.setInheritsPopupMenu(true);
        textBlock.setInheritsPopupMenu(true);
        subRow.setInheritsPopupMenu(true);
    }

    private static Color nameColour(State state)
    {
        switch (state)
        {
            case BLOCKED:
                return SlayerTheme.STATE_BLOCKED;
            case UPGRADE:
                return SlayerTheme.TEXT_MUTED;
            case OWNED:
            default:
                return SlayerTheme.TEXT_PRIMARY;
        }
    }

    private static Tag tagFor(State state)
    {
        switch (state)
        {
            case UPGRADE:
                return new Tag("upgrade", Tag.Variant.UPGRADE);
            case BLOCKED:
                return new Tag("missing", Tag.Variant.MISSING);
            case OWNED:
            default:
                return null;
        }
    }

    @Override
    public Dimension getMaximumSize()
    {
        // Stretch horizontally inside a BoxLayout-Y section, but never grow vertically.
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    public JLabel getSpriteLabel()
    {
        return sprite;
    }

    public JLabel getNameLabel()
    {
        return nameLabel;
    }

    public JLabel getSlotLabel()
    {
        return slotLabel;
    }
}

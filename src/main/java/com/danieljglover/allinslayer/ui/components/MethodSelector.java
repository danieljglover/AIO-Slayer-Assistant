package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * The segmented {@code Melee | Ranged | Magic} combat-method control (MV-FE3, ADR-0013). It lets the
 * player override the variant's recommended style; the chosen method is threaded through the engine
 * ({@code selectedMethod ?? recommendedStyle}).
 *
 * <p>Modelled on {@link ModeSelector}: three {@link JToggleButton}s in a {@link ButtonGroup} so all
 * three options - and which is active - are always visible, with the active segment carrying the one
 * legitimate brand accent. A combo was the plan's first pass, but a segmented control is the only clean
 * way to render an individual option <em>disabled</em> (the FR for a method with no viable loadout): a
 * {@link #setMethodEnabled(CombatStyle, boolean)} call greys exactly that segment while the others stay
 * clickable, which a {@code JComboBox} cannot express without disabling the whole control.</p>
 */
public class MethodSelector extends JPanel
{
    private static final CombatStyle[] ORDER = {CombatStyle.MELEE, CombatStyle.RANGED, CombatStyle.MAGIC};

    private final Map<CombatStyle, JToggleButton> buttons = new EnumMap<>(CombatStyle.class);
    private final Consumer<CombatStyle> onSelect;
    private CombatStyle current = CombatStyle.MELEE;

    public MethodSelector(Consumer<CombatStyle> onSelect)
    {
        super(new GridLayout(1, ORDER.length, SlayerTheme.SPACE_1, 0));
        this.onSelect = onSelect;
        setBackground(SlayerTheme.SURFACE_PAGE);

        ButtonGroup group = new ButtonGroup();
        for (CombatStyle style : ORDER)
        {
            JToggleButton button = segment(style);
            buttons.put(style, button);
            group.add(button);
            button.addActionListener(e -> onClick(style));
            add(button);
        }
        applySelection();
    }

    /** Display label for a method ({@code MELEE -> "Melee"}); the plugin parses it back to a style. */
    public static String label(CombatStyle style)
    {
        if (style == null)
        {
            return null;
        }
        String name = style.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private static JToggleButton segment(CombatStyle style)
    {
        JToggleButton button = new JToggleButton(label(style));
        button.setName("method-" + style.name().toLowerCase(Locale.ROOT));
        button.setFont(SlayerTheme.TYPE_CAPTION);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setPreferredSize(new Dimension(0, SlayerTheme.SEGMENT_HEIGHT));
        return button;
    }

    private void onClick(CombatStyle style)
    {
        if (style == current)
        {
            // ButtonGroup forbids deselecting the active segment; keep visuals consistent and do not
            // re-fire the callback for a no-op click (mirrors ModeSelector).
            applySelection();
            return;
        }
        current = style;
        applySelection();
        if (onSelect != null)
        {
            onSelect.accept(style);
        }
    }

    /** Reflect the selected method without firing the callback (non-destructive re-render). */
    public void setMethod(CombatStyle style)
    {
        if (style == null || !buttons.containsKey(style))
        {
            return;
        }
        current = style;
        applySelection();
    }

    /** Enable/disable a single method segment (FR: disable a method with no viable loadout). */
    public void setMethodEnabled(CombatStyle style, boolean enabled)
    {
        JToggleButton button = buttons.get(style);
        if (button != null)
        {
            button.setEnabled(enabled);
        }
    }

    public CombatStyle getMethod()
    {
        return current;
    }

    public JToggleButton getButton(CombatStyle style)
    {
        return buttons.get(style);
    }

    private void applySelection()
    {
        for (CombatStyle style : ORDER)
        {
            boolean active = style == current;
            JToggleButton button = buttons.get(style);
            button.setSelected(active);
            style(button, active);
        }
    }

    private static void style(JToggleButton button, boolean active)
    {
        button.setForeground(active ? SlayerTheme.ACCENT_BRAND : SlayerTheme.TEXT_SECONDARY);
        button.setBackground(active ? SlayerTheme.SURFACE_CARD : SlayerTheme.SURFACE_PAGE);
        button.setBorder(active
            ? BorderFactory.createMatteBorder(0, 0, 2, 0, SlayerTheme.ACCENT_BRAND)
            : BorderFactory.createEmptyBorder(0, 0, 2, 0));
    }
}

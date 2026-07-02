package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * The segmented {@code DPS | Cost} control (ui-recommendations.md §3.3, audit Finding 6).
 *
 * <p>Two {@link JToggleButton}s in a {@link ButtonGroup} so both options - and which one is active -
 * are always visible (the old {@code Mode: DPS} relabel button hid that it toggled). The active
 * segment is the one legitimate {@code accent/brand} use here; the inactive segment is
 * {@code text/secondary}. Clicking the inactive segment fires the supplied callback; re-clicking the
 * active one is a no-op (no spurious recompute).</p>
 */
public class ModeSelector extends JPanel
{
    private final JToggleButton dpsButton;
    private final JToggleButton costButton;
    private final Consumer<AdviceMode> onSelect;
    private AdviceMode current;

    public ModeSelector(AdviceMode initial, Consumer<AdviceMode> onSelect)
    {
        super(new GridLayout(1, 2, SlayerTheme.SPACE_1, 0));
        this.onSelect = onSelect;
        this.current = initial == null ? AdviceMode.DPS : initial;
        setBackground(SlayerTheme.SURFACE_PAGE);

        dpsButton = segment("DPS");
        costButton = segment("Cost");

        ButtonGroup group = new ButtonGroup();
        group.add(dpsButton);
        group.add(costButton);

        dpsButton.addActionListener(e -> onClick(AdviceMode.DPS));
        costButton.addActionListener(e -> onClick(AdviceMode.COST));

        add(dpsButton);
        add(costButton);

        applySelection();
    }

    private static JToggleButton segment(String text)
    {
        JToggleButton button = new JToggleButton(text);
        button.setName("mode-" + text.toLowerCase());
        button.setFont(SlayerTheme.TYPE_CAPTION);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setPreferredSize(new Dimension(0, SlayerTheme.SEGMENT_HEIGHT));
        return button;
    }

    private void onClick(AdviceMode mode)
    {
        if (mode == current)
        {
            // ButtonGroup forbids deselecting the active segment; keep visuals consistent and
            // do not re-fire the callback for a no-op click.
            applySelection();
            return;
        }
        current = mode;
        applySelection();
        if (onSelect != null)
        {
            onSelect.accept(mode);
        }
    }

    /** Reflect the mode without firing the callback (non-destructive re-render). */
    public void setMode(AdviceMode mode)
    {
        if (mode == null)
        {
            return;
        }
        current = mode;
        applySelection();
    }

    private void applySelection()
    {
        boolean dpsActive = current == AdviceMode.DPS;
        dpsButton.setSelected(dpsActive);
        costButton.setSelected(!dpsActive);
        style(dpsButton, dpsActive);
        style(costButton, !dpsActive);
    }

    private static void style(JToggleButton button, boolean active)
    {
        button.setForeground(active ? SlayerTheme.ACCENT_BRAND : SlayerTheme.TEXT_SECONDARY);
        button.setBackground(active ? SlayerTheme.SURFACE_CARD : SlayerTheme.SURFACE_PAGE);
        button.setBorder(active
            ? BorderFactory.createMatteBorder(0, 0, 2, 0, SlayerTheme.ACCENT_BRAND)
            : BorderFactory.createEmptyBorder(0, 0, 2, 0));
    }

    public AdviceMode getMode()
    {
        return current;
    }

    public JToggleButton getDpsButton()
    {
        return dpsButton;
    }

    public JToggleButton getCostButton()
    {
        return costButton;
    }
}

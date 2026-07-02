package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The one key/value row used across the dashboard (ui-recommendations.md §8, ADR-0004).
 *
 * <p>A fixed-width caption key ({@code type/caption}, {@code text/secondary}) on the left and a
 * flexible value ({@code type/body}, {@code text/primary}) on the right, laid out with
 * {@link GridBagLayout} so widths come from the layout manager - never from {@code <html>} width
 * hacks or hardcoded pixel math. This replaces the old {@code row()} / {@code keyValueRow()} split
 * and the deleted {@code KEY_WIDTH}/{@code VALUE_WIDTH} constants.</p>
 */
public class KeyValueRow extends JPanel
{
    private final JLabel keyLabel;
    private final JLabel valueLabel;

    public KeyValueRow(String key, String value)
    {
        super(new GridBagLayout());
        setBackground(SlayerTheme.SURFACE_CARD);

        keyLabel = new JLabel(key);
        keyLabel.setName("kv-key");
        keyLabel.setFont(SlayerTheme.TYPE_CAPTION);
        keyLabel.setForeground(SlayerTheme.TEXT_SECONDARY);
        keyLabel.setPreferredSize(new Dimension(SlayerTheme.KEY_COL_WIDTH,
            keyLabel.getPreferredSize().height));

        valueLabel = new JLabel(value);
        valueLabel.setName("kv-value");
        valueLabel.setFont(SlayerTheme.TYPE_BODY);
        valueLabel.setForeground(SlayerTheme.TEXT_PRIMARY);
        // Overflow-proof (W8 F3): let the layout shrink the value (JLabel then auto-ellipsizes) and
        // keep the full text reachable on hover, so a long value can never blow the panel width.
        valueLabel.setMinimumSize(new Dimension(1, valueLabel.getPreferredSize().height));
        valueLabel.setToolTipText(value);

        GridBagConstraints keyConstraints = new GridBagConstraints();
        keyConstraints.gridx = 0;
        keyConstraints.gridy = 0;
        keyConstraints.weightx = 0;
        keyConstraints.anchor = GridBagConstraints.LINE_START;
        keyConstraints.insets = new Insets(0, 0, 0, SlayerTheme.SPACE_4);
        add(keyLabel, keyConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = 0;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.anchor = GridBagConstraints.LINE_START;
        add(valueLabel, valueConstraints);
    }

    /** Replace the value text in place (same label instance), for non-destructive re-renders. */
    public void setValue(String value)
    {
        valueLabel.setText(value);
        valueLabel.setToolTipText(value);
    }

    /** Tint the value for state encoding (e.g. {@code state/met}, {@code state/blocked}). */
    public void setValueColor(Color color)
    {
        valueLabel.setForeground(color);
    }

    public JLabel getKeyLabel()
    {
        return keyLabel;
    }

    public JLabel getValueLabel()
    {
        return valueLabel;
    }

    @Override
    public Dimension getPreferredSize()
    {
        // Height is pinned to the row token; width derives from the layout manager but is capped to
        // the panel width so a long value can never inflate the dashboard past the viewport (W8 F3).
        Dimension preferred = super.getPreferredSize();
        return new Dimension(Math.min(preferred.width, SlayerTheme.PANEL_WIDTH), SlayerTheme.ROW_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize()
    {
        // Cap the width to the panel (W8 F3): the row fills its card column but never forces it wider.
        return new Dimension(SlayerTheme.PANEL_WIDTH, SlayerTheme.ROW_HEIGHT);
    }
}

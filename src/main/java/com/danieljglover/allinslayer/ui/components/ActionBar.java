package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.util.ImageUtil;

/**
 * The compact 24px toolbar (ui-recommendations.md §3.1) that replaces the three stacked full-width
 * buttons (P0-2). WEST is the segmented {@link ModeSelector} (DPS|Cost); EAST is a right-aligned
 * strip of borderless {@link IconButton}s - refresh then export.
 *
 * <p>Export is enabled only when a recommendation exists (FR-9). The refresh/export icons load from
 * {@code /icons/*.png} via {@link ImageUtil#loadImageResource}; {@link IconButton} generates the
 * hover and disabled (grayscale) variants in code, and strips its own decorations explicitly (the
 * 1.12.31.1 {@code removeButtonDecorations} only tags a style class).</p>
 */
public class ActionBar extends JPanel
{
    private final ModeSelector modeSelector;
    private final IconButton refreshButton;
    private final IconButton exportButton;

    /**
     * @param mode              the initial advice mode
     * @param onModeSelect      fired when the player switches mode (MODE_TOGGLE)
     * @param onRefresh         fired by the refresh button (manual nudge, RefreshSource.MANUAL)
     * @param onExport          fired by the export button (copy loadout)
     * @param hasRecommendation whether a recommendation exists; gates the export button
     */
    public ActionBar(AdviceMode mode, Consumer<AdviceMode> onModeSelect,
        Runnable onRefresh, Runnable onExport, boolean hasRecommendation)
    {
        super(new BorderLayout());
        setName("action-bar");
        setBackground(SlayerTheme.SURFACE_PAGE);
        setBorder(new EmptyBorder(0, SlayerTheme.SPACE_2, 0, SlayerTheme.SPACE_2));

        modeSelector = new ModeSelector(mode, onModeSelect);
        add(modeSelector, BorderLayout.WEST);

        BufferedImage refreshIcon = ImageUtil.loadImageResource(ActionBar.class, "/icons/refresh.png");
        BufferedImage exportIcon = ImageUtil.loadImageResource(ActionBar.class, "/icons/export.png");

        refreshButton = new IconButton(refreshIcon, "Refresh");
        refreshButton.setName("action-refresh");
        if (onRefresh != null)
        {
            refreshButton.addActionListener(e -> onRefresh.run());
        }

        exportButton = new IconButton(exportIcon, "Export loadout");
        exportButton.setName("action-export");
        if (onExport != null)
        {
            exportButton.addActionListener(e -> onExport.run());
        }
        exportButton.setEnabled(hasRecommendation);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SlayerTheme.SPACE_1, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        actions.add(exportButton);
        add(actions, BorderLayout.EAST);
    }

    /** Reflect the mode without firing the callback (non-destructive re-render). */
    public void setMode(AdviceMode mode)
    {
        modeSelector.setMode(mode);
    }

    /** Enable/disable export in place (FR-9 re-render). */
    public void setExportEnabled(boolean enabled)
    {
        exportButton.setEnabled(enabled);
    }

    public ModeSelector getModeSelector()
    {
        return modeSelector;
    }

    public IconButton getRefreshButton()
    {
        return refreshButton;
    }

    public IconButton getExportButton()
    {
        return exportButton;
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(preferred.width, SlayerTheme.ACTION_BAR_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return new Dimension(Integer.MAX_VALUE, SlayerTheme.ACTION_BAR_HEIGHT);
    }
}

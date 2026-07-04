package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.RefreshSource;
import com.danieljglover.allinslayer.ui.SlayerPanelState;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Color;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The humane three-line task identity header (ui-recommendations.md §5, audit Finding 5).
 *
 * <pre>
 * Abyssal demons                 type/title,   text/primary   (task name, plain text)
 * 147 remaining                  type/caption, text/secondary (hidden when remaining &lt;= 0)
 * updated 21:48 · gem / helm check  type/caption, text/muted  (minutes only, humane source)
 * </pre>
 *
 * <p>Replaces the old {@code Source: MENU_CHECK | Updated: 21:48:12} dev framing: no raw enums, no
 * seconds (those stay in the gated Diagnostics surface). {@link #update(SlayerPanelState)} mutates
 * the same label instances in place so reactive re-renders never rebuild the header.</p>
 */
public class TaskHeader extends JPanel
{
    private static final DateTimeFormatter TIME =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final JLabel nameLabel;
    private final JLabel remainingLabel;
    private final JLabel metaLabel;

    public TaskHeader()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(SlayerTheme.SURFACE_PAGE);
        setBorder(new EmptyBorder(SlayerTheme.SPACE_2, SlayerTheme.SPACE_2,
            SlayerTheme.SPACE_2, SlayerTheme.SPACE_2));

        nameLabel = line("task-header-name", SlayerTheme.TYPE_TITLE, SlayerTheme.TEXT_PRIMARY);
        remainingLabel = line("task-header-remaining", SlayerTheme.TYPE_CAPTION, SlayerTheme.TEXT_SECONDARY);
        metaLabel = line("task-header-meta", SlayerTheme.TYPE_CAPTION, SlayerTheme.TEXT_MUTED);

        add(nameLabel);
        add(remainingLabel);
        add(metaLabel);
    }

    private static JLabel line(String name, Font font, Color foreground)
    {
        JLabel label = new JLabel();
        label.setName(name);
        label.setFont(font);
        label.setForeground(foreground);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    /** Populate (or refresh) the header from the current state, mutating the labels in place. */
    public void update(SlayerPanelState state)
    {
        nameLabel.setText(taskName(state));

        int remaining = state.getRemaining();
        if (remaining > 0)
        {
            remainingLabel.setText(remaining + " remaining");
            remainingLabel.setVisible(true);
        }
        else
        {
            remainingLabel.setText("");
            remainingLabel.setVisible(false);
        }

        metaLabel.setText(meta(state));
    }

    private static String taskName(SlayerPanelState state)
    {
        if (state.getTask() != null && state.getTask().getTask() != null)
        {
            return state.getTask().getTask();
        }
        return "No Slayer task";
    }

    private static String meta(SlayerPanelState state)
    {
        String time = state.getUpdatedAt() == null ? "unknown" : TIME.format(state.getUpdatedAt());
        String source = humaneSource(state.getRefreshSource());
        // An empty source (null / unknown-future enum) must not leave a dangling " · " separator.
        if (source.isEmpty())
        {
            return "updated " + time;
        }
        return "updated " + time + " · " + source;
    }

    /**
     * Maps a {@link RefreshSource} to player-facing copy (plan §2.2, ui-recommendations.md §5). The
     * raw enum stays in the gated Diagnostics surface; this is the humane wording for the header.
     */
    static String humaneSource(RefreshSource source)
    {
        if (source == null)
        {
            return "";
        }
        switch (source)
        {
            case STARTUP:
                return "startup";
            case MANUAL:
                return "manual refresh";
            case VARBIT:
                return "task changed";
            case CHAT:
                return "new assignment";
            case MENU_CHECK:
                return "gem / helm check";
            case ITEM_CONTAINER:
                return "gear update";
            case GAME_STATE:
                return "login";
            case MODE_TOGGLE:
                return "mode change";
            case LOCATION_SELECT:
                return "location change";
            case VARIANT_SELECT:
                return "variant change";
            case METHOD_SELECT:
                return "style change";
            case MASTER_SELECT:
                return "master change";
            default:
                return "";
        }
    }

    public JLabel getNameLabel()
    {
        return nameLabel;
    }

    public JLabel getRemainingLabel()
    {
        return remainingLabel;
    }

    public JLabel getMetaLabel()
    {
        return metaLabel;
    }
}

package com.danieljglover.allinslayer.ui.advisor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Displays an immutable task summary supplied by the plugin; never reads live game state. */
public final class AdvisorOverlay extends OverlayPanel
{
    private volatile boolean enabled;
    private volatile String[] summary = new String[] {"", ""};

    public AdvisorOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(200, 0));
    }

    public void update(String title, String detail)
    {
        summary = new String[] {title == null ? "" : title, detail == null ? "" : detail};
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        String[] current = summary;
        if (!enabled || current[0].isEmpty())
        {
            return null;
        }
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("All-In Slayer").color(new Color(225, 181, 92)).build());
        panelComponent.getChildren().add(LineComponent.builder().left(current[0]).build());
        if (!current[1].isEmpty())
        {
            for (String line : current[1].split("\\n"))
            {
                panelComponent.getChildren().add(LineComponent.builder().left(line).build());
            }
        }
        return super.render(graphics);
    }
}

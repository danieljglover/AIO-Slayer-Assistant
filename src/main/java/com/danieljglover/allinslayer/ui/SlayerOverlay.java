package com.danieljglover.allinslayer.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.Setter;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class SlayerOverlay extends OverlayPanel
{
    @Setter
    private volatile boolean enabled;
    @Setter
    private volatile String taskName;
    @Setter
    private volatile int remaining;
    @Setter
    private volatile String method;

    @Inject
    public SlayerOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!enabled || taskName == null)
        {
            return null;
        }
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text("All-In Slayer").build());
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Task:").right(taskName).build());
        if (remaining > 0)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Remaining:").right(Integer.toString(remaining)).build());
        }
        if (method != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Method:").right(method).build());
        }
        return super.render(graphics);
    }
}

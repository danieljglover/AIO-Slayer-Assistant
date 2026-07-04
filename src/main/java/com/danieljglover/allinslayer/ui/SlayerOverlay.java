package com.danieljglover.allinslayer.ui;

import java.awt.Color;
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
    @Setter
    private volatile String location;
    // Compact conditional warnings fed by recompute (client thread). Name resolution happens there - the
    // overlay never touches ItemManager. Each field null/false hides its line so nothing lingers stale.
    @Setter
    private volatile String requiredItemMissing;
    @Setter
    private volatile boolean antifireWarning;
    @Setter
    private volatile String bankHint;
    @Setter
    private volatile boolean bossTrip;

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
        if (location != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Where:").right(location).build());
        }
        if (requiredItemMissing != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Bring:").right(requiredItemMissing).rightColor(Color.RED).build());
        }
        if (antifireWarning)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Bring antifire").leftColor(Color.RED).build());
        }
        if (bossTrip)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Boss - separate trip").build());
        }
        if (bankHint != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left(bankHint).leftColor(Color.ORANGE).build());
        }
        return super.render(graphics);
    }
}

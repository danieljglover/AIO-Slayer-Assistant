package com.danieljglover.allinslayer.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A read-only restock checklist shown ONLY while the bank is open (Phase 2): the items from the
 * current recommendation the player is still short of, so they know what to withdraw. The plugin
 * pushes the resolved lines (name + shortfall) on the client thread - the overlay itself only checks
 * whether the bank interface is open (a cheap widget read, valid on the render thread) and draws.
 * Strictly advisory: it never clicks, withdraws, or highlights anything.
 */
public class BankChecklistOverlay extends OverlayPanel
{
    private final Client client;

    @Setter
    private volatile boolean enabled;
    /** Outstanding withdrawal lines ("14x Shark"), pushed by the plugin; empty hides the overlay. */
    @Setter
    private volatile List<String> lines = Collections.emptyList();

    @Inject
    public BankChecklistOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.TOP_CENTER);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        List<String> current = lines;
        if (!enabled || current.isEmpty() || !isBankOpen())
        {
            return null;
        }
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text("Still to withdraw").build());
        for (String line : current)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left(line).leftColor(Color.YELLOW).build());
        }
        return super.render(graphics);
    }

    private boolean isBankOpen()
    {
        Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
        return items != null && !items.isHidden();
    }
}

package com.danieljglover.allinslayer.ui.components;

import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.buildOnEdt;
import static com.danieljglover.allinslayer.ui.components.ComponentTestSupport.runOnEdt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.ui.RefreshSource;
import com.danieljglover.allinslayer.ui.SlayerDebugSnapshot;
import com.danieljglover.allinslayer.ui.SlayerPanelState;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.Test;

/**
 * T09 - TaskHeader. Three humane lines, minutes not seconds, the humane RefreshSource map, the
 * remaining line hidden at 0, the name rendered literally, and {@link TaskHeader#update} mutating in
 * place.
 */
public class TaskHeaderTest
{
    private static final Instant UPDATED = Instant.parse("2026-06-28T12:34:56Z");

    private static SlayerPanelState taskState(String name, int remaining, RefreshSource source)
    {
        TaskData task = new TaskData();
        task.setTask(name);
        return SlayerPanelState.forTask(task, null, remaining, AdviceMode.DPS, null, null,
            source, UPDATED, SlayerDebugSnapshot.empty());
    }

    @Test
    public void metaShowsMinutesNotSecondsAndHumaneSource()
    {
        TaskHeader header = buildOnEdt(TaskHeader::new);

        runOnEdt(() -> header.update(taskState("Abyssal demons", 147, RefreshSource.MENU_CHECK)));

        String minutes = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault()).format(UPDATED);
        String withSeconds = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault()).format(UPDATED);

        String meta = header.getMetaLabel().getText();
        assertEquals("updated " + minutes + " · gem / helm check", meta);
        assertFalse("Seconds are dev-grade precision, not for the header", meta.contains(withSeconds));
    }

    @Test
    public void remainingLineShownAboveZeroHiddenAtZero()
    {
        TaskHeader header = buildOnEdt(TaskHeader::new);

        runOnEdt(() -> header.update(taskState("Abyssal demons", 147, RefreshSource.CHAT)));
        assertTrue(header.getRemainingLabel().isVisible());
        assertEquals("147 remaining", header.getRemainingLabel().getText());

        runOnEdt(() -> header.update(taskState("Abyssal demons", 0, RefreshSource.CHAT)));
        assertFalse("Remaining line hides when the count is unknown / 0",
            header.getRemainingLabel().isVisible());
    }

    @Test
    public void nameIsRenderedLiterallyNotAsMarkupOrEscaped()
    {
        TaskHeader header = buildOnEdt(TaskHeader::new);

        runOnEdt(() -> header.update(
            taskState("Aberrant <spectres> & banshees", 73, RefreshSource.VARBIT)));

        String name = header.getNameLabel().getText();
        assertEquals("Aberrant <spectres> & banshees", name);
        assertFalse("Plain text, not HTML-escaped (ADR-0004)", name.contains("&lt;"));
        assertFalse("Plain text, not wrapped in <html>", name.toLowerCase().contains("<html"));
    }

    @Test
    public void noTaskStateShowsAPlayerFacingName()
    {
        TaskHeader header = buildOnEdt(TaskHeader::new);

        runOnEdt(() -> header.update(SlayerPanelState.noTask(0, AdviceMode.DPS, null, null,
            RefreshSource.STARTUP, UPDATED, SlayerDebugSnapshot.empty())));

        assertEquals("No Slayer task", header.getNameLabel().getText());
        assertTrue(header.getMetaLabel().getText().endsWith("startup"));
    }

    @Test
    public void updateMutatesTheSameLabelInstances()
    {
        TaskHeader header = buildOnEdt(TaskHeader::new);
        runOnEdt(() -> header.update(taskState("Abyssal demons", 147, RefreshSource.CHAT)));

        javax.swing.JLabel name = header.getNameLabel();
        javax.swing.JLabel remaining = header.getRemainingLabel();
        javax.swing.JLabel meta = header.getMetaLabel();

        runOnEdt(() -> header.update(taskState("Greater demons", 146, RefreshSource.MANUAL)));

        assertSame(name, header.getNameLabel());
        assertSame(remaining, header.getRemainingLabel());
        assertSame(meta, header.getMetaLabel());
        assertEquals("Greater demons", name.getText());
        assertEquals("146 remaining", remaining.getText());
    }

    @Test
    public void humaneSourceMapCoversEveryEnumValue()
    {
        assertEquals("startup", TaskHeader.humaneSource(RefreshSource.STARTUP));
        assertEquals("manual refresh", TaskHeader.humaneSource(RefreshSource.MANUAL));
        assertEquals("task changed", TaskHeader.humaneSource(RefreshSource.VARBIT));
        assertEquals("new assignment", TaskHeader.humaneSource(RefreshSource.CHAT));
        assertEquals("gem / helm check", TaskHeader.humaneSource(RefreshSource.MENU_CHECK));
        assertEquals("gear update", TaskHeader.humaneSource(RefreshSource.ITEM_CONTAINER));
        assertEquals("login", TaskHeader.humaneSource(RefreshSource.GAME_STATE));
        assertEquals("mode change", TaskHeader.humaneSource(RefreshSource.MODE_TOGGLE));
        assertEquals("location change", TaskHeader.humaneSource(RefreshSource.LOCATION_SELECT));
        assertEquals("master change", TaskHeader.humaneSource(RefreshSource.MASTER_SELECT));
    }
}

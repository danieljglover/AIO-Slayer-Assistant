package com.danieljglover.allinslayer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.TaskData;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.MenuAction;
import org.junit.Test;

public class SlayerPanelStateTest
{
    private static final Instant NOW = Instant.parse("2026-06-28T12:34:56Z");

    @Test
    public void noTaskStateHasNoTaskStatusAndDefensiveItemNameCopy()
    {
        Map<Integer, String> names = new HashMap<>();
        names.put(1, "Old name");

        SlayerPanelState state = SlayerPanelState.noTask(
            0,
            AdviceMode.DPS,
            "No bank snapshot",
            names,
            RefreshSource.MANUAL,
            NOW,
            SlayerDebugSnapshot.empty());

        names.put(1, "Changed name");

        assertEquals(PanelStatus.NO_TASK, state.getStatus());
        assertEquals("No Slayer task detected", state.getStatusMessage());
        assertNull(state.getTask());
        assertNull(state.getRecommendation());
        assertEquals("Old name", state.getItemNames().get(1));
        assertFalse(state.getItemNames().isEmpty());
    }

    @Test
    public void unsupportedTaskStateCarriesUnsupportedTargetId()
    {
        SlayerDebugSnapshot debug = SlayerDebugSnapshot.empty()
            .withSlayerState(999, 42, 7, -1, "Unsupported target", 999);

        SlayerPanelState state = SlayerPanelState.unsupportedTask(
            999,
            42,
            AdviceMode.COST,
            "10m ago",
            RefreshSource.VARBIT,
            NOW,
            debug);

        assertEquals(PanelStatus.UNSUPPORTED_TASK, state.getStatus());
        assertEquals("Unsupported Slayer target: 999", state.getStatusMessage());
        assertEquals(999, state.getDebug().getUnsupportedTargetId());
        assertEquals(RefreshSource.VARBIT, state.getRefreshSource());
    }

    @Test
    public void taskStateStatusReflectsWhetherLoadoutExists()
    {
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");

        SlayerPanelState missingLoadout = SlayerPanelState.forTask(
            task,
            null,
            84,
            AdviceMode.DPS,
            null,
            null,
            RefreshSource.CHAT,
            NOW,
            SlayerDebugSnapshot.empty());

        SlayerPanelState withLoadout = SlayerPanelState.forTask(
            task,
            new Recommendation(),
            84,
            AdviceMode.COST,
            "1m ago",
            null,
            RefreshSource.ITEM_CONTAINER,
            NOW,
            SlayerDebugSnapshot.empty());

        assertEquals(PanelStatus.TASK_WITHOUT_LOADOUT, missingLoadout.getStatus());
        assertEquals("No owned loadout found", missingLoadout.getStatusMessage());
        assertEquals(PanelStatus.TASK_WITH_LOADOUT, withLoadout.getStatus());
        assertEquals("Ready", withLoadout.getStatusMessage());
    }

    @Test
    public void shortFactoriesDefaultPricesEmptyAndDeveloperModeFalse()
    {
        SlayerPanelState noTask = SlayerPanelState.noTask(
            0, AdviceMode.DPS, "No bank snapshot", null, RefreshSource.MANUAL, NOW,
            SlayerDebugSnapshot.empty());
        SlayerPanelState unsupported = SlayerPanelState.unsupportedTask(
            999, 42, AdviceMode.COST, "10m ago", RefreshSource.VARBIT, NOW,
            SlayerDebugSnapshot.empty());

        TaskData task = new TaskData();
        task.setTask("Bloodvelds");
        SlayerPanelState shortForTask = SlayerPanelState.forTask(
            task, new Recommendation(), 10, AdviceMode.DPS, null, null, RefreshSource.CHAT, NOW,
            SlayerDebugSnapshot.empty());

        for (SlayerPanelState state : new SlayerPanelState[]{noTask, unsupported, shortForTask})
        {
            assertNotNull("itemPrices must never be null", state.getItemPrices());
            assertTrue("itemPrices must default empty", state.getItemPrices().isEmpty());
            assertFalse("developerMode must default false", state.isDeveloperMode());
        }
    }

    @Test
    public void fullForTaskCarriesPricesAndDeveloperModeWithDefensiveCopy()
    {
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");

        Map<Integer, Integer> prices = new HashMap<>();
        prices.put(4151, 2_000_000);

        SlayerPanelState state = SlayerPanelState.forTask(
            task,
            new Recommendation(),
            84,
            AdviceMode.DPS,
            "1m ago",
            null,
            prices,
            RefreshSource.MANUAL,
            NOW,
            SlayerDebugSnapshot.empty(),
            "Slayer Tower",
            90,
            true);

        prices.put(4151, 1);

        assertEquals(Integer.valueOf(2_000_000), state.getItemPrices().get(4151));
        assertTrue(state.isDeveloperMode());
        assertEquals("Slayer Tower", state.getSelectedLocationName());
        assertEquals(90, state.getPlayerSlayerLevel());
    }

    @Test
    public void developerModeOverloadsCarryFlagWhileShortFactoriesStayFalse()
    {
        SlayerPanelState noTaskDev = SlayerPanelState.noTask(
            0, AdviceMode.DPS, "No bank snapshot", null, RefreshSource.MANUAL, NOW,
            SlayerDebugSnapshot.empty(), true);
        SlayerPanelState unsupportedDev = SlayerPanelState.unsupportedTask(
            999, 42, AdviceMode.COST, "10m ago", RefreshSource.VARBIT, NOW,
            SlayerDebugSnapshot.empty(), true);

        assertTrue("noTask overload must carry developerMode", noTaskDev.isDeveloperMode());
        assertEquals(PanelStatus.NO_TASK, noTaskDev.getStatus());
        assertTrue("unsupportedTask overload must carry developerMode", unsupportedDev.isDeveloperMode());
        assertEquals(PanelStatus.UNSUPPORTED_TASK, unsupportedDev.getStatus());
        // The unsupported overload preserves the target-id reconciliation of the 7-arg form.
        assertEquals(999, unsupportedDev.getDebug().getUnsupportedTargetId());

        // The original 7-arg factories still default developerMode=false (zero blast radius).
        SlayerPanelState noTaskDefault = SlayerPanelState.noTask(
            0, AdviceMode.DPS, null, null, RefreshSource.MANUAL, NOW, SlayerDebugSnapshot.empty());
        SlayerPanelState unsupportedDefault = SlayerPanelState.unsupportedTask(
            999, 42, AdviceMode.COST, null, RefreshSource.VARBIT, NOW, SlayerDebugSnapshot.empty());
        assertFalse(noTaskDefault.isDeveloperMode());
        assertFalse(unsupportedDefault.isDeveloperMode());
    }

    @Test
    public void fullForTaskCarriesRequiredItemOwnedWhileShortOverloadsDefaultNull()
    {
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");

        SlayerPanelState owned = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, null, null, null, RefreshSource.MANUAL,
            NOW, SlayerDebugSnapshot.empty(), null, 90, false, Boolean.TRUE);
        SlayerPanelState missing = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, null, null, null, RefreshSource.MANUAL,
            NOW, SlayerDebugSnapshot.empty(), null, 90, false, Boolean.FALSE);
        SlayerPanelState shortForTask = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, null, null, RefreshSource.MANUAL, NOW,
            SlayerDebugSnapshot.empty());

        assertEquals(Boolean.TRUE, owned.getRequiredItemOwned());
        assertEquals(Boolean.FALSE, missing.getRequiredItemOwned());
        assertNull("the short overloads leave required-item ownership unknown",
            shortForTask.getRequiredItemOwned());
    }

    @Test
    public void bankStaleOverloadCarriesTheFlagWhileShorterFactoriesDefaultFalse()
    {
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");

        SlayerPanelState stale = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, "8d ago", null, null, RefreshSource.MANUAL,
            NOW, SlayerDebugSnapshot.empty(), null, 90, false, null, null, null, true);
        SlayerPanelState fresh = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, "12m ago", null, null, RefreshSource.MANUAL,
            NOW, SlayerDebugSnapshot.empty(), null, 90, false, null, null, null, false);
        SlayerPanelState shortForTask = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, null, null, RefreshSource.CHAT, NOW,
            SlayerDebugSnapshot.empty());

        assertTrue("the bankStale overload carries the flag", stale.isBankStale());
        assertFalse("false flows through the overload", fresh.isBankStale());
        assertFalse("the shorter overloads default bankStale false", shortForTask.isBankStale());
    }

    @Test
    public void masterOverloadCarriesSelectionAndNamesWhileShorterFactoriesDefaultEmpty()
    {
        // WA-11 (ADR-0018 #8): the fullest forTask threads the selected master + the masterId->name
        // display map; every shorter overload defaults them (null / empty) so call sites compile
        // unchanged and render today's master-less Task card.
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        Map<String, String> masterNames = new HashMap<>();
        masterNames.put("duradel", "Duradel");

        SlayerPanelState state = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, "12m ago", null, null, RefreshSource.MANUAL,
            NOW, SlayerDebugSnapshot.empty(), null, 90, false, null, null, null, false,
            "duradel", masterNames);

        masterNames.put("duradel", "Changed");

        assertEquals("duradel", state.getSelectedMaster());
        assertEquals("masterNames must be defensively copied", "Duradel",
            state.getMasterNames().get("duradel"));

        SlayerPanelState shortForTask = SlayerPanelState.forTask(
            task, new Recommendation(), 84, AdviceMode.DPS, null, null, RefreshSource.CHAT, NOW,
            SlayerDebugSnapshot.empty());
        assertNull("shorter overloads default no master selection", shortForTask.getSelectedMaster());
        assertNotNull("masterNames must never be null", shortForTask.getMasterNames());
        assertTrue(shortForTask.getMasterNames().isEmpty());

        SlayerPanelState noTask = SlayerPanelState.noTask(
            0, AdviceMode.DPS, null, null, RefreshSource.MANUAL, NOW, SlayerDebugSnapshot.empty());
        assertNull(noTask.getSelectedMaster());
        assertTrue(noTask.getMasterNames().isEmpty());
    }

    @Test
    public void bankNotScannedMasterOverloadCarriesSelectionWhileTheOldOneDefaults()
    {
        // The bank-gate state renders the Task card too, so the master selector/context must survive
        // the gate: the new overload threads selectedMaster + masterNames, the old one defaults them.
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        Map<String, String> masterNames = new HashMap<>();
        masterNames.put("nieve", "Nieve");

        SlayerPanelState state = SlayerPanelState.bankNotScanned(
            task, 84, AdviceMode.DPS, null, null, RefreshSource.MANUAL, NOW,
            SlayerDebugSnapshot.empty(), null, 90, false, null, "nieve", masterNames);
        assertEquals("nieve", state.getSelectedMaster());
        assertEquals("Nieve", state.getMasterNames().get("nieve"));

        SlayerPanelState old = SlayerPanelState.bankNotScanned(
            task, 84, AdviceMode.DPS, null, null, RefreshSource.MANUAL, NOW,
            SlayerDebugSnapshot.empty(), null, 90, false, null);
        assertNull(old.getSelectedMaster());
        assertTrue(old.getMasterNames().isEmpty());
    }

    @Test
    public void bankNotScannedCarriesTaskAndDeveloperModeWithNullRecommendation()
    {
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");

        Map<Integer, String> names = new HashMap<>();
        names.put(1, "Old name");

        SlayerPanelState state = SlayerPanelState.bankNotScanned(
            task,
            84,
            AdviceMode.DPS,
            null,
            names,
            RefreshSource.MANUAL,
            NOW,
            SlayerDebugSnapshot.empty(),
            "Slayer Tower",
            90,
            true,
            Boolean.TRUE);

        names.put(1, "Changed name");

        assertEquals(PanelStatus.BANK_NOT_SCANNED, state.getStatus());
        assertNull("the bank-gate state produces no recommendation", state.getRecommendation());
        assertEquals("Abyssal demons", state.getTask().getTask());
        assertTrue("developerMode must be carried so Diagnostics stays reachable", state.isDeveloperMode());
        assertEquals("Slayer Tower", state.getSelectedLocationName());
        assertEquals(90, state.getPlayerSlayerLevel());
        assertEquals(Boolean.TRUE, state.getRequiredItemOwned());
        assertEquals("Old name", state.getItemNames().get(1));
        assertTrue("itemPrices must never be null", state.getItemPrices().isEmpty());
    }

    @Test
    public void debugSnapshotKeepsMenuDiagnosticsWhenAddingSlayerState()
    {
        SlayerDebugSnapshot debug = SlayerDebugSnapshot.empty()
            .withMenu("Check", MenuAction.CC_OP, 11864, 11864, true)
            .withSlayerState(12, 74, 0, -1, "Matched Abyssal demons", -1);

        assertEquals("Check", debug.getMenuOption());
        assertEquals(MenuAction.CC_OP, debug.getMenuAction());
        assertEquals(11864, debug.getRawItemId());
        assertEquals(11864, debug.getMappedItemId());
        assertEquals(12, debug.getSlayerTargetVarp());
        assertEquals(74, debug.getSlayerCountVarp());
        assertEquals("Matched Abyssal demons", debug.getDetectorResult());
    }
}

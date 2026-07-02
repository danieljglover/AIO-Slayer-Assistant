package com.danieljglover.allinslayer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.loadout.Consumables;
import com.danieljglover.allinslayer.loadout.MagicSetup;
import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.TaskUnlock;
import com.danieljglover.allinslayer.model.UnlockType;
import com.danieljglover.allinslayer.model.Weakness;
import com.danieljglover.allinslayer.ui.components.InvisibleScrollBarUI;
import com.danieljglover.allinslayer.ui.components.KeyValueRow;
import com.danieljglover.allinslayer.ui.components.LoadoutItemRow;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import org.junit.Test;

/**
 * Tests for the merged single-scrollable dashboard (ADR-0002, Wave 5 T18-T22). The panel is
 * constructed with a fake {@link ItemIconRenderer} (ADR-0001 seam) so no live {@code ItemManager} is
 * needed; the fake records what the panel asked it to draw.
 */
public class SlayerPanelTest
{
    private RecordingRenderer renderer;

    // ---- T18: shell restructure ------------------------------------------------------------------

    @Test
    public void noTaskShowsHeaderAndErrorPanelWithExportDisabled() throws Exception
    {
        SlayerPanel panel = panel();

        runOnEdt(() -> panel.render(SlayerPanelState.noTask(
            0, AdviceMode.DPS, null, null, RefreshSource.STARTUP,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty())));

        assertEquals("No Slayer task", header(panel, "task-header-name").getText());
        assertNotNull("the empty state is a PluginErrorPanel", firstOfType(panel, PluginErrorPanel.class));
        assertFalse("export is disabled without a recommendation (FR-9)",
            button(panel, "action-export").isEnabled());
    }

    @Test
    public void dashboardDropsTheTabScaffold() throws Exception
    {
        SlayerPanel panel = panel();

        runOnEdt(() -> panel.render(SlayerPanelState.noTask(
            0, AdviceMode.DPS, null, null, RefreshSource.STARTUP,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty())));

        for (Component component : allComponents(panel))
        {
            assertFalse("the MaterialTab split is gone (P1-6)", component instanceof MaterialTab);
            assertFalse("the MaterialTabGroup is gone (P1-6)", component instanceof MaterialTabGroup);
        }
    }

    @Test
    public void bodyScrollsInsideOneHiddenScrollbarPaneWithAnchoredHeader() throws Exception
    {
        SlayerPanel panel = panel();

        runOnEdt(() -> panel.render(SlayerPanelState.noTask(
            0, AdviceMode.DPS, null, null, RefreshSource.STARTUP,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty())));

        List<JScrollPane> scrollPanes = allOfType(panel, JScrollPane.class);
        assertEquals("exactly one scroll body (ADR-0002)", 1, scrollPanes.size());
        JScrollPane scroll = scrollPanes.get(0);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
        // Vertical AS_NEEDED so a tall dashboard is actually scrollable via wheel/bar (RV3 N-B).
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, scroll.getVerticalScrollBarPolicy());
        assertTrue("mouse-wheel scrolling stays on", scroll.isWheelScrollingEnabled());

        // The header band (task header + action bar) is anchored, never inside the scroll body.
        assertFalse("task header is anchored, not scrolled",
            isInside(header(panel, "task-header-name"), scroll));
        assertFalse("action bar is anchored, not scrolled",
            isInside(button(panel, "action-export"), scroll));
    }

    // ---- T19: task section -----------------------------------------------------------------------

    @Test
    public void taskSectionShowsSlayerLevelWeaknessAndRequiredItemRowOwned() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");
        task.setSlayerLevel(60);
        task.setWeakness(new Weakness(CombatStyle.MAGIC, "smoke"));
        task.setRequiredItemId(4166); // nose peg
        task.setRequiredItemName("Nose peg");

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.HEAD, 4166); // required item is part of the loadout => owned
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MAGIC);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());

        Map<Integer, String> names = new HashMap<>();
        names.put(4166, "Nose peg");

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 73, AdviceMode.DPS, null, names, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty())));

        // remaining is shown in the (anchored) header, not the task card.
        assertEquals("73 remaining", header(panel, "task-header-remaining").getText());

        List<String> texts = labelTexts(panel);
        assertTrue("Slayer level row", texts.contains("60"));
        assertTrue("Weakness row", texts.stream().anyMatch(t -> t.contains("smoke")));

        LoadoutItemRow required = firstOfType(panel, LoadoutItemRow.class);
        assertNotNull("required item renders as a sprite row", required);
        assertEquals("Nose peg", required.getNameLabel().getText());
        assertEquals("owned required item uses primary text",
            SlayerTheme.TEXT_PRIMARY, required.getNameLabel().getForeground());
    }

    @Test
    public void taskSectionMarksRequiredItemBlockedWhenGenuinelyMissing() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");
        task.setSlayerLevel(60);
        task.setRequiredItemId(4166);
        task.setRequiredItemName("Nose peg");

        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MAGIC);
        rec.setWorn(new EnumMap<>(EquipmentSlot.class));
        rec.setInventory(new ArrayList<>());

        // The authoritative signal: the plugin resolved that the player does NOT own the item.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 73, AdviceMode.DPS, null, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 73, false,
            Boolean.FALSE)));

        LoadoutItemRow required = firstOfType(panel, LoadoutItemRow.class);
        assertNotNull(required);
        assertEquals("a genuinely missing required item is blocked (red)",
            SlayerTheme.STATE_BLOCKED, required.getNameLabel().getForeground());
    }

    @Test
    public void taskSectionMarksRequiredItemOwnedEvenWhenABetterItemIsWorn() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");
        task.setSlayerLevel(60);
        task.setRequiredItemId(4166); // nose peg required, but a slayer helm is worn in the HEAD slot
        task.setRequiredItemName("Nose peg");

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.HEAD, 11864); // a higher-tier item, NOT the required id
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MAGIC);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());

        Map<Integer, String> names = new HashMap<>();
        names.put(4166, "Nose peg");

        // requiredItemOwned unknown (null) here -> the simplified rule (LD13) stays neutral (OWNED),
        // never a false "missing" red, when there is no authoritative ownership signal.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 73, AdviceMode.DPS, null, names, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty())));

        LoadoutItemRow required = firstOfType(panel, LoadoutItemRow.class);
        assertNotNull(required);
        assertEquals("Nose peg", required.getNameLabel().getText());
        assertEquals("an owned required item is OWNED even when a better item is worn (B1)",
            SlayerTheme.TEXT_PRIMARY, required.getNameLabel().getForeground());
    }

    @Test
    public void taskSectionDoesNotRedFlagAnOwnedRequiredItemWithoutALoadout() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");
        task.setSlayerLevel(60);
        task.setRequiredItemId(4166);
        task.setRequiredItemName("Nose peg");

        // No loadout (rec == null), but the plugin resolved the player owns the required item.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, null, 73, AdviceMode.DPS, null, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 73, false,
            Boolean.TRUE)));

        LoadoutItemRow required = firstOfType(panel, LoadoutItemRow.class);
        assertNotNull("the required item still renders without a loadout (FR-6)", required);
        assertNotEquals("a required item the player owns is never red, even with no loadout (B1)",
            SlayerTheme.STATE_BLOCKED, required.getNameLabel().getForeground());
        assertEquals("an owned required item is OWNED/primary",
            SlayerTheme.TEXT_PRIMARY, required.getNameLabel().getForeground());
    }

    @Test
    public void slayerLevelRowIsGreenWhenMetAndRedWhenBelow() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(new EnumMap<>(EquipmentSlot.class));
        rec.setInventory(new ArrayList<>());

        // Player meets the requirement (90 >= 85) -> requirement-met green (P1-10, uses STATE_MET).
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, null, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false, null)));
        assertEquals("a met Slayer requirement is green",
            SlayerTheme.STATE_MET, slayerLevelValue(panel).getForeground());

        // Player below the requirement (70 < 85) -> blocked red.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, null, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 70, false, null)));
        assertEquals("an unmet Slayer requirement is blocked red",
            SlayerTheme.STATE_BLOCKED, slayerLevelValue(panel).getForeground());
    }

    // ---- T20: where & how section ----------------------------------------------------------------

    @Test
    public void whereSectionShowsRecommendedLocationAsTheSoleBrandElementWithTags() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = locatedTask();
        Recommendation rec = locatedRecommendation();

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 73, AdviceMode.DPS, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 75)));

        JLabel recommended = componentByName(panel, "where-recommended", JLabel.class);
        assertEquals("Catacombs of Kourend", recommended.getText());
        assertEquals("the recommended location is the only brand element in the body",
            SlayerTheme.ACCENT_BRAND, recommended.getForeground());

        // No section header is painted brand (brand is reserved for real state).
        for (Component component : allComponents(scrollBody(panel)))
        {
            if (component instanceof JLabel && "section-header".equals(component.getName()))
            {
                assertFalse("section headers are never brand",
                    SlayerTheme.ACCENT_BRAND.equals(component.getForeground()));
            }
        }

        List<String> tags = tagTexts(panel);
        assertTrue("multi tag", tags.contains("multi"));
        assertTrue("burst tag", tags.contains("burst"));
        assertTrue("konar tag", tags.contains("konar"));
    }

    @Test
    public void locationComboIsBuiltOnceAndKeepsSelectionAcrossReRenders() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = locatedTask();
        Recommendation rec = locatedRecommendation();

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 73, AdviceMode.DPS, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Slayer Tower", 75)));

        JComboBox<?> first = locationCombo(panel);
        assertEquals("Slayer Tower", first.getSelectedItem());
        assertTrue(hasComboItem(first, "Catacombs of Kourend"));
        assertTrue(hasComboItem(first, "Stronghold Slayer Cave"));

        // A reactive re-render (a kill ticks the count) must not recreate the combo nor drop the pick.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 72, AdviceMode.DPS, null, null, RefreshSource.VARBIT,
            Instant.parse("2026-06-28T12:35:30Z"), SlayerDebugSnapshot.empty(),
            "Slayer Tower", 75)));

        JComboBox<?> second = locationCombo(panel);
        assertSame("the combo is built once and reused (ADR-0002)", first, second);
        assertEquals("the chosen location persists across reactive re-renders", "Slayer Tower",
            second.getSelectedItem());
    }

    @Test
    public void selectingALocationFiresTheCallback() throws Exception
    {
        SlayerPanel panel = panel();
        AtomicReference<String> selected = new AtomicReference<>();
        panel.setOnSelectLocation(selected::set);
        TaskData task = locatedTask();
        Recommendation rec = locatedRecommendation();

        runOnEdt(() ->
        {
            panel.render(SlayerPanelState.forTask(
                task, rec, 73, AdviceMode.DPS, null, null, RefreshSource.MANUAL,
                Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
                "Slayer Tower", 75));
            locationCombo(panel).setSelectedItem("Catacombs of Kourend");
        });

        assertEquals("Catacombs of Kourend", selected.get());
    }

    // ---- DT-FE1: Where&How order (variant -> attack style -> location -> recommended prose, G7) ---

    @Test
    public void whereSectionRendersVariantAboveLocationAboveRecommendedProse() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() ->
        {
            // A multi-variant, multi-location task so both combos are visible, plus a recommendation so
            // the recommended-location headline renders.
            panel.render(variantState(variantTask(), null, null));
            panel.setSize(SlayerTheme.PANEL_WIDTH, 4000);
            layoutTree(panel);
        });

        Container where = componentByName(panel, "where-section", Container.class);
        int variantTop = topEdgeIn(variantCombo(panel), where);
        int locationTop = topEdgeIn(locationCombo(panel), where);
        int recommendedTop = topEdgeIn(componentByName(panel, "where-recommended", JLabel.class), where);

        assertTrue("the variant control reads above the location control (G7)",
            variantTop < locationTop);
        assertTrue("the recommended-location headline/prose reads below the location control (G7), "
                + "not above the variant as it did before",
            locationTop < recommendedTop);
    }

    // ---- DT-FE2: location dropdown filtered by the resolved variant (ADR-0017 #1/#6) -------------

    @Test
    public void locationDropdownIsFilteredToTheSelectedVariantsLocationSubset() throws Exception
    {
        SlayerPanel panel = panel();
        // variantTask() has three task locations: Catacombs of Kourend, Slayer Tower, Stronghold Slayer
        // Cave. The compiler (DT-B5) derives a per-variant subset of those names; here the default
        // variant links only to Slayer Tower.
        TaskData task = variantTask();
        task.getVariants().get(0).setLocationNames(Arrays.asList("Slayer Tower"));

        runOnEdt(() -> panel.render(variantState(task, "Aberrant spectre", null)));

        JComboBox<?> combo = locationCombo(panel);
        assertEquals("only the resolved variant's linked locations are listed (ADR-0017 #6)",
            1, combo.getItemCount());
        assertTrue("the variant's linked location is present", hasComboItem(combo, "Slayer Tower"));
        assertFalse("a task location outside the variant's subset is filtered out",
            hasComboItem(combo, "Catacombs of Kourend"));
    }

    @Test
    public void locationDropdownMatchesDriftedLocationNamesViaTheSharedRule() throws Exception
    {
        // WB-2 (D6): the same case/whitespace-drifted row the advisor test uses - both call sites
        // now delegate to MonsterVariant.locationNameMatches, so panel and advisor cannot disagree.
        SlayerPanel panel = panel();
        TaskData task = variantTask();
        task.getVariants().get(0).setLocationNames(Arrays.asList(" slayer tower "));

        runOnEdt(() -> panel.render(variantState(task, "Aberrant spectre", null)));

        JComboBox<?> combo = locationCombo(panel);
        assertEquals(1, combo.getItemCount());
        assertTrue("the drifted row still resolves the task's display name",
            hasComboItem(combo, "Slayer Tower"));
    }

    @Test
    public void methodProseIsLabelledAsTheWikiMethod() throws Exception
    {
        // WB-3 (D7): the method prose is the wiki's recommendedMethod free text, which can differ
        // from the effective combat style the loadout actually gears for - label it honestly as
        // the wiki's answer instead of implying it is the engine's.
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(locatedTask(), null, null)));

        List<String> labels = labelTexts(componentByName(panel, "where-section", Container.class));
        assertTrue("the prose caption reads 'Wiki method'", labels.contains("Wiki method"));
        assertFalse("the bare 'Method' caption is gone", labels.contains("Method"));
    }

    @Test
    public void locationDropdownFallsBackToAllTaskLocationsForAnUnlinkedVariant() throws Exception
    {
        SlayerPanel panel = panel();
        // variantTask()'s variants carry no locationNames (null) - the fallback sentinel = all task
        // locations apply (FR-6, byte-identical to today).
        runOnEdt(() -> panel.render(variantState(variantTask(), "Aberrant spectre", null)));

        JComboBox<?> combo = locationCombo(panel);
        assertEquals("null/empty locationNames shows all task locations (FR-6)", 3, combo.getItemCount());
        assertTrue(hasComboItem(combo, "Catacombs of Kourend"));
        assertTrue(hasComboItem(combo, "Slayer Tower"));
        assertTrue(hasComboItem(combo, "Stronghold Slayer Cave"));
    }

    // ---- MV-FE1: variant selector ----------------------------------------------------------------

    @Test
    public void variantComboListsEachVariantWithBossSuffixWhenMultiple() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = variantTask();

        runOnEdt(() -> panel.render(variantState(task, null, null)));

        JComboBox<?> combo = variantCombo(panel);
        assertTrue("multi-variant task shows the variant combo (FR-3)", combo.isVisible());
        assertEquals(3, combo.getItemCount());
        assertTrue(hasComboItem(combo, "Aberrant spectre"));
        assertTrue(hasComboItem(combo, "Deviant spectre"));
        assertTrue("boss variant carries the (Boss) marker (FR-7)",
            hasComboItem(combo, "Repugnant spectre (Boss)"));
    }

    @Test
    public void variantComboHiddenAndInertForSingleVariantTask() throws Exception
    {
        SlayerPanel panel = panel();
        AtomicReference<String> fired = new AtomicReference<>();
        panel.setOnSelectVariant(fired::set);

        TaskData task = locatedTask();
        MonsterVariant only = variant("Aberrant spectre", true, CombatStyle.MAGIC, false);
        task.setVariants(Arrays.asList(only));

        runOnEdt(() -> panel.render(variantState(task, null, null)));

        JComboBox<?> combo = variantCombo(panel);
        assertFalse("single-variant task hides the variant combo (FR-3)", combo.isVisible());
        assertNull("reconciling a single variant must not fire the callback", fired.get());
    }

    @Test
    public void variantComboHiddenForNoVariantTask() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(locatedTask(), null, null)));
        assertFalse("a task with no variants hides the variant combo", variantCombo(panel).isVisible());
    }

    @Test
    public void variantComboPreselectsTheDefaultVariantWhenNoSelection() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null)));
        assertEquals("the isDefault variant is pre-selected (FR-6)", "Aberrant spectre",
            variantCombo(panel).getSelectedItem());
    }

    @Test
    public void variantComboPreselectsTheSelectedVariantNameWithItsLabel() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(variantTask(), "Repugnant spectre", null)));
        assertEquals("the selected boss variant is pre-selected with its (Boss) label",
            "Repugnant spectre (Boss)", variantCombo(panel).getSelectedItem());
    }

    @Test
    public void selectingAVariantFiresTheCallbackWithTheRawName() throws Exception
    {
        SlayerPanel panel = panel();
        AtomicReference<String> selected = new AtomicReference<>();
        panel.setOnSelectVariant(selected::set);

        runOnEdt(() ->
        {
            panel.render(variantState(variantTask(), null, null));
            variantCombo(panel).setSelectedItem("Repugnant spectre (Boss)");
        });

        assertEquals("the callback gets the raw variant name, not the display label",
            "Repugnant spectre", selected.get());
    }

    @Test
    public void variantComboPreselectsExactlyTheSharedResolverVariant() throws Exception
    {
        // MV-FX / code-review S1: the panel pre-selects exactly the variant MonsterVariant.resolve
        // picks - the SAME shared rule the advisor gears from - so the two can never drift. Covers the
        // default-rule, an explicit name match, and the all-boss-no-default (first listed) case.
        SlayerPanel panel = panel();

        TaskData multi = variantTask();
        runOnEdt(() -> panel.render(variantState(multi, null, null)));
        assertEquals("no selection pre-selects the shared resolver's default-rule variant",
            expectedLabel(MonsterVariant.resolve(multi, null)), variantCombo(panel).getSelectedItem());

        runOnEdt(() -> panel.render(variantState(multi, "Repugnant spectre", null)));
        assertEquals("an explicit name pre-selects the shared resolver's matched variant",
            expectedLabel(MonsterVariant.resolve(multi, "Repugnant spectre")),
            variantCombo(panel).getSelectedItem());

        TaskData allBoss = locatedTask();
        allBoss.setWeakness(new Weakness(CombatStyle.MELEE, null));
        allBoss.setVariants(Arrays.asList(
            variant("Abyssal Sire", false, CombatStyle.MAGIC, true),
            variant("K'ril Tsutsaroth", false, CombatStyle.MELEE, true)));
        SlayerPanel bossPanel = panel();
        runOnEdt(() -> bossPanel.render(variantState(allBoss, null, null)));
        assertEquals("all-boss task with no default pre-selects the shared resolver's first variant",
            expectedLabel(MonsterVariant.resolve(allBoss, null)),
            variantCombo(bossPanel).getSelectedItem());
    }

    /** The combo label the panel shows for a variant: raw name + a "(Boss)" suffix for bosses (FR-7). */
    private static String expectedLabel(MonsterVariant v)
    {
        return v.isBoss() ? v.getName() + " (Boss)" : v.getName();
    }

    @Test
    public void variantComboIsBuiltOnceAndKeepsSelectionAcrossReRenders() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = variantTask();

        runOnEdt(() -> panel.render(variantState(task, "Deviant spectre", null)));
        JComboBox<?> first = variantCombo(panel);
        assertEquals("Deviant spectre", first.getSelectedItem());

        runOnEdt(() -> panel.render(variantState(task, "Deviant spectre", null)));
        JComboBox<?> second = variantCombo(panel);
        assertSame("the variant combo is built once and reused (ADR-0002)", first, second);
        assertEquals("the chosen variant persists across reactive re-renders", "Deviant spectre",
            second.getSelectedItem());
    }

    // ---- MV-FE3: method selector -----------------------------------------------------------------

    @Test
    public void methodSelectorOffersAllThreeStyles() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null)));
        assertNotNull(methodButton(panel, "method-melee"));
        assertNotNull(methodButton(panel, "method-ranged"));
        assertNotNull(methodButton(panel, "method-magic"));
    }

    @Test
    public void methodSelectorDefaultsToTheVariantsRecommendedStyle() throws Exception
    {
        SlayerPanel panel = panel();
        // The default variant (Aberrant spectre) is MAGIC-weak -> magic is pre-selected.
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null)));
        assertTrue("the recommended style is selected by default (FR-6)",
            methodButton(panel, "method-magic").isSelected());
        assertFalse(methodButton(panel, "method-melee").isSelected());
    }

    @Test
    public void methodSelectorReflectsTheSelectedMethodOverride() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(variantTask(), null, CombatStyle.RANGED)));
        assertTrue("an explicit method override is reflected", methodButton(panel, "method-ranged").isSelected());
        assertFalse(methodButton(panel, "method-magic").isSelected());
    }

    @Test
    public void selectingAMethodFiresTheCallbackWithItsLabel() throws Exception
    {
        SlayerPanel panel = panel();
        AtomicReference<String> selected = new AtomicReference<>();
        panel.setOnSelectMethod(selected::set);

        runOnEdt(() ->
        {
            panel.render(variantState(variantTask(), null, null));
            methodButton(panel, "method-ranged").doClick();
        });

        assertEquals("Ranged", selected.get());
    }

    @Test
    public void reflectingTheMethodDoesNotFireTheCallback() throws Exception
    {
        SlayerPanel panel = panel();
        AtomicReference<String> fired = new AtomicReference<>();
        panel.setOnSelectMethod(fired::set);

        runOnEdt(() -> panel.render(variantState(variantTask(), null, CombatStyle.RANGED)));

        assertNull("a programmatic reconcile must not fire the method callback", fired.get());
    }

    @Test
    public void methodWithNoViableLoadoutIsDisabledWhileTheOthersStayEnabled() throws Exception
    {
        SlayerPanel panel = panel();
        // The effective method (MAGIC) produced no loadout (rec == null -> TASK_WITHOUT_LOADOUT): the
        // narrow no-owned-weapon / magic-no-spell case the backend flagged.
        runOnEdt(() -> panel.render(variantState(variantTask(), null, CombatStyle.MAGIC, /*rec*/ null)));

        assertFalse("the non-viable effective method is disabled",
            methodButton(panel, "method-magic").isEnabled());
        assertTrue("other methods stay enabled (no per-method signal to disable them)",
            methodButton(panel, "method-melee").isEnabled());
        assertTrue(methodButton(panel, "method-ranged").isEnabled());
    }

    @Test
    public void allMethodsEnabledWhenAViableLoadoutExists() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(variantTask(), null, CombatStyle.MAGIC)));
        assertTrue(methodButton(panel, "method-melee").isEnabled());
        assertTrue(methodButton(panel, "method-ranged").isEnabled());
        assertTrue(methodButton(panel, "method-magic").isEnabled());
    }

    // ---- WA-11: master selector + master-context Task section (ADR-0018 #8/#9, PD-E) --------------

    @Test
    public void taskSectionShowsTheMasterContextLineForTheSelectedMaster() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(masterState(masterTask(), "duradel", masterNames())));

        JTextArea note = componentByName(panel, "task-master-context", JTextArea.class);
        assertEquals("the selected master's amounts + extension render as one context line (WA-11)",
            "Duradel gives 130-200; extended 200-250 with Augment my abbies (100 pts).",
            note.getText());
        assertTrue("the master context lives in the Task card",
            isInside(note, componentByName(panel, "task-section", Container.class)));
    }

    @Test
    public void masterContextDefaultsToTheFirstAssigningMasterWhenNoSelection() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(masterState(masterTask(), null, masterNames())));

        JTextArea note = componentByName(panel, "task-master-context", JTextArea.class);
        assertTrue("no selection falls back to the first assigning master",
            note.getText().startsWith("Duradel gives 130-200"));
    }

    @Test
    public void masterContextFallsBackToTheMasterIdWhenNoDisplayNameIsThreaded() throws Exception
    {
        SlayerPanel panel = panel();
        // No masterNames map (e.g. meta resource degraded): the slug renders capitalised, never blank.
        runOnEdt(() -> panel.render(masterState(masterTask(), "duradel", null)));

        JTextArea note = componentByName(panel, "task-master-context", JTextArea.class);
        assertTrue("the slug is prettified as the display-name fallback",
            note.getText().startsWith("Duradel gives"));
    }

    @Test
    public void masterContextOmitsTheUnlockClauseWhenNoExtensionUnlockExists() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = masterTask();
        task.setUnlocks(null); // defensive: the WA-7 build gate makes this impossible on real data
        runOnEdt(() -> panel.render(masterState(task, "duradel", masterNames())));

        JTextArea note = componentByName(panel, "task-master-context", JTextArea.class);
        assertEquals("Duradel gives 130-200; extended 200-250.", note.getText());
    }

    @Test
    public void masterContextOmitsTheExtensionClauseWhenTheTaskHasNoExtendedRange() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = masterTask();
        task.setExtendedAmount(null);
        task.setUnlocks(null); // no extension data at all - just the base amounts
        runOnEdt(() -> panel.render(masterState(task, "duradel", masterNames())));

        JTextArea note = componentByName(panel, "task-master-context", JTextArea.class);
        assertEquals("no extension range means no extended clause and no dangling separator",
            "Duradel gives 130-200.", note.getText());
    }

    @Test
    public void masterComboListsAssigningMastersByDisplayNameAndPreselectsTheSelection() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(masterState(masterTask(), "nieve", masterNames())));

        JComboBox<?> combo = masterCombo(panel);
        assertTrue(combo.isVisible());
        assertEquals(2, combo.getItemCount());
        assertEquals("Duradel", combo.getItemAt(0));
        assertEquals("Nieve", combo.getItemAt(1));
        assertEquals("the threaded selection is pre-selected", "Nieve", combo.getSelectedItem());
    }

    @Test
    public void masterComboHiddenForSingleMasterAndNoMasterTasks() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData single = masterTask();
        single.setAssignedBy(Arrays.asList("duradel"));
        runOnEdt(() -> panel.render(masterState(single, null, masterNames())));
        assertFalse("a single-assigner task hides the combo (nothing to choose)",
            masterCombo(panel).isVisible());
        assertNotNull("the context line still renders for the lone master",
            componentByName(panel, "task-master-context", JTextArea.class));

        runOnEdt(() -> panel.render(variantState(locatedTask(), null, null)));
        assertFalse("a task with no assigners hides the combo", masterCombo(panel).isVisible());
    }

    @Test
    public void selectingAMasterFiresTheCallbackWithTheMasterId() throws Exception
    {
        SlayerPanel panel = panel();
        AtomicReference<String> selected = new AtomicReference<>();
        panel.setOnSelectMaster(selected::set);

        runOnEdt(() ->
        {
            panel.render(masterState(masterTask(), null, masterNames()));
            masterCombo(panel).setSelectedItem("Nieve");
        });

        assertEquals("the callback gets the masterId slug, not the display name", "nieve",
            selected.get());
    }

    @Test
    public void reconcilingTheMasterComboDoesNotFireTheCallback() throws Exception
    {
        SlayerPanel panel = panel();
        AtomicReference<String> fired = new AtomicReference<>();
        panel.setOnSelectMaster(fired::set);

        runOnEdt(() -> panel.render(masterState(masterTask(), "nieve", masterNames())));

        assertNull("a programmatic reconcile must not fire the master callback", fired.get());
    }

    @Test
    public void changingTheSelectedMasterReRendersTheTaskContext() throws Exception
    {
        // Plan R6: the Task-section self-diff key must cover selectedMaster so a selector change
        // re-renders the context (ADR-0002).
        SlayerPanel panel = panel();
        TaskData task = masterTask();
        runOnEdt(() -> panel.render(masterState(task, "duradel", masterNames())));
        assertTrue(componentByName(panel, "task-master-context", JTextArea.class)
            .getText().startsWith("Duradel"));

        runOnEdt(() -> panel.render(masterState(task, "nieve", masterNames())));
        assertEquals("switching master re-renders the context line for the new master",
            "Nieve gives 120-185; extended 200-250 with Augment my abbies (100 pts).",
            componentByName(panel, "task-master-context", JTextArea.class).getText());
    }

    @Test
    public void valueEqualMasterStateTriggersNoTaskRebuild() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = masterTask();
        runOnEdt(() -> panel.render(masterState(task, "duradel", masterNames())));
        int afterFirst = panel.sectionRebuildCount();

        // A DISTINCT but value-equal names map + same selection must not rebuild (NFR-4).
        runOnEdt(() -> panel.render(masterState(task, "duradel", masterNames())));
        assertEquals("a value-equal master slice triggers zero rebuilds", afterFirst,
            panel.sectionRebuildCount());
    }

    @Test
    public void masterWeightRowRendersOnlyWhenTheTaskCarriesAWeight() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData weighted = masterTask();
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("duradel", 12);
        weighted.setWeightByMaster(weights);
        runOnEdt(() -> panel.render(masterState(weighted, "duradel", masterNames())));
        KeyValueRow row = componentByName(panel, "task-master-weight", KeyValueRow.class);
        assertEquals("Weight", row.getKeyLabel().getText());
        assertEquals("12", row.getValueLabel().getText());

        // weightByMaster is null until WC-W lands - render nothing (team contract 2026-07-02).
        runOnEdt(() -> panel.render(masterState(masterTask(), "duradel", masterNames())));
        for (Component component : allComponents(panel))
        {
            assertFalse("no weight row without weight data",
                "task-master-weight".equals(component.getName()));
        }

        // A weight authored for another master only renders nothing for the selected one.
        runOnEdt(() -> panel.render(masterState(weighted, "nieve", masterNames())));
        for (Component component : allComponents(panel))
        {
            assertFalse("no weight row when the selected master has no weight",
                "task-master-weight".equals(component.getName()));
        }
    }

    @Test
    public void otherAssigningMastersAmountsRenderAsASecondaryLine() throws Exception
    {
        // PD-E disposition: per-master amounts render for ALL masters; the selected master owns the
        // context line, the others render compactly.
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(masterState(masterTask(), "duradel", masterNames())));

        JTextArea note = componentByName(panel, "task-master-amounts", JTextArea.class);
        assertEquals("Also: Nieve 120-185.", note.getText());
    }

    @Test
    public void skipBlockNoteRendersInTheTaskSection() throws Exception
    {
        // WD-12 (ADR-0020 #5): the advisor's master-aware skip/block advisory renders as
        // task-skip-block-note inside the Task section (NG-4 note-only).
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setSkipBlockNote("Consider blocking Aviansies (costs 100 points; you have 250). "
            + "Turael can skip this free (resets your streak).");

        runOnEdt(() -> panel.render(masterStateWithRec(masterTask(), "duradel", masterNames(), rec)));

        JTextArea note = componentByName(panel, "task-skip-block-note", JTextArea.class);
        assertTrue("the skip/block advisory renders as a note",
            note.getText().contains("Consider blocking Aviansies"));
        assertEquals("the advisory reads as primary advice, mirroring task-master-context",
            SlayerTheme.TEXT_PRIMARY, note.getForeground());
        assertTrue("the note lives in the Task section",
            isInside(note, componentByName(panel, "task-section", Container.class)));
    }

    @Test
    public void noSkipBlockNoteWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // skipBlockNote == null
        runOnEdt(() -> panel.render(masterStateWithRec(masterTask(), "duradel", masterNames(), rec)));

        Container taskSection = componentByName(panel, "task-section", Container.class);
        for (Component component : allComponents(taskSection))
        {
            assertNotEquals("no skip/block note when none is set", "task-skip-block-note",
                component.getName());
        }
    }

    private static JComboBox<?> masterCombo(Container c)
    {
        return componentByName(c, "master-combo", JComboBox.class);
    }

    // ---- MV-FE2: boss separateness note (FR-7) ---------------------------------------------------

    @Test
    public void bossVariantShowsSeparateTripNoteWithLocationInTheLoadoutCard() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = variantTask();
        Recommendation rec = locatedRecommendation();
        rec.setBoss(true);
        rec.setVariantName("Repugnant spectre");
        rec.setVariantLocation("God Wars Dungeon");
        rec.setVariantRequirement("boss - separate trip");

        runOnEdt(() -> panel.render(variantState(task, "Repugnant spectre", null, rec)));

        JTextArea note = componentByName(panel, "loadout-boss-note", JTextArea.class);
        assertTrue("the note flags it is a boss", note.getText().contains("Boss"));
        assertTrue("the note says separate trip", note.getText().contains("separate trip"));
        assertTrue("the note carries the variant location", note.getText().contains("God Wars Dungeon"));
    }

    @Test
    public void nonBossVariantShowsNoSeparateTripNote() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // boss == false
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no boss note for a non-boss variant", "loadout-boss-note",
                component.getName());
        }
    }

    @Test
    public void strategyVariantShowsWikiStrategyNoteInTheLoadoutCard() throws Exception
    {
        // FR-S5 / MV-S4: a variant with a strategy renders the loadout-strategy-note (primary +
        // secondary weapons), mirroring the boss-note pattern.
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setStrategyNote("Wiki strategy: Emberlight (MELEE); also Scorching bow (RANGED). "
            + "Switch to ranged for the shield-down window.");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-strategy-note", JTextArea.class);
        assertTrue("note names the primary weapon", note.getText().contains("Emberlight"));
        assertTrue("note names the secondary weapon", note.getText().contains("Scorching bow"));
    }

    @Test
    public void noStrategyNoteWhenRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // strategyNote == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no strategy note when none is set", "loadout-strategy-note",
                component.getName());
        }
    }

    // ---- DT-FE3: owned inventory supplies + safespot/access notes (ADR-0017 #4, DT-B9/B10) -------

    @Test
    public void loadoutRendersOwnedInventorySuppliesThroughTheInventoryGrid() throws Exception
    {
        // DT-B9 now populates rec.inventory with an owned-driven pack list (required item, Dwarf
        // multicannon parts [6,8,10,12] + a cannonball, antifire); the panel renders it through the
        // already-wired InventoryGrid (nothing new to build, but locked here for FE3 traceability).
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Blue dragons");
        task.setSlayerLevel(1);
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(Arrays.asList(6, 8, 10, 12, 2)); // cannon parts + steel cannonballs (id 2)
        rec.setEstimatedDps(10.0);
        rec.setTotalGearCost(1_000_000L);
        Map<Integer, String> names = new HashMap<>();
        names.put(4151, "Abyssal whip");

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", names, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false)));

        assertNotNull("owned inventory supplies render through the wired InventoryGrid (DT-B9/FE3)",
            componentByName(panel, "inventory-grid", Container.class));
        assertTrue("the cannon base sprite was drawn through the seam", renderer.drew(6));
        assertTrue("the cannonball sprite was drawn through the seam", renderer.drew(2));
    }

    @Test
    public void safespotLocationHintRendersAsANote() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setLocationHint("Safespot available - attack with RANGED from a safe tile.");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-location-hint", JTextArea.class);
        assertTrue("the safespot hint renders as a note", note.getText().contains("Safespot available"));
        assertEquals("the hint reads as primary advice, mirroring the strategy note",
            SlayerTheme.TEXT_PRIMARY, note.getForeground());
    }

    @Test
    public void locationAccessNoteRendersAsANote() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setAccessNote("Requires the Lunar Diplomacy quest to enter.");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-access-note", JTextArea.class);
        assertTrue("the free-text access note renders", note.getText().contains("Lunar Diplomacy"));
    }

    @Test
    public void noLocationNotesWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // locationHint == null, accessNote == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no safespot hint when none is set", "loadout-location-hint",
                component.getName());
            assertNotEquals("no access note when none is set", "loadout-access-note",
                component.getName());
        }
    }

    @Test
    public void antifireUnownedNoteRendersAsANote() throws Exception
    {
        // DT-B11 (PD-3): the advisor's antifire "note when unowned" nudge renders as loadout-antifire-note,
        // mirroring the location-hint/access-note pattern.
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setAntifireNote("Antifire recommended - none found in your bank.");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-antifire-note", JTextArea.class);
        assertTrue("the antifire nudge renders as a note", note.getText().contains("Antifire"));
        assertEquals("the nudge reads as primary advice, mirroring the strategy note",
            SlayerTheme.TEXT_PRIMARY, note.getForeground());
    }

    @Test
    public void noAntifireNoteWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // antifireNote == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no antifire note when none is set", "loadout-antifire-note",
                component.getName());
        }
    }

    @Test
    public void cannonDpsNoteRendersAsASeparateLineBesideTheHeadline() throws Exception
    {
        // WD-6 (ADR-0020 #3): the honest single-target cannon DPS line renders as loadout-cannon-dps,
        // separate from the "excl. cannon" headline. Secondary text, like the dps-scope caption.
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setCannonDpsNote("Cannon adds ~6.3 DPS (single-target)");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-cannon-dps", JTextArea.class);
        assertTrue("the cannon line names its single-target scope",
            note.getText().contains("Cannon adds") && note.getText().contains("single-target"));
        assertEquals("the cannon line reads as secondary, like the dps-scope caption",
            SlayerTheme.TEXT_SECONDARY, note.getForeground());
        // The WB-7 "excl. cannon" headline scope is still present and separate.
        assertEquals("single-target, excl. cannon",
            componentByName(panel, "loadout-dps-scope", JTextArea.class).getText());
    }

    @Test
    public void noCannonDpsNoteWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // cannonDpsNote == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no cannon line when none is set", "loadout-cannon-dps",
                component.getName());
        }
    }

    @Test
    public void survivalNoteRendersAsANote() throws Exception
    {
        // WD-3 (ADR-0020 #1): the advisor's prayer/survival advisory renders as loadout-survival-note,
        // mirroring the antifire note pattern (NG-4 note-only).
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setSurvivalNote("Protect from Melee. Max hit ~29; keep your HP above it.");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-survival-note", JTextArea.class);
        assertTrue("the survival advisory renders as a note", note.getText().contains("Protect from Melee"));
        assertEquals("the advisory reads as primary advice, mirroring the antifire note",
            SlayerTheme.TEXT_PRIMARY, note.getForeground());
    }

    @Test
    public void noSurvivalNoteWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // survivalNote == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no survival note when none is set", "loadout-survival-note",
                component.getName());
        }
    }

    @Test
    public void unlockGuardNoteRendersAsANote() throws Exception
    {
        // WA-12 (ADR-0018 #9b): the advisor's unlock-gated gear guard renders as
        // loadout-unlock-guard-note, mirroring the strategy/antifire note pattern (NG-4 note-only).
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setUnlockGuardNote(
            "Needs a Slayer unlock you haven't bought: Malevolent masquerade (slayer helmet).");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-unlock-guard-note", JTextArea.class);
        assertTrue("the unlock guard renders as a note", note.getText().contains("Malevolent masquerade"));
        assertEquals("the guard reads as primary advice, mirroring the strategy note",
            SlayerTheme.TEXT_PRIMARY, note.getForeground());
    }

    @Test
    public void noUnlockGuardNoteWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // unlockGuardNote == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no unlock guard note when none is set", "loadout-unlock-guard-note",
                component.getName());
        }
    }

    @Test
    public void strategyFallbackNoteRendersAsASecondaryNote() throws Exception
    {
        // WB-4 (D8): a malformed strategy's stat-engine fallback is disclosed, not silent.
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setStrategyFallbackNote(
            "Wiki strategy data unavailable for this variant - gear picked by the stat engine.");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-strategy-fallback-note", JTextArea.class);
        assertTrue(note.getText().contains("strategy data unavailable"));
        assertEquals("a data-quality disclosure reads as secondary, not tactical advice",
            SlayerTheme.TEXT_SECONDARY, note.getForeground());
    }

    @Test
    public void noStrategyFallbackNoteWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // strategyFallbackNote == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no fallback note when none is set", "loadout-strategy-fallback-note",
                component.getName());
        }
    }

    @Test
    public void unlockHintRendersAsASecondaryNote() throws Exception
    {
        // WA-13 (ADR-0018 #9d): the "what to unlock next" hint renders as loadout-unlock-hint in
        // text/secondary - a points-spending lever, not task-critical advice.
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setUnlockHint("Unlock next: Bigger and Badder (50 pts; you have 60).");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea note = componentByName(panel, "loadout-unlock-hint", JTextArea.class);
        assertTrue(note.getText().contains("Bigger and Badder"));
        assertEquals("the hint reads as a secondary lever, like the bank nudges",
            SlayerTheme.TEXT_SECONDARY, note.getForeground());
    }

    @Test
    public void noUnlockHintWhenTheRecommendationHasNone() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation(); // unlockHint == null
        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("no unlock hint when none is set", "loadout-unlock-hint",
                component.getName());
        }
    }

    @Test
    public void estDpsIsScopedSingleTargetExclCannon() throws Exception
    {
        // WB-7 (PD-B / D2-cheap): the DPS estimate models one target and no cannon - say so under
        // the number instead of letting it read as trip-wide throughput. A separate secondary line
        // (not appended to the KeyValueRow value) so the copy can never ellipsize away at 225px.
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(variantState(locatedTask(), null, null)));

        JTextArea scope = componentByName(panel, "loadout-dps-scope", JTextArea.class);
        assertEquals("single-target, excl. cannon", scope.getText());
        assertEquals("the scope reads as a secondary qualifier, not advice",
            SlayerTheme.TEXT_SECONDARY, scope.getForeground());
        assertTrue("the scope sits in the loadout card with the Est. DPS row",
            isInside(scope, componentByName(panel, "loadout-section", Container.class)));
    }

    // ---- T21: loadout section --------------------------------------------------------------------

    @Test
    public void loadoutSectionRendersWornGridRowsInventoryAndDps() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151); // abyssal whip
        worn.put(EquipmentSlot.HEAD, 11864);  // slayer helmet

        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(Arrays.asList(2434, 385)); // prayer pot, shark
        rec.setEstimatedDps(12.4);
        rec.setTotalGearCost(28_400_000L);

        Map<Integer, String> names = new HashMap<>();
        names.put(4151, "Abyssal whip");
        names.put(11864, "Slayer helmet");
        names.put(2434, "Prayer potion");
        names.put(385, "Shark");
        Map<Integer, Integer> prices = new HashMap<>();
        prices.put(4151, 2_000_000);

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", names, prices, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false)));

        // EquipmentGrid asked the seam to draw the worn ids (at-a-glance overview).
        assertTrue("equipment grid drew the worn weapon", renderer.drew(4151));
        assertTrue("equipment grid drew the worn helm", renderer.drew(11864));

        // Worn detail rows (primary, Decision 3).
        List<LoadoutItemRow> rows = allOfType(panel, LoadoutItemRow.class);
        assertTrue("a worn detail row for the whip exists",
            rows.stream().anyMatch(r -> "Abyssal whip".equals(r.getNameLabel().getText())
                && r.getNameLabel().getForeground().equals(SlayerTheme.TEXT_PRIMARY)));

        List<String> texts = labelTexts(panel);
        assertTrue("Est. DPS row", texts.stream().anyMatch(t -> t.contains("12.4")));
        assertTrue("bank age row", texts.contains("12m ago"));
        assertNotNull("inventory grid present",
            componentByName(panel, "inventory-grid", Container.class));
        assertTrue("export enabled with a recommendation", button(panel, "action-export").isEnabled());
    }

    @Test
    public void loadoutSectionShowsEmptyStateWhileTaskIntelStillRenders() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = locatedTask();

        // FR-6: a task with no buildable loadout still renders task + where; loadout shows empty.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, null, 73, AdviceMode.DPS, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 75)));

        assertNotNull("task section still renders the required-or-level intel",
            componentByName(panel, "task-section", Container.class));
        assertNotNull("where section still renders",
            componentByName(panel, "where-recommended", JLabel.class));
        assertFalse("export disabled with no recommendation (FR-9)",
            button(panel, "action-export").isEnabled());
        List<String> texts = labelTexts(panel);
        assertTrue("the loadout shows a player-facing empty state",
            texts.stream().anyMatch(t -> t.toLowerCase().contains("loadout")));
    }

    // ---- LD13: consumables, bank-gate prompt, no upgrades ----------------------------------------

    @Test
    public void bankNotScannedShowsGatePromptAndNoGearRowsButKeepsTaskIntel() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = locatedTask(); // has locations but no required item

        runOnEdt(() -> panel.render(SlayerPanelState.bankNotScanned(
            task, 73, AdviceMode.DPS, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Slayer Tower", 75, false, null)));

        // FR-1: task + where/how still render under the bank gate.
        assertNotNull("task section still renders under the bank gate",
            componentByName(panel, "task-section", Container.class));
        assertNotNull("where section still renders under the bank gate",
            componentByName(panel, "where-recommended", JLabel.class));

        // The loadout card shows the "open your bank" prompt and NO gear rows.
        Container loadout = componentByName(panel, "loadout-section", Container.class);
        assertTrue("the loadout card shows the bank-gate prompt",
            allText(loadout).toLowerCase().contains("open your bank"));
        assertTrue("no gear rows are rendered while the bank is unscanned",
            allOfType(loadout, LoadoutItemRow.class).isEmpty());
        assertFalse("export is disabled with no recommendation (FR-9)",
            button(panel, "action-export").isEnabled());
    }

    @Test
    public void loadoutCardRendersFoodAndPotionRows() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());
        rec.setEstimatedDps(10.0);
        rec.setTotalGearCost(1_000_000L);
        // shark food, cooked karambwan combo, super combat potion (FR-4/FR-5).
        rec.setConsumables(new Consumables(385, 3144, 12695, null));

        Map<Integer, String> names = new HashMap<>();
        names.put(4151, "Abyssal whip");
        names.put(385, "Shark");
        names.put(3144, "Cooked karambwan");
        names.put(12695, "Super combat potion(4)");

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", names, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false)));

        List<LoadoutItemRow> rows = allOfType(panel, LoadoutItemRow.class);
        assertTrue("a food row for the best owned food (FR-4)",
            rows.stream().anyMatch(r -> "Shark".equals(r.getNameLabel().getText())));
        assertTrue("a combo-food row when karambwan is owned (FR-4)",
            rows.stream().anyMatch(r -> "Cooked karambwan".equals(r.getNameLabel().getText())));
        assertTrue("a potion row for the best owned style-matching potion (FR-5)",
            rows.stream().anyMatch(r -> "Super combat potion(4)".equals(r.getNameLabel().getText())));
        // The sprite was drawn through the seam.
        assertTrue("the food sprite was drawn through the seam", renderer.drew(385));
        assertTrue("the potion sprite was drawn through the seam", renderer.drew(12695));
    }

    @Test
    public void magicTaskRendersSpellAndRuneRowsWithShortRuneBlocked() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");
        task.setSlayerLevel(60);
        task.setWeakness(new Weakness(CombatStyle.MAGIC, "fire"));

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 1387); // staff of fire
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MAGIC);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());
        rec.setEstimatedDps(8.0);
        rec.setTotalGearCost(50_000L);

        Map<Integer, Integer> runeReq = new LinkedHashMap<>();
        runeReq.put(556, 7);   // air rune (owned)
        runeReq.put(554, 10);  // fire rune (short)
        Map<Integer, Integer> runesShort = new LinkedHashMap<>();
        runesShort.put(554, 6); // short 6 fire runes
        MagicSetup magic = new MagicSetup("fire", "Fire Surge", 24, false, runeReq, runesShort);
        rec.setConsumables(new Consumables(null, null, null, magic));

        Map<Integer, String> names = new HashMap<>();
        names.put(1387, "Staff of fire");
        names.put(556, "Air rune");
        names.put(554, "Fire rune");

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", names, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false)));

        assertTrue("the chosen spell name is shown (FR-6)",
            allText(panel).contains("Fire Surge"));

        List<LoadoutItemRow> rows = allOfType(panel, LoadoutItemRow.class);
        LoadoutItemRow air = rows.stream()
            .filter(r -> "Air rune".equals(r.getNameLabel().getText())).findFirst().orElse(null);
        LoadoutItemRow fire = rows.stream()
            .filter(r -> "Fire rune".equals(r.getNameLabel().getText())).findFirst().orElse(null);
        assertNotNull("an owned rune renders a rune row", air);
        assertNotNull("a short rune renders a rune row", fire);
        assertEquals("an owned rune is plain primary text",
            SlayerTheme.TEXT_PRIMARY, air.getNameLabel().getForeground());
        assertEquals("a rune the player is short of is BLOCKED/red (FR-6)",
            SlayerTheme.STATE_BLOCKED, fire.getNameLabel().getForeground());
    }

    @Test
    public void noUpgradesSectionInAnyState() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(richState(false))); // a full TASK_WITH_LOADOUT state

        // FR-7: no upgrades concept survives anywhere - no caption, no muted-upgrade row.
        assertFalse("no 'upgrades' caption in the loadout card",
            allText(panel).toLowerCase().contains("upgrade"));
        for (LoadoutItemRow row : allOfType(panel, LoadoutItemRow.class))
        {
            assertNotEquals("no upgrade-styled (muted) row remains anywhere (FR-7): "
                    + row.getNameLabel().getText(),
                SlayerTheme.TEXT_MUTED, row.getNameLabel().getForeground());
        }
        // and the upgrades helper is gone from the class entirely (guard).
        for (java.lang.reflect.Method method : SlayerPanel.class.getDeclaredMethods())
        {
            assertNotEquals("the upgrades helper must be deleted (FR-7)",
                "upgradesToShow", method.getName());
        }
    }

    @Test
    public void valueEqualRecommendationWithConsumablesCausesNoLoadoutRebuild() throws Exception
    {
        SlayerPanel panel = panel();

        runOnEdt(() -> panel.render(consumablesState()));
        int afterFirst = panel.sectionRebuildCount();
        assertTrue("first render builds the sections", afterFirst > 0);

        // A DISTINCT but value-equal recommendation (incl. consumables) must not rebuild (NFR-4).
        SlayerPanelState second = consumablesState();
        assertNotSame("a fresh state instance", consumablesState(), second);
        runOnEdt(() -> panel.render(second));

        assertEquals("a value-equal recommendation incl. consumables triggers zero rebuilds (NFR-4)",
            afterFirst, panel.sectionRebuildCount());
    }

    // ---- T22: non-destructive re-render ----------------------------------------------------------

    @Test
    public void valueEqualSliceCausesNoSectionRebuildAndKeepsScroll() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task1 = locatedTask();
        Recommendation rec1 = locatedRecommendation();
        SlayerPanelState first = SlayerPanelState.forTask(
            task1, rec1, 84, AdviceMode.DPS, "12m ago", null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 75);

        runOnEdt(() -> panel.render(first));
        int rebuildsAfterFirst = panel.sectionRebuildCount();
        assertTrue("first render builds the sections", rebuildsAfterFirst > 0);

        // Production builds a NEW Recommendation/TaskData on every event (AllInSlayerPlugin), so the
        // zero-rebuild NFR must rest on value-equality, not object identity. Re-render a DISTINCT but
        // value-equal slice and prove the self-diff still does nothing.
        TaskData task2 = locatedTask();
        Recommendation rec2 = locatedRecommendation();
        assertNotSame("the second render uses a fresh recommendation instance", rec1, rec2);
        assertEquals("...that is value-equal to the first (the contract the self-diff depends on)",
            rec1, rec2);
        SlayerPanelState second = SlayerPanelState.forTask(
            task2, rec2, 84, AdviceMode.DPS, "12m ago", null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 75);

        // Pin a scroll position, then re-render the value-equal slice.
        runOnEdt(() -> scrollBar(panel).setValues(40, 10, 0, 200));
        runOnEdt(() -> panel.render(second));

        assertEquals("a value-equal (but distinct) slice still triggers zero section rebuilds (NFR)",
            rebuildsAfterFirst, panel.sectionRebuildCount());
        assertEquals("the scroll position is preserved across the value-equal re-render (NFR)",
            40, scrollBar(panel).getValue());
    }

    @Test
    public void remainingTickUpdatesHeaderOnlyAndKeepsTheCombo() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = locatedTask();
        Recommendation rec = locatedRecommendation();

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 75)));
        int rebuilds = panel.sectionRebuildCount();
        JComboBox<?> combo = locationCombo(panel);

        // A kill ticks the remaining count - nothing the sections show changed.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 83, AdviceMode.DPS, "12m ago", null, RefreshSource.VARBIT,
            Instant.parse("2026-06-28T12:35:10Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 75)));

        assertEquals("a remaining tick rebuilds no section", rebuilds, panel.sectionRebuildCount());
        assertEquals("only the header reflects the new count", "83 remaining",
            header(panel, "task-header-remaining").getText());
        assertSame("the combo survives the re-render", combo, locationCombo(panel));
    }

    // ---- T23: developer-mode Diagnostics (ADR-0003, P0-5) ----------------------------------------

    @Test
    public void diagnosticsHiddenForDefaultPlayers() throws Exception
    {
        SlayerPanel panel = panel();

        runOnEdt(() -> panel.render(SlayerPanelState.unsupportedTask(
            1234, 50, AdviceMode.DPS, null, RefreshSource.MENU_CHECK,
            Instant.parse("2026-06-28T12:34:56Z"),
            SlayerDebugSnapshot.empty().withSlayerState(11, 22, 33, 44, "DURADEL", 1234),
            false)));

        Container diagnostics = componentByName(panel, "diagnostics-section", Container.class);
        assertFalse("default players never see the diagnostics surface (P0-5)",
            diagnostics.isVisible());

        // Nothing developer-grade leaks into the visible tree.
        List<String> texts = labelTexts(panel);
        assertFalse("no raw detector telemetry", texts.contains("DURADEL"));
        assertFalse("no raw status enum", texts.contains("UNSUPPORTED_TASK"));
        assertFalse("no raw refresh-source enum", texts.contains("MENU_CHECK"));
    }

    @Test
    public void diagnosticsSurfacesEveryDebugFieldInDeveloperMode() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(new EnumMap<>(EquipmentSlot.class));
        rec.setInventory(new ArrayList<>());

        SlayerDebugSnapshot debug = SlayerDebugSnapshot.empty()
            .withMenu("Check", MenuAction.CC_OP, 4155, 4156, true)
            .withSlayerState(1101, 2202, 3303, 4404, "DURADEL", 1234);

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", null, null, RefreshSource.MENU_CHECK,
            Instant.parse("2026-06-28T12:34:56Z"), debug, null, 90, true)));

        Container diagnostics = componentByName(panel, "diagnostics-section", Container.class);
        assertTrue("diagnostics is shown when developer mode is on", diagnostics.isVisible());

        // Every preserved field (FR-2/3/4) is surfaced verbatim.
        assertEquals("TASK_WITH_LOADOUT", diagValue(panel, "diag-status"));
        assertEquals("MENU_CHECK", diagValue(panel, "diag-source"));
        assertEquals("Check", diagValue(panel, "diag-menu-option"));
        assertEquals("CC_OP", diagValue(panel, "diag-menu-action"));
        assertEquals("4155", diagValue(panel, "diag-raw-item"));
        assertEquals("4156", diagValue(panel, "diag-mapped-item"));
        assertEquals("true", diagValue(panel, "diag-matched-check"));
        assertEquals("1101", diagValue(panel, "diag-target-varp"));
        assertEquals("2202", diagValue(panel, "diag-count-varp"));
        assertEquals("3303", diagValue(panel, "diag-area-varp"));
        assertEquals("4404", diagValue(panel, "diag-boss-varbit"));
        assertEquals("DURADEL", diagValue(panel, "diag-detector"));
        assertEquals("1234", diagValue(panel, "diag-unsupported"));
    }

    @Test
    public void diagnosticsClearsWhenDeveloperModeIsTurnedOff() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(new EnumMap<>(EquipmentSlot.class));
        rec.setInventory(new ArrayList<>());
        SlayerDebugSnapshot debug = SlayerDebugSnapshot.empty()
            .withSlayerState(1, 2, 3, 4, "DURADEL", -1);

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), debug, null, 90, true)));
        assertTrue("shown while developer mode is on",
            componentByName(panel, "diagnostics-section", Container.class).isVisible());

        // Toggle developer mode off: the surface hides and its telemetry is cleared out of the tree.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), debug, null, 90, false)));

        Container diagnostics = componentByName(panel, "diagnostics-section", Container.class);
        assertFalse("hidden again when developer mode is off", diagnostics.isVisible());
        assertFalse("its telemetry is cleared from the tree", labelTexts(panel).contains("DURADEL"));
    }

    @Test
    public void diagnosticsSelfDiffsOnTheDebugSliceAndCountsTowardRebuilds() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(new EnumMap<>(EquipmentSlot.class));
        rec.setInventory(new ArrayList<>());

        SlayerDebugSnapshot debugA = SlayerDebugSnapshot.empty().withSlayerState(1, 2, 3, 4, "A", -1);
        SlayerPanelState withA = SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), debugA, null, 90, true);

        runOnEdt(() -> panel.render(withA));
        int afterFirst = panel.sectionRebuildCount();

        // The same debug slice triggers no rebuild anywhere.
        runOnEdt(() -> panel.render(withA));
        assertEquals("an unchanged debug slice rebuilds nothing", afterFirst,
            panel.sectionRebuildCount());

        // A changed debug slice rebuilds exactly the diagnostics section (it counts in the probe).
        SlayerDebugSnapshot debugB = SlayerDebugSnapshot.empty().withSlayerState(1, 2, 3, 4, "B", -1);
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), debugB, null, 90, true)));
        assertEquals("a changed debug slice rebuilds only diagnostics", afterFirst + 1,
            panel.sectionRebuildCount());
    }

    // ---- T24: player-language empty/error copy (P1-9, UX §4) -------------------------------------

    @Test
    public void noTaskShowsPlayerLanguageCopy() throws Exception
    {
        SlayerPanel panel = panel();

        runOnEdt(() -> panel.render(SlayerPanelState.noTask(
            0, AdviceMode.DPS, null, null, RefreshSource.STARTUP,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty())));

        String text = allText(panel);
        assertTrue("no-task title", text.contains("No Slayer task"));
        assertTrue("no-task body names the levers (Slayer master / gem / helm)",
            text.contains("Slayer master") && text.contains("Slayer helm"));
        assertNoDevLanguage(panel);
    }

    @Test
    public void unsupportedTaskShowsPlayerLanguageCopyDistinctFromNoTask() throws Exception
    {
        SlayerPanel panel = panel();

        runOnEdt(() -> panel.render(SlayerPanelState.unsupportedTask(
            9999, 40, AdviceMode.DPS, null, RefreshSource.VARBIT,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty())));

        String text = allText(panel);
        assertTrue("unsupported reads as a data gap, not a bug", text.contains("not in our data yet"));
        assertFalse("never the raw 'Unsupported Slayer target' framing",
            text.contains("Unsupported Slayer target"));
        assertNoDevLanguage(panel);
    }

    @Test
    public void noLoadoutShowsPlayerLanguageEmptyStateAndTaskIntelStaysVisible() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = locatedTask();

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, null, 73, AdviceMode.DPS, "9m ago", null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 75)));

        // Task intel still renders (FR-6) while the loadout shows a player-facing empty state.
        assertNotNull("task section still renders",
            componentByName(panel, "task-section", Container.class));
        String text = allText(panel);
        assertTrue("no-loadout title", text.contains("No owned loadout found"));
        assertTrue("no-loadout body names the levers (scan bank / Cost mode)",
            text.contains("Scan your bank") && text.contains("Cost mode"));
        assertNoDevLanguage(panel);
    }

    @Test
    public void withLoadoutCardRendersNoDeadNoBankNuance() throws Exception
    {
        // WB-6 (D12): a recommendation exists only past the ADR-0003 bank gate (bankLastSeen
        // non-null -> bankAge non-null), so the old bankAge==null else-branch on the with-loadout
        // card was unreachable in production - dead UX. Never-scanned is the BANK_NOT_SCANNED
        // gate's prompt, not a nuance note under real gear. Even fed the impossible state
        // directly, the card renders no no-bank note.
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());
        rec.setEstimatedDps(10.0);
        rec.setTotalGearCost(1_000_000L);

        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component component : allComponents(loadout))
        {
            assertNotEquals("the dead no-bank nuance is gone from the with-loadout card",
                "loadout-no-bank-note", component.getName());
        }
    }

    // ---- DT-FE4: bank login-staleness note (G4 / PD-5) -------------------------------------------

    @Test
    public void loadoutCardShowsReopenNoteWhenTheBankSnapshotIsStale() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());
        rec.setEstimatedDps(10.0);
        rec.setTotalGearCost(1_000_000L);
        Map<Integer, String> names = new HashMap<>();
        names.put(4151, "Abyssal whip");

        // A stale snapshot ("8d ago", bankStale=true) shows the reopen nudge under the bank-seen row.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "8d ago", names, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false, null,
            null, null, /*bankStale*/ true)));

        JTextArea note = componentByName(panel, "loadout-bank-stale-note", JTextArea.class);
        assertTrue("the stale note carries the snapshot age", note.getText().contains("8d ago"));
        assertTrue("the stale note nudges the player to reopen the bank",
            note.getText().toLowerCase().contains("reopen"));
        assertEquals("the stale note is a non-blocking notice, not the blocked red",
            SlayerTheme.TEXT_SECONDARY, note.getForeground());
        assertNoDevLanguage(panel);
    }

    @Test
    public void loadoutCardHasNoStaleNoteWhenTheBankIsFresh() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());
        rec.setEstimatedDps(10.0);
        rec.setTotalGearCost(1_000_000L);
        Map<Integer, String> names = new HashMap<>();
        names.put(4151, "Abyssal whip");

        // A fresh snapshot ("12m ago", bankStale=false) shows the normal bank-seen row and no nudge.
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "12m ago", names, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false, null,
            null, null, /*bankStale*/ false)));

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        for (Component c : allComponents(loadout))
        {
            assertNotEquals("no stale note when the bank is fresh", "loadout-bank-stale-note",
                c.getName());
        }
        assertTrue("the normal bank-seen row still shows the age",
            labelTexts(panel).contains("12m ago"));
    }

    @Test
    public void longPlayerNotesWrapInsteadOfClipping() throws Exception
    {
        SlayerPanel panel = panel();
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());
        rec.setEstimatedDps(10.0);
        rec.setTotalGearCost(1_000_000L);

        // The long stale-bank nudge is the wrap vehicle (S1 / FR-10 wrap, not clip) - it is the
        // long player note the with-loadout card actually renders (WB-6 removed the dead no-bank
        // nuance this test used to drive).
        runOnEdt(() -> panel.render(SlayerPanelState.forTask(
            task, rec, 84, AdviceMode.DPS, "8d ago", null, null, RefreshSource.MANUAL,
            Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(), null, 90, false, null,
            null, null, /*bankStale*/ true)));

        JTextArea note = componentByName(panel, "loadout-bank-stale-note", JTextArea.class);
        assertTrue("the long note line-wraps (no <html>, ADR-0004)", note.getLineWrap());
        assertTrue("wraps on word boundaries", note.getWrapStyleWord());
        assertFalse("a wrapping note is never editable", note.isEditable());

        // Constrained to the panel width, the long copy wraps to more than one line (it would clip on a
        // plain JLabel). Preferred height exceeds a single caption line; width stays within the panel.
        runOnEdt(() -> note.setSize(SlayerTheme.PANEL_WIDTH, Short.MAX_VALUE));
        int lineHeight = note.getFontMetrics(note.getFont()).getHeight();
        assertTrue("the note wraps to multiple lines within the panel width",
            note.getPreferredSize().height > lineHeight);
        assertTrue("the note never forces horizontal overflow",
            note.getPreferredSize().width <= SlayerTheme.PANEL_WIDTH);
    }

    // ---- T25: colour discipline + legacy removed (P1-10, P2-11, ADR-0004) ------------------------

    @Test
    public void noHtmlInAnyPanelOwnedLabel() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(richState(true))); // full dashboard + diagnostics

        // RuneLite's PluginErrorPanel wraps its own description in <html> internally; that library
        // component is out of our control. The rule (ADR-0004) is no <html> in the labels WE render.
        PluginErrorPanel errorPanel = firstOfType(panel, PluginErrorPanel.class);
        for (Component component : allComponents(panel))
        {
            if (component instanceof JLabel && !isInside(component, errorPanel))
            {
                String text = ((JLabel) component).getText();
                assertFalse("no <html> markup hack in our own label: " + text,
                    text != null && text.toLowerCase().contains("<html"));
            }
        }
    }

    @Test
    public void legacyHelpersAndFixedWidthPanelAreGone()
    {
        List<String> banned = Arrays.asList(
            "row", "section", "keyValueRow", "chips", "addChip", "wrappedValueLabel",
            "escapeHtml", "fixedWidth", "sectionHeading", "upgradesToShow");
        for (java.lang.reflect.Method method : SlayerPanel.class.getDeclaredMethods())
        {
            assertFalse("legacy HTML/width helper still present: " + method.getName(),
                banned.contains(method.getName()));
        }
        for (Class<?> nested : SlayerPanel.class.getDeclaredClasses())
        {
            assertNotEquals("FixedWidthPanel must be deleted (ADR-0004)",
                "FixedWidthPanel", nested.getSimpleName());
        }
    }

    @Test
    public void sectionHeadersAreNeverBrand() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(richState(true)));

        int headers = 0;
        for (Component component : allComponents(panel))
        {
            if (component instanceof JLabel && "section-header".equals(component.getName()))
            {
                headers++;
                assertEquals("section headers stay primary text",
                    SlayerTheme.TEXT_PRIMARY, component.getForeground());
                assertNotEquals("section headers never spend the brand accent",
                    SlayerTheme.ACCENT_BRAND, component.getForeground());
            }
        }
        assertTrue("there is at least one section header", headers > 0);
    }

    @Test
    public void brandOrangeAppearsOnlyOnRealState() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(richState(false)));

        // Brand is reserved for real state: the recommended location, the active mode segment, and the
        // active attack-style segment (MV-FE3 - the same legitimate brand use as the mode segment).
        List<String> allowed = Arrays.asList("where-recommended", "mode-dps", "mode-cost",
            "method-melee", "method-ranged", "method-magic");
        for (Component component : allComponents(panel))
        {
            boolean textBearing = component instanceof JLabel || component instanceof AbstractButton;
            if (textBearing && SlayerTheme.ACCENT_BRAND.equals(component.getForeground()))
            {
                assertTrue("brand orange leaked onto: " + component.getName(),
                    allowed.contains(component.getName()));
            }
        }
    }

    // ---- T26: collapsible sections (P2-13) -------------------------------------------------------

    @Test
    public void clickingASectionHeaderTogglesItsBody() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(richState(false)));

        Container taskCard = componentByName(panel, "task-section", Container.class);
        Component content = componentByName(taskCard, "section-content", JComponent.class);
        Component bar = componentByName(taskCard, "section-header-bar", JComponent.class);

        assertTrue("sections start expanded", content.isVisible());
        runOnEdt(() -> clickHeader(bar));
        assertFalse("clicking the header collapses the body", content.isVisible());
        runOnEdt(() -> clickHeader(bar));
        assertTrue("clicking the header again expands the body", content.isVisible());
    }

    @Test
    public void collapsedStateSurvivesAReactiveReRender() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(richState(false)));

        Container loadoutCard = componentByName(panel, "loadout-section", Container.class);
        Component content = componentByName(loadoutCard, "section-content", JComponent.class);
        Component bar = componentByName(loadoutCard, "section-header-bar", JComponent.class);

        runOnEdt(() -> clickHeader(bar));
        assertFalse("collapsed after the click", content.isVisible());

        // A reactive re-render (even one that rebuilds the body) must not reset the collapse, just
        // like scroll / combo / mode in T22.
        runOnEdt(() -> panel.render(richState(false)));
        assertFalse("collapsed state persists across the re-render", content.isVisible());
    }

    // ---- W8 F2/F5/F6: live-render layout invariants ----------------------------------------------

    @Test
    public void whyAndMethodWrapAsTextAreasInsteadOfClipping() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(longProseState()));

        JTextArea why = componentByName(panel, "where-why", JTextArea.class);
        JTextArea method = componentByName(panel, "where-method", JTextArea.class);

        assertTrue("Why wraps as a JTextArea, no <html> (F2, ADR-0004)", why.getLineWrap());
        assertTrue("Why wraps on word boundaries", why.getWrapStyleWord());
        assertFalse("a wrapping note is never editable", why.isEditable());
        assertTrue("Method wraps as a JTextArea (F2)", method.getLineWrap());

        // Constrained to the panel width the prose wraps; it never forces horizontal overflow.
        runOnEdt(() ->
        {
            why.setSize(SlayerTheme.PANEL_WIDTH, Short.MAX_VALUE);
            method.setSize(SlayerTheme.PANEL_WIDTH, Short.MAX_VALUE);
        });
        assertTrue("Why stays within the panel width (F2/F6)",
            why.getPreferredSize().width <= SlayerTheme.PANEL_WIDTH);
        assertTrue("Method stays within the panel width (F2/F6)",
            method.getPreferredSize().width <= SlayerTheme.PANEL_WIDTH);

        // The prose is no longer a single-line KeyValueRow that clips.
        for (KeyValueRow row : allOfType(panel, KeyValueRow.class))
        {
            assertNotEquals("Why is no longer a clipping KeyValueRow (F2)", "Why",
                row.getKeyLabel().getText());
            assertNotEquals("Method is no longer a clipping KeyValueRow (F2)", "Method",
                row.getKeyLabel().getText());
        }
    }

    @Test
    public void scrollViewTracksTheViewportWidthSoNothingClipsSilently() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(longProseState()));

        Component view = allOfType(panel, JScrollPane.class).get(0).getViewport().getView();
        assertTrue("the scroll view is Scrollable (F5)", view instanceof javax.swing.Scrollable);
        javax.swing.Scrollable scrollable = (javax.swing.Scrollable) view;
        assertTrue("it tracks the viewport width so content cannot exceed 225 (F5)",
            scrollable.getScrollableTracksViewportWidth());
        assertFalse("but not the height, so vertical scroll is preserved (F5)",
            scrollable.getScrollableTracksViewportHeight());
    }

    @Test
    public void noDashboardDescendantExceedsThePanelWidthWithLongProse() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() ->
        {
            panel.render(longProseState());
            panel.setSize(SlayerTheme.PANEL_WIDTH, 4000);
            layoutTree(panel);
        });

        Container body = componentByName(panel, "dashboard-body", Container.class);
        for (Component component : allComponents(body))
        {
            if (component == body)
            {
                continue;
            }
            assertTrue("no dashboard descendant overflows the panel width (F6): "
                    + component.getName() + " w=" + component.getWidth(),
                component.getWidth() <= SlayerTheme.PANEL_WIDTH);
        }
    }

    // ---- W10 G1/G4: scrollbar hidden but the wheel still scrolls ---------------------------------

    @Test
    public void verticalScrollbarIsHiddenButTheWheelStillScrolls() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() -> panel.render(richState(false)));

        JScrollPane scroll = allOfType(panel, JScrollPane.class).get(0);
        // Policy stays AS_NEEDED so the bar is "visible" to the wheel handler (NEVER kills the wheel).
        assertEquals("vertical policy stays AS_NEEDED so the wheel keeps working (G1)",
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, scroll.getVerticalScrollBarPolicy());
        assertTrue("mouse-wheel scrolling stays on (G1)", scroll.isWheelScrollingEnabled());

        // ...but the bar reserves zero width and is a no-paint UI, so it is effectively invisible.
        assertEquals("the vertical scrollbar reserves zero width - no visible bar, no gutter (G1)",
            0, scroll.getVerticalScrollBar().getPreferredSize().width);
        assertTrue("a no-paint scrollbar UI is installed (G1)",
            scroll.getVerticalScrollBar().getUI() instanceof InvisibleScrollBarUI);
        // The native wheel step is preserved.
        assertEquals("the unit increment stays native (G1)",
            16, scroll.getVerticalScrollBar().getUnitIncrement());
    }

    // ---- W10 G2/G4: grids left-align with the rows (off-centre fix) ------------------------------

    @Test
    public void loadoutGridsLeftAlignWithTheWornRowsAfterLayout() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() ->
        {
            panel.render(richState(false)); // worn gear + inventory => both grids render
            panel.setSize(SlayerTheme.PANEL_WIDTH, 4000);
            layoutTree(panel);
        });

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        // The grids now sit one wrapper level deeper (a full-width leftRow), but componentByName walks
        // the whole subtree so the lookup is unchanged.
        Container equipmentGrid = componentByName(panel, "equipment-grid", Container.class);
        Container inventoryGrid = componentByName(panel, "inventory-grid", Container.class);

        // A worn detail row is the flush-left reference edge (it fills the card column width).
        LoadoutItemRow wornRow = allOfType(panel, LoadoutItemRow.class).stream()
            .filter(r -> "Abyssal whip".equals(r.getNameLabel().getText()))
            .findFirst().orElseThrow(() -> new AssertionError("no worn whip row"));

        int rowLeft = leftEdgeIn(wornRow, loadout);
        assertEquals("the equipment grid sits flush-left like the worn rows (G2)",
            rowLeft, leftEdgeIn(equipmentGrid, loadout));
        assertEquals("the inventory grid sits flush-left like the worn rows (G2)",
            rowLeft, leftEdgeIn(inventoryGrid, loadout));
    }

    @Test
    public void noLoadoutSectionRowFloatsOffTheLeftEdge() throws Exception
    {
        SlayerPanel panel = panel();
        runOnEdt(() ->
        {
            panel.render(richState(false));
            panel.setSize(SlayerTheme.PANEL_WIDTH, 4000);
            layoutTree(panel);
        });

        Container loadout = componentByName(panel, "loadout-section", Container.class);
        // Every content row in the loadout card (grids included) shares one left origin - nothing
        // floats indented (G2/G4-iii). The grids are the reference edge that used to drift. Scope the
        // sweep to the loadout subtree so Task-card rows (a different ancestor) are excluded.
        int gridLeft = leftEdgeIn(componentByName(loadout, "equipment-grid", Container.class), loadout);
        for (LoadoutItemRow row : allOfType(loadout, LoadoutItemRow.class))
        {
            assertEquals("a loadout row floats off the shared left edge: " + row.getNameLabel().getText(),
                gridLeft, leftEdgeIn(row, loadout));
        }
        for (KeyValueRow row : allOfType(loadout, KeyValueRow.class))
        {
            assertEquals("a key/value row floats off the shared left edge: " + row.getKeyLabel().getText(),
                gridLeft, leftEdgeIn(row, loadout));
        }
    }

    // ---- fixtures & helpers ----------------------------------------------------------------------

    /** The left edge of {@code c} measured in {@code ancestor}'s coordinate space (headless-safe). */
    private static int leftEdgeIn(Component c, Container ancestor)
    {
        int x = 0;
        Component current = c;
        while (current != null && current != ancestor)
        {
            x += current.getX();
            current = current.getParent();
        }
        return x;
    }

    /** The top edge of {@code c} measured in {@code ancestor}'s coordinate space (headless-safe). */
    private static int topEdgeIn(Component c, Container ancestor)
    {
        int y = 0;
        Component current = c;
        while (current != null && current != ancestor)
        {
            y += current.getY();
            current = current.getParent();
        }
        return y;
    }

    /** Recursive top-down {@code doLayout} so headless layout propagates parent sizes to children. */
    private static void layoutTree(Component component)
    {
        component.doLayout();
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                layoutTree(child);
            }
        }
    }

    /** A TASK_WITH_LOADOUT state whose Why/Method prose is long enough to clip on a single-line label. */
    private static SlayerPanelState longProseState()
    {
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        task.setLocations(Arrays.asList(
            new SlayerLocation("Catacombs of Kourend", true, false, true, true)));

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        worn.put(EquipmentSlot.HEAD, 11864);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(Arrays.asList(2434, 385, 565));
        rec.setEstimatedDps(12.4);
        rec.setTotalGearCost(28_400_000L);
        rec.setRecommendedLocation(new SlayerLocation("Catacombs of Kourend", true, false, true, true));
        rec.setLocationReason("Gear MELEE, cannon allowed, burst stacks, Konar-lockable - by far the "
            + "best experience-per-hour spot for this task and also one of the safest available.");
        rec.setMethod("Set up your cannon in the multi-combat area and fight with melee, restocking "
            + "cannonballs and prayer potions from your inventory between trips as the count drops.");

        Map<Integer, String> names = new HashMap<>();
        names.put(4151, "Abyssal whip");
        names.put(11864, "Slayer helmet");
        names.put(2434, "Prayer potion");
        names.put(385, "Shark");
        names.put(565, "Blood rune");
        names.put(22324, "Ghrazi rapier");

        return SlayerPanelState.forTask(task, rec, 84, AdviceMode.DPS, "12m ago", names,
            RefreshSource.MANUAL, Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            "Catacombs of Kourend", 90);
    }

    private static void clickHeader(Component bar)
    {
        MouseEvent event = new MouseEvent(bar, MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(), 0, 1, 1, 1, false);
        for (MouseListener listener : bar.getMouseListeners())
        {
            listener.mouseClicked(event);
        }
    }

    private static String diagValue(Container c, String rowName)
    {
        return componentByName(c, rowName, KeyValueRow.class).getValueLabel().getText();
    }

    /** All rendered label text joined - the player-facing surface a test inspects for copy / leaks. */
    private static String allText(Container c)
    {
        return String.join(" | ", labelTexts(c));
    }

    /** Asserts no developer-language leaks into the player-facing surface (audit Finding 5). */
    private static void assertNoDevLanguage(Container c)
    {
        String text = allText(c);
        assertFalse("no 'Source:' dev framing", text.contains("Source:"));
        assertFalse("no 'then refresh' (contradicts the reactive model)", text.contains("then refresh"));
        for (RefreshSource source : RefreshSource.values())
        {
            assertFalse("no raw RefreshSource enum (" + source.name() + ") in player text",
                text.contains(source.name()));
        }
        for (PanelStatus status : PanelStatus.values())
        {
            assertFalse("no raw PanelStatus enum (" + status.name() + ") in player text",
                text.contains(status.name()));
        }
    }

    /** A full TASK_WITH_LOADOUT state (worn + inventory + upgrade + required-owned + location). */
    private static SlayerPanelState richState(boolean developerMode)
    {
        TaskData task = new TaskData();
        task.setTask("Abyssal demons");
        task.setSlayerLevel(85);
        task.setWeakness(new Weakness(CombatStyle.MELEE, null));
        task.setRequiredItemId(11864); // slayer helmet (worn => OWNED, not blocked)
        task.setRequiredItemName("Slayer helmet");
        task.setLocations(Arrays.asList(
            new SlayerLocation("Catacombs of Kourend", true, false, true, true)));

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151);
        worn.put(EquipmentSlot.HEAD, 11864);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(Arrays.asList(2434));
        rec.setEstimatedDps(12.4);
        rec.setTotalGearCost(28_400_000L);
        rec.setRecommendedLocation(new SlayerLocation("Catacombs of Kourend", true, false, true, true));
        rec.setLocationReason("Best XP here");
        rec.setMethod("Cannon + melee");

        Map<Integer, String> names = new HashMap<>();
        names.put(4151, "Abyssal whip");
        names.put(11864, "Slayer helmet");
        names.put(2434, "Prayer potion");
        names.put(22324, "Ghrazi rapier");
        Map<Integer, Integer> prices = new HashMap<>();
        prices.put(4151, 2_000_000);
        prices.put(22324, 55_000_000);

        SlayerDebugSnapshot debug = SlayerDebugSnapshot.empty()
            .withMenu("Check", MenuAction.CC_OP, 11864, 11864, true)
            .withSlayerState(1, 2, 3, 4, "DURADEL", -1);

        return SlayerPanelState.forTask(task, rec, 84, AdviceMode.DPS, "12m ago", names, prices,
            RefreshSource.MENU_CHECK, Instant.parse("2026-06-28T12:34:56Z"), debug,
            "Catacombs of Kourend", 90, developerMode);
    }

    /** A TASK_WITH_LOADOUT state whose recommendation carries consumables (food, potion, magic). */
    private static SlayerPanelState consumablesState()
    {
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");
        task.setSlayerLevel(60);
        task.setWeakness(new Weakness(CombatStyle.MAGIC, "fire"));

        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 1387);
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MAGIC);
        rec.setWorn(worn);
        rec.setInventory(new ArrayList<>());
        rec.setEstimatedDps(8.0);
        rec.setTotalGearCost(50_000L);
        Map<Integer, Integer> runeReq = new LinkedHashMap<>();
        runeReq.put(556, 7);
        runeReq.put(554, 10);
        MagicSetup magic = new MagicSetup("fire", "Fire Surge", 24, false, runeReq,
            new LinkedHashMap<>());
        rec.setConsumables(new Consumables(385, 3144, 12695, magic));

        Map<Integer, String> names = new HashMap<>();
        names.put(1387, "Staff of fire");
        names.put(385, "Shark");
        names.put(3144, "Cooked karambwan");
        names.put(12695, "Super combat potion(4)");
        names.put(556, "Air rune");
        names.put(554, "Fire rune");

        return SlayerPanelState.forTask(task, rec, 84, AdviceMode.DPS, "12m ago", names, null,
            RefreshSource.MANUAL, Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            null, 90, false);
    }

    private static TaskData locatedTask()
    {
        TaskData task = new TaskData();
        task.setTask("Aberrant spectres");
        task.setSlayerLevel(60);
        task.setLocations(Arrays.asList(
            new SlayerLocation("Catacombs of Kourend", true, false, true, true),
            new SlayerLocation("Slayer Tower", false, false, false, true),
            new SlayerLocation("Stronghold Slayer Cave", true, true, false, true)));
        return task;
    }

    private static Recommendation locatedRecommendation()
    {
        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MAGIC);
        rec.setWorn(new EnumMap<>(EquipmentSlot.class));
        rec.setInventory(new ArrayList<>());
        rec.setLocation(new SlayerLocation("Catacombs of Kourend", true, false, true, true));
        rec.setRecommendedLocation(new SlayerLocation("Catacombs of Kourend", true, false, true, true));
        rec.setLocationReason("Best XP here, cannon works");
        rec.setMethod("Burst stacks");
        return rec;
    }

    /** A task with three variants (one boss) for the MV-FE selector tests. */
    private static TaskData variantTask()
    {
        TaskData task = locatedTask();
        task.setWeakness(new Weakness(CombatStyle.MAGIC, "smoke"));
        task.setVariants(Arrays.asList(
            variant("Aberrant spectre", true, CombatStyle.MAGIC, false),
            variant("Deviant spectre", false, CombatStyle.MAGIC, false),
            variant("Repugnant spectre", false, CombatStyle.MELEE, true)));
        return task;
    }

    private static MonsterVariant variant(String name, boolean isDefault, CombatStyle style, boolean boss)
    {
        MonsterVariant v = new MonsterVariant();
        v.setName(name);
        v.setDefault(isDefault);
        v.setBoss(boss);
        v.setWeakness(new Weakness(style, null));
        return v;
    }

    private static SlayerPanelState variantState(TaskData task, String selectedVariant,
        CombatStyle selectedMethod)
    {
        return variantState(task, selectedVariant, selectedMethod, locatedRecommendation());
    }

    private static SlayerPanelState variantState(TaskData task, String selectedVariant,
        CombatStyle selectedMethod, Recommendation rec)
    {
        return SlayerPanelState.forTask(task, rec, 84, AdviceMode.DPS, "12m ago", null, null,
            RefreshSource.MANUAL, Instant.parse("2026-06-28T12:34:56Z"), SlayerDebugSnapshot.empty(),
            null, 90, false, null, selectedVariant, selectedMethod);
    }

    /**
     * A task assigned by two masters, with per-master amounts, an extension range, and the (build-gate
     * guaranteed, WA-7) single EXTENSION-typed unlock - the WA-11 master-context render inputs.
     */
    private static TaskData masterTask()
    {
        TaskData task = locatedTask();
        task.setAssignedBy(Arrays.asList("duradel", "nieve"));
        Map<String, int[]> amounts = new LinkedHashMap<>();
        amounts.put("duradel", new int[]{130, 200});
        amounts.put("nieve", new int[]{120, 185});
        task.setAmountByMaster(amounts);
        Map<String, int[]> extended = new LinkedHashMap<>();
        extended.put("duradel", new int[]{200, 250});
        extended.put("nieve", new int[]{200, 250});
        task.setExtendedAmount(extended);
        TaskUnlock extension = new TaskUnlock();
        extension.setUnlockId("augment-my-abbies");
        extension.setName("Augment my abbies");
        extension.setPointsCost(100);
        extension.setType(UnlockType.EXTENSION);
        task.setUnlocks(Arrays.asList(extension));
        return task;
    }

    private static Map<String, String> masterNames()
    {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("duradel", "Duradel");
        names.put("nieve", "Nieve");
        return names;
    }

    private static SlayerPanelState masterState(TaskData task, String selectedMaster,
        Map<String, String> masterNames)
    {
        return SlayerPanelState.forTask(task, locatedRecommendation(), 84, AdviceMode.DPS, "12m ago",
            null, null, RefreshSource.MANUAL, Instant.parse("2026-06-28T12:34:56Z"),
            SlayerDebugSnapshot.empty(), null, 90, false, null, null, null, false, selectedMaster,
            masterNames);
    }

    private static SlayerPanelState masterStateWithRec(TaskData task, String selectedMaster,
        Map<String, String> masterNames, Recommendation rec)
    {
        return SlayerPanelState.forTask(task, rec, 84, AdviceMode.DPS, "12m ago",
            null, null, RefreshSource.MANUAL, Instant.parse("2026-06-28T12:34:56Z"),
            SlayerDebugSnapshot.empty(), null, 90, false, null, null, null, false, selectedMaster,
            masterNames);
    }

    private static JComboBox<?> variantCombo(Container c)
    {
        return componentByName(c, "variant-combo", JComboBox.class);
    }

    private static AbstractButton methodButton(Container c, String name)
    {
        return componentByName(c, name, AbstractButton.class);
    }

    private SlayerPanel panel() throws Exception
    {
        renderer = new RecordingRenderer();
        AtomicReference<SlayerPanel> ref = new AtomicReference<>();
        runOnEdt(() -> ref.set(new SlayerPanel(renderer)));
        return ref.get();
    }

    private static JLabel header(Container c, String name)
    {
        return componentByName(c, name, JLabel.class);
    }

    /** The value label of the "Slayer lvl" KeyValueRow in the Task card (its tint encodes met/unmet). */
    private static JLabel slayerLevelValue(Container c)
    {
        for (KeyValueRow row : allOfType(c, KeyValueRow.class))
        {
            if ("Slayer lvl".equals(row.getKeyLabel().getText()))
            {
                return row.getValueLabel();
            }
        }
        throw new AssertionError("No 'Slayer lvl' row found");
    }

    private static AbstractButton button(Container c, String name)
    {
        return componentByName(c, name, AbstractButton.class);
    }

    private static JComboBox<?> locationCombo(Container c)
    {
        return componentByName(c, "where-location-combo", JComboBox.class);
    }

    private static Container scrollBody(Container c)
    {
        return allOfType(c, JScrollPane.class).get(0);
    }

    private static javax.swing.JScrollBar scrollBar(Container c)
    {
        return allOfType(c, JScrollPane.class).get(0).getVerticalScrollBar();
    }

    private static List<String> tagTexts(Container c)
    {
        List<String> texts = new ArrayList<>();
        for (Component component : allComponents(c))
        {
            if (component instanceof JLabel && "tag".equals(component.getName()))
            {
                texts.add(((JLabel) component).getText());
            }
        }
        return texts;
    }

    private static boolean hasComboItem(JComboBox<?> comboBox, String item)
    {
        for (int i = 0; i < comboBox.getItemCount(); i++)
        {
            if (item.equals(comboBox.getItemAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isInside(Component child, Container ancestor)
    {
        Component current = child;
        while (current != null)
        {
            if (current == ancestor)
            {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static void runOnEdt(Runnable runnable) throws InvocationTargetException, InterruptedException
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            runnable.run();
        }
        else
        {
            SwingUtilities.invokeAndWait(runnable);
        }
    }

    private static List<String> labelTexts(Container container)
    {
        List<String> texts = new ArrayList<>();
        for (Component component : allComponents(container))
        {
            // JLabels and the wrapping-note JTextAreas are both player-facing rendered text.
            if (component instanceof JLabel)
            {
                String text = ((JLabel) component).getText();
                if (text != null)
                {
                    texts.add(text);
                }
            }
            else if (component instanceof JTextArea)
            {
                String text = ((JTextArea) component).getText();
                if (text != null)
                {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    private static <T extends Component> T firstOfType(Container container, Class<T> type)
    {
        for (Component component : allComponents(container))
        {
            if (type.isInstance(component))
            {
                return type.cast(component);
            }
        }
        return null;
    }

    private static <T extends Component> List<T> allOfType(Container container, Class<T> type)
    {
        List<T> found = new ArrayList<>();
        for (Component component : allComponents(container))
        {
            if (type.isInstance(component))
            {
                found.add(type.cast(component));
            }
        }
        return found;
    }

    private static <T extends Component> T componentByName(Container container, String name, Class<T> type)
    {
        for (Component component : allComponents(container))
        {
            if (type.isInstance(component) && name.equals(component.getName()))
            {
                return type.cast(component);
            }
        }
        throw new AssertionError("Missing component named: " + name);
    }

    private static List<Component> allComponents(Container container)
    {
        List<Component> components = new ArrayList<>();
        collectComponents(container, components);
        return components;
    }

    private static void collectComponents(Component component, List<Component> components)
    {
        components.add(component);
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                collectComponents(child, components);
            }
        }
    }

    /** A call-recording {@link ItemIconRenderer} fake (ADR-0001 seam) for the {@code ui} package. */
    static final class RecordingRenderer implements ItemIconRenderer
    {
        final List<Integer> ids = new ArrayList<>();
        final Map<JLabel, Boolean> dimByLabel = new HashMap<>();

        @Override
        public void render(JLabel label, int itemId, int quantity, boolean stackable, boolean dim)
        {
            ids.add(itemId);
            dimByLabel.put(label, dim);
        }

        boolean drew(int itemId)
        {
            return ids.contains(itemId);
        }
    }
}

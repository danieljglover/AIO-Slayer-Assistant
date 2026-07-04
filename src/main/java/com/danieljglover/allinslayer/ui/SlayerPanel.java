package com.danieljglover.allinslayer.ui;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.loadout.Consumables;
import com.danieljglover.allinslayer.loadout.MagicSetup;
import com.danieljglover.allinslayer.loadout.MonsterProfile;
import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.StrategyMethod;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.TaskUnlock;
import com.danieljglover.allinslayer.model.UnlockType;
import com.danieljglover.allinslayer.ui.components.ActionBar;
import com.danieljglover.allinslayer.ui.components.EquipmentGrid;
import com.danieljglover.allinslayer.ui.components.InventoryGrid;
import com.danieljglover.allinslayer.ui.components.InvisibleScrollBarUI;
import com.danieljglover.allinslayer.ui.components.KeyValueRow;
import com.danieljglover.allinslayer.ui.components.LoadoutItemRow;
import com.danieljglover.allinslayer.ui.components.MethodSelector;
import com.danieljglover.allinslayer.ui.components.SectionCard;
import com.danieljglover.allinslayer.ui.components.Tag;
import com.danieljglover.allinslayer.ui.theme.SlayerTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import lombok.Setter;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;

/**
 * The All-In Slayer side panel: one scrollable dashboard laid out as a guided flow - header (task +
 * actions, anchored) -&gt; TASK -&gt; MONSTER -&gt; LOCATION -&gt; LOADOUT (ADR-0002).
 *
 * <p>The flow gates progressively: a multi-variant task shows a "Choose a monster" placeholder and
 * keeps the Location and Loadout steps locked until the player picks which monster to fight; the
 * location list is then filtered to where that monster exists, with the advisor's recommendation
 * pre-selected, and the loadout renders with the recommended attack style pre-selected but
 * overridable. Single-variant (and variant-less) tasks auto-complete the Monster step so nothing
 * blocks.</p>
 *
 * <p>{@link #render(SlayerPanelState)} is the sole public update entry (FR-1). Re-render is
 * non-destructive: the header, mode and export-enabled update in place; the location combo is built
 * once and only its model/selection are reconciled; each section self-diffs its backing slice and
 * rebuilds its body only when that slice changed; the vertical scroll value is captured and restored.
 * Item sprites are drawn through the injected {@link ItemIconRenderer} seam (ADR-0001), never via
 * {@code ItemManager} directly, so the panel is testable headless.</p>
 */
public class SlayerPanel extends PluginPanel
{
    /** The strategy-method combo's first item: revert to the engine's auto-pick (Phase 1 dynamic inventory). */
    private static final String METHOD_AUTO_LABEL = "Auto (recommended)";

    private final ItemIconRenderer renderer;

    private final HeaderBand headerBand;
    private final ActionBar actionBar;
    private final JScrollPane scrollPane;
    private final JPanel dashboardBody;
    private final PluginErrorPanel emptyState;
    private final TaskSection taskSection;
    private final MonsterSection monsterSection;
    private final LocationSection locationSection;
    private final LoadoutSection loadoutSection;
    private final DiagnosticsSection diagnosticsSection;

    @Setter
    private Runnable onRefresh;
    @Setter
    private Runnable onToggleMode;
    @Setter
    private Runnable onExport;
    @Setter
    private Consumer<String> onSelectLocation;
    // MV-B7: the variant/method selection callbacks. The plugin sets the selected name and recomputes;
    // the visible variant-combo / method-combo controls that invoke these are MV-FE1 / MV-FE3.
    @Setter
    private Consumer<String> onSelectVariant;
    @Setter
    private Consumer<String> onSelectMethod;
    // Dynamic inventory (Phase 1): the strategy-method override callback. Receives the method id (or null
    // for "Auto"); the plugin saves it per task and recomputes. Distinct from onSelectMethod, which
    // toggles the combat style.
    @Setter
    private Consumer<String> onSelectMethodId;
    // WA-11 (ADR-0018 #8 / PD-E): the master-selection callback. Receives the masterId slug (never
    // the display label); the plugin stores it and recomputes, mirroring the variant callback.
    @Setter
    private Consumer<String> onSelectMaster;

    @Inject
    public SlayerPanel(ItemManager itemManager)
    {
        this(new ItemManagerIconRenderer(itemManager));
    }

    /** Package-private seam constructor for headless tests (a fake {@link ItemIconRenderer}). */
    SlayerPanel(ItemIconRenderer renderer)
    {
        super(false);
        this.renderer = renderer;

        setLayout(new BorderLayout());
        setBackground(SlayerTheme.SURFACE_PAGE);

        actionBar = new ActionBar(
            AdviceMode.DPS,
            mode -> run(onToggleMode),
            () -> run(onRefresh),
            () -> run(onExport),
            false);
        headerBand = new HeaderBand(actionBar);
        add(headerBand, BorderLayout.NORTH);

        dashboardBody = new JPanel();
        dashboardBody.setName("dashboard-body");
        dashboardBody.setLayout(new BoxLayout(dashboardBody, BoxLayout.Y_AXIS));
        dashboardBody.setBackground(SlayerTheme.SURFACE_PAGE);

        emptyState = new PluginErrorPanel();
        emptyState.setAlignmentX(LEFT_ALIGNMENT);
        taskSection = new TaskSection();
        monsterSection = new MonsterSection();
        locationSection = new LocationSection();
        loadoutSection = new LoadoutSection();
        diagnosticsSection = new DiagnosticsSection();

        dashboardBody.add(emptyState);
        dashboardBody.add(Box.createVerticalStrut(SlayerTheme.SPACE_3));
        dashboardBody.add(taskSection);
        dashboardBody.add(Box.createVerticalStrut(SlayerTheme.SPACE_5));
        dashboardBody.add(monsterSection);
        dashboardBody.add(Box.createVerticalStrut(SlayerTheme.SPACE_5));
        dashboardBody.add(locationSection);
        dashboardBody.add(Box.createVerticalStrut(SlayerTheme.SPACE_5));
        dashboardBody.add(loadoutSection);
        dashboardBody.add(Box.createVerticalStrut(SlayerTheme.SPACE_5));
        dashboardBody.add(diagnosticsSection);

        // A viewport-tracking view (W8 F5): the scroll view is forced to the 225 viewport width so no
        // descendant can silently exceed it (PluginPanel never h-scrolls). BorderLayout.NORTH keeps the
        // body at that full width and its natural height. The proper ADR-0004 replacement for the
        // deleted FixedWidthPanel - honour the viewport width instead of hardcoding it.
        JPanel bodyWrap = new ViewportTrackingPanel();
        bodyWrap.setBackground(SlayerTheme.SURFACE_PAGE);
        bodyWrap.add(dashboardBody, BorderLayout.NORTH);

        scrollPane = new JScrollPane(bodyWrap);
        scrollPane.setBorder(null);
        scrollPane.setBackground(SlayerTheme.SURFACE_PAGE);
        // Vertical: AS_NEEDED so a tall dashboard is actually scrollable (a JScrollPane ignores the
        // mouse wheel when no scrollbar is shown, so NEVER would silently make tall content unreachable -
        // RV3 N-B). Horizontal stays NEVER; the viewport-tracking view keeps content within width.
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(true);
        // Hide the bar without disabling the wheel (W10 G1): the policy MUST stay AS_NEEDED (so the bar
        // is still isVisible() and the wheel handler keeps scrolling - NEVER would kill the wheel), but
        // a zero preferred width + a no-paint UI leave nothing to render and no gutter reserved. Wheel
        // and keyboard scroll the body; there is no draggable thumb (the requested behaviour). Mirrors
        // RuneLite's CustomScrollBarUI / Flipping Copilot's thin bar (research-r3 §3a).
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUI(new InvisibleScrollBarUI());
        verticalBar.setPreferredSize(new Dimension(0, 0));
        verticalBar.setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        render(SlayerPanelState.noTask(0, AdviceMode.DPS, null, null, RefreshSource.STARTUP,
            Instant.now(), SlayerDebugSnapshot.empty()));
    }

    /** Must be called on the Swing EDT. The single render entry point (FR-1). */
    public void render(SlayerPanelState state)
    {
        int scrollValue = scrollPane.getVerticalScrollBar().getValue();

        // In-place header / action updates - never a rebuild (ADR-0002).
        headerBand.getTaskHeader().update(state);
        actionBar.setMode(state.getMode());
        actionBar.setExportEnabled(state.getRecommendation() != null);

        if (hasTask(state))
        {
            emptyState.setVisible(false);
            taskSection.setVisible(true);
            monsterSection.setVisible(true);
            locationSection.setVisible(true);
            loadoutSection.setVisible(true);
            taskSection.update(state);
            monsterSection.update(state);
            locationSection.update(state);
            loadoutSection.update(state);
        }
        else
        {
            applyEmptyState(state);
            emptyState.setVisible(true);
            taskSection.setVisible(false);
            monsterSection.setVisible(false);
            locationSection.setVisible(false);
            loadoutSection.setVisible(false);
        }

        updateDiagnostics(state);

        revalidate();
        repaint();
        scrollPane.getVerticalScrollBar().setValue(scrollValue);
    }

    /** Total section-body rebuilds since construction (the self-diff probe for the NFR tests). */
    int sectionRebuildCount()
    {
        return taskSection.rebuilds + monsterSection.rebuilds + locationSection.rebuilds
            + loadoutSection.rebuilds + diagnosticsSection.rebuilds;
    }

    /** Diagnostics is developer-only (ADR-0003): rendered solely when developer mode is on. */
    private void updateDiagnostics(SlayerPanelState state)
    {
        if (state.isDeveloperMode())
        {
            diagnosticsSection.setVisible(true);
            diagnosticsSection.update(state);
        }
        else
        {
            diagnosticsSection.hideAndClear();
        }
    }

    private static boolean hasTask(SlayerPanelState state)
    {
        // BANK_NOT_SCANNED is "has task" too (ADR-0003): Task + Where/How render; only the loadout
        // card shows the gate prompt instead of gear.
        return state.getStatus() == PanelStatus.BANK_NOT_SCANNED
            || state.getStatus() == PanelStatus.TASK_WITH_LOADOUT
            || state.getStatus() == PanelStatus.TASK_WITHOUT_LOADOUT;
    }

    private void applyEmptyState(SlayerPanelState state)
    {
        if (state.getStatus() == PanelStatus.UNSUPPORTED_TASK)
        {
            emptyState.setContent("Task not in our data yet",
                "We don't have advice for this task yet. Your task count still shows above. "
                    + "We add tasks over time.");
        }
        else
        {
            emptyState.setContent("No Slayer task",
                "Get a task from a Slayer master, or check your enchanted gem or Slayer helm, "
                    + "to see advice here.");
        }
    }

    // ---- scroll view -----------------------------------------------------------------------------

    /**
     * The {@link JScrollPane} view (W8 F5). It tracks the viewport width so the viewport forces it to
     * exactly the 225px width - every BoxLayout-Y child then lays out within 225 (wrapping notes wrap,
     * capped rows cap, capped grids stay) and nothing can silently clip on the right, which a plain
     * {@code JPanel} view (sized to its widest child) allowed. Height grows freely so vertical scroll
     * is preserved (ADR-0002). The proper ADR-0004 replacement for the deleted {@code FixedWidthPanel}.
     */
    private static final class ViewportTrackingPanel extends JPanel implements Scrollable
    {
        ViewportTrackingPanel()
        {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true; // force the view to the viewport width - nothing exceeds 225 silently
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false; // let height grow so the vertical scroll still works
        }
    }

    // ---- header band -----------------------------------------------------------------------------

    /** Anchored (never-scrolling) top band: the humane {@code TaskHeader} above the {@code ActionBar}. */
    private static final class HeaderBand extends JPanel
    {
        private final com.danieljglover.allinslayer.ui.components.TaskHeader taskHeader;

        HeaderBand(ActionBar actionBar)
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(SlayerTheme.SURFACE_PAGE);
            taskHeader = new com.danieljglover.allinslayer.ui.components.TaskHeader();
            add(taskHeader);
            add(actionBar);
        }

        com.danieljglover.allinslayer.ui.components.TaskHeader getTaskHeader()
        {
            return taskHeader;
        }
    }

    // ---- TASK section ----------------------------------------------------------------------------

    private final class TaskSection extends SectionCard
    {
        private final JPanel body = column();

        // Master axis (WA-11, ADR-0018 #8 / PD-E): which assigning master's amounts/economy to show.
        // Built once + reconciled non-destructively, hidden+inert for a <=1-master task - the same
        // idiom as the variant/location combos. The context lines live in masterBody (rebuilt on
        // diff); the combo is a separate card child so it survives every rebuild.
        private final JLabel masterCaption;
        private final JComboBox<String> masterCombo = new JComboBox<>();
        private boolean updatingMaster;
        private List<String> currentMasterIds = Collections.emptyList();
        private Map<String, String> currentMasterNames = Collections.emptyMap();

        private final JPanel masterBody = column();
        private List<Object> lastKey;
        private int rebuilds;

        TaskSection()
        {
            super("1. Task");
            setName("task-section");
            addContent(body);

            addGap(SlayerTheme.SPACE_2);
            masterCaption = caption("Master");
            addContent(masterCaption);
            masterCombo.setName("master-combo");
            styleCombo(masterCombo);
            masterCombo.addActionListener(e ->
            {
                if (updatingMaster)
                {
                    return;
                }
                Object selected = masterCombo.getSelectedItem();
                if (selected != null)
                {
                    // The combo shows display names; the callback gets the masterId slug so the
                    // plugin can validate it against task.assignedBy.
                    String masterId = masterIdForLabel(
                        currentMasterIds, currentMasterNames, selected.toString());
                    if (masterId != null)
                    {
                        runWith(onSelectMaster, masterId);
                    }
                }
            });
            addContent(masterCombo);
            addContent(masterBody);
        }

        void update(SlayerPanelState state)
        {
            reconcileMaster(state);

            // selectedMaster + masterNames extend the key (plan R6 / ADR-0018 consequences) so a
            // selector change re-renders the master context; both are stable across a value-equal
            // render, so the zero-rebuild NFR-4 still holds.
            // + the skip/block note (WD-12) so a change in the disliked-task advisory re-renders this
            // section; it is stable across a value-equal render, so the zero-rebuild NFR-4 still holds.
            List<Object> key = Arrays.asList(
                state.getTask(), requiredState(state), state.getPlayerSlayerLevel(),
                state.getSelectedMaster(), state.getMasterNames(),
                state.getRecommendation() == null ? null : state.getRecommendation().getSkipBlockNote());
            if (key.equals(lastKey))
            {
                return;
            }
            lastKey = key;
            rebuilds++;
            rebuild(state);
        }

        private void rebuild(SlayerPanelState state)
        {
            body.removeAll();
            TaskData task = state.getTask();
            body.add(slayerLevelRow(task, state.getPlayerSlayerLevel()));

            String weakness = weaknessText(task);
            if (weakness != null)
            {
                body.add(new KeyValueRow("Weakness", weakness));
            }

            if (task.getRequiredItemId() != null)
            {
                LoadoutItemRow.State requiredState = requiredState(state);
                body.add(new LoadoutItemRow(renderer, task.getRequiredItemId(),
                    requiredName(state), "Required", 1, false, null, requiredState));
            }

            rebuildMasterContext(state);

            body.revalidate();
            body.repaint();
        }

        /**
         * The master-context lines (WA-11, ADR-0018 #8/#9): for the effective master, one prose line
         * "<Master> gives A-B; extended X-Y with <EXTENSION unlock> (N pts)." plus the per-(master,
         * task) weight when authored (null until WC-W - render nothing); the OTHER assigning masters'
         * amounts render compactly below (PD-E: amounts for all masters, economy for the selected).
         */
        private void rebuildMasterContext(SlayerPanelState state)
        {
            masterBody.removeAll();
            TaskData task = state.getTask();
            String master = effectiveMaster(state);
            if (master != null)
            {
                String context = masterContextText(task, master, state.getMasterNames());
                if (context != null)
                {
                    JTextArea note = wrappingNote(context, SlayerTheme.TEXT_PRIMARY);
                    note.setName("task-master-context");
                    masterBody.add(note);
                }
                Integer weight = task.getWeightByMaster() == null
                    ? null
                    : task.getWeightByMaster().get(master);
                if (weight != null)
                {
                    KeyValueRow row = new KeyValueRow("Weight", String.valueOf(weight));
                    row.setName("task-master-weight");
                    addLeft(masterBody, row);
                }
                String others = otherMastersText(task, master, state.getMasterNames());
                if (others != null)
                {
                    JTextArea also = wrappingNote(others, SlayerTheme.TEXT_SECONDARY);
                    also.setName("task-master-amounts");
                    masterBody.add(also);
                }
            }
            // The skip/block advisory (WD-12 / ADR-0020 #5): the advisor sets it only when the current
            // task is in the config disliked set. A Task-section note (mirrors task-master-context),
            // rendered only when set; independent of the master combo (renders even with no master).
            Recommendation rec = state.getRecommendation();
            if (rec != null && rec.getSkipBlockNote() != null && !rec.getSkipBlockNote().trim().isEmpty())
            {
                JTextArea skip = wrappingNote(rec.getSkipBlockNote().trim(), SlayerTheme.TEXT_PRIMARY);
                skip.setName("task-skip-block-note");
                masterBody.add(skip);
            }
            masterBody.revalidate();
            masterBody.repaint();
        }

        /**
         * Reconcile the master combo non-destructively (WA-11): items are display names (resolved from
         * {@code state.getMasterNames()}, slug prettified as the degraded-meta fallback), the effective
         * master is pre-selected without firing, and the combo hides for a <=1-master task (nothing to
         * choose) - identical to the variant-combo idiom.
         */
        private void reconcileMaster(SlayerPanelState state)
        {
            currentMasterIds = assigningMasters(state.getTask());
            currentMasterNames = state.getMasterNames();
            List<String> labels = new ArrayList<>(currentMasterIds.size());
            for (String masterId : currentMasterIds)
            {
                labels.add(masterDisplayName(masterId, currentMasterNames));
            }
            if (!comboItemsEqual(masterCombo, labels))
            {
                updatingMaster = true;
                masterCombo.removeAllItems();
                for (String label : labels)
                {
                    masterCombo.addItem(label);
                }
                updatingMaster = false;
            }

            String effective = effectiveMaster(state);
            String selectedLabel = effective == null
                ? null
                : masterDisplayName(effective, currentMasterNames);
            if (selectedLabel != null && !selectedLabel.equals(masterCombo.getSelectedItem()))
            {
                updatingMaster = true;
                masterCombo.setSelectedItem(selectedLabel);
                updatingMaster = false;
            }

            boolean multiple = currentMasterIds.size() > 1;
            masterCombo.setVisible(multiple);
            masterCaption.setVisible(multiple);
        }
    }

    // ---- MONSTER section (step 2) ----------------------------------------------------------------

    /**
     * Step 2 of the guided flow: pick WHICH monster of the assignment to hunt (MV-FE1). A
     * multi-variant task with no explicit pick shows a {@code Choose a monster...} placeholder and
     * keeps the Location and Loadout steps gated ({@link #variantResolved}); a single-variant task
     * shows its monster in a disabled combo (nothing to choose) and a variant-less task shows the
     * task name - both auto-complete the step. The combo is built once and reconciled
     * non-destructively, like the master/location combos (ADR-0002).
     */
    private final class MonsterSection extends SectionCard
    {
        private static final String CHOOSE_PLACEHOLDER = "Choose a monster...";

        private final JComboBox<String> variantCombo = new JComboBox<>();
        private boolean updatingVariant;
        private List<MonsterVariant> currentVariants = Collections.emptyList();

        private final JPanel body = column();
        private List<Object> lastKey;
        private int rebuilds;

        MonsterSection()
        {
            super("2. Monster");
            setName("monster-section");

            variantCombo.setName("variant-combo");
            styleCombo(variantCombo);
            variantCombo.addActionListener(e ->
            {
                if (updatingVariant)
                {
                    return;
                }
                Object selected = variantCombo.getSelectedItem();
                if (selected != null)
                {
                    // The combo shows a display label ("(Boss)" suffix); the callback gets the raw
                    // variant name so MonsterVariant.resolve can match it. The placeholder maps to
                    // no variant name, so re-selecting it can never fire the callback.
                    String name = variantNameForLabel(currentVariants, selected.toString());
                    if (name != null)
                    {
                        runWith(onSelectVariant, name);
                    }
                }
            });
            addContent(variantCombo);
            addContent(body);
        }

        void update(SlayerPanelState state)
        {
            reconcileVariant(state);

            List<Object> key = Arrays.asList(
                variantLabels(currentVariants),
                state.getSelectedVariantName(),
                variantResolved(state),
                state.getTask() == null ? null : state.getTask().getTask());
            if (key.equals(lastKey))
            {
                return;
            }
            lastKey = key;
            rebuilds++;
            rebuild(state);
        }

        private void rebuild(SlayerPanelState state)
        {
            body.removeAll();
            if (currentVariants.isEmpty())
            {
                // No variant data: the monster IS the task, so the step auto-completes.
                addLeft(body, new KeyValueRow("Monster", state.getTask().getTask()));
            }
            else if (!variantResolved(state))
            {
                JTextArea hint = wrappingNote(
                    "Pick which monster you'll fight - its locations and loadout unlock once chosen.",
                    SlayerTheme.TEXT_SECONDARY);
                hint.setName("monster-gate-hint");
                addLeft(body, hint);
            }
            body.revalidate();
            body.repaint();
        }

        /**
         * Reconcile the variant combo non-destructively (MV-FE1). Items are display labels (a boss
         * variant gets a {@code " (Boss)"} suffix, FR-7). While the step is unresolved (multiple
         * variants, no pick) a placeholder heads the list and is pre-selected; once resolved the
         * placeholder disappears and the effective variant is pre-selected without firing. A
         * single-variant combo stays visible but disabled so the completed step still reads.
         */
        private void reconcileVariant(SlayerPanelState state)
        {
            currentVariants = variantsOf(state);
            boolean resolved = variantResolved(state);
            List<String> items = new ArrayList<>(variantLabels(currentVariants));
            if (!resolved)
            {
                items.add(0, CHOOSE_PLACEHOLDER);
            }
            if (!comboItemsEqual(variantCombo, items))
            {
                updatingVariant = true;
                variantCombo.removeAllItems();
                for (String item : items)
                {
                    variantCombo.addItem(item);
                }
                updatingVariant = false;
            }

            String selectedLabel = resolved
                ? selectedVariantLabel(state, currentVariants)
                : CHOOSE_PLACEHOLDER;
            if (selectedLabel != null && !selectedLabel.equals(variantCombo.getSelectedItem()))
            {
                updatingVariant = true;
                variantCombo.setSelectedItem(selectedLabel);
                updatingVariant = false;
            }

            variantCombo.setVisible(!currentVariants.isEmpty());
            variantCombo.setEnabled(currentVariants.size() > 1);
        }
    }

    // ---- LOCATION section (step 3) ---------------------------------------------------------------

    /**
     * Step 3 of the guided flow: WHERE to fight the chosen monster. Locked behind the Monster step
     * ({@link #variantResolved}) - while gated it shows only a hint. Once unlocked, the combo lists
     * the task's locations filtered to the chosen variant ({@link #locationNames}) with the advisor's
     * recommendation pre-selected (best option first, manual override via the combo), followed by the
     * recommended-location headline, its tags and the why/method prose.
     */
    private final class LocationSection extends SectionCard
    {
        private final JComboBox<String> combo = new JComboBox<>();
        private boolean updatingCombo;

        private final JPanel body = column();
        private List<Object> lastKey;
        private int rebuilds;

        LocationSection()
        {
            super("3. Location");
            setName("where-section");

            combo.setName("where-location-combo");
            styleCombo(combo);
            combo.addActionListener(e ->
            {
                if (updatingCombo)
                {
                    return;
                }
                Object selected = combo.getSelectedItem();
                if (selected != null)
                {
                    runWith(onSelectLocation, selected.toString());
                }
            });
            addContent(combo);

            // The recommended-location headline + why/method prose read below the location control
            // they describe (DT-FE1 / G7).
            addGap(SlayerTheme.SPACE_3);
            addContent(body);
        }

        void update(SlayerPanelState state)
        {
            boolean gated = !variantResolved(state);
            if (!gated)
            {
                reconcileCombo(state);
            }
            combo.setVisible(!gated && !locationNames(state).isEmpty());

            List<Object> key = Arrays.asList(gated,
                recommendedLocation(state), locationReason(state), methodText(state));
            if (key.equals(lastKey))
            {
                return;
            }
            lastKey = key;
            rebuilds++;
            rebuild(state, gated);
        }

        private void rebuild(SlayerPanelState state, boolean gated)
        {
            body.removeAll();

            if (gated)
            {
                JTextArea hint = wrappingNote("Choose a monster above to see where to fight it.",
                    SlayerTheme.TEXT_MUTED);
                hint.setName("where-gate-hint");
                addLeft(body, hint);
                body.revalidate();
                body.repaint();
                return;
            }

            SlayerLocation recommended = recommendedLocation(state);
            JLabel recommendedLabel = new JLabel(recommended == null ? "" : recommended.getName());
            recommendedLabel.setName("where-recommended");
            recommendedLabel.setFont(SlayerTheme.TYPE_TITLE);
            recommendedLabel.setForeground(SlayerTheme.ACCENT_BRAND); // the one real-state brand use here
            recommendedLabel.setAlignmentX(LEFT_ALIGNMENT);
            body.add(recommendedLabel);

            if (recommended != null)
            {
                JPanel tags = tagRow(recommended);
                if (tags.getComponentCount() > 0)
                {
                    body.add(tags);
                }
            }

            // Why / Method are prose, not scannable key/values: render them as wrapping notes so they
            // wrap within the panel instead of clipping on a single-line KeyValueRow (W8 F2 / FR-10).
            String reason = locationReason(state);
            if (!reason.isEmpty())
            {
                body.add(caption("Why"));
                JTextArea why = wrappingNote(reason, SlayerTheme.TEXT_PRIMARY);
                why.setName("where-why");
                body.add(why);
            }
            String method = methodText(state);
            if (!method.isEmpty())
            {
                // "Wiki method" (WB-3 / D7): the prose is the wiki's recommendedMethod free text,
                // which can differ from the effective style the loadout gears for - honest label.
                body.add(caption("Wiki method"));
                JTextArea methodNote = wrappingNote(method, SlayerTheme.TEXT_PRIMARY);
                methodNote.setName("where-method");
                body.add(methodNote);
            }

            body.revalidate();
            body.repaint();
        }

        private void reconcileCombo(SlayerPanelState state)
        {
            List<String> names = locationNames(state);
            if (!comboItemsEqual(combo, names))
            {
                updatingCombo = true;
                combo.removeAllItems();
                for (String name : names)
                {
                    combo.addItem(name);
                }
                updatingCombo = false;
            }

            String selected = selectedLocationName(state);
            if (selected != null && !selected.equals(combo.getSelectedItem()))
            {
                updatingCombo = true;
                combo.setSelectedItem(selected);
                updatingCombo = false;
            }
        }
    }

    // ---- LOADOUT section -------------------------------------------------------------------------

    private final class LoadoutSection extends SectionCard
    {
        // Method axis (MV-FE3): the combat style override for the suggested loadout. Lives with the
        // loadout it re-gears (the recommended style is pre-selected; a click overrides it).
        private final JLabel methodCaption;
        private final MethodSelector methodSelector;
        // Dynamic inventory (Phase 1): the strategy-method override combo, below the style segments.
        // "Auto (recommended)" plus each pickable method; shown only when the strategy has >=2 methods.
        private final JLabel strategyMethodCaption;
        private final JComboBox<String> strategyMethodCombo = new JComboBox<>();
        private final Map<String, String> methodLabelToId = new HashMap<>();
        private boolean suppressMethodEvent;

        private final JPanel body = column();
        private List<Object> lastKey;
        private int rebuilds;

        LoadoutSection()
        {
            super("4. Loadout");
            setName("loadout-section");

            methodCaption = caption("Attack style");
            addContent(methodCaption);
            methodSelector = new MethodSelector(style -> runWith(onSelectMethod, MethodSelector.label(style)));
            methodSelector.setName("method-selector");
            methodSelector.setAlignmentX(LEFT_ALIGNMENT);
            methodSelector.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, methodSelector.getPreferredSize().height));
            addContent(methodSelector);
            addGap(SlayerTheme.SPACE_3);

            strategyMethodCaption = caption("Strategy method");
            addContent(strategyMethodCaption);
            strategyMethodCombo.setName("strategy-method-combo");
            styleCombo(strategyMethodCombo);
            strategyMethodCombo.addActionListener(e ->
            {
                if (suppressMethodEvent)
                {
                    return;
                }
                Object selected = strategyMethodCombo.getSelectedItem();
                runWith(onSelectMethodId, selected == null ? null : methodLabelToId.get(selected.toString()));
            });
            addContent(strategyMethodCombo);
            addGap(SlayerTheme.SPACE_3);

            addContent(body);
        }

        void update(SlayerPanelState state)
        {
            // Gated behind the Monster step, like the Location section: until the player picks a
            // monster the loadout would be for a monster they may not fight.
            boolean gated = !variantResolved(state);
            methodCaption.setVisible(!gated);
            methodSelector.setVisible(!gated);
            if (!gated)
            {
                reconcileMethod(state);
            }
            reconcileStrategyMethod(state, gated);

            // Status is in the key so BANK_NOT_SCANNED (gate prompt) and TASK_WITHOUT_LOADOUT (empty
            // state) - both rec==null - render distinctly. Status is stable across a value-equal
            // re-render, so the zero-rebuild NFR-4 still holds.
            List<Object> key = Arrays.asList(gated, state.getStatus(), state.getRecommendation(),
                state.getItemNames(), state.getItemPrices(), state.getBankAge(), state.isBankStale(),
                state.getTask() == null ? null : state.getTask().getRequiredItemId());
            if (key.equals(lastKey))
            {
                return;
            }
            lastKey = key;
            rebuilds++;
            rebuild(state, gated);
        }

        /**
         * Reconcile the method selector (MV-FE3). The selection reflects {@code selectedMethod ??
         * recommendedStyle} (the variant's default style, ADR-0013) without firing the callback. A
         * method is disabled only when it yields no viable loadout: the panel holds ONE recommendation
         * (for the effective method), so it can assess viability only for that method - the engine
         * returning an empty loadout surfaces as {@link PanelStatus#TASK_WITHOUT_LOADOUT}, the narrow
         * no-owned-weapon / magic-no-spell case the backend flagged. Other methods stay enabled (the
         * panel has no per-method signal to disable them, and most are viable - a wrong-type weapon
         * still scores positive DPS).
         */
        private void reconcileMethod(SlayerPanelState state)
        {
            CombatStyle effective = effectiveMethod(state);
            methodSelector.setMethod(effective);

            boolean effectiveNotViable = state.getStatus() == PanelStatus.TASK_WITHOUT_LOADOUT
                && effective != null;
            for (CombatStyle style : CombatStyle.values())
            {
                methodSelector.setMethodEnabled(style, !(effectiveNotViable && style == effective));
            }
        }

        /**
         * Reconcile the strategy-method override combo (Phase 1 dynamic inventory). Shown only when the
         * variant is resolved AND its strategy has >=2 pickable methods; the items are "Auto
         * (recommended)" plus each method label, and the active method (rec.methodId) is pre-selected
         * without firing the callback. Selecting "Auto" clears the per-task override.
         */
        private void reconcileStrategyMethod(SlayerPanelState state, boolean gated)
        {
            Recommendation rec = gated ? null : state.getRecommendation();
            List<StrategyMethod> options = rec == null ? null : rec.getMethodOptions();
            boolean visible = options != null && options.size() >= 2;
            strategyMethodCaption.setVisible(visible);
            strategyMethodCombo.setVisible(visible);
            if (!visible)
            {
                return;
            }

            List<String> labels = new ArrayList<>();
            labels.add(METHOD_AUTO_LABEL);
            methodLabelToId.clear();
            for (StrategyMethod method : options)
            {
                String label = method.getLabel() == null ? method.getMethodId() : method.getLabel();
                if (label == null)
                {
                    continue;
                }
                labels.add(label);
                methodLabelToId.put(label, method.getMethodId());
            }

            suppressMethodEvent = true;
            try
            {
                if (!comboItemsEqual(strategyMethodCombo, labels))
                {
                    strategyMethodCombo.removeAllItems();
                    for (String label : labels)
                    {
                        strategyMethodCombo.addItem(label);
                    }
                }
                String activeLabel = labelForMethodId(options, rec.getMethodId());
                strategyMethodCombo.setSelectedItem(activeLabel == null ? METHOD_AUTO_LABEL : activeLabel);
            }
            finally
            {
                suppressMethodEvent = false;
            }
        }

        private String labelForMethodId(List<StrategyMethod> options, String methodId)
        {
            if (methodId == null)
            {
                return null;
            }
            for (StrategyMethod method : options)
            {
                if (methodId.equals(method.getMethodId()))
                {
                    return method.getLabel() == null ? method.getMethodId() : method.getLabel();
                }
            }
            return null;
        }

        private void rebuild(SlayerPanelState state, boolean gated)
        {
            body.removeAll();

            if (gated)
            {
                JTextArea hint = wrappingNote("Choose a monster above to get a suggested loadout.",
                    SlayerTheme.TEXT_MUTED);
                hint.setName("loadout-gate-hint");
                addLeft(body, hint);
                body.revalidate();
                body.repaint();
                return;
            }

            // Bank-gate (ADR-0003 / FR-1): a task is present but the bank has never been scanned, so no
            // loadout exists yet. Show the "open your bank" prompt and NO gear rows; Task + Where/How
            // still render (they are separate sections).
            if (state.getStatus() == PanelStatus.BANK_NOT_SCANNED)
            {
                JTextArea prompt = wrappingNote(state.getStatusMessage(), SlayerTheme.TEXT_PRIMARY);
                prompt.setName("loadout-bank-gate");
                addLeft(body, prompt);
                body.revalidate();
                body.repaint();
                return;
            }

            Recommendation rec = state.getRecommendation();
            if (rec == null)
            {
                JLabel empty = new JLabel("No owned loadout found");
                empty.setFont(SlayerTheme.TYPE_BODY);
                empty.setForeground(SlayerTheme.TEXT_PRIMARY);
                addLeft(body, empty);
                JTextArea hint = wrappingNote(
                    "Scan your bank, or switch to Cost mode for cheaper options.",
                    SlayerTheme.TEXT_SECONDARY);
                hint.setName("loadout-no-loadout-hint");
                addLeft(body, hint);
                if (state.getBankAge() == null)
                {
                    addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
                    addLeft(body, noBankNote());
                }
                body.revalidate();
                body.repaint();
                return;
            }

            // Boss separateness (FR-7 / MV-FE2): a boss variant is a standalone trip, not part of the
            // grind, so flag it prominently above the loadout with its location + requirement notes.
            if (rec.isBoss())
            {
                addLeft(body, bossNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // The "Wiki strategy" guidance note (ADR-0015 / MV-S4): shown whenever the resolved variant
            // has a strategy, so it reads as context for the loadout above the worn grid. Informational
            // even when the loadout recommends a stat-driven fallback (the user owns no strategy weapon).
            if (rec.getStrategyNote() != null && !rec.getStrategyNote().trim().isEmpty())
            {
                addLeft(body, strategyNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // The strategy-fallback disclosure (WB-4 / D8): the advisor sets it only for a
            // present-but-malformed strategy whose gear fell back to the stat engine. Rendered in
            // the strategy note's slot so the player learns WHY there is no wiki guidance.
            if (rec.getStrategyFallbackNote() != null && !rec.getStrategyFallbackNote().trim().isEmpty())
            {
                addLeft(body, strategyFallbackNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // Tactical location notes (DT-FE3 / DT-B10, ADR-0017 #4): the safespot method hint (set by
            // the advisor only for a safespot location + ranged/magic, NG-4 - a note, not combat maths)
            // and the effective location's free-text travel/access note. Each renders only when the
            // advisor set it (non-null), mirroring the boss/strategy notes above.
            if (rec.getLocationHint() != null && !rec.getLocationHint().trim().isEmpty())
            {
                addLeft(body, locationHintNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }
            if (rec.getAccessNote() != null && !rec.getAccessNote().trim().isEmpty())
            {
                addLeft(body, locationAccessNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // The antifire "note when unowned" nudge (DT-FE / DT-B11, PD-3): the advisor sets it only for
            // a draconic monster when the player owns no antifire (the InventorySelector supply covers the
            // owned case). Renders only when set, mirroring the location notes above.
            if (rec.getAntifireNote() != null && !rec.getAntifireNote().trim().isEmpty())
            {
                addLeft(body, antifireNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // The prayer/survival advisory (WD-3 / ADR-0020 #1): the advisor composes it from the
            // resolved monster's offence (prayer from attack styles, max-hit survival line, owned-driven
            // poison/venom nudge). A note only (NG-4), rendered only when set, mirroring the antifire note.
            if (rec.getSurvivalNote() != null && !rec.getSurvivalNote().trim().isEmpty())
            {
                addLeft(body, survivalNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // Dynamic inventory (Phase 1): the selected method's prayers, the sustain sizing line, and the
            // "strategy wants but you own none" advisory. Each renders only when the advisor set it,
            // mirroring the notes above. Prayers are a compact KeyValueRow; the others are wrapping notes.
            if (rec.getPrayers() != null && !rec.getPrayers().isEmpty())
            {
                addLeft(body, new KeyValueRow("Prayers", String.join(", ", rec.getPrayers())));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }
            if (rec.getSustainNote() != null && !rec.getSustainNote().trim().isEmpty())
            {
                addLeft(body, wrappingNote(rec.getSustainNote().trim(), SlayerTheme.TEXT_PRIMARY));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }
            if (rec.getMissingKeyItemsNote() != null && !rec.getMissingKeyItemsNote().trim().isEmpty())
            {
                addLeft(body, wrappingNote(rec.getMissingKeyItemsNote().trim(), SlayerTheme.TEXT_SECONDARY));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // The unlock-gated gear guard (WA-12 / ADR-0018 #9b): the advisor sets it only when a
            // recommended item's gear family is gated behind an unowned reward-shop unlock. A note
            // only (NG-4), rendered only when set, mirroring the strategy/antifire notes above.
            if (rec.getUnlockGuardNote() != null && !rec.getUnlockGuardNote().trim().isEmpty())
            {
                addLeft(body, unlockGuardNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // The "what to unlock next" hint (WA-13 / ADR-0018 #9d): the advisor computes the
            // highest-value affordable unowned unlock; this only renders it. text/secondary - a
            // points-spending lever, like the bank nudges, not task-critical advice.
            if (rec.getUnlockHint() != null && !rec.getUnlockHint().trim().isEmpty())
            {
                addLeft(body, unlockHintNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }

            // At-a-glance worn grid (secondary overview, Decision 3). Wrapped full-width so it sits
            // flush-left like the worn rows below, not floating off-centre (W10 G2).
            addLeft(body, leftRow(new EquipmentGrid(renderer, rec.getWorn(), state.getItemNames())));
            addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));

            // Worn detail rows (primary, Decision 3).
            if (rec.getWorn() != null)
            {
                for (Map.Entry<EquipmentSlot, Integer> entry : rec.getWorn().entrySet())
                {
                    int itemId = entry.getValue();
                    addLeft(body, new LoadoutItemRow(renderer, itemId, itemName(state, itemId),
                        prettySlot(entry.getKey()), 1, false, price(state, itemId),
                        LoadoutItemRow.State.OWNED));
                }
            }

            // The full 28-slot suggested trip inventory (owned supplies + consumables); empty wells
            // pad the unfilled slots so the bag keeps its 4x7 in-game shape.
            if (rec.getTripInventory() != null && !rec.getTripInventory().isEmpty())
            {
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
                addLeft(body, caption("Inventory"));
                addLeft(body, leftRow(new InventoryGrid(renderer, rec.getTripInventory(), state.getItemNames())));
            }

            addConsumables(body, state, rec.getConsumables());

            addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            addLeft(body, new KeyValueRow("Est. DPS",
                String.format(Locale.ROOT, "%.1f", rec.getEstimatedDps())));
            // The estimate's honest scope (WB-7 / PD-B, D2-cheap): the DPS model covers one target
            // and no cannon term. A separate secondary line under the number - appended to the row
            // value it would ellipsize away at 225px. The full cannon DPS term stays P2 (Wave D).
            JTextArea dpsScope = wrappingNote("single-target, excl. cannon", SlayerTheme.TEXT_SECONDARY);
            dpsScope.setName("loadout-dps-scope");
            addLeft(body, dpsScope);
            // WD-6 (ADR-0020 #3): the honest single-target cannon DPS line - a SEPARATE additive term
            // beside the "excl. cannon" headline (never folded into the ranked number). Rendered only
            // when non-null (cannon location + owned cannon); carries the multi-combat ceiling caveat.
            if (rec.getCannonDpsNote() != null && !rec.getCannonDpsNote().isEmpty())
            {
                JTextArea cannonDps = wrappingNote(rec.getCannonDpsNote(), SlayerTheme.TEXT_SECONDARY);
                cannonDps.setName("loadout-cannon-dps");
                addLeft(body, cannonDps);
            }
            addLeft(body, gearCostRow(rec.getTotalGearCost()));

            addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            // WB-6 (D12): bankAge is never null on the with-loadout card - a recommendation exists
            // only past the ADR-0003 bank gate (bankLastSeen non-null -> bankAgeText non-null), so
            // the old else-branch rendering the no-bank nuance here was unreachable dead UX. The
            // never-scanned case is the BANK_NOT_SCANNED gate prompt above; the null check remains
            // purely defensive for direct API callers.
            if (state.getBankAge() != null)
            {
                addLeft(body, new KeyValueRow("Bank seen", state.getBankAge()));
                // Login-staleness nudge (DT-FE4 / G4, PD-5): an aged snapshot still gives usable advice,
                // so this is a lever, not an error - shown under the bank-seen row when past threshold.
                if (state.isBankStale())
                {
                    addLeft(body, bankStaleNote(state.getBankAge()));
                }
            }

            body.revalidate();
            body.repaint();
        }
    }

    /**
     * Render the consumable half of a loadout (plan section 5 / FR-10): the best owned food, an
     * optional combo food (karambwan), the best owned style-matching potion, and - for a magic task -
     * the chosen spell plus its per-cast rune rows. A rune the player is short of (listed in
     * {@link MagicSetup#getRunesShort()}) renders BLOCKED/red; the rest are plain OWNED. Absent parts
     * are omitted cleanly: with nothing to show, no caption is rendered.
     */
    private void addConsumables(JPanel body, SlayerPanelState state, Consumables consumables)
    {
        if (consumables == null)
        {
            return;
        }
        MagicSetup magic = consumables.getMagic();
        boolean hasRunes = magic != null && magic.getRuneRequirement() != null
            && !magic.getRuneRequirement().isEmpty();
        boolean hasSpell = magic != null && magic.getSpellName() != null;
        boolean anything = consumables.getFoodId() != null || consumables.getComboFoodId() != null
            || consumables.getPotionId() != null || hasSpell || hasRunes;
        if (!anything)
        {
            return;
        }

        addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
        addLeft(body, caption("Consumables"));

        if (consumables.getFoodId() != null)
        {
            addLeft(body, consumableRow(state, consumables.getFoodId(), "Food"));
        }
        if (consumables.getComboFoodId() != null)
        {
            addLeft(body, consumableRow(state, consumables.getComboFoodId(), "Combo food"));
        }
        if (consumables.getPotionId() != null)
        {
            addLeft(body, consumableRow(state, consumables.getPotionId(), "Potion"));
        }

        if (hasSpell)
        {
            addLeft(body, new KeyValueRow("Spell", magic.getSpellName()));
        }
        if (hasRunes)
        {
            for (Map.Entry<Integer, Integer> entry : magic.getRuneRequirement().entrySet())
            {
                int runeId = entry.getKey();
                boolean shortOfIt = magic.getRunesShort() != null
                    && magic.getRunesShort().containsKey(runeId);
                addLeft(body, new LoadoutItemRow(renderer, runeId, itemName(state, runeId), "Rune",
                    entry.getValue(), true, price(state, runeId),
                    shortOfIt ? LoadoutItemRow.State.BLOCKED : LoadoutItemRow.State.OWNED));
            }
        }
    }

    private LoadoutItemRow consumableRow(SlayerPanelState state, int itemId, String slotText)
    {
        return new LoadoutItemRow(renderer, itemId, itemName(state, itemId), slotText, 1, false,
            price(state, itemId), LoadoutItemRow.State.OWNED);
    }

    // ---- DIAGNOSTICS section (developer-only, ADR-0003) ------------------------------------------

    /**
     * The developer-only diagnostics surface (ADR-0003). It is the same scroll-body self-diffing
     * {@link SectionCard} as the others, keyed on the rendered telemetry (status, refresh source and
     * the full {@link SlayerDebugSnapshot}), but it is only updated and shown when
     * {@code state.isDeveloperMode()} is true. Default players never see it; when off it is hidden
     * and its body cleared so no telemetry lingers in the tree. It surfaces the FR-2/3/4 diagnostic
     * fields and carries only local game/client state (FR-11) - the raw enums and varps that the
     * humane header deliberately hides.
     */
    private final class DiagnosticsSection extends SectionCard
    {
        private final JPanel body = column();
        private List<Object> lastKey;
        private int rebuilds;

        DiagnosticsSection()
        {
            super("Diagnostics (developer)");
            setName("diagnostics-section");
            addContent(body);
        }

        void update(SlayerPanelState state)
        {
            List<Object> key = Arrays.asList(
                state.getStatus(), state.getRefreshSource(), state.getDebug());
            if (key.equals(lastKey))
            {
                return;
            }
            lastKey = key;
            rebuilds++;
            rebuild(state);
        }

        /** Default-player path: hide and clear so no diagnostics labels remain in the tree. */
        void hideAndClear()
        {
            setVisible(false);
            if (lastKey != null)
            {
                lastKey = null;
                body.removeAll();
                body.revalidate();
                body.repaint();
            }
        }

        private void rebuild(SlayerPanelState state)
        {
            body.removeAll();
            SlayerDebugSnapshot debug = state.getDebug();
            body.add(diagRow("diag-status", "Status", String.valueOf(state.getStatus())));
            body.add(diagRow("diag-source", "Source", String.valueOf(state.getRefreshSource())));
            body.add(diagRow("diag-menu-option", "Menu option", debug.getMenuOption()));
            body.add(diagRow("diag-menu-action", "Menu action", String.valueOf(debug.getMenuAction())));
            body.add(diagRow("diag-raw-item", "Raw item id", String.valueOf(debug.getRawItemId())));
            body.add(diagRow("diag-mapped-item", "Mapped item id",
                String.valueOf(debug.getMappedItemId())));
            body.add(diagRow("diag-matched-check", "Matched check",
                String.valueOf(debug.isMatchedTaskCheck())));
            body.add(diagRow("diag-target-varp", "Target varp",
                String.valueOf(debug.getSlayerTargetVarp())));
            body.add(diagRow("diag-count-varp", "Count varp",
                String.valueOf(debug.getSlayerCountVarp())));
            body.add(diagRow("diag-area-varp", "Area varp", String.valueOf(debug.getSlayerAreaVarp())));
            body.add(diagRow("diag-boss-varbit", "Boss varbit",
                String.valueOf(debug.getBossTargetVarbit())));
            body.add(diagRow("diag-detector", "Detector", debug.getDetectorResult()));
            body.add(diagRow("diag-unsupported", "Unsupported target",
                String.valueOf(debug.getUnsupportedTargetId())));
            body.revalidate();
            body.repaint();
        }
    }

    // ---- shared helpers --------------------------------------------------------------------------

    /** Non-destructive combo reconcile probe shared by the master/variant/location combos. */
    private static boolean comboItemsEqual(JComboBox<String> box, List<String> names)
    {
        if (box.getItemCount() != names.size())
        {
            return false;
        }
        for (int i = 0; i < names.size(); i++)
        {
            if (!names.get(i).equals(box.getItemAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    private static KeyValueRow diagRow(String name, String key, String value)
    {
        KeyValueRow row = new KeyValueRow(key, value);
        row.setName(name);
        return row;
    }

    /**
     * The "Slayer lvl" row, tinted by whether the player meets the requirement (P1-10). Green
     * ({@code STATE_MET}) when the player's Slayer level reaches the task's requirement, blocked red
     * when it is known and below it, and neutral when the level is unknown ({@code playerSlayerLevel}
     * defaults to -1 outside the supported-task path).
     */
    private static KeyValueRow slayerLevelRow(TaskData task, int playerSlayerLevel)
    {
        KeyValueRow row = new KeyValueRow("Slayer lvl", String.valueOf(task.getSlayerLevel()));
        if (playerSlayerLevel >= 0)
        {
            row.setValueColor(playerSlayerLevel >= task.getSlayerLevel()
                ? SlayerTheme.STATE_MET
                : SlayerTheme.STATE_BLOCKED);
        }
        return row;
    }

    private static JPanel column()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SlayerTheme.SURFACE_CARD);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    /**
     * Pin a fixed-size component (one of the loadout grids) flush-left inside a full-width row so the
     * BoxLayout-Y card body cannot offset it (W10 G2). The grids are the only width-capped children in
     * the body ({@code getMaximumSize() == getPreferredSize()}), so in a column of full-width rows they
     * are the only thing {@code BoxLayout} can mis-position on the minor axis when sibling
     * {@code alignmentX} values differ - which left them indented. A full-width wrapper occupies the
     * whole column and hugs the grid to the {@code WEST}, so the grid sits at the same left edge as the
     * worn rows regardless of any sibling's alignment. This is Flipping Copilot's wrapper idiom
     * (FlipPanel:40-42) - left alignment by construction, not by {@code alignmentX}.
     */
    private static JComponent leftRow(JComponent fixed)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(fixed, BorderLayout.WEST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, fixed.getPreferredSize().height));
        return row;
    }

    /**
     * Add {@code component} to a section body forcing it flush-left ({@code LEFT_ALIGNMENT}) so the
     * whole BoxLayout-Y column shares one minor-axis alignment (W10 G2). Mixing alignmentX is what let
     * a LEFT-aligned child (a width-capped grid, even one wrapped by {@link #leftRow}) drift right
     * while the CENTER-default rows filled from the left edge - a uniform column has no such slack.
     * Matches the convention {@code dashboardBody} and {@link SectionCard#addContent} already use.
     */
    private static void addLeft(JPanel body, Component component)
    {
        if (component instanceof JComponent)
        {
            ((JComponent) component).setAlignmentX(LEFT_ALIGNMENT);
        }
        body.add(component);
    }

    /**
     * The non-blocking "no bank seen yet" notice (UX §4 State C). Shown whenever {@code bankAge} is
     * null - advice is then built only from worn + carried gear - in {@code text/secondary}, not the
     * blocked red, because it is reassurance and a lever ("open your bank"), not an error.
     */
    private static JTextArea noBankNote()
    {
        JTextArea note = wrappingNote(
            "We haven't seen your bank yet - advice uses what you're wearing and carrying. "
                + "Open your bank to improve it.",
            SlayerTheme.TEXT_SECONDARY);
        note.setName("loadout-no-bank-note");
        return note;
    }

    /**
     * The login-staleness nudge (DT-FE4 / G4, PD-5): shown when the persisted bank snapshot has aged
     * past the threshold ({@code state.isBankStale()}). {@code text/secondary} - a non-blocking lever,
     * like {@link #noBankNote}, never the blocked red: the loadout is still usable, just built from an
     * older snapshot, and reopening the bank refreshes it.
     */
    private static JTextArea bankStaleNote(String bankAge)
    {
        JTextArea note = wrappingNote(
            "Bank last scanned " + bankAge + " - reopen your bank to refresh for accurate advice.",
            SlayerTheme.TEXT_SECONDARY);
        note.setName("loadout-bank-stale-note");
        return note;
    }

    /**
     * A read-only, line-wrapping note styled to read and sit like a caption label, without the
     * {@code <html>} hack ADR-0004 bans. Long player-facing copy then wraps within the panel width
     * instead of clipping mid-word (S1 / FR-10). The max height tracks the (wrapped) preferred height
     * so it does not stretch vertically inside the BoxLayout-Y card body - the same idiom
     * {@code LoadoutItemRow} uses.
     */
    private static JTextArea wrappingNote(String text, Color foreground)
    {
        JTextArea note = new JTextArea(text)
        {
            @Override
            public Dimension getMaximumSize()
            {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setEditable(false);
        note.setFocusable(false);
        note.setOpaque(true);
        note.setBackground(SlayerTheme.SURFACE_CARD);
        note.setForeground(foreground);
        note.setFont(SlayerTheme.TYPE_CAPTION);
        note.setBorder(null);
        note.setAlignmentX(LEFT_ALIGNMENT);
        return note;
    }

    /**
     * The FR-7 "Boss - separate trip" note (MV-FE2), shown when the selected variant is a boss so its
     * loadout is not mistaken for a grind loadout. Appends the variant's free-text location and any
     * requirement that adds detail beyond the headline (the dataset often stores the requirement AS
     * "boss - separate trip", which would just duplicate the headline).
     */
    private static JTextArea bossNote(Recommendation rec)
    {
        StringBuilder text = new StringBuilder("Boss - separate trip.");
        if (rec.getVariantLocation() != null && !rec.getVariantLocation().trim().isEmpty())
        {
            text.append(" Location: ").append(rec.getVariantLocation().trim()).append('.');
        }
        String requirement = rec.getVariantRequirement();
        if (requirement != null && !requirement.trim().isEmpty()
            && !requirement.trim().equalsIgnoreCase("boss - separate trip"))
        {
            text.append(' ').append(requirement.trim());
            if (!requirement.trim().endsWith("."))
            {
                text.append('.');
            }
        }
        JTextArea note = wrappingNote(text.toString(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-boss-note");
        return note;
    }

    /**
     * The "Wiki strategy" guidance note (ADR-0015 / MV-S4), mirroring {@link #bossNote}. The advisor
     * composes the text (primary + secondary weapons + the free-text note); this only renders it. Shown
     * whenever the resolved variant has a strategy, informational even when the player owns none of the
     * strategy weapons - the secondary weapon (e.g. "switch to Scorching bow for ranged") lives here,
     * not in a second loadout.
     */
    private static JTextArea strategyNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getStrategyNote().trim(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-strategy-note");
        return note;
    }

    /**
     * The strategy-fallback disclosure (WB-4 / D8): the advisor sets it only when a variant's
     * authored strategy exists but is malformed, so the loadout is a stat pick, not the wiki
     * guide. {@code text/secondary} - a data-quality notice, not tactical advice.
     */
    private static JTextArea strategyFallbackNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getStrategyFallbackNote().trim(), SlayerTheme.TEXT_SECONDARY);
        note.setName("loadout-strategy-fallback-note");
        return note;
    }

    /**
     * The safespot method hint (DT-FE3 / DT-B10, ADR-0017 #4 / GAP-2). The advisor composes it only for
     * a safespot location fought with ranged/magic; this only renders it, mirroring {@link #strategyNote}.
     * Primary text - it is real tactical advice, like the strategy line.
     */
    private static JTextArea locationHintNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getLocationHint().trim(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-location-hint");
        return note;
    }

    /**
     * The effective location's free-text travel/access note (DT-FE3 / DT-B10): a quest/teleport/agility
     * gate the player should know before the trip. Rendered only when the advisor set it, mirroring the
     * boss/strategy notes.
     */
    private static JTextArea locationAccessNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getAccessNote().trim(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-access-note");
        return note;
    }

    /**
     * The antifire "note when unowned" nudge (DT-B11 / PD-3): the advisor sets it only when the monster
     * is draconic and the player owns no antifire; this only renders it, mirroring {@link #strategyNote}.
     * Primary text - real supply advice, like the strategy/safespot notes.
     */
    private static JTextArea antifireNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getAntifireNote().trim(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-antifire-note");
        return note;
    }

    /**
     * The prayer/survival advisory (WD-3 / ADR-0020 #1): the advisor composes it from the resolved
     * monster's offence; this only renders it, mirroring {@link #antifireNote}. Primary text - real
     * survival advice (which prayer, max hit, poison/venom), never a combat-maths change (NG-4).
     */
    private static JTextArea survivalNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getSurvivalNote().trim(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-survival-note");
        return note;
    }

    /**
     * The unlock-gated gear guard note (WA-12 / ADR-0018 #9b): the advisor composes it from the
     * curated item-&gt;reward map + the player-state seam; this only renders it, mirroring
     * {@link #antifireNote}. Primary text - real "buy this unlock" advice, never blocked-red (the
     * loadout itself is untouched, NG-4).
     */
    private static JTextArea unlockGuardNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getUnlockGuardNote().trim(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-unlock-guard-note");
        return note;
    }

    /**
     * The "what to unlock next" hint (WA-13 / ADR-0018 #9d), composed by the advisor's
     * {@code UnlockAdvisor} from the player's points + ownership and the rewards catalogue.
     * {@code text/secondary} - a lever ("spend your points here"), never blocking advice.
     */
    private static JTextArea unlockHintNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getUnlockHint().trim(), SlayerTheme.TEXT_SECONDARY);
        note.setName("loadout-unlock-hint");
        return note;
    }

    private static JLabel caption(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(SlayerTheme.TYPE_CAPTION);
        label.setForeground(SlayerTheme.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JPanel tagRow(SlayerLocation location)
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, SlayerTheme.SPACE_2, SlayerTheme.SPACE_1));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        if (location.isMulti())
        {
            panel.add(new Tag("multi"));
        }
        if (location.isCannonEffective())
        {
            panel.add(new Tag("cannon"));
        }
        if (location.isBurst())
        {
            panel.add(new Tag("burst"));
        }
        if (location.isKonarLockable())
        {
            panel.add(new Tag("konar"));
        }
        return panel;
    }

    private static JPanel gearCostRow(long totalGearCost)
    {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(SlayerTheme.SURFACE_CARD);
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel key = new JLabel("Gear cost");
        key.setFont(SlayerTheme.TYPE_CAPTION);
        key.setForeground(SlayerTheme.TEXT_SECONDARY);
        key.setPreferredSize(new Dimension(SlayerTheme.KEY_COL_WIDTH, key.getPreferredSize().height));
        key.setMaximumSize(new Dimension(SlayerTheme.KEY_COL_WIDTH, SlayerTheme.ROW_HEIGHT));
        row.add(key);
        row.add(Box.createHorizontalStrut(SlayerTheme.SPACE_4));

        row.add(new com.danieljglover.allinslayer.ui.components.PriceLabel(totalGearCost));
        row.add(Box.createHorizontalGlue());

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, SlayerTheme.ROW_HEIGHT));
        return row;
    }

    private void run(Runnable callback)
    {
        if (callback != null)
        {
            callback.run();
        }
    }

    private void runWith(Consumer<String> callback, String value)
    {
        if (callback != null)
        {
            callback.accept(value);
        }
    }

    private static String itemName(SlayerPanelState state, int itemId)
    {
        String name = state.getItemNames().get(itemId);
        return name == null ? "#" + itemId : name;
    }

    private static Integer price(SlayerPanelState state, int itemId)
    {
        return state.getItemPrices().get(itemId);
    }

    private static String requiredName(SlayerPanelState state)
    {
        TaskData task = state.getTask();
        Integer id = task.getRequiredItemId();
        if (id != null && state.getItemNames().get(id) != null)
        {
            return state.getItemNames().get(id);
        }
        if (task.getRequiredItemName() != null)
        {
            return task.getRequiredItemName();
        }
        return id == null ? "Required item" : "#" + id;
    }

    /**
     * Ownership state of the task's required item (B1 / FR-7). The authoritative signal is
     * {@code state.getRequiredItemOwned()}; when the plugin resolved it we trust it directly
     * (OWNED/BLOCKED). When it is unknown ({@code null}) we stay neutral (OWNED: primary text, no tag)
     * rather than asserting the item is missing, which would alarm a player who actually owns it. The
     * recommended-upgrades concept is gone (ADR-0005), so there is no {@code missingUpgrades} fallback.
     */
    private static LoadoutItemRow.State requiredState(SlayerPanelState state)
    {
        TaskData task = state.getTask();
        if (task == null || task.getRequiredItemId() == null)
        {
            return null;
        }
        Boolean owned = state.getRequiredItemOwned();
        if (owned != null)
        {
            return owned ? LoadoutItemRow.State.OWNED : LoadoutItemRow.State.BLOCKED;
        }
        // No ownership signal: neutral (primary, no tag), never a false "missing" red.
        return LoadoutItemRow.State.OWNED;
    }

    private static String weaknessText(TaskData task)
    {
        if (task.getWeakness() == null || task.getWeakness().getStyle() == null)
        {
            return null;
        }
        String weakness = String.valueOf(task.getWeakness().getStyle());
        if (task.getWeakness().getElement() != null)
        {
            weakness += " (" + task.getWeakness().getElement() + ")";
        }
        return weakness;
    }

    private static String methodText(SlayerPanelState state)
    {
        if (state.getRecommendation() != null && state.getRecommendation().getMethod() != null)
        {
            return state.getRecommendation().getMethod();
        }
        TaskData task = state.getTask();
        return task == null || task.getRecommendedMethod() == null ? "" : task.getRecommendedMethod();
    }

    /** Shared look for the variant + location combos (left-aligned, full width, card surface). */
    private static void styleCombo(JComboBox<String> combo)
    {
        combo.setAlignmentX(LEFT_ALIGNMENT);
        combo.setBackground(SlayerTheme.SURFACE_CARD);
        combo.setForeground(SlayerTheme.TEXT_PRIMARY);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, combo.getPreferredSize().height));
    }

    private static List<MonsterVariant> variantsOf(SlayerPanelState state)
    {
        if (state.getTask() == null || state.getTask().getVariants() == null)
        {
            return Collections.emptyList();
        }
        return state.getTask().getVariants();
    }

    /**
     * Whether the Monster step (step 2) is complete, unlocking Location and Loadout. A task with at
     * most one variant auto-resolves (there is nothing to choose); a multi-variant task resolves only
     * once {@code selectedVariantName} is set - by the player's pick or the plugin's boss-varbit seed
     * (MV-B8) - never by the silent deterministic default, which the player has not confirmed.
     */
    private static boolean variantResolved(SlayerPanelState state)
    {
        return variantsOf(state).size() <= 1 || state.getSelectedVariantName() != null;
    }

    private static List<String> variantLabels(List<MonsterVariant> variants)
    {
        List<String> labels = new ArrayList<>(variants.size());
        for (MonsterVariant v : variants)
        {
            labels.add(variantLabel(v));
        }
        return labels;
    }

    /** Display label for a variant: its name, with a {@code " (Boss)"} suffix for a boss (FR-7). */
    private static String variantLabel(MonsterVariant variant)
    {
        return variant.isBoss() ? variant.getName() + " (Boss)" : variant.getName();
    }

    private static String variantNameForLabel(List<MonsterVariant> variants, String label)
    {
        for (MonsterVariant v : variants)
        {
            if (variantLabel(v).equals(label))
            {
                return v.getName();
            }
        }
        return null;
    }

    /** The label to pre-select: the resolved selected/default variant's label, or null if none. */
    private static String selectedVariantLabel(SlayerPanelState state, List<MonsterVariant> variants)
    {
        MonsterVariant variant = MonsterVariant.resolve(state.getTask(), state.getSelectedVariantName());
        return variant == null ? null : variantLabel(variant);
    }

    /** The variant's recommended (default) combat style, via the engine's {@link MonsterProfile} seam. */
    private static CombatStyle recommendedStyle(SlayerPanelState state)
    {
        TaskData task = state.getTask();
        if (task == null)
        {
            return null;
        }
        MonsterVariant variant = MonsterVariant.resolve(task, state.getSelectedVariantName());
        MonsterProfile profile = variant == null
            ? MonsterProfile.fromTask(task)
            : MonsterProfile.fromVariant(task, variant);
        return profile.recommendedStyle();
    }

    /** The effective method the engine geared for: the user's pick, else the recommended style. */
    private static CombatStyle effectiveMethod(SlayerPanelState state)
    {
        return state.getSelectedMethod() != null ? state.getSelectedMethod() : recommendedStyle(state);
    }

    // ---- master-context helpers (WA-11, ADR-0018 #8/#9) -------------------------------------------

    /** The task's assigning masterIds ({@code task.assignedBy}); empty when absent (FR-6 sentinel). */
    private static List<String> assigningMasters(TaskData task)
    {
        if (task == null || task.getAssignedBy() == null)
        {
            return Collections.emptyList();
        }
        return task.getAssignedBy();
    }

    /**
     * The master the context renders for: the threaded selection when it assigns this task, else the
     * first assigning master (the deterministic default, mirroring the location fallback); null for a
     * master-less task (no context to show).
     */
    private static String effectiveMaster(SlayerPanelState state)
    {
        List<String> masters = assigningMasters(state.getTask());
        if (masters.isEmpty())
        {
            return null;
        }
        String selected = state.getSelectedMaster();
        return selected != null && masters.contains(selected) ? selected : masters.get(0);
    }

    /**
     * Display name for a masterId: the plugin-resolved name when threaded, else the slug with its
     * first letter capitalised - a degraded meta resource still renders honestly, never blank.
     */
    private static String masterDisplayName(String masterId, Map<String, String> masterNames)
    {
        String name = masterNames == null ? null : masterNames.get(masterId);
        if (name != null && !name.trim().isEmpty())
        {
            return name;
        }
        return masterId.isEmpty()
            ? masterId
            : Character.toUpperCase(masterId.charAt(0)) + masterId.substring(1);
    }

    /** Reverse of {@link #masterDisplayName}: the masterId whose label matches, or null. */
    private static String masterIdForLabel(List<String> masterIds, Map<String, String> masterNames,
        String label)
    {
        for (String masterId : masterIds)
        {
            if (masterDisplayName(masterId, masterNames).equals(label))
            {
                return masterId;
            }
        }
        return null;
    }

    /** "A-B" for a two-value range, "A" when degenerate, null when absent - never a dangling dash. */
    private static String amountRangeText(int[] range)
    {
        if (range == null || range.length == 0)
        {
            return null;
        }
        int low = range[0];
        int high = range.length > 1 ? range[1] : low;
        return low == high ? String.valueOf(low) : low + "-" + high;
    }

    /**
     * The task's single EXTENSION-typed unlock (the WA-7 build gate guarantees exactly one on every
     * task with {@code extendedAmount}); null when the task has none (defensive - real data cannot).
     */
    private static TaskUnlock extensionUnlock(TaskData task)
    {
        if (task.getUnlocks() == null)
        {
            return null;
        }
        for (TaskUnlock unlock : task.getUnlocks())
        {
            if (unlock != null && unlock.getType() == UnlockType.EXTENSION)
            {
                return unlock;
            }
        }
        return null;
    }

    /**
     * The master-context line (WA-11 / ADR-0018 #5): "<Master> gives A-B; extended X-Y with
     * <EXTENSION unlock> (N pts)." Clauses are omitted cleanly when their data is absent; null when
     * the task carries no per-master amounts at all for this master (nothing honest to say).
     */
    private static String masterContextText(TaskData task, String masterId,
        Map<String, String> masterNames)
    {
        String amounts = amountRangeText(
            task.getAmountByMaster() == null ? null : task.getAmountByMaster().get(masterId));
        String extended = amountRangeText(
            task.getExtendedAmount() == null ? null : task.getExtendedAmount().get(masterId));
        if (amounts == null && extended == null)
        {
            return null;
        }

        StringBuilder text = new StringBuilder(masterDisplayName(masterId, masterNames));
        if (amounts != null)
        {
            text.append(" gives ").append(amounts);
        }
        if (extended != null)
        {
            text.append(amounts != null ? "; extended " : " extended ").append(extended);
            TaskUnlock unlock = extensionUnlock(task);
            if (unlock != null)
            {
                text.append(" with ")
                    .append(unlock.getName() != null ? unlock.getName() : unlock.getUnlockId());
                if (unlock.getPointsCost() != null)
                {
                    text.append(" (").append(unlock.getPointsCost()).append(" pts)");
                }
            }
        }
        text.append('.');
        return text.toString();
    }

    /**
     * The other assigning masters' amounts, compact (PD-E: amounts render for ALL masters): "Also:
     * Nieve 120-185." Null when the effective master is the only assigner.
     */
    private static String otherMastersText(TaskData task, String effectiveMaster,
        Map<String, String> masterNames)
    {
        StringBuilder entries = new StringBuilder();
        for (String masterId : assigningMasters(task))
        {
            if (masterId == null || masterId.equals(effectiveMaster))
            {
                continue;
            }
            if (entries.length() > 0)
            {
                entries.append(", ");
            }
            entries.append(masterDisplayName(masterId, masterNames));
            String amounts = amountRangeText(
                task.getAmountByMaster() == null ? null : task.getAmountByMaster().get(masterId));
            if (amounts != null)
            {
                entries.append(' ').append(amounts);
            }
        }
        return entries.length() == 0 ? null : "Also: " + entries + ".";
    }

    /**
     * The location-dropdown items (DT-FE2 / ADR-0017 #6): the task's locations, filtered to the
     * resolved variant's per-variant subset. {@code MonsterVariant.locationNames} (compiler-derived,
     * DT-B5) is a subset of the task's location names; null/empty is the fallback sentinel = "all task
     * locations apply" (FR-6, byte-identical to today for every unlinked or boss variant). We iterate
     * the task locations (so order + the SlayerLocation flag source stay task-authoritative) and keep
     * those named by the variant. If nothing matched (a display-string mismatch, plan R2), we fall back
     * to all task locations rather than show an empty dropdown.
     */
    private static List<String> locationNames(SlayerPanelState state)
    {
        List<String> all = new ArrayList<>();
        if (state.getTask() == null || state.getTask().getLocations() == null)
        {
            return all;
        }
        for (SlayerLocation location : state.getTask().getLocations())
        {
            all.add(location.getName());
        }

        MonsterVariant variant = MonsterVariant.resolve(state.getTask(), state.getSelectedVariantName());
        if (variant == null || variant.getLocationNames() == null
            || variant.getLocationNames().isEmpty())
        {
            return all;
        }
        List<String> subset = new ArrayList<>();
        for (String name : all)
        {
            if (matchesIgnoreCase(variant.getLocationNames(), name))
            {
                subset.add(name);
            }
        }
        return subset.isEmpty() ? all : subset;
    }

    /**
     * Membership test over the SHARED trimmed/case-insensitive rule
     * ({@link MonsterVariant#locationNameMatches}, WB-2 / D6) - the same match the advisor's
     * candidate set applies, so dropdown and loadout can never disagree on display-string drift.
     */
    private static boolean matchesIgnoreCase(List<String> names, String candidate)
    {
        for (String name : names)
        {
            if (MonsterVariant.locationNameMatches(name, candidate))
            {
                return true;
            }
        }
        return false;
    }

    private static String selectedLocationName(SlayerPanelState state)
    {
        String selected = state.getSelectedLocationName();
        if (selected != null && findLocation(state, selected) != null)
        {
            return selected;
        }
        SlayerLocation recommended = recommendedLocation(state);
        if (recommended != null)
        {
            return recommended.getName();
        }
        List<String> names = locationNames(state);
        return names.isEmpty() ? null : names.get(0);
    }

    private static SlayerLocation recommendedLocation(SlayerPanelState state)
    {
        Recommendation rec = state.getRecommendation();
        if (rec != null)
        {
            if (rec.getRecommendedLocation() != null)
            {
                return rec.getRecommendedLocation();
            }
            if (rec.getLocation() != null)
            {
                return rec.getLocation();
            }
        }
        return firstLocation(state);
    }

    private static SlayerLocation firstLocation(SlayerPanelState state)
    {
        if (state.getTask() == null
            || state.getTask().getLocations() == null
            || state.getTask().getLocations().isEmpty())
        {
            return null;
        }
        return state.getTask().getLocations().get(0);
    }

    private static SlayerLocation findLocation(SlayerPanelState state, String name)
    {
        if (state.getTask() == null || state.getTask().getLocations() == null)
        {
            return null;
        }
        for (SlayerLocation location : state.getTask().getLocations())
        {
            if (location.getName().equals(name))
            {
                return location;
            }
        }
        return null;
    }

    private static String locationReason(SlayerPanelState state)
    {
        if (state.getRecommendation() != null && state.getRecommendation().getLocationReason() != null)
        {
            return state.getRecommendation().getLocationReason();
        }
        return "";
    }

    private static String prettySlot(EquipmentSlot slot)
    {
        if (slot == null)
        {
            return "Item";
        }
        String text = slot.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

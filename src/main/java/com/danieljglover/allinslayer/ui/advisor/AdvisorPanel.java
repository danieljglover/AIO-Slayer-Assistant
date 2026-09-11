package com.danieljglover.allinslayer.ui.advisor;

import com.danieljglover.allinslayer.loadout.advisor.RequirementEvaluator;
import com.danieljglover.allinslayer.loadout.advisor.DepartureEvaluator;
import com.danieljglover.allinslayer.model.advisor.DepartureCheck;
import com.danieljglover.allinslayer.model.advisor.ChargeObservation;
import com.danieljglover.allinslayer.loadout.advisor.SpellPlanner;
import com.danieljglover.allinslayer.integration.advisor.ShortestPathBridge;
import com.danieljglover.allinslayer.integration.advisor.BankSetupFilter;
import com.danieljglover.allinslayer.loadout.advisor.RequirementEvaluator.Assessment;
import com.danieljglover.allinslayer.model.advisor.AccountProgress.State;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.RecommendationGoal;
import com.danieljglover.allinslayer.model.advisor.ReturnDestination;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Setup;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Evidence;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.ItemOption;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.ItemDefinition;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Location;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Master;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Method;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Monster;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Supply;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Task;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.RouteDestination;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk.ItemLoss;
import com.danieljglover.allinslayer.model.advisor.WildernessRisk.Scenario;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.OverlayLayout;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/** Native, passive advisor. Public setters and callbacks are used on the Swing EDT. */
public final class AdvisorPanel extends PluginPanel
{
    private static final Color PAGE = new Color(30, 30, 30);
    private static final Color CARD = new Color(40, 40, 40);
    private static final Color FIELD = new Color(48, 48, 48);
    private static final Color TEXT = new Color(225, 225, 225);
    private static final Color MUTED = new Color(170, 170, 170);
    private static final Color ACCENT = new Color(225, 181, 92);
    private static final Color GOOD = new Color(135, 193, 139);
    private static final Color WARNING = new Color(234, 161, 112);
    private static final String[] EQUIPMENT_CELLS = {
        "", "HEAD", "", "CAPE", "AMULET", "AMMO", "WEAPON", "BODY", "SHIELD",
        "", "LEGS", "", "HANDS", "FEET", "RING"
    };

    private final JLabel activeTitle = label("No active task", TEXT, true);
    private final JTextArea activeDetail = prose("Log in to detect your Slayer assignment.", MUTED);
    private final JTextArea status = prose("Loading bundled Slayer catalogue...", MUTED);
    private final JToggleButton activeTab = new JToggleButton("Active task", true);
    private final JToggleButton browseTab = new JToggleButton("Catalogue");
    private final JToggleButton setupTab = new JToggleButton("Setup", true);
    private final JToggleButton checksTab = new JToggleButton("Checks");
    private final JToggleButton guideTab = new JToggleButton("Guide");
    private final JTextField search = new JTextField();
    private final JComboBox<Option> masters = combo();
    private final JComboBox<Option> tasks = combo();
    private final JComboBox<Option> monsters = combo();
    private final JComboBox<Option> locations = combo();
    private final JComboBox<Option> methods = combo();
    private final JComboBox<RecommendationGoal> goal = new JComboBox<>(RecommendationGoal.values());
    private final JComboBox<ReturnDestination> returnDestination = new JComboBox<>(ReturnDestination.values());
    private final JCheckBox wilderness = check("Allow Wilderness");
    private final JCheckBox groups = check("Allow group methods");
    private final JPanel browseControls = column(PAGE);
    private final JPanel masterControl = field("Slayer master", masters);
    private final JPanel results = column(PAGE);
    private final JScrollPane scroll;
    private final Set<String> expanded = new HashSet<>();
    private final Context active = new Context();
    private final Context browse = new Context();
    private Consumer<Selection> onSelection;
    private Runnable onRefresh;
    private Runnable onExport;
    private final JButton copySetup = button("Copy setup", () -> run(onExport));
    private Consumer<Boolean> onBankFilter;
    private BankSetupFilter.State bankFilterState = new BankSetupFilter.State(false, false, "Open your bank to filter the selected setup.");
    private final JButton filterBank = button("Filter bank", () -> {
        if (onBankFilter != null) { onBankFilter.accept(!bankFilterState.isActive()); }
    });
    private final JButton departure = button("Ready to leave check", this::showDeparture);
    private Runnable onClearConfirmations;
    private Consumer<String> onConfirmRequirement;
    private Consumer<RouteDestination> onRoute;
    private Runnable onClearRoute;
    private ShortestPathBridge.State routeState = new ShortestPathBridge.State(false, false, false, "", "", ShortestPathBridge.Phase.NONE);
    private SlayerCatalogue catalogue;
    private ItemManager itemManager;
    private boolean browsing;
    private boolean updating;
    private String activeTaskId;
    private String activeAssignmentIdentity;
    private int activeRemaining;
    private String activeStatus = "Log in to detect your Slayer assignment.";
    private Set<String> activeAllowedMonsters = new LinkedHashSet<>();

    public AdvisorPanel()
    {
        super(false);
        setLayout(new BorderLayout());
        setBackground(PAGE);
        JPanel heading = column(PAGE);
        heading.setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 8));
        heading.add(label("ALL-IN SLAYER", ACCENT, true));
        heading.add(gap(3));
        heading.add(activeTitle);
        heading.add(gap(6));
        JPanel tabs = new JPanel(new GridLayout(1, 2, 4, 0));
        tabs.setOpaque(false);
        tabs.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup tabGroup = new ButtonGroup();
        tabGroup.add(activeTab);
        tabGroup.add(browseTab);
        styleButton(activeTab);
        styleButton(browseTab);
        tabs.add(activeTab);
        tabs.add(browseTab);
        heading.add(tabs);
        heading.add(gap(6));

        search.setBackground(FIELD);
        search.setForeground(TEXT);
        search.setCaretColor(TEXT);
        search.setFont(FontManager.getDefaultFont().deriveFont(12f));
        search.setAlignmentX(LEFT_ALIGNMENT);
        search.setPreferredSize(new Dimension(185, 26));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        search.setToolTipText("Search master, task, monster or location names");
        search.getAccessibleContext().setAccessibleName("Search the Slayer catalogue");
        browseControls.add(label("Search catalogue", MUTED, false));
        browseControls.add(search);
        browseControls.add(gap(4));
        tasks.getAccessibleContext().setAccessibleName("Assignment");
        browseControls.add(tasks);
        browseControls.add(gap(4));
        browseControls.setVisible(false);
        heading.add(browseControls);
        heading.add(field("Monster", monsters));
        styleCombo(goal);
        styleCombo(returnDestination);
        masterControl.setVisible(false);
        JPanel overrides = column(PAGE);
        overrides.add(masterControl);
        overrides.add(field("Location", locations));
        overrides.add(field("Method", methods));
        overrides.add(field("Rank setups for", goal));
        overrides.add(field("After task", returnDestination));
        overrides.add(wilderness);
        overrides.add(groups);
        // Keep the existing selector instances mounted so live refreshes retain focus and filters.
        heading.add(disclosure("controls", "Change setup", () -> overrides));
        heading.add(gap(6));
        JPanel views = new JPanel(new GridLayout(1, 3, 3, 0));
        views.setOpaque(false);
        views.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup viewGroup = new ButtonGroup();
        for (JToggleButton tab : Arrays.asList(setupTab, checksTab, guideTab))
        {
            styleButton(tab);
            viewGroup.add(tab);
            views.add(tab);
        }
        heading.add(views);
        add(heading, BorderLayout.NORTH);

        JPanel footer = column(PAGE);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        departure.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        departure.setEnabled(false);
        footer.add(departure);
        footer.add(gap(4));
        filterBank.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        footer.add(filterBank);
        footer.add(gap(4));
        JPanel actions = new JPanel(new GridLayout(1, 2, 4, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.add(button("Refresh", () -> run(onRefresh)));
        actions.add(copySetup);
        footer.add(actions);
        status.setVisible(false);
        footer.add(status);
        add(footer, BorderLayout.SOUTH);

        ViewportPanel viewport = new ViewportPanel();
        results.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        viewport.add(results, BorderLayout.NORTH);
        scroll = new JScrollPane(viewport, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        add(scroll, BorderLayout.CENTER);

        activeTab.addActionListener(event -> changeTab(false));
        browseTab.addActionListener(event -> changeTab(true));
        setupTab.addActionListener(event -> changeView(View.SETUP));
        checksTab.addActionListener(event -> changeView(View.CHECKS));
        guideTab.addActionListener(event -> changeView(View.GUIDE));
        masters.addActionListener(event -> select("master"));
        tasks.addActionListener(event -> select("task"));
        monsters.addActionListener(event -> select("monster"));
        locations.addActionListener(event -> select("location"));
        methods.addActionListener(event -> select("method"));
        goal.addActionListener(event -> preferenceChanged());
        returnDestination.addActionListener(event -> preferenceChanged());
        wilderness.addActionListener(event -> preferenceChanged());
        groups.addActionListener(event -> preferenceChanged());
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent event) { searchChanged(); }
            @Override public void removeUpdate(DocumentEvent event) { searchChanged(); }
            @Override public void changedUpdate(DocumentEvent event) { searchChanged(); }
        });
        updateBankFilterButton();
        showEmpty("Choose Catalogue to explore every bundled assignment, or log in with an active task.");
    }

    public void setCatalogue(SlayerCatalogue catalogue)
    {
        this.catalogue = catalogue;
        populateSelectors();
        updateActiveHeader();
        showStatus(catalogue == null ? "Slayer data unavailable." : "");
        renderCurrent();
    }

    /** Only requests asynchronous sprites. Item definitions and game state remain client-thread work. */
    public void setItemManager(ItemManager itemManager)
    {
        this.itemManager = itemManager;
        renderCurrent();
    }

    public void setOnSelection(Consumer<Selection> callback) { onSelection = callback; }
    public void setOnRefresh(Runnable callback) { onRefresh = callback; }
    public void setOnExport(Runnable callback) { onExport = callback; }
    public void setOnBankFilter(Consumer<Boolean> callback) { onBankFilter = callback; }
    public void setOnClearConfirmations(Runnable callback) { onClearConfirmations = callback; }
    public void setOnConfirmRequirement(Consumer<String> callback) { onConfirmRequirement = callback; }
    public void setOnRoute(Consumer<RouteDestination> callback) { onRoute = callback; }
    public void setOnClearRoute(Runnable callback) { onClearRoute = callback; }

    public void setBankFilterState(BankSetupFilter.State state)
    {
        bankFilterState = state;
        updateBankFilterButton();
    }

    private void updateBankFilterButton()
    {
        Context context = current();
        Setup setup = context.result == null ? null : displayedSetup(context);
        filterBank.setText(bankFilterState.isActive() ? "Clear bank filter" : "Filter bank");
        filterBank.setEnabled(bankFilterState.isActive() || (setup != null && bankFilterState.isAvailable()));
        filterBank.setToolTipText(bankFilterState.getMessage());
        filterBank.setForeground(bankFilterState.isActive() ? ACCENT : TEXT);
    }

    public void setRouteState(ShortestPathBridge.State state)
    {
        if (!Objects.equals(routeState, state))
        {
            routeState = state;
            renderCurrent();
        }
    }

    public void setPreferences(RecommendationGoal preferredGoal, boolean allowWilderness, boolean allowGroups,
        ReturnDestination preferredReturn)
    {
        RecommendationGoal nextGoal = preferredGoal == null ? RecommendationGoal.SLAYER_XP : preferredGoal;
        boolean changed = goal.getSelectedItem() != nextGoal || wilderness.isSelected() != allowWilderness
            || groups.isSelected() != allowGroups || returnDestination.getSelectedItem() != preferredReturn;
        updating = true;
        try
        {
            goal.setSelectedItem(nextGoal);
            returnDestination.setSelectedItem(preferredReturn);
            wilderness.setSelected(allowWilderness);
            groups.setSelected(allowGroups);
        }
        finally
        {
            updating = false;
        }
        if (changed) { preferenceChanged(); }
    }

    public void setActiveTask(String taskId, int remaining, String text)
    {
        if (!Objects.equals(activeTaskId, taskId))
        {
            active.reset();
            active.taskId = taskId;
        }
        activeTaskId = taskId;
        activeRemaining = remaining;
        activeStatus = text == null ? "" : text;
        updateActiveHeader();
        if (!browsing)
        {
            populateSelectors();
        }
    }

    /** Resets saved active filters when an assignment changes while the catalogue is visible. */
    public void setActiveAssignmentIdentity(String identity)
    {
        if (!Objects.equals(activeAssignmentIdentity, identity))
        {
            activeAssignmentIdentity = identity;
            active.masterId = null;
            active.clearOverrides();
            active.request = null;
            active.result = null;
        }
    }

    public void showStatus(String text)
    {
        String message = text == null ? "" : text;
        status.setToolTipText(message);
        status.setText(message.startsWith("Setup copied.") ? "Setup copied to clipboard." : message);
        status.setVisible(!message.isEmpty());
    }

    public void setActiveAllowedMonsters(Set<String> monsterIds)
    {
        activeAllowedMonsters = new LinkedHashSet<>(monsterIds);
    }

    public void invalidatePreparation()
    {
        active.preparationCurrent = false;
        browse.preparationCurrent = false;
        renderCurrent();
    }

    public void render(RecommendationRequest request, RecommendationResult result)
    {
        if (request == null)
        {
            return;
        }
        if (request.isActiveTask())
        {
            setActiveTask(request.getTaskId(), request.getRemaining(), activeStatus);
            // Active assignments can constrain the monster (for example a boss assignment).
            // The plugin publishes only its latest revision, so its active context is authoritative.
            active.masterId = request.getMasterId();
            active.monsterId = request.getMonsterId();
            active.locationId = request.getLocationId();
            active.methodId = request.getMethodId();
            if (!browsing) { populateSelectors(); }
        }
        Context target = request.isActiveTask() ? active : browse;
        // A queued computation may arrive after a user changes context. Do not move their selection.
        if (!Objects.equals(target.taskId, request.getTaskId())
            || !Objects.equals(target.monsterId, request.getMonsterId())
            || !Objects.equals(target.locationId, request.getLocationId())
            || !Objects.equals(target.methodId, request.getMethodId())
            || (!request.isActiveTask() && !Objects.equals(target.masterId, request.getMasterId()))
            || request.getGoal() != goal.getSelectedItem()
            || request.getReturnDestination() != returnDestination.getSelectedItem()
            || request.isAllowWilderness() != wilderness.isSelected()
            || request.isAllowGroups() != groups.isSelected())
        {
            return;
        }
        target.request = request;
        target.result = result;
        target.preparationCurrent = true;
        if (target == current())
        {
            renderCurrent();
        }
    }

    private void changeTab(boolean browseSelected)
    {
        if (updating || browsing == browseSelected)
        {
            return;
        }
        browsing = browseSelected;
        browseControls.setVisible(browsing);
        masterControl.setVisible(browsing);
        showStatus("");
        populateSelectors();
        renderCurrent();
        emitSelection();
    }

    private void changeView(View view)
    {
        Context context = current();
        if (context.view == view) { return; }
        context.view = view;
        renderCurrent();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private void showDeparture()
    {
        changeView(View.CHECKS);
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private void preferenceChanged()
    {
        if (!updating)
        {
            for (Context context : Arrays.asList(active, browse))
            {
                Location selectedLocation = catalogue == null ? null : catalogue.getLocations().get(context.locationId);
                if (!wilderness.isSelected() && selectedLocation != null && selectedLocation.isWilderness())
                {
                    context.locationId = null;
                    context.methodId = null;
                }
                context.request = null;
                context.result = null;
            }
            populateSelectors();
            renderCurrent();
            emitSelection();
        }
    }

    private void searchChanged()
    {
        if (updating || !browsing)
        {
            return;
        }
        String previousTask = browse.taskId;
        String previousMaster = browse.masterId;
        populateSelectors();
        if (!Objects.equals(previousTask, browse.taskId) || !Objects.equals(previousMaster, browse.masterId))
        {
            browse.request = null;
            browse.result = null;
            renderCurrent();
            emitSelection();
        }
    }

    private void select(String field)
    {
        if (updating)
        {
            return;
        }
        Context context = current();
        showStatus("");
        if ("master".equals(field))
        {
            context.masterId = selected(masters);
            context.taskId = null;
            context.clearOverrides();
        }
        else if ("task".equals(field))
        {
            context.taskId = selected(tasks);
            context.clearOverrides();
        }
        else if ("monster".equals(field))
        {
            context.monsterId = selected(monsters);
            context.locationId = null;
            context.methodId = null;
        }
        else if ("location".equals(field))
        {
            context.locationId = selected(locations);
            context.methodId = null;
        }
        else
        {
            context.methodId = selected(methods);
        }
        context.request = null;
        context.result = null;
        populateSelectors();
        renderCurrent();
        emitSelection();
    }

    private void emitSelection()
    {
        if (onSelection != null && !updating)
        {
            Context context = current();
            onSelection.accept(new Selection(browsing, context.taskId, context.masterId,
                context.monsterId, context.locationId, context.methodId,
                (RecommendationGoal) goal.getSelectedItem(), wilderness.isSelected(), groups.isSelected(),
                (ReturnDestination) returnDestination.getSelectedItem()));
        }
    }

    private void populateSelectors()
    {
        if (catalogue == null)
        {
            return;
        }
        updating = true;
        try
        {
            Context context = current();
            if (browsing)
            {
                List<Task> matching = catalogue.getTasks().values().stream()
                    .filter(this::matchesSearch).sorted(Comparator.comparing(Task::getName)).collect(Collectors.toList());
                Set<String> matchingMasters = matching.stream().flatMap(task -> task.getMasterIds().stream())
                    .collect(Collectors.toSet());
                List<Option> masterOptions = catalogue.getMasters().values().stream()
                    .filter(master -> matchingMasters.contains(master.getId()))
                    .map(master -> new Option(master.getId(), master.getName())).collect(Collectors.toList());
                fill(masters, masterOptions, context.masterId, "All masters");
                context.masterId = selected(masters);
                List<Option> taskOptions = matching.stream()
                    .filter(task -> context.masterId == null || task.getMasterIds().contains(context.masterId))
                    .map(task -> new Option(task.getId(), task.getName())).collect(Collectors.toList());
                fill(tasks, taskOptions, context.taskId, null);
                String nextTask = selected(tasks);
                if (!Objects.equals(context.taskId, nextTask))
                {
                    context.taskId = nextTask;
                    context.clearOverrides();
                }
            }
            Task task = catalogue.getTasks().get(context.taskId);
            List<Monster> taskMonsters = task == null ? new ArrayList<>() : task.getMonsterIds().stream()
                .filter(id -> browsing || activeAllowedMonsters.isEmpty() || activeAllowedMonsters.contains(id))
                .map(catalogue.getMonsters()::get).filter(Objects::nonNull).collect(Collectors.toList());
            fill(monsters, taskMonsters.stream().map(monster -> new Option(monster.getId(), monsterLabel(monster, taskMonsters)))
                .collect(Collectors.toList()), context.monsterId, "Automatic");
            context.monsterId = selected(monsters);
            Set<String> locationIds = new LinkedHashSet<>();
            if (task != null && context.monsterId == null && (browsing || activeAllowedMonsters.isEmpty()))
            {
                locationIds.addAll(task.getLocationIds());
            }
            for (Monster monster : taskMonsters)
            {
                if (context.monsterId == null || context.monsterId.equals(monster.getId()))
                {
                    locationIds.addAll(monster.getLocationIds());
                }
            }
            List<Option> locationOptions = locationIds.stream().map(catalogue.getLocations()::get)
                .filter(Objects::nonNull).map(location -> new Option(location.getId(), location.getName()
                    + (location.isWilderness() ? " (Wilderness)" : "")))
                .collect(Collectors.toList());
            fill(locations, locationOptions, context.locationId, "Automatic");
            context.locationId = selected(locations);
            List<Option> methodOptions = methodsFor(task).stream()
                .filter(Method::isSelectable)
                .filter(method -> browsing || activeAllowedMonsters.isEmpty()
                    || method.getMonsterIds().stream().anyMatch(activeAllowedMonsters::contains))
                .filter(method -> context.monsterId == null || method.getMonsterIds().isEmpty()
                    || method.getMonsterIds().contains(context.monsterId))
                .filter(method -> context.locationId == null || method.getLocationIds().isEmpty()
                    || method.getLocationIds().contains(context.locationId))
                .map(method -> new Option(method.getId(), method.getName())).collect(Collectors.toList());
            fill(methods, methodOptions, context.methodId, "Automatic");
            context.methodId = selected(methods);
        }
        finally
        {
            updating = false;
        }
    }

    private String monsterLabel(Monster monster, List<Monster> options)
    {
        List<Monster> sameName = options.stream().filter(other -> other.getName().equals(monster.getName()))
            .collect(Collectors.toList());
        if (sameName.size() < 2) { return monster.getName(); }
        String label = monster.getName() + (monster.getCombatLevel() > 0 ? " (level " + monster.getCombatLevel() + ")" : "");
        if (sameName.stream().filter(other -> other.getCombatLevel() == monster.getCombatLevel()).count() > 1)
        {
            Location location = monster.getLocationIds().stream().map(catalogue.getLocations()::get)
                .filter(Objects::nonNull).findFirst().orElse(null);
            if (location != null) { label += " - " + location.getName(); }
        }
        return label;
    }

    private boolean matchesSearch(Task task)
    {
        String needle = search.getText().trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty())
        {
            return true;
        }
        StringBuilder names = new StringBuilder(task.getName()).append(' ').append(task.getId());
        task.getMasterIds().forEach(id -> {
            Master master = catalogue.getMasters().get(id);
            if (master != null) { names.append(' ').append(master.getName()); }
        });
        Set<String> locationIds = new HashSet<>(task.getLocationIds());
        task.getMonsterIds().forEach(id -> {
            Monster monster = catalogue.getMonsters().get(id);
            if (monster != null)
            {
                names.append(' ').append(monster.getName());
                locationIds.addAll(monster.getLocationIds());
            }
        });
        locationIds.forEach(id -> {
            Location location = catalogue.getLocations().get(id);
            if (location != null) { names.append(' ').append(location.getName()); }
        });
        return names.toString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private void updateActiveHeader()
    {
        Task task = catalogue == null ? null : catalogue.getTasks().get(activeTaskId);
        activeTitle.setText(task == null ? "No active task" : task.getName() + " - " + Math.max(0, activeRemaining) + " left");
        activeTitle.setToolTipText(activeStatus);
        activeDetail.setText((task == null ? "" : Math.max(0, activeRemaining) + " remaining\n") + activeStatus);
    }

    private void renderCurrent()
    {
        updateBankFilterButton();
        departure.setText("Choose a setup");
        departure.setForeground(MUTED);
        departure.setEnabled(false);
        int scrollPosition = scroll == null ? 0 : scroll.getVerticalScrollBar().getValue();
        results.removeAll();
        Context context = current();
        setupTab.setSelected(context.view == View.SETUP);
        checksTab.setSelected(context.view == View.CHECKS);
        guideTab.setSelected(context.view == View.GUIDE);
        checksTab.setText("Checks");
        checksTab.setForeground(TEXT);
        copySetup.setEnabled(false);
        Task task = catalogue == null ? null : catalogue.getTasks().get(context.taskId);
        if (task == null)
        {
            showEmpty(browsing ? "No assignments match this search. Try a monster, master or place name."
                : "No active assignment detected. Open Catalogue to plan any Slayer task.");
            return;
        }
        if (browsing)
        {
            JTextArea preview = prose("On-task preview", ACCENT);
            preview.setToolTipText("Assumes a " + task.getName() + " assignment for Slayer bonuses and task-only access. Your actual assignment is unchanged.");
            results.add(preview);
        }
        if (context.request != null)
        {
            JTextArea bank = prose(compactBankStatus(context.request.getPlayer()), MUTED);
            bank.setToolTipText(bankStatus(context.request.getPlayer()));
            results.add(bank);
        }
        Setup setup = context.result == null ? null : displayedSetup(context);
        List<RequirementCheck> requirements = setup == null ? new ArrayList<>() : selectedRequirements(task, setup, context);
        List<String> issues = setup == null ? new ArrayList<>() : preparationIssues(setup, requirements);
        DepartureCheck ready = setup == null || !context.preparationCurrent ? null
            : DepartureEvaluator.assess(catalogue, context.request, setup, requirements.stream()
                .collect(Collectors.toMap(check -> check.requirement, check -> check.assessment,
                    (first, second) -> first, LinkedHashMap::new)));
        departure.setEnabled(setup != null);
        departure.setText(ready == null ? "Checking preparation..." : ready.summary(browsing));
        departure.setForeground(ready == null ? MUTED : ready.isReady() ? (browsing ? ACCENT : GOOD) : WARNING);
        departure.setToolTipText("Compare worn equipment, packed supplies, pouch contents and charges. Click to review remaining actions.");
        int checkCount = issues.size() + (int) requirements.stream().filter(check -> check.assessment.getState() != State.MET).count();
        if (ready != null) { checkCount = Math.max(checkCount, ready.getEntries().size()); }
        checksTab.setText(checkCount == 0 ? "Checks" : "Checks " + checkCount);
        checksTab.setForeground(checkCount == 0 ? TEXT : WARNING);
        checksTab.setToolTipText(checkCount + " unresolved checks for the selected setup");
        copySetup.setEnabled(setup != null && setup.isFeasible());
        copySetup.setToolTipText(setup != null && setup.isFeasible()
            ? "Copy this setup for Inventory Setups" : "Resolve the setup's blockers before copying");

        if (context.view == View.GUIDE)
        {
            results.add(guideView(task, setup, context));
        }
        else if (context.result == null)
        {
            results.add(prose("Updating setup...", MUTED));
        }
        else if (setup == null)
        {
            results.add(prose("No matching setup. Change the filters or open Guide.", WARNING));
            appendLines(results, context.result.getNotices().stream().filter(notice -> !routineNotice(notice))
                .collect(Collectors.toList()), WARNING);
        }
        else if (context.view == View.CHECKS)
        {
            results.add(departureView(ready, context));
            results.add(gap(6));
            results.add(checksView(task, setup, context, requirements, issues));
        }
        else
        {
            results.add(setupCard(setup, context));
        }
        results.revalidate();
        results.repaint();
        if (scroll != null)
        {
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scrollPosition));
        }
    }

    private Setup displayedSetup(Context context)
    {
        Setup best = context.result.best();
        return best == null && !context.result.getSetups().isEmpty() ? context.result.getSetups().get(0) : best;
    }

    private JPanel departureView(DepartureCheck ready, Context context)
    {
        JPanel body = card();
        body.add(label("Ready to leave", ACCENT, true));
        if (ready == null)
        {
            body.add(prose("Checking the latest equipment and backpack...", MUTED));
            return body;
        }
        body.add(prose(ready.summary(context.request.isTaskPreview()), ready.isReady() ? GOOD : WARNING, true));
        if (context.request.isTaskPreview())
        {
            body.add(prose("Checks packing for this preview. Your actual assignment is unchanged.", MUTED));
        }
        if (ready.isReady())
        {
            body.add(prose("Selected equipment is worn and supplies are packed. No outstanding preparation checks.", TEXT));
        }
        for (DepartureCheck.Entry entry : ready.getEntries())
        {
            body.add(gap(4));
            body.add(disclosure("departure:" + setupKey(displayedSetup(context)) + ":" + entry.getTitle(),
                entry.getTitle(), () -> prose(entry.getDetail(), entry.isVerification() ? WARNING : TEXT)));
        }
        body.add(gap(6));
        body.add(disclosure("stored-resources:" + setupKey(displayedSetup(context)), "Charges & looting bag",
            () -> storedResources(displayedSetup(context), context.request)));
        return body;
    }

    private JPanel storedResources(Setup setup, RecommendationRequest request)
    {
        JPanel body = column(CARD);
        body.add(prose("Weapon Charges balances are read automatically when enabled. For a fresh observation, use the carried item's Check option in game.", MUTED));
        List<Choice> choices = new ArrayList<>(setup.getEquipment().values());
        choices.addAll(setup.getInventory());
        Set<Integer> shown = new HashSet<>();
        PlayerSnapshot player = request.getPlayer();
        for (Choice choice : choices)
        {
            if (!shown.add(choice.getItemId())) { continue; }
            ChargeObservation charge = player.getPreparation().getCharges().get(choice.getItemId());
            com.danieljglover.allinslayer.model.advisor.ItemValue value = player.getItemValues().get(choice.getItemId());
            if (charge != null || value != null && value.isChargesUnknown())
            {
                body.add(gap(5));
                body.add(prose(choice.getName(), TEXT, true));
                body.add(prose(charge == null ? "Balance unknown. Check this item in game."
                    : DepartureEvaluator.chargeDetails(charge, player), charge != null && charge.isObserved() ? GOOD : WARNING));
                com.danieljglover.allinslayer.model.advisor.DeathRule rule = catalogue.getDeathRules().get(choice.getItemId());
                if (rule != null && "ETHER_WEAPON".equals(rule.getContentsType()))
                {
                    body.add(prose(String.format(Locale.ENGLISH,
                        "Alert above %,d usable charges (%,d total ether). Change the limit in AIO settings.",
                        request.getWildernessChargeLimit(), request.getWildernessChargeLimit() + 1000), MUTED));
                }
            }
        }
        if (choices.stream().anyMatch(c -> catalogue.getPreparation().getLootingBagIds().contains(c.getItemId())))
        {
            body.add(gap(5));
            boolean known = player.getDeathContext().isLootingBagContentsKnown();
            body.add(prose("Looting bag", TEXT, true));
            body.add(prose(player.getDeathContext().getLootingBagDetail(), known ? TEXT : WARNING));
            if (known)
            {
                Map<Integer, Integer> contents = player.getDeathContext().getLootingBagContents();
                body.add(prose(contents.isEmpty() ? "Empty - confirmed" : contents.size() + " item types to empty at a bank",
                    contents.isEmpty() ? GOOD : WARNING));
                contents.forEach((id, count) -> {
                    com.danieljglover.allinslayer.model.advisor.ItemValue item = player.getItemValues().get(id);
                    body.add(prose((item == null ? "Item " + id : item.getName()) + " x"
                        + String.format(Locale.ENGLISH, "%,d", count), TEXT));
                });
            }
            else { body.add(prose("Use View/Check on the bag in game to refresh. Closing it keeps the observation.", MUTED)); }
        }
        return body;
    }

    private JPanel setupCard(Setup setup, Context context)
    {
        JPanel card = card();
        Monster monster = catalogue.getMonsters().get(setup.getMonsterId());
        Location location = locationFor(catalogue.getTasks().get(context.taskId), setup.getLocationId());
        Method method = catalogue.getMethods().get(setup.getMethodId());
        if (monster != null) { card.add(prose(monster.getName(), ACCENT, true)); }
        if (location != null)
        {
            JPanel destinationRow = new JPanel(new BorderLayout(4, 0));
            destinationRow.setAlignmentX(LEFT_ALIGNMENT);
            destinationRow.setOpaque(false);
            destinationRow.add(prose(location.getName() + (location.isWilderness()
                && !location.getName().toLowerCase(Locale.ROOT).contains("wilderness") ? " / Wilderness" : ""),
                location.isWilderness() ? WARNING : MUTED), BorderLayout.CENTER);
            RouteDestination destination = routeDestination(setup);
            boolean showing = destination != null && routeState.isActive() && destination.getId().equals(routeState.getRouteId());
            JButton route = button(showing ? "Clear" : "Route", () -> {
                if (showing) { run(onClearRoute); }
                else if (onRoute != null && destination != null) { onRoute.accept(destination); }
            });
            route.setMargin(new java.awt.Insets(2, 4, 2, 4));
            String unavailable = routeUnavailable(setup, context, destination);
            route.setEnabled(showing || unavailable == null);
            route.setToolTipText(showing ? "Clear this Slayer route" : unavailable != null
                ? unavailable : "Show route to " + destination.getLabel()
                    + ("ENTRANCE".equals(destination.getArrival()) ? " (entrance)" : "") + " with Shortest Path");
            JPanel routeControl = new JPanel(new java.awt.GridBagLayout());
            routeControl.setOpaque(false);
            routeControl.add(route);
            destinationRow.add(routeControl, BorderLayout.EAST);
            card.add(destinationRow);
            if (showing)
            {
                card.add(prose(routeState.getMessage() + ("ENTRANCE".equals(destination.getArrival()) ? " (entrance)" : ""),
                    ACCENT));
            }
        }
        if (method != null)
        {
            String name = method.getName();
            Task task = catalogue.getTasks().get(context.taskId);
            for (String prefix : Arrays.asList(monster == null ? "" : monster.getName(), task.getName()))
            {
                if (!prefix.isEmpty() && name.regionMatches(true, 0, prefix + " - ", 0, prefix.length() + 3))
                {
                    name = name.substring(prefix.length() + 3);
                    break;
                }
            }
            JTextArea methodName = prose(name, MUTED);
            methodName.setToolTipText(setup.getSummary());
            card.add(methodName);
        }
        card.add(actionRow(!setup.isFeasible() ? "Blocked - review checks" : requiresEtherPreparation(setup, context.request)
            ? "Can prepare - check charges" : "Available with known items",
            setup.isFeasible() && !requiresEtherPreparation(setup, context.request) ? GOOD : WARNING, () -> changeView(View.CHECKS)));
        if (setup.getWildernessRisk() != null)
        {
            appendCompactRisk(card, setup.getWildernessRisk());
        }
        if (!setup.isFeasible() && !setup.getBlockers().isEmpty())
        {
            card.add(prose(shortBlocker(setup.getBlockers().get(0)), WARNING));
        }
        long notices = context.result.getNotices().stream().filter(notice -> !routineNotice(notice)).count();
        if (notices > 0)
        {
            card.add(actionRow(notices + " planning notice" + (notices == 1 ? "" : "s") + " - review", WARNING,
                () -> {
                    expanded.add("planning:" + context.taskId);
                    changeView(View.CHECKS);
                }));
        }
        card.add(gap(6));
        JPanel equipmentHeading = new JPanel(new BorderLayout());
        equipmentHeading.setOpaque(false);
        equipmentHeading.setAlignmentX(LEFT_ALIGNMENT);
        equipmentHeading.add(label("Equipment", TEXT, true), BorderLayout.WEST);
        JButton why = button("Why?", () -> {
            expanded.add("reason:" + setupKey(setup));
            changeView(View.GUIDE);
        });
        why.setMargin(new java.awt.Insets(1, 4, 1, 4));
        Choice head = setup.getEquipment().get("HEAD");
        why.setToolTipText(head != null && !empty(head.getExplanation()) ? head.getExplanation() : "Why this equipment was selected");
        equipmentHeading.add(why, BorderLayout.EAST);
        card.add(equipmentHeading);
        card.add(gap(3));
        JPanel equipment = new JPanel(new GridLayout(5, 3, 3, 3));
        equipment.setOpaque(false);
        for (String slot : EQUIPMENT_CELLS)
        {
            equipment.add(slot.isEmpty() ? blankCell() : itemCell(setup.getEquipment().get(slot), slot, context.request));
        }
        card.add(equipment);
        card.add(gap(8));
        JLabel inventoryHeading = label("Inventory", TEXT, true);
        card.add(inventoryHeading);
        card.add(gap(3));
        JPanel inventory = new JPanel(new GridLayout(0, 4, 3, 3));
        inventory.setOpaque(false);
        int placed = 0;
        int reserved = 0;
        for (Choice choice : setup.getInventory())
        {
            if ("RESERVED BOOST".equals(choice.getSlot()) || "RESERVED FOOD".equals(choice.getSlot()))
            {
                String kind = "RESERVED FOOD".equals(choice.getSlot()) ? "Food" : "Boost";
                for (int index = 0; index < choice.getQuantity() && placed < 28; index++)
                {
                    JPanel cell = emptyInventoryCell();
                    cell.setLayout(new BorderLayout());
                    JLabel caption = new JLabel(kind, SwingConstants.CENTER);
                    caption.setForeground(MUTED);
                    caption.setFont(caption.getFont().deriveFont(10f));
                    String tooltip = "Reserved for " + choice.getName() + "; see Checks for missing supplies.";
                    cell.setToolTipText(tooltip);
                    caption.setToolTipText(tooltip);
                    cell.getAccessibleContext().setAccessibleName(tooltip);
                    cell.add(caption);
                    inventory.add(cell);
                    placed++;
                    reserved++;
                }
                continue;
            }
            if (isPouchContent(choice) || choice.getQuantity() <= 0 || choice.getItemId() <= 0) { continue; }
            PlayerSnapshot.ItemStats stats = context.request.getPlayer().getItems().get(choice.getItemId());
            boolean stackable = stats != null && stats.isStackable();
            int copies = stackable ? 1 : choice.getQuantity();
            for (int index = 0; index < copies && placed < 28; index++)
            {
                Choice tile = stackable ? choice : new Choice(choice.getItemId(), 1,
                    index < choice.getCarried() ? 1 : 0,
                    index >= choice.getCarried() && index < choice.getCarried() + choice.getWithdraw() ? 1 : 0,
                    0, choice.getSlot(), choice.getName(), choice.getOrigin(), choice.isRequired(), choice.getExplanation());
                inventory.add(itemCell(tile, "", context.request));
                placed++;
            }
        }
        for (int index = placed; index < 28; index++) { inventory.add(emptyInventoryCell()); }
        inventoryHeading.setText("Inventory - " + (placed - reserved) + "/28 slots");
        if (reserved > 0) { inventoryHeading.setToolTipText(reserved + " additional slots reserved for missing supplies."); }
        card.add(inventory);
        appendPouchConfiguration(card, setup, false);
        List<Choice> all = new ArrayList<>(setup.getEquipment().values());
        all.addAll(setup.getInventory());
        int withdraw = all.stream().mapToInt(Choice::getWithdraw).sum();
        int missing = all.stream().mapToInt(Choice::getMissing).sum();
        card.add(gap(6));
        JTextArea packing = prose("Withdraw " + withdraw + "  |  Missing " + missing, missing > 0 ? WARNING : GOOD, true);
        packing.setToolTipText("Additional bank withdrawals for equipment, loose inventory and rune pouch contents.");
        card.add(packing);
        card.add(disclosure("items:" + setupKey(setup), "Packing list", () -> itemDetails(setup)));
        List<String> tripNotes = tripPreparationNotes(setup);
        if (!tripNotes.isEmpty())
        {
            card.add(gap(3));
            card.add(disclosure("trip:" + setupKey(setup), "Trip preparation", () -> {
                JPanel preparation = column(CARD);
                appendLines(preparation, tripNotes, TEXT);
                return preparation;
            }));
        }
        return card;
    }

    private static boolean isPouchContent(Choice choice)
    {
        return "RUNE POUCH".equals(choice.getSlot());
    }

    private static List<String> tripPreparationNotes(Setup setup)
    {
        return setup.getExplanations().stream().filter(note -> note.startsWith("Return:")
            || note.startsWith("Escape:") || note.startsWith("Looting bag:")
            || note.startsWith("Rune pouch:") || note.startsWith("Casting:") || note.startsWith("Boosts:")
            || note.startsWith("Food:"))
            .distinct().collect(Collectors.toList());
    }

    private void appendPouchConfiguration(JPanel body, Setup setup, boolean detailed)
    {
        List<Choice> runes = setup.getInventory().stream().filter(AdvisorPanel::isPouchContent)
            .collect(Collectors.toList());
        if (runes.isEmpty()) { return; }
        body.add(gap(6));
        body.add(label("Rune pouch configuration", ACCENT, true));
        for (Choice rune : runes)
        {
            if (detailed)
            {
                appendChoice(body, rune, null);
            }
            else
            {
                JTextArea line = prose(rune.getName() + " x" + String.format(Locale.ENGLISH, "%,d", rune.getQuantity())
                    + (rune.getMissing() > 0 ? " (missing " + rune.getMissing() + ")" : ""),
                    rune.getMissing() > 0 ? WARNING : TEXT);
                line.setToolTipText("Available with you: " + rune.getCarried() + "; withdraw: " + rune.getWithdraw()
                    + "; missing: " + rune.getMissing()
                    + (empty(rune.getExplanation()) ? "" : ". " + rune.getExplanation()));
                body.add(line);
            }
        }
        body.add(prose("Load these exact amounts; unload extra runes.", MUTED));
    }

    private JPanel checksView(Task task, Setup setup, Context context, List<RequirementCheck> requirements, List<String> issues)
    {
        JPanel body = card();
        body.add(label("Preparation checks", ACCENT, true));
        if (!issues.isEmpty())
        {
            appendLines(body, issues, WARNING);
            body.add(gap(6));
        }
        body.add(label("Account requirements", TEXT, true));
        body.add(requirementConfirmations(requirements, context));
        body.add(disclosure("poh-return-shortcuts", "Optional house shortcuts", () -> {
            JPanel shortcuts = column(CARD);
            shortcuts.add(prose("Confirm only facilities in your own house. These unlock extra return routes; they are optional for this setup.", MUTED));
            Set<String> imported = new LinkedHashSet<>(context.request.getPlayer().getAccountProgress()
                .getConfiguredRequirements().values());
            if (!imported.isEmpty())
            {
                shortcuts.add(prose("Using saved Shortest Path settings. These describe configured facilities, not a scan of your house.", ACCENT));
                appendLines(shortcuts, new ArrayList<>(imported), TEXT);
            }
            RequirementEvaluator evaluator = new RequirementEvaluator(context.request);
            Set<String> houseRequirements = new LinkedHashSet<>();
            catalogue.getPreparation().getTeleports().values().forEach(teleport ->
                teleport.getRequirements().stream().filter(requirement -> requirement.startsWith("POH has ")
                    || "Own a player-owned house".equals(requirement)).forEach(houseRequirements::add));
            List<RequirementCheck> checks = houseRequirements.stream()
                .map(requirement -> new RequirementCheck(requirement, evaluator.assess(requirement)))
                .collect(Collectors.toList());
            shortcuts.add(requirementConfirmations(checks, context));
            return shortcuts;
        }));
        if (setup.getWildernessRisk() != null)
        {
            body.add(gap(8));
            appendRiskSummary(body, setup.getWildernessRisk(), false);
            body.add(disclosure("risk:" + setupKey(setup), "Death scenarios & item outcomes", () -> riskDetails(setup)));
        }
        body.add(gap(6));
        body.add(disclosure("planning:" + task.getId(), "Planning notes", () -> {
            JPanel notes = column(CARD);
            notes.add(prose(bankStatus(context.request.getPlayer()), MUTED));
            if (!browsing && !empty(activeStatus)) { notes.add(prose(activeStatus, TEXT)); }
            appendLines(notes, context.result.getNotices(), TEXT);
            return notes;
        }));
        return body;
    }

    private JPanel guideView(Task task, Setup setup, Context context)
    {
        JPanel body = card();
        if (setup != null)
        {
            Method method = catalogue.getMethods().get(setup.getMethodId());
            body.add(prose(method == null ? setup.getTitle() : method.getName(), ACCENT, true));
            body.add(disclosure("reason:" + setupKey(setup), "Why this setup", () -> {
                JPanel reasons = column(CARD);
                Choice head = setup.getEquipment().get("HEAD");
                if (head != null && !empty(head.getExplanation())) { reasons.add(prose(head.getExplanation(), ACCENT)); }
                appendLines(reasons, setup.getExplanations(), TEXT);
                return reasons;
            }));
            body.add(gap(6));
            body.add(prose(setup.getSummary(), TEXT));
            appendLines(body, method == null ? setup.getGuidance() : method.getGuidance(), TEXT);
            if (method != null) { appendLines(body, method.getRisks(), WARNING); }
            Location location = locationFor(task, setup.getLocationId());
            if (location != null)
            {
                body.add(gap(5));
                body.add(disclosure("travel:" + setupKey(setup), "Getting there & access", () -> {
                    JPanel travel = column(CARD);
                    travel.add(prose(location.getName(), ACCENT, true));
                    appendLines(travel, location.getNotes(), TEXT);
                    appendLines(travel, location.getRequirements(), MUTED);
                    appendSupplies(travel, "Travel options", location.getTravel());
                    appendRouteDetails(travel, setup, context);
                    return travel;
                }));
            }
            body.add(gap(5));
            body.add(disclosure("ranked:" + context.taskId,
                "Other setups (" + context.result.getSetups().size() + ")", () -> rankedSetups(context)));
            body.add(gap(5));
            body.add(disclosure("evidence:" + setupKey(setup), "Wiki sources", () -> evidenceDetails(setup.getEvidence())));
        }
        else
        {
            body.add(prose(task.getName(), ACCENT, true));
            body.add(prose("Choose a setup to show its strategy here.", MUTED));
        }
        body.add(gap(6));
        body.add(disclosure("task:" + task.getId(), "Assignment & locations", () -> taskDetails(task, context)));
        List<Method> allMethods = methodsFor(task);
        body.add(gap(5));
        body.add(disclosure("methods:" + task.getId(), "All methods (" + allMethods.size() + ")", () -> {
            JPanel methodsBody = column(CARD);
            for (Method method : allMethods)
            {
                methodsBody.add(disclosure("method:" + method.getId(), method.getName(), () -> methodDetails(method)));
                methodsBody.add(gap(5));
            }
            return methodsBody;
        }));
        return body;
    }

    private static List<String> preparationIssues(Setup setup, List<RequirementCheck> requirements)
    {
        Set<String> issues = new LinkedHashSet<>();
        for (String blocker : setup.getBlockers())
        {
            boolean accountRequirement = blocker.startsWith("Confirm requirement: ")
                || blocker.startsWith("Requirement not met: ") || blocker.startsWith("Cannot verify requirement: ");
            if (!accountRequirement || requirements.stream().noneMatch(check -> blocker.contains(check.requirement)))
            {
                issues.add(blocker);
            }
        }
        List<Choice> choices = new ArrayList<>(setup.getEquipment().values());
        choices.addAll(setup.getInventory());
        for (Choice choice : choices)
        {
            if (choice.getMissing() > 0 && issues.stream().noneMatch(issue -> issue.contains(choice.getName())))
            {
                issues.add("Missing: " + choice.getName() + " x" + choice.getMissing());
            }
        }
        return new ArrayList<>(issues);
    }

    private RouteDestination routeDestination(Setup setup)
    {
        return catalogue.getRouteDestinations().values().stream()
            .filter(destination -> destination.getLocationId().equals(setup.getLocationId())
                && destination.getMonsterIds().contains(setup.getMonsterId()))
            .findFirst().orElse(null);
    }

    private String routeUnavailable(Setup setup, Context context, RouteDestination destination)
    {
        if (destination == null) { return "No verified route destination for this monster at this location."; }
        if (!routeState.isAvailable()) { return "Install and enable Shortest Path in RuneLite's Plugin Hub."; }
        if (context.request == null || !context.request.getPlayer().isLoggedIn()) { return "Log in to start a route."; }
        Location location = locationFor(catalogue.getTasks().get(context.taskId), setup.getLocationId());
        if (!context.request.isAllowWilderness() && (location.isWilderness() || setup.getWildernessRisk() != null))
        {
            return "Enable Allow Wilderness before routing to this location.";
        }
        return null;
    }

    private void appendRouteDetails(JPanel body, Setup setup, Context context)
    {
        body.add(gap(6));
        body.add(label("Shortest Path", ACCENT, true));
        RouteDestination destination = routeDestination(setup);
        String unavailable = routeUnavailable(setup, context, destination);
        if (unavailable != null) { body.add(prose(unavailable, WARNING)); }
        if (destination == null) { return; }
        body.add(prose(destination.getLabel() + ("ENTRANCE".equals(destination.getArrival()) ? " - entrance destination" : " - monster destination"), TEXT));
        if (!empty(destination.getNote())) { body.add(prose(destination.getNote(), TEXT)); }
        body.add(prose("Routes consider regular unlocked transport types and owned teleport items. Shortest Path checks access and applies its travel costs, spending limit and configured house teleports. You move and use teleports yourself.", MUTED));
        body.add(prose(routeState.isBankSeen()
            ? "Banked teleport items can be included, with a bank stop to collect them. Reopen the bank after changing its contents."
            : "Using carried items. Open your bank while Shortest Path is enabled to include banked teleport items.", MUTED));
        body.add(prose("Route sent confirms the handoff only. Check Shortest Path for completion or an unreachable destination.", MUTED));
        body.add(prose("Travel items suggested by Shortest Path are separate from the combat packing list and its loss estimate. Check any added items before entering the Wilderness.", MUTED));
        if (!empty(routeState.getMessage())) { body.add(prose(routeState.getMessage(), MUTED)); }
        body.add(disclosure("route-source:" + destination.getId(), "Destination sources", () -> evidenceDetails(destination.getEvidence())));
    }

    private void appendCompactRisk(JPanel body, WildernessRisk risk)
    {
        Scenario baseline = risk.getPlanned().isEmpty() ? null : risk.getPlanned().get(0);
        boolean known = baseline != null && baseline.isComplete();
        boolean withinBudget = known && baseline.getPermanentLosses() == 0 && baseline.getTotalRisk() <= risk.getBudget();
        String amount = baseline == null ? "unavailable" : compactGp(baseline.getTotalRisk()) + (known ? "" : "+ / incomplete");
        JButton row = actionRow("Planned risk: " + amount + " / " + compactGp(risk.getBudget()),
            withinBudget ? TEXT : WARNING, () -> changeView(View.CHECKS));
        row.setToolTipText(baseline == null ? "Open Checks for missing risk information"
            : riskAmount(baseline) + "; budget " + gp(risk.getBudget()) + "; " + baseline.getName()
                + "; " + baseline.getProtectedSlots() + " protected items");
        body.add(row);
        if (!known) { body.add(prose("Loss and permanent loss unconfirmed.", WARNING)); }
        if (baseline != null && baseline.getPermanentLosses() > 0)
        {
            body.add(prose("Permanent item loss: " + baseline.getPermanentLosses(), WARNING, true));
        }
        if (baseline != null && baseline.getTotalRisk() > risk.getBudget())
        {
            body.add(prose("Over the loss budget.", WARNING, true));
        }
        if (risk.getPlannedEtherCharges() > 0)
        {
            body.add(prose("Planned loss assumes " + risk.getPlannedEtherCharges() + " usable charges per weapon.", MUTED));
        }
    }

    private static String compactGp(long value)
    {
        if (value < 0) { return "unknown"; }
        if (value >= 1_000_000) { return String.format(Locale.ENGLISH, "%.2fm", value / 1_000_000.0); }
        if (value >= 1_000) { return String.format(Locale.ENGLISH, "%.1fk", value / 1_000.0); }
        return value + " gp";
    }

    private static String shortBlocker(String blocker)
    {
        if (blocker.startsWith("Wilderness loss cannot")) { return "Wilderness loss is incomplete."; }
        if (blocker.startsWith("This setup risks permanent")) { return "This setup risks permanent item loss."; }
        if (blocker.startsWith("Estimated Wilderness loss exceeds")) { return "Planned loss exceeds your budget."; }
        if (blocker.startsWith("Requirement not met: ") || blocker.startsWith("Cannot verify requirement: "))
        {
            int detail = blocker.lastIndexOf(" (");
            return detail < 0 ? blocker : blocker.substring(0, detail);
        }
        return blocker;
    }

    private static boolean routineNotice(String notice)
    {
        return notice.startsWith("Log in to verify") || notice.startsWith("Open your bank")
            || notice.startsWith("Bank ownership uses") || notice.startsWith("On-task preview:")
            || notice.startsWith("Goals use relative") || notice.startsWith("No setup is currently verified")
            || notice.startsWith("Wilderness locations are excluded");
    }

    private static JButton actionRow(String text, Color color, Runnable action)
    {
        JButton row = button("<html><body style='width:145px'>" + html(text) + " &rsaquo;</body></html>", action);
        row.setHorizontalAlignment(SwingConstants.LEFT);
        row.setForeground(color);
        row.setBackground(CARD);
        row.setMargin(new java.awt.Insets(3, 0, 3, 0));
        return row;
    }

    private JPanel rankedSetups(Context context)
    {
        JPanel body = column(CARD);
        body.add(prose("Ranked for " + context.request.getGoal() + ". Blocked options remain visible for planning.", MUTED));
        int index = 1;
        for (Setup setup : context.result.getSetups())
        {
            body.add(prose(index++ + ". " + setup.getTitle(), TEXT, true));
            Location location = locationFor(catalogue.getTasks().get(context.taskId), setup.getLocationId());
            if (location != null) { body.add(prose(location.getName(), MUTED)); }
            body.add(prose(readinessText(setup, context.request, true),
                setup.isFeasible() && !requiresEtherPreparation(setup, context.request) ? GOOD : WARNING));
            if (setup.getWildernessRisk() != null)
            {
                appendRiskSummary(body, setup.getWildernessRisk(), true);
            }
            appendLines(body, setup.getBlockers(), WARNING);
            body.add(button("Select this setup", () -> {
                context.monsterId = setup.getMonsterId();
                context.locationId = setup.getLocationId();
                context.methodId = setup.getMethodId();
                context.request = null;
                context.result = null;
                context.view = View.SETUP;
                populateSelectors();
                renderCurrent();
                emitSelection();
            }));
            body.add(gap(8));
        }
        return body;
    }

    private boolean requiresEtherPreparation(Setup setup, RecommendationRequest request)
    {
        if (setup.getWildernessRisk() == null || setup.getWildernessRisk().getPlannedEtherCharges() <= 0) { return false; }
        if (request == null) { return true; }
        List<Choice> choices = new ArrayList<>(setup.getEquipment().values());
        choices.addAll(setup.getInventory());
        return choices.stream().anyMatch(choice -> {
            com.danieljglover.allinslayer.model.advisor.DeathRule rule = catalogue.getDeathRules().get(choice.getItemId());
            if (rule == null || !"ETHER_WEAPON".equals(rule.getContentsType())) { return false; }
            ChargeObservation charge = request.getPlayer().getPreparation().getCharges().get(choice.getItemId());
            return charge == null || !charge.isObserved() || charge.getCharges() <= 0
                || charge.getCharges() > request.getWildernessChargeLimit();
        });
    }

    private String readinessText(Setup setup, RecommendationRequest request, boolean compact)
    {
        if (request != null && request.isTaskPreview())
        {
            if (!setup.isFeasible()) { return compact ? "On-task preview - blocked" : "On-task preview - blocked; review requirements"; }
            if (requiresEtherPreparation(setup, request)) { return "On-task preview - can prepare; set weapon charges"; }
            return compact ? "On-task preview - available" : "On-task preview - available with known items";
        }
        if (!setup.isFeasible()) { return compact ? "Blocked" : "Blocked - review requirements"; }
        if (requiresEtherPreparation(setup, request)) { return "Can prepare - set weapon charges"; }
        return compact ? "Available" : "Available with your known items";
    }

    private void appendEtherAssumption(JPanel body, WildernessRisk risk)
    {
        int charges = risk.getPlannedEtherCharges();
        if (charges <= 0) { return; }
        body.add(prose(String.format(Locale.ENGLISH,
            "Planned loss assumes %,d usable charges (%,d total ether) in each Wilderness weapon. Carried loss uses fresh Check observations when available. More ether increases loss.",
            charges, (long) charges + 1000), WARNING, true));
        body.add(prose("Change Planned ether charges in the plugin's RuneLite settings. This preparation target does not verify actual carried charges.", MUTED));
    }

    private void appendRiskSummary(JPanel body, WildernessRisk risk, boolean compact)
    {
        body.add(prose("Wilderness loss estimate", ACCENT, true));
        appendEtherAssumption(body, risk);
        if (risk.getPlanned().isEmpty())
        {
            body.add(prose("Planned loss unavailable. Budget and permanent loss are unconfirmed.", WARNING));
        }
        else
        {
            Scenario baseline = risk.getPlanned().get(0);
            boolean withinBudget = baseline.isComplete() && baseline.getPermanentLosses() == 0
                && baseline.getTotalRisk() <= risk.getBudget();
            body.add(prose("Planned: " + riskAmount(baseline), withinBudget ? GOOD : WARNING, true));
            String status = baseline.getTotalRisk() > risk.getBudget() ? "exceeded"
                : withinBudget ? "within budget" : "not confirmed";
            body.add(prose("Budget " + gp(risk.getBudget()) + " - " + status,
                withinBudget ? GOOD : WARNING));
            body.add(prose(permanentLossText(baseline),
                baseline.isComplete() && baseline.getPermanentLosses() == 0 ? MUTED : WARNING));
            if (!compact)
            {
                body.add(prose(baseline.getName() + "; "
                    + baseline.getProtectedSlots() + " protected item slots.", MUTED));
            }
        }
        if (!compact)
        {
            body.add(prose("Carried comparison at selected destination/route: " + risk.getArea(), MUTED));
            if (risk.getCarried().isEmpty())
            {
                body.add(prose("Actual carried loss unavailable.", WARNING));
            }
            else
            {
                Scenario carried = risk.getCarried().get(0);
                body.add(prose(riskAmount(carried) + ". " + permanentLossText(carried),
                    carried.isComplete() && carried.getPermanentLosses() == 0 ? TEXT : WARNING));
            }
        }
    }

    private JPanel riskDetails(Setup setup)
    {
        WildernessRisk risk = setup.getWildernessRisk();
        JPanel body = column(CARD);
        body.add(prose("Selected destination/route: " + risk.getArea(), ACCENT, true));
        appendEtherAssumption(body, risk);
        body.add(prose("The first scenario sets the budget baseline: normally three items unskulled or zero skulled. Protect Item's fourth item (first when skulled) is conditional and is not assumed in the budget.", MUTED));
        body.add(prose("High-risk worlds and account restrictions can reduce protection. These estimates use the selected destination or route, not an assertion of your current location.", MUTED));
        body.add(prose("Budget: " + gp(risk.getBudget()) + ". Change the Wilderness loss budget in the plugin's RuneLite configuration.", TEXT));
        appendLines(body, risk.getNotes(), WARNING);
        appendRiskScenarios(body, "Planned equipment, supplies & switches", risk.getPlanned(),
            "risk-planned:" + setupKey(setup));
        appendRiskScenarios(body, "Actual carried items at selected destination/route", risk.getCarried(),
            "risk-carried:" + setupKey(setup));
        return body;
    }

    private void appendRiskScenarios(JPanel body, String title, List<Scenario> scenarios, String key)
    {
        body.add(gap(6));
        body.add(prose(title, ACCENT, true));
        if (scenarios.isEmpty())
        {
            body.add(prose("Scenario data unavailable; loss is unconfirmed.", WARNING));
            return;
        }
        for (int index = 0; index < scenarios.size(); index++)
        {
            Scenario scenario = scenarios.get(index);
            body.add(prose(scenario.getName() + (index == 0 ? " - baseline" : ""), TEXT, true));
            body.add(prose("Total: " + riskAmount(scenario),
                scenario.isComplete() && scenario.getPermanentLosses() == 0 ? TEXT : WARNING));
            body.add(prose("Lost value " + gp(scenario.getLostValue()) + " / repairs "
                + gp(scenario.getRepairCost()) + " / entry fee " + gp(scenario.getEntryFee()), MUTED));
            body.add(prose("Protected item slots: " + scenario.getProtectedSlots() + ". "
                + permanentLossText(scenario),
                scenario.isComplete() && scenario.getPermanentLosses() == 0 ? MUTED : WARNING));
            if (!scenario.isComplete())
            {
                body.add(prose("Incomplete: some protection rules, prices or contents remain unverified. This amount is not a verified total.", WARNING));
            }
            appendLines(body, scenario.getWarnings(), WARNING);
            body.add(disclosure(key + ":" + index, "Item outcomes (" + scenario.getItems().size() + ")",
                () -> riskItemDetails(scenario)));
            body.add(gap(6));
        }
    }

    private JPanel riskItemDetails(Scenario scenario)
    {
        JPanel body = column(CARD);
        if (scenario.getItems().isEmpty())
        {
            body.add(prose("No item outcomes were captured for this scenario.", MUTED));
        }
        for (ItemLoss item : scenario.getItems())
        {
            Color color = item.isKnown() && !item.isPermanent() ? TEXT : WARNING;
            body.add(prose(item.getName() + " x" + item.getQuantity(), color, true));
            body.add(prose("Protected quantity: " + item.getProtectedQuantity() + " of "
                + item.getQuantity() + " / " + pretty(item.getOutcome()), color));
            body.add(prose("Protection valuation: " + gp(item.getProtectionValue()), MUTED));
            body.add(prose("Lost value: " + gp(item.getLostValue()) + " / repair cost: "
                + gp(item.getRepairCost()), color));
            if (item.isPermanent()) { body.add(prose("Permanent loss", WARNING, true)); }
            if (!item.isKnown())
            {
                body.add(prose("Unknown behavior or valuation; incomplete estimate, not a verified total.", WARNING));
            }
            if (!empty(item.getDetail())) { body.add(prose(item.getDetail(), color)); }
            body.add(gap(5));
        }
        return body;
    }

    private static String riskAmount(Scenario scenario)
    {
        return "Estimated " + gp(scenario.getTotalRisk())
            + (scenario.isComplete() ? "" : " (incomplete)");
    }

    private static String permanentLossText(Scenario scenario)
    {
        if (!scenario.isComplete())
        {
            return scenario.getPermanentLosses() == 0 ? "Permanent loss: unconfirmed"
                : "Possible permanent losses: " + scenario.getPermanentLosses();
        }
        return "Permanent losses: " + scenario.getPermanentLosses();
    }

    private static String gp(long value)
    {
        return value < 0 ? "unknown" : String.format(Locale.ENGLISH, "%,d gp", value);
    }

    private JPanel itemDetails(Setup setup)
    {
        JPanel body = column(CARD);
        for (Map.Entry<String, Choice> entry : setup.getEquipment().entrySet())
        {
            appendChoice(body, entry.getValue(), pretty(entry.getKey()));
        }
        List<Choice> inventory = setup.getInventory().stream().filter(choice -> !isPouchContent(choice))
            .collect(Collectors.toList());
        if (!inventory.isEmpty()) { body.add(label("Inventory", ACCENT, true)); }
        for (Choice choice : inventory) { appendChoice(body, choice, null); }
        appendPouchConfiguration(body, setup, true);
        return body;
    }

    private void appendChoice(JPanel body, Choice choice, String slot)
    {
        if ("RESERVED BOOST".equals(choice.getSlot()) || "RESERVED FOOD".equals(choice.getSlot()))
        {
            body.add(prose(choice.getQuantity() + " empty slots reserved for " + choice.getName(), MUTED));
            return;
        }
        if (choice.getItemId() <= 0 && choice.getMissing() == 0)
        {
            body.add(prose((slot == null ? "" : slot + ": ") + choice.getName(), MUTED));
            return;
        }
        body.add(prose((slot == null ? "" : slot + ": ") + choice.getName() + " x" + choice.getQuantity(), TEXT, true));
        body.add(prose(choice.getOrigin() + (choice.isRequired() ? " - required" : ""), ACCENT));
        body.add(prose((isPouchContent(choice) ? "Available with you " : "Carried ") + choice.getCarried()
            + " / withdraw " + choice.getWithdraw()
            + " / missing " + choice.getMissing(), choice.getMissing() > 0 ? WARNING : MUTED));
        if (!empty(choice.getExplanation())) { body.add(prose(choice.getExplanation(), MUTED)); }
        body.add(gap(5));
    }

    private List<RequirementCheck> selectedRequirements(Task task, Setup setup, Context context)
    {
        Set<String> requirements = new LinkedHashSet<>(task.getRequirements());
        addLevelRequirement(requirements, "SLAYER", task.getSlayerLevel());
        Master master = catalogue.getMasters().get(context.request.getMasterId());
        Monster monster = catalogue.getMonsters().get(setup.getMonsterId());
        Location location = locationFor(task, setup.getLocationId());
        Method method = catalogue.getMethods().get(setup.getMethodId());
        if (location != null && location.isTaskOnly() || method != null && method.isTaskOnly())
        {
            requirements.add("Task-only");
        }
        if (master != null && !context.request.isActiveTask())
        {
            requirements.addAll(master.getRequirements());
            addLevelRequirement(requirements, "SLAYER", master.getSlayerLevel());
            addLevelRequirement(requirements, "COMBAT", master.getCombatLevel());
        }
        if (monster != null)
        {
            requirements.addAll(monster.getRequirements());
            addLevelRequirement(requirements, "SLAYER", monster.getSlayerLevel());
        }
        if (location != null) { requirements.addAll(location.getRequirements()); }
        List<Choice> selectedItems = new ArrayList<>(setup.getEquipment().values());
        selectedItems.addAll(setup.getInventory());
        Set<Integer> selectedIds = selectedItems.stream().map(Choice::getItemId).collect(Collectors.toSet());
        if (method != null)
        {
            SpellPlanner spells = new SpellPlanner(catalogue.getPreparation(), context.request);
            method.getRequirements().stream().filter(requirement -> !spells.managesRequirement(requirement))
                .forEach(requirements::add);
            method.getEquipment().values().forEach(options -> options.stream()
                .filter(option -> option.getItemIds().stream().anyMatch(selectedIds::contains))
                .forEach(option -> requirements.addAll(option.getRequirements())));
        }
        for (Choice choice : selectedItems)
        {
            // Portable jewellery use is checked by the travel plan; its wear/recharge
            // requirements must not appear as unmet combat equipment requirements.
            if ("TRAVEL".equals(choice.getSlot()) || isPouchContent(choice)) { continue; }
            ItemDefinition definition = catalogue.getItems().get(choice.getItemId());
            if (definition != null)
            {
                requirements.addAll(definition.getRequirements());
                definition.getLevels().forEach((skill, level) -> addLevelRequirement(requirements, skill, level));
            }
        }
        List<String> requirementMessages = new ArrayList<>(setup.getBlockers());
        for (String blocker : requirementMessages)
        {
            String prefix = "Confirm requirement: ";
            if (blocker.startsWith(prefix)) { requirements.add(blocker.substring(prefix.length())); }
        }
        RequirementEvaluator evaluator = new RequirementEvaluator(context.request, setup.getMonsterId());
        Map<String, RequirementCheck> checks = new LinkedHashMap<>();
        for (String requirement : requirements)
        {
            Assessment assessment = evaluator.assess(requirement);
            if (assessment.getState() != State.MET || assessment.isAssumed())
            {
                checks.putIfAbsent(com.danieljglover.allinslayer.model.advisor.AccountProgress.key(requirement),
                    new RequirementCheck(requirement, assessment));
            }
        }
        return new ArrayList<>(checks.values());
    }

    private JPanel requirementConfirmations(List<RequirementCheck> requirements, Context context)
    {
        JPanel body = column(CARD);
        int visible = 0;
        for (RequirementCheck check : requirements)
        {
            String requirement = check.requirement;
            Assessment assessment = check.assessment;
            if (assessment.getState() == State.MET)
            {
                if (assessment.isAssumed())
                {
                    body.add(prose(requirement, TEXT));
                    body.add(prose(assessment.getDetail(), ACCENT));
                    body.add(gap(6));
                }
                continue;
            }
            visible++;
            body.add(prose(requirement, TEXT));
            if (assessment.canConfirm())
            {
                body.add(prose(assessment.getDetail(), MUTED));
                JButton confirm = button("Confirm completed", () -> {
                    if (onConfirmRequirement != null)
                    {
                        onConfirmRequirement.accept(RequirementEvaluator.confirmationRequirement(requirement));
                    }
                });
                confirm.setEnabled(context.request.getPlayer().isLoggedIn());
                body.add(confirm);
            }
            else
            {
                body.add(prose(assessment.getState() == State.UNMET ? "Not met" : "Not available", WARNING));
                body.add(prose(assessment.getDetail(), MUTED));
            }
            body.add(gap(6));
        }
        if (visible == 0)
        {
            body.add(prose("No unmet account requirements.", GOOD));
        }
        body.add(disclosure("confirm-help", "Confirmation settings", () -> {
            JPanel settings = column(CARD);
            settings.add(prose("Quests, diaries, levels and supported unlocks update automatically. Confirmed requirements are hidden.", MUTED));
            JButton reset = button("Reset manual confirmations", () -> run(onClearConfirmations));
            reset.setEnabled(context.request.getPlayer().isLoggedIn());
            settings.add(reset);
            return settings;
        }));
        return body;
    }

    private static void addLevelRequirement(Set<String> requirements, String skill, int level)
    {
        if (level > 0) { requirements.add(skill.toUpperCase(Locale.ROOT) + " >= " + level); }
    }

    private JPanel taskDetails(Task task, Context context)
    {
        JPanel body = column(CARD);
        body.add(prose("Slayer level " + task.getSlayerLevel(), TEXT));
        appendLines(body, task.getRequirements(), WARNING);
        appendLines(body, task.getNotes(), TEXT);
        appendSupplies(body, "Required task items", task.getRequiredItems());
        body.add(gap(5));
        body.add(label("Monsters", ACCENT, true));
        for (String monsterId : task.getMonsterIds())
        {
            Monster monster = catalogue.getMonsters().get(monsterId);
            if (monster == null) { continue; }
            body.add(disclosure("monster:" + monsterId, monster.getName(), () -> {
                JPanel detail = column(CARD);
                detail.add(prose("Slayer level " + monster.getSlayerLevel() + (monster.isBoss() ? " / Boss" : ""), TEXT));
                appendLines(detail, monster.getRequirements(), WARNING);
                detail.add(evidenceDetails(monster.getEvidence()));
                return detail;
            }));
            body.add(gap(4));
        }
        body.add(gap(5));
        body.add(label("Slayer masters", ACCENT, true));
        for (String masterId : task.getMasterIds())
        {
            Master master = catalogue.getMasters().get(masterId);
            if (master == null) { continue; }
            body.add(disclosure("master:" + masterId + ":" + task.getId(), master.getName(), () -> {
                JPanel detail = column(CARD);
                detail.add(prose(master.getLocation(), TEXT));
                detail.add(prose("Combat " + master.getCombatLevel() + " / Slayer " + master.getSlayerLevel(), MUTED));
                String amount = task.getAmounts().get(masterId);
                if (amount != null) { detail.add(prose("Assignment amount: " + amount, TEXT)); }
                appendLines(detail, master.getRequirements(), WARNING);
                appendLines(detail, master.getNotes(), TEXT);
                detail.add(evidenceDetails(master.getEvidence()));
                return detail;
            }));
            body.add(gap(4));
        }
        body.add(label("Locations & travel", ACCENT, true));
        Set<String> allLocations = new LinkedHashSet<>(task.getLocationIds());
        for (String monsterId : task.getMonsterIds())
        {
            Monster monster = catalogue.getMonsters().get(monsterId);
            if (monster != null) { allLocations.addAll(monster.getLocationIds()); }
        }
        for (String locationId : allLocations)
        {
            Location location = locationFor(task, locationId);
            if (location == null) { continue; }
            body.add(disclosure("location:" + locationId, location.getName(), () -> {
                JPanel detail = column(CARD);
                List<String> flags = new ArrayList<>();
                if (location.isWilderness()) { flags.add("Wilderness"); }
                if (location.isMulti()) { flags.add("Multi-combat"); }
                if (location.isCannon()) { flags.add("Cannon permitted"); }
                if (location.isBarrage()) { flags.add("Barrage"); }
                if (location.isSafespot()) { flags.add("Safespot"); }
                if (location.isTaskOnly()) { flags.add("Task only"); }
                detail.add(prose(String.join(" / ", flags), MUTED));
                appendLines(detail, location.getRequirements(), WARNING);
                appendLines(detail, location.getNotes(), TEXT);
                appendSupplies(detail, "Travel options", location.getTravel());
                detail.add(evidenceDetails(location.getEvidence()));
                return detail;
            }));
            body.add(gap(4));
        }
        body.add(evidenceDetails(task.getEvidence()));
        return body;
    }

    private JPanel methodDetails(Method method)
    {
        JPanel body = column(CARD);
        body.add(prose(method.getSummary(), TEXT));
        if (!empty(method.getCoverageStatus())) { body.add(prose(method.getCoverageStatus(), MUTED)); }
        body.add(prose(method.getStyle() + (empty(method.getRole()) ? "" : " / " + method.getRole())
            + (method.isGroup() ? " / Group" : "") + (method.isTaskOnly() ? " / Task only" : ""), MUTED));
        if (!method.isSelectable()) { body.add(prose("Reference guidance; not ranked as a complete setup.", MUTED)); }
        if (!empty(method.getRankingReason())) { body.add(prose(method.getRankingReason(), ACCENT)); }
        appendLines(body, method.getRequirements(), WARNING);
        appendLines(body, method.getGuidance(), TEXT);
        appendLines(body, method.getRisks(), WARNING);
        if (!method.getEquipment().isEmpty())
        {
            body.add(gap(5));
            body.add(label("Wiki equipment preference", ACCENT, true));
        }
        for (Map.Entry<String, List<ItemOption>> entry : method.getEquipment().entrySet())
        {
            body.add(prose(pretty(entry.getKey()), TEXT, true));
            int rank = 1;
            for (ItemOption option : entry.getValue())
            {
                body.add(prose(rank++ + ". " + option.getName(), TEXT));
                appendLines(body, option.getRequirements(), MUTED);
                if (!option.getRequires().isEmpty())
                {
                    body.add(prose("Requires companion item(s): " + option.getRequires().stream()
                        .map(group -> group.stream().map(this::knownItemName).collect(Collectors.joining(" or ")))
                        .collect(Collectors.joining("; ")), MUTED));
                }
            }
        }
        appendSupplies(body, "Required items", method.getRequiredItems());
        appendSupplies(body, "Inventory", method.getInventory());
        appendSupplies(body, "Switches", method.getSwitches());
        body.add(evidenceDetails(method.getEvidence()));
        return body;
    }

    private String knownItemName(int itemId)
    {
        RecommendationRequest request = current().request;
        PlayerSnapshot.ItemStats stats = request == null ? null : request.getPlayer().getItems().get(itemId);
        if (stats != null) { return stats.getName(); }
        for (Method method : catalogue.getMethods().values())
        {
            for (List<ItemOption> options : method.getEquipment().values())
            {
                for (ItemOption option : options)
                {
                    if (option.getItemIds().contains(itemId)) { return option.getName(); }
                }
            }
        }
        return "item " + itemId;
    }

    private static void appendSupplies(JPanel body, String title, List<Supply> supplies)
    {
        if (supplies.isEmpty()) { return; }
        body.add(gap(5));
        body.add(label(title, ACCENT, true));
        for (Supply supply : supplies)
        {
            body.add(prose(supply.getName() + " x" + supply.getQuantity() + (supply.isRequired() ? " (required)" : ""), TEXT));
        }
    }

    private JPanel evidenceDetails(List<Evidence> evidence)
    {
        JPanel body = column(CARD);
        if (evidence.isEmpty())
        {
            body.add(prose("No revision evidence recorded for this entry.", WARNING));
            return body;
        }
        Set<String> seen = new HashSet<>();
        for (Evidence source : evidence)
        {
            String key = source.getUrl() + ":" + source.getRevisionId();
            if (!seen.add(key)) { continue; }
            body.add(prose(source.getTitle(), TEXT, true));
            body.add(prose(source.getUrl(), MUTED));
            body.add(prose(source.isMissing() ? "Page missing at source audit"
                : "Page " + source.getPageId() + " / revision " + source.getRevisionId(), source.isMissing() ? WARNING : MUTED));
            body.add(prose(source.getTimestamp(), MUTED));
            body.add(gap(7));
        }
        return body;
    }

    private List<Method> methodsFor(Task task)
    {
        if (task == null || catalogue == null) { return new ArrayList<>(); }
        Set<String> ids = new LinkedHashSet<>();
        for (String monsterId : task.getMonsterIds())
        {
            Monster monster = catalogue.getMonsters().get(monsterId);
            if (monster != null) { ids.addAll(monster.getMethodIds()); }
        }
        return ids.stream().map(catalogue.getMethods()::get).filter(Objects::nonNull)
            .sorted(Comparator.comparing(Method::getName)).collect(Collectors.toList());
    }

    private Location locationFor(Task task, String locationId)
    {
        if (task != null && task.getLocationOverrides().containsKey(locationId))
        {
            return task.getLocationOverrides().get(locationId);
        }
        return catalogue.getLocations().get(locationId);
    }

    private JPanel itemCell(Choice choice, String slot, RecommendationRequest request)
    {
        JPanel cell = new JPanel();
        cell.setLayout(new OverlayLayout(cell));
        cell.setBackground(FIELD);
        cell.setPreferredSize(new Dimension(40, 38));
        cell.setMinimumSize(new Dimension(0, 38));
        cell.setBorder(BorderFactory.createLineBorder(choice != null && choice.getMissing() > 0 ? WARNING : new Color(63, 63, 63)));
        JLabel icon = new JLabel("", SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(36, 32));
        icon.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        icon.setAlignmentX(0.5f);
        icon.setAlignmentY(0.5f);
        if (choice == null || (choice.getItemId() <= 0 && choice.getMissing() == 0))
        {
            icon.setText(shortName(pretty(slot), 5));
            icon.setForeground(MUTED);
            icon.setFont(icon.getFont().deriveFont(10f));
            cell.add(icon);
            cell.setToolTipText(pretty(slot) + ": no item recommended");
            return cell;
        }
        String tooltip = "<html>" + html(choice.getName()) + " x" + choice.getQuantity()
            + "<br>" + html(choice.getOrigin()) + (choice.isRequired() ? " (required)" : "")
            + "<br>Carried: " + choice.getCarried() + "<br>Withdraw: " + choice.getWithdraw()
            + "<br>Missing: " + choice.getMissing()
            + (empty(choice.getExplanation()) ? "" : "<br>" + html(choice.getExplanation())) + "</html>";
        cell.setToolTipText(tooltip);
        cell.getAccessibleContext().setAccessibleName(pretty(slot) + " " + choice.getName() + ", quantity " + choice.getQuantity());
        icon.setToolTipText(tooltip);
        if (itemManager != null && choice.getItemId() > 0)
        {
            AsyncBufferedImage image = itemManager.getImage(choice.getItemId(), choice.getQuantity(), false);
            icon.setIcon(new ImageIcon(image));
            image.onLoaded(() -> SwingUtilities.invokeLater(() -> {
                icon.setIcon(new ImageIcon(image));
                icon.repaint();
            }));
        }
        else
        {
            icon.setText(shortName(choice.getName(), 5));
            icon.setForeground(TEXT);
            icon.setFont(icon.getFont().deriveFont(10f));
        }
        String count = choice.getQuantity() <= 1 ? "" : choice.getQuantity() >= 1000
            ? String.format(Locale.ENGLISH, "%.1fk", choice.getQuantity() / 1000.0) : String.valueOf(choice.getQuantity());
        JLabel quantity = cellCaption(count + (choice.getMissing() > 0 ? " !" : ""),
            choice.getMissing() > 0 ? WARNING : ACCENT);
        quantity.setToolTipText(tooltip);
        quantity.setHorizontalAlignment(SwingConstants.LEFT);
        quantity.setVerticalAlignment(SwingConstants.TOP);
        quantity.setAlignmentX(0.5f);
        quantity.setAlignmentY(0.5f);
        quantity.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        quantity.setBorder(BorderFactory.createEmptyBorder(1, 2, 0, 0));
        cell.add(quantity);
        cell.add(icon);
        return cell;
    }

    private static JPanel blankCell()
    {
        JPanel blank = new JPanel();
        blank.setOpaque(false);
        blank.setMinimumSize(new Dimension(0, 0));
        return blank;
    }

    private static JPanel emptyInventoryCell()
    {
        JPanel blank = blankCell();
        blank.setOpaque(true);
        blank.setBackground(FIELD);
        blank.setPreferredSize(new Dimension(40, 38));
        blank.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 55)));
        return blank;
    }

    private JPanel disclosure(String key, String title, Supplier<JComponent> content)
    {
        JPanel wrapper = column(CARD);
        JButton toggle = button("", null);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        toggle.setMargin(new java.awt.Insets(5, 5, 5, 5));
        JPanel contentHolder = new JPanel(new BorderLayout());
        contentHolder.setOpaque(false);
        boolean[] built = {false};
        Runnable update = () -> {
            boolean open = expanded.contains(key);
            toggle.setText("<html><body style='width:145px'>" + (open ? "[-] " : "[+] ") + html(title) + "</body></html>");
            toggle.getAccessibleContext().setAccessibleName(title + (open ? ", expanded" : ", collapsed"));
            if (open && !built[0])
            {
                contentHolder.add(content.get(), BorderLayout.CENTER);
                contentHolder.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
                built[0] = true;
            }
            contentHolder.setVisible(open);
            wrapper.revalidate();
            wrapper.repaint();
        };
        toggle.addActionListener(event -> {
            int position = scroll.getVerticalScrollBar().getValue();
            if (!expanded.remove(key)) { expanded.add(key); }
            update.run();
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(position));
        });
        wrapper.add(toggle);
        wrapper.add(contentHolder);
        update.run();
        return wrapper;
    }

    private void showEmpty(String message)
    {
        results.removeAll();
        JPanel empty = card();
        empty.add(prose(message, MUTED));
        results.add(empty);
        results.revalidate();
        results.repaint();
    }

    private static String bankStatus(PlayerSnapshot player)
    {
        if (player == null || !player.isLoggedIn()) { return "Catalogue preview. Log in to check owned equipment."; }
        if (player.getBankSeenAt() == 0) { return "Bank not captured. Open your bank to include stored gear."; }
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(player.getBankSeenAt()));
        return "Bank snapshot: " + time + ". Reopen your bank after changing stored items.";
    }

    private static String compactBankStatus(PlayerSnapshot player)
    {
        if (player == null || !player.isLoggedIn()) { return "Log in to check owned items."; }
        if (player.getBankSeenAt() == 0) { return "Open bank to include stored gear."; }
        String time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(player.getBankSeenAt()));
        return "Bank captured " + time;
    }

    private Context current() { return browsing ? browse : active; }
    private static String setupKey(Setup setup) { return setup.getMonsterId() + ":" + setup.getLocationId() + ":" + setup.getMethodId(); }
    private static boolean empty(String value) { return value == null || value.isEmpty(); }
    private static void run(Runnable action) { if (action != null) { action.run(); } }
    private static String selected(JComboBox<Option> combo) { Option option = (Option) combo.getSelectedItem(); return option == null ? null : option.id; }

    private static void fill(JComboBox<Option> combo, List<Option> values, String selection, String automatic)
    {
        combo.removeAllItems();
        if (automatic != null) { combo.addItem(new Option(null, automatic)); }
        values.sort(Comparator.comparing(option -> option.name));
        Option selected = null;
        for (Option option : values)
        {
            combo.addItem(option);
            if (Objects.equals(selection, option.id)) { selected = option; }
        }
        if (selected != null) { combo.setSelectedItem(selected); }
        else if (combo.getItemCount() > 0) { combo.setSelectedIndex(0); }
        combo.setEnabled(!values.isEmpty());
        combo.setToolTipText(combo.getSelectedItem() == null ? "No entries" : combo.getSelectedItem().toString());
    }

    private static JComboBox<Option> combo()
    {
        JComboBox<Option> combo = new JComboBox<>();
        styleCombo(combo);
        return combo;
    }

    private static void styleCombo(JComboBox<?> combo)
    {
        combo.setBackground(FIELD);
        combo.setForeground(TEXT);
        combo.setFocusable(true);
        combo.setFont(FontManager.getDefaultFont().deriveFont(12f));
        combo.setAlignmentX(LEFT_ALIGNMENT);
        combo.setMinimumSize(new Dimension(0, 28));
        combo.setPreferredSize(new Dimension(185, 28));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
    }

    private static JPanel field(String title, JComponent component)
    {
        JPanel field = column(PAGE);
        field.add(label(title, MUTED, false));
        component.getAccessibleContext().setAccessibleName(title);
        field.add(component);
        field.add(gap(5));
        return field;
    }

    private static JCheckBox check(String text)
    {
        JCheckBox check = new JCheckBox(text);
        check.setOpaque(false);
        check.setForeground(TEXT);
        check.setFocusPainted(false);
        check.setFont(FontManager.getDefaultFont().deriveFont(12f));
        check.setAlignmentX(LEFT_ALIGNMENT);
        return check;
    }

    private static JButton button(String text, Runnable action)
    {
        JButton button = new JButton(text);
        styleButton(button);
        if (action != null) { button.addActionListener(event -> action.run()); }
        return button;
    }

    private static void styleButton(javax.swing.AbstractButton button)
    {
        button.setBackground(FIELD);
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setFont(FontManager.getDefaultFont().deriveFont(12f));
        button.setMargin(new java.awt.Insets(5, 4, 5, 4));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
    }

    private static JPanel column(Color background)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMinimumSize(new Dimension(0, 0));
        return panel;
    }

    private static JPanel card()
    {
        JPanel panel = column(CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 7, 8, 7));
        return panel;
    }

    private static JLabel label(String text, Color color, boolean bold)
    {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(FontManager.getDefaultFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, 12f));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel cellCaption(String text, Color color)
    {
        JLabel label = label(text, color, false);
        label.setFont(label.getFont().deriveFont(10f));
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private static JTextArea prose(String text, Color color) { return prose(text, color, false); }

    private static JTextArea prose(String text, Color color, boolean bold)
    {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(color);
        area.setFont(FontManager.getDefaultFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, 12f));
        area.setAlignmentX(LEFT_ALIGNMENT);
        area.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        area.setMinimumSize(new Dimension(0, 0));
        area.setColumns(1);
        return area;
    }

    private static void appendLines(JPanel body, Collection<String> lines, Color color)
    {
        for (String line : lines)
        {
            // Source-authoring task-table diagnostics are retained in the data, not shown as player guidance.
            if (!empty(line) && !line.startsWith("SYNTHETIC ")) { body.add(prose(line, color)); }
        }
    }

    private static JComponent gap(int height)
    {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setAlignmentX(LEFT_ALIGNMENT);
        gap.setPreferredSize(new Dimension(0, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        gap.setMinimumSize(new Dimension(0, height));
        return gap;
    }

    private static String pretty(String value)
    {
        if (empty(value)) { return ""; }
        return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String shortName(String value, int length)
    {
        if (value == null) { return "?"; }
        return value.length() <= length ? value : value.substring(0, Math.max(1, length - 1)) + ".";
    }

    private static String html(String value)
    {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final class Option
    {
        private final String id;
        private final String name;
        private Option(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private static final class Context
    {
        private String taskId;
        private String masterId;
        private String monsterId;
        private String locationId;
        private String methodId;
        private RecommendationRequest request;
        private RecommendationResult result;
        private boolean preparationCurrent;
        private View view = View.SETUP;
        private void clearOverrides() { monsterId = null; locationId = null; methodId = null; }
        private void reset() { taskId = null; masterId = null; clearOverrides(); request = null; result = null; preparationCurrent = false; view = View.SETUP; }
    }

    private enum View { SETUP, CHECKS, GUIDE }

    private static final class RequirementCheck
    {
        private final String requirement;
        private final Assessment assessment;
        private RequirementCheck(String requirement, Assessment assessment)
        {
            this.requirement = requirement;
            this.assessment = assessment;
        }
    }

    private static final class ViewportPanel extends JPanel implements Scrollable
    {
        private ViewportPanel() { super(new BorderLayout()); setBackground(PAGE); }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(PANEL_WIDTH, 650); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(18, visible.height - 36); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static final class Selection
    {
        private final boolean browsing;
        private final String taskId;
        private final String masterId;
        private final String monsterId;
        private final String locationId;
        private final String methodId;
        private final RecommendationGoal goal;
        private final ReturnDestination returnDestination;
        private final boolean allowWilderness;
        private final boolean allowGroups;

        public Selection(boolean browsing, String taskId, String masterId, String monsterId,
            String locationId, String methodId, RecommendationGoal goal, boolean allowWilderness, boolean allowGroups,
            ReturnDestination returnDestination)
        {
            this.browsing = browsing;
            this.taskId = taskId;
            this.masterId = masterId;
            this.monsterId = monsterId;
            this.locationId = locationId;
            this.methodId = methodId;
            this.goal = goal;
            this.returnDestination = returnDestination;
            this.allowWilderness = allowWilderness;
            this.allowGroups = allowGroups;
        }

        public boolean isBrowsing() { return browsing; }
        public String getTaskId() { return taskId; }
        public String getMasterId() { return masterId; }
        public String getMonsterId() { return monsterId; }
        public String getLocationId() { return locationId; }
        public String getMethodId() { return methodId; }
        public RecommendationGoal getGoal() { return goal; }
        public ReturnDestination getReturnDestination() { return returnDestination; }
        public boolean isAllowWilderness() { return allowWilderness; }
        public boolean isAllowGroups() { return allowGroups; }
    }
}

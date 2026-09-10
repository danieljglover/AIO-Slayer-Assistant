package com.danieljglover.allinslayer.integration.advisor;

import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Setup;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;

/** Client-thread-only, temporary bank view. Actual bank positions and the user's saved setups are preserved. */
@Singleton
public final class BankSetupFilter
{
    private static final String TAG_PREFIX = "__allinslayer_setup_";
    private static final String LAYOUTS_GROUP = "banktaglayouts";
    private static final String[] EQUIPMENT_CELLS = {
        "", "HEAD", "", "CAPE", "AMULET", "AMMO", "WEAPON", "BODY", "SHIELD",
        "", "LEGS", "", "HANDS", "FEET", "RING"
    };
    // Bank Tags unions custom predicates with saved tags using prefix matching. A private name
    // prevents a user's similarly named tag from admitting unrelated equipment.
    private final String tag = TAG_PREFIX + UUID.randomUUID();
    @Inject private Client client;
    @Inject private ItemManager itemManager;
    @Inject private BankTagsPlugin bankTags;
    @Inject private TagManager tagManager;
    @Inject private BankSearch bankSearch;
    @Inject private ConfigManager configManager;
    @Inject private ClientThread clientThread;
    private Set<Integer> itemIds = Collections.emptySet();
    private int[] layoutItems = new int[0];
    private Layout openedLayout;
    private String setupKey;
    private boolean registered;
    private String previousTab;
    private Consumer<State> listener;
    private final Set<Widget> hiddenLayoutButtons = new LinkedHashSet<>();
    private State state = new State(false, false, "Open your bank to filter the selected setup.");

    public void start(Consumer<State> listener)
    {
        this.listener = listener;
        // Recover only our private temporary keys if a previous client exited without cleanup.
        for (String group : Arrays.asList(BankTagsPlugin.CONFIG_GROUP, LAYOUTS_GROUP))
        {
            for (String key : configManager.getConfigurationKeys(group + ".layout_" + TAG_PREFIX))
            {
                configManager.unsetConfiguration(group, key.substring(group.length() + 1));
            }
        }
        String remembered = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, "tab");
        if (remembered != null && remembered.startsWith(TAG_PREFIX))
        {
            if (remembered.equals(bankTags.getActiveTag())) { bankTags.closeBankTag(); }
            configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, "tab");
        }
        refresh();
        listener.accept(state);
    }

    public void setSetup(Setup setup, PlayerSnapshot player)
    {
        refresh();
        String nextKey = setup == null ? null : setup.getMonsterId() + ":" + setup.getLocationId() + ":" + setup.getMethodId();
        if (!Objects.equals(setupKey, nextKey)) { clear(); }
        setupKey = nextKey;
        Set<Integer> next = new LinkedHashSet<>();
        if (setup != null)
        {
            setup.getEquipment().values().forEach(choice -> add(next, choice));
            // Pouch contents are virtual inventory rows but still need to be found in the bank.
            setup.getInventory().forEach(choice -> add(next, choice));
        }
        int[] nextLayout = buildLayout(setup, player);
        if (!next.equals(itemIds) || !Arrays.equals(nextLayout, layoutItems))
        {
            itemIds = Collections.unmodifiableSet(next);
            layoutItems = nextLayout;
            if (ownsView())
            {
                if (itemIds.isEmpty()) { clear(); }
                else { openLayout(); }
            }
        }
        refresh();
    }

    private int[] buildLayout(Setup setup, PlayerSnapshot player)
    {
        // Eight bank columns: the panel's 3x5 equipment grid, a separator, then the 4x7 inventory.
        int[] cells = new int[56];
        Arrays.fill(cells, -1);
        if (setup == null) { return cells; }
        for (int index = 0; index < EQUIPMENT_CELLS.length; index++)
        {
            Choice choice = setup.getEquipment().get(EQUIPMENT_CELLS[index]);
            if (choice != null && choice.getItemId() > 0 && choice.getQuantity() > 0)
            {
                cells[(index / 3) * 8 + index % 3] = itemManager.canonicalize(choice.getItemId());
            }
        }
        int inventorySlot = 0;
        int pouchSlot = 0;
        for (Choice choice : setup.getInventory())
        {
            if (choice.getItemId() <= 0 || choice.getQuantity() <= 0) { continue; }
            int id = itemManager.canonicalize(choice.getItemId());
            if ("RUNE POUCH".equals(choice.getSlot()))
            {
                if (pouchSlot < 4) { cells[48 + pouchSlot++] = id; }
                continue;
            }
            PlayerSnapshot.ItemStats stats = player.getItems().get(choice.getItemId());
            int copies = stats != null && stats.isStackable() ? 1 : choice.getQuantity();
            for (int copy = 0; copy < copies && inventorySlot < 28; copy++, inventorySlot++)
            {
                cells[(inventorySlot / 4) * 8 + 4 + inventorySlot % 4] = id;
            }
        }
        // Blocked previews can exceed the physical backpack. Keep their remaining checklist IDs
        // in an appendix, so core Bank Tags never auto-inserts them into an equipment-shaped gap.
        Set<Integer> represented = new LinkedHashSet<>();
        for (int id : cells) { represented.add(id); }
        Set<Integer> extras = new LinkedHashSet<>();
        setup.getEquipment().values().forEach(choice -> add(extras, choice));
        setup.getInventory().forEach(choice -> add(extras, choice));
        extras.removeAll(represented);
        int end = cells.length;
        cells = Arrays.copyOf(cells, end + extras.size());
        for (int id : extras) { cells[end++] = id; }
        return cells;
    }

    private void add(Set<Integer> ids, Choice choice)
    {
        if (choice != null && choice.getItemId() > 0 && choice.getQuantity() > 0)
        {
            ids.add(itemManager.canonicalize(choice.getItemId()));
        }
    }

    public void open()
    {
        refresh();
        if (ownsView()) { return; }
        if (!bankOpen() || itemIds.isEmpty()) { return; }
        previousTab = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, "tab");
        tagManager.registerTag(tag, id -> id > 0 && itemIds.contains(itemManager.canonicalize(id)));
        registered = true;
        // Bank Tag Layouts otherwise auto-saves a new layout for any tag when its default is on.
        // This key belongs only to our private view and is removed with it, not a user preference.
        configManager.setConfiguration(LAYOUTS_GROUP, "layout_" + tag, "DISABLED");
        openLayout();
        // An existing Bank Tag Layouts preview is cancelled at the end of the first build.
        // Rebuild once afterward so its old scrollbar dimensions cannot survive in this view.
        clientThread.invokeLater(() -> { if (ownsView() && bankOpen()) { bankSearch.layoutBank(); } });
        refresh();
    }

    private void openLayout()
    {
        openedLayout = new Layout(tag, layoutItems.clone());
        bankTags.openTag(tag, openedLayout, BankTagsService.OPTION_HIDE_TAG_NAME);
    }

    public void finishLayout()
    {
        if (ownsView())
        {
            if (bankTags.getActiveLayout() != openedLayout)
            {
                // Core tag editing reloads a named layout and can discard our in-memory layout.
                // Never treat its compact bank widgets as our grid, or rebuild inside the script VM.
                clientThread.invokeLater(this::refresh);
                return;
            }
            Widget title = client.getWidget(InterfaceID.Bankmain.TITLE);
            if (title != null) { title.setText("All-In Slayer setup"); }
            Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
            if (container == null) { return; }
            for (int index = 0; index < layoutItems.length; index++)
            {
                Widget cell = container.getChild(index);
                if (cell == null) { continue; }
                // Core allows layout drag saves when its tag sidebar is disabled. This is a fixed
                // preparation view; retain core withdrawal handlers but suppress layout drags.
                cell.setOnDragCompleteListener((JavaScriptCallback) event -> client.setDraggedOnWidget(null));
                int expected = layoutItems[index];
                if (expected > 0 && cell.getItemId() > 0 && itemManager.canonicalize(cell.getItemId()) != expected)
                {
                    // Potion storage can substitute its selected withdrawal dose despite an exact
                    // item filter. Show the planned form as unavailable instead of a different dose.
                    cell.setItemId(expected);
                    cell.setItemQuantity(0);
                    cell.setItemQuantityMode(ItemQuantityMode.NEVER);
                    cell.setOpacity(120);
                    cell.setName(itemManager.getItemComposition(expected).getName() + " (not in bank)");
                    cell.clearActions();
                }
            }
            hideLayoutPreview();
        }
    }

    public void afterBankBuild()
    {
        // Bank Tag Layouts schedules updateButton from BUILD post. Queue after that callback.
        clientThread.invokeLater(() -> { if (ownsView()) { hideLayoutPreview(); } });
    }

    private void hideLayoutPreview()
    {
        Widget content = client.getWidget(WidgetInfo.BANK_CONTENT_CONTAINER);
        if (content == null) { return; }
        for (Widget child : content.getDynamicChildren())
        {
            String[] actions = child.getActions();
            if (actions != null && Arrays.asList(actions).contains("Preview auto layout") && !child.isSelfHidden())
            {
                hiddenLayoutButtons.add(child);
                child.setHidden(true);
            }
        }
    }

    public void refresh()
    {
        boolean open = bankOpen();
        // A different tag or a native bank search belongs to the user/other plugin. Never reopen ours.
        if (registered && (!open || !ownsView() || bankTags.getActiveLayout() != openedLayout)) { release(); }
        if (ownsView()) { hideLayoutPreview(); }
        State next = new State(open && !itemIds.isEmpty(), ownsView(), !open
            ? "Open your bank to filter the selected setup."
            : itemIds.isEmpty() ? "Choose a setup with equipment or supplies first."
            : ownsView() ? "Show your normal bank again."
            : "Arrange your bank like this setup: equipment left, inventory right, pouch runes below equipment.");
        if (!next.equals(state))
        {
            state = next;
            if (listener != null) { listener.accept(state); }
        }
    }

    public void clear()
    {
        release();
        refresh();
    }

    public void reset()
    {
        clear();
        itemIds = Collections.emptySet();
        layoutItems = new int[0];
        setupKey = null;
        refresh();
    }

    public void stop()
    {
        reset();
        listener = null;
    }

    private void release()
    {
        if (!registered) { return; }
        boolean restoreTab = tag.equals(configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, "tab"));
        hiddenLayoutButtons.forEach(button -> button.setHidden(false));
        hiddenLayoutButtons.clear();
        if (ownsView()) { bankTags.closeBankTag(); }
        tagManager.unregisterTag(tag);
        registered = false;
        openedLayout = null;
        configManager.unsetConfiguration(LAYOUTS_GROUP, "layout_" + tag);
        // Core layouts can be saved by user drags or newly encountered bank representations.
        configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, "layout_" + tag);
        // Bank Tags remembers even programmatically opened views. Do not leave a dead temporary tag
        // there after the bank closes, and do not overwrite a subsequent user-selected tab/search.
        if (restoreTab)
        {
            if (previousTab == null) { configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, "tab"); }
            else { configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, "tab", previousTab); }
        }
        previousTab = null;
    }

    private boolean ownsView() { return registered && tag.equals(bankTags.getActiveTag()); }

    private boolean bankOpen()
    {
        Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
        return client.getGameState() == GameState.LOGGED_IN && items != null && !items.isHidden();
    }

    @Value
    public static class State
    {
        boolean available;
        boolean active;
        String message;
    }
}

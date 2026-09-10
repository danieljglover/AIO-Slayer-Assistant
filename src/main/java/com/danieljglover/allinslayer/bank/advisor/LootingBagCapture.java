package com.danieljglover.allinslayer.bank.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.util.Text;

/** Client-thread-only, session-local observations of the bag; never requests a game action. */
@Singleton
public final class LootingBagCapture
{
    private static final int BAG_SLOTS = 28;
    private static final String UNOBSERVED = "Bag contents have not been observed this session.";
    private static final String STALE = "Bag contents may have changed; check the bag to refresh.";
    private static final Snapshot UNKNOWN = new Snapshot(false, Collections.emptyMap(), UNOBSERVED);
    private final Client client;
    private Snapshot snapshot = UNKNOWN;
    private long accountHash = -1;
    private RuneScapeProfileType profileType;
    private Widget ignoredViewMarker;
    private boolean carried;
    private PickupTarget pickupTarget;
    private PickupTarget telegrabTarget;
    private WorldPoint objectLootTarget;
    private boolean objectLootAnimation;

    @Inject
    public LootingBagCapture(Client client)
    {
        this.client = client;
    }

    public Snapshot capture()
    {
        return syncSession() && client.getGameState() == GameState.LOGGED_IN ? snapshot : UNKNOWN;
    }

    public boolean observe(ItemContainerChanged event)
    {
        Snapshot before = snapshot;
        if (!syncSession()) { return !before.equals(snapshot); }
        if (event.getContainerId() == InventoryID.LOOTING_BAG)
        {
            // A transmitted container is fresh even if its view has already closed. An absent
            // container, however, can also mean "not sent", so it never proves an empty bag.
            ItemContainer container = event.getItemContainer();
            if (container != null)
            {
                Map<Integer, Integer> contents = new LinkedHashMap<>();
                for (Item item : container.getItems())
                {
                    if (item != null) { add(contents, item.getId(), item.getQuantity()); }
                }
                confirm(contents);
            }
        }
        else if (event.getContainerId() == InventoryID.INV)
        {
            boolean present = hasBag(event.getItemContainer(), false);
            if (carried && !present) { invalidate(); }
            if (!present) { clearPendingActions(); }
            carried = present;
        }
        return !before.equals(snapshot);
    }

    public boolean gameTick()
    {
        Snapshot before = snapshot;
        if (syncSession() && client.getGameState() == GameState.LOGGED_IN)
        {
            boolean present = hasBag(client.getItemContainer(InventoryID.INV), false);
            if (carried && !present) { invalidate(); }
            if (!present) { clearPendingActions(); }
            carried = present;
            observeContentsView();
        }
        return !before.equals(snapshot);
    }

    public boolean observe(MenuOptionClicked event)
    {
        Snapshot before = snapshot;
        if (!syncSession() || event.isConsumed()) { return !before.equals(snapshot); }
        MenuAction action = event.getMenuAction();
        String option = plain(event.getMenuOption()).toLowerCase(Locale.ROOT);
        Widget clicked = event.getWidget();
        int clickedId = event.getItemId();
        if (clickedId <= 0 && clicked != null) { clickedId = clicked.getItemId(); }
        Widget selected = client.getSelectedWidget();
        boolean usedOnBag = action == MenuAction.WIDGET_TARGET_ON_WIDGET
            && (bagId(clickedId) || selected != null && bagId(selected.getItemId()));
        boolean removedBag = bagId(clickedId) && (option.equals("destroy") || option.equals("drop"));
        int component = event.getParam1();
        boolean deposit = component >>> 16 == InterfaceID.WILDERNESS_LOOTINGBAG
            && option.startsWith("store-");
        boolean bankContents = component == InterfaceID.Bankmain.DEPOSITCONTAINERS
            || component == InterfaceID.BankDepositbox.DEPOSIT_LOOTINGBAG
            || component == InterfaceID.Bankside.LOOTINGBAG_BANKALL
            || component == InterfaceID.Bankside.LOOTINGBAG_ITEMS && !option.equals("examine");
        boolean collecting = hasBag(client.getItemContainer(InventoryID.INV), true);
        boolean groundPickup = collecting && (action == MenuAction.GROUND_ITEM_THIRD_OPTION
            && option.equals("take") || action == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM);
        boolean lootObject = collecting && objectAction(action)
            && (option.equals("loot") || option.equals("steal") || option.startsWith("search")
                || option.equals("collect") || option.equals("claim")
                || (option.equals("open") || option.equals("unlock"))
                    && plain(event.getMenuTarget()).toLowerCase(Locale.ROOT).contains("chest"));
        if (groundPickup)
        {
            PickupTarget target = new PickupTarget(event.getId(), clickedPoint(event));
            if (action == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM) { telegrabTarget = target; }
            else { pickupTarget = target; }
        }
        if (lootObject)
        {
            objectLootTarget = clickedPoint(event);
            objectLootAnimation = false;
        }
        else if (!objectLootAnimation && (objectAction(action) || action == MenuAction.WALK))
        {
            objectLootTarget = null;
            objectLootAnimation = false;
        }
        if (usedOnBag || removedBag || deposit || bankContents || groundPickup || lootObject)
        {
            // These actions can alter the bag without transmitting container 516. Their success
            // and quantities are not guaranteed, so retain the old snapshot only as unknown.
            invalidate();
        }
        return !before.equals(snapshot);
    }

    public boolean observe(ItemDespawned event)
    {
        return pickupCompleted(event.getItem().getId(), event.getTile().getWorldLocation(), true);
    }

    public boolean observe(ItemQuantityChanged event)
    {
        return event.getNewQuantity() < event.getOldQuantity()
            && pickupCompleted(event.getItem().getId(), event.getTile().getWorldLocation(), false);
    }

    public boolean observe(AnimationChanged event)
    {
        Snapshot before = snapshot;
        if (!syncSession() || event.getActor() != client.getLocalPlayer() || objectLootTarget == null
            || !hasBag(client.getItemContainer(InventoryID.INV), true)) { return !before.equals(snapshot); }
        if (event.getActor().getAnimation() != -1)
        {
            if (objectLootTarget.distanceTo(client.getLocalPlayer().getWorldLocation()) > 2) { return false; }
            objectLootAnimation = true;
            invalidate();
        }
        else if (objectLootAnimation)
        {
            // A fresh View can finish while the player's earlier loot action is animating.
            invalidate();
            objectLootTarget = null;
            objectLootAnimation = false;
        }
        return !before.equals(snapshot);
    }

    private boolean pickupCompleted(int itemId, WorldPoint point, boolean removed)
    {
        Snapshot before = snapshot;
        if (!syncSession()) { return !before.equals(snapshot); }
        boolean pickup = pickupTarget != null && pickupTarget.matches(itemId, point);
        boolean telegrab = telegrabTarget != null && telegrabTarget.matches(itemId, point);
        if ((pickup || telegrab) && hasBag(client.getItemContainer(InventoryID.INV), true))
        {
            // A View response can arrive after Take was clicked but before walking or a
            // telegrab finishes. The later ground-stack change invalidates that observation.
            // Another player may have taken it instead; uncertainty is preferable to inferring loot.
            invalidate();
        }
        if (removed)
        {
            if (pickup) { pickupTarget = null; }
            if (telegrab) { telegrabTarget = null; }
        }
        return !before.equals(snapshot);
    }

    private WorldPoint clickedPoint(MenuOptionClicked event)
    {
        WorldView worldView = client.getWorldView(event.getMenuEntry().getWorldViewId());
        return worldView == null ? null
            : WorldPoint.fromScene(worldView, event.getParam0(), event.getParam1(), worldView.getPlane());
    }

    private void clearPendingActions()
    {
        pickupTarget = null;
        telegrabTarget = null;
        objectLootTarget = null;
        objectLootAnimation = false;
    }

    public boolean observe(ChatMessage event)
    {
        Snapshot before = snapshot;
        if (!syncSession()) { return !before.equals(snapshot); }
        if ((event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
            && hasBag(client.getItemContainer(InventoryID.INV), true))
        {
            String message = plain(event.getMessage());
            if (message.startsWith("You have been awarded ") && message.contains("Agility dispenser"))
            {
                invalidate();
            }
        }
        return !before.equals(snapshot);
    }

    public boolean observe(GameStateChanged event)
    {
        Snapshot before = snapshot;
        GameState state = event.getGameState();
        if (state != GameState.LOGGED_IN && state != GameState.LOADING)
        {
            reset();
        }
        else
        {
            syncSession();
        }
        return !before.equals(snapshot);
    }

    public void reset()
    {
        snapshot = UNKNOWN;
        accountHash = -1;
        profileType = null;
        ignoredViewMarker = null;
        carried = false;
        clearPendingActions();
    }

    public void invalidate()
    {
        snapshot = new Snapshot(false, snapshot.getContents(), STALE);
        // A mutation can be clicked while the previous empty view still exists. Do not allow
        // that old widget tree to make the snapshot known again before the game rebuilds it.
        ignoredViewMarker = viewMarker();
    }

    private boolean syncSession()
    {
        GameState state = client.getGameState();
        if (state != GameState.LOGGED_IN && state != GameState.LOADING)
        {
            reset();
            return false;
        }
        long currentHash = client.getAccountHash();
        if (currentHash == -1)
        {
            reset();
            return false;
        }
        RuneScapeProfileType currentType = RuneScapeProfileType.getCurrent(client);
        if (currentHash != accountHash || currentType != profileType)
        {
            reset();
            accountHash = currentHash;
            profileType = currentType;
            // Neither a buffered container nor an old widget is evidence for a new account.
            ignoredViewMarker = viewMarker();
        }
        return true;
    }

    private void observeContentsView()
    {
        Widget title = client.getWidget(InterfaceID.WildernessLootingbag.TITLE);
        Widget grid = client.getWidget(InterfaceID.WildernessLootingbag.ITEMS);
        if (title == null || title.isHidden() || !plain(title.getText()).equals("Looting bag")
            || grid == null || grid.isHidden())
        {
            return;
        }
        Widget marker = grid.getChild(0);
        if (marker == null || marker == ignoredViewMarker) { return; }
        Widget[] children = grid.getDynamicChildren();
        if (children == null || children.length < BAG_SLOTS) { return; }
        Map<Integer, Integer> contents = new LinkedHashMap<>();
        for (int slot = 0; slot < BAG_SLOTS; slot++)
        {
            Widget item = grid.getChild(slot);
            if (item == null) { return; }
            if (!item.isSelfHidden()) { add(contents, item.getItemId(), item.getItemQuantity()); }
        }
        Widget empty = grid.getChild(BAG_SLOTS);
        boolean explicitlyEmpty = empty != null && !empty.isHidden()
            && plain(empty.getText()).equals("The bag is empty.");
        // Group 81 also displays inventory 93 for "Add to bag". A missing container, unloaded
        // grid, or empty inventory in that mode must never be interpreted as an empty bag.
        if (!contents.isEmpty() || explicitlyEmpty)
        {
            confirm(contents);
            ignoredViewMarker = marker;
        }
    }

    private Widget viewMarker()
    {
        Widget grid = client.getWidget(InterfaceID.WildernessLootingbag.ITEMS);
        return grid == null ? null : grid.getChild(0);
    }

    private void confirm(Map<Integer, Integer> contents)
    {
        snapshot = new Snapshot(true, contents, contents.isEmpty()
            ? "An empty bag was observed this session." : "Bag contents were observed this session.");
        ignoredViewMarker = viewMarker();
    }

    private static boolean hasBag(ItemContainer inventory, boolean openOnly)
    {
        if (inventory == null) { return false; }
        for (Item item : inventory.getItems())
        {
            if (item != null && item.getQuantity() > 0
                && (item.getId() == ItemID.LOOTING_BAG_OPEN || !openOnly && item.getId() == ItemID.LOOTING_BAG))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean bagId(int id)
    {
        return id == ItemID.LOOTING_BAG || id == ItemID.LOOTING_BAG_OPEN;
    }

    private static boolean objectAction(MenuAction action)
    {
        return action == MenuAction.GAME_OBJECT_FIRST_OPTION || action == MenuAction.GAME_OBJECT_SECOND_OPTION
            || action == MenuAction.GAME_OBJECT_THIRD_OPTION || action == MenuAction.GAME_OBJECT_FOURTH_OPTION
            || action == MenuAction.GAME_OBJECT_FIFTH_OPTION;
    }

    private static String plain(String text)
    {
        return text == null ? "" : Text.removeTags(text).trim();
    }

    private static void add(Map<Integer, Integer> contents, int id, int quantity)
    {
        if (id > 0 && quantity > 0)
        {
            contents.merge(id, quantity,
                (first, second) -> (int) Math.min(Integer.MAX_VALUE, (long) first + second));
        }
    }

    private static final class PickupTarget
    {
        private final int itemId;
        private final WorldPoint point;

        private PickupTarget(int itemId, WorldPoint point)
        {
            this.itemId = itemId;
            this.point = point;
        }

        private boolean matches(int id, WorldPoint location)
        {
            return itemId == id && point != null && point.equals(location);
        }
    }

    @Getter
    @EqualsAndHashCode
    public static final class Snapshot
    {
        private final boolean known;
        private final Map<Integer, Integer> contents;
        private final String detail;

        private Snapshot(boolean known, Map<Integer, Integer> contents, String detail)
        {
            this.known = known;
            this.contents = Collections.unmodifiableMap(new LinkedHashMap<>(contents));
            this.detail = detail;
        }
    }
}

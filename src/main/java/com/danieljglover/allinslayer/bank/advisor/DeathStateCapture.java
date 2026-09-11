package com.danieljglover.allinslayer.bank.advisor;

import com.danieljglover.allinslayer.data.AdvisorDataService;
import com.danieljglover.allinslayer.model.advisor.DeathContext;
import com.danieljglover.allinslayer.model.advisor.DeathRule;
import com.danieljglover.allinslayer.model.advisor.ItemValue;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.SkullIcon;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.vars.AccountType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

/** Read-only client-thread observations; prices come from RuneLite's existing item cache. */
@Singleton
public final class DeathStateCapture
{
    private static final Pattern WILDERNESS_LEVEL = Pattern.compile("(?i)level\\s*:\\s*(\\d+)");
    // RuneLite's current ParamID.QUIVER_AMMO_AVAILABLE; not named in every supported API release.
    private static final int QUIVER_AMMO_AVAILABLE = 1910;
    private static final int[] POUCH_TYPES = {
        VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
        VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
    };
    private static final int[] POUCH_QUANTITIES = {
        VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
        VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
    };
    private final Client client;
    private final ItemManager itemManager;
    private final AdvisorDataService data;
    private final LootingBagCapture lootingBag;

    @Inject
    public DeathStateCapture(Client client, ItemManager itemManager, AdvisorDataService data, LootingBagCapture lootingBag)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.data = data;
        this.lootingBag = lootingBag;
    }

    public int fingerprint()
    {
        int result = status().hashCode();
        try
        {
            Widget bag = client.getWidget(InterfaceID.WildernessLootingbag.ITEMS);
            return 31 * result + (bag != null && !bag.isHidden() ? 1 : 0);
        }
        catch (RuntimeException ignored)
        {
            return 31 * result;
        }
    }

    public DeathContext capture()
    {
        DeathContext status = status();
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return status;
        }
        Map<Integer, Integer> pouch = new LinkedHashMap<>();
        boolean pouchKnown = capturePouch(pouch);
        Map<Integer, Integer> quiver = new LinkedHashMap<>();
        boolean quiverKnown = captureQuiver(quiver);
        LootingBagCapture.Snapshot bag = lootingBag.capture();
        return new DeathContext(status.isKnown(), status.isSkulled(), status.isProtectItemActive(),
            status.isHighRisk(), status.isUltimateIronman(), status.getWildernessLevel(),
            pouch, bag.getContents(), pouchKnown, bag.isKnown(), quiver, quiverKnown, bag.getDetail());
    }

    public Map<Integer, ItemValue> values(Set<Integer> ownedIds, Map<Integer, Integer> carried,
        DeathContext context)
    {
        Set<Integer> ids = new LinkedHashSet<>(ownedIds);
        ids.addAll(carried.keySet());
        ids.addAll(context.getPouchContents().keySet());
        ids.addAll(context.getLootingBagContents().keySet());
        ids.addAll(context.getQuiverContents().keySet());
        for (DeathRule rule : data.getCatalogue().getDeathRules().values())
        {
            ids.addAll(rule.getReplacementItems().keySet());
            ids.addAll(rule.getRepairCostItems().keySet());
            ids.addAll(rule.getAlwaysLostItems().keySet());
        }
        Map<Integer, ItemValue> values = new LinkedHashMap<>();
        for (int id : ids)
        {
            values.put(id, describeValue(id));
        }
        return values;
    }

    boolean containsRunePouch(Map<Integer, Integer> items)
    {
        return items.entrySet().stream().anyMatch(entry ->
        {
            DeathRule rule = data.getCatalogue().getDeathRules().get(entry.getKey());
            return entry.getValue() != null && entry.getValue() > 0
                && rule != null && "RUNE_POUCH".equals(rule.getContentsType());
        });
    }

    public Map<Integer, Integer> readCarried(ItemContainer container)
    {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        if (container == null)
        {
            return counts;
        }
        for (Item item : container.getItems())
        {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }
            ItemComposition definition = itemManager.getItemComposition(item.getId());
            if (definition.getPlaceholderTemplateId() == -1)
            {
                // Notes cannot be selected as equipment, but are still actual valuables at risk.
                counts.merge(item.getId(), item.getQuantity(), DeathStateCapture::add);
            }
        }
        return counts;
    }

    private DeathContext status()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return DeathContext.unknown();
        }
        try
        {
            Player player = client.getLocalPlayer();
            if (player == null)
            {
                return DeathContext.unknown();
            }
            EnumSet<WorldType> world = client.getWorldType();
            AccountType account = client.getAccountType();
            int skull = player.getSkullIcon();
            boolean highRisk = world.contains(WorldType.HIGH_RISK) || skull == SkullIcon.SKULL_HIGH_RISK;
            boolean known = account != null && !world.contains(WorldType.DEADMAN)
                && !world.contains(WorldType.SEASONAL) && !world.contains(WorldType.PVP_ARENA)
                && !world.contains(WorldType.LAST_MAN_STANDING);
            return new DeathContext(known, skull != SkullIcon.NONE && skull != SkullIcon.SKULL_FIGHT_PIT,
                client.isPrayerActive(Prayer.PROTECT_ITEM), highRisk, account == AccountType.ULTIMATE_IRONMAN,
                wildernessLevel(), Collections.emptyMap(), Collections.emptyMap(), false, false);
        }
        catch (RuntimeException ignored)
        {
            return DeathContext.unknown();
        }
    }

    private int wildernessLevel()
    {
        if (client.getVarbit(VarbitID.INSIDE_WILDERNESS) == null)
        {
            return -1;
        }
        if (client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 0)
        {
            return 0;
        }
        Widget level = client.getWidget(InterfaceID.PvpIcons.WILDERNESSLEVEL);
        if (level == null || level.isHidden() || level.getText() == null)
        {
            return -1;
        }
        // Reading the game's depth also handles caves and instances without coordinate guesses.
        Matcher matcher = WILDERNESS_LEVEL.matcher(Text.removeTags(level.getText()));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private boolean capturePouch(Map<Integer, Integer> contents)
    {
        try
        {
            EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
            if (runes == null || runes.size() == 0)
            {
                return false;
            }
            for (int i = 0; i < POUCH_TYPES.length; i++)
            {
                if (client.getVarbit(POUCH_TYPES[i]) == null || client.getVarbit(POUCH_QUANTITIES[i]) == null)
                {
                    return false;
                }
                int type = client.getVarbitValue(POUCH_TYPES[i]);
                int quantity = client.getVarbitValue(POUCH_QUANTITIES[i]);
                if (quantity < 0 || (quantity > 0 && type <= 0))
                {
                    return false;
                }
                if (quantity > 0)
                {
                    int id = runes.getIntValue(type);
                    if (id <= 0)
                    {
                        return false;
                    }
                    contents.merge(id, quantity, DeathStateCapture::add);
                }
            }
            return true;
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    private ItemValue describeValue(int id)
    {
        try
        {
            ItemComposition exact = itemManager.getItemComposition(id);
            ItemComposition base = exact.getNote() == -1 ? exact
                : itemManager.getItemComposition(exact.getLinkedNoteId());
            long market = itemManager.getItemPrice(id);
            long official = itemManager.getItemPriceWithSource(id, false);
            long protection = official > 0 ? official : base.isGeTradeable() ? -1 : base.getHaPrice();
            if (base.getId() == ItemID.COINS || base.getId() == ItemID.PLATINUM)
            {
                market = protection = base.getId() == ItemID.COINS ? 1 : 1000;
            }
            // ItemManager returns zero when no price has loaded, not proof of a worthless item.
            return new ItemValue(id, exact.getName(), market > 0 ? market : -1, protection,
                base.isTradeable(), exact.isStackable(), chargesUnknown(base));
        }
        catch (RuntimeException ignored)
        {
            return new ItemValue(id, null, -1, -1, false, false, true);
        }
    }

    private boolean captureQuiver(Map<Integer, Integer> contents)
    {
        try
        {
            ItemContainer worn = client.getItemContainer(InventoryID.WORN);
            Item cape = worn == null ? null : worn.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
            if (cape == null || itemManager.getItemComposition(cape.getId()).getIntValue(QUIVER_AMMO_AVAILABLE) != 1)
            {
                return false;
            }
            // These transmitted values also drive RuneLite's own quiver ammunition infobox.
            int id = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
            int quantity = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);
            if (quantity < 0 || (quantity > 0 && id <= 0))
            {
                return false;
            }
            if (quantity > 0)
            {
                contents.put(id, quantity);
            }
            return true;
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    private static boolean chargesUnknown(ItemComposition item)
    {
        String name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
        switch (item.getId())
        {
            case 28947: case 28949: case 28951: case 28953:
                return true; // Unblessed quivers can have partial blessing progress as well as sunfire charges.
            case 24551: case 25867: case 25870: case 25872: case 25874: case 25876:
            case 25878: case 25880: case 25882: case 25884: case 25886: case 25888:
            case 25890: case 25892: case 25894: case 25896: case 33021:
                return false; // Corrupted crystal weapons have no stored charge balance.
            default:
                break;
        }
        if (name.contains("uncharged") || name.contains("(empty)") || name.contains("(inactive)")
            || name.contains("corrupted") || name.contains("(broken)") || name.contains("(mangled)"))
        {
            return false;
        }
        switch (item.getId())
        {
            case 22550: case 22545: case 22555: case 27655: case 27660: case 27665: case 27679: case 27788:
                return true; // Ether stored in Wilderness weapons is lost even if the weapon is protected.
            default:
                break;
        }
        String[] actions = item.getInventoryActions();
        if (actions != null)
        {
            for (String action : actions)
            {
                if (action != null && action.equalsIgnoreCase("Uncharge"))
                {
                    return true;
                }
            }
        }
        // Stored charges are not supplied by bank/inventory containers. Never assume full or empty.
        return name.contains("arclight") || name.contains("iban's staff") || name.contains("slayer's staff (e)")
            || name.contains("trident") || name.contains("blowpipe") || name.contains("serpentine helm")
            || name.contains("tanzanite helm") || name.contains("magma helm")
            || name.contains("tumeken's shadow") || name.contains("sanguinesti staff")
            || name.contains("scythe of vitur") || name.contains("eye of ayak")
            || name.contains("blood fury") || name.contains("suffering (r")
            || name.contains("bracelet of ethereum") || name.contains("tonalztics of ralos")
            || name.contains("toxic staff of the dead") || name.contains("venator bow")
            || name.contains("warped sceptre") || name.contains("lithic sceptre")
            || name.contains("tome of fire") || name.contains("tome of water") || name.contains("tome of earth")
            || name.contains("ring of endurance") || name.contains("abyssal tentacle")
            || name.contains("crystal bow") || name.contains("crystal shield")
            || name.contains("crystal halberd") || name.contains("crystal helm")
            || name.contains("crystal body") || name.contains("crystal legs")
            || name.contains("bow of faerdhinen") || name.contains("blade of saeldor");
    }

    private static int add(int first, int second)
    {
        return (int) Math.min(Integer.MAX_VALUE, (long) first + second);
    }
}

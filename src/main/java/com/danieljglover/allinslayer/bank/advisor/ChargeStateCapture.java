package com.danieljglover.allinslayer.bank.advisor;

import com.danieljglover.allinslayer.model.advisor.ChargeObservation;
import com.danieljglover.allinslayer.integration.advisor.WeaponChargesBridge;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;

/**
 * Client-thread Check observations with optional, explicitly labelled Weapon Charges estimates.
 * Moving the same item between inventory and equipment preserves its observed balance.
 */
@Singleton
public final class ChargeStateCapture
{
    private static final int CHECK_WINDOW_TICKS = 3;
    private static final Map<String, Rule> FAMILIES = new LinkedHashMap<>();
    private static final Map<Integer, Rule> RULES = rules();
    private static final int SCALES = 12934;
    private static final Map<String, Integer> DARTS = darts();
    private final Client client;
    private final ConfigManager config;
    private final WeaponChargesBridge bridge;
    private final Map<Integer, ChargeObservation> observations = new LinkedHashMap<>();
    private String profile;
    private long accountHash;
    private int pendingItemId = -1;
    private int pendingTick;
    private int suppressThroughTick = -1;
    private boolean sessionSettled;

    public void setSessionSettled(boolean ready)
    {
        syncIdentity();
        sessionSettled = ready;
        if (!ready) { invalidate(); }
    }

    @Inject
    public ChargeStateCapture(Client client, ConfigManager config, WeaponChargesBridge bridge)
    {
        this.client = client;
        this.config = config;
        this.bridge = bridge;
    }

    public void invalidate()
    {
        observations.clear();
        pendingItemId = -1;
        suppressThroughTick = -1;
    }

    public void equipmentUsed()
    {
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn == null) { return; }
        for (Item item : worn.getItems()) { observations.remove(item.getId()); }
        pendingItemId = -1;
    }

    public void providerChanged(ConfigChanged event)
    {
        if (!WeaponChargesBridge.CONFIG_GROUP.equals(event.getGroup()) || !syncIdentity()) { return; }
        observations.entrySet().removeIf(entry -> {
            String family = bridge.familyKey(entry.getKey());
            if (family == null || !(family.equals(event.getKey()) || "blowpipe".equals(family)
                && event.getKey().startsWith("blowpipe"))) { return false; }
            if ("blowpipe".equals(family)) { return blowpipeChanged(event, entry.getValue()); }
            return balanceChanged(event.getNewValue(), entry.getValue().getCharges());
        });
    }

    private static boolean blowpipeChanged(ConfigChanged event, ChargeObservation observation)
    {
        Map<Integer, Integer> resources = observation.getResources();
        if ("blowpipeScales".equals(event.getKey()))
        {
            return balanceChanged(event.getNewValue(), resources.getOrDefault(SCALES, 0));
        }
        int darts = resources.entrySet().stream().filter(e -> DARTS.containsValue(e.getKey()))
            .mapToInt(Map.Entry::getValue).sum();
        if ("blowpipeDarts".equals(event.getKey())) { return balanceChanged(event.getNewValue(), darts); }
        if ("blowpipeDartType".equals(event.getKey()) && darts > 0)
        {
            Integer dartId = event.getNewValue() == null ? null : DARTS.get(event.getNewValue().toLowerCase(Locale.ROOT));
            return dartId == null || !resources.containsKey(dartId);
        }
        return false;
    }

    private static boolean balanceChanged(String raw, int observed)
    {
        try
        {
            double value = Double.parseDouble(raw);
            return !Double.isFinite(value) || value != observed;
        }
        catch (RuntimeException ignored) { return true; }
    }

    public void observe(MenuOptionClicked event)
    {
        if (!syncIdentity() || !sessionSettled) { return; }
        String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
        int clickedId = clickedItemId(event);
        if (!"check".equals(option))
        {
            pendingItemId = -1;
            if (option.equals("wear") || option.equals("wield") || option.equals("equip")
                || option.equals("remove") && equipmentSlot(event.getParam1()) >= 0) { return; }
            if (option.contains("charge") || option.equals("use") || option.contains("dismantle")) { invalidate(); }
            else if (event.isItemOp() || equipmentSlot(event.getParam1()) >= 0 && !option.equals("examine")
                || option.contains("deposit") || option.contains("withdraw"))
            {
                if (clickedId <= 0 && option.contains("deposit")) { invalidate(); return; }
                Rule affected = rule(clickedId);
                observations.keySet().removeIf(id -> affected != null && rule(id) == affected);
            }
            return;
        }
        int tick = client.getTickCount();
        if (tick <= suppressThroughTick) { return; }
        if (pendingItemId != -1 && tick - pendingTick <= CHECK_WINDOW_TICKS)
        {
            // Two checks can produce indistinguishable generic messages in either order.
            pendingItemId = -1;
            suppressThroughTick = tick + CHECK_WINDOW_TICKS;
            return;
        }
        pendingItemId = -1;
        int group = event.getParam1() >>> 16;
        if (group != InterfaceID.INVENTORY && group != InterfaceID.BANKSIDE
            && group != InterfaceID.WORNITEMS && group != InterfaceID.EQUIPMENT)
        {
            return;
        }
        int id = clickedId;
        Rule rule = rule(id);
        if (rule == null || !uniquelyCarried(id, rule)) { return; }
        observations.remove(id);
        pendingItemId = id;
        pendingTick = tick;
    }

    private int clickedItemId(MenuOptionClicked event)
    {
        int slot = equipmentSlot(event.getParam1());
        if (slot >= 0)
        {
            // Equipped-item menus belong to a slot layer, which need not carry an item ID.
            // Resolve the clicked slot in WORN, as Weapon Charges does for equipped weapons.
            ItemContainer worn = client.getItemContainer(InventoryID.WORN);
            Item item = worn == null ? null : worn.getItem(slot);
            return item == null ? -1 : item.getId();
        }
        int id = event.getItemId();
        Widget widget = event.getWidget();
        return id > 0 || widget == null ? id : widget.getItemId();
    }

    private static int equipmentSlot(int component)
    {
        switch (component)
        {
            case InterfaceID.Wornitems.SLOT0: case InterfaceID.Equipment.SLOT0: return 0;
            case InterfaceID.Wornitems.SLOT1: case InterfaceID.Equipment.SLOT1: return 1;
            case InterfaceID.Wornitems.SLOT2: case InterfaceID.Equipment.SLOT2: return 2;
            case InterfaceID.Wornitems.SLOT3: case InterfaceID.Equipment.SLOT3: return 3;
            case InterfaceID.Wornitems.SLOT4: case InterfaceID.Equipment.SLOT4: return 4;
            case InterfaceID.Wornitems.SLOT5: case InterfaceID.Equipment.SLOT5: return 5;
            case InterfaceID.Wornitems.SLOT7: case InterfaceID.Equipment.SLOT7: return 7;
            case InterfaceID.Wornitems.SLOT9: case InterfaceID.Equipment.SLOT9: return 9;
            case InterfaceID.Wornitems.SLOT10: case InterfaceID.Equipment.SLOT10: return 10;
            case InterfaceID.Wornitems.SLOT12: case InterfaceID.Equipment.SLOT12: return 12;
            case InterfaceID.Wornitems.SLOT13: case InterfaceID.Equipment.SLOT13: return 13;
            default: return -1;
        }
    }

    public void observe(ChatMessage event)
    {
        if (!syncIdentity() || !sessionSettled
            || event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
        {
            return;
        }
        if (pendingItemId == -1)
        {
            invalidateForChargeMessage(event.getMessage());
            return;
        }
        int tick = client.getTickCount();
        if (tick < pendingTick || tick - pendingTick > CHECK_WINDOW_TICKS)
        {
            invalidateForChargeMessage(event.getMessage());
            pendingItemId = -1;
            return;
        }
        Rule rule = rule(pendingItemId);
        if (rule == null) { pendingItemId = -1; return; }
        String message = Text.removeTags(event.getMessage());
        Matcher match = rule.message.matcher(message);
        boolean full = rule.fullMessage != null && rule.fullMessage.matcher(message).matches();
        if (!match.matches() && !full) { invalidateForChargeMessage(event.getMessage()); return; }
        int id = pendingItemId;
        pendingItemId = -1;
        if (!uniquelyCarried(id, rule)) { return; }
        try
        {
            if (rule == FAMILIES.get("blowpipe"))
            {
                int scales = count(match.group(3));
                if (scales > 16383) { return; }
                Map<Integer, Integer> resources = new LinkedHashMap<>();
                resources.put(SCALES, scales);
                int charges = 0;
                if (match.group(1) != null)
                {
                    Integer dartId = DARTS.get(match.group(1).toLowerCase(Locale.ROOT));
                    if (dartId == null) { return; }
                    int darts = count(match.group(2));
                    if (darts > 16383) { return; }
                    resources.put(dartId, darts);
                    charges = Math.min(darts, scales);
                }
                observations.put(id, new ChargeObservation(id, charges, tick, true, "In-game Check", resources));
                return;
            }
            int charges = full ? rule.maximum : count(match.group(1));
            if (charges >= 0 && charges <= rule.maximum)
            {
                observations.put(id, new ChargeObservation(id, charges, tick, true, "In-game Check",
                    rule == FAMILIES.get("serpentine_helm") ? Collections.singletonMap(SCALES, charges) : Collections.emptyMap()));
            }
        }
        catch (NumberFormatException ignored)
        {
            // An unexpected game message must remain unknown, never become zero.
        }
    }

    public Map<Integer, ChargeObservation> capture()
    {
        if (!syncIdentity() || !sessionSettled) { return Collections.emptyMap(); }
        observations.entrySet().removeIf(entry -> !uniquelyCarried(entry.getKey(), rule(entry.getKey())));
        Map<Integer, ChargeObservation> result = new LinkedHashMap<>();
        bridge.snapshot().forEach((id, reading) -> {
            int charges = reading.getCharges() == null ? resourceCapacity(reading.getResources()) : reading.getCharges();
            result.put(id, new ChargeObservation(id, charges, -1, false,
                reading.getSource() + " estimate (last check unknown)", reading.getResources()));
        });
        result.putAll(observations);
        return Collections.unmodifiableMap(result);
    }

    private void invalidateForChargeMessage(String message)
    {
        String text = Text.removeTags(message).toLowerCase(Locale.ROOT);
        if (text.matches(".*(?:charg|revenant ether|scales|darts?|pages?|blood shard|crystal shard|degrad|disintegrat|shatter).*"))
        {
            observations.clear();
        }
    }

    private static int count(String value)
    {
        return "one".equalsIgnoreCase(value) || "a".equalsIgnoreCase(value) ? 1
            : "no".equalsIgnoreCase(value) ? 0 : Integer.parseInt(value.replace(",", ""));
    }

    private static int resourceCapacity(Map<Integer, Integer> resources)
    {
        Integer scales = resources.get(SCALES);
        Integer darts = resources.entrySet().stream().filter(e -> DARTS.containsValue(e.getKey()))
            .map(Map.Entry::getValue).findFirst().orElse(null);
        return scales == null || darts == null ? -1 : Math.min(scales, darts);
    }

    private Rule rule(int id)
    {
        Rule nativeRule = RULES.get(id);
        return nativeRule != null ? nativeRule : FAMILIES.get(bridge.familyKey(id));
    }

    private boolean syncIdentity()
    {
        String current = client.getGameState() == GameState.LOGGED_IN ? config.getRSProfileKey() : null;
        long hash = current == null ? 0 : client.getAccountHash();
        if (!Objects.equals(profile, current) || accountHash != hash)
        {
            invalidate();
            sessionSettled = false;
            profile = current;
            accountHash = hash;
        }
        return current != null;
    }

    private boolean uniquelyCarried(int itemId, Rule rule)
    {
        if (rule == null) { return false; }
        int count = 0;
        boolean exact = false;
        for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
        {
            ItemContainer container = client.getItemContainer(containerId);
            if (container == null) { continue; }
            for (Item item : container.getItems())
            {
                if (rule(item.getId()) == rule && item.getQuantity() > 0)
                {
                    if (item.getQuantity() > 1 || ++count > 1) { return false; }
                    exact |= item.getId() == itemId;
                }
            }
        }
        return count == 1 && exact;
    }

    private static Map<Integer, Rule> rules()
    {
        Map<Integer, Rule> result = new LinkedHashMap<>();
        // The game reports usable ether charges separately from the activation deposit.
        add(result, "craws_bow|webweaver_bow", "Your bow has ([\\d,]+) charges? left powering it\\.", 16000, 22550, 27655);
        add(result, "viggoras_chainmace|ursine_chainmace", "Your chainmace has ([\\d,]+) charges? left powering it\\.", 16000, 22545, 27660);
        add(result, "thammarons_sceptre|accursed_sceptre", "Your sceptre has ([\\d,]+) charges? left powering it\\.", 16000,
            22555, 27788, 27665, 27679);
        add(result, "ibans_staff", "You have ([\\d,]+|a|no) charges? left on the staff\\.", 2500, 1409, 12658);
        add(result, "trident_of_the_seas", "Your Trident of the seas(?: \\(o\\))? has ([\\d,]+|one|no) charges?\\.", 2500, 11907, 11905, 33322);
        add(result, "trident_of_the_swamp", "Your Trident of the swamp(?: \\(o\\))? has ([\\d,]+|one|no) charges?\\.", 2500, 12899, 33314);
        add(result, "trident_of_the_seas_e", "Your Trident of the seas \\(e\\)(?: \\(o\\))? has ([\\d,]+|one|no) charges?\\.", 10000, 22288, 33326);
        add(result, "trident_of_the_swamp_e", "Your Trident of the swamp \\(e\\)(?: \\(o\\))? has ([\\d,]+|one|no) charges?\\.", 10000, 22292, 33318);
        add(result, "warped_sceptre", "Your warped sceptre has ([\\d,]+) charges? remaining\\.", 20000, 28585);
        addFull(result, "sanguinesti_staff", "Your (?:Holy s|S)anguinesti staff has ([\\d,]+) charges? remaining\\.",
            "Your (?:Holy s|S)anguinesti staff is already fully charged\\.", 20000, 22323, 25731);
        add(result, "tumekens_shadow", "Tumeken's shadow has ([\\d,]+) charges? remaining\\.", 20000, 27275);
        add(result, "abyssal_tentacle", "Your abyssal tentacle can perform ([\\d,]+) more attacks\\.", Integer.MAX_VALUE, 12006, 26484);
        add(result, "scythe_of_vitur", "Your (?:Sanguine s|Holy s|S)cythe of vitur has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 22325, 25736, 25739);
        add(result, "amulet_of_blood_fury", "Your Amulet of blood fury will work for ([\\d,]+) more hits\\.", Integer.MAX_VALUE, 24780);
        add(result, "arclight", "Your arclight has ([\\d,]+) charges? left\\.", 10000, 19675);
        add(result, "crystal_halberd", "Your crystal halberd has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 23987);
        add(result, "crystal_bow", "Your crystal bow has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 23983);
        add(result, "bow_of_faerdhinen", "Your bow of Faerdhinen has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 25865);
        add(result, "blade_of_saeldor", "Your blade of Saeldor has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 23995);
        add(result, "crystal_helm", "Your crystal helm has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 23971, 27705, 27717, 27729, 27741, 27753, 27765, 27777);
        add(result, "crystal_body", "Your crystal body has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 23975, 27697, 27709, 27721, 27733, 27745, 27757, 27769);
        add(result, "crystal_legs", "Your crystal legs has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 23979, 27701, 27713, 27725, 27737, 27749, 27761, 27773);
        add(result, "venator_bow", "Your venator bow has ([\\d,]+) charges? remaining\\.", Integer.MAX_VALUE, 27610, 30434);
        add(result, "serpentine_helm", "Scales: ([\\d,]+) \\([\\d.]+%\\)\\.?", 11000, 12931, 13197, 13199);
        add(result, "tome_of_fire", "Your tome has been charged with (?:Burnt|Searing) Pages\\. It currently holds ([\\d,]+|one) charges?\\.", Integer.MAX_VALUE, 20714);
        add(result, "tome_of_water", "Your tome currently holds ([\\d,]+|one) charges?\\.", Integer.MAX_VALUE, 25574);
        add(result, "eye_of_ayak", "The Eye of Ayak has been charged with (?:runes|demon tears)\\. It currently has ([\\d,]+) charges?\\.", 50000, 31113);
        add(result, "blowpipe", "Darts: (?:([a-z]+)(?: dart)? x ([\\d,]+)|None)\\. Scales: ([\\d,]+) \\([\\d.]+%\\)\\.", 16383, 12926, 28688);
        return Collections.unmodifiableMap(result);
    }

    private static void add(Map<Integer, Rule> rules, String keys, String message, int maximum, int... ids)
    {
        addFull(rules, keys, message, null, maximum, ids);
    }

    private static void addFull(Map<Integer, Rule> rules, String keys, String message, String fullMessage,
        int maximum, int... ids)
    {
        Rule rule = new Rule(Pattern.compile(message, Pattern.CASE_INSENSITIVE),
            fullMessage == null ? null : Pattern.compile(fullMessage, Pattern.CASE_INSENSITIVE), maximum);
        for (String key : keys.split("\\|")) { FAMILIES.put(key, rule); }
        for (int id : ids) { rules.put(id, rule); }
    }

    private static Map<String, Integer> darts()
    {
        Map<String, Integer> result = new LinkedHashMap<>();
        String[] names = {"bronze", "iron", "steel", "mithril", "adamant", "rune", "amethyst", "dragon"};
        int[] ids = {806, 807, 808, 809, 810, 811, 25849, 11230};
        for (int index = 0; index < names.length; index++) { result.put(names[index], ids[index]); }
        return Collections.unmodifiableMap(result);
    }

    private static final class Rule
    {
        private final Pattern message;
        private final Pattern fullMessage;
        private final int maximum;

        private Rule(Pattern message, Pattern fullMessage, int maximum)
        {
            this.message = message;
            this.fullMessage = fullMessage;
            this.maximum = maximum;
        }
    }
}

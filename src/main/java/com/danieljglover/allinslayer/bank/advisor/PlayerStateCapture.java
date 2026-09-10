package com.danieljglover.allinslayer.bank.advisor;

import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.PreparationSnapshot;
import com.danieljglover.allinslayer.model.advisor.AccountProgress;
import com.danieljglover.allinslayer.model.advisor.AccountProgress.State;
import com.danieljglover.allinslayer.model.advisor.DeathContext;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.Spellbook;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/** All methods are called on the client thread. Persist only observations tied to an RS profile. */
@Slf4j
@Singleton
public final class PlayerStateCapture
{
    private static final String GROUP = "allinslayer";
    private static final String BANK_KEY = "advisorBankV2";
    private static final String REQUIREMENTS_KEY = "advisorConfirmedRequirementsV2";
    private static final String[] SLOTS = {
        "HEAD", "CAPE", "AMULET", "WEAPON", "BODY", "SHIELD", null,
        "LEGS", null, "HANDS", "FEET", null, "RING", "AMMO"
    };
    private final Client client;
    private final ConfigManager config;
    private final ItemManager itemManager;
    private final Gson gson;
    private final AccountProgressCapture accountProgressCapture;
    private final DeathStateCapture deathStateCapture;
    private final ChargeStateCapture chargeStateCapture;
    private String profile;
    private BankObservation bank;
    private final Set<String> confirmations = new HashSet<>();
    private boolean bankDirty;
    private boolean bankWasOpen;
    private boolean preparationSettled;

    public boolean settlePreparation()
    {
        syncProfile();
        boolean ready = client.getGameState() == GameState.LOGGED_IN
            && client.getLocalPlayer() != null && profile != null;
        boolean changed = preparationSettled != ready;
        preparationSettled = ready;
        chargeStateCapture.setSessionSettled(ready);
        return changed;
    }

    public void invalidatePreparation()
    {
        preparationSettled = false;
        chargeStateCapture.setSessionSettled(false);
    }

    @Inject
    public PlayerStateCapture(Client client, ConfigManager config, ItemManager itemManager, Gson gson,
        AccountProgressCapture accountProgressCapture, DeathStateCapture deathStateCapture, ChargeStateCapture chargeStateCapture)
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        this.gson = gson;
        this.accountProgressCapture = accountProgressCapture;
        this.deathStateCapture = deathStateCapture;
        this.chargeStateCapture = chargeStateCapture;
    }

    public int deathStateFingerprint()
    {
        return deathStateCapture.fingerprint();
    }

    public void bankChanged()
    {
        bankDirty = true;
    }

    public void clear()
    {
        invalidatePreparation();
        chargeStateCapture.invalidate();
        profile = null;
        bank = null;
        bankDirty = false;
        bankWasOpen = false;
        confirmations.clear();
    }

    public void confirm(String requirement)
    {
        syncProfile();
        if (profile != null && requirement != null && !requirement.trim().isEmpty())
        {
            // Confirmation toggles are reversible and never override a known failed skill check.
            if (!confirmations.remove(requirement))
            {
                confirmations.add(requirement);
            }
            config.setRSProfileConfiguration(GROUP, REQUIREMENTS_KEY, gson.toJson(confirmations));
        }
    }

    public void clearConfirmations()
    {
        syncProfile();
        confirmations.clear();
        if (profile != null)
        {
            config.unsetRSProfileConfiguration(GROUP, REQUIREMENTS_KEY);
        }
    }

    public PlayerSnapshot capture()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return new PlayerSnapshot(Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), false, 0);
        }
        syncProfile();
        ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INV);
        ItemContainer equipmentContainer = client.getItemContainer(InventoryID.WORN);
        Map<Integer, Integer> inventory = read(inventoryContainer);
        Map<Integer, Integer> carried = new LinkedHashMap<>(inventory);
        Map<Integer, Integer> worn = read(equipmentContainer);
        merge(carried, worn);
        Map<Integer, Integer> riskCarried = deathStateCapture.readCarried(client.getItemContainer(InventoryID.INV));
        merge(riskCarried, deathStateCapture.readCarried(client.getItemContainer(InventoryID.WORN)));
        DeathContext deathContext = deathStateCapture.capture();
        Widget bankWidget = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
        boolean bankOpen = bankWidget != null && !bankWidget.isHidden();
        ItemContainer liveBank = client.getItemContainer(InventoryID.BANK);
        if (bankOpen && liveBank != null && (bankDirty || bank == null || !bankWasOpen))
        {
            BankObservation next = new BankObservation();
            next.version = 3;
            next.seenAt = System.currentTimeMillis();
            next.items = read(liveBank);
            next.carried = new HashMap<>(carried);
            next.pouchContents = new HashMap<>(usablePouchContents(deathContext, carried, next.items));
            bank = next;
            bankDirty = false;
            if (profile != null)
            {
                config.setRSProfileConfiguration(GROUP, BANK_KEY, gson.toJson(next));
            }
        }
        bankWasOpen = bankOpen;
        Map<Integer, Integer> pouch = usablePouchContents(deathContext, carried,
            bank == null ? Collections.emptyMap() : bank.items);
        Map<Integer, Integer> owned = new LinkedHashMap<>(carried);
        merge(owned, pouch);
        if (bank != null)
        {
            bank.items.forEach((id, quantity) ->
            {
                // Loading the pouch can withdraw runes without increasing loose inventory.
                // Compare both outside-bank stores together so moving runes between them does
                // not subtract twice, while a closed-bank withdrawal cannot add owned stock.
                long outside = (long) carried.getOrDefault(id, 0) + pouch.getOrDefault(id, 0);
                long previous = (long) bank.carried.getOrDefault(id, 0) + bank.pouchContents.getOrDefault(id, 0);
                long newlyOutside = Math.max(0, outside - previous);
                int estimate = (int) Math.max(0, quantity - newlyOutside);
                owned.merge(id, estimate, PlayerStateCapture::add);
            });
        }
        Map<String, Integer> levels = new HashMap<>();
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                levels.put(skill.name(), client.getRealSkillLevel(skill));
            }
        }
        if (client.getLocalPlayer() != null)
        {
            levels.put("COMBAT", client.getLocalPlayer().getCombatLevel());
        }
        Set<String> confirmed = new HashSet<>(confirmations);
        AccountProgress accountProgress = accountProgressCapture.capture();
        Map<String, State> progress = new LinkedHashMap<>(accountProgress.getRequirements());
        for (int id : worn.keySet())
        {
            progress.put(AccountProgress.key("Can equip: " + itemManager.getItemComposition(id).getName()), State.MET);
        }
        Map<Integer, PlayerSnapshot.ItemStats> stats = new HashMap<>();
        for (int id : owned.keySet())
        {
            PlayerSnapshot.ItemStats value = describe(id);
            if (value != null)
            {
                stats.put(id, value);
            }
        }
        PreparationSnapshot prepared = preparation(equipmentContainer, inventory, owned.keySet());
        Set<Integer> valuedIds = new HashSet<>(owned.keySet());
        prepared.getCharges().values().forEach(charges -> valuedIds.addAll(charges.getResources().keySet()));
        return new PlayerSnapshot(owned, carried, stats, levels, confirmed, true,
            bank == null ? 0 : bank.seenAt, captureSpellbook(), new AccountProgress(progress, accountProgress.getConfiguredRequirements()),
            deathStateCapture.values(valuedIds, riskCarried, deathContext), riskCarried, deathContext, prepared);
    }

    private PreparationSnapshot preparation(ItemContainer worn,
        Map<Integer, Integer> inventory, Set<Integer> ownedIds)
    {
        if (!preparationSettled) { return PreparationSnapshot.unknown(); }
        Map<String, PreparationSnapshot.Stack> equipment = new LinkedHashMap<>();
        // Empty containers need not exist in the client hash table. A completed logged-in
        // tick settles the initial packets; before that tick, absence remains unknown.
        for (int index = 0; index < SLOTS.length; index++)
        {
            Item item = worn == null ? null : worn.getItem(index);
            if (SLOTS[index] != null && item != null && item.getId() > 0 && item.getQuantity() > 0)
            {
                equipment.put(SLOTS[index], new PreparationSnapshot.Stack(item.getId(), item.getQuantity()));
            }
        }
        Map<Integer, Integer> canonicalIds = new LinkedHashMap<>();
        for (int id : ownedIds)
        {
            // Canonicalization only joins linked worn forms here, never notes, doses or charge variants.
            canonicalIds.put(id, itemManager.canonicalize(id));
        }
        return new PreparationSnapshot(true, inventory, equipment, canonicalIds, chargeStateCapture.capture());
    }

    private Map<Integer, Integer> usablePouchContents(DeathContext context,
        Map<Integer, Integer> carried, Map<Integer, Integer> bankItems)
    {
        // Global pouch varbits alone do not prove the account currently owns a usable pouch.
        return context.isPouchContentsKnown()
            && (deathStateCapture.containsRunePouch(carried) || deathStateCapture.containsRunePouch(bankItems))
            ? context.getPouchContents() : Collections.emptyMap();
    }

    private Spellbook captureSpellbook()
    {
        try
        {
            // RuneLite's spellbook varbit uses the same 0-3 mapping as Inventory Setups.
            switch (client.getVarbitValue(VarbitID.SPELLBOOK))
            {
                case 0: return Spellbook.STANDARD;
                case 1: return Spellbook.ANCIENT;
                case 2: return Spellbook.LUNAR;
                case 3: return Spellbook.ARCEUUS;
                default: return null;
            }
        }
        catch (RuntimeException ignored)
        {
            // Unavailable cache state must not silently become the Standard spellbook.
            return null;
        }
    }

    private void syncProfile()
    {
        String key = client.getGameState() == GameState.LOGGED_IN ? config.getRSProfileKey() : null;
        if (Objects.equals(profile, key))
        {
            return;
        }
        boolean pendingBankChange = bankDirty;
        clear();
        bankDirty = pendingBankChange;
        profile = key;
        if (key == null)
        {
            return;
        }
        try
        {
            BankObservation loaded = gson.fromJson(config.getRSProfileConfiguration(GROUP, BANK_KEY), BankObservation.class);
            if (loaded != null && (loaded.version == 2 || loaded.version == 3) && loaded.seenAt > 0
                && loaded.seenAt <= System.currentTimeMillis() && validCounts(loaded.items) && validCounts(loaded.carried)
                && (loaded.version == 2 || validCounts(loaded.pouchContents)))
            {
                // Older observations did not record pouch contents. Deduct all currently loaded
                // runes from the old bank estimate rather than inventing an unobserved baseline.
                if (loaded.version == 2) { loaded.pouchContents = Collections.emptyMap(); }
                bank = loaded;
            }
        }
        catch (RuntimeException ex)
        {
            log.warn("Ignoring invalid AIO Slayer bank observation");
        }
        try
        {
            String[] loaded = gson.fromJson(config.getRSProfileConfiguration(GROUP, REQUIREMENTS_KEY), String[].class);
            if (loaded != null)
            {
                for (String requirement : loaded)
                {
                    if (requirement != null && !requirement.trim().isEmpty())
                    {
                        confirmations.add(requirement);
                    }
                }
            }
        }
        catch (RuntimeException ex)
        {
            log.warn("Ignoring invalid AIO Slayer access confirmations");
        }
    }

    private Map<Integer, Integer> read(ItemContainer container)
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
            if (definition.getPlaceholderTemplateId() != -1 || definition.getNote() != -1)
            {
                continue; // Neither placeholders nor bank notes can be worn or used as supplies.
            }
            counts.merge(item.getId(), item.getQuantity(), PlayerStateCapture::add);
        }
        return counts;
    }

    private PlayerSnapshot.ItemStats describe(int id)
    {
        ItemComposition definition = itemManager.getItemComposition(id);
        String name = definition.getName();
        String lower = name.toLowerCase(Locale.ROOT);
        boolean usable = !lower.contains("(broken)") && !lower.contains("(mangled)")
            && !lower.matches(".*(?:helm|platebody|platelegs|plateskirt|hood|robe top|robe skirt|staff|warspear|hammers|crossbow) 0$");
        ItemStats stats = itemManager.getItemStats(id);
        ItemEquipmentStats e = stats == null ? null : stats.getEquipment();
        if (e == null)
        {
            return new PlayerSnapshot.ItemStats(id, name, null, "ANY", usable, false,
                definition.isStackable(), 0, 0, 0, 0);
        }
        String slot = e.getSlot() >= 0 && e.getSlot() < SLOTS.length ? SLOTS[e.getSlot()] : null;
        // Uncharged jewellery can retain its combat stats. Powered weapons need a usable
        // charged form; the bundled item definitions apply any additional form restrictions.
        if ("WEAPON".equals(slot) && (lower.contains("uncharged") || lower.contains("(empty)")
            || lower.contains("(inactive)")))
        {
            usable = false;
        }
        double melee = Math.max(e.getAstab(), Math.max(e.getAslash(), e.getAcrush()));
        String style = "ANY";
        if ("WEAPON".equals(slot))
        {
            style = "MELEE";
            if (e.getRstr() > 0 || e.getArange() > melee)
            {
                style = "RANGED";
            }
            if (e.getMdmg() > 0 || e.getAmagic() > Math.max(melee, e.getArange())
                || lower.contains("staff") || lower.contains("wand") || lower.contains("trident"))
            {
                style = "MAGIC";
            }
        }
        else if (e.getMdmg() > 0 || e.getAmagic() > Math.max(melee, e.getArange()))
        {
            style = "MAGIC";
        }
        else if (e.getRstr() > 0 || e.getArange() > Math.max(melee, e.getAmagic()))
        {
            style = "RANGED";
        }
        else if (e.getStr() > 0 || melee > Math.max(e.getArange(), e.getAmagic()))
        {
            style = "MELEE";
        }
        double defence = (e.getDstab() + e.getDslash() + e.getDcrush() + e.getDmagic() + e.getDrange()) / 5.0;
        return new PlayerSnapshot.ItemStats(id, name, slot, style, usable, e.isTwoHanded(),
            definition.isStackable(), melee, e.getStr(), defence, e.getPrayer(),
            e.getArange(), e.getRstr(), e.getAmagic(), e.getMdmg());
    }

    private static void merge(Map<Integer, Integer> target, Map<Integer, Integer> source)
    {
        source.forEach((id, quantity) -> target.merge(id, quantity, PlayerStateCapture::add));
    }

    private static int add(int first, int second)
    {
        return (int) Math.min(Integer.MAX_VALUE, (long) first + second);
    }

    private static boolean validCounts(Map<Integer, Integer> counts)
    {
        return counts != null && counts.entrySet().stream().allMatch(e -> e.getKey() != null
            && e.getKey() > 0 && e.getValue() != null && e.getValue() >= 0);
    }

    private static final class BankObservation
    {
        private int version;
        private long seenAt;
        private Map<Integer, Integer> items;
        private Map<Integer, Integer> carried;
        private Map<Integer, Integer> pouchContents;
    }
}

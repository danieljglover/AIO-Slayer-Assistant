package com.danieljglover.allinslayer.bank.advisor;

import com.danieljglover.allinslayer.model.advisor.AccountProgress;
import com.danieljglover.allinslayer.model.advisor.AccountProgress.State;
import com.danieljglover.allinslayer.integration.advisor.ShortestPathHouseSettings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.VarbitComposition;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarbitID;

/** Factual account observations. Call only on the client thread; nothing is persisted here. */
@Singleton
public final class AccountProgressCapture
{
    private static final String[] DIARY_TIERS = {"Easy", "Medium", "Hard", "Elite"};
    private final Client client;
    private final ShortestPathHouseSettings houseSettings;

    @Inject
    public AccountProgressCapture(Client client, ShortestPathHouseSettings houseSettings)
    {
        this.client = client;
        this.houseSettings = houseSettings;
    }

    public AccountProgress capture()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return new AccountProgress(Collections.emptyMap());
        }
        Map<String, State> requirements = new LinkedHashMap<>();
        captureQuests(requirements);
        captureDiaries(requirements);
        captureUnlocks(requirements);
        captureHouse(requirements);
        return new AccountProgress(requirements, houseSettings.capture());
    }

    private void captureHouse(Map<String, State> requirements)
    {
        try
        {
            VarbitComposition definition = client.getVarbit(VarbitID.POH_HOUSE_LOCATION);
            int[] varps = client.getVarps();
            if (definition == null || varps == null || definition.getIndex() < 0
                || definition.getIndex() >= varps.length) { return; }
            int location = client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION);
            // Reviewed portal encodings 1-9 also used by Shortest Path. Unrecognised
            // values are not evidence against ownership, and say nothing about furniture.
            if (location >= 1 && location <= 9)
            {
                put(requirements, State.MET, "Own a player-owned house");
            }
        }
        catch (RuntimeException ignored)
        {
            // Keep the quest proof or manual fallback when the cache field is unavailable.
        }
    }

    private void captureQuests(Map<String, State> requirements)
    {
        for (Quest quest : Quest.values())
        {
            QuestState progress = readQuest(quest);
            State completed = progress == null ? State.UNKNOWN : state(progress == QuestState.FINISHED);
            State started = progress == null ? State.UNKNOWN : state(progress != QuestState.NOT_STARTED);
            questAliases(requirements, quest.getName(), completed, started);
            authoredQuestRequirements(requirements, quest, completed, started);

            String rfdPrefix = "Recipe for Disaster - ";
            if (quest.getName().startsWith(rfdPrefix))
            {
                String part = quest.getName().substring(rfdPrefix.length());
                questAliases(requirements, "Recipe for Disaster: " + part, completed, started);
                questAliases(requirements, "Recipe for Disaster/" + part, completed, started);
                if (quest != Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST
                    && quest != Quest.RECIPE_FOR_DISASTER__CULINAROMANCER)
                {
                    questAliases(requirements, "Recipe for Disaster: Freeing " + part, completed, started);
                    questAliases(requirements, "Recipe for Disaster/Freeing " + part, completed, started);
                }
            }

            // Finishing the quest proves these milestones. An unfinished quest cannot disprove
            // them, because the player may already have passed the relevant intermediate step.
            // Omit unsupported milestones so the evaluator can retain its manual fallback.
            if (completed != State.MET)
            {
                continue;
            }
            switch (quest)
            {
                case DADDYS_HOME:
                    // The reward grants a house, or coins if the player already owns one.
                    put(requirements, State.MET, "Own a player-owned house");
                    break;
                case ANOTHER_SLICE_OF_HAM:
                    put(requirements, State.MET, "Received the ancient mace during Another Slice of H.A.M.");
                    break;
                case ZOGRE_FLESH_EATERS:
                    put(requirements, State.MET,
                        "Spoken to Grish to unlock the comp ogre bow during Zogre Flesh Eaters");
                    break;
                case RECIPE_FOR_DISASTER:
                    put(requirements, State.MET, "Completed Daero's training or defeated the Culinaromancer");
                    break;
                case DESERT_TREASURE_II__THE_FALLEN_EMPIRE:
                    put(requirements, State.MET,
                        "Defeated the Whisperer during Desert Treasure II",
                        "Defeated Duke Sucellus during Desert Treasure II",
                        "Defeated Vardorvis during Desert Treasure II",
                        "Defeated the Leviathan during Desert Treasure II");
                    break;
                default:
                    break;
            }
        }
    }

    private QuestState readQuest(Quest quest)
    {
        try
        {
            // QUEST_STATUS_GET can return NOT_STARTED for a missing table row. Check that the
            // row exists first so an unavailable cache definition does not become negative evidence.
            Object[] name = client.getDBTableField(quest.getId(), DBTableID.Quest.COL_DISPLAYNAME, 0);
            if (name == null || name.length == 0 || !(name[0] instanceof String)
                || ((String) name[0]).isEmpty())
            {
                return null;
            }
            return quest.getState(client);
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    private static void questAliases(Map<String, State> requirements, String name, State completed, State started)
    {
        put(requirements, completed, name, "Quest: " + name, name + " completion", name + " completed",
            "Completed " + name, "Completion of " + name);
        put(requirements, started, "Started " + name);
    }

    private static void authoredQuestRequirements(Map<String, State> requirements, Quest quest,
        State completed, State started)
    {
        // These reviewed source phrases describe just one quest gate. Keep the complete phrase:
        // splitting prose at "and" would corrupt quest names and overlook gear or skill gates.
        switch (quest)
        {
            case CABIN_FEVER:
                put(requirements, completed, "Cabin Fever to access Mos Le'Harmless",
                    "Cabin Fever to access Mos Le'Harmless Cave");
                break;
            case DEATH_PLATEAU:
                put(requirements, completed, "Death Plateau completed before assignment");
                break;
            case DESERT_TREASURE_I:
                put(requirements, started, "Started Desert Treasure I to receive assignments and enter the Smoke Dungeon");
                break;
            case DRAGON_SLAYER_II:
                put(requirements, completed, "Dragon Slayer II for Corsair Cove Dungeon",
                    "Dragon Slayer II for Corsair Cove Dungeon / Myths' Guild basement red dragons",
                    "Dragon Slayer II for Corsair Cove Dungeon and Vorkath");
                break;
            case DWARF_CANNON:
                put(requirements, completed, "Dwarf Cannon completion to use a dwarf multicannon");
                break;
            case ELEMENTAL_WORKSHOP_I:
                put(requirements, completed, "Elemental Workshop I completion for the basic shield requirement");
                break;
            case THE_FREMENNIK_EXILES:
                put(requirements, completed, "The Fremennik Exiles for Basilisk Knight access",
                    "The Fremennik Exiles for Jormungand's Prison");
                break;
            case THE_FREMENNIK_TRIALS:
                put(requirements, completed,
                    "The Fremennik Trials to speak to Mord Gunnars and access Jatizso/Neitiznot for island ice trolls");
                break;
            case HEROES_QUEST:
                put(requirements, completed, "Heroes' Quest for Heroes' Guild basement");
                break;
            case HORROR_FROM_THE_DEEP:
                put(requirements, completed, "Horror from the Deep to be assigned and access the Lighthouse basement");
                break;
            case LEGENDS_QUEST:
                put(requirements, completed, "Completion of Legends' Quest is required to be assigned Shadow warriors.");
                break;
            case LOST_CITY:
                put(requirements, completed, "Lost City completion for Zanaris access");
                break;
            case MONKEY_MADNESS_II:
                put(requirements, completed, "Monkey Madness II for Demonic gorillas");
                break;
            case A_PORCINE_OF_INTEREST:
                put(requirements, completed,
                    "Quest A Porcine of Interest to access Sourhog Cave; no combat or Slayer level requirement.");
                break;
            case PRIEST_IN_PERIL:
                put(requirements, completed, "Priest in Peril is required to access Mort'ton for the fast Loar Shades.",
                    "Priest in Peril is required.",
                    "Priest in Peril must be completed for access to Morytania (west of Canifis).",
                    "Priest in Peril must be completed to enter the Slayer Tower ground floor.",
                    "Priest in Peril must be completed to enter the Slayer Tower.");
                break;
            case REGICIDE:
                put(requirements, completed, "Completion of Regicide to receive the task",
                    "Regicide (access to Zul-Andra)");
                break;
            case RUM_DEAL:
                put(requirements, started, "Rum Deal must be started to access Braindeath Island.");
                break;
            case SINS_OF_THE_FATHER:
                put(requirements, completed, "Completion of Sins of the Father for Darkmeyer access.",
                    "Sins of the Father for the profitable Vyrewatch Sentinel path.");
                break;
            case SONG_OF_THE_ELVES:
                put(requirements, completed, "Iorwerth Dungeon requires Song of the Elves.",
                    "Song of the Elves for Iorwerth Dungeon", "Song of the Elves for Mynydd",
                    "Song of the Elves for Prifddinas and Iorwerth Dungeon",
                    "Song of the Elves is required for Iorwerth Dungeon.",
                    "Using the rowboat between Ynysdail and north of Gwenith requires Song of the Elves.");
                break;
            case A_TASTE_OF_HOPE:
                put(requirements, completed, "A Taste of Hope for practical Vyrewatch access and Ivandis flail use.");
                break;
            case WATCHTOWER:
                put(requirements, completed, "Watchtower for Ogre Enclave");
                break;
            case SECRETS_OF_THE_NORTH:
                put(requirements, completed, "Complete Secrets of the North to access Ghorrock Dungeon and refight the Phantom Muspah.");
                break;
            case THE_HEART_OF_DARKNESS:
                put(requirements, completed, "The Heart of Darkness for Ruins of Tapoyauik",
                    "The Heart of Darkness is required for repeatable encounters.",
                    "The Heart of Darkness is required for the Ruins of Tapoyauik.");
                break;
            case THE_FINAL_DAWN:
                put(requirements, completed, "Completion of The Final Dawn for Earthen Nagua in the Crypt of Tonali");
                break;
            default:
                break;
        }
    }

    private void captureDiaries(Map<String, State> requirements)
    {
        diary(requirements, new String[]{"Ardougne"}, 1,
            VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Desert", "Kharidian Desert"}, 1,
            VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
            VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Falador"}, 1,
            VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
            VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Fremennik"}, 1,
            VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
            VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Kandarin"}, 1,
            VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
            VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE);
        // The game's area_task_complete script requires 2 for the original Karamja tiers;
        // value 1 is partial progress. Its elite tier uses the newer boolean completion flag.
        diary(requirements, new String[]{"Karamja"}, 2,
            VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE,
            VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Kourend & Kebos", "Kourend and Kebos", "Kourend"}, 1,
            VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
            VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Lumbridge & Draynor", "Lumbridge and Draynor", "Lumbridge"}, 1,
            VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Morytania"}, 1,
            VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
            VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Varrock"}, 1,
            VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
            VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Western Provinces", "Western"}, 1,
            VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
            VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE);
        diary(requirements, new String[]{"Wilderness"}, 1,
            VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
            VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE);
    }

    private void diary(Map<String, State> requirements, String[] areas, int originalTierValue, int... varbits)
    {
        for (int tier = 0; tier < DIARY_TIERS.length; tier++)
        {
            State completed = readVarbit(varbits[tier], tier == 3 ? 1 : originalTierValue);
            for (String area : areas)
            {
                String name = DIARY_TIERS[tier] + " " + area + " Diary";
                String reordered = area + " " + DIARY_TIERS[tier] + " Diary";
                put(requirements, completed, name, name + " completed", name + " complete",
                    name + " is complete", name + " completion", "Completed " + name,
                    "Completion of " + name, reordered, reordered + " completed");
            }
        }
    }

    private void captureUnlocks(Map<String, State> requirements)
    {
        flag(requirements, VarbitID.PRAYER_RIGOUR_UNLOCKED, "Rigour unlocked");
        flag(requirements, VarbitID.PRAYER_AUGURY_UNLOCKED, "Augury unlocked");
        flag(requirements, VarbitID.PRAYER_PRESERVE_UNLOCKED, "Preserve unlocked");
        flag(requirements, VarbitID.PRAYER_DEADEYE_UNLOCKED, "Deadeye unlocked");
        flag(requirements, VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED, "Mystic Vigour unlocked");
        flag(requirements, VarbitID.MAGICTRAINING_BONESPEACHES, "Bones to Peaches unlocked",
            "Bones to Peaches requires the Mage Training Arena unlock.");
        flag(requirements, VarbitID.KOUREND_ELITE_REWARD,
            "Elite Kourend & Kebos Diary reward claimed");
        flag(requirements, VarbitID.KOUREND_SLAYER_HELM_BONUS,
            "Captain Cleive's Slayer helmet protection unlocked");
        flag(requirements, VarbitID.GARGBOSS_UNLOCKED_ROOF,
            "Brittle key used to unlock the Slayer Tower roof");

        // The purchased superior unlock can be disabled separately. Requirements describing
        // superior spawns need both the purchase and its currently enabled state.
        State superiors = and(readVarbit(VarbitID.SLAYER_UNLOCK_SUPERIORMOBS, 1),
            not(readVarbit(VarbitID.SLAYER_TOGGLEOFF_SUPERIORMOBS, 1)));
        put(requirements, superiors,
            "Bigger and Badder",
            "Bigger and Badder (50 points) for Colossal Hydra superior spawns",
            "Bigger and Badder enables Ancient Custodian superior spawns.",
            "Bigger and Badder enables Greater abyssal demon superior spawns.",
            "Bigger and Badder enables Insatiable Bloodveld and Insatiable mutated Bloodveld superior spawns.",
            "Bigger and Badder for Abhorrent spectre and Repugnant spectre superior spawns",
            "Bigger and Badder for Cave abomination superior spawns",
            "Bigger and Badder for Dire gryphon superior spawns",
            "Bigger and Badder for Monstrous basilisk and Basilisk Sentinel superior spawns",
            "Bigger and Badder for Mutated Tortoise and Mutated Terrorbird superior spawns",
            "Bigger and Badder for Nechryarch superior spawns",
            "Bigger and Badder for Shadow Wyrm and Magma strykewyrm superior spawns",
            "Bigger and Badder unlock for 50 Slayer reward points",
            "Bigger and Badder unlock for Choke devil superior spawns",
            "Bigger and Badder unlock for Guardian Drake superior spawns",
            "Bigger and Badder unlock for Night beast superior spawns",
            "Bigger and Badder unlocks King kurask superior spawns.",
            "Bigger and Badder unlocks Spiked Turoth superior spawns.",
            "Bigger and Badder unlocks Vitreous Jelly superior spawns.");
        flag(requirements, VarbitID.SLAYER_UNLOCK_AVIANSIES, "Watch the birdie unlock");
        flag(requirements, VarbitID.SLAYER_UNLOCK_TZHAAR,
            "Hot Stuff unlock purchased for 100 slayer points before TzHaar can be assigned.");
        flag(requirements, VarbitID.SLAYER_UNLOCK_REDDRAGONS, "Seeing red unlock for 50 Slayer reward points");
        flag(requirements, VarbitID.SLAYER_UNLOCK_LIZARDMEN,
            "Reptile got ripped must be unlocked for Chaeldar, Konar, Nieve, and Duradel to assign Lizardmen.");
        flag(requirements, VarbitID.SLAYER_UNLOCK_BASILISK,
            "Basilocked unlock for Konar, Nieve, and Duradel assignments");
        flag(requirements, VarbitID.SLAYER_UNLOCK_VAMPYRES,
            "Actual Vampyre Slayer unlock for Chaeldar, Konar, Nieve/Steve, and Duradel/Kuradal assignments.");
        flag(requirements, VarbitID.SLAYER_UNLOCK_WARPED_CREATURES,
            "Warped Reality unlock for 60 Slayer reward points");
        flag(requirements, VarbitID.SLAYER_UNLOCK_GRYPHONS,
            "Wings Spread unlock for Nieve/Steve or Duradel/Kuradal assignments");
        flag(requirements, VarbitID.SLAYER_UNLOCK_AQUANITES,
            "The Lured In Slayer reward unlock is required before Nieve or Duradel can assign Aquanites.");
        flag(requirements, VarbitID.SLAYER_LONGER_ANKOU,
            "Ankou very much extends assignments to 91-150 Ankou.");
        flag(requirements, VarbitID.SLAYER_LONGER_ABYSSALDEMONS,
            "Augment my abbies extends assignments to 200-250 Abyssal demons.");
        flag(requirements, VarbitID.SLAYER_LONGER_BLOODVELD,
            "Bleed me dry extends Bloodveld assignments to 200-250.");
        flag(requirements, VarbitID.SLAYER_LONGER_WYRMS,
            "Can of Wyrms unlock for extended 200-250 assignments");
        flag(requirements, VarbitID.SLAYER_LONGER_GREATERDEMONS,
            "Greater Challenge extends assignments to 200-250.");
        flag(requirements, VarbitID.SLAYER_LONGER_CAVEHORRORS,
            "Horrorific unlock for 200-250 extended assignments");
        flag(requirements, VarbitID.SLAYER_LONGER_CAVEKRAKEN,
            "Krack on unlock for 150-200 extended assignments");
        flag(requirements, VarbitID.SLAYER_LONGER_VAMPYRES,
            "More at stake unlock for 200-250 extended tasks.");
        flag(requirements, VarbitID.SLAYER_LONGER_DARKBEASTS,
            "Need More Darkness unlock for 110-135 task sizes");
        flag(requirements, VarbitID.SLAYER_LONGER_CUSTODIANS,
            "Un-restraining Order extends assignments to 200-250.");
    }

    private void flag(Map<String, State> requirements, int varbit, String... names)
    {
        put(requirements, readVarbit(varbit, 1), names);
    }

    private State readVarbit(int varbit, int completedValue)
    {
        try
        {
            VarbitComposition definition = client.getVarbit(varbit);
            int[] varps = client.getVarps();
            if (definition == null || varps == null || definition.getIndex() < 0
                || definition.getIndex() >= varps.length)
            {
                return State.UNKNOWN;
            }
            int value = client.getVarbitValue(varbit);
            // Unexpected future encodings must be reviewed instead of assuming their meaning.
            return value < 0 || value > completedValue ? State.UNKNOWN : state(value == completedValue);
        }
        catch (RuntimeException ignored)
        {
            return State.UNKNOWN;
        }
    }

    private static State state(boolean met)
    {
        return met ? State.MET : State.UNMET;
    }

    private static State not(State value)
    {
        return value == State.UNKNOWN ? State.UNKNOWN : state(value == State.UNMET);
    }

    private static State and(State left, State right)
    {
        if (left == State.UNMET || right == State.UNMET)
        {
            return State.UNMET;
        }
        return left == State.MET && right == State.MET ? State.MET : State.UNKNOWN;
    }

    private static void put(Map<String, State> requirements, State value, String... names)
    {
        for (String name : names)
        {
            requirements.put(name, value);
        }
    }
}

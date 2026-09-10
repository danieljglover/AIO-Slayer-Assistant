package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reviewed preparation rules bundled with the catalogue, never fetched by the client. */
@Data
@NoArgsConstructor
public final class TripPreparationData
{
    private int combatCasts = 500;
    private Map<String, Spell> spells = new LinkedHashMap<>();
    private Map<String, Integer> runeIds = new LinkedHashMap<>();
    private Map<Integer, List<String>> infiniteRunes = new LinkedHashMap<>();
    private Map<Integer, List<String>> combinationRunes = new LinkedHashMap<>();
    private Map<Integer, String> poweredWeapons = new LinkedHashMap<>();
    private Map<String, Teleport> teleports = new LinkedHashMap<>();
    private Map<String, List<String>> masterRoutes = new LinkedHashMap<>();
    private List<String> bankRoutes = new ArrayList<>();
    private List<String> houseRoutes = new ArrayList<>();
    private List<Pouch> pouches = new ArrayList<>();
    private List<Integer> lootingBagIds = new ArrayList<>();
    private Map<Integer, Integer> blightedReplacements = new LinkedHashMap<>();
    private List<String> wildernessEscapeNotes = new ArrayList<>();
    private List<String> lootingBagNotes = new ArrayList<>();
    private List<SlayerCatalogue.Evidence> evidence = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Spell
    {
        private String name;
        private String spellbook;
        private String family;
        private int magicLevel;
        private boolean offensive = true;
        private Map<String, Integer> runes = new LinkedHashMap<>();
        private List<String> requirements = new ArrayList<>();
        private List<Integer> weaponIds = new ArrayList<>();
        private int sackItemId;
        private List<SlayerCatalogue.Evidence> evidence = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Teleport
    {
        private String id;
        private String name;
        private String destination;
        private List<Integer> itemIds = new ArrayList<>();
        private Map<Integer, Integer> charges = new LinkedHashMap<>();
        private String spellName;
        private List<Integer> requiredItemIds = new ArrayList<>();
        private List<String> requirements = new ArrayList<>();
        private int wildernessLimit = 20;
        private boolean emergency;
        private boolean consumable;
        private boolean remoteContact;
        private int escapePriority = 100;
        private boolean bankNearby;
        private String note;
        private List<SlayerCatalogue.Evidence> evidence = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Pouch
    {
        private int itemId;
        private int capacity;
        private int maxPerRune;
        private List<SlayerCatalogue.Evidence> evidence = new ArrayList<>();
    }
}

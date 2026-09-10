package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bundled, authoring-time compiled Slayer knowledge. No live network data is used. */
@Data
@NoArgsConstructor
public class SlayerCatalogue
{
    private Map<String, Master> masters = new LinkedHashMap<>();
    private Map<String, Task> tasks = new LinkedHashMap<>();
    private Map<String, Monster> monsters = new LinkedHashMap<>();
    private Map<String, Location> locations = new LinkedHashMap<>();
    private Map<String, Method> methods = new LinkedHashMap<>();
    private Map<Integer, ItemDefinition> items = new LinkedHashMap<>();
    private Map<Integer, DeathRule> deathRules = new LinkedHashMap<>();
    private Map<String, WildernessArea> wildernessAreas = new LinkedHashMap<>();
    private Map<String, RouteDestination> routeDestinations = new LinkedHashMap<>();
    private TripPreparationData preparation = new TripPreparationData();

    @Data
    @NoArgsConstructor
    public static class RouteDestination
    {
        private String id;
        private List<String> monsterIds = new ArrayList<>();
        private String locationId;
        private String label;
        private String arrival;
        private List<RoutePoint> points = new ArrayList<>();
        private List<Evidence> evidence = new ArrayList<>();
        private String note;
    }

    @Data
    @NoArgsConstructor
    public static class RoutePoint
    {
        private int x;
        private int y;
        private int plane;
    }

    @Data
    @NoArgsConstructor
    public static class Master
    {
        private String id;
        private String name;
        private String location;
        private int combatLevel;
        private int slayerLevel;
        private List<String> requirements = new ArrayList<>();
        private List<String> notes = new ArrayList<>();
        private List<Evidence> evidence = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Task
    {
        private String id;
        private String name;
        private int targetId;
        private int slayerLevel;
        private List<String> masterIds = new ArrayList<>();
        private boolean slayerHelmApplies = true;
        private List<String> monsterIds = new ArrayList<>();
        private List<String> locationIds = new ArrayList<>();
        private List<String> notes = new ArrayList<>();
        private List<String> requirements = new ArrayList<>();
        private Map<String, String> amounts = new LinkedHashMap<>();
        private List<Supply> requiredItems = new ArrayList<>();
        private Map<String, Location> locationOverrides = new LinkedHashMap<>();
        private List<Evidence> evidence = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Monster
    {
        private String id;
        private String name;
        private List<Integer> npcIds = new ArrayList<>();
        private int combatLevel;
        private boolean repeatable = true;
        private List<String> locationIds = new ArrayList<>();
        private List<String> methodIds = new ArrayList<>();
        private List<String> requirements = new ArrayList<>();
        private int slayerLevel;
        private boolean boss;
        private boolean undead;
        private List<Evidence> evidence = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Location
    {
        private String id;
        private String name;
        private String assignmentAreaId;
        private boolean wilderness;
        private boolean multi;
        private boolean cannon;
        private boolean barrage;
        private boolean safespot;
        private boolean taskOnly;
        private List<String> requirements = new ArrayList<>();
        private List<String> notes = new ArrayList<>();
        private List<Supply> travel = new ArrayList<>();
        private List<Supply> requiredItems = new ArrayList<>();
        private List<Evidence> evidence = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Method
    {
        private String id;
        private String name;
        private String style;
        private String role;
        private String summary;
        private List<String> monsterIds = new ArrayList<>();
        private List<String> locationIds = new ArrayList<>();
        private List<String> requirements = new ArrayList<>();
        private List<String> guidance = new ArrayList<>();
        private List<String> risks = new ArrayList<>();
        private Map<String, List<ItemOption>> equipment = new LinkedHashMap<>();
        private boolean equipmentReviewed;
        private List<Supply> requiredItems = new ArrayList<>();
        private List<Supply> inventory = new ArrayList<>();
        private List<Supply> switches = new ArrayList<>();
        private int xpRank;
        private int profitRank;
        private int effortRank;
        private String rankingReason;
        private boolean cannon;
        private boolean barrage;
        private boolean group;
        private boolean taskOnly;
        private boolean selectable = true;
        private String coverageStatus = "Inherited guidance; completeness not verified";
        private List<Evidence> evidence = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class ItemOption
    {
        private String name;
        private List<Integer> itemIds = new ArrayList<>();
        private List<List<Integer>> requires = new ArrayList<>();
        private List<String> requirements = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Supply
    {
        private String name;
        private List<Integer> itemIds = new ArrayList<>();
        private int quantity = 1;
        private boolean required;
        private boolean stackable;
        private String waiverRequirement;
    }

    @Data
    @NoArgsConstructor
    public static class ItemDefinition
    {
        private int id;
        private String name;
        private String slot;
        private String style;
        private Map<String, Integer> levels = new LinkedHashMap<>();
        private List<String> requirements = new ArrayList<>();
        private boolean usable = true;
        private boolean requirementsKnown;
        private CombatBonus combatBonus = CombatBonus.NONE;
    }

    @Data
    @NoArgsConstructor
    public static class Evidence
    {
        private String url;
        private String title;
        private String timestamp;
        private long pageId;
        private long revisionId;
        private boolean missing;
    }
}

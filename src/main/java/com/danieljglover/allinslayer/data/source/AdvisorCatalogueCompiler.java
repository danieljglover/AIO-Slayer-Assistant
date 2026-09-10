package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.danieljglover.allinslayer.model.advisor.CombatBonus;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Compile modular source and explicit authoring evidence into the passive advisor graph. */
public final class AdvisorCatalogueCompiler
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> SLOTS = new LinkedHashSet<>(Arrays.asList(
        "HEAD", "CAPE", "AMULET", "WEAPON", "BODY", "SHIELD", "LEGS", "HANDS", "FEET", "RING", "AMMO"));
    private static final Set<String> SUPPORT = new LinkedHashSet<>(Arrays.asList(
        "rules", "support", "routing", "travel", "inventory", "mechanics", "trip-planning", "drops",
        "risk", "caution", "hazards", "source-context", "task-management", "banking", "skip", "avoid",
        "access", "finisher", "phase", "phase-one", "phase-two", "phase-three", "phase-four",
        "boss-phase", "phase-management", "specials", "utility", "special", "rotation", "overview",
        "location", "konar", "konar-location", "required", "requirements", "decision", "optimization", "equipment", "prayer-training"));
    private final Path root;
    private final SlayerCatalogue catalogue = new SlayerCatalogue();
    private final Map<String, JsonObject> items = new LinkedHashMap<>();
    private final Map<String, JsonObject> strategies = new LinkedHashMap<>();
    private final Map<String, JsonObject> monsterSources = new LinkedHashMap<>();
    private final Map<String, String> strategyPaths = new LinkedHashMap<>();
    private final Map<String, List<String>> methodLocationExclusions = new LinkedHashMap<>();
    private final Map<String, JsonObject> weapons = new LinkedHashMap<>();
    private final Map<String, Integer> unresolved = new LinkedHashMap<>();
    private JsonObject audit;
    private JsonObject wikiEquipment;
    private JsonObject overrides;
    private JsonObject spells;
    private JsonObject equipmentLinks;

    private AdvisorCatalogueCompiler(Path root)
    {
        this.root = root;
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length != 2)
        {
            throw new IllegalArgumentException("usage: AdvisorCatalogueCompiler <sourceRoot> <outputJson>");
        }
        AdvisorCatalogueCompiler compiler = new AdvisorCatalogueCompiler(Paths.get(args[0]));
        SlayerCatalogue result = compiler.compile();
        Path output = Paths.get(args[1]);
        Files.createDirectories(output.getParent());
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8))
        {
            GSON.toJson(result, writer);
        }
        Path coverage = output.resolveSibling("advisor-coverage.json");
        JsonObject report = new JsonObject();
        report.addProperty("tasks", result.getTasks().size());
        report.addProperty("monsters", result.getMonsters().size());
        report.addProperty("methods", result.getMethods().size());
        report.addProperty("itemDefinitions", result.getItems().size());
        report.addProperty("selectableMethods", result.getMethods().values().stream().filter(Method::isSelectable).count());
        report.addProperty("methodsWithAllSlots", result.getMethods().values().stream()
            .filter(m -> m.getEquipment().keySet().containsAll(SLOTS)).count());
        report.add("unresolvedNames", GSON.toJsonTree(compiler.unresolved));
        try (Writer writer = Files.newBufferedWriter(coverage, StandardCharsets.UTF_8))
        {
            GSON.toJson(report, writer);
        }
        System.out.println("Advisor catalogue: " + result.getMasters().size() + " masters, "
            + result.getTasks().size() + " tasks, " + result.getMonsters().size() + " variants, "
            + result.getMethods().size() + " methods; " + compiler.unresolved.size()
            + " unresolved authored item labels retained as guidance (see advisor-coverage.json)");
    }

    private SlayerCatalogue compile() throws IOException
    {
        audit = read(root.resolve("advisor/wiki-audit.json"));
        wikiEquipment = object(read(root.resolve("advisor/wiki-equipment.json")), "pages");
        overrides = read(root.resolve("advisor/overrides.json"));
        spells = object(read(root.resolve("advisor/spells.json")), "spells");
        equipmentLinks = read(root.resolve("advisor/equipment-links.json"));
        loadItems();
        loadCombatBonuses();
        loadMasters();
        loadLocations();
        DeathRuleCompiler.load(root, catalogue);
        loadMonsters();
        loadTasks();
        loadStrategies();
        RouteDestinationCompiler.load(root, catalogue);
        TripPreparationCompiler.load(root, catalogue);
        validate();
        return catalogue;
    }

    private void loadItems() throws IOException
    {
        JsonObject authoredItems = object(read(root.resolve("advisor/equipment-items.json")), "items");
        for (Map.Entry<String, JsonElement> entry : authoredItems.entrySet())
        {
            JsonObject source = entry.getValue().getAsJsonObject();
            items.put(key(entry.getKey()), source);
            items.putIfAbsent(key(string(source, "name")), source);
        }
        for (String directory : Arrays.asList("items", "weapons"))
        {
            for (Path path : files(directory, ".json"))
            {
                JsonObject source = read(path);
                JsonObject resolved = items.get(key(string(source, "name")));
                if (resolved == null)
                {
                    resolved = source;
                    items.put(key(string(source, "name")), source);
                }
                String id = string(source, directory.equals("items") ? "itemKey" : "weaponId");
                items.put(key(id), resolved);
                for (String alias : strings(source, "aliases"))
                {
                    items.putIfAbsent(key(alias), resolved);
                }
                if (directory.equals("weapons"))
                {
                    weapons.put(id, resolved);
                }
            }
        }
        JsonObject itemOverrides = object(overrides, "items");
        for (Map.Entry<String, JsonElement> entry : itemOverrides.entrySet())
        {
            JsonObject source = items.get(key(entry.getKey()));
            if (source == null)
            {
                source = new JsonObject();
                source.addProperty("name", entry.getKey());
                items.put(key(entry.getKey()), source);
            }
            for (Map.Entry<String, JsonElement> field : entry.getValue().getAsJsonObject().entrySet())
            {
                source.add(field.getKey(), field.getValue());
            }
        }
        for (Map.Entry<String, JsonElement> entry : object(overrides, "aliases").entrySet())
        {
            JsonObject source = items.get(key(entry.getValue().getAsString()));
            if (source != null)
            {
                items.put(key(entry.getKey()), source);
            }
        }
        for (JsonObject source : new LinkedHashSet<>(items.values()))
        {
            for (int id : integers(source, "itemIds"))
            {
                ItemDefinition item = new ItemDefinition();
                item.setId(id);
                item.setName(string(source, "name"));
                item.setSlot(string(source, "slot"));
                item.setStyle("ANY");
                item.setUsable(true);
                item.setRequirementsKnown(bool(source, "requirementsKnown"));
                item.setRequirements(strings(source, "requirements"));
                for (Map.Entry<String, JsonElement> level : object(source, "levels").entrySet())
                {
                    item.getLevels().put(level.getKey().toUpperCase(Locale.ROOT), level.getValue().getAsInt());
                }
                catalogue.getItems().putIfAbsent(id, item);
            }
        }
    }

    private void loadCombatBonuses() throws IOException
    {
        for (JsonElement raw : array(read(root.resolve("advisor/combat-bonuses.json")), "families"))
        {
            JsonObject source = raw.getAsJsonObject();
            CombatBonus bonus = CombatBonus.valueOf(string(source, "bonus"));
            if (bonus == CombatBonus.NONE || integers(source, "itemIds").isEmpty()
                || evidenceList(source).isEmpty())
            {
                throw new IllegalArgumentException("Combat bonus family needs an effect, item IDs and evidence");
            }
            for (int id : integers(source, "itemIds"))
            {
                ItemDefinition item = catalogue.getItems().get(id);
                String slot = bonus.isSlayer() ? "HEAD" : "AMULET";
                if (item == null || !slot.equals(item.getSlot()))
                {
                    throw new IllegalArgumentException("Combat bonus references an unknown or wrong-slot item: " + id);
                }
                if (item.getCombatBonus() != CombatBonus.NONE)
                {
                    throw new IllegalArgumentException("Duplicate combat bonus item ID: " + id);
                }
                item.setCombatBonus(bonus);
            }
        }
    }

    private void loadMasters() throws IOException
    {
        for (Path path : files("masters", ".json"))
        {
            JsonObject source = read(path);
            Master master = new Master();
            master.setId(string(source, "masterId"));
            master.setName(string(source, "name"));
            master.setLocation(string(source, "location"));
            JsonObject requirements = object(source, "requirements");
            master.setCombatLevel(number(requirements, "combatLevel"));
            master.setSlayerLevel(number(requirements, "slayerLevel"));
            master.setRequirements(strings(requirements, "quests"));
            master.setNotes(strings(source, "notes"));
            master.setEvidence(evidence(path));
            put(catalogue.getMasters(), master.getId(), master);
        }
    }

    private void loadLocations() throws IOException
    {
        for (Path path : files("locations", ".json"))
        {
            JsonObject source = read(path);
            Location location = new Location();
            location.setId(string(source, "locationId"));
            location.setName(string(source, "name"));
            location.setAssignmentAreaId(string(source, "assignmentAreaId"));
            location.setWilderness(bool(source, "wilderness"));
            location.setMulti(bool(source, "multi"));
            location.setCannon(bool(source, "cannon"));
            location.setBarrage(bool(source, "burst"));
            location.setSafespot(bool(source, "safeSpot"));
            add(location.getNotes(), string(source, "accessNote"));
            JsonObject travel = object(source, "travel");
            add(location.getNotes(), string(travel, "note"));
            for (JsonElement raw : array(travel, "items"))
            {
                JsonObject item = raw.getAsJsonObject();
                Supply supply = supply(string(item, "itemKey"), false);
                supply.setQuantity(Math.max(1, number(item, "quantity")));
                location.getTravel().add(supply);
            }
            location.getRequirements().addAll(strings(source, "advisorRequirements"));
            location.setTaskOnly(bool(source, "taskOnly"));
            location.setEvidence(evidence(path));
            location.getEvidence().addAll(evidenceList(source));
            JsonObject patched = GSON.toJsonTree(location).getAsJsonObject();
            for (Map.Entry<String, JsonElement> field : object(object(overrides, "locations"), location.getId()).entrySet()) patched.add(field.getKey(), field.getValue());
            location = GSON.fromJson(patched, Location.class);
            put(catalogue.getLocations(), location.getId(), location);
        }
    }

    private void loadMonsters() throws IOException
    {
        for (Path path : files("monsters", ".json"))
        {
            JsonObject source = read(path);
            Monster monster = new Monster();
            monster.setId(string(source, "variantId"));
            monster.setName(string(source, "name"));
            monster.setNpcIds(integers(source, "npcIds"));
            monster.setCombatLevel(number(source, "combatLevel"));
            monster.setRepeatable(!source.has("advisorRepeatable") || bool(source, "advisorRepeatable"));
            add(monster.getLocationIds(), string(source, "locationId"));
            monster.setBoss(bool(source, "boss"));
            monster.setUndead(bool(source, "undead"));
            // Requirement prose often contains alternative locations. Preserve it on methods;
            // only explicit variant requirements are eligible to block selection.
            String requirement = string(source, "requirement");
            if (!requirement.isEmpty())
            {
                monster.getRequirements().add(requirement);
            }
            if (source.has("slayerLevel")) monster.setSlayerLevel(number(source, "slayerLevel"));
            monster.setEvidence(evidence(path));
            monster.getEvidence().addAll(evidenceList(source));
            put(catalogue.getMonsters(), monster.getId(), monster);
            monsterSources.put(monster.getId(), source);
        }
    }

    private void loadTasks() throws IOException
    {
        for (Path path : files("tasks", ".json"))
        {
            JsonObject source = read(path);
            Task task = new Task();
            task.setId(string(source, "taskId"));
            task.setName(string(source, "name"));
            task.setTargetId(number(source, "slayerTargetId"));
            task.setSlayerLevel(number(source, "slayerLevel"));
            task.setSlayerHelmApplies(!source.has("slayerHelmApplies") || bool(source, "slayerHelmApplies"));
            task.setMasterIds(strings(source, "masterIds"));
            task.setMonsterIds(strings(source, "variantIds"));
            task.setLocationIds(strings(source, "locationIds"));
            task.setRequirements(strings(source, "questReqs"));
            task.setNotes(strings(source, "taskNotes"));
            add(task.getNotes(), string(source, "recommendedMethod"));
            for (Map.Entry<String, JsonElement> entry : object(source, "amountByMaster").entrySet())
            {
                JsonArray range = entry.getValue().getAsJsonArray();
                String amount = range.get(0).getAsString();
                if (range.size() > 1 && !range.get(0).equals(range.get(1)))
                {
                    amount += "-" + range.get(1).getAsString();
                }
                task.getAmounts().put(entry.getKey(), amount);
            }
            int requiredId = number(source, "requiredItemId");
            String requiredName = string(source, "requiredItemName");
            if (requiredId > 0)
            {
                Supply item = supply(requiredName, true);
                if (item.getItemIds().isEmpty())
                {
                    item.getItemIds().add(requiredId);
                }
                task.getRequiredItems().add(item);
            }
            else if (!requiredName.isEmpty())
            {
                add(task.getNotes(), "Required protection or damage restriction: " + requiredName);
            }
            for (JsonElement raw : array(source, "locationComparison"))
            {
                JsonObject row = raw.getAsJsonObject();
                String locationId = string(row, "locationId");
                Location base = catalogue.getLocations().get(locationId);
                if (base == null)
                {
                    throw new IllegalArgumentException("Unknown task location " + locationId);
                }
                Location local = GSON.fromJson(GSON.toJson(base), Location.class);
                if (row.has("cannonable")) local.setCannon(bool(row, "cannonable"));
                if (row.has("multicombat")) local.setMulti(bool(row, "multicombat"));
                if (row.has("safespottable")) local.setSafespot(bool(row, "safespottable"));
                local.getNotes().addAll(strings(row, "notes"));
                task.getLocationOverrides().put(locationId, local);
            }
            JsonObject patchedTask = GSON.toJsonTree(task).getAsJsonObject();
            for (Map.Entry<String, JsonElement> field : object(object(overrides, "tasks"), task.getId()).entrySet()) patchedTask.add(field.getKey(), field.getValue());
            task = GSON.fromJson(patchedTask, Task.class);
            task.setEvidence(evidence(path));
            put(catalogue.getTasks(), task.getId(), task);
            // A variant's source location is an example, not an exhaustive location list.
            // Exact per-variant task table names narrow the graph before shared family fallback.
            for (String monsterId : task.getMonsterIds())
            {
                Monster monster = catalogue.getMonsters().get(monsterId);
                if (monster == null) throw new IllegalArgumentException("Unknown task variant " + monsterId);
                if (!task.getId().equals("boss"))
                {
                    monster.setSlayerLevel(Math.max(monster.getSlayerLevel(), task.getSlayerLevel()));
                }
                for (JsonElement raw : array(source, "variantInfo"))
                {
                    JsonObject row = raw.getAsJsonObject();
                    if (!monsterId.equals(string(row, "variantId"))) continue;
                    for (String name : strings(row, "locations"))
                    {
                        for (String locationId : task.getLocationIds())
                        {
                            Location location = catalogue.getLocations().get(locationId);
                            if (location != null && key(location.getName()).equals(key(name)))
                            {
                                add(monster.getLocationIds(), locationId);
                            }
                        }
                    }
                }
                if (monster.getLocationIds().isEmpty() && !monster.isBoss())
                {
                    monster.getLocationIds().addAll(task.getLocationIds());
                }
            }
        }
    }

    private void loadStrategies() throws IOException
    {
        for (Path path : files("strategies", ".json"))
        {
            JsonObject source = read(path);
            String id = string(source, "strategyId");
            put(strategies, id, source);
            strategyPaths.put(id, root.relativize(path).toString());
        }
        for (Path path : files("strategies", ".md"))
        {
            SourceStrategy parsed = StrategyMarkdownParser.parse(path.toString(), Files.readString(path));
            JsonObject source = GSON.toJsonTree(parsed).getAsJsonObject();
            if (!strategies.containsKey(parsed.getStrategyId()))
            {
                strategies.put(parsed.getStrategyId(), source);
                strategyPaths.put(parsed.getStrategyId(), root.relativize(path).toString());
            }
        }
        for (Map.Entry<String, JsonObject> entry : strategies.entrySet())
        {
            String strategyId = entry.getKey();
            JsonObject source = entry.getValue();
            List<String> variants = strings(source, "variantIds");
            for (Map.Entry<String, JsonObject> monster : monsterSources.entrySet())
            {
                if (strategyId.equals(string(monster.getValue(), "strategyId"))) add(variants, monster.getKey());
            }
            variants.removeIf(id -> !catalogue.getMonsters().containsKey(id));
            JsonObject plugin = source.has("plugin") ? object(source, "plugin") : source;
            List<Method> methods = new ArrayList<>();
            for (String kind : Arrays.asList("methods", "styleOptions"))
            {
                int index = 0;
                for (JsonElement raw : array(source, kind))
                {
                    JsonObject authored = raw.getAsJsonObject();
                    String localId = string(authored, kind.equals("methods") ? "methodId" : "styleId");
                    if (localId.isEmpty()) localId = "entry-" + (++index);
                    Method method = method(strategyId + (kind.equals("methods") ? "-" : "-equipment-") + localId,
                        authored, plugin, source, variants);
                    methods.add(method);
                }
            }
            if (methods.isEmpty())
            {
                JsonObject generic = new JsonObject();
                generic.addProperty("label", strategyId.replace('-', ' '));
                generic.addProperty("summary", string(plugin, "note"));
                generic.addProperty("combatStyle", string(plugin, "primaryStyle"));
                methods.add(method(strategyId + "-general", generic, plugin, source, variants));
            }
            String page = wikiTitle(string(source, "sourceUrl"));
            for (Evidence evidence : evidence(root.resolve(strategyPaths.get(strategyId))))
            {
                if (!evidence.isMissing() && wikiEquipment.has(evidence.getTitle())) page = evidence.getTitle();
            }
            JsonArray tables = array(wikiEquipment, page);
            for (JsonElement tableRaw : tables)
            {
                JsonObject table = tableRaw.getAsJsonObject();
                JsonObject authored = new JsonObject();
                authored.addProperty("label", string(table, "name"));
                authored.addProperty("combatStyle", string(table, "style"));
                authored.addProperty("role", "wiki-equipment");
                authored.addProperty("summary", "Equipment priorities from " + page + ": " + string(table, "name"));
                Method method = method(strategyId + "-wiki-equipment-" + number(table, "index"), authored, plugin, source, variants);
                method.setEquipment(equipment(object(table, "equipment"), true));
                method.getSwitches().addAll(supplies(strings(table, "switchNames"), false));
                method.setEvidence(evidenceList(table));
                method.setCoverageStatus("Current wiki equipment table imported; method context and full prose require review");
                method.setSelectable(false);
                method.getGuidance().addAll(strings(table, "unresolvedCells"));
                methods.add(method);
            }
            // Match one same-style current table only where unambiguous. Multiple boss roles
            // must keep their own tables; arbitrary same-style inheritance creates invalid sets.
            for (Method method : methods)
            {
                if (!method.getRole().equals("wiki-equipment") && method.getEquipment().size() < 8
                    && bool(source, "sameStyleEquipmentReviewed"))
                {
                    List<JsonObject> matches = new ArrayList<>();
                    for (JsonElement raw : tables)
                    {
                        JsonObject table = raw.getAsJsonObject();
                        if (method.getStyle().equals(string(table, "style"))) matches.add(table);
                    }
                    if (matches.size() == 1)
                    {
                        Map<String, List<ItemOption>> complete = equipment(object(matches.get(0), "equipment"), true);
                        method.getEquipment().forEach(complete::put);
                        method.setEquipment(complete);
                        method.setCoverageStatus("Authored method with same-style wiki equipment; context review incomplete");
                    }
                }
                if (method.getEquipment().getOrDefault("WEAPON", Collections.emptyList()).isEmpty())
                {
                    List<String> weaponNames = new ArrayList<>();
                    if (method.getStyle().equals(string(plugin, "primaryStyle")))
                    {
                        weaponNames.addAll(strings(plugin, "primaryWeapons"));
                    }
                    for (JsonElement raw : array(plugin, "secondaryWeapons"))
                    {
                        JsonObject weapon = raw.getAsJsonObject();
                        if (method.getStyle().equals(string(weapon, "style"))) add(weaponNames, string(weapon, "weaponId"));
                    }
                    if (!weaponNames.isEmpty()) method.getEquipment().put("WEAPON", options(weaponNames));
                }
                attachEquipment(method, page);
                enforceNamedEquipment(method);
                JsonObject methodOverride = object(object(overrides, "methods"), method.getId());
                if (methodOverride.size() > 0)
                {
                    JsonObject combined = GSON.toJsonTree(method).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> field : methodOverride.entrySet()) combined.add(field.getKey(), field.getValue());
                    method = GSON.fromJson(combined, Method.class);
                }
                finishMethodLocations(method);
                if (method.isSelectable())
                {
                    List<String> repeatable = method.getMonsterIds().stream()
                        .filter(id -> catalogue.getMonsters().get(id).isRepeatable()).collect(Collectors.toList());
                    if (repeatable.isEmpty())
                    {
                        method.setSelectable(false);
                        method.getGuidance().add("Incidental encounter guidance; this is not a repeatable preparation target.");
                    }
                    else
                    {
                        method.setMonsterIds(repeatable);
                    }
                }
                put(catalogue.getMethods(), method.getId(), method);
                for (String variant : method.getMonsterIds()) add(catalogue.getMonsters().get(variant).getMethodIds(), method.getId());
            }
        }
        for (Monster monster : catalogue.getMonsters().values())
        {
            if (monster.getMethodIds().isEmpty())
            {
                Method method = new Method();
                method.setId(monster.getId() + "-source-gap");
                method.setName(monster.isRepeatable() ? "Strategy coverage pending" : "Incidental encounter");
                method.setStyle("MELEE");
                method.setRole("source-context");
                method.setSummary(monster.isRepeatable() ? "No authored strategy is linked to this combat variant."
                    : "This encounter can count towards the task, but is not a repeatable preparation target.");
                method.setSelectable(false);
                method.getMonsterIds().add(monster.getId());
                method.getLocationIds().addAll(monster.getLocationIds());
                method.setEvidence(monster.getEvidence());
                catalogue.getMethods().put(method.getId(), method);
                monster.getMethodIds().add(method.getId());
            }
        }
    }

    private void enforceNamedEquipment(Method method)
    {
        if (!method.isSelectable()) return;
        String label = method.getName().toLowerCase(Locale.ROOT);
        if (label.contains("guthan"))
        {
            String[][] set = {{"HEAD", "Guthan's helm"}, {"BODY", "Guthan's platebody"},
                {"LEGS", "Guthan's chainskirt"}, {"WEAPON", "Guthan's warspear"}};
            for (String[] slot : set)
            {
                method.getEquipment().put(slot[0], options(Collections.singletonList(slot[1])));
                if (method.getRequiredItems().stream().noneMatch(item -> item.getName().equals(slot[1]))) method.getRequiredItems().add(supply(slot[1], true));
            }
        }
        if (label.contains("venator") && !method.getMonsterIds().contains("venator")) method.getRequiredItems().add(supply("Venator bow", true));
        if (label.contains("scorching")) method.getRequiredItems().add(supply("Scorching bow", true));
        if (label.contains("bowfa")) method.getRequiredItems().add(supply("Bow of Faerdhinen or Bow of Faerdhinen (c)", true));
    }

    private void attachEquipment(Method method, String page)
    {
        if (method.getRole().equals("wiki-equipment") || !method.isSelectable() || method.isEquipmentReviewed()) return;
        JsonObject link = object(object(equipmentLinks, "pages"), page);
        int index = number(object(link, "styles"), method.getStyle());
        String context = (method.getId() + " " + method.getName()).toLowerCase(Locale.ROOT);
        for (JsonElement selectorRaw : array(link, "selectors"))
        {
            JsonObject selector = selectorRaw.getAsJsonObject();
            if (context.contains(string(selector, "contains")) && method.getStyle().equals(string(selector, "style"))) index = number(selector, "index");
        }
        boolean general = false;
        if (index == 0 && method.getMonsterIds().stream().noneMatch(id -> catalogue.getMonsters().get(id).isBoss()))
        {
            link = object(equipmentLinks, "general");
            index = number(link, method.getStyle());
            page = string(link, "page");
            general = true;
        }
        for (JsonElement tableRaw : array(wikiEquipment, page))
        {
            JsonObject table = tableRaw.getAsJsonObject();
            if (number(table, "index") != index) continue;
            Map<String, List<ItemOption>> complete = equipment(object(table, "equipment"), true);
            // A method's own named weapon/protection takes precedence over a general style
            // table. Other slots retain current wiki priority order from the bound table.
            for (String slot : Arrays.asList("WEAPON", "SHIELD", "AMMO"))
            {
                List<ItemOption> authored = method.getEquipment().get(slot);
                if (authored != null && !authored.isEmpty()) complete.put(slot, authored);
            }
            method.setEquipment(complete);
            method.getEvidence().addAll(evidenceList(table));
            method.getSwitches().addAll(supplies(strings(table, "switchNames"), false));
            method.getGuidance().addAll(strings(table, "unresolvedCells"));
            method.setCoverageStatus(general
                ? "General Slayer training equipment with explicit task constraints; method prose coverage remains under audit"
                : "Current wiki equipment priorities linked by reviewed style/role context; full method prose coverage remains under audit");
            if (general) method.getGuidance().add("Equipment priorities use the Wiki's general Slayer training " + method.getStyle().toLowerCase(Locale.ROOT) + " table because this source has no dedicated equipment grid.");
            break;
        }
    }

    private Method method(String id, JsonObject source, JsonObject plugin, JsonObject strategy, List<String> variants)
    {
        Method method = new Method();
        method.setId(id.toLowerCase(Locale.ROOT).replace('_', '-'));
        method.setName(first(string(source, "label"), string(source, "name"), id.replace('-', ' ')));
        method.setStyle(first(string(source, "combatStyle"), string(source, "style"), string(plugin, "primaryStyle"), "MELEE"));
        method.setRole(first(string(source, "role"), "general"));
        boolean mixedStyle = !Arrays.asList("MELEE", "RANGED", "MAGIC").contains(method.getStyle());
        if (mixedStyle) method.setStyle(first(string(plugin, "primaryStyle"), "MELEE"));
        if (!Arrays.asList("MELEE", "RANGED", "MAGIC").contains(method.getStyle())) method.setStyle("MELEE");
        method.setSummary(first(string(source, "summary"), string(source, "description"), string(plugin, "note")));
        method.getMonsterIds().addAll(variants);
        for (String variant : variants) for (String location : catalogue.getMonsters().get(variant).getLocationIds()) add(method.getLocationIds(), location);
        method.setSelectable(!mixedStyle && !SUPPORT.contains(method.getRole()));
        String intent = (id + " " + method.getName()).toLowerCase(Locale.ROOT);
        if (intent.contains("skip") || intent.contains("requirements-and-access") || intent.contains("location-handling")
            || intent.contains("task-management") || intent.contains("access-and-travel") || intent.contains("inventory-and")
            || intent.contains("-travel") || intent.contains("-transportation") || intent.contains("-killcount")) method.setSelectable(false);
        narrowMethodLinks(method);
        method.getGuidance().addAll(strings(strategy, "requirements"));
        method.getGuidance().addAll(strings(strategy, "mechanics"));
        for (String field : Arrays.asList("recommendedFor", "steps", "notes", "fallbacks")) method.getGuidance().addAll(strings(source, field));
        for (String prayer : strings(source, "prayers")) add(method.getGuidance(), "Preparation prayer reference: " + prayer);
        method.setRisks(strings(source, "risks"));
        method.setEquipment(equipment(object(object(source, "equipment"), "slots"), false));
        for (Map.Entry<String, JsonElement> slot : object(object(source, "equipment"), "slots").entrySet())
        {
            if (slot.getKey().equalsIgnoreCase("special")) method.getSwitches().addAll(supplies(strings(object(object(source, "equipment"), "slots"), slot.getKey()), false));
        }
        for (String raw : strings(source, "requiredOrKeyItems"))
        {
            Supply supply = supply(raw, false);
            if (supply.getItemIds().isEmpty()) add(method.getGuidance(), "Key item or prerequisite: " + raw);
            else method.getInventory().add(supply);
        }
        method.getInventory().addAll(supplies(strings(source, "inventory"), false));
        String context = (id + " " + method.getName() + " " + method.getRole()).toLowerCase(Locale.ROOT);
        method.setGroup(context.matches(".*\\b(tank|attacker|duo|group|team)\\b.*"));
        method.setCannon(context.contains("cannon"));
        method.setBarrage(context.contains("barrage") || context.contains("barrag") || context.contains("burst"));
        // Qualitative tiers are intentionally not calculated XP/GP rates. The reason is shown.
        if (method.isSelectable())
        {
            method.setXpRank(method.isBarrage() || method.isCannon() || context.contains("speed") ? 1 : 2);
            method.setProfitRank(context.contains("profit") ? 1 : 2);
            method.setEffortRank(context.contains("low-attention") || context.contains("afk") || context.contains("safespot") ? 1 : method.isBarrage() || method.isGroup() ? 3 : 2);
            method.setRankingReason("Qualitative authored-method tier: area damage/speed for XP; explicit profit methods for profit; safespot/low-attention methods for effort. No measured XP or GP rates.");
        }
        String path = strategyPaths.get(string(strategy, "strategyId"));
        method.setEvidence(evidence(path == null ? root : root.resolve(path)));
        if (method.getEvidence().isEmpty())
        {
            Evidence evidence = new Evidence();
            evidence.setUrl(string(strategy, "sourceUrl"));
            evidence.setTitle(wikiTitle(string(strategy, "sourceUrl")));
            method.getEvidence().add(evidence);
        }
        JsonObject authored = object(source, "advisor");
        if (authored.size() > 0)
        {
            JsonObject combined = GSON.toJsonTree(method).getAsJsonObject();
            for (Map.Entry<String, JsonElement> field : authored.entrySet()) combined.add(field.getKey(), field.getValue());
            method = GSON.fromJson(combined, Method.class);
        }
        Set<String> availableLocations = new LinkedHashSet<>();
        for (String variant : method.getMonsterIds()) availableLocations.addAll(catalogue.getMonsters().get(variant).getLocationIds());
        method.getLocationIds().retainAll(availableLocations);
        methodLocationExclusions.put(method.getId(), strings(authored, "excludeLocationIds"));
        applyDamageConstraints(method);
        applyCastingRequirements(method);
        if (method.isCannon())
        {
            for (String part : Arrays.asList("Cannon base", "Cannon stand", "Cannon barrels", "Cannon furnace")) method.getRequiredItems().add(supply(part, true));
            Supply ammunition = supply("Cannonball", true);
            ammunition.setQuantity(1000);
            method.getRequiredItems().add(ammunition);
        }
        for (List<Supply> supplies : Arrays.asList(method.getRequiredItems(), method.getInventory(), method.getSwitches()))
        {
            for (Supply item : supplies)
            {
                if (item.getItemIds().isEmpty())
                {
                    Supply resolved = supply(item.getName(), item.isRequired());
                    item.setItemIds(resolved.getItemIds());
                    item.setStackable(resolved.isStackable());
                }
                if (item.isRequired() && item.getItemIds().isEmpty())
                {
                    method.setSelectable(false);
                    method.getRisks().add("Mandatory item has no resolved usable item IDs: " + item.getName());
                }
            }
        }
        return method;
    }

    private void finishMethodLocations(Method method)
    {
        List<String> excludedLocations = methodLocationExclusions.getOrDefault(method.getId(), Collections.emptyList());
        for (String excluded : excludedLocations)
        {
            if (!catalogue.getLocations().containsKey(excluded))
                throw new IllegalArgumentException(method.getId() + ": unknown excluded location " + excluded);
        }
        method.getLocationIds().removeAll(excludedLocations);
        // Compilation resolves locations explicitly. An empty intersection must not turn
        // back into unrestricted locations through the runtime's legacy empty-list fallback.
        if (method.getLocationIds().isEmpty()) method.setSelectable(false);
    }

    private void applyCastingRequirements(Method method)
    {
        if (!method.getStyle().equals("MAGIC")) return;
        String text = (method.getId() + " " + method.getName() + " " + method.getSummary()).toLowerCase(Locale.ROOT);
        if (!text.contains("barrage") && !text.contains("burst"))
        {
            return;
        }
        method.setBarrage(true);
        String spellName;
        if (text.contains("smoke")) spellName = text.contains("burst") ? "Smoke Burst" : "Smoke Barrage";
        else if (text.contains("blood")) spellName = text.contains("burst") ? "Blood Burst" : "Blood Barrage";
        else if (text.contains("shadow")) spellName = text.contains("burst") ? "Shadow Burst" : "Shadow Barrage";
        else spellName = text.contains("burst") ? "Ice Burst" : "Ice Barrage";
        JsonObject spell = object(spells, spellName);
        if (spell.size() == 0)
        {
            method.setSelectable(false);
            method.getRisks().add("Casting requirements and rune quantities have not been resolved.");
            return;
        }
        method.getRequirements().add("MAGIC >= " + number(spell, "magicLevel"));
        method.getRequirements().add("Desert Treasure I");
        method.getRequirements().add("Ancient Magicks spellbook selected");
        method.getGuidance().add("Spell preparation baseline: " + spellName + "; rune quantities cover 500 casts before rune-saving or infinite-rune equipment. Higher spells in the guidance require their own level and runes.");
        for (Map.Entry<String, JsonElement> rune : object(spell, "runes").entrySet())
        {
            Supply supply = supply(rune.getKey(), true);
            supply.setQuantity(rune.getValue().getAsInt() * 500);
            method.getRequiredItems().add(supply);
        }
        method.getEvidence().addAll(evidenceList(spell));
    }

    private void applyDamageConstraints(Method method)
    {
        String identities = String.join(" ", method.getMonsterIds());
        if (identities.contains("kurask") || identities.contains("turoth"))
        {
            String weapons = method.getStyle().equals("MELEE")
                ? "Leaf-bladed battleaxe or Leaf-bladed sword or Leaf-bladed spear"
                : method.getStyle().equals("RANGED") ? "Rune crossbow or Armadyl crossbow or Zaryte crossbow or Magic shortbow (i)"
                : "Slayer's staff or Staff of the dead or Toxic staff of the dead";
            method.getRequiredItems().add(supply(weapons, true));
            if (method.getStyle().equals("RANGED"))
            {
                Supply ammo = supply("Broad bolts or Amethyst broad bolts or Broad arrows", true);
                ammo.setQuantity(1000);
                method.getRequiredItems().add(ammo);
            }
            if (method.getStyle().equals("MAGIC"))
            {
                method.getGuidance().add("Spell preparation baseline: Magic Dart; this monster requires Magic Dart for magic damage.");
            }
        }
        if (identities.contains("venator") && !identities.contains("bow"))
        {
            method.getRequiredItems().add(supply(method.getStyle().equals("RANGED") ? "Blisterwood stake"
                : "Sunspear or Hallowed flail or Blisterwood flail or Ivandis flail", true));
        }
        if (identities.contains("vyrewatch-sentinel")) method.getRequiredItems().add(supply("Blisterwood flail or Hallowed flail or Sunspear", true));
        if (!method.getMonsterIds().isEmpty() && method.getMonsterIds().stream().allMatch(id ->
            id.contains("cerberus") || id.contains("alchemical-hydra") || id.contains("araxxor")
                || id.contains("thermonuclear") || id.equals("kraken") || id.equals("boss-kraken") || id.equals("cave-kraken-kraken"))) method.setTaskOnly(true);
    }

    private void narrowMethodLinks(Method method)
    {
        String context = (method.getName() + " " + method.getId()).toLowerCase(Locale.ROOT);
        String[] areaTokens = {"catacombs", "wilderness", "iorwerth", "stronghold", "slayer-tower", "karuulm", "fremennik", "chasm", "brimhaven", "taverley", "mourner", "waterbirth", "lighthouse", "lithkren", "myths", "curtain", "prifddinas"};
        for (String token : areaTokens)
        {
            if (!context.replace(' ', '-').contains(token)) continue;
            List<String> matching = method.getLocationIds().stream().filter(id ->
                token.equals("wilderness")
                    ? catalogue.getLocations().get(id).isWilderness() != context.replace(' ', '-').contains("non-wilderness")
                    : (id + " " + catalogue.getLocations().get(id).getName()).toLowerCase(Locale.ROOT).replace(' ', '-').contains(token))
                .collect(Collectors.toList());
            if (!matching.isEmpty()) method.setLocationIds(matching);
            else
            {
                method.setSelectable(false);
                method.getRisks().add("Location-specific method has no resolved matching source location.");
            }
        }
        for (String form : Arrays.asList("greater", "mutated", "brutal", "baby", "wyrmling", "lava", "shadow", "elder", "feral", "sentinel", "tormented", "demonic"))
        {
            if (!context.contains(form)) continue;
            List<String> candidates = method.getMonsterIds().stream().filter(id -> id.contains(form)).collect(Collectors.toList());
            if (!candidates.isEmpty()) method.setMonsterIds(candidates);
        }
        Set<String> actualLocations = new LinkedHashSet<>();
        for (String id : method.getMonsterIds()) actualLocations.addAll(catalogue.getMonsters().get(id).getLocationIds());
        method.getLocationIds().retainAll(actualLocations);
        String label = method.getName().toLowerCase(Locale.ROOT).replace('-', ' ');
        for (String king : Arrays.asList("rex", "prime", "supreme"))
        {
            if (!label.contains(king + " only")) continue;
            method.getMonsterIds().removeIf(id -> !id.contains("dagannoth") || !id.contains(king));
            if (method.getMonsterIds().isEmpty()) method.setSelectable(false);
        }
        if (label.contains("tribrid") || method.getRole().equals("tribrid") || method.getRole().equals("all-kings"))
        {
            method.setSelectable(false);
            method.getGuidance().add("Tribrid reference: full mandatory switches have not been normalized into a selectable preparation.");
        }
        boolean superior = method.getRole().equals("superior");
        List<String> matchingVariants = method.getMonsterIds().stream().filter(id ->
            id.contains("superior") || id.startsWith("king-kurask") || id.startsWith("colossal-") || id.startsWith("monstrous-")
                || id.equals("blood-starved-venator")).collect(Collectors.toList());
        if (superior && !matchingVariants.isEmpty()) method.setMonsterIds(matchingVariants);
        else if (!superior && method.getMonsterIds().size() > matchingVariants.size()) method.getMonsterIds().removeAll(matchingVariants);
        if (superior)
        {
            method.setSelectable(false);
            method.getGuidance().add("Superior encounter guidance; this spawn cannot be selected as a repeatable task method.");
        }
        if (method.getLocationIds().isEmpty()) method.setSelectable(false);
    }

    private Map<String, List<ItemOption>> equipment(JsonObject slots, boolean wiki)
    {
        Map<String, List<ItemOption>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : slots.entrySet())
        {
            String slot = entry.getKey().toUpperCase(Locale.ROOT);
            if (slot.equals("NECK")) slot = "AMULET";
            if (!SLOTS.contains(slot))
            {
                if (!slot.equals("SPECIAL") && !slot.equals("SPELLBOOK") && !slot.equals("SPELL") && !slot.equals("RUNES")) unresolved.merge("Unsupported authored equipment slot: " + slot, 1, Integer::sum);
                continue;
            }
            List<String> names = new ArrayList<>();
            for (JsonElement raw : entry.getValue().getAsJsonArray()) names.add(wiki ? string(raw.getAsJsonObject(), "name") : raw.getAsString());
            result.put(slot, options(names));
        }
        return result;
    }

    private List<ItemOption> options(List<String> names)
    {
        List<ItemOption> result = new ArrayList<>();
        for (String name : names)
        {
            JsonArray family = array(object(overrides, "optionFamilies"), name);
            if (family.size() > 0)
            {
                List<String> alternatives = new ArrayList<>();
                for (JsonElement alternative : family) alternatives.add(alternative.getAsString());
                result.addAll(options(alternatives));
                continue;
            }
            ItemOption option = new ItemOption();
            option.setName(name);
            JsonObject item = resolve(name);
            if (item != null)
            {
                option.setName(first(string(item, "name"), name));
                option.setItemIds(integers(item, "itemIds"));
                option.setRequirements(strings(item, "requirements"));
                for (Map.Entry<String, JsonElement> level : object(item, "levels").entrySet()) option.getRequirements().add(level.getKey().toUpperCase(Locale.ROOT) + " >= " + level.getValue().getAsInt());
                for (JsonElement group : array(item, "requires"))
                {
                    List<Integer> ids = new ArrayList<>();
                    for (JsonElement id : group.getAsJsonArray()) ids.add(id.getAsInt());
                    option.getRequires().add(ids);
                }
            }
            if (option.getItemIds().isEmpty())
            {
                // Unknown category labels are reported, never emitted as a selectable item.
                unresolved.merge("Unresolved equipment option: " + name, 1, Integer::sum);
            }
            else result.add(option);
        }
        return result;
    }

    private Supply supply(String name, boolean required)
    {
        Supply supply = new Supply();
        supply.setName(name);
        supply.setRequired(required);
        for (String alternative : name.split("(?i)\\s+or\\s+"))
        {
            JsonObject source = resolve(alternative);
            if (source != null)
            {
                for (int id : integers(source, "itemIds")) if (!supply.getItemIds().contains(id)) supply.getItemIds().add(id);
                supply.setStackable(bool(source, "stackable"));
            }
        }
        return supply;
    }

    private List<Supply> supplies(List<String> names, boolean required)
    {
        List<Supply> result = new ArrayList<>();
        for (String name : names) result.add(supply(name, required));
        return result;
    }

    private JsonObject resolve(String name)
    {
        JsonObject direct = items.get(key(name));
        if (direct != null) return direct;
        String cleaned = name.replaceFirst("(?i)^\\d+\\s+", "")
            .replaceFirst("(?i)\\s+(?:for|if|when|while|after|on task|on crush|on stab)\\b.*$", "");
        direct = items.get(key(cleaned));
        if (direct == null) unresolved.merge(name, 1, Integer::sum);
        return direct;
    }

    private List<Evidence> evidence(Path path)
    {
        JsonObject record = object(object(audit, "records"), root.relativize(path).toString());
        return evidenceList(record);
    }

    private List<Evidence> evidenceList(JsonObject source)
    {
        List<Evidence> result = new ArrayList<>();
        for (JsonElement raw : array(source, "evidence")) result.add(GSON.fromJson(raw, Evidence.class));
        return result;
    }

    private void validate()
    {
        for (Location location : catalogue.getLocations().values())
        {
            String area = location.getAssignmentAreaId();
            if (area != null && !area.isEmpty())
            {
                references(location.getId(), Collections.singletonList(area), catalogue.getLocations());
                if (area.equals(location.getId())) throw new IllegalArgumentException("Location is its own assignment area: " + area);
                String ancestor = catalogue.getLocations().get(area).getAssignmentAreaId();
                if (ancestor != null && !ancestor.isEmpty()) throw new IllegalArgumentException("Assignment area must be canonical: " + area);
            }
        }
        for (Task task : catalogue.getTasks().values())
        {
            references(task.getId(), task.getMasterIds(), catalogue.getMasters());
            references(task.getId(), task.getMonsterIds(), catalogue.getMonsters());
            references(task.getId(), task.getLocationIds(), catalogue.getLocations());
        }
        for (Monster monster : catalogue.getMonsters().values())
        {
            references(monster.getId(), monster.getLocationIds(), catalogue.getLocations());
            references(monster.getId(), monster.getMethodIds(), catalogue.getMethods());
        }
        for (Method method : catalogue.getMethods().values())
        {
            references(method.getId(), method.getMonsterIds(), catalogue.getMonsters());
            references(method.getId(), method.getLocationIds(), catalogue.getLocations());
            if (!Arrays.asList("MELEE", "RANGED", "MAGIC").contains(method.getStyle())) throw new IllegalArgumentException("Invalid method style " + method.getId() + ": " + method.getStyle());
            for (Map.Entry<String, List<ItemOption>> slot : method.getEquipment().entrySet())
            {
                if (!SLOTS.contains(slot.getKey())) throw new IllegalArgumentException("Unknown equipment slot " + slot.getKey());
                for (ItemOption option : slot.getValue()) for (int id : option.getItemIds()) if (id <= 0) throw new IllegalArgumentException("Invalid equipment item ID " + id);
            }
        }
    }

    private static void references(String owner, List<String> ids, Map<String, ?> targets)
    {
        for (String id : ids) if (!targets.containsKey(id)) throw new IllegalArgumentException(owner + " references missing " + id);
    }

    private static <T> void put(Map<String, T> target, String id, T value)
    {
        if (id.isEmpty() || !id.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) throw new IllegalArgumentException("Invalid source ID " + id);
        if (target.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate source ID " + id);
    }

    private List<Path> files(String directory, String suffix) throws IOException
    {
        Path path = root.resolve(directory);
        if (!Files.exists(path)) return Collections.emptyList();
        try (Stream<Path> paths = Files.walk(path))
        {
            return paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(suffix)).sorted().collect(Collectors.toList());
        }
    }

    private static JsonObject read(Path path) throws IOException
    {
        if (!Files.exists(path)) return new JsonObject();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static JsonObject object(JsonObject source, String name)
    {
        JsonElement value = source.get(name);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    private static JsonArray array(JsonObject source, String name)
    {
        JsonElement value = source.get(name);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }

    private static String string(JsonObject source, String name)
    {
        JsonElement value = source.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static int number(JsonObject source, String name)
    {
        JsonElement value = source.get(name);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    private static boolean bool(JsonObject source, String name)
    {
        JsonElement value = source.get(name);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private static List<String> strings(JsonObject source, String name)
    {
        List<String> result = new ArrayList<>();
        for (JsonElement value : array(source, name)) if (value.isJsonPrimitive()) result.add(value.getAsString());
        return result;
    }

    private static List<Integer> integers(JsonObject source, String name)
    {
        List<Integer> result = new ArrayList<>();
        for (JsonElement value : array(source, name)) result.add(value.getAsInt());
        return result;
    }

    private static String key(String value)
    {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String first(String... values)
    {
        for (String value : values) if (!value.isEmpty()) return value;
        return "";
    }

    private static void add(List<String> values, String value)
    {
        if (value != null && !value.isEmpty() && !values.contains(value)) values.add(value);
    }

    private static String wikiTitle(String url)
    {
        if (url.isEmpty()) return "";
        int prefix = url.indexOf("/w/");
        String title = prefix >= 0 ? url.substring(prefix + 3) : url;
        return URLDecoder.decode(title, StandardCharsets.UTF_8).replace('_', ' ').split("#")[0];
    }
}

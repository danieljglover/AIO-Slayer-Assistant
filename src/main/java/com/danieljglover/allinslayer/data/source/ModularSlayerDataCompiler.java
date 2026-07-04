package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MasterData;
import com.danieljglover.allinslayer.model.MasterEconomy;
import com.danieljglover.allinslayer.model.MasterRequirements;
import com.danieljglover.allinslayer.model.MonsterStrategy;
import com.danieljglover.allinslayer.model.StrategyItemRef;
import com.danieljglover.allinslayer.model.StrategyMethod;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.RewardData;
import com.danieljglover.allinslayer.model.RewardEffect;
import com.danieljglover.allinslayer.model.LocationQuality;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.SlayerMeta;
import com.danieljglover.allinslayer.model.StrategyWeapon;
import com.danieljglover.allinslayer.model.TaskUnlock;
import com.danieljglover.allinslayer.model.UnlockType;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ModularSlayerDataCompiler
{
    private static final Gson GSON = new Gson();

    /** The valid source effect strings (WA-8): exactly the {@link RewardEffect} names. */
    private static final Set<String> REWARD_EFFECTS = Stream.of(RewardEffect.values())
        .map(Enum::name).collect(Collectors.toSet());

    private ModularSlayerDataCompiler()
    {
    }

    public static List<TaskData> compile(Path sourceRoot)
    {
        ModularSlayerDataSet data = load(sourceRoot);
        List<String> errors = validate(data);
        if (!errors.isEmpty())
        {
            throw new SlayerDataValidationException(errors);
        }
        return compile(data);
    }

    /**
     * WA-8 (ADR-0018 #1): compile the SECOND generated resource's content -
     * {@code slayer-meta.json = {masters, rewards}} - from {@code masters/*.json} +
     * {@code rewards/*.json}. Runs the same whole-dataset strict validation as
     * {@link #compile(Path)} (one broken domain fails the whole build, ADR-0016).
     */
    public static SlayerMeta compileMeta(Path sourceRoot)
    {
        ModularSlayerDataSet data = load(sourceRoot);
        List<String> errors = validate(data);
        if (!errors.isEmpty())
        {
            throw new SlayerDataValidationException(errors);
        }
        return compileMeta(data);
    }

    private static ModularSlayerDataSet load(Path sourceRoot)
    {
        ModularSlayerDataSet data = new ModularSlayerDataSet();
        data.setMasters(loadJson(sourceRoot.resolve("masters"), SourceMaster.class));
        data.setTasks(loadJson(sourceRoot.resolve("tasks"), SourceTask.class));
        data.setMonsters(loadMonsterFamilies(sourceRoot.resolve("monsters")));
        data.setLocations(loadJson(sourceRoot.resolve("locations"), SourceLocation.class));
        data.setWeapons(loadJson(sourceRoot.resolve("weapons"), SourceWeapon.class));
        data.setItems(loadJson(sourceRoot.resolve("items"), SourceItem.class));
        data.setStrategies(loadStrategies(sourceRoot.resolve("strategies")));
        data.setRewards(loadJson(sourceRoot.resolve("rewards"), SourceReward.class));
        return data;
    }

    private static List<SourceStrategy> loadStrategies(Path dir)
    {
        if (!Files.isDirectory(dir))
        {
            return Collections.emptyList();
        }
        try (Stream<Path> paths = Files.list(dir))
        {
            List<Path> files = paths
                .filter(path -> Files.isRegularFile(path) || Files.isDirectory(path))
                .sorted()
                .collect(Collectors.toList());
            List<SourceStrategy> values = new ArrayList<>();
            for (Path file : files)
            {
                if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".md"))
                {
                    values.add(StrategyMarkdownParser.parse(file.toString(),
                        new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
                }
                else if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".json"))
                {
                    values.add(readJson(file, SourceStrategy.class));
                }
                else if (Files.isDirectory(file))
                {
                    Path json = file.resolve("strategy.json");
                    if (Files.isRegularFile(json))
                    {
                        values.add(readJson(json, SourceStrategy.class));
                    }
                }
            }
            return values;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("failed to load strategy data from " + dir, e);
        }
    }

    private static <T> List<T> loadJson(Path dir, Class<T> type)
    {
        if (!Files.isDirectory(dir))
        {
            return Collections.emptyList();
        }
        try (Stream<Path> paths = Files.list(dir))
        {
            List<Path> files = paths
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted()
                .collect(Collectors.toList());
            List<T> values = new ArrayList<>();
            for (Path file : files)
            {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
                {
                    values.add(GSON.fromJson(reader, type));
                }
            }
            return values;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("failed to load modular slayer data from " + dir, e);
        }
    }

    private static List<SourceMonsterFamily> loadMonsterFamilies(Path dir)
    {
        if (!Files.isDirectory(dir))
        {
            return Collections.emptyList();
        }
        try (Stream<Path> paths = Files.list(dir))
        {
            List<Path> entries = paths.sorted().collect(Collectors.toList());
            List<SourceMonsterFamily> families = new ArrayList<>();
            for (Path entry : entries)
            {
                if (Files.isRegularFile(entry) && entry.getFileName().toString().endsWith(".json"))
                {
                    families.add(readJson(entry, SourceMonsterFamily.class));
                }
                else if (Files.isDirectory(entry))
                {
                    SourceMonsterFamily family = loadMonsterFamilyDirectory(entry);
                    if (family != null)
                    {
                        families.add(family);
                    }
                }
            }
            return families;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("failed to load monster data from " + dir, e);
        }
    }

    private static SourceMonsterFamily loadMonsterFamilyDirectory(Path dir)
    {
        try (Stream<Path> paths = Files.list(dir))
        {
            List<Path> files = paths
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                .sorted()
                .collect(Collectors.toList());
            if (files.isEmpty())
            {
                return null;
            }

            SourceMonsterFamily family = new SourceMonsterFamily();
            String monsterId = dir.getFileName().toString();
            family.setMonsterId(monsterId);
            family.setName(monsterId);
            List<SourceMonsterVariant> variants = new ArrayList<>();
            for (Path file : files)
            {
                variants.add(readJson(file, SourceMonsterVariant.class));
            }
            family.setVariants(variants);
            return family;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("failed to load monster variant data from " + dir, e);
        }
    }

    private static <T> T readJson(Path file, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }

    private static List<String> validate(ModularSlayerDataSet data)
    {
        List<String> errors = new ArrayList<>();
        Set<String> masterIds = collectUnique("masterId", data.getMasters().stream()
            .map(SourceMaster::getMasterId).collect(Collectors.toList()), errors);
        collectUnique("taskId", data.getTasks().stream()
            .map(SourceTask::getTaskId).collect(Collectors.toList()), errors);
        Set<String> locationIds = collectUnique("locationId", data.getLocations().stream()
            .map(SourceLocation::getLocationId).collect(Collectors.toList()), errors);
        Set<String> weaponIds = collectUnique("weaponId", data.getWeapons().stream()
            .map(SourceWeapon::getWeaponId).collect(Collectors.toList()), errors);
        Set<String> strategyIds = collectUnique("strategyId", data.getStrategies().stream()
            .map(SourceStrategy::getStrategyId).collect(Collectors.toList()), errors);
        Set<String> variantIds = collectVariantIds(data, errors);
        collectUnique("rewardId", data.getRewards().stream()
            .map(SourceReward::getRewardId).collect(Collectors.toList()), errors);
        validateItems(data, errors);

        // WA-8 (ADR-0018 #6): rewards must be renderable and mappable - a real cost (the runtime
        // RewardData.pointsCost is a primitive) and an effect string that IS a RewardEffect value
        // (the WA-3 contract keeps it a String at source precisely so a typo fails HERE, loudly,
        // instead of Gson silently nulling an enum).
        for (SourceReward reward : data.getRewards())
        {
            if (reward == null)
            {
                continue;
            }
            if (reward.getPointsCost() == null)
            {
                errors.add("reward " + reward.getRewardId() + " has null pointsCost");
            }
            if (reward.getEffect() == null || !REWARD_EFFECTS.contains(reward.getEffect()))
            {
                errors.add("reward " + reward.getRewardId() + " has unknown effect: "
                    + reward.getEffect());
            }
        }

        for (SourceTask task : data.getTasks())
        {
            if (task == null)
            {
                continue;
            }
            Set<String> taskMasterIds = new HashSet<>();
            if (task.getMasterIds() != null)
            {
                for (String masterId : task.getMasterIds())
                {
                    if (masterId != null)
                    {
                        taskMasterIds.add(masterId);
                    }
                    if (!masterIds.contains(masterId))
                    {
                        errors.add("task " + task.getTaskId() + " references unknown masterId: " + masterId);
                    }
                }
            }
            // A task assigned by any master must carry an amount range for each of those masters and no
            // extras: amountByMaster's key set is exactly masterIds. weightByMaster/extendedAmount are
            // per-master overlays keyed by a subset (a listed master MAY lack a weight - e.g. araxytes
            // are assigned indirectly by Turael/Spria - so weight coverage is not required).
            if (!taskMasterIds.isEmpty())
            {
                Set<String> amountKeys = task.getAmountByMaster() == null
                    ? Collections.<String>emptySet() : task.getAmountByMaster().keySet();
                for (String masterId : taskMasterIds)
                {
                    if (!amountKeys.contains(masterId))
                    {
                        errors.add("task " + task.getTaskId() + " masterId " + masterId
                            + " has no amountByMaster entry");
                    }
                }
                for (String amountKey : amountKeys)
                {
                    if (!taskMasterIds.contains(amountKey))
                    {
                        errors.add("task " + task.getTaskId()
                            + " amountByMaster key not in masterIds: " + amountKey);
                    }
                }
            }
            if (task.getWeightByMaster() != null)
            {
                for (String weightKey : task.getWeightByMaster().keySet())
                {
                    if (!taskMasterIds.contains(weightKey))
                    {
                        errors.add("task " + task.getTaskId()
                            + " weightByMaster key not in masterIds: " + weightKey);
                    }
                }
            }
            if (task.getExtendedAmount() != null)
            {
                for (String extendedKey : task.getExtendedAmount().keySet())
                {
                    if (!taskMasterIds.contains(extendedKey))
                    {
                        errors.add("task " + task.getTaskId()
                            + " extendedAmount key not in masterIds: " + extendedKey);
                    }
                }
            }
            if (task.getVariantIds() != null)
            {
                for (String variantId : task.getVariantIds())
                {
                    if (!variantIds.contains(variantId))
                    {
                        errors.add("task " + task.getTaskId() + " references unknown variantId: " + variantId);
                    }
                }
                if (task.getVariantIds().size() > 1)
                {
                    String defaultVariantId = task.getDefaultVariantId();
                    if (defaultVariantId == null || defaultVariantId.trim().isEmpty())
                    {
                        errors.add("missing defaultVariantId for multi-variant task: " + task.getTaskId());
                    }
                    else if (!task.getVariantIds().contains(defaultVariantId))
                    {
                        errors.add("defaultVariantId " + defaultVariantId + " is not in task "
                            + task.getTaskId() + " variantIds");
                    }
                }
            }
            // A variantInfo row's variantId (when present) is a foreign key into the monster variants;
            // a null variantId is an info-only aggregate row (no linkage). Duplicate variantIds across
            // rows are allowed - each row carries distinct location/note detail.
            if (task.getVariantInfo() != null)
            {
                for (SourceTaskVariantInfo info : task.getVariantInfo())
                {
                    if (info != null && info.getVariantId() != null
                        && !variantIds.contains(info.getVariantId()))
                    {
                        errors.add("task " + task.getTaskId()
                            + " references unknown variantInfo variantId: " + info.getVariantId());
                    }
                }
            }
            if (task.getLocationIds() != null)
            {
                for (String locationId : task.getLocationIds())
                {
                    if (!locationIds.contains(locationId))
                    {
                        errors.add("task " + task.getTaskId() + " references unknown locationId: " + locationId);
                    }
                }
            }
            // WA-7 (ADR-0018 #5): a task with an extendedAmount must name the exact unlock that
            // enables it - exactly one EXTENSION-typed row with a real cost - so the panel can say
            // "extended 200-250 with <unlock> (N pts)" without parsing free-text notes.
            if (task.getExtendedAmount() != null && !task.getExtendedAmount().isEmpty())
            {
                List<SourceTaskUnlock> extensions = (task.getUnlocks() == null
                    ? Collections.<SourceTaskUnlock>emptyList() : task.getUnlocks()).stream()
                    .filter(u -> u != null && u.getType() == UnlockType.EXTENSION)
                    .collect(Collectors.toList());
                if (extensions.size() != 1)
                {
                    errors.add("task " + task.getTaskId() + " has extendedAmount but "
                        + extensions.size() + " EXTENSION-typed unlocks (need exactly 1)");
                }
                else if (extensions.get(0).getPointsCost() == null)
                {
                    errors.add("task " + task.getTaskId() + " EXTENSION unlock "
                        + extensions.get(0).getUnlockId() + " has null pointsCost");
                }
            }
            // DT-B6 (ADR-0017 #5 / GAP-5): locationComparison[].locationId is a foreign key into the
            // location files. It is not compiled into runtime, but validate it so the data stays honest.
            if (task.getLocationComparison() != null)
            {
                Set<String> taskLocationIds = task.getLocationIds() == null
                    ? Collections.<String>emptySet() : new HashSet<>(task.getLocationIds());
                for (SourceTaskLocationComparison row : task.getLocationComparison())
                {
                    if (row != null && row.getLocationId() != null
                        && !locationIds.contains(row.getLocationId()))
                    {
                        errors.add("task " + task.getTaskId()
                            + " references unknown locationComparison locationId: " + row.getLocationId());
                    }
                    // A comparison row must join to one of the task's own locations (not just any known
                    // location) so the runtime quality overlay always attaches to a listed location.
                    else if (row != null && row.getLocationId() != null
                        && !taskLocationIds.contains(row.getLocationId()))
                    {
                        errors.add("task " + task.getTaskId()
                            + " locationComparison locationId not in locationIds: " + row.getLocationId());
                    }
                }
            }
        }
        for (SourceMonsterFamily family : data.getMonsters())
        {
            if (family == null || family.getVariants() == null)
            {
                continue;
            }
            for (SourceMonsterVariant variant : family.getVariants())
            {
                if (variant != null && variant.getStrategyId() != null
                    && !strategyIds.contains(variant.getStrategyId()))
                {
                    errors.add("variant " + variant.getVariantId() + " references unknown strategyId: "
                        + variant.getStrategyId());
                }
                // DT-B6 (ADR-0017 #5): the variant-level locationId FK (the DT-B5 derivation's home
                // location) must resolve to a known location file.
                if (variant != null && variant.getLocationId() != null
                    && !locationIds.contains(variant.getLocationId()))
                {
                    errors.add("variant " + variant.getVariantId() + " references unknown locationId: "
                        + variant.getLocationId());
                }
            }
        }
        for (SourceStrategy strategy : data.getStrategies())
        {
            if (strategy == null)
            {
                continue;
            }
            if (strategy.getVariantIds() != null)
            {
                for (String variantId : strategy.getVariantIds())
                {
                    if (!variantIds.contains(variantId))
                    {
                        errors.add("strategy " + strategy.getStrategyId()
                            + " references unknown variantId: " + variantId);
                    }
                }
            }
            validateWeaponRefs(strategy.getStrategyId(), primaryWeapons(strategy), weaponIds, errors);
            if (secondaryWeapons(strategy) != null)
            {
                for (SourceStrategyWeapon weapon : secondaryWeapons(strategy))
                {
                    if (weapon != null)
                    {
                        validateWeaponRefs(strategy.getStrategyId(),
                            Collections.singletonList(weapon.getWeaponId()), weaponIds, errors);
                    }
                }
            }
        }
        return errors;
    }

    /**
     * The supply-item catalogue is structured data (unlike the free-text strategy names it resolves),
     * so it fails the build the way weapons do: every row needs an {@code itemKey}, a {@code name}, and
     * a non-empty {@code itemIds}; and no two rows may share an {@code itemKey}, {@code name}, or
     * {@code alias} (a collision would make resolution ambiguous). Mirrors the weapon-id uniqueness pass.
     */
    private static void validateItems(ModularSlayerDataSet data, List<String> errors)
    {
        List<String> lookupNames = new ArrayList<>();
        collectUnique("itemKey", data.getItems().stream()
            .map(SourceItem::getItemKey).collect(Collectors.toList()), errors);
        for (SourceItem item : data.getItems())
        {
            if (item == null)
            {
                continue;
            }
            String id = item.getItemKey() == null ? "(null)" : item.getItemKey();
            if (item.getName() == null || item.getName().trim().isEmpty())
            {
                errors.add("item " + id + " has no name");
            }
            if (item.getItemIds() == null || item.getItemIds().isEmpty())
            {
                errors.add("item " + id + " has empty itemIds");
            }
            if (item.getName() != null)
            {
                lookupNames.add(item.getName());
            }
            if (item.getAliases() != null)
            {
                lookupNames.addAll(item.getAliases());
            }
        }
        collectUnique("item name/alias", lookupNames, errors);
    }

    private static void validateWeaponRefs(String strategyId, List<String> refs, Set<String> weaponIds,
        List<String> errors)
    {
        if (refs == null)
        {
            return;
        }
        for (String weaponId : refs)
        {
            if (!weaponIds.contains(weaponId))
            {
                errors.add("strategy " + strategyId + " references unknown weaponId: " + weaponId);
            }
        }
    }

    private static List<TaskData> compile(ModularSlayerDataSet data)
    {
        Map<String, SourceMonsterVariant> variants = variantMap(data);
        Map<String, SourceLocation> locations = data.getLocations().stream()
            .filter(l -> l != null && l.getLocationId() != null)
            .collect(Collectors.toMap(SourceLocation::getLocationId, l -> l, (a, b) -> a, LinkedHashMap::new));
        Map<String, SourceStrategy> strategies = data.getStrategies().stream()
            .filter(s -> s != null && s.getStrategyId() != null)
            .collect(Collectors.toMap(SourceStrategy::getStrategyId, s -> s, (a, b) -> a, LinkedHashMap::new));
        Map<String, SourceWeapon> weapons = data.getWeapons().stream()
            .filter(w -> w != null && w.getWeaponId() != null)
            .collect(Collectors.toMap(SourceWeapon::getWeaponId, w -> w, (a, b) -> a, LinkedHashMap::new));
        // The item-name resolver over the supply catalogue + weapon catalogue; carries the strategy
        // methods' free-text item lists to real ids, accumulating unresolved names for the report below.
        ItemNameResolver resolver = new ItemNameResolver(data.getItems(), data.getWeapons());

        List<SourceTask> sourceTasks = new ArrayList<>(data.getTasks());
        sourceTasks.sort((a, b) -> nullToEmpty(a.getTaskId()).compareTo(nullToEmpty(b.getTaskId())));
        List<TaskData> tasks = new ArrayList<>();
        for (SourceTask source : sourceTasks)
        {
            TaskData task = new TaskData();
            task.setTask(source.getName());
            task.setSlayerTargetId(source.getSlayerTargetId());
            task.setSlayerLevel(source.getSlayerLevel());
            task.setQuestReqs(nullToEmptyList(source.getQuestReqs()));
            task.setAssignedBy(nullToEmptyList(source.getMasterIds()));
            task.setAmountByMaster(source.getAmountByMaster());
            // WA-7 (ADR-0018 #3/#4): the assignment-economy fields reach runtime. Empty compiles
            // to null (the DT-B5 discipline) so an unauthored task emits no new keys (FR-6).
            task.setExtendedAmount(emptyToNull(source.getExtendedAmount()));
            task.setWeightByMaster(emptyToNull(source.getWeightByMaster()));
            task.setUnlocks(resolveUnlocks(source.getUnlocks()));
            task.setMonsters(resolveMonsterNames(source, variants));
            task.setNpcIds(resolveNpcIds(source, variants));
            task.setWeakness(source.getWeakness());
            task.setMonsterDefence(source.getMonsterDefence());
            // WD-2 (ADR-0020 #1): carry the optional task-level offence fallback through to runtime.
            task.setOffence(source.getOffence());
            task.setSlayerHelmApplies(source.isSlayerHelmApplies());
            task.setDemon(source.isDemon());
            task.setDragon(source.isDragon());
            task.setKalphite(source.isKalphite());
            task.setUndead(source.isUndead());
            task.setRequiredItemId(source.getRequiredItemId());
            task.setRequiredItemName(source.getRequiredItemName());
            task.setLocations(resolveLocations(source, locations));
            task.setRecommendedMethod(source.getRecommendedMethod());
            task.setVariants(resolveVariants(source, variants, strategies, weapons, locations, resolver));
            tasks.add(task);
        }
        List<String> unresolved = resolver.unresolvedReport();
        if (!unresolved.isEmpty())
        {
            System.err.println("[slayer-data] " + unresolved.size()
                + " unresolved strategy item name(s) - add to items/ to make them packable/actionable:");
            for (String line : unresolved)
            {
                System.err.println("[slayer-data]   " + line);
            }
        }
        return tasks;
    }

    /** WA-8: masters + rewards -> the slayer-meta bundle, sorted by id (deterministic output). */
    private static SlayerMeta compileMeta(ModularSlayerDataSet data)
    {
        List<SourceMaster> sourceMasters = data.getMasters().stream()
            .filter(m -> m != null)
            .sorted((a, b) -> nullToEmpty(a.getMasterId()).compareTo(nullToEmpty(b.getMasterId())))
            .collect(Collectors.toList());
        List<MasterData> masters = new ArrayList<>();
        for (SourceMaster source : sourceMasters)
        {
            MasterData master = new MasterData();
            master.setMasterId(source.getMasterId());
            master.setName(source.getName());
            master.setAliases(source.getAliases());
            master.setLocation(source.getLocation());
            master.setRequirements(resolveRequirements(source.getRequirements()));
            master.setEconomy(resolveEconomy(source.getEconomy()));
            master.setNotes(source.getNotes());
            masters.add(master);
        }

        List<SourceReward> sourceRewards = data.getRewards().stream()
            .filter(r -> r != null)
            .sorted((a, b) -> nullToEmpty(a.getRewardId()).compareTo(nullToEmpty(b.getRewardId())))
            .collect(Collectors.toList());
        List<RewardData> rewards = new ArrayList<>();
        for (SourceReward source : sourceRewards)
        {
            RewardData reward = new RewardData();
            reward.setRewardId(source.getRewardId());
            reward.setName(source.getName());
            reward.setPointsCost(source.getPointsCost()); // validated non-null
            reward.setEffect(RewardEffect.valueOf(source.getEffect())); // validated a member
            rewards.add(reward);
        }

        SlayerMeta meta = new SlayerMeta();
        meta.setMasters(masters);
        meta.setRewards(rewards);
        return meta;
    }

    private static MasterRequirements resolveRequirements(SourceMasterRequirements source)
    {
        if (source == null)
        {
            return null;
        }
        MasterRequirements requirements = new MasterRequirements();
        requirements.setCombatLevel(source.getCombatLevel());
        requirements.setSlayerLevel(source.getSlayerLevel());
        requirements.setQuests(source.getQuests());
        return requirements;
    }

    private static MasterEconomy resolveEconomy(SourceMasterEconomy source)
    {
        if (source == null)
        {
            return null;
        }
        MasterEconomy economy = new MasterEconomy();
        economy.setBasePoints(source.getBasePoints());
        economy.setStreakMultipliers(source.getStreakMultipliers());
        economy.setBlockCost(source.getBlockCost());
        economy.setZeroPoints(source.isZeroPoints());
        economy.setStreakResets(source.isStreakResets());
        economy.setDiaryBoostedPoints(source.getDiaryBoostedPoints());
        economy.setDiaryBoostNote(source.getDiaryBoostNote());
        economy.setSeparateStreak(source.isSeparateStreak());
        return economy;
    }

    private static Map<String, SourceMonsterVariant> variantMap(ModularSlayerDataSet data)
    {
        Map<String, SourceMonsterVariant> variants = new LinkedHashMap<>();
        for (SourceMonsterFamily family : data.getMonsters())
        {
            if (family == null || family.getVariants() == null)
            {
                continue;
            }
            for (SourceMonsterVariant variant : family.getVariants())
            {
                if (variant != null && variant.getVariantId() != null)
                {
                    variants.put(variant.getVariantId(), variant);
                }
            }
        }
        return variants;
    }

    private static List<String> resolveMonsterNames(SourceTask source, Map<String, SourceMonsterVariant> variants)
    {
        List<String> names = new ArrayList<>();
        if (source.getVariantIds() == null)
        {
            return names;
        }
        for (String variantId : source.getVariantIds())
        {
            SourceMonsterVariant variant = variants.get(variantId);
            if (variant != null && variant.getName() != null && !names.contains(variant.getName()))
            {
                names.add(variant.getName());
            }
        }
        return names;
    }

    private static List<Integer> resolveNpcIds(SourceTask source, Map<String, SourceMonsterVariant> variants)
    {
        List<Integer> npcIds = new ArrayList<>();
        if (source.getVariantIds() == null)
        {
            return npcIds;
        }
        for (String variantId : source.getVariantIds())
        {
            SourceMonsterVariant variant = variants.get(variantId);
            if (variant != null && variant.getNpcIds() != null)
            {
                npcIds.addAll(variant.getNpcIds());
            }
        }
        return npcIds;
    }

    private static List<SlayerLocation> resolveLocations(SourceTask source, Map<String, SourceLocation> locations)
    {
        List<SlayerLocation> resolved = new ArrayList<>();
        if (source.getLocationIds() == null)
        {
            return resolved;
        }
        // WD-5a (ADR-0020 #2): index the task's locationComparison rows by locationId so the quality
        // overlay can be attached to the matching location; absent -> null overlay (FR-6).
        Map<String, SourceTaskLocationComparison> comparisons = new LinkedHashMap<>();
        if (source.getLocationComparison() != null)
        {
            for (SourceTaskLocationComparison row : source.getLocationComparison())
            {
                if (row != null && row.getLocationId() != null)
                {
                    comparisons.put(row.getLocationId(), row);
                }
            }
        }
        for (String locationId : source.getLocationIds())
        {
            SourceLocation location = locations.get(locationId);
            if (location != null)
            {
                // DT-B4 (ADR-0017 #2): carry safeSpot + accessNote through to runtime so the
                // suggestion/note/loadout-hint consumers can read them (they were dropped before).
                SlayerLocation slayerLocation = new SlayerLocation(location.getName(),
                    location.isMulti(), location.isCannon(), location.isBurst(),
                    location.isKonarLockable(), location.isWilderness(),
                    location.isSafeSpot(), location.getAccessNote());
                slayerLocation.setQuality(resolveQuality(comparisons.get(locationId)));
                resolved.add(slayerLocation);
            }
        }
        return resolved;
    }

    /** WD-5a: compile a locationComparison row into the runtime {@link LocationQuality} overlay. */
    private static LocationQuality resolveQuality(SourceTaskLocationComparison row)
    {
        if (row == null)
        {
            return null;
        }
        return new LocationQuality(row.getAmount(), row.getMulticombat(), row.getCannonable(),
            row.getSafespottable(), row.getNotes());
    }

    private static List<MonsterVariant> resolveVariants(SourceTask source, Map<String, SourceMonsterVariant> variants,
        Map<String, SourceStrategy> strategies, Map<String, SourceWeapon> weapons,
        Map<String, SourceLocation> locations, ItemNameResolver resolver)
    {
        List<MonsterVariant> resolved = new ArrayList<>();
        if (source.getVariantIds() == null)
        {
            return resolved;
        }
        List<String> taskLocationNames = taskLocationNames(source, locations);
        for (String variantId : source.getVariantIds())
        {
            SourceMonsterVariant sourceVariant = variants.get(variantId);
            if (sourceVariant == null)
            {
                continue;
            }
            MonsterVariant variant = new MonsterVariant();
            variant.setName(sourceVariant.getName());
            variant.setNpcIds(sourceVariant.getNpcIds());
            variant.setCombatLevel(sourceVariant.getCombatLevel());
            variant.setWeakness(sourceVariant.getWeakness());
            variant.setMonsterDefence(sourceVariant.getMonsterDefence());
            // WD-2 (ADR-0020 #1): per-variant offence; MonsterProfile.fromVariant falls back to the
            // task-level offence when this is null.
            variant.setOffence(sourceVariant.getOffence());
            variant.setDemon(sourceVariant.isDemon());
            variant.setDragon(sourceVariant.isDragon());
            variant.setKalphite(sourceVariant.isKalphite());
            variant.setUndead(sourceVariant.isUndead());
            variant.setBoss(sourceVariant.isBoss());
            variant.setDefault(variantId.equals(source.getDefaultVariantId()));
            variant.setLocation(sourceVariant.getLocation());
            variant.setLocationNames(
                deriveLocationNames(variantId, sourceVariant, source, locations, taskLocationNames));
            variant.setRequirement(sourceVariant.getRequirement());
            variant.setBossId(sourceVariant.getBossId());
            if (sourceVariant.getStrategyId() != null)
            {
                variant.setStrategy(
                    resolveStrategy(strategies.get(sourceVariant.getStrategyId()), weapons, resolver));
            }
            resolved.add(variant);
        }
        return resolved;
    }

    /** The task's resolved location names in task order (skipping unknown ids), for subset matching. */
    private static List<String> taskLocationNames(SourceTask source, Map<String, SourceLocation> locations)
    {
        List<String> names = new ArrayList<>();
        if (source.getLocationIds() == null)
        {
            return names;
        }
        for (String locationId : source.getLocationIds())
        {
            SourceLocation location = locations.get(locationId);
            if (location != null && location.getName() != null)
            {
                names.add(location.getName());
            }
        }
        return names;
    }

    /**
     * Derive a variant's location subset (ADR-0017 #1): the authored home location (variant.locationId,
     * if it is one of the task's locationIds) first, then the {@code variantInfo[]} rows that match this
     * variantId resolved by exact case-insensitive trimmed name match against the task location names,
     * added in TASK order, deduped. Unmatched strings and out-of-task ids are dropped (no fabrication).
     * Boss variants link like any other (amends GAP-3): a boss lives in exactly one place (K'ril in the
     * God Wars Dungeon, Skotizo under the Catacombs), so hiding the other task locations is MORE honest
     * than the old fall-through to all of them; a boss with no authored linkage still returns null.
     * Returns null (the "all task locations apply" fallback sentinel) when nothing links, keeping
     * unlinked variants byte-identical to today (FR-6).
     */
    private static List<String> deriveLocationNames(String variantId, SourceMonsterVariant variant,
        SourceTask source, Map<String, SourceLocation> locations, List<String> taskLocationNames)
    {
        if (taskLocationNames.isEmpty())
        {
            return null;
        }
        Set<String> result = new LinkedHashSet<>();

        String homeId = variant.getLocationId();
        if (homeId != null && source.getLocationIds() != null && source.getLocationIds().contains(homeId))
        {
            SourceLocation home = locations.get(homeId);
            if (home != null && home.getName() != null)
            {
                result.add(home.getName());
            }
        }

        Set<String> wanted = new HashSet<>();
        if (source.getVariantInfo() != null)
        {
            for (SourceTaskVariantInfo info : source.getVariantInfo())
            {
                if (info == null || info.getLocations() == null
                    || !nullSafeEquals(variantId, info.getVariantId()))
                {
                    continue;
                }
                for (String locName : info.getLocations())
                {
                    if (locName != null)
                    {
                        wanted.add(locName.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        if (!wanted.isEmpty())
        {
            for (String taskName : taskLocationNames)
            {
                if (wanted.contains(taskName.trim().toLowerCase(Locale.ROOT)))
                {
                    result.add(taskName);
                }
            }
        }

        return result.isEmpty() ? null : new ArrayList<>(result);
    }

    private static boolean nullSafeEquals(String a, String b)
    {
        return a != null && a.equals(b);
    }

    private static MonsterStrategy resolveStrategy(SourceStrategy source, Map<String, SourceWeapon> weapons,
        ItemNameResolver resolver)
    {
        if (source == null)
        {
            return null;
        }
        MonsterStrategy strategy = new MonsterStrategy();
        strategy.setPrimaryStyle(primaryStyle(source));
        strategy.setPrimaryWeapons(resolveStrategyWeapons(primaryWeapons(source), null, weapons));
        List<StrategyWeapon> secondary = new ArrayList<>();
        if (secondaryWeapons(source) != null)
        {
            for (SourceStrategyWeapon sourceWeapon : secondaryWeapons(source))
            {
                secondary.addAll(resolveStrategyWeapons(Collections.singletonList(sourceWeapon.getWeaponId()),
                    sourceWeapon.getStyle(), weapons));
            }
        }
        strategy.setSecondaryWeapons(secondary);
        strategy.setNote(note(source));
        strategy.setSourceUrl(source.getSourceUrl());
        strategy.setMethods(resolveMethods(source.getMethods(), resolver));
        return strategy;
    }

    /**
     * The guide's authored {@code methods[]} -> runtime {@link StrategyMethod}s with their item strings
     * resolved to ids (the dynamic-inventory wiring). Null/empty -> null (FR-6: no methods = today's
     * weapon-override-only behaviour). Every method is kept, including {@code role == "general"} (a
     * null-style method documenting strategy-wide key items the planner still reads).
     */
    private static List<StrategyMethod> resolveMethods(List<SourceStrategyMethod> source,
        ItemNameResolver resolver)
    {
        if (source == null || source.isEmpty())
        {
            return null;
        }
        List<StrategyMethod> resolved = new ArrayList<>();
        for (SourceStrategyMethod row : source)
        {
            if (row == null)
            {
                continue;
            }
            StrategyMethod method = new StrategyMethod();
            method.setMethodId(row.getMethodId());
            method.setLabel(row.getLabel());
            method.setCombatStyle(row.getCombatStyle());
            method.setRole(row.getRole());
            method.setSummary(row.getSummary());
            method.setKeyItems(emptyToNull(resolver.resolveList(row.getRequiredOrKeyItems())));
            method.setPrayers(emptyToNull(row.getPrayers()));
            method.setEquipment(resolveEquipment(row.getEquipment(), resolver));
            method.setInventory(emptyToNull(resolver.resolveList(row.getInventory())));
            method.setRisks(emptyToNull(row.getRisks()));
            resolved.add(method);
        }
        return resolved.isEmpty() ? null : resolved;
    }

    /** The method's per-slot equipment -> resolved refs keyed by {@link EquipmentSlot}; unparsable slots skipped. */
    private static Map<EquipmentSlot, List<StrategyItemRef>> resolveEquipment(
        SourceStrategyEquipment equipment, ItemNameResolver resolver)
    {
        if (equipment == null || equipment.getSlots() == null || equipment.getSlots().isEmpty())
        {
            return null;
        }
        Map<EquipmentSlot, List<StrategyItemRef>> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : equipment.getSlots().entrySet())
        {
            EquipmentSlot slot = parseSlot(entry.getKey());
            if (slot == null)
            {
                continue; // "special" and any unknown key: no runtime slot to place it in
            }
            List<StrategyItemRef> refs = resolver.resolveList(entry.getValue());
            if (!refs.isEmpty())
            {
                resolved.put(slot, refs);
            }
        }
        return resolved.isEmpty() ? null : resolved;
    }

    /** Map an authored equipment-slot key to the runtime {@link EquipmentSlot}, or null when there is none. */
    private static EquipmentSlot parseSlot(String key)
    {
        if (key == null)
        {
            return null;
        }
        switch (key.trim().toLowerCase(Locale.ROOT))
        {
            case "head":
                return EquipmentSlot.HEAD;
            case "cape":
                return EquipmentSlot.CAPE;
            case "neck":
            case "amulet":
                return EquipmentSlot.AMULET;
            case "ammo":
            case "ammunition":
                return EquipmentSlot.AMMO;
            case "weapon":
            case "mainhand":
            case "2h":
                return EquipmentSlot.WEAPON;
            case "body":
            case "torso":
                return EquipmentSlot.BODY;
            case "shield":
            case "offhand":
                return EquipmentSlot.SHIELD;
            case "legs":
                return EquipmentSlot.LEGS;
            case "hands":
            case "gloves":
                return EquipmentSlot.HANDS;
            case "feet":
            case "boots":
                return EquipmentSlot.FEET;
            case "ring":
                return EquipmentSlot.RING;
            default:
                return null; // e.g. "special" - no worn slot
        }
    }

    private static <T> List<T> emptyToNull(List<T> values)
    {
        return values == null || values.isEmpty() ? null : values;
    }

    private static com.danieljglover.allinslayer.model.CombatStyle primaryStyle(SourceStrategy strategy)
    {
        return strategy.getPlugin() == null ? strategy.getPrimaryStyle() : strategy.getPlugin().getPrimaryStyle();
    }

    private static List<String> primaryWeapons(SourceStrategy strategy)
    {
        return strategy.getPlugin() == null ? strategy.getPrimaryWeapons() : strategy.getPlugin().getPrimaryWeapons();
    }

    private static List<SourceStrategyWeapon> secondaryWeapons(SourceStrategy strategy)
    {
        return strategy.getPlugin() == null
            ? strategy.getSecondaryWeapons()
            : strategy.getPlugin().getSecondaryWeapons();
    }

    private static String note(SourceStrategy strategy)
    {
        return strategy.getPlugin() == null ? strategy.getNote() : strategy.getPlugin().getNote();
    }

    private static List<StrategyWeapon> resolveStrategyWeapons(List<String> weaponIds,
        com.danieljglover.allinslayer.model.CombatStyle style, Map<String, SourceWeapon> weapons)
    {
        List<StrategyWeapon> resolved = new ArrayList<>();
        if (weaponIds == null)
        {
            return resolved;
        }
        for (String weaponId : weaponIds)
        {
            SourceWeapon weapon = weapons.get(weaponId);
            if (weapon == null || weapon.getItemIds() == null || weapon.getItemIds().isEmpty())
            {
                continue;
            }
            resolved.add(new StrategyWeapon(weapon.getName(), weapon.getItemIds().get(0), style));
        }
        return resolved;
    }

    /** WA-7: source unlock rows -> runtime {@link TaskUnlock}s; null/empty -> null (FR-6). */
    private static List<TaskUnlock> resolveUnlocks(List<SourceTaskUnlock> source)
    {
        if (source == null || source.isEmpty())
        {
            return null;
        }
        List<TaskUnlock> resolved = new ArrayList<>();
        for (SourceTaskUnlock row : source)
        {
            if (row == null)
            {
                continue;
            }
            TaskUnlock unlock = new TaskUnlock();
            unlock.setUnlockId(row.getUnlockId());
            unlock.setName(row.getName());
            unlock.setPointsCost(row.getPointsCost());
            unlock.setType(row.getType());
            unlock.setNotes(row.getNotes());
            resolved.add(unlock);
        }
        return resolved.isEmpty() ? null : resolved;
    }

    private static <K, V> Map<K, V> emptyToNull(Map<K, V> values)
    {
        return values == null || values.isEmpty() ? null : values;
    }

    private static List<String> nullToEmptyList(List<String> values)
    {
        return values == null ? Collections.emptyList() : values;
    }

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }

    private static Set<String> collectVariantIds(ModularSlayerDataSet data, List<String> errors)
    {
        List<String> ids = new ArrayList<>();
        for (SourceMonsterFamily family : data.getMonsters())
        {
            if (family == null || family.getVariants() == null)
            {
                continue;
            }
            for (SourceMonsterVariant variant : family.getVariants())
            {
                if (variant != null)
                {
                    ids.add(variant.getVariantId());
                }
            }
        }
        return collectUnique("variantId", ids, errors);
    }

    private static Set<String> collectUnique(String label, List<String> ids, List<String> errors)
    {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (String id : ids)
        {
            if (id == null || id.trim().isEmpty())
            {
                continue;
            }
            if (!seen.add(id) && duplicates.add(id))
            {
                errors.add("duplicate " + label + ": " + id);
            }
        }
        return seen;
    }
}

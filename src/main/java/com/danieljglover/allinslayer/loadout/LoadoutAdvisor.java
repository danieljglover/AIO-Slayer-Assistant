package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.data.SlayerDataService;
import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MasterData;
import com.danieljglover.allinslayer.model.MasterEconomy;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.MonsterStrategy;
import com.danieljglover.allinslayer.model.StrategyMethod;
import com.danieljglover.allinslayer.model.LocationQuality;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.StrategyWeapon;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Produces a {@link Recommendation} for a task by orchestrating the stat-driven {@link GearSelector},
 * the {@link ConsumableSelector}, and the {@link DpsEstimator} over the player's owned items
 * (plan section 4-5, ADR-0001/0006). The combat style is fixed by the task's {@code weakness}; there
 * is no longer any {@code StyleLoadout} list and no recommended-upgrades concept (ADR-0005).
 *
 * <p>A loadout is "viable" only when a weapon is selected for the weakness style; otherwise this
 * returns {@link Optional#empty()} and the plugin renders the TASK_WITHOUT_LOADOUT state.
 */
@Slf4j
@Singleton
public class LoadoutAdvisor
{
    /** Variant names already warned about a malformed strategy, so the warning fires once (ADR-0015). */
    private static final Set<String> WARNED_MALFORMED_STRATEGY = ConcurrentHashMap.newKeySet();

    private final GearSelector gearSelector;
    private final ConsumableSelector consumableSelector;
    private final DpsEstimator dpsEstimator;
    private final PriceService priceService;
    private final SlayerUnlockStateProvider unlockState;
    private final SlayerDataService dataService;

    @Inject
    public LoadoutAdvisor(GearSelector gearSelector, ConsumableSelector consumableSelector,
        DpsEstimator dpsEstimator, PriceService priceService, SlayerUnlockStateProvider unlockState,
        SlayerDataService dataService)
    {
        this.gearSelector = gearSelector;
        this.consumableSelector = consumableSelector;
        this.dpsEstimator = dpsEstimator;
        this.priceService = priceService;
        this.unlockState = unlockState;
        this.dataService = dataService;
    }

    public Optional<Recommendation> recommend(TaskData task, OwnedItems owned, PlayerStats stats,
        AdviceMode mode, boolean haveCannon)
    {
        return recommend(task, owned, stats, mode, haveCannon, null, null, null);
    }

    public Optional<Recommendation> recommend(TaskData task, OwnedItems owned, PlayerStats stats,
        AdviceMode mode, boolean haveCannon, String selectedLocationName)
    {
        return recommend(task, owned, stats, mode, haveCannon, selectedLocationName, null, null);
    }

    /**
     * @param selectedVariantName the user-selected monster variant (MV-B5); resolves to the
     *     {@link MonsterProfile} that drives the engine. Null / single-variant / no-variant tasks
     *     resolve to the task default profile so the loadout is byte-identical to today (FR-6).
     * @param selectedMethod the user-selected combat method (MV-B9, ADR-0013); the engine gears for
     *     {@code selectedMethod ?? recommendedStyle}, so the user may override the recommended style.
     *     Null = use the variant's recommended style (today's behaviour).
     */
    public Optional<Recommendation> recommend(TaskData task, OwnedItems owned, PlayerStats stats,
        AdviceMode mode, boolean haveCannon, String selectedLocationName, String selectedVariantName,
        CombatStyle selectedMethod)
    {
        return recommend(task, owned, stats, mode, haveCannon, selectedLocationName, selectedVariantName,
            selectedMethod, null, Collections.emptySet());
    }

    /**
     * @param selectedMaster the user-selected assigning master id (WA-11), or null; feeds the WD-12
     *     skip/block advisory (its block cost / free-skip reachability).
     * @param dislikedTasks the config-declared disliked/blocked task-name set (WD-12 / ADR-0020 #5);
     *     never null (empty = the feature is inert). Matched against the task name, case-insensitively.
     */
    public Optional<Recommendation> recommend(TaskData task, OwnedItems owned, PlayerStats stats,
        AdviceMode mode, boolean haveCannon, String selectedLocationName, String selectedVariantName,
        CombatStyle selectedMethod, String selectedMaster, Set<String> dislikedTasks)
    {
        return recommend(task, owned, stats, mode, haveCannon, selectedLocationName, selectedVariantName,
            selectedMethod, selectedMaster, dislikedTasks, null, 0);
    }

    /**
     * @param selectedMethodId the user's explicit strategy-method choice (the manual override behind
     *     {@link MethodPicker}), remembered per task; null = auto-pick the best feasible method.
     * @param remaining the remaining task kill count (SLAYER_COUNT), sizing the trip's sustain (food vs
     *     restore potions); &le; 0 clamps to a default trip length so the bag is still sensible.
     */
    public Optional<Recommendation> recommend(TaskData task, OwnedItems owned, PlayerStats stats,
        AdviceMode mode, boolean haveCannon, String selectedLocationName, String selectedVariantName,
        CombatStyle selectedMethod, String selectedMaster, Set<String> dislikedTasks,
        String selectedMethodId, int remaining)
    {
        if (task == null)
        {
            return Optional.empty();
        }

        // Resolve the selected variant -> the profile the engine reads (ADR-0010). No/single-variant
        // tasks resolve to the task default (FR-6 byte-identical).
        MonsterVariant variant = MonsterVariant.resolve(task, selectedVariantName);
        MonsterProfile profile = variant == null
            ? MonsterProfile.fromTask(task)
            : MonsterProfile.fromVariant(task, variant);

        // The DEFAULT method is the strategy's primaryStyle when the variant has a (valid) strategy,
        // else the variant's recommended style (ADR-0013 + ADR-0015 "strategy overrides method"). This
        // is derived at advise-time - the variant's authored weakness.style is NOT mutated - so a guide
        // whose style differs from the authored weakness (e.g. Abyssal Sire magic->melee Emberlight)
        // defaults to the guide's style automatically. The user can still toggle the method.
        MonsterStrategy validStrategy = validStrategy(variant);
        CombatStyle defaultMethod = validStrategy != null
            ? validStrategy.getPrimaryStyle()
            : profile.recommendedStyle();
        CombatStyle style = selectedMethod != null ? selectedMethod : defaultMethod;
        if (style == null)
        {
            return Optional.empty(); // no recommended style and no method chosen -> not viable
        }

        // The loadout couples to the EFFECTIVE location = the user-selected one, else the variant's
        // suggested location (ADR-0017 #3/#4). Its Wilderness flag credits Wilderness weapons
        // (ADR-0009 A.5) - so a variant whose SUGGESTED location is Wilderness credits those weapons by
        // default now, not only when the user hand-picks a Wilderness location (PD-1, the one intended
        // behaviour change vs today). Computed before gear selection so it can feed GearSelector.
        SlayerLocation recommendedLocation = chooseLocation(task, variant, style, haveCannon);
        SlayerLocation effectiveLocation = findLocation(task, selectedLocationName)
            .orElse(recommendedLocation);
        boolean wilderness = effectiveLocation != null && effectiveLocation.isWilderness();

        // Dynamic inventory (Phase 1): auto-pick the strategy method (or honour the manual override),
        // constrained to the user's style toggle when set. The picked method's style refines the
        // effective style when the user has NOT toggled one - so the bag/prayers/sizing follow the
        // guide's chosen method. A null pick (no strategy methods) leaves today's style/behaviour intact.
        StrategyMethod pickedMethod = validStrategy == null ? null
            : MethodPicker.pick(validStrategy, owned, effectiveLocation, selectedMethod, selectedMethodId);
        if (selectedMethod == null && pickedMethod != null && pickedMethod.getCombatStyle() != null)
        {
            style = pickedMethod.getCombatStyle();
        }

        // The wiki /Strategies override ids for the EFFECTIVE style (ADR-0015 / MV-S2). Empty unless the
        // variant has a strategy that documents this style and at least one such weapon could apply.
        List<Integer> strategyWeaponIds = strategyOverrideIds(variant, style);

        // Gear is stat-driven over owned items; the profile + effective style drive selection
        // (GearSelector returns an empty map when no weapon is owned for the style). When the strategy
        // override list is non-empty, an owned strategy weapon wins the WEAPON slot (bank-aware).
        Map<EquipmentSlot, Integer> worn = gearSelector.select(owned, profile, style, stats, mode,
            wilderness, strategyWeaponIds);
        if (worn.isEmpty())
        {
            return Optional.empty(); // not viable -> TASK_WITHOUT_LOADOUT (plan section 6)
        }

        String element = profile.element();
        Integer weaponId = worn.get(EquipmentSlot.WEAPON);

        // WD-9 (ADR-0020 #4): on a multi-combat task the magic setup prefers an affordable Ancient
        // Barrage/Burst over the standard book. The multi signal is the effective location's task-scoped
        // LocationQuality.multicombat overlay; absent -> false -> standard book (FR-6).
        boolean multicombat = isMulticombat(effectiveLocation);
        Consumables consumables =
            consumableSelector.select(owned, style, element, stats, weaponId, multicombat);
        int spellBaseMaxHit = consumables.getMagic() == null
            ? 0
            : consumables.getMagic().getSpellBaseMaxHit();
        double dps = dpsEstimator.estimate(style, worn, stats, profile, spellBaseMaxHit);

        long cost = 0;
        for (int id : worn.values())
        {
            cost += Math.max(0, priceService.price(id));
        }

        Recommendation rec = new Recommendation();
        rec.setStyle(style);
        rec.setWorn(worn);
        // Owned-driven inventory supplies for this variant/location (ADR-0017 #4, DT-B9): required
        // item, cannon+cannonballs, antifire - only what the player owns.
        List<Integer> inventory = InventorySelector.select(owned, profile, effectiveLocation, task);
        rec.setInventory(inventory);
        // WA-12 (ADR-0018 #9b): the unlock-gated gear guard over the WHOLE loadout (worn + supplies),
        // read through the player-state seam. A note only - the gear pick above is untouched (NG-4).
        List<Integer> loadoutIds = new ArrayList<>(worn.values());
        loadoutIds.addAll(inventory);
        rec.setUnlockGuardNote(UnlockGatedItems.guardNote(loadoutIds, unlockState));
        // WA-13 (ADR-0018 #9d): the "what to unlock next" hint from points + ownership (seam), the
        // global catalogue, and this task's EXTENSION unlock. Advice only (NG-4); null = no hint.
        rec.setUnlockHint(UnlockAdvisor.nextUnlockHint(unlockState, dataService.rewards(), task));
        rec.setLocation(effectiveLocation);
        rec.setRecommendedLocation(recommendedLocation);
        // Tactical notes from the effective location (DT-B10 / GAP-2): a safespot ranged/magic method
        // hint (a note only, NG-4) and the free-text access note. FE renders each only when non-null.
        rec.setLocationHint(locationHint(effectiveLocation, style));
        rec.setAccessNote(effectiveLocation == null ? null : effectiveLocation.getAccessNote());
        // The PD-3 antifire "note when unowned" nudge (DT-B11): non-null only when the monster is
        // draconic and the player owns no antifire - the unowned counterpart to the InventorySelector
        // antifire supply above. A note only (NG-4); FE renders it only when non-null.
        rec.setAntifireNote(InventorySelector.antifireNote(owned, profile));
        // WD-3 (ADR-0020 #1): the note-only prayer/survival advisory from the monster's offence
        // (prayer from attackStyles, survival from maxHit, owned-driven poison/venom nudge). Null when
        // there is no offence data or no clause applies (FR-6). DRAGONFIRE defers to the antifireNote.
        rec.setSurvivalNote(survivalNote(profile, owned));
        // WD-12 (ADR-0020 #5): the master-aware skip/block advisory over shipped substrate (selected
        // master + MasterData economy block cost + points seam + the config disliked set). Note-only
        // (NG-4); null when the task is not disliked.
        rec.setSkipBlockNote(skipBlockNote(task, selectedMaster, dislikedTasks));
        // WD-6 (ADR-0020 #3): the honest single-target cannon DPS line - a SEPARATE additive display
        // term (never folded into weaponRank, ADR-0008). Non-null only when the effective location
        // supports a cannon AND the player owns one; on a multi-combat spot it also carries the ceiling
        // caveat. FE renders it only when non-null.
        rec.setCannonDpsNote(cannonDpsNote(effectiveLocation, owned, multicombat));
        rec.setLocationReason(locationReason(task, style, stats, owned, haveCannon, recommendedLocation));
        rec.setMethod(task.getRecommendedMethod());
        rec.setEstimatedDps(dps);
        rec.setTotalGearCost(cost);
        rec.setConsumables(consumables);
        // Dynamic inventory (Phase 1): size the trip's sustain (prayer-restore vs food) from the picked
        // method's prayers, the monster's offence, and the remaining kill count, then compose the 28-slot
        // bag in layers (owned base supplies -> method key/inventory supplies -> sized sustain -> food
        // fill). With no picked method + no prayers this reduces to today's supplies + potion x2 + runes
        // + combo x4 + food-fill (FR-6). The missing-key-items advisory lists resolved strategy items the
        // player owns none of (owned-only substitution keeps the loadout usable).
        SustainModel.Result sustain =
            SustainModel.estimate(pickedMethod, profile.getOffence(), dps, stats, remaining);
        TripPlanContext tripContext = new TripPlanContext(owned, profile, task, effectiveLocation,
            validStrategy, pickedMethod, consumables, sustain);
        TripPlanner.TripPlan tripPlan = TripPlanner.plan(tripContext);
        rec.setTripInventory(tripPlan.getSlots());
        rec.setMissingKeyItemsNote(tripPlan.getMissingKeyItemsNote());
        rec.setSustainNote(sustain == null ? null : sustain.getNote());
        if (pickedMethod != null)
        {
            rec.setMethodId(pickedMethod.getMethodId());
            rec.setMethodLabel(pickedMethod.getLabel());
            rec.setPrayers(pickedMethod.getPrayers());
        }
        if (validStrategy != null)
        {
            List<StrategyMethod> options = MethodPicker.pickable(validStrategy);
            rec.setMethodOptions(options.isEmpty() ? null : options);
        }
        // The resolved variant identity for the UI card (FR-7 boss separateness, MV-FE2).
        if (variant != null)
        {
            rec.setVariantName(variant.getName());
            rec.setBoss(variant.isBoss());
            rec.setVariantLocation(variant.getLocation());
            rec.setVariantRequirement(variant.getRequirement());
            // The "Wiki strategy" note (ADR-0015 / MV-S4): shown whenever the variant has a strategy,
            // INDEPENDENT of ownership and the effective style (it tells the user the wiki's answer
            // even when the loadout falls back to a stat pick). Null for a no-strategy variant.
            rec.setStrategyNote(composeStrategyNote(variant.getStrategy()));
            // WB-4 (D8): a PRESENT but malformed strategy used to fall back to the stat engine with
            // only a one-time log.warn - invisible to the player. Disclose it. An absent strategy
            // is normal (most variants) and stays silent.
            if (variant.getStrategy() != null && validStrategy(variant) == null)
            {
                rec.setStrategyFallbackNote("Wiki strategy data unavailable for this variant - "
                    + "gear picked by the stat engine.");
            }
        }
        // No recommended-upgrades concept (ADR-0005 / FR-7): the advisor never builds an upgrade list.
        return Optional.of(rec);
    }

    /**
     * The priority-ordered wiki {@code /Strategies} weapon ids that OVERRIDE the DPS weapon pick for
     * the effective {@code style}, or an empty list when no override applies (ADR-0015 + amended toggle
     * rule). The strategy drives EVERY style it documents, not just the primary: the documented styles
     * are {@code primaryStyle} (served by the priority-ordered {@code primaryWeapons}) plus each
     * secondary weapon's own {@code style} (served by that weapon). When the effective style is the
     * primary style the {@code primaryWeapons} ids lead, followed by any secondary weapon also tagged
     * with that style; when the effective style is a secondary-only style, just those secondaries. An
     * undocumented style yields an empty list (the stat engine drives that style). A null/malformed
     * strategy (null {@code primaryStyle} or empty {@code primaryWeapons}) yields an empty list and a
     * one-time {@code log.warn} - no fabrication, no silent wrong answer.
     */
    private static List<Integer> strategyOverrideIds(MonsterVariant variant, CombatStyle style)
    {
        MonsterStrategy strategy = validStrategy(variant);
        if (strategy == null)
        {
            // A present-but-malformed strategy (null primaryStyle / empty primaryWeapons) warns once;
            // an absent strategy is silent. Either way -> no override, the stat engine drives.
            if (variant != null && variant.getStrategy() != null)
            {
                String key = variant.getName() == null ? "<unnamed>" : variant.getName();
                if (WARNED_MALFORMED_STRATEGY.add(key))
                {
                    log.warn("Ignoring malformed strategy on variant '{}' (null primaryStyle or empty "
                        + "primaryWeapons); using the stat engine", key);
                }
            }
            return Collections.emptyList();
        }
        if (style == null)
        {
            return Collections.emptyList();
        }

        List<Integer> ids = new ArrayList<>();
        // The primary style is served by the priority-ordered primary weapons...
        if (style == strategy.getPrimaryStyle())
        {
            addWeaponIds(ids, strategy.getPrimaryWeapons());
        }
        // ...and each secondary weapon documents its own style (the amended multi-style override). A
        // secondary tagged with the primary style appends after the primaries (union of both sources).
        if (strategy.getSecondaryWeapons() != null)
        {
            for (StrategyWeapon weapon : strategy.getSecondaryWeapons())
            {
                if (weapon != null && style == weapon.getStyle())
                {
                    addWeaponId(ids, weapon);
                }
            }
        }
        return ids;
    }

    /**
     * The "Wiki strategy" guidance line (ADR-0015 / MV-S4), e.g. {@code "Wiki strategy: Emberlight
     * (MELEE); also Scorching bow / Toxic blowpipe (RANGED). <note>"}. Composed whenever a strategy
     * exists (informational, ownership-independent); the SECONDARY weapons live here, never as a second
     * auto-equipped loadout. Secondary weapons sharing a style are priority-ordered ALTERNATIVES, so
     * they join into one {@code " / "} clause per style (like the primaries) rather than reading as
     * "bring all of these" - one "also" clause per style, not per weapon.
     * Returns null when there is no strategy or it is malformed (no usable primary weapons).
     */
    private static String composeStrategyNote(MonsterStrategy strategy)
    {
        if (strategy == null || strategy.getPrimaryStyle() == null
            || strategy.getPrimaryWeapons() == null || strategy.getPrimaryWeapons().isEmpty())
        {
            return null;
        }
        StringBuilder note = new StringBuilder("Wiki strategy: ");
        note.append(weaponNames(strategy.getPrimaryWeapons()))
            .append(" (").append(strategy.getPrimaryStyle()).append(')');
        if (strategy.getSecondaryWeapons() != null)
        {
            Map<CombatStyle, List<String>> byStyle = new LinkedHashMap<>();
            for (StrategyWeapon weapon : strategy.getSecondaryWeapons())
            {
                if (weapon == null || weapon.getName() == null || weapon.getName().trim().isEmpty())
                {
                    continue;
                }
                byStyle.computeIfAbsent(weapon.getStyle(), s -> new ArrayList<>())
                    .add(weapon.getName().trim());
            }
            for (Map.Entry<CombatStyle, List<String>> entry : byStyle.entrySet())
            {
                note.append("; also ").append(String.join(" / ", entry.getValue()));
                if (entry.getKey() != null)
                {
                    note.append(" (").append(entry.getKey()).append(')');
                }
            }
        }
        note.append('.');
        if (strategy.getNote() != null && !strategy.getNote().trim().isEmpty())
        {
            note.append(' ').append(strategy.getNote().trim());
        }
        return note.toString();
    }

    /** Priority-ordered primary weapon display names joined as alternatives (skips null/blank names). */
    private static String weaponNames(List<StrategyWeapon> weapons)
    {
        List<String> names = new ArrayList<>();
        for (StrategyWeapon weapon : weapons)
        {
            if (weapon != null && weapon.getName() != null && !weapon.getName().trim().isEmpty())
            {
                names.add(weapon.getName().trim());
            }
        }
        return String.join(" / ", names);
    }

    /**
     * The variant's strategy if it is structurally valid (non-null {@code primaryStyle} and a non-empty
     * {@code primaryWeapons}), else null. Pure (no logging) - the shared validity gate for both the
     * default-method derivation and the override-id computation (ADR-0015).
     */
    private static MonsterStrategy validStrategy(MonsterVariant variant)
    {
        if (variant == null || variant.getStrategy() == null)
        {
            return null;
        }
        MonsterStrategy strategy = variant.getStrategy();
        if (strategy.getPrimaryStyle() == null || strategy.getPrimaryWeapons() == null
            || strategy.getPrimaryWeapons().isEmpty())
        {
            return null;
        }
        return strategy;
    }

    private static void addWeaponIds(List<Integer> ids, List<StrategyWeapon> weapons)
    {
        for (StrategyWeapon weapon : weapons)
        {
            addWeaponId(ids, weapon);
        }
    }

    private static void addWeaponId(List<Integer> ids, StrategyWeapon weapon)
    {
        if (weapon != null && weapon.getItemId() != null)
        {
            ids.add(weapon.getItemId());
        }
    }

    /**
     * The suggested location for the resolved {@code variant} (ADR-0017 #3): the candidate set is the
     * variant's {@code locationNames}-scoped subset of the task's locations, else - for an unlinked
     * variant (null/empty {@code locationNames}) or a no-variant task - ALL task locations
     * (byte-identical to today, the FR-6 anchor). Today's scoring is then applied WITHIN that set:
     * cannon-if-owned, else burst-if-magic, else the highest {@link LocationQuality#getAmount() amount}
     * (WD-5b, ADR-0020 #2) with authored order as the tie-break - so where no candidate carries a
     * quality overlay the pick degrades to the authored-first location (byte-identical to today, FR-6).
     */
    private SlayerLocation chooseLocation(TaskData task, MonsterVariant variant, CombatStyle style,
        boolean haveCannon)
    {
        List<SlayerLocation> candidates = candidateLocations(task, variant);
        if (candidates.isEmpty())
        {
            return null;
        }
        SlayerLocation cannonLoc = null;
        SlayerLocation burstLoc = null;
        for (SlayerLocation l : candidates)
        {
            if (haveCannon && l.isCannonEffective() && cannonLoc == null)
            {
                cannonLoc = l;
            }
            if (l.isBurst() && burstLoc == null)
            {
                burstLoc = l;
            }
        }
        if (haveCannon && cannonLoc != null)
        {
            return cannonLoc;
        }
        if (style == CombatStyle.MAGIC && burstLoc != null)
        {
            return burstLoc;
        }
        // WD-5b: rank on the quality amount, authored order breaking ties. A strictly-greater test keeps
        // the scan STABLE (first authored wins equal amounts), so all-UNKNOWN quality -> candidates.get(0).
        SlayerLocation best = candidates.get(0);
        for (SlayerLocation l : candidates)
        {
            if (qualityAmount(l) > qualityAmount(best))
            {
                best = l;
            }
        }
        return best;
    }

    /**
     * The {@link LocationQuality#getAmount() amount} ranking key for a location, or {@link
     * Integer#MIN_VALUE} when the location carries no quality overlay or no authored amount - so an
     * absent overlay ranks below any authored amount and never displaces the authored-order pick (FR-6).
     */
    /**
     * Whether the location's task-scoped {@link LocationQuality#getMulticombat() multicombat} overlay is
     * present and true (WD-9 Ancients signal). Absent overlay / null flag -> false (single-target, FR-6).
     */
    /**
     * The honest single-target cannon DPS line (WD-6, ADR-0020 #3), or {@code null} when the effective
     * location does not support a cannon or the player does not own one (FR-6). On a {@code multicombat}
     * spot the single-target number is kept but the ceiling caveat is appended (real rate is higher);
     * multi-target cannon DPS itself stays unmodelled (per-spot density is unsourced).
     */
    static String cannonDpsNote(SlayerLocation location, OwnedItems owned, boolean multicombat)
    {
        if (location == null || !location.isCannonEffective() || !InventorySelector.ownsCannon(owned))
        {
            return null;
        }
        String line = String.format(Locale.ROOT, "Cannon adds ~%.1f DPS (single-target)",
            CannonDpsModel.singleTargetDps());
        return multicombat
            ? line + "; hits multiple targets here, real rate is higher"
            : line;
    }

    private static boolean isMulticombat(SlayerLocation location)
    {
        return location != null && location.getQuality() != null
            && Boolean.TRUE.equals(location.getQuality().getMulticombat());
    }

    private static int qualityAmount(SlayerLocation location)
    {
        if (location == null || location.getQuality() == null
            || location.getQuality().getAmount() == null)
        {
            return Integer.MIN_VALUE;
        }
        return location.getQuality().getAmount();
    }

    /**
     * The task locations that apply to the resolved variant: the variant's {@code locationNames}
     * resolved to the shared task {@link SlayerLocation} objects (preserving the compiler-derived
     * order, home location first), or ALL task locations when the variant has no per-variant linkage
     * (null/empty {@code locationNames} - the fallback sentinel, ADR-0017 #1). A non-empty
     * {@code locationNames} whose entries resolve to nothing (a data inconsistency) also falls back to
     * all task locations - safe, never fabricated.
     */
    private static List<SlayerLocation> candidateLocations(TaskData task, MonsterVariant variant)
    {
        List<SlayerLocation> all = task.getLocations();
        if (all == null || all.isEmpty())
        {
            return Collections.emptyList();
        }
        List<String> names = variant == null ? null : variant.getLocationNames();
        if (names == null || names.isEmpty())
        {
            return all;
        }
        List<SlayerLocation> subset = new ArrayList<>();
        for (String name : names)
        {
            for (SlayerLocation l : all)
            {
                // The shared trimmed/case-insensitive rule (WB-2 / D6) - the same match the panel
                // dropdown filter applies, so the two candidate sets cannot disagree on drift.
                if (MonsterVariant.locationNameMatches(name, l.getName()))
                {
                    subset.add(l);
                    break;
                }
            }
        }
        return subset.isEmpty() ? all : subset;
    }

    /**
     * The safespot method hint for the effective location (ADR-0017 #4 / GAP-2): when the location has
     * a safespot AND the effective style is ranged or magic, suggest using it. Safespotting is a
     * ranged/magic tactic, so a melee style gets no hint; a non-safespot location gets none either.
     * This is a NOTE only - it never changes the combat maths (NG-4).
     */
    private static String locationHint(SlayerLocation location, CombatStyle style)
    {
        if (location == null || !location.isSafeSpot())
        {
            return null;
        }
        if (style == CombatStyle.RANGED || style == CombatStyle.MAGIC)
        {
            return "Safespot available - attack with " + style + " from a safe tile.";
        }
        return null;
    }

    // WD-3 owned-driven cure lists (ids from net.runelite.api.ItemID, pinned - not raw literals - so a
    // client-constant drift fails to compile, the R3 discipline). Anti-venom cures BOTH venom and
    // poison; antipoison cures poison only.
    private static final int[] VENOM_CURES = {
        net.runelite.api.ItemID.ANTIVENOM4, net.runelite.api.ItemID.ANTIVENOM3,
        net.runelite.api.ItemID.ANTIVENOM2, net.runelite.api.ItemID.ANTIVENOM1,
        net.runelite.api.ItemID.ANTIVENOM4_12913, net.runelite.api.ItemID.ANTIVENOM3_12915,
        net.runelite.api.ItemID.ANTIVENOM2_12917, net.runelite.api.ItemID.ANTIVENOM1_12919,
        net.runelite.api.ItemID.EXTENDED_ANTIVENOM4, net.runelite.api.ItemID.EXTENDED_ANTIVENOM3,
        net.runelite.api.ItemID.EXTENDED_ANTIVENOM2, net.runelite.api.ItemID.EXTENDED_ANTIVENOM1,
    };
    private static final int[] POISON_CURES = {
        net.runelite.api.ItemID.ANTIPOISON4, net.runelite.api.ItemID.ANTIPOISON3,
        net.runelite.api.ItemID.ANTIPOISON2, net.runelite.api.ItemID.ANTIPOISON1,
        net.runelite.api.ItemID.SUPERANTIPOISON4, net.runelite.api.ItemID.SUPERANTIPOISON3,
        net.runelite.api.ItemID.SUPERANTIPOISON2, net.runelite.api.ItemID.SUPERANTIPOISON1,
        net.runelite.api.ItemID.ANTIPOISON_MIX2, net.runelite.api.ItemID.ANTIPOISON_MIX1,
        net.runelite.api.ItemID.ANTIPOISON_SUPERMIX2, net.runelite.api.ItemID.ANTIPOISON_SUPERMIX1,
        // anti-venom also cures poison
        net.runelite.api.ItemID.ANTIVENOM4, net.runelite.api.ItemID.ANTIVENOM3,
        net.runelite.api.ItemID.ANTIVENOM2, net.runelite.api.ItemID.ANTIVENOM1,
        net.runelite.api.ItemID.ANTIVENOM4_12913, net.runelite.api.ItemID.ANTIVENOM3_12915,
        net.runelite.api.ItemID.ANTIVENOM2_12917, net.runelite.api.ItemID.ANTIVENOM1_12919,
        net.runelite.api.ItemID.EXTENDED_ANTIVENOM4, net.runelite.api.ItemID.EXTENDED_ANTIVENOM3,
        net.runelite.api.ItemID.EXTENDED_ANTIVENOM2, net.runelite.api.ItemID.EXTENDED_ANTIVENOM1,
    };

    /**
     * The note-only prayer/survival advisory (WD-3, ADR-0020 #1), composed from the resolved monster's
     * {@link MonsterProfile#getOffence() offence}: each clause is emitted only when its datum is present.
     * <ul>
     *   <li>Prayer: the overhead protection prayers for {@code attackStyles} (MELEE/RANGED/MAGIC;
     *       DRAGONFIRE defers to {@code antifireNote}, TYPELESS is unprayable).</li>
     *   <li>Survival: "Max hit ~N; keep your HP above it." keyed to {@code maxHit} - never a computed
     *       food quantity (fabrication risk, ADR-0020 #1).</li>
     *   <li>Poison/venom: an owned-driven antivenom/antipoison nudge (venom cure also covers poison).</li>
     * </ul>
     * Returns null when there is no offence data or no clause applies (FR-6). A NOTE only (NG-4).
     */
    private static String survivalNote(MonsterProfile profile, OwnedItems owned)
    {
        if (profile == null || profile.getOffence() == null)
        {
            return null;
        }
        MonsterOffence offence = profile.getOffence();
        List<String> clauses = new ArrayList<>();
        String prayer = prayerClause(offence.getAttackStyles());
        if (prayer != null)
        {
            clauses.add(prayer);
        }
        if (offence.getMaxHit() != null)
        {
            clauses.add("Max hit ~" + offence.getMaxHit() + "; keep your HP above it.");
        }
        // Venom dominates poison (anti-venom cures both); each nudge is owned-driven, like the antifire
        // nudge - suppressed when the matching cure is already owned.
        if (offence.isVenomous() && !ownsAny(owned, VENOM_CURES))
        {
            clauses.add("Venomous - bring antivenom.");
        }
        else if (offence.isPoisonous() && !ownsAny(owned, POISON_CURES))
        {
            clauses.add("Poisonous - bring antipoison.");
        }
        return clauses.isEmpty() ? null : String.join(" ", clauses);
    }

    /**
     * The overhead-prayer clause for the monster's attack styles, or null when none is prayable. Distinct
     * styles are de-duplicated and joined (e.g. "Protect from Melee/Magic."). DRAGONFIRE and TYPELESS
     * yield no prayer (dragonfire defers to the antifire note; typeless is unprayable).
     */
    private static String prayerClause(List<AttackStyle> styles)
    {
        if (styles == null || styles.isEmpty())
        {
            return null;
        }
        List<String> prayers = new ArrayList<>();
        for (AttackStyle style : styles)
        {
            String prayer = prayerName(style);
            if (prayer != null && !prayers.contains(prayer))
            {
                prayers.add(prayer);
            }
        }
        return prayers.isEmpty() ? null : "Protect from " + String.join("/", prayers) + ".";
    }

    /** The overhead protection prayer target for a prayable attack style, or null (unprayable). */
    private static String prayerName(AttackStyle style)
    {
        if (style == null)
        {
            return null;
        }
        switch (style)
        {
            case MELEE:
                return "Melee";
            case RANGED:
                return "Missiles";
            case MAGIC:
                return "Magic";
            default:
                return null; // DRAGONFIRE defers to antifireNote; TYPELESS is unprayable
        }
    }

    /** True when the player owns any id in {@code ids} (null owned -> treated as none owned). */
    private static boolean ownsAny(OwnedItems owned, int[] ids)
    {
        if (owned == null)
        {
            return false;
        }
        for (int id : ids)
        {
            if (owned.has(id))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * The skip/block advisory (WD-12, ADR-0020 #5), note-only: when the CURRENT task is in the
     * config-declared disliked set, suggest blocking it with the selected master (when that master can
     * block AND the player can afford the block cost, from {@link MasterData} economy + the point seam)
     * and always surface the Turael/Spria free-skip escape (low-tier reset, always reachable). Returns
     * null when the task is not disliked, and never asserts a cost it cannot substantiate (unknown
     * points -> no block clause). Weight informs nothing here (assignment frequency does not gate the
     * advice). A NOTE only (NG-4).
     */
    private String skipBlockNote(TaskData task, String selectedMaster, Set<String> dislikedTasks)
    {
        if (task == null || dislikedTasks == null || dislikedTasks.isEmpty() || !isDisliked(task, dislikedTasks))
        {
            return null;
        }
        String taskName = task.getTask() == null ? "this task" : task.getTask();
        List<String> clauses = new ArrayList<>();
        // Block clause: only when the selected master can block (non-zero-points, has a block cost) AND
        // the point balance is known AND sufficient - so we never claim an unaffordable/unknown cost.
        MasterEconomy economy = selectedMaster == null ? null
            : dataService.masterById(selectedMaster).map(MasterData::getEconomy).orElse(null);
        if (economy != null && !economy.isZeroPoints() && economy.getBlockCost() != null)
        {
            java.util.OptionalInt points = unlockState.rewardPoints();
            int cost = economy.getBlockCost();
            if (points.isPresent() && points.getAsInt() >= cost)
            {
                clauses.add("Consider blocking " + taskName + " (costs " + cost + " points; you have "
                    + points.getAsInt() + ").");
            }
        }
        // Free-skip clause: Turael/Spria reset the task for free (at the cost of the streak) and are
        // always reachable - the honest escape when a block is unaffordable or the master cannot block.
        clauses.add("Turael can skip this free (resets your streak).");
        return String.join(" ", clauses);
    }

    /** True when the task name is in the disliked set, matched trimmed + case-insensitively. */
    private static boolean isDisliked(TaskData task, Set<String> dislikedTasks)
    {
        String name = task.getTask();
        if (name == null)
        {
            return false;
        }
        String needle = name.trim().toLowerCase(java.util.Locale.ROOT);
        for (String disliked : dislikedTasks)
        {
            if (disliked != null && disliked.trim().toLowerCase(java.util.Locale.ROOT).equals(needle))
            {
                return true;
            }
        }
        return false;
    }

    private Optional<SlayerLocation> findLocation(TaskData task, String selectedLocationName)
    {
        if (selectedLocationName == null || selectedLocationName.trim().isEmpty()
            || task.getLocations() == null)
        {
            return Optional.empty();
        }
        for (SlayerLocation location : task.getLocations())
        {
            if (selectedLocationName.equals(location.getName()))
            {
                return Optional.of(location);
            }
        }
        return Optional.empty();
    }

    private String locationReason(TaskData task, CombatStyle style, PlayerStats stats,
        OwnedItems owned, boolean haveCannon, SlayerLocation location)
    {
        List<String> parts = new ArrayList<>();
        parts.add("Gear: " + style);
        if (stats == null)
        {
            parts.add("Slayer: unknown/" + task.getSlayerLevel());
        }
        else
        {
            parts.add("Slayer: " + stats.getSlayer() + "/" + task.getSlayerLevel());
        }
        parts.add("Unlocks: " + unlockSummary(task, owned));
        if (location != null)
        {
            List<String> locationTraits = new ArrayList<>();
            if (location.isCannonEffective())
            {
                locationTraits.add(haveCannon ? "cannon enabled" : "cannon permitted");
            }
            if (location.isBurst())
            {
                locationTraits.add("burstable");
            }
            if (location.isMulti())
            {
                locationTraits.add("multi-combat");
            }
            if (!locationTraits.isEmpty())
            {
                parts.add("Location: " + String.join(", ", locationTraits));
            }
        }
        return String.join(" | ", parts);
    }

    private String unlockSummary(TaskData task, OwnedItems owned)
    {
        List<String> unlocks = new ArrayList<>();
        if (task.getQuestReqs() != null)
        {
            unlocks.addAll(task.getQuestReqs());
        }
        if (task.getRequiredItemName() != null)
        {
            if (task.getRequiredItemId() != null && owned != null && owned.has(task.getRequiredItemId()))
            {
                unlocks.add("owns " + task.getRequiredItemName());
            }
            else
            {
                unlocks.add("needs " + task.getRequiredItemName());
            }
        }
        return unlocks.isEmpty() ? "none" : String.join(", ", unlocks);
    }
}

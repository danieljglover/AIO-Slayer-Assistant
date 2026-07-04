package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.SlayerLocation;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Recommendation
{
    private CombatStyle style;
    private Map<EquipmentSlot, Integer> worn;
    private List<Integer> inventory;
    // The full suggested 28-slot trip inventory composed by TripInventoryPlanner from the owned
    // supplies + consumables (rendered by the loadout card's inventory grid); ids are drawn only from
    // `inventory` and `consumables`, so LoadoutItems name/price coverage is unchanged.
    private List<TripSlot> tripInventory;
    private SlayerLocation location;
    private SlayerLocation recommendedLocation;
    private String locationReason;
    private String method;
    private double estimatedDps;
    private long totalGearCost;
    private Consumables consumables;
    // The resolved monster-variant identity for the UI card (MV-B5/MV-FE2). variantName is null for a
    // no-variant task; boss drives the FR-7 "Boss - separate trip" note; variantLocation/requirement
    // are the variant's free-text notes (ADR-0011, v1 location is a note not a rich SlayerLocation).
    private String variantName;
    private boolean boss;
    private String variantLocation;
    private String variantRequirement;
    // The composed "Wiki strategy" guidance line (ADR-0015 / MV-S4), set whenever the resolved variant
    // has a strategy - INFORMATIONAL and ownership-independent (it tells the user the wiki's answer even
    // when the loadout recommends a stat-driven fallback). Null when the variant has no strategy.
    private String strategyNote;
    // WB-4 (D8): the visible strategy-fallback disclosure - non-null ONLY when the resolved variant
    // HAS a strategy but it is malformed (null primaryStyle / empty primaryWeapons), so the gear was
    // picked by the stat engine instead of the wiki guide. An absent strategy is normal and stays
    // silent. FE renders it only when non-null (text/secondary - a data-quality notice).
    private String strategyFallbackNote;
    // DT-B10 (ADR-0017 #4 / GAP-2): tactical notes derived from the EFFECTIVE (suggested-or-selected)
    // location. locationHint is the safespot method hint - set only when the effective location has a
    // safespot AND the effective style is ranged or magic (a NOTE, never a combat-maths change, NG-4);
    // null for melee or a non-safespot location. accessNote is the effective location's free-text
    // travel/access note (quest/teleport/agility gate), or null. FE renders each only when non-null,
    // mirroring strategyNote/bossNote.
    private String locationHint;
    private String accessNote;
    // DT-B11 (ADR-0017 #4 / PD-3): the antifire "note when unowned" nudge - the unowned counterpart to
    // the InventorySelector antifire supply. Set to a non-null recommendation string ONLY when the
    // monster is draconic (the dragon flag) AND the player owns no antifire protection (potion or
    // shield); null for a non-draconic task or when antifire is already owned (the supply covers that
    // case). A NOTE only - never a combat-maths change (NG-4). FE renders it only when non-null,
    // mirroring locationHint/accessNote/strategyNote.
    private String antifireNote;
    // WD-3 (ADR-0020 #1): the note-only prayer/survival advisory composed from the resolved monster's
    // offence (MonsterProfile.offence): the overhead-prayer clause from attackStyles, the max-hit
    // survival line, and an owned-driven antipoison/antivenom nudge. Non-null only when offence data is
    // present and at least one clause applies; null for a monster with no authored offence (FR-6). A
    // NOTE only - never a combat-maths change (NG-4). FE renders it only when non-null, mirroring
    // antifireNote. DRAGONFIRE defers to antifireNote (no duplication); TYPELESS is unprayable.
    private String survivalNote;
    // WA-12 (ADR-0018 #9b): the unlock-gated gear guard - non-null ONLY when a recommended item's
    // gear family is gated behind a reward-shop unlock the player has not bought (curated map in
    // UnlockGatedItems; ownership read through the SlayerUnlockStateProvider seam). A NOTE only,
    // never a gear or combat-maths change (NG-4). FE renders it only when non-null, mirroring the
    // strategy/antifire notes.
    private String unlockGuardNote;
    // WA-13 (ADR-0018 #9d): the "what to unlock next" hint - the highest-value affordable unowned
    // unlock from the player's points + ownership (seam), the global rewards catalogue, and the
    // current task's EXTENSION unlock (UnlockAdvisor). Null when the balance is unknown, nothing is
    // affordable, or everything is owned. Advice only (NG-4); FE renders it only when non-null, in
    // text/secondary (a points-spending lever, like the bank nudges).
    private String unlockHint;
    // WD-12 (ADR-0020 #5): the master-aware skip/block advisory - non-null ONLY when the current task is
    // in the config-declared disliked set. Suggests blocking with the selected master when affordable
    // (from MasterData economy + the point seam) and always surfaces the Turael/Spria free-skip escape.
    // Never asserts an unknown/unaffordable cost. A NOTE only (NG-4); rendered as a Task-section note
    // (mirrors task-master-context). FE renders it only when non-null.
    private String skipBlockNote;
    // WD-6 (ADR-0020 #3): the honest single-target cannon DPS line - non-null ONLY when the effective
    // location supports a cannon AND the player owns one. A SEPARATE additive display term beside the
    // worn-gear "Est. DPS" (never folded into weapon ranking, ADR-0008); on a multi-combat spot it also
    // carries the ceiling caveat (real rate is higher). FE renders it only when non-null.
    private String cannonDpsNote;
    // Dynamic-inventory (Phase 1): the strategy method the trip inventory was built for. methodId is the
    // stable id (persisted as the per-task manual override); methodLabel is the human label for the
    // method selector and the loadout card. Both null when the variant has no strategy methods (a
    // pre-methods or .md-only strategy) - the loadout is then byte-identical to today (FR-6).
    private String methodId;
    private String methodLabel;
    // The selected method's prayers, verbatim ("Protect from Melee", "Piety"); null/empty when the
    // method authored none. Rendered as a loadout note row; never a combat-maths input (NG-4).
    private java.util.List<String> prayers;
    // The SustainModel sizing line ("Prayer-primary: ~6 restore potions for 112 kills..."); null when
    // the method uses no prayers (nothing to say). A note only.
    private String sustainNote;
    // The "strategy recommends but you own none" advisory from the trip planner (owned-only
    // substitution keeps the loadout usable; this tells the player what to buy). Null when nothing is
    // missing. A note only.
    private String missingKeyItemsNote;
    // The pickable strategy methods (style-bearing) for the method selector; null/empty when the variant
    // has no strategy methods (the selector is then hidden). The selected one is methodId above.
    private java.util.List<com.danieljglover.allinslayer.model.StrategyMethod> methodOptions;
    // Phase 2 live diff: per trip-item-id and per worn-slot carry status (carried/partial/missing) vs
    // what the player is currently carrying, computed by the plugin from InventoryService.liveCarried()
    // after this recommendation is built. Null when no diff was computed (e.g. bank never scanned); the
    // grids then render without tint (today's look). Never a combat-maths input (NG-4).
    private Map<Integer, LoadoutDiff.Status> inventoryDiff;
    private Map<EquipmentSlot, LoadoutDiff.Status> wornDiff;
}

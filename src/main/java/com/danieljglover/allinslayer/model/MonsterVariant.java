package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One selectable monster under a {@link TaskData} (MV-A1 design section 2, ADR-0010/0011). A task's
 * {@code variants} list lets the user pick WHICH monster of the assignment to gear for; the selected
 * variant resolves to a {@link com.danieljglover.allinslayer.loadout.MonsterProfile} that drives the
 * engine. A task with no {@code variants} (the back-compat default) keeps using its task-level profile.
 *
 * <p>Nullable {@code weakness}/{@code monsterDefence} mean "inherit the task default" (ADR-0012.3, for
 * UNKNOWN-weakness variants like Vampyres / Bloodveld); the category flags are PER-VARIANT (a demon
 * sibling credits demonbane while a non-demon sibling under the same task does not, FR-5). Per ADR-0011
 * the variant axis is the monster profile only - {@code location}/{@code requirement} are free-text
 * notes in v1, NOT a rich {@link SlayerLocation}.
 */
@Data
@NoArgsConstructor
public class MonsterVariant
{
    private String name;                 // display name, e.g. "K'ril Tsutsaroth"
    private List<Integer> npcIds;        // identity (may be null/empty when UNKNOWN)
    private Integer combatLevel;         // display only (nullable)
    private Weakness weakness;           // nullable -> inherit task default (style = recommended)
    private MonsterDefence monsterDefence; // nullable -> inherit task default
    private boolean demon;               // category flags FOR THIS VARIANT (engine bonus-applicability)
    private boolean dragon;
    private boolean kalphite;
    private boolean undead;
    private boolean isBoss;              // FR-7 marker ("boss - separate trip")
    private boolean isDefault;           // OQ-2: the default selection; exactly one per multi-variant task
    private String location;             // free-text location label (note only in v1, ADR-0011)
    // DT-B3 (ADR-0017): the NAMED SUBSET of the task's locations that apply to THIS variant, derived
    // by the compiler (DT-B5) from the variant's locationId + the task's variantInfo[].locations. Gson
    // default null; null OR empty is the fallback sentinel = "all task locations apply", which keeps
    // unlinked variants (and every boss variant, GAP-3) byte-identical to today (FR-6). Names are a
    // subset of the task's SlayerLocation names, in task order.
    private List<String> locationNames;
    private String requirement;          // free-text gating (quest/item/slayer) - display only
    // Optional varbit SLAYER_TARGET_BOSSID (4723) value for live boss pre-selection on the Boss task
    // (ADR-0014 / MV-B8). Nullable; the exact bossId mapping is a live-verify item - unmapped falls
    // through to the deterministic default, never fabricated.
    private Integer bossId;
    // Optional wiki /Strategies gear guide (ADR-0015 / MV-S1). Gson default null = no strategy = today's
    // pure-DPS behaviour (FR-6 / FR-S4). When present + applicable, its weapon overrides the DPS pick.
    private MonsterStrategy strategy;
    // WD-2 (ADR-0020 #1): the per-variant monster-offence domain (HP/max-hit/attack-styles/magic
    // level/poison), overlaid onto MonsterProfile with a task-level fallback. Gson default null =
    // no offence = no advisory (FR-6).
    private MonsterOffence offence;

    /**
     * Resolve the selected variant of a task deterministically (OQ-2 / ADR-0014): the name match if
     * any, else the {@code isDefault} variant, else the first listed, else null (a task with no
     * variants resolves to its task-level default profile). For the all-boss Boss task this always
     * lands on a boss variant.
     *
     * <p>This is the ONE shared resolution used by BOTH the loadout engine (which drives the
     * recommendation) and the panel (which pre-selects the variant), so the panel's pre-selected
     * variant can never drift from the variant the advisor geared for (the FR-6 default-variant
     * coupling, code-review S1). Null-task safe for the panel's pre-render path.
     *
     * @param task the assignment; null or no variants -> null (use the task-level profile)
     * @param selectedName the user-selected variant name; null -> the default rule
     * @return the resolved variant, or null
     */
    public static MonsterVariant resolve(TaskData task, String selectedName)
    {
        if (task == null)
        {
            return null;
        }
        List<MonsterVariant> variants = task.getVariants();
        if (variants == null || variants.isEmpty())
        {
            return null;
        }
        if (selectedName != null)
        {
            for (MonsterVariant v : variants)
            {
                if (selectedName.equals(v.getName()))
                {
                    return v;
                }
            }
        }
        for (MonsterVariant v : variants)
        {
            if (v.isDefault())
            {
                return v;
            }
        }
        return variants.get(0);
    }

    /**
     * The ONE location-name match rule (WB-2 / D6): trimmed + case-insensitive, defensive against
     * display-string drift between a variant's compiler-derived {@code locationNames} and the
     * task's authored {@code SlayerLocation} names. Both the panel dropdown filter and the
     * advisor's candidate set delegate here, so they can never disagree on a drifted row. Null
     * never matches - no fabricated links.
     */
    public static boolean locationNameMatches(String locationName, String candidate)
    {
        if (locationName == null || candidate == null)
        {
            return false;
        }
        return locationName.trim().equalsIgnoreCase(candidate.trim());
    }
}

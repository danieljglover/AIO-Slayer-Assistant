package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A monster variant's wiki {@code /Strategies} gear guide (ADR-0015 / MV-S1). Optional and nested under
 * {@link MonsterVariant#getStrategy()} (Gson default null = no strategy = today's pure-DPS behaviour, the
 * FR-6 / FR-S4 regression anchor). When present and applicable the guide's weapon OVERRIDES the computed
 * DPS weapon pick for the WEAPON slot; every other slot stays stat-driven over owned items (ADR-0015
 * decision 1). The override is bank-aware: it only ever recommends a weapon the player OWNS, else it
 * falls back to the stat-driven pick (ADR-0015 decision 2).
 *
 * <p><b>Amended toggle rule (supersedes ADR-0015 FR-S3 "primary only"):</b> the strategy drives EVERY
 * style it documents, not just the primary. The documented styles are {@code primaryStyle} (served by
 * {@code primaryWeapons}) plus each secondary weapon's own {@code style} (served by that weapon). When the
 * effective method is a documented style and the player owns a documented weapon for it, that weapon wins
 * the WEAPON slot. When the effective method is NOT documented (e.g. magic on a melee+ranged strategy) the
 * stat engine drives normally. The policy (which ids apply for a style) lives in {@code LoadoutAdvisor}.
 */
@Data
@NoArgsConstructor
public class MonsterStrategy
{
    private CombatStyle primaryStyle;              // the default method this strategy sets (== variant weakness style)
    private List<StrategyWeapon> primaryWeapons;   // priority order; highest-priority OWNED one wins the WEAPON slot
    private List<StrategyWeapon> secondaryWeapons; // each carries its own style; documents extra methods (note + override)
    private String note;                           // free-text "Wiki strategy" guidance line (nullable)
    private String sourceUrl;                      // the /Strategies page (provenance; display/audit only)
}

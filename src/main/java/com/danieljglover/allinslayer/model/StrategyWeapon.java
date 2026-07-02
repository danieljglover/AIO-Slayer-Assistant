package com.danieljglover.allinslayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One weapon named by a monster's wiki {@code /Strategies} page (ADR-0015 / MV-S1). The {@code itemId}
 * is resolved at AUTHORING time from {@code docs/loadout/weapon-reference-1h.md}/{@code -2h.md} and baked
 * into {@code slayer-data.json} as a raw int (the same raw-id keying {@code WeaponEffectRegistry} /
 * {@code ConditionalBonusRegistry} use - never a runtime {@code ItemManager} name lookup, never an
 * {@code ItemVariationMapping} collapse). Charge/recolour variants a player might own are listed as
 * additional priority entries on the owning {@link MonsterStrategy}.
 *
 * <p>{@code style} is nullable on PRIMARY weapons - they use the strategy's {@code primaryStyle}. On a
 * SECONDARY weapon it names the style that weapon is for (e.g. a Scorching bow for the ranged window),
 * which is the style key the override gate matches against (MV-S2 amended toggle rule).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyWeapon
{
    private String name;       // display name (provenance / the "Wiki strategy" note text)
    private Integer itemId;    // raw item id, matched against owned.ids(); nullable if unresolved
    private CombatStyle style; // the style this weapon is for; null on a primary (uses primaryStyle)
}

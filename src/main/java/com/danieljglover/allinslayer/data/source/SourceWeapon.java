package com.danieljglover.allinslayer.data.source;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A {@code weapons/*.json} row: a build-time weapon-name -> item-id lookup for strategy
 * resolution, and nothing more (live stats come from {@code ItemManager} at runtime; special
 * effects from the curated Java registries). WB-8 (PD-C): the five dead fields the audit found
 * ({@code slot}/{@code styles}/{@code attackSpeedTicks}/{@code effects}/{@code aliases}) were
 * parsed but never populated by any of the 79 files nor read by the compiler - deleted, and
 * {@code WeaponSourceSchemaTest} pins the schema to exactly these three so they stay gone.
 */
@Data
@NoArgsConstructor
public class SourceWeapon
{
    private String weaponId;
    private String name;
    private List<Integer> itemIds;
}

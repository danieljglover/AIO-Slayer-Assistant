package com.danieljglover.allinslayer.loadout;

import java.util.Map;
import lombok.Value;

/**
 * The magic half of a {@link Consumables} recommendation (plan section 5): the chosen element, the
 * spell to cast and its base max hit (fed to the DPS estimator, ADR-0006), whether the weapon is a
 * powered staff (which supplies its own attack so needs no runes), the per-cast rune requirement
 * ({@code runeId -> count}), and any runes the player is short of ({@code runeId -> missing count})
 * so the panel can flag them.
 *
 * <p>Immutable value type: equality is by field value (NFR-4 self-diff). The two maps should be
 * supplied unmodifiable by the selector.
 */
@Value
public class MagicSetup
{
    String element;
    String spellName;
    int spellBaseMaxHit;
    boolean poweredStaff;
    Map<Integer, Integer> runeRequirement;
    Map<Integer, Integer> runesShort;
}

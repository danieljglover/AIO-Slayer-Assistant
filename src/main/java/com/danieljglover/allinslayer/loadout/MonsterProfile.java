package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.Weakness;
import lombok.Value;

/**
 * The immutable monster profile the loadout engine consumes (MV-A1 design section 3, ADR-0010): the
 * weakness (recommended style + element), the defence profile, and the category flags that gate the
 * bane bonuses. The engine reads THIS instead of {@link TaskData} so the source of the
 * weakness/defence/flags can move from "the task" to "the user-selected variant" without any
 * combat-maths change (NG-4).
 *
 * <p>{@link #fromTask(TaskData)} reproduces today's task-level profile and is the one-line adapter that
 * migrates every existing engine call site green - the FR-6 regression anchor. {@link
 * #fromVariant(TaskData, MonsterVariant)} overlays the variant's fields with a per-field fallback to the
 * task default (ADR-0012.3) and takes the category flags from the variant (FR-5). {@code
 * slayerHelmApplies} always comes from the task (it is a task property, not a per-monster one).
 *
 * <p>The {@code weakness.style} here is the RECOMMENDED (default) combat style; the style the engine
 * actually uses comes from the method selector (ADR-0013) - see {@link #recommendedStyle()}.
 */
@Value
public class MonsterProfile
{
    Weakness weakness;
    MonsterDefence defence;
    boolean demon;
    boolean dragon;
    boolean kalphite;
    boolean undead;
    boolean slayerHelmApplies;
    // WD-2 (ADR-0020 #1): the monster's offence (what it does to the player), nullable. Drives the
    // note-only prayer/survival advisory (WD-3); absent -> null -> no note (FR-6). Never a maths input.
    MonsterOffence offence;

    /** Today's task-level profile (FR-6 anchor); null-safe for tasks with no weakness/defence. */
    public static MonsterProfile fromTask(TaskData t)
    {
        if (t == null)
        {
            return new MonsterProfile(null, null, false, false, false, false, false, null);
        }
        return new MonsterProfile(t.getWeakness(), t.getMonsterDefence(),
            t.isDemon(), t.isDragon(), t.isKalphite(), t.isUndead(), t.isSlayerHelmApplies(),
            t.getOffence());
    }

    /**
     * The profile for a selected variant: weakness/defence come from the variant when set, else fall
     * back to the task default per field (ADR-0012.3); the category flags come from the variant (FR-5);
     * {@code slayerHelmApplies} comes from the task.
     */
    public static MonsterProfile fromVariant(TaskData t, MonsterVariant v)
    {
        if (v == null)
        {
            return fromTask(t);
        }
        Weakness weakness = v.getWeakness() != null ? v.getWeakness()
            : (t == null ? null : t.getWeakness());
        MonsterDefence defence = v.getMonsterDefence() != null ? v.getMonsterDefence()
            : (t == null ? null : t.getMonsterDefence());
        boolean slayerHelm = t != null && t.isSlayerHelmApplies();
        // Offence overlays the variant's, falling back to the task-level offence (the exact
        // null-fallback idiom used for weakness/defence above); absent everywhere -> null (FR-6).
        MonsterOffence offence = v.getOffence() != null ? v.getOffence()
            : (t == null ? null : t.getOffence());
        return new MonsterProfile(weakness, defence,
            v.isDemon(), v.isDragon(), v.isKalphite(), v.isUndead(), slayerHelm, offence);
    }

    /** The recommended (default) combat style - the method selector's default (ADR-0013). */
    public CombatStyle recommendedStyle()
    {
        return weakness == null ? null : weakness.getStyle();
    }

    /** The wiki-accurate magic element for the magic spell pick (inert unless the method is magic). */
    public String element()
    {
        return weakness == null ? null : weakness.getElement();
    }
}

package com.danieljglover.allinslayer.loadout;

/**
 * The task predicate that gates a {@link ConditionalBonus} (ADR-0007). Each constant tests one fact
 * on the {@link BonusContext}. Extensible: a future conditional source whose bonus depends on a new
 * task predicate adds a constant here (plus its registry row), no {@code GearSelector} change.
 */
public enum BonusCondition
{
    /** Slayer-helm / Black-mask family: applies when the task counts for the Slayer-helm bonus. */
    ON_SLAYER_TASK
    {
        @Override
        public boolean test(BonusContext ctx)
        {
            return ctx.isSlayerHelmApplies();
        }
    },

    /** Salve-amulet family: applies when the task monster is undead. */
    VS_UNDEAD
    {
        @Override
        public boolean test(BonusContext ctx)
        {
            return ctx.isUndead();
        }
    },

    /** Dragonbane (Dragon hunter lance): applies when the task monster is draconic. */
    VS_DRAGON
    {
        @Override
        public boolean test(BonusContext ctx)
        {
            return ctx.isDragon();
        }
    },

    /** Demonbane (Arclight/Emberlight/Darklight/Scorching bow): applies when the monster is a demon. */
    VS_DEMON
    {
        @Override
        public boolean test(BonusContext ctx)
        {
            return ctx.isDemon();
        }
    },

    /** Keris family: applies when the task monster is a kalphite (or scarab). */
    VS_KALPHITE
    {
        @Override
        public boolean test(BonusContext ctx)
        {
            return ctx.isKalphite();
        }
    },

    /** Wilderness weapons (Viggora's/Craw's/Webweaver/Thammaron's): applies in a Wilderness location. */
    VS_WILDERNESS
    {
        @Override
        public boolean test(BonusContext ctx)
        {
            return ctx.isWilderness();
        }
    };

    /** @return whether this condition holds for the given task context. */
    public abstract boolean test(BonusContext ctx);
}

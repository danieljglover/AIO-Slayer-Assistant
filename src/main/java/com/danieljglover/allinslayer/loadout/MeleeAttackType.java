package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.MonsterDefence;

/**
 * The three melee attack types. Each knows how to read the matching defensive bonus off a
 * {@link MonsterDefence} and the matching offensive attack bonus off a {@link Bonuses}, so the gear
 * selector can pick the type a monster is weakest to (argmin defence) and score weapons for it
 * (plan section 4, steps 2-3).
 */
public enum MeleeAttackType
{
    STAB
    {
        @Override
        public int monsterDefence(MonsterDefence d)
        {
            return d.getStab();
        }

        @Override
        public int attackBonus(Bonuses b)
        {
            return b.getAstab();
        }
    },
    SLASH
    {
        @Override
        public int monsterDefence(MonsterDefence d)
        {
            return d.getSlash();
        }

        @Override
        public int attackBonus(Bonuses b)
        {
            return b.getAslash();
        }
    },
    CRUSH
    {
        @Override
        public int monsterDefence(MonsterDefence d)
        {
            return d.getCrush();
        }

        @Override
        public int attackBonus(Bonuses b)
        {
            return b.getAcrush();
        }
    };

    /** The monster's defensive bonus against this attack type. */
    public abstract int monsterDefence(MonsterDefence d);

    /** The item's offensive attack bonus for this attack type. */
    public abstract int attackBonus(Bonuses b);
}

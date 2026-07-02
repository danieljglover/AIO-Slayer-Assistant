package com.danieljglover.allinslayer.loadout;

import static org.junit.Assert.assertEquals;

import com.danieljglover.allinslayer.model.MonsterDefence;
import org.junit.Test;

public class MeleeAttackTypeTest
{
    // defenceLevel, stab, slash, crush, magic, range
    private final MonsterDefence def = new MonsterDefence(100, 10, 20, 30, 40, 50);
    // astab, aslash, acrush, amagic, arange, meleeStr, rangedStr, magicDmg%, aspeed
    private final Bonuses bonuses = new Bonuses(1, 2, 3, 0, 0, 0, 0, 0, 4);

    @Test
    public void stabReadsStabDefenceAndStabAttack()
    {
        assertEquals(10, MeleeAttackType.STAB.monsterDefence(def));
        assertEquals(1, MeleeAttackType.STAB.attackBonus(bonuses));
    }

    @Test
    public void slashReadsSlashDefenceAndSlashAttack()
    {
        assertEquals(20, MeleeAttackType.SLASH.monsterDefence(def));
        assertEquals(2, MeleeAttackType.SLASH.attackBonus(bonuses));
    }

    @Test
    public void crushReadsCrushDefenceAndCrushAttack()
    {
        assertEquals(30, MeleeAttackType.CRUSH.monsterDefence(def));
        assertEquals(3, MeleeAttackType.CRUSH.attackBonus(bonuses));
    }
}

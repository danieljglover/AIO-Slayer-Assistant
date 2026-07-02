package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StrategyMarkdownParserTest
{
    @Test
    public void parsesFrontmatterAndBody()
    {
        String md = "---\n"
            + "strategyId: tormented-demon\n"
            + "variantIds: [tormented-demon]\n"
            + "primaryStyle: MELEE\n"
            + "primaryWeapons: [emberlight, arclight]\n"
            + "secondaryWeapons:\n"
            + "  - weaponId: scorching-bow\n"
            + "    style: RANGED\n"
            + "note: Switch to Scorching bow for the shield-down window\n"
            + "sourceUrl: https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies\n"
            + "---\n"
            + "# Tormented Demon\n"
            + "Use demonbane melee and a ranged swap.\n";

        SourceStrategy strategy = StrategyMarkdownParser.parse("strategies/tormented-demon.md", md);

        assertEquals("tormented-demon", strategy.getStrategyId());
        assertEquals("tormented-demon", strategy.getVariantIds().get(0));
        assertEquals(CombatStyle.MELEE, strategy.getPrimaryStyle());
        assertEquals("emberlight", strategy.getPrimaryWeapons().get(0));
        assertEquals("scorching-bow", strategy.getSecondaryWeapons().get(0).getWeaponId());
        assertEquals(CombatStyle.RANGED, strategy.getSecondaryWeapons().get(0).getStyle());
        assertTrue(strategy.getBody().contains("demonbane melee"));
    }
}

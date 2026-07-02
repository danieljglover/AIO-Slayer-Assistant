package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.loadout.MonsterProfile;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterStrategy;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.StrategyWeapon;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * MV-B6: the monster-variant validation harness (plan section 8). It pins the FR-6 critical invariant -
 * existing tasks' recommended {@code weakness.style} must never silently flip from the cross-check
 * elemental-weakness data (decision #11 / ADR-0013) - and enforces the variant structural rules as the
 * MV-D* authoring lands: every task resolves to a non-null profile (or carries variants that each do),
 * exactly one {@code isDefault} per multi-variant task, and no reanimated monster is selectable
 * (ADR-0012.1/0012.4).
 */
public class MonsterVariantDatasetTest
{
    // The reviewed, curated recommended style per existing task. PRESERVED, not re-authored from the
    // wiki elemental icon (ADR-0013): Black/Blue/Red dragons stay RANGED, Metal dragons MELEE, etc.
    // The method selector lets the user OVERRIDE this; the stored style does not flip.
    private static final Map<String, CombatStyle> PINNED_STYLE = new HashMap<>();

    static
    {
        m("Abyssal demons", CombatStyle.MELEE);
        m("Smoke devils", CombatStyle.MAGIC);
        m("Aberrant spectres", CombatStyle.MAGIC);
        m("Ankou", CombatStyle.MELEE);
        m("Aquanites", CombatStyle.MELEE);
        m("Araxytes", CombatStyle.MELEE);
        m("Aviansie", CombatStyle.RANGED);
        m("Basilisks", CombatStyle.MELEE);
        m("Black demons", CombatStyle.MELEE);
        m("Black dragons", CombatStyle.RANGED);
        m("Bloodveld", CombatStyle.MELEE);
        m("Blue dragons", CombatStyle.RANGED);
        m("Cave horrors", CombatStyle.MELEE);
        m("Cave kraken", CombatStyle.MAGIC);
        m("Dagannoth", CombatStyle.MELEE);
        m("Dark beasts", CombatStyle.MELEE);
        m("Drakes", CombatStyle.RANGED);
        m("Dust devils", CombatStyle.MELEE);
        m("Elves", CombatStyle.MELEE);
        m("Fire giants", CombatStyle.MELEE);
        m("Fossil Island wyverns", CombatStyle.RANGED);
        m("Gargoyles", CombatStyle.MELEE);
        m("Greater demons", CombatStyle.MELEE);
        m("Gryphons", CombatStyle.RANGED);
        m("Hellhounds", CombatStyle.MELEE);
        m("Kalphite", CombatStyle.MELEE);
        m("Kurask", CombatStyle.MELEE);
        m("Lizardmen", CombatStyle.MELEE);
        m("Metal dragons", CombatStyle.MELEE);
        m("Mutated zygomites", CombatStyle.MELEE);
        m("Nechryael", CombatStyle.MELEE);
        m("Red dragons", CombatStyle.RANGED);
        m("Skeletal wyverns", CombatStyle.RANGED);
        m("Spiritual creatures", CombatStyle.MELEE);
        m("Suqahs", CombatStyle.MELEE);
        m("Trolls", CombatStyle.MELEE);
        m("TzHaar", CombatStyle.MELEE);
        m("Vampyres", CombatStyle.MELEE);
        m("Warped creatures", CombatStyle.MELEE);
        m("Waterfiends", CombatStyle.MELEE);
        m("Wyrms", CombatStyle.MELEE);
        m("Frost Dragons", CombatStyle.RANGED);
        // "Boss" is a meta-task whose recommended style lives per boss variant (ADR-0014), so it is not
        // pinned here.
    }

    private static void m(String task, CombatStyle style)
    {
        PINNED_STYLE.put(task, style);
    }

    private List<TaskData> duradel;
    private Collection<TaskData> all;

    @Before
    public void setUp()
    {
        SlayerDataService service = new SlayerDataService(new Gson());
        service.load();
        all = service.all();
        duradel = all.stream()
            .filter(t -> t.getAssignedBy() != null && t.getAssignedBy().contains("duradel"))
            .collect(Collectors.toList());
    }

    private MonsterVariant findVariant(String name)
    {
        for (TaskData t : all)
        {
            if (t.getVariants() == null)
            {
                continue;
            }
            for (MonsterVariant v : t.getVariants())
            {
                if (name.equals(v.getName()))
                {
                    return v;
                }
            }
        }
        return null;
    }

    @Test
    public void existingTaskStylesArePreservedNeverFlippedFromTheWikiElementalIcon()
    {
        // FR-6-critical guard (decision #11): the cross-check reports several tasks as MAGIC by their
        // wiki elemental weakness, but the recommended STYLE is a different axis and must not flip.
        for (TaskData t : duradel)
        {
            CombatStyle expected = PINNED_STYLE.get(t.getTask());
            if (expected == null)
            {
                continue; // Boss meta-task / not-yet-pinned new task
            }
            assertNotNull("recommended style present for " + t.getTask(), t.getWeakness());
            assertEquals("recommended style must be preserved for " + t.getTask(),
                expected, t.getWeakness().getStyle());
        }
    }

    @Test
    public void everyTaskResolvesToANonNullProfileOrCarriesResolvableVariants()
    {
        // FR-1/FR-6: a no-variant task uses its (non-null) task-level profile; a variant-bearing task
        // must have every variant resolve to a non-null weakness+defence (the inherit rule fills nulls
        // from the task default, ADR-0012.3). The Boss meta-task carries a null task-level profile but
        // every boss variant is self-contained (ADR-0014) - covered by the variants branch.
        for (TaskData t : duradel)
        {
            List<MonsterVariant> variants = t.getVariants();
            if (variants == null || variants.isEmpty())
            {
                assertNotNull(t.getTask() + " task-level weakness", t.getWeakness());
                assertNotNull(t.getTask() + " task-level style", t.getWeakness().getStyle());
                assertNotNull(t.getTask() + " task-level defence", t.getMonsterDefence());
                continue;
            }
            for (MonsterVariant v : variants)
            {
                MonsterProfile p = MonsterProfile.fromVariant(t, v);
                assertNotNull(t.getTask() + " / " + v.getName() + " resolved weakness", p.getWeakness());
                assertNotNull(t.getTask() + " / " + v.getName() + " resolved style", p.recommendedStyle());
                assertNotNull(t.getTask() + " / " + v.getName() + " resolved defence", p.getDefence());
            }
        }
    }

    @Test
    public void everyMultiVariantTaskHasExactlyOneDefault()
    {
        // OQ-2 / ADR-0014: exactly one isDefault per multi-variant task (the base monster, or for the
        // all-boss Boss task a deterministic first-listed fallback).
        for (TaskData t : duradel)
        {
            List<MonsterVariant> variants = t.getVariants();
            if (variants == null || variants.size() <= 1)
            {
                continue;
            }
            long defaults = variants.stream().filter(MonsterVariant::isDefault).count();
            assertEquals("exactly one isDefault variant for " + t.getTask(), 1L, defaults);
        }
    }

    @Test
    public void noReanimatedMonsterIsSelectable()
    {
        // ADR-0012.1/0012.4: reanimated monsters do not count toward tasks and are excluded.
        for (TaskData t : duradel)
        {
            if (t.getVariants() == null)
            {
                continue;
            }
            for (MonsterVariant v : t.getVariants())
            {
                assertFalse("reanimated variant must not be selectable: " + t.getTask() + " / "
                    + v.getName(), v.getName() != null && v.getName().toLowerCase().contains("reanimated"));
            }
        }
    }

    @Test
    public void everyVariantHasANonEmptyName()
    {
        for (TaskData t : duradel)
        {
            if (t.getVariants() == null)
            {
                continue;
            }
            for (MonsterVariant v : t.getVariants())
            {
                assertNotNull(t.getTask() + " variant name", v.getName());
                assertFalse(t.getTask() + " variant name empty", v.getName().trim().isEmpty());
            }
        }
    }

    // --- MV-S3: authored wiki /Strategies gear -------------------------------------------------

    @Test
    public void everyAuthoredStrategyIsStructurallyWellFormed()
    {
        // ADR-0015 / MV-S1: a present strategy must have a primaryStyle, a NON-EMPTY priority-ordered
        // primaryWeapons list, a valid (positive) itemId on every named weapon (never fabricated/zero),
        // every SECONDARY weapon tagged with its own style (the override gate key), and a sourceUrl.
        int authored = 0;
        for (TaskData t : all)
        {
            if (t.getVariants() == null)
            {
                continue;
            }
            for (MonsterVariant v : t.getVariants())
            {
                MonsterStrategy s = v.getStrategy();
                if (s == null)
                {
                    continue;
                }
                authored++;
                String where = t.getTask() + " / " + v.getName();
                assertNotNull(where + " primaryStyle", s.getPrimaryStyle());
                assertNotNull(where + " primaryWeapons", s.getPrimaryWeapons());
                assertFalse(where + " primaryWeapons empty", s.getPrimaryWeapons().isEmpty());
                assertNotNull(where + " sourceUrl", s.getSourceUrl());
                assertTrue(where + " sourceUrl is a wiki /Strategies (or main) page",
                    s.getSourceUrl().startsWith("https://oldschool.runescape.wiki/w/"));
                for (StrategyWeapon w : s.getPrimaryWeapons())
                {
                    assertNotNull(where + " primary weapon", w);
                    assertNotNull(where + " primary weapon name", w.getName());
                    assertFalse(where + " primary weapon name empty", w.getName().trim().isEmpty());
                    assertNotNull(where + " primary weapon id (" + w.getName() + ")", w.getItemId());
                    assertTrue(where + " primary weapon id positive (" + w.getName() + ")",
                        w.getItemId() > 0);
                }
                if (s.getSecondaryWeapons() != null)
                {
                    for (StrategyWeapon w : s.getSecondaryWeapons())
                    {
                        assertNotNull(where + " secondary weapon", w);
                        assertNotNull(where + " secondary weapon name", w.getName());
                        assertNotNull(where + " secondary weapon id (" + w.getName() + ")", w.getItemId());
                        assertTrue(where + " secondary weapon id positive (" + w.getName() + ")",
                            w.getItemId() > 0);
                        assertNotNull(where + " secondary weapon style (" + w.getName() + ")",
                            w.getStyle());
                    }
                }
            }
        }
        assertTrue("MV-S3 authored a substantial number of strategies (got " + authored + ")",
            authored >= 60);
    }

    @Test
    public void questBossAndSummonedVariantsInheritTaskFamilyStrategy()
    {
        // These variants do not need dedicated boss strategy pages, but they are assigned task variants and
        // should inherit the family strategy that explains quest-boss handling or why the summon is ignored.
        for (String name : Arrays.asList("Dad", "Ice Troll King", "Arrg"))
        {
            MonsterVariant v = findVariant(name);
            assertNotNull("expected the variant to exist: " + name, v);
            assertNotNull(name + " should inherit Trolls strategy", v.getStrategy());
            assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Trolls", v.getStrategy().getSourceUrl());
        }

        MonsterVariant darkAnkou = findVariant("Dark Ankou");
        assertNotNull("expected the variant to exist: Dark Ankou", darkAnkou);
        assertNotNull("Dark Ankou should inherit Ankou strategy", darkAnkou.getStrategy());
        assertEquals("https://oldschool.runescape.wiki/w/Slayer_task/Ankous", darkAnkou.getStrategy().getSourceUrl());
    }

    @Test
    public void tormentedDemonStrategyMatchesTheWikiGuide()
    {
        // The headline user case: the real Tormented Demon variant carries a demonbane-melee strategy
        // with Emberlight as the top primary and a Scorching bow ranged secondary (Tormented_Demon/Strategies).
        MonsterVariant td = findVariant("Tormented Demon");
        assertNotNull("Tormented Demon variant present", td);
        MonsterStrategy s = td.getStrategy();
        assertNotNull("Tormented Demon has an authored strategy", s);
        assertEquals(CombatStyle.MELEE, s.getPrimaryStyle());
        assertEquals("Emberlight leads the melee priority", "Emberlight",
            s.getPrimaryWeapons().get(0).getName());
        assertEquals("Emberlight item id", Integer.valueOf(29589),
            s.getPrimaryWeapons().get(0).getItemId());
        assertTrue("Osmumten's fang is also a documented primary (the fallback)",
            s.getPrimaryWeapons().stream().anyMatch(w -> "Osmumten's fang".equals(w.getName())));
        StrategyWeapon scorch = s.getSecondaryWeapons().stream()
            .filter(w -> "Scorching bow".equals(w.getName())).findFirst().orElse(null);
        assertNotNull("Scorching bow ranged secondary present", scorch);
        assertEquals(CombatStyle.RANGED, scorch.getStyle());
        assertEquals("Scorching bow item id", Integer.valueOf(29591), scorch.getItemId());
    }

    @Test
    public void styleMismatchStrategiesKeepTheirAuthoredWeaknessButDocumentTheGuideStyle()
    {
        // ADR-0013 + ADR-0015: the authored weakness.style is NOT mutated; the guide style lives on the
        // strategy. Abyssal Sire (Boss-pool, weakness MAGIC) carries a MELEE strategy; Vorkath (Boss-pool,
        // weakness MELEE) carries a RANGED strategy. The advisor derives the default method from the
        // strategy (covered in LoadoutAdvisorTest) - here we pin the DATA shape.
        MonsterVariant sire = findVariant("Abyssal Sire");
        assertNotNull(sire);
        assertNotNull(sire.getStrategy());
        assertEquals("Abyssal Sire guide style is MELEE", CombatStyle.MELEE,
            sire.getStrategy().getPrimaryStyle());

        MonsterVariant vorkath = findVariant("Vorkath");
        assertNotNull(vorkath);
        assertNotNull(vorkath.getStrategy());
        assertEquals("Vorkath guide style is RANGED", CombatStyle.RANGED,
            vorkath.getStrategy().getPrimaryStyle());
    }
}

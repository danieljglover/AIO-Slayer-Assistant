package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.model.TaskUnlock;
import com.danieljglover.allinslayer.model.UnlockType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModularSlayerDataCompilationTest
{
    @Test
    public void compilesRepresentativeTaskWithVariantStrategyAndWeaponReferences() throws IOException
    {
        Path root = Files.createTempDirectory("modular-slayer-slice");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("locations/catacombs-of-kourend.json"),
            "{\"locationId\":\"catacombs-of-kourend\",\"name\":\"Catacombs of Kourend\","
                + "\"multi\":true,\"cannon\":false,\"burst\":false,\"konarLockable\":true}");
        write(root.resolve("weapons/demonbane.json"),
            "{\"weaponId\":\"emberlight\",\"name\":\"Emberlight\",\"itemIds\":[29589]}");
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\",\"slayerTargetId\":222,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],\"amountByMaster\":{\"duradel\":[130,200]},"
                + "\"monsterIds\":[\"greater-demons\"],\"variantIds\":[\"greater-demon\",\"tormented-demon\"],"
                + "\"defaultVariantId\":\"greater-demon\",\"locationIds\":[\"catacombs-of-kourend\"],"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":70,\"stab\":30,\"slash\":30,\"crush\":30,\"magic\":40,\"range\":30},"
                + "\"slayerHelmApplies\":true,\"demon\":true,"
                + "\"recommendedMethod\":\"Melee the Catacombs of Kourend\"}");
        write(root.resolve("monsters/greater-demons.json"),
            "{\"monsterId\":\"greater-demons\",\"name\":\"Greater demons\",\"variants\":["
                + "{\"variantId\":\"greater-demon\",\"name\":\"Greater demon\",\"npcIds\":[2025],"
                + "\"combatLevel\":92,\"demon\":true,\"location\":\"Catacombs of Kourend\",\"requirement\":\"none\","
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":70,\"stab\":30,\"slash\":30,\"crush\":30,\"magic\":40,\"range\":30}},"
                + "{\"variantId\":\"tormented-demon\",\"name\":\"Tormented Demon\",\"npcIds\":[13599],"
                + "\"combatLevel\":450,\"demon\":true,\"boss\":false,\"strategyId\":\"tormented-demon\","
                + "\"location\":\"Ancient Guthixian Temple\",\"requirement\":\"While Guthix Sleeps\","
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":\"water\"},"
                + "\"monsterDefence\":{\"defenceLevel\":150,\"stab\":75,\"slash\":175,\"crush\":68,\"magic\":5,\"range\":150}}]}");
        write(root.resolve("strategies/tormented-demon.md"),
            "---\n"
                + "strategyId: tormented-demon\n"
                + "variantIds: [tormented-demon]\n"
                + "primaryStyle: MELEE\n"
                + "primaryWeapons: [emberlight]\n"
                + "note: Demonbane melee is core.\n"
                + "sourceUrl: https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies\n"
                + "---\n"
                + "# Tormented Demon\n"
                + "Use demonbane melee.\n");

        List<TaskData> tasks = ModularSlayerDataCompiler.compile(root);

        assertEquals(1, tasks.size());
        TaskData task = tasks.get(0);
        assertEquals("Greater demons", task.getTask());
        assertEquals("duradel", task.getAssignedBy().get(0));
        assertEquals(2, task.getVariants().size());

        MonsterVariant tormented = task.getVariants().stream()
            .filter(v -> "Tormented Demon".equals(v.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Tormented Demon variant"));
        assertNotNull(tormented.getStrategy());
        assertEquals(Integer.valueOf(29589),
            tormented.getStrategy().getPrimaryWeapons().get(0).getItemId());
    }

    @Test
    public void emitsRequiredItemIdAndNameFromTaskSource() throws IOException
    {
        Path root = Files.createTempDirectory("modular-slayer-required-item");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("tasks/gargoyles.json"),
            "{\"taskId\":\"gargoyles\",\"name\":\"Gargoyles\",\"slayerTargetId\":74,"
                + "\"slayerLevel\":75,\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"gargoyle\"],\"defaultVariantId\":\"gargoyle\","
                + "\"requiredItemId\":4162,\"requiredItemName\":\"Rock hammer\","
                + "\"slayerHelmApplies\":true}");
        write(root.resolve("monsters/gargoyles/gargoyle-lvl111.json"),
            "{\"variantId\":\"gargoyle\",\"name\":\"Gargoyle\",\"npcIds\":[1543],\"combatLevel\":111,"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":80,\"stab\":40,\"slash\":40,\"crush\":40,\"magic\":50,\"range\":40}}");

        List<TaskData> tasks = ModularSlayerDataCompiler.compile(root);

        assertEquals(1, tasks.size());
        assertEquals(Integer.valueOf(4162), tasks.get(0).getRequiredItemId());
        assertEquals("Rock hammer", tasks.get(0).getRequiredItemName());
    }

    @Test
    public void compilesMonsterVariantsFromFamilyDirectoryFiles() throws IOException
    {
        Path root = Files.createTempDirectory("modular-slayer-variant-files");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\",\"slayerTargetId\":222,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"greater-demon\",\"tormented-demon\"],"
                + "\"defaultVariantId\":\"greater-demon\",\"slayerHelmApplies\":true,\"demon\":true}");
        write(root.resolve("monsters/greater-demons/greater-demon-lvl92.json"),
            "{\"variantId\":\"greater-demon\",\"name\":\"Greater demon\",\"npcIds\":[2025],"
                + "\"combatLevel\":92,\"demon\":true,"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":70,\"stab\":30,\"slash\":30,\"crush\":30,\"magic\":40,\"range\":30}}");
        write(root.resolve("monsters/greater-demons/tormented-demon-lvl450.json"),
            "{\"variantId\":\"tormented-demon\",\"name\":\"Tormented Demon\",\"npcIds\":[13599],"
                + "\"combatLevel\":450,\"demon\":true,"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":\"water\"},"
                + "\"monsterDefence\":{\"defenceLevel\":150,\"stab\":75,\"slash\":175,\"crush\":68,\"magic\":5,\"range\":150}}");

        List<TaskData> tasks = ModularSlayerDataCompiler.compile(root);

        assertEquals(1, tasks.size());
        assertEquals(2, tasks.get(0).getVariants().size());
        assertEquals("Greater demon", tasks.get(0).getVariants().get(0).getName());
        assertEquals("Tormented Demon", tasks.get(0).getVariants().get(1).getName());
    }

    @Test
    public void compilesStrategyFromJsonSourceWithPluginFields() throws IOException
    {
        Path root = Files.createTempDirectory("modular-slayer-json-strategy");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("weapons/emberlight.json"),
            "{\"weaponId\":\"emberlight\",\"name\":\"Emberlight\",\"itemIds\":[29589]}");
        write(root.resolve("weapons/scorching-bow.json"),
            "{\"weaponId\":\"scorching-bow\",\"name\":\"Scorching bow\",\"itemIds\":[29591]}");
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\",\"slayerTargetId\":222,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"greater-demons-k-ril-tsutsaroth\"],"
                + "\"defaultVariantId\":\"greater-demons-k-ril-tsutsaroth\",\"slayerHelmApplies\":true,"
                + "\"demon\":true}");
        write(root.resolve("monsters/greater-demons/k-ril-lvl650.json"),
            "{\"variantId\":\"greater-demons-k-ril-tsutsaroth\",\"name\":\"K'ril Tsutsaroth\","
                + "\"npcIds\":[3129],\"combatLevel\":650,\"demon\":true,\"boss\":true,"
                + "\"strategyId\":\"k-ril-tsutsaroth\","
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":\"water\"},"
                + "\"monsterDefence\":{\"defenceLevel\":270,\"stab\":70,\"slash\":80,\"crush\":80,"
                + "\"magic\":80,\"range\":80}}");
        write(root.resolve("strategies/k-ril-tsutsaroth/strategy.json"),
            "{\"strategyId\":\"k-ril-tsutsaroth\","
                + "\"variantIds\":[\"greater-demons-k-ril-tsutsaroth\"],"
                + "\"sourceUrl\":\"https://oldschool.runescape.wiki/w/K'ril_Tsutsaroth/Strategies\","
                + "\"plugin\":{\"primaryStyle\":\"MELEE\",\"primaryWeapons\":[\"emberlight\"],"
                + "\"secondaryWeapons\":[{\"weaponId\":\"scorching-bow\",\"style\":\"RANGED\"}],"
                + "\"note\":\"Scorching bow bind method is a major solo option.\"},"
                + "\"methods\":[{\"methodId\":\"solo-scorching-bow\",\"label\":\"Solo Scorching bow\","
                + "\"combatStyle\":\"RANGED\",\"steps\":[\"Bind K'ril with Scorching bow.\"]}],"
                + "\"styleOptions\":[{\"styleId\":\"ranged\",\"label\":\"Ranged\",\"combatStyle\":\"RANGED\"}]}");

        List<TaskData> tasks = ModularSlayerDataCompiler.compile(root);

        MonsterVariant kril = tasks.get(0).getVariants().get(0);
        assertNotNull(kril.getStrategy());
        assertEquals(Integer.valueOf(29589), kril.getStrategy().getPrimaryWeapons().get(0).getItemId());
        assertEquals(Integer.valueOf(29591), kril.getStrategy().getSecondaryWeapons().get(0).getItemId());
    }

    @Test
    public void emitsUnlocksExtendedAmountAndWeightByMasterOntoTaskData() throws IOException
    {
        // WA-7 (ADR-0018 #3/#4/#5): the three additive assignment fields reach runtime. A task
        // without them (or with an EMPTY unlocks list, the authored-nothing case) compiles them
        // null so its serialised form carries no new keys (FR-6 sentinel, the DT-B5 discipline).
        Path root = Files.createTempDirectory("modular-slayer-unlocks");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("tasks/abyssal-demons.json"),
            "{\"taskId\":\"abyssal-demons\",\"name\":\"Abyssal demons\",\"slayerTargetId\":98,"
                + "\"slayerLevel\":85,\"masterIds\":[\"duradel\"],"
                + "\"amountByMaster\":{\"duradel\":[130,200]},"
                + "\"extendedAmount\":{\"duradel\":[200,250]},"
                + "\"weightByMaster\":{\"duradel\":12},"
                + "\"unlocks\":[{\"unlockId\":\"augment-my-abbies\",\"name\":\"Augment my abbies\","
                + "\"pointsCost\":100,\"type\":\"EXTENSION\",\"notes\":\"Extends to 200-250.\"},"
                + "{\"unlockId\":\"bigger-and-badder\",\"name\":\"Bigger and Badder\",\"pointsCost\":50}],"
                + "\"variantIds\":[\"abyssal-demon\"],\"defaultVariantId\":\"abyssal-demon\","
                + "\"slayerHelmApplies\":true,\"demon\":true}");
        write(root.resolve("tasks/hellhounds.json"),
            "{\"taskId\":\"hellhounds\",\"name\":\"Hellhounds\",\"slayerTargetId\":31,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],\"unlocks\":[],"
                + "\"variantIds\":[\"hellhound\"],\"defaultVariantId\":\"hellhound\","
                + "\"slayerHelmApplies\":true}");
        write(root.resolve("monsters/abyssal-demons/abyssal-demon-lvl124.json"),
            "{\"variantId\":\"abyssal-demon\",\"name\":\"Abyssal demon\",\"npcIds\":[415],"
                + "\"combatLevel\":124,\"demon\":true,"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":135,\"stab\":20,\"slash\":20,\"crush\":20,"
                + "\"magic\":0,\"range\":20}}");
        write(root.resolve("monsters/hellhounds/hellhound-lvl122.json"),
            "{\"variantId\":\"hellhound\",\"name\":\"Hellhound\",\"npcIds\":[104],"
                + "\"combatLevel\":122,"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":100,\"stab\":25,\"slash\":25,\"crush\":25,"
                + "\"magic\":50,\"range\":25}}");

        List<TaskData> tasks = ModularSlayerDataCompiler.compile(root);

        TaskData abyssal = tasks.stream().filter(t -> "Abyssal demons".equals(t.getTask()))
            .findFirst().orElseThrow(() -> new AssertionError("missing Abyssal demons"));
        assertArrayEquals(new int[]{200, 250}, abyssal.getExtendedAmount().get("duradel"));
        assertEquals(Integer.valueOf(12), abyssal.getWeightByMaster().get("duradel"));
        assertEquals(2, abyssal.getUnlocks().size());
        TaskUnlock extension = abyssal.getUnlocks().get(0);
        assertEquals("augment-my-abbies", extension.getUnlockId());
        assertEquals("Augment my abbies", extension.getName());
        assertEquals(Integer.valueOf(100), extension.getPointsCost());
        assertEquals(UnlockType.EXTENSION, extension.getType());
        assertTrue(extension.getNotes().contains("200-250"));
        assertNull("untyped source row stays untyped", abyssal.getUnlocks().get(1).getType());

        TaskData hellhounds = tasks.stream().filter(t -> "Hellhounds".equals(t.getTask()))
            .findFirst().orElseThrow(() -> new AssertionError("missing Hellhounds"));
        assertNull("empty unlocks list compiles to null (no new key emitted)",
            hellhounds.getUnlocks());
        assertNull(hellhounds.getExtendedAmount());
        assertNull(hellhounds.getWeightByMaster());
    }

    @Test
    public void validateRejectsAnExtendedAmountWithoutACostedExtensionUnlock() throws IOException
    {
        // WA-7 (ADR-0018 #5): the build-time cross-check - extendedAmount needs exactly one
        // EXTENSION-typed unlock, and that unlock's pointsCost must be non-null. Catches the
        // 4-null-costs class of bug and the "extension without its enabling unlock" latent gap.
        Path missing = Files.createTempDirectory("modular-slayer-ext-missing");
        writeExtensionFixture(missing,
            "[{\"unlockId\":\"bigger-and-badder\",\"pointsCost\":50}]");
        assertValidationFails(missing, "0 EXTENSION");

        Path nullCost = Files.createTempDirectory("modular-slayer-ext-nullcost");
        writeExtensionFixture(nullCost,
            "[{\"unlockId\":\"bleed-me-dry\",\"type\":\"EXTENSION\"}]");
        assertValidationFails(nullCost, "null pointsCost");
    }

    private static void writeExtensionFixture(Path root, String unlocksJson) throws IOException
    {
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("tasks/bloodveld.json"),
            "{\"taskId\":\"bloodveld\",\"name\":\"Bloodveld\",\"slayerTargetId\":76,"
                + "\"slayerLevel\":50,\"masterIds\":[\"duradel\"],"
                + "\"extendedAmount\":{\"duradel\":[200,250]},"
                + "\"unlocks\":" + unlocksJson + ","
                + "\"variantIds\":[\"bloodveld\"],\"defaultVariantId\":\"bloodveld\","
                + "\"slayerHelmApplies\":true}");
        write(root.resolve("monsters/bloodveld/bloodveld-lvl76.json"),
            "{\"variantId\":\"bloodveld\",\"name\":\"Bloodveld\",\"npcIds\":[484],"
                + "\"combatLevel\":76,"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":18,\"stab\":0,\"slash\":0,\"crush\":0,"
                + "\"magic\":0,\"range\":15}}");
    }

    private static void assertValidationFails(Path root, String expectedFragment)
    {
        try
        {
            ModularSlayerDataCompiler.compile(root);
            fail("expected SlayerDataValidationException containing: " + expectedFragment);
        }
        catch (SlayerDataValidationException ex)
        {
            assertTrue("expected an error mentioning bloodveld + '" + expectedFragment
                    + "' but got: " + ex.getErrors(),
                ex.getErrors().stream().anyMatch(e -> e.contains("bloodveld")
                    && e.contains(expectedFragment)));
        }
    }

    private static void write(Path path, String content) throws IOException
    {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}

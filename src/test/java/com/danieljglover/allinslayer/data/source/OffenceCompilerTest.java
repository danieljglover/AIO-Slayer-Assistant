package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.loadout.MonsterProfile;
import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * WD-2 (ADR-0020 #1): the compiler carries {@code offence} from the source variant onto
 * {@link MonsterVariant} and from the source task onto {@link TaskData}, and
 * {@link MonsterProfile#fromVariant} overlays the variant with a task-level fallback. Absent
 * everywhere -> null (FR-6).
 */
public class OffenceCompilerTest
{
    @Test
    public void compilesVariantAndTaskOffenceWithFallbackAndAbsentNull() throws IOException
    {
        Path root = Files.createTempDirectory("offence-compile");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("locations/catacombs-of-kourend.json"),
            "{\"locationId\":\"catacombs-of-kourend\",\"name\":\"Catacombs of Kourend\","
                + "\"multi\":true,\"cannon\":false,\"burst\":false,\"konarLockable\":true}");
        // Task carries a task-level offence default; one variant overrides it, one omits it (fallback).
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\",\"slayerTargetId\":222,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],\"amountByMaster\":{\"duradel\":[130,200]},"
                + "\"monsterIds\":[\"greater-demons\"],\"variantIds\":[\"greater-demon\",\"tormented-demon\"],"
                + "\"defaultVariantId\":\"greater-demon\",\"locationIds\":[\"catacombs-of-kourend\"],"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"slayerHelmApplies\":true,\"demon\":true,"
                + "\"offence\":{\"hitpoints\":87,\"maxHit\":15,\"attackStyles\":[\"MELEE\"],\"poisonous\":true}}");
        write(root.resolve("monsters/greater-demons.json"),
            "{\"monsterId\":\"greater-demons\",\"name\":\"Greater demons\",\"variants\":["
                + "{\"variantId\":\"greater-demon\",\"name\":\"Greater demon\",\"npcIds\":[2025],"
                + "\"demon\":true,\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"offence\":{\"hitpoints\":87,\"maxHit\":15,\"attackStyles\":[\"MELEE\"],"
                + "\"attackSpeedTicks\":4,\"magicLevel\":80}},"
                + "{\"variantId\":\"tormented-demon\",\"name\":\"Tormented Demon\",\"npcIds\":[13599],"
                + "\"demon\":true,\"weakness\":{\"style\":\"MELEE\",\"element\":\"water\"}}]}");

        List<TaskData> tasks = ModularSlayerDataCompiler.compile(root);
        assertEquals(1, tasks.size());
        TaskData task = tasks.get(0);

        // Task-level offence reached runtime.
        assertEquals(Integer.valueOf(87), task.getOffence().getHitpoints());
        assertEquals(AttackStyle.MELEE, task.getOffence().getAttackStyles().get(0));

        MonsterVariant greater = task.getVariants().get(0);
        MonsterVariant tormented = task.getVariants().get(1);

        // The variant with its own offence overlays it (magicLevel is variant-only).
        assertEquals(Integer.valueOf(80), greater.getOffence().getMagicLevel());
        assertEquals(Integer.valueOf(4), greater.getOffence().getAttackSpeedTicks());
        MonsterProfile pg = MonsterProfile.fromVariant(task, greater);
        assertEquals(Integer.valueOf(87), pg.getOffence().getHitpoints());

        // The variant WITHOUT offence has null on the variant, but the profile falls back to the task.
        assertNull("variant omits its own offence", tormented.getOffence());
        MonsterProfile pt = MonsterProfile.fromVariant(task, tormented);
        assertEquals("profile inherits task offence", Integer.valueOf(15), pt.getOffence().getMaxHit());
    }

    @Test
    public void offenceAbsentEverywhereCompilesToNull() throws IOException
    {
        Path root = Files.createTempDirectory("offence-absent");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("locations/catacombs-of-kourend.json"),
            "{\"locationId\":\"catacombs-of-kourend\",\"name\":\"Catacombs of Kourend\","
                + "\"multi\":true,\"cannon\":false,\"burst\":false,\"konarLockable\":true}");
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\",\"slayerTargetId\":222,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],\"amountByMaster\":{\"duradel\":[130,200]},"
                + "\"monsterIds\":[\"greater-demons\"],\"variantIds\":[\"greater-demon\"],"
                + "\"defaultVariantId\":\"greater-demon\",\"locationIds\":[\"catacombs-of-kourend\"],"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},\"slayerHelmApplies\":true}");
        write(root.resolve("monsters/greater-demons.json"),
            "{\"monsterId\":\"greater-demons\",\"name\":\"Greater demons\",\"variants\":["
                + "{\"variantId\":\"greater-demon\",\"name\":\"Greater demon\",\"npcIds\":[2025],"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null}}]}");

        TaskData task = ModularSlayerDataCompiler.compile(root).get(0);
        assertNull(task.getOffence());
        assertNull(task.getVariants().get(0).getOffence());
        assertNull(MonsterProfile.fromVariant(task, task.getVariants().get(0)).getOffence());
    }

    private static void write(Path path, String content) throws IOException
    {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}

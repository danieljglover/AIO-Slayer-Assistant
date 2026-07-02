package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.LocationQuality;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.TaskData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WD-5a (ADR-0020 #2): the compiler compiles the previously validate-only
 * {@code locationComparison[]} into a runtime {@link LocationQuality} overlay on the matching
 * {@link SlayerLocation}. A location with no comparison row carries a null overlay (FR-6).
 */
public class LocationQualityCompilerTest
{
    @Test
    public void compilesComparisonRowsIntoTheLocationQualityOverlay() throws IOException
    {
        Path root = Files.createTempDirectory("location-quality");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("locations/catacombs-of-kourend.json"),
            "{\"locationId\":\"catacombs-of-kourend\",\"name\":\"Catacombs of Kourend\","
                + "\"multi\":true,\"cannon\":false,\"burst\":true,\"konarLockable\":true}");
        write(root.resolve("locations/slayer-tower.json"),
            "{\"locationId\":\"slayer-tower\",\"name\":\"Slayer Tower\","
                + "\"multi\":false,\"cannon\":false,\"burst\":false,\"konarLockable\":false}");
        // Only catacombs carries a comparison row; slayer-tower has none (null overlay, FR-6).
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\",\"slayerTargetId\":222,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],\"amountByMaster\":{\"duradel\":[130,200]},"
                + "\"monsterIds\":[\"greater-demons\"],\"variantIds\":[\"greater-demon\"],"
                + "\"defaultVariantId\":\"greater-demon\","
                + "\"locationIds\":[\"catacombs-of-kourend\",\"slayer-tower\"],"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},\"slayerHelmApplies\":true,"
                + "\"locationComparison\":[{\"locationId\":\"catacombs-of-kourend\","
                + "\"name\":\"Catacombs of Kourend\",\"amount\":450,\"multicombat\":true,"
                + "\"cannonable\":false,\"safespottable\":true,\"notes\":[\"burst spot\"]}]}");
        write(root.resolve("monsters/greater-demons.json"),
            "{\"monsterId\":\"greater-demons\",\"name\":\"Greater demons\",\"variants\":["
                + "{\"variantId\":\"greater-demon\",\"name\":\"Greater demon\",\"npcIds\":[2025],"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null}}]}");

        List<TaskData> tasks = ModularSlayerDataCompiler.compile(root);
        TaskData task = tasks.get(0);
        SlayerLocation catacombs = task.getLocations().get(0);
        SlayerLocation tower = task.getLocations().get(1);

        LocationQuality q = catacombs.getQuality();
        assertEquals(Integer.valueOf(450), q.getAmount());
        assertTrue(q.getMulticombat());
        assertEquals(Boolean.FALSE, q.getCannonable());
        assertTrue(q.getSafespottable());
        assertEquals("burst spot", q.getNotes().get(0));

        assertNull("no comparison row -> null overlay (FR-6)", tower.getQuality());
    }

    @Test
    public void noComparisonMeansNullOverlayEverywhere() throws IOException
    {
        Path root = Files.createTempDirectory("location-quality-absent");
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
        assertNull(task.getLocations().get(0).getQuality());
    }

    private static void write(Path path, String content) throws IOException
    {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}

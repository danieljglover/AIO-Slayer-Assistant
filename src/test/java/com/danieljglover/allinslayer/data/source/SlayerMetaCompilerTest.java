package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.MasterData;
import com.danieljglover.allinslayer.model.RewardData;
import com.danieljglover.allinslayer.model.RewardEffect;
import com.danieljglover.allinslayer.model.SlayerMeta;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * WA-8 (ADR-0018 #1/#6): the compiler builds the SECOND generated resource's content -
 * {@code slayer-meta.json = {masters, rewards}} - from {@code masters/*.json} +
 * {@code rewards/*.json}, deterministically ordered, with the source's STRING effect mapped
 * loudly to {@link RewardEffect} (a typo must fail the build, not Gson-null through).
 */
public class SlayerMetaCompilerTest
{
    @Test
    public void compilesMastersAndRewardsIntoMetaDeterministicallyOrdered() throws IOException
    {
        Path root = Files.createTempDirectory("slayer-meta-compile");
        write(root.resolve("masters/turael.json"),
            "{\"masterId\":\"turael\",\"name\":\"Turael\",\"aliases\":[\"Aya\"],"
                + "\"location\":\"Burthorpe\","
                + "\"economy\":{\"basePoints\":0,\"zeroPoints\":true,\"streakResets\":true}}");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\",\"aliases\":[\"Kuradal\"],"
                + "\"location\":\"Shilo Village\","
                + "\"requirements\":{\"combatLevel\":100,\"slayerLevel\":50,"
                + "\"quests\":[\"Shilo Village\"]},"
                + "\"economy\":{\"basePoints\":15,"
                + "\"streakMultipliers\":{\"10\":5,\"50\":15,\"100\":25,\"250\":35,\"1000\":50},"
                + "\"blockCost\":100}}");
        write(root.resolve("masters/nieve.json"),
            "{\"masterId\":\"nieve\",\"name\":\"Nieve\"}");
        write(root.resolve("rewards/malevolent-masquerade.json"),
            "{\"rewardId\":\"malevolent-masquerade\",\"name\":\"Malevolent masquerade\","
                + "\"pointsCost\":400,\"effect\":\"GEAR_UNLOCK\","
                + "\"notes\":\"Learn to craft a Slayer helmet.\"}");
        write(root.resolve("rewards/bigger-and-badder.json"),
            "{\"rewardId\":\"bigger-and-badder\",\"name\":\"Bigger and Badder\","
                + "\"pointsCost\":50,\"effect\":\"SUPERIOR\"}");

        SlayerMeta meta = ModularSlayerDataCompiler.compileMeta(root);

        // Masters sorted by masterId (deterministic generation), straight-copied enrichment.
        assertEquals(3, meta.getMasters().size());
        MasterData duradel = meta.getMasters().get(0);
        assertEquals("duradel", duradel.getMasterId());
        assertEquals("Kuradal", duradel.getAliases().get(0));
        assertEquals("Shilo Village", duradel.getLocation());
        assertEquals(Integer.valueOf(100), duradel.getRequirements().getCombatLevel());
        assertEquals(Integer.valueOf(15), duradel.getEconomy().getBasePoints());
        assertEquals(Integer.valueOf(5), duradel.getEconomy().getStreakMultipliers().get("10"));
        assertEquals(Integer.valueOf(100), duradel.getEconomy().getBlockCost());

        MasterData nieve = meta.getMasters().get(1);
        assertEquals("nieve", nieve.getMasterId());
        assertNull("a stub master compiles identity-only", nieve.getEconomy());
        assertNull(nieve.getRequirements());

        MasterData turael = meta.getMasters().get(2);
        assertTrue(turael.getEconomy().isZeroPoints());
        assertTrue(turael.getEconomy().isStreakResets());

        // Rewards sorted by rewardId, effect string -> enum.
        assertEquals(2, meta.getRewards().size());
        RewardData bab = meta.getRewards().get(0);
        assertEquals("bigger-and-badder", bab.getRewardId());
        assertEquals(50, bab.getPointsCost());
        assertEquals(RewardEffect.SUPERIOR, bab.getEffect());
        RewardData helm = meta.getRewards().get(1);
        assertEquals("malevolent-masquerade", helm.getRewardId());
        assertEquals(400, helm.getPointsCost());
        assertEquals(RewardEffect.GEAR_UNLOCK, helm.getEffect());
    }

    @Test
    public void validateRejectsBadRewardRows() throws IOException
    {
        // A typo'd effect string must FAIL the build (the WA-3 contract's reason for keeping the
        // source effect a String), as must a missing cost (RewardData.pointsCost is a primitive)
        // and a duplicate id.
        Path unknownEffect = Files.createTempDirectory("slayer-meta-bad-effect");
        write(unknownEffect.resolve("rewards/broader-fletching.json"),
            "{\"rewardId\":\"broader-fletching\",\"name\":\"Broader Fletching\","
                + "\"pointsCost\":300,\"effect\":\"GEAR_UNLOCKED\"}");
        assertValidationFails(unknownEffect, "broader-fletching", "GEAR_UNLOCKED");

        Path nullCost = Files.createTempDirectory("slayer-meta-null-cost");
        write(nullCost.resolve("rewards/ring-bling.json"),
            "{\"rewardId\":\"ring-bling\",\"name\":\"Ring bling\",\"effect\":\"GEAR_UNLOCK\"}");
        assertValidationFails(nullCost, "ring-bling", "pointsCost");

        Path duplicate = Files.createTempDirectory("slayer-meta-dup");
        write(duplicate.resolve("rewards/a.json"),
            "{\"rewardId\":\"like-a-boss\",\"name\":\"Like a Boss\",\"pointsCost\":200,"
                + "\"effect\":\"TASK_UNLOCK\"}");
        write(duplicate.resolve("rewards/b.json"),
            "{\"rewardId\":\"like-a-boss\",\"name\":\"Like a Boss\",\"pointsCost\":200,"
                + "\"effect\":\"TASK_UNLOCK\"}");
        assertValidationFails(duplicate, "duplicate rewardId", "like-a-boss");
    }

    @Test
    public void taskCompilationAlsoRejectsBadRewardRows() throws IOException
    {
        // validate() is shared by both entry points, so a broken rewards/ file breaks the whole
        // strict build - not just the meta half (ADR-0016 discipline).
        Path root = Files.createTempDirectory("slayer-meta-shared-validate");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("rewards/bad.json"),
            "{\"rewardId\":\"bad-reward\",\"name\":\"Bad\",\"pointsCost\":10,\"effect\":\"NOPE\"}");
        try
        {
            ModularSlayerDataCompiler.compile(root);
            fail("expected the task compile to reject the bad reward row too");
        }
        catch (SlayerDataValidationException ex)
        {
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("bad-reward")));
        }
    }

    private static void assertValidationFails(Path root, String fragmentA, String fragmentB)
    {
        try
        {
            ModularSlayerDataCompiler.compileMeta(root);
            fail("expected SlayerDataValidationException containing '" + fragmentA + "' + '"
                + fragmentB + "'");
        }
        catch (SlayerDataValidationException ex)
        {
            assertTrue("expected an error with '" + fragmentA + "' + '" + fragmentB
                    + "' but got: " + ex.getErrors(),
                ex.getErrors().stream().anyMatch(e -> e.contains(fragmentA)
                    && e.contains(fragmentB)));
        }
    }

    private static void write(Path path, String content) throws IOException
    {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}

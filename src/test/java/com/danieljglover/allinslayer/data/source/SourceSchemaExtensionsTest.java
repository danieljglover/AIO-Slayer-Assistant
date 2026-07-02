package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.UnlockType;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WA-3 (ADR-0018 #2/#3/#5/#6): the source schema grows additively - master identity/economy,
 * per-task assignment weights, typed unlocks, and a new rewards domain. Every extension is
 * Gson-default-null so the existing source files parse unchanged (FR-6).
 */
public class SourceSchemaExtensionsTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void masterLocationRequirementsAndEconomyDeserialize()
    {
        String json = "{\"masterId\":\"duradel\",\"name\":\"Duradel\",\"aliases\":[\"Kuradal\"],"
            + "\"location\":\"Shilo Village\","
            + "\"requirements\":{\"combatLevel\":100,\"slayerLevel\":50,\"quests\":[\"Shilo Village\"]},"
            + "\"economy\":{\"basePoints\":15,"
            + "\"streakMultipliers\":{\"10\":5,\"50\":15,\"100\":25,\"250\":35,\"1000\":50},"
            + "\"blockCost\":100,\"zeroPoints\":false,\"streakResets\":false}}";

        SourceMaster master = GSON.fromJson(json, SourceMaster.class);

        assertEquals("Shilo Village", master.getLocation());
        assertEquals(Integer.valueOf(100), master.getRequirements().getCombatLevel());
        assertEquals(Integer.valueOf(50), master.getRequirements().getSlayerLevel());
        assertEquals("Shilo Village", master.getRequirements().getQuests().get(0));
        assertEquals(Integer.valueOf(15), master.getEconomy().getBasePoints());
        assertEquals(Integer.valueOf(5), master.getEconomy().getStreakMultipliers().get("10"));
        assertEquals(Integer.valueOf(50), master.getEconomy().getStreakMultipliers().get("1000"));
        assertEquals(Integer.valueOf(100), master.getEconomy().getBlockCost());
        assertFalse(master.getEconomy().isZeroPoints());
        assertFalse(master.getEconomy().isStreakResets());
    }

    @Test
    public void zeroPointStreakResettingMasterDeserializes()
    {
        // Turael/Spria: assignments award no points and reset the streak (FR-R2 section 1a).
        String json = "{\"masterId\":\"turael\",\"name\":\"Turael\",\"aliases\":[\"Aya\"],"
            + "\"economy\":{\"basePoints\":0,\"zeroPoints\":true,\"streakResets\":true}}";

        SourceMaster master = GSON.fromJson(json, SourceMaster.class);

        assertEquals(Integer.valueOf(0), master.getEconomy().getBasePoints());
        assertTrue(master.getEconomy().isZeroPoints());
        assertTrue(master.getEconomy().isStreakResets());
        assertNull("no block cost authored -> null", master.getEconomy().getBlockCost());
    }

    @Test
    public void existingMasterStubsStillParseWithNullExtensions()
    {
        // FR-6: today's {masterId, name} stub files keep loading; the new fields default null.
        SourceMaster stub = GSON.fromJson("{\"masterId\":\"nieve\",\"name\":\"Nieve\"}",
            SourceMaster.class);

        assertEquals("nieve", stub.getMasterId());
        assertNull(stub.getLocation());
        assertNull(stub.getRequirements());
        assertNull(stub.getEconomy());
    }

    @Test
    public void taskWeightByMasterDeserializesAndDefaultsNull()
    {
        SourceTask task = GSON.fromJson(
            "{\"taskId\":\"abyssal-demons\",\"weightByMaster\":{\"duradel\":12,\"konar\":9}}",
            SourceTask.class);
        assertEquals(Integer.valueOf(12), task.getWeightByMaster().get("duradel"));
        assertEquals(Integer.valueOf(9), task.getWeightByMaster().get("konar"));

        SourceTask absent = GSON.fromJson("{\"taskId\":\"bloodveld\"}", SourceTask.class);
        assertNull("weightByMaster defaults null when absent", absent.getWeightByMaster());
    }

    @Test
    public void taskUnlockTypeDeserializesAndDefaultsNull()
    {
        SourceTaskUnlock typed = GSON.fromJson(
            "{\"unlockId\":\"bleed-me-dry\",\"name\":\"Bleed me dry\",\"pointsCost\":75,"
            + "\"type\":\"EXTENSION\"}", SourceTaskUnlock.class);
        assertEquals(UnlockType.EXTENSION, typed.getType());

        SourceTaskUnlock untyped = GSON.fromJson(
            "{\"unlockId\":\"bigger-and-badder\",\"pointsCost\":50}", SourceTaskUnlock.class);
        assertNull("type defaults null when absent (all pre-WA-5 rows)", untyped.getType());
    }

    @Test
    public void rewardFileDeserializes()
    {
        SourceReward reward = GSON.fromJson(
            "{\"rewardId\":\"malevolent-masquerade\",\"name\":\"Malevolent masquerade\","
            + "\"pointsCost\":400,\"effect\":\"GEAR_UNLOCK\","
            + "\"notes\":\"Learn to craft a Slayer helmet.\"}", SourceReward.class);

        assertEquals("malevolent-masquerade", reward.getRewardId());
        assertEquals("Malevolent masquerade", reward.getName());
        assertEquals(Integer.valueOf(400), reward.getPointsCost());
        assertEquals("GEAR_UNLOCK", reward.getEffect());
        assertTrue(reward.getNotes().contains("Slayer helmet"));
    }
}

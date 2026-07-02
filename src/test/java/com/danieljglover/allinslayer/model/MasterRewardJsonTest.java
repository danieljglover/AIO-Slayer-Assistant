package com.danieljglover.allinslayer.model;

import com.google.gson.Gson;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WA-2 (ADR-0018 #1/#3/#6): the runtime master/reward models carried by the second generated
 * resource {@code slayer-meta.json}. Shapes mirror the WA-3 source schema so the WA-8 compile
 * step is a straight copy; all fields Gson-default so a stub master (today's
 * {@code {masterId, name}} files) loads cleanly with nulls.
 */
public class MasterRewardJsonTest
{
    private final Gson gson = new Gson();

    @Test
    public void masterDataRoundTripsAllFieldsIncludingEconomy()
    {
        String json = "{\"masterId\":\"duradel\",\"name\":\"Duradel\","
            + "\"aliases\":[\"Kuradal\"],"
            + "\"location\":\"Shilo Village\","
            + "\"requirements\":{\"combatLevel\":100,\"slayerLevel\":50,"
            + "\"quests\":[\"Shilo Village\"]},"
            + "\"economy\":{\"basePoints\":15,"
            + "\"streakMultipliers\":{\"10\":5,\"50\":15,\"100\":25,\"250\":35,\"1000\":50},"
            + "\"blockCost\":100,\"zeroPoints\":false,\"streakResets\":false}}";

        MasterData m = gson.fromJson(json, MasterData.class);
        assertEquals("duradel", m.getMasterId());
        assertEquals("Duradel", m.getName());
        assertEquals(Arrays.asList("Kuradal"), m.getAliases());
        assertEquals("Shilo Village", m.getLocation());
        assertEquals(Integer.valueOf(100), m.getRequirements().getCombatLevel());
        assertEquals(Integer.valueOf(50), m.getRequirements().getSlayerLevel());
        assertEquals(Arrays.asList("Shilo Village"), m.getRequirements().getQuests());
        assertEquals(Integer.valueOf(15), m.getEconomy().getBasePoints());
        assertEquals(Integer.valueOf(5), m.getEconomy().getStreakMultipliers().get("10"));
        assertEquals(Integer.valueOf(50), m.getEconomy().getStreakMultipliers().get("1000"));
        assertEquals(Integer.valueOf(100), m.getEconomy().getBlockCost());
        assertFalse(m.getEconomy().isZeroPoints());
        assertFalse(m.getEconomy().isStreakResets());

        // Serialise -> deserialise preserves the nested blocks.
        MasterData back = gson.fromJson(gson.toJson(m), MasterData.class);
        assertEquals(Integer.valueOf(15), back.getEconomy().getBasePoints());
        assertEquals(Integer.valueOf(35), back.getEconomy().getStreakMultipliers().get("250"));
        assertEquals(Integer.valueOf(100), back.getRequirements().getCombatLevel());
    }

    @Test
    public void stubMasterDefaultsCleanly()
    {
        // Today's masters/*.json are {masterId, name} stubs - they must load with every enrichment
        // field null (the FR-6 sentinel: absent = unauthored, never a fabricated default).
        MasterData stub = gson.fromJson("{\"masterId\":\"turael\",\"name\":\"Turael\"}",
            MasterData.class);
        assertEquals("turael", stub.getMasterId());
        assertEquals("Turael", stub.getName());
        assertNull("aliases default null", stub.getAliases());
        assertNull("location defaults null", stub.getLocation());
        assertNull("requirements default null", stub.getRequirements());
        assertNull("economy defaults null", stub.getEconomy());
    }

    @Test
    public void zeroPointsAndStreakResetsFlagsRoundTripTrue()
    {
        // Turael/Spria: assignments award nothing and taking a task resets the streak. Primitive
        // booleans (absent = false), so the true rows must round-trip explicitly.
        String json = "{\"masterId\":\"turael\",\"name\":\"Turael\","
            + "\"economy\":{\"basePoints\":0,\"zeroPoints\":true,\"streakResets\":true}}";
        MasterData m = gson.fromJson(json, MasterData.class);
        assertTrue(m.getEconomy().isZeroPoints());
        assertTrue(m.getEconomy().isStreakResets());
        assertEquals(Integer.valueOf(0), m.getEconomy().getBasePoints());
        assertNull("absent multipliers default null", m.getEconomy().getStreakMultipliers());

        MasterData back = gson.fromJson(gson.toJson(m), MasterData.class);
        assertTrue(back.getEconomy().isZeroPoints());
        assertTrue(back.getEconomy().isStreakResets());
    }

    @Test
    public void rewardDataRoundTripsAndEffectDefaultsNullWhenAbsent()
    {
        // WA-6/WA-8: the global reward catalogue rows. effect is the machine tag the gear guards
        // key on (ADR-0018 #6); the compiler maps the source STRING to this enum failing loudly,
        // so at runtime an absent key (never an unknown value) is the only null path.
        String json = "{\"rewardId\":\"malevolent-masquerade\",\"name\":\"Malevolent masquerade\","
            + "\"pointsCost\":400,\"effect\":\"GEAR_UNLOCK\"}";
        RewardData r = gson.fromJson(json, RewardData.class);
        assertEquals("malevolent-masquerade", r.getRewardId());
        assertEquals("Malevolent masquerade", r.getName());
        assertEquals(400, r.getPointsCost());
        assertEquals(RewardEffect.GEAR_UNLOCK, r.getEffect());

        RewardData back = gson.fromJson(gson.toJson(r), RewardData.class);
        assertEquals(RewardEffect.GEAR_UNLOCK, back.getEffect());
        assertEquals(400, back.getPointsCost());

        RewardData bare = gson.fromJson("{\"rewardId\":\"x\",\"name\":\"X\"}", RewardData.class);
        assertNull("effect defaults null when absent", bare.getEffect());
        assertEquals("pointsCost defaults 0 when absent", 0, bare.getPointsCost());
    }

    @Test
    public void rewardEffectCarriesExactlyTheFiveContractValues()
    {
        // The WA-3 contract (team memory): the source-string -> enum mapping must be 1:1 with
        // GEAR_UNLOCK|TASK_UNLOCK|SUPERIOR|CONVENIENCE|COSMETIC. Pin the set so a drift in either
        // layer fails the build, not the compiler run.
        assertEquals(5, RewardEffect.values().length);
        assertEquals(RewardEffect.GEAR_UNLOCK, RewardEffect.valueOf("GEAR_UNLOCK"));
        assertEquals(RewardEffect.TASK_UNLOCK, RewardEffect.valueOf("TASK_UNLOCK"));
        assertEquals(RewardEffect.SUPERIOR, RewardEffect.valueOf("SUPERIOR"));
        assertEquals(RewardEffect.CONVENIENCE, RewardEffect.valueOf("CONVENIENCE"));
        assertEquals(RewardEffect.COSMETIC, RewardEffect.valueOf("COSMETIC"));
    }
}

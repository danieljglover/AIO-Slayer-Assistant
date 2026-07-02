package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * WA-6 (ADR-0018 #6): the new global {@code rewards/*.json} source domain - the account-wide
 * reward-point purchases no per-task {@code unlocks[]} row can hold. Seeded exactly from FR-R2
 * unlocks-audit.md section 1c (costs authoritative). Known double-modelling the ADR blesses:
 * Bigger and Badder and Unholy Helmet also appear as per-task {@code unlocks[]} display rows -
 * the GLOBAL reward is authoritative for ownership.
 */
public class RewardSourceCoverageTest
{
    private static final Path REWARDS = Paths.get("src/main/data/slayer/rewards");
    private static final Gson GSON = new Gson();
    private static final Set<String> EFFECTS = new HashSet<>(Arrays.asList(
        "GEAR_UNLOCK", "TASK_UNLOCK", "SUPERIOR", "CONVENIENCE", "COSMETIC"));

    private static Map<String, SourceReward> load() throws IOException
    {
        Map<String, SourceReward> byId = new HashMap<>();
        try (Stream<Path> paths = Files.list(REWARDS))
        {
            for (Path file : (Iterable<Path>) paths
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()::iterator)
            {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
                {
                    SourceReward r = GSON.fromJson(reader, SourceReward.class);
                    assertTrue("duplicate rewardId " + r.getRewardId(),
                        byId.put(r.getRewardId(), r) == null);
                }
            }
        }
        return byId;
    }

    @Test
    public void everyRewardFileCarriesTheFullRow() throws IOException
    {
        Map<String, SourceReward> rewards = load();
        assertTrue("catalogue is non-empty", !rewards.isEmpty());
        for (SourceReward r : rewards.values())
        {
            assertNotNull("rewardId", r.getRewardId());
            assertNotNull(r.getRewardId() + " name", r.getName());
            assertNotNull(r.getRewardId() + " pointsCost", r.getPointsCost());
            assertNotNull(r.getRewardId() + " effect", r.getEffect());
            // The WA-8 compiler maps this string to RewardEffect failing loudly; pin the value
            // set here so a typo dies in THIS test, not the generation run.
            assertTrue(r.getRewardId() + " effect '" + r.getEffect() + "' is a contract value",
                EFFECTS.contains(r.getEffect()));
        }
    }

    @Test
    public void theSevenGlobalUnlocksMatchTheWikiCatalogue() throws IOException
    {
        Map<String, SourceReward> rewards = load();
        assertRow(rewards, "malevolent-masquerade", "Malevolent masquerade", 400, "GEAR_UNLOCK");
        assertRow(rewards, "broader-fletching", "Broader Fletching", 300, "GEAR_UNLOCK");
        assertRow(rewards, "ring-bling", "Ring bling", 150, "GEAR_UNLOCK");
        assertRow(rewards, "like-a-boss", "Like a Boss", 200, "TASK_UNLOCK");
        assertRow(rewards, "task-storage", "Task Storage", 500, "CONVENIENCE");
        assertRow(rewards, "i-wildy-more-slayer", "I Wildy More Slayer", 0, "TASK_UNLOCK");
        assertRow(rewards, "bigger-and-badder", "Bigger and Badder", 50, "SUPERIOR");
    }

    @Test
    public void exactlyTheTenHelmRecoloursAtOneThousandEach() throws IOException
    {
        Map<String, SourceReward> rewards = load();
        // unlocks-audit.md section 1c: the ten 1000-point cosmetic helm recolour unlocks.
        String[] recolours = {
            "king-black-bonnet", "kalphite-khat", "unholy-helmet", "dark-mantle", "undead-head",
            "use-more-head", "eye-see-you", "twisted-vision", "absolutely-slayin", "oath-breaker",
        };
        for (String id : recolours)
        {
            SourceReward r = rewards.get(id);
            assertNotNull(id + " present", r);
            assertEquals(id + " costs 1000", Integer.valueOf(1000), r.getPointsCost());
            assertEquals(id + " is COSMETIC", "COSMETIC", r.getEffect());
        }
        long cosmetics = rewards.values().stream()
            .filter(r -> "COSMETIC".equals(r.getEffect())).count();
        assertEquals("exactly the ten recolours are COSMETIC", recolours.length, cosmetics);
        assertEquals("catalogue is exactly 7 globals + 10 recolours", 17, rewards.size());
    }

    private static void assertRow(Map<String, SourceReward> rewards, String id, String name,
        int cost, String effect)
    {
        SourceReward r = rewards.get(id);
        assertNotNull(id + " present", r);
        assertEquals(id + " name", name, r.getName());
        assertEquals(id + " cost", Integer.valueOf(cost), r.getPointsCost());
        assertEquals(id + " effect", effect, r.getEffect());
    }
}

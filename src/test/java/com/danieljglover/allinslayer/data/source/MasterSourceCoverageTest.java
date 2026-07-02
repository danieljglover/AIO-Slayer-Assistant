package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WA-4 (ADR-0018 #2/#3): the 9 {@code masters/*.json} carry the master-intrinsic enrichment -
 * location, use-requirements, and the reward-point economy. Sources: FR-R1 masters-coverage.md
 * section 3 headers + FR-R2 unlocks-audit.md sections 1a/1b (streak multipliers x5/x15/x25/x35/x50
 * at the 10th/50th/100th/250th/1000th task; block costs Turael/Spria 40, Mazchna 50, Vannaka 60,
 * Chaeldar 70, Konar 80, Nieve 90, Duradel/Krystilia 100), re-verified against each master's own
 * wiki page 2026-07-01 (Konar Cb 75; Chaeldar Cb 70 + Lost City; Duradel Cb 100 + Slayer 50 +
 * Shilo Village).
 *
 * <p>Deliberately NOT modelled (honest limits, not gaps): the elite-diary base-point boosts
 * (Nieve 12 -> 15 with Western Provinces elite, Konar 18 -> 20 with Kourend &amp; Kebos elite) -
 * {@code basePoints} is the undiaried base; the 4-task grace rule; the flat 30-point skip cost
 * (master-independent, so it lives nowhere on a master row).
 */
public class MasterSourceCoverageTest
{
    private static final Path MASTERS = Paths.get("src/main/data/slayer/masters");
    private static final Gson GSON = new Gson();

    private static Map<String, SourceMaster> load() throws IOException
    {
        Map<String, SourceMaster> byId = new HashMap<>();
        try (Stream<Path> paths = Files.list(MASTERS))
        {
            for (Path file : (Iterable<Path>) paths
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()::iterator)
            {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
                {
                    SourceMaster m = GSON.fromJson(reader, SourceMaster.class);
                    byId.put(m.getMasterId(), m);
                }
            }
        }
        return byId;
    }

    @Test
    public void allNineMastersCarryLocationAndEconomy() throws IOException
    {
        Map<String, SourceMaster> masters = load();
        assertEquals("exactly the 9 known masters", 9, masters.size());
        for (SourceMaster m : masters.values())
        {
            assertNotNull(m.getMasterId() + " has a location", m.getLocation());
            assertNotNull(m.getMasterId() + " has an economy block", m.getEconomy());
            assertNotNull(m.getMasterId() + " has a base points value",
                m.getEconomy().getBasePoints());
            assertNotNull(m.getMasterId() + " has a block cost", m.getEconomy().getBlockCost());
        }
    }

    @Test
    public void basePointsAndBlockCostsMatchTheWikiEconomy() throws IOException
    {
        Map<String, SourceMaster> masters = load();
        // unlocks-audit.md section 1a (base) + 1b (block).
        assertEconomy(masters, "turael", 0, 40);
        assertEconomy(masters, "spria", 0, 40);
        assertEconomy(masters, "mazchna", 6, 50);
        assertEconomy(masters, "vannaka", 8, 60);
        assertEconomy(masters, "chaeldar", 10, 70);
        assertEconomy(masters, "konar", 18, 80);
        assertEconomy(masters, "nieve", 12, 90);
        assertEconomy(masters, "duradel", 15, 100);
        assertEconomy(masters, "krystilia", 25, 100);
    }

    private static void assertEconomy(Map<String, SourceMaster> masters, String id,
        int basePoints, int blockCost)
    {
        SourceMasterEconomy economy = masters.get(id).getEconomy();
        assertEquals(id + " base points", Integer.valueOf(basePoints), economy.getBasePoints());
        assertEquals(id + " block cost", Integer.valueOf(blockCost), economy.getBlockCost());
    }

    @Test
    public void pointAwardingMastersCarryTheStandardStreakMultipliers() throws IOException
    {
        Map<String, SourceMaster> masters = load();
        for (String id : new String[] {"mazchna", "vannaka", "chaeldar", "konar", "nieve",
            "duradel", "krystilia"})
        {
            Map<String, Integer> mult = masters.get(id).getEconomy().getStreakMultipliers();
            assertNotNull(id + " has streak multipliers", mult);
            assertEquals(id + " 10th", Integer.valueOf(5), mult.get("10"));
            assertEquals(id + " 50th", Integer.valueOf(15), mult.get("50"));
            assertEquals(id + " 100th", Integer.valueOf(25), mult.get("100"));
            assertEquals(id + " 250th", Integer.valueOf(35), mult.get("250"));
            assertEquals(id + " 1000th", Integer.valueOf(50), mult.get("1000"));
            assertEquals(id + " exactly the five milestones", 5, mult.size());
        }
    }

    @Test
    public void turaelAndSpriaAwardZeroPointsAndResetTheStreak() throws IOException
    {
        Map<String, SourceMaster> masters = load();
        for (String id : new String[] {"turael", "spria"})
        {
            SourceMaster m = masters.get(id);
            assertTrue(id + " zeroPoints", m.getEconomy().isZeroPoints());
            assertTrue(id + " streakResets", m.getEconomy().isStreakResets());
            assertEquals(id + " base 0", Integer.valueOf(0), m.getEconomy().getBasePoints());
            assertNull(id + " no streak multipliers (0 points regardless)",
                m.getEconomy().getStreakMultipliers());
        }
        // And nobody else does - a copy-paste of the Turael block onto a real master must fail.
        for (String id : new String[] {"mazchna", "vannaka", "chaeldar", "konar", "nieve",
            "duradel", "krystilia"})
        {
            assertFalse(id + " not zeroPoints", masters.get(id).getEconomy().isZeroPoints());
            assertFalse(id + " not streakResets", masters.get(id).getEconomy().isStreakResets());
        }
    }

    @Test
    public void aliasesCoverTheKnownAlternateIdentities() throws IOException
    {
        Map<String, SourceMaster> masters = load();
        // Steve (post-MM2 Nieve), Kuradal (post-WGS Duradel), Aya (post-APoI Turael).
        assertTrue("nieve aliases Steve", masters.get("nieve").getAliases().contains("Steve"));
        assertTrue("duradel aliases Kuradal",
            masters.get("duradel").getAliases().contains("Kuradal"));
        assertTrue("turael aliases Aya", masters.get("turael").getAliases().contains("Aya"));
    }

    @Test
    public void requirementsMatchTheWikiAccessRules() throws IOException
    {
        Map<String, SourceMaster> masters = load();

        // No-requirement masters: anyone can use them (Spria needs her quest, below).
        assertNull("turael has no requirements", masters.get("turael").getRequirements());
        assertNull("krystilia has no requirements", masters.get("krystilia").getRequirements());

        assertTrue(masters.get("spria").getRequirements().getQuests()
            .contains("A Porcine of Interest"));

        SourceMasterRequirements mazchna = masters.get("mazchna").getRequirements();
        assertEquals(Integer.valueOf(20), mazchna.getCombatLevel());
        assertTrue(mazchna.getQuests().contains("Priest in Peril"));

        assertEquals(Integer.valueOf(40),
            masters.get("vannaka").getRequirements().getCombatLevel());

        SourceMasterRequirements chaeldar = masters.get("chaeldar").getRequirements();
        assertEquals(Integer.valueOf(70), chaeldar.getCombatLevel());
        assertTrue(chaeldar.getQuests().contains("Lost City"));

        assertEquals(Integer.valueOf(75),
            masters.get("konar").getRequirements().getCombatLevel());

        assertEquals(Integer.valueOf(85),
            masters.get("nieve").getRequirements().getCombatLevel());

        SourceMasterRequirements duradel = masters.get("duradel").getRequirements();
        assertEquals(Integer.valueOf(100), duradel.getCombatLevel());
        assertEquals(Integer.valueOf(50), duradel.getSlayerLevel());
        assertTrue(duradel.getQuests().contains("Shilo Village"));
    }
}

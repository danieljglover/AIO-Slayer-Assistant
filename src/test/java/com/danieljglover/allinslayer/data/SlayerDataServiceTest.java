package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MasterData;
import com.danieljglover.allinslayer.model.RewardEffect;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.io.StringReader;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlayerDataServiceTest
{
    private SlayerDataService service;

    @Before
    public void setUp()
    {
        service = new SlayerDataService(new Gson());
        service.load();
    }

    @Test
    public void loadsBundledTasks()
    {
        // WC-1..11: the bundled dataset must match the compiled source tree, and the pre-C1
        // baseline of 43 is a floor (a fixed literal was a contested line for every parallel
        // family agent).
        assertEquals(com.danieljglover.allinslayer.data.source.ModularSlayerDataCompiler
            .compile(java.nio.file.Paths.get("src/main/data/slayer")).size(), service.all().size());
        assertTrue(service.all().size() >= 43);
        assertTrue(service.byTaskName("Greater demons").isPresent());
        assertTrue(service.byTaskName("Boss").isPresent());
    }

    @Test
    public void findsByTaskNameCaseInsensitive()
    {
        Optional<TaskData> t = service.byTaskName("abyssal demons");
        assertTrue(t.isPresent());
        assertEquals(85, t.get().getSlayerLevel());
        assertEquals(CombatStyle.MELEE, t.get().getWeakness().getStyle());
    }

    @Test
    public void missingTaskReturnsEmpty()
    {
        assertFalse(service.byTaskName("not a real task").isPresent());
    }

    @Test
    public void loadsBundledMastersFromTheMetaResource()
    {
        // WA-8 (ADR-0018 #1): the second generated resource slayer-meta.json reaches runtime
        // additively - task loading (asserted above) is untouched.
        Optional<MasterData> duradel = service.masterById("duradel");
        assertTrue(duradel.isPresent());
        assertEquals("Duradel", duradel.get().getName());
        assertEquals(Integer.valueOf(15), duradel.get().getEconomy().getBasePoints());
        assertEquals(Integer.valueOf(100), duradel.get().getEconomy().getBlockCost());
        assertFalse(service.masterById("not a master").isPresent());
        assertFalse(service.masterById(null).isPresent());
    }

    @Test
    public void parsesRewardsFromMetaContent()
    {
        // The rewards half of the meta parse, pinned synthetically so it does not depend on the
        // rewards/*.json authoring timeline (the WA-6 coverage test owns the real catalogue).
        SlayerDataService s = new SlayerDataService(new Gson());
        s.loadMeta(new StringReader("{\"masters\":[{\"masterId\":\"duradel\",\"name\":\"Duradel\"}],"
            + "\"rewards\":[{\"rewardId\":\"malevolent-masquerade\",\"name\":\"Malevolent masquerade\","
            + "\"pointsCost\":400,\"effect\":\"GEAR_UNLOCK\"}]}"));

        assertEquals(1, s.rewards().size());
        assertEquals("malevolent-masquerade", s.rewards().get(0).getRewardId());
        assertEquals(400, s.rewards().get(0).getPointsCost());
        assertEquals(RewardEffect.GEAR_UNLOCK, s.rewards().get(0).getEffect());
        assertTrue(s.masterById("duradel").isPresent());
    }

    @Test
    public void corruptMetaDegradesToNoMastersWithoutTouchingTasks()
    {
        // WB-5 discipline applied to the second resource: a corrupt slayer-meta.json must not
        // take down task data (masters/rewards render is additive, the loadout core is not).
        SlayerDataService s = new SlayerDataService(new Gson());
        s.loadTasks(new StringReader("[{\"task\":\"Bloodveld\",\"slayerTargetId\":76}]"));
        s.loadMeta(new StringReader("{ not json ["));

        assertTrue("tasks survive a corrupt meta resource", s.byTaskName("Bloodveld").isPresent());
        assertFalse(s.masterById("duradel").isPresent());
        assertTrue(s.rewards().isEmpty());
    }

    @Test
    public void corruptResourceDegradesToEmptyInsteadOfThrowing()
    {
        // WB-5 (FR-C1 D9): a non-parseable resource must log-and-degrade, never throw through
        // the plugin's startUp(). Empty service = the panel's existing no-data path.
        SlayerDataService corrupt = new SlayerDataService(new Gson());
        corrupt.loadTasks(new StringReader("{ this is not json ["));
        assertTrue("corrupt data -> empty service, not an exception", corrupt.all().isEmpty());

        // Wrong top-level shape (an object where the task array belongs) degrades the same way.
        SlayerDataService wrongShape = new SlayerDataService(new Gson());
        wrongShape.loadTasks(new StringReader("{\"tasks\":[]}"));
        assertTrue(wrongShape.all().isEmpty());
    }

    @Test
    public void nullEntriesAndNullTaskNamesAreSkippedNotFatal()
    {
        // WB-5 (FR-C1 D9): a null array entry or a task with no name used to NPE the whole load
        // (t.getTask().toLowerCase()). Skip the bad row, keep the good ones.
        SlayerDataService partial = new SlayerDataService(new Gson());
        partial.loadTasks(new StringReader(
            "[{\"task\":null,\"slayerTargetId\":5},null,{\"task\":\"Bloodveld\",\"slayerTargetId\":76}]"));

        assertEquals("only the valid row is indexed", 1, partial.all().size());
        assertTrue(partial.byTaskName("Bloodveld").isPresent());
        assertTrue(partial.byTargetVarp(76).isPresent());
    }
}

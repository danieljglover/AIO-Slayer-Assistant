package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * WC-W (ADR-0018 #3, PD-D): retrofit {@code weightByMaster} onto the 43 pre-C1 task families.
 *
 * <p>Weights are provisional from FR-R1 masters-coverage.md section 3 (the nine per-master wiki
 * tables, fetched 2026-07-01), per the PD-D disposition: trust the family SET, spot-check the
 * numbers. SPOT-CHECK (2026-07-02, live wiki re-fetch of the Duradel / Konar quo Maten /
 * Krystilia / Vannaka pages): 19 task/master weight pairs re-verified, ALL matching section 3 -
 * see {@code docs/full-review/wcw-spotcheck.md} and
 * {@link #spotCheckedWeightsMatchTheLiveWiki()}, where those pairs are pinned hard. The
 * remaining pairs stay soft (present + positive, not value-pinned) per PD-D's "no hard
 * assertions on fragile absolute numbers".
 *
 * <p>HONEST UNKNOWN (never fabricate): the Turael and Spria wiki fetches both omitted the
 * Araxytes row (a verified summarizer failure mode, section 3 caveat), so section 3 has NO
 * weight for araxytes x {turael, spria}. Those two entries are deliberately ABSENT from
 * {@code araxytes.json}'s weightByMaster and pinned absent here - authoring them requires a
 * fresh wiki verification first.
 *
 * <p>Scope: exactly the 43 pre-C1 families by name. WC-1..11 family agents ADD new task files
 * in parallel; globbing the directory would race their in-flight work (the DT-B6/FR-BE2
 * shared-tree lesson), so this test never lists the directory.
 */
public class TaskWeightRetrofitTest
{
    private static final Path TASKS = Paths.get("src/main/data/slayer/tasks");
    private static final Gson GSON = new Gson();

    /** The 43 pre-C1 task families (masters-coverage.md section 1.3). */
    private static final List<String> RETROFIT_FAMILIES = Arrays.asList(
        "aberrant-spectres", "abyssal-demons", "ankou", "aquanites", "araxytes", "aviansie",
        "basilisks", "black-demons", "black-dragons", "bloodveld", "blue-dragons", "boss",
        "cave-horrors", "cave-kraken", "dagannoth", "dark-beasts", "drakes", "dust-devils",
        "elves", "fire-giants", "fossil-island-wyverns", "frost-dragons", "gargoyles",
        "greater-demons", "gryphons", "hellhounds", "kalphite", "kurask", "lizardmen",
        "metal-dragons", "mutated-zygomites", "nechryael", "red-dragons", "skeletal-wyverns",
        "smoke-devils", "spiritual-creatures", "suqahs", "trolls", "tzhaar", "vampyres",
        "warped-creatures", "waterfiends", "wyrms");

    /**
     * The documented-UNKNOWN (taskId -> masterIds whose weight section 3 could not source).
     * Non-empty by construction - it is what keeps the "keys == masterIds minus UNKNOWN"
     * assertion from being satisfiable by a lazy "weights for everything" retrofit.
     */
    private static final Map<String, Set<String>> UNKNOWN_WEIGHTS = unknownWeights();

    private static Map<String, Set<String>> unknownWeights()
    {
        Map<String, Set<String>> unknown = new LinkedHashMap<>();
        unknown.put("araxytes", new HashSet<>(Arrays.asList("turael", "spria")));
        return unknown;
    }

    private static SourceTask load(String taskId) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(
            TASKS.resolve(taskId + ".json"), StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, SourceTask.class);
        }
    }

    @Test
    public void allFortyThreeExistingTasksCarryWeightByMaster() throws IOException
    {
        assertEquals("the pre-C1 family list itself", 43, RETROFIT_FAMILIES.size());
        for (String taskId : RETROFIT_FAMILIES)
        {
            SourceTask task = load(taskId);
            assertNotNull(taskId + " carries weightByMaster", task.getWeightByMaster());
            assertFalse(taskId + " weightByMaster is non-empty",
                task.getWeightByMaster().isEmpty());
        }
    }

    @Test
    public void weightKeysAreExactlyTheTasksMastersMinusTheDocumentedUnknowns() throws IOException
    {
        for (String taskId : RETROFIT_FAMILIES)
        {
            SourceTask task = load(taskId);
            Set<String> expected = new TreeSet<>(task.getMasterIds());
            expected.removeAll(UNKNOWN_WEIGHTS.getOrDefault(taskId, new HashSet<>()));

            Map<String, Integer> weights = task.getWeightByMaster();
            assertNotNull(taskId + " carries weightByMaster", weights);
            assertEquals(taskId + " weight keys == masterIds minus documented UNKNOWNs",
                expected, new TreeSet<>(weights.keySet()));
            for (Map.Entry<String, Integer> entry : weights.entrySet())
            {
                assertNotNull(taskId + "/" + entry.getKey() + " weight non-null",
                    entry.getValue());
                assertTrue(taskId + "/" + entry.getKey() + " weight positive",
                    entry.getValue() > 0);
            }
        }
    }

    @Test
    public void documentedUnknownsStayHonestlyAbsent() throws IOException
    {
        // Non-vacuity guard: the UNKNOWN list is real and each entry is genuinely omitted.
        assertFalse("the documented-UNKNOWN list is non-empty", UNKNOWN_WEIGHTS.isEmpty());
        for (Map.Entry<String, Set<String>> entry : UNKNOWN_WEIGHTS.entrySet())
        {
            SourceTask task = load(entry.getKey());
            for (String masterId : entry.getValue())
            {
                assertTrue(entry.getKey() + " lists " + masterId + " as an assigner",
                    task.getMasterIds().contains(masterId));
                assertFalse(entry.getKey() + " has NO fabricated weight for " + masterId,
                    task.getWeightByMaster().containsKey(masterId));
            }
        }
    }

    @Test
    public void spotCheckedWeightsMatchTheLiveWiki() throws IOException
    {
        // 19 pairs re-fetched from the live wiki 2026-07-02 (4 masters); all matched section 3.
        // Full record: docs/full-review/wcw-spotcheck.md. These are the ONLY value-pinned pairs.
        assertWeight("metal-dragons", "duradel", 14);
        assertWeight("abyssal-demons", "duradel", 12);
        assertWeight("boss", "duradel", 12);
        assertWeight("hellhounds", "duradel", 10);
        assertWeight("mutated-zygomites", "duradel", 2);
        assertWeight("araxytes", "duradel", 10);
        assertWeight("metal-dragons", "konar", 15);
        assertWeight("wyrms", "konar", 10);
        assertWeight("waterfiends", "konar", 2);
        assertWeight("kurask", "konar", 3);
        assertWeight("mutated-zygomites", "konar", 2);
        assertWeight("greater-demons", "krystilia", 8);
        assertWeight("black-dragons", "krystilia", 4);
        assertWeight("spiritual-creatures", "krystilia", 6);
        assertWeight("ankou", "krystilia", 6);
        assertWeight("gargoyles", "vannaka", 5);
        assertWeight("gryphons", "vannaka", 10);
        assertWeight("nechryael", "vannaka", 5);
        assertWeight("kalphite", "vannaka", 7);
    }

    private static void assertWeight(String taskId, String masterId, int weight)
        throws IOException
    {
        Map<String, Integer> weights = load(taskId).getWeightByMaster();
        assertNotNull(taskId + " carries weightByMaster", weights);
        assertEquals(taskId + "/" + masterId + " wiki-verified weight",
            Integer.valueOf(weight), weights.get(masterId));
    }

    @Test
    public void compilerEmitsTheRetrofittedWeightsToTheRuntimeDataset()
    {
        // WA-7 wired the emission (setWeightByMaster, empty->null); this proves the retrofit
        // data actually rides it - every one of the 43 reaches TaskData non-null. TaskData is
        // keyed by display name (getTask), so resolve each family through its source name.
        Map<String, TaskData> byName =
            ModularSlayerDataCompiler.compile(Paths.get("src/main/data/slayer")).stream()
                .collect(Collectors.toMap(TaskData::getTask, Function.identity()));
        for (String taskId : RETROFIT_FAMILIES)
        {
            SourceTask source = loadQuietly(taskId);
            TaskData task = byName.get(source.getName());
            assertNotNull(taskId + " compiled", task);
            assertNotNull(taskId + " weightByMaster reaches the runtime model",
                task.getWeightByMaster());
            assertEquals(taskId + " compiled weights == source weights",
                source.getWeightByMaster(), task.getWeightByMaster());
        }
    }

    private static SourceTask loadQuietly(String taskId)
    {
        try
        {
            return load(taskId);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}

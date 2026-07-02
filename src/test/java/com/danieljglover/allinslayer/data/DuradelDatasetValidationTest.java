package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DuradelDatasetValidationTest
{
    // Curated, reviewed canonical Duradel task-name set (exact spelling/case as in slayer-data.json).
    // Pins the reviewed list so no task can be silently added or removed without updating this test.
    private static final Set<String> EXPECTED_DURADEL_TASKS = new HashSet<>(Arrays.asList(
        "Abyssal demons",
        "Smoke devils",
        "Aberrant spectres",
        "Ankou",
        "Aquanites",
        "Araxytes",
        "Aviansie",
        "Basilisks",
        "Black demons",
        "Black dragons",
        "Bloodveld",
        "Blue dragons",
        "Boss",
        "Cave horrors",
        "Cave kraken",
        "Dagannoth",
        "Dark beasts",
        "Drakes",
        "Dust devils",
        "Elves",
        "Fire giants",
        "Fossil Island wyverns",
        "Frost Dragons",
        "Gargoyles",
        "Greater demons",
        "Gryphons",
        "Hellhounds",
        "Kalphite",
        "Kurask",
        "Lizardmen",
        "Metal dragons",
        "Mutated zygomites",
        "Nechryael",
        "Red dragons",
        "Skeletal wyverns",
        "Spiritual creatures",
        "Suqahs",
        "Trolls",
        "TzHaar",
        "Vampyres",
        "Warped creatures",
        "Waterfiends",
        "Wyrms"));

    private List<TaskData> duradel;

    @Before
    public void setUp()
    {
        SlayerDataService service = new SlayerDataService(new Gson());
        service.load();
        duradel = service.all().stream()
            .filter(t -> t.getAssignedBy() != null && t.getAssignedBy().contains("duradel"))
            .collect(Collectors.toList());
    }

    @Test
    public void hasFullDuradelTaskSet()
    {
        assertTrue("expected >= 40 Duradel tasks, found " + duradel.size(), duradel.size() >= 40);
    }

    @Test
    public void noDuplicateTaskNames()
    {
        Set<String> names = new HashSet<>();
        for (TaskData t : duradel)
        {
            assertTrue("duplicate task: " + t.getTask(), names.add(t.getTask().toLowerCase()));
        }
    }

    @Test
    public void noDuplicateTargetIds()
    {
        Set<Integer> ids = new HashSet<>();
        for (TaskData t : duradel)
        {
            assertTrue("duplicate slayerTargetId: " + t.getSlayerTargetId(),
                ids.add(t.getSlayerTargetId()));
        }
    }

    @Test
    public void frostDragonsAddedAsTheAuthoritativeListRequires()
    {
        // MV-D11 / FR-2: Frost Dragons is the one Duradel task missing from the original 42-set and is
        // added (task 43, draconic). Additions to the authoritative list must be explicit.
        TaskData frost = taskByName("Frost Dragons");
        assertTrue("Frost Dragons is draconic (dragonbane applies)", frost.isDragon());
        assertNotNull("Frost Dragons has a weakness", frost.getWeakness());
        assertNotNull("Frost Dragons carries variants", frost.getVariants());
    }

    @Test
    public void taskNamesMatchCanonicalSet()
    {
        // Curated, reviewed canonical Duradel task-name set. Any dataset name NOT in this set
        // (e.g. a re-introduced hallucination) fails; any expected name missing from data fails.
        Set<String> expected = EXPECTED_DURADEL_TASKS;
        Set<String> actual = duradel.stream()
            .map(t -> t.getTask())
            .collect(Collectors.toSet());
        assertEquals("dataset Duradel task names must equal the canonical reviewed set",
            expected, actual);
    }

    @Test
    public void undeadTasksAreExactlyAnkouAndAberrantSpectres()
    {
        // LFB-1 / FR-12.4: the Salve-amulet vs-undead predicate reads TaskData.undead. The undead set
        // is data-curated to exactly {Ankou, Aberrant spectres} (verified against the OSRS Undead
        // attribute). The count assertion guards against accidental adds and documents the set.
        assertTrue("Ankou is undead", taskByName("Ankou").isUndead());
        assertTrue("Aberrant spectres is undead", taskByName("Aberrant spectres").isUndead());
        // Look undead but are NOT (Salve does not apply): magically-animated remains / vampyres / demons.
        assertFalse("Skeletal wyverns are not undead", taskByName("Skeletal wyverns").isUndead());
        assertFalse("Vampyres are not undead", taskByName("Vampyres").isUndead());
        assertFalse("Abyssal demons are not undead", taskByName("Abyssal demons").isUndead());

        long undeadCount = duradel.stream().filter(TaskData::isUndead).count();
        assertEquals("exactly two Duradel tasks are undead", 2L, undeadCount);
    }

    @Test
    public void dragonTasksAreExactlyTheDraconicSet()
    {
        // WDB-7 (ADR-0008) + WDBX correction: the Dragon-hunter vs-dragon predicate reads
        // TaskData.dragon. Draconic = creatures with the OSRS dragon attribute that dragonbane
        // weapons hit. Verified against the OSRS Wiki Dragonbane_weapons page: dragons, wyverns,
        // Wyrms, Drakes (and Hydras) all take dragonbane damage. Wyrms (MELEE) is the impactful
        // case - Dragon hunter lance now ranks up there. Drakes (RANGED) flagged for correctness.
        Set<String> expected = new HashSet<>(Arrays.asList(
            "Black dragons", "Blue dragons", "Red dragons", "Metal dragons",
            "Fossil Island wyverns", "Skeletal wyverns", "Wyrms", "Drakes", "Frost Dragons"));
        Set<String> actual = duradel.stream().filter(TaskData::isDragon)
            .map(TaskData::getTask).collect(Collectors.toSet());
        assertEquals("dragon flag must mark exactly the draconic set (dragons + wyverns + wyrms + drakes + frost)", expected, actual);
        // Look draconic but are NOT flagged (uncertain / out of scope):
        assertFalse("Hellhounds are not draconic", taskByName("Hellhounds").isDragon());
    }

    @Test
    public void demonTasksAreExactlyTheVerifiedDemonSet()
    {
        // WDB-9 / FR-14.3 (ADR-0009): demonbane predicate reads TaskData.demon. Verified by the OSRS
        // demon attribute = creatures the wiki marks as demonic, so demonbane weapons apply.
        Set<String> expected = new HashSet<>(Arrays.asList(
            "Abyssal demons", "Black demons", "Bloodveld", "Greater demons", "Hellhounds", "Nechryael",
            "Waterfiends"));
        Set<String> actual = duradel.stream().filter(TaskData::isDemon)
            .map(TaskData::getTask).collect(Collectors.toSet());
        assertEquals("demon flag marks exactly the seven verified demon tasks", expected, actual);
        assertEquals(7L, duradel.stream().filter(TaskData::isDemon).count());
        assertTrue("Hellhounds are demons; demonbane weapons work on them", taskByName("Hellhounds").isDemon());
        assertTrue("Waterfiends are demons; demonbane weapons work on them", taskByName("Waterfiends").isDemon());
        assertFalse("Smoke devils are not demons", taskByName("Smoke devils").isDemon());
        assertFalse("Dust devils are not demons", taskByName("Dust devils").isDemon());
    }

    @Test
    public void kalphiteTasksAreExactlyKalphite()
    {
        // WDB-9 / FR-14.3 (ADR-0009): the Keris predicate reads TaskData.kalphite = exactly {Kalphite}.
        Set<String> actual = duradel.stream().filter(TaskData::isKalphite)
            .map(TaskData::getTask).collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList("Kalphite")), actual);
        assertEquals(1L, duradel.stream().filter(TaskData::isKalphite).count());
    }

    @Test
    public void wildernessLocationFlagsMatchCurrentDataset()
    {
        // WDB-17 (ADR-0009 A.5): the per-LOCATION Wilderness flag controls Wilderness weapon credit.
        // Wiki source completion adds Wilderness task locations as each task page is migrated.
        Set<String> withWildy = duradel.stream()
            .filter(t -> t.getLocations() != null
                && t.getLocations().stream().anyMatch(l -> l.isWilderness()))
            .map(TaskData::getTask)
            .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList("Ankou", "Abyssal demons", "Aviansie", "Black demons",
            "Black dragons", "Bloodveld", "Dust devils", "Fire giants", "Greater demons", "Hellhounds", "Nechryael",
            "Spiritual creatures", "Vampyres")), withWildy);
    }

    private TaskData taskByName(String name)
    {
        return duradel.stream()
            .filter(t -> name.equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("task not found in dataset: " + name));
    }

    @Test
    public void everyTaskIsComplete()
    {
        for (TaskData t : duradel)
        {
            String where = "task '" + t.getTask() + "'";
            assertTrue(where + " name", t.getTask() != null && !t.getTask().isEmpty());
            assertTrue(where + " targetId", t.getSlayerTargetId() > 0);
            assertTrue(where + " slayerLevel", t.getSlayerLevel() >= 1 && t.getSlayerLevel() <= 99);
            assertNotNull(where + " monsters", t.getMonsters());
            assertFalse(where + " monsters empty", t.getMonsters().isEmpty());
            assertNotNull(where + " npcIds", t.getNpcIds());
            assertFalse(where + " npcIds empty", t.getNpcIds().isEmpty());
            assertNotNull(where + " locations", t.getLocations());
            assertFalse(where + " locations empty", t.getLocations().isEmpty());
            // ADR-0014: the Boss meta-task has a NULL task-level profile (each boss variant carries its
            // own). Its variants are validated by MonsterVariantDatasetTest; skip the profile asserts.
            if ("Boss".equals(t.getTask()))
            {
                assertNotNull(where + " variants", t.getVariants());
                assertFalse(where + " variants empty", t.getVariants().isEmpty());
                continue;
            }
            assertNotNull(where + " weakness", t.getWeakness());
            assertNotNull(where + " weakness style", t.getWeakness().getStyle());
            assertNotNull(where + " monsterDefence", t.getMonsterDefence());
            // The engine is now stat- and weakness-driven (loadouts/StyleLoadout removed, LD14): a MAGIC
            // weakness must name an element so ConsumableSelector can pick the spell tier (FR-6).
            if (t.getWeakness().getStyle() == CombatStyle.MAGIC)
            {
                String element = t.getWeakness().getElement();
                assertNotNull(where + " MAGIC weakness element", element);
                assertFalse(where + " MAGIC weakness element empty", element.isEmpty());
            }
        }
    }
}

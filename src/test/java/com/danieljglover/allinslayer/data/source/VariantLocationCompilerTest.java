package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.TaskData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ADR-0017 compiler emission: DT-B4 (location safeSpot/accessNote reach runtime) and DT-B5 (per-variant
 * locationNames derivation). Temp-dir fixtures give full control over the derivation edge cases; a
 * real-dataset spot-check pins the abyssal-demons wiring end to end.
 */
public class VariantLocationCompilerTest
{
    // ---- DT-B4 : SlayerLocation.safeSpot + accessNote reach runtime -------------------------------

    @Test
    public void compilerEmitsSafeSpotAndAccessNoteOntoRuntimeLocation() throws IOException
    {
        Path root = tempDir();
        writeMaster(root);
        write(root.resolve("locations/safespot-cave.json"),
            "{\"locationId\":\"safespot-cave\",\"name\":\"Safespot Cave\",\"multi\":true,"
                + "\"cannon\":false,\"burst\":true,\"konarLockable\":false,\"safeSpot\":true,"
                + "\"accessNote\":\"Range from behind the rocks; needs Priest in Peril.\"}");
        write(root.resolve("locations/plain-cave.json"),
            "{\"locationId\":\"plain-cave\",\"name\":\"Plain Cave\",\"multi\":false,"
                + "\"cannon\":true,\"burst\":false,\"konarLockable\":true}");
        write(root.resolve("tasks/gargoyles.json"),
            "{\"taskId\":\"gargoyles\",\"name\":\"Gargoyles\",\"slayerTargetId\":74,\"slayerLevel\":75,"
                + "\"masterIds\":[\"duradel\"],\"variantIds\":[\"gargoyle\"],"
                + "\"defaultVariantId\":\"gargoyle\",\"locationIds\":[\"safespot-cave\",\"plain-cave\"]}");
        writeGargoyle(root);

        List<SlayerLocation> locations = ModularSlayerDataCompiler.compile(root).get(0).getLocations();

        assertEquals(2, locations.size());
        SlayerLocation safespot = locations.get(0);
        assertEquals("Safespot Cave", safespot.getName());
        assertTrue("safeSpot survives compilation", safespot.isSafeSpot());
        assertEquals("Range from behind the rocks; needs Priest in Peril.", safespot.getAccessNote());

        SlayerLocation plain = locations.get(1);
        assertFalse("a location with no safeSpot key defaults false", plain.isSafeSpot());
        assertNull("a location with no accessNote key defaults null", plain.getAccessNote());
    }

    // ---- DT-B5 : per-variant locationNames derivation (ADR-0017 #1) -------------------------------

    @Test
    public void variantHomeLocationIdComesFirstThenVariantInfoMatchesInTaskOrder() throws IOException
    {
        // (a) locationId-first: the variant's authored home location (charlie, task-position 3) leads,
        // then the variantInfo matches follow in TASK order (Alpha at position 1), deduped.
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        writeLocation(root, "bravo", "Bravo");
        writeLocation(root, "charlie", "Charlie");
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"v1\"],\"defaultVariantId\":\"v1\","
                + "\"locationIds\":[\"alpha\",\"bravo\",\"charlie\"],"
                + "\"variantInfo\":[{\"variantId\":\"v1\",\"name\":\"Mob\","
                + "\"locations\":[\"Alpha\",\"Charlie\"]}]}");
        writeVariant(root, "{\"variantId\":\"v1\",\"name\":\"Mob\",\"locationId\":\"charlie\"}");

        assertEquals(list("Charlie", "Alpha"), onlyVariant(root).getLocationNames());
    }

    @Test
    public void variantInfoMatchesAreTaskOrderedCaseInsensitiveTrimmedAndUnmatchedDropped()
        throws IOException
    {
        // (b) variantInfo-only: matches are exact case-insensitive TRIMMED against the task location
        // names, emitted in TASK order (not variantInfo order), and unmatched strings are dropped -
        // no fuzzy matching, no fabrication.
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        writeLocation(root, "bravo", "Bravo");
        writeLocation(root, "charlie", "Charlie");
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"v1\"],\"defaultVariantId\":\"v1\","
                + "\"locationIds\":[\"alpha\",\"bravo\",\"charlie\"],"
                + "\"variantInfo\":[{\"variantId\":\"v1\",\"name\":\"Mob\","
                + "\"locations\":[\"  charlie \",\"ALPHA\",\"Nowhere\"]}]}");
        writeVariant(root, "{\"variantId\":\"v1\",\"name\":\"Mob\"}");

        assertEquals(list("Alpha", "Charlie"), onlyVariant(root).getLocationNames());
    }

    @Test
    public void unlinkedVariantAndBossVariantEmitNoLocationNames() throws IOException
    {
        // (c) neither signal -> empty (null sentinel). A BOSS variant emits empty even when it has a
        // valid in-task locationId and a matching variantInfo row: bosses are handled as a free-text
        // note only (GAP-3, DL disposition "boss variants emit EMPTY locationNames").
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        writeLocation(root, "bravo", "Bravo");
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"base\",\"bossx\"],\"defaultVariantId\":\"base\","
                + "\"locationIds\":[\"alpha\",\"bravo\"],"
                + "\"variantInfo\":[{\"variantId\":\"bossx\",\"name\":\"Boss\","
                + "\"locations\":[\"Alpha\"]}]}");
        write(root.resolve("monsters/fam/base.json"), "{\"variantId\":\"base\",\"name\":\"Base\"}");
        write(root.resolve("monsters/fam/bossx.json"),
            "{\"variantId\":\"bossx\",\"name\":\"Boss\",\"boss\":true,\"locationId\":\"bravo\"}");

        TaskData task = ModularSlayerDataCompiler.compile(root).get(0);
        MonsterVariant base = named(task, "Base");
        MonsterVariant boss = named(task, "Boss");
        assertNull("an unlinked variant emits no locationNames", base.getLocationNames());
        assertNull("a boss variant emits no locationNames even with in-task location signal (GAP-3)",
            boss.getLocationNames());
    }

    @Test
    public void variantLocationIdOutsideTheTaskIsDroppedButFreeTextNoteRemains() throws IOException
    {
        // (d) a variant.locationId pointing OUTSIDE the task's locationIds is dropped from the linkage
        // (candidates are always a subset of task locations); the free-text `location` note survives.
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        writeLocation(root, "bravo", "Bravo");
        writeLocation(root, "gamma", "Gamma"); // exists as a file, but NOT in the task's locationIds
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"v1\"],\"defaultVariantId\":\"v1\","
                + "\"locationIds\":[\"alpha\",\"bravo\"]}");
        writeVariant(root,
            "{\"variantId\":\"v1\",\"name\":\"Mob\",\"locationId\":\"gamma\","
                + "\"location\":\"Gamma Cave (free text)\"}");

        MonsterVariant v = onlyVariant(root);
        assertNull("out-of-task locationId is dropped from the linkage", v.getLocationNames());
        assertEquals("free-text location note is untouched", "Gamma Cave (free text)", v.getLocation());
    }

    @Test
    public void derivationIsDeterministicAcrossCompilations() throws IOException
    {
        // (e) compiling the same sources twice derives identical locationNames.
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        writeLocation(root, "bravo", "Bravo");
        writeLocation(root, "charlie", "Charlie");
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"v1\"],\"defaultVariantId\":\"v1\","
                + "\"locationIds\":[\"alpha\",\"bravo\",\"charlie\"],"
                + "\"variantInfo\":[{\"variantId\":\"v1\",\"name\":\"Mob\","
                + "\"locations\":[\"Charlie\",\"Alpha\"]}]}");
        writeVariant(root, "{\"variantId\":\"v1\",\"name\":\"Mob\"}");

        List<String> first = onlyVariant(root).getLocationNames();
        List<String> second = onlyVariant(root).getLocationNames();
        assertEquals(list("Alpha", "Charlie"), first);
        assertEquals(first, second);
    }

    @Test
    public void realAbyssalDemonsDatasetDerivesTheExpectedSubsets()
    {
        // Real-dataset spot-check (plan DT-B5): abyssal-demons has variantInfo, multiple variants, and
        // a boss - the representative wiring proof.
        TaskData abyssal = realTask("Abyssal demons");
        assertEquals(list("Abyssal Area", "Slayer Tower", "Wilderness Slayer Cave"),
            named(abyssal, "Abyssal demon").getLocationNames());
        assertEquals(list("Abyssal Area", "Catacombs of Kourend", "Slayer Tower"),
            named(abyssal, "Greater abyssal demon").getLocationNames());
        assertNull("the Abyssal Sire boss variant has no location linkage (GAP-3)",
            named(abyssal, "Abyssal Sire").getLocationNames());
    }

    @Test
    public void everyRealDerivedLocationSubsetIsWithinItsTaskAndBossesAreEmpty()
    {
        // Robust invariant over the whole real dataset: any non-null locationNames is a duplicate-free
        // subset of its task's location names, and every boss variant is empty (churn-proof guard).
        for (TaskData task : ModularSlayerDataCompiler.compile(REAL_ROOT))
        {
            List<String> taskNames = new ArrayList<>();
            for (SlayerLocation loc : task.getLocations())
            {
                taskNames.add(loc.getName());
            }
            if (task.getVariants() == null)
            {
                continue;
            }
            for (MonsterVariant v : task.getVariants())
            {
                if (v.isBoss())
                {
                    assertTrue("boss " + v.getName() + " must have empty locationNames",
                        v.getLocationNames() == null || v.getLocationNames().isEmpty());
                }
                List<String> names = v.getLocationNames();
                if (names == null)
                {
                    continue;
                }
                assertFalse("locationNames must not be an empty list (use null sentinel): "
                    + task.getTask() + "/" + v.getName(), names.isEmpty());
                assertEquals("no duplicates in " + task.getTask() + "/" + v.getName(),
                    names.size(), new java.util.LinkedHashSet<>(names).size());
                for (String name : names)
                {
                    assertTrue(task.getTask() + "/" + v.getName() + " -> '" + name
                        + "' is not one of its task locations " + taskNames, taskNames.contains(name));
                }
            }
        }
    }

    // ---- DT-B6 : validator FK checks for locationComparison + variant.locationId -----------------

    @Test
    public void brokenLocationComparisonLocationIdFailsValidation() throws IOException
    {
        // GAP-5: locationComparison[].locationId is a foreign key into the location files; a broken one
        // must fail generation rather than rot silently.
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"locationIds\":[\"alpha\"],"
                + "\"locationComparison\":[{\"locationId\":\"ghost-location\",\"name\":\"Ghost\"}]}");

        assertValidationError(root, "unknown locationComparison locationId: ghost-location");
    }

    @Test
    public void brokenVariantLocationIdFailsValidation() throws IOException
    {
        // The variant-level locationId FK (used by the DT-B5 derivation) is now validated too.
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"v1\"],\"defaultVariantId\":\"v1\",\"locationIds\":[\"alpha\"]}");
        writeVariant(root, "{\"variantId\":\"v1\",\"name\":\"Mob\",\"locationId\":\"ghost-location\"}");

        assertValidationError(root, "variant v1 references unknown locationId: ghost-location");
    }

    @Test
    public void validLocationComparisonAndVariantLocationIdFksCompileCleanly() throws IOException
    {
        // Guard against over-strict validation: FKs that resolve to a known location must pass.
        Path root = tempDir();
        writeMaster(root);
        writeLocation(root, "alpha", "Alpha");
        write(root.resolve("tasks/t.json"),
            "{\"taskId\":\"t\",\"name\":\"T\",\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"v1\"],\"defaultVariantId\":\"v1\",\"locationIds\":[\"alpha\"],"
                + "\"locationComparison\":[{\"locationId\":\"alpha\",\"name\":\"Alpha\"}]}");
        writeVariant(root, "{\"variantId\":\"v1\",\"name\":\"Mob\",\"locationId\":\"alpha\"}");

        assertEquals(1, ModularSlayerDataCompiler.compile(root).size());
    }

    private static void assertValidationError(Path root, String expected)
    {
        try
        {
            ModularSlayerDataCompiler.compile(root);
            fail("expected validation failure containing: " + expected);
        }
        catch (SlayerDataValidationException ex)
        {
            assertTrue("expected an error containing '" + expected + "' but got " + ex.getErrors(),
                ex.getErrors().stream().anyMatch(e -> e.contains(expected)));
        }
    }

    // ---- helpers ----------------------------------------------------------------------------------

    private static final Path REAL_ROOT = Paths.get("src/main/data/slayer");

    private static MonsterVariant onlyVariant(Path root)
    {
        return ModularSlayerDataCompiler.compile(root).get(0).getVariants().get(0);
    }

    private static MonsterVariant named(TaskData task, String name)
    {
        return task.getVariants().stream()
            .filter(v -> name.equals(v.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing variant: " + name));
    }

    private static TaskData realTask(String name)
    {
        return ModularSlayerDataCompiler.compile(REAL_ROOT).stream()
            .filter(t -> name.equals(t.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing real task: " + name));
    }

    private static List<String> list(String... names)
    {
        List<String> out = new ArrayList<>();
        for (String name : names)
        {
            out.add(name);
        }
        return out;
    }

    private static void writeLocation(Path root, String id, String name) throws IOException
    {
        write(root.resolve("locations/" + id + ".json"),
            "{\"locationId\":\"" + id + "\",\"name\":\"" + name + "\",\"multi\":false,"
                + "\"cannon\":false,\"burst\":false,\"konarLockable\":false}");
    }

    private static void writeVariant(Path root, String json) throws IOException
    {
        write(root.resolve("monsters/fam/v1.json"), json);
    }

    // ---- shared fixtures --------------------------------------------------------------------------

    private static void writeMaster(Path root) throws IOException
    {
        write(root.resolve("masters/duradel.json"), "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
    }

    private static void writeGargoyle(Path root) throws IOException
    {
        write(root.resolve("monsters/gargoyles/gargoyle.json"),
            "{\"variantId\":\"gargoyle\",\"name\":\"Gargoyle\",\"npcIds\":[1543],\"combatLevel\":111,"
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
                + "\"monsterDefence\":{\"defenceLevel\":80,\"stab\":40,\"slash\":40,\"crush\":40,"
                + "\"magic\":50,\"range\":40}}");
    }

    private static Path tempDir() throws IOException
    {
        return Files.createTempDirectory("adr0017-compiler");
    }

    private static void write(Path path, String json) throws IOException
    {
        Files.createDirectories(path.getParent());
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }
}

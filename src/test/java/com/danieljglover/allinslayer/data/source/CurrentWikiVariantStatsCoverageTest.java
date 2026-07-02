package com.danieljglover.allinslayer.data.source;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CurrentWikiVariantStatsCoverageTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void monsterVariantStatsMatchCurrentWikiInfoboxes() throws IOException
    {
        assertVariant("aberrant-spectres/aberrant-spectre-lvl96.json", 90, 20, 20, 20, 0, null);
        assertVariant("ankou/ankou-lvl75.json", null, 0, 0, 0, 0, "air");
        assertVariant("aquanites/aquanite-lvl145.json", null, null, 80, 80, 140, null);
        assertVariant("aquanites/elder-aquanite-lvl305.json", null, null, null, null, 140, null);
        assertVariant("araxytes/araxyte-lvl146.json", null, null, 30, 20, 20, "fire");
        assertVariant("araxytes/dreadborn-araxyte-lvl281.json", null, null, null, null, 10, null);
        assertVariant("aviansie/aviansie-lvl69.json", null, 0, 0, 0, 0, "air");
        assertVariant("basilisks/basilisk-lvl61.json", 75, null, null, 0, null, "earth");
        assertVariant("black-demons/black-demon-lvl172.json", null, 0, 0, 0, null, "water");
        assertVariant("black-dragons/black-dragon-lvl227.json", 200, 0, 70, 70, null, "water");
        assertVariant("bloodveld/bloodveld-lvl76.json", 30, 0, 0, 0, 0, null);
        assertVariant("blue-dragons/blue-dragon-lvl111.json", 95, 0, 70, 70, 60, "water");
        assertVariant("gargoyles/gargoyle-lvl111.json", 107, 50, 60, -20, 20, "earth");
        assertCombatLevel("hellhounds/greater-skeleton-hellhound-vet-ion-lvl281.json", 231);
        assertCombatLevel("hellhounds/skeleton-hellhound-vet-ion-lvl214.json", 194);
    }

    @Test
    public void spiritualCreaturesTaskUsesConcreteVariantsOnly() throws IOException
    {
        SourceTask task = read(Paths.get("src/main/data/slayer/tasks/spiritual-creatures.json"), SourceTask.class);

        assertFalse(task.getVariantIds().contains("spiritual-creature"));
    }

    private static void assertVariant(String relativePath, Integer defenceLevel, Integer stab, Integer slash,
        Integer crush, Integer magic, String element) throws IOException
    {
        SourceMonsterVariant variant = read(Paths.get("src/main/data/slayer/monsters", relativePath),
            SourceMonsterVariant.class);

        if (defenceLevel != null)
        {
            assertEquals(relativePath + " defenceLevel", defenceLevel.intValue(),
                variant.getMonsterDefence().getDefenceLevel());
        }
        if (stab != null)
        {
            assertEquals(relativePath + " stab", stab.intValue(), variant.getMonsterDefence().getStab());
        }
        if (slash != null)
        {
            assertEquals(relativePath + " slash", slash.intValue(), variant.getMonsterDefence().getSlash());
        }
        if (crush != null)
        {
            assertEquals(relativePath + " crush", crush.intValue(), variant.getMonsterDefence().getCrush());
        }
        if (magic != null)
        {
            assertEquals(relativePath + " magic", magic.intValue(), variant.getMonsterDefence().getMagic());
        }
        if (element != null)
        {
            assertEquals(relativePath + " element", element, variant.getWeakness().getElement());
        }
    }

    private static void assertCombatLevel(String relativePath, int combatLevel) throws IOException
    {
        SourceMonsterVariant variant = read(Paths.get("src/main/data/slayer/monsters", relativePath),
            SourceMonsterVariant.class);

        assertEquals(relativePath + " combatLevel", Integer.valueOf(combatLevel), variant.getCombatLevel());
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

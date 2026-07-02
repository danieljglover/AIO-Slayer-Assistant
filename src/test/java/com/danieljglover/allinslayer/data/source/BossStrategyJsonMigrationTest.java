package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.MonsterVariant;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BossStrategyJsonMigrationTest
{
    private static final Gson GSON = new Gson();
    private static final Path DATA_ROOT = Paths.get("src/main/data/slayer");
    private static final Path BOSS_DIR = DATA_ROOT.resolve("monsters/boss");
    private static final Path STRATEGIES_DIR = DATA_ROOT.resolve("strategies");

    @Test
    public void everyBossVariantUsesDirectoryJsonStrategy() throws IOException
    {
        for (SourceMonsterVariant variant : bossVariants())
        {
            assertNotNull("boss variant should declare strategyId: " + variant.getName(), variant.getStrategyId());

            Path json = STRATEGIES_DIR.resolve(variant.getStrategyId()).resolve("strategy.json");
            assertTrue("boss strategy JSON missing for " + variant.getName() + ": " + json, Files.exists(json));
            assertFalse("legacy boss strategy markdown should be migrated: " + variant.getStrategyId(),
                Files.exists(STRATEGIES_DIR.resolve(variant.getStrategyId() + ".md")));

            SourceStrategy strategy = read(json, SourceStrategy.class);
            assertEquals(variant.getStrategyId(), strategy.getStrategyId());
            assertTrue("strategy should map boss variant " + variant.getVariantId(),
                strategy.getVariantIds().contains(variant.getVariantId()));
            assertNotNull("boss strategy should keep plugin loadout fields: " + strategy.getStrategyId(),
                strategy.getPlugin());
            assertNotNull("boss strategy should keep source URL: " + strategy.getStrategyId(),
                strategy.getSourceUrl());
            assertFalse("boss strategy should include LLM-readable requirements: " + strategy.getStrategyId(),
                strategy.getRequirements().isEmpty());
            assertFalse("boss strategy should include LLM-readable mechanics: " + strategy.getStrategyId(),
                strategy.getMechanics().isEmpty());
            assertFalse("boss strategy should include method breakdowns: " + strategy.getStrategyId(),
                strategy.getMethods().isEmpty());
            assertFalse("boss strategy should include style options: " + strategy.getStrategyId(),
                strategy.getStyleOptions().isEmpty());
        }
    }

    @Test
    public void phantomMuspahHasCompiledStrategyAndWikiMethodCoverage() throws IOException
    {
        SourceStrategy strategy = read(STRATEGIES_DIR.resolve("phantom-muspah/strategy.json"), SourceStrategy.class);

        assertEquals("phantom-muspah", strategy.getStrategyId());
        assertEquals("https://oldschool.runescape.wiki/w/Phantom_Muspah/Strategies", strategy.getSourceUrl());
        assertEquals(CombatStyle.RANGED, strategy.getPlugin().getPrimaryStyle());
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("twisted-bow"));
        assertTrue(strategy.getPlugin().getPrimaryWeapons().contains("bow-of-faerdhinen"));
        assertTrue(strategy.getPlugin().getSecondaryWeapons().stream()
            .anyMatch(weapon -> "tumeken-s-shadow".equals(weapon.getWeaponId())
                && weapon.getStyle() == CombatStyle.MAGIC));

        Set<String> methodIds = strategy.getMethods().stream()
            .map(SourceStrategyMethod::getMethodId)
            .collect(Collectors.toSet());
        assertTrue(methodIds.contains("general"));
        assertTrue(methodIds.contains("ranged-form"));
        assertTrue(methodIds.contains("melee-form-stepback-or-kite"));
        assertTrue(methodIds.contains("shielded-phase"));
        assertTrue(methodIds.contains("shield-skip"));

        assertTrue(strategy.getMechanics().stream().anyMatch(value -> value.contains("Ranged form")));
        assertTrue(strategy.getMechanics().stream().anyMatch(value -> value.contains("Melee form")));
        assertTrue(strategy.getMechanics().stream().anyMatch(value -> value.contains("prayer shield")));

        TaskData boss = ModularSlayerDataCompiler.compile(DATA_ROOT).stream()
            .filter(task -> "Boss".equals(task.getTask()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Boss task"));
        MonsterVariant muspah = boss.getVariants().stream()
            .filter(variant -> "Phantom Muspah".equals(variant.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing Phantom Muspah"));

        assertNotNull(muspah.getStrategy());
        assertEquals(CombatStyle.RANGED, muspah.getStrategy().getPrimaryStyle());
        assertEquals("Twisted bow", muspah.getStrategy().getPrimaryWeapons().get(0).getName());
        assertTrue(muspah.getStrategy().getNote().contains("Shielded"));
    }

    private static List<SourceMonsterVariant> bossVariants() throws IOException
    {
        try (Stream<Path> paths = Files.list(BOSS_DIR))
        {
            return paths
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted()
                .map(path -> {
                    try
                    {
                        return read(path, SourceMonsterVariant.class);
                    }
                    catch (IOException e)
                    {
                        throw new IllegalStateException("failed to read " + path, e);
                    }
                })
                .collect(Collectors.toList());
        }
    }

    private static <T> T read(Path path, Class<T> type) throws IOException
    {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return GSON.fromJson(reader, type);
        }
    }
}

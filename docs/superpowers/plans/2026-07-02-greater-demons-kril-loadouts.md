# Greater Demons K'ril Loadouts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add method-aware K'ril strategy loadout profiles so Greater demon tasks can recommend the Scorching bow solo path when owned while preserving melee fallback and existing strategy behavior.

**Architecture:** Add optional strategy profiles to the source and runtime strategy models, compile profile weapon IDs from the modular source graph, and let `LoadoutAdvisor` try authored profiles before the existing primary/secondary weapon fallback. Surface the chosen profile on `Recommendation` so the panel can explain the active K'ril loadout and select the recommendation's effective style.

**Tech Stack:** Java 11, Gson, Lombok `@Data`/`@NoArgsConstructor`, JUnit 4, Gradle, jq, RuneLite model/item IDs.

## Global Constraints

- Java targets release 11; tests use JUnit 4 and Mockito.
- Edit Slayer knowledge in `src/main/data/slayer`, not generated `build/` output.
- Treat `src/main/resources/data/slayer-data.json` as removed legacy data; do not recreate it.
- Preserve plugin compliance: no automation, no game actions, no input generation, no live prayer/tile prompts, no runtime HTTP data collection.
- Keep source IDs stable, lowercase, and hyphenated. Cross-file references use IDs, not display names.
- Use ASCII in project docs and source data unless the existing file already needs non-ASCII content.
- Data-only changes need focused source coverage plus `./gradlew test --tests 'com.danieljglover.allinslayer.data.source.*'`.
- Shared compiler, runtime model, or plugin behavior changes need `./gradlew cleanTest test` before claiming completion.
- Do not revert unrelated dirty files under `Agents/`, `docs/full-review/`, `docs/adr/`, or `scratchpad/`.

---

## File Structure

- Create `src/main/java/com/danieljglover/allinslayer/model/StrategyProfile.java`: runtime profile model nested under `MonsterStrategy`.
- Modify `src/main/java/com/danieljglover/allinslayer/model/MonsterStrategy.java`: add optional `profiles`.
- Create `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategyProfile.java`: source DTO for `plugin.profiles[]`.
- Modify `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategy.java`: add optional legacy root `profiles`.
- Modify `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategyPlugin.java`: add preferred nested `profiles`.
- Modify `src/main/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompiler.java`: validate and compile profile weapon references.
- Modify `src/main/java/com/danieljglover/allinslayer/loadout/Recommendation.java`: add profile label/note fields for UI.
- Modify `src/main/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisor.java`: resolve profile-driven style and weapon overrides before existing strategy fallback.
- Modify `src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java`: render active profile note and derive default method from `Recommendation.style`.
- Modify `src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json`: add K'ril runtime profiles in priority order.
- Modify tests:
  - `src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompilationTest.java`
  - `src/test/java/com/danieljglover/allinslayer/data/source/StrategyJsonSourceCoverageTest.java`
  - `src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java`
  - `src/test/java/com/danieljglover/allinslayer/ui/SlayerPanelTest.java`

---

### Task 1: Compile Strategy Profiles From Source

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/model/StrategyProfile.java`
- Create: `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategyProfile.java`
- Modify: `src/main/java/com/danieljglover/allinslayer/model/MonsterStrategy.java`
- Modify: `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategy.java`
- Modify: `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategyPlugin.java`
- Modify: `src/main/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompiler.java`
- Test: `src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompilationTest.java`

**Interfaces:**
- Produces: `StrategyProfile` with getters/setters for `profileId`, `label`, `combatStyle`, `role`, `weapons`, and `notes`.
- Produces: `MonsterStrategy#getProfiles(): List<StrategyProfile>`.
- Produces: compiler support for `plugin.profiles[].weapons[]` source weapon IDs resolved to runtime `StrategyWeapon` item IDs.

- [ ] **Step 1: Write the failing compiler test**

Add imports to `ModularSlayerDataCompilationTest.java`:

```java
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.StrategyProfile;
```

In `compilesStrategyFromJsonSourceWithPluginFields`, replace the K'ril strategy source string with this version:

```java
        write(root.resolve("strategies/k-ril-tsutsaroth/strategy.json"),
            "{\"strategyId\":\"k-ril-tsutsaroth\","
                + "\"variantIds\":[\"greater-demons-k-ril-tsutsaroth\"],"
                + "\"sourceUrl\":\"https://oldschool.runescape.wiki/w/K'ril_Tsutsaroth/Strategies\","
                + "\"plugin\":{\"primaryStyle\":\"MELEE\",\"primaryWeapons\":[\"emberlight\"],"
                + "\"secondaryWeapons\":[{\"weaponId\":\"scorching-bow\",\"style\":\"RANGED\"}],"
                + "\"profiles\":[{\"profileId\":\"solo-scorching-bow\","
                + "\"label\":\"Solo Scorching bow bind\",\"combatStyle\":\"RANGED\","
                + "\"role\":\"solo\",\"weapons\":[\"scorching-bow\"],"
                + "\"notes\":[\"Bind K'ril and kite counterclockwise.\"]}],"
                + "\"note\":\"Scorching bow bind method is a major solo option.\"},"
                + "\"methods\":[{\"methodId\":\"solo-scorching-bow\",\"label\":\"Solo Scorching bow\","
                + "\"combatStyle\":\"RANGED\",\"steps\":[\"Bind K'ril with Scorching bow.\"]}],"
                + "\"styleOptions\":[{\"styleId\":\"ranged\",\"label\":\"Ranged\",\"combatStyle\":\"RANGED\"}]}");
```

After the existing secondary weapon assertion in that test, add:

```java
        assertNotNull(kril.getStrategy().getProfiles());
        assertEquals(1, kril.getStrategy().getProfiles().size());
        StrategyProfile profile = kril.getStrategy().getProfiles().get(0);
        assertEquals("solo-scorching-bow", profile.getProfileId());
        assertEquals("Solo Scorching bow bind", profile.getLabel());
        assertEquals(CombatStyle.RANGED, profile.getCombatStyle());
        assertEquals("solo", profile.getRole());
        assertEquals(Integer.valueOf(29591), profile.getWeapons().get(0).getItemId());
        assertEquals(CombatStyle.RANGED, profile.getWeapons().get(0).getStyle());
        assertTrue(profile.getNotes().get(0).contains("counterclockwise"));
```

Add this test method near the existing validation tests:

```java
    @Test
    public void validateRejectsUnknownStrategyProfileWeapon() throws IOException
    {
        Path root = Files.createTempDirectory("modular-slayer-profile-missing-weapon");
        write(root.resolve("masters/duradel.json"),
            "{\"masterId\":\"duradel\",\"name\":\"Duradel\"}");
        write(root.resolve("weapons/emberlight.json"),
            "{\"weaponId\":\"emberlight\",\"name\":\"Emberlight\",\"itemIds\":[29589]}");
        write(root.resolve("tasks/greater-demons.json"),
            "{\"taskId\":\"greater-demons\",\"name\":\"Greater demons\",\"slayerTargetId\":222,"
                + "\"slayerLevel\":1,\"masterIds\":[\"duradel\"],"
                + "\"variantIds\":[\"greater-demons-k-ril-tsutsaroth\"],"
                + "\"defaultVariantId\":\"greater-demons-k-ril-tsutsaroth\",\"slayerHelmApplies\":true,"
                + "\"demon\":true}");
        write(root.resolve("monsters/greater-demons/k-ril-lvl650.json"),
            "{\"variantId\":\"greater-demons-k-ril-tsutsaroth\",\"name\":\"K'ril Tsutsaroth\","
                + "\"npcIds\":[3129],\"combatLevel\":650,\"demon\":true,\"boss\":true,"
                + "\"strategyId\":\"k-ril-tsutsaroth\","
                + "\"weakness\":{\"style\":\"MELEE\",\"element\":\"water\"},"
                + "\"monsterDefence\":{\"defenceLevel\":270,\"stab\":70,\"slash\":80,\"crush\":80,"
                + "\"magic\":80,\"range\":80}}");
        write(root.resolve("strategies/k-ril-tsutsaroth/strategy.json"),
            "{\"strategyId\":\"k-ril-tsutsaroth\","
                + "\"variantIds\":[\"greater-demons-k-ril-tsutsaroth\"],"
                + "\"plugin\":{\"primaryStyle\":\"MELEE\",\"primaryWeapons\":[\"emberlight\"],"
                + "\"profiles\":[{\"profileId\":\"solo-scorching-bow\","
                + "\"label\":\"Solo Scorching bow bind\",\"combatStyle\":\"RANGED\","
                + "\"role\":\"solo\",\"weapons\":[\"missing-bow\"]}]}}");

        assertValidationFails(root, "strategy k-ril-tsutsaroth references unknown weaponId: missing-bow");
    }
```

- [ ] **Step 2: Run the compiler test to verify it fails**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataCompilationTest
```

Expected: FAIL during compilation with missing symbols for `StrategyProfile`, `getProfiles`, or `setProfiles`.

- [ ] **Step 3: Add runtime and source profile DTOs**

Create `src/main/java/com/danieljglover/allinslayer/model/StrategyProfile.java`:

```java
package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One runtime strategy loadout path from a wiki strategy. Profiles are optional and sit below
 * {@link MonsterStrategy}: rich boss strategies can choose between method-specific weapon orders
 * without replacing the stat-driven slot picker for the rest of the loadout.
 */
@Data
@NoArgsConstructor
public class StrategyProfile
{
    private String profileId;
    private String label;
    private CombatStyle combatStyle;
    private String role;
    private List<StrategyWeapon> weapons;
    private List<String> notes;
}
```

Create `src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategyProfile.java`:

```java
package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.CombatStyle;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceStrategyProfile
{
    private String profileId;
    private String label;
    private CombatStyle combatStyle;
    private String role;
    private List<String> weapons;
    private List<String> notes;
}
```

- [ ] **Step 4: Add profile fields to source and runtime strategy models**

In `MonsterStrategy.java`, add the import and field:

```java
import java.util.List;
```

Keep the existing import and add this field after `sourceUrl`:

```java
    private List<StrategyProfile> profiles;          // optional method/loadout paths for rich strategies
```

In `SourceStrategy.java`, add:

```java
    private List<SourceStrategyProfile> profiles;
```

Place it after `private SourceStrategyPlugin plugin;`.

In `SourceStrategyPlugin.java`, add:

```java
    private List<SourceStrategyProfile> profiles;
```

Place it after `private List<SourceStrategyWeapon> secondaryWeapons;`.

- [ ] **Step 5: Compile profiles in `ModularSlayerDataCompiler`**

Add this import:

```java
import com.danieljglover.allinslayer.model.StrategyProfile;
```

In `validate`, inside the `for (SourceStrategy strategy : data.getStrategies())` loop and after secondary weapon validation, add:

```java
            validateProfiles(strategy, weaponIds, errors);
```

Add these helper methods near `validateWeaponRefs`:

```java
    private static void validateProfiles(SourceStrategy strategy, Set<String> weaponIds,
        List<String> errors)
    {
        List<SourceStrategyProfile> profiles = profiles(strategy);
        if (profiles == null)
        {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (SourceStrategyProfile profile : profiles)
        {
            if (profile == null)
            {
                continue;
            }
            String profileId = profile.getProfileId();
            if (profileId == null || profileId.trim().isEmpty())
            {
                errors.add("strategy " + strategy.getStrategyId() + " has profile with blank profileId");
            }
            else if (!seen.add(profileId))
            {
                errors.add("strategy " + strategy.getStrategyId()
                    + " has duplicate profileId: " + profileId);
            }
            if (profile.getCombatStyle() == null)
            {
                errors.add("strategy " + strategy.getStrategyId()
                    + " profile " + profileId + " has null combatStyle");
            }
            if (profile.getWeapons() == null || profile.getWeapons().isEmpty())
            {
                errors.add("strategy " + strategy.getStrategyId()
                    + " profile " + profileId + " has no weapons");
            }
            else
            {
                validateWeaponRefs(strategy.getStrategyId(), profile.getWeapons(), weaponIds, errors);
            }
        }
    }
```

In `resolveStrategy`, before `strategy.setNote(note(source));`, add:

```java
        strategy.setProfiles(resolveStrategyProfiles(profiles(source), weapons));
```

Add this helper near `secondaryWeapons`:

```java
    private static List<SourceStrategyProfile> profiles(SourceStrategy strategy)
    {
        return strategy.getPlugin() == null ? strategy.getProfiles() : strategy.getPlugin().getProfiles();
    }
```

Add this helper near `resolveStrategyWeapons`:

```java
    private static List<StrategyProfile> resolveStrategyProfiles(List<SourceStrategyProfile> source,
        Map<String, SourceWeapon> weapons)
    {
        if (source == null)
        {
            return null;
        }
        List<StrategyProfile> resolved = new ArrayList<>();
        for (SourceStrategyProfile row : source)
        {
            if (row == null)
            {
                continue;
            }
            StrategyProfile profile = new StrategyProfile();
            profile.setProfileId(row.getProfileId());
            profile.setLabel(row.getLabel());
            profile.setCombatStyle(row.getCombatStyle());
            profile.setRole(row.getRole());
            profile.setWeapons(resolveStrategyWeapons(row.getWeapons(), row.getCombatStyle(), weapons));
            profile.setNotes(row.getNotes());
            resolved.add(profile);
        }
        return resolved.isEmpty() ? null : resolved;
    }
```

- [ ] **Step 6: Run the compiler test to verify it passes**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.data.source.ModularSlayerDataCompilationTest
```

Expected: PASS.

- [ ] **Step 7: Commit Task 1**

```bash
git add src/main/java/com/danieljglover/allinslayer/model/StrategyProfile.java \
  src/main/java/com/danieljglover/allinslayer/model/MonsterStrategy.java \
  src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategyProfile.java \
  src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategy.java \
  src/main/java/com/danieljglover/allinslayer/data/source/SourceStrategyPlugin.java \
  src/main/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompiler.java \
  src/test/java/com/danieljglover/allinslayer/data/source/ModularSlayerDataCompilationTest.java
git commit -m "feat(data): compile strategy loadout profiles"
```

---

### Task 2: Make LoadoutAdvisor Select Strategy Profiles

**Files:**
- Modify: `src/main/java/com/danieljglover/allinslayer/loadout/Recommendation.java`
- Modify: `src/main/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisor.java`
- Test: `src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java`

**Interfaces:**
- Consumes: `MonsterStrategy#getProfiles()` from Task 1.
- Produces: `Recommendation#getStrategyProfileLabel()` and `Recommendation#getStrategyProfileNote()`.
- Produces: default profile selection that tries authored profiles in order when `selectedMethod == null`, and filters profiles by `selectedMethod` when the user explicitly selects a style.

- [ ] **Step 1: Write the failing advisor tests**

Add this import to `LoadoutAdvisorTest.java`:

```java
import com.danieljglover.allinslayer.model.StrategyProfile;
```

Add this helper near `demonStrategy()`:

```java
    private static com.danieljglover.allinslayer.model.MonsterStrategy krilProfileStrategy()
    {
        com.danieljglover.allinslayer.model.MonsterStrategy s =
            new com.danieljglover.allinslayer.model.MonsterStrategy();
        s.setPrimaryStyle(CombatStyle.MELEE);
        s.setPrimaryWeapons(Arrays.asList(
            new com.danieljglover.allinslayer.model.StrategyWeapon("Emberlight", STRAT_MELEE, null),
            new com.danieljglover.allinslayer.model.StrategyWeapon("Osmumten's fang", BETTER_MELEE, null)));
        s.setSecondaryWeapons(Collections.singletonList(
            new com.danieljglover.allinslayer.model.StrategyWeapon(
                "Scorching bow", STRAT_RANGED, CombatStyle.RANGED)));

        StrategyProfile ranged = new StrategyProfile();
        ranged.setProfileId("solo-scorching-bow");
        ranged.setLabel("Solo Scorching bow bind");
        ranged.setCombatStyle(CombatStyle.RANGED);
        ranged.setRole("solo");
        ranged.setWeapons(Collections.singletonList(
            new com.danieljglover.allinslayer.model.StrategyWeapon(
                "Scorching bow", STRAT_RANGED, CombatStyle.RANGED)));
        ranged.setNotes(Collections.singletonList("Bind K'ril and kite counterclockwise."));

        StrategyProfile melee = new StrategyProfile();
        melee.setProfileId("solo-melee");
        melee.setLabel("Solo melee");
        melee.setCombatStyle(CombatStyle.MELEE);
        melee.setRole("solo");
        melee.setWeapons(Arrays.asList(
            new com.danieljglover.allinslayer.model.StrategyWeapon("Emberlight", STRAT_MELEE, CombatStyle.MELEE),
            new com.danieljglover.allinslayer.model.StrategyWeapon("Osmumten's fang", BETTER_MELEE, CombatStyle.MELEE)));
        melee.setNotes(Collections.singletonList("Use demonbane melee or Fang when Scorching bow is unavailable."));

        s.setProfiles(Arrays.asList(ranged, melee));
        return s;
    }
```

Add this helper near `strategyVariant(...)`:

```java
    private static MonsterVariant krilVariant(com.danieljglover.allinslayer.model.MonsterStrategy s)
    {
        MonsterVariant v = variant("K'ril Tsutsaroth", CombatStyle.MELEE, true, true);
        v.setDemon(true);
        v.setStrategy(s);
        return v;
    }
```

Add these tests after `strategyOverridesEveryDocumentedStyle`:

```java
    @Test
    public void strategyProfileCanDefaultKrilToScorchingBowWhenOwned()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(80, 80, 4));
        table.put(BETTER_MELEE, meleeW(150, 150, 4));
        table.put(STRAT_RANGED, rangedW(60, 60, 5));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(STRAT_MELEE, BETTER_MELEE, STRAT_RANGED));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(krilVariant(krilProfileStrategy())));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "K'ril Tsutsaroth", null);

        assertEquals("default profile order picks Scorching bow", CombatStyle.RANGED, rec.get().getStyle());
        assertEquals(Integer.valueOf(STRAT_RANGED), rec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertEquals("Solo Scorching bow bind", rec.get().getStrategyProfileLabel());
        assertTrue(rec.get().getStrategyProfileNote().contains("counterclockwise"));
    }

    @Test
    public void strategyProfileFallsBackToMeleeWhenScorchingBowNotOwned()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(80, 80, 4));
        table.put(BETTER_MELEE, meleeW(150, 150, 4));
        table.put(STRAT_RANGED, rangedW(60, 60, 5));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(STRAT_MELEE, BETTER_MELEE));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(krilVariant(krilProfileStrategy())));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "K'ril Tsutsaroth", null);

        assertEquals("unowned ranged profile falls through to melee profile",
            CombatStyle.MELEE, rec.get().getStyle());
        assertEquals(Integer.valueOf(STRAT_MELEE), rec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertEquals("Solo melee", rec.get().getStrategyProfileLabel());
    }

    @Test
    public void explicitMeleeMethodDoesNotSwitchToScorchingBowProfile()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(STRAT_MELEE, meleeW(80, 80, 4));
        table.put(BETTER_MELEE, meleeW(150, 150, 4));
        table.put(STRAT_RANGED, rangedW(60, 60, 5));
        EquipmentStatsProvider sp = table::get;
        OwnedItems owned = OwnedItems.fromCounts(counts(STRAT_MELEE, BETTER_MELEE, STRAT_RANGED));

        TaskData task = meleeTask();
        task.setVariants(Collections.singletonList(krilVariant(krilProfileStrategy())));
        Optional<Recommendation> rec = advisorReal(sp, id -> 1000, food(SHARK, 20))
            .recommend(task, owned, stats(), AdviceMode.DPS, false, null, "K'ril Tsutsaroth",
                CombatStyle.MELEE);

        assertEquals(CombatStyle.MELEE, rec.get().getStyle());
        assertEquals(Integer.valueOf(STRAT_MELEE), rec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertEquals("Solo melee", rec.get().getStrategyProfileLabel());
    }
```

- [ ] **Step 2: Run the advisor tests to verify they fail**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.loadout.LoadoutAdvisorTest
```

Expected: FAIL during compilation because `Recommendation` has no strategy profile accessors, or FAIL because the default K'ril recommendation remains `MELEE`.

- [ ] **Step 3: Add profile fields to `Recommendation`**

In `Recommendation.java`, add after `strategyNote`:

```java
    // Active method/loadout profile chosen from MonsterStrategy.profiles. Null when no profile drove
    // the loadout. UI renders it as a concise explanation above the generic wiki strategy note.
    private String strategyProfileLabel;
    private String strategyProfileNote;
```

- [ ] **Step 4: Add profile selection helpers to `LoadoutAdvisor`**

Add imports:

```java
import com.danieljglover.allinslayer.model.StrategyProfile;
```

Add this private class after the fields:

```java
    private static final class StrategyProfilePick
    {
        private final StrategyProfile profile;
        private final CombatStyle style;
        private final List<Integer> weaponIds;
        private final Map<EquipmentSlot, Integer> worn;
        private final SlayerLocation recommendedLocation;
        private final SlayerLocation effectiveLocation;

        private StrategyProfilePick(StrategyProfile profile, CombatStyle style, List<Integer> weaponIds,
            Map<EquipmentSlot, Integer> worn, SlayerLocation recommendedLocation,
            SlayerLocation effectiveLocation)
        {
            this.profile = profile;
            this.style = style;
            this.weaponIds = weaponIds;
            this.worn = worn;
            this.recommendedLocation = recommendedLocation;
            this.effectiveLocation = effectiveLocation;
        }
    }
```

Add these helpers near `strategyOverrideIds`:

```java
    private StrategyProfilePick strategyProfilePick(TaskData task, MonsterVariant variant,
        MonsterProfile profile, CombatStyle selectedMethod, OwnedItems owned, PlayerStats stats,
        AdviceMode mode, boolean haveCannon, String selectedLocationName)
    {
        MonsterStrategy strategy = variant == null ? null : variant.getStrategy();
        if (strategy == null || strategy.getProfiles() == null || strategy.getProfiles().isEmpty())
        {
            return null;
        }
        for (StrategyProfile candidate : strategy.getProfiles())
        {
            if (candidate == null || candidate.getCombatStyle() == null)
            {
                continue;
            }
            CombatStyle style = candidate.getCombatStyle();
            if (selectedMethod != null && selectedMethod != style)
            {
                continue;
            }
            List<Integer> ids = strategyProfileWeaponIds(candidate);
            if (ids.isEmpty())
            {
                continue;
            }
            SlayerLocation recommendedLocation = chooseLocation(task, variant, style, haveCannon);
            SlayerLocation effectiveLocation = findLocation(task, selectedLocationName)
                .orElse(recommendedLocation);
            boolean wilderness = effectiveLocation != null && effectiveLocation.isWilderness();
            Map<EquipmentSlot, Integer> worn = gearSelector.select(owned, profile, style, stats, mode,
                wilderness, ids);
            Integer weaponId = worn.get(EquipmentSlot.WEAPON);
            if (weaponId != null && ids.contains(weaponId))
            {
                return new StrategyProfilePick(candidate, style, ids, worn, recommendedLocation,
                    effectiveLocation);
            }
        }
        return null;
    }

    private static List<Integer> strategyProfileWeaponIds(StrategyProfile profile)
    {
        List<Integer> ids = new ArrayList<>();
        if (profile == null || profile.getWeapons() == null)
        {
            return ids;
        }
        addWeaponIds(ids, profile.getWeapons());
        return ids;
    }

    private static String composeStrategyProfileNote(StrategyProfile profile)
    {
        if (profile == null || profile.getLabel() == null || profile.getLabel().trim().isEmpty())
        {
            return null;
        }
        StringBuilder note = new StringBuilder("Strategy profile: ");
        note.append(profile.getLabel().trim()).append('.');
        if (profile.getNotes() != null)
        {
            for (String line : profile.getNotes())
            {
                if (line != null && !line.trim().isEmpty())
                {
                    note.append(' ').append(line.trim());
                }
            }
        }
        return note.toString();
    }
```

- [ ] **Step 5: Use profile selection in `recommend`**

In `recommend`, after `MonsterProfile profile = ...`, replace the default method block through the worn selection with this code:

```java
        StrategyProfilePick profilePick = strategyProfilePick(task, variant, profile, selectedMethod,
            owned, stats, mode, haveCannon, selectedLocationName);

        MonsterStrategy validStrategy = validStrategy(variant);
        CombatStyle defaultMethod = validStrategy != null
            ? validStrategy.getPrimaryStyle()
            : profile.recommendedStyle();
        CombatStyle style = profilePick != null
            ? profilePick.style
            : (selectedMethod != null ? selectedMethod : defaultMethod);
        if (style == null)
        {
            return Optional.empty(); // no recommended style and no method chosen -> not viable
        }

        SlayerLocation recommendedLocation = profilePick == null
            ? chooseLocation(task, variant, style, haveCannon)
            : profilePick.recommendedLocation;
        SlayerLocation effectiveLocation = profilePick == null
            ? findLocation(task, selectedLocationName).orElse(recommendedLocation)
            : profilePick.effectiveLocation;
        boolean wilderness = effectiveLocation != null && effectiveLocation.isWilderness();

        List<Integer> strategyWeaponIds = profilePick == null
            ? strategyOverrideIds(variant, style)
            : profilePick.weaponIds;

        Map<EquipmentSlot, Integer> worn = profilePick == null
            ? gearSelector.select(owned, profile, style, stats, mode, wilderness, strategyWeaponIds)
            : profilePick.worn;
        if (worn.isEmpty())
        {
            return Optional.empty(); // not viable -> TASK_WITHOUT_LOADOUT (plan section 6)
        }
```

In the `if (variant != null)` block after `rec.setStrategyNote(...)`, add:

```java
            if (profilePick != null)
            {
                rec.setStrategyProfileLabel(profilePick.profile.getLabel());
                rec.setStrategyProfileNote(composeStrategyProfileNote(profilePick.profile));
            }
```

- [ ] **Step 6: Run advisor tests to verify they pass**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.loadout.LoadoutAdvisorTest
```

Expected: PASS.

- [ ] **Step 7: Commit Task 2**

```bash
git add src/main/java/com/danieljglover/allinslayer/loadout/Recommendation.java \
  src/main/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisor.java \
  src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java
git commit -m "feat(loadout): select strategy loadout profiles"
```

---

### Task 3: Render Active Strategy Profile In The Panel

**Files:**
- Modify: `src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java`
- Test: `src/test/java/com/danieljglover/allinslayer/ui/SlayerPanelTest.java`

**Interfaces:**
- Consumes: `Recommendation#getStrategyProfileNote()` and `Recommendation#getStyle()` from Task 2.
- Produces: loadout note component named `loadout-strategy-profile-note`.
- Produces: method selector default derived from `Recommendation.style` when the user has not selected a method.

- [ ] **Step 1: Write failing panel tests**

In `SlayerPanelTest.java`, add this test after `strategyVariantShowsWikiStrategyNoteInTheLoadoutCard`:

```java
    @Test
    public void activeStrategyProfileRendersAboveGenericStrategyNote() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setStrategyProfileLabel("Solo Scorching bow bind");
        rec.setStrategyProfileNote("Strategy profile: Solo Scorching bow bind. Bind K'ril and kite.");
        rec.setStrategyNote("Wiki strategy: Emberlight (MELEE); also Scorching bow (RANGED).");

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        JTextArea profile = componentByName(panel, "loadout-strategy-profile-note", JTextArea.class);
        JTextArea generic = componentByName(panel, "loadout-strategy-note", JTextArea.class);
        assertTrue(profile.getText().contains("Solo Scorching bow bind"));
        assertTrue("generic note still renders", generic.getText().contains("Wiki strategy"));
    }
```

Add this test near the method selector tests:

```java
    @Test
    public void defaultMethodSelectorReflectsRecommendationStyle() throws Exception
    {
        SlayerPanel panel = panel();
        Recommendation rec = locatedRecommendation();
        rec.setStyle(CombatStyle.RANGED);

        runOnEdt(() -> panel.render(variantState(variantTask(), null, null, rec)));

        assertTrue("default method follows the recommendation style",
            methodButton(panel, "method-ranged").isSelected());
        assertFalse(methodButton(panel, "method-magic").isSelected());
    }
```

- [ ] **Step 2: Run panel tests to verify they fail**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.ui.SlayerPanelTest
```

Expected: FAIL because `loadout-strategy-profile-note` is missing or the selector still defaults from variant weakness.

- [ ] **Step 3: Render strategy profile notes**

In `SlayerPanel.java`, in `LoadoutSection.rebuild`, insert this block before the existing `rec.getStrategyNote()` block:

```java
            // Active strategy profile: the advisor sets this when a method-specific strategy path,
            // such as K'ril's Scorching bow solo profile, drove the weapon pick.
            if (rec.getStrategyProfileNote() != null && !rec.getStrategyProfileNote().trim().isEmpty())
            {
                addLeft(body, strategyProfileNote(rec));
                addLeft(body, Box.createVerticalStrut(SlayerTheme.SPACE_3));
            }
```

Add this helper before `strategyNote(Recommendation rec)`:

```java
    private static JTextArea strategyProfileNote(Recommendation rec)
    {
        JTextArea note = wrappingNote(rec.getStrategyProfileNote().trim(), SlayerTheme.TEXT_PRIMARY);
        note.setName("loadout-strategy-profile-note");
        return note;
    }
```

- [ ] **Step 4: Make the default method selector read `Recommendation.style`**

Replace `effectiveMethod(SlayerPanelState state)` with:

```java
    /** The effective method the engine geared for: the user's pick, else the recommendation style. */
    private static CombatStyle effectiveMethod(SlayerPanelState state)
    {
        if (state.getSelectedMethod() != null)
        {
            return state.getSelectedMethod();
        }
        Recommendation rec = state.getRecommendation();
        if (rec != null && rec.getStyle() != null)
        {
            return rec.getStyle();
        }
        return recommendedStyle(state);
    }
```

- [ ] **Step 5: Run panel tests to verify they pass**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.ui.SlayerPanelTest
```

Expected: PASS.

- [ ] **Step 6: Commit Task 3**

```bash
git add src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java \
  src/test/java/com/danieljglover/allinslayer/ui/SlayerPanelTest.java
git commit -m "feat(ui): show active strategy profile"
```

---

### Task 4: Author K'ril Profiles In Source Data

**Files:**
- Modify: `src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json`
- Modify: `src/test/java/com/danieljglover/allinslayer/data/source/StrategyJsonSourceCoverageTest.java`
- Modify: `src/test/java/com/danieljglover/allinslayer/data/source/GreaterDemonsSourceCoverageTest.java`

**Interfaces:**
- Consumes: `plugin.profiles[]` from Task 1.
- Produces: K'ril profile order `solo-scorching-bow`, `solo-melee`, `melee-tank`, `team-attacker`, `solo-shadow-5-0`.

- [ ] **Step 1: Write failing source coverage assertions**

In `StrategyJsonSourceCoverageTest.java`, add this import:

```java
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
```

In `krilStrategyIsJsonAndContainsAllWikiStrategyMethods`, after the style assertions, add:

```java
        assertEquals(Arrays.asList(
            "solo-scorching-bow",
            "solo-melee",
            "melee-tank",
            "team-attacker",
            "solo-shadow-5-0"), profileIds(strategy));
        assertContains(profileWeapons(strategy, "solo-scorching-bow"), "scorching-bow");
        assertContains(profileWeapons(strategy, "solo-melee"), "emberlight");
        assertContains(profileWeapons(strategy, "solo-melee"), "osmumten-s-fang");
        assertContains(profileWeapons(strategy, "melee-tank"), "zamorakian-hasta");
        assertContains(profileWeapons(strategy, "team-attacker"), "abyssal-tentacle");
        assertContains(profileWeapons(strategy, "solo-shadow-5-0"), "tumeken-s-shadow");
```

Add these helpers:

```java
    private static List<String> profileIds(SourceStrategy strategy)
    {
        return strategy.getPlugin().getProfiles().stream()
            .map(SourceStrategyProfile::getProfileId)
            .collect(Collectors.toList());
    }

    private static Set<String> profileWeapons(SourceStrategy strategy, String profileId)
    {
        return strategy.getPlugin().getProfiles().stream()
            .filter(profile -> profileId.equals(profile.getProfileId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing profile " + profileId))
            .getWeapons().stream()
            .collect(Collectors.toSet());
    }
```

In `GreaterDemonsSourceCoverageTest.java`, add this test:

```java
    @Test
    public void krilProfileSourceMatchesGreaterDemonBossUse() throws IOException
    {
        SourceStrategy strategy = read(Paths.get("src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json"),
            SourceStrategy.class);

        assertContains(strategy.getVariantIds().stream().collect(Collectors.toSet()),
            "greater-demons-k-ril-tsutsaroth");
        assertTrue(strategy.getPlugin().getProfiles().stream()
            .anyMatch(profile -> "solo-scorching-bow".equals(profile.getProfileId())
                && profile.getCombatStyle() == com.danieljglover.allinslayer.model.CombatStyle.RANGED
                && profile.getWeapons().contains("scorching-bow")));
    }
```

- [ ] **Step 2: Run source coverage tests to verify they fail**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.data.source.StrategyJsonSourceCoverageTest --tests com.danieljglover.allinslayer.data.source.GreaterDemonsSourceCoverageTest
```

Expected: FAIL because `plugin.profiles` is absent from `k-ril-tsutsaroth/strategy.json`.

- [ ] **Step 3: Add K'ril profiles to source JSON**

In `src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json`, add this `profiles` array inside the existing `plugin` object after `secondaryWeapons` and before `note`:

```json
    "profiles": [
      {
        "profileId": "solo-scorching-bow",
        "label": "Solo Scorching bow bind",
        "combatStyle": "RANGED",
        "role": "solo",
        "weapons": [
          "scorching-bow",
          "twisted-bow",
          "bow-of-faerdhinen"
        ],
        "notes": [
          "Use Scorching bow special attacks with Lightbearer to bind K'ril and kite counterclockwise.",
          "Pray Protect from Missiles and heal from minions with Blood Barrage between kills."
        ]
      },
      {
        "profileId": "solo-melee",
        "label": "Solo melee",
        "combatStyle": "MELEE",
        "role": "solo",
        "weapons": [
          "emberlight",
          "osmumten-s-fang",
          "arclight"
        ],
        "notes": [
          "Use high magic defence and Protect from Melee, keeping enough supplies for Prayer Smash and poison."
        ]
      },
      {
        "profileId": "melee-tank",
        "label": "Melee tank",
        "combatStyle": "MELEE",
        "role": "tank",
        "weapons": [
          "emberlight",
          "arclight",
          "zamorakian-hasta",
          "ghrazi-rapier",
          "scythe-of-vitur",
          "abyssal-tentacle"
        ],
        "notes": [
          "Tank with strong defensive gear; Protect from Magic avoids Prayer Smash but slows kills."
        ]
      },
      {
        "profileId": "team-attacker",
        "label": "Team attacker",
        "combatStyle": "MELEE",
        "role": "attacker",
        "weapons": [
          "emberlight",
          "arclight",
          "osmumten-s-fang",
          "ghrazi-rapier",
          "abyssal-tentacle"
        ],
        "notes": [
          "Wait for the tank to tag K'ril, then attack with offensive melee while praying against the most dangerous minion."
        ]
      },
      {
        "profileId": "solo-shadow-5-0",
        "label": "Solo Tumeken's shadow 5:0",
        "combatStyle": "MAGIC",
        "role": "solo",
        "weapons": [
          "tumeken-s-shadow"
        ],
        "notes": [
          "Use Tumeken's shadow with 5:0 movement; this is high effort and should not beat owned Scorching bow or melee defaults."
        ]
      }
    ],
```

- [ ] **Step 4: Validate changed JSON**

Run:

```bash
jq empty src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json
```

Expected: no output and exit code 0.

- [ ] **Step 5: Run source coverage tests to verify they pass**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.data.source.StrategyJsonSourceCoverageTest --tests com.danieljglover.allinslayer.data.source.GreaterDemonsSourceCoverageTest
```

Expected: PASS.

- [ ] **Step 6: Commit Task 4**

```bash
git add src/main/data/slayer/strategies/k-ril-tsutsaroth/strategy.json \
  src/test/java/com/danieljglover/allinslayer/data/source/StrategyJsonSourceCoverageTest.java \
  src/test/java/com/danieljglover/allinslayer/data/source/GreaterDemonsSourceCoverageTest.java
git commit -m "data: add kril strategy loadout profiles"
```

---

### Task 5: Prove Real K'ril Data Uses Profiles End To End

**Files:**
- Modify: `src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java`
- Runtime generated resource through Gradle task: `build/generated/resources/slayer/data/slayer-data.json`

**Interfaces:**
- Consumes: compiled K'ril profile data from Task 4.
- Produces: regression tests using real bundled Slayer data, not synthetic models.

- [ ] **Step 1: Write failing real-data K'ril tests**

In `LoadoutAdvisorTest.java`, add this test after `tormentedDemonOnRealDataRecommendsEmberlightNotFang`:

```java
    @Test
    public void krilOnRealDataDefaultsToScorchingBowProfileWhenOwned()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(EMBERLIGHT, meleeW(80, 80, 4));
        table.put(FANG, meleeW(150, 150, 4));
        table.put(SCORCHING_BOW, rangedW(60, 60, 5));
        EquipmentStatsProvider sp = table::get;
        LoadoutAdvisor advisor = advisorReal(sp, id -> 1000, food(SHARK, 20));
        TaskData task = realTaskWith("K'ril Tsutsaroth", null);

        Optional<Recommendation> rec = advisor.recommend(
            task, OwnedItems.fromCounts(counts(EMBERLIGHT, FANG, SCORCHING_BOW)), stats(),
            AdviceMode.DPS, false, null, "K'ril Tsutsaroth", null);

        assertEquals(CombatStyle.RANGED, rec.get().getStyle());
        assertEquals(Integer.valueOf(SCORCHING_BOW), rec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertEquals("Solo Scorching bow bind", rec.get().getStrategyProfileLabel());
    }

    @Test
    public void krilOnRealDataFallsBackToMeleeProfileWithoutScorchingBow()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(EMBERLIGHT, meleeW(80, 80, 4));
        table.put(FANG, meleeW(150, 150, 4));
        table.put(SCORCHING_BOW, rangedW(60, 60, 5));
        EquipmentStatsProvider sp = table::get;
        LoadoutAdvisor advisor = advisorReal(sp, id -> 1000, food(SHARK, 20));
        TaskData task = realTaskWith("K'ril Tsutsaroth", null);

        Optional<Recommendation> rec = advisor.recommend(
            task, OwnedItems.fromCounts(counts(EMBERLIGHT, FANG)), stats(), AdviceMode.DPS,
            false, null, "K'ril Tsutsaroth", null);

        assertEquals(CombatStyle.MELEE, rec.get().getStyle());
        assertEquals(Integer.valueOf(EMBERLIGHT), rec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertEquals("Solo melee", rec.get().getStrategyProfileLabel());
    }
```

- [ ] **Step 2: Run real-data tests to verify they fail before generation**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.loadout.LoadoutAdvisorTest
```

Expected: FAIL if the generated Slayer data does not yet include profiles, or PASS only if Task 4's source data is already being compiled into test resources by Gradle. If it passes, continue to Step 4.

- [ ] **Step 3: Regenerate Slayer data**

Run:

```bash
./gradlew generateSlayerData
```

Expected: task succeeds and refreshes generated resources under `build/generated/resources/slayer/`.

- [ ] **Step 4: Run real-data tests to verify they pass**

Run:

```bash
./gradlew test --tests com.danieljglover.allinslayer.loadout.LoadoutAdvisorTest
```

Expected: PASS.

- [ ] **Step 5: Commit Task 5**

```bash
git add src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java
git commit -m "test(loadout): pin kril profile recommendation"
```

Do not add generated `build/` files to the commit.

---

### Task 6: Final Verification

**Files:**
- No source edits unless a verification failure identifies a defect.

**Interfaces:**
- Consumes: all previous task commits.
- Produces: verified implementation evidence.

- [ ] **Step 1: Validate all Slayer JSON**

Run:

```bash
find src/main/data/slayer -type f -name '*.json' -print0 | xargs -0 -n1 jq empty
```

Expected: no output and exit code 0.

- [ ] **Step 2: Run source suite**

Run:

```bash
./gradlew test --tests 'com.danieljglover.allinslayer.data.source.*'
```

Expected: PASS.

- [ ] **Step 3: Run full verification**

Run:

```bash
./gradlew cleanTest test
```

Expected: PASS.

- [ ] **Step 4: Inspect worktree**

Run:

```bash
git status --short
```

Expected: only unrelated pre-existing dirty files remain, or a clean tree if those were handled separately. No generated `build/` files should be staged.

- [ ] **Step 5: Handle verification failures**

If Step 1, 2, or 3 fails because of this implementation, stop and return to the task that introduced
the failing behavior. Add or adjust that task's failing test first, make the minimal implementation
change, rerun the failing command, and use that task's commit step. If all commands pass without edits,
do not create an empty commit.

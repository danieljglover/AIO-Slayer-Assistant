# All-In Slayer v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a working, Plugin-Hub-compliant RuneLite "All-In Slayer" plugin that detects the player's current Slayer task and shows, in a side panel + overlay, a bank-aware recommended loadout (DPS vs cost) plus task intel (level, weakness, locations, method, required items) — proven end-to-end on Duradel's task set.

**Architecture:** A read-only advisor. Static task knowledge is bundled as a reviewable JSON resource and loaded by `SlayerDataService`. `TaskDetector` reads Slayer varbits/varplayers to resolve the current `TaskData`. `InventoryService` snapshots inventory/equipment/bank into an `OwnedItems` set. `LoadoutAdvisor` combines the task's curated per-style gear options with what the player owns, choosing a style via a light `DpsEstimator` and items per slot by DPS-rank (DPS mode) or GE price (cost mode). UI (`SlayerPanel`, `SlayerOverlay`) renders the result; `InventorySetupsExporter` emits an Inventory Setups import string to the clipboard. Core logic operates on plain data structures so it is unit-testable without mocking the RuneLite client.

**Tech Stack:** Java 11, Gradle 8.10, RuneLite external-plugin API (`net.runelite:client:latest.release`), Lombok 1.18.30, Gson (RuneLite-provided), JUnit 4.12 + Mockito 3.12.4. Package root: `com.danieljglover.allinslayer`.

---

## Notes on the spec (minor corrections locked here)

- **Package name:** spec §5 wrote `net.danieljglover.slayer`; corrected to **`com.danieljglover.allinslayer`** (correct reverse-domain for `danieljglover.com`, and matches the "All-In Slayer" name). All tasks below use this.
- **Inventory Setups export** is via the **clipboard import string** (confirmed JSON shape `{"setup":{"inv":[],"eq":[],"rp":[],"name":..},"layout":[]}`) — no hard dependency on the Inventory Setups plugin being installed.
- **Data pipeline** (automated seeding from osrs-tools / Bucket API) is a **separate future plan**. This plan ships a **hand-curated, validated Duradel dataset** (Task 11). The schema is identical, so the future pipeline can regenerate the same file.
- **gameval IDs:** referenced by value with the gameval name in a comment, and defined as local `static final int` constants, so the build does not break if a generated constant name shifts between RuneLite releases.

---

## Shared Contracts (types & signatures used across tasks)

These are defined in Task 2 (model) and Tasks 5–8 (services). Listed here so every later task uses identical names. **Do not rename these.**

**Enums**
- `CombatStyle { MELEE, RANGED, MAGIC }`
- `EquipmentSlot { HEAD, CAPE, AMULET, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING }`

**Model (immutable POJOs, package `com.danieljglover.allinslayer.model`)**
- `Weakness { CombatStyle style; String element; }` (fields may be null if unknown)
- `MonsterDefence { int defenceLevel, stab, slash, crush, magic, range; }`
- `SlayerLocation { String name; boolean multi, cannon, burst, konarLockable; }`
- `StyleLoadout { CombatStyle style; Map<EquipmentSlot,List<Integer>> slotOptions; List<Integer> inventory; Integer spellMaxHit; }` (`spellMaxHit` non-null only for MAGIC)
- `TaskData { String task; int slayerLevel; List<String> questReqs; List<String> assignedBy; Map<String,int[]> amountByMaster; List<String> monsters; List<Integer> npcIds; Weakness weakness; MonsterDefence monsterDefence; boolean slayerHelmApplies; Integer requiredItemId; String requiredItemName; List<SlayerLocation> locations; Map<CombatStyle,StyleLoadout> loadouts; String recommendedMethod; }`

**Services / logic**
- `SlayerDataService` — `void load()`, `Optional<TaskData> byTaskName(String name)`, `Optional<TaskData> byTargetVarp(int slayerTargetId)`, `Collection<TaskData> all()`
- `record PlayerStats(int attack, int strength, int defence, int ranged, int magic, int slayer)` (use a Lombok `@Value` class; Java 11 has no `record`)
- `OwnedItems` — `boolean has(int itemId)`, `int quantity(int itemId)`, `Set<Integer> ids()`; static `OwnedItems.fromContainers(Item[] inv, Item[] worn, Item[] bank)`
- `Bonuses { int astab,aslash,acrush,amagic,arange, meleeStr, rangedStr, magicDmgPercent, attackSpeedTicks; }`
- `EquipmentStatsProvider` (interface) — `Bonuses get(int itemId)`
- `PriceService` (interface) — `int price(int itemId)`
- `DpsEstimator` — `double estimate(CombatStyle style, Map<EquipmentSlot,Integer> equipped, PlayerStats stats, TaskData task)`
- `LoadoutAdvisor` — `Optional<Recommendation> recommend(TaskData task, OwnedItems owned, PlayerStats stats, AdviceMode mode, boolean haveCannon)`
- `enum AdviceMode { DPS, COST }`
- `Recommendation { CombatStyle style; Map<EquipmentSlot,Integer> worn; List<Integer> inventory; SlayerLocation location; String method; double estimatedDps; long totalGearCost; List<Integer> missingUpgrades; }`
- `InventorySetupsExporter` — `String buildImportString(Recommendation rec, String setupName)`, `void copyToClipboard(String s)`

---

## Task 1: Project scaffolding (builds + loads in RuneLite)

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `runelite-plugin.properties`
- Create: `LICENSE` (BSD 2-Clause)
- Create: `.gitignore`
- Create: `src/main/java/com/danieljglover/allinslayer/AllInSlayerPlugin.java`
- Create: `src/main/java/com/danieljglover/allinslayer/AllInSlayerConfig.java`
- Create: `src/test/java/com/danieljglover/allinslayer/AllInSlayerPluginTest.java`

- [ ] **Step 1: Create `settings.gradle`**

```gradle
rootProject.name = 'all-in-slayer'
```

- [ ] **Step 2: Create `build.gradle`**

```gradle
plugins {
    id 'java'
}

repositories {
    mavenLocal()
    maven {
        url = 'https://repo.runelite.net'
    }
    mavenCentral()
}

def runeLiteVersion = 'latest.release'

dependencies {
    compileOnly group: 'net.runelite', name: 'client', version: runeLiteVersion

    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'

    testImplementation group: 'net.runelite', name: 'client', version: runeLiteVersion
    testImplementation group: 'net.runelite', name: 'jshell', version: runeLiteVersion
    testImplementation 'junit:junit:4.12'
    testImplementation 'org.mockito:mockito-core:3.12.4'
    testImplementation 'org.projectlombok:lombok:1.18.30'
    testAnnotationProcessor 'org.projectlombok:lombok:1.18.30'
}

group = 'com.danieljglover.allinslayer'
version = '1.0-SNAPSHOT'

tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
    options.release.set(11)
}

tasks.register('run', JavaExec) {
    dependsOn 'build'
    mainClass = 'com.danieljglover.allinslayer.AllInSlayerPluginTest'
    classpath = sourceSets.main.runtimeClasspath + sourceSets.test.runtimeClasspath
}
```

- [ ] **Step 3: Create `gradle/wrapper/gradle-wrapper.properties`**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-all.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 4: Create `runelite-plugin.properties`**

```properties
displayName=All-In Slayer
author=danieljglover
description=Bank-aware Slayer advisor: task intel and recommended loadouts (DPS vs cost)
tags=slayer,combat,pve,loadout,gear,task
plugins=com.danieljglover.allinslayer.AllInSlayerPlugin
```

- [ ] **Step 5: Create `LICENSE`** (BSD 2-Clause; Hub requirement)

```text
BSD 2-Clause License

Copyright (c) 2026, danieljglover
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

- [ ] **Step 6: Create `.gitignore`**

```gitignore
.gradle/
build/
*.iml
.idea/
out/
bin/
.DS_Store
```

- [ ] **Step 7: Create `AllInSlayerConfig.java`**

```java
package com.danieljglover.allinslayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AllInSlayerConfig.GROUP)
public interface AllInSlayerConfig extends Config
{
    String GROUP = "allinslayer";

    @ConfigItem(
        keyName = "haveCannon",
        name = "I own a cannon",
        description = "Allow cannon-based location/method recommendations where cannons are permitted",
        position = 1
    )
    default boolean haveCannon()
    {
        return false;
    }

    @ConfigItem(
        keyName = "adviceMode",
        name = "Loadout mode",
        description = "Whether the recommended loadout favours DPS or lowest cost",
        position = 2
    )
    default AdviceMode adviceMode()
    {
        return AdviceMode.DPS;
    }

    @ConfigItem(
        keyName = "showOverlay",
        name = "Show task overlay",
        description = "Show the compact in-game overlay for the current task",
        position = 3
    )
    default boolean showOverlay()
    {
        return true;
    }
}
```

- [ ] **Step 8: Create `AdviceMode.java`** (referenced by config + advisor)

```java
package com.danieljglover.allinslayer;

public enum AdviceMode
{
    DPS,
    COST
}
```

- [ ] **Step 9: Create a minimal `AllInSlayerPlugin.java`** (empty but valid; services wired in Task 10)

```java
package com.danieljglover.allinslayer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
    name = "All-In Slayer",
    description = "Bank-aware Slayer advisor: task intel and recommended loadouts",
    tags = {"slayer", "combat", "pve", "loadout", "gear", "task"}
)
public class AllInSlayerPlugin extends Plugin
{
    @Inject
    private AllInSlayerConfig config;

    @Override
    protected void startUp()
    {
        log.debug("All-In Slayer started");
    }

    @Override
    protected void shutDown()
    {
        log.debug("All-In Slayer stopped");
    }

    @Provides
    AllInSlayerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AllInSlayerConfig.class);
    }
}
```

- [ ] **Step 10: Create the dev-runner `AllInSlayerPluginTest.java`**

```java
package com.danieljglover.allinslayer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AllInSlayerPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(AllInSlayerPlugin.class);
        RuneLite.main(args);
    }
}
```

- [ ] **Step 11: Build to verify scaffolding compiles**

Run: `./gradlew build` (Windows: `gradlew.bat build` once the wrapper jar is present; if absent run `gradle wrapper --gradle-version 8.10` first, or `gradle build`)
Expected: `BUILD SUCCESSFUL`. (First run downloads the RuneLite client + Gradle 8.10; allow time.)

- [ ] **Step 12: Commit**

```bash
git add .
git commit -m "feat: scaffold All-In Slayer external plugin (builds, loads, config)"
```

---

## Task 2: Data model classes

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/model/CombatStyle.java`
- Create: `src/main/java/com/danieljglover/allinslayer/model/EquipmentSlot.java`
- Create: `src/main/java/com/danieljglover/allinslayer/model/Weakness.java`
- Create: `src/main/java/com/danieljglover/allinslayer/model/MonsterDefence.java`
- Create: `src/main/java/com/danieljglover/allinslayer/model/SlayerLocation.java`
- Create: `src/main/java/com/danieljglover/allinslayer/model/StyleLoadout.java`
- Create: `src/main/java/com/danieljglover/allinslayer/model/TaskData.java`
- Test: `src/test/java/com/danieljglover/allinslayer/model/TaskDataJsonTest.java`

- [ ] **Step 1: Write the failing test** (Gson round-trips a TaskData with all fields)

```java
package com.danieljglover.allinslayer.model;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TaskDataJsonTest
{
    @Test
    public void roundTripsAllFields()
    {
        StyleLoadout magic = new StyleLoadout();
        magic.setStyle(CombatStyle.MAGIC);
        Map<EquipmentSlot, java.util.List<Integer>> opts = new EnumMap<>(EquipmentSlot.class);
        opts.put(EquipmentSlot.WEAPON, Arrays.asList(4675, 1387)); // ancient staff, staff of air
        magic.setSlotOptions(opts);
        magic.setInventory(Arrays.asList(560, 565)); // death, blood runes
        magic.setSpellMaxHit(30);

        TaskData t = new TaskData();
        t.setTask("Nechryael");
        t.setSlayerLevel(80);
        t.setQuestReqs(Collections.singletonList("Priest in Peril"));
        t.setAssignedBy(Arrays.asList("duradel", "nieve"));
        t.setMonsters(Arrays.asList("Nechryael", "Greater nechryael"));
        t.setWeakness(new Weakness(CombatStyle.MAGIC, "air"));
        t.setMonsterDefence(new MonsterDefence(115, 0, 0, 0, 0, 0));
        t.setSlayerHelmApplies(true);
        t.setLocations(Collections.singletonList(
            new SlayerLocation("Catacombs of Kourend", true, false, true, true)));
        Map<CombatStyle, StyleLoadout> loadouts = new EnumMap<>(CombatStyle.class);
        loadouts.put(CombatStyle.MAGIC, magic);
        t.setLoadouts(loadouts);
        t.setRecommendedMethod("Ice Barrage in Catacombs");

        Gson gson = new Gson();
        String json = gson.toJson(t);
        TaskData back = gson.fromJson(json, TaskData.class);

        assertEquals("Nechryael", back.getTask());
        assertEquals(80, back.getSlayerLevel());
        assertEquals(CombatStyle.MAGIC, back.getWeakness().getStyle());
        assertTrue(back.getLocations().get(0).isBurst());
        assertEquals(Integer.valueOf(30),
            back.getLoadouts().get(CombatStyle.MAGIC).getSpellMaxHit());
        assertEquals(Arrays.asList(4675, 1387),
            back.getLoadouts().get(CombatStyle.MAGIC).getSlotOptions().get(EquipmentSlot.WEAPON));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.model.TaskDataJsonTest`
Expected: COMPILE FAIL (model classes do not exist yet).

- [ ] **Step 3: Create the enums**

```java
// CombatStyle.java
package com.danieljglover.allinslayer.model;

public enum CombatStyle
{
    MELEE,
    RANGED,
    MAGIC
}
```

```java
// EquipmentSlot.java
package com.danieljglover.allinslayer.model;

public enum EquipmentSlot
{
    HEAD, CAPE, AMULET, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING
}
```

- [ ] **Step 4: Create the value classes** (Lombok `@Data`/`@NoArgsConstructor`/`@AllArgsConstructor` so Gson can construct + getters/setters exist)

```java
// Weakness.java
package com.danieljglover.allinslayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Weakness
{
    private CombatStyle style;  // nullable
    private String element;     // nullable: "air","water","earth","fire"
}
```

```java
// MonsterDefence.java
package com.danieljglover.allinslayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonsterDefence
{
    private int defenceLevel;
    private int stab;
    private int slash;
    private int crush;
    private int magic;
    private int range;
}
```

```java
// SlayerLocation.java
package com.danieljglover.allinslayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlayerLocation
{
    private String name;
    private boolean multi;
    private boolean cannon;
    private boolean burst;
    private boolean konarLockable;
}
```

```java
// StyleLoadout.java
package com.danieljglover.allinslayer.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StyleLoadout
{
    private CombatStyle style;
    private Map<EquipmentSlot, List<Integer>> slotOptions; // ordered BIS -> budget per slot
    private List<Integer> inventory;                       // suggested inventory item ids
    private Integer spellMaxHit;                            // non-null only for MAGIC
}
```

```java
// TaskData.java
package com.danieljglover.allinslayer.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskData
{
    private String task;
    private int slayerLevel;
    private List<String> questReqs;
    private List<String> assignedBy;
    private Map<String, int[]> amountByMaster;
    private List<String> monsters;
    private List<Integer> npcIds;
    private Weakness weakness;
    private MonsterDefence monsterDefence;
    private boolean slayerHelmApplies;
    private Integer requiredItemId;     // nullable
    private String requiredItemName;    // nullable
    private List<SlayerLocation> locations;
    private Map<CombatStyle, StyleLoadout> loadouts;
    private String recommendedMethod;
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `gradle test --tests com.danieljglover.allinslayer.model.TaskDataJsonTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/model src/test/java/com/danieljglover/allinslayer/model
git commit -m "feat: add Slayer task data model with Gson round-trip test"
```

---

## Task 3: SlayerDataService (loads bundled JSON)

**Files:**
- Create: `src/main/resources/data/slayer-data.json` (small sample for now; full Duradel set in Task 11)
- Create: `src/main/java/com/danieljglover/allinslayer/data/SlayerDataService.java`
- Test: `src/test/java/com/danieljglover/allinslayer/data/SlayerDataServiceTest.java`

- [ ] **Step 1: Create the sample resource `src/main/resources/data/slayer-data.json`**

```json
[
  {
    "task": "Abyssal demons",
    "slayerLevel": 85,
    "questReqs": [],
    "assignedBy": ["duradel", "nieve", "konar", "krystilia"],
    "amountByMaster": { "duradel": [130, 200] },
    "monsters": ["Abyssal demon", "Greater abyssal demon"],
    "npcIds": [415, 416],
    "weakness": { "style": "MELEE", "element": null },
    "monsterDefence": { "defenceLevel": 135, "stab": 20, "slash": 20, "crush": 20, "magic": 0, "range": 20 },
    "slayerHelmApplies": true,
    "requiredItemId": null,
    "requiredItemName": null,
    "locations": [
      { "name": "Catacombs of Kourend", "multi": true, "cannon": false, "burst": true, "konarLockable": true },
      { "name": "Slayer Tower", "multi": false, "cannon": false, "burst": false, "konarLockable": true }
    ],
    "loadouts": {
      "MELEE": {
        "style": "MELEE",
        "slotOptions": {
          "WEAPON": [22325, 1305],
          "HEAD": [21264, 4720],
          "BODY": [21295, 1127],
          "LEGS": [21304, 1079]
        },
        "inventory": [12695, 385, 2434],
        "spellMaxHit": null
      }
    },
    "recommendedMethod": "Melee in Catacombs (cannon banned); Slayer helm (i) on task"
  },
  {
    "task": "Smoke devils",
    "slayerLevel": 93,
    "questReqs": [],
    "assignedBy": ["duradel", "nieve", "konar"],
    "amountByMaster": { "duradel": [130, 200] },
    "monsters": ["Smoke devil", "Nuclear smoke devil"],
    "npcIds": [495, 496],
    "weakness": { "style": "MAGIC", "element": "air" },
    "monsterDefence": { "defenceLevel": 80, "stab": 0, "slash": 0, "crush": 0, "magic": 0, "range": 0 },
    "slayerHelmApplies": true,
    "requiredItemId": 4166,
    "requiredItemName": "Facemask",
    "locations": [
      { "name": "Smoke Devil Dungeon", "multi": true, "cannon": true, "burst": true, "konarLockable": false }
    ],
    "loadouts": {
      "MAGIC": {
        "style": "MAGIC",
        "slotOptions": {
          "WEAPON": [4675, 1387],
          "HEAD": [21264, 4720]
        },
        "inventory": [560, 565, 12695],
        "spellMaxHit": 30
      }
    },
    "recommendedMethod": "Ice Barrage stacks in Smoke Devil Dungeon; facemask required"
  }
]
```

- [ ] **Step 2: Write the failing test**

```java
package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
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
        assertTrue(service.all().size() >= 2);
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
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.data.SlayerDataServiceTest`
Expected: COMPILE FAIL (`SlayerDataService` does not exist).

- [ ] **Step 4: Implement `SlayerDataService`**

```java
package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SlayerDataService
{
    private static final String RESOURCE = "/data/slayer-data.json";

    private final Gson gson;
    private final Map<String, TaskData> byName = new LinkedHashMap<>();

    @Inject
    public SlayerDataService(Gson gson)
    {
        this.gson = gson;
    }

    public void load()
    {
        byName.clear();
        try (InputStream in = SlayerDataService.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                log.error("Slayer data resource not found: {}", RESOURCE);
                return;
            }
            List<TaskData> tasks = gson.fromJson(
                new InputStreamReader(in, StandardCharsets.UTF_8),
                new TypeToken<List<TaskData>>() {}.getType());
            if (tasks == null)
            {
                return;
            }
            for (TaskData t : tasks)
            {
                byName.put(t.getTask().toLowerCase(Locale.ROOT), t);
            }
            log.debug("Loaded {} slayer tasks", byName.size());
        }
        catch (IOException e)
        {
            log.error("Failed to load slayer data", e);
        }
    }

    public Optional<TaskData> byTaskName(String name)
    {
        if (name == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    public Collection<TaskData> all()
    {
        return Collections.unmodifiableCollection(byName.values());
    }
}
```

> Note: `byTargetVarp(int)` (from Shared Contracts) is added in Task 4, where the varp→task mapping is defined.

- [ ] **Step 5: Run test to verify it passes**

Run: `gradle test --tests com.danieljglover.allinslayer.data.SlayerDataServiceTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/data/slayer-data.json src/main/java/com/danieljglover/allinslayer/data src/test/java/com/danieljglover/allinslayer/data
git commit -m "feat: add SlayerDataService loading bundled task JSON"
```

---

## Task 4: TaskDetector (varbits -> current task)

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/task/SlayerVarbits.java`
- Create: `src/main/java/com/danieljglover/allinslayer/task/TaskDetector.java`
- Modify: `src/main/java/com/danieljglover/allinslayer/data/SlayerDataService.java` (add `byTargetVarp`)
- Test: `src/test/java/com/danieljglover/allinslayer/task/TaskDetectorTest.java`

**Approach:** The Slayer "target" varp (`SLAYER_TARGET`=395) holds a creature/type id, not a name. Rather than reimplement RuneLite's DB-table lookup, we map the small set of target ids we support to task names in our JSON via a `slayerTargetId` field — but to avoid bloating the model, Task 4 keys detection off the **task name** resolved by a tiny `targetId -> name` map seeded from our dataset's `npcIds`/known target ids. For v1 we resolve via a dedicated `targetVarpId` lookup table built from the dataset (each task lists its `slayerTargetId`). Add that one field.

- [ ] **Step 1: Add `slayerTargetId` to the model and sample data**

Modify `TaskData.java` — add field after `task`:

```java
    private int slayerTargetId; // SLAYER_TARGET varp value identifying this task (0 if unmapped)
```

Modify `src/main/resources/data/slayer-data.json` — add `"slayerTargetId"` to each task (Abyssal demons = 12, Smoke devils = 84; these are the in-game assignment ids — **verify against the live game/DB table in Task 11**):

```json
  { "task": "Abyssal demons", "slayerTargetId": 12, "slayerLevel": 85, ... }
  { "task": "Smoke devils", "slayerTargetId": 84, "slayerLevel": 93, ... }
```

- [ ] **Step 2: Add `byTargetVarp` to `SlayerDataService`** (and index it in `load()`)

In `SlayerDataService`, add a second index and method:

```java
    private final Map<Integer, TaskData> byTarget = new java.util.HashMap<>();

    // inside load(), within the for-loop after byName.put(...):
    if (t.getSlayerTargetId() > 0)
    {
        byTarget.put(t.getSlayerTargetId(), t);
    }

    // inside load(), at the start of the method after byName.clear():
    // byTarget.clear();

    public Optional<TaskData> byTargetVarp(int slayerTargetId)
    {
        return Optional.ofNullable(byTarget.get(slayerTargetId));
    }
```

- [ ] **Step 3: Create `SlayerVarbits.java`** (id constants)

```java
package com.danieljglover.allinslayer.task;

/**
 * Slayer varp/varbit ids (gameval names in comments). Defined locally so a generated-constant
 * rename across RuneLite releases cannot break the build.
 */
public final class SlayerVarbits
{
    private SlayerVarbits() {}

    public static final int SLAYER_TARGET = 395;          // VarPlayerID.SLAYER_TARGET
    public static final int SLAYER_COUNT = 394;           // VarPlayerID.SLAYER_COUNT
    public static final int SLAYER_AREA = 2096;           // VarPlayerID.SLAYER_AREA
    public static final int SLAYER_TARGET_BOSSID = 4723;  // VarbitID.SLAYER_TARGET_BOSSID
}
```

- [ ] **Step 4: Write the failing test** (mock `Client`)

```java
package com.danieljglover.allinslayer.task;

import com.danieljglover.allinslayer.data.SlayerDataService;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.util.Optional;
import net.runelite.api.Client;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskDetectorTest
{
    @Mock
    private Client client;

    private TaskDetector detector;

    @Before
    public void setUp()
    {
        SlayerDataService data = new SlayerDataService(new Gson());
        data.load();
        detector = new TaskDetector(client, data);
    }

    @Test
    public void resolvesCurrentTaskFromTargetVarp()
    {
        when(client.getVarpValue(SlayerVarbits.SLAYER_TARGET)).thenReturn(12); // abyssal demons
        when(client.getVarpValue(SlayerVarbits.SLAYER_COUNT)).thenReturn(74);

        Optional<TaskData> task = detector.resolveCurrentTask();

        assertTrue(task.isPresent());
        assertEquals("Abyssal demons", task.get().getTask());
        assertEquals(74, detector.getRemaining());
    }

    @Test
    public void noTaskWhenTargetZero()
    {
        when(client.getVarpValue(SlayerVarbits.SLAYER_TARGET)).thenReturn(0);
        assertFalse(detector.resolveCurrentTask().isPresent());
    }
}
```

- [ ] **Step 5: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.task.TaskDetectorTest`
Expected: COMPILE FAIL (`TaskDetector` missing).

- [ ] **Step 6: Implement `TaskDetector`**

```java
package com.danieljglover.allinslayer.task;

import com.danieljglover.allinslayer.data.SlayerDataService;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;

@Singleton
public class TaskDetector
{
    private final Client client;
    private final SlayerDataService data;

    @Getter
    private int remaining;

    @Inject
    public TaskDetector(Client client, SlayerDataService data)
    {
        this.client = client;
        this.data = data;
    }

    /** Reads the live slayer varps and resolves the current task (empty if none assigned). */
    public Optional<TaskData> resolveCurrentTask()
    {
        int targetId = client.getVarpValue(SlayerVarbits.SLAYER_TARGET);
        this.remaining = client.getVarpValue(SlayerVarbits.SLAYER_COUNT);
        if (targetId <= 0)
        {
            return Optional.empty();
        }
        return data.byTargetVarp(targetId);
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `gradle test --tests com.danieljglover.allinslayer.task.TaskDetectorTest`
Expected: PASS (2 tests).

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add TaskDetector resolving current task from slayer varps"
```

---

## Task 5: InventoryService + OwnedItems

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/bank/OwnedItems.java`
- Create: `src/main/java/com/danieljglover/allinslayer/bank/InventoryService.java`
- Test: `src/test/java/com/danieljglover/allinslayer/bank/OwnedItemsTest.java`
- Test: `src/test/java/com/danieljglover/allinslayer/bank/InventoryServiceTest.java`

- [ ] **Step 1: Write the failing `OwnedItemsTest`**

```java
package com.danieljglover.allinslayer.bank;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Item;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OwnedItemsTest
{
    @Test
    public void mergesCountsAcrossSources()
    {
        Map<Integer, Integer> inv = new HashMap<>();
        inv.put(560, 100); // death runes in inventory
        Map<Integer, Integer> bank = new HashMap<>();
        bank.put(560, 5000);
        bank.put(11865, 1); // slayer helm in bank

        OwnedItems owned = OwnedItems.fromCounts(merge(inv, bank));

        assertTrue(owned.has(560));
        assertTrue(owned.has(11865));
        assertEquals(5100, owned.quantity(560));
        assertFalse(owned.has(999999));
    }

    @Test
    public void fromContainersFiltersEmptyAndNullSlots()
    {
        Item a = mock(Item.class);
        when(a.getId()).thenReturn(11865);
        when(a.getQuantity()).thenReturn(1);
        Item empty = mock(Item.class);
        when(empty.getId()).thenReturn(-1);
        when(empty.getQuantity()).thenReturn(0);

        OwnedItems owned = OwnedItems.fromContainers(
            new Item[]{a, empty, null}, null, null);

        assertTrue(owned.has(11865));
        assertEquals(1, owned.ids().size());
    }

    private static Map<Integer, Integer> merge(Map<Integer, Integer> a, Map<Integer, Integer> b)
    {
        Map<Integer, Integer> m = new HashMap<>(a);
        b.forEach((k, v) -> m.merge(k, v, Integer::sum));
        return m;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.bank.OwnedItemsTest`
Expected: COMPILE FAIL (`OwnedItems` missing).

- [ ] **Step 3: Implement `OwnedItems`**

```java
package com.danieljglover.allinslayer.bank;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Item;

/** Immutable view of every item the player has across inventory, worn equipment, and last-seen bank. */
public final class OwnedItems
{
    private final Map<Integer, Integer> counts;

    private OwnedItems(Map<Integer, Integer> counts)
    {
        this.counts = counts;
    }

    public boolean has(int itemId)
    {
        return counts.getOrDefault(itemId, 0) > 0;
    }

    public int quantity(int itemId)
    {
        return counts.getOrDefault(itemId, 0);
    }

    public Set<Integer> ids()
    {
        return Collections.unmodifiableSet(counts.keySet());
    }

    public static OwnedItems fromCounts(Map<Integer, Integer> counts)
    {
        return new OwnedItems(new HashMap<>(counts));
    }

    public static OwnedItems fromContainers(Item[] inventory, Item[] worn, Item[] bank)
    {
        Map<Integer, Integer> m = new HashMap<>();
        addAll(m, inventory);
        addAll(m, worn);
        addAll(m, bank);
        return new OwnedItems(m);
    }

    private static void addAll(Map<Integer, Integer> m, Item[] items)
    {
        if (items == null)
        {
            return;
        }
        for (Item it : items)
        {
            if (it == null)
            {
                continue;
            }
            int id = it.getId();
            int q = it.getQuantity();
            if (id > 0 && q > 0)
            {
                m.merge(id, q, Integer::sum);
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle test --tests com.danieljglover.allinslayer.bank.OwnedItemsTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Write the failing `InventoryServiceTest`** (bank snapshot persists and merges with live inventory)

```java
package com.danieljglover.allinslayer.bank;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InventoryServiceTest
{
    @Mock private Client client;
    @Mock private ConfigManager configManager;
    @Mock private ItemContainer bankContainer;
    @Mock private ItemContainer invContainer;
    @Mock private ItemContainer wornContainer;

    private InventoryService service;

    @Before
    public void setUp()
    {
        service = new InventoryService(client, configManager, new Gson());
    }

    @Test
    public void persistedBankMergesWithLiveInventory()
    {
        // bank seen earlier: 1 slayer helm
        Item helm = mock(11865, 1);
        when(bankContainer.getItems()).thenReturn(new Item[]{helm});
        // simulate the snapshot the service would have written, returned by config
        service.onBankChanged(bankContainer);

        // capture what was written and feed it back on read
        String stored = "{\"11865\":1}";
        lenient().when(configManager.getRSProfileConfiguration(
            eq(InventoryService.GROUP), eq(InventoryService.BANK_SNAPSHOT_KEY)))
            .thenReturn(stored);

        // live inventory: 100 death runes
        Item runes = mock(560, 100);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(invContainer);
        when(invContainer.getItems()).thenReturn(new Item[]{runes});
        when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(wornContainer);
        when(wornContainer.getItems()).thenReturn(new Item[0]);

        OwnedItems owned = service.currentOwned();

        assertTrue(owned.has(11865)); // from persisted bank
        assertTrue(owned.has(560));   // from live inventory
    }

    private static Item mock(int id, int qty)
    {
        Item i = org.mockito.Mockito.mock(Item.class);
        when(i.getId()).thenReturn(id);
        when(i.getQuantity()).thenReturn(qty);
        return i;
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.bank.InventoryServiceTest`
Expected: COMPILE FAIL (`InventoryService` missing).

- [ ] **Step 7: Implement `InventoryService`**

```java
package com.danieljglover.allinslayer.bank;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.config.ConfigManager;

/** Tracks live inventory/equipment and persists a last-seen bank snapshot (bank is only readable while open). */
@Slf4j
@Singleton
public class InventoryService
{
    public static final String GROUP = "allinslayer";
    public static final String BANK_SNAPSHOT_KEY = "bankSnapshot";
    public static final String BANK_TS_KEY = "bankSnapshotTs";

    private final Client client;
    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public InventoryService(Client client, ConfigManager configManager, Gson gson)
    {
        this.client = client;
        this.configManager = configManager;
        this.gson = gson;
    }

    /** Call when the bank ItemContainer changes; serialises a {itemId: qty} snapshot to the RS profile. */
    public void onBankChanged(ItemContainer bank)
    {
        if (bank == null)
        {
            return;
        }
        Map<Integer, Integer> snapshot = new HashMap<>();
        for (Item it : bank.getItems())
        {
            if (it != null && it.getId() > 0 && it.getQuantity() > 0)
            {
                snapshot.merge(it.getId(), it.getQuantity(), Integer::sum);
            }
        }
        configManager.setRSProfileConfiguration(GROUP, BANK_SNAPSHOT_KEY, gson.toJson(snapshot));
        configManager.setRSProfileConfiguration(GROUP, BANK_TS_KEY, String.valueOf(System.currentTimeMillis()));
    }

    /** Live inventory + worn + last-seen bank, merged. */
    public OwnedItems currentOwned()
    {
        Map<Integer, Integer> counts = new HashMap<>();
        addContainer(counts, client.getItemContainer(InventoryID.INVENTORY));
        addContainer(counts, client.getItemContainer(InventoryID.EQUIPMENT));

        String snapshot = configManager.getRSProfileConfiguration(GROUP, BANK_SNAPSHOT_KEY);
        if (snapshot != null && !snapshot.isEmpty())
        {
            Map<Integer, Integer> bank = gson.fromJson(
                snapshot, new TypeToken<Map<Integer, Integer>>() {}.getType());
            if (bank != null)
            {
                bank.forEach((k, v) -> counts.merge(k, v, Integer::sum));
            }
        }
        return OwnedItems.fromCounts(counts);
    }

    /** Epoch millis the bank was last seen, or null. */
    public Long bankLastSeen()
    {
        String ts = configManager.getRSProfileConfiguration(GROUP, BANK_TS_KEY);
        return ts == null ? null : Long.parseLong(ts);
    }

    private void addContainer(Map<Integer, Integer> counts, ItemContainer c)
    {
        if (c == null)
        {
            return;
        }
        for (Item it : c.getItems())
        {
            if (it != null && it.getId() > 0 && it.getQuantity() > 0)
            {
                counts.merge(it.getId(), it.getQuantity(), Integer::sum);
            }
        }
    }
}
```

> Note: the test's `currentOwned()` path uses the **stored** snapshot string. Because `onBankChanged` writes via the mocked `ConfigManager` (which records nothing back automatically), the test stubs `getRSProfileConfiguration(...)` to return the equivalent JSON. This verifies the merge contract, which is the behaviour that matters.

- [ ] **Step 8: Run tests to verify they pass**

Run: `gradle test --tests com.danieljglover.allinslayer.bank.InventoryServiceTest`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/bank src/test/java/com/danieljglover/allinslayer/bank
git commit -m "feat: add OwnedItems + InventoryService with persisted bank snapshot"
```

---

## Task 6: PriceService, EquipmentStatsProvider, DpsEstimator

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/PlayerStats.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/Bonuses.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/EquipmentStatsProvider.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/DefaultEquipmentStatsProvider.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/PriceService.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/DefaultPriceService.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/DpsEstimator.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/DefaultDpsEstimator.java`
- Test: `src/test/java/com/danieljglover/allinslayer/loadout/DefaultDpsEstimatorTest.java`

- [ ] **Step 1: Create `PlayerStats` and `Bonuses` value classes**

```java
// PlayerStats.java
package com.danieljglover.allinslayer.loadout;

import lombok.Value;

@Value
public class PlayerStats
{
    int attack;
    int strength;
    int defence;
    int ranged;
    int magic;
    int slayer;
}
```

```java
// Bonuses.java
package com.danieljglover.allinslayer.loadout;

import lombok.Value;

@Value
public class Bonuses
{
    int astab;
    int aslash;
    int acrush;
    int amagic;
    int arange;
    int meleeStr;
    int rangedStr;
    int magicDmgPercent;
    int attackSpeedTicks;

    public static Bonuses zero()
    {
        return new Bonuses(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
```

- [ ] **Step 2: Create the provider/price interfaces**

```java
// EquipmentStatsProvider.java
package com.danieljglover.allinslayer.loadout;

public interface EquipmentStatsProvider
{
    Bonuses get(int itemId);
}
```

```java
// PriceService.java
package com.danieljglover.allinslayer.loadout;

public interface PriceService
{
    int price(int itemId);
}
```

- [ ] **Step 3: Create the RuneLite-backed implementations** (thin adapters; no unit test — verified in-client)

```java
// DefaultEquipmentStatsProvider.java
package com.danieljglover.allinslayer.loadout;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemEquipmentStats;

@Singleton
public class DefaultEquipmentStatsProvider implements EquipmentStatsProvider
{
    private final ItemManager itemManager;

    @Inject
    public DefaultEquipmentStatsProvider(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    @Override
    public Bonuses get(int itemId)
    {
        ItemStats stats = itemManager.getItemStats(itemId);
        if (stats == null || stats.getEquipment() == null)
        {
            return Bonuses.zero();
        }
        ItemEquipmentStats e = stats.getEquipment();
        return new Bonuses(
            e.getAstab(), e.getAslash(), e.getAcrush(), e.getAmagic(), e.getArange(),
            e.getStr(), e.getRstr(), e.getMdmg(), e.getAspeed() > 0 ? e.getAspeed() : 4);
    }
}
```

```java
// DefaultPriceService.java
package com.danieljglover.allinslayer.loadout;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;

@Singleton
public class DefaultPriceService implements PriceService
{
    private final ItemManager itemManager;

    @Inject
    public DefaultPriceService(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    @Override
    public int price(int itemId)
    {
        return itemManager.getItemPrice(itemId);
    }
}
```

- [ ] **Step 4: Create the `DpsEstimator` interface**

```java
// DpsEstimator.java
package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.Map;

public interface DpsEstimator
{
    double estimate(CombatStyle style, Map<EquipmentSlot, Integer> equipped, PlayerStats stats, TaskData task);
}
```

- [ ] **Step 5: Write the failing `DefaultDpsEstimatorTest`**

```java
package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.StyleLoadout;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class DefaultDpsEstimatorTest
{
    private TaskData meleeTask()
    {
        TaskData t = new TaskData();
        t.setSlayerHelmApplies(false);
        t.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        return t;
    }

    @Test
    public void higherStrengthGearGivesHigherMeleeDps()
    {
        // item 1 = strong weapon (str 80), item 2 = weak weapon (str 5); same speed
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(1, new Bonuses(70, 70, 70, 0, 0, 80, 0, 0, 4));
        table.put(2, new Bonuses(20, 20, 20, 0, 0, 5, 0, 0, 4));
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99);
        Map<EquipmentSlot, Integer> strong = new EnumMap<>(EquipmentSlot.class);
        strong.put(EquipmentSlot.WEAPON, 1);
        Map<EquipmentSlot, Integer> weak = new EnumMap<>(EquipmentSlot.class);
        weak.put(EquipmentSlot.WEAPON, 2);

        double dpsStrong = est.estimate(CombatStyle.MELEE, strong, stats, meleeTask());
        double dpsWeak = est.estimate(CombatStyle.MELEE, weak, stats, meleeTask());

        assertTrue("strong gear should out-dps weak gear", dpsStrong > dpsWeak);
    }

    @Test
    public void magicUsesSpellMaxHitAndScalesWithMagicDamage()
    {
        Map<Integer, Bonuses> table = new HashMap<>();
        table.put(10, new Bonuses(0, 0, 0, 60, 0, 0, 0, 0, 5));   // 0% magic dmg
        table.put(11, new Bonuses(0, 0, 0, 60, 0, 0, 0, 20, 5));  // +20% magic dmg
        EquipmentStatsProvider provider = table::get;
        DefaultDpsEstimator est = new DefaultDpsEstimator(provider);

        TaskData t = new TaskData();
        t.setMonsterDefence(new MonsterDefence(80, 0, 0, 0, 0, 0));
        StyleLoadout magic = new StyleLoadout();
        magic.setStyle(CombatStyle.MAGIC);
        magic.setSpellMaxHit(30);
        Map<CombatStyle, StyleLoadout> loadouts = new EnumMap<>(CombatStyle.class);
        loadouts.put(CombatStyle.MAGIC, magic);
        t.setLoadouts(loadouts);

        Map<EquipmentSlot, Integer> base = new EnumMap<>(EquipmentSlot.class);
        base.put(EquipmentSlot.WEAPON, 10);
        Map<EquipmentSlot, Integer> boosted = new EnumMap<>(EquipmentSlot.class);
        boosted.put(EquipmentSlot.WEAPON, 11);

        double dpsBase = est.estimate(CombatStyle.MAGIC, base, stats(), t);
        double dpsBoosted = est.estimate(CombatStyle.MAGIC, boosted, stats(), t);

        assertTrue(dpsBase > 0);
        assertTrue("magic dmg % should raise dps", dpsBoosted > dpsBase);
    }

    private PlayerStats stats()
    {
        return new PlayerStats(99, 99, 99, 99, 99, 99);
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.loadout.DefaultDpsEstimatorTest`
Expected: COMPILE FAIL (`DefaultDpsEstimator` missing).

- [ ] **Step 7: Implement `DefaultDpsEstimator`** (light estimate; orders owned options — not exact published DPS)

```java
package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.StyleLoadout;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultDpsEstimator implements DpsEstimator
{
    private final EquipmentStatsProvider provider;

    @Inject
    public DefaultDpsEstimator(EquipmentStatsProvider provider)
    {
        this.provider = provider;
    }

    @Override
    public double estimate(CombatStyle style, Map<EquipmentSlot, Integer> equipped, PlayerStats stats, TaskData task)
    {
        int astab = 0, aslash = 0, acrush = 0, amagic = 0, arange = 0;
        int meleeStr = 0, rangedStr = 0, magicDmg = 0, weaponSpeed = 4;
        for (Integer itemId : equipped.values())
        {
            Bonuses b = provider.get(itemId);
            if (b == null)
            {
                continue;
            }
            astab += b.getAstab();
            aslash += b.getAslash();
            acrush += b.getAcrush();
            amagic += b.getAmagic();
            arange += b.getArange();
            meleeStr += b.getMeleeStr();
            rangedStr += b.getRangedStr();
            magicDmg += b.getMagicDmgPercent();
            if (b.getAttackSpeedTicks() > 0)
            {
                weaponSpeed = b.getAttackSpeedTicks();
            }
        }

        double helm = task.isSlayerHelmApplies() ? (7.0 / 6.0) : 1.0;
        MonsterDefence def = task.getMonsterDefence();

        switch (style)
        {
            case MELEE:
            {
                int effStr = stats.getStrength() + 8;
                int effAtk = stats.getAttack() + 8;
                double maxHit = Math.floor(0.5 + effStr * (meleeStr + 64) / 640.0) * helm;
                int[][] pairs = {
                    {astab, def.getStab()}, {aslash, def.getSlash()}, {acrush, def.getCrush()}
                };
                double bestHc = 0;
                for (int[] p : pairs)
                {
                    double atkRoll = effAtk * (p[0] + 64) * helm;
                    double defRoll = (def.getDefenceLevel() + 9) * (p[1] + 64);
                    bestHc = Math.max(bestHc, hitChance(atkRoll, defRoll));
                }
                return dps(bestHc, maxHit, weaponSpeed);
            }
            case RANGED:
            {
                int effRng = stats.getRanged() + 8;
                double maxHit = Math.floor(0.5 + effRng * (rangedStr + 64) / 640.0) * helm;
                double atkRoll = effRng * (arange + 64) * helm;
                double defRoll = (def.getDefenceLevel() + 9) * (def.getRange() + 64);
                return dps(hitChance(atkRoll, defRoll), maxHit, weaponSpeed);
            }
            case MAGIC:
            {
                StyleLoadout ml = task.getLoadouts() == null ? null : task.getLoadouts().get(CombatStyle.MAGIC);
                int base = (ml != null && ml.getSpellMaxHit() != null) ? ml.getSpellMaxHit() : 0;
                if (base == 0)
                {
                    return 0;
                }
                double maxHit = base * (1 + magicDmg / 100.0) * helm;
                int effMag = stats.getMagic() + 8;
                double atkRoll = effMag * (amagic + 64) * helm;
                double defRoll = (def.getDefenceLevel() + 9) * (def.getMagic() + 64);
                return dps(hitChance(atkRoll, defRoll), maxHit, weaponSpeed);
            }
            default:
                return 0;
        }
    }

    private double hitChance(double atk, double def)
    {
        return atk > def ? 1 - (def + 2) / (2 * (atk + 1)) : atk / (2 * (def + 1));
    }

    private double dps(double hitChance, double maxHit, int speedTicks)
    {
        double avgHit = hitChance * (maxHit / 2.0);
        return avgHit / (speedTicks * 0.6);
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `gradle test --tests com.danieljglover.allinslayer.loadout.DefaultDpsEstimatorTest`
Expected: PASS (2 tests).

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/loadout src/test/java/com/danieljglover/allinslayer/loadout
git commit -m "feat: add light DPS estimator, price + equipment-stats services"
```

---

## Task 7: LoadoutAdvisor

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/Recommendation.java`
- Create: `src/main/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisor.java`
- Test: `src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java`

- [ ] **Step 1: Create `Recommendation`**

```java
package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.SlayerLocation;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Recommendation
{
    private CombatStyle style;
    private Map<EquipmentSlot, Integer> worn;
    private List<Integer> inventory;
    private SlayerLocation location;
    private String method;
    private double estimatedDps;
    private long totalGearCost;
    private List<Integer> missingUpgrades;
}
```

- [ ] **Step 2: Write the failing `LoadoutAdvisorTest`**

```java
package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.MonsterDefence;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.StyleLoadout;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoadoutAdvisorTest
{
    private static final int BIS = 100, BUDGET = 101;

    private TaskData meleeTaskWithWeaponOptions()
    {
        TaskData t = new TaskData();
        t.setMonsterDefence(new MonsterDefence(100, 10, 10, 10, 10, 10));
        StyleLoadout melee = new StyleLoadout();
        melee.setStyle(CombatStyle.MELEE);
        Map<EquipmentSlot, java.util.List<Integer>> opts = new EnumMap<>(EquipmentSlot.class);
        opts.put(EquipmentSlot.WEAPON, Arrays.asList(BIS, BUDGET)); // BIS first
        melee.setSlotOptions(opts);
        melee.setInventory(Collections.emptyList());
        Map<CombatStyle, StyleLoadout> loadouts = new EnumMap<>(CombatStyle.class);
        loadouts.put(CombatStyle.MELEE, melee);
        t.setLoadouts(loadouts);
        t.setLocations(Collections.singletonList(
            new SlayerLocation("Catacombs", true, false, true, false)));
        return t;
    }

    private LoadoutAdvisor advisor(PriceService prices)
    {
        // dps estimator that just rewards owning the BIS item id
        DpsEstimator est = (style, equipped, stats, task) ->
            equipped.containsValue(BIS) ? 10.0 : 1.0;
        return new LoadoutAdvisor(est, prices);
    }

    private PlayerStats stats()
    {
        return new PlayerStats(99, 99, 99, 99, 99, 99);
    }

    @Test
    public void dpsModePicksBisWhenOwned()
    {
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(BIS, 1);
        counts.put(BUDGET, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);

        Optional<Recommendation> rec = advisor(id -> 1000).recommend(
            meleeTaskWithWeaponOptions(), owned, stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertEquals(Integer.valueOf(BIS), rec.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void costModePicksCheaperOwnedItem()
    {
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(BIS, 1);
        counts.put(BUDGET, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);
        // BIS expensive, BUDGET cheap
        PriceService prices = id -> id == BIS ? 1_000_000 : 5_000;

        Optional<Recommendation> rec = advisor(prices).recommend(
            meleeTaskWithWeaponOptions(), owned, stats(), AdviceMode.COST, false);

        assertTrue(rec.isPresent());
        assertEquals(Integer.valueOf(BUDGET), rec.get().getWorn().get(EquipmentSlot.WEAPON));
    }

    @Test
    public void missingBisRecordedWhenOnlyBudgetOwned()
    {
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(BUDGET, 1);
        OwnedItems owned = OwnedItems.fromCounts(counts);

        Optional<Recommendation> rec = advisor(id -> 1000).recommend(
            meleeTaskWithWeaponOptions(), owned, stats(), AdviceMode.DPS, false);

        assertTrue(rec.isPresent());
        assertEquals(Integer.valueOf(BUDGET), rec.get().getWorn().get(EquipmentSlot.WEAPON));
        assertTrue(rec.get().getMissingUpgrades().contains(BIS));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.loadout.LoadoutAdvisorTest`
Expected: COMPILE FAIL (`LoadoutAdvisor` missing).

- [ ] **Step 4: Implement `LoadoutAdvisor`**

```java
package com.danieljglover.allinslayer.loadout;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.SlayerLocation;
import com.danieljglover.allinslayer.model.StyleLoadout;
import com.danieljglover.allinslayer.model.TaskData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LoadoutAdvisor
{
    private final DpsEstimator dpsEstimator;
    private final PriceService priceService;

    @Inject
    public LoadoutAdvisor(DpsEstimator dpsEstimator, PriceService priceService)
    {
        this.dpsEstimator = dpsEstimator;
        this.priceService = priceService;
    }

    public Optional<Recommendation> recommend(TaskData task, OwnedItems owned, PlayerStats stats,
        AdviceMode mode, boolean haveCannon)
    {
        if (task == null || task.getLoadouts() == null || task.getLoadouts().isEmpty())
        {
            return Optional.empty();
        }

        CombatStyle bestStyle = null;
        Map<EquipmentSlot, Integer> bestWorn = null;
        List<Integer> bestInv = null;
        List<Integer> bestMissing = null;
        double bestDps = -1;

        for (Map.Entry<CombatStyle, StyleLoadout> e : task.getLoadouts().entrySet())
        {
            StyleLoadout sl = e.getValue();
            Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
            List<Integer> missing = new ArrayList<>();
            if (sl.getSlotOptions() != null)
            {
                for (Map.Entry<EquipmentSlot, List<Integer>> so : sl.getSlotOptions().entrySet())
                {
                    Integer chosen = pickItem(so.getValue(), owned, mode);
                    if (chosen != null)
                    {
                        worn.put(so.getKey(), chosen);
                    }
                    else if (!so.getValue().isEmpty())
                    {
                        missing.add(so.getValue().get(0)); // BIS not owned
                    }
                }
            }
            double dps = dpsEstimator.estimate(e.getKey(), worn, stats, task);
            if (dps > bestDps)
            {
                bestDps = dps;
                bestStyle = e.getKey();
                bestWorn = worn;
                bestInv = sl.getInventory();
                bestMissing = missing;
            }
        }

        if (bestStyle == null)
        {
            return Optional.empty();
        }

        if (task.getRequiredItemId() != null && !owned.has(task.getRequiredItemId()))
        {
            bestMissing.add(task.getRequiredItemId());
        }

        SlayerLocation location = chooseLocation(task, bestStyle, haveCannon);

        long cost = 0;
        for (int id : bestWorn.values())
        {
            cost += Math.max(0, priceService.price(id));
        }

        Recommendation rec = new Recommendation();
        rec.setStyle(bestStyle);
        rec.setWorn(bestWorn);
        rec.setInventory(bestInv == null ? new ArrayList<>() : bestInv);
        rec.setLocation(location);
        rec.setMethod(task.getRecommendedMethod());
        rec.setEstimatedDps(bestDps);
        rec.setTotalGearCost(cost);
        rec.setMissingUpgrades(bestMissing);
        return Optional.of(rec);
    }

    private Integer pickItem(List<Integer> options, OwnedItems owned, AdviceMode mode)
    {
        List<Integer> ownedOptions = new ArrayList<>();
        for (int id : options)
        {
            if (owned.has(id))
            {
                ownedOptions.add(id);
            }
        }
        if (ownedOptions.isEmpty())
        {
            return null;
        }
        if (mode == AdviceMode.DPS)
        {
            return ownedOptions.get(0); // list is BIS -> budget; first owned is closest to BIS
        }
        return ownedOptions.stream()
            .min(Comparator.comparingInt(id ->
            {
                int p = priceService.price(id);
                return p <= 0 ? Integer.MAX_VALUE : p;
            }))
            .orElse(ownedOptions.get(0));
    }

    private SlayerLocation chooseLocation(TaskData task, CombatStyle style, boolean haveCannon)
    {
        if (task.getLocations() == null || task.getLocations().isEmpty())
        {
            return null;
        }
        SlayerLocation cannonLoc = null;
        SlayerLocation burstLoc = null;
        for (SlayerLocation l : task.getLocations())
        {
            if (haveCannon && l.isCannon() && cannonLoc == null)
            {
                cannonLoc = l;
            }
            if (l.isBurst() && burstLoc == null)
            {
                burstLoc = l;
            }
        }
        if (haveCannon && cannonLoc != null)
        {
            return cannonLoc;
        }
        if (style == CombatStyle.MAGIC && burstLoc != null)
        {
            return burstLoc;
        }
        return task.getLocations().get(0);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `gradle test --tests com.danieljglover.allinslayer.loadout.LoadoutAdvisorTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/loadout/Recommendation.java src/main/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisor.java src/test/java/com/danieljglover/allinslayer/loadout/LoadoutAdvisorTest.java
git commit -m "feat: add LoadoutAdvisor (style/slot selection, DPS vs cost, cannon-aware location)"
```

---

## Task 8: InventorySetupsExporter (clipboard import string)

**Files:**
- Create: `src/main/java/com/danieljglover/allinslayer/integration/InventorySetupsExporter.java`
- Test: `src/test/java/com/danieljglover/allinslayer/integration/InventorySetupsExporterTest.java`

**Format (confirmed from a real export):** `{"setup":{"inv":[{"id":..,"q":..}],"eq":[{"id":..}|null x14],"rp":[],"name":".."},"layout":[..]}`. The `eq` array is indexed by RuneLite equipment slot index: HEAD0 CAPE1 AMULET2 WEAPON3 BODY4 SHIELD5 LEGS7 HANDS9 FEET10 RING12 AMMO13.

- [ ] **Step 1: Write the failing test**

```java
package com.danieljglover.allinslayer.integration;

import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.CombatStyle;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InventorySetupsExporterTest
{
    @Test
    public void buildsImportStringWithWeaponInSlot3AndInventory()
    {
        Map<EquipmentSlot, Integer> worn = new EnumMap<>(EquipmentSlot.class);
        worn.put(EquipmentSlot.WEAPON, 4151); // abyssal whip -> eq index 3
        worn.put(EquipmentSlot.HEAD, 11865);  // slayer helm -> eq index 0

        Recommendation rec = new Recommendation();
        rec.setStyle(CombatStyle.MELEE);
        rec.setWorn(worn);
        rec.setInventory(Arrays.asList(12695, 385)); // super combat, shark

        String json = new InventorySetupsExporter().buildImportString(rec, "Abyssal demons");

        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonObject setup = root.getAsJsonObject("setup");
        assertEquals("Abyssal demons", setup.get("name").getAsString());

        JsonArray eq = setup.getAsJsonArray("eq");
        assertEquals(14, eq.size());
        assertEquals(4151, eq.get(3).getAsJsonObject().get("id").getAsInt());  // weapon
        assertEquals(11865, eq.get(0).getAsJsonObject().get("id").getAsInt()); // head
        assertTrue(eq.get(1).isJsonNull()); // cape empty

        JsonArray inv = setup.getAsJsonArray("inv");
        assertEquals(2, inv.size());
        assertEquals(12695, inv.get(0).getAsJsonObject().get("id").getAsInt());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests com.danieljglover.allinslayer.integration.InventorySetupsExporterTest`
Expected: COMPILE FAIL (`InventorySetupsExporter` missing).

- [ ] **Step 3: Implement `InventorySetupsExporter`**

```java
package com.danieljglover.allinslayer.integration;

import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Builds an Inventory Setups import string from a recommendation and copies it to the clipboard. */
@Slf4j
@Singleton
public class InventorySetupsExporter
{
    // RuneLite equipment slot indices used by Inventory Setups' "eq" array.
    private static final Map<EquipmentSlot, Integer> SLOT_INDEX = new EnumMap<>(EquipmentSlot.class);
    static
    {
        SLOT_INDEX.put(EquipmentSlot.HEAD, 0);
        SLOT_INDEX.put(EquipmentSlot.CAPE, 1);
        SLOT_INDEX.put(EquipmentSlot.AMULET, 2);
        SLOT_INDEX.put(EquipmentSlot.WEAPON, 3);
        SLOT_INDEX.put(EquipmentSlot.BODY, 4);
        SLOT_INDEX.put(EquipmentSlot.SHIELD, 5);
        SLOT_INDEX.put(EquipmentSlot.LEGS, 7);
        SLOT_INDEX.put(EquipmentSlot.HANDS, 9);
        SLOT_INDEX.put(EquipmentSlot.FEET, 10);
        SLOT_INDEX.put(EquipmentSlot.RING, 12);
        SLOT_INDEX.put(EquipmentSlot.AMMO, 13);
    }
    private static final int EQ_SIZE = 14;

    private final Gson gson = new Gson();

    public String buildImportString(Recommendation rec, String setupName)
    {
        JsonObject setup = new JsonObject();

        JsonArray eq = new JsonArray();
        JsonObject[] slots = new JsonObject[EQ_SIZE];
        if (rec.getWorn() != null)
        {
            for (Map.Entry<EquipmentSlot, Integer> e : rec.getWorn().entrySet())
            {
                Integer idx = SLOT_INDEX.get(e.getKey());
                if (idx != null)
                {
                    JsonObject item = new JsonObject();
                    item.addProperty("id", e.getValue());
                    slots[idx] = item;
                }
            }
        }
        for (int i = 0; i < EQ_SIZE; i++)
        {
            eq.add(slots[i] == null ? JsonNull.INSTANCE : slots[i]);
        }
        setup.add("eq", eq);

        JsonArray inv = new JsonArray();
        List<Integer> invItems = rec.getInventory();
        if (invItems != null)
        {
            for (Integer id : invItems)
            {
                JsonObject item = new JsonObject();
                item.addProperty("id", id);
                inv.add(item);
            }
        }
        setup.add("inv", inv);

        setup.add("rp", new JsonArray());     // rune pouch: not modelled in v1
        setup.addProperty("name", setupName);

        JsonObject root = new JsonObject();
        root.add("setup", setup);
        root.add("layout", new JsonArray());  // layout is optional; Inventory Setups recomputes it
        return gson.toJson(root);
    }

    public void copyToClipboard(String s)
    {
        try
        {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(s), null);
        }
        catch (HeadlessException | IllegalStateException ex)
        {
            log.warn("Could not copy setup to clipboard", ex);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle test --tests com.danieljglover.allinslayer.integration.InventorySetupsExporterTest`
Expected: PASS.

> Open item (verify in Task 9 manual check): confirm the installed Inventory Setups version accepts an empty `layout` and `rp`. If it rejects the import, populate `layout` with worn ids followed by inventory ids. The `setup` object shape is the part that must be exact.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/integration src/test/java/com/danieljglover/allinslayer/integration
git commit -m "feat: add Inventory Setups clipboard export"
```

---

## Task 9: UI — side panel + overlay

UI glue is verified in-client (no unit tests). The panel/overlay are passive renderers updated by the plugin (Task 10).

**Files:**
- Create: `src/main/resources/panel_icon.png` (a 24×24 PNG for the navigation button)
- Create: `src/main/java/com/danieljglover/allinslayer/ui/SlayerPanel.java`
- Create: `src/main/java/com/danieljglover/allinslayer/ui/SlayerOverlay.java`

- [ ] **Step 1: Add a 24×24 navigation icon** at `src/main/resources/panel_icon.png` (any simple Slayer-themed 24×24 PNG; can be a placeholder for now).

- [ ] **Step 2: Create `SlayerPanel`**

```java
package com.danieljglover.allinslayer.ui;

import com.danieljglover.allinslayer.AdviceMode;
import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.EquipmentSlot;
import com.danieljglover.allinslayer.model.TaskData;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Map;
import java.util.function.IntFunction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Setter;
import net.runelite.client.ui.PluginPanel;

public class SlayerPanel extends PluginPanel
{
    private final JLabel taskTitle = new JLabel("No slayer task detected");
    private final JLabel taskInfo = new JLabel();
    private final JLabel loadoutInfo = new JLabel();
    private final JButton modeToggle = new JButton("Mode: DPS");
    private final JButton exportButton = new JButton("Export to Inventory Setups");

    @Setter
    private Runnable onToggleMode;
    @Setter
    private Runnable onExport;

    public SlayerPanel()
    {
        setLayout(new BorderLayout());
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        taskTitle.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        taskInfo.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        loadoutInfo.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        modeToggle.addActionListener(e ->
        {
            if (onToggleMode != null)
            {
                onToggleMode.run();
            }
        });
        exportButton.addActionListener(e ->
        {
            if (onExport != null)
            {
                onExport.run();
            }
        });

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        buttons.add(modeToggle);
        buttons.add(exportButton);

        content.add(taskTitle);
        content.add(taskInfo);
        content.add(loadoutInfo);
        content.add(buttons);
        add(content, BorderLayout.NORTH);
        showNoTask();
    }

    public void showNoTask()
    {
        taskTitle.setText("No slayer task detected");
        taskInfo.setText("");
        loadoutInfo.setText("");
        exportButton.setEnabled(false);
    }

    /** Must be called on the Swing EDT. */
    public void update(TaskData task, Recommendation rec, AdviceMode mode, IntFunction<String> itemName, String bankAge)
    {
        modeToggle.setText("Mode: " + mode);
        if (task == null)
        {
            showNoTask();
            return;
        }
        taskTitle.setText("<html><b>" + task.getTask() + "</b></html>");
        StringBuilder info = new StringBuilder("<html>");
        info.append("Slayer level: ").append(task.getSlayerLevel()).append("<br>");
        if (task.getWeakness() != null && task.getWeakness().getStyle() != null)
        {
            info.append("Weakness: ").append(task.getWeakness().getStyle());
            if (task.getWeakness().getElement() != null)
            {
                info.append(" (").append(task.getWeakness().getElement()).append(")");
            }
            info.append("<br>");
        }
        if (task.getRequiredItemName() != null)
        {
            info.append("Required: ").append(task.getRequiredItemName()).append("<br>");
        }
        if (rec != null && rec.getLocation() != null)
        {
            info.append("Location: ").append(rec.getLocation().getName()).append("<br>");
        }
        if (rec != null && rec.getMethod() != null)
        {
            info.append("Method: ").append(rec.getMethod()).append("<br>");
        }
        info.append("</html>");
        taskInfo.setText(info.toString());

        if (rec == null)
        {
            loadoutInfo.setText("<html><i>No owned loadout found"
                + (bankAge != null ? " (bank " + bankAge + ")" : "") + "</i></html>");
            exportButton.setEnabled(false);
            return;
        }

        StringBuilder lo = new StringBuilder("<html>");
        lo.append("<b>Loadout (").append(rec.getStyle()).append(")</b><br>");
        if (rec.getWorn() != null)
        {
            for (Map.Entry<EquipmentSlot, Integer> e : rec.getWorn().entrySet())
            {
                lo.append(e.getKey()).append(": ").append(itemName.apply(e.getValue())).append("<br>");
            }
        }
        lo.append("Est. DPS: ").append(String.format("%.2f", rec.getEstimatedDps())).append("<br>");
        lo.append("Gear cost: ").append(String.format("%,d", rec.getTotalGearCost())).append(" gp<br>");
        if (rec.getMissingUpgrades() != null && !rec.getMissingUpgrades().isEmpty())
        {
            lo.append("<i>Upgrades you don't own: ");
            for (int id : rec.getMissingUpgrades())
            {
                lo.append(itemName.apply(id)).append(" ");
            }
            lo.append("</i><br>");
        }
        if (bankAge != null)
        {
            lo.append("<font color='gray'>Bank last seen: ").append(bankAge).append("</font>");
        }
        lo.append("</html>");
        loadoutInfo.setText(lo.toString());
        exportButton.setEnabled(true);
    }
}
```

- [ ] **Step 3: Create `SlayerOverlay`**

```java
package com.danieljglover.allinslayer.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.Setter;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class SlayerOverlay extends OverlayPanel
{
    @Setter
    private volatile boolean enabled;
    @Setter
    private volatile String taskName;
    @Setter
    private volatile String method;

    @Inject
    public SlayerOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!enabled || taskName == null)
        {
            return null;
        }
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text("All-In Slayer").build());
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Task:").right(taskName).build());
        if (method != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Method:").right(method).build());
        }
        return super.render(graphics);
    }
}
```

- [ ] **Step 4: Build to verify it compiles**

Run: `gradle build`
Expected: `BUILD SUCCESSFUL` (existing tests still pass).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/danieljglover/allinslayer/ui src/main/resources/panel_icon.png
git commit -m "feat: add Slayer side panel and task overlay"
```

---

## Task 10: Wire everything into the plugin

**Files:**
- Modify: `src/main/java/com/danieljglover/allinslayer/AllInSlayerPlugin.java`

- [ ] **Step 1: Replace `AllInSlayerPlugin` with the wired version**

```java
package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.bank.InventoryService;
import com.danieljglover.allinslayer.bank.OwnedItems;
import com.danieljglover.allinslayer.data.SlayerDataService;
import com.danieljglover.allinslayer.integration.InventorySetupsExporter;
import com.danieljglover.allinslayer.loadout.LoadoutAdvisor;
import com.danieljglover.allinslayer.loadout.PlayerStats;
import com.danieljglover.allinslayer.loadout.Recommendation;
import com.danieljglover.allinslayer.model.TaskData;
import com.danieljglover.allinslayer.task.SlayerVarbits;
import com.danieljglover.allinslayer.task.TaskDetector;
import com.danieljglover.allinslayer.ui.SlayerOverlay;
import com.danieljglover.allinslayer.ui.SlayerPanel;
import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
    name = "All-In Slayer",
    description = "Bank-aware Slayer advisor: task intel and recommended loadouts",
    tags = {"slayer", "combat", "pve", "loadout", "gear", "task"}
)
public class AllInSlayerPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private AllInSlayerConfig config;
    @Inject private SlayerDataService dataService;
    @Inject private TaskDetector taskDetector;
    @Inject private InventoryService inventoryService;
    @Inject private LoadoutAdvisor loadoutAdvisor;
    @Inject private InventorySetupsExporter exporter;
    @Inject private ItemManager itemManager;
    @Inject private OverlayManager overlayManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private SlayerPanel panel;
    @Inject private SlayerOverlay overlay;

    private NavigationButton navButton;
    private AdviceMode mode;
    private volatile Recommendation lastRecommendation;
    private volatile String lastSetupName;

    @Override
    protected void startUp()
    {
        dataService.load();
        mode = config.adviceMode();

        panel.setOnToggleMode(() ->
        {
            mode = (mode == AdviceMode.DPS) ? AdviceMode.COST : AdviceMode.DPS;
            clientThread.invoke(this::recompute);
        });
        panel.setOnExport(this::exportCurrent);

        navButton = NavigationButton.builder()
            .tooltip("All-In Slayer")
            .icon(ImageUtil.loadImageResource(getClass(), "/panel_icon.png"))
            .priority(6)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);

        overlay.setEnabled(config.showOverlay());
        overlayManager.add(overlay);

        clientThread.invoke(this::recompute);
    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged ev)
    {
        if (ev.getVarpId() == SlayerVarbits.SLAYER_TARGET
            || ev.getVarpId() == SlayerVarbits.SLAYER_COUNT
            || ev.getVarpId() == SlayerVarbits.SLAYER_AREA)
        {
            recompute();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged ev)
    {
        int id = ev.getContainerId();
        if (id == InventoryID.BANK.getId())
        {
            inventoryService.onBankChanged(ev.getItemContainer());
        }
        if (id == InventoryID.BANK.getId()
            || id == InventoryID.INVENTORY.getId()
            || id == InventoryID.EQUIPMENT.getId())
        {
            recompute();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        recompute();
    }

    /** Runs on the client thread; reads game state, computes a recommendation, updates UI on the EDT. */
    private void recompute()
    {
        Optional<TaskData> task = taskDetector.resolveCurrentTask();
        if (!task.isPresent())
        {
            lastRecommendation = null;
            overlay.setTaskName(null);
            SwingUtilities.invokeLater(panel::showNoTask);
            return;
        }

        TaskData t = task.get();
        OwnedItems owned = inventoryService.currentOwned();
        PlayerStats stats = buildStats();
        Optional<Recommendation> rec = loadoutAdvisor.recommend(t, owned, stats, mode, config.haveCannon());

        lastRecommendation = rec.orElse(null);
        lastSetupName = t.getTask();

        overlay.setEnabled(config.showOverlay());
        overlay.setTaskName(t.getTask());
        overlay.setMethod(rec.map(Recommendation::getMethod).orElse(null));

        final String bankAge = bankAgeText();
        final TaskData ft = t;
        final Recommendation fr = lastRecommendation;
        final AdviceMode fm = mode;
        SwingUtilities.invokeLater(() ->
            panel.update(ft, fr, fm, id -> itemManager.getItemComposition(id).getName(), bankAge));
    }

    private void exportCurrent()
    {
        Recommendation rec = lastRecommendation;
        if (rec == null)
        {
            return;
        }
        String json = exporter.buildImportString(rec, lastSetupName);
        exporter.copyToClipboard(json);
        log.debug("Copied Inventory Setups import string for {}", lastSetupName);
    }

    private PlayerStats buildStats()
    {
        return new PlayerStats(
            client.getRealSkillLevel(Skill.ATTACK),
            client.getRealSkillLevel(Skill.STRENGTH),
            client.getRealSkillLevel(Skill.DEFENCE),
            client.getRealSkillLevel(Skill.RANGED),
            client.getRealSkillLevel(Skill.MAGIC),
            client.getRealSkillLevel(Skill.SLAYER));
    }

    private String bankAgeText()
    {
        Long ts = inventoryService.bankLastSeen();
        if (ts == null)
        {
            return null;
        }
        long mins = Duration.between(Instant.ofEpochMilli(ts), Instant.now()).toMinutes();
        if (mins < 1)
        {
            return "just now";
        }
        if (mins < 60)
        {
            return mins + "m ago";
        }
        return (mins / 60) + "h ago";
    }

    @Provides
    AllInSlayerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AllInSlayerConfig.class);
    }
}
```

> Binding note: `LoadoutAdvisor` depends on the `DpsEstimator`, `EquipmentStatsProvider`, and `PriceService` **interfaces**. Guice needs them bound to the `Default*` implementations. RuneLite's plugin Guice module binds concrete classes automatically when there's a single implementation annotated for injection; if Guice reports an unbound interface at startup, add a `configure()` override in the plugin via a small `AbstractModule` (Step 2).

- [ ] **Step 2: If Guice cannot resolve the interfaces, add explicit bindings**

Create `src/main/java/com/danieljglover/allinslayer/AllInSlayerModule.java`:

```java
package com.danieljglover.allinslayer;

import com.danieljglover.allinslayer.loadout.DefaultDpsEstimator;
import com.danieljglover.allinslayer.loadout.DefaultEquipmentStatsProvider;
import com.danieljglover.allinslayer.loadout.DefaultPriceService;
import com.danieljglover.allinslayer.loadout.DpsEstimator;
import com.danieljglover.allinslayer.loadout.EquipmentStatsProvider;
import com.danieljglover.allinslayer.loadout.PriceService;
import com.google.inject.AbstractModule;

public class AllInSlayerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(EquipmentStatsProvider.class).to(DefaultEquipmentStatsProvider.class);
        bind(PriceService.class).to(DefaultPriceService.class);
        bind(DpsEstimator.class).to(DefaultDpsEstimator.class);
    }
}
```

Then annotate the plugin with the module: change the `@PluginDescriptor`-annotated class declaration to also configure the module by overriding `configure(Binder)` — RuneLite `Plugin` supports a `configure(Binder binder)` method:

```java
    @Override
    public void configure(com.google.inject.Binder binder)
    {
        binder.install(new AllInSlayerModule());
    }
```

- [ ] **Step 3: Build**

Run: `gradle build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verification in the client**

Run: `gradle run`
Then:
1. Log in. Confirm the **All-In Slayer** navigation button appears and the panel opens.
2. With **no** task: panel shows "No slayer task detected".
3. Get/already have a Duradel task that exists in the dataset (e.g. Abyssal demons). Confirm the panel shows level/weakness/location/method and a loadout with items you own, an est. DPS, and gear cost.
4. Open your bank once, then close it; confirm the loadout now reflects banked gear and shows a "Bank last seen" note.
5. Toggle **Mode: DPS/COST**; confirm the chosen items change when you own both a BIS and a cheaper option.
6. Click **Export to Inventory Setups**, then in the Inventory Setups plugin choose "Import setup (from clipboard)"; confirm a setup is created. If import fails, apply the `layout` fallback noted in Task 8.
7. Toggle the overlay via config; confirm it appears/disappears.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: wire services, panel, overlay, and events into the plugin"
```

---

## Task 11: Populate the full Duradel dataset (+ validation gate)

This task replaces the 2-task sample with **every task Duradel can assign**. Because this is data entry, correctness is enforced by a **validation test** (complete code below) rather than by reviewing 47 rows by eye. The engineer fills `slayer-data.json` until the test passes.

**Source of truth:** https://oldschool.runescape.wiki/w/Duradel#Assignments (columns: Monster · Amount · Extended · Unlock req · Weight). Cross-check each monster's `slayer_level`, `elemental_weakness`, and defence stats via the OSRS Wiki **Bucket API** (`infobox_monster`) or the monster's wiki page. **Validate every row against the live wiki** — do not trust any pre-filled value (the research pass produced a hallucinated "Frost dragons" row; it does not exist).

**Files:**
- Modify: `src/main/resources/data/slayer-data.json` (expand to the full Duradel set)
- Test: `src/test/java/com/danieljglover/allinslayer/data/DuradelDatasetValidationTest.java`

**Per-task field checklist** (every Duradel task object must have): `task`, `slayerTargetId` (>0, from the in-game SLAYER_TARGET varp — verify with RuneLite dev tools `::varp 395` while on each task, or the wiki's assignment id), `slayerLevel` (1–99), `questReqs` (array, may be empty), `assignedBy` (must include `"duradel"`), `amountByMaster.duradel` ([min,max]), `monsters` (≥1), `npcIds` (≥1), `weakness.style` (non-null), `monsterDefence`, `slayerHelmApplies`, `requiredItemId`/`requiredItemName` (null if none), `locations` (≥1, each with multi/cannon/burst/konarLockable), `loadouts` (≥1 style with non-empty `slotOptions`), `recommendedMethod`.

- [ ] **Step 1: Enumerate Duradel's task list** from the cited table. As of the current wiki, Duradel assigns ~47 tasks including: Abyssal demons, Adamant dragons, Ankou, Aviansie*, Black demons, Black dragons, Bloodveld, Blue dragons, Boss*, Brine rats, Cave horrors, Cave kraken, Dagannoth, Dark beasts, Drakes, Dust devils, Fire giants, Fossil Island wyverns, Gargoyles, Greater demons, Hellhounds, Iron dragons, Kalphite, Kurask, Lizardmen*, Mithril dragons, Nechryael, Red dragons*, Rune dragons, Skeletal wyverns, Smoke devils, Spiritual creatures, Steel dragons, Suqahs, Trolls, TzHaar*, Wyrms, and others (* = requires a point unlock). **Confirm the exact current list from the wiki table; add/remove rows to match.**

- [ ] **Step 2: Fill each task object.** Two fully-worked, correctly-shaped examples to use as templates (verify the ids against the wiki before trusting them):

```json
  {
    "task": "Gargoyles",
    "slayerTargetId": 49,
    "slayerLevel": 75,
    "questReqs": [],
    "assignedBy": ["vannaka", "chaeldar", "nieve", "duradel", "konar"],
    "amountByMaster": { "duradel": [185, 250] },
    "monsters": ["Gargoyle"],
    "npcIds": [412, 413, 1543],
    "weakness": { "style": "MELEE", "element": null },
    "monsterDefence": { "defenceLevel": 80, "stab": 40, "slash": 40, "crush": 20, "magic": 100, "range": 40 },
    "slayerHelmApplies": true,
    "requiredItemId": 4162,
    "requiredItemName": "Rock hammer",
    "locations": [
      { "name": "Slayer Tower (top floor)", "multi": false, "cannon": false, "burst": false, "konarLockable": true }
    ],
    "loadouts": {
      "MELEE": {
        "style": "MELEE",
        "slotOptions": {
          "WEAPON": [22325, 1305],
          "HEAD": [21264, 11865],
          "BODY": [21295, 10551],
          "LEGS": [21304, 1079],
          "HANDS": [22981, 7462],
          "FEET": [13239, 11840],
          "RING": [28307, 6737]
        },
        "inventory": [4162, 12695, 385, 2434],
        "spellMaxHit": null
      }
    },
    "recommendedMethod": "Melee on Slayer Tower top floor; carry a rock/granite hammer to finish them at low HP"
  },
  {
    "task": "Hellhounds",
    "slayerTargetId": 50,
    "slayerLevel": 1,
    "questReqs": [],
    "assignedBy": ["vannaka", "chaeldar", "nieve", "duradel", "konar", "krystilia"],
    "amountByMaster": { "duradel": [130, 200] },
    "monsters": ["Hellhound"],
    "npcIds": [104, 105],
    "weakness": { "style": "MELEE", "element": null },
    "monsterDefence": { "defenceLevel": 60, "stab": 20, "slash": 20, "crush": 20, "magic": 50, "range": 20 },
    "slayerHelmApplies": true,
    "requiredItemId": null,
    "requiredItemName": null,
    "locations": [
      { "name": "Stronghold Slayer Cave", "multi": true, "cannon": true, "burst": false, "konarLockable": true },
      { "name": "Catacombs of Kourend", "multi": true, "cannon": false, "burst": false, "konarLockable": true }
    ],
    "loadouts": {
      "MELEE": {
        "style": "MELEE",
        "slotOptions": {
          "WEAPON": [22325, 1305],
          "HEAD": [21264, 11865],
          "BODY": [21295, 10551],
          "LEGS": [21304, 1079]
        },
        "inventory": [12695, 385, 2434],
        "spellMaxHit": null
      }
    },
    "recommendedMethod": "Cannon + melee in Stronghold Slayer Cave (cannon allowed); great alching/herb drops"
  }
```

- [ ] **Step 3: Write the validation test** (the completeness gate)

```java
package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.model.StyleLoadout;
import com.danieljglover.allinslayer.model.TaskData;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DuradelDatasetValidationTest
{
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
            assertNotNull(where + " weakness", t.getWeakness());
            assertNotNull(where + " weakness style", t.getWeakness().getStyle());
            assertNotNull(where + " monsterDefence", t.getMonsterDefence());
            assertNotNull(where + " locations", t.getLocations());
            assertFalse(where + " locations empty", t.getLocations().isEmpty());
            assertNotNull(where + " loadouts", t.getLoadouts());
            assertFalse(where + " loadouts empty", t.getLoadouts().isEmpty());
            for (StyleLoadout sl : t.getLoadouts().values())
            {
                assertNotNull(where + " slotOptions", sl.getSlotOptions());
                assertFalse(where + " slotOptions empty", sl.getSlotOptions().isEmpty());
            }
        }
    }
}
```

- [ ] **Step 4: Run the gate; fill data until green**

Run: `gradle test --tests com.danieljglover.allinslayer.data.DuradelDatasetValidationTest`
Expected: initially FAIL (only the 2 sample tasks). Add Duradel tasks to `slayer-data.json` until all assertions PASS.

- [ ] **Step 5: Run the full suite**

Run: `gradle test`
Expected: all tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/data/slayer-data.json src/test/java/com/danieljglover/allinslayer/data/DuradelDatasetValidationTest.java
git commit -m "feat: complete Duradel task dataset with validation gate"
```

---

## Task 12: Hub packaging & release prep

**Files:**
- Create: `README.md`
- Create: `docs/plugin-hub-manifest.txt` (the manifest content to submit later)

- [ ] **Step 1: Create `README.md`**

```markdown
# All-In Slayer

A read-only Slayer advisor for [RuneLite](https://runelite.net). Detects your current Slayer
task and shows, in a side panel and overlay: the task's Slayer level, weakness, locations, and
recommended method, plus a **bank-aware loadout** built from gear you actually own — toggleable
between **DPS** and **cost** — with one-click export to the Inventory Setups plugin.

v1 covers **Duradel's** task set; later versions add the remaining masters.

## Rules compliance

This plugin is a passive advisor. It never automates input, never tells you where to stand or
when to pray in real time, and never sends your data anywhere. All task data is bundled and
fully reviewable. See `docs/superpowers/specs/2026-06-28-all-in-slayer-design.md`.

## Build

    gradle build      # compile + test
    gradle run        # launch RuneLite with the plugin loaded

## License

BSD 2-Clause. See `LICENSE`.
```

- [ ] **Step 2: Create the Hub manifest template** at `docs/plugin-hub-manifest.txt`

```text
# Submit by: forking github.com/runelite/plugin-hub, adding this file as
# plugins/all-in-slayer (no extension), and opening a PR. Fill `commit` with the
# release commit hash AFTER pushing this repo to a public GitHub remote.
repository=https://github.com/danieljglover/AIO-Slayer-Assistant.git
commit=<40-char release commit hash>
```

- [ ] **Step 3: Full clean build**

Run: `gradle clean build`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 4: Commit and tag**

```bash
git add README.md docs/plugin-hub-manifest.txt
git commit -m "docs: add README and plugin-hub manifest template"
git tag v1.0.0
```

> Submission (manual, outside this plan): push the repo to a public GitHub remote, then follow `docs/plugin-hub-manifest.txt`. Hub CI will scan for reflection/native/dependency-hash issues; address any findings.

---

## Self-Review (performed against the spec)

**Spec coverage:**
- §2 task intel (level/weakness/locations/method/required items) → model (Task 2) + panel/overlay (Tasks 9–10) + data (Tasks 3, 11). ✓
- §2 bank-aware best owned setup, DPS vs cost → Tasks 5–7. ✓
- §2 cannon configurable option → `AllInSlayerConfig.haveCannon` (Task 1) consumed in `LoadoutAdvisor.chooseLocation` (Task 7). ✓
- §3 rules guardrails (no automation/auto-tile/live-prayer/exfiltration) → design is passive display only; no input APIs used anywhere in Tasks 1–12. ✓
- §5 module architecture → packages `data/task/bank/loadout/integration/ui` (Tasks 2–9). ✓
- §6 bundled, reviewable, validated data → resource JSON (Task 3) + validation gate (Task 11); automated pipeline correctly deferred to a separate plan. ✓
- §7 varbit task detection, no chat parsing → `TaskDetector` (Task 4). ✓
- §8 bank-open-only constraint + last-seen snapshot → `InventoryService` (Task 5). ✓
- §9 hybrid loadout engine (curated tiers + light DPS; cost = cheapest effective owned) → Tasks 6–7. ✓
- §10 Inventory Setups export → Task 8 + button wiring (Task 10). ✓
- §11 panel + overlay + config → Tasks 1, 9, 10. ✓
- §12 tests → unit tests in Tasks 2–8, validation gate Task 11. ✓
- §13 BSD license, Gradle, gameval ids → Task 1 + `SlayerVarbits` (Task 4) + Task 12. ✓

**Type consistency:** `OwnedItems`, `TaskData`, `Recommendation`, `PlayerStats`, `Bonuses`, `DpsEstimator`/`PriceService`/`EquipmentStatsProvider` interfaces, `AdviceMode`, `CombatStyle`, `EquipmentSlot` are defined once and referenced with identical signatures across Tasks 5–10. `slayerTargetId` added to the model in Task 4 and required by the Task 11 gate. ✓

**Placeholder scan:** code steps contain complete, compilable code. The only intentionally-unfilled artifact is the Hub manifest `commit=` hash (unknowable until release) and the dataset rows in Task 11 — both gated (the dataset by a mechanical test, the manifest by a documented manual submission step). ✓

**Known risks carried from the spec (verify during implementation):**
- `slayerTargetId` values per task must be confirmed in-game (`::varp 395`) or against the wiki — the gate checks presence, not correctness.
- `monsterDefence` and item ids in the dataset are illustrative until verified against the wiki.
- Inventory Setups import may need the `layout` fallback (Task 8) depending on the installed version.
- Guice interface bindings may need the explicit module (Task 10 Step 2).
- `ItemEquipmentStats` getter names (`getAstab`, `getMdmg`, `getAspeed`, …) should be confirmed against the `runelite-client` version resolved by `latest.release`.

---

## References

- Design spec: `docs/superpowers/specs/2026-06-28-all-in-slayer-design.md`
- RuneLite example plugin: https://github.com/runelite/example-plugin
- RuneLite API Javadocs: https://static.runelite.net/runelite-api/apidocs/
- Duradel assignments: https://oldschool.runescape.wiki/w/Duradel#Assignments
- OSRS Wiki Bucket API: `https://oldschool.runescape.wiki/api.php?action=bucket`

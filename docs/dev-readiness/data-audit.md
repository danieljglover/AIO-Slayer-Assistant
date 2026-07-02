# Slayer Data Audit & Duradel Readiness Matrix

Task: **DT-CTX-DATA** · Audited: 2026-07-01 · Source of truth: `src/main/data/slayer/`
Compiled runtime dataset cross-checked: `build/generated/resources/slayer/data/slayer-data.json`
(regenerated via `./gradlew generateSlayerData`, 631 KB, 43 task entries).

> **Scope note / status:** COMPLETE. All 628 source files were swept programmatically (scripts in the
> task scratchpad), and every finding below was cross-checked against the compiled runtime output the
> plugin actually loads. Field-presence, referential-integrity, and coverage numbers are from full sweeps,
> not sampling.

---

## 0. Headline findings (read this first)

1. **Referential integrity of the source is excellent.** Zero broken references across
   task→variant, task→location, task→master, variant→strategy, strategy→variant, and
   strategy→weapon. Every multi-variant task has a valid `defaultVariantId`. The generator's strict
   validation is doing its job.
2. **Data enrichment is now near-universal**, not the 11 tasks from 2026-06-30. **42 / 43 tasks**
   carry the rich `wikiPageId`+`variantInfo`+`taskNotes`+`recommendedMethod`+`locationComparison`
   fields. The single exception is the `boss` meta-task, which is deliberately bare.
3. **Strategy/loadout coverage at runtime is 100%.** All 228 emitted variant entries carry a
   `strategy` block with a non-empty `primaryWeapons` list. Goal (c) — loadout — is well supported.
4. **The load-bearing gap is location-BY-variant (goal b).** At runtime, location flags exist **only
   at the task level**; each variant carries `location` as a free-text display **string**, not a
   structured/linked location. The plugin cannot map a chosen variant to a specific location or its
   flags. See GAP-1.
5. **The compiler silently drops `safeSpot` and `accessNote`.** They are authored in source
   (`locations/*.json` + `locationComparison[]`) but never reach the runtime location object. Safespot
   info — which drives method/loadout — is invisible to the plugin. See GAP-2.

---

## 1. Schema summary per category

Counts: masters **9**, tasks **43**, monster families **43** / variant files **231** (226 assignable +
5 display-only), locations **169**, weapons **79**, strategies **97** (88 JSON dirs + 9 legacy `.md`).

### 1a. `masters/*.json` (9)
Only two fields, both present in all 9. Masters: chaeldar, duradel, konar, krystilia, mazchna, nieve,
spria, turael, vannaka.
```json
{ "masterId": "duradel", "name": "Duradel" }
```
No inconsistencies.

### 1b. `tasks/*.json` (43)
The richest file type. **Always present (43/43):** `taskId, name, slayerTargetId, slayerLevel,
questReqs, masterIds, amountByMaster, monsterIds, variantIds, defaultVariantId, weakness,
monsterDefence, slayerHelmApplies, undead, dragon, demon, kalphite, requiredItemId, requiredItemName,
locationIds, recommendedMethod`.

**Partial (enrichment) fields:**

| Field | Present | Missing in |
|---|---|---|
| `wikiPageId` | 42/43 | `boss` |
| `variantInfo` | 42/43 | `boss` |
| `taskNotes` | 42/43 | `boss` |
| `locationComparison` | 41/43 | `boss`, `lizardmen` |
| `unlocks` | 39/43 | boss, hellhounds, kalphite, nechryael |
| `combatLevel` | 38/43 | boss + 4 others |
| `extendedAmount` | 30/43 | 13 tasks (tasks that don't extend) |

Example (trimmed — full task JSON is large; see `tasks/abyssal-demons.json`):
```json
{
  "taskId": "abyssal-demons", "name": "Abyssal demons", "wikiPageId": 298016,
  "slayerLevel": 85, "masterIds": ["krystilia","vannaka","chaeldar","konar","nieve","duradel"],
  "amountByMaster": { "duradel": [130,200], ... },
  "variantIds": ["abyssal-demon","greater-abyssal-demon","abyssal-demons-abyssal-sire"],
  "defaultVariantId": "abyssal-demon",
  "weakness": {"style":"MELEE","element":null},
  "monsterDefence": {"defenceLevel":135,"stab":20,"slash":20,"crush":20,"magic":0,"range":20},
  "slayerHelmApplies": true, "demon": true, "locationIds": ["abyssal-area","catacombs-of-kourend", ...],
  "variantInfo": [ { "variantId":"abyssal-demon","locations":["Abyssal Area","Slayer Tower", ...], "notes":[...] } ],
  "locationComparison": [ { "locationId":"abyssal-area","multicombat":false,"cannonable":false,"safespottable":false,"notes":[...] } ],
  "taskNotes": [ ... ], "recommendedMethod": "Use Catacombs magic ..."
}
```

**Key schema subtleties (matter for the plugin):**
- `locationIds` (task-level) are stable FK references into `locations/*.json`. **This is the only
  structured location linkage.**
- `variantInfo[].locations` are **display strings** ("Abyssal Area", "Slayer Tower"), *not* IDs.
- `locationComparison[].locationId` *looks* like an FK and carries its own flags
  (`multicombat/cannonable/safespottable`), but is **per-task**, not per-variant, and is **not always a
  valid FK** (see GAP-5). It duplicates flag semantics that also live on `locations/*.json`.

### 1c. `monsters/<family>/*.json` — variant files (231)
**Always present (231/231):** `variantId, name, npcIds, combatLevel, weakness, monsterDefence, demon,
dragon, kalphite, undead, boss, location, requirement, bossId`.
**Partial:** `strategyId` 229/231 (2 missing), `locationId` **63/231**.
```json
{
  "variantId":"abyssal-demons-abyssal-sire","name":"Abyssal Sire","npcIds":[5886,5887,...],
  "combatLevel":350,"weakness":{"style":"MELEE","element":null},
  "monsterDefence":{"defenceLevel":250,"stab":40,...},
  "demon":true,"boss":true,"location":"Abyssal Nexus","requirement":"boss - separate trip",
  "bossId":null,"strategyId":"abyssal-sire"
}
```
- `location` is a display string; `bossId` is present-but-often-null.
- `locationId` (only 63 files) **is** a clean resolvable FK (0 broken) — but the compiler discards it
  (not in the runtime variant object). See GAP-1.
- `weakness.element` is set on 188/231 (null where weakness is a pure melee/ranged style).

### 1d. `locations/*.json` (169)
**Always present (169/169):** `locationId, name, multi, cannon, burst, konarLockable`.
**Partial:** `wilderness` 158/169, `safeSpot` 157/169, `accessNote` 157/169.
```json
{
  "locationId":"catacombs-of-kourend","name":"Catacombs of Kourend",
  "multi":true,"cannon":false,"burst":true,"konarLockable":true,"safeSpot":true,"wilderness":false,
  "accessNote":"Black demons here can be safespotted ..."
}
```
Naming convention: many task-specific variants exist (e.g. `catacombs-of-kourend-fire-giants`,
`brimhaven-dungeon-metal-dragons-task-only`) so one physical area maps to several tuned location files.

### 1e. `weapons/*.json` (79)
Uniform, all three fields present in all 79. The compiler emits **only the first `itemIds` entry** to
the runtime strategy recommendation.
```json
{ "weaponId":"arclight", "name":"Arclight", "itemIds":[19675] }
```

### 1f. `strategies/<id>/strategy.json` (88) + legacy `strategies/*.md` (9)
JSON strategies: **all 88/88** carry `strategyId, variantIds, sourceUrl, plugin, requirements,
mechanics, methods, styleOptions`; 53/88 also carry a free-text `body`. **The `plugin` block is present
on all 88 with all four sub-fields** (`primaryStyle, primaryWeapons, secondaryWeapons, note`) — this is
the block the runtime consumes. `methods[]`/`styleOptions[]` are rich per-slot equipment tables for
LLM/maintainer use (see `strategies/abyssal-demons/strategy.json` for a full example).

Legacy `.md` (9): araxxor, branda-the-fire-queen, demonic-gorilla, flight-kilisa, flockleader-geerin,
lizardman-shaman, skotizo, tormented-demon, wingman-skree. They carry the same plugin fields in YAML
front-matter (`primaryStyle`, `primaryWeapons`, `secondaryWeapons`, `note`, `sourceUrl`) and the compiler
parses them into the same runtime `strategy` block — so they are **functionally equivalent at runtime**,
just not migrated to JSON. All 9 are still referenced by a variant (none orphaned).

---

## 2. Duradel coverage matrix (core output)

**Duradel assigns all 43 tasks** (Duradel is the top-tier master; `masters/duradel.json` is just
`{id,name}` — the assignment list lives on each task's `masterIds`). So the Duradel matrix = every task.

Legend: **#var** = assignable variants · **1×def** = exactly one `isDefault` (verified at runtime) ·
**prof** = all assignable variants have complete weakness+defence profile · **#loc** = task-level
locations · **loc-flags** = task locations carry multi/cannon/burst/konar/wilderness (safeSpot/accessNote
are dropped at compile — see GAP-2) · **strat** = every variant resolves to a strategy w/ plugin weapons
(`J`=JSON, `M`=legacy md) · **loc/var** = location linkage granularity · **enr** = wiki-enriched.

| Duradel task | dur amt | #var | 1×def | prof | #loc | loc-flags | strat (plugin weps) | loc/var | enr |
|---|---|---|---|---|---|---|---|---|---|
| aberrant-spectres | 130–200 | 4 | ✅ | ✅ | 4 | ✅ | ✅ all J | task-only | ✅ |
| abyssal-demons | 130–200 | 3 | ✅ | ✅ | 5 | ✅ | ✅ all J | task-only | ✅ |
| ankou | 50–80 | 6 | ✅ | ⚠️ dark-ankou null | 6 | ✅ | ✅ all J | task-only | ✅ |
| aquanites | 30–50 | 2 | ✅ | ✅ | 1 | ✅ | ✅ all J | task-only | ✅ |
| araxytes | 60–80 | 4 | ✅ | ✅ | 1 | ✅ | ✅ (1 via **M** araxxor) | task-only | ✅ |
| aviansie | 120–200 | 5 | ✅ | ✅ | 2 | ✅ | ✅ (3 via **M**) | task-only | ✅ |
| basilisks | 130–200 | 4 | ✅ | ✅ | 2 | ✅ | ✅ all J | task-only | ✅ |
| black-demons | 130–200 | 7 | ✅ | ✅ | 8 | ✅ | ✅ (2 via **M**) | task-only | ✅ |
| black-dragons | 10–20 | 4 | ✅ | ✅ | 10 | ✅ | ✅ all J | task-only | ✅ |
| bloodveld | 130–200 | 5 | ✅ | ❌ 4 null | 9 | ✅ | ✅ all J | task-only | ✅ |
| blue-dragons | 110–170 | 4 | ✅ | ✅ | 10 | ✅ | ✅ all J | task-only | ✅ |
| **boss** | 3–35 | 33 | ✅ | ✅ | **1 (placeholder)** | ⚠️ | ✅ all J | **none** | ❌ |
| cave-horrors | 130–200 | 2 | ✅ | ✅ | 1 | ✅ | ✅ all J | task-only | ✅ |
| cave-kraken | 100–120 | 2 | ✅ | ✅ | 1 | ✅ | ✅ all J | task-only | ✅ |
| dagannoth | 130–200 | 7 | ✅ | ✅ | 4 | ✅ | ✅ all J | task-only | ✅ |
| dark-beasts | 10–20 | 2 | ✅ | ✅ | 2 | ✅ | ✅ all J | task-only | ✅ |
| drakes | 50–110 | 2 | ✅ | ✅ | 1 | ✅ | ✅ all J | task-only | ✅ |
| dust-devils | 130–200 | 2 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only | ✅ |
| elves | 100–170 | 6 | ✅ | ✅ | 12 | ✅ | ✅ all J | task-only | ✅ |
| fire-giants | 130–200 | 4 | ✅ | ✅ | 10 | ✅ | ✅ (1 via **M**) | task-only | ✅ |
| fossil-island-wyverns | 20–50 | 4 | ✅ | ✅ | 2 | ✅ | ✅ all J | task-only | ✅ |
| frost-dragons | 70–120 | 1 | ✅ | ✅ | 2 | ✅ | ✅ all J | task-only | ✅ |
| gargoyles | 130–200 | 4 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only(+63 loc FK) | ✅ |
| greater-demons | 130–200 | 9 | ✅ | ✅ | 10 | ✅ | ✅ (2 via **M**) | task-only | ✅ |
| gryphons | 100–210 | 3 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only | ✅ |
| hellhounds | 130–200 | 8 | ✅ | ✅ | 12 | ✅ | ✅ all J | task-only(+loc FK) | ✅ |
| kalphite | 130–200 | 5 | ✅ | ✅ | 2 | ✅ | ✅ all J | task-only | ✅ |
| kurask | 130–200 | 2 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only | ✅ |
| lizardmen | 130–210 | 3 | ✅ | ✅ | 3 | ⚠️ | ✅ (1 via **M**) | task-only | ⚠️ no locComp |
| metal-dragons | 35–45 | 9 | ✅ | ✅ | 6 | ✅ | ✅ all J | task-only | ✅ |
| mutated-zygomites | 20–30 | 3 | ✅ | ✅ | 4 | ✅ | ✅ all J | task-only | ✅ |
| nechryael | 130–200 | 3 | ✅ | ✅ | 6 | ✅ | ✅ all J | task-only | ✅ |
| red-dragons | 30–65 | 3 | ✅ | ✅ | 8 | ✅ | ✅ all J | task-only(+loc FK) | ✅ |
| skeletal-wyverns | 20–40 | 1 | ✅ | ✅ | 2 | ✅ | ✅ all J | task-only | ✅ |
| smoke-devils | 130–200 | 3 | ✅ | ✅ | 1 | ✅ | ✅ all J | task-only | ✅ |
| spiritual-creatures | 130–200 | 15 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only | ⚠️ locComp FKs broken |
| suqahs | 60–90 | 1 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only | ✅ |
| trolls | 130–200 | 20 | ✅ | ✅ | 7 | ✅ | ✅ all J | task-only | ✅ |
| tzhaar | 130–199 | 7 | ✅ | ✅ | 4 | ✅ | ✅ all J | task-only | ✅ |
| vampyres | 100–210 | 5 | ✅ | ✅ | 7 | ✅ | ✅ all J | task-only | ✅ |
| warped-creatures | 130–200 | 5 | ✅ | ✅ | 4 | ✅ | ✅ all J | task-only | ✅ |
| waterfiends | 130–200 | 1 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only | ✅ |
| wyrms | 100–160 | 5 | ✅ | ✅ | 3 | ✅ | ✅ all J | task-only | ✅ |

**Matrix reading:**
- (a) **variant** — fully covered. 43/43 tasks list variants, all with exactly one `isDefault`
  (verified against the runtime output).
- (b) **location by variant** — **NOT covered anywhere**. The `loc/var` column is `task-only` for
  every regular task: location flags are attached to the task, not the variant. 63 source variants have
  a `locationId` FK, but it is dropped at compile. `boss` has no per-task location at all (single
  placeholder). This is the central goal-blocking gap.
- (c) **loadout** — fully covered. Every variant reaches runtime with a `strategy{primaryStyle,
  primaryWeapons[itemId], secondaryWeapons}` block; 0 variants have empty weapons.

**Wiki-enrichment verification:** the 2026-06-30 milestone enriched 11 tasks; enrichment has since
expanded to **all tasks except `boss`**. The per-task coverage tests live in
`src/test/java/com/danieljglover/allinslayer/data/source/*SourceCoverageTest.java` (46 test classes),
and `TaskWikiPageIdSourceCoverageTest` pins each task's `wikiPageId` (with `boss → null`).

---

## 3. Referential integrity

Full cross-reference sweep of every ID edge. **Source integrity is clean** except one issue (GAP-5):

| Edge | Result |
|---|---|
| task.masterIds → masters | ✅ 0 broken |
| task.variantIds → variant files | ✅ 0 broken |
| task.defaultVariantId ∈ variantIds (multi-variant) | ✅ 0 bad |
| task.locationIds → locations | ✅ 0 broken |
| variant.strategyId → strategy (JSON or md) | ✅ 0 broken (2 variants have no strategyId — both non-assignable) |
| strategy.variantIds → variant files | ✅ 0 broken |
| strategy.plugin weapons → weapons | ✅ 0 broken (primary + secondary) |
| **task.locationComparison[].locationId → locations** | ❌ **9 broken** (all in `spiritual-creatures`) — see GAP-5 |

**Boss meta-task vs `monsters/boss/`:** the `boss` task lists 33 variant IDs; all 33 resolve to variant
files and each resolves to a boss strategy (JSON) with plugin weapons. No mismatch. (Many boss variants
are aliases of the same NPC referenced from two tasks, e.g. `abyssal-sire` appears as both
`boss-abyssal-sire` and `abyssal-demons-abyssal-sire`; `king-black-dragon` from both boss and
black-dragons — this is intentional cross-linking, not a break.)

**Orphans (dead/unlinked data, not integrity breaks):**
- **11 orphan location files** never referenced by any `task.locationIds`: `abyssal-nexus`,
  `ancient-cavern`, `dark-altar`, `dark-altar-elves`, `forthos-dungeon`, `karuulm-slayer-dungeon`,
  `lunar-isle`, `mourner-tunnels-iorwerth-dungeon`, `smoke-dungeon`, `trollheim-death-plateau`,
  `wilderness-ankou-area`.
- **5 non-assignable variant files** (present but in no `task.variantIds`; some appear only as
  `variantInfo` display rows): `reanimated-bloodveld`, `reanimated-elf`, `reanimated-troll`,
  `skeleton-hellhound-tarn-s-lair`, `spiritual-creature`.

---

## 4. Strategy census

| Metric | Count |
|---|---|
| Strategy sources total | 97 (88 JSON dirs + 9 legacy `.md`) |
| JSON strategies with a `plugin` block incl. `primaryWeapons` | **88 / 88 (100%)** |
| Variant files carrying `strategyId` | 229 / 231 |
| → resolves to a **JSON** strategy | 220 |
| → resolves to a **legacy `.md`** strategy | 9 |
| Variants with **no** `strategyId` | 2 (`reanimated-troll`, `skeleton-hellhound-tarn-s-lair`) — **both non-assignable** |
| **Assignable variant occurrences at runtime with a strategy + non-empty weapons** | **228 / 228 (100%)** |
| Legacy `.md` strategies still referenced by a variant | 9 / 9 (none orphaned) |
| Orphan JSON strategy dirs (unreferenced) | 0 |

**Bottom line:** every monster/variant that can actually be assigned has a strategy that produces a
plugin loadout. The only "no strategy" variants are non-assignable file leftovers. The 9 legacy `.md`
strategies are a **maintainability** debt, not a runtime capability gap — they still emit a full plugin
strategy block. Duradel tasks that still lean on `.md`: araxytes (araxxor), aviansie (flight-kilisa,
flockleader-geerin, wingman-skree), black-demons (demonic-gorilla, skotizo), fire-giants
(branda-the-fire-queen), greater-demons (skotizo, tormented-demon), lizardmen (lizardman-shaman).

---

## 5. What the runtime actually consumes (compiler behaviour)

The plugin loads a **flat list of 43 task entries**. Per task it gets: `task, slayerTargetId,
slayerLevel, questReqs, assignedBy, amountByMaster, monsters[], npcIds[], weakness, monsterDefence,
category flags, requiredItemName, locations[], recommendedMethod, variants[]`.

- **`locations[]` (task-level)** object = `{name, multi, cannon, burst, konarLockable, wilderness}`.
- **`variants[]`** object = `{name, npcIds, combatLevel, weakness, monsterDefence, category flags,
  isBoss, isDefault, location, requirement, strategy}` where `strategy = {primaryStyle,
  primaryWeapons:[{name,itemId}], secondaryWeapons:[{name,itemId,style}], note}`.

**Fields authored in source but DROPPED by the compiler (never reach the plugin):**
`locations.safeSpot`, `locations.accessNote`, the entire `variantInfo[]` block, the entire
`locationComparison[]` block (incl. its `multicombat/cannonable/safespottable` flags and per-location
notes), `unlocks`, `extendedAmount`, `taskNotes`, `wikiPageId`, and the variant-level `locationId` FK.
These exist purely as source/LLM context today. `variant.location` survives only as a **display string**.

---

## 6. GAP LIST — ranked by impact on the goal

Goal: for every Duradel task, suggest (a) the monster variant, (b) the location **by variant**, and
(c) the loadout from variant + location variables + strategy.

### 🔴 GAP-1 — No per-variant → location linkage (blocks goal b) — HIGHEST
Location data is **per-task only**. At runtime each variant's `location` is free text
("Slayer Tower (floor 1) / Stronghold Slayer Cave / Deepfin Mine"); the flag-bearing `locations[]`
array hangs off the task, not the variant. The plugin cannot answer "for *this* variant, which location
and what are its flags?". Source *does* have partial signal the compiler throws away:
`monsters/*.locationId` FK (63/231 variants, all valid) and `variantInfo[].locations` (display strings).
**Fix direction:** compile a structured per-variant location list (resolve `variantInfo[].locations`
and/or `variant.locationId` into location objects) and emit it on the runtime variant.
**Impact:** blocks (b) for essentially all 43 Duradel tasks.

### 🔴 GAP-2 — `safeSpot` + `accessNote` never reach runtime (degrades b & c) — HIGH
`locations.safeSpot`, `locations.accessNote`, and `locationComparison[].safespottable/cannonable/
multicombat/notes` are authored but dropped by the compiler. Safespottability is a first-order input to
method and loadout choice (e.g. ranged/mage safespot vs melee). Also 12 location files are missing
`safeSpot` entirely (see GAP-9), so even the source is incomplete.
**Fix direction:** add `safeSpot` (and optionally `accessNote`) to the runtime location object and
backfill the 12 missing source files.

### 🟠 GAP-3 — `boss` meta-task is un-enriched (blocks a/b/c for boss) — MEDIUM
33 boss variants, **no `variantInfo`, no `locationComparison`**, and a single placeholder location
`various-boss-locations` (which itself has no meaningful flags). Boss location/loadout comes *only* from
each variant's strategy block; there is no task-level location guidance and no per-variant location.
**Impact:** the "Boss" Duradel assignment (3–35) can suggest a variant + loadout (strategy exists) but
cannot suggest a real location. Lower priority because Boss is a single opt-in task.

### 🟠 GAP-4 — 5 assignable variants have null combat profile (degrades a/c) — MEDIUM
`Dark Ankou` (ankou) and 4 Bloodveld variants (`Bloodveld (GWD)`, `Mutated Bloodveld`,
`Insatiable Bloodveld`, `Insatiable mutated Bloodveld`) have **`weakness = null` and
`monsterDefence = null`** — confirmed null in the runtime output too. The plugin gets no per-variant
weakness/defence for these and must fall back to the task-level profile. (`reanimated-bloodveld` also
has null weakness but is non-assignable.)
**Fix direction:** author weakness + monsterDefence for these 5 variant files.

### 🟡 GAP-5 — `spiritual-creatures` locationComparison FKs are broken (data quality) — LOW-MED
9 `locationComparison[].locationId` values in `tasks/spiritual-creatures.json`
(`spiritual-rangers-god-wars-dungeon`, `spiritual-warriors-ancient-prison`,
`spiritual-mages-wilderness-god-wars-dungeon`, …) do not exist in `locations/*.json`. It doesn't break
the runtime (locationComparison isn't compiled), but it means `locationComparison.locationId` is **not a
reliable FK**, and the generator's validation does **not** check it. If GAP-1/GAP-2 are fixed by reading
locationComparison, this must be fixed first (and validation extended to cover it).

### 🟡 GAP-6 — `variantInfo` ↔ `variantIds` misalignment (weakens per-variant context) — LOW
Several tasks have assignable `variantIds` with **no matching `variantInfo` entry**, so the (display)
per-variant location strings/notes don't cover every assignable variant: `boss` (0 of 33),
`spiritual-creatures` (15 assignable → aggregated to 3 variantInfo rows), `aviansie`, `black-demons`,
`dagannoth`, `kalphite`, `tzhaar`. Conversely some `variantInfo` rows are display-only
(`abyssal-demon-catacombs`, `the-jormungand`, `kolodion-final-form`, reanimated-*). Matters only if
`variantInfo` becomes a compile input for GAP-1.

### 🟡 GAP-7 — 9 legacy `.md` strategies not migrated to JSON (maintainability) — LOW
araxxor, branda-the-fire-queen, demonic-gorilla, flight-kilisa, flockleader-geerin, lizardman-shaman,
skotizo, tormented-demon, wingman-skree. Functionally fine at runtime, but two authoring formats + weaker
structured validation for these. Affects 6 Duradel tasks (araxytes, aviansie, black-demons, fire-giants,
greater-demons, lizardmen).

### ⚪ GAP-8 — Orphan data (housekeeping) — LOW
11 orphan location files + 5 non-assignable variant files (listed in §3). Either wire them into a task
or delete them; today they're dead weight and slightly muddy the census.

### ⚪ GAP-9 — 12 location files missing `safeSpot`/`wilderness`/`accessNote` (source completeness) — LOW
Missing all three: `ancient-cavern, forthos-dungeon, karuulm-slayer-dungeon, kebos-lowlands,
lizardman-canyon, lizardman-settlement, lunar-isle, mourner-tunnels-iorwerth-dungeon, smoke-dungeon,
trollheim-death-plateau, various-boss-locations`; `wilderness-ankou-area` missing `safeSpot`+`accessNote`.
`wilderness` DOES compile to runtime, so the 4 task-referenced ones (`kebos-lowlands`, `lizardman-canyon`,
`lizardman-settlement`, `various-boss-locations`) emit a default `false` — correct in these cases but
relying on a default rather than explicit data. Tied to GAP-2 (safeSpot).

---

## Appendix — method & artifacts
- Sweep scripts: `<scratchpad>/audit.py` (field presence), `matrix.py` (Duradel matrix + integrity +
  census), `deep.py` (profile/location deep-dive). All read source directly + cross-check the compiled
  runtime JSON.
- Runtime regenerated with `./gradlew generateSlayerData` (exit 0) prior to cross-checking.
- No production, data, or test file was modified during this audit.

# Monster Variant Selection - Technical Design & Build Plan (MV-A1, Gate 2)

- Author: Architect / Principal Engineer (board MV-A1)
- Date: 2026-06-29
- Inputs: `docs/variants/prd.md` (MV-P1, 7 FRs), `docs/variants/INDEX.md` + `variants-*.md` (MV-R1
  enumeration, ~215 blocks), the loadout engine (`loadout/*`, ADR-0001..0009), `model/TaskData.java`,
  `src/main/resources/data/slayer-data.json` (42 tasks).
- ADRs: **0010** (per-variant profile + `MonsterProfile` seam), **0011** (variant x location orthogonal),
  **0012** (data scope + the 7 quality resolutions).
- Status: design + breakdown for the build. The "Decisions for Gate 2" section (bottom) is what the
  orchestrator presents to the human before build.

---

## 1. The change in one paragraph

Today every engine entry point (`GearSelector.select`, `DefaultDpsEstimator.estimate`,
`BonusContext.from`) reads one profile straight off `TaskData`: `weakness` + `monsterDefence` +
`demon`/`dragon`/`kalphite`/`undead`. We add an optional `List<MonsterVariant>` under each task, introduce
a `MonsterProfile` value type that the engine reads instead of `TaskData`, and resolve the user-selected
variant -> `MonsterProfile` inside `LoadoutAdvisor` (which already orchestrates everything and already
threads a `selectedLocationName`). The combat maths is untouched; only the *source* of the
weakness/defence/flags moves from "the task" to "the selected variant". A task with no variants, or with
its default variant selected, reproduces today's loadout byte-for-byte (FR-6).

---

## 2. Data model

### 2.1 Types

`model/MonsterVariant.java` (new, Lombok `@Data @NoArgsConstructor`):

```
String  name;            // display name, e.g. "K'ril Tsutsaroth", "Greater abyssal demon"
List<Integer> npcIds;    // identity (may be empty/UNKNOWN -> omitted)
Integer combatLevel;     // display only (nullable)
Weakness weakness;       // nullable -> inherit task default (UNKNOWN-weakness fallback, ADR-0012.3)
MonsterDefence monsterDefence; // nullable -> inherit task default
boolean demon, dragon, kalphite, undead; // category flags FOR THIS VARIANT (default false)
boolean isBoss;          // FR-7 marker ("boss - separate trip")
boolean isDefault;       // OQ-2: the default selection; exactly one per multi-variant task
String  location;        // free-text location label (note only in v1, ADR-0011)
String  requirement;     // free-text gating (quest/item/slayer) - display only
```

`model/TaskData.java`: add `private List<MonsterVariant> variants;` (Gson defaults null -> behaves as
today). NOTHING else changes; the existing task-level profile fields stay and ARE the default variant's
profile.

`loadout/MonsterProfile.java` (new, immutable `@Value`): the unit the engine consumes.

```
Weakness weakness; MonsterDefence defence;
boolean demon, dragon, kalphite, undead, slayerHelmApplies;

static MonsterProfile fromTask(TaskData t)              // today's profile
static MonsterProfile fromVariant(TaskData t, MonsterVariant v)  // variant fields, per-field fallback to task
```

`fromVariant` fallback rule (per field): `v.weakness != null ? v.weakness : t.weakness`, same for
`monsterDefence`; category flags come from the VARIANT (`v.demon` etc.), `slayerHelmApplies` from the task.
This single rule implements ADR-0012.3 (UNKNOWN-weakness variants inherit the task default).

### 2.2 JSON shape (`slayer-data.json`)

A task gains an optional `variants` array. Existing 42 tasks with no `variants` key are unchanged. Example
(Greater demons, abbreviated):

```json
{
  "task": "Greater demons",
  "weakness": { "style": "MELEE", "element": null },
  "monsterDefence": { "defenceLevel": 70, "stab": 0, "slash": 0, "crush": 0, "magic": 0, "range": 0 },
  "demon": true,
  "variants": [
    { "name": "Greater demon", "isDefault": true, "demon": true,
      "weakness": { "style": "MELEE", "element": null },
      "monsterDefence": { "defenceLevel": 70, "stab": 0, "slash": 0, "crush": 0, "magic": 0, "range": 0 } },
    { "name": "K'ril Tsutsaroth", "isBoss": true, "demon": true, "npcIds": [3129],
      "weakness": { "style": "MELEE", "element": null },
      "monsterDefence": { "defenceLevel": 240, "stab": 0, "slash": 0, "crush": 0, "magic": 0, "range": 0 },
      "location": "God Wars Dungeon", "requirement": "boss - separate trip" },
    { "name": "Skotizo", "isBoss": true, "demon": true,
      "weakness": { "style": "MELEE", "element": null }, "...": "def 200, demonbane dominates (gaps tie-break)" }
  ]
}
```

The default variant's `weakness.style`/`monsterDefence` MUST equal the production task profile
(Greater demons is **MELEE**, like every demon task - section 3.1 / decision #11). The default variant
duplicates the task-level profile on purpose: it keeps the resolver uniform (always resolves to a variant)
AND keeps the task-level fields as the regression anchor / fallback source. The wiki's "Water 40%"
elemental note for Greater demons would map to `element` only, and only matters if a variant's style is
MAGIC - it never flips the demon tasks off MELEE.

### 2.3 Default-selection rule (OQ-2)

`resolveVariant(task, selectedName)`: if `selectedName` matches a variant by name -> that one; else the
`isDefault` variant; else the first variant; else (no variants) a synthetic profile from the task default.
Deterministic, validated (exactly one `isDefault` per multi-variant task).

---

## 3. Engine re-keying (the seam)

The re-key is "read the profile, not the task". Mechanically:

| Site | Today | After |
|------|-------|-------|
| `GearSelector.select(owned, task, player, mode, wilderness)` | reads `task.getWeakness()`, `task.getMonsterDefence()`, `BonusContext.from(task, wilderness)`, `task.getWeakness().getElement()` | `select(owned, MonsterProfile, player, mode, wilderness)` reads those off the profile |
| `DefaultDpsEstimator.estimate(style, worn, stats, task, spell)` | `BonusContext.from(task)`, `task.getMonsterDefence()` | `estimate(style, worn, stats, MonsterProfile, spell)` |
| `BonusContext.from(TaskData[, wilderness])` | reads task flags | add `from(MonsterProfile, wilderness)`; flags come from the profile |
| `ConsumableSelector.select(owned, style, element, stats, weaponId)` | UNCHANGED | advisor passes `profile.weakness.style`/`element` |
| `LoadoutAdvisor.recommend(...)` | `task.getWeakness()` etc. | resolves `MonsterProfile` from (task, selectedVariantName), drives the engine |

`MonsterProfile.fromTask(task)` is the one-line adapter that migrates every existing test call site; that
keeps the 42-task baseline green and IS the FR-6 regression check. `selectedVariantName` threads exactly
like the existing `selectedLocationName`: `LoadoutAdvisor.recommend(task, owned, stats, mode, haveCannon,
selectedLocationName, selectedVariantName)` (new last param; existing overloads delegate with `null`).

No combat-maths change. No new DPS mechanics (NG-4). Category bonuses follow the variant automatically
because `BonusContext` is built from the profile (FR-5).

### 3.1 Two weakness axes - DO NOT conflate (resolves the cross-check divergence)

The MV-R1 cross-check (`docs/variants/crosscheck/`) reports dragons as MAGIC-weak because the wiki lists
an "elemental weakness" (Water/Earth/Fire %), while production `slayer-data.json` marks Black/Blue/Red
dragons **RANGED**. These are two DIFFERENT concepts, and the engine's `Weakness` type already encodes
both as separate fields:

- **`Weakness.style`** (MELEE/RANGED/MAGIC) = which combat style to bring; drives `GearSelector`. Derived
  from the monster's defence profile / conventional best style.
- **`Weakness.element`** (string, e.g. "water") = the wiki "elemental weakness"; a MAGIC-only spell
  selector used by `ConsumableSelector` *when `style == MAGIC`*. The wiki's % magnitude is NOT modelled
  (no new combat maths, NG-4).

A wiki "elemental weakness of Water 40%" means *if you cast magic, prefer water spells* - it does NOT make
magic the best style for a Black dragon (you bring ranged). The cross-check conflated the two axes.
**Decision (FR-6-critical): PRESERVE the existing `style` for existing tasks (Black/Blue/Red dragons stay
RANGED - byte-identical, FR-6); map the wiki elemental weakness only onto the `element` field.** For new
variants/bosses the author sets `style` from the defence profile / convention (the same basis the 42-task
set used), and records `element` for magic relevance. Never flip an existing task's style from the wiki
elemental list. This is Gate-2 decision #11.

---

## 4. Variant x location axis (OQ-1) - see ADR-0011

**Orthogonal in v1.** Variant axis = the monster profile (drives the loadout). Location axis = the
existing rich `SlayerLocation` combo (multi/cannon/burst/konar/wilderness), UNCHANGED, and the
`wilderness` predicate keeps flowing from it (WDB-17 untouched). Pure location-renames that share a
profile are collapsed into the base variant at authoring time (they would produce an identical loadout).
Location-bound variants/bosses carry a free-text `location` NOTE (FR-7), not a rich location, in v1.
Rationale + rejected options (nested, keep-all): ADR-0011.

---

## 5. UI selector (for the Frontend engineer)

Mirror the existing `where-location-combo` precedent exactly (`SlayerPanel` inner section + the
non-destructive `reconcileCombo` pattern, lines ~369-499; `AllInSlayerPlugin.setOnSelectLocation` wiring,
lines ~97-100, 225/240/242).

- **Control:** a `JComboBox<String>` named `variant-combo`, placed ABOVE the location combo (you pick the
  monster, then where). Built once, reconciled non-destructively on render.
- **Population:** one entry per variant of the current task; boss entries rendered with a `" (Boss)"`
  suffix (FR-7). Hidden + inert when the task has <=1 variant (FR-3) - identical to how the location combo
  hides when there are no locations.
- **Default state:** the `isDefault` variant is pre-selected (FR-6); selection is reconciled from
  `state.getSelectedVariantName()`.
- **Callback:** `onSelectVariant: Consumer<String>` -> `AllInSlayerPlugin` sets `volatile
  selectedVariantName` and `recompute(RefreshSource.VARIANT_SELECT)` (re-render trigger).
- **Boss separateness (FR-7):** when the selected variant `isBoss`, the recommendation card shows a
  "Boss - separate trip" note (and the variant's `location`/`requirement` text), so a boss loadout is not
  mistaken for a grind loadout.
- **Reset:** `selectedVariantName` is cleared on task change and validated against the task's variants
  (mirrors `hasLocation` -> null at lines 242-244), so a stale selection never leaks across tasks.

`SlayerPanelState` gains `selectedVariantName` (and the panel reads the variant list off
`state.getTask().getVariants()`). `Recommendation` carries the resolved variant name + `isBoss` for the
card note. UI designer NOT required (contained control, manifest default = FE).

---

## 6. Data migration (the large authoring task)

From the flat 42-task dataset to the variant-aware shape:

- **Source -> JSON.** Each `variants-*.md` group file's `###` blocks become `variants` arrays under the
  matching `##` task. Parse `def_bonuses` (tolerate missing/`UNKNOWN` 5th value -> 0), `weakness` (resolve
  ties to a single style per ADR-0012), `categories` -> flags, `requirement`/`notes` -> isBoss
  (`"boss - separate trip"` / `notes: BOSS.`) and `requirement` text.
- **Apply the ADR-0012 resolutions:** drop reanimated; collapse same-profile location splits into the
  base variant (ADR-0011); UNKNOWN-weakness variants authored with null profile (inherit); multi-form
  bosses as per-form variants; Boss meta-task excluded; Gryphons base-only; add Frost Dragons (task 43).
- **Category flags = the ENGINE's meaning, NOT the raw wiki attribute.** `demon`/`dragon`/`kalphite`/
  `undead` mean "demonbane / dragonbane / Keris / Salve APPLIES", which is a curated subset. So:
  Hellhounds & Waterfiends have the wiki demon attribute but `demon=false` (demonbane does not apply -
  pinned set {Abyssal, Black, Greater demons, Nechryael}); Greater Skeleton Hellhound is `undead=true`,
  not demon; Vorkath is `dragon=true` AND `undead=true` (dragonbane and Salve both apply), boss; all
  dragons + Skeletal Wyvern + Vorkath + Frost = `dragon=true`. The author maps wiki categories through the
  engine's bonus-applicability lens, cross-checked against `docs/variants/crosscheck/`.
- **Weakness style preserved, element mapped (section 3.1):** never flip an existing task's `style` from
  the wiki elemental list; record the wiki elemental weakness only on `element`.
- **Mark exactly one `isDefault`** per multi-variant task (the base, non-boss monster).
- **Scope estimate:** ~215 enumerated blocks collapse to ~90-110 profile-distinct variants across 42->43
  tasks. This is the bulk of the build effort; it is split one task per group file (waves MV-D*) so each
  is independently testable against the validation harness, and `slayer-data.json` has a single owner per
  group to avoid concurrent edits (it is a hot file).

---

## 7. Requirements traceability (PRD -> plan)

Every MV-P1 FR/NG mapped to a task and a verification step. Zero unmapped IDs.

| Req | Verbatim acceptance criterion (abbrev.) | Task(s) | Verification |
|-----|------------------------------------------|---------|--------------|
| **FR-1** Variant-aware data: every task >=1 variant, each with non-null weakness/defence/flags; bosses appear as variants | MV-B1, MV-B6, MV-D1..D11 | `SlayerDataServiceTest`/`DuradelDatasetValidationTest`: every task has >=1 resolvable variant; Skotizo/K'ril/Tormented/Demonic gorilla present under parents |
| **FR-2** Authoritative Duradel list verified vs wiki (additions/removals explicit) | MV-D11, MV-B6 | dataset test asserts task set == MV-R1 list (Frost Dragons added -> 43) |
| **FR-3** Variant selector renders for >1 variant, hidden/inert for single | MV-FE1 | `SlayerPanelTest`: one entry per variant (multi); hidden+inert (single) |
| **FR-4** Re-drive on selection: two variants of one task -> different recommendation; chosen profile is the variant's | MV-B3, MV-B5 | `LoadoutAdvisorTest`/`GearSelectorTest`: style/weapon ranking FLIPS between two sibling profiles of one task |
| **FR-5** Category bonuses follow the variant | MV-B2, MV-B5 | test: a `demon` variant credits demonbane while a non-demon sibling under the same task does not |
| **FR-6** Default selection: non-null rec on fresh task; single-variant task == today | MV-B2..B5, MV-B6 | migrated baseline suite stays green via `MonsterProfile.fromTask`; default-variant test yields non-null rec |
| **FR-7** Boss separateness surfaced not hidden | MV-FE1, MV-FE2 | `SlayerPanelTest`: boss variant carries a "(Boss)" marker / "separate trip" note |
| **NG-1** No automation | (whole design) | advice-only; no equip/withdraw code added |
| **NG-2** No boss-fight modelling beyond existing axes | ADR-0012.5 | only profile re-drive; no prayer/supplies/phases |
| **NG-3** No in-fight switching | (design) | one loadout per selected variant |
| **NG-4** No new combat maths | section 3 | engine maths untouched; profile is the only new input |
| **NG-5** Data-model design deferred to here | this doc + ADR-0010/0011/0012 | resolved |
| **NG-6** Masters other than Duradel/Kuradal out of scope | MV-D* | enumeration is Duradel-only |
| **NG-7** Rune-pouch contents not read | (inherited) | unchanged |

---

## 8. Dependency-ordered TDD breakdown

Each task is red-green-refactor, small, with an explicit exit condition. Waves are dependency-ordered;
within a wave, `slayer-data.json` and the panel are single-owner (hot files).

### Wave 0 - model + seam (no behaviour change; the regression anchor)

- **MV-B1 - `MonsterVariant` type + `TaskData.variants`.** TDD: `TaskDataJsonTest` round-trips a `variants`
  block; a task with no `variants` key loads with `variants == null` (back-compat). *Exit:* existing 42
  tasks load byte-identical; a variant block parses.
- **MV-B2 - `MonsterProfile` + `BonusContext.from(MonsterProfile, wilderness)`.** TDD:
  `fromTask` equals today's weakness/defence/flags; `fromVariant` overlays variant fields; a null-weakness
  variant falls back to the task; `BonusContext` from a demon variant has `demon=true`, from a non-demon
  sibling `demon=false`. *Exit:* FR-5 unit-level proven; FR-6 fallback proven.
- **MV-B3 - re-key `GearSelector` to `MonsterProfile`.** TDD: migrate `GearSelectorTest` via
  `MonsterProfile.fromTask` (stays green = FR-6); add a test where two profiles under one task with
  different defence flip the melee attack type / weapon ranking (FR-4). *Exit:* baseline ranking unchanged
  at default profile; flip proven.
- **MV-B4 - re-key `DefaultDpsEstimator.estimate` to `MonsterProfile`.** TDD: migrate
  `DefaultDpsEstimatorTest` green; add a test where Est. DPS differs between two variant profiles (defence
  + category multiplier) (FR-5). *Exit:* display DPS uses the selected profile.
- **MV-B5 - `LoadoutAdvisor` resolves the selected variant -> profile.** New `selectedVariantName` param +
  `resolveVariant` + default rule. TDD: boss vs base variant of one task -> different recommendation
  (FR-4); category bonus follows variant (FR-5); no-variant / single-variant task == today (FR-6);
  default-variant non-null on fresh task (FR-6). *Exit:* relative-flip acceptance green; baseline green.

### Wave 1 - validation harness + data authoring (large)

- **MV-B6 - dataset-validation harness (authored first, red).** Assert: every Duradel task has >=1
  resolvable variant; every variant has a resolved-or-inherited profile; exactly one `isDefault` per
  multi-variant task; named bosses present under parents; no reanimated variant selectable; Duradel task
  set == MV-R1 list. *Exit:* the harness compiles and fails until data is authored (drives MV-D*).
- **MV-D1..D10 - author variants, one per group file** (demons, dragons, wyrms-wyverns,
  undead-basilisks, kalphite-misc, giants-beasts, devils-aerial, humanoids, misc). Each applies the
  ADR-0012 resolutions (exclude reanimated, collapse same-profile location splits, mark bosses/superiors,
  per-form multi-form bosses, weakness tie-breaks). Single-owner edits to `slayer-data.json`. *Exit:* that
  group's slice of MV-B6 goes green.
- **MV-D11 - add Frost Dragons (task 43) + update the task-set assertion.** *Exit:* FR-2 task set ==
  MV-R1 list.

### Wave 2 - wiring + UI

- **MV-B7 - thread `selectedVariantName` end to end.** `AllInSlayerPlugin` volatile field + reset on task
  change + validate against `task.variants` + `RefreshSource.VARIANT_SELECT`; `SlayerPanelState
  .selectedVariantName`; `Recommendation` carries resolved variant name + `isBoss`. TDD: state factory
  carries the selected variant; reset-on-task-change unit test. *Exit:* recompute passes the selected
  variant to the advisor.
- **MV-FE1 - variant `JComboBox` in `SlayerPanel`.** Modelled on `where-location-combo`: populate from
  variants, hide when <=1, "(Boss)" suffix, `onSelectVariant` callback, non-destructive reconcile. TDD
  `SlayerPanelTest`: one entry per variant (multi); hidden+inert (single) (FR-3); boss marker present
  (FR-7); selection fires the callback. *Exit:* FR-3 green.
- **MV-FE2 - boss separateness note (FR-7).** When the selected variant `isBoss`, the card shows a
  "Boss - separate trip" note + the variant's location/requirement. TDD `SlayerPanelTest`: note present
  for a boss selection, absent otherwise. *Exit:* FR-7 green.

**Dependency order:** B1 -> B2 -> {B3, B4} -> B5 -> B6 -> {D1..D11} -> B7 -> {FE1 -> FE2}. B3/B4 are
parallelizable (different files); D1..D11 are parallelizable in authoring but serialize on the
`slayer-data.json` write (one owner at a time per the hot-file rule).

## 9. Test strategy

- **Relative-behaviour acceptance, never fragile absolute numbers** (R-1, mirrors the WDB discipline):
  FR-4/FR-5 assert a style/weapon/ranking FLIP between two sibling variant profiles of one task, plus that
  the consumed profile is the variant's not the task's. These stay valid as exact stats are corrected.
- **FR-6 regression anchor:** every existing engine test is migrated by wrapping its `TaskData` in
  `MonsterProfile.fromTask(task)`; the full suite staying green is the proof that the default path is
  byte-identical to today. **Add a guard test that pins existing tasks' `weakness.style` post-migration**
  (Black/Blue/Red dragons stay RANGED, Metal dragons MELEE, etc.) so the cross-check elemental-weakness
  data can never silently flip a production style (decision #11).
- **Dataset validation (FR-1/FR-2):** structural assertions only (presence, resolvability, exactly-one
  default, named bosses, task-set equality) - no per-monster stat hard-asserts beyond what MV-R1 verified.
- Run `./gradlew cleanTest test`; report counts, never assert "passes" without the output (culture Bar).

---

## 10. Decisions for Gate 2

Each load-bearing decision with my RECOMMENDATION + 1-line rationale, for the orchestrator to present.

1. **OQ-1 - variant x location axis.** REC: **orthogonal in v1** (variant = profile, existing location
   combo unchanged; collapse same-profile location splits; bosses' location is a note). Rationale: zero
   re-key of the working wilderness/location machinery, short usable combo; nested is the future path
   (ADR-0011).
2. **OQ-2 - default-selection rule.** REC: **the `isDefault` (base, non-boss) variant**, validated
   exactly-one per task, fallback first-listed. Rationale: deterministic + its profile == task default, so
   a fresh task reproduces today's loadout (FR-6).
3. **Boss meta-task in/out.** REC: **OUT of variant selection in v1.** Rationale: 32-boss pool, no
   enumerated stats, bespoke trips; the task-alternative bosses still participate under their parents.
4. **Reanimated monsters.** REC: **EXCLUDE** (incl. Reanimated kalphite, consciously). Rationale: they do
   not count toward tasks.
5. **UNKNOWN-weakness variants (Vampyres, Bloodveld, GWD/Reanimated hellhounds).** REC: **inherit the task
   default profile** (null variant weakness -> fallback). Rationale: keeps them selectable for
   identity/location without fabricating a weakness.
6. **Multi-form bosses (Dagannoth Kings, Kalphite Queen).** REC: **per-form variants** (DK Rex/Prime/
   Supreme; KQ crawling/airborne). Rationale: distinct profiles want distinct loadouts; the model already
   supports it.
7. **Weakness tie-breaks / magic-element vs defence ambiguities** (Skotizo, Demonic gorilla, Araxyte,
   etc.). REC: **encode a single resolved weakness per variant** per `gaps-research.md` (e.g. Skotizo ->
   MELEE, demonbane dominates). Rationale: the engine needs one style; the tie reasoning is recorded, not
   modelled.
8. **Gryphons (Dire superior / Shellbane boss, stats UNKNOWN).** REC: **base-only in v1**, defer
   Dire/Shellbane. Rationale: no fabricated profile (R-1 discipline).
9. **Frost Dragons (only Duradel task missing from the 42-set).** REC: **ADD as task 43** (draconic).
   Rationale: FR-2 (authoritative list) requires it; additions must be explicit.
10. **Engine seam shape.** REC: **re-key the engine onto `MonsterProfile`** (vs overlaying a synthetic
    `TaskData`). Rationale: honest unit, no fragile bean-copy; `MonsterProfile.fromTask` keeps the
    baseline green (ADR-0010).
11. **Weakness model: style vs wiki elemental weakness (FR-6-CRITICAL).** The cross-check reports dragons
    MAGIC (wiki elemental weakness); production marks them RANGED. REC: **PRESERVE the existing `style`
    (Black/Blue/Red dragons stay RANGED, byte-identical FR-6); map the wiki elemental weakness only onto
    the `element` field** (a magic-only spell selector; % not modelled, NG-4). Rationale: the wiki
    "elemental weakness" and the engine's combat-style weakness are two different axes the `Weakness` type
    already separates; "has a Water weakness" does not mean magic is the best style. New variants set
    `style` from the defence profile / convention, `element` for magic relevance. Never silently flip an
    existing task's style. (section 3.1)
12. **Cross-check category corrections** (fold into data authoring, ADR-0012). REC: **accept** - Vorkath
    `dragon=true`+`undead=true` (boss); Greater Skeleton Hellhound `undead=true` not demon; Hellhounds &
    Waterfiends `demon=false` despite the wiki attribute (demonbane does not apply); all dragons +
    Skeletal Wyvern + Vorkath + Frost = `dragon=true`. Rationale: engine flags mean "this bonus applies",
    a curated subset, not the raw wiki attribute.

---

## MV-A2 amendments (Gate-2) - human-overturned decisions

The MV-A1 design above is the approved base. At Gate 2 the human revised two areas. This section amends
the design. New ADRs: **0013** (user-selected combat method; element wiki-accurate; default method =
recommended style - supersedes the ADR-0010 "engine fixes the style" stance) and **0014** (Boss task
included, supersedes the "Boss OUT" resolution in decision #3 / ADR-0012.5).

> **History / superseded steers (recorded so nobody relitigates):** an interim framing "re-author every
> task's STYLE to the wiki elemental icon" was raised and then REJECTED by the user - flipping a
> recommended combat style to its elemental-weakness icon is a category error that worsens default
> loadouts (the two `Weakness` axes are distinct, ADR-0012.8). The final resolution is the combat-method
> selector below: styles are PRESERVED as the default and OFFERED as user-selectable options.

### Amendment 1 + 3 (folded) - combat METHOD is a user-selected axis (ADR-0013)

The plugin does not lock the player to one style. For the selected variant it OFFERS melee / ranged /
magic as a selectable method, defaults to the variant's recommended (efficient) style, and re-drives the
loadout for whichever method the user picks. This is a NEW selection axis alongside variant + location.

- **Method is a selected INPUT, not a fixed engine field.** `LoadoutAdvisor` resolves the EFFECTIVE style
  = `selectedMethod ?? recommendedStyle` and drives the engine with it, OVERRIDING the
  defence/convention-derived style. `monsterDefence`'s lowest of stab/slash/crush still picks the melee
  attack type WITHIN the chosen style; `element` narrows the spell only when the active method is magic;
  category flags still gate the bane bonuses. No combat-maths change (NG-4).
- **Default method = the recommended style.** For the 42 existing tasks this is today's curated
  `weakness.style`, so a fresh task / default variant / default method reproduces today's loadout -
  **FR-6 byte-identical is PRESERVED BY DEFAULT** (it changes only on a method switch). New variants/bosses
  carry their OWN recommended style.
- **Styles PRESERVED on existing tasks** (reaffirms ADR-0012.8 / decision #11; the style-pinning guard
  test stays, pinning the existing curated styles). The only change is that the user may OVERRIDE the
  style via the method selector; the stored recommended style does not flip.
- **`element` refined to wiki-accurate, per variant**, so the magic method picks the right spell. Mostly
  inert at the default method (style != MAGIC) except for tasks whose recommended style is already MAGIC,
  where it is a deliberate small improvement (right spell) - guarded by a test. A variant with no wiki
  elemental weakness keeps the existing fallback (air / best unresisted) so the magic method always works.
- **`MonsterProfile` = defence + category flags + element + recommended (default) style.** STYLE-TO-USE
  comes from the method selector. `monsterDefence` and the 42 existing task profiles are NOT re-authored.

**The five load-bearing method-selector decisions (my recs; the orchestrator surfaces the flagged ones):**

1. **Which methods to offer per variant.** REC: **offer all three always; DISABLE/hide only a method that
   yields no viable loadout** (the player owns no weapon for that style - `GearSelector` already returns
   empty worn). Surface monster resistance via the Est. DPS number, NOT by hiding. Defer monster
   magic-immunity hiding (no immunity data; no fabrication). **FLAG to user** (offer-all vs filter-to-sensible).
2. **Default-method rule.** REC: **the variant's recommended style** (existing tasks = today's curated
   `weakness.style`; new variants = their authored style). Preserves FR-6 at the default.
3. **UI control.** REC: a third control **`method-combo`** (`JComboBox<String>` Melee/Ranged/Magic) below
   `variant-combo` and above the location combo (pick monster -> method -> where), built once + reconciled
   non-destructively like the other combos, default selection = recommended style. A 3-button segmented
   toggle is the more ergonomic alternative; combo chosen for precedent + lowest FE risk. **FLAG to user**
   (combo vs segmented toggle). Methods with no viable loadout are rendered disabled.
4. **Engine override.** REC: `GearSelector.select` takes an explicit effective `CombatStyle` (sourced by
   the advisor from `selectedMethod ?? recommendedStyle`) INSTEAD of deriving it via `styleOf`; the
   within-style `meleeAttackType` argmin over `monsterDefence`, the `element`-based spell pick, and the
   category-bonus gating are all UNCHANGED. `DefaultDpsEstimator.estimate(style, ...)` already takes a
   style param - the advisor passes the selected method.
5. **Composition with Boss + multi-variant.** REC: three ORTHOGONAL axes - variant picks the profile
   (defence + category + element + recommendedStyle), method picks the style-to-use, location picks where.
   `selectedMethod` is per the selected variant and RESETS to that variant's recommended style on variant
   change AND task change (mirrors `selectedLocationName`'s reset). For the Boss task each boss variant
   carries its own recommended style; the method selector works identically (user may switch).

### Re-framed FR-6

> **FR-6 (new):** at the DEFAULT method, a single-variant / default-variant task reproduces today's
> loadout (byte-identical preserved); selecting a different method re-drives the loadout for that style.
> The relative-behaviour acceptance (FR-4/FR-5 flips between two sibling variant profiles) still holds.

### Amendment 2 - the 'Boss' slayer task is IN variant selection (overturns decision #3, ADR-0014)

The Boss task participates; its variants = the assignable boss pool (`variants-boss.md`, ~31
Duradel-rollable bosses; MV-R2 enumerates each boss's defence/weakness in parallel).

- **Boss task in the dataset.** A task whose EVERY variant `isBoss`. The Boss task-level
  `weakness`/`monsterDefence` are **null** (a meta-task has no own profile). The harness EXEMPTS the Boss
  task from the "task-level profile non-null" assertion while REQUIRING each of its variants to carry a
  non-null resolved profile.
- **Each boss carries its OWN profile - no fabrication.** A boss variant must have non-null
  weakness+defence (else it resolves to a null profile -> no recommendation, since there is no meaningful
  Boss task-level fallback). A boss MV-R2 has not yet enumerated is **DEFERRED** (omitted, logged), never
  shipped with a fabricated or null profile (Silverlight / Dire-gryphon discipline).
- **Default selection for an all-boss task.** `resolveVariant` always lands on a boss variant: the
  live-rolled boss if known, else the `isDefault` boss, else the first listed. `isDefault` marks a
  deterministic fallback (first listed) only to satisfy the exactly-one-default rule; live pre-selection
  overrides it.
- **Live pre-selection from varbit `SLAYER_TARGET_BOSSID` (4723).** `MonsterVariant` gains an optional
  `bossId`. When the task is Boss and the user has not overridden, the plugin reads varbit 4723, matches
  it to a boss variant's `bossId`, and seeds `selectedVariantName` (mirrors the location-combo auto-seed);
  unavailable/unmapped -> deterministic default. **The exact bossId -> boss mapping is a LIVE-VERIFY item**
  (the varbit may be a slayer enum, not an NPC id); unmapped values fall through safely - do not fabricate.
- **Boss separateness (FR-7) already covered:** every boss variant is `isBoss` + "separate trip", so the
  existing FR-7 card-note design surfaces it with no new mechanism.
- **Orthogonal-axis consistency (ADR-0011) preserved:** a boss's place is a free-text `location` NOTE, not
  a rich `SlayerLocation`; the location/wilderness machinery is untouched.

### New / changed build sub-tasks (append/adjust to section 8)

- **MV-B9 (new) - method axis through the engine + advisor.** `GearSelector.select` takes an explicit
  effective `CombatStyle` (replacing the internal `styleOf` derivation); `LoadoutAdvisor` resolves
  `selectedMethod ?? recommendedStyle` and passes it (and to `DefaultDpsEstimator.estimate`, which already
  takes a style). Expose the recommended style on `MonsterProfile`/`MonsterVariant`. TDD: forcing each of
  the 3 methods on one variant re-drives best-in-style gear; default method == recommended style ==
  today's pick (FR-6); a method with no owned weapon yields empty worn (drives the UI disable). *Exit:*
  method override proven; default byte-identical.
- **MV-FE3 (new) - `method-combo` control.** `JComboBox<String>` Melee/Ranged/Magic below `variant-combo`,
  default = recommended style, non-destructive reconcile, resets on variant/task change, `onSelectMethod`
  callback -> `selectedMethod` volatile + `RefreshSource.METHOD_SELECT`; non-viable methods disabled. TDD
  `SlayerPanelTest`: three entries, default selection, callback fires, disabled when no owned weapon.
  *Exit:* method selectable in the panel.
- **MV-D12 (new) - author the Boss task + its boss variants** from MV-R2's `variants-boss.md` (enumerated
  stats). Single-owner `slayer-data.json` edit. Each boss: `isBoss=true`, `requirement` "boss - separate
  trip", `location`, category flags, `bossId`, OWN `weakness`(recommended style + element)/`monsterDefence`.
  Boss task-level profile null. Defer (log) un-enumerated bosses. *Exit:* the all-boss-task slice of MV-B6
  goes green.
- **MV-B8 (new) - boss live pre-selection.** Add `MonsterVariant.bossId`; extend `TaskDetector` /
  `AllInSlayerPlugin` to read varbit 4723 when the task is Boss and seed `selectedVariantName`
  (validated against the task's variants; user override wins; unmapped -> deterministic default). TDD:
  unit-test the bossId->variant resolver (table) + the "no override -> seeded, override -> kept" path; the
  live varbit read is field-injected/client-thread -> manual checklist. *Exit:* a known bossId pre-selects
  its variant headlessly.
- **MV-B3 / MV-B4 (UNCHANGED from MV-A1) - "migrate green".** `MonsterProfile.fromTask` keeps the baseline
  byte-identical at the default method; no baseline-assertion churn (styles are preserved).
- **MV-D1..D11 (scope: add variants + REFINE element only).** They ADD variants and refine each task's
  `element` to wiki-accurate; they do NOT re-author task-level `style`/`monsterDefence` (preserved).
  Single-owner `slayer-data.json` as before.
- **MV-B6 (scope adjusted) - validation rules.** (a) the style-pinning guard test STAYS, pinning the
  EXISTING curated styles (preserved); (b) every existing task has a non-null wiki-accurate `element` where
  a magic method needs one (existing magic-task rule retained, extended to refined values); (c) Boss task
  exempt from task-level-non-null but every Boss variant non-null; (d) all-boss-task rule (every variant
  isBoss, one isDefault fallback, deferred bosses logged).
- **MV-D11 (unchanged):** add Frost Dragons (task 43) - recommended style + element by the same rule.

**Revised dependency order:** B1 -> B2 -> {B3, B4} -> B5 -> B6 -> {D1..D12} -> {B7, B8, B9} -> {FE1 ->
FE2, FE3}. B8 depends on B7 (both thread selection state); B9 (method) is parallel to B7/B8 (different
seam) but shares `LoadoutAdvisor` so serialize the advisor edit; D12 serializes on the `slayer-data.json`
write like the other D* tasks.

### Revised section 7 traceability rows

- **FR-6** row replacement: *Verification* = "at the DEFAULT method a default-variant task reproduces
  today's loadout (migrated baseline suite green, byte-identical); selecting another method re-drives the
  loadout; FR-4/FR-5 relative flips green." Tasks: MV-B2..B5, MV-B9, MV-B6.
- **FR-1** row addition: the Boss task (every variant isBoss, each with a resolved non-null profile,
  deferred bosses logged) - Tasks MV-D12, MV-B6.
- **FR-4** row additions: forcing a method re-drives best-in-style gear (MV-B9); live boss pre-selection
  from varbit 4723 (MV-B8) re-drives onto the rolled boss's profile.
- **New (method axis):** a method selector offers melee/ranged/magic, defaults to the recommended style,
  disables non-viable methods - Tasks MV-B9, MV-FE3.

### Revised section 9 test strategy

- **FR-6 byte-identical is PRESERVED at the default method.** The existing engine tests migrate green via
  `MonsterProfile.fromTask` (no baseline-assertion churn); the style-pinning guard test stays pinning the
  EXISTING styles. New tests cover (a) the method override re-driving best-in-style gear (MV-B9), (b) the
  default method == recommended style, (c) a non-viable method yielding empty worn, (d) the `element`
  refinement changing the spell only when the active method is magic.
- **Relative-behaviour acceptance is unchanged** (FR-4/FR-5 flip between two sibling variant profiles; the
  consumed profile is the variant's not the task's). These stay valid as exact stats are corrected.
- **Boss task (MV-D12/MV-B6):** structural assertions - every variant isBoss + non-null profile, one
  isDefault fallback, deferred bosses logged; plus the MV-B8 bossId->variant resolver table. The live
  varbit-4723 read is a QA manual-checklist item (cannot be confirmed headless).
- Run `./gradlew cleanTest test`; report counts, never assert "passes" without the output.

---

## Strategy-Guide Gear (MV-S) - Gate-4-retriage increment (ADR-0015)

A new override layer on top of the shipped variant feature. When a monster has a wiki `/Strategies` page,
the guide's gear OVERRIDES the computed weapon/method; the stat engine fills every other slot from owned
items and falls back to the DPS pick where the guide is silent. Driven by four user Gate-0 decisions
(strategy overrides weapon+method; bank-aware ownership; multi-style = the strategy drives EVERY documented
style + a note listing all of them; method-toggle re-drives across documented styles). New ADR: **0015**. This section amends sections 2 (data model), 3/5 (engine seam + UI), 7
(traceability), and 8 (build breakdown).

### S.1 The change in one paragraph

The DPS weapon ranking (ADR-0008/0009) picks the highest-DPS owned weapon for the effective style. The
wiki sometimes prescribes a different weapon (Emberlight on a demon boss, not the higher-raw-DPS Fang) and
a melee+ranged mix the single-loadout engine cannot express. We add an optional nested
`MonsterVariant.strategy` = { `primaryStyle`, `primaryWeapons` (name+id, priority order),
`secondaryWeapons` (name+id+style), `note`, `sourceUrl` }. **AMENDED (user decision, supersedes the original
"primary only" gate):** the strategy drives EVERY style it documents. Build a `style -> strategyWeapons` map
= {`primaryStyle` -> `primaryWeapons`} union {each `secondaryWeapon` -> its own `style`}. When the selected
variant has a strategy AND the effective method is a style the strategy documents AND the player OWNS a
strategy weapon for that style, the highest-priority owned one wins the WEAPON slot, bypassing the DPS
ranking. An undocumented method (e.g. magic on a melee+ranged strategy), or no owned documented weapon ->
stat engine. Everything else is unchanged. Variants with no strategy behave exactly as today (no regression).

### S.2 Data model (amends section 2)

`model/MonsterStrategy.java` (new, Lombok `@Data @NoArgsConstructor`):

```
CombatStyle          primaryStyle;       // the default method this strategy sets
List<StrategyWeapon> primaryWeapons;     // priority order; highest-priority OWNED one wins the WEAPON slot
List<StrategyWeapon> secondaryWeapons;   // each carries its own style; documents extra methods (note + override when that method is active)
String               note;               // free-text "Wiki strategy" guidance line (nullable)
String               sourceUrl;          // the /Strategies page (provenance; display/audit only)
```

`model/StrategyWeapon.java` (new): `String name; Integer itemId; CombatStyle style;` (`style` nullable on
primaries - they use the strategy's `primaryStyle`).

`model/MonsterVariant.java`: add `private MonsterStrategy strategy;` (Gson defaults null -> behaves as
today; variants without a strategy are unchanged). NOTHING else changes.

- **Lives nested under the variant** (not a parallel dataset): locality beats a join key; `resolve(...)`
  already returns the variant, which carries its strategy. ADR-0015 decision.
- **Name -> id resolved at AUTHORING time** from `docs/loadout/weapon-reference-1h.md`/`-2h.md` (the
  name->id source) + baked as raw ints, matched against `owned.ids()`. No runtime name lookup (consistent
  with `WeaponEffectRegistry`/`ConditionalBonusRegistry` raw-id keying). Charge/recolour id variants a
  player might own are listed as extra priority entries; we do NOT canonicalise via `ItemVariationMapping`
  (LFB proved that collapse erases meaningful state).
- **The DEFAULT method is DERIVED from `strategy.primaryStyle` at advise-time** (`LoadoutAdvisor`:
  `defaultMethod = validStrategy != null ? strategy.primaryStyle : recommendedStyle`), NOT by requiring the
  authored `weakness.style` to match. So `weakness.style` and `strategy.primaryStyle` MAY differ - the guide
  wins for the default ("strategy overrides method"). This handles the ~7 guide-vs-weakness mismatches the
  boss research found (Abyssal Sire magic->melee, Vorkath ->ranged, Callisto/Chaos Elemental magic->ranged,
  Crazy archaeologist ->magic, Kree'arra ->magic, GG-Dawn ->ranged, Barrows Dharok/Guthan/Torag ->magic)
  automatically. MV-B6 validation must NOT assert `weakness.style == primaryStyle` (they are allowed to
  differ); it asserts only the structural validity (non-empty `primaryWeapons` with resolved ids, secondary
  weapons carry a style). The variant's authored `weakness.style` is never mutated.

### S.3 Engine override seam (amends section 3)

Policy in the advisor, mechanism in the selector (the same split as every other engine input):

- **`LoadoutAdvisor` decides whether the strategy APPLIES (AMENDED multi-style rule).** After resolving the
  variant + effective style, compute the override list from the `style -> strategyWeapons` map = {`primaryStyle`
  -> `primaryWeapons`} union {each `secondaryWeapon` -> its `style`}: if `variant.strategy != null` AND the
  effective style is documented (it is the `primaryStyle`, served by `primaryWeapons`, OR a secondary weapon's
  `style`, served by that weapon) -> the priority-ordered `List<Integer>` of that style's weapon ids; else an
  empty list. Malformed strategy (null `primaryStyle` / empty `primaryWeapons`) -> empty list + `log.warn`
  once.
- **`GearSelector.select` gains an overload** `select(owned, profile, style, player, mode, wilderness,
  List<Integer> strategyWeaponIds)`. Existing overloads delegate with an empty list (additive, back-compat,
  mirrors how `wilderness` was added). When the list is non-empty, the weapon/shield interplay
  (`resolveWeaponAndShield`) FIRST tries the override: walk the ids in priority order, pick the first that
  is owned AND a viable WEAPON candidate (present in `scored`, `weaponRank > 0`); that id wins the WEAPON
  slot, bypassing the DPS ranking. If the strategy weapon is 2h, no shield; if 1h, keep the stat-picked best
  shield (reuse the existing `putOneHandAndShield` / 2h logic). If NO override id is owned-and-viable -> fall
  through to the normal DPS ranking (bank-aware fallback).
- **Composition with category bonuses / WeaponEffect (the DPS display is correct for free).** The override
  only substitutes the WEAPON id in the worn map. `DefaultDpsEstimator.estimate` already credits the worn
  weapon's `WeaponEffect` and the max-applicable `ConditionalBonus` over equipped items (WDB-8/LFB-5), so an
  Emberlight worn on a demon variant displays its demonbane-credited DPS with no extra wiring. No
  combat-maths change (NG-4); no new double-count (the substitution path is identical to any worn weapon).
- **Method-toggle interaction (AMENDED, Gate-0 decision #4):** the gate is "the effective style is DOCUMENTED
  by the strategy". Switching the method to another DOCUMENTED style (e.g. ranged on a melee+ranged demon
  strategy) re-drives the override to that style's weapon (Method=Ranged -> Scorching bow). Switching to an
  UNDOCUMENTED style (e.g. magic) yields an empty override list -> the stat engine drives that style normally.
  The example: Tormented Demon Method=Melee -> Emberlight, Method=Ranged -> Scorching bow, Method=Magic -> stat
  engine. (Supersedes the original "gate exactly on `primaryStyle`, secondaries are note-only" rule.)

### S.4 UI - the "Wiki strategy" note (amends section 5)

Mirror the existing boss-note pattern (`SlayerPanel.bossNote` / `wrappingNote`, lines ~694-700, ~1014):

- `Recommendation` gains `String strategyNote`. `LoadoutAdvisor` composes it whenever the resolved variant
  has a strategy: `"Wiki strategy: <primary weapon(s)> (<primaryStyle>)"` + `"; also <secondary weapon(s)>
  (<style>)"` for each secondary + the free-text `note`. Null when the variant has no strategy.
- `SlayerPanel` renders it as a `loadout-strategy-note` wrapping note, placed with the boss note (above the
  worn grid) so it reads as context for the loadout. Rendered whenever `rec.getStrategyNote() != null` -
  i.e. INFORMATIONAL even when the player owns none of the strategy weapons (the loadout still recommends
  only owned gear; the note tells the user what the wiki recommends).
- No new control, no UI-designer involvement (a single note line; FE owns it).

### S.5 Fallbacks & honesty (Gate-0 #2)

| Situation | Behaviour |
|-----------|-----------|
| No `/Strategies` page (no `strategy`) | stat engine, byte-identical to today |
| Strategy + a DOCUMENTED style active + >=1 weapon for that style owned | highest-priority owned-and-viable wins WEAPON slot |
| Strategy + NO documented weapon for the active style owned | stat-driven best owned weapon (bank-aware fallback) |
| Method is an UNDOCUMENTED style (not primary, no secondary for it) | no weapon override; stat engine for the chosen style |
| Malformed strategy | treated as no strategy, stat engine, `log.warn` once |
| Any strategy present | "Wiki strategy" note shown (informational, ownership-independent) |

### S.6 Test strategy (relative/behavioural, mirrors section 9)

- **The Fang-vs-Emberlight case (the headline unit test, FR-S1):** a demon variant with a strategy
  (`primaryStyle=MELEE`, `primaryWeapons=[Emberlight]`) + the player owns BOTH Emberlight and a higher-raw-DPS
  Fang -> **Emberlight is recommended in the WEAPON slot** (the override beats the DPS ranking). Relative,
  not an absolute DPS assert.
- **Ownership fallback (FR-S2):** same strategy, player owns the Fang but NOT Emberlight -> falls back to the
  stat-driven best owned weapon (the Fang). No unowned gear recommended.
- **Method toggle across documented styles + off (FR-S3, AMENDED):** a melee+ranged strategy (Emberlight
  primary MELEE + Scorching bow secondary RANGED). Method=Melee -> Emberlight; Method=Ranged -> Scorching bow
  (the override follows the documented secondary style); Method=Magic (UNDOCUMENTED) -> no override, stat
  engine drives. The original "any non-primary method disables the override" is superseded - only undocumented
  styles disable it.
- **No-strategy regression (FR-S4):** a variant with no `strategy` (empty override list) -> recommendation
  byte-identical to today (the FR-6 anchor extends; the full migrated suite stays green).
- **Note (FR-S5):** `SlayerPanelTest` - a strategy variant renders the `loadout-strategy-note` with the
  primary + secondary weapons; a non-strategy variant has no such note.
- **Data validation (MV-B6 extension):** every authored `strategy` has a non-null `primaryStyle` and a
  non-empty `primaryWeapons` with resolved ids; secondary weapons carry a style. Do NOT assert
  `strategy.primaryStyle == variant.weakness.style` - they are allowed to differ (the guide overrides the
  default method; ~7 mismatch cases are intentional).

### S.7 Build sub-task breakdown (dependency-ordered, TDD; appends to section 8)

- **MV-S1 - model + parse.** `MonsterStrategy` + `StrategyWeapon` types; `MonsterVariant.strategy` field.
  TDD: `TaskDataJsonTest` round-trips a `strategy` block; a variant with no `strategy` key loads with
  `strategy == null` (back-compat). *Exit:* shape parses, no regression.
- **MV-S2 - engine override seam.** `GearSelector.select` strategy-ids overload + override-first logic in
  `resolveWeaponAndShield`; `LoadoutAdvisor` computes the override list per the AMENDED multi-style rule
  (`style -> strategyWeapons` map). TDD: FR-S1 (Emberlight beats Fang), FR-S2 (ownership fallback), FR-S3
  amended (method drives every documented style; undocumented -> stat engine), FR-S4 (no-strategy
  byte-identical). *Exit:* the override + all fallbacks proven; baseline green. **(Done: 370 tests green.)**
- **MV-S3 - data authoring** from `docs/variants/strategies/strategies-*.md` (MV-SR, in parallel). Attach
  `strategy` blocks to the variants that have a `/Strategies` page (demon bosses first: Tormented Demon,
  Skotizo; then the rest MV-SR enumerates). Single-owner `slayer-data.json` edit. Resolve names->ids from
  the weapon-reference docs. *Exit:* MV-B6's strategy-validation slice goes green; the live Fang/Emberlight
  case is real data, not just a synthetic test.
- **MV-S4 - UI note.** `Recommendation.strategyNote` (composed by the advisor) + the `loadout-strategy-note`
  panel row. TDD `SlayerPanelTest`: note present for a strategy variant, absent otherwise (FR-S5). *Exit:*
  the Wiki-strategy line renders.

**Dependency order:** MV-S1 -> MV-S2 -> {MV-S3 (data), MV-S4 (UI)}. MV-S3 and MV-S4 are parallel after the
seam exists; MV-S3 serializes on the `slayer-data.json` write like the other D* tasks. MV-S2 shares
`LoadoutAdvisor` + `GearSelector` - single-owner sequential.

### S.8 Traceability (appends to section 7)

| Req | Acceptance criterion | Task(s) | Verification |
|-----|----------------------|---------|--------------|
| **FR-S1** Strategy weapon overrides the DPS pick when owned + a documented style | MV-S2, MV-S3 | `GearSelectorTest`/`LoadoutAdvisorTest`: Emberlight beats higher-DPS Fang on a demon strategy variant |
| **FR-S2** Bank-aware: unowned strategy weapon -> stat-driven fallback | MV-S2 | test: own Fang not Emberlight -> Fang recommended; no unowned gear |
| **FR-S3** (AMENDED) Method drives EVERY documented style; an undocumented style disables the override | MV-S2 | test: Method=Melee->Emberlight, Method=Ranged->Scorching bow, Method=Magic->stat engine |
| **FR-S4** No-strategy variant unchanged (regression) | MV-S1, MV-S2 | migrated suite green; empty override = today's path |
| **FR-S5** Multi-style surfaced as a "Wiki strategy" note, not a 2nd loadout | MV-S4 | `SlayerPanelTest`: note lists primary + secondary; one loadout only |


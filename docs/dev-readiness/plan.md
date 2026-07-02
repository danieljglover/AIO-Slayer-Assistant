# DT-A1 Plan - Location by Variant + Loadout/Location Coupling

Design: `docs/adr/0017-variant-location-suggestion.md` (amends ADR-0011). Scope contract:
`docs/dev-readiness/readiness-gaps.md`. Grounding: `data-audit.md`, `code-flow-audit.md`.

Builds ON (in parallel, assumed landed): **DT-B1** (requiredItemId compiles) and **DT-D1** (5 null
profiles, 9 spiritual-creatures FK ids, 12 location safeSpot/wilderness/accessNote backfills).

Baseline: 629 tests green. Every wave must keep the suite green. No combat-maths change. All model
changes additive (Gson: absent field = today's behaviour).

---

## LOAD-BEARING PRODUCT DECISIONS (for the DL to dispose before/at build)

These change *what the user sees*, not just how it is built. My recommendation is first; the DL/user
decides.

- **PD-1 - Does the suggested (un-selected) location's Wilderness flag credit Wilderness weapons by
  default?** Today only a *user-selected* Wilderness location does; the default loadout is always
  non-Wilderness. The feature ("loadout from the location by variant") wants the *suggested* location to
  drive gear. **Rec: YES** - it is the point of the feature. Cost: for tasks whose suggested location is
  Wilderness, the default loadout changes vs today, and loadout tests pinning the old default must be
  updated (DT-B8). *Alt (conservative):* keep selected-only (feature becomes cosmetic until the user picks).
- **PD-2 - Cannon supply trigger.** Add Dwarf multicannon + cannonballs to inventory when the effective
  location is cannonable and **the player owns a cannon** (owned-driven, consistent with ADR-0001), or
  gate on the existing `config.haveCannon()` flag? **Rec: owned-driven** (+ keep `haveCannon()` only for
  the location cannon-bias in `chooseLocation`). *Alt:* `haveCannon()` flag drives both.
- **PD-3 - Antifire/dragonfire granularity.** Trigger antifire supply off the `dragon` flag (all
  dragon-flagged tasks) or a per-task authored "uses dragonfire" signal? **Rec: `dragon` flag +
  owned-driven + note when unowned** (no new data). Risk: over/under-suggests (wyverns use ice breath;
  some safespotted dragons need no antifire). Refine per-task later if noisy. *Alt:* author a per-task flag.
- **PD-4 - Required item: shown in the inventory supplies grid AND the Task-section requirement row?**
  **Rec: Task row = ownership/requirement status (DT-B1's consumer); supplies grid = owned pack-list
  (include when owned).** Mild duplication, both framings useful. *Alt:* one place only.
- **PD-5 - Bank staleness warning threshold.** When does "reopen to refresh" appear? **Rec: 7 days.**
- **PD-6 - Attack-style selector for single-style tasks (G7 polish).** **Rec: keep it visible** (hiding
  needs per-method viability the single-recommendation panel does not have; low value). *Alt:* hide when
  one method.
- **PD-7 - Teleports in inventory supplies.** **Rec: DEFER** (no per-task teleport data; fabrication
  risk). Documented, not built.

---

## DISPOSITION of the product decisions (2026-07-01)

All seven dispose as the architect's recommendation — each follows from the /goal text (bank/owned-
driven selection; "suggested loadout based on the location and the variables of each variant and
location"). Recorded for Gate 4 presentation:

- **PD-1 = YES** — suggested-location Wilderness credits gear by default. This is the point of the
  feature and the one intended behaviour change; DT-B8 updates the pinned tests as documented change.
- **PD-2 = owned-driven** cannon supplies (haveCannon() stays only as the chooseLocation bias).
- **PD-3 = dragon-flag + owned-driven + note when unowned** (no new data; refine per-task if noisy).
- **PD-4 = both** (task row = requirement status; supplies grid = owned pack-list).
- **PD-5 = 7 days** staleness threshold.
- **PD-6 = keep the attack-style selector visible** for single-style tasks.
- **PD-7 = DEFER teleports** (no per-task data; fabrication risk). Documented, not built.

---

## Requirements traceability (goal/gaps -> task -> verification)

| Req (goal pillar / gap) | Task | Verification |
|---|---|---|
| Pillar B - suggest variant first | (done) + G7 ordering -> **DT-FE1** | Panel test: variant control renders above the location control/prose |
| Pillar C / GAP-1 / G2 - location by variant (linkage) | **DT-B3, DT-B5** | Compiler test: a linked variant emits its subset `locationNames`; an unlinked variant emits empty (all-task fallback) |
| GAP-2 / G10 - safeSpot + accessNote reach runtime | **DT-B2, DT-B4** | Compiler test: `SlayerLocation.safeSpot/accessNote` populated from the location file |
| Pillar C - location suggested by variant | **DT-B7** | Advisor test: suggested location is drawn from the variant's candidate set; unlinked = today's pick (byte-identical) |
| Pillar D / G3 - loadout from location variables | **DT-B8** | Advisor test: suggested-location Wilderness credits a Wilderness weapon with nothing selected (PD-1) |
| Pillar D / G6 - inventory supplies filled | **DT-B9** | Advisor test: cannon+cannonballs (cannon loc + owned), antifire (dragon + owned), required item (owned) populate `inventory` |
| GAP-2 - safespot -> method/gear hint | **DT-B10, DT-FE3** | Advisor test: safespot + ranged/magic sets a hint; panel renders it |
| GAP-5 - locationComparison FKs validated | **DT-B6** | Validator test: a broken `locationComparison.locationId` fails generation (post DT-D1 data = green) |
| G7 - panel order variant->location->loadout | **DT-FE1, DT-FE2** | Panel tests: order + variant-filtered dropdown |
| Pillar D / G6 - inventory renders | **DT-FE3** | Panel test: `InventoryGrid`/supply rows render when `inventory` non-empty |
| E / G4 - login staleness UX | **DT-FE4** | Plugin test: `bankAgeText` "N days ago" tier; panel shows staleness note past threshold; no snapshot -> existing gate |
| GAP-3 - boss meta-task | **DT-B5 (empty locationNames) + existing boss note** | Compiler test: boss variants emit empty `locationNames`; no fabricated flags |
| G1 requiredItemId, GAP-4 null profiles, GAP-5/9 data | **DT-B1 / DT-D1 (parallel, not this plan)** | Owned by the parallel backend/data tasks; DT-B6/DT-B9 consume their output |
| G8 method-is-prose, G9 Frost varp, GAP-7/8 housekeeping | **Not in scope** (documented QA/maintainability items) | - |

Zero in-scope gaps unmapped. G8/G9/GAP-7/GAP-8 are explicitly out (QA/live/housekeeping per DT-GAP).

---

## Task breakdown (dependency-ordered, TDD, sized)

Size tiers: **small** (~1 file + test, <1h), **standard** (2-3 files, a real behaviour), **large**
(cross-cutting derivation + several tests). Each task is red-first: write the failing test, then the code.

### Wave 1 - runtime models (parallel-safe: different files)

**DT-B2 - `SlayerLocation` gains `safeSpot` + `accessNote`.** *small*
- Files: `model/SlayerLocation.java`, `SlayerLocationTest` (or `TaskDataJsonTest`).
- Red: assert a `SlayerLocation` round-trips `safeSpot=true`/`accessNote="..."`, and the existing
  6-arg/5-arg constructors still compile (back-compat).
- Do: add `boolean safeSpot` (default false) + `String accessNote` (default null); add an additive
  all-args constructor, keep the existing back-compat constructors delegating with false/null.
- Exit: field present, Gson-defaulted, existing constructors intact, suite green.
- Conflict: none (own file). Parallel with DT-B3.

**DT-B3 - `MonsterVariant` gains `locationNames`.** *small*
- Files: `model/MonsterVariant.java`, `TaskDataJsonTest`.
- Red: assert a variant round-trips `locationNames=["A","B"]` and that an absent key -> null (fallback
  sentinel = "all task locations apply").
- Do: add `List<String> locationNames` (Gson default null). Javadoc: null/empty = no per-variant linkage.
- Exit: field present + documented, absent->null pinned, suite green.
- Conflict: none (own file). Parallel with DT-B2.

### Wave 2 - compiler emission (SERIALIZE on `ModularSlayerDataCompiler.java`; after DT-B1 + Wave 1)

> All three edit `ModularSlayerDataCompiler.java`, which **DT-B1 is editing now**. Serialize:
> DT-B1 -> DT-B4 -> DT-B5 -> DT-B6. One owner at a time on this file.

**DT-B4 - compiler emits `safeSpot`/`accessNote` on `SlayerLocation`.** *standard* (needs DT-B2, DT-B1)
- Files: `data/source/ModularSlayerDataCompiler.java` (`resolveLocations`), compiler test.
- Red: compile a fixture location with `safeSpot`/`accessNote`; assert they survive to the runtime
  `SlayerLocation` (today they are dropped at `:437-438`).
- Do: pass `location.isSafeSpot()`/`getAccessNote()` into the `SlayerLocation` constructor.
- Exit: enrichment reaches runtime; deterministic-generation test green; suite green.
- Conflict: `ModularSlayerDataCompiler.java` (serialize).

**DT-B5 - compiler derives + emits per-variant `locationNames`.** *large* (needs DT-B3, DT-B4)
- Files: `ModularSlayerDataCompiler.java` (`resolveVariants` + a new derivation helper), compiler test.
- Red (several cases): (a) a variant with a valid in-task `locationId` -> that name first; (b) a variant
  with a matching `variantInfo[]` row -> exact-name matches added in task order, unmatched dropped; (c) a
  variant with neither (incl. every boss variant, GAP-3) -> empty list; (d) a `variant.locationId`
  pointing outside the task's `locationIds` -> dropped (empty or variantInfo-only); (e) determinism.
- Do: implement the ADR-0017 #1 derivation (locationId-first, then exact case-insensitive-trim
  variantInfo matches, deduped, subset of task location names, task order). Set on `MonsterVariant`.
- Exit: linked variants carry subsets; unlinked carry empty; boss variants empty; generation
  deterministic; suite green. Spot-check a known task (e.g. abyssal-demons) in the generated JSON.
- Conflict: `ModularSlayerDataCompiler.java` (serialize). This is the core of Pillar C.

**DT-B6 - validator: `locationComparison[].locationId` + `variant.locationId` FKs.** *small* (needs DT-D1)
- Files: `ModularSlayerDataCompiler.java` (`validate`), validator test.
- Red: a fixture with a broken `locationComparison.locationId` fails generation with a clear error; a
  fixture with a broken `variant.locationId` fails.
- Do: extend `validate` to check both FK sets against `locationIds`.
- Exit: broken FKs fail the build; with DT-D1's data fix the real dataset passes; suite green.
- Conflict: `ModularSlayerDataCompiler.java` (serialize). **Depends on DT-D1 landing** or the real
  dataset fails validation.

### Wave 3 - engine coupling (SERIALIZE on `LoadoutAdvisor.java`; after DT-B5)

**DT-B7 - variant-aware `chooseLocation`.** *standard* (needs DT-B3, DT-B5)
- Files: `loadout/LoadoutAdvisor.java`, `LoadoutAdvisorTest`.
- Red: for a variant with `locationNames`, the suggested location is drawn from that subset (re-ranked by
  the existing cannon/burst/first instincts); for an unlinked variant the pick is byte-identical to today.
- Do: `chooseLocation(task, variant, style, haveCannon)` builds the candidate set = variant subset (else
  all task locations) and applies today's scoring within it.
- Exit: variant-scoped suggestion; unlinked path byte-identical (FR-6); suite green.
- Conflict: `LoadoutAdvisor.java` (serialize B7->B8->B9->B10).

**DT-B8 - effective-location Wilderness feeds gear (PD-1).** *standard* (needs DT-B7)
- Files: `LoadoutAdvisor.java`, `LoadoutAdvisorTest`.
- Red: with nothing selected and a variant whose suggested location is Wilderness, a Wilderness weapon is
  credited (today it is not until selected). Update any existing test that pinned the non-Wilderness
  default for such a task (documenting the intended change).
- Do: `effectiveLocation = findLocation(selected).orElse(chooseLocation(...))`; feed
  `effectiveLocation.isWilderness()` into `GearSelector.select` (replaces the selected-only wilderness).
- Exit: loadout couples to the suggested location; PD-1 behaviour test green; suite green (with the
  documented test updates).
- Conflict: `LoadoutAdvisor.java` (serialize). **Gated on PD-1.**

**DT-B9 - `InventorySelector` (owned-driven supplies).** *standard* (needs DT-B1, DT-B8)
- Files: new `loadout/InventorySelector.java` (+ curated cannon/cannonball/antifire id table), wiring in
  `LoadoutAdvisor.recommend` (replaces the empty `inventory` at `:145`), tests.
- Red: cannon+cannonballs when effective location cannonable + cannon owned (PD-2); antifire when
  `dragon` + owned (PD-3); required item when owned (PD-4); nothing added when unowned.
- Do: assemble `List<Integer>` supplies from owned items + effective location + profile flags.
- Exit: `inventory` populated per rules; owned-only; suite green.
- Conflict: new file parallel-safe; `LoadoutAdvisor.java` wiring serializes after DT-B8.

**DT-B10 - safespot/accessNote hint on `Recommendation`.** *small* (needs DT-B4, DT-B8)
- Files: `loadout/Recommendation.java` (add a `locationHint`/`accessNote` string), `LoadoutAdvisor.java`
  wiring, test.
- Red: effective location `safeSpot` + ranged/magic style -> a non-null hint; melee or no-safespot ->
  null; `accessNote` surfaced.
- Do: compose the hint from the effective location; set on `Recommendation`.
- Exit: hint set correctly; suite green.
- Conflict: `Recommendation.java` (additive), `LoadoutAdvisor.java` (serialize, last on this file).

### Wave 4 - frontend

> DT-FE1..FE3 all edit `SlayerPanel.java` -> **SERIALIZE** (one frontend engineer, FE1->FE2->FE3). DT-FE4's
> plugin part is parallel-safe; its panel part serializes after FE3.

**DT-FE1 - Where&How order: variant -> (attack style) -> location + recommended prose.** *standard*
(needs nothing new; UI only)
- Files: `ui/SlayerPanel.java` (`WhereSection` layout), `SlayerPanelTest`.
- Red: assert the variant control renders above the location control and the recommended-location
  headline/prose (today the prose prints above the variant).
- Do: move `whereBody` (recommended headline + why/method prose) to render with/after the location combo;
  keep variant -> attack style -> location. (PD-6: leave attack-style visible.)
- Exit: order test green; non-destructive re-render contract preserved (ADR-0002); suite green.
- Conflict: `SlayerPanel.java` (serialize FE1->FE2->FE3).

**DT-FE2 - location dropdown filtered by variant.** *standard* (needs DT-B5)
- Files: `SlayerPanel.java` (`locationNames`/`reconcileCombo`), `SlayerPanelTest`.
- Red: for a selected variant with `locationNames`, the dropdown lists only that subset; for an unlinked
  variant it lists all task locations (fallback).
- Do: `locationNames(state)` resolves the selected variant (`MonsterVariant.resolve`) and filters the
  task locations to the variant's `locationNames` (empty/null -> all).
- Exit: filtered dropdown; fallback intact; suite green.
- Conflict: `SlayerPanel.java` (serialize).

**DT-FE3 - inventory supplies + safespot hint rendering.** *standard* (needs DT-B9, DT-B10)
- Files: `SlayerPanel.java` (`LoadoutSection`), `SlayerPanelTest`.
- Red: when `rec.getInventory()` is non-empty, supply rows/`InventoryGrid` render (grid already wired at
  `:728-733`); when a location hint is present it renders as a note.
- Do: ensure the inventory grid renders the supplies; render the `locationHint`/`accessNote` note
  (mirror `strategyNote`/`bossNote`).
- Exit: supplies + hint render; empty -> nothing; suite green.
- Conflict: `SlayerPanel.java` (serialize).

**DT-FE4 - bank login-staleness UX (G4).** *small* (plugin part parallel; panel part after FE3)
- Files: `AllInSlayerPlugin.java` (`bankAgeText` + a staleness flag), optionally `SlayerPanelState.java`
  (carry a `bankStale`/days signal), `SlayerPanel.java` (staleness note), tests.
- Red: `bankAgeText` returns "N days ago" past 24h; a snapshot older than the threshold (PD-5) sets a
  staleness signal; the panel shows "Bank scanned N days ago - reopen to refresh"; **no snapshot -> the
  existing `BANK_NOT_SCANNED` gate is unchanged** (already correct).
- Do: add the days tier + threshold flag; render the note.
- Exit: age tiers + staleness note; gate path unchanged; suite green.
- Conflict: `AllInSlayerPlugin.java`/`SlayerPanelState.java` parallel-safe from the panel; the
  `SlayerPanel.java` note serializes after DT-FE3.

---

## Dependency graph (summary)

```
DT-B1, DT-D1  (parallel, external to this plan)
   |
Wave1:  DT-B2 || DT-B3                      (parallel; own files)
   |
Wave2:  DT-B4 -> DT-B5 -> DT-B6            (serialize: ModularSlayerDataCompiler.java; B4 after DT-B1; B6 after DT-D1)
   |
Wave3:  DT-B7 -> DT-B8 -> DT-B9 -> DT-B10  (serialize: LoadoutAdvisor.java; B8 gated on PD-1; B9 after DT-B1)
   |
Wave4:  DT-FE1 -> DT-FE2 -> DT-FE3         (serialize: SlayerPanel.java; FE2 after DT-B5; FE3 after DT-B9+DT-B10)
        DT-FE4                             (plugin part parallel; panel note after DT-FE3)
```

Parallel-safe pairs: **{DT-B2, DT-B3}**; **{DT-B9 new file, DT-FE1}** once their deps are met; DT-FE4's
plugin part with any panel task. Everything else on a shared file is serialized above.

## Risks

- **R1 (primary):** PD-1 changes default loadouts where the suggested location is Wilderness -> existing
  pinned loadout tests may fail. Mitigation: DT-B8 updates those tests as documented behaviour change;
  review the diff.
- **R2:** `variantInfo` name-matching (DT-B5) is exact-match; a display-string mismatch silently yields a
  thinner subset (falls back to all task locations - safe, not wrong). Mitigation: spot-check the
  generated JSON for a few enriched tasks; unmatched strings are acceptable (no fabrication).
- **R3:** curated cannon/antifire id tables (DT-B9) can drift from `net.runelite.api.ItemID`. Mitigation:
  pin ids from the real constant source + a test row per id (same discipline as `RuneTable`/WeaponEffect).
- **R4:** file-serialization on `ModularSlayerDataCompiler.java` (with in-flight DT-B1) and
  `SlayerPanel.java` throttles parallelism. Mitigation: the ordering above; a single owner per hot file.

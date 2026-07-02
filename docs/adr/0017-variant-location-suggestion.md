---
status: proposed
amends: loadout/0011
relates-to: 0016, loadout/0009, loadout/0010, loadout/0013, loadout/0015
---

# Location by variant, and loadout coupled to the suggested location

## Context

The engagement goal is: for every Duradel task, suggest (1) the monster **variant**, (2) the
**location by that variant**, and (3) the **loadout** (equipment + inventory) from the variant's
variables + the location's variables + the wiki strategy; bank-scan-gated with login-time staleness
messaging.

Pillar (1) is done (isDefault pre-select + strategy-driven default method). Pillar (2) is
**blocked**: ADR-0011 declared variant and location **orthogonal** in v1 and deferred per-variant rich
locations as "the documented future evolution once rich per-variant locations are wanted." That
evolution is now the ask. The audits (`docs/dev-readiness/{data,code-flow}-audit.md`) establish the
exact state:

- **Locations are task-level only.** The runtime task carries a flat `List<SlayerLocation> locations`
  (from `task.locationIds`); each variant carries only a free-text `location` **string** (display-only,
  surfaced in the boss note). The plugin cannot answer "for *this* variant, which location and what are
  its flags?" (GAP-1/G2).
- **The compiler drops location signal:** `SourceLocation.safeSpot`/`accessNote`, the entire
  `variantInfo[]` block, `locationComparison[]`, and the variant-level `locationId` FK (63/231 valid)
  never reach runtime (GAP-2/G10).
- **The loadout barely touches location.** Only `wilderness` feeds gear, and only from the
  **user-selected** location (defaulting to `false` when nothing is selected). `cannon`/`burst` only
  steer the recommended-location pick; `safeSpot` is not in the runtime model. The **inventory list is
  always empty** (`LoadoutAdvisor.java:145`) - no cannonballs, antifire, or required item (G3/G6).
- **`chooseLocation` is variant-agnostic** (cannon-if-owned -> burst-if-magic -> first).
- **Bank:** a persisted bank-open snapshot already exists and is reused across sessions; a literal
  login scan is infeasible (the client cannot read an unopened bank - DL ruling). Only the login-time
  **staleness/absence UX** is missing (G4).
- **Boss meta-task** has no `variantInfo`/`locationComparison` and one placeholder location (GAP-3).

The source **already carries** every signal we need (`SourceLocation.safeSpot/accessNote`,
`SourceMonsterVariant.locationId`, `SourceTask.variantInfo/locationComparison`); the work is compiler
emission + runtime coupling, not new source fields.

Constraints: additive model changes only (Gson back-compat: an absent field reproduces today's
behaviour); deterministic generation; the 629-test suite stays green through every wave; no
combat-maths change. DT-B1 (requiredItemId compiles) and DT-D1 (5 null profiles, 9 FK ids, 12 location
backfills) land in parallel - this design assumes both.

## Decision

**Couple the two axes: the variant filters and suggests a location; the suggested-or-selected location
feeds the loadout. This supersedes ADR-0011's orthogonality.** Six sub-decisions:

### 1. Per-variant location linkage = a subset of the task's location list, by name

The key insight: **the task's `locations[]` remains the single source of location flags; a variant
references a subset of them by name.** We do NOT nest rich location objects under each variant (the
migration ADR-0011 rejected) and we do NOT duplicate flag data.

- Add `MonsterVariant.locationNames : List<String>` (Gson default null). Each entry is a location
  **name** that also appears in the task's `locations[]` (names are the existing location key -
  `findLocation`/`selectedLocationName` already match by name).
- The compiler derives it **deterministically** per variant, preserving task-location order, deduped:
  1. If `variant.locationId` resolves to a location that is in the task's `locationIds` -> add that
     name **first** (the authored "home" location).
  2. If a `variantInfo[]` row matches this `variantId`, resolve each `variantInfo.locations` display
     string by **exact case-insensitive trimmed name match** against the task's location names; add
     each match. Unmatched strings are dropped (no fuzzy matching, no fabrication).
  3. The result is a subset of the task's location names. A `variant.locationId` that points **outside**
     the task's `locationIds` is dropped from the list (it still survives as the free-text `location`
     note). Variant candidates are always `subset of` task locations, so the shared task location objects
     supply all flags.
- **Empty/null `locationNames` is the fallback sentinel** = "no per-variant linkage; all task locations
  apply" = today's behaviour. This is how GAP-6 (variantInfo coverage holes) and the boss meta-task
  (GAP-3, no per-variant data) degrade gracefully to the task-level list. **FR-6 back-compat: a variant
  with no linkage produces exactly today's location set.**

### 2. Runtime location enrichment: `safeSpot` + `accessNote` only

Add `SlayerLocation.safeSpot : boolean` (Gson default false) and `SlayerLocation.accessNote : String`
(default null), compiled from the location file, via an additive constructor mirroring the existing
`wilderness` back-compat constructor. We do **not** add `cannonable`/`multicombat` - those from
`locationComparison[]` are redundant with the existing `cannon`/`multi` on the location file. We do
**not** compile `locationComparison[]` flags into runtime (per-task, not per-variant, redundant, and 9
were broken FKs); it stays source/LLM context.

### 3. Location suggestion rule: variant-scoped, same scoring instincts

`chooseLocation` becomes variant-aware: `chooseLocation(task, variant, style, haveCannon)`.
- **Candidate set** = the variant's `locationNames` resolved to task `SlayerLocation` objects; if the
  variant has no linkage (empty/null) -> **all** task locations (byte-identical to today).
- Apply today's scoring **within the candidate set**: `haveCannon && cannon`-capable -> that; else
  `MAGIC && burst`-capable -> that; else the **first** candidate (which, for a linked variant, is the
  authored home location). When the candidate set is all task locations, this is byte-identical to
  today's `chooseLocation` - the FR-6 anchor.

### 4. Loadout coupled to the suggested-or-selected location; owned-driven inventory supplies

- **Effective location** for the loadout = user-selected location, else the suggested location (from
  #3). Its `wilderness` flag feeds `GearSelector` (today only the *selected* location did, defaulting
  to non-Wilderness). This is what makes the loadout derive from "the location by variant."
- **Inventory supplies** (populates the always-empty `Recommendation.inventory`, owned-driven per
  ADR-0001) via a new `InventorySelector`:
  - `task.requiredItemId` (now compiled by DT-B1) when owned.
  - Dwarf multicannon + cannonballs when the effective location `isCannon()` and the player owns a
    cannon (curated cannon/cannonball ids, like `RuneTable`).
  - Antifire protection when the profile is `dragon` and the player owns an antifire potion/shield.
  - The list stays `List<Integer>` (no quantities) and renders via the **existing** `InventoryGrid`
    (already wired at `SlayerPanel.java:728-733`, just never fed). ConsumableSelector is unchanged.
- **Safespot** feeds a **note/hint** only (never combat maths, NG-4): when the effective location
  `isSafeSpot()` and the style is ranged/magic, surface a hint; `accessNote` surfaces as a note.

### 5. Validator extension holds the FK fixes

Extend the compiler validator to check `task.locationComparison[].locationId` and `variant.locationId`
resolve to known locations. DT-D1 fixes the 9 broken `spiritual-creatures` FKs; this change keeps them
fixed. (We still do not *compile* `locationComparison`; we validate it so the data stays honest.)

### 6. Panel order variant -> location -> loadout; boss stays a note; login staleness UX

- Reorder Where&How so the variant control reads first, then the location control **with** the
  recommended-location headline/prose (today the location prose prints *above* the variant). Keep the
  attack-style selector after variant. Filter the location dropdown to the resolved variant's
  `locationNames` (fallback: all).
- **Boss (GAP-3):** cheapest honest handling - boss variants get empty `locationNames` and keep their
  free-text `location` in the existing boss note. No fabricated `SlayerLocation` flags, no per-boss
  location dropdown enrichment. Boss is one opt-in task; do not gold-plate.
- **Bank staleness (G4):** no snapshot at login -> the existing `BANK_NOT_SCANNED` gate + "open your
  bank" prompt (already implemented) is the answer. Snapshot present -> extend `bankAgeText` with a
  "N days ago" tier and show a "Bank scanned N days ago - reopen to refresh" note past a threshold.

## Consequences

- Pillar (2) is delivered without migrating location data under variants: one lightweight additive
  field (`locationNames`) + two enrichment fields, all Gson-back-compat.
- **Behaviour change (intended, not byte-identical):** for a variant whose suggested location is a
  Wilderness location, the **default** loadout now credits Wilderness weapons (today it never did until
  the user selected that location). Existing loadout tests that pinned the non-Wilderness default for
  such tasks encode the *old* behaviour and must be updated as part of the change. This is the primary
  regression surface and is called out as a load-bearing product decision in the plan.
- The runtime location model gains `safeSpot`/`accessNote`, benefiting every consumer (suggestion, note,
  loadout hint) at once.
- `locationComparison` becomes validated FK data even though it is not compiled - it stops silently
  rotting (GAP-5).
- Additive `MonsterVariant`/`SlayerLocation` fields keep every no-variant / unlinked-variant task
  byte-identical (FR-6), so the 629-suite migrates green and the diff is scoped to genuinely-linked
  variants.

## Considered options

- **Nest rich per-variant `SlayerLocation` sets under each variant** (ADR-0011's named future). Rejected:
  duplicates flag data, forces a large migration of all location data under variants, and creates a
  variant x location combinatorial explosion. Referencing the shared task locations by name gets the
  same behaviour for one string list.
- **Compile `locationComparison[]` flags (`multicombat/cannonable/safespottable`) into the variant.**
  Rejected: per-task not per-variant, redundant with the location file's `multi`/`cannon`, and 9 FKs
  were broken. Kept as source/LLM context; validated but not compiled.
- **Feed the loadout only from the user-selected location (today's rule), leaving the suggested location
  cosmetic.** Rejected: fails the core "loadout from the location by variant" requirement - the loadout
  would ignore the location until the user manually picks it. (This is offered as the conservative
  alternative in the plan's product-decision list.)
- **Fabricate boss locations / flags** to make the boss dropdown rich. Rejected: no honest source data
  (GAP-3); the free-text note is the cheapest truthful handling.
- **A true login bank scan.** Infeasible - the OSRS client cannot read an unopened bank (DL ruling);
  the persisted snapshot + staleness UX is the honest equivalent.

## Rejected refinements (deferred, documented not built)

- **Teleports in inventory supplies** - no per-task teleport data; synthesising them risks fabrication.
- **Per-task authored "uses dragonfire" signal** - v1 triggers antifire off the `dragon` flag
  (owned-driven, note when unowned); refine later if it over/under-suggests (e.g. wyverns use ice
  breath).
- **Inventory quantities** (cannonballs count) - `Recommendation.inventory` stays `List<Integer>`; a
  quantity-bearing model is a future enhancement.

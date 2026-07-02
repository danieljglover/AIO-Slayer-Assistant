---
status: proposed
relates-to: 0016, 0018, 0020
---

# One repeatable recipe for authoring a new task family

## Context

Coverage is Duradel-shaped: 43 families exist, all on Duradel's list; the low-level, Wilderness, and a
band of mid/high families are missing (FR-R1: Vannaka 19/46, Krystilia 12/37, Turael/Spria/Mazchna
~2 each). Wave C expands the dataset by authoring families, phased: C1 (this engagement) = 11 mid/high
families assignable by first-class masters (Turoth, Jellies, Brine rats, Custodian stalker, Hydras,
Lesser Nagua, Scabarites, Lesser demons, Shadow warriors, Jungle horrors, Fever spiders); C2
(Krystilia Wilderness set) and C3 (starter tier) follow with the same recipe.

Authoring is done by parallel per-family agents (LLM + human) against the OSRS wiki. Without a fixed
recipe each family drifts in file layout, test coverage, and data honesty, and parallel agents collide
on shared files. The modular data pattern (ADR-0016) and the ~40 existing families already establish
the shape; this ADR freezes it as the contract so C1/C2/C3 are mechanical and reviewable.

## Decision

Every new family is authored with the **same file set, the same coverage test, and the same data
discipline**. A family is not done until `./gradlew cleanTest test` is green (the strict compiler
validates it and its coverage test passes).

### File set (per family `<family>`)

1. `src/main/data/slayer/tasks/<family>.json` - the task. Required keys: `taskId`, `name`,
   `wikiPageId` (or null with a note), `combatLevel`, `slayerTargetId` (the real SLAYER_TARGET varp;
   never a synthetic id unless genuinely unknown and flagged, per D5/Frost-Dragons), `slayerLevel`,
   `questReqs`, `masterIds`, `amountByMaster`, `extendedAmount` (when the family extends),
   **`weightByMaster`** (ADR-0018 #3), `unlocks[]` (with `type`, ADR-0018 #5), `variantIds` +
   `defaultVariantId` (for multi-variant), `weakness`, `monsterDefence`, category flags
   (`slayerHelmApplies`/`undead`/`dragon`/`demon`/`kalphite`), `requiredItemId`/`requiredItemName`
   when the task needs an item, `locationIds`, `variantInfo[]`, `locationComparison[]`, `taskNotes[]`,
   `recommendedMethod`, optional task-level `offence` (ADR-0020 #1, the fallback when a family's
   variants share offence). **`locationComparison[]` rows now compile into a runtime `LocationQuality`
   overlay** (ADR-0020 #2, WD-5a): author each row's `amount` (wiki kills/density figure - the location
   ranking key), `multicombat`, `cannonable`, `safespottable`, `notes` per location that has richer
   task-scoped truth than the intrinsic location file flags. Absent -> null overlay -> today's authored
   order (FR-6).
2. `src/main/data/slayer/monsters/<family>/<variant>.json` - one file per variant (`variantId`,
   `name`, `npcIds`, `combatLevel`, `weakness`, `monsterDefence`, flags, `boss`, `location`,
   `locationId`, `requirement`, `bossId`, `strategyId`, optional **`offence`**). The directory name is
   the `monsterId`. The `offence` object (ADR-0020 #1) is the NEW additive monster-offence domain -
   what the monster does to the player - authored per variant (a superior hits harder than its base):
   `offence { hitpoints:Integer, maxHit:Integer, attackStyles:[MELEE|RANGED|MAGIC|DRAGONFIRE|TYPELESS],
   attackSpeedTicks:Integer, magicLevel:Integer, poisonous:boolean, venomous:boolean }`. Every field is
   optional (honest UNKNOWN - omit the key where the wiki does not state it). Author `magicLevel` for
   Twisted-bow-relevant targets (WD-11). It drives the note-only prayer/survival advisory (WD-3),
   never combat maths (NG-4).
3. `src/main/data/slayer/locations/<location>.json` - one per location the family uses. **Reuse an
   existing shared location by id where one exists** (Catacombs of Kourend, Slayer Tower, etc.); create
   a new file only for genuinely new locations. Keys: `locationId`, `name`, `multi`, `cannon`, `burst`,
   `konarLockable`, `safeSpot`, `wilderness`, `accessNote`.
4. `src/main/data/slayer/strategies/<family>/strategy.json` - the wiki `/Strategies` gear + method
   context. `plugin { primaryStyle, primaryWeapons[weaponId], secondaryWeapons[{weaponId, style}],
   note }` drives the runtime override (ADR-0015); `variantIds`, `sourceUrl`, `requirements[]`,
   `mechanics[]`, `methods[]` are source/LLM context. Every `weaponId` must resolve to an existing
   `weapons/*.json`; add a weapon file (name -> raw itemId, from `net.runelite.api.ItemID`) if a
   strategy needs a weapon not yet present.
5. `src/test/java/com/danieljglover/allinslayer/data/source/<Family>SourceCoverageTest.java` - the
   coverage test (below).

### Coverage test (mirrors the existing `<Family>SourceCoverageTest` pattern)

Read the source JSON directly and assert: `wikiPageId`, `combatLevel`, `slayerTargetId`, `slayerLevel`,
`questReqs`; every `masterIds` member and its `amountByMaster` array; `weightByMaster` per master;
`extendedAmount` (or its absence); a representative set of `locationIds`; the `unlocks[]` (id, cost,
type); key `taskNotes` substrings; the `variantInfo[]` rows; each authored variant's `offence`
(hitpoints/maxHit/attackStyles, plus `magicLevel` for Tbow-relevant targets) with **honest UNKNOWN
asserted absent** where the wiki does not state it (ADR-0020 #1); each `locationComparison[]` row's
`amount`/`multicombat`/`cannonable`/`safespottable` (ADR-0020 #2). Then compile via
`ModularSlayerDataCompiler` and assert the runtime `TaskData` resolves the variants, the default
variant, the strategy links, the variant/task `offence` (with the task-level fallback), and the
per-location `LocationQuality` overlay. The test is the family's acceptance evidence.

### Data discipline (non-negotiable, culture: evidence over assertion, no fabrication)

- **Wiki-sourced.** Every number comes from the family's own `oldschool.runescape.wiki/w/Slayer_task/
  <Family>` page and the linked monster/strategy pages, fetched at authoring time; `sourceUrl` records
  it.
- **Honest UNKNOWN.** Where the wiki does not give a value, use null / omit the key (Gson default) and
  a `taskNotes` note; **never invent a stat, weight, or id.** A missing profile field falls back to the
  task default (ADR-0010); a missing weight is acceptable (the skip/block advisor treats it as
  unknown).
- **Weights are wiki-re-verified.** FR-R1 section 3 weights are summarizer-extracted; re-verify each
  family's `weightByMaster` against its own wiki page during authoring (PD-D). The family SET is
  trustworthy; exact weights/quantities are spot-checked.
- **FK completeness up front.** The compiler is strict (ADR-0016): every referenced masterId (exists),
  locationId, variantId, weaponId, strategyId must resolve. Shared new locations are created before the
  parallel family fan-out, or owned by exactly one family, so parallel agents do not both author the
  same file.

## Consequences

- C1/C2/C3 become mechanical: same files, same test, same discipline; a reviewer checks one shape.
- Parallel per-family agents are conflict-free on family-owned files (task/monsters/strategy/coverage
  test); the only serialization point is a **shared location file** two families both need - resolved
  by pre-creating shared locations (`WC-0`) or single-owner assignment.
- The strict compiler + the per-family coverage test make "done" objective: green build = the family is
  integrated, validated, and covered.
- Weights authored here feed the future skip/block advisor (Wave D/P2) with no rework.
- **Offence + location quality are authored up front** (ADR-0020 sub-decision 6): C2/C3 families carry
  the `offence` object and `locationComparison` quality fields as they are written, so Wave D's D3/D4
  backfill of the existing 54 families is a one-time pass, not a recurring second visit. Both are
  additive and honest-UNKNOWN-tolerant, so a family that omits them degrades to today's behaviour (FR-6).

## Considered options

- **Bulk-import all families from one scraped table.** Rejected: the summarizer-extracted tables are
  reliable for the family set but not for exact per-row numbers (FR-R1 section 4.3 caveat); per-family
  wiki authoring with a coverage test is the honest unit.
- **Skip the per-family coverage test (rely on the compiler alone).** Rejected: the compiler validates
  references and shape, not that the authored numbers match the wiki; the coverage test is the only
  guard on data fidelity.

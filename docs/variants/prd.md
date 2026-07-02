# All-In Slayer - Monster Variant Selection: PRD (lite)

- Author: Architect / Principal Engineer (board MV-P1)
- Date: 2026-06-29
- Track: T2 feature on the (now complete) loadout engine
- Status: Gate 1 draft for review. Detailed design + ADRs + TDD breakdown are the next task (MV-A1),
  which depends on the research enumeration (MV-R1 -> `docs/variants/`). This PRD does NOT design the
  data model; it sets the problem, requirements, and acceptance criteria the design must satisfy.
- Gate 0: CONFIRMED via AskUserQuestion (manifest.md "Monster Variant Selection"): master scope =
  Duradel's full assignable task list (verified vs the wiki, not assumed = the existing 42-task set);
  breadth = ALL variants incl. bosses; coupling = re-drive the loadout per selected variant.
- Domain anchor (verified from code): `model/TaskData.java` holds ONE task-level `weakness` +
  `monsterDefence` + category flags (`demon`/`dragon`/`kalphite`/`undead`) + a flat `monsters:List<String>`
  / `npcIds`. The engine (`LoadoutAdvisor` -> `GearSelector` / `ConsumableSelector` /
  `DefaultDpsEstimator` / `ConditionalBonusRegistry`) drives off that single per-task profile. Data:
  `src/main/resources/data/slayer-data.json` (42 tasks), loaded by `data/SlayerDataService.java`.

## 1. Problem & motivation

A single Slayer assignment is satisfiable by **many different monsters**, and they are not the same
fight. "Greater demons" can be killed as the regular Greater demon, but the same assignment also counts
toward **Skotizo, K'ril Tsutsaroth, Tormented Demons, and Demonic gorillas** - each with its own
defence level, defensive bonuses, weakness, and category. Beyond bosses, ordinary tasks split into
**leveled / location variants** (e.g. different-level Black demons in different locations) and
**superior** spawns, again with distinct stats.

The plugin today models each task as **one** profile. So the loadout (gear, weapon, consumables, and
the displayed Est. DPS) is always computed against the task default. A player who is on "Greater demons"
but intends to kill K'ril, or who is killing a higher-level location variant, gets advice tuned to the
wrong monster: wrong accuracy roll vs the real defence, sometimes the wrong weakness/style, and a DPS
number that does not reflect what they will actually hit.

**What the user wants:** pick the specific monster (variant or boss) they intend to kill for this
assignment, and have the loadout re-drive off **that monster's** profile.

## 2. Users

- **Primary:** an OSRS Slayer player in RuneLite who, for the current assignment, wants the loadout
  tuned to the exact monster they will kill - especially when that is a **boss** (Skotizo, K'ril,
  Tormented Demon, Demonic gorillas) or a **higher-level / superior** variant whose stats differ
  materially from the base monster.
- Single-player, local, read-only against game state. No accounts, server, or PII (unchanged).

## 3. In-scope requirements (testable)

Each FR has a binary, headless-testable acceptance criterion. "Profile" = the variant's
`{weakness, monsterDefence, category flags}` triple that the engine consumes.

- **FR-1 Variant-aware data.** Every Duradel-assignable task carries an enumerated set of monster
  variants sourced from MV-R1, each with its own profile plus identity/metadata (name, npcId(s),
  combat level, location, requirements, isBoss). *Accept:* a dataset-validation test asserts every
  Duradel task has >=1 variant and every variant has a non-null weakness, monsterDefence, and the
  category flags resolved; bosses (Skotizo, K'ril Tsutsaroth, Tormented Demon, Demonic gorillas)
  appear as variants under their parent task(s).
- **FR-2 Authoritative task list.** The Duradel-assignable task list is verified against the OSRS wiki
  Duradel page, not assumed equal to the existing 42-task set. *Accept:* a test asserts the dataset's
  Duradel task set equals the MV-R1 enumerated list (additions/removals are explicit, not silent).
- **FR-3 Variant selector in the panel.** When the current task has >1 variant, the side panel presents
  a control to choose the intended variant. *Accept:* a `SlayerPanelTest` asserts the variant control
  renders with one entry per variant for a multi-variant task and is hidden/inert for a single-variant
  task. (Mirrors the existing `where-location-combo` / `onSelectLocation` pattern.)
- **FR-4 Re-drive on selection.** Selecting a variant recomputes gear, weapon, consumables, and Est.
  DPS against the **selected variant's** profile, not the task default. *Accept:* a `LoadoutAdvisor` /
  engine test shows that two variants of one task with different defence/weakness yield different
  recommendations (e.g. style or weapon ranking flips when the selected variant's weakness/defence
  differs), and that the chosen profile is the variant's, not the task's.
- **FR-5 Category bonuses follow the variant.** The existing `ConditionalBonusRegistry` predicates
  (`VS_DEMON` / `VS_DRAGON` / `VS_KALPHITE` / undead, and the on-task helm) are driven by the
  **selected variant's** flags. *Accept:* a test shows a variant flagged `demon` credits demonbane while
  a sibling variant under the same task that is not a demon does not (within one task).
- **FR-6 Default selection.** A loadout is shown without forcing the user to choose: a deterministic
  default variant is selected on task change. *Accept:* a test asserts the default-selection rule
  (rule chosen in MV-A1; candidate: the base/most-common non-boss variant) yields a non-null
  recommendation immediately on a fresh task, and that the prior behaviour (task-default profile) is
  preserved when a task has exactly one variant.
- **FR-7 Boss separateness is surfaced, not hidden.** Boss variants are selectable but visibly marked
  as a different kind of trip (different loadout, typically a separate trip with supplies/prayer not
  modelled here). *Accept:* a `SlayerPanelTest` asserts boss variants carry a distinguishing marker
  (e.g. a "Boss" tag / note) in the selector or recommendation, so the user is not misled into treating
  a boss loadout as a grind loadout.

## 4. Non-goals (binding for this feature)

- **NG-1 No automation** - advice only; never equips/withdraws/switches. (Unchanged.)
- **NG-2 No boss-fight modelling beyond the loadout axes already modelled.** Prayer, supplies, special
  attacks, phase transitions, mechanics, and inventory/trip planning for bosses are out of scope; we
  re-drive the existing gear/weapon/consumable/DPS axes against the boss's profile only.
- **NG-3 No in-fight / per-phase gear switching** - one loadout per selected variant (extends NG-3 of
  the loadout PRD).
- **NG-4 No new combat maths.** This feature re-keys the existing engine onto a per-variant profile; it
  does not add new DPS mechanics (those are owned by the loadout/weapon ADRs 0006-0009).
- **NG-5 No detailed data-model design here.** The variant data shape, default-selection rule, and the
  variant/location-axis interaction are decided in MV-A1, informed by the MV-R1 file shape.
- **NG-6 Masters other than Duradel/Kuradal** are out of scope for the enumeration (Duradel = Kuradal
  post-WGS; the assignable list is the anchor). Other masters' tasks are unaffected.
- **NG-7 Rune-pouch contents** still not read (inherited NG-4 of the loadout PRD).

## 5. Key constraints & risks

- **R-1 Data completeness / hallucination (highest risk).** The whole feature rests on a correct,
  complete per-variant enumeration. MV-R1 must parse the OSRS wiki directly, checkpoint to disk, and
  not invent npcIds/defence stats. A wrong defence stat silently produces a wrong loadout with no error.
  *Mitigation:* dataset-validation tests (FR-1/FR-2), and FR-4/FR-5 only assert relative behaviour
  (ranking flips) so they stay valid even as exact numbers are corrected.
- **R-2 Bosses need very different loadouts and separate trips.** A boss is not a "grind the assignment"
  trip; treating its loadout as interchangeable with the base monster's would mislead. FR-7 surfaces
  this; NG-2 bounds how far we model it.
- **R-3 Single-profile assumption is baked across the engine.** `TaskData` carries one profile and
  every engine entry point reads it; `BonusContext.from(TaskData)`, `LoadoutAdvisor.recommend(...)`,
  `GearSelector.select(...)`, `DefaultDpsEstimator.estimate(...)`. Introducing a per-variant axis is a
  cross-cutting re-key, not a local change. *Mitigation:* MV-A1 designs the seam; the default-single
  variant must reproduce today's behaviour exactly (FR-6) so the 42-task baseline is regression-safe.
- **R-4 Two selection axes now.** A variant axis is added alongside the **existing location combo**.
  Some variants are inherently location-bound (e.g. the Wilderness Ankou already threaded into
  `BonusContext` by WDB-17). Whether variant and location are orthogonal, nested, or one subsumes the
  other is the central design question for MV-A1 (see open questions).
- **R-5 Backward compatibility.** Existing dataset/tests assume one profile per task. The migration must
  keep the 42-task suite green (or migrate it deliberately), and the loaded JSON must remain valid.
- **R-6 Data volume / UI.** "All variants incl. bosses" across the full Duradel list is a large dataset;
  the selector must stay usable (grouping bosses vs base/superior/location variants) - a UX detail for
  MV-A1/FE, flagged here.

## 6. How it touches the system (HIGH level - detail deferred to MV-A1)

This is an orientation map, not a design. The data shape is decided in MV-A1 once the MV-R1 file shape
is known.

- **Model (`model/TaskData.java` + a new variant type).** Today: one task-level
  `weakness`+`monsterDefence`+`demon`/`dragon`/`kalphite`/`undead`+flat `monsters`/`npcIds`. The feature
  adds a **per-variant axis below the task** carrying each variant's own profile + identity (name,
  npcId(s), combat level, location, requirements, isBoss). Exact shape = MV-A1.
- **Data (`slayer-data.json` + `data/SlayerDataService.java`).** The variant enumeration from MV-R1 is
  encoded per task; the loader parses the new structure. Must stay valid/loadable; migration keeps the
  baseline green.
- **Engine (`loadout/*`).** `LoadoutAdvisor.recommend(...)`, `GearSelector.select(...)`,
  `ConsumableSelector`, `DefaultDpsEstimator.estimate(...)`, and `ConditionalBonusRegistry` /
  `BonusContext.from(TaskData)` currently consume the task profile. They must consume the **selected
  variant's** profile instead (the cleanest seam is likely "resolve a variant -> profile, feed the
  existing engine" so the combat maths is untouched - to be confirmed in MV-A1).
- **State + wiring (`ui/SlayerPanelState.java` + `AllInSlayerPlugin.recompute`).** A **selected variant**
  is threaded through recompute exactly as the **selected location** already is; default on task change
  per FR-6.
- **UI (`ui/SlayerPanel.java`).** Add a variant `JComboBox` modelled on the existing
  `where-location-combo` / `onSelectLocation` callback, with re-render on selection and the FR-7 boss
  marker. Pull in the UI designer only if the control needs real visual design (manifest default: FE
  handles a contained control).

## 7. Open design questions (surface at Gate 1 -> resolve in MV-A1)

- **OQ-1 (central): variant x location axis interaction.** Are variant and the existing location combo
  orthogonal (two independent selectors), nested (location is a property of a variant), or does variant
  subsume location? The Wilderness-Ankou precedent (location already in `BonusContext`) suggests overlap.
  This shapes the data model and the UI, so it gates MV-A1.
- **OQ-2: default-selection rule.** Base/most-common non-boss variant vs first-listed vs "none until
  chosen". FR-6 needs a concrete rule.
- **OQ-3: boss presentation.** Same selector with a "Boss" tag, or a visually separated group / explicit
  separate-trip note. FR-7 sets the requirement; the presentation is open.

---
status: accepted
supersedes-stance-in: 0012
relates-to: 0010, 0011, 0013
---

# The generic 'Boss' slayer task participates in variant selection

## Context

ADR-0012.5 EXCLUDED the Duradel "Boss" meta-task from variant selection in v1: a 32-boss pool, no
enumerated stats, each a bespoke separate trip. At Gate 2 the human **overturned** this: the Boss task
must participate, with its variants = the assignable boss pool (`variants-boss.md`, ~31 Duradel-rollable
bosses; Alchemical Hydra is Konar-exclusive). MV-R2 is enumerating each boss's defence/weakness into
`variants-boss.md` in parallel. Two facts make this clean now:

- The model already supports it: a boss is just a `MonsterVariant` with `isBoss=true` carrying its own
  `{weakness, defence, flags, location, requirement}`, resolved through the same `MonsterProfile` seam
  (ADR-0010). The Boss task is simply a task whose EVERY variant `isBoss`.
- The live client exposes the rolled boss: varbit **`SLAYER_TARGET_BOSSID` (4723)** is already declared
  in `SlayerVarbits`. When the assigned task is Boss, the game knows exactly which boss was rolled - so
  the plugin can pre-select the right variant instead of guessing.

The Boss meta-task has NO natural task-level weakness/defence (there is no "default boss"), which
interacts with the `MonsterProfile.fromVariant` per-field fallback (ADR-0010): a boss variant with a
null weakness would fall back to a null task-level weakness, `styleOf` would return null, and the engine
would produce no recommendation.

## Decision

**Include Boss as a task whose variants are the assignable bosses; each boss carries its OWN resolved
profile (no task-level fallback); pre-select the live-rolled boss from varbit 4723, else a deterministic
first.**

1. **Dataset.** The Boss task's `variants` array = the assignable boss pool from `variants-boss.md`, each
   `isBoss=true` with `requirement` "boss - separate trip", its `location`, category flags
   (Abyssal Sire demon, KBD/Vorkath dragon, Kalphite Queen kalphite, Barrows/Vet'ion undead, ...), and
   its OWN wiki `weakness`/`monsterDefence` from MV-R2. The Boss task-level `weakness`/`monsterDefence`
   are left null (a meta-task has no own profile).

2. **No fabrication, so no null-unresolvable boss.** A boss variant MUST carry a non-null weakness +
   defence (else it resolves to a null profile and yields no recommendation, because there is no
   meaningful Boss task-level fallback). A boss whose stats MV-R2 has not yet enumerated is DEFERRED
   (omitted, logged explicitly), never shipped with a fabricated or null profile - the same discipline
   that excluded Silverlight (ADR-0009) and the Dire/Shellbane gryphon (ADR-0012.2).

3. **Default selection for an all-boss task.** `resolveVariant` always lands on a boss variant (every
   variant `isBoss`): the live-rolled boss if known, else the `isDefault` boss, else the first listed.
   "Which boss is default" is meaningless statically, so `isDefault` marks a deterministic fallback (the
   first listed) purely to satisfy the exactly-one-default validation rule; live pre-selection overrides
   it.

4. **Live pre-selection from varbit 4723.** `MonsterVariant` gains an optional `bossId` (the
   `SLAYER_TARGET_BOSSID` value for that boss). When the resolved task is Boss and the user has not
   overridden the selection, the plugin reads varbit 4723, matches it to the boss variant's `bossId`,
   and seeds `selectedVariantName` (mirroring the auto-seed of the location combo). If the varbit is
   unavailable or its value is unmapped, it falls to the deterministic default (3). **The exact
   bossId -> boss mapping must be verified against a live client before hard-coding** (the varbit may be
   a slayer-specific enum, not an NPC id); unmapped values fall through safely - do not fabricate the
   mapping.

5. **Boss separateness (FR-7) is already covered.** Every boss variant is `isBoss` with the "separate
   trip" note, so the existing FR-7 card-note design (ADR-0010 / plan section 5) surfaces it with no new
   mechanism.

6. **Orthogonal-axis consistency (ADR-0011) preserved.** A boss's inherent place is a free-text
   `location` NOTE, not a rich `SlayerLocation`; the existing location/wilderness machinery is untouched.

## Considered options

- **Keep Boss OUT (ADR-0012.5).** Overturned by the human at Gate 2.
- **Ship every boss now with a placeholder profile.** Rejected: fabricates stats, violates R-1 / the
  no-fabrication discipline. Bosses without enumerated stats are deferred, not faked.
- **Ignore varbit 4723 and always default to first.** Rejected: the live client knows the rolled boss;
  defaulting to a wrong boss when the answer is available is a worse experience. The varbit pre-selects;
  first-listed is only the unmapped fallback.

## Consequences

- A new build sub-task **MV-D12** authors the Boss task + its enumerated boss variants (single-owner
  `slayer-data.json`), and a new **MV-B8** wires `bossId` + the varbit-4723 pre-selection through
  `TaskDetector`/`AllInSlayerPlugin`. The validation harness gains an all-boss-task rule (every variant
  `isBoss`, exactly one `isDefault` fallback, each variant has a resolved non-null profile, deferred
  bosses logged).
- The Boss task is the one task with null task-level weakness/defence; the harness must exempt it from
  the "task-level profile non-null" assertion while requiring each of its variants to be non-null.
- The exact varbit-4723 mapping is a live-verify item (cannot be confirmed headless), on the QA checklist;
  the headless path is proven by seeding `selectedVariantName` directly.
</content>

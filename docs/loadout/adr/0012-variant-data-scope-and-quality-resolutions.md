---
status: accepted
relates-to: 0010, 0011
---

# Variant dataset scope and data-quality resolutions

## Context

MV-R1 enumerated ~215 variant blocks but `docs/variants/INDEX.md` lists seven load-bearing data-quality
findings the design must resolve, plus the master scope/quality risk R-1 (a wrong defence stat silently
produces a wrong loadout). These resolutions must be settled before the data-authoring waves, and several
are user-visible scope calls. None may fabricate a stat the wiki does not give (the discipline that
excluded Silverlight in ADR-0009 and deferred the dragon set in WDB).

## Decision

1. **Reanimated monsters: EXCLUDE.** They do not count toward a Slayer task; they are not selectable
   variants. (Known exception: the wiki tags *Reanimated kalphite* as counting - still excluded in v1 for
   consistency, recorded here so it is a conscious omission, not a miss.)
2. **Dire vs Shellbane gryphon:** Dire gryphon is the real superior, Shellbane is the boss (the brief had
   them swapped). Dire's full stats are UNKNOWN (page not fetched), so Gryphons ships **base-only** in v1;
   Dire/Shellbane are deferred until stats are fetched - no fabricated profile.
3. **UNKNOWN-weakness variants** (all Vampyres, all Bloodveld, GWD/Reanimated hellhounds, Dark Ankou):
   the variant is still selectable for identity/location but its profile **inherits the task default**
   (null `weakness`/`monsterDefence` on the variant -> `MonsterProfile.fromVariant` falls back per ADR-0010).
   No weakness is invented.
4. **Multi-form bosses: model each form as its own variant.** Dagannoth Kings -> Rex (MELEE) / Prime
   (RANGED) / Supreme (MAGIC); Kalphite Queen -> crawling (CRUSH) / airborne (MAGIC). Distinct forms have
   distinct profiles and genuinely want distinct loadouts; the variant model already supports N entries,
   so per-form is the honest, useful representation.
5. **The "Boss" meta-task: EXCLUDE from variant selection in v1.** It is a 32-boss pool with no enumerated
   stats; each is a bespoke separate trip (`variants-boss.md`). The bosses that matter as *task
   alternatives* (Skotizo, K'ril, Tormented Demon, Demonic gorilla) are enumerated under their parent
   tasks and DO participate. The standalone Boss assignment is left as today.
6. **Missing `drange`:** treat a missing/UNKNOWN ranged-defence bonus as 0 (the `MonsterDefence` default).
   A live `NPCDefinition` second pass could fill it later.
7. **Smoke/Dust devils:** their MAGIC weakness is correct (barraged due to magic LEVEL 1, not low magic
   DEFENCE - smoke devil magic def is 600). Keep the MAGIC flag; do not infer weakness from defence
   bonuses. No action beyond a data-authoring note.

**Frost Dragons** (the only Duradel task missing from the current 42-set, npc 14922) are ADDED as a new
draconic task, taking the dataset to 43 tasks (FR-2).

**Weakness tie-breaks** (Skotizo melee/magic, Demonic gorilla melee/ranged, Araxyte crush/magic, etc.):
encode a single resolved weakness per variant per the `gaps-research.md` notes (e.g. Skotizo -> MELEE,
demonbane dominates). The data author records the chosen style; ties are not carried into the model.

8. **Weakness model: combat-style weakness vs wiki "elemental weakness" are two axes (FR-6-critical).**
   The cross-check (`docs/variants/crosscheck/`) reports dragons as MAGIC because the wiki lists an
   elemental weakness (Water/Earth/Fire %); production marks Black/Blue/Red dragons RANGED. The engine's
   `Weakness` type already separates these: `style` (MELEE/RANGED/MAGIC) drives gear selection;
   `element` (string) is the wiki elemental weakness, a MAGIC-only spell selector used only when
   `style == MAGIC` (the % magnitude is not modelled - NG-4). **PRESERVE the existing `style` for the 42
   tasks** (dragons stay RANGED - byte-identical, FR-6); map the wiki elemental weakness only onto
   `element`. New variants set `style` from the defence profile / convention and `element` for magic
   relevance. A wiki elemental weakness must NEVER silently flip an existing task's style.

9. **Category flags carry the engine's bonus-applicability meaning, NOT the raw wiki attribute.**
   `demon`/`dragon`/`kalphite`/`undead` mean "demonbane / dragonbane / Keris / Salve applies" - a curated
   subset. Cross-check corrections honoured: Hellhounds & Waterfiends have the wiki demon attribute but
   `demon=false` (demonbane does not apply; pinned set {Abyssal, Black, Greater demons, Nechryael});
   Greater Skeleton Hellhound `undead=true` not demon; Vorkath `dragon=true`+`undead=true` (both apply),
   boss; all dragons + Skeletal Wyvern + Vorkath + Frost = `dragon=true`.

## Consequences

- Dataset-validation tests (FR-1/FR-2) assert: every Duradel task has >=1 variant, every variant has a
  resolved-or-inherited profile, exactly one `isDefault` per multi-variant task, the named bosses appear
  under their parents, no reanimated variant is selectable, and the Duradel task set equals the MV-R1 list
  (additions/removals explicit, not silent).
- Deferred (no fabrication): Dire/Shellbane gryphon stats, per-variant rich locations, the standalone
  Boss meta-task, and any `drange` second pass.
</content>

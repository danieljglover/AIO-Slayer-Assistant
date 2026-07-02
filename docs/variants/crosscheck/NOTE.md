# Cross-check source (SECONDARY — not the primary enumeration)

`variants-dragons-bosses.md` + `gaps-a.md` were produced by `mv-research-a`, a supplementary researcher that
finished its 13-task dragon/boss slice **after** the primary researcher (`mv-research`) had already enumerated
the same tasks via its own kickoff fan-out. To avoid the build's data-authoring step double-counting, these are
moved OUT of the primary `docs/variants/variants-*.md` glob and kept here as an independent **cross-check**.

**Canonical primary set:** `docs/variants/variants-*.md` (the 10 files in the parent dir) + `INDEX.md` + `gaps-research.md`.

**Why keep this cross-check:** it independently re-derived the same 13 tasks from the raw wiki and carries
specific corrections/flags the Architect (MV-A1) should reconcile against the primary set:
- **DIVERGENCE — dragons' weakness style.** This source reports dragons as **MAGIC**-weak via the wiki's
  elemental-weakness system (Water/Earth/Fire %), with lowest melee def = stab. The **current production dataset
  (`slayer-data.json`) marks Black/Blue/Red dragons as RANGED-weak.** This conflict is load-bearing for FR-6
  (single-variant tasks must reproduce today's behaviour) and must be an explicit MV-A1 design decision: does the
  variant model adopt the wiki elemental weakness or preserve the existing style-based weakness? Resolve before build.
- Vorkath = dual **draconic + undead** (Salve applies as well as dragonbane); it is a boss (separate trip).
- **Greater Skeleton Hellhound = UNDEAD, not demon.**
- Hellhounds & Waterfiends carry the demon attribute on the wiki but **demonbane: NO** (consistent with team
  memory: the demonbane set is exactly {Abyssal, Black, Greater demons, Nechryael}).
- All dragons + Skeletal Wyvern + Vorkath + Frost dragon = draconic (DHL/DHCB) — consistent with the pinned
  draconic set, plus the new Frost dragon.
- `gaps-a.md`: Frost dragon LOCATION unknown; Vorkath per-style def bonuses unknown; a few blank elemental types.

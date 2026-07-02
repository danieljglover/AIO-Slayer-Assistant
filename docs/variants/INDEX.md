# Monster Variant Enumeration — INDEX

MV-R1 deliverable. All data wiki-parsed from OSRS `Infobox Monster` (raw), **no guessed stats** —
unfindable values are `UNKNOWN` inline and listed in `gaps-research.md`.

- **Scope:** Duradel's 43 assignable tasks (`duradel-tasks.md`). **201 variant blocks** across 9 group files.
- **Missing from current dataset:** only **Frost Dragons** (Sailing 87 / Combat 85; npc id 14922) — captured
  in `variants-dragons.md`. Everything else in `slayer-data.json` matches Duradel's table.

## Files
| File | Tasks | Variant blocks |
|------|-------|----------------|
| `duradel-tasks.md` | Step-1 authoritative task list (43) + missing-flag + amount cross-check | — |
| `variants-demons.md` | Abyssal demons, Black demons, Greater demons, Nechryael | 28 |
| `variants-dragons.md` | Black/Blue/Red/Metal/Frost dragons | 24 |
| `variants-wyrms-wyverns.md` | Wyrms, Drakes, Fossil Island Wyverns, Skeletal Wyverns | 12 |
| `variants-undead-basilisks.md` | Aberrant spectres, Ankou, Vampyres, Basilisks | 19 |
| `variants-kalphite-misc.md` | Kalphites, Araxytes, Mutated zygomites, Aquanites | 14 |
| `variants-giants-beasts.md` | Fire giants, Dagannoth, Hellhounds, Dark beasts, Bloodveld, (+) | 32 |
| `variants-devils-aerial.md` | Smoke devils, Dust devils, Cave kraken, Aviansie, Gryphons | 16 |
| `variants-humanoids.md` | Trolls, Elves, Lizardmen, Suqahs, Cave horrors | 24 |
| `variants-misc.md` | Spiritual creatures, Warped creatures, TzHaar, Waterfiends, Kurask, Gargoyles | 32 |
| `variants-boss.md` | Boss meta-task: 32-boss assignable pool (no per-stat enum — bespoke trips) | — |
| `gaps-research.md` | All UNKNOWN/ambiguous values + resolution notes | — |
| `crosscheck/` | SECONDARY independent re-derivation of the 13 dragon/boss tasks (`mv-research-a`) — see `crosscheck/NOTE.md`. Use as a cross-check; reconcile its dragons-weakness DIVERGENCE (MAGIC-elemental vs the dataset's RANGED) in MV-A1. | — |

Per-variant fields: name; NPC id(s) or UNKNOWN; combat level; defence (level + stab/slash/crush/magic/ranged
bonuses); weakness (style + element/attack-type); category flags (demon/dragon/kalphite/undead); location(s);
requirement (quest/item/"boss — separate trip"); isBoss marker.

## Load-bearing data-quality findings for MV-A1 (the Architect MUST resolve these in design)
1. **Reanimated monsters do NOT count toward slayer tasks** — they were enumerated per the original brief but
   the wiki confirms they don't satisfy a task. **Flag for removal / exclusion** from selectable variants.
2. **Dire gryphon = the real superior; Shellbane gryphon = the BOSS** (the brief had these swapped). Dire
   gryphon full stats UNKNOWN (page not fetched) — chase if Gryphons gets per-variant superior support.
3. **Genuinely-UNKNOWN weakness (no defence profile to derive from):** all **Vampyres** (damage gated by
   silver/Ivandis/Blisterwood, not defence), all **Bloodveld**, **GWD/Reanimated hellhounds**. Design needs a
   defined fallback (task-level default weakness, or a "special handling" note) for variants with no usable
   defence-derived weakness — do NOT fabricate one.
4. **Multi-form bosses:** Kalphite Queen (crush crawling / magic airborne) and Dagannoth Kings (Rex MELEE /
   Prime RANGED / Supreme MAGIC) have form-dependent weakness — recorded per form. Decide v1 handling.
5. **Boss meta-task scope:** `variants-boss.md` is a 32-boss pool with no per-stat enum. Decide whether the
   "Boss" task participates in variant selection at all (recommend excluding — each boss is a bespoke trip).
6. **`drange` (ranged-defence bonus) unset** on several older infoboxes — treat missing as 0 / task default for
   v1; a live `NPCDefinition` second pass could fill later.
7. **Smoke/Dust devils barraged due to magic LEVEL 1, not low magic DEFENCE** (smoke devil magic def = 600) —
   the existing MAGIC weakness flag is still correct for them; just don't infer it from defence bonuses.

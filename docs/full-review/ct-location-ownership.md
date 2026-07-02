# CT-L - Location ownership map for the C2/C3 family fan-out

Status: BINDING fan-out contract for the C2 (Krystilia/Wilderness) and C3 (Turael/Spria/Mazchna
starter) authoring batches (`c2-c3-worklist.md` §1-§4, ADR-0019). This is the C2/C3 analogue of
`wc0-location-ownership.md` (which governed C1).
Authored 2026-07-02 by the backend/data engineer (batch CT-L).

## What this doc governs (and what it does not)

- **Governs FILENAMES only.** Its single job is to guarantee that no two parallel authoring agents
  create the same `locations/*.json` file. It assigns exactly one OWNER per genuinely new location
  and tells every other family to either reuse an existing file by id or create a family-tuned copy.
- **Does NOT re-author monster stats or flags.** Per ADR-0019 each family agent verifies its own
  `multi`/`cannon`/`burst`/`safeSpot`/`wilderness` flags against its family's own wiki page at
  authoring time. Where this doc states a flag it is a wiki-confirmed hint (cited), not a mandate;
  the authoring agent still verifies and, if a shared file's base flags do not fit its monster,
  makes a tuned copy (rule 2) rather than editing the shared file.

## Binding rules (identical to WC-0, restated)

1. **Reuse an existing location by exact `locationId`** when its flags match your family's wiki row.
   **Never edit an existing `locations/*.json` file.** If the base file's flags/prose are wrong for
   your family, create a family-tuned copy (rule 2).
2. **Family-tuned copy naming:** `<base-id>-<your-monster-plural>` (precedent:
   `fremennik-slayer-dungeon-turoths`, `charred-dungeon-lesser-demons`,
   `stalker-den-custodian-stalkers`). Family-suffixed names cannot collide across parallel agents.
3. **Create ONLY the filenames assigned to you below.** If authoring reveals a location this doc
   missed and another family also needs it, STOP and flag the DL rather than racing to create it.
4. **Schema (all 9 keys, every new file):** `locationId`, `name`, `multi`, `cannon`, `burst`,
   `konarLockable`, `safeSpot`, `wilderness`, `accessNote`. `LocationSourceCompletenessTest` reads
   the RAW key set and fails any file missing `safeSpot`/`wilderness`/`accessNote` (the booleans are
   primitives; a missing key silently compiles to `false`). Copy the shape of a recent sibling.
5. **Every Krystilia (C2) location MUST carry `wilderness: true`.** Krystilia kills only count in the
   Wilderness. Every C3 starter location is `wilderness: false` unless noted.
6. **`konarLockable`:** Konar assigns NONE of the C2/C3 families -> `konarLockable: false` on every
   new file in this wave. (Konar's assign list among the new families is empty; verified against the
   worklist master tables.)
7. **Honest UNKNOWN:** where a flag is not wiki-verifiable, use the conservative default
   (`multi:false`, `cannon:false`, `burst:false`, `safeSpot:false`) and say so in `accessNote`.
   Never guess a favourable flag.
8. **Boss variants carry NO location file** (ADR-0014). This makes the `wilderness-bosses` meta-task
   (C2 family #23) need ZERO new location files - see its section.

---

## Headline result

**The shared-created set is NOT empty (unlike WC-0's).** Four genuinely new locations are each needed
by 2+ net-new families. Each is assigned a single OWNER (the lowest `slayerTargetId` among the
sharing families, for determinism); every other sharer references it by id or, if flags diverge,
creates a tuned copy. All other new locations are single-family and collision-free.

### Final shared-created set (owner creates the base file; others reference or tuned-copy)

| # | new base location | owner (id) | referenced by | evidence |
|---|---|---|---|---|
| 1 | `lumbridge-swamp-caves` | cave-bugs (279) | cave-slime (281), wall-beasts (304); cave-crawlers (280) & rockslugs (301) as an ALT to their primary Fremennik | Lumbridge Swamp Caves wiki (fetched 2026-07-02): cave bug/cave slime/wall beast/cave crawler/rockslug all present; **cannon usable**; light source + spiny helmet needed; multi/safespot not stated (rule 7). `wilderness:false`. |
| 2 | `frozen-waste-plateau` | ice-giants (261) | ice-warriors (262) | Frozen Waste Plateau wiki (fetched 2026-07-02): ice giants + ice warriors (+ ice spiders) spawn here; **Wilderness level 50**; multi/cannon not stated (rule 7). `wilderness:true`. |
| 3 | `graveyard-of-shadows` | zombies (273) | skeletons (271), green-dragons (259) - **VERIFY** each on its own page before referencing; if their spawn is a distinct spot or flags differ, make a tuned copy instead | Graveyard of Shadows wiki (fetched 2026-07-02): wandering zombies confirmed (**Wilderness levels 18-21**); green-dragons worklist row cites "Graveyard of Shadows"; skeletons "Graveyard of Shadows" plausible but NOT confirmed in the fetched excerpt. multi/cannon not stated (rule 7). `wilderness:true`. |
| 4 | `bandit-camp-wilderness` | bandits (252) | black-knights (254), pirates (267) - **VERIFY** on each own page; black knights are "south of the Bandit Camp" and pirates "near the Bandit Camp", so they are likely DISTINCT adjacent spots -> tuned copies (`bandit-camp-wilderness-black-knights`, `-pirates`) are the expected outcome, not a shared reference | worklist rows #1/#3/#16 (Bandit Camp cluster east of the Ruins). PROVISIONAL - not separately wiki-fetched; the owner assignment prevents any collision regardless of the outcome. `wilderness:true`. |

Rows 1-2 are wiki-confirmed shared files. Rows 3-4 exist to PIN OWNERSHIP so parallel agents never
collide; the honest expectation is that skeletons/green-dragons/black-knights/pirates end up with
distinct spots or tuned copies. Either way the owner column is authoritative for the base filename.

---

## Existing locations reused (already on disk - reuse by id, tuned copy if flags differ)

| existing file | reused by (families) | note |
|---|---|---|
| `fremennik-slayer-dungeon` | cave-crawlers, cockatrice, pyrefiends, rockslugs | base EXISTS; flags differ per monster -> tuned copy `<base>-<monsters>` where needed. No shared-created (base exists). |
| `stronghold-of-security` | catablepon (2nd level), flesh-crawlers (2nd level), minotaurs (1st level) | base EXISTS; tuned copy per monster/level if flags/prose differ. |
| `slayer-tower` | banshees, crawling-hands (ground floor) | base EXISTS; tuned copy if the ground-floor flags differ from the base. |
| `catacombs-of-kourend` | banshees (alt), shades (alt) - verify | base EXISTS. Only reference if the family's wiki page lists it. |
| `king-black-dragon-lair`, `vetion-hellhound-spawns`, `various-boss-locations` | wilderness-bosses (if it used files - it does NOT, rule 8) | listed for completeness; see wilderness-bosses section. |

No existing-file EDIT is authorised in this wave (rule 1). Base-file flag divergences are handled by
tuned copies exactly as in C1.

---

## Wave C2 - Krystilia (Wilderness) families

All C2 locations carry `wilderness:true`, `konarLockable:false`. Shared families (#2 bears, #9
hill-giants, #11 ice-warriors, #19 scorpions, #20 skeletons, #21 spiders, #22 zombies) also author
their NON-Wilderness starter locations (marked "+ non-wildy").

| # | family (id) | locations to author (base slug) | ownership |
|---|---|---|---|
| 1 | bandits (252) | `bandit-camp-wilderness` | **OWNER** of shared #4 |
| 2 | bears (253) | `bears-wilderness` + non-wildy `bears-area` (grizzly bears/cubs) | own (single-family); use monster-suffixed slugs to avoid clashing with any generic "bears" |
| 3 | black-knights (254) | `bandit-camp-wilderness-black-knights` (tuned copy of #4) OR reference `bandit-camp-wilderness` if flags match | reference/tuned-copy shared #4 |
| 4 | chaos-druids (255) | `chaos-druid-wilderness` (Elder chaos druid: verify if it counts) | own |
| 5 | dark-warriors (256) | `dark-warriors-fortress` | own |
| 6 | earth-warriors (257) | `earth-warriors-wilderness` (below Edgeville dungeon / Obelisk of Air) | own |
| 7 | ents (258) | `ent-wilderness` (verify npcIds) | own |
| 8 | green-dragons (259) | reference/tuned-copy `graveyard-of-shadows` (shared #3) + any distinct wildy green-dragon spot as `green-dragons-wilderness` | reference shared #3; own extras |
| 9 | hill-giants (260) | `hill-giants-wilderness` (Giant Pen / Bone Yard) + non-wildy `hill-giants-area` (e.g. Edgeville Dungeon hill giants) | own |
| 10 | ice-giants (261) | `frozen-waste-plateau` | **OWNER** of shared #2 |
| 11 | ice-warriors (262) | reference `frozen-waste-plateau` (shared #2); + non-wildy `ice-warriors-area` if the starter spawn differs | reference shared #2; own non-wildy |
| 12 | lava-dragons (263) | `lava-dragon-isle` (deep Wildy; risk note) | own |
| 13 | magic-axes (264) | `axe-hut` (Wilderness Axe Hut; verify npcIds) | own |
| 14 | mammoths (265) | `mammoth-wilderness` (near Chaos Temple / west Wildy) | own |
| 15 | moss-giants (266) | `moss-giants-wilderness` | own |
| 16 | pirates (267) | `bandit-camp-wilderness-pirates` (tuned copy of #4) OR reference | reference/tuned-copy shared #4 |
| 17 | revenants (268) | `revenant-caves` (multi-way, PKer-heavy, safeSpot:false) | own |
| 18 | rogues (269) | `rogues-castle` (deep NE Wildy) | own |
| 19 | scorpions (270) | `scorpions-wilderness` + non-wildy `scorpions-area` | own |
| 20 | skeletons (271) | reference/tuned-copy `graveyard-of-shadows` (shared #3) + non-wildy `skeletons-area` | reference shared #3; own non-wildy |
| 21 | spiders (272) | `spiders-wilderness` + non-wildy `spiders-area` (do NOT fold deadly red spiders) | own |
| 22 | zombies (273) | `graveyard-of-shadows` + non-wildy `zombies-area` | **OWNER** of shared #3 |
| 23 | wilderness-bosses (274) | **NONE** (rule 8) | see below |

### wilderness-bosses (#23) - no location files

Per rule 8 / ADR-0014, boss variants carry a free-text `location` note only, no `locations/*.json`.
The recommended shape (worklist §2.1 option 1) is a new `wilderness-bosses.json` with one boss
variant each (Callisto/Artio, Venenatis/Spindel, Vet'ion/Calvar'ion, Chaos Elemental, Chaos Fanatic,
Crazy Archaeologist, Scorpia, King Black Dragon, + the revenant-caves-adjacent set), each variant's
`location` a note string. Reuse the existing `MonsterVariant.bossId` / `SLAYER_TARGET_BOSSID` (4723)
machinery. **This family authors ZERO location files** - its only shared-file risk is the
`TaskUnlockIntegrityTest` unlock-count pin if it adds a "Like a boss" unlock row (worklist §4:
isolate in batch C2-H, serialize vs C2-G revenants only if both touch that pin).

---

## Wave C3 - Turael / Spria / Mazchna starter families

All C3 locations `wilderness:false`, `konarLockable:false`, no extension/unlock (worklist §3.1).
The 7 shared families are authored in C2 (above) - not repeated here.

| # | family (id) | locations to author | ownership |
|---|---|---|---|
| 1 | banshees (275) | reuse `slayer-tower` (ground floor) -> tuned copy `slayer-tower-banshees` if flags differ | reuse existing |
| 2 | bats (276) | `bat-spawns` (bats/giant bats are common; pick the canonical wiki spot) | own |
| 3 | birds (277) | `bird-spawns` (confirm which birds count) | own |
| 4 | catablepon (278) | reuse `stronghold-of-security` (2nd level) -> tuned copy `stronghold-of-security-catablepon` if needed | reuse existing |
| 5 | cave-bugs (279) | `lumbridge-swamp-caves` | **OWNER** of shared #1 |
| 6 | cave-crawlers (280) | reuse `fremennik-slayer-dungeon` (primary); reference/tuned-copy `lumbridge-swamp-caves` (shared #1) as alt | reuse existing + reference shared #1 |
| 7 | cave-slime (281) | reference `lumbridge-swamp-caves` (shared #1) | reference shared #1 |
| 8 | cockatrice (282) | reuse `fremennik-slayer-dungeon` -> tuned copy `fremennik-slayer-dungeon-cockatrice` if flags differ | reuse existing |
| 9 | cows (283) | `cow-field` (Lumbridge/common) | own |
| 10 | crabs (284) | `crab-shores` (confirm which crabs count: sand/rock/etc.) | own |
| 11 | crawling-hands (285) | reuse `slayer-tower` (ground floor) -> tuned copy if flags differ | reuse existing |
| 12 | dogs (286) | `dog-spawns` (guard dog/jackal/wild dog) | own |
| 13 | dwarves (287) | `dwarven-mine` (Dwarven Mine / Keldagrim) | own |
| 14 | flesh-crawlers (288) | reuse `stronghold-of-security` (2nd level) -> tuned copy if needed | reuse existing |
| 15 | ghosts (289) | `ghost-spawns` | own |
| 16 | ghouls (290) | `canifis-ghoul-area` (west of Canifis, River Salve) | own |
| 17 | goblins (291) | `goblin-spawns` (very common) | own |
| 18 | hobgoblins (292) | `hobgoblin-area` (Vine path / Edgeville dungeon hobgoblins - NOT the wildy file) | own |
| 19 | icefiends (293) | `ice-mountain` | own |
| 20 | killerwatts (294) | `killerwatt-plane` (Draynor Manor; insulated boots note) | own |
| 21 | lizards (295) | `kharidian-desert-lizards` (ice cooler requiredItem) | own |
| 22 | minotaurs (296) | reuse `stronghold-of-security` (1st level) -> tuned copy `stronghold-of-security-minotaurs` if needed | reuse existing |
| 23 | mogres (297) | `mudskipper-point` (fishing-explosive/bailing note) | own |
| 24 | monkeys (298) | `monkey-spawns` (Karamja / Ape Atoll, non-M'amba) | own |
| 25 | pyrefiends (299) | reuse `fremennik-slayer-dungeon` -> tuned copy; `smoke-dungeon` EXISTS (alt) | reuse existing |
| 26 | rats (300) | `rat-spawns` (do NOT fold brine rats) | own |
| 27 | rockslugs (301) | reuse `fremennik-slayer-dungeon` (primary); reference/tuned-copy `lumbridge-swamp-caves` (shared #1) as alt | reuse existing + reference shared #1 |
| 28 | shades (302) | `mortton` (Mort'ton) -> and/or reuse `catacombs-of-kourend` if listed | own + maybe reuse |
| 29 | sourhogs (303) | `sourhog-cave` (Draynor) | own |
| 30 | wall-beasts (304) | reference `lumbridge-swamp-caves` (shared #1); spiny helmet requiredItem | reference shared #1 |
| 31 | wolves (305) | `white-wolf-mountain` (wolf/white wolf/big wolf) | own |

### C3 notes

- **Only `lumbridge-swamp-caves` (shared #1) is shared among net-new C3 families.** Every other new
  C3 location is single-family. Fremennik/Stronghold/Slayer-Tower/Catacombs are existing base files
  (reuse by id, tuned copy on flag divergence).
- `requiredItemId` families (banshees earmuffs, cockatrice mirror shield, lizards ice cooler,
  rockslugs bag of salt, wall-beasts spiny helmet, cave-slime light) resolve the item id from
  `net.runelite.api.gameval.ItemID` at authoring time (fever-spiders 6708 precedent). Do NOT invent.
- Slugs above are RECOMMENDED base names; the authoring agent may pick the canonical wiki spot name.
  If your chosen slug could clash with another net-new family, monster-suffix it (rule 2).

---

## araxytes contested UNKNOWN - RESOLVED (no data change)

Worklist §5.3 flagged `araxytes.json`'s turael/spria as a contested UNKNOWN: either (a) remove
turael/spria from `masterIds`, or (b) add their missing `weightByMaster`. Settled by live fetch
2026-07-02 of three pages:

- **Araxytes page** ("Assigned by"): Turael, Spria, Nieve, Duradel - category "Spiders, Araxytes".
  (Aya NOT listed.)
- **Turael page:** NO Araxytes row. Has a **Spiders** row: 15-30, weight 6.
- **Spria page:** NO Araxytes row. Has a **Spiders** row: 15-30, weight 6 (Spria total weight 178).

**Mechanic:** Turael/Spria do not assign "Araxytes" as a distinct weighted task. They assign the
generic **Spiders** task; araxytes are an ALTERNATIVE kill under Spiders once the player has 92
Slayer + Priest in Peril. So:

- Option (b) is WRONG: there is no araxyte-specific turael/spria weight to add; the weight-6 belongs
  to the Spiders task row, not an Araxytes row.
- Option (a) is WRONG: the Araxytes page itself still lists Turael/Spria under "Assigned by"
  (ADR-0019: the family's own page is the source of truth), so removing them contradicts the wiki.

**Resolution: the current `araxytes.json` is already correct and honest and is LEFT UNCHANGED.**
`masterIds` keeps turael/spria (they can assign indirectly); `amountByMaster` turael/spria = [15,30]
(the Spiders quantity); `weightByMaster` correctly OMITS turael/spria (no araxyte-specific weight -
the honest-UNKNOWN treatment WC-W pinned). `taskNotes` already documents the indirect-spiders
mechanic ("Turael/Aya and Spria can assign Araxytes indirectly as a spiders task."). No JSON touched,
so no source-data test run was required for this item.

**Downstream (C3 `rats`/`spiders` authors):** the C3 `spiders` family (272, authored in C2 as a
shared family) is the generic Turael/Spria/Mazchna Spiders task. Do NOT fold araxytes into it;
araxytes is its own on-disk family and the "spiders can alt-kill araxytes" relationship is
represented by araxytes.json keeping turael/spria in its masterIds, not by the spiders family
referencing araxytes.

---

## Provenance

- Repo facts: `ls`/`Read`/`jq` over `src/main/data/slayer/locations/` (206 files) and
  `tasks/araxytes.json` on the working tree, 2026-07-02.
- Wiki facts: WebFetch of Araxytes, Turael, Spria, Lumbridge Swamp Caves, Frozen Waste Plateau,
  Graveyard of Shadows, all fetched 2026-07-02. Shared-set rows 1-2 wiki-confirmed; rows 3-4 pin
  ownership to prevent collisions (authoring agents verify the exact spot per ADR-0019).
- Per-family monster stats/flags are the authoring agent's job (ADR-0019); this doc is filename
  ownership only.
</content>
</invoke>

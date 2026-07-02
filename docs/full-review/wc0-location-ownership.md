# WC-0 - Location ownership map for the C1 family fan-out

Status: BINDING fan-out contract for WC-1..WC-11 (plan.md Wave C1, ADR-0019).
Authored 2026-07-02 by the backend/data engineer (fr-wc0). Every location claim below was fetched
live from the OSRS wiki on 2026-07-02 (page names verified against the wiki `allpages` API; four
families have no `Slayer task/` subpage and were sourced from their monster page, per ADR-0019
"the family's own wiki page and the linked monster pages").

## Headline result

**The shared-created set is EMPTY.** Every location two or more C1 families need already exists in
`src/main/data/slayer/locations/` (Fremennik Slayer Dungeon, Catacombs of Kourend, Wilderness
Slayer Cave, Ruins of Tapoyauik). Every genuinely NEW location is needed by exactly ONE family, so
no shared file was pre-created and the fan-out is conflict-free provided every agent creates ONLY
the filenames assigned to it below.

Cross-family overlap matrix (the evidence):

| Location | Families (of the 11) | File status |
|---|---|---|
| Fremennik Slayer Dungeon | Turoth, Jellies | EXISTS `fremennik-slayer-dungeon` |
| Catacombs of Kourend | Jellies, Lesser demons | EXISTS `catacombs-of-kourend` |
| Wilderness Slayer Cave | Jellies, Lesser demons | EXISTS `wilderness-slayer-cave` |
| Ruins of Tapoyauik | Jellies, Lesser Nagua | EXISTS `ruins-of-tapoyauik` |
| every other location below | exactly one family | NEW, owned by that family |

## Binding rules (all 11 agents)

1. **Reuse an existing location by exact `locationId`** when its flags match your family's wiki
   row. Do NOT edit any existing `locations/*.json` file, ever; if the base file's flags or prose
   are wrong for your family, create a family-tuned copy instead (rule 2). Corrections the base
   file itself needs are out of C1 scope and are listed in "Existing-file notes" below.
2. **Family-tuned copy naming:** `<base-id>-<your-monster-plural>` (established precedent:
   `fremennik-slayer-dungeon-kurasks`, `karuulm-slayer-dungeon-wyrms`, `catacombs-of-kourend-fire-giants`,
   `stalker-den-zygomites`). Family-suffixed names cannot collide across parallel agents.
3. **Create ONLY the filenames assigned to you below.** If authoring reveals a location this doc
   missed, check this table first; if another family also needs it, STOP and flag the DL rather
   than racing to create it.
4. **Schema (all 9 keys, every new file):** `locationId`, `name`, `multi`, `cannon`, `burst`,
   `konarLockable`, `safeSpot`, `wilderness`, `accessNote`.
   `LocationSourceCompletenessTest` reads the RAW key set and fails any file missing
   `safeSpot`/`wilderness`/`accessNote` (the booleans are primitives; a missing key silently
   compiles to false). Copy the shape of a recent sibling such as `deepfin-mine.json`.
5. **Orphans are fine.** A location file not referenced by any `task.locationIds` is valid
   (GAP-5 precedent: 9 orphan `spiritual-*` files live in the tree, suite green at 792).
   Create your locations before or after your task file in any order.
6. **`konarLockable`:** true only if Konar assigns your family AND the location is one Konar can
   lock the task to (your slayer-task page). Konar assigns, of the 11: Turoth, Jellies, Brine
   rats, Hydras, Lesser Nagua. All other families: false on every new file.
7. **Honest UNKNOWN:** where a flag is not wiki-verifiable, use the conservative default
   (`multi:false`, `cannon:false`, `burst:false`, `safeSpot:false`) and say so in `accessNote`.
   Never guess a favourable flag; `cannon`/`burst`/`safeSpot` feed location scoring and hints.
8. **Boss variants carry NO location file** (ADR-0014: bosses keep a free-text `location` note
   only). Alchemical Hydra room, Amoxliatl, Zakl'n Gritch: notes, not files.

---

## Per-family ownership

### WC-1 Turoth
Source: https://oldschool.runescape.wiki/w/Slayer_task/Turoth (masters wiki-verified: Vannaka,
Chaeldar, Konar, Nieve).
- **Reuse:** `fremennik-slayer-dungeon` (wiki row: single, no cannon, safespottable - matches the
  base file's flags; sole turoth location, 22 spawns). Base accessNote is basilisk prose; if you
  want turoth prose (leaf-bladed/broad/Magic Dart requirement), tuned copy
  `fremennik-slayer-dungeon-turoths` per rule 2.
- **Shared-created:** none. **Unique-to-create:** none (tuned copy optional).

### WC-2 Jellies
Source: https://oldschool.runescape.wiki/w/Slayer_task/Jellies (masters wiki-verified: Krystilia,
Vannaka, Chaeldar, Konar).
- **Reuse:** `fremennik-slayer-dungeon` (10 jellies; single/no-cannon/safespot matches);
  `catacombs-of-kourend` (18 Warped Jellies; multi/no-cannon/safespot matches);
  `wilderness-slayer-cave` (9 jellies; multi+cannon match, but base `safeSpot:false` vs the wiki
  jellies row "safespottable" - tuned copy `wilderness-slayer-cave-jellies` if you want the
  safespot flag); `ruins-of-tapoyauik` (10 Chilled Jellies; single/no-cannon/safespot matches;
  base accessNote is blue-dragon prose - tuned copy `ruins-of-tapoyauik-jellies` optional).
- **Shared-created:** none. **Unique-to-create:** only the optional tuned copies named above.
- Variants: Jelly (Fremennik SD + Wilderness Slayer Cave), Warped Jelly (Catacombs), Chilled
  Jelly (Ruins of Tapoyauik).

### WC-3 Brine rats
Source: https://oldschool.runescape.wiki/w/Brine_rat (no `Slayer task/` subpage exists; monster
page lists masters Turael, Spria, Vannaka, Chaeldar, Konar, Nieve - NOTE this is BROADER than
G3's four; re-verify and author per the wiki, PD-D. Direct assignment requires Olaf's Quest;
47 Slayer).
- **Reuse:** none.
- **Unique-to-create:** `brine-rat-cavern` - sole location, 7 spawns, spade required to enter,
  two safespots (black bones SE, NW corner) so `safeSpot:true`; `konarLockable:true` (Konar
  assigns, sole location); `wilderness:false`. `multi`/`cannon` were not on the monster page -
  verify from the Brine Rat Cavern page, else rule 7 conservative defaults.

### WC-4 Custodian stalker
Source: https://oldschool.runescape.wiki/w/Slayer_task/Custodian_stalker (masters wiki-verified:
Chaeldar, Nieve; Shadows of Custodia required).
- **Reuse:** none. **CAUTION:** `stalker-den-zygomites` EXISTS but is the Ancient Zygomite tuned
  copy (its accessNote counts 9 zygomites) - do NOT link it.
- **Unique-to-create:** `stalker-den-custodian-stalkers` - Stalker Den is the sole location for
  all three variants (Juvenile 54 / Mature 67 / Elder 76 Slayer). Wiki: multicombat + cannonable
  zones in the south-west, single-combat caverns south-east and north; stalkers are aggressive.
  Flag call is yours (the sibling zygomite copy chose `multi:true, cannon:true`);
  `konarLockable:false` (Konar does not assign).

### WC-5 Hydras
Source: https://oldschool.runescape.wiki/w/Slayer_task/Hydras (master wiki-verified: Konar only).
- **Reuse:** `karuulm-slayer-dungeon` exists but its base flags are the dungeon-wide shape
  (`multi:true, cannon:false, safeSpot:false`) while the wiki hydra row is 17 hydras, SINGLE,
  CANNONABLE, no safespot - follow the established Karuulm tuned-copy pattern instead.
- **Unique-to-create:** `karuulm-slayer-dungeon-hydras` (single, cannon:true, safeSpot:false,
  konarLockable:true, wilderness:false; boots of stone/brimstone or Elite Kourend & Kebos diary
  in accessNote).
- Alchemical Hydra (boss, task-only) and Colossal Hydra (superior): variant notes, no location
  file (rule 8).

### WC-6 Lesser Nagua
Source: https://oldschool.runescape.wiki/w/Slayer_task/Lesser_Nagua (masters wiki-verified:
Chaeldar, Konar).
- **Reuse:** `ruins-of-tapoyauik` (Frost Nagua, 11 spawns; NOTE base `safeSpot:true` is
  blue-dragon-derived while the wiki Frost Nagua row says NOT safespottable - tuned copy
  `ruins-of-tapoyauik-frost-nagua` recommended). **CAUTION:** `neypotzli-wyrmlings` EXISTS but is
  the Wyrmling tuned copy - do NOT link it.
- **Unique-to-create:** `neypotzli-sulphur-nagua` (Sulphur Nagua; single, no cannon,
  safespottable per wiki; supplies can be made inside; Perilous Moons; konarLockable per Konar's
  list) and `crypt-of-tonali` (Earthen Nagua; The Final Dawn required; multi/cannon/safespot
  unspecified on the task page - verify from the Crypt of Tonali / Earthen Nagua pages, else
  rule 7 defaults with an honest accessNote; moonlight moths inside restore prayer).
- Amoxliatl (boss variant, Ruins of Tapoyauik): note, no location file (rule 8).

### WC-7 Scabarites
Source: https://oldschool.runescape.wiki/w/Slayer_task/Scabarites (master wiki-verified: Nieve
only; partial Contact! required for assignment).
- **Reuse:** none.
- **Unique-to-create:** `sophanem-dungeon-cavern` (27 locust riders + 18 scarab mages; multi:true,
  cannon:true, safeSpot:false), `sophanem-dungeon-maze` (2 locust riders + 10 scarab mages;
  multi:true, cannon:false, safeSpot:false), `uzer-mastaba` (19 Small scarabs; multi:false,
  cannon:false, safeSpot:false; partial The Curse of Arrav for access). All `wilderness:false`,
  `konarLockable:false`. Giant Scarab (Nightmare Zone) is not a location file.

### WC-8 Lesser demons
Source: https://oldschool.runescape.wiki/w/Slayer_task/Lesser_demon (masters wiki-verified:
Krystilia, Vannaka, Chaeldar).
- **Reuse (7):** `demonic-ruins` (multi/cannon/safespot match; wilderness), `taverley-dungeon`
  (5 demons; single/cannon/safespot match), `wilderness-slayer-cave` (7 demons; multi/cannon
  match; same base `safeSpot:false` caveat as Jellies - tuned copy
  `wilderness-slayer-cave-lesser-demons` if wanted), `catacombs-of-kourend` (8 demons;
  multi/no-cannon/safespot match), `isle-of-souls-dungeon` (4 demons; single/cannon/safespot
  match), `charred-dungeon` (6 demons; wiki row says NOT safespottable vs base `safeSpot:true` -
  tuned copy `charred-dungeon-lesser-demons` if wanted), `king-black-dragon-lair` (4 demons;
  wiki row says cannonable + safespottable vs the boss-flavoured base `cannon:false,
  safeSpot:false` - tuned copy `king-black-dragon-lair-lesser-demons` recommended).
- **Unique-to-create (pick a representative set; all are yours alone):**
  - `lava-maze` - the SURFACE Lava Maze (2 demons, single, cannon, safespot, level 40-45
    Wilderness, slash weapon/knife needed). **DISTINCT from the existing `lava-maze-dungeon`**
    (the underground black-dragon dungeon) - do not link or edit that file.
  - `chasm-of-fire-middle` - 9 demons (single, cannon, safespot; Yama contracts note). The
    existing `chasm-of-fire-bottom` is the black-demon bottom level - do not link it.
  - `sisterhood-sanctuary` - 9 demons (single, cannon, safespot; under Slepe). The existing
    `slepe` file is the surface Vyrewatch location - do not link it.
  - `crandor-and-karamja-dungeon` - 11 demons (single, cannon, safespot).
  - `crandor` - 2 demons (single, cannon, safespot).
  - `wizards-tower` - 1 caged demon (single, cannon, safespot).
  - `kourend-castle` - 1 caged demon (single, NO cannon, safespot; ranged/magic/halberd
    required; prayer altar + bank nearby).
  - `kingstown` - 1 demon (single, cannon, safespot).
  - `temple-of-ikov` - 3 demons (multi, cannon, safespot; shiny key + partial Temple of Ikov).
  - `viyeldi-caves` - 3 demons (multi, cannon, safespot; Legends' Quest area requirements).
  - `wilderness` flag: true ONLY on `lava-maze` here (demonic-ruins/wilderness-slayer-cave
    already carry it). `konarLockable:false` everywhere (Konar does not assign).
- Zakl'n Gritch (GWD, K'ril bodyguard): alternative kill note, no location file.

### WC-9 Shadow warriors
Source: https://oldschool.runescape.wiki/w/Shadow_warrior (no `Slayer task/` subpage; masters
wiki-verified: Vannaka, Chaeldar; Legends' Quest completion required for assignment).
- **Reuse:** none.
- **Unique-to-create:** `legends-guild-dungeon` - 19 spawns; safespottable (fence by the pit
  scorpions, fungus near the cavern wall) so `safeSpot:true`; `multi`/`cannon` not on the monster
  page - verify from the Legends' Guild Dungeon page, else rule 7 defaults. `wilderness:false`,
  `konarLockable:false`.

### WC-10 Jungle horrors
Source: https://oldschool.runescape.wiki/w/Jungle_horror (no `Slayer task/` subpage; masters
wiki-verified: Chaeldar, Vannaka; Cabin Fever required).
- **Reuse:** none. **CAUTION:** `mos-le-harmless-cave` EXISTS but is the CAVE HORROR cave -
  jungle horrors roam the island SURFACE. Do not link the cave file.
- **Unique-to-create:** `mos-le-harmless` - ~50 spawns across the island, 10-12 in the largest
  camp north of the cave exit; multiple safespots (walls, plants, trees) so `safeSpot:true`;
  `multi`/`cannon` not stated on the monster page - verify from the Mos Le'Harmless page, else
  rule 7 defaults. `wilderness:false`, `konarLockable:false`.

### WC-11 Fever spiders
Source: https://oldschool.runescape.wiki/w/Fever_spider (no `Slayer task/` subpage; masters
wiki-verified: Vannaka, Chaeldar; Rum Deal started; 42 Slayer; slayer gloves or take disease
damage).
- **Reuse:** none.
- **Unique-to-create:** `braindeath-island` - brewery basement, 11 spawns; many crate safespots
  (SW corner noted) so `safeSpot:true`; `multi`/`cannon` not stated - verify, else rule 7
  defaults. `wilderness:false`, `konarLockable:false`. Put the slayer-gloves requirement in
  `accessNote`.

---

## Existing-file notes (for the record - do NOT edit these in C1)

Divergences found while cross-referencing; each family works around them with tuned copies
(rule 2). If the DL wants the base files themselves corrected, that is a separate hygiene task:
- `wilderness-slayer-cave.json` `safeSpot:false` is hellhound-flavoured; the wiki marks jellies
  and lesser demons there safespottable.
- `king-black-dragon-lair.json` `cannon:false, safeSpot:false` is KBD-boss-flavoured; the lesser
  demon row says cannonable + safespottable.
- `charred-dungeon.json` `safeSpot:true` does not hold for its lesser demons (wiki: not
  safespottable).
- `ruins-of-tapoyauik.json` `safeSpot:true` + blue-dragon accessNote does not hold for Frost
  Nagua (wiki: not safespottable).
- `karuulm-slayer-dungeon.json` `multi:true, cannon:false` does not hold for the hydra area
  (wiki: single, cannonable).

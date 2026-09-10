# Bundled routing source evidence

The bundled routing snapshot resolves 387 exact monster/location bindings from
859 bindings in the authored source graph: 266 lead to monster spawn groups and
121 lead to clearly labelled entrances or dungeon arrival points. It contains
4,841 distinct targets across routes, with at most 189 targets in one route.
472 bindings remain unavailable. These counts describe coordinate coverage,
not verification of every inherited catalogue association or every walking path.

The editable source is `src/main/data/slayer/advisor/routing-destinations.json`.
`RouteDestinationCompiler` validates the exact monster/location links, unique
route IDs and pair bindings, explicit integer coordinates and planes, 1..512
unique targets, arrival labels, and pinned evidence. The runtime consumes the
compiled bundle only; it does not fetch Wiki data or infer coordinates.

## Authoring and evidence

`scripts/author-routing-destinations.py` reads the modular source graph and raw
OSRS Wiki revisions through the MediaWiki API. The API requests include page
IDs, revision IDs, revision timestamps, and main-slot source. Each destination
contains the exact Wiki evidence used for its spawn row or entrance. Missing
pages and rows are retained as gaps, never as proof of a location. Raw source
pages are cached under `/tmp/aio-routing-wiki-cache`; the authoring page archive
is `/tmp/aio-routing-pages.json`. Neither is a runtime input.

Location aliases in the script are reviewed authoring mappings, not fuzzy
runtime searches. Selected combat levels must be explicitly included when the
Wiki row lists levels. Some rows aggregate several levels of the same species;
those routes disclose that individual spawn tiles do not identify a particular
combat level. Generic non-Wilderness groups exclude Wilderness rows, ambiguous
region classification, and identified temporary/instanced quest contexts.
Cow fields, hobgoblins, common skeletons and rats use bounded ordinary areas.
The 74/86 Zygomite subareas and eastern task-only Gryphon dungeon have explicit
variant/area guards. Ordinary araxytes do not inherit Araxxor's boss destination.

## Coordinate interpretation

Wiki `mapID=-1` is the full world map; surface/default `mapID=0` points retain
explicit source planes (or Module:Map's documented default zero). Positive map
IDs can rearrange dungeon rooms and floors and must not be treated directly as
RuneLite WorldPoints. For these maps the script applies only unique inverse
rectangles from the pinned Quest Helper mapping source. It does not use an
identity fallback for unmatched legacy/custom map layers. Distinct conversion
results are rejected. A route may retain the independently converted subset
while explicitly disclosing omitted points.

The current Wiki map registry was checked against
`MediaWiki:Kartographer-map-version`, page 348925, revision 15294527, timestamp
2026-08-12T20:50:46Z (map version `2026-08-12_a`). Wiki map IDs are matched by
reviewed area names, never by Quest Helper enum ordinals. Four old mappings for
Karamja/Miscellania area names were excluded because those names are absent from
the current registry. Map ID 9 must not be treated as old Karamja Underground.
The script contains 132 non-mirror inverse rectangles. The two known overlapping
God Wars rectangles are accepted only when their resulting WorldPoints agree.

Pinned coordinate sources:

- [Quest Helper mapping table](https://github.com/Zoinkwiz/quest-helper/blob/633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a/src/main/java/com/questhelper/util/worldmap/WorldMapPointMapping.java)
  and [inverse conversion](https://github.com/Zoinkwiz/quest-helper/blob/633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a/src/main/java/com/questhelper/util/worldmap/WorldPointMapper.java).
- [Shortest Path transport endpoints](https://github.com/Skretzo/shortest-path/blob/6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a/src/main/resources/transports/seasonal_transports.tsv)
  and [Wilderness lever endpoint](https://github.com/Skretzo/shortest-path/blob/6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a/src/main/resources/transports/teleportation_levers.tsv).
- [Wiki map registry documentation](https://oldschool.runescape.wiki/w/RuneScape:Map/mapIDs)
  and [current registry](https://maps.runescape.wiki/osrs/versions/2026-08-12_a/basemaps.json).

Transport landing coordinates are used as destination evidence only. They do
not enable or assume a seasonal teleport. Their labels and geography were
reviewed against current Wiki encounter/location evidence. The upstream entry
labelled Araxxor at 3748,9373 was rejected because it identifies a different
cave. Araxxor instead uses the current Wiki entrance scenery at 3656,3408.
Scurrius uses the exact normal manhole approach at 3236,3458 and directs players
east through Varrock Sewers. King Black Dragon uses the
Wilderness-side lever at 3067,10253. Entrance routes describe where they stop;
for example, the Wilderness Edgeville skeleton route ends at the surface
Edgeville Dungeon entrance and explicitly says to continue to the Wilderness
section. Its original Wiki marker (3097,3469) was blocked in manual routing;
the bundled endpoint is the normal transport origin (3096,3468), corroborated
by the reverse ladder transport. Fremennik, Brimhaven, Taverley, Wilderness
Slayer Cave, Mos Le'Harmless and Ancient Cavern entrances likewise use exact
normal transport origins. Stronghold Slayer Cave uses a normal Slayer-ring
arrival coordinate; this does not assume use or ownership of the ring. This does not assert that the endpoint is the monster's spawn.

## Remaining coverage

- 88: No independent spawn row: conditional encounter/superior or Wiki coordinate gap.
- 101: Legacy map coordinates lack an unambiguous world conversion or verified entrance.
- 283: No compatible exact location/level row; inherited join or unreviewed source label.

The first category involving incompatible rows mixes contradictory inherited
joins with source labels that still need individual review; it is not a claim
that every omitted pair is impossible. Examples include combat-level variants
joined to dungeons where only another level is listed, quest-only bosses,
superior monsters without independent spawns, and ordinary variants inheriting
boss locations. Valid matching rows whose legacy coordinates cannot yet be
converted remain explicit coordinate gaps. The full pair list is in the
latest authoring report at `/tmp/aio-routing-data-report.md`; its structured
counterpart is `/tmp/aio-routing-gaps.json`.

The six canonical bosses flagged in the bounded entrance review now have
normal-source approaches: Abyssal Sire (DIP arrival), Cerberus (Taverley cave
origins), Giant Mole (Falador Park arrival), Sarachnis (Forthos entrances),
Scorpia (Scorpion Pit cavern origins), and Thermonuclear smoke devil (BKP
arrival). Falador Park and BKP are explicitly labelled approaches, not the boss
or dungeon entrance tile. Their current Wiki transportation/location context
corroborates the normal-source coordinates. Some inherited alternate boss
locations remain unsupported, including Kraken at generic Kraken Cove and
Thermonuclear smoke devil at generic Smoke Devil Dungeon; the canonical boss
location has a route. Quest-only/superior variants and contradictory joins are
not replaced by routes to another encounter.

Araxxor and Shellbane retain current Wiki scenery/entrance pins because no
normal transport approach was available in the bounded review. Their world
coordinates are source-backed, but footprint walkability has not been manually
verified. The route result must not be presented as reachable unless Shortest
Path actually finds a complete path.

Source validation uses `jq empty` and the normal `generateSlayerData`/`build`
compiler validation. No automated tests or game interactions were added. Source
coordinate validation cannot prove that every path remains reachable with a
particular account's unlocks; Shortest Path owns its path/transport evaluation.

## Quest Helper mapping license

The inverse rectangles are derived from the pinned Quest Helper mapping table.
The following source notice is retained for that material:

```text
/*
 * Copyright (c) 2024, Zoinkwiz <https://github.com/Zoinkwiz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
```

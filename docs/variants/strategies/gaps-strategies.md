# Strategy-research gaps

Items where the OSRS wiki gave no usable guide-recommended **weapon** (or no page at all), or that could not be
processed because no variant profile exists in `slayer-data.json`. Append-only.

## Bosses

### No recommended-weapon block on the wiki (page exists, tactics only → `primaryWeapons: UNKNOWN`)
- **Dad** (Troll Stronghold quest boss) — https://oldschool.runescape.wiki/w/Dad — Strategy section gives only
  tactics + a 40% earth-spell weakness; names no weapon. Plugin style MELEE is defensible (Protect from Melee
  blocks all his damage). Low-level earth spells noted for safespotting.
- **Ice Troll King** (Fremennik quest boss) — https://oldschool.runescape.wiki/w/Ice_Troll_King — guide says
  MELEE is best (magic/ranged defence ~2000 vs melee 45) but names no weapon. 35% fire weakness noted but
  impractical. An alt recoil/Neitiznot-shield cheese exists. Plugin MELEE correct.
- **Arrg** (Troll Romance quest boss) — https://oldschool.runescape.wiki/w/Arrg — strategy redirects to
  Troll Romance#Fighting Arrg; no equipment block. Lowest defence crush/stab (35) vs slash (60); 50% earth-spell
  weakness. Uses melee (slash) + ranged. Plugin MELEE plausible but unconfirmed by a weapon rec.

### No strategy page / not a real boss
- **Dark Ankou** — https://oldschool.runescape.wiki/w/Dark_Ankou — page has only a "Prayer info" section; no
  Strategy and no equipment block. It is a minion randomly summoned during the Skotizo fight (max hit 8, 60 HP),
  not a standalone boss; the wiki says there is no benefit to fighting them. Plugin style correctly UNKNOWN.

### Deferred / un-enumerated — no variant profile in slayer-data.json (cannot attach a strategy)
The backend deliberately deferred these (none fabricated), so there is no boss variant block to research:
- **Duke Sucellus**, **The Leviathan**, **Vardorvis**, **The Whisperer** — DT2 bosses.
- **Commander Zilyana** — GWD Saradomin boss (weakness was UNKNOWN at MV-R2).
- **Zulrah magma (crimson) form** — third Zulrah form (green + tanzanite are in the data; magma deferred).
- **Tormented Demon** — named in the MV-SR brief but NOT present as a boss variant in slayer-data.json, so not
  processed. (If added later: demonbane melee Emberlight/Arclight + Scorching bow ranged switch for the shield
  phase — author from `Tormented_Demon/Strategies` when enumerated.)

### Notes / corrections worth flagging to the team
- **Branda the Fire Queen** and **Shellbane gryphon** were flagged "possibly fabricated" in the research brief but
  are BOTH real OSRS bosses with full /Strategies pages (Royal Titans and the Sailing-update Gryphons boss
  respectively). Their gear is captured in `strategies-bosses.md` — no gap.
- **Dagannoth Kings** counter-styles confirmed against the wiki and match the plugin data exactly
  (Prime→RANGED, Rex→MAGIC, Supreme→MELEE).

## Monsters
(non-boss slice, MV-SR — owned by Research-monsters)

None unresolved. All 151 non-boss variants (53 distinct groups) were checked against
`oldschool.runescape.wiki/w/<Monster>/Strategies` on 2026-06-29 via `action=raw`; every monster resolved
cleanly to either a real page (16 groups → gear extracted in `strategies-monsters.md`) or 404 / a
`Slayer task/*` redirect (= no real combat strategy page → stat engine). No ambiguous or unreachable cases.

Minor notes (recorded, not blocking):
- **Tormented Demon** DOES have a usable non-boss /Strategies page and IS present as a non-boss variant in
  slayer-data.json (under Greater demons) — gear captured in `strategies-monsters.md`. (Cross-ref: the Bosses
  section above noted it is absent as a *boss* variant; the two are consistent — it's a non-boss entry.)
- **Frost dragon/Strategies** carries an `{{Incomplete}}` banner ("Equipment Guide is a loose outline, likely
  not completely accurate"). Gear extracted is the page's current best (DHL on crush; DHCB ranged; DHW/Harmonised
  magic) but may shift as the (Sailing-era) page matures.
- `Iron dragon/Strategies` redirects into `Metal dragons/Strategies`; Bronze/Steel have no own page and rely on
  that combined guide. Mithril/Adamant/Rune each have a standalone page.
- Generic `Slayer task/*` redirects (Dust devil, Dark beast, Drake, Suqah, Greater Nechryael) are **not** real
  combat strategy pages and are recorded as no-page → stat engine.

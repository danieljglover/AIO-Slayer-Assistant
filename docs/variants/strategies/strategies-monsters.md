# Slayer Monster /Strategies pages — non-boss variants (MV-SR)

Scope: every **non-boss** variant (`isBoss=false`) in `src/main/resources/data/slayer-data.json`
(43 tasks → **151 distinct non-boss variant names**). For each distinct monster we checked whether
`https://oldschool.runescape.wiki/w/<Monster>/Strategies` exists. Where it does, the guide's recommended
gear is extracted so the build can prefer it over the stat engine; where it does not (the **majority**),
the build falls back to the stat/DPS engine.

Method: `curl -s '<url>?action=raw'` against the live OSRS wiki (oldschool.runescape.wiki), 2026-06-29.
- HTTP 404 = no page.
- A page whose wikitext starts with `#REDIRECT` to a **`Slayer task/<x>`** target = NO real /Strategies
  page (that's the generic slayer-guide redirect, not a combat strategy page) — recorded as **no**.
- Leveled / location / god / superior variants that share one strategy are grouped under their base monster.
- Gear extracted from the page's `{{Recommended equipment}}` blocks (priority order as written) + the prose.
  `UNKNOWN` where the page is ambiguous; nothing invented.

## Headline
- **Distinct monsters checked:** 53 groups (covering all 151 variant names).
- **HAD a /Strategies page (16 groups → 28 of the 151 variant names):** Smoke devil, Aquanite, Aviansie,
  Basilisk Knight, Demonic gorilla, Tormented Demon, Lizardman shaman, Vyrewatch Sentinel, Skeletal Wyvern,
  Fossil Island wyverns (Ancient/Spitting/Taloned/Long-tailed — one shared "Wyverns guide" page),
  Frost dragon, Brutal black dragon, and the metal/draconic dragons {Bronze, Iron, Steel → *Metal dragons*
  combined page; Mithril, Adamant, Rune → own pages}.
- **The rest = NO page → stat engine.** This is the expected majority (see the explicit no-page list below).

---

## Monsters WITH a /Strategies page (prefer the guide gear)

| Monster (group) | Page | Primary style |
|---|---|---|
| Smoke devil | [/w/Smoke_devil/Strategies](https://oldschool.runescape.wiki/w/Smoke_devil/Strategies) | MAGIC (Ice Barrage) |
| Aquanite | [/w/Aquanite/Strategies](https://oldschool.runescape.wiki/w/Aquanite/Strategies) | MELEE |
| Aviansie | [/w/Aviansie/Strategies](https://oldschool.runescape.wiki/w/Aviansie/Strategies) | RANGED |
| Basilisk Knight | [/w/Basilisk_Knight/Strategies](https://oldschool.runescape.wiki/w/Basilisk_Knight/Strategies) | MELEE (RANGED to safespot) |
| Demonic gorilla | [/w/Demonic_gorilla/Strategies](https://oldschool.runescape.wiki/w/Demonic_gorilla/Strategies) | HYBRID melee+ranged (prayer-switcher) |
| Tormented Demon | [/w/Tormented_Demon/Strategies](https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies) | MELEE (demonbane) |
| Lizardman shaman | [/w/Lizardman_shaman/Strategies](https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies) | RANGED |
| Vyrewatch Sentinel | [/w/Vyrewatch_Sentinel/Strategies](https://oldschool.runescape.wiki/w/Vyrewatch_Sentinel/Strategies) | MELEE (Blisterwood) |
| Skeletal Wyvern | [/w/Skeletal_Wyvern/Strategies](https://oldschool.runescape.wiki/w/Skeletal_Wyvern/Strategies) | MELEE (dragonbane) / RANGED |
| Fossil Island wyverns (Spitting / Taloned / Long-tailed / Ancient) | [/w/Ancient_Wyvern/Strategies](https://oldschool.runescape.wiki/w/Ancient_Wyvern/Strategies) ("Wyverns guide") | MELEE (dragonbane) |
| Frost dragon | [/w/Frost_dragon/Strategies](https://oldschool.runescape.wiki/w/Frost_dragon/Strategies) | MELEE on crush (dragonbane) |
| Brutal black dragon | [/w/Brutal_black_dragon/Strategies](https://oldschool.runescape.wiki/w/Brutal_black_dragon/Strategies) | RANGED |
| Bronze / Iron / Steel dragon | [/w/Metal_dragons/Strategies](https://oldschool.runescape.wiki/w/Metal_dragons/Strategies) (Iron redirects here) | MELEE (dragonbane) |
| Mithril dragon | [/w/Mithril_dragon/Strategies](https://oldschool.runescape.wiki/w/Mithril_dragon/Strategies) | MELEE (dragonbane) |
| Adamant dragon | [/w/Adamant_dragon/Strategies](https://oldschool.runescape.wiki/w/Adamant_dragon/Strategies) | MELEE (dragonbane); Shadow fastest off-task |
| Rune dragon | [/w/Rune_dragon/Strategies](https://oldschool.runescape.wiki/w/Rune_dragon/Strategies) | MELEE (dragonbane); Shadow best overall |

### Smoke devil
- **Covers variants:** Smoke devil (Nuclear smoke devil = superior, no own page → use this guide).
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MAGIC**
- `primaryWeapons` (priority): Kodai wand → Volatile nightmare staff / Nightmare staff → Dragon hunter wand / Ancient sceptre → Blue moon spear → Accursed sceptre (a)
- `secondaryWeapons`: none for a different style — barrage with a cannon is the method (Ice Barrage at 94 Magic, Ice Burst at lower); lured ranged (magic shortbow) is an ironman-only alternative.
- `keyGearNote`: BIS magic-damage gear (Virtus/ancestral); use a staff/wand that autocasts Ancient Magicks (Kodai = +15% mdmg + infinite/saved water runes). Bracelet of slaughter over tormented bracelet for XP/h. Facemask/Slayer helm required to enter.

### Aquanite
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (slash to sever the lure, then stab)
- `primaryWeapons` (priority): Ghrazi rapier → Osmumten's fang → Blade of saeldor (on stab) → Abyssal dagger → Voidwaker (on stab)
- `secondaryWeapons`: a fast 4-tick slash weapon (Abyssal whip / Scimitar) or Saradomin godsword spec — used to **sever the lure** (drops stab Defence 60→10); Voidwaker's regular slash also severs.
- `keyGearNote`: Attacks with Magic — pray Protect from Magic. Slash severs the lure (no damage needed) → big stab-accuracy gain; carry a fast slash weapon for the sever, then stab. SGS spec severs + restores prayer.

### Aviansie
- **Covers variants:** Aviansie, Flight Kilisa, Flockleader Geerin, Wingman Skree (the GWD Armadyl minions share the gear).
- `hasStrategyPage`: **yes**
- `primaryStyle`: **RANGED**
- `primaryWeapons` (priority): Toxic blowpipe → Bow of faerdhinen (w/ crystal armour) → Eclipse atlatl → Hunters' sunlight crossbow → any crossbow (Zaryte/Armadyl/Dragon)
- `secondaryWeapons`: Wilderness-GWD low-risk set is also ranged (Toxic blowpipe → Armadyl crossbow → Dragon crossbow → Magic shortbow (i)).
- `keyGearNote`: BIS ranged gear; wear an Armadyl-affiliated item **and** a Zamorak item (and ideally a Saradomin item) to avoid aggression in GWD. Protect from Missiles. Regular GWD vs Wilderness GWD have separate sets.

### Basilisk Knight
- **Covers variants:** Basilisk Knight (and the Sentinel-tier basilisk is its own non-page monster — uses stat engine).
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (fastest); **RANGED** preferred for safe safespotting
- `primaryWeapons` (melee, priority): Inquisitor's mace → Ursine chainmace (u) / Osmumten's fang → Zamorakian hasta (crush) / Zombie axe (crush)
- `secondaryWeapons` (ranged, priority): Dragon dart / Dragon knife / Hunters' sunlight crossbow → Toktz-xil-ul / Rune knife → Zaryte crossbow → Armadyl crossbow → Dragon hunter crossbow / Dragon crossbow. Zaryte crossbow spec (Ruby/Dragonstone dragon bolts (e)).
- `keyGearNote`: Requires **V's shield** to survive (their petrify); weak to **heavy ranged** so crossbows are typical. Pray Protect from Magic and tank/safespot. At higher levels fast crush weapons (hasta/cudgel) can beat the fang/rapier.

### Demonic gorilla
- **Covers variants:** Demonic gorilla (counts as black demons / monkeys). Note: the data file lists Balfrug Kreeyath & Porazdir under Black demons — those are separate, no page.
- `hasStrategyPage`: **yes**
- `primaryStyle`: **HYBRID** — must bring **≥2 styles** (gorillas overhead-pray and switch). Recommended pair = melee + ranged.
- `primaryWeapons` (melee, priority): Emberlight → Arclight (demonbane; they count as black demons) → Osmumten's fang → Noxious halberd
- `secondaryWeapons` (ranged, priority): Twisted bow → Scorching bow → Toxic blowpipe → Bow of faerdhinen (full crystal) → Eclipse atlatl → Hunters' sunlight crossbow / Karil's crossbow
- `keyGearNote`: Each gorilla cycles all three protection prayers (switches after 70+ unblocked damage) and attacks in all three styles — you switch both attack style and protection prayer. Demonbane melee (Emberlight/Arclight) is strong since they're black demons. SGS/blowpipe spec for sustain.

### Tormented Demon
- `hasStrategyPage`: **yes**
- `sourceUrl`: https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies
- `primaryStyle`: **MELEE** (strongest; demonbane)
- `primaryWeapons` (melee, priority): Emberlight (stab) → Arclight (stab) → Abyssal bludgeon → Abyssal dagger → Osmumten's fang → Belle's folly → Abyssal tentacle → Abyssal whip → Zamorakian hasta / Sarachnis cudgel
- `secondaryWeapons`: shield-break needs a **crush** melee weapon OR **heavy ranged** (crossbow/ballista) OR a **spell** (not powered-staff); for the shieldless burst: Dharok's greataxe / Tzhaar-ket-om / Dragon 2h / Granite maul / SGS spec. Ranged option: Scorching bow / Twisted bow / Toxic blowpipe. Magic: Dark Demonbane (82 Magic).
- `keyGearNote`: Demon → demonbane (Arclight/Emberlight) is core; set to **crush** (or use heavy ranged / spells) to break the fire shield, then punish during the shieldless window. Prayer-switcher (melee/magic/ranged + fire bombs).

### Lizardman shaman
- **Covers variants:** Lizardman shaman (Lizardman, Lizardman brute = no page → stat engine).
- `hasStrategyPage`: **yes**
- `primaryStyle`: **RANGED** (recommended for mobility; melee viable)
- `primaryWeapons` (ranged, priority): Toxic blowpipe (amethyst/adamant darts) → Rosewood blowpipe / Bow of faerdhinen → Twisted bow → Karil's crossbow / Hunters' sunlight crossbow / Scorching bow → Rune crossbow / Magic shortbow (i) / Crystal bow
- `secondaryWeapons` (melee, priority): Ghrazi rapier / Noxious halberd (on stab) → Osmumten's fang → Scythe of vitur → Voidwaker (on stab) → Zamorakian hasta / Abyssal dagger (p++) → Belle's folly
- `keyGearNote`: Weak to **stab** (negative stab Defence) and **ranged** (no ranged Defence); avoid slash/crush/magic (high defence there). On task: complete Hard Kourend & Kebos Diary + talk to Captain Cleive so the **Slayer helm** gets the **Shayzien** acid-protection effect (else wear Shayzien helm (5)). Protect from Missiles.

### Vyrewatch Sentinel
- **Covers variants:** Vyrewatch Sentinel (Feral Vampyre, Vampyre Juvinate, Vyrewatch = no page → stat engine).
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE**
- `primaryWeapons` (priority): Blisterwood flail (the required vampyre-bane weapon)
- `secondaryWeapons`: shield = Antler guard (with Burst of strength often beats Avernic defender for damage + afk).
- `keyGearNote`: Vyres require a **vampyre-specific weapon** — Blisterwood flail. Protect from Melee (43 Prayer); use the Darkmeyer altar to refill prayer cheaply.

### Skeletal Wyvern
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (dragonbane, BIS) — **RANGED** equally promoted for safespotting
- `primaryWeapons` (melee, priority): Dragon hunter lance → Inquisitor's mace / Blade of saeldor → Osmumten's fang → Abyssal whip → Zombie axe
- `secondaryWeapons` (ranged, priority): Dragon hunter crossbow → Bow of faerdhinen (crystal) / Hunters' sunlight crossbow → other crossbows. Dragonstone dragon bolts (e) work (one of the few draconic creatures hit by Dragon's breath).
- `keyGearNote`: **Draconic → dragonbane** (DHL / DHCB). Must carry an anti-icy-breath **shield** (elemental/mind/dragonfire/wyvern shield) — the freeze chance scales with magic Defence; swap to a ranged-bonus shield once tolerant when safespotting. Cannon allowed in lower Asgarnian Ice Dungeon.

### Fossil Island wyverns (Spitting / Taloned / Long-tailed / Ancient Wyvern)
- **Covers variants:** all four Fossil Island wyvern variants share the single "Wyverns guide" at Ancient Wyvern/Strategies.
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (tank ranged-Defence, dragonbane)
- `primaryWeapons` (melee, priority): Dragon hunter lance (stab; on slash with oathplate) → Osmumten's fang → Ghrazi rapier → Zamorakian hasta / Abyssal dagger → Abyssal whip
- `secondaryWeapons`: Magic — Eye of ayak → Dragon hunter wand → Sanguinesti staff → Trident of the swamp → Twinflame staff. Ranged — Dragon hunter crossbow → Hunters' sunlight crossbow → Zaryte/Armadyl/Dragon/Rune crossbow.
- `keyGearNote`: **Draconic → dragonbane.** Tank by maximising **ranged Defence** (Barrows/Bandos/Justiciar) + Protect from Melee (their ranged hits through prayer). Anti-icy-breath shield (Ancient/Dragonfire) needed but note its negative offensive bonuses.

### Frost dragon
- **Covers variants:** Frost dragon (Sailing-era, Grimstone Dungeon; task 43 in the dataset). Page marked `{{Incomplete}}` but exists and is gear-specific.
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE on crush** (weakest to crush, then stab; dragonbane)
- `primaryWeapons` (melee, priority): Dragon hunter lance (on crush)
- `secondaryWeapons`: Ranged — Dragon hunter crossbow → Zaryte crossbow → Armadyl crossbow → Dragon crossbow (heavy ranged best ranged option); Magic — Dragon hunter wand → Harmonised nightmare staff (100% **fire** elemental weakness, not the usual water). Zaryte crossbow / Toxic blowpipe spec listed.
- `keyGearNote`: Unlike chromatic dragons, **weakest to crush then stab** and **100% weak to fire spells**. As a dragon, weak to dragonbane. Standard dragonfire protection fully negates their icy breath. Safespottable or Protect from Melee + full dragonfire protection.

### Bronze / Iron / Steel dragon (Metal dragons combined page)
- **Covers variants:** Bronze dragon, Iron dragon, Steel dragon. (Iron dragon/Strategies `#REDIRECT`s to Metal dragons/Strategies; Bronze/Steel have no own page and use this combined guide.) Mithril/Adamant/Rune have their own pages (below).
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (dragonbane; magic if no dragonbane weapon)
- `primaryWeapons` (melee, priority): Dragon hunter lance → Osmumten's fang → Ghrazi rapier → Zamorakian hasta → Abyssal dagger
- `secondaryWeapons`: Ranged — Dragon hunter crossbow → Zaryte/Armadyl → Dragon crossbow → Rune crossbow. Magic — Dragon hunter wand / Harmonised nightmare staff (Earth spells) → Sanguinesti staff.
- `keyGearNote`: Weak to **stab** + **heavy ranged** + **Earth spells** (50% bonus). Very high Defence → maximise stab bonus; DHL is best stab vs draconic, then fang. **Do NOT use slash** (Abyssal whip) — high slash Defence. No dragonbane weapon → use magic (Earth) setup.

### Mithril dragon
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (dragonbane, cost-effective) — magic comparable with BIS
- `primaryWeapons` (melee, priority): Dragon hunter lance / Osmumten's fang → Ghrazi rapier / Zamorakian hasta (if no DHL/fang)
- `secondaryWeapons`: Magic — Tumeken's shadow (top off-task / 2nd on-task) → Dragon hunter wand / Harmonised nightmare staff (Earth Surge) / Eye of ayak → Sanguinesti / Trident of swamp. Ranged — Dragon hunter crossbow → Hunters' sunlight crossbow → Zaryte/Armadyl/Dragon → Rune crossbow (Ruby/Diamond dragon bolts (e)). Tank variant: Fang/DHL + Justiciar + Elysian.
- `keyGearNote`: **Draconic → dragonbane**; 50% Earth-spell weakness makes magic strong. Bring a defender + extended super antifire (not anti-dragon/dragonfire shield) for best DPS. High ranged Defence → DHCB nearly required if ranging.

### Adamant dragon
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (dragonbane reliable at base-90 melee); **Tumeken's shadow fastest off-task**
- `primaryWeapons` (melee, priority): Dragon hunter lance → (Ghrazi rapier / Osmumten's fang as alternatives at base 90)
- `secondaryWeapons`: Ranged — Dragon hunter crossbow (passive makes kills easy) → Twisted bow (comparable) → Zaryte/Armadyl/Dragon. Magic — Tumeken's shadow (fastest off-task) → Harmonised nightmare staff / Dragon hunter wand / Eye of ayak (Earth spells).
- `keyGearNote`: **Draconic → dragonbane**; low-ish Magic Defence + elemental (Earth) weakness → Shadow beats dragonbane off-task. Bandos/Justiciar/Barrows + Protect from Magic + Piety. **Blood Forfeit** ranged attack hits through prayer — keep HP ~40-50; Vengeance counters it.

### Rune dragon
- `hasStrategyPage`: **yes**
- `primaryStyle`: **MELEE** (dragonbane reliable at base-90); **Tumeken's shadow best across all styles**
- `primaryWeapons` (melee, priority): Dragon hunter lance → Osmumten's fang (fang out-damages DHL in full Justiciar/fury/barrows gloves)
- `secondaryWeapons`: Ranged — Dragon hunter crossbow → Hunters' sunlight crossbow → Zaryte/Armadyl/Dragon. Magic — Tumeken's shadow (surpasses dragonbane) → Harmonised nightmare staff / Dragon hunter wand / Eye of ayak (Earth spells).
- `keyGearNote`: **Draconic → dragonbane**; relatively low Magic Defence → Shadow is the single best weapon overall. Bandos/Justiciar/Barrows + Protect from Magic + Piety. Healing **Ranged** attack hits through prayer — Vengeance when a purple hitsplat appears.

---

## Monsters with NO /Strategies page → fall back to the stat/DPS engine

This is the **majority**. Grouped by distinct monster (leveled / location / god / superior variants that share a
profile are folded in). All checked 2026-06-29; HTTP 404 unless noted.

**Demons / devils**
- Abyssal demon, Greater abyssal demon (superior) — no page
- Smoke devil base monster *has* a page (above); **Nuclear smoke devil** (superior) — no own page
- Black demon (all levels 172/178/184), Balfrug Kreeyath, Porazdir — no page
- Greater demon (all levels 92/100/101/113), Tstanon Karlak — no page
- Dust devil → `/Strategies` `#REDIRECT`s to *Slayer task/Dust devils* (generic guide, **not** a combat strategy page) — treat as no; Choke devil (superior) — no page

**Spectres / undead**
- Aberrant spectre, Abhorrent spectre, Deviant spectre, Repugnant spectre — no page
- Ankou (all levels 75/82/86/95/98) — no page
- Feral Vampyre, Vampyre Juvinate — no page (Vyrewatch Sentinel has one, above)

**Basilisks**
- Basilisk, Monstrous basilisk (superior), Basilisk Sentinel — no page (Basilisk Knight has one, above)

**Aquatic / kraken / aviansie kin**
- Elder aquanite (superior) — no page (Aquanite base has one, above)
- Cave kraken — no page
- Waterfiend — no page
- Flight Kilisa, Flockleader Geerin, Wingman Skree — no own page (Aviansie guide covers them, above)

**Araxytes**
- Araxyte (level 96 / 146), Dreadborn Araxyte (superior) — no page

**Dragons (chromatic + babies + brutal blue/red)**
- Black dragon, Baby black dragon — no page (Brutal black dragon has one, above)
- Blue dragon, Baby blue dragon, Brutal blue dragon — task-page strategy context migrated to
  `src/main/data/slayer/strategies/blue-dragons/strategy.json`.
- Red dragon, Baby red dragon, Brutal red dragon — no page

**Bloodvelds**
- Bloodveld, Bloodveld (GWD), Mutated Bloodveld, Insatiable Bloodveld, Insatiable mutated Bloodveld —
  task-page strategy context migrated to `src/main/data/slayer/strategies/bloodveld/strategy.json`.
  Reanimated bloodveld is retained as source context only, not a selectable runtime variant.

**Cave horrors / beasts / drakes / dagannoth**
- Cave horror, Cave abomination (superior) — no page
- Dark beast → `/Strategies` `#REDIRECT`s to *Slayer task/Dark beast* (not a combat strategy page) — treat as no; Night beast (superior) — no page
- Drake → `/Strategies` `#REDIRECT`s to *Slayer task/Drake* — treat as no; Guardian Drake (superior) — no page
- Dagannoth, Dagannoth spawn, Dagannoth fledgeling — no page

**Elves (Prifddinas / Iorwerth)**
- Iorwerth Warrior, Elf Warrior, Mourner, Guard (Prifddinas), Iorwerth Archer, Elf Archer — no page

**Giants / gargoyles / gryphons**
- Fire giant, Fire giant (Catacombs) — no page
- Gargoyle, Marble gargoyle (superior) — no page
- Gryphon — no page

**Hellhounds**
- Hellhound, Hellhound (GWD), Skeleton Hellhound (Tarn's Lair / Vet'ion / Calvar'ion), Greater Skeleton Hellhound (Vet'ion / Calvar'ion) — no page

**Kalphites**
- Kalphite Worker, Kalphite Soldier, Kalphite Guardian — no page

**Kurask / lizardmen**
- Kurask, King kurask (superior) — no page
- Lizardman, Lizardman brute — no page (Lizardman shaman has one, above)

**Nechryael / zygomites**
- Nechryael — no page; Greater Nechryael (superior) → `/Strategies` `#REDIRECT`s to *Slayer task/Nechryael* — treat as no; Nechryarch (superior) — no page
- Zygomite (level 74 / 86), Ancient Zygomite (superior) — no page

**Spiritual creatures (all gods)**
- Spiritual creature; Spiritual ranger / warrior / mage for Zamorak, Saradomin, Bandos, Armadyl, Zaros — no page (base "Spiritual creature/mage/warrior/ranger" all 404)

**Suqah / trolls / TzHaar**
- Suqah → `/Strategies` `#REDIRECT`s to *Slayer task/Suqah* — treat as no
- Mountain troll, Ice troll (+ runt / male / female / grunt), Troll general — no page
- TzHaar-Xil, TzHaar-Hur, TzHaar-Mej, TzHaar-Ket (level 149 / 221) — no page

**Warped creatures / waterfiends / wyrms**
- Warped Terrorbird (level 96 / 138), Warped Tortoise — no page
- Wyrm, Shadow Wyrm (superior), Wyrmling — no page
- Lava Strykewyrm, Magma strykewyrm — no page

> Note on draconic no-page monsters: the dragonbane category is already handled by the stat/DPS engine
> (`TaskData.dragon` / WDBX). The guide pages above only *confirm* dragonbane preference; for the no-page
> draconic monsters (chromatic dragons, Wyrms, Drakes, Skeletal-wyvern kin without their own page) the engine
> already routes dragonbane correctly.

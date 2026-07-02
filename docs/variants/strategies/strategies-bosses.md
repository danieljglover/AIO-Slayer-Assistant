# Boss /Strategies — guide-recommended gear (MV-SR boss slice)

Source of truth: the OSRS wiki `/Strategies` pages (or the main page Strategy section where no subpage exists).
Discipline: extracted from the guide, never invented; `UNKNOWN` where the page is ambiguous. OSRS, not RS3.

**Scope = every variant with `isBoss=true` in `slayer-data.json`** (Boss task pool + boss-flagged variants under
regular tasks). Derived from the JSON: **46 unique boss variants** (58 entries incl. duplicate monsters that
appear under both the "Boss" task and a regular task — e.g. Cerberus, Vorkath, K'ril, Skotizo, Dagannoth Kings,
Kree'arra, KBD, Thermonuclear smoke devil, Abyssal Sire, GG-Dawn). One block per unique monster/form below.

Each block (priority order within `primaryWeapons` / `secondaryWeapons`):
- `hasStrategyPage` yes/no (+ URL) · `primaryWeapons` (main-style BiS / commonly-recommended) · `primaryStyle`
  (MELEE/RANGED/MAGIC the guide centres on) · `secondaryWeapons` (recommended mix/switch + style + why) ·
  `keyGearNote` · `mechanicsNote` (gear-relevant only — we do NOT model prayer/supplies).

> **Headline flags where the GUIDE'S style differs from the plugin's data weakness** (the whole point of this slice):
> Callisto & Chaos Elemental — plugin MAGIC, guide **RANGED**. Crazy archaeologist — plugin MELEE, guide **MAGIC**.
> Kree'arra — plugin "MELEE" (Boss pool) but airborne, guide is **MAGIC (Tumeken's shadow)/chinchompa**.
> Barrows Dharok/Guthan/Torag — guide leans **MAGIC** (air weakness) regardless of the brother's own attack style.
> K'ril Tsutsaroth & Skotizo — demon bosses, guide centres **demonbane melee (Emberlight/Arclight)**.
> Grotesque Guardians Dawn — plugin MELEE but she is melee-immune; guide is **RANGED** (or a halberd).

## Status: COMPLETE — 46 boss variants checked, 44 with a usable /Strategies (or main-page Strategy) page

- **44 / 46 have guide gear** (a /Strategies subpage, or a main-page Strategy/Equipment section — Kraken, Dad,
  Ice Troll King, Arrg use the main page).
- **4 monsters give NO recommended weapons** (only tactics, no equipment block): **Dad, Ice Troll King, Arrg**
  (Troll quest bosses — `primaryWeapons: UNKNOWN`, plugin MELEE confirmed defensible by their defence stats) and
  **Dark Ankou** (a Skotizo minion, not a real boss — no page, style UNKNOWN). All four appended to
  `gaps-strategies.md`.
- **2 prompt-hint corrections discovered (both real, NOT skipped):** **Branda the Fire Queen** = a Royal Titans
  boss (paired with Eldric the Ice King) → has `Royal_Titans/Strategies`. **Shellbane gryphon** = a real
  level-235 Gryphons boss released 19 Nov 2025 with Sailing → has `Shellbane_gryphon/Strategies`.
- **Deferred / un-enumerated bosses (NOT in slayer-data.json → no profile → skipped):** Duke Sucellus, The
  Leviathan, Vardorvis, The Whisperer (DT2), Commander Zilyana (GWD Sara), Zulrah magma (crimson) form.
  Tormented Demon (named in the brief) is also NOT in the data → not processed. See `gaps-strategies.md`.

---

## Barrows brothers
> The wiki recommends one unified setup for the whole minigame: high-level **Magic with air spells** (every
> brother takes 50% extra from wind spells), with a dedicated **Ranged** kit for **Ahrim** (the only ranged-weak
> brother). The plugin's per-brother "centred style" follows each brother's own attack type and so diverges from
> the wiki's kill-style for Dharok/Guthan/Torag (guide = magic). All six share `Barrows/Strategies`.

### Ahrim the Blighted
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Barrows/Strategies
- primaryWeapons: Toxic blowpipe, Rosewood blowpipe / Hunters' sunlight crossbow, Magic shortbow (i), Karil's crossbow, Yew shortbow
- primaryStyle: RANGED
- secondaryWeapons: Tumeken's shadow / Eye of ayak (MAGIC — with air spells or Shadow you can mage Ahrim like the others, skipping the ranged switch); Dragon dagger (MELEE spec to speed the kill)
- keyGearNote: Ahrim is the only brother weak to Ranged, so the guide carves out a dedicated ranged setup (Masori/void/d'hide + blowpipe). Carrying air spells or a Shadow lets you skip ranged gear entirely.
- mechanicsNote: Attacks with Magic (Protect from Magic). Drains Strength over a long fight → guide pushes specs/high DPS to end fast. No shield/phase. (Plugin RANGED — matches.)

### Dharok the Wretched
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Barrows/Strategies
- primaryWeapons: Eye of ayak, Tumeken's shadow > Harmonised nightmare staff, Sanguinesti staff / Trident of the swamp, Staff of the dead / Twinflame staff, Smoke battlestaff (low-level: Iban's staff (u), Staff of air)
- primaryStyle: MAGIC
- secondaryWeapons: NONE (a melee weapon is optional only for tunnel reward-potential, not for the brother)
- keyGearNote: Dharok uses melee → vulnerable to Magic; the shared BiS magic setup (air spells, 50% wind weakness) is the recommended kill. No per-brother switch.
- mechanicsNote: Keep Protect from Melee up always — Wretched Strength raises his max hit as he loses HP (up to 57 at 1 HP). Avoid freezing at low HP. No shield/phase. (Plugin MAGIC — matches.)

### Guthan the Infested
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Barrows/Strategies
- primaryWeapons: Eye of ayak, Tumeken's shadow > Harmonised nightmare staff, Sanguinesti staff / Trident of the swamp, Staff of the dead / Twinflame staff, Smoke battlestaff (low-level: Iban's staff (u), Staff of air)
- primaryStyle: MAGIC
- secondaryWeapons: NONE (melee optional only for tunnel reward-potential)
- keyGearNote: Guthan uses melee → vulnerable to Magic; killed with the shared magic setup (50% wind weakness). No per-brother switch.
- mechanicsNote: Protect from Melee fully negates his damage (his heal-on-hit set effect never lands). No shield/phase. (Plugin MAGIC — matches.)

### Karil the Tainted
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Barrows/Strategies
- primaryWeapons: Eye of ayak, Tumeken's shadow > Harmonised nightmare staff, Sanguinesti staff / Trident of the swamp, Staff of the dead / Twinflame staff (low-level: Iban's staff (u), Staff of air); air spells especially effective
- primaryStyle: MAGIC
- secondaryWeapons: Melee weapon (MELEE — Karil is weak to both melee and magic; a melee/Piety kill is a supported alternative); Dragon dagger spec
- keyGearNote: Wiki centres Karil on Magic (50% air weakness) with the shared magic setup; no dedicated ranged gear is used against him despite his own style being Ranged.
- mechanicsNote: Attacks with Ranged (Protect from Missiles). Tainted Shot drains 20% Agility through prayer (bring Restore — supplies). Fast 4-tick attack. No shield/phase. (Plugin MELEE — wiki leans MAGIC but lists melee Piety as a valid alt, so MELEE is defensible.)

### Torag the Corrupted
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Barrows/Strategies
- primaryWeapons: Eye of ayak, Tumeken's shadow > Harmonised nightmare staff, Sanguinesti staff / Trident of the swamp, Staff of the dead / Twinflame staff, Smoke battlestaff (low-level: Iban's staff (u), Staff of air)
- primaryStyle: MAGIC
- secondaryWeapons: NONE (melee optional only for tunnel reward-potential)
- keyGearNote: Torag uses melee → vulnerable to Magic; shared magic setup (50% wind weakness). No per-brother switch.
- mechanicsNote: Protect from Melee fully negates him; his run-energy drain is gear-irrelevant. No shield/phase. (Plugin MAGIC — matches.)

### Verac the Defiled
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Barrows/Strategies
- primaryWeapons: Eye of ayak, Tumeken's shadow > Harmonised nightmare staff, Sanguinesti staff / Trident of the swamp, Staff of the dead / Twinflame staff (low-level: Iban's staff (u), Staff of air); air spells especially effective
- primaryStyle: MAGIC
- secondaryWeapons: Melee weapon with strong stab-defence armour (MELEE — Verac's Defiler effect hits through Protect from Melee, so a tankier melee kill is viable)
- keyGearNote: Wiki centres Verac on Magic (50% air weakness) with the shared setup; uniquely recommends high-STAB-defence armour because his 25% Defiler effect ignores armour/Defence and penetrates protection prayers.
- mechanicsNote: Protect from Melee still recommended (negates ~75%; on the 25% that penetrate, max hit drops 23→15). Only brother needing food. No shield/phase. (Plugin MELEE — wiki leans MAGIC but stab-tank approach makes MELEE defensible.)

---

## God Wars Dungeon + demon bosses

### General Graardor
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/General_Graardor/Strategies
- primaryWeapons: Osmumten's fang; Inquisitor's mace / Ghrazi rapier / Blade of saeldor; Abyssal tentacle; Zamorakian hasta
- primaryStyle: MELEE
- secondaryWeapons: Voidwaker / Saradomin godsword (with Lightbearer) / Dragon claws / Bandos godsword (MELEE specs); Elder maul or Dragon warhammer (MELEE def-reduction spec); Blood ancient sceptre (MAGIC, Blood Barrage heal off minions post-kill). Page notes ranged (Bow of faerdhinen) / magic (Tumeken's shadow) kiting are actually the strongest solo styles.
- keyGearNote: Guide says melee solo is "generally not recommended" vs magic/ranged; if doing melee, Osmumten's fang is strongly recommended. Tank body for Ranged defence. No demonbane relevance.
- mechanicsNote: 3 bodyguards (Steelwill magic / Grimspike ranged / Strongstack melee) target the killing-blow player after Graardor dies — kill order Steelwill→Grimspike→Strongstack with prayer swaps. Walk-under to skip hits.

### Kree'arra
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Kree'arra/Strategies
- primaryWeapons: Tumeken's shadow (only direct-kill weapon competitive with chins); otherwise Black / Red chinchompa (indirect AoE method)
- primaryStyle: MAGIC (Shadow is the recommended direct method; chinchompas are ranged-thrown AoE)
- secondaryWeapons: Eldritch nightmare staff (MAGIC, prayer-restore spec / on minions); Bow of faerdhinen / Twisted bow / crossbow (RANGED, finish Kree'arra in the chin method); Toxic blowpipe (RANGED, kill Wingman Skree); Blood Barrage runes (MAGIC, heal off minions)
- keyGearNote: Airborne — regular melee does NOT work (only halberds/salamanders, not recommended). NOT a melee target despite the Boss-pool "MELEE" tag; guide methods are Shadow (magic) or chinchompas. Protect from Missiles always.
- mechanicsNote: Bodyguards — Flight Kilisa (melee, main threat), Wingman Skree (magic), Flockleader Geerin (ranged). His magic attack rolls vs Ranged defence. Walk under after each attack.

### K'ril Tsutsaroth
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/K'ril_Tsutsaroth/Strategies
- primaryWeapons: Emberlight; Osmumten's fang; Arclight (Solo Melee tab order)
- primaryStyle: MELEE
- secondaryWeapons: Scorching bow (RANGED — top-billed solo method: special-attack bind avoids all damage, highest ranged DPS); Tumeken's shadow (MAGIC, 5:0 method, highest DPS); Blood ancient sceptre / Kodai wand (MAGIC, Blood Barrage heal off minions); Toxic blowpipe (RANGED spec)
- keyGearNote: **Demon boss — demonbane is central:** Emberlight and Arclight are the top two Solo Melee weapons (Arclight +70% acc/dmg vs demons). Magic-defence gear + Protect from Melee (watch the prayer-smash).
- mechanicsNote: Melee can poison through prayer. Prayer Smash (1/9 vs Protect from Melee) hits 35-49 and halves prayer (Spectral spirit shield quarters drain). 3 demon bodyguards (Balfrug magic / Tstanon melee / Zakl'n ranged) target a random player on death.

### Skotizo
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Skotizo/Strategies
- primaryWeapons: Emberlight; Arclight (Melee tab order)
- primaryStyle: MELEE
- secondaryWeapons: Twisted bow / Scorching bow / Bow of faerdhinen (RANGED — guide says ranged is "the best method for most players"; Twisted bow recommended over melee if you don't own Emberlight); Burning claws / Dragon claws / Voidwaker (MELEE spec); Elder maul / Bandos godsword / Dragon warhammer (MELEE def-reduction); Toxic blowpipe (RANGED, altars)
- keyGearNote: **Demon — demonbane central:** Arclight/Emberlight take only 15% damage cut per active altar (vs 25%) AND one-shot an awakened altar regardless of stats (Silverlight/Darklight do NOT). Carry Arclight/Emberlight even on a ranged setup just to clear altars.
- mechanicsNote: Activates up to 4 Awakened Altars cutting your damage (max 60% with demonbane, 100% otherwise) — run to disable (Protect from Magic while travelling, stamina pots). At 2/3 HP summons 3 reanimated demon spawns + possible Dark Ankou; focus Skotizo + altars.

### Grotesque Guardians - Dawn
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies
- primaryWeapons: Venator bow (with Toxic blowpipe); Bow of faerdhinen; Eclipse atlatl; Hunters' sunlight crossbow; Magic shortbow (i) — "Ranged switches" tab
- primaryStyle: RANGED (Dawn is the airborne twin — immune to non-halberd melee; must be hit with Ranged, Magic, or a halberd)
- secondaryWeapons: Noxious halberd (MELEE — only halberd fast enough to hit Dawn); Toxic blowpipe (RANGED spec / orb-skip finisher); Crystal halberd (MELEE finisher spec)
- keyGearNote: Magic NOT recommended (Dawn resists it more than ranged). Venator bow favoured to ricochet onto both twins when stacked. Must carry rock hammer / granite hammer / rock thrownhammer to finish each twin. (Plugin tags Dawn MELEE — wiki requires ranged/magic/halberd.)
- mechanicsNote: Shared boss with Dusk. Dawn attackable in Phase 1 and Phase 3 (launches energy spheres self-healing 90 HP — absorb or "orb skip" with 90+ damage). Rockfall AoE stuns. Stack Dawn on Dusk for Venator ricochet.

### Grotesque Guardians - Dusk
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies
- primaryWeapons: Scythe of vitur; Granite hammer / Soulreaper axe; Ghrazi rapier / Blade of saeldor / Inquisitor's mace > Noxious halberd / Abyssal tentacle; Osmumten's fang / Abyssal whip / Abyssal bludgeon
- primaryStyle: MELEE
- secondaryWeapons: Dragon claws (MELEE spec); Crystal halberd (MELEE spec, best as last hit before a phase ends, also reaches Dawn); Burning claws / Dragon dagger (p++) / Saradomin godsword (MELEE specs)
- keyGearNote: Dusk is immune to magic and ranged — melee only. -1 flat armour in P2 favours multi-hit (Scythe of vitur); abysmal Defence so most weapons viable. Carry a rock/granite/rock-thrown hammer to finish.
- mechanicsNote: Shared boss with Dawn. Dusk attackable in Phase 2 (avoid blinding attack; never stand under — trample ~40) and Phase 4 (absorbs Dawn, grows 6x6, uses melee + ranged — pray his last-used style). Lightning between phases.

---

## Wilderness bosses
> Wilderness weapons (Ursine/Viggora's chainmace, Webweaver/Craw's bow, Accursed/Thammaron's sceptre) carry a
> +50% bonus vs Wilderness monsters and dominate these guides.

### Callisto
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Callisto/Strategies
- primaryWeapons: Webweaver bow, Craw's bow, Bow of faerdhinen, Zaryte crossbow / Armadyl crossbow / Dragon crossbow, Rune crossbow, Magic shortbow (i)
- primaryStyle: RANGED
- secondaryWeapons: Accursed sceptre > Thammaron's sceptre (MAGIC), Eye of ayak, Sanguinesti staff / Trident of the swamp/seas, Warped sceptre (MAGIC — "fairly vulnerable to magic," but weaker vs PKers); Dragon warhammer / Vulnerability (def-lower)
- keyGearNote: **Plugin MAGIC, guide RANGED** (low ranged defence). Webweaver bow has a powerful Wilderness bonus → fastest (higher-risk) kills; Craw's bow and Accursed/Thammaron's sceptre also Wilderness weapons. 3-4 protected items.
- mechanicsNote: Freeze to hold him (first bind always lands if magic acc ≥0). Protect from Magic vs white-orb knockback (≤50). Bear traps at 66%/33% HP break freezes. Avoid melee (35-55 through prayer).

### Venenatis
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Venenatis/Strategies
- primaryWeapons: Ursine chainmace / Viggora's chainmace, Inquisitor's mace, Soulreaper axe / Abyssal bludgeon, Zombie axe / Zamorakian hasta, Sarachnis cudgel (all crush)
- primaryStyle: MELEE
- secondaryWeapons: Webweaver bow / Craw's bow (RANGED — "very competitive damage... despite higher ranged defences," lets you stay near the exit); Twisted bow, Toxic blowpipe, Zaryte crossbow > Armadyl/Dragon/Rune crossbow (RANGED); Dinh's bulwark (one-shots spiderlings, tanks PKers); Elder maul / Dragon warhammer (def-lower spec)
- keyGearNote: Ursine chainmace is strongest (Wilderness mace, needs revenant ether); Viggora's is the budget Wilderness alt. Weakest to crush. ~4 protected items.
- mechanicsNote: 8 ranged then 8 magic cycle, moves every 4 attacks; spawns spiderlings (kill fast — drain prayer/empower her) and a sticky web AoE (avoid). Switch overheads if ranging. (Plugin MELEE — matches.)

### Vet'ion
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Vet%27ion/Strategies
- primaryWeapons: Ursine chainmace / Viggora's chainmace, Soulreaper axe > Inquisitor's mace, Abyssal bludgeon / Zamorakian hasta / Zombie axe (crush), Sarachnis cudgel / Elder maul / Dual macuahuitl, Leaf-bladed battleaxe / Dragon mace
- primaryStyle: MELEE
- secondaryWeapons: NONE for damage (pure melee fight). Specs: Elder maul, Abyssal bludgeon, Dragon mace. Anti-PKer: Dinh's bulwark, Rune crossbow w/ cheap bolts to fight back while frozen.
- keyGearNote: Ursine chainmace "by far the most effective weapon" (Wilderness mace, needs revenant ether); Viggora's is budget Wilderness alt. Crush defence -10. Salve amulet (e) free/kept, prioritise (does NOT stack with slayer helm). 3-4 high-strength items.
- mechanicsNote: Two phases; at 50% each phase summons skeletal hellhounds granting him damage-immunity until killed (Protect from Melee). AoE lightning (dodge glowing tiles) + shield bash (melee range). Enraged form after P1 (faster). All attacks avoidable.

### Scorpia
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Scorpia/Strategies
- primaryWeapons: Accursed sceptre / Thammaron's sceptre, Eye of ayak, Sanguinesti staff / Trident of the swamp, Trident of the seas, Warped sceptre
- primaryStyle: MAGIC
- secondaryWeapons: NONE (single magic setup; Trident of the swamp preferred to also poison her); freeze spells for control not damage
- keyGearNote: **Plugin MAGIC — matches.** Accursed/Thammaron's sceptre are Wilderness weapons (top picks). Very low magic defence → prioritise magic damage + prayer bonus over accuracy; Trident of the swamp adds poison. 3-4 items.
- mechanicsNote: Protect from Missiles (offspring deal ranged + poison). At ≤99 HP summons two guardians that heal her within 3 tiles — freeze the guardians and lure her away to despawn them, then finish.

### Chaos Elemental
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Chaos_Elemental/Strategies
- primaryWeapons: Webweaver bow / Craw's bow, Twisted bow, Bow of faerdhinen, Toxic blowpipe, Armadyl crossbow
- primaryStyle: RANGED
- secondaryWeapons: Ursine chainmace / Viggora's chainmace, Osmumten's fang, Noxious halberd, Abyssal tentacle / Abyssal whip, Verac's flail (MELEE — flinch/safespot soloing; godsword/Dharok/Verac noted for fast flinch); Voidwaker / Dragon claws (specs)
- keyGearNote: **Plugin MAGIC, guide RANGED** (or melee safespot) — magic is NOT recommended here. Webweaver/Craw's bow + Ursine/Viggora's chainmace are Wilderness weapons. 3-4 items.
- mechanicsNote: Protect from Magic (Discord is random style, most often magic). Madness attack unequips ≤4 items — counter with inventory-blocking food (summer/curry pies) + disable vial smashing. Confusion teleports you. Safespot/flinch avoids all damage.

### Chaos Fanatic
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Chaos_Fanatic/Strategies
- primaryWeapons: Webweaver bow / Craw's bow, Twisted bow > Bow of faerdhinen, Scorching bow, Magic shortbow (i) / Crystal bow
- primaryStyle: RANGED
- secondaryWeapons: NONE (single ranged setup)
- keyGearNote: **Plugin RANGED — matches.** Webweaver/Craw's bow are top picks and Wilderness weapons. High magic level/defence → range him. 3-4 items.
- mechanicsNote: Protect from Magic. Avoid the green special by moving off your tile. Can unequip items — bring inventory-blocking food + disable vial smashing.

### Crazy archaeologist
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Crazy_archaeologist/Strategies
- primaryWeapons: Accursed sceptre / Thammaron's sceptre, Eye of ayak > Sanguinesti staff, Trident of the swamp, Trident of the seas, Warped sceptre / Iban's staff (u)
- primaryStyle: MAGIC
- secondaryWeapons: NONE (single magic setup; melee possible but not recommended). Iban's staff (u) is a budget option (Att 50 req).
- keyGearNote: **Plugin MELEE, guide MAGIC** — weak to magic, negligible magic level (accuracy barely matters → prioritise magic damage + prayer bonus, cheap robes fine). Accursed/Thammaron's sceptre are Wilderness weapons. 3-4 items.
- mechanicsNote: Protect from Missiles. Avoid "Rain of knowledge!" 3x3 AoE by stepping off. Flinchable behind northern obstacles for no-damage kill.

---

## Slayer-pool bosses (Abyssal Sire, Hydra, Cerberus, Kraken, Thermy, Sarachnis, Giant Mole)

### Abyssal Sire
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Abyssal_Sire/Strategies
- primaryWeapons: Emberlight (stab) > Osmumten's fang; Scythe of vitur (with Oathplate, slash); Ghrazi rapier / Arclight
- primaryStyle: MELEE (the guide's optimal kill — phases 2-3; plugin data tags MAGIC)
- secondaryWeapons: Scorching bow + bronze/amethyst arrows (RANGED — recommended Phase-1 tool to one-shot the four respiratory vents without a gear switch); Tumeken's shadow / Sanguinesti / Trident of the swamp (MAGIC — alt boss-damage style); Shadow Barrage cast (MAGIC, mandatory to stun the Sire); Burning/Dragon claws (MELEE spec, Phase 3)
- keyGearNote: **Demonbane-weak (Abyssal demon).** Optimal kill is melee (Emberlight + Avernic + Lightbearer); magic/ranged only wakes/stuns the Sire and pops the vents (demonbane one-shots them, else need 50+ max hit). Plugin MAGIC ≠ guide melee.
- mechanicsNote: P1 = Shadow Barrage stun (100%) then destroy 4 respiratory vents (~27s). P2 = melee Row 1, dodge miasma/forced-teleport blast. P3 = run from the 96-dmg explosion, do NOT kill the 15 spawns. Protect from Missiles vs spawns/scions.

### Alchemical Hydra
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Alchemical_Hydra/Strategies
- primaryWeapons: Twisted bow > Toxic blowpipe / Dragon hunter crossbow (with ruby dragon bolts (e)) > Bow of faerdhinen; Eclipse atlatl
- primaryStyle: RANGED
- secondaryWeapons: Zaryte crossbow + ruby bolts (e) (RANGED spec — skip the flame-wall, esp. with Lightbearer); Dragon warhammer / Tonalztics of ralos (def-reduction specs); melee alt exists (Dragon hunter lance stab > Scythe of vitur) since it's draconic, but ranged is primary
- keyGearNote: Weakest to ranged (+45 ranged resistance, Def 100). Magic NOT recommended (Magic 260). Draconic → dragonbane (DHCB / DH lance) effective. Often prioritise prayer bonus over ranged accuracy (low boss defence).
- mechanicsNote: 4 phases at 25% HP, each a colour with a matching vent to lure to (green→red, blue→green, red→blue) for the 75% damage-reduction drop; grey = enrage (alternates mage/ranged every hit, ≤55). Prayer-flick; Bracelet of slaughter in final phase to extend the task.

### Cerberus
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Cerberus/Strategies
- primaryWeapons: Scythe of vitur (best — exploits the 6-tick walk-under for double hits); Arclight / Emberlight (set to stab, demonbane); Osmumten's fang / Ghrazi rapier / Inquisitor's mace (crush)
- primaryStyle: MELEE
- secondaryWeapons: Twisted bow (RANGED — "extremely effective" despite her ranged resistance; more reaction time on lava pools); Scorching bow (RANGED — similar DPS to non-max melee)
- keyGearNote: Weak to MELEE, primarily CRUSH (slightly resists stab, resists slash/magic/ranged). Demonic → Arclight/Emberlight apply (set to stab). Low Def (100). Spectral spirit shield / Ward of Arceuus cuts the soul-attack prayer drain.
- mechanicsNote: Protect from Magic default. Triple attack on hit #1 and every 10th. Souls (every 7th, <400 HP): three ghosts red=melee/blue=magic/green=ranged W→E — flick in sequence. Lava (every 5th, <200 HP): dodge the targeted pool. Souls+lava on attacks 14/15 is the main killer.

### Kraken
- hasStrategyPage: no (no /Strategies subpage) — main page Equipment section: https://oldschool.runescape.wiki/w/Kraken
- primaryWeapons: Eye of ayak / Tumeken's shadow > Harmonised nightmare staff or Sanguinesti staff > Trident of the swamp / Staff of the dead > Trident of the seas / Twinflame staff > Warped sceptre
- primaryStyle: MAGIC
- secondaryWeapons: Sanguinesti staff (MAGIC) or Blood barrage/blitz for passive healing on long trips; Volatile/Eldritch nightmare staff or Toxic blowpipe as optional spec switches. NONE strictly required.
- keyGearNote: MAGIC — Ranged deals only 1/7 damage and melee can't reach it. Wiki: only wear mage armour if Virtus/Ancestral, otherwise wear best RANGED armour for magic-defence/survivability. Occult + magic-damage gear prioritised.
- mechanicsNote: Fishing explosive on the large whirlpool to start (else disturb all 4 small whirlpools, needs 7+ range weapon). Kraken + 4 tentacles use typeless magical-ranged (protection prayers do nothing); Kraken max 28 but inaccurate. Turn off Auto Retaliate. No phases/shields.

### Thermonuclear smoke devil
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil/Strategies
- primaryWeapons: Tumeken's shadow > Eye of ayak (cannot safespot) > Harmonised nightmare staff (air spells) > Sanguinesti staff > Trident of the swamp; Kodai wand / Nightmare staff w/ air spells; Ice ancient sceptre for the safespot/freeze method
- primaryStyle: MAGIC (plugin tags MELEE/MAGIC; guide's fastest is magic)
- secondaryWeapons: Dragon claws (MELEE spec — POH/Nardah burst method); Blood Barrage (MAGIC, self-heal); melee (Piety + Redemption) is a listed but slower alternative
- keyGearNote: Air spells hit a 20% elemental weakness (since June 2025). MUST wear a slayer helmet / facemask / gas mask or take extra smoke damage + stat drain. Max hit only 8.
- mechanicsNote: Typeless magical-ranged attack (protection prayers useless), very accurate, 2-tick, max 8. Safespot via freeze (player spells reach 9 tiles vs boss's 8 — use longrange; Ice ancient sceptre best). Redemption to heal (>9 HP). No dwarf cannon allowed.

### Sarachnis
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Sarachnis/Strategies
- primaryWeapons: Dual macuahuitl (with blood moon set) / Zamorakian hasta (crush) / Sarachnis cudgel / Zombie axe (crush) / Abyssal bludgeon; Abyssal tentacle / Saradomin sword (crush) / Colossal blade
- primaryStyle: MELEE
- secondaryWeapons: Burning claws or Dragon dagger (MELEE spec). No dedicated ranged/magic switch — fight is melee; magic-defensive armour (Karil's/void/d'hide) is worn for the magic spawn, not for attacking.
- keyGearNote: Weak to CRUSH; high magic AND ranged defence (Def only 150). High strength + fast crush weapon. Prioritise magic-defence armour for the magic spawn. Aranea boots negate the web (else a transformation ring). 40% fire-spell weakness exists but melee crush is still recommended.
- mechanicsNote: Heals on hits (5 melee / 10 ranged). Melee when adjacent, ranged out of range — flick prayers (≤31). Every 4th attack = sticky web bind (6 ticks) then she sprints. Spawns 2 minions at 66%/33% (orange melee + blue magic) — tank melee, kill magic, pray ranged.

### Giant Mole
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Giant_Mole/Strategies
- primaryWeapons: Osmumten's fang (stab) > Noxious halberd / Ghrazi rapier > Zamorakian hasta (stab) / Blade of saeldor > Belle's folly / Zombie axe > Abyssal dagger; cheap burst method: Dharok's greataxe (at 1 HP)
- primaryStyle: MELEE
- secondaryWeapons: Twisted bow (RANGED — one of the most effective setups; low ranged defence, works from a safespot); Tumeken's shadow (MAGIC — splashed magic can't trigger the burrow); Dwarf multicannon; poison weapon strongly recommended (poison doesn't trigger burrowing)
- keyGearNote: Melee uses STAB (lowest stab defence). Max hit 21; Protect from Melee negates all damage. Def 200 but no defensive threat. Dharok's-at-1HP is the standard affordable fast-kill; Twisted bow the high-end alt.
- mechanicsNote: From 50%→5% HP, every player hit has 25% chance to burrow to a new lair (loses aggro). Burrowing can extinguish light (use bullseye lantern). Poison + splashed-magic do NOT cause burrowing. Falador shield 3/4 tracks her. No phases/shields/specials.

---

## Dragon / Zulrah / Kalphite Queen / Dagannoth Kings

### King Black Dragon
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/King_Black_Dragon/Strategies
- primaryWeapons: Dragon hunter lance, Osmumten's fang, Ghrazi rapier
- primaryStyle: MELEE
- secondaryWeapons: Twisted bow + Dragon hunter crossbow (RANGED — full alternative ranged style); Voidwaker / Burning claws (MELEE spec burst); Dragon hunter wand (MAGIC — AFK pet-hunting method)
- keyGearNote: **Draconic → dragonbane Dragon hunter lance is the recommended main weapon** (scales with gear; Osmumten's fang better with weaker gear). Anti-dragon / Dragonfire shield strongly advised. Wilderness — only bring what you can lose.
- mechanicsNote: Dragonfire (≤65) + 3 special breaths (poison / stat-drain / freeze) — antifire matters. Melee in range with Protect from Melee reduces special-breath frequency; 2h weapons (twisted bow) force Protect from Magic.

### Vorkath
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Vorkath/Strategies
- primaryWeapons: Dragon hunter crossbow (with ruby + diamond dragon bolts (e)) — the "most effective ranged weapon"; Toxic blowpipe (alternative ranged)
- primaryStyle: RANGED (plugin tags MELEE in the Boss pool / RANGED elsewhere; guide headline method is ranged)
- secondaryWeapons: Dragon hunter lance (MELEE — the dedicated melee-method weapon); Bandos godsword / Dragon warhammer (MELEE def-reduction spec); Crumble Undead via a magic staff (Slayer's staff / dust battlestaff, MAGIC) to kill the Zombified Spawn; Burning claws / Voidwaker / Zaryte crossbow (burst specs)
- keyGearNote: Weak to stab melee and ranged; dragonbane DH lance/crossbow get passive bonuses that stack with Salve (i)/(ei). Crossbow pairs with Dragonfire ward + Protect from Missiles; blowpipe/melee force Protect from Magic + super antifire.
- mechanicsNote: 6 standard attacks then a special, repeating. Keep antifire + venom protection always. Specials: Rapid Fire (acid pools — Woox Walk), Zombified Spawn (freeze — must Crumble Undead, needs magic atk > -64). Use divine potions (prayer-disabling dragonfire). Immune to recoil/Vengeance.

### Zulrah - serpentine (green) form
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Zulrah/Strategies
- primaryWeapons: Tumeken's shadow / Eye of ayak; Harmonised nightmare staff or Sanguinesti staff / Twinflame staff (fire spells + Tome of fire); Trident of the swamp
- primaryStyle: MAGIC
- secondaryWeapons: Ranged (Twisted bow / Bow of faerdhinen, RANGED) covers the tanzanite phase in the hybrid setup — wiki's "best setup uses both Ranged and Magic to exploit Zulrah's weaknesses"
- keyGearNote: Green (serpentine) form is WEAK TO MAGIC (-45 magic def, +50 ranged def); attacks with Ranged → Protect from Missiles. Zulrah has 50% fire-spell weakness (Tome of fire / Harmonised) especially at lower Magic. Serpentine helm avoids anti-venom.
- mechanicsNote: Multi-form rotation boss (green/crimson/tanzanite); damage capped at 50. Counter each: green=magic, crimson(melee)=magic, tanzanite=ranged. "Jad" phase alternates ranged/magic (prayer-flick). Envenoms.

### Zulrah - tanzanite (blue) form
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Zulrah/Strategies
- primaryWeapons: Twisted bow / Toxic blowpipe; Bow of faerdhinen (crystal armour); Hunters' sunlight crossbow
- primaryStyle: RANGED
- secondaryWeapons: Magic (Tumeken's shadow / powered staves, MAGIC) is the main weapon for the OTHER forms in the hybrid; wiki notes magic "performs much worse than even simple ranged switches" on the tanzanite phase, so ranged is the counter here
- keyGearNote: Tanzanite (blue) form is WEAK TO RANGED (+300 magic def, +0 ranged def); attacks mostly Magic (some Ranged) → Protect from Magic. Use ranged here even in an otherwise magic-led trip.
- mechanicsNote: Same rotation / cap-50 mechanics. Magic near-useless on it (magic-defence heavy); ranged is the explicit counter. Envenoms; snakelings/venom clouds per rotation.

### Kalphite Queen
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Kalphite_Queen/Strategies
- primaryWeapons: Toxic blowpipe (amethyst darts+), Twisted bow, Bow of faerdhinen (crystal), Dragon/Rune crossbow — for the airborne phase 2 (plugin RANGED)
- primaryStyle: RANGED
- secondaryWeapons: Melee for phase 1 (Keris partisan of breaching / Keris partisan — kalphite bonus; Inquisitor's mace, Scythe of vitur, Soulreaper axe); Elder maul / Dragon warhammer / Bandos godsword (MELEE def-drop specs); Tumeken's shadow / Eye of ayak (MAGIC — viable 0-switch for both phases)
- keyGearNote: **Two phases — ground form best meleed, airborne wasp form best ranged.** Keris partisan / of breaching is the kalphite-relevant melee weapon (use crush). Slayer helm (i) on a kalphite task is the single biggest DPS boost.
- mechanicsNote: 510 HP across 2 phases; overheads add defence not immunity → pray Protect from Magic throughout. Lower her Defence with DWH/Elder maul. Antidote++ / serpentine helm for poison.

### Kalphite Queen (airborne form)
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Kalphite_Queen/Strategies
- primaryWeapons: Tumeken's shadow, Eye of ayak (Max Mage 0-switch); magic works on this phase (airborne "Protect from Melee" form only adds defence, not immunity)
- primaryStyle: MAGIC
- secondaryWeapons: Ranged (Toxic blowpipe / Twisted bow / Bow of faerdhinen, RANGED — more common phase-2 weapon, generally out-DPSes magic with switches); Salamander (MAGIC, flinching option)
- keyGearNote: Phase 2 (airborne wasp) has Protect from Melee active (defence only). Shadow lets you 0-switch both phases; otherwise ranged preferred. Occult + ancestral/virtus for the magic setup.
- mechanicsNote: Second phase (20-min timer or reverts, keeping HP). Pray Protect from Magic. Walk under at phase start to deny a free hit. Salamanders flinch with magic (other magic has travel time).

### Kalphite Queen (crawling form)
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Kalphite_Queen/Strategies
- primaryWeapons: Keris partisan of breaching / Keris partisan (kalphite bonus, crush), Inquisitor's mace, Scythe of vitur, Soulreaper axe; flinching: Zombie axe, Verac's flail, Colossal blade
- primaryStyle: MELEE
- secondaryWeapons: Elder maul / Dragon warhammer / Bandos godsword (MELEE def-reduction specs at start); Bone dagger (p++) / Arkan blade (MELEE flinch specs)
- keyGearNote: Phase 1 ground/crawling form best meleed. Keris partisan of breaching is the standout kalphite-specific weapon (crush). Verac's / Zombie axe hit through high defence when flinching. Slayer helm (i) on task = biggest single DPS gain.
- mechanicsNote: First of two phases; overheads only boost defence. Lower her Defence early with DWH/Elder maul. Standing in melee range lowers her DPS. Flinch (10-tick) when low on supplies. Poison protection.

### Dagannoth Prime
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies
- primaryWeapons: Twisted bow, Bow of faerdhinen (crystal), Toxic blowpipe, Crystal bow, Hunters' sunlight crossbow / Rune crossbow
- primaryStyle: RANGED
- secondaryWeapons: NONE for killing Prime (single-style; the ranged weapon in a tribrid combo covers it). Toxic blowpipe as a spec option.
- keyGearNote: Prime is a MAGIC attacker, weak to RANGED → kill with ranged, pray Protect from Magic (hits ≤50, hardest of the three). Ranged-defence/prayer gear for Spinolyps; Telekinetic Grab for its drops.
- mechanicsNote: One of three Kings, each a different style. Prime = 3x3 AoE magic, fight ranged. Reachable from the N/NE wall out of Supreme's/Rex's aggro. Antipoison for Spinolyps unless serpentine helm.

### Dagannoth Rex
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies
- primaryWeapons: Eye of ayak / Sanguinesti staff, Trident of the swamp (> Trident of the seas), Twinflame staff, Toxic staff of the dead, Warped sceptre, Iban's staff
- primaryStyle: MAGIC
- secondaryWeapons: NONE for killing Rex (single-style magic). Ice Barrage (MAGIC, freeze); SGS / Guthan's / Sanguinesti (heal off Rex/Spinolyps to extend trips)
- keyGearNote: Rex is a MELEE attacker, weak to MAGIC → kill with magic (extremely low magic defence, so wear full melee armour for ranged defence and still hit him). Pray Protect from Melee; safespot vs the east wall. Magic "highly effective against Rex."
- mechanicsNote: Rex = melee, fight magic; easiest King, can be bound/lured/safespotted. Sustain with Guthan's/blood spells/SGS off Rex + Spinolyps. Antipoison.

### Dagannoth Supreme
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies
- primaryWeapons: Osmumten's fang / Abyssal tentacle, Scythe of vitur, Inquisitor's mace / Ghrazi rapier / Blade of saeldor / Noxious halberd, Abyssal whip, Zombie axe
- primaryStyle: MELEE
- secondaryWeapons: NONE for killing Supreme (single-style melee). Saradomin godsword (MELEE) doubles as spec + kill weapon to save inventory.
- keyGearNote: Supreme is a RANGED attacker, killed with MELEE → pray Protect from Missiles. Hits harder than Rex, less than Prime; its ranged attack can hit multiple players facing it. Kill after Prime so both aren't attacking at once.
- mechanicsNote: Supreme = ranged, fight melee; near both Prime and Rex, so positioning matters (stay N/W to avoid Rex). High def bonuses (99 Def) let you tank Rex+Supreme while praying Protect from Magic. Antipoison.

> **Dagannoth Kings counter-style confirmation:** Prime (magic attacker → RANGED), Rex (melee attacker → MAGIC),
> Supreme (ranged attacker → MELEE) — matches the plugin's data styles exactly.

---

## Inferno / Fight Cave / Araxxor / troll quest bosses / misc

### TzTok-Jad
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/TzHaar_Fight_Cave/Strategies (the Jad page redirects strategy to the Fight Cave guide)
- primaryWeapons: Toxic blowpipe + Twisted bow (combo), Bow of faerdhinen, Zaryte crossbow / Armadyl crossbow, Hunters' sunlight crossbow / Karil's crossbow / Rune crossbow
- primaryStyle: RANGED (plugin tags MELEE; guide centres entirely on ranged)
- secondaryWeapons: Twinflame staff / Tumeken's shadow (MAGIC — exploits TzHaar 40% water weakness, but slower/pricier); Scythe of vitur / Blade of saeldor (MELEE — only at top gear, only early waves, takes more damage)
- keyGearNote: Ranged avoids Jad's no-warning melee and lets you safespot waves. Twisted bow best on Jad/Ket-Zek specifically (very magic-resistant); blowpipe is the all-round pick.
- mechanicsNote: Protect from Melee at melee range, flick to Missiles/Magic on his attack animations; at distance he only uses ranged/magic. Tag the 4 healers (Yt-HurKot).

### TzKal-Zuk
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Inferno/Strategies (Zuk page redirects strategy to the Inferno guide)
- primaryWeapons: Toxic blowpipe + Twisted bow (main combo); fallbacks Bow of faerdhinen, Armadyl crossbow, Dragon crossbow
- primaryStyle: RANGED (plugin tags MELEE; the Inferno is a ranged encounter)
- secondaryWeapons: Kodai wand / Nightmare staff / Tumeken's shadow + Eye of ayak (MAGIC — for magers/Jads/Zuk and Blood Barrage healing); blowpipe for rangers/healers
- keyGearNote: Twisted bow is "the most influential gear investment"; bowfa the affordable next-best. Crystal armour preferred over Armadyl/d'hide for ranged defence + prayer bonus.
- mechanicsNote: Tbow on the set mager, Jad, and Zuk; blowpipe on rangers/healers. Tag Zuk's Jal-MejJak healers fast. Manage safespot range — Armadyl/Zaryte xbow (range 8) and crossbows (7) often need longrange; tbow/bowfa (range 10) stay rapid.

### Araxxor
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Araxxor/Strategies
- primaryWeapons: Scythe of vitur (slash with oathplate, else crush), Inquisitor's mace / Soulreaper axe, Abyssal bludgeon > Ursine chainmace (u) > Zamorakian hasta, Sarachnis cudgel / Dual macuahuitl / Zombie axe
- primaryStyle: MELEE
- secondaryWeapons: Noxious halberd (MELEE — safely 1-hit araxyte spawns / hit the Mirrorback from 1 tile); Hunters' sunlight crossbow / Karil's crossbow / Heavy ballista (RANGED — alt for hatched araxytes); Elder maul or Dragon warhammer (def-drain) + Dragon/Burning claws / Voidwaker / Bandos godsword (specs)
- keyGearNote: True melee boss — weak to crush (+15 crush def, lowest). Plugin MELEE correct. Bring a noxious halberd alongside the main crush weapon for egg spawns.
- mechanicsNote: He uses whichever of your defensive rolls is lower (melee up close, magic/ranged at distance). Elder maul spec caps the defence drain (one suffices). Extended anti-venom+ for venom/acid pools. Enrage <255 HP: faster cleaves, step-under to make him self-damage.

### Dad
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Dad (Strategy on the base page; no /Strategies subpage)
- primaryWeapons: UNKNOWN (no Recommended-equipment block — guide gives only tactics)
- primaryStyle: MELEE
- secondaryWeapons: Earth-spell MAGIC (low-level earth spells for safespotting from the arena gate — guide says far more effective than low-level ranged there); NONE specifically named
- keyGearNote: Troll Stronghold quest boss (later NMZ). Plugin MELEE reasonable; Protect from Melee blocks all his damage. 40% earth-spell weakness noted but no specific weapons recommended. → gaps.
- mechanicsNote: Hits into the 20s with an occasional rapid double-hit (≤~54); 1/3 chance of a knockback+stun swing — stand by a wall to negate. Spare him at the end (don't kill after surrender).

### Ice Troll King
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Ice_Troll_King (Strategy on the base page; no /Strategies subpage)
- primaryWeapons: UNKNOWN (no Recommended-equipment block — guide says MELEE is best but names no weapon)
- primaryStyle: MELEE
- secondaryWeapons: Best Ranged or Magic gear + Neitiznot shield + rings of recoil (RANGED/MAGIC — slow near-AFK recoil cheese at distance where he only uses his weak ranged attack); alternative method, not the main
- keyGearNote: Plugin MELEE correct — his magic/ranged defence is ~2000, melee defence only 45, so melee is the style. Wear melee armour, pray Protect from Magic. 35% fire weakness noted but high mage def makes magic impractical. → gaps (no named weapon).
- mechanicsNote: Melee + a freezing magic attack + ranged rock throws + a knockback special. Do NOT use Protect from Melee (he slams you into the wall). Neitiznot shield cuts his ranged max 7→2.

### Arrg
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Arrg (Strategy redirects to Troll Romance#Fighting Arrg; no Recommended-equipment block)
- primaryWeapons: UNKNOWN (no weapon block; lowest defence is crush/stab 35 vs slash 60; 50% earth-spell weakness)
- primaryStyle: MELEE
- secondaryWeapons: NONE specifically named — guide focuses on prayer switching/safespotting
- keyGearNote: Troll Romance quest boss (later NMZ). Plugin MELEE plausible but the wiki names no recommended weapons. Uses both melee (slash) and ranged. → gaps.
- mechanicsNote: Safespot by trapping him behind the western mountain wall (like Dad). Otherwise flick Protect from Melee / Missiles as he switches — tricky (4-tick, prayer penetration, no warning).

### Dark Ankou
- hasStrategyPage: no — https://oldschool.runescape.wiki/w/Dark_Ankou (page exists but only a "Prayer info" section; no Strategy and no equipment block)
- primaryWeapons: UNKNOWN
- primaryStyle: UNKNOWN (a Crush-attacker minion; wiki gives no recommended player gear)
- secondaryWeapons: NONE
- keyGearNote: Not a standalone boss — a minion randomly summoned during the Skotizo fight (max hit 8, 60 HP). Plugin style correctly UNKNOWN. Wiki says no real benefit to fighting them; focus Skotizo. → gaps.
- mechanicsNote: One in the room at a time; killing one may make Skotizo summon another. 100% poison/venom immune. No gear recommendation exists.

### Branda the Fire Queen
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Royal_Titans/Strategies (Branda redirects to the Royal Titans guide; paired with Eldric the Ice King)
- primaryWeapons: Scythe of vitur, Inquisitor's mace / Soulreaper axe, Blade of saeldor / Ghrazi rapier, Noxious halberd / Dual macuahuitl / Osmumten's fang, Abyssal tentacle (low-level: Dual macuahuitl, Abyssal tentacle/whip / Saradomin's blessed sword, Zombie axe / Sarachnis cudgel, Dragon scimitar)
- primaryStyle: MELEE (primary) with a mandatory RANGED switch
- secondaryWeapons: RANGED — Toxic blowpipe / Eclipse atlatl, Dragon/Rune crossbow w/ ruby bolts (e), Hunters' sunlight crossbow, Karil's crossbow / Magic shortbow (i) (used in her retreat phase when out of melee — hidden 6x ranged accuracy buff then); MAGIC — Twinflame staff (highly recommended) / Purging staff / Kodai wand / Ancient sceptre (kill fire/ice elementals — water/fire spells hit 3x3)
- keyGearNote: 3-style boss designed to teach gear switching: prioritise melee strength, but you MUST bring ranged for her retreat phases and ideally magic for elementals. Weak to water (50%) and slightly to crush.
- mechanicsNote: 600 HP, +700 ranged def normally but +500% (6x) hidden ranged accuracy whenever she's out of melee distance / during the Elemental Blast charge. Twinflame staff auto-swaps fire/water for elementals. Water/ice spells douse her fire walls.

### Shellbane gryphon
- hasStrategyPage: yes — https://oldschool.runescape.wiki/w/Shellbane_gryphon/Strategies (REAL — released 19 Nov 2025 with Sailing; level-235 Gryphons-task boss after the Troubled Tortugans quest)
- primaryWeapons: Scythe of vitur > Soulreaper axe, Ghrazi rapier > Noxious halberd / Osmumten's fang / Blade of saeldor, Abyssal tentacle > Abyssal whip / Abyssal dagger, Zombie axe / Belle's folly, Arkan blade / Colossal blade > Dragon scimitar
- primaryStyle: MELEE
- secondaryWeapons: MAGIC theoretically effective (50% wind/air-spell weakness) but the guide says it's impractical due to the corrosive-spit special → effectively NONE recommended
- keyGearNote: Primarily weak to melee (crush def +40 highest, stab +10 lowest, slash +20 → scythe/zombie axe set to slash). A **tortugan shield is REQUIRED** — without it you take heavy damage even through Protect from Melee. With tortugan shield + Protect from Melee, melee damage is fully negated.
- mechanicsNote: Need equipped weight ≥40kg to counter the Knockback special. Corrosive spit (move 1 tile or unequip to avoid equipment corrosion); Whirlwind (move 1 tile per attack so they don't stack into a 50-70+ hit).

---

## Deferred / un-enumerated bosses (no profile in slayer-data.json → skipped)
These are NOT in the dataset (the backend deliberately deferred them, none fabricated), so there is no variant
block to attach a strategy to. Listed for completeness:
- **Duke Sucellus**, **The Leviathan**, **Vardorvis**, **The Whisperer** — DT2 bosses, deferred.
- **Commander Zilyana** — GWD Saradomin boss, deferred (weakness was UNKNOWN at MV-R2).
- **Zulrah magma (crimson) form** — the third Zulrah form; green + tanzanite are in the data, magma deferred.
- **Tormented Demon** — named in the MV-SR brief but NOT present as a boss variant in the data; not processed.

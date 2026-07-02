# OSRS Weapon Reference — DPS Engine Dataset

> ## ⤷ The EXHAUSTIVE weapon enumeration lives in two companion files (WR2)
> This file (WR1) is the **curated analysis** — a speed summary plus the special-mechanics and
> special-attack distillation the engine needs. For the **complete, every-weapon enumeration** with
> full offensive stats + attack speed + special-attack notes, see:
> - **[weapon-reference-1h.md](weapon-reference-1h.md)** — all **408** one-handed weapons (247 melee / 65 ranged / 96 magic). Commit `f01e2e4`.
> - **[weapon-reference-2h.md](weapon-reference-2h.md)** — all **154** two-handed weapons (+ a supplement for the staves/chinchompas OSRS slot-codes as 1h). Commit `6e43dd1`.
>
> Both were produced by deterministic HTML parsing of the [One-handed](https://oldschool.runescape.wiki/w/One-handed_slot_table)
> and [Two-handed](https://oldschool.runescape.wiki/w/Two-handed_slot_table) slot tables, count-verified
> against the source (408/408 and 154/154), **562 weapons total**. They supersede the curated §1–§3
> speed tables below for completeness. (WR1 originally under-covered low-tier + missed newer weapons
> e.g. Scorching bow — now in [weapon-reference-2h.md](weapon-reference-2h.md).)
>
> **Purpose.** An authoritative, wiki-cross-checked weapon dataset to inform the plugin's
> weapon-DPS selection engine ([weapon-dps-design.md](weapon-dps-design.md), ADR-0008) and its
> special-effects modelling. The plugin reads **base stats live from the game** at runtime, so the
> highest-value content here is (1) the **attack-speed table** the current selector ignores, (2) the
> **passive special-mechanic** formulas (special-effects-table candidates), and (3) the **special-attack
> (spec)** list — flagged separately because the engine recommends a single *continuous* loadout and
> excludes spec-based ranking.
>
> **Status:** research reference, compiled 2026-06-29 from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki).
> Every attack speed below was taken from an **individual weapon page**, not the slot-table summaries
> (which were found to mis-state several speeds — see [Caveats](#9-caveats-discrepancies--what-was-deprioritised)).

## Conventions

- **Tick = 0.6 s.** `seconds = ticks × 0.6` → 4t = 2.4 s, 5t = 3.0 s, 6t = 3.6 s, 7t = 4.2 s.
  Source: [Game tick](https://oldschool.runescape.wiki/w/Game_tick), [Attack speed](https://oldschool.runescape.wiki/w/Attack_speed).
- **Ranged speeds** are quoted on the **Rapid** combat style (the DPS style). For almost every ranged
  weapon **Accurate / Longrange are +1 tick slower** than Rapid; exceptions are noted inline.
- **Magic speeds:** *powered* staves have their own fixed speed (almost always 4t, some 3t/5t).
  *Standard* (spellbook-casting) staves take the **cast spell's** speed — standard combat spells &
  Ancient Magicks autocast at **5t**.
- **Slayer-relevance flag:** ✅ staple on Slayer tasks · ⚠️ niche/situational · ❌ not used for Slayer
  (PvP / utility / outclassed low-tier).
- **Offence columns** list only the DPS-relevant bonuses (the chosen attack bonus + the strength /
  ranged-strength / magic-damage that drives the max hit). Defensive stats are out of scope.

---

## 1. Melee weapons

Source pages cited in §8. Speeds verified per-page (e.g. the 1h slot-table summary wrongly lists
Osmumten's fang as 4t; its weapon page confirms **5t**).

| Weapon | Hands | Attack type | Speed (t / s) | Offence (att / str) | Passive mechanic | Spec | Slayer |
|---|---|---|---|---|---|---|---|
| Scythe of vitur | 2h | slash (crush opt) | **5 / 3.0** | slash +125 / str +75 | 1×3 arc; multi-hit 100/50/25% vs ≥2-tile | — | ✅ |
| Soulreaper axe | 2h | slash / crush | **5 / 3.0** | slash +134 / str +121 | Soul stacks +6%/stack str, max +30% (8 self-dmg/atk) | Behead | ✅ |
| Osmumten's fang | 1h | stab / slash | **5 / 3.0** | stab +105 / str +103 | Accuracy rolled **twice**; dmg band 15–85% of max | Eviscerate (25%) | ✅ |
| Ghrazi rapier | 1h | stab / slash | **4 / 2.4** | stab +94 / str +89 | none (pure stats) | — | ✅ |
| Blade of saeldor (c) | 1h | slash / stab | **4 / 2.4** | slash +94 / str +89 | none (crystal) | — | ✅ |
| Inquisitor's mace | 1h | crush (stab opt) | **4 / 2.4** | crush +95 / str +89 | +2.5% acc&dmg per Inquisitor's piece (≈+7.5% set), crush only | — | ✅ (crush) |
| Dragon hunter lance | 1h | stab/slash/crush | **4 / 2.4** | stab +85 / str +70 | **+20% acc & +20% dmg vs draconic** | — | ✅ (dragons) |
| Abyssal tentacle | 1h | slash | **4 / 2.4** | slash +90 / str +86 | degrades (whip-charged) | Binding Tentacle (50%) | ✅ |
| Abyssal whip | 1h | slash | **4 / 2.4** | slash +82 / str +82 | none | Energy Drain (50%) | ✅ |
| Abyssal bludgeon | 2h | crush | **4 / 2.4** | crush +102 / str +85 | none | Penance (50%) | ✅ (crush) |
| Abyssal dagger | 1h | stab | **4 / 2.4** | stab +75 / str +75 | none | Abyssal Puncture (25%) | ⚠️ |
| Zamorakian hasta | 1h | stab/slash/crush | **4 / 2.4** | stab +85 / str +75 | 1h (frees shield) | Shove (25%) | ⚠️ |
| Zamorakian spear | 2h | stab/slash/crush | **4 / 2.4** | stab +85 / str +75 | — | Shove (25%) | ⚠️ |
| Saradomin sword | 2h | slash / crush | **4 / 2.4** | slash +82 / str +82 | +1–16 extra magic dmg per hit | Sara's Lightning (100%) | ⚠️ |
| Voidwaker | 1h | slash/stab/crush | **4 / 2.4** | slash +80 / str +80 | none | Disrupt (50%) | ⚠️ |
| Keris partisan | 1h | stab / crush | **4 / 2.4** | stab +58 / str +45 | +33% dmg & 1/51 triple-dmg vs Kalphites/scarabs | (variant-specific) | ⚠️ (kalphites) |
| Arclight | 1h | slash / stab | **4 / 2.4** | slash +38 / str +8 | **+70% acc & +70% dmg vs demons** (49% vs Duke) | Weaken (50%) | ✅ (demons) |
| Emberlight | 1h | slash/stab/crush | **4 / 2.4** | stab +63 / str +13 | +70% acc & +70% dmg vs demons (Arclight upgrade) | Weaken (50%) | ✅ (demons) |
| Sarachnis cudgel | 1h | crush / stab | **4 / 2.4** | crush +70 / str +70 | none | — | ⚠️ (budget crush) |
| Viggora's chainmace | 1h | crush / stab | **4 / 2.4** | crush +67 / str +66 | +50% acc & dmg vs Wilderness NPCs (ether) | — | ✅ (Wildy) |
| Dragon scimitar | 1h | slash / stab | **4 / 2.4** | slash +67 / str +66 | none | Sever (55%) | ✅ (budget) |
| Dragon dagger | 1h | stab / slash | **4 / 2.4** | stab +40 / str +40 | none | Puncture (25%) | ⚠️ (DDS) |
| Dragon claws | 2h | slash / stab | **4 / 2.4** | slash +57 / str +56 | none | Slice & Dice (50%) | ⚠️ (burst) |
| Granite hammer | 1h | crush | **4 / 2.4** | crush +57 / str +56 | none | Hammer Blow (60%) | ⚠️ |
| Leaf-bladed sword | 1h | stab / slash | **4 / 2.4** | stab +50 / str +49 | can damage Turoth/Kurask | — | ⚠️ |
| Toktz-xil-ak (obby) | 1h | stab / slash | **4 / 2.4** | slash +52 / str +51 | +20% dmg w/ berserker necklace + obby set | — | ⚠️ |
| Brine sabre | 1h | slash / stab | **4 / 2.4** | slash +47 / str +46 | none | — | ❌ budget |
| Rune scimitar | 1h | slash / stab | **4 / 2.4** | slash +45 / str +44 | none | — | ✅ (early) |
| Verac's flail | 2h | crush / stab | **5 / 3.0** | crush +82 / str +72 | set: ~1/4 hits ignore Def + prayers | — | ⚠️ |
| Leaf-bladed battleaxe | 1h | crush / slash | **5 / 3.0** | crush +75 / str +67 | can damage Turoth/Kurask | — | ✅ (Turoth/Kurask) |
| Tzhaar-ket-em (obby mace) | 1h | crush | **5 / 3.0** | crush +56 / str +57 | +20% dmg w/ berserker neck + obby | — | ❌ |
| Dragon longsword | 1h | slash / stab | **5 / 3.0** | slash +69 / str +71 | none | Cleave (25%) | ❌ budget |
| Dragon mace | 1h | crush | **5 / 3.0** | crush +67 / str +66 | none | Shatter (25%) | ❌ budget |
| Dragon sword | 1h | stab / slash | **5 / 3.0** | stab +62 / str +60 | none | Wild Stab (40%) | ❌ budget |
| Elder maul | 2h | crush | **6 / 3.6** | crush +135 / str +147 | highest crush str | Pulverize (50%) | ✅ (crush/opener) |
| Dragon warhammer | 2h | crush | **6 / 3.6** | crush +95 / str +85 | none | Smash (50%) | ⚠️ (def-drain) |
| Colossal blade | 2h | slash | **6 / 3.6** | slash +98 / str +100 | +2 max/tile of target size, cap +10 (5×5) | — | ✅ (large, budget) |
| Armadyl godsword | 2h | slash / crush | **6 / 3.6** | slash +132 / str +132 | none | The Judgement (50%) | ⚠️ (burst) |
| Bandos godsword | 2h | slash / crush | **6 / 3.6** | slash +132 / str +132 | none | Warstrike (50%) | ⚠️ (def-drain) |
| Saradomin godsword | 2h | slash / crush | **6 / 3.6** | slash +132 / str +132 | none | Healing Blade (50%) | ⚠️ |
| Zamorak godsword | 2h | slash / crush | **6 / 3.6** | slash +132 / str +132 | none | Ice Cleave (50%) | ⚠️ |
| Ancient godsword | 2h | slash / crush | **6 / 3.6** | slash +132 / str +132 | none | Blood Sacrifice (50%) | ⚠️ |
| Crystal halberd | 2h | slash / stab | **7 / 4.2** | slash +110 / str +118 | 2-tile reach | Sweep (30%) | ✅ (big/multi) |
| Dharok's greataxe | 2h | slash / crush | **7 / 4.2** | slash +103 / str +105 | set Wrath: dmg ↑ as HP ↓ (≈+98.9% @ 1 HP) | — | ⚠️ (low-HP strat) |
| Granite maul | 2h | crush | **7 / 4.2** | crush +81 / str +79 | none | Quick Smash (60%) | ❌ (PvP) |
| Tzhaar-ket-om (obby maul) | 2h | crush | **7 / 4.2** | crush +80 / str +85 | +20% dmg w/ berserker neck + obby | — | ⚠️ (TzHaar) |
| Rune 2h sword | 2h | slash | **7 / 4.2** | slash +69 / str +70 | none | — | ❌ |

> **Not in OSRS:** *Noxious halberd* (RS3 only) — excluded. *Ahrim's staff* is magic (§3).

---

## 2. Ranged weapons

Rapid speeds shown; **Accurate/Longrange = +1 tick** unless noted. Bow ranged strength comes mostly
from **ammo** (the bow's own rstr is usually 0 — listed where it is built-in).

| Weapon | Hands | Ammo | Speed Rapid (t / s) · variance | Offence (rng att / rstr) | Passive mechanic | Spec | Slayer |
|---|---|---|---|---|---|---|---|
| Twisted bow | 2h | arrows | **5 / 3.0** · Acc/Long 6t | +70 / ammo | Scales acc & dmg vs target Magic (§2 formulas) | — | ✅ (high-magic/HP) |
| Bow of faerdhinen (c) | 2h | none (self) | **4 / 2.4** · 5t | +128 / +106 | crystal-armour set effect | — | ✅ (best no-ammo) |
| Crystal bow | 2h | none (self) | **4 / 2.4** · 5t | +100 / +78 | crystal-armour set effect | — | ⚠️ |
| Toxic blowpipe | 2h | darts | **2 / 1.2** (PvM) · 3t else/PvP | +30 / +20 (+dart) | +20 rstr built-in; 25% venom; scales as ammo | Toxic Siphon (50%) | ✅ (top DPS, low-def) |
| Zaryte crossbow | 1h+shield | bolts ≤ dragon | **5 / 3.0** · 6t | +110 / ammo | +10% enchanted-bolt effects | Evoke (75%) | ✅ |
| Dragon hunter crossbow | 1h+shield | bolts ≤ dragon | **5 / 3.0** · 6t | +95 / ammo | **+30% rng acc & +25% dmg vs draconic** | — | ✅ (dragons) |
| Armadyl crossbow | 1h+shield | bolts ≤ dragon | **5 / 3.0** · 6t | +100 / ammo | — | Armadyl Eye (50%) | ✅ |
| Dragon crossbow | 1h+shield | bolts ≤ dragon | **5 / 3.0** · 6t | +94 / ammo | — | Annihilate (60%) | ⚠️ |
| Rune crossbow | 1h+shield | bolts ≤ runite | **5 / 3.0** · 6t | +90 / ammo | — | — | ✅ (budget) |
| Karil's crossbow | 2h | bolt racks | **3 / 1.8** · 4t | +84 / ammo | set: drain Agility; +amulet of the damned → 25% 2nd hit (½) | — | ⚠️ (utility) |
| Dorgeshuun crossbow | 1h+shield | bone bolts | **4 / 2.4** · 5t | +42 / ammo | — | Snipe (75%, def-ignoring opener) | ⚠️ (opener) |
| Hunters' Sunlight crossbow | 1h+shield | sunlight/moonlight bolts | **3 / 1.8** · 4t | +79 / ammo | rolls vs target **heavy** rng defence | — | ⚠️ |
| Dark bow | 2h | arrows ×2 | **8 / 4.8** · 9t | +95 / +60 (drag arrows) | fires 2 arrows/attack | Descent of Darkness/Dragons (55%) | ⚠️ |
| Magic shortbow (i) | 2h | arrows ≤ amethyst | **3 / 1.8** · 4t | +75 / ammo | — | Snapshot (50%) | ⚠️ |
| Magic shortbow | 2h | arrows ≤ amethyst | **3 / 1.8** · 4t | +69 / ammo | — | Snapshot (55%) | ⚠️ |
| Heavy ballista | 2h | javelins | **6 / 3.6** · 7t | +125 / +15 (+jav) | — | Concentrated Shot (65%) | ⚠️ |
| Light ballista | 2h | javelins | **6 / 3.6** · 7t | +110 / +jav | — | Concentrated Shot (65%) | ⚠️ |
| Venator bow | 2h | arrows | **4 / 2.4** · 5t | +90 / +25 | multicombat pierce: ≤3 targets, 2nd/3rd ⌊⅔·max⌋ (ancient essence) | — | ⚠️ (grouped) |
| Tonalztics of ralos (c) | 1h+shield | none (self) | **6 / 3.6** · 7t | +115 / +55 | charged = **2 hits**, each 0–75% max; vs **light** rng def | Division (50%) | ⚠️ |
| Eclipse atlatl | 2h | atlatl darts | **3 / 1.8** · 4t | +87 / — | dmg uses **melee Str bonus + Str level**; acc uses ranged | Eclipse (50%) | ⚠️ |
| Craw's bow | 2h | none (ether) | **3 / 1.8** · 4t | +75 / +60 | +50% acc & dmg vs Wilderness NPCs | — | ✅ (Wildy) |
| Webweaver bow | 2h | none (ether) | **3 / 1.8** · 4t | +85 / +65 | +50% acc & dmg vs Wilderness NPCs | Swarm (50%) | ✅ (Wildy) |
| Black chinchompa | 2h-occupy | self (consumed) | **3 / 1.8** (med fuse) · 4t | +80 / +30 | 3×3 AoE | — | ✅ (stacked NPCs) |
| Red chinchompa | 2h-occupy | self (consumed) | **3 / 1.8** (med fuse) · 4t | +70 / +15 | 3×3 AoE | — | ✅ (stacked NPCs) |
| Grey chinchompa | 2h-occupy | self (consumed) | **3 / 1.8** (med fuse) · 4t | +0 / +0 | 3×3 AoE | — | ⚠️ |
| Dragon thrownaxe | 1h | self (thrown) | **4 / 2.4** · 5t | +36 / +47 | — | Momentum Throw (~25%, *verify*) | ❌ |
| Dragon dart | 1h | self (thrown) | **2 / 1.2** · 3t | +0 / +35 | — | **none** (darts have no spec) | ⚠️ (blowpipe ammo) |
| Throwing knives / darts (metal) | 1h | self (thrown) | **2 / 1.2** · 3t | varies | — | — | ❌ low-tier |

> Chinchompa speed note: **medium fuse = 3t**, short/long fuse = 4t (counter-intuitive but confirmed).

---

## 3. Magic weapons

Two families: **powered staves** (built-in spell, fixed scaling max hit, own speed, no runes) and
**standard casters** (cast spellbook spells — speed = the spell, standard combat/Ancients = 5t).

| Weapon | Hands | Type | Speed (t / s) | Built-in max hit & scaling | Offence (mag att / mdmg%) | Passive mechanic | Spec |
|---|---|---|---|---|---|---|---|
| Tumeken's shadow | 2h | powered | **5 / 3.0** | ⌊Magic/3⌋+1 → 34 @99 | +35 / — | **×3 worn mag atk & mdmg** (×4 ToA), 100% dmg cap | — |
| Sanguinesti staff | 1h | powered | **4 / 2.4** | ⌊Magic/3⌋−1 (min 5) → 32 @99 | +25 / +0% | 1/6 chance heal ½ dmg | — |
| Trident of the swamp | 1h | powered | **4 / 2.4** | ⌊Magic/3⌋−2 → 31 @99 | +25 / +0% | 25% venom (100% w/ serp helm); Zulrah scales | — |
| Trident of the seas | 1h | powered | **4 / 2.4** | ⌊Magic/3⌋−5 → 28 @99 | +15 / +0% | poison chance (*verify infobox*) | — |
| Eye of ayak | 1h | powered | **3 / 1.8** | ⌊Magic/3⌋−6 → 27 @99 | +30 / +0% | fastest powered staff; +2 Prayer | Soul Rend (50%) |
| Warped sceptre | 1h | powered | **4 / 2.4** | ⌊(8·Magic+96)/37⌋ → 24 @99 | +12 / +0% | stores earth+chaos runes | — |
| Accursed sceptre (a) | 1h | powered | **4 / 2.4** | ⌊Magic/3⌋−6 → 27 @99 | +22 / +0% | revenant ether; (a) autocasts | Condemn (50%) |
| Thammaron's sceptre (a) | 1h | powered | **4 / 2.4** | ⌊Magic/3⌋−8 → 25 @99 | +15 / +0% | +50% mag acc&dmg vs Wildy NPCs | — |
| Bone staff | 1h | powered | **4 / 2.4** | ⌊Magic/3⌋+5 → 38 @99 | +14 / +0% | +10 max hit vs rats | — |
| Crystal staff (perfected) | 1h | powered | **4 / 2.4** | **fixed 39** | +184 / +0% | fixed max hit (no scaling) | — |
| Crystal staff (attuned) | 1h | powered | **4 / 2.4** | **fixed 31** | +128 / +0% | fixed max hit | — |
| Crystal staff (basic) | 1h | powered | **4 / 2.4** | **fixed 23** | +84 / +0% | fixed max hit | — |
| Kodai wand | 1h | standard caster | 5 / 3.0 (spell) | — (casts spellbook) | +28 / +15% | 15% rune-save + unlimited water runes | — |
| Master wand | 1h | standard caster | 5 / 3.0 | — | +20 / +0% | — | — |
| Harmonised nightmare staff | 1h | standard caster | **4t autocast std** / 5t else | — | +16 / +15% | standard-spellbook autocasts at 4t | — |
| Volatile nightmare staff | 1h | standard caster | 5 / 3.0 | — | +16 / +15% | — | Immolate (55%) |
| Eldritch nightmare staff | 1h | standard caster | 5 / 3.0 | — | +16 / +15% | — | Prayer-restore (55%) |
| Nightmare staff (base) | 1h | standard caster | 5 / 3.0 | — | +16 / +15% | 1h (offhand allowed) | — |
| Ancient sceptre | 1h | standard caster (Ancients) | 5 / 3.0 | — | +20 / +5% | +10% Ancient secondary effects | — |
| Dragon hunter wand | 1h | standard caster | 5 / 3.0 | — | +16 / +10% | **+75% mag acc & +40% dmg vs draconic** | — |
| Slayer's staff (e) | 1h | spell-locked (Magic Dart) | 5 / 3.0 | 13 + ⌊Magic/6⌋ | +12 / +0% | enhanced Magic Dart, Slayer only | — |
| Slayer's staff | 1h | spell-locked (Magic Dart) | 5 / 3.0 | 10 + ⌊Magic/10⌋ | +12 / +0% | Magic Dart | — |
| Iban's staff | 1h | spell-locked (Iban Blast) | 5 / 3.0 | fixed 25 | +10 / +0% | charged | — |
| Elemental/battle/mystic staves | 2h | standard casters | spell (5t std) | — | varies / +0% | unlimited elemental runes (their element) | — |
| Dawnbringer | 1h | powered | 4 / 2.4 | (ToB-only, crumbles) | high / — | Verzik only — **not a Slayer weapon** | Pulsate (35%) |

> **Not in OSRS:** *Wand of the warlock* (no such item — likely confusion with Dragon hunter wand /
> RS3). Nightmare staves (incl. Volatile) are **one-handed**.

---

## 4. Special mechanics (passive) — the special-effects-table candidates

These change **sustained** (no-spec) DPS and are the candidates for the engine's special-effects
modelling. Grouped by how they fit the engine's two data homes
([weapon-dps-design.md §3.4](weapon-dps-design.md): `WeaponEffect` vs `ConditionalBonusRegistry`).

### 4a. Formula-shape mechanics → `WeaponEffect{accuracyRolls, damageMultiplier}`

| Weapon | Style | Exact effect / formula | Condition | Fits v1 model? |
|---|---|---|---|---|
| **Osmumten's fang** | Melee (5t, stab) | Accuracy rolled **twice** → effective hit chance `1−(1−p)²`. Damage always **15–85% of max** (mean = 0.5·max, **unchanged** — variance only). *(Inside ToA the target's defence is also re-rolled; not relevant to Slayer.)* | always (stab) | ✅ `accuracyRolls=2, dmgMult=1.0` |
| **Scythe of vitur** | Melee (5t) | Vs ≥2-tile target hits up to 3× at **100/50/25%** of max, each rolled independently → mean swing = `p·(max/2)·(1+0.5+0.25)` = ×1.75. 2×2 → 2 hits, 3×3+ → 3 hits, 1×1 → 1 hit. | vs ≥2-tile monster | ✅ `dmgMult=1.75` (over-credits 1-tile) |

### 4b. Symmetric task-conditional multipliers → `ConditionalBonusRegistry` (ADR-0007 extension)

| Weapon | Style | Effect | Condition | Fits v1 model? |
|---|---|---|---|---|
| **Dragon hunter lance** | Melee (4t) | **+20% accuracy & +20% damage** (symmetric) | vs draconic (excl. Elvarg & revenant dragons) | ✅ symmetric ×1.20 — fits single scalar |

### 4c. Real mechanics that need a model extension (defer / flag — see §6)

| Weapon | Style | Effect | Why it doesn't fit v1 |
|---|---|---|---|
| **Twisted bow** | Ranged | acc & dmg scale with target Magic (cap +140% acc / +250% dmg; M-cap 250, 350 CoX) | needs a per-target scaling function, not a scalar |
| **Tumeken's shadow** | Magic | ×3 worn magic atk & mdmg (×4 ToA), 100% dmg cap | magic weapon (out of v1 weapon-DPS scope); multiplies *other* gear |
| **Dragon hunter crossbow** | Ranged | **+30% acc / +25% dmg** vs draconic (asymmetric) | asymmetric → needs `ConditionalBonus` acc/dmg split |
| **Dragon hunter wand** | Magic | +75% acc / +40% dmg vs draconic | magic + asymmetric |
| **Arclight / Emberlight** | Melee | +70% acc / +70% dmg vs demons (49% vs Duke) | symmetric — *would* fit; demon predicate + task flag not yet modelled |
| **Darklight** | Melee | +60% acc / +60% dmg vs demons | as above (lower tier) |
| **Keris (+ partisans)** | Melee | +33% dmg & 1/51 triple-dmg vs Kalphites/scarabs | proc term (1/51 ×3) not a clean scalar; niche |
| **Colossal blade** | Melee | +2 max/tile of size, cap +10 | additive-to-max-hit, size-scaled — different shape |
| **Soulreaper axe** | Melee | +6% str/soul, max +30% (5 stacks), 8 self-dmg/atk | ramping build-up + HP cost; not steady-state |
| **Viggora's / Craw's / Webweaver / Thammaron's** | Mixed | +50% acc & dmg vs Wilderness NPCs | Wilderness predicate; symmetric (would fit if Wildy modelled) |
| **Leaf-bladed / broad ammo** | Melee/Ranged | enables (and ~+17.5% dmg, *verify*) vs Turoth/Kurask | gating mechanic + unverified % |
| **Bone weapons** | All | +10 max vs rats (Scurrius) | additive-to-max; very niche |
| **Sanguinesti staff** | Magic | 1/6 heal ½ dmg | sustain (HP), not DPS; magic |
| **Toxic blowpipe** | Ranged | +20 rstr built-in; 25% venom | rstr is read live; venom is DoT not on-hit DPS |

### 4d. Gear-level conditionals already modelled (for reference — not weapon rows)

These live on armour/amulet slots and are already in `ConditionalBonusRegistry` (LFB / ADR-0007).
Listed so weapon-DPS work doesn't double-count them. **The engine's encoded values are authoritative
for the plugin** (verified at build time against the 1.12.31.1 client); the wiki phrasing is noted
where it differs.

| Item | Encoded multiplier (plugin) | Condition | Wiki note |
|---|---|---|---|
| Black mask / Slayer helm | melee ×1.1667 (7/6) | on Slayer task | imbued adds rng/mag ×1.15 |
| Slayer helm (i) / Black mask (i) | melee ×1.1667 + rng/mag ×1.15 | on Slayer task | — |
| Salve amulet (e) / (ei) | **×1.20** | vs undead | (e) = melee only; (ei) = all 3 styles |
| Salve amulet (base) / (i) | melee ×1.1667; (i) adds rng ×1.1667, **mag ×1.15** | vs undead | salve takes priority over black mask (no stack) |

> Per LFB-5: only the **higher** of black-mask vs salve applies to a DPS number (no stacking).

---

## 5. Special attacks (spec) — excluded from continuous-DPS ranking

The engine recommends a single **continuous** loadout and does **not** spec-swap, so these are ranked
on their *sustained no-spec* DPS only (correctly placing e.g. AGS below Fang as a main-hand). This
list exists so the engine can **recognise** spec-tools, not rank by them.

| Weapon | Style | Spec | Energy | Effect |
|---|---|---|---|---|
| Dragon dagger | Melee | Puncture | 25% | 2 hits, +25% acc / +15% dmg each |
| Dragon claws | Melee | Slice & Dice | 50% | 4 hits, cascading; guaranteed once one lands |
| Armadyl godsword | Melee | The Judgement | 50% | ×1.375 max hit, ×2 accuracy |
| Bandos godsword | Melee | Warstrike | 50% | ×1.21 dmg; drains target stats = dmg |
| Saradomin godsword | Melee | Healing Blade | 50% | heal 50% dmg→HP, 25%→prayer |
| Zamorak godsword | Melee | Ice Cleave | 50% | +10% dmg, ×2 acc, freeze ~20s |
| Ancient godsword | Melee | Blood Sacrifice | 50% | dmg + sigil heal on target death |
| Dragon warhammer | Melee | Smash | 50% | −30% target current Defence |
| Elder maul | Melee | Pulverize | 50% | −35% target Defence, +25% acc |
| Statius's warhammer | Melee | Smash | 35% | −70% target Defence |
| Barrelchest anchor | Melee | Sunder | 50% | drains combat by 10% of dmg |
| Granite maul | Melee | Quick Smash | 60% (50% ornate) | instant extra attack |
| Granite hammer | Melee | Hammer Blow | 60% | +50% acc, +5 guaranteed dmg |
| Voidwaker | Melee | Disrupt | 50% | guaranteed magic hit 50–150% of max melee, ignores def |
| Abyssal bludgeon | Melee | Penance | 50% | +0.5% dmg per missing prayer pt |
| Abyssal dagger | Melee | Abyssal Puncture | 25% | 2 hits, +25% acc / −15% dmg |
| Abyssal whip | Melee | Energy Drain | 50% | drains target run energy (PvP) |
| Abyssal tentacle | Melee | Binding Tentacle | 50% | bind 5s + ~50% poison |
| Saradomin sword | Melee | Saradomin's Lightning | 100% | +10% melee dmg + 1–16 magic |
| Dragon mace / longsword / sword | Melee | Shatter / Cleave / Wild Stab | 25 / 25 / 40% | budget burst (Wild Stab ignores Prot Melee) |
| Dragon halberd / 2h / spear | Melee | Sweep / Powerstab / Shove | 30 / 60 / 25% | AoE / AoE / stun |
| Zamorakian hasta / spear | Melee | Shove | 25% | stun 3s |
| Ancient mace | Melee | Favour of the War God | 100% | drains prayer = dmg, transfers to user |
| Bone dagger | Melee | Backstab | 75% | def-ignoring guaranteed hit, lowers Def |
| Soulreaper axe | Melee | Behead | 0% (consumes souls) | acc/dmg burst, heals 8 HP/stack |
| Magic shortbow (i) | Ranged | Snapshot | 55% (i: 50%) | 2 arrows |
| Dark bow | Ranged | Descent of Darkness/Dragons | 55% | 2 arrows, +30% (+50% drag arrows) |
| Armadyl crossbow | Ranged | Armadyl Eye | 50% | ×2 acc + ×2 bolt-proc chance |
| Dragon crossbow | Ranged | Annihilate | 60% | 3×3 AoE ≤9 targets |
| Zaryte crossbow | Ranged | Evoke | 75% | ×2 acc, guarantees bolt proc |
| Dorgeshuun crossbow | Ranged | Snipe | 75% | def-ignoring guaranteed hit (opener), lowers Def |
| Toxic blowpipe | Ranged | Toxic Siphon | 50% | +100% acc, +50% dmg, heals ½ dmg |
| Heavy / Light ballista | Ranged | Concentrated Shot | 65% | +25% acc & dmg |
| Tonalztics of ralos | Ranged | Division | 50% | −Def by 10% of target Magic |
| Webweaver bow | Ranged | Swarm | 50% | 4 hits, ×2 acc, ≤40% max each |
| Dragon thrownaxe | Ranged | Momentum Throw | ~25% | next attack next tick, +25% acc *(verify)* |
| Dragon knife | Ranged | Duality | 25% | 2 knives, separate rolls |
| Seercull | Ranged | Soulshot | 100% | guaranteed hit; lowers target Magic |
| Volatile nightmare staff | Magic | Immolate | 55% | +50% acc; 49–97 dmg scaling on Magic |
| Eldritch nightmare staff | Magic | (prayer restore) | 55% | restores prayer = 50% dmg (cap 120) |
| Accursed sceptre | Magic | Condemn | 50% | −Def & −Magic up to 15% |
| Eye of ayak | Magic | Soul Rend | 50% | ×2 acc, +30% max, drains target Magic def |
| Dawnbringer | Magic | Pulsate | 35% | guaranteed 75–150 (Verzik/ToB only) |

---

## 6. Implications for the engine

**Headline win — attack speed.** The selector currently ranks the WEAPON slot by flat `att+str` and
never reads `Bonuses.attackSpeedTicks`. §1–§3 give the speed for every common weapon; the speed data
is already carried live, so routing the weapon slot through `weaponDps` (ADR-0008) is the fix. The
sharpest concrete cases this data confirms: **Fang 5t** vs **AGS 6t** (Fang wins on sustained DPS);
**blowpipe 2t** (Rapid, PvM) dominating low-defence ranged tasks; **Scythe 5t** multi-hit vs large
monsters; **Sanguinesti / tridents 4t** vs the **5t Tumeken's shadow**.

**v1 special-effects table — recommended set (matches design DEC-1):**
1. **Osmumten's fang** — `WeaponEffect{accuracyRolls=2, damageMultiplier=1.0}`. Pure formula-shape, fits exactly.
2. **Scythe of vitur** — `WeaponEffect{accuracyRolls=1, damageMultiplier=1.75}`. Fits; over-credits the rare 1-tile case (documented).
3. **Dragon hunter lance** — `ConditionalBonusRegistry` VS_DRAGON ×1.20 melee (symmetric). Zero model change.

These three are **correct-by-construction** in the existing two data homes. Each future passive weapon
in §4a/§4b = **one registry row + one test row**.

**Edge cases the model must handle / accept as limitations:**
- **Scythe vs 1-tile monsters** — the ×1.75 over-estimates (it hits once). Most scythe targets are large; accepted (ADR-0008).
- **Fang mean is unchanged** — the 15–85% band is *variance-only*; do **not** add a damage multiplier (only `accuracyRolls=2`). Easy to get wrong.
- **No-double-count** — DHL's conditional multiplier is applied *multiplicatively* in `weaponDps`; the weapon slot must be **excluded** from the additive `(m-1)·L` term (design §3.5).
- **DHL excludes Elvarg & revenant dragons** — if `TaskData.dragon` is set per-task, ensure those tasks aren't flagged draconic.

**Defer to v2 (need model extensions, flagged in §4c):**
- **Twisted bow** — per-target Magic-scaling function (acc cap +140%, dmg cap +250%, M-cap 250).
- **Tumeken's shadow** + magic weapon DPS generally — the spell-ordering boundary (design §6, DEC-2).
- **Dragon hunter crossbow / wand** — asymmetric acc≠dmg → needs a `ConditionalBonus` acc/dmg split (DEC-3).
- **Arclight/Emberlight/Darklight (demons), Wilderness weapons, Keris (kalphites)** — symmetric or proc bonuses gated on a *monster-category* predicate the dataset doesn't yet carry (only `undead`/`dragon` exist). Cheap to add per-category if the user wants demon/kalphite/Wildy coverage.

---

## 7. Slayer-relevant weapon shortlist (what's actually equipped on tasks)

- **Melee:** Scythe of vitur, Osmumten's fang, Ghrazi rapier / Blade of saeldor (c), Inquisitor's mace (crush), Abyssal tentacle/whip/bludgeon, Dragon hunter lance (dragons), Arclight/Emberlight (demons), Leaf-bladed battleaxe (Turoth/Kurask), Colossal blade (budget large), Dragon scimitar / rune scimitar (budget), Viggora's chainmace (Wildy).
- **Ranged:** Toxic blowpipe, Bow of faerdhinen (c), Twisted bow, Zaryte / Dragon hunter / Armadyl crossbow (+ enchanted bolts), rune crossbow (budget), chinchompas (stacked-NPC tasks), Venator bow (grouped), Craw's/Webweaver (Wildy).
- **Magic:** Tumeken's shadow, Sanguinesti staff, Trident of swamp/seas, Dragon hunter wand (draconic), Slayer's staff (e) (Magic Dart), Bone staff (Scurrius), Kodai wand / Ancient sceptre / Nightmare staves (freezing & barrage tasks).

---

## 8. Sources

All from `https://oldschool.runescape.wiki`. Slot tables used for breadth, individual weapon pages for
every attack speed and special-effect formula (slot-table speeds were not trusted — see §9).

- Slot tables: [One-handed_slot_table](https://oldschool.runescape.wiki/w/One-handed_slot_table), [Two-handed_slot_table](https://oldschool.runescape.wiki/w/Two-handed_slot_table)
- Reference: [Attack_speed](https://oldschool.runescape.wiki/w/Attack_speed), [Game_tick](https://oldschool.runescape.wiki/w/Game_tick), [Special_attacks](https://oldschool.runescape.wiki/w/Special_attacks), [Powered_staff](https://oldschool.runescape.wiki/w/Powered_staff)
- Melee pages: Scythe_of_vitur, Osmumten's_fang, Soulreaper_axe, Ghrazi_rapier, Blade_of_saeldor, Inquisitor's_mace, Dragon_hunter_lance, Abyssal_tentacle/whip/bludgeon/dagger, Zamorakian_hasta, Saradomin_sword, Voidwaker, Keris_partisan, Arclight, Emberlight, Darklight, Sarachnis_cudgel, Viggora's_chainmace, Verac's_flail, Colossal_blade, Dharok's_greataxe, Crystal_halberd, Granite_hammer/maul, Elder_maul, Dragon_warhammer/claws/dagger/scimitar, Armadyl/Bandos/Saradomin/Zamorak/Ancient_godsword, Leaf-bladed_battleaxe
- Ranged pages: Twisted_bow, Bow_of_faerdhinen, Crystal_bow, Toxic_blowpipe, Zaryte/Dragon_hunter/Armadyl/Dragon/Rune/Karil's/Dorgeshuun_crossbow, Sunlight_crossbow, Dark_bow, Magic_shortbow, Heavy/Light_ballista, Venator_bow, Tonalztics_of_ralos, Eclipse_atlatl, Craw's_bow, Webweaver_bow, Hunters'_spear, Dragon_thrownaxe/dart, Throwing_knife, Dart, Red_chinchompa, Chinchompa_(weapon)
- Magic pages: Tumeken's_shadow, Sanguinesti_staff, Trident_of_the_swamp/seas, Warped_sceptre, Accursed_sceptre, Thammaron's_sceptre, Eye_of_ayak, Bone_staff, Crystal_staff_(basic/attuned/perfected), Nightmare_staff (+ Volatile/Eldritch/Harmonised), Kodai_wand, Master_wand, Ancient_sceptre, Dragon_hunter_wand, Iban's_staff, Slayer's_staff, Salve_amulet, Black_mask, Berserker_necklace, Inquisitor's_armour, Bone_mace

---

## 9. Caveats, discrepancies & what was deprioritised

**Slot-table speeds untrusted.** The 1h slot-table summary mis-stated several speeds (e.g. Osmumten's
fang listed as 4t; the weapon page confirms **5t**). Every speed in this doc was taken from an
individual weapon page or the Attack-speed page.

**Discrepancies surfaced vs the kickoff brief / common belief (wiki treated as authoritative):**
- **Tonalztics of ralos** — the brief described "33% / magic-based"; the wiki says **2 hits each 0–75%
  of max ranged, rolled vs *light ranged* defence**. Used the wiki values.
- **Dragon darts have NO special attack** (the "Atomize" reference is wrong). Dragon **knife** has
  Duality; **thrownaxe** Momentum Throw (~25%, exact name/effect *worth re-verifying*).
- **Chinchompa medium fuse = 3t** (faster than short/long 4t) — counter-intuitive but confirmed twice.
- **Trident of the seas poison** — the fetched prose said "no poison"; the long-standing mechanic is
  regular poison (swamp = venom). **Flagged — verify the live infobox before hard-coding.**
- **Dragon dagger spec** — established value is **+25% accuracy / +15% damage** (one summary rendered
  it +15%/+15%).

**Formula precision to re-verify before hard-coding:**
- **Twisted bow** — caps are solid (acc +140%, dmg +250%, M-cap 250 / 350 CoX) but the exact
  polynomial constants rendered inconsistently across fetches; confirm against `Calculator:Twisted_bow`.
- **Emberlight** — a distinct on-hit defence-lowering passive (beyond Arclight's +70%) is commonly
  attributed but was **not** confirmed on the page; treat Emberlight = Arclight's demonbane until verified.
- **Leaf-bladed +17.5% damage** vs Turoth/Kurask — single-source figure; the *gating* (must use
  leaf-bladed/broad/magic) is certain, the % is not.
- **Keris partisan variants** — base +33% dmg & 1/51 triple confirmed; per-variant (breaching /
  corruption / the sun) numbers not enumerated.

**Deprioritised (NOT "complete" — conscious scope cuts):**
- Low-tier weapons (bronze→adamant, most God d'hide-tier thrown) given **compact rows only**;
  full stat lines reserved for PvM/Slayer-relevant and special-mechanic weapons.
- **Defensive stats** omitted entirely (out of scope — DPS engine reads offence).
- **Exact per-tier elemental/battle/mystic staff** magic bonuses not individually fetched (compact row).
- **PvP-only** weapons (Statius's warhammer, Ancient mace, Seercull, Granite maul) listed in the spec
  table for completeness but not stat-detailed.
- **Bolt/arrow/dart ammo tables** (ranged-strength per ammo) not enumerated — the engine reads ammo
  rstr live and the AMMO slot is picked by highest rstr (plan §4.7).

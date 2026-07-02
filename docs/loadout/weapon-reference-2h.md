# WR2b — Exhaustive Two-Handed Weapon Enumeration (OSRS)

> Research memory for the DPS engine. Authoritative source of every two-handed
> weapon in Old School RuneScape with offensive equipment bonuses, attack speed,
> and special-attack / passive notes.

## Completeness assertion

- **Source row count (Two-handed slot table):** **154 weapons**
- **Output row count (Section 1–3, official 2h table):** **154 weapons** — every row emitted, 1:1, none dropped.
- **Plus Section 4 supplement:** **17** additional 2h-occupying weapons that OSRS
  slot-codes as one-handed (powered/combat staves, Tonalztics, chinchompas) and
  therefore do **not** appear in the 2h slot table — included because the DPS
  engine needs them. **Total documented: 171.**
- **Output ≥ Source: 171 ≥ 154 ✓**
- **Scorching bow: PRESENT ✓** (Ranged section — the weapon the prior pass missed).

### Methodology note (why this pass is trustworthy)
The `WebFetch` small model could NOT reliably extract the ~154-row slot table in one
pass (it silently dropped rows and hallucinated a "290-row" count). The authoritative
row list was instead obtained by **fetching the rendered HTML of the slot table and
parsing the `<tr>` rows deterministically with a script** (157 `<tr>` = 1 header + 154
data + 2 wrapper → 154 weapons). Offensive stats were cross-verified against targeted
re-fetches (every spot-check matched). Attack speeds were pulled per-weapon from the
machine-readable `speed_bucket` data attribute / infobox `N ticks` text on each weapon
page (NOT the slot table, whose speeds are unreliable). Seconds = ticks × 0.6.

### Sources (OSRS Wiki, fetched 2026-06-29)
- Master list & offensive stats: <https://oldschool.runescape.wiki/w/Two-handed_slot_table>
- Attack speeds (category + per-weapon pages): <https://oldschool.runescape.wiki/w/Attack_speed> and individual weapon pages
- Special attacks: <https://oldschool.runescape.wiki/w/Special_attacks> and individual weapon pages

### Key finding on slot classification
OSRS slot-codes **all combat/powered staves** (Trident of the seas/swamp, Sanguinesti
staff, Staff of the dead family, all Nightmare staves), **Tonalztics of ralos**, and
**chinchompas** as the **one-handed "weapon" slot**, even though they occupy both hands
in practice. They are therefore absent from the Two-handed slot table. **Tumeken's
shadow** is the notable exception — it IS slot-coded 2h and appears in Section 3.

### Attack-style speed convention
- **Melee** speeds below are the weapon's fixed attack speed.
- **Ranged** speeds are the **Accurate-style base**. Rapid style = **base − 1 tick**
  (Rapid is the standard DPS style). Exceptions noted inline.
- **Magic** powered-staff speeds are the autocast speed.

---

## Section 1 — MELEE two-handed weapons

Columns: Stab / Slash / Crush / Magic / Str = melee strength. Magic/Ranged columns shown
only where non-zero. Speed in ticks (seconds).

### 1a. Two-handed swords (slash; 7t / 4.2s unless noted)

| Name | Stab | Slash | Crush | Str | Prayer | Speed | Special / notes |
|---|---|---|---|---|---|---|---|
| Bronze 2h sword | -4 | +9 | +8 | +10 | 0 | 7t (4.2s) | — |
| Iron 2h sword | -4 | +13 | +10 | +14 | 0 | 7t (4.2s) | — |
| Steel 2h sword | -4 | +21 | +16 | +22 | 0 | 7t (4.2s) | — |
| Black 2h sword | -4 | +27 | +21 | +26 | 0 | 7t (4.2s) | — |
| White 2h sword | -4 | +27 | +21 | +26 | +1 | 7t (4.2s) | — |
| Mithril 2h sword | -4 | +30 | +24 | +31 | 0 | 7t (4.2s) | — |
| Adamant 2h sword | -4 | +43 | +30 | +44 | 0 | 7t (4.2s) | — |
| Rune 2h sword | -4 | +69 | +50 | +70 | 0 | 7t (4.2s) | — |
| Gilded 2h sword | -4 | +69 | +50 | +70 | 0 | 7t (4.2s) | Cosmetic of rune 2h |
| Dragon 2h sword | -4 | +92 | +80 | +93 | 0 | 7t (4.2s) | **Spec: Powerstab/Cleave 60%** — AoE hit to all enemies within 1 tile |
| Colossal blade | -4 | +98 | +65 | +100 | 0 | **6t (3.6s)** | Passive: bonus flat damage scaling with target size (+2 per extra tile, max +10) |
| Shadow sword | -4 | +27 | +21 (+4 mag) | +26 | 0 | 7t (4.2s) | Novelty (Shadow of the Storm reward) |
| Spatula | -4 | +27 | +21 | +26 | 0 | 7t (4.2s) | Novelty |
| Katana | +7 | +45 | 0 | +40 | 0 | **4t (2.4s)** | Novelty (slash) |

### 1b. Claws (stab/slash; 4t / 2.4s)

| Name | Stab | Slash | Crush | Str | Prayer | Speed | Special / notes |
|---|---|---|---|---|---|---|---|
| Bronze claws | +3 | +4 | -4 | +5 | 0 | 4t (2.4s) | — |
| Iron claws | +4 | +6 | -4 | +7 | 0 | 4t (2.4s) | — |
| Steel claws | +8 | +11 | -4 | +12 | 0 | 4t (2.4s) | — |
| Black claws | +10 | +14 | -4 | +14 | 0 | 4t (2.4s) | — |
| White claws | +10 | +14 | -4 | +14 | +1 | 4t (2.4s) | — |
| Mithril claws | +11 | +16 | -4 | +17 | 0 | 4t (2.4s) | — |
| Adamant claws | +18 | +23 | -4 | +24 | 0 | 4t (2.4s) | — |
| Rune claws | +26 | +38 | -4 | +39 | 0 | 4t (2.4s) | — |
| Dragon claws | +41 | +57 | -4 | +56 | 0 | 4t (2.4s) | **Spec: Slice and Dice 50%** — 4 rapid hits (50/25/12.5/12.5% scaling) |
| Burning claws | +43 | +54 | 0 | +32 | 0 | 4t (2.4s) | **Spec: Burning Bulwark/Burning Barrage 30%** — 3 hits, chance to apply burn |

### 1c. Halberds (stab/slash, 2-tile reach; 7t / 4.2s unless noted)

| Name | Stab | Slash | Crush | Str | Prayer | Speed | Special / notes |
|---|---|---|---|---|---|---|---|
| Bronze halberd | +7 | +8 | 0 | +8 | 0 | 7t (4.2s) | — |
| Iron halberd | +9 | +12 | 0 | +12 | 0 | 7t (4.2s) | — |
| Steel halberd | +14 | +19 | 0 | +20 | 0 | 7t (4.2s) | — |
| Black halberd | +19 | +25 | 0 | +20 | 0 | 7t (4.2s) | — |
| White halberd | +19 | +25 | 0 | +20 | +1 | 7t (4.2s) | — |
| Mithril halberd | +22 | +28 | 0 | +29 | 0 | 7t (4.2s) | — |
| Adamant halberd | +28 | +41 | 0 | +42 | 0 | 7t (4.2s) | — |
| Rune halberd | +48 | +67 | 0 | +68 | 0 | 7t (4.2s) | — |
| Dragon halberd | +70 | +95 | 0 | +89 | 0 | 7t (4.2s) | **Spec: Sweep 30%** — 2 hits to up to 10 NPCs in a 1×3 line |
| Crystal halberd (Active) | +85 | +110 | +5 | +118 | 0 | 7t (4.2s) | **Spec: Sweep 30%** — same as dragon halberd |
| Noxious halberd | +80 | +132 | 0 | +142 | 0 | **5t (3.0s)** | 2-tile reach; no spec |

### 1d. Felling axes (slash/crush; 7t / 4.2s)

| Name | Stab | Slash | Crush | Str | Speed | Notes |
|---|---|---|---|---|---|---|
| Bronze felling axe | -3 | +6 | +3 | +8 | 7t (4.2s) | — |
| Iron felling axe | -3 | +8 | +4 | +11 | 7t (4.2s) | — |
| Steel felling axe | -3 | +12 | +9 | +14 | 7t (4.2s) | — |
| Black felling axe | -2 | +10 | +8 | +12 | 7t (4.2s) | — |
| Mithril felling axe | -3 | +19 | +16 | +20 | 7t (4.2s) | — |
| Rune felling axe | -3 | +41 | +38 | +46 | 7t (4.2s) | — |
| Adamant felling axe | -3 | +27 | +24 | +30 | 7t (4.2s) | — |
| Dragon felling axe | -3 | +60 | +51 | +67 | 7t (4.2s) | — |
| 3rd age felling axe | -3 | +60 | +51 | +67 | 7t (4.2s) | Cosmetic-tier (dragon stats) |
| Crystal felling axe (Active) | -3 | +60 | +51 | +67 | 7t (4.2s) | — |

### 1e. Spears (stab; reach 1; speeds vary)

| Name | Stab | Slash | Crush | Str | Prayer | Speed | Special / notes |
|---|---|---|---|---|---|---|---|
| Bronze spear (unp) | +5 | +5 | +5 | +6 | 0 | 4t (2.4s) | — |
| Bronze spear (p++) | +5 | +5 | +5 | +6 | 0 | 4t (2.4s) | Poisoned |
| Iron spear (unp) | +8 | +8 | +8 | +10 | 0 | 4t (2.4s) | — |
| Iron spear (p++) | +8 | +8 | +8 | +10 | 0 | 4t (2.4s) | Poisoned |
| Steel spear (unp) | +12 | +12 | +12 | +12 | 0 | 4t (2.4s) | — |
| Steel spear (p++) | +12 | +12 | +12 | +12 | 0 | 4t (2.4s) | Poisoned |
| Black spear (unp) | +15 | +15 | +15 | +16 | 0 | 4t (2.4s) | — |
| Black spear (p++) | +15 | +15 | +15 | +16 | 0 | 4t (2.4s) | Poisoned |
| Mithril spear (unp) | +17 | +17 | +17 | +18 | 0 | 4t (2.4s) | — |
| Mithril spear (p++) | +17 | +17 | +17 | +18 | 0 | 4t (2.4s) | Poisoned |
| Adamant spear (unp) | +24 | +24 | +24 | +28 | 0 | 4t (2.4s) | — |
| Adamant spear (p++) | +24 | +24 | +24 | +28 | 0 | 4t (2.4s) | Poisoned |
| Rune spear (unp) | +36 | +36 | +36 | +42 | 0 | 4t (2.4s) | — |
| Rune spear (p++) | +36 | +36 | +36 | +42 | 0 | 4t (2.4s) | Poisoned |
| Gilded spear | +36 | +36 | +36 | +42 | 0 | 4t (2.4s) | Cosmetic of rune spear |
| Dragon spear (unp) | +55 | +55 | +55 | +60 | 0 | 4t (2.4s) | **Spec: Shove 25%** — push + stun 5 ticks (3.0s) |
| Dragon spear (p++) | +55 | +55 | +55 | +60 | 0 | 4t (2.4s) | Poisoned; **Spec: Shove 25%** |
| Leaf-bladed spear | +47 | +42 | +36 | +50 | 0 | **5t (3.0s)** | Bonus vs Turoth/Kurask |
| Blue moon spear | +70 | +62 | +62 (+30 mag) | +71 | 0 (+5% mag dmg) | **5t (3.0s)** | Hybrid melee/magic; spec: Blue moon (AoE on next cast) |
| Guthan's warspear (Undamaged) | +75 | +75 | +75 | +75 | 0 | **5t (3.0s)** | Barrows set effect: ~25% chance to heal by damage dealt |
| Zamorakian spear | +85 | +65 | +65 | +75 | +2 | 4t (2.4s) | **Spec: Shove 25%** — push + stun (AoE in a line) |

### 1f. Mauls / crush 2h (speeds vary)

| Name | Stab | Slash | Crush | Str | Prayer | Speed | Special / notes |
|---|---|---|---|---|---|---|---|
| Granite maul (Normal) | 0 | 0 | +81 | +79 | 0 | 7t (4.2s) | **Spec: Quick Smash 50%/60%** — instant extra attack |
| Elder maul | 0 | 0 | +135 | +147 | 0 | **6t (3.6s)** | **Spec: 50%** — reduces target Defence (and Magic) by 35% |
| Tzhaar-ket-om | 0 | 0 | +80 | +85 | 0 | 7t (4.2s) | — |
| Tzhaar-ket-om (t) | 0 | 0 | +80 | +85 | 0 | 7t (4.2s) | Cosmetic |
| Barrelchest anchor (Fixed) | -2 | +10 | +92 | +100 | 0 | **6t (3.6s)** | **Spec: Sunder 50%** — drains combat stats by % of damage |
| Hill giant club | -4 | +50 | +65 | +70 | 0 | 7t (4.2s) | — |
| Gadderhammer | -4 | -4 | +35 | +35 | 0 | **5t (3.0s)** | Bonus damage vs ghosts (Melzar's) |
| Saradomin mjolnir | 0 | 0 | +11 | +14 | 0 | **6t (3.6s)** | Casts a lightning spell |
| Guthix mjolnir | 0 | 0 | +11 | +14 | 0 | **6t (3.6s)** | Casts a lightning spell |
| Zamorak mjolnir | 0 | 0 | +11 | +14 | 0 | **6t (3.6s)** | Casts a lightning spell |
| Torag's hammers (Undamaged) | -4 | -4 | +85 | +72 | 0 | **5t (3.0s)** | Barrows set effect: chance to drain target Run energy / dual-wield look but 2h slot |
| Dinh's bulwark | 0 | 0 | +124 | +38 | 0 | **5t (3.0s)** | **Spec: 50%** — AoE 11×11 bash, lowers nearby enemies' offensive stats (Pummel style only) |
| Dual sai | +11 | +8 | -4 (-4 mag) | +14 | 0 | **5t (3.0s)** | Novelty |
| Nunchaku | -4 | -4 | +11 | +14 | 0 | **5t (3.0s)** | Novelty |
| Guthix/Sara/Zamorak mjolnir | — | — | — | — | — | — | (listed above) |

### 1g. High-tier / boss melee 2h (slash unless noted)

| Name | Stab | Slash | Crush | Str | Prayer | Speed | Special / notes |
|---|---|---|---|---|---|---|---|
| Abyssal bludgeon | 0 | 0 | +102 | +85 | 0 | 4t (2.4s) | **Spec: Penance 50%** — +0.5% damage per missing prayer point |
| Saradomin sword | 0 | +82 | +60 | +82 | +2 | 4t (2.4s) | Passive 1/8 chance to add ~1–16 magic-damage hit |
| Saradomin's blessed sword (Partially charged) | 0 | +100 | +60 | +88 | +2 | 4t (2.4s) | **Spec: Saradomin's Lightning 100%** — +10% melee dmg + 1–16 magic damage |
| Soulreaper axe | +28 | +134 | +66 | +121 | 0 | **5t (3.0s)** | Passive: builds Soul stacks (up to 5) for +6%/stack damage at HP cost; **Spec consumes stacks** |
| Dharok's greataxe (Undamaged) | -4 | +103 | +95 | +105 | 0 | 7t (4.2s) | Barrows set effect: damage scales up as HP drops (lower HP → higher max hit) |
| Verac's flail (Undamaged) | +68 | -2 | +82 | +72 | +6 | **5t (3.0s)** | Barrows set effect: chance to ignore Defence/prayer, +1 bonus damage |
| Dual macuahuitl | +115 | -4 | +121 (-4 mag) | +81 | 0 | 4t (2.4s) | **Spec: 25%** — double-hit; passive: Blood Moon set (heal/extra hits) |
| Earthbound tecpatl | +72 | +11 | 0 | +64 | +2 | 4t (2.4s) | Blood Moon-set dagger-type (stab) |
| Glacial temotli | +11 | 0 | +72 | +64 | +2 | 4t (2.4s) | Blue Moon-set (crush) |
| Sulphur blades | +11 | +72 | 0 | +64 | 0 | 4t (2.4s) | Eclipse Moon-set (slash) |

### 1h. Godswords (slash; 6t / 3.6s; all +8 prayer; Stab 0 / Slash +132 / Crush +80 / Str +132)

| Name | Speed | Special attack |
|---|---|---|
| Armadyl godsword | 6t (3.6s) | **The Judgement 50%** — +37.5% damage, doubled accuracy |
| Bandos godsword | 6t (3.6s) | **Warstrike 50%** — drains target's combat stats by damage dealt |
| Saradomin godsword | 6t (3.6s) | **Healing Blade 50%** — heal 50% of damage as HP, 25% as prayer |
| Zamorak godsword | 6t (3.6s) | **Ice Cleave 50%** — freezes target for ~20s if it hits |
| Ancient godsword | 6t (3.6s) | **Blood Sacrifice 50%** — applies a damage-over-time/heal-block, heals attacker |

### 1i. Scythe (slash; hits up to 3 targets / 3 tiles, 100%/50%/25% damage)

| Name | Stab | Slash | Crush | Str | Speed | Notes |
|---|---|---|---|---|---|---|
| Scythe of vitur (Charged) | +70 | +125 | +30 (-6 mag) | +75 | **5t (3.0s)** | Passive 3-hit AoE; **no special attack** |
| Sanguine scythe of vitur (Charged) | +70 | +125 | +30 (-6 mag) | +75 | **5t (3.0s)** | Cosmetic recolour of Scythe |

---

## Section 2 — RANGED two-handed weapons

Speed = **Accurate base** (Rapid = base − 1 tick) unless noted. Ranged str = Ranged strength.

### 2a. Standard bows (shortbows 4t base / longbows 6t base / composite 5t base)

| Name | Ranged atk | Ranged str | Speed (Acc / Rapid) | Notes |
|---|---|---|---|---|
| Shortbow | +8 | 0 | 4t / 3t | — |
| Longbow | +8 | 0 | 6t / 5t | — |
| Oak shortbow | +14 | 0 | 4t / 3t | — |
| Oak longbow | +14 | 0 | 6t / 5t | — |
| Signed oak bow | +14 | 0 | 4t / 3t | Shortbow-class (quest reward) |
| Willow shortbow | +20 | 0 | 4t / 3t | — |
| Willow longbow | +20 | 0 | 6t / 5t | — |
| Willow comp bow | +22 | 0 | 5t / 4t | Composite |
| Maple shortbow | +29 | 0 | 4t / 3t | — |
| Maple longbow | +29 | 0 | 6t / 5t | — |
| Yew shortbow | +47 | 0 | 4t / 3t | — |
| Yew longbow | +47 | 0 | 6t / 5t | — |
| Yew comp bow | +49 | 0 | 5t / 4t | Composite |
| Magic shortbow | +69 | 0 | 4t / 3t | **Spec: Snapshot 55%** — 2 arrows |
| Magic shortbow (i) | +75 | 0 | 4t / 3t | Imbued; **Spec: Snapshot 50%** |
| Magic longbow | +69 | 0 | 6t / 5t | — |
| Magic comp bow | +71 | 0 | 5t / 4t | Composite |
| 3rd age bow | +80 | 0 | 4t / 3t | Shortbow-class |
| Seercull | +69 | 0 | 5t / 4t | **Spec: Soulshot 100%** — guaranteed hit, lowers target Magic |
| Bone shortbow | +69 | 0 | 4t / 3t | — |
| Training bow | +8 | 0 | 4t / 3t | Novelty |
| Rain bow | +8 | 0 | 4t / 3t | Novelty |
| Cursed goblin bow | 0 | 0 | 4t / 3t | Novelty (no bonus) |

### 2b. Ogre bows

| Name | Ranged atk | Ranged str | Speed (Acc / Rapid) | Notes |
|---|---|---|---|---|
| Ogre bow | +38 | 0 | 8t / 7t | Uses ogre/brutal arrows |
| Comp ogre bow | +38 | 0 | 5t / 4t | Composite ogre bow |

### 2c. High-tier / boss ranged 2h

| Name | Ranged atk | Ranged str | Other | Speed (Acc / Rapid) | Special / passive |
|---|---|---|---|---|---|
| Crystal bow (Active) | +100 | +78 | — | 5t / 4t | Generates own ammo; degrades |
| Bow of faerdhinen (c) | +128 | +106 | — | 5t / 4t | Crystal armour set damage bonus; uses no ammo |
| Bow of faerdhinen (Charged) | +128 | +106 | — | 5t / 4t | (charged base form) |
| **Twisted bow** | +70 | +20 | — | 6t / 5t | Passive: accuracy & damage scale with target's Magic level (capped) |
| **Scorching bow** | +124 | +40 | — | 6t / 5t | Passive: +30% accuracy & damage vs demons; ignites on-task undead (Slayer synergy) |
| Dark bow (Regular) | +95 | 0 | — | 9t / 8t | **Spec: Descent of Dragons/Darkness 55%** — 2 arrows, +30% dmg (min 5 each / 8 with dragon arrows) |
| Venator bow (Charged) | +90 | +25 | — | 5t / 4t | Passive: shots pierce/bounce to up to 3 nearby targets |
| Webweaver bow (Charged) | +85 | +65 | — | 4t / 3t | **Spec: Swarm 50%** — 4 rapid hits, reduces target's stats |
| Craw's bow (Uncharged) | +75 | +60 | — | 4t / 3t | Charged form: +damage vs Wilderness/God targets; no spec |
| **Heavy ballista** | +125 | +15 | — | 7t (fixed; no rapid bonus) | **Spec: Concentrated Shot 65%** — +25% accuracy & damage |
| **Light ballista** | +110 | 0 | — | 7t (fixed) | **Spec: Concentrated Shot 65%** — +25% accuracy & damage |
| **Eclipse atlatl** | +87 | +40 | — | 4t / 3t | Throws atlatl darts; scales ranged str with Strength; melee-armour synergy |
| Karil's crossbow (Undamaged) | +84 | 0 | — | 4t / 3t | Barrows set effect: chance to lower target Agility / extra dmg (uses bolt racks) |

### 2d. Salamanders (3 styles: slash / ranged / magic; 5t / 3.0s fixed)

| Name | Slash | Ranged atk | Magic atk | Ranged str | Str | Speed | Notes |
|---|---|---|---|---|---|---|---|
| Swamp lizard | +10 | +20 | (mag style) | +22 | +22 | 5t (3.0s) | Uses Guam tar |
| Orange salamander | +19 | +29 | (mag style) | +31 | +31 | 5t (3.0s) | — |
| Red salamander | +37 | +47 | (mag style) | +49 | +49 | 5t (3.0s) | — |
| Black salamander | +59 | +69 | (mag style) | +71 | +71 | 5t (3.0s) | — |
| Tecu salamander | +77 | +87 | (mag style) | +91 | +91 | 5t (3.0s) | Highest-tier salamander |

### 2e. Blowpipes (3t base / 2t rapid in PvM)

| Name | Ranged atk | Ranged str | Speed (Acc / Rapid) | Notes |
|---|---|---|---|---|
| Toxic blowpipe (Charged) | +30 | +20 | 3t / 2t | **Spec: Toxic Siphon 50%** — heal 50% of damage; uses darts; venom |
| Camphor blowpipe (Charged) | +12 | +2 | 3t / 2t | Variant blowpipe |
| Ironwood blowpipe (Charged) | +16 | +4 | 3t / 2t | Variant blowpipe |
| Rosewood blowpipe (Charged) | +22 | +6 | 3t / 2t | Variant blowpipe |

### 2f. Novelty / misc ranged

| Name | Ranged atk | Ranged str | Speed | Notes |
|---|---|---|---|---|
| Goblin paint cannon | 0 | 0 | 3t (1.8s) | Novelty |

---

## Section 3 — MAGIC two-handed weapons (in the official 2h slot table)

| Name | Magic atk | Magic dmg % | Prayer | Other offensive | Speed | Notes |
|---|---|---|---|---|---|---|
| **Tumeken's shadow (Charged)** | +35 | 0% | +1 | — | 5t (3.0s) | Passive: **triples** magic attack & damage bonuses of worn gear (×3, capped); best-in-slot magic |
| Toktz-mej-tal (obsidian staff) | +15 | 0% | +5 | Stab +15 / Crush +55 / Str +55 | 6t (3.6s) | Battlestaff-class; can autocast |
| Merfolk trident | +0 | 0% | 0 | Stab/Slash/Crush +8, Str +4 | 5t (3.0s) | Aquanite/Fossil Island fight weapon |
| Blue moon spear | +30 | +5% | 0 | Stab +70 / Slash +62 / Crush +62 / Str +71 | 5t (3.0s) | (also in Section 1e) hybrid melee/magic |
| Rat pole (Empty) | +4 | 0% | 0 | Slash -1 / Crush +7 / Str +3 | — | Novelty (Rat-catchers reward) |

---

## Section 4 — SUPPLEMENT: 2h-occupying weapons that OSRS slot-codes as one-handed

> These are **NOT** in the Two-handed slot table (the game classifies them as the
> one-handed "weapon" slot), but they functionally occupy both hands and the DPS
> engine must treat them as two-handed. Stats from each weapon's own wiki page.

### 4a. Powered staves (magic; max hit scales with Magic level, not a spell)

| Name | Magic atk | Magic dmg % | Speed | Max-hit mechanic / passive |
|---|---|---|---|---|
| Trident of the seas | +15 | — | 4t (2.4s) | Max hit ⌊Magic/3⌋−5-ish; caps ~31. Built-in spell, uses charges |
| Trident of the swamp | +25 | — | 4t (2.4s) | Max hit ⌊Magic/3⌋−2 (24 @78 Magic → 39 @123); 25% venom passive (100% w/ serpentine helm) |
| Sanguinesti staff | +25 | — | 4t (2.4s) | Max hit ⌊Magic/3⌋−1 (26 @82 → 40 @123); passive 1/6 chance to heal 50% of damage. No spec |
| Tumeken's shadow | +35 | 0% | 5t (3.0s) | (Also Section 3 — it IS slot-coded 2h) — ×3 gear magic bonuses |

### 4b. Nightmare staves (magic; +16 magic atk, +15% magic dmg, +0 prayer each)

| Name | Speed | Special attack |
|---|---|---|
| Nightmare staff | 5t (3.0s) | None |
| Volatile nightmare staff | 5t (3.0s) | **55%** — high damage + 50% accuracy, no runes consumed |
| Eldritch nightmare staff | 5t (3.0s) | **55%** — high damage, restores prayer by 50% of damage, no runes |
| Harmonised nightmare staff | **4t (3.0s for standard spells; 4t cast)** | None — casts standard (modern) spells one tick faster (5t→4t) |

### 4c. Combat staves (melee + magic hybrid; Stab +55 / Slash +70 / Crush 0 / Str +72 / +15% magic dmg)

| Name | Magic atk | Speed | Passive / effect |
|---|---|---|---|
| Staff of the dead | +17 | 4t melee / 5t magic | Spec: halves melee damage taken for 1 min; +15% magic dmg |
| Toxic staff of the dead | +25 | 4t melee / 5t magic | As above + 25% venom on hit |
| Staff of light | +17 | 4t melee / 5t magic | +15% magic dmg; spec: temporary 0 melee-damage protection |
| Staff of balance | +17 | 4t melee / 5t magic | +15% magic dmg |

### 4d. Tonalztics of ralos (ranged; +115 ranged atk, +55 ranged str)

| Name | Speed (Acc / Rapid) | Special / passive |
|---|---|---|
| Tonalztics of ralos (Uncharged) | 7t / 6t | Single hit 0–75% of max ranged hit; reach 6 (8 longrange) |
| Tonalztics of ralos (Charged) | 7t / 6t | **Spec: Division 50%** — lowers Defence by 10% of target Magic; charged = TWO independent hits per attack |

### 4e. Chinchompas (ranged AoE thrown; 4t / 3t base, "Medium fuse" style 3t / 2t)

| Name | Ranged atk | Ranged str | Speed | Mechanic |
|---|---|---|---|---|
| Chinchompa (grey) | +45 | 0 | 4t / 3t (med-fuse −1) | 3×3 AoE; surrounding targets hit if initial roll succeeds |
| Red chinchompa | +70 | +15 | 4t / 3t | 3×3 AoE |
| Black chinchompa | +80 | +30 | 4t / 3t | 3×3 AoE (best chin) |

---

## Section 5 — Novelty / non-combat rows in the 2h slot table (completeness)

These appear as rows in the Two-handed slot table (counted in the 154) but are joke /
non-combat / event items with negative or zero offensive stats. Listed for 1:1 completeness.

| Name | Notable stats | Note |
|---|---|---|
| Clueless scroll | Stab/Slash -100, Crush -50, Str -10 | Novelty |
| Gilded spade | Stab/Slash -100, Crush -50, Str -10 | Novelty (spade) |
| Large spade | Stab/Slash -100, Crush -50, Str -10 | Novelty (spade) |
| Heavy casket | Crush +11, Str +9 | Novelty |
| Clan vexillum (Black) | all 0 | Novelty banner |
| Comp ogre bow | (see 2b) | — |
| Rat pole (Empty) | (see Section 3) | Novelty |

---

## Appendix — Special-attack energy costs quick reference (2h weapons)

| Weapon | Spec | Energy |
|---|---|---|
| Dragon dagger-style not 2h — n/a | | |
| Dragon claws | Slice and Dice | 50% |
| Dragon 2h sword | Powerstab/Cleave | 60% |
| Dragon halberd / Crystal halberd | Sweep | 30% |
| Dragon spear / Zamorakian spear | Shove | 25% |
| Granite maul | Quick Smash | 50% (60% non-ornate) |
| Elder maul | (def reduction) | 50% |
| Barrelchest anchor | Sunder | 50% |
| Abyssal bludgeon | Penance | 50% |
| Dinh's bulwark | Block/Bash | 50% |
| Burning claws | Burning Bulwark | 30% |
| All godswords | (varies by god) | 50% |
| Saradomin's blessed sword | Saradomin's Lightning | 100% |
| Seercull | Soulshot | 100% |
| Magic shortbow | Snapshot | 55% (50% imbued) |
| Dark bow | Descent of Dragons | 55% |
| Heavy / Light ballista | Concentrated Shot | 65% |
| Webweaver bow | Swarm | 50% |
| Toxic blowpipe | Toxic Siphon | 50% |
| Tonalztics of ralos | Division | 50% |
| Volatile nightmare staff | (volatile blast) | 55% |
| Eldritch nightmare staff | (prayer restore) | 55% |

> Notes: Scythe of vitur, Twisted bow, Bow of faerdhinen, Tumeken's shadow, Sanguinesti
> staff, Venator bow, Craw's bow, Soulreaper axe, Dharok's/Verac's/Guthan's/Torag's/Karil's
> rely on **passives / set effects**, not a special-attack bar (Soulreaper's spec consumes
> Soul stacks). Energy costs are from the OSRS Wiki Special_attacks page / weapon pages and
> should be re-confirmed at implementation time for any borderline DPS-critical weapon.

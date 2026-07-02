# Gaps — dragons + bosses slice (variants-dragons-bosses.md)

Never-invented values. Each item below is UNKNOWN/uncertain in the source and is also flagged inline in the variants file.

## Elemental weakness type missing on infobox (percent present, type blank)
- **Brutal blue dragon**: `elementalweaknesspercent = 50` but no `elementalweaknesstype`. Blue-dragon family is
  Water 50% (normal Blue/Baby blue both state Water), so almost certainly Water — but the infobox does not state
  it explicitly for the brutal variant. Recorded as "MAGIC 50%, element likely Water".
- **Baby black dragon**: no elemental fields at all on the infobox. Recorded weakness from lowest-def (stab 0 =
  MELEE-stab). Parent Black dragon is Water 50%; baby's element not stated.

## No stated elemental weakness — weakness derived from lowest defensive bonus only
- **Hellhound** (all variants/levels): every defensive bonus = 0 and NO elemental type. Genuinely UNKNOWN style
  lean — any style works; recorded as UNKNOWN.
- **Reanimated hellhound / Reanimated dagannoth / Reanimated troll**: no elemental type stated; defences near 0.
  Derived from lowest-def.
- **Night beast** (Dark beast superior): no elemental type; derived MELEE-stab (stab 75 lowest).

## Range defence split (light/standard/heavy)
- OSRS infoboxes split ranged defence into light/standard/heavy. Reported all three throughout. The single
  "ranged defence" value the plugin may expect = the `standard` column (middle value) unless modelling ammo weight.

## Boss stat blocks without per-style defensive bonuses
- **Vorkath**: infobox gives Defence level (214 post-quest / 164 DS2) but NOT stab/slash/crush/magic/range
  defensive bonuses (uses scaled/phase mechanics). Per-style defence bonuses UNKNOWN. Elemental weakness Fire 40%
  is stated. In practice ranged (esp. on the standard phase) is the meta — not a wiki "weakness" field.

## Location uncertainty
- **Frost dragon**: Sailing-locked task (Sailing 87). Wiki page has full combat stats (npc 14922, cmb 202) but the
  exact island/dungeon location is NOT confirmed from the infobox (infoboxes don't carry location). Marked UNKNOWN.
  Recommend a follow-up fetch of the Frost dragon page body / Slayer_task page for the spawn location before
  hard-coding any location string.

## Category/flag confirmations (NOT gaps — recorded for the dataset author)
- **Vorkath** carries BOTH `dragon` (draconic -> DHL/DHCB) AND `undead` (Salve amulet applies). Dual-flag.
- **Greater Skeleton Hellhound** (Vet'ion & Calvar'ion) is `undead` (NOT demon, NOT draconic) despite being a
  Hellhound-task monster -> Salve applies; demonbane does NOT.
- **Skeleton Hellhound** (Tarn) and **Hellhound** and **Waterfiend** carry the `demon` attribute, but per team
  memory the plugin demonbane set is EXACTLY {Abyssal/Black/Greater demons, Nechryael} — so demonbane must NOT be
  flagged for Hellhounds/Waterfiends. Confirmed inline.
- **Skeletal Wyvern** is `dragon` (draconic, takes dragonbane) and is NOT undead — confirms team memory.

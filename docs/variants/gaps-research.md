# Research gaps — stats/flags that could not be pinned from the wiki

One list across all Duradel task groups. "UNKNOWN" in a variant file means the value is here. Nothing was
invented; ambiguous-but-resolvable cases are recorded with how to resolve them.

## Demons (variants-demons.md)
- Reanimated abyssal: combat level is N/A on the wiki. NOTE: reanimated monsters do NOT actually count toward
  the slayer task — included per brief, flagged for removal by Architect.
- Skotizo: melee vs magic defence tied at 80 → recorded MELEE/MAGIC (Arclight + Water 40% resolve to melee+demonbane in practice).
- Demonic gorilla & Balfrug Kreeyath: melee/ranged defence tied at 0 → recorded MELEE/RANGED.

## Dragons (variants-dragons.md)
- No stat fields UNKNOWN. Caveat: only Brutal black dragon has an explicit Slayer 77 sub-requirement; other
  brutal/metal/frost variants have a blank `slaylvl` field on the wiki.

## Wyrms / Drakes / Wyverns (variants-wyrms-wyverns.md)
- None. All 11 variants complete. (Confirmed Fossil Island + Skeletal wyverns have NO superior; Skeletal wyvern is NOT undead.)

## Undead / Basilisks (variants-undead-basilisks.md)
- `drange` (ranged defence bonus) unset/UNKNOWN on these older infoboxes for most variants — needs a second pass or live NPCDefinition.
- Vampyre variants (Feral, Juvenile, Juvinate, Vyrewatch, Vyrewatch Sentinel): all-zero defence bonuses + no
  element on the infobox → weakness genuinely UNKNOWN (damage is gated by silver / Ivandis / Blisterwood, not a defence profile).
- Dark Ankou: weakness UNKNOWN (sparse infobox). Ankou task has no true superior slayer monster.

## Kalphite / Araxyte / Zygomite / Aquanite (variants-kalphite-misc.md)
- Araxyte: crush vs magic defence tied at 20 → MELEE/MAGIC tie (fire 50% noted).
- Kalphite Queen: weakness is form-dependent (crush while crawling / magic while airborne) — recorded per form, not a single value.
- Reanimated kalphite: combat level N/A on wiki. No superior exists for Kalphites or Zygomites (confirmed).

## Giants / Beasts / Hounds (variants-giants-beasts.md)
- All Bloodveld variants + GWD/Reanimated hellhounds: all-zero defence bonuses + no element → weakness UNKNOWN (no infobox data to disambiguate; standard play is melee).
- Confirmed (not gaps): Fire giants and Dagannoth have NO superior; Dark beast is NOT a demon; Dagannoth Kings differ (Rex MAGIC / Prime RANGED / Supreme MELEE).

## Devils / Kraken / Aerial (variants-devils-aerial.md)
- Thermonuclear smoke devil: ranged defence bonus UNKNOWN.
- Dire gryphon (the REAL superior of Gryphon — Shellbane gryphon is the BOSS, brief had this swapped): full stats (ids/combat/def) UNKNOWN, page not fetched.
- Reanimated aviansie: elemental weakness UNKNOWN.
- Kree'arra: ranged vs magic weakness ambiguous.
- Note: smoke/dust devils are barraged due to magic LEVEL 1, not low magic DEFENCE (smoke devil magic def is 600).

## Misc — Spiritual / Warped / TzHaar / Waterfiend / Kurask / Gargoyle (variants-misc.md)
- Zamorak spiritual creatures + all TzHaar: all-zero melee bonuses → MAGIC chosen via elemental weakness, but melee is equally valid (tie).
- King kurask: cannon-immunity not on its own infobox (assumed from the kurask line; kurask are cannon-immune everywhere).
- Warped Terrorbird: no elemental weakness listed.

## Boss pool (variants-boss.md)
SUPERSEDES the old "intentionally not enumerated" note — Boss IS now in scope (MV-R2); all 32 assignable bosses
profiled in variants-boss.md. Residual unfindable/ambiguous values:

- **Barrows brothers — undead correction (Architect call):** all six infoboxes have `attributes = spectral`,
  NOT `undead` (verified directly on Ahrim + Dharok raw pages). Per the team rule, they are flagged categories=[]
  (no undead). This contradicts BOTH the earlier variants-boss.md table AND the common assumption that Salve works
  on Barrows in-game. NEEDS A DECISION: do we flag undead by the strict wiki `attributes` field (→ no undead for
  Barrows, no Salve credit) or by actual Salve-eligibility? (The spectres/Ankou undead rule DID have `undead` in
  attributes.) Recommend: keep strict (categories=[]) unless QA confirms Salve fires on Barrows live.
- **DT2 bosses — ranged defence UNKNOWN:** Duke Sucellus, The Leviathan, Vardorvis, The Whisperer all lack a
  `drange` field on the infobox (stab/slash/crush/magic present). Recorded range=UNKNOWN. Weakness derived from
  the present bonuses (all melee except Whisperer=magic). Resolve via live NPCDefinition if range matters.
- **Defence level UNKNOWN:** Araxxor and Kraken have no defence_level on their infobox (def bonuses present).
- **All-tied / no-lean defences:** Commander Zilyana (all five tied at 100, no element → weakness UNKNOWN, style
  is accuracy-driven); Grotesque Guardians Dusk (all 0, element Earth 40% breaks the tie); Chaos Elemental (all 70,
  element Air 50% breaks the tie). Recorded as such, nothing invented.
- **Multi-form bosses** (Abyssal Sire, Vorkath, Zulrah, Alchemical Hydra, Kalphite Queen, Phantom Muspah,
  Grotesque Guardians, Dagannoth Kings, the DT2 quest/post-quest/Awakened tiers): per-form def_bonuses recorded
  where the infobox splits them; the loadout must pick the form/tier relevant to the player's fight.
- **Kalphite Queen per-form split:** infobox uses paired dstab1/dstab2 etc.; collapsed to the dominant pair in the
  profile. Verify exact per-form split live if a form-specific loadout is wanted.
- **Zulrah / Dagannoth / Barrows ranged defence** comes from split dlight/dstandard/dheavy (identical per NPC) —
  collapsed to a single `range` value. Format note, not a gap.
- **Vorkath is dragon AND undead** (attributes=dragon,undead,fiery) — both dragonbane and Salve apply. Confirmed,
  not a gap; flagged so the category model expects two flags on one boss.
- **GWD bosses** (Zilyana/Graardor/Kree'arra/K'ril) need 40 boss-faction kill count + GWD access (Death Plateau +
  a 70 stat), NOT a Slayer level — recorded in the profile requirement field.
- **Kree'arra / Dawn flying:** numerically melee-weak by defence but flying — practical weakness is ranged/magic
  (melee needs a halberd-type/special weapon). Recorded both the defensive-lowest style and the practical note.

## Humanoids — Trolls / Elves / Lizardmen / Suqahs / Cave horrors (variants-humanoids.md)
- No monster in these tasks declares an `attributes` field on its infobox → all `categories` are empty (incl.
  reanimated, which the wiki does not tag undead). Confirmed, not a miss.
- Ice troll grunt: infobox combat 100 vs body text 102 → used infobox 100.
- King kurask cannon-immunity (also relevant here) noted in misc.
- Out-of-scope troll filler (Thrower trolls, Stick/Rock/Twig/Berry trolls, River troll, etc.) intentionally NOT
  enumerated — not on the Duradel "Alternative(s)" named list. Flag for Architect if full troll coverage is wanted.

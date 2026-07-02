# Strategy-Guide gear override (wiki /Strategies weapon beats the DPS pick)

Status: accepted (extends ADR-0008/0009 weapon ranking; composes with ADR-0010 MonsterProfile, ADR-0013
combat-method axis, ADR-0014 Boss task). Board MV-SA.

## Context

The pure-DPS weapon ranking (ADR-0008/0009) picks the highest sustained-DPS owned weapon for the effective
style. Live testing exposed two gaps the DPS model cannot express:

1. **It recommends a Fang on demon bosses** (Tormented Demon, Skotizo) where the wiki `/Strategies` page
   prescribes **Emberlight** (demonbane). On a demon the demonbane conditional *is* credited by the engine,
   but the model still mis-ranks where the wiki's curated answer reflects mechanics we do not model (spec
   weapons, defence reduction, per-phase behaviour, accuracy floors).
2. **It cannot express a melee + ranged mix.** Tormented Demon's wiki strategy is melee primary
   (Emberlight) **plus** a Scorching bow for the shield-down ranged window. The engine emits exactly one
   loadout for one style; the secondary weapon has nowhere to go.

The user decided: **when a monster has a wiki `/Strategies` page, the guide's gear OVERRIDES the computed
weapon/method.** This ADR records the override layer.

## Decision

Add an optional per-variant **strategy** that, when present and applicable, makes the guide's primary
weapon win the WEAPON slot, bypassing the DPS weapon ranking, while the stat engine keeps filling every
other slot from owned items. Five load-bearing choices:

1. **Strategy overrides the WEAPON slot only; the stat engine fills the rest.** A strategy supplies the
   weapon (and the default method); armour, food, potions, runes, and the within-slot details remain
   stat-driven from owned items (ADR-0001). Where the guide is silent on a slot, the normal DPS pick stands.
   This is the minimum change that fixes the Fang-vs-Emberlight case without re-authoring the whole engine.

2. **Bank-aware: recommend the strategy weapon only if OWNED.** The override searches the strategy's
   priority-ordered `primaryWeapons` and forces the **highest-priority OWNED-and-viable** one into the
   WEAPON slot. If the player owns none of them, it falls back to the stat-driven best owned weapon. We
   never recommend gear the player lacks (preserves the owned-only discipline, ADR-0001).

3. **Multi-style = the strategy drives every style it documents; the note surfaces them all.** The loadout
   recommends the strategy's weapon for **whichever documented style is the effective method** (see decision
   4). The "Wiki strategy" **note line** (mirrors the FR-7 boss-note pattern) lists the primary AND every
   secondary weapon with its style, so the user sees the full mix regardless of which method is active. One
   loadout, one card, plus the guidance line - no second simultaneous loadout.

4. **AMENDED (user decision, supersedes the original "primary only" gate): the override drives EVERY style
   the strategy documents, not just the primary.** Build a `style -> strategyWeapons` map as the union of
   {`primaryStyle` -> `primaryWeapons`} and {each `secondaryWeapon` -> its own `style`}. The override applies
   when the variant has a strategy AND the **effective (selected) method** is a style the strategy documents
   AND the player **owns** a strategy weapon for that style; then the highest-priority OWNED weapon for that
   style wins the WEAPON slot (bypassing the DPS ranking). A strategy sets the DEFAULT method to its
   `primaryStyle`, **derived at advise-time in `LoadoutAdvisor` (`defaultMethod = validStrategy != null ?
   strategy.primaryStyle : recommendedStyle`), NOT by requiring the authored `weakness.style` to match.** So a
   guide whose style differs from the plugin's authored weakness (the ~7 cases: Abyssal Sire magic->melee
   Emberlight, Vorkath ->ranged DHCB, Callisto/Chaos Elemental magic->ranged, Crazy archaeologist ->magic,
   Kree'arra ->magic/chins, GG-Dawn melee-immune->ranged, Barrows Dharok/Guthan/Torag ->magic) defaults to the
   GUIDE's style automatically - the strategy is authoritative ("strategy overrides method"); the authored
   `weakness.style` is left untouched. If the effective style is **NOT documented** by the strategy (e.g.
   magic on a melee+ranged demon strategy), OR no documented weapon for it is owned, the override switches
   off and the stat engine drives that style normally. Example (Tormented Demon = Emberlight MELEE primary +
   Scorching bow RANGED secondary): Method=Melee -> Emberlight; Method=Ranged -> Scorching bow; Method=Magic
   (undocumented) -> stat engine. The "Wiki strategy" note stays visible as guidance regardless. (The
   original ADR gated strictly on `effectiveStyle == primaryStyle`, surfacing secondaries as note-only; that
   is now generalised so a documented secondary style is also overridden.)

5. **The override seam lives in `GearSelector`, gated by `LoadoutAdvisor`.** The advisor decides whether the
   strategy *applies* (variant has a strategy AND `effectiveStyle == primaryStyle`) - it owns the variant,
   the strategy, and the effective style. It passes a priority-ordered `List<Integer>` of strategy weapon ids
   (empty = no override) into `GearSelector.select`. The selector owns the *mechanism*: pick the
   highest-priority owned-and-viable id from that list for the WEAPON slot, reusing the existing 2h-vs-shield
   interplay (a 2h strategy weapon drops the shield; a 1h keeps the stat-picked best shield). Policy in the
   advisor, mechanism in the selector - the same split as every other engine input.

**The chosen weapon still gets its conditional bonus in the DPS display, for free.** The override only
substitutes the WEAPON id in the worn map. `DefaultDpsEstimator.estimate` already credits the worn weapon's
`WeaponEffect` and the max-applicable `ConditionalBonus` over equipped items (WDB-8 / LFB-5), so Emberlight
worn on a demon variant displays its demonbane-credited DPS with no extra wiring.

## Data shape

A nested optional `MonsterVariant.strategy` (NOT a parallel dataset - locality beats a join key; the
resolved variant already carries it):

```
MonsterStrategy {
  CombatStyle          primaryStyle;       // the default method this strategy sets
  List<StrategyWeapon> primaryWeapons;     // priority order; highest-priority OWNED one wins the slot
  List<StrategyWeapon> secondaryWeapons;   // NOTE only (name + style); never auto-equipped
  String               note;               // free-text "Wiki strategy" guidance line (nullable)
  String               sourceUrl;          // the /Strategies page (provenance)
}
StrategyWeapon { String name; Integer itemId; CombatStyle style; }   // style nullable on primaries
```

**Name -> id is resolved at AUTHORING time** from `docs/loadout/weapon-reference-1h.md` / `-2h.md` (the
same name->id source the prior weapon work pinned ids from) and baked into `slayer-data.json` as raw ints,
matched against `owned.ids()`. No runtime ItemManager name lookup - consistent with how
`WeaponEffectRegistry`/`ConditionalBonusRegistry` key by raw id. Charge/recolour id variants that a player
might own are covered by listing them as additional priority entries (we do NOT canonicalise via
`ItemVariationMapping` - LFB proved that collapse erases meaningful state for some items).

## Fallbacks & honesty

- **No `/Strategies` page** (variant has no `strategy`) -> stat engine, byte-identical to today. The vast
  majority of variants.
- **Strategy present, a DOCUMENTED style active, >=1 weapon for that style owned** -> the highest-priority
  owned weapon for that style wins the WEAPON slot (amended: documented = the primary style OR any secondary
  weapon's style).
- **Strategy present, NO documented weapon for the active style owned** -> stat-driven best owned weapon
  (bank-aware fallback).
- **Method is an UNDOCUMENTED style** (not the primary and no secondary weapon for it) -> no weapon override;
  stat engine for the chosen style.
- **Malformed strategy** (null `primaryStyle`, empty `primaryWeapons`) -> treated as no strategy, stat
  engine, `log.warn` once. No fabrication, no silent wrong answer.
- The **"Wiki strategy" note** renders whenever a strategy exists (informational), listing primary +
  secondary weapons + the free-text note, EVEN when the player owns neither - so the user learns the wiki's
  answer without the loadout ever recommending unowned gear.

## Consequences

- One new model type tree (`MonsterStrategy`/`StrategyWeapon`), one new `MonsterVariant.strategy` field, one
  new `GearSelector.select` overload (priority-ordered strategy ids; existing overloads delegate empty), one
  composed `Recommendation.strategyNote` string, and a new `loadout-strategy-note` panel row. The combat
  maths is untouched (NG-4).
- Variants without a strategy are provably unchanged (empty override list = today's path) - the FR-6
  regression anchor extends cleanly.
- The override is unconditional once owned + style matches: it does NOT compare DPS, by design (the Fang may
  out-DPS Emberlight in the raw model; the wiki answer wins anyway). The DPS *display* still reflects the
  chosen weapon honestly.
- Rejected: (a) re-encoding spec-weapon / defence-reduction mechanics into the DPS model (large, NG-4); (c) a
  parallel strategy dataset keyed by name (a join key to keep in sync vs nested locality).
- **Originally rejected, now ACCEPTED via the decision-4 amendment:** (b) overriding the secondary weapon when
  the method toggle selects its style. This is no longer a "second override path" - it is the same single
  override mechanism reading a `style -> weapons` map; the only state is the existing method selector. The
  note still conveys the full mix so the user sees it without toggling.

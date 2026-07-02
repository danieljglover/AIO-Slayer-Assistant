---
status: accepted
supersedes-stance-in: 0010
reaffirms: 0012
relates-to: 0011, 0014
---

# Combat method is a user-selected axis; styles preserved as the default; element wiki-accurate

## Context

ADR-0010 made the task-level `weakness` the default variant's profile and treated "the engine picks the
style" as fixed. At Gate 2 the human first overturned "preserve byte-identical", then - after a
clarification round - landed on a different and better resolution: **the player should not be locked to
one style at all.** The plugin should OFFER melee / ranged / magic as selectable methods for the chosen
monster, defaulting to its recommended (efficient) style and re-driving the loadout for whichever method
the user picks.

This dissolves the "preserve vs flip the wiki style" argument that ADR-0012.8 / decision #11 wrestled
with. The `Weakness` type's two axes are still real and still independent:

- `style` (MELEE/RANGED/MAGIC) = which style to BRING; today derived from the monster's defence /
  convention; it is the RECOMMENDED style.
- `element` (water/earth/fire/air) = the wiki "weakness icon"; a MAGIC-ONLY spell selector consumed only
  when the active method is magic; magnitude not modelled (NG-4).

"Greater demons weak to water" does NOT mean magic is their best style - you melee them, but if a player
chooses the magic method, the water element selects the right spell. Flipping a task's recommended STYLE
to its elemental icon would be a category error that worsens the default loadout - so styles are NOT
flipped.

## Decision

**Combat method becomes a USER-SELECTED input that overrides the engine's style choice; the default
method is the variant's recommended style (today's curated `weakness.style`); `element` is refined to
wiki-accurate so the magic method picks the right spell.**

1. **Method is a selected input, not a fixed engine field.** For the selected variant the user picks a
   method (melee / ranged / magic); `LoadoutAdvisor` resolves the EFFECTIVE style =
   `selectedMethod ?? recommendedStyle` and drives `GearSelector` / `DefaultDpsEstimator` with it,
   overriding the defence/convention-derived style. Within-style picking is unchanged: `monsterDefence`'s
   lowest of stab/slash/crush still selects the melee attack type; `element` narrows the spell only when
   the active method is magic; category flags still gate the bane bonuses. No combat-maths change (NG-4).

2. **Default method = the variant's recommended style.** For the 42 existing tasks this is today's curated
   `weakness.style`, so a fresh task / default variant / default method reproduces today's loadout - **FR-6
   is preserved BY DEFAULT** (byte-identical at the default method; it changes only if the user switches
   method). New variants/bosses carry their OWN correct recommended style.

3. **Styles are PRESERVED on existing tasks** (reaffirming ADR-0012.8 / decision #11 - the
   style-pinning guard test stays, pinning the existing curated styles). What this ADR changes vs ADR-0010
   is only that the user may now OVERRIDE the style via the method selector; the stored recommended style
   does not flip.

4. **`element` refined to wiki-accurate, per variant.** So the magic method picks the correct spell. This
   is mostly inert at the default method (style != MAGIC) except for tasks whose recommended style is
   already MAGIC, where it is a deliberate small improvement (right spell) - guarded by a test. A variant
   with no wiki elemental weakness keeps the existing sensible fallback (air / best unresisted), so the
   magic method always works.

5. **`MonsterProfile` = defence + category flags + element + recommended (default) style.** The
   STYLE-TO-USE comes from the method selector, threaded through the advisor exactly as
   `selectedVariantName` / `selectedLocationName` are. `monsterDefence` and the existing 42 task profiles
   are NOT re-authored (the earlier "re-author all to wiki" framing is a REJECTED alternative below).
   Concretely the seam is: `LoadoutAdvisor` computes `effectiveStyle = selectedMethod ?? recommendedStyle`
   and passes it into `GearSelector.select(..., effectiveStyle, ...)` (replacing its internal
   `styleOf(task)` derivation) and into `DefaultDpsEstimator.estimate(effectiveStyle, ...)` (which already
   takes the style as an explicit parameter, so no estimator signature change). `meleeAttackType`
   (argmin of `monsterDefence.{stab,slash,crush}`) and `element` (spell pick) stay as within-style
   refinements unchanged.

6. **Offer all three methods; disable a method with no viable owned weapon (owned-driven, honest).**
   `GearSelector.select` already returns an empty worn map when no weapon is owned for the style, so
   viability is a FREE signal - try the selection for a method, and if it is empty the method is offered
   but disabled (greyed), never silently hidden. We do NOT build a monster-resistance / immunity table to
   pre-hide "non-viable" methods: that is new fabricated data (NG-4 / R-1 discipline) and would mislead
   (a 600-magic-defence monster is still legally magic-able, just bad). Monster resistance is surfaced
   honestly via the Est. DPS number for the chosen method, not by removing the choice. (hide-vs-grey of a
   no-gear method is a minor FE detail; grey is recommended so the option stays visible.)

7. **Method composes orthogonally with variant and boss selection.** The method applies to WHATEVER
   variant is selected, including a boss variant (ADR-0014). The default method follows the SELECTED
   variant's `recommendedStyle`, so on a task/variant change the method RESETS to the new variant's
   recommended style (switching from a melee base monster to a magic-weak boss re-defaults to magic).
   Boss variants carry their own `recommendedStyle` from MV-R2; no boss-specific method logic is needed.

## Considered options

- **Flip every existing task's style to the wiki elemental icon** (the first overturn framing). REJECTED:
  a category error (elemental weakness != best combat style) that worsens default loadouts; conflates the
  two `Weakness` axes ADR-0012.8 separated.
- **Preserve-only (lock one curated style per monster).** REJECTED by the user: they want the alternatives
  offered, not hidden.
- **Selected combat method (offer styles as options), default = recommended style.** CHOSEN: gives the
  user the alternatives, keeps the efficient default, and makes FR-6 byte-identical at the default.

## Consequences

- **FR-6 re-framed:** at the DEFAULT method a single/default-variant task reproduces today's loadout
  (byte-identical preserved); switching method re-drives the loadout for that style. The migrated baseline
  suite stays green at the default (no baseline-assertion churn); new tests cover method override and the
  element refinement.
- A new selection axis (method) joins variant + location: `selectedMethod` threaded through the advisor,
  `MonsterVariant`/`MonsterProfile` expose the recommended style, a third UI control, and `GearSelector`
  takes an explicit effective-style input instead of deriving it.
- A method that yields no viable loadout (no owned weapon for that style) is disabled/hidden
  (owned-driven, honest); monster resistance is surfaced via Est. DPS, not by fabricating an immunity
  table.
</content>

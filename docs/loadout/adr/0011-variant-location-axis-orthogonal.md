---
status: accepted
relates-to: 0009, 0010
---

# Variant and location are orthogonal axes in v1

## Context

The side panel already has a location combo (`where-location-combo` / `onSelectLocation`) threading a
`selectedLocationName` end to end; WDB-17 (ADR-0009) already derives a per-LOCATION `wilderness` predicate
from it (Viggora's chainmace on Ankou-Wilderness). The variant feature adds a second selection axis. The
MV-R1 enumeration (`docs/variants/`) shows the two axes overlap: many "variants" are split purely by
location (e.g. `Abyssal demon (Standard)` vs `(Catacombs of Kourend)` vs `(Wilderness Slayer Cave)`),
often with an identical defence/weakness profile. OQ-1 asks: orthogonal, nested, or does variant subsume
location?

## Decision

**Orthogonal in v1, with de-duplication at data-authoring time.**

- The **variant axis** owns the monster PROFILE: `{weakness, defence, category flags}`. Its entries are
  the *profile-distinct* monsters - base monster, superiors, bosses, and leveled variants whose defence
  differs materially. Pure location-renames that share the base profile are **collapsed** into the base
  variant; they do not appear as separate variant entries (keeps the combo short and meaningful - R-6).
- The **existing location axis** is UNCHANGED: it owns *where* (multi / cannon / burst / konar /
  wilderness), and the `wilderness` predicate keeps flowing exactly as WDB-17 built it.
- A location-bound variant (e.g. a GWD boss) carries a free-text `location` label surfaced as a NOTE
  (FR-7), not wired into the rich `SlayerLocation` combo for v1.

The two selectors are independent: variant = *which monster* (drives the loadout profile), location =
*where you fight it* (drives cannon/burst/wilderness hints). A boss has one inherent place, shown as a
note rather than as a competing location entry.

## Considered options

- **Nested / variant-subsumes-location** (each variant carries its own rich `SlayerLocation` set).
  Rejected for v1: the enumerated per-variant locations are free text with no cannon/burst/multi flags, so
  this needs a large, low-value migration of all location data under variants. It is the documented future
  evolution once rich per-variant locations are wanted.
- **Keep every enumerated location-split as a selectable variant.** Rejected: same-profile entries produce
  an identical loadout - pure UI noise that bloats the combo (R-6) with no behavioural difference.

## Consequences

- Zero re-key of the working location / wilderness machinery (R-3/R-5 safe; WDB-17 preserved untouched).
- The variant combo stays short (the ~215 enumerated blocks collapse to ~90-110 profile-distinct
  variants), which is what makes "all variants incl. bosses" usable.
- Cost: a location-bound boss still shows the task's generic location combo (mostly moot for a boss trip),
  and the variant's free-text location is a note, not a rich location, in v1.
</content>

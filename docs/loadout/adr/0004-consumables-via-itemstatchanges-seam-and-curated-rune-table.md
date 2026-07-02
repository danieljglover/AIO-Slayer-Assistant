---
status: accepted
---

# Food/potions classified via a `ConsumableEffectsProvider` seam over RuneLite's `ItemStatChangesService`; runes from a curated element->tier table

Food heals and potion boosts are read from RuneLite's
`net.runelite.client.plugins.itemstats.ItemStatChangesService` (`getItemStatChanges(id)` ->
`Effect.calculate(client)` -> `StatChange[]`), not a hand-curated heal/boost table. This is
level-aware (anglerfish, Saradomin brew) and auto-handles new consumables. We wrap it behind our own
small seam `ConsumableEffectsProvider` (`Integer healAmount(id)`, `Set<BoostedStat> boostedStats(id)`)
- mirroring the project's `EquipmentStatsProvider` / `ItemIconRenderer` seam idiom - so the consumable
selector is headless-testable with a fake and never touches the live service or `Client` in a test.

- **Food:** read the `HITPOINTS` `StatChange.getTheoretical()`; recommend the highest-heal owned food;
  surface Cooked karambwan (curated id) as a combo-eat when owned.
- **Potions:** classify by which combat stats are positively boosted (ATTACK/STRENGTH/DEFENCE -> melee,
  RANGED -> ranged, MAGIC -> magic); pick the best owned potion for the task style by **boost
  magnitude** (no curated "best potion" list); collapse dose variants with `ItemVariationMapping`.

**Runes are different - they are curated**, because the live API does not give spell rune costs:
a small in-repo table maps element -> spell tier (Bolt/Blast/Wave/Surge) -> {rune id -> count, Magic
level required, base max hit}. The selector picks the highest tier castable given Magic level and owned
rune counts (inventory + worn + last-seen bank; rune pouch deferred, PRD NG-4). A curated
elemental/combination-staff id->element map removes that element's rune from the requirement; a curated
powered-staff id set means "no runes required" (the weapon supplies its own attack).

Why a seam over the service rather than calling it inline, and why curated runes: the service is the
authority for *effects* but (a) is a core-plugin singleton whose injectability is unverified (see
**Risk R1 / spike LD00** - fallback is a curated `id->heal`/`id->boosts` map), and (b) carries no spell
rune costs at all. So effects come from the API behind a fakeable seam; rune costs come from a curated
table we own. `calculate(client)` reads live levels -> client-thread only (PRD FR-11).

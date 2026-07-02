# DT-GAP — Dev-Testing Readiness Synthesis (2026-07-01)

Project-lead synthesis of `data-audit.md` (DT-CTX-DATA) + `code-flow-audit.md` (DT-CTX-CODE)
against the goal: for every Duradel task, suggest (1) monster variant, (2) location BY variant,
(3) loadout from variant + location variables + strategy; bank-scan-gated (scan persisted, user told
to open the bank first). Baseline: 629 tests green, source referential integrity clean.

## Readiness by goal pillar

| Pillar | State | Blocking gaps |
|---|---|---|
| A. All Duradel monsters plugged in | **~DONE** — Duradel assigns all 43 tasks; 228/228 runtime variants carry a strategy with plugin weapons; exactly one isDefault per task | GAP-4 (5 variants null weakness/defence), GAP-3 (boss meta-task no real location), G9 (Frost Dragons synthetic varp — QA/live item) |
| B. Suggest variant first | **DONE** — isDefault pre-select + default method = strategy.primaryStyle | G7 panel ordering polish only |
| C. Location by variant | **BLOCKED — the feature of this engagement.** Runtime has NO variant→location link; compiler drops variant.locationId (63 FKs), variantInfo, locationComparison; chooseLocation + panel combo are variant-agnostic | GAP-1/G2 (core), GAP-2/G10 (safeSpot/accessNote/cannonable/multi dropped), GAP-5 (broken FKs must be fixed + validated if locationComparison becomes compile input), GAP-6 (variantInfo coverage) |
| D. Loadout from variant+location variables | **PARTIAL** — variant variables + strategy override are strong; location side thin | G3 (only selected-location wilderness feeds loadout; suggested-location traits ignored; cannon never adds supplies), G6 (inventory always empty — no cannonballs/antifire/teleports/required item), G1 (requiredItemId compiler regression — ACTIVE, cheap fix) |
| E. Bank scan + gate messaging | **~DONE** — bank-open scan persists across sessions; gate text exists ("Open your bank once so All-In Slayer can read your gear.") | G4 delta: a literal login scan is INFEASIBLE (client can't read an unopened bank). Realistic completion = on-login check: no snapshot → surface the open-your-bank prompt; stale snapshot → show age. |

## Disposition

**To DESIGN (DT-A1, architect — one coherent feature):** GAP-1/G2 + GAP-2/G10 + G3 + G6 + GAP-3 +
G7 + G4-staleness-UX. This amends ADR-0011 (variant×location orthogonality) and needs: compiler
emission of per-variant location links w/ full location flags, a per-variant location-suggestion rule,
loadout coupling to the suggested/selected location's traits (wilderness, cannonable→cannonballs,
safespot→method hint), inventory supplies, validator extension (locationComparison FKs), and panel
ordering variant→location→loadout.

**Direct build now (no design needed, independent of DT-A1):**
- DT-B1: G1 requiredItemId compiler fix (regression; consumers already exist).
- DT-D1: data authoring — GAP-4 (5 null profiles), GAP-5 (9 spiritual-creatures FK ids), GAP-9
  (12 location files missing safeSpot/wilderness/accessNote).

**Documented, not built (QA/live items):** G5 bossId varbit mapping (needs live capture), G9 Frost
Dragons real varp (needs live capture), GAP-7 (9 legacy .md strategies — maintainability, works today),
GAP-8 (orphan-file housekeeping — fold into DT-D1 only if trivial).

**Feasibility ruling:** "scanned on login" is interpreted as: persisted snapshot loaded + login-time
staleness/absence messaging, since the OSRS client cannot read bank contents until the bank is opened.
The existing bank-open scan + persistence already provides the scan; the delta is the login-time UX.

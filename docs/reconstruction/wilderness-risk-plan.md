# Wilderness death risk implementation plan

User-approved scope: calculate Wilderness setup losses, distinguish protected
items, lost items, repair fees and permanent untradeable loss, and use that risk
when selecting owned loadouts. Three protected items is the normal default;
Protect Item is a conditional comparison. The June 2026 Trouver rework applies.

## Constraints

- Java 11, passive client-thread reads, immutable worker inputs, Swing EDT output.
- No runtime Wiki fetching, game interaction, automated tests or new dependencies.
- Preserve the current worktree and existing Slayer/Salve, eligibility, set and
  ammunition constraints. Compile and validate source JSON before completion.

## Work

- [x] Compile a reviewed item-ID death-rule map and location risk profiles from
  `advisor/death-rules.json` and `advisor/wilderness-risk.json`. Record current
  MediaWiki page IDs, revisions and timestamps in a source review document.
  Rules distinguish DROP, KEEP, REPAIR, LOCKABLE, POUCH, ALWAYS_LOST, CONVERT and
  UNKNOWN. Preserve exact locked and ornamented IDs; never infer locks from an
  equipment family. Unknown Wilderness depth uses the above-20 scenario.
- [x] Capture prices, death valuations, actual carried items (including notes),
  skull, world restrictions, account type and observable container contents.
  Unsupported charges and unknown item behavior remain explicit unknowns.
- [x] Add a pure calculator for all four skull/prayer scenarios, high-risk and
  Ultimate Ironman restrictions, item quantities, fixed repairs, transformations,
  contained items, entry fees, and a separate actual-carried comparison. Compute
  protection per item, not per inventory stack. Unknown data cannot prove a
  setup within budget or safe from permanent loss.
- [x] Add a configurable loss budget (default 500,000 gp) and conservative
  Wilderness equipment selection. Keep source combat constraints, prefer owned
  alternatives within budget, reject permanent loss, and explain when no complete
  setup meets the budget. Include supplies and switches in the final assessment.
- [x] Show planned and carried risk, all protection scenarios, per-item outcomes,
  conditional fourth item, valuation and charge limitations in the plugin panel.
- [x] Review calculation boundaries and integration, run JSON validation and
  `./gradlew generateSlayerData` / `./gradlew build`, then reopen the development
  client and inspect only the plugin panel. Record manual checks and limitations.

## Shared interfaces

`SlayerCatalogue.deathRules: Map<Integer, DeathRule>` and
`wildernessAreas: Map<String, WildernessArea>` are immutable-by-convention bundled
data. `DeathRule` uses kind, locked, protectable (default true), protectionValue
(default -1), repairCost, replacementItems, contentsLost, and evidence. A positive
protection value overrides cached valuation. `WildernessArea` uses minLevel,
maxLevel, wildernessTravel, pvmUsesPvpRules, entryFee and evidence.

`PlayerSnapshot` adds `itemValues: Map<Integer, ItemValue>`,
`riskCarried: Map<Integer, Integer>` and `deathContext: DeathContext`.
`ItemValue` contains id, name, marketPrice (-1 unknown), protectionValue (-1
unknown), tradeable, stackable, chargesUnknown. `DeathContext` contains known,
skulled, protectItemActive, highRisk, ultimateIronman, wildernessLevel,
pouchContents, lootingBagContents, pouchContentsKnown and lootingBagContentsKnown.
All capture models are immutable and included in snapshot equality.

The calculator consumes these facts with a selected location and item quantities,
produces immutable scenario and item outcomes, and never calls RuneLite APIs.

Ruling: the user authorized implementation. No additional plan approval is needed.
The 500,000 gp budget is an editable product default, not an assumed account
preference. Permanent untradeable loss is always excluded from automatic picks.

Verification details and the pending owner account check are recorded in
`wilderness-risk-implementation.md`. Source uncertainty is explicit in the
calculator and blocks automatic readiness, rather than guessing unsupported
charge balances or item transformations.

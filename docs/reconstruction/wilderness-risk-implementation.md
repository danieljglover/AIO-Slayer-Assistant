# Wilderness risk runtime and verification

## Runtime

`WildernessRiskCalculator` is a pure worker-side calculation. It consumes the
bundled exact-ID `DeathRule` map, a destination `WildernessArea`, and immutable
`PlayerSnapshot` values. It assesses the proposed equipment/inventory and actual
carried inventory separately. Bank notes are excluded from usable equipment but
included in carried exposure. Bank ownership itself is never at risk.

Normal protection allocates three individual items unskulled or zero skulled,
with separate conditional Protect Item comparisons. Each protected unit consumes
one slot; a stack is not one protected item. High-risk worlds suppress normal
protection. UIM suppresses normal protection and remains explicitly incomplete
because its remaining exceptions have not been fully verified. Unsupported
special worlds also remain incomplete. Equal-value protection boundary ties are
explicitly uncertain rather than falsely assigning a guaranteed kept item.

Replacement loss, repair costs and entry deposits are distinct totals.
Permanent untradeable loss is counted separately from gp. Component mappings can
price converted or destroyed parts, variable repair materials, and independently
lost imbues or minimum activation charges. Rune pouches are assessed once using
their replacement note, not both the pouch quote and replacement component.
Locked pouches retain the pouch without inventing a mangled repair charge.

`DeathStateCapture` reads on the client thread. It preserves exact item IDs and
uses RuneLite's existing ItemManager cache: `getItemPrice` for replacement
estimates and `getItemPriceWithSource(id, false)` for official protection prices,
with cache alchemy values only when there is no effective GE price. Reviewed
protection overrides take precedence (for example Avernic defender: 600,000 gp).
No custom HTTP request or game action is performed.

Rune pouch contents use the six transmitted rune slots and RUNEPOUCH_RUNE enum.
Looting bag contents are known only while the contents interface is visible.
Quiver ammunition is read only for a worn compatible quiver, through the same
varps used by RuneLite's AmmoPlugin and item parameter 1910. Its protection rule
remains explicitly uncertain, even when its quantity is observable. Unobserved
stored charges and unsupported container contents never silently become zero.

The client tick compares a lightweight death-state fingerprint, including skull,
prayer, world restrictions, depth and bag visibility. Item/container/varbit
updates also invalidate the snapshot. Immutable values participate in request
equality. New account or panel state cancels stale worker calculations without
publishing stale results or creating an error notification.

## Selection

The default configurable budget is 500,000 gp. The planner keeps the three-item
unskulled baseline independent of Protect Item's observed state; a currently
skulled account instead uses the zero-item baseline. Automatic readiness requires
complete valuation, no permanent untradeable loss and a total within budget.
Unknown-depth areas use above-20 rules; KBD's Wilderness approach is an exposure
even though its chamber is outside Wilderness.

Equipment selection compares compatible owned alternatives and optional empty
slots, retaining required protection, method set dependencies and ammunition.
Whole-loadout Slayer/Salve scoring applies one eligible non-stacking bonus.
Supplies are included in gear comparisons. The final supply pass rebuilds full
inventories to reconsider an earlier source alternative after later supplies
change trip cost or protected items. It preserves fulfillment and visible potion
doses, including fallback food/prayer supplies and switch ammunition.

The search is heuristic and bounded: at most 600 gear comparisons per setup,
3,000 shared comparisons per request, and 32 alternate full-inventory attempts
per final supply plan within that shared budget. Exhaustion is disclosed and
never bypasses final risk assessment. Narrowing the selected monster/location
focuses the search. These limits do not prove a globally optimal combination.

The native panel shows planned/carried summaries, budget status, skull/prayer
scenarios, protected quantities, per-unit protection values, item outcomes,
replacement and repair amounts, entry deposits, and uncertainty. The carried
comparison describes the selected destination/route, not the player's current
tile. Risk source revisions are included with the setup's Wiki evidence.

## Verification (2026-09-08)

- `./gradlew build` passed after all code changes (`/tmp/aio-wilderness-final-build.log`).
- Source JSON validation passes across all Slayer JSON files.
- `generateSlayerData` passed with 483 exact death-rule IDs in 214 groups,
  57 location profiles and 18 explicitly unknown depth profiles.
- Independent code review found and verified fixes for pouch double valuation,
  omitted quiver contents, optional-slot removal, KBD travel filtering,
  completed-trip supply alternatives, and request-wide search/cancellation.
- The development client starts with the plugin loaded. Native panel interaction
  verified Skeletons -> Calvar'ion -> Skeletal Tomb, the 500,000 gp budget,
  incomplete logged-out estimates, a 50,000 gp maximum entry deposit, and
  expandable death scenarios. No game interaction was performed.
- Account-backed gear prices, live container changes, skull changes and rendered
  per-item losses require the owner to log in/open the bank. The client was at
  Play Now during this verification; no account behavior is claimed as tested.
- No automated tests, test sources or dependencies were added, per AGENTS.md.

See `wilderness-risk-sources.md` for pinned current Wiki evidence and unresolved
mechanics. The in-game Items Kept on Death interface remains authoritative.

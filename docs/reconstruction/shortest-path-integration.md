# Shortest Path integration

## Implementation plan

1. Compile bundled, evidenced destinations for exact monster/location pairs.
   Prefer Wiki spawn coordinates; label entrance destinations for encounters
   whose interior is instanced or not represented in Shortest Path's graph.
   Ambiguous destinations remain unavailable rather than using guessed tiles.
2. Use Quest Helper's RuneLite PluginMessage handoff: shortestpath/path with
   WorldPoint start and target (or a set of targets), and shortestpath/clear.
   Route-specific configuration considers owned inventory/bank teleports and
   the bank collection path, retains unlock checks, and respects Wilderness
   consent. Saved Shortest Path settings are not edited.
3. Add Route/Clear controls beside the selected location. Show absent-plugin,
   login, missing-coordinate, entrance, and bank-capture states concisely.
   Explain bank pickup and external route limits in Guide, not the main layout.
4. Check current upstream protocol, compile/validate source references, review
   changes, and manually inspect the development plugin panel. No automated
   tests or actual game interaction.

## Source revisions

- Shortest Path: 6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a.
- Quest Helper: 633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a.
- Research checkouts: /tmp/aio-shortest-path-source and
  /tmp/aio-quest-helper-source; source Wiki probe:
  /tmp/aio-route-wiki-probe.json.

## Execution notes

- Ruling: use the existing feature worktree and idle agents because the ongoing
  reconstruction is uncommitted and all four agent slots already exist. Review
  scoped before/after artifacts; do not commit or revert unrelated work.
- Ruling: routing displays travel guidance only. Shortest Path owns routing and
  transport requirements; no walking, teleporting, or other game action occurs.
- Ruling: destinations use current Wiki API source evidence. Unknown or generic
  spawn labels need explicit mapping; no fuzzy runtime geocoding or HTTP fetches.

## Destination coverage

See [bundled routing evidence and gaps](routing-sources.md) for the current
verified coverage, coordinate conversions and withheld catalogue bindings.
Entrance destinations explicitly require the player to continue inside. A
shared location arrival does not establish that every nearby spawn supports
the selected cannon, barrage or task-only method; follow the selected guide.

## Handoff and ownership

Route uses the same public `PluginMessage("shortestpath", "path", data)`
mechanism as [Quest Helper](https://github.com/Zoinkwiz/quest-helper/blob/633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a/src/main/java/com/questhelper/steps/DetailedQuestStep.java).
`start` is the player's world position; `target` is a `WorldPoint` or set of
verified spawn points. The external pathfinder chooses the cheapest reachable
candidate under its travel-cost model.

The optional integration has no compile-time Shortest Path dependency. Discovery
checks the active plugin on the EDT; requests and client observations run on the
client thread. Swing receives immutable route-state updates. Public reflected
getters inspect route targets and completed results; API incompatibility cannot
justify clearing an unverified route.

The [upstream message handler](https://github.com/Skretzo/shortest-path/blob/6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a/src/main/java/shortestpath/ShortestPathPlugin.java)
keeps overrides globally, including across config-less requests. AIO tags its
own overrides, releases them before foreign requests, and clears a path only
while its destination lease remains valid. Manual route commands, foreign
messages, destination changes, account changes and plugin shutdown relinquish
that lease. A queued cancellation is generation-guarded. Cleanup-only clear and
config-release messages use the known owner's public handler so they also reach
it after EventBus unregistration; route requests always use the EventBus. Upstream has no
owner-qualified clear protocol, so direct third-party calls that bypass its
messages and menu events cannot be distinguished perfectly.

Saved settings are not changed. AIO temporarily enables regular transport types,
uses `Inventory` or `Inventory and Bank` item mode, and retains Shortest Path's
spending limit, transport costs, configured house facilities and unlock checks.
Bank mode and its collection path require a fresh bank event for the current
account while Shortest Path is active. The AIO persisted bank snapshot alone
cannot authorize use of Shortest Path's separate bank cache.

`Allow Wilderness` gates Wilderness destinations and known Wilderness travel.
The upstream avoid-Wilderness option is also supplied; it permits escape when
already inside the Wilderness. Source destination flags remain necessary since
upstream can accept Wilderness targets even with avoidance enabled.

## Manual verification

Use `./gradlew run` or the existing local Jagex launch helper with Shortest Path
installed from the Plugin Hub. The integration never starts or enables it for
the user. Never place credentials or local session files in evidence artifacts.

Verified with the installed Plugin Hub build on 2026-09-09:

- A logged-in account and a new bank capture enabled Route on a selected setup.
- Aberrant spectre / Stronghold Slayer Cave changed from Calculating route to
  Route ready. Shortest Path displayed pickup of an owned Slayer ring (8) at
  the bank before travel.
- Calvar'ion / Skeletal Tomb reached Route ready (entrance) in the final code
  build. The Skeleton entrance check exposed a non-walkable Wiki scenery pin;
  the authored endpoint was corrected to the pinned transport approach tile.
  After generation and a panel-only plugin reload, the corrected Skeleton route
  reached Route ready (entrance).
- Clear removed that route and restored the Route button. A rapid Route/Clear
  double click also left no route after the upstream callback settled. Switching
  from Catalogue back to Active Task cleared the prior destination.
- Only plugin-panel controls were used. No travel or other game action was
  performed by the agent.

Remaining manual scenarios for the owner include taking the route, comparing
unlocked versus locked shortcuts, account switching and Quest Helper takeover. No automated test files are used in this project.

`./gradlew clean generateSlayerData build compileDevJava` passed. All Slayer
source JSON passed `jq empty`. Existing unresolved strategy extraction warnings
remain separate from routing validation. The final 387-destination snapshot also
passed `./gradlew generateSlayerData build`, and its generated Skeleton endpoint
was verified as 3096,3468,0. No AIO/bridge errors appeared in the live run log.

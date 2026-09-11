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

- Shortest Path: 7e76a5e2cda3da3c772ddd347d75d89eae0139eb (B1 review, 2026-09-11).
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
client thread. Swing receives immutable route-state updates. No reflective
access, class loading, or direct calls into Shortest Path are used. Runtime
plugin discovery uses RuneLite PluginManager and the plugin class name.

The [upstream message handler](https://github.com/Skretzo/shortest-path/blob/7e76a5e2cda3da3c772ddd347d75d89eae0139eb/src/main/java/shortestpath/ShortestPathPlugin.java#L509)
accepts `path` and `clear`. It has no owner-qualified clear, request identifier,
or reached/unreachable notification. Its optional `transports` message contains
transport details without enough information to identify a completed request.
AIO therefore shows **Route sent - see Shortest Path** throughout a handoff.
This confirms submission, not successful calculation or arrival. Shortest Path
shows its own route, including unreachable destinations and bank pickup steps.

AIO holds a local lease for its most recent request. Foreign `path`/`clear`
messages, manual Set Target/Set Start/Find closest/Clear Path menu commands,
and the configured Shortest Path clear hotkey revoke it. The key listener only
observes the existing shortcut; it never consumes or generates input. Exact
message identity distinguishes AIO's own messages from nested foreign messages.
AIO sends `clear` only while its lease remains valid. Selection/account changes
and AIO shutdown respect the same condition, preserving an observed foreign route.

Upstream queues path creation on the client thread. An immediate Clear also
queues a cancellation after that creation; a generation check suppresses it if
another request or manual command intervenes. Repeated AIO Clear calls do not
invalidate that pending cancellation. The bridge remains subscribed until
shutdown cancellation has settled. Lifecycle and other off-thread observations
invalidate the generation immediately, before their client-thread cleanup runs.

Overrides are global upstream. A nonempty config-only `path` replaces the
overrides without changing the destination; a harmless marker restores the
saved defaults. This release runs before a foreign config-less request's queued
path calculation, including requests from Quest Helper's Swing controls. A
foreign request with its own nonempty config already replaces AIO's overrides,
so AIO does not overwrite it with a later release. Generation guards also stop
old release callbacks from changing newer requests.

Shortest Path retains its path and overrides while disabled. AIO remembers its
own uninterrupted lease for that plugin instance and clears it on re-enable
only if no intervening route, menu command, or lifecycle gap invalidates it.
AIO never invokes a disabled plugin's handler. Disabling AIO discards any
suspended lease; it cannot verify ownership during an interval without observers.
If both plugins are disabled before cleanup can reach Shortest Path, upstream
may retain the old route/settings. Use Shortest Path's Clear Path control after
re-enabling to reset them. Retaining AIO's lease across that unobserved interval
would risk erasing a route chosen while AIO was disabled.

This is cooperation through the existing messages and public events, not an
upstream ownership guarantee. Direct third-party calls that bypass those
interfaces cannot be observed. Upstream automatic arrival/cancellation can
leave the conservative sent status visible until Clear or another handoff;
AIO does not infer successful travel from elapsed ticks or player proximity.

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

Historical verification before B1, with the installed Plugin Hub build on
2026-09-09 (the old ready/incomplete labels below no longer apply):

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

## B1 verification (2026-09-11)

Codex implemented the bridge; local headless Claude reviewed the upstream
protocol and patch, and local headless Kimi reviewed takeover/lifecycle cases.
No automated tests or test harnesses were added. Source and compiled-bytecode
inspection found no reflective member calls in either ShortestPath integration
class. RuneLite's typed ConfigManager API accepts a Type parameter; passing
Keybind.class to that API does not inspect another plugin. `./gradlew build`
passed.

Claude identified the off-thread config release and retained upstream state
issues, both addressed in the final patch. Kimi's final review found no blocking
correctness bugs. Its suggestion to retain a suspended lease while AIO is
unregistered was declined: events during that gap are unobservable, so a later
clear could erase a foreign route. The upstream retained-state limitation and
manual reset above are intentional. All bridge lifecycle/route callers were
confirmed to run on the client thread.

The user subsequently requested Claude Sonnet with medium effort. That explicit
headless follow-up was attempted but hit the local Claude usage limit; it did
not produce an additional review. Kimi and Codex reviewed the final changes.

Live verification uses the existing Main profile with the installed Shortest
Path and Quest Helper plugins. The owner logs in and performs any game actions;
agent interaction is confined to RuneLite plugin panels. Evidence is local under
`/tmp/aio-b1-team/` and is not included in distributable artifacts.

Initial live pass:

- Skeleton / Edgeville Dungeon Route displayed the conservative sent message
  and Shortest Path's bank pickup guidance. Clear removed the route guidance
  and restored the Route button.
- Disabling Shortest Path disabled Route. Guide / Getting there & access showed
  "Install and enable Shortest Path in RuneLite's Plugin Hub." Re-enabling
  restored availability and required a fresh bank observation.
- Starting Quest Helper's Bear Your Soul guidance after an AIO request replaced
  the route and returned AIO to Route. Changing to Catalogue preserved that
  quest route; disabling AIO also preserved its visible path.
- The owner used Set Target on a different game tile. AIO returned to Route,
  and changing to Catalogue preserved the manually chosen path. No agent
  clicked the game tile or moved the character.

Final build pass (development client restarted, then owner logged in and opened
the bank again):

- Route showed the sent message and fresh bank pickup guidance. Clear removed it.
  Rapid Route/Clear panel clicks also left no lingering route after callbacks
  settled (`final-rapid-route-clear.png`). This is a UI smoke check; generation
  and queue ordering were additionally reviewed in source, without a harness.
- Disabling and re-enabling Shortest Path with an AIO route removed the retained
  route and restored Route (`final-sp-reenable-clears-owned-route.png`).
- Quest Helper's Bear Your Soul helper replaced an AIO route, with AIO reverting
  to Route (`final-quest-takeover.png`). Its route survived Shortest Path
  disable/re-enable (`final-sp-reenable-preserves-quest-route.png`) and AIO
  disable (`final-aio-disabled-preserves-quest-route.png`).
- Disabling AIO while it owned a route removed that route
  (`final-aio-disabled-clears-owned-route.png`). AIO and Shortest Path were
  restored enabled, the temporary quest helper stopped, and AIO left on Active
  task. No game controls were used by the agent.

B1 is complete for the supported message/menu integration. No claim is made
that travel was performed, an unreachable route was detected by AIO, or the
configured clear hotkey, account switching, nested foreign callbacks, or an
uninstalled-provider profile were exercised live. Those paths were reviewed;
the disabled-provider UI was exercised. Other Plugin Hub blockers remain open.

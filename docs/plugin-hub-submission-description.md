# Add All-In Slayer

Draft Plugin Hub pull-request description. No submission has been made. Packaging
evidence currently covers candidate `977a21e5158c15a1481b7c23de31a9411208b342`;
the final manifest must identify the published, verified release commit. When
pasting into the Hub PR, expand repository-relative evidence links to full
GitHub URLs pinned to that published commit.

## Purpose and player interaction

All-In Slayer is a passive Slayer preparation advisor. It matches assignments
and catalogue selections to bundled monster variants, locations, equipment and
supplies, using observed account requirements and owned items. Catalogue
on-task previews are labelled separately from the detected assignment.

Players choose a setup, inspect preparation checks and optionally filter their
bank or request a route. Players perform every withdrawal, equip, teleport,
movement and combat action themselves. AIO generates no game input, invokes no
game actions and adds no new server-action menu entries. Updating a checklist
from client events does not execute its recommended actions.

## Bank Tags and setup export

The explicit Filter bank button uses core Bank Tags' registered-tag and
in-memory Layout APIs. The temporary bank view places equipment on the left,
the 28-slot inventory on the right and pouch runes below equipment. Repeated
cells reference existing bank stock; missing items are placeholders. Capture
continues reading the full bank ItemContainer, independently of this view.

Existing core withdrawal handlers remain attached to available items. AIO does
not call them or chain withdrawals. It suppresses layout dragging in its fixed
view, changes the bank-view title and hides the Bank Tag Layouts preview button
temporarily. If potion storage renders a different dose from the planned item,
AIO displays an unavailable placeholder and removes that placeholder's actions.
These are bank-view changes, not inventory, equipment or combat click-zone changes.

A private runtime tag and compatibility key prevent saving this temporary view
as a user layout. Cleanup removes those private records, restores hidden controls
and restores the remembered tab only while it still refers to AIO's view.
Another tag or native search releases the filter without reopening it. Existing
saved tags and layouts are preserved. External Bank Tag Layouts is optional;
core Bank Tags is the declared dependency.

Copy setup explicitly copies portable Inventory Setups JSON to the clipboard.
The player imports it; AIO does not require Inventory Setups or auto-import,
withdraw or equip its contents.

## Optional Shortest Path and Weapon Charges integrations

Route explicitly sends a public `PluginMessage("shortestpath", "path", data)`
handoff, using the same message mechanism as Quest Helper. Shortest Path
calculates and displays travel; AIO does not walk or activate transports.
Temporary request overrides allow relevant transport/item modes while retaining
upstream costs, spending limits, configured house facilities and unlock checks.
Saved Shortest Path preferences are not rewritten. Use of its bank cache
requires a fresh bank observation for the current account while it is active.

Upstream has no owner-qualified clear or request-completion notification. AIO
therefore reports Route sent, not guaranteed reachability or arrival. A local
ownership record observes foreign messages, native route commands and the clear
hotkey. Clear and lifecycle cleanup act only while that ownership remains valid.
Unobserved direct calls by another plugin cannot be guaranteed detectable;
disabled-provider lifecycle gaps can leave an old route for the player to clear
in Shortest Path. Missing or disabled providers produce a panel message. There
is no reflective inspection of the upstream pathfinder.

Weapon Charges support reads available provider configuration through reviewed
bundled item mappings, without reflective metadata lookup. AIO also observes
messages from player-operated Check actions. Unknown balances stay unknown;
estimates are distinguished from observations. Wilderness activation ether is
separate from usable charges. AIO never triggers Check or loads charges.

## Account observations and bundled data

Bank observations are timestamped and stored through RuneLite's RuneScape-profile
configuration, scoped to the account. Profile changes clear transient state
before loading that account's observation. A stored snapshot can become stale;
opening the bank refreshes it. Inventory, equipment, progress and preparation
checks use client-provided state, with unknown requirements kept explicit.
AIO does not publish account or bank contents to a custom service. RuneLite
controls persistence and any synchronization of its configuration storage.

Wiki-derived advice is compiled from `src/main/data/slayer` during the build and
ships as generated resources. Runtime code does not fetch Wiki pages or collect
data over HTTP. Authoring compilers are excluded from runtime classes. Source
attribution and applicable license notices are bundled. RuneLite-provided item
data and the optional plugins retain their own behavior and data sources.

## Static boss preparation and policy review

The catalogue includes Slayer-associated bosses and demi-bosses. Their Wiki
material is static preparation text: equipment, supplies, access and strategy
notes. It is not driven by a boss's current attacks, projectiles or phase.
AIO supplies no live attack prediction, prayer-switch prompts, attack counters
or combat safe/unsafe tile indicators. Explicit travel destinations handed to
Shortest Path are location guidance, not reactive boss-positioning prompts.

RuneLite lists new high-end PvM boss plugins among features it is not currently
considering. We flag the inclusion of boss preparation material for reviewer
interpretation; preparation-only behavior is not a claimed exemption or prior
approval. Jagex's guidelines also restrict boss assistance and server-action
menu additions. The boundaries above describe the implementation for review
against those rules, rather than asserting acceptance.

- [RuneLite rejected or rolled-back features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Jagex Third Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)

Both policy pages were checked on 2026-09-11.

## Review evidence

- [README, screenshots and limitations](../README.md)
- [Bank filter implementation notes](reconstruction/bank-filter.md)
- [Shortest Path protocol and ownership limits](reconstruction/shortest-path-integration.md)
- [Weapon Charges verification](reconstruction/weapon-charges-integration.md)
- [Exact-candidate Hub packaging and startup evidence](reconstruction/hub-release-verification.md)
- [Submission checklist and outstanding work](plugin-hub-readiness.md)

The recorded candidate passed strict official packaging/API checks and packaged
catalogue startup. Fresh owner-operated Weapon Charges Check and charge-limit
boundary verification remain outstanding in B2. This description does not mark
those checks passed or imply that RuneLite performs functional testing.

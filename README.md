# All-In Slayer

A passive, Java 11 RuneLite plugin for planning Slayer trips with equipment you
own. All task information, browsing, equipment recommendations, inventory
checklists, and strategy guidance live in the RuneLite sidebar.

The plugin never performs game actions, switches equipment, generates input,
or displays live prayer/tile instructions.

## Using the plugin

- **Active Task** reads your assignment and remaining count from the game. Boss
  assignments and location restrictions come from RuneLite's current game tables.
- **Catalogue** lets you search Slayer masters and their assignments, then explore
  monster variants, locations, and methods without an active task.
- **Setup** shows the destination, equipment, compact inventory, and packing list.
  **Checks** lists outstanding preparation and account checks plus detailed death
  risk. Its count covers the selected setup, not unused equipment alternatives.
  **Guide** contains the selected strategy, equipment reasoning, travel, other
  setups, assignment details, and Wiki sources.
- Open **Change setup** for location/method overrides, master filtering, ranking,
  and Wilderness/group preferences. The monster selector and Catalogue search
  stay visible; **Refresh** and **Copy setup** remain at the bottom of each view.
- Choose **Slayer XP**, **Profit**, or **Low effort**. Recommendations explain their
  ordering; they are qualitative rankings, not XP/hour or GP/hour predictions.
- Equipment follows the wiki's ordered alternatives for each documented slot.
  The plugin chooses the best usable alternative you own. Compatible owned items
  may fill gaps, marked **Owned fallback**. Missing mandatory equipment prevents
  a method from being recommended as ready.
- Helmet and amulet choices are compared together for applicable Slayer and
  Salve bonuses, including imbued and cosmetic forms. Salve and Slayer boosts
  never stack. Changes are labelled **Bonus comparison** and explain the active
  bonus; this is a relative stat estimate, not measured DPS.
- Equipment switches, required items, supplies, and travel items appear in the
  inventory checklist, with carried, withdraw, and missing quantities.
- Trip packing reserves required items, casting resources, a return to the
  selected Slayer master, and a usable Wilderness escape before filling spare
  slots with owned food. A shield switch can reserve one empty slot. Shortages
  remain visible; the planner never invents bank stock to fill the inventory.
- **Trip preparation** holds return, escape, casting and looting-bag details.
  Spells are checked against current levels, spellbook, unlocks and casting
  weapons. Rune quantities include both combat and travel. An owned rune pouch
  gets an exact configuration when its capacity and risk make it worthwhile;
  contained runes appear separately from the 28 physical slots.
- Wilderness preparations consider blighted food, restores and eligible spell
  sacks, plus an owned looting bag when it fits the loss budget. The planned
  bag must be emptied at a bank before departure; current contents are assessed
  separately. Bag contents are loot storage, never usable combat supplies.
- Select a different monster, location, or strategy to compare its setup.
  Unavailable options show why they cannot currently be used.
- **Route** beside the selected location sends its verified monster tiles or
  encounter entrance to **Shortest Path**, using the same plugin-message handoff
  as Quest Helper. Install and enable Shortest Path from RuneLite's Plugin Hub.
  It finds a route from your position using its account unlock checks and owned
  teleport items. Open your bank while both plugins are enabled to include
  banked items and the stop needed to collect them.
  **Clear** removes the current Slayer route; changing the destination clears it
  too. Unverified destinations remain unavailable. See **Guide > Getting there
  & access** for destination evidence and travel details.
- **Copy setup** copies an Inventory Setups import string. Import it yourself
  through Inventory Setups in RuneLite; the plugin does not change other plugins'
  configuration or your bank.
- **Filter bank**, at the bottom of the panel, shows the selected equipment and
  inventory in your open bank, arranged like the panel: equipment on the left,
  the 28 inventory slots on the right, and pouch runes below the equipment.
  Food repeats in its planned slots; each copy still represents the same bank
  stock. Unavailable items appear faded. Withdraw and equip items yourself;
  the packing list retains the required quantities. **Clear bank filter**
  returns to the normal bank view.
  The filter follows item changes within the setup and ends when you change
  setups, close the bank, log out, or switch to another bank view. It uses
  RuneLite's built-in **Bank Tags** plugin, which is enabled with All-In Slayer;
  Inventory Setups and Bank Tag Layouts are optional. Saved item tags and layouts
  are preserved, and charged item variants match the selected setup.
- The **Ready to leave** check above **Filter bank** compares the selected setup
  with your actual worn equipment and backpack. Click **N things left** to see
  remaining actions in **Checks**; each row expands for details. Bank ownership
  does not count as equipped or packed. Pouch runes must match the displayed
  configuration, and Wilderness checks include extra carried items and risk.
  **Charges & looting bag** shows automatic **Weapon Charges** estimates and your
  own in-game **Check** observations, including blowpipe darts and scales. Fresh
  checks can verify loaded charges; estimates remain labelled. Viewing the bag
  once records its contents, including an empty bag, after you close the view.
  Possible contents changes prompt another check. Catalogue reports **Packed for
  preview**, with assignment assumptions kept visible. See the
  [departure check notes](docs/reconstruction/ready-to-leave.md).
- **Wildy charge limit** in AIO settings warns only above your chosen
  usable-charge balance (default 500), excluding the 1,000 activation ether.
  A confirmed 500 usable charges / 1,500 total ether passes a 500 limit. Empty
  and unverified weapons still need attention. Planned ether preparation and
  actual carried risk remain separate from this alert limit.
- **Melee boosts**, **Ranged boosts** and **Magic boosts** in AIO settings let
  you reserve inventory slots for the chosen strategy's combat style. Set
  **Boost slots** and list **Preferred boosts** in priority order, separated by
  commas. For example, two melee slots with `Super combat potion, Combat potion`
  bring two owned super combat bottles, falling back to combat potions if no
  super combat is owned. Fuller bottles come first; a reusable heart takes one
  slot. Missing boosts leave marked empty slots and appear in **Checks**.
  Required strategy items retain priority. Set the count to **0** to keep
  automatic packing. See the [boost reservation notes](docs/reconstruction/boost-reservations.md).

**Change setup > After task** chooses a return to your Slayer master, a bank,
or your house. Owned seed pods and charged jewellery are checked separately
for Wilderness escape; the trip details list other owned escape options. House
tablets work across spellbooks; spells require the appropriate level, book and
runes. Saved Shortest Path POH settings are reused when its house routing is
enabled and labelled as configured facilities. Otherwise, optional house
shortcuts can be confirmed in **Checks**. See the
[teleport source notes](docs/reconstruction/return-teleport-sources.md).

Open your bank to capture what you own. Before the first scan, recommendations
use inventory and equipped items only. A last-seen bank observation is stored
per RuneScape account and shown with its age. It is an estimate while the bank
is closed: reopen the bank to refresh purchases, consumption, and transfers.
Bank placeholders and noted items are not treated as usable equipment/supplies.

**Account requirements** in **Checks** automatically checks quests, achievement diaries, base
skill levels, the selected spellbook, and supported account unlocks. Confirmed
requirements are hidden; unmet or unresolved level checks show your current
base level and the required level. Other access requirements can be
confirmed manually. Those confirmations are account-scoped and reversible,
and cannot override a known failed check. Catalogue uses an explicitly labelled
on-task planning assumption; it does not change the detected assignment or
confirm completion of quests, diaries or other account unlocks.

Wilderness and group methods are visible, but excluded from automatic advice
unless enabled. An active Wilderness assignment still requires eligible
Wilderness kills, but does not override the checkbox: enable it explicitly to
receive a location recommendation. Wilderness travel, including the route to
King Black Dragon, is included in that restriction.

Routing temporarily includes regular transport types without changing saved
Shortest Path settings. Its travel costs, spending limit, configured house
teleports, unlock knowledge and search cutoff still apply. It may report an
incomplete route. The packing list includes bundled travel, return and escape
preparations in its death-loss estimate. Additional items suggested dynamically
by Shortest Path still need checking against the packing list and risk. You
collect items and travel yourself. See the
[integration notes](docs/reconstruction/shortest-path-integration.md).

**Wilderness death risk** compares the proposed equipment, inventory, ammunition
and switches with what you currently carry. It shows ordinary protected items,
conditional Protect Item scenarios, replacement losses, repair fees and permanent
untradeable losses. Exact owned IDs distinguish Trouver locks and ornamented
forms. The bundled rules reflect the June 2026 death-system rework.

The default loss budget is **500,000 gp**, adjustable in plugin configuration.
Automatic Wilderness picks must fit it without relying on Protect Item, avoid
permanent untradeable loss, and have sufficiently known death rules and prices.
Owned alternatives and optional empty slots are considered while preserving
mandatory equipment, set dependencies and ammunition compatibility. Searches are
bounded; select a monster/location to focus a large task's comparison.

Wilderness methods follow the current Krystilia and boss strategy priorities:
chainmaces, Webweaver/Craw bows, powered or autocasting sceptres, Venator in
suitable multi-target locations, and cannon alternatives where supported. The
weapon order remains contextual when comparing affordable owned setups.

Catalogue searches assume the selected assignment for Slayer bonuses and
task-only preparations, including when your detected task is different. These
results and copied setups are labelled **On-task preview**. Account levels,
quests, unlocks and Wilderness loss still use observed state; a selected
Krystilia preview still requires an eligible Wilderness location. Preview
assumptions never change your detected assignment or saved account facts.

Ether-weapon plans use an explicit preparation target: **500 usable charges plus
1,000 activation ether per weapon** by default. Change `Planned ether charges`
for a different trip. Setup says **Can prepare - check charges**, links to the
full preparation details in **Checks**, and
requires observed loose ether to fund the target. This does not read or confirm
loaded charges; carrying more ether increases loss. A protected Venator retains
its charges, while an unprotected bow still has an incomplete contents estimate.

Unknown prices, internal charges and container contents remain explicit rather
than being valued at zero. Carried estimates include bank notes and separately
observed containers; they describe exposure at the selected destination/route,
not an assertion about the current tile. The game's Items Kept on Death interface
remains authoritative. See the [rule evidence and limits](docs/reconstruction/wilderness-risk-sources.md).

## Settings

| Setting | Default |
| --- | --- |
| Recommendation goal | Slayer XP |
| Include Wilderness | Off |
| Include group methods | Off |
| Wilderness loss budget (gp) | 500,000 |
| Planned ether charges (usable, plus activation) | 500 |
| Show task overlay | On |

The rebuilt engine replaces the previous DPS/Cost and manual cannon ownership
settings. Cannon methods now depend on observed equipment and supplies. The
existing overlay preference is preserved; bank snapshots start fresh in the
versioned format.

## Data and accuracy

Editable data lives in `src/main/data/slayer`. The build validates and compiles
that source graph into bundled resources, including `data/advisor-catalogue.json`.
There are no runtime wiki requests or custom HTTP data collection.

The source audit uses the OSRS Wiki MediaWiki API and records URLs, page IDs,
revision IDs, timestamps, missing pages, and extraction gaps. Retrieval of a page
is distinct from verification of every strategy detail. Read the current
[coverage report](docs/reconstruction/data-coverage.md) for the exact boundaries
of the migrated knowledge and any unresolved evidence.

Wiki priorities remain contextual. A charged item with unobservable remaining
charges, an unknown quest gate, or incomplete strategy data cannot be treated as
proof that a trip is ready. These cases are reported as requirements or guidance.

## Build and manual verification

Clan testers can [download the Windows preview ZIP](https://github.com/danieljglover/AIO-Slayer-Assistant/raw/refs/heads/main/downloads/aio-slayer-windows-preview-68918a0.zip)
without building from source. Extract the entire ZIP and follow `READ-ME-FIRST.txt`,
then run `Start-AIO-Slayer.bat`. This preview contains source revision `68918a0`;
its [SHA-256 checksum](downloads/aio-slayer-windows-preview-68918a0.zip.sha256)
is included for verification. Windows launch still needs tester verification.

Use the checked-in Gradle wrapper with JDK 11 or newer:

```sh
./gradlew build
./gradlew generateSlayerData
./gradlew run
```

On Windows, open PowerShell in the repository folder and use:

```powershell
.\gradlew.bat build
.\gradlew.bat run
```

`run` opens RuneLite with AIO loaded and uses your own RuneLite profiles and Hub
plugins. For a Jagex Account, complete the one-time login setup in the
[Windows guide](packaging/windows/READ-ME-FIRST.txt), based on
[RuneLite's official instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
Account credentials stay on your computer and must never be committed or shared.

The plugin targets Java 11 bytecode. This project deliberately has no automated
tests, test files, or testing dependencies. Compilation and source validation are
followed by manual checks in RuneLite. See the
[manual verification record](docs/reconstruction/verification.md).

The existing local Jagex launcher helpers remain in `scripts/`. Their session
file, `scripts/.jagex-env`, must stay local and is never part of the plugin or
source-data tooling.

For a Windows clan preview, run `./gradlew windowsPreviewZip`. Share only the ZIP
under `build/distributions/`. It contains the plugin, its existing development
launcher, intact runtime dependency JARs, a Windows batch launcher and
[tester instructions](packaging/windows/READ-ME-FIRST.txt). Testers need Java
11+ (the launcher can use RuneLite's bundled Java), but no build tools. Jagex
Accounts use RuneLite's documented local development-login setup. Personal
profiles, session files, repository scripts and other development plugins are
excluded. Rebuild and redistribute after incompatible game/client updates.
After an update, use `./gradlew --refresh-dependencies windowsPreviewZip` (Windows:
`.\gradlew.bat --refresh-dependencies windowsPreviewZip`) so Gradle checks the
current RuneLite release instead of reusing its cached dynamic-version result.
Check `BUILD-INFO.txt` in the ZIP for the included client version. Use the same
`--refresh-dependencies` flag with `run` when updating a local development client.

## Architecture

- `AllInSlayerPlugin`: lifecycle, event coalescing, background computation, and EDT publication.
- `bank/advisor`: account-scoped inventory, equipment, bank, skill, and quest snapshots.
- `task/advisor`: active assignment and location-table resolution.
- `model/advisor`: catalogue and immutable recommendation request/result snapshots.
- `loadout/advisor`: eligibility, equipment dependencies, inventory planning, and ranking.
- `ui/advisor`: native Swing catalogue/task panel and optional passive overlay.
- `data/source`: authoring-time source compilation and validation.
- `integration/advisor`: explicit clipboard export for Inventory Setups.

Historical design documents describe superseded implementations. The current
[reconstruction contract](docs/reconstruction/implementation.md) and code define
the new implementation.

## License

[BSD 2-Clause](LICENSE). Wiki-derived material retains its source attribution;
see the source evidence and [OSRS Wiki copyright policy](https://oldschool.runescape.wiki/w/RuneScape:Copyrights).

Unofficial fan project, not affiliated with Jagex or the RuneLite developers.

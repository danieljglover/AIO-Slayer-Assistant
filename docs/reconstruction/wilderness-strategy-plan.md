# Wilderness strategy priorities

Implement the current Krystilia guide and relevant boss strategies as contextual
preparations. Keep ordinary task variants, boss alternatives, equipment bonuses,
account checks, and Wilderness death calculations intact.

1. Verify current raw MediaWiki source and record revision evidence. Author
   complete Wilderness grids and distinct weapon, cannon, Venator and spell
   methods with explicit monster and location links. Preserve supporting
   travel, access, alternatives and task-management guidance.
2. Preserve the reviewed weapon order when comparing compatible Wilderness
   loadouts within the same loss constraints. Flat equipment stats cannot model
   Wilderness passives, Venator bounces, attack speed or boss weaknesses.
3. Correct charge-risk handling only where the exact item mechanic is verified.
   Never equate an owned charged item with a known remaining charge count.
   Keep planned and carried loss calculations truthful.
4. Validate all source JSON, regenerate the catalogue, compile with
   `./gradlew build`, and review the resulting source and runtime links. Manual
   client checks may use the plugin panel only; login and gameplay belong to
   the owner. Do not introduce automated tests.

Work stays on the existing reconstruction branch and preserves unrelated dirty
files. No runtime Wiki fetching, game actions, input generation, secrets, commits
or publication. Independent source research and charge-mechanic work use the
existing available agent slots; integration and final review remain coordinated.

Implemented 60 reviewed methods (20 boss/style and 40 Krystilia methods), source
location exclusions, and source weapon ordering within the same risk constraints.
The final compiler guards empty resolved locations after all overrides. Full
Verac and Chaos Fanatic crystal-armour conditions have separate methods. Seven
assignment/risk prose strings no longer act as false kill-access requirements;
actual Slayer levels, quest and equipment requirements remain enforced.

Protected Venator charges now use explicit reviewed retention rules. Ether
preparations use a configurable target (500 usable charges plus 1,000 activation
ether by default), require enough observed loose ether, and keep actual loaded
charges unverified. The panel and copied setup state the preparation assumption.

Live panel review observed Ursine chainmace with Salve amulet (e) for Vet'ion,
conditional ether preparation for Calvar'ion, and the cave lesser-demon Venator
method. The lesser-demon review exposed the assignment-prose gate corrected
above. No gameplay was performed; login and bank opening were done by the owner.

The owner's helmet questions were traced to two distinct conditions: Salve
suppresses Slayer bonuses at Vet'ion, and Catalogue previews previously omitted
task bonuses. The owner then specified that Catalogue searches must presume the
selected task. Catalogue now applies that explicit planning assumption to Slayer
bonuses and task-only preparations, labels the result and exports as an on-task
preview, and keeps the detected task and saved account facts unchanged. Both
helmet forms remain eligible. Real levels, quests and non-task access checks
remain enforced; Krystilia previews still require Wilderness kills.

All source JSON and catalogue generation passed. The final Java build and native
client reload are recorded in the delivery response; the source evidence and
method inventory are in `wilderness-strategy-sources.md`. Independent reviews
covered planner risk ordering, planned/actual charge separation, UI/export
assumptions, location bindings and method-specific equipment dependencies.

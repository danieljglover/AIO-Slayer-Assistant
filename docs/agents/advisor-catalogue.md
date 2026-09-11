# Advisor catalogue authoring

The RuneLite runtime reads the bundled `data/advisor-catalogue.json` generated
by `AdvisorCatalogueCompiler`. `./gradlew generateSlayerData` runs this compiler
and the original modular graph validator. Neither runtime recommendations nor
Gradle generation fetch wiki pages.

## Additional source inputs

The `src/main/data/slayer/advisor/` directory supplements the normalized masters,
tasks, monsters, locations and strategies:

- `wiki-audit.json` records the MediaWiki evidence manifest and extraction gaps.
- `wiki-equipment.json` preserves ordered equipment grids and source context.
- `equipment-items.json` records item IDs, usable forms, equipment slots, equip
  requirements and the evidence supporting each authoring decision.
- `combat-bonuses.json` maps reviewed item IDs to Slayer/Salve effects. Keep
  imbued and regular forms separate; the compiler rejects duplicate IDs, unknown
  items, and effects on the wrong equipment slot.
- `death-rules.json` maps exact item forms to reviewed death behavior, protection
  value overrides, repairs, replacement components and independent contents loss.
  Never collapse locked or ornamented forms through a family alias.
- `wilderness-risk.json` records encounter/travel depths, entry deposits and
  Revenant Caves' special death rules. Unknown depths use -1/-1, never zero.
- `routing-destinations.json` binds exact monster/location pairs to verified
  world points. `RouteDestinationCompiler` rejects duplicate bindings, bad
  references, invalid coordinates and missing provenance. Use `MONSTER` for
  verified spawn tiles and `ENTRANCE` for an explicitly labelled approach.
  Legacy Wiki map coordinates require an evidenced world-coordinate conversion;
  never guess an area centre or infer the plane. See the
  [routing integration notes](../reconstruction/shortest-path-integration.md).
- `equipment-links.json` binds grids to reviewed strategy styles and contexts.
- `equipment-families.json` expands broad wiki rows into concrete alternatives,
  retaining individual eligibility and distinguishing trimmed cape forms.
- `overrides.json` supplies explicit method, item, protection and access rules.
- `spells.json` records supported spell prerequisites and rune quantities.
- `casting-resources.json` records exact rune, combination-rune, infinite-source,
  pouch and powered-weapon forms. Charged equipment is not an infinite resource
  unless its source establishes that behavior without an unknown charge count.
- `trip-preparation.json` records master, bank and house return routes, charged teleport forms,
  Wilderness limits, helper items, blighted substitutions and looting-bag rules.
  `TripPreparationCompiler` validates all three preparation files together.
  Item charges of zero mean unlimited; omit unusable uncharged forms. Portable
  use requirements are separate from equipment or jewellery recharge gates.
  `escapePriority` ranks eligible exits after Wilderness coverage; it is not a
  measured route time. Bank routes require a reviewed nearby usable bank.
  House facilities may come from labelled Shortest Path configuration or manual
  confirmation; neither is a game-observed furniture scan.
- Other evidence and alias files preserve authoring provenance; inspect the
  compiler before assuming a file changes runtime behavior.

`scripts/audit-advisor-wiki.py` performs authoring-time MediaWiki extraction.
Review its output before accepting eligibility or strategy mappings. The helper
`scripts/normalize-advisor-overrides.py` rewrites the corresponding override
source; make durable changes in that helper as well as regenerating its output.
Do not treat generated `build/` resources as source input.

## Required semantics

- A combat method must identify the actual monster variants and compatible
  locations. A referral to another monster is not a method for the current one.
  Cross-family task credit must be represented in the task's `variantIds`; a
  monster appearing under Boss does not automatically link it to ordinary tasks.
- Mark incidental forms with monster `advisorRepeatable: false`. They remain
  visible task-counting context but are excluded from selectable preparations.
  Preserve distinct NPC identities when forms have different task categories.
- Wilderness matching uses location flags, not a substring of its name:
  `Non-Wilderness` must never count as a Wilderness area. The checkbox governs
  automatic recommendations even for an active Wilderness assignment.
- A chamber may declare `assignmentAreaId` referencing its canonical assignment
  area. This preserves Konar's area lock without inheriting the parent area's
  combat capabilities or access rules. The compiler validates the reference;
  undeclared or unrecognized area relationships remain blocked.
- Preserve travel, access, skip/block, phase and other supporting methods as
  guidance. They must not become selectable combat setups merely because they
  inherit an equipment grid.
- Equipment grids are contextual. Match solo, tank, attacker, combat style and
  boss-specific sections explicitly. Generic Slayer equipment is appropriate
  only where the method has no overriding weapon, set or combat constraint.
  Set `advisor.equipmentReviewed` only for a complete reviewed authored grid;
  the compiler preserves that grid instead of inheriting another table.
- Use source `advisor.excludeLocationIds` to remove locations replaced by a
  reviewed method. The compiler validates and applies exclusions after method
  overrides; a method with no remaining locations is not selectable. Wilderness
  weapon order is a source preference, not a flat stat or measured DPS ranking.
- Krystilia methods distinguish cannon, Venator multi-target, blood barrage and
  single-target weapon contexts. Bind them to exact variants and supported
  locations; Wilderness travel risk alone does not grant Wilderness weapon boosts.
- Death rules with `contentsType: ETHER_WEAPON` model the activation deposit and
  a configurable planned usable-charge target separately from actual carried
  charges. Planned targets require owned loose ether and explicit preparation;
  they are never reported as observed loaded quantities. Only reviewed exact
  forms may use `chargesKeptWhenProtected` to retain hidden charges in protected
  death scenarios.
- Monster `undead` controls Salve eligibility for that variant; a task-family flag
  does not apply to every linked boss. Task `slayerHelmApplies` can disable the
  helmet effect, but cannot bypass target or assignment-area checks. Catalogue
  requests explicitly assume the selected task for bonuses and task-only access;
  label these as on-task previews. Active-task requests use observed task state.
  A Krystilia preview still requires Wilderness kills. A boss-assignment diary
  alternative applies only to a matching concrete Boss preview, not a normal
  creature task. Keep preview assumptions separate from saved account facts.
- The bundled master/task list filters catalogue previews. A logged-in active
  assignment with kills remaining takes precedence over a missing master link;
  retain a coverage notice and continue checking levels, gear, assignment area
  and Wilderness restrictions. Never substitute a saved catalogue master when
  the active master is unknown.
- Ordered alternatives within one slot are not a list of mandatory switches.
  Multi-style methods need explicit required switches and compatible ammunition.
- `Choice.slot == "RUNE POUCH"` denotes planned contained runes, excluded from
  physical slot counts and Inventory Setups' inventory array. The physical pouch
  still uses one slot. Both loose and contained runes share observed ownership;
  add combat and travel consumption rather than satisfying both with one stack.
  Completed-trip risk must use `baseline(equipment, inventory)` to include the
  contained target exactly once. Looting-bag contents never join usable stock.
- Item-option `requires` groups represent equipped dependencies: all groups must
  be satisfied, with alternatives inside each group. A method defined by a full
  armour set must require each piece.
- Required protective equipment must be equipped. A mixed shield/potion group
  means equipped protection OR enough usable consumables in the trip plan;
  merely carrying the shield never provides protection.
- `requirementsKnown` means equip requirements have been reviewed. Do not derive
  it from page retrieval, crafting requirements, or ownership alone. Unknown
  equipment uses an explicit account confirmation until evidence is available.
- `RequirementEvaluator.assess` is shared by the recommendation engine and the
  Account requirements panel. Current quest, diary and supported unlock facts
  come from `AccountProgressCapture` on the client thread; levels and spellbook
  come from the same immutable snapshot. MET, UNMET and UNKNOWN are distinct.
  Saved manual confirmations cannot override known unmet or unavailable checks.
- Requirement text uses stable exact names. Whole-name facts are checked before
  AND/OR expressions so quest names and diary regions retain their meaning.
  Mixed preparations retain manual checks for unsupported remaining conditions.
  Diary equipment exemptions use the same evaluator as the panel.

See [the runtime contract](../reconstruction/implementation.md),
[equipment eligibility review](../reconstruction/item-review.md), and
[coverage report](../reconstruction/data-coverage.md). Source parsing, reference
validation, Java compilation and manual native-client checks are required;
this repository deliberately has no automated tests.

# Trip packing implementation plan

User request: fill the usable 28-slot inventory, bring next-task and Wilderness
escape teleports, validate offensive spell requirements and rune quantities,
configure an owned rune pouch when it saves slots, use appropriate blighted
supplies/sacks, and include Wilderness looting-bag logic.

Architecture: bundle reviewed preparation rules alongside the existing Wiki
catalogue. Plan required gear, ammunition, casting and travel first, compress
runes into a feasible owned pouch, then fill remaining capacity with useful
owned food/restoration. All nested contents have explicit placement and risk;
storage is never extra usable combat inventory. Display preparation concisely
with detailed rune/pouch/teleport/bag guidance on demand; export exact quantities.

- [x] Verify current raw Wiki source for teleport thresholds, master-return
  options, offensive spell levels/books/runes, sacks, pouches and looting bags.
- [x] Compile pinned preparation data with reference/quantity/capacity checks.
- [x] Implement spell and travel planning, bag selection, complete capacity fill,
  pouch allocation and explicit shortages without exceeding observed ownership.
- [x] Integrate planned container contents with death risk and existing capture.
- [x] Update panel/export to distinguish carried inventory from pouch contents.
- [x] Review scoped changes, validate JSON, generate data, compile cleanly and
  manually inspect plugin panels without game interaction.

Ruling: the user's implementation request authorizes these reversible changes;
no additional design approval is needed. Preserve this dirty feature worktree.
No commits, automated tests, runtime HTTP, game input or secret/session reads.
Use the existing idle agents for bounded research/implementation and review;
root owns integration and final verification. Default to the selected/detected
Slayer master; ambiguous master selection stays explicit. Fill useful capacity,
but preserve a real slot needed for displaced equipment and report shortages.

Verification: all Slayer source JSON passed jq validation. Clean data generation,
Java build and dev launcher compilation passed. Live panel inspection confirmed
28/28 physical slots including an empty-target looting bag, separate rune-pouch
configuration and visible rune shortages. Follow-up fixes cover exact Slayer-ring
death rules, completed quests satisfying partial-progress gates, and funded
burst/barrage alternatives. Final client inspection confirmed the Skeleton setup at 28/28 physical slots
with complete planned risk, and the Dust devils pouch configuration with its
real rune shortage and completed partial-quest check hidden. All interactions
were confined to plugin panels; the owner logged in and opened the bank.

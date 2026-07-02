# Monster Variant Selection - Code Review

Merge-gate reviews for the Monster Variant Selection feature (board MV-*). Reverse-chronological.

---

## MV-SRV - Strategy-Guide Gear override (MV-S1..S4): `3feab8d..HEAD` -> APPROVE-WITH-NITS

**Reviewer:** Code Reviewer. **Date:** 2026-06-30. **HEAD:** `d6afc4f`. **Scope:** the strategy-increment
diff `git diff 3feab8d..HEAD` (11 commits: 92f5635, f9d3fbf, f6fda03, be36bb7, 1e721ff, 7ed1d0d, a81fe74,
f65f305, 059c2c1, 5e8753e, d6afc4f). Reviewed against `docs/loadout/adr/0015-strategy-guide-gear-override.md`
and `docs/variants/plan.md` `## Strategy-Guide Gear (MV-S)`. Rigor: `superpowers:requesting-code-review`.

**Verdict: APPROVE-WITH-NITS. 0 Blocker / 0 Should / 3 Nit.** Suite re-run by me (not on faith):
`./gradlew cleanTest test` BUILD SUCCESSFUL, aggregated over 48 XMLs = **377 tests, 0 fail / 0 error /
0 skip** (was 371 at MV-S2 follow-up; +6 = 4 dataset-validation + 2 live-dataset advisor). Merge-ready.

### What I verified

**Override engine (MV-S2) - faithful to ADR-0015 + the amended multi-style rule.** The 7-arg
`GearSelector.select` overload is additive: the 6-arg delegates `Collections.emptyList()`, and an empty
list is byte-identical to today's pure-DPS path (test-pinned `emptyStrategyListIsByteIdenticalToTheNoOverloadPath`,
the FR-S4 anchor). Override-first in `resolveWeaponAndShield`: `pickStrategyWeapon` walks the priority ids
and returns the first match in the already-viability-filtered (`weaponRank > 0`) twoHand/oneHand lists, so
owned + equipable + viable is enforced for free; an unowned/wrong-style id is simply absent and skipped
(bank-aware priority walk). 2h override drops the shield; 1h keeps the stat-picked `bestShield` (which is
computed above the pick) - both proven by `strategyOverrideRespectsTwoHandAndShieldInterplay`. No
`DpsEstimator` signature change; combat maths untouched (NG-4).

**The chosen weapon still gets its ConditionalBonus/WeaponEffect in the DPS display.** The override only
substitutes the WEAPON id in the worn map; `DefaultDpsEstimator.estimate` is unchanged (not in the diff)
and already credits the worn weapon's `WeaponEffect` + max-applicable `ConditionalBonus` over equipped
items (WDB-8 / LFB-5). So Emberlight on a demon variant displays its demonbane-credited DPS with no new
wiring and no new double-count. Confirmed by code-path inspection, not a new test (the path is pre-existing
and already covered).

**Default method (MV-S2 follow-up) - correct, and tests do NOT assert `primaryStyle == weakness.style`.**
`defaultMethod = validStrategy != null ? strategy.primaryStyle : recommendedStyle` (advisor, derived at
advise-time). The authored `weakness.style` is never mutated. `styleMismatchVariantsDefaultToTheStrategyStyleOnRealData`
pins Abyssal Sire (real authored weakness MAGIC, via `realTaskWith("Abyssal Sire", MAGIC)`) defaulting MELEE,
and Vorkath (authored weakness MELEE) defaulting RANGED - the test targets the real entries whose weakness
differs from the guide, proving the divergence without ever asserting equality. Confirmed: no test in the
suite asserts `strategy.primaryStyle == weakness.style`.

**Generalized multi-style toggle.** `strategyOverrideIds` builds the union: `primaryStyle` served by the
priority-ordered `primaryWeapons`, plus each secondary weapon's own `style` served by that weapon; a secondary
tagged with the primary style appends after the primaries. Undocumented effective style -> empty list -> stat
engine. Proven synthetically (`strategyOverridesEveryDocumentedStyle`,
`methodToggledToAnUndocumentedStyleDisablesTheOverride`) and on real data (Tormented Demon Melee->Emberlight,
Ranged->Scorching bow). Malformed strategy (null `primaryStyle` / empty `primaryWeapons`) -> empty list +
one-time `log.warn`, treated as no strategy; the shared pure `validStrategy()` gate feeds both the
default-method derivation and the override-id computation.

**Data (MV-S3) - clean and honest.** 77 strategy blocks (65 monsters; 12 live under both a Boss task and a
regular task with the same strategy). Byte-faithful injection confirmed: JSON line count unchanged
(1165 -> 1165), valid JSON, 43 tasks. All 6 gap variants (Dad, Ice Troll King, Arrg, Dark Ankou, Phantom
Muspah, Brutal black dragon) carry NO strategy (script-verified + `noStrategyBlockOnNoPageOrNoWeaponVariants`
pins them plus a sample of stat-engine mobs). **Item ids verified hard:** I parsed the authoring script's
81-entry name->id table and cross-checked every id against the authoritative `net.runelite.api.ItemID`
constants (`/tmp/itemids.txt`, 16334 entries) by normalized name match - **0 mismatches**, so no fabricated
ids and no real-but-wrong ids. The 78 distinct strategy itemIds in the JSON are all in the verified table
(no hand-drift). Spot-checked Emberlight 29589, Scorching bow 29591, Arclight 19675, Fang 26219, DHCB 21012
- all correct; the demonbane ids match the already-shipped `ConditionalBonusRegistry`. Spot-checked
Tormented Demon / Skotizo / Abyssal Sire / Vorkath blocks against the wiki guide narrative + source URLs
(real `/Strategies` pages) - faithful, with honest notes ("Guide centres melee despite the data weakness").

**Test adequacy - non-vacuous.** The headline live-dataset test gives Fang 3x Emberlight's raw stats
(150/150 vs 50/50), so Emberlight winning proves the OVERRIDE fired, not a DPS coincidence; it also covers
Ranged->Scorching bow and Fang-only->Fang (priority-walk fallback). The GearSelector override tests use a
baseline-then-override relative assertion (the higher-DPS weapon wins WITHOUT the override, the strategy
weapon wins WITH it). Dataset-validation asserts a floor of `authored >= 60`, positive ids, secondary styles,
and wiki sourceUrls so it can't pass vacuously. Round-trip + absent-key-null pinned in `TaskDataJsonTest`;
note present/absent pinned in `SlayerPanelTest`.

### Disposition of the raw-id charge-form limitation -> ACCEPT for v1 (documented Nit, not a Should)

The override matches by exact raw item id, so a degraded/alt-charge banked copy (bofa (c) 25867 vs 25865,
an empty Toxic blowpipe, a partially-charged Karil's xbow) won't match and the loadout falls back to the
stat-driven DPS pick. I judge this **acceptable for v1** and **not a blocker or a Should**, because:
(1) it is the same raw-id keying discipline `WeaponEffectRegistry` / `ConditionalBonusRegistry` use, an
ADR-backed deliberate choice (LFB proved `ItemVariationMapping` collapse erases meaningful state);
(2) the failure mode is safe and honest - a stat-pick fallback (still a sensible owned weapon) AND the
"Wiki strategy" note still shows the user the wiki's answer; no wrong answer, no unowned recommendation,
no crash; (3) the mitigation is DATA-ONLY - list the charge/recolour ids as extra priority entries on the
strategy, no engine change - exactly as ADR-0015 anticipates. QA should be aware (already on the QA list).

### Nits (3, none blocking)

- **N1 (stale comment).** `MonsterStrategy.primaryStyle` carries the field comment `// the default method
  this strategy sets (== variant weakness style)`. The `(== variant weakness style)` clause is wrong and
  directly contradicts the MV-S2 follow-up: `primaryStyle` and `weakness.style` intentionally differ (Abyssal
  Sire, Vorkath, and ~5 others). The CODE is correct; the comment lies. Drop the parenthetical.
- **N2 (untested COST-mode interaction).** The override fires regardless of `AdviceMode` - in COST mode an
  owned strategy weapon is forced over a cheaper viable weapon, bypassing "cheapest viable." This is
  defensible by ADR-0015 ("the override is unconditional once owned + style matches; it does NOT compare
  DPS") and safe (owned gear only), but it is undocumented for COST and has no test. Either add a one-line
  note + a COST-mode test, or consciously waive. Low severity.
- **N3 (no-action).** `LoadoutAdvisor.WARNED_MALFORMED_STRATEGY` is a static set, never cleared - bounded by
  the count of distinct malformed-variant names, harmless, persists for the plugin lifetime. Fine as is.

**Out of scope (pre-existing, noted):** Abyssal Sire appears twice with differing authored `weakness.style`
(MELEE in the boss-pool entry, MAGIC in the boss-default entry) - an MV-RV data trait already shipped; the
same MELEE strategy is on both, so the increment is consistent.

**Bottom line:** policy-in-advisor / mechanism-in-selector is clean, the empty-list regression anchor holds,
the ids are hard-verified against the real `ItemID` source, and every claim maps to a non-vacuous test.
The override is safe, honest, and reversible. Merge-ready; the 3 Nits are cleanup, not gates.

---

## MV-RV - Monster Variant Selection (MV-B1..B9 / MV-D1..D12 / MV-FE1..FE3): `494cb76..HEAD` -> APPROVE-WITH-NITS

**Reviewer:** Code Reviewer. **Date:** 2026-06-29. **HEAD:** `f6bde9a`. **Scope:** the full feature
diff `git diff 494cb76..HEAD` (7 MV code commits `79f16b8..f6bde9a`: backend B1-B9/D1-D12 + frontend
FE1/FE2/FE3). Reviewed against `docs/variants/prd.md` (FR-1..7), `docs/variants/plan.md` + the
`## MV-A2 amendments`, and ADR-0010/0011/0012/0013/0014. Rigor: `superpowers:requesting-code-review`
(correct, simple, safe, tested).

**Verdict: APPROVE-WITH-NITS. 0 Blocker / 1 Should / 3 Nit.** Merge-ready once the team-lead notes the
Should (it does not block - the code is correct today). The engine re-key is faithful, the data
discipline is honest, and every FR maps to a non-vacuous test.

**Suite re-run by me (forced cleanTest):** `./gradlew cleanTest test --console=plain` -> BUILD
SUCCESSFUL; aggregated `build/test-results/test/*.xml` across **47 files = 346 tests, 0 failures /
0 errors / 0 skipped** (was 330 at the backend close-out; +16 from the FE wave). Confirmed, not on faith.

### What I verified (faithful to the design)

**Engine re-key (MV-B2..B5/B9) - correct, no maths leaked.**
- `MonsterProfile.fromTask` reproduces the task-level profile field-for-field (FR-6 anchor); `fromVariant`
  overlays variant weakness/defence with a per-field fallback to the task default (ADR-0012.3) and takes
  category flags from the variant, `slayerHelmApplies` from the task (FR-5). Both null-safe.
- The method-axis seam is exactly the design: `GearSelector.select` takes an explicit effective
  `CombatStyle`; `LoadoutAdvisor` resolves `selectedMethod ?? recommendedStyle` (advisor line 81) and
  passes it. The within-style `meleeAttackType` argmin over `monsterDefence`, the `element`-based spell
  pick, and the `BonusContext`/category gating are UNCHANGED - the style param only replaces the old
  internal `styleOf` derivation. `DefaultDpsEstimator`/`DpsEstimator` cleanly swap `TaskData -> MonsterProfile`;
  no formula in `meleeCore`/`rangedCore`/`dps`/the magic core changed. No-double-count (weapon excluded
  from the additive `(m-1)*L`, own conditional applied multiplicatively in `weaponRank`) is intact.
- The override genuinely re-drives gear: `forcingTheMethodReDrivesBestInStyleGear` and
  `selectedMethodOverridesRecommendedStyle` flip the worn weapon MELEE<->RANGED on one profile.

**Data integrity (`slayer-data.json`, 43 tasks) - clean.**
- Existing 42 tasks' `weakness.style` PRESERVED: the only `-` removals of a style/defence line in the
  whole JSON diff are the Boss meta-task's task-level profile being set null (ADR-0014). The RANGED+fire
  line is the NEW Frost Dragons task (task 43), not a flip. `monsterDefence` of existing tasks untouched.
- Boss task: task-level `weakness`/`monsterDefence` = null; **33 boss variants, every one carries a
  non-null weakness AND defence** (verified by script - zero null profiles); exactly one `isDefault`
  (Abyssal Sire); deferred bosses omitted, none fabricated. Every multi-variant task has exactly one
  `isDefault` (script-verified across all 43). Reanimated excluded (test-guarded). Barrows brothers carry
  real per-brother styles and `undead=false`/all-flags-false (spectral, strict - Salve does not apply).
- `dragon` set updated to include Frost Dragons; `DuradelDatasetValidationTest` pins the exact draconic set.

**Test adequacy - non-vacuous, real controls.**
- FR-4/FR-5 flips use real controls: defence-profile flip picks stab-vs-crush weapon; demon-vs-non-demon
  sibling flips Arclight-vs-scimitar; the advisor tests run a REAL `DefaultDpsEstimator` so the style
  actually differentiates weapons. FR-6 style-pin (`existingTaskStylesArePreserved...`) pins all 41
  existing curated styles + Frost Dragons. FE tests cover hidden-when-<=1 / no-variants, "(Boss)" marker,
  callback fires raw name (not label), built-once-reused, method default==recommended style, override
  reflected, callback fires label, programmatic reconcile does NOT fire, disable scoped to the effective
  method (others stay enabled), all-enabled-when-viable, boss note with location. Wiring helpers
  (`hasVariant`/`resolveBossVariantName`/`isBossTask`/`parseMethod`) unit-tested incl. override-wins and
  unmapped-falls-through.

### The 2 FE-flagged items - explicit judgement

1. **Method-disable scoped to ONLY the effective method (-> Nit, accept for v1).** `reconcileMethod`
   disables a segment only when `status == TASK_WITHOUT_LOADOUT` for the effective method; the other two
   stay enabled. This is honest given the contract: the panel holds ONE recommendation, so it has no
   per-method viability signal, and the backend established most methods are viable anyway (a wrong-type
   weapon still scores positive DPS - the only true non-viable cases are no-weapon-at-all or magic-no-spell).
   Worst case (owns no weapon at all) shows two methods enabled that also yield TASK_WITHOUT_LOADOUT when
   clicked - mildly understated, never wrong (the user still gets the empty-loadout state). Documented in
   the `reconcileMethod` javadoc + team memory. Richer per-method viability needs a frozen state-contract
   change; **acceptable for v1, recorded as N1 to revisit, not a Should.**

2. **`resolveVariant` duplicated into `SlayerPanel` (-> Should, share it).** `SlayerPanel.resolveVariant`
   (12 lines, static) is a byte-for-byte copy of `LoadoutAdvisor.resolveVariant`. They are identical
   today and the behaviour is correct, but the panel's pre-selection MUST track the advisor's resolution
   or the UI would show variant X selected while the engine geared for variant Y (an FR-6 coupling). Two
   copies that can silently drift is exactly the divergence the studio avoids. Low-effort fix: widen
   `LoadoutAdvisor.resolveVariant` to public (or lift to a small shared resolver / a `MonsterVariant`
   static) and call it from both. **Should - share before this grows a third caller.**

### Findings

- **S1 (Should):** share `resolveVariant` between `LoadoutAdvisor` and `SlayerPanel` (FE item #2 above).
- **N1 (Nit):** method-disable narrow scope (FE item #1) - accept for v1, revisit with a per-method
  viability state-contract.
- **N2 (Nit / QA):** no boss carries a `bossId` yet (script: 0 of 33), so MV-B8's live pre-selection path
  is dormant and always falls to the deterministic default. Correct + safe (never fabricated), but the
  varbit-4723 -> boss mapping is 100% un-exercised in production until QA maps it. Documented on
  `MonsterVariant.bossId` + team memory - a live-verify item, not a silent bug.
- **N3 (Nit / QA-research):** several refined `element` values are best-guess fallbacks on monsters with
  no strong wiki elemental weakness (e.g. Kalphites/zygomites = "fire"). Inert at the default method
  (style != MAGIC); only changes the recommended spell tier if the user forces magic, and the % is not
  modelled (NG-4). Data-quality nuance, not a correctness issue. (TzHaar="water", ice creatures="fire"
  are wiki-accurate - the author did reason about it.)

### Residuals = live-client items already on the QA list (not bugs)
bossId<->varbit-4723 mapping (N2); Frost Dragons real Duradel target varp (synthetic `slayerTargetId 9001`
shipped); Barrows/Salve applicability (shipped `undead=false`); deferred-boss enumeration; method-switch
re-drives the loadout live. All documented as live-verify in team memory.

**Bottom line:** the re-key is honest and maths-free, the data discipline (preserve styles, null Boss
profile + non-null boss variants, deferred-not-fabricated, no faked bossId) holds, and the tests would
fail if the behaviour broke. One non-blocking DRY Should. I would be happy to be paged about this in a year.
</content>

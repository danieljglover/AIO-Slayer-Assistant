# Code Review - Side-Panel Redesign (Waves 1-6)

- Reviewer: Code Reviewer
- Date: 2026-06-29
- Scope: the merge gate for the full side-panel redesign (Waves 1-6), reviewed against
  `docs/plan.md` (FR-1..FR-11, §9 NFRs, §10 risks), `docs/adr/0001-0004`, and
  `docs/ui-review/{ui,ux}-recommendations.md`. Working-tree state (nothing committed).
- Suite: `gradlew.bat cleanTest test` -> BUILD SUCCESSFUL, **135 tests, 0 fail/0 error/0 skip**
  (confirmed: 135 executed test-cases in `build/test-results`, 135 `@Test` methods). Claim verified.

## Verdict: CHANGES-REQUESTED

One Blocking correctness defect on a load-bearing, player-facing signal (required-item ownership),
with a small correct fix that uses data already present in the state. Everything else is
approve-quality: the architecture honours all four ADRs, threading is correct, the self-diff is sound,
diagnostics are properly gated, there is no `<html>` and no security smell, and the test suite is
green and (mostly) non-vacuous. Fix the one Blocker (and ideally the Should-fixes) and this is an
approve - the turnaround should be fast.

---

## Blocking

### B1. Required-item OWNED/BLOCKED is computed from a weaker proxy than the signal already in hand
- **Where:** `ui/SlayerPanel.java:743-766` (`requiredState` / `loadoutContains`), keyed in at `:254`.
- **What:** the panel marks the required item `OWNED` iff its id is in `recommendation.worn` values
  or `recommendation.inventory`, else `BLOCKED` (alarming red "missing"). This is wrong whenever the
  player **owns** the required item but the advisor equipped a higher-tier item in that slot, or the
  required item is not a worn/inventory item at all -> the panel then shows red "missing" for an item
  the player owns. In the no-loadout state (`recommendation == null`) it is **always** `BLOCKED`, so a
  new player with a task who has not scanned their bank sees the required item in red even if they own
  it. This misinforms on one of the three questions the panel exists to answer (P1-10; UX Q1
  "required item with owned/missing state").
- **The real signal already exists:** `LoadoutAdvisor.java:89-92` adds `requiredItemId` to
  `missingUpgrades` **iff `!owned.has(requiredItemId)`**. So for the with-loadout case,
  `recommendation.getMissingUpgrades().contains(requiredItemId)` *is* the authoritative "not owned"
  answer - and it is already carried in `SlayerPanelState`. The panel ignores it.
- **Smallest correct fix (with-loadout, no state change):** in `requiredState`, when `rec != null`,
  return `BLOCKED` iff `rec.getMissingUpgrades() != null && rec.getMissingUpgrades().contains(id)`,
  else `OWNED`. ~3 lines; removes the false "missing" in the common path using existing data. Add a
  test for "required item owned but a better item is worn -> OWNED".
- **Full fix (covers the no-loadout case):** add a `Boolean requiredItemOwned` to `SlayerPanelState`
  (additive, defaulting null like the other overloads), set it from `owned.has(requiredItemId)` in
  `AllInSlayerPlugin.recompute(...)` (where `owned` is already in scope at `:242`), and have
  `requiredState` prefer it. This is the only way the no-loadout state can be correct, and it matches
  the architect's own escape hatch noted in `_team/MEMORY.md` (Wave 5). If the team consciously accepts
  "always BLOCKED when no loadout could be built" as a documented v1 limitation, the with-loadout fix
  alone clears the Blocker, but the no-loadout red-on-owned case should then be tracked as a follow-up.

---

## Should-fix

### S1. Long player-facing notes are plain non-wrapping JLabels and clip at ~225px (FR-10)
- **Where:** the no-loadout hint `ui/SlayerPanel.java:463`; the no-bank note `:633-643`
  (`noBankNote()`). Both are full sentences in `TYPE_CAPTION` on a plain `JLabel`.
- **What:** FR-10 / NFR §9 require long copy to "wrap or truncate cleanly inside panel width". With
  `HORIZONTAL_SCROLLBAR_NEVER` there is correctly **no** horizontal scrollbar (good), but these
  multi-sentence notes are cut off mid-word rather than wrapped. The empty-state *titles/bodies* that
  go through `PluginErrorPanel` wrap fine (its internal `<html>`); these two inline notes do not.
- **Fix:** wrap them. The clean RuneLite idiom that does not reintroduce `<html>` (ADR-0004) is a
  read-only, line-wrapped `JTextArea` styled as a label (`setLineWrap(true)`,
  `setWrapStyleWord(true)`, opaque card bg, caption font), or route the no-loadout copy through the
  same `PluginErrorPanel`/wrapping path the other empty states use. The long diagnostic *keys* clipping
  is dev-only -> Nit (see N3), not part of this.

### S2. The NFR self-diff tests reuse the *same* object reference, so they do not guard the contract the plugin actually relies on
- **Where:** `ui/SlayerPanelTest.java:348-396` (`unchangedSliceCausesNoSectionRebuildAndKeepsScroll`,
  `remainingTickUpdatesHeaderOnly...`).
- **What:** both tests pass the identical `rec`/`task` instances across renders, so they would pass
  even if `equals()` were identity-based. Production never does this: `recompute(...)` builds a **new**
  `Recommendation` every event (`AllInSlayerPlugin.java:103,251`). The zero-rebuild NFR only holds
  because `Recommendation`/`TaskData`/`SlayerLocation` are Lombok `@Data` (value `equals`) - which is
  correct today, but the test would not catch a regression (e.g. someone adds a timestamp field to
  `Recommendation`, and every kill silently starts rebuilding the loadout section -> the flicker
  ADR-0002 exists to prevent).
- **Fix:** in one NFR test, re-render with a **distinct but value-equal** `Recommendation` (and same
  task/names/prices/bankAge) and assert 0 rebuilds. Locks in the value-equality contract that the
  whole self-diff depends on. (The self-diff logic itself is sound - this is a test-adequacy gap.)

---

## Nits

- **N1. Dead token / dead state.** `SlayerTheme.STATE_MET` (`theme/SlayerTheme.java:49`) is never used,
  and `SlayerPanelState.playerSlayerLevel` (`:28`) is plumbed through every factory but never displayed.
  ui-rec §4 / P1-10 envisaged a "requirement met" green on the Slayer-level row (player level vs
  required); it was not implemented (the row is always neutral `TEXT_PRIMARY`). Either wire the
  met/unmet colour using `playerSlayerLevel` vs `task.getSlayerLevel()`, or drop the unused token+field
  so they do not imply a feature that is not there.
- **N2. A not-owned required item appears twice.** When `!owned`, the advisor puts `requiredItemId` in
  `missingUpgrades`, so the panel renders it both as a `BLOCKED` "missing" row in the Task card and as
  a muted "upgrade" row in the Loadout card (`SlayerPanel.java:505-514`). Same item, two different
  framings (red blocker vs aspirational upgrade). Consider filtering `requiredItemId` out of the
  upgrade list. (Becomes relevant after B1.)
- **N3. Long diagnostic keys clip** at ~225px (same non-wrapping `KeyValueRow` cause as S1). Dev-only
  surface, acceptable for v1; flagged for completeness.
- **N4. `gearCostRow` clamps cost to `Integer.MAX_VALUE`** (`SlayerPanel.java:693`) because `PriceLabel`
  takes an `int` while `totalGearCost` is a `long`. Gear cost > ~2.147b gp would display capped. Rare;
  acceptable, but a `PriceLabel(long)` overload would remove the cast entirely.
- **N5. `onConfigChanged` reuses `RefreshSource.MANUAL`** (`AllInSlayerPlugin.java:192`), so toggling
  developer mode shows the header meta as "manual refresh". Harmless; documented in team memory.
- **N6. `SPACE_5` strut below the loadout section remains even when Diagnostics is hidden**
  (`SlayerPanel.java:115`) -> a ~12px trailing gap for default players. Negligible.

---

## Adjudication of the six open flags

1. **Required-item OWNED/BLOCKED heuristic - BLOCKER (B1).** Not acceptable as-is: a strictly better
   signal (`missingUpgrades`, from `LoadoutAdvisor.java:89-92`) is already in the state and ignored;
   the with-loadout path can show red "missing" for an owned item, and the no-loadout path is always
   red. Smallest correct fix specified in B1.
2. **No-`html` guard excludes the `PluginErrorPanel` subtree - REASONABLE, no finding.** ADR-0004's
   rule is about *our* labels not interpreting game-supplied strings as markup. The error panel's
   `<html>` wraps only static, developer-authored copy (`applyEmptyState` `:199-213` interpolates **no**
   task/enum/untrusted data), so there is no injection surface. Scoping the guard to our labels
   (`SlayerPanelTest.java:619-628`) is correct.
3. **Visual clipping of long copy - SHOULD-FIX for the two player-facing notes (S1); NIT for the
   dev-only diagnostic keys (N3).** No horizontal scrollbar ever appears (FR-10 half satisfied); the
   "wrap/truncate cleanly" half is not met for the inline notes.
4. **Threading / EDT correctness - PASS.** `getItemComposition` (`AllInSlayerPlugin.java:287`) and
   `getItemPrice` (`:263`, and `DefaultPriceService.java:21` via the advisor) run only inside
   `recompute(...)`, which is always entered on the client thread (`clientThread.invoke*`, lines
   88-192; subscriber events fire on the client thread; `onConfigChanged` correctly bounces EDT->client
   thread, `:186-194`). `panel.render` is dispatched via `SwingUtilities.invokeLater` (`:229,:279`) ->
   EDT. The only EDT `ItemManager` touch is `getImage(...)` in `ItemManagerIconRenderer.java:33` - the
   ADR-0001 sanctioned async exception. The dim path (`:36`) registers `onLoaded` and sets the icon -
   the same mechanism `addTo` uses, so no new threading hazard; `JLabel.setIcon` self-repaints, so the
   missing explicit `revalidate/repaint` is fine. No `getItemComposition`/`getItemPrice` on the EDT.
5. **Self-diff correctness - SOUND (one test-adequacy gap, S2).** Rebuild-count probe, scroll
   capture/restore, build-once combo, and `equals()`-based slice keys are all correct. The keys
   deliberately exclude `remaining` so a kill ticks only the header (`:441-442` loadout key;
   `:333-334` where key; `:254` task key). Value-equality holds because the three model types are
   `@Data`, so a value-equal *new* `Recommendation` compares equal -> 0 rebuilds. Scroll restore is
   exact on unchanged slices and best-effort after a rebuild (revalidate is async) - acknowledged in
   the plan, acceptable. Strengthen one NFR test per S2.
6. **Diagnostics gating - PASS, no sensitive-data smell (FR-11).** `developerMode == false` calls
   `hideAndClear()` (`SlayerPanel.java:569-580`): `setVisible(false)` + `body.removeAll()` +
   `lastKey = null`, so no telemetry lingers in the tree (proven by
   `diagnosticsClearsWhenDeveloperModeIsTurnedOff`). Diagnostics is built only when devMode is on, and
   surfaces only local game/client state (status, source, menu option/action, item ids, varps,
   detector result) - nothing network/account/PII. ADR-0003 honoured.

---

## FR-1..FR-11 coverage

| FR | Status | Note |
|---|---|---|
| FR-1 single `render(SlayerPanelState)` entry | PASS | sole public update method; in-place header/mode/export + self-diff sections |
| FR-2 RefreshSource in header + debug | PASS | humane in `TaskHeader`; raw enum in `diag-source` |
| FR-3 menu-check diagnostics even on non-match | PASS | captured in `onMenuOptionClicked`; surfaced in dev mode |
| FR-4 raw slayer varps, -1 when unreadable | PASS | diagnostics rows; `readBossTargetVarbit` returns -1 on throw |
| FR-5 distinguish no-task / unsupported / supported | PASS | distinct copy + tests |
| FR-6 task intel renders without loadout | PASS | task+where render; loadout empty state; export disabled |
| FR-7 manual refresh -> MANUAL | PASS | refresh `IconButton` -> `onRefresh` -> `recompute(MANUAL)` |
| FR-8 mode shows/toggles/keeps position | PASS | `ModeSelector` + scroll/combo preserved |
| FR-9 export only with a recommendation | PASS | `setExportEnabled(rec != null)`; tests both ways |
| FR-10 stable dims, wrap/truncate, no h-scroll | **PARTIAL** | no horizontal scrollbar (PASS); long inline notes clip instead of wrapping (S1) |
| FR-11 no sensitive/network data | PASS | diagnostics = local game/client state only |

P0-1..P0-5, P1-6..P1-10, P2-11/12/13 and Decisions 1-3 are all implemented; P2-14 (icon tabs)
explicitly out of scope after the merge. Note P1-10's required-item *colour* is structurally present
but its OWNED/BLOCKED *determination* is the B1 defect.

## Test adequacy

Assertions are specific and non-vacuous (exact colours by token, combo `==` identity, rebuild-count
probe, per-field diagnostics values, dev-language absence across every enum). Two gaps: S2 (self-diff
test uses same-instance, not value-equal-distinct) and the missing B1 case (required item owned but a
better item worn). No dead code left from the rewrite beyond N1; legacy helpers/`FixedWidthPanel`
deletion is enforced by reflection in `legacyHelpersAndFixedWidthPanelAreGone`.

---

# RV2 - Re-review of the Wave 7 fixes

- Reviewer: Code Reviewer (fresh re-review; did not author the fixes)
- Date: 2026-06-29
- Scope: confirm B1, S1, S2, N1, N2, N4 are correctly resolved and nothing regressed. Files changed
  since RV1: `ui/SlayerPanelState.java`, `AllInSlayerPlugin.java`, `ui/SlayerPanel.java`,
  `ui/components/PriceLabel.java` + their tests.
- Suite: `gradlew.bat cleanTest test` -> BUILD SUCCESSFUL, **143 tests, 0 fail / 0 error / 0 skip**
  (summed from `build/test-results/test`: 29 classes, tests=143 skipped=0 failures=0 errors=0).
  Touched classes: SlayerPanelTest 32, SlayerPanelStateTest 8, PriceLabelTest 7. Claim verified.

## Verdict: APPROVE

The one Blocker is fully fixed using the authoritative ownership signal, both Should-fixes are
resolved with non-vacuous tests, and all three addressed nits are done. No regressions and no new
EDT/threading hazard. Clear to merge.

## Per-finding confirmation

- **B1 - CLEARED (full fix, incl. no-loadout).**
  - (a) Flag set authoritatively: `AllInSlayerPlugin.java:246-248` computes
    `requiredItemOwned = t.getRequiredItemId() == null ? null : owned.has(t.getRequiredItemId())` and
    passes it on the single supported-task `forTask` (`:284`). `owned` (`:242`) is the same
    `OwnedItems` the advisor consults at `LoadoutAdvisor.java:89`, so panel and advisor now agree by
    construction rather than by proxy.
  - (b) With-loadout fallback uses `missingUpgrades`: `SlayerPanel.java:813-819` returns BLOCKED iff
    `rec.getMissingUpgrades().contains(requiredItemId)`, else OWNED.
  - (c) No-loadout case is no longer always-red: `:822` returns neutral OWNED (primary, no tag) when
    there is no ownership signal and no loadout. In production the flag is always set for a present
    required item, so a genuinely-unowned item with no loadout still renders BLOCKED via the flag
    (`:808-811`); this neutral branch is the conservative "never a false red" default.
  - (d) Old worn/inventory heuristic gone: no `loadoutContains` remains (grep clean); `requiredState`
    (`:801-823`) is fully rewritten to the flag -> missingUpgrades -> neutral precedence.
  - (e) Zero blast radius: the 13-arg `forTask` delegates to the new 14-arg overload with `null`
    (`SlayerPanelState.java:116-118`); the 7/9/11-arg short forms still chain through. All prior call
    sites compile (suite green; both the short and full forms exercised in tests).
  - Tests non-vacuous: `taskSectionMarksRequiredItemOwnedEvenWhenABetterItemIsWorn` (null flag, worn
    HEAD = a different id, empty `missingUpgrades` -> OWNED; the old heuristic would have shown red),
    `taskSectionDoesNotRedFlagAnOwnedRequiredItemWithoutALoadout` (rec null, flag TRUE -> not red),
    `taskSectionMarksRequiredItemBlockedWhenGenuinelyMissing` (flag FALSE -> BLOCKED). Note: a flip in
    `requiredItemOwned` changes `requiredState`, which is in the Task self-diff key (`:256-257`), so
    ownership changes correctly re-render.

- **S1 - RESOLVED.** `wrappingNote` (`SlayerPanel.java:673-694`) is a read-only line-wrapping
  `JTextArea` (lineWrap + wrapStyleWord, opaque SURFACE_CARD, TYPE_CAPTION, no border, `getMaximumSize`
  tracks the wrapped pref height). Both the no-loadout hint (`loadout-no-loadout-hint`, `:467-470`) and
  the no-bank note (`loadout-no-bank-note`, `:656-664`) use it. No `<html>` (ADR-0004 intact); the
  `noHtmlInAnyPanelOwnedLabel` guard stays JLabel-scoped. `longPlayerNotesWrapInsteadOfClipping`
  constrains to `PANEL_WIDTH` and asserts pref height > one line and width <= panel (would clip on a
  JLabel) - it locks the wrap, not just the flags.

- **S2 - RESOLVED.** `valueEqualSliceCausesNoSectionRebuildAndKeepsScroll`
  (`SlayerPanelTest.java:487-522`) re-renders a DISTINCT but value-equal `Recommendation`/`TaskData`
  (`assertNotSame` then `assertEquals`) and asserts 0 additional section rebuilds + scroll preserved
  (40). This locks the Lombok `@Data` value-equality contract production relies on (a new Recommendation
  per event).

- **N1 - RESOLVED.** `slayerLevelRow` (`SlayerPanel.java:630-640`) tints via `playerSlayerLevel`:
  `STATE_MET` when `>= task.getSlayerLevel()`, `STATE_BLOCKED` when known-and-below, neutral when
  unknown (`< 0`). `slayerLevelRowIsGreenWhenMetAndRedWhenBelow` covers 90>=85 (green) and 70<85 (red);
  the unknown=-1 neutral path is the default elsewhere. `STATE_MET` and `playerSlayerLevel` are no
  longer dead.

- **N2 - RESOLVED.** `upgradesToShow` (`:830-848`) filters `requiredItemId` out of the Loadout upgrade
  list. `requiredItemIsNotDuplicatedInTheUpgradesList` asserts exactly one rapier row (the blocked
  Task-card row), while a genuine non-required upgrade still renders muted.

- **N4 - RESOLVED.** `PriceLabel(long)` / `setPrice(long)` (`PriceLabel.java:33-71`): within `int`
  range it delegates to the int path (identical text/colour/tooltip); above `Integer.MAX_VALUE` it uses
  `quantityToStackSize(long)` + `formatNumber(long)` tooltip + PRICE_HIGH. `gearCostRow` (`:744`) now
  passes the `long` directly - the `Integer.MAX_VALUE` clamp is gone. Both long-path tests pass.

## Regressions / EDT / threading

None. `requiredItemOwned` is a pure boolean computed inside `recompute(...)` on the client thread
(`owned.has`, `:248`); no new `ItemManager` call and nothing new on the EDT. The Task self-diff key
gained `requiredState` + `playerSlayerLevel` (`:256-257`) and the Loadout key gained `requiredItemId`
(`:446`), so ownership/level/required-item changes re-render and the value-equal NFR still holds (S2).

## Residual (out of RV2 scope, previously accepted)

N3 (dev-only diagnostic key clipping), N5 (`onConfigChanged` reuses MANUAL), N6 (~12px strut below
loadout when diagnostics hidden) remain as documented and reviewer-accepted. One informational note,
not a finding: above ~2.147b gp the long price path formats via `quantityToStackSize` rather than the
`quantityToRSDecimalStack` used below it, a negligible cosmetic difference at a boundary gear cost
essentially never reached (`quantityToRSDecimalStack` is int-only).

---

# RV3 - Re-review of the Wave 8 round-2 layout fixes (F1-F6)

- Reviewer: Code Reviewer (fresh re-review; did not author the fixes)
- Date: 2026-06-29
- Scope: confirm the three live-render defects (`round2-review.md`) are fixed and the panel-width
  invariant is locked, with no regressions. Files changed: `ui/components/{EquipmentGrid,InventoryGrid,
  EquipmentSlotCell,KeyValueRow}.java`, `ui/SlayerPanel.java` + their tests. Reviewed against
  `render-rootcause.md`, `research-r2-grids-and-layout.md` §0/§3, ADR-0002/0004.
- Suite: `gradlew.bat test --rerun-tasks` -> BUILD SUCCESSFUL, **153 tests, 0 fail / 0 error / 0 skip**
  (summed from `build/test-results/test`: tests=153 skipped=0 failures=0 errors=0; +10 vs RV2 =
  EquipmentGridTest +3, InventoryGridTest +2, KeyValueRowTest +2, SlayerPanelTest +3). All 10 new W8
  test methods confirmed present and green in the report. Claim verified.

## Verdict: APPROVE-WITH-NITS

All six fixes are correctly implemented and locked by non-vacuous tests; the interlinked render defects
(grid overflow/oversized cells, Why/Method clip, bright empty cross) are resolved and the
no-descendant-exceeds-225 invariant is enforced through a real laid-out tree. No prior-wave contract
regressed (one JScrollPane, self-diff counts, scroll capture/restore, collapse persistence, no `<html>`,
no `FixedWidthPanel`). Two nits below, neither blocking: one W8 visual choice to confirm on the live
render (N-A), one pre-existing item outside the W8 diff (N-B).

## Per-fix confirmation

- **F1 - CONFIRMED (grids capped + left-anchored).** `EquipmentGrid` (`:50` `setAlignmentX(LEFT_ALIGNMENT)`,
  `:68-72` `getMaximumSize()` returns `getPreferredSize()`) and `InventoryGrid` (`:29`, `:43-47`). The cap
  is correct and load-bearing: with `max == pref` (118 / 158, both < 225) a BoxLayout-Y cross-axis clamps
  the grid to its preferred width, so `DynamicGridLayout`'s `sw` stays 1.0 and cells hold 38x34. Verified
  the prevention is real, not just preferred-size: `filledCellsStayAtTheGridCellWidthWhenOfferedExtraWidth`
  / `cellsStayAtTheGridCellWidthWhenOfferedExtraWidth` put the grid in a 400-wide BoxLayout-Y host,
  `layoutTree`, and assert the laid-out `weapon.getWidth() == 38`. This is NON-VACUOUS - without the cap,
  BoxLayout would stretch the grid to 400 and `DynamicGridLayout` would scale the cell to ~128 (fails). The
  direct-`setSize(400)` trap the engineer flagged is correctly avoided (the test sizes the parent, not the
  grid). `maximumSizeEqualsPreferredSizeAndIsLeftAnchored` locks the cap + anchor on both grids.

- **F2 - CONFIRMED (Why/Method wrap, not KeyValueRow).** `WhereSection.rebuild` (`SlayerPanel.java:421-438`)
  renders `caption("Why"/"Method")` + `wrappingNote(value, TEXT_PRIMARY)` named `where-why` / `where-method`,
  via the existing `wrappingNote` JTextArea idiom (`:734-755`, lineWrap + wrapStyleWord, no `<html>`,
  small min width so it neither inflates the body nor clips). Short scannable rows (Slayer lvl, Weakness,
  Est. DPS, Gear cost, Bank seen) stay `KeyValueRow`. Where self-diff key is unchanged
  (`[recommendedLocation, locationReason, method]`, `:389-390`), so F2 changes only the rebuilt body, not
  the diff. `whyAndMethodWrapAsTextAreasInsteadOfClipping` asserts both are wrapping JTextAreas, pref width
  <= 225 after `setSize`, and that no `KeyValueRow` is keyed "Why"/"Method" - faithful and non-vacuous.

- **F3 - CONFIRMED (KeyValueRow overflow-proof).** Value label gets `setMinimumSize(1, h)` + `setToolTipText`
  (`KeyValueRow.java:44-45`); `setValue` refreshes the tooltip too (`:65-69`); `getPreferredSize().width`
  is `min(super.width, PANEL_WIDTH)` and `getMaximumSize().width == PANEL_WIDTH` (`:87-101`). GridBag
  (value `weightx=1, fill=HORIZONTAL`, min width 1) shrinks the value so the JLabel auto-ellipsizes while
  the key column holds `KEY_COL_WIDTH`; the row can no longer inflate the body past 225 (protects the dev
  diagnostics N3 too). `valueCanShrinkAndKeepsTheFullTextOnHover` and `rowWidthIsCappedToThePanelWidth`
  exercise a genuinely over-225 value (non-vacuous).

- **F4 - CONFIRMED functionally (empty wells differentiated; grid + rows both kept, Decision 3).** Empty
  `EquipmentSlotCell()` -> `SURFACE_PAGE` bg + no border (`:22-28`); filled -> `SURFACE_CARD` + `LineBorder`
  (`:31-40`); both keep the 38x34 `configureWell`. `LoadoutSection.rebuild` keeps BOTH the `EquipmentGrid`
  (`:544`) and the worn detail rows (`:548-557`) - the "drop grid" option was correctly NOT taken.
  `emptyWellsRecedeMutedAndBorderlessWhileFilledWellsKeepTheBorderedCard` locks the tones. See **N-A** for
  a visual caveat on the empty tone direction.

- **F5 - CONFIRMED (viewport-tracking scroll view).** `bodyWrap` is now `ViewportTrackingPanel`
  (`SlayerPanel.java:127`, class `:232-268`) implementing `Scrollable` with `tracksViewportWidth=true` /
  `tracksViewportHeight=false`, so the viewport forces the view to ~225 (every child lays out within 225)
  while height grows for vertical scroll. Still exactly ONE `JScrollPane`
  (`bodyScrollsInsideOneHiddenScrollbarPaneWithAnchoredHeader` asserts 1); self-diff and scroll
  capture/restore NFRs stay green (`valueEqualSliceCausesNoSectionRebuildAndKeepsScroll`,
  `remainingTickUpdatesHeaderOnlyAndKeepsTheCombo`); it is named distinctly, not a `FixedWidthPanel`
  resurrection (`legacyHelpersAndFixedWidthPanelAreGone` passes). The increments/`getPreferredScrollableViewportSize`
  are sensible. `scrollViewTracksTheViewportWidthSoNothingClipsSilently` locks the Scrollable contract.

- **F6 - CONFIRMED (invariants locked, assertions non-vacuous & faithful).** The laid-out-cell tests
  genuinely exercise the F1 cap (see F1 above - they would fail without it). The panel-level invariant
  `noDashboardDescendantExceedsThePanelWidthWithLongProse` first `panel.setSize(225, 4000)` then
  `layoutTree(panel)` (recursive top-down `doLayout` that propagates the viewport-forced 225 down through
  the real `JScrollPane`/`JViewport`/`ViewportTrackingPanel`/BoxLayout chain) BEFORE asserting every
  `dashboard-body` descendant `getWidth() <= 225` with long Why/Method prose - it validates the laid-out
  tree, not preferred sizes, and is non-vacuous (without F5 the unconstrained wrapping-note pref width
  would drive the view > 225 and the assertion would fail). Test count delta matches the claim exactly.

## Regressions / EDT / threading

None. The changes are construction-time layout/styling plus a passive `Scrollable` view; no new
threading, no client read on the EDT, `render(state)` remains the sole entry, `SlayerTheme` untouched,
no `<html>` reintroduced. One-scrollpane, value-equal self-diff, scroll preserve, and collapse
persistence all still green.

## Nits (non-blocking)

- **N-A. F4 empty-well tone is lighter than the card it sits on, so it may not visually "recede".**
  `EquipmentSlotCell.java:26` paints empty wells `SURFACE_PAGE` (= `DARK_GRAY_COLOR` 40,40,40), but the
  grid sits on the card body `SURFACE_CARD` (= `DARKER_GRAY_COLOR` 30,30,30) - so empty wells render
  ~10 levels LIGHTER than their surround, the opposite tone direction to `round2-review.md` F4 ("darker
  ... so the cross recedes"). The border removal does cut prominence and F1 already kills the stretched
  bright-void, so this is a minor visual judgment, not a functional defect. Recommend QA / UI Designer
  confirm on the live render; if the lighter patches read as advancing, blend to `SURFACE_CARD` (no
  border) or add a darker token. Low-risk, reversible. (The test faithfully locks the chosen tone, so
  any change is a one-line + test update.)

- **N-B. Pre-existing, OUT of W8 scope: `VERTICAL_SCROLLBAR_NEVER` + wheel may not actually scroll.**
  `SlayerPanel.java:134` keeps the vertical bar `NEVER` and relies on the wheel; `JScrollPane`'s wheel
  handler ignores wheel events when neither scrollbar is visible, so a dashboard taller than the viewport
  may not be user-scrollable (~75% confidence, from `BasicScrollPaneUI` behaviour; not run on a live
  client). This is a Wave-5 decision, NOT introduced by W8 - F5 correctly preserves height-growth and the
  programmatic scroll-restore NFR. Flagging only because F5's stated goal is "vertical scroll still works":
  QA should confirm wheel scroll on the live client; if it does not, switch the vertical policy to
  `AS_NEEDED`. Not an RV3 finding against W8.

## Residual (previously accepted, unchanged)

N3 (dev-only diagnostic key clipping - now partially mitigated by the F3 KeyValueRow width cap), N5
(`onConfigChanged` reuses MANUAL), N6 (~12px strut below loadout when diagnostics hidden) remain as
documented.

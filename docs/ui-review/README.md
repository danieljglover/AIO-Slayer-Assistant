# All-In Slayer — Side Panel UI/UX Review

- Date: 2026-06-29
- Method: design-review engagement. Top Plugin Hub plugins (by live install count)
  were studied at source, the current panel was audited at code level, then UX and UI designers
  produced recommendations. This file is the **consolidated, prioritised verdict**; the four detail
  files below are the evidence.
- Decision: **review + recommendations only — no code shipped in this engagement.**

## The detail files (evidence)

| File | What it is |
|---|---|
| [`research-top-plugins.md`](research-top-plugins.md) | Top Plugin Hub plugins by **live** install count + UI/UX patterns read from 5 comparable panels' source (Quest Helper, Inventory Setups, Party Panel, Banked Experience, Equipment Inspector). |
| [`current-ui-audit.md`](current-ui-audit.md) | 9 code-level findings on the shipped panel, each cited to file:line, each with Swing/RuneLite feasibility. |
| [`ux-recommendations.md`](ux-recommendations.md) | Information architecture, workflows, interaction model; ASCII wireframe; P0/P1/P2 table. |
| [`ui-recommendations.md`](ui-recommendations.md) | Design tokens + buildable component specs (verified against `client-1.12.31.1` bytecode); ASCII mockup. |

---

## The verdict: why it feels clunky

The 2026-06-28 redesign **delivered the information architecture the spec asked for** (one
`SlayerPanelState`, three tabs, preserved refresh source, empty states). It still feels clunky because
the **presentation and interaction choices were left as "acceptable for v1" and never revisited.** The
panel is correct but it doesn't look or behave like a RuneLite tool — it looks like a config dump.

Three root causes, each confirmed by comparing to what the most-installed plugins actually do:

1. **It's all text.** The Loadout tab is a column of `Item: Abyssal whip` strings with **zero item
   sprites** — even though every item ID is already in hand and `ItemManager` is already injected and
   already resolving names. **All 5** comparable top panels render item sprites via
   `ItemManager.getImage(...).addTo(label)`. This is the single biggest "spreadsheet, not a game tool"
   offender. (audit Finding 1)

2. **It wastes vertical space and hides its controls.** Three full-width stacked text buttons
   (Refresh / "Mode: DPS" / Export) eat ~100px of chrome before any task data appears, where the
   convention is a ~24px compact icon bar. The "Mode: DPS" button hides that it even toggles. (audit
   Findings 2, 6, 7)

3. **It leaks the machine at the player.** `Source: ITEM_CONTAINER | Updated: 21:48:12`, raw enum
   names, and a permanent **Debug tab** of varps and menu actions. **None** of the top plugins ship a
   player-facing debug surface. (audit Findings 3, 5)

Supporting issues: two competing row systems + `<html><div style='width:Npx'>` width hacks make the
tabs look like two different plugins (Finding 4); a manual **Refresh** as a primary action implies the
panel is stale until pressed, undermining trust that it's live, when it already auto-updates on every
relevant event (Finding 7); and a full teardown+revalidate of all three tabs on every event (plus an
unfiltered `GameStateChanged` recompute) makes it feel twitchy (Finding 9).

The two designers **independently converged** on the same redesign: one scrollable dashboard, item
sprites, a compact icon action bar, a visible segmented DPS/Cost control, a humanised header, and
Debug gated behind a developer config.

---

## How the top plugins set the bar

From live install counts pulled today (`api.runelite.net/pluginhub`, 28.4M total installs) and source
read from the 5 comparable side panels:

- **Item sprites everywhere.** `ItemManager.getImage(id, qty, stackable).addTo(label)` — the
  `AsyncBufferedImage` repaints itself when loaded. Inventory Setups, Party Panel, Banked Experience,
  Equipment Inspector, Quest Helper all do this. We don't.
- **Compact icon action bars,** not stacked text buttons — `SwingUtil.removeButtonDecorations` +
  16px icon + tooltip + hover colour (Quest Helper, Inventory Setups).
- **Reactive panels.** **None of the 5** ships a manual Refresh button; they rebuild/recolour from game
  events.
- **Brand colour for real state only** (met/unmet, active), not decorative headings (Quest Helper,
  Banked Experience).
- **`PluginErrorPanel`** for empties, **`FontManager`** RuneScape fonts, and per-row right-click → Wiki
  via `LinkBrowser` (Equipment Inspector) are the shared idioms that make a panel feel native.

Every API needed is available — the client resolves to `client-1.12.31.1`, verified from bytecode.
(One correction the UI designer caught: `DimmableMaterialTab` does **not** exist in this client; icon
tabs are still possible but dimming would be hand-rolled, so text tabs are recommended for now.)

---

## Prioritised roadmap (consolidated — UX + UI + audit, deduplicated)

Priority = player-impact × confidence. Effort: S ≈ hours, M ≈ a day, L ≈ multi-day. Every item maps to
an audit Finding and a top-plugin precedent.

### P0 — the "no longer clunky" core (do these together)

| # | Change | Why | Evidence | Effort |
|---|---|---|---|---|
| P0-1 | **Render gear as item sprites** — worn rows/grid, inventory grid, required item, missing upgrades — via `ItemManager.getImage(...).addTo()`. Use `LoadoutItemRow` (sprite + name + slot + price) and/or `EquipmentGrid`. | The biggest visual gap; players read gear by sprite, not by reading names. | Finding 1; all 5 top panels | M |
| P0-2 | **Collapse the 3 stacked full-width buttons into one ~24px icon action bar** (Refresh + Export as borderless 16px icons with tooltips). | Reclaims ~100px; reads native. | Findings 2,7; Quest Helper, Inventory Setups | M |
| P0-3 | **Segmented `[DPS][Cost]` control** (both shown, active = `BRAND_ORANGE`) replacing the relabel button, placed by the loadout it governs. | A relabel button hides that it toggles and what the alternative is. | Finding 6 | M |
| P0-4 | **Humanise the header** — `updated 21:48 · helm check`; drop `Source:`, raw enums, and seconds. | Leaked internal state machine reads as unfinished. | Finding 5 | S |
| P0-5 | **Gate the Debug tab behind a developer config** — default players see only player-facing content; diagnostics fully preserved when enabled. ⚠️ *deviates from the approved spec — see decisions below.* | No top plugin ships a player-facing debug surface. | Finding 3 | S |

### P1 — finish the interaction model and consistency

| # | Change | Why | Evidence | Effort |
|---|---|---|---|---|
| P1-6 | **Merge Task + Loadout into one scrollable dashboard** ordered by the 3 player questions (task → where/how → loadout); remove the Task/Loadout tab split. ⚠️ *see decisions.* | Tabbing within one task forces a click between two halves of one decision. | Finding 4; Quest Helper, Equipment Inspector, Banked Experience, Inventory Setups | M |
| P1-7 | **Reactive by default; demote Refresh** to the icon-bar fallback (keep it — spec FR-7 — just not as the headline). | A prominent Refresh implies the panel is stale until pressed. | Finding 7 | S |
| P1-8 | **Non-destructive re-render** — preserve scroll, selected location, mode, combo-box popup/focus; rebuild only the changed section; **filter `GameStateChanged` to `LOGGED_IN`**; build the location combo once. | Full teardown every event causes flicker/scroll jumps and drops the combo mid-interaction. | Finding 9 | M |
| P1-9 | **Player-language empty/error states** for every state, each naming the actual lever (no "then refresh", no `UNSUPPORTED_TASK`). | Dead-ends and dev language. | Findings 5; spec FR-5/6 | S |
| P1-10 | **Encode owned/missing with colour** on required-item and missing-upgrade rows; reserve `BRAND_ORANGE` for real state (not every section heading). | Player can't tell at a glance what they own / what's missing. | Finding 4 + colour rules; Quest Helper, Banked Experience | S |

### P2 — polish

| # | Change | Why | Evidence | Effort |
|---|---|---|---|---|
| P2-11 | **Unify on one `KeyValueRow`/`SectionCard`**; delete `FixedWidthPanel`, `fixedWidth()`, `KEY_WIDTH/VALUE_WIDTH` math, and all `<html><div style='width:Npx'>` hacks; widths from layout managers. | Two row systems look like two plugins; pixel math is brittle. | Finding 4 | M |
| P2-12 | **Per-row right-click → Wiki** lookup on item rows (`LinkBrowser` → `Special:Lookup`). | Cheap, high-utility, expected. | Equipment Inspector | S |
| P2-13 | **Collapsible sections** (chevron + bold header). | Long scroll on small client windows. | Quest Helper, Banked Experience | M |
| P2-14 | **Icon tabs** — only if assets exist; dimming is hand-rolled (no `DimmableMaterialTab`). Low ROI; text tabs are fine. | Minor polish. | Finding 8 | M |

**Sequencing:** P0-1 + P0-2 + P0-3 land together and deliver most of the win. P0-4/5 are cheap and
high-trust. If P1-6 (the merge) is approved, do it with P0-1 so the sprites have their home. Everything
else is incremental and independently shippable.

---

## Decisions — CONFIRMED 2026-06-29 (by Daniel Glover)

All three product calls were confirmed as recommended. These are now the binding choices for any
implementation track:

1. ✅ **Gate the Debug tab behind a developer config (P0-5).** Default players see only player-facing
   content; diagnostics are fully preserved when developer mode is on. This formally supersedes the
   approved spec's "keep Debug player-visible" decision (spec lines 280, 363, 725) — the helm/gem
   diagnosis phase is treated as complete.
2. ✅ **Full merge to one scrollable dashboard (P1-6).** Remove the Task/Loadout tab split; order by the
   3 player questions (task → where/how → loadout). (2-tab fallback is abandoned.)
3. ✅ **Worn gear as sprite+name rows as primary (P0-1)**, with the equipment-tab grid available as a
   secondary at-a-glance view. Both are specced in `ui-recommendations.md`.

---

## Next step (optional)

If you want this built, the natural follow-on is a **T1/T2 implementation track**: turn the P0 (and
chosen P1) items into a TDD build plan against `SlayerPanel`, `SlayerPanelState`, and
`AllInSlayerPlugin`, with the component specs in `ui-recommendations.md` as the build contract. Say the
word and I'll spin the team back up for the build (it would re-enter at Gate 0/scope).

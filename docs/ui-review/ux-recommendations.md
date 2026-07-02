# All-In Slayer - Side Panel UX Recommendations (Board UX1)

- Author: UX Designer
- Date: 2026-06-29
- Scope: Information architecture, user workflows, and interaction model for the RuneLite side panel. Structure, flow, and behaviour - NOT pixel-level visual styling (that is the UI Designer's layer; see "Hand-off to UI Designer").
- Inputs read: `docs/ui-review/current-ui-audit.md` (9 findings, Frontend Engineer), `docs/ui-review/research-top-plugins.md` (top-5 panel patterns), the approved spec `docs/superpowers/specs/2026-06-28-all-in-slayer-side-panel-design.md`, `SlayerPanel.java`, and the data model (`TaskData`, `Recommendation`, `SlayerLocation`, `SlayerPanelState`).

## The player and the job

One persona: a player mid-Slayer-grind who has just been assigned (or is mid-) a task and wants to start killing efficiently with gear they already own. They open the panel to answer three questions, in this fixed priority order:

1. **What is my task and how many are left?**
2. **Where do I kill them, and how (cannon / burst / method)?**
3. **What do I wear, from what I own?**

These three are not three separate jobs. They are one continuous decision made at the start of a trip: *"Abyssal demons, 142 left -> cannon them in the Catacombs -> wear my melee set."* The panel's job is to let the player read that decision top-to-bottom in one pass, then go. Every click, tab-switch, or "where am I" moment inserted between those three answers is friction on the single most common interaction the plugin has.

That framing drives every recommendation below.

---

## 1. Information architecture decision

### Decision: merge Task + Loadout into ONE scrollable dashboard; remove the Task/Loadout tab split; gate Debug behind a developer config.

The current panel splits the player's one decision across three tabs (Task / Loadout / Debug, `SlayerPanel.java:90-95`). Question 2 (where/how) lives on the Task tab; question 3 (what to wear) lives on the Loadout tab. To plan a trip the player must click between two tabs that answer two halves of the same thought. That is the structural root of "clunky."

**Why merge, argued against the comparables (research-top-plugins.md):**

- **Top panels tab between distinct entities or workflows, never between facets of one current thing.** Party Panel tabs per *party member*; Loot Logger tabs per *loot category*; Quest Helper cards between *list / detail / settings*. Within a single entity, everything sits on one surface. All-In Slayer has exactly **one current task**. Splitting that one task's intel from its loadout across tabs is tabbing *within* a single entity - which none of the five do.
- **Quest Helper is the closest analog and it does not tab here.** Its quest *detail* view scrolls the active step and that step's *requirements* together on one surface (`QuestRequirementsPanel` lives inside the step/overview view, research 2.1). Our "task" is the detail view; our "loadout" is its "requirements." They belong on the same scroll.
- **Equipment Inspector and Banked Experience are single scrolling surfaces** (item rows + total; selection grid + breakdown). Item-heavy advice is presented as one continuous, scrollable column, not paginated behind tabs.
- **Inventory Setups' detail view shows inventory AND equipment together** on one surface; it never tabs gear away from inventory.

A vertical scroll is cheap and native in a RuneLite panel; a tab-switch is a mode change. We should pay the cheap cost (scroll) to remove the expensive one (mode change between two halves of one decision).

**Content order on the single dashboard** (maps directly to the three questions):

1. Header (always visible, does not scroll): task name + remaining + humanised "updated" meta.
2. **TASK** section -> question 1 detail (Slayer level, weakness, required item with owned/missing state).
3. **WHERE & HOW** section -> question 2 (recommended location + capability chips + why + method + location selector).
4. **LOADOUT** section -> question 3 (mode control, worn gear, inventory, DPS, cost, missing upgrades, bank age).

The player gets questions 1 and 2 without scrolling; they scroll only into the deeper "what do I own" detail. Correct prioritisation, and it matches how they read the decision.

### Fate of the Debug tab: hide it behind a developer config (default players never see it).

**Recommendation: remove Debug from the default player view; render it only when `config.developerMode()` is true** (audit Finding 3 confirms this is EASY and reversible - gate the `tabs.addTab(...)` call). When enabled, surface it as a collapsed, visually-subordinate "Diagnostics (developer)" section at the very bottom of the same scroll, or as a second tab if the team prefers hard separation. Default players see only player-facing content.

Justification from the research: **none of the top plugins ships a player-facing debug surface** (research Section 3, item 5). Internal telemetry (raw varps, menu actions, mapped item IDs) presented as a primary, permanent tab signals "unfinished tool" and dilutes the panel's purpose - it is a live contributor to the "clunky/unintuitive" feeling.

**This is a deliberate deviation from the approved spec and needs PM sign-off.** The spec (lines 280, 725) explicitly says the Debug tab "is part of the product" and "do not hide it behind a developer config while the helm/gem update issue is being diagnosed." That decision was correct *for the diagnostic phase*. The audit (Finding 3, 2026-06-29) notes that phase is "presumably ending," and the team lead's brief asks me to recommend gating it. So: gate it, but flag this as a product call at the gate. The diagnostic capability is fully preserved (same content, same state) - it just stops being the first thing a new player sees. See the acceptance-criteria trace for the exact spec lines this revises.

### Options considered (diverge -> converge)

| Option | Verdict |
|---|---|
| A. Keep 3 tabs (Task / Loadout / Debug) - status quo | Rejected. Tabs within one entity; forces a click between questions 2 and 3; ships dev telemetry to players. |
| **B. One scrollable dashboard (Task+Loadout merged), Debug gated** | **Chosen.** Matches player mental model and the dominant comparable pattern; removes the inter-question click and the dev tab. |
| C. Two tabs (Task / Loadout), Debug gated | Fallback if the team wants minimal change from the approved spec. Still splits one decision across a mode change; weaker than B but far better than A. |

If the team rejects a full merge at the gate, **fall back to C, not A.**

---

## 2. Proposed layout (ASCII wireframe)

Low-fidelity grey-box for the main `TASK_WITH_LOADOUT` state, within RuneLite's fixed ~225px panel width. Boxes are structure, not styling.

```
+-------------------------------------+
| All-In Slayer            (o) (^)    |  Title + compact icon bar:
+-------------------------------------+    (o)=refresh  (^)=export
| Abyssal demons                      |  Q1  task name (largest, bold)
| 142 remaining                       |      remaining (prominent)
| Updated 21:48 - gem check           |      humanised meta (small, muted)
+=====================================+
| TASK                                |  section label (subtle)
|   Slayer lvl   85                    |
|   Weakness     Slash                 |
|   Required     [img] Facemask  OWNED |  icon + owned/missing state
+=====================================+
| WHERE & HOW                         |  Q2
|   Catacombs of Kourend               |  recommended location (primary)
|     [multi] [cannon] [burst]         |  capability chips
|   Why   Best XP here, cannon works   |
|   Method  Cannon + melee, Slayer helm|  wraps cleanly, no h-scroll
|   Location [ Catacombs of Kourend v ]|  selector (persists across renders)
+=====================================+
| LOADOUT   Melee        [DPS][Cost](^)|  Q3  collapsible header
|                                      |    segmented mode (DPS active)
|   [img] Slayer helm (i)        Head  |  worn gear = sprite + name + slot
|   [img] Abyssal whip           Weapon|
|   [img] Fire cape              Cape  |
|   [img] Amulet of torture      Amulet|
|   ... (remaining filled slots)       |
|                                      |
|   Inventory                          |
|   [img][img][img][img][img]          |  sprite grid + hover tooltips
|                                      |
|   Est. DPS     12.4                   |
|   Gear cost    28.4M gp               |
|                                      |
|   Upgrades you do not own             |
|   [img] Ghrazi rapier   (+ DPS)       |  missing = muted/red state
|                                      |
|   Bank seen    12m ago                |
+-------------------------------------+
        (single vertical scroll)
```

Notes on structure (the "what," not the "how"):

- **Header is anchored** (does not scroll). Action affordances are a compact icon bar in the title row (refresh + export), not stacked full-width text buttons. This reclaims the ~100px of vertical chrome the current three-button stack eats (audit Finding 2).
- **Worn gear renders as icon rows (sprite + item name + slot)**, modelled on Equipment Inspector's `ItemPanel` (research 2.5). The player is *assembling* this set from their bank, so they need the **name** to find each item, not just a pretty gear-tab silhouette. An equipment-tab grid (Party Panel style) is a valid later enhancement but is worse for "find this in my bank."
- **Inventory renders as a compact sprite grid** with tooltips (Party Panel `PlayerInventoryPanel`, research 2.3) - it mirrors the in-game inventory mental model and is denser than rows.
- **Required item, worn items, and missing upgrades carry an owned/missing visual state** (Quest Helper met/unmet colouring, research 2.1). Reserve the brand colour for real state, not decoration.
- The **mode control is scoped to the LOADOUT section header** because that is the only content it changes - proximity signals the relationship honestly (it does not relabel the whole panel).

The other three states reuse this skeleton with sections collapsed/replaced - see Section 4.

---

## 3. Interaction model

### 3.1 Reactive by default; Refresh is a demoted fallback, not the headline

The panel already auto-recomputes on startup, varbit, chat, menu-check, item-container, and game-state events (audit Finding 7, `AllInSlayerPlugin.java:110,125,134,153,169,176`). **None of the top five plugins uses a manual Refresh button** (research Section 3, item 3) - they all rebuild/recolour from game events.

- **Stance:** the panel must *feel* live. The remaining count ticking down as the player kills is the strongest "this is alive" signal - it must update without a teardown so it does not flicker.
- Spec FR-7 still *requires* a manual refresh control, so keep one - but **demote it from a full-width primary button to a small icon in the header bar** (audit Finding 7 confirms EASY). It is reassurance ("nudge it if you don't trust it"), not a required step. A prominent Refresh implies the panel is stale until pressed, which undermines confidence that it is live.

### 3.2 DPS/Cost: a visible segmented control, not a relabelling button

Replace the `Mode: DPS` relabel button (audit Finding 6) with a **two-segment selector showing BOTH options with the active one visually dominant** (`[DPS][Cost]`). A button that shows only the current value gives no signal that it toggles or what the alternative is - the player has to click and observe to discover a second mode exists (Norman: visibility of state + a signifier for the alternative).

- **Placement: inside the LOADOUT section header**, adjacent to the gear it governs. It changes the recommended gear, the DPS number, and the cost - all in that section - so it sits with what it affects, not floating in the global header claiming to change everything.
- Toggling mode must **keep the player's scroll position and the rest of the panel stable** (spec FR-8: "keep the selected tab unchanged"); in the single-scroll model that generalises to "keep scroll position and section states unchanged."

### 3.3 Location selection is a local refinement, not a global recompute

The location selector (`JComboBox`, currently rebuilt on every render, audit Finding 9) must:

- **Default to the recommended location**, with the recommendation shown above it and the selector below for override.
- **Build once and reuse** - update its model/selection in place, never recreate it each render (recreating it can drop the open popup and focus mid-interaction).
- **Update only the WHERE & HOW derived bits** (chips, why, and the loadout if location affects it) on change - a partial re-render (the pattern already in `selectLocation` `SlayerPanel.java:380-393`), NOT a full teardown that flips the header meta to a "location change" source and resets scroll.
- The player's **selected location must persist across reactive re-renders** - if a kill ticks the remaining count, their chosen location must not snap back to the recommendation.

### 3.4 Persistence across re-renders (the panel re-renders constantly)

Because the panel reacts to nearly every event, re-render must be non-destructive. The following must survive every re-render:

- **Scroll position** (audit Finding 9: full teardown causes scroll jumps).
- **Selected location** (3.3).
- **Mode selection** (session state).
- **Collapsed/expanded section states** (if Section 5's collapsibles ship).
- **Combo box selection, popup, and focus** (do not recreate the component).
- **Selected tab**, if the 2-tab fallback (Option C) is chosen.

Mechanism (structural requirement for the FE): rebuild only the section whose data changed, or update components in place, instead of `removeAll()` + recreate on the whole tree. Also **filter `GameStateChanged` to `LOGGED_IN`** so login/loading blips do not churn a rebuild and flicker the meta line (audit Finding 9).

### 3.5 What auto-updates vs what must not

| Auto-updates reactively | Must NOT change on re-render |
|---|---|
| Task name, remaining count | Player's selected location |
| Weakness, required item + owned/missing state | Scroll position |
| Recommended location, chips, why, method | Mode selection |
| Loadout gear, DPS, gear cost, missing upgrades | Collapsed/expanded states |
| Bank age, humanised updated time/source | Combo box popup/focus |

---

## 4. Workflow walkthroughs (all four states)

Empty/error copy below is in **player language** - the direct fix for audit Finding 5 (developer-language leakage). The current copy leaks raw enums (`Source: ITEM_CONTAINER`), the word "Source:", `UNSUPPORTED_TASK`, and "then refresh" (which contradicts the reactive model). Replace per the table at the end of this section.

### State A - NO_TASK

- **Sees:** Header "No Slayer task" + humanised meta "Updated 21:48 - startup" (no remaining count). Body is a single `PluginErrorPanel`. No WHERE & HOW or LOADOUT sections.
- **Copy:** title "No Slayer task" / body "Get a task from a Slayer master, or check your enchanted gem or Slayer helm, to see advice here." (Actionable; no "refresh" - the panel reacts on its own.)
- **Does:** gets or checks a task; the panel updates itself. The humanised meta proves the plugin is watching even with no task (this is why we keep the meta line in the no-task state - spec line 343).

### State B - UNSUPPORTED_TASK

- **Sees:** Header "Task not yet supported" + remaining count if known + humanised meta. Body explains the data gap; the count stays visible.
- **Copy:** title "Task not in our data yet" / body "We don't have advice for this task yet (target #1234). Your task count still shows above. We add tasks over time." (Reassures it is a data gap, not a bug; never shows the raw `UNSUPPORTED_TASK` string or `Unsupported Slayer target: 1234` framing.)
- **Does:** nothing they can fix - the goal is to reassure, not dead-end. The raw target id lives in the dev Diagnostics section for the maintainer.

### State C - TASK_WITHOUT_LOADOUT

- **Sees:** Full header (task + remaining + meta). **TASK and WHERE & HOW sections render in full** (spec FR-6 - task intel must not disappear just because gear advice is missing). The LOADOUT section shows an inline empty state. The mode control stays present (toggling Cost may produce a loadout). Export is disabled *and looks disabled*.
- **Copy (no loadout):** title "No owned loadout found" / body "We couldn't build a set from your bank, inventory, and worn gear. Scan your bank, or switch to Cost mode for cheaper options."
- **Copy (no bank seen yet):** "We haven't seen your bank yet - advice uses what you're wearing and carrying. Open your bank to improve it." (Non-blocking warning, names the actual lever.)
- **Does:** scans bank or toggles Cost; the loadout section fills in reactively without losing scroll or the chosen location.

### State D - TASK_WITH_LOADOUT

- **Sees:** the full dashboard from Section 2. Export enabled.
- **Does:** reads top-to-bottom; optionally overrides location; toggles DPS/Cost to compare; exports to Inventory Setups; goes to kill. Subsequent kills tick the remaining count live with no flicker and no scroll jump.

### Developer-language -> player-language mapping (audit Finding 5)

Render the meta line as `Updated 21:48 - <humanised source>` and drop the word "Source:". Raw enum stays only in the dev Diagnostics section.

| `RefreshSource` (raw) | Player-facing text |
|---|---|
| STARTUP | startup |
| MANUAL | manual refresh |
| VARBIT | task changed |
| CHAT | task message |
| MENU_CHECK | gem / helm check |
| ITEM_CONTAINER | bank / inventory update |
| GAME_STATE | login |
| MODE_TOGGLE | mode change |
| LOCATION_SELECT | (hide - local change, do not surface) |

---

## 5. Prioritised UX recommendations (P0 / P1 / P2)

Effort is rough (S/M/L). Evidence cites audit findings by number and research plugins by name.

| # | Recommendation | Player problem it solves | Evidence | Effort |
|---|---|---|---|---|
| **P0-1** | Merge Task + Loadout into one scrollable dashboard ordered by the 3 questions; remove the Task/Loadout tab split | Tabbing within one task forces a click between "where/how" and "what to wear" - two halves of one decision | Quest Helper (detail view), Equipment Inspector, Banked Experience, Inventory Setups (single surfaces); audit Finding 4 | M |
| **P0-2** | Render loadout gear as item **sprites** (worn rows, inventory grid, required item, missing upgrades), not text | A wall of `Item: name` reads like a config dump; players recognise gear by sprite instantly | audit Finding 1; Inventory Setups, Party Panel, Equipment Inspector, Banked Experience (all 5 use `ItemManager` sprites) | M |
| **P0-3** | Reactive by default; demote Refresh from full-width primary button to a small header icon | Prominent Refresh implies the panel is stale until pressed, undermining trust that it is live | audit Finding 7; research "none of the 5 uses a manual Refresh" | S |
| **P0-4** | Humanise the header meta; remove all developer language (raw enums, "Source:") | "Source: ITEM_CONTAINER" is meaningless and reads as a leaked internal state machine | audit Finding 5 | S |
| **P0-5** | Hide Debug behind a developer config; default players see only player-facing content | Internal telemetry as a primary tab signals "unfinished tool" and dilutes purpose | audit Finding 3; research "none of the top plugins ship a debug surface" (needs PM sign-off - revises spec) | S |
| **P1-6** | Segmented `[DPS][Cost]` control (both options shown, active dominant), scoped to the Loadout section | A relabel button hides that it toggles and what the alternative is | audit Finding 6; Quest Helper (brand colour = active state) | M |
| **P1-7** | Collapse the 3 stacked full-width buttons into a compact header icon bar (refresh, export) | ~100px of chrome before any task data appears | audit Finding 2; Quest Helper, Inventory Setups (icon action bar) | M |
| **P1-8** | Preserve scroll / location / mode / collapsed state across re-renders; partial re-render not full teardown; filter `GameStateChanged` to LOGGED_IN | Panel feels twitchy - flicker, scroll jumps, combo box dropping mid-interaction | audit Finding 9 | M |
| **P1-9** | Player-language empty/error states for every state, each naming the actual lever | Dead-ends and dev language; player does not know what to do next | audit Finding 5; Inventory Setups (`PluginErrorPanel`); spec FR-5/FR-6 | S |
| **P1-10** | Encode owned/missing with colour on required-item and missing-upgrade rows; reserve brand colour for real state | Player cannot tell at a glance if they own the required item or what is missing | Quest Helper (met/unmet), Banked Experience (state colours); audit note on decorative `BRAND_ORANGE` | S |
| **P2-11** | Collapsible sections (Task / Where & How / Loadout) with chevron + bold header | Long single scroll on small client windows | Quest Helper, Banked Experience (collapsible sections) | M |
| **P2-12** | Per-row right-click -> Wiki lookup on item rows | Player wants item detail/price fast without alt-tabbing | Equipment Inspector, Quest Helper (`LinkBrowser`) | S |
| **P2-13** | Unify on one item/key-value row component; drop HTML width hacks and `FixedWidthPanel` | Two row systems make the tabs look like two different plugins; brittle pixel math | audit Finding 4 | M |
| **P2-14** | Icon tabs (only if Debug stays as a dev tab AND assets exist) | Minor polish; text tabs are already legible | audit Finding 8 (gated on assets) | M |

**Sequencing:** P0-1 and P0-2 deliver the bulk of the "no longer clunky" win and should land together (the merge gives the loadout sprites a home; the sprites give the merged dashboard its payoff). P0-3/4/5 are cheap and high-trust. P1-6/7/8 finish the interaction model. P2 is polish.

---

## Hand-off to UI Designer (structural requirements to realise visually)

I own structure/flow/interaction; you own sprites, icon styling, colour, fonts, and exact component geometry. The structure you must visually realise:

- **One scrolling dashboard**, sections in this order: Header (anchored) -> TASK -> WHERE & HOW -> LOADOUT. Header never scrolls.
- **Compact icon action bar** in the title row carrying refresh + export (you choose the glyphs, hover, and disabled treatment).
- **Loadout worn rows must carry: sprite + item name + slot label + an owned-state.** Inventory must be a sprite grid with tooltips. (Densities/sizes are yours; the fields are mine.)
- **Required item and missing-upgrade items need a distinct owned vs missing visual state.** Brand colour is for real state only, not section-heading decoration.
- **The mode control must show both `DPS` and `Cost` with the active one visually dominant** - a selector, not a relabel.
- **Capability chips** (multi / cannon / burst / konar) stay as compact chips on the recommended location.
- Everything must wrap or truncate cleanly inside ~225px with no horizontal scroll (spec FR-10).

I do not specify your colours, sprites, fonts, or spacing - flag any structural requirement above that fights the visual language and we will resolve it.

---

## Acceptance-criteria trace (and the one deviation)

Mapping to the approved spec's Success Criteria and Functional Requirements. Where this redesign satisfies a criterion, the section is named; the single conflict is flagged for the gate.

| Spec criterion | Where this design satisfies it |
|---|---|
| Answer task / count / where / loadout without dense HTML (Success Criteria) | Sections 1-2: single dashboard ordered by the 3 questions; sprites replace HTML text rows |
| Single `SlayerPanelState` render (Success / FR-1) | Unchanged - all states in Section 4 render from one state object |
| No-task state keeps last source + debug (Success / FR-5) | State A keeps the humanised meta; dev Diagnostics keeps raw varps |
| Distinguish unsupported vs no-task (FR-5) | States A and B are visually and textually distinct (Section 4) |
| Task intel visible without loadout (FR-6) | State C: TASK + WHERE & HOW render in full; only LOADOUT shows empty state |
| Manual refresh control exists (FR-7) | Section 3.1: kept as a demoted header icon |
| Mode toggle shows + changes mode, keeps tab/position (FR-8) | Section 3.2: segmented control; scroll/state preserved |
| Export enabled only with a recommendation (FR-9) | State D enables export; A/B/C disable it and show it disabled |
| Stable layout, wrap/truncate within panel width (FR-10) | Section 2 + Hand-off: no horizontal scroll; anchored header |
| Tab persists across render (Tab Selection) | Section 3.4: in the merged model this becomes scroll + state persistence; under fallback C, selected tab persists |
| **Debug tab always visible to players (lines 280, 725)** | **DEVIATION.** P0-5 / Section 1 recommend gating Debug behind `config.developerMode()`. The diagnostic content is fully preserved; it is removed from the default player view. This revises spec lines 280, 363, and 725 and **requires PM sign-off at the gate.** |

**Open questions for the gate:**

1. PM decision on gating the Debug tab (the deviation above). The diagnostic phase appears to be ending (audit Finding 3); is it ended enough to demote Debug to a developer config?
2. Full merge (Option B) vs the 2-tab fallback (Option C) if the team wants minimal change from the approved spec. My recommendation is B.
3. Worn gear as icon+name rows (my recommendation, optimised for "find it in my bank") vs an equipment-tab grid (prettier, worse for that job) - confirm the trade-off with the UI Designer.
```
---
status: accepted
---

# Plain-text labels, a token holder, and client-thread price plumbing replace the HTML/width hacks

Three coupled choices that reverse visible parts of the shipped panel, recorded together so a future
reader does not "fix" them back.

**1. Labels carry plain text, never `<html>`.** The current panel wraps values in
`<html><div style='width:Npx'>...</div></html>` and hand-escapes `<`, `>`, `&`
(`SlayerPanel.java:435,517,595,726`). A Swing `JLabel` only interprets its text as markup when the
string begins with `<html>`; plain text is rendered literally. We pass plain text and set widths via
layout managers, which (a) removes the brittle pixel math and the two competing width systems the
audit flagged (Finding 4), and (b) makes untrusted task/item strings inert by construction. Do NOT
reintroduce `<html>` to style or wrap a label: it re-enables markup interpretation of game-supplied
strings and reintroduces the escaping burden. The existing "escapes HTML" tests change meaning - they
now assert the literal string is present and is not interpreted.

**2. A single token holder `SlayerTheme` is the only source of colours, fonts, spacing, sizes.**
Components read `SlayerTheme` / `FontManager` constants (per `ui-recommendations.md` §1), never
`new Font(...)` or ad-hoc RGB. Brand orange is reserved for real state (active mode, active state,
recommended location), not section headings. `FixedWidthPanel`, `fixedWidth()`,
`KEY_WIDTH/VALUE_WIDTH`, `row()`, `section()`, `chips()` and the HTML helpers are deleted; one
`KeyValueRow`/`SectionCard`/`Tag` system replaces the two divergent ones.

**3. Per-item GE price is plumbed through the state on the client thread.** `Recommendation` only
carries `totalGearCost` (aggregate); per-item price needs `ItemManager.getItemPrice(int)` - verified
present in `client-1.12.31.1`. To honour "client reads on the client thread" we resolve prices
alongside `collectNames(...)` into a new `Map<Integer,Integer> itemPrices` on `SlayerPanelState`
(mirroring `itemNames`), and `PriceLabel` reads from the state. Price is treated as optional: a
missing/zero price renders muted, so the P0 sprite work does not block on it. Inventory quantities
are not in the model (`Recommendation.inventory` is `List<Integer>`), so inventory sprites render at
qty=1 with no stack number; adding stack counts is a deliberate out-of-scope follow-up.

---
status: accepted
---

# Item sprites render through an injected `ItemIconRenderer` seam, not `ItemManager` directly

The redesign must draw item sprites in leaf Swing components (`LoadoutItemRow`,
`EquipmentGrid`, `InventoryGrid`) via `ItemManager.getImage(id, qty, stackable).addTo(label)`,
but those components must stay unit-testable headless with no live client. Verified against
`client-1.12.31.1`: `AsyncBufferedImage`'s only public constructor is
`AsyncBufferedImage(ClientThread, int, int, int)`, so a test cannot construct or stub a returned
image, and `ItemManager` itself is a heavy client service. We therefore put a one-method seam
between the components and `ItemManager`:

```java
public interface ItemIconRenderer { void render(JLabel label, int itemId, int quantity, boolean stackable); }
```

Production binding `ItemManagerIconRenderer` wraps the injected `ItemManager`
(`itemManager.getImage(itemId, quantity, stackable).addTo(label)`); tests inject a no-op /
recording fake. `SlayerPanel` gets an `@Inject` constructor taking `ItemManager` (Guice builds the
production renderer) plus a package-private constructor taking an `ItemIconRenderer` for tests.

Why this and not the alternatives: (a) passing `ItemManager` straight into the components couples
every component test to a service whose return value cannot be faked; (b) pre-resolving
`AsyncBufferedImage`s into `SlayerPanelState` on the client thread would put mutable AWT image
objects into an immutable DTO and cross the thread boundary awkwardly, when `getImage(...).addTo(...)`
is already EDT-safe (it self-loads via the client thread internally, the idiom all 5 researched
panels use). Item *names* and *prices* still resolve on the client thread into the state (see
ADR-0004); only the image bytes flow through this seam, called on the EDT.

Consequence: leaf components depend on `ItemIconRenderer` + an item id, never on `ItemManager`;
their tests assert "asked to render id X (qty, stackable)" and structural state, never pixels.
Actual sprite pixels are covered by the manual checklist in `docs/plan.md`.

## Build note (T02, 2026-06-29)

As built, the seam carries a fifth `dim` flag - `void render(JLabel label, int itemId, int quantity,
boolean stackable, boolean dim)` - extending the illustrative 4-arg signature above. The UPGRADE state
(plan T10) needs a dimmed sprite, and only the seam holds the `AsyncBufferedImage`, so dimming belongs
here, not in the components. The production `ItemManagerIconRenderer` applies `ImageUtil.alphaOffset(img,
-80)` in an `onLoaded` callback when `dim` is true (you cannot dim bytes that have not loaded yet),
otherwise it uses the plain `getImage(...).addTo(label)` idiom. Fakes record `(id, qty, stackable, dim)`.

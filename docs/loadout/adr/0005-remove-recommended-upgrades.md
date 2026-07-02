---
status: accepted
---

# Remove the recommended-upgrades concept entirely

The "upgrades you do not own" feature is removed from the model, engine, and UI. `Recommendation`
loses its `missingUpgrades` field; `LoadoutAdvisor` no longer builds any missing/BIS-gap list;
`LoadoutItems.ids(...)` no longer adds upgrade ids; `SlayerPanel` loses the upgrades section
(`upgradesToShow`, the UPGRADE rows) and the required-item ownership logic simplifies to the
authoritative `SlayerPanelState.requiredItemOwned` flag only (flag non-null -> OWNED/BLOCKED; flag null
-> neutral OWNED), dropping the old `missingUpgrades` fallback.

Why: the product goal is 100% focus on the best loadout the player can field **right now** from owned
items. With selection now stat-driven over owned gear (ADR-0001), the very notion of a curated "BIS you
do not own" list no longer has a source - there is no static BIS to diff against. Keeping a half-built
upgrades surface would be dead weight and split user attention.

This is recorded as an explicit "no" because a future reader will see the gap and may try to
re-introduce upgrade hints; the decision is deliberate and tied to the owned-only mandate. The
`LoadoutItemRow.State.UPGRADE` / `Tag.Variant.UPGRADE` leaf-component capabilities are **retained**
(generic, tested, low-churn) but become unused by the panel; do not re-wire them to a new upgrades
surface without revisiting this ADR.

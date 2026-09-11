# Food overrides

One shared Food section in the native AIO settings applies to every combat
style. Override food defaults to disabled, preserving existing automatic
packing. Preferred foods defaults to `Shark` and accepts comma, semicolon or
newline separators, with up to 28 unique names in priority order.

When enabled, the planner removes recognised food alternatives from optional
inventory supply groups. Required strategy supplies, switches, travel items,
casting resources, boost reservations and restoration retain priority. The
remaining slots are filled from owned usable foods in the configured order;
stock shortages move to the next listed food. Unlisted foods are not fallback
choices. Existing required quantities of a preferred food count toward packing
without being duplicated. Required food from another family remains required.

Names are case-insensitive and whitespace is normalised. `Karambwan` is accepted
as an alias for `Cooked karambwan`. Other foods and partial portions require
their full in-game names. Only unnoted, non-stackable food is used to fill slots;
stackable supplies such as purple sweets would need a separate quantity policy.
Blighted forms must be explicitly listed and remain restricted to Wilderness
combat destinations. The Wilderness cost optimiser preserves the food list's
order instead of silently substituting cheaper or blighted food.

An enabled override with no usable matching food, an empty list or insufficient
stock leaves empty Food cells and blocks preparation. These markers have item
ID 0 and are excluded from bank filtering, export and death-loss calculations.
Actual selected food uses the same bank layout, export, departure and risk
paths as other inventory supplies. Config changes trigger recalculation.

## Food recognition

`PlayerStateCapture` captures `PlayerSnapshot.ItemStats.food` on the client
thread. An item needs an Eat inventory action and a RuneLite Item Stats effect
with strictly positive theoretical Hitpoints healing and no negative Hitpoints
outcome. Range effects use their minimum theoretical value. This recognises
food even at full health, when its immediate relative healing may be zero.
Noted and placeholder forms are excluded. Unknown effects stay unrecognised.

The core `ItemStatChanges` singleton is injected directly, so this does not
require enabling the Item Stats panel plugin. No effect is applied, no food is
eaten, and there are no runtime network requests. The captured flag is immutable
and its comparison participates in snapshot equality.

The implementation was checked against RuneLite 1.12.38
[item effects](https://github.com/runelite/runelite/blob/runelite-parent-1.12.38/runelite-client/src/main/java/net/runelite/client/plugins/itemstats/ItemStatChanges.java)
and [theoretical stat changes](https://github.com/runelite/runelite/blob/runelite-parent-1.12.38/runelite-client/src/main/java/net/runelite/client/plugins/itemstats/StatBoost.java).
Coverage includes cooked karambwan, supported pie and pizza portions, hunter
foods and blighted foods. Rock cake and poisonous karambwan fail the healing
check. Coverage remains conservative: regular kebabs and spicy stew currently
lack a usable Hitpoints effect in RuneLite's item effect data.

## Manual verification

- With the override disabled, compare the existing automatic packing.
- Enable it with `Shark, Cooked karambwan`; confirm remaining slots use sharks
  first and then owned cooked karambwan if sharks run out. Switch combat styles
  and confirm the same shared preference applies.
- Confirm a configured food outside the previous automatic shortlist, such as
  a supported pie or pizza portion, works with its exact in-game name, including
  while at full Hitpoints.
- Check an empty list, an unowned food, ordinary non-food, rock cake, a noted
  form and insufficient stock. Confirm shortages and no invented ownership.
- Check an explicitly listed blighted food inside and outside Wilderness combat
  destinations, including travel through Wilderness to the non-Wilderness KBD
  lair. Normal food preferences must not silently become blighted food.
- Reserve boosts and use a method with mandatory supplies. Confirm these retain
  priority, required food is preserved, and the grid never exceeds 28 slots.
- Confirm selected food quantities in bank filtering, copied setups, departure
  checks and Wilderness risk. Repeat with the Item Stats plugin disabled.

Verification uses `./gradlew build` and owner-operated manual checks. No
automated tests or game-input automation are added.

On 2026-09-11, `./gradlew generateSlayerData build` passed. A separate logged-out
RuneLite client loaded the plugin, displayed the collapsed Food section, and
accepted enabling Override food without injection or AIO errors. The owner's
Main client was left running. Bank-based packing checks remain manual.

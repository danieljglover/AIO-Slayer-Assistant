# Ready to leave

The footer keeps preparation to one status above Filter bank. Clicking it opens
the departure checklist at the top of Checks. Only outstanding actions appear;
each expands to explain its quantity, placement or unverified condition.

## What is checked

- Selected equipment must occupy its worn slot. An inventory copy waiting to be
  equipped is reserved before checking inventory supplies or switches.
- Inventory quantities must be physically packed. Bank stock, notes, other
  potion doses and different charged item forms do not satisfy these checks.
  RuneLite's linked worn forms are treated as the same item.
- The selected rune pouch must be carried, its contents observable, and its rune
  types and quantities must match the configuration exactly. Loose runes and
  pouch contents are separate stores; extra pouch runes need unloading.
- Existing account requirements and setup blockers remain outstanding. A saved
  charge confirmation cannot suppress an unknown or insufficient current charge
  check. Catalogue checks preparation for a preview, never a real assignment.
- Wilderness preparation flags carried items or quantities outside the plan,
  looting bag contents, permanent losses, an exceeded risk budget and incomplete
  carried loss estimates. A planned loss estimate is not proof of actual risk.

Completed checks disappear. An empty checklist shows Ready to leave on Active
task, or Packed for preview in Catalogue. Logout, account changes and pending
recalculations never retain a ready status. Capture waits for a completed
logged-in game tick with a local player and account profile. After that point,
absent inventory/equipment containers are treated as empty: RuneLite may never
create a container for an empty slot collection. This follows the game's packet
ordering, not an explicit container-loaded flag. The gate resets on every game
state or account transition.

## Charge observations

There is no universal loaded-charge field in RuneLite item containers. This
plugin reads the active Weapon Charges plugin's public item metadata and current
RuneScape-profile balances when it is installed and enabled. Its persisted
floats are floored and shown as estimates: upstream records one balance per
weapon family, without an observation timestamp or individual-item identity.
Even a saved zero can originate from an uninitialised consumption estimate.
These values therefore do not prove a weapon empty, clear a departure check,
reduce required ether or become confirmed Wilderness risk. Unknown or invalid
values remain unknown. Disabling the provider removes its estimates.

The collapsed Charges & looting bag section in Checks shows balances and their
source. For a fresh observation, use the carried item's Check action yourself.
AIO matches the resulting game message within three ticks. It requires one
unique carried copy of that weapon family and an identified inventory/equipment
widget. Checks on banked copies, overlapping checks, ambiguous messages and
different accounts cannot supply a verified balance. Native observations remain
in memory for the current session and take precedence over imported estimates.
Equipped-item menus may identify only their slot layer, with no item ID. AIO
maps both the equipment tab and equipment-statistics slot components to the WORN
container before matching the Check response, including head, shield and weapon
slots. It never substitutes a banked copy for a worn slot.

Native Check support covers Wilderness ether weapons, Iban's staff, tridents,
warped sceptre, Sanguinesti staff, Tumeken's shadow, Eye of Ayak, abyssal tentacle,
scythes, blood fury, Arclight, crystal weapons and armour, Venator bow, serpentine
helm variants, fire/water tomes and blowpipes. Known ornamented forms are
included; active Weapon Charges metadata extends supported item-family IDs.
Blowpipe observations keep both the exact dart type/count and scale count. The
lower of the two counts is only a conservative readiness bound, not an attack
count prediction. Recharge dialogues handled by Weapon Charges update the
imported estimate; they do not become an AIO carried-item Check.

Moving the same item between backpack and equipment, withdrawing unrelated
supplies and ordinary game messages preserve a native observation. Banking or
replacing that weapon family, charging, use, worn-item animations/hitsplats and
charge-related messages invalidate it conservatively. Provider balance changes
also invalidate conflicting native observations. Unsupported or invalidated
balances stay explicitly unverified; AIO never performs Check or any other game
action. Exact numbered teleport jewellery forms are checked through item IDs.

Wilderness weapon counts are usable charges, separate from the 1,000 activation
ether. Wildy charge limit in AIO settings controls the excess-charge
warning, from 1 to 16,000 usable charges (default 500). A confirmed positive
balance at or below the limit passes this check; exceeding it asks to remove
excess ether. A zero or unverified balance still needs attention. For example,
1,500 total ether means 500 usable charges and passes a 500 limit. Raising the
limit does not change the planned preparation amount or conceal actual risk.

Planned ether charges remains the preparation target and the assumption used for
planned loss. Fresh observations update actual ether loss and reduce the loose
ether needed to reach that target. Powered staffs require the configured cast count.
An observed charge count alone does not price every stored resource. The
existing death model still flags unsupported loaded-resource values as unknown.

Charge message evidence: maintained Plugin Hub weapon-charges-2 source,
[ChargedWeapon.java](https://github.com/geheur/weapon-charges/blob/8da860f3628cbd4fc72ba09cfdff4b67ad88bac9/src/main/java/com/weaponcharges/ChargedWeapon.java),
reviewed 2026-09-10. The bridge uses public enum fields and reads config; it never
invokes upstream game handlers or writes another plugin's settings. Weapon
Charges is optional and there is no runtime network fetching.

## Looting bag observations

A fresh container-516 event or a newly built Looting bag contents view records
the bag. Closing that view retains the observation. The same interface group
also displays Add to bag using the ordinary inventory: only the Looting bag
title and complete 28-slot contents grid qualify. An empty view needs the
explicit "The bag is empty." message. Polling an old container or seeing a blank
grid never proves an empty bag.

Deposits, use-on-bag, removal, collecting loot and other plausible mutations
mark the observation stale until another fresh observation. Delayed ground
pickups and telegrabs are checked again at completion so an earlier View reply
cannot leave stale contents confirmed. Account/session changes clear the bag;
ordinary region loading and closing its view do not. Known contents contribute
to carried risk and departure asks for an empty bag. Unknown contents remain
explicitly unknown; bag items never count as usable combat supplies.

Implementation evidence, reviewed 2026-09-10:

- [Dude Where's My Stuff looting bag tracking](https://github.com/Thource/dude-wheres-my-stuff/blob/05e130ded80009cea3408ce7e84e11851c575dc6/src/main/java/dev/thource/runelite/dudewheresmystuff/carryable/LootingBag.java)
  distinguishes View/Add to bag and the explicit empty message.
- [Looting Bag Organizer interface research](https://github.com/robrichardson13/looting-bag-organizer/blob/4bdd6f8505150078dbb4a68e3d87487cec529ccc/docs/RESEARCH.md)
  records the current contents-grid scripts and shared interface behavior.
- [Looting Bag Value](https://github.com/pwatts6060/runelite-plugins/blob/57860f7ab196ee6adacda529579a96ede88a933c/src/main/java/com/lootingbag/LootingBagPlugin.java)
  correlates pickup/telegrab completion with bag changes.

AIO owns its session observations rather than importing another plugin's
unverifiable saved bag contents. No separate looting bag plugin is required.

## Sidebar icon

`src/main/resources/icons/aio-slayer-assistant.png` replaces the generic red S
with a steel Slayer helmet and gold preparation checkmark, sized to 24px. The
unique path avoids sharing `panel_icon.png` with another built-in development
plugin on the same classpath.
Generated with the built-in image generation tool on 2026-09-10; original retained
at `/home/danny/.codex/generated_images/01a08149-25b8-7d82-93f2-47864e91eafd/exec-613cdc3a-8f12-40a3-8fb7-db6f07d97079.png`.

Prompt: Create one clean transparent-background UI icon for a RuneLite Old
School RuneScape plugin named AIO Slayer Assistant. Icon only, no text or letters.
A recognisable front-view grey steel Slayer helmet: compact angular full-face
helmet, two small sideways horn spikes near temples, narrow dark eye openings
and a dark perforated mouth guard. Add a small strong gold checkmark at the lower
right, representing prepared gear. Match the low-resolution hand-pixelled
fantasy item icon aesthetic of Old School RuneScape, crisp pixel edges and strong
silhouette. This must be legible when downscaled to 24 by 24 pixels, so very
simple, no fine texture, no gradients outside the helmet, no glow, no drop shadow
or background. Steel grey, dark charcoal outlines, restrained pale highlights,
warm gold checkmark. Helmet fills nearly the entire square, checkmark clearly
separate and recognisable. Export as a square PNG with actual alpha transparency.
Use a small logical pixel grid and nearest-neighbour style enlarged rendering,
without any visible grid. This is an original plugin toolbar icon.

## Manual verification

Compile with `./gradlew build`. The owner performs game interactions; there are
no automated tests in this project.

1. Select a setup while logged in. Leave a selected helmet in the backpack:
   Equip must remain outstanding even when Withdraw and Missing are both zero.
2. Equip it, then pack the exact listed supplies. Check that each completed row
   disappears and changes refresh the footer without keeping stale green text.
3. For a pouch plan, compare loose runes, a banked pouch, an incomplete carried
   pouch and its exact configuration. Unload extra rune types and amounts.
4. Enable Weapon Charges and inspect automatic balances in Charges & looting
   bag. They must say estimate and must not silently verify a departure check.
   Disable it and confirm its estimates disappear without breaking AIO.
5. Manually Check a supported carried weapon. Compare insufficient, matching and
   excess cast counts, a fully charged Sanguinesti staff, and a blowpipe's
   dart type/count and scales. Move it between backpack and equipment or withdraw
   food: the observation should survive. Use, recharge or bank it: recheck is
   required. Unrelated banked copies and two carried family copies cannot verify
   the carried weapon. A same-tick provider resource change must invalidate a
   conflicting blowpipe observation.
   Repeat Check on an equipped chainmace, helm and shield in both equipment
   interfaces. For a chainmace with 500 usable charges, set the alert limit to
   499, 500 and 501: only 499 should produce an excess-charge alert. Check that
   zero still needs charging, estimates stay unverified, and actual ether risk
   includes all observed charges plus the activation deposit at every limit.
6. View an empty/nonempty bag, close the view and inspect the retained status.
   Add to bag with an empty inventory must never prove an empty bag. After a
   deposit or pickup, inspect the refreshed contents or recheck instruction.
   Request View and then a distant pickup before its response; after the pickup
   completes, an earlier empty View response must not remain confirmed. Repeat
   with a telegrab and a decrease in a ground-item stack.
7. In Wilderness setups, bring an extra item, excess ammunition or bag loot.
   Confirm the checklist flags the additional risk and respects unknown values.
8. Switch Catalogue/Active, change the selected setup, hop worlds and log out.
   A preview or stale account capture must never claim actual task readiness.

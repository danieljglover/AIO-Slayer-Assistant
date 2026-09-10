# Selected setup bank filter

The panel's Filter bank button opens a temporary Bank Tags view containing the
displayed setup's equipment and inventory. This includes optional switches,
travel items, looting bag and virtual RUNE POUCH rows. Positive item IDs with a
positive planned quantity are included even when the setup has blockers or
some items are already carried. A filter is a view of existing bank entries; it
does not create missing items. Quantities remain in the packing list. Equipment
occupies a 3x5 grid on the left, followed by a blank separator column and the
4x7 inventory on the right. Up to four pouch runes occupy the lower-left row.
Nonstackable supplies repeat in their planned positions, while stackables
occupy one cell. Every repeated cell refers to the same bank stock. Missing
items remain faded layout placeholders. An overflow appendix retains checklist
IDs that do not fit the 28 physical slots in a blocked preview.

`BankSetupFilter` uses the core `TagManager.registerTag` and
`BankTagsPlugin.openTag` APIs with an in-memory `Layout`, `OPTION_HIDE_TAG_NAME`
and no modification flag. All-In Slayer declares the same core Bank Tags
dependency as Inventory Setups. No external Hub plugin is required for filtering. ItemManager
canonicalization matches bank placeholders, notes and linked worn forms without
fuzzy matching charge/dose families. A mismatched potion-storage dose is
replaced by a passive placeholder for the planned form after core rendering.
No withdrawal or equipment action occurs.

The filter reads an immutable set derived from the displayed recommendation.
Item/slot changes within the same monster/location/method refresh the bank view;
stack-quantity changes that preserve slots do not relayout it. Client and
item-definition access is on the client thread. Button state reaches Swing through the EDT. Publishing a
setup and opening its filter are guarded by the recommendation revision.

Selection/preference changes, a different automatic recommendation, account
changes and plugin shutdown release the filter. Closing the bank or opening a
different tag/native search also releases it, without reopening it on the next
bank visit. Cleanup closes a view only while its tag is ours. Bank Tags stores
even temporary opens in its remembered-tab setting: the previous value is
restored only while that setting still points at our temporary tag. Saved
item tags and layouts are never edited. A unique private runtime name prevents
collisions with the core saved-tag prefix matcher. The private
`banktaglayouts.layout_<runtime tag>` key is set to `DISABLED` while the view is
open, preventing Bank Tag Layouts' automatic persistence. Its preview button is
hidden only during this view, and core layout drags are suppressed. Cleanup
removes our private compatibility key and any core layout record for that key;
startup removes leftovers from an interrupted client. Bank capture still reads
the complete ItemContainer, independently of visible bank widgets. Widget-close
and account cleanup are queued outside game scripts because Bank Tags' close
operation rebuilds the bank and RuneLite scripts are not reentrant. If core tag
editing replaces the active in-memory layout, widget decoration stops and
cleanup is deferred instead of treating the compact bank as our layout.

Sources inspected on 2026-09-10:

- [Inventory Setups](https://github.com/dillydill123/inventory-setups/tree/6f9678715ef3f4c5293480e3926a51c8418e4c44),
  especially `doBankSearch` and `resetBankSearch`.
- [Bank Tag Layouts](https://github.com/geheur/bank-tag-custom-layouts/tree/a904d95d242cc62f9f2e028399287bea72c01f05).
- [RuneLite Bank Tags](https://github.com/runelite/runelite/tree/runelite-parent-1.12.38/runelite-client/src/main/java/net/runelite/client/plugins/banktags),
  including TagManager, BankTagsService, BankTagsPlugin and TabInterface.

Manual verification checklist (the owner performs game interactions):

- Open a bank, choose a setup and click Filter bank. Check equipment, switches,
  runes, pouch, food, potions, travel and looting bag against the packing list.
- Check the left equipment shape, 28 right inventory positions, repeated food,
  stacked resources, faded missing items, empty slots and lower-left pouch runes.
- Confirm a selected charged jewellery/weapon form does not admit other charge
  states; a bank placeholder may appear but is never counted as owned stock.
- Withdraw items manually and confirm the filter remains active and the full
  bank observation still updates. Check an inventory with pouch-contained runes.
- Clear the filter, then try a different task/location/method, native search,
  other bank tags, closing/reopening the bank, logout and disabling the plugin.
- Confirm existing Inventory Setups and custom bank layouts still open normally.

Verification uses `./gradlew build` and manual checks; this project has no
automated tests.

The owner confirmed the first filter-only build worked in the live bank on
2026-09-10, then requested the matching equipment/inventory layout. The arranged
view was visually checked with Skeletons and Vet'ion, including all 28 inventory
positions and repeated food slots. The plugin's Clear button restored the full
normal bank, and Filter reopened the arranged view. The final implementation
passed `./gradlew build` and code review. Pouch-specific and potion-storage
edge cases still need manual account checks.

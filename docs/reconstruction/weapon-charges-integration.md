# Weapon Charges integration

## Bundled compatibility mapping

`WeaponChargesBridge` imports estimates through RuneLite's
`ConfigManager.getRSProfileConfiguration`, using a reviewed table in
`WeaponChargeMappings`. It never loads upstream enums, reads their fields,
invokes their handlers or writes their config. Weapon Charges is optional;
plugin discovery and active-state checks run on the EDT. Profile and game-state
reads run on the client thread. Pending discovery callbacks are invalidated on
shutdown or another lifecycle event.

The [Plugin Hub manifest](https://github.com/runelite/plugin-hub/blob/master/plugins/weapon-charges-2)
was checked on 2026-09-11 and pins geheur/weapon-charges at
`8da860f3628cbd4fc72ba09cfdff4b67ad88bac9`. The repository's default branch was
still at `51fbbcb130111c6f0fd146bbb87394bef4019b73`; it lacks the Hub version's
ornamented trident IDs. The bundled mapping follows the Hub revision.

Reviewed sources:

- [ChargedWeapon](https://github.com/geheur/weapon-charges/blob/8da860f3628cbd4fc72ba09cfdff4b67ad88bac9/src/main/java/com/weaponcharges/ChargedWeapon.java)
  supplies 31 families and 67 charged item IDs. `itemIds` is assigned from
  `chargedItemIds`, excluding uncharged forms. `configKeyName` identifies the
  balance; `settingsConfigKey` only groups display settings. In particular,
  Ursine, Webweaver and Accursed have their own balance keys.
- [WeaponChargesPlugin](https://github.com/geheur/weapon-charges/blob/8da860f3628cbd4fc72ba09cfdff4b67ad88bac9/src/main/java/com/weaponcharges/WeaponChargesPlugin.java)
  reads family balances from the `weaponCharges` RuneScape-profile config group.
  Blowpipe uses `blowpipeScales`, `blowpipeDarts` and `blowpipeDartType`, with
  eight known uppercase dart names. `UNKNOWN` has no usable item ID. This
  revision has no plugin-message API for importing charge data.
- [RuneLite forbidden language features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#forbidden-language-features)
  prohibit Java reflection, including public-member reflection.

Upstream BSD-2-Clause attribution is retained alongside the table and in
`src/main/resources/META-INF/LICENSE-weapon-charges.txt`, which is bundled in
the plugin JAR.

## Estimates, observations and unknown values

Provider data remains an estimate shared by all items in a family, with no
timestamp or individual-item identity. Finite nonnegative saved balances are
floored. Missing, malformed, negative, non-finite or overflowing values do not
become zero. Missing/disabled Weapon Charges removes its estimates. Unknown
dart names cannot identify ammunition; partial blowpipe resources remain
explicitly incomplete. Future unmapped item IDs remain unknown until reviewed.

`ChargeStateCapture` independently observes the owner's carried-item Check
action and matching game message. Those native rules and their three-tick
correlation window are unchanged by B2. They work without Weapon Charges,
support equipped-item slot resolution, reject ambiguous carried copies and
take precedence over imported estimates. Profile changes and charge-changing
actions retain the existing invalidation rules. Estimates never clear a
departure check or count as confirmed stored resources in Wilderness risk.

Wilderness weapon balances represent usable charges. The fixed 1,000 activation
ether is separate and counted in death risk in addition to the observed usable
balance. The charge-limit condition remains strictly `usable > limit`: 500
usable charges plus 1,000 activation ether passes a 500 limit, and warns at 499.
Zero or unknown usable charges still require attention. Raising the alert limit
does not change planned ether or reduce actual death risk.

## Updating the table

1. Read the current Plugin Hub manifest and inspect its pinned source revision.
2. Compare every `chargedItemIds` list and `configKeyName`, including ornamented
   and alternate forms. Do not import `unchargedItemIds` or display-setting keys.
3. Review blowpipe resource keys, dart enum names/IDs and persistence semantics.
   Preserve unknown handling if upstream adds a resource or changes its schema.
4. Update the bundled table, source revision and attribution together. Review
   native Check rules separately if a new family needs fresh observation support.
5. Run `./gradlew build` and manually verify with the owner. No upstream code is
   downloaded or inspected at runtime, and this project has no automated tests.

## B2 verification (2026-09-11)

- `./gradlew build` passed after replacing reflection.
- Source comparison matched all 31 families, 67 charged item IDs and eight dart
  names against the Hub revision. No missing, extra or different entries.
- Compiled bridge/table bytecode has no reflective member access or dynamic
  class loading. `git diff --check` passed.
- Claude was invoked headlessly with Sonnet and medium effort, but its account
  usage limit prevented a B2 review. Kimi independently reviewed the capture,
  readiness and risk consumers, then the finished bridge/table patch; no blocking
  findings. An initial concern about Eye of Ayak coverage was disproved by the
  pinned source and withdrawn in the patch review. Codex separately checked all
  eight dart mappings against upstream.
- The rebuilt development client launched with the existing profile. With
  Weapon Charges enabled, Calvar'ion's Ursine chainmace showed a 500-charge
  estimate with unknown last-check time. Disabling the provider changed it to
  Balance unknown and kept the Check requirement outstanding.
- Fresh owner-operated Check and charge-limit verification are pending; B2
  stays unchecked until completed.

Live checklist (the owner performs all game interactions):

1. Log in and open the bank. Select a setup using a Wilderness weapon. With
   Weapon Charges enabled, confirm its saved balance says estimate and still
   requests a fresh Check.
2. Disable Weapon Charges before checking the weapon. Confirm the estimate
   disappears and the balance becomes unknown. Re-enable it and confirm the
   estimate returns.
3. Carry exactly one Ursine chainmace. Use its Check option and compare the
   game's usable charge count with AIO's green In-game Check observation.
   Repeat while equipped to exercise slot-based item resolution.
4. For a positive observed balance N, set the Wildy charge limit to N-1, N and
   N+1 (within its allowed range). Only N-1 should show excess ether. Confirm
   actual risk always includes N usable charges plus 1,000 activation ether.
5. Disable Weapon Charges after a fresh Check. The native observation should
   remain. Bank the weapon to invalidate it; its balance should be unknown
   while the provider is disabled. Check it again while the provider remains
   disabled to confirm native capture is independent.
6. Restore the provider and original AIO settings. If a blowpipe is available,
   compare its estimated and freshly checked dart type/count and scales.

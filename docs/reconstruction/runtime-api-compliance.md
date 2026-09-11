# Runtime API compliance

## B3 changes

`SetupExporter` now receives RuneLite's Gson through constructor injection, and
`AllInSlayerPlugin` injects the exporter. RuneLite binds this Gson to its shared
`RuneLiteAPI.GSON` instance. The existing portable Inventory Setups JSON tree,
equipment order, 28 inventory slots, rune-pouch notes and preview notes are
unchanged; serialization uses `gson.toJson(portable)`.

The four restricted widget references now use their exact `InterfaceID`
equivalents, as defined by RuneLite's own `WidgetInfo` aliases:

| Previous WidgetInfo | Supported InterfaceID | Packed component ID |
| --- | --- | --- |
| `BANK_ITEM_CONTAINER` | `Bankmain.ITEMS` | 786444 |
| `BANK_CONTENT_CONTAINER` | `Bankmain.ITEMS_CONTAINER` | 786441 |
| `LOOTING_BAG_CONTAINER` | `WildernessLootingbag.ITEMS` | 5308421 |
| `PVP_WILDERNESS_LEVEL` | `PvpIcons.WILDERNESSLEVEL` | 5898290 |

The bank-open/visibility checks, temporary bank-layout logic, looting-bag view
fingerprint, and Wilderness-level text parsing keep their existing behavior.
All widget reads remain on the client thread. The looting-bag capture service
already used `InterfaceID`; B3 changes the death-state fingerprint's reference
to the same contents widget.

## Official API recorder verification (2026-09-11)

Tooling source: runelite/plugin-hub-tooling
`9b441d86aa16ec9500ca4654b4c14696890c9cba`.
Its restriction list is byte-identical to the checklist's original
`5654eb60d6e664d69dad0dda4fd0e794aea1aa88` revision.

- [Restricted API list](https://github.com/runelite/plugin-hub-tooling/blob/9b441d86aa16ec9500ca4654b4c14696890c9cba/package/src/main/resources/net/runelite/pluginhub/packager/disallowed-apis.txt)
  bans WidgetInfo/WidgetID and fresh Gson/GsonBuilder constructors.
- [RecorderPlugin](https://github.com/runelite/plugin-hub-tooling/blob/9b441d86aa16ec9500ca4654b4c14696890c9cba/apirecorder/src/main/java/net/runelite/pluginhub/apirecorder/RecorderPlugin.java)
  records source-level API usage during javac analysis, including inlined
  component constants that a bytecode-only scan would miss.
- [API checker](https://github.com/runelite/plugin-hub-tooling/blob/9b441d86aa16ec9500ca4654b4c14696890c9cba/apirecorder/src/main/java/net/runelite/pluginhub/apirecorder/API.java)
  provides `parseCommented` and `disallowed`, used for the policy check.

Built the upstream `:apirecorder:shadowJar`. An external Gradle init script
compiled two source selections with `-Xplugin:RuneLiteAPIRecorder`, the project's
compile classpath and Lombok annotation processor, targeting Java 11. Neither
verification task adds a source set or test to the project; outputs stay under
`/tmp/aio-b3-team`. The recorded API files were decoded and checked using the
upstream API checker and current restriction list.

| Scope | Recorded API references | Result |
| --- | --- | --- |
| All `src/main/java` before B4 | 654 | FAIL: Gson and GsonBuilder constructors in B4 authoring code |
| Runtime, excluding `data/source` | 596 | PASS: no restricted API matches |

The runtime record contains all four supported component constants and no
WidgetInfo/WidgetID usage. The only full-source restricted signatures are
`Lcom/google/gson/Gson;.<init>()V:b` and
`Lcom/google/gson/GsonBuilder;.<init>()V:b`. Source inspection locates these in
the six authoring compiler classes listed under B4. The B3 patch itself did not
move or exclude those classes from the production build.

This is the official source recorder and restricted-API check, not a complete
Plugin Hub packaging/submission run. The separate runtime result did not make
the whole project Hub-compliant. The later B4 verification below resolves the
full-source scan; build configuration and other submission checks remain open.

B4 follow-up (2026-09-11): the unchanged upstream Hub Gradle integration ran
`compileJava` over the full production source set after authoring files moved
to `src/dataGenerator/java`. It recorded 596 references and passed the official
restricted-API checker with zero findings, without verification-time source
exclusions. Its packaging task also passed. See
[B4 artifact and scan evidence](data-generation-isolation.md#hub-recording-and-artifact).

Local evidence:

- `/tmp/aio-b3-team/api-scan.init.gradle`: recording tasks.
- `/tmp/aio-b3-team/api-recording.log`: both compilations passed.
- `/tmp/aio-b3-team/api-allsource/api` and `api-runtime/api`: recorder output.
- `/tmp/aio-b3-team/check-apis.jsh`: invokes the upstream policy checker.
- `/tmp/aio-b3-team/api-scan-result.log`: full-source failure/runtime pass.
- `/tmp/aio-b3-team/build.log`: `./gradlew build` passed.

Kimi's independent patch review found no blockers and confirmed the four widget
aliases and exporter injection. The actual scan disproved an initial research
claim that Gson constructors were not checked; the final review acknowledged
the correction. Claude was invoked with Sonnet and medium effort, but its
account usage limit prevented a review. Neither reviewer modified the repo.

## Live verification

The rebuilt development client launched with the existing profile and both
AIO Slayer and Wilderness Sentinel. Only the owner operates game controls.

- The owner logged in and checked the looting bag. The AIO panel showed
  "An empty bag was observed this session" and "Empty - confirmed" after the
  contents view had closed. Screenshot: `/tmp/aio-b3-team/01-empty-bag-confirmed.png`.
- Clicking AIO's Copy setup reported success. The live clipboard output parsed
  as JSON with 14 equipment slots, 28 populated inventory slots, weapon ID 27655,
  spellbook value 4, an empty layout array and retained preparation notes.
  Evidence: `/tmp/aio-b3-team/copied-setup.json` and
  `/tmp/aio-b3-team/02-setup-copied.png`. No Inventory Setups import was performed.
- The bank filter correctly remained disabled while the bank was closed. After
  the owner reopened it, the panel reported a fresh Bank captured timestamp and
  enabled Filter bank. Clicking that AIO control showed the selected equipment
  on the left and the 28 inventory slots on the right. Clear bank filter restored
  the previous normal bank view. No bank items or game controls were clicked.
  Screenshots: `/tmp/aio-b3-team/04-bank-captured.png`, `05-bank-layout.png`,
  and `06-normal-bank-restored.png` in the same directory.
- The owner entered Wilderness level 2. The running client visibly showed
  "Level: 2" while AIO continued updating. Screenshot:
  `/tmp/aio-b3-team/07-wilderness-level-2.png`. The migrated reference resolves
  to the exact same component (5898290), and the existing `level\\s*:\\s*(\\d+)`
  parser is unchanged. This verifies the live display and lookup equivalence;
  AIO does not expose its captured integer directly in the panel, so that value
  was not independently inspected. No game controls were used by the agent.

The B3 runtime changes, build and live checks above are verified. Its checklist
box is now complete because B4 also passed the recorder/checker over the full
production main source set.

The client log contains unrelated login exceptions from CorpFfaPlugin and
NpcAggroAreaPlugin and pre-existing websocket port conflicts. No AIO exception
was found during these checks.

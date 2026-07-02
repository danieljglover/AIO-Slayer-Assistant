# WA-0 Spike - Player-state signals (reward points, unlock varbits, current master)

Date: 2026-07-01. Owner: backend (fr-be2). Gates ONLY the WA-10 default binding (PD-A).

## Verdict: SUCCESS - all three signals exist and are client-readable. WA-10 GOES: bind the varbit-backed provider as default.

Every id below was verified against the artifacts this plugin actually compiles against - not
transcribed from memory or a community wiki alone.

## Verification method (reproducible)

1. **Pinned API constants.** The project builds against `net.runelite:client:latest.release`,
   resolved in the local gradle cache to **1.12.31.1**. Every varbit/varp id below was dumped from
   that exact jar:
   `javap -constants -cp runelite-api-1.12.31.1.jar net.runelite.api.gameval.VarbitID | grep -i slayer`
   (same for `net.runelite.api.Varbits`, `VarPlayer`, `VarPlayerID`). The `gameval` classes are
   generated from Jagex's own gameval names shipped in the game cache - these are Jagex's internal
   names, not community guesses.
2. **Stock Slayer plugin bytecode.** `javap -c net.runelite.client.plugins.slayer.SlayerPlugin`
   (client-1.12.31.1.jar) shows the stock plugin live-reading `getVarbitValue(4068)` (points),
   `getVarbitValue(4069)` (streak) and `getVarbitValue(4067)` (master) - including the branch
   `master == 7 ? read 5617 : read 4069` for the streak display (Krystilia's separate wilderness
   streak), which pins value 7 = Krystilia from the plugin's own logic.
3. **Master-value mapping.** OSRS wiki varbit registry (`RuneScape:Varbit/4067`) documents the full
   value table; cross-corroborated by (a) the stock plugin's 7=Krystilia branch above and (b)
   RuneLite `NpcID` gameval names (`SLAYER_MASTER_1_TUREAL`, `SLAYER_MASTER_3` = Vannaka,
   `SLAYER_MASTER_4` = Chaeldar) matching the same ordering. Three independent sources agree.

## Signal 1 - Reward points balance: varbit 4068 (VERIFIED, strongest)

| id | gameval name | legacy `Varbits` name | read by stock plugin |
|---|---|---|---|
| **4068** | `SLAYER_POINTS` | `SLAYER_POINTS` | yes (`getVarbitValue(4068)`, points tooltip) |

Integer point balance. `SlayerUnlockStateProvider.rewardPoints()` reads this directly.

## Signal 2 - Unlock ownership: per-unlock varbits (VERIFIED names; live values = QA item)

All from `net.runelite.api.gameval.VarbitID` in the pinned jar. Boolean 1 = purchased (per gameval
naming convention; live-observation of each value is a QA checklist item, see Caveats).

**Global rewards (the WA-6 catalogue -> varbit map WA-10 needs):**

| rewardId (WA-6) | varbit | gameval name |
|---|---|---|
| malevolent-masquerade | 3202 | `SLAYER_HELM_UNLOCKED` |
| ring-bling | 3207 | `SLAYER_RING_UNLOCKED` |
| broader-fletching | 3208 | `SLAYER_AMMO_UNLOCKED` |
| like-a-boss | 4724 | `SLAYER_UNLOCK_BOSSES` |
| bigger-and-badder | 5358 | `SLAYER_UNLOCK_SUPERIORMOBS` |
| task-storage | 12442 | `SLAYER_UNLOCK_STORAGE` |
| i-wildy-more-slayer | 13636 | `SLAYER_UNLOCK_WILDY_EXTRATASKS` |
| helm recolour: black | 5080 | `SLAYER_UNLOCK_HELM_BLACK` |
| helm recolour: green | 5081 | `SLAYER_UNLOCK_HELM_GREEN` |
| helm recolour: red | 5082 | `SLAYER_UNLOCK_HELM_RED` |
| helm recolour: purple | 5631 | `SLAYER_UNLOCK_HELM_PURPLE` |
| helm recolour: turquoise | 6096 | `SLAYER_UNLOCK_HELM_TURQUOISE` |
| helm recolour: hydra | 6570 | `SLAYER_UNLOCK_HELM_HYDRA` |
| helm recolour: twisted | 10104 | `SLAYER_UNLOCK_HELM_TWISTED` |
| helm recolour: araxyte | 11023 | `SLAYER_UNLOCK_HELM_ARAXYTE` |
| helm recolour: hooded | 19720 | `SLAYER_UNLOCK_HELM_HOODED` |

**Task-extension unlocks (`extendedAmount` enablers, per-task `unlocks[]` EXTENSION rows):**
`SLAYER_LONGER_*`: darkbeasts 4031, ankou 4085, suqah 4086, blackdragons 4087, metaldragons 4088,
abyssaldemons 4090, blackdemons 4091, greaterdemons 4092, bloodveld 4746, aberrantspectres 4747,
aviansies 4748, mithrildragons 4749, cavehorrors 4750, dustdevils 4751, skeletalwyverns 4752,
gargoyles 4753, nechryael 4754, cavekraken 4755, spiritualgwd 4757, scabarites 5359,
fossilwyverns 5733, adamantdragons 6094, runedragons 6095, basilisk 9455, vampyres 10389,
araxytes 11022, revenants 14822, custodians 17219, wyrms 19602, aquanites 19603,
gryphon 15398 (`SLAYER_UNLOCK_LONGER_GRYPHON`), frost dragons 15399 (`SLAYER_UNLOCK_LONGER_FROST_DRAGONS`).

**Task-access unlocks (TASK_UNLOCK-typed rows):**
`SLAYER_UNLOCK_*`: reddragons 2462, mithrildragons 4094, aviansies 4095, notedmithrilbars 4589,
tzhaar 4691, lizardmen 4996, basilisk 9456, vampyres 10388, grotesquekills 6485,
warped_creatures 15286, gryphons 19604, aquanites 19605, fossilwyvernblock 240 (a block toggle,
not a purchase).

## Signal 3 - Current/assigning master: varbit 4067 (VERIFIED, incl. value map)

| id | gameval name | semantics |
|---|---|---|
| **4067** | `SLAYER_MASTER` | the master who assigned the CURRENT task; 0 = no task |

Value map (wiki registry, corroborated twice - see method §3):
**0** none · **1** Turael/Aya · **2** Mazchna · **3** Vannaka · **4** Chaeldar ·
**5** Duradel/Kuradal · **6** Nieve/Steve · **7** Krystilia · **8** Konar · **9** Spria.

Maps 1:1 onto our 9 `masterId`s (turael, mazchna, vannaka, chaeldar, duradel, nieve, krystilia,
konar, spria). Note the semantics: this is the **assigning master of the current task**, not a
"preferred master" - exactly what the master-context line wants. When 0 (no task), fall back to the
config primary master (PD-E selector default unchanged).

## Bonus (recorded for the P2 economy/streak features, not consumed in Wave A)

- Task streak: varbit **4069** (`SLAYER_TASKS_COMPLETED`, legacy `SLAYER_TASK_STREAK`).
- Wilderness (Krystilia) streak: varbit **5617** (`SLAYER_WILDERNESS_TASKS_COMPLETED`) - the stock
  plugin switches to it when master == 7.
- Blocked-task list: varps 1096 + 4830..4841 (`SLAYER_REWARDS_BLOCKED*`, from `VarPlayerID`).
- Superior spawns toggled OFF (post-purchase): varbit 14825 (`SLAYER_TOGGLEOFF_SUPERIORMOBS`).

## Caveats (honest limits)

1. **Unlock varbit VALUES were not observed on a live logged-in client.** The ids and names are
   Jagex's own gameval data in the pinned jar (high confidence), and 4067/4068/4069 are additionally
   proven by the stock plugin's reads; the per-unlock booleans rest on the gameval naming alone.
   QA live-verify item: on a dev client, confirm one owned unlock reads 1 and one unowned reads 0.
2. **Legacy `Varbits.SUPERIOR_ENABLED = 5362` is NOT the purchase bit** we should use: it has no
   gameval name in 1.12.31.1 and the stock slayer plugin no longer references it. Use 5358
   (`SLAYER_UNLOCK_SUPERIORMOBS`) for `ownsUnlock("bigger-and-badder")`.
3. Varbit reads require a logged-in client and the client thread (same constraint as the existing
   `SLAYER_TARGET_BOSSID` read in `AllInSlayerPlugin`). The provider must degrade to empty/0 when
   logged out - consumers already tolerate that via the seam contract.
4. House idiom: pin these ids as local constants in `task/SlayerVarbits.java` (gameval name in a
   comment), NOT as references to the generated classes - a generated-constant rename across
   RuneLite releases must not break the build (existing file's documented rule).

## Binding decision (PD-A)

**WA-10 GOES.** Clean, verified ids exist for points (4068), a usable unlock subset (the full
global-rewards table above), and the current master (4067 incl. value map). Per the DL disposition
(PD-A: varbit-default iff clean verified ids), `VarbitSlayerUnlockStateProvider` becomes the default
binding; `ConfigSlayerUnlockStateProvider` (WA-9) remains the fallback for logged-out/no-signal
states and manual override.

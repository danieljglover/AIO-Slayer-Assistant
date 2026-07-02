# Variant authoring spec (for MV-D* proposal agents)

You convert ONE `variants-<group>.md` source file into a strict JSON proposal of `variants` arrays,
one per task, applying the ADR-0012 / ADR-0013 / ADR-0014 resolutions. Write your output to the
proposal path given to you. Return only a short summary (task names done, variant counts, any
deferred/UNKNOWN items). DO NOT edit `slayer-data.json` - the backend engineer integrates it.

## Output format (STRICT JSON, no comments, no trailing commas)
A single JSON object keyed by EXACT task name (as in `slayer-data.json` / `current-task-profiles.json`):

```json
{
  "Greater demons": [
    { "name": "Greater demon", "npcIds": [2025,2026], "combatLevel": 92, "isDefault": true,
      "weakness": { "style": "MELEE", "element": "water" },
      "monsterDefence": { "defenceLevel": 81, "stab": 0, "slash": 0, "crush": 0, "magic": -10, "range": 0 },
      "demon": true, "dragon": false, "kalphite": false, "undead": false,
      "isBoss": false, "location": "Standard / Catacombs / Wilderness", "requirement": "none" },
    { "name": "K'ril Tsutsaroth", "npcIds": [3129], "combatLevel": 650, "isBoss": true,
      "weakness": { "style": "MELEE", "element": "water" },
      "monsterDefence": { "defenceLevel": 270, "stab": 70, "slash": 80, "crush": 80, "magic": 80, "range": 80 },
      "demon": true, "location": "God Wars Dungeon - Zamorak", "requirement": "boss - separate trip" }
  ]
}
```

Field rules (match `model/MonsterVariant.java`): `name` (string, required, unique within the task),
`npcIds` (int array, omit if UNKNOWN/empty), `combatLevel` (int, omit if N/A), `weakness`
(`{style, element}` - may be omitted entirely to inherit the task default), `monsterDefence`
(`{defenceLevel,stab,slash,crush,magic,range}` - omit to inherit task default), category flags
`demon/dragon/kalphite/undead` (booleans, default false), `isBoss` (bool), `isDefault` (bool),
`location` (free-text string note), `requirement` (free-text string), `bossId` (int, ONLY for the
Boss meta-task; omit otherwise).

## THE LOAD-BEARING RULES (do not deviate)

1. **PRESERVE the recommended STYLE; never flip it from the wiki elemental icon (ADR-0013 / decision
   #11).** The md's `weakness:` line often derives a style from the lowest defence bonus (e.g. it calls
   demons MAGIC because magic-def is lowest). IGNORE that for `style`. Set every variant's
   `weakness.style` = the parent task's **preservedStyle** from `current-task-profiles.json`. The user's
   method selector lets them pick another style at runtime; the stored recommended style does NOT flip.
   - EXCEPTION - multi-form bosses with genuinely form-dependent styles (ADR-0012.6): Dagannoth Kings
     (Rex MELEE / Prime RANGED / Supreme MAGIC) and Kalphite Queen (crawling MELEE-crush / airborne
     MAGIC or RANGED) get per-FORM styles as stated in the md / `gaps-research.md`.
   - EXCEPTION - the Boss meta-task: each boss carries its OWN recommended style from `variants-boss.md`.

2. **`element` = wiki-accurate, per variant (ADR-0013).** Map the md's stated elemental weakness
   (e.g. "Water 40%") to the lowercase element string ("water"). If the md states no element, use the
   parent task's `currentElement`; if that is null and the style is MAGIC, use "air" (cheapest
   unresisted fallback); if style != MAGIC and no wiki element, you may set the element to the wiki value
   if stated, else null. Element is mostly inert unless the active method is magic.

3. **The DEFAULT variant duplicates the task profile.** Mark exactly ONE variant `isDefault: true` - the
   base, non-boss monster. Its `weakness.style` MUST equal preservedStyle and its `monsterDefence` should
   equal the task-level `monsterDefence` from `current-task-profiles.json` (the FR-6 regression anchor).
   For the all-boss Boss task, `isDefault` marks a deterministic first-listed fallback only.

4. **Category flags = the ENGINE's bonus-applicability lens, NOT the raw wiki attribute (decision #12).**
   `demon`=Arclight/demonbane applies, ONLY for {Abyssal, Black, Greater demons, Nechryael} families.
   `dragon`=dragonbane applies (all dragons + Skeletal/Fossil wyverns + Wyrms + Drakes + Vorkath + Frost).
   `kalphite`=Keris applies (kalphites incl. Kalphite Queen). `undead`=Salve applies.
   - Hellhounds & Waterfiends have the wiki demon attribute but `demon=false` (demonbane does NOT apply).
   - Greater Skeleton Hellhound is `undead=true`, NOT demon. Vorkath is `dragon=true` AND `undead=true`.
   - Carry the PARENT task's flags onto same-family variants; a non-applicable sibling/boss keeps false.
   - Cross-check against `crosscheck/` corrections for the dragon/boss tasks.

5. **EXCLUDE reanimated monsters entirely (decision #4).** Any variant the md flags as "reanimated" /
   "do NOT count toward the task" is DROPPED (not emitted).

6. **COLLAPSE same-profile location splits into the base variant (ADR-0011 / decision, plan §6).** When
   the md lists several entries that differ only by location/NPC id but share an identical
   defence+style+flags profile (e.g. "Abyssal demon (Standard)" / "(Catacombs)" / "(Wilderness)"), emit
   ONE base variant. Merge their npcIds into that variant and summarise locations in the `location` note.
   Distinct profiles (different defence/superior/boss) stay separate variants.

7. **UNKNOWN-weakness variants (decision #5): Vampyres, Bloodveld, GWD/Reanimated hellhounds.** Emit the
   variant with NO `weakness` and NO `monsterDefence` (omit both keys) so it inherits the task default
   (ADR-0012.3). Keep it selectable for identity/location; do NOT fabricate a weakness.

8. **Bosses (FR-7):** `isBoss: true`, `requirement: "boss - separate trip"`, a `location` note. A boss
   that counts toward a task appears under that task (e.g. Skotizo under BOTH Black demons AND Greater
   demons - emit it under both). Multi-form bosses -> per-form variants (decision #6).

9. **No fabrication (R-1 discipline).** If a value is genuinely UNKNOWN, omit that key (let it inherit)
   or, for a boss with no enumerated profile, DEFER it (do not emit) and list it in your summary. Never
   invent a defence/weakness. Gryphons: base-only in v1; defer Dire/Shellbane (decision #8).

10. **Exactly one isDefault per task; superiors/regional variants are NOT default.**

Read `gaps-research.md` and `crosscheck/` for tie-breaks (Skotizo MELEE, Demonic gorilla, Araxyte, DK
forms, KQ forms). Validate your JSON parses before writing.

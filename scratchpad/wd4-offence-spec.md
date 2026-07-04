# WD-4 offence authoring spec (read this fully before editing)

You add an additive `offence` block to OSRS monster VARIANT JSON files, wiki-sourced, honest-UNKNOWN.
ADR-0020 #1 / WD-1 schema. **FR-6: additive only. Never reshape or edit any existing field.**

## The offence block (insert as a new top-level key in each variant JSON, after `"strategyId"`)
```
"offence": {
  "hitpoints": <int>,
  "maxHit": <int>,
  "attackStyles": ["MELEE"],        // subset of MELEE | RANGED | MAGIC | DRAGONFIRE | TYPELESS
  "attackSpeedTicks": <int>,
  "magicLevel": <int>,              // OMIT unless the monster has a Magic level / casts magic
  "poisonous": false,
  "venomous": false
}
```

## Rules (culture: evidence over assertion, NO fabrication)
1. **Honest UNKNOWN = OMIT the key.** If the wiki does not state a field, leave it out entirely.
   Never guess a number. `hitpoints` is almost always stated. `poisonous`/`venomous` default `false`
   (only `true` if the wiki says it poisons/envenoms).
2. **magicLevel is the PRIORITY field** (feeds the Twisted bow evaluator, WD-11). For any monster with a
   Magic level in its stats box (dragons, dagannoth, aviansies, spectres, spiritual mages, demons that
   cast, etc.) author `magicLevel`. Dragons: `attackStyles` include `"DRAGONFIRE"` (plus MELEE, and MAGIC
   only if it casts spells). A monster's Magic *level* exists even if its main attack is melee - author it.
3. `attackStyles` = the monster's attack style(s) from the wiki combat/attack-info box.
4. `maxHit` = the highest single hit the wiki lists (across styles). If several styles, pick the largest.
5. One `offence` per variant file. Superiors/higher variants hit harder - use that variant's own stats.
6. Fetch the family's own wiki page (and per-variant pages if variants differ). Use the live wiki:
   `https://oldschool.runescape.wiki/w/<Monster_name>`. Verify hitpoints/maxHit/attack style/magic level.

## Coverage test (extend the existing `<Family>SourceCoverageTest.java`)
- Read the existing test file first. Add ONE new `@Test` method named `variantCarriesHonestOffence`
  that reads one representative variant JSON via the same `read(...)`/Gson helper the test already uses,
  and asserts its `offence` (hitpoints + at least one attackStyle; magicLevel where authored).
- Mirror this shape (from GoblinsSourceCoverageTest):
```java
@Test
public void variantCarriesHonestOffence() throws IOException {
    SourceMonsterVariant v = read(Paths.get("src/main/data/slayer/monsters/<fam>/<variant>.json"), SourceMonsterVariant.class);
    assertNotNull(v.getOffence());
    assertEquals(Integer.valueOf(<hp>), v.getOffence().getHitpoints());
    assertTrue(v.getOffence().getAttackStyles().contains(AttackStyle.<STYLE>));
    // if magicLevel authored: assertEquals(Integer.valueOf(<m>), v.getOffence().getMagicLevel());
}
```
- Add imports if missing: `import com.danieljglover.allinslayer.model.AttackStyle;`,
  `import static org.junit.Assert.assertNotNull;`, `assertTrue`, `assertEquals`. If the test uses a
  different read-helper signature, match it exactly (read the file, do not assume).
- If a family has NO existing coverage test (only `boss`), skip the test for it and note that.

## HARD constraints
- Do NOT run git, gradle, or any build/commit. Only Write/Edit the JSON + test files.
- Do NOT touch files outside your assigned families' `monsters/<family>/` dirs and their coverage tests.
- Valid JSON (comma after the previous key when inserting `offence`).
- Report back: per family, the offence you authored (hp/maxHit/styles/magicLevel), any field left
  honest-UNKNOWN and why, and any family you could NOT complete (no silent skips).

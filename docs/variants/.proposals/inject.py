#!/usr/bin/env python3
"""Inject the per-group variant proposals into slayer-data.json as the single owner of the file.
- attaches `variants` to each matching task (key appended after existing fields)
- nulls the Boss meta-task's task-level weakness/monsterDefence (ADR-0014) and attaches boss variants
- appends Frost Dragons as a new task (MV-D11)
Re-emits via emit.py formatting so unchanged tasks produce zero diff."""
import json, collections, os, sys
sys.path.insert(0, os.path.dirname(__file__))
import emit

ROOT = "/home/danny/Documents/git/AIO-Slayer-Assistant"
DATA = f"{ROOT}/src/main/resources/data/slayer-data.json"
PROP = f"{ROOT}/docs/variants/.proposals"

GROUPS = ["demons", "dragons", "wyrms-wyverns", "undead-basilisks", "kalphite-misc",
          "giants-beasts", "devils-aerial", "humanoids", "misc", "boss"]

def load(p):
    return json.load(open(p), object_pairs_hook=collections.OrderedDict)

def main():
    data = load(DATA)
    by_name = {t["task"]: t for t in data}

    proposals = {}
    for g in GROUPS:
        path = f"{PROP}/{g}.json"
        if not os.path.exists(path):
            print(f"WARNING: missing proposal {g}.json", file=sys.stderr)
            continue
        obj = load(path)
        for task, variants in obj.items():
            proposals[task] = variants

    attached, missing = [], []
    for task, variants in proposals.items():
        if task == "Frost Dragons":
            continue  # new task, handled below
        if task not in by_name:
            missing.append(task)
            continue
        t = by_name[task]
        if task == "Boss":
            # ADR-0014: the meta-task has no own profile; each boss variant is self-contained.
            t["variants"] = variants
            t["weakness"] = None
            t["monsterDefence"] = None
            attached.append(f"{task}({len(variants)})")
            continue
        # FR-6 anchor: normalise the isDefault variant to EXACTLY the task profile (style/element/
        # defence/category flags) so a fresh/default selection reproduces today's loadout byte-for-byte,
        # regardless of any transcription drift. Non-default variants keep their wiki-refined values.
        defaults = [v for v in variants if v.get("isDefault")]
        if len(defaults) == 1:
            d = defaults[0]
            d["weakness"] = collections.OrderedDict(t.get("weakness") or {})
            d["monsterDefence"] = collections.OrderedDict(t.get("monsterDefence") or {})
            for flag in ("demon", "dragon", "kalphite", "undead"):
                d[flag] = bool(t.get(flag, False))
        t["variants"] = variants
        attached.append(f"{task}({len(variants)})")

    # Frost Dragons (MV-D11): build task 43 from the dragons proposal's variant + authored task fields.
    if "Frost Dragons" in proposals:
        fv = proposals["Frost Dragons"]
        default = next((v for v in fv if v.get("isDefault")), fv[0])
        frost = collections.OrderedDict()
        frost["task"] = "Frost Dragons"
        frost["slayerTargetId"] = 9001  # synthetic non-colliding id; live varp mapping is a QA item
        frost["slayerLevel"] = 85
        frost["questReqs"] = []
        frost["assignedBy"] = ["duradel"]
        frost["amountByMaster"] = {"duradel": [40, 60]}
        frost["monsters"] = ["Frost dragon"]
        frost["npcIds"] = default.get("npcIds", [14922])
        frost["weakness"] = default.get("weakness", {"style": "RANGED", "element": "fire"})
        frost["monsterDefence"] = default.get("monsterDefence")
        frost["slayerHelmApplies"] = True
        frost["dragon"] = True
        frost["requiredItemId"] = None
        frost["requiredItemName"] = None
        frost["locations"] = [{"name": "Asgarnian Ice Dungeon (deep)", "multi": False,
                               "cannon": False, "burst": False, "konarLockable": False}]
        frost["recommendedMethod"] = "Ranged/Melee; Sailing access. Frost dragons (dragonbane applies)"
        frost["variants"] = fv
        data.append(frost)
        attached.append("Frost Dragons(NEW)")

    open(DATA, "w").write(emit.emit(data))
    print("ATTACHED:", ", ".join(attached))
    if missing:
        print("MISSING task in data (proposal name mismatch):", missing, file=sys.stderr)
        sys.exit(2)

if __name__ == "__main__":
    main()

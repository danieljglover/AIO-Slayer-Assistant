#!/usr/bin/env python3
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources/data/slayer-data.json"
OUT = ROOT / "src/main/data/slayer"


def slug(value):
    text = value.lower().replace("&", " and ")
    text = re.sub(r"[^a-z0-9]+", "-", text).strip("-")
    return text or "unnamed"


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def write_text(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(value, encoding="utf-8")


def first_item_id(strategy_weapon):
    item_id = strategy_weapon.get("itemId")
    if item_id is None:
        return None
    return int(item_id)


def read_source_data():
    if SOURCE.exists():
        return json.loads(SOURCE.read_text(encoding="utf-8"))
    return json.loads(subprocess.check_output([
        "git",
        "show",
        "HEAD:src/main/resources/data/slayer-data.json",
    ]))


def source_variant_record(variant):
    record = {
        "variantId": slug(variant["name"]),
        "name": variant.get("name"),
        "npcIds": variant.get("npcIds"),
        "combatLevel": variant.get("combatLevel"),
        "weakness": variant.get("weakness"),
        "monsterDefence": variant.get("monsterDefence"),
        "demon": variant.get("demon", False),
        "dragon": variant.get("dragon", False),
        "kalphite": variant.get("kalphite", False),
        "undead": variant.get("undead", False),
        "boss": variant.get("isBoss", False),
        "location": variant.get("location"),
        "requirement": variant.get("requirement"),
        "bossId": variant.get("bossId"),
    }
    if variant.get("strategy"):
        record["strategyId"] = slug(variant["name"])
    return record


def variant_filename(variant):
    name = variant["variantId"].replace("level-", "lvl")
    level = variant.get("combatLevel")
    if level is not None and f"lvl{level}" not in name:
        name = f"{name}-lvl{level}"
    return f"{name}.json"


def main():
    data = read_source_data()
    masters = {}
    locations = {}
    weapons = {}
    strategies = {}
    seen_variant_ids = set()
    variant_signatures = {}
    variant_ids_by_task = {}

    for task in data:
        for variant in task.get("variants") or []:
            base_variant_id = slug(variant["name"])
            signature = json.dumps(source_variant_record(variant), sort_keys=True)
            variant_signatures.setdefault(base_variant_id, set()).add(signature)

    conflicting_variant_ids = {
        variant_id for variant_id, signatures in variant_signatures.items()
        if len(signatures) > 1
    }

    for task in data:
        task_id = slug(task["task"])
        variant_ids = []
        for variant in task.get("variants") or []:
            base_variant_id = slug(variant["name"])
            if base_variant_id in conflicting_variant_ids:
                variant_ids.append(f"{task_id}-{base_variant_id}")
            else:
                variant_ids.append(base_variant_id)
        variant_ids_by_task[task_id] = variant_ids

    for task in data:
        task_id = slug(task["task"])
        for master_id in task.get("assignedBy") or []:
            masters.setdefault(master_id, {
                "masterId": master_id,
                "name": master_id.capitalize(),
            })

        for location in task.get("locations") or []:
            location_id = slug(location["name"])
            locations.setdefault(location_id, {
                "locationId": location_id,
                **location,
            })

        for index, variant in enumerate(task.get("variants") or []):
            strategy = variant.get("strategy")
            if not strategy:
                continue
            strategy_id = slug(variant["name"])
            entry = strategies.setdefault(strategy_id, {
                "variantName": variant["name"],
                "variantIds": set(),
                "strategy": strategy,
            })
            entry["variantIds"].add(variant_ids_by_task[task_id][index])
            for weapon in (strategy.get("primaryWeapons") or []) + (strategy.get("secondaryWeapons") or []):
                item_id = first_item_id(weapon)
                if item_id is None:
                    continue
                weapon_id = slug(weapon["name"])
                record = weapons.setdefault(weapon_id, {
                    "weaponId": weapon_id,
                    "name": weapon["name"],
                    "itemIds": [],
                })
                if item_id not in record["itemIds"]:
                    record["itemIds"].append(item_id)

    for master_id, master in sorted(masters.items()):
        write_json(OUT / "masters" / f"{master_id}.json", master)

    for location_id, location in sorted(locations.items()):
        write_json(OUT / "locations" / f"{location_id}.json", location)

    for weapon_id, weapon in sorted(weapons.items()):
        weapon["itemIds"].sort()
        write_json(OUT / "weapons" / f"{weapon_id}.json", weapon)

    for task in data:
        task_id = slug(task["task"])
        variant_ids = variant_ids_by_task[task_id]
        default_variant_id = None
        for index, variant in enumerate(task.get("variants") or []):
            if variant.get("isDefault"):
                default_variant_id = variant_ids[index]
                break
        source_task = {
            "taskId": task_id,
            "name": task.get("task"),
            "slayerTargetId": task.get("slayerTargetId", 0),
            "slayerLevel": task.get("slayerLevel", 1),
            "questReqs": task.get("questReqs") or [],
            "masterIds": task.get("assignedBy") or [],
            "amountByMaster": task.get("amountByMaster"),
            "monsterIds": [task_id],
            "variantIds": variant_ids,
            "defaultVariantId": default_variant_id,
            "weakness": task.get("weakness"),
            "monsterDefence": task.get("monsterDefence"),
            "slayerHelmApplies": task.get("slayerHelmApplies", False),
            "undead": task.get("undead", False),
            "dragon": task.get("dragon", False),
            "demon": task.get("demon", False),
            "kalphite": task.get("kalphite", False),
            "requiredItemId": task.get("requiredItemId"),
            "requiredItemName": task.get("requiredItemName"),
            "locationIds": [slug(l["name"]) for l in task.get("locations") or []],
            "recommendedMethod": task.get("recommendedMethod"),
        }
        write_json(OUT / "tasks" / f"{task_id}.json", source_task)

        used_variant_filenames = set()
        for index, variant in enumerate(task.get("variants") or []):
            variant_id = variant_ids[index]
            if variant_id in seen_variant_ids:
                continue
            seen_variant_ids.add(variant_id)
            source_variant = source_variant_record(variant)
            source_variant["variantId"] = variant_id
            filename = variant_filename(source_variant)
            if filename in used_variant_filenames:
                filename = f"{filename[:-5]}-{index + 1}.json"
            used_variant_filenames.add(filename)
            write_json(OUT / "monsters" / task_id / filename, source_variant)

    for strategy_id, entry in sorted(strategies.items()):
        strategy = entry["strategy"]
        primary_weapons = [slug(w["name"]) for w in strategy.get("primaryWeapons") or []]
        secondary_weapons = []
        for weapon in strategy.get("secondaryWeapons") or []:
            secondary_weapons.append({
                "weaponId": slug(weapon["name"]),
                "style": weapon.get("style"),
            })
        lines = [
            "---",
            f"strategyId: {strategy_id}",
            "variantIds: [" + ", ".join(sorted(entry["variantIds"])) + "]",
            f"primaryStyle: {strategy.get('primaryStyle')}",
            "primaryWeapons: [" + ", ".join(primary_weapons) + "]",
        ]
        if secondary_weapons:
            lines.append("secondaryWeapons:")
            for weapon in secondary_weapons:
                lines.append(f"  - weaponId: {weapon['weaponId']}")
                lines.append(f"    style: {weapon['style']}")
        if strategy.get("note"):
            lines.append("note: " + strategy["note"].replace("\n", " "))
        if strategy.get("sourceUrl"):
            lines.append("sourceUrl: " + strategy["sourceUrl"])
        lines.extend([
            "---",
            f"# {entry['variantName']}",
            "",
            strategy.get("note") or "Strategy context imported from the source runtime dataset.",
            "",
        ])
        write_text(OUT / "strategies" / f"{strategy_id}.md", "\n".join(lines))


if __name__ == "__main__":
    main()

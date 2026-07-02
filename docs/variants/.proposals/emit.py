#!/usr/bin/env python3
"""Faithful emitter for slayer-data.json that preserves the curated formatting, so injecting
`variants` produces a minimal diff. Round-trip-verified against the current file before use."""
import json, sys, collections

INDENT = "  "

def jv(v):
    return json.dumps(v, ensure_ascii=False)

def inline_obj(d):
    # one-line object: { "k": v, ... }
    return "{ " + ", ".join(f'{jv(k)}: {jv(v)}' for k, v in d.items()) + " }"

# scalar/array fields that render inline on their own line
def render_field(key, val, indent):
    pad = INDENT * indent
    if isinstance(val, dict):
        return f'{pad}{jv(key)}: {inline_obj(val)}'
    if key == "locations" and isinstance(val, list):
        if not val:
            return f'{pad}{jv(key)}: []'
        inner = ",\n".join(f'{INDENT*(indent+1)}{inline_obj(l)}' for l in val)
        return f'{pad}{jv(key)}: [\n{inner}\n{pad}]'
    if key == "variants" and isinstance(val, list):
        if not val:
            return f'{pad}{jv(key)}: []'
        inner = ",\n".join(render_variant(v, indent+1) for v in val)
        return f'{pad}{jv(key)}: [\n{inner}\n{pad}]'
    # plain scalar or simple array -> inline json
    return f'{pad}{jv(key)}: {jv(val)}'

def render_variant(v, indent):
    pad = INDENT * indent
    parts = []
    for k, val in v.items():
        if isinstance(val, dict):
            parts.append(f'{jv(k)}: {inline_obj(val)}')
        else:
            parts.append(f'{jv(k)}: {jv(val)}')
    return pad + "{ " + ", ".join(parts) + " }"

def render_task(t, indent=1):
    pad = INDENT * indent
    lines = [render_field(k, v, indent+1) for k, v in t.items()]
    return f'{pad}{{\n' + ",\n".join(lines) + f'\n{pad}}}'

def emit(data):
    return "[\n" + ",\n".join(render_task(t) for t in data) + "\n]\n"

if __name__ == "__main__":
    path = sys.argv[1]
    data = json.load(open(path), object_pairs_hook=collections.OrderedDict)
    sys.stdout.write(emit(data))

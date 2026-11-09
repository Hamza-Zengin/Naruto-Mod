#!/usr/bin/env python3
"""
Pre-build audit script for Naruto-Mod models, layer definitions, and json assets.
Prevents NoSuchElementException and missing model resource crashes at runtime.
"""
import os
import sys
import re
import json

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_JAVA = os.path.join(REPO_ROOT, "src/main/java")
SRC_ASSETS = os.path.join(REPO_ROOT, "src/main/resources/assets/narutomod")

errors = []

# 1. Audit getChild vs addOrReplaceChild across all Java classes
for root, dirs, files in os.walk(SRC_JAVA):
    for f in files:
        if f.endswith(".java"):
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8") as fl:
                lines = fl.readlines()
            content = "".join(lines)
            
            # Find all getChild calls
            for idx, line in enumerate(lines):
                matches = re.findall(r'\.getChild\(\"([^\"]+)\"\)', line)
                for part in matches:
                    # Check if part is added in this file
                    if f'addOrReplaceChild("{part}"' not in content:
                        errors.append(f"{f}:{idx+1} - ModelPart '{part}' requested via getChild() but never defined with addOrReplaceChild()!")

# 2. Audit ModelLayerLocations in NarutoRenderEvents
render_events_path = os.path.join(SRC_JAVA, "com/sekwah/narutomod/client/renderer/NarutoRenderEvents.java")
if os.path.exists(render_events_path):
    with open(render_events_path, "r", encoding="utf-8") as fl:
        re_content = fl.read()
    layers = re.findall(r'public static final ModelLayerLocation ([A-Z0-9_]+_LAYER)\s*=', re_content)
    for layer in layers:
        if f"registerLayerDefinition({layer}" not in re_content:
            errors.append(f"NarutoRenderEvents.java: ModelLayerLocation '{layer}' defined but not registered in layerDefinitions()!")

# 3. Audit Item & Block Model JSON references
models_dir = os.path.join(SRC_ASSETS, "models")
textures_dir = os.path.join(SRC_ASSETS, "textures")

if os.path.exists(models_dir):
    for root, dirs, files in os.walk(models_dir):
        for f in files:
            if f.endswith(".json"):
                p = os.path.join(root, f)
                with open(p, "r", encoding="utf-8") as fl:
                    try:
                        data = json.load(fl)
                    except Exception as e:
                        errors.append(f"Invalid JSON {p}: {e}")
                        continue
                
                def check_parent(parent_path):
                    if parent_path.startswith("narutomod:"):
                        rel = parent_path.replace("narutomod:", "")
                        target = os.path.join(models_dir, rel + ".json")
                        if not os.path.exists(target):
                            errors.append(f"{f} references missing parent model: {target}")

                if "parent" in data:
                    check_parent(data["parent"])
                if "base" in data and isinstance(data["base"], dict) and "parent" in data["base"]:
                    check_parent(data["base"]["parent"])
                if "perspectives" in data and isinstance(data["perspectives"], dict):
                    for pk, pv in data["perspectives"].items():
                        if isinstance(pv, dict) and "parent" in pv:
                            check_parent(pv["parent"])

                textures = data.get("textures", {})
                for tk, tv in textures.items():
                    if isinstance(tv, str) and tv.startswith("narutomod:"):
                        rel = tv.replace("narutomod:", "")
                        target = os.path.join(textures_dir, rel + ".png")
                        if not os.path.exists(target):
                            errors.append(f"{f} references missing texture: {target}")

if errors:
    print(f"FAILED: Found {len(errors)} model/part audit errors:")
    for err in errors:
        print(f"  [ERROR] {err}")
    sys.exit(1)
else:
    print("SUCCESS: All Java model parts, layer definitions, and JSON assets are 100% consistent!")
    sys.exit(0)

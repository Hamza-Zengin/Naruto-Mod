#!/usr/bin/env python3
import json
import os
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS_ITEM_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "assets", "narutomod", "models", "item")

def resolve_model_path(model_id):
    # e.g., "narutomod:item/weapons/kunai_3d" -> "src/main/resources/assets/narutomod/models/item/weapons/kunai_3d.json"
    if ":" in model_id:
        domain, path = model_id.split(":", 1)
    else:
        domain = "minecraft"
        path = model_id

    if domain == "narutomod":
        # path is like item/weapons/kunai_3d
        rel = path.replace("item/", "", 1) if path.startswith("item/") else path
        return os.path.join(MODELS_ITEM_DIR, rel + ".json")
    return None

def main():
    errors = []
    checked = 0
    separate_transforms = 0

    for fname in os.listdir(MODELS_ITEM_DIR):
        if not fname.endswith(".json"):
            continue

        fpath = os.path.join(MODELS_ITEM_DIR, fname)
        checked += 1
        try:
            with open(fpath, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            errors.append(f"Invalid JSON in {fname}: {e}")
            continue

        if data.get("loader") == "forge:separate_transforms":
            separate_transforms += 1
            base = data.get("base", {}).get("parent")
            if not base:
                errors.append(f"{fname}: Missing 'base.parent' in separate_transforms")
            else:
                base_path = resolve_model_path(base)
                if base_path and not os.path.exists(base_path):
                    errors.append(f"{fname}: base parent '{base}' does not exist at {base_path}")
                elif base_path:
                    with open(base_path, "r", encoding="utf-8") as bf:
                        bdata = json.load(bf)
                        if "display" not in bdata or "firstperson_righthand" not in bdata["display"]:
                            errors.append(f"{fname}: base model {base} lacks 'firstperson_righthand' display configuration")

            persp = data.get("perspectives", {})
            gui = persp.get("gui", {}).get("parent")
            if not gui:
                errors.append(f"{fname}: Missing 'perspectives.gui.parent'")
            else:
                gui_path = resolve_model_path(gui)
                if gui_path and not os.path.exists(gui_path):
                    errors.append(f"{fname}: gui parent '{gui}' does not exist at {gui_path}")

    print(f"Checked {checked} item models. Found {separate_transforms} using forge:separate_transforms.")
    if errors:
        print("ERRORS FOUND:")
        for err in errors:
            print(f" - {err}")
        sys.exit(1)
    else:
        print("SUCCESS: All separate_transforms weapon models and perspectives are 100% valid!")

if __name__ == "__main__":
    main()

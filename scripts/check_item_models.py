#!/usr/bin/env python3
import json
import os
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEMS_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "assets", "narutomod", "items")
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
    checked_models = 0
    checked_client_items = 0
    select_display_context = 0

    if not os.path.isdir(ITEMS_DIR):
        errors.append(f"Client items directory does not exist: {ITEMS_DIR}")
    else:
        for fname in sorted(os.listdir(ITEMS_DIR)):
            if not fname.endswith(".json"):
                continue

            fpath = os.path.join(ITEMS_DIR, fname)
            checked_client_items += 1
            try:
                with open(fpath, "r", encoding="utf-8") as f:
                    data = json.load(f)
            except Exception as e:
                errors.append(f"Invalid JSON in items/{fname}: {e}")
                continue

            model_obj = data.get("model", {})
            if model_obj.get("type") == "minecraft:select" and model_obj.get("property") == "minecraft:display_context":
                select_display_context += 1

                fallback = model_obj.get("fallback", {}).get("model")
                if not fallback:
                    errors.append(f"items/{fname}: Missing 'fallback.model'")
                else:
                    fallback_path = resolve_model_path(fallback)
                    if fallback_path and not os.path.exists(fallback_path):
                        errors.append(f"items/{fname}: fallback model '{fallback}' does not exist at {fallback_path}")
                    elif fallback_path:
                        with open(fallback_path, "r", encoding="utf-8") as bf:
                            bdata = json.load(bf)
                            if "display" not in bdata or "firstperson_righthand" not in bdata["display"]:
                                errors.append(f"items/{fname}: fallback model {fallback} lacks 'firstperson_righthand' display configuration")

                cases = model_obj.get("cases", [])
                if not cases:
                    errors.append(f"items/{fname}: Missing 'cases'")
                else:
                    covered = set()
                    for case in cases:
                        for ctx in case.get("when", []):
                            covered.add(ctx)
                        gui = case.get("model", {}).get("model")
                        if not gui:
                            errors.append(f"items/{fname}: Case missing 'model.model'")
                        else:
                            gui_path = resolve_model_path(gui)
                            if gui_path and not os.path.exists(gui_path):
                                errors.append(f"items/{fname}: gui model '{gui}' does not exist at {gui_path}")
                    for expected_ctx in ("gui", "ground", "fixed"):
                        if expected_ctx not in covered:
                            errors.append(f"items/{fname}: Missing display_context '{expected_ctx}' in cases")

    for fname in sorted(os.listdir(MODELS_ITEM_DIR)):
        if not fname.endswith(".json"):
            continue

        fpath = os.path.join(MODELS_ITEM_DIR, fname)
        checked_models += 1
        try:
            with open(fpath, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            errors.append(f"Invalid JSON in models/item/{fname}: {e}")
            continue

        if data.get("loader") == "neoforge:separate_transforms":
            errors.append(f"models/item/{fname}: Still uses removed 'neoforge:separate_transforms' loader!")

    if select_display_context < 8:
        errors.append(f"Expected at least 8 client item definitions using minecraft:select + minecraft:display_context, found {select_display_context}")

    print(f"Checked {checked_client_items} client items ({select_display_context} using minecraft:select/display_context) and {checked_models} item models.")
    if errors:
        print("ERRORS FOUND:")
        for err in errors:
            print(f" - {err}")
        sys.exit(1)
    else:
        print("SUCCESS: All 26.3 client item definitions and item models are 100% valid!")

if __name__ == "__main__":
    main()

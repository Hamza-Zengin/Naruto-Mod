#!/usr/bin/env python3
"""
Checks that all textures under assets/narutomod/textures/ are referenced
by at least one block/item model JSON, entity/gui renderer in Java, or code.
"""

import os
import sys

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    textures_dir = os.path.join(root, "src/main/resources/assets/narutomod/textures")
    models_dir = os.path.join(root, "src/main/resources/assets/narutomod/models")
    java_dir = os.path.join(root, "src/main/java")
    resources_dir = os.path.join(root, "src/main/resources")

    # Collect all texture basenames and relative paths without extension
    all_textures = []
    for dirpath, _, filenames in os.walk(textures_dir):
        for f in filenames:
            if f.endswith(".png"):
                full_path = os.path.join(dirpath, f)
                rel_path = os.path.relpath(full_path, textures_dir)
                all_textures.append(rel_path)

    # Read all text in models, resources, and java
    search_corpus = []
    for scan_dir in [models_dir, java_dir, resources_dir]:
        for dirpath, _, filenames in os.walk(scan_dir):
            for f in filenames:
                if f.endswith((".json", ".java", ".toml", ".mcmeta")):
                    try:
                        with open(os.path.join(dirpath, f), "r", encoding="utf-8", errors="ignore") as file:
                            search_corpus.append(file.read())
                    except Exception:
                        pass

    combined_corpus = "\n".join(search_corpus)

    unreferenced = []
    for tex in all_textures:
        name_no_ext = os.path.splitext(tex)[0]
        base_no_ext = os.path.basename(name_no_ext)
        # Check either full path (e.g. item/weapons/kunai) or basename (kunai)
        if name_no_ext not in combined_corpus and base_no_ext not in combined_corpus:
            unreferenced.append(tex)

    if unreferenced:
        print(f"WARNING/FAIL: Found {len(unreferenced)} potentially unreferenced textures:")
        for t in unreferenced:
            print(f"  - {t}")
        sys.exit(1)
    else:
        print(f"SUCCESS: All {len(all_textures)} textures are referenced in models or code!")

if __name__ == "__main__":
    main()

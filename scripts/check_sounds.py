#!/usr/bin/env python3
"""
Sound registration audit script for Naruto-Mod.
Verifies that all SoundEvent keys registered in Java exist in sounds.json,
all sound files exist on disk, and all subtitles are localized in en_us.json.
"""
import os
import sys
import re
import json
import hashlib

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOUNDS_JAVA = os.path.join(REPO_ROOT, "src/main/java/com/sekwah/narutomod/sounds/NarutoSounds.java")
SOUNDS_JSON = os.path.join(REPO_ROOT, "src/main/resources/assets/narutomod/sounds.json")
SOUNDS_DIR = os.path.join(REPO_ROOT, "src/main/resources/assets/narutomod/sounds")
EN_LANG = os.path.join(REPO_ROOT, "src/main/resources/assets/narutomod/lang/en_us.json")

# Relative paths under assets/narutomod/sounds/ that are intentionally shared duplicates
ALLOWED_DUPLICATES = set()

errors = []

# 1. Parse Java registered sound keys
if not os.path.exists(SOUNDS_JAVA):
    errors.append(f"Missing {SOUNDS_JAVA}")
    sys.exit(1)

with open(SOUNDS_JAVA, "r", encoding="utf-8") as f:
    java_content = f.read()

# Pattern: register("sound_key")
java_sounds = set(re.findall(r'register\(\"([^\"]+)\"\)', java_content))
print(f"Found {len(java_sounds)} SoundEvents registered in NarutoSounds.java: {sorted(java_sounds)}")

# 2. Parse sounds.json
if not os.path.exists(SOUNDS_JSON):
    errors.append(f"Missing {SOUNDS_JSON}")
    sys.exit(1)

with open(SOUNDS_JSON, "r", encoding="utf-8") as f:
    try:
        sounds_data = json.load(f)
    except Exception as e:
        errors.append(f"Invalid JSON in sounds.json: {e}")
        sounds_data = {}

# Check for missing sounds in sounds.json
json_keys = set(sounds_data.keys())
missing_in_json = java_sounds - json_keys
if missing_in_json:
    errors.append(f"SoundEvents registered in Java but MISSING in sounds.json: {sorted(missing_in_json)}")

# 3. Check language file subtitles
with open(EN_LANG, "r", encoding="utf-8") as f:
    en_lang = json.load(f)

# 4. Check sound file references and subtitles
for event_name, event_data in sounds_data.items():
    if "subtitle" in event_data:
        sub = event_data["subtitle"]
        if sub not in en_lang:
            errors.append(f"Sound '{event_name}' references subtitle '{sub}' which is missing in en_us.json!")

    for s in event_data.get("sounds", []):
        s_name = s if isinstance(s, str) else s.get("name", "")
        if s_name.startswith("minecraft:"):
            # Vanilla Minecraft sound - valid
            continue
        if s_name.startswith("narutomod:"):
            rel_name = s_name.replace("narutomod:", "")
            target_file = os.path.join(SOUNDS_DIR, rel_name + ".ogg")
            if not os.path.exists(target_file):
                errors.append(f"Sound '{event_name}' references missing ogg file: {target_file}")
        else:
            target_file = os.path.join(SOUNDS_DIR, s_name + ".ogg")
            if not os.path.exists(target_file):
                errors.append(f"Sound '{event_name}' references missing ogg file: {target_file}")

# 5. Check for duplicate placeholder .ogg files via SHA-256
hash_to_files = {}
for root, _, files in os.walk(SOUNDS_DIR):
    for fname in sorted(files):
        if not fname.endswith(".ogg"):
            continue
        full_path = os.path.join(root, fname)
        rel_path = os.path.relpath(full_path, SOUNDS_DIR).replace("\\", "/")
        if rel_path in ALLOWED_DUPLICATES:
            continue
        with open(full_path, "rb") as f:
            file_hash = hashlib.sha256(f.read()).hexdigest()
        hash_to_files.setdefault(file_hash, []).append(rel_path)

for file_hash, paths in hash_to_files.items():
    if len(paths) > 1:
        errors.append(
            f"Duplicate placeholder .ogg files detected (SHA-256 {file_hash[:12]}...): {paths}"
        )

if errors:
    print(f"\nFAILED: Found {len(errors)} sound audit errors:")
    for err in errors:
        print(f"  ❌ {err}")
    sys.exit(1)

print("\nSUCCESS: All Naruto-Mod SoundEvents, sounds.json mappings, audio files, and subtitles are 100% consistent!")
sys.exit(0)

#!/usr/bin/env python3
import json
import sys
from pathlib import Path

def main():
    lang_dir = Path("src/main/resources/assets/narutomod/lang")
    en_path = lang_dir / "en_us.json"
    if not en_path.exists():
        print(f"Error: {en_path} not found!", file=sys.stderr)
        return 1

    with open(en_path, "r", encoding="utf-8") as f:
        en_data = json.load(f)

    en_keys = set(en_data.keys())
    has_errors = False

    for lang_file in sorted(lang_dir.glob("*.json")):
        if lang_file.name == "en_us.json":
            continue

        try:
            with open(lang_file, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            print(f"❌ {lang_file.name}: Invalid JSON: {e}", file=sys.stderr)
            has_errors = True
            continue

        keys = set(data.keys())
        missing = en_keys - keys
        extra = keys - en_keys

        if missing or extra:
            has_errors = True
            print(f"❌ {lang_file.name}: {len(missing)} missing keys, {len(extra)} extra keys")
            if missing:
                print(f"   Missing sample: {list(missing)[:5]}")
            if extra:
                print(f"   Extra sample: {list(extra)[:5]}")
        else:
            print(f"✅ {lang_file.name}: Perfect parity ({len(keys)} keys)")

    if has_errors:
        print("\nLanguage audit failed!", file=sys.stderr)
        return 1
    print("\nAll language files match en_us.json perfectly!")
    return 0

if __name__ == "__main__":
    sys.exit(main())

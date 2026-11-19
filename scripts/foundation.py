#!/usr/bin/env python3
"""
Audits and repairs foundation support layers (y=0) across all Naruto Mod
structure .nbt files so no non-bridge structure piece has unsupported base columns.

Usage:
  python3 scripts/foundation.py          # Repair any unsupported base columns
  python3 scripts/foundation.py --check  # Verify without modifying files
"""
import argparse
import glob
import os
import sys
import nbtlib
from nbtlib.tag import Compound, Int, String

STRUCT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "data", "narutomod", "structure",
)

EXCLUDED = {"empty.nbt", "kumo_sky_bridge.nbt"}

FOUNDATION_DEFAULTS = {
    "suna_": "minecraft:sandstone",
    "kiri_": "minecraft:dark_prismarine",
    "kumo_": "minecraft:polished_diorite",
    "iwa_": "minecraft:stone_bricks",
    "akatsuki_": "minecraft:deepslate_tiles",
    "myobokuzan_": "minecraft:mossy_stone_bricks",
    "valley_madara": "minecraft:deepslate",
    "valley_": "minecraft:stone",
}


def pick_foundation_block(filename: str) -> str:
    for prefix, block in FOUNDATION_DEFAULTS.items():
        if filename.startswith(prefix):
            return block
    return "minecraft:cobblestone"


def audit_and_fix(check_only: bool) -> int:
    files = sorted(glob.glob(os.path.join(STRUCT_DIR, "*.nbt")))
    issues = 0
    fixed = 0

    for path in files:
        name = os.path.basename(path)
        if name in EXCLUDED:
            continue

        nbt_file = nbtlib.load(path)
        palette = nbt_file["palette"]
        blocks = nbt_file["blocks"]
        air_ids = {
            i for i, e in enumerate(palette)
            if str(e["Name"]) in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air")
        }

        y0_cols = set()
        y1_solid_cols = set()
        for b in blocks:
            state = int(b["state"])
            if state in air_ids:
                continue
            x, y, z = (int(b["pos"][0]), int(b["pos"][1]), int(b["pos"][2]))
            if y == 0:
                y0_cols.add((x, z))
            elif y == 1:
                y1_solid_cols.add((x, z))

        missing = sorted(y1_solid_cols - y0_cols)
        if not missing:
            continue

        issues += len(missing)
        if check_only:
            print(f"[FAIL] {name}: {len(missing)} column(s) have y=1 blocks without y=0 support")
            continue

        fill_name = pick_foundation_block(name)
        fill_state = next((i for i, e in enumerate(palette) if str(e["Name"]) == fill_name), None)
        if fill_state is None:
            fill_state = len(palette)
            palette.append(Compound({"Name": String(fill_name)}))

        for (x, z) in missing:
            blocks.append(Compound({
                "pos": nbtlib.tag.List[Int]([Int(x), Int(0), Int(z)]),
                "state": Int(fill_state),
            }))

        nbtlib.File(nbt_file.root if hasattr(nbt_file, "root") else nbt_file).save(path, gzipped=True)
        fixed += 1
        print(f"[FIXED] {name}: added {len(missing)} foundation blocks ({fill_name})")

    if check_only:
        if issues > 0:
            print(f"FAILED: {issues} unsupported base columns found across structure .nbt files.")
            return 1
        print(f"SUCCESS: All {len(files) - len(EXCLUDED)} grounded structure .nbt files have solid y=0 base support.")
        return 0

    print(f"Done. Fixed {fixed} files ({issues} columns).")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit/fix base foundation support in structure .nbt files.")
    parser.add_argument("--check", action="store_true", help="Only check without modifying files")
    args = parser.parse_args()
    return audit_and_fix(args.check)


if __name__ == "__main__":
    sys.exit(main())

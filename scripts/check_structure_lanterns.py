#!/usr/bin/env python3
"""
Checks all structure .nbt files under src/main/resources/data/narutomod/structure/*.nbt
for unsupported or floating lanterns (minecraft:lantern, minecraft:soul_lantern,
narutomod:stone_lantern, minecraft:sea_lantern).

Uses only Python standard library modules.
"""

import gzip
import io
import pathlib
import struct
import sys

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

AIR_AND_LIQUID_BLOCKS = {
    "minecraft:air",
    "minecraft:cave_air",
    "minecraft:void_air",
    "minecraft:structure_void",
    "minecraft:water",
    "minecraft:lava",
    "minecraft:bubble_column",
}

NON_SUPPORT_BLOCKS = AIR_AND_LIQUID_BLOCKS | {
    "minecraft:lantern",
    "minecraft:soul_lantern",
    "minecraft:torch",
    "minecraft:wall_torch",
    "minecraft:soul_torch",
    "minecraft:soul_wall_torch",
    "minecraft:redstone_torch",
    "minecraft:redstone_wall_torch",
    "minecraft:vine",
    "minecraft:short_grass",
    "minecraft:tall_grass",
    "minecraft:fern",
    "minecraft:large_fern",
    "minecraft:dead_bush",
    "minecraft:seagrass",
    "minecraft:tall_seagrass",
    "minecraft:kelp",
    "minecraft:kelp_plant",
}


class NBTReader:
    def __init__(self, data: bytes):
        self.buf = io.BytesIO(data)

    def read_u8(self) -> int:
        return struct.unpack(">B", self.buf.read(1))[0]

    def read_i8(self) -> int:
        return struct.unpack(">b", self.buf.read(1))[0]

    def read_i16(self) -> int:
        return struct.unpack(">h", self.buf.read(2))[0]

    def read_u16(self) -> int:
        return struct.unpack(">H", self.buf.read(2))[0]

    def read_i32(self) -> int:
        return struct.unpack(">i", self.buf.read(4))[0]

    def read_i64(self) -> int:
        return struct.unpack(">q", self.buf.read(8))[0]

    def read_f32(self) -> float:
        return struct.unpack(">f", self.buf.read(4))[0]

    def read_f64(self) -> float:
        return struct.unpack(">d", self.buf.read(8))[0]

    def read_string(self) -> str:
        length = self.read_u16()
        raw = self.buf.read(length)
        return raw.decode("utf-8", errors="replace")

    def read_tag_payload(self, tag_type: int):
        if tag_type == TAG_BYTE:
            return self.read_i8()
        if tag_type == TAG_SHORT:
            return self.read_i16()
        if tag_type == TAG_INT:
            return self.read_i32()
        if tag_type == TAG_LONG:
            return self.read_i64()
        if tag_type == TAG_FLOAT:
            return self.read_f32()
        if tag_type == TAG_DOUBLE:
            return self.read_f64()
        if tag_type == TAG_BYTE_ARRAY:
            length = self.read_i32()
            return list(struct.unpack(f">{length}b", self.buf.read(length)))
        if tag_type == TAG_STRING:
            return self.read_string()
        if tag_type == TAG_LIST:
            elem_type = self.read_u8()
            length = self.read_i32()
            return [self.read_tag_payload(elem_type) for _ in range(length)]
        if tag_type == TAG_COMPOUND:
            result = {}
            while True:
                t = self.read_u8()
                if t == TAG_END:
                    break
                name = self.read_string()
                result[name] = self.read_tag_payload(t)
            return result
        if tag_type == TAG_INT_ARRAY:
            length = self.read_i32()
            return list(struct.unpack(f">{length}i", self.buf.read(length * 4)))
        if tag_type == TAG_LONG_ARRAY:
            length = self.read_i32()
            return list(struct.unpack(f">{length}q", self.buf.read(length * 8)))
        raise ValueError(f"Unknown NBT tag type: {tag_type}")

    def read_root(self):
        root_type = self.read_u8()
        if root_type == TAG_END:
            return {}
        _root_name = self.read_string()
        return self.read_tag_payload(root_type)


def load_nbt(path: pathlib.Path) -> dict:
    raw = path.read_bytes()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    return NBTReader(raw).read_root()


def is_valid_support(block_name: str | None) -> bool:
    if block_name is None:
        return False
    return block_name not in NON_SUPPORT_BLOCKS


def check_file(path: pathlib.Path) -> list[str]:
    data = load_nbt(path)
    palette = data.get("palette", [])
    blocks = data.get("blocks", [])
    size = data.get("size", [0, 0, 0])

    pos_to_entry = {}
    for b in blocks:
        pos = tuple(b["pos"])
        state_idx = b["state"]
        if 0 <= state_idx < len(palette):
            pos_to_entry[pos] = palette[state_idx]

    errors = []
    for (x, y, z), entry in sorted(pos_to_entry.items()):
        name = entry.get("Name", "")
        props = entry.get("Properties", {})

        if name in ("minecraft:lantern", "minecraft:soul_lantern"):
            hanging = str(props.get("hanging", "false")).lower() == "true"
            if hanging:
                above_pos = (x, y + 1, z)
                above_entry = pos_to_entry.get(above_pos)
                above_name = above_entry.get("Name") if above_entry else None
                if y + 1 >= size[1] or not is_valid_support(above_name):
                    errors.append(
                        f"{path.name}: {name}[hanging=true] at ({x},{y},{z}) "
                        f"has invalid hanging support above ({x},{y+1},{z}): {above_name}"
                    )
            else:
                below_pos = (x, y - 1, z)
                below_entry = pos_to_entry.get(below_pos)
                below_name = below_entry.get("Name") if below_entry else None
                if y - 1 < 0 or not is_valid_support(below_name):
                    errors.append(
                        f"{path.name}: {name}[hanging=false] at ({x},{y},{z}) "
                        f"has invalid ground support below ({x},{y-1},{z}): {below_name}"
                    )

        elif name in ("narutomod:stone_lantern", "minecraft:sea_lantern"):
            below_pos = (x, y - 1, z)
            below_entry = pos_to_entry.get(below_pos)
            below_name = below_entry.get("Name") if below_entry else None
            if y - 1 < 0 or not is_valid_support(below_name):
                errors.append(
                    f"{path.name}: {name} at ({x},{y},{z}) "
                    f"has invalid ground support below ({x},{y-1},{z}): {below_name}"
                )

    return errors


def main() -> int:
    repo_root = pathlib.Path(__file__).resolve().parent.parent
    struct_dir = repo_root / "src" / "main" / "resources" / "data" / "narutomod" / "structure"
    nbt_files = sorted(struct_dir.glob("*.nbt"))

    if not nbt_files:
        print(f"No .nbt files found in {struct_dir}", file=sys.stderr)
        return 1

    all_errors = []
    for nbt_path in nbt_files:
        all_errors.extend(check_file(nbt_path))

    if all_errors:
        for err in all_errors:
            print(f"[ERROR] {err}")
        print(f"\nTotal lantern errors: {len(all_errors)} across {len(nbt_files)} structure files.")
        return 1

    print(f"OK: 0 lantern errors across {len(nbt_files)} structure files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

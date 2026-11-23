#!/usr/bin/env python3
"""
Pads all village, shrine, and arena NBT structure files under
src/main/resources/data/narutomod/structure/ with explicit minecraft:air
blocks for every empty voxel in y >= 1 (keeping y == 0 untouched).

Excludes:
  - empty.nbt
  - kumo_sky_bridge.nbt
  - valley_*.nbt
"""
import argparse
import gzip
import io
import os
import struct
import sys

try:
    import nbtlib
    from nbtlib.tag import Compound, Int, List, String
except ImportError:
    class _NBTList(list):
        _elem_tag = 0

    class _ListMeta:
        def __getitem__(self, item_cls):
            class TypedList(_NBTList):
                _elem_tag = getattr(item_cls, "_tag_id", 10)
            return TypedList

    class Byte(int):
        _tag_id = 1

    class Short(int):
        _tag_id = 2

    class Int(int):
        _tag_id = 3

    class Long(int):
        _tag_id = 4

    class Float(float):
        _tag_id = 5

    class Double(float):
        _tag_id = 6

    class ByteArray(list):
        _tag_id = 7

    class String(str):
        _tag_id = 8

    class Compound(dict):
        _tag_id = 10

    class IntArray(list):
        _tag_id = 11

    class LongArray(list):
        _tag_id = 12

    List = _ListMeta()

    def _tag_id_of(val) -> int:
        if hasattr(val, "_tag_id"):
            return val._tag_id
        if isinstance(val, _NBTList):
            return 9
        if isinstance(val, dict):
            return 10
        if isinstance(val, list):
            return 9
        if isinstance(val, str):
            return 8
        if isinstance(val, bool):
            return 1
        if isinstance(val, int):
            return 3
        if isinstance(val, float):
            return 6
        raise TypeError(f"Unsupported NBT value type: {type(val)}")

    def _write_str(buf: io.BytesIO, s: str):
        raw = s.encode("utf-8")
        buf.write(struct.pack(">H", len(raw)))
        buf.write(raw)

    def _write_payload(buf: io.BytesIO, tag_id: int, val):
        if tag_id == 1:
            buf.write(struct.pack(">b", int(val)))
        elif tag_id == 2:
            buf.write(struct.pack(">h", int(val)))
        elif tag_id == 3:
            buf.write(struct.pack(">i", int(val)))
        elif tag_id == 4:
            buf.write(struct.pack(">q", int(val)))
        elif tag_id == 5:
            buf.write(struct.pack(">f", float(val)))
        elif tag_id == 6:
            buf.write(struct.pack(">d", float(val)))
        elif tag_id == 7:
            buf.write(struct.pack(">i", len(val)))
            if val:
                buf.write(struct.pack(f">{len(val)}b", *val))
        elif tag_id == 8:
            _write_str(buf, str(val))
        elif tag_id == 9:
            elem_tag = getattr(val, "_elem_tag", 0)
            if elem_tag == 0 and val:
                elem_tag = _tag_id_of(val[0])
            buf.write(struct.pack(">Bi", elem_tag, len(val)))
            for item in val:
                _write_payload(buf, elem_tag, item)
        elif tag_id == 10:
            for k, v in val.items():
                tid = _tag_id_of(v)
                buf.write(struct.pack(">B", tid))
                _write_str(buf, str(k))
                _write_payload(buf, tid, v)
            buf.write(b"\x00")
        elif tag_id == 11:
            buf.write(struct.pack(">i", len(val)))
            if val:
                buf.write(struct.pack(f">{len(val)}i", *val))
        elif tag_id == 12:
            buf.write(struct.pack(">i", len(val)))
            if val:
                buf.write(struct.pack(f">{len(val)}q", *val))
        else:
            raise ValueError(f"Unsupported NBT write tag_id={tag_id}")

    class _NBTReader:
        def __init__(self, data: bytes):
            self.buf = io.BytesIO(data)

        def _u8(self) -> int:
            return struct.unpack(">B", self.buf.read(1))[0]

        def _u16(self) -> int:
            return struct.unpack(">H", self.buf.read(2))[0]

        def _i32(self) -> int:
            return struct.unpack(">i", self.buf.read(4))[0]

        def _str(self) -> String:
            return String(self.buf.read(self._u16()).decode("utf-8", errors="replace"))

        def _payload(self, tid: int):
            if tid == 1:
                return Byte(struct.unpack(">b", self.buf.read(1))[0])
            if tid == 2:
                return Short(struct.unpack(">h", self.buf.read(2))[0])
            if tid == 3:
                return Int(self._i32())
            if tid == 4:
                return Long(struct.unpack(">q", self.buf.read(8))[0])
            if tid == 5:
                return Float(struct.unpack(">f", self.buf.read(4))[0])
            if tid == 6:
                return Double(struct.unpack(">d", self.buf.read(8))[0])
            if tid == 7:
                n = self._i32()
                return ByteArray(struct.unpack(f">{n}b", self.buf.read(n)))
            if tid == 8:
                return self._str()
            if tid == 9:
                et = self._u8()
                n = self._i32()
                lst = _NBTList(self._payload(et) for _ in range(n))
                lst._elem_tag = et
                return lst
            if tid == 10:
                res = Compound()
                while True:
                    t = self._u8()
                    if t == 0:
                        break
                    k = self._str()
                    res[k] = self._payload(t)
                return res
            if tid == 11:
                n = self._i32()
                return IntArray(struct.unpack(f">{n}i", self.buf.read(n * 4)))
            if tid == 12:
                n = self._i32()
                return LongArray(struct.unpack(f">{n}q", self.buf.read(n * 8)))
            raise ValueError(f"Unknown NBT tag type: {tid}")

        def read_root(self) -> Compound:
            rt = self._u8()
            if rt == 0:
                return Compound()
            _ = self._str()
            return self._payload(rt)

    class _File(Compound):
        def __init__(self, root: Compound):
            super().__init__(root)
            self.root = root

        def save(self, path: str, gzipped: bool = True):
            buf = io.BytesIO()
            buf.write(b"\x0a\x00\x00")
            _write_payload(buf, 10, self.root)
            raw = buf.getvalue()
            if gzipped:
                with gzip.open(path, "wb") as f:
                    f.write(raw)
            else:
                with open(path, "wb") as f:
                    f.write(raw)

    class _NbtLibShim:
        File = _File

        @staticmethod
        def load(path: str) -> Compound:
            with open(path, "rb") as f:
                raw = f.read()
            if raw[:2] == b"\x1f\x8b":
                raw = gzip.decompress(raw)
            return _NBTReader(raw).read_root()

    nbtlib = _NbtLibShim()


STRUCT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "data", "narutomod", "structure",
)

EXCLUDED_FILES = {"empty.nbt", "kumo_sky_bridge.nbt"}


def is_target_structure(filename: str) -> bool:
    if not filename.endswith(".nbt"):
        return False
    if filename in EXCLUDED_FILES or filename.startswith("valley_"):
        return False
    return True


def pad_structure_file(path: str, dry_run: bool = False) -> tuple[int, int, tuple[int, int, int]]:
    root = nbtlib.load(path)
    sx, sy, sz = (int(v) for v in root["size"])

    palette = root["palette"]
    air_idx = None
    for idx, entry in enumerate(palette):
        if str(entry.get("Name", "")) == "minecraft:air" and not entry.get("Properties"):
            air_idx = idx
            break

    palette_modified = False
    if air_idx is None:
        air_idx = len(palette)
        palette.append(Compound({"Name": String("minecraft:air")}))
        palette_modified = True

    blocks = root["blocks"]
    occupied = set()
    for block in blocks:
        px, py, pz = (int(v) for v in block["pos"])
        occupied.add((px, py, pz))

    added = 0
    for x in range(sx):
        for y in range(1, sy):
            for z in range(sz):
                if (x, y, z) not in occupied:
                    blocks.append(Compound({
                        "pos": List[Int]([Int(x), Int(y), Int(z)]),
                        "state": Int(air_idx),
                    }))
                    added += 1

    if not dry_run and (added > 0 or palette_modified):
        nbt_file = nbtlib.File(root)
        nbt_file.save(path, gzipped=True)

    return added, len(blocks), (sx, sy, sz)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Pad village/shrine/arena NBT structures with minecraft:air for y >= 1."
    )
    parser.add_argument(
        "--structure-dir",
        default=STRUCT_DIR,
        help="Directory containing .nbt structure files",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Inspect structures without modifying files",
    )
    args = parser.parse_args()

    if not os.path.isdir(args.structure_dir):
        print(f"Error: structure directory not found: {args.structure_dir}", file=sys.stderr)
        return 1

    filenames = sorted(f for f in os.listdir(args.structure_dir) if is_target_structure(f))
    total_added = 0
    for fn in filenames:
        path = os.path.join(args.structure_dir, fn)
        added, total_blocks, (sx, sy, sz) = pad_structure_file(path, dry_run=args.dry_run)
        total_added += added
        action = "would add" if args.dry_run else "added"
        print(f"{fn:30s} size=[{sx},{sy},{sz}] {action} {added:5d} air blocks (total={total_blocks})")

    print(f"Processed {len(filenames)} structure files; total air blocks {'to add' if args.dry_run else 'added'}: {total_added}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

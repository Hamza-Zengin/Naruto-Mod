#!/usr/bin/env python3
"""
Procedural 2x-scale NBT structure generator for the Valley of the End
(data/narutomod/structure/valley_*.nbt) using nbtlib.

Pieces generated:
  - valley_waterfall.nbt        (59x60x59) Multi-tier waterfall & plunge basin
  - valley_hashirama_statue.nbt (49x58x49) Light-stone Senju obelisk monument + wood/leaf motifs
  - valley_madara_statue.nbt    (49x58x49) Dark-stone Uchiha obelisk monument + red/white fan crown
  - valley_lower_river.nbt      (53x28x49) Broad canyon river gorge & layered cliffs
  - valley_canyon_walls.nbt     (49x46x41) Terraced canyon walls with moss, vines & stone arch
  - valley_shore.nbt            (49x22x37) Riverside pebble/moss shore with torii arch & lanterns
"""
import math
import os
import random
import nbtlib
from nbtlib.tag import Compound, Int, List, String

STRUCT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "data", "narutomod", "structure",
)
DATA_VERSION = 3465


class StructureBuilder:
    def __init__(self, sx: int, sy: int, sz: int):
        self.sx = sx
        self.sy = sy
        self.sz = sz
        self.blocks = {}  # (x, y, z) -> (name, props_tuple, nbt_compound_or_None)

    def in_bounds(self, x: int, y: int, z: int) -> bool:
        return 0 <= x < self.sx and 0 <= y < self.sy and 0 <= z < self.sz

    def set(self, x: int, y: int, z: int, name: str, props=None, nbt=None):
        if not self.in_bounds(x, y, z):
            return
        pt = tuple(sorted((props or {}).items()))
        self.blocks[(x, y, z)] = (name, pt, nbt)

    def clear(self, x: int, y: int, z: int):
        self.blocks.pop((x, y, z), None)

    def fill(self, x0: int, y0: int, z0: int, x1: int, y1: int, z1: int, name: str, props=None):
        xa, xb = min(x0, x1), max(x0, x1)
        ya, yb = min(y0, y1), max(y0, y1)
        za, zb = min(z0, z1), max(z0, z1)
        for x in range(xa, xb + 1):
            for y in range(ya, yb + 1):
                for z in range(za, zb + 1):
                    self.set(x, y, z, name, props)

    def fill_air(self, x0: int, y0: int, z0: int, x1: int, y1: int, z1: int):
        xa, xb = min(x0, x1), max(x0, x1)
        ya, yb = min(y0, y1), max(y0, y1)
        za, zb = min(z0, z1), max(z0, z1)
        for x in range(xa, xb + 1):
            for y in range(ya, yb + 1):
                for z in range(za, zb + 1):
                    self.clear(x, y, z)

    def add_jigsaw(self, x: int, y: int, z: int, orientation: str, name: str, target: str, pool: str,
                   final_state: str = "minecraft:air", joint: str = "rollable"):
        nbt = Compound({
            "id": String("minecraft:jigsaw"),
            "name": String(name),
            "target": String(target),
            "pool": String(pool),
            "final_state": String(final_state),
            "joint": String(joint),
        })
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, nbt)

    def add_arch_x(self, cx: int, y0: int, z0: int, z1: int, half_w: int, height: int,
                   pillar_mat: str, arch_mat: str, lantern_block: str = "minecraft:lantern"):
        """Builds a stone arch spanning X around cx from z0..z1."""
        for z in range(z0, z1 + 1):
            self.fill(cx - half_w, y0, z, cx - half_w, y0 + height - 2, z, pillar_mat)
            self.fill(cx + half_w, y0, z, cx + half_w, y0 + height - 2, z, pillar_mat)
            self.fill(cx - half_w, y0 + height, z, cx + half_w, y0 + height, z, arch_mat)
            for dx in (-half_w + 1, half_w - 1):
                self.set(cx + dx, y0 + height - 1, z, arch_mat)
        self.set(cx, y0 + height - 1, (z0 + z1) // 2, lantern_block, {"hanging": "true"})

    def add_arch_z(self, x0: int, x1: int, y0: int, cz: int, half_w: int, height: int,
                   pillar_mat: str, arch_mat: str, lantern_block: str = "minecraft:lantern"):
        """Builds a stone arch spanning Z around cz from x0..x1."""
        for x in range(x0, x1 + 1):
            self.fill(x, y0, cz - half_w, x, y0 + height - 2, cz - half_w, pillar_mat)
            self.fill(x, y0, cz + half_w, x, y0 + height - 2, cz + half_w, pillar_mat)
            self.fill(x, y0 + height, cz - half_w, x, y0 + height, cz + half_w, arch_mat)
            for dz in (-half_w + 1, half_w - 1):
                self.set(x, y0 + height - 1, cz + dz, arch_mat)
        self.set((x0 + x1) // 2, y0 + height - 1, cz, lantern_block, {"hanging": "true"})

    def add_lantern_post(self, x: int, y: int, z: int, wall_mat: str, lantern_mat: str = "minecraft:lantern"):
        self.set(x, y, z, wall_mat)
        self.set(x, y + 1, z, wall_mat)
        self.set(x, y + 2, z, lantern_mat, {"hanging": "false"})

    def apply_buried_foundation(self, depth: int = 4, dark_theme: bool = False):
        if depth <= 0 or not self.blocks:
            return
        shifted = {}
        footprint = set()
        for (x, y, z), val in self.blocks.items():
            shifted[(x, y + depth, z)] = val
            if y <= 1 and val[0] not in ("minecraft:air", "minecraft:vine"):
                footprint.add((x, z))
        self.sy += depth
        self.blocks = shifted

        rng = random.Random(707 + (1 if dark_theme else 0))
        for (x, z) in sorted(footprint):
            base_name = self.blocks.get((x, depth, z), ("minecraft:stone", (), None))[0]
            for fy in range(depth):
                if dark_theme:
                    mat = "minecraft:deepslate" if fy < depth - 1 else ("minecraft:cobbled_deepslate" if rng.random() < 0.45 else "minecraft:deepslate")
                elif base_name == "minecraft:cobblestone":
                    mat = "minecraft:stone" if fy < depth - 2 else ("minecraft:mossy_cobblestone" if rng.random() < 0.2 else "minecraft:cobblestone")
                else:
                    mat = "minecraft:deepslate" if fy == 0 and rng.random() < 0.35 else ("minecraft:cobblestone" if fy == depth - 1 and rng.random() < 0.3 else "minecraft:stone")
                self.set(x, fy, z, mat)

    def save(self, filename: str):
        self.apply_buried_foundation(depth=4, dark_theme=("madara" in filename))
        palette_list = []
        palette_map = {}

        def get_state(name, props_tuple):
            key = (name, props_tuple)
            if key not in palette_map:
                idx = len(palette_list)
                palette_map[key] = idx
                entry = Compound({"Name": String(name)})
                if props_tuple:
                    entry["Properties"] = Compound({k: String(v) for k, v in props_tuple})
                palette_list.append(entry)
            return palette_map[key]

        # Reserve index 0 for air
        get_state("minecraft:air", ())

        block_tags = []
        for (x, y, z), (name, props_tuple, nbt) in sorted(self.blocks.items()):
            state_idx = get_state(name, props_tuple)
            b = Compound({
                "pos": List[Int]([Int(x), Int(y), Int(z)]),
                "state": Int(state_idx),
            })
            if nbt is not None:
                b["nbt"] = nbt
            block_tags.append(b)

        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(self.sx), Int(self.sy), Int(self.sz)]),
            "palette": List[Compound](palette_list),
            "blocks": List[Compound](block_tags),
            "entities": List[Compound]([]),
        })
        nbt_file = nbtlib.File(root)
        out_path = os.path.join(STRUCT_DIR, filename)
        nbt_file.save(out_path, gzipped=True)
        print(f"Generated {filename}: size=[{self.sx},{self.sy},{self.sz}] blocks={len(block_tags)} palette={len(palette_list)}")


def stratified_rock(y: int, rng: random.Random) -> str:
    r = rng.random()
    if y < 12:
        if r < 0.35:
            return "minecraft:deepslate"
        if r < 0.65:
            return "minecraft:cobblestone"
        return "minecraft:stone"
    if y < 28:
        if r < 0.28:
            return "minecraft:andesite"
        if r < 0.45:
            return "minecraft:cobblestone"
        if r < 0.58:
            return "minecraft:mossy_stone_bricks"
        return "minecraft:stone"
    if r < 0.32:
        return "minecraft:andesite"
    if r < 0.48:
        return "minecraft:stone_bricks"
    return "minecraft:stone"


def smoothstep(edge0: float, edge1: float, x: float) -> float:
    if edge1 <= edge0:
        return 1.0 if x >= edge1 else 0.0
    t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3.0 - 2.0 * t)


def organic_noise(x: int, z: int) -> float:
    return 0.5 + 0.3 * math.sin(x * 0.29 + z * 0.17) + 0.2 * math.cos(x * 0.13 - z * 0.34)


def decorate_cliff_foliage(sb: StructureBuilder, x: int, top_y: int, z: int, rng: random.Random, is_outer: bool = False):
    if is_outer or top_y <= 3:
        sb.set(x, top_y, z, "minecraft:moss_block" if (x + z) % 3 == 0 else "minecraft:grass_block")
    else:
        sb.set(x, top_y, z, "minecraft:moss_block")

    leaf_prob = 0.36 if is_outer else 0.26
    if rng.random() < leaf_prob:
        h = rng.randint(1, 2)
        leaf_type = "minecraft:jungle_leaves" if rng.random() < 0.65 else "minecraft:oak_leaves"
        for dy in range(1, h + 1):
            sb.set(x, top_y + dy, z, leaf_type, {"persistent": "true"})

    if top_y >= 6 and rng.random() < 0.18:
        for dy in range(1, rng.randint(3, 7)):
            if (x, top_y - dy, z - 1) not in sb.blocks:
                sb.set(x, top_y - dy, z - 1, "minecraft:vine", {"south": "true"})


def build_valley_waterfall():
    sx, sy, sz = 59, 60, 59
    sb = StructureBuilder(sx, sy, sz)
    rng = random.Random(101)

    # Organic plunge basin & tapered canyon rim
    for x in range(sx):
        for z in range(sz):
            noise = organic_noise(x, z)
            # Connector zones stay intact for seamless jigsaw attachment
            west_conn = x <= 14 and 18 <= z <= 34
            east_conn = x >= 44 and 18 <= z <= 34
            north_conn = z <= 14 and 10 <= x <= 48

            # Organic outer mask: trim corners outside rounded valley silhouette
            dx_n = abs(x - 29) / 29.0
            dz_n = abs(z - 29) / 29.0
            radial = math.sqrt(dx_n * dx_n + dz_n * dz_n)
            if not (west_conn or east_conn or north_conn) and radial > 1.12 + (noise - 0.5) * 0.18:
                continue

            floor_mat = "minecraft:cobblestone" if (x + z) % 5 == 0 else ("minecraft:andesite" if (x * 3 + z) % 7 == 0 else "minecraft:stone")
            sb.set(x, 0, z, floor_mat)
            if (10 <= x <= 48 and 0 <= z <= 38) or (west_conn and 20 <= z <= 32) or (east_conn and 20 <= z <= 32):
                sb.fill(x, 1, z, x, 2, z, "minecraft:water")

    # Recessed underwater glow ring near splash impact
    for x in range(20, 39, 3):
        sb.set(x, 0, 34, "minecraft:sea_lantern")
        sb.set(x, 0, 26, "minecraft:sea_lantern")
    for z in range(28, 37, 4):
        sb.set(22, 0, z, "minecraft:sea_lantern")
        sb.set(36, 0, z, "minecraft:sea_lantern")

    # Flanking & rear canyon cliffs with smooth exterior slope (no sheer cutoff walls at x=0, x=58, z=58)
    for x in range(sx):
        for z in range(sz):
            if (x, 0, z) not in sb.blocks:
                continue
            in_west_wing = x < 11 and (z > 33 or z < 19)
            in_east_wing = x > 47 and (z > 33 or z < 19)
            in_south_cliff = z >= 39
            if not (in_south_cliff or in_west_wing or in_east_wing):
                continue

            noise = organic_noise(x, z)
            # Outer slope factor tapers height smoothly down to 1..3 near exterior boundary
            t_west = smoothstep(0.0, 9.0, x) if x < 12 else 1.0
            t_east = smoothstep(0.0, 9.0, sx - 1 - x) if x > sx - 13 else 1.0
            t_north = smoothstep(0.0, 8.0, z) if (z < 10 and (x < 11 or x > 47)) else 1.0
            t_south = smoothstep(0.0, 9.0, sz - 1 - z) if z > 48 else 1.0
            outer_taper = min(t_west, t_east, t_north, t_south)

            if in_south_cliff:
                depth = z - 39
                base_h = 18 + min(38, depth * 2 + int(4 * math.sin(x * 0.22)))
                if 21 <= x <= 37:
                    if z <= 42:
                        base_h = 4
                    elif z <= 46:
                        base_h = 18
                    elif z <= 50:
                        base_h = 34
                    elif z <= 53:
                        base_h = 48
                    else:
                        base_h = 56
                # Preserve upper reservoir channel while tapering outside ridge
                if not (22 <= x <= 36 and 46 <= z <= 55):
                    base_h = int(2 + (base_h - 2) * outer_taper + noise * 2.5)
                else:
                    # Rear lip behind reservoir still slopes down at z=56..58
                    rear_t = smoothstep(0.0, 4.0, sz - 1 - z) if z >= 55 else 1.0
                    base_h = int(4 + (base_h - 4) * rear_t)
            else:
                edge_dist = (10 - x) if x < 11 else (x - 48)
                raw_h = 8 + min(24, edge_dist * 3 + (z // 5))
                base_h = int(2 + (raw_h - 2) * outer_taper + noise * 2.0)

            top_y = min(sy - 2, max(1, base_h))
            for y in range(1, top_y + 1):
                sb.set(x, y, z, stratified_rock(y, rng))
            decorate_cliff_foliage(sb, x, top_y, z, rng, is_outer=(outer_taper < 0.55))

    # Multi-tier Waterfall Flow & Foam Ledges (x=23..35)
    sb.fill(23, 49, 52, 35, 52, 55, "minecraft:water")
    sb.fill(23, 35, 50, 35, 51, 51, "minecraft:water")
    sb.fill(22, 33, 47, 36, 34, 50, "minecraft:mossy_stone_bricks")
    sb.fill(23, 35, 47, 35, 35, 49, "minecraft:water")
    sb.fill(28, 34, 48, 30, 37, 50, "minecraft:moss_block")
    sb.fill(23, 19, 45, 35, 34, 46, "minecraft:water")
    sb.fill(22, 17, 42, 36, 18, 45, "minecraft:stone_bricks")
    sb.fill(23, 19, 42, 35, 19, 44, "minecraft:water")
    sb.fill(23, 2, 39, 35, 18, 41, "minecraft:water")
    for x in range(23, 36, 2):
        sb.set(x, 2, 38, "minecraft:white_stained_glass")
        sb.set(x, 3, 39, "minecraft:white_stained_glass")

    # Flanking riverside stone terraces & shrine lanterns
    for side_x in (11, 47):
        sb.fill(side_x - 2, 1, 32, side_x + 2, 4, 37, "minecraft:stone_bricks")
        sb.add_lantern_post(side_x, 5, 34, "minecraft:stone_brick_wall", "minecraft:soul_lantern")
    sb.add_lantern_post(11, 5, 16, "minecraft:stone_brick_wall", "minecraft:lantern")
    sb.add_lantern_post(47, 5, 16, "minecraft:stone_brick_wall", "minecraft:lantern")

    # 3 Outgoing Jigsaws (aligned at y=2)
    sb.add_jigsaw(0, 2, 26, "west_up", "narutomod:out", "narutomod:entry", "narutomod:valley_hashirama_pool")
    sb.add_jigsaw(29, 2, 0, "north_up", "narutomod:out", "narutomod:entry", "narutomod:valley_river_pool")
    sb.add_jigsaw(58, 2, 26, "east_up", "narutomod:out", "narutomod:entry", "narutomod:valley_madara_pool")

    sb.save("valley_waterfall.nbt")


def build_valley_hashirama_monument():
    """Light-stone Senju Obelisk with Senju crest, wood/leaf motifs, organic jungle slope."""
    sx, sy, sz = 49, 58, 49
    sb = StructureBuilder(sx, sy, sz)
    rng = random.Random(202)

    # Organic base footprint (keeps East waterfall connector & North path connector flush)
    for x in range(sx):
        for z in range(sz):
            noise = organic_noise(x, z)
            east_conn = x >= 34 and 14 <= z <= 36
            north_conn = z <= 14 and 18 <= x <= 32
            dx_n = max(0.0, (12 - x) / 12.0)
            dz_n = abs(z - 24) / 24.0
            if not (east_conn or north_conn) and (dx_n * dx_n + dz_n * dz_n > 1.15 + (noise - 0.5) * 0.2):
                continue

            sb.set(x, 0, z, "minecraft:stone")
            if x >= 38:
                sb.fill(x, 1, z, x, 2, z, "minecraft:water")
            elif x >= 34:
                sb.fill(x, 1, z, x, 2, z, "minecraft:stone_bricks")
            else:
                sb.fill(x, 1, z, x, 2, z, "minecraft:stone")
                if x >= 11 and z >= 10 and z <= 38:
                    pass
                else:
                    sb.set(x, 2, z, "minecraft:moss_block" if (x + z) % 3 == 0 else "minecraft:grass_block")

    # Quay walkway (x=34..37)
    sb.fill(34, 3, 4, 37, 3, 44, "minecraft:polished_andesite")
    for z in range(6, 44, 8):
        sb.add_lantern_post(37, 4, z, "minecraft:stone_brick_wall", "minecraft:lantern")

    # Ceremonial Staircase
    for step in range(7):
        sx_pos = 34 - step
        y_pos = 3 + step
        sb.fill(sx_pos, 3, 19, sx_pos, y_pos, 29, "minecraft:stone_bricks")
        for z in range(19, 30):
            sb.set(sx_pos, y_pos, z, "minecraft:stone_brick_stairs", {"facing": "west", "half": "bottom", "shape": "straight"})
        sb.fill(sx_pos, y_pos + 1, 18, sx_pos, y_pos + 1, 18, "minecraft:polished_andesite")
        sb.fill(sx_pos, y_pos + 1, 30, sx_pos, y_pos + 1, 30, "minecraft:polished_andesite")

    # Grand Stepped Pedestal / Plinth
    sb.fill(11, 3, 15, 29, 7, 33, "minecraft:stone_bricks")
    sb.fill(12, 8, 16, 28, 10, 32, "minecraft:polished_andesite")
    sb.fill(13, 11, 17, 27, 11, 31, "minecraft:chiseled_stone_bricks")
    for lx in (12, 28):
        for lz in (16, 32):
            sb.add_lantern_post(lx, 12, lz, "minecraft:stone_brick_wall", "minecraft:soul_lantern")

    # Ceremonial Archway & leaf garlands
    sb.add_arch_z(31, 33, 4, 24, 6, 8, "minecraft:oak_log", "minecraft:polished_andesite", "minecraft:lantern")
    for z in range(18, 31):
        sb.set(32, 13, z, "minecraft:oak_leaves", {"persistent": "true"})
        if z % 3 == 0:
            sb.set(31, 12, z, "minecraft:jungle_leaves", {"persistent": "true"})

    # Light-Stone Senju Obelisk Shaft
    tiers = [(12, 20, 5), (21, 30, 4), (31, 40, 3), (41, 48, 2), (49, 53, 1)]
    cx, cz = 20, 24
    for y0, y1, r in tiers:
        for y in range(y0, y1 + 1):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    is_corner = abs(dx) == r and abs(dz) == r
                    if is_corner and r >= 3:
                        mat = "minecraft:oak_log"
                    elif y == y0 or y == y1:
                        mat = "minecraft:polished_andesite"
                    elif (dx + dz + y) % 4 == 0:
                        mat = "minecraft:calcite"
                    else:
                        mat = "minecraft:smooth_quartz"
                    sb.set(cx + dx, y, cz + dz, mat)
        if r >= 2:
            for dx in range(-r - 1, r + 2):
                for dz in (-r - 1, r + 1):
                    if rng.random() < 0.72:
                        sb.set(cx + dx, y0, cz + dz, "minecraft:jungle_leaves" if (dx + dz) % 2 == 0 else "minecraft:moss_block",
                               {"persistent": "true"} if (dx + dz) % 2 == 0 else None)
    sb.set(cx, 54, cz, "minecraft:gold_block")
    sb.set(cx, 55, cz, "minecraft:soul_lantern", {"hanging": "false"})

    # Inlaid Senju Clan Emblem
    crest_offsets = [
        (0, 0, "minecraft:gold_block"), (1, 0, "minecraft:gold_block"), (2, 0, "minecraft:gold_block"),
        (3, 0, "minecraft:copper_block"), (4, 0, "minecraft:copper_block"), (5, 0, "minecraft:gold_block"),
        (6, 0, "minecraft:gold_block"), (7, 0, "minecraft:gold_block"), (8, 0, "minecraft:gold_block"),
        (2, -1, "minecraft:copper_block"), (2, 1, "minecraft:copper_block"),
        (3, -2, "minecraft:gold_block"), (3, 2, "minecraft:gold_block"),
        (4, -2, "minecraft:gold_block"), (4, 2, "minecraft:gold_block"),
        (5, -2, "minecraft:copper_block"), (5, 2, "minecraft:copper_block"),
        (6, -1, "minecraft:copper_block"), (6, 1, "minecraft:copper_block"),
        (7, -2, "minecraft:dark_oak_planks"), (7, 2, "minecraft:dark_oak_planks"),
        (8, -2, "minecraft:gold_block"), (8, 2, "minecraft:gold_block"),
    ]
    for dy, off, mat in crest_offsets:
        y = 22 + dy
        face_r = 4 if y <= 30 else 3
        if abs(off) <= face_r:
            sb.set(cx + face_r, y, cz + off, mat)
            sb.set(cx + off, y, cz - face_r, mat)

    # Organic backing ridge along x=0..10 (peaks inside around x=7..9, slopes smoothly to x=0 & north/south edges)
    for x in range(0, 11):
        for z in range(sz):
            if (x, 0, z) not in sb.blocks:
                continue
            noise = organic_noise(x, z)
            t_west = smoothstep(0.0, 6.0, x)
            t_z = min(smoothstep(0.0, 9.0, z), smoothstep(0.0, 9.0, sz - 1 - z))
            taper = min(t_west, t_z)
            peak_h = 28 - abs(x - 8) * 2 + int(3 * math.cos(z * 0.25))
            h = int(2 + (peak_h - 2) * taper + noise * 2.0)
            top_y = min(sy - 3, max(2, h))
            for y in range(1, top_y + 1):
                sb.set(x, y, z, stratified_rock(y, rng))
            decorate_cliff_foliage(sb, x, top_y, z, rng, is_outer=(taper < 0.55))

    # North approach path
    sb.fill(21, 3, 0, 27, 3, 14, "minecraft:mossy_stone_bricks")

    # Jigsaws
    sb.add_jigsaw(48, 2, 24, "east_up", "narutomod:entry", "narutomod:entry", "minecraft:empty")
    sb.add_jigsaw(24, 2, 0, "north_up", "narutomod:out", "narutomod:entry", "narutomod:valley_of_the_end_addons")

    sb.save("valley_hashirama_statue.nbt")


def build_valley_madara_monument():
    """Dark-stone Uchiha Obelisk crowned with Red/White Uchiha Fan, organic jungle slope."""
    sx, sy, sz = 49, 58, 49
    sb = StructureBuilder(sx, sy, sz)
    rng = random.Random(303)

    for x in range(sx):
        for z in range(sz):
            noise = organic_noise(x, z)
            west_conn = x <= 14 and 14 <= z <= 36
            north_conn = z <= 14 and 18 <= x <= 32
            dx_n = max(0.0, (x - 36) / 12.0)
            dz_n = abs(z - 24) / 24.0
            if not (west_conn or north_conn) and (dx_n * dx_n + dz_n * dz_n > 1.15 + (noise - 0.5) * 0.2):
                continue

            sb.set(x, 0, z, "minecraft:deepslate")
            if x <= 10:
                sb.fill(x, 1, z, x, 2, z, "minecraft:water")
            elif x <= 14:
                sb.fill(x, 1, z, x, 2, z, "minecraft:polished_blackstone_bricks")
            else:
                sb.fill(x, 1, z, x, 2, z, "minecraft:deepslate")
                if not (x <= 37 and 10 <= z <= 38):
                    sb.set(x, 2, z, "minecraft:moss_block" if (x + z) % 3 == 0 else "minecraft:grass_block")

    sb.fill(11, 3, 4, 14, 3, 44, "minecraft:polished_blackstone")
    for z in range(6, 44, 8):
        sb.add_lantern_post(11, 4, z, "minecraft:polished_blackstone_wall", "minecraft:soul_lantern")

    for step in range(7):
        sx_pos = 14 + step
        y_pos = 3 + step
        sb.fill(sx_pos, 3, 19, sx_pos, y_pos, 29, "minecraft:polished_blackstone_bricks")
        for z in range(19, 30):
            sb.set(sx_pos, y_pos, z, "minecraft:polished_blackstone_brick_stairs",
                   {"facing": "east", "half": "bottom", "shape": "straight"})
        sb.set(sx_pos, y_pos + 1, 18, "minecraft:polished_blackstone")
        sb.set(sx_pos, y_pos + 1, 30, "minecraft:polished_blackstone")

    sb.fill(19, 3, 15, 37, 7, 33, "minecraft:deepslate_bricks")
    sb.fill(20, 8, 16, 36, 10, 32, "minecraft:polished_blackstone_bricks")
    sb.fill(21, 11, 17, 35, 11, 31, "minecraft:deepslate_tiles")
    for lx in (20, 36):
        for lz in (16, 32):
            sb.add_lantern_post(lx, 12, lz, "minecraft:polished_blackstone_wall", "minecraft:soul_lantern")

    sb.add_arch_z(15, 17, 4, 24, 6, 8, "minecraft:polished_blackstone_bricks", "minecraft:deepslate_tiles", "minecraft:soul_lantern")

    cx, cz = 28, 24
    tiers = [(12, 20, 5), (21, 29, 4), (30, 36, 3), (37, 40, 2)]
    for y0, y1, r in tiers:
        for y in range(y0, y1 + 1):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    is_corner = abs(dx) == r and abs(dz) == r
                    if is_corner:
                        mat = "minecraft:crying_obsidian" if y % 4 == 0 else "minecraft:polished_blackstone"
                    elif y == y0 or y == y1:
                        mat = "minecraft:deepslate_tiles"
                    elif (dx + dz + y) % 5 == 0:
                        mat = "minecraft:cracked_polished_blackstone_bricks"
                    else:
                        mat = "minecraft:polished_blackstone_bricks"
                    sb.set(cx + dx, y, cz + dz, mat)

    for dy in range(-3, 4):
        for dz in range(-3, 4):
            dist = dy * dy + dz * dz
            if dist <= 10:
                mat = "minecraft:red_terracotta" if dy >= 0 else "minecraft:white_concrete"
                if dist >= 7:
                    mat = "minecraft:blackstone"
                sb.set(cx - 4, 25 + dy, cz + dz, mat)

    sb.fill(cx - 1, 41, cz - 1, cx + 1, 44, cz + 1, "minecraft:white_concrete")
    sb.fill(cx - 1, 40, cz - 1, cx + 1, 40, cz + 1, "minecraft:gold_block")
    fan_cy, fan_r = 50, 5.6
    for dy in range(-6, 7):
        for dz in range(-6, 7):
            dist = math.sqrt(dy * dy + dz * dz)
            if dist <= fan_r:
                if dist >= fan_r - 1.15:
                    mat = "minecraft:obsidian"
                elif dy >= 0:
                    mat = "minecraft:red_concrete"
                else:
                    mat = "minecraft:white_concrete"
                sb.fill(cx - 1, fan_cy + dy, cz + dz, cx + 1, fan_cy + dy, cz + dz, mat)
    sb.set(cx, 56, cz, "minecraft:soul_lantern", {"hanging": "false"})

    # Organic backing ridge along x=38..48
    for x in range(38, sx):
        for z in range(sz):
            if (x, 0, z) not in sb.blocks:
                continue
            noise = organic_noise(x, z)
            t_east = smoothstep(0.0, 6.0, sx - 1 - x)
            t_z = min(smoothstep(0.0, 9.0, z), smoothstep(0.0, 9.0, sz - 1 - z))
            taper = min(t_east, t_z)
            peak_h = 28 - abs(x - 40) * 2 + int(3 * math.sin(z * 0.25))
            h = int(2 + (peak_h - 2) * taper + noise * 2.0)
            top_y = min(sy - 3, max(2, h))
            for y in range(1, top_y + 1):
                sb.set(x, y, z, "minecraft:deepslate" if y < 18 else stratified_rock(y, rng))
            decorate_cliff_foliage(sb, x, top_y, z, rng, is_outer=(taper < 0.55))

    sb.fill(21, 3, 0, 27, 3, 14, "minecraft:deepslate_tiles")

    sb.add_jigsaw(0, 2, 24, "west_up", "narutomod:entry", "narutomod:entry", "minecraft:empty")
    sb.add_jigsaw(24, 2, 0, "north_up", "narutomod:out", "narutomod:entry", "narutomod:valley_of_the_end_addons")

    sb.save("valley_madara_statue.nbt")


def build_valley_lower_river():
    sx, sy, sz = 53, 28, 45
    sb = StructureBuilder(sx, sy, sz)
    rng = random.Random(404)

    for x in range(sx):
        for z in range(sz):
            noise = organic_noise(x, z)
            meander = int(2.5 * math.sin(z * 0.14))
            river_half = 11
            dist_from_center = abs(x - (26 + meander))

            # Feather extreme outer x margins organically
            outer_taper = min(smoothstep(0.0, 8.0, x), smoothstep(0.0, 8.0, sx - 1 - x))
            if dist_from_center > 22 + (noise - 0.5) * 4.0 and (z < 6 or z > sz - 7):
                continue

            sb.set(x, 0, z, "minecraft:cobblestone" if (x + z) % 3 == 0 else "minecraft:stone")
            if dist_from_center <= river_half:
                sb.fill(x, 1, z, x, 2, z, "minecraft:water")
            elif dist_from_center <= river_half + 3:
                sb.set(x, 1, z, "minecraft:stone_bricks")
                sb.set(x, 2, z, "minecraft:moss_block" if rng.random() < 0.5 else "minecraft:grass_block")
            else:
                cliff_d = dist_from_center - (river_half + 3)
                raw_h = 4 + min(20, cliff_d * 2 + int(2 * math.cos(z * 0.3)))
                h = int(2 + (raw_h - 2) * outer_taper + noise * 1.5)
                top_y = min(sy - 2, max(2, h))
                for y in range(1, top_y + 1):
                    sb.set(x, y, z, stratified_rock(y, rng))
                decorate_cliff_foliage(sb, x, top_y, z, rng, is_outer=(outer_taper < 0.55))

    sb.fill(11, 1, 18, 15, 3, 26, "minecraft:stone_bricks")
    sb.add_lantern_post(14, 4, 19, "minecraft:stone_brick_wall", "minecraft:lantern")
    sb.add_lantern_post(14, 4, 25, "minecraft:stone_brick_wall", "minecraft:lantern")
    sb.add_lantern_post(39, 4, 14, "minecraft:stone_brick_wall", "minecraft:soul_lantern")
    sb.add_lantern_post(39, 4, 30, "minecraft:stone_brick_wall", "minecraft:soul_lantern")

    sb.add_jigsaw(26, 2, 44, "south_up", "narutomod:entry", "narutomod:entry", "minecraft:empty")
    sb.add_jigsaw(26, 2, 0, "north_up", "narutomod:out", "narutomod:entry", "narutomod:valley_of_the_end_addons")

    sb.save("valley_lower_river.nbt")


def build_valley_canyon_walls():
    sx, sy, sz = 49, 46, 41
    sb = StructureBuilder(sx, sy, sz)
    rng = random.Random(505)

    for x in range(sx):
        for z in range(sz):
            noise = organic_noise(x, z)
            south_conn = z >= 31 and 18 <= x <= 30
            dx_n = abs(x - 24) / 24.0
            dz_n = max(0.0, (24 - z) / 24.0)
            if not south_conn and (dx_n * dx_n + dz_n * dz_n > 1.12 + (noise - 0.5) * 0.2):
                continue

            sb.set(x, 0, z, "minecraft:stone")
            t_x = min(smoothstep(0.0, 9.0, x), smoothstep(0.0, 9.0, sx - 1 - x))
            t_z = smoothstep(0.0, 9.0, z) if z < 26 else 1.0
            outer_taper = min(t_x, t_z)

            if z > 32 and 18 <= x <= 30:
                top_y = 2
                sb.fill(x, 1, z, x, 2, z, "minecraft:stone_bricks")
            else:
                dist_edge = max(0, 14 - z) + max(0, abs(x - 24) - 10)
                raw_h = 5 + min(34, dist_edge * 2 + int(3 * math.sin(x * 0.25)))
                h = int(2 + (raw_h - 2) * outer_taper + noise * 2.0)
                top_y = min(sy - 3, max(2, h))
                for y in range(1, top_y + 1):
                    sb.set(x, y, z, stratified_rock(y, rng))
                decorate_cliff_foliage(sb, x, top_y, z, rng, is_outer=(outer_taper < 0.55))

    sb.fill(19, 1, 25, 29, 3, 33, "minecraft:stone_bricks")
    sb.add_arch_x(24, 4, 27, 29, 4, 6, "minecraft:mossy_stone_bricks", "minecraft:stone_bricks", "minecraft:lantern")
    sb.add_lantern_post(20, 4, 32, "minecraft:stone_brick_wall", "minecraft:lantern")
    sb.add_lantern_post(28, 4, 32, "minecraft:stone_brick_wall", "minecraft:lantern")

    sb.add_jigsaw(24, 2, 40, "south_up", "narutomod:entry", "narutomod:entry", "minecraft:empty")
    sb.save("valley_canyon_walls.nbt")


def build_valley_shore():
    sx, sy, sz = 49, 22, 37
    sb = StructureBuilder(sx, sy, sz)
    rng = random.Random(606)

    for x in range(sx):
        for z in range(sz):
            noise = organic_noise(x, z)
            south_conn = z >= 26 and 18 <= x <= 30
            dx_n = abs(x - 24) / 24.0
            dz_n = max(0.0, (24 - z) / 24.0)
            if not south_conn and (dx_n * dx_n + dz_n * dz_n > 1.08 + (noise - 0.5) * 0.22):
                continue

            sb.set(x, 0, z, "minecraft:cobblestone")
            if z >= 26:
                if 19 <= x <= 29:
                    sb.fill(x, 1, z, x, 2, z, "minecraft:stone_bricks")
                else:
                    sb.set(x, 1, z, "minecraft:cobblestone")
                    sb.set(x, 2, z, "minecraft:moss_block" if (x + z) % 2 == 0 else "minecraft:grass_block")
            else:
                t_edge = min(smoothstep(0.0, 8.0, x), smoothstep(0.0, 8.0, sx - 1 - x), smoothstep(0.0, 8.0, z))
                slope = max(0, (25 - z) // 5)
                raw_h = 2 + min(6, slope + (1 if rng.random() < 0.25 else 0))
                h = int(1 + (raw_h - 1) * t_edge + noise * 1.2)
                for y in range(1, max(2, h)):
                    sb.set(x, y, z, "minecraft:stone" if y < h - 1 else "minecraft:cobblestone")
                sb.set(x, max(1, h), z, "minecraft:moss_block" if (x + z) % 3 == 0 else "minecraft:grass_block")

    sb.fill(18, 1, 20, 30, 3, 32, "minecraft:stone_bricks")
    sb.add_arch_x(24, 4, 22, 24, 4, 7, "minecraft:chiseled_stone_bricks", "minecraft:stone_bricks", "minecraft:lantern")
    for tx, tz in ((12, 14), (36, 15)):
        sb.fill(tx, 3, tz, tx, 8, tz, "minecraft:oak_log")
        sb.fill(tx - 2, 7, tz - 2, tx + 2, 10, tz + 2, "minecraft:jungle_leaves", {"persistent": "true"})

    sb.add_lantern_post(19, 4, 28, "minecraft:stone_brick_wall", "minecraft:lantern")
    sb.add_lantern_post(29, 4, 28, "minecraft:stone_brick_wall", "minecraft:lantern")
    sb.add_jigsaw(24, 2, 36, "south_up", "narutomod:entry", "narutomod:entry", "minecraft:empty")

    sb.save("valley_shore.nbt")


def main():
    build_valley_waterfall()
    build_valley_hashirama_monument()
    build_valley_madara_monument()
    build_valley_lower_river()
    build_valley_canyon_walls()
    build_valley_shore()


if __name__ == "__main__":
    main()

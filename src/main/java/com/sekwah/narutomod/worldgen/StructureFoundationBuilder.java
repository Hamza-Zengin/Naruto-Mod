package com.sekwah.narutomod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Builds proportional 2x2 corner-first symmetric support pillars, fills foundation columns,
 * seals water-edge floor lips, and selects theme-matched foundation blocks for
 * {@link AnchoredJigsawStructure}.
 */
final class StructureFoundationBuilder {

    static final int PILLAR_SIZE = 2;
    static final int TIER2_MIN_EMBED_DEPTH = 3;
    static final int TIER2_MAX_EMBED_DEPTH = 4;
    static final int MIN_COLUMN_CLEAR_GAP = 5;

    private StructureFoundationBuilder() {
    }

    static void applyPieceFoundations(WorldGenLevel level,
                                      BlockPos.MutableBlockPos cursor,
                                      BoundingBox pieceBox,
                                      int x0,
                                      int x1,
                                      int z0,
                                      int z1,
                                      int worldMinY,
                                      int maxFoundationDepth,
                                      int[][] baseYs,
                                      int[][] gapDepths,
                                      BlockState[][] baseStates) {
        boolean[][] pillarMask = planPiecePillars(pieceBox, x0, x1, z0, z1, baseYs, gapDepths);

        for (int x = x0; x <= x1; x++) {
            int xi = x - x0;
            for (int z = z0; z <= z1; z++) {
                int zi = z - z0;
                int baseY = baseYs[xi][zi];
                BlockState baseState = baseStates[xi][zi];
                if (baseY < 0 || baseState == null) {
                    continue;
                }

                int gap = gapDepths[xi][zi];
                if (gap <= 0) {
                    continue;
                }

                if (pillarMask[xi][zi]) {
                    int embedDepth = computeTier2EmbedDepth(x, z);
                    int stopY = Math.max(worldMinY, baseY - Math.min(maxFoundationDepth, gap + embedDepth));
                    fillFoundationColumn(level, cursor, baseState, x, z, baseY, stopY);
                } else if (baseY - 1 >= worldMinY) {
                    // Water-edge lip seal: seal 1 block below baseY so water does not bleed under walls/floors
                    BlockState immediatelyBelow = level.getBlockState(cursor.set(x, baseY - 1, z));
                    if (!immediatelyBelow.getFluidState().isEmpty()) {
                        int sealY = baseY - 1;
                        BlockState fillState = pickFoundationBlock(baseState, x, sealY, z, 0);
                        level.setBlock(cursor.set(x, sealY, z), fillState, 2);
                    }
                }
            }
        }
    }

    static boolean[][] planPiecePillars(BoundingBox pieceBox,
                                        int x0,
                                        int x1,
                                        int z0,
                                        int z1,
                                        int[][] baseYs,
                                        int[][] gapDepths) {
        int width = (x1 - x0) + 1;
        int depthSpan = (z1 - z0) + 1;
        boolean[][] pillarMask = new boolean[width][depthSpan];

        int[] xStarts = computeAxisPillarStarts(pieceBox.minX(), pieceBox.maxX(), PILLAR_SIZE, MIN_COLUMN_CLEAR_GAP);
        int[] zStarts = computeAxisPillarStarts(pieceBox.minZ(), pieceBox.maxZ(), PILLAR_SIZE, MIN_COLUMN_CLEAR_GAP);
        if (xStarts.length == 0 || zStarts.length == 0) {
            return pillarMask;
        }

        // Priority 1: Canonical pieceBox corner 2x2 pillars
        int[] cornerXIndices = xStarts.length == 1 ? new int[]{0} : new int[]{0, xStarts.length - 1};
        int[] cornerZIndices = zStarts.length == 1 ? new int[]{0} : new int[]{0, zStarts.length - 1};
        for (int xiIdx : cornerXIndices) {
            int px0 = xStarts[xiIdx];
            int px1 = Math.min(pieceBox.maxX(), px0 + PILLAR_SIZE - 1);
            for (int ziIdx : cornerZIndices) {
                int pz0 = zStarts[ziIdx];
                int pz1 = Math.min(pieceBox.maxZ(), pz0 + PILLAR_SIZE - 1);
                markPillarFootprint(pillarMask, baseYs, gapDepths, x0, x1, z0, z1, px0, px1, pz0, pz1);
            }
        }

        // Priority 2: Convex footprint corners on L-shaped / curved overhanging pieces
        for (int x = x0; x <= x1; x++) {
            int xi = x - x0;
            for (int z = z0; z <= z1; z++) {
                int zi = z - z0;
                if (baseYs[xi][zi] < 0 || gapDepths[xi][zi] <= 0) {
                    continue;
                }
                boolean noNegX = (x == pieceBox.minX()) || (xi > 0 && baseYs[xi - 1][zi] < 0);
                boolean noPosX = (x == pieceBox.maxX()) || (xi < width - 1 && baseYs[xi + 1][zi] < 0);
                boolean noNegZ = (z == pieceBox.minZ()) || (zi > 0 && baseYs[xi][zi - 1] < 0);
                boolean noPosZ = (z == pieceBox.maxZ()) || (zi < depthSpan - 1 && baseYs[xi][zi + 1] < 0);
                if (!isConvexFootprintCorner(true, noNegX, noPosX, noNegZ, noPosZ)) {
                    continue;
                }

                int cx0 = (noPosX && !noNegX) ? Math.max(pieceBox.minX(), x - (PILLAR_SIZE - 1)) : x;
                int cx1 = (noNegX && noPosX) ? x : Math.min(pieceBox.maxX(), cx0 + (PILLAR_SIZE - 1));
                int cz0 = (noPosZ && !noNegZ) ? Math.max(pieceBox.minZ(), z - (PILLAR_SIZE - 1)) : z;
                int cz1 = (noNegZ && noPosZ) ? z : Math.min(pieceBox.maxZ(), cz0 + (PILLAR_SIZE - 1));
                // Allow convex wing corners as long as >= 4 clear blocks separate them from existing pillars
                if (!hasNearbyOtherPillar(pillarMask, x0, z0, cx0, cx1, cz0, cz1, 4)) {
                    markPillarFootprint(pillarMask, baseYs, gapDepths, x0, x1, z0, z1, cx0, cx1, cz0, cz1);
                }
            }
        }

        // Priority 3: Remaining symmetric edge & interior 2x2 grid pillars
        for (int xs : xStarts) {
            int gx0 = xs;
            int gx1 = Math.min(pieceBox.maxX(), xs + PILLAR_SIZE - 1);
            for (int zs : zStarts) {
                int gz0 = zs;
                int gz1 = Math.min(pieceBox.maxZ(), zs + PILLAR_SIZE - 1);
                if (!hasNearbyOtherPillar(pillarMask, x0, z0, gx0, gx1, gz0, gz1, MIN_COLUMN_CLEAR_GAP)) {
                    markPillarFootprint(pillarMask, baseYs, gapDepths, x0, x1, z0, z1, gx0, gx1, gz0, gz1);
                }
            }
        }

        return pillarMask;
    }

    static void markPillarFootprint(boolean[][] pillarMask,
                                    int[][] baseYs,
                                    int[][] gapDepths,
                                    int x0,
                                    int x1,
                                    int z0,
                                    int z1,
                                    int px0,
                                    int px1,
                                    int pz0,
                                    int pz1) {
        int ix0 = Math.max(x0, px0);
        int ix1 = Math.min(x1, px1);
        int iz0 = Math.max(z0, pz0);
        int iz1 = Math.min(z1, pz1);
        if (ix0 > ix1 || iz0 > iz1) {
            return;
        }
        boolean hasOverhangColumn = false;
        for (int x = ix0; x <= ix1 && !hasOverhangColumn; x++) {
            int xi = x - x0;
            for (int z = iz0; z <= iz1; z++) {
                int zi = z - z0;
                if (baseYs[xi][zi] >= 0 && gapDepths[xi][zi] > 0) {
                    hasOverhangColumn = true;
                    break;
                }
            }
        }
        if (!hasOverhangColumn) {
            return;
        }
        for (int x = ix0; x <= ix1; x++) {
            int xi = x - x0;
            for (int z = iz0; z <= iz1; z++) {
                int zi = z - z0;
                if (baseYs[xi][zi] >= 0 && gapDepths[xi][zi] > 0) {
                    pillarMask[xi][zi] = true;
                }
            }
        }
    }

    static boolean hasNearbyOtherPillar(boolean[][] pillarMask,
                                        int x0,
                                        int z0,
                                        int px0,
                                        int px1,
                                        int pz0,
                                        int pz1,
                                        int minClearGap) {
        int width = pillarMask.length;
        int depthSpan = pillarMask[0].length;
        int checkMinXi = Math.max(0, (px0 - minClearGap) - x0);
        int checkMaxXi = Math.min(width - 1, (px1 + minClearGap) - x0);
        int checkMinZi = Math.max(0, (pz0 - minClearGap) - z0);
        int checkMaxZi = Math.min(depthSpan - 1, (pz1 + minClearGap) - z0);
        for (int xi = checkMinXi; xi <= checkMaxXi; xi++) {
            int wx = x0 + xi;
            for (int zi = checkMinZi; zi <= checkMaxZi; zi++) {
                int wz = z0 + zi;
                boolean insideSelf = wx >= px0 && wx <= px1 && wz >= pz0 && wz <= pz1;
                if (!insideSelf && pillarMask[xi][zi]) {
                    return true;
                }
            }
        }
        return false;
    }

    static void fillFoundationColumn(WorldGenLevel level,
                                     BlockPos.MutableBlockPos cursor,
                                     BlockState baseState,
                                     int x,
                                     int z,
                                     int baseY,
                                     int stopY) {
        boolean reachedNaturalSolid = false;
        for (int y = baseY - 1; y >= stopY; y--) {
            BlockState existing = level.getBlockState(cursor.set(x, y, z));
            if (isFoundationReplaceable(existing)) {
                if (reachedNaturalSolid) {
                    // Do not punch across caves below an already-hit solid floor
                    break;
                }
                int depth = (baseY - 1) - y;
                BlockState fillState = pickFoundationBlock(baseState, x, y, z, depth);
                level.setBlock(cursor, fillState, 2);
            } else {
                reachedNaturalSolid = true;
                // Embed 3-4 blocks into natural solid ground
                int depth = (baseY - 1) - y;
                BlockState fillState = pickFoundationBlock(baseState, x, y, z, depth);
                level.setBlock(cursor, fillState, 2);
            }
        }
    }

    static int selectPillarSize(int gapDepth, int pieceWidth) {
        return PILLAR_SIZE;
    }

    static int computePillarStride(int pillarSize) {
        int clampedSize = Math.max(PILLAR_SIZE, pillarSize);
        // Ensures clear gap between adjacent pillars is always >= 6 blocks (>= MIN_COLUMN_CLEAR_GAP = 5)
        return clampedSize + 6;
    }

    static int[] computeAxisPillarStarts(int min, int max, int pillarSize, int minClearGap) {
        if (pillarSize <= 0 || min > max) {
            return new int[0];
        }
        int span = (max - min) + 1;
        if (span <= pillarSize) {
            return new int[]{min};
        }
        int firstStart = min;
        int lastStart = max - pillarSize + 1;
        int interiorSpan = lastStart - (firstStart + pillarSize);
        if (interiorSpan < minClearGap) {
            if (lastStart > firstStart + pillarSize) {
                return new int[]{firstStart, lastStart};
            }
            return new int[]{firstStart};
        }
        int safeGap = Math.max(1, minClearGap);
        int interiorCount = 0;
        if (interiorSpan >= safeGap + pillarSize + safeGap) {
            interiorCount = (interiorSpan - safeGap) / (pillarSize + safeGap);
        }
        int[] starts = new int[interiorCount + 2];
        starts[0] = firstStart;
        if (interiorCount > 0) {
            int totalGap = interiorSpan - interiorCount * pillarSize;
            int gaps = interiorCount + 1;
            int baseGap = totalGap / gaps;
            int remainder = totalGap % gaps;
            int pos = firstStart + pillarSize;
            for (int i = 0; i < interiorCount; i++) {
                int gapBefore = baseGap + (i < remainder ? 1 : 0);
                pos += gapBefore;
                starts[i + 1] = pos;
                pos += pillarSize;
            }
        }
        starts[starts.length - 1] = lastStart;
        return starts;
    }

    static boolean isAxisPillarCoord(int coord, int[] starts, int pillarSize) {
        if (pillarSize <= 0 || starts == null) {
            return false;
        }
        for (int start : starts) {
            if (coord >= start && coord < start + pillarSize) {
                return true;
            }
        }
        return false;
    }

    static boolean isOnPiecePillarGrid(int x, int z, int[] xStarts, int[] zStarts, int pillarSize) {
        return isAxisPillarCoord(x, xStarts, pillarSize) && isAxisPillarCoord(z, zStarts, pillarSize);
    }

    static boolean isOnGlobalPillarGrid(int x, int z, int pillarSize, int stride) {
        if (stride <= 0 || pillarSize <= 0) {
            return false;
        }
        return Math.floorMod(x, stride) < pillarSize && Math.floorMod(z, stride) < pillarSize;
    }

    static boolean isCornerPillarPoint(int x, int z, int minX, int maxX, int minZ, int maxZ, int pillarSize) {
        if (pillarSize <= 0 || minX > maxX || minZ > maxZ) {
            return false;
        }
        boolean nearMinX = x >= minX && x < minX + pillarSize;
        boolean nearMaxX = x <= maxX && x > maxX - pillarSize;
        boolean nearMinZ = z >= minZ && z < minZ + pillarSize;
        boolean nearMaxZ = z <= maxZ && z > maxZ - pillarSize;
        return (nearMinX || nearMaxX) && (nearMinZ || nearMaxZ);
    }

    static boolean isConvexFootprintCorner(boolean hasBase,
                                           boolean noNegX,
                                           boolean noPosX,
                                           boolean noNegZ,
                                           boolean noPosZ) {
        return hasBase && (noNegX || noPosX) && (noNegZ || noPosZ);
    }

    static int computeTier2EmbedDepth(int x, int z) {
        return ((x + z) & 1) == 0 ? TIER2_MIN_EMBED_DEPTH : TIER2_MAX_EMBED_DEPTH;
    }

    static boolean isStructuralBase(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)) {
            return false;
        }
        return state.isSolidRender() || !state.canBeReplaced();
    }

    static boolean isFoundationReplaceable(BlockState state) {
        return state.isAir()
                || !state.getFluidState().isEmpty()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES);
    }

    static BlockState pickFoundationBlock(BlockState baseState, int x, int y, int z, int depth) {
        if (baseState.is(Blocks.SANDSTONE) || baseState.is(Blocks.CUT_SANDSTONE)
                || baseState.is(Blocks.SMOOTH_SANDSTONE) || baseState.is(Blocks.CHISELED_SANDSTONE)
                || baseState.is(Blocks.SAND)) {
            return depth == 0 && baseState.is(Blocks.SAND)
                    ? Blocks.SANDSTONE.defaultBlockState()
                    : Blocks.SANDSTONE.defaultBlockState();
        }
        if (baseState.is(Blocks.DEEPSLATE) || baseState.is(Blocks.DEEPSLATE_BRICKS)
                || baseState.is(Blocks.DEEPSLATE_TILES) || baseState.is(Blocks.POLISHED_BLACKSTONE)
                || baseState.is(Blocks.POLISHED_BLACKSTONE_BRICKS) || baseState.is(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)) {
            return depth < 2 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
        }
        if (baseState.is(Blocks.DARK_PRISMARINE) || baseState.is(Blocks.PRISMARINE_BRICKS)) {
            return depth < 2 ? Blocks.DARK_PRISMARINE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (baseState.is(Blocks.DIRT) || baseState.is(Blocks.GRASS_BLOCK) || baseState.is(Blocks.MOSS_BLOCK)) {
            return depth < 2 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (depth < 2) {
            if (baseState.is(Blocks.MOSSY_STONE_BRICKS) || ((x + z) & 3) == 0) {
                return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            }
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (y < 16) {
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        return ((x * 3 + y + z * 5) & 7) == 0
                ? Blocks.COBBLESTONE.defaultBlockState()
                : Blocks.STONE.defaultBlockState();
    }
}

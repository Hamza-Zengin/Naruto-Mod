package com.sekwah.narutomod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Handles water/lava filtering during site selection, exposed structure water framing,
 * and adjacent lava pool clearing for {@link AnchoredJigsawStructure}.
 */
final class StructureLiquidHandler {

    static final int LAVA_SCAN_PADDING = 2;

    private static final int[][] CARDINAL_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private StructureLiquidHandler() {
    }

    static void frameStructureWater(WorldGenLevel level,
                                    BoundingBox chunkBox,
                                    BoundingBox pieceBox,
                                    int x0,
                                    int x1,
                                    int z0,
                                    int z1,
                                    int scanLow,
                                    int scanHigh,
                                    int worldMinY,
                                    int worldMaxY,
                                    int maxFoundationDepth,
                                    BlockState themeState) {
        BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos auxPos = new BlockPos.MutableBlockPos();

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = scanLow; y <= scanHigh; y++) {
                    BlockState sourceState = level.getBlockState(sourcePos.set(x, y, z));
                    if (!sourceState.is(Blocks.WATER)) {
                        continue;
                    }
                    BlockState aboveSource = y + 1 < worldMaxY
                            ? level.getBlockState(auxPos.set(x, y + 1, z))
                            : Blocks.AIR.defaultBlockState();
                    BlockState belowSource = y - 1 >= worldMinY
                            ? level.getBlockState(auxPos.set(x, y - 1, z))
                            : Blocks.STONE.defaultBlockState();

                    for (int[] dir : CARDINAL_DIRECTIONS) {
                        int nx = x + dir[0];
                        int nz = z + dir[1];
                        if (nx < chunkBox.minX() || nx > chunkBox.maxX()
                                || nz < chunkBox.minZ() || nz > chunkBox.maxZ()) {
                            continue;
                        }
                        BlockState neighborState = level.getBlockState(neighborPos.set(nx, y, nz));
                        BlockState belowNeighbor = y - 1 >= worldMinY
                                ? level.getBlockState(auxPos.set(nx, y - 1, nz))
                                : Blocks.STONE.defaultBlockState();
                        if (!isWaterEdgeToFrame(sourceState, neighborState, aboveSource, belowSource, belowNeighbor,
                                y, pieceBox.minY())) {
                            continue;
                        }

                        level.setBlock(neighborPos, StructureFoundationBuilder.pickFoundationBlock(themeState, nx, y, nz, 0), 2);
                        int supportStopY = Math.max(worldMinY, y - maxFoundationDepth);
                        for (int sy = y - 1; sy >= supportStopY; sy--) {
                            BlockState existing = level.getBlockState(auxPos.set(nx, sy, nz));
                            if (!StructureFoundationBuilder.isFoundationReplaceable(existing)) {
                                break;
                            }
                            int depth = y - sy;
                            level.setBlock(auxPos, StructureFoundationBuilder.pickFoundationBlock(themeState, nx, sy, nz, depth), 2);
                        }
                    }
                }
            }
        }
    }

    static boolean hasLavaInSampleColumn(ChunkGenerator generator,
                                         int x,
                                         int z,
                                         int surfaceY,
                                         int floorY,
                                         LevelHeightAccessor heightAccessor,
                                         RandomState randomState) {
        NoiseColumn column = generator.getBaseColumn(x, z, heightAccessor, randomState);
        int minCheckY = Math.max(heightAccessor.getMinY(), Math.min(surfaceY, floorY) - 4);
        int maxCheckY = Math.min(heightAccessor.getMaxY() - 1, Math.max(surfaceY, floorY) + 2);
        for (int y = maxCheckY; y >= minCheckY; y--) {
            if (isLavaBlock(column.getBlock(y))) {
                return true;
            }
        }
        return false;
    }

    static void clearLavaAroundPiece(WorldGenLevel level,
                                     BlockPos.MutableBlockPos cursor,
                                     BoundingBox chunkBox,
                                     BoundingBox pieceBox,
                                     int minBaseY,
                                     int maxBaseY,
                                     int worldMinY,
                                     int worldMaxY,
                                     BlockState themeState) {
        int lx0 = Math.max(chunkBox.minX(), pieceBox.minX() - LAVA_SCAN_PADDING);
        int lx1 = Math.min(chunkBox.maxX(), pieceBox.maxX() + LAVA_SCAN_PADDING);
        int lz0 = Math.max(chunkBox.minZ(), pieceBox.minZ() - LAVA_SCAN_PADDING);
        int lz1 = Math.min(chunkBox.maxZ(), pieceBox.maxZ() + LAVA_SCAN_PADDING);
        int ly0 = Math.max(worldMinY, minBaseY - 4);
        int ly1 = Math.min(worldMaxY - 1, maxBaseY + 3);

        for (int x = lx0; x <= lx1; x++) {
            for (int z = lz0; z <= lz1; z++) {
                for (int y = ly1; y >= ly0; y--) {
                    BlockState state = level.getBlockState(cursor.set(x, y, z));
                    if (isLavaBlock(state)) {
                        int depth = Math.max(0, minBaseY - y);
                        BlockState fillState = StructureFoundationBuilder.pickFoundationBlock(themeState, x, y, z, depth);
                        level.setBlock(cursor, fillState, 2);
                    }
                }
            }
        }
    }

    static boolean isLavaBlock(BlockState state) {
        return state.is(Blocks.LAVA) || state.getFluidState().is(FluidTags.LAVA);
    }

    static boolean shouldRejectCenterWater(boolean allowShallowWater, int centerSurfaceY, int centerFloorY) {
        if (!allowShallowWater) {
            return centerSurfaceY > centerFloorY;
        }
        return (centerSurfaceY - centerFloorY) > 6;
    }

    static boolean shouldRejectWaterSample(int surfaceY, int floorY, int deepWaterLimit) {
        return (surfaceY - floorY) > deepWaterLimit;
    }

    static boolean isWaterEdgeToFrame(BlockState sourceState,
                                        BlockState neighborState,
                                        BlockState aboveSourceState,
                                        BlockState belowSourceState,
                                        BlockState belowNeighborState,
                                        int y,
                                        int pieceMinY) {
        if (!sourceState.is(Blocks.WATER)) {
            return false;
        }
        if (!(neighborState.isAir()
                || (neighborState.canBeReplaced() && neighborState.getFluidState().isEmpty()))) {
            return false;
        }
        if (!belowNeighborState.getFluidState().isEmpty()) {
            return false;
        }
        if (y > pieceMinY + 6
                && (!aboveSourceState.getFluidState().isEmpty() || !belowSourceState.getFluidState().isEmpty())) {
            return false;
        }
        return true;
    }
}

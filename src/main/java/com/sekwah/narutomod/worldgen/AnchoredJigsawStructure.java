package com.sekwah.narutomod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper around {@link JigsawStructure} that prevents large mod structures from floating
 * on small hilltop spikes or steep cliffs:
 * <ol>
 *   <li>Samples terrain height across a 17-point radial grid around the candidate chunk center
 *       and rejects locations with lava pools, excessive elevation spread, or water exceeding limits.</li>
 *   <li>Anchors the jigsaw start elevation toward the surrounding terrain median/lower-quartile
 *       instead of an isolated peak spike.</li>
 *   <li>During {@link #afterPlace}, scans the full piece height, places proportional 2x2 corner-first
 *       symmetric foundation columns (plus convex overhang corners) for {@code gap >= 1}, seals fluid
 *       lips, frames exposed structure water edges, clears adjacent lava pools, and removes floating
 *       natural terrain overhangs above placed pieces.</li>
 * </ol>
 */
public class AnchoredJigsawStructure extends Structure {

    public static final MapCodec<AnchoredJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            JigsawStructure.CODEC.forGetter(s -> s.delegate),
            Codec.INT.optionalFieldOf("max_height_variance", 16).forGetter(s -> s.maxHeightVariance),
            Codec.INT.optionalFieldOf("sample_radius", 24).forGetter(s -> s.sampleRadius),
            Codec.INT.optionalFieldOf("max_foundation_depth", 36).forGetter(s -> s.maxFoundationDepth),
            Codec.BOOL.optionalFieldOf("allow_shallow_water", false).forGetter(s -> s.allowShallowWater)
    ).apply(instance, AnchoredJigsawStructure::new));

    static final int PILLAR_SIZE = 2;
    static final int TIER2_MIN_EMBED_DEPTH = 3;
    static final int TIER2_MAX_EMBED_DEPTH = 4;
    static final int MIN_COLUMN_CLEAR_GAP = 5;
    static final int LAVA_SCAN_PADDING = 2;
    static final int OVERHEAD_CLEAR_HEIGHT = 36;

    private static final int[][] DIRECTIONS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1},
            {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static final int[][] CARDINAL_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final ConcurrentHashMap<StructurePoolElement, Boolean> SKY_BRIDGE_CACHE = new ConcurrentHashMap<>();

    private final JigsawStructure delegate;
    private final int maxHeightVariance;
    private final int sampleRadius;
    private final int maxFoundationDepth;
    private final boolean allowShallowWater;

    public AnchoredJigsawStructure(JigsawStructure delegate,
                                   int maxHeightVariance,
                                   int sampleRadius,
                                   int maxFoundationDepth,
                                   boolean allowShallowWater) {
        super(new StructureSettings(
                delegate.biomes(),
                delegate.spawnOverrides(),
                delegate.step(),
                delegate.terrainAdaptation()
        ));
        this.delegate = delegate;
        this.maxHeightVariance = Math.max(4, maxHeightVariance);
        this.sampleRadius = Math.max(8, sampleRadius);
        this.maxFoundationDepth = Math.max(4, maxFoundationDepth);
        this.allowShallowWater = allowShallowWater;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        if (generator instanceof FlatLevelSource) {
            return delegate.findValidGenerationPoint(context);
        }

        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();

        int[] surfaceSamples = new int[17];
        int deepWaterLimit = allowShallowWater ? 6 : 1;
        int maxWaterSamples = allowShallowWater ? 8 : 1;
        int deepWaterSamples = 0;

        int centerSurfaceY = generator.getFirstOccupiedHeight(
                cx, cz, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        int centerFloorY = generator.getFirstOccupiedHeight(
                cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
        if (shouldRejectCenterWater(allowShallowWater, centerSurfaceY, centerFloorY)) {
            return Optional.empty();
        }
        if (hasLavaInSampleColumn(generator, cx, cz, centerSurfaceY, centerFloorY, heightAccessor, randomState)) {
            return Optional.empty();
        }
        surfaceSamples[0] = centerSurfaceY;

        int idx = 1;
        int innerR = Math.max(4, sampleRadius / 2);
        for (int ring = 0; ring < 2; ring++) {
            int r = ring == 0 ? innerR : sampleRadius;
            int diag = Math.max(3, (int) Math.round(r * 0.7071D));
            for (int[] dir : DIRECTIONS) {
                int stepX = dir[0] != 0 && dir[1] != 0 ? dir[0] * diag : dir[0] * r;
                int stepZ = dir[0] != 0 && dir[1] != 0 ? dir[1] * diag : dir[1] * r;
                int sx = cx + stepX;
                int sz = cz + stepZ;

                int sY = generator.getFirstOccupiedHeight(
                        sx, sz, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
                int fY = generator.getFirstOccupiedHeight(
                        sx, sz, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
                if (hasLavaInSampleColumn(generator, sx, sz, sY, fY, heightAccessor, randomState)) {
                    return Optional.empty();
                }
                if (shouldRejectWaterSample(sY, fY, deepWaterLimit)) {
                    deepWaterSamples++;
                }
                surfaceSamples[idx++] = sY;
            }
        }

        if (deepWaterSamples > maxWaterSamples) {
            return Optional.empty();
        }

        int[] sorted = Arrays.copyOf(surfaceSamples, surfaceSamples.length);
        Arrays.sort(sorted);
        int minY = sorted[0];
        int maxY = sorted[sorted.length - 1];
        if ((maxY - minY) > maxHeightVariance) {
            return Optional.empty();
        }

        int p25Y = sorted[4];
        int medianY = sorted[8];
        int targetSurfaceY = (p25Y + medianY) / 2;
        int rawDy = targetSurfaceY - centerSurfaceY;

        Optional<GenerationStub> delegateStubOpt = delegate.findValidGenerationPoint(context);
        if (delegateStubOpt.isEmpty()) {
            return delegateStubOpt;
        }

        GenerationStub delegateStub = delegateStubOpt.get();
        int delegateY = delegateStub.position().getY();
        boolean preserveSunkenOffset = delegateY <= centerSurfaceY - 3;
        int safeDy = computeSafeVerticalShift(rawDy, delegateY, generator.getSeaLevel(), preserveSunkenOffset);
        if (delegateY + safeDy < heightAccessor.getMinY() + 8) {
            safeDy = (heightAccessor.getMinY() + 8) - delegateY;
        }
        if (safeDy == 0) {
            return delegateStubOpt;
        }

        final int verticalShift = safeDy;
        BlockPos shiftedPos = delegateStub.position().offset(0, verticalShift, 0);
        return Optional.of(new GenerationStub(shiftedPos, builder -> {
            StructurePiecesBuilder inner = delegateStub.getPiecesBuilder();
            inner.offsetPiecesVertically(verticalShift);
            for (StructurePiece piece : inner.build().pieces()) {
                builder.addPiece(piece);
            }
        }));
    }

    static int computeSafeVerticalShift(int rawDy, int delegateY, int seaLevel, boolean preserveSunkenOffset) {
        int clampedDy = Math.max(0, Math.min(4, rawDy));
        if (!preserveSunkenOffset) {
            int minSafeStartY = seaLevel + 1;
            if (delegateY + clampedDy < minSafeStartY) {
                clampedDy = minSafeStartY - delegateY;
            }
        }
        return clampedDy;
    }

    @Override
    public void afterPlace(WorldGenLevel level,
                           StructureManager structureManager,
                           ChunkGenerator chunkGenerator,
                           RandomSource random,
                           BoundingBox chunkBox,
                           ChunkPos chunkPos,
                           PiecesContainer pieces) {
        super.afterPlace(level, structureManager, chunkGenerator, random, chunkBox, chunkPos, pieces);

        if (maxFoundationDepth <= 0) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int worldMinY = level.getMinY() + 1;
        int worldMaxY = level.getMaxY();

        for (StructurePiece piece : pieces.pieces()) {
            BoundingBox pieceBox = piece.getBoundingBox();
            if (!pieceBox.intersects(chunkBox)) {
                continue;
            }
            if (piece instanceof PoolElementStructurePiece poolPiece
                    && isSkyBridgeElement(poolPiece.getElement())) {
                continue;
            }

            int x0 = Math.max(chunkBox.minX(), pieceBox.minX());
            int x1 = Math.min(chunkBox.maxX(), pieceBox.maxX());
            int z0 = Math.max(chunkBox.minZ(), pieceBox.minZ());
            int z1 = Math.min(chunkBox.maxZ(), pieceBox.maxZ());

            int scanLow = Math.max(worldMinY, pieceBox.minY());
            int scanHigh = Math.min(worldMaxY - 1, pieceBox.maxY());
            if (scanLow > scanHigh) {
                continue;
            }

            BlockState dominantBaseState = null;
            int minBaseY = Integer.MAX_VALUE;
            int maxBaseY = Integer.MIN_VALUE;

            int width = (x1 - x0) + 1;
            int depthSpan = (z1 - z0) + 1;
            int[][] baseYs = new int[width][depthSpan];
            int[][] gapDepths = new int[width][depthSpan];
            BlockState[][] baseStates = new BlockState[width][depthSpan];

            for (int x = x0; x <= x1; x++) {
                int xi = x - x0;
                for (int z = z0; z <= z1; z++) {
                    int zi = z - z0;
                    int baseY = -1;
                    BlockState baseState = null;
                    for (int y = scanLow; y <= scanHigh; y++) {
                        BlockState candidate = level.getBlockState(cursor.set(x, y, z));
                        if (isStructuralBase(candidate)) {
                            if (baseY < 0) {
                                baseY = y;
                                baseState = candidate;
                                break;
                            }
                        }
                    }
                    baseYs[xi][zi] = baseY;
                    baseStates[xi][zi] = baseState;
                    if (baseY < 0) {
                        continue;
                    }

                    if (dominantBaseState == null) {
                        dominantBaseState = baseState;
                    }
                    if (baseY < minBaseY) {
                        minBaseY = baseY;
                    }
                    if (baseY > maxBaseY) {
                        maxBaseY = baseY;
                    }

                    int gap = 0;
                    int maxMeasureY = Math.max(worldMinY, baseY - maxFoundationDepth);
                    for (int y = baseY - 1; y >= maxMeasureY; y--) {
                        BlockState existing = level.getBlockState(cursor.set(x, y, z));
                        if (!isFoundationReplaceable(existing)) {
                            break;
                        }
                        gap++;
                    }
                    gapDepths[xi][zi] = gap;
                }
            }

            if (dominantBaseState == null) {
                dominantBaseState = Blocks.COBBLESTONE.defaultBlockState();
            }
            if (minBaseY == Integer.MAX_VALUE) {
                minBaseY = scanLow;
                maxBaseY = scanHigh;
            }

            clearLavaAroundPiece(level, cursor, chunkBox, pieceBox, minBaseY, maxBaseY, worldMinY, worldMaxY, dominantBaseState);

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

            frameStructureWater(level, chunkBox, pieceBox, x0, x1, z0, z1, scanLow, scanHigh, worldMinY, worldMaxY,
                    maxFoundationDepth, dominantBaseState);
            clearOverheadNaturalTerrain(level, cursor, chunkBox, pieces, piece, pieceBox, x0, x1, z0, z1, scanHigh, worldMaxY,
                    baseYs);
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

    private static void markPillarFootprint(boolean[][] pillarMask,
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

    private static boolean hasNearbyOtherPillar(boolean[][] pillarMask,
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

    private static void frameStructureWater(WorldGenLevel level,
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

                        level.setBlock(neighborPos, pickFoundationBlock(themeState, nx, y, nz, 0), 2);
                        int supportStopY = Math.max(worldMinY, y - maxFoundationDepth);
                        for (int sy = y - 1; sy >= supportStopY; sy--) {
                            BlockState existing = level.getBlockState(auxPos.set(nx, sy, nz));
                            if (!isFoundationReplaceable(existing)) {
                                break;
                            }
                            int depth = y - sy;
                            level.setBlock(auxPos, pickFoundationBlock(themeState, nx, sy, nz, depth), 2);
                        }
                    }
                }
            }
        }
    }

    static void clearOverheadNaturalTerrain(WorldGenLevel level,
                                            BlockPos.MutableBlockPos cursor,
                                            BoundingBox chunkBox,
                                            PiecesContainer pieces,
                                            StructurePiece currentPiece,
                                            BoundingBox pieceBox,
                                            int x0,
                                            int x1,
                                            int z0,
                                            int z1,
                                            int scanHigh,
                                            int worldMaxY,
                                            int[][] baseYs) {
        if (pieceBox.maxY() >= worldMaxY - 1 || scanHigh < pieceBox.maxY()) {
            return;
        }
        int maxClearY = Math.min(worldMaxY - 1, pieceBox.maxY() + OVERHEAD_CLEAR_HEIGHT);

        for (int x = x0; x <= x1; x++) {
            int xi = x - x0;
            for (int z = z0; z <= z1; z++) {
                int zi = z - z0;
                if (baseYs[xi][zi] < 0) {
                    continue;
                }
                for (int y = pieceBox.maxY() + 1; y <= maxClearY; y++) {
                    if (isInsideOtherPiece(pieces, currentPiece, x, y, z)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor.set(x, y, z));
                    if (isNaturalOverhangBlock(state) || !state.getFluidState().isEmpty()) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        int bx0 = Math.max(chunkBox.minX(), pieceBox.minX() - 3);
        int bx1 = Math.min(chunkBox.maxX(), pieceBox.maxX() + 3);
        int bz0 = Math.max(chunkBox.minZ(), pieceBox.minZ() - 3);
        int bz1 = Math.min(chunkBox.maxZ(), pieceBox.maxZ() + 3);
        for (int x = bx0; x <= bx1; x++) {
            int dxOutside = x < pieceBox.minX() ? pieceBox.minX() - x : (x > pieceBox.maxX() ? x - pieceBox.maxX() : 0);
            for (int z = bz0; z <= bz1; z++) {
                int dzOutside = z < pieceBox.minZ() ? pieceBox.minZ() - z : (z > pieceBox.maxZ() ? z - pieceBox.maxZ() : 0);
                int dist = Math.max(dxOutside, dzOutside);
                if (dist == 0) {
                    continue;
                }
                int startTerraceY = pieceBox.minY() + 3 + dist;
                for (int y = startTerraceY; y <= maxClearY; y++) {
                    if (isInsideOtherPiece(pieces, currentPiece, x, y, z)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor.set(x, y, z));
                    if (isNaturalOverhangBlock(state)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static boolean isInsideOtherPiece(PiecesContainer pieces,
                                              StructurePiece currentPiece,
                                              int x,
                                              int y,
                                              int z) {
        for (StructurePiece other : pieces.pieces()) {
            if (other == currentPiece) {
                continue;
            }
            BoundingBox box = other.getBoundingBox();
            if (x >= box.minX() && x <= box.maxX()
                    && y >= box.minY() && y <= box.maxY()
                    && z >= box.minZ() && z <= box.maxZ()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLavaInSampleColumn(ChunkGenerator generator,
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

    private static void clearLavaAroundPiece(WorldGenLevel level,
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
                        BlockState fillState = pickFoundationBlock(themeState, x, y, z, depth);
                        level.setBlock(cursor, fillState, 2);
                    }
                }
            }
        }
    }

    private static void fillFoundationColumn(WorldGenLevel level,
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

    static boolean isNaturalOverhangBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.GOLD_ORES)) {
            return true;
        }
        String blockPath = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (blockPath.endsWith("_terracotta")) {
            return true;
        }
        return state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.TERRACOTTA)
                || state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.REDSTONE_ORE)
                || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(Blocks.EMERALD_ORE)
                || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.LAPIS_ORE)
                || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE);
    }

    static int computeTier2EmbedDepth(int x, int z) {
        return ((x + z) & 1) == 0 ? TIER2_MIN_EMBED_DEPTH : TIER2_MAX_EMBED_DEPTH;
    }

    private static boolean isSkyBridgeElement(StructurePoolElement element) {
        Boolean cached = SKY_BRIDGE_CACHE.get(element);
        if (cached != null) {
            return cached;
        }
        if (SKY_BRIDGE_CACHE.size() > 2048) {
            SKY_BRIDGE_CACHE.clear();
        }
        boolean isSkyBridge = element.toString().contains("sky_bridge");
        SKY_BRIDGE_CACHE.putIfAbsent(element, isSkyBridge);
        return isSkyBridge;
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

    @Override
    public StructureType<?> type() {
        return NarutoStructures.ANCHORED_JIGSAW.get();
    }
}

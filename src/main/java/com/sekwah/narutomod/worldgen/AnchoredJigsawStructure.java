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
 *   <li>During {@link #afterPlace}, places globally grid-aligned spaced stone columns and outer
 *       footprint corner pillars for any {@code gap >= 1} overhang, seals 1-block fluid lips on
 *       non-pillar water-edge points, and clears adjacent lava pools.</li>
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

    static final int TIER2_MIN_EMBED_DEPTH = 3;
    static final int TIER2_MAX_EMBED_DEPTH = 4;
    static final int MIN_COLUMN_CLEAR_GAP = 5;
    static final int LAVA_SCAN_PADDING = 2;

    private static final int[][] DIRECTIONS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1},
            {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
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
        int clampedDy = Math.max(-maxHeightVariance, Math.min(3, rawDy));

        Optional<GenerationStub> delegateStubOpt = delegate.findValidGenerationPoint(context);
        if (delegateStubOpt.isEmpty() || clampedDy == 0) {
            return delegateStubOpt;
        }

        GenerationStub delegateStub = delegateStubOpt.get();
        int safeDy = clampedDy;
        if (delegateStub.position().getY() + safeDy < heightAccessor.getMinY() + 8) {
            safeDy = (heightAccessor.getMinY() + 8) - delegateStub.position().getY();
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
            int scanHigh = Math.min(worldMaxY, pieceBox.minY() + Math.min(12, Math.max(4, pieceBox.getYSpan() / 2)));
            int pieceWidth = Math.max(pieceBox.getXSpan(), pieceBox.getZSpan());

            BlockState dominantBaseState = null;
            int minBaseY = Integer.MAX_VALUE;
            int maxBaseY = Integer.MIN_VALUE;
            int footprintMinX = Integer.MAX_VALUE;
            int footprintMaxX = Integer.MIN_VALUE;
            int footprintMinZ = Integer.MAX_VALUE;
            int footprintMaxZ = Integer.MIN_VALUE;

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
                            baseY = y;
                            baseState = candidate;
                            break;
                        }
                    }
                    baseYs[xi][zi] = baseY;
                    baseStates[xi][zi] = baseState;
                    if (baseY < 0) {
                        continue;
                    }

                    if (x < footprintMinX) {
                        footprintMinX = x;
                    }
                    if (x > footprintMaxX) {
                        footprintMaxX = x;
                    }
                    if (z < footprintMinZ) {
                        footprintMinZ = z;
                    }
                    if (z > footprintMaxZ) {
                        footprintMaxZ = z;
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
            if (footprintMinX == Integer.MAX_VALUE) {
                footprintMinX = x0;
                footprintMaxX = x1;
                footprintMinZ = z0;
                footprintMaxZ = z1;
            }

            clearLavaAroundPiece(level, cursor, chunkBox, pieceBox, minBaseY, maxBaseY, worldMinY, worldMaxY, dominantBaseState);

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

                    int pillarSize = selectPillarSize(gap, pieceWidth);
                    int stride = computePillarStride(pillarSize);
                    boolean isPillar = isOnGlobalPillarGrid(x, z, pillarSize, stride)
                            || isCornerPillarPoint(x, z, footprintMinX, footprintMaxX, footprintMinZ, footprintMaxZ, pillarSize);

                    if (isPillar) {
                        int embedDepth = computeTier2EmbedDepth(x, z);
                        int stopY = Math.max(worldMinY, baseY - Math.min(maxFoundationDepth, gap + embedDepth));
                        fillFoundationColumn(level, cursor, baseState, x, z, baseY, stopY);
                    } else {
                        // Water-edge lip seal: seal 1 block below baseY so water does not bleed under walls/floors
                        BlockState immediatelyBelow = level.getBlockState(cursor.set(x, baseY - 1, z));
                        if (!immediatelyBelow.getFluidState().isEmpty()) {
                            int sealY = baseY - 1;
                            if (sealY >= worldMinY) {
                                BlockState fillState = pickFoundationBlock(baseState, x, sealY, z, 0);
                                level.setBlock(cursor.set(x, sealY, z), fillState, 2);
                            }
                        }
                    }
                }
            }
        }
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
        if (gapDepth > 28 && pieceWidth >= 26) {
            return 4;
        }
        if (gapDepth > 20 || pieceWidth >= 20) {
            return 3;
        }
        return 2;
    }

    static int computePillarStride(int pillarSize) {
        int clampedSize = Math.max(2, Math.min(4, pillarSize));
        // Ensures clear gap between adjacent pillars is always >= 6 blocks (>= MIN_COLUMN_CLEAR_GAP = 5)
        return clampedSize + 6;
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

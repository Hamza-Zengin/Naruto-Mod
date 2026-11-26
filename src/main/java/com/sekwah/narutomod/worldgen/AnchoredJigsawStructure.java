package com.sekwah.narutomod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
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
 *   <li>During {@link #afterPlace}, scans the full piece height, delegates proportional 2x2 corner-first
 *       symmetric foundation building to {@link StructureFoundationBuilder}, delegates water framing and
 *       lava clearing to {@link StructureLiquidHandler}, and removes floating natural terrain overhangs
 *       above placed pieces.</li>
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

    static final int PILLAR_SIZE = StructureFoundationBuilder.PILLAR_SIZE;
    static final int TIER2_MIN_EMBED_DEPTH = StructureFoundationBuilder.TIER2_MIN_EMBED_DEPTH;
    static final int TIER2_MAX_EMBED_DEPTH = StructureFoundationBuilder.TIER2_MAX_EMBED_DEPTH;
    static final int MIN_COLUMN_CLEAR_GAP = StructureFoundationBuilder.MIN_COLUMN_CLEAR_GAP;
    static final int LAVA_SCAN_PADDING = StructureLiquidHandler.LAVA_SCAN_PADDING;
    static final int OVERHEAD_CLEAR_HEIGHT = 36;

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
        if (StructureLiquidHandler.shouldRejectCenterWater(allowShallowWater, centerSurfaceY, centerFloorY)) {
            return Optional.empty();
        }
        if (StructureLiquidHandler.hasLavaInSampleColumn(
                generator, cx, cz, centerSurfaceY, centerFloorY, heightAccessor, randomState)) {
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
                if (StructureLiquidHandler.hasLavaInSampleColumn(
                        generator, sx, sz, sY, fY, heightAccessor, randomState)) {
                    return Optional.empty();
                }
                if (StructureLiquidHandler.shouldRejectWaterSample(sY, fY, deepWaterLimit)) {
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
                        if (StructureFoundationBuilder.isStructuralBase(candidate)) {
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
                        if (!StructureFoundationBuilder.isFoundationReplaceable(existing)) {
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

            StructureLiquidHandler.clearLavaAroundPiece(
                    level, cursor, chunkBox, pieceBox, minBaseY, maxBaseY, worldMinY, worldMaxY, dominantBaseState);
            StructureFoundationBuilder.applyPieceFoundations(
                    level, cursor, pieceBox, x0, x1, z0, z1, worldMinY, maxFoundationDepth,
                    baseYs, gapDepths, baseStates);
            StructureLiquidHandler.frameStructureWater(
                    level, chunkBox, pieceBox, x0, x1, z0, z1, scanLow, scanHigh, worldMinY, worldMaxY,
                    maxFoundationDepth, dominantBaseState);
            clearOverheadNaturalTerrain(
                    level, cursor, chunkBox, pieces, piece, pieceBox, x0, x1, z0, z1, scanHigh, worldMaxY, baseYs);
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

    static boolean[][] planPiecePillars(BoundingBox pieceBox,
                                        int x0,
                                        int x1,
                                        int z0,
                                        int z1,
                                        int[][] baseYs,
                                        int[][] gapDepths) {
        return StructureFoundationBuilder.planPiecePillars(pieceBox, x0, x1, z0, z1, baseYs, gapDepths);
    }

    static boolean isLavaBlock(BlockState state) {
        return StructureLiquidHandler.isLavaBlock(state);
    }

    static boolean shouldRejectCenterWater(boolean allowShallowWater, int centerSurfaceY, int centerFloorY) {
        return StructureLiquidHandler.shouldRejectCenterWater(allowShallowWater, centerSurfaceY, centerFloorY);
    }

    static boolean shouldRejectWaterSample(int surfaceY, int floorY, int deepWaterLimit) {
        return StructureLiquidHandler.shouldRejectWaterSample(surfaceY, floorY, deepWaterLimit);
    }

    static int selectPillarSize(int gapDepth, int pieceWidth) {
        return StructureFoundationBuilder.selectPillarSize(gapDepth, pieceWidth);
    }

    static int computePillarStride(int pillarSize) {
        return StructureFoundationBuilder.computePillarStride(pillarSize);
    }

    static int[] computeAxisPillarStarts(int min, int max, int pillarSize, int minClearGap) {
        return StructureFoundationBuilder.computeAxisPillarStarts(min, max, pillarSize, minClearGap);
    }

    static boolean isAxisPillarCoord(int coord, int[] starts, int pillarSize) {
        return StructureFoundationBuilder.isAxisPillarCoord(coord, starts, pillarSize);
    }

    static boolean isOnPiecePillarGrid(int x, int z, int[] xStarts, int[] zStarts, int pillarSize) {
        return StructureFoundationBuilder.isOnPiecePillarGrid(x, z, xStarts, zStarts, pillarSize);
    }

    static boolean isOnGlobalPillarGrid(int x, int z, int pillarSize, int stride) {
        return StructureFoundationBuilder.isOnGlobalPillarGrid(x, z, pillarSize, stride);
    }

    static boolean isCornerPillarPoint(int x, int z, int minX, int maxX, int minZ, int maxZ, int pillarSize) {
        return StructureFoundationBuilder.isCornerPillarPoint(x, z, minX, maxX, minZ, maxZ, pillarSize);
    }

    static boolean isConvexFootprintCorner(boolean hasBase,
                                           boolean noNegX,
                                           boolean noPosX,
                                           boolean noNegZ,
                                           boolean noPosZ) {
        return StructureFoundationBuilder.isConvexFootprintCorner(hasBase, noNegX, noPosX, noNegZ, noPosZ);
    }

    static boolean isWaterEdgeToFrame(BlockState sourceState,
                                        BlockState neighborState,
                                        BlockState aboveSourceState,
                                        BlockState belowSourceState,
                                        BlockState belowNeighborState,
                                        int y,
                                        int pieceMinY) {
        return StructureLiquidHandler.isWaterEdgeToFrame(
                sourceState, neighborState, aboveSourceState, belowSourceState, belowNeighborState, y, pieceMinY);
    }

    static int computeTier2EmbedDepth(int x, int z) {
        return StructureFoundationBuilder.computeTier2EmbedDepth(x, z);
    }

    static boolean isStructuralBase(BlockState state) {
        return StructureFoundationBuilder.isStructuralBase(state);
    }

    static boolean isFoundationReplaceable(BlockState state) {
        return StructureFoundationBuilder.isFoundationReplaceable(state);
    }

    static BlockState pickFoundationBlock(BlockState baseState, int x, int y, int z, int depth) {
        return StructureFoundationBuilder.pickFoundationBlock(baseState, x, y, z, depth);
    }

    @Override
    public StructureType<?> type() {
        return NarutoStructures.ANCHORED_JIGSAW.get();
    }
}

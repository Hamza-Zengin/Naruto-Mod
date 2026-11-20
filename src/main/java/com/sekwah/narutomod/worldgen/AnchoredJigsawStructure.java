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
 *       and rejects locations whose elevation spread or deep-water coverage exceeds configured limits.</li>
 *   <li>Anchors the jigsaw start elevation toward the surrounding terrain median/lower-quartile
 *       instead of an isolated peak spike.</li>
 *   <li>During {@link #afterPlace}, dynamically extends solid foundation columns downward from
 *       each piece's bottom footprint until reaching natural solid ground.</li>
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
        int deepWaterLimit = allowShallowWater ? 6 : 3;
        int deepWaterSamples = 0;

        int centerSurfaceY = generator.getFirstOccupiedHeight(
                cx, cz, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        int centerFloorY = generator.getFirstOccupiedHeight(
                cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
        if ((centerSurfaceY - centerFloorY) > deepWaterLimit) {
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
                if ((sY - fY) > deepWaterLimit) {
                    deepWaterSamples++;
                }
                surfaceSamples[idx++] = sY;
            }
        }

        if (deepWaterSamples > (allowShallowWater ? 8 : 5)) {
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
            int scanHigh = Math.min(worldMaxY, pieceBox.minY() + 4);

            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
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
                    if (baseY < 0) {
                        continue;
                    }

                    int stopY = Math.max(worldMinY, baseY - maxFoundationDepth);
                    for (int y = baseY - 1; y >= stopY; y--) {
                        BlockState existing = level.getBlockState(cursor.set(x, y, z));
                        if (!isFoundationReplaceable(existing)) {
                            break;
                        }
                        int depth = (baseY - 1) - y;
                        BlockState fillState = pickFoundationBlock(baseState, x, y, z, depth);
                        level.setBlock(cursor, fillState, 2);
                    }
                }
            }
        }
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

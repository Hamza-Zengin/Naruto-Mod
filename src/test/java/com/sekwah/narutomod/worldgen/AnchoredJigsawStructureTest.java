package com.sekwah.narutomod.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit tests for {@link AnchoredJigsawStructure} foundation classification, direct spaced pillar grid,
 * corner pillar detection, water/lava filtering, material selection, and structure JSON terrain adaptation.
 */
public class AnchoredJigsawStructureTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void testFoundationReplaceableAndStructuralBase() {
        Assertions.assertFalse(AnchoredJigsawStructure.isStructuralBase(Blocks.AIR.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isStructuralBase(Blocks.WATER.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isStructuralBase(Blocks.LAVA.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isStructuralBase(Blocks.STONE.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isStructuralBase(Blocks.SANDSTONE.defaultBlockState()));

        Assertions.assertTrue(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.AIR.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.WATER.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.LAVA.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.STONE.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.DEEPSLATE.defaultBlockState()));
    }

    @Test
    public void testPickFoundationBlockMatchesTheme() {
        BlockState sand = AnchoredJigsawStructure.pickFoundationBlock(Blocks.SANDSTONE.defaultBlockState(), 0, 60, 0, 0);
        Assertions.assertEquals(Blocks.SANDSTONE, sand.getBlock());

        BlockState darkShallow = AnchoredJigsawStructure.pickFoundationBlock(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 0, 40, 0, 0);
        Assertions.assertEquals(Blocks.COBBLED_DEEPSLATE, darkShallow.getBlock());

        BlockState darkDeep = AnchoredJigsawStructure.pickFoundationBlock(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 0, 40, 0, 5);
        Assertions.assertEquals(Blocks.DEEPSLATE, darkDeep.getBlock());

        BlockState dirtShallow = AnchoredJigsawStructure.pickFoundationBlock(Blocks.GRASS_BLOCK.defaultBlockState(), 0, 64, 0, 0);
        Assertions.assertEquals(Blocks.DIRT, dirtShallow.getBlock());

        BlockState dirtDeep = AnchoredJigsawStructure.pickFoundationBlock(Blocks.GRASS_BLOCK.defaultBlockState(), 0, 64, 0, 4);
        Assertions.assertEquals(Blocks.STONE, dirtDeep.getBlock());
    }

    @Test
    public void testPillarScalingAndClearGapAtLeastFiveBlocks() {
        // Default 2x2: gap <= 20 and width < 20
        int size2 = AnchoredJigsawStructure.selectPillarSize(12, 16);
        Assertions.assertEquals(2, size2);
        int stride2 = AnchoredJigsawStructure.computePillarStride(size2);
        Assertions.assertEquals(8, stride2);
        Assertions.assertTrue(stride2 - size2 >= AnchoredJigsawStructure.MIN_COLUMN_CLEAR_GAP);

        // Medium 3x3: gap > 20 or width >= 20
        Assertions.assertEquals(3, AnchoredJigsawStructure.selectPillarSize(21, 16));
        Assertions.assertEquals(3, AnchoredJigsawStructure.selectPillarSize(14, 20));
        int stride3 = AnchoredJigsawStructure.computePillarStride(3);
        Assertions.assertEquals(9, stride3);
        Assertions.assertTrue(stride3 - 3 >= AnchoredJigsawStructure.MIN_COLUMN_CLEAR_GAP);

        // Large 4x4: gap > 28 and width >= 26
        int size4 = AnchoredJigsawStructure.selectPillarSize(29, 26);
        Assertions.assertEquals(4, size4);
        int stride4 = AnchoredJigsawStructure.computePillarStride(size4);
        Assertions.assertEquals(10, stride4);
        Assertions.assertTrue(stride4 - size4 >= AnchoredJigsawStructure.MIN_COLUMN_CLEAR_GAP);
    }

    @Test
    public void testGlobalPillarGridFloorModAlignmentAcrossNegativeCoordinates() {
        for (int size : new int[]{2, 3, 4}) {
            int stride = AnchoredJigsawStructure.computePillarStride(size);
            int minClearGap = Integer.MAX_VALUE;
            int currentGap = 0;
            boolean sawFirstPillar = false;

            // Scan across negative and positive world coordinates at z = -stride (inside pillar band)
            for (int x = -32; x <= 32; x++) {
                boolean onGrid = AnchoredJigsawStructure.isOnGlobalPillarGrid(x, -stride, size, stride);
                if (onGrid) {
                    if (sawFirstPillar && currentGap > 0) {
                        minClearGap = Math.min(minClearGap, currentGap);
                    }
                    sawFirstPillar = true;
                    currentGap = 0;
                } else if (sawFirstPillar) {
                    currentGap++;
                }
            }

            Assertions.assertTrue(minClearGap >= 5,
                    "Expected clear gap >= 5 blocks for pillarSize=" + size + ", got " + minClearGap);
            Assertions.assertEquals(6, minClearGap);

            // Verify exact 2x2 / 3x3 / 4x4 footprint around negative boundary (-stride .. -stride + size - 1)
            for (int dx = 0; dx < stride; dx++) {
                for (int dz = 0; dz < stride; dz++) {
                    boolean expected = dx < size && dz < size;
                    Assertions.assertEquals(expected,
                            AnchoredJigsawStructure.isOnGlobalPillarGrid(-stride + dx, -stride + dz, size, stride));
                }
            }
        }
    }

    @Test
    public void testCornerPillarPointAndDirectPillarEmbedDepth() {
        int minX = 100;
        int maxX = 116;
        int minZ = 200;
        int maxZ = 216;
        int pillarSize = AnchoredJigsawStructure.selectPillarSize(1, 16);
        Assertions.assertEquals(2, pillarSize);

        // All four 2x2 outer footprint corners must be corner pillars
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(100, 200, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(101, 201, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(115, 200, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(116, 201, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(100, 215, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(101, 216, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(115, 215, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertTrue(AnchoredJigsawStructure.isCornerPillarPoint(116, 216, minX, maxX, minZ, maxZ, pillarSize));

        // Mid-edge, interior, out-of-bounds, and invalid pillarSize are not corner pillars
        Assertions.assertFalse(AnchoredJigsawStructure.isCornerPillarPoint(108, 200, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertFalse(AnchoredJigsawStructure.isCornerPillarPoint(100, 208, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertFalse(AnchoredJigsawStructure.isCornerPillarPoint(104, 204, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertFalse(AnchoredJigsawStructure.isCornerPillarPoint(99, 200, minX, maxX, minZ, maxZ, pillarSize));
        Assertions.assertFalse(AnchoredJigsawStructure.isCornerPillarPoint(100, 200, minX, maxX, minZ, maxZ, 0));

        // Direct pillar embed depth (3-4 blocks) applies to all pillars (including gap >= 1)
        int embedEven = AnchoredJigsawStructure.computeTier2EmbedDepth(10, 20);
        int embedOdd = AnchoredJigsawStructure.computeTier2EmbedDepth(10, 21);
        Assertions.assertTrue(embedEven >= 3 && embedEven <= 4);
        Assertions.assertTrue(embedOdd >= 3 && embedOdd <= 4);
    }

    @Test
    public void testAllStructureJsonsUseBeardThin() throws IOException {
        Path structureDir = Path.of("src/main/resources/data/narutomod/worldgen/structure");
        Assertions.assertTrue(Files.isDirectory(structureDir), "Expected structure directory at " + structureDir);

        List<Path> jsonFiles;
        try (Stream<Path> paths = Files.list(structureDir)) {
            jsonFiles = paths.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }

        Assertions.assertEquals(10, jsonFiles.size(), "Expected 10 structure JSON files");
        for (Path jsonFile : jsonFiles) {
            String content = Files.readString(jsonFile);
            JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
            Assertions.assertTrue(obj.has("terrain_adaptation"), "Missing terrain_adaptation in " + jsonFile.getFileName());
            Assertions.assertEquals("beard_thin", obj.get("terrain_adaptation").getAsString(),
                    "Expected beard_thin in " + jsonFile.getFileName());
        }
    }

    @Test
    public void testWaterAndLavaDetectionHelpers() {
        Assertions.assertTrue(AnchoredJigsawStructure.isLavaBlock(Blocks.LAVA.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isLavaBlock(Blocks.WATER.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isLavaBlock(Blocks.STONE.defaultBlockState()));

        // Strict center water check when allowShallowWater == false (depth must be 0)
        Assertions.assertFalse(AnchoredJigsawStructure.shouldRejectCenterWater(false, 64, 64));
        Assertions.assertTrue(AnchoredJigsawStructure.shouldRejectCenterWater(false, 65, 64));

        // Shallow water allowed up to 6 blocks when allowShallowWater == true
        Assertions.assertFalse(AnchoredJigsawStructure.shouldRejectCenterWater(true, 68, 62));
        Assertions.assertTrue(AnchoredJigsawStructure.shouldRejectCenterWater(true, 69, 62));

        // Perimeter sample rejection threshold (deepWaterLimit = 1 when allowShallowWater == false)
        Assertions.assertFalse(AnchoredJigsawStructure.shouldRejectWaterSample(64, 64, 1));
        Assertions.assertFalse(AnchoredJigsawStructure.shouldRejectWaterSample(65, 64, 1));
        Assertions.assertTrue(AnchoredJigsawStructure.shouldRejectWaterSample(66, 64, 1));
    }
}

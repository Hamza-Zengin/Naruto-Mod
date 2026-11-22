package com.sekwah.narutomod.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit tests for {@link AnchoredJigsawStructure} foundation classification, proportional 2x2 corner-first
 * pillar grid, convex overhang corner detection, overhang clearing classification, water edge framing,
 * water/lava filtering, material selection, and structure JSON terrain adaptation.
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
    public void testProportionalTwoByTwoPillarSizeAndAxisGridSpacing() {
        // Always proportional 2x2 regardless of gap depth or piece width
        Assertions.assertEquals(2, AnchoredJigsawStructure.selectPillarSize(1, 13));
        Assertions.assertEquals(2, AnchoredJigsawStructure.selectPillarSize(21, 20));
        Assertions.assertEquals(2, AnchoredJigsawStructure.selectPillarSize(34, 48));

        int stride = AnchoredJigsawStructure.computePillarStride(AnchoredJigsawStructure.PILLAR_SIZE);
        Assertions.assertEquals(8, stride);
        Assertions.assertTrue(stride - AnchoredJigsawStructure.PILLAR_SIZE >= AnchoredJigsawStructure.MIN_COLUMN_CLEAR_GAP);

        // Verify computeAxisPillarStarts across typical and negative ranges
        int[][] ranges = {
                {0, 12},     // 13 wide
                {0, 14},     // 15 wide
                {0, 15},     // 16 wide
                {0, 18},     // 19 wide
                {-100, -42}, // 59 wide negative coordinates
                {-15, 20}    // 36 wide spanning zero
        };
        for (int[] range : ranges) {
            int min = range[0];
            int max = range[1];
            int[] starts = AnchoredJigsawStructure.computeAxisPillarStarts(
                    min, max, AnchoredJigsawStructure.PILLAR_SIZE, AnchoredJigsawStructure.MIN_COLUMN_CLEAR_GAP);
            Assertions.assertTrue(starts.length >= 2, "Expected corner starts for [" + min + ".." + max + "]");
            Assertions.assertEquals(min, starts[0]);
            Assertions.assertEquals(max - AnchoredJigsawStructure.PILLAR_SIZE + 1, starts[starts.length - 1]);

            for (int i = 1; i < starts.length; i++) {
                int clearGap = starts[i] - (starts[i - 1] + AnchoredJigsawStructure.PILLAR_SIZE);
                Assertions.assertTrue(clearGap >= AnchoredJigsawStructure.MIN_COLUMN_CLEAR_GAP,
                        "Expected >= 5 blocks clear gap in [" + min + ".." + max + "], got " + clearGap);
            }
        }

        // Exact check for 19-wide piece: corners at 0..1 and 17..18, center at 9..10
        int[] starts19 = AnchoredJigsawStructure.computeAxisPillarStarts(0, 18, 2, 5);
        Assertions.assertArrayEquals(new int[]{0, 9, 17}, starts19);
        Assertions.assertTrue(AnchoredJigsawStructure.isOnPiecePillarGrid(0, 17, starts19, starts19, 2));
        Assertions.assertTrue(AnchoredJigsawStructure.isOnPiecePillarGrid(9, 10, starts19, starts19, 2));
        Assertions.assertFalse(AnchoredJigsawStructure.isOnPiecePillarGrid(4, 0, starts19, starts19, 2));
    }

    @Test
    public void testPlanPiecePillarsCornerFirstAndNoDoubleThickPillars() {
        BoundingBox pieceBox = new BoundingBox(0, 60, 0, 18, 72, 18);
        int width = 19;
        int depth = 19;
        int[][] baseYs = new int[width][depth];
        int[][] gapDepths = new int[width][depth];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                baseYs[x][z] = 60;
                gapDepths[x][z] = 6;
            }
        }

        boolean[][] mask = AnchoredJigsawStructure.planPiecePillars(pieceBox, 0, 18, 0, 18, baseYs, gapDepths);

        // All 4 2x2 corners + center must be marked, and every run of true cells along X or Z is exactly 2
        Assertions.assertTrue(mask[0][0] && mask[1][1]);
        Assertions.assertTrue(mask[17][0] && mask[18][1]);
        Assertions.assertTrue(mask[0][17] && mask[1][18]);
        Assertions.assertTrue(mask[17][17] && mask[18][18]);
        Assertions.assertTrue(mask[9][9] && mask[10][10]);

        for (int x = 0; x < width; x++) {
            int run = 0;
            for (int z = 0; z < depth; z++) {
                if (mask[x][z]) {
                    run++;
                    Assertions.assertTrue(run <= 2, "Pillar thickness exceeded 2 at x=" + x + ", z=" + z);
                } else {
                    run = 0;
                }
            }
        }
    }

    @Test
    public void testConvexOverhangCornerDetection() {
        Assertions.assertTrue(AnchoredJigsawStructure.isConvexFootprintCorner(true, true, false, true, false));
        Assertions.assertTrue(AnchoredJigsawStructure.isConvexFootprintCorner(true, false, true, false, true));
        Assertions.assertFalse(AnchoredJigsawStructure.isConvexFootprintCorner(false, true, false, true, false));
        Assertions.assertFalse(AnchoredJigsawStructure.isConvexFootprintCorner(true, true, false, false, false));
        Assertions.assertFalse(AnchoredJigsawStructure.isConvexFootprintCorner(true, false, false, false, false));

        // L-shaped footprint: [0..14, 0..14] with [0..6, 0..6] cut out so (7, 0) and (0, 7) are convex corners
        BoundingBox pieceBox = new BoundingBox(0, 60, 0, 14, 70, 14);
        int[][] baseYs = new int[15][15];
        int[][] gapDepths = new int[15][15];
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                boolean occupied = !(x <= 6 && z <= 6);
                baseYs[x][z] = occupied ? 60 : -1;
                gapDepths[x][z] = occupied ? 5 : 0;
            }
        }

        boolean[][] mask = AnchoredJigsawStructure.planPiecePillars(pieceBox, 0, 14, 0, 14, baseYs, gapDepths);
        // Cutout convex overhang corners (7, 0) and (0, 7) must receive 2x2 support pillars
        Assertions.assertTrue(mask[7][0] && mask[8][1], "Expected 2x2 pillar at convex corner (7, 0)");
        Assertions.assertTrue(mask[0][7] && mask[1][8], "Expected 2x2 pillar at convex corner (0, 7)");
        // Outer corners (13..14, 0..1), (0..1, 13..14), (13..14, 13..14) must also be supported
        Assertions.assertTrue(mask[13][0] && mask[14][1]);
        Assertions.assertTrue(mask[0][13] && mask[1][14]);
        Assertions.assertTrue(mask[13][13] && mask[14][14]);
    }

    @Test
    public void testNaturalOverhangBlockClassification() {
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.STONE.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.TERRACOTTA.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                        net.minecraft.resources.Identifier.withDefaultNamespace("orange_terracotta")).defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.RED_SAND.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.SANDSTONE.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.DIRT.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.GRASS_BLOCK.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.ANDESITE.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.DEEPSLATE.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.IRON_ORE.defaultBlockState()));

        Assertions.assertFalse(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.AIR.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.WATER.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.OAK_PLANKS.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.STONE_BRICKS.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.OAK_FENCE.defaultBlockState()));
        Assertions.assertFalse(AnchoredJigsawStructure.isNaturalOverhangBlock(Blocks.LANTERN.defaultBlockState()));
    }

    @Test
    public void testWaterEdgeFramingHelper() {
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        // Basin water adjacent to air should be framed
        Assertions.assertTrue(AnchoredJigsawStructure.isWaterEdgeToFrame(water, air, air, stone, stone, 65, 60));
        // Bottom layer of a 2-deep pool (y <= minY + 6, water above) should still be framed
        Assertions.assertTrue(AnchoredJigsawStructure.isWaterEdgeToFrame(water, air, water, stone, stone, 65, 60));

        // Adjacent water-to-water (jigsaw river connection) must stay open
        Assertions.assertFalse(AnchoredJigsawStructure.isWaterEdgeToFrame(water, water, air, stone, stone, 65, 60));
        // Already framed by solid block
        Assertions.assertFalse(AnchoredJigsawStructure.isWaterEdgeToFrame(water, stone, air, stone, stone, 65, 60));
        // Non-water source block
        Assertions.assertFalse(AnchoredJigsawStructure.isWaterEdgeToFrame(stone, air, air, stone, stone, 65, 60));
        // Waterfall cascade (y > minY + 6 with water above/below) must not be bricked up
        Assertions.assertFalse(AnchoredJigsawStructure.isWaterEdgeToFrame(water, air, water, water, stone, 80, 60));
        // Water directly below neighbor air (waterfall splash) must not be sealed
        Assertions.assertFalse(AnchoredJigsawStructure.isWaterEdgeToFrame(water, air, air, stone, water, 65, 60));
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

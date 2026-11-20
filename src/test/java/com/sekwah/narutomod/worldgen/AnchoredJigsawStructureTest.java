package com.sekwah.narutomod.worldgen;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AnchoredJigsawStructure} foundation classification and material selection.
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
        Assertions.assertFalse(AnchoredJigsawStructure.isStructuralBase(Blocks.JUNGLE_LEAVES.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isStructuralBase(Blocks.STONE.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isStructuralBase(Blocks.SANDSTONE.defaultBlockState()));

        Assertions.assertTrue(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.AIR.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.WATER.defaultBlockState()));
        Assertions.assertTrue(AnchoredJigsawStructure.isFoundationReplaceable(Blocks.JUNGLE_LEAVES.defaultBlockState()));
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
}

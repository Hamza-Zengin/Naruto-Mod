package com.sekwah.narutomod.datagen;

import com.mojang.math.Quadrant;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.block.RotatableDecorativeBlock;
import com.sekwah.narutomod.block.weapons.MakibishiBlock;
import com.sekwah.narutomod.block.weapons.PaperBombBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.stream.Stream;

public class NarutoBlockStateProvider extends ModelProvider {

    public NarutoBlockStateProvider(PackOutput output) {
        super(output, NarutoMod.MOD_ID);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.of(
                NarutoBlocks.BONSAI_TREE,
                NarutoBlocks.TRAINING_DUMMY,
                NarutoBlocks.PAPER_BOMB,
                NarutoBlocks.WIRE_TRAP,
                NarutoBlocks.WEAPON_RACK,
                NarutoBlocks.MAKIBISHI
        );
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.empty();
    }

    @Override
    public String getName() {
        return "Block State Definitions - " + NarutoMod.MOD_ID;
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Bonsai Tree
        Identifier bonsaiModel = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/bonsai_tree");
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(NarutoBlocks.BONSAI_TREE.get())
                        .with(PropertyDispatch.initial(RotatableDecorativeBlock.FACING)
                                .select(Direction.NORTH, BlockModelGenerators.plainVariant(bonsaiModel))
                                .select(Direction.SOUTH, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(bonsaiModel).withYRot(Quadrant.R180)))
                                .select(Direction.EAST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(bonsaiModel).withYRot(Quadrant.R90)))
                                .select(Direction.WEST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(bonsaiModel).withYRot(Quadrant.R270))))
        );

        // Training Dummy
        Identifier trainingDummyModel = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/training_dummy");
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(NarutoBlocks.TRAINING_DUMMY.get())
                        .with(PropertyDispatch.initial(RotatableDecorativeBlock.FACING)
                                .select(Direction.NORTH, BlockModelGenerators.plainVariant(trainingDummyModel))
                                .select(Direction.SOUTH, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(trainingDummyModel).withYRot(Quadrant.R180)))
                                .select(Direction.EAST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(trainingDummyModel).withYRot(Quadrant.R90)))
                                .select(Direction.WEST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(trainingDummyModel).withYRot(Quadrant.R270))))
        );

        // Wire Trap
        Identifier wireTrapModel = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/wire_trap");
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(NarutoBlocks.WIRE_TRAP.get())
                        .with(PropertyDispatch.initial(RotatableDecorativeBlock.FACING)
                                .select(Direction.NORTH, BlockModelGenerators.plainVariant(wireTrapModel))
                                .select(Direction.SOUTH, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(wireTrapModel).withYRot(Quadrant.R180)))
                                .select(Direction.EAST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(wireTrapModel).withYRot(Quadrant.R90)))
                                .select(Direction.WEST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(wireTrapModel).withYRot(Quadrant.R270))))
        );

        // Weapon Rack
        Identifier weaponRackModel = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/weapon_rack");
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(NarutoBlocks.WEAPON_RACK.get())
                        .with(PropertyDispatch.initial(RotatableDecorativeBlock.FACING)
                                .select(Direction.NORTH, BlockModelGenerators.plainVariant(weaponRackModel))
                                .select(Direction.SOUTH, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(weaponRackModel).withYRot(Quadrant.R180)))
                                .select(Direction.EAST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(weaponRackModel).withYRot(Quadrant.R90)))
                                .select(Direction.WEST, BlockModelGenerators.variant(
                                        BlockModelGenerators.plainModel(weaponRackModel).withYRot(Quadrant.R270))))
        );

        // Makibishi (1..4 charges)
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(NarutoBlocks.MAKIBISHI.get())
                        .with(PropertyDispatch.initial(MakibishiBlock.CHARGES)
                                .select(1, BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/makibishi_1")))
                                .select(2, BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/makibishi_2")))
                                .select(3, BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/makibishi_3")))
                                .select(4, BlockModelGenerators.plainVariant(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/makibishi_4"))))
        );

        // Paper Bomb
        Identifier paperBombVisible = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/paper_bomb");
        Identifier paperBombHidden = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "block/paper_bomb_hidden");

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(NarutoBlocks.PAPER_BOMB.get())
                        .with(PropertyDispatch.initial(PaperBombBlock.FACE, PaperBombBlock.FACING, PaperBombBlock.HIDDEN)
                                .generate((face, facing, hidden) -> {
                                    Identifier model = hidden ? paperBombHidden : paperBombVisible;
                                    Variant variant = BlockModelGenerators.plainModel(model);
                                    Quadrant x = Quadrant.R0;
                                    Quadrant y = Quadrant.R0;
                                    boolean uvlock = false;

                                    if (face == AttachFace.CEILING) {
                                        x = Quadrant.R180;
                                        y = switch (facing) {
                                            case EAST -> Quadrant.R270;
                                            case NORTH -> Quadrant.R180;
                                            case WEST -> Quadrant.R90;
                                            default -> Quadrant.R0;
                                        };
                                    } else if (face == AttachFace.FLOOR) {
                                        y = switch (facing) {
                                            case EAST -> Quadrant.R90;
                                            case SOUTH -> Quadrant.R180;
                                            case WEST -> Quadrant.R270;
                                            default -> Quadrant.R0;
                                        };
                                    } else if (face == AttachFace.WALL) {
                                        x = Quadrant.R90;
                                        uvlock = true;
                                        y = switch (facing) {
                                            case EAST -> Quadrant.R90;
                                            case SOUTH -> Quadrant.R180;
                                            case WEST -> Quadrant.R270;
                                            default -> Quadrant.R0;
                                        };
                                    }

                                    if (x != Quadrant.R0) {
                                        variant = variant.withXRot(x);
                                    }
                                    if (y != Quadrant.R0) {
                                        variant = variant.withYRot(y);
                                    }
                                    if (uvlock) {
                                        variant = variant.withUvLock(true);
                                    }
                                    return BlockModelGenerators.variant(variant);
                                }))
        );
    }
}

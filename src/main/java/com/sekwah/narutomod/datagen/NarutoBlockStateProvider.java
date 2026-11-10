package com.sekwah.narutomod.datagen;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.block.RotatableDecorativeBlock;
import com.sekwah.narutomod.block.weapons.PaperBombBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

public class NarutoBlockStateProvider extends BlockStateProvider {

    public NarutoBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, NarutoMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Bonsai Tree
        VariantBlockStateBuilder bonsaiBuilder = getVariantBuilder(NarutoBlocks.BONSAI_TREE.get());
        ModelFile bonsaiModel = models().getExistingFile(modLoc("block/bonsai_tree"));
        bonsaiBuilder.partialState().with(RotatableDecorativeBlock.FACING, Direction.NORTH)
                .modelForState().modelFile(bonsaiModel).addModel();
        bonsaiBuilder.partialState().with(RotatableDecorativeBlock.FACING, Direction.SOUTH)
                .modelForState().modelFile(bonsaiModel).rotationY(180).addModel();
        bonsaiBuilder.partialState().with(RotatableDecorativeBlock.FACING, Direction.EAST)
                .modelForState().modelFile(bonsaiModel).rotationY(90).addModel();
        bonsaiBuilder.partialState().with(RotatableDecorativeBlock.FACING, Direction.WEST)
                .modelForState().modelFile(bonsaiModel).rotationY(270).addModel();

        // Paper Bomb
        ModelFile paperBombVisible = models().getExistingFile(modLoc("block/paper_bomb"));
        ModelFile paperBombHidden = models().getExistingFile(modLoc("block/paper_bomb_hidden"));

        getVariantBuilder(NarutoBlocks.PAPER_BOMB.get()).forAllStates(state -> {
            AttachFace face = state.getValue(PaperBombBlock.FACE);
            Direction facing = state.getValue(PaperBombBlock.FACING);
            boolean hidden = state.getValue(PaperBombBlock.HIDDEN);
            ModelFile model = hidden ? paperBombHidden : paperBombVisible;

            int x = 0;
            int y = 0;
            boolean uvlock = false;

            if (face == AttachFace.CEILING) {
                x = 180;
                switch (facing) {
                    case EAST -> y = 270;
                    case NORTH -> y = 180;
                    case SOUTH -> y = 0;
                    case WEST -> y = 90;
                    default -> {}
                }
            } else if (face == AttachFace.FLOOR) {
                switch (facing) {
                    case EAST -> y = 90;
                    case NORTH -> y = 0;
                    case SOUTH -> y = 180;
                    case WEST -> y = 270;
                    default -> {}
                }
            } else if (face == AttachFace.WALL) {
                x = 90;
                uvlock = true;
                switch (facing) {
                    case EAST -> y = 90;
                    case NORTH -> y = 0;
                    case SOUTH -> y = 180;
                    case WEST -> y = 270;
                    default -> {}
                }
            }

            ConfiguredModel.Builder<?> builder = ConfiguredModel.builder().modelFile(model);
            if (x != 0) builder.rotationX(x);
            if (y != 0) builder.rotationY(y);
            if (uvlock) builder.uvLock(uvlock);
            return builder.build();
        });
    }
}

package com.sekwah.narutomod.block;

import com.sekwah.narutomod.client.gui.ClientScreenBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Interactive Shinobi Mission Board block.
 * Right-clicking opens the Ninja Menu directly on the Missions tab.
 */
public class MissionBoardBlock extends RotatableDecorativeBlock {

    private static final VoxelShape SHAPE_NS = Shapes.or(
            Block.box(1.0D, 0.0D, 5.5D, 3.5D, 1.5D, 10.5D),
            Block.box(12.5D, 0.0D, 5.5D, 15.0D, 1.5D, 10.5D),
            Block.box(1.5D, 1.5D, 6.75D, 3.0D, 15.0D, 9.25D),
            Block.box(13.0D, 1.5D, 6.75D, 14.5D, 15.0D, 9.25D),
            Block.box(2.5D, 4.0D, 7.0D, 13.5D, 14.5D, 9.0D),
            Block.box(0.5D, 14.5D, 6.0D, 15.5D, 16.0D, 10.0D)
    );

    private static final VoxelShape SHAPE_EW = Shapes.or(
            Block.box(5.5D, 0.0D, 1.0D, 10.5D, 1.5D, 3.5D),
            Block.box(5.5D, 0.0D, 12.5D, 10.5D, 1.5D, 15.0D),
            Block.box(6.75D, 1.5D, 1.5D, 9.25D, 15.0D, 3.0D),
            Block.box(6.75D, 1.5D, 13.0D, 9.25D, 15.0D, 14.5D),
            Block.box(7.0D, 4.0D, 2.5D, 9.0D, 14.5D, 13.5D),
            Block.box(6.0D, 14.5D, 0.5D, 10.0D, 16.0D, 15.5D)
    );

    public MissionBoardBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE_NS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            ClientScreenBridge.openMissionsScreen();
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.85F, 1.05F);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }
}

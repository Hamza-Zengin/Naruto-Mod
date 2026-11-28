package com.sekwah.narutomod.block;

import com.sekwah.narutomod.client.gui.ClientScreenBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Interactive 2-Block-Tall Shinobi Mission Board block.
 * Right-clicking either half opens the Ninja Menu directly on the Missions tab.
 */
public class MissionBoardBlock extends RotatableDecorativeBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER_NORTH = Shapes.or(
            Block.box(0.75D, 0.0D, 5.25D, 4.0D, 2.5D, 10.75D),
            Block.box(12.0D, 0.0D, 5.25D, 15.25D, 2.5D, 10.75D),
            Block.box(1.35D, 2.5D, 6.35D, 3.4D, 16.0D, 9.65D),
            Block.box(12.6D, 2.5D, 6.35D, 14.65D, 16.0D, 9.65D),
            Block.box(2.75D, 6.5D, 5.75D, 13.25D, 9.25D, 10.25D),
            Block.box(2.9D, 9.25D, 6.9D, 13.1D, 16.0D, 9.1D)
    );

    private static final VoxelShape UPPER_NORTH = Shapes.or(
            Block.box(1.35D, 0.0D, 6.35D, 3.4D, 11.5D, 9.65D),
            Block.box(12.6D, 0.0D, 6.35D, 14.65D, 11.5D, 9.65D),
            Block.box(2.9D, 0.0D, 6.9D, 13.1D, 11.0D, 9.1D),
            Block.box(0.0D, 11.0D, 4.2D, 16.0D, 13.8D, 11.8D),
            Block.box(0.0D, 13.6D, 7.1D, 16.0D, 16.0D, 8.9D)
    );

    private static final Map<Direction, VoxelShape> LOWER_SHAPES = buildHorizontalShapes(LOWER_NORTH);
    private static final Map<Direction, VoxelShape> UPPER_SHAPES = buildHorizontalShapes(UPPER_NORTH);

    public MissionBoardBlock(BlockBehaviour.Properties properties) {
        super(properties, LOWER_NORTH);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    private static Map<Direction, VoxelShape> buildHorizontalShapes(VoxelShape northShape) {
        EnumMap<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            map.put(dir, rotateShapeFromNorth(dir, northShape));
        }
        return map;
    }

    private static VoxelShape rotateShapeFromNorth(Direction facing, VoxelShape northShape) {
        if (facing == Direction.NORTH) {
            return northShape;
        }
        VoxelShape[] rotated = new VoxelShape[]{Shapes.empty()};
        northShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (facing) {
                case EAST -> Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX);
                case SOUTH -> Shapes.box(1.0D - maxX, minY, 1.0D - maxZ, 1.0D - minX, maxY, 1.0D - minZ);
                case WEST -> Shapes.box(minZ, minY, 1.0D - maxX, maxZ, maxY, 1.0D - minX);
                default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (directionToNeighbour.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (directionToNeighbour == Direction.UP)) {
            if (neighbourState.is(this) && neighbourState.getValue(HALF) != half) {
                return state.setValue(FACING, neighbourState.getValue(FACING));
            }
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && (player.preventsBlockDrops() || !player.hasCorrectToolForDrops(state))) {
            preventDropFromBottomPart(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void preventDropFromBottomPart(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.is(state.getBlock()) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState replacement = belowState.getFluidState().is(Fluids.WATER)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(belowPos, replacement, 35);
                level.levelEvent(player, 2001, belowPos, Block.getId(belowState));
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        Map<Direction, VoxelShape> map = state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPES : LOWER_SHAPES;
        return map.getOrDefault(facing, LOWER_NORTH);
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

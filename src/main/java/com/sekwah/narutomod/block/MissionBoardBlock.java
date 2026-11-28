package com.sekwah.narutomod.block;

import com.sekwah.narutomod.client.gui.ClientScreenBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
 * Interactive 3x3 Multi-Block Shinobi Mission Board block.
 * Right-clicking any of the 9 parts opens the Ninja Menu directly on the Missions tab.
 */
public class MissionBoardBlock extends RotatableDecorativeBlock {

    public enum BoardPart implements StringRepresentable {
        BOTTOM_LEFT("bottom_left", -1, 0),
        BOTTOM_CENTER("bottom_center", 0, 0),
        BOTTOM_RIGHT("bottom_right", 1, 0),
        MID_LEFT("mid_left", -1, 1),
        MID_CENTER("mid_center", 0, 1),
        MID_RIGHT("mid_right", 1, 1),
        TOP_LEFT("top_left", -1, 2),
        TOP_CENTER("top_center", 0, 2),
        TOP_RIGHT("top_right", 1, 2);

        private final String serializedName;
        private final int dx;
        private final int dy;

        BoardPart(String serializedName, int dx, int dy) {
            this.serializedName = serializedName;
            this.dx = dx;
            this.dy = dy;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

        public int getDx() {
            return this.dx;
        }

        public int getDy() {
            return this.dy;
        }

        @Nullable
        public static BoardPart fromOffset(int dx, int dy) {
            for (BoardPart part : values()) {
                if (part.dx == dx && part.dy == dy) {
                    return part;
                }
            }
            return null;
        }
    }

    public static final EnumProperty<BoardPart> PART = EnumProperty.create("part", BoardPart.class);

    private static final VoxelShape BOTTOM_LEFT_NORTH = Shapes.or(
            Block.box(1.0D, 0.0D, 4.5D, 6.0D, 4.5D, 11.5D),
            Block.box(2.0D, 4.5D, 5.8D, 5.2D, 16.0D, 10.2D),
            Block.box(4.5D, 6.0D, 4.75D, 16.0D, 9.25D, 11.25D),
            Block.box(5.0D, 9.25D, 6.75D, 16.0D, 16.0D, 9.25D)
    );

    private static final VoxelShape BOTTOM_CENTER_NORTH = Shapes.or(
            Block.box(0.0D, 6.0D, 4.75D, 16.0D, 9.25D, 11.25D),
            Block.box(0.0D, 9.25D, 6.75D, 16.0D, 16.0D, 9.25D)
    );

    private static final VoxelShape BOTTOM_RIGHT_NORTH = Shapes.or(
            Block.box(10.0D, 0.0D, 4.5D, 15.0D, 4.5D, 11.5D),
            Block.box(10.8D, 4.5D, 5.8D, 14.0D, 16.0D, 10.2D),
            Block.box(0.0D, 6.0D, 4.75D, 11.5D, 9.25D, 11.25D),
            Block.box(0.0D, 9.25D, 6.75D, 11.0D, 16.0D, 9.25D)
    );

    private static final VoxelShape MID_LEFT_NORTH = Shapes.or(
            Block.box(2.0D, 0.0D, 6.0D, 5.0D, 16.0D, 10.0D),
            Block.box(5.0D, 0.0D, 6.75D, 16.0D, 16.0D, 9.25D)
    );

    private static final VoxelShape MID_CENTER_NORTH = Block.box(0.0D, 0.0D, 6.85D, 16.0D, 16.0D, 9.15D);

    private static final VoxelShape MID_RIGHT_NORTH = Shapes.or(
            Block.box(11.0D, 0.0D, 6.0D, 14.0D, 16.0D, 10.0D),
            Block.box(0.0D, 0.0D, 6.75D, 11.0D, 16.0D, 9.25D)
    );

    private static final VoxelShape TOP_LEFT_NORTH = Shapes.or(
            Block.box(2.0D, 0.0D, 6.0D, 5.0D, 7.5D, 10.0D),
            Block.box(5.0D, 0.0D, 6.4D, 16.0D, 7.5D, 9.6D),
            Block.box(0.0D, 7.5D, 2.5D, 16.0D, 11.8D, 13.5D),
            Block.box(0.0D, 11.8D, 6.9D, 16.0D, 15.2D, 9.1D)
    );

    private static final VoxelShape TOP_CENTER_NORTH = Shapes.or(
            Block.box(0.0D, 0.0D, 5.85D, 16.0D, 7.5D, 9.85D),
            Block.box(0.0D, 7.5D, 2.5D, 16.0D, 11.8D, 13.5D),
            Block.box(0.0D, 11.8D, 6.9D, 16.0D, 15.4D, 9.1D)
    );

    private static final VoxelShape TOP_RIGHT_NORTH = Shapes.or(
            Block.box(11.0D, 0.0D, 6.0D, 14.0D, 7.5D, 10.0D),
            Block.box(0.0D, 0.0D, 6.4D, 11.0D, 7.5D, 9.6D),
            Block.box(0.0D, 7.5D, 2.5D, 16.0D, 11.8D, 13.5D),
            Block.box(0.0D, 11.8D, 6.9D, 16.0D, 15.2D, 9.1D)
    );

    private static final Map<BoardPart, Map<Direction, VoxelShape>> SHAPES = buildAllShapes();

    public MissionBoardBlock(BlockBehaviour.Properties properties) {
        super(properties, BOTTOM_CENTER_NORTH);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BoardPart.BOTTOM_CENTER));
    }

    private static Map<BoardPart, Map<Direction, VoxelShape>> buildAllShapes() {
        EnumMap<BoardPart, Map<Direction, VoxelShape>> all = new EnumMap<>(BoardPart.class);
        all.put(BoardPart.BOTTOM_LEFT, buildHorizontalShapes(BOTTOM_LEFT_NORTH));
        all.put(BoardPart.BOTTOM_CENTER, buildHorizontalShapes(BOTTOM_CENTER_NORTH));
        all.put(BoardPart.BOTTOM_RIGHT, buildHorizontalShapes(BOTTOM_RIGHT_NORTH));
        all.put(BoardPart.MID_LEFT, buildHorizontalShapes(MID_LEFT_NORTH));
        all.put(BoardPart.MID_CENTER, buildHorizontalShapes(MID_CENTER_NORTH));
        all.put(BoardPart.MID_RIGHT, buildHorizontalShapes(MID_RIGHT_NORTH));
        all.put(BoardPart.TOP_LEFT, buildHorizontalShapes(TOP_LEFT_NORTH));
        all.put(BoardPart.TOP_CENTER, buildHorizontalShapes(TOP_CENTER_NORTH));
        all.put(BoardPart.TOP_RIGHT, buildHorizontalShapes(TOP_RIGHT_NORTH));
        return all;
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

    /**
     * Returns the world direction corresponding to model +X (left-to-right across the board)
     * consistent with Minecraft blockstate y-rotations of North-facing models.
     */
    public static Direction getRightAxis(Direction facing) {
        return facing.getClockWise();
    }

    public static BlockPos getAnchorPos(BlockPos pos, BlockState state) {
        BoardPart part = state.getValue(PART);
        Direction rightAxis = getRightAxis(state.getValue(FACING));
        return pos.relative(rightAxis, -part.getDx()).below(part.getDy());
    }

    public static BlockPos getPartPos(BlockPos anchorPos, Direction facing, BoardPart part) {
        Direction rightAxis = getRightAxis(facing);
        return anchorPos.relative(rightAxis, part.getDx()).above(part.getDy());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos anchorPos = context.getClickedPos();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BoardPart part : BoardPart.values()) {
            BlockPos checkPos = getPartPos(anchorPos, facing, part);
            if (checkPos.getY() < level.getMinY() || checkPos.getY() > level.getMaxY() - 1) {
                return null;
            }
            if (!level.getBlockState(checkPos).canBeReplaced(context)) {
                return null;
            }
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, BoardPart.BOTTOM_CENTER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (BoardPart part : BoardPart.values()) {
            if (part == BoardPart.BOTTOM_CENTER) {
                continue;
            }
            BlockPos targetPos = getPartPos(pos, facing, part);
            level.setBlock(targetPos, state.setValue(PART, part), Block.UPDATE_ALL);
        }
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
        BoardPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        Direction rightAxis = getRightAxis(facing);

        BoardPart expectedNeighbour = null;
        if (directionToNeighbour == Direction.UP && part.getDy() < 2) {
            expectedNeighbour = BoardPart.fromOffset(part.getDx(), part.getDy() + 1);
        } else if (directionToNeighbour == Direction.DOWN && part.getDy() > 0) {
            expectedNeighbour = BoardPart.fromOffset(part.getDx(), part.getDy() - 1);
        } else if (directionToNeighbour == rightAxis && part.getDx() < 1) {
            expectedNeighbour = BoardPart.fromOffset(part.getDx() + 1, part.getDy());
        } else if (directionToNeighbour == rightAxis.getOpposite() && part.getDx() > -1) {
            expectedNeighbour = BoardPart.fromOffset(part.getDx() - 1, part.getDy());
        }

        if (expectedNeighbour != null) {
            if (neighbourState.is(this)
                    && neighbourState.getValue(PART) == expectedNeighbour
                    && neighbourState.getValue(FACING) == facing) {
                return state;
            }
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            boolean suppressDrops = player.preventsBlockDrops() || !player.hasCorrectToolForDrops(state);
            destroyOtherParts(level, pos, state, player, suppressDrops);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void destroyOtherParts(Level level, BlockPos pos, BlockState state, Player player, boolean suppressDrops) {
        BoardPart brokenPart = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        BlockPos anchorPos = getAnchorPos(pos, state);

        // Handle BOTTOM_CENTER first so survival breaking of any other part still drops 1 board cleanly,
        // while creative / wrong-tool breaks remove BOTTOM_CENTER with flag 35 (no drop).
        if (brokenPart != BoardPart.BOTTOM_CENTER) {
            BlockState anchorState = level.getBlockState(anchorPos);
            if (anchorState.is(this)
                    && anchorState.getValue(PART) == BoardPart.BOTTOM_CENTER
                    && anchorState.getValue(FACING) == facing) {
                if (!suppressDrops) {
                    Block.dropResources(anchorState, level, anchorPos, null, player, player.getMainHandItem());
                }
                BlockState replacement = anchorState.getFluidState().is(Fluids.WATER)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(anchorPos, replacement, 35);
                level.levelEvent(player, 2001, anchorPos, Block.getId(anchorState));
            }
        }

        for (BoardPart part : BoardPart.values()) {
            if (part == brokenPart || part == BoardPart.BOTTOM_CENTER) {
                continue;
            }
            BlockPos partPos = getPartPos(anchorPos, facing, part);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)
                    && partState.getValue(PART) == part
                    && partState.getValue(FACING) == facing) {
                BlockState replacement = partState.getFluidState().is(Fluids.WATER)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(partPos, replacement, 35);
                level.levelEvent(player, 2001, partPos, Block.getId(partState));
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        Map<Direction, VoxelShape> map = SHAPES.get(state.getValue(PART));
        if (map != null) {
            return map.getOrDefault(facing, BOTTOM_CENTER_NORTH);
        }
        return BOTTOM_CENTER_NORTH;
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

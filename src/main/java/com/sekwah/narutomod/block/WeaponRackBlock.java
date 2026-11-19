package com.sekwah.narutomod.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Functional 3D Shinobi Weapon Rack with 2 display slots (upper/lower).
 * Right-click with an item inserts 1 item into the targeted slot; right-click empty-handed
 * retrieves a displayed item; produces comparator signals 0 / 7 / 15.
 */
public class WeaponRackBlock extends RotatableDecorativeBlock implements EntityBlock {

    private static final VoxelShape SHAPE_NS = Shapes.or(
            Block.box(1.0D, 0.0D, 5.5D, 15.0D, 2.0D, 10.5D),
            Block.box(2.0D, 2.0D, 6.5D, 4.0D, 16.0D, 9.5D),
            Block.box(12.0D, 2.0D, 6.5D, 14.0D, 16.0D, 9.5D)
    );

    private static final VoxelShape SHAPE_EW = Shapes.or(
            Block.box(5.5D, 0.0D, 1.0D, 10.5D, 2.0D, 15.0D),
            Block.box(6.5D, 2.0D, 2.0D, 9.5D, 16.0D, 4.0D),
            Block.box(6.5D, 2.0D, 12.0D, 9.5D, 16.0D, 14.0D)
    );

    public WeaponRackBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE_NS);
    }

    public static int resolveSlotFromHitY(double localHitY) {
        return localHitY >= 0.5D ? WeaponRackBlockEntity.SLOT_UPPER : WeaponRackBlockEntity.SLOT_LOWER;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponRackBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            return rack.getAnalogOutputSignal();
        }
        return 0;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        int preferredSlot = resolveSlotFromHitY(hit.getLocation().y - pos.getY());
        if (rack.findInsertSlot(preferredSlot) < 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int placedSlot = rack.placeItem(preferredSlot, stack);
        if (placedSlot >= 0) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.85F, 1.25F);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        int preferredSlot = resolveSlotFromHitY(hit.getLocation().y - pos.getY());
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack extracted = rack.takeItem(preferredSlot);
        if (!extracted.isEmpty()) {
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, extracted);
            } else if (!player.getInventory().add(extracted)) {
                player.drop(extracted, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.9F, 1.1F);
            return InteractionResult.SUCCESS_SERVER;
        }

        player.sendOverlayMessage(Component.translatable("block.narutomod.weapon_rack.empty")
                .withStyle(ChatFormatting.GRAY));
        return InteractionResult.SUCCESS_SERVER;
    }
}

package com.sekwah.narutomod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores 2 weapon display slots (upper & lower), syncs to clients for 3D rendering,
 * drops contents on block removal, and calculates analog comparator signal levels.
 */
public class WeaponRackBlockEntity extends BlockEntity {

    public static final int SLOT_COUNT = 2;
    public static final int SLOT_UPPER = 0;
    public static final int SLOT_LOWER = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public WeaponRackBlockEntity(BlockPos pos, BlockState state) {
        super(NarutoBlockEntities.WEAPON_RACK.get(), pos, state);
    }

    public static int computeComparatorSignal(int occupiedSlots) {
        if (occupiedSlots <= 0) {
            return 0;
        }
        return occupiedSlots == 1 ? 7 : 15;
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return this.items.get(slot);
    }

    public boolean isSlotEmpty(int slot) {
        return getItem(slot).isEmpty();
    }

    public int getOccupiedSlotCount() {
        int count = 0;
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public int getAnalogOutputSignal() {
        return computeComparatorSignal(getOccupiedSlotCount());
    }

    public int findInsertSlot(int preferredSlot) {
        int clamped = (preferredSlot == SLOT_UPPER) ? SLOT_UPPER : SLOT_LOWER;
        if (isSlotEmpty(clamped)) {
            return clamped;
        }
        int fallback = (clamped == SLOT_UPPER) ? SLOT_LOWER : SLOT_UPPER;
        return isSlotEmpty(fallback) ? fallback : -1;
    }

    public int findExtractSlot(int preferredSlot) {
        int clamped = (preferredSlot == SLOT_UPPER) ? SLOT_UPPER : SLOT_LOWER;
        if (!isSlotEmpty(clamped)) {
            return clamped;
        }
        int fallback = (clamped == SLOT_UPPER) ? SLOT_LOWER : SLOT_UPPER;
        return !isSlotEmpty(fallback) ? fallback : -1;
    }

    public int placeItem(int preferredSlot, ItemStack sourceStack) {
        if (sourceStack.isEmpty()) {
            return -1;
        }
        int targetSlot = findInsertSlot(preferredSlot);
        if (targetSlot < 0) {
            return -1;
        }
        this.items.set(targetSlot, sourceStack.copyWithCount(1));
        syncChanges();
        return targetSlot;
    }

    public ItemStack takeItem(int preferredSlot) {
        int targetSlot = findExtractSlot(preferredSlot);
        if (targetSlot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(targetSlot).copy();
        this.items.set(targetSlot, ItemStack.EMPTY);
        syncChanges();
        return removed;
    }

    private void syncChanges() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
            this.level.updateNeighbourForOutputSignal(this.worldPosition, state.getBlock());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            for (int i = 0; i < this.items.size(); i++) {
                ItemStack stack = this.items.get(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(this.level,
                            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                            stack.copy());
                    this.items.set(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
